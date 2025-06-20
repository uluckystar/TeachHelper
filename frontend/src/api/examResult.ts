import request from '@/utils/request'

// 考试结果相关API
export const examResultApi = {
  // 学生查看自己的考试结果
  getStudentExamResult(examId: number) {
    return request.get(`/exam-results/student/exam/${examId}`)
  },

  // 教师查看考试的所有学生结果
  getTeacherExamResults(examId: number) {
    return request.get(`/exam-results/teacher/exam/${examId}`)
  },

  // 教师查看指定学生的考试结果
  getStudentExamResultForTeacher(examId: number, studentId: number) {
    return request.get(`/exam-results/teacher/exam/${examId}/student/${studentId}`)
  },

  // 管理员查看考试所有学生结果
  getAllStudentResults(examId: number) {
    return request.get(`/exam-results/admin/exam/${examId}/students`)
  },

  // 获取考试结果统计（所有角色）
  getExamStatistics(examId: number) {
    return request.get(`/exam-results/exam/${examId}/statistics`)
  }
}
