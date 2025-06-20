package com.teachhelper.service.knowledge;

import com.teachhelper.dto.request.DocumentUploadRequest;
import com.teachhelper.dto.response.KnowledgeDocumentResponse;
import com.teachhelper.entity.KnowledgeDocument;
import com.teachhelper.entity.ProcessingStatus;
import com.teachhelper.exception.ResourceNotFoundException;
import com.teachhelper.repository.KnowledgeDocumentRepository;
import com.teachhelper.repository.KnowledgePointRepository;
import com.teachhelper.service.knowledge.processors.DocumentProcessor;
import com.teachhelper.service.knowledge.processors.DocumentProcessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文档处理服务
 */
@Service
@Transactional
public class DocumentProcessingService {
    
    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);
    
    @Autowired
    private KnowledgeDocumentRepository documentRepository;
    
    @Autowired
    private KnowledgeBaseService knowledgeBaseService;
    
    @Autowired
    private DocumentProcessorFactory processorFactory;
    
    @Autowired
    private VectorStoreService vectorStoreService;
    
    @Autowired
    private KnowledgeExtractionService knowledgeExtractionService;
    
    @Autowired
    private KnowledgePointRepository knowledgePointRepository;
    
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    
    @Value("${app.upload.max-file-size:52428800}") // 50MB default
    private long maxFileSize;
    
    /**
     * 上传文档
     */
    public KnowledgeDocumentResponse uploadDocument(Long userId, DocumentUploadRequest request) {
        log.info("Uploading document for user: {}, knowledge base: {}", userId, request.getKnowledgeBaseId());
        
        MultipartFile file = request.getFile();
        
        // 验证文件
        validateFile(file);
        
        // 验证知识库权限
        if (!knowledgeBaseService.isKnowledgeBaseOwnedByUser(request.getKnowledgeBaseId(), userId)) {
            throw new ResourceNotFoundException("Knowledge base not found or access denied");
        }
        
        // 保存文件
        String savedFilePath = saveFile(file);
        
        // 创建文档记录
        KnowledgeDocument document = new KnowledgeDocument();
        document.setKnowledgeBaseId(request.getKnowledgeBaseId());
        document.setFileName(file.getOriginalFilename());
        document.setFileType(getFileExtension(file.getOriginalFilename()));
        document.setFileSize((long) file.getSize());
        document.setFilePath(savedFilePath);
        document.setTitle(request.getTitle() != null ? request.getTitle() : file.getOriginalFilename());
        document.setStatus(ProcessingStatus.PENDING);
        // createdAt和updatedAt会在@PrePersist中自动设置
        
        KnowledgeDocument saved = documentRepository.save(document);
        log.info("Document uploaded with ID: {}", saved.getId());
        
        // 异步处理文档（传递请求参数以获取分块配置）
        processDocumentAsync(saved.getId(), request);
        
        return convertToResponse(saved);
    }
    
    /**
     * 异步处理文档
     */
    @Async
    public void processDocumentAsync(Long documentId) {
        processDocumentAsync(documentId, null);
    }
    
    /**
     * 异步处理文档（带请求参数）
     */
    @Async
    public void processDocumentAsync(Long documentId, DocumentUploadRequest request) {
        try {
            processDocumentWithRequest(documentId, request);
        } catch (Exception e) {
            log.error("Failed to process document: {}", documentId, e);
            updateProcessingStatus(documentId, ProcessingStatus.FAILED, e.getMessage());
        }
    }
    
    /**
     * 处理文档内容（带请求参数）
     */
    private void processDocumentWithRequest(Long documentId, DocumentUploadRequest request) {
        log.info("Processing document: {}", documentId);
        
        KnowledgeDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        
        try {
            // 更新状态为处理中
            updateProcessingStatus(documentId, ProcessingStatus.PROCESSING, "开始处理文档...");
            
            // 根据文件类型获取处理器
            Optional<DocumentProcessor> processorOpt = processorFactory.getProcessor(document.getFileType());
            if (!processorOpt.isPresent()) {
                throw new IllegalArgumentException("No processor found for file type: " + document.getFileType());
            }
            
            DocumentProcessor processor = processorOpt.get();
            updateProcessingStatus(documentId, ProcessingStatus.PROCESSING, "提取文档内容...");
            
            // 提取内容
            String content = processor.extractText(document.getFilePath());
            document.setContent(content);
            updateProcessingStatus(documentId, ProcessingStatus.PROCESSING, "内容提取完成...");
            
            // 存储到向量数据库（使用分块处理）
            updateProcessingStatus(documentId, ProcessingStatus.PROCESSING, "向量化文档内容...");
            // 从请求参数中获取分块配置
            Integer chunkSize = request != null ? request.getChunkSize() : null;
            Integer chunkOverlap = request != null ? request.getChunkOverlap() : null;
            vectorStoreService.addDocumentToVectorStore(document, chunkSize, chunkOverlap);
            updateProcessingStatus(documentId, ProcessingStatus.PROCESSING, "向量存储完成...");
            
            // 提取知识点（AI标签）
            updateProcessingStatus(documentId, ProcessingStatus.PROCESSING, "提取知识点...");
            knowledgeExtractionService.extractKnowledgePoints(document);
            
            // 完成处理
            document.setStatus(ProcessingStatus.COMPLETED);
            document.setProcessingError(null);
            documentRepository.save(document);
            
            log.info("Document {} processed successfully", documentId);
            
        } catch (Exception e) {
            log.error("Error processing document: {}", documentId, e);
            updateProcessingStatus(documentId, ProcessingStatus.FAILED, e.getMessage());
        }
    }
    
    /**
     * 更新处理状态
     */
    private void updateProcessingStatus(Long documentId, ProcessingStatus status, String message) {
        documentRepository.findById(documentId).ifPresent(document -> {
            document.setStatus(status);
            document.setProcessingError(message);
            documentRepository.save(document);
        });
    }
    
    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size");
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null || !isSupportedFile(fileName)) {
            throw new IllegalArgumentException("Unsupported file type. Supported: " + 
                processorFactory.getSupportedFileTypesDescription());
        }
    }
    
    /**
     * 检查是否支持的文件类型
     */
    private boolean isSupportedFile(String fileName) {
        String extension = getFileExtension(fileName);
        return processorFactory.isSupported(extension);
    }
    
    /**
     * 保存文件
     */
    private String saveFile(MultipartFile file) {
        try {
            // 确保上传目录存在
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // 生成唯一文件名
            String originalFileName = file.getOriginalFilename();
            String extension = getFileExtension(originalFileName);
            String newFileName = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(newFileName);
            
            // 保存文件
            Files.copy(file.getInputStream(), filePath);
            
            log.info("File saved: {}", filePath.toString());
            return filePath.toString();
            
        } catch (IOException e) {
            log.error("Failed to save file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to save file", e);
        }
    }
    
    /**
     * 删除文档及其相关数据
     */
    @Transactional
    public void deleteDocument(Long documentId, Long userId) {
        log.info("Deleting document {} for user: {}", documentId, userId);
        
        KnowledgeDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        
        // 验证权限
        if (!knowledgeBaseService.isKnowledgeBaseOwnedByUser(document.getKnowledgeBaseId(), userId)) {
            throw new ResourceNotFoundException("Document not found or access denied");
        }
        
        try {
            // 1. 删除相关的知识点
            log.info("Deleting knowledge points for document: {}", documentId);
            deleteKnowledgePointsByDocument(documentId);
            
            // 2. 删除向量存储中的数据
            log.info("Removing document from vector store: {}", documentId);
            vectorStoreService.removeDocumentFromVectorStore(documentId);
            
            // 3. 删除物理文件
            log.info("Deleting physical file for document: {}", documentId);
            deletePhysicalFile(document.getFilePath());
            
            // 4. 删除数据库记录
            log.info("Deleting document record: {}", documentId);
            documentRepository.delete(document);
            
            log.info("Document {} deleted successfully", documentId);
            
        } catch (Exception e) {
            log.error("Failed to delete document: {}", documentId, e);
            throw new RuntimeException("Failed to delete document: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除文档相关的知识点
     */
    private void deleteKnowledgePointsByDocument(Long documentId) {
        try {
            log.info("Deleting knowledge points for document: {}", documentId);
            
            // 直接使用 Repository 删除知识点
            knowledgePointRepository.deleteBySourceDocumentId(documentId);
            
            log.info("Successfully deleted knowledge points for document: {}", documentId);
        } catch (Exception e) {
            log.warn("Failed to delete knowledge points for document: {}", documentId, e);
            // 继续执行，不中断删除流程
        }
    }
    
    /**
     * 删除物理文件
     */
    private void deletePhysicalFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            log.warn("File path is empty, skipping physical file deletion");
            return;
        }
        
        try {
            Path file = Paths.get(filePath);
            if (Files.exists(file)) {
                Files.delete(file);
                log.info("Physical file deleted: {}", filePath);
            } else {
                log.warn("Physical file not found: {}", filePath);
            }
        } catch (Exception e) {
            log.error("Failed to delete physical file: {}", filePath, e);
            // 不抛出异常，避免影响数据库记录的删除
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
    
    /**
     * 获取知识库的文档列表
     */
    @Transactional(readOnly = true)
    public List<KnowledgeDocumentResponse> getKnowledgeBaseDocuments(Long knowledgeBaseId, Long userId) {
        log.info("Getting documents for knowledge base: {}, user: {}", knowledgeBaseId, userId);
        
        // 验证权限
        if (!knowledgeBaseService.isKnowledgeBaseOwnedByUser(knowledgeBaseId, userId)) {
            throw new ResourceNotFoundException("Knowledge base not found or access denied");
        }
        
        List<KnowledgeDocument> documents = documentRepository.findByKnowledgeBaseIdOrderByCreatedAtDesc(knowledgeBaseId);
        log.info("Found {} documents for knowledge base: {}", documents.size(), knowledgeBaseId);
        
        return documents.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * 根据ID获取文档
     */
    @Transactional(readOnly = true)
    public KnowledgeDocumentResponse getDocumentById(Long documentId, Long userId) {
        log.info("Getting document by id: {}, user: {}", documentId, userId);
        
        Optional<KnowledgeDocument> documentOpt = documentRepository.findById(documentId);
        if (!documentOpt.isPresent()) {
            return null;
        }
        
        KnowledgeDocument document = documentOpt.get();
        
        // 验证权限 - 检查文档所属的知识库是否属于用户
        if (!knowledgeBaseService.isKnowledgeBaseOwnedByUser(document.getKnowledgeBaseId(), userId)) {
            return null;
        }
        
        return convertToResponse(document);
    }

    /**
     * 转换为响应对象
     */
    private KnowledgeDocumentResponse convertToResponse(KnowledgeDocument document) {
        KnowledgeDocumentResponse response = new KnowledgeDocumentResponse();
        response.setId(document.getId());
        response.setKnowledgeBaseId(document.getKnowledgeBaseId());
        response.setTitle(document.getTitle());
        response.setFileName(document.getFileName());
        response.setFileType(document.getFileType());
        response.setFileSize(document.getFileSize());
        response.setFilePath(document.getFilePath());
        response.setContent(document.getContent());
        response.setDescription(document.getDescription());
        response.setProcessingStatus(document.getStatus().name());
        response.setProcessingError(document.getProcessingError());
        response.setProcessingProgress(document.getProcessingProgress());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        
        // 查询并设置知识点数量
        long knowledgePointCount = knowledgePointRepository.countBySourceDocumentId(document.getId());
        response.setExtractedKnowledgePointsCount((int) knowledgePointCount);
        
        // 如果有关联的知识库，设置知识库名称
        if (document.getKnowledgeBase() != null) {
            response.setKnowledgeBaseName(document.getKnowledgeBase().getName());
        }
        
        return response;
    }
}
