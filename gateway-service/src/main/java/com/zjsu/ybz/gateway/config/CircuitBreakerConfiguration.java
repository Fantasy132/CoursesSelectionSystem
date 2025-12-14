package com.zjsu.ybz.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 熔断器配置
 */
@Configuration
public class CircuitBreakerConfiguration {

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        // 滑动窗口大小
                        .slidingWindowSize(10)
                        // 最小调用次数
                        .minimumNumberOfCalls(5)
                        // 失败率阈值（50%）
                        .failureRateThreshold(50)
                        // 慢调用阈值
                        .slowCallRateThreshold(50)
                        // 慢调用持续时间
                        .slowCallDurationThreshold(Duration.ofSeconds(3))
                        // 等待时间（熔断器打开后等待多久尝试半开）
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        // 半开状态允许的调用次数
                        .permittedNumberOfCallsInHalfOpenState(3)
                        // 自动从 OPEN 转换到 HALF_OPEN
                        .automaticTransitionFromOpenToHalfOpenEnabled(true)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        // 超时时间
                        .timeoutDuration(Duration.ofSeconds(5))
                        .build())
                .build());
    }
}
