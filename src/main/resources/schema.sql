ALTER TABLE sps_rule ADD COLUMN IF NOT EXISTS warning_message TEXT AFTER java_preview;
ALTER TABLE sps_rule ADD COLUMN IF NOT EXISTS start_line INT AFTER sort_no;
ALTER TABLE sps_rule ADD COLUMN IF NOT EXISTS end_line INT AFTER start_line;
ALTER TABLE sps_rule ADD COLUMN IF NOT EXISTS line_count INT AFTER end_line;
ALTER TABLE sps_rule ADD COLUMN IF NOT EXISTS segment_title VARCHAR(500) AFTER line_count;
ALTER TABLE sps_rule ADD COLUMN IF NOT EXISTS split_reason VARCHAR(200) AFTER segment_title;
