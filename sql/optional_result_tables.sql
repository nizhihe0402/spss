-- 可选：如果后续要把校验结果落库，再执行本 SQL。
-- 当前完整包默认不依赖这些表。

CREATE TABLE IF NOT EXISTS sps_validate_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  script_id BIGINT NULL,
  table_id BIGINT NULL,
  project_id BIGINT NULL,
  year VARCHAR(10) NULL,
  total_rows INT DEFAULT 0,
  student_count INT DEFAULT 0,
  passed_count INT DEFAULT 0,
  failed_count INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SPSS执行前数据校验结果主表';

CREATE TABLE IF NOT EXISTS sps_validate_result_student (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  result_id BIGINT NOT NULL,
  student_id BIGINT NULL,
  student_name VARCHAR(100) NULL,
  passed_flag CHAR(1) NOT NULL COMMENT '1通过 0未通过',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_result_id(result_id),
  INDEX idx_student_id(student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SPSS执行前数据校验学生结果表';

CREATE TABLE IF NOT EXISTS sps_validate_result_violation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  result_id BIGINT NOT NULL,
  student_id BIGINT NULL,
  line_no INT NULL,
  rule_code VARCHAR(64) NOT NULL,
  rule_name VARCHAR(100) NOT NULL,
  field_name VARCHAR(100) NULL,
  message VARCHAR(1000) NULL,
  table_id BIGINT NULL,
  project_id BIGINT NULL,
  year VARCHAR(10) NULL,
  question_id BIGINT NULL,
  option_id BIGINT NULL,
  content VARCHAR(500) NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_result_id(result_id),
  INDEX idx_student_id(student_id),
  INDEX idx_rule_code(rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SPSS执行前数据校验违规明细表';
