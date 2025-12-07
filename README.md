# 课程选课系统 (Course Selection System)

## 📌 1. 项目简介

**项目名称：** 课程选课系统微服务版
**版本：** v1.2.0 (微服务架构)
**基于版本：** v1.0 单体应用
**拆分日期：** 2025年11月

### 微服务架构说明

本项目将原单体应用按照业务边界拆分为三个独立的微服务：

1. **user-service（用户服务）**

   - 负责学生/用户信息管理
   - 提供学生的 CRUD 操作
   - 端口：8100
2. **catalog-service（课程目录服务）**

   - 负责课程信息管理
   - 提供课程的 CRUD 操作
   - 管理课程容量和选课人数
   - 端口：8101
3. **enrollment-service（选课服务）**

   - 负责选课业务逻辑
   - 通过 HTTP 调用 user-service 验证学生信息
   - 通过 HTTP 调用 catalog-service 验证课程信息和更新选课人数
   - 端口：8102

---

## 🏗️ 2. 架构图

```
客户端 (Client)
    ↓
    ├─→ user-service (8100) → user_db (MySQL:33061)
    │   └── 学生/用户管理
    │       ├── 创建学生
    │       ├── 查询学生
    │       ├── 更新学生
    │       └── 删除学生
    │
    ├─→ catalog-service (8101) → catalog_db (MySQL:33070)
    │   └── 课程管理
    │       ├── 创建课程
    │       ├── 查询课程
    │       ├── 更新课程
    │       └── 删除课程
    │
    └─→ enrollment-service (8102) → enrollment_db (MySQL:33080)
        ├── 选课管理
        │   ├── 学生选课
        │   ├── 退课
        │   └── 查询选课记录
        │
        ├── HTTP调用 → user-service（验证学生存在性）
        └── HTTP调用 → catalog-service（验证课程存在性和容量）
```

### 服务间通信

- enrollment-service 通过 **RestTemplate** 调用其他服务的 API
- 选课时验证学生是否存在（调用 user-service）
- 选课时验证课程是否存在（调用 catalog-service）
- 检查课程容量是否已满
- 更新课程的已选人数

---

## 🛠️ 3. 技术栈

| 技术           | 版本   | 用途             |
| -------------- | ------ | ---------------- |
| Spring Boot    | 3.5.6  | 微服务框架       |
| Java           | 21     | 开发语言         |
| Maven          | 3.8.7  | 项目构建工具     |
| MySQL          | 8.0    | 数据库           |
| Hibernate/JPA  | 6.6.29 | ORM 框架         |
| Docker         | 20.10+ | 容器化           |
| Docker Compose | 2.0+   | 容器编排         |
| RestTemplate   | -      | 服务间 HTTP 通信 |

---

## 💻 4. 环境要求

### 开发环境

- **JDK:** 21 或更高版本
- **Maven:** 3.8+
- **Docker:** 20.10+
- **Docker Compose:** 2.0+
- **操作系统:** Windows 10/11 + WSL2 或 Linux 或 macOS

### 验证环境

```bash
# 检查 Java 版本
java -version

# 检查 Maven 版本
mvn -version

# 检查 Docker 版本
docker --version

# 检查 Docker Compose 版本
docker-compose --version
```

---

## 🚀 5. 构建和运行步骤

### 方式一：使用 Docker Compose（推荐）

#### 步骤 1：克隆项目

```bash
git clone https://github.com/Fantasy132/CoursesSelectionSystem.git
cd CoursesSelectionSystem
```

#### 步骤 2：构建 JAR 包

```bash
# 构建 user-service
cd user-service
mvn clean package -DskipTests
cd ..

# 构建 catalog-service
cd catalog-service
mvn clean package -DskipTests
cd ..

# 构建 enrollment-service
cd enrollment-service
mvn clean package -DskipTests
cd ..
```

#### 步骤 3：启动所有服务

```bash
docker-compose up -d --build
```

