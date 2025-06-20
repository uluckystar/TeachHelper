<template>
  <!-- 搜索状态提示条 -->
  <div class="search-status-bar" v-if="hasActiveSearch">
    <div class="search-status-content">
      <div class="search-info">
        <span class="search-mode-indicator">
          <el-icon>
            <MagicStick v-if="searchMode === 'smart'" />
            <Search v-else />
          </el-icon>
          <span class="mode-text">
            {{ searchMode === 'smart' ? 'AI智能搜索' : '基础搜索' }}
          </span>
        </span>
        <span v-if="searchQuery" class="search-keyword">
          关键词: "{{ searchQuery }}"
        </span>
        <span v-if="activeSubjectFilter" class="search-filter">
          <el-icon><Reading /></el-icon>
          学科: {{ activeSubjectFilter }}
        </span>
        <span v-if="activeGradeFilter" class="search-filter">
          <el-icon><School /></el-icon>
          年级: {{ activeGradeFilter }}
        </span>
        <span v-if="activeStatusFilter" class="search-filter">
          <el-icon><Flag /></el-icon>
          状态: {{ getStatusLabel(activeStatusFilter) }}
        </span>
      </div>
      <div class="search-actions">
        <span class="result-count">
          <el-icon><Document /></el-icon>
          共找到 {{ totalResults }} 个知识库
        </span>
        <el-button 
          v-if="searchMode === 'smart' && hasVectorResults" 
          size="small" 
          link 
          @click="$emit('show-vector-results')"
          class="action-btn"
        >
          <el-icon><View /></el-icon>
          查看AI搜索详情
        </el-button>
        <el-button 
          size="small" 
          link 
          @click="$emit('clear-all-search')"
          class="action-btn clear-btn"
        >
          <el-icon><RefreshLeft /></el-icon>
          清除所有搜索
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { 
  Search, 
  MagicStick, 
  Reading, 
  School, 
  Flag, 
  Document, 
  View, 
  RefreshLeft 
} from '@element-plus/icons-vue'

interface Props {
  searchMode: 'basic' | 'smart'
  searchQuery?: string
  activeSubjectFilter?: string
  activeGradeFilter?: string
  activeStatusFilter?: string
  totalResults: number
  hasVectorResults?: boolean
}

interface Emits {
  (e: 'show-vector-results'): void
  (e: 'clear-all-search'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const hasActiveSearch = computed(() => {
  return !!(
    props.searchQuery || 
    props.activeSubjectFilter || 
    props.activeGradeFilter || 
    props.activeStatusFilter
  )
})

const getStatusLabel = (status: string) => {
  const statusMap: Record<string, string> = {
    'published': '已发布',
    'draft': '草稿',
    'archived': '已归档'
  }
  return statusMap[status] || status
}
</script>

<style scoped>
.search-status-bar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 14px 20px;
  border-radius: 10px;
  margin-bottom: 16px;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
  backdrop-filter: blur(10px);
}

.search-status-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.search-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.search-mode-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
}

.mode-text {
  font-weight: 600;
}

.search-keyword {
  background: rgba(255, 255, 255, 0.15);
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
}

.search-filter {
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(255, 255, 255, 0.1);
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 12px;
}

.search-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.result-count {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  opacity: 0.9;
}

.search-actions .action-btn {
  color: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
  padding: 6px 12px;
  font-size: 12px;
}

.search-actions .action-btn:hover {
  color: white;
  border-color: rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-1px);
}

.clear-btn:hover {
  background: rgba(255, 107, 107, 0.3) !important;
  border-color: rgba(255, 107, 107, 0.5) !important;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .search-status-content {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  
  .search-info {
    justify-content: center;
  }
  
  .search-actions {
    justify-content: center;
  }
}
</style>
