# SPSS 规则引擎统一改造方案

> 日期：2026-07-10 | 状态：待确认

## 0. 背景与目标

### 当前问题

| 问题 | 严重度 | 影响范围 |
|---|---|---|
| v1/v2 双引擎共存，v2 依赖 v1 执行，无清晰迁移路径 | 🔴 严重 | 架构核心 |
| `SpssRuleParser` 通过反射访问 `ConditionalRuleStep.delegate` 私有字段 | 🔴 严重 | 运行时脆断 |
| `RuleExecuteV2Controller` 366 行 God Controller | 🟡 中等 | 可维护性 |
| 缺失版本生命周期服务层（schema 存在但未实现） | 🟡 中等 | 功能缺口 |
| 零事务管理（`PersistenceService` 多步操作无 `@Transactional`） | 🟡 中等 | 数据一致性 |
| 硬编码 tableId、文件路径、中文关键词 | 🟡 中等 | 可配置性 |
| 15+ 处 `catch (Exception ignored) {}`，零 SLF4J 使用 | 🟡 中等 | 可观测性 |
| 表达式每次执行重新解析（AST 无缓存） | 🟡 中等 | 性能 |
| 明文数据库密码 | 🟢 低 | 安全性 |
| `.gitattributes` 缺失，CRLF 警告泛滥 | 🟢 低 | 开发体验 |

### 改造目标

1. **合并 v1/v2 为单一引擎** `engine/` — 一个 `Rule` 实体，一个 `RuleExecutor`
2. **消除反射** — 条件栈扁平化替代 `ConditionalRuleStep` 递归嵌套
3. **建立服务层** — `ScriptService` / `ExecutionService` / `VersionService` / `PersistenceService`
4. **拆分 God Controller** — 4 个职责单一的 Controller
5. **基础设施修复** — 日志、事务、异常处理、硬编码消除

---

## 1. 核心设计：条件栈扁平化

### 1.1 问题

v1 的 `ConditionalRuleStep` 用递归嵌套表达 DO IF 条件：

```
ConditionalStep(C1,
    ConditionalStep(C2,
        ComputeStep(target, expr)))
```

这导致必须通过反射访问 `delegate` 私有字段才能拿到内层步骤。

### 1.2 方案

解析时维护一个**条件栈**，在每个叶子步骤处将栈中所有条件 AND 拼接，产出**扁平列表**：

```
Step(condition="C1 AND C2", action=ComputeStep(target, expr))
```

每个 Step 的 `condition` 是普通字符串，运行时由现有的 `ConditionExpression` 求值。**无递归、无反射。**

### 1.3 真实 SPSS 脚本 DO IF 嵌套分析

分析覆盖 `docs/sources/sps/` 全部 13 个 `.sps` 文件：

| 文件 | DO IF 数量 | 最大深度 | 有嵌套？ |
|---|---|---|---|
| 表1-1.sps | 1 | 1 | ❌ |
| 表1-2.sps | 1 | 1 | ❌ |
| 表1-3.sps | 4 | 1 | ❌ |
| 表2-1.sps | ~90 | **3** | ✅ |
| 表2-2.sps | ~90 | 1 | ❌ |
| 表2-3.sps | 2 | 1 | ❌ |
| 表3-1.sps | 1 | 1 | ❌ |
| 表3-2.sps | 1 | 1 | ❌ |
| 表3-3.sps | 1 | 1 | ❌ |
| 修近视筛查语法.sps | ~50 | **3** | ✅ |
| 结局判定/血压偏高语法.sps | ~200 | 1 | ❌ |
| 结局判定/超重肥胖...语法.sps | ~200 | 1 | ❌ |
| 结局判定/脊柱弯曲判定-1126.sps | 0 | 0 | ❌ |

**结论**：仅 2/13 文件存在嵌套，最大深度 = 3，所有嵌套均为同一业务逻辑（身份证号校验）的复制品。11/13 文件完全不需要嵌套能力。

### 1.4 算法详解

#### 例 1：深度 1，有 ELSE

SPSS：
```
DO IF (PrimaryFirst1).
    COMPUTE MatchSequence = 1 - PrimaryLast.
ELSE.
    COMPUTE MatchSequence = MatchSequence + 1.
END IF.
```

