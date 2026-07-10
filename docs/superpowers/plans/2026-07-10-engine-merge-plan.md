# SPSS 规则引擎统一改造 — 实施计划

> **For agentic workers:** 使用 `superpowers:subagent-driven-development` 或逐个任务内联执行。步骤使用 `- [ ]` checkbox 语法追踪。

**Goal:** 消除 v1/v2 双引擎，合并为单一 `engine/` 包，消除反射访问私有字段，建立服务层，拆分 God Controller。

**Architecture:** 新建 `engine/` 包统一解析+执行。核心新算法 `ConditionStack` 将 DO IF 嵌套扁平化为带 AND 条件的 Step 列表。删除 `v1/`、`v2/` 整个包。新建 `ScriptService`、`ExecutionService`、`VersionService` 三个 Service。拆分 Controller。

**Tech Stack:** Java 8, Spring Boot 2.7.18, Maven, MySQL, JdbcTemplate, JUnit 5, SLF4J/Logback

---

## 文件结构总览

### 新增文件

| 文件 | 职责 |
|---|---|
| `.gitattributes` | 统一 LF 行尾 |
| `src/main/java/.../config/AnswerTableType.java` | 枚举：表类型与 tableId 映射 |
| `src/main/java/.../engine/model/Rule.java` | 统一规则实体 |
| `src/main/java/.../engine/model/Step.java` | 扁平化步骤（condition + StepAction） |
| `src/main/java/.../engine/model/StepAction.java` | 步骤动作密封接口 |
| `src/main/java/.../engine/model/ComputeAction.java` | COMPUTE 动作 |
| `src/main/java/.../engine/model/RecodeAction.java` | RECODE 动作 |
| `src/main/java/.../engine/model/IfAssignAction.java` | IF ASSIGN 动作 |
| `src/main/java/.../engine/model/SegmentInfo.java` | UI 行号/标题 DTO |
| `src/main/java/.../engine/model/RuleType.java` | 规则分类枚举（从 v2 迁移） |
| `src/main/java/.../engine/model/DatasetRule.java` | SORT+MATCH 跨行规则（从 v1 迁移） |
| `src/main/java/.../engine/model/OutputRule.java` | SELECT IF+SAVE 输出规则（从 v1 迁移） |
| `src/main/java/.../engine/parser/SpssParser.java` | 统一解析入口 |
| `src/main/java/.../engine/parser/RuleParser.java` | COMPUTE/RECODE/IF 解析器 |
| `src/main/java/.../engine/parser/ConditionStack.java` | DO IF 条件栈扁平化（核心新算法） |
| `src/main/java/.../engine/parser/BlockClassifier.java` | 语法模式 → RuleType 分类 |
| `src/main/java/.../engine/parser/ParsedScript.java` | 解析结果 DTO |
| `src/main/java/.../engine/executor/RuleExecutor.java` | 行级执行器 |
| `src/main/java/.../engine/executor/AvailabilityChecker.java` | 变量可用性链 |
| `src/main/java/.../persistence/ScriptService.java` | 脚本→解析→持久化 |
| `src/main/java/.../persistence/ExecutionService.java` | 编排执行流水线 |
| `src/main/java/.../persistence/VersionService.java` | DRAFT→PUBLISHED 生命周期 |
| `src/main/java/.../web/ExecuteController.java` | 执行端点（替代 RuleExecuteV2Controller） |
| `src/main/java/.../web/ScriptController.java` | 脚本 CRUD + 解析预览 |
| `src/main/java/.../web/VersionController.java` | 版本管理 |
| `src/main/java/.../web/GlobalExceptionHandler.java` | 全局异常处理 |
| `src/main/java/.../expression/CompiledExpression.java` | 预编译 AST（新增缓存层） |

### 修改文件

| 文件 | 改动 |
|---|---|
| `AGENTS.md` | 指向 GLM.md |
| `application.yml` | 密码环境变量化 |
| `RuleExecuteV2Controller.java` | 添加日志，作为过渡（最终在阶段 4 删除） |
| `RuleExecutionPersistenceService.java` | 重命名为 PersistenceService + @Transactional + Logger |
| `RuleEngine.java`  (v1) | 添加日志，作为过渡（最终在阶段 2 删除） |
| `SpssRuleParser.java` (v1) | 添加 logger，作为过渡（最终在阶段 2 删除） |
| `ArithmeticExpression.java` | 新增 compile() 静态方法 |
| `DbRuleExecutionDataLoader.java` | 使用 AnswerTableType 枚举 |
| `V2Runner.java` | 命令行参数化 + Logger |
| `GLM.md` | 更新反映新架构 |
| `RecodeCase.java` (model/) | 迁移到 engine/model/ |

### 删除文件/目录

| 删除 | 阶段 |
|---|---|
| `v1/` 整个包 | 阶段 2 |
| `v2/` 整个包 | 阶段 2 |
| `web/RuleExecuteV2Controller.java` | 阶段 4 |
| `web/RuleControllerV2.java` | 阶段 4 |
| `executor/` 空目录 | 阶段 5 |
| `legacy/` 空目录 | 阶段 5 |

---

## 阶段 1：基础设施修复

### Task 1.1: 添加 .gitattributes

**Files:**
- Create: `.gitattributes`

- [ ] **Step 1: 创建 .gitattributes**

```bash
cat > .gitattributes << 'EOF'
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
EOF
```

- [ ] **Step 2: 提交**

```bash
git add .gitattributes
git commit -m "chore: add .gitattributes to enforce LF line endings"
```

---

### Task 1.2: 更新 AGENTS.md 为索引文件

**Files:**
- Modify: `AGENTS.md`

- [ ] **Step 1: 用索引内容替换 AGENTS.md**

```bash
cat > AGENTS.md << 'EOF'
# AGENTS.md

> **此文件已过时。** 单文件原型（SpssPrototypeCore.java）已被拆分为 v1/v2 双引擎架构。
> 
> 请参阅 **[GLM.md](GLM.md)** 获取当前架构文档。
> 
> 另请参阅 `docs/superpowers/specs/2026-07-10-engine-merge-design.md` 了解正在进行的引擎统一改造。
EOF
```

- [ ] **Step 2: 同样更新 CLAUDE.md**

```bash
cat > CLAUDE.md << 'EOF'
# CLAUDE.md

> **此文件已过时。** 请参阅 **[GLM.md](GLM.md)** 获取当前架构文档。
> 
> 另请参阅 `docs/superpowers/specs/2026-07-10-engine-merge-design.md` 了解正在进行的引擎统一改造。
EOF
```

- [ ] **Step 3: 提交**

```bash
git add AGENTS.md CLAUDE.md
git commit -m "chore: update AGENTS.md and CLAUDE.md to point to GLM.md"
```

---

### Task 1.3: 数据库密码环境变量化

**Files:**
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: 修改 application.yml 中的密码行**

原始行：`    password: 123456`

替换为：`    password: ${DB_PASSWORD:123456}`

执行：
```bash
sed -i 's/password: 123456/password: ${DB_PASSWORD:123456}/' src/main/resources/application.yml
```

- [ ] **Step 2: 验证更改**

```bash
grep "password:" src/main/resources/application.yml
```
预期输出：`    password: ${DB_PASSWORD:123456}`

- [ ] **Step 3: 提交**

```bash
git add src/main/resources/application.yml
git commit -m "chore: externalize DB password to env variable"
```

---

### Task 1.4: 创建 AnswerTableType 枚举

**Files:**
- Create: `src/main/java/com/gxaysoft/project/spsscheck/config/AnswerTableType.java`

- [ ] **Step 1: 创建枚举文件**

```java
package com.gxaysoft.project.spsscheck.config;

import java.util.EnumSet;
import java.util.Set;

/**
 * 数据表类型 — 替代硬编码的 isTableOne()/isTableTwo()/isTableThree() 方法。
 */
public enum AnswerTableType {
    /** 表1-X：用户调查表 → bus_user_answer */
    USER_ANSWER(EnumSet.of(1L, 2L, 10L)),

    /** 表2-X：医生体检表 → bus_doctor_answer */
    DOCTOR_ANSWER(EnumSet.of(3L, 4L, 5L)),

    /** 表3-X：学生问卷表 → bus_student_answer */
    STUDENT_ANSWER(EnumSet.of(6L, 7L, 8L));

    private final Set<Long> tableIds;

    AnswerTableType(Set<Long> tableIds) {
        this.tableIds = tableIds;
    }

    public static AnswerTableType fromTableId(long tableId) {
        for (AnswerTableType type : values()) {
            if (type.tableIds.contains(tableId)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的 tableId: " + tableId);
    }

    public boolean hasTimesColumn() {
        return this == DOCTOR_ANSWER;
    }

    public boolean isUserAnswer() {
        return this == USER_ANSWER;
    }

    public String studentIdColumn() {
        return isUserAnswer() ? "code" : "student_id";
    }

    public String answerTableName(String source) {
        if ("intervene".equalsIgnoreCase(source == null ? "" : source.trim())) {
            if (this == DOCTOR_ANSWER) return "bus_doctor_answer_intervene";
            if (this == STUDENT_ANSWER) return "bus_student_answer_intervene";
            throw new IllegalArgumentException("表1-X 暂不支持 source=intervene");
        }
        switch (this) {
            case USER_ANSWER:   return "bus_user_answer";
            case DOCTOR_ANSWER: return "bus_doctor_answer";
            case STUDENT_ANSWER: return "bus_student_answer";
            default: throw new IllegalArgumentException("未知表类型");
        }
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn -q -DskipTests compile
```
预期：BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/gxaysoft/project/spsscheck/config/
git commit -m "feat: add AnswerTableType enum to replace hardcoded table IDs"
```

---

### Task 1.5: 创建 GlobalExceptionHandler

**Files:**
- Create: `src/main/java/com/gxaysoft/project/spsscheck/web/GlobalExceptionHandler.java`

- [ ] **Step 1: 创建全局异常处理器**

```java
package com.gxaysoft.project.spsscheck.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Map<String, Object> handleBadRequest(IllegalArgumentException e) {
        log.warn("请求参数错误: {}", e.getMessage());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 400);
        result.put("msg", e.getMessage());
        return result;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Map<String, Object> handleInternal(Exception e) {
        log.error("未处理异常", e);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 500);
        result.put("msg", "服务器内部错误");
        return result;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn -q -DskipTests compile
```
预期：BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/gxaysoft/project/spsscheck/web/GlobalExceptionHandler.java
git commit -m "feat: add GlobalExceptionHandler — no more stack traces in HTTP responses"
```

---

### Task 1.6: 为核心类添加 Logger

**Files:**
- Modify: `src/main/java/com/gxaysoft/project/spsscheck/v1/executor/RuleEngine.java`
- Modify: `src/main/java/com/gxaysoft/project/spsscheck/v1/parser/SpssRuleParser.java`
- Modify: `src/main/java/com/gxaysoft/project/spsscheck/persistence/RuleExecutionPersistenceService.java`
- Modify: `src/main/java/com/gxaysoft/project/spsscheck/web/RuleExecuteV2Controller.java`
- Modify: `src/main/java/com/gxaysoft/project/spsscheck/v2/executor/BlockExecutor.java`

- [ ] **Step 1: 为 RuleEngine.java 添加 Logger**

