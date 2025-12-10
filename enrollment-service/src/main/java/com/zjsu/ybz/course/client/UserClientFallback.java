package com.zjsu.ybz.course.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * User Service Fallback Implementation
 * UserClient 的降级处理实现
 */
@Component
public class UserClientFallback implements UserClient {
    
    private static final Logger log = LoggerFactory.getLogger(UserClientFallback.class);
    
    @Override
    public Map<String, Object> getStudentByStudentId(String studentId) {
        log.warn("UserClient fallback triggered for studentId: {}", studentId);
        
        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("success", false);
        fallbackResponse.put("message", "User service is temporarily unavailable");
        fallbackResponse.put("data", null);
        
        return fallbackResponse;
    }
}
