#!/bin/bash

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== 测试微服务拆分 ===${NC}"
echo ""

# 等待服务启动
echo -e "${YELLOW}等待服务启动...${NC}"
sleep 5

# 1. 测试用户服务 - 创建学生
echo -e "\n${GREEN}1. 测试用户服务 - 创建学生${NC}"
STUDENT_RESPONSE=$(curl -s -X POST http://localhost:8100/api/students \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "2024001",
    "name": "张三",
    "major": "计算机科学与技术",
    "grade": 2024,
    "email": "zhangsan@example.edu.cn"
  }')
echo "$STUDENT_RESPONSE" | jq '.'

# 2. 获取所有学生
echo -e "\n${GREEN}2. 获取所有学生${NC}"
curl -s http://localhost:8100/api/students | jq '.'

# 3. 测试课程目录服务 - 创建课程
echo -e "\n${GREEN}3. 测试课程目录服务 - 创建课程${NC}"
COURSE_RESPONSE=$(curl -s -X POST http://localhost:8101/api/courses \
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
  }')
echo "$COURSE_RESPONSE" | jq '.'

# 4. 获取所有课程
echo -e "\n${GREEN}4. 获取所有课程${NC}"
COURSES=$(curl -s http://localhost:8101/api/courses)
echo "$COURSES" | jq '.'

# 提取课程ID
COURSE_ID=$(echo "$COURSES" | jq -r '.data[0].id')
echo -e "${YELLOW}课程ID: $COURSE_ID${NC}"

# 5. 测试选课（验证服务间通信）
echo -e "\n${GREEN}5. 测试学生选课（验证服务间通信）${NC}"
if [ -n "$COURSE_ID" ] && [ "$COURSE_ID" != "null" ]; then
  ENROLLMENT_RESPONSE=$(curl -s -X POST http://localhost:8102/api/enrollments \
    -H "Content-Type: application/json" \
    -d "{
      \"courseId\": \"$COURSE_ID\",
      \"studentId\": \"2024001\"
    }")
  echo "$ENROLLMENT_RESPONSE" | jq '.'
  
  # 检查是否成功
  if echo "$ENROLLMENT_RESPONSE" | jq -e '.success == true' > /dev/null; then
    echo -e "${GREEN}✓ 选课成功${NC}"
  else
    echo -e "${RED}✗ 选课失败${NC}"
  fi
else
  echo -e "${RED}✗ 无法获取课程ID，跳过选课测试${NC}"
fi

# 6. 查询选课记录
echo -e "\n${GREEN}6. 查询所有选课记录${NC}"
curl -s http://localhost:8102/api/enrollments | jq '.'

# 7. 按学生查询选课记录
echo -e "\n${GREEN}7. 按学生查询选课记录${NC}"
curl -s http://localhost:8102/api/enrollments/student/2024001 | jq '.'

# 8. 测试学生不存在的情况
echo -e "\n${GREEN}8. 测试选课失败（学生不存在）${NC}"
if [ -n "$COURSE_ID" ] && [ "$COURSE_ID" != "null" ]; then
  ERROR_RESPONSE=$(curl -s -X POST http://localhost:8102/api/enrollments \
    -H "Content-Type: application/json" \
    -d "{
      \"courseId\": \"$COURSE_ID\",
      \"studentId\": \"9999999\"
    }")
  echo "$ERROR_RESPONSE" | jq '.'
  
  # 检查是否返回404错误
  if echo "$ERROR_RESPONSE" | jq -e '.success == false' > /dev/null; then
    echo -e "${GREEN}✓ 正确返回错误信息${NC}"
  else
    echo -e "${RED}✗ 错误处理异常${NC}"
  fi
fi

# 9. 测试课程不存在的情况
echo -e "\n${GREEN}9. 测试选课失败（课程不存在）${NC}"
ERROR_RESPONSE=$(curl -s -X POST http://localhost:8102/api/enrollments \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": "non-existent-course",
    "studentId": "2024001"
  }')
echo "$ERROR_RESPONSE" | jq '.'

# 检查是否返回404错误
if echo "$ERROR_RESPONSE" | jq -e '.success == false' > /dev/null; then
  echo -e "${GREEN}✓ 正确返回错误信息${NC}"
else
  echo -e "${RED}✗ 错误处理异常${NC}"
fi

# 10. 测试重复选课
echo -e "\n${GREEN}10. 测试重复选课（应该失败）${NC}"
if [ -n "$COURSE_ID" ] && [ "$COURSE_ID" != "null" ]; then
  ERROR_RESPONSE=$(curl -s -X POST http://localhost:8102/api/enrollments \
    -H "Content-Type: application/json" \
    -d "{
      \"courseId\": \"$COURSE_ID\",
      \"studentId\": \"2024001\"
    }")
  echo "$ERROR_RESPONSE" | jq '.'
  
  if echo "$ERROR_RESPONSE" | jq -e '.success == false' > /dev/null; then
    echo -e "${GREEN}✓ 正确阻止重复选课${NC}"
  else
    echo -e "${RED}✗ 重复选课检查失败${NC}"
  fi
fi

# 11. 测试按课程查询选课记录
echo -e "\n${GREEN}11. 按课程查询选课记录${NC}"
if [ -n "$COURSE_ID" ] && [ "$COURSE_ID" != "null" ]; then
  curl -s http://localhost:8102/api/enrollments/course/$COURSE_ID | jq '.'
fi

# 12. 创建第二个学生用于容量测试
echo -e "\n${GREEN}12. 创建第二个学生${NC}"
curl -s -X POST http://localhost:8100/api/students \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "2024002",
    "name": "李四",
    "major": "软件工程",
    "grade": 2024,
    "email": "lisi@example.edu.cn"
  }' | jq '.'

