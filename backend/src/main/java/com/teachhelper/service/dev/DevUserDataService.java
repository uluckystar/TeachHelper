package com.teachhelper.service.dev;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachhelper.entity.Role;
import com.teachhelper.entity.Student;
import com.teachhelper.entity.User;
import com.teachhelper.repository.StudentRepository;
import com.teachhelper.repository.UserRepository;

/**
 * 开发环境用户数据创建服务
 */
@Service
@Transactional
public class DevUserDataService {

    private static final Logger log = LoggerFactory.getLogger(DevUserDataService.class);

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 创建用户示例数据
     */
    public List<User> createUsers() {
        // 管理员
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@teachhelper.com");
        admin.setPassword(passwordEncoder.encode("password"));
        admin.setRoles(Set.of(Role.ADMIN));
        admin.setOrganizationId("ORG001");
        admin.setLlmApiCallLimitPerDay(500);
        admin.setLlmApiCallCountToday(0);
        admin.setBalance(new BigDecimal("1000.00"));
        admin.setEnabled(true);
        
        // 教师们
        User teacher1 = createTeacher("teacher1", "张伟", "zhang.wei@school.edu.cn", "500.00", 0);
        User teacher2 = createTeacher("teacher2", "李明", "li.ming@school.edu.cn", "500.00", 0);
        User teacher3 = createTeacher("teacher3", "王雷", "wang.lei@school.edu.cn", "300.00", 15);
        User teacher4 = createTeacher("teacher4", "刘燕", "liu.yan@school.edu.cn", "450.00", 8);
        
        // 学生们
        User student1 = createStudentUser("student1", "陈小明", "chen.xiaoming@student.edu.cn", "100.00", 0);
        User student2 = createStudentUser("student2", "刘小红", "liu.xiaohong@student.edu.cn", "100.00", 0);
        User student3 = createStudentUser("student3", "赵小刚", "zhao.xiaogang@student.edu.cn", "100.00", 0);
        User student4 = createStudentUser("student4", "黄小雨", "huang.xiaoyu@student.edu.cn", "80.00", 2);
        User student5 = createStudentUser("student5", "吴小丽", "wu.xiaoli@student.edu.cn", "120.00", 1);
        User student6 = createStudentUser("student6", "孙小军", "sun.xiaojun@student.edu.cn", "90.00", 3);
        User student7 = createStudentUser("student7", "马小斌", "ma.xiaobin@student.edu.cn", "110.00", 0);
        User student8 = createStudentUser("student8", "张小飞", "zhang.xiaofei@student.edu.cn", "95.00", 1);
        User student9 = createStudentUser("student9", "李小华", "li.xiaohua@student.edu.cn", "105.00", 0);
        User student10 = createStudentUser("student10", "王小强", "wang.xiaoqiang@student.edu.cn", "85.00", 2);
        
        return userRepository.saveAll(List.of(admin, teacher1, teacher2, teacher3, teacher4, 
                                            student1, student2, student3, student4, student5, 
                                            student6, student7, student8, student9, student10));
    }

    /**
     * 创建学生信息示例数据
     */
    public List<Student> createStudents() {
        Student student1 = createStudent("2021001001", "陈小明", "chen.xiaoming@student.edu.cn", "计算机科学与技术1班", "计算机科学与技术");
        Student student2 = createStudent("2021001002", "刘小红", "liu.xiaohong@student.edu.cn", "计算机科学与技术1班", "计算机科学与技术");
        Student student3 = createStudent("2021001003", "赵小刚", "zhao.xiaogang@student.edu.cn", "计算机科学与技术2班", "计算机科学与技术");
        Student student4 = createStudent("2021001004", "黄小雨", "huang.xiaoyu@student.edu.cn", "计算机科学与技术2班", "计算机科学与技术");
        Student student5 = createStudent("2021002001", "吴小丽", "wu.xiaoli@student.edu.cn", "软件工程1班", "软件工程");
        Student student6 = createStudent("2021002002", "孙小军", "sun.xiaojun@student.edu.cn", "软件工程1班", "软件工程");
        Student student7 = createStudent("2021002003", "马小斌", "ma.xiaobin@student.edu.cn", "软件工程2班", "软件工程");
        Student student8 = createStudent("2021003001", "张小飞", "zhang.xiaofei@student.edu.cn", "网络工程1班", "网络工程");
        Student student9 = createStudent("2021003002", "李小华", "li.xiaohua@student.edu.cn", "网络工程1班", "网络工程");
        Student student10 = createStudent("2021004001", "王小强", "wang.xiaoqiang@student.edu.cn", "信息安全1班", "信息安全");
        
        return studentRepository.saveAll(List.of(student1, student2, student3, student4, student5, 
                                                student6, student7, student8, student9, student10));
    }

    private User createTeacher(String username, String name, String email, String balance, int todayApiCalls) {
        User teacher = new User();
        teacher.setUsername(username);
        teacher.setEmail(email);
        teacher.setPassword(passwordEncoder.encode("password"));
        teacher.setRoles(Set.of(Role.TEACHER));
        teacher.setOrganizationId("ORG001");
        teacher.setLlmApiCallLimitPerDay(200);
        teacher.setLlmApiCallCountToday(todayApiCalls);
        teacher.setBalance(new BigDecimal(balance));
        teacher.setEnabled(true);
        return teacher;
    }

    private User createStudentUser(String username, String name, String email, String balance, int todayApiCalls) {
        User student = new User();
        student.setUsername(username);
        student.setEmail(email);
        student.setPassword(passwordEncoder.encode("password"));
        student.setRoles(Set.of(Role.STUDENT));
        student.setOrganizationId("ORG001");
        student.setLlmApiCallLimitPerDay(50);
        student.setLlmApiCallCountToday(todayApiCalls);
        student.setBalance(new BigDecimal(balance));
        student.setEnabled(true);
        return student;
    }

    private Student createStudent(String studentId, String name, String email, String className, String major) {
        Student student = new Student();
        student.setStudentId(studentId);
        student.setName(name);
        student.setEmail(email);
        student.setClassName(className);
        student.setMajor(major);
        return student;
    }

    /**
     * 清空用户和学生数据
     */
    @Transactional
    public void clearUserData() {
        try {
            // 先删除学生档案
            log.info("删除学生档案数据...");
            studentRepository.deleteAll();
        } catch (Exception e) {
            log.warn("删除学生档案失败: {}", e.getMessage());
        }
        
        try {
            // 再删除用户数据
            log.info("删除用户数据...");
            userRepository.deleteAll();
        } catch (Exception e) {
            log.warn("删除用户数据失败: {}", e.getMessage());
        }
        
        // 强制刷新并验证
        try {
            userRepository.flush();
            log.info("用户数据清理完成，当前用户数量: {}", userRepository.count());
        } catch (Exception e) {
            log.warn("刷新用户数据失败: {}", e.getMessage());
        }
    }
}
