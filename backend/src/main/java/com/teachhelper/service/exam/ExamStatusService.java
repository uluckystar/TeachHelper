package com.teachhelper.service.exam;

import java.util.Set;

import com.teachhelper.entity.Exam;
import com.teachhelper.entity.ExamStatus;
import com.teachhelper.repository.ExamRepository;
import com.teachhelper.service.student.StudentAnswerService;

/**
 * 考试状态管理服务
 * 专门负责考试状态的检查和更新
 */
public class ExamStatusService {
    
    private final ExamRepository examRepository;
    private final StudentAnswerService studentAnswerService;
    
    public ExamStatusService(ExamRepository examRepository, StudentAnswerService studentAnswerService) {
        this.examRepository = examRepository;
        this.studentAnswerService = studentAnswerService;
    }
    
    /**
     * 检查并更新单个考试状态为 EVALUATED
     * 当所有学生答案都已批阅完成时，将考试状态更新为 EVALUATED
     * 
     * @param examId 考试ID
     * @return 是否更新了状态
     */
    public boolean checkAndUpdateExamToEvaluated(Long examId) {
        try {
            Exam exam = examRepository.findById(examId).orElse(null);
            if (exam == null) {
                return false;
            }
            
            // 如果考试已经是 EVALUATED 状态，不需要更新
            if (exam.getStatus() == ExamStatus.EVALUATED) {
                return false;
            }
            
            // 获取考试的总答案数量
            long totalAnswers = studentAnswerService.getAnswerCountByExamId(examId);
            
            // 如果没有答案，不需要更新状态
            if (totalAnswers == 0) {
                return false;
            }
            
            // 获取已评阅的答案数量
            long evaluatedAnswers = studentAnswerService.getEvaluatedAnswerCountByExamId(examId);
            
            // 如果所有答案都已评阅，更新考试状态为 EVALUATED
            if (evaluatedAnswers >= totalAnswers) {
                exam.setStatus(ExamStatus.EVALUATED);
                examRepository.save(exam);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("检查并更新考试状态时发生错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 批量检查并更新多个考试的状态为 EVALUATED
     * 
     * @param examIds 考试ID集合
     * @return 成功更新的考试数量
     */
    public int checkAndUpdateExamsToEvaluated(Set<Long> examIds) {
        int updatedCount = 0;
        for (Long examId : examIds) {
            if (checkAndUpdateExamToEvaluated(examId)) {
                updatedCount++;
            }
        }
        return updatedCount;
    }
}
