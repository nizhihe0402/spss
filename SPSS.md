# SPSS 规则引擎 —— 深度分析与修复方案

> 本文件汇总对 `spss-rule-engine-codex` 的深度代码分析与修复建议。
> 所有结论均来自**完整阅读源码并经 Grep 交叉验证**;架构权威文档见 `GLM.md`。
> 分析日期基准:2026-07-15。

---

## 一、项目概述

| 维度 | 情况 |
|---|---|
| 定位 | 解析 SPSS `.sps` 数据审核脚本,转成 Java 规则,对学生常见病/学校卫生监测表单数据执行清洗校验 |
| 技术栈 | Java 8 · Spring Boot 2.7.18 · Maven · JUnit 5 · 手写 JDBC · 静态 HTML/JS 前端 |
| 规模 | 88 个 Java 文件 / 约 9774 行;核心逻辑高度集中在 `RuleParser`(958 行) |
| 目标库 | MySQL(`application.yml` 指向 `localhost:3306/h2025`) |
| 数据模型 | EAV(每学生多条 `question_id→content`)→ pivot 成宽表 `RowContext` |

**本质**:不是通用 SPSS 解释器,而是针对特定业务子集(COMPUTE / RECODE / IF / DO IF / SORT+MATCH FILES / SELECT IF)的规则引擎。方向定位清晰合理。

---

## 二、真实架构与数据流(经 Grep 确认)

```
.sps ─► SpssParser.parse()  [静态, final class]
          └─聚合─► RuleParser.parseRules()        → List<Rule>
                   RuleParser.parseDatasetRules() → List<DatasetRule>
                   RuleParser.parseOutputRules()  → List<OutputRule>
                                    ▼
                   ParsedScript { rules, datasetRules, outputRules, unsupportedStatements(恒空) }
                                    ▼
   Rule { List<Step> }  每个 Step = 可选条件 + StepAction
                         (ComputeAction | RecodeAction | IfAssignAction)
                                    ▼
   RuleExecutor.execute(rows, rules)  逐行 × 逐规则 × 逐 Step
```

- `SpssParser` 是入口(静态、`final`),聚合 `RuleParser` 的三个**静态**方法(`RuleParser` 是私有构造的工具类,不返回 `ParsedScript`)。
- `Rule` 有两种执行形态:**无 Step**(直接用 `expression`)与**有 Step**(逐 `step.execute`)。检查规则最终按 `target != 0 → flag=1(未通过)` 生成标记。
- DB 驱动执行由 `persistence/ExecutionService` 编排:
  `EAV 答案 → AnswerPivot.pivot() → 每生一行 RowContext → StudentInfoEnricher 补充人口学信息 → RuleExecutor 执行 → StudentSpssRuleResultBuilder 构建结果`。持久化为**手写 JDBC**,无 ORM。
- 变量映射:`bus_question.export_content → SPSS 变量名`,由 `QuestionVariableNameSelector` 把关(仅接受 `[A-Za-z][A-Za-z0-9_]*`)。

---

## 三、文档与实现的偏差(GLM.md 曾多处失真,本轮已校准)

| # | 旧文档声称 | 代码实际 | 证据 |
|---|---|---|---|
| 1 | `ConditionStack` 是"核心新算法",负责 DO IF 扁平化 | **死代码**。生产解析器从不 import/实例化;真正解析用 `RuleParser.findActiveDoIfCondition()` 内联正则栈 | Grep `ConditionStack` |
| 2 | `DATEDIFF/MISSING()/XDATE.*/MOD()/CHAR.SUBSTR/NUMBER()/STRING()` 等**不支持** | `ArithmeticExpression` **完整实现了**这些函数求值 | `ArithmeticExpression.java:134-236` |
| 3 | 分类"按 SPSS 语法模式,不要用关键字/中文名" | `BlockClassifier` 恰恰**硬编码变量名白名单**(`ID3/AGE2/BMI/BPC/SFZ_DATE`)+ 英文关键字 | `BlockClassifier.java:12-16,52-57` |
| 4 | "`AvailabilityChecker` 追踪变量可用链"(暗示执行时用) | `RuleExecutor` **不调用**它;仅 CLI/Simulator/测试用 | Grep + `RuleExecutor.java` |
| 5 | `RuleParser` 有实例 `parse()` 返回 `ParsedScript` | `RuleParser` 是**静态工具类**,`SpssParser` 才是入口 | `RuleParser.java:58` |
| 6 | `ParsedScript.unsupportedStatements` 记录不支持语句 | 字段存在但 `SpssParser.parse()` **从不填充**,恒为空 | `SpssParser.java:19-36` |

