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
import com.teachhelper.entity.User;
import com.teachhelper.repository.UserRepository;

/**
 * 开发环境用户数据创建服务
 */
@Service
@Transactional
public class DevUserDataService {

    private static final Logger log = LoggerFactory.getLogger(DevUserDataService.class);    @Autowired
    private UserRepository userRepository;

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
    }    /**
     * 创建学生信息示例数据 (已废弃 - 现在统一使用User表)
     * @deprecated 使用createUsers()方法代替
     */
    @Deprecated
    public void createStudents() {
        log.warn("createStudents()方法已废弃，Student表已不再使用，所有学生数据已合并到User表中");
        // 不再创建Student实体，因为已经合并到User表中
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

    /**
     * 清空用户数据
     */
    @Transactional
    public void clearUserData() {
        try {
            // 删除用户数据（包含学生数据，因为已合并）
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
