package com.teachhelper.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 知识库收藏请求DTO
 */
@Data
@Schema(description = "知识库收藏请求")
public class KnowledgeBaseFavoriteRequest {
    
    @NotNull(message = "知识库ID不能为空")
    @Schema(description = "知识库ID", required = true)
    private Long knowledgeBaseId;
}
