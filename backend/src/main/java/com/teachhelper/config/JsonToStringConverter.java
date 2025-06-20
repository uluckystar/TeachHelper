package com.teachhelper.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA属性转换器：将JSON字符串与数据库JSON类型相互转换
 */
@Converter
public class JsonToStringConverter implements AttributeConverter<String, String> {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonToStringConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 验证是否为有效JSON并格式化
            Object jsonObject = objectMapper.readValue(attribute, Object.class);
            return objectMapper.writeValueAsString(jsonObject);
        } catch (JsonProcessingException e) {
            logger.warn("Invalid JSON format: {}, returning as-is: {}", attribute, e.getMessage());
            return attribute;
        }
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        return dbData;
    }
}
