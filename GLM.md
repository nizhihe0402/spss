# GLM.md

This file provides guidance to GLM when working with code in this repository.
It supersedes the now-outdated architecture description in `AGENTS.md` and
`docs/SPSS规则解析与清洗落地详细设计.md` — those describe an early single-file
prototype that no longer matches the code.

## Build & Run

```bash
# Compile (Java 8, Spring Boot 2.7.18, Maven)
mvn -q -DskipTests package

# Run the Spring Boot web app (serves the rule-management UI + REST API)
java -Dfile.encoding=UTF-8 -jar target/spss-rule-engine-codex-*.jar

# Prototype CLI against a single table's test fixtures
java -Dfile.encoding=UTF-8 \
  -cp target/classes \
  com.gxaysoft.project.spsscheck.prototype.PrototypeCli \
  docs/sources/sps/表1-3.sps \
  docs/sources/sql/相关表数据.sql \
  docs/sources/cvs/表1-3.csv

# V2Runner batch scanner: scans every .sps, parses + executes, prints a summary
java -Dfile.encoding=UTF-8 -cp target/classes \
  com.gxaysoft.project.spsscheck.V2Runner

# Tests (JUnit 5)
mvn -q test
```

**Toolchain reality:** `pom.xml` declares **Java 8**, not 17. Source code is
deliberately written to Java 8 — explicit generic typing (`new ArrayList<String>()`),
no `var`, no records, no `List.of()`. Keep new code Java 8-compatible.

## What this project is

A rule engine that parses SPSS `.sps` data-audit scripts and executes them as
Java rules against database-sourced school-health form data
(学生常见病 / 学校卫生监测 — student common disease & school hygiene monitoring).

It is **NOT** a general IBM SPSS interpreter. It supports a restricted subset:

**Supported:** `COMPUTE`, `RECODE ... INTO`, plain `RECODE`, `IF(cond) var=val`,
`DO IF ... END IF` (no `ELSE` branch negation beyond the stack hack noted below),
`SORT CASES BY` + `MATCH FILES /BY /FIRST /LAST`, `SELECT IF` + `SAVE OUTFILE`,
`VARIABLE LABELS` / `VALUE LABELS`.

**Unsupported (parsed into `sps_unsupported_statement`, flagged in report):**
`DATEDIFF`/`DATEDIF`, `MISSING()`, `$SYSMIS`, `LTRIM`/`RTRIM`, `CHAR.LENGTH`,
`CHAR.SUBSTR`, `NUMBER()`, `STRING()`, `XDATE.*`, `MOD()`, `SPLIT FILE LAYERED BY`,
`DATASET COPY`/`ACTIVATE`, `FILTER OFF`/`USE ALL`, `LEAVE`/`FORMAT`/`VARIABLE LEVEL`,
`DESCRIPTIVES`/`FREQUENCIES`/`CTABLES`, general `LOOP`/`AGGREGATE`/`ANY()`.

## Architecture — unified engine

