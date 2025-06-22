package com.teachhelper.controller.exam;

import java.util.Map;

import com.teachhelper.service.exam.ExamStatusService;

/**
 * 考试状态管理控制器
 * 提供简单的考试状态检查和更新端点
 */
public class ExamStatusController {
    
    private final ExamStatusService examStatusService;
    
    public ExamStatusController(ExamStatusService examStatusService) {
        this.examStatusService = examStatusService;
    }
    
    /**
     * 手动检查并更新指定考试的状态
     * 
     * @param examId 考试ID
     * @return 操作结果
     */
    public Map<String, Object> checkAndUpdateExamStatus(Long examId) {
        try {
            boolean updated = examStatusService.checkAndUpdateExamToEvaluated(examId);
            
            if (updated) {
                return Map.of(
                    "success", true,
                    "message", "考试状态已更新为 EVALUATED",
                    "examId", examId
                );
            } else {
                return Map.of(
                    "success", false,
                    "message", "考试状态无需更新或条件不满足",
                    "examId", examId
                );
            }
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "检查考试状态时发生错误: " + e.getMessage(),
                "examId", examId
            );
        }
    }
}