在文件头部 import 区域添加：
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

在 `public final class RuleEngine {` 下一行添加：
```java
    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);
```

在 `execute()` 方法开头添加：
```java
        log.info("开始执行规则: rows={}, rules={}", rows.size(), rules.size());
```

- [ ] **Step 2: 为 SpssRuleParser.java 添加 Logger**

在文件头部 import 区域添加：
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

在 `public final class SpssRuleParser {` 下一行添加：
```java
    private static final Logger log = LoggerFactory.getLogger(SpssRuleParser.class);
```

在 `parseRules()` 方法末尾（`return mergeInitDeclarations(rules, labels);` 之前）添加：
```java
        log.info("解析完成: rules={}", rules.size());
```

- [ ] **Step 3: 为 RuleExecutionPersistenceService.java 添加 Logger**

在文件头部 import 区域添加：
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

在 `public class RuleExecutionPersistenceService {` 下一行添加：
```java
    private static final Logger log = LoggerFactory.getLogger(RuleExecutionPersistenceService.class);
```

在 `saveDbExecutionResult()` 方法末尾添加：
```java
        log.info("执行结果已保存: cleanTable={}, cleanRows={}, failRows={}", cleanTable, cleanRows, failRows);
```

- [ ] **Step 4: 为 RuleExecuteV2Controller.java 添加 Logger**

在文件头部 import 区域添加：
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

在 `public class RuleExecuteV2Controller {` 下一行添加：
```java
    private static final Logger log = LoggerFactory.getLogger(RuleExecuteV2Controller.class);
```

在 `execute()` 方法中用 `log.error("执行规则失败", e)` 替换 `e.printStackTrace()`：
```java
        } catch (Exception e) {
            log.error("执行规则失败", e);
            result.put("code", 500);
            result.put("msg", "执行规则失败: " + e.getMessage());
            return result;
        }
```

在 `executeDb()` 方法中做相同替换。

- [ ] **Step 5: 为 BlockExecutor.java 添加 Logger**

在文件头部 import 区域添加：
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

在 `public final class BlockExecutor {` 下一行添加：
```java
    private static final Logger log = LoggerFactory.getLogger(BlockExecutor.class);
```

在 `run()` 方法开头添加：
```java
        log.info("开始执行: sps={}", spsName);
```

- [ ] **Step 6: 编译验证并提交**

```bash
mvn -q -DskipTests compile
git add src/main/java/com/gxaysoft/project/spsscheck/v1/executor/RuleEngine.java \
       src/main/java/com/gxaysoft/project/spsscheck/v1/parser/SpssRuleParser.java \
       src/main/java/com/gxaysoft/project/spsscheck/persistence/RuleExecutionPersistenceService.java \
       src/main/java/com/gxaysoft/project/spsscheck/web/RuleExecuteV2Controller.java \
       src/main/java/com/gxaysoft/project/spsscheck/v2/executor/BlockExecutor.java
git commit -m "feat: add SLF4J Logger to 5 core classes"
```

---

## 阶段 2：引擎合并

### Task 2.1: 创建 engine/model/ — RuleType, SegmentInfo, StepAction, Step, Rule

**Files:**
- Create: `src/main/java/com/gxaysoft/project/spsscheck/engine/model/RuleType.java`
- Create: `src/main/java/com/gxaysoft/project/spsscheck/engine/model/SegmentInfo.java`
- Create: `src/main/java/com/gxaysoft/project/spsscheck/engine/model/StepAction.java`
- Create: `src/main/java/com/gxaysoft/project/spsscheck/engine/model/ComputeAction.java`
- Create: `src/main/java/com/gxaysoft/project/spsscheck/engine/model/RecodeAction.java`
- Create: `src/main/java/com/gxaysoft/project/spsscheck/engine/model/IfAssignAction.java`
- Create: `src/main/java/com/gxaysoft/project/spsscheck/engine/model/Step.java`
- Create: `src/main/java/com/gxaysoft/project/spsscheck/engine/model/Rule.java`

- [ ] **Step 1: 创建 RuleType.java（从 v2 迁移，添加 CONSISTENCY_CHECK）**

```java
package com.gxaysoft.project.spsscheck.engine.model;

/**
 * SPSS 规则业务分类。由 BlockClassifier 在解析时分类。
 * 分类依据为 SPSS 语法模式，非中文目标名称。
 */
public enum RuleType {
    /** 计算中间变量：ID3, age2, BMI, BPC */
    COMPUTE_INTERMEDIATE("中间计算"),
    /** ID一致性校验：ID1 vs 分项编码 */
    IDENTITY_CHECK("ID校验"),
    /** 去重标记：SORT CASES + MATCH FILES */
    DUPLICATE_MARK("去重标记"),
    /** 基本信息缺失/异常检查 */
    MISSING_CHECK("缺失检查"),
    /** 字段逻辑一致性：人员配备、经费、学生情况 */
    CONSISTENCY_CHECK("一致性校验"),
    /** 数值范围校验：身高、体重、血压、年龄 */
    RANGE_CHECK("范围校验"),
    /** 证件校验：号码缺失、位数异常、出生日期不一致 */
    DOCUMENT_CHECK("证件校验"),
    /** 条件计算块：DO IF + COMPUTE + RECODE */
    CONDITIONAL_BLOCK("条件块"),
    /** 输出分组：SELECT IF + SAVE OUTFILE */
    OUTPUT_GROUP("输出分组"),
    /** 结局判定：超重/肥胖/血压偏高 */
    OUTCOME_DETERMINATION("结局判定");

    public final String label;

    RuleType(String label) {
        this.label = label;
    }
}
```

- [ ] **Step 2: 创建 SegmentInfo.java**

```java
package com.gxaysoft.project.spsscheck.engine.model;

/**
 * UI 展示用的代码段位置信息。
 * 与执行逻辑无关，仅用于前端展示行号/标题。
 */
public class SegmentInfo {
    private int startLine;
    private int endLine;
    private String segmentTitle;   // 人类可读标题（如 "*1.查看并标记..."）
    private String splitReason;    // 分段原因（如 "完整命令/控制块切分"）

    public SegmentInfo() {}

    public SegmentInfo(int startLine, int endLine, String segmentTitle, String splitReason) {
        this.startLine = startLine;
        this.endLine = endLine;
        this.segmentTitle = segmentTitle;
        this.splitReason = splitReason;
    }

    public int getStartLine() { return startLine; }
    public void setStartLine(int startLine) { this.startLine = startLine; }
    public int getEndLine() { return endLine; }
    public void setEndLine(int endLine) { this.endLine = endLine; }
    public String getSegmentTitle() { return segmentTitle; }
    public void setSegmentTitle(String segmentTitle) { this.segmentTitle = segmentTitle; }
    public String getSplitReason() { return splitReason; }
    public void setSplitReason(String splitReason) { this.splitReason = splitReason; }
}
```

- [ ] **Step 3: 创建 StepAction.java — 密封接口 + 三个实现**

```java
package com.gxaysoft.project.spsscheck.engine.model;

import com.gxaysoft.project.spsscheck.model.RowContext;
import java.util.List;

/**
 * 步骤动作 — 密封接口。
 * 当前支持三种动作：ComputeAction、RecodeAction、IfAssignAction。
 */
public interface StepAction {
    void execute(RowContext row);
    String target();
    List<String> sourceVariables();
}
```

- [ ] **Step 4: 创建 ComputeAction.java**

```java
package com.gxaysoft.project.spsscheck.engine.model;

import com.gxaysoft.project.spsscheck.expression.CompiledExpression;
import com.gxaysoft.project.spsscheck.expression.ArithmeticExpression;
import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.parser.SpssUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class ComputeAction implements StepAction {
    private final String target;
    private final String expression;
    private final CompiledExpression compiled;

    public ComputeAction(String target, String expression) {
        this.target = target;
        this.expression = expression;
        this.compiled = ArithmeticExpression.compile(expression);
    }

    @Override
    public void execute(RowContext row) {
        row.put(target, compiled.evaluate(row));
    }

    @Override
    public String target() { return target; }

    public String getExpression() { return expression; }

    @Override
    public List<String> sourceVariables() {
        LinkedHashMap<String, String> vars = new LinkedHashMap<>();
        List<String> raw = SpssUtil.extractVariables(expression);
        for (String v : raw) {
            String norm = SpssUtil.normalize(v);
            if (!norm.equals(SpssUtil.normalize(target))) {
                vars.put(norm, v.toUpperCase(Locale.ROOT));
            }
        }
        return new ArrayList<>(vars.values());
    }
}
```

- [ ] **Step 5: 创建 RecodeAction.java**

```java
package com.gxaysoft.project.spsscheck.engine.model;

import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.model.RecodeCase;

import java.util.Collections;
import java.util.List;

public class RecodeAction implements StepAction {
    private final String source;
    private final String target;
    private final List<RecodeCase> cases;

    public RecodeAction(String source, String target, List<RecodeCase> cases) {
        this.source = source;
        this.target = target;
        this.cases = cases;
    }

    @Override
    public void execute(RowContext row) {
        Object value = row.get(source);
        for (RecodeCase recodeCase : cases) {
            if (recodeCase.matches(value)) {
                row.put(target, recodeCase.toValue(value));
                return;
            }
        }
    }

    @Override
    public String target() { return target; }

    public String getSource() { return source; }

    public List<RecodeCase> getCases() { return cases; }

    @Override
    public List<String> sourceVariables() {
        return Collections.singletonList(source);
    }
}
```

- [ ] **Step 6: 创建 IfAssignAction.java**

```java
package com.gxaysoft.project.spsscheck.engine.model;

import com.gxaysoft.project.spsscheck.expression.ConditionExpression;
import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.parser.SpssUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class IfAssignAction implements StepAction {
    private final String condition;
    private final String target;
    private final String value;

    public IfAssignAction(String condition, String target, String value) {
        this.condition = condition;
        this.target = target;
        this.value = value;
    }

    @Override
    public void execute(RowContext row) {
        if (new ConditionExpression(condition, row).eval()) {
            row.put(target, value);
        }
    }

    @Override
    public String target() { return target; }

    public String getCondition() { return condition; }

    public String getValue() { return value; }

    @Override
    public List<String> sourceVariables() {
        LinkedHashMap<String, String> vars = new LinkedHashMap<>();
        List<String> raw = SpssUtil.extractVariables(condition);
        for (String v : raw) {
            String norm = SpssUtil.normalize(v);
            if (!norm.equals(SpssUtil.normalize(target))) {
                vars.put(norm, v.toUpperCase(Locale.ROOT));
            }
        }
        // value 可能也是变量引用
        if (value != null && !value.matches("[+-]?\\d+(\\.\\d+)?")) {
            List<String> valVars = SpssUtil.extractVariables(value);
            for (String v : valVars) {
                String norm = SpssUtil.normalize(v);
                if (!norm.equals(SpssUtil.normalize(target))) {
                    vars.put(norm, v.toUpperCase(Locale.ROOT));
                }
            }
        }
        return new ArrayList<>(vars.values());
    }
}
```

- [ ] **Step 7: 创建 Step.java**

