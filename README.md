# 校园选课系统（单体版）

> 基于 Spring Boot 的校园选课与教学资源管理平台

## 📋 项目说明

本项目是一个基于 Spring Boot 的单体架构选课管理系统，实现了课程管理、学生管理和选课管理三大核心功能。系统采用 RESTful API 设计，使用内存存储数据，为后续微服务架构改造打下基础。

### 主要功能

- **课程管理**：创建、查询、更新、删除课程信息
- **学生管理**：学生信息的增删改查，支持数据验证
- **选课管理**：学生选课、退课，支持容量限制和重复检查

### 技术栈

- **框架**：Spring Boot 3.4.x
- **语言**：Java 24
- **构建工具**：Maven
- **数据存储**：ConcurrentHashMap（内存存储）
- **API设计**：RESTful
- **测试工具**：Postman

## 🚀 如何运行项目

### 前置要求

- JDK 17 或更高版本
- Maven 3.6+ 或使用项目自带的 Maven Wrapper
- IDE（推荐 IntelliJ IDEA 或 VS Code）

### 方式一：使用 Maven 命令

```bash
# 1. 克隆或下载项目到本地
git clone [你的仓库地址]
cd course-management-system

# 2. 编译项目
mvn clean compile

# 3. 运行项目
mvn spring-boot:run
```

### 方式二：使用 Maven Wrapper（推荐）

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

### 方式三：使用 IDE

1. 导入项目到 IDE（File → Open → 选择项目文件夹）
2. 等待 Maven 依赖下载完成
3. 找到主类 `CourseApplication.java`
4. 右键选择 `Run 'CourseApplication'`

### 验证运行

项目启动成功后，会在控制台看到类似输出：

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v3.4.0)

INFO: Started CourseApplication in 2.345 seconds
```

**默认访问地址**：`http://localhost:8080`

### 修改端口（可选）

如果 8080 端口被占用，修改 `src/main/resources/application.yml`：

```yaml
server:
  port: 8081  # 修改为其他端口
```

## 📚 API 接口列表

### 基础URL

```
http://localhost:8080
```

### 一、课程管理 API

| 方法 | 路径 | 说明 | 状态码 |
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

| 方法 | 路径 | 说明 | 状态码 |
|------|------|------|--------|
| GET | `/api/students` | 查询所有学生 | 200 |
| GET | `/api/students/{id}` | 查询单个学生 | 200, 404 |
| POST | `/api/students` | 创建学生 | 201, 400 |
| PUT | `/api/students/{id}` | 更新学生信息 | 200, 404 |
| DELETE | `/api/students/{id}` | 删除学生 | 204, 400, 404 |

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

| 方法 | 路径 | 说明 | 状态码 |
|------|------|------|--------|
| GET | `/api/enrollments` | 查询所有选课记录 | 200 |
| GET | `/api/enrollments/course/{courseId}` | 按课程查询 | 200 |
| GET | `/api/enrollments/student/{studentId}` | 按学生查询 | 200 |
| POST | `/api/enrollments` | 学生选课 | 201, 400, 404 |
| DELETE | `/api/enrollments/{id}` | 学生退课 | 204, 404 |

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

| 状态码 | 说明 | 使用场景 |
|--------|------|----------|
| 200 | OK | 查询、更新成功 |
| 201 | Created | 创建成功 |
| 204 | No Content | 删除成功 |
| 400 | Bad Request | 请求参数错误、业务规则违反 |
| 404 | Not Found | 资源不存在 |
| 500 | Internal Server Error | 服务器内部错误 |

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
