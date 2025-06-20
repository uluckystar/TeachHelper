<template>
  <div class="integrated-toolbar">
    <!-- 左侧区域：面包屑和视图切换 -->
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
    </div>
    
    <!-- 中间区域：搜索功能 -->
    <div class="toolbar-center">
      <div class="search-container">
        <!-- 搜索输入框 -->
        <el-input
          v-model="searchText"
          :placeholder="searchPlaceholder"
          :prefix-icon="Search"
          clearable
          size="default"
          style="width: 320px;"
          @input="handleSearchInput"
          @clear="handleSearchClear"
          @keyup.enter="handleSearchSubmit"
          class="main-search-input"
        />
        
        <!-- 搜索模式切换 -->
        <el-button-group class="search-mode-toggle">
          <el-button 
            :type="searchMode === 'basic' ? 'primary' : ''" 
            @click="handleModeChange('basic')"
            size="default"
            :title="'基础搜索：按知识库名称、学科、年级等条件搜索'"
          >
            <el-icon><Search /></el-icon>
            基础
          </el-button>
          <el-button 
            :type="searchMode === 'smart' ? 'primary' : ''" 
            @click="handleModeChange('smart')"
            size="default"
            :title="'AI智能搜索：根据语义理解查找最相关的内容'"
          >
            <el-icon><MagicStick /></el-icon>
            AI智能
          </el-button>
        </el-button-group>
        
        <!-- 高级筛选和搜索 -->
        <el-popover
          placement="bottom-start"
          :width="420"
          trigger="click"
          v-model:visible="showAdvancedFilter"
        >
          <template #reference>
            <el-button size="default" :type="hasActiveFilters ? 'primary' : ''">
              <el-icon><Filter /></el-icon>
              高级筛选
              <el-badge :value="activeFilterCount" v-if="activeFilterCount > 0" class="filter-badge" />
            </el-button>
          </template>
          
          <!-- 高级筛选面板 -->
          <div class="advanced-filter-panel">
            <div class="filter-section">
              <label class="filter-label">学科筛选</label>
              <el-select 
                v-model="filterForm.subject" 
                placeholder="选择学科" 
                clearable
                style="width: 100%;"
              >
                <el-option label="全部学科" value="" />
                <el-option 
                  v-for="subject in subjects" 
                  :key="subject"
                  :label="subject"
                  :value="subject"
                />
              </el-select>
            </div>
            
            <div class="filter-section">
              <label class="filter-label">年级筛选</label>
              <el-select 
                v-model="filterForm.grade" 
                placeholder="选择年级" 
                clearable
                style="width: 100%;"
              >
                <el-option label="全部年级" value="" />
                <el-option 
                  v-for="grade in gradeLevels" 
                  :key="grade"
                  :label="grade"
                  :value="grade"
                />
              </el-select>
            </div>
            
            <div class="filter-section">
              <label class="filter-label">状态筛选</label>
              <el-select 
                v-model="filterForm.status" 
                placeholder="选择状态" 
                clearable
                style="width: 100%;"
              >
                <el-option label="全部状态" value="" />
                <el-option label="已发布" value="published" />
                <el-option label="草稿" value="draft" />
                <el-option label="已归档" value="archived" />
              </el-select>
            </div>
            
            <div class="filter-section">
              <label class="filter-label">创建时间</label>
              <el-date-picker
                v-model="filterForm.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                style="width: 100%;"
              />
            </div>
            
            <div class="filter-actions">
              <el-button @click="clearAllFilters" size="small">清空筛选</el-button>
              <el-button type="primary" @click="applyFilters" size="small">应用筛选</el-button>
            </div>
          </div>
        </el-popover>
      </div>
    </div>
    
    <!-- 右侧区域：操作按钮 -->
    <div class="toolbar-right">
      <div class="action-buttons">
        <el-button type="primary" icon="Plus" @click="$emit('create-knowledge-base')">
          创建知识库
        </el-button>
        <el-button icon="Upload" @click="$emit('batch-upload')">
          批量上传
        </el-button>
        <el-dropdown @command="handleToolbarAction" trigger="click">
          <el-button icon="MoreFilled" />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="refresh" icon="Refresh">刷新列表</el-dropdown-item>
              <el-dropdown-item command="export" icon="Download">导出数据</el-dropdown-item>
              <el-dropdown-item command="import" icon="Upload">导入数据</el-dropdown-item>
              <el-dropdown-item divided command="settings" icon="Setting">系统设置</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { 
  House, 
  Search, 
  MagicStick, 
  Filter,
  Plus,
  Upload,
  MoreFilled,
  Refresh,
  Download,
  Setting
} from '@element-plus/icons-vue'