栈轨迹：
```
初始:  stack = []

"DO IF (PrimaryFirst1)"     → push "PrimaryFirst1"         stack = [PrimaryFirst1]
"COMPUTE MS = 1 - PL"       → 拍平 → Step(PrimaryFirst1, COMPUTE MS=1-PL)
"ELSE"                      → pop → push "NOT(PrimaryFirst1)"  stack = [NOT(PrimaryFirst1)]
"COMPUTE MS = MS + 1"       → 拍平 → Step(NOT(PrimaryFirst1), COMPUTE MS=MS+1)
"END IF"                    → pop                          stack = []
```

产出：
```
Step(condition="PrimaryFirst1",       action=COMPUTE MS = 1 - PL)
Step(condition="NOT(PrimaryFirst1)",  action=COMPUTE MS = MS + 1)
```

#### 例 2：深度 2，有 ELSE（身份证出生日期校验）

SPSS（修近视筛查 line 241-250）：
```
DO IF (grade <> 53 AND ZJTYPE = 1).                          ← C1
    DO IF (MISSING(SFZ) OR MISSING(BIRTH)                    ← C2
           OR LENGTH(RTRIM(SFZ)) < 14).
        COMPUTE 身份证出生日期异常 = 1.
    ELSE.
        COMPUTE 身份证出生日期异常 = (复杂计算).
    END IF.
END IF.
```

栈轨迹：
```
初始:  stack = []

DO IF (C1)            → push C1               [C1]
DO IF (C2)            → push C2               [C1, C2]
COMPUTE ... = 1       → 拍平 → Step(C1∧C2, COMPUTE ...=1)
ELSE                  → pop C2, push ¬C2      [C1, ¬C2]
COMPUTE ... = (计算)   → 拍平 → Step(C1∧¬C2, COMPUTE ...=(计算))
END IF                → pop                   [C1]
END IF                → pop                   []
```

产出：
```
Step(condition="C1 AND C2",       action=COMPUTE 身份证出生日期异常 = 1)
Step(condition="C1 AND NOT(C2)",  action=COMPUTE 身份证出生日期异常 = (复杂计算))
```

**注意**：外层 C1 没有 ELSE。不满足 C1 的行 → 不执行任何 Step → 变量保持原值（$SYSMIS）。

#### 例 3：深度 3，多层 ELSE（身份证性别校验）

SPSS（修近视筛查 line 275-297，表2-1 line 324-346 一致）：
```
DO IF (grade <> 53 AND ZJTYPE = 1).                          ← C1
    DO IF (LENGTH(SFZ) = 18 AND NOT MISSING(gender)).        ← C2
        COMPUTE #gender_bit = NUMBER(CHAR.SUBSTR(SFZ,17,1)).  ← 中间计算
        DO IF (NOT MISSING(#gender_bit)).                     ← C3
            COMPUTE 身份证性别异常 = (MOD校验逻辑).
        ELSE.
            COMPUTE 身份证性别异常 = 1.
        END IF.
    ELSE.
        COMPUTE 身份证性别异常 = 1.
    END IF.
END IF.
```

栈轨迹：
```
初始:  stack = []

DO IF (C1)       → push C1    [C1]
DO IF (C2)       → push C2    [C1, C2]
COMPUTE #gb      → 拍平 Step(C1∧C2, COMPUTE #gb=NUMBER(...))
DO IF (C3)       → push C3    [C1, C2, C3]
COMPUTE 异常=(校验) → 拍平 Step(C1∧C2∧C3, COMPUTE 异常=(校验))
ELSE             → pop C3 → push ¬C3   [C1, C2, ¬C3]
COMPUTE 异常=1    → 拍平 Step(C1∧C2∧¬C3, COMPUTE 异常=1)
END IF           → pop ¬C3   [C1, C2]
ELSE             → pop C2 → push ¬C2   [C1, ¬C2]
COMPUTE 异常=1    → 拍平 Step(C1∧¬C2, COMPUTE 异常=1)
END IF           → pop ¬C2   [C1]
END IF           → pop C1    []
```

