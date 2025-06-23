package com.teachhelper.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;

public class ExamCreateRequest {
    
    @NotBlank(message = "Exam title is required")
    @Size(max = 200, message = "Exam title must not exceed 200 characters")
    private String title;
    
    @Size(max = 1000, message = "Exam description must not exceed 1000 characters")
    private String description;
    
    // 目标班级ID列表，可选字段
    private List<Long> targetClassroomIds;
    
    // 考试时长（分钟）
    @Min(value = 1, message = "Exam duration must be at least 1 minute")
    private Integer duration;
    
    // 考试开始时间
    private LocalDateTime startTime;
    
    // 考试结束时间
    private LocalDateTime endTime;
    
    public ExamCreateRequest() {}
    
    public ExamCreateRequest(String title, String description) {
        this.title = title;
        this.description = description;
    }
    
    public ExamCreateRequest(String title, String description, List<Long> targetClassroomIds) {
        this.title = title;
        this.description = description;
        this.targetClassroomIds = targetClassroomIds;
    }
    
    public ExamCreateRequest(String title, String description, List<Long> targetClassroomIds, 
                           Integer duration, LocalDateTime startTime, LocalDateTime endTime) {
        this.title = title;
        this.description = description;
        this.targetClassroomIds = targetClassroomIds;
        this.duration = duration;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<Long> getTargetClassroomIds() {
        return targetClassroomIds;
    }
    
    public void setTargetClassroomIds(List<Long> targetClassroomIds) {
        this.targetClassroomIds = targetClassroomIds;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
