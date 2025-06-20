import axios from '@/utils/request'

// 向量搜索API接口

/**
 * 执行向量搜索
 */
export const vectorSearch = (params: {
  query: string
  searchScope?: string[]
  similarityThreshold?: number
  maxResults?: number
}) => {
  return axios.post('/api/vector-search', params)
}

/**
 * 获取搜索历史
 */
export const getSearchHistory = () => {
  return axios.get('/api/vector-search/history')
}

/**
 * 清空搜索历史
 */
export const clearSearchHistory = () => {
  return axios.delete('/api/vector-search/history')
}

/**
 * 保存搜索到收藏
 */
export const saveSearchToFavorites = (query: string, results: any[]) => {
  return axios.post('/api/vector-search/favorites', {
    query,
    results
  })
}

/**
 * 获取收藏的搜索
 */
export const getFavoriteSearches = () => {
  return axios.get('/api/vector-search/favorites')
}

/**
 * 导出搜索结果
 */
export const exportSearchResults = (query: string, results: any[]) => {
  return axios.post('/api/vector-search/export', {
    query,
    results
  }, {
    responseType: 'blob'
  })
}
