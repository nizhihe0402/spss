-- ============================================================
-- SPSS 规则引擎 — 全部 sps_*_clean / sps_*_fail 表结构
-- 策略: 镜像源表列 + 业务元数据列
-- 幂等: 先 DROP IF EXISTS 再 CREATE ... LIKE + ALTER
-- 表名规范: sps_<源表名>_clean / sps_<源表名>_fail
-- （代码侧 RuleExecutionPersistenceService.cleanTableName/failTableName 同步）
-- ============================================================

-- ============================================================
-- 1. bus_user_answer
-- ============================================================
DROP TABLE IF EXISTS sps_bus_user_answer_clean;
CREATE TABLE sps_bus_user_answer_clean LIKE bus_user_answer;
ALTER TABLE sps_bus_user_answer_clean
    ADD COLUMN is_valid       TINYINT   DEFAULT 1   COMMENT '1有效 0无效',
    ADD COLUMN invalid_reason JSON      NULL         COMMENT '无效原因',
    ADD COLUMN clean_task_id  BIGINT    NULL         COMMENT '清洗批次',
    ADD COLUMN clean_time     DATETIME  NULL         COMMENT '清洗时间',
    ADD COLUMN source_id      BIGINT    NULL         COMMENT '源answer表id',
    ADD UNIQUE INDEX uk_source (source_id);

DROP TABLE IF EXISTS sps_bus_user_answer_fail;
CREATE TABLE sps_bus_user_answer_fail LIKE bus_user_answer;
ALTER TABLE sps_bus_user_answer_fail
    ADD COLUMN clean_task_id  BIGINT       NULL COMMENT '清洗批次',
    ADD COLUMN source_id      BIGINT       NULL COMMENT '源answer表id',
    ADD COLUMN division_id    BIGINT       NULL COMMENT '区县(执行参数)',
    ADD COLUMN school_id      BIGINT       NULL COMMENT '学校(执行参数)',
    ADD COLUMN rule_code      VARCHAR(50)  NULL COMMENT '规则编号',
    ADD COLUMN rule_target    VARCHAR(100) NULL COMMENT '规则目标变量',
    ADD COLUMN rule_name      VARCHAR(300) NULL COMMENT '规则名称',
    ADD COLUMN failed_value   VARCHAR(500) NULL COMMENT '失败值',
    ADD COLUMN reason         VARCHAR(500) NULL COMMENT '失败原因',
    ADD COLUMN reason_detail  LONGTEXT     NULL COMMENT '失败详情';

-- ============================================================
-- 2. bus_doctor_answer
-- ============================================================
DROP TABLE IF EXISTS sps_bus_doctor_answer_clean;
CREATE TABLE sps_bus_doctor_answer_clean LIKE bus_doctor_answer;
ALTER TABLE sps_bus_doctor_answer_clean
    ADD COLUMN is_valid       TINYINT   DEFAULT 1   COMMENT '1有效 0无效',
    ADD COLUMN invalid_reason JSON      NULL         COMMENT '无效原因',
    ADD COLUMN clean_task_id  BIGINT    NULL         COMMENT '清洗批次',
    ADD COLUMN clean_time     DATETIME  NULL         COMMENT '清洗时间',
    ADD COLUMN source_id      BIGINT    NULL         COMMENT '源answer表id',
    ADD UNIQUE INDEX uk_source (source_id);

DROP TABLE IF EXISTS sps_bus_doctor_answer_fail;
CREATE TABLE sps_bus_doctor_answer_fail LIKE bus_doctor_answer;
ALTER TABLE sps_bus_doctor_answer_fail
    ADD COLUMN clean_task_id  BIGINT       NULL COMMENT '清洗批次',
    ADD COLUMN source_id      BIGINT       NULL COMMENT '源answer表id',
    ADD COLUMN division_id    BIGINT       NULL COMMENT '区县(执行参数)',
    ADD COLUMN school_id      BIGINT       NULL COMMENT '学校(执行参数)',
    ADD COLUMN rule_code      VARCHAR(50)  NULL COMMENT '规则编号',
    ADD COLUMN rule_target    VARCHAR(100) NULL COMMENT '规则目标变量',
    ADD COLUMN rule_name      VARCHAR(300) NULL COMMENT '规则名称',
    ADD COLUMN failed_value   VARCHAR(500) NULL COMMENT '失败值',
    ADD COLUMN reason         VARCHAR(500) NULL COMMENT '失败原因',
    ADD COLUMN reason_detail  LONGTEXT     NULL COMMENT '失败详情';

-- ============================================================
-- 3. bus_doctor_answer_intervene
-- ============================================================
DROP TABLE IF EXISTS sps_bus_doctor_answer_intervene_clean;
CREATE TABLE sps_bus_doctor_answer_intervene_clean LIKE bus_doctor_answer_intervene;
ALTER TABLE sps_bus_doctor_answer_intervene_clean
    ADD COLUMN is_valid       TINYINT   DEFAULT 1   COMMENT '1有效 0无效',
    ADD COLUMN invalid_reason JSON      NULL         COMMENT '无效原因',
    ADD COLUMN clean_task_id  BIGINT    NULL         COMMENT '清洗批次',
    ADD COLUMN clean_time     DATETIME  NULL         COMMENT '清洗时间',
    ADD COLUMN source_id      BIGINT    NULL         COMMENT '源answer表id',
    ADD UNIQUE INDEX uk_source (source_id);

