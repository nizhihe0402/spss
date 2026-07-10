package com.gxaysoft.project.spsscheck.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Map<String, Object> handleBadRequest(IllegalArgumentException e) {
        log.warn("请求参数错误: {}", e.getMessage());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 400);
        result.put("msg", e.getMessage());
        return result;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Map<String, Object> handleInternal(Exception e) {
        log.error("未处理异常", e);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 500);
        result.put("msg", "服务器内部错误");
        return result;
    }
}