产出（按目标变量分组为 2 条 Rule）：
```
Rule "#gender_bit" (COMPUTE_INTERMEDIATE):
  Step(condition="C1 AND C2",
       action=COMPUTE #gender_bit = NUMBER(CHAR.SUBSTR(SFZ,17,1), F8.0))

Rule "身份证性别异常" (DOCUMENT_CHECK):
  Step(condition="C1 AND C2 AND C3",
       action=COMPUTE 身份证性别异常 = ((MOD(#gender_bit,2)=1 AND gender=2)
                                    OR (MOD(#gender_bit,2)=0 AND gender=1)))
  Step(condition="C1 AND C2 AND NOT(C3)",
       action=COMPUTE 身份证性别异常 = 1)
  Step(condition="C1 AND NOT(C2)",
       action=COMPUTE 身份证性别异常 = 1)
```

其中 `C1 = grade <> 53 AND ZJTYPE = 1`，`C2 = LENGTH(SFZ) = 18 AND NOT MISSING(gender)`，`C3 = NOT MISSING(#gender_bit)`。

### 1.5 ELSE 取反规则

```
ELSE 语义 = "前面所有同层条件都失败时"
ELSE 编码 = pop 栈顶条件 C → push NOT(C)
其余栈中条件不变（它们筛选出的行才进入了当前 ELSE 分支）
```

Java 的 `if-else if-else` 级联对应 SPSS 的 `ELSE + DO IF` 嵌套：

```java
if (L=1) {           // DO IF (L=1)
    if (A) {         //   DO IF (A)
        // 指令1     //     指令1: L=1 AND A
    } else if (B) {  //   ELSE. DO IF (B)
        // 指令2     //     指令2: L=1 AND NOT(A) AND B
    } else {         //   ELSE.
        // 指令3     //     指令3: L=1 AND NOT(A) AND NOT(B)
    }                //   END IF. END IF.
} else {             // ELSE.
    // 指令4         //   指令4: NOT(L=1)
}                    // END IF.
```

### 1.6 算法伪代码

```java
List<Step> parseBlock(String text, List<String> parentStack) {
    List<Step> steps = new ArrayList<>();

    for each statement in text:
        if matches "DO IF (condition)":
            List<String> innerStack = new ArrayList<>(parentStack);
            innerStack.add(condition);
            steps.addAll(parseBlock(innerBlock, innerStack));

        else if matches "ELSE":
            String last = parentStack.remove(parentStack.size() - 1);
            parentStack.add("NOT(" + last + ")");

        else if matches "END IF":
            parentStack.remove(parentStack.size() - 1);
            return steps;

        else if matches "COMPUTE" / "RECODE" / "IF":
            String condition = parentStack.isEmpty()
                ? null
                : String.join(" AND ", parentStack);
            steps.add(new Step(parseAction(statement), condition));

    return steps;
}
```

---

## 2. 统一引擎架构

### 2.1 包结构

