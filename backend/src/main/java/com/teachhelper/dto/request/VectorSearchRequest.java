package com.teachhelper.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 向量搜索请求DTO
 */
@Data
public class VectorSearchRequest {
    
    /**
     * 搜索查询内容
     */
    @NotBlank(message = "搜索内容不能为空")
    @Size(max = 500, message = "搜索内容不能超过500个字符")
    private String query;
    
    /**
     * 相似度阈值（0.1-1.0）
     */
    @Min(value = 0, message = "相似度阈值不能小于0")
    @Max(value = 1, message = "相似度阈值不能大于1")
    private double similarityThreshold = 0.7;
    
    /**
     * 搜索范围
     */
    private List<String> searchScope;
    
    /**
     * 限制返回数量
     */
    @Min(value = 1, message = "返回数量至少为1")
    @Max(value = 100, message = "返回数量不能超过100")
    private Integer limit = 20;
    
    /**
     * 指定知识库ID（可选）
     */
    private Long knowledgeBaseId;
}
