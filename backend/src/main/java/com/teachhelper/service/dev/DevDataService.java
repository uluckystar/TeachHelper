package com.teachhelper.service.dev;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachhelper.repository.AIUsageStatsRepository;
import com.teachhelper.repository.ExamRepository;
import com.teachhelper.repository.KnowledgeBaseRepository;
import com.teachhelper.repository.PaperGenerationHistoryRepository;
import com.teachhelper.repository.PaperGenerationTemplateRepository;
import com.teachhelper.repository.QuestionRepository;
import com.teachhelper.repository.RubricCriterionRepository;
import com.teachhelper.repository.StudentAnswerRepository;
import com.teachhelper.repository.StudentRepository;
import com.teachhelper.repository.SystemRubricRepository;
import com.teachhelper.repository.UserAIConfigRepository;
import com.teachhelper.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 开发环境主数据服务
 * 统一管理所有开发阶段的示例数据生成
 */
@Service
@Slf4j
public class DevDataService {
    
    @Autowired
    private DevUserDataService devUserDataService;
    
    @Autowired
    private DevQuestionDataService devQuestionDataService;
    
    @Autowired
    private DevAnswerDataService devAnswerDataService;
    
    @Autowired
    private DevAIDataService devAIDataService;
    
    @Autowired
    private DevPaperDataService devPaperDataService;
    
    @Autowired
    private DevKnowledgeDataService devKnowledgeDataService;
    
    @Autowired
    private DevRubricDataService devRubricDataService;
    
    @Autowired
    private DevExamDataService devExamDataService;
    
    // Repository dependencies for statistics and cleanup
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private ExamRepository examRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private RubricCriterionRepository rubricCriterionRepository;
    
    @Autowired
    private StudentAnswerRepository studentAnswerRepository;
    
    @Autowired
    private UserAIConfigRepository userAIConfigRepository;
    
    @Autowired
    private AIUsageStatsRepository aiUsageStatsRepository;
    
    @Autowired
    private PaperGenerationTemplateRepository paperTemplateRepository;
    
    @Autowired
    private PaperGenerationHistoryRepository paperHistoryRepository;
    
    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;
    
    @Autowired
    private SystemRubricRepository systemRubricRepository;
    
    /**
     * 生成示例数据（检查是否已存在）
     */
    @Transactional
    public void generateSampleData() {
        // 检查是否已有数据
        if (userRepository.count() > 0) {
            log.warn("⚠️ 数据库中已存在数据，将强制重新生成");
        }
        
        generateSampleDataForce();
    }
    
