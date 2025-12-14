# User Service 登录认证功能

## 功能概述

User Service 现已集成完整的 JWT 身份认证系统，包括：

- ✅ 用户登录（验证学号和密码）
- ✅ 用户注册（创建新账号）
- ✅ JWT Token 生成
- ✅ Token 验证和刷新
- ✅ 密码 BCrypt 加密存储
- ✅ 通过 Gateway 路由访问

## 快速开始

### 1. 测试账号

数据库中预置了以下测试账号（密码均为 `123456`）：

| 学号 | 姓名 | 邮箱 |
|------|------|------|
| 2021001 | 张三 | zhangsan@example.com |
| 2021002 | 李四 | lisi@example.com |
| 2021003 | 王五 | wangwu@example.com |

### 2. 登录获取 Token

```bash
curl -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "2021001",
    "password": "123456"
  }'
```

**响应示例：**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdHVkZW50SWQiOiIyMDIxMDAxIiwibmFtZSI6IuW8oOS4iSIsImlhdCI6MTYzOTEyMzQ1NiwiZXhwIjoxNjM5MjA5ODU2fQ.xxx",
  "studentId": "2021001",
  "name": "张三",
  "email": "zhangsan@example.com",
  "message": "登录成功"
}
```

### 3. 使用 Token 访问受保护资源

```bash
TOKEN="your-jwt-token-here"

curl -X GET http://localhost:8090/api/students/studentId/2021001 \
  -H "Authorization: Bearer $TOKEN"
```

### 4. 注册新用户

```bash
curl -X POST http://localhost:8090/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "2021004",
    "name": "新用户",
    "password": "mypassword123",
    "email": "newuser@example.com",
    "major": "软件工程",
    "grade": 2021
  }'
```

## API 端点

### 认证相关

| 方法 | 路径 | 说明 | 需要 Token |
|------|------|------|-----------|
| POST | `/api/auth/login` | 用户登录 | ❌ |
| POST | `/api/auth/register` | 用户注册 | ❌ |
| GET | `/api/auth/validate` | 验证 Token | ✅ |
| POST | `/api/auth/refresh` | 刷新 Token | ✅ |
| POST | `/api/auth/logout` | 用户登出 | ❌ |

### 学生管理

| 方法 | 路径 | 说明 | 需要 Token |
|------|------|------|-----------|
| GET | `/api/students` | 获取所有学生 | 建议 ✅ |
| GET | `/api/students/{id}` | 获取指定学生 | 建议 ✅ |
| GET | `/api/students/studentId/{studentId}` | 通过学号查询 | 建议 ✅ |
| POST | `/api/students` | 创建学生 | 建议 ✅ |
| PUT | `/api/students/{id}` | 更新学生信息 | 建议 ✅ |
| DELETE | `/api/students/{id}` | 删除学生 | 建议 ✅ |

## 技术实现

### JWT Token 配置

```yaml
jwt:
  secret: YourSecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLongForHS256Algorithm
  expiration: 86400000  # 24小时