#### 步骤 4：查看服务状态

```bash
docker-compose ps
```

期望输出：

```
NAME                 COMMAND                  SERVICE              STATUS              PORTS
catalog-db           "docker-entrypoint.s…"   catalog-db           running (healthy)   0.0.0.0:33070->3306/tcp
catalog-service      "java -jar app.jar"      catalog-service      running             0.0.0.0:8101->8101/tcp
enrollment-db        "docker-entrypoint.s…"   enrollment-db        running (healthy)   0.0.0.0:33080->3306/tcp
enrollment-service   "java -jar app.jar"      enrollment-service   running             0.0.0.0:8102->8102/tcp
user-db              "docker-entrypoint.s…"   user-db              running (healthy)   0.0.0.0:33061->3306/tcp
user-service         "java -jar app.jar"      user-service         running             0.0.0.0:8100->8100/tcp
```

#### 步骤 5：查看日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f user-service
docker-compose logs -f catalog-service
docker-compose logs -f enrollment-service
```

#### 步骤 6：停止服务

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷（清空数据库）
docker-compose down -v
```

### 方式二：本地开发运行

#### 前置条件

1. 安装并启动 MySQL 8.0
2. 创建数据库：

```sql
-- 创建用户数据库
CREATE DATABASE IF NOT EXISTS user_db;
CREATE USER IF NOT EXISTS 'user_user'@'localhost' IDENTIFIED BY 'user_pass';
GRANT ALL PRIVILEGES ON user_db.* TO 'user_user'@'localhost';

-- 创建课程目录数据库
CREATE DATABASE IF NOT EXISTS catalog_db;
CREATE USER IF NOT EXISTS 'catalog_user'@'localhost' IDENTIFIED BY 'catalog_pass';
GRANT ALL PRIVILEGES ON catalog_db.* TO 'catalog_user'@'localhost';

-- 创建选课数据库
CREATE DATABASE IF NOT EXISTS enrollment_db;
CREATE USER IF NOT EXISTS 'enrollment_user'@'localhost' IDENTIFIED BY 'enrollment_pass';
GRANT ALL PRIVILEGES ON enrollment_db.* TO 'enrollment_user'@'localhost';

FLUSH PRIVILEGES;
```

#### 启动服务

```bash
# 终端 1：启动 user-service
cd user-service
mvn spring-boot:run

# 终端 2：启动 catalog-service
cd catalog-service
mvn spring-boot:run

# 终端 3：启动 enrollment-service
cd enrollment-service
mvn spring-boot:run
```

---

## 🌐 5.5 Nacos 服务注册与发现

本项目使用 Nacos 作为服务注册中心和配置中心，实现微服务间的服务发现和负载均衡。

### Nacos 架构说明

```
                    Nacos Server (8848/8849)
                           ↓
        ┌──────────────────┼──────────────────┐
        ↓                  ↓                  ↓
  user-service     catalog-service    enrollment-service
   (注册)              (注册)              (注册+调用)
        ↓                  ↓                  ↓
    服务发现           服务发现         通过服务名调用其他服务
```

### Nacos 控制台访问

**访问地址：**

- 地址：http://localhost:8849/

**默认账号密码：**

- 用户名：`nacos`
- 密码：`nacos`

### 查看服务注册情况

1. 登录 Nacos 控制台
2. 点击左侧菜单 **"服务管理" → "服务列表"**
3. 选择命名空间：`public`（默认）或 `dev`（如已创建）
4. 输入分组名称：`COURSEHUB_GROUP`
5. 点击**查询**

你应该看到以下三个服务：

| 服务名称           | IP地址     | 端口 | 健康状态 |
| ------------------ | ---------- | ---- | -------- |
| user-service       | 172.18.0.x | 8100 | ✓ 健康  |
| catalog-service    | 172.18.0.x | 8101 | ✓ 健康  |
| enrollment-service | 172.18.0.x | 8102 | ✓ 健康  |

