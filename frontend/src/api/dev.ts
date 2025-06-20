import api from '@/utils/request'

// 开发工具相关API
export const devApi = {
  // 生成示例数据 (需要登录和管理员权限)
  async generateSampleData(): Promise<string> {
    const response = await api.post<string>('/dev/generate-sample-data')
    return response.data
  },
  
  // 初始化数据 (无需登录，仅开发环境)
  async initializeData(): Promise<string> {
    const response = await api.post<string>('/dev/init-data')
    return response.data
  }
}
