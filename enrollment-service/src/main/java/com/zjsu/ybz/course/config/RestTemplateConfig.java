package com.zjsu.ybz.course.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置类
 * 配置支持 Nacos 服务发现的 RestTemplate
 */
@Configuration
public class RestTemplateConfig {
    
    /**
     * 创建 RestTemplate
     * 不使用负载均衡，直接通过配置的 URL 调用服务
     * 
     * 使用示例:
     * restTemplate.getForObject(userServiceUrl + "/api/students/{id}", ...)
     * 其中 userServiceUrl 从配置文件中读取
     */
    @Bean
    // @LoadBalanced  // 禁用 Nacos 服务发现时需要注释掉
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
