<template>
  <div class="knowledge-base-list">
    <!-- 网格视图 -->
    <div v-if="viewMode === 'grid'" class="grid-view">
      <div class="grid-container">
        <div
          v-for="kb in knowledgeBases"
          :key="kb.id"
          class="knowledge-base-card"
          @click="$emit('select', kb)"
        >
          <div class="card-header">
            <div class="status-indicator" :class="kb.status"></div>
            <el-dropdown trigger="click" @command="(command) => handleAction(command, kb)">
              <el-button type="text" class="action-btn">
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit" icon="Edit">编辑</el-dropdown-item>
                  <el-dropdown-item command="upload" icon="Upload">上传文档</el-dropdown-item>
                  <el-dropdown-item command="generate" icon="MagicStick">AI出题</el-dropdown-item>
                  <el-dropdown-item command="favorite" :icon="kb.isFavorited ? 'StarFilled' : 'Star'">
                    {{ kb.isFavorited ? '取消收藏' : '收藏' }}
                  </el-dropdown-item>
                  <el-dropdown-item divided command="delete" icon="Delete" class="danger-item">
                    删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>

          <div class="card-content">
            <h3 class="kb-title">{{ kb.name }}</h3>
            <p class="kb-description">{{ kb.description || '暂无描述' }}</p>
            
            <div class="kb-meta">
              <div class="meta-item">
                <el-icon><School /></el-icon>
                <span>{{ kb.gradeLevel }}</span>
              </div>
              <div class="meta-item">
                <el-icon><Reading /></el-icon>
                <span>{{ kb.subject }}</span>
              </div>
            </div>

            <div class="kb-stats">
              <div class="stat-item">
                <span class="stat-number">{{ kb.documentCount || 0 }}</span>
                <span class="stat-label">文档</span>
              </div>
              <div class="stat-item">
                <span class="stat-number">{{ kb.knowledgePointCount || 0 }}</span>
                <span class="stat-label">知识点</span>
              </div>
              <div class="stat-item">
                <span class="stat-number">{{ kb.questionCount || 0 }}</span>
                <span class="stat-label">题目</span>
              </div>
            </div>

            <div class="kb-tags" v-if="kb.tags && kb.tags.length > 0">
              <el-tag
                v-for="tag in kb.tags.slice(0, 3)"
                :key="tag"
                size="small"
                class="tag-item"
              >
                {{ tag }}
              </el-tag>
              <el-tag v-if="kb.tags.length > 3" size="small" type="info">
                +{{ kb.tags.length - 3 }}
              </el-tag>
            </div>
          </div>

          <div class="card-footer">
            <span class="update-time">
              更新于 {{ formatDate(kb.updatedAt) }}
            </span>
            <el-icon v-if="kb.isFavorited" class="favorite-icon"><StarFilled /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- 列表视图 -->
    <div v-else class="list-view">
      <el-table
        :data="knowledgeBases"
        style="width: 100%"
        @row-click="(row) => $emit('select', row)"
      >
        <el-table-column width="60">
          <template #default="{ row }">
            <div class="status-indicator" :class="row.status"></div>
          </template>
        </el-table-column>

        <el-table-column prop="name" label="知识库名称" min-width="200">
          <template #default="{ row }">
            <div class="list-title">
              <span class="title-text">{{ row.name }}</span>
              <el-icon v-if="row.isFavorited" class="favorite-icon"><StarFilled /></el-icon>
            </div>
            <div class="list-description">{{ row.description || '暂无描述' }}</div>
          </template>
        </el-table-column>

        <el-table-column prop="gradeLevel" label="年级" width="100" />
        <el-table-column prop="subject" label="学科" width="120" />

        <el-table-column label="统计" width="200">
          <template #default="{ row }">
            <div class="stats-inline">
              <span class="stat-inline">{{ row.documentCount || 0 }}文档</span>
              <span class="stat-inline">{{ row.knowledgePointCount || 0 }}知识点</span>
              <span class="stat-inline">{{ row.questionCount || 0 }}题目</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="标签" width="150">
          <template #default="{ row }">
            <div class="tags-inline" v-if="row.tags && row.tags.length > 0">
              <el-tag
                v-for="tag in row.tags.slice(0, 2)"
                :key="tag"
                size="small"
                class="tag-inline"
              >
                {{ tag }}
              </el-tag>
              <el-tag v-if="row.tags.length > 2" size="small" type="info">
                +{{ row.tags.length - 2 }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="updatedAt" label="更新时间" width="120">
          <template #default="{ row }">
            {{ formatDate(row.updatedAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button type="primary" size="small" @click.stop="handleAction('edit', row)">
                编辑
              </el-button>
              <el-dropdown trigger="click" @command="(command) => handleAction(command, row)">
                <el-button type="primary" size="small">
                  更多<el-icon><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="upload" icon="Upload">上传文档</el-dropdown-item>
                    <el-dropdown-item command="generate" icon="MagicStick">AI出题</el-dropdown-item>
                    <el-dropdown-item command="favorite" :icon="row.isFavorited ? 'StarFilled' : 'Star'">
                      {{ row.isFavorited ? '取消收藏' : '收藏' }}
                    </el-dropdown-item>
                    <el-dropdown-item divided command="delete" icon="Delete" class="danger-item">
                      删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        :page-sizes="[20, 50, 100, 200]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'
import { 
  MoreFilled, 
  School, 
  Reading, 
  StarFilled, 
  ArrowDown 
} from '@element-plus/icons-vue'

// 导入统一的类型定义
import type { KnowledgeBase as BaseKnowledgeBase } from '@/api/knowledge'

// 扩展类型定义
interface KnowledgeBase extends BaseKnowledgeBase {
  status?: 'draft' | 'published' | 'archived'
  tags?: string[]
  questionCount?: number
  lastAccessTime?: string
}

interface Props {
  knowledgeBases: KnowledgeBase[]
  viewMode: 'grid' | 'list'
  total: number
  currentPage: number
  pageSize: number
}

interface Emits {
  (e: 'select', kb: KnowledgeBase): void
  (e: 'action', action: string, kb: KnowledgeBase): void
  (e: 'page-change'): void
  (e: 'update:current-page', page: number): void
  (e: 'update:page-size', size: number): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const formatDate = (dateString?: string) => {
  if (!dateString) return '未知'
  try {
    const date = new Date(dateString)
    return formatDistanceToNow(date, { addSuffix: true, locale: zhCN })
  } catch {
    return '未知'
  }
}

const handleAction = (action: string, kb: KnowledgeBase) => {
  emit('action', action, kb)
}

const handleSizeChange = (size: number) => {
  emit('update:page-size', size)
  emit('page-change')
}

const handleCurrentChange = (page: number) => {
  emit('update:current-page', page)
  emit('page-change')
}
</script>

<style scoped>
.knowledge-base-list {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 网格视图样式 */
.grid-view {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.knowledge-base-card {
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
  height: 280px;
  display: flex;
  flex-direction: column;
}

.knowledge-base-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 16px 8px;
}

.status-indicator {
  width: 8px;
  height: 8px;
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

.action-btn {
  padding: 4px 8px;
  color: #909399;
}

.action-btn:hover {
  color: #409eff;
}

.card-content {
  flex: 1;
  padding: 0 16px;
  display: flex;
  flex-direction: column;
}

.kb-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-description {
  font-size: 14px;
  color: #606266;
  margin: 0 0 12px 0;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
  flex: 1;
}

.kb-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.kb-stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 12px;
  padding: 8px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
}

.stat-item {
  text-align: center;
}

.stat-number {
  display: block;
  font-size: 18px;
  font-weight: 600;
  color: #409eff;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.kb-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.tag-item {
  font-size: 10px;
}

.card-footer {
  padding: 8px 16px;
  background: #f8f9fa;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
}

.favorite-icon {
  color: #f56c6c;
  font-size: 14px;
}

/* 列表视图样式 */
.list-view {
  flex: 1;
  overflow-y: auto;
}

.list-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-text {
  font-weight: 600;
  color: #303133;
}

.list-description {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stats-inline {
  display: flex;
  gap: 8px;
}

.stat-inline {
  font-size: 12px;
  color: #606266;
  background: #f0f9ff;
  padding: 2px 6px;
  border-radius: 3px;
}

.tags-inline {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.tag-inline {
  font-size: 10px;
}

/* 分页样式 */
.pagination-wrapper {
  padding: 20px;
  border-top: 1px solid #e4e7ed;
  background: white;
  display: flex;
  justify-content: center;
}

/* 危险操作样式 */
:deep(.danger-item) {
  color: #f56c6c !important;
}

:deep(.danger-item:hover) {
  background: #fef0f0 !important;
}
</style>
