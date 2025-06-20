package com.teachhelper.service.ai;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.teachhelper.entity.AIProvider;
import com.teachhelper.service.ai.impl.ClaudeClient;
import com.teachhelper.service.ai.impl.DeepSeekClient;
import com.teachhelper.service.ai.impl.GenericAIClient;
import com.teachhelper.service.ai.impl.OpenAIClient;

/**
 * AI客户端工厂
 */
@Service
public class AIClientFactory {
    
    private final Map<AIProvider, AIClient> clients = new HashMap<>();
    
    @Autowired
    public AIClientFactory(OpenAIClient openAIClient,
                          DeepSeekClient deepSeekClient,
                          ClaudeClient claudeClient,
                          @Qualifier("aiRestTemplate") RestTemplate aiRestTemplate) {
        clients.put(AIProvider.OPENAI, openAIClient);
        clients.put(AIProvider.DEEPSEEK, deepSeekClient);
        clients.put(AIProvider.CLAUDE, claudeClient);
        
        // 添加其他提供商的默认实现，传入配置好的RestTemplate
        clients.put(AIProvider.ALIBABA_TONGYI, new GenericAIClient(AIProvider.ALIBABA_TONGYI, aiRestTemplate));
        clients.put(AIProvider.BAIDU_ERNIE, new GenericAIClient(AIProvider.BAIDU_ERNIE, aiRestTemplate));
        clients.put(AIProvider.TENCENT_HUNYUAN, new GenericAIClient(AIProvider.TENCENT_HUNYUAN, aiRestTemplate));
        clients.put(AIProvider.CUSTOM, new GenericAIClient(AIProvider.CUSTOM, aiRestTemplate));
    }
    
    /**
     * 获取指定提供商的AI客户端
     */
    public AIClient getClient(AIProvider provider) {
        AIClient client = clients.get(provider);
        if (client == null) {
            throw new IllegalArgumentException("不支持的AI提供商: " + provider);
        }
        return client;
    }
    
    /**
     * 检查提供商是否支持
     */
    public boolean isSupported(AIProvider provider) {
        return clients.containsKey(provider);
    }
    
    /**
     * 获取所有支持的提供商
     */
    public AIProvider[] getSupportedProviders() {
        return clients.keySet().toArray(new AIProvider[0]);
    }
}
