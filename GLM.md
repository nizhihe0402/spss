# GLM.md

本文件为 GLM 在本仓库工作时提供指导。它取代 `AGENTS.md` 与
`docs/SPSS规则解析与清洗落地详细设计.md` 中已过时的架构描述——那两份描述的是
早期单文件原型,已与代码不符。

> **配套文档:**
> - `SPSS.md` —— 完整的深度代码分析与修复方案(缺陷清单、优先级、逐条修复例子)。
> - `CLAUDE.md` —— 快速上手速览(命令 + 架构 + 陷阱)。

## 构建与运行

```bash
# 编译(Java 8、Spring Boot 2.7.18、Maven)
mvn -q -DskipTests package

# 运行 Spring Boot Web 应用(提供规则管理 UI + REST API,端口 8080)
java -Dfile.encoding=UTF-8 -jar target/spss-rule-engine-codex-*.jar

# CLI 原型,针对单表固定样本
java -Dfile.encoding=UTF-8 \
  -cp target/classes \
  com.gxaysoft.project.spsscheck.prototype.PrototypeCli \
  docs/sources/sps/表1-3.sps \
  docs/sources/sql/相关表数据.sql \
  docs/sources/cvs/表1-3.csv

# V2Runner 批量扫描器:扫描每个 .sps,解析 + 执行,打印汇总
java -Dfile.encoding=UTF-8 -cp target/classes \
  com.gxaysoft.project.spsscheck.V2Runner

# 测试(JUnit 5)
mvn -q test
mvn -q test -Dtest=SpssCheckTest              # 单个测试类
mvn -q test -Dtest=SpssCheckTest#methodName   # 单个测试方法
```

**工具链事实:** `pom.xml` 声明 **Java 8**(不是 17)。源码刻意按 Java 8 编写——
显式泛型(`new ArrayList<String>()`)、无 `var`、无 records、无 `List.of()`。
新代码请保持 Java 8 兼容。

## 本项目是什么

一个规则引擎,解析 SPSS `.sps` 数据审核脚本,并作为 Java 规则对来自数据库的
学校卫生监测表单数据(学生常见病 / 学校卫生监测)执行。

它**不是**通用 IBM SPSS 解释器,只支持一个受限子集。**完整的「已兼容语法/函数 +
范例 + 涉及类」清单见下文《已兼容的 SPSS 语法与函数(详解)》一节** —— 那里是最新、
经代码核对的权威说明;本处仅作概览。

**语句级(由 `RuleParser` 解析成 `Rule`/`DatasetRule`/`OutputRule`):**
`COMPUTE`、`RECODE ... INTO`、就地 `RECODE`、`IF(cond) var=val`、
`DO IF ... [ELSE] ... END IF`、`VARIABLE LABELS`、
`SORT CASES BY` + `MATCH FILES /BY /FIRST /LAST`、`SELECT IF` + `SAVE OUTFILE`。

**表达式内函数(由 `ArithmeticExpression` / `ConditionExpression` 在 COMPUTE/IF
表达式里求值):** `MOD`、`RND`、`DATEDIFF`/`DATEDIF`、`XDATE.YEAR/MONTH/MDAY`、
`NUMBER`、`STRING`、`CHAR.LENGTH`/`LENGTH`、`CHAR.SUBSTR`、`LTRIM`/`RTRIM`、
`MISSING()`、`$SYSMIS`。

> ⚠️ **勘误:** 上列函数在本文件旧版本中被错误地列为「不支持,写入
> `sps_unsupported_statement`」。实际上它们**已在求值器中实现并会被执行**(各自有
> 精度/正确性限制,见详解章节与 `SPSS.md`)。真正被忽略的只有下面这些「硬边界命令」。

**仅作块边界被跳过(既不解析成规则,`SpssParser` 也**不**填充
`ParsedScript.unsupportedStatements`):** `DATASET COPY/ACTIVATE`、
`FILTER OFF`/`USE ALL`、`FREQUENCIES`/`DESCRIPTIVES`/`CTABLES`、
`STRING`/`NUMERIC`/`FORMATS` 声明、`VALUE LABELS`、`VARIABLE LEVEL/WIDTH`、
`SPLIT FILE`。见 `RuleParser.findNextHardBoundary`。

