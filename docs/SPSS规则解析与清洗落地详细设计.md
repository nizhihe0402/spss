# SPSS 规则解析与清洗落地详细设计

## 1. 背景与目标

现有业务每年存在 10～20 个 `.sps` 数据审核脚本，脚本用于对学校卫生监测、学生健康检查等表单数据进行校验、清洗、异常分组输出。数据实际来源不是 Excel，而是数据库中的问卷/检查明细数据。系统需要支持：

1. 上传 `.sps` 文件并解析规则。
2. 将解析后的规则保存到数据库。
3. 允许页面编辑、启用、禁用、发布版本。
4. 从数据库读取原始数据，按规则执行校验和清洗。
5. 保存异常数据、清洗后数据和执行日志。
6. 后续可支持增量清洗、重复数据判断、规则版本追溯。

本方案不把全部 SPS 语法完整实现为 IBM SPSS 解释器，而是实现“受限 SPS 数据审核规则引擎”。

---

## 2. 总体架构

```text
SPS文件上传
  ↓
SpssRuleParser 解析
  ↓
RuleSet / Rule / RuleStep / OutputRule
  ↓
规则报告与人工确认
  ↓
保存数据库：sps_script / sps_rule / sps_rule_step / sps_output_rule
  ↓
发布规则版本
  ↓
执行任务
  ↓
数据库读取原始明细数据
  ↓
Pivot 成 RowContext / 临时宽表数据
  ↓
Java 执行行级规则
  ↓
执行数据集级规则：重复样本、分组状态等
  ↓
执行输出规则：异常分组、清理后数据
  ↓
保存异常结果、清洗后结果、日志
```

---

## 3. 规则分类

### 3.1 中间计算规则

例如：

```spss
COMPUTE ID3=((10**5)*province)+((10**3)*city)+(county*(10**1))+point.
```

系统保存为：

```json
{
  "type": "COMPUTE",
  "target": "ID3",
  "expression": "((10**5)*province)+((10**3)*city)+(county*(10**1))+point"
}
```

中间计算不直接表示异常，但后续校验规则会依赖它。

### 3.2 行级校验规则

例如：

```spss
COMPUTE ID是否一致=ID1 - ID3.
RECODE ID是否一致 (0=0) (SYSMIS=1) (ELSE=1).
```

业务含义：

```text
ID1 等于 ID3 时为 0，否则为 1。
```

保存为一条 `ROW_CHECK`，包含两个步骤：

1. `COMPUTE`
2. `RECODE`

### 3.3 数据集级校验规则

例如：

```spss
SORT CASES BY ID1(A).
MATCH FILES
  /FILE=*
  /BY ID1
  /FIRST=PrimaryFirst1
  /LAST=PrimaryLast.
```

业务含义：

```text
按 ID1 分组，每组第一条 PrimaryFirst1=1，后续重复条 PrimaryFirst1=0。
```

这不是行级规则，不能单行判断。建议保存为：

```json
{
  "type": "DUPLICATE_MARK",
  "sortBy": "ID1",
  "by": ["ID1"],
  "firstVariable": "PrimaryFirst1",
  "lastVariable": "PrimaryLast"
}
```

### 3.4 输出分组规则

例如：

```spss
SELECT IF (ID是否一致 = 1).
SAVE OUTFILE='.../01-表1-1-ID不一致.sav'.
```

保存为：

```json
{
  "type": "OUTPUT_RULE",
  "outputType": "ERROR_GROUP",
  "outputName": "01-表1-1-ID不一致",
  "condition": "ID是否一致 = 1"
}
```

`表1-1-清理后` 这类输出规则的 `outputType` 应保存为 `CLEAN_DATA`。

### 3.5 暂不支持/仅展示语句

第一版不建议执行以下统计或复杂语法：

- `DESCRIPTIVES`
- `FREQUENCIES`
- `CTABLES`
- 通用 `LOOP`
- 通用 `AGGREGATE`
- 复杂 `DO IF / ELSE / END IF` 分支
- `ANY()`、`MISSING()` 等函数

但必须保存到 `sps_unsupported_statement`，用于报告和人工确认。

---

## 4. 已修复的核心问题

### 4.1 普通 RECODE 覆盖字段值

旧问题：

```text
COMPUTE 产生差值，RECODE 只写 flags，SELECT IF 读取 values，导致漏筛。
```

修复后：

```text
COMPUTE 先写差值；
RECODE 再覆盖同名字段为 0/1；
SELECT IF 读取覆盖后的字段。
```