```
src/main/java/com/gxaysoft/project/spsscheck
├── engine/                         ← 新建：统一引擎
│   ├── model/
│   │   ├── Rule.java               ← 统一规则实体（合并 v1 SpssCheckRule + v2 RuleDefinition）
│   │   ├── RuleType.java           ← 业务分类枚举（从 v2 迁移）
│   │   ├── Step.java               ← 扁平化步骤（condition + StepAction）
│   │   ├── StepAction.java         ← 密封接口（ComputeAction | RecodeAction | IfAssignAction）
│   │   ├── RecodeCase.java         ← 现有，直接迁移
│   │   ├── DatasetRule.java        ← SORT + MATCH FILES（从 v1 SpssDatasetRule 迁移）
│   │   ├── OutputRule.java         ← SELECT IF + SAVE（从 v1 SpssOutputRule 迁移）
│   │   └── SegmentInfo.java        ← UI 用：startLine, endLine, segmentTitle, splitReason
│   │
│   ├── parser/
│   │   ├── SpssParser.java         ← 统一解析入口 → 产出 ParsedScript
│   │   ├── RuleParser.java         ← COMPUTE/RECODE/IF 解析（从 v1 SpssRuleParser 重构）
│   │   ├── ConditionStack.java     ← 条件栈，DO IF 扁平化（核心新算法）
│   │   ├── BlockClassifier.java    ← 语法模式 → RuleType 分类（从 v2 BlockParser.classify* 迁移）
│   │   └── ParsedScript.java       ← 解析结果 DTO
│   │
│   ├── executor/
│   │   ├── RuleExecutor.java       ← 行级执行（从 v1 RuleEngine.execute 迁移）
│   │   └── AvailabilityChecker.java ← 变量可用性链（从 v1 RuleAvailabilityChecker 迁移）
│   │
│   └── handler/
│       ├── HandlerRegistry.java    ← RuleType → Handler 分发（从 v2 迁移）
│       └── CheckHandler.java       ← 校验类规则执行（从 v2 迁移）
│
├── expression/                     ← 不变。改动：ArithmeticExpression 预编译 AST
│   ├── ArithmeticExpression.java   ← 新增 .compile() 静态方法，缓存 AST
│   └── ConditionExpression.java    ← 不变
│
├── io/                             ← 不变
├── model/                          ← 不变（AnswerRecord, QuestionMapping, RowContext 等）
├── parser/                         ← 不变（QuestionSqlParser, QuestionJsonParser 等）
│
├── persistence/
│   ├── ScriptService.java          ← 新建：脚本→解析→持久化
│   ├── ExecutionService.java       ← 新建：编排执行流水线
│   ├── VersionService.java         ← 新建：DRAFT→PUBLISHED 生命周期
│   ├── PersistenceService.java     ← 改造：+@Transactional, +Logger
│   ├── DbConnection.java           ← 不变
│   ├── SchemaInitializer.java      ← 不变
│   ├── SpsRepository.java          ← 不变
│   └── ...
│
├── execution/                      ← 不变
│   ├── DbRuleExecutionDataLoader.java
│   └── RuleCorrectionRuntimeService.java
│
├── validation/                     ← 不变
├── web/                            ← 拆分
│   ├── ScriptController.java       ← 新建
│   ├── ExecuteController.java      ← 新建（替代 RuleExecuteV2Controller）
│   ├── VersionController.java      ← 新建
│   ├── RuleController.java         ← 保留
│   ├── UploadController.java       ← 保留
│   ├── ...
│   └── GlobalExceptionHandler.java ← 新建：@ControllerAdvice
│
└── config/
    └── AnswerTableType.java        ← 新建：枚举替代硬编码 tableId
```

### 2.2 删除清单

| 删除 | 原因 |
|---|---|
| `v1/` 整个包 | 合并到 `engine/` |
| `v2/` 整个包 | 合并到 `engine/` |
| `v1/model/ConditionalRuleStep.java` | 被 ConditionStack 扁平化替代 |
| `v1/model/PositionedRuleStep.java` | 解析临时使用，不输出到 Rule |
| `v1/parser/RuleDescriptionBuilder.java` | 合并到 `BlockClassifier` |
| `v2/model/RuleDefinition.java` | 被 `Rule` 替代 |
| `v2/model/SpssSegment.java` | 被 `SegmentInfo` 替代 |
| `v2/model/RuleHandler.java` | 简化接口，移到 `engine/handler/` |
| `v2/executor/BlockExecutor.java` | 被 `RuleExecutor` 替代 |
| `web/RuleExecuteV2Controller.java` | 被 `ExecuteController` 替代 |
| `web/RuleControllerV2.java` | 合并到 `RuleController` |
| `executor/` 空目录 | 死代码 |
| `legacy/` 空目录 | 死代码 |

### 2.3 统一 Rule 实体

```java
package com.gxaysoft.project.spsscheck.engine.model;

public class Rule {
    // ── 业务标识 ──
    private String target;              // 目标变量名（唯一键）
    private RuleType type;              // 业务分类（用于 UI 展示和统计分析）

    // ── 执行权威 ──
    private List<Step> steps;           // 执行步骤链（无条件时为 null）
    private String expression;          // 无 Step 时的简化表达式
    private boolean checkRule;          // 校验规则标志（结果强制 0/1）
    private String spssSource;          // 原始 SPSS 文本片段

    // ── UI 标注 ──
    private SegmentInfo segment;        // 行号、标题
    private String javaPreview;         // 人类可读的 Java 伪代码

    // ── 依赖 ──
    private List<String> sourceVariables;   // 依赖变量列表
    private String sourceQuestionMappings;  // var → question_id 映射
}
```

### 2.4 Step 实体

