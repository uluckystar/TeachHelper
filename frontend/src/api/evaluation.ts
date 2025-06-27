import api from '@/utils/request'
import type {
  StudentAnswerResponse,
  ManualEvaluationRequest,
  BatchEvaluationResult,
  EvaluationStatistics
} from '@/types/api'

export const evaluationApi = {
  // 评估单个答案
  async evaluateAnswer(answerId: number): Promise<StudentAnswerResponse> {
    const response = await api.post<StudentAnswerResponse>(`/evaluations/answer/${answerId}`)
    return response.data
  },

  // 手动评估答案
  async manuallyEvaluateAnswer(answerId: number, request: ManualEvaluationRequest): Promise<StudentAnswerResponse> {
    const response = await api.post<StudentAnswerResponse>(`/evaluations/manual/${answerId}`, request)
    return response.data
  },

  // 批量评估答案
  async batchEvaluateAnswers(answerIds: number[]): Promise<string> {
    const response = await api.post<string>('/evaluations/batch', answerIds)
    return response.data
  },

  // 评估题目的所有答案
  async evaluateAnswersByQuestion(questionId: number): Promise<string> {
    const response = await api.post<string>(`/evaluations/batch/question/${questionId}`)
    return response.data
  },

  // 评估考试的所有答案
  async evaluateAnswersByExam(examId: number): Promise<string> {
    const response = await api.post<string>(`/evaluations/batch/exam/${examId}`)
    return response.data
  },

  // 重新评估答案
  async revaluateAnswer(answerId: number): Promise<StudentAnswerResponse> {
    const response = await api.post<StudentAnswerResponse>(`/evaluations/revaluate/${answerId}`)
    return response.data
  },

  // 批量重新评估题目的所有已评估答案
  async batchRevaluateAnswersByQuestion(questionId: number): Promise<string> {
    const response = await api.post<string>(`/evaluations/batch/revaluate/question/${questionId}`)
    return response.data
  },

  // 批量重新评估所有答案（包括未评估和已评估的）
  async batchEvaluateAllAnswersByQuestion(questionId: number): Promise<string> {
    const response = await api.post<string>(`/evaluations/batch/all/question/${questionId}`)
    return response.data
  },

  // 获取题目评估统计
  async getEvaluationStatistics(questionId: number): Promise<EvaluationStatistics> {
    const response = await api.get<EvaluationStatistics>(`/evaluations/statistics/question/${questionId}`)
    return response.data
  },

  // 获取题目评估进度
  async getEvaluationProgress(questionId: number): Promise<number> {
    const response = await api.get<number>(`/evaluations/progress/question/${questionId}`)
    return response.data
  },

  // 获取批量评估结果
  async getBatchEvaluationResult(taskId: string): Promise<BatchEvaluationResult> {
    const response = await api.get<BatchEvaluationResult>(`/evaluations/result/${taskId}`)
    return response.data
  },

  // 获取批量评估进度
  async getBatchEvaluationProgress(taskId: string): Promise<number> {
    const response = await api.get<number>(`/evaluations/progress/${taskId}`)
    return response.data
  },

  // 批量评估题目的所有答案（别名方法）
  async batchEvaluateAnswersByQuestion(questionId: number): Promise<string> {
    return this.evaluateAnswersByQuestion(questionId)
  },

  // AI评估单个答案（别名方法）
  async aiEvaluateAnswer(answerId: number): Promise<StudentAnswerResponse> {
    return this.evaluateAnswer(answerId)
  },

  // 获取题目评估统计（别名方法）
  async getQuestionStatistics(questionId: number): Promise<EvaluationStatistics> {
    return this.getEvaluationStatistics(questionId)
  },

  // 获取最近的评估任务
  async getRecentTasks(): Promise<any[]> {
    const response = await api.get('/evaluations/tasks/recent')
    return response.data
  },

  // 预检查批量评估任务
  async precheckBatchTask(data: any): Promise<any> {
    const response = await api.post('/evaluations/tasks/precheck', data)
    return response.data
  },

  // 创建批量评估任务
  async createBatchTask(data: any): Promise<any> {
    const response = await api.post('/evaluations/tasks', data)
    return response.data
  },

  // 取消评估任务
  async cancelTask(taskId: string): Promise<void> {
    await api.post(`/evaluations/tasks/${taskId}/cancel`)
  },

  // 获取考试统计数据
  async getExamStatistics(examId: number): Promise<any> {
    const response = await api.get(`/evaluations/exam/${examId}/statistics`)
    return response.data
  },

  // 开始批量评估
  async startBatchEvaluation(examId: number, data: any): Promise<any> {
    const response = await api.post(`/evaluations/exam/${examId}/batch`, data)
    return response.data
  },

  // 获取任务状态
  async getTaskStatus(taskId: string): Promise<any> {
    const response = await api.get(`/evaluations/tasks/${taskId}/status`)
    return response.data
  },

  // 批量调分
  async batchAdjustScores(examId: number, data: any): Promise<any> {
    const response = await api.post(`/evaluations/exam/${examId}/adjust-scores`, data)
    return response.data
  },

  // 更新评分标准
  async updateRubricSettings(examId: number, data: any): Promise<any> {
    const response = await api.put(`/evaluations/exam/${examId}/rubric`, data)
    return response.data
  },

  // 导出评估报告
  async exportEvaluationReport(examId: number): Promise<any> {
    const response = await api.get(`/evaluations/exam/${examId}/export`, {
      responseType: 'blob'
    })
    return response
  },

  // 获取任务列表（分页）
  async getTasks(params?: any): Promise<any> {
    const response = await api.get('/tasks', { params })
    return response.data
  },

  // 获取任务统计信息
  async getTaskStats(): Promise<EvaluationStatistics> {
    const response = await api.get('/evaluations/tasks/stats')
    return response.data
  },

  // 获取任务详情
  async getTaskDetail(taskId: string): Promise<any> {
    const response = await api.get(`/evaluations/tasks/${taskId}`)
    return response.data
  },

  // 获取任务日志
  async getTaskLogs(taskId: string): Promise<any> {
    const response = await api.get(`/evaluations/tasks/${taskId}/logs`)
    return response.data
  },

  // 获取任务结果
  async getTaskResults(taskId: string, params?: any): Promise<any> {
    const response = await api.get(`/evaluations/tasks/${taskId}/results`, { params })
    return response.data
  },

  // 导出任务结果
  async exportTaskResults(taskId: string, format: string = 'excel'): Promise<any> {
    const response = await api.get(`/evaluations/tasks/${taskId}/results/export`, {
      params: { format },
      responseType: 'blob'
    })
    return response
  },

  // 重启任务
  async restartTask(taskId: string): Promise<any> {
    const response = await api.post(`/evaluations/tasks/${taskId}/restart`)
    return response.data
  },

  // 暂停任务
  async pauseTask(taskId: string): Promise<any> {
    const response = await api.post(`/evaluations/tasks/${taskId}/pause`)
    return response.data
  },

  // 恢复任务
  async resumeTask(taskId: string): Promise<any> {
    const response = await api.post(`/evaluations/tasks/${taskId}/resume`)
    return response.data
  }
}
