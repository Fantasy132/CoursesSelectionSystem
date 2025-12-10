package com.zjsu.ybz.course.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * User Service Feign Client
 * 用于调用用户服务的 Feign 客户端
 */
@FeignClient(
    name = "user-service",
    fallback = UserClientFallback.class
)
public interface UserClient {
    
    /**
     * 根据学生ID获取学生信息
     * @param studentId 学生ID
     * @return 学生信息响应
     */
    @GetMapping("/api/students/studentId/{studentId}")
    Map<String, Object> getStudentByStudentId(@PathVariable("studentId") String studentId);
}
