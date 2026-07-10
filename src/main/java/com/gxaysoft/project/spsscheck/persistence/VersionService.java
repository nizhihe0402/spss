package com.gxaysoft.project.spsscheck.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class VersionService {
    private static final Logger log = LoggerFactory.getLogger(VersionService.class);
    private final JdbcTemplate jdbc;

    public VersionService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public Long publishVersion(Long scriptId) {
        log.info("发布版本: scriptId={}", scriptId);
        Integer maxVersion = jdbc.queryForObject(
                "SELECT COALESCE(MAX(version_no), 0) FROM sps_rule_version WHERE script_id=?",
                Integer.class, scriptId);
        int newVersion = (maxVersion == null ? 0 : maxVersion) + 1;

        jdbc.update("INSERT INTO sps_rule_version (script_id, version_no, status, created_time) " +
                "VALUES (?, ?, 'PUBLISHED', NOW())", scriptId, newVersion);

        Long versionId = jdbc.queryForObject(
                "SELECT id FROM sps_rule_version WHERE script_id=? AND version_no=?",
                Long.class, scriptId, newVersion);

        log.info("版本已发布: scriptId={}, versionId={}, versionNo={}", scriptId, versionId, newVersion);
        return versionId;
    }

    public List<Map<String, Object>> listVersions(Long scriptId) {
        try {
            return jdbc.queryForList(
                    "SELECT id, version_no, status, created_time FROM sps_rule_version " +
                    "WHERE script_id=? ORDER BY version_no DESC", scriptId);
        } catch (Exception e) {
            log.warn("查询版本列表失败: scriptId={}, error={}", scriptId, e.getMessage());
            return new ArrayList<>();
        }
    }

    public Long getActiveVersionId(Long scriptId) {
        try {
            return jdbc.queryForObject(
                    "SELECT id FROM sps_rule_version WHERE script_id=? AND status='PUBLISHED' " +
                    "ORDER BY version_no DESC LIMIT 1", Long.class, scriptId);
        } catch (Exception e) {
            log.warn("获取当前激活版本失败: scriptId={}", scriptId);
            return null;
        }
    }
}
