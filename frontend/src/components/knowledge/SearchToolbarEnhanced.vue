<template>
  <div class="enhanced-search-area">
    <el-input
      v-model="searchText"
      :placeholder="placeholder"
      :prefix-icon="Search"
      clearable
      size="small"
      style="width: 280px; margin-right: 8px;"
      @input="handleInput"
      @clear="handleClear"
      @keyup.enter="handleSearch"
    />
    
    <!-- 搜索模式切换 -->
    <div class="search-mode-toggle" style="margin-right: 12px;">
      <el-button-group>
        <el-button 
          :type="searchMode === 'basic' ? 'primary' : ''" 
          @click="setSearchMode('basic')"
          size="small"
          class="search-mode-btn"
          :title="'基础搜索：按知识库名称、学科、年级等条件搜索'"
        >
          <el-icon><Search /></el-icon>
          <span>基础搜索</span>
        </el-button>
        <el-button 
          :type="searchMode === 'smart' ? 'primary' : ''" 
          @click="setSearchMode('smart')"
          size="small"
          class="search-mode-btn"
          :title="'AI智能搜索：根据语义理解查找最相关的内容'"
        >
          <el-icon><MagicStick /></el-icon>
          <span>AI智能搜索</span>
        </el-button>
      </el-button-group>
    </div>
    
    <!-- 快速筛选下拉菜单 -->
    <el-dropdown trigger="click" @command="handleQuickFilter">
      <el-button size="small" style="margin-right: 12px;">
        筛选 <el-icon><ArrowDown /></el-icon>
      </el-button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item divided>
            <span style="font-weight: bold;">学科筛选</span>
          </el-dropdown-item>
          <el-dropdown-item command="subject:">全部学科</el-dropdown-item>
          <el-dropdown-item 
            v-for="subject in subjects" 
            :key="subject"
            :command="`subject:${subject}`"
          >
            {{ subject }}
          </el-dropdown-item>
          <el-dropdown-item divided>
            <span style="font-weight: bold;">年级筛选</span>
          </el-dropdown-item>
          <el-dropdown-item command="grade:">全部年级</el-dropdown-item>
          <el-dropdown-item 
            v-for="grade in gradeLevels" 
            :key="grade"
            :command="`grade:${grade}`"
          >
            {{ grade }}
          </el-dropdown-item>
          <el-dropdown-item divided>
            <span style="font-weight: bold;">状态筛选</span>
          </el-dropdown-item>
          <el-dropdown-item command="status:">全部状态</el-dropdown-item>
          <el-dropdown-item command="status:published">已发布</el-dropdown-item>
          <el-dropdown-item command="status:draft">草稿</el-dropdown-item>
          <el-dropdown-item command="status:archived">已归档</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
    
    <el-button size="small" @click="showAdvancedSearch" icon="Setting">
      高级搜索
    </el-button>

    <!-- 工具栏按钮 -->
    <div class="toolbar-buttons" style="margin-left: 16px;">
      <el-button type="primary" icon="Plus" @click="$emit('create-knowledge-base')">
        创建知识库
      </el-button>
      <el-button icon="Upload" @click="$emit('batch-upload')">
        批量上传
      </el-button>
      <el-dropdown @command="handleToolbarAction">
        <el-button icon="MoreFilled" />
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="refresh" icon="Refresh">刷新</el-dropdown-item>
            <el-dropdown-item command="export" icon="Download">导出数据</el-dropdown-item>
            <el-dropdown-item command="import" icon="Upload">导入数据</el-dropdown-item>
            <el-dropdown-item divided command="settings" icon="Setting">设置</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { debounce } from 'lodash-es'
import { 
  Search, 
  MagicStick, 
  ArrowDown, 
  Setting, 
  Plus, 
  Upload, 
  MoreFilled 
} from '@element-plus/icons-vue'

interface Props {
  subjects: string[]
  gradeLevels: string[]
  searchMode: 'basic' | 'smart'
}

interface Emits {
  (e: 'search', query: string): void
  (e: 'quick-filter', filter: string): void
  (e: 'mode-change', mode: 'basic' | 'smart'): void
  (e: 'advanced-search'): void
  (e: 'clear'): void
  (e: 'create-knowledge-base'): void
  (e: 'batch-upload'): void
  (e: 'toolbar-action', action: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const searchText = ref('')

const placeholder = computed(() => {
  return props.searchMode === 'basic' 
    ? '🔍 基础搜索：输入知识库名称、关键词进行快速查找'
    : '🧠 AI智能搜索：描述您要找的内容，如"高一数学函数的应用题"、"初中物理力学概念"等'
})

// 防抖搜索
const debouncedSearch = debounce((query: string) => {
  if (query.trim()) {
    emit('search', query)
  }
}, 300)

const handleInput = (value: string) => {
  debouncedSearch(value)
}

const handleSearch = () => {
  if (searchText.value.trim()) {
    emit('search', searchText.value)
  }
}

const handleClear = () => {
  searchText.value = ''
  emit('clear')
}

const setSearchMode = (mode: 'basic' | 'smart') => {
  emit('mode-change', mode)
}

const handleQuickFilter = (command: string) => {
  emit('quick-filter', command)
}

const showAdvancedSearch = () => {
  emit('advanced-search')
}

const handleToolbarAction = (action: string) => {
  emit('toolbar-action', action)
}
</script>

<style scoped>
.enhanced-search-area {
  display: flex;
  align-items: center;
  width: 100%;
}

.search-mode-toggle {
  display: flex;
  align-items: center;
}

.search-mode-btn {
  font-size: 12px;
  padding: 8px 12px;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.search-mode-btn span {
  margin-left: 4px;
}

.search-mode-btn:hover {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.search-mode-btn .el-icon {
  font-size: 14px;
}

.search-mode-btn:first-child {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
  border-right: 1px solid #dee2e6;
}

.search-mode-btn:last-child {
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
}

.toolbar-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.el-button-group) {
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border-radius: 6px;
  overflow: hidden;
}

:deep(.el-button-group .el-button:first-child) {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}

:deep(.el-button-group .el-button:last-child) {
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
}

:deep(.el-button-group .el-button + .el-button) {
  margin-left: -1px;
}
</style>
