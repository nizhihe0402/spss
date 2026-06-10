package com.gxaysoft.project.spsscheck.io;

import com.gxaysoft.project.spsscheck.model.AnswerRecord;
import com.gxaysoft.project.spsscheck.model.QuestionMapping;
import com.gxaysoft.project.spsscheck.model.RowContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * Queries bus_student.card and bus_student.id_type by student_id
 * to supplement SFZ (身份证号) and ZJTYPE (证件类型) variables.
 */
public final class StudentInfoEnricher {

    private StudentInfoEnricher() {}

    /**
     * Load SFZ/ZJTYPE from bus_student for the given student IDs.
     * Returns synthetic QuestionMappings so the availability checker knows these vars exist.
     */
    public static LoadResult load(DataSource ds, Set<String> studentIds) throws Exception {
        Map<String, Map<String, String>> info = new LinkedHashMap<>();
        Map<String, QuestionMapping> mappings = new LinkedHashMap<>();

        if (studentIds.isEmpty()) {
            mappings.put("SFZ", new QuestionMapping(-2L, "SFZ", "身份证号", -2L));
            mappings.put("ZJTYPE", new QuestionMapping(-2L, "ZJTYPE", "证件类型", -2L));
            return new LoadResult(info, mappings);
        }

        // Build IN clause — batch by 500 to avoid SQL length limits
        List<String> idList = new ArrayList<>(studentIds);
        try (Connection conn = ds.getConnection()) {
            for (int i = 0; i < idList.size(); i += 500) {
                int end = Math.min(i + 500, idList.size());
                List<String> batch = idList.subList(i, end);

                StringBuilder sql = new StringBuilder(
                    "SELECT student_id, card, id_type FROM bus_student WHERE student_id IN (");
                for (int j = 0; j < batch.size(); j++) {
                    if (j > 0) sql.append(",");
                    sql.append("?");
                }
                sql.append(") AND del_flag='0'");

                try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                    for (int j = 0; j < batch.size(); j++) {
                        ps.setLong(j + 1, Long.parseLong(batch.get(j)));
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String sid = String.valueOf(rs.getLong("student_id"));
                            String card = rs.getString("card");
                            String idType = rs.getString("id_type");
                            Map<String, String> item = new LinkedHashMap<>();
                            if (card != null && !card.isEmpty()) item.put("SFZ", card);
                            if (idType != null) item.put("ZJTYPE", idType);
                            info.put(sid, item);
                        }
                    }
                }
            }
        }

        // Synthetic mappings
        mappings.put("SFZ", new QuestionMapping(-2L, "SFZ", "身份证号", -2L));
        mappings.put("ZJTYPE", new QuestionMapping(-2L, "ZJTYPE", "证件类型", -2L));

        return new LoadResult(info, mappings);
    }

    public static void enrichRows(List<RowContext> rows, Map<String, Map<String, String>> studentInfo) {
        StudentInfoLoader.enrichRows(rows, studentInfo);
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