## 架构 —— 统一引擎

> **注:** 原先的 `v1/`(基于步骤)与 `v2/`(基于块/处理器)双引擎已于 2026-07-10
> 合并进 `engine/`。`ConditionalRuleStep`(v1 的递归条件包装)已移除——条件现在于
> 解析期解析,直接存到 `Step` 对象上。

```
com.gxaysoft.project.spsscheck
├── (根)                PrototypeCli, SpsApplication, V2Runner,
│                        SpsUploadToDb, SpsUploadSimulator, SpsUploadV2
├── config/              AnswerTableType(枚举:USER_ANSWER / DOCTOR_ANSWER / STUDENT_ANSWER)
├── engine/              ← 统一的解析 + 执行(原 v1 + v2 合并)
│   ├── model/           Rule, RuleType, Step, StepAction,
│   │                    ComputeAction, RecodeAction, IfAssignAction,
│   │                    DatasetRule, OutputRule, SegmentInfo
│   ├── parser/          SpssParser(顶层入口), RuleParser,
│   │                    BlockClassifier, ConditionStack(死代码), ParsedScript
│   └── executor/        RuleExecutor, AvailabilityChecker
├── execution/           DbRuleExecutionDataLoader, RuleCorrectionRuntimeService
├── parser/              SpssUtil, QuestionSqlParser, QuestionJsonParser,
│                        QuestionVariableNameSelector   ← 校验 export_content 是否为 SPSS 变量名
├── expression/          ArithmeticExpression(递归下降 + - * / ** ()),
│                        ConditionExpression, CompiledExpression
├── io/                  PrototypeFileReaders, AnswerPivot, OutputWriter,
│                        StudentInfoLoader, StudentInfoEnricher, TableIdDetector
├── model/               AnswerRecord, QuestionMapping, RecodeCase, RowContext
├── persistence/         DbConnection, SchemaInitializer, SpsRepository,
│                        ScriptService, VersionService, ExecutionService,
│                        RuleExecutionPersistenceService, RuleCorrectionPlan,
│                        ScriptQuestionMappingService, SourceQuestionMappingSyncService
├── validation/          AnswerDataValidator, AnswerDataValidationReport/Issue,
│                        StudentSpssRuleResultBuilder, StudentValidationResultBuilder,
│                        SourceQuestionMappingFormatter
└── web/                 Controllers + GlobalExceptionHandler(约 14 个 Controller)
```

### 引擎设计

`SpssParser`(入口,静态 `final` 类)把 `.sps` 文本解析成一个 `ParsedScript`,内含
`List<Rule>`、`List<DatasetRule>`、`List<OutputRule>`。它聚合工具类 `RuleParser` 的三个
**静态**方法(`parseRules` / `parseDatasetRules` / `parseOutputRules`);`RuleParser`
本身是私有构造的工具类,不返回 `ParsedScript`。

每个 `Rule` 含 `List<Step>`,每个 `Step` 把一个可选条件字符串与一个 `StepAction`
(`ComputeAction`、`RecodeAction`、`IfAssignAction` 之一)配对。条件在解析期从 DO IF
嵌套中解析。

> ⚠️ **实现现状(与旧描述不符):** DO IF 条件扁平化实际由
> `RuleParser.findActiveDoIfCondition()` 完成——它对每条语句都用正则从头重扫前缀文本
> 维护一个本地条件栈。独立的 `ConditionStack` 类**是死代码**:生产解析器从不 import
> 或实例化它,仅 `ConditionStackTest` 使用。要么让 `RuleParser` 真正改用
> `ConditionStack`,要么删除该类以消除误导。
> 另注:`ParsedScript` 含 `unsupportedStatements` 字段,但 `SpssParser.parse()`
> **从不填充**它。

`RuleExecutor.execute(rows, rules)` 先遍历行、再遍历规则,分派每个 `Step.execute(row)`。
`AvailabilityChecker` 追踪变量链 `DB 列 ∪ 前序规则目标 ∪ 数据集规则变量`。

