package com.gxaysoft.project.spsscheck.io;

import com.gxaysoft.project.spsscheck.model.AnswerRecord;
import com.gxaysoft.project.spsscheck.model.QuestionMapping;
import com.gxaysoft.project.spsscheck.model.RowContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StudentInfoEnricher {

    private StudentInfoEnricher() {}

    public static Set<String> collectStudentIds(List<AnswerRecord> answers) {
        Set<String> studentIds = new LinkedHashSet<String>();
        if (answers == null) {
            return studentIds;
        }
        for (AnswerRecord answer : answers) {
            if (answer == null) {
                continue;
            }
            if (answer.getStudentId() > 0) {
                studentIds.add(String.valueOf(answer.getStudentId()));
                continue;
            }
            String sampleKey = answer.getSampleKey();
            if (sampleKey != null) {
                String trimmed = sampleKey.trim();
                if (trimmed.matches("\\d+")) {
                    studentIds.add(trimmed);
                }
            }
        }
        return studentIds;
    }

    public static LoadResult load(DataSource ds, Set<String> studentIds) throws Exception {
        Map<String, Map<String, String>> info = new LinkedHashMap<String, Map<String, String>>();
        Map<String, QuestionMapping> mappings = new LinkedHashMap<String, QuestionMapping>();

        if (studentIds.isEmpty()) {
            addDocumentMappings(mappings, -2L);
            return new LoadResult(info, mappings);
        }

        List<String> idList = new ArrayList<String>(studentIds);
        try (Connection conn = ds.getConnection()) {
            for (int i = 0; i < idList.size(); i += 500) {
                int end = Math.min(i + 500, idList.size());
                List<String> batch = idList.subList(i, end);

                StringBuilder sql = new StringBuilder(
                        "SELECT s.student_id, s.card, s.id_type, sc.school_code " +
                                "FROM bus_student s LEFT JOIN bus_school sc ON sc.school_id=s.school_id AND sc.del_flag='0' " +
                                "WHERE s.student_id IN (");
                for (int j = 0; j < batch.size(); j++) {
                    if (j > 0) sql.append(",");
                    sql.append("?");
                }
                sql.append(") AND s.del_flag='0'");

                try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                    for (int j = 0; j < batch.size(); j++) {
                        ps.setLong(j + 1, Long.parseLong(batch.get(j)));
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String sid = String.valueOf(rs.getLong("student_id"));
                            String card = rs.getString("card");
                            String idType = rs.getString("id_type");
                            String schoolCode = rs.getString("school_code");
                            Map<String, String> item = new LinkedHashMap<String, String>();
                            String zjType = toSpssZjtype(idType);
                            putDocumentNumber(item, zjType, card);
                            if (zjType != null) item.put("ZJTYPE", zjType);
                            if (schoolCode != null && !schoolCode.trim().isEmpty()) item.put("SCHOOL", schoolCode.trim());
                            info.put(sid, item);
                        }
                    }
                }
            }
        }

        addDocumentMappings(mappings, -2L);
        return new LoadResult(info, mappings);
    }

    public static void enrichRows(List<RowContext> rows, Map<String, Map<String, String>> studentInfo) {
        for (RowContext row : rows) {
            Map<String, String> info = studentInfo.get(row.getSampleKey());
            if (info != null) {
                for (Map.Entry<String, String> e : info.entrySet()) {
                    if (isCorrectionManagedField(e.getKey())) {
                        continue;
                    }
                    row.put(e.getKey(), e.getValue());
                }
            }
        }
    }

    private static boolean isCorrectionManagedField(String key) {
        return "SCHOOL".equalsIgnoreCase(key)
                || "ZJTYPE".equalsIgnoreCase(key)
                || isDocumentNumberVariable(key);
    }

    public static void mergeMissingStudentInfo(Map<String, Map<String, String>> target,
                                               Map<String, Map<String, String>> fallback) {
        if (target == null || fallback == null || fallback.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Map<String, String>> entry : fallback.entrySet()) {
            Map<String, String> existing = target.get(entry.getKey());
            if (existing == null) {
                target.put(entry.getKey(), entry.getValue());
                continue;
            }
            Map<String, String> fallbackFields = entry.getValue();
            if (fallbackFields == null || fallbackFields.isEmpty()) {
                continue;
            }
            for (Map.Entry<String, String> field : fallbackFields.entrySet()) {
                String key = field.getKey();
                String value = field.getValue();
                if (key == null || value == null || value.trim().isEmpty()) {
                    continue;
                }
                String current = existing.get(key);
                if (current == null || current.trim().isEmpty()) {
                    existing.put(key, value);
                }
            }
        }
    }

    public static String toSpssZjtype(String idType) {
        if (idType == null) {
            return null;
        }
        String value = idType.trim();
        if (value.isEmpty()) {
            return null;
        }
        if ("0".equals(value)) {
            return "1";
        }
        return value;
    }

    public static void putDocumentNumber(Map<String, String> info, String zjType, String card) {
        if (info == null || card == null || card.trim().isEmpty()) {
            return;
        }
        String variable = documentNumberVariable(zjType);
        if (variable != null) {
            info.put(variable, card.trim());
        }
    }

    public static String documentNumberVariable(String zjType) {
        String value = toSpssZjtype(zjType);
        if (value == null) return "SFZ";
        if ("1".equals(value)) return "SFZ";
        if ("2".equals(value)) return "MTP";
        if ("3".equals(value)) return "TRPMT";
        if ("4".equals(value)) return "HZ";
        return "SFZ";
    }

    public static boolean isDocumentNumberVariable(String variable) {
        return "SFZ".equalsIgnoreCase(variable)
                || "MTP".equalsIgnoreCase(variable)
                || "TRPMT".equalsIgnoreCase(variable)
                || "HZ".equalsIgnoreCase(variable);
    }

    public static void addDocumentMappings(Map<String, QuestionMapping> mappings, long tableId) {
        mappings.put("SFZ", new QuestionMapping(tableId, "SFZ", "身份证号", tableId));
        mappings.put("MTP", new QuestionMapping(tableId, "MTP", "港澳居民来往内地通行证", tableId));
        mappings.put("TRPMT", new QuestionMapping(tableId, "TRPMT", "台湾居民来往大陆通行证", tableId));
        mappings.put("HZ", new QuestionMapping(tableId, "HZ", "护照", tableId));
        mappings.put("ZJTYPE", new QuestionMapping(tableId, "ZJTYPE", "证件类型", tableId));
    }

    public static class LoadResult {
        public final Map<String, Map<String, String>> studentInfo;
        public final Map<String, QuestionMapping> mappings;

        LoadResult(Map<String, Map<String, String>> info, Map<String, QuestionMapping> mappings) {
            this.studentInfo = info;
            this.mappings = mappings;
        }
    }
}
