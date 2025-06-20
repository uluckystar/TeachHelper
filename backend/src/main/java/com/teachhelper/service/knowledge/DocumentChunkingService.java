package com.teachhelper.service.knowledge;

import com.teachhelper.entity.KnowledgeDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文档分块处理服务
 * 将长文档分割成适合向量化的小块
 */
@Service
@RequiredArgsConstructor
public class DocumentChunkingService {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DocumentChunkingService.class);

    @Value("${app.vector.chunk-size:2000}")
    private int defaultChunkSize;
    
    @Value("${app.vector.chunk-overlap:200}")
    private int defaultChunkOverlap;
    
    @Value("${app.vector.min-chunk-size:100}")
    private int minChunkSize;

    /**
     * 将文档内容分块
     */
    public List<DocumentChunk> chunkDocument(KnowledgeDocument document, Integer customChunkSize, Integer customChunkOverlap) {
        if (document.getContent() == null || document.getContent().trim().isEmpty()) {
            log.warn("Document {} has no content to chunk", document.getId());
            return List.of();
        }

        int chunkSize = customChunkSize != null ? customChunkSize : defaultChunkSize;
        int chunkOverlap = customChunkOverlap != null ? customChunkOverlap : defaultChunkOverlap;
        
        // 确保参数有效
        chunkSize = Math.max(chunkSize, minChunkSize);
        chunkOverlap = Math.min(chunkOverlap, chunkSize / 2);
        
        String content = document.getContent();
        List<DocumentChunk> chunks = new ArrayList<>();
        
        log.info("开始分块文档 {} ({}字符) - 块大小: {}, 重叠: {}", 
                document.getId(), content.length(), chunkSize, chunkOverlap);
        
        // 优先按段落分块
        List<String> paragraphs = splitByParagraphs(content);
        
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        int currentPosition = 0;
        
        for (String paragraph : paragraphs) {
            // 如果单个段落超过块大小，需要进一步拆分
            if (paragraph.length() > chunkSize) {
                // 先保存当前块（如果有内容）
                if (currentChunk.length() > 0) {
                    chunks.add(createChunk(document, currentChunk.toString(), chunkIndex++, currentPosition));
                    currentPosition += currentChunk.length();
                    currentChunk.setLength(0);
                }
                
                // 拆分大段落
                List<DocumentChunk> largeParagraphChunks = splitLargeParagraph(
                    document, paragraph, chunkIndex, currentPosition, chunkSize, chunkOverlap);
                chunks.addAll(largeParagraphChunks);
                chunkIndex += largeParagraphChunks.size();
                currentPosition += paragraph.length();
                continue;
            }
            
            // 检查添加这个段落是否会超过块大小
            if (currentChunk.length() + paragraph.length() > chunkSize && currentChunk.length() > 0) {
                // 保存当前块
                chunks.add(createChunk(document, currentChunk.toString(), chunkIndex++, currentPosition));
                
                // 处理重叠：保留上一块的末尾部分
                String overlapContent = getOverlapContent(currentChunk.toString(), chunkOverlap);
                currentPosition += currentChunk.length() - overlapContent.length();
                currentChunk.setLength(0);
                currentChunk.append(overlapContent);
            }
            
            currentChunk.append(paragraph).append("\n");
        }
        
        // 保存最后一块
        if (currentChunk.length() > 0) {
            chunks.add(createChunk(document, currentChunk.toString(), chunkIndex, currentPosition));
        }
        
        log.info("文档 {} 分块完成，共生成 {} 块", document.getId(), chunks.size());
        return chunks;
    }

    /**
     * 按段落分割文本
     */
    private List<String> splitByParagraphs(String content) {
        List<String> paragraphs = new ArrayList<>();
        
        // 按双换行符分割
        String[] parts = content.split("\n\n+");
        
        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                paragraphs.add(part);
            }
        }
        
        // 如果没有段落分割，按单换行符分割
        if (paragraphs.size() <= 1) {
            paragraphs.clear();
            String[] lines = content.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty()) {
                    paragraphs.add(line);
                }
            }
        }
        
        return paragraphs;
    }

    /**
     * 分割超大段落
     */
    private List<DocumentChunk> splitLargeParagraph(KnowledgeDocument document, String paragraph, 
                                                   int startIndex, int startPosition, 
                                                   int chunkSize, int chunkOverlap) {
        List<DocumentChunk> chunks = new ArrayList<>();
        
        // 按句子分割
        List<String> sentences = splitBySentences(paragraph);
        
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = startIndex;
        int currentPosition = startPosition;
        
        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > chunkSize && currentChunk.length() > 0) {
                // 保存当前块
                chunks.add(createChunk(document, currentChunk.toString(), chunkIndex++, currentPosition));
                
                // 处理重叠
                String overlapContent = getOverlapContent(currentChunk.toString(), chunkOverlap);
                currentPosition += currentChunk.length() - overlapContent.length();
                currentChunk.setLength(0);
                currentChunk.append(overlapContent);
            }
            
            currentChunk.append(sentence);
        }
        
        // 保存最后一块
        if (currentChunk.length() > 0) {
            chunks.add(createChunk(document, currentChunk.toString(), chunkIndex, currentPosition));
        }
        
        return chunks;
    }

    /**
     * 按句子分割文本
     */
    private List<String> splitBySentences(String text) {
        List<String> sentences = new ArrayList<>();
        
        // 中文句号、英文句号、问号、感叹号
        String[] parts = text.split("[。.!?！？]+");
        
        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                sentences.add(part + "。"); // 添加句号
            }
        }
        
        return sentences;
    }

    /**
     * 获取重叠内容
     */
    private String getOverlapContent(String content, int overlapSize) {
        if (content.length() <= overlapSize) {
            return content;
        }
        
        String overlap = content.substring(content.length() - overlapSize);
        
        // 尝试在词边界处截断
        int lastSpace = overlap.lastIndexOf(' ');
        int lastPunctuation = Math.max(overlap.lastIndexOf('。'), overlap.lastIndexOf('.'));
        
        if (lastPunctuation > lastSpace && lastPunctuation > overlapSize / 2) {
            return overlap.substring(lastPunctuation + 1);
        } else if (lastSpace > overlapSize / 2) {
            return overlap.substring(lastSpace + 1);
        }
        
        return overlap;
    }

    /**
     * 创建文档块
     */
    private DocumentChunk createChunk(KnowledgeDocument document, String content, int index, int position) {
        content = content.trim();
        
        Map<String, Object> metadata = Map.of(
            "documentId", document.getId(),
            "knowledgeBaseId", document.getKnowledgeBaseId(),
            "title", document.getTitle(),
            "fileName", document.getFileName(),
            "fileType", document.getFileType(),
            "chunkIndex", index,
            "chunkPosition", position,
            "chunkLength", content.length(),
            "type", "DOCUMENT_CHUNK",
            "originalChunkId", document.getId() + "_chunk_" + index
        );
        
        // 使用UUID作为向量存储的ID，避免格式问题
        String chunkId = UUID.randomUUID().toString();
        
        return new DocumentChunk(
            chunkId,
            content,
            metadata,
            index,
            position,
            content.length()
        );
    }

    /**
     * 文档块数据类
     */
    public static class DocumentChunk {
        private final String id;
        private final String content;
        private final Map<String, Object> metadata;
        private final int index;
        private final int position;
        private final int length;

        public DocumentChunk(String id, String content, Map<String, Object> metadata, 
                           int index, int position, int length) {
            this.id = id;
            this.content = content;
            this.metadata = metadata;
            this.index = index;
            this.position = position;
            this.length = length;
        }

        // Getters
        public String getId() { return id; }
        public String getContent() { return content; }
        public Map<String, Object> getMetadata() { return metadata; }
        public int getIndex() { return index; }
        public int getPosition() { return position; }
        public int getLength() { return length; }
    }
}
