import api from '@/utils/request'

// 任务管理通用API
export const taskApi = {
  // 获取任务列表（支持分页和筛选）
  async getTasks(params?: {
    page?: number
    size?: number
    type?: string
    status?: string
    priority?: string
    name?: string
    sort?: string
  }): Promise<any> {
    const response = await api.get('/tasks', { params })
    return response.data
  },

  // 获取任务统计信息
  async getTaskStatistics(): Promise<any> {
    const response = await api.get('/tasks/stats')
    return response.data
  },

  // 创建新任务
  async createTask(data: {
    type: string
    name: string
    description?: string
    configuration?: any
    priority?: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
    autoStart?: boolean
  }): Promise<any> {
    const response = await api.post('/tasks', data)
    return response.data
  },

  // 获取任务详情
  async getTaskDetail(taskId: string): Promise<any> {
    const response = await api.get(`/tasks/${taskId}`)
    return response.data
  },

  // 更新任务
  async updateTask(taskId: string, data: any): Promise<any> {
    const response = await api.put(`/tasks/${taskId}`, data)
    return response.data
  },

  // 删除任务
  async deleteTask(taskId: string): Promise<void> {
    await api.delete(`/tasks/${taskId}`)
  },

  // 启动任务
  async startTask(taskId: string): Promise<any> {
    const response = await api.post(`/tasks/${taskId}/start`)
    return response.data
  },

  // 暂停任务
  async pauseTask(taskId: string): Promise<any> {
    const response = await api.post(`/tasks/${taskId}/pause`)
    return response.data
  },

  // 恢复任务
  async resumeTask(taskId: string): Promise<any> {
    const response = await api.post(`/tasks/${taskId}/resume`)
    return response.data
  },

  // 取消任务
  async cancelTask(taskId: string): Promise<any> {
    const response = await api.post(`/tasks/${taskId}/cancel`)
    return response.data
  },

  // 重试任务
  async retryTask(taskId: string): Promise<any> {
    const response = await api.post(`/tasks/${taskId}/retry`)
    return response.data
  },

  // 获取任务日志
  async getTaskLogs(taskId: string, params?: {
    page?: number
    size?: number
    level?: string
  }): Promise<any> {
    const response = await api.get(`/tasks/${taskId}/logs`, { params })
    return response.data
  },

  // 获取任务结果
  async getTaskResults(taskId: string, params?: {
    page?: number
    size?: number
    format?: string
  }): Promise<any> {
    const response = await api.get(`/tasks/${taskId}/results`, { params })
    return response.data
  },

  // 导出任务结果
  async exportTaskResults(taskId: string, format: string = 'excel'): Promise<any> {
    const response = await api.get(`/tasks/${taskId}/results/export`, {
      params: { format },
      responseType: 'blob'
    })
    return response
  },

  // 批量操作
  // 批量暂停任务
  async batchPauseTasks(filter?: any): Promise<any> {
    const response = await api.post('/tasks/batch/pause', { filter })
    return response.data
  },

  // 批量恢复任务
  async batchResumeTasks(filter?: any): Promise<any> {
    const response = await api.post('/tasks/batch/resume', { filter })
    return response.data
  },

  // 批量取消任务
  async batchCancelTasks(filter?: any): Promise<any> {
    const response = await api.post('/tasks/batch/cancel', { filter })
    return response.data
  },

  // 清理已完成任务
  async clearCompletedTasks(filter?: any): Promise<any> {
    const response = await api.delete('/tasks/batch/clear', { 
      data: { filter } 
    })
    return response.data
  },

  // 实时任务监控
  // 获取运行中任务列表
  async getRunningTasks(): Promise<any> {
    const response = await api.get('/tasks', {
      params: { status: 'RUNNING' }
    })
    return response.data
  },

  // 获取最近任务
  async getRecentTasks(limit: number = 10): Promise<any> {
    const response = await api.get('/tasks', {
      params: { 
        size: limit,
        sort: 'createdAt,desc'
      }
    })
    return response.data
  },

  // 获取失败任务
  async getFailedTasks(): Promise<any> {
    const response = await api.get('/tasks', {
      params: { status: 'FAILED' }
    })
    return response.data
  },

  // 任务模板相关
  // 获取任务模板
  async getTaskTemplates(): Promise<any> {
    const response = await api.get('/tasks/templates')
    return response.data
  },

  // 从模板创建任务
  async createTaskFromTemplate(templateId: string, data: any): Promise<any> {
    const response = await api.post(`/tasks/templates/${templateId}/create`, data)
    return response.data
  },

  // 预设任务类型
  taskTypes: {
    BATCH_EVALUATION: 'BATCH_EVALUATION',
    AI_GENERATION: 'AI_GENERATION', 
    KNOWLEDGE_PROCESSING: 'KNOWLEDGE_PROCESSING'
  },

  // 任务状态
  taskStatuses: {
    PENDING: 'PENDING',
    RUNNING: 'RUNNING',
    PAUSED: 'PAUSED',
    COMPLETED: 'COMPLETED',
    FAILED: 'FAILED',
    CANCELLED: 'CANCELLED'
  },

  // 任务优先级
  taskPriorities: {
    LOW: 'LOW',
    MEDIUM: 'MEDIUM',
    HIGH: 'HIGH',
    URGENT: 'URGENT'
  }
}

export default taskApi