> 以上偏差已在本轮更新 `GLM.md` 时逐条勘误。

---

## 四、严重正确性缺陷(按业务影响排序)

### 🔴 P0-1　条件求值器用字符串 split 解析布尔逻辑 —— 括号必崩、NOT 失效
`ConditionExpression.java:25,35`

```java
condition.split("(?i)\\s+OR\\s+|\\s*\\|\\s*")   // 先劈 OR
expression.split("(?i)\\s+AND\\s+|\\s*&\\s*")   // 再劈 AND
```
- **带括号的复合条件被从中间劈开**:`A=1 AND (B=2 OR C=3)` → 按 OR 劈碎,括号语义丢失。
- **一般 `NOT(...)` 不被支持**:`eval()` 只认 `NOT MISSING(...)`;而 DO IF 的 ELSE 分支生成的 `NOT(cond)` 落入数值比较分支 → `left=null` → **恒为 false**。
- **后果**:所有 `DO IF … ELSE … END IF` 的 ELSE 块逻辑**永不执行**;任何带括号布尔条件结果错误。对"审核正确性即生命线"的引擎,这是最严重缺陷。
- **反差**:算术用规范递归下降(`ArithmeticExpression`),条件却退化成 split,明显赶工。

### 🔴 P0-2　`SimpleDateFormat` 静态共享,线程不安全 + 格式硬编码
`ArithmeticExpression.java:15`
```java
private static final SimpleDateFormat SPSS_DATE = new SimpleDateFormat("dd/MM/yyyy", ...);
```
- `SimpleDateFormat` **非线程安全**,Web 并发下 `parse()` 数据竞争 → 抛异常或返回**错误日期**。
- 格式**硬编码 `dd/MM/yyyy`**,与国内常见 `yyyy-MM-dd` 不符 → 生日解析失败 → 年龄算不出。
- **业务放大**:年龄是超重肥胖/血压偏高等**按龄界值判定**的关键输入,年龄错 → 判定全错。

### 🔴 P0-3　DATEDIFF 月/年用 30/365 硬近似
`ArithmeticExpression.java:166-167`
```java
case "months": return diffDays / 30L;
case "years":  return diffDays / 365L;
```
不考虑闰年与实际月长,年龄有系统性偏差,边界年龄(如恰好 7 岁/18 岁分组)误判。

### 🟠 P1-4　`checkRule` 解析期不设,消费端重复推导 4 份
`setCheckRule(deriveCheckRule(rule))` 出现在 `SnippetController:230`、`ExecutionService:88`、`SpsUploadSimulator:186`、`StudentValidationResultBuilder:75`,各有一份 `deriveCheckRule`。
- `RuleParser` 产出的 `Rule.checkRule` 默认 `false` → 纯 `SpssParser+RuleExecutor` 路径(`V2Runner`/`PrototypeCli`)**不生成任何 flag**。
- 四处推导若不同步,同一规则在「预览/执行/校验」中检查语义**可能不一致**。

### 🟠 P1-5　`IfAssignAction` 把赋值右侧当字符串存(不求值)
`IfAssignAction.java:20` → `row.put(target, value)`。`IF(cond) x = y+1` 会把字面串 `"y+1"` 存入 `x`,后续数值化失败变 null。仅常量赋值可靠。

### 🟠 P1-5b　`CHAR.SUBSTR` 在算术上下文返回子串 hashCode
`ArithmeticExpression.java:220`:`return BigDecimal.valueOf(str.substring(s,end).hashCode());` —— 一旦参与算术,数值完全无意义。

