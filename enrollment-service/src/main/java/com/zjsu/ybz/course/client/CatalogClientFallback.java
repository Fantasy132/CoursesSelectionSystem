package com.zjsu.ybz.course.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Catalog Service Fallback Implementation
 * CatalogClient 的降级处理实现
 */
@Component
public class CatalogClientFallback implements CatalogClient {
    
    private static final Logger log = LoggerFactory.getLogger(CatalogClientFallback.class);
    
    @Override
    public Map<String, Object> getCourseById(String courseId) {
        log.warn("CatalogClient fallback triggered for courseId: {}", courseId);
        
        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("success", false);
        fallbackResponse.put("message", "Catalog service is temporarily unavailable");
        fallbackResponse.put("data", null);
        
        return fallbackResponse;
    }
    
    @Override
    public void updateCourseEnrolledCount(String courseId, Map<String, Integer> request) {
        log.warn("CatalogClient fallback triggered for updateCourseEnrolledCount, courseId: {}", courseId);
        // 降级时不执行更新操作，避免数据不一致
        // 在实际生产环境中，可以考虑将此操作放入消息队列进行异步重试
    }
}