### 4.2 表1-3 table_id 自动识别

表1-3 的 CSV 内 `table_id=10`。若错误使用表1-1的 table_id，会导致变量映射错乱，宽表样本数为 0。

修复后：

```text
CSV 读取 AnswerRecord 时保留 table_id；
TableIdDetector 自动识别出现次数最多的 table_id；
QuestionSqlParser 按正确 table_id 解析变量映射。
```

### 4.3 中间变量可执行性判断

`ID3`、`PrimaryFirst1` 不是数据库原始字段，而是规则生成变量。可执行性判断必须使用：

```text
原始变量 + 前序规则生成变量 + 数据集规则生成变量
```

不能只依赖 `bus_question` 映射。

### 4.4 DO IF 条件块

表1-3 中存在：

```spss
DO IF (PRIMARY = 1).
RECODE PG011 ... INTO 小学教室1年级代码缺失或异常.
END IF.
```

修复后，`DO IF` 条件会作为 `ConditionalRuleStep` 包装到规则步骤上。

保存时建议保存：

```json
{
  "stepType": "RECODE",
  "condition": "PRIMARY = 1",
  "source": "PG011",
  "target": "小学教室1年级代码缺失或异常"
}
```

---

## 5. 数据来源与 Pivot

数据库答案通常是明细结构：

```text
sample_key / student_id / code
question_id
content
```

SPSS 需要宽表：

```text
ID1, PROVINCE, CITY, COUNTY, POINT, A121, A122, A123 ...
```

因此执行前需要：

```text
bus_question.export_content → SPSS变量名
bus_question.export_sort → 导出顺序，不参与规则执行变量映射
answer.question_id → 变量名
sample_key → 一行 RowContext
```

当前原型的 `AnswerPivot.pivot()` 负责完成此转换。

---

## 6. 规则保存设计

### 6.1 保存三类内容

不要只保存 Java 字符串，也不要只保存 SPS 原文。推荐保存：

```text
1. spss_source：原始片段，用于追溯
2. rule_json：结构化规则，用于执行和编辑
3. java_preview：预览文本，只给人看，不作为执行依据
```

### 6.2 核心表

最小可用表：

```text
sps_script
sps_rule
sps_rule_step
sps_output_rule
sps_rule_change_log
```

增强表：

```text
sps_rule_version
sps_unsupported_statement
sps_run_batch
sps_run_row_temp
sps_run_var_temp
sps_check_error_detail
sps_clean_unique_index
```

详细 DDL 见：`docs/spss_rule_schema.sql`。

---

## 7. 页面编辑设计

页面允许编辑的内容：

| 类型 | 可编辑项 |
|---|---|
| 规则基础信息 | 规则名称、启用状态、是否影响清洗、风险提示 |
| COMPUTE | 目标变量、表达式 |
| RECODE | 来源变量、目标变量、重编码规则 |
| IF_ASSIGN | 条件、目标变量、赋值 |
| DUPLICATE_MARK | 排序字段、分组字段、FIRST变量、LAST变量 |
| OUTPUT_RULE | 输出名称、输出类型、筛选条件 |

不建议用户编辑：

```text
spss_source
java_preview
系统内部ID
已发布版本的原始内容
```

编辑流程：

```text
解析结果 = DRAFT
人工修改 = DRAFT
审核确认 = REVIEWING
发布 = PUBLISHED
执行任务固定使用某个 PUBLISHED version_id
```

---

## 8. 执行流程设计

### 8.1 小数据量原型流程

```text
读取全部答案
  ↓
Pivot 成 List<RowContext>
  ↓
RuleEngine.execute 行级规则
  ↓
SpssDatasetRule.execute 数据集规则
  ↓
SpssOutputRule.matches 输出分组
```

适合验证，不适合大批量生产。

### 8.2 大数据生产流程

大数据不要一次性放入 JVM 内存。推荐：

```text
分批读取原始数据
  ↓
分批执行行级规则
  ↓
临时结果落库：sps_run_row_temp / sps_run_var_temp
  ↓
数据集规则用 DB/Redis 辅助生成状态变量
  ↓
分批执行输出规则
  ↓
保存清洗后数据和异常数据
```

### 8.3 去重规则处理

全量批次内去重可用 Redis：

```text
sps:run:{runId}:seen:ID1
```

增量清洗不能只用 runId Key，必须增加业务范围去重索引：

```text
sps_clean_unique_index 作为权威索引
Redis Hash/Set 作为缓存
```