DROP TABLE IF EXISTS sps_bus_doctor_answer_intervene_fail;
CREATE TABLE sps_bus_doctor_answer_intervene_fail LIKE bus_doctor_answer_intervene;
ALTER TABLE sps_bus_doctor_answer_intervene_fail
    ADD COLUMN clean_task_id  BIGINT       NULL COMMENT '清洗批次',
    ADD COLUMN source_id      BIGINT       NULL COMMENT '源answer表id',
    ADD COLUMN division_id    BIGINT       NULL COMMENT '区县(执行参数)',
    ADD COLUMN school_id      BIGINT       NULL COMMENT '学校(执行参数)',
    ADD COLUMN rule_code      VARCHAR(50)  NULL COMMENT '规则编号',
    ADD COLUMN rule_target    VARCHAR(100) NULL COMMENT '规则目标变量',
    ADD COLUMN rule_name      VARCHAR(300) NULL COMMENT '规则名称',
    ADD COLUMN failed_value   VARCHAR(500) NULL COMMENT '失败值',
    ADD COLUMN reason         VARCHAR(500) NULL COMMENT '失败原因',
    ADD COLUMN reason_detail  LONGTEXT     NULL COMMENT '失败详情';

-- ============================================================
-- 4. bus_doctor_answer_retest
-- ============================================================
DROP TABLE IF EXISTS sps_bus_doctor_answer_retest_clean;
CREATE TABLE sps_bus_doctor_answer_retest_clean LIKE bus_doctor_answer_retest;
ALTER TABLE sps_bus_doctor_answer_retest_clean
    ADD COLUMN is_valid       TINYINT   DEFAULT 1   COMMENT '1有效 0无效',
    ADD COLUMN invalid_reason JSON      NULL         COMMENT '无效原因',
    ADD COLUMN clean_task_id  BIGINT    NULL         COMMENT '清洗批次',
    ADD COLUMN clean_time     DATETIME  NULL         COMMENT '清洗时间',
    ADD COLUMN source_id      BIGINT    NULL         COMMENT '源answer表id',
    ADD UNIQUE INDEX uk_source (source_id);

DROP TABLE IF EXISTS sps_bus_doctor_answer_retest_fail;
CREATE TABLE sps_bus_doctor_answer_retest_fail LIKE bus_doctor_answer_retest;
ALTER TABLE sps_bus_doctor_answer_retest_fail
    ADD COLUMN clean_task_id  BIGINT       NULL COMMENT '清洗批次',
    ADD COLUMN source_id      BIGINT       NULL COMMENT '源answer表id',
    ADD COLUMN division_id    BIGINT       NULL COMMENT '区县(执行参数)',
    ADD COLUMN school_id      BIGINT       NULL COMMENT '学校(执行参数)',
    ADD COLUMN rule_code      VARCHAR(50)  NULL COMMENT '规则编号',
    ADD COLUMN rule_target    VARCHAR(100) NULL COMMENT '规则目标变量',
    ADD COLUMN rule_name      VARCHAR(300) NULL COMMENT '规则名称',
    ADD COLUMN failed_value   VARCHAR(500) NULL COMMENT '失败值',
    ADD COLUMN reason         VARCHAR(500) NULL COMMENT '失败原因',
    ADD COLUMN reason_detail  LONGTEXT     NULL COMMENT '失败详情';

-- ============================================================
-- 5. bus_doctor_answer_retest_intervene
-- ============================================================
DROP TABLE IF EXISTS sps_bus_doctor_answer_retest_intervene_clean;
CREATE TABLE sps_bus_doctor_answer_retest_intervene_clean LIKE bus_doctor_answer_retest_intervene;
ALTER TABLE sps_bus_doctor_answer_retest_intervene_clean
    ADD COLUMN is_valid       TINYINT   DEFAULT 1   COMMENT '1有效 0无效',
    ADD COLUMN invalid_reason JSON      NULL         COMMENT '无效原因',
    ADD COLUMN clean_task_id  BIGINT    NULL         COMMENT '清洗批次',
    ADD COLUMN clean_time     DATETIME  NULL         COMMENT '清洗时间',
    ADD COLUMN source_id      BIGINT    NULL         COMMENT '源answer表id',
    ADD UNIQUE INDEX uk_source (source_id);

