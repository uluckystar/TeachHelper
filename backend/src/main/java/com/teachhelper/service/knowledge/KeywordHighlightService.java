package com.teachhelper.service.knowledge;

import com.teachhelper.dto.response.VectorSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 关键词高亮服务
 * 负责在搜索结果中高亮显示关键词，并提供关键词位置信息
 */
@Service
@Slf4j
public class KeywordHighlightService {
    
    private static final String HIGHLIGHT_START_TAG = "<mark>";
    private static final String HIGHLIGHT_END_TAG = "</mark>";
    private static final int CONTEXT_LENGTH = 100; // 上下文长度
    
    /**
     * 对搜索结果进行关键词高亮处理
     * 
     * @param content 原始内容
     * @param query 搜索查询词
     * @return 高亮处理后的内容和位置信息
     */
    public HighlightResult highlightKeywords(String content, String query) {
        if (content == null || content.isEmpty() || query == null || query.isEmpty()) {
            return new HighlightResult(content, Collections.emptyList(), Collections.emptyList());
        }
        
        // 提取关键词
        List<String> keywords = extractKeywords(query);
        if (keywords.isEmpty()) {
            return new HighlightResult(content, Collections.emptyList(), Collections.emptyList());
        }
        
        // 查找关键词位置
        List<VectorSearchResponse.KeywordPosition> positions = findKeywordPositions(content, keywords);
        
        // 生成高亮内容
        String highlightedContent = generateHighlightedContent(content, positions);
        
        // 提取匹配的关键词
        List<String> matchedKeywords = positions.stream()
                .map(VectorSearchResponse.KeywordPosition::getKeyword)
                .distinct()
                .toList();
        
        log.debug("关键词高亮处理完成，匹配到 {} 个关键词，{} 个位置", matchedKeywords.size(), positions.size());
        
        return new HighlightResult(highlightedContent, matchedKeywords, positions);
    }
    
    /**
     * 从查询中提取关键词
     * 支持中文分词和英文单词分割
     */
    private List<String> extractKeywords(String query) {
        List<String> keywords = new ArrayList<>();
        
        // 清理查询文本
        String cleanQuery = query.trim();
        
        // 简单的关键词提取：按空格和标点符号分割
        String[] tokens = cleanQuery.split("[\\s\\p{Punct}]+");
        
        for (String token : tokens) {
            token = token.trim();
            if (!token.isEmpty() && token.length() > 1) { // 过滤单字符
                keywords.add(token);
            }
        }
        
        // 如果没有找到关键词，将整个查询作为关键词
        if (keywords.isEmpty() && !cleanQuery.isEmpty()) {
            keywords.add(cleanQuery);
        }
        
        // 按长度排序，优先匹配长关键词
        keywords.sort((a, b) -> Integer.compare(b.length(), a.length()));
        
        log.debug("从查询 '{}' 中提取到关键词: {}", query, keywords);
        return keywords;
    }
    
    /**
     * 查找关键词在文本中的位置
     */
    private List<VectorSearchResponse.KeywordPosition> findKeywordPositions(String content, List<String> keywords) {
        List<VectorSearchResponse.KeywordPosition> positions = new ArrayList<>();
        Set<Integer> usedPositions = new HashSet<>(); // 避免重叠
        
        for (String keyword : keywords) {
            // 使用不区分大小写的正则匹配
            Pattern pattern = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            Matcher matcher = pattern.matcher(content);
            
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                
                // 检查是否与已有位置重叠
                boolean overlaps = false;
                for (int i = start; i < end; i++) {
                    if (usedPositions.contains(i)) {
                        overlaps = true;
                        break;
                    }
                }
                
                if (!overlaps) {
                    // 标记已使用的位置
                    for (int i = start; i < end; i++) {
                        usedPositions.add(i);
                    }
                    
                    // 创建位置信息
                    VectorSearchResponse.KeywordPosition position = new VectorSearchResponse.KeywordPosition();
                    position.setKeyword(content.substring(start, end)); // 使用原文中的实际文本
                    position.setStartIndex(start);
                    position.setEndIndex(end);
                    position.setContext(extractContext(content, start, end));
                    
                    positions.add(position);
                }
            }
        }
        
        // 按位置排序
        positions.sort(Comparator.comparingInt(VectorSearchResponse.KeywordPosition::getStartIndex));
        
        return positions;
    }
    
    /**
     * 提取关键词周围的上下文
     */
    private String extractContext(String content, int start, int end) {
        int contextStart = Math.max(0, start - CONTEXT_LENGTH);
        int contextEnd = Math.min(content.length(), end + CONTEXT_LENGTH);
        
        String context = content.substring(contextStart, contextEnd);
        
        // 如果不是从开头开始，添加省略号
        if (contextStart > 0) {
            context = "..." + context;
        }
        
        // 如果不是到结尾，添加省略号
        if (contextEnd < content.length()) {
            context = context + "...";
        }
        
        return context.trim();
    }
    
    /**
     * 生成高亮内容
     */
    private String generateHighlightedContent(String content, List<VectorSearchResponse.KeywordPosition> positions) {
        if (positions.isEmpty()) {
            return content;
        }
        
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        
        for (VectorSearchResponse.KeywordPosition position : positions) {
            // 添加关键词前的文本
            result.append(content, lastEnd, position.getStartIndex());
            
            // 添加高亮的关键词
            result.append(HIGHLIGHT_START_TAG)
                  .append(content, position.getStartIndex(), position.getEndIndex())
                  .append(HIGHLIGHT_END_TAG);
            
            lastEnd = position.getEndIndex();
        }
        
        // 添加最后一个关键词后的文本
        result.append(content.substring(lastEnd));
        
        return result.toString();
    }
    
    /**
     * 高亮结果封装类
     */
    public static class HighlightResult {
        private final String highlightedContent;
        private final List<String> matchedKeywords;
        private final List<VectorSearchResponse.KeywordPosition> keywordPositions;
        
        public HighlightResult(String highlightedContent, List<String> matchedKeywords, 
                              List<VectorSearchResponse.KeywordPosition> keywordPositions) {
            this.highlightedContent = highlightedContent;
            this.matchedKeywords = matchedKeywords;
            this.keywordPositions = keywordPositions;
        }
        
        public String getHighlightedContent() {
            return highlightedContent;
        }
        
        public List<String> getMatchedKeywords() {
            return matchedKeywords;
        }
        
        public List<VectorSearchResponse.KeywordPosition> getKeywordPositions() {
            return keywordPositions;
        }
    }
}
