CREATE DATABASE IF NOT EXISTS user_db;
CREATE USER IF NOT EXISTS 'user_user'@'localhost' IDENTIFIED BY 'user_pass';
GRANT ALL PRIVILEGES ON user_db.* TO 'user_user'@'localhost';
FLUSH PRIVILEGES;

USE user_db;

-- 学生表（添加密码字段）
CREATE TABLE IF NOT EXISTS students (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    major VARCHAR(100),
    grade INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 添加索引
CREATE INDEX idx_student_id ON students(student_id);
CREATE INDEX idx_email ON students(email);

-- 插入测试数据（密码为 BCrypt 加密后的 "123456"）
INSERT INTO students (id, student_id, name, email, password, major, grade) 
VALUES 
    (UUID(), '2021001', '张三', 'zhangsan@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '计算机科学', 2021),
    (UUID(), '2021002', '李四', 'lisi@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '软件工程', 2021),
    (UUID(), '2021003', '王五', 'wangwu@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '数据科学', 2021)
ON DUPLICATE KEY UPDATE name=name;
