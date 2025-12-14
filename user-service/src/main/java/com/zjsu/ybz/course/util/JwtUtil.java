package com.zjsu.ybz.course.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
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
 * JWT 工具类
 * 使用 HS512 算法生成和验证 JWT Token
 * Token 有效期：24 小时（86400000 毫秒）
 * 
 * @author CSS Team
 * @version 1.0
 */
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 默认 24 小时（86400000 毫秒）
    private Long expiration;

    private static final String CLAIM_KEY_STUDENT_ID = "studentId";
    private static final String CLAIM_KEY_NAME = "name";
    private static final String CLAIM_KEY_EMAIL = "email";

    /**
     * 获取签名密钥（HS512 算法）
     * 密钥长度必须至少为 512 位（64 字节）
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // HS512 需要至少 512 位的密钥
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 JWT Token（使用 HS512 算法）
     * @param studentId 学号
     * @param name 姓名
     * @return JWT Token
     */
    public String generateToken(String studentId, String name) {
        return generateToken(studentId, name, null);
    }

    /**
     * 生成 JWT Token（包含邮箱，使用 HS512 算法）
     * @param studentId 学号
     * @param name 姓名
     * @param email 邮箱（可选）
     * @return JWT Token
     */
    public String generateToken(String studentId, String name, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_KEY_STUDENT_ID, studentId);
        claims.put(CLAIM_KEY_NAME, name);
        if (email != null) {
            claims.put(CLAIM_KEY_EMAIL, email);
        }
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        logger.debug("生成 Token - 学号: {}, 有效期至: {}", studentId, expiryDate);
        
        return Jwts.builder()
                .claims(claims)
                .subject(studentId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512) // 明确指定 HS512 算法
                .compact();
    }

    /**
     * 从 Token 中提取学号
     * @param token JWT Token
     * @return 学号
     */
    public String getStudentIdFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * 从 Token 中提取姓名
     * @param token JWT Token
     * @return 姓名
     */
    public String getNameFromToken(String token) {
        return (String) getAllClaimsFromToken(token).get(CLAIM_KEY_NAME);
    }

    /**
     * 从 Token 中提取邮箱
     * @param token JWT Token
     * @return 邮箱
     */
    public String getEmailFromToken(String token) {
        return (String) getAllClaimsFromToken(token).get(CLAIM_KEY_EMAIL);
    }

    /**
     * 从 Token 中提取签发时间
     * @param token JWT Token
     * @return 签发时间
     */
    public Date getIssuedAtFromToken(String token) {
        return getClaimFromToken(token, Claims::getIssuedAt);
    }

    /**
     * 从 Token 中提取过期时间
     * @param token JWT Token
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * 从 Token 中提取特定声明
     * @param token JWT Token
     * @param claimsResolver 声明解析函数
     * @return 声明值
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 从 Token 中提取所有声明
     * @param token JWT Token
     * @return 所有声明
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
        } catch (JwtException e) {
            logger.error("Token 解析失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 验证 Token 是否过期
     * @param token JWT Token
     * @return true: 已过期, false: 未过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            boolean expired = expiration.before(new Date());
            if (expired) {
                logger.debug("Token 已过期: {}", expiration);
            }
            return expired;
        } catch (ExpiredJwtException e) {
            logger.debug("Token 已过期异常");
            return true;
        } catch (Exception e) {
            logger.error("验证 Token 过期时出错: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 验证 Token 是否有效
     * @param token JWT Token
     * @param studentId 学号
     * @return true: 有效, false: 无效
     */
    public boolean validateToken(String token, String studentId) {
        try {
            String tokenStudentId = getStudentIdFromToken(token);
            boolean valid = tokenStudentId.equals(studentId) && !isTokenExpired(token);
            
            if (valid) {
                logger.debug("Token 验证成功 - 学号: {}", studentId);
            } else {
                logger.warn("Token 验证失败 - 学号不匹配或已过期");
            }
            
            return valid;
        } catch (Exception e) {
            logger.error("Token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证 Token 格式是否正确且未过期
     * @param token JWT Token
     * @return true: 有效, false: 无效
     */
    public boolean validateTokenFormat(String token) {
        try {
            getAllClaimsFromToken(token);
            boolean valid = !isTokenExpired(token);
            
            if (valid) {
                logger.debug("Token 格式验证成功");
            } else {
                logger.debug("Token 格式正确但已过期");
            }
            
            return valid;
        } catch (ExpiredJwtException e) {
            logger.debug("Token 已过期");
            return false;
        } catch (JwtException e) {
            logger.error("Token 格式无效: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Token 验证异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取 Token 剩余有效时间（毫秒）
     * @param token JWT Token
     * @return 剩余有效时间（毫秒），如果已过期返回 0
     */
    public long getTokenRemainingTime(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(remaining, 0);
        } catch (Exception e) {
            logger.error("获取 Token 剩余时间失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 检查 Token 是否需要刷新（剩余时间少于 2 小时）
     * @param token JWT Token
     * @return true: 需要刷新, false: 不需要刷新
     */
    public boolean shouldRefreshToken(String token) {
        try {
            long remaining = getTokenRemainingTime(token);
            long twoHours = 2 * 60 * 60 * 1000; // 2 小时
            return remaining > 0 && remaining < twoHours;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 刷新 Token（生成新的 Token）
     * @param token 旧的 JWT Token
     * @return 新的 JWT Token
     */
    public String refreshToken(String token) {
        try {
            String studentId = getStudentIdFromToken(token);
            String name = getNameFromToken(token);
            String email = getEmailFromToken(token);
            
            logger.info("刷新 Token - 学号: {}", studentId);
            return generateToken(studentId, name, email);
        } catch (Exception e) {
            logger.error("刷新 Token 失败: {}", e.getMessage());
            throw new JwtException("无法刷新 Token", e);
        }
    }

    /**
     * 获取 Token 配置信息
     * @return Token 配置信息
     */
    public Map<String, Object> getTokenInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("algorithm", "HS512");
        info.put("expirationMs", expiration);
        info.put("expirationHours", expiration / (60 * 60 * 1000));
        return info;
    }
}