### 🟡 P2-6　链式幂求值为 null
`ArithmeticExpression.parsePower()`(`:72`)只处理一次 `**`,`2 ** 3 ** 2` 残留 token 使 `parse()` 末尾 `pos<length` → **整表达式返回 null**。

### 🟡 P2-7　`AnswerPivot` 重复答案静默覆盖
`AnswerPivot.java:32` 同 `sampleKey+variable` 后者覆盖前者,多选题/重复行丢数据且无告警。

---

## 五、代码质量与技术债

**设计亮点(值得肯定):**
- `ArithmeticExpression` 递归下降规范,`BigDecimal` 精度,**null 传播**正确对应 SPSS `$SYSMIS` 语义(除零→null、操作数 null→null)。
- `RecodeCase` 设计干净:`LOWEST/HIGHEST` 开区间、`thru` 闭区间、`COPY` 保留源值、数值 `compareTo` 匹配。
- `RecodeAction` 首个匹配即覆盖 target 并 return,符合 SPSS RECODE 语义。
- v1/v2 双引擎合并为 `engine/` 方向正确;条件解析期解析、执行期不再递归包装的思路对(只是被 P0-1 拖累)。
- 严格遵守 Java 8。

**主要技术债:**
1. **`CompiledExpression` 是伪编译**(`:13-15`):`compile()` 每次 `evaluate` 都重新 `new ArithmeticExpression().parse()`,**零缓存**;叠加 `Step.execute` **每行每步 `new ConditionExpression`**,大数据集性能浪费显著。
2. **`findActiveDoIfCondition` O(n²) 量级**(`RuleParser.java:569`):对每条语句都 `substring` 后正则从头重扫前缀。
3. **多趟全文正则扫描**:`parseRules` 里 COMPUTE、RECODE INTO、standalone IF 各扫一遍全文。
4. **`BlockClassifier` 冗余分支**:`:26/27` 都返回 `COMPUTE_INTERMEDIATE`,`:39/40` 都返回 `MISSING_CHECK`,反复试错堆砌未清理。
5. **正则边界脆弱**:大量以 `\.` 作语句终止符,与小数点潜在冲突;200 字符回看、500 字符硬截断源块。
6. **`ConditionStack` 死代码**应删除或真正接入。

---

## 六、修复方案详解

> 说明:代码示意均保持 **Java 8 兼容**(显式泛型、无 `var`)。例子取自学生健康监测场景。

### 🔴 P0-1　重写 `ConditionExpression`(支持括号 / `NOT(...)` / AND-OR 优先级)

**错误例子 ①(括号被劈碎 → 漏判肥胖学生)**
```spss
DO IF (age >= 7 AND (bmi < 14 OR bmi > 30)).
  COMPUTE abnormal = 1.
END IF.
```
- 输入:`age=8, bmi=32`(肥胖),应命中。
- 当前:按 ` OR ` 先劈成 `["age>=7 AND (bmi<14", "bmi>30)"]`;前段括号被忽略、`32<14` 假;后段 `bmi>30)` 因残留 `)` 使 `parse()` 返回 null 而假 → **整体假 → 肥胖学生漏报**。

**错误例子 ②(`DO IF … ELSE` 恒失效)**
```spss
DO IF (sex = 1).
  COMPUTE bpref = 120.
ELSE.
  COMPUTE bpref = 118.
END IF.
```
- ELSE 分支条件为 `NOT(sex = 1)` → 恒假 → **女生 `bpref` 永远不被设为 118**。

**修复方案**:用递归下降布尔解析器替换 split(优先级由低到高:`OR → AND → NOT → 比较/括号原子`,数值/字符串两侧复用 `ArithmeticExpression`):
```java
boolean parseOr()   { boolean v = parseAnd();
                      while (matchKeyword("OR") || match('|')) v = parseAnd() | v; return v; }
boolean parseAnd()  { boolean v = parseNot();
                      while (matchKeyword("AND") || match('&')) v = parseNot() & v; return v; }
boolean parseNot()  { if (matchKeyword("NOT")) return !parseNot(); return parseAtom(); }
boolean parseAtom() {
    if (match('(')) { boolean v = parseOr(); expect(')'); return v; }   // ← 括号
    if (isMissingCall(...)) return evalMissing(...);
    // 否则:切出 left/op/right,数值或字符串比较(复用现有 compare/evalStringComparison)
}
```
**影响面**:全引擎条件正确性核心。**工作量中,风险中**(需回归 13 个 `.sps` 样本 + 补单测)。

