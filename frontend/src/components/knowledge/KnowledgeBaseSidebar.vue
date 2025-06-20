<template>
  <div class="sidebar">
    <!-- 收藏夹 -->
    <div class="sidebar-section">
      <div class="section-header">
        <h4>
          <el-icon><StarFilled /></el-icon>
          收藏夹
        </h4>
        <el-button 
          v-if="favoriteKnowledgeBases.length > 0"
          type="text" 
          size="small" 
          @click="clearFavorites"
        >
          清空
        </el-button>
      </div>
      <div class="section-content">
        <div v-if="favoriteKnowledgeBases.length === 0" class="empty-state">
          <el-icon><StarFilled /></el-icon>
          <p>暂无收藏</p>
          <span>点击知识库卡片上的星星来收藏</span>
        </div>
        <div 
          v-for="kb in favoriteKnowledgeBases" 
          :key="`fav-${kb.id}`"
          class="favorite-item"
          @click="$emit('select-knowledge-base', kb)"
        >
          <div class="favorite-info">
            <div class="favorite-name">{{ kb.name }}</div>
            <div class="favorite-meta">{{ kb.gradeLevel }} · {{ kb.subject }}</div>
          </div>
          <el-button 
            type="text" 
            size="small" 
            @click.stop="$emit('toggle-favorite', kb)"
            class="unfavorite-btn"
          >
            <el-icon><StarFilled /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <!-- 分类浏览 -->
    <div class="sidebar-section">
      <div class="section-header">
        <h4>
          <el-icon><FolderOpened /></el-icon>
          分类浏览
        </h4>
      </div>
      <div class="section-content">
        <!-- 按学科分类 -->
        <div class="category-group">
          <div class="category-title" @click="toggleCategory('subject')">
            <el-icon>
              <ArrowRight v-if="!categoryExpanded.subject" />
              <ArrowDown v-else />
            </el-icon>
            按学科
          </div>
          <div v-show="categoryExpanded.subject" class="category-items">
            <div 
              v-for="subject in subjectCategories" 
              :key="subject.name"
              class="category-item"
              :class="{ active: selectedCategory === `subject:${subject.name}` }"
              @click="selectCategory('subject', subject.name)"
            >
              <span class="category-name">{{ subject.name }}</span>
              <span class="category-count">({{ subject.count }})</span>
            </div>
          </div>
        </div>

        <!-- 按年级分类 -->
        <div class="category-group">
          <div class="category-title" @click="toggleCategory('grade')">
            <el-icon>
              <ArrowRight v-if="!categoryExpanded.grade" />
              <ArrowDown v-else />
            </el-icon>
            按年级
          </div>
          <div v-show="categoryExpanded.grade" class="category-items">
            <div 
              v-for="grade in gradeCategories" 
              :key="grade.name"
              class="category-item"
              :class="{ active: selectedCategory === `grade:${grade.name}` }"
              @click="selectCategory('grade', grade.name)"
            >
              <span class="category-name">{{ grade.name }}</span>
              <span class="category-count">({{ grade.count }})</span>
            </div>
          </div>
        </div>

        <!-- 按状态分类 -->
        <div class="category-group">
          <div class="category-title" @click="toggleCategory('status')">
            <el-icon>
              <ArrowRight v-if="!categoryExpanded.status" />
              <ArrowDown v-else />
            </el-icon>
            按状态
          </div>
          <div v-show="categoryExpanded.status" class="category-items">
            <div 
              v-for="status in statusCategories" 
              :key="status.name"
              class="category-item"
              :class="{ active: selectedCategory === `status:${status.value}` }"
              @click="selectCategory('status', status.value || status.name)"
            >
              <div class="status-item">
                <div class="status-indicator" :class="status.value"></div>
                <span class="category-name">{{ status.name }}</span>
              </div>
              <span class="category-count">({{ status.count }})</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 最近访问 -->
    <div class="sidebar-section">
      <div class="section-header">
        <h4>
          <el-icon><Clock /></el-icon>
          最近访问
        </h4>
        <el-button 
          v-if="recentKnowledgeBases.length > 0"
          type="text" 
          size="small" 
          @click="clearRecent"
        >
          清空
        </el-button>
      </div>
      <div class="section-content">
        <div v-if="recentKnowledgeBases.length === 0" class="empty-state">
          <el-icon><Clock /></el-icon>
          <p>暂无最近访问</p>
        </div>
        <div 
          v-for="kb in recentKnowledgeBases" 
          :key="`recent-${kb.id}`"
          class="recent-item"
          @click="$emit('select-knowledge-base', kb)"
        >
          <div class="recent-info">
            <div class="recent-name">{{ kb.name }}</div>
            <div class="recent-meta">{{ kb.gradeLevel }} · {{ kb.subject }}</div>
            <div class="recent-time">{{ formatDate(kb.lastAccessTime) }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="sidebar-section">
      <div class="section-header">
        <h4>
          <el-icon><Lightning /></el-icon>
          快捷操作
        </h4>
      </div>
      <div class="section-content">
        <div class="quick-actions">
          <el-button 
            type="primary" 
            size="small" 
            style="width: 100%; margin-bottom: 8px;"
            @click="$emit('create-knowledge-base')"
            icon="Plus"
          >
            创建知识库
          </el-button>
          <el-button 
            type="default" 
            size="small" 
            style="width: 100%; margin-bottom: 8px;"
            @click="$emit('batch-upload')"
            icon="Upload"
          >
            批量上传
          </el-button>
          <el-button 
            type="default" 
            size="small" 
            style="width: 100%;"
            @click="$emit('export-data')"
            icon="Download"
          >
            导出数据
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'
import { 
  StarFilled, 
  FolderOpened, 
  Clock, 
  Lightning,
  ArrowRight, 
  ArrowDown 
} from '@element-plus/icons-vue'

// 导入统一的类型定义
import type { KnowledgeBase as BaseKnowledgeBase } from '@/api/knowledge'

// 扩展类型定义
interface KnowledgeBase extends BaseKnowledgeBase {
  status?: 'draft' | 'published' | 'archived'
  lastAccessTime?: string
}

interface CategoryItem {
  name: string
  count: number
  value?: string
}

interface Props {
  favoriteKnowledgeBases: KnowledgeBase[]
  recentKnowledgeBases: KnowledgeBase[]
  subjectCategories: CategoryItem[]
  gradeCategories: CategoryItem[]
  statusCategories: CategoryItem[]
  selectedCategory?: string
}

interface Emits {
  (e: 'select-knowledge-base', kb: KnowledgeBase): void
  (e: 'toggle-favorite', kb: KnowledgeBase): void
  (e: 'clear-favorites'): void
  (e: 'clear-recent'): void
  (e: 'select-category', type: string, value: string): void
  (e: 'create-knowledge-base'): void
  (e: 'batch-upload'): void
  (e: 'export-data'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const categoryExpanded = reactive({
  subject: true,
  grade: true,
  status: true
})

const toggleCategory = (type: string) => {
  if (type in categoryExpanded) {
    categoryExpanded[type as keyof typeof categoryExpanded] = !categoryExpanded[type as keyof typeof categoryExpanded]
  }
}

const selectCategory = (type: string, value: string) => {
  emit('select-category', type, value)
}

const clearFavorites = () => {
  emit('clear-favorites')
}

const clearRecent = () => {
  emit('clear-recent')
}

const formatDate = (dateString?: string) => {
  if (!dateString) return ''
  try {
    const date = new Date(dateString)
    return formatDistanceToNow(date, { addSuffix: true, locale: zhCN })
  } catch {
    return ''
  }
}
</script>

<style scoped>
.sidebar {
  width: 260px;
  background: #f8f9fa;
  border-right: 1px solid #e4e7ed;
  padding: 20px 0;
  overflow-y: auto;
  height: 100%;
}

.sidebar-section {
  margin-bottom: 24px;
  padding: 0 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 6px;
}

.section-content {
  display: flex;
  flex-direction: column;
}

/* 收藏夹样式 */
.favorite-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.favorite-item:hover {
  background: #e6f7ff;
}

.favorite-info {
  flex: 1;
  min-width: 0;
}

.favorite-name {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.favorite-meta {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

.unfavorite-btn {
  color: #f56c6c;
  padding: 2px 4px;
}

/* 分类浏览样式 */
.category-group {
  margin-bottom: 16px;
}

.category-title {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
  color: #606266;
  cursor: pointer;
  padding: 4px 0;
  transition: color 0.2s;
}

.category-title:hover {
  color: #409eff;
}

.category-items {
  margin-left: 16px;
  margin-top: 4px;
}

.category-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 12px;
}

.category-item:hover {
  background: #e6f7ff;
}

.category-item.active {
  background: #409eff;
  color: white;
}

.category-name {
  color: #606266;
  flex: 1;
}

.category-item.active .category-name {
  color: white;
}

.category-count {
  color: #909399;
  font-size: 11px;
}

.category-item.active .category-count {
  color: rgba(255, 255, 255, 0.8);
}

.status-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-indicator {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.status-indicator.published {
  background: #67c23a;
}

.status-indicator.draft {
  background: #e6a23c;
}

.status-indicator.archived {
  background: #909399;
}

/* 最近访问样式 */
.recent-item {
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
  margin-bottom: 4px;
}

.recent-item:hover {
  background: #e6f7ff;
}

.recent-info {
  width: 100%;
}

.recent-name {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-meta {
  font-size: 11px;
  color: #909399;
  margin: 2px 0;
}

.recent-time {
  font-size: 10px;
  color: #c0c4cc;
}

/* 空状态样式 */
.empty-state {
  text-align: center;
  padding: 20px 12px;
  color: #909399;
}

.empty-state .el-icon {
  font-size: 24px;
  margin-bottom: 8px;
  opacity: 0.5;
}

.empty-state p {
  margin: 0 0 4px 0;
  font-size: 13px;
}

.empty-state span {
  font-size: 11px;
  opacity: 0.8;
}

/* 快捷操作样式 */
.quick-actions {
  display: flex;
  flex-direction: column;
}
</style>
