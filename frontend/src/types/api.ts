// 基础类型定义
export type QuestionType = 'ESSAY' | 'SHORT_ANSWER' | 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'CODING' | 'CASE_ANALYSIS'

export interface QuestionOption {
  id?: number
  content: string
  isCorrect: boolean
  optionOrder?: number
}

// 用户相关类型
export interface User {
  id: number
  username: string
  email: string
  roles: string[]
  createdAt?: string
  updatedAt?: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email: string
  roles?: string[]
}

export interface AuthResponse {
  token: string
  type: string
  id: number
  username: string
  email: string
  roles: string[]
}

// 考试相关类型
export interface Exam {
  id: number
  title: string
  description: string
  createdBy: string
  createdAt: string
  updatedAt: string
  status?: 'DRAFT' | 'PUBLISHED' | 'IN_PROGRESS' | 'ENDED' | 'EVALUATED'
  questions?: Question[]
}

export interface ExamResponse {
  id: number
  title: string
  description: string
  createdBy: string
  createdAt: string
  updatedAt: string
  status?: 'DRAFT' | 'PUBLISHED' | 'IN_PROGRESS' | 'ENDED' | 'EVALUATED'
  duration?: number
  startTime?: string
  endTime?: string
  totalScore?: number
  totalQuestions?: number
  totalAnswers?: number
  evaluatedAnswers?: number
  questionCount?: number
  settings?: string[]
}

export interface ExamUpdateRequest {
  title: string
  description: string
  status?: string
  duration?: number
  startTime?: string
  endTime?: string
  password?: string
  settings?: string[]
  allowedStudentIds?: number[]
  allowedClassIds?: number[]
}

export interface ExamCreateRequest {
  title: string
  description: string
}

// 学生答案相关类型
export interface StudentAnswerSubmitRequest {
  questionId: number
  answerText: string
  studentId?: string // 新增字段以支持现有组件
}

export interface StudentAnswerResponse {
  id: number
  answerText: string
  score?: number
  feedback?: string
  evaluated: boolean
  evaluatedAt?: string
  submittedAt: string
  questionId: number
  studentId: number
  examId: number
  questionContent?: string
  questionType?: string
  maxScore?: number
  // 新增字段以支持现有组件
  studentName?: string
  questionTitle?: string
  questionOptions?: QuestionOption[]
  referenceAnswer?: string
  student?: {
    id: number
    name: string
    userId?: number
    email?: string
    studentNumber?: string
  }
}

// 手动评估请求类型
export interface ManualEvaluationRequest {
  score: number
  feedback?: string
}

// 问题级别的评分标准类型（映射后端 RubricCriterion 实体）
export interface RubricCriterion {
  id?: number
  criterionText: string
  points: number
  orderIndex?: number
  questionId?: number
  // 新增字段以支持现有组件
  name?: string
  description?: string
  weight?: number
  maxScore?: number
  criterion?: string
  scoreLevels?: ScoreLevel[]
  editing?: boolean
  saving?: boolean
  originalData?: any
  // UI增强字段
  tempId?: number
  isNew?: boolean
  isModified?: boolean
}

// 问题相关类型
export interface Question {
  id: number
  title: string
  content: string
  questionType: 'ESSAY' | 'SHORT_ANSWER' | 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'CODING' | 'CASE_ANALYSIS'
  maxScore: number
  examId?: number
  examTitle?: string
  createdAt: string
  updatedAt: string
  rubricCriteria?: RubricCriterion[]
}

export interface QuestionResponse {
  id: number
  title: string
  content: string
  questionType: QuestionType
  maxScore: number
  examId?: number
  examTitle?: string
  createdAt: string
  updatedAt: string
  options?: QuestionOption[]
  referenceAnswer?: string
  // Statistics properties
  totalAnswers?: number
  evaluatedAnswers?: number
  averageScore?: number | null; // <--- MODIFIED
  rubricCriteria?: RubricCriterionResponse[]
}

export interface QuestionCreateRequest {
  title: string
  content: string
  questionType: 'ESSAY' | 'SHORT_ANSWER' | 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'CODING' | 'CASE_ANALYSIS'
  maxScore: number
  examId: number
  referenceAnswer?: string
  options?: QuestionOption[]
}

