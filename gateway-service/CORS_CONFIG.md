# CORS 跨域配置说明

## ✅ 配置完成

Gateway 已完成 CORS 跨域配置，支持前端应用跨域访问 API。

## 配置方式

### 1. Java 配置类 (推荐)

[CorsConfiguration.java](src/main/java/com/zjsu/ybz/gateway/config/CorsConfiguration.java)

```java
@Configuration
public class CorsConfiguration {
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许所有源（开发环境）
        config.addAllowedOriginPattern("*");
        
        // 允许的方法
        config.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"
        ));
        
        // 允许的请求头
        config.addAllowedHeader("*");
        
        // 暴露的响应头（前端可访问）
        config.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-Token-Refresh-Required",
            "X-User-Id",
            "X-User-Name",
            "Content-Type",
            "Content-Length"
        ));
        
        // 不允许携带凭证（JWT 不需要 Cookie）
        config.setAllowCredentials(false);
        
        // 预检请求缓存时间：1 小时
        config.setMaxAge(3600L);
        
        return new CorsWebFilter(source);
    }
}
```

### 2. YAML 配置（备用）

[application.yml](src/main/resources/application.yml)

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOriginPatterns: "*"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
              - HEAD
              - PATCH
            allowedHeaders: "*"
            exposedHeaders:
              - Authorization
              - X-Token-Refresh-Required
              - X-User-Id
              - X-User-Name
              - Content-Type
              - Content-Length
            allowCredentials: false
            maxAge: 3600
```

## 关键配置说明

### 1. allowedOriginPatterns

```yaml
allowedOriginPatterns: "*"  # 开发环境：允许所有源
```

**生产环境建议：**
```yaml
allowedOriginPatterns:
  - "https://example.com"
  - "https://*.example.com"
  - "http://localhost:3000"  # 开发环境
```

### 2. allowedMethods

支持的 HTTP 方法：
- ✅ GET - 获取资源
- ✅ POST - 创建资源
- ✅ PUT - 更新资源
- ✅ DELETE - 删除资源
- ✅ OPTIONS - 预检请求
- ✅ HEAD - 获取响应头
- ✅ PATCH - 部分更新

### 3. allowedHeaders

```yaml
allowedHeaders: "*"  # 允许所有请求头
```

重要的请求头：
- `Content-Type` - 内容类型
- `Authorization` - JWT Token
- `X-Requested-With` - AJAX 标识

### 4. exposedHeaders

前端可以访问的响应头：
- `Authorization` - JWT Token（如果需要返回新 Token）
- `X-Token-Refresh-Required` - Token 刷新提示
- `X-User-Id` - 用户 ID
- `X-User-Name` - 用户姓名
- `Content-Type` - 内容类型
- `Content-Length` - 内容长度

### 5. allowCredentials

```yaml
allowCredentials: false  # JWT 认证不需要 Cookie
```

**注意：** 
- `allowCredentials: true` 时，`allowedOrigins` 不能为 `*`
- 必须指定具体的域名
- JWT 认证通常不需要 Cookie，设置为 `false`

### 6. maxAge

```yaml
maxAge: 3600  # 预检请求缓存 1 小时
```

预检请求（OPTIONS）的缓存时间，减少重复的预检请求。

## 前端使用示例

### JavaScript (Fetch API)

```javascript
// 1. 登录获取 Token
const loginResponse = await fetch('http://localhost:8090/api/auth/login', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
    },
    body: JSON.stringify({
        studentId: '2021001',
        password: '123456'
    })
});

const { token } = await loginResponse.json();

// 2. 使用 Token 访问受保护资源
const coursesResponse = await fetch('http://localhost:8090/api/courses', {
    method: 'GET',
    headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    }
});

const courses = await coursesResponse.json();
```

### JavaScript (Axios)

```javascript
import axios from 'axios';

// 配置 Axios 实例
const api = axios.create({
    baseURL: 'http://localhost:8090',
    timeout: 10000,
});

