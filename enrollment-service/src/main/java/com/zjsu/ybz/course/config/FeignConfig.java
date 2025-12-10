package com.zjsu.ybz.course.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 日志配置
 * 启用详细的请求响应日志
 */
@Configuration
public class FeignConfig {
    
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;  // FULL 级别会记录请求和响应的全部信息，包括 URL
    }
}
