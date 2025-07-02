import api from '@/utils/request'
import { longTimeoutApi } from '@/utils/request'
import type {
  StudentAnswer,
  StudentAnswerSubmitRequest,
  StudentAnswerResponse,
  PageResponse,
  StudentExamPaperResponse,
  ImportResult
} from '@/types/api'

export const answerApi = {
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
  async updateAnswer(id: number, answerData: Partial<StudentAnswer>): Promise<StudentAnswerResponse> {
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

  // 根据考试ID获取答案（支持分页和筛选）
  async getAnswersByExamWithFilters(
    examId: number,
    page: number = 1,
    size: number = 20,
    questionId?: number,
    evaluated?: boolean,
    keyword?: string
  ): Promise<PageResponse<StudentAnswerResponse>> {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size)
    })
    
    if (questionId !== undefined) {
      params.append('questionId', String(questionId))
    }
    if (evaluated !== undefined) {
      params.append('evaluated', String(evaluated))
    }
    if (keyword && keyword.trim()) {
      params.append('keyword', keyword.trim())
    }
    
    const response = await api.get<PageResponse<StudentAnswerResponse>>(`/exams/${examId}/answers?${params.toString()}`)
    return response.data
  },

  // 按学生分组获取考试答案 - 学生试卷视图
  async getExamPapers(
    examId: number,
    page: number = 1,
    size: number = 20,
    studentKeyword?: string
  ): Promise<PageResponse<StudentExamPaperResponse>> {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size)
    })
    
    if (studentKeyword && studentKeyword.trim()) {
      params.append('studentKeyword', studentKeyword.trim())
    }
    
    const response = await api.get<PageResponse<StudentExamPaperResponse>>(`/exams/${examId}/papers?${params.toString()}`)
    return response.data
  },

  // 获取单个学生的试卷详情
  async getStudentExamPaper(examId: number, studentId: number): Promise<StudentExamPaperResponse> {
    const response = await api.get<StudentExamPaperResponse>(`/exams/${examId}/papers/${studentId}`)
    return response.data
  },

  // 导出学生试卷
  async exportStudentPaper(examId: number, studentId: number, format: string = 'pdf'): Promise<Blob> {
    const response = await api.get(`/exams/${examId}/papers/${studentId}/export`, {
      params: { format },
      responseType: 'blob'
    })
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
  async exportAnswers(examId?: number, questionId?: number, evaluated?: boolean): Promise<void> {
    const params: Record<string, any> = {}
    if (examId !== undefined) params.examId = examId
    if (questionId !== undefined) params.questionId = questionId
    if (evaluated !== undefined) params.evaluated = evaluated
    
    const response = await api.get('/student-answers/export', { params, responseType: 'blob' });
    const blob = new Blob([response.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `answers_export_${new Date().getTime()}.xlsx`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
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

  // 手动评估学生答案
  async manuallyEvaluateAnswer(answerId: number, score: number, feedback: string): Promise<StudentAnswerResponse> {
    const response = await api.post<StudentAnswerResponse>(`/evaluations/manual/${answerId}`, {
      score,
      feedback
    })
    return response.data
  },

  // 获取学习通可用科目列表
  async getLearningSubjects(): Promise<string[]> {
    const response = await api.get<string[]>('/student-answers/learning/subjects')
    return response.data
  },

  // 获取学习通科目下的班级列表
  async getLearningSubjectClasses(subject: string): Promise<string[]> {
    const response = await api.get<string[]>(`/student-answers/learning/subjects/${encodeURIComponent(subject)}/classes`)
    return response.data
  },
  
  // 导入学习通答案文件到指定考试（异步）
  async importLearningAnswerFile(file: File, examId?: number): Promise<{ success: boolean; message: string; taskId: string; fileName: string }> {
    const formData = new FormData()
    formData.append('file', file)
    if (examId !== undefined) {
      formData.append('examId', String(examId))
    }
    const response = await api.post('/student-answers/import/learning-file', formData)
    return response.data
  },

  // 删除某个学生在某场考试中的所有答案
  async deleteStudentExamAnswers(studentId: number, examId: number): Promise<void> {
    await api.delete(`/student-answers/student/${studentId}/exam/${examId}`)
  },

  // 批量删除考试答案
  async batchDeleteExamAnswers(examId: number, request: {
    deleteType: 'answers' | 'students',
    answerIds?: number[],
    studentIds?: number[]
  }): Promise<{ message: string; deletedCount: number; examId: number }> {
    const response = await api.delete<{ message: string; deletedCount: number; examId: number }>(`/exams/${examId}/answers/batch`, {
      data: request
    })
    return response.data
  },

  // 异步导入学习通答案
  async importLearningAnswersAsync(
    subject: string, 
    classFolders: string[], 
    examId?: number
  ): Promise<{
    data: {
      success: boolean;
      message: string;
      taskId: string;
      fileName?: string;
    }
  }> {
    // 使用 URLSearchParams 发送参数，与后端的 @RequestParam 匹配
    const params = new URLSearchParams();
    params.append('subject', subject);
    params.append('classFolders', classFolders.join(','));
    if (examId !== undefined) {
      params.append('examId', String(examId));
    }
    
    const response = await api.post('/student-answers/import/learning-async', params, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      }
    });
    return response.data;
  },

  // 基于模板导入学习通答案（异步）
  async importLearningAnswersWithTemplate(
    subject: string,
    classFolders: string[],
    templateId: number,
    examId?: number
  ): Promise<{ success: boolean; message: string; result: any; }> {
    const response = await longTimeoutApi.post('/student-answers/import-learning-answers-with-template', {
      subject,
      classFolders,
      templateId,
      examId
    })
    return response.data
  },

  // 嵌套压缩包答案导入
  async importNestedZipAnswers(answerPath: string, questionId: number): Promise<ImportResult> {
    const response = await longTimeoutApi.post<ImportResult>('/student-answers/import-nested-zip', null, {
      params: {
        answerPath,
        questionId
      }
    })
    return response.data
  },

  // 获取嵌套压缩包答案的科目列表
  async getNestedZipSubjects(): Promise<string[]> {
    const response = await api.get<string[]>('/student-answers/nested-zip-subjects')
    return response.data
  },

  // 获取指定科目下的作业/实验列表
  async getNestedZipAssignments(subject: string): Promise<string[]> {
    const response = await api.get<string[]>('/student-answers/nested-zip-assignments', {
      params: { subject }
    })
    return response.data
  },

  // 基于科目和作业的嵌套压缩包导入
  async importNestedZipAnswersBySubject(subject: string, assignment: string, questionId: number): Promise<ImportResult> {
    const response = await longTimeoutApi.post<ImportResult>('/student-answers/import-nested-zip-by-subject', null, {
      params: {
        subject,
        assignment,
        questionId
      }
    })
    return response.data
  },

  // 大作业答案导入
  async importMajorAssignmentAnswers(subject: string, assignment: string, questionId: number): Promise<ImportResult> {
    const response = await longTimeoutApi.post<ImportResult>('/student-answers/import-major-assignment', null, {
      params: { subject, assignment, questionId }
    })
    return response.data
  },

  // 获取大作业导入的作业列表
  async getMajorAssignmentAssignments(subject: string): Promise<string[]> {
    const response = await api.get<string[]>('/student-answers/major-assignment-assignments', {
      params: { subject }
    })
    return response.data
  },

  // 文件夹批量上传答案
  async uploadFolderAnswers(formData: FormData): Promise<ImportResult> {
    const response = await api.post<ImportResult>('/student-answers/import-folder-upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return response.data
  },

  // 测试文件名解析
  async testFileNameParse(fileName: string): Promise<{
    success: boolean
    fileName: string
    studentName?: string
    studentNumber?: string
    parseMethod?: string
    errorMessage?: string
  }> {
    const response = await api.post('/student-answers/test-filename-parse', { fileName })
    return response.data
  }
}

// 为兼容性导出别名
export const studentAnswerApi = answerApi
