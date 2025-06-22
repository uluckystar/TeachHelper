package com.teachhelper.service.task;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.teachhelper.entity.StudentAnswer;
import com.teachhelper.ai.AIEvaluationService;
import com.teachhelper.service.exam.ExamStatusService;
import com.teachhelper.service.student.StudentAnswerService;

/**
 * 批量评阅执行服务 - 简化版本
 * 负责执行批量评阅任务并在完成后更新考试状态
 */
public class SimpleBatchEvaluationExecutorService {
    
    private final AIEvaluationService aiEvaluationService;
    private final StudentAnswerService studentAnswerService;
    private final ExamStatusService examStatusService;
    
    public SimpleBatchEvaluationExecutorService(
            AIEvaluationService aiEvaluationService,
            StudentAnswerService studentAnswerService,
            ExamStatusService examStatusService) {
        this.aiEvaluationService = aiEvaluationService;
        this.studentAnswerService = studentAnswerService;
        this.examStatusService = examStatusService;
    }
    
    /**
     * 执行批量评阅任务
     * 
     * @param examId 考试ID
     * @param questionId 题目ID
     * @param answerIds 待评阅的答案ID列表
     * @return 异步任务结果
     */
    public CompletableFuture<Void> executeBatchEvaluation(
            Long examId, Long questionId, List<Long> answerIds) {
        
        return CompletableFuture.runAsync(() -> {
            try {
                // 收集相关的考试ID
                Set<Long> affectedExamIds = new HashSet<>();
                affectedExamIds.add(examId);
                
                System.out.println("开始批量评阅任务 - 考试ID: " + examId + ", 题目ID: " + questionId + ", 答案数量: " + answerIds.size());
                
                // 逐个评阅答案
                for (Long answerId : answerIds) {
                    try {
                        StudentAnswer answer = studentAnswerService.getAnswerById(answerId);
                        if (answer != null) {
                            // 执行AI评阅
                            aiEvaluationService.evaluateAnswer(answer);
                            
                            // 收集答案所属的考试ID
                            if (answer.getQuestion() != null && answer.getQuestion().getExam() != null) {
                                affectedExamIds.add(answer.getQuestion().getExam().getId());
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("评阅答案 " + answerId + " 时发生错误: " + e.getMessage());
                    }
                }
                
                System.out.println("批量评阅任务完成，开始检查考试状态");
                
                // 批阅完成后，检查并更新相关考试的状态
                int updatedExams = examStatusService.checkAndUpdateExamsToEvaluated(affectedExamIds);
                
                System.out.println("考试状态检查完成，更新了 " + updatedExams + " 个考试的状态为 EVALUATED");
                
            } catch (Exception e) {
                System.err.println("批量评阅任务执行失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
