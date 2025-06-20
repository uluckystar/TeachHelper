<template>
  <div class="knowledge-base-management">
    <!-- 顶部工具栏 -->
    <div class="top-toolbar">
      <div class="toolbar-left">
        <div class="breadcrumb-area">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>
              <el-icon><House /></el-icon>
              知识库
            </el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentPath">{{ currentPath }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="view-actions">
          <el-button-group>
            <el-button 
              :type="viewMode === 'grid' ? 'primary' : ''" 
              @click="viewMode = 'grid'" 
              icon="Grid"
              size="small"
            />
            <el-button 
              :type="viewMode === 'list' ? 'primary' : ''" 
              @click="viewMode = 'list'" 
              icon="List"
              size="small"
            />
          </el-button-group>
        </div>
      </div>
      
      <div class="toolbar-right">
        <!-- 搜索工具栏组件 -->
        <SearchToolbar
          :subjects="subjects"
          :grade-levels="gradeLevels"
          :search-mode="searchMode"
          @search="handleSearch"
          @quick-filter="handleQuickFilter"
          @mode-change="handleSearchModeChange"
          @advanced-search="showAdvancedSearch = true"
          @clear="handleSearchClear"
        />
        
        <el-button type="primary" @click="showCreateDialog = true" icon="Plus">
          创建知识库
        </el-button>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="main-content">
      <!-- 侧边栏组件 -->
      <KnowledgeBaseSidebar
        :favorite-knowledge-bases="favoriteKnowledgeBases"
        :recent-knowledge-bases="recentKnowledgeBases"
        :subject-categories="subjectCategories"
        :grade-categories="gradeCategories"
        :status-categories="statusCategories"
        :selected-category="selectedCategory"
        @select-knowledge-base="handleSelectKnowledgeBase"
        @toggle-favorite="toggleFavorite"
        @clear-favorites="clearFavorites"
        @clear-recent="clearRecent"
        @select-category="handleSelectCategory"
        @create-knowledge-base="showCreateDialog = true"
        @batch-upload="handleBatchUpload"
        @export-data="handleExportData"
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

    <!-- 高级搜索对话框组件 -->
    <AdvancedSearchDialog
      v-model="showAdvancedSearch"
      :subjects="subjects"
      :grade-levels="gradeLevels"
      :searching="searching"
      @search="handleAdvancedSearch"
      @tab-change="handleSearchTabChange"
    />

    <!-- AI智能搜索结果对话框组件 -->
    <VectorSearchDialog
      v-model="showVectorSearchDialog"
      :results="vectorSearchResults"
      :current-query="currentSearchQuery"
      @reopen-search="showAdvancedSearch = true"
      @view-detail="viewVectorSearchResult"
      @generate-question="generateFromVectorResult"
    />

    <!-- 创建/编辑知识库对话框组件 -->
    <KnowledgeBaseFormDialog
      v-model="showCreateDialog"
      :knowledge-base="editingKb"
      :grade-levels="gradeLevels"
      :subjects="subjects"
      :available-tags="availableTags"
      :saving="saving"
      @save="saveKnowledgeBase"
      @subject-created="handleSubjectCreated"
    />

    <!-- 文档上传对话框 -->
    <DocumentUploadDialog
      v-model="showUploadDocDialog"
      :knowledge-base="selectedKb"
      @uploaded="handleDocumentUploaded"
    />

    <!-- AI出题对话框 -->
    <AIQuestionGenerationDialog
      v-model="showAIQuestionDialog"
      :knowledge-base="selectedKb"
      @generated="handleQuestionsGenerated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { House } from '@element-plus/icons-vue'

// 导入组件
import SearchToolbar from '@/components/knowledge/SearchToolbar.vue'
import KnowledgeBaseSidebar from '@/components/knowledge/KnowledgeBaseSidebar.vue'
import KnowledgeBaseList from '@/components/knowledge/KnowledgeBaseList.vue'
import AdvancedSearchDialog from '@/components/knowledge/AdvancedSearchDialog.vue'
import VectorSearchDialog from '@/components/knowledge/VectorSearchDialog.vue'
import KnowledgeBaseFormDialog from '@/components/knowledge/KnowledgeBaseFormDialog.vue'
import DocumentUploadDialog from './components/DocumentUploadDialog.vue'
import AIQuestionGenerationDialog from './components/AIQuestionGenerationDialog.vue'

// 导入API
import { knowledgeBaseApi } from '@/api/knowledge'
// import * as vectorSearchAPI from '@/api/knowledge/vectorSearch'

