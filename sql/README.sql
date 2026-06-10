-- 本包不强制改表。
-- 如果你要持久化执行结果，可新增以下两张表。

CREATE TABLE IF NOT EXISTS sps_execute_student_result (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  execute_id BIGINT NULL COMMENT '执行批次ID',
  script_id BIGINT NULL COMMENT '脚本ID',
  project_id BIGINT NULL COMMENT '项目ID',
  table_id BIGINT NULL COMMENT '表ID',
  student_id BIGINT NOT NULL COMMENT '学生ID',
  student_name VARCHAR(100) NULL COMMENT '学生姓名',
  pass_flag CHAR(1) NOT NULL COMMENT '是否通过：1通过，0未通过',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_execute_id (execute_id),
  KEY idx_student_id (student_id),
  KEY idx_project_table (project_id, table_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='SPSS规则执行学生结果';

CREATE TABLE IF NOT EXISTS sps_execute_violation_detail (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  execute_result_id BIGINT NOT NULL COMMENT 'sps_execute_student_result.id',
  student_id BIGINT NOT NULL COMMENT '学生ID',
  rule_code VARCHAR(100) NOT NULL COMMENT '规则编码',
  rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
  message VARCHAR(1000) NULL COMMENT '原因说明',
  question_id BIGINT NULL COMMENT '题目ID',
  option_id BIGINT NULL COMMENT '选项ID',
  content VARCHAR(255) NULL COMMENT '答案内容',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_result_id (execute_result_id),
  KEY idx_student_id (student_id),
  KEY idx_rule_code (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='SPSS规则执行违规明细';
