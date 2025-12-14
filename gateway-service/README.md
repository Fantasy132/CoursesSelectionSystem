# Gateway Service - API 网关服务

## 概述
Gateway Service 是微服务架构的统一入口，负责路由转发、负载均衡、熔断降级等功能。

## 服务端口
- **8090**

## 功能特性

### 1. 服务路由
网关会将请求自动路由到对应的微服务：

| 路径 | 目标服务 | 说明 |
|------|---------|------|
| `/api/courses/**` | catalog-service | 课程管理相关API |
| `/api/enrollments/**` | enrollment-service | 选课管理相关API |
| `/api/students/**` | user-service | 学生管理相关API |

### 2. 负载均衡
- 使用 Spring Cloud LoadBalancer
- 通过 Nacos 服务发现自动负载均衡
- 支持多实例轮询

### 3. 熔断降级
- 集成 Resilience4j Circuit Breaker
- 配置参数：
  - 滑动窗口大小：10 次调用
  - 失败率阈值：50%
  - 熔断等待时间：30 秒
  - 请求超时时间：5 秒

### 4. 重试机制
- GET 请求自动重试最多 3 次
- 仅对 BAD_GATEWAY 和 GATEWAY_TIMEOUT 错误重试
- 指数退避策略：100ms → 200ms → 400ms

### 5. 跨域支持
- 全局 CORS 配置
- 支持所有常用 HTTP 方法
- 允许所有请求头

### 6. 请求日志
- 全局日志过滤器
- 记录请求方法、路径、来源
- 记录响应状态和处理耗时

## API 端点示例

### 课程服务 (Catalog Service)
```bash
# 获取所有课程
GET http://localhost:8090/api/courses

# 获取指定课程
GET http://localhost:8090/api/courses/{id}

# 通过课程代码查询
GET http://localhost:8090/api/courses/code/{code}

# 创建课程
POST http://localhost:8090/api/courses

# 更新课程
PUT http://localhost:8090/api/courses/{id}

# 删除课程
DELETE http://localhost:8090/api/courses/{id}
```

### 选课服务 (Enrollment Service)
```bash
# 创建选课记录
POST http://localhost:8090/api/enrollments

# 删除选课记录
DELETE http://localhost:8090/api/enrollments/{id}

# 获取所有选课记录
GET http://localhost:8090/api/enrollments

# 获取某课程的选课记录
GET http://localhost:8090/api/enrollments/course/{courseId}

# 获取某学生的选课记录
GET http://localhost:8090/api/enrollments/student/{studentId}
```

### 用户服务 (User Service)
```bash
# 获取所有学生
GET http://localhost:8090/api/students

# 获取指定学生
GET http://localhost:8090/api/students/{id}

# 通过学号查询
GET http://localhost:8090/api/students/studentId/{studentId}

# 创建学生
POST http://localhost:8090/api/students

# 更新学生信息
PUT http://localhost:8090/api/students/{id}

# 删除学生
DELETE http://localhost:8090/api/students/{id}
```

## 监控端点

### Gateway 监控
```bash
# 查看网关路由信息
GET http://localhost:8090/actuator/gateway/routes

# 查看网关过滤器
GET http://localhost:8090/actuator/gateway/globalfilters
```

### 熔断器监控
```bash
# 查看熔断器状态
GET http://localhost:8090/actuator/circuitbreakers

# 查看熔断器事件
GET http://localhost:8090/actuator/circuitbreakerevents
```

### 健康检查
```bash
# 查看服务健康状态
GET http://localhost:8090/actuator/health

# 查看 Nacos 注册信息
GET http://localhost:8090/actuator/nacos-discovery
```

## 服务降级

当后端服务不可用时，网关会返回友好的降级响应：

```json
{
  "success": false,
  "message": "课程服务暂时不可用，请稍后重试",
  "timestamp": "2025-12-10T16:45:00",
  "status": 503
}
```

## 配置说明

### Nacos 配置
- 服务器地址：`localhost:8848`（可通过环境变量 `NACOS_SERVER_ADDR` 覆盖）
- 命名空间：`dev`
- 分组：`COURSEHUB_GROUP`

### 环境变量
- `NACOS_SERVER_ADDR`: Nacos 服务器地址（默认：localhost:8848）
- `NACOS_ENABLED`: 是否启用 Nacos（默认：true）
- `SERVER_PORT`: 服务端口（默认：8090）

## 构建和运行

### 本地运行
```bash
./mvnw clean package -DskipTests
java -jar target/gateway-service-0.0.1-SNAPSHOT.jar
```

### Docker 运行
```bash
docker build -t gateway-service .
docker run -p 8090:8090 -e NACOS_SERVER_ADDR=nacos:8848 gateway-service
```

### Docker Compose
```bash
docker-compose up gateway-service
```

## 日志级别
- Gateway：DEBUG
- Nacos：INFO
- Resilience4j：INFO

## 技术栈
- Spring Boot 3.5.6
- Spring Cloud Gateway 4.2.0
- Spring Cloud Alibaba Nacos 2023.0.3.2
- Resilience4j
- Java 21
