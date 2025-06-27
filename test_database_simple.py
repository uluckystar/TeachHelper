#!/usr/bin/env python3
import subprocess
import time

def run_mysql_query(query):
    """执行MySQL查询并返回结果"""
    cmd = ['mysql', '-u', 'root', '-p12345', '-e', f"USE teach_helper; {query}"]
    result = subprocess.run(cmd, capture_output=True, text=True)
    return result.stdout

def test_exam_association():
    """测试考试关联功能"""
    print("开始测试学习通答案导入的考试关联功能...")
    
    # 1. 查询当前状态
    print("🔍 检查当前数据库状态...")
    
    total_query = "SELECT COUNT(*) FROM questions WHERE source_type LIKE '%学习通%';"
    total_result = run_mysql_query(total_query)
    print(f"学习通题目总数: {total_result.strip()}")
    
    exam_query = "SELECT COUNT(*) FROM questions WHERE source_type LIKE '%学习通%' AND exam_id IS NOT NULL;"
    exam_result = run_mysql_query(exam_query)
    print(f"有考试关联的题目数: {exam_result.strip()}")
    
    # 2. 查看最新的考试
    exam_list_query = "SELECT id, title FROM exams ORDER BY id DESC LIMIT 3;"
    exams = run_mysql_query(exam_list_query)
    print(f"可用考试:\n{exams}")
    
    # 3. 检查应用是否运行
    print("🔍 检查应用状态...")
    lsof_result = subprocess.run(['lsof', '-i', ':8080'], capture_output=True, text=True)
    if '8080' in lsof_result.stdout:
        print("✅ 应用正在端口8080运行")
    else:
        print("❌ 应用未在端口8080运行")
        return False
    
    # 4. 检查最近的题目创建情况
    recent_query = """
    SELECT id, title, exam_id, created_at 
    FROM questions 
    WHERE source_type LIKE '%学习通%' 
    ORDER BY created_at DESC 
    LIMIT 5;
    """
    recent_result = run_mysql_query(recent_query)
    print(f"最近的学习通题目:\n{recent_result}")
    
    # 5. 结论
    exam_count = int(exam_result.strip().split('\n')[-1])
    if exam_count > 0:
        print(f"✅ 发现 {exam_count} 个题目已关联考试")
        return True
    else:
        print("❌ 没有题目关联到考试")
        
        # 手动测试一下关联是否工作
        print("🔧 手动测试数据库关联...")
        test_update = "UPDATE questions SET exam_id = 49 WHERE id = 334 AND source_type LIKE '%学习通%';"
        run_mysql_query(test_update)
        
        verify_query = "SELECT id, exam_id FROM questions WHERE id = 334;"
        verify_result = run_mysql_query(verify_query)
        print(f"手动关联测试结果: {verify_result}")
        
        # 恢复
        restore_update = "UPDATE questions SET exam_id = NULL WHERE id = 334;"
        run_mysql_query(restore_update)
        
        return False

if __name__ == "__main__":
    success = test_exam_association()
    if success:
        print("\n🎉 考试关联功能正常!")
    else:
        print("\n❌ 考试关联功能需要修复!") 