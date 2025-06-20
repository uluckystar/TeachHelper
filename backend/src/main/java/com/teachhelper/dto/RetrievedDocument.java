package com.teachhelper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 检索到的文档数据传输对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedDocument {
    
    /**
     * 文档内容
     */
    private String content;
    
    /**
     * 相似度分数
     */
    private Double score;
    
    /**
     * 文档元数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 文档ID
     */
    private String id;
    
    /**
     * 文档标题
     */
    private String title;
    
    /**
     * 文档来源
     */
    private String source;
}
