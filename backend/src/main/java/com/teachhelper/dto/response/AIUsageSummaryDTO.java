package com.teachhelper.dto.response;

import lombok.Data;

@Data
public class AIUsageSummaryDTO {
    private String provider;
    private String model;
    private Long requestCount;
    private Long tokenCount;
    private Double estimatedCost;
}