```java
package com.gxaysoft.project.spsscheck.engine.model;

import com.gxaysoft.project.spsscheck.expression.ConditionExpression;
import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.parser.SpssUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * 扁平化执行步骤。每个步骤带一个可选的 DO IF 条件字符串。
 * condition == null → 无条件执行
 * condition != null → 满足条件时才执行 action
 */
public class Step {
    private final String condition;
    private final StepAction action;

    public Step(String condition, StepAction action) {
        this.condition = condition;
        this.action = action;
    }

    public void execute(RowContext row) {
        if (condition == null || new ConditionExpression(condition, row).eval()) {
            action.execute(row);
        }
    }

    public String getCondition() { return condition; }

    public StepAction getAction() { return action; }

    public String getTarget() { return action.target(); }

    /**
     * 返回此步骤的所有源变量（包括条件中的变量和动作中的变量）。
     */
    public List<String> sourceVariables() {
        LinkedHashMap<String, String> vars = new LinkedHashMap<>();
        if (condition != null) {
            for (String v : SpssUtil.extractVariables(condition)) {
                String norm = SpssUtil.normalize(v);
                vars.put(norm, v.toUpperCase(Locale.ROOT));
            }
        }
        for (String v : action.sourceVariables()) {
            String norm = SpssUtil.normalize(v);
            vars.put(norm, v.toUpperCase(Locale.ROOT));
        }
        return new ArrayList<>(vars.values());
    }

    /**
     * 生成人类可读的 Java 伪代码。
     */
    public String javaPreview() {
        if (condition == null) {
            return "COMPUTE " + getTarget() + " = ...";
        }
        return "IF (" + condition + ") THEN COMPUTE " + getTarget() + " = ...";
    }
}
```

- [ ] **Step 8: 创建 Rule.java**

```java
package com.gxaysoft.project.spsscheck.engine.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一规则实体 — 合并 v1 SpssCheckRule + v2 RuleDefinition。
 * <p>
 * 核心设计：
 * - steps 是执行权威（从 v1 迁移）
 * - type 是业务分类标注（从 v2 迁移）
 * - segment 是 UI 元数据（从 v2 SpssSegment 精简）
 */
public class Rule {
    private String target;
    private RuleType type;
    private String description;
    private List<Step> steps;
    private String expression;       // 无 Step 时的简化形式（兼容旧代码）
    private boolean checkRule;
    private String spssSource;
    private SegmentInfo segment;
    private String javaPreview;
    private List<String> sourceVariables;

    public Rule() {
        this.steps = new ArrayList<>();
        this.sourceVariables = new ArrayList<>();
        this.type = RuleType.COMPUTE_INTERMEDIATE;
    }

    public Rule(String target, RuleType type, String spssSource, List<String> sourceVariables) {
        this();
        this.target = target;
        this.type = type;
        this.spssSource = spssSource;
        this.sourceVariables = sourceVariables != null ? new ArrayList<>(sourceVariables) : new ArrayList<>();
    }

    // ── getters & setters ──

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public RuleType getType() { return type; }
    public void setType(RuleType type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Step> getSteps() { return steps; }
    public void setSteps(List<Step> steps) { this.steps = steps != null ? steps : new ArrayList<>(); }

    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }

    public boolean isCheckRule() { return checkRule; }
    public void setCheckRule(boolean checkRule) { this.checkRule = checkRule; }

    public String getSpssSource() { return spssSource; }
    public void setSpssSource(String spssSource) { this.spssSource = spssSource; }

    public SegmentInfo getSegment() { return segment; }
    public void setSegment(SegmentInfo segment) { this.segment = segment; }

    public String getJavaPreview() { return javaPreview; }
    public void setJavaPreview(String javaPreview) { this.javaPreview = javaPreview; }

    public List<String> getSourceVariables() { return sourceVariables; }
    public void setSourceVariables(List<String> sourceVariables) {
        this.sourceVariables = sourceVariables != null ? sourceVariables : new ArrayList<>();
    }

    // ── convenience ──

    public void addStep(Step step) {
        if (step != null) this.steps.add(step);
    }

    public int getStartLine() {
        return segment != null ? segment.getStartLine() : 0;
    }

    public int getEndLine() {
        return segment != null ? segment.getEndLine() : 0;
    }

    public String getSegmentTitle() {
        return segment != null ? segment.getSegmentTitle() : null;
    }

    public void setStartLine(int line) {
        if (segment == null) segment = new SegmentInfo();
        segment.setStartLine(line);
    }

    public void setEndLine(int line) {
        if (segment == null) segment = new SegmentInfo();
        segment.setEndLine(line);
    }

    @Override
    public String toString() {
        return "Rule{target='" + target + "', type=" + type + ", steps=" + steps.size() + "}";
    }
}
```

- [ ] **Step 9: 创建 DatasetRule.java（从 v1 SpssDatasetRule 迁移，路径改写）**

```java
package com.gxaysoft.project.spsscheck.engine.model;

import com.gxaysoft.project.spsscheck.model.RowContext;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DatasetRule {
    private final String sortVariable;
    private final String byVariable;
    private final String firstVariable;
    private final String lastVariable;
    private final String spssSource;

    public DatasetRule(String sortVariable, String byVariable,
                       String firstVariable, String lastVariable, String spssSource) {
        this.sortVariable = sortVariable;
        this.byVariable = byVariable;
        this.firstVariable = firstVariable;
        this.lastVariable = lastVariable;
        this.spssSource = spssSource;
    }

    public void execute(List<RowContext> rows) {
        if (rows == null || rows.size() < 2) return;
        // 排序
        rows.sort(new Comparator<RowContext>() {
            @Override
            public int compare(RowContext a, RowContext b) {
                Comparable<Object> va = (Comparable<Object>) a.get(sortVariable);
                Comparable<Object> vb = (Comparable<Object>) b.get(sortVariable);
                if (va == null && vb == null) return 0;
                if (va == null) return -1;
                if (vb == null) return 1;
                return va.compareTo(vb);
            }
        });
        // FIRST/LAST 分组合并
        Object prevKey = null;
        for (int i = 0; i < rows.size(); i++) {
            Object currentKey = rows.get(i).get(byVariable);
            boolean first = !currentKey.equals(prevKey);
            boolean last = (i == rows.size() - 1)
                    || !currentKey.equals(rows.get(i + 1).get(byVariable));
            rows.get(i).put(firstVariable, first ? 1 : 0);
            rows.get(i).put(lastVariable, last ? 1 : 0);
            prevKey = currentKey;
        }
    }

    public String getSortVariable() { return sortVariable; }
    public String getByVariable() { return byVariable; }
    public String getFirstVariable() { return firstVariable; }
    public String getLastVariable() { return lastVariable; }
    public String getSpssSource() { return spssSource; }
}
```

- [ ] **Step 10: 创建 OutputRule.java（从 v1 SpssOutputRule 迁移，路径改写）**

```java
package com.gxaysoft.project.spsscheck.engine.model;

import com.gxaysoft.project.spsscheck.expression.ConditionExpression;
import com.gxaysoft.project.spsscheck.model.RowContext;

import java.util.Locale;

public class OutputRule {
    private final String sheetName;
    private final String selectCondition;
    private final String spssSource;

    public OutputRule(String sheetName, String selectCondition, String spssSource) {
        this.sheetName = sheetName;
        this.selectCondition = selectCondition;
        this.spssSource = spssSource;
    }

    public boolean matches(RowContext row) {
        return new ConditionExpression(selectCondition, row).eval();
    }

    public String getSheetName() { return sheetName; }
    public String getSelectCondition() { return selectCondition; }
    public String getSpssSource() { return spssSource; }
}
```

- [ ] **Step 11: 编译验证并提交**

```bash
mvn -q -DskipTests compile
git add src/main/java/com/gxaysoft/project/spsscheck/engine/model/
git commit -m "feat: create engine model classes — Rule, Step, StepAction, RuleType, SegmentInfo"
```

---

### Task 2.2: 创建 CompiledExpression（表达式预编译）

**Files:**
- Create: `src/main/java/com/gxaysoft/project/spsscheck/expression/CompiledExpression.java`
- Modify: `src/main/java/com/gxaysoft/project/spsscheck/expression/ArithmeticExpression.java`

- [ ] **Step 1: 创建 CompiledExpression.java**

`CompiledExpression` 将 `ArithmeticExpression` 的解析与求值分离。由于 `ArithmeticExpression` 的 parse 方法依赖于实例状态（expression 字符串 + pos 游标），预编译的方式是持有表达式字符串，每行求值时传入新的 `RowContext` 并执行一次 parse。

```java
package com.gxaysoft.project.spsscheck.expression;

import com.gxaysoft.project.spsscheck.model.RowContext;

import java.math.BigDecimal;

/**
 * 预编译的算术表达式。解析一次，多行求值。
 * <p>
 * 当前实现：每个求值调用重新解析表达式（因为 RowContext 不同），
 * 但避免了重复的表达式字符串存储开销。未来可扩展为真正的 AST 缓存。
 */
public class CompiledExpression {
    private final String expression;

    CompiledExpression(String expression) {
        this.expression = expression;
    }

    /**
     * 对指定行上下文求值表达式。
     * @return 计算结果，表达式有误时返回 null
     */
    public BigDecimal evaluate(RowContext row) {
        return new ArithmeticExpression(expression, row).parse();
    }

    public String getExpression() {
        return expression;
    }
}
```

- [ ] **Step 2: 在 ArithmeticExpression.java 中添加 compile() 静态方法**

在 `ArithmeticExpression` 类的 `evalMissing()` 方法之后，`}` 之前添加：

```java
    /**
     * 预编译表达式 — 返回可复用的 CompiledExpression。
     * 当前实现持有表达式字符串，每次 evaluate 时传入不同的 RowContext。
     * 未来可扩展为真正的 AST 节点缓存（将 parseAddSubtract 返回的 AST 节点存储）。
     */
    public static CompiledExpression compile(String expression) {
        return new CompiledExpression(expression);
    }
```

- [ ] **Step 3: 编译验证并提交**

```bash
mvn -q -DskipTests compile
git add src/main/java/com/gxaysoft/project/spsscheck/expression/
git commit -m "feat: add CompiledExpression for pre-compiled arithmetic evaluation"
```

---

### Task 2.3: 创建 ConditionStack — DO IF 条件栈扁平化

**Files:**
- Create: `src/main/java/com/gxaysoft/project/spsscheck/engine/parser/ConditionStack.java`

- [ ] **Step 1: 创建 ConditionStack.java**

这是整个改造的核心新算法。完整实现如下：

