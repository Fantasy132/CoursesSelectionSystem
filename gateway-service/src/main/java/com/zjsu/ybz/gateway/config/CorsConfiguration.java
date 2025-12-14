package com.zjsu.ybz.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * CORS 跨域配置
 * 适配 JWT 认证场景，支持 Authorization 头和自定义头
 */
@Configuration
public class CorsConfiguration {

    @Bean
    public CorsWebFilter corsWebFilter() {
        org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
        
        // 允许的源（生产环境应该配置具体域名）
        config.addAllowedOriginPattern("*");  // 允许所有源，生产环境建议指定具体域名
        
        // 允许的 HTTP 方法
        config.setAllowedMethods(Arrays.asList(
            "GET", 
            "POST", 
            "PUT", 
            "DELETE", 
            "OPTIONS", 
            "HEAD", 
            "PATCH"
        ));
        
        // 允许的请求头
        config.addAllowedHeader("*");  // 允许所有请求头
        
        // 暴露的响应头（让客户端可以访问）
        config.setExposedHeaders(Arrays.asList(
            "Authorization",              // JWT Token
            "X-Token-Refresh-Required",   // Token 刷新提示
            "X-User-Id",                  // 用户 ID
            "X-User-Name",                // 用户姓名
            "Content-Type",               // 内容类型
            "Content-Length",             // 内容长度
            "Date"                        // 日期
        ));
        
        // 是否允许携带凭证（Cookies、HTTP 认证等）
        // 注意：当设置为 true 时，allowedOrigins 不能为 "*"，必须指定具体域名
        config.setAllowCredentials(false);  // JWT 认证不需要 Cookie，设置为 false
        
        // 预检请求的缓存时间（秒）
        config.setMaxAge(3600L);  // 1 小时
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsWebFilter(source);
    }
}
