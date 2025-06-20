package com.teachhelper.service.ai;

import com.teachhelper.entity.UserAIConfig;

/**
 * 统一的AI客户端接口
 */
public interface AIClient {
    
    /**
     * 发送聊天请求
     * @param prompt 提示词
     * @param config AI配置
     * @return AI响应
     */
    AIResponse chat(String prompt, UserAIConfig config);
    
    /**
     * 检查配置是否有效
     * @param config AI配置
     * @return 是否有效
     */
    boolean validateConfig(UserAIConfig config);
    
    /**
     * 获取支持的提供商
     * @return 提供商名称
     */
    String getProviderName();
    
    /**
     * 估算token数量
     * @param text 文本内容
     * @return token数量
     */
    int estimateTokens(String text);
}
