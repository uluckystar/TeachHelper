package com.teachhelper.service.classroom;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachhelper.dto.request.ClassroomCreateRequest;
import com.teachhelper.dto.response.ClassroomResponse;
import com.teachhelper.entity.Classroom;
import com.teachhelper.entity.Role;
import com.teachhelper.entity.User;
import com.teachhelper.exception.ResourceNotFoundException;
import com.teachhelper.repository.ClassroomRepository;
import com.teachhelper.service.auth.AuthService;

@Service
@Transactional
public class ClassroomService {
    
    @Autowired
    private ClassroomRepository classroomRepository;
    
    @Autowired
    private AuthService authService;
    
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 8;
    private final SecureRandom random = new SecureRandom();
    
    /**
     * 创建新班级
     */
    public Classroom createClassroom(ClassroomCreateRequest request) {
        User currentUser = authService.getCurrentUser();
        
        // 检查用户权限：只有教师和管理员可以创建班级
        if (!currentUser.getRoles().contains(Role.TEACHER) && !currentUser.getRoles().contains(Role.ADMIN)) {
            throw new RuntimeException("只有教师和管理员可以创建班级");
        }
        
        // 生成唯一的班级代码
        String classCode = generateUniqueClassCode();
        
        Classroom classroom = new Classroom(request.getName(), request.getDescription(), currentUser, classCode);
        return classroomRepository.save(classroom);
    }
    
    /**
     * 获取班级详情
     */
    public Classroom getClassroomById(Long id) {
        return classroomRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("班级不存在，ID: " + id));
    }
    
    /**
     * 获取当前用户相关的班级列表
     * - 教师/管理员：获取自己创建的班级
     * - 学生：获取自己加入的班级
     */
    public List<Classroom> getClassroomsByCurrentUser() {
        User currentUser = authService.getCurrentUser();
        
        if (currentUser.getRoles().contains(Role.ADMIN)) {
            // 管理员获取所有班级
            return classroomRepository.findAllByOrderByCreatedAtDesc();
        } else if (currentUser.getRoles().contains(Role.STUDENT)) {
            // 学生获取自己加入的班级
            return classroomRepository.findByStudentId(currentUser.getId());
        } else {
            // 教师获取自己创建的班级
            return classroomRepository.findByCreatedByIdOrderByCreatedAtDesc(currentUser.getId());
        }
    }
    
    /**
     * 学生加入班级
     */
    public Classroom joinClassroom(String classCode) {
        User currentUser = authService.getCurrentUser();
        
        // 检查用户权限：只有学生可以加入班级
        if (!currentUser.getRoles().contains(Role.STUDENT)) {
            throw new RuntimeException("只有学生可以加入班级");
        }
        
        // 根据班级代码查找班级
        Classroom classroom = classroomRepository.findByClassCode(classCode)
            .orElseThrow(() -> new ResourceNotFoundException("班级代码无效: " + classCode));
        
        // 检查学生是否已经在班级中
        if (classroom.getStudents().contains(currentUser)) {
            throw new RuntimeException("您已经在该班级中了");
        }
        
        // 添加学生到班级
        classroom.addStudent(currentUser);
        return classroomRepository.save(classroom);
    }
    
    /**
     * 学生离开班级
     */
    public void leaveClassroom(Long classroomId) {
        User currentUser = authService.getCurrentUser();
        
        if (!currentUser.getRoles().contains(Role.STUDENT)) {
            throw new RuntimeException("只有学生可以离开班级");
        }
        
        Classroom classroom = getClassroomById(classroomId);
        
        if (!classroom.getStudents().contains(currentUser)) {
            throw new RuntimeException("您不在该班级中");
        }
        
        classroom.removeStudent(currentUser);
        classroomRepository.save(classroom);
    }
    
    /**
     * 教师从班级移除学生
     */
    public void removeStudentFromClassroom(Long classroomId, Long studentId) {
        User currentUser = authService.getCurrentUser();
        Classroom classroom = getClassroomById(classroomId);
        
        // 检查权限：只有班级创建者或管理员可以移除学生
        if (!classroom.getCreatedBy().equals(currentUser) && !currentUser.getRoles().contains(Role.ADMIN)) {
            throw new RuntimeException("无权限移除学生");
        }
        
        // 查找要移除的学生
        User studentToRemove = classroom.getStudents().stream()
            .filter(student -> student.getId().equals(studentId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("学生不在该班级中"));
        
        classroom.removeStudent(studentToRemove);
        classroomRepository.save(classroom);
    }
    
    /**
     * 更新班级信息
     */
    public Classroom updateClassroom(Long id, ClassroomCreateRequest request) {
        User currentUser = authService.getCurrentUser();
        Classroom classroom = getClassroomById(id);
        
        // 检查权限：只有班级创建者或管理员可以更新班级
        if (!classroom.getCreatedBy().equals(currentUser) && !currentUser.getRoles().contains(Role.ADMIN)) {
            throw new RuntimeException("无权限更新班级");
        }
        
        classroom.setName(request.getName());
        classroom.setDescription(request.getDescription());
        return classroomRepository.save(classroom);
    }
    
    /**
     * 删除班级
     */
    public void deleteClassroom(Long id) {
        User currentUser = authService.getCurrentUser();
        Classroom classroom = getClassroomById(id);
        
        // 检查权限：只有班级创建者或管理员可以删除班级
        if (!classroom.getCreatedBy().equals(currentUser) && !currentUser.getRoles().contains(Role.ADMIN)) {
            throw new RuntimeException("无权限删除班级");
        }
        
        classroomRepository.delete(classroom);
    }
    
    /**
     * 根据班级代码查找班级
     */
    public Classroom getClassroomByCode(String classCode) {
        return classroomRepository.findByClassCode(classCode)
            .orElseThrow(() -> new ResourceNotFoundException("班级代码无效: " + classCode));
    }
    
    /**
     * 转换为响应DTO
     */
    public ClassroomResponse convertToResponse(Classroom classroom) {
        ClassroomResponse response = new ClassroomResponse();
        response.setId(classroom.getId());
        response.setName(classroom.getName());
        response.setDescription(classroom.getDescription());
        response.setClassCode(classroom.getClassCode());
        response.setCreatedBy(classroom.getCreatedBy().getUsername());
        response.setCreatedAt(classroom.getCreatedAt());
        response.setUpdatedAt(classroom.getUpdatedAt());
        response.setStudentCount(classroom.getStudents().size());
        response.setExamCount(classroom.getExams().size());
        
        // 转换学生列表
        List<ClassroomResponse.StudentResponse> students = classroom.getStudents().stream()
            .map(student -> new ClassroomResponse.StudentResponse(
                student.getId(),
                student.getUsername(),
                student.getEmail(),
                null // joinedAt 需要额外的关联表来记录
            ))
            .collect(Collectors.toList());
        response.setStudents(students);
        
        return response;
    }
    
    /**
     * 生成唯一的班级代码
     */
    private String generateUniqueClassCode() {
        String code;
        do {
            code = generateRandomCode();
        } while (classroomRepository.existsByClassCode(code));
        return code;
    }
    
    /**
     * 生成随机班级代码
     */
    private String generateRandomCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
