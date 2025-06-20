package com.teachhelper.entity;

/**
 * AI提供商枚举
 */
public enum AIProvider {
    OPENAI("OpenAI", "https://api.openai.com/v1/chat/completions", true),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1/chat/completions", true),
    CLAUDE("Claude", "https://api.anthropic.com/v1/messages", true),
    ALIBABA_TONGYI("阿里通义千问", "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation", true),
    BAIDU_ERNIE("百度文心一言", "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions", true),
    TENCENT_HUNYUAN("腾讯混元", "https://hunyuan.tencentcloudapi.com/", true),
    CUSTOM("自定义", "", true);

    private final String displayName;
    private final String defaultEndpoint;
    private final boolean enabled;

    AIProvider(String displayName, String defaultEndpoint, boolean enabled) {
        this.displayName = displayName;
        this.defaultEndpoint = defaultEndpoint;
        this.enabled = enabled;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultEndpoint() {
        return defaultEndpoint;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 获取提供商描述
     */
    public String getDescription() {
        switch (this) {
            case OPENAI:
                return "OpenAI GPT系列模型";
            case DEEPSEEK:
                return "DeepSeek AI模型";
            case CLAUDE:
                return "Anthropic Claude模型";
            case ALIBABA_TONGYI:
                return "阿里巴巴通义千问模型";
            case BAIDU_ERNIE:
                return "百度文心一言模型";
            case TENCENT_HUNYUAN:
                return "腾讯混元模型";
            case CUSTOM:
                return "自定义AI提供商";
            default:
                return "未知提供商";
        }
    }

    /**
     * 获取默认模型
     */
    public String getDefaultModel() {
        switch (this) {
            case OPENAI:
                return "gpt-3.5-turbo";
            case DEEPSEEK:
                return "deepseek-chat";
            case CLAUDE:
                return "claude-3-sonnet-20240229";
            case ALIBABA_TONGYI:
                return "qwen-turbo";
            case BAIDU_ERNIE:
                return "ernie-bot-turbo";
            case TENCENT_HUNYUAN:
                return "hunyuan-lite";
            default:
                return "custom-model";
        }
    }

    /**
     * 获取token定价（每1K token的价格，单位：美元）
     */
    public double getInputTokenPrice() {
        switch (this) {
            case OPENAI:
                return 0.0015; // GPT-3.5-turbo
            case DEEPSEEK:
                return 0.0014;
            case CLAUDE:
                return 0.003;
            case ALIBABA_TONGYI:
                return 0.0012;
            case BAIDU_ERNIE:
                return 0.0012;
            case TENCENT_HUNYUAN:
                return 0.0012;
            default:
                return 0.002;
        }
    }

    public double getOutputTokenPrice() {
        switch (this) {
            case OPENAI:
                return 0.002; // GPT-3.5-turbo
            case DEEPSEEK:
                return 0.002;
            case CLAUDE:
                return 0.015;
            case ALIBABA_TONGYI:
                return 0.0012;
            case BAIDU_ERNIE:
                return 0.0012;
            case TENCENT_HUNYUAN:
                return 0.0012;
            default:
                return 0.002;
        }
    }
}
