# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A rule engine that parses a **restricted subset of SPSS `.sps`** data-audit scripts and
executes them as Java rules against school-health form data (学生常见病 / 学校卫生监测).
It is **not** a general IBM SPSS interpreter. Spring Boot 2.7 web app + a CLI prototype.

> **`GLM.md` is the authoritative, code-verified architecture doc** — read it for the deep
> dive (supported syntax/functions with examples, execution semantics, known correctness
> limits). `AGENTS.md` and `docs/SPSS规则解析与清洗落地详细设计.md` describe an old single-file
> prototype and are **stale** — do not trust them.

## Build / Run / Test

```bash
# Toolchain: Java 8, Spring Boot 2.7.18, Maven, JUnit 5. Keep new code Java-8 compatible
# (explicit generics, no var / records / List.of()).

mvn -q -DskipTests package                 # build
java -Dfile.encoding=UTF-8 -jar target/spss-rule-engine-codex-*.jar   # run web app (port 8080)

mvn -q test                                # run all tests
mvn -q test -Dtest=SpssCheckTest           # single test class
mvn -q test -Dtest=SpssCheckTest#methodName   # single test method

# CLI prototype against one table's fixtures (no DB needed):
java -Dfile.encoding=UTF-8 -cp target/classes \
  com.gxaysoft.project.spsscheck.prototype.PrototypeCli \
  docs/sources/sps/表1-3.sps docs/sources/sql/相关表数据.sql docs/sources/cvs/表1-3.csv

# Batch scanner: parse + execute every .sps, print a summary (has hardcoded fallback paths):
java -Dfile.encoding=UTF-8 -cp target/classes com.gxaysoft.project.spsscheck.V2Runner
```

There is **no linter** configured. No golden-file harness beyond fixtures under `docs/sources/`.

## Database

- Config in `src/main/resources/application.yml`: MySQL at `localhost:3306/h2025`, user `root`,
  password from env `DB_PASSWORD` (default `123456`).
- `spring.sql.init.mode=always` runs `src/main/resources/schema.sql` (idempotent
  `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`) on **every** startup. DDL of record is
  `docs/spss_rule_schema.sql`.
- **The web app + persistence/execution services require a live MySQL.** The `PrototypeCli`
  path runs entirely off fixtures without a DB.

## Architecture (the big picture)

**Parse pipeline** (`engine/parser`):
`SpssParser.parse(text)` is the entry point (static, `final`). It aggregates three **static**
methods on the util class `RuleParser` — `parseRules` / `parseDatasetRules` / `parseOutputRules`
— into a `ParsedScript { rules, datasetRules, outputRules, unsupportedStatements }`.

- Each `Rule` holds a `List<Step>`; each `Step` = optional condition + one `StepAction`
  (`ComputeAction` | `RecodeAction` | `IfAssignAction`). DO IF conditions are resolved **at
  parse time** by `RuleParser.findActiveDoIfCondition()` and stored on the `Step`.
- `BlockClassifier` assigns a business `RuleType` (RANGE_CHECK, OUTCOME_DETERMINATION, …) by
  SPSS syntax pattern.

**Execution** (`engine/executor`, `expression`):
`RuleExecutor.execute(rows, rules)` iterates per-row × per-rule × per-step.
`ArithmeticExpression` (recursive-descent, `BigDecimal`, null-propagates SPSS missing) evaluates
COMPUTE; `ConditionExpression` evaluates conditions. `DatasetRule` does cross-row SORT +
FIRST/LAST; `OutputRule` does SELECT IF grouping into output sheets.

**DB-backed data flow** (orchestrated by `persistence/ExecutionService`):
EAV answers (`bus_doctor_answer`: student × `question_id` → `content`) → `AnswerPivot.pivot()`
→ one wide-table `RowContext` per student → `StudentInfoEnricher` adds demographics →
`RuleExecutor` runs → `StudentSpssRuleResultBuilder` shapes the result. Persistence is
**hand-written JDBC** (`SpsRepository`, `DbConnection`), no ORM.

**Variable mapping:** `bus_question.export_content` → SPSS variable name, gated by
`QuestionVariableNameSelector` (accepts only `[A-Za-z][A-Za-z0-9_]*`, filtering out Chinese/
malformed values). Use it wherever a var name is derived from `bus_question`.

**Packages:** `engine/` (unified parser+executor, former v1+v2) · `expression/` · `io/`
(readers, pivot, enrichers, `TableIdDetector`) · `parser/` (question SQL/JSON parsers, `SpssUtil`)
· `persistence/` (JDBC services) · `validation/` · `web/` (~14 controllers + `GlobalExceptionHandler`).
Root holds entry points: `SpsApplication` (web), `PrototypeCli`, `V2Runner`, and `SpsUpload*`
dev scaffolds.

## Key patterns & gotchas

- **Three-layer rule storage per rule:** `spss_source` (traceability) + `rule_json`
  (execution/edit authority) + `java_preview` (human-readable only). **`java_preview` is NOT an
  execution basis** — execute `rule_json` / `sps_rule_step` rows only.
- **`checkRule` is not set during parsing.** Each consumer (`ExecutionService`,
  `SnippetController`, `SpsUploadSimulator`, `StudentValidationResultBuilder`) derives it via its
  own `deriveCheckRule`. Pure `SpssParser` + `RuleExecutor` paths (`V2Runner`, `PrototypeCli`)
  therefore emit **no** check flags.
- **`RECODE` overwrites the target's value** (the COMPUTE→RECODE→SELECT IF chain relies on it).
  **SORT CASES must precede FIRST/LAST** in `DatasetRule`.
- **`table_id` filtering is critical** — wrong `table_id` yields zero rows. `TableIdDetector`
  auto-detects the most frequent one. The CSV sample key **varies by table**: 表1-x uses `code`,
  表2-x uses `student_id` (see `AnswerTableType`, `PrototypeFileReaders`).
- **`ConditionStack` is dead code** (only `ConditionStackTest` uses it); the real DO IF handling
  is inline in `RuleParser`. `ParsedScript.unsupportedStatements` is declared but never populated.
- **Known correctness limits (documented in `GLM.md`)** worth knowing before touching the engine:
  `ConditionExpression` can't handle parenthesized booleans or general `NOT(...)`, so `DO IF … ELSE`
  and `ELSE IF` branches don't execute; `ArithmeticExpression` uses a thread-unsafe static
  `SimpleDateFormat` hardcoded to `dd/MM/yyyy`, and DATEDIFF month/year are 30/365 approximations.
- **Data caution:** some files under `docs/sources/cvs/` and `docs/sources/data/` look like real
  student PII. Do not commit additional real PII; prefer synthetic fixtures.