    /**
     * 强制生成示例数据（清空后重新生成）
     */
    @Transactional
    public void generateSampleDataForce() {
        try {
            log.info("🚀 开始生成TeachHelper系统完整示例数据...");
            
            // 1. 清空所有数据
            clearAllData();
            
            // 2. 按依赖关系依次生成数据
            // 首先生成用户和学生数据（基础数据）
            log.info("📋 Step 1/9: 生成用户和学生数据...");
            devUserDataService.createUsers();
            devUserDataService.createStudents();
            
            // 生成考试数据
            log.info("📝 Step 2/9: 生成考试数据...");
            devExamDataService.createExams();
            
            // 生成题目和评分标准数据
            log.info("❓ Step 3/9: 生成题目和评分标准数据...");
            devQuestionDataService.createQuestionsAndRubrics();
            
            // 生成学生答案数据
            log.info("📝 Step 4/9: 生成学生答案数据...");
            devAnswerDataService.createStudentAnswers();
            
            // 生成AI配置和使用统计数据
            log.info("🤖 Step 5/9: 生成AI配置和使用统计数据...");
            devAIDataService.createAIConfigs();
            devAIDataService.createAIUsageStats();
            
            // 生成试卷模板和生成历史数据
            log.info("📋 Step 6/9: 生成试卷模板和生成历史数据...");
            devPaperDataService.createPaperTemplates();
            
            // 生成知识库数据（包含向量化处理）
            log.info("📚 Step 7/9: 生成知识库数据（含向量存储）...");
            devKnowledgeDataService.createKnowledgeBases();
            
            // 生成系统评分标准数据
            log.info("📊 Step 8/9: 生成系统评分标准数据...");
            devRubricDataService.createSystemRubrics();
            
            // 生成任务管理数据
            log.info("⚙️ Step 9/9: 任务管理数据已禁用");
            // taskDataInitializer 已禁用，不再生成示例任务数据
            
            log.info("🎉 示例数据生成完成！");
            printDataSummary();
            
        } catch (Exception e) {
            log.error("❌ 生成示例数据时出错：{}", e.getMessage(), e);
            throw new RuntimeException("生成示例数据失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 清空所有数据
     */
    @Transactional
    public void clearAllData() {
        log.info("🗑️ 开始清空所有数据...");
        
        try {
            // 按依赖关系逆序清理 - 使用更严格的顺序
            log.info("任务管理数据清理已禁用");
            // taskDataInitializer 已禁用，不再清理示例任务数据
            
            log.info("清理AI使用统计...");
            try {
                aiUsageStatsRepository.deleteAll();
                aiUsageStatsRepository.flush();
            } catch (Exception e) {
                log.warn("清理AI使用统计失败: {}", e.getMessage());
            }
            
            log.info("清理AI配置...");
            try {
                userAIConfigRepository.deleteAll();
                userAIConfigRepository.flush();
            } catch (Exception e) {
                log.warn("清理AI配置失败: {}", e.getMessage());
            }
            
            log.info("清理学生答案...");
            try {
                studentAnswerRepository.deleteAll();
                studentAnswerRepository.flush();
            } catch (Exception e) {
                log.warn("清理学生答案失败: {}", e.getMessage());
            }
            
            log.info("清理评分准则...");
            try {
                rubricCriterionRepository.deleteAll();
                rubricCriterionRepository.flush();
            } catch (Exception e) {
                log.warn("清理评分准则失败: {}", e.getMessage());
            }
            
            log.info("清理题目...");
            try {
                questionRepository.deleteAll();
                questionRepository.flush();
            } catch (Exception e) {
                log.warn("清理题目失败: {}", e.getMessage());
            }
            
            log.info("清理系统评分标准...");
            try {
                systemRubricRepository.deleteAll();
                systemRubricRepository.flush();
            } catch (Exception e) {
                log.warn("清理系统评分标准失败: {}", e.getMessage());
            }
            
            log.info("清理考试...");
            try {
                examRepository.deleteAll();
                examRepository.flush();
            } catch (Exception e) {
                log.warn("清理考试失败: {}", e.getMessage());
            }
            
            log.info("清理试卷生成历史...");
            try {
                paperHistoryRepository.deleteAll();
                paperHistoryRepository.flush();
            } catch (Exception e) {
                log.warn("清理试卷生成历史失败: {}", e.getMessage());
            }
            
            log.info("清理试卷模板...");
            try {
                paperTemplateRepository.deleteAll();
                paperTemplateRepository.flush();
            } catch (Exception e) {
                log.warn("清理试卷模板失败: {}", e.getMessage());
            }
            
            log.info("清理知识库数据（含向量存储）...");
            try {
                devKnowledgeDataService.clearKnowledgeData();
            } catch (Exception e) {
                log.warn("清理知识库数据失败: {}", e.getMessage());
            }
            
            log.info("清理学生档案...");
            try {
                studentRepository.deleteAll();
                studentRepository.flush();
            } catch (Exception e) {
                log.warn("清理学生档案失败: {}", e.getMessage());
            }
            
            log.info("清理用户角色和用户...");
            try {
                devUserDataService.clearUserData();
            } catch (Exception e) {
                log.warn("清理用户数据失败: {}", e.getMessage());
            }
            
            // 最终验证清理结果
            log.info("数据清理验证：");
            log.info("- 用户数量: {}", userRepository.count());
            log.info("- 学生数量: {}", studentRepository.count());
            log.info("- 考试数量: {}", examRepository.count());
            log.info("- 题目数量: {}", questionRepository.count());
            
            log.info("✅ 数据清理完成");
            
        } catch (Exception e) {
            log.error("❌ 清理数据时出错：{}", e.getMessage(), e);
            // 继续执行，不中断流程
        }
    }
    
    /**
     * 打印数据汇总信息
     */
    private void printDataSummary() {
        log.info("\n=== TeachHelper 示例数据汇总 ===");
        log.info("👥 用户总数: {}", userRepository.count());
        log.info("🎓 学生档案: {}", studentRepository.count());
        log.info("📝 考试数量: {}", examRepository.count());
        log.info("❓ 题目总数: {}", questionRepository.count());
        log.info("✍️ 学生答案: {}", studentAnswerRepository.count());
        log.info("🤖 AI配置: {}", userAIConfigRepository.count());
        log.info("📊 AI使用统计: {}", aiUsageStatsRepository.count());
        log.info("📄 试卷模板: {}", paperTemplateRepository.count());
        log.info("📋 生成历史: {}", paperHistoryRepository.count());
        log.info("📖 知识库: {}", knowledgeBaseRepository.count());
        log.info("⭐ 评分标准: {}", systemRubricRepository.count());
        log.info("🎯 评分准则: {}", rubricCriterionRepository.count());
        log.info("================================");
    }
    
    /**
     * 获取数据统计信息
     */
    public String getDataStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("📊 TeachHelper 数据库统计\n\n");
        
        stats.append("👥 用户管理:\n");
        stats.append("  • 用户总数: ").append(userRepository.count()).append("\n");
        stats.append("  • 学生档案: ").append(studentRepository.count()).append("\n\n");
        
        stats.append("📚 教学考试:\n");
        stats.append("  • 考试数量: ").append(examRepository.count()).append("\n");
        stats.append("  • 题目总数: ").append(questionRepository.count()).append("\n");
        stats.append("  • 学生答案: ").append(studentAnswerRepository.count()).append("\n\n");
        
        stats.append("🤖 AI智能系统:\n");
        stats.append("  • AI配置: ").append(userAIConfigRepository.count()).append("\n");
        stats.append("  • 使用统计: ").append(aiUsageStatsRepository.count()).append("\n\n");
        
        stats.append("📄 试卷生成:\n");
        stats.append("  • 试卷模板: ").append(paperTemplateRepository.count()).append("\n");
        stats.append("  • 生成历史: ").append(paperHistoryRepository.count()).append("\n\n");
        
        stats.append("📖 知识管理:\n");
        stats.append("  • 知识库: ").append(knowledgeBaseRepository.count()).append("\n\n");
        
        stats.append("⭐ 评分系统:\n");
        stats.append("  • 评分标准: ").append(systemRubricRepository.count()).append("\n");
        stats.append("  • 评分准则: ").append(rubricCriterionRepository.count()).append("\n\n");
        
        return stats.toString();
    }
}
