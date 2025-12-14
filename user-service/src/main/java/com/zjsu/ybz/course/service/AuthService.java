package com.zjsu.ybz.course.service;

import com.zjsu.ybz.course.dto.LoginRequest;
import com.zjsu.ybz.course.dto.LoginResponse;
import com.zjsu.ybz.course.dto.RegisterRequest;
import com.zjsu.ybz.course.model.Student;
import com.zjsu.ybz.course.repository.StudentRepository;
import com.zjsu.ybz.course.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 认证服务
 */
@Service
public class AuthService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        // 查找学生
        Optional<Student> studentOpt = studentRepository.findByStudentId(request.getStudentId());
        
        if (studentOpt.isEmpty()) {
            return LoginResponse.failure("学号或密码错误");
        }

        Student student = studentOpt.get();

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), student.getPassword())) {
            return LoginResponse.failure("学号或密码错误");
        }

        // 生成 JWT Token
        String token = jwtUtil.generateToken(student.getStudentId(), student.getName());

        return LoginResponse.success(token, student.getStudentId(), student.getName(), student.getEmail());
    }

    /**
     * 用户注册
     */
    public LoginResponse register(RegisterRequest request) {
        // 检查学号是否已存在
        if (studentRepository.findByStudentId(request.getStudentId()).isPresent()) {
            return LoginResponse.failure("学号已存在");
        }

        // 检查邮箱是否已存在
        if (studentRepository.findByEmail(request.getEmail()).isPresent()) {
            return LoginResponse.failure("邮箱已被使用");
        }

        // 创建新学生
        Student student = new Student();
        student.setStudentId(request.getStudentId());
        student.setName(request.getName());
        student.setPassword(passwordEncoder.encode(request.getPassword()));
        student.setEmail(request.getEmail());
        student.setMajor(request.getMajor());
        student.setGrade(request.getGrade());

        studentRepository.save(student);

        // 生成 JWT Token
        String token = jwtUtil.generateToken(student.getStudentId(), student.getName());

        return LoginResponse.success(token, student.getStudentId(), student.getName(), student.getEmail());
    }

    /**
     * 验证 Token
     */
    public boolean validateToken(String token) {
        return jwtUtil.validateTokenFormat(token);
    }

    /**
     * 从 Token 获取学号
     */
    public String getStudentIdFromToken(String token) {
        return jwtUtil.getStudentIdFromToken(token);
    }
}