```java
package com.gxaysoft.project.spsscheck.engine.model;

public class Step {
    private String condition;   // null 表示无条件执行
    private StepAction action;  // ComputeAction | RecodeAction | IfAssignAction

    // 执行
    public void execute(RowContext row) {
        if (condition == null || evalCondition(condition, row)) {
            action.execute(row);
        }
    }
}

// StepAction 密封接口
public interface StepAction {
    void execute(RowContext row);
    String target();
    List<String> sourceVariables();
}

public class ComputeAction implements StepAction {
    private final String target;
    private final String expression;
    private final CompiledExpression compiled;  // 预编译的 AST（新增缓存）
}

public class RecodeAction implements StepAction {
    private final String source;
    private final String target;
    private final List<RecodeCase> cases;
}

public class IfAssignAction implements StepAction {
    private final String condition;   // IF 自身的条件（与 DO IF 条件不同）
    private final String target;
    private final String value;
}
```

---

## 3. 表达式预编译

### 3.1 当前问题

```java
// RuleEngine.java 当前实现
for (RowContext row : rows) {
    for (SpssCheckRule rule : rules) {
        // 每行都重新词法分析 + 建立 AST
        BigDecimal computed = new ArithmeticExpression(rule.getExpression(), row).parse();
    }
}
```

10,000 行 × 30 条 COMPUTE = 300,000 次重复解析。

### 3.2 方案

```java
// 解析时（一次）
CompiledExpression ast = ArithmeticExpression.compile(expression);

// 执行时（每行）
BigDecimal result = ast.evaluate(row);
```

`ArithmeticExpression` 已是递归下降 AST 结构，改造量约 30 行：构造函数保留（兼容旧代码），新增静态 `compile()` 方法返回可复用的 `CompiledExpression` 对象。

---

## 4. 服务层设计

### 4.1 ScriptService

```java
@Service
public class ScriptService {
    private static final Logger log = LoggerFactory.getLogger(ScriptService.class);

    /**
     * 保存或更新脚本，触发完整解析。
     * @return 解析结果（含分类、规则列表、不支持的语句列表）
     */
    @Transactional
    public ParsedScript saveAndParse(Long scriptId, String spsText) {
        // 1. 持久化到 sps_script
        // 2. 调用 SpssParser.parse() 解析
        // 3. 每条 Rule 持久化到 sps_rule + sps_rule_step
        // 4. 标记 unsupported_statements
        // 5. 同步 source_question_mappings
        return parsedScript;
    }

    public ParsedScript getParsedRules(Long scriptId) { ... }

    public Map<String, QuestionMapping> loadMappings(Long scriptId, Long tableId) { ... }
}
```

### 4.2 ExecutionService

```java
@Service
public class ExecutionService {
    private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);

    private final ScriptService scriptService;
    private final PersistenceService persistenceService;

    /**
     * 完整执行流水线：加载→透视→富化→执行→构建结果。
     */
    public ExecuteResult execute(ExecuteRequest req) {
        // 1. 校验（AnswerDataValidator）
        // 2. 加载 Rules（scriptService）
        // 3. 加载 Mappings
        // 4. 加载 StudentInfo
        // 5. AnswerPivot.pivot()
        // 6. StudentInfoEnricher.enrich()
        // 7. RuleExecutor.execute(rows, rules)
        // 8. DatasetRule.execute(rows)
        // 9. StudentSpssRuleResultBuilder.build()
        log.info("执行完成: scriptId={}, tableId={}, rows={}, rules={}",
            req.scriptId, req.tableId, rows.size(), rules.size());
        return result;
    }

    public ExecuteResult executeFromDb(ExecuteDbRequest req) {
        // 同上 + DbRuleExecutionDataLoader
    }
}
```

### 4.3 VersionService

```java
@Service
public class VersionService {
    private static final Logger log = LoggerFactory.getLogger(VersionService.class);

    /**
     * 发布脚本的最新解析结果为正式版本。
     * 执行始终绑定到 PUBLISHED 的 version_id。
     */
    @Transactional
    public Long publishVersion(Long scriptId) {
        // 1. 校验所有 Rule 的 Availability（变量链完整性）
        // 2. 新建 sps_rule_version 行 (status = PUBLISHED)
        // 3. 关联 sps_rule.version_id = versionId
        // 4. 记录操作人到 sps_rule_change_log
        log.info("版本已发布: scriptId={}, versionId={}", scriptId, versionId);
        return versionId;
    }

    public List<VersionSummary> listVersions(Long scriptId) { ... }

    public Long getActiveVersionId(Long scriptId) { ... }
}
```

