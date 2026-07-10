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

# V2 batch runner: scans every .sps, classifies + executes, prints a summary
java -Dfile.encoding=UTF-8 -cp target/classes \
  com.gxaysoft.project.spsscheck.v2.V2Runner

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

## Architecture — v1/v2 dual engine (transitional)

The single-file `SpssPrototypeCore.java` described in the design docs has been
split into packages. **Two parsing+execution systems now coexist** and are
deliberately coupled during a migration that is not yet finished.

```
com.gxaysoft.project.spsscheck
├── (root)            PrototypeCli, SpsApplication, SpsUploadSimulator, SpsUploadToDb
├── parser/           SpssUtil, QuestionSqlParser, QuestionJsonParser,
│                     QuestionVariableNameSelector     ← new: validates export_content as SPSS var
├── expression/       ArithmeticExpression (recursive-descent + - * / ** ()), ConditionExpression
├── io/               PrototypeFileReaders, AnswerPivot, OutputWriter,
│                     StudentInfoLoader, StudentInfoEnricher, TableIdDetector
├── model/            AnswerRecord, QuestionMapping, RecodeCase, RowContext
├── v1/               ← Step-based engine (the live execution core)
│   ├── model/        SpssCheckRule, RuleStep, ComputeRuleStep, RecodeRuleStep,
│   │                 IfAssignRuleStep, ConditionalRuleStep, PositionedRuleStep,
│   │                 RuleAvailability, SpssDatasetRule, SpssOutputRule
│   ├── parser/       SpssRuleParser, RuleDescriptionBuilder
│   └── executor/     RuleEngine, RuleAvailabilityChecker
├── v2/               ← Block/Handler engine (classification + report layer today)
│   ├── model/        RuleDefinition, RuleType (enum), RuleHandler, SpssSegment
│   ├── parser/       BlockParser
│   ├── executor/     BlockExecutor
│   └── handler/      HandlerRegistry, ComputeHandler, CheckHandler,
│                     DuplicateMarkHandler, OutputGroupHandler
├── persistence/      DbConnection, SchemaInitializer, SpsRepository,
│                     SourceQuestionMappingSyncService    ← new: back-fills source_question_mappings
├── validation/       AnswerDataValidator, AnswerDataValidationReport/Issue,
│                     StudentSpssRuleResultBuilder, StudentValidationResultBuilder,
│                     SourceQuestionMappingFormatter     ← new: formats "var -> question_id"
└── web/              RuleController, RuleControllerV2, RuleExecuteV2Controller, RunController,
                      ScriptController, SnippetController, OutputRuleController,
                      UnsupportedController, UploadController
```

### The two engines

**v1 — Step-based (`v1/`)** is the live execution core.
`SpssRuleParser.parseRules()` → `List<SpssCheckRule>`, each carrying a
`List<RuleStep>` (`ComputeRuleStep` / `RecodeRuleStep` / `IfAssignRuleStep` /
`ConditionalRuleStep`). `RuleEngine.execute()` runs the step chain per row.
`RuleAvailabilityChecker` tracks the variable chain
`DB columns ∪ prior-rule-targets ∪ dataset-rule-variables`.

**v2 — Block/Handler-based (`v2/`)** classifies by business semantics.
`BlockParser.parse()` splits SPS text on command boundaries into line-aware
`SpssSegment`s, classifies each into a `RuleType`, and produces
`RuleDefinition`s. `HandlerRegistry` dispatches to a `RuleHandler` per type.

**Critical coupling — v2 leans on v1 for execution.** `BlockParser.mergeV1Execution()`
calls back into `SpssRuleParser.parseRules()` and copies v1's parsed `steps`
into v2's `RuleDefinition`. `BlockExecutor.run()` also reuses v1's
`parseOutputRules()` / `parseDatasetRules()`. v2 has no independent step
execution today.

### Which engine is the web app using?

`RuleExecuteV2Controller` (the live `/api/v2/rules/execute` endpoint) walks the
**v1** pipeline end-to-end: `SpssRuleParser.parseRules` → `AnswerPivot.pivot` →
`RuleEngine.execute` → `SpssDatasetRule.execute` →
`StudentSpssRuleResultBuilder.build`. The "V2" in the controller name refers to
the controller generation, **not** the `v2/` engine. `BlockExecutor`/`V2Runner`
is used only by the batch CLI scanner. Do not be misled by the name.

