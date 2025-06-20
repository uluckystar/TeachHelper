<template>
  <div class="enhanced-sort-filter-bar">
    <!-- 左侧：选择和统计信息 -->
    <div class="left-section">
      <div class="selection-area">
        <el-checkbox 
          :model-value="isAllSelected"
          :indeterminate="isIndeterminate"
          @change="handleSelectAll"
          class="select-all-checkbox"
        >
          全选
        </el-checkbox>
        
        <!-- 批量操作快捷按钮 -->
        <div class="batch-actions" v-if="selectedCount > 0">
          <el-tag type="info" class="selection-tag">
            已选择 {{ selectedCount }} 项
          </el-tag>
          <el-button-group size="small">
            <el-button @click="handleBatchAction('download')" icon="Download">
              下载
            </el-button>
            <el-button @click="handleBatchAction('delete')" type="danger" icon="Delete">
              删除
            </el-button>
            <el-button @click="handleBatchAction('cancel')" icon="Close">
              取消
            </el-button>
          </el-button-group>
        </div>
      </div>
      
      <!-- 统计信息 -->
      <div class="stats-info" v-if="selectedCount === 0">
        <el-icon class="stats-icon"><Collection /></el-icon>
        <span class="stats-text">
          共 {{ totalCount }} 个知识库
          <span v-if="filteredCount !== totalCount" class="filtered-indicator">
            (筛选后 {{ filteredCount }} 个)
          </span>
        </span>
      </div>
    </div>
    
    <!-- 右侧：排序和视图控制 -->
    <div class="right-section">
      <!-- 快速筛选标签 -->
      <div class="quick-filters" v-if="activeFilters.length > 0">
        <el-button-group size="small">
          <el-button 
            v-for="filter in activeFilters"
            :key="filter.key"
            type="primary"
            @click="handleRemoveFilter(filter.key)"
            class="filter-tag"
          >
            {{ filter.label }}
            <el-icon class="remove-icon"><Close /></el-icon>
          </el-button>
        </el-button-group>
        <el-divider direction="vertical" />
      </div>
      
      <!-- 排序控制 -->
      <div class="sort-controls">
        <el-select 
          :model-value="sortBy" 
          @update:model-value="handleSortChange"
          size="small" 
          class="sort-select"
          placeholder="排序方式"
        >
          <template #prefix>
            <el-icon><Sort /></el-icon>
          </template>
          <el-option 
            v-for="option in sortOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          >
            <div class="sort-option-item">
              <el-icon><component :is="option.icon" /></el-icon>
              <span>{{ option.label }}</span>
            </div>
          </el-option>
        </el-select>
        
        <!-- 排序方向切换 -->
        <el-button 
          @click="toggleSortOrder"
          size="small"
          :title="sortOrderTitle"
          class="sort-order-btn"
        >
          <el-icon>
            <component :is="sortOrder === 'asc' ? 'SortUp' : 'SortDown'" />
          </el-icon>
        </el-button>
      </div>
      
      <!-- 视图模式切换 -->
      <div class="view-controls">
        <el-divider direction="vertical" />
        <el-button-group size="small">
          <el-button 
            :type="viewMode === 'grid' ? 'primary' : ''" 
            @click="handleViewModeChange('grid')"
            :title="'网格视图'"
          >
            <el-icon><Grid /></el-icon>
          </el-button>
          <el-button 
            :type="viewMode === 'list' ? 'primary' : ''" 
            @click="handleViewModeChange('list')"
            :title="'列表视图'"
          >
            <el-icon><List /></el-icon>
          </el-button>
        </el-button-group>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { 
  Collection, 
  Close,
  Download, 
  Delete,
  Sort,
  SortUp,
  SortDown,
  Document,
  Timer,
  Star,
  User,
  Calendar,
  DataAnalysis,
  Reading,
  Grid,
  List
} from '@element-plus/icons-vue'

interface Props {
  selectedCount?: number
  totalCount?: number
  filteredCount?: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
  viewMode?: 'grid' | 'list'
  activeQuickFilter?: string
  activeSubjectFilter?: string
  activeGradeFilter?: string
  activeStatusFilter?: string
}

interface Emits {
  (e: 'select-all', value: boolean): void
  (e: 'batch-action', action: string): void
  (e: 'remove-filter', filterKey: string): void
  (e: 'sort-change', sortBy: string): void
  (e: 'sort-order-change', order: 'asc' | 'desc'): void
  (e: 'view-mode-change', mode: 'grid' | 'list'): void
}

const props = withDefaults(defineProps<Props>(), {
  selectedCount: 0,
  totalCount: 0,
  filteredCount: 0,
  sortBy: 'name',
  sortOrder: 'asc',
  viewMode: 'grid',
  activeQuickFilter: 'all',
  activeSubjectFilter: '',
  activeGradeFilter: '',
  activeStatusFilter: ''
})

const emit = defineEmits<Emits>()

// 计算属性
const isAllSelected = computed(() => {
  return props.selectedCount > 0 && props.selectedCount === props.filteredCount
})

