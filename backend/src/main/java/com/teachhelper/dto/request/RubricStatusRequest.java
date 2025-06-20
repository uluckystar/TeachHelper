package com.teachhelper.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 评分标准状态切换请求 DTO
 */
public class RubricStatusRequest {
    
    @NotNull(message = "状态不能为空")
    private Boolean isActive;
    
    // Getters and Setters
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    // 兼容性方法
    public Boolean isActive() {
        return isActive;
    }
}
