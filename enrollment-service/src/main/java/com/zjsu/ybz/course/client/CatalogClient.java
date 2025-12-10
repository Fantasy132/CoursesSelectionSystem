package com.zjsu.ybz.course.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Catalog Service Feign Client
 * 用于调用课程目录服务的 Feign 客户端
 */
@FeignClient(
    name = "catalog-service",
    fallback = CatalogClientFallback.class
)
public interface CatalogClient {
    
    /**
     * 根据课程ID获取课程信息
     * @param courseId 课程ID
     * @return 课程信息响应
     */
    @GetMapping("/api/courses/{courseId}")
    Map<String, Object> getCourseById(@PathVariable("courseId") String courseId);
    
    /**
     * 更新课程的已选人数
     * @param courseId 课程ID
     * @param request 包含新的已选人数的请求体
     */
    @PutMapping("/api/courses/{courseId}/update-enrolled")
    void updateCourseEnrolledCount(
        @PathVariable("courseId") String courseId, 
        @RequestBody Map<String, Integer> request
    );
}