// 系统级评估标准类型
export interface Rubric {
  id: number
  name: string
  description?: string
  subject?: string
  totalScore: number
  criteria: SystemRubricCriterion[]
  isActive: boolean
  usageCount?: number
  createdAt: string
  updatedAt: string
  createdBy?: string
  // 新增字段以支持现有组件
  lastUsed?: string
}

// 系统级评估标准中的评分项
export interface SystemRubricCriterion {
  id: number
  name: string
  description?: string
  weight: number
  scoreLevels: ScoreLevel[]
}

// 评分等级
export interface ScoreLevel {
  id: number
  level: string
  description: string
  score: number
  // 新增字段以支持现有组件
  name?: string
}

// 学生信息类型
export interface StudentInfo {
  id: number;
  name: string;
  userId?: number;
  email?: string;
  studentNumber?: string;
}

// 学生答案相关类型
export interface StudentAnswer {
  id: number
  answerText: string
  score?: number
  feedback?: string
  evaluated: boolean
  evaluatedAt?: string
  submittedAt: string
  student?: StudentInfo
  questionId?: number
  questionTitle?: string
  questionContent?: string
  // 新增字段以支持现有组件
  questionType?: string
  maxScore?: number
}

// 新增的类型定义

// 用于在 StudentExamOverallResult 中表示单个问题的答案详情
export interface ExamAnswerDetail {
  // 来自 StudentAnswer 实体
  studentAnswerId: number; // StudentAnswer 记录的 ID
  answerText: string | null; // 学生的答案文本
  obtainedScore?: number | null; // 此答案获得的分数
  feedback?: string | null; // 对此答案的反馈
  isEvaluated: boolean; // 标记答案是否已评估
  evaluatedAt?: string | null; // 评估时间
  submittedAt: string; // 此特定答案提交或最终确定的时间

  // 来自 Question 实体
  questionId: number; // 问题的 ID
  questionTitle: string; // 问题标题
  questionContent: string; //问题的 HTML 内容
  questionType: QuestionType; // 现有 QuestionType
  maxScore: number; // 此问题的最高分数
  referenceAnswer?: string | null; // 参考答案
  options?: QuestionOption[]; // 现有 QuestionOption，用于选择题
}

// 学生个人整体考试结果的类型
export interface StudentExamOverallResult {
  // 考试尝试/结果元数据
  examId: number;
  studentId: number;
  studentName?: string;
  examTitle?: string; // Made optional to align with other declarations
  examDescription?: string; // 新增字段以支持现有组件
  examTotalScore: number;
  obtainedScore: number;
  startTime?: string | null; // 开始时间
  submitTime?: string | null; // 提交时间
  durationInSeconds?: number | null; // 持续时间（秒）
  status: string; // 例如：'EVALUATED', 'SUBMITTED', 'IN_PROGRESS' (整体尝试状态)

  // 详细答案列表
  answers: ExamAnswerDetail[];
}

// 考试统计数据的类型 (供教师/管理员使用)
export interface AggregatedExamStatistics { // <--- RENAMED
  examId: number;
  examTitle?: string; // Already optional, consistent
  averageScore: number | null; // Consistent with QuestionResponse.averageScore
  highestScore: number | null;
  lowestScore: number | null;
  submissionCount: number; // 提交人数
  medianScore?: number | null; // 中位数分数
  standardDeviation?: number | null; // 标准差
  // 例如：[{ range: "0-10%", count: 5, percentage: 10.0 }, ...]
  scoreDistribution?: Array<{ range: string; count: number; percentage?: number }>;
  passRate?: number | null; // 通过率 (如果定义了及格分数)
  questionAverageScores?: Array<{
    questionId: number;
    questionTitle?: string;
    averageScore: number | null; // 平均得分
    correctRate?: number | null; // 正确率 (针对选择题/判断题)
  }>;
  totalParticipants?: number; // 可能参加考试的总学生数
  completionRate?: number; // 完成率 = submissionCount / totalParticipants
}

// 考试中单个学生结果摘要的类型 (供管理员/教师列表使用)
export interface ExamStudentResultSummary {
  studentId: number;
  studentName: string;
  userId?: number;
  obtainedScore: number | null; // 学生获得的分数
  totalScore: number; // 考试的总分
  submitTime?: string | null; // 提交时间
  status: string; // 例如 'EVALUATED', 'SUBMITTED'
  attemptId?: number; // 考试尝试 ID 或类似的唯一标识符
}

