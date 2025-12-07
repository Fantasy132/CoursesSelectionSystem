package com.zjsu.ybz.course.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
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
     * 创建支持负载均衡的 RestTemplate
     * 使用 @LoadBalanced 注解后，可以通过服务名调用其他服务
     * 
     * 使用示例:
     * restTemplate.getForObject("http://user-service/api/students/{id}", ...)
     * 其中 user-service 是在 Nacos 中注册的服务名
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