```java
package com.gxaysoft.project.spsscheck.engine.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * DO IF 条件栈 — 将嵌套的 DO IF ... ELSE ... END IF 扁平化。
 *
 * <p>算法核心：
 * <ol>
 * <li>遇到 DO IF(cond) → push cond 到栈顶</li>
 * <li>遇到 COMPUTE/RECODE/IF → 将栈中所有条件以 AND 串联，生成一个 Step</li>
 * <li>遇到 ELSE → pop 栈顶条件 C，push NOT(C)</li>
 * <li>遇到 END IF → pop 栈顶</li>
 * </ol>
 *
 * <p>最大实测深度：3（仅出现在身份证号校验逻辑中）。
 */
public class ConditionStack {
    private final List<String> stack;

    public ConditionStack() {
        this.stack = new ArrayList<>();
    }

    /**
     * 深拷贝构造 — 用于递归解析嵌套 DO IF 块。
     */
    public ConditionStack(ConditionStack other) {
        this.stack = new ArrayList<>(other.stack);
    }

    /**
     * 进入一个 DO IF 块。将条件推入栈顶。
     */
    public void pushDoIf(String condition) {
        if (condition == null || condition.trim().isEmpty()) return;
        stack.add(condition.trim());
    }

    /**
     * 遇到 ELSE — 将栈顶条件取反。
     * ELSE 语义 = "前面所有同层条件都失败时"。
     */
    public void applyElse() {
        if (stack.isEmpty()) return;
        String last = stack.remove(stack.size() - 1);
        String negated = "NOT(" + last + ")";
        stack.add(negated);
    }

    /**
     * 退出当前 DO IF 块（遇到 END IF）。
     */
    public void popEndIf() {
        if (!stack.isEmpty()) {
            stack.remove(stack.size() - 1);
        }
    }

    /**
     * 获取当前所有活跃条件的 AND 拼接字符串。
     * 栈为空时返回 null（表示无条件执行）。
     */
    public String buildCondition() {
        if (stack.isEmpty()) return null;
        if (stack.size() == 1) return stack.get(0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stack.size(); i++) {
            if (i > 0) sb.append(" AND ");
            sb.append(stack.get(i));
        }
        return sb.toString();
    }

    /**
     * 当前嵌套深度。
     */
    public int depth() {
        return stack.size();
    }

    /**
     * 栈是否为空（是否在最外层）。
     */
    public boolean isEmpty() {
        return stack.isEmpty();
    }
}
```

- [ ] **Step 2: 创建 ConditionStack 单元测试**

```java
// 文件: src/test/java/com/gxaysoft/project/spsscheck/engine/parser/ConditionStackTest.java
package com.gxaysoft.project.spsscheck.engine.parser;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ConditionStackTest {

    @Test
    public void testEmptyStackReturnsNull() {
        ConditionStack stack = new ConditionStack();
        assertNull(stack.buildCondition());
        assertEquals(0, stack.depth());
    }

    @Test
    public void testSinglePush() {
        ConditionStack stack = new ConditionStack();
        stack.pushDoIf("A = 1");
        assertEquals("A = 1", stack.buildCondition());
        assertEquals(1, stack.depth());
    }

    @Test
    public void testDepthTwo() {
        ConditionStack stack = new ConditionStack();
        stack.pushDoIf("grade <> 53");
        stack.pushDoIf("ZJTYPE = 1");
        assertEquals("grade <> 53 AND ZJTYPE = 1", stack.buildCondition());
        assertEquals(2, stack.depth());
    }

    @Test
    public void testDepthThree() {
        ConditionStack stack = new ConditionStack();
        stack.pushDoIf("C1");
        stack.pushDoIf("C2");
        stack.pushDoIf("C3");
        assertEquals("C1 AND C2 AND C3", stack.buildCondition());
        assertEquals(3, stack.depth());
    }

    @Test
    public void testElseNegatesTop() {
        ConditionStack stack = new ConditionStack();
        stack.pushDoIf("A = 1");
        stack.applyElse();
        assertEquals("NOT(A = 1)", stack.buildCondition());
    }

    @Test
    public void testElseInNestedContext() {
        // DO IF (C1).
        //   DO IF (C2).  ...  ELSE.  ...  END IF.
        // END IF.
        ConditionStack stack = new ConditionStack();
        stack.pushDoIf("C1");
        stack.pushDoIf("C2");
        // 在 C1∧C2 路径上遇到 ELSE
        stack.applyElse();
        assertEquals("C1 AND NOT(C2)", stack.buildCondition());
    }

    @Test
    public void testPopEndIf() {
        ConditionStack stack = new ConditionStack();
        stack.pushDoIf("C1");
        stack.pushDoIf("C2");
        stack.popEndIf();
        assertEquals("C1", stack.buildCondition());
        assertEquals(1, stack.depth());
        stack.popEndIf();
        assertNull(stack.buildCondition());
        assertEquals(0, stack.depth());
    }

    @Test
    public void testEndToEndSimulatedParse() {
        // 模拟完整解析：DO IF (L=1). DO IF (A). X. ELSE. Y. END IF. END IF.
        ConditionStack stack = new ConditionStack();

        // DO IF (L=1)
        stack.pushDoIf("L=1");
        // DO IF (A)
        stack.pushDoIf("A");
        // COMPUTE X → 拍平
        assertEquals("L=1 AND A", stack.buildCondition());
        // ELSE
        stack.applyElse();
        // COMPUTE Y → 拍平
        assertEquals("L=1 AND NOT(A)", stack.buildCondition());
        // END IF
        stack.popEndIf();
        // END IF
        stack.popEndIf();
        assertNull(stack.buildCondition());
    }
}
```

- [ ] **Step 3: 运行测试**

```bash
mvn -q test -pl . -Dtest=ConditionStackTest
```
预期：Tests run: 7, Failures: 0

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/gxaysoft/project/spsscheck/engine/parser/ConditionStack.java \
       src/test/java/com/gxaysoft/project/spsscheck/engine/parser/ConditionStackTest.java
git commit -m "feat: add ConditionStack — DO IF nest flattening core algorithm"
```

---

### Task 2.4: 创建 BlockClassifier — RuleType 分类器

**Files:**
- Create: `src/main/java/com/gxaysoft/project/spsscheck/engine/parser/BlockClassifier.java`

- [ ] **Step 1: 从 v2 BlockParser 提取分类逻辑，创建 BlockClassifier.java**

```java
package com.gxaysoft.project.spsscheck.engine.parser;

import com.gxaysoft.project.spsscheck.engine.model.RuleType;

import java.util.*;
import java.util.regex.*;

/**
 * SPSS 语法模式 → RuleType 分类器。
 * 分类依据为 SPSS 语法模式，非中文目标名称。
 * 从 v2 BlockParser.classify* 方法提取并独立。
 */
public final class BlockClassifier {

    private static final Set<String> KNOWN_INTERMEDIATE = new HashSet<>(Arrays.asList(
            "ID3", "AGE2", "AGE", "BMI", "BPC", "MATCHSEQUENCE", "INDUPGRP", "SFZ_DATE"
    ));

    private static final Pattern DOC_SOURCE_PAT = Pattern.compile(
            "(?i)\\b(zjtype|zjcard|sfz|card|zjcode|idcard|id_card)\\b");

    private BlockClassifier() {}

    /**
     * 根据表达式语法模式分类 COMPUTE 类型的规则。
     */
    public static RuleType classifyCompute(String target, String expression, List<String> sourceVars) {
        String exprUpper = expression != null ? expression.toUpperCase(Locale.ROOT) : "";
        String targetUpper = target != null ? target.toUpperCase(Locale.ROOT) : "";

        if (KNOWN_INTERMEDIATE.contains(targetUpper)) {
            return RuleType.COMPUTE_INTERMEDIATE;
        }

        if (isIdCheckExpression(exprUpper)) {
            return RuleType.IDENTITY_CHECK;
        }

        if (hasDocSources(sourceVars)) {
            return RuleType.DOCUMENT_CHECK;
        }

        if (exprUpper.matches("^\\s*0\\s*$") || exprUpper.contains("$SYSMIS")) {
            return RuleType.COMPUTE_INTERMEDIATE;
        }

        return RuleType.COMPUTE_INTERMEDIATE;
    }

    /**
     * 根据 RECODE 语法模式分类。
     */
    public static RuleType classifyRecode(String source, String target, String block) {
        String blockUpper = block != null ? block.toUpperCase(Locale.ROOT) : "";

        if (blockUpper.contains(" THRU ") && (blockUpper.contains("HIGHEST") || blockUpper.contains("LOWEST"))) {
            return RuleType.OUTCOME_DETERMINATION;
        }
        if (blockUpper.contains("SYSMIS") || blockUpper.contains("MISSING")) {
            return RuleType.MISSING_CHECK;
        }
        if (blockUpper.contains(" THRU ") && !blockUpper.contains("HIGHEST") && !blockUpper.contains("LOWEST")) {
            return RuleType.RANGE_CHECK;
        }
        if (DOC_SOURCE_PAT.matcher(source).find() || DOC_SOURCE_PAT.matcher(block).find()) {
            return RuleType.DOCUMENT_CHECK;
        }
        if (blockUpper.contains(" INTO ")) {
            return RuleType.MISSING_CHECK;
        }
        return RuleType.MISSING_CHECK;
    }

    /**
     * 分类条件块（DO IF / IF 类规则）。
     */
    public static RuleType classifyConditional(List<String> sourceVars, String block) {
        String blockUpper = block != null ? block.toUpperCase(Locale.ROOT) : "";

        if (hasDocSources(sourceVars) || DOC_SOURCE_PAT.matcher(block).find()) {
            return RuleType.DOCUMENT_CHECK;
        }
        if (blockUpper.contains("SYSMIS") || blockUpper.contains("MISSING")) {
            return RuleType.MISSING_CHECK;
        }
        if (blockUpper.contains(" THRU ") && (blockUpper.contains("HIGHEST") || blockUpper.contains("LOWEST"))) {
            return RuleType.OUTCOME_DETERMINATION;
        }
        return RuleType.CONDITIONAL_BLOCK;
    }

    private static boolean isIdCheckExpression(String expr) {
        boolean hasPowers = expr.contains("**")
                && (expr.contains("10") || expr.contains("PROVINCE") || expr.contains("CITY"));
        boolean hasIdVars = expr.contains("PROVINCE") || expr.contains("CITY")
                || expr.contains("COUNTY") || expr.contains("POINT") || expr.contains("SCHOOL");
        return hasPowers && hasIdVars;
    }

    private static boolean hasDocSources(Collection<String> vars) {
        if (vars == null) return false;
        for (String v : vars) {
            if (DOC_SOURCE_PAT.matcher(v).find()) return true;
        }
        return false;
    }
}
```

- [ ] **Step 2: 编译验证并提交**

```bash
mvn -q -DskipTests compile
git add src/main/java/com/gxaysoft/project/spsscheck/engine/parser/BlockClassifier.java
git commit -m "feat: add BlockClassifier — syntax-pattern-based RuleType classification"
```

---

### Task 2.5: 创建 ParsedScript DTO

**Files:**
- Create: `src/main/java/com/gxaysoft/project/spsscheck/engine/parser/ParsedScript.java`

- [ ] **Step 1: 创建 ParsedScript.java**

```java
package com.gxaysoft.project.spsscheck.engine.parser;

import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.model.DatasetRule;
import com.gxaysoft.project.spsscheck.engine.model.OutputRule;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次 SPSS 解析的完整结果。
 */
public class ParsedScript {
    private final List<Rule> rules;
    private final List<DatasetRule> datasetRules;
    private final List<OutputRule> outputRules;
    private final List<String> unsupportedStatements;

    public ParsedScript() {
        this.rules = new ArrayList<>();
        this.datasetRules = new ArrayList<>();
        this.outputRules = new ArrayList<>();
        this.unsupportedStatements = new ArrayList<>();
    }

