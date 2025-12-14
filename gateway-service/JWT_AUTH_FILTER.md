# Gateway JWT 认证过滤器说明

## 功能概述

Gateway 现已集成 JWT 认证过滤器，提供统一的身份验证和授权管理。

## 核心功能

### 1. 白名单机制

以下路径**不需要** JWT 认证：

```
/api/auth/login          # 登录接口
/api/auth/register       # 注册接口
/api/auth/validate       # Token 验证接口
/fallback/catalog        # 降级接口
/fallback/enrollment     # 降级接口
/fallback/user           # 降级接口
/actuator/health         # 健康检查
/actuator/info           # 应用信息
```

### 2. Token 验证

- 验证 Token 签名（HS512 算法）
- 检查 Token 是否过期
- 提取用户信息（学号、姓名、邮箱）

### 3. 用户信息传递

验证成功后，自动添加以下请求头传递给下游服务：

```
X-User-Id: 学号
X-User-Name: 姓名
X-User-Email: 邮箱
X-Auth-Token: 原始 JWT Token
```

### 4. Token 刷新提示

当 Token 剩余有效期少于 2 小时时，响应头会包含：

```
X-Token-Refresh-Required: true
```

提示前端刷新 Token。

## 使用方式

### 客户端请求示例

#### 1. 登录（无需 Token）

```bash
curl -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "2021001",
    "password": "123456"
  }'
```

**响应：**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "studentId": "2021001",
  "name": "张三",
  "expiresIn": 86400000
}
```

#### 2. 访问受保护资源（需要 Token）

```bash
# 方式 1：使用 Authorization Header（推荐）
curl -X GET http://localhost:8090/api/courses \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."

# 方式 2：使用 Query Parameter（不推荐）
curl -X GET "http://localhost:8090/api/courses?token=eyJhbGciOiJIUzUxMiJ9..."
```

### 下游服务获取用户信息

下游服务（catalog-service、enrollment-service、user-service）可以直接从请求头获取用户信息：

```java
@GetMapping("/api/courses")
public ResponseEntity<?> getCourses(
    @RequestHeader("X-User-Id") String userId,
    @RequestHeader("X-User-Name") String userName,
    @RequestHeader(value = "X-User-Email", required = false) String email
) {
    // 使用用户信息进行业务逻辑处理
    logger.info("用户 {} ({}) 正在访问课程列表", userName, userId);
    // ...
}
```

## 错误处理

### 401 未授权

#### 1. 未提供 Token

**请求：**
```bash
curl -X GET http://localhost:8090/api/courses
```

**响应：**
```json
{
  "status": 401,
  "message": "未提供认证 Token",
  "timestamp": 1702540800000
}
```

#### 2. Token 无效或已过期

**请求：**
```bash
curl -X GET http://localhost:8090/api/courses \
  -H "Authorization: Bearer invalid_token"
```

**响应：**
```json
{
  "status": 401,
  "message": "Token 无效或已过期",
  "timestamp": 1702540800000
}
```

#### 3. Token 验证失败

**响应：**
```json
{
  "status": 401,
  "message": "Token 验证失败",
  "timestamp": 1702540800000
}
```

## 配置说明

### application.yml

```yaml
jwt:
  # JWT 密钥（必须至少 64 字节）
  secret: YourSecretKeyForJWTTokenGeneration...
  # Token 有效期（毫秒）：24 小时
  expiration: 86400000
```

**重要：** 生产环境必须修改 `jwt.secret`！

## 过滤器执行顺序

JWT 认证过滤器的优先级为 `-100`，在其他过滤器之前执行：

```
-100: JWT 认证过滤器（JwtAuthenticationFilter）
   0: 路由过滤器
  10: 日志过滤器（LoggingGlobalFilter）
```

## 日志记录

JWT 认证过滤器会记录以下日志：

```
DEBUG - JWT 认证过滤器 - 请求路径: /api/courses
DEBUG - 白名单路径，直接放行: /api/auth/login
DEBUG - JWT Token 验证成功 - 学号: 2021001, 姓名: 张三
WARN  - 未提供 JWT Token，拒绝访问: /api/courses
WARN  - JWT Token 验证失败: /api/students
INFO  - Token 即将过期，建议刷新 - 学号: 2021001, 剩余时间: 3600000ms
ERROR - JWT Token 处理异常: Token expired
```

## 测试场景

### 1. 白名单路径测试

```bash
# 登录（应该成功）
curl -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"studentId":"2021001","password":"123456"}'