## Execution flow (live, v1 path)

```
.sps ──► SpssRuleParser ──► List<SpssCheckRule> + List<SpssDatasetRule> + List<SpssOutputRule>
.sql/.json ──► QuestionSqlParser/QuestionJsonParser ──► Map<SPSS变量名, QuestionMapping>
.csv ──► PrototypeFileReaders ──► List<AnswerRecord(sampleKey, question_id, content, table_id)>
                  │
        AnswerPivot.pivot() ──► List<RowContext>  (EAV → wide table, one row per student)
                  │
        RuleEngine.execute(rows, rules)       ← row-level: compute → recode → flag
        SpssDatasetRule.execute(rows)         ← cross-row: SORT + FIRST/LAST
        StudentSpssRuleResultBuilder.build()  ← per-student pass/fail + failure detail
```

### Rule step types (`RuleStep` interface)
- `ComputeRuleStep` — eval arithmetic expr → store in target variable
- `RecodeRuleStep` — map source through recode cases (equals / range / missing / else) → target
- `IfAssignRuleStep` — `IF(cond) var=val` conditional assignment
- `ConditionalRuleStep` — wraps any step with a DO IF condition; tracks nesting via a stack in `findActiveDoIfCondition()`

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

## Critical design rules

1. **`java_preview` is NOT an execution basis.** Execute structured `rule_json`
   or `sps_rule_step` rows only. Three-layer save per rule: `spss_source`
   (traceability) + `rule_json` (execution/edit authority) + `java_preview`
   (human only).

2. **Variable availability chain:** `RuleAvailabilityChecker` tracks
   `DB columns ∪ prior-rule-targets ∪ dataset-rule-variables`. Rules that
   depend on not-yet-available variables are flagged non-executable.

3. **RECODE overwrites the target field's value.** The COMPUTE→RECODE→SELECT IF
   chain depends on RECODE replacing the computed value, not just setting a flag.

4. **SORT CASES must happen before FIRST/LAST assignment.** `SpssDatasetRule.execute()`
   sorts by `sortVariable`, then groups — matching SPSS semantics.

5. **`table_id` filtering is critical.** CSV data may contain answers for
   multiple tables; the wrong `table_id` produces zero rows. `TableIdDetector`
   auto-detects the most frequent `table_id`, or pass it explicitly.

6. **Rule lifecycle:** DRAFT → REVIEWING → PUBLISHED. Execution binds to a
   PUBLISHED `version_id`.

## Rule type classification (v2 `RuleType` enum)

`IDENTITY_CHECK` · `DOCUMENT_CHECK` · `MISSING_CHECK` · `RANGE_CHECK` ·
`OUTCOME_DETERMINATION` · `DUPLICATE_MARK` · `OUTPUT_GROUP` ·
`COMPUTE_INTERMEDIATE` · `CONDITIONAL_BLOCK`.

Classification is by **SPSS syntax pattern**, not by Chinese target names
(see `BlockParser.classifyByRecodePattern` / `classifyV1Rule`). Keep it that
way — keyword-driven classification regresses quickly.

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
- **Reflection into `ConditionalRuleStep.delegate`.** `SpssRuleParser.hasRecodeForTargetInSteps()`
  and `unwrapRecodeStep()` read the private `delegate` field via reflection. Add
  a public accessor to `ConditionalRuleStep` before refactoring that area.
- **Hardcoded paths in `V2Runner.main()`** (`docs/sources/sql/bus_question_*.json`,
  `docs/sources/data/学生证件类型证件号.json`) with no null checks — breaks off-host.
- **Rule version / publish / audit service layer not implemented.** The schema
  (`sps_rule_version`, `sps_rule_change_log`) exists; the Service layer does not.
  Design doc P0 items "save parse results to DB", "publish version", "execute by
  published version_id" are still open.
- **Line-ending noise:** working copy triggers many `LF will be replaced by CRLF`
  warnings. A `.gitattributes` (`* text=auto eol=lf`) is missing.

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
