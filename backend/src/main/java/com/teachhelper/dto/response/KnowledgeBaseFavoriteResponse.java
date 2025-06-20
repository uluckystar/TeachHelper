package com.teachhelper.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库收藏响应DTO
 */
@Data
@Schema(description = "知识库收藏响应")
public class KnowledgeBaseFavoriteResponse {
    
    @Schema(description = "收藏ID")
    private Long id;
    
    @Schema(description = "用户ID")
    private Long userId;
    
    @Schema(description = "知识库ID")
    private Long knowledgeBaseId;
    
    @Schema(description = "知识库信息")
    private KnowledgeBaseResponse knowledgeBase;
    
    @Schema(description = "收藏时间")
    private LocalDateTime createdAt;
}