const isIndeterminate = computed(() => {
  return props.selectedCount > 0 && props.selectedCount < props.filteredCount
})

// 当前激活的筛选器
const activeFilters = computed(() => {
  const filters = []
  
  if (props.activeQuickFilter && props.activeQuickFilter !== 'all') {
    const filterLabels = {
      recent: '最近使用',
      starred: '已收藏',
      mine: '我创建的'
    }
    filters.push({
      key: 'quickFilter',
      label: filterLabels[props.activeQuickFilter as keyof typeof filterLabels] || props.activeQuickFilter
    })
  }
  
  if (props.activeSubjectFilter) {
    filters.push({
      key: 'subject',
      label: `学科: ${props.activeSubjectFilter}`
    })
  }
  
  if (props.activeGradeFilter) {
    filters.push({
      key: 'grade',
      label: `年级: ${props.activeGradeFilter}`
    })
  }
  
  if (props.activeStatusFilter) {
    filters.push({
      key: 'status',
      label: `状态: ${props.activeStatusFilter}`
    })
  }
  
  return filters
})

// 排序选项
const sortOptions = [
  { value: 'name', label: '名称', icon: Reading },
  { value: 'createdAt', label: '创建时间', icon: Calendar },
  { value: 'documentCount', label: '文档数量', icon: Document },
  { value: 'knowledgePointCount', label: '知识点数量', icon: DataAnalysis }
]

// 排序方向标题
const sortOrderTitle = computed(() => {
  const sortLabel = sortOptions.find(opt => opt.value === props.sortBy)?.label || '当前字段'
  return props.sortOrder === 'asc' ? `${sortLabel}升序` : `${sortLabel}降序`
})

// 事件处理
const handleSelectAll = (val: any) => {
  const checked = typeof val === 'boolean' ? val : !!val
  emit('select-all', checked)
}

const handleBatchAction = (action: string) => {
  emit('batch-action', action)
}

const handleRemoveFilter = (filterKey: string) => {
  emit('remove-filter', filterKey)
}

const handleSortChange = (sortBy: string) => {
  emit('sort-change', sortBy)
}

const toggleSortOrder = () => {
  const newOrder = props.sortOrder === 'asc' ? 'desc' : 'asc'
  emit('sort-order-change', newOrder)
}

const handleViewModeChange = (mode: 'grid' | 'list') => {
  emit('view-mode-change', mode)
}
</script>

<style scoped>
.enhanced-sort-filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: #fafbfc;
  border-bottom: 1px solid #e4e7ed;
  min-height: 56px;
  transition: all 0.3s ease;
}

.left-section {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  min-width: 0;
}

.selection-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.select-all-checkbox {
  font-weight: 500;
}

.batch-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  animation: slideInLeft 0.3s ease;
}

.selection-tag {
  font-size: 12px;
  font-weight: 500;
}

.stats-info {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 14px;
  font-weight: 500;
}

.stats-icon {
  color: #409eff;
  font-size: 16px;
}

.filtered-indicator {
  color: #67c23a;
  font-weight: 600;
}

.right-section {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.quick-filters {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  font-size: 12px;
  background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
  border: none;
  border-radius: 16px;
  color: white;
  transition: all 0.3s ease;
}

.filter-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

.remove-icon {
  font-size: 12px;
  margin-left: 4px;
}

.sort-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-select {
  width: 140px;
}

.sort-option-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-order-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.sort-order-btn:hover {
  transform: translateY(-1px);
  color: #409eff;
}

.view-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.view-controls .el-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.view-controls .el-button:hover {
  transform: translateY(-1px);
}

/* 动画 */
@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 深层样式覆盖 */
:deep(.el-checkbox) {
  margin-right: 0;
}

:deep(.el-checkbox__label) {
  font-weight: 500;
  color: #606266;
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #409eff;
  border-color: #409eff;
}

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

:deep(.el-button-group .el-button) {
  border-radius: 0;
  transition: all 0.3s ease;
}

:deep(.el-button-group .el-button:first-child) {
  border-top-left-radius: 6px;
  border-bottom-left-radius: 6px;
}

:deep(.el-button-group .el-button:last-child) {
  border-top-right-radius: 6px;
  border-bottom-right-radius: 6px;
}

:deep(.el-button-group .el-button:hover) {
  z-index: 1;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .enhanced-sort-filter-bar {
    padding: 10px 16px;
    flex-wrap: wrap;
    gap: 12px;
  }
  
  .left-section {
    flex: 1 1 auto;
    min-width: 200px;
  }
  
  .right-section {
    flex: 1 1 auto;
    justify-content: flex-end;
  }
  
  .quick-filters {
    display: none; /* 小屏幕隐藏筛选标签 */
  }
}

@media (max-width: 768px) {
  .enhanced-sort-filter-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  
  .left-section,
  .right-section {
    flex: 1;
    justify-content: space-between;
  }
  
  .batch-actions {
    flex-direction: column;
    gap: 8px;
  }
  
  .batch-actions .el-button-group {
    width: 100%;
  }
  
  .sort-select {
    width: 120px;
  }
}
</style>