```

- **算法**: HS256
- **有效期**: 24 小时
- **包含信息**: 学号、姓名、签发时间、过期时间

### 密码加密

- **算法**: BCrypt
- **强度**: 10（默认）
- **存储**: 60 字符哈希值（格式：`$2a$10$...`）

### 数据库变更

Student 表新增字段：
```sql
password VARCHAR(255) NOT NULL  -- BCrypt 加密后的密码
```

## 项目结构

```
user-service/
├── src/main/java/com/zjsu/ybz/course/
│   ├── controller/
│   │   ├── AuthController.java          # 认证控制器
│   │   └── StudentController.java       # 学生管理控制器
│   ├── service/
│   │   ├── AuthService.java             # 认证服务
│   │   └── StudentService.java          # 学生服务
│   ├── model/
│   │   └── Student.java                 # 学生实体（含密码字段）
│   ├── dto/
│   │   ├── LoginRequest.java            # 登录请求 DTO
│   │   ├── LoginResponse.java           # 登录响应 DTO
│   │   └── RegisterRequest.java         # 注册请求 DTO
│   ├── repository/
│   │   └── StudentRepository.java       # 数据访问层
│   └── util/
│       └── JwtUtil.java                 # JWT 工具类
├── pom.xml                              # 新增 JWT 和 Security 依赖
├── application.yml                      # JWT 配置
├── mysql.sql                            # 数据库脚本（含密码字段）
├── AUTH_API.md                          # API 详细文档
└── User-Auth-API.postman_collection.json # Postman 测试集合
```

## 测试

### 自动化测试脚本

```bash
# 在项目根目录执行
wsl bash /mnt/f/school/微服务开发/CSS_microservices/scripts/test-auth-api.sh
```

测试内容包括：
1. ✅ 正常登录
2. ✅ Token 验证
3. ✅ 使用 Token 访问资源
4. ✅ 错误密码登录（401）
5. ✅ 注册新用户
6. ✅ 重复注册（400）
7. ✅ 无效 Token 验证（400/401）

### Postman 测试

1. 导入 `User-Auth-API.postman_collection.json`
2. 设置环境变量 `baseUrl=http://localhost:8090`
3. 执行测试用例
4. Token 会自动保存到环境变量中

## 安全注意事项

### 生产环境配置

1. **修改 JWT 密钥**
   ```yaml
   jwt:
     secret: "使用强随机字符串（至少256位）"
   ```

2. **使用 HTTPS**
   - 所有包含 Token 的请求必须通过 HTTPS 传输
   - 防止 Token 在网络传输中被截获

3. **Token 存储**
   - 前端使用 localStorage 或 sessionStorage
   - 不要在 URL 中传递 Token
   - 设置 HttpOnly Cookie（可选）

4. **密码策略**
   - 实施密码复杂度要求
   - 限制登录失败次数
   - 实现账户锁定机制

5. **Token 刷新**
   - 实现 Refresh Token 机制
   - 缩短 Access Token 有效期
   - 支持 Token 黑名单

## Gateway 路由配置

Gateway 已配置认证路由：

```yaml
- id: user-service-auth
  uri: lb://user-service
  predicates:
    - Path=/api/auth/**
  filters:
    - name: CircuitBreaker
      args:
        name: userCircuitBreaker
        fallbackUri: forward:/fallback/user
```

所有 `/api/auth/**` 请求会自动路由到 user-service。

## 依赖说明

### 新增依赖

```xml
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- Spring Security Crypto -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

## 常见问题

### Q1: Token 过期了怎么办？
A: 客户端收到 401 错误时，需要跳转到登录页重新登录获取新 Token。

### Q2: 如何在前端保存 Token？
A: 推荐使用 localStorage：
```javascript
localStorage.setItem('token', response.data.token);
```

### Q3: 如何在每个请求中携带 Token？
A: 在请求头中添加 Authorization：
```javascript
headers: {
  'Authorization': `Bearer ${token}`
}
```

### Q4: 密码忘记了怎么办？
A: 当前版本未实现密码重置功能，需要管理员直接修改数据库。

### Q5: 为什么注册后需要再次登录？
A: 注册成功后会直接返回 Token，不需要再次登录。

## 下一步优化建议

1. **实现 Refresh Token**
   - 分离 Access Token 和 Refresh Token
   - Access Token 短期有效（15分钟）
   - Refresh Token 长期有效（7天）

2. **添加权限管理**
   - 实现 RBAC（基于角色的访问控制）
   - 区分学生、教师、管理员角色
   - 在 Token 中包含角色信息

3. **实现密码重置**
   - 邮箱验证码重置
   - 密码找回功能
   - 安全问题验证

4. **增强安全性**
   - 登录失败次数限制
   - 账户临时锁定
   - 异地登录提醒
   - 登录日志记录

5. **Token 黑名单**
   - 使用 Redis 存储黑名单
   - 支持强制登出
   - Token 撤销功能

## 参考文档

- [完整 API 文档](AUTH_API.md)
- [Postman 测试集合](User-Auth-API.postman_collection.json)
- [JWT 官方文档](https://jwt.io/)
- [BCrypt 加密算法](https://en.wikipedia.org/wiki/Bcrypt)