### 创建 dev 命名空间（可选）

如需使用 `dev` 命名空间（与配置文件一致）：

1. 在 Nacos 控制台点击左侧 **"命名空间"** 菜单
2. 点击右上角 **"新建命名空间"** 按钮
3. 填写信息：
   - **命名空间ID**: `dev`
   - **命名空间名**: `开发环境`
4. 点击**确定**
5. 重启所有微服务以重新注册到 dev 命名空间：
   ```bash
   docker-compose restart user-service catalog-service enrollment-service
   ```

### Nacos 配置说明

每个服务的 `application.yml` 中已配置 Nacos：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: nacos:8848          # Nacos 服务器地址
        namespace: dev                    # 命名空间
        group: COURSEHUB_GROUP           # 服务分组
        ephemeral: true                   # 临时实例
        heart-beat-interval: 5000         # 心跳间隔（毫秒）
        heart-beat-timeout: 15000         # 心跳超时（毫秒）
        metadata:
          version: 1.0.0                  # 服务版本
          service-type: xxx-management    # 服务类型
```

### 服务间调用

enrollment-service 通过 `@LoadBalanced` 的 RestTemplate 调用其他服务：

```java
// 使用服务名而非 IP 地址
String url = "http://user-service/api/students/studentId/" + studentId;
restTemplate.getForObject(url, Map.class);
```

### 测试 Nacos 功能

运行测试脚本验证 Nacos 集成：

```bash
# Linux/WSL
bash test-nacos-services.sh

# 或使用 PowerShell
.\test-nacos.ps1
```

测试内容包括：

- ✓ 服务健康检查
- ✓ Nacos 注册状态
- ✓ 服务发现功能
- ✓ 容器间通过服务名通信

### Nacos 端口说明

| 端口 | 用途                  | 访问方式              |
| ---- | --------------------- | --------------------- |
| 8848 | Nacos Server API      | http://localhost:8848 |
| 9848 | gRPC 端口（服务注册） | 内部使用              |
| 8849 | Nacos Web 控制台      | http://localhost:8849 |

---

## 📚 6. API 文档

### 基础 URL

- **User Service:** `http://localhost:8100`
- **Catalog Service:** `http://localhost:8101`
- **Enrollment Service:** `http://localhost:8102`

### 6.1 用户服务 API (user-service)

#### 学生管理

| 方法   | 路径                                    | 说明             | 请求体       | 响应码 |
| ------ | --------------------------------------- | ---------------- | ------------ | ------ |
| GET    | `/api/students`                       | 获取所有学生     | -            | 200    |
| GET    | `/api/students/{id}`                  | 根据ID获取学生   | -            | 200    |
| GET    | `/api/students/studentId/{studentId}` | 根据学号获取学生 | -            | 200    |
| POST   | `/api/students`                       | 创建学生         | Student JSON | 201    |
| PUT    | `/api/students/{id}`                  | 更新学生信息     | Student JSON | 200    |
| DELETE | `/api/students/{id}`                  | 删除学生         | -            | 200    |

**创建学生请求示例：**

```json
POST /api/students
Content-Type: application/json

{
  "studentId": "2024001",
  "name": "张三",
  "major": "计算机科学与技术",
  "grade": 2024,
  "email": "zhangsan@example.edu.cn"
}
```

