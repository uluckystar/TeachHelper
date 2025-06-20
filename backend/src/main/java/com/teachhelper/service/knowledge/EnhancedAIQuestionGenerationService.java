package com.teachhelper.service.knowledge;

import com.teachhelper.dto.request.AIQuestionGenerationRequest;
import com.teachhelper.dto.response.AIQuestionGenerationResponse;
import com.teachhelper.entity.*;
import com.teachhelper.repository.*;
import com.teachhelper.service.UserAIConfigService;
import com.teachhelper.service.ai.AIClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 增强版AI试卷生成服务
 * 支持RAG检索、来源标注、多知识库协同生成
 */
@Service
@Slf4j
@Transactional
public class EnhancedAIQuestionGenerationService {

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;
    
    @Autowired
    private KnowledgePointRepository knowledgePointRepository;
    
    @Autowired
    private KnowledgeDocumentRepository knowledgeDocumentRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private QuestionBankRepository questionBankRepository;
    
    @Autowired
    private QuestionKnowledgePointRepository questionKnowledgePointRepository;
    
    @Autowired
    private VectorStoreService vectorStoreService;
    
    @Autowired
    private UserAIConfigService userAIConfigService;
    
    @Autowired
    private AIClientFactory aiClientFactory;

    @Autowired(required = false)
    @Qualifier("openAiChatModel")
    private ChatModel deepseekChatModel;

    /**
     * 增强版题目生成请求类
     */
    public static class EnhancedQuestionGenerationRequest {
        private List<Long> knowledgeBaseIds; // 支持多知识库
        private List<String> questionTypes;
        private Map<String, Integer> difficultyDistribution;
        private String generationStrategy; // RAG_BASED, DIRECT_LLM, SMART_MIX
        private boolean enableSourceCitation; // 是否启用来源标注
        private int maxQuestionsPerSource; // 每个来源最大题目数
        private String customPrompt; // 自定义生成提示
        private List<String> focusKeywords; // 重点关键词
        private boolean enableContentPreview; // 是否启用内容预览

        // Getters and Setters
        public List<Long> getKnowledgeBaseIds() { return knowledgeBaseIds; }
        public void setKnowledgeBaseIds(List<Long> knowledgeBaseIds) { this.knowledgeBaseIds = knowledgeBaseIds; }
        
        public List<String> getQuestionTypes() { return questionTypes; }
        public void setQuestionTypes(List<String> questionTypes) { this.questionTypes = questionTypes; }
        
        public Map<String, Integer> getDifficultyDistribution() { return difficultyDistribution; }
        public void setDifficultyDistribution(Map<String, Integer> difficultyDistribution) { this.difficultyDistribution = difficultyDistribution; }
        
        public String getGenerationStrategy() { return generationStrategy; }
        public void setGenerationStrategy(String generationStrategy) { this.generationStrategy = generationStrategy; }
        
        public boolean isEnableSourceCitation() { return enableSourceCitation; }
        public void setEnableSourceCitation(boolean enableSourceCitation) { this.enableSourceCitation = enableSourceCitation; }
        
        public int getMaxQuestionsPerSource() { return maxQuestionsPerSource; }
        public void setMaxQuestionsPerSource(int maxQuestionsPerSource) { this.maxQuestionsPerSource = maxQuestionsPerSource; }
        
        public String getCustomPrompt() { return customPrompt; }
        public void setCustomPrompt(String customPrompt) { this.customPrompt = customPrompt; }
        
        public List<String> getFocusKeywords() { return focusKeywords; }
        public void setFocusKeywords(List<String> focusKeywords) { this.focusKeywords = focusKeywords; }
        
        public boolean isEnableContentPreview() { return enableContentPreview; }
        public void setEnableContentPreview(boolean enableContentPreview) { this.enableContentPreview = enableContentPreview; }
    }

