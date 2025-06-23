package com.teachhelper.service.exam;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachhelper.entity.Exam;
import com.teachhelper.entity.ExamStatus;
import com.teachhelper.repository.ExamRepository;

@Service
@Transactional
public class ExamSchedulerService {
    
    @Autowired
    private ExamRepository examRepository;
    
    /**
     * 每分钟检查一次是否有考试需要自动结束
     */
    @Scheduled(fixedRate = 60000) // 每60秒执行一次
    public void autoEndExpiredExams() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // 查找所有已发布但结束时间已过的考试
            List<Exam> expiredExams = examRepository.findByStatusAndEndTimeBefore(
                ExamStatus.PUBLISHED, now
            );
            
            for (Exam exam : expiredExams) {
                System.out.println("自动结束过期考试: " + exam.getTitle() + " (ID: " + exam.getId() + ")");
                exam.setStatus(ExamStatus.ENDED);
                examRepository.save(exam);
            }
            
            if (!expiredExams.isEmpty()) {
                System.out.println("自动结束了 " + expiredExams.size() + " 个过期考试");
            }
            
        } catch (Exception e) {
            System.err.println("自动结束考试任务执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 每小时检查一次即将到期的考试（用于日志记录）
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void checkExamsNearingEnd() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oneHourLater = now.plusHours(1);
            
            // 查找1小时内将要结束的考试
            List<Exam> soonEndingExams = examRepository.findByStatusAndEndTimeBetween(
                ExamStatus.PUBLISHED, now, oneHourLater
            );
            
            for (Exam exam : soonEndingExams) {
                System.out.println("提醒：考试 \"" + exam.getTitle() + "\" 将在1小时内结束，结束时间: " + exam.getEndTime());
            }
            
        } catch (Exception e) {
            System.err.println("检查即将结束考试任务执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
