package com.teachhelper.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 文档处理统计DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentProcessingStatistics {

    private Long knowledgeBaseId;
    
    private Long totalDocuments;
    
    private Long pendingDocuments;
    
    private Long processingDocuments;
    
    private Long completedDocuments;
    
    private Long failedDocuments;
    
    private Long totalFileSize;
    
    private Long totalWordCount;
    
    private Long totalKnowledgePoints;
    
    private Double averageProcessingTime; // 平均处理时间（毫秒）
    
    private Double successRate; // 成功率
}
