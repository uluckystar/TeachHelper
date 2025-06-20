<template>
  <div class="sidebar">
    <!-- 快速访问 -->
    <div class="sidebar-section">
      <h4>快速访问</h4>
      <ul class="quick-access-list">
        <li 
          class="access-item" 
          :class="{ active: activeFilter === 'all' }" 
          @click="setFilter('all')"
        >
          <el-icon><Collection /></el-icon>
          <span>全部知识库</span>
          <span class="count">{{ totalCount }}</span>
        </li>
        <li 
          class="access-item" 
          :class="{ active: activeFilter === 'recent' }" 
          @click="setFilter('recent')"
        >
          <el-icon><Timer /></el-icon>
          <span>最近使用</span>
        </li>
        <li 
          class="access-item" 
          :class="{ active: activeFilter === 'starred' }" 
          @click="setFilter('starred')"
        >
          <el-icon><Star /></el-icon>
          <span>已收藏</span>
        </li>
        <li 
          class="access-item" 
          :class="{ active: activeFilter === 'mine' }" 
          @click="setFilter('mine')"
        >
          <el-icon><User /></el-icon>
          <span>我创建的</span>
        </li>
      </ul>
    </div>
    
    <!-- 按学科分类 -->
    <div class="sidebar-section">
      <h4>按学科分类</h4>
      <ul class="category-list">
        <li 
          v-for="subject in subjects" 
          :key="subject"
          class="category-item"
          :class="{ active: activeSubject === subject }"
          @click="setSubjectFilter(subject)"
        >
          <el-icon><FolderOpened /></el-icon>
          <span>{{ subject }}</span>
        </li>
      </ul>
    </div>

    <!-- 高级搜索 -->
    <div class="sidebar-section">
      <h4>高级搜索</h4>
      <el-button 
        type="primary" 
        size="small" 
        @click="$emit('advanced-search')" 
        style="width: 100%;"
      >
        <el-icon><Search /></el-icon>
        智能搜索
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { 
  Collection, 
  Timer, 
  Star, 
  User, 
  FolderOpened, 
  Search 
} from '@element-plus/icons-vue'

interface Props {
  subjects: string[]
  totalCount: number
  activeFilter?: string
  activeSubject?: string
}

interface Emits {
  (e: 'filter-change', filter: string): void
  (e: 'subject-filter', subject: string): void
  (e: 'advanced-search'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const setFilter = (filter: string) => {
  emit('filter-change', filter)
}

const setSubjectFilter = (subject: string) => {
  emit('subject-filter', subject)
}
</script>

<style scoped>
/* 侧边栏样式 - 完整复刻原始设计 */
.sidebar {
  width: 260px;
  background: #f8f9fa;
  border-right: 1px solid #e4e7ed;
  padding: 20px 0;
  overflow-y: auto;
}

.sidebar-section {
  margin-bottom: 24px;
  padding: 0 20px;
}

.sidebar-section h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.quick-access-list,
.category-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.access-item,
.category-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
  margin-bottom: 4px;
  font-size: 14px;
}

.access-item:hover,
.category-item:hover {
  background: #e9ecef;
}

.access-item.active,
.category-item.active {
  background: #409eff;
  color: #fff;
}

.access-item .count {
  margin-left: auto;
  background: #e9ecef;
  color: #606266;
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 12px;
}

.access-item.active .count {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}
</style>
