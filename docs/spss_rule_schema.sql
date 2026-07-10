-- SPSS规则解析、版本、执行落地建议表结构
-- MySQL 5.7 可用。若使用人大金仓/PostgreSQL，可将 LONGTEXT 替换为 TEXT/JSONB。

CREATE TABLE sps_script (
    id              BIGINT PRIMARY KEY,
    table_code      VARCHAR(50),
    table_id        BIGINT,
    script_name     VARCHAR(200),
    script_content  LONGTEXT,
    parse_status    VARCHAR(30),
    parse_message   TEXT,
    version_no      INT DEFAULT 1,
    status          VARCHAR(30),
    created_by      VARCHAR(64),
    created_time    DATETIME,
    updated_time    DATETIME
);

CREATE TABLE sps_script_question_mapping (
    id                  BIGINT PRIMARY KEY,
    script_id            BIGINT NOT NULL,
    variable_name        VARCHAR(100) NOT NULL,
    question_id          BIGINT NOT NULL,
    question_content     VARCHAR(1000),
    source_table_id      BIGINT,
    export_content       VARCHAR(300),
    sort_no              INT,
    created_time         DATETIME,
    INDEX idx_sps_script_question_script (script_id),
    INDEX idx_sps_script_question_question (question_id),
    INDEX idx_sps_script_question_variable (variable_name)
);

CREATE TABLE sps_rule_version (
    id              BIGINT PRIMARY KEY,
    script_id        BIGINT NOT NULL,
    version_no       INT NOT NULL,
    version_name     VARCHAR(200),
    status           VARCHAR(30),
    remark           TEXT,
    created_by       VARCHAR(64),
    created_time     DATETIME,
    published_by     VARCHAR(64),
    published_time   DATETIME
);

CREATE TABLE sps_rule (
    id                  BIGINT PRIMARY KEY,
    script_id            BIGINT NOT NULL,
    version_id           BIGINT,
    rule_code            VARCHAR(100) NOT NULL,
    rule_name            VARCHAR(300),
    rule_type            VARCHAR(50),
    rule_category        VARCHAR(50),
    enabled              TINYINT DEFAULT 1,
    editable             TINYINT DEFAULT 1,
    target_variable      VARCHAR(100),
    source_variables     VARCHAR(1000),
    source_question_mappings TEXT,
    correction_enabled   TINYINT DEFAULT 0,
    correction_type      VARCHAR(100),
    correction_variables VARCHAR(500),
    correction_source    VARCHAR(500),
    correction_strategy  VARCHAR(1000),
    correction_apply_stage VARCHAR(100),
    correction_write_clean TINYINT DEFAULT 0,
    correction_write_source TINYINT DEFAULT 0,
    correction_description TEXT,
    depend_variables     VARCHAR(1000),
    generated_variables  VARCHAR(1000),
    action_type          VARCHAR(50),
    affect_clean         TINYINT DEFAULT 0,
    spss_source          LONGTEXT,
    rule_json            LONGTEXT,
    java_preview         LONGTEXT,
    parse_confidence     VARCHAR(20),
    warning_message      TEXT,
    sort_no              INT,
    created_time         DATETIME,
    updated_time         DATETIME,
    INDEX idx_sps_rule_version (version_id),
    INDEX idx_sps_rule_script (script_id),
    INDEX idx_sps_rule_type (rule_type)
);

CREATE TABLE sps_rule_step (
    id              BIGINT PRIMARY KEY,
    rule_id         BIGINT NOT NULL,
    step_no         INT NOT NULL,
    step_type       VARCHAR(50) NOT NULL,
    condition_text  TEXT,
    source_variable VARCHAR(100),
    target_variable VARCHAR(100),
    expression_text TEXT,
    assign_value    VARCHAR(100),
    recode_json     LONGTEXT,
    raw_spss        LONGTEXT,
    step_json       LONGTEXT,
    created_time    DATETIME,
    updated_time    DATETIME,
    INDEX idx_sps_rule_step_rule (rule_id)
);

