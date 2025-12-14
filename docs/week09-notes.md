# Week 09 学习笔记 - Gateway 路由与 JWT 认证

## 📋 目录

- [Gateway 路由配置说明](#gateway-路由配置说明)
- [JWT 认证流程说明](#jwt-认证流程说明)
- [测试验证结果](#测试验证结果)

---

## 🌐 Gateway 路由配置说明

### 1. 服务注册与发现

Gateway 通过 Nacos 实现服务发现和路由转发：

```yaml
spring:
  application:
    name: gateway-service
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        namespace: dev
        group: COURSEHUB_GROUP
        ephemeral: true
        heart-beat-interval: 5000
        heart-beat-timeout: 15000
```

**关键配置**：

- **namespace**: `dev` - 开发环境隔离
- **group**: `COURSEHUB_GROUP` - 服务分组
- **ephemeral**: `true` - 临时实例（服务停止自动注销）
- **heart-beat-interval**: 5s - 心跳间隔

### 2. 路由规则

Gateway 配置了 4 条主要路由规则：

#### 2.1 课程管理路由（Catalog Service）

```yaml
- id: catalog-service-courses
  uri: lb://catalog-service          # 负载均衡地址
  predicates:
    - Path=/api/courses/**            # 匹配规则
  filters:
    - name: CircuitBreaker            # 熔断器
      args:
        name: catalogCircuitBreaker
        fallbackUri: forward:/fallback/catalog
    - name: Retry                      # 重试机制
      args:
        retries: 3
        statuses: BAD_GATEWAY,GATEWAY_TIMEOUT
        methods: GET
        backoff:
          firstBackoff: 100ms
          maxBackoff: 500ms
          factor: 2
```

**功能说明**：

- **Path 匹配**: `/api/courses/**` → `catalog-service`
- **负载均衡**: `lb://catalog-service` 使用 Nacos 服务发现
- **熔断保护**: 服务异常时降级到 `/fallback/catalog`
- **重试策略**: GET 请求失败重试 3 次，指数退避（100ms → 200ms → 400ms）

#### 2.2 选课管理路由（Enrollment Service）

```yaml
- id: enrollment-service-enrollments
  uri: lb://enrollment-service
  predicates:
    - Path=/api/enrollments/**
  filters:
    - name: CircuitBreaker
      args:
        name: enrollmentCircuitBreaker
        fallbackUri: forward:/fallback/enrollment
    - name: Retry
      args:
        retries: 3
        statuses: BAD_GATEWAY,GATEWAY_TIMEOUT
        methods: GET
```

**功能说明**：

- **Path 匹配**: `/api/enrollments/**` → `enrollment-service`
- **熔断保护**: 降级到 `/fallback/enrollment`
- **重试策略**: 仅对 GET 请求重试，避免重复提交

#### 2.3 学生管理路由（User Service）

```yaml
- id: user-service-students
  uri: lb://user-service
  predicates:
    - Path=/api/students/**
  filters:
    - name: CircuitBreaker
      args:
        name: userCircuitBreaker
        fallbackUri: forward:/fallback/user
    - name: Retry
      args:
        retries: 3
        statuses: BAD_GATEWAY,GATEWAY_TIMEOUT
        methods: GET
```

**功能说明**：

- **Path 匹配**: `/api/students/**` → `user-service`
- **熔断保护**: 降级到 `/fallback/user`

#### 2.4 认证接口路由（User Service Auth）

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

**功能说明**：

- **Path 匹配**: `/api/auth/**` → `user-service`（登录、注册）
- **无重试**: 认证请求不重试，避免重复处理

### 3. 路由转发流程

```
客户端请求
    ↓
Gateway (8090)
    ↓
JWT 认证过滤器 (优先级 -100)
    ↓
路由匹配 (predicates)
    ↓
熔断器 + 重试过滤器
    ↓
负载均衡 (lb://)
    ↓
目标服务 (catalog/enrollment/user)
```

### 4. CORS 跨域配置

```yaml
globalcors:
  cors-configurations:
    '[/**]':
      allowedOriginPatterns: "*"
      allowedMethods: [GET, POST, PUT, DELETE, OPTIONS]
      allowedHeaders: "*"
      exposedHeaders: [Authorization, X-Token-Refresh-Required, X-User-Id]
      allowCredentials: false
      maxAge: 3600
```

**关键特性**：

- **暴露响应头**: 允许前端读取 `Authorization`, `X-User-Id` 等自定义头
- **预检缓存**: OPTIONS 请求结果缓存 1 小时

---

## 🔐 JWT 认证流程说明

### 1. 认证架构

```
                    ┌─────────────────┐
                    │  客户端 (前端)  │
                    └────────┬────────┘
                             │
                    1. POST /api/auth/login
                      { studentId, password }
                             │
                             ▼
                    ┌─────────────────┐
                    │  Gateway (8090) │
                    └────────┬────────┘
                             │
                    2. 白名单放行 (/api/auth/**)
                             │
                             ▼
                    ┌─────────────────┐
                    │  User Service   │
                    │  AuthController │
                    └────────┬────────┘
                             │
                    3. BCrypt 密码验证
                             │
                    4. 生成 JWT Token
                             │
                             ▼
                    返回 Token 给客户端
                             │
                             ▼
          ┌──────────────────────────────────┐
          │  后续请求携带 Token               │
          │  Authorization: Bearer <token>    │
          └──────────────┬───────────────────┘
                         │
                5. JWT 认证过滤器验证
                         │
                6. 提取用户信息添加到请求头
                         │
                         ▼
                  转发到后端服务
```

### 2. JWT 认证过滤器实现

#### 2.1 核心代码结构

[gateway-service/src/main/java/com/zjsu/ybz/gateway/filter/JwtAuthenticationFilter.java](../gateway-service/src/main/java/com/zjsu/ybz/gateway/filter/JwtAuthenticationFilter.java)

```java
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
  
    @Autowired
    private JwtUtil jwtUtil;
  
    /**
     * 白名单：不需要 JWT 认证的路径
     */
    private static final List<String> WHITE_LIST = Arrays.asList(
        "/api/auth/login",           // 登录接口
        "/api/auth/register",        // 注册接口
        "/api/auth/validate",        // Token 验证接口
        "/fallback/**",              // 降级接口
        "/actuator/**"               // 监控端点
    );
  
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
      
        // 1. 白名单检查
        if (isWhiteListPath(path)) {
            return chain.filter(exchange);
        }
      
        // 2. 提取 Token
        String token = extractToken(exchange.getRequest());
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange.getResponse(), "未提供认证 Token");
        }
      
        // 3. 验证 Token
        if (!jwtUtil.validateToken(token)) {
            return unauthorized(exchange.getResponse(), "Token 无效或已过期");
        }
      
        // 4. 提取用户信息
        Map<String, String> userInfo = jwtUtil.extractUserInfo(token);
      
        // 5. 添加到请求头，传递给下游服务
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
            .header("X-User-Id", userInfo.get("studentId"))
            .header("X-User-Name", userInfo.get("name"))
            .header("X-User-Email", userInfo.get("email"))
            .build();
      
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }
  
    @Override
    public int getOrder() {
        return -100;  // 优先级最高，在所有过滤器之前执行
    }
}
```

#### 2.2 Token 提取方式

支持两种方式提取 Token：

**方式 1: Authorization Header（推荐）**

```http
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**方式 2: Query Parameter（备用）**

```http
GET /api/students?token=eyJhbGciOiJIUzUxMiJ9...
```

代码实现：

```java
private String extractToken(ServerHttpRequest request) {
    // 方式 1: 从 Authorization Header 提取
    String authHeader = request.getHeaders().getFirst("Authorization");
    if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
        return authHeader.substring(7);
    }
  
    // 方式 2: 从 Query Parameter 提取
    String tokenParam = request.getQueryParams().getFirst("token");
    if (StringUtils.hasText(tokenParam)) {
        return tokenParam;
    }
  
    return null;
}
```

### 3. JWT Token 生成（User Service）

#### 3.1 Token 结构

```json
{
  "header": {
    "alg": "HS512",
    "typ": "JWT"
  },
  "payload": {
    "sub": "学生学号",
    "studentId": "202301001",
    "name": "张三",
    "email": "zhangsan@zjsu.edu.cn",
    "iat": 1699999999,
    "exp": 1700086399
  },
  "signature": "..."
}
```

#### 3.2 生成代码

[user-service/src/main/java/com/zjsu/ybz/user/service/AuthService.java](../user-service/src/main/java/com/zjsu/ybz/user/service/AuthService.java)

```java
public String generateToken(Student student) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expiration);  // 24小时
  
    Map<String, Object> claims = new HashMap<>();
    claims.put("studentId", student.getStudentId());
    claims.put("name", student.getName());
    claims.put("email", student.getEmail());
  
    return Jwts.builder()
        .subject(student.getStudentId())
        .claims(claims)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(getSigningKey(), SignatureAlgorithm.HS512)
        .compact();
}
```

**关键参数**：

- **算法**: HS512（对称加密）
- **密钥**: 512-bit (64字节) 密钥
- **过期时间**: 24小时 (86400000ms)

### 4. 用户信息传递机制

#### 4.1 请求头传递

Gateway 验证 Token 后，提取用户信息添加到请求头：

```http
X-User-Id: 202301001
X-User-Name: 张三
X-User-Email: zhangsan@zjsu.edu.cn
X-Auth-Token: eyJhbGciOiJIUzUxMiJ9...
```

#### 4.2 后端服务接收

后端服务通过 `@RequestHeader` 获取用户信息：

```java
@GetMapping("/profile")
public ResponseEntity<Student> getProfile(
    @RequestHeader("X-User-Id") String studentId,
    @RequestHeader("X-User-Name") String name) {
    // 直接使用 studentId 查询数据，无需再次验证
    return ResponseEntity.ok(studentService.findByStudentId(studentId));
}
```

**优势**：

- 后端服务无需依赖 JWT 库
- 避免重复验证 Token
- 统一由 Gateway 处理认证逻辑

### 5. 白名单管理

不需要认证的路径：

| 路径                   | 说明       | 原因           |
| ---------------------- | ---------- | -------------- |
| `/api/auth/login`    | 登录接口   | 用户尚未登录   |
| `/api/auth/register` | 注册接口   | 用户尚未登录   |
| `/api/auth/validate` | Token 验证 | 公开验证接口   |
| `/fallback/**`       | 降级接口   | 熔断器回退路径 |
| `/actuator/health`   | 健康检查   | 监控需要       |

**注意**: 白名单使用 `startsWith` 匹配，确保子路径也生效。

### 6. Token 刷新机制

#### 6.1 刷新检测

Gateway 检测 Token 剩余时间，提示前端刷新：

```java
if (jwtUtil.shouldRefreshToken(token)) {
    long remainingTime = jwtUtil.getTokenRemainingTime(token);
    logger.info("Token 即将过期，建议刷新 - 剩余时间: {}ms", remainingTime);
  
    // 添加响应头提示前端
    exchange.getResponse().getHeaders()
        .add("X-Token-Refresh-Required", "true");
}
```

#### 6.2 刷新策略

[gateway-service/src/main/java/com/zjsu/ybz/gateway/util/JwtUtil.java](../gateway-service/src/main/java/com/zjsu/ybz/gateway/util/JwtUtil.java)

```java
/**
 * 检查 Token 是否需要刷新
 * 策略：剩余时间少于 20% 时建议刷新
 */
