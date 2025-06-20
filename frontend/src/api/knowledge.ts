import request from '@/utils/request'

export interface KnowledgeBase {
  id: number
  name: string
  description?: string
  subject?: string
  gradeLevel?: string
  createdBy: number
  createdAt: string
  updatedAt: string
  documentCount: number
  knowledgePointCount: number
  questionCount?: number
  favoriteCount?: number
  isFavorited?: boolean
}

export interface KnowledgeBaseCreateRequest {
  name: string
  description?: string
  subject?: string
  gradeLevel?: string
}

export interface KnowledgeDocument {
  id: number
  fileName: string
  fileType: string
  fileSize: number
  description?: string
  processingStatus: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  processingError?: string
  processingProgress?: number
  createdAt: string
  knowledgeBaseId: number
  knowledgeBaseName: string
}

export interface DocumentProcessingStatistics {
  total: number
  pending: number
  processing: number
  completed: number
  failed: number
}

export interface KnowledgePoint {
  id: number
  title: string
  description?: string
  difficulty: 'EASY' | 'MEDIUM' | 'HARD'
  tags: string[]
  knowledgeBaseId: number
  summary?: string
  isAIGenerated?: boolean
  createdAt: string
  updatedAt?: string
}

export interface KnowledgePointResponse {
  id: number
  title: string
  description?: string
  difficulty: 'EASY' | 'MEDIUM' | 'HARD'
  tags: string[]
  knowledgeBaseId: number
  summary?: string
  isAIGenerated?: boolean
  createdAt: string
  updatedAt?: string
}

export interface KnowledgePointStatistics {
  totalCount: number
  difficultyDistribution: {
    EASY: number
    MEDIUM: number
    HARD: number
  }
  tagDistribution: Record<string, number>
  aiGeneratedCount: number
}

