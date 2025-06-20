package com.teachhelper.dto.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 向量搜索响应DTO
 */
@Data
public class VectorSearchResponse {
    
    /**
     * 搜索结果列表
     */
    private List<VectorSearchResult> results;
    
    /**
     * 总数量
     */
    private int totalCount;
    
    /**
     * 搜索用时（毫秒）
     */
    private long searchTime;
    
    /**
     * 搜索结果项
     */
    @Data
    public static class VectorSearchResult {
        
        /**
         * 结果ID
         */
        private String id;
        
        /**
         * 标题
         */
        private String title;
        
        /**
         * 内容
         */
        private String content;
        
        /**
         * 类型：document, knowledge_point, question
         */
        private String type;
        
        /**
         * 来源
         */
        private String source;
        
        /**
         * 相似度分数（0-1）
         */
        private double similarity;
        
        /**
         * 元数据
         */
        private Map<String, Object> metadata;
        
        /**
         * 知识库ID
         */
        private String knowledgeBaseId;
        
        /**
         * 文档ID
         */
        private String documentId;
        
        /**
         * 分块索引
         */
        private Integer chunkIndex;
        
        /**
         * 分块ID
         */
        private String chunkId;
        
        /**
         * 高亮显示的内容 (关键词用 <mark> 标签包围)
         */
        private String highlightedContent;
        
        /**
         * 匹配的关键词列表
         */
        private List<String> matchedKeywords;
        
        /**
         * 关键词在原文中的位置信息
         */
        private List<KeywordPosition> keywordPositions;
    }
    
    /**
     * 关键词位置信息
     */
    @Data
    public static class KeywordPosition {
        
        /**
         * 关键词
         */
        private String keyword;
        
        /**
         * 开始位置
         */
        private int startIndex;
        
        /**
         * 结束位置
         */
        private int endIndex;
        
        /**
         * 上下文片段 (包含关键词前后的文本)
         */
        private String context;
    }
}
