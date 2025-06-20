package com.teachhelper.dto.request;

import lombok.Data;

@Data
public class KnowledgeBaseCreateRequest {
    private String name;
    private String description;
    private String subject;
    private String gradeLevel;
    
    // Manual getters (fallback for Lombok issues)
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getSubject() { return subject; }
    public String getGradeLevel() { return gradeLevel; }
}