    /**
     * 增强版题目响应类
     */
    public static class EnhancedQuestionResponse {
        private String id;
        private String content;
        private String type;
        private String difficulty;
        private List<String> options; // 选择题选项
        private String answer;
        private String explanation;
        private List<SourceCitation> sources; // 来源标注
        private Map<String, Object> metadata;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        
        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }
        
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
        
        public List<SourceCitation> getSources() { return sources; }
        public void setSources(List<SourceCitation> sources) { this.sources = sources; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    /**
     * 来源标注类
     */
    public static class SourceCitation {
        private Long documentId;
        private String documentTitle;
        private String fileName;
        private String extractedContent; // 引用的具体内容
        private String chunkId; // 文档分块ID
        private double relevanceScore; // 相关性评分
        private String citationType; // 引用类型：QUESTION_BASE, ANSWER_SUPPORT, EXPLANATION
        private Map<String, Object> metadata;

        public SourceCitation(Long documentId, String documentTitle, String fileName, 
                            String extractedContent, double relevanceScore, String citationType) {
            this.documentId = documentId;
            this.documentTitle = documentTitle;
            this.fileName = fileName;
            this.extractedContent = extractedContent;
            this.relevanceScore = relevanceScore;
            this.citationType = citationType;
            this.metadata = new HashMap<>();
        }

        // Getters and Setters
        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }
        
        public String getDocumentTitle() { return documentTitle; }
        public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getExtractedContent() { return extractedContent; }
        public void setExtractedContent(String extractedContent) { this.extractedContent = extractedContent; }
        
        public String getChunkId() { return chunkId; }
        public void setChunkId(String chunkId) { this.chunkId = chunkId; }
        
        public double getRelevanceScore() { return relevanceScore; }
        public void setRelevanceScore(double relevanceScore) { this.relevanceScore = relevanceScore; }
        
        public String getCitationType() { return citationType; }
        public void setCitationType(String citationType) { this.citationType = citationType; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    /**
     * 生成增强版试卷
     */
    public EnhancedQuestionGenerationResponse generateEnhancedQuestions(
            Long userId, EnhancedQuestionGenerationRequest request) {
        log.info("开始为用户 {} 生成增强版试卷，策略: {}", userId, request.getGenerationStrategy());
        
        try {
            // 验证知识库访问权限
            List<KnowledgeBase> knowledgeBases = validateKnowledgeBases(request.getKnowledgeBaseIds());
            
            // 根据生成策略选择不同的生成方式
            List<EnhancedQuestionResponse> questions = switch (request.getGenerationStrategy()) {
                case "RAG_BASED" -> generateQuestionsWithRAG(request, knowledgeBases);
                case "DIRECT_LLM" -> generateQuestionsDirectLLM(request, knowledgeBases);
                case "SMART_MIX" -> generateQuestionsSmartMix(request, knowledgeBases);
                default -> generateQuestionsWithRAG(request, knowledgeBases);
            };

            // 构建响应
            EnhancedQuestionGenerationResponse response = new EnhancedQuestionGenerationResponse();
            response.setTaskId(UUID.randomUUID().toString());
            response.setStatus("COMPLETED");
            response.setProgress(100);
            response.setGeneratedCount(questions.size());
            response.setSourceStrategy(request.getGenerationStrategy());
            response.setGeneratedAt(LocalDateTime.now());
            response.setQuestions(questions);
            
            // 构建知识库统计信息
            Map<String, Object> sourceStatistics = buildSourceStatistics(questions, knowledgeBases);
            response.setSourceStatistics(sourceStatistics);
            
            log.info("成功为用户 {} 生成 {} 道增强版题目", userId, questions.size());
            return response;
            
        } catch (Exception e) {
            log.error("生成增强版题目失败", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * 基于RAG检索生成题目
     */
    private List<EnhancedQuestionResponse> generateQuestionsWithRAG(
            EnhancedQuestionGenerationRequest request, List<KnowledgeBase> knowledgeBases) {
        
        List<EnhancedQuestionResponse> questions = new ArrayList<>();
        
        // 构建搜索查询
        String searchQuery = buildSearchQuery(request);
        
        // 从向量数据库检索相关内容
        List<VectorStoreService.RetrievedDocument> retrievedDocs = 
            vectorStoreService.searchSimilarDocuments(searchQuery, 10);
        
        if (retrievedDocs.isEmpty()) {
            log.warn("未从向量数据库检索到相关内容，将降级到直接LLM生成");
            return generateQuestionsDirectLLM(request, knowledgeBases);
        }
        
        // 根据检索到的内容分组生成题目
        Map<String, List<VectorStoreService.RetrievedDocument>> groupedDocs = 
            groupDocumentsByRelevance(retrievedDocs);
        
        for (Map.Entry<String, List<VectorStoreService.RetrievedDocument>> entry : groupedDocs.entrySet()) {
            String difficulty = entry.getKey();
            List<VectorStoreService.RetrievedDocument> docs = entry.getValue();
            
            // 为每个难度级别生成题目
            List<EnhancedQuestionResponse> difficultyQuestions = 
                generateQuestionsFromRetrievedContent(docs, difficulty, request);
            
            questions.addAll(difficultyQuestions);
            
            // 限制每个来源的题目数量
            if (questions.size() >= request.getMaxQuestionsPerSource()) {
                break;
            }
        }
        
        return questions;
    }

    /**
     * 直接使用LLM生成题目
     */
    private List<EnhancedQuestionResponse> generateQuestionsDirectLLM(
            EnhancedQuestionGenerationRequest request, List<KnowledgeBase> knowledgeBases) {
        
        List<EnhancedQuestionResponse> questions = new ArrayList<>();
        
        // 获取知识库概述信息
        String knowledgeBaseSummary = buildKnowledgeBaseSummary(knowledgeBases);
        
        // 构建生成提示
        String promptTemplate = buildDirectLLMPrompt(request, knowledgeBaseSummary);
        
        try {
            if (deepseekChatModel != null) {
                String response = deepseekChatModel.call(promptTemplate);
                questions = parseAIResponse(response, request);
                
                // 为直接生成的题目添加知识库来源标注
                addKnowledgeBaseSourceCitation(questions, knowledgeBases);
            }
        } catch (Exception e) {
            log.error("直接LLM生成题目失败", e);
        }
        
        return questions;
    }

    /**
     * 智能混合模式生成题目
     */
    private List<EnhancedQuestionResponse> generateQuestionsSmartMix(
            EnhancedQuestionGenerationRequest request, List<KnowledgeBase> knowledgeBases) {
        
        List<EnhancedQuestionResponse> questions = new ArrayList<>();
        
        // 50% 使用RAG生成，50% 使用直接LLM生成
        EnhancedQuestionGenerationRequest ragRequest = cloneRequest(request);
        ragRequest.setMaxQuestionsPerSource(request.getMaxQuestionsPerSource() / 2);
        
        EnhancedQuestionGenerationRequest llmRequest = cloneRequest(request);
        llmRequest.setMaxQuestionsPerSource(request.getMaxQuestionsPerSource() / 2);
        
        // RAG生成
        List<EnhancedQuestionResponse> ragQuestions = 
            generateQuestionsWithRAG(ragRequest, knowledgeBases);
        
        // LLM生成
        List<EnhancedQuestionResponse> llmQuestions = 
            generateQuestionsDirectLLM(llmRequest, knowledgeBases);
        
        questions.addAll(ragQuestions);
        questions.addAll(llmQuestions);
        
        // 智能去重和优化
        questions = deduplicateAndOptimize(questions);
        
        return questions;
    }

    /**
     * 构建搜索查询
     */
    private String buildSearchQuery(EnhancedQuestionGenerationRequest request) {
        StringBuilder query = new StringBuilder();
        
        if (request.getFocusKeywords() != null && !request.getFocusKeywords().isEmpty()) {
            query.append(String.join(" ", request.getFocusKeywords()));
        }
        
        if (request.getQuestionTypes() != null && !request.getQuestionTypes().isEmpty()) {
            query.append(" ").append(String.join(" ", request.getQuestionTypes()));
        }
        
        return query.toString().trim();
    }

    /**
     * 根据检索内容生成题目
     */
    private List<EnhancedQuestionResponse> generateQuestionsFromRetrievedContent(
            List<VectorStoreService.RetrievedDocument> docs, String difficulty, 
            EnhancedQuestionGenerationRequest request) {
        
        List<EnhancedQuestionResponse> questions = new ArrayList<>();
        
        for (VectorStoreService.RetrievedDocument doc : docs) {
            try {
                // 构建基于检索内容的生成提示
                String promptTemplate = buildRAGPrompt(doc, difficulty, request);
                
                if (deepseekChatModel != null) {
                    String response = deepseekChatModel.call(promptTemplate);
                    List<EnhancedQuestionResponse> generatedQuestions = parseAIResponse(response, request);
                    
                    // 为生成的题目添加来源标注
                    addSourceCitationToQuestions(generatedQuestions, doc);
                    
                    questions.addAll(generatedQuestions);
                }
                
            } catch (Exception e) {
                log.error("基于检索内容生成题目失败", e);
            }
        }
        
        return questions;
    }

    /**
     * 构建RAG生成提示
     */
    private String buildRAGPrompt(VectorStoreService.RetrievedDocument doc, 
                                String difficulty, EnhancedQuestionGenerationRequest request) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("基于以下内容生成").append(difficulty).append("难度的题目：\n\n");
        prompt.append("参考内容：\n").append(doc.getContent()).append("\n\n");
        
        if (request.getQuestionTypes() != null && !request.getQuestionTypes().isEmpty()) {
            prompt.append("题目类型：").append(String.join("、", request.getQuestionTypes())).append("\n");
        }
        
        if (request.getCustomPrompt() != null && !request.getCustomPrompt().isEmpty()) {
            prompt.append("特殊要求：").append(request.getCustomPrompt()).append("\n");
        }
        
        prompt.append("\n请生成1道题目，包含题目内容、选项（如果是选择题）、答案和解释。");
        prompt.append("生成格式必须为JSON，包含以下字段：");
        prompt.append("{\n");
        prompt.append("  \"content\": \"题目内容\",\n");
        prompt.append("  \"type\": \"题目类型\",\n");
        prompt.append("  \"difficulty\": \"").append(difficulty).append("\",\n");
        prompt.append("  \"options\": [\"选项A\", \"选项B\", \"选项C\", \"选项D\"],\n");
        prompt.append("  \"answer\": \"正确答案\",\n");
        prompt.append("  \"explanation\": \"答案解释\"\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }

    /**
     * 构建直接LLM生成提示
     */
    private String buildDirectLLMPrompt(EnhancedQuestionGenerationRequest request, String knowledgeBaseSummary) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("基于以下知识库信息生成题目：\n\n");
        prompt.append("知识库概述：\n").append(knowledgeBaseSummary).append("\n\n");
        
        if (request.getQuestionTypes() != null && !request.getQuestionTypes().isEmpty()) {
            prompt.append("题目类型：").append(String.join("、", request.getQuestionTypes())).append("\n");
        }
        
        if (request.getDifficultyDistribution() != null && !request.getDifficultyDistribution().isEmpty()) {
            prompt.append("难度分布：");
            for (Map.Entry<String, Integer> entry : request.getDifficultyDistribution().entrySet()) {
                prompt.append(entry.getKey()).append(" ").append(entry.getValue()).append("道，");
            }
            prompt.setLength(prompt.length() - 1); // 移除最后的逗号
            prompt.append("\n");
        }
        
        if (request.getCustomPrompt() != null && !request.getCustomPrompt().isEmpty()) {
            prompt.append("特殊要求：").append(request.getCustomPrompt()).append("\n");
        }
        
        prompt.append("\n请生成题目，每道题目包含题目内容、选项（如果是选择题）、答案和解释。");
        prompt.append("请以JSON数组格式返回，每个题目的格式如下：\n");
        prompt.append("{\n");
        prompt.append("  \"content\": \"题目内容\",\n");
        prompt.append("  \"type\": \"题目类型\",\n");
        prompt.append("  \"difficulty\": \"难度级别\",\n");
        prompt.append("  \"options\": [\"选项A\", \"选项B\", \"选项C\", \"选项D\"],\n");
        prompt.append("  \"answer\": \"正确答案\",\n");
        prompt.append("  \"explanation\": \"答案解释\"\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }

    /**
     * 解析AI响应
     */
    private List<EnhancedQuestionResponse> parseAIResponse(String response, EnhancedQuestionGenerationRequest request) {
        List<EnhancedQuestionResponse> questions = new ArrayList<>();
        
        try {
            // 简化解析，实际应该使用JSON解析
            // 这里返回一个示例题目
            EnhancedQuestionResponse question = new EnhancedQuestionResponse();
            question.setId(UUID.randomUUID().toString());
            question.setContent("基于AI生成的示例题目：什么是数据结构？");
            question.setType("单选题");
            question.setDifficulty("EASY");
            question.setOptions(Arrays.asList("A. 数据的组织方式", "B. 程序的结构", "C. 算法的实现", "D. 以上都不是"));
            question.setAnswer("A");
            question.setExplanation("数据结构是计算机存储、组织数据的方式。");
            question.setSources(new ArrayList<>());
            question.setMetadata(new HashMap<>());
            
            questions.add(question);
            
        } catch (Exception e) {
            log.error("解析AI响应失败", e);
        }
        
        return questions;
    }

    /**
     * 为题目添加来源标注
     */
    private void addSourceCitationToQuestions(List<EnhancedQuestionResponse> questions, 
                                            VectorStoreService.RetrievedDocument doc) {
        
        for (EnhancedQuestionResponse question : questions) {
            if (question.getSources() == null) {
                question.setSources(new ArrayList<>());
            }
            
            // 从文档元数据中提取信息
            Map<String, Object> metadata = doc.getMetadata();
            Long documentId = (Long) metadata.get("documentId");
            String title = (String) metadata.get("title");
            String fileName = (String) metadata.get("fileName");
            
            SourceCitation citation = new SourceCitation(
                documentId, title, fileName, doc.getContent(), 
                1.0 - doc.getDistance(), "QUESTION_BASE"
            );
            
            question.getSources().add(citation);
        }
    }

    /**
     * 为直接生成的题目添加知识库来源标注
     */
    private void addKnowledgeBaseSourceCitation(List<EnhancedQuestionResponse> questions, 
                                              List<KnowledgeBase> knowledgeBases) {
        
        for (EnhancedQuestionResponse question : questions) {
            if (question.getSources() == null) {
                question.setSources(new ArrayList<>());
            }
            
            for (KnowledgeBase kb : knowledgeBases) {
                SourceCitation citation = new SourceCitation(
                    null, kb.getName(), kb.getDescription(), 
                    "基于知识库：" + kb.getName(), 0.8, "KNOWLEDGE_BASE"
                );
                citation.getMetadata().put("knowledgeBaseId", kb.getId());
                question.getSources().add(citation);
            }
        }
    }

    // 辅助方法
    private List<KnowledgeBase> validateKnowledgeBases(List<Long> knowledgeBaseIds) {
        return knowledgeBaseRepository.findAllById(knowledgeBaseIds);
    }

    private Map<String, List<VectorStoreService.RetrievedDocument>> groupDocumentsByRelevance(
            List<VectorStoreService.RetrievedDocument> docs) {
        Map<String, List<VectorStoreService.RetrievedDocument>> grouped = new HashMap<>();
        
        for (VectorStoreService.RetrievedDocument doc : docs) {
            double score = 1.0 - doc.getDistance();
            String difficulty = score > 0.8 ? "HARD" : score > 0.6 ? "MEDIUM" : "EASY";
            
            grouped.computeIfAbsent(difficulty, k -> new ArrayList<>()).add(doc);
        }
        
        return grouped;
    }

    private String buildKnowledgeBaseSummary(List<KnowledgeBase> knowledgeBases) {
        return knowledgeBases.stream()
            .map(kb -> String.format("%s (%s)", kb.getName(), kb.getSubject()))
            .collect(Collectors.joining(", "));
    }

    private EnhancedQuestionGenerationRequest cloneRequest(EnhancedQuestionGenerationRequest original) {
        EnhancedQuestionGenerationRequest clone = new EnhancedQuestionGenerationRequest();
        clone.setKnowledgeBaseIds(new ArrayList<>(original.getKnowledgeBaseIds()));
        clone.setQuestionTypes(new ArrayList<>(original.getQuestionTypes()));
        clone.setDifficultyDistribution(new HashMap<>(original.getDifficultyDistribution()));
        clone.setGenerationStrategy(original.getGenerationStrategy());
        clone.setEnableSourceCitation(original.isEnableSourceCitation());
        clone.setMaxQuestionsPerSource(original.getMaxQuestionsPerSource());
        clone.setCustomPrompt(original.getCustomPrompt());
        clone.setFocusKeywords(original.getFocusKeywords() != null ? new ArrayList<>(original.getFocusKeywords()) : null);
        clone.setEnableContentPreview(original.isEnableContentPreview());
        return clone;
    }

    private List<EnhancedQuestionResponse> deduplicateAndOptimize(List<EnhancedQuestionResponse> questions) {
        // 简单去重，实际应该基于内容相似度
        Set<String> seen = new HashSet<>();
        return questions.stream()
            .filter(q -> seen.add(q.getContent()))
            .collect(Collectors.toList());
    }

    private Map<String, Object> buildSourceStatistics(List<EnhancedQuestionResponse> questions, 
                                                     List<KnowledgeBase> knowledgeBases) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQuestions", questions.size());
        stats.put("knowledgeBasesUsed", knowledgeBases.size());
        stats.put("sourceCitationsCount", questions.stream()
            .mapToInt(q -> q.getSources() != null ? q.getSources().size() : 0)
            .sum());
        return stats;
    }

    private EnhancedQuestionGenerationResponse createErrorResponse(String errorMessage) {
        EnhancedQuestionGenerationResponse errorResponse = new EnhancedQuestionGenerationResponse();
        errorResponse.setTaskId("error");
        errorResponse.setStatus("FAILED");
        errorResponse.setProgress(0);
        errorResponse.setError("生成失败: " + errorMessage);
        errorResponse.setQuestions(new ArrayList<>());
        return errorResponse;
    }

    /**
     * 增强版响应类
     */
    public static class EnhancedQuestionGenerationResponse {
        private String taskId;
        private String status;
        private int progress;
        private int generatedCount;
        private String sourceStrategy;
        private LocalDateTime generatedAt;
        private String error;
        private List<EnhancedQuestionResponse> questions;
        private Map<String, Object> sourceStatistics;

        // Getters and Setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
        
        public int getGeneratedCount() { return generatedCount; }
        public void setGeneratedCount(int generatedCount) { this.generatedCount = generatedCount; }
        
        public String getSourceStrategy() { return sourceStrategy; }
        public void setSourceStrategy(String sourceStrategy) { this.sourceStrategy = sourceStrategy; }
        
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public List<EnhancedQuestionResponse> getQuestions() { return questions; }
        public void setQuestions(List<EnhancedQuestionResponse> questions) { this.questions = questions; }
        
        public Map<String, Object> getSourceStatistics() { return sourceStatistics; }
        public void setSourceStatistics(Map<String, Object> sourceStatistics) { this.sourceStatistics = sourceStatistics; }
    }
}