public boolean shouldRefreshToken(String token) {
    long remainingTime = getTokenRemainingTime(token);
    long refreshThreshold = expiration / 5;  // 20% 阈值（约 4.8 小时）
    return remainingTime < refreshThreshold;
}
```

#### 6.3 前端处理

前端监听响应头 `X-Token-Refresh-Required`，调用刷新接口：

```javascript
axios.interceptors.response.use(response => {
    if (response.headers['x-token-refresh-required'] === 'true') {
        // 调用刷新接口获取新 Token
        refreshToken();
    }
    return response;
});
```

### 7. 错误处理

#### 7.1 401 未授权响应

```java
private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
  
    String body = String.format(
        "{\"status\":401,\"message\":\"%s\",\"timestamp\":%d}", 
        message, System.currentTimeMillis()
    );
  
    return response.writeWith(
        Mono.just(response.bufferFactory().wrap(body.getBytes()))
    );
}
```

#### 7.2 常见错误场景

| 错误         | 原因                      | 响应                                              |
| ------------ | ------------------------- | ------------------------------------------------- |
| 未提供 Token | 缺少 `Authorization` 头 | `{"status":401,"message":"未提供认证 Token"}`   |
| Token 无效   | 签名验证失败              | `{"status":401,"message":"Token 无效或已过期"}` |
| Token 过期   | `exp` 时间已过          | `{"status":401,"message":"Token 无效或已过期"}` |

---

## ✅ 测试验证结果

### 1. 测试脚本

使用 Python 脚本进行自动化测试：[test-microservices.py](../test-microservices.py)

### 2. 测试用例覆盖

| 测试类别         | 测试用例数   | 状态                  |
| ---------------- | ------------ | --------------------- |
| Gateway 注册检查 | 5            | ✅ 通过               |
| 路由转发测试     | 12           | ✅ 通过               |
| JWT 认证过滤器   | 3            | ✅ 通过               |
| 登录接口         | 3            | ✅ 通过               |
| 用户信息提取     | 2            | ✅ 通过               |
| **总计**   | **25** | **✅ 全部通过** |

### 3. 测试结果摘要

```
========================================
📊 测试结果汇总
========================================
✅ 测试套件 1: Gateway 服务注册检查 - 5/5 通过
✅ 测试套件 2: Gateway 路由配置测试 - 12/12 通过
✅ 测试套件 3: JWT 认证过滤器测试 - 3/3 通过
✅ 测试套件 4: 登录接口 JWT Token 测试 - 3/3 通过
✅ 测试套件 5: 后端服务用户信息提取测试 - 2/2 通过

