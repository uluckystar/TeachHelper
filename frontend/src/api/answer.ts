import api from '@/utils/request'
import type {
  StudentAnswer,
  StudentAnswerSubmitRequest,
  StudentAnswerResponse
} from '@/types/api'

export const studentAnswerApi = {
  // 提交单个答案
  async submitAnswer(answerData: StudentAnswerSubmitRequest): Promise<StudentAnswerResponse> {
    const response = await api.post<StudentAnswerResponse>('/student-answers', answerData)
    return response.data
  },

  // 获取答案详情
  async getAnswer(id: number): Promise<StudentAnswerResponse> {
    const response = await api.get<StudentAnswerResponse>(`/student-answers/${id}`)
    return response.data
  },

  // 更新答案
  async updateAnswer(id: number, answerData: StudentAnswerSubmitRequest): Promise<StudentAnswerResponse> {
    const response = await api.put<StudentAnswerResponse>(`/student-answers/${id}`, answerData)
    return response.data
  },

  // 删除答案
  async deleteAnswer(id: number): Promise<void> {
    await api.delete(`/student-answers/${id}`)
  },

  // 获取所有答案（分页）
  async getAllAnswers(page = 0, size = 10): Promise<StudentAnswerResponse[]> {
    const response = await api.get<StudentAnswerResponse[]>('/student-answers', {
      params: { page, size }
    })
    return response.data
  },

  // 根据题目ID获取所有答案
  async getAnswersByQuestion(questionId: number): Promise<StudentAnswerResponse[]> {
    const response = await api.get<StudentAnswerResponse[]>(`/student-answers/question/${questionId}`)
    return response.data
  },

  // 根据学生ID获取所有答案
  async getAnswersByStudent(studentId: number): Promise<StudentAnswerResponse[]> {
    const response = await api.get<StudentAnswerResponse[]>(`/student-answers/student/${studentId}`)
    return response.data
  },

  // 根据考试ID获取所有答案
  async getAnswersByExam(examId: number): Promise<StudentAnswerResponse[]> {
    const response = await api.get<StudentAnswerResponse[]>(`/student-answers/exam/${examId}`)
    return response.data
  },

  // 获取所有未评估的答案
  async getUnevaluatedAnswers(): Promise<StudentAnswerResponse[]> {
    const response = await api.get<StudentAnswerResponse[]>('/student-answers/unevaluated')
    return response.data
  },

  // 获取指定题目的未评估答案
  async getUnevaluatedAnswersByQuestion(questionId: number): Promise<StudentAnswerResponse[]> {
    const response = await api.get<StudentAnswerResponse[]>(`/student-answers/question/${questionId}/unevaluated`)
    return response.data
  },

  // 批量提交答案
  async batchSubmitAnswers(requests: StudentAnswerSubmitRequest[]): Promise<StudentAnswerResponse[]> {
    const response = await api.post<StudentAnswerResponse[]>('/student-answers/batch', requests)
    return response.data
  },

  // 导入答案文件
  async importAnswers(file: File): Promise<string> {
    const formData = new FormData()
    formData.append('file', file)
    const response = await api.post<string>('/student-answers/import', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    return response.data
  },

  // 导出答案
  async exportAnswers(examId?: number, questionId?: number, evaluated?: boolean): Promise<string> {
    const params: Record<string, any> = {}
    if (examId !== undefined) params.examId = examId
    if (questionId !== undefined) params.questionId = questionId
    if (evaluated !== undefined) params.evaluated = evaluated
    
    const response = await api.get<string>('/student-answers/export', { params })
    return response.data
  },

  // 获取我的考试答案（学生视角）
  async getMyAnswersByExam(examId: number): Promise<StudentAnswerResponse[]> {
    const response = await api.get<StudentAnswerResponse[]>(`/student-answers/my-exam/${examId}`)
    return response.data
  },

  // 导入答案到考试
  async importAnswersToExam(examId: number, file: File): Promise<string> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('examId', examId.toString())
    const response = await api.post<string>(`/student-answers/exam/${examId}/import`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    return response.data
  },

  // 下载考试报告
  async downloadExamReport(examId: number): Promise<Blob> {
    const response = await api.get(`/student-answers/exam/${examId}/report`, {
      responseType: 'blob'
    })
    return response.data
  },

  // 检查学生是否已提交考试
  async hasStudentSubmittedExam(examId: number, studentId: number): Promise<boolean> {
    const response = await api.get<boolean>(`/student-answers/exam/${examId}/student/${studentId}/submitted`)
    return response.data
  },

  // 检查当前学生是否已提交考试
  async hasCurrentStudentSubmittedExam(examId: number): Promise<boolean> {
    const response = await api.get<boolean>(`/student-answers/exam/${examId}/my-submission-status`)
    return response.data
  },

  // 获取当前学生的提交详情
  async getSubmissionDetail(examId: number): Promise<{
    submittedAt: string
    answeredQuestions: number
    totalQuestions: number
    score: number | null
  }> {
    const response = await api.get(`/student-answers/exam/${examId}/my-submission-detail`)
    return response.data
  },

  // 正式提交整个考试
  async submitExam(examId: number): Promise<void> {
    await api.post(`/student-answers/exam/${examId}/submit`)
  },
}

// 导出answerApi作为默认导出和命名导出
export { studentAnswerApi as answerApi }
export default studentAnswerApi
