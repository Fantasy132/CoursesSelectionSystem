#!/bin/bash

# User Service 认证功能测试脚本

GATEWAY_URL="http://localhost:8090"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "=========================================="
echo "User Service 认证功能测试"
echo "=========================================="
echo ""

# 测试 1: 用户登录
echo -e "${BLUE}[测试 1] 用户登录${NC}"
echo -e "${YELLOW}请求: POST /api/auth/login${NC}"

LOGIN_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -X POST "${GATEWAY_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "2021001",
    "password": "123456"
  }')

HTTP_CODE=$(echo "$LOGIN_RESPONSE" | grep "HTTP_CODE" | cut -d':' -f2)
BODY=$(echo "$LOGIN_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" == "200" ]; then
    echo -e "${GREEN}✓ 登录成功 (200)${NC}"
    TOKEN=$(echo "$BODY" | jq -r '.token')
    echo "Token: ${TOKEN:0:50}..."
    echo "$BODY" | jq '.'
else
    echo -e "${RED}✗ 登录失败 (HTTP $HTTP_CODE)${NC}"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
fi
echo ""

# 测试 2: 验证 Token
if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
    echo -e "${BLUE}[测试 2] 验证 Token${NC}"
    echo -e "${YELLOW}请求: GET /api/auth/validate${NC}"
    
    VALIDATE_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
      -X GET "${GATEWAY_URL}/api/auth/validate" \
      -H "Authorization: Bearer $TOKEN")
    
    HTTP_CODE=$(echo "$VALIDATE_RESPONSE" | grep "HTTP_CODE" | cut -d':' -f2)
    BODY=$(echo "$VALIDATE_RESPONSE" | sed '/HTTP_CODE/d')
    
    if [ "$HTTP_CODE" == "200" ]; then
        echo -e "${GREEN}✓ Token 验证成功 (200)${NC}"
        echo "$BODY" | jq '.'
    else
        echo -e "${RED}✗ Token 验证失败 (HTTP $HTTP_CODE)${NC}"
        echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
    fi
    echo ""
fi

# 测试 3: 使用 Token 获取学生信息
if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
    echo -e "${BLUE}[测试 3] 使用 Token 获取学生信息${NC}"
    echo -e "${YELLOW}请求: GET /api/students/studentId/2021001${NC}"
    
    STUDENT_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
      -X GET "${GATEWAY_URL}/api/students/studentId/2021001" \
      -H "Authorization: Bearer $TOKEN")
    
    HTTP_CODE=$(echo "$STUDENT_RESPONSE" | grep "HTTP_CODE" | cut -d':' -f2)
    BODY=$(echo "$STUDENT_RESPONSE" | sed '/HTTP_CODE/d')
    
    if [ "$HTTP_CODE" == "200" ]; then
        echo -e "${GREEN}✓ 获取学生信息成功 (200)${NC}"
        echo "$BODY" | jq '.'
    else
        echo -e "${RED}✗ 获取学生信息失败 (HTTP $HTTP_CODE)${NC}"
        echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
    fi
    echo ""
fi

# 测试 4: 错误密码登录
echo -e "${BLUE}[测试 4] 错误密码登录（预期失败）${NC}"
echo -e "${YELLOW}请求: POST /api/auth/login (错误密码)${NC}"

FAIL_LOGIN_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -X POST "${GATEWAY_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "2021001",
    "password": "wrongpassword"
  }')

HTTP_CODE=$(echo "$FAIL_LOGIN_RESPONSE" | grep "HTTP_CODE" | cut -d':' -f2)
BODY=$(echo "$FAIL_LOGIN_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" == "401" ]; then
    echo -e "${GREEN}✓ 正确返回 401 未授权${NC}"
    echo "$BODY" | jq '.'
else
    echo -e "${RED}✗ 预期返回 401，实际返回 $HTTP_CODE${NC}"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
fi
echo ""

# 测试 5: 注册新用户
RANDOM_ID=$((2021100 + RANDOM % 900))
echo -e "${BLUE}[测试 5] 注册新用户 (学号: $RANDOM_ID)${NC}"
echo -e "${YELLOW}请求: POST /api/auth/register${NC}"

REGISTER_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -X POST "${GATEWAY_URL}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"studentId\": \"${RANDOM_ID}\",
    \"name\": \"测试用户${RANDOM_ID}\",
    \"password\": \"test123456\",
    \"email\": \"test${RANDOM_ID}@example.com\",
    \"major\": \"测试专业\",
    \"grade\": 2021
  }")

HTTP_CODE=$(echo "$REGISTER_RESPONSE" | grep "HTTP_CODE" | cut -d':' -f2)
BODY=$(echo "$REGISTER_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" == "201" ]; then
    echo -e "${GREEN}✓ 注册成功 (201)${NC}"
    NEW_TOKEN=$(echo "$BODY" | jq -r '.token')
    echo "新用户 Token: ${NEW_TOKEN:0:50}..."
    echo "$BODY" | jq '.'
else
    echo -e "${RED}✗ 注册失败 (HTTP $HTTP_CODE)${NC}"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
fi
echo ""

# 测试 6: 重复注册（预期失败）
echo -e "${BLUE}[测试 6] 重复注册（预期失败）${NC}"
echo -e "${YELLOW}请求: POST /api/auth/register (相同学号)${NC}"

DUPLICATE_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -X POST "${GATEWAY_URL}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "2021001",
    "name": "重复用户",
    "password": "123456",
    "email": "duplicate@example.com"
  }')

HTTP_CODE=$(echo "$DUPLICATE_RESPONSE" | grep "HTTP_CODE" | cut -d':' -f2)
BODY=$(echo "$DUPLICATE_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" == "400" ]; then
    echo -e "${GREEN}✓ 正确返回 400 错误请求${NC}"
    echo "$BODY" | jq '.'
else
    echo -e "${RED}✗ 预期返回 400，实际返回 $HTTP_CODE${NC}"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
fi
echo ""

# 测试 7: 无效 Token 验证
echo -e "${BLUE}[测试 7] 无效 Token 验证（预期失败）${NC}"
echo -e "${YELLOW}请求: GET /api/auth/validate (无效 Token)${NC}"

INVALID_TOKEN_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -X GET "${GATEWAY_URL}/api/auth/validate" \
  -H "Authorization: Bearer invalid.token.here")

HTTP_CODE=$(echo "$INVALID_TOKEN_RESPONSE" | grep "HTTP_CODE" | cut -d':' -f2)
BODY=$(echo "$INVALID_TOKEN_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" == "400" ] || [ "$HTTP_CODE" == "401" ]; then
    echo -e "${GREEN}✓ 正确返回错误状态码 ($HTTP_CODE)${NC}"
    echo "$BODY" | jq '.'
else
    echo -e "${RED}✗ 预期返回 400/401，实际返回 $HTTP_CODE${NC}"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
fi
echo ""

echo "=========================================="
echo "测试完成"
echo "=========================================="