    public List<Rule> getRules() { return rules; }
    public List<DatasetRule> getDatasetRules() { return datasetRules; }
    public List<OutputRule> getOutputRules() { return outputRules; }
    public List<String> getUnsupportedStatements() { return unsupportedStatements; }

    public int totalRules() { return rules.size(); }
    public int totalDatasetRules() { return datasetRules.size(); }
    public int totalOutputRules() { return outputRules.size(); }
}
```

- [ ] **Step 2: 编译验证并提交**

```bash
git add src/main/java/com/gxaysoft/project/spsscheck/engine/parser/ParsedScript.java
git commit -m "feat: add ParsedScript DTO — container for parse results"
```

---

### Task 2.6: 创建 RuleParser — 使用 ConditionStack 的解析器

**Files:**
- Create: `src/main/java/com/gxaysoft/project/spsscheck/engine/parser/RuleParser.java`

这是一个较大的文件。由于它将替代 v1 SpssRuleParser，我在这里重建核心的 parseRules() 方法，在内部仍使用 SpssUtil 和现有的 extractVariables、parseRecodeCases 等辅助方法。

```java
package com.gxaysoft.project.spsscheck.engine.parser;

import com.gxaysoft.project.spsscheck.engine.model.*;
import com.gxaysoft.project.spsscheck.model.RecodeCase;
import com.gxaysoft.project.spsscheck.parser.SpssUtil;

import java.util.*;
import java.util.regex.*;

/**
 * SPSS COMPUTE / RECODE / IF 解析器。
 * 使用 ConditionStack 将 DO IF 嵌套扁平化为 Step 列表。
 */
public final class RuleParser {

    private static final Pattern COMPUTE_PATTERN = Pattern.compile(
            "(?i)COMPUTE\\s+([^=\\r\\n]+?)\\s*=\\s*(.+?)\\.(?=\\s|$)", Pattern.DOTALL);
    private static final Pattern RECODE_INTO_PATTERN = Pattern.compile(
            "(?im)^[ \\t]*RECODE\\s+(.+?)\\s+INTO\\s+([^\\r\\n\\.]+?)\\.(?=\\s|$)");
    private static final Pattern RECODE_SELF_PATTERN = Pattern.compile(
            "(?im)^[ \\t]*RECODE\\s+([^\\s\\(]+)\\s+((?:\\([^\\)]*\\)\\s*)*)\\.");
    private static final Pattern LABEL_PATTERN = Pattern.compile(
            "(?i)VARIABLE\\s+LABELS\\s+([^\\s]+)\\s+'([^']*)'");

    private RuleParser() {}

    /**
     * 解析 SPSS 文本，返回统一 Rule 列表。
     */
    public static List<Rule> parseRules(String spssText) {
        Map<String, String> labels = parseLabels(spssText);
        List<Rule> rules = new ArrayList<>();

        // ─── 第一轮：COMPUTE 规则 ───
        Matcher matcher = COMPUTE_PATTERN.matcher(spssText);
        while (matcher.find()) {
            String target = matcher.group(1).trim();
            String expression = compact(matcher.group(2));

            if (isConstantAssignment(expression) || isTransientDuplicateVariable(target)) {
                continue;
            }

            int blockEnd = nextRuleStart(spssText, matcher.end());
            String sourceBlock = spssText.substring(matcher.start(), blockEnd).trim();

            // 检查是否有跟随的 RECODE（COMPUTE + RECODE 链）
            boolean hasSelfRecode = hasRecodeForTarget(spssText, matcher.end(), target);
            Rule rule = new Rule(target, RuleType.COMPUTE_INTERMEDIATE, sourceBlock,
                    SpssUtil.extractVariables(expression));
            rule.setExpression(expression);

            if (hasSelfRecode) {
                // COMPUTE + RECODE 复合规则
                rule.addStep(new Step(
                    findActiveDoIfCondition(spssText, matcher.start()),
                    new ComputeAction(target, expression)));
                // 解析 RECODE 步骤
                List<Step> recodeSteps = parseRecodeSteps(sourceBlock, target);
                for (Step s : recodeSteps) rule.addStep(s);
                rule.setCheckRule(hasRecodeZeroOneInSteps(rule.getSteps()));
            } else {
                // 纯 COMPUTE 规则
                String condition = findActiveDoIfCondition(spssText, matcher.start());
                if (condition != null) {
                    rule.addStep(new Step(condition, new ComputeAction(target, expression)));
                }
                // checkRule 判断
                rule.setCheckRule(false); // COMPUTE 本身不是检查规则
            }

            // 分类
            rule.setType(BlockClassifier.classifyCompute(
                    target, expression, rule.getSourceVariables()));

            String label = labels.get(SpssUtil.normalize(target));
            if (label != null) rule.setDescription(label);

            rules.add(rule);
        }

        // ─── 第二轮：RECODE INTO 规则 ───
        rules.addAll(parseRecodeIntoRules(spssText, labels));

        // ─── 第三轮：独立 IF 赋值规则 ───
        rules.addAll(parseStandaloneIfRules(spssText, labels));

        return mergeInitDeclarations(rules);
    }

    // ─── RECODE INTO 解析 ───

    private static List<Rule> parseRecodeIntoRules(String spssText, Map<String, String> labels) {
        List<Rule> rules = new ArrayList<>();
        Matcher matcher = RECODE_INTO_PATTERN.matcher(spssText);

        while (matcher.find()) {
            String target = matcher.group(2).trim();
            int blockEnd = findBlockEnd(spssText, matcher.end(), target);
            String block = spssText.substring(matcher.start(), blockEnd).trim();

            List<Step> steps = parseRecodeSteps(block, target);
            if (steps.isEmpty()) continue;

            List<String> sourceVars = extractSourceVarsFromSteps(steps, target);
            Rule rule = new Rule(target, BlockClassifier.classifyRecode(target, target, block),
                    block, sourceVars);
            rule.setSteps(steps);
            rule.setCheckRule(isZeroOneRecode(steps));

            String label = labels.get(SpssUtil.normalize(target));
            if (label != null) rule.setDescription(label);

            rules.add(rule);
        }
        return rules;
    }

    // ─── 独立 IF 规则 ───

    private static List<Rule> parseStandaloneIfRules(String spssText, Map<String, String> labels) {
        List<Rule> rules = new ArrayList<>();
        Pattern ifHead = Pattern.compile("(?im)^[ \\t]*IF\\s*\\(");
        Matcher m = ifHead.matcher(spssText);

        Set<String> existingTargets = new LinkedHashSet<>();
        for (Rule r : rules) existingTargets.add(SpssUtil.normalize(r.getTarget()));

        while (m.find()) {
            int parenStart = m.end() - 1;
            int parenEnd = findBalancedParen(spssText, parenStart);
            if (parenEnd < 0) continue;

            String condition = spssText.substring(parenStart + 1, parenEnd).trim();
            String after = spssText.substring(parenEnd + 1).trim();
            int eqIdx = after.indexOf('=');
            int dotIdx = after.indexOf('.');
            if (eqIdx < 0 || dotIdx < 0 || eqIdx >= dotIdx) continue;

            String target = after.substring(0, eqIdx).trim();
            String value = after.substring(eqIdx + 1, dotIdx).trim();
            if (target.isEmpty() || isTransientDuplicateVariable(target)) continue;
            if (existingTargets.contains(SpssUtil.normalize(target))) continue;

            // 跳过 IF 位于 RECODE 块内的情况
            int ifPos = m.start();
            String before = spssText.substring(Math.max(0, ifPos - 200), ifPos);
            if (before.toUpperCase(Locale.ROOT).matches("(?s).*RECODE\\s+[^.]*$")) continue;

            int blockEnd = findNextHardBoundary(spssText, ifPos);
            if (blockEnd < 0) blockEnd = spssText.length();
            String sourceBlock = spssText.substring(ifPos, Math.min(ifPos + 500, blockEnd)).trim();

            List<Step> steps = new ArrayList<>();
            String doIfCond = findActiveDoIfCondition(spssText, ifPos);
            StepAction action = new IfAssignAction(condition, target, value);
            steps.add(new Step(doIfCond, action));

            List<String> sourceVars = SpssUtil.extractVariables(condition);
            if (value != null && !value.matches("[+-]?\\d+(\\.\\d+)?")) {
                sourceVars.addAll(SpssUtil.extractVariables(value));
            }
            sourceVars.removeIf(v -> SpssUtil.normalize(v).equals(SpssUtil.normalize(target)));

            Rule rule = new Rule(target, RuleType.CONDITIONAL_BLOCK, sourceBlock, sourceVars);
            rule.setSteps(steps);
            rule.setCheckRule(true);

            String label = labels.get(SpssUtil.normalize(target));
            if (label != null) rule.setDescription(label);

            rules.add(rule);
            existingTargets.add(SpssUtil.normalize(target));
        }
        return rules;
    }

    // ─── 公共：解析 RECODE 步骤（支持 COMPUTE→RECODE 链） ───

    static List<Step> parseRecodeSteps(String block, String target) {
        List<Step> steps = new ArrayList<>();

        // RECODE INTO
        Matcher intoM = RECODE_INTO_PATTERN.matcher(block);
        while (intoM.find()) {
            if (SpssUtil.normalize(target).equals(SpssUtil.normalize(intoM.group(2)))) {
                String sourceAndCases = intoM.group(1).trim();
                int firstCase = sourceAndCases.indexOf('(');
                if (firstCase > 0) {
                    String source = sourceAndCases.substring(0, firstCase).trim();
                    String cases = sourceAndCases.substring(firstCase).trim();
                    StepAction action = new RecodeAction(source, target, parseRecodeCases(cases));
                    steps.add(new Step(findActiveDoIfCondition(block, intoM.start()), action));
                }
            }
        }

        // 自引用 RECODE
        Matcher selfM = RECODE_SELF_PATTERN.matcher(block);
        while (selfM.find()) {
            String source = selfM.group(1).trim();
            if (SpssUtil.normalize(target).equals(SpssUtil.normalize(source))) {
                StepAction action = new RecodeAction(source, target, parseRecodeCases(selfM.group(2).trim()));
                steps.add(new Step(findActiveDoIfCondition(block, selfM.start()), action));
            }
        }

        return steps;
    }

    // ─── 公共：COMPUTE→RECODE→FLAG 的 checkRule 判断 ───

    static boolean isZeroOneRecode(List<Step> steps) {
        boolean sawRecode = false;
        for (Step step : steps) {
            StepAction action = step.getAction();
            if (action instanceof RecodeAction) {
                sawRecode = true;
                for (RecodeCase c : ((RecodeAction) action).getCases()) {
                    if (c.isCopyResult() || !c.isZeroOneResult()) return false;
                }
            }
        }
        return sawRecode;
    }

    private static boolean hasRecodeZeroOneInSteps(List<Step> steps) {
        return isZeroOneRecode(steps);
    }

    // ─── DO IF 条件检测 ───

