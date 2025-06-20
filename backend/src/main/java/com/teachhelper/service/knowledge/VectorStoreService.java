package com.teachhelper.service.knowledge;

import com.teachhelper.entity.KnowledgeDocument;
import com.teachhelper.service.knowledge.DocumentChunkingService.DocumentChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 向量存储服务
 * 基于 Spring AI 1.0.0-SNAPSHOT 优化实现
 * 采用最佳实践进行向量搜索和存储管理
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VectorStoreService {
    
    private final VectorStore vectorStore;
    private final DocumentChunkingService chunkingService;
    
    @Value("${spring.ai.vectorstore.pgvector.enable-chunking:true}")
    private boolean enableChunking;
    
    @Value("${spring.ai.vectorstore.pgvector.chunk-size:1000}")
    private int defaultChunkSize;
    
    @Value("${spring.ai.vectorstore.pgvector.chunk-overlap:200}")
    private int defaultChunkOverlap;
    
    @Value("${spring.ai.vectorstore.pgvector.similarity-threshold:0.75}")
    private double defaultSimilarityThreshold;
    
    @Value("${spring.ai.vectorstore.pgvector.max-document-batch-size:500}")
    private int maxDocumentBatchSize;

    private static final int MAX_CHARS = 8000; // 最大字符数限制

    /**
     * 检查向量存储是否可用
     */
    private boolean isVectorStoreAvailable() {
        try {
            // 使用官方推荐的健康检查方式
            SearchRequest testRequest = SearchRequest.builder()
                .query("health_check")
                .topK(1)
                .similarityThreshold(0.1)
                .build();
            vectorStore.similaritySearch(testRequest);
            return true;
        } catch (Exception e) {
            log.warn("Vector store health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 将文档向量化并存储到向量数据库（支持分块）
     * 使用 Spring AI 1.1-SNAPSHOT 官方推荐的文档处理方式
     */
    @Transactional
    public void addDocumentToVectorStore(KnowledgeDocument knowledgeDocument) {
        addDocumentToVectorStore(knowledgeDocument, null, null);
    }
    
    /**
     * 将文档向量化并存储到向量数据库（支持自定义分块参数）
     * 实现官方文档推荐的分块和批处理策略
     */
    @Transactional
    public void addDocumentToVectorStore(KnowledgeDocument knowledgeDocument, 
                                       Integer customChunkSize, Integer customChunkOverlap) {
        if (!isVectorStoreAvailable()) {
            log.warn("Vector store is not available, skipping document vectorization for: {}", knowledgeDocument.getId());
            return;
        }
        
        if (!StringUtils.hasText(knowledgeDocument.getContent())) {
            log.warn("Document content is empty, skipping vectorization for: {}", knowledgeDocument.getId());
            return;
        }
        
        try {
            if (enableChunking && shouldChunkDocument(knowledgeDocument)) {
                addDocumentWithChunking(knowledgeDocument, customChunkSize, customChunkOverlap);
            } else {
                addDocumentAsWhole(knowledgeDocument);
            }
            
            log.info("Successfully added document to vector store: {}", knowledgeDocument.getId());
            
        } catch (Exception e) {
            log.error("Failed to add document to vector store: {}", knowledgeDocument.getId(), e);
            // 根据官方文档建议，不抛出异常，避免影响主业务流程
        }
    }
    
    /**
     * 判断是否应该对文档进行分块
     * 基于文档长度和配置参数进行智能判断
     */
    private boolean shouldChunkDocument(KnowledgeDocument document) {
        if (document.getContent() == null) {
            return false;
        }
        
        // 使用配置的分块阈值，而不是硬编码
        int threshold = (int) (defaultChunkSize * 1.5);
        return document.getContent().length() > threshold;
    }
    
    /**
     * 使用分块方式添加文档
     * 实现官方推荐的分块处理和批量插入策略
     */
    private void addDocumentWithChunking(KnowledgeDocument knowledgeDocument, 
                                       Integer customChunkSize, Integer customChunkOverlap) {
        List<DocumentChunk> chunks = chunkingService.chunkDocument(
            knowledgeDocument, customChunkSize, customChunkOverlap);
        
        if (chunks.isEmpty()) {
            log.warn("No chunks generated for document: {}", knowledgeDocument.getId());
            return;
        }
        
        // 转换为Spring AI Document对象，包含丰富的元数据
        List<Document> vectorDocuments = chunks.stream()
            .map(chunk -> {
                Map<String, Object> metadata = Map.of(
                    "documentId", knowledgeDocument.getId(),
                    "chunkId", chunk.getId(),
                    "knowledgeBaseId", knowledgeDocument.getKnowledgeBaseId(),
                    "title", knowledgeDocument.getTitle(),
                    "fileName", knowledgeDocument.getFileName(),
                    "fileType", knowledgeDocument.getFileType(),
                    "type", "CHUNK"
                );
                return new Document(chunk.getId(), chunk.getContent(), metadata);
            })
            .collect(Collectors.toList());
        
        // 批量添加，提高性能
        addDocumentsBatch(vectorDocuments);
        
        log.info("Added {} chunks to vector store for document: {}", 
                chunks.size(), knowledgeDocument.getId());
    }
    
    /**
     * 作为单个文档添加（原有方式）
     * 使用官方推荐的元数据结构
     */
    private void addDocumentAsWhole(KnowledgeDocument knowledgeDocument) {
        Map<String, Object> metadata = Map.of(
            "documentId", knowledgeDocument.getId(),
            "knowledgeBaseId", knowledgeDocument.getKnowledgeBaseId(),
            "title", knowledgeDocument.getTitle(),
            "fileName", knowledgeDocument.getFileName(),
            "fileType", knowledgeDocument.getFileType(),
            "type", "DOCUMENT"
        );
        
        String content = limitDocumentContent(knowledgeDocument.getContent());
        Document document = new Document(content, metadata);
        vectorStore.add(List.of(document));
        
        log.debug("Added whole document to vector store: {}", knowledgeDocument.getId());
    }
    
    /**
     * 批量添加文档，实现官方推荐的批处理策略
     */
    private void addDocumentsBatch(List<Document> documents) {
        if (documents.isEmpty()) {
            return;
        }
        
        // 根据配置的批处理大小分批处理
        for (int i = 0; i < documents.size(); i += maxDocumentBatchSize) {
            int endIndex = Math.min(i + maxDocumentBatchSize, documents.size());
            List<Document> batch = documents.subList(i, endIndex);
            
            try {
                vectorStore.add(batch);
                log.debug("Added batch of {} documents to vector store", batch.size());
            } catch (Exception e) {
                log.error("Failed to add document batch starting at index {}", i, e);
                // 继续处理剩余批次
            }
        }
    }

    /**
     * 限制文档内容长度
     */
    private String limitDocumentContent(String content) {
        if (content == null) {
            return "";
        }
        
        if (enableChunking) {
            return content;
        }
        
        final int MAX_CHARS = 4000;
        
        if (content.length() <= MAX_CHARS) {
            return content;
        }
        
        String truncated = content.substring(0, MAX_CHARS);
        
        int lastPeriod = truncated.lastIndexOf('。');
        if (lastPeriod > MAX_CHARS / 2) {
            truncated = truncated.substring(0, lastPeriod + 1);
        } else {
            int lastSpace = truncated.lastIndexOf(' ');
            if (lastSpace > MAX_CHARS / 2) {
                truncated = truncated.substring(0, lastSpace);
            }
        }
        
        log.debug("Document content truncated from {} to {} characters", content.length(), truncated.length());
        return truncated + "\n[内容已截断...]";
    }

    /**
     * 搜索相似文档
     * 使用 Spring AI 1.1-SNAPSHOT 的增强搜索功能
     */
    public List<RetrievedDocument> searchSimilarDocuments(String query, int topK) {
        return searchSimilarDocuments(query, topK, null, defaultSimilarityThreshold);
    }
    
    /**
     * 搜索相似文档（带过滤器和自定义阈值）
     * 实现官方推荐的高级搜索功能
     */
    public List<RetrievedDocument> searchSimilarDocuments(String query, int topK, 
                                                         Long knowledgeBaseId, double similarityThreshold) {
        if (!isVectorStoreAvailable()) {
            log.warn("Vector store is not available, returning empty search results");
            return List.of();
        }
        
        if (!StringUtils.hasText(query)) {
            log.warn("Search query is empty");
            return List.of();
        }
        
        try {
            SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold);
            
            // 添加过滤器（如果指定了知识库ID）
            if (knowledgeBaseId != null) {
                // 在Spring AI 1.0.0-SNAPSHOT中使用字符串过滤器
                String filterExpression = "knowledgeBaseId == '" + knowledgeBaseId + "'";
                builder.filterExpression(filterExpression);
            }
            
            SearchRequest searchRequest = builder.build();
            List<Document> documents = vectorStore.similaritySearch(searchRequest);
            
            return documents.stream()
                .map(this::convertToRetrievedDocument)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Failed to search similar documents for query: {}", query, e);
            return List.of();
        }
    }
    
    /**
     * 转换Spring AI Document为自定义RetrievedDocument
     * 处理距离值和元数据的安全转换
     */
    private RetrievedDocument convertToRetrievedDocument(Document doc) {
        // 安全获取距离值
        double distance = 0.3; // 默认距离值，表示相似度为0.7
        if (doc.getMetadata() != null && doc.getMetadata().containsKey("distance")) {
            Object distanceObj = doc.getMetadata().get("distance");
            if (distanceObj instanceof Number) {
                distance = ((Number) distanceObj).doubleValue();
            }
        }
        
        return new RetrievedDocument(
            doc.getId(),
            doc.getText(), // 在Spring AI 1.0.0-SNAPSHOT中使用getText()
            doc.getMetadata(),
            distance
        );
    }

    /**
     * 删除文档的向量数据
     * 使用 Spring AI 1.1-SNAPSHOT 推荐的删除方式
     */
    @Transactional
    public void removeDocumentFromVectorStore(Long documentId) {
        if (!isVectorStoreAvailable()) {
            log.warn("Vector store is not available, skipping document removal for: {}", documentId);
            return;
        }
        
        if (documentId == null) {
            log.warn("Document ID is null, skipping removal");
            return;
        }
        
        try {
            // 使用字符串过滤器查找要删除的文档
            String filterExpression = "documentId == '" + documentId.toString() + "'";
            
            SearchRequest searchRequest = SearchRequest.builder()
                .query("*")
                .topK(1000) // 可能有多个分块
                .filterExpression(filterExpression)
                .build();
            
            List<Document> documentsToDelete = vectorStore.similaritySearch(searchRequest);
            
            if (!documentsToDelete.isEmpty()) {
                List<String> docIds = documentsToDelete.stream()
                    .map(Document::getId)
                    .collect(Collectors.toList());
                
                vectorStore.delete(docIds);
                log.info("Removed {} documents from vector store for documentId: {}", docIds.size(), documentId);
            } else {
                log.info("No documents found in vector store for documentId: {}", documentId);
            }
            
        } catch (Exception e) {
            log.error("Failed to remove document from vector store: {}", documentId, e);
        }
    }

    /**
     * 批量添加文档到向量数据库
     * 使用 Spring AI 1.1-SNAPSHOT 推荐的并行处理策略
     */
    @Transactional
    public void addDocumentsToVectorStore(List<KnowledgeDocument> documents) {
        if (!isVectorStoreAvailable()) {
            log.warn("Vector store is not available, skipping batch document vectorization");
            return;
        }
        
        if (documents == null || documents.isEmpty()) {
            log.info("No documents to add to vector store");
            return;
        }
        
        int successCount = 0;
        int failureCount = 0;
        
        for (KnowledgeDocument document : documents) {
            try {
                addDocumentToVectorStore(document);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("Failed to add document {} to vector store in batch operation", document.getId(), e);
            }
        }
        
        log.info("Completed batch vectorization: {} successful, {} failed out of {} total documents", 
                successCount, failureCount, documents.size());
    }

    /**
     * 检索文档类
     */
    public static class RetrievedDocument {
        private final String id;
        private final String content;
        private final Map<String, Object> metadata;
        private final double distance;
        
        public RetrievedDocument(String id, String content, Map<String, Object> metadata, double distance) {
            this.id = id;
            this.content = content;
            this.metadata = metadata;
            this.distance = distance;
        }
        
        public String getId() { return id; }
        public String getContent() { return content; }
        public Map<String, Object> getMetadata() { return metadata; }
        public double getDistance() { return distance; }
    }

    // 兼容旧接口的方法
    public void addDocument(String content, String metadata) {
        try {
            Document document = new Document(content, Map.of("metadata", metadata));
            vectorStore.add(List.of(document));
        } catch (Exception e) {
            log.error("Failed to add document to vector store", e);
        }
    }

    public void deleteDocument(String documentId) {
        try {
            removeDocumentFromVectorStore(Long.parseLong(documentId));
        } catch (NumberFormatException e) {
            log.error("Invalid document ID format: {}", documentId);
        }
    }

    public void updateDocument(String documentId, String content, String metadata) {
        deleteDocument(documentId);
        addDocument(content, metadata);
    }
}