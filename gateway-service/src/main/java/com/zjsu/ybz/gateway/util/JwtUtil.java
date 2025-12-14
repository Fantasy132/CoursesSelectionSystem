package com.zjsu.ybz.gateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT 工具类 - Gateway 版本
 * 用于在 API Gateway 中验证 JWT Token
 * 使用 HS512 算法进行签名验证
 */
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 获取密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 从 Token 中提取所有声明
     */
    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            logger.warn("Token 已过期: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            logger.error("Token 格式错误: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            logger.error("Token 签名验证失败: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            logger.error("Token 解析失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 从 Token 中提取特定声明
     */
    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 从 Token 中提取学号
     */
    public String getStudentIdFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("studentId", String.class));
    }

    /**
     * 从 Token 中提取姓名
     */
    public String getNameFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("name", String.class));
    }

    /**
     * 从 Token 中提取邮箱
     */
    public String getEmailFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("email", String.class));
    }

    /**
     * 从 Token 中提取过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * 检查 Token 是否过期
     */
    public Boolean isTokenExpired(String token) {
        try {
            final Date expirationDate = getExpirationDateFromToken(token);
            return expirationDate.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * 验证 Token
     * @param token JWT Token
     * @return 是否有效
     */
    public Boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException e) {
            logger.error("Token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证 Token 并检查学号
     * @param token JWT Token
     * @param studentId 学号
     * @return 是否有效
     */
    public Boolean validateToken(String token, String studentId) {
        try {
            final String tokenStudentId = getStudentIdFromToken(token);
            return (tokenStudentId.equals(studentId) && !isTokenExpired(token));
        } catch (JwtException e) {
            logger.error("Token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 Token 中提取所有用户信息
     * @param token JWT Token
     * @return 用户信息 Map
     */
    public Map<String, String> extractUserInfo(String token) {
        Map<String, String> userInfo = new HashMap<>();
        try {
            Claims claims = getAllClaimsFromToken(token);
            userInfo.put("studentId", claims.get("studentId", String.class));
            userInfo.put("name", claims.get("name", String.class));
            
            String email = claims.get("email", String.class);
            if (email != null) {
                userInfo.put("email", email);
            }
            
            logger.debug("从 Token 中提取用户信息: {}", userInfo);
            return userInfo;
        } catch (JwtException e) {
            logger.error("提取用户信息失败: {}", e.getMessage());
            return userInfo;
        }
    }

    /**
     * 获取 Token 剩余有效时间（毫秒）
     */
    public long getTokenRemainingTime(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.getTime() - System.currentTimeMillis();
        } catch (JwtException e) {
            logger.error("获取 Token 剩余时间失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 检查 Token 是否需要刷新（剩余时间少于 2 小时）
     */
    public boolean shouldRefreshToken(String token) {
        try {
            long remainingTime = getTokenRemainingTime(token);
            // 剩余时间少于 2 小时则需要刷新
            return remainingTime > 0 && remainingTime < 2 * 60 * 60 * 1000;
        } catch (JwtException e) {
            return false;
        }
    }
}
