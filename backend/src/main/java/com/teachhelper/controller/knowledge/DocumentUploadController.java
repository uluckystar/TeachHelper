package com.teachhelper.controller.knowledge;

import com.teachhelper.dto.request.DocumentUploadRequest;
import com.teachhelper.dto.response.KnowledgeDocumentResponse;
import com.teachhelper.service.knowledge.DocumentProcessingService;
import com.teachhelper.security.JwtTokenProvider;
import com.teachhelper.repository.UserRepository;
import com.teachhelper.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 文档上传控制器
 */
@RestController
@RequestMapping("/api/knowledge/documents")
@CrossOrigin(origins = "*")
public class DocumentUploadController {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadController.class);

    @Autowired
    private DocumentProcessingService documentProcessingService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取文档列表
     */
    @GetMapping
    public ResponseEntity<List<KnowledgeDocumentResponse>> getDocuments(
            @RequestParam Long knowledgeBaseId,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("Getting documents for knowledge base: {}", knowledgeBaseId);
            
            // 从请求中获取用户ID
            String token = extractToken(httpRequest);
            String username = jwtTokenProvider.getUsernameFromToken(token);
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            Long userId = user.getId();
            
            // 获取文档列表
            List<KnowledgeDocumentResponse> documents = documentProcessingService.getKnowledgeBaseDocuments(knowledgeBaseId, userId);
            
            log.info("Found {} documents for knowledge base: {}", documents.size(), knowledgeBaseId);
            return ResponseEntity.ok(documents);
            
        } catch (Exception e) {
            log.error("Failed to get documents for knowledge base: {}", knowledgeBaseId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 上传文档
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @ModelAttribute DocumentUploadRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("Uploading document: {}", request.getFile().getOriginalFilename());
            
            // 从请求中获取用户ID
            String token = extractToken(httpRequest);
            String username = jwtTokenProvider.getUsernameFromToken(token);
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            Long userId = user.getId();
            
            // 处理文档上传
            KnowledgeDocumentResponse response = documentProcessingService.uploadDocument(userId, request);
            
            log.info("Document upload successful: {}", response.getId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Document upload failed", e);
            return ResponseEntity.badRequest().body("Document upload failed: " + e.getMessage());
        }
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<?> deleteDocument(
            @PathVariable Long documentId,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("Deleting document: {}", documentId);
            
            // 从请求中获取用户ID
            String token = extractToken(httpRequest);
            String username = jwtTokenProvider.getUsernameFromToken(token);
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            Long userId = user.getId();
            
            // 删除文档
            documentProcessingService.deleteDocument(documentId, userId);
            
            log.info("Document deletion successful: {}", documentId);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("Document deletion failed", e);
            return ResponseEntity.badRequest().body("Document deletion failed: " + e.getMessage());
        }
    }

    /**
     * 下载文档
     */
    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long documentId,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("Downloading document: {}", documentId);
            
            // 从请求中获取用户ID
            String token = extractToken(httpRequest);
            String username = jwtTokenProvider.getUsernameFromToken(token);
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            Long userId = user.getId();
            
            // 获取文档信息
            KnowledgeDocumentResponse document = documentProcessingService.getDocumentById(documentId, userId);
            if (document == null) {
                log.warn("Document not found or access denied: {}", documentId);
                return ResponseEntity.notFound().build();
            }
            
            // 获取文件路径
            Path filePath = Paths.get(document.getFilePath());
            if (!Files.exists(filePath)) {
                log.warn("Document file not found: {}", document.getFilePath());
                return ResponseEntity.notFound().build();
            }
            
            // 创建Resource
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("Document file not readable: {}", document.getFilePath());
                return ResponseEntity.notFound().build();
            }
            
            // 确定文件类型
            String contentType = null;
            try {
                contentType = Files.probeContentType(filePath);
            } catch (IOException ex) {
                log.warn("Could not determine file type for: {}", document.getFilePath());
            }
            
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            log.info("Document download successful: {} ({})", documentId, document.getFileName());
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + document.getFileName() + "\"")
                    .body(resource);
            
        } catch (MalformedURLException e) {
            log.error("Malformed URL for document: {}", documentId, e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Document download failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取文档处理状态
     */
    @GetMapping("/{documentId}/status")
    public ResponseEntity<?> getDocumentStatus(@PathVariable Long documentId) {
        try {
            // TODO: 实现获取文档状态的逻辑
            log.info("Getting document status: {}", documentId);
            return ResponseEntity.ok().body("{\"status\": \"PROCESSING\", \"progress\": 50}");
        } catch (Exception e) {
            log.error("Failed to get document status", e);
            return ResponseEntity.badRequest().body("Failed to get document status: " + e.getMessage());
        }
    }

    /**
     * 从请求中提取JWT令牌
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
