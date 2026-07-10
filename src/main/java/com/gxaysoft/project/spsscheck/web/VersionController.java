package com.gxaysoft.project.spsscheck.web;

import com.gxaysoft.project.spsscheck.persistence.VersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/versions")
public class VersionController {
    private static final Logger log = LoggerFactory.getLogger(VersionController.class);

    @Autowired
    private VersionService versionService;

    @PostMapping("/{scriptId}/publish")
    public Map<String, Object> publish(@PathVariable Long scriptId) {
        Long versionId = versionService.publishVersion(scriptId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("msg", "版本发布成功");
        result.put("versionId", versionId);
        return result;
    }

    @GetMapping("/{scriptId}/list")
    public Map<String, Object> list(@PathVariable Long scriptId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("versions", versionService.listVersions(scriptId));
        return result;
    }

    @GetMapping("/{scriptId}/active")
    public Map<String, Object> active(@PathVariable Long scriptId) {
        Long versionId = versionService.getActiveVersionId(scriptId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("activeVersionId", versionId);
        return result;
    }
}
