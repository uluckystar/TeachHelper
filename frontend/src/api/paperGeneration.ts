import api from '@/utils/request'

export interface PaperGenerationRequest {
  title: string
  description?: string
  knowledgeBaseId: string
  multipleChoiceCount: number
  fillBlankCount: number
  shortAnswerCount: number
  difficulty: 'EASY' | 'MEDIUM' | 'HARD'
  duration: number
  totalScore: number
  requirements?: string
}

export interface PaperGenerationResponse {
  id: number
  title: string
  description?: string
  questions: Question[]
  totalScore: number
  duration: number
  createdAt: string
  createdBy: number
}

export interface Question {
  id: number
  content: string
  type: 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'FILL_BLANK' | 'SHORT_ANSWER' | 'ESSAY'
  difficulty: 'EASY' | 'MEDIUM' | 'HARD'
  score: number
  options?: QuestionOption[]
  answer?: string
  explanation?: string
}

export interface QuestionOption {
  id: number
  content: string
  isCorrect: boolean
}

export const paperGenerationApi = {
  // 生成试卷（同步）
  async generatePaper(request: PaperGenerationRequest): Promise<PaperGenerationResponse> {
    const response = await api.post<PaperGenerationResponse>('/knowledge/paper-generation/generate', request)
    return response.data
  },

  // 基于模板生成试卷
  async generateFromTemplate(templateId: number, paperTitle: string): Promise<PaperGenerationResponse> {
    const response = await api.post<PaperGenerationResponse>(`/knowledge/paper-generation/templates/${templateId}/generate`, {
      paperTitle
    })
    return response.data
  },

  // 获取模板列表
  async getTemplates(params?: any): Promise<any> {
    const response = await api.get('/knowledge/paper-generation/templates', { params })
    return response.data
  },

  // 创建模板
  async createTemplate(template: any): Promise<any> {
    const response = await api.post('/knowledge/paper-generation/templates', template)
    return response.data
  },

  // 更新模板
  async updateTemplate(templateId: number, template: any): Promise<any> {
    const response = await api.put(`/knowledge/paper-generation/templates/${templateId}`, template)
    return response.data
  },

  // 删除模板
  async deleteTemplate(templateId: number): Promise<void> {
    await api.delete(`/knowledge/paper-generation/templates/${templateId}`)
  }
}
