package com.gxaysoft.project.spsscheck.persistence;

import java.sql.Connection;
import java.sql.Statement;

public class SchemaInitializer {

    public static void ensureTables(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS sps_script (" +
                "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  table_code VARCHAR(50)," +
                "  table_id BIGINT," +
                "  script_name VARCHAR(200)," +
                "  script_content LONGTEXT," +
                "  parse_status VARCHAR(30)," +
                "  parse_message TEXT," +
                "  version_no INT DEFAULT 1," +
                "  status VARCHAR(30) DEFAULT 'DRAFT'," +
                "  created_by VARCHAR(64)," +
                "  created_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "  updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS sps_rule (" +
                "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  script_id BIGINT NOT NULL," +
                "  version_id BIGINT," +
                "  rule_code VARCHAR(100) NOT NULL," +
                "  rule_name VARCHAR(300)," +
                "  rule_type VARCHAR(50)," +
                "  rule_category VARCHAR(50)," +
                "  enabled TINYINT DEFAULT 1," +
                "  editable TINYINT DEFAULT 1," +
                "  target_variable VARCHAR(100)," +
                "  source_variables VARCHAR(1000)," +
                "  action_type VARCHAR(50)," +
                "  affect_clean TINYINT DEFAULT 0," +
                "  spss_source LONGTEXT," +
                "  rule_json LONGTEXT," +
                "  java_preview LONGTEXT," +
                "  parse_confidence VARCHAR(20)," +
                "  sort_no INT," +
                "  start_line INT," +
                "  end_line INT," +
                "  line_count INT," +
                "  segment_title VARCHAR(500)," +
                "  split_reason VARCHAR(200)," +
                "  created_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "  updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "  INDEX idx_script (script_id)," +
                "  INDEX idx_version (version_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS sps_rule_step (" +
                "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  rule_id BIGINT NOT NULL," +
                "  step_no INT NOT NULL," +
                "  step_type VARCHAR(50) NOT NULL," +
                "  condition_text TEXT," +
                "  source_variable VARCHAR(100)," +
                "  target_variable VARCHAR(100)," +
                "  expression_text TEXT," +
                "  assign_value VARCHAR(100)," +
                "  recode_json LONGTEXT," +
                "  raw_spss LONGTEXT," +
                "  step_json LONGTEXT," +
                "  created_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "  updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "  INDEX idx_rule (rule_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS sps_output_rule (" +
                "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  script_id BIGINT NOT NULL," +
                "  version_id BIGINT," +
                "  output_code VARCHAR(100)," +
                "  output_name VARCHAR(300)," +
                "  output_type VARCHAR(50)," +
                "  select_condition TEXT," +
                "  source_variables VARCHAR(1000)," +
                "  save_path_original VARCHAR(1000)," +
                "  enabled TINYINT DEFAULT 1," +
                "  spss_source LONGTEXT," +
                "  rule_json LONGTEXT," +
                "  java_preview LONGTEXT," +
                "  sort_no INT," +
                "  created_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "  updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "  INDEX idx_script (script_id)," +
                "  INDEX idx_version (version_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS sps_unsupported_statement (" +
                "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  script_id BIGINT NOT NULL," +
                "  version_id BIGINT," +
                "  statement_type VARCHAR(100)," +
                "  raw_spss LONGTEXT," +
                "  reason VARCHAR(500)," +
                "  risk_level VARCHAR(20)," +
                "  sort_no INT," +
                "  created_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // Ensure columns exist for upgraded databases
            try { stmt.execute("ALTER TABLE sps_rule ADD COLUMN warning_message TEXT AFTER java_preview"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE sps_rule ADD COLUMN start_line INT AFTER sort_no"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE sps_rule ADD COLUMN end_line INT AFTER start_line"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE sps_rule ADD COLUMN line_count INT AFTER end_line"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE sps_rule ADD COLUMN segment_title VARCHAR(500) AFTER line_count"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE sps_rule ADD COLUMN split_reason VARCHAR(200) AFTER segment_title"); } catch (Exception ignored) {}
        }
    }
}