推荐：

```text
DB 权威索引 + Redis 加速判断
```

---

## 9. Redis 去重设计

### 9.1 全量任务内去重

```text
SADD sps:run:{runId}:seen:ID1 {id1}
```

返回值：

| 返回 | PrimaryFirst1 |
|---:|---:|
| 1 | 1 |
| 0 | 0 |

### 9.2 增量去重

Redis Key 不应使用 runId，而应使用业务范围：

```text
sps:clean:owner:{projectId}:{tableId}:{year}:DUP_ID1
```

Hash 结构：

```text
HSET key {ID1} {ownerRowKey}
```

同时写入数据库表 `sps_clean_unique_index`。Redis 仅作为缓存，不作为最终权威。

---

## 10. 当前代码模块说明

### 10.1 `SpssRuleParser`

职责：

- 解析行级规则
- 解析输出规则
- 解析数据集级规则
- 解析 `DO IF` 条件块
- 提取变量依赖

### 10.2 `RuleEngine`

职责：

- 对 `RowContext` 执行 `COMPUTE`
- 执行 `RECODE`
- 执行 `IF_ASSIGN`
- 生成 0/1 校验标记

### 10.3 `SpssDatasetRule`

职责：

- 模拟 `SORT CASES BY ...`
- 模拟 `MATCH FILES /BY /FIRST /LAST`
- 生成 `PrimaryFirst1`、`PrimaryLast`

### 10.4 `RuleAvailabilityChecker`

职责：

- 判断规则是否可执行
- 识别缺失变量
- 正确处理中间生成变量

### 10.5 `PrototypeCli`

职责：

- 用于命令行验证 `.sps + sql + csv`
- 自动识别 `table_id`
- 输出规则数量、映射数量、宽表样本数、输出分组数量

---

## 11. 迁移到正式工程的建议包结构

```text
com.xxx.spsscheck
├── parser
│   ├── SpssRuleParser
│   ├── SpssStatementSplitter
│   └── UnsupportedStatementCollector
├── model
│   ├── RuleSet
│   ├── SpssRule
│   ├── RuleStep
│   ├── OutputRule
│   └── RowContext
├── expression
│   ├── ArithmeticExpression
│   └── ConditionExpression
├── executor
│   ├── RowRuleExecutor
│   ├── DatasetRuleExecutor
│   ├── OutputRuleExecutor
│   └── StreamingRunExecutor
├── persistence
│   ├── RuleRepository
│   ├── RuleStepRepository
│   ├── OutputRuleRepository
│   └── RunResultRepository
├── report
│   ├── ParseReportService
│   └── RunReportService
└── web
    ├── SpssScriptController
    ├── SpssRuleController
    └── SpssRunController
```

---

## 12. API 设计建议

```text
POST /sps/scripts/upload
GET  /sps/scripts/{id}/parse-report
GET  /sps/rules?versionId=xxx
PUT  /sps/rules/{id}
POST /sps/rule-versions/{id}/publish
POST /sps/run
GET  /sps/run/{runId}
GET  /sps/run/{runId}/outputs
GET  /sps/run/{runId}/errors
```

---

## 13. 后续开发优先级

### P0

1. 拆分当前单文件为正式包结构。
2. 保存解析结果到数据库。
3. 页面展示规则解析报告。
4. 支持规则编辑、启用、禁用。
5. 支持版本发布。
6. 实现按已发布版本执行。

### P1

1. 大数据分批执行。
2. 临时结果落库。
3. Redis + DB 去重索引。
4. 规则执行日志。
5. 规则变更审计。

### P2

1. 更完整的条件表达式解析。
2. 统计语句 `FREQUENCIES / DESCRIPTIVES / CTABLES` 输出。
3. 通用 `DO IF / ELSE` 支持。
4. 标准库规则：超重肥胖、营养不良、血压偏高、脊柱弯曲等。

---

## 14. 风险与边界

当前原型可以覆盖表1-1、表1-3这类数据审核脚本的主干语法，但不能承诺等价执行全部 IBM SPSS Syntax。

必须在解析报告中显示：

```text
支持语句数
不支持语句数
未匹配变量
系统生成变量
条件块规则
输出规则
风险提示
```

正式上线前，每张表至少准备以下测试集：

```text
正常数据
ID不一致
ID重复
基本信息缺失
字段关系不一致
DO IF 条件不满足时不应误报
清理后数据命中
输出异常分组命中
```
