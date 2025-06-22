import api from '@/utils/request'

export interface ClassroomResponse {
  id: number
  name: string
  description: string
  classCode: string
  createdBy: string
  createdAt: string
  updatedAt: string
  studentCount: number
  students: StudentResponse[]
  examCount: number
}

export interface StudentResponse {
  id: number
  username: string
  email: string
  joinedAt: string
}

export interface ClassroomCreateRequest {
  name: string
  description?: string
}

export interface JoinClassroomRequest {
  classCode: string
}

export const classroomApi = {
  // 创建班级
  async createClassroom(data: ClassroomCreateRequest): Promise<ClassroomResponse> {
    const response = await api.post<ClassroomResponse>('/classrooms', data)
    return response.data
  },

  // 获取班级详情
  async getClassroom(id: number): Promise<ClassroomResponse> {
    const response = await api.get<ClassroomResponse>(`/classrooms/${id}`)
    return response.data
  },

  // 获取班级列表
  async getAllClassrooms(): Promise<ClassroomResponse[]> {
    const response = await api.get<ClassroomResponse[]>('/classrooms')
    return response.data
  },

  // 学生加入班级
  async joinClassroom(data: JoinClassroomRequest): Promise<ClassroomResponse> {
    const response = await api.post<ClassroomResponse>('/classrooms/join', data)
    return response.data
  },

  // 学生离开班级
  async leaveClassroom(id: number): Promise<void> {
    await api.post(`/classrooms/${id}/leave`)
  },

  // 教师移除学生
  async removeStudent(classroomId: number, studentId: number): Promise<void> {
    await api.delete(`/classrooms/${classroomId}/students/${studentId}`)
  },

  // 更新班级
  async updateClassroom(id: number, data: ClassroomCreateRequest): Promise<ClassroomResponse> {
    const response = await api.put<ClassroomResponse>(`/classrooms/${id}`, data)
    return response.data
  },

  // 删除班级
  async deleteClassroom(id: number): Promise<void> {
    await api.delete(`/classrooms/${id}`)
  },

  // 根据班级代码预览班级信息
  async getClassroomByCode(classCode: string): Promise<ClassroomResponse> {
    const response = await api.get<ClassroomResponse>(`/classrooms/code/${classCode}`)
    return response.data
  }
}
