# OpenFeign 和 Resilience4j 集成说明

## 实现内容

### 1. 依赖配置
在 `enrollment-service/pom.xml` 中添加了以下依赖：
- `spring-cloud-starter-openfeign`：OpenFeign 声明式 REST 客户端
- `resilience4j-spring-boot3`：Resilience4j 熔断器核心库
- `spring-cloud-starter-circuitbreaker-resilience4j`：Spring Cloud 熔断器集成

### 2. Feign Client 接口

#### UserClient
- **接口路径**：`com.zjsu.ybz.course.client.UserClient`
- **目标服务**：user-service
- **接口方法**：
  - `getStudentByStudentId(String studentId)`：根据学生ID获取学生信息
- **Fallback 实现**：`UserClientFallback`
  - 当 user-service 不可用时返回降级响应

#### CatalogClient
- **接口路径**：`com.zjsu.ybz.course.client.CatalogClient`
- **目标服务**：catalog-service
- **接口方法**：
  - `getCourseById(String courseId)`：根据课程ID获取课程信息
  - `updateCourseEnrolledCount(String courseId, Map<String, Integer> request)`：更新课程已选人数
- **Fallback 实现**：`CatalogClientFallback`
  - 当 catalog-service 不可用时返回降级响应

### 3. Resilience4j 熔断器配置

在 `application.yml` 中配置的熔断器参数：

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowType: COUNT_BASED           # 基于计数的滑动窗口
        slidingWindowSize: 10                    # 滑动窗口大小：10次调用
        minimumNumberOfCalls: 5                  # 最小调用次数：5次
        failureRateThreshold: 50                 # 失败率阈值：50%
        waitDurationInOpenState: 10000           # 熔断器打开后等待时间：10秒
        permittedNumberOfCallsInHalfOpenState: 3 # 半开状态允许的调用次数：3次
        automaticTransitionFromOpenToHalfOpenEnabled: true
```

**工作原理**：
- 当滑动窗口内的调用次数达到 5 次后，开始计算失败率
- 如果失败率达到或超过 50%，熔断器打开
- 熔断器打开后，等待 10 秒进入半开状态
- 半开状态下允许 3 次探测调用
- 如果探测成功，熔断器关闭；如果失败，重新打开

### 4. OpenFeign 配置

```yaml
spring:
  cloud:
    openfeign:
      circuitbreaker:
        enabled: true                           # 启用熔断器
      client:
        config:
          default:
            connectTimeout: 5000                # 连接超时：5秒
            readTimeout: 5000                   # 读取超时：5秒
            loggerLevel: basic                  # 日志级别：basic
```

### 5. 服务改造

#### EnrollmentService 改造
- 移除了 `RestTemplate` 依赖
- 注入 `UserClient` 和 `CatalogClient`
- 使用 Feign Client 替换原有的 RestTemplate 调用
- 增加了降级响应的检测逻辑

#### 启动类改造
- 添加 `@EnableFeignClients` 注解启用 Feign 客户端扫描
- 移除了 `RestTemplateConfig` 配置类

### 6. 监控端点

新增了以下监控端点（通过 Actuator）：
- `/actuator/circuitbreakers`：查看所有熔断器状态
- `/actuator/circuitbreakerevents`：查看熔断器事件

## 测试建议

### 1. 正常场景测试
启动所有服务，测试正常的选课和退课功能。

### 2. 降级测试
**方法 1：关闭目标服务**
```bash
# 关闭 user-service
docker stop user-service
# 测试选课接口，应该触发 UserClient 降级
curl -X POST http://localhost:8102/api/enrollments \
  -H "Content-Type: application/json" \
  -d '{"courseId":"CS101","studentId":"20230001"}'
```

**方法 2：模拟服务超时**
在 user-service 或 catalog-service 中添加延迟：
```java
Thread.sleep(6000); // 超过 readTimeout 5秒
```

### 3. 熔断器测试
连续发送多个请求使失败率达到 50%，观察熔断器打开：
```bash
# 发送 10 次请求，观察熔断器状态
for i in {1..10}; do
  curl -X POST http://localhost:8102/api/enrollments \
    -H "Content-Type: application/json" \
    -d "{\"courseId\":\"CS101\",\"studentId\":\"2023000$i\"}"
done

# 查看熔断器状态
curl http://localhost:8102/actuator/circuitbreakers
```

### 4. 监控日志
观察日志中的以下信息：
- `UserClient fallback triggered`：用户服务降级
- `CatalogClient fallback triggered`：课程目录服务降级
- 熔断器状态变化日志

## 优势

1. **声明式调用**：使用注解方式定义接口，代码更简洁
2. **服务降级**：当依赖服务不可用时，提供降级响应，提高系统可用性
3. **熔断保护**：快速失败，避免级联故障
4. **负载均衡**：与 Spring Cloud LoadBalancer 集成，自动进行负载均衡
5. **可监控性**：通过 Actuator 端点实时查看熔断器状态

## 注意事项

1. Fallback 方法应该提供有意义的降级响应，而不是简单地返回错误
2. 熔断器参数需要根据实际业务场景调整
3. 在生产环境中，建议将降级的重要操作（如更新课程人数）放入消息队列进行异步重试
4. 定期监控熔断器状态和事件，及时发现系统问题