> ⚠️ **注意:** `RuleExecutor` 本身**不调用** `AvailabilityChecker`——它直接按 `rules`
> 列表顺序执行。可用性过滤只在部分入口(`ExecutionService`、`PrototypeCli`、
> `SpsUploadSimulator`)显式调用。变量可用性实际依赖 `.sps` 中 COMPUTE 的书写顺序。

`RuleType` 是按业务语义分类规则的枚举:
`IDENTITY_CHECK` · `DOCUMENT_CHECK` · `MISSING_CHECK` · `RANGE_CHECK` ·
`OUTCOME_DETERMINATION` · `DUPLICATE_MARK` · `OUTPUT_GROUP` ·
`COMPUTE_INTERMEDIATE` · `CONDITIONAL_BLOCK`。

分类按 **SPSS 语法模式**(见 `BlockClassifier`)。**但请注意:** 现实现同时**硬编码了
变量名白名单**(`ID3/AGE2/BMI/BPC/SFZ_DATE` 等)与英文关键字,并含冗余分支——与"纯语法
模式、不用关键字"的理想有出入(详见 `SPSS.md` 第五节)。

## 执行流程

```
.sps ──► SpssParser.parse() ──► ParsedScript { rules, datasetRules, outputRules }
.sql/.json ──► QuestionSqlParser/QuestionJsonParser ──► Map<SPSS变量名, QuestionMapping>
.csv ──► PrototypeFileReaders ──► List<AnswerRecord(sampleKey, question_id, content, table_id)>
                  │
        AnswerPivot.pivot() ──► List<RowContext>  (EAV → 宽表,每生一行)
                  │
        RuleExecutor.execute(rows, rules)      ← 行级:每行执行 Step.execute()
        DatasetRule.execute(rows)              ← 跨行:SORT + FIRST/LAST
        OutputRule.matches(row)                ← SELECT IF 分组 → 输出表
```

### Step 动作类型

- `ComputeAction` —— 求值算术表达式,把结果存入目标变量。
- `RecodeAction` —— 把源变量经 recode case(equals / range / missing / else)映射到目标。
  **首个命中即写回并停止。**
- `IfAssignAction` —— `IF(cond) target = value` 条件赋值。
  ⚠️ value 按字符串存储,仅常量可靠(见 `SPSS.md` P1-5)。

每个 `Step` 携带一个于解析期从 DO IF 嵌套解析出的可选条件。条件为 null 的 `Step` 无条件
执行;有条件的仅当条件为真时执行。

## 已兼容的 SPSS 语法与函数(详解,带范例与涉及类)

> 本节由代码逐行核对得出,是「到底支持什么」的权威说明。入口
> `SpssParser.parse(text)` 依次调用 `RuleParser.parseRules` /
> `parseDatasetRules` / `parseOutputRules`,产出
> `ParsedScript { rules, datasetRules, outputRules, unsupportedStatements(恒为空) }`。
> 每个条目给出:**SPSS 写法范例 → 解析/执行结果 → 涉及的类**。

### A. 语句级语法(`RuleParser`)

正则常量集中在 `RuleParser` 顶部:`COMPUTE_PATTERN` / `RECODE_INTO_PATTERN` /
`RECODE_SELF_PATTERN` / `IF_ASSIGN_PATTERN` / `LABEL_PATTERN`。

#### A1. `COMPUTE` —— 算术赋值
```spss
COMPUTE bmi = weight / (height/100)**2 .
```
- 解析:`Rule{target=BMI, expression="weight / (height/100)**2", steps=[]}`。无 DO IF
  包裹时 `steps` 为空,执行期直接用 `expression`。
- 执行:`RuleExecutor` 无-Step 分支 → `new ArithmeticExpression(expr,row).parse()` →
  `row.put("BMI", 结果)`。
- 涉及类:`RuleParser.parseRules` · `ComputeAction` · `ArithmeticExpression` ·
  `BlockClassifier.classifyCompute`。

#### A2. `RECODE ... INTO` —— 映射到新变量
```spss
RECODE age (7 thru 12=1)(13 thru 18=2)(ELSE=0) INTO agegrp .
```
- 解析:`Rule{target=AGEGRP, steps=[RecodeAction(source=AGE,
  cases=[range 7~12→1, range 13~18→2, else→0])]}`。
