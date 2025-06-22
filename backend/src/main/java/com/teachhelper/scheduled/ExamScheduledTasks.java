package com.teachhelper.scheduled;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.teachhelper.service.exam.ExamService;

/**
 * 考试相关的定时任务
 */
@Component
public class ExamScheduledTasks {
    
    @Autowired
    private ExamService examService;
    
    /**
     * 每分钟检查一次是否有考试需要自动结束
     */
    @Scheduled(fixedRate = 60000) // 每60秒执行一次
    public void autoEndExpiredExams() {
        try {
            System.out.println("=== 定时任务执行 - 检查过期考试 ===");
            int endedCount = examService.autoEndExpiredExams();
            if (endedCount > 0) {
                System.out.println("自动结束了 " + endedCount + " 个考试");
            } else {
                System.out.println("没有需要自动结束的考试");
            }
        } catch (Exception e) {
            System.err.println("自动结束考试任务执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
