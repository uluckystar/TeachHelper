#!/bin/bash

# 中文用户名注册功能测试脚本
echo "=================="
echo "中文用户名注册功能测试"
echo "=================="

API_BASE="http://localhost:8080/api"

# 检查后端服务是否运行
echo "检查后端服务状态..."
if curl -s "$API_BASE/auth/health" > /dev/null 2>&1; then
    echo "✅ 后端服务正常运行"
else
    echo "❌ 后端服务未运行，请先启动后端服务"
    echo "启动命令: mvn spring-boot:run"
    exit 1
fi

# 测试用例函数
test_register() {
    local username="$1"
    local email="$2"
    local password="$3"
    local role="$4"
    local description="$5"
    
    echo ""
    echo "测试: $description"
    echo "用户名: $username"
    
    response=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
        -X POST \
        -H "Content-Type: application/json" \
        -d "{
            \"username\": \"$username\",
            \"email\": \"$email\",
            \"password\": \"$password\",
            \"roles\": [\"$role\"]
        }" \
        "$API_BASE/auth/register")
    
    http_code=$(echo "$response" | grep "HTTP_CODE:" | cut -d: -f2)
    response_body=$(echo "$response" | grep -v "HTTP_CODE:")
    
    if [ "$http_code" -eq 200 ]; then
        echo "✅ 注册成功: $response_body"
    else
        echo "❌ 注册失败 ($http_code): $response_body"
    fi
}

# 测试有效的中文用户名
echo ""
echo "=== 有效中文用户名测试 ==="

test_register "张三" "zhangsan@example.com" "Test123456" "STUDENT" "纯中文用户名"
test_register "李四ABC" "lisi@example.com" "Test123456" "TEACHER" "中英文混合用户名"
test_register "王五_测试" "wangwu@example.com" "Test123456" "STUDENT" "带下划线的中文用户名"
test_register "测试用户123" "testuser123@example.com" "Test123456" "TEACHER" "中文数字混合用户名"

# 测试无效的中文用户名
echo ""
echo "=== 无效中文用户名测试 ==="

test_register "管理员" "admin@example.com" "Test123456" "STUDENT" "中文保留词测试"
test_register "教师" "teacher@example.com" "Test123456" "STUDENT" "中文保留词测试"
test_register "学生" "student@example.com" "Test123456" "STUDENT" "中文保留词测试"
test_register "张" "zhang@example.com" "Test123456" "STUDENT" "用户名过短测试"
test_register "张三@测试" "special@example.com" "Test123456" "STUDENT" "包含特殊字符测试"

# 测试角色安全性
echo ""
echo "=== 角色安全性测试 ==="

test_register "中文管理员" "admin2@example.com" "Test123456" "ADMIN" "尝试注册管理员角色"

echo ""
echo "=================="
echo "测试完成"
echo "=================="