// 导入类型定义
import type { 
  KnowledgeBase, 
  VectorSearchResult, 
  SearchOptions, 
  CategoryItem 
} from '@/types/knowledge'

// 响应式数据
const viewMode = ref<'grid' | 'list'>('grid')
const searchMode = ref<'basic' | 'smart'>('basic')
const currentPath = ref('')
const selectedCategory = ref('')

// 知识库数据
const knowledgeBases = ref<KnowledgeBase[]>([])
const favoriteKnowledgeBases = ref<KnowledgeBase[]>([])
const recentKnowledgeBases = ref<KnowledgeBase[]>([])

// 基础数据
const subjects = ref<string[]>([])
const gradeLevels = ref<string[]>([])
const availableTags = ref<string[]>([])

// 分类统计数据
const subjectCategories = ref<Array<{name: string, count: number}>>([])
const gradeCategories = ref<Array<{name: string, count: number}>>([])
const statusCategories = ref<Array<{name: string, value: string, count: number}>>([])

// 分页数据
const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

// 搜索相关
const searching = ref(false)
const currentSearchQuery = ref('')
const vectorSearchResults = ref<VectorSearchResult[]>([])

// 对话框状态
const showAdvancedSearch = ref(false)
const showVectorSearchDialog = ref(false)
const showCreateDialog = ref(false)
const showUploadDocDialog = ref(false)
const showAIQuestionDialog = ref(false)

// 表单相关
const editingKb = ref<KnowledgeBase | null>(null)
const selectedKb = ref<KnowledgeBase | null>(null)
const saving = ref(false)

// 计算属性
const isSearching = computed(() => {
  return currentSearchQuery.value.trim() !== ''
})

// 方法定义
const loadKnowledgeBases = async () => {
  try {
    const params = {
      page: pagination.currentPage,
      size: pagination.pageSize,
      category: selectedCategory.value,
      query: currentSearchQuery.value
    }
    
    const response = await knowledgeBaseApi.getKnowledgeBases(params)
    knowledgeBases.value = response.data.content
    pagination.total = response.data.totalElements
  } catch (error) {
    console.error('加载知识库失败:', error)
    ElMessage.error('加载知识库失败')
  }
}

const loadFavorites = async () => {
  try {
    const response = await knowledgeBaseAPI.getFavoriteKnowledgeBases()
    favoriteKnowledgeBases.value = response.data
  } catch (error) {
    console.error('加载收藏知识库失败:', error)
  }
}

const loadRecent = async () => {
  try {
    const response = await knowledgeBaseAPI.getRecentKnowledgeBases()
    recentKnowledgeBases.value = response.data
  } catch (error) {
    console.error('加载最近访问失败:', error)
  }
}

const loadBasicData = async () => {
  try {
    const [subjectsRes, gradesRes, tagsRes, categoriesRes] = await Promise.all([
      knowledgeBaseAPI.getSubjects(),
      knowledgeBaseAPI.getGradeLevels(),
      knowledgeBaseAPI.getAvailableTags(),
      knowledgeBaseAPI.getCategories()
    ])
    
    subjects.value = subjectsRes.data
    gradeLevels.value = gradesRes.data
    availableTags.value = tagsRes.data
    
    const categories = categoriesRes.data
    subjectCategories.value = categories.subjects
    gradeCategories.value = categories.grades
    statusCategories.value = categories.statuses
  } catch (error) {
    console.error('加载基础数据失败:', error)
  }
}

// 搜索相关方法
const handleSearch = (query: string) => {
  currentSearchQuery.value = query
  pagination.currentPage = 1
  
  if (searchMode.value === 'smart') {
    performVectorSearch(query)
  } else {
    loadKnowledgeBases()
  }
}

const handleQuickFilter = (filter: string) => {
  const [type, value] = filter.split(':')
  if (value) {
    selectedCategory.value = filter
  } else {
    selectedCategory.value = ''
  }
  pagination.currentPage = 1
  loadKnowledgeBases()
}

const handleSearchModeChange = (mode: 'basic' | 'smart') => {
  searchMode.value = mode
  currentSearchQuery.value = ''
  selectedCategory.value = ''
  loadKnowledgeBases()
}

const handleSearchClear = () => {
  currentSearchQuery.value = ''
  selectedCategory.value = ''
  loadKnowledgeBases()
}