interface Props {
  subjects?: string[]
  gradeLevels?: string[]
  searchMode?: 'basic' | 'smart'
  currentPath?: string
}

interface Emits {
  (e: 'search', query: string, mode: 'basic' | 'smart'): void
  (e: 'filter-change', filters: FilterForm): void
  (e: 'mode-change', mode: 'basic' | 'smart'): void
  (e: 'clear-search'): void
  (e: 'create-knowledge-base'): void
  (e: 'batch-upload'): void
  (e: 'toolbar-action', action: string): void
}

interface FilterForm {
  subject: string
  grade: string
  status: string
  dateRange: [Date, Date] | []
}

const props = withDefaults(defineProps<Props>(), {
  subjects: () => [],
  gradeLevels: () => [],
  searchMode: 'basic',
  currentPath: ''
})

const emit = defineEmits<Emits>()

// 响应式数据
const searchText = ref('')
const showAdvancedFilter = ref(false)
const filterForm = ref<FilterForm>({
  subject: '',
  grade: '',
  status: '',
  dateRange: []
})

// 防抖搜索
let searchTimeout: number | null = null

// 计算属性
const searchPlaceholder = computed(() => {
  if (props.searchMode === 'smart') {
    return '🧠 AI智能搜索：描述您要找的内容，如"高一数学函数的应用题"'
  } else {
    return '🔍 基础搜索：输入知识库名称、关键词等进行搜索'
  }
})

const hasActiveFilters = computed(() => {
  return !!(filterForm.value.subject || 
           filterForm.value.grade || 
           filterForm.value.status || 
           filterForm.value.dateRange)
})

const activeFilterCount = computed(() => {
  let count = 0
  if (filterForm.value.subject) count++
  if (filterForm.value.grade) count++
  if (filterForm.value.status) count++
  if (filterForm.value.dateRange) count++
  return count
})

// 事件处理方法
const handleSearchInput = () => {
  // 防抖处理
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
  searchTimeout = setTimeout(() => {
    if (searchText.value.trim()) {
      emit('search', searchText.value.trim(), props.searchMode)
    }
  }, 500)
}

const handleSearchClear = () => {
  searchText.value = ''
  emit('clear-search')
}

const handleSearchSubmit = () => {
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
  if (searchText.value.trim()) {
    emit('search', searchText.value.trim(), props.searchMode)
  } else {
    emit('clear-search')
  }
}

const handleModeChange = (mode: 'basic' | 'smart') => {
  emit('mode-change', mode)
  // 如果有搜索内容，重新搜索
  if (searchText.value.trim()) {
    emit('search', searchText.value.trim(), mode)
  }
}

const applyFilters = () => {
  emit('filter-change', { ...filterForm.value })
  showAdvancedFilter.value = false
}

const clearAllFilters = () => {
  filterForm.value = {
    subject: '',
    grade: '',
    status: '',
    dateRange: []
  }
  emit('filter-change', { ...filterForm.value })
}

const handleToolbarAction = (action: string) => {
  emit('toolbar-action', action)
}

// 监听筛选表单变化，自动应用筛选
watch(filterForm, () => {
  emit('filter-change', { ...filterForm.value })
}, { deep: true })
</script>

<style scoped>
.integrated-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  min-height: 72px;
}

.toolbar-left {
  flex-shrink: 0;
}

.breadcrumb-area {
  display: flex;
  align-items: center;
}

