import api from '@/utils/request'
import type { QuestionBank, Question, PageResponse } from '@/types/api'

export const questionBankApi = {
  // 创建题目库
  async createQuestionBank(data: {
    name: string
    description?: string
    subject?: string
    gradeLevel?: string
    isPublic?: boolean
  }): Promise<QuestionBank> {
    const response = await api.post<QuestionBank>('/question-banks', data)
    return response.data
  },

  // 获取我的题目库列表
  async getMyQuestionBanks(page = 0, size = 10): Promise<PageResponse<QuestionBank>> {
    const response = await api.get<PageResponse<QuestionBank>>('/question-banks/my', {
      params: { page, size }
    })
    return response.data
  },

  // 获取可访问的题目库列表（包括公开的）
  async getAccessibleQuestionBanks(page = 0, size = 10): Promise<PageResponse<QuestionBank>> {
    const response = await api.get<PageResponse<QuestionBank>>('/question-banks', {
      params: { page, size }
    })
    return response.data
  },

  // 获取题目库详情
  async getQuestionBank(id: number): Promise<QuestionBank> {
    const response = await api.get<QuestionBank>(`/question-banks/${id}`)
    return response.data
  },

  // 更新题目库
  async updateQuestionBank(id: number, data: {
    name?: string
    description?: string
    subject?: string
    gradeLevel?: string
    isPublic?: boolean
  }): Promise<QuestionBank> {
    const response = await api.put<QuestionBank>(`/question-banks/${id}`, data)
    return response.data
  },

  // 删除题目库
  async deleteQuestionBank(id: number): Promise<void> {
    await api.delete(`/question-banks/${id}`)
  },

  // 获取题目库中的题目
  async getQuestionBankQuestions(id: number, page = 0, size = 10): Promise<PageResponse<Question>> {
    const response = await api.get<PageResponse<Question>>(`/question-banks/${id}/questions`, {
      params: { page, size }
    })
    return response.data
  },

  // 添加题目到题目库
  async addQuestionToBank(bankId: number, questionId: number): Promise<void> {
    await api.post(`/question-banks/${bankId}/questions/${questionId}`)
  },

  // 从题目库移除题目
  async removeQuestionFromBank(bankId: number, questionId: number): Promise<void> {
    await api.delete(`/question-banks/${bankId}/questions/${questionId}`)
  },

  // 获取题目库统计信息
  async getQuestionBankStatistics(id: number): Promise<{ questionCount: number }> {
    const response = await api.get<{ questionCount: number }>(`/question-banks/${id}/statistics`)
    return response.data
  },

  // 获取所有科目
  async getAllSubjects(): Promise<string[]> {
    const response = await api.get<string[]>('/question-banks/metadata/subjects')
    return response.data
  },

  // 获取所有年级
  async getAllGradeLevels(): Promise<string[]> {
    const response = await api.get<string[]>('/question-banks/metadata/grade-levels')
    return response.data
  }
}
