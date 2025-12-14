# User Service 认证 API 文档

## 概述
User Service 提供了完整的用户认证功能，包括登录、注册、Token 验证等接口。

## 认证流程

1. **注册**：用户通过 `/api/auth/register` 注册账号
2. **登录**：用户通过 `/api/auth/login` 登录，获取 JWT Token
3. **访问受保护资源**：在请求头中携带 Token：`Authorization: Bearer {token}`
4. **Token 验证**：后端通过 `/api/auth/validate` 验证 Token 有效性

## API 端点

### 1. 用户登录

**请求**
```http
POST /api/auth/login
Content-Type: application/json

{
  "studentId": "2021001",
  "password": "123456"
}
```

**成功响应 (200 OK)**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "studentId": "2021001",
  "name": "张三",
  "email": "zhangsan@example.com",
  "message": "登录成功"
}
```

**失败响应 (401 Unauthorized)**
```json
{
  "token": null,
  "studentId": null,
  "name": null,
  "email": null,
  "message": "学号或密码错误"
}
```

**通过网关访问**
```bash
curl -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"studentId":"2021001","password":"123456"}'
```

---

### 2. 用户注册

**请求**
```http
POST /api/auth/register
Content-Type: application/json

{
  "studentId": "2021004",
  "name": "赵六",
  "password": "123456",
  "email": "zhaoliu@example.com",
  "major": "软件工程",
  "grade": 2021
}
```

**成功响应 (201 Created)**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "studentId": "2021004",
  "name": "赵六",
  "email": "zhaoliu@example.com",
  "message": "登录成功"
}
```

**失败响应 (400 Bad Request)**
```json
{
  "token": null,
  "studentId": null,
  "name": null,
  "email": null,
  "message": "学号已存在"
}
```

**验证规则**
- `studentId`: 必填
- `name`: 必填
- `password`: 必填，最少 6 位
- `email`: 必填，格式必须正确
- `major`: 可选
- `grade`: 可选

**通过网关访问**
```bash
curl -X POST http://localhost:8090/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "studentId":"2021004",
    "name":"赵六",
    "password":"123456",
    "email":"zhaoliu@example.com",
    "major":"软件工程",
    "grade":2021
  }'
```

---

### 3. 验证 Token

**请求**
```http
GET /api/auth/validate
Authorization: Bearer {token}
```

**成功响应 (200 OK)**
```json
{
  "valid": true,
  "studentId": "2021001"
}
```

**失败响应 (401 Unauthorized)**
```json
{
  "valid": false,
  "message": "Token 无效或已过期"
}
```

**通过网关访问**
```bash
curl -X GET http://localhost:8090/api/auth/validate \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 4. 刷新 Token

**请求**
```http
POST /api/auth/refresh
Authorization: Bearer {token}
```

**成功响应 (200 OK)**
```json
{
  "success": true,
  "message": "请重新登录以获取新的 Token"
}
```

**失败响应 (401 Unauthorized)**
```json
{
  "success": false,
  "message": "Token 无效或已过期"
}
```

---

### 5. 登出

**请求**
```http
POST /api/auth/logout
```

**响应 (200 OK)**
```json
{
  "success": true,
  "message": "登出成功"
}
```

注：JWT Token 是无状态的，登出主要由客户端处理（删除本地存储的 Token）

---

## JWT Token 配置

- **加密算法**: HS256
- **密钥**: 256 位（在 application.yml 中配置）
- **有效期**: 24 小时（86400000 毫秒）
- **Token 包含信息**:
  - `sub`: 学号（studentId）
  - `name`: 姓名
  - `iat`: 签发时间
  - `exp`: 过期时间

## 密码安全

- 使用 BCrypt 加密算法
- 密码强度等级：10（默认）
- 存储格式：`$2a$10$...`（60 字符）
- 密码验证：自动进行盐值和哈希比对

## 测试账号

数据库中预置了以下测试账号（密码均为 `123456`）：

| 学号 | 姓名 | 邮箱 | 专业 |
|------|------|------|------|
| 2021001 | 张三 | zhangsan@example.com | 计算机科学 |
| 2021002 | 李四 | lisi@example.com | 软件工程 |
| 2021003 | 王五 | wangwu@example.com | 数据科学 |

## 使用示例

### 完整登录流程

```bash
# 1. 登录获取 Token
TOKEN=$(curl -s -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"studentId":"2021001","password":"123456"}' \
  | jq -r '.token')

echo "获取到的 Token: $TOKEN"

# 2. 使用 Token 访问受保护的接口（示例：获取学生信息）
curl -X GET http://localhost:8090/api/students/studentId/2021001 \
  -H "Authorization: Bearer $TOKEN"

# 3. 验证 Token
curl -X GET http://localhost:8090/api/auth/validate \
  -H "Authorization: Bearer $TOKEN"
```

### 注册新用户

```bash
curl -X POST http://localhost:8090/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "studentId":"2021004",
    "name":"新用户",
    "password":"mypassword123",
    "email":"newuser@example.com",
    "major":"信息安全",
    "grade":2021
  }'
```

## 错误处理

### 常见错误码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 201 | 创建成功（注册） |
| 400 | 请求参数错误 |
| 401 | 未授权（Token 无效或过期） |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 验证错误示例

**密码太短**
```json
{
  "timestamp": "2025-12-10T16:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "密码长度不能少于6位"
}
```

**邮箱格式错误**
```json
{
  "timestamp": "2025-12-10T16:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "邮箱格式不正确"
}
```

## 安全建议

1. **生产环境配置**
   - 修改 JWT 密钥为强随机字符串（至少 256 位）
   - 使用 HTTPS 传输 Token
   - 设置合理的 Token 过期时间

2. **客户端存储**
   - 使用 localStorage 或 sessionStorage 存储 Token
   - 在每次请求时添加 `Authorization` 头
   - Token 过期后自动跳转到登录页

3. **密码策略**
   - 强制要求密码复杂度
   - 限制登录失败次数
   - 定期提醒用户修改密码

## 集成示例

### JavaScript/Axios

```javascript
// 登录
const login = async (studentId, password) => {
  try {
    const response = await axios.post('http://localhost:8090/api/auth/login', {
      studentId,
      password
    });
    
    // 保存 Token
    localStorage.setItem('token', response.data.token);
    localStorage.setItem('studentId', response.data.studentId);
    
    return response.data;
  } catch (error) {
    console.error('登录失败:', error.response.data.message);
    throw error;
  }
};

// 使用 Token 访问 API
const getStudentInfo = async (studentId) => {
  const token = localStorage.getItem('token');
  
  try {
    const response = await axios.get(
      `http://localhost:8090/api/students/studentId/${studentId}`,
      {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      }
    );
    
    return response.data;
  } catch (error) {
    if (error.response.status === 401) {
      // Token 过期，跳转到登录页
      window.location.href = '/login';
    }
    throw error;
  }
};
```

### Python/Requests

```python
import requests

# 登录
def login(student_id, password):
    response = requests.post(
        'http://localhost:8090/api/auth/login',
        json={
            'studentId': student_id,
            'password': password
        }
    )
    
    if response.status_code == 200:
        data = response.json()
        return data['token']
    else:
        raise Exception(f"登录失败: {response.json()['message']}")

# 使用 Token
token = login('2021001', '123456')

response = requests.get(
    'http://localhost:8090/api/students/studentId/2021001',
    headers={'Authorization': f'Bearer {token}'}
)

print(response.json())
```
