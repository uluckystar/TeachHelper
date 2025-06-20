// 知识库管理相关类型定义

export interface KnowledgeBase {
  id?: number
  name: string
  description?: string
  subject?: string
  gradeLevel?: string
  status?: 'draft' | 'published' | 'archived'
  tags?: string[]
  documentCount?: number
  knowledgePointCount?: number
  questionCount?: number
  favoriteCount?: number
  isFavorited?: boolean
  createdBy?: number
  createdAt?: string
  updatedAt?: string
  lastAccessTime?: string
}

export interface KnowledgeBaseCreateRequest {
  name: string
  description?: string
  subject?: string
  gradeLevel?: string
  tags?: string[]
}

export interface VectorSearchResult {
  id: string
  title: string
  type: string
  similarity: number
  content: string
  source: string
  knowledgeBaseId?: string
  knowledgeBaseName?: string     // 新增：知识库名称
  documentId?: string
  chunkIndex?: number
  chunkId?: string
  highlightedContent?: string
  matchedKeywords?: string[]
  keywordPositions?: Array<{
    keyword: string
    startIndex: number
    endIndex: number
    context?: string
  }>
  contextBefore?: string         // 新增：前文上下文
  contextAfter?: string          // 新增：后文上下文
}

export interface SearchOptions {
  knowledgeBaseIds?: number[]
  threshold?: number
  limit?: number
  includeContent?: boolean
  highlightKeywords?: boolean
}

export interface CategoryItem {
  name: string
  count: number
  value?: string
}
