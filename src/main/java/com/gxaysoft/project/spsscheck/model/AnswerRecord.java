package com.gxaysoft.project.spsscheck.model;

/**
 * One raw answer row from bus_doctor_answer style CSV.
 *
 * The old engine only needed sampleKey/questionId/content/tableId.  The run-time
 * validator also needs project_id, option_id, year and del_flag to check whether
 * uploaded data can safely be used for SPSS rule execution.
 */
public class AnswerRecord {
    private final long rawId;
    private final int rowNumber;
    private final String sampleKey;
    private final long questionId;
    private final long optionId;
    private final long studentId;
    private final String content;
    private final long projectId;
    private final long tableId;
    private final String times;
    private final String year;
    private final String delFlag;

    public AnswerRecord(String sampleKey, long questionId, String content) {
        this(-1L, -1, sampleKey, questionId, -1L, parseLongQuietly(sampleKey), content, -1L, -1L, null, null, null);
    }

    public AnswerRecord(String sampleKey, long questionId, String content, long tableId) {
        this(-1L, -1, sampleKey, questionId, -1L, parseLongQuietly(sampleKey), content, -1L, tableId, null, null, null);
    }

    public AnswerRecord(long rawId,
                        int rowNumber,
                        String sampleKey,
                        long questionId,
                        long optionId,
                        long studentId,
                        String content,
                        long projectId,
                        long tableId,
                        String times,
                        String year,
                        String delFlag) {
        this.rawId = rawId;
        this.rowNumber = rowNumber;
        this.sampleKey = sampleKey;
        this.questionId = questionId;
        this.optionId = optionId;
        this.studentId = studentId;
        this.content = content;
        this.projectId = projectId;
        this.tableId = tableId;
        this.times = times;
        this.year = year;
        this.delFlag = delFlag;
    }

    public long getRawId() {
        return rawId;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public String getSampleKey() {
        return sampleKey;
    }

    public long getQuestionId() {
        return questionId;
    }

    public long getOptionId() {
        return optionId;
    }

    public long getStudentId() {
        return studentId;
    }

    public String getContent() {
        return content;
    }

    public long getProjectId() {
        return projectId;
    }

    public long getTableId() {
        return tableId;
    }

    public String getTimes() {
        return times;
    }

    public String getYear() {
        return year;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public boolean isActive() {
        return delFlag == null || delFlag.trim().isEmpty() || "0".equals(delFlag.trim());
    }

    private static long parseLongQuietly(String value) {
        if (value == null) return -1L;
        try {
            String v = value.trim();
            if (v.isEmpty()) return -1L;
            return Long.parseLong(v);
        } catch (Exception ignored) {
            return -1L;
        }
    }
}
