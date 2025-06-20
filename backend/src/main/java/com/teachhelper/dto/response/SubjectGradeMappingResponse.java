package com.teachhelper.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 学科年级关联响应
 */
@Data
public class SubjectGradeMappingResponse {
    
    private Long id;
    private Long subjectId;
    private String subjectName;
    private Long gradeLevelId;
    private String gradeLevelName;
    private String gradeLevelCategory;
    private Integer priority;
    private Boolean isRecommended;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