> **Note:** The former `v1/` and `v2/` packages (step-based and block/handler-based
> dual engines) have been merged into `engine/` as of 2026-07-10.
> `ConditionalRuleStep` (v1's recursive conditional wrapper) is gone — conditions
> are now resolved at parse time and stored directly on `Step` objects.

```
com.gxaysoft.project.spsscheck
├── (root)               PrototypeCli, SpsApplication, V2Runner,
│                        SpsUploadToDb, SpsUploadSimulator, SpsUploadV2
├── config/              AnswerTableType (enum: USER_ANSWER / DOCTOR_ANSWER / STUDENT_ANSWER)
├── engine/              ← Unified parsing + execution (former v1 + v2 merged)
│   ├── model/           Rule, RuleType, Step, StepAction,
│   │                    ComputeAction, RecodeAction, IfAssignAction,
│   │                    DatasetRule, OutputRule, SegmentInfo
│   ├── parser/          SpssParser (top-level entry), RuleParser,
│   │                    BlockClassifier, ConditionStack, ParsedScript
│   └── executor/        RuleExecutor, AvailabilityChecker
├── execution/           DbRuleExecutionDataLoader (DB-based execution plumbing)
├── parser/              SpssUtil, QuestionSqlParser, QuestionJsonParser,
│                        QuestionVariableNameSelector   ← validates export_content as SPSS var
├── expression/          ArithmeticExpression (recursive-descent + - * / ** ()),
│                        ConditionExpression
├── io/                  PrototypeFileReaders, AnswerPivot, OutputWriter,
│                        StudentInfoLoader, StudentInfoEnricher, TableIdDetector
├── model/               AnswerRecord, QuestionMapping, RecodeCase, RowContext
├── persistence/         DbConnection, SchemaInitializer, SpsRepository,
│                        ExecutionService, SourceQuestionMappingSyncService,
│                        ScriptQuestionMappingService
├── validation/          AnswerDataValidator, AnswerDataValidationReport/Issue,
│                        StudentSpssRuleResultBuilder, StudentValidationResultBuilder,
│                        SourceQuestionMappingFormatter
└── web/                 Controllers + GlobalExceptionHandler
```

### Engine design

`SpssParser` (entry point) parses `.sps` text into a `ParsedScript` containing
`List<Rule>`, `List<DatasetRule>`, and `List<OutputRule>`.

Each `Rule` holds a `List<Step>`, where each `Step` pairs an optional condition
string with a `StepAction` (one of `ComputeAction`, `RecodeAction`,
`IfAssignAction`). Conditions are resolved at parse time from DO IF nesting via
`ConditionStack` — there is no recursive wrapping at execution time.

`RuleExecutor.execute(rows, rules)` iterates rows then rules, dispatching each
`Step.execute(row)`. `AvailabilityChecker` tracks the variable chain
`DB columns ∪ prior-rule-targets ∪ dataset-rule-variables`.

`RuleType` is an enum classifying rules by business semantics:
`IDENTITY_CHECK` · `DOCUMENT_CHECK` · `MISSING_CHECK` · `RANGE_CHECK` ·
`OUTCOME_DETERMINATION` · `DUPLICATE_MARK` · `OUTPUT_GROUP` ·
`COMPUTE_INTERMEDIATE` · `CONDITIONAL_BLOCK`.

Classification is by **SPSS syntax pattern**, not by Chinese target names
(see `BlockClassifier.classifyByRecodePattern` / `classifyCompute`). Keep it
that way — keyword-driven classification regresses quickly.

## Execution flow

```
.sps ──► SpssParser.parse() ──► ParsedScript { rules, datasetRules, outputRules }
.sql/.json ──► QuestionSqlParser/QuestionJsonParser ──► Map<SPSS变量名, QuestionMapping>
.csv ──► PrototypeFileReaders ──► List<AnswerRecord(sampleKey, question_id, content, table_id)>
                  │
        AnswerPivot.pivot() ──► List<RowContext>  (EAV → wide table, one row per student)
                  │
        RuleExecutor.execute(rows, rules)      ← row-level: Step.execute() per row
        DatasetRule.execute(rows)              ← cross-row: SORT + FIRST/LAST
        OutputRule.matches(row)                ← SELECT IF grouping → output sheet
```

### Step action types

- `ComputeAction` — evaluate arithmetic expression, store result in target variable
- `RecodeAction` — map source through recode cases (equals / range / missing / else) into target
- `IfAssignAction` — `IF(cond) target = value` conditional assignment

Each `Step` carries an optional condition resolved from DO IF nesting at parse time.
A `Step` with a null condition runs unconditionally; one with a condition runs only
when the condition evaluates to true.

## Variable mapping: `bus_question` → SPSS variable names

```
bus_question.question_id      ─┐
bus_question.export_content    ├── SPSS variable name (e.g., "ID1", "A121", "Q6")
bus_question.export_sort       │    export order ONLY; not used for rule-execution mapping
bus_question.table_id         ─┘
bus_doctor_answer.question_id ── JOIN ──► answer.content → RowContext.put(variableName, content)
bus_doctor_answer.student_id  ── identifies which row the answer belongs to
```

`QuestionVariableNameSelector.variableNameFromExportContent()` gates this: it
returns the name only when `export_content` matches `[A-Za-z][A-Za-z0-9_]*`,
filtering out Chinese text and malformed values. Use it wherever a variable
name is derived from `bus_question`.

The CSV sample key varies by table: 表1-1 uses `code`, 表2-1 uses `student_id`.
`PrototypeFileReaders` reads both (`codeIndex` with `studentIndex` fallback).
`AnswerTableType` (config package) encodes table grouping: table IDs 1/2/10 are
USER_ANSWER (keyed by `code`), 3/4/5 are DOCTOR_ANSWER, 6/7/8 are STUDENT_ANSWER.

## Critical design rules

1. **`java_preview` is NOT an execution basis.** Execute structured `rule_json`
   or `sps_rule_step` rows only. Three-layer save per rule: `spss_source`
   (traceability) + `rule_json` (execution/edit authority) + `java_preview`
   (human only).

2. **Variable availability chain:** `AvailabilityChecker` tracks
   `DB columns ∪ prior-rule-targets ∪ dataset-rule-variables`. Rules that
   depend on not-yet-available variables are flagged non-executable.

3. **RECODE overwrites the target field's value.** The COMPUTE→RECODE→SELECT IF
   chain depends on RECODE replacing the computed value, not just setting a flag.

4. **SORT CASES must happen before FIRST/LAST assignment.** `DatasetRule.execute()`
   sorts by `sortVariable`, then groups — matching SPSS semantics.

5. **`table_id` filtering is critical.** CSV data may contain answers for
   multiple tables; the wrong `table_id` produces zero rows. `TableIdDetector`
   auto-detects the most frequent `table_id`, or pass it explicitly.

6. **Rule lifecycle:** DRAFT → REVIEWING → PUBLISHED. Execution binds to a
   PUBLISHED `version_id`.

## Database schema for rule storage

`docs/spss_rule_schema.sql` — MySQL 5.7 (compatible with PostgreSQL/Kingbase).

Rule storage: `sps_script`, `sps_rule`, `sps_rule_step`, `sps_output_rule`,
`sps_rule_change_log`, `sps_unsupported_statement`, `sps_rule_version`.
Execution: `sps_run_batch`, `sps_run_row_temp`, `sps_run_var_temp`,
`sps_check_error_detail`. Dedup: `sps_clean_unique_index`.

`sps_rule.source_question_mappings` (new column, TEXT) caches the formatted
`var -> question_id` lookup produced by `SourceQuestionMappingFormatter`, back-filled
by `SourceQuestionMappingSyncService.syncScript(scriptId)` and surfaced in the UI.
Both `docs/spss_rule_schema.sql` (DDL) and `src/main/resources/schema.sql`
(idempotent `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`) declare it.

## Upstream database (`healthdetection_2025`)

| Table | Purpose |
|---|---|
| `bus_question` | Question definitions; `export_content` → SPSS var; `table_id` |
| `bus_doctor_answer` | Student answers: `question_id`, `student_id`, `content`, `table_id`, `project_id` |
| `bus_student` | Demographics: name, sex, birthday, school, grade, class |
| `bus_school` | School info: name, type, region |
| `bus_project` / `bus_project_table` | Project/year definitions and table membership |
| `bus_option` | Answer options (select-type questions) |

Full schema: `docs/sources/sql/整个库表结构.sql`.

## Test fixtures in `docs/sources/`

```
sps/          13 SPSS scripts across 3 table groups + outcome determination
              表1-1/1-2/1-3 (行政), 表2-1/2-2/2-3 (学生体检), 表3-1/3-2/3-3,
              修近视筛查语法.sps, 结局判定/ (超重肥胖/血压偏高/脊柱弯曲)
cvs/          sample answer CSVs (one per table)
sql/          整个库表结构.sql (full schema dump), 相关表数据.sql (INSERT data),
              bus_question_*.json/.csv/.xlsx (question export snapshots)
data/         学生证件类型证件号.json (student id-type/number enrichment)
```

## Known issues & tech debt

- **`AGENTS.md` is stale.** It describes the single-file prototype. Trust this
  file instead, and update `AGENTS.md` when you make structural changes.
- **Hardcoded paths in `V2Runner.main()`** have fallbacks via CLI args and
  system properties (`mappingPath`, `studentPath`), but the defaults still
  reference local fixture paths — breaks off-host unless overridden.
- **Rule version / publish / audit service layer not implemented.** The schema
  (`sps_rule_version`, `sps_rule_change_log`) exists; the Service layer does not.
  Design doc P0 items "save parse results to DB", "publish version", "execute by
  published version_id" are still open.
- **Line-ending noise:** working copy triggers many `LF will be replaced by CRLF`
  warnings. `.gitattributes` (`* text=auto eol=lf`) is in place but may need
  `git add --renormalize .` on existing tracked files.

## Data-handling caution

Some files under `docs/sources/cvs/` and `docs/sources/data/` look like **real
student health records** (e.g. `bus_student_answer_*.csv` with thousands of
rows, `学生证件类型证件号.json`). Do not commit additional real PII. Prefer
synthetic/de-identified fixtures. When in doubt, surface it rather than
proceeding.

## Verification

No golden-file harness beyond fixtures. Manual verification: run the prototype
CLI against known fixtures and compare against `docs/table1-3-verify.txt`; run
`mvn test` for the JUnit 5 suite under `src/test/java/.../parser` and `.../validation`.
