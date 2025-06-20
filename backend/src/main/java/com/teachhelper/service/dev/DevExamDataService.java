package com.teachhelper.service.dev;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachhelper.entity.Exam;
import com.teachhelper.entity.Role;
import com.teachhelper.entity.User;
import com.teachhelper.repository.ExamRepository;
import com.teachhelper.repository.UserRepository;

/**
 * 开发环境考试数据服务
 * 专门负责创建和管理考试相关的测试数据
 */
@Service
public class DevExamDataService {
    
    @Autowired
    private ExamRepository examRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 创建考试数据
     */
    @Transactional
    public void createExams() {
        if (examRepository.count() > 0) {
            System.out.println("考试数据已存在，跳过创建");
            return;
        }
        
        List<User> teachers = userRepository.findByRole(Role.TEACHER);
        if (teachers.isEmpty()) {
            System.out.println("未找到教师用户，跳过考试创建");
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // 创建多种类型的考试
        createExam(teachers.get(0), "Java基础编程考试", "测试学生Java基础编程能力，包括语法、面向对象等", 
                  90, now.plusDays(1), now.plusDays(8));
        
        createExam(teachers.get(1), "数据结构与算法期中考试", "涵盖数组、链表、栈、队列、树等数据结构的理解和应用", 
                  120, now.plusDays(3), now.plusDays(10));
        
        createExam(teachers.get(0), "Web开发综合实践", "HTML、CSS、JavaScript、Spring Boot等Web开发技术综合考试", 
                  150, now.plusDays(5), now.plusDays(12));
        
        createExam(teachers.get(2), "计算机网络原理考试", "网络协议、TCP/IP、HTTP、网络安全等知识点考查", 
                  100, now.plusDays(7), now.plusDays(14));
        
        createExam(teachers.get(1), "数据库系统原理", "SQL语言、数据库设计、事务处理、索引优化等内容", 
                  110, now.plusDays(2), now.plusDays(9));
        
        createExam(teachers.get(3), "软件工程概论", "软件开发生命周期、需求分析、设计模式、项目管理", 
                  90, now.plusDays(4), now.plusDays(11));
        
        createExam(teachers.get(0), "算法分析与设计", "分治法、贪心算法、动态规划、回溯法等算法设计思想", 
                  120, now.plusDays(6), now.plusDays(13));
        
        createExam(teachers.get(2), "操作系统原理", "进程管理、内存管理、文件系统、并发控制等核心概念", 
                  100, now.plusDays(8), now.plusDays(15));
        
        // 创建一些已结束的考试（用于历史数据）
        createExam(teachers.get(1), "C语言程序设计期末考试", "C语言基础语法、指针、结构体、文件操作等", 
                  120, now.minusDays(15), now.minusDays(8));
        
        createExam(teachers.get(3), "离散数学基础", "集合论、图论、逻辑推理、组合数学等数学基础", 
                  90, now.minusDays(20), now.minusDays(13));
        
        System.out.println("考试数据创建完成");
    }
    
    /**
     * 创建单个考试
     */
    private void createExam(User creator, String title, String description, 
                           int duration, LocalDateTime startTime, LocalDateTime endTime) {
        Exam exam = new Exam();
        exam.setTitle(title);
        exam.setDescription(description);
        exam.setDuration(duration);
        exam.setStartTime(startTime);
        exam.setEndTime(endTime);
        exam.setCreatedBy(creator);
        exam.setCreatedAt(LocalDateTime.now());
        exam.setUpdatedAt(LocalDateTime.now());
        examRepository.save(exam);
    }
    
    /**
     * 清理考试数据
     */
    @Transactional
    public void clearExamData() {
        examRepository.deleteAll();
        System.out.println("考试数据清理完成");
    }
}
