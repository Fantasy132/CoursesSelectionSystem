package com.zjsu.ybz.course.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试
 * 测试 HS512 算法和 24 小时有效期
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;
    
    // 至少 64 字节的密钥（512 位）
    private static final String TEST_SECRET = "YourSecretKeyForJWTTokenGenerationMustBeAtLeast512BitsLongForHS512AlgorithmPleaseChangeThisInProduction";
    private static final Long TEST_EXPIRATION = 86400000L; // 24 小时
    
    private static final String TEST_STUDENT_ID = "2021001";
    private static final String TEST_NAME = "测试用户";
    private static final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // 使用反射设置私有字段
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", TEST_EXPIRATION);
    }

    @Test
    void testGenerateToken() {
        // 测试生成 Token
        String token = jwtUtil.generateToken(TEST_STUDENT_ID, TEST_NAME);
        
        assertNotNull(token, "Token 不应为空");
        assertFalse(token.isEmpty(), "Token 不应为空字符串");
        assertTrue(token.split("\\.").length == 3, "Token 应包含三个部分（header.payload.signature）");
        
        System.out.println("生成的 Token: " + token);
    }

    @Test
    void testGenerateTokenWithEmail() {
        // 测试生成包含邮箱的 Token
        String token = jwtUtil.generateToken(TEST_STUDENT_ID, TEST_NAME, TEST_EMAIL);
        
        assertNotNull(token);
        
        // 验证可以提取邮箱
        String extractedEmail = jwtUtil.getEmailFromToken(token);
        assertEquals(TEST_EMAIL, extractedEmail, "提取的邮箱应该匹配");
    }

    @Test
    void testExtractStudentId() {
        // 测试提取学号
        String token = jwtUtil.generateToken(TEST_STUDENT_ID, TEST_NAME);
        String extractedStudentId = jwtUtil.getStudentIdFromToken(token);
        
        assertEquals(TEST_STUDENT_ID, extractedStudentId, "提取的学号应该匹配");
    }

    @Test
    void testExtractName() {
        // 测试提取姓名
        String token = jwtUtil.generateToken(TEST_STUDENT_ID, TEST_NAME);
        String extractedName = jwtUtil.getNameFromToken(token);
        
        assertEquals(TEST_NAME, extractedName, "提取的姓名应该匹配");
    }

    @Test
    void testExtractEmail() {
        // 测试提取邮箱
        String token = jwtUtil.generateToken(TEST_STUDENT_ID, TEST_NAME, TEST_EMAIL);
        String extractedEmail = jwtUtil.getEmailFromToken(token);
        
        assertEquals(TEST_EMAIL, extractedEmail, "提取的邮箱应该匹配");
    }

    @Test
    void testExtractIssuedAt() {
        // 测试提取签发时间
        Date beforeGeneration = new Date();
        String token = jwtUtil.generateToken(TEST_STUDENT_ID, TEST_NAME);
        Date afterGeneration = new Date();
        
        Date issuedAt = jwtUtil.getIssuedAtFromToken(token);
        
        assertNotNull(issuedAt, "签发时间不应为空");
        assertTrue(issuedAt.getTime() >= beforeGeneration.getTime(), "签发时间应该在生成前时间之后");
        assertTrue(issuedAt.getTime() <= afterGeneration.getTime(), "签发时间应该在生成后时间之前");
    }

    @Test
    void testExtractExpirationDate() {
        // 测试提取过期时间
        String token = jwtUtil.generateToken(TEST_STUDENT_ID, TEST_NAME);
        Date expirationDate = jwtUtil.getExpirationDateFromToken(token);
        
        assertNotNull(expirationDate, "过期时间不应为空");
        
        // 验证过期时间约为 24 小时后
        long expectedExpiration = System.currentTimeMillis() + TEST_EXPIRATION;
        long actualExpiration = expirationDate.getTime();
        long difference = Math.abs(expectedExpiration - actualExpiration);
        
        assertTrue(difference < 1000, "过期时间应该是 24 小时后（误差小于 1 秒）");
    }

    @Test
    void testIsTokenExpired() {
        // 测试 Token 是否过期（新生成的应该未过期）
        String token = jwtUtil.generateToken(TEST_STUDENT_ID, TEST_NAME);
        
        assertFalse(jwtUtil.isTokenExpired(token), "新生成的 Token 不应该过期");
    }

    @Test
    void testValidateToken() {
        // 测试验证 Token
        String token = jwtUtil.generateToken(TEST_STUDENT_ID, TEST_NAME);
        
        assertTrue(jwtUtil.validateToken(token, TEST_STUDENT_ID), "有效的 Token 应该通过验证");
        assertFalse(jwtUtil.validateToken(token, "wrongStudentId"), "学号不匹配的 Token 不应该通过验证");
    }

    @Test
    void testValidateTokenFormat() {
        // 测试验证 Token 格式
        String token = jwtUtil.generateToken(TEST_STUDENT_ID, TEST_NAME);
        
        assertTrue(jwtUtil.validateTokenFormat(token), "有效的 Token 格式应该通过验证");
        assertFalse(jwtUtil.validateTokenFormat("invalid.token.format"), "无效的 Token 格式不应该通过验证");
    }

    @Test
    void testGetTokenRemainingTime() {
        // 测试获取 Token 剩余时间
        String token = jwtUtil.generateToken(TEST_STUDENT_ID, TEST_NAME);
        long remainingTime = jwtUtil.getTokenRemainingTime(token);
        
        assertTrue(remainingTime > 0, "剩余时间应该大于 0");
        assertTrue(remainingTime <= TEST_EXPIRATION, "剩余时间不应该超过有效期");
        
        System.out.println("Token 剩余时间（毫秒）: " + remainingTime);
        System.out.println("Token 剩余时间（小时）: " + (remainingTime / (60 * 60 * 1000)));
    }

    @Test
    void testShouldRefreshToken() {
        // 测试是否需要刷新 Token（新生成的不需要）
        String token = jwtUtil.generateToken(TEST_STUDENT_ID, TEST_NAME);
        
        assertFalse(jwtUtil.shouldRefreshToken(token), "新生成的 Token 不需要刷新（剩余时间超过 2 小时）");
    }

    @Test
    void testRefreshToken() {
        // 测试刷新 Token
        String oldToken = jwtUtil.generateToken(TEST_STUDENT_ID, TEST_NAME, TEST_EMAIL);
        
        // 稍微等待一下，确保新 Token 的签发时间不同
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        String newToken = jwtUtil.refreshToken(oldToken);
        
        assertNotNull(newToken, "刷新后的 Token 不应为空");
        assertNotEquals(oldToken, newToken, "新旧 Token 应该不同");
        
        // 验证新 Token 包含相同的用户信息
        assertEquals(TEST_STUDENT_ID, jwtUtil.getStudentIdFromToken(newToken), "学号应该相同");
        assertEquals(TEST_NAME, jwtUtil.getNameFromToken(newToken), "姓名应该相同");
        assertEquals(TEST_EMAIL, jwtUtil.getEmailFromToken(newToken), "邮箱应该相同");
    }

    @Test
    void testGetTokenInfo() {
        // 测试获取 Token 配置信息
        Map<String, Object> info = jwtUtil.getTokenInfo();
        
        assertNotNull(info);
        assertEquals("HS512", info.get("algorithm"), "算法应该是 HS512");
        assertEquals(TEST_EXPIRATION, info.get("expirationMs"), "过期时间（毫秒）应该匹配");
        assertEquals(24L, info.get("expirationHours"), "过期时间（小时）应该是 24");
        
        System.out.println("Token 配置信息: " + info);
    }

    @Test
    void testInvalidToken() {
        // 测试无效的 Token
        String invalidToken = "invalid.jwt.token";
        
        assertThrows(JwtException.class, () -> {
            jwtUtil.getStudentIdFromToken(invalidToken);
        }, "无效的 Token 应该抛出异常");
    }

    @Test
    void testTokenWithDifferentSecret() {
        // 测试用不同密钥生成的 Token 无法验证
        String token = jwtUtil.generateToken(TEST_STUDENT_ID, TEST_NAME);
        
        // 创建使用不同密钥的 JwtUtil
        JwtUtil differentJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(differentJwtUtil, "secret", "DifferentSecretKeyThatIsAtLeast512BitsLongForHS512AlgorithmTestingPurposesOnly");
        ReflectionTestUtils.setField(differentJwtUtil, "expiration", TEST_EXPIRATION);
        
        assertThrows(JwtException.class, () -> {
            differentJwtUtil.getStudentIdFromToken(token);
        }, "使用不同密钥应该无法解析 Token");
    }

    @Test
    void testHS512AlgorithmStrength() {
        // 验证 Token 是使用 HS512 算法生成的
        String token = jwtUtil.generateToken(TEST_STUDENT_ID, TEST_NAME);
        
        // JWT Token 格式: header.payload.signature
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT Token 应该包含三个部分");
        
        // HS512 生成的签名应该比 HS256 更长
        String signature = parts[2];
        assertTrue(signature.length() > 85, "HS512 签名应该足够长（通常 > 85 字符）");
        
        System.out.println("签名长度: " + signature.length());
    }
}
