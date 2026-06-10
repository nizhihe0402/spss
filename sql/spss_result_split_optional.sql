-- 可选：如需持久化执行结果，可以新增该表。
-- 不影响仅页面展示。
CREATE TABLE IF NOT EXISTS sps_execute_validate_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  script_id BIGINT NULL,
  project_id BIGINT NULL,
  table_id BIGINT NULL,
  year VARCHAR(10) NULL,
  student_id BIGINT NOT NULL,
  student_name VARCHAR(100) NULL,
  result_status VARCHAR(20) NOT NULL COMMENT 'PASS/FAIL',
  rule_codes VARCHAR(1000) NULL,
  rule_names VARCHAR(2000) NULL,
  messages TEXT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SPSS规则执行前数据校验结果';