========================================
总计: 25/25 测试通过 (100%)
========================================
```

### 4. 关键测试验证

#### 4.1 Gateway 注册到 Nacos

```python
# 测试用例: 验证 Gateway 在 Nacos 注册
response = requests.get(
    "http://localhost:8848/nacos/v1/ns/instance/list",
    params={
        "serviceName": "gateway-service",
        "namespaceId": "dev",
        "groupName": "COURSEHUB_GROUP"
    }
)
assert response.json()["count"] >= 1
```

✅ **结果**: Gateway 成功注册，实例数 ≥ 1

#### 4.2 路由转发验证

```python
# 测试用例: 验证课程路由转发
response = requests.get(
    "http://localhost:8090/api/courses",
    headers={"Authorization": f"Bearer {jwt_token}"}
)
assert response.status_code == 200
```

✅ **结果**: 请求正确转发到 `catalog-service`

#### 4.3 JWT 认证过滤器

```python
# 测试用例: 无 Token 访问受保护接口
response = requests.get("http://localhost:8090/api/students")
assert response.status_code == 401
assert "未提供认证 Token" in response.text
```

✅ **结果**: 认证过滤器正常拦截

#### 4.4 登录返回 JWT Token

```python
# 测试用例: 登录获取 Token
response = requests.post(
    "http://localhost:8090/api/auth/login",
    json={"studentId": "test_1234567890", "password": "password123"}
)
token = response.json()["data"]["token"]
assert token is not None and len(token) > 0
```

✅ **结果**: 成功生成 JWT Token (长度 > 200 字符)

#### 4.5 用户信息提取

```python
# 测试用例: 验证请求头包含用户信息
response = requests.get(
    f"http://localhost:8100/api/students/{test_user_id}",
    headers={"Authorization": f"Bearer {jwt_token}"}
)
# 后端日志显示收到 X-User-Id, X-User-Name, X-User-Email 请求头
```

✅ **结果**: 用户信息正确传递到后端服务

---

## 📝 总结

### 成功实现的功能

1. ✅ **Gateway 服务注册**: 成功注册到 Nacos (`dev` 命名空间，`COURSEHUB_GROUP` 分组)
2. ✅ **路由配置**: 4 条路由规则正确转发请求到对应服务
3. ✅ **JWT 认证过滤器**: 全局过滤器拦截未认证请求，白名单正常工作
4. ✅ **JWT Token 生成**: 登录接口返回有效 HS512 Token，24 小时有效期
5. ✅ **用户信息传递**: Gateway 提取 Token 信息添加到请求头，后端服务成功接收

### 技术亮点

- **负载均衡**: `lb://` 协议结合 Nacos 实现服务发现
- **熔断保护**: CircuitBreaker 提供降级机制
- **重试策略**: 指数退避算法减少服务压力
- **CORS 支持**: 完善的跨域配置支持前后端分离
- **Token 刷新**: 智能检测过期时间，提前提示刷新

### 最佳实践

1. **认证集中化**: Gateway 统一处理认证，后端服务无需重复验证
2. **白名单管理**: 合理配置白名单，避免认证死循环
3. **请求头传递**: 通过 `X-User-*` 头传递用户信息，解耦 JWT 依赖
4. **错误处理**: 统一 401 响应格式，便于前端处理
5. **日志记录**: 完善的日志输出，便于问题排查

---

**文档日期**: 2024-12-21
**测试环境**: Docker Compose + Spring Cloud 2023.0.3 + Spring Boot 3.2.1
**验证状态**: ✅ 全部测试通过 (25/25)

![1765726932564](image/week09-notes/1765726932564.png)

![1765726951232](image/week09-notes/1765726951232.png)

![1765726957176](image/week09-notes/1765726957176.png)

![1765726965177](image/week09-notes/1765726965177.png)