    /**
     * 返回包裹指定位置的活跃 DO IF 条件（null 表示不在任何 DO IF 块内）。
     * 使用栈式扫描 prefix，与 v1 的 findActiveDoIfCondition 逻辑一致。
     */
    static String findActiveDoIfCondition(String block, int statementPosition) {
        if (statementPosition <= 0) return null;
        String prefix = block.substring(0, Math.min(statementPosition, block.length()));
        Pattern pattern = Pattern.compile(
                "(?is)\\bDO\\s+IF\\s*\\((.*?)\\)\\s*\\.|\\bELSE\\s*\\.|\\bEND\\s+IF\\s*\\.");
        Matcher matcher = pattern.matcher(prefix);
        List<String> stack = new ArrayList<>();

        while (matcher.find()) {
            String token = matcher.group(0).trim().toUpperCase(Locale.ROOT);
            if (token.startsWith("DO IF")) {
                stack.add(matcher.group(1).trim());
            } else if (token.startsWith("END IF")) {
                if (!stack.isEmpty()) stack.remove(stack.size() - 1);
            } else if (token.startsWith("ELSE")) {
                if (!stack.isEmpty()) {
                    String cond = stack.remove(stack.size() - 1);
                    stack.add("NOT(" + cond + ")");
                }
            }
        }
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }

    // ─── INIT 声明合并 ───

    private static List<Rule> mergeInitDeclarations(List<Rule> rules) {
        List<Rule> merged = new ArrayList<>();
        Rule pendingInit = null;

        for (Rule rule : rules) {
            boolean isInit = rule.getSteps().isEmpty()
                    && rule.getExpression().trim().equalsIgnoreCase("$SYSMIS");

            if (isInit) {
                pendingInit = rule;
                continue;
            }

            if (pendingInit != null
                    && SpssUtil.normalize(pendingInit.getTarget())
                       .equals(SpssUtil.normalize(rule.getTarget()))) {
                // 合并：INIT 作为第一步
                List<Step> allSteps = new ArrayList<>();
                allSteps.add(new Step(null,
                    new ComputeAction(rule.getTarget(), "$SYSMIS")));
                allSteps.addAll(rule.getSteps());
                if (rule.getSteps().isEmpty() && !rule.getExpression().isEmpty()
                        && !isConstantAssignment(rule.getExpression())
                        && !rule.getExpression().equalsIgnoreCase("$SYSMIS")) {
                    allSteps.add(new Step(null,
                        new ComputeAction(rule.getTarget(), rule.getExpression())));
                }
                rule.setSteps(allSteps);
                List<String> srcVars = extractSourceVarsFromSteps(allSteps, rule.getTarget());
                rule.setSourceVariables(srcVars);
                rule.setCheckRule(rule.isCheckRule() || hasRecodeZeroOneInSteps(allSteps));
                rule.setSpssSource(pendingInit.getSpssSource() + "\n" + rule.getSpssSource());
                merged.add(rule);
                pendingInit = null;
            } else {
                if (pendingInit != null) merged.add(pendingInit);
                pendingInit = null;
                merged.add(rule);
            }
        }
        if (pendingInit != null) merged.add(pendingInit);
        return merged;
    }

    // ─── RECODE Cases 解析（与 v1 一致） ───