- 执行:取 `AGE`,按 case 顺序 **首个命中即写回** `AGEGRP` 并停止。
- 涉及类:`RuleParser.parseRecodeIntoRules` → `parseRecodeCases` · `RecodeAction` ·
  `RecodeCase` · `BlockClassifier.classifyRecode`。

#### A3. 就地 `RECODE`(source==target,覆盖原值)
```spss
COMPUTE bpflag = 0 .
RECODE bpflag (1=0)(SYSMIS=1)(ELSE=1) .
```
- 解析:COMPUTE 后紧跟对同一变量的 RECODE 时,合成
  `Rule{target=BPFLAG, steps=[Compute, Recode(source=BPFLAG,target=BPFLAG)]}`。
- 关键语义:RECODE **覆盖** target 的值(不是设额外标志);COMPUTE→RECODE→SELECT IF
  链依赖这一点(见「关键设计规则 #3」)。
- 涉及类:`RuleParser.parseRules`(`hasSelfRecode` 分支)+ `parseRuleSteps` ·
  `RECODE_SELF_PATTERN` · `RecodeAction`。

#### A4. `IF (cond) var = value` —— 条件赋值
```spss
IF (age >= 18) adult = 1 .
```
- 解析:`IfAssignAction(condition="age >= 18", target=ADULT, value="1")`。
- ⚠️ **赋值右侧按字符串存储**:仅当 value 是常量(如 `1`)才可靠;
  `IF (a>0) x = y+1 .` 会把字面串 `"y+1"` 存入 `x`,后续数值化失败变 null。
- 涉及类:`RuleParser.parseStandaloneIfAssignRules` · `IfAssignAction` ·
  `ConditionExpression` · `BlockClassifier.classifyConditional`。

#### A5. `DO IF ... [ELSE] ... END IF` —— 条件块
```spss
DO IF (sex = 1).
  COMPUTE bmilevel = 1.
END IF.
```
- 解析:块内语句被打上条件 → `Step(condition="sex = 1", ComputeAction)`;嵌套 DO IF
  用 AND 串接。
- 涉及类:`RuleParser.findActiveDoIfCondition`(内联条件栈)· `Step` ·
  `ConditionExpression`。
- ⚠️ **限制(务必知悉):**
  - **`ELSE IF` 不被识别**(`findActiveDoIfCondition` 只认 `DO IF` / `ELSE.` /
    `END IF`)。
  - **`ELSE` 分支执行期失效**:ELSE 生成 `NOT(cond)` 条件串,而 `ConditionExpression`
    只认 `NOT MISSING(...)`,一般 `NOT(...)` 恒求值为 false → **ELSE 块逻辑不会执行**。

#### A6. `VARIABLE LABELS` —— 变量中文标签
```spss
VARIABLE LABELS bmi '体质指数' .
```
- 解析:进入 label map,填充对应 `Rule.description`(仅描述用途,不参与执行)。
- 涉及类:`RuleParser.parseLabels` · `LABEL_PATTERN`。
- 注:`VALUE LABELS`(值标签)**不解析**,仅作块边界跳过。

#### A7. `SORT CASES BY` + `MATCH FILES /FIRST /LAST` —— 跨行去重标记
```spss
SORT CASES BY id(A).
MATCH FILES /FILE=* /BY id /FIRST=primaryfirst /LAST=primarylast.
```
- 解析:`DatasetRule{sortVariable=ID, byVariable=ID, first=..., last=...}`。
- 执行:**先 SORT 再分组** 打 FIRST/LAST 标记(对应 SPSS 语义,见「关键设计规则 #4」)。
- 涉及类:`RuleParser.parseDatasetRules` · `DatasetRule`。

#### A8. `SELECT IF` + `SAVE OUTFILE` —— 输出分组
```spss
SELECT IF (bmilevel = 1).
SAVE OUTFILE='肥胖名单.sav'.
```
- 解析:`OutputRule{sheetName="肥胖名单", condition="bmilevel = 1"}`(sheet 名取自
  SAVE 路径去扩展名)。
- 执行:`OutputRule.matches(row)` 决定该行是否进入对应输出表。
- 涉及类:`RuleParser.parseOutputRules` · `OutputRule`。

### B. 算术表达式(`ArithmeticExpression`,递归下降求值)

出现在 COMPUTE 右侧与条件的数值侧。结果为 `BigDecimal`,**null 传播** 对应 SPSS
缺失语义(任一操作数为 null → 结果 null;除以 0 → null)。

| 能力 | 范例 | 说明 |
|---|---|---|
| `+ - * /` | `a + b*2 - c/3` | 标准四则;`/` 用 `MathContext.DECIMAL64` |
| `**` 幂 | `(height/100)**2` | 幂优先级高于 `*`;⚠️ 链式 `a**b**c` 会求值为 null |
| 一元 `+ -` | `-x` | |
| `( )` | `(a+b)*c` | |
| `$SYSMIS` | `COMPUTE x = $SYSMIS .` | 求值为 null(缺失/初始化声明) |

**函数**(`evalFunction` / `evalStringFunction`):

| 函数 | 范例 | 说明 / ⚠️ 限制 |
|---|---|---|
| `MOD(a,b)` | `MOD(id,2)` | 取余;b=0 → null |
| `RND(a[,n])` | `RND(bmi,1)` | 四舍五入 HALF_UP |
| `DATEDIFF(d1,d2,'unit')` | `DATEDIFF(testdate,birthday,'years')` | 日期差。⚠️ months=天/30、years=天/365 **近似**,非精确日历 |
| `XDATE.YEAR/MONTH/MDAY(d)` | `XDATE.YEAR(birthday)` | 取年/月/日 |
| `NUMBER(s,fmt)` | `NUMBER(codestr,F8.0)` | 字符串转数字(fmt 被忽略) |
| `CHAR.LENGTH(s)` / `LENGTH(s)` | `CHAR.LENGTH(idcard)` | 字符串长度 |
| `CHAR.SUBSTR(s,i,n)` | `CHAR.SUBSTR(idcard,7,8)` | 1-based 截取。⚠️ **算术上下文返回子串 hashCode**,只适合 `=""` 比较 |
| `MISSING(var)` | `MISSING(weight)` | 缺失=1 否则=0 |
| 字符串函数 | `LTRIM(x)` `RTRIM(x)` `STRING(n,fmt)` | 仅在字符串比较侧(`evalString`)有效 |

> ⚠️ **日期函数共同风险:** `ArithmeticExpression.SPSS_DATE` 是**静态共享的
> `SimpleDateFormat`(线程不安全)**,Web 并发下会解析错乱;且格式**硬编码
> `dd/MM/yyyy`**,与常见 `yyyy-MM-dd` 数据不符。年龄是超重肥胖/血压偏高等按龄判定的
> 关键输入,落地前务必修正(见 `SPSS.md` P0-2/P0-3)。

### C. 条件表达式(`ConditionExpression`)

用于 `IF()` / `DO IF()` / `SELECT IF()`。

| 能力 | 范例 | 说明 |
|---|---|---|
| 比较 | `age >= 7`、`sex <> 2`、`bmi = 0` | `= <> > < >= <=` |
| 逻辑 与/或 | `a=1 AND b=2`、`x>0 OR y>0` | 关键字 AND/OR;符号写法 `&`、`&#124;`。⚠️ 用**字符串 split** 实现 |
| 缺失判定 | `MISSING(weight)`、`NOT MISSING(height)` | |
| 字符串比较 | `zjtype = "1"`、`name <> ""` | 任一侧含引号或 `LTRIM/RTRIM/CHAR.SUBSTR/STRING/MISSING` 时走字符串比较 |

> ⚠️ **条件求值的根本限制(影响正确性,见 `SPSS.md` P0-1):**
> - **不支持括号布尔逻辑**:`a=1 AND (b=2 OR c=3)` 会从 OR 处被劈开、括号丢失 → 结果错误。
> - **不支持一般 `NOT(...)`**(仅 `NOT MISSING`)→ 见 A5 的 ELSE 失效。
> - 布尔优先级依赖「先 split OR 再 split AND」的巧合,仅在无括号时近似「AND 高于 OR」。

### D. RECODE case 类型(`RecodeCase`)

| 类型 | 范例 | 工厂方法 / 语义 |
|---|---|---|
| 等值 | `(1=0)` | `equalsCase`,数值 `compareTo` 比较 |
| 范围 | `(7 thru 12=1)` | `range`,闭区间;支持 `LOWEST thru x` / `x thru HIGHEST` 开区间 |
| 缺失 | `(SYSMIS=1)` / `(MISSING=1)` | `missing`,null 或空串命中 |
| 兜底 | `(ELSE=0)` | `elseCase`,总命中(放最后) |
| 复制 | `(1 thru 5=COPY)` | `isCopyResult`,命中时保留源值 |

### E. 当前不处理 / 会被跳过的语法

- **块边界命令(跳过,不报错、不登记):** `DATASET COPY/ACTIVATE`、
  `FILTER OFF`/`USE ALL`、`FREQUENCIES`/`DESCRIPTIVES`/`CTABLES`、
  `STRING`/`NUMERIC`/`FORMATS`、`VALUE LABELS`、`VARIABLE LEVEL/WIDTH`、`SPLIT FILE`
  —— 见 `RuleParser.findNextHardBoundary`。
- `ELSE IF`:见 A5。
- `ParsedScript.unsupportedStatements` 目前 **恒为空**(`SpssParser` 不填充);若需要
  「不支持语句报告」,需在解析入口补充扫描逻辑。

## 变量映射:`bus_question` → SPSS 变量名

```
bus_question.question_id      ─┐
bus_question.export_content    ├── SPSS 变量名(如 "ID1"、"A121"、"Q6")
bus_question.export_sort       │    仅导出顺序;不用于规则执行映射
bus_question.table_id         ─┘
bus_doctor_answer.question_id ── JOIN ──► answer.content → RowContext.put(变量名, content)
bus_doctor_answer.student_id  ── 标识该答案属于哪一行
```

`QuestionVariableNameSelector.variableNameFromExportContent()` 对此把关:仅当
`export_content` 匹配 `[A-Za-z][A-Za-z0-9_]*` 时返回该名称,过滤掉中文文本与畸形值。
凡从 `bus_question` 推导变量名处都应使用它。

CSV 样本键因表而异:表1-1 用 `code`,表2-1 用 `student_id`。`PrototypeFileReaders`
两者都读(`codeIndex` 以 `studentIndex` 兜底)。`AnswerTableType`(config 包)编码表分组:
table_id 1/2/10 为 USER_ANSWER(以 `code` 为键),3/4/5 为 DOCTOR_ANSWER,6/7/8 为
STUDENT_ANSWER。

## 关键设计规则

1. **`java_preview` 不是执行依据。** 只执行结构化的 `rule_json` 或 `sps_rule_step` 行。
   每条规则三层保存:`spss_source`(溯源)+ `rule_json`(执行/编辑权威)+ `java_preview`
   (仅供人看)。

2. **变量可用性链:** `AvailabilityChecker` 追踪 `DB 列 ∪ 前序规则目标 ∪ 数据集规则变量`。
   依赖尚不可用变量的规则会被标记为不可执行。
   ⚠️ 注意:`RuleExecutor` 本身不强制此链(见上文引擎设计的注意)。

3. **RECODE 覆盖目标字段的值。** COMPUTE→RECODE→SELECT IF 链依赖 RECODE 替换算得的值,
   而非仅设置一个标志。

4. **SORT CASES 必须先于 FIRST/LAST 赋值。** `DatasetRule.execute()` 先按 `sortVariable`
   排序,再分组——对应 SPSS 语义。

5. **`table_id` 过滤至关重要。** CSV 数据可能含多个表的答案;错误的 `table_id` 会得到零行。
   `TableIdDetector` 自动检测最频繁的 `table_id`,也可显式传入。

6. **规则生命周期:** DRAFT → REVIEWING → PUBLISHED。执行绑定到一个 PUBLISHED 的
   `version_id`。

## 规则存储的数据库 Schema

`docs/spss_rule_schema.sql` —— MySQL 5.7(兼容 PostgreSQL/Kingbase)。

规则存储:`sps_script`、`sps_rule`、`sps_rule_step`、`sps_output_rule`、
`sps_rule_change_log`、`sps_unsupported_statement`、`sps_rule_version`。
执行:`sps_run_batch`、`sps_run_row_temp`、`sps_run_var_temp`、
`sps_check_error_detail`。去重:`sps_clean_unique_index`。

`sps_rule.source_question_mappings`(新增 TEXT 列)缓存由 `SourceQuestionMappingFormatter`
生成的 `变量 -> question_id` 查找表,由 `SourceQuestionMappingSyncService.syncScript(scriptId)`
回填并在 UI 呈现。`docs/spss_rule_schema.sql`(DDL)与 `src/main/resources/schema.sql`
(幂等的 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`)都声明了它。

## 上游数据库

> 运行配置见 `src/main/resources/application.yml`,实际库名为 `h2025`
> (`jdbc:mysql://localhost:3306/h2025`)。

| 表 | 用途 |
|---|---|
| `bus_question` | 问题定义;`export_content` → SPSS 变量;`table_id` |
| `bus_doctor_answer` | 学生答案:`question_id`、`student_id`、`content`、`table_id`、`project_id` |
| `bus_student` | 人口学:姓名、性别、生日、学校、年级、班级 |
| `bus_school` | 学校信息:名称、类型、区域 |
| `bus_project` / `bus_project_table` | 项目/年度定义与表隶属 |
| `bus_option` | 答案选项(选择题) |

完整 schema:`docs/sources/sql/整个库表结构.sql`。

## `docs/sources/` 中的测试固定样本

```
sps/          13 个 SPSS 脚本,横跨 3 个表组 + 结局判定
              表1-1/1-2/1-3(行政)、表2-1/2-2/2-3(学生体检)、表3-1/3-2/3-3、
              修近视筛查语法.sps、结局判定/(超重肥胖/血压偏高/脊柱弯曲)
cvs/          样本答案 CSV(每表一份)
sql/          整个库表结构.sql(完整 schema 导出)、相关表数据.sql(INSERT 数据)、
              bus_question_*.json/.csv/.xlsx(问题导出快照)
data/         学生证件类型证件号.json(学生证件类型/号码补充)
```

## 已知问题与技术债

- **`AGENTS.md` 已过时。** 它描述的是单文件原型。请信任本文件,并在做结构性改动时更新
  `AGENTS.md`。
- **`V2Runner.main()` 中的硬编码路径** 虽有 CLI 参数与系统属性(`mappingPath`、
  `studentPath`)兜底,但默认值仍指向本地固定样本路径——非本机运行时若不覆盖会失败。
- **规则版本 / 发布 / 审计服务层。** Schema(`sps_rule_version`、`sps_rule_change_log`)
  已存在;`VersionService`、`ScriptService` 等服务类现在也已存在,但完整的
  「保存解析结果入库 / 发布版本 / 按已发布 version_id 执行」生命周期是否完整**未在本轮
  逐一核实**,改动前请复核。
- **换行符噪声:** 工作副本会触发大量 `LF will be replaced by CRLF` 警告。`.gitattributes`
  (`* text=auto eol=lf`)已就位,但对既有已跟踪文件可能需要 `git add --renormalize .`。
- **引擎正确性缺陷(重要):** 条件求值、日期处理等存在若干会影响审核正确性的缺陷,
  完整清单与修复方案见 **`SPSS.md`**。

## 数据处理注意

`docs/sources/cvs/` 与 `docs/sources/data/` 下的部分文件疑似**真实学生健康记录**
(如数千行的 `bus_student_answer_*.csv`、`学生证件类型证件号.json`)。请勿再提交额外的
真实 PII,优先使用合成/去标识化的样本。存疑时先提出,不要径直处理。

## 验证

除固定样本外无 golden-file 校验框架。手动验证:用 CLI 原型跑已知固定样本,与
`docs/table1-3-verify.txt` 对比;运行 `mvn test` 执行 `src/test/java/.../parser` 与
`.../validation` 下的 JUnit 5 套件。
