package com.teachhelper.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 学科创建/更新请求DTO
 */
@Data
public class SubjectRequest {
    
    @NotBlank(message = "学科名称不能为空")
    @Size(max = 100, message = "学科名称长度不能超过100个字符")
    private String name;
    
    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;
    
    private Integer sortOrder = 0;
    
    // Manual getters and setters for compatibility
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
