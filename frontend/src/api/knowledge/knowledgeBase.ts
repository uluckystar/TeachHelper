import axios from '@/utils/request'

// 知识库类型定义
export interface KnowledgeBase {
  id?: number
  name: string
  description?: string
  subject: string
  gradeLevel: string
  status?: 'draft' | 'published' | 'archived'
  tags?: string[]
  documentCount?: number
  questionCount?: number
  createTime?: string
  updateTime?: string
  lastAccessTime?: string
  createdBy?: string
}

// 知识库API接口

/**
 * 获取知识库列表
 */
export const getKnowledgeBases = (params: {
  page: number
  size: number
  category?: string
  query?: string
}) => {
  return axios.get('/api/knowledge-bases', { params })
}

/**
 * 获取知识库详情
 */
export const getKnowledgeBase = (id: number) => {
  return axios.get(`/api/knowledge-bases/${id}`)
}

/**
 * 创建知识库
 */
export const createKnowledgeBase = (data: any) => {
  return axios.post('/api/knowledge-bases', data)
}

/**
 * 更新知识库
 */
export const updateKnowledgeBase = (id: number, data: any) => {
  return axios.put(`/api/knowledge-bases/${id}`, data)
}

/**
 * 删除知识库
 */
export const deleteKnowledgeBase = (id: number) => {
  return axios.delete(`/api/knowledge-bases/${id}`)
}

/**
 * 获取收藏的知识库
 */
export const getFavoriteKnowledgeBases = () => {
  return axios.get('/api/knowledge-bases/favorites')
}

/**
 * 收藏知识库
 */
export const favoriteKnowledgeBase = (id: number) => {
  return axios.post(`/api/knowledge-bases/${id}/favorite`)
}

/**
 * 取消收藏知识库
 */
export const unfavoriteKnowledgeBase = (id: number) => {
  return axios.delete(`/api/knowledge-bases/${id}/favorite`)
}

/**
 * 清空收藏
 */
export const clearFavorites = () => {
  return axios.delete('/api/knowledge-bases/favorites')
}

/**
 * 获取最近访问的知识库
 */
export const getRecentKnowledgeBases = () => {
  return axios.get('/api/knowledge-bases/recent')
}

/**
 * 添加到最近访问
 */
export const addToRecent = (id: number) => {
  return axios.post(`/api/knowledge-bases/${id}/recent`)
}

/**
 * 清空最近访问
 */
export const clearRecent = () => {
  return axios.delete('/api/knowledge-bases/recent')
}

/**
 * 获取学科列表
 */
export const getSubjects = () => {
  return axios.get('/api/knowledge-bases/subjects')
}

/**
 * 获取年级列表
 */
export const getGradeLevels = () => {
  return axios.get('/api/knowledge-bases/grade-levels')
}

/**
 * 获取可用标签列表
 */
export const getAvailableTags = () => {
  return axios.get('/api/knowledge-bases/tags')
}

/**
 * 获取分类统计信息
 */
export const getCategories = () => {
  return axios.get('/api/knowledge-bases/categories')
}

/**
 * 批量操作知识库
 */
export const batchOperation = (operation: string, ids: number[]) => {
  return axios.post('/api/knowledge-bases/batch', {
    operation,
    ids
  })
}

/**
 * 导出知识库数据
 */
export const exportKnowledgeBases = (ids?: number[]) => {
  return axios.post('/api/knowledge-bases/export', { ids }, {
    responseType: 'blob'
  })
}

/**
 * 根据ID列表批量获取知识库
 */
export const getKnowledgeBasesByIds = (ids: number[]) => {
  return axios.post('/api/knowledge-bases/by-ids', ids)
}
