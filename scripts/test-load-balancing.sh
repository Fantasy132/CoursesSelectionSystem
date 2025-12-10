#!/bin/bash

echo "======================================"
echo "负载均衡验证脚本"
echo "======================================"
echo ""

# 测试参数
ENROLLMENT_SERVICE_URL="http://localhost:8102"
TEST_ITERATIONS=15

echo "步骤 1: 检查所有服务实例是否在 Nacos 中注册"
echo "--------------------------------------"
echo "访问 Nacos 控制台: http://localhost:8848/nacos"
echo "用户名: nacos, 密码: nacos"
echo ""
echo "预期结果:"
echo "  - user-service: 3 个实例 (端口 8100, 8110, 8120)"
echo "  - catalog-service: 3 个实例 (端口 8101, 8111, 8121)"
echo "  - enrollment-service: 1 个实例 (端口 8102)"
echo ""
read -p "按回车键继续..."
echo ""

echo "步骤 2: 测试 User Service 负载均衡"
echo "--------------------------------------"
echo "通过 Enrollment Service 间接调用 User Service"
echo "发送 ${TEST_ITERATIONS} 次请求，观察请求分发到不同的 user-service 实例"
echo ""

# 测试用的学生 ID（需要确保数据库中存在）
STUDENT_ID="20230001"

echo "测试 GET /api/students/studentId/${STUDENT_ID} (通过 Enrollment Service)"
for i in $(seq 1 $TEST_ITERATIONS); do
    echo -n "请求 #${i}: "
    # 通过 enrollment-service 触发对 user-service 的调用
    # 可以查看 enrollment-service 的日志来观察负载均衡
    curl -s -X POST "${ENROLLMENT_SERVICE_URL}/api/enrollments" \
         -H "Content-Type: application/json" \
         -d "{\"courseId\":\"CS101\",\"studentId\":\"${STUDENT_ID}\"}" \
         -w " (HTTP %{http_code})\n" \
         -o /dev/null
    sleep 0.5
done

echo ""
echo "步骤 3: 测试 Catalog Service 负载均衡"
echo "--------------------------------------"
echo "通过 Enrollment Service 间接调用 Catalog Service"
echo "发送 ${TEST_ITERATIONS} 次请求"
echo ""

# 测试用的课程 ID
COURSE_ID="CS101"

echo "测试 GET /api/courses/${COURSE_ID} (通过 Enrollment Service)"
for i in $(seq 1 $TEST_ITERATIONS); do
    echo -n "请求 #${i}: "
    curl -s -X POST "${ENROLLMENT_SERVICE_URL}/api/enrollments" \
         -H "Content-Type: application/json" \
         -d "{\"courseId\":\"${COURSE_ID}\",\"studentId\":\"2023000${i}\"}" \
         -w " (HTTP %{http_code})\n" \
         -o /dev/null
    sleep 0.5
done

echo ""
echo "步骤 4: 查看服务日志验证负载均衡"
echo "--------------------------------------"
echo "使用以下命令查看各个服务实例的日志:"
echo ""
echo "User Service 实例 1: docker logs user-service-1 --tail 50"
echo "User Service 实例 2: docker logs user-service-2 --tail 50"
echo "User Service 实例 3: docker logs user-service-3 --tail 50"
echo ""
echo "Catalog Service 实例 1: docker logs catalog-service-1 --tail 50"
echo "Catalog Service 实例 2: docker logs catalog-service-2 --tail 50"
echo "Catalog Service 实例 3: docker logs catalog-service-3 --tail 50"
echo ""
echo "Enrollment Service: docker logs enrollment-service --tail 50"
echo ""

echo "步骤 5: 验证预期结果"
echo "--------------------------------------"
echo "✓ 请求应该均匀分布在 3 个 user-service 实例上"
echo "✓ 请求应该均匀分布在 3 个 catalog-service 实例上"
echo "✓ 每个实例的日志中都应该能看到处理请求的记录"
echo "✓ Nacos 控制台显示所有实例状态为 UP"
echo ""

echo "步骤 6: 测试实例故障转移"
echo "--------------------------------------"
echo "停止一个实例并观察请求是否自动转移到其他实例"
echo ""
echo "停止 user-service-2: docker stop user-service-2"
echo "再次发送请求观察是否只分发到 user-service-1 和 user-service-3"
echo ""
echo "恢复实例: docker start user-service-2"
echo ""

echo "======================================"
echo "负载均衡验证完成"
echo "======================================"
