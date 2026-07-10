package com.gxaysoft.project.spsscheck.web;

import com.gxaysoft.project.spsscheck.engine.parser.ParsedScript;
import com.gxaysoft.project.spsscheck.persistence.ScriptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/scripts")
public class ScriptController {
    private static final Logger log = LoggerFactory.getLogger(ScriptController.class);

    @Autowired
    private ScriptService scriptService;

    @GetMapping("/{id}")
    public Map<String, Object> getScript(@PathVariable Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("content", scriptService.loadScriptContent(id));
        return result;
    }

    @GetMapping("/{id}/rules")
    public Map<String, Object> getRules(@PathVariable Long id) {
        ParsedScript parsed = scriptService.getParsedRules(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("rules", parsed.getRules());
        result.put("datasetRules", parsed.getDatasetRules());
        result.put("outputRules", parsed.getOutputRules());
        result.put("totalRules", parsed.totalRules());
        return result;
    }
}
