import api from '@/utils/request'

// 请求/响应类型定义
export interface ExamTemplateRequest {
  templateName?: string
  examTitle?: string
  description?: string
}

export interface ExamTemplateResponse {
  id: number
  templateName: string
  subject: string
  examTitle: string
  description: string
  totalQuestions: number
  matchedQuestions: number
  status: 'DRAFT' | 'READY' | 'APPLIED' | 'ARCHIVED'
  sourceFiles: string[]
  createdTime: string
  updatedTime: string
  matchingProgress: number
}

export interface ExamTemplateQuestion {
  id: number
  questionNumber: number
  questionContent: string
  sectionHeader?: string
  questionType?: string
  score?: number
  correctAnswer?: string
  options?: string
  explanation?: string
  isRequired?: boolean
  matchedQuestionId?: number
  isMatched?: boolean
  isConfirmed?: boolean
  matchingStrategy?: string
  matchingConfidence?: number
  matchingReason?: string
  hasIssues?: boolean
  issues?: string
  suggestions?: string
  originalIndex?: number
  sourceDocument?: string
  confirming?: boolean
  unconfirming?: boolean
}

export interface ExamTemplateQuestionUpdateRequest {
  sectionHeader?: string
  questionContent?: string
  questionType?: string
  score?: number
  options?: string
  correctAnswer?: string
  explanation?: string
}

// 类型别名以保持兼容性
export type ExamTemplate = ExamTemplateResponse

export interface TemplatePreview {
  templateQuestions: number
  templateMatchedQuestions: number
  documentsFound: number
  documentQuestions?: Record<number, string>
  unmatchedQuestions?: number[]
  matchedCount?: number
  totalDocumentQuestions?: number
  templateStatus: string
  canImport: boolean
  error?: string
}

export interface ImportResult {
  success: boolean
  startTime: string
  endTime: string
  totalProcessed: number
  successCount: number
  failureCount: number
  successfulStudents: string[]
  failedStudents: string[]
  errorMessages: string[]
  errorMessage?: string
  metadata?: Record<string, any>
}

// ApiResponse 包装类型
interface ApiResponse<T> {
  code: number
  message: string
  data: T
  success: boolean
}

const API_BASE = '/exam-templates'

export const examTemplateApi = {
  /**
   * 从学习通文档提取试卷模板
   */
  async extractTemplate(data: {
    subject: string
    classFolders: string[]
    templateName?: string
  }) {
    const params = new URLSearchParams()
    params.append('subject', data.subject)
    data.classFolders.forEach(folder => params.append('classFolders', folder))
    if (data.templateName) {
      params.append('templateName', data.templateName)
    }
    
    const response = await api.post<ApiResponse<ExamTemplateResponse>>(`${API_BASE}/extract`, null, { params })
    return response.data
  },

  /**
   * 获取用户的模板列表
   */
  async getUserTemplates(page = 0, size = 10) {
    const response = await api.get<ApiResponse<{
      content: ExamTemplateResponse[]
      totalElements: number
      totalPages: number
      size: number
      number: number
    }>>(`${API_BASE}/my`, {
      params: { page, size }
    })
    return response.data
  },

  /**
   * 根据ID获取模板详情
   */
  async getTemplateById(id: number) {
    const response = await api.get<ApiResponse<ExamTemplateResponse>>(`${API_BASE}/${id}`)
    return response.data.data
  },

  /**
   * 更新模板信息
   */
  async updateTemplate(id: number, data: ExamTemplateRequest) {
    const response = await api.put<ApiResponse<ExamTemplateResponse>>(`${API_BASE}/${id}`, data)
    return response.data
  },

  /**
   * 删除模板
   */
  async deleteTemplate(id: number) {
    const response = await api.delete<ApiResponse<void>>(`${API_BASE}/${id}`)
    return response.data
  },

  /**
   * 获取模板的题目列表
   */
  async getTemplateQuestions(id: number) {
    const response = await api.get<ApiResponse<ExamTemplateQuestion[]>>(`${API_BASE}/${id}/questions`)
    return response.data.data
  },

  /**
   * 手动匹配题目到题库
   */
  async matchQuestion(templateId: number, questionNumber: number, questionId: number) {
    const response = await api.post<ApiResponse<void>>(`${API_BASE}/${templateId}/questions/${questionNumber}/match`, null, {
      params: { questionId }
    })
    return response.data
  },

  /**
   * 取消题目匹配
   */
  async unmatchQuestion(templateId: number, questionNumber: number) {
    const response = await api.post<ApiResponse<void>>(`${API_BASE}/${templateId}/questions/${questionNumber}/unmatch`)
    return response.data
  },

  /**
   * 搜索候选题目以供匹配
   */
  async searchCandidateQuestions(templateId: number, questionNumber: number, keyword?: string, limit = 10) {
    const params: any = { limit }
    if (keyword) {
      params.keyword = keyword
    }
    
    const response = await api.get<ApiResponse<any[]>>(`${API_BASE}/${templateId}/questions/${questionNumber}/candidates`, { params })
    return response.data
  },

  /**
   * 标记模板为就绪状态
   */
  async markTemplateReady(templateId: number) {
    const response = await api.post<ApiResponse<ExamTemplateResponse>>(`${API_BASE}/${templateId}/ready`)
    return response.data
  },

  /**
   * 基于模板导入学生答案
   */
  async importAnswersWithTemplate(data: {
    templateId: number
    examId: number
    subject: string
    classFolder?: string
  }) {
    const response = await api.post<ApiResponse<ImportResult>>(`${API_BASE}/${data.templateId}/import-answers`, null, {
      params: {
        examId: data.examId,
        subject: data.subject,
        classFolder: data.classFolder
      }
    })
    return response.data
  },

  /**
   * 更新模板题目
   */
  async updateTemplateQuestion(templateId: number, questionId: number, data: ExamTemplateQuestionUpdateRequest) {
    const response = await api.put<ApiResponse<ExamTemplateQuestion>>(`${API_BASE}/${templateId}/questions/${questionId}`, data)
    return response.data.data
  },

  /**
   * 确认模板题目
   */
  async confirmTemplateQuestion(templateId: number, questionId: number) {
    const response = await api.post<ApiResponse<void>>(`${API_BASE}/${templateId}/questions/${questionId}/confirm`)
    return response.data
  },

  /**
   * 取消确认模板题目
   */
  async unconfirmTemplateQuestion(templateId: number, questionId: number) {
    const response = await api.post<ApiResponse<void>>(`${API_BASE}/${templateId}/questions/${questionId}/unconfirm`)
    return response.data
  }
}

// 导出便捷函数
export const getTemplateById = examTemplateApi.getTemplateById
export const getTemplateQuestions = examTemplateApi.getTemplateQuestions
export const updateTemplateQuestion = examTemplateApi.updateTemplateQuestion
export const confirmTemplateQuestion = examTemplateApi.confirmTemplateQuestion
export const unconfirmTemplateQuestion = examTemplateApi.unconfirmTemplateQuestion
export const markTemplateReady = examTemplateApi.markTemplateReady
export const importAnswersFromTemplate = examTemplateApi.importAnswersWithTemplate