**响应示例：**

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "studentId": "2024001",
    "name": "张三",
    "major": "计算机科学与技术",
    "grade": 2024,
    "email": "zhangsan@example.edu.cn",
    "createdAt": "2025-12-03T10:30:00"
  }
}
```

### 6.2 课程目录服务 API (catalog-service)

#### 课程管理

| 方法   | 路径                                  | 说明             | 请求体        | 响应码 |
| ------ | ------------------------------------- | ---------------- | ------------- | ------ |
| GET    | `/api/courses`                      | 获取所有课程     | -             | 200    |
| GET    | `/api/courses/{id}`                 | 根据ID获取课程   | -             | 200    |
| GET    | `/api/courses/code/{code}`          | 根据课程代码获取 | -             | 200    |
| POST   | `/api/courses`                      | 创建课程         | Course JSON   | 201    |
| PUT    | `/api/courses/{id}`                 | 更新课程信息     | Course JSON   | 200    |
| PUT    | `/api/courses/{id}/update-enrolled` | 更新已选人数     | enrolled: int | 200    |
| DELETE | `/api/courses/{id}`                 | 删除课程         | -             | 200    |

**创建课程请求示例：**

```json
POST /api/courses
Content-Type: application/json

{
  "code": "CS101",
  "title": "计算机科学导论",
  "instructor": {
    "name": "张教授",
    "email": "zhang@example.edu.cn",
    "department": "计算机学院"
  },
  "scheduleSlot": {
    "dayOfWeek": "MONDAY",
    "startTime": "08:00",
    "endTime": "10:00"
  },
  "capacity": 60,
  "enrolled": 0
}
```

**响应示例：**

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "id": "650e8400-e29b-41d4-a716-446655440001",
    "code": "CS101",
    "title": "计算机科学导论",
    "instructor": {
      "id": null,
      "name": "张教授",
      "email": "zhang@example.edu.cn"
    },
    "schedule": null,
    "capacity": 60,
    "enrolled": 0,
    "createdAt": "2025-12-03T10:35:00"
  }
}
```

### 6.3 选课服务 API (enrollment-service)

#### 选课管理

| 方法   | 路径                                     | 说明               | 请求体          | 响应码 |
| ------ | ---------------------------------------- | ------------------ | --------------- | ------ |
| GET    | `/api/enrollments`                     | 获取所有选课记录   | -               | 200    |
| GET    | `/api/enrollments/student/{studentId}` | 获取学生的选课记录 | -               | 200    |
| GET    | `/api/enrollments/course/{courseId}`   | 获取课程的选课记录 | -               | 200    |
| POST   | `/api/enrollments`                     | 学生选课           | Enrollment JSON | 201    |
| DELETE | `/api/enrollments/{id}`                | 退课               | -               | 200    |

**学生选课请求示例：**

```json
POST /api/enrollments
Content-Type: application/json

{
  "courseId": "650e8400-e29b-41d4-a716-446655440001",
  "studentId": "2024001"
}
```

**响应示例：**

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "id": "750e8400-e29b-41d4-a716-446655440002",
    "courseId": "650e8400-e29b-41d4-a716-446655440001",
    "studentId": "2024001",
    "status": "ACTIVE",
    "enrolledAt": "2025-12-03T10:40:00"
  }
}
```

### 错误响应格式

所有服务的错误响应格式统一：

```json
{
  "code": 404,
  "message": "Student not found with id: 9999999",
  "data": null
}
```

常见错误码：

- `400` - 请求参数错误、业务逻辑错误（如重复选课、课程已满）
- `404` - 资源不存在
- `500` - 服务器内部错误

---

## 🧪 7. 测试说明

### 自动化测试脚本

项目提供了完整的自动化测试脚本 `test-services.sh`，测试所有功能和服务间通信。

#### 运行测试

```bash
# 在 WSL 或 Linux/Mac 中运行
bash test-services.sh

