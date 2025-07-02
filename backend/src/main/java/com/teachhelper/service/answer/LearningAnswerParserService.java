package com.teachhelper.service.answer;

import com.teachhelper.dto.request.StudentAnswerImportData;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LearningAnswerParserService {

    private static final Logger log = LoggerFactory.getLogger(LearningAnswerParserService.class);

    @Autowired
    private LibreOfficeConverterService libreOfficeConverterService;

    @Autowired
    private HtmlContentExtractor htmlContentExtractor;
    
    @Autowired
    private OCRService ocrService;

    /**
     * 解析学习通答案文档
     * @param file 文档文件
     * @return 解析后的学生答案数据
     */
    public StudentAnswerImportData parseLearningAnswerDocument(File file) throws IOException {
        log.info("🚀 开始解析学习通答案文档: {}", file.getName());
        
        String content = extractTextFromDocument(file);
        if (content == null || content.trim().isEmpty()) {
            log.warn("⚠️ 无法提取文档内容或内容为空: {}", file.getName());
            return null;
        }
        
        log.info("📄 成功提取文档内容，长度: {} 字符", content.length());
        
        return parseAnswerContent(content, file.getName());
    }

    /**
     * 从学习通考试文档中提取考试模板信息
     * @param file 文档文件
     * @return 解析后的考试模板数据
     */
    public ExamTemplateData parseExamTemplate(File file) throws IOException {
        log.info("🔍 开始解析学习通考试模板: {}", file.getName());
        
        String content = extractTextFromDocument(file);
        
        // 如果内容为null，表示无法解析该文件
        if (content == null) {
            log.warn("跳过无法解析的文档: {}", file.getName());
            return null;
        }
        
        return parseExamTemplateContent(content, file.getName());
    }

    /**
     * 解析考试模板内容
     */
    private ExamTemplateData parseExamTemplateContent(String content, String fileName) {
        log.info("📚 开始解析考试模板内容，文件: {}", fileName);
        
        ExamTemplateData templateData = new ExamTemplateData();
        
        // 清理内容
        String cleanedContent = cleanDocumentContent(content);
        
        // 1. 从文件名或内容中提取考试标题
        parseExamTitleFromContent(cleanedContent, fileName, templateData);
        
        // 2. 解析大题结构
        List<ExamTemplateData.SectionInfo> sections = parseTemplateSections(cleanedContent);
        templateData.setSections(sections);
        
        // 3. 统计总体信息
        calculateTemplateSummary(templateData);
        
        log.info("✅ 考试模板解析完成: 标题={}, 大题数={}, 总题数={}", 
                templateData.getExamTitle(), 
                sections.size(),
                sections.stream().mapToInt(s -> s.getQuestions() != null ? s.getQuestions().size() : 0).sum());
        
        return templateData;
    }

    /**
     * 从内容中解析考试标题
     */
    private void parseExamTitleFromContent(String content, String fileName, ExamTemplateData templateData) {
        // 从文件名提取考试标题 - 匹配《xxx》格式
        Pattern titlePattern = Pattern.compile("《([^》]+)》");
        Matcher titleMatcher = titlePattern.matcher(fileName);
        if (titleMatcher.find()) {
            templateData.setExamTitle(titleMatcher.group(1));
        } else {
            // 如果文件名中没有标题，使用文件名去除扩展名
            String title = fileName.replaceAll("\\.(doc|docx)$", "");
            templateData.setExamTitle(title);
        }
        
        // 从内容中提取学生信息作为样本
        Pattern namePattern = Pattern.compile("(?:答题人|姓名)\\s*[:：]\\s*([^\\s]+)");
        Matcher nameMatcher = namePattern.matcher(content);
        if (nameMatcher.find()) {
            templateData.setStudentName(nameMatcher.group(1).trim());
        }
        
        Pattern classPattern = Pattern.compile("班级\\s*[:：]\\s*([^\\s]+)");
        Matcher classMatcher = classPattern.matcher(content);
        if (classMatcher.find()) {
            templateData.setClassName(classMatcher.group(1).trim());
        }
    }

    /**
     * 解析大题部分
     */
    private List<ExamTemplateData.SectionInfo> parseTemplateSections(String content) {
        List<ExamTemplateData.SectionInfo> sections = new ArrayList<>();
        
        // 匹配大题标题：一.、二.、三. 等，格式：一.单选题(共25题,25分）
        Pattern sectionPattern = Pattern.compile("([一二三四五六七八九十]+)\\s*[.、]\\s*([^\\n]*?)\\s*(?=\\n|$)");
        Matcher sectionMatcher = sectionPattern.matcher(content);
        
        List<SectionMatch> sectionMatches = new ArrayList<>();
        while (sectionMatcher.find()) {
            String sectionNumber = sectionMatcher.group(1);
            String sectionTitle = sectionMatcher.group(2).trim();
            int startPos = sectionMatcher.start();
            
            // 检查是否包含题目数量和分数信息
            if (sectionTitle.contains("题") && (sectionTitle.contains("分") || sectionTitle.contains("共"))) {
                sectionMatches.add(new SectionMatch(sectionNumber, sectionTitle, startPos));
                log.debug("🔍 找到大题部分: {} - {}", sectionNumber, sectionTitle);
            }
        }
        
        // 为每个大题确定内容范围并解析
        for (int i = 0; i < sectionMatches.size(); i++) {
            SectionMatch current = sectionMatches.get(i);
            int endPos = (i + 1 < sectionMatches.size()) ? 
                sectionMatches.get(i + 1).startPos : content.length();
            
            String sectionContent = content.substring(current.startPos, endPos);
            ExamTemplateData.SectionInfo sectionInfo = parseSingleSection(current, sectionContent);
            
            if (sectionInfo != null) {
                sections.add(sectionInfo);
            }
        }
        
        return sections;
    }

    /**
     * 解析单个大题部分
     */
    private ExamTemplateData.SectionInfo parseSingleSection(SectionMatch sectionMatch, String sectionContent) {
        ExamTemplateData.SectionInfo sectionInfo = new ExamTemplateData.SectionInfo();
        sectionInfo.setSectionNumber(sectionMatch.sectionNumber);
        sectionInfo.setSectionTitle(sectionMatch.sectionTitle);
        
        // 解析题目类型和分数信息
        parseSectionTypeAndScore(sectionInfo);
        
        // 解析该部分的所有题目
        List<ExamTemplateData.QuestionInfo> questions = parseQuestionsInTemplateSection(sectionContent, sectionInfo);
        sectionInfo.setQuestions(questions);
        
        return sectionInfo;
    }

    /**
     * 解析大题的类型和分数信息
     */
    private void parseSectionTypeAndScore(ExamTemplateData.SectionInfo sectionInfo) {
        String title = sectionInfo.getSectionTitle();
        
        // 提取题目类型
        if (title.contains("选择题") || title.contains("单选")) {
            sectionInfo.setQuestionType("单选题");
        } else if (title.contains("多选")) {
            sectionInfo.setQuestionType("多选题");
        } else if (title.contains("判断题")) {
            sectionInfo.setQuestionType("判断题");
        } else if (title.contains("填空题")) {
            sectionInfo.setQuestionType("填空题");
        } else if (title.contains("简答题")) {
            sectionInfo.setQuestionType("简答题");
        } else if (title.contains("论述题")) {
            sectionInfo.setQuestionType("论述题");
        } else if (title.contains("计算题")) {
            sectionInfo.setQuestionType("计算题");
        } else if (title.contains("分析题")) {
            sectionInfo.setQuestionType("分析题");
        } else {
            sectionInfo.setQuestionType("其他");
        }
        
        // 匹配分数格式：共25题,25分 或 共 25 题 ,25 分
        Pattern scorePattern = Pattern.compile("共\\s*(\\d+)\\s*题\\s*[，,]\\s*(\\d+)\\s*分");
        Matcher scoreMatcher = scorePattern.matcher(title);
        log.debug("🔍 尝试解析大题分数，标题: {}", title);
        if (scoreMatcher.find()) {
            try {
                int questionCount = Integer.parseInt(scoreMatcher.group(1));
                int totalScore = Integer.parseInt(scoreMatcher.group(2));
                sectionInfo.setQuestionCount(questionCount);
                sectionInfo.setTotalScore(totalScore);
                if (questionCount > 0) {
                    sectionInfo.setScorePerQuestion((double) totalScore / questionCount);
                }
                log.debug("✅ 成功解析大题分数: {}题，{}分，每题{}分", 
                        questionCount, totalScore, sectionInfo.getScorePerQuestion());
            } catch (NumberFormatException e) {
                log.warn("❌ 解析大题分数失败: {}", title);
            }
        } else {
            log.warn("❌ 未找到分数模式，标题: {}", title);
        }
    }

    /**
     * 解析大题部分的题目 - 智能版本
     * 利用题目序号连续性和大题信息进行精确解析
     */
    private List<ExamTemplateData.QuestionInfo> parseQuestionsInTemplateSection(String sectionContent, 
                                                                               ExamTemplateData.SectionInfo sectionInfo) {
        List<ExamTemplateData.QuestionInfo> questions = new ArrayList<>();
        
        log.debug("🔍 开始解析大题 {} 的题目，预期题目数: {}", 
                sectionInfo.getSectionNumber(), sectionInfo.getQuestionCount());
        
        // 按行分割内容
        String[] lines = sectionContent.split("\\n");
        
        // 智能查找题目起始序号
        int expectedStartNumber = findExpectedStartNumber(lines, sectionInfo);
        int expectedEndNumber = calculateExpectedEndNumber(expectedStartNumber, sectionInfo);
        
        log.debug("📊 大题 {} 预期题目序号范围: {} - {}", 
                sectionInfo.getSectionNumber(), expectedStartNumber, expectedEndNumber);
        
        // 使用智能解析方法
        questions = parseQuestionsWithSequentialValidation(lines, expectedStartNumber, expectedEndNumber, sectionInfo);
        
        log.info("📝 大题 {} 解析完成: 预期{}题，实际解析{}题", 
                sectionInfo.getSectionNumber(), sectionInfo.getQuestionCount(), questions.size());
        
        return questions;
    }

    /**
     * 查找预期的题目起始序号
     */
    private int findExpectedStartNumber(String[] lines, ExamTemplateData.SectionInfo sectionInfo) {
        // 查找第一个可能的题目行来推断起始序号
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // 匹配可能的题目行格式
            Pattern candidatePattern = Pattern.compile("^(\\d{1,2})\\s*[.、]\\s*([^\\s].{10,})");
            Matcher candidateMatcher = candidatePattern.matcher(line);
            
            if (candidateMatcher.find()) {
                int number = Integer.parseInt(candidateMatcher.group(1));
                String content = candidateMatcher.group(2);
                
                // 验证这是否看起来像真正的题目
                if (isLikelyQuestionContent(content)) {
                    log.debug("🎯 找到可能的起始题目: {}. {}", number, 
                            content.length() > 30 ? content.substring(0, 30) + "..." : content);
                    return number;
                }
            }
        }
        
        // 如果找不到，默认从1开始
        return 1;
    }

    /**
     * 计算预期的结束序号
     */
    private int calculateExpectedEndNumber(int startNumber, ExamTemplateData.SectionInfo sectionInfo) {
        if (sectionInfo.getQuestionCount() != null && sectionInfo.getQuestionCount() > 0) {
            return startNumber + sectionInfo.getQuestionCount() - 1;
        }
        // 如果没有题目数量信息，假设最多50题
        return startNumber + 49;
    }

    /**
     * 判断内容是否像题目
     */
    private boolean isLikelyQuestionContent(String content) {
        if (content == null || content.trim().length() < 5) {
            return false;
        }
        
        String trimmed = content.trim();
        
        // 排除明显不是题目的内容
        if (isDefinitelyNotQuestion(trimmed)) {
            return false;
        }
        
        // 检查题目特征
        return hasQuestionCharacteristics(trimmed);
    }

    /**
     * 检查是否明确不是题目
     */
    private boolean isDefinitelyNotQuestion(String content) {
        // IP地址格式
        if (content.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}.*")) {
            return true;
        }
        
        // 日期时间格式
        if (content.matches("^\\d{4}-\\d{2}-\\d{2}.*") || content.matches("^\\d{2}:\\d{2}.*")) {
            return true;
        }
        
        // 分数信息
        if (content.contains("学生得分") || content.contains("学生答案") || content.contains("正确答案")) {
            return true;
        }
        
        // 纯数字或短内容
        if (content.matches("^\\d+(\\.\\d+)*$") || content.length() < 8) {
            return true;
        }
        
        // 系统信息
        if (content.contains("提交时间") || content.contains("批阅教师") || content.contains("考试得分")) {
            return true;
        }
        
        return false;
    }

    /**
     * 检查是否具有题目特征
     */
    private boolean hasQuestionCharacteristics(String content) {
        // 包含问号
        if (content.contains("?") || content.contains("？")) {
            return true;
        }
        
        // 包含括号（常见于选择题、判断题）
        if (content.contains("( )") || content.contains("()")) {
            return true;
        }
        
        // 包含常见题目关键词
        String[] questionKeywords = {
            "什么", "如何", "怎样", "为什么", "是否", "能否", 
            "下列", "以下", "正确", "错误", "描述", "属于"
        };
        
        for (String keyword : questionKeywords) {
            if (content.contains(keyword)) {
                return true;
            }
        }
        
        // 长度合理且包含中文
        if (content.length() >= 10 && containsChineseCharacters(content)) {
            return true;
        }
        
        return false;
    }

    /**
     * 使用连续性验证解析题目
     */
    private List<ExamTemplateData.QuestionInfo> parseQuestionsWithSequentialValidation(
            String[] lines, int expectedStartNumber, int expectedEndNumber, ExamTemplateData.SectionInfo sectionInfo) {
        
        List<ExamTemplateData.QuestionInfo> questions = new ArrayList<>();
        ExamTemplateData.QuestionInfo currentQuestion = null;
        List<String> currentOptions = new ArrayList<>();
        StringBuilder currentQuestionContent = new StringBuilder();
        
        int currentExpectedNumber = expectedStartNumber;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // 检查是否是期望的题目序号
            Pattern questionPattern = Pattern.compile("^(" + currentExpectedNumber + ")\\s*[.、]\\s*(.*)");
            Matcher questionMatcher = questionPattern.matcher(line);
            
            if (questionMatcher.find()) {
                int foundNumber = Integer.parseInt(questionMatcher.group(1));
                String questionContent = questionMatcher.group(2);
                
                // 验证这确实是一个题目
                if (isLikelyQuestionContent(questionContent)) {
                    // 保存上一题
                    if (currentQuestion != null) {
                        finalizeQuestion(currentQuestion, currentQuestionContent.toString(), currentOptions, sectionInfo);
                        questions.add(currentQuestion);
                    }
                    
                    // 开始新题
                    currentQuestion = new ExamTemplateData.QuestionInfo();
                    currentQuestion.setQuestionNumber(foundNumber);
                    currentQuestion.setScore(sectionInfo.getScorePerQuestion());
                    
                    currentQuestionContent = new StringBuilder(questionContent);
                    currentOptions.clear();
                    
                    // 更新期望的下一个序号
                    currentExpectedNumber = foundNumber + 1;
                    
                    log.debug("✅ 找到题目 {}: {}", foundNumber, 
                            questionContent.length() > 30 ? questionContent.substring(0, 30) + "..." : questionContent);
                    
                    // 如果超出预期范围，停止解析
                    if (foundNumber > expectedEndNumber) {
                        log.debug("📍 题目序号 {} 超出预期范围 {}，停止解析", foundNumber, expectedEndNumber);
                        break;
                    }
                }
            } else if (currentQuestion != null) {
                // 处理题目的其他内容（选项、答案等）
                processQuestionLine(line, currentQuestion, currentOptions, currentQuestionContent);
            }
        }
        
        // 处理最后一题
        if (currentQuestion != null) {
            finalizeQuestion(currentQuestion, currentQuestionContent.toString(), currentOptions, sectionInfo);
            questions.add(currentQuestion);
        }
        
        return questions;
    }

    /**
     * 处理题目行的其他内容
     */
    private void processQuestionLine(String line, ExamTemplateData.QuestionInfo currentQuestion, 
                                   List<String> currentOptions, StringBuilder currentQuestionContent) {
        log.debug("🔍 处理行: [{}]", line);
        
        // 检查是否是选项行：支持 "A、选项" 和 "A 、 选项" 两种格式
        Pattern optionPattern = Pattern.compile("^([A-Z])\\s*、\\s*(.*)");
        Matcher optionMatcher = optionPattern.matcher(line);
        
        if (optionMatcher.find()) {
            String optionLabel = optionMatcher.group(1);
            String optionContent = optionMatcher.group(2);
            String fullOption = optionLabel + "、" + optionContent;
            currentOptions.add(fullOption);
            log.info("✅ 添加选项: [{}]", fullOption);
        } else if (line.contains("正确答案：") || line.contains("正确答案:")) {
            // 提取正确答案，支持"学生答案：C正确答案：C"这种格式
            Pattern answerPattern = Pattern.compile("正确答案[：:]\\s*([A-Z]|[^\\s]+)");
            Matcher answerMatcher = answerPattern.matcher(line);
            if (answerMatcher.find()) {
                String correctAnswer = answerMatcher.group(1).trim();
                currentQuestion.setCorrectAnswer(correctAnswer);
                log.info("✅ 提取正确答案: [{}]", correctAnswer);
            } else {
                // 备用方案：简单分割
                String correctAnswer = line.replaceAll(".*正确答案[：:]\\s*", "").trim();
                if (!correctAnswer.isEmpty()) {
                    currentQuestion.setCorrectAnswer(correctAnswer);
                    log.info("✅ 备用方案提取正确答案: [{}]", correctAnswer);
                }
            }
        } else if (line.contains("学生答案") || line.contains("学生得分") || 
                   line.contains("批语") || isDefinitelyNotQuestion(line)) {
            // 跳过学生信息相关行
            log.debug("⏭️ 跳过学生信息行: [{}]", line);
        } else {
            // 检查这行是否包含内联选项（题目内容中的选项）
            if (containsInlineOptions(line)) {
                log.debug("🔍 检测到内联选项行: [{}]", line);
                // 分离题目内容和选项
                String[] separated = separateQuestionAndOptions(line);
                String questionPart = separated[0];
                String optionsPart = separated[1];
                
                // 添加题目部分
                if (!questionPart.trim().isEmpty()) {
                    if (currentQuestionContent.length() > 0) {
                        currentQuestionContent.append(" ");
                    }
                    currentQuestionContent.append(questionPart);
                    log.debug("📝 添加题目内容: [{}]", questionPart);
                }
                
                // 解析内联选项
                if (!optionsPart.trim().isEmpty()) {
                    parseInlineOptions(optionsPart, currentOptions);
                }
            } else {
                // 正常添加到题目内容
                if (currentQuestionContent.length() > 0) {
                    currentQuestionContent.append(" ");
                }
                currentQuestionContent.append(line);
                log.debug("📝 添加题目内容: [{}]", line);
            }
        }
    }
    
    /**
     * 检查行是否包含内联选项
     */
    private boolean containsInlineOptions(String line) {
        // 检查是否包含多个选项标识符（A、B、C、D），支持空格格式
        Pattern optionPattern = Pattern.compile("([A-D])\\s*、");
        Matcher matcher = optionPattern.matcher(line);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count >= 2; // 至少包含2个选项才认为是内联选项
    }
    
    /**
     * 分离题目内容和选项
     */
    private String[] separateQuestionAndOptions(String line) {
        // 查找第一个选项的位置，支持空格格式
        Pattern firstOptionPattern = Pattern.compile("\\s+([A-D])\\s*、");
        Matcher matcher = firstOptionPattern.matcher(line);
        
        if (matcher.find()) {
            int optionStart = matcher.start();
            String questionPart = line.substring(0, optionStart).trim();
            String optionsPart = line.substring(optionStart).trim();
            return new String[]{questionPart, optionsPart};
        }
        
        return new String[]{line, ""};
    }
    
    /**
     * 解析内联选项
     */
    private void parseInlineOptions(String optionsText, List<String> currentOptions) {
        // 匹配所有选项：A、xxx B、xxx C、xxx D、xxx，支持空格格式
        Pattern optionPattern = Pattern.compile("([A-D])\\s*、\\s*([^A-D]*?)(?=\\s*[A-D]\\s*、|$)");
        Matcher matcher = optionPattern.matcher(optionsText);
        
        while (matcher.find()) {
            String optionLabel = matcher.group(1);
            String optionContent = matcher.group(2).trim();
            
            if (!optionContent.isEmpty()) {
                String fullOption = optionLabel + "、" + optionContent;
                currentOptions.add(fullOption);
                log.debug("📝 解析内联选项: {}", fullOption);
            }
        }
    }

    /**
     * 完善题目信息
     */
    private void finalizeQuestion(ExamTemplateData.QuestionInfo question, String questionContent, 
                                 List<String> options, ExamTemplateData.SectionInfo sectionInfo) {
        log.info("🎯 完善题目 {} 信息", question.getQuestionNumber());
        log.info("📋 原始题目内容: [{}]", questionContent);
        log.info("📋 收集到的选项数量: {}", options.size());
        
        for (int i = 0; i < options.size(); i++) {
            log.info("📋 选项 {}: [{}]", i + 1, options.get(i));
        }
        
        // 清理题目内容，移除可能残留的选项信息
        String cleanedContent = cleanQuestionContentForTemplate(questionContent);
        question.setQuestionContent(cleanedContent);
        log.info("📋 清理后题目内容: [{}]", cleanedContent);
        
        // 如果有选项，设置选项
        if (!options.isEmpty()) {
            question.setOptions(new ArrayList<>(options));
            log.info("✅ 题目 {} 设置了 {} 个选项", question.getQuestionNumber(), options.size());
        } else {
            log.warn("⚠️ 题目 {} 没有选项！", question.getQuestionNumber());
        }
        
        // 如果没有设置分数，使用大题的平均分数
        if (question.getScore() == null && sectionInfo.getScorePerQuestion() != null) {
            question.setScore(sectionInfo.getScorePerQuestion());
        }
        
        log.info("✅ 题目 {} 完成: 内容=[{}], 正确答案=[{}], 选项数={}", 
                question.getQuestionNumber(), 
                cleanedContent.length() > 50 ? cleanedContent.substring(0, 50) + "..." : cleanedContent,
                question.getCorrectAnswer(),
                question.getOptions() != null ? question.getOptions().size() : 0);
    }
    
    /**
     * 清理题目内容，移除选项信息（用于模板解析）
     */
    private String cleanQuestionContentForTemplate(String questionContent) {
        if (questionContent == null || questionContent.trim().isEmpty()) {
            return "";
        }
        
        String cleaned = questionContent.trim();
        
        // 移除题目末尾的选项信息（如：A 、 Flash C 、 NVRAM）
        // 查找第一个独立的选项标识符位置
        Pattern optionPattern = Pattern.compile("\\s+([A-D])\\s*、");
        Matcher matcher = optionPattern.matcher(cleaned);
        
        if (matcher.find()) {
            // 截取到第一个选项之前的内容
            cleaned = cleaned.substring(0, matcher.start()).trim();
        }
        
        // 移除其他可能的选项残留
        cleaned = cleaned.replaceAll("\\s+[A-D]\\s*、.*$", "");
        
        // 确保题目以适当的标点结尾
        if (!cleaned.isEmpty() && !cleaned.matches(".*[?？。！]\\s*\\(?\\s*\\)?\\s*$")) {
            // 如果题目没有以问号或句号结尾，且包含括号，保持原样
            // 否则可能需要补充标点，但为了安全起见，保持原样
        }
        
        return cleaned.trim();
    }

    /**
     * 计算模板汇总信息
     */
    private void calculateTemplateSummary(ExamTemplateData templateData) {
        List<ExamTemplateData.SectionInfo> sections = templateData.getSections();
        if (sections == null || sections.isEmpty()) {
            return;
        }
        
        int totalQuestions = 0;
        int totalScore = 0;
        
        for (ExamTemplateData.SectionInfo section : sections) {
            if (section.getQuestions() != null) {
                totalQuestions += section.getQuestions().size();
            }
            if (section.getTotalScore() != null) {
                totalScore += section.getTotalScore();
            }
        }
        
        templateData.setTotalQuestions(totalQuestions);
        templateData.setTotalScore(totalScore);
    }

    /**
     * 考试模板数据类
     */
    public static class ExamTemplateData {
        private String examTitle;
        private String subject;
        private String studentName;
        private String className;
        private Integer totalQuestions;
        private Integer totalScore;
        private List<SectionInfo> sections;
        
        // 构造函数和getter/setter
        public ExamTemplateData() {}
        
        public String getExamTitle() { return examTitle; }
        public void setExamTitle(String examTitle) { this.examTitle = examTitle; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        
        public Integer getTotalQuestions() { return totalQuestions; }
        public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }
        
        public Integer getTotalScore() { return totalScore; }
        public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }
        
        public List<SectionInfo> getSections() { return sections; }
        public void setSections(List<SectionInfo> sections) { this.sections = sections; }
        
        public static class SectionInfo {
            private String sectionNumber;
            private String sectionTitle;
            private String questionType;
            private Integer questionCount;
            private Integer totalScore;
            private Double scorePerQuestion;
            private List<QuestionInfo> questions;
            
            public SectionInfo() {}
            
            // Getter和Setter方法
            public String getSectionNumber() { return sectionNumber; }
            public void setSectionNumber(String sectionNumber) { this.sectionNumber = sectionNumber; }
            
            public String getSectionTitle() { return sectionTitle; }
            public void setSectionTitle(String sectionTitle) { this.sectionTitle = sectionTitle; }
            
            public String getQuestionType() { return questionType; }
            public void setQuestionType(String questionType) { this.questionType = questionType; }
            
            public Integer getQuestionCount() { return questionCount; }
            public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }
            
            public Integer getTotalScore() { return totalScore; }
            public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }
            
            public Double getScorePerQuestion() { return scorePerQuestion; }
            public void setScorePerQuestion(Double scorePerQuestion) { this.scorePerQuestion = scorePerQuestion; }
            
            public List<QuestionInfo> getQuestions() { return questions; }
            public void setQuestions(List<QuestionInfo> questions) { this.questions = questions; }
        }
        
        public static class QuestionInfo {
            private Integer questionNumber;
            private String questionContent;
            private String correctAnswer;
            private List<String> options;
            private Double score;
            
            public QuestionInfo() {}
            
            // Getter和Setter方法
            public Integer getQuestionNumber() { return questionNumber; }
            public void setQuestionNumber(Integer questionNumber) { this.questionNumber = questionNumber; }
            
            public String getQuestionContent() { return questionContent; }
            public void setQuestionContent(String questionContent) { this.questionContent = questionContent; }
            
            public String getCorrectAnswer() { return correctAnswer; }
            public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
            
            public List<String> getOptions() { return options; }
            public void setOptions(List<String> options) { this.options = options; }
            
            public Double getScore() { return score; }
            public void setScore(Double score) { this.score = score; }
        }
    }

    /**
     * 从文档中提取文本内容
     */
    private String extractTextFromDocument(File file) throws IOException {
        String fileName = file.getName().toLowerCase();
        
        try (FileInputStream fis = new FileInputStream(file)) {
            // 尝试检测文件格式
            if (fileName.endsWith(".docx")) {
                return extractFromDocx(fis);
            } else if (fileName.endsWith(".doc")) {
                return extractFromDoc(fis);
            } else {
                throw new IllegalArgumentException("不支持的文件格式: " + fileName);
            }
        } catch (Exception e) {
            log.warn("使用标准方式解析失败，尝试备用方案: {}", e.getMessage());
            // 如果标准方式失败，尝试另一种格式
            return extractWithFallback(file);
        }
    }

    /**
     * 提取DOCX文档内容（使用文件流）
     */
    private String extractFromDocx(FileInputStream fis) throws IOException {
        try (XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }
    
    /**
     * 提取DOCX文档内容（使用文件路径，更可靠）
     */
    private String extractFromDocx(File docxFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(docxFile);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    /**
     * 提取DOC文档内容
     */
    private String extractFromDoc(FileInputStream fis) throws IOException {
        try (HWPFDocument document = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    /**
     * 备用提取方案 - 尝试不同的格式和方法
     */
    private String extractWithFallback(File file) throws IOException {
        String fileName = file.getName().toLowerCase();
        
        try (FileInputStream fis = new FileInputStream(file)) {
            // 如果文件名是.doc但实际是.docx格式
            if (fileName.endsWith(".doc")) {
                try {
                    log.info("尝试使用DOCX格式解析DOC文件: {}", fileName);
                    return extractFromDocx(fis);
                } catch (Exception e) {
                    log.debug("DOCX格式解析失败: {}", e.getMessage());
                }
            }
            
            // 如果文件名是.docx但实际是.doc格式
            if (fileName.endsWith(".docx")) {
                try (FileInputStream fis2 = new FileInputStream(file)) {
                    log.info("尝试使用DOC格式解析DOCX文件: {}", fileName);
                    return extractFromDoc(fis2);
                } catch (Exception e) {
                    log.debug("DOC格式解析失败: {}", e.getMessage());
                }
            }
        }
        
        // 尝试使用Apache Tika解析（最强的备用方案）
        try {
            log.info("尝试使用Apache Tika解析文件: {}", fileName);
            return extractWithTika(file);
        } catch (Exception e) {
            log.debug("Tika解析失败: {}", e.getMessage());
        }
        
        // 尝试使用LibreOffice转换后解析（最强的备用方案）
        if (libreOfficeConverterService.isLibreOfficeAvailable()) {
            try {
                log.info("尝试使用LibreOffice转换文件: {}", fileName);
                return extractWithLibreOfficeConversion(file);
            } catch (Exception e) {
                log.debug("LibreOffice转换解析失败: {}", e.getMessage());
            }
        }
        
        // 尝试纯文本读取（适用于RTF或其他文本格式）
        try {
            log.info("尝试使用文本格式读取文件: {}", fileName);
            return extractAsPlainText(file);
        } catch (Exception e) {
            log.debug("纯文本格式解析失败: {}", e.getMessage());
        }
        
        // 记录错误但不中断整个导入过程
        log.error("无法解析文档: {}，跳过该文件", fileName);
        return null; // 返回null表示跳过该文件
    }

    /**
     * 使用LibreOffice转换后解析文档
     */
    private String extractWithLibreOfficeConversion(File file) throws Exception {
        String fileName = file.getName().toLowerCase();
        
        // 首先尝试转换为docx
        try {
            File convertedDocx = libreOfficeConverterService.convertDocToDocx(file);
            log.info("成功转换为DOCX: {}", convertedDocx.getName());
            
            // 解析转换后的docx文件（使用文件路径方法更可靠）
            String content = extractFromDocx(convertedDocx);
            log.debug("DOCX提取内容长度: {}, 包含中文: {}", 
                content != null ? content.length() : 0, 
                content != null ? containsChineseCharacters(content) : false);
            
            if (content != null && content.length() > 200) {
                log.debug("DOCX提取内容前200字符: {}", content.substring(0, 200));
            }
            
            if (content != null && content.length() > 50 && containsChineseCharacters(content)) {
                log.info("LibreOffice转换DOCX解析成功，内容长度: {}", content.length());
                return content;
            } else {
                log.warn("DOCX解析失败，内容长度: {}, 包含中文: {}", 
                    content != null ? content.length() : 0, 
                    content != null ? containsChineseCharacters(content) : false);
                // 即使验证失败，如果内容长度合理，也尝试强制使用
                if (content != null && content.length() > 100) {
                    log.info("强制使用DOCX内容进行解析，内容长度: {}", content.length());
                    return content;
                }
            }
        } catch (Exception e) {
            log.debug("LibreOffice DOCX转换失败: {}", e.getMessage());
        }
        
        // 如果docx转换失败，尝试转换为HTML
        try {
            File convertedHtml = libreOfficeConverterService.convertDocToHtml(file);
            log.info("成功转换为HTML: {}", convertedHtml.getName());
            
            // 从HTML中提取纯文本
            String content = htmlContentExtractor.extractTextFromHtml(convertedHtml);
            log.debug("HTML提取的内容长度: {}, 是否有效: {}", content.length(), htmlContentExtractor.isValidContent(content));
            
            if (content.length() > 200) {
                log.debug("HTML提取内容前200字符: {}", content.substring(0, 200));
            }
            
            if (htmlContentExtractor.isValidContent(content)) {
                log.info("LibreOffice转换HTML解析成功，内容长度: {}", content.length());
                return content;
            } else {
                log.warn("HTML内容验证失败，但尝试强制使用该内容进行解析");
                // 即使验证失败，也尝试解析，可能只是验证条件过于严格
                if (content.length() > 100) {
                    log.info("强制使用HTML内容进行解析，内容长度: {}", content.length());
                    return content;
                }
            }
        } catch (Exception e) {
            log.debug("LibreOffice HTML转换失败: {}", e.getMessage());
        }
        
        throw new IOException("LibreOffice转换后仍无法解析文档内容");
    }

    /**
     * 使用Apache Tika解析文档
     */
    private String extractWithTika(File file) throws Exception {
        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        BodyContentHandler handler = new BodyContentHandler(-1); // 无限制
        
        try (FileInputStream fis = new FileInputStream(file)) {
            ParseContext parseContext = new ParseContext();
            parser.parse(fis, handler, metadata, parseContext);
            
            String content = handler.toString().trim();
            if (content.length() > 50 && containsChineseCharacters(content)) {
                log.info("Tika成功解析文档，提取内容长度: {}", content.length());
                return content;
            } else {
                throw new IOException("Tika提取的内容不足或无中文内容");
            }
        }
    }

    /**
     * 尝试以纯文本方式读取文件
     */
    private String extractAsPlainText(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        
        // 尝试不同的字符编码
        String[] encodings = {"UTF-8", "GBK", "GB2312", "ISO-8859-1"};
        
        for (String encoding : encodings) {
            try (FileInputStream fis = new FileInputStream(file);
                 InputStreamReader isr = new InputStreamReader(fis, encoding);
                 BufferedReader reader = new BufferedReader(isr)) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    // 过滤掉控制字符和乱码
                    String cleanLine = line.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                                          .replaceAll("\\\\[a-zA-Z0-9]+", " ") // 移除RTF控制代码
                                          .trim();
                    
                    if (!cleanLine.isEmpty() && cleanLine.length() > 2) {
                        content.append(cleanLine).append("\n");
                    }
                }
                
                // 如果提取到了有意义的内容，返回结果
                String result = content.toString().trim();
                if (result.length() > 50 && containsChineseCharacters(result)) {
                    log.info("使用{}编码成功提取文本内容", encoding);
                    return result;
                }
                
                content.setLength(0); // 清空缓冲区，尝试下一种编码
                
            } catch (Exception e) {
                log.debug("使用{}编码读取失败: {}", encoding, e.getMessage());
            }
        }
        
        throw new IOException("无法以文本格式读取文件");
    }

    /**
     * 检查文本是否包含中文字符
     */
    private boolean containsChineseCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // 检查是否包含中文字符（包括中文标点符号）
        return text.matches(".*[\\u4e00-\\u9fa5\\u3000-\\u303f\\uff00-\\uffef].*");
    }

    /**
     * 解析答案内容，提取学生信息和答案
     */
    private StudentAnswerImportData parseAnswerContent(String content, String fileName) {
        StudentAnswerImportData importData = new StudentAnswerImportData();
        
        // 清理内容，过滤图片和乱码
        String cleanedContent = cleanDocumentContent(content);
        
        // 1. 优先从内容中解析学生信息
        parseStudentInfoFromContent(cleanedContent, importData);
        
        // 2. 如果文件名中的信息更完整，则补充
        parseStudentInfoFromFileName(fileName, importData);
        
        // 解析答案内容
        parseAnswersFromContent(cleanedContent, importData);
        
        return importData;
    }

    /**
     * 从文档内容中解析学生信息
     */
    private void parseStudentInfoFromContent(String content, StudentAnswerImportData importData) {
        // 答题人/姓名
        Pattern namePattern = Pattern.compile("(?:答题人|姓名)\\s*[:：]\\s*([^\\s]+)");
        Matcher nameMatcher = namePattern.matcher(content);
        if (nameMatcher.find()) {
            importData.setStudentName(nameMatcher.group(1).trim());
        }

        // 学号
        Pattern numberPattern = Pattern.compile("学号\\s*[:：]\\s*(\\d+)");
        Matcher numberMatcher = numberPattern.matcher(content);
        if (numberMatcher.find()) {
            importData.setStudentNumber(numberMatcher.group(1).trim());
        }

        // 班级
        Pattern classPattern = Pattern.compile("班级\\s*[:：]\\s*([^\\s]+)");
        Matcher classMatcher = classPattern.matcher(content);
        if (classMatcher.find()) {
            importData.setClassName(classMatcher.group(1).trim());
        }

        log.debug("从内容解析的学生信息: 姓名={}, 学号={}, 班级={}", 
                 importData.getStudentName(), importData.getStudentNumber(), importData.getClassName());
    }

    /**
     * 从文件名解析学生信息
     * 文件名格式: miniprogram1166115562438104-段志贤-2024-2025-2《路由与交换技术》期末考试--A卷.doc
     * 或: 大数据与基础科学学院-计算机类-计算机类2022-2206181018-冯华睿-2024-2025-2《路由与交换技术》期末考试--A卷.doc
     */
    private void parseStudentInfoFromFileName(String fileName, StudentAnswerImportData importData) {
        // 移除文件扩展名
        String nameWithoutExtension = fileName.replaceAll("\\.(doc|docx)$", "");
        
        // 正则表达式模式匹配学生信息
        // 模式1: miniprogram数字-姓名-考试信息（特殊处理miniprogram开头的学号）
        Pattern pattern1 = Pattern.compile("(miniprogram\\d+)-([^-]+)-(.+)");
        Matcher matcher1 = pattern1.matcher(nameWithoutExtension);
        
        if (matcher1.find()) {
            // 如果学号为空，则设置
            if (importData.getStudentNumber() == null || importData.getStudentNumber().trim().isEmpty()) {
                importData.setStudentNumber(matcher1.group(1));
            }
            // 如果姓名为空，则设置
            if (importData.getStudentName() == null || importData.getStudentName().trim().isEmpty()) {
                importData.setStudentName(matcher1.group(2));
            }
            // 考试标题总是从文件名获取，可能更准确
            importData.setExamTitle(matcher1.group(3));
            log.debug("解析文件名模式1补充信息: 学号={}, 姓名={}, 考试={}", 
                     importData.getStudentNumber(), importData.getStudentName(), importData.getExamTitle());
            return;
        }

        // 模式2: 学院-专业-专业年级学号-姓名-考试信息
        Pattern pattern2 = Pattern.compile("([^-]+)-([^-]+)-([^-]*)(\\d{10,})-([^-]+)-(.+)");
        Matcher matcher2 = pattern2.matcher(nameWithoutExtension);
        
        if (matcher2.find()) {
            // 补充信息
            if (importData.getCollege() == null || importData.getCollege().trim().isEmpty()) {
                importData.setCollege(matcher2.group(1));
            }
            if (importData.getMajor() == null || importData.getMajor().trim().isEmpty()) {
                importData.setMajor(matcher2.group(2));
            }
            if (importData.getStudentNumber() == null || importData.getStudentNumber().trim().isEmpty()) {
                importData.setStudentNumber(matcher2.group(4));
            }
            if (importData.getStudentName() == null || importData.getStudentName().trim().isEmpty()) {
                importData.setStudentName(matcher2.group(5));
            }
            importData.setExamTitle(matcher2.group(6));
            log.debug("解析文件名模式2补充信息: 学院={}, 专业={}, 学号={}, 姓名={}", 
                     importData.getCollege(), importData.getMajor(), importData.getStudentNumber(), importData.getStudentName());
            return;
        }

        // 模式3: 专业年级-学号-姓名
        Pattern pattern3 = Pattern.compile("([^-]+)(\\d{4})-(\\d+)-([^-]+)");
        Matcher matcher3 = pattern3.matcher(nameWithoutExtension);
        
        if (matcher3.find()) {
            if (importData.getMajor() == null || importData.getMajor().trim().isEmpty()) {
                importData.setMajor(matcher3.group(1) + matcher3.group(2));
            }
            if (importData.getStudentNumber() == null || importData.getStudentNumber().trim().isEmpty()) {
                importData.setStudentNumber(matcher3.group(3));
            }
            if (importData.getStudentName() == null || importData.getStudentName().trim().isEmpty()) {
                importData.setStudentName(matcher3.group(4));
            }
            log.debug("解析文件名模式3补充信息: 专业={}, 学号={}, 姓名={}", 
                     importData.getMajor(), importData.getStudentNumber(), importData.getStudentName());
            return;
        }

        log.warn("无法从文件名解析学生信息: {}", fileName);
    }

    /**
     * 从文档内容解析答案
     */
    private void parseAnswersFromContent(String content, StudentAnswerImportData importData) {
        List<StudentAnswerImportData.QuestionAnswer> answers = new ArrayList<>();
        
        // 清理内容，过滤图片和乱码
        String cleanedContent = cleanDocumentContent(content);
        
        // 添加调试日志
        log.debug("开始解析文档内容，原始长度: {}, 清理后长度: {}", content.length(), cleanedContent.length());
        if (cleanedContent.length() > 200) {
            log.debug("清理后内容前200字符: {}", cleanedContent.substring(0, 200));
        }
        
        // 尝试学习通格式解析（包含"学生答案："的格式）
        if (cleanedContent.contains("学生答案：") || cleanedContent.contains("学生答案:")) {
            log.info("检测到学习通考试结果格式，使用专用解析器");
            parseLearningAnswers(cleanedContent, answers);
        } else {
            // 使用通用解析方法
            log.info("使用通用答案解析器");
            parseGeneralAnswers(cleanedContent, answers);
        }
        
        importData.setAnswers(answers);
        log.info("解析到 {} 道题目的答案", answers.size());
    }
    
    /**
     * 清理文档内容，去除格式化字符但保留有效内容
     */
    private String cleanDocumentContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }

        String cleaned = content;
        
        // 移除XML标签和HTML标签
        cleaned = cleaned.replaceAll("<[^>]+>", " ");
        
        // 移除多余的空白字符，但保留基本结构
        cleaned = cleaned.replaceAll("\\s{3,}", "\n"); // 多个空格替换为换行
        cleaned = cleaned.replaceAll("[ \\t]{2,}", " "); // 多个空格/制表符替换为单个空格
        
        // 移除一些特殊的格式字符，但保留中文内容
        cleaned = cleaned.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", ""); // 控制字符
        
        // 保留中文标点符号和基本标点
        // 不要过度清理，避免丢失重要信息
        
        return cleaned.trim();
    }
    
    /**
     * 解析学习通格式答案，优化版本
     */
    private void parseLearningAnswers(String content, List<StudentAnswerImportData.QuestionAnswer> answers) {
        log.info("🔍 开始解析学习通考试答案（简化版）");
        
        // 按题号分割文档内容
        List<String> questionBlocks = splitByQuestionNumbers(content);
        log.info("📊 按题号分割得到 {} 个题目块", questionBlocks.size());
        
        for (int i = 0; i < questionBlocks.size(); i++) {
            String questionBlock = questionBlocks.get(i);
            int questionNumber = i + 1;
            
            log.debug("🔍 解析第{}题，内容长度: {}", questionNumber, questionBlock.length());
            
            // 从题目块中提取学生答案
            String studentAnswer = extractStudentAnswerFromBlock(questionBlock, questionNumber);
            
            // 从题目块中提取得分
            Double score = extractScoreFromBlock(questionBlock, questionNumber);
            
            // 从题目块中提取题目内容（去掉学生答案和得分信息）
            String questionContent = extractQuestionContentFromBlock(questionBlock, questionNumber);
            
            // 创建答案对象
            StudentAnswerImportData.QuestionAnswer qa = new StudentAnswerImportData.QuestionAnswer();
            qa.setQuestionContent(questionContent);
            qa.setAnswerContent(studentAnswer);
            qa.setScore(score);
            
            answers.add(qa);
            
            log.info("✅ 题目{}: 答案=\"{}\", 分数={}", 
                     questionNumber, 
                     studentAnswer != null ? (studentAnswer.length() > 50 ? studentAnswer.substring(0, 50) + "..." : studentAnswer) : "null",
                     score);
        }
        
        log.info("🎯 解析完成，总共 {} 道题目", answers.size());
    }
    
    /**
     * 按题号分割文档内容
     */
    private List<String> splitByQuestionNumbers(String content) {
        List<String> questionBlocks = new ArrayList<>();
        
        // 匹配题号：1.、2.、3. 等（数字后面跟点号）
        Pattern questionPattern = Pattern.compile("(\\d+)\\s*\\.");
        Matcher matcher = questionPattern.matcher(content);
        
        List<QuestionPosition> questionPositions = new ArrayList<>();
        while (matcher.find()) {
            int questionNum = Integer.parseInt(matcher.group(1));
            int position = matcher.start();
            questionPositions.add(new QuestionPosition(questionNum, position));
            log.debug("📍 找到题号 {} 在位置 {}", questionNum, position);
        }
        
        log.info("🔢 总共找到 {} 个题号标记", questionPositions.size());
        
        // 按题号排序（通常应该已经按顺序，但保险起见）
        questionPositions.sort((a, b) -> Integer.compare(a.questionNumber, b.questionNumber));
        
        // 按位置分割内容
        for (int i = 0; i < questionPositions.size(); i++) {
            QuestionPosition currentPos = questionPositions.get(i);
            int startPos = currentPos.position;
            int endPos = (i + 1 < questionPositions.size()) ? questionPositions.get(i + 1).position : content.length();
            
            String questionBlock = content.substring(startPos, endPos).trim();
            questionBlocks.add(questionBlock);
            
            log.debug("📋 题目{} 内容长度: {} 字符", currentPos.questionNumber, questionBlock.length());
            
            // 如果题目内容很短，可能是分割有问题
            if (questionBlock.length() < 20) {
                log.warn("⚠️ 题目{} 内容太短，可能分割有误: \"{}\"", currentPos.questionNumber, questionBlock);
            }
            
            // 检查是否包含"学生答案"字段
            if (!questionBlock.contains("学生答案")) {
                log.warn("⚠️ 题目{} 中未找到'学生答案'字段", currentPos.questionNumber);
            }
        }
        
        log.info("✅ 完成题目分割，共分割出 {} 个题目块", questionBlocks.size());
        return questionBlocks;
    }
    
    /**
     * 题目位置信息
     */
    private static class QuestionPosition {
        int questionNumber;
        int position;
        
        QuestionPosition(int questionNumber, int position) {
            this.questionNumber = questionNumber;
            this.position = position;
        }
    }
    
    /**
     * 从题目块中提取学生答案 - 专门针对学习通标准格式
     */
    private String extractStudentAnswerFromBlock(String questionBlock, int questionNumber) {
        log.debug("🔍 题目{} 原始内容: \"{}\"", questionNumber, 
                 questionBlock.length() > 300 ? questionBlock.substring(0, 300) + "..." : questionBlock);
        
        String answer = null;
        
        // 主要格式: "学生答案：C正确答案：C" 或 "学生答案： √ 正确答案： √ "
        Pattern mainPattern = Pattern.compile("学生答案[：:]\\s*([^\\n\\r]*?)(?=正确答案|$)", Pattern.MULTILINE | Pattern.DOTALL);
        Matcher mainMatcher = mainPattern.matcher(questionBlock);
        if (mainMatcher.find()) {
            answer = mainMatcher.group(1).trim();
            log.debug("📍 题目{} 主格式匹配: \"{}\"", questionNumber, answer);
            
            // 如果答案不为空且不是空白字符，直接返回
            if (!answer.isEmpty() && !answer.matches("\\s+")) {
                log.info("✅ 题目{} 提取到学生答案: \"{}\"", questionNumber, answer);
                return answer;
            }
        }
        
        // 备用格式: 对于主观题，提取"学生答案："后面的多行内容
        Pattern subjectivePattern = Pattern.compile("学生答案[：:]\\s*([\\s\\S]*?)(?=正确答案|$)", Pattern.MULTILINE | Pattern.DOTALL);
        Matcher subjectiveMatcher = subjectivePattern.matcher(questionBlock);
        if (subjectiveMatcher.find()) {
            String subjectiveAnswer = subjectiveMatcher.group(1).trim();
            log.debug("📍 题目{} 主观题格式匹配: \"{}\"", questionNumber, 
                     subjectiveAnswer.length() > 100 ? subjectiveAnswer.substring(0, 100) + "..." : subjectiveAnswer);
            
            // 清理主观题答案内容
            subjectiveAnswer = subjectiveAnswer.replaceAll("\\s*正确答案.*", ""); // 移除正确答案部分
            subjectiveAnswer = subjectiveAnswer.trim();
            
            if (!subjectiveAnswer.isEmpty() && subjectiveAnswer.length() > 2) {
                log.info("✅ 题目{} 提取到主观题答案: \"{}\"", questionNumber, 
                        subjectiveAnswer.length() > 50 ? subjectiveAnswer.substring(0, 50) + "..." : subjectiveAnswer);
                return subjectiveAnswer;
            }
        }
        
        // 特殊格式: 查找单独的选择答案（A、B、C、D）或判断答案（√、×）
        Pattern choicePattern = Pattern.compile("学生答案[：:]\\s*([A-D√×])\\s*正确答案");
        Matcher choiceMatcher = choicePattern.matcher(questionBlock);
        if (choiceMatcher.find()) {
            answer = choiceMatcher.group(1).trim();
            log.info("✅ 题目{} 提取到选择/判断答案: \"{}\"", questionNumber, answer);
            return answer;
        }
        
        // 兜底策略: 在整个块中查找可能的答案模式
        String[] lines = questionBlock.split("\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.contains("学生答案")) {
                // 尝试从这一行提取答案
                String extractedFromLine = extractAnswerFromSingleLine(line);
                if (extractedFromLine != null && !extractedFromLine.isEmpty() && !extractedFromLine.equals("学生未作答")) {
                    log.info("✅ 题目{} 从单行提取到答案: \"{}\"", questionNumber, extractedFromLine);
                    return extractedFromLine;
                }
            }
        }
        
        log.warn("⚠️ 题目{} 未找到有效学生答案", questionNumber);
        return "学生未作答";
    }
    
    /**
     * 从单行中提取答案
     */
    private String extractAnswerFromSingleLine(String line) {
        // 格式1: "学生答案：C正确答案：C"
        Pattern pattern1 = Pattern.compile("学生答案[：:]\\s*([^正确答案\\n\\r]+?)(?=正确答案|$)");
        Matcher matcher1 = pattern1.matcher(line);
        if (matcher1.find()) {
            String answer = matcher1.group(1).trim();
            if (!answer.isEmpty()) {
                return answer;
            }
        }
        
        // 格式2: "学生答案： √ 正确答案： √"（有空格的情况）
        Pattern pattern2 = Pattern.compile("学生答案[：:]\\s+([A-D√×])\\s+正确答案");
        Matcher matcher2 = pattern2.matcher(line);
        if (matcher2.find()) {
            return matcher2.group(1).trim();
        }
        
        return null;
    }
    
    /**
     * 从题目块中提取得分
     */
    private Double extractScoreFromBlock(String questionBlock, int questionNumber) {
        // 匹配"学生得分：xxx 分"
        Pattern scorePattern = Pattern.compile("学生得分[：:]\\s*(\\d+(?:\\.\\d+)?)\\s*分");
        Matcher matcher = scorePattern.matcher(questionBlock);
        
        if (matcher.find()) {
            try {
                Double score = Double.parseDouble(matcher.group(1));
                log.debug("📊 题目{} 提取到得分: {}", questionNumber, score);
                return score;
            } catch (NumberFormatException e) {
                log.warn("⚠️ 题目{} 得分解析失败: {}", questionNumber, matcher.group(1));
            }
        }
        
        log.debug("📊 题目{} 未找到得分信息", questionNumber);
        return null;
    }
    
    /**
     * 从题目块中提取题目内容（去掉答案和得分信息）
     */
    private String extractQuestionContentFromBlock(String questionBlock, int questionNumber) {
        // 移除学生答案和得分相关的信息，保留题目内容
        String content = questionBlock;
        
        // 移除"学生答案：xxx正确答案：xxx"部分
        content = content.replaceAll("学生答案[：:].*?正确答案[：:].*?(?=\\n|$)", "");
        
        // 移除"学生得分：xxx 分"部分
        content = content.replaceAll("学生得分[：:].*?分", "");
        
        // 移除多余的空行和空格
        content = content.replaceAll("\\n\\s*\\n", "\\n").trim();
        
        // 如果内容为空，使用题号作为标识
        if (content.isEmpty() || content.length() < 10) {
            content = "题目" + questionNumber;
        }
        
        log.debug("📝 题目{} 提取到题目内容长度: {}", questionNumber, content.length());
        return content;
    }
    
    /**
     * 通用答案解析方法 - 用于非学习通格式的文档
     */
    private void parseGeneralAnswers(String content, List<StudentAnswerImportData.QuestionAnswer> answers) {
        log.info("🔍 开始通用答案解析");
        
        if (content == null || content.trim().isEmpty()) {
            log.warn("内容为空，无法解析");
            return;
        }
        
        // 清理内容，移除多余的空行和空格
        String cleanedContent = content.trim().replaceAll("\\n\\s*\\n", "\n").replaceAll("\\s+", " ");
        
        // 检查是否是单个题目的完整答案（常见于作业批量导入）
        if (isSingleQuestionAnswer(cleanedContent)) {
            log.info("🎯 识别为单个题目的完整答案，直接导入整个内容");
            
            StudentAnswerImportData.QuestionAnswer qa = new StudentAnswerImportData.QuestionAnswer();
            
            // 尝试提取题目标题作为题目内容
            String questionTitle = extractQuestionTitle(cleanedContent);
            qa.setQuestionContent(questionTitle);
            
            // 整个文档内容作为答案
            qa.setAnswerContent(cleanedContent);
            qa.setScore(5.0); // 默认分数
            
            answers.add(qa);
            log.info("✅ 单题目答案导入完成，题目: {}, 答案长度: {} 字符", 
                    questionTitle, cleanedContent.length());
            return;
        }
        
        // 原有的多题目解析逻辑（保留作为备选）
        parseMultipleQuestions(cleanedContent, answers);
        
        log.info("🎯 通用解析完成，解析到 {} 道题目", answers.size());
    }
    
    /**
     * 判断是否是单个题目的完整答案
     */
    private boolean isSingleQuestionAnswer(String content) {
        // 检查特征：
        // 1. 内容不是很长（单个题目答案通常不会超过5000字符）
        if (content.length() > 5000) {
            return false;
        }
        
        // 2. 包含明显的题目标题或编号
        if (content.matches(".*\\b\\d+\\.\\d+\\b.*") || // 如 3.3
            content.contains("题目") || content.contains("作业") || 
            content.contains("实验") || content.contains("练习")) {
            return true;
        }
        
        // 3. 包含技术术语但没有明显的多题目分割
        boolean hasTechTerms = content.contains("router") || content.contains("config") || 
                              content.contains("interface") || content.contains("命令") ||
                              content.contains("配置") || content.contains("步骤");
        
        // 4. 计算可能的题目分割点数量
        int possibleQuestions = countPossibleQuestionMarkers(content);
        
        // 如果有技术术语但分割点少于3个，认为是单个题目
        if (hasTechTerms && possibleQuestions < 3) {
            return true;
        }
        
        // 5. 内容相对较短且格式统一
        if (content.length() < 1000 && possibleQuestions < 2) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 计算可能的题目标记数量
     */
    private int countPossibleQuestionMarkers(String content) {
        int count = 0;
        String[] lines = content.split("\\n");
        
        for (String line : lines) {
            line = line.trim();
            if (isNewQuestionLine(line)) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * 提取题目标题
     */
    private String extractQuestionTitle(String content) {
        String[] lines = content.split("\\n");
        
        // 查找第一行中的题目标题
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // 包含数字编号的标题（如 3.3 路由器配置步骤说明）
            if (line.matches(".*\\d+\\.\\d+.*")) {
                return line.length() > 50 ? line.substring(0, 50) + "..." : line;
            }
            
            // 包含关键词的标题
            if (line.contains("作业") || line.contains("实验") || line.contains("练习") || 
                line.contains("题目") || line.contains("说明") || line.contains("分析")) {
                return line.length() > 50 ? line.substring(0, 50) + "..." : line;
            }
            
            // 如果第一行内容较短且有意义，作为标题
            if (line.length() > 5 && line.length() < 100 && !line.startsWith("http")) {
                return line;
            }
        }
        
        // 默认标题
        return "学生答案文档";
    }
    
    /**
     * 多题目解析逻辑（原有逻辑的重构版本）
     */
    private void parseMultipleQuestions(String content, List<StudentAnswerImportData.QuestionAnswer> answers) {
        log.info("🔍 按多题目格式解析");
        
        // 按行分割内容
        String[] lines = content.split("\\n");
        StringBuilder currentQuestion = new StringBuilder();
        String currentAnswer = "";
        Double currentScore = null;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // 检查是否是答案行
            if (line.toLowerCase().contains("答案") || line.toLowerCase().contains("answer")) {
                if (currentQuestion.length() > 0) {
                    // 保存当前题目
                    StudentAnswerImportData.QuestionAnswer qa = new StudentAnswerImportData.QuestionAnswer();
                    qa.setQuestionContent(currentQuestion.toString().trim());
                    qa.setAnswerContent(currentAnswer.isEmpty() ? "未作答" : currentAnswer);
                    qa.setScore(currentScore != null ? currentScore : 5.0);
                    answers.add(qa);
                    
                    // 重置状态
                    currentQuestion = new StringBuilder();
                    currentAnswer = "";
                    currentScore = null;
                }
                
                // 提取答案内容
                currentAnswer = extractAnswerFromLine(line);
                
            } else if (line.toLowerCase().contains("分数") || line.toLowerCase().contains("得分")) {
                // 提取分数
                currentScore = extractScoreFromLine(line);
                
            } else if (isNewQuestionLine(line)) {
                // 新题目开始
                if (currentQuestion.length() > 0) {
                    // 保存上一个题目
                    StudentAnswerImportData.QuestionAnswer qa = new StudentAnswerImportData.QuestionAnswer();
                    qa.setQuestionContent(currentQuestion.toString().trim());
                    qa.setAnswerContent(currentAnswer.isEmpty() ? "未作答" : currentAnswer);
                    qa.setScore(currentScore != null ? currentScore : 5.0);
                    answers.add(qa);
                }
                
                // 开始新题目
                currentQuestion = new StringBuilder(line);
                currentAnswer = "";
                currentScore = null;
                
            } else if (currentQuestion.length() > 0) {
                // 继续添加到当前题目
                currentQuestion.append(" ").append(line);
            } else if (isLikelyQuestionForGeneral(line)) {
                // 如果没有明确的编号但看起来像题目内容，开始一个新题目
                currentQuestion = new StringBuilder(line);
                currentAnswer = "";
                currentScore = null;
                log.debug("🔍 识别到无编号题目: {}", line.substring(0, Math.min(50, line.length())));
            }
        }
        
        // 处理最后一个题目
        if (currentQuestion.length() > 0) {
            StudentAnswerImportData.QuestionAnswer qa = new StudentAnswerImportData.QuestionAnswer();
            qa.setQuestionContent(currentQuestion.toString().trim());
            qa.setAnswerContent(currentAnswer.isEmpty() ? "未作答" : currentAnswer);
            qa.setScore(currentScore != null ? currentScore : 5.0);
            answers.add(qa);
        }
    }
    
    /**
     * 判断是否是新题目行 - 支持多种编号格式
     */
    private boolean isNewQuestionLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        
        // 支持的题目编号格式：
        // 1. 标准数字编号：1. 2. 3.
        // 2. 中文数字编号：一、二、三、
        // 3. 中文括号编号：（1）（2）（3）
        // 4. 英文括号编号：(1) (2) (3)
        // 5. 小数编号：1.1 2.1 3.3
        // 6. 纯数字编号：1、2、3、
        String[] patterns = {
            "^\\d+\\s*[.、]\\s*.*",                    // 1. 或 1、
            "^[一二三四五六七八九十]+\\s*[.、]\\s*.*",      // 一、二、三、
            "^[（(]\\s*\\d+\\s*[）)]\\s*.*",           // （1）或 (1)
            "^\\d+\\.\\d+\\s*.*",                     // 1.1 2.1 3.3
            "^第\\s*\\d+\\s*题\\s*.*",                 // 第1题 第2题
            "^题目\\s*\\d+\\s*.*",                     // 题目1 题目2
            "^\\d+\\s*[:：]\\s*.*"                     // 1: 或 1：
        };
        
        for (String pattern : patterns) {
            if (line.matches(pattern)) {
                log.debug("🎯 匹配题目格式: {} -> {}", pattern, line.substring(0, Math.min(30, line.length())));
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 判断内容是否可能是题目（用于没有明确编号的情况）
     */
    private boolean isLikelyQuestionForGeneral(String line) {
        if (line == null || line.trim().length() < 10) {
            return false;
        }
        
        String trimmed = line.trim();
        
        // 排除明显不是题目的内容
        if (trimmed.contains("学生答案") || trimmed.contains("正确答案") || 
            trimmed.contains("学生得分") || trimmed.contains("批语") ||
            trimmed.matches("^\\d+(\\.\\d+)*$") || // 纯数字
            trimmed.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}.*")) { // IP地址
            return false;
        }
        
        // 检查是否包含命令、配置等关键词（针对网络技术类题目）
        if (trimmed.contains("router") || trimmed.contains("config") || 
            trimmed.contains("interface") || trimmed.contains("shutdown") ||
            trimmed.contains("clock rate") || trimmed.contains("ip address") ||
            trimmed.contains("含义") || trimmed.contains("命令") ||
            trimmed.contains("配置") || trimmed.contains("步骤")) {
            return true;
        }
        
        // 检查是否包含问号或其他题目特征
        if (trimmed.contains("?") || trimmed.contains("？") ||
            trimmed.contains("什么") || trimmed.contains("如何") ||
            trimmed.contains("怎样") || trimmed.contains("为什么")) {
            return true;
        }
        
        // 如果内容较长且包含中文，也可能是题目
        if (trimmed.length() > 20 && containsChineseCharacters(trimmed)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 从答案行提取答案内容
     */
    private String extractAnswerFromLine(String line) {
        // 移除"答案："、"答案:"等前缀
        String answer = line.replaceAll("^.*?答案[：:]?\\s*", "").trim();
        return answer.isEmpty() ? "未作答" : answer;
    }
    
    /**
     * 从分数行提取分数
     */
    private Double extractScoreFromLine(String line) {
        Pattern pattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*分");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                log.warn("无法解析分数: {}", matcher.group(1));
            }
        }
        return null;
    }
    
    /**
     * 解析大题结构
     */
    private List<SectionInfo> parseSectionStructure(String content) {
        List<SectionInfo> sections = new ArrayList<>();
        
        // 匹配大题标题：一.、二.、三. 等
        Pattern sectionPattern = Pattern.compile("([一二三四五六七八九十]+)\\s*[.、]\\s*([^\\n]*?)\\s*(?=\\n|$)");
        Matcher sectionMatcher = sectionPattern.matcher(content);
        
        List<SectionMatch> sectionMatches = new ArrayList<>();
        while (sectionMatcher.find()) {
            String sectionNumber = sectionMatcher.group(1);
            String sectionTitle = sectionMatcher.group(2).trim();
            int startPos = sectionMatcher.start();
            
            // 检查是否包含题目数量和分数信息
            if (sectionTitle.contains("题") && (sectionTitle.contains("分") || sectionTitle.contains("共"))) {
                sectionMatches.add(new SectionMatch(sectionNumber, sectionTitle, startPos));
                log.debug("🔍 找到大题部分: {} - {}", sectionNumber, sectionTitle);
            }
        }
        
        // 为每个大题确定内容范围
        for (int i = 0; i < sectionMatches.size(); i++) {
            SectionMatch current = sectionMatches.get(i);
            int endPos = (i + 1 < sectionMatches.size()) ? 
                sectionMatches.get(i + 1).startPos : content.length();
            
            String sectionContent = content.substring(current.startPos, endPos);
            SectionInfo sectionInfo = new SectionInfo(current.sectionNumber, current.sectionTitle, sectionContent);
            
            // 解析该部分的分数信息
            parseSectionScoreInfo(sectionInfo);
            sections.add(sectionInfo);
        }
        
        // 如果没有找到大题结构，将整个内容作为一个部分
        if (sections.isEmpty()) {
            sections.add(new SectionInfo("", "全部题目", content));
        }
        
        return sections;
    }
    
    /**
     * 解析大题部分内的小题 - 修复版：现在专门用于学生答案解析
     * 对于考试模板解析，应该使用 parseQuestionsInTemplateSection 方法
     */
    private List<QuestionInfo> parseQuestionsInSection(SectionInfo section, String fullContent) {
        List<QuestionInfo> questions = new ArrayList<>();
        String sectionContent = section.getContent();
        
        log.info("📚 解析学生答案大题部分: {}", section.getSectionTitle());
        log.debug("📄 大题内容长度: {}", sectionContent.length());
        
        // 第一步：提取所有学生答案和得分
        List<String> studentAnswers = extractStudentAnswersFromSection(sectionContent);
        List<Double> scores = extractScoresFromSection(sectionContent);
        
        log.info("📊 大题 {} - 提取到 {} 个答案，{} 个得分", 
            section.getSectionTitle(), studentAnswers.size(), scores.size());
        
        // 打印前几个答案进行调试
        for (int debugIdx = 0; debugIdx < Math.min(3, studentAnswers.size()); debugIdx++) {
            String answer = studentAnswers.get(debugIdx);
            log.info("🔍 调试答案 {}: \"{}\"", debugIdx + 1, 
                    answer.length() > 100 ? answer.substring(0, 100) + "..." : answer);
        }
        
        // 第二步：提取题目内容 - 使用改进的解析方法，保留选项信息
        List<String> questionContents = extractQuestionContentsImproved(sectionContent, section);
        
        log.info("📝 提取到 {} 个题目内容", questionContents.size());
        
        // 第三步：匹配题目、答案和得分
        int maxCount = Math.max(Math.max(questionContents.size(), studentAnswers.size()), scores.size());
        
        log.info("🔗 开始匹配，最大题目数: {} (题目内容: {}, 学生答案: {}, 得分: {})", 
                maxCount, questionContents.size(), studentAnswers.size(), scores.size());
        
        for (int i = 0; i < maxCount; i++) {
            QuestionInfo questionInfo = new QuestionInfo();
            
            // 设置题目内容
            if (i < questionContents.size()) {
                questionInfo.setQuestionContent(questionContents.get(i));
            } else {
                questionInfo.setQuestionContent("题目内容解析失败");
            }
            
            // 设置学生答案 - 改进的逻辑
            String studentAnswer = "学生未作答";
            if (i < studentAnswers.size()) {
                String extractedAnswer = studentAnswers.get(i);
                if (extractedAnswer != null && !extractedAnswer.trim().isEmpty() && 
                    !extractedAnswer.equals("学生未作答")) {
                    studentAnswer = extractedAnswer;
                } else {
                    log.warn("⚠️ 第{}题答案为空或无效: \"{}\"", i + 1, extractedAnswer);
                }
            } else {
                log.warn("⚠️ 第{}题没有对应的学生答案", i + 1);
            }
            questionInfo.setStudentAnswer(studentAnswer);
            
            // 设置得分
            if (i < scores.size()) {
                questionInfo.setScore(scores.get(i));
            } else {
                // 根据大题信息推断分数
                questionInfo.setScore(inferScoreFromSection(section, questionInfo.getQuestionContent()));
            }
            
            questions.add(questionInfo);
            
            log.debug("✅ 题目{}: 答案=\"{}\" (长度: {}), 分数={}", 
                     i + 1, 
                     studentAnswer.length() > 50 ? studentAnswer.substring(0, 50) + "..." : studentAnswer,
                     studentAnswer.length(),
                     questionInfo.getScore());
        }
        
        return questions;
    }
    
    /**
     * 从大题部分提取学生答案 - 改进版，支持更多答案格式
     */
    private List<String> extractStudentAnswersFromSection(String sectionContent) {
        List<String> answers = new ArrayList<>();
        
        log.info("🔍 开始提取学生答案，内容长度: {}", sectionContent.length());
        
        // 打印内容片段用于调试
        if (sectionContent.length() > 500) {
            log.debug("📄 大题内容前500字符: {}", sectionContent.substring(0, 500));
        } else {
            log.debug("📄 大题完整内容: {}", sectionContent);
        }
        
        // 方法1：标准格式 学生答案：xxx正确答案：或学生答案：xxx学生得分：
        // 修复：处理连续格式，如"学生答案：C正确答案：C"
        Pattern standardPattern = Pattern.compile("学生答案[：:]\\s*([^正确学生]*?)(?=正确答案|学生得分|\\d+\\s*[.、]|$)", Pattern.DOTALL);
        Matcher standardMatcher = standardPattern.matcher(sectionContent);
        
        int matchCount = 0;
        while (standardMatcher.find()) {
            matchCount++;
            String answer = standardMatcher.group(1).trim();
            // 处理跨行的答案内容
            answer = answer.replaceAll("\\s+", " ").trim();
            
            if (answer.isEmpty()) {
                answer = "学生未作答";
                log.warn("🔴 标准格式第{}个匹配为空", matchCount);
            } else {
                log.info("🟢 标准格式第{}个匹配成功: \"{}\"", matchCount, 
                        answer.length() > 30 ? answer.substring(0, 30) + "..." : answer);
            }
            answers.add(answer);
        }
        
        log.info("📊 标准格式共匹配到 {} 个答案", matchCount);
        
        // 方法2：如果标准格式没有找到答案，尝试其他格式
        if (answers.isEmpty()) {
            log.info("🔄 标准格式无结果，尝试'我的答案'格式");
            // 尝试匹配 我的答案：xxx 格式
            Pattern myAnswerPattern = Pattern.compile("我的答案[：:]\\s*([^\\n\\r]*?)(?=正确答案|得分|\\d+\\s*[.、]|$)", Pattern.DOTALL);
            Matcher myAnswerMatcher = myAnswerPattern.matcher(sectionContent);
            
            int myAnswerCount = 0;
            while (myAnswerMatcher.find()) {
                myAnswerCount++;
                String answer = myAnswerMatcher.group(1).trim();
                answer = answer.replaceAll("\\s+", " ").trim();
                
                if (answer.isEmpty()) {
                    answer = "学生未作答";
                    log.warn("🔴 '我的答案'格式第{}个匹配为空", myAnswerCount);
                } else {
                    log.info("🟢 '我的答案'格式第{}个匹配成功: \"{}\"", myAnswerCount, 
                            answer.length() > 30 ? answer.substring(0, 30) + "..." : answer);
                }
                answers.add(answer);
            }
            log.info("📊 '我的答案'格式共匹配到 {} 个答案", myAnswerCount);
        }
        
        // 方法3：如果还是没有找到，尝试匹配答案：xxx 格式
        if (answers.isEmpty()) {
            log.info("🔄 前面格式无结果，尝试'答案'格式");
            Pattern answerPattern = Pattern.compile("答案[：:]\\s*([^\\n\\r]*?)(?=正确|得分|\\d+\\s*[.、]|$)", Pattern.DOTALL);
            Matcher answerMatcher = answerPattern.matcher(sectionContent);
            
            int answerCount = 0;
            while (answerMatcher.find()) {
                answerCount++;
                String answer = answerMatcher.group(1).trim();
                answer = answer.replaceAll("\\s+", " ").trim();
                
                if (answer.isEmpty()) {
                    answer = "学生未作答";
                    log.warn("🔴 '答案'格式第{}个匹配为空", answerCount);
                } else {
                    log.info("🟢 '答案'格式第{}个匹配成功: \"{}\"", answerCount, 
                            answer.length() > 30 ? answer.substring(0, 30) + "..." : answer);
                }
                answers.add(answer);
            }
            log.info("📊 '答案'格式共匹配到 {} 个答案", answerCount);
        }
        
        // 方法4：主观题特殊处理 - 在题目和得分之间查找内容
        if (answers.isEmpty()) {
            String sectionTitle = sectionContent.substring(0, Math.min(100, sectionContent.length()));
            log.info("🔄 前面格式都无结果，尝试主观题特殊处理");
            log.debug("📋 大题标题片段: {}", sectionTitle);
            
            if (sectionTitle.contains("简答题") || sectionTitle.contains("论述题") || sectionTitle.contains("分析题") || sectionTitle.contains("计算题")) {
                log.info("🎯 识别为主观题类型，开始特殊解析");
                // 对于主观题，尝试在题目结束和得分开始之间提取答案
                Pattern subjectivePattern = Pattern.compile("\\d+\\s*[.、]\\s*[\\s\\S]*?(?:正确答案[：:].*?)?\\s*([\\s\\S]*?)\\s*(?=学生得分|\\d+\\s*[.、]|$)");
                Matcher subjectiveMatcher = subjectivePattern.matcher(sectionContent);
                
                int subjectiveCount = 0;
                while (subjectiveMatcher.find()) {
                    subjectiveCount++;
                    String potentialAnswer = subjectiveMatcher.group(1).trim();
                    log.debug("🔍 主观题第{}个原始匹配内容: \"{}\"", subjectiveCount, 
                             potentialAnswer.length() > 100 ? potentialAnswer.substring(0, 100) + "..." : potentialAnswer);
                    
                    // 清理潜在的答案内容
                    potentialAnswer = potentialAnswer.replaceAll("学生答案[：:]", "");
                    potentialAnswer = potentialAnswer.replaceAll("正确答案[：:].*", "");
                    potentialAnswer = potentialAnswer.replaceAll("我的答案[：:]", "");
                    potentialAnswer = potentialAnswer.replaceAll("\\s+", " ").trim();
                    
                    log.debug("🧹 清理后内容: \"{}\"", potentialAnswer.length() > 100 ? potentialAnswer.substring(0, 100) + "..." : potentialAnswer);
                    
                    if (!potentialAnswer.isEmpty() && potentialAnswer.length() > 2 && 
                        !potentialAnswer.matches("^[\\d\\s.、]+$")) { // 不是纯数字和标点
                        answers.add(potentialAnswer);
                        log.info("🟢 主观题第{}个答案提取成功: \"{}\"", subjectiveCount, 
                                potentialAnswer.length() > 50 ? potentialAnswer.substring(0, 50) + "..." : potentialAnswer);
                    } else {
                        answers.add("学生未作答");
                        log.warn("🔴 主观题第{}个答案无效，设为未作答。原因: 长度={}, 内容=\"{}\"", 
                                subjectiveCount, potentialAnswer.length(), potentialAnswer);
                    }
                }
                log.info("📊 主观题特殊处理共解析到 {} 个答案", subjectiveCount);
            } else {
                log.info("🚫 不是主观题类型，跳过特殊处理");
            }
        }
        
        log.info("🎯 学生答案提取完成，总共提取到 {} 个答案", answers.size());
        
        // 汇总所有答案的状态
        int validAnswers = 0;
        int emptyAnswers = 0;
        for (int i = 0; i < answers.size(); i++) {
            String answer = answers.get(i);
            if (answer.equals("学生未作答")) {
                emptyAnswers++;
            } else {
                validAnswers++;
                log.debug("📋 有效答案 {}: \"{}\"", i + 1, 
                         answer.length() > 60 ? answer.substring(0, 60) + "..." : answer);
            }
        }
        
        log.info("📊 答案统计: 有效答案 {} 个，未作答 {} 个", validAnswers, emptyAnswers);
        return answers;
    }
    
    /**
     * 从大题部分提取得分
     */
    private List<Double> extractScoresFromSection(String sectionContent) {
        List<Double> scores = new ArrayList<>();
        
        log.debug("🔍 开始从大题部分提取得分，内容长度: {}", sectionContent.length());
        
        // 扩展正则表达式以匹配更多格式
        String[] scorePatterns = {
            "学生得分[：:]?\\s*(\\d+(?:\\.\\d+)?)\\s*分",    // 标准格式：学生得分：1 分
            "得分[：:]?\\s*(\\d+(?:\\.\\d+)?)\\s*分",       // 简化格式：得分：1 分  
            "分数[：:]?\\s*(\\d+(?:\\.\\d+)?)\\s*分",       // 替代格式：分数：1 分
            "(\\d+(?:\\.\\d+)?)\\s*分",                    // 最简格式：1 分
        };
        
        for (String patternStr : scorePatterns) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(sectionContent);
            
            while (matcher.find()) {
                try {
                    Double score = Double.parseDouble(matcher.group(1));
                    scores.add(score);
                    log.info("📊 提取得分成功: {} (使用模式: {})", score, patternStr);
                } catch (NumberFormatException e) {
                    log.warn("⚠️ 分数解析失败: {}", matcher.group(1));
                    scores.add(0.0);
                }
            }
            
            // 如果已经找到分数，不再尝试其他模式
            if (!scores.isEmpty()) {
                break;
            }
        }
        
        if (scores.isEmpty()) {
            log.warn("⚠️ 未能从大题部分提取到任何分数");
            // 打印内容片段用于调试
            if (sectionContent.length() > 200) {
                log.debug("📄 大题部分内容片段（前200字符）: {}", sectionContent.substring(0, 200));
            } else {
                log.debug("📄 大题部分完整内容: {}", sectionContent);
            }
        }
        
        return scores;
    }

    /**
     * 改进的题目内容提取方法 - 支持学生答案文档解析，保留选项信息
     */
    private List<String> extractQuestionContentsImproved(String sectionContent, SectionInfo section) {
        List<String> contents = new ArrayList<>();
        
        log.debug("📝 使用改进方法提取题目内容，题目类型: {}", section.getSectionTitle());
        
        // 根据题目类型采用不同的解析策略
        String sectionTitle = section.getSectionTitle().toLowerCase();
        
        if (sectionTitle.contains("选择题") || sectionTitle.contains("单选") || sectionTitle.contains("多选")) {
            contents = extractChoiceQuestionsImproved(sectionContent);
        } else if (sectionTitle.contains("判断题") || sectionTitle.contains("判断")) {
            contents = extractTrueFalseQuestionsImproved(sectionContent);
        } else if (sectionTitle.contains("填空题") || sectionTitle.contains("填空")) {
            contents = extractFillBlankQuestionsImproved(sectionContent);
        } else if (sectionTitle.contains("简答题") || sectionTitle.contains("简答")) {
            contents = extractShortAnswerQuestionsImproved(sectionContent);
        } else if (sectionTitle.contains("论述题") || sectionTitle.contains("论述")) {
            contents = extractEssayQuestionsImproved(sectionContent);
        } else if (sectionTitle.contains("计算题") || sectionTitle.contains("计算")) {
            contents = extractCalculationQuestionsImproved(sectionContent);
        } else if (sectionTitle.contains("分析题") || sectionTitle.contains("分析")) {
            contents = extractCaseAnalysisQuestionsImproved(sectionContent);
        } else {
            // 通用解析方法
            contents = extractGeneralQuestionsImproved(sectionContent);
        }
        
        return contents;
    }

    /**
     * 从大题部分提取题目内容 - 已弃用，现在使用统一的解析方法
     */
    @Deprecated
    private List<String> extractQuestionContentsFromSection(String sectionContent, SectionInfo section) {
        // ⚠️ 这个方法有问题：它只提取题目内容，不提取选项
        // 现在应该使用 parseQuestionsWithSequentialValidation 方法
        
        log.warn("⚠️ 使用了已弃用的 extractQuestionContentsFromSection 方法，这会导致选项丢失");
        
        List<String> contents = new ArrayList<>();
        
        // 根据题目类型采用不同的解析策略
        String sectionTitle = section.getSectionTitle().toLowerCase();
        
        if (sectionTitle.contains("选择题") || sectionTitle.contains("单选") || sectionTitle.contains("多选")) {
            contents = extractChoiceQuestions(sectionContent);
        } else if (sectionTitle.contains("判断题") || sectionTitle.contains("判断")) {
            contents = extractTrueFalseQuestions(sectionContent);
        } else if (sectionTitle.contains("填空题") || sectionTitle.contains("填空")) {
            contents = extractFillBlankQuestions(sectionContent);
        } else if (sectionTitle.contains("简答题") || sectionTitle.contains("简答")) {
            contents = extractShortAnswerQuestions(sectionContent);
        } else if (sectionTitle.contains("论述题") || sectionTitle.contains("论述")) {
            contents = extractEssayQuestions(sectionContent);
        } else if (sectionTitle.contains("计算题") || sectionTitle.contains("计算")) {
            contents = extractCalculationQuestions(sectionContent);
        } else {
            // 通用解析方法
            contents = extractGeneralQuestions(sectionContent);
        }
        
        return contents;
    }
    
    /**
     * 提取选择题 - 修复版：同时提取题目和选项
     */
    private List<String> extractChoiceQuestions(String content) {
        List<String> questions = new ArrayList<>();
        
        // 注意：这个方法现在不应该被直接使用
        // 选择题的提取应该通过 parseQuestionsWithSequentialValidation 来处理
        // 以确保选项也被正确提取
        
        // 匹配格式：数字. 题目内容? ( )
        Pattern pattern = Pattern.compile("(\\d+)\\s*[.、]\\s*([^\\n]*?\\?[^\\n]*?\\([\\s]*\\))");
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            String questionContent = matcher.group(2).trim();
            questions.add(questionContent);
            log.debug("🔍 提取选择题: {}", questionContent.length() > 50 ? 
                questionContent.substring(0, 50) + "..." : questionContent);
        }
        
        log.warn("⚠️ 注意：extractChoiceQuestions 方法只提取题目内容，不包含选项。建议使用 parseQuestionsWithSequentialValidation");
        return questions;
    }
    
    /**
     * 提取判断题
     */
    private List<String> extractTrueFalseQuestions(String content) {
        List<String> questions = new ArrayList<>();
        
        // 匹配格式：数字. 题目内容。( )
        Pattern pattern = Pattern.compile("(\\d+)\\s*[.、]\\s*([^\\n]*?\\([\\s]*\\))");
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            String questionContent = matcher.group(2).trim();
            // 判断题特征：不包含问号，但包含括号
            if (!questionContent.contains("?") && !questionContent.contains("？")) {
                questions.add(questionContent);
                log.debug("🔍 提取判断题: {}", questionContent.length() > 50 ? 
                    questionContent.substring(0, 50) + "..." : questionContent);
            }
        }
        
        return questions;
    }
    
    /**
     * 提取填空题
     */
    private List<String> extractFillBlankQuestions(String content) {
        List<String> questions = new ArrayList<>();
        
        // 匹配格式：数字. 题目内容（包含下划线或空格）
        Pattern pattern = Pattern.compile("(\\d+)\\s*[.、]\\s*([^\\n]*?(?:____+|\\s{3,})[^\\n]*)");
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            String questionContent = matcher.group(2).trim();
            questions.add(questionContent);
            log.debug("🔍 提取填空题: {}", questionContent.length() > 50 ? 
                questionContent.substring(0, 50) + "..." : questionContent);
        }
        
        return questions;
    }
    
    /**
     * 提取简答题
     */
    private List<String> extractShortAnswerQuestions(String content) {
        List<String> questions = new ArrayList<>();
        
        // 匹配格式：数字. 开始，直到遇到学生得分或下一题
        Pattern pattern = Pattern.compile("(\\d+)\\s*[.、]\\s*([\\s\\S]*?)(?=\\d+\\s*[.、]|学生得分|$)");
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            String questionContent = matcher.group(2).trim();
            // 清理内容，移除答案部分
            questionContent = cleanQuestionContent(questionContent);
            if (questionContent.length() > 10) { // 过滤过短的内容
                questions.add(questionContent);
                log.debug("🔍 提取简答题: {}", questionContent.length() > 50 ? 
                    questionContent.substring(0, 50) + "..." : questionContent);
            }
        }
        
        return questions;
    }
    
    /**
     * 改进的选择题提取方法 - 包含选项信息
     */
    private List<String> extractChoiceQuestionsImproved(String content) {
        List<String> questions = new ArrayList<>();
        
        // 分行处理，找到完整的题目+选项结构
        String[] lines = content.split("\\n");
        StringBuilder currentQuestion = new StringBuilder();
        boolean collectingOptions = false;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // 检查是否是题目开始：数字. 开头
            if (line.matches("^\\d+\\s*[.、]\\s*.*")) {
                // 保存上一个题目（如果有）
                if (currentQuestion.length() > 0) {
                    questions.add(currentQuestion.toString().trim());
                    log.debug("🔍 提取选择题（含选项）: {}", 
                        currentQuestion.length() > 50 ? 
                        currentQuestion.substring(0, 50) + "..." : currentQuestion);
                }
                
                // 开始新题目
                currentQuestion = new StringBuilder(line);
                collectingOptions = true;
                
            } else if (collectingOptions && line.matches("^[A-Z]\\s*[、.]\\s*.*")) {
                // 选项行
                currentQuestion.append("\n").append(line);
                
            } else if (collectingOptions && (line.contains("学生答案") || line.contains("正确答案") || line.contains("学生得分"))) {
                // 答案信息行，停止收集选项
                collectingOptions = false;
                
            } else if (collectingOptions && !line.contains("学生答案") && !line.contains("正确答案") && !line.contains("学生得分")) {
                // 继续添加题目内容
                currentQuestion.append(" ").append(line);
            }
        }
        
        // 添加最后一个题目
        if (currentQuestion.length() > 0) {
            questions.add(currentQuestion.toString().trim());
            log.debug("🔍 提取选择题（含选项）: {}", 
                currentQuestion.length() > 50 ? 
                currentQuestion.substring(0, 50) + "..." : currentQuestion);
        }
        
        return questions;
    }
    
    /**
     * 改进的判断题提取方法
     */
    private List<String> extractTrueFalseQuestionsImproved(String content) {
        List<String> questions = new ArrayList<>();
        
        // 判断题通常没有选项，直接提取题目内容
        Pattern pattern = Pattern.compile("(\\d+)\\s*[.、]\\s*([^\\n]*?)(?=\\d+\\s*[.、]|学生答案|正确答案|学生得分|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            String questionContent = matcher.group(2).trim();
            // 清理内容
            questionContent = cleanQuestionContentForAnswerParsing(questionContent);
            if (questionContent.length() > 5) {
                questions.add(questionContent);
                log.debug("🔍 提取判断题: {}", questionContent.length() > 50 ? 
                    questionContent.substring(0, 50) + "..." : questionContent);
            }
        }
        
        return questions;
    }
    
    /**
     * 改进的简答题提取方法
     */
    private List<String> extractShortAnswerQuestionsImproved(String content) {
        List<String> questions = new ArrayList<>();
        
        // 简答题通常有较长的题目内容
        Pattern pattern = Pattern.compile("(\\d+)\\s*[.、]\\s*([\\s\\S]*?)(?=\\d+\\s*[.、]|学生得分|$)");
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            String questionContent = matcher.group(2).trim();
            // 清理内容，移除学生答案等信息
            questionContent = cleanQuestionContentForAnswerParsing(questionContent);
            if (questionContent.length() > 10) {
                questions.add(questionContent);
                log.debug("🔍 提取简答题: {}", questionContent.length() > 50 ? 
                    questionContent.substring(0, 50) + "..." : questionContent);
            }
        }
        
        return questions;
    }
    
    /**
     * 改进的论述题提取方法
     */
    private List<String> extractEssayQuestionsImproved(String content) {
        return extractShortAnswerQuestionsImproved(content); // 使用相同的方法
    }
    
    /**
     * 改进的计算题提取方法
     */
    private List<String> extractCalculationQuestionsImproved(String content) {
        return extractShortAnswerQuestionsImproved(content); // 使用相同的方法
    }
    
    /**
     * 改进的案例分析题提取方法
     */
    private List<String> extractCaseAnalysisQuestionsImproved(String content) {
        return extractShortAnswerQuestionsImproved(content); // 使用相同的方法
    }
    
    /**
     * 改进的填空题提取方法
     */
    private List<String> extractFillBlankQuestionsImproved(String content) {
        List<String> questions = new ArrayList<>();
        
        // 填空题可能包含下划线或空格
        Pattern pattern = Pattern.compile("(\\d+)\\s*[.、]\\s*([^\\n]*?)(?=\\d+\\s*[.、]|学生答案|正确答案|学生得分|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            String questionContent = matcher.group(2).trim();
            questionContent = cleanQuestionContentForAnswerParsing(questionContent);
            if (questionContent.length() > 5) {
                questions.add(questionContent);
                log.debug("🔍 提取填空题: {}", questionContent.length() > 50 ? 
                    questionContent.substring(0, 50) + "..." : questionContent);
            }
        }
        
        return questions;
    }
    
    /**
     * 改进的通用题目提取方法
     */
    private List<String> extractGeneralQuestionsImproved(String content) {
        List<String> questions = new ArrayList<>();
        
        // 通用模式：数字. 开始的行
        Pattern pattern = Pattern.compile("(\\d+)\\s*[.、]\\s*([\\s\\S]*?)(?=\\d+\\s*[.、]|学生得分|$)");
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            String questionContent = matcher.group(2).trim();
            questionContent = cleanQuestionContentForAnswerParsing(questionContent);
            if (isValidQuestionContentForAnswerParsing(questionContent)) {
                questions.add(questionContent);
                log.debug("🔍 提取通用题目: {}", questionContent.length() > 50 ? 
                    questionContent.substring(0, 50) + "..." : questionContent);
            }
        }
        
        return questions;
    }
    
    /**
     * 为答案解析清理题目内容
     */
    private String cleanQuestionContentForAnswerParsing(String content) {
        if (content == null) return "";
        
        // 移除学生答案和得分信息，但保留题目内容和选项
        String cleaned = content.replaceAll("学生答案[：:].*?(?=正确答案|学生得分|$)", "");
        cleaned = cleaned.replaceAll("正确答案[：:].*?(?=学生得分|$)", "");
        cleaned = cleaned.replaceAll("学生得分[：:].*", "");
        cleaned = cleaned.replaceAll("批语[：:].*", "");
        
        // 规范化空格和换行，但保留选项的换行结构
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        return cleaned;
    }
    
    /**
     * 验证是否是有效的题目内容（用于答案解析）
     */
    private boolean isValidQuestionContentForAnswerParsing(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = content.trim();
        
        // 过滤明显不是题目的内容
        if (trimmed.matches("^\\d+(\\.\\d+)*$") || // 纯数字
            trimmed.length() < 5 || // 太短
            trimmed.contains("学生答案") || trimmed.contains("学生得分") || // 答案信息
            trimmed.contains("正确答案") || trimmed.contains("批语")) { // 评分信息
            return false;
        }
        
        return true;
    }

    /**
     * 提取论述题
     */
    private List<String> extractEssayQuestions(String content) {
        return extractShortAnswerQuestions(content); // 使用相同的方法
    }
    
    /**
     * 提取计算题
     */
    private List<String> extractCalculationQuestions(String content) {
        return extractShortAnswerQuestions(content); // 使用相同的方法
    }
    
    /**
     * 通用题目提取方法
     */
    private List<String> extractGeneralQuestions(String content) {
        List<String> questions = new ArrayList<>();
        
        // 通用模式：数字. 开始的行
        Pattern pattern = Pattern.compile("(\\d+)\\s*[.、]\\s*([^\\n]+)");
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            String questionContent = matcher.group(2).trim();
            // 过滤明显不是题目的内容
            if (!isValidQuestionContent(questionContent)) {
                continue;
            }
            
            questions.add(questionContent);
            log.debug("🔍 提取通用题目: {}", questionContent.length() > 50 ? 
                questionContent.substring(0, 50) + "..." : questionContent);
        }
        
        return questions;
    }
    
    /**
     * 验证是否是有效的题目内容
     */
    private boolean isValidQuestionContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = content.trim();
        
        // 过滤IP地址
        if (trimmed.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+.*")) {
            return false;
        }
        
        // 过滤纯数字
        if (trimmed.matches("^\\d+(\\.\\d+)*$")) {
            return false;
        }
        
        // 过滤选项行（A、B、C、D）
        if (trimmed.matches("^[A-Z]、\\s*.*") && trimmed.length() < 100) {
            return false;
        }
        
        // 过滤学生信息行
        if (trimmed.contains("学生答案") || trimmed.contains("学生得分") || 
            trimmed.contains("正确答案") || trimmed.contains("批语")) {
            return false;
        }
        
        // 题目应该有一定长度
        if (trimmed.length() < 5) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 清理题目内容
     */
    private String cleanQuestionContent(String content) {
        if (content == null) return "";
        
        // 移除学生答案和得分信息
        String cleaned = content.replaceAll("学生答案[：:].*", "");
        cleaned = cleaned.replaceAll("学生得分[：:].*", "");
        cleaned = cleaned.replaceAll("正确答案[：:].*", "");
        cleaned = cleaned.replaceAll("批语[：:].*", "");
        
        // 规范化空格和换行
         cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
         return cleaned;
    }
     
    /**
     * 从大题信息推断题目分数
     */
    private Double inferScoreFromSection(SectionInfo section, String questionContent) {
        // 如果大题有分数信息，使用该信息
        if (section.getScorePerQuestion() != null) {
            return section.getScorePerQuestion();
        }
        
        // 根据题目类型推断默认分数
        String sectionTitle = section.getSectionTitle().toLowerCase();
        if (sectionTitle.contains("选择题") || sectionTitle.contains("单选")) {
            return 1.0; // 选择题通常1分
        } else if (sectionTitle.contains("判断题")) {
            return 0.5; // 判断题通常0.5分
        } else if (sectionTitle.contains("填空题")) {
            return 2.0; // 填空题通常2分
        } else if (sectionTitle.contains("简答题")) {
            return 4.0; // 简答题通常4分
        } else if (sectionTitle.contains("论述题")) {
            return 15.0; // 论述题通常15分
        } else if (sectionTitle.contains("计算题")) {
            return 10.0; // 计算题通常10分
        }
        
        return 5.0; // 默认5分
    }
    
    /**
     * 解析大题的分数信息
     */
    private void parseSectionScoreInfo(SectionInfo section) {
        String title = section.getSectionTitle();
        
        // 匹配各种分数格式
        Pattern[] patterns = {
            Pattern.compile("共\\s*(\\d+)\\s*题[，,]\\s*(\\d+)\\s*分"), // 共25题,25分
            Pattern.compile("\\(\\s*共\\s*(\\d+)\\s*题[，,]\\s*(\\d+)\\s*分\\s*\\)"), // (共25题,25分)
            Pattern.compile("每题\\s*(\\d+(?:\\.\\d+)?)\\s*分"), // 每题1分
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(title);
            if (matcher.find()) {
                try {
                    if (pattern.pattern().contains("共.*题")) {
                        int questionCount = Integer.parseInt(matcher.group(1));
                        int totalScore = Integer.parseInt(matcher.group(2));
                        section.setQuestionCount(questionCount);
                        section.setTotalScore(totalScore);
                        if (questionCount > 0) {
                            section.setScorePerQuestion((double) totalScore / questionCount);
                        }
                        log.debug("📊 大题分数信息: 共{}题，总{}分，每题{}分", 
                            questionCount, totalScore, section.getScorePerQuestion());
                    } else {
                        double scorePerQuestion = Double.parseDouble(matcher.group(1));
                        section.setScorePerQuestion(scorePerQuestion);
                        log.debug("📊 大题分数信息: 每题{}分", scorePerQuestion);
                    }
                    break;
                } catch (NumberFormatException e) {
                    log.warn("解析大题分数失败: {}", matcher.group());
                }
            }
        }
    }
    
    /**
     * 大题匹配结果（用于学生答案解析）
     */
    private static class SectionMatch {
        String sectionNumber;
        String sectionTitle;
        int startPos;
        
        SectionMatch(String sectionNumber, String sectionTitle, int startPos) {
            this.sectionNumber = sectionNumber;
            this.sectionTitle = sectionTitle;
            this.startPos = startPos;
        }
    }
    
    /**
     * 大题信息
     */
    private static class SectionInfo {
        private String sectionNumber;
        private String sectionTitle;
        private String content;
        private Integer questionCount;
        private Integer totalScore;
        private Double scorePerQuestion;
        
        public SectionInfo(String sectionNumber, String sectionTitle, String content) {
            this.sectionNumber = sectionNumber;
            this.sectionTitle = sectionTitle;
            this.content = content;
        }
        
        // Getters and Setters
        public String getSectionNumber() { return sectionNumber; }
        public String getSectionTitle() { return sectionTitle; }
        public String getContent() { return content; }
        public Integer getQuestionCount() { return questionCount; }
        public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }
        public Integer getTotalScore() { return totalScore; }
        public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }
        public Double getScorePerQuestion() { return scorePerQuestion; }
        public void setScorePerQuestion(Double scorePerQuestion) { this.scorePerQuestion = scorePerQuestion; }
    }
    
    /**
     * 题目信息
     */
    private static class QuestionInfo {
        private String questionContent;
        private String studentAnswer;
        private Double score;
        
        // Getters and Setters
        public String getQuestionContent() { return questionContent; }
        public void setQuestionContent(String questionContent) { this.questionContent = questionContent; }
        public String getStudentAnswer() { return studentAnswer; }
        public void setStudentAnswer(String studentAnswer) { this.studentAnswer = studentAnswer; }
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
    }
    
    public List<String> getAvailableSubjects(String uploadDir) {
        List<String> subjects = new ArrayList<>();
        File baseDir = new File(uploadDir, "answer");
        if (baseDir.exists() && baseDir.isDirectory()) {
            File[] subjectDirs = baseDir.listFiles(File::isDirectory);
            if (subjectDirs != null) {
                for (File subjectDir : subjectDirs) {
                    subjects.add(subjectDir.getName());
                }
            }
        }
        return subjects;
    }

    public List<String> getClassFolders(String uploadDir, String subject) {
        List<String> classFolders = new ArrayList<>();
        File subjectDir = new File(uploadDir, "answer/" + subject);
        if (subjectDir.exists() && subjectDir.isDirectory()) {
            File[] classFolderDirs = subjectDir.listFiles(File::isDirectory);
            if (classFolderDirs != null) {
                for (File classFolderDir : classFolderDirs) {
                    classFolders.add(classFolderDir.getName());
                }
            }
        }
        return classFolders;
    }

    public List<File> getAnswerDocuments(String uploadDir, String subject, String classFolder) {
        List<File> documents = new ArrayList<>();
        File classFolderDir = new File(uploadDir, "answer/" + subject + "/" + classFolder);
        if (classFolderDir.exists() && classFolderDir.isDirectory()) {
            addFilesFromDir(documents, classFolderDir);
        }
        return documents;
    }

    public List<File> getAllAnswerDocumentsForSubject(String uploadDir, String subject) {
        List<File> documents = new ArrayList<>();
        File subjectDir = new File(uploadDir, "answer/" + subject);
        if (subjectDir.exists() && subjectDir.isDirectory()) {
            File[] classFolderDirs = subjectDir.listFiles(File::isDirectory);
            if (classFolderDirs != null) {
                for (File classFolderDir : classFolderDirs) {
                    addFilesFromDir(documents, classFolderDir);
                }
            }
        }
        return documents;
    }

    private void addFilesFromDir(List<File> documents, File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        // 首先检查是否有converted文件夹
        File convertedDir = new File(dir, "converted");
        Set<String> convertedFileNames = new HashSet<>();
        
        if (convertedDir.exists() && convertedDir.isDirectory()) {
            log.debug("🔍 发现converted文件夹: {}", convertedDir.getAbsolutePath());
            // 收集converted文件夹中的文件名（去掉扩展名进行比较）
            File[] convertedFiles = convertedDir.listFiles();
            if (convertedFiles != null) {
                for (File convertedFile : convertedFiles) {
                    if (!convertedFile.isDirectory()) {
                        String fileName = convertedFile.getName().toLowerCase();
                        if (fileName.endsWith(".doc") || fileName.endsWith(".docx") || fileName.endsWith(".wps")) {
                            // 优先使用转换后的文档
                            documents.add(convertedFile);
                            log.info("✅ 优先使用转换后文档: {}", convertedFile.getName());
                            
                            // 记录已转换的文件名（去掉扩展名）
                            String baseFileName = fileName.replaceAll("\\.(doc|docx|wps)$", "");
                            convertedFileNames.add(baseFileName);
                        }
                    }
                }
            }
        }
        
        // 然后处理其他文件，跳过已经有转换版本的文档
        for (File file : files) {
            if (file.isDirectory()) {
                // 跳过 converted 文件夹（已经处理过了）
                if (!file.getName().equals("converted")) {
                    addFilesFromDir(documents, file);
                }
            } else {
                String fileName = file.getName().toLowerCase();
                if (fileName.endsWith(".doc") || fileName.endsWith(".docx") || fileName.endsWith(".wps")) {
                    // 检查是否已经有转换后的版本
                    String baseFileName = fileName.replaceAll("\\.(doc|docx|wps)$", "");
                    
                    if (convertedFileNames.contains(baseFileName)) {
                        log.debug("⏭️ 跳过原文档（已有转换版本）: {}", file.getName());
                    } else {
                        documents.add(file);
                        log.debug("📄 添加原文档: {}", file.getName());
                    }
                }
            }
        }
    }

    private static String extractStudentAnswerFromLine(String line) {
        if (line == null) return "";
        return line.replaceAll("我的答案[：:]?\\s*", "")
                   .replaceAll("【答案】\\s*", "")
                   .replaceAll("答案：\\s*", "")
                   .replaceAll("回答：\\s*", "")
                   .trim();
    }

    /**
     * 从学习通格式的行中提取学生答案
     * 格式: "学生答案：C正确答案：C"
     */
    private static String extractStudentAnswerFromLearningFormat(String line) {
        if (line == null) return "";
        
        // 处理学习通格式: 学生答案：C正确答案：C
        if (line.contains("学生答案")) {
            String pattern = "学生答案[：:]?\\s*([^正确答案\\n\\r]*).*";
            java.util.regex.Pattern compiledPattern = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = compiledPattern.matcher(line);
            if (matcher.find()) {
                String answer = matcher.group(1);
                if (answer != null) {
                    return answer.trim();
                }
            }
            
            // 备用方案：简单分割
            String[] parts = line.split("学生答案[：:]?\\s*");
            if (parts.length > 1) {
                String answerPart = parts[1];
                // 移除"正确答案"后的内容
                if (answerPart.contains("正确答案")) {
                    answerPart = answerPart.split("正确答案")[0];
                }
                return answerPart.trim();
            }
        }
        
        // 原有的格式
        return extractStudentAnswerFromLine(line);
    }

    /**
     * 专门用于基于模板导入的学生答案解析
     * 只解析学生信息、答案和分数，不解析题目内容
     * @param file 学习通答案文档文件
     * @param expectedQuestionCount 模板题目数量，用于验证
     * @return 只包含学生答案和分数的导入数据
     * @throws IOException 文件读取异常
     */
    public StudentAnswerImportData parseStudentAnswersOnlyForTemplate(File file, int expectedQuestionCount) throws IOException {
        log.info("🚀 开始基于模板解析学生答案: {} (期望题目数: {})", file.getName(), expectedQuestionCount);
        
        String content = extractTextFromDocument(file);
        if (content == null || content.trim().isEmpty()) {
            log.warn("⚠️ 无法提取文档内容或内容为空: {}", file.getName());
            return null;
        }
        
        log.info("📄 成功提取文档内容，长度: {} 字符", content.length());
        
        return parseStudentAnswersOnly(content, file.getName(), expectedQuestionCount);
    }

    /**
     * 解析学生答案内容（仅答案和分数，不包含题目内容）
     */
    private StudentAnswerImportData parseStudentAnswersOnly(String content, String fileName, int expectedQuestionCount) {
        StudentAnswerImportData importData = new StudentAnswerImportData();
        
        // 清理内容，过滤图片和乱码
        String cleanedContent = cleanDocumentContent(content);
        
        // 1. 解析学生信息
        parseStudentInfoFromContent(cleanedContent, importData);
        parseStudentInfoFromFileName(fileName, importData);
        
        // 2. 只解析学生答案和分数，不解析题目内容
        parseAnswersOnlyFromContent(cleanedContent, importData, expectedQuestionCount);
        
        return importData;
    }

    /**
     * 从文档内容中只解析学生答案和分数，不解析题目内容
     * 采用简单的逐行解析，专门针对学习通标准格式，支持多行主观题答案
     */
    private void parseAnswersOnlyFromContent(String content, StudentAnswerImportData importData, int expectedQuestionCount) {
        List<StudentAnswerImportData.QuestionAnswer> answers = new ArrayList<>();
        
        log.info("🔍 开始简化解析学生答案（基于模板，期望题目数: {}）", expectedQuestionCount);
        
        // 按行分割内容
        String[] lines = content.split("\\n");
        log.info("📄 文档总行数: {}", lines.length);
        
        int currentQuestionNumber = 0;
        StringBuilder currentAnswerBuilder = new StringBuilder();
        Double currentScore = null;
        boolean collectingAnswer = false; // 是否正在收集多行答案
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // 只有在非答案收集状态下才检查题目开始行
            if (!collectingAnswer && line.matches("^\\d+\\..*")) {
                // 保存上一题的数据（如果有的话）
                if (currentQuestionNumber > 0) {
                    String finalAnswer = currentAnswerBuilder.toString().trim();
                    if (finalAnswer.isEmpty()) {
                        finalAnswer = "学生未作答";
                    }
                    
                    StudentAnswerImportData.QuestionAnswer qa = new StudentAnswerImportData.QuestionAnswer();
                    qa.setQuestionNumber(currentQuestionNumber);
                    qa.setAnswerContent(finalAnswer);
                    qa.setScore(currentScore);
                    qa.setQuestionContent(null); // 题目内容来自模板
                    answers.add(qa);
                    
                    log.debug("✅ 第{}题解析完成 - 答案: \"{}\", 分数: {}", 
                             currentQuestionNumber, 
                             finalAnswer.length() > 50 ? finalAnswer.substring(0, 50) + "..." : finalAnswer, 
                             currentScore);
                }
                
                // 开始新题
                currentQuestionNumber++;
                currentAnswerBuilder = new StringBuilder();
                currentScore = null;
                collectingAnswer = false;
                
                log.debug("📝 开始解析第{}题", currentQuestionNumber);
                continue;
            }
            
            // 检查是否是学生答案行的开始
            if (line.contains("学生答案：") || line.contains("学生答案:")) {
                String answerFromLine = extractAnswerFromLearningLine(line);
                currentAnswerBuilder.append(answerFromLine);
                
                // 检查是否在同一行就包含了完整的答案（包含"正确答案"）
                if (line.contains("正确答案：") || line.contains("正确答案:")) {
                    // 单行完整答案，不需要继续收集
                    collectingAnswer = false;
                    log.debug("📍 第{}题 - 单行完整答案: \"{}\"", currentQuestionNumber, answerFromLine);
                } else {
                    // 多行答案，开始收集
                    collectingAnswer = true;
                    log.debug("📍 第{}题 - 开始收集多行答案: \"{}\"", currentQuestionNumber, answerFromLine);
                }
                continue;
            }
            
            // 如果正在收集答案，检查是否应该停止
            if (collectingAnswer) {
                // 遇到学生得分行时停止收集
                if (line.contains("学生得分：") || line.contains("学生得分:")) {
                    collectingAnswer = false;
                    currentScore = extractScoreFromLearningLine(line);
                    log.debug("📊 第{}题 - 提取分数: {}", currentQuestionNumber, currentScore);
                    continue;
                }
                
                // 遇到"正确答案"行时停止收集
                if (line.contains("正确答案：") || line.contains("正确答案:")) {
                    collectingAnswer = false;
                    log.debug("📍 第{}题 - 遇到正确答案，停止收集", currentQuestionNumber);
                    continue;
                }
                
                // 继续收集答案内容（包括空行，因为可能是答案的一部分）
                if (currentAnswerBuilder.length() > 0) {
                    currentAnswerBuilder.append("\n");
                }
                currentAnswerBuilder.append(line);
                log.debug("📍 第{}题 - 追加答案行: \"{}\"", currentQuestionNumber, line);
                continue;
            }
            
            // 检查是否是学生得分行（如果没有在收集答案时处理）
            if (!collectingAnswer && (line.contains("学生得分：") || line.contains("学生得分:"))) {
                currentScore = extractScoreFromLearningLine(line);
                log.debug("📊 第{}题 - 提取分数: {}", currentQuestionNumber, currentScore);
                continue;
            }
        }
        
        // 保存最后一题的数据
        if (currentQuestionNumber > 0) {
            String finalAnswer = currentAnswerBuilder.toString().trim();
            if (finalAnswer.isEmpty()) {
                finalAnswer = "学生未作答";
            }
            
            StudentAnswerImportData.QuestionAnswer qa = new StudentAnswerImportData.QuestionAnswer();
            qa.setQuestionNumber(currentQuestionNumber);
            qa.setAnswerContent(finalAnswer);
            qa.setScore(currentScore);
            qa.setQuestionContent(null); // 题目内容来自模板
            answers.add(qa);
            
            log.debug("✅ 第{}题（最后一题）解析完成 - 答案: \"{}\", 分数: {}", 
                     currentQuestionNumber,
                     finalAnswer.length() > 50 ? finalAnswer.substring(0, 50) + "..." : finalAnswer,
                     currentScore);
        }
        
        importData.setAnswers(answers);
        log.info("🎯 简化解析完成，成功解析 {} 道题目的答案", answers.size());
    }
    
    /**
     * 从学习通答案行中提取答案（专门处理标准格式）
     * 格式：学生答案：C正确答案：C 或 学生答案： √ 正确答案： √
     */
    private String extractAnswerFromLearningLine(String line) {
        // 直接匹配学习通标准格式
        if (line.contains("学生答案：")) {
            String[] parts = line.split("学生答案：");
            if (parts.length > 1) {
                String answerPart = parts[1];
                // 如果包含"正确答案"，分割掉
                if (answerPart.contains("正确答案")) {
                    answerPart = answerPart.split("正确答案")[0];
                }
                String answer = answerPart.trim();
                
                // 如果答案为空，返回未作答
                if (answer.isEmpty()) {
                    return "学生未作答";
                }
                
                return answer;
            }
        }
        
        // 处理冒号格式
        if (line.contains("学生答案:")) {
            String[] parts = line.split("学生答案:");
            if (parts.length > 1) {
                String answerPart = parts[1];
                if (answerPart.contains("正确答案")) {
                    answerPart = answerPart.split("正确答案")[0];
                }
                String answer = answerPart.trim();
                
                if (answer.isEmpty()) {
                    return "学生未作答";
                }
                
                return answer;
            }
        }
        
        return "学生未作答";
    }
    
    /**
     * 从学习通得分行中提取分数
     * 格式：学生得分：1 分
     */
    private Double extractScoreFromLearningLine(String line) {
        // 匹配学生得分格式
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("学生得分[：:]\\s*(\\d+(?:\\.\\d+)?)\\s*分");
        java.util.regex.Matcher matcher = pattern.matcher(line);
        
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                log.warn("⚠️ 分数解析失败: {}", matcher.group(1));
            }
        }
        
        return null;
    }

    /**
     * 专门用于嵌套压缩包导入的答案解析 - 直接将整个文档内容作为答案
     * @param file 答案文档文件
     * @return 包含完整文档内容的答案数据
     * @throws IOException 文件读取异常
     */
    public StudentAnswerImportData parseNestedZipAnswerDocument(File file) throws IOException {
        log.info("🚀 开始解析嵌套压缩包答案文档: {}", file.getName());
        
        StudentAnswerImportData importData = new StudentAnswerImportData();
        String fileName = file.getName();
        String fileExtension = getFileExtension(fileName);
        
        try {
            String content = null;
            String contentType = "未知";
            
            // 首先检查是否是图片文件，优先使用OCR
            if (ocrService.isSupportedImageFormat(fileName)) {
                log.info("🖼️ 检测到图片文件，使用OCR识别: {}", fileName);
                contentType = "图片+OCR文字识别";
                content = ocrService.extractTextFromImage(file);
                
                if (content != null && !content.trim().isEmpty() && 
                    !content.contains("OCR识别失败") && !content.contains("OCR处理异常")) {
                    log.info("✅ 图片OCR识别成功: {}, 识别文字长度: {} 字符", fileName, content.length());
                } else {
                    log.warn("⚠️ 图片OCR识别失败或内容为空: {}", fileName);
                    if (content == null || content.trim().isEmpty()) {
                        content = "图片OCR识别未能提取到文字内容";
                    }
                }
            } else {
                // 尝试提取文档内容
                contentType = "文档内容提取";
                content = extractTextFromDocument(file);
            }
            
            if (content != null && !content.trim().isEmpty()) {
                // 内容解析成功，将整个内容作为答案
                String answerContent = String.format("【文件名】: %s\n【文件格式】: %s\n【文件大小】: %.1f KB\n【处理方式】: %s\n\n【答案内容】:\n%s", 
                    fileName, 
                    fileExtension.toUpperCase(), 
                    file.length() / 1024.0,
                    contentType,
                    content.trim());
                
                StudentAnswerImportData.QuestionAnswer qa = new StudentAnswerImportData.QuestionAnswer();
                qa.setQuestionContent("学生提交的答案文档");
                qa.setAnswerContent(answerContent);
                qa.setScore(5.0);
                
                importData.setAnswers(List.of(qa));
                log.info("✅ 内容解析成功: {}, 内容长度: {} 字符", fileName, content.length());
                
            } else {
                // 内容为空或解析失败
                String answerContent = String.format("【文件名】: %s\n【文件格式】: %s\n【文件大小】: %.1f KB\n【处理方式】: %s\n【状态】: 内容为空或无法解析", 
                    fileName, 
                    fileExtension.toUpperCase(), 
                    file.length() / 1024.0,
                    contentType);
                
                StudentAnswerImportData.QuestionAnswer qa = new StudentAnswerImportData.QuestionAnswer();
                qa.setQuestionContent("学生提交的答案文档");
                qa.setAnswerContent(answerContent);
                qa.setScore(0.0);
                
                importData.setAnswers(List.of(qa));
                log.warn("⚠️ 内容为空: {}", fileName);
            }
            
        } catch (Exception e) {
            // 文档解析出错，记录文件信息
            String contentType = ocrService.isSupportedImageFormat(fileName) ? "图片+OCR文字识别" : "文档内容提取";
            String answerContent = String.format("【文件名】: %s\n【文件格式】: %s\n【文件大小】: %.1f KB\n【处理方式】: %s\n【状态】: 解析失败\n【错误信息】: %s", 
                fileName, 
                fileExtension.toUpperCase(), 
                file.length() / 1024.0,
                contentType,
                e.getMessage());
            
            StudentAnswerImportData.QuestionAnswer qa = new StudentAnswerImportData.QuestionAnswer();
            qa.setQuestionContent("学生提交的答案文档");
            qa.setAnswerContent(answerContent);
            qa.setScore(0.0);
            
            importData.setAnswers(List.of(qa));
            log.error("❌ 文档解析失败: {}, 错误: {}", fileName, e.getMessage());
        }
        
        return importData;
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "未知";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
} 