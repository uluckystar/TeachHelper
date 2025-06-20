<template>
  <div class="knowledge-base-management-simplified">
    <!-- 整合的顶部工具栏 -->
    <IntegratedToolbar
      :subjects="subjects"
      :grade-levels="gradeLevels"
      :search-mode="searchMode"
      :current-path="currentPath"
      @search="handleSearch"
      @filter-change="handleAdvancedFilterChange"
      @mode-change="handleSearchModeChange"
      @clear-search="handleClearSearch"
      @create-knowledge-base="showCreateDialog = true"
      @batch-upload="handleBatchUpload"
      @toolbar-action="handleToolbarAction"
    />

    <!-- 搜索状态条 -->
    <SearchStatusBar
      v-if="hasActiveSearch"
      :search-mode="searchMode"
      :search-query="currentSearchQuery"
      :active-subject-filter="activeSubjectFilter"
      :active-grade-filter="activeGradeFilter"
      :active-status-filter="activeStatusFilter"
      :total-results="pagination.total"
      :has-vector-results="vectorSearchResults.length > 0"
      @show-vector-results="showVectorSearchDialog = true"
      @clear-all-search="handleClearAllSearch"
    />

    <!-- 主内容区域 -->
    <div class="main-content">
      <!-- 原始样式侧边栏 -->
      <OriginalSidebar
        :subjects="subjects"
        :total-count="pagination.total"
        :active-filter="activeFilter"
        :active-subject="activeSubjectFilter"
        @filter-change="handleFilterChange"
        @subject-filter="handleSubjectFilter"
        @advanced-search="showAdvancedSearchDialog = true"
      />

      <!-- 内容区域 -->
      <div class="content-area">
        <!-- 操作栏 -->
        <div class="action-bar" v-if="selectedItems.length > 0">
          <div class="selected-info">
            已选择 {{ selectedItems.length }} 个知识库
          </div>
          <div class="batch-actions">
            <el-button size="small" @click="batchDownload">批量下载</el-button>
            <el-button size="small" @click="batchDelete" type="danger">批量删除</el-button>
            <el-button size="small" @click="selectedItems = []">取消选择</el-button>
          </div>
        </div>

        <!-- 优化的排序和筛选栏 -->
        <SortFilterBar
          :selected-count="selectedItems.length"
          :total-count="knowledgeBases.length"
          :filtered-count="pagination.total"
          :sort-by="sortBy"
          :sort-order="sortOrder"
          :view-mode="viewMode"
          :active-quick-filter="activeFilter"
          :active-subject-filter="activeSubjectFilter"
          :active-grade-filter="activeGradeFilter"
          :active-status-filter="activeStatusFilter"
          @select-all="selectAll"
          @batch-action="handleBatchAction"
          @remove-filter="handleRemoveFilter"
          @sort-change="handleSortChange"
          @sort-order-change="handleSortOrderChange"
          @view-mode-change="handleViewModeChange"
        />

        <!-- 知识库列表组件 -->
        <KnowledgeBaseList
          :knowledge-bases="knowledgeBases"
          :view-mode="viewMode"
          :total="pagination.total"
          :current-page="pagination.currentPage"
          :page-size="pagination.pageSize"
          @select="handleSelectKnowledgeBase"
          @action="handleKnowledgeBaseAction"
          @page-change="loadKnowledgeBases"
          @update:current-page="pagination.currentPage = $event"
          @update:page-size="pagination.pageSize = $event"
        />
      </div>
    </div>

    <!-- 高级搜索对话框组件 -->
    <AdvancedSearchDialog
      v-model="showAdvancedSearchDialog"
      :subjects="subjects"
      :grade-levels="gradeLevels"
      :searching="searching"
      @search="handleAdvancedSearch"
      @tab-change="handleSearchTabChange"
    />

    <!-- 向量搜索结果对话框组件 -->
    <VectorSearchDialog
      v-model="showVectorSearchDialog"
      :results="vectorSearchResults"
      :current-query="currentSearchQuery"
      @generate-question="handleGenerateQuestion"
      @reopen-search="showAdvancedSearchDialog = true"
    />

    <!-- 创建/编辑知识库对话框组件 -->
    <!-- Note: 由于现有API接口限制，此处存在类型冲突，需要在后续版本中统一接口定义 -->
    <KnowledgeBaseFormDialog
      v-model="showCreateDialog"
      :knowledge-base="editingKb as any"
      :grade-levels="gradeLevels"
      :subjects="subjects"
      :available-tags="availableTags"
      :saving="saving"
      @save="saveKnowledgeBase as any"
      @subject-created="handleSubjectCreated"
    />

    <!-- 文档上传对话框 -->
    <!-- Note: 由于现有API接口限制，此处存在类型冲突，需要在后续版本中统一接口定义 -->
    <DocumentUploadDialog
      v-model="showUploadDocDialog"
      :knowledge-base="selectedKb as any"
      @uploaded="handleDocumentUploaded"
    />

    <!-- AI出题对话框 -->
    <!-- Note: 由于现有API接口限制，此处存在类型冲突，需要在后续版本中统一接口定义 -->
    <AIQuestionGenerationDialog
      v-model="showAIQuestionDialog"
      :knowledge-base="selectedKb as any"
      @generated="handleQuestionsGenerated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { House } from '@element-plus/icons-vue'

