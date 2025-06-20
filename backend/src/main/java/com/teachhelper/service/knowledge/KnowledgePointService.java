package com.teachhelper.service.knowledge;

import com.teachhelper.dto.response.KnowledgePointResponse;
import com.teachhelper.entity.DifficultyLevel;
import com.teachhelper.entity.KnowledgePoint;
import com.teachhelper.repository.KnowledgePointRepository;
import com.teachhelper.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识点服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgePointService {

    private final KnowledgePointRepository knowledgePointRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final VectorStoreService vectorStoreService;

    /**
     * 根据知识库ID获取知识点列表
     */
    @Transactional(readOnly = true)
    public Page<KnowledgePointResponse> getKnowledgePointsByKnowledgeBase(Long knowledgeBaseId, 
                                                                         Pageable pageable, 
                                                                         String difficulty, 
                                                                         String keyword) {
        log.info("获取知识库 {} 的知识点列表", knowledgeBaseId);
        
        Page<KnowledgePoint> knowledgePoints;
        
        if (difficulty != null && keyword != null) {
            DifficultyLevel difficultyLevel = DifficultyLevel.valueOf(difficulty.toUpperCase());
            knowledgePoints = knowledgePointRepository.findByKnowledgeBaseIdAndDifficultyLevelAndTitleContainingIgnoreCase(
                knowledgeBaseId, difficultyLevel, keyword, pageable);
        } else if (difficulty != null) {
            DifficultyLevel difficultyLevel = DifficultyLevel.valueOf(difficulty.toUpperCase());
            knowledgePoints = knowledgePointRepository.findByKnowledgeBaseIdAndDifficultyLevel(
                knowledgeBaseId, difficultyLevel, pageable);
        } else if (keyword != null) {
            knowledgePoints = knowledgePointRepository.findByKnowledgeBaseIdAndTitleContainingIgnoreCase(
                knowledgeBaseId, keyword, pageable);
        } else {
            knowledgePoints = knowledgePointRepository.findByKnowledgeBaseIdOrderByCreatedAtDesc(
                knowledgeBaseId, pageable);
        }
        
        return knowledgePoints.map(this::convertToResponse);
    }

    /**
     * 根据ID获取知识点详情
     */
    @Transactional(readOnly = true)
    public KnowledgePointResponse getKnowledgePointById(Long id) {
        log.info("获取知识点详情 - ID: {}", id);
        
        KnowledgePoint knowledgePoint = knowledgePointRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("知识点不存在"));
        
        return convertToResponse(knowledgePoint);
    }

    /**
     * 获取知识点统计信息
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getKnowledgePointStatistics(Long knowledgeBaseId) {
        log.info("获取知识库 {} 的知识点统计", knowledgeBaseId);
        
        List<KnowledgePoint> allPoints = knowledgePointRepository.findByKnowledgeBaseId(knowledgeBaseId);
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalCount", allPoints.size());
        
        // 按难度级别统计
        Map<String, Long> difficultyStats = allPoints.stream()
            .collect(Collectors.groupingBy(
                kp -> kp.getDifficultyLevel().name(),
                Collectors.counting()
            ));
        statistics.put("difficultyDistribution", difficultyStats);
        
        // AI生成的知识点统计
        long aiGeneratedCount = allPoints.stream()
            .mapToLong(kp -> kp.getSourceDocumentId() != null ? 1 : 0)
            .sum();
        statistics.put("aiGeneratedCount", aiGeneratedCount);
        statistics.put("manualCount", allPoints.size() - aiGeneratedCount);
        
        // 最近更新时间
        Optional<KnowledgePoint> latestPoint = allPoints.stream()
            .max(Comparator.comparing(KnowledgePoint::getUpdatedAt));
        latestPoint.ifPresent(kp -> statistics.put("lastUpdated", kp.getUpdatedAt()));
        
        return statistics;
    }

    /**
     * 获取知识点树形结构
     */
    @Transactional(readOnly = true)
    public List<Object> getKnowledgePointTree(Long knowledgeBaseId) {
        log.info("构建知识库 {} 的知识点树", knowledgeBaseId);
        
        List<KnowledgePoint> allPoints = knowledgePointRepository.findByKnowledgeBaseId(knowledgeBaseId);
        
        // 简单的分组逻辑：按照关键词或标题前缀分组
        Map<String, List<KnowledgePoint>> groupedPoints = allPoints.stream()
            .collect(Collectors.groupingBy(this::extractCategory));
        
        List<Object> tree = new ArrayList<>();
        
        for (Map.Entry<String, List<KnowledgePoint>> entry : groupedPoints.entrySet()) {
            Map<String, Object> categoryNode = new HashMap<>();
            categoryNode.put("id", "category_" + entry.getKey().hashCode());
            categoryNode.put("label", entry.getKey());
            categoryNode.put("type", "category");
            categoryNode.put("count", entry.getValue().size());
            
            List<Object> children = entry.getValue().stream()
                .map(kp -> {
                    Map<String, Object> pointNode = new HashMap<>();
                    pointNode.put("id", kp.getId());
                    pointNode.put("label", kp.getTitle());
                    pointNode.put("type", "knowledge_point");
                    pointNode.put("difficulty", kp.getDifficultyLevel().name());
                    pointNode.put("documentCount", kp.getSourceDocumentId() != null ? 1 : 0);
                    return pointNode;
                })
                .collect(Collectors.toList());
            
            categoryNode.put("children", children);
            tree.add(categoryNode);
        }
        
        return tree;
    }

    /**
     * 获取相关内容片段
     */
    @Transactional(readOnly = true)
    public List<Object> getRelatedContent(Long knowledgePointId) {
        log.info("获取知识点 {} 的相关内容", knowledgePointId);
        
        KnowledgePoint knowledgePoint = knowledgePointRepository.findById(knowledgePointId)
            .orElseThrow(() -> new RuntimeException("知识点不存在"));
        
        List<Object> relatedContent = new ArrayList<>();
        
        try {
            // 使用向量搜索找到相关内容
            List<VectorStoreService.RetrievedDocument> similarDocs = vectorStoreService
                .searchSimilarDocuments(knowledgePoint.getTitle() + " " + knowledgePoint.getContent(), 5);
            
            for (VectorStoreService.RetrievedDocument doc : similarDocs) {
                Map<String, Object> content = new HashMap<>();
                content.put("id", doc.getId());
                content.put("content", doc.getContent().length() > 200 
                    ? doc.getContent().substring(0, 200) + "..." 
                    : doc.getContent());
                content.put("source", doc.getMetadata().get("fileName"));
                content.put("similarity", Math.round((1 - doc.getDistance()) * 100) / 100.0);
                
                relatedContent.add(content);
            }
        } catch (Exception e) {
            log.warn("获取相关内容失败，返回空列表", e);
        }
        
        return relatedContent;
    }

    /**
     * 提取知识点分类
     */
    private String extractCategory(KnowledgePoint knowledgePoint) {
        String title = knowledgePoint.getTitle();
        
        // 简单的分类逻辑：提取标题中的章节信息
        if (title.contains("第") && title.contains("章")) {
            int start = title.indexOf("第");
            int end = title.indexOf("章") + 1;
            if (start >= 0 && end > start) {
                return title.substring(start, end);
            }
        }
        
        // 根据关键词分类
        if (knowledgePoint.getKeywords() != null) {
            String[] keywords = knowledgePoint.getKeywords().split("[,，]");
            if (keywords.length > 0) {
                return keywords[0].trim();
            }
        }
        
        // 根据难度级别分类
        return knowledgePoint.getDifficultyLevel().name() + "级别";
    }

    /**
     * 转换为响应对象
     */
    private KnowledgePointResponse convertToResponse(KnowledgePoint knowledgePoint) {
        KnowledgePointResponse response = new KnowledgePointResponse();
        response.setId(knowledgePoint.getId());
        response.setTitle(knowledgePoint.getTitle());
        response.setContent(knowledgePoint.getContent());
        response.setSummary(knowledgePoint.getSummary());
        response.setDifficulty(knowledgePoint.getDifficultyLevel().name());
        response.setKeywords(knowledgePoint.getKeywords());
        response.setKnowledgeBaseId(knowledgePoint.getKnowledgeBaseId());
        response.setSourceDocumentId(knowledgePoint.getSourceDocumentId());
        response.setCreatedAt(knowledgePoint.getCreatedAt());
        response.setUpdatedAt(knowledgePoint.getUpdatedAt());
        response.setIsAIGenerated(knowledgePoint.getSourceDocumentId() != null);
        
        return response;
    }
}