// 管理员/教师获取某场考试所有学生结果时的响应类型
export interface ExamResultsOverview {
  examId: number;
  examTitle?: string; // Made optional to align
  examTotalScore: number; // 考试总分
  results: ExamStudentResultSummary[]; // 学生结果摘要列表
  statistics?: AggregatedExamStatistics; // <--- MODIFIED: 可选：此处可嵌入基本统计信息
}

// 提交答案相关 (如果需要单独定义提交结构)
export interface SubmitAnswerPayload {
  questionId: number;
  answerText: string | null;
}

// 提交考试负载
export interface SubmitExamPayload {
  examId: number;
  answers: SubmitAnswerPayload[];
}

// 用于教师评估单个答案的请求体
export interface EvaluateAnswerRequest {
  score: number;
  feedback?: string;
}

// AI评分标准相关类型
export interface RubricGenerateRequest {
  questionId: number
}

export interface RubricCriterionResponse {
  id: number
  criterionText: string
  points: number
  // UI扩展字段
  editing?: boolean
  saving?: boolean
  originalData?: any
  // 兼容性字段（映射到主字段）
  criterion?: string
  maxScore?: number
  description?: string
  weight?: number
}

export interface RubricGenerateResponse {
  criteria: RubricCriterionResponse[]
  totalScore: number
  message: string
}

export interface RubricApplyResponse {
  appliedCriteria: RubricCriterionResponse[]
  message: string
}

// Individual AI suggestion item (matches backend RubricSuggestionResponse)
export interface RubricSuggestionItem {
  criterionText: string
  points: number
  // UI增强字段
  id?: string | number
  description?: string
  generatedAt?: string
}

// For backward compatibility with existing code expecting wrapper structure
export interface RubricSuggestionResponse {
  suggestions: RubricCriterionResponse[]
  totalScore: number
  message: string
}

// 考试结果相关类型
export interface ExamResult {
  examId: number
  studentId: number
  studentName: string
  examTitle?: string // 新增字段以支持现有组件
  examDescription?: string // 新增字段以支持现有组件
  totalScore: number
  answeredQuestions: number
  status: string
  startTime: string
  submitTime: string
  duration: number // 用时（秒）
  answers: StudentAnswer[]
}

export interface ExamStatistics { // This is the original ExamStatistics
  examId: number
  examTitle?: string // <--- MODIFIED
  examDescription: string
  totalQuestions: number
  totalAnswers: number
  evaluatedAnswers: number
  unevaluatedAnswers: number
  evaluationProgress: number
  averageScore?: number | null // <--- MODIFIED
  maxScore?: number | null // <--- MODIFIED
  minScore?: number | null // <--- MODIFIED
  totalPossibleScore: number
  lastEvaluationTime: string
  examCreatedAt: string
  totalStudents: number
  studentsSubmitted: number
  studentsEvaluated: number
}

// 评估统计类型
export interface EvaluationStatistics {
  totalAnswers: number
  evaluatedAnswers: number
  unevaluatedAnswers: number
  evaluationProgress: number
  averageScore?: number | null
}

// 批量评估结果类型
export interface BatchEvaluationResult {
  taskId: string
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | 'COMPLETED_WITH_ERRORS' | 'IN_PROGRESS'
  totalAnswers: number
  successfulEvaluations: number
  failedEvaluations: number
  createdAt: string
  completedAt?: string
  message?: string
  // 新增字段以支持现有组件
  startTime?: string
  endTime?: string
  successRate?: number
  durationInMillis?: number
  errors?: string[]
}

// AI生成任务相关类型
export interface AIGenerationTaskResponse {
  taskId: string
  message: string
  questionId: number
  status?: string
}

export interface AIGenerationStatusResponse {
  taskId: string
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
  message: string
  progress: number // 0-100
  createdAt: string
  updatedAt: string
  questionId: number
  
  // AI统计信息
  totalTokens?: number
  promptTokens?: number
  completionTokens?: number
  processingTimeMs?: number
  
  // 结果数据
  suggestions?: RubricSuggestionItem[]
  error?: string
  
  // 对比模式支持
  existingCriteria?: RubricCriterion[]
  comparisonMode?: boolean
  generationMode?: string
}

