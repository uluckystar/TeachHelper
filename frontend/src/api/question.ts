import api from '@/utils/request'
import type { QuestionResponse, QuestionCreateRequest, RubricCriterion, RubricSuggestionResponse, RubricSuggestionItem, AIGenerationTaskResponse, AIGenerationStatusResponse } from '@/types/api'

export const questionApi = {
  // 创建题目
  async createQuestion(data: QuestionCreateRequest): Promise<QuestionResponse> {
    const response = await api.post<QuestionResponse>('/questions', data)
    return response.data
  },

  // 获取题目详情
  async getQuestion(id: number): Promise<QuestionResponse> {
    const response = await api.get<QuestionResponse>(`/questions/${id}`)
    return response.data
  },

  // 获取题目详情（别名方法）
  async getQuestionById(id: number): Promise<QuestionResponse> {
    const response = await api.get<QuestionResponse>(`/questions/${id}`)
    return response.data
  },

  // 获取题目列表（支持分页）
  async getAllQuestions(page = 0, size = 10): Promise<QuestionResponse[]> {
    const response = await api.get<QuestionResponse[]>('/questions', {
      params: { page, size }
    })
    return response.data
  },

  // 根据考试ID获取题目列表
  async getQuestionsByExam(examId: number): Promise<QuestionResponse[]> {
    const response = await api.get<QuestionResponse[]>(`/questions/exam/${examId}`)
    return response.data
  },

  // 学生参加考试时获取题目列表（不包含答案）
  async getQuestionsForTaking(examId: number): Promise<QuestionResponse[]> {
    const response = await api.get<QuestionResponse[]>(`/questions/exam/${examId}/take`)
    return response.data
  },

  // 更新题目
  async updateQuestion(id: number, data: QuestionCreateRequest): Promise<QuestionResponse> {
    const response = await api.put<QuestionResponse>(`/questions/${id}`, data)
    return response.data
  },

  // 删除题目
  async deleteQuestion(id: number): Promise<void> {
    await api.delete(`/questions/${id}`)
  },

  // 评分标准相关API
  // 添加评分标准
  async addRubricCriterion(questionId: number, criterion: RubricCriterion): Promise<RubricCriterion> {
    const response = await api.post<RubricCriterion>(`/questions/${questionId}/rubric`, criterion)
    return response.data
  },

  // 获取题目的评分标准
  async getRubricCriteria(questionId: number): Promise<RubricCriterion[]> {
    const response = await api.get<RubricCriterion[]>(`/questions/${questionId}/rubric`)
    return response.data
  },

  // 更新评分标准
  async updateRubricCriterion(criterionId: number, criterion: RubricCriterion): Promise<RubricCriterion> {
    const response = await api.put<RubricCriterion>(`/questions/rubric/${criterionId}`, criterion)
    return response.data
  },

  // 删除评分标准
  async deleteRubricCriterion(criterionId: number): Promise<void> {
    await api.delete(`/questions/rubric/${criterionId}`)
  },

  // AI评分标准生成相关API
  // 生成AI评分标准建议
  async generateRubric(questionId: number): Promise<RubricSuggestionItem[]> {
    const response = await api.post<RubricSuggestionItem[]>(`/questions/${questionId}/generate-rubric`, {}, {
      timeout: 0 // 不设置超时限制，AI生成需要更多时间
    })
    return response.data
  },

  // 应用AI生成的评分标准建议
  async applyRubricSuggestions(questionId: number, suggestions: RubricSuggestionItem[]): Promise<RubricCriterion[]> {
    const response = await api.post<RubricCriterion[]>(`/questions/${questionId}/apply-rubric-suggestions`, suggestions)
    return response.data
  },

  // 获取题目列表（支持搜索和筛选）
  async getQuestions(params?: any): Promise<any> {
    const response = await api.get('/questions', { params })
    return response.data
  },

  // 获取题目列表（支持来源筛选和完整分页信息）
  async getQuestionsWithPagination(params?: {
    page?: number
    size?: number
    keyword?: string
    questionType?: string
    subject?: string
    gradeLevel?: string
    examId?: number
    source?: 'SELF_CREATED' | 'INTERNET' | 'AI_GENERATED' | 'AI_ORGANIZED'
    questionBankId?: number
    sourceKnowledgeBaseId?: number
  }): Promise<{ 
    content: QuestionResponse[]
    totalElements: number
    totalPages: number
    number: number
    size: number
    numberOfElements: number
    first: boolean
    last: boolean
    empty: boolean
  }> {
    const response = await api.get('/questions', { params })
    return response.data
  },

  // 流式AI评分标准生成相关API
  // 创建异步AI生成任务
  async generateRubricAsync(questionId: number): Promise<AIGenerationTaskResponse> {
    const response = await api.post<AIGenerationTaskResponse>(`/questions/${questionId}/generate-rubric-async`)
    return response.data
  },

  // 智能AI评分标准生成（带现有评分标准检测）
  async generateRubricSmart(questionId: number, customPrompt?: string): Promise<any> {
    const data = customPrompt ? { customPrompt } : {}
    const response = await api.post(`/questions/${questionId}/generate-rubric-smart`, data)
    return response.data
  },

  // 根据用户选择的模式生成评分标准
  async generateRubricWithMode(questionId: number, mode: string, targetScore?: number, confirmed?: boolean, customPrompt?: string): Promise<AIGenerationTaskResponse> {
    const params = new URLSearchParams({ mode })
    if (targetScore !== undefined && targetScore !== null) params.append('targetScore', targetScore.toString())
    if (confirmed !== undefined && confirmed !== null) params.append('confirmed', confirmed.toString())
    
    const data = customPrompt ? { customPrompt } : {}
    const response = await api.post<AIGenerationTaskResponse>(`/questions/${questionId}/generate-rubric-with-mode?${params}`, data)
    return response.data
  },

  // 检查现有评分标准
  async checkExistingRubrics(questionId: number): Promise<any> {
    const response = await api.get(`/questions/${questionId}/existing-rubrics`)
    return response.data
  },

  // 查询AI生成状态
  async getGenerationStatus(taskId: string): Promise<AIGenerationStatusResponse> {
    const response = await api.get<AIGenerationStatusResponse>(`/questions/generation-status/${taskId}`)
    return response.data
  },

  // 选择性替换评分标准
  async replaceRubricsSelective(questionId: number, data: { selectedSuggestions: any[], replaceAll?: boolean }): Promise<any> {
    const response = await api.post(`/questions/${questionId}/replace-rubrics-selective`, data)
    return response.data
  },

  // 重新生成单个评分标准
  async regenerateSingleRubric(questionId: number, data: { criterionIndex: string, customPrompt?: string }): Promise<any> {
    const response = await api.post(`/questions/${questionId}/regenerate-single-rubric`, data)
    return response.data
  },

  // 取消AI生成任务
  async cancelGenerationTask(taskId: string): Promise<void> {
    await api.delete(`/questions/generation-task/${taskId}`)
  },

  // AI题目生成相关API
  // 生成题目（使用任务管理系统）
  async generateQuestions(data: any): Promise<any> {
    const response = await api.post('/tasks', {
      type: 'AI_GENERATION',
      name: `AI题目生成 - ${data.questionTypes?.join(', ') || '题目'}`,
      description: `生成${data.count || 1}道题目`,
      configuration: { ...data, action: 'GENERATE_QUESTIONS' },
      priority: 'MEDIUM',
      autoStart: true
    })
    return { taskId: response.data.id }
  },

  // 保存生成的题目
  async saveGeneratedQuestions(data: any): Promise<any> {
    const response = await api.post('/questions/save-generated', data)
    return response.data
  },

  // 获取知识点列表
  async getKnowledgePoints(): Promise<any> {
    const response = await api.get('/knowledge/points')
    return response.data
  },

  // 任务管理相关方法
  // 获取题目生成任务列表
  async getQuestionTasks(params?: any): Promise<any> {
    const response = await api.get('/tasks', {
      params: {
        type: 'AI_GENERATION',
        action: 'GENERATE_QUESTIONS',
        ...params
      }
    })
    return response.data
  },

  // 获取任务详情
  async getTaskDetail(taskId: string): Promise<any> {
    const response = await api.get(`/tasks/${taskId}`)
    return response.data
  },

  // 获取任务日志
  async getTaskLogs(taskId: string): Promise<any> {
    const response = await api.get(`/tasks/${taskId}/logs`)
    return response.data
  },

  // 获取任务结果
  async getTaskResults(taskId: string, params?: any): Promise<any> {
    const response = await api.get(`/tasks/${taskId}/results`, { params })
    return response.data
  },

  // 任务控制方法
  async pauseTask(taskId: string): Promise<any> {
    const response = await api.post(`/tasks/${taskId}/pause`)
    return response.data
  },

  async resumeTask(taskId: string): Promise<any> {
    const response = await api.post(`/tasks/${taskId}/resume`)
    return response.data
  },

  async retryTask(taskId: string): Promise<any> {
    const response = await api.post(`/tasks/${taskId}/retry`)
    return response.data
  },

  async deleteTask(taskId: string): Promise<void> {
    await api.delete(`/tasks/${taskId}`)
  },

  async exportTaskResults(taskId: string, format: string = 'excel'): Promise<any> {
    const response = await api.get(`/tasks/${taskId}/export`, {
      params: { format },
      responseType: 'blob'
    })
    return response
  },

  // 获取任务统计
  async getTaskStatistics(): Promise<any> {
    const response = await api.get('/tasks/stats')
    return response.data
  },

  // 确认AI整理的题目
  async confirmAIOrganizedQuestion(questionId: number): Promise<void> {
    await api.patch(`/questions/${questionId}/confirm-ai-organized`)
  },

  // AI生成参考答案
  async generateReferenceAnswer(questionId: number): Promise<{ referenceAnswer: string }> {
    // AI生成需要更长时间，不设置超时限制
    const response = await api.post<{ referenceAnswer: string }>(`/questions/${questionId}/generate-reference-answer`, {}, {
      timeout: 0 // 不设置超时限制
    })
    return response.data
  },
}

export default questionApi
