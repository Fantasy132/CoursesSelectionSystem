#!/bin/bash

# Gateway JWT 认证过滤器测试脚本
# 用于测试白名单、Token 验证、用户信息传递功能

BASE_URL="http://localhost:8090"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "======================================"
echo "   Gateway JWT 认证过滤器测试"
echo "======================================"
echo ""

# 测试计数器
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 测试函数
test_request() {
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    TEST_NAME=$1
    HTTP_METHOD=$2
    URL=$3
    HEADERS=$4
    BODY=$5
    EXPECTED_STATUS=$6
    
    echo -e "${YELLOW}[测试 $TOTAL_TESTS]${NC} $TEST_NAME"
    
    if [ "$HTTP_METHOD" == "GET" ]; then
        RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$URL" $HEADERS)
    elif [ "$HTTP_METHOD" == "POST" ]; then
        RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$URL" $HEADERS -d "$BODY")
    fi
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n-1)
    
    if [ "$HTTP_CODE" == "$EXPECTED_STATUS" ]; then
        echo -e "  ${GREEN}✓ 通过${NC} (HTTP $HTTP_CODE)"
        echo "  响应: $BODY"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        return 0
    else
        echo -e "  ${RED}✗ 失败${NC} (期望 HTTP $EXPECTED_STATUS, 实际 HTTP $HTTP_CODE)"
        echo "  响应: $BODY"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

echo "----------------------------------------"
echo "第一部分：白名单路径测试（无需 Token）"
echo "----------------------------------------"
echo ""

# 测试 1: 登录接口（白名单）
test_request \
    "登录接口 - 应该无需 Token" \
    "POST" \
    "$BASE_URL/api/auth/login" \
    '-H "Content-Type: application/json"' \
    '{"studentId":"2021001","password":"123456"}' \
    "200"

echo ""

# 获取 Token 用于后续测试
echo "获取测试用 Token..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"studentId":"2021001","password":"123456"}')

TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token')

if [ -z "$TOKEN" ] || [ "$TOKEN" == "null" ]; then
    echo -e "${RED}错误: 无法获取 Token，跳过后续测试${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Token 获取成功${NC}"
echo "Token: ${TOKEN:0:50}..."
echo ""

# 测试 2: 健康检查（白名单）
test_request \
    "健康检查 - 应该无需 Token" \
    "GET" \
    "$BASE_URL/actuator/health" \
    "" \
    "" \
    "200"

echo ""
echo ""

echo "----------------------------------------"
echo "第二部分：受保护路径测试（需要 Token）"
echo "----------------------------------------"
echo ""

# 测试 3: 不提供 Token 访问课程列表（应该失败）
test_request \
    "访问课程列表（无 Token）- 应该返回 401" \
    "GET" \
    "$BASE_URL/api/courses" \
    "" \
    "" \
    "401"

echo ""

# 测试 4: 使用无效 Token 访问课程列表（应该失败）
test_request \
    "访问课程列表（无效 Token）- 应该返回 401" \
    "GET" \
    "$BASE_URL/api/courses" \
    '-H "Authorization: Bearer invalid_token_xyz123"' \
    "" \
    "401"

echo ""

# 测试 5: 使用有效 Token 访问课程列表（应该成功）
test_request \
    "访问课程列表（有效 Token）- 应该返回 200" \
    "GET" \
    "$BASE_URL/api/courses" \
    "-H \"Authorization: Bearer $TOKEN\"" \
    "" \
    "200"

echo ""

# 测试 6: 使用有效 Token 访问选课列表（应该成功）
test_request \
    "访问选课列表（有效 Token）- 应该返回 200" \
    "GET" \
    "$BASE_URL/api/enrollments" \
    "-H \"Authorization: Bearer $TOKEN\"" \
    "" \
    "200"

echo ""

# 测试 7: 使用有效 Token 访问学生信息（应该成功）
test_request \
    "访问学生信息（有效 Token）- 应该返回 200" \
    "GET" \
    "$BASE_URL/api/students/2021001" \
    "-H \"Authorization: Bearer $TOKEN\"" \
    "" \
    "200"

echo ""
echo ""

echo "----------------------------------------"
echo "第三部分：用户信息传递测试"
echo "----------------------------------------"
echo ""

# 测试 8: 检查 Gateway 是否传递用户信息到下游服务
echo -e "${YELLOW}[测试 8]${NC} 验证用户信息传递（X-User-Id, X-User-Name, X-User-Email）"
echo "  说明: 此测试需要下游服务支持返回接收到的请求头"
echo "  如果下游服务返回数据中包含用户信息，说明传递成功"
echo ""

RESPONSE=$(curl -s -X GET "$BASE_URL/api/students/2021001" \
    -H "Authorization: Bearer $TOKEN")
echo "  响应: $RESPONSE"
echo ""

echo "----------------------------------------"
echo "第四部分：Query Parameter Token 测试"
echo "----------------------------------------"
echo ""

# 测试 9: 使用 Query Parameter 传递 Token（不推荐方式）
test_request \
    "通过 Query Parameter 传递 Token - 应该返回 200" \
    "GET" \
    "$BASE_URL/api/courses?token=$TOKEN" \
    "" \
    "" \
    "200"

echo ""
echo ""

echo "======================================"
echo "           测试结果汇总"
echo "======================================"
echo ""
echo -e "总测试数: $TOTAL_TESTS"
echo -e "${GREEN}通过: $PASSED_TESTS${NC}"
echo -e "${RED}失败: $FAILED_TESTS${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✓ 所有测试通过！${NC}"
    exit 0
else
    echo -e "${RED}✗ 部分测试失败，请检查日志${NC}"
    exit 1
fi
