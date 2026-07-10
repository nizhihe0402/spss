package com.gxaysoft.project.spsscheck.web;

import com.gxaysoft.project.spsscheck.execution.DbRuleExecutionDataLoader;
import com.gxaysoft.project.spsscheck.persistence.ExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/execute")
public class ExecuteController {
    private static final Logger log = LoggerFactory.getLogger(ExecuteController.class);

    @Autowired
    private ExecutionService executionService;

    @PostMapping("/upload")
    public Map<String, Object> executeFromUpload(
            @RequestParam("csvFile") MultipartFile csvFile,
            @RequestParam("scriptId") Long scriptId,
            @RequestParam(value = "tableId", required = false) Long tableId,
            @RequestParam(value = "mappingFile", required = false) MultipartFile mappingFile,
            @RequestParam(value = "studentFile", required = false) MultipartFile studentFile,
            @RequestParam(value = "strictValidate", defaultValue = "false") boolean strictValidate) {
        log.info("CSV 执行请求: scriptId={}, fileSize={}", scriptId,
                csvFile != null ? csvFile.getSize() : 0);
        try {
            byte[] mappingBytes = mappingFile != null && !mappingFile.isEmpty() ? mappingFile.getBytes() : null;
            byte[] studentBytes = studentFile != null && !studentFile.isEmpty() ? studentFile.getBytes() : null;
            return executionService.executeFromUpload(csvFile.getBytes(), scriptId, tableId,
                    mappingBytes, studentBytes, strictValidate);
        } catch (Exception e) {
            log.error("CSV执行失败", e);
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("code", 500);
            err.put("msg", "执行规则失败: " + e.getMessage());
            return err;
        }
    }

    @PostMapping("/db")
    public Map<String, Object> executeFromDb(
            @RequestParam("scriptId") Long scriptId,
            @RequestParam("projectId") Long projectId,
            @RequestParam("tableId") Long tableId,
            @RequestParam("divisionId") Long divisionId,
            @RequestParam(value = "schoolId", required = false) Long schoolId,
            @RequestParam("year") String year,
            @RequestParam(value = "source", defaultValue = "normal") String source,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "studentClass", required = false) String studentClass) {
        log.info("DB执行请求: scriptId={}, projectId={}, tableId={}, year={}, grade={}, class={}",
                scriptId, projectId, tableId, year, grade, studentClass);
        DbRuleExecutionDataLoader.Request req = new DbRuleExecutionDataLoader.Request();
        req.scriptId = scriptId;
        req.projectId = projectId;
        req.tableId = tableId;
        req.divisionId = divisionId;
        req.schoolId = schoolId != null ? schoolId : -1L;
        req.year = year;
        req.source = source;
        req.grade = grade;
        req.studentClass = studentClass;
        return executionService.executeFromDb(req);
    }
}
