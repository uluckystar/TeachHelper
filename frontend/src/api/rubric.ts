import api from '@/utils/request'
import type {
  RubricGenerateResponse,
  RubricApplyResponse,
  RubricCriterionResponse,
  Rubric
} from '@/types/api'

export const rubricApi = {
  // 系统级别评估标准管理

  // 获取所有评估标准
  async getAllRubrics(): Promise<Rubric[]> {
    const response = await api.get<Rubric[]>('/rubrics')
    return response.data
  },

  // 创建评估标准
  async createRubric(data: Omit<Rubric, 'id'>): Promise<Rubric> {
    const response = await api.post<Rubric>('/rubrics', data)
    return response.data
  },

  // 更新评估标准
  async updateRubric(id: number, data: Partial<Rubric>): Promise<Rubric> {
    const response = await api.put<Rubric>(`/rubrics/${id}`, data)
    return response.data
  },

  // 删除评估标准
  async deleteRubric(id: number): Promise<void> {
    await api.delete(`/rubrics/${id}`)
  },

  // 切换评估标准状态
  async toggleRubricStatus(id: number, isActive: boolean): Promise<Rubric> {
    const response = await api.put<Rubric>(`/rubrics/${id}/status`, { isActive })
    return response.data
  },

  // 获取单个评估标准详情
  async getRubricById(id: number): Promise<Rubric> {
    const response = await api.get<Rubric>(`/rubrics/${id}`)
    return response.data
  },

  // 题目级别评分标准管理（保持现有功能）

  // 生成AI评分标准
  async generateRubric(questionId: number): Promise<RubricGenerateResponse> {
    const response = await api.post<RubricGenerateResponse>(`/questions/${questionId}/generate-rubric`, {}, {
      timeout: 60000 // 设置60秒超时，因为AI生成需要更多时间
    })
    return response.data
  },

  // 应用评分标准
  async applyRubric(questionId: number): Promise<RubricApplyResponse> {
    const response = await api.post<RubricApplyResponse>(`/questions/${questionId}/apply-rubric-suggestions`)
    return response.data
  },

  // 获取题目的评分标准
  async getQuestionRubrics(questionId: number): Promise<RubricCriterionResponse[]> {
    const response = await api.get<RubricCriterionResponse[]>(`/questions/${questionId}/rubric`)
    return response.data
  },

  // 获取题目的评分标准（别名方法）
  async getCriteriaByQuestion(questionId: number): Promise<RubricCriterionResponse[]> {
    return this.getQuestionRubrics(questionId)
  },

  // 创建评分标准
  async createCriterion(data: { 
    questionId: number
    criterionText: string
    points: number
    description: string
  }): Promise<RubricCriterionResponse> {
    const response = await api.post<RubricCriterionResponse>(`/questions/${data.questionId}/rubric`, {
      criterionText: data.criterionText,
      points: data.points,
      description: data.description
    })
    return response.data
  },

  // 更新评分标准
  async updateCriterion(criterionId: number, data: {
    criterionText?: string
    points?: number
    description?: string
  }): Promise<RubricCriterionResponse> {
    const response = await api.put<RubricCriterionResponse>(`/questions/rubric/${criterionId}`, {
      criterionText: data.criterionText,
      points: data.points
    })
    return response.data
  },

  // 删除问题级别评分标准
  async deleteQuestionRubric(criterionId: number): Promise<void> {
    await api.delete(`/questions/rubric/${criterionId}`)
  },

  // 删除评分标准（别名方法）
  async deleteCriterion(criterionId: number): Promise<void> {
    return this.deleteQuestionRubric(criterionId)
  },

  // 更新问题级别评分标准（别名方法保持向后兼容）
  async updateQuestionRubric(criterionId: number, criterion: Partial<RubricCriterionResponse>): Promise<RubricCriterionResponse> {
    const response = await api.put<RubricCriterionResponse>(`/questions/rubric/${criterionId}`, criterion)
    return response.data
  }
}
