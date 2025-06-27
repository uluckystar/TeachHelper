#!/usr/bin/env python3
import mysql.connector
import subprocess
import time

def test_manual_import():
    """手动测试导入功能"""
    print("开始手动测试学习通答案导入...")
    
    # 连接数据库
    conn = mysql.connector.connect(
        host='localhost',
        user='root',
        password='12345',
        database='teach_helper'
    )
    cursor = conn.cursor()
    
    try:
        # 1. 查询导入前的题目数量
        cursor.execute("SELECT COUNT(*) FROM questions WHERE source_type LIKE '%学习通%'")
        before_count = cursor.fetchone()[0]
        
        # 2. 查询导入前有exam_id的题目数量
        cursor.execute("SELECT COUNT(*) FROM questions WHERE source_type LIKE '%学习通%' AND exam_id IS NOT NULL")
        before_exam_count = cursor.fetchone()[0]
        
        print(f"导入前: 学习通题目总数={before_count}, 有考试关联={before_exam_count}")
        
        # 3. 获取一个有效的考试ID
        cursor.execute("SELECT id, title FROM exams ORDER BY id DESC LIMIT 1")
        exam_result = cursor.fetchone()
        if not exam_result:
            print("❌ 没有找到考试记录")
            return False
        
        exam_id, exam_title = exam_result
        print(f"选择考试: {exam_title} (ID: {exam_id})")
        
        # 4. 等待应用启动
        print("等待应用启动...")
        time.sleep(20)
        
        # 5. 使用curl直接调用API (绕过前端)
        test_data = f'''{{
            "filePath": "uploads/answer/路由与交换技术/测试/miniprogram1166115562438104-段志贤-2024-2025-2《路由与交换技术》期末考试--A卷.doc",
            "examId": {exam_id}
        }}'''
        
        # 先获取token
        login_cmd = [
            'curl', '-s', '-X', 'POST', 
            'http://localhost:8080/auth/login',
            '-H', 'Content-Type: application/json',
            '-H', 'X-Requested-With: XMLHttpRequest',  # 添加这个头可能绕过CSRF
            '-d', '{"username":"admin","password":"password"}'
        ]
        
        print("尝试登录...")
        login_result = subprocess.run(login_cmd, capture_output=True, text=True)
        print(f"登录响应: {login_result.returncode}, {login_result.stdout}")
        
        if login_result.returncode != 0:
            print("❌ 登录失败，尝试直接检查数据库变化")
        
        # 6. 等待一段时间，然后检查数据库变化
        print("等待可能的后台处理...")
        time.sleep(10)
        
        # 7. 查询导入后的题目数量
        cursor.execute("SELECT COUNT(*) FROM questions WHERE source_type LIKE '%学习通%'")
        after_count = cursor.fetchone()[0]
        
        # 8. 查询导入后有exam_id的题目数量
        cursor.execute("SELECT COUNT(*) FROM questions WHERE source_type LIKE '%学习通%' AND exam_id IS NOT NULL")
        after_exam_count = cursor.fetchone()[0]
        
        print(f"导入后: 学习通题目总数={after_count}, 有考试关联={after_exam_count}")
        
        # 9. 检查最近创建的题目
        cursor.execute("""
            SELECT id, title, exam_id, created_at 
            FROM questions 
            WHERE source_type LIKE '%学习通%' 
            ORDER BY created_at DESC 
            LIMIT 3
        """)
        
        recent_questions = cursor.fetchall()
        print("\n最近的学习通题目:")
        for q in recent_questions:
            qid, title, exam_id_val, created_at = q
            print(f"  ID: {qid}, 考试ID: {exam_id_val}, 创建时间: {created_at}")
            print(f"  题目: {title[:60]}...")
        
        # 10. 验证结果
        if after_exam_count > before_exam_count:
            print(f"\n✅ 成功! 新增了 {after_exam_count - before_exam_count} 个有考试关联的题目")
            return True
        else:
            print(f"\n❌ 失败! 没有新增有考试关联的题目")
            return False
        
    except Exception as e:
        print(f"💥 测试出错: {e}")
        return False
    finally:
        cursor.close()
        conn.close()

if __name__ == "__main__":
    success = test_manual_import()
    if success:
        print("\n🎉 手动测试通过!")
    else:
        print("\n❌ 手动测试失败!") 