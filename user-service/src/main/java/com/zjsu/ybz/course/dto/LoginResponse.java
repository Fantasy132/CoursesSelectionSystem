package com.zjsu.ybz.course.dto;

/**
 * 登录响应 DTO
 */
public class LoginResponse {
    
    private String token;
    private String studentId;
    private String name;
    private String email;
    private String message;

    public LoginResponse() {
    }

    public LoginResponse(String token, String studentId, String name, String email) {
        this.token = token;
        this.studentId = studentId;
        this.name = name;
        this.email = email;
        this.message = "登录成功";
    }

    public static LoginResponse success(String token, String studentId, String name, String email) {
        return new LoginResponse(token, studentId, name, email);
    }

    public static LoginResponse failure(String message) {
        LoginResponse response = new LoginResponse();
        response.setMessage(message);
        return response;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
