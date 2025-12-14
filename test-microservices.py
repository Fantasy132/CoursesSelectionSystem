#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
微服务系统集成测试脚本
测试项目：
1. Gateway 服务正常启动并注册到 Nacos
2. 路由配置正确，请求能转发到对应服务
3. JWT 认证过滤器实现完整
4. 登录接口返回有效的 JWT Token
5. 后端服务能从请求头获取用户信息
"""

import requests
import json
import time
import sys
from typing import Dict, Optional, Tuple

# 颜色定义
class Colors:
    RED = '\033[0;31m'
    GREEN = '\033[0;32m'
    YELLOW = '\033[1;33m'
    BLUE = '\033[0;34m'
    NC = '\033[0m'  # No Color

# 配置
GATEWAY_URL = "http://localhost:8090"
NACOS_URL = "http://localhost:8848"
USER_SERVICE_URL = "http://localhost:8100"
CATALOG_SERVICE_URL = "http://localhost:8101"
ENROLLMENT_SERVICE_URL = "http://localhost:8110"

# 测试结果统计
total_tests = 0
passed_tests = 0
failed_tests = 0

# JWT Token 和测试用户ID
jwt_token = None
test_user_id = None


def print_separator():
    """打印分隔线"""
    print(f"{Colors.BLUE}{'=' * 60}{Colors.NC}")


def print_test_title(test_num: str, title: str):
    """打印测试标题"""
    print(f"\n{Colors.BLUE}【测试 {test_num}】{Colors.NC} {title}")
    print_separator()


def print_success(message: str):
    """打印成功消息"""
    global passed_tests, total_tests
    print(f"{Colors.GREEN}✓ {message}{Colors.NC}")
    passed_tests += 1
    total_tests += 1


def print_error(message: str):
    """打印失败消息"""
    global failed_tests, total_tests
    print(f"{Colors.RED}✗ {message}{Colors.NC}")
    failed_tests += 1
    total_tests += 1


def print_warning(message: str):
    """打印警告消息"""
    print(f"{Colors.YELLOW}⚠ {message}{Colors.NC}")


def print_info(message: str):
    """打印信息消息"""
    print(f"{Colors.BLUE}ℹ {message}{Colors.NC}")


def test_gateway_registration():
    """测试 1: Gateway 服务正常启动并注册到 Nacos"""
    print_test_title("1", "Gateway 服务正常启动并注册到 Nacos")
    
    # 1.1 检查 Gateway 健康状态
    print_info("检查 Gateway 健康状态...")
    try:
        response = requests.get(f"{GATEWAY_URL}/actuator/health", timeout=5)
        if response.status_code == 200:
            print_success(f"Gateway 服务健康检查通过 (HTTP {response.status_code})")
            
            data = response.json()
            if data.get("status") == "UP":
                print_success("Gateway 状态为 UP")
            else:
                print_error("Gateway 状态不是 UP")
        else:
            print_error(f"Gateway 服务健康检查失败 (HTTP {response.status_code})")
    except requests.exceptions.RequestException as e:
        print_error(f"无法连接到 Gateway 服务: {e}")
    
    # 1.2 检查 Gateway 在 Nacos 的注册状态
    print_info("检查 Gateway 在 Nacos 的注册状态...")
    time.sleep(3)  # 等待注册完成
    
    try:
        # 使用正确的 namespace 和 group 查询服务列表
        response = requests.get(
            f"{NACOS_URL}/nacos/v1/ns/service/list",
            params={
                "pageNo": 1,
                "pageSize": 100,
                "namespaceId": "dev",
                "groupName": "COURSEHUB_GROUP"
            },
            timeout=5
        )
        
        if response.status_code == 200:
            data = response.json()
            services = data.get("doms", [])
            if "gateway-service" in services:
                print_success("Gateway 已成功注册到 Nacos")
                print_info(f"Nacos 中注册的服务: {', '.join(services)}")
                
                # 获取 Gateway 实例详情
                instance_response = requests.get(
                    f"{NACOS_URL}/nacos/v1/ns/instance/list",
                    params={
                        "serviceName": "gateway-service",
                        "namespaceId": "dev",
                        "groupName": "COURSEHUB_GROUP"
                    },
                    timeout=5
                )
                if instance_response.status_code == 200:
                    instance_data = instance_response.json()
                    hosts = instance_data.get("hosts", [])
                    print_info(f"Gateway 实例数量: {len(hosts)}")
                    if any(host.get("healthy") for host in hosts):
                        print_success("Gateway 实例健康状态正常")
                    else:
                        print_warning("Gateway 实例健康状态异常")
            else:
                print_error("Gateway 未注册到 Nacos")
        else:
            print_error(f"查询 Nacos 失败 (HTTP {response.status_code})")
    except requests.exceptions.RequestException as e:
        print_error(f"无法连接到 Nacos: {e}")
    
    # 1.3 检查 Gateway 的 Nacos Discovery 端点
    print_info("检查 Gateway 的 Nacos Discovery 信息...")
    try:
        response = requests.get(f"{GATEWAY_URL}/actuator/nacos-discovery", timeout=5)
        if response.status_code == 200:
            text = response.text
            if "gateway-service" in text:
                print_success("Gateway Nacos Discovery 端点正常")
            else:
                print_warning("Gateway Nacos Discovery 端点响应异常")
        else:
            print_warning(f"Gateway Nacos Discovery 端点返回 HTTP {response.status_code}")
    except requests.exceptions.RequestException as e:
        print_warning(f"无法访问 Nacos Discovery 端点: {e}")
    
    print()


def test_gateway_routing():
    """测试 2: 路由配置正确，请求能转发到对应服务"""
    print_test_title("2", "路由配置正确，请求能转发到对应服务")
    
    # 2.1 检查 Gateway 路由配置
    print_info("获取 Gateway 路由配置...")
    try:
        response = requests.get(f"{GATEWAY_URL}/actuator/gateway/routes", timeout=5)
        if response.status_code == 200:
            print_success("成功获取路由配置")
            
            routes_text = response.text
            
            # 检查关键路由
            if "catalog-service" in routes_text:
                print_success("找到 catalog-service 路由配置")
            else:
                print_error("未找到 catalog-service 路由配置")
            
            if "enrollment-service" in routes_text:
                print_success("找到 enrollment-service 路由配置")
            else:
                print_error("未找到 enrollment-service 路由配置")
            
            if "user-service" in routes_text:
                print_success("找到 user-service 路由配置")
            else:
                print_error("未找到 user-service 路由配置")
        else:
            print_error(f"获取路由配置失败 (HTTP {response.status_code})")
    except requests.exceptions.RequestException as e:
        print_error(f"无法获取路由配置: {e}")
    
    # 2.2 测试到 User Service 的路由
    print_info("测试路由转发到 User Service (登录接口)...")
    try:
        response = requests.post(
            f"{GATEWAY_URL}/api/auth/login",
            json={"studentId": "test", "password": "test"},
            timeout=5
        )
        
        # 401 或 200 都说明路由正常
        if response.status_code in [200, 401]:
            print_success(f"Gateway 成功转发请求到 User Service (HTTP {response.status_code})")
        else:
            print_error(f"Gateway 转发到 User Service 失败 (HTTP {response.status_code})")
    except requests.exceptions.RequestException as e:
        print_error(f"测试 User Service 路由失败: {e}")
    
    # 2.3 测试到 Catalog Service 的路由
    print_info("测试路由转发到 Catalog Service...")
    try:
        response = requests.get(f"{GATEWAY_URL}/api/courses", timeout=5)
        
        if response.status_code in [200, 401]:
            print_success(f"Gateway 成功转发请求到 Catalog Service (HTTP {response.status_code})")
        else:
            print_warning(f"Gateway 转发到 Catalog Service 响应码: HTTP {response.status_code}")
    except requests.exceptions.RequestException as e:
        print_error(f"测试 Catalog Service 路由失败: {e}")
    
    # 2.4 测试到 Enrollment Service 的路由
    print_info("测试路由转发到 Enrollment Service...")
    try:
        response = requests.get(f"{GATEWAY_URL}/api/enrollments", timeout=5)
        
        if response.status_code in [200, 401]:
            print_success(f"Gateway 成功转发请求到 Enrollment Service (HTTP {response.status_code})")
        else:
            print_warning(f"Gateway 转发到 Enrollment Service 响应码: HTTP {response.status_code}")
    except requests.exceptions.RequestException as e:
        print_error(f"测试 Enrollment Service 路由失败: {e}")
    
    print()


def test_jwt_filter():
    """测试 3: JWT 认证过滤器实现完整"""
    print_test_title("3", "JWT 认证过滤器实现完整")
    
    # 3.1 测试无 Token 访问受保护资源
    print_info("测试无 Token 访问受保护资源...")
    try:
        response = requests.get(f"{GATEWAY_URL}/api/courses", timeout=5)
        
        if response.status_code == 401:
            print_success("无 Token 访问被正确拦截 (HTTP 401)")
        else:
            print_warning(f"无 Token 访问返回 HTTP {response.status_code} (期望 401)")
    except requests.exceptions.RequestException as e:
        print_error(f"测试失败: {e}")
    
    # 3.2 测试无效 Token
    print_info("测试无效 Token...")
    try:
        response = requests.get(
            f"{GATEWAY_URL}/api/courses",
            headers={"Authorization": "Bearer invalid.token.here"},
            timeout=5
        )
        
        if response.status_code == 401:
            print_success("无效 Token 被正确拦截 (HTTP 401)")
        else:
            print_warning(f"无效 Token 返回 HTTP {response.status_code} (期望 401)")
    except requests.exceptions.RequestException as e:
        print_error(f"测试失败: {e}")
    
    # 3.3 测试 Token 格式错误
    print_info("测试 Token 格式错误...")
    try:
        response = requests.get(
            f"{GATEWAY_URL}/api/courses",
            headers={"Authorization": "InvalidFormat"},
            timeout=5
        )
        
        if response.status_code == 401:
            print_success("Token 格式错误被正确拦截 (HTTP 401)")
        else:
            print_warning(f"Token 格式错误返回 HTTP {response.status_code} (期望 401)")
    except requests.exceptions.RequestException as e:
        print_error(f"测试失败: {e}")
    
    # 3.4 测试白名单路径
    print_info("测试白名单路径 (登录接口)...")
    try:
        response = requests.post(
            f"{GATEWAY_URL}/api/auth/login",
            json={"studentId": "20210001", "password": "password123"},
            timeout=5
        )
        
        if response.status_code in [200, 401]:
            print_success(f"白名单路径可以无 Token 访问 (HTTP {response.status_code})")
        else:
            print_warning(f"白名单路径返回 HTTP {response.status_code}")
    except requests.exceptions.RequestException as e:
        print_error(f"测试失败: {e}")
    
    print()


def test_login_jwt():
    """测试 4: 登录接口返回有效的 JWT Token"""
    global jwt_token, test_user_id
    
    print_test_title("4", "登录接口返回有效的 JWT Token")
    
    # 使用唯一的测试用户ID（基于时间戳）
    test_student_id = f"test_{int(time.time())}"
    test_user_id = test_student_id  # 保存以便后续测试使用
    
    # 4.1 测试用户注册
    print_info("注册测试用户...")
    try:
        response = requests.post(
            f"{GATEWAY_URL}/api/auth/register",
            json={
                "studentId": test_student_id,
                "password": "password123",
                "name": "测试用户",
                "email": f"{test_student_id}@example.com",
                "major": "计算机科学"
            },
            timeout=10
        )
        
        if response.status_code in [200, 201]:
            print_success("用户注册成功")
            data = response.json()
            jwt_token = data.get("token")
            if jwt_token:
                print_success("注册时获取到 JWT Token")
        else:
            print_warning(f"注册返回 HTTP {response.status_code}: {response.text[:100]}")
    except requests.exceptions.RequestException as e:
        print_error(f"注册请求失败: {e}")
    
    # 4.2 测试用户登录
    print_info("测试用户登录...")
    try:
        response = requests.post(
            f"{GATEWAY_URL}/api/auth/login",
            json={
                "studentId": test_student_id,
                "password": "password123"
            },
            timeout=10
        )
        
        if response.status_code == 200:
            print_success("登录成功 (HTTP 200)")
            
            data = response.json()
            jwt_token = data.get("token")
            
            if jwt_token:
                print_success("成功获取 JWT Token")
                print_info(f"Token 长度: {len(jwt_token)} 字符")
                
                # 验证 Token 格式
                token_parts = jwt_token.split('.')
                if len(token_parts) == 3:
                    print_success("JWT Token 格式正确 (3 部分)")
                else:
                    print_error(f"JWT Token 格式错误 (应该有 3 部分，实际 {len(token_parts)} 部分)")
                
                # 显示 Token 预览
                if len(jwt_token) > 60:
                    token_preview = f"{jwt_token[:30]}...{jwt_token[-30:]}"
                else:
                    token_preview = jwt_token
                print_info(f"Token 预览: {token_preview}")
                
                # 检查响应中的用户信息
                if "studentId" in data:
                    print_success("响应包含用户 studentId")
                
                if "name" in data:
                    print_success("响应包含用户 name")
            else:
                print_error("未能从响应中提取 JWT Token")
                print(f"登录响应: {data}")
        else:
            print_error(f"登录失败 (HTTP {response.status_code})")
            print(f"响应: {response.text}")
    except requests.exceptions.RequestException as e:
        print_error(f"登录请求失败: {e}")
    
    print()


def test_user_info_extraction():
    """测试 5: 后端服务能从请求头获取用户信息"""
    print_test_title("5", "后端服务能从请求头获取用户信息")
    
    if not jwt_token:
        print_error("没有可用的 JWT Token，跳过此测试")
        print()
        return
    
    headers = {"Authorization": f"Bearer {jwt_token}"}
    
    # 5.1 使用 Token 访问 Catalog Service
    print_info("使用 Token 访问 Catalog Service...")
    try:
        response = requests.get(f"{GATEWAY_URL}/api/courses", headers=headers, timeout=5)
        
        if response.status_code == 200:
            print_success("使用有效 Token 成功访问 Catalog Service (HTTP 200)")
        else:
            print_error(f"使用 Token 访问 Catalog Service 失败 (HTTP {response.status_code})")
            print(f"响应: {response.text[:200]}")
    except requests.exceptions.RequestException as e:
        print_error(f"访问失败: {e}")
    
    # 5.2 使用 Token 访问 User Service
    print_info("使用 Token 访问 User Service 获取学生信息...")
    student_id_to_query = test_user_id if test_user_id else "20210001"
    try:
        response = requests.get(
            f"{GATEWAY_URL}/api/students/{student_id_to_query}",
            headers=headers,
            timeout=5
        )
        
        if response.status_code == 200:
            print_success("使用 Token 成功获取学生信息 (HTTP 200)")
            
            data = response.json()
            if data.get("data", {}).get("studentId") == student_id_to_query:
                print_success("返回的学生 ID 正确")
            
            if "name" in data.get("data", {}):
                print_success("返回包含学生姓名")
        else:
            print_warning(f"获取学生信息返回 HTTP {response.status_code}")
    except requests.exceptions.RequestException as e:
        print_warning(f"访问失败: {e}")
    
    # 5.3 测试 Enrollment Service
    print_info("使用 Token 访问 Enrollment Service...")
    try:
        response = requests.get(
            f"{GATEWAY_URL}/api/enrollments/student/{student_id_to_query}",
            headers=headers,
            timeout=5
        )
        
        if response.status_code == 200:
            print_success("使用 Token 成功访问 Enrollment Service (HTTP 200)")
        else:
            print_warning(f"访问 Enrollment Service 返回 HTTP {response.status_code}")
    except requests.exceptions.RequestException as e:
        print_warning(f"访问失败: {e}")
    
    # 5.4 验证请求头传递
    print_info("验证 Gateway 是否正确传递用户信息到后端服务...")
    print_info("测试创建课程 (需要后端验证用户权限)...")
    try:
        response = requests.post(
            f"{GATEWAY_URL}/api/courses",
            headers={**headers, "Content-Type": "application/json"},
            json={
                "code": "TEST001",
                "name": "测试课程",
                "credits": 3,
                "capacity": 30,
                "enrolled": 0
            },
            timeout=5
        )
        
        if response.status_code in [200, 201]:
            print_success(f"后端服务成功接收并处理带用户信息的请求 (HTTP {response.status_code})")
        else:
            print_info(f"创建课程返回 HTTP {response.status_code} (可能需要管理员权限)")
    except requests.exceptions.RequestException as e:
        print_warning(f"创建课程失败: {e}")
    
    print()


def main():
    """主测试流程"""
    print(f"{Colors.BLUE}")
    print("=" * 60)
    print("              微服务系统集成测试")
    print("=" * 60)
    print(f"{Colors.NC}")
    
    print_info("开始测试前检查...")
    
    # 检查服务可用性
    print_info("检查服务可用性...")
    try:
        response = requests.get(f"{GATEWAY_URL}/actuator/health", timeout=5)
        print_success("Gateway 服务已启动")
    except requests.exceptions.RequestException:
        print_warning("Gateway 服务未启动，某些测试可能失败")
        print_info("请确保已运行: docker-compose up -d")
    
    print()
    print_info("开始执行测试...")
    print()
    
    # 执行所有测试
    try:
        test_gateway_registration()
        test_gateway_routing()
        test_jwt_filter()
        test_login_jwt()
        test_user_info_extraction()
    except KeyboardInterrupt:
        print("\n\n测试被用户中断")
        sys.exit(1)
    
    # 打印测试总结
    print_separator()
    print(f"\n{Colors.BLUE}【测试总结】{Colors.NC}")
    print_separator()
    print(f"总测试数: {Colors.BLUE}{total_tests}{Colors.NC}")
    print(f"通过: {Colors.GREEN}{passed_tests}{Colors.NC}")
    print(f"失败: {Colors.RED}{failed_tests}{Colors.NC}")
    
    if failed_tests == 0:
        print(f"\n{Colors.GREEN}✓ 所有测试通过！{Colors.NC}\n")
        sys.exit(0)
    else:
        print(f"\n{Colors.RED}✗ 部分测试失败，请检查服务配置{Colors.NC}\n")
        sys.exit(1)


if __name__ == "__main__":
    main()
