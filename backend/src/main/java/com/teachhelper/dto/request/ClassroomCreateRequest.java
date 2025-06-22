package com.teachhelper.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建班级请求 DTO
 */
public class ClassroomCreateRequest {
    
    @NotBlank(message = "班级名称不能为空")
    @Size(max = 100, message = "班级名称长度不能超过100个字符")
    private String name;
    
    @Size(max = 500, message = "班级描述长度不能超过500个字符")
    private String description;
    
    // 构造方法
    public ClassroomCreateRequest() {}
    
    public ClassroomCreateRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}
