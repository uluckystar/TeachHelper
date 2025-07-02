import request from '@/utils/request'

export interface ExamPaperTemplate {
  id: number
  name: string
  description?: string
  subject: string
  gradeLevel: string
  totalScore: number
  duration?: number
  status: 'DRAFT' | 'READY' | 'PUBLISHED' | 'ARCHIVED'
  templateType: 'MANUAL' | 'AI_GENERATED' | 'DOCUMENT_EXTRACTED' | 'COPIED'
  questionTypeConfig?: string
  difficultyConfig?: string
  knowledgeBaseConfig?: string
  tags?: string
  isPublic: boolean
  usageCount: number
  createdBy: number
  creatorName?: string
  createdAt: string
  updatedAt: string
  lastUsedAt?: string
  questionCount: number
  configuredQuestionCount: number
  isComplete: boolean
  isUsable: boolean
  questions: ExamPaperTemplateQuestion[]
}

export interface ExamPaperTemplateQuestion {
  id: number
  templateId: number
  questionOrder: number
  questionType: string
  questionContent?: string
  questionId?: number
  score?: number
  difficultyLevel?: string
  knowledgeTags?: string
  questionConfig?: string
  isRequired: boolean
  status: 'DRAFT' | 'CONFIGURED' | 'READY'
  createdAt: string
  updatedAt: string
  isConfigured: boolean
  isReady: boolean
  referencedQuestion?: QuestionSummary
}

export interface QuestionSummary {
  id: number
  content: string
  questionType: string
  score?: number
}

export interface ExamPaperTemplateRequest {
  name: string
  description?: string
  subject: string
  gradeLevel: string
  totalScore: number
  duration?: number
  status?: string
  templateType?: string
  questionTypeConfig?: string
  difficultyConfig?: string
  knowledgeBaseConfig?: string
  tags?: string
  isPublic?: boolean
  questions?: ExamPaperTemplateQuestionRequest[]
}

export interface ExamPaperTemplateQuestionRequest {
  id?: number
  questionOrder: number
  questionType: string
  questionContent?: string
  questionId?: number
  score?: number
  difficultyLevel?: string
  knowledgeTags?: string
  questionConfig?: string
  isRequired?: boolean
  status?: string
}

export const examPaperTemplateApi = {
  // 创建试卷模板
  createTemplate: (data: ExamPaperTemplateRequest) => {
    return request.post<ExamPaperTemplate>('/templates/exam-paper', data)
  },

  // 更新试卷模板
  updateTemplate: (id: number, data: ExamPaperTemplateRequest) => {
    return request.put<ExamPaperTemplate>(`/templates/exam-paper/${id}`, data)
  },

  // 删除试卷模板
  deleteTemplate: (id: number) => {
    return request.delete(`/templates/exam-paper/${id}`)
  },

  // 获取模板详情
  getTemplate: (id: number) => {
    return request.get<ExamPaperTemplate>(`/templates/exam-paper/${id}`)
  },

  // 获取用户模板列表
  getUserTemplates: () => {
    return request.get<ExamPaperTemplate[]>('/templates/exam-paper/user')
  },

  // 获取公开模板列表
  getPublicTemplates: () => {
    return request.get<ExamPaperTemplate[]>('/templates/exam-paper/public')
  },

  // 获取可用模板列表
  getUsableTemplates: () => {
    return request.get<ExamPaperTemplate[]>('/templates/exam-paper/usable')
  },

  // 搜索模板
  searchTemplates: (params?: { keyword?: string, subject?: string, gradeLevel?: string }) => {
    return request.get<ExamPaperTemplate[]>('/templates/exam-paper/search', { params })
  },

  // 复制模板
  duplicateTemplate: (id: number) => {
    return request.post<ExamPaperTemplate>(`/templates/exam-paper/${id}/duplicate`)
  },

  // 应用模板创建考试
  applyTemplateToExam: (templateId: number, examTitle: string, examDescription?: string) => {
    return request.post<any>(`/templates/exam-paper/${templateId}/apply`, null, {
      params: { examTitle, examDescription }
    })
  },

  // 应用模板到已有考试
  applyTemplateToExistingExam: (templateId: number, examId: number, replaceExisting?: boolean) => {
    return request.post<any>(`/templates/exam-paper/${templateId}/apply-to-existing`, null, {
      params: { examId, replaceExisting }
    })
  },

  // 获取可应用的考试列表
  getApplicableExams: () => {
    return request.get<any[]>('/templates/exam-paper/applicable-exams')
  },

  // 添加模板题目
  addTemplateQuestion: (templateId: number, questionData: any) => {
    return request.post<any>(`/templates/exam-paper/${templateId}/questions`, questionData)
  },

  // 更新模板题目
  updateTemplateQuestion: (templateId: number, questionId: number, questionData: any) => {
    return request.put<any>(`/templates/exam-paper/${templateId}/questions/${questionId}`, questionData)
  },

  // 删除模板题目
  deleteTemplateQuestion: (templateId: number, questionId: number) => {
    return request.delete(`/templates/exam-paper/${templateId}/questions/${questionId}`)
  },

  // 批量添加模板题目
  addTemplateQuestions: (templateId: number, questionsData: any[]) => {
    return request.post<any>(`/templates/exam-paper/${templateId}/questions/batch`, questionsData)
  },

  // 标记模板为就绪状态
  markTemplateReady: (templateId: number) => {
    return request.put<ExamPaperTemplate>(`/templates/exam-paper/${templateId}/status`, null, {
      params: { status: 'READY' }
    })
  },

  // 标记模板为已发布状态
  markTemplatePublished: (templateId: number) => {
    return request.put<ExamPaperTemplate>(`/templates/exam-paper/${templateId}/status`, null, {
      params: { status: 'PUBLISHED' }
    })
  },

  // 取消模板公开状态
  cancelTemplatePublished: (templateId: number) => {
    return request.put<ExamPaperTemplate>(`/templates/exam-paper/${templateId}/status`, null, {
      params: { status: 'READY' }
    })
  },

  // 标记题目为已配置状态
  markQuestionConfigured: (templateId: number, questionId: number) => {
    return request.put<ExamPaperTemplateQuestion>(`/templates/exam-paper/${templateId}/questions/${questionId}/status`, null, {
      params: { status: 'CONFIGURED' }
    })
  },

  // 标记题目为就绪状态
  markQuestionReady: (templateId: number, questionId: number) => {
    return request.put<ExamPaperTemplateQuestion>(`/templates/exam-paper/${templateId}/questions/${questionId}/status`, null, {
      params: { status: 'READY' }
    })
  },

  // 从学习通文档创建模板
  createFromDocument: (formData: FormData) => {
    return request.post<ExamPaperTemplate>('/templates/exam-paper/create-from-document', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  // 重新导入学习通文档
  reimportFromDocument: (templateId: number, formData: FormData) => {
    return request.put<ExamPaperTemplate>(`/templates/exam-paper/${templateId}/reimport-from-document`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
} 