# 或者在 Windows PowerShell 中运行
wsl bash test-services.sh
```

#### 测试覆盖内容

测试脚本会自动执行以下测试：

1. ✅ **用户服务测试**

   - 创建学生
   - 获取所有学生
   - 按学号查询学生
   - 更新和删除学生
2. ✅ **课程目录服务测试**

   - 创建课程
   - 获取所有课程
   - 按课程代码查询
   - 更新课程信息
3. ✅ **选课服务测试**

   - 学生选课（验证服务间通信）
   - 查询选课记录
   - 按学生/课程查询选课记录
   - 退课功能
4. ✅ **业务逻辑验证**

   - 学生不存在时选课失败（404）
   - 课程不存在时选课失败（404）
   - 重复选课检测（400）
   - 课程容量统计正确性
   - 课程已选人数自动更新
5. ✅ **服务间通信测试**

   - enrollment-service → user-service
   - enrollment-service → catalog-service

#### 预期输出

测试成功时会显示：

```
=== 测试完成 ===
测试覆盖内容：
  ✓ 用户服务 CRUD 操作
  ✓ 课程目录服务 CRUD 操作
  ✓ 选课服务业务逻辑
  ✓ 服务间通信（enrollment -> user, catalog）
  ✓ 学生不存在错误处理
  ✓ 课程不存在错误处理
  ✓ 重复选课检查
  ✓ 课程容量统计
  ✓ 按学号/课程代码查询
```

### 手动测试

#### 使用 curl 测试

```bash
# 1. 创建学生
curl -X POST http://localhost:8100/api/students \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "2024001",
    "name": "张三",
    "major": "计算机科学与技术",
    "grade": 2024,
    "email": "zhangsan@example.edu.cn"
  }'

# 2. 创建课程
curl -X POST http://localhost:8101/api/courses \
  -H "Content-Type: application/json" \
  -d '{
    "code": "CS101",
    "title": "计算机科学导论",
    "instructor": {
      "name": "张教授",
      "email": "zhang@example.edu.cn",
      "department": "计算机学院"
    },
    "scheduleSlot": {
      "dayOfWeek": "MONDAY",
      "startTime": "08:00",
      "endTime": "10:00"
    },
    "capacity": 60,
    "enrolled": 0
  }'

# 3. 学生选课（需要替换实际的 courseId）
curl -X POST http://localhost:8102/api/enrollments \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": "你的课程ID",
    "studentId": "2024001"
  }'
```

#### 使用 Postman 测试

1. 导入 Postman Collection（如有提供）
2. 设置环境变量：
   - `user_service_url`: `http://localhost:8100`
   - `catalog_service_url`: `http://localhost:8101`
   - `enrollment_service_url`: `http://localhost:8102`
3. 按照 API 文档顺序测试各个接口

### 清空测试数据

如需重新开始测试，清空所有数据：

```bash
docker-compose down -v
docker-compose up -d --build
```

---

## 📝 项目结构

```
CSS_microservices/
├── user-service/              # 用户服务
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/zjsu/ybz/course/
│   │       │       ├── controller/    # 控制器
│   │       │       ├── service/       # 业务逻辑
│   │       │       ├── repository/    # 数据访问
│   │       │       ├── model/         # 实体类
│   │       │       └── config/        # 配置类
│   │       └── resources/
│   │           └── application.yml    # 配置文件
│   ├── Dockerfile
│   ├── pom.xml
│   └── mysql.sql
│
├── catalog-service/           # 课程目录服务
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/zjsu/ybz/course/
│   │       │       ├── controller/
│   │       │       ├── service/
│   │       │       ├── repository/
│   │       │       └── model/
│   │       └── resources/
│   │           └── application.yml
│   ├── Dockerfile
│   ├── pom.xml
│   └── mysql.sql
│
├── enrollment-service/        # 选课服务
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/zjsu/ybz/course/
│   │       │       ├── controller/
│   │       │       ├── service/
│   │       │       ├── repository/
│   │       │       ├── model/
│   │       │       ├── config/
│   │       │       └── exception/     # 异常处理
│   │       └── resources/
│   │           └── application.yml
│   ├── Dockerfile
│   ├── pom.xml
│   └── mysql.sql
│
├── docker-compose.yml         # Docker Compose 配置
├── test-services.sh          # 自动化测试脚本
└── README.md                 # 项目文档
```

---

## 🔧 常见问题

### Q1: 端口被占用怎么办？

修改 `docker-compose.yml` 中的端口映射：