// 导入组件
import KnowledgeBaseSidebar from '@/components/knowledge/KnowledgeBaseSidebar.vue'
import OriginalSidebar from '@/components/knowledge/OriginalSidebar.vue'
import KnowledgeBaseList from '@/components/knowledge/KnowledgeBaseList.vue'
import KnowledgeBaseFormDialog from '@/components/knowledge/KnowledgeBaseFormDialog.vue'
import AdvancedSearchDialog from '@/components/knowledge/AdvancedSearchDialog.vue'
import VectorSearchDialog from '@/components/knowledge/VectorSearchDialog.vue'
import SearchToolbarEnhanced from '@/components/knowledge/SearchToolbarEnhanced.vue'
import SearchStatusBar from '@/components/knowledge/SearchStatusBar.vue'
import IntegratedToolbar from '@/components/knowledge/IntegratedToolbar.vue'
import SortFilterBar from '@/components/knowledge/SortFilterBar.vue'
import DocumentUploadDialog from './components/DocumentUploadDialog.vue'
import AIQuestionGenerationDialog from './components/AIQuestionGenerationDialog.vue'

// 导入API和类型
import { knowledgeBaseApi, type KnowledgeBase as BaseKnowledgeBase } from '@/api/knowledge'
import type { VectorSearchResult, CategoryItem } from '@/types/knowledge'

// 扩展类型定义
interface KnowledgeBase extends BaseKnowledgeBase {
  status?: 'draft' | 'published' | 'archived'
  tags?: string[]
  questionCount?: number
  lastAccessTime?: string
}

// 响应式数据
const viewMode = ref<'grid' | 'list'>('grid')
const selectedCategory = ref('')
const currentPath = ref('')

// 搜索相关状态
const searchMode = ref<'basic' | 'smart'>('basic')
const currentSearchQuery = ref('')
const activeSubjectFilter = ref('')
const activeGradeFilter = ref('')
const activeStatusFilter = ref('')
const searching = ref(false)

// 知识库数据
const knowledgeBases = ref<KnowledgeBase[]>([])
const favoriteKnowledgeBases = ref<KnowledgeBase[]>([])
const recentKnowledgeBases = ref<KnowledgeBase[]>([])
const vectorSearchResults = ref<VectorSearchResult[]>([])

// 基础数据
const subjects = ref<string[]>([])
const gradeLevels = ref<string[]>([])
const availableTags = ref<string[]>([])

// 分类统计数据
const subjectCategories = ref<CategoryItem[]>([])
const gradeCategories = ref<CategoryItem[]>([])
const statusCategories = ref<CategoryItem[]>([])

// 分页数据
const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

// 选择和筛选状态
const selectedItems = ref<KnowledgeBase[]>([])
const activeFilter = ref('all')
const sortBy = ref('name')
const sortOrder = ref<'asc' | 'desc'>('asc')