const handleAdvancedSearch = async ({ type, data }: { type: 'basic' | 'smart', data: any }) => {
  searching.value = true
  
  try {
    if (type === 'smart') {
      currentSearchQuery.value = data.query
      await performVectorSearch(data.query, data.options)
    } else {
      // 基础搜索逻辑
      Object.assign(pagination, { currentPage: 1 })
      // 构建搜索参数
      const searchParams = { ...data }
      await loadKnowledgeBases()
    }
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败')
  } finally {
    searching.value = false
  }
}

const performVectorSearch = async (query: string, options = {}) => {
  try {
    searching.value = true
    currentSearchQuery.value = query
    
    const searchParams = {
      query,
      ...options
    }
    
    const response = await vectorSearchAPI.vectorSearch(searchParams)
    vectorSearchResults.value = response.data
    showVectorSearchDialog.value = true
  } catch (error) {
    console.error('向量搜索失败:', error)
    ElMessage.error('向量搜索失败')
  } finally {
    searching.value = false
  }
}

const handleSearchTabChange = (tab: string) => {
  // 处理搜索标签页切换
}

// 知识库操作方法
const handleSelectKnowledgeBase = (kb: KnowledgeBase) => {
  selectedKb.value = kb
  // 添加到最近访问
  addToRecent(kb)
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
      await knowledgeBaseAPI.updateKnowledgeBase(editingKb.value.id, data)
      ElMessage.success('知识库更新成功')
    } else {
      await knowledgeBaseAPI.createKnowledgeBase(data)
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
    
    await knowledgeBaseAPI.deleteKnowledgeBase(kb.id)
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
  
  try {
    if (kb.isFavorited) {
      await knowledgeBaseAPI.unfavoriteKnowledgeBase(kb.id)
      ElMessage.success('已取消收藏')
    } else {
      await knowledgeBaseAPI.favoriteKnowledgeBase(kb.id)
      ElMessage.success('收藏成功')
    }
    
    await loadKnowledgeBases()
    await loadFavorites()
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  }
}

// 侧边栏操作方法
const handleSelectCategory = (type: string, value: string) => {
  selectedCategory.value = `${type}:${value}`
  pagination.currentPage = 1
  loadKnowledgeBases()
}

const clearFavorites = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有收藏吗？', '确认清空', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await knowledgeBaseAPI.clearFavorites()
    await loadFavorites()
    await loadKnowledgeBases()
    ElMessage.success('收藏已清空')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清空收藏失败:', error)
      ElMessage.error('清空收藏失败')
    }
  }
}

const clearRecent = async () => {
  try {
    await ElMessageBox.confirm('确定要清空最近访问记录吗？', '确认清空', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await knowledgeBaseAPI.clearRecent()
    await loadRecent()
    ElMessage.success('最近访问已清空')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清空最近访问失败:', error)
      ElMessage.error('清空最近访问失败')
    }
  }
}

const addToRecent = async (kb: KnowledgeBase) => {
  if (!kb.id) return
  
  try {
    await knowledgeBaseAPI.addToRecent(kb.id)
    await loadRecent()
  } catch (error) {
    console.error('添加到最近访问失败:', error)
  }
}

// 其他操作方法
const handleSubjectCreated = (subject: string) => {
  if (!subjects.value.includes(subject)) {
    subjects.value.push(subject)
  }
}

const handleDocumentUploaded = () => {
  showUploadDocDialog.value = false
  ElMessage.success('文档上传成功')
  loadKnowledgeBases()
}

const handleQuestionsGenerated = () => {
  showAIQuestionDialog.value = false
  ElMessage.success('题目生成成功')
}

const handleBatchUpload = () => {
  ElMessage.info('批量上传功能开发中...')
}

const handleExportData = () => {
  ElMessage.info('数据导出功能开发中...')
}

// 向量搜索结果处理
const viewVectorSearchResult = (result: VectorSearchResult) => {
  ElMessage.info('查看详情功能开发中...')
}

const generateFromVectorResult = (result: VectorSearchResult) => {
  ElMessage.info('基于内容出题功能开发中...')
}

// 生命周期
onMounted(async () => {
  await Promise.all([
    loadBasicData(),
    loadKnowledgeBases(),
    loadFavorites(),
    loadRecent()
  ])
})
</script>

<style scoped>
.knowledge-base-management {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
}

.top-toolbar {
  height: 60px;
  background: white;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.breadcrumb-area {
  display: flex;
  align-items: center;
}

.view-actions {
  margin-left: 20px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

:deep(.el-breadcrumb__inner) {
  color: #606266 !important;
}

:deep(.el-breadcrumb__inner.is-link:hover) {
  color: #409eff !important;
}
</style>