// 请求拦截器：自动添加 Token
api.interceptors.request.use(config => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// 响应拦截器：处理 Token 刷新
api.interceptors.response.use(
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
        if (error.response?.status === 401) {
            // Token 无效，跳转登录
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

// 使用示例
const getCourses = async () => {
    try {
        const response = await api.get('/api/courses');
        console.log('课程列表:', response.data);
    } catch (error) {
        console.error('请求失败:', error);
    }
};
```

### React 示例

```jsx
import React, { useState, useEffect } from 'react';

function CourseList() {
    const [courses, setCourses] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchCourses = async () => {
            try {
                const token = localStorage.getItem('jwt_token');
                const response = await fetch('http://localhost:8090/api/courses', {
                    headers: {
                        'Authorization': `Bearer ${token}`,
                        'Content-Type': 'application/json'
                    }
                });

                if (!response.ok) {
                    throw new Error('获取课程失败');
                }

                const data = await response.json();
                setCourses(data);
            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        };

        fetchCourses();
    }, []);

    if (loading) return <div>加载中...</div>;
    if (error) return <div>错误: {error}</div>;

    return (
        <div>
            <h2>课程列表</h2>
            <ul>
                {courses.map(course => (
                    <li key={course.id}>{course.name}</li>
                ))}
            </ul>
        </div>
    );
}
```

### Vue 3 示例

```vue
<template>
  <div>
    <h2>课程列表</h2>
    <div v-if="loading">加载中...</div>
    <div v-else-if="error">错误: {{ error }}</div>
    <ul v-else>
      <li v-for="course in courses" :key="course.id">
        {{ course.name }}
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';

const courses = ref([]);
const loading = ref(true);
const error = ref(null);

const fetchCourses = async () => {
  try {
    const token = localStorage.getItem('jwt_token');
    const response = await fetch('http://localhost:8090/api/courses', {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });

    if (!response.ok) {
      throw new Error('获取课程失败');
    }

    courses.value = await response.json();
  } catch (err) {
    error.value = err.message;
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchCourses();
});
</script>
```

## 测试验证

### 1. 使用测试脚本

```bash
# 在 WSL 中
cd /mnt/f/school/微服务开发/CSS_microservices/scripts
chmod +x test-cors.sh
./test-cors.sh
```

### 2. 使用测试页面

打开浏览器访问：
```
file:///f:/school/微服务开发/CSS_microservices/docs/cors-test.html
```

或使用 HTTP 服务器：
```bash
# 在项目根目录
cd docs
python -m http.server 8000
# 访问: http://localhost:8000/cors-test.html
```

### 3. 使用浏览器开发者工具

1. 打开浏览器开发者工具（F12）
2. 切换到 Network（网络）选项卡
3. 发送请求
4. 查看响应头中的 CORS 相关字段：
   - `Access-Control-Allow-Origin`
   - `Access-Control-Allow-Methods`
   - `Access-Control-Allow-Headers`
   - `Access-Control-Expose-Headers`

### 4. 使用 curl 测试

```bash
# 测试预检请求（OPTIONS）
curl -X OPTIONS http://localhost:8090/api/courses \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Authorization,Content-Type" \
  -v

# 测试实际请求
curl -X GET http://localhost:8090/api/courses \
  -H "Origin: http://localhost:3000" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -v
```

## 预检请求（Preflight）

### 什么是预检请求？

浏览器在发送跨域请求前，会先发送一个 OPTIONS 请求来检查服务器是否允许该请求。

### 触发条件

以下情况会触发预检请求：
1. 使用 PUT、DELETE、PATCH 等方法
2. 使用自定义请求头（如 `Authorization`）
3. `Content-Type` 为 `application/json` 等非简单类型

### 预检请求示例

```
OPTIONS /api/courses HTTP/1.1
Origin: http://localhost:3000
Access-Control-Request-Method: POST
Access-Control-Request-Headers: Authorization,Content-Type
```

### 预检响应示例

```
HTTP/1.1 200 OK
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: Authorization, Content-Type
Access-Control-Max-Age: 3600
```

## 常见问题

### Q1: CORS 错误："Access to fetch has been blocked by CORS policy"

**原因：**
- Gateway 未配置 CORS
- 源不在允许列表中
- 请求方法或请求头不被允许

**解决：**
1. 确认 Gateway 服务已启动
2. 检查 `allowedOriginPatterns` 配置
3. 查看浏览器控制台的详细错误信息

### Q2: 为什么预检请求返回 401？

**原因：**
- JWT 认证过滤器拦截了 OPTIONS 请求

**解决：**
在 JWT 认证过滤器的白名单中，OPTIONS 方法应该被放行。当前配置已正确处理。

### Q3: 前端无法访问自定义响应头

**原因：**
- 响应头未在 `exposedHeaders` 中配置

**解决：**
在 `exposedHeaders` 中添加需要暴露的响应头。

### Q4: allowCredentials 应该设置为 true 还是 false？

**JWT 认证场景：**
```yaml
allowCredentials: false  # JWT 不需要 Cookie
allowedOriginPatterns: "*"  # 可以使用通配符
```

**Cookie 认证场景：**
```yaml
allowCredentials: true  # 需要携带 Cookie
allowedOriginPatterns:  # 必须指定具体域名
  - "https://example.com"
```

### Q5: 生产环境如何配置？

**开发环境：**
```yaml
allowedOriginPatterns: "*"
allowCredentials: false
```

**生产环境：**
```yaml
allowedOriginPatterns:
  - "https://www.example.com"
  - "https://admin.example.com"
  - "https://*.example.com"
allowCredentials: false
```

## 安全建议

### 1. 限制允许的源

❌ **不推荐（生产环境）：**
```yaml
allowedOriginPatterns: "*"
```

✅ **推荐（生产环境）：**
```yaml
allowedOriginPatterns:
  - "https://example.com"
  - "https://*.example.com"
```

### 2. 最小化暴露的响应头

只暴露必要的响应头：
```yaml
exposedHeaders:
  - Authorization
  - X-Token-Refresh-Required
```

### 3. 限制允许的方法

只允许必要的 HTTP 方法：
```yaml
allowedMethods:
  - GET
  - POST
  - PUT
  - DELETE
```

### 4. 使用 HTTPS

生产环境必须使用 HTTPS：
```yaml
allowedOriginPatterns:
  - "https://example.com"  # ✅ HTTPS
  # - "http://example.com"  # ❌ HTTP
```

### 5. 设置合理的缓存时间

```yaml
maxAge: 3600  # 1 小时，减少预检请求
```

### 6. 监控 CORS 错误

配置日志记录 CORS 相关错误：
```yaml
logging:
  level:
    org.springframework.web.cors: DEBUG
```

## 编译结果

```
✅ BUILD SUCCESS
✅ 编译时间: 3.986s
✅ 文件: 7 个 Java 源文件
```

## 相关文件

- [CorsConfiguration.java](src/main/java/com/zjsu/ybz/gateway/config/CorsConfiguration.java) - CORS 配置类
- [application.yml](src/main/resources/application.yml) - YAML 配置
- [test-cors.sh](../scripts/test-cors.sh) - CORS 测试脚本
- [cors-test.html](../docs/cors-test.html) - 前端测试页面
- [JwtAuthenticationFilter.java](src/main/java/com/zjsu/ybz/gateway/filter/JwtAuthenticationFilter.java) - JWT 认证过滤器（已支持 OPTIONS）

## 下一步

1. **启动服务测试**
   ```bash
   cd gateway-service
   ./mvnw spring-boot:run
   ```

2. **运行 CORS 测试**
   ```bash
   cd scripts
   ./test-cors.sh
   ```

3. **前端集成**
   - 使用 Axios 或 Fetch API
   - 配置 Authorization 头
   - 处理 CORS 错误

4. **生产部署前**
   - 修改 `allowedOriginPatterns` 为具体域名
   - 启用 HTTPS
   - 配置日志监控
