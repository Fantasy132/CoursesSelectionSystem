#!/bin/bash

# CORS 跨域配置测试脚本
# 测试 Gateway 的 CORS 配置是否正确

BASE_URL="http://localhost:8090"
ORIGIN="http://localhost:3000"  # 模拟前端源
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "======================================"
echo "      CORS 跨域配置测试"
echo "======================================"
echo ""

# 测试计数器
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 测试函数
test_cors() {
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    TEST_NAME=$1
    URL=$2
    METHOD=$3
    ORIGIN=$4
    EXPECTED_HEADER=$5
    
    echo -e "${YELLOW}[测试 $TOTAL_TESTS]${NC} $TEST_NAME"
    echo "  请求: $METHOD $URL"
    echo "  源: $ORIGIN"
    
    # 发送预检请求（OPTIONS）
    if [ "$METHOD" == "OPTIONS" ]; then
        RESPONSE=$(curl -s -i -X OPTIONS "$URL" \
            -H "Origin: $ORIGIN" \
            -H "Access-Control-Request-Method: POST" \
            -H "Access-Control-Request-Headers: Authorization,Content-Type")
    else
        RESPONSE=$(curl -s -i -X "$METHOD" "$URL" \
            -H "Origin: $ORIGIN" \
            -H "Authorization: Bearer test_token")
    fi
    
    # 检查响应头
    if echo "$RESPONSE" | grep -i "$EXPECTED_HEADER" > /dev/null; then
        echo -e "  ${GREEN}✓ 通过${NC} - 找到响应头: $EXPECTED_HEADER"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        
        # 显示相关的 CORS 响应头
        echo "$RESPONSE" | grep -i "access-control" | while read line; do
            echo -e "    ${BLUE}$line${NC}"
        done
    else
        echo -e "  ${RED}✗ 失败${NC} - 未找到响应头: $EXPECTED_HEADER"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo "  完整响应头:"
        echo "$RESPONSE" | head -20
    fi
    echo ""
}

echo "----------------------------------------"
echo "第一部分：预检请求测试 (OPTIONS)"
echo "----------------------------------------"
echo ""

# 测试 1: OPTIONS 请求 - 检查 Allow-Origin
test_cors \
    "预检请求 - 验证 Access-Control-Allow-Origin" \
    "$BASE_URL/api/courses" \
    "OPTIONS" \
    "http://localhost:3000" \
    "Access-Control-Allow-Origin"

# 测试 2: OPTIONS 请求 - 检查 Allow-Methods
test_cors \
    "预检请求 - 验证 Access-Control-Allow-Methods" \
    "$BASE_URL/api/enrollments" \
    "OPTIONS" \
    "http://localhost:8080" \
    "Access-Control-Allow-Methods"

# 测试 3: OPTIONS 请求 - 检查 Allow-Headers
test_cors \
    "预检请求 - 验证 Access-Control-Allow-Headers" \
    "$BASE_URL/api/students" \
    "OPTIONS" \
    "http://example.com" \
    "Access-Control-Allow-Headers"

echo "----------------------------------------"
echo "第二部分：实际请求测试 (GET/POST)"
echo "----------------------------------------"
echo ""

# 测试 4: GET 请求带 Origin
test_cors \
    "GET 请求 - 验证 Access-Control-Allow-Origin" \
    "$BASE_URL/api/auth/login" \
    "GET" \
    "http://localhost:3000" \
    "Access-Control-Allow-Origin"

# 测试 5: POST 请求带 Origin
test_cors \
    "POST 请求 - 验证 Access-Control-Allow-Origin" \
    "$BASE_URL/api/auth/login" \
    "POST" \
    "http://localhost:8080" \
    "Access-Control-Allow-Origin"

echo "----------------------------------------"
echo "第三部分：暴露响应头测试"
echo "----------------------------------------"
echo ""

echo -e "${YELLOW}[测试 6]${NC} 验证暴露的响应头（Exposed Headers）"
echo "  说明: 测试客户端是否可以访问自定义响应头"
echo ""

# 登录获取 Token
echo "  获取测试 Token..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -H "Origin: http://localhost:3000" \
    -d '{"studentId":"2021001","password":"123456"}')

TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token' 2>/dev/null)

if [ -z "$TOKEN" ] || [ "$TOKEN" == "null" ]; then
    echo -e "  ${YELLOW}⚠ 警告: 无法获取 Token，跳过此测试${NC}"
    echo "  响应: $LOGIN_RESPONSE"
else
    echo -e "  ${GREEN}✓ Token 获取成功${NC}"
    
    # 测试带 Token 的请求，检查暴露的响应头
    echo ""
    echo "  发送带 Token 的请求，检查响应头..."
    RESPONSE=$(curl -s -i -X GET "$BASE_URL/api/courses" \
        -H "Origin: http://localhost:3000" \
        -H "Authorization: Bearer $TOKEN")
    
    # 检查是否有 Access-Control-Expose-Headers
    if echo "$RESPONSE" | grep -i "Access-Control-Expose-Headers" > /dev/null; then
        echo -e "  ${GREEN}✓ 找到 Access-Control-Expose-Headers${NC}"
        EXPOSED_HEADERS=$(echo "$RESPONSE" | grep -i "Access-Control-Expose-Headers" | cut -d: -f2-)
        echo -e "    ${BLUE}暴露的响应头:$EXPOSED_HEADERS${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "  ${YELLOW}⚠ 未找到 Access-Control-Expose-Headers（可能未配置）${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
fi

echo ""
echo ""

echo "----------------------------------------"
echo "第四部分：不同源测试"
echo "----------------------------------------"
echo ""

# 测试不同的 Origin
ORIGINS=(
    "http://localhost:3000"
    "http://localhost:8080"
    "http://127.0.0.1:3000"
    "https://example.com"
)

for ORIGIN in "${ORIGINS[@]}"; do
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -e "${YELLOW}[测试 $TOTAL_TESTS]${NC} 测试源: $ORIGIN"
    
    RESPONSE=$(curl -s -i -X OPTIONS "$BASE_URL/api/courses" \
        -H "Origin: $ORIGIN" \
        -H "Access-Control-Request-Method: GET")
    
    if echo "$RESPONSE" | grep -i "Access-Control-Allow-Origin" > /dev/null; then
        ALLOWED_ORIGIN=$(echo "$RESPONSE" | grep -i "Access-Control-Allow-Origin" | cut -d: -f2- | tr -d '\r')
        echo -e "  ${GREEN}✓ 允许${NC} - 响应: Access-Control-Allow-Origin:$ALLOWED_ORIGIN"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "  ${RED}✗ 拒绝${NC} - 未返回 Access-Control-Allow-Origin"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    echo ""
done

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
    echo -e "${GREEN}✓ 所有 CORS 测试通过！${NC}"
    echo ""
    echo "CORS 配置正常，前端可以跨域访问 API"
    exit 0
else
    echo -e "${YELLOW}⚠ 部分测试失败${NC}"
    echo ""
    echo "建议检查："
    echo "  1. Gateway 服务是否正常运行"
    echo "  2. CORS 配置是否正确加载"
    echo "  3. 查看 Gateway 日志"
    exit 1
fi
