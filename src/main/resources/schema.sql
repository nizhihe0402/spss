ALTER TABLE sps_rule ADD COLUMN IF NOT EXISTS warning_message TEXT AFTER java_preview;
ALTER TABLE sps_rule ADD COLUMN IF NOT EXISTS source_question_mappings TEXT AFTER source_variables;
ALTER TABLE sps_rule ADD COLUMN IF NOT EXISTS start_line INT AFTER sort_no;
ALTER TABLE sps_rule ADD COLUMN IF NOT EXISTS end_line INT AFTER start_line;
ALTER TABLE sps_rule ADD COLUMN IF NOT EXISTS line_count INT AFTER end_line;
ALTER TABLE sps_rule ADD COLUMN IF NOT EXISTS segment_title VARCHAR(500) AFTER line_count;
ALTER TABLE sps_rule ADD COLUMN IF NOT EXISTS split_reason VARCHAR(200) AFTER segment_title;
ALTER TABLE sps_rule ADD COLUMN IF NOT EXISTS execution_chain LONGTEXT AFTER java_preview;

ALTER TABLE sps_script ADD COLUMN IF NOT EXISTS project_id BIGINT AFTER table_id;
ALTER TABLE sps_script ADD COLUMN IF NOT EXISTS project_type VARCHAR(20) AFTER project_id;
ALTER TABLE sps_script ADD COLUMN IF NOT EXISTS year VARCHAR(10) AFTER project_type;

CREATE TABLE IF NOT EXISTS sps_script_question_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    script_id BIGINT NOT NULL,
    variable_name VARCHAR(100) NOT NULL,
    question_id BIGINT NOT NULL,
    question_content VARCHAR(1000),
    source_table_id BIGINT,
    export_content VARCHAR(300),
    sort_no INT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_script (script_id),
    INDEX idx_question (question_id),
    INDEX idx_variable (variable_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
