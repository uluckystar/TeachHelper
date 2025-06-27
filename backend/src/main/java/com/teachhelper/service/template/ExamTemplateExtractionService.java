package com.teachhelper.service.template;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teachhelper.dto.request.StudentAnswerImportData;
import com.teachhelper.dto.response.ExamTemplateResponse;
import com.teachhelper.entity.ExamTemplate;
import com.teachhelper.entity.ExamTemplateQuestion;
import com.teachhelper.entity.User;
import com.teachhelper.repository.ExamTemplateRepository;
import com.teachhelper.repository.ExamTemplateQuestionRepository;
import com.teachhelper.service.answer.LearningAnswerParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamTemplateExtractionService {
    
    private static final Logger log = LoggerFactory.getLogger(ExamTemplateExtractionService.class);

    @Autowired
    private LearningAnswerParserService learningAnswerParserService;
    
    @Autowired
    private ExamTemplateRepository examTemplateRepository;
    
    @Autowired
    private ExamTemplateQuestionRepository examTemplateQuestionRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private com.teachhelper.service.answer.QuestionScoreParsingService questionScoreParsingService;
    
    @Value("${app.upload.dir:/uploads}")
    private String uploadDir;
    
    /**
     * 从学习通文档中提取试卷模板
     */
    @Transactional
    public ExamTemplateResponse extractTemplateFromLearningDocuments(
            String subject, 
            List<String> classFolders, 
            User createdBy) {
        
        log.info("开始从学习通文档提取试卷模板，科目: {}, 班级: {}", subject, classFolders);
        
        // 收集所有文档
        List<File> allDocuments = collectDocuments(subject, classFolders);
        
        // 解析文档并提取题目结构
        Map<Integer, QuestionTemplate> questionMap = new HashMap<>();
        List<String> sourceFiles = new ArrayList<>();
        List<String> parseErrors = new ArrayList<>();
        
        int successCount = 0;
        int failCount = 0;
        
        for (File document : allDocuments) {
            try {
                StudentAnswerImportData importData = learningAnswerParserService.parseLearningAnswerDocument(document);
                if (importData != null && importData.getAnswers() != null && !importData.getAnswers().isEmpty()) {
                    mergeQuestionsFromDocument(questionMap, importData, document.getName());
                    sourceFiles.add(document.getName());
                    successCount++;
                } else {
                    log.warn("文档解析为空: {}", document.getName());
                    parseErrors.add("文档解析为空: " + document.getName());
                    failCount++;
                }
            } catch (Exception e) {
                log.error("解析文档失败: {}", document.getName(), e);
                parseErrors.add("解析失败: " + document.getName() + " - " + e.getMessage());
                failCount++;
            }
        }
        
        // 创建模板
        ExamTemplate template = createTemplate(subject, questionMap, sourceFiles, parseErrors, createdBy);
        
        // 保存模板
        template = examTemplateRepository.save(template);
        
        // 保存题目
        List<ExamTemplateQuestion> templateQuestions = createTemplateQuestions(template, questionMap);
        examTemplateQuestionRepository.saveAll(templateQuestions);
        
        // 更新统计信息
        template.setTotalQuestions(questionMap.size());
        template.setMatchedQuestions(0); // 初始状态都未匹配
        examTemplateRepository.save(template);
        
        log.info("模板提取完成，总题目数: {}, 成功解析: {}, 失败: {}", 
                questionMap.size(), successCount, failCount);
        
        return convertToResponse(template);
    }
    
    /**
     * 收集所有相关文档
     */
    private List<File> collectDocuments(String subject, List<String> classFolders) {
        List<File> documents = new ArrayList<>();
        
        for (String classFolder : classFolders) {
            List<File> classDocuments = learningAnswerParserService.getAnswerDocuments(uploadDir, subject, classFolder);
            documents.addAll(classDocuments);
        }
        
        log.info("收集到 {} 个文档用于模板提取", documents.size());
        return documents;
    }
    
    /**
     * 从单个文档中合并题目到模板
     */
    private void mergeQuestionsFromDocument(Map<Integer, QuestionTemplate> questionMap, 
                                          StudentAnswerImportData importData, 
                                          String documentName) {
        
        // 清理分数解析上下文，为新文档做准备
        questionScoreParsingService.clearScoreContext();
        
        for (StudentAnswerImportData.QuestionAnswer qa : importData.getAnswers()) {
            Integer questionNumber = qa.getQuestionNumber();
            if (questionNumber == null || questionNumber <= 0) {
                continue;
            }
            
            QuestionTemplate existing = questionMap.get(questionNumber);
            if (existing == null) {
                // 创建新题目模板
                QuestionTemplate template = new QuestionTemplate();
                template.questionNumber = questionNumber;
                template.questionContent = qa.getQuestionContent();
                template.sectionHeader = qa.getSectionHeader();
                
                // 使用智能解析服务
                template.questionType = questionScoreParsingService.detectQuestionType(qa.getQuestionContent(), qa.getSectionHeader());
                template.score = questionScoreParsingService.parseQuestionScore(qa.getQuestionContent(), qa.getSectionHeader());
                template.options = extractQuestionOptions(qa.getQuestionContent(), template.questionType);
                template.correctAnswer = extractCorrectAnswer(qa.getQuestionContent(), qa.getAnswerContent());
                
                template.sourceDocuments = new ArrayList<>();
                template.sourceDocuments.add(documentName);
                template.sampleAnswers = new ArrayList<>();
                template.sampleAnswers.add(qa.getAnswerContent());
                
                questionMap.put(questionNumber, template);
                
                log.debug("创建新题目模板: 题号={}, 类型={}, 分数={}", 
                        questionNumber, template.questionType, template.score);
            } else {
                // 合并已存在的题目模板
                mergeQuestionTemplate(existing, qa, documentName);
            }
        }
    }
    
    /**
     * 合并题目模板信息
     */
    private void mergeQuestionTemplate(QuestionTemplate template, 
                                     StudentAnswerImportData.QuestionAnswer qa, 
                                     String documentName) {
        
        // 添加来源文档
        if (!template.sourceDocuments.contains(documentName)) {
            template.sourceDocuments.add(documentName);
        }
        
        // 添加示例答案
        if (qa.getAnswerContent() != null && !qa.getAnswerContent().trim().isEmpty()) {
            template.sampleAnswers.add(qa.getAnswerContent());
        }
        
        // 更新题目内容（如果更完整）
        boolean contentUpdated = false;
        if (qa.getQuestionContent() != null && 
            qa.getQuestionContent().length() > template.questionContent.length()) {
            template.questionContent = qa.getQuestionContent();
            contentUpdated = true;
        }
        
        // 如果内容更新了，重新解析题目信息
        if (contentUpdated) {
            // 使用当前段落标题（优先使用已存在的，如果没有则使用新的）
            String sectionHeader = template.sectionHeader != null ? template.sectionHeader : qa.getSectionHeader();
            template.questionType = questionScoreParsingService.detectQuestionType(qa.getQuestionContent(), sectionHeader);
            template.options = extractQuestionOptions(qa.getQuestionContent(), template.questionType);
            template.correctAnswer = extractCorrectAnswer(qa.getQuestionContent(), qa.getAnswerContent());
        }
        
        // 更新分数（取更合理的分数）
        java.math.BigDecimal newScore = questionScoreParsingService.parseQuestionScore(qa.getQuestionContent(), qa.getSectionHeader());
        if (newScore != null && (template.score == null || newScore.compareTo(template.score) > 0)) {
            template.score = newScore;
        }
        
        // 合并选项（保留更完整的选项）
        List<String> newOptions = extractQuestionOptions(qa.getQuestionContent(), template.questionType);
        if (newOptions.size() > (template.options != null ? template.options.size() : 0)) {
            template.options = newOptions;
        }
        
        // 更新段落标题（优先保留已有的段落标题，避免被null覆盖）
        boolean sectionHeaderUpdated = false;
        if (qa.getSectionHeader() != null && !qa.getSectionHeader().trim().isEmpty()) {
            if (template.sectionHeader == null || template.sectionHeader.trim().isEmpty()) {
                template.sectionHeader = qa.getSectionHeader();
                sectionHeaderUpdated = true;
                log.debug("更新题目{}的段落标题: {}", template.questionNumber, template.sectionHeader);
            }
        }
        
        // 如果段落标题更新了，重新解析题目类型
        if (sectionHeaderUpdated && !contentUpdated) {
            String oldType = template.questionType;
            template.questionType = questionScoreParsingService.detectQuestionType(template.questionContent, template.sectionHeader);
            if (!oldType.equals(template.questionType)) {
                log.debug("题目{}类型因段落标题更新而改变: {} -> {}", template.questionNumber, oldType, template.questionType);
                // 重新提取选项（如果类型变为选择题）
                template.options = extractQuestionOptions(template.questionContent, template.questionType);
            }
        }
        
        // 增加出现频次
        template.frequency++;
        
        log.debug("合并题目模板: 题号={}, 频次={}, 分数={}, 选项数={}", 
                template.questionNumber, template.frequency, template.score, 
                template.options != null ? template.options.size() : 0);
    }
    
    /**
     * 提取题目选项（主要针对选择题）
     */
    private List<String> extractQuestionOptions(String questionContent, String questionType) {
        List<String> options = new ArrayList<>();
        
        if (questionContent == null || !"选择题".equals(questionType)) {
            return options;
        }
        
        // 匹配选择题选项的各种格式
        java.util.regex.Pattern[] patterns = {
            java.util.regex.Pattern.compile("([A-D])[.、]\\s*([^\\n]+?)(?=[A-D][.、]|$)", java.util.regex.Pattern.MULTILINE),
            java.util.regex.Pattern.compile("([A-D])\\)\\s*([^\\n]+?)(?=[A-D]\\)|$)", java.util.regex.Pattern.MULTILINE),
            java.util.regex.Pattern.compile("([A-D])：\\s*([^\\n]+?)(?=[A-D]：|$)", java.util.regex.Pattern.MULTILINE)
        };
        
        for (java.util.regex.Pattern pattern : patterns) {
            java.util.regex.Matcher matcher = pattern.matcher(questionContent);
            while (matcher.find()) {
                String optionLetter = matcher.group(1);
                String optionContent = matcher.group(2).trim();
                if (!optionContent.isEmpty()) {
                    options.add(optionLetter + ". " + optionContent);
                }
            }
            if (!options.isEmpty()) {
                break; // 找到选项就停止
            }
        }
        
        log.debug("提取到{}个选项: {}", options.size(), options);
        return options;
    }
    
    /**
     * 提取正确答案
     */
    private String extractCorrectAnswer(String questionContent, String answerContent) {
        if (answerContent == null || answerContent.trim().isEmpty()) {
            return null;
        }
        
        String answer = answerContent.trim();
        
        // 清理答案内容
        answer = answer.replaceAll("学生答案[：:]?\\s*", "");
        answer = answer.replaceAll("我的答案[：:]?\\s*", "");
        answer = answer.replaceAll("答案[：:]?\\s*", "");
        
        // 尝试从答案中提取正确答案
        if (answer.contains("正确答案：")) {
            String[] parts = answer.split("正确答案：");
            if (parts.length > 1) {
                String correctAnswer = parts[1].trim();
                // 对于选择题，只取选项字母
                if (questionContent != null && questionContent.toLowerCase().contains("选择")) {
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^[A-D]");
                    java.util.regex.Matcher matcher = pattern.matcher(correctAnswer);
                    if (matcher.find()) {
                        return matcher.group();
                    }
                }
                // 对于判断题，标准化答案
                if (questionContent != null && questionContent.toLowerCase().contains("判断")) {
                    if (correctAnswer.contains("√") || correctAnswer.toLowerCase().contains("正确") || correctAnswer.toLowerCase().contains("true")) {
                        return "√";
                    } else if (correctAnswer.contains("×") || correctAnswer.toLowerCase().contains("错误") || correctAnswer.toLowerCase().contains("false")) {
                        return "×";
                    }
                }
                // 对于主观题，保留完整答案但限制长度
                return correctAnswer.length() > 200 ? correctAnswer.substring(0, 197) + "..." : correctAnswer;
            }
        }
        
        // 如果没有明确的"正确答案："标记，对选择题尝试提取选项字母
        if (questionContent != null && questionContent.toLowerCase().contains("选择")) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[A-D]");
            java.util.regex.Matcher matcher = pattern.matcher(answer);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        
        // 对于判断题，从学生答案中推断
        if (questionContent != null && questionContent.toLowerCase().contains("判断")) {
            if (answer.contains("√") || answer.toLowerCase().contains("正确")) {
                return "√";
            } else if (answer.contains("×") || answer.toLowerCase().contains("错误")) {
                return "×";
            }
        }
        
        // 返回清理后的答案内容（为主观题保留学生答案作为参考）
        return answer.length() > 100 ? answer.substring(0, 97) + "..." : answer;
    }
    
    /**
     * 创建模板
     */
    private ExamTemplate createTemplate(String subject, 
                                      Map<Integer, QuestionTemplate> questionMap,
                                      List<String> sourceFiles,
                                      List<String> parseErrors,
                                      User createdBy) {
        
        ExamTemplate template = new ExamTemplate();
        
        // 基本信息
        template.setTemplateName(generateTemplateName(subject, sourceFiles));
        template.setSubject(subject);
        template.setExamTitle(extractExamTitle(sourceFiles));
        template.setDescription(generateDescription(questionMap, sourceFiles, parseErrors));
        template.setStatus(ExamTemplate.TemplateStatus.DRAFT);
        template.setCreatedBy(createdBy);
        
        // 元数据
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("sourceFiles", sourceFiles);
            metadata.put("parseErrors", parseErrors);
            metadata.put("extractionTime", new Date());
            metadata.put("questionTypes", analyzeQuestionTypes(questionMap));
            template.setParseMetadata(objectMapper.writeValueAsString(metadata));
            template.setSourceFiles(objectMapper.writeValueAsString(sourceFiles));
        } catch (Exception e) {
            log.error("序列化元数据失败", e);
        }
        
        return template;
    }
    
    /**
     * 生成模板名称
     */
    private String generateTemplateName(String subject, List<String> sourceFiles) {
        String timestamp = new Date().toString().substring(4, 19);
        return String.format("%s-模板-%s", subject, timestamp);
    }
    
    /**
     * 提取考试标题
     */
    private String extractExamTitle(List<String> sourceFiles) {
        if (sourceFiles.isEmpty()) {
            return "未知考试";
        }
        
        String firstFile = sourceFiles.get(0);
        // 从文件名中提取考试信息
        if (firstFile.contains("期末考试")) {
            return firstFile.substring(firstFile.indexOf("期末考试") - 20, firstFile.indexOf("期末考试") + 4);
        } else if (firstFile.contains("期中考试")) {
            return firstFile.substring(firstFile.indexOf("期中考试") - 20, firstFile.indexOf("期中考试") + 4);
        } else if (firstFile.contains("考试")) {
            return firstFile.substring(0, Math.min(firstFile.length(), 30));
        }
        
        return "考试试卷";
    }
    
    /**
     * 生成描述
     */
    private String generateDescription(Map<Integer, QuestionTemplate> questionMap,
                                     List<String> sourceFiles,
                                     List<String> parseErrors) {
        StringBuilder desc = new StringBuilder();
        
        desc.append(String.format("从 %d 个学习通文档中提取的试卷模板\n", sourceFiles.size()));
        desc.append(String.format("提取到 %d 道题目\n", questionMap.size()));
        
        if (!parseErrors.isEmpty()) {
            desc.append(String.format("解析失败 %d 个文档\n", parseErrors.size()));
        }
        
        // 题型分布
        Map<String, Long> typeCount = questionMap.values().stream()
            .collect(Collectors.groupingBy(q -> q.questionType, Collectors.counting()));
        
        desc.append("\n题型分布:\n");
        typeCount.forEach((type, count) -> 
            desc.append(String.format("- %s: %d 题\n", type, count)));
        
        return desc.toString();
    }
    
    /**
     * 分析题目类型分布
     */
    private Map<String, Integer> analyzeQuestionTypes(Map<Integer, QuestionTemplate> questionMap) {
        return questionMap.values().stream()
            .collect(Collectors.groupingBy(
                q -> q.questionType, 
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
    }
    
    /**
     * 创建模板题目列表
     */
    private List<ExamTemplateQuestion> createTemplateQuestions(ExamTemplate template, 
                                                             Map<Integer, QuestionTemplate> questionMap) {
        
        List<ExamTemplateQuestion> templateQuestions = new ArrayList<>();
        
        for (QuestionTemplate qt : questionMap.values()) {
            ExamTemplateQuestion etq = new ExamTemplateQuestion();
            
            etq.setExamTemplate(template);
            etq.setQuestionNumber(qt.questionNumber);
            etq.setQuestionContent(qt.questionContent);
            etq.setSectionHeader(qt.sectionHeader);
            etq.setQuestionType(qt.questionType);
            etq.setIsConfirmed(false);  // 使用确认状态而不是匹配状态
            etq.setHasIssues(false);
            etq.setSourceDocument(String.join(", ", qt.sourceDocuments.subList(0, Math.min(3, qt.sourceDocuments.size()))));
            
            // 设置题目分数
            if (qt.score != null) {
                etq.setScore(qt.score.doubleValue());
            } else {
                // 使用智能默认分数
                java.math.BigDecimal defaultScore = questionScoreParsingService.getIntelligentDefaultScore(qt.questionContent, qt.sectionHeader);
                etq.setScore(defaultScore.doubleValue());
            }
            
            // 设置选择题选项
            if (qt.options != null && !qt.options.isEmpty()) {
                try {
                    etq.setOptions(objectMapper.writeValueAsString(qt.options));
                } catch (Exception e) {
                    log.error("序列化选项失败", e);
                }
            }
            
            // 设置正确答案
            if (qt.correctAnswer != null) {
                etq.setCorrectAnswer(qt.correctAnswer);
            }
            
            // 设置简化的说明信息
            StringBuilder explanation = new StringBuilder();
            
            // 如果有正确答案，显示正确答案
            if (qt.correctAnswer != null && !qt.correctAnswer.trim().isEmpty()) {
                explanation.append("参考答案: ").append(qt.correctAnswer);
            }
            
            // 对于主观题，可以添加一个示例答案
            if (!"选择题".equals(qt.questionType) && !"判断题".equals(qt.questionType) && !qt.sampleAnswers.isEmpty()) {
                if (explanation.length() > 0) {
                    explanation.append("\n");
                }
                explanation.append("学生答案示例: ").append(qt.sampleAnswers.get(0));
                if (qt.sampleAnswers.get(0).length() > 100) {
                    explanation.setLength(explanation.length() - qt.sampleAnswers.get(0).length());
                    explanation.append(qt.sampleAnswers.get(0).substring(0, 97)).append("...");
                }
            }
            
            if (explanation.length() > 0) {
                etq.setExplanation(explanation.toString());
            }
            
            // 设置验证信息
            List<String> issues = validateQuestion(qt);
            if (!issues.isEmpty()) {
                etq.setHasIssues(true);
                try {
                    etq.setIssues(objectMapper.writeValueAsString(issues));
                } catch (Exception e) {
                    log.error("序列化问题列表失败", e);
                }
            }
            
            templateQuestions.add(etq);
        }
        
        return templateQuestions;
    }
    
    /**
     * 验证题目质量
     */
    private List<String> validateQuestion(QuestionTemplate qt) {
        List<String> issues = new ArrayList<>();
        
        if (qt.questionContent == null || qt.questionContent.trim().length() < 10) {
            issues.add("题目内容过短，可能不完整");
        }
        
        if (qt.frequency < 2) {
            issues.add("该题目只在1个文档中出现，可能是个别学生的特殊情况");
        }
        
        if (qt.sampleAnswers.isEmpty()) {
            issues.add("没有找到学生答案示例");
        }
        
        if (qt.questionType.equals("未知")) {
            issues.add("无法确定题目类型");
        }
        
        return issues;
    }
    
    /**
     * 转换为响应对象
     */
    private ExamTemplateResponse convertToResponse(ExamTemplate template) {
        ExamTemplateResponse response = new ExamTemplateResponse();
        
        response.setId(template.getId());
        response.setTemplateName(template.getTemplateName());
        response.setSubject(template.getSubject());
        response.setExamTitle(template.getExamTitle());
        response.setDescription(template.getDescription());
        response.setTotalQuestions(template.getTotalQuestions());
        response.setMatchedQuestions(template.getMatchedQuestions());
        response.setStatus(template.getStatus().name());
        response.setCreatedTime(template.getCreatedAt());
        response.setUpdatedTime(template.getUpdatedAt());
        
        return response;
    }
    
    /**
     * 内部题目模板类
     */
    private static class QuestionTemplate {
        Integer questionNumber;
        String questionContent;
        String sectionHeader;
        String questionType;
        java.math.BigDecimal score;              // 题目分数
        List<String> options;                    // 选择题选项
        String correctAnswer;                    // 正确答案
        List<String> sourceDocuments;
        List<String> sampleAnswers;
        int frequency = 1;
    }
} 