# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Compile (Java 17, Maven)
mvn -q -DskipTests package

# Run prototype CLI against a single table's test fixtures
java -Dfile.encoding=UTF-8 \
  -cp target/classes \
  com.gxaysoft.project.spsscheck.prototype.PrototypeCli \
  docs/sources/sps/表1-3.sps \
  docs/sources/sql/相关表数据.sql \
  docs/sources/cvs/表1-3.csv
```

No test framework is configured. Verification is manual: run against known fixtures and compare against `docs/table1-3-verify.txt`.

## Test fixtures in `docs/sources/`

```
sps/          13 SPSS scripts across 3 table groups + outcome determination
  ├── 表1-1.sps, 表1-2.sps, 表1-3.sps     Administrative survey tables
  ├── 表2-1.sps, 表2-2.sps, 表2-3.sps     Student health examination tables
  ├── 表3-1.sps, 表3-2.sps, 表3-3.sps     Additional tables
  ├── 修近视筛查语法.sps                    Myopia screening
  └── 结局判定/                              Outcome determination (lookup-table rules)
       ├── 超重肥胖生长迟缓营养不良语法.sps
       ├── 血压偏高语法.sps
       └── 脊柱弯曲判定-1126.sps

cvs/          8 CSV files with sample answer data (one per table)
sql/
  ├── 整个库表结构.sql    Full MySQL schema dump of healthdetection_2025 (~100 tables)
  └── 相关表数据.sql        INSERT data for bus_question, bus_option, bus_table, etc.
```

## What this project is

A prototype engine that parses SPSS `.sps` data-audit scripts and executes them as Java rules against database-sourced form data (school health monitoring surveys — 学生常见病/学校卫生监测). It is NOT a general IBM SPSS interpreter — it supports a restricted subset of SPSS syntax.

**Supported SPSS syntax:** `COMPUTE`, `RECODE ... INTO`, plain `RECODE`, `IF(condition) var=value`, `DO IF ... END IF` conditional blocks, `SORT CASES BY` + `MATCH FILES /BY /FIRST /LAST`, `SELECT IF` + `SAVE OUTFILE`, `VARIABLE LABELS` / `VALUE LABELS`.

**Notable unsupported syntax that appears in real scripts** (parse into `sps_unsupported_statement`, flag in report):
- `DATEDIFF` / `DATEDIF` — date-based age calculation (表2-1, 结局判定)
- `MISSING()`, `$SYSMIS`, `LTRIM()`, `RTRIM()` — null/string functions
- `CHAR.LENGTH`, `CHAR.SUBSTR`, `NUMBER()`, `STRING()` — type conversion functions
- `XDATE.YEAR/MONTH/MDAY`, `MOD()` — date extraction and modulus
- `SPLIT FILE LAYERED BY` — grouped analysis (表2-1 grade-based age validation)
- `DATASET COPY` / `DATASET ACTIVATE` — multi-dataset workflows
- `FILTER OFF` / `USE ALL` — filter state management
- `LEAVE`, `FORMAT`, `VARIABLE LEVEL`, `VARIABLE WIDTH` — metadata statements
- `DESCRIPTIVES`, `FREQUENCIES`, `CTABLES` — statistical output procedures
- `DO IF ... ELSE ... END IF` — ELSE branches (current parser only handles DO IF wrapper, not ELSE negation)
- `LOOP`, `AGGREGATE`, `ANY()` — general-purpose constructs

## Architecture (all in one file)

The entire engine lives in `SpssPrototypeCore.java` (~1550 lines). Execution flow:

```
.sps ──► SpssRuleParser ──► List<SpssCheckRule> + List<SpssDatasetRule> + List<SpssOutputRule>
.sql ──► QuestionSqlParser ──► Map<SPSS变量名, QuestionMapping(question_id, table_id)>
.csv ──► PrototypeFileReaders ──► List<AnswerRecord(sampleKey, question_id, content, table_id)>
                  │
        AnswerPivot.pivot() ──► List<RowContext>  (EAV → wide table, one row per student)
                  │
        RuleEngine.execute(rows, rules)       ← row-level: compute → recode → flag
        SpssDatasetRule.execute(rows)         ← cross-row: SORT + FIRST/LAST
        SpssOutputRule.matches(row)           ← SELECT IF grouping → output sheet
