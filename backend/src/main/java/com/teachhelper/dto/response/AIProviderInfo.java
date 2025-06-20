package com.teachhelper.dto.response;

import com.teachhelper.entity.AIProvider;

import lombok.Data;

@Data
public class AIProviderInfo {
    private String provider;
    private String displayName;
    private String description;
    private Boolean supported;
    
    public AIProviderInfo() {}
    
    public AIProviderInfo(AIProvider provider) {
        this.provider = provider.name();
        this.displayName = provider.getDisplayName();
        this.description = provider.getDescription();
        this.supported = true;
    }
}
