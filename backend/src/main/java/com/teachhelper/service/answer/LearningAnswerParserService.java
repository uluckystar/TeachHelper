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

    /**
     * 解析学习通答案文档
     * @param file 文档文件
     * @return 解析后的学生答案数据
     */
    public StudentAnswerImportData parseLearningAnswerDocument(File file) throws IOException {
        String content = extractTextFromDocument(file);
        
        // 如果内容为null，表示无法解析该文件
        if (content == null) {
            log.warn("跳过无法解析的文档: {}", file.getName());
            return null;
        }
        
        return parseAnswerContent(content, file.getName());
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
        parseAnswersFromContent(content, importData);
        
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
     * 解析学习通考试结果格式的答案 - 简化版
     * 核心思路：题目数量 = "学生答案："数量，直接按顺序匹配
     */
    private void parseLearningAnswers(String content, List<StudentAnswerImportData.QuestionAnswer> answers) {
        // 1. 提取所有"学生答案："
        List<String> studentAnswers = extractAllStudentAnswers(content);
        log.info("提取到 {} 个学生答案", studentAnswers.size());
        
        // 2. 提取所有"学生得分："
        List<Double> scores = extractAllScores(content);
        log.info("提取到 {} 个得分", scores.size());
        
        // 3. 按顺序生成题目答案
        for (int i = 0; i < studentAnswers.size(); i++) {
            StudentAnswerImportData.QuestionAnswer qa = new StudentAnswerImportData.QuestionAnswer();
            
            // 设置题目编号（从1开始）
            qa.setQuestionNumber(i + 1);
            
            // 设置答案内容
            qa.setAnswerContent(studentAnswers.get(i));
            
            // 设置得分（如果有）
            if (i < scores.size() && scores.get(i) != null) {
                qa.setScore(scores.get(i));
                log.debug("题目 {} 设置得分: {}", i + 1, scores.get(i));
            }
            
            // 设置题目内容（使用题目编号作为标识，避免空内容）
            qa.setQuestionContent("题目" + (i + 1));
            
            answers.add(qa);
            
            log.debug("题目 {}: 答案={}, 得分={}", 
                     i + 1, 
                     studentAnswers.get(i), 
                     i < scores.size() ? scores.get(i) : "无");
        }
        
        log.info("最终生成 {} 道题目答案", answers.size());
    }
    
    /**
     * 提取所有"学生答案："内容 - 按位置顺序
     */
    private List<String> extractAllStudentAnswers(String content) {
        List<AnswerMatch> answerMatches = new ArrayList<>();
        
        // 方案1：标准格式 - 学生答案：(内容)正确答案：
        Pattern standardPattern = Pattern.compile("学生答案[：:]([^正确答案]*?)正确答案[：:]");
        Matcher standardMatcher = standardPattern.matcher(content);
        
        while (standardMatcher.find()) {
            String answer = standardMatcher.group(1).trim();
            if (answer.isEmpty()) {
                answer = "学生未作答";
            }
            answerMatches.add(new AnswerMatch(standardMatcher.start(), answer, "标准格式"));
        }
        
        // 方案2：主观题格式 - 学生答案：(多行内容)正确答案：
        Pattern subjectivePattern = Pattern.compile("学生答案[：:]([\\s\\S]*?)正确答案[：:]", Pattern.DOTALL);
        Matcher subjectiveMatcher = subjectivePattern.matcher(content);
        
        while (subjectiveMatcher.find()) {
            String answer = subjectiveMatcher.group(1).trim();
            if (answer.isEmpty()) {
                answer = "学生未作答";
            }
            answerMatches.add(new AnswerMatch(subjectiveMatcher.start(), answer, "主观题格式"));
        }
        
        // 方案3：兜底格式 - 学生答案：(多行内容到学生得分或文档结尾)
        Pattern fallbackPattern = Pattern.compile("学生答案[：:]([\\s\\S]*?)(?=学生得分|批语|$)", Pattern.DOTALL);
        Matcher fallbackMatcher = fallbackPattern.matcher(content);
        
        while (fallbackMatcher.find()) {
            String answer = fallbackMatcher.group(1).trim();
            if (answer.isEmpty()) {
                answer = "学生未作答";
            }
            if (!answer.contains("正确答案")) {
                answerMatches.add(new AnswerMatch(fallbackMatcher.start(), answer, "兜底格式"));
            }
        }
        
        // 按位置排序
        answerMatches.sort((a, b) -> Integer.compare(a.position, b.position));
        
        // 去重：如果两个匹配位置相同，优先选择标准格式，然后是主观题格式
        List<AnswerMatch> uniqueMatches = new ArrayList<>();
        for (AnswerMatch match : answerMatches) {
            boolean isDuplicate = false;
            for (AnswerMatch existing : uniqueMatches) {
                if (Math.abs(match.position - existing.position) < 10) { // 允许小范围位置差异
                    isDuplicate = true;
                    // 如果新匹配优先级更高，替换现有匹配
                    if (getFormatPriority(match.format) > getFormatPriority(existing.format)) {
                        uniqueMatches.remove(existing);
                        uniqueMatches.add(match);
                    }
                    break;
                }
            }
            if (!isDuplicate) {
                uniqueMatches.add(match);
            }
        }
        
        // 再次按位置排序
        uniqueMatches.sort((a, b) -> Integer.compare(a.position, b.position));
        
        // 提取答案并记录日志
        List<String> answers = new ArrayList<>();
        for (int i = 0; i < uniqueMatches.size(); i++) {
            AnswerMatch match = uniqueMatches.get(i);
            answers.add(match.answer);
            log.debug("找到学生答案({}) {}: {}", match.format, i + 1, 
                     match.answer.length() > 100 ? match.answer.substring(0, 100) + "..." : match.answer);
        }
        
        log.info("总共提取到 {} 个学生答案", answers.size());
        return answers;
    }
    
    /**
     * 答案匹配结果
     */
    private static class AnswerMatch {
        int position;
        String answer;
        String format;
        
        AnswerMatch(int position, String answer, String format) {
            this.position = position;
            this.answer = answer;
            this.format = format;
        }
    }
    
    /**
     * 获取格式优先级
     */
    private int getFormatPriority(String format) {
        switch (format) {
            case "标准格式": return 3;
            case "主观题格式": return 2;
            case "兜底格式": return 1;
            default: return 0;
        }
    }
    
    /**
     * 提取所有"学生得分："
     */
    private List<Double> extractAllScores(String content) {
        List<Double> scores = new ArrayList<>();
        
        // 正则表达式：学生得分：数字 分
        Pattern pattern = Pattern.compile("学生得分[：:]?\\s*(\\d+(?:\\.\\d+)?)\\s*分");
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            try {
                Double score = Double.parseDouble(matcher.group(1));
                scores.add(score);
                log.debug("找到学生得分 {}: {}", scores.size(), score);
            } catch (NumberFormatException e) {
                scores.add(null);
                log.warn("无法解析得分: {}", matcher.group(1));
            }
        }
        
        return scores;
    }

    /**
     * 题目信息类，用于组织题目数据
     */
    private static class QuestionInfo {
        private String title;
        private String questionNumber;
        private String section;
        private StringBuilder content = new StringBuilder();
        private String studentAnswer;
        private Double score;
        private int lineNumber;
        
        public QuestionInfo(String title, String section, int lineNumber) {
            this.title = title;
            this.section = section;
            this.lineNumber = lineNumber;
        }
        
        public void addContent(String line) {
            if (content.length() > 0) {
                content.append("\n");
            }
            content.append(line);
        }
        
        public void processAnswer() {
            // 如果没有找到明确的学生答案，尝试从内容末尾提取
            if (studentAnswer == null && content.length() > 0) {
                String contentStr = content.toString();
                String[] lines = contentStr.split("\n");
                
                // 从后往前找答案
                for (int i = lines.length - 1; i >= 0; i--) {
                    String line = lines[i].trim();
                    if (line.startsWith("我的答案") || line.startsWith("【答案】") || 
                        line.startsWith("答案：") || line.startsWith("回答：") || line.contains("学生答案")) {
                        studentAnswer = extractStudentAnswerFromLearningFormat(line);
                        
                        // 从内容中移除这行
                        StringBuilder newContent = new StringBuilder();
                        for (int j = 0; j < i; j++) {
                            if (newContent.length() > 0) newContent.append("\n");
                            newContent.append(lines[j]);
                        }
                        content = newContent;
                        break;
                    }
                }
            }
        }
        
        // Getters and setters
        public String getTitle() { return title; }
        public String getQuestionNumber() { return questionNumber; }
        public void setQuestionNumber(String questionNumber) { this.questionNumber = questionNumber; }
        public String getSection() { return section; }
        public String getContent() { return content.toString().trim(); }
        public String getStudentAnswer() { return studentAnswer; }
        public void setStudentAnswer(String studentAnswer) { this.studentAnswer = studentAnswer; }
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
        
        public String getTitlePreview() {
            if (title == null) return "";
            return title.length() > 50 ? title.substring(0, 50) + "..." : title;
        }
    }
    
    /**
     * 提取题目编号
     */
    private String extractQuestionNumber(String line) {
        if (line == null || line.trim().isEmpty()) return "0";
        
        // 匹配各种题目编号格式
        Pattern[] patterns = {
            Pattern.compile("^(\\d+)\\s*[.、].*"),  // 1. 或 1、
            Pattern.compile("^\\((\\d+)\\).*"),     // (1)
            Pattern.compile("^([一二三四五六七八九十]+)\\s*[.、].*") // 一. 或 一、
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(line.trim());
            if (matcher.find()) {
                String number = matcher.group(1);
                // 如果是中文数字，转换为阿拉伯数字
                if (number.matches("[一二三四五六七八九十]+")) {
                    return String.valueOf(chineseToArabic(number));
                }
                return number;
            }
        }
        
        return "0";
    }
    
    /**
     * 中文数字转阿拉伯数字
     */
    private int chineseToArabic(String chinese) {
        String[] chineseNums = {"", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};
        for (int i = 1; i < chineseNums.length; i++) {
            if (chineseNums[i].equals(chinese)) {
                return i;
            }
        }
        
        // 处理十几的情况
        if (chinese.startsWith("十")) {
            if (chinese.length() == 1) return 10;
            String remainder = chinese.substring(1);
            for (int i = 1; i < chineseNums.length; i++) {
                if (chineseNums[i].equals(remainder)) {
                    return 10 + i;
                }
            }
        }
        
        return 0;
    }
    
    /**
     * 清理题目标题
     */
    private String cleanQuestionTitle(String title) {
        if (title == null) return "";
        
        // 移除学生得分信息
        String cleaned = title.replaceAll("\\s*学生得分[：:]?\\s*\\d+(\\.\\d+)?\\s*分?\\s*", " ");
        cleaned = cleaned.replaceAll("\\s*得分[：:]?\\s*\\d+(\\.\\d+)?\\s*分?\\s*", " ");
        
        // 规范化空格
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        return cleaned;
    }
    
    /**
     * 从分数行解析分数
     */
    private Double parseScoreFromLine(String line) {
        if (line == null || !isScoreInfo(line)) return null;
        
        Pattern pattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*分");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                log.warn("无法解析分数: {}", line);
            }
        }
        return null;
    }

    private String extractQuestionContentFromBlock(String block, String title) {
        String content = block.substring(title.length()).trim();
        
        String[] lines = content.split("\\r?\\n");
        StringBuilder cleanContent = new StringBuilder();
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (!trimmedLine.startsWith("我的答案") && !trimmedLine.startsWith("【答案】") && !trimmedLine.startsWith("答案：") && !isScoreInfo(trimmedLine)) {
                cleanContent.append(line).append("\n");
            }
        }
        
        return cleanContent.toString().trim();
    }
    
    private Double parseScoreFromBlock(String block) {
        String[] lines = block.split("\\r?\\n");
        for (String line : lines) {
            if (isScoreInfo(line)) {
                // Regex to find numbers (including decimals)
                Pattern p = Pattern.compile("(\\d+(\\.\\d+)?)");
                Matcher m = p.matcher(line);
                if (m.find()) {
                    try {
                        return Double.parseDouble(m.group(1));
                    } catch (NumberFormatException e) {
                        log.warn("无法从行 '{}' 中解析分数", line, e);
                    }
                }
            }
        }
        return null;
    }

    private record QuestionMarker(String title, int lineNumber) {}

    private List<QuestionMarker> findQuestionMarkers(String[] lines) {
        List<QuestionMarker> markers = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (isQuestionStart(line)) {
                markers.add(new QuestionMarker(line, i));
            }
        }
        return markers;
    }

    private String getTextBlock(String[] lines, int startLine, int endLine) {
        StringBuilder block = new StringBuilder();
        for (int i = startLine; i < endLine; i++) {
            block.append(lines[i]).append("\n");
        }
        return block.toString();
    }
    
    private String findAnswerInBlock(String block) {
        String[] lines = block.split("\\r?\\n");
        // Search from bottom to top, as answer is usually at the end of a block
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.startsWith("我的答案") || line.startsWith("【答案】") || line.startsWith("答案：") || line.startsWith("回答：")) {
                return extractStudentAnswerFromLine(line);
            }
        }
        
        // Fallback for answers embedded in score lines
        for (String line : lines) {
             if (isScoreInfo(line)) {
                String answer = extractAnswerFromScoreLine(line);
                if (answer != null && !answer.isBlank()) return answer;
            }
        }
        return null;
    }

    private String cleanQuestionContent(String content) {
         if (content == null || content.trim().isEmpty()) {
             return content;
         }
         String cleaned = content.trim();
         cleaned = cleaned.replaceAll("\\s*学生得分[：:]?\\s*\\d+(\\.\\d+)?\\s*分?\\s*", " ");
         cleaned = cleaned.replaceAll("\\s*得分[：:]?\\s*\\d+(\\.\\d+)?\\s*分?\\s*", " ");
         cleaned = cleaned.replaceAll("\\s*分数[：:]?\\s*\\d+(\\.\\d+)?\\s*分?\\s*", " ");
         cleaned = cleaned.replaceAll("\\s*学生答案[：:]?.*", "");
         cleaned = cleaned.replaceAll("\\s*正确答案[：:]?.*", "");
         cleaned = cleaned.replaceAll("\\s+", " ").trim();
         return cleaned;
    }
     
    private boolean isScoreInfo(String line) {
         if (line == null || line.trim().isEmpty()) return false;
         String trimmedLine = line.trim();
         return trimmedLine.contains("学生得分") || trimmedLine.contains("得分：") || trimmedLine.contains("得分:") || trimmedLine.matches(".*\\d+\\s*分\\s*$");
    }

    private String extractAnswerFromScoreLine(String line) {
        if (line == null || !isScoreInfo(line)) return null;
        String[] patterns = {"学生得分[：:]?", "得分[：:]?"};
        for (String pattern : patterns) {
            String[] parts = line.split(pattern);
            if (parts.length > 0) {
                String beforeScore = parts[0].trim();
                if (beforeScore.length() > 10 && (beforeScore.contains("(") || beforeScore.contains("）") || beforeScore.contains("答案"))) {
                    return cleanQuestionContent(beforeScore); // Clean the extracted answer part
                }
            }
        }
        return null;
    }
 
    private boolean isSectionHeader(String line) {
        if (line == null || line.trim().isEmpty()) return false;
        String trimmedLine = line.trim();
        if (trimmedLine.matches("^[一二三四五六七八九十]+[.、\\s]*\\s*(单选题|多选题|选择题|填空题|简答题|论述题|计算题|判断题|分析题).*")) return true;
        if (trimmedLine.matches("^\\d+[.、\\s]*\\s*(单选题|多选题|选择题|填空题|简答题|论述题|计算题|判断题|分析题).*")) return true;
        if (trimmedLine.matches("^[一二三四五六七八九十]+\\s*[.、]\\s*.*题\\s*\\([^)]*\\).*")) return true;
        if (trimmedLine.matches(".*题\\s*\\([^)]*\\).*") && (trimmedLine.contains("选择题") || trimmedLine.contains("填空题") || trimmedLine.contains("简答题") || trimmedLine.contains("判断题"))) return true;
        if (trimmedLine.matches("^[一二三四五六七八九十]+[.].*题.*\\(.*\\).*")) return true;
        return false;
    }

    private boolean isQuestionStart(String line) {
        if (line == null || line.trim().isEmpty()) return false;
        String trimmedLine = line.trim();
        if (isSectionHeader(trimmedLine)) return false;
        if (trimmedLine.contains("学生得分") || trimmedLine.contains("正确答案") || trimmedLine.contains("学生答案")) return false;
        
        // 排除IP地址、选项等非题目内容
        if (trimmedLine.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+.*")) return false; // IP地址
        if (trimmedLine.matches("^[A-Z]、\\s*.*")) return false; // 选项
        if (trimmedLine.matches("^[一二三四五六七八九十]+、\\s*.*") && trimmedLine.length() < 50) return false; // 短的中文选项
        if (trimmedLine.matches("^\\d+\\.\\s*[^\\u4e00-\\u9fa5]*$") && trimmedLine.length() < 20) return false; // 纯数字或英文的短行
        
        // 真正的题目格式 - 必须包含问号或明显的题目特征
        if (trimmedLine.matches("^\\d+[.、]\\s*.{10,}.*[?？].*")) return true; // 有问号的题目
        if (trimmedLine.matches("^\\d+[.、]\\s*.{15,}.*[（(].*[)）].*")) return true; // 有括号的题目
        if (trimmedLine.matches("^[一二三四五六七八九十]+[.、]\\s*.{15,}.*")) return true; // 中文数字开头的长题目
        
        return false;
    }

    private boolean isQuestionContent(String line) {
        if (line == null || line.trim().isEmpty()) return false;
        String trimmedLine = line.trim();
        if (trimmedLine.contains("学生答案") || trimmedLine.contains("学生得分") || isSectionHeader(trimmedLine) || isQuestionStart(trimmedLine)) return false;
        return true;
    }

    private void parseGeneralAnswers(String content, List<StudentAnswerImportData.QuestionAnswer> answers) {
        // This is a fallback parser. We will keep its original logic.
        // It should be simple, assuming a very structured format.
        String[] lines = content.split("\\r?\\n");
        String currentQuestion = null;
        StringBuilder currentAnswer = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.toLowerCase().startsWith("q:") || line.toLowerCase().startsWith("question:")) {
                        if (currentQuestion != null && currentAnswer.length() > 0) {
            StudentAnswerImportData.QuestionAnswer qa = new StudentAnswerImportData.QuestionAnswer();
            qa.setQuestionContent(currentQuestion);
            qa.setAnswerContent(currentAnswer.toString().trim());
            answers.add(qa);
        }
                currentQuestion = line.substring(line.indexOf(":") + 1).trim();
                currentAnswer.setLength(0);
            } else if (line.toLowerCase().startsWith("a:") || line.toLowerCase().startsWith("answer:")) {
                if (currentAnswer.length() > 0) currentAnswer.append("\n");
                currentAnswer.append(line.substring(line.indexOf(":") + 1).trim());
            } else if (currentQuestion != null) {
                // Assume it's a continuation of an answer
                if (currentAnswer.length() > 0) currentAnswer.append("\n");
                currentAnswer.append(line);
            }
        }

        if (currentQuestion != null && currentAnswer.length() > 0) {
            StudentAnswerImportData.QuestionAnswer qa = new StudentAnswerImportData.QuestionAnswer();
            qa.setQuestionContent(currentQuestion);
            qa.setAnswerContent(currentAnswer.toString().trim());
            answers.add(qa);
        }
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
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    addFilesFromDir(documents, file);
                } else {
                    String fileName = file.getName().toLowerCase();
                    if (fileName.endsWith(".doc") || fileName.endsWith(".docx") || fileName.endsWith(".wps")) {
                        documents.add(file);
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
} 