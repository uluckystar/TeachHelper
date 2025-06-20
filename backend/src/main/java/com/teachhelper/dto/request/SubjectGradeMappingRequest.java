package com.teachhelper.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

/**
 * 学科年级关联创建/更新请求
 */
@Data
public class SubjectGradeMappingRequest {
    
    @NotNull(message = "学科ID不能为空")
    private Long subjectId;
    
    @NotNull(message = "年级ID不能为空") 
    private Long gradeLevelId;
    
    @Min(value = 0, message = "优先级不能小于0")
    private Integer priority = 0;
    
    private Boolean isRecommended = true;
}
