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
     * @LoadBalanced 注解启用客户端负载均衡，支持通过服务名调用
     * 
     * 使用示例:
     * restTemplate.getForObject("http://catalog-service/api/courses/{id}", ...)
     * 其中 "catalog-service" 是注册到 Nacos 的服务名
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
