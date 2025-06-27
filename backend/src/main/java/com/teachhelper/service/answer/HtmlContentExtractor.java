package com.teachhelper.service.answer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

@Slf4j
@Component
public class HtmlContentExtractor {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern STYLE_PATTERN = Pattern.compile("<style[^>]*>.*?</style>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_ENTITY_PATTERN = Pattern.compile("&[a-zA-Z][a-zA-Z0-9]*;");

    /**
     * 从HTML文件中提取纯文本内容
     */
    public String extractTextFromHtml(File htmlFile) throws IOException {
        StringBuilder content = new StringBuilder();
        
        try (FileInputStream fis = new FileInputStream(htmlFile);
             InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
             BufferedReader reader = new BufferedReader(isr)) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        
        String htmlContent = content.toString();
        return cleanHtmlContent(htmlContent);
    }

    /**
     * 清理HTML内容，提取纯文本
     */
    private String cleanHtmlContent(String htmlContent) {
        // 移除样式和脚本标签
        String cleaned = STYLE_PATTERN.matcher(htmlContent).replaceAll("");
        cleaned = SCRIPT_PATTERN.matcher(cleaned).replaceAll("");
        
        // 保留段落和换行结构，将块级元素替换为换行符
        cleaned = cleaned.replaceAll("(?i)</(p|div|br|h[1-6]|li|tr|td)>", "\n");
        cleaned = cleaned.replaceAll("(?i)<(p|div|br|h[1-6]|li|tr|td)[^>]*>", "\n");
        
        // 移除剩余的HTML标签，但用空格替换以防止词语粘连
        cleaned = HTML_TAG_PATTERN.matcher(cleaned).replaceAll(" ");
        
        // 解码HTML实体
        cleaned = decodeHtmlEntities(cleaned);
        
        // 清理多余的空白字符，但保持换行结构
        cleaned = cleaned.replaceAll("[ \\t]+", " "); // 合并空格和制表符
        cleaned = cleaned.replaceAll(" *\\n *", "\n"); // 清理换行周围的空格
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n"); // 合并过多的换行
        
        return cleaned.trim();
    }

    /**
     * 解码常见的HTML实体
     */
    private String decodeHtmlEntities(String text) {
        return text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            // 中文常见实体
            .replace("&#8203;", "") // 零宽度空格
            .replace("&#8204;", "") // 零宽度非连接符
            .replace("&#8205;", ""); // 零宽度连接符
    }

    /**
     * 检查提取的内容是否有效
     */
    public boolean isValidContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        // 检查是否包含中文字符
        boolean hasChineseCharacters = content.matches(".*[\\u4e00-\\u9fa5].*");
        
        // 检查内容长度是否合理
        boolean hasReasonableLength = content.length() > 50;
        
        return hasChineseCharacters && hasReasonableLength;
    }
} 