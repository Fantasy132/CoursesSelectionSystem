package com.zjsu.ybz.gateway.filter;

import com.zjsu.ybz.gateway.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * JWT 认证全局过滤器
 * 功能：
 * 1. 白名单路径放行
 * 2. 验证 JWT Token
 * 3. 提取用户信息并传递给下游服务
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 白名单：不需要 JWT 认证的路径
     */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/login",           // 登录接口
            "/api/auth/register",        // 注册接口
            "/api/auth/validate",        // Token 验证接口（公开）
            "/fallback/catalog",         // 降级接口
            "/fallback/enrollment",      // 降级接口
            "/fallback/user",            // 降级接口
            "/actuator/health",          // 健康检查
            "/actuator/info"             // 应用信息
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        logger.debug("JWT 认证过滤器 - 请求路径: {}", path);

        // 检查是否在白名单中
        if (isWhiteListPath(path)) {
            logger.debug("白名单路径，直接放行: {}", path);
            return chain.filter(exchange);
        }

        // 提取 Token
        String token = extractToken(request);
        
        if (!StringUtils.hasText(token)) {
            logger.warn("未提供 JWT Token，拒绝访问: {}", path);
            return unauthorized(exchange.getResponse(), "未提供认证 Token");
        }

        // 验证 Token
        try {
            if (!jwtUtil.validateToken(token)) {
                logger.warn("JWT Token 验证失败: {}", path);
                return unauthorized(exchange.getResponse(), "Token 无效或已过期");
            }

            // 提取用户信息
            Map<String, String> userInfo = jwtUtil.extractUserInfo(token);
            String studentId = userInfo.get("studentId");
            String name = userInfo.get("name");
            String email = userInfo.getOrDefault("email", "");

            logger.debug("JWT Token 验证成功 - 学号: {}, 姓名: {}", studentId, name);

            // 将用户信息添加到请求头，传递给下游服务
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", studentId)
                    .header("X-User-Name", name)
                    .header("X-User-Email", email)
                    .header("X-Auth-Token", token)  // 传递原始 Token（可选）
                    .build();

            // 检查是否需要刷新 Token
            if (jwtUtil.shouldRefreshToken(token)) {
                long remainingTime = jwtUtil.getTokenRemainingTime(token);
                logger.info("Token 即将过期，建议刷新 - 学号: {}, 剩余时间: {}ms", studentId, remainingTime);
                // 添加响应头提示前端刷新 Token
                exchange.getResponse().getHeaders().add("X-Token-Refresh-Required", "true");
            }

            // 继续执行过滤器链
            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            logger.error("JWT Token 处理异常: {}", e.getMessage(), e);
            return unauthorized(exchange.getResponse(), "Token 验证失败");
        }
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhiteListPath(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    /**
     * 从请求中提取 Token
     * 支持两种方式：
     * 1. Authorization Header: Bearer <token>
     * 2. Query Parameter: token=<token>
     */
    private String extractToken(ServerHttpRequest request) {
        // 方式 1: 从 Authorization Header 提取
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 方式 2: 从 Query Parameter 提取（不推荐，但支持某些场景）
        String tokenParam = request.getQueryParams().getFirst("token");
        if (StringUtils.hasText(tokenParam)) {
            logger.debug("从 Query Parameter 提取 Token");
            return tokenParam;
        }

        return null;
    }

    /**
     * 返回 401 未授权响应
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        
        String body = String.format("{\"status\":401,\"message\":\"%s\",\"timestamp\":%d}", 
                message, System.currentTimeMillis());
        
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    /**
     * 过滤器优先级：-100 表示在其他过滤器之前执行
     * 数字越小，优先级越高
     */
    @Override
    public int getOrder() {
        return -100;
    }
}
