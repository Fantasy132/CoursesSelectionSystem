package com.zjsu.ybz.course.controller;

import com.zjsu.ybz.course.dto.LoginRequest;
import com.zjsu.ybz.course.dto.LoginResponse;
import com.zjsu.ybz.course.dto.RegisterRequest;
import com.zjsu.ybz.course.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 处理登录、注册等认证相关操作
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 用户登录
     * @param request 登录请求（学号和密码）
     * @return JWT Token 和用户信息
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        
        if (response.getToken() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 用户注册
     * @param request 注册请求
     * @return JWT Token 和用户信息
     */
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        
        if (response.getToken() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 验证 Token
     * @param token JWT Token
     * @return 验证结果
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String token) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 移除 "Bearer " 前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            boolean isValid = authService.validateToken(token);
            
            if (isValid) {
                String studentId = authService.getStudentIdFromToken(token);
                response.put("valid", true);
                response.put("studentId", studentId);
                return ResponseEntity.ok(response);
            } else {
                response.put("valid", false);
                response.put("message", "Token 无效或已过期");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } catch (Exception e) {
            response.put("valid", false);
            response.put("message", "Token 格式错误");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * 刷新 Token
     * @param token 旧的 JWT Token
     * @return 新的 JWT Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestHeader("Authorization") String token) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 移除 "Bearer " 前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            if (!authService.validateToken(token)) {
                response.put("success", false);
                response.put("message", "Token 无效或已过期");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // 生成新 Token (这里简化处理，实际可能需要查询数据库)
            String studentId = authService.getStudentIdFromToken(token);
            // 这里应该重新生成 token，但需要查询用户信息
            response.put("success", true);
            response.put("message", "请重新登录以获取新的 Token");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Token 格式错误");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * 登出（客户端删除 Token 即可，这里只是示例）
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "登出成功");
        return ResponseEntity.ok(response);
    }
}