### 4.4 PersistenceService（改造现有 `RuleExecutionPersistenceService`）

```java
@Service
public class PersistenceService {
    private static final Logger log = LoggerFactory.getLogger(PersistenceService.class);

    @Transactional
    public SaveSummary saveExecutionResult(...) {
        // 原有逻辑不变
        // 所有 catch (Exception ignored) {} 替换为:
        //   catch (Exception e) { log.warn("xxx 失败: {}", e.getMessage()); }
        log.info("执行结果已保存: cleanTable={}, cleanRows={}, failRows={}",
            cleanTable, cleanRows, failRows);
    }
}
```

### 4.5 依赖关系

```
Controller 层
    │
    ├── ScriptService ──────┬── SpssParser (engine)
    │                       └── JdbcTemplate
    │
    ├── ExecutionService ───┬── ScriptService
    │                       ├── PersistenceService
    │                       ├── RuleExecutor (engine)
    │                       └── AnswerPivot / StudentInfoEnricher (io)
    │
    ├── VersionService ─────┬── ScriptService
    │                       └── JdbcTemplate
    │
    └── PersistenceService ─── JdbcTemplate
```

---

## 5. Controller 拆分

### 5.1 ExecuteController（核心，替代 RuleExecuteV2Controller）

```java
@RestController
@RequestMapping("/api/execute")
public class ExecuteController {
    private static final Logger log = LoggerFactory.getLogger(ExecuteController.class);

    @Autowired
    private ExecutionService executionService;

    /**
     * CSV 上传执行
     */
    @PostMapping("/upload")
    public Map<String, Object> executeFromUpload(
            @RequestParam("csvFile") MultipartFile csvFile,
            @RequestParam("scriptId") Long scriptId,
            @RequestParam(value = "tableId", required = false) Long tableId,
            @RequestParam(value = "mappingFile", required = false) MultipartFile mappingFile,
            @RequestParam(value = "studentFile", required = false) MultipartFile studentFile,
            @RequestParam(value = "strictValidate", defaultValue = "false") boolean strictValidate) {

        log.info("CSV 执行请求: scriptId={}, fileSize={}", scriptId,
            csvFile != null ? csvFile.getSize() : 0);

        ExecuteRequest req = ExecuteRequest.fromUpload(
            csvFile, scriptId, tableId, mappingFile, studentFile, strictValidate);
        return executionService.execute(req).toMap();
    }

    /**
     * 数据库数据执行
     */
    @PostMapping("/db")
    public Map<String, Object> executeFromDb(
            @RequestParam("scriptId") Long scriptId,
            @RequestParam("projectId") Long projectId,
            @RequestParam("tableId") Long tableId,
            @RequestParam("divisionId") Long divisionId,
            @RequestParam(value = "schoolId", required = false) Long schoolId,
            @RequestParam("year") String year,
            @RequestParam(value = "source", defaultValue = "normal") String source) {

        log.info("数据库执行请求: scriptId={}, projectId={}, tableId={}, year={}",
            scriptId, projectId, tableId, year);

        ExecuteDbRequest req = new ExecuteDbRequest(
            scriptId, projectId, tableId, divisionId,
            schoolId != null ? schoolId : 0L, year, source);
        return executionService.executeFromDb(req).toMap();
    }
}
```

### 5.2 ScriptController（新建）

```java
@RestController
@RequestMapping("/api/scripts")
public class ScriptController {
    @Autowired private ScriptService scriptService;

    @GetMapping("/{id}")           // 获取脚本内容
    @PostMapping                   // 上传新脚本（解析 + 持久化）
    @GetMapping("/{id}/rules")     // 查看解析出的规则列表（含分类）
    @GetMapping("/{id}/preview")   // 获取 javaPreview
    @GetMapping("/{id}/unsupported") // 不支持的语句列表
}
```

### 5.3 VersionController（新建）

```java
@RestController
@RequestMapping("/api/versions")
public class VersionController {
    @Autowired private VersionService versionService;

    @PostMapping("/{scriptId}/publish")   // 发布版本
    @GetMapping("/{scriptId}/list")       // 版本历史
    @GetMapping("/{scriptId}/active")     // 当前激活版本
}
```