### 🔴 P0-2　日期改 `DateTimeFormatter`(线程安全 + 多格式)

**错误例子**:`birthday="2015-03-01"` → `SPSS_DATE.parse` 按 `dd/MM/yyyy` 抛异常 → 返回 null → `age` 为 null → 该生年龄相关检查被静默跳过。并发下 `Calendar` 竞争 → 偶发错误日期。

**修复方案**:
```java
private static final List<DateTimeFormatter> DATE_FMTS = Arrays.asList(
    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
    DateTimeFormatter.ofPattern("yyyy/MM/dd"),
    DateTimeFormatter.ofPattern("dd/MM/yyyy"));
static LocalDate parseDate(String s) {
    for (DateTimeFormatter f : DATE_FMTS) {
        try { return LocalDate.parse(s.trim(), f); } catch (DateTimeParseException ignore) {}
    }
    return null;
}
```
(`DateTimeFormatter` 天生线程安全。)**工作量小,风险低**,与 P0-3 合并最经济。

### 🔴 P0-3　`DATEDIFF` 用 `java.time` 精确算龄

**错误例子**:`birthday=2010-01-01, testdate=2018-12-31`,真实 8 岁;当前 `3287/365=9` → **偏大 1 岁** → 取错 BMI/血压界值。

**修复方案**:
```java
long years  = ChronoUnit.YEARS.between(birth, test);
long months = ChronoUnit.MONTHS.between(birth, test);
long days   = ChronoUnit.DAYS.between(birth, test);
```
**工作量小(与 P0-2 同处),收益高**。

### 🟠 P1-4　`checkRule` 收敛到解析期单点判定

> ⚠️ 各 `deriveCheckRule` 具体函数体因环境限制未能可靠取回,以下为**示意,落地前须核对实际实现**。

**错误例子**:同一 `.sps`,`V2Runner` 跑(不调 deriveCheckRule → 全 false → 无 flag)与 Web 跑(调 deriveCheckRule → true)得到**不同的检查项/未通过数**。

**修复方案**:解析期单点判定并落 `rule_json`,删除 4 处副本:
```java
// RuleParser 内(或独立 CheckRuleClassifier),parseRules 收尾:
r.setCheckRule(deriveCheckRule(r));   // deriveCheckRule = 现有4份实现的并集
```
**工作量中**;**前置:先本地读回 4 份 deriveCheckRule 做并集对比**。

### 🟠 P1-5　`IfAssignAction` 右侧改为表达式求值

**错误例子**:`IF (age>=18) score = base + 10 .` → 存入字面串 `"base + 10"` → `getDecimal` 失败 → null。

**修复方案**(区分数值表达式 vs 字符串字面量):
```java
public void execute(RowContext row) {
    if (new ConditionExpression(condition, row).eval()) {
        String v = value.trim();
        if (v.startsWith("\"") || v.startsWith("'")) {
            row.put(target, unquote(v));
        } else {
            BigDecimal n = new ArithmeticExpression(v, row).parse();
            row.put(target, n != null ? n : v);   // 求值失败保底存原串
        }
    }
}
```
**工作量小,风险低**。

### 🟠 P1-5b　`CHAR.SUBSTR` 修正算术返回值

**错误例子**:`COMPUTE provincecode = NUMBER(CHAR.SUBSTR(idcard,1,2), F2.0).`,`idcard="420106..."` 应得 `42`;当前返回 `"42".hashCode()` → 完全错误。

**修复方案**:
```java
String sub = str.substring(s, end);
try { return new BigDecimal(sub.trim()); }        // 数值子串 → 真数值
catch (NumberFormatException e) { return null; }  // 非数值:交字符串比较路径处理
```
**工作量小,风险低**。

### 🟡 P2-7a　`AnswerPivot` 重复答案告警

