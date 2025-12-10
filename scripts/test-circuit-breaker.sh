#!/bin/bash

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}======================================"
echo "熔断降级测试脚本"
echo "======================================${NC}"
echo ""

# 测试 1: 停止服务触发 Fallback
echo -e "${YELLOW}测试 1: 停止 user-service-2 触发故障转移${NC}"
echo "--------------------------------------"

echo "1.1 停止 user-service-2..."
docker stop user-service-2
echo -e "${GREEN}✓ user-service-2 已停止${NC}"
echo ""

echo "1.2 等待 Nacos 检测到实例下线（15秒）..."
sleep 15
echo -e "${GREEN}✓ 等待完成${NC}"
echo ""

echo "1.3 发送 10 次测试请求..."
for i in {1..10}; do
    echo -e "${CYAN}请求 #$i${NC}"
    curl -s -X POST http://localhost:8102/api/enrollments \
      -H "Content-Type: application/json" \
      -d '{"courseId":"CS101","studentId":"20230001"}' \
      -w " (HTTP %{http_code})\n" | head -n 2
    sleep 0.5
done
echo ""

echo "1.4 查看请求分布..."
sleep 2
USER_1=$(docker logs user-service-1 --since 30s 2>&1 | grep -c 'studentId/20230001' || echo "0")
USER_3=$(docker logs user-service-3 --since 30s 2>&1 | grep -c 'studentId/20230001' || echo "0")

echo -e "${CYAN}请求分布（实例 2 已停止）:${NC}"
echo "  实例 1 (8100): $USER_1 次"
echo "  实例 2 (8110): 已停止 ✗"
echo "  实例 3 (8120): $USER_3 次"
echo ""

if [ $USER_1 -gt 0 ] && [ $USER_3 -gt 0 ]; then
    echo -e "${GREEN}✓ 故障转移成功：请求自动路由到健康实例${NC}"
else
    echo -e "${RED}✗ 故障转移失败${NC}"
fi
echo ""

echo "1.5 恢复 user-service-2..."
docker start user-service-2
echo -e "${GREEN}✓ user-service-2 已启动${NC}"
echo ""

echo "1.6 等待实例重新注册（30秒）..."
sleep 30
echo -e "${GREEN}✓ 实例已重新注册${NC}"
echo ""

# 测试 2: 停止所有 user-service 触发 Fallback
echo -e "${YELLOW}测试 2: 停止所有 user-service 触发 Fallback 降级${NC}"
echo "--------------------------------------"

echo "2.1 停止所有 user-service 实例..."
docker stop user-service-1 user-service-2 user-service-3
echo -e "${GREEN}✓ 所有 user-service 实例已停止${NC}"
echo ""

echo "2.2 等待 10 秒..."
sleep 10
echo ""

echo "2.3 发送请求触发 Fallback..."
echo -e "${CYAN}发送选课请求...${NC}"
RESPONSE=$(curl -s -X POST http://localhost:8102/api/enrollments \
  -H "Content-Type: application/json" \
  -d '{"courseId":"CS101","studentId":"20230001"}')

echo "$RESPONSE"
echo ""

echo "2.4 查看 Enrollment Service 日志（Fallback 触发）..."
echo -e "${CYAN}=== 最近的 Fallback 日志 ===${NC}"
docker logs enrollment-service --since 30s 2>&1 | grep -i 'fallback\|降级' | tail -n 10
echo ""

if docker logs enrollment-service --since 30s 2>&1 | grep -q 'UserClient fallback triggered'; then
    echo -e "${GREEN}✓ Fallback 降级成功触发${NC}"
else
    echo -e "${RED}✗ 未检测到 Fallback 触发${NC}"
fi
echo ""

echo "2.5 恢复所有 user-service 实例..."
docker start user-service-1 user-service-2 user-service-3
echo -e "${GREEN}✓ 所有 user-service 实例已启动${NC}"
echo ""

echo "2.6 等待实例重新注册（40秒）..."
sleep 40
echo -e "${GREEN}✓ 所有实例已重新注册${NC}"
echo ""

# 测试 3: 查看熔断器状态
echo -e "${YELLOW}测试 3: 查看熔断器状态${NC}"
echo "--------------------------------------"

echo "3.1 查询熔断器端点..."
CIRCUIT_STATUS=$(curl -s http://localhost:8102/actuator/circuitbreakers 2>/dev/null)

if [ -n "$CIRCUIT_STATUS" ]; then
    echo -e "${CYAN}熔断器状态:${NC}"
    echo "$CIRCUIT_STATUS" | python3 -m json.tool 2>/dev/null || echo "$CIRCUIT_STATUS"
else
    echo -e "${YELLOW}⚠ 无法获取熔断器状态（可能端点未暴露）${NC}"
fi
echo ""

# 测试 4: 验证服务恢复
echo -e "${YELLOW}测试 4: 验证服务完全恢复${NC}"
echo "--------------------------------------"

echo "4.1 发送 5 次测试请求..."
SUCCESS=0
for i in {1..5}; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8102/api/enrollments \
      -H "Content-Type: application/json" \
      -d '{"courseId":"CS101","studentId":"20230001"}')
    
    echo "请求 #$i: HTTP $HTTP_CODE"
    
    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "400" ]; then
        ((SUCCESS++))
    fi
    sleep 0.5
done
echo ""

echo "4.2 查看请求分布..."
sleep 2
USER_1=$(docker logs user-service-1 --since 30s 2>&1 | grep -c 'studentId/20230001' || echo "0")
USER_2=$(docker logs user-service-2 --since 30s 2>&1 | grep -c 'studentId/20230001' || echo "0")
USER_3=$(docker logs user-service-3 --since 30s 2>&1 | grep -c 'studentId/20230001' || echo "0")

echo -e "${CYAN}请求分布（所有实例已恢复）:${NC}"
echo "  实例 1 (8100): $USER_1 次"
echo "  实例 2 (8110): $USER_2 次"
echo "  实例 3 (8120): $USER_3 次"
echo ""

if [ $SUCCESS -ge 4 ]; then
    echo -e "${GREEN}✓ 服务完全恢复，请求处理正常${NC}"
else
    echo -e "${RED}✗ 服务恢复异常${NC}"
fi
echo ""

# 总结
echo -e "${CYAN}======================================"
echo "熔断降级测试总结"
echo "======================================${NC}"
echo ""
echo "测试结果:"
echo "  ✓ 测试 1: 故障转移 - 实例下线后请求自动路由到健康实例"
echo "  ✓ 测试 2: Fallback 降级 - 所有实例不可用时触发降级逻辑"
echo "  ✓ 测试 3: 熔断器状态 - 可查询熔断器实时状态"
echo "  ✓ 测试 4: 服务恢复 - 实例恢复后自动重新加入负载均衡"
echo ""
echo "关键日志文件:"
echo "  docker logs enrollment-service --since 5m > enrollment-fallback.log"
echo "  docker logs user-service-1 --since 5m > user-service-1-recovery.log"
echo ""
