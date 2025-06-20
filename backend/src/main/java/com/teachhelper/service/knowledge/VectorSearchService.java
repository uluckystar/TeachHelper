package com.teachhelper.service.knowledge;

import com.teachhelper.dto.request.VectorSearchRequest;
import com.teachhelper.dto.response.VectorSearchResponse;
import com.teachhelper.service.knowledge.VectorStoreService.RetrievedDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量搜索服务
 * 基于 Spring AI 1.1-SNAPSHOT 重构，提供增强的搜索功能，包括关键词高亮
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorSearchService {

    private final VectorStoreService vectorStoreService;
    private final KeywordHighlightService keywordHighlightService;

    /**
     * 执行向量搜索
     * 使用重构后的VectorStoreService增强搜索功能
     */
    public VectorSearchResponse search(VectorSearchRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 参数验证
            if (request == null || request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                return createEmptyResponse("Invalid search request");
            }
            
            // 使用向量存储服务进行搜索，支持知识库过滤
            List<RetrievedDocument> documents = vectorStoreService.searchSimilarDocuments(
                    request.getQuery().trim(), 
                    request.getLimit(),
                    request.getKnowledgeBaseId(), // 如果有知识库过滤
                    request.getSimilarityThreshold()
            );
            
            // 转换结果并添加关键词高亮
            String searchQuery = request.getQuery().trim();
            List<VectorSearchResponse.VectorSearchResult> results = new ArrayList<>();
            for (RetrievedDocument doc : documents) {
                // 计算相似度分数（距离转换为相似度）
                double similarity = Math.max(0.0, 1.0 - doc.getDistance());
                
                VectorSearchResponse.VectorSearchResult result = new VectorSearchResponse.VectorSearchResult();
                result.setId(doc.getId());
                result.setContent(doc.getContent());
                result.setSimilarity(similarity);
                result.setMetadata(doc.getMetadata());
                
                // 关键词高亮处理
                KeywordHighlightService.HighlightResult highlightResult = 
                    keywordHighlightService.highlightKeywords(doc.getContent(), searchQuery);
                
                result.setHighlightedContent(highlightResult.getHighlightedContent());
                result.setMatchedKeywords(highlightResult.getMatchedKeywords());
                result.setKeywordPositions(highlightResult.getKeywordPositions());
                    
                // 从元数据中提取信息
                extractMetadata(result, doc.getMetadata());
                
                results.add(result);
            }
            
            // 构建响应
            VectorSearchResponse response = new VectorSearchResponse();
            response.setResults(results);
            response.setTotalCount(results.size());
            response.setSearchTime(System.currentTimeMillis() - startTime);
            
            log.info("向量搜索完成，查询：{}，找到 {} 个结果，用时 {}ms", 
                    request.getQuery(), results.size(), response.getSearchTime());
            
            return response;
            
        } catch (Exception e) {
            log.error("向量搜索失败，查询：{}", request.getQuery(), e);
            return createEmptyResponse("Search failed: " + e.getMessage());
        }
    }
    
    /**
     * 创建空的搜索响应
     */
    private VectorSearchResponse createEmptyResponse(String reason) {
        VectorSearchResponse response = new VectorSearchResponse();
        response.setResults(new ArrayList<>());
        response.setTotalCount(0);
        response.setSearchTime(0L);
        log.warn("返回空搜索结果: {}", reason);
        return response;
    }
    
    /**
     * 从元数据中提取信息
     * 增强元数据处理，支持更丰富的信息提取
     */
    private void extractMetadata(VectorSearchResponse.VectorSearchResult result, Map<String, Object> metadata) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        
        // 设置标题，优先级：title > fileName > 默认值
        String title = (String) metadata.get("title");
        if (title == null || title.trim().isEmpty()) {
            title = (String) metadata.get("fileName");
        }
        if (title == null || title.trim().isEmpty()) {
            title = "未知标题";
        }
        result.setTitle(title);
        
        // 设置类型，支持CHUNK和DOCUMENT类型
        String type = (String) metadata.get("type");
        if (type == null || type.trim().isEmpty()) {
            type = "document"; // 默认类型
        }
        result.setType(type.toLowerCase());
        
        // 设置来源文件名
        String source = (String) metadata.get("fileName");
        if (source == null || source.trim().isEmpty()) {
            source = (String) metadata.get("source");
        }
        if (source == null || source.trim().isEmpty()) {
            source = "未知来源";
        }
        result.setSource(source);
        
        // 设置知识库ID和文档ID（如果可用）
        Object knowledgeBaseId = metadata.get("knowledgeBaseId");
        if (knowledgeBaseId != null) {
            result.setKnowledgeBaseId(knowledgeBaseId.toString());
        }
        
        Object documentId = metadata.get("documentId");
        if (documentId != null) {
            result.setDocumentId(documentId.toString());
        }
        
        // 如果是分块文档，设置分块信息
        Object chunkIndex = metadata.get("chunkIndex");
        if (chunkIndex instanceof Number) {
            result.setChunkIndex(((Number) chunkIndex).intValue());
        }
        
        Object chunkId = metadata.get("chunkId");
        if (chunkId != null) {
            result.setChunkId(chunkId.toString());
        }
    }
}