// 对话框状态
const showCreateDialog = ref(false)
const showUploadDocDialog = ref(false)
const showAIQuestionDialog = ref(false)
const showAdvancedSearchDialog = ref(false)
const showVectorSearchDialog = ref(false)

// 表单相关
const editingKb = ref<KnowledgeBase | null>(null)
const selectedKb = ref<KnowledgeBase | null>(null)
const saving = ref(false)

// 计算属性
const hasActiveSearch = computed(() => {
  return !!(currentSearchQuery.value || activeSubjectFilter.value || activeGradeFilter.value || activeStatusFilter.value)
})

// 搜索状态栏支持
const hasVectorResults = computed(() => {
  return vectorSearchResults.value.length > 0
})

// 清空向量搜索结果
const clearVectorSearchResults = () => {
  vectorSearchResults.value = []
  showVectorSearchDialog.value = false
}

// 导出搜索结果
const exportSearchResults = () => {
  if (vectorSearchResults.value.length === 0) {
    ElMessage.warning('没有搜索结果可导出')
    return
  }
  
  try {
    const exportData = vectorSearchResults.value.map(result => ({
      标题: result.title || '未知标题',
      类型: result.type === 'document' ? '文档' : '知识点',
      相似度: (result.similarity * 100).toFixed(1) + '%',
      内容: result.content,
      来源: result.source || '未知来源'
    }))
    
    // 简单的CSV导出
    const csvContent = [
      Object.keys(exportData[0]).join(','),
      ...exportData.map(row => Object.values(row).map(val => `"${val}"`).join(','))
    ].join('\n')
    
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    const url = URL.createObjectURL(blob)
    link.setAttribute('href', url)
    link.setAttribute('download', `AI搜索结果_${currentSearchQuery.value}_${new Date().toLocaleDateString()}.csv`)
    link.style.visibility = 'hidden'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    ElMessage.success('搜索结果已导出')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

// 方法定义
const loadKnowledgeBases = async () => {
  try {
    const params = {
      page: pagination.currentPage - 1, // API uses 0-based paging
      size: pagination.pageSize,
    }
    
    const response = await knowledgeBaseApi.getKnowledgeBases(params)
    knowledgeBases.value = response.content
    pagination.total = response.totalElements
  } catch (error) {
    console.error('加载知识库失败:', error)
    ElMessage.error('加载知识库失败')
  }
}

const loadBasicData = async () => {
  try {
    // 临时数据，实际应该从API获取
    subjects.value = ['语文', '数学', '英语', '物理', '化学', '生物', '历史', '地理', '政治']
    gradeLevels.value = ['小学一年级', '小学二年级', '小学三年级', '小学四年级', '小学五年级', '小学六年级', 
                        '初一', '初二', '初三', '高一', '高二', '高三']
    availableTags.value = ['重点', '难点', '易错点', '基础']
    
    // 临时分类数据
    subjectCategories.value = subjects.value.map(s => ({ name: s, count: Math.floor(Math.random() * 10) }))
    gradeCategories.value = gradeLevels.value.slice(0, 6).map(g => ({ name: g, count: Math.floor(Math.random() * 5) }))
    statusCategories.value = [
      { name: '草稿', value: 'draft', count: 5 },
      { name: '已发布', value: 'published', count: 15 },
      { name: '已归档', value: 'archived', count: 2 }
    ]
  } catch (error) {
    console.error('加载基础数据失败:', error)
  }
}

// 搜索相关方法
const handleSearch = async (query: string) => {
  currentSearchQuery.value = query
  
  if (searchMode.value === 'smart') {
    await performSmartSearch(query)
  } else {
    await performBasicSearch(query)
  }
}

const performBasicSearch = async (query: string) => {
  try {
    searching.value = true
    pagination.currentPage = 1
    
    const params = {
      page: 0,
      size: pagination.pageSize,
      name: query,
      subject: activeSubjectFilter.value,
      gradeLevel: activeGradeFilter.value,
      status: activeStatusFilter.value
    }
    
    const response = await knowledgeBaseApi.getKnowledgeBases(params)
    knowledgeBases.value = response.content
    pagination.total = response.totalElements
    
    ElMessage.success(`找到 ${response.totalElements} 个相关知识库`)
  } catch (error) {
    console.error('基础搜索失败:', error)
    ElMessage.error('搜索失败')
  } finally {
    searching.value = false
  }
}

const performSmartSearch = async (query: string) => {
  try {
    searching.value = true
    
    // 执行真正的向量搜索API调用
    const vectorSearchRequest = {
      query,
      similarityThreshold: 0.6,
      limit: 50
    }
    
    const response = await knowledgeBaseApi.vectorSearch(vectorSearchRequest)
    const searchResults = response.data?.results || []
    
    // 存储向量搜索结果
    vectorSearchResults.value = searchResults
    
    if (searchResults.length === 0) {
      ElMessage.info('未找到相关内容，将显示所有知识库')
      await performBasicSearch('')
      return
    }
    
    // 从搜索结果中提取知识库ID
    const knowledgeBaseIds = new Set<number>()
    searchResults.forEach((result: any) => {
      if (result.metadata?.knowledgeBaseId) {
        knowledgeBaseIds.add(result.metadata.knowledgeBaseId)
      }
    })
    
    // 获取相关知识库详情
    if (knowledgeBaseIds.size > 0) {
      const idsArray = Array.from(knowledgeBaseIds)
      
      // 添加合理的数量限制，防止性能问题
      const MAX_KNOWLEDGE_BASES = 100
      if (idsArray.length > MAX_KNOWLEDGE_BASES) {
        console.warn(`向量搜索返回了 ${idsArray.length} 个知识库，超过建议的最大数量 ${MAX_KNOWLEDGE_BASES}，将只显示前 ${MAX_KNOWLEDGE_BASES} 个`)
        ElMessage.warning(`搜索结果过多，只显示前 ${MAX_KNOWLEDGE_BASES} 个最相关的知识库`)
        idsArray.splice(MAX_KNOWLEDGE_BASES)
      }
      
      // 使用新的API直接按ID列表获取知识库，避免分页限制问题
      const filteredKnowledgeBases = await knowledgeBaseApi.getKnowledgeBasesByIds(idsArray)
      
      knowledgeBases.value = filteredKnowledgeBases
      pagination.total = filteredKnowledgeBases.length
      
      ElMessage.success(`基于AI搜索找到 ${filteredKnowledgeBases.length} 个相关知识库`)
    } else {
      // 如果没有找到对应的知识库ID，直接显示搜索结果对话框
      ElMessage.success(`AI搜索找到 ${searchResults.length} 个相关内容片段`)
      showVectorSearchDialog.value = true
    }
  } catch (error) {
    console.error('智能搜索失败:', error)
    ElMessage.error('智能搜索失败，已切换到基础搜索')
    searchMode.value = 'basic'
    await performBasicSearch(query)
  } finally {
    searching.value = false
  }
}

const handleQuickFilter = (filter: string) => {
  const [type, value] = filter.split(':')
  
  switch (type) {
    case 'subject':
      activeSubjectFilter.value = value
      break
    case 'grade':
      activeGradeFilter.value = value
      break
    case 'status':
      activeStatusFilter.value = value
      break
  }
  
  // 重新搜索
  pagination.currentPage = 1
  if (currentSearchQuery.value) {
    handleSearch(currentSearchQuery.value)
  } else {
    loadKnowledgeBases()
  }
}

const handleSearchModeChange = (mode: 'basic' | 'smart') => {
  searchMode.value = mode
  vectorSearchResults.value = []
  
  if (currentSearchQuery.value) {
    handleSearch(currentSearchQuery.value)
  }
}

const handleClearSearch = () => {
  currentSearchQuery.value = ''
  activeSubjectFilter.value = ''
  activeGradeFilter.value = ''
  activeStatusFilter.value = ''
  vectorSearchResults.value = []
  
  pagination.currentPage = 1
  loadKnowledgeBases()
}

const handleClearAllSearch = () => {
  handleClearSearch()
}

const handleAdvancedSearch = async (searchData: any) => {
  if (searchData.type === 'basic') {
    // 设置筛选器
    activeSubjectFilter.value = searchData.data.subject || ''
    activeGradeFilter.value = searchData.data.gradeLevel || ''
    activeStatusFilter.value = searchData.data.status || ''
    
    await performBasicSearch(searchData.data.name || '')
  } else {
    // 智能搜索
    searchMode.value = 'smart'
    await performSmartSearch(searchData.data.query)
  }
  
  showAdvancedSearchDialog.value = false
}

const handleSearchTabChange = (tab: string) => {
  if (tab === 'smart') {
    searchMode.value = 'smart'
  } else {
    searchMode.value = 'basic'
  }
}

// 工具栏操作
const handleBatchUpload = () => {
  ElMessage.info('批量上传功能开发中...')
}

const handleToolbarAction = (action: string) => {
  switch (action) {
    case 'refresh':
      loadKnowledgeBases()
      break
    case 'export':
      handleExportData()
      break
    case 'import':
      ElMessage.info('导入数据功能开发中...')
      break
    case 'settings':
      ElMessage.info('设置功能开发中...')
      break
  }
}

const handleExportData = () => {
  ElMessage.info('导出数据功能开发中...')
}

// 侧边栏操作方法
const handleFilterChange = (filter: string) => {
  activeFilter.value = filter
  pagination.currentPage = 1
  
  // 根据筛选条件加载知识库
  switch (filter) {
    case 'all':
      loadKnowledgeBases()
      break
    case 'recent':
      ElMessage.info('最近使用功能开发中...')
      break
    case 'starred':
      ElMessage.info('收藏功能开发中...')
      break
    case 'mine':
      ElMessage.info('我的创建功能开发中...')
      break
  }
}

const handleSubjectFilter = (subject: string) => {
  activeSubjectFilter.value = subject
  pagination.currentPage = 1
  
  if (currentSearchQuery.value) {
    handleSearch(currentSearchQuery.value)
  } else {
    loadKnowledgeBases()
  }
}

// 批量操作方法
const selectAll = (selected: boolean) => {
  if (selected) {
    selectedItems.value = [...knowledgeBases.value]
  } else {
    selectedItems.value = []
  }
}

const batchDownload = () => {
  if (selectedItems.value.length === 0) {
    ElMessage.warning('请先选择要下载的知识库')
    return
  }
  ElMessage.info('批量下载功能开发中...')
}

const batchDelete = async () => {
  if (selectedItems.value.length === 0) {
    ElMessage.warning('请先选择要删除的知识库')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedItems.value.length} 个知识库吗？此操作不可恢复。`,
      '确认批量删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 批量删除逻辑
    for (const item of selectedItems.value) {
      if (item.id) {
        await knowledgeBaseApi.deleteKnowledgeBase(item.id)
      }
    }
    
    ElMessage.success('批量删除完成')
    selectedItems.value = []
    await loadKnowledgeBases()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
      ElMessage.error('批量删除失败')
    }
  }
}

const handleBatchAction = (action: string) => {
  switch (action) {
    case 'download':
      batchDownload()
      break
    case 'delete':
      batchDelete()
      break
    case 'export':
      ElMessage.info('批量导出功能开发中...')
      break
  }
}

// 筛选器管理
const handleRemoveFilter = (filterType: string) => {
  switch (filterType) {
    case 'subject':
      activeSubjectFilter.value = ''
      break
    case 'grade':
      activeGradeFilter.value = ''
      break
    case 'status':
      activeStatusFilter.value = ''
      break
    case 'query':
      currentSearchQuery.value = ''
      break
  }
  
  pagination.currentPage = 1
  if (currentSearchQuery.value) {
    handleSearch(currentSearchQuery.value)
  } else {
    loadKnowledgeBases()
  }
}

// 排序相关
const handleSortChange = (sortField: string) => {
  sortBy.value = sortField
  loadKnowledgeBases()
}

const handleSortOrderChange = (order: 'asc' | 'desc') => {
  sortOrder.value = order
  loadKnowledgeBases()
}

const handleViewModeChange = (mode: 'grid' | 'list') => {
  viewMode.value = mode
}

// 知识库操作方法
const handleSelectKnowledgeBase = (kb: KnowledgeBase) => {
  selectedKb.value = kb
}

const handleKnowledgeBaseAction = async (action: string, kb: KnowledgeBase) => {
  selectedKb.value = kb
  
  switch (action) {
    case 'edit':
      editingKb.value = kb
      showCreateDialog.value = true
      break
    case 'upload':
      showUploadDocDialog.value = true
      break
    case 'generate':
      showAIQuestionDialog.value = true
      break
    case 'favorite':
      await toggleFavorite(kb)
      break
    case 'delete':
      await deleteKnowledgeBase(kb)
      break
  }
}

const saveKnowledgeBase = async (data: KnowledgeBase) => {
  try {
    saving.value = true
    
    if (editingKb.value?.id) {
      await knowledgeBaseApi.updateKnowledgeBase(editingKb.value.id, data)
      ElMessage.success('知识库更新成功')
    } else {
      await knowledgeBaseApi.createKnowledgeBase(data)
      ElMessage.success('知识库创建成功')
    }
    
    showCreateDialog.value = false
    editingKb.value = null
    await loadKnowledgeBases()
  } catch (error) {
    console.error('保存知识库失败:', error)
    ElMessage.error('保存知识库失败')
  } finally {
    saving.value = false
  }
}

const deleteKnowledgeBase = async (kb: KnowledgeBase) => {
  if (!kb.id) {
    ElMessage.error('知识库ID无效')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除知识库 "${kb.name}" 吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await knowledgeBaseApi.deleteKnowledgeBase(kb.id)
    ElMessage.success('知识库删除成功')
    await loadKnowledgeBases()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除知识库失败:', error)
      ElMessage.error('删除知识库失败')
    }
  }
}

const toggleFavorite = async (kb: KnowledgeBase) => {
  if (!kb.id) {
    ElMessage.error('知识库ID无效')
    return
  }
  
  ElMessage.info('收藏功能开发中...')
}

// 对话框事件处理
const handleSubjectCreated = (subject: string) => {
  if (!subjects.value.includes(subject)) {
    subjects.value.push(subject)
  }
}

const handleDocumentUploaded = () => {
  ElMessage.success('文档上传成功')
  showUploadDocDialog.value = false
  loadKnowledgeBases()
}

const handleQuestionsGenerated = () => {
  ElMessage.success('题目生成成功')
  showAIQuestionDialog.value = false
}

// 高级筛选处理
const handleAdvancedFilterChange = (filters: any) => {
  activeSubjectFilter.value = filters.subject || ''
  activeGradeFilter.value = filters.grade || ''
  activeStatusFilter.value = filters.status || ''
  
  pagination.currentPage = 1
  if (currentSearchQuery.value) {
    handleSearch(currentSearchQuery.value)
  } else {
    loadKnowledgeBases()
  }
}

// 向量搜索相关
const handleGenerateQuestion = (result: VectorSearchResult) => {
  ElMessage.info('基于内容出题功能开发中...')
}

const handleShowVectorResults = () => {
  showVectorSearchDialog.value = true
}

const handleVectorSearchDialogClose = () => {
  showVectorSearchDialog.value = false
}

// 初始化
onMounted(() => {
  loadKnowledgeBases()
  loadBasicData()
})
</script>

<style scoped>
.knowledge-base-management-simplified {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.main-content {
  display: flex;
  gap: 20px;
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.content-area {
  flex: 1;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 6px;
  border-left: 4px solid #409eff;
}

.selected-info {
  font-weight: 500;
  color: #409eff;
}

.batch-actions {
  display: flex;
  gap: 8px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .main-content {
    flex-direction: column;
    gap: 16px;
    padding: 16px;
  }
  
  .knowledge-base-management-simplified {
    padding: 16px;
  }
}
</style>
