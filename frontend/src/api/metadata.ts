import api from '@/utils/request'

// 简化的Subject API - 用于QuestionLibraryView
export const subjectApi = {
  // 获取所有学科名称
  getAllSubjectNames: async (): Promise<string[]> => {
    try {
      const response = await api.get('/question-banks/metadata/subjects')
      return response.data || []
    } catch (error) {
      console.error('Failed to load subjects:', error)
      return []
    }
  },

  // 以下方法为管理页面提供兼容性支持
  getAllSubjects: async () => {
    try {
      const response = await api.get('/metadata/subjects')
      return response
    } catch (error) {
      console.error('Failed to load subjects:', error)
      return { data: [] }
    }
  },

  getAllSubjectsList: async () => {
    try {
      const response = await api.get('/metadata/subjects')
      return response
    } catch (error) {
      console.error('Failed to load subjects:', error)
      return { data: [] }
    }
  },

  getAllCategories: async () => {
    try {
      // 暂时返回空数组，因为后端可能没有这个接口
      return { data: [] }
    } catch (error) {
      console.error('Failed to load categories:', error)
      return { data: [] }
    }
  },

  getSubject: (id: number) => api.get(`/metadata/subjects/${id}`),
  createSubject: (data: any) => api.post('/metadata/subjects', data),
  teacherCreateSubject: (data: any) => api.post('/metadata/subjects/teacher-create', data),
  updateSubject: (id: number, data: any) => api.put(`/metadata/subjects/${id}`, data),
  deleteSubject: (id: number) => api.delete(`/metadata/subjects/${id}`)
}

// 简化的Grade Level API - 用于QuestionLibraryView  
export const gradeLevelApi = {
  // 获取所有年级名称
  getAllGradeLevelNames: async (): Promise<string[]> => {
    try {
      const response = await api.get('/question-banks/metadata/grade-levels')
      return response.data || []
    } catch (error) {
      console.error('Failed to load grade levels:', error)
      return []
    }
  },

  // 以下方法为管理页面提供兼容性支持
  getAllGradeLevels: async () => {
    try {
      const response = await api.get('/metadata/grade-levels')
      return response
    } catch (error) {
      console.error('Failed to load grade levels:', error)
      return { data: [] }
    }
  },

  getAllGradeLevelsList: async () => {
    try {
      const response = await api.get('/metadata/grade-levels')
      return response
    } catch (error) {
      console.error('Failed to load grade levels:', error)
      return { data: [] }
    }
  },

  getAllGradeLevelCategories: async () => {
    try {
      // 暂时返回空数组，因为后端可能没有这个接口
      return { data: [] }
    } catch (error) {
      console.error('Failed to load categories:', error)
      return { data: [] }
    }
  },

  getGradeLevel: (id: number) => api.get(`/metadata/grade-levels/${id}`),
  createGradeLevel: (data: any) => api.post('/metadata/grade-levels', data),
  updateGradeLevel: (id: number, data: any) => api.put(`/metadata/grade-levels/${id}`, data),
  deleteGradeLevel: (id: number) => api.delete(`/metadata/grade-levels/${id}`),
  
  // 新增：根据年级获取推荐学科
  getRecommendedSubjectsByGrade: (gradeLevel: string) => 
    api.get(`/metadata/grade-levels/${encodeURIComponent(gradeLevel)}/recommended-subjects`),
  
  // 新增：获取年级类别
  getGradeCategory: (gradeLevel: string) => 
    api.get(`/metadata/grade-levels/${encodeURIComponent(gradeLevel)}/category`)
}

// 学科年级关联 API
export const subjectGradeMappingApi = {
  // 获取所有关联关系
  getAllMappings: () => api.get('/metadata/subject-grade-mappings'),
  
  // 根据年级ID获取推荐学科
  getSubjectsByGradeId: (gradeId: number) => 
    api.get(`/metadata/subject-grade-mappings/grade/${gradeId}/subjects`),
    
  // 根据学科ID获取适用年级
  getGradesBySubjectId: (subjectId: number) => 
    api.get(`/metadata/subject-grade-mappings/subject/${subjectId}/grades`),
    
  // 创建关联关系
  createMapping: (data: {
    subjectId: number,
    gradeLevelId: number,
    priority?: number,
    isRecommended?: boolean
  }) => api.post('/metadata/subject-grade-mappings', data),
  
  // 批量创建关联关系
  createBatchMappings: (data: {
    subjectId: number,
    gradeLevelIds: number[],
    priority?: number,
    isRecommended?: boolean
  }) => api.post('/metadata/subject-grade-mappings/batch', data),
  
  // 更新关联关系
  updateMapping: (id: number, data: {
    priority?: number,
    isRecommended?: boolean
  }) => api.put(`/metadata/subject-grade-mappings/${id}`, data),
  
  // 删除关联关系
  deleteMapping: (id: number) => api.delete(`/metadata/subject-grade-mappings/${id}`),
  
  // 删除学科的所有关联
  deleteMappingsBySubject: (subjectId: number) => 
    api.delete(`/metadata/subject-grade-mappings/subject/${subjectId}`),
    
  // 删除年级的所有关联
  deleteMappingsByGrade: (gradeId: number) => 
    api.delete(`/metadata/subject-grade-mappings/grade/${gradeId}`)
}

// 数据迁移 API
export const migrationApi = {
  // 执行学科年级数据迁移
  migrateSubjectGradeData: () => api.post('/migration/subject-grade/migrate'),
  
  // 检查迁移状态
  checkMigrationStatus: () => api.get('/migration/subject-grade/status'),
  
  // 清空关联数据
  clearMappingData: () => api.delete('/migration/subject-grade/clear')
}

// 向后兼容的导出
export const getSubjects = subjectApi.getAllSubjects
export const getAllSubjects = subjectApi.getAllSubjects
export const getAllSubjectsList = subjectApi.getAllSubjectsList
export const getAllCategories = subjectApi.getAllCategories
export const getSubject = subjectApi.getSubject
export const createSubject = subjectApi.createSubject
export const updateSubject = subjectApi.updateSubject
export const deleteSubject = subjectApi.deleteSubject

export const getGradeLevels = gradeLevelApi.getAllGradeLevels
export const getAllGradeLevels = gradeLevelApi.getAllGradeLevels
export const getAllGradeLevelsList = gradeLevelApi.getAllGradeLevelsList
export const getAllGradeLevelCategories = gradeLevelApi.getAllGradeLevelCategories
export const getGradeLevel = gradeLevelApi.getGradeLevel
export const createGradeLevel = gradeLevelApi.createGradeLevel
export const updateGradeLevel = gradeLevelApi.updateGradeLevel
export const deleteGradeLevel = gradeLevelApi.deleteGradeLevel

export const getAllMappings = subjectGradeMappingApi.getAllMappings
export const getSubjectsByGradeId = subjectGradeMappingApi.getSubjectsByGradeId
export const getGradesBySubjectId = subjectGradeMappingApi.getGradesBySubjectId
export const createMapping = subjectGradeMappingApi.createMapping
export const createBatchMappings = subjectGradeMappingApi.createBatchMappings
export const updateMapping = subjectGradeMappingApi.updateMapping
export const deleteMapping = subjectGradeMappingApi.deleteMapping
export const deleteMappingsBySubject = subjectGradeMappingApi.deleteMappingsBySubject
export const deleteMappingsByGrade = subjectGradeMappingApi.deleteMappingsByGrade

export const migrateSubjectGradeData = migrationApi.migrateSubjectGradeData
export const checkMigrationStatus = migrationApi.checkMigrationStatus
export const clearMappingData = migrationApi.clearMappingData