**错误例子**:某生同一变量 `HEIGHT` 有两条答案 `150`、`160` → 静默保留 `160`,`150` 丢失无提示。

**修复方案**:
```java
Object prev = row.get(variable);
if (prev != null && !prev.equals(answer.getContent())) {
    log.warn("重复答案覆盖: key={}, var={}, {} -> {}",
             answer.getSampleKey(), variable, prev, answer.getContent());
}
row.put(variable, answer.getContent());
```
**工作量极小**,行为不变(仍取最后一条),仅增可观测性。

### 🟡 P2-7b　`CompiledExpression` 真正缓存 AST

**问题**:`compile()` 每次 `evaluate` 都重新 parse。1 万行 × 20 条 COMPUTE → 同一表达式被解析 20 万次。
**修复方案**:把 `ArithmeticExpression` 重构为先解析成节点树(AST),`compile()` 只解析一次,`evaluate(row)` 遍历 AST 求值。**工作量较大**,属性能优化,可放最后。

### 🟡 P2-7c　清理 `BlockClassifier` 冗余分支

删除 `:26/:27`、`:39/:40` 永远走不到的重复 `return`。**行为零变化,风险为零**。
> 备注:更深层的"硬编码变量名 + 关键字驱动分类"与文档声明矛盾,是否重做属**设计决策**,另议。

---

## 七、汇总表(修复优先级)

| 编号 | 事项 | 类型 | 工作量 | 风险 | 前置条件 |
|---|---|---|---|---|---|
| P0-1 | `ConditionExpression` 递归下降重写(括号/NOT/优先级) | 正确性 | 中 | 中(需回归13样本) | 补单测 |
| P0-2 | 日期 `DateTimeFormatter` 线程安全 + 多格式 | 正确性/并发 | 小 | 低 | — |
| P0-3 | `DATEDIFF` 用 `java.time` 精确算龄 | 正确性 | 小 | 低 | 与 P0-2 合并 |
| P1-4 | `checkRule` 收敛解析期单点 | 一致性 | 中 | 中 | 先读回4份 deriveCheckRule |
| P1-5 | `IfAssignAction` 右侧表达式求值 | 正确性 | 小 | 低 | — |
| P1-5b | `CHAR.SUBSTR` 修正算术返回值 | 正确性 | 小 | 低 | — |
| P2-7a | `AnswerPivot` 重复答案告警 | 可观测 | 极小 | 极低 | — |
| P2-7b | `CompiledExpression` AST 缓存 | 性能 | 大 | 中 | 求值器重构 |
| P2-7c | `BlockClassifier` 删冗余分支 | 清理 | 极小 | 零 | — |

**建议批次**:
- **第一批(高收益低风险)**:P0-2 + P0-3(同处)、P1-5、P1-5b、P2-7a、P2-7c。
- **第二批(核心正确性)**:P0-1(配回归测试)。
- **第三批(需取证/大重构)**:P1-4(先读回 4 份实现)、P2-7b。

---

## 八、分析方法与覆盖范围说明

**已完整读全文 + Grep 交叉验证(结论可靠):**
`SpssParser`、`RuleParser`(958行)、`BlockClassifier`、`ConditionStack`、`ArithmeticExpression`、`ConditionExpression`、`CompiledExpression`、`RuleExecutor`、`AvailabilityChecker`、`Step`/`StepAction`/三个 `Action`、`Rule`、`RowContext`、`RecodeCase`、`AnswerPivot`、`QuestionVariableNameSelector`、`ParsedScript`、`ExecutionService`(部分)、`pom.xml`、`application.yml`。

**未逐行深入(结论从略,如需再核):**
`SpsRepository`/`SchemaInitializer`/`VersionService`/`ScriptService` 的 SQL 细节、14 个 Controller 的完整端点与鉴权、`AnswerDataValidator`、`RuleCorrectionRuntimeService`、`deriveCheckRule` 各实现体、测试质量。

> 注:分析期间工具输出出现间歇性异常,所有写入结论均已通过"能否编译/自洽性/行号连续性/Grep 复核"筛除污染,仅保留可信读取。
