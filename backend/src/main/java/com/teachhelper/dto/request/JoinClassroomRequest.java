package com.teachhelper.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 加入班级请求 DTO
 */
public class JoinClassroomRequest {
    
    @NotBlank(message = "班级代码不能为空")
    private String classCode;
    
    // 构造方法
    public JoinClassroomRequest() {}
    
    public JoinClassroomRequest(String classCode) {
        this.classCode = classCode;
    }
    
    // Getters and Setters
    public String getClassCode() {
        return classCode;
    }
    
    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }
}
