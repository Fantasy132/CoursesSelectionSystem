package com.zjsu.ybz.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 服务降级处理控制器
 * 当微服务不可用时返回友好的错误信息
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/catalog")
    @PostMapping("/catalog")
    public ResponseEntity<Map<String, Object>> catalogFallback() {
        return createFallbackResponse("课程服务暂时不可用，请稍后重试");
    }

    @GetMapping("/enrollment")
    @PostMapping("/enrollment")
    public ResponseEntity<Map<String, Object>> enrollmentFallback() {
        return createFallbackResponse("选课服务暂时不可用，请稍后重试");
    }

    @GetMapping("/user")
    @PostMapping("/user")
    public ResponseEntity<Map<String, Object>> userFallback() {
        return createFallbackResponse("用户服务暂时不可用，请稍后重试");
    }

    private ResponseEntity<Map<String, Object>> createFallbackResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }
}
