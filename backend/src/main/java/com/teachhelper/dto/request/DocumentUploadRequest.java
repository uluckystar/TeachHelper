package com.teachhelper.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

/**
 * 文档上传请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadRequest {

    @NotNull(message = "知识库ID不能为空")
    private Long knowledgeBaseId;

    @NotNull(message = "文件不能为空")
    private MultipartFile file;

    @NotBlank(message = "文档标题不能为空")
    private String title;

    private String description;

    // OCR配置
    private String ocrEngine = "tesseract"; // tesseract, paddleocr, 等
    
    private String ocrLanguage = "chi_sim"; // 识别语言
    
    // 知识点提取配置
    private Boolean extractKnowledgePoints = true;
    
    private Boolean autoClassifyDifficulty = true;
    
    // 处理选项
    private Boolean enableVectorization = true; // 是否生成向量嵌入
    
    private Integer chunkSize = 1000; // 文本分块大小
    
    private Integer chunkOverlap = 200; // 分块重叠大小
    
    // Manual getters (fallback for Lombok issues)
    public Long getKnowledgeBaseId() { return knowledgeBaseId; }
    public MultipartFile getFile() { return file; }
    public String getTitle() { return title; }
    public Integer getChunkSize() { return chunkSize; }
    public Integer getChunkOverlap() { return chunkOverlap; }
}