```

**Key classes:**

| Class | Role |
|---|---|
| `SpssRuleParser` | Regex-based SPS parser: COMPUTE, RECODE (INTO and self), IF, DO IF nesting via stack |
| `QuestionSqlParser` | Parses SQL INSERT INTO `bus_question` statements. Maps `export_sort`/`export_content` → SPSS variable name, keyed by `table_id` |
| `AnswerPivot` | Pivots entity-attribute-value answer rows into wide-table `RowContext` per sample |
| `RuleEngine` | Row-level executor: evaluates `ArithmeticExpression`, dispatches `RuleStep.execute()` |
| `RuleAvailabilityChecker` | Tracks variable chain: DB originals → prior rule targets → dataset rule variables |
| `ArithmeticExpression` | Hand-written recursive-descent parser: `+ - * / ** ()`, variable lookup, Unicode identifier support |
| `ConditionExpression` | AND/OR splitting + regex comparison operators: `= <> < > <= >=` |
| `TableIdDetector` | Auto-detects most frequent `table_id` in CSV when not explicitly provided |
| `SpssDatasetRule` | Sorts rows by `sortVariable`, groups by `byVariable`, sets FIRST/LAST flags |

**Rule step types (`RuleStep` interface):**
- `ComputeRuleStep` — eval arithmetic expr → store in target variable
- `RecodeRuleStep` — map source through recode cases (equals, range, missing, else) → target
- `IfAssignRuleStep` — `IF(cond) var=val` conditional assignment
- `ConditionalRuleStep` — wraps any step with a DO IF condition; tracks nesting via stack in `findActiveDoIfCondition()`

## Variable mapping: `bus_question` → SPSS variable names

The upstream database links questions to SPSS variables via `bus_question`:

```
bus_question.question_id      ─┐
bus_question.export_sort       ├── SPSS variable name (e.g., "ID1", "A121", "Q6")
bus_question.export_content    │    Prototype uses export_sort first, falls back to export_content
bus_question.table_id         ─┘
bus_doctor_answer.question_id ── JOIN ──► answer.content → RowContext.put(variableName, content)
bus_doctor_answer.student_id  ── identifies which row the answer belongs to
```

The CSV format varies slightly across tables:
- 表1-1 CSV: uses `code` field as sample key
- 表2-1 CSV: uses `student_id` field as sample key
- Prototype reads both (`codeIndex` with `studentIndex` fallback)

## Critical design rules

1. **`java_preview` is NOT an execution basis.** Execution must use structured `rule_json` or `sps_rule_step` rows.

2. **Three-layer save per rule:** `spss_source` (traceability) + `rule_json` (execution/editing authority) + `java_preview` (human only).

3. **Variable availability chain:** `RuleAvailabilityChecker` tracks `DB columns ∪ prior-rule-targets ∪ dataset-rule-variables`. Rules dependent on not-yet-available variables are flagged non-executable.

4. **RECODE overwrites the target field's value.** The COMPUTE→RECODE→SELECT IF chain depends on RECODE replacing the computed value, not just setting a flag.

5. **SORT CASES must happen before FIRST/LAST assignment.** `SpssDatasetRule.execute()` sorts by `sortVariable`, then groups — matching SPSS semantics.

6. **`table_id` filtering is critical.** CSV data may contain answers for multiple tables. Using the wrong `table_id` for variable mapping produces zero rows. `TableIdDetector` auto-detects, or pass as 4th CLI arg.

## Upstream database (healthdetection_2025)

Core tables relevant to this engine (full schema in `docs/sources/sql/整个库表结构.sql`):

| Table | Purpose |
|---|---|
| `bus_question` | Question definitions; `export_sort`/`export_content` → SPSS variable name; `table_id` |
| `bus_doctor_answer` | Student answers: `question_id`, `student_id`, `content`, `table_id`, `project_id` |
| `bus_student` | Student demographics: name, sex, birthday, school, grade, class |
| `bus_school` | School info: name, type, region |
| `bus_project` | Project/year definitions |
| `bus_project_table` | Which tables belong to which projects |
| `bus_option` | Answer options (for select-type questions) |

The engine reads `bus_doctor_answer` content and pivots it by `student_id` + `question_id → variableName` into wide rows for rule execution.

## Target migration structure

When splitting the monolith:

```
com.xxx.spsscheck
├── parser/        SpssRuleParser, SpssStatementSplitter, UnsupportedStatementCollector
├── model/         RuleSet, SpssRule, RuleStep, OutputRule, RowContext
├── expression/    ArithmeticExpression, ConditionExpression
├── executor/      RowRuleExecutor, DatasetRuleExecutor, OutputRuleExecutor, StreamingRunExecutor
├── persistence/   RuleRepository, RuleStepRepository, OutputRuleRepository, RunResultRepository
├── report/        ParseReportService, RunReportService
└── web/           SpssScriptController, SpssRuleController, SpssRunController
```

Rule lifecycle: DRAFT → REVIEWING → PUBLISHED. Execution always binds to a PUBLISHED version_id.

## Database schema for rule storage

`docs/spss_rule_schema.sql` — MySQL 5.7 (compatible with PostgreSQL/Kingbase). Core: `sps_script`, `sps_rule`, `sps_rule_step`, `sps_output_rule`, `sps_rule_change_log`. Execution: `sps_run_batch`, `sps_run_row_temp`, `sps_run_var_temp`, `sps_check_error_detail`. Dedup: `sps_clean_unique_index`.