CREATE TABLE sps_output_rule (
    id                  BIGINT PRIMARY KEY,
    script_id            BIGINT NOT NULL,
    version_id           BIGINT,
    output_code          VARCHAR(100),
    output_name          VARCHAR(300),
    output_type          VARCHAR(50),
    select_condition     TEXT,
    source_variables     VARCHAR(1000),
    save_path_original   VARCHAR(1000),
    enabled              TINYINT DEFAULT 1,
    spss_source          LONGTEXT,
    rule_json            LONGTEXT,
    java_preview         LONGTEXT,
    sort_no              INT,
    created_time         DATETIME,
    updated_time         DATETIME,
    INDEX idx_sps_output_version (version_id),
    INDEX idx_sps_output_script (script_id)
);

CREATE TABLE sps_unsupported_statement (
    id              BIGINT PRIMARY KEY,
    script_id        BIGINT NOT NULL,
    version_id       BIGINT,
    statement_type   VARCHAR(100),
    raw_spss         LONGTEXT,
    reason           VARCHAR(500),
    risk_level       VARCHAR(20),
    sort_no          INT,
    created_time     DATETIME
);

CREATE TABLE sps_rule_change_log (
    id              BIGINT PRIMARY KEY,
    rule_id          BIGINT,
    version_id       BIGINT,
    change_type      VARCHAR(50),
    field_name       VARCHAR(100),
    old_value        LONGTEXT,
    new_value        LONGTEXT,
    changed_by       VARCHAR(64),
    changed_time     DATETIME,
    change_reason    VARCHAR(500)
);

CREATE TABLE sps_run_batch (
    id              BIGINT PRIMARY KEY,
    script_id        BIGINT NOT NULL,
    rule_version_id  BIGINT,
    table_code       VARCHAR(50),
    table_id         BIGINT,
    project_id       BIGINT,
    year             VARCHAR(10),
    status           VARCHAR(30),
    total_count      INT,
    clean_count      INT,
    error_count      INT,
    created_time     DATETIME,
    finished_time    DATETIME
);

CREATE TABLE sps_run_row_temp (
    id              BIGINT PRIMARY KEY,
    run_id          BIGINT NOT NULL,
    row_key         VARCHAR(100) NOT NULL,
    row_no          INT,
    row_json        LONGTEXT,
    created_time    DATETIME,
    INDEX idx_sps_temp_run_row (run_id, row_key),
    INDEX idx_sps_temp_run_no (run_id, row_no)
);

CREATE TABLE sps_run_var_temp (
    id              BIGINT PRIMARY KEY,
    run_id          BIGINT NOT NULL,
    row_key         VARCHAR(100) NOT NULL,
    row_no          INT,
    var_name        VARCHAR(100) NOT NULL,
    var_value       VARCHAR(500),
    var_num         DECIMAL(20,6),
    created_time    DATETIME,
    INDEX idx_sps_var_run_name_value (run_id, var_name, var_value),
    INDEX idx_sps_var_run_row_name (run_id, row_key, var_name)
);

CREATE TABLE sps_clean_unique_index (
    id              BIGINT PRIMARY KEY,
    project_id      BIGINT NOT NULL,
    table_id        BIGINT NOT NULL,
    year            VARCHAR(10),
    rule_code       VARCHAR(100) NOT NULL,
    dedup_field     VARCHAR(100) NOT NULL,
    dedup_value     VARCHAR(300) NOT NULL,
    owner_row_key   VARCHAR(100) NOT NULL,
    owner_batch_id  BIGINT,
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    created_time    DATETIME,
    updated_time    DATETIME,
    UNIQUE KEY uk_sps_clean_unique (project_id, table_id, year, rule_code, dedup_field, dedup_value)
);

CREATE TABLE sps_check_error_detail (
    id              BIGINT PRIMARY KEY,
    run_id          BIGINT NOT NULL,
    row_key         VARCHAR(100) NOT NULL,
    rule_code       VARCHAR(100),
    rule_name       VARCHAR(300),
    output_name     VARCHAR(300),
    field_values    LONGTEXT,
    row_json        LONGTEXT,
    created_time    DATETIME,
    INDEX idx_sps_error_run (run_id),
    INDEX idx_sps_error_rule (run_id, rule_code)
);