```yaml
services:
  user-service:
    ports:
      - "8200:8100"  # 将本地端口改为 8200
```

### Q2: 数据库连接失败？

检查 MySQL 容器是否正常运行：

```bash
docker-compose ps
docker-compose logs user-db
```

### Q3: 服务启动失败？

查看服务日志：

```bash
docker-compose logs -f user-service
```

常见原因：

- JAR 包未构建或路径错误
- 数据库未就绪
- 端口冲突

### Q4: 如何重新构建某个服务？

```bash
# 重新构建并启动特定服务
docker-compose up -d --build user-service
```

---

## 📄 许可证

本项目仅用于学习和研究目的。

---

## 👥 贡献者

- **开发者**: Fantasy132
- **GitHub**: https://github.com/Fantasy132/CoursesSelectionSystem

---

## 📞 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 Issue: https://github.com/Fantasy132/CoursesSelectionSystem/issues
- Pull Request: https://github.com/Fantasy132/CoursesSelectionSystem/pulls

---

**最后更新**: 2025年12月3日
**版本**: v1.2.0

|------|------|------|--------|
| GET | `/api/courses` | 查询所有课程 | 200 |
| GET | `/api/courses/{id}` | 查询单个课程 | 200, 404 |
| POST | `/api/courses` | 创建课程 | 201, 400 |
| PUT | `/api/courses/{id}` | 更新课程 | 200, 404 |
| DELETE | `/api/courses/{id}` | 删除课程 | 204, 404 |

#### 创建课程示例

```bash
POST http://localhost:8080/api/courses
Content-Type: application/json

{
  "code": "CS101",
  "title": "计算机科学导论",
  "instructor": {
    "id": "T001",
    "name": "张教授",
    "email": "zhang@example.edu.cn"
  },
  "schedule": {
    "dayOfWeek": "MONDAY",
    "startTime": "08:00",
    "endTime": "10:00",
    "expectedAttendance": 50
  },
  "capacity": 60
}
```

### 二、学生管理 API

| 方法   | 路径                   | 说明         | 状态码        |
| ------ | ---------------------- | ------------ | ------------- |
| GET    | `/api/students`      | 查询所有学生 | 200           |
| GET    | `/api/students/{id}` | 查询单个学生 | 200, 404      |
| POST   | `/api/students`      | 创建学生     | 201, 400      |
| PUT    | `/api/students/{id}` | 更新学生信息 | 200, 404      |
| DELETE | `/api/students/{id}` | 删除学生     | 204, 400, 404 |

#### 创建学生示例

```bash
POST http://localhost:8080/api/students
Content-Type: application/json

{
  "studentId": "2024001",
  "name": "张三",
  "major": "计算机科学与技术",
  "grade": 2024,
  "email": "zhangsan@example.edu.cn"
}
```

**注意事项**：

- `id` 字段由系统自动生成（UUID），不需要在请求中提供
- `studentId`（学号）必须全局唯一
- `email` 必须符合标准邮箱格式
- `createdAt` 由系统自动生成
- 删除学生前必须确保该学生没有选课记录

### 三、选课管理 API

| 方法   | 路径                                     | 说明             | 状态码        |
| ------ | ---------------------------------------- | ---------------- | ------------- |
| GET    | `/api/enrollments`                     | 查询所有选课记录 | 200           |
| GET    | `/api/enrollments/course/{courseId}`   | 按课程查询       | 200           |
| GET    | `/api/enrollments/student/{studentId}` | 按学生查询       | 200           |
| POST   | `/api/enrollments`                     | 学生选课         | 201, 400, 404 |
| DELETE | `/api/enrollments/{id}`                | 学生退课         | 204, 404      |

#### 学生选课示例

```bash
POST http://localhost:8080/api/enrollments
Content-Type: application/json

{
  "courseId": "课程ID",
  "studentId": "学生ID"
}
```

**业务规则**：

