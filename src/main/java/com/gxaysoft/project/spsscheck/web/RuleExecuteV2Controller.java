package com.gxaysoft.project.spsscheck.web;

import com.gxaysoft.project.spsscheck.io.PrototypeFileReaders;
import com.gxaysoft.project.spsscheck.model.AnswerRecord;
import com.gxaysoft.project.spsscheck.validation.AnswerDataValidationReport;
import com.gxaysoft.project.spsscheck.validation.AnswerDataValidator;
import com.gxaysoft.project.spsscheck.validation.StudentValidationResultBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * V2 执行入口。
 *
 * 重点：该接口返回 data.passedList / data.failedList，前端直接分两张表展示。
 * 不再把 ERROR/WARN 字段明细作为主展示结果。
 */
@RestController
@RequestMapping("/api/v2/rules")
public class RuleExecuteV2Controller {

    @Autowired
    private JdbcTemplate jdbc;

    @PostMapping("/execute")
    public Map<String, Object> execute(@RequestParam(value = "csvFile", required = false) MultipartFile csvFile,
                                       @RequestParam(value = "file", required = false) MultipartFile file,
                                       @RequestParam(value = "scriptId", required = false) Long scriptId,
                                       @RequestParam(value = "tableId", required = false) Long tableId,
                                       @RequestParam(value = "table_id", required = false) Long tableId2,
                                       @RequestParam(value = "projectId", required = false) Long projectId,
                                       @RequestParam(value = "project_id", required = false) Long projectId2,
                                       @RequestParam(value = "year", required = false) String year,
                                       @RequestParam(value = "fieldCheck", required = false) String fieldCheck,
                                       @RequestParam(value = "strictValidate", required = false, defaultValue = "false") boolean strictValidate) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        try {
            MultipartFile actualFile = csvFile != null ? csvFile : file;
            if (actualFile == null || actualFile.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "请上传CSV数据文件");
                return result;
            }

            Long actualTableId = tableId != null ? tableId : tableId2;
            Long actualProjectId = projectId != null ? projectId : projectId2;

            PrototypeFileReaders.AnswerCsvLoadResult csvLoad = PrototypeFileReaders.readAnswerCsvDetailed(actualFile.getBytes());
            applyDefaults(csvLoad, actualTableId, actualProjectId, year);

            AnswerDataValidationReport validationReport = new AnswerDataValidator(jdbc).validate(csvLoad);
            Map<String, Object> splitResult = new StudentValidationResultBuilder(jdbc).build(csvLoad, validationReport);

            result.put("code", 0);
            result.put("msg", validationReport.isPassed() ? "校验通过" : "校验完成，存在未通过学生");
            result.put("scriptId", scriptId);
            result.put("data", splitResult);
            result.put("validationReport", validationReport.toMap(300));
            return result;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            result.put("code", 500);
            result.put("msg", "执行规则失败：" + e.getMessage());
            result.put("trace", sw.toString());
            return result;
        }
    }

    /**
     * 页面可传 tableId/projectId/year 作为缺省值。
     * 如果 CSV 行里已有这些字段，不覆盖，避免掩盖待校验数据本身的问题。
     */
    private void applyDefaults(PrototypeFileReaders.AnswerCsvLoadResult csvLoad,
                               Long tableId,
                               Long projectId,
                               String year) {
        if (csvLoad == null || csvLoad.getAnswers().isEmpty()) return;
        if ((tableId == null || tableId.longValue() <= 0)
                && (projectId == null || projectId.longValue() <= 0)
                && (year == null || year.trim().length() == 0)) {
            return;
        }
        for (int i = 0; i < csvLoad.getAnswers().size(); i++) {
            AnswerRecord a = csvLoad.getAnswers().get(i);
            long nextTableId = a.getTableId() > 0 ? a.getTableId() : (tableId == null ? a.getTableId() : tableId.longValue());
            long nextProjectId = a.getProjectId() > 0 ? a.getProjectId() : (projectId == null ? a.getProjectId() : projectId.longValue());
            String nextYear = !isBlank(a.getYear()) ? a.getYear() : year;
            AnswerRecord replaced = new AnswerRecord(
                    a.getRawId(),
                    a.getRowNumber(),
                    a.getSampleKey(),
                    a.getQuestionId(),
                    a.getOptionId(),
                    a.getStudentId(),
                    a.getContent(),
                    nextProjectId,
                    nextTableId,
                    a.getTimes(),
                    nextYear,
                    a.getDelFlag()
            );
            csvLoad.getAnswers().set(i, replaced);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
