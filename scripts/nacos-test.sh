#!/bin/bash

echo "==================================================="
echo "   Nacos 服务注册与发现测试脚本"
echo "==================================================="
echo ""

echo ">>> 步骤 1: 启动所有服务..."
docker-compose up -d

echo ""
echo ">>> 步骤 2: 等待服务启动 (30秒)..."
sleep 30

echo ""
echo ">>> 步骤 3: 检查 Nacos 控制台..."
echo "访问 Nacos 控制台: http://localhost:8848/nacos/"
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" http://localhost:8848/nacos/

echo ""
echo ">>> 步骤 4: 检查服务注册情况..."
echo ""
echo "--- Catalog Service ---"
curl -s "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=catalog-service&groupName=COURSEHUB_GROUP" | jq '{service: .name, instanceCount: (.hosts | length), instances: [.hosts[] | {ip: .ip, port: .port, healthy: .healthy}]}'

echo ""
echo "--- User Service ---"
curl -s "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=user-service&groupName=COURSEHUB_GROUP" | jq '{service: .name, instanceCount: (.hosts | length), instances: [.hosts[] | {ip: .ip, port: .port, healthy: .healthy}]}'

echo ""
echo "--- Enrollment Service ---"
curl -s "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=enrollment-service&groupName=COURSEHUB_GROUP" | jq '{service: .name, instanceCount: (.hosts | length), instances: [.hosts[] | {ip: .ip, port: .port, healthy: .healthy}]}'

echo ""
echo ">>> 步骤 5: 测试服务调用..."
echo ""
echo "测试 Enrollment Service (通过服务发现调用其他服务):"
for i in {1..10}; do
    echo -n "第 $i 次请求: "
    response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8102/api/enrollments)
    if [ "$response" -eq 200 ]; then
        echo "✓ 成功 (HTTP $response)"
    else
        echo "✗ 失败 (HTTP $response)"
    fi
    sleep 1
done

echo ""
echo ">>> 步骤 6: 测试健康检查端点..."
echo ""
echo "--- User Service Health ---"
curl -s http://localhost:8100/actuator/health | jq '{status: .status, nacosDiscovery: .components.nacosDiscovery.status}'

echo ""
echo "--- Catalog Service Health ---"
curl -s http://localhost:8101/actuator/health | jq '{status: .status, nacosDiscovery: .components.nacosDiscovery.status}'

echo ""
echo "--- Enrollment Service Health ---"
curl -s http://localhost:8102/actuator/health | jq '{status: .status, nacosDiscovery: .components.nacosDiscovery.status}'

echo ""
echo ">>> 步骤 7: 查看容器状态..."
docker-compose ps

echo ""
echo "==================================================="
echo "   测试完成！"
echo "==================================================="
echo ""
echo "访问 Nacos 控制台: http://localhost:8849/"
echo "登录凭据: nacos / nacos"
echo ""
echo "服务访问地址:"
echo "  - User Service:       http://localhost:8100/api/students"
echo "  - Catalog Service:    http://localhost:8101/api/courses"
echo "  - Enrollment Service: http://localhost:8102/api/enrollments"
echo ""
