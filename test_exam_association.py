#!/usr/bin/env python3
import requests
import json
import time

# 配置
API_BASE = "http://localhost:8080"
USERNAME = "admin"
PASSWORD = "password"

def login():
    """登录获取token"""
    login_data = {
        "username": USERNAME,
        "password": PASSWORD
    }
    response = requests.post(f"{API_BASE}/auth/login", json=login_data)
    print(f"登录响应状态码: {response.status_code}")
    print(f"登录响应内容: {response.text}")
    if response.status_code == 200:
        result = response.json()
        if result["success"]:
            return result["data"]["token"]
    raise Exception(f"登录失败: {response.text}")

def get_headers(token):
    """获取请求头"""
    return {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

def test_learning_answer_import():
    """测试学习通答案导入"""
    print("开始测试学习通答案导入...")
    
    # 1. 登录
    token = login()
    headers = get_headers(token)
    print("✅ 登录成功")
    
    # 2. 获取最新的考试ID
    response = requests.get(f"{API_BASE}/exams", headers=headers)
    if response.status_code != 200:
        raise Exception(f"获取考试列表失败: {response.text}")
    
    exams = response.json()["data"]["content"]
    if not exams:
        raise Exception("没有找到考试")
    
    exam_id = exams[0]["id"]  # 使用第一个考试
    exam_title = exams[0]["title"]
    print(f"✅ 选择考试: {exam_title} (ID: {exam_id})")
    
    # 3. 准备导入数据 - 单个文件导入
    file_path = "uploads/answer/路由与交换技术/测试/miniprogram1166115562438104-段志贤-2024-2025-2《路由与交换技术》期末考试--A卷.doc"
    
    # 发起异步导入请求
    import_data = {
        "filePath": file_path,
        "examId": exam_id
    }
    
    print(f"📤 开始导入文件: {file_path}")
    print(f"🎯 目标考试ID: {exam_id}")
    
    response = requests.post(f"{API_BASE}/student-answers/import-learning-answer-file", 
                           json=import_data, headers=headers)
    
    if response.status_code != 200:
        raise Exception(f"导入请求失败: {response.text}")
    
    result = response.json()
    if not result["success"]:
        raise Exception(f"导入失败: {result['message']}")
    
    task_id = result["data"]["taskId"]
    print(f"✅ 导入任务已创建: {task_id}")
    
    # 4. 监控任务进度
    max_wait = 60  # 最多等待60秒
    start_time = time.time()
    
    while time.time() - start_time < max_wait:
        response = requests.get(f"{API_BASE}/tasks/{task_id}", headers=headers)
        if response.status_code == 200:
            task = response.json()["data"]
            status = task["status"]
            progress = task.get("progress", 0)
            
            print(f"📊 任务状态: {status}, 进度: {progress}%")
            
            if status == "COMPLETED":
                print("✅ 导入任务完成!")
                break
            elif status == "FAILED":
                error = task.get("errorMessage", "未知错误")
                raise Exception(f"任务失败: {error}")
        
        time.sleep(2)
    else:
        raise Exception("任务超时")
    
    # 5. 验证题目是否正确关联到考试
    print(f"\n🔍 验证题目是否关联到考试 {exam_id}...")
    
    # 查询该考试的题目
    response = requests.get(f"{API_BASE}/questions/by-exam/{exam_id}", headers=headers)
    if response.status_code != 200:
        raise Exception(f"查询考试题目失败: {response.text}")
    
    questions = response.json()["data"]
    import_questions = [q for q in questions if "学习通" in str(q.get("sourceType", ""))]
    
    print(f"📋 考试中总题目数: {len(questions)}")
    print(f"📚 学习通导入题目数: {len(import_questions)}")
    
    if import_questions:
        print("✅ 发现学习通导入的题目:")
        for i, q in enumerate(import_questions[:3], 1):  # 只显示前3个
            print(f"  {i}. {q['title'][:50]}...")
            print(f"     考试ID: {q.get('examId', 'NULL')}")
            print(f"     来源: {q.get('sourceType', 'N/A')}")
        
        # 检查是否有题目没有关联考试
        unlinked = [q for q in import_questions if not q.get("examId")]
        if unlinked:
            print(f"❌ 发现 {len(unlinked)} 个题目没有关联考试!")
            return False
        else:
            print("✅ 所有学习通导入的题目都正确关联到考试!")
            return True
    else:
        print("❌ 没有发现学习通导入的题目")
        return False

if __name__ == "__main__":
    try:
        success = test_learning_answer_import()
        if success:
            print("\n🎉 测试通过! 学习通答案导入功能正常工作")
        else:
            print("\n❌ 测试失败! 题目没有正确关联考试")
    except Exception as e:
        print(f"\n💥 测试出错: {e}") 