import api from '@/utils/request'
import type { ExamResponse, ExamCreateRequest, ExamStatistics, ExamPublishResponse } from '@/types/api'
import type { ClassroomResponse } from './classroom'

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
  async publishExam(examId: number): Promise<ExamPublishResponse> {
    try {
      const response = await api.post<ExamPublishResponse>(`/exams/${examId}/publish`)
      return response.data
    } catch (error: any) {
      // 处理422状态码（验证失败）的友好错误
      if (error.response?.status === 422) {
        const errorData = error.response.data
        // 抛出包含详细信息的错误，让调用方处理
        const validationError = new Error(errorData.message || '考试必须包含至少一个题目才能发布')
        ;(validationError as any).code = 422
        ;(validationError as any).data = errorData
        throw validationError
      }
      // 其他错误正常抛出
      throw error
    }
  },

  // 结束考试
  async endExam(examId: number): Promise<ExamResponse> {
    const response = await api.post<ExamResponse>(`/exams/${examId}/end`)
    return response.data
  },

  // 撤销发布考试
  async unpublishExam(examId: number): Promise<ExamResponse> {
    const response = await api.post<ExamResponse>(`/exams/${examId}/unpublish`)
    return response.data
  },

  // 调整考试班级
  async updateExamClassrooms(examId: number, classroomIds: number[]): Promise<ExamResponse> {
    const response = await api.put<ExamResponse>(`/exams/${examId}/classrooms`, classroomIds)
    return response.data
  },

  // 获取考试班级信息
  async getExamClassrooms(examId: number): Promise<ClassroomResponse[]> {
    const response = await api.get<ClassroomResponse[]>(`/exams/${examId}/classrooms`)
    return response.data
  },

  // 获取考试列表（简化版本）
  async getExams(params: any): Promise<any> {
    const response = await api.get('/exams', { params })
    return response.data
  },

  // 获取学生可参与的考试列表
  async getStudentExams(): Promise<ExamResponse[]> {
    const response = await api.get<ExamResponse[]>('/exams/student/my-exams')
    return response.data
  },

  // 获取学生的考试详情（包含答题状态）
  async getStudentExamDetail(examId: number): Promise<ExamResponse> {
    const response = await api.get<ExamResponse>(`/exams/${examId}/student`)
    return response.data
  },

  // 学生专用：获取可参加的考试（仅已发布状态）
  async getAvailableExams(): Promise<ExamResponse[]> {
    const response = await api.get<ExamResponse[]>('/exams/available')
    return response.data
  },

  exportExamResults: async (id: number): Promise<BlobPart> => {
    const response = await api.get<BlobPart>(`/exams/${id}/export-results`, { responseType: 'blob' })
    return response.data
  },

  exportStudentPaper: async (examId: number, studentId: number, format: string): Promise<BlobPart> => {
    const response = await api.get<BlobPart>(`/exams/${examId}/papers/${studentId}/export?format=${format}`, { responseType: 'blob' })
    return response.data
  },

  // 一键导出所有试卷
  exportAllStudentPapers: async (examId: number, format: string = 'pdf'): Promise<BlobPart> => {
    const response = await api.get<BlobPart>(`/exams/${examId}/papers/export-all?format=${format}`, { responseType: 'blob' })
    return response.data
  },

  async getExamQuestionsWithStats(examId: number) {
    const response = await api.get(`/exams/${examId}/questions-with-stats`);
    return response.data;
  }
}