export interface AIQuestionGenerationRequest {
  knowledgeBaseId: number
  questionTypes: ('SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'FILL_BLANK' | 'SHORT_ANSWER' | 'ESSAY')[]
  difficultyDistribution: {
    EASY: number
    MEDIUM: number
    HARD: number
  }
  knowledgePointIds?: number[]
  generationStrategy: 'KNOWLEDGE_BASED' | 'AI_GENERATED' | 'VECTOR_BASED' | 'INTERNET_SOURCE' | 'MIXED'
  aiConfigId: number
  tags?: string[]
  additionalRequirements?: string
}

export interface AIQuestionGenerationResponse {
  taskId: string
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
  progress?: number
  questions?: any[]
  error?: string
  totalTokens?: number
  processingTimeMs?: number
  generatedCount?: number
}

// 试卷生成相关类型定义
export interface PaperGenerationRequest {
  title: string
  description?: string
  subject: string
  gradeLevel: string
  totalScore: number
  timeLimit?: number
  questionTypes: QuestionTypeConfig[]
  difficulty?: DifficultyConfig
  knowledgeBaseIds?: string[]
  customRequirements?: string
}

export interface QuestionTypeConfig {
  type: 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'SHORT_ANSWER' | 'ESSAY' | 'CODING' | 'CASE_ANALYSIS'
  count: number
  scorePerQuestion: number
  requirements?: string
}

export interface DifficultyConfig {
  easyRatio: number
  mediumRatio: number
  hardRatio: number
}

export interface PaperGenerationResponse {
  examId: number
  examTitle: string
  examDescription?: string
  questions: GeneratedQuestion[]
  warnings: string[]
  generationSummary: string
  statistics: GenerationStatistics
  generatedAt: string
}

export interface GeneratedQuestion {
  id?: number
  title: string
  content: string
  questionType: string
  maxScore: number
  referenceAnswer?: string
  difficulty: string
  knowledgePoint?: string
  sourceDocument?: string
  options?: QuestionOption[]
}

export interface QuestionOption {
  content: string
  isCorrect: boolean
}

export interface GenerationStatistics {
  totalQuestions: number
  totalScore: number
  estimatedTime?: number
  typeStatistics: TypeStatistics
  difficultyStatistics: DifficultyStatistics
}

export interface TypeStatistics {
  singleChoice?: number
  multipleChoice?: number
  trueFalse?: number
  shortAnswer?: number
  essay?: number
  coding?: number
  caseAnalysis?: number
}

export interface DifficultyStatistics {
  easy: number
  medium: number
  hard: number
}

export interface PaperGenerationTemplate {
  id: number
  name: string
  description?: string
  subject: string
  gradeLevel: string
  totalScore: number
  timeLimit?: number
  questionConfig: string
  difficultyConfig: string
  knowledgeBaseConfig?: string
  createdBy: number
  creatorName?: string
  createdAt: string
  updatedAt: string
}

export interface PaperGenerationTemplateRequest {
  name: string
  description?: string
  subject: string
  gradeLevel: string
  totalScore: number
  timeLimit?: number
  questionConfig: string
  difficultyConfig: string
  knowledgeBaseConfig?: string
}

export interface RetrievedDocument {
  documentId: string
  content: string
  similarity: number
  metadata: Record<string, any>
  source?: string
}

// 系统状态检查API
export const systemApi = {
  // 检查后端状态
  checkBackendStatus: () => {
    return request.get('/debug/status')
  },

  // 检查AI服务状态
  checkAIStatus: () => {
    return request.get('/debug/ai/status')
  },

  // 检查数据库状态
  checkDatabaseStatus: () => {
    return request.get('/debug/database')
  },

  // 测试DeepSeek API
  testDeepSeek: (message: string = '你好，这是一个测试消息') => {
    return request.post('/debug/ai/test/deepseek', { message })
  },

  // 切换AI模型
  switchAIModel: (modelName: string) => {
    return request.post(`/debug/ai/switch-model/${modelName}`)
  }
}

// 向量搜索相关接口
export interface VectorSearchRequest {
  query: string
  similarityThreshold: number
  knowledgeBaseId?: number
  limit?: number
}

export interface VectorSearchResult {
  id: string
  title: string
  content: string
  type: 'document' | 'knowledge_point' | 'question' | 'chunk'
  source: string
  similarity: number
  metadata?: Record<string, any>
  // 增强的搜索结果字段
  highlightedContent?: string  // 高亮显示的内容
  matchedKeywords?: string[]   // 匹配的关键词
  keywordPositions?: Array<{   // 关键词位置信息
    keyword: string
    startIndex: number
    endIndex: number
    context: string
  }>
  knowledgeBaseId?: string     // 所属知识库ID
  knowledgeBaseName?: string   // 所属知识库名称
  documentId?: string          // 文档ID
  chunkIndex?: number          // 文档块索引
  contextBefore?: string       // 前文上下文
  contextAfter?: string        // 后文上下文
}

export interface VectorSearchResponse {
  results: VectorSearchResult[]
  totalCount: number
  searchTime: number
}

// 知识点分类相关接口
export interface KnowledgePointNode {
  id: string
  label: string
  type: 'subject' | 'chapter' | 'knowledge_point'
  count: number
  difficulty?: number
  children?: KnowledgePointNode[]
  documentCount?: number
  questionCount?: number
}

export interface ContentSnippet {
  id: string
  content: string
  source: string
  similarity: number
}

// 知识库API
export const knowledgeBaseApi = {
  // 创建知识库
  async createKnowledgeBase(data: KnowledgeBaseCreateRequest): Promise<KnowledgeBase> {
    const response = await request.post<KnowledgeBase>('/knowledge-bases', data)
    return response.data
  },

  // 获取知识库列表
  async getKnowledgeBases(params?: {
    page?: number
    size?: number
    subject?: string
    gradeLevel?: string
    name?: string
    sort?: string
    direction?: string
  }): Promise<{ content: KnowledgeBase[], totalElements: number }> {
    const response = await request.get('/knowledge-bases', { params })
    return response.data
  },

  // 获取我的知识库
  async getMyKnowledgeBases(): Promise<KnowledgeBase[]> {
    const response = await request.get<KnowledgeBase[]>('/knowledge-bases/my')
    return response.data
  },

  // 获取知识库详情
  async getKnowledgeBase(id: number): Promise<KnowledgeBase> {
    const response = await request.get<KnowledgeBase>(`/knowledge-bases/${id}`)
    return response.data
  },

  // 根据ID列表批量获取知识库（用于向量搜索）
  async getKnowledgeBasesByIds(ids: number[]): Promise<KnowledgeBase[]> {
    const response = await request.post<KnowledgeBase[]>('/knowledge-bases/by-ids', ids)
    return response.data
  },

  // 更新知识库
  async updateKnowledgeBase(id: number, data: KnowledgeBaseCreateRequest): Promise<KnowledgeBase> {
    const response = await request.put<KnowledgeBase>(`/knowledge-bases/${id}`, data)
    return response.data
  },

  // 删除知识库
  async deleteKnowledgeBase(id: number): Promise<void> {
    await request.delete(`/knowledge-bases/${id}`)
  },

  // 获取所有学科
  async getSubjects(): Promise<string[]> {
    const response = await request.get('/knowledge-bases/subjects')
    return response.data
  },

  // 获取所有年级
  async getGradeLevels(): Promise<string[]> {
    const response = await request.get('/knowledge-bases/grade-levels')
    return response.data
  },

  // 获取知识库的知识点
  async getKnowledgePoints(id: number): Promise<KnowledgePoint[]> {
    const response = await request.get<KnowledgePoint[]>(`/knowledge-bases/${id}/knowledge-points`)
    return response.data
  },

  // 获取知识点分类树
  getKnowledgePointTree: (knowledgeBaseId?: number) => {
    return request.get<KnowledgePointNode[]>('/knowledge/knowledge-points/tree', {
      params: { knowledgeBaseId }
    })
  },

  // 向量搜索
  vectorSearch: (searchRequest: VectorSearchRequest) => {
    return request.post<VectorSearchResponse>('/knowledge/vector-search', searchRequest)
  },

  // 获取相关内容片段
  getRelatedContent: (knowledgePointId: string, limit = 5) => {
    return request.get<ContentSnippet[]>(`/knowledge/knowledge-points/${knowledgePointId}/related-content`, {
      params: { limit }
    })
  },

  // 收藏知识库
  async favoriteKnowledgeBase(id: number): Promise<void> {
    await request.post(`/knowledge-bases/${id}/favorite`)
  },

  // 取消收藏知识库
  async unfavoriteKnowledgeBase(id: number): Promise<void> {
    await request.delete(`/knowledge-bases/${id}/favorite`)
  },

  // 切换收藏状态
  async toggleFavorite(id: number): Promise<boolean> {
    const response = await request.post<boolean>(`/knowledge-bases/${id}/toggle-favorite`)
    return response.data
  },

  // 获取收藏的知识库列表
  async getFavoriteKnowledgeBases(): Promise<KnowledgeBase[]> {
    const response = await request.get<KnowledgeBase[]>('/knowledge-bases/favorites')
    return response.data
  },
}

// 试卷生成API
export const paperGenerationApi = {
  // 生成试卷
  generatePaper: (data: PaperGenerationRequest) => {
    return request.post<PaperGenerationResponse>('/paper-generation/generate', data)
  },

  // 基于模板生成试卷
  generateFromTemplate: (templateId: number, title: string) => {
    return request.post<PaperGenerationResponse>(
      `/paper-generation/generate-from-template/${templateId}`,
      null,
      { params: { title } }
    )
  },

  // 创建模板
  createTemplate: (data: PaperGenerationTemplateRequest) => {
    return request.post<PaperGenerationTemplate>('/paper-generation/templates', data)
  },

  // 更新模板
  updateTemplate: (id: number, data: PaperGenerationTemplateRequest) => {
    return request.put<PaperGenerationTemplate>(`/paper-generation/templates/${id}`, data)
  },

  // 删除模板
  deleteTemplate: (id: number) => {
    return request.delete(`/paper-generation/templates/${id}`)
  },

  // 获取模板列表
  getTemplates: (params?: { subject?: string, gradeLevel?: string }) => {
    return request.get<PaperGenerationTemplate[]>('/paper-generation/templates', { params })
  },

  // 获取模板详情
  getTemplate: (id: number) => {
    return request.get<PaperGenerationTemplate>(`/paper-generation/templates/${id}`)
  },

  // 复制模板
  duplicateTemplate: (id: number) => {
    return request.post<PaperGenerationTemplate>(`/paper-generation/templates/${id}/duplicate`)
  },

  // 智能生成试卷
  smartGenerate: (data: PaperGenerationRequest) => {
    return request.post<PaperGenerationResponse>('/paper-generation/smart-generate', data)
  },

  // 知识库出题
  knowledgeGenerate: (data: PaperGenerationRequest) => {
    return request.post<PaperGenerationResponse>('/paper-generation/knowledge-generate', data)
  },

  // 题库组合
  bankGenerate: (data: PaperGenerationRequest) => {
    return request.post<PaperGenerationResponse>('/paper-generation/bank-generate', data)
  },

  // 获取生成历史
  getHistory: (params?: { 
    page?: number
    size?: number
    startDate?: string
    endDate?: string
    status?: string
    knowledgeBaseId?: string
  }) => {
    return request.get('/paper-generation/history', { params })
  },

  // 重新生成
  regenerate: (id: string) => {
    return request.post(`/paper-generation/regenerate/${id}`)
  },

  // 删除历史
  deleteHistory: (id: string) => {
    return request.delete(`/paper-generation/history/${id}`)
  },

  // 清空所有历史
  clearHistory: () => {
    return request.delete('/paper-generation/history/clear')
  },

  // 下载试卷
  download: (id: string) => {
    return request.get(`/paper-generation/download/${id}`, {
      responseType: 'blob'
    })
  }
}

// 文档上传配置接口
export interface DocumentUploadConfig {
  ocrEngine?: 'tesseract' | 'baidu' | 'tencent'
  ocrLanguage?: string
  extractKnowledgePoints?: boolean
  autoClassifyDifficulty?: boolean
  enableVectorization?: boolean
  chunkSize?: number
  chunkOverlap?: number
}

// 文档管理API
export const documentApi = {
  // 上传文档 (新版本 - 支持更多配置)
  uploadDocument: (
    file: File,
    knowledgeBaseId: number,
    title: string,
    description?: string,
    config: DocumentUploadConfig = {}
  ) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('knowledgeBaseId', knowledgeBaseId.toString())
    formData.append('title', title)
    if (description) formData.append('description', description)
    
    // 添加配置参数
    if (config.ocrEngine) formData.append('ocrEngine', config.ocrEngine)
    if (config.ocrLanguage) formData.append('ocrLanguage', config.ocrLanguage)
    if (config.extractKnowledgePoints !== undefined) {
      formData.append('extractKnowledgePoints', config.extractKnowledgePoints.toString())
    }
    if (config.autoClassifyDifficulty !== undefined) {
      formData.append('autoClassifyDifficulty', config.autoClassifyDifficulty.toString())
    }
    if (config.enableVectorization !== undefined) {
      formData.append('enableVectorization', config.enableVectorization.toString())
    }
    if (config.chunkSize) formData.append('chunkSize', config.chunkSize.toString())
    if (config.chunkOverlap) formData.append('chunkOverlap', config.chunkOverlap.toString())

    return request.post<KnowledgeDocument>('/knowledge/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // 获取文档处理状态
  getDocumentStatus: (documentId: number) => {
    return request.get<{
      status: string
      progress: number
      error?: string
    }>(`/knowledge/documents/${documentId}/status`)
  },

  // 批量上传文档
  batchUploadDocuments: (files: File[], knowledgeBaseId: number) => {
    const formData = new FormData()
    files.forEach(file => formData.append('files', file))
    formData.append('knowledgeBaseId', knowledgeBaseId.toString())

    return request.post<KnowledgeDocument[]>('/knowledge/documents/batch-upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // 获取文档列表 (使用正确的后端端点)
  getDocuments: (knowledgeBaseId: number) => {
    return request.get<KnowledgeDocument[]>('/knowledge/documents', {
      params: { knowledgeBaseId }
    })
  },

  // 获取文档详情
  getDocument: (id: number) => {
    return request.get<KnowledgeDocument>(`/knowledge/documents/${id}`)
  },

  // 删除文档
  deleteDocument: (id: number) => {
    return request.delete(`/knowledge/documents/${id}`)
  },

  // 重新处理文档（使用任务管理系统）
  reprocessDocument: (id: number) => {
    return request.post('/tasks', {
      type: 'KNOWLEDGE_PROCESSING',
      name: `文档重新处理 - ${id}`,
      description: `重新处理文档ID: ${id}`,
      configuration: { documentId: id, action: 'REPROCESS' },
      priority: 'HIGH',
      autoStart: true
    })
  },

  // 批量处理文档
  batchProcessDocuments: (documentIds: number[]) => {
    return request.post('/tasks', {
      type: 'KNOWLEDGE_PROCESSING',
      name: `批量文档处理 - ${documentIds.length}个文档`,
      description: `批量处理${documentIds.length}个文档`,
      configuration: { documentIds, action: 'BATCH_PROCESS' },
      priority: 'MEDIUM',
      autoStart: true
    })
  },

  // 搜索文档
  searchDocuments: (query: string, knowledgeBaseId?: number, limit = 10) => {
    return request.get<RetrievedDocument[]>('/knowledge/documents/search', {
      params: { query, knowledgeBaseId, limit }
    })
  },

  // 获取处理统计
  getProcessingStatistics: (knowledgeBaseId?: number) => {
    return request.get<DocumentProcessingStatistics>('/knowledge/documents/statistics', {
      params: { knowledgeBaseId }
    })
  },

  // 获取支持的文件格式
  getSupportedFormats: () => {
    return request.get<string[]>('/knowledge/documents/supported-formats')
  }
}

// AI出题API
export const aiQuestionGenerationApi = {
  // AI生成题目（直接调用，不使用任务管理系统）
  async generateQuestions(data: AIQuestionGenerationRequest): Promise<AIQuestionGenerationResponse> {
    const response = await request.post('/knowledge/ai-generation/generate', data)
    return response.data
  },

  // 查询生成状态（用于兼容）
  async getGenerationStatus(taskId: string): Promise<AIQuestionGenerationResponse> {
    const response = await request.get(`/knowledge/ai-generation/status/${taskId}`)
    return response.data
  },

  // 预览生成配置
  async previewGeneration(data: AIQuestionGenerationRequest): Promise<any> {
    const response = await request.post('/knowledge/ai-generation/preview', data)
    return response.data
  },

  // AI生成题目（使用任务管理系统）
  async generateQuestionsWithTask(data: AIQuestionGenerationRequest): Promise<{ taskId: string }> {
    const response = await request.post('/tasks', {
      type: 'AI_GENERATION',
      name: `AI题目生成 - ${data.questionTypes.join(', ')}`,
      description: `为知识库生成${data.questionTypes.length}种类型的题目`,
      configuration: data,
      priority: 'MEDIUM',
      autoStart: true
    })
    return { taskId: response.data.id }
  },

  // 查询生成状态（使用任务管理系统）
  async getTaskGenerationStatus(taskId: string): Promise<AIQuestionGenerationResponse> {
    const response = await request.get(`/tasks/${taskId}`)
    const task = response.data
    
    return {
      taskId: task.id,
      status: task.status as any,
      progress: task.progress,
      questions: task.result?.questions || [],
      error: task.error,
      totalTokens: task.result?.totalTokens,
      processingTimeMs: task.result?.processingTimeMs,
      generatedCount: task.result?.generatedCount
    }
  },

  // 取消生成（使用任务管理系统）
  async cancelGeneration(taskId: string): Promise<void> {
    await request.post(`/tasks/${taskId}/cancel`)
  },

  // 获取任务列表
  async getTasks(params?: any): Promise<any> {
    const response = await request.get('/tasks', { params })
    return response.data
  },

  // 获取任务详情
  async getTaskDetail(taskId: string): Promise<any> {
    const response = await request.get(`/tasks/${taskId}`)
    return response.data
  },

  // 获取任务日志
  async getTaskLogs(taskId: string): Promise<any> {
    const response = await request.get(`/tasks/${taskId}/logs`)
    return response.data
  },

  // 获取任务结果
  async getTaskResults(taskId: string, params?: any): Promise<any> {
    const response = await request.get(`/tasks/${taskId}/results`, { params })
    return response.data
  },

  // 暂停任务
  async pauseTask(taskId: string): Promise<any> {
    const response = await request.post(`/tasks/${taskId}/pause`)
    return response.data
  },

  // 恢复任务
  async resumeTask(taskId: string): Promise<any> {
    const response = await request.post(`/tasks/${taskId}/resume`)
    return response.data
  },

  // 重试任务
  async retryTask(taskId: string): Promise<any> {
    const response = await request.post(`/tasks/${taskId}/retry`)
    return response.data
  },

  // 删除任务
  async deleteTask(taskId: string): Promise<void> {
    await request.delete(`/tasks/${taskId}`)
  },

  // 导出任务结果
  async exportTaskResults(taskId: string, format: string = 'excel'): Promise<any> {
    const response = await request.get(`/tasks/${taskId}/results/export`, {
      params: { format },
      responseType: 'blob'
    })
    return response
  }
}

// 知识库任务管理API
export const knowledgeTaskApi = {
  // 获取知识库相关任务
  async getKnowledgeTasks(knowledgeBaseId?: number, params?: any): Promise<any> {
    const response = await request.get('/tasks', {
      params: {
        type: 'KNOWLEDGE_PROCESSING',
        ...params,
        ...(knowledgeBaseId && { 'config.knowledgeBaseId': knowledgeBaseId })
      }
    })
    return response.data
  },

  // 获取任务统计
  async getTaskStatistics(): Promise<any> {
    const response = await request.get('/tasks/stats')
    return response.data
  },

  // 暂停所有知识处理任务
  async pauseAllKnowledgeTasks(): Promise<void> {
    await request.post('/tasks/batch/pause', {
      filter: { type: 'KNOWLEDGE_PROCESSING', status: 'RUNNING' }
    })
  },

  // 清理已完成的知识处理任务
  async clearCompletedKnowledgeTasks(): Promise<void> {
    await request.delete('/tasks/batch/clear', {
      data: { filter: { type: 'KNOWLEDGE_PROCESSING', status: 'COMPLETED' } }
    })
  },

  // 监控文档处理进度
  async monitorDocumentProcessing(documentId: number): Promise<any> {
    const response = await request.get('/tasks', {
      params: {
        type: 'KNOWLEDGE_PROCESSING',
        'config.documentId': documentId,
        status: ['PENDING', 'RUNNING']
      }
    })
    return response.data
  }
}

// 知识点管理API
export const knowledgePointApi = {
  // 获取知识点列表
  async getKnowledgePoints(params?: {
    knowledgeBaseId?: number
    page?: number
    size?: number
    difficulty?: string
    tags?: string[]
    search?: string
  }): Promise<{ content: KnowledgePointResponse[], totalElements: number }> {
    const response = await request.get('/knowledge/points', { params })
    return response.data
  },

  // 获取知识点详情
  async getKnowledgePoint(id: number): Promise<KnowledgePointResponse> {
    const response = await request.get(`/knowledge/points/${id}`)
    return response.data
  },

  // 搜索知识点
  async searchKnowledgePoints(params: {
    query: string
    knowledgeBaseId?: number
    difficulty?: string
    limit?: number
  }): Promise<KnowledgePointResponse[]> {
    const response = await request.get('/knowledge/points/search', { params })
    return response.data
  },

  // 获取知识点统计
  async getKnowledgePointStatistics(knowledgeBaseId?: number): Promise<KnowledgePointStatistics> {
    const response = await request.get('/knowledge/points/statistics', {
      params: { knowledgeBaseId }
    })
    return response.data
  },

  // 获取知识点相关内容
  async getRelatedContent(id: number, params?: {
    limit?: number
    contentType?: string
  }): Promise<RetrievedDocument[]> {
    const response = await request.get(`/knowledge/points/${id}/related-content`, { params })
    return response.data
  },

  // 创建知识点
  async createKnowledgePoint(data: {
    title: string
    description?: string
    difficulty: 'EASY' | 'MEDIUM' | 'HARD'
    tags: string[]
    knowledgeBaseId: number
    summary?: string
  }): Promise<KnowledgePointResponse> {
    const response = await request.post('/knowledge/points', data)
    return response.data
  },

  // 更新知识点
  async updateKnowledgePoint(id: number, data: {
    title?: string
    description?: string
    difficulty?: 'EASY' | 'MEDIUM' | 'HARD'
    tags?: string[]
    summary?: string
  }): Promise<KnowledgePointResponse> {
    const response = await request.put(`/knowledge/points/${id}`, data)
    return response.data
  },

  // 删除知识点
  async deleteKnowledgePoint(id: number): Promise<void> {
    await request.delete(`/knowledge/points/${id}`)
  }
}

export default {
  knowledgeBaseApi,
  paperGenerationApi,
  documentApi,
  aiQuestionGenerationApi,
  knowledgeTaskApi,
  systemApi,
  knowledgePointApi
}