# 健康检查（应该成功）
curl http://localhost:8090/actuator/health
```

### 2. 受保护路径测试

```bash
# 获取 Token
TOKEN=$(curl -s -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"studentId":"2021001","password":"123456"}' | jq -r '.token')

# 访问课程列表（应该成功）
curl -X GET http://localhost:8090/api/courses \
  -H "Authorization: Bearer $TOKEN"

# 访问选课列表（应该成功）
curl -X GET http://localhost:8090/api/enrollments \
  -H "Authorization: Bearer $TOKEN"

# 访问学生信息（应该成功）
curl -X GET http://localhost:8090/api/students/2021001 \
  -H "Authorization: Bearer $TOKEN"
```

### 3. 无效 Token 测试

```bash
# 使用无效 Token（应该返回 401）
curl -X GET http://localhost:8090/api/courses \
  -H "Authorization: Bearer invalid_token"

# 不提供 Token（应该返回 401）
curl -X GET http://localhost:8090/api/courses
```

## 前端集成示例

### JavaScript (Axios)

```javascript
// 配置 Axios 拦截器
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器 - 处理 Token 刷新
axios.interceptors.response.use(
  response => {
    // 检查是否需要刷新 Token
    if (response.headers['x-token-refresh-required'] === 'true') {
      console.log('Token 即将过期，建议刷新');
      // 调用刷新接口
      refreshToken();
    }
    return response;
  },
  error => {
    if (error.response && error.response.status === 401) {
      // Token 过期或无效，跳转登录
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// 刷新 Token
async function refreshToken() {
  const oldToken = localStorage.getItem('token');
  try {
    const response = await axios.post('/api/auth/refresh', { token: oldToken });
    localStorage.setItem('token', response.data.token);
  } catch (error) {
    console.error('Token 刷新失败', error);
    localStorage.removeItem('token');
    window.location.href = '/login';
  }
}
```

## 安全建议

1. ✅ **使用 HTTPS**：生产环境必须使用 HTTPS 传输 Token
2. ✅ **强密钥**：JWT 密钥至少 64 字节（512 位）
3. ✅ **Token 存储**：使用 localStorage 或 sessionStorage，不要存储在 Cookie 或 URL
4. ✅ **刷新机制**：实现 Token 刷新，避免频繁重新登录
5. ✅ **黑名单**：考虑实现 Token 黑名单（注销时添加）
6. ✅ **日志监控**：监控异常的认证失败行为
7. ✅ **CORS 配置**：正确配置跨域策略
8. ✅ **最小权限**：Token 中只包含必要信息

## 常见问题

### Q1: 为什么登录后访问其他接口还是返回 401？

A: 确认请求头中包含 `Authorization: Bearer <token>`，且 Token 格式正确。

### Q2: 如何修改白名单？

A: 编辑 [JwtAuthenticationFilter.java](gateway-service\src\main\java\com\zjsu\ybz\gateway\filter\JwtAuthenticationFilter.java) 中的 `WHITE_LIST` 常量。

### Q3: 下游服务如何获取用户信息？

A: 从请求头读取 `X-User-Id`、`X-User-Name`、`X-User-Email`。

### Q4: Token 过期时间可以修改吗？

A: 可以，修改 `application.yml` 中的 `jwt.expiration`（单位：毫秒）。

### Q5: 支持多种认证方式吗？

A: 当前支持两种方式：
   1. Authorization Header（推荐）
   2. Query Parameter（不推荐，用于特殊场景）

## 相关文档

- [JWT_HS512_GUIDE.md](../user-service/JWT_HS512_GUIDE.md) - JWT 工具类详细说明
- [AUTH_API.md](../user-service/AUTH_API.md) - 认证接口文档
- [QUICKSTART.md](QUICKSTART.md) - Gateway 快速开始指南