.toolbar-center {
  flex: 1;
  display: flex;
  justify-content: center;
  max-width: 800px;
  margin: 0 32px;
}

.search-container {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  max-width: 680px;
}

.main-search-input {
  flex: 1;
  min-width: 280px;
}

.search-mode-toggle {
  flex-shrink: 0;
}

.search-mode-toggle .el-button {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.search-mode-toggle .el-button:hover {
  transform: translateY(-1px);
}

.toolbar-right {
  flex-shrink: 0;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 高级筛选面板样式 */
.advanced-filter-panel {
  padding: 16px;
}

.filter-section {
  margin-bottom: 16px;
}

.filter-section:last-of-type {
  margin-bottom: 20px;
}

.filter-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 8px;
}

.filter-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border-top: 1px solid #e4e7ed;
  padding-top: 16px;
}

.filter-actions .el-button {
  flex: 1;
}

/* 筛选徽章样式 */
.filter-badge {
  margin-left: 4px;
}

/* 面包屑样式优化 */
:deep(.el-breadcrumb__inner) {
  color: #606266 !important;
  font-weight: 500;
}

:deep(.el-breadcrumb__inner.is-link:hover) {
  color: #409eff !important;
}

/* 搜索输入框优化 */
:deep(.main-search-input .el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

:deep(.main-search-input .el-input__wrapper:hover) {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.12);
}

:deep(.main-search-input .el-input__wrapper.is-focus) {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

/* 按钮组优化 */
:deep(.el-button-group .el-button) {
  border-radius: 0;
  transition: all 0.3s ease;
}

:deep(.el-button-group .el-button:first-child) {
  border-top-left-radius: 8px;
  border-bottom-left-radius: 8px;
}

:deep(.el-button-group .el-button:last-child) {
  border-top-right-radius: 8px;
  border-bottom-right-radius: 8px;
}

:deep(.el-button-group .el-button:hover) {
  z-index: 1;
  transform: translateY(-1px);
}

/* 下拉菜单优化 */
:deep(.el-dropdown-menu) {
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border: 1px solid #e4e7ed;
}

:deep(.el-dropdown-menu__item) {
  padding: 10px 16px;
  font-size: 14px;
  transition: all 0.2s ease;
}

:deep(.el-dropdown-menu__item:hover) {
  background: #f5f7fa;
  color: #409eff;
}

/* 日期选择器优化 */
:deep(.el-date-editor .el-input__wrapper) {
  border-radius: 6px;
}

/* 选择器优化 */
:deep(.el-select .el-input__wrapper) {
  border-radius: 6px;
  transition: all 0.3s ease;
}

:deep(.el-select .el-input__wrapper:hover) {
  border-color: #c0c4cc;
}

:deep(.el-select .el-input__wrapper.is-focus) {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .toolbar-center {
    margin: 0 20px;
  }
  
  .search-container {
    max-width: 600px;
  }
  
  .main-search-input {
    min-width: 240px;
  }
}

@media (max-width: 1200px) {
  .toolbar-center {
    margin: 0 16px;
  }
  
  .search-container {
    max-width: 500px;
  }
  
  .search-mode-toggle .el-button span {
    display: none; /* 隐藏文字，只显示图标 */
  }
}

@media (max-width: 768px) {
  .integrated-toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
    padding: 16px;
  }
  
  .toolbar-left,
  .toolbar-center,
  .toolbar-right {
    margin: 0;
  }
  
  .toolbar-center {
    order: 1;
  }
  
  .toolbar-left {
    order: 0;
  }
  
  .toolbar-right {
    order: 2;
    justify-content: center;
  }
  
  .search-container {
    flex-direction: column;
    gap: 12px;
    max-width: none;
  }
  
  .main-search-input {
    width: 100%;
    min-width: auto;
  }
  
  .search-mode-toggle {
    width: 100%;
  }
  
  .search-mode-toggle .el-button {
    flex: 1;
  }
  
  .action-buttons {
    justify-content: center;
    flex-wrap: wrap;
  }
}
</style>