// AI 配置相关类型
export type AIProvider = 'OPENAI' | 'DEEPSEEK' | 'CLAUDE' | 'ALIBABA_TONGYI' | 'BAIDU_ERNIE' | 'TENCENT_HUNYUAN' | 'CUSTOM'

export interface UserAIConfig {
  id: number
  userId: number
  provider: AIProvider
  name: string
  apiKey: string
  apiUrl: string
  model: string
  maxTokens: number
  temperature: number
  isActive: boolean
  isDefault: boolean
  description?: string
  createdAt: string
  updatedAt: string
}

export interface UserAIConfigRequest {
  name: string
  provider: AIProvider
  apiKey: string
  apiUrl: string
  model: string
  maxTokens: number
  temperature: number
  description?: string
}

export interface UserAIConfigResponse {
  id: number
  provider: AIProvider
  providerDisplayName: string
  maskedApiKey: string
  apiEndpoint?: string
  modelName?: string
  maxTokens: number
  temperature: number
  isActive: boolean
  isDefault: boolean
  createdAt: string
  updatedAt: string
  name?: string
  description?: string
  usageStats?: {
    totalRequests: number
    totalCost: number
  }
}

export interface AIUsageStats {
  totalRequests: number
  totalTokens: number
  totalCost: number
  averageResponseTime: number
  successRate: number
  lastUsed?: string
}

export interface AIConfigTestRequest {
  prompt: string
  maxTokens?: number
  expectedResponse?: string
}

export interface AIConfigTestResponse {
  success: boolean
  response?: string
  error?: string
  latencyMs: number
  tokensUsed: number
  cost: number
  inputTokens?: number
  outputTokens?: number
}

export interface AIProviderInfo {
  provider: AIProvider
  name: string
  description: string
  defaultModels: string[]
  supportedFeatures: string[]
  documentationUrl?: string
  defaultEndpoint?: string
  enabled: boolean
  inputTokenPrice: number
  outputTokenPrice: number
}

// REMOVE DUPLICATE StudentInfo if it was at the end. 
// The correct one is already defined earlier in the file.
// export interface StudentInfo {
// id: number;
// name: string;
// userId?: number;
// email?: string;
// studentNumber?: string;
// }

// 评估任务类型
export interface EvaluationTask {
  id: string
  taskId?: string
  examId?: number
  examTitle?: string
  questionId?: number
  questionTitle?: string
  taskType: 'BATCH_EVALUATION' | 'RUBRIC_GENERATION' | 'FULL_EVALUATION' | 'QUICK_EVALUATION'
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | 'COMPLETED_WITH_ERRORS'
  progress: number // 0-100
  totalAnswers?: number
  successfulEvaluations?: number
  failedEvaluations?: number
  processedCount?: number
  totalCount?: number
  createdAt: string
  startedAt?: string
  completedAt?: string
  description?: string
  errorMessage?: string
  aiConfigId?: number
  aiConfigName?: string
  rubricId?: number
  rubricName?: string
  batchSize?: number
  concurrency?: number
  priority?: 'LOW' | 'NORMAL' | 'HIGH'
  logs?: TaskLog[]
}

// 任务日志类型
export interface TaskLog {
  id: number
  timestamp: string
  level: 'INFO' | 'WARN' | 'ERROR' | 'DEBUG'
  message: string
}

// 任务统计类型
export interface TaskStatistics {
  total: number
  pending: number
  running: number
  completed: number
  failed?: number
}

// ======================== 学科和年级管理类型 ========================

// 学科请求类型
export interface SubjectRequest {
  name: string
  description?: string
  category?: string
  sortOrder?: number
}

// 学科响应类型
export interface SubjectResponse {
  id: number
  name: string
  description?: string
  category?: string
  isActive: boolean
  sortOrder?: number
  createdBy?: string
  createdAt: string
  updatedAt: string
  usageCount: number
}

// 年级请求类型
export interface GradeLevelRequest {
  name: string
  description?: string
  category?: string
  sortOrder?: number
}

// 年级响应类型
export interface GradeLevelResponse {
  id: number
  name: string
  description?: string
  category?: string
  isActive: boolean
  sortOrder?: number
  createdBy?: string
  createdAt: string
  updatedAt: string
  usageCount: number
}
