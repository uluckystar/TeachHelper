import api from '@/utils/request'
import type {
  UserAIConfig,
  UserAIConfigRequest,
  UserAIConfigResponse,
  AIConfigTestRequest,
  AIConfigTestResponse,
  AIProviderInfo,
  AIUsageStats
} from '@/types/api'

export const userAIConfigApi = {
  // 获取当前用户的所有AI配置
  async getConfigs(): Promise<UserAIConfigResponse[]> {
    const response = await api.get<UserAIConfigResponse[]>('/user-ai-config')
    return response.data
  },

  // 获取特定配置详情
  async getConfig(id: number): Promise<UserAIConfigResponse> {
    const response = await api.get<UserAIConfigResponse>(`/user-ai-config/${id}`)
    return response.data
  },

  // 创建新的AI配置
  async createConfig(config: UserAIConfigRequest): Promise<UserAIConfigResponse> {
    const response = await api.post<UserAIConfigResponse>('/user-ai-config', config)
    return response.data
  },

  // 更新AI配置
  async updateConfig(id: number, config: UserAIConfigRequest): Promise<UserAIConfigResponse> {
    const response = await api.put<UserAIConfigResponse>(`/user-ai-config/${id}`, config)
    return response.data
  },

  // 删除AI配置
  async deleteConfig(id: number): Promise<void> {
    await api.delete(`/user-ai-config/${id}`)
  },

  // 激活/停用AI配置
  async toggleConfig(id: number, isActive: boolean): Promise<UserAIConfigResponse> {
    const response = await api.put<UserAIConfigResponse>(`/user-ai-config/${id}/toggle`, { isActive })
    return response.data
  },

  // 测试AI配置
  async testConfig(id: number, testRequest: AIConfigTestRequest): Promise<AIConfigTestResponse> {
    // AI测试请求需要更长的超时时间
    const response = await api.post<AIConfigTestResponse>(`/user-ai-config/${id}/test`, testRequest, {
      timeout: 0 // 不设置超时限制
    })
    return response.data
  },

  // 获取使用统计
  async getUsageStats(id: number): Promise<AIUsageStats> {
    const response = await api.get<AIUsageStats>(`/user-ai-config/${id}/stats`)
    return response.data
  },

  // 获取支持的AI提供商信息
  async getProviders(): Promise<AIProviderInfo[]> {
    const response = await api.get<AIProviderInfo[]>('/user-ai-config/providers')
    return response.data
  },

  // 重置使用统计
  async resetStats(id: number): Promise<void> {
    await api.post(`/user-ai-config/${id}/reset-stats`)
  },

  // 导入配置
  async importConfig(configData: any): Promise<UserAIConfigResponse> {
    const response = await api.post<UserAIConfigResponse>('/user-ai-config/import', configData)
    return response.data
  },

  // 导出配置
  async exportConfig(id: number): Promise<any> {
    const response = await api.get(`/user-ai-config/${id}/export`)
    return response.data
  }
}
