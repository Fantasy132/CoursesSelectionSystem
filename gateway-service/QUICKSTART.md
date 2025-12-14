# Gateway Service 快速启动指南

## 前置条件
1. Java 21 已安装
2. Maven 已安装（或使用项目提供的 mvnw）
3. Nacos 服务已运行（端口 8848）
4. 其他微服务已启动（catalog-service, enrollment-service, user-service）

## 启动步骤

### 方式一：使用 Maven 本地运行

```bash
# 1. 进入项目目录
cd gateway-service

# 2. 编译打包（Windows）
.\mvnw.cmd clean package -DskipTests

# 或使用 WSL
wsl bash -c "cd /mnt/f/school/微服务开发/CSS_microservices/gateway-service && ./mvnw clean package -DskipTests"

# 3. 运行服务
java -jar target/gateway-service-0.0.1-SNAPSHOT.jar
```

### 方式二：使用 Docker Compose（推荐）

```bash
# 在项目根目录
docker-compose up gateway-service
```

## 验证启动成功

### 1. 健康检查
```bash
curl http://localhost:8090/actuator/health
```

预期响应：
```json
{
  "status": "UP"
}
```

### 2. 查看路由配置
```bash
curl http://localhost:8090/actuator/gateway/routes | jq '.'
```

### 3. 测试路由转发
```bash
# 测试课程服务
curl http://localhost:8090/api/courses

# 测试用户服务
curl http://localhost:8090/api/students

# 测试选课服务
curl http://localhost:8090/api/enrollments
```

## 使用测试脚本

```bash
# 在项目根目录下执行
wsl bash /mnt/f/school/微服务开发/CSS_microservices/scripts/test-gateway-routes.sh
```

## 导入 Postman 集合

1. 打开 Postman
2. 点击 Import
3. 选择文件：`gateway-service/Gateway-API-Tests.postman_collection.json`
4. 导入后即可开始测试所有 API 端点

## 监控和管理

### 查看服务注册信息
```bash
curl http://localhost:8090/actuator/nacos-discovery
```

### 查看熔断器状态
```bash
curl http://localhost:8090/actuator/circuitbreakers
```

### 查看熔断器事件
```bash
curl http://localhost:8090/actuator/circuitbreakerevents
```

## 常见问题

### 1. 端口 8090 已被占用
修改 `application.yml` 中的 `server.port` 或通过环境变量：
```bash
SERVER_PORT=8091 java -jar target/gateway-service-0.0.1-SNAPSHOT.jar
```

### 2. 无法连接到 Nacos
检查 Nacos 是否运行：
```bash
curl http://localhost:8848/nacos
```

或修改 Nacos 地址：
```bash
NACOS_SERVER_ADDR=your-nacos-server:8848 java -jar target/gateway-service-0.0.1-SNAPSHOT.jar
```

### 3. 路由返回 503 Service Unavailable
- 检查后端服务是否已启动
- 检查后端服务是否已注册到 Nacos
- 查看网关日志中的错误信息

### 4. 熔断器一直处于 OPEN 状态
- 检查后端服务健康状态
- 等待 30 秒（配置的 waitDurationInOpenState）
- 查看熔断器事件了解详细信息

## 日志查看

网关会记录所有请求的详细信息：
- 请求方法和路径
- 来源 IP 地址
- 响应状态码
- 请求处理耗时

日志级别设置：
- Gateway：DEBUG
- Nacos：INFO
- Resilience4j：INFO

## 性能调优建议

1. **调整熔断器参数**：根据实际业务调整 `slidingWindowSize`、`failureRateThreshold` 等
2. **调整超时时间**：根据后端服务响应时间调整 `timeoutDuration`
3. **调整重试策略**：根据幂等性调整重试次数和间隔
4. **负载均衡策略**：可以配置不同的负载均衡算法

## 下一步

- 配置认证授权（JWT/OAuth2）
- 添加限流功能
- 配置请求日志持久化
- 集成分布式追踪（Sleuth/Zipkin）
