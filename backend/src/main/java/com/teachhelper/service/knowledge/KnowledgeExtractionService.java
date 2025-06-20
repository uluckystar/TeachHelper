package com.teachhelper.service.knowledge;

import com.teachhelper.entity.DifficultyLevel;
import com.teachhelper.entity.KnowledgeDocument;
import com.teachhelper.entity.KnowledgePoint;
import com.teachhelper.repository.KnowledgePointRepository;
import com.teachhelper.service.ai.LegacyAIServiceAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 知识提取服务
 * 使用AI自动提取和标记文档中的知识点
 */
@Service
public class KnowledgeExtractionService {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KnowledgeExtractionService.class);

    @Autowired
    private KnowledgePointRepository knowledgePointRepository;

    @Autowired
    private LegacyAIServiceAdapter aiServiceAdapter;

    /**
     * 从文档内容中提取知识点
     */
    @Transactional
    public List<KnowledgePoint> extractKnowledgePoints(KnowledgeDocument document) {
        try {
            log.info("Extracting knowledge points from document: {}", document.getId());
            
            String content = document.getContent();
            if (content == null || content.trim().isEmpty()) {
                log.warn("Document content is empty: {}", document.getId());
                return new ArrayList<>();
            }

            // 使用AI分析文档内容并提取知识点
            String aiResponse = analyzeDocumentContent(content);
            
            // 解析AI响应并创建知识点
            List<KnowledgePoint> extractedPoints = parseKnowledgePoints(aiResponse, document);
            
            // 保存到数据库
            List<KnowledgePoint> savedPoints = knowledgePointRepository.saveAll(extractedPoints);
            
            log.info("Extracted {} knowledge points from document: {}", savedPoints.size(), document.getId());
            return savedPoints;
            
        } catch (Exception e) {
            log.error("Failed to extract knowledge points from document: {}", document.getId(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 删除指定文档的所有知识点
     */
    @Transactional
    public void deleteKnowledgePointsByDocument(Long documentId) {
        try {
            log.info("Deleting knowledge points for document: {}", documentId);
            
            // 查找与文档相关的知识点
            List<KnowledgePoint> knowledgePoints = knowledgePointRepository.findBySourceDocumentId(documentId);
            
            if (knowledgePoints.isEmpty()) {
                log.info("No knowledge points found for document: {}", documentId);
                return;
            }
            
            log.info("Found {} knowledge points to delete for document: {}", knowledgePoints.size(), documentId);
            
            // 删除知识点
            knowledgePointRepository.deleteBySourceDocumentId(documentId);
            
            log.info("Successfully deleted {} knowledge points for document: {}", knowledgePoints.size(), documentId);
            
        } catch (Exception e) {
            log.error("Failed to delete knowledge points for document: {}", documentId, e);
            throw new RuntimeException("Failed to delete knowledge points for document: " + documentId, e);
        }
    }

    /**
     * 限制内容长度
     */
    private String limitContentLength(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        
        if (content.length() <= maxLength) {
            return content;
        }
        
        // 截取前部分内容并添加省略号
        return content.substring(0, maxLength) + "...";
    }

    /**
     * 使用AI分析文档内容
     */
    private String analyzeDocumentContent(String content) {
        // 限制内容长度避免token超限
        String limitedContent = limitContentLength(content, 4000);
        
        try {
            // 获取当前用户ID
            Long userId = getCurrentUserId();
            
            // 使用新的AI服务适配器
            return aiServiceAdapter.extractKnowledge(limitedContent, userId);
            
        } catch (Exception e) {
            log.error("Failed to analyze document content", e);
            // 返回简单的默认响应
            return "默认知识点\n这是从文档中提取的知识点\n难度：MEDIUM";
        }
    }
    
    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
                // 这里需要根据实际的用户认证实现来获取用户ID
                // 暂时返回null，使用默认配置
                return null;
            }
        } catch (Exception e) {
            log.debug("获取当前用户ID失败，使用默认配置", e);
        }
        return null;
    }

    /**
     * 解析AI响应并创建知识点对象
     */
    private List<KnowledgePoint> parseKnowledgePoints(String aiResponse, KnowledgeDocument document) {
        List<KnowledgePoint> knowledgePoints = new ArrayList<>();
        
        try {
            // 简化版本的解析 - 可以后续使用JSON解析库优化
            // 这里先用简单的文本解析
            if (aiResponse.contains("knowledge_points")) {
                // 解析JSON格式的响应
                knowledgePoints = parseJsonResponse(aiResponse, document);
            } else {
                // 解析自由文本格式的响应
                knowledgePoints = parseTextResponse(aiResponse, document);
            }
            
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", e.getMessage());
            // 创建一个默认的知识点
            KnowledgePoint defaultPoint = createDefaultKnowledgePoint(document);
            knowledgePoints.add(defaultPoint);
        }
        
        return knowledgePoints;
    }

    /**
     * 解析JSON格式的AI响应
     */
    private List<KnowledgePoint> parseJsonResponse(String aiResponse, KnowledgeDocument document) {
        List<KnowledgePoint> points = new ArrayList<>();
        
        // 简化的JSON解析 - 在生产环境中应该使用专业的JSON库
        // 这里暂时使用简单的字符串处理
        String[] lines = aiResponse.split("\n");
        KnowledgePoint currentPoint = null;
        
        for (String line : lines) {
            line = line.trim();
            if (line.contains("\"name\":")) {
                if (currentPoint != null) {
                    points.add(currentPoint);
                }
                currentPoint = new KnowledgePoint();
                currentPoint.setKnowledgeBaseId(document.getKnowledgeBaseId());
                currentPoint.setSourceDocumentId(document.getId());
                
                String name = extractJsonValue(line, "name");
                currentPoint.setTitle(name);
            } else if (currentPoint != null) {
                if (line.contains("\"description\":")) {
                    String description = extractJsonValue(line, "description");
                    currentPoint.setContent(description);
                } else if (line.contains("\"difficulty\":")) {
                    String difficulty = extractJsonValue(line, "difficulty");
                    try {
                        currentPoint.setDifficultyLevel(DifficultyLevel.valueOf(difficulty.toUpperCase()));
                    } catch (Exception e) {
                        currentPoint.setDifficultyLevel(DifficultyLevel.MEDIUM);
                    }
                } else if (line.contains("\"subject\":")) {
                    String subject = extractJsonValue(line, "subject");
                    // KnowledgePoint 可能没有 subject 字段，暂时跳过或添加到关键词中
                } else if (line.contains("\"keywords\":")) {
                    String keywords = extractJsonValue(line, "keywords");
                    currentPoint.setKeywords(keywords);
                }
            }
        }
        
        if (currentPoint != null) {
            points.add(currentPoint);
        }
        
        return points;
    }

    /**
     * 解析自由文本格式的AI响应
     */
    private List<KnowledgePoint> parseTextResponse(String aiResponse, KnowledgeDocument document) {
        List<KnowledgePoint> points = new ArrayList<>();
        
        // 简单的文本解析逻辑
        String[] paragraphs = aiResponse.split("\n\n");
        
        for (String paragraph : paragraphs) {
            if (paragraph.trim().length() > 10) { // 过滤掉太短的内容
                KnowledgePoint point = new KnowledgePoint();
                point.setKnowledgeBaseId(document.getKnowledgeBaseId());
                point.setSourceDocumentId(document.getId());
                
                // 提取第一行作为标题
                String[] lines = paragraph.split("\n");
                if (lines.length > 0) {
                    point.setTitle(lines[0].replaceAll("[0-9.]", "").trim());
                    point.setContent(paragraph);
                    point.setDifficultyLevel(DifficultyLevel.MEDIUM);
                    
                    points.add(point);
                }
            }
        }
        
        return points;
    }

    /**
     * 从JSON行中提取值
     */
    private String extractJsonValue(String line, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(line);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    /**
     * 创建默认知识点
     */
    private KnowledgePoint createDefaultKnowledgePoint(KnowledgeDocument document) {
        KnowledgePoint point = new KnowledgePoint();
        point.setKnowledgeBaseId(document.getKnowledgeBaseId());
        point.setSourceDocumentId(document.getId());
        point.setTitle(document.getTitle() != null ? document.getTitle() : "文档知识点");
        point.setContent("从文档中提取的知识点");
        point.setDifficultyLevel(DifficultyLevel.MEDIUM);
        point.setKeywords("文档,知识点");
        
        return point;
    }
}
