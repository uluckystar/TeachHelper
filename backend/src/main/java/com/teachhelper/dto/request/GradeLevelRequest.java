package com.teachhelper.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 年级创建/更新请求DTO
 */
@Data
public class GradeLevelRequest {
    
    @NotBlank(message = "年级名称不能为空")
    @Size(max = 100, message = "年级名称长度不能超过100个字符")
    private String name;
    
    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;
    
    @Size(max = 50, message = "类别长度不能超过50个字符")
    private String category; // 如：小学、初中、高中、本科、研究生
    
    private Integer sortOrder = 0;
    
    // Manual getters and setters for compatibility
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