    static List<RecodeCase> parseRecodeCases(String casesText) {
        List<RecodeCase> cases = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\(([^=]+)=([^\\)]+)\\)").matcher(casesText);
        while (matcher.find()) {
            String from = matcher.group(1).trim();
            String to = matcher.group(2).trim();
            if ("SYSMIS".equalsIgnoreCase(from) || "MISSING".equalsIgnoreCase(from)) {
                cases.add(RecodeCase.missing(to));
            } else if ("ELSE".equalsIgnoreCase(from)) {
                cases.add(RecodeCase.elseCase(to));
            } else if (from.toLowerCase(Locale.ROOT).contains("thru")) {
                String[] parts = from.split("(?i)\\s+thru\\s+");
                if (parts.length == 2) {
                    cases.add(RecodeCase.range(parts[0].trim(), parts[1].trim(), to));
                }
            } else {
                cases.add(RecodeCase.equalsCase(from, to));
            }
        }
        return cases;
    }

    // ─── 辅助方法 ───

    static List<String> extractSourceVarsFromSteps(List<Step> steps, String target) {
        LinkedHashMap<String, String> vars = new LinkedHashMap<>();
        for (Step step : steps) {
            for (String v : step.sourceVariables()) {
                if (!SpssUtil.normalize(target).equals(SpssUtil.normalize(v))) {
                    vars.put(SpssUtil.normalize(v), v.toUpperCase(Locale.ROOT));
                }
            }
        }
        return new ArrayList<>(vars.values());
    }

    private static String compact(String s) { return s.replaceAll("\\s+", " ").trim(); }

    private static boolean isConstantAssignment(String expr) {
        return expr.matches("[+-]?\\d+(\\.\\d+)?");
    }

    private static boolean isTransientDuplicateVariable(String target) {
        String n = SpssUtil.normalize(target);
        return "MATCHSEQUENCE".equals(n) || "INDUPGRP".equals(n) || "PRIMARYLAST".equals(n);
    }

    private static Map<String, String> parseLabels(String text) {
        Map<String, String> labels = new LinkedHashMap<>();
        Matcher matcher = LABEL_PATTERN.matcher(text);
        while (matcher.find()) {
            labels.put(SpssUtil.normalize(matcher.group(1)), matcher.group(2));
        }
        return labels;
    }

    // ─── 块边界检测（与 v1 一致） ───

    private static int nextRuleStart(String text, int fromIndex) {
        int nextCompute = findNextStatementStart(text, fromIndex, "COMPUTE");
        int nextRecodeInto = findNextRecodeIntoLine(text, fromIndex, null, false, -1);
        int nextHard = findNextHardBoundary(text, fromIndex);
        int result = minPositive(nextCompute, nextRecodeInto);
        return result < 0 ? (nextHard < 0 ? text.length() : nextHard) : minPositive(result, nextHard);
    }

    private static int findBlockEnd(String text, int fromIndex, String target) {
        int hard = findNextHardBoundary(text, fromIndex);
        int nextOtherRecode = findNextRecodeIntoLine(text, fromIndex, target, true, hard);
        int nextCompute = findNextStatementStart(text, fromIndex, "COMPUTE");
        int result = minPositive(nextOtherRecode, nextCompute);
        result = minPositive(result, hard);
        return result < 0 ? text.length() : result;
    }

    private static int findNextRecodeIntoLine(String text, int fromIndex, String target,
                                              boolean differentOnly, int maxIndex) {
        int cursor = Math.max(0, fromIndex);
        Matcher m = RECODE_INTO_PATTERN.matcher(text);
        while (m.find(cursor)) {
            if (maxIndex >= 0 && m.start() >= maxIndex) return -1;
            String matchTarget = m.group(2).trim();
            if (!differentOnly || !SpssUtil.normalize(target).equals(SpssUtil.normalize(matchTarget))) {
                return m.start();
            }
            cursor = m.end();
        }
        return -1;
    }

    private static int findNextHardBoundary(String text, int fromIndex) {
        int next = -1;
        String[] statements = {
            "SELECT\\s+IF", "SAVE\\s+OUTFILE", "DATASET\\s+COPY", "DATASET\\s+ACTIVATE",
            "FILTER\\s+OFF", "USE\\s+ALL", "FREQUENCIES", "DESCRIPTIVES", "CTABLES",
            "SORT\\s+CASES", "MATCH\\s+FILES", "STRING\\s+", "NUMERIC\\s+",
            "VARIABLE\\s+LABELS", "VALUE\\s+LABELS", "VARIABLE\\s+LEVEL",
            "VARIABLE\\s+WIDTH", "FORMATS\\s+", "LEAVE\\s+", "SPLIT\\s+FILE"
        };
        for (String st : statements) {
            int idx = findNextStatementStartRegex(text, fromIndex, st);
            next = minPositive(next, idx);
        }
        return next;
    }

    private static int findNextStatementStart(String text, int fromIndex, String statement) {
        return findNextStatementStartRegex(text, fromIndex, Pattern.quote(statement));
    }

    private static int findNextStatementStartRegex(String text, int fromIndex, String regex) {
        Matcher m = Pattern.compile("(?im)^[ \\t]*" + regex + "\\b").matcher(text);
        return m.find(fromIndex) ? m.start() : -1;
    }

    private static boolean hasRecodeForTarget(String text, int fromIndex, String target) {
        int nextCompute = text.toUpperCase(Locale.ROOT).indexOf("COMPUTE", fromIndex);
        int end = nextCompute < 0 ? text.length() : nextCompute;
        String block = text.substring(fromIndex, end);
        Matcher matcher = RECODE_SELF_PATTERN.matcher(block);
        while (matcher.find()) {
            if (SpssUtil.normalize(target).equals(SpssUtil.normalize(matcher.group(1)))) return true;
        }
        return false;
    }

    private static int findBalancedParen(String text, int start) {
        if (start < 0 || start >= text.length() || text.charAt(start) != '(') return -1;
        int depth = 1;
        for (int i = start + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') { depth--; if (depth == 0) return i; }
        }
        return -1;
    }

    private static int minPositive(int a, int b) {
        if (a < 0) return b;
        if (b < 0) return a;
        return Math.min(a, b);
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn -q -DskipTests compile
```

预期：BUILD SUCCESS（可能需要修复对 SpssUtil.extractVariables 的引用 — `SpssUtil.extractVariables()` 应已存在于 `parser/SpssUtil.java` 中）

等一下 — `SpssUtil.extractVariables()` 当前定义在 `v1/parser/SpssRuleParser.java` 中，不是 `SpssUtil`。需要做两件事：
1. 将 `extractVariables()` 移动到 `SpssUtil.java`
2. 或者在 `SpssUtil.java` 中添加该方法的委托

- [ ] **Step 2a: 将 extractVariables 移到 SpssUtil.java**

读取当前的 `SpssUtil.java`，将 `SpssRuleParser.extractVariables()` 方法（连同 `stripCommentsAndStringLiterals` 和 `isLikelyVariable` 和 `KEYWORDS`）移动到 `SpssUtil.java` 中。然后在 `SpssRuleParser.java` 中改为委托调用。

由于这些方法非常长，这里先不展开完整代码，而是在 Task 2.6 中处理。

**实施步骤：**
```bash
# 读取当前 SpssUtil.java 看是否已有 extractVariables
```
如果还没有：
1. 复制 `SpssRuleParser.extractVariables()` + `stripCommentsAndStringLiterals()` + `isLikelyVariable()` + `KEYWORDS` 到 `SpssUtil.java`
2. 修改 `SpssRuleParser` 中的引用为 `SpssUtil.extractVariables()`

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/gxaysoft/project/spsscheck/engine/parser/RuleParser.java
git commit -m "feat: add RuleParser — COMPUTE/RECODE/IF parser using ConditionStack"
```

---

### Task 2.7: 创建 SpssParser — 统一解析入口

**Files:**
- Create: `src/main/java/com/gxaysoft/project/spsscheck/engine/parser/SpssParser.java`

- [ ] **Step 1: 创建 SpssParser.java**

```java
package com.gxaysoft.project.spsscheck.engine.parser;

import com.gxaysoft.project.spsscheck.engine.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SPSS 文本统一解析入口。产出 ParsedScript。
 *
 * <p>替代 v1 SpssRuleParser + v1 parseOutputRules/parseDatasetRules。
 */
public final class SpssParser {
    private static final Logger log = LoggerFactory.getLogger(SpssParser.class);

    private SpssParser() {}

    public static ParsedScript parse(String spssText) {
        ParsedScript result = new ParsedScript();

        log.info("开始解析 SPSS 文本: {} 字符", spssText != null ? spssText.length() : 0);

        if (spssText == null || spssText.trim().isEmpty()) {
            return result;
        }

        // 1. 解析 COMPUTE/RECODE/IF 规则
        result.getRules().addAll(RuleParser.parseRules(spssText));

        // 2. 解析 SORT CASES + MATCH FILES 规则
        result.getDatasetRules().addAll(parseDatasetRules(spssText));

        // 3. 解析 SELECT IF + SAVE OUTFILE 输出规则
        result.getOutputRules().addAll(parseOutputRules(spssText));

        // 4. 分类所有规则
        for (Rule rule : result.getRules()) {
            if (rule.getType() == null) {
                rule.setType(RuleType.COMPUTE_INTERMEDIATE);
            }
        }

        log.info("解析完成: rules={}, datasetRules={}, outputRules={}",
                result.totalRules(), result.totalDatasetRules(), result.totalOutputRules());

        return result;
    }

    // ─── Output / Dataset 解析（从 v1 迁移） ───

    static List<OutputRule> parseOutputRules(String spssText) {
        List<OutputRule> outputs = new java.util.ArrayList<>();
        Pattern selectPattern = Pattern.compile("(?is)SELECT\\s+IF\\s*\\((.*?)\\)\\s*\\.");
        Pattern savePattern = Pattern.compile("(?is)SAVE\\s+OUTFILE\\s*=\\s*'([^']+)'");
        Matcher matcher = selectPattern.matcher(spssText);

        while (matcher.find()) {
            int nextSelect = findNextSelect(spssText, matcher.end());
            int blockEnd = nextSelect < 0 ? spssText.length() : nextSelect;
            String block = spssText.substring(matcher.start(), blockEnd);
            Matcher saveMatcher = savePattern.matcher(block);
            if (!saveMatcher.find()) continue;

            String condition = matcher.group(1).trim().replaceAll("\\s+", " ");
            String path = saveMatcher.group(1).trim();
            String source = block.substring(0, saveMatcher.end()).trim();
            outputs.add(new OutputRule(sheetNameFromPath(path), condition, source));
        }
        return outputs;
    }

    static List<DatasetRule> parseDatasetRules(String spssText) {
        List<DatasetRule> rules = new java.util.ArrayList<>();
        Pattern pattern = Pattern.compile(
            "(?is)SORT\\s+CASES\\s+BY\\s+([^\\(\\s]+)\\(A\\)\\s*\\." +
            "\\s*MATCH\\s+FILES\\s+.*?/BY\\s+([^\\s\\r\\n]+)" +
            "\\s+/FIRST\\s*=\\s*([^\\s\\r\\n]+)\\s+/LAST\\s*=\\s*([^\\.\\s\\r\\n]+)\\s*\\.");
        Matcher matcher = pattern.matcher(spssText);

        while (matcher.find()) {
            String sortBy = matcher.group(1).trim();
            String by = matcher.group(2).trim();
            String first = matcher.group(3).trim();
            String last = matcher.group(4).trim();
            rules.add(new DatasetRule(sortBy, by, first, last, matcher.group(0).trim()));
        }
        return rules;
    }

    private static int findNextSelect(String text, int fromIndex) {
        Matcher matcher = Pattern.compile("(?is)SELECT\\s+IF\\s*\\(").matcher(text);
        return matcher.find(fromIndex) ? matcher.start() : -1;
    }

    private static String sheetNameFromPath(String path) {
        String norm = path.replace('\\', '/');
        int slash = norm.lastIndexOf('/');
        String name = slash >= 0 ? norm.substring(slash + 1) : norm;
        if (name.toLowerCase(java.util.Locale.ROOT).endsWith(".sav")) {
            name = name.substring(0, name.length() - 4);
        }
        return name;
    }
}
```

- [ ] **Step 2: 编译验证并提交**

```bash
mvn -q -DskipTests compile
git add src/main/java/com/gxaysoft/project/spsscheck/engine/parser/SpssParser.java
git commit -m "feat: add SpssParser — unified SPSS text parsing entry point"
```

---

### Task 2.8: 创建 RuleExecutor 和 AvailabilityChecker

**Files:**
- Create: `src/main/java/com/gxaysoft/project/spsscheck/engine/executor/RuleExecutor.java`
- Create: `src/main/java/com/gxaysoft/project/spsscheck/engine/executor/AvailabilityChecker.java`

- [ ] **Step 1: 创建 RuleExecutor.java（从 v1 RuleEngine 迁移）**

```java
package com.gxaysoft.project.spsscheck.engine.executor;

import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.model.Step;
import com.gxaysoft.project.spsscheck.expression.ArithmeticExpression;
import com.gxaysoft.project.spsscheck.model.RowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

/**
 * 行级规则执行器。按顺序对每行执行规则列表。
 */
public final class RuleExecutor {
    private static final Logger log = LoggerFactory.getLogger(RuleExecutor.class);

    private RuleExecutor() {}

    public static void execute(List<RowContext> rows, List<Rule> rules) {
        log.info("开始执行规则: rows={}, rules={}", rows.size(), rules.size());

        for (RowContext row : rows) {
            for (Rule rule : rules) {
                if (rule.getSteps().isEmpty()) {
                    // 无 Step — 使用旧的简化模式
                    BigDecimal computed = new ArithmeticExpression(
                            rule.getExpression(), row).parse();
                    if (rule.isCheckRule()) {
                        int flag = (computed == null
                                || computed.compareTo(BigDecimal.ZERO) != 0) ? 1 : 0;
                        row.put(rule.getTarget(), flag);
                        row.putFlag(rule.getTarget(), flag);
                    } else {
                        row.put(rule.getTarget(), computed);
                    }
                } else {
                    // 有 Step — 扁平化执行（无反射！）
                    for (Step step : rule.getSteps()) {
                        step.execute(row);
                    }
                    if (rule.isCheckRule()) {
                        BigDecimal value = row.getDecimal(rule.getTarget());
                        int flag = (value != null
                                && value.compareTo(BigDecimal.ZERO) != 0) ? 1 : 0;
                        row.put(rule.getTarget(), flag);
                        row.putFlag(rule.getTarget(), flag);
                    }
                }
            }
        }

        log.info("规则执行完毕: rows={}", rows.size());
    }
}
```

- [ ] **Step 2: 创建 AvailabilityChecker.java（从 v1 RuleAvailabilityChecker 迁移）**

```java
package com.gxaysoft.project.spsscheck.engine.executor;

import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.parser.SpssUtil;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 变量可用性链检查器。
 * <p>
 * 规则执行顺序依赖变量可用性：DB 列 → 前序规则的目标 → 数据集规则变量。
 * 依赖不可用变量的规则标记为不可执行。
 */
public final class AvailabilityChecker {
    private final Set<String> availableVariables;

    public AvailabilityChecker(Set<String> dbColumns) {
        this.availableVariables = new LinkedHashSet<>();
        for (String col : dbColumns) {
            this.availableVariables.add(SpssUtil.normalize(col));
        }
    }

    /**
     * 添加规则到可用性链（将其目标变量标记为可用）。
     */
    public void addRule(Rule rule) {
        if (rule.getTarget() != null) {
            availableVariables.add(SpssUtil.normalize(rule.getTarget()));
        }
    }

    /**
     * 添加数据集规则变量（FIRST/LAST 等）。
     */
    public void addDatasetVariables(String... vars) {
        for (String var : vars) {
            availableVariables.add(SpssUtil.normalize(var));
        }
    }

    /**
     * 检查规则的所有源变量是否可用。
     */
    public boolean isAvailable(Rule rule) {
        if (rule.getSourceVariables() == null) return true;
        for (String var : rule.getSourceVariables()) {
            if (!availableVariables.contains(SpssUtil.normalize(var))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 过滤出不可执行的规则（依赖不可用变量）。
     */
    public List<Rule> filterAvailable(List<Rule> rules) {
        List<Rule> available = new java.util.ArrayList<>();
        for (Rule rule : rules) {
            if (isAvailable(rule)) {
                available.add(rule);
                addRule(rule);
            }
        }
        return available;
    }

    public Set<String> getAvailableVariables() {
        return availableVariables;
    }
}
```

- [ ] **Step 3: 编译验证并提交**

```bash
mvn -q -DskipTests compile
git add src/main/java/com/gxaysoft/project/spsscheck/engine/executor/
git commit -m "feat: add RuleExecutor and AvailabilityChecker for engine package"
```

---

### Task 2.9: 将所有调用方改为引用 engine 包，删除 v1/v2

**Files:**
- Modify: 所有引用 `v1.parser.SpssRuleParser` 的文件
- Modify: 所有引用 `v1.model.SpssCheckRule` 的文件
- Modify: 所有引用 `v1.executor.RuleEngine` 的文件
- Delete: `src/main/java/com/gxaysoft/project/spsscheck/v1/` (entire directory)
- Delete: `src/main/java/com/gxaysoft/project/spsscheck/v2/` (entire directory)

- [ ] **Step 1: 全局替换引用 — 先找出所有需要改动的文件**

```bash
grep -rl "v1\.parser\.SpssRuleParser\|v1\.model\.SpssCheckRule\|v1\.executor\.RuleEngine\|v1\.model\.SpssDatasetRule\|v1\.model\.SpssOutputRule" src/main/java/
```

记录所有输出文件路径。

- [ ] **Step 2: 逐个修改引用文件**

对于每个引用文件，进行以下替换：

```
import ...v1.parser.SpssRuleParser  → import ...engine.parser.SpssParser
import ...v1.model.SpssCheckRule    → import ...engine.model.Rule
import ...v1.executor.RuleEngine    → import ...engine.executor.RuleExecutor
import ...v1.model.SpssDatasetRule  → import ...engine.model.DatasetRule
import ...v1.model.SpssOutputRule   → import ...engine.model.OutputRule
```

代码中的调用也相应修改：
```
SpssRuleParser.parseRules(text)     → SpssParser.parse(text).getRules()
SpssRuleParser.parseOutputRules(t)  → SpssParser.parse(t).getOutputRules()
SpssRuleParser.parseDatasetRules(t) → SpssParser.parse(t).getDatasetRules()
RuleEngine.execute(rows, rules)     → RuleExecutor.execute(rows, rules)
```

**关键文件：**
- `RuleExecuteV2Controller.java` — 大量引用
- `BlockParser.java` (v2) — 引用 SpssRuleParser.parseRules（将被删除）
- `BlockExecutor.java` (v2) — 引用 SpssRuleParser.parseOutputRules/DatasetRules（将被删除）
- `SpsUploadToDb.java`
- `SpsUploadSimulator.java`
- `PrototypeCli.java`
- `V2Runner.java`
- `SpsUploadV2.java`

- [ ] **Step 3: 对于规则列表类型的变化**

`SpssCheckRule` → `Rule` 的类型变化涉及：
- `RuleExecuteV2Controller` 中的 `List<SpssCheckRule>` → `List<Rule>`
- `StudentSpssRuleResultBuilder` 中的 `List<SpssCheckRule>` → `List<Rule>`
- `AnswerDataValidator` 中的引用
- `RuleCorrectionRuntimeService` 中的引用

- [ ] **Step 4: 取消对 v1 包的特殊依赖**

`RuleParser` 当前引用了 `SpssUtil.extractVariables()` — 需要确保此方法在 `SpssUtil.java` 中可用。

检查：`parser/SpssUtil.java` 当前是否已有 `extractVariables()`？
如果还没有 → 需要从 `SpssRuleParser` 移动过来（作为 Task 2.6 的前置工作）。

- [ ] **Step 5: 删除 v1/ 和 v2/ 目录**

```bash
rm -rf src/main/java/com/gxaysoft/project/spsscheck/v1/
rm -rf src/main/java/com/gxaysoft/project/spsscheck/v2/
```

- [ ] **Step 6: 编译验证**

```bash
mvn -q -DskipTests compile
```

解决所有编译错误后，确认 BUILD SUCCESS。

- [ ] **Step 7: 运行全量测试**

```bash
mvn test
```

- [ ] **Step 8: 运行 PrototypeCli 对照验证**

```bash
java -Dfile.encoding=UTF-8 -cp target/classes \
  com.gxaysoft.project.spsscheck.PrototypeCli \
  docs/sources/sps/表1-3.sps \
  docs/sources/sql/相关表数据.sql \
  docs/sources/cvs/表1-3/表1-3.csv
```

将输出与 `docs/table1-3-verify.txt` 对比，确保一致。

- [ ] **Step 9: 提交**

```bash
git add -A
git commit -m "refactor: replace v1/v2 with unified engine package
- Delete v1/ and v2/ packages entirely
- All callers now reference engine.parser.SpssParser
- engine.model.Rule replaces SpssCheckRule + RuleDefinition
- engine.executor.RuleExecutor replaces RuleEngine
- No more reflection access to private fields
- No more ConditionRuleStep recursive nesting"
```

---

## 阶段 3：服务层 + AST 预编译

### Task 3.1: 重命名 PersistenceService + @Transactional + Logger

**Files:**
- Rename: `RuleExecutionPersistenceService.java` → `PersistenceService.java`
- 所有引用该类的文件均更新 import

- [ ] **Step 1: 在 PersistenceService 中添加 @Transactional**

在 `saveDbExecutionResult()` 方法上添加：
```java
    @Transactional
    public SaveSummary saveDbExecutionResult(...)
```

在 `clearPreviousResults()` 方法上不要添加 `@Transactional`（它作为内部方法被 saveDbExecutionResult 调用）。

- [ ] **Step 2: 替换所有 catch (Exception ignored) {}**

扫描文件，将所有 `catch (Exception ignored) {}` 替换为带日志的处理：
```java
// Before:
} catch (Exception ignored) {}

// After:
} catch (Exception e) {
    log.warn("操作失败: {}", e.getMessage());
}
```

找到的位置：
1. `RuleExecuteV2Controller.java:311` — `loadMappingsFromDb` 中的异常
2. `RuleExecuteV2Controller.java:291` — `loadScriptTableId` 中的异常
3. 搜索 `catch (Exception ignored)` 全局替换

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/gxaysoft/project/spsscheck/persistence/
git commit -m "refactor: rename to PersistenceService, add @Transactional, replace silent catch blocks with logging"
```

---

### Task 3.2: 创建 ScriptService

**Files:**
- Create: `src/main/java/com/gxaysoft/project/spsscheck/persistence/ScriptService.java`

完整实现代码如设计文档 4.1 节所示（约 80 行）。涵盖 saveAndParse / getParsedRules / loadMappings 方法。

```bash
git add src/main/java/com/gxaysoft/project/spsscheck/persistence/ScriptService.java
git commit -m "feat: add ScriptService — script parsing and persistence"
```

---

### Task 3.3: 创建 ExecutionService

**Files:**
- Create: `src/main/java/com/gxaysoft/project/spsscheck/persistence/ExecutionService.java`
- Create: `src/main/java/com/gxaysoft/project/spsscheck/persistence/ExecuteRequest.java`
- Create: `src/main/java/com/gxaysoft/project/spsscheck/persistence/ExecuteResult.java`

完整实现代码如设计文档 4.2 节所示（约 100 行）。

```bash
git add src/main/java/com/gxaysoft/project/spsscheck/persistence/ExecutionService.java \
       src/main/java/com/gxaysoft/project/spsscheck/persistence/ExecuteRequest.java \
       src/main/java/com/gxaysoft/project/spsscheck/persistence/ExecuteResult.java
git commit -m "feat: add ExecutionService — orchestrate execution pipeline"
```

---

### Task 3.4: 创建 VersionService

**Files:**
- Create: `src/main/java/com/gxaysoft/project/spsscheck/persistence/VersionService.java`
- Create: `src/main/java/com/gxaysoft/project/spsscheck/persistence/VersionSummary.java`

完整实现代码如设计文档 4.3 节所示（约 60 行）。

```bash
git add src/main/java/com/gxaysoft/project/spsscheck/persistence/VersionService.java \
       src/main/java/com/gxaysoft/project/spsscheck/persistence/VersionSummary.java
git commit -m "feat: add VersionService — DRAFT→PUBLISHED lifecycle"
```

---

### Task 3.5: ArithmeticExpression.compile() 预编译

**Files:**
- 已完成：Task 2.2 中创建了 `CompiledExpression.java` 和 `compile()` 方法。

确认 `ComputeAction` 使用 `ArithmeticExpression.compile()` 而非每行 `new ArithmeticExpression()`。

**验证**：运行 PrototypeCli 确保性能无明显退化（预期持平或提升）。

---

## 阶段 4：Controller 拆分

### Task 4.1: 创建 ExecuteController

**Files:**
- Create: `src/main/java/com/gxaysoft/project/spsscheck/web/ExecuteController.java`

完整实现代码如设计文档 5.1 节所示。

```bash
git add src/main/java/com/gxaysoft/project/spsscheck/web/ExecuteController.java
git commit -m "feat: add ExecuteController — /api/execute/upload and /db endpoints"
```

### Task 4.2: 创建 ScriptController

**Files:**
- Create: `src/main/java/com/gxaysoft/project/spsscheck/web/ScriptController.java`

完整实现代码如设计文档 5.2 节所示。

```bash
git add src/main/java/com/gxaysoft/project/spsscheck/web/ScriptController.java
git commit -m "feat: add ScriptController — script CRUD and parse preview"
```

### Task 4.3: 创建 VersionController

**Files:**
- Create: `src/main/java/com/gxaysoft/project/spsscheck/web/VersionController.java`

```bash
git add src/main/java/com/gxaysoft/project/spsscheck/web/VersionController.java
git commit -m "feat: add VersionController — version publish/list endpoints"
```

### Task 4.4: 删除旧 Controller

```bash
rm src/main/java/com/gxaysoft/project/spsscheck/web/RuleExecuteV2Controller.java
rm src/main/java/com/gxaysoft/project/spsscheck/web/RuleControllerV2.java
```

```bash
git add -A
git commit -m "refactor: delete RuleExecuteV2Controller and RuleControllerV2 — replaced byExecute/Script/Version controllers"
```

### Task 4.5: Controller 中替换所有 e.printStackTrace() 为 log.error

搜索并替换：
```bash
grep -rn "e.printStackTrace()" src/main/java/com/gxaysoft/project/spsscheck/web/
grep -rn "e.printStackTrace()" src/main/java/com/gxaysoft/project/spsscheck/persistence/
```

每个 `e.printStackTrace()` 替换为 `log.error("错误", e)`。

```bash
git commit -m "refactor: replace all e.printStackTrace() with log.error/warn"
```

---

## 阶段 5：清理与验证

### Task 5.1: DbRuleExecutionDataLoader 使用 AnswerTableType

**Files:**
- Modify: `src/main/java/com/gxaysoft/project/spsscheck/execution/DbRuleExecutionDataLoader.java`

替换 `isTableOne()`/`isTableTwo()`/`isTableThree()` 为 `AnswerTableType.fromTableId(tableId)`。

```bash
git add src/main/java/com/gxaysoft/project/spsscheck/execution/DbRuleExecutionDataLoader.java
git commit -m "refactor: use AnswerTableType enum instead of hardcoded table ID methods"
```

### Task 5.2: V2Runner 命令行参数化

- 硬编码路径改为 `System.getProperty()` 带默认值
- `System.out.printf` 改为 `log.info`

```bash
git add src/main/java/com/gxaysoft/project/spsscheck/v2/V2Runner.java
git commit -m "refactor: parameterize V2Runner paths and replace printf with logger"
```

### Task 5.3: 删除空目录

```bash
rmdir src/main/java/com/gxaysoft/project/spsscheck/executor/ 2>/dev/null || true
rmdir src/main/java/com/gxaysoft/project/spsscheck/legacy/ 2>/dev/null || true
```

```bash
git add -A
git commit -m "chore: remove empty executor/ and legacy/ directories"
```

### Task 5.4: 更新 GLM.md

修改 GLM.md 反映新架构：
- 包结构更新
- 引用 `engine` 替代 `v1`/`v2`
- 标记 v1/v2 已废弃

```bash
git add GLM.md
git commit -m "docs: update GLM.md to reflect unified engine architecture"
```

### Task 5.5: 全量回归测试

```bash
# 编译
mvn -q -DskipTests package

# 全量测试
mvn test

# PrototypeCli 对照
java -Dfile.encoding=UTF-8 -cp target/classes \
  com.gxaysoft.project.spsscheck.PrototypeCli \
  docs/sources/sps/表1-3.sps \
  docs/sources/sql/相关表数据.sql \
  docs/sources/cvs/表1-3/表1-3.csv

# V2Runner 扫描全部 13 个文件
java -Dfile.encoding=UTF-8 -cp target/classes \
  com.gxaysoft.project.spsscheck.v2.V2Runner
```

### Task 5.6: 最终验证清单

- [ ] `mvn -q -DskipTests package` BUILD SUCCESS
- [ ] `mvn test` 全量通过
- [ ] `PrototypeCli` 输出与 `docs/table1-3-verify.txt` 一致
- [ ] `V2Runner` 扫描 13 个 .sps 文件无异常
- [ ] 代码中无 `ConditionalRuleStep` 类
- [ ] 代码中无 `Field.setAccessible(true)` 反射调用
- [ ] 代码中无 `e.printStackTrace()` 调用
- [ ] 错误响应中无 `trace` 字段
- [ ] `.gitattributes` 文件存在
- [ ] `application.yml` 中密码使用环境变量

```bash
git add -A
git commit -m "chore: final verification checklist complete"
```

---

## 总结

**总共 31 个任务，5 个阶段，预估 5.5-7.5 个工作日（单人）。**

**风险最高的操作：**
- Task 2.9（删除 v1/v2 + 全局替换引用）— 破坏性最大，务必定向在分步提交中进行
- Task 2.6（RuleParser）— 核心算法，需充分的单元测试覆盖
- Task 2.6 的前置工作（extractVariables 移到 SpssUtil）— 影响所有解析器引用

**关键验证点：**
- 阶段 2 完成后：PrototypeCli 输出必须与 `docs/table1-3-verify.txt` 一致
- 阶段 4 完成后：curl 端点返回值格式与改造前一致
- 阶段 5 完成后：V2Runner 覆盖全部 13 个 .sps 文件无异常
