#!/bin/bash

# Gateway 路由测试脚本

GATEWAY_URL="http://localhost:8090"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=================================="
echo "Gateway Service 路由测试"
echo "=================================="
echo ""

# 检查网关是否运行
echo -e "${YELLOW}检查网关健康状态...${NC}"
curl -s "${GATEWAY_URL}/actuator/health" | jq '.' || echo -e "${RED}网关未启动或健康检查失败${NC}"
echo ""

# 测试路由配置
echo -e "${YELLOW}获取网关路由配置...${NC}"
curl -s "${GATEWAY_URL}/actuator/gateway/routes" | jq '.' || echo -e "${RED}无法获取路由配置${NC}"
echo ""

# 测试课程服务路由
echo -e "${YELLOW}测试课程服务路由 (GET /api/courses)...${NC}"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" "${GATEWAY_URL}/api/courses")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d':' -f2)
BODY=$(echo "$RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" == "200" ] || [ "$HTTP_CODE" == "503" ]; then
    echo -e "${GREEN}✓ 路由正常，状态码: $HTTP_CODE${NC}"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
else
    echo -e "${RED}✗ 路由失败，状态码: $HTTP_CODE${NC}"
fi
echo ""

# 测试用户服务路由
echo -e "${YELLOW}测试用户服务路由 (GET /api/students)...${NC}"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" "${GATEWAY_URL}/api/students")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d':' -f2)
BODY=$(echo "$RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" == "200" ] || [ "$HTTP_CODE" == "503" ]; then
    echo -e "${GREEN}✓ 路由正常，状态码: $HTTP_CODE${NC}"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
else
    echo -e "${RED}✗ 路由失败，状态码: $HTTP_CODE${NC}"
fi
echo ""

# 测试选课服务路由
echo -e "${YELLOW}测试选课服务路由 (GET /api/enrollments)...${NC}"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" "${GATEWAY_URL}/api/enrollments")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d':' -f2)
BODY=$(echo "$RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" == "200" ] || [ "$HTTP_CODE" == "503" ]; then
    echo -e "${GREEN}✓ 路由正常，状态码: $HTTP_CODE${NC}"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
else
    echo -e "${RED}✗ 路由失败，状态码: $HTTP_CODE${NC}"
fi
echo ""

# 测试熔断器状态
echo -e "${YELLOW}查看熔断器状态...${NC}"
curl -s "${GATEWAY_URL}/actuator/circuitbreakers" | jq '.' || echo -e "${RED}无法获取熔断器状态${NC}"
echo ""

echo "=================================="
echo "测试完成"
echo "=================================="