DROP TABLE IF EXISTS sps_bus_doctor_answer_retest_intervene_fail;
CREATE TABLE sps_bus_doctor_answer_retest_intervene_fail LIKE bus_doctor_answer_retest_intervene;
ALTER TABLE sps_bus_doctor_answer_retest_intervene_fail
    ADD COLUMN clean_task_id  BIGINT       NULL COMMENT '清洗批次',
    ADD COLUMN source_id      BIGINT       NULL COMMENT '源answer表id',
    ADD COLUMN division_id    BIGINT       NULL COMMENT '区县(执行参数)',
    ADD COLUMN school_id      BIGINT       NULL COMMENT '学校(执行参数)',
    ADD COLUMN rule_code      VARCHAR(50)  NULL COMMENT '规则编号',
    ADD COLUMN rule_target    VARCHAR(100) NULL COMMENT '规则目标变量',
    ADD COLUMN rule_name      VARCHAR(300) NULL COMMENT '规则名称',
    ADD COLUMN failed_value   VARCHAR(500) NULL COMMENT '失败值',
    ADD COLUMN reason         VARCHAR(500) NULL COMMENT '失败原因',
    ADD COLUMN reason_detail  LONGTEXT     NULL COMMENT '失败详情';

-- ============================================================
-- 6. bus_student_answer
-- ============================================================
DROP TABLE IF EXISTS sps_bus_student_answer_clean;
CREATE TABLE sps_bus_student_answer_clean LIKE bus_student_answer;
ALTER TABLE sps_bus_student_answer_clean
    ADD COLUMN is_valid       TINYINT   DEFAULT 1   COMMENT '1有效 0无效',
    ADD COLUMN invalid_reason JSON      NULL         COMMENT '无效原因',
    ADD COLUMN clean_task_id  BIGINT    NULL         COMMENT '清洗批次',
    ADD COLUMN clean_time     DATETIME  NULL         COMMENT '清洗时间',
    ADD COLUMN source_id      BIGINT    NULL         COMMENT '源answer表id',
    ADD UNIQUE INDEX uk_source (source_id);

DROP TABLE IF EXISTS sps_bus_student_answer_fail;
CREATE TABLE sps_bus_student_answer_fail LIKE bus_student_answer;
ALTER TABLE sps_bus_student_answer_fail
    ADD COLUMN clean_task_id  BIGINT       NULL COMMENT '清洗批次',
    ADD COLUMN source_id      BIGINT       NULL COMMENT '源answer表id',
    ADD COLUMN division_id    BIGINT       NULL COMMENT '区县(执行参数)',
    ADD COLUMN school_id      BIGINT       NULL COMMENT '学校(执行参数)',
    ADD COLUMN rule_code      VARCHAR(50)  NULL COMMENT '规则编号',
    ADD COLUMN rule_target    VARCHAR(100) NULL COMMENT '规则目标变量',
    ADD COLUMN rule_name      VARCHAR(300) NULL COMMENT '规则名称',
    ADD COLUMN failed_value   VARCHAR(500) NULL COMMENT '失败值',
    ADD COLUMN reason         VARCHAR(500) NULL COMMENT '失败原因',
    ADD COLUMN reason_detail  LONGTEXT     NULL COMMENT '失败详情';

-- ============================================================
-- 7. bus_student_answer_intervene
-- ============================================================
DROP TABLE IF EXISTS sps_bus_student_answer_intervene_clean;
CREATE TABLE sps_bus_student_answer_intervene_clean LIKE bus_student_answer_intervene;
ALTER TABLE sps_bus_student_answer_intervene_clean
    ADD COLUMN is_valid       TINYINT   DEFAULT 1   COMMENT '1有效 0无效',
    ADD COLUMN invalid_reason JSON      NULL         COMMENT '无效原因',
    ADD COLUMN clean_task_id  BIGINT    NULL         COMMENT '清洗批次',
    ADD COLUMN clean_time     DATETIME  NULL         COMMENT '清洗时间',
    ADD COLUMN source_id      BIGINT    NULL         COMMENT '源answer表id',
    ADD UNIQUE INDEX uk_source (source_id);

DROP TABLE IF EXISTS sps_bus_student_answer_intervene_fail;
CREATE TABLE sps_bus_student_answer_intervene_fail LIKE bus_student_answer_intervene;
ALTER TABLE sps_bus_student_answer_intervene_fail
    ADD COLUMN clean_task_id  BIGINT       NULL COMMENT '清洗批次',
    ADD COLUMN source_id      BIGINT       NULL COMMENT '源answer表id',
    ADD COLUMN division_id    BIGINT       NULL COMMENT '区县(执行参数)',
    ADD COLUMN school_id      BIGINT       NULL COMMENT '学校(执行参数)',
    ADD COLUMN rule_code      VARCHAR(50)  NULL COMMENT '规则编号',
    ADD COLUMN rule_target    VARCHAR(100) NULL COMMENT '规则目标变量',
    ADD COLUMN rule_name      VARCHAR(300) NULL COMMENT '规则名称',
    ADD COLUMN failed_value   VARCHAR(500) NULL COMMENT '失败值',
    ADD COLUMN reason         VARCHAR(500) NULL COMMENT '失败原因',
    ADD COLUMN reason_detail  LONGTEXT     NULL COMMENT '失败详情';
