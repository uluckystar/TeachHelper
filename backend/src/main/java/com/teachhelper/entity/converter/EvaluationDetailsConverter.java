package com.teachhelper.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teachhelper.dto.EvaluationDetails;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;

@Converter
public class EvaluationDetailsConverter implements AttributeConverter<EvaluationDetails, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(EvaluationDetails attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            // Consider logging this exception
            throw new IllegalArgumentException("Error converting EvaluationDetails to JSON", e);
        }
    }

    @Override
    public EvaluationDetails convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, EvaluationDetails.class);
        } catch (IOException e) {
            // Consider logging this exception
            throw new IllegalArgumentException("Error converting JSON to EvaluationDetails", e);
        }
    }
} 