### 5.4 GlobalExceptionHandler（新建）

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(IllegalArgumentException e) {
        return Map.of("code", 400, "msg", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleInternal(Exception e) {
        log.error("未处理异常", e);
        return Map.of("code", 500, "msg", "服务器内部错误");
        // 不再暴露 stack trace
    }
}
```

---

## 6. 硬编码消除

### 6.1 AnswerTableType 枚举

```java
public enum AnswerTableType {
    USER_ANSWER(EnumSet.of(1L, 2L, 10L)),         // bus_user_answer
    DOCTOR_ANSWER(EnumSet.of(3L, 4L, 5L)),         // bus_doctor_answer
    STUDENT_ANSWER(EnumSet.of(6L, 7L, 8L));         // bus_student_answer

    private final Set<Long> tableIds;

    public static AnswerTableType fromTableId(long tableId) { ... }
    public String answerTableName(String source) { ... }
    public boolean hasTimesColumn() { ... }
    public boolean isUserAnswer() { ... }
}
```

替代 `DbRuleExecutionDataLoader` 中所有 `isTableOne()`/`isTableTwo()`/`isTableThree()` 硬编码方法。

### 6.2 V2Runner 去硬编码

当前：
```java
// V2Runner.java
Path mappingPath = Paths.get("docs/sources/sql/bus_question_202606081429.json");
Path studentPath = Paths.get("docs/sources/data/学生证件类型证件号.json");
```

改为：
```java
// V2Runner.java
String mappingPath = System.getProperty("mappingPath",
    "docs/sources/sql/bus_question_202606081429.json");
String studentPath = System.getProperty("studentPath",
    "docs/sources/data/学生证件类型证件号.json");
```

### 6.3 日志输出替代 printf

```java
// 当前 V2Runner
System.out.printf("%n─── %s ───%n", r.spsName);

// 改为
log.info("─── {} ───", r.spsName);
```

---

## 7. 安全与配置修复

### 7.1 数据库密码

```yaml
# application.yml
spring:
  datasource:
    password: ${DB_PASSWORD:123456}  # 默认值仅用于本地开发
```

### 7.2 .gitattributes

```
* text=auto eol=lf
*.java text eol=lf
*.sps text eol=lf
*.sql text eol=lf
*.json text eol=lf
*.csv text eol=lf
*.yml text eol=lf
*.xml text eol=lf
*.html text eol=lf
*.js text eol=lf
*.md text eol=lf
```

---

## 8. 实施阶段

### 阶段 1：基础设施修复（预估 0.5 天）

**零风险，不改业务逻辑**

| # | 任务 | 文件 |
|---|---|---|
| 1.1 | 添加 `.gitattributes` | 新增 1 个文件 |
| 1.2 | 更新 `AGENTS.md` → 指向 `GLM.md` | 编辑 1 个文件 |
| 1.3 | 密码环境变量化 | `application.yml` 改 1 行 |
| 1.4 | 5 个核心类添加 Logger | 各加 1 行 `private static final Logger` |
| 1.5 | 新建 `GlobalExceptionHandler` | 新增 1 个类 |
| 1.6 | `AnswerTableType` 枚举 | 新增 1 个类 |

**验证**：`mvn -q -DskipTests package` 编译通过

### 阶段 2：引擎合并（预估 2-3 天）

**核心改造**

| # | 任务 | 说明 |
|---|---|---|
| 2.1 | 新建 `engine/model/` — `Rule`, `Step`, `StepAction`, `SegmentInfo` | 数据模型层 |
| 2.2 | 实现 `ConditionStack` 条件栈 | 核心新算法，含单元测试 |
| 2.3 | 重构 `RuleParser` 使用 `ConditionStack` | 基于 v1 `SpssRuleParser` 改写 |
| 2.4 | 迁移 `BlockClassifier` | 从 v2 `BlockParser.classify*` 提取分类逻辑 |
| 2.5 | 迁移 `RuleExecutor` | 从 v1 `RuleEngine` 改写，增加预编译 AST |
| 2.6 | 迁移 `DatasetRule`、`OutputRule`、`RecodeCase`、`RuleType` | 从 v1/v2 挪到 engine |
| 2.7 | 预编译 `ArithmeticExpression` AST | 新增 `compile()` + `CompiledExpression` |
| 2.8 | Web 层改引用 `engine.SpssParser` | 替换所有 `v1.SpssRuleParser` 引用 |
| 2.9 | 删除 `v1/`、`v2/` 包 | 确认无编译引用后删除 |

**验证**：
- `mvn test` 全量通过
- `PrototypeCli` 对比 `docs/table1-3-verify.txt` 输出一致
- 运行 `V2Runner` 覆盖全部 13 个 .sps 文件

### 阶段 3：服务层 + 表达式预编译（预估 1-2 天）

| # | 任务 |
|---|---|
| 3.1 | 新建 `ScriptService` |
| 3.2 | 新建 `ExecutionService`（编排流水线） |
| 3.3 | 新建 `VersionService`（DRAFT→PUBLISHED） |
| 3.4 | 改造 `PersistenceService`（`@Transactional` + Logger） |
| 3.5 | `ArithmeticExpression.compile()` 预编译 AST |

**验证**：单元测试 + Controller 注入 Service 后端点测试

### 阶段 4：Controller 拆分（预估 1 天）

| # | 任务 |
|---|---|
| 4.1 | 新建 `ExecuteController` |
| 4.2 | 新建 `ScriptController` |
| 4.3 | 新建 `VersionController` |
| 4.4 | 删除 `RuleExecuteV2Controller`、`RuleControllerV2` |
| 4.5 | Controller 中所有 `e.printStackTrace()` 替换为 `log.error/warn` |

**验证**：`curl` 调用各端点对比原返回值

### 阶段 5：清理与验证（预估 1 天）

| # | 任务 |
|---|---|
| 5.1 | `DbRuleExecutionDataLoader` 使用 `AnswerTableType` |
| 5.2 | `V2Runner` 命令行参数化 |
| 5.3 | 删除 `executor/`、`legacy/` 空目录 |
| 5.4 | `BlockParser` 中 `KNOWN_INTERMEDIATE` 移到配置文件 |
| 5.5 | 全量 `mvn test` + 13 个 .sps 文件回归测试 |
| 5.6 | 更新 `GLM.md` 反映新架构 |

### 总时间估算

```
阶段1: ■■■■□□□□□□  0.5 天（独立，可立即开始）
阶段2: ■■■■■■■■□□  2-3 天（核心）
阶段3: ■■■■■■□□□□  1-2 天（依赖阶段2）
阶段4: ■■■■□□□□□□  1 天（依赖阶段3）
阶段5: ■■■■□□□□□□  1 天（依赖阶段4）
────────────────────
总计: 5.5-7.5 个工作日（单人）
```

---

## 9. 风险与缓解措施

| 风险 | 概率 | 影响 | 缓解 |
|---|---|---|---|
| `ConditionStack` 算法遗漏边界情况 | 中 | 高 | 阶段 2 先写完整的单元测试（覆盖全部 13 个 .sps 文件） |
| 删除 v1/v2 后遗漏引用 | 低 | 中 | IDE 自动检查 + `mvn compile` 编译验证 |
| 预编译 AST 引入回归 | 低 | 中 | `PrototypeCli` 对比 `table1-3-verify.txt` |
| 版本生命周期破坏已有功能 | 低 | 低 | 新功能，不影响现有执行路径 |

---

## 10. 验证清单

改造完成后逐项验证：

- [ ] `mvn -q -DskipTests package` 编译通过
- [ ] `mvn test` 全量通过
- [ ] `PrototypeCli` 输出与 `docs/table1-3-verify.txt` 一致
- [ ] `V2Runner` 覆盖 13 个 .sps 文件无异常
- [ ] `curl /api/execute/upload` 返回格式与改造前一致
- [ ] `curl /api/execute/db` 返回格式与改造前一致
- [ ] `curl /api/scripts/{id}/rules` 正确返回规则列表（含分类）
- [ ] `curl /api/versions/{id}/publish` 正确创建版本
- [ ] 日志中不再出现 `e.printStackTrace()` 原始堆栈
- [ ] 错误响应中不再包含 `trace` 字段
- [ ] `ConditionalRuleStep` 类已删除
- [ ] 无反射调用 `Field.setAccessible(true)` 残留
- [ ] v1/v2 包已删除
- [ ] executor/、legacy/ 空目录已删除
- [ ] .gitattributes 已添加