# 13. 第二个学生选课
echo -e "\n${GREEN}13. 第二个学生选课${NC}"
if [ -n "$COURSE_ID" ] && [ "$COURSE_ID" != "null" ]; then
  curl -s -X POST http://localhost:8102/api/enrollments \
    -H "Content-Type: application/json" \
    -d "{
      \"courseId\": \"$COURSE_ID\",
      \"studentId\": \"2024002\"
    }" | jq '.'
fi

# 14. 验证课程已选人数增加
echo -e "\n${GREEN}14. 验证课程已选人数${NC}"
if [ -n "$COURSE_ID" ] && [ "$COURSE_ID" != "null" ]; then
  UPDATED_COURSE=$(curl -s http://localhost:8101/api/courses/$COURSE_ID)
  echo "$UPDATED_COURSE" | jq '.'
  
  ENROLLED=$(echo "$UPDATED_COURSE" | jq -r '.data.enrolled')
  echo -e "${YELLOW}当前已选人数: $ENROLLED${NC}"
  
  if [ "$ENROLLED" = "2" ]; then
    echo -e "${GREEN}✓ 课程人数统计正确${NC}"
  else
    echo -e "${RED}✗ 课程人数统计错误${NC}"
  fi
fi

# 15. 测试按学号查询学生
echo -e "\n${GREEN}15. 按学号查询学生${NC}"
curl -s http://localhost:8100/api/students/studentId/2024001 | jq '.'

# 16. 测试按课程代码查询课程
echo -e "\n${GREEN}16. 按课程代码查询课程${NC}"
curl -s http://localhost:8101/api/courses/code/CS101 | jq '.'

echo -e "\n${BLUE}=== 测试完成 ===${NC}"
echo -e "${YELLOW}测试覆盖内容：${NC}"
echo "  ✓ 用户服务 CRUD 操作"
echo "  ✓ 课程目录服务 CRUD 操作"
echo "  ✓ 选课服务业务逻辑"
echo "  ✓ 服务间通信（enrollment -> user, catalog）"
echo "  ✓ 学生不存在错误处理"
echo "  ✓ 课程不存在错误处理"
echo "  ✓ 重复选课检查"
echo "  ✓ 课程容量统计"
echo "  ✓ 按学号/课程代码查询"