- 课程选课人数不能超过容量（capacity）
- 同一学生不能重复选择同一门课程
- 选课时必须验证课程和学生是否存在
- 选课成功后，课程的 `enrolled` 字段自动增加

## 📁 项目结构

```
src/
├── main/
│   ├── java/com/zjsu/ybz/course/
│   │   ├── CourseApplication.java          # 启动类
│   │   ├── model/                          # 实体类
│   │   │   ├── Course.java
│   │   │   ├── Instructor.java
│   │   │   ├── ScheduleSlot.java
│   │   │   ├── Student.java
│   │   │   └── Enrollment.java
│   │   ├── repository/                     # 数据访问层
│   │   │   ├── CourseRepository.java
│   │   │   ├── StudentRepository.java
│   │   │   └── EnrollmentRepository.java
│   │   ├── service/                        # 业务逻辑层
│   │   │   ├── CourseService.java
│   │   │   ├── StudentService.java
│   │   │   └── EnrollmentService.java
│   │   └── controller/                     # 控制器层
│   │       ├── CourseController.java
│   │       ├── StudentController.java
│   │       └── EnrollmentController.java
│   │       └── ApiResponse.java
│   │       └── GlobalExceptionHandler.java
│   └── resources/
│       └── application.yml                 # 配置文件
└── test/                                   # 测试代码
```

## 🧪 测试说明

### 导入 Postman Collection

1. 在项目根目录找到 `API_test.postman_collection.json`
2. 打开 Postman，点击 **Import** 按钮
3. 选择该 JSON 文件导入
4. 创建环境变量：
   - 变量名：`base_url`
   - 值：`http://localhost:8080`
5. 激活环境，开始测试

### 测试场景

项目包含以下测试场景（详见 Postman Collection）：

#### 场景 1：完整的课程管理流程

1. 创建 3 门不同的课程
2. 查询所有课程，验证返回 3 条记录
3. 根据 ID 查询某门课程
4. 更新该课程的信息
5. 删除该课程
6. 再次查询，验证返回 404

#### 场景 2：选课业务流程

1. 创建一门容量为 2 的课程
2. 学生 S001 选课，验证成功
3. 学生 S002 选课，验证成功
4. 学生 S003 选课，验证失败（容量已满）
5. 学生 S001 再次选课，验证失败（重复选课）
6. 查询课程，验证 enrolled 字段为 2

#### 场景 3：学生管理流程

1. 创建 3 个不同学号的学生
2. 查询所有学生，验证返回 3 条记录
3. 根据 ID 查询某个学生
4. 更新该学生的专业和邮箱信息
5. 尝试让一个不存在的学生选课，验证返回 404
6. 让学生选课后，尝试删除该学生，验证返回错误
7. 删除没有选课记录的学生，验证删除成功

#### 场景 4：错误处理

1. 查询不存在的课程 ID，验证返回 404
2. 创建课程时缺少必填字段，验证返回 400
3. 选课时提供不存在的课程 ID，验证返回 404
4. 创建学生时使用重复的 studentId，验证返回错误
5. 创建学生时使用无效的邮箱格式，验证返回错误

### 运行全部测试

在 Postman 中：

1. 右键点击 Collection
2. 选择 **Run collection**
3. 点击 **Run** 按钮
4. 查看测试结果统计

### 关键测试截图

为作业提交准备以下截图：

1. 应用启动成功的终端截图
2. 创建课程成功（包含请求和响应）
3. 查询所有课程列表
4. 选课容量限制测试（显示 400 错误）
5. 重复选课验证测试
6. Collection Runner 的测试结果总览

## 📦 统一响应格式

### 成功响应

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    // 实际数据
  }
}
```

### 错误响应

```json
{
  "code": 404,
  "message": "Course not found",
  "data": null
}
```

### HTTP 状态码

| 状态码 | 说明                  | 使用场景                   |
| ------ | --------------------- | -------------------------- |
| 200    | OK                    | 查询、更新成功             |
| 201    | Created               | 创建成功                   |
| 204    | No Content            | 删除成功                   |
| 400    | Bad Request           | 请求参数错误、业务规则违反 |
| 404    | Not Found             | 资源不存在                 |
| 500    | Internal Server Error | 服务器内部错误             |

## 🔧 配置说明

### application.yml

```yaml
server:
  port: 8080  # 服务端口

spring:
  application:
    name: course-management-system  # 应用名称
```

### Maven 依赖

```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
  
    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
</dependencies>
```

## 🎯 业务规则实现

### 1. 课程容量限制

- 课程有 `capacity`（容量）和 `enrolled`（已选人数）字段
- 选课时检查：`enrolled < capacity`
- 不满足则返回 400 错误："Course is full"

### 2. 重复选课检查

- 选课前查询该学生是否已选择该课程
- 如已存在记录，返回 400 错误："Student already enrolled in this course"

### 3. 课程和学生存在性验证

- 选课前验证课程是否存在，不存在返回 404："Course not found"
- 选课前验证学生是否存在，不存在返回 404："Student not found"

### 4. 学生删除限制

- 删除学生前检查是否有选课记录
- 如有记录，返回 400 错误："无法删除：该学生存在选课记录"

### 5. 级联更新

- 学生选课成功后，自动将课程的 `enrolled` 字段 +1
- 学生退课后，自动将课程的 `enrolled` 字段 -1

### 6. 数据验证

- **学号唯一性**：创建学生时检查 studentId 是否已存在
- **邮箱格式**：使用正则表达式验证邮箱格式
- **必填字段**：使用 `@NotNull`、`@NotBlank` 等注解验证

## 📝 开发规范

### 代码规范

- 类名使用大驼峰命名法（PascalCase）
- 方法名和变量名使用小驼峰命名法（camelCase）
- 常量使用全大写加下划线（UPPER_SNAKE_CASE）
- 包名使用全小写，必须包含姓名缩写

### 分层架构

```
Controller -> Service -> Repository
```

- **Controller**：处理 HTTP 请求，参数验证，返回响应
- **Service**：业务逻辑处理，事务管理
- **Repository**：数据访问，CRUD 操作

### RESTful 设计原则

- 使用名词复数形式：`/api/courses`、`/api/students`
- 使用正确的 HTTP 方法：GET（查询）、POST（创建）、PUT（更新）、DELETE（删除）
- 返回合适的状态码
- URL 中使用 ID 标识资源：`/api/courses/{id}`

## 🐛 常见问题

### 1. 端口被占用

**错误信息**：

```
Web server failed to start. Port 8080 was already in use.
```

**解决方法**：

- 修改 `application.yml` 中的端口号
- 或关闭占用 8080 端口的程序

### 2. Maven 依赖下载失败

**解决方法**：

```bash
# 清除本地缓存
mvn clean

# 强制更新依赖
mvn clean install -U
```

### 3. 找不到主类

**解决方法**：

- 检查包名是否包含姓名缩写
- 确认主类上有 `@SpringBootApplication` 注解
- IDE 中刷新项目（Reload Project）

### 4. 请求 404 错误（所有接口）

**原因**：

- Controller 类上缺少 `@RestController` 注解
- Controller 类上缺少 `@RequestMapping` 注解
- 方法上的 URL 映射不正确

### 5. JSON 格式错误

**解决方法**：

- 检查请求 Header 中是否设置 `Content-Type: application/json`
- 验证 JSON 格式是否正确（可使用 JSON 在线验证工具）

## 📞 联系方式

- **学生姓名**：於泊臻
- **学号**：2335020234
- **GitHub**：https://github.com/Fantasy132/CoursesSelectionSystem
- **邮箱**：1321765450@qq.com

## 📄 License

本项目仅用于学习目的。

## 🙏 致谢

感谢老师的指导和帮助！

---

**最后更新时间**：2025年10月24日
