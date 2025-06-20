import api from '@/utils/request'
import type { ExamResponse, ExamCreateRequest, ExamStatistics } from '@/types/api'

export const examApi = {
  // 创建考试
  async createExam(data: ExamCreateRequest): Promise<ExamResponse> {
    const response = await api.post<ExamResponse>('/exams', data)
    return response.data
  },

  // 获取考试详情
  async getExam(id: number): Promise<ExamResponse> {
    const response = await api.get<ExamResponse>(`/exams/${id}`)
    return response.data
  },

  // 获取考试列表
  async getAllExams(page = 0, size = 10): Promise<ExamResponse[]> {
    const response = await api.get<ExamResponse[]>('/exams', {
      params: { page, size }
    })
    return response.data
  },

  // 更新考试
  async updateExam(id: number, data: ExamCreateRequest): Promise<ExamResponse> {
    const response = await api.put<ExamResponse>(`/exams/${id}`, data)
    return response.data
  },

  // 删除考试
  async deleteExam(id: number): Promise<void> {
    await api.delete(`/exams/${id}`)
  },

  // 搜索考试
  async searchExams(keyword: string): Promise<ExamResponse[]> {
    const response = await api.get<ExamResponse[]>('/exams/search', {
      params: { keyword }
    })
    return response.data
  },

  // 获取考试统计信息
  async getExamStatistics(examId: number): Promise<ExamStatistics> {
    const response = await api.get<ExamStatistics>(`/exams/${examId}/statistics`)
    return response.data
  },

  // 发布考试
  async publishExam(examId: number): Promise<ExamResponse> {
    const response = await api.post<ExamResponse>(`/exams/${examId}/publish`)
    return response.data
  },

  // 结束考试
  async endExam(examId: number): Promise<ExamResponse> {
    const response = await api.post<ExamResponse>(`/exams/${examId}/end`)
    return response.data
  },

  // 获取考试列表（简化版本）
  async getExams(params?: any): Promise<any> {
    const response = await api.get('/exams', { params })
    return response.data
  },

  // 获取我的考试（学生）
  async getMyExams(params?: any): Promise<any> {
    const response = await api.get('/exams/my', { params })
    return response.data
  }
}
