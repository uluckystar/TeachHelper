<template>
  <div class="knowledge-base-detail">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="breadcrumb-nav">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>
              <router-link to="/knowledge">知识库</router-link>
            </el-breadcrumb-item>
            <el-breadcrumb-item>{{ knowledgeBase?.name || '详情' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-actions">
          <el-button 
            :icon="knowledgeBase?.isFavorited ? StarFilled : Star"
            @click="toggleFavorite"
            :loading="favoriteLoading"
          >
            {{ knowledgeBase?.isFavorited ? '已收藏' : '收藏' }}
          </el-button>
          <el-button icon="Upload" @click="showUploadDialog = true">
            上传文档
          </el-button>
          <el-button type="primary" icon="MagicStick" @click="showAIQuestionDialog = true">
            AI出题
          </el-button>
          <el-dropdown @command="handleMoreAction">
            <el-button icon="MoreFilled">
              更多 <el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="edit" icon="Edit">编辑知识库</el-dropdown-item>
                <el-dropdown-item command="export" icon="Download">导出数据</el-dropdown-item>
                <el-dropdown-item divided command="delete" icon="Delete">删除知识库</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>

    <!-- 知识库信息区域 -->
    <div class="kb-info-section" v-if="knowledgeBase">
      <div class="kb-info-card">
        <div class="kb-basic-info">
          <div class="kb-icon">
            <el-icon size="48"><Collection /></el-icon>
          </div>
          <div class="kb-details">
            <h1 class="kb-title">{{ knowledgeBase.name }}</h1>
            <p class="kb-description" v-if="knowledgeBase.description">
              {{ knowledgeBase.description }}
            </p>
            <div class="kb-meta">
              <el-tag v-if="knowledgeBase.subject" size="large">{{ knowledgeBase.subject }}</el-tag>
              <el-tag v-if="knowledgeBase.gradeLevel" size="large" type="success">{{ knowledgeBase.gradeLevel }}</el-tag>
              <span class="create-time">创建于 {{ formatDate(knowledgeBase.createdAt) }}</span>
            </div>
          </div>
        </div>
        
        <div class="kb-stats">
          <div class="stat-item">
            <div class="stat-value">{{ knowledgeBase.documentCount || 0 }}</div>
            <div class="stat-label">文档</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ knowledgeBase.knowledgePointCount || 0 }}</div>
            <div class="stat-label">知识点</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ knowledgeBase.favoriteCount || 0 }}</div>
            <div class="stat-label">收藏</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <!-- 文档管理 -->
        <el-tab-pane label="文档管理" name="documents">
          <DocumentManagement 
            :knowledge-base-id="knowledgeBaseId"
            @document-uploaded="handleDocumentUploaded"
          />
        </el-tab-pane>
        
        <!-- 知识点 -->
        <el-tab-pane label="知识点" name="knowledge-points">
          <KnowledgePointManagement 
            :knowledge-base-id="knowledgeBaseId"
          />
        </el-tab-pane>
        
        <!-- 题目 -->
        <el-tab-pane label="题目" name="questions">
          <GeneratedQuestions 
            :knowledge-base-id="knowledgeBaseId"
          />
        </el-tab-pane>
        
        <!-- 设置 -->
        <el-tab-pane label="设置" name="settings">
          <KnowledgeBaseSettings 
            :knowledge-base="knowledgeBase"
            @updated="handleKnowledgeBaseUpdated"
          />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 文档上传对话框 -->
    <DocumentUploadDialog
      v-model="showUploadDialog"
      :knowledge-base-id="knowledgeBase?.id || 0"
      @uploaded="handleDocumentUploaded"
    />

    <!-- AI出题对话框 -->
    <AIQuestionGenerationDialog
      v-model="showAIQuestionDialog"
      :knowledge-base="knowledgeBase || { id: 0, name: '', description: '' }"
      @generated="handleQuestionsGenerated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Collection,
  Star,
  StarFilled,
  Upload,
  MagicStick,
  MoreFilled,
  ArrowDown,
  Edit,
  Download,
  Delete
} from '@element-plus/icons-vue'
import * as knowledgeBaseApi from '@/api/knowledge'
import type { KnowledgeBase } from '@/api/knowledge'
import DocumentManagement from '@/components/knowledge/DocumentManagement.vue'
import KnowledgePointManagement from '@/components/knowledge/KnowledgePointManagement.vue'
import GeneratedQuestions from '@/components/knowledge/GeneratedQuestions.vue'
import KnowledgeBaseSettings from '@/components/knowledge/KnowledgeBaseSettings.vue'
import DocumentUploadDialog from '@/components/knowledge/DocumentUploadDialog.vue'
import AIQuestionGenerationDialog from '@/components/knowledge/AIQuestionGenerationDialog.vue'

const route = useRoute()
const router = useRouter()

// Props
const knowledgeBaseId = computed(() => Number(route.params.id))

// 响应式数据
const knowledgeBase = ref<KnowledgeBase | null>(null)
const loading = ref(false)
const favoriteLoading = ref(false)
const activeTab = ref('documents')
const showUploadDialog = ref(false)
const showAIQuestionDialog = ref(false)

// 生命周期
onMounted(() => {
  loadKnowledgeBase()
})

// 方法
const loadKnowledgeBase = async () => {
  if (!knowledgeBaseId.value) return
  
  loading.value = true
  try {
    knowledgeBase.value = await knowledgeBaseApi.knowledgeBaseApi.getKnowledgeBase(knowledgeBaseId.value)
  } catch (error) {
    ElMessage.error('加载知识库详情失败')
    console.error('Load knowledge base failed:', error)
    // 返回到知识库列表页面
    router.push('/knowledge')
  } finally {
    loading.value = false
  }
}

const toggleFavorite = async () => {
  if (!knowledgeBase.value) return
  
  favoriteLoading.value = true
  try {
    const newStatus = await knowledgeBaseApi.knowledgeBaseApi.toggleFavorite(knowledgeBase.value.id)
    knowledgeBase.value.isFavorited = newStatus
    
    if (newStatus) {
      knowledgeBase.value.favoriteCount = (knowledgeBase.value.favoriteCount || 0) + 1
    } else {
      knowledgeBase.value.favoriteCount = Math.max((knowledgeBase.value.favoriteCount || 1) - 1, 0)
    }
    
    ElMessage.success(newStatus ? '收藏成功' : '取消收藏成功')
  } catch (error) {
    ElMessage.error('操作失败，请重试')
    console.error('Toggle favorite failed:', error)
  } finally {
    favoriteLoading.value = false
  }
}

const handleMoreAction = (command: string) => {
  switch (command) {
    case 'edit':
      editKnowledgeBase()
      break
    case 'export':
      exportKnowledgeBase()
      break
    case 'delete':
      deleteKnowledgeBase()
      break
  }
}

const editKnowledgeBase = () => {
  // TODO: 实现编辑功能，可以跳转到编辑页面或显示编辑对话框
  ElMessage.info('编辑功能开发中...')
}

const exportKnowledgeBase = () => {
  // TODO: 实现导出功能
  ElMessage.info('导出功能开发中...')
}

const deleteKnowledgeBase = async () => {
  if (!knowledgeBase.value) return
  
  try {
    await ElMessageBox.confirm(
      `确定要删除知识库"${knowledgeBase.value.name}"吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await knowledgeBaseApi.knowledgeBaseApi.deleteKnowledgeBase(knowledgeBase.value.id)
    ElMessage.success('知识库删除成功')
    
    // 返回到知识库列表页面
    router.push('/knowledge')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除知识库失败')
      console.error('Delete knowledge base failed:', error)
    }
  }
}

const handleTabClick = (tab: any) => {
  // 可以在这里处理标签页切换事件
  console.log('Tab clicked:', tab.name)
}

const handleDocumentUploaded = () => {
  // 刷新知识库信息
  loadKnowledgeBase()
  ElMessage.success('文档上传成功')
}

const handleQuestionsGenerated = () => {
  ElMessage.success('题目生成成功')
}

const handleKnowledgeBaseUpdated = () => {
  // 刷新知识库信息
  loadKnowledgeBase()
  ElMessage.success('知识库更新成功')
}

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString()
}
</script>

<style scoped>
.knowledge-base-detail {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

/* 页面头部 */
.page-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 16px 24px;
  flex-shrink: 0; /* 防止头部被压缩 */
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.breadcrumb-nav {
  font-size: 14px;
}

.breadcrumb-nav a {
  color: #409eff;
  text-decoration: none;
}

.breadcrumb-nav a:hover {
  text-decoration: underline;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* 知识库信息区域 */
.kb-info-section {
  background: #f5f7fa;
  padding: 24px;
  flex-shrink: 0;
}

/* 知识库信息卡片 */
.kb-info-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16px 20px; /* 减少垂直内边距 */
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: white;
  flex-shrink: 0; /* 防止卡片被压缩 */
}

.kb-basic-info {
  display: flex;
  gap: 12px; /* 减少间距 */
  flex: 1;
  align-items: center; /* 垂直居中对齐 */
}

.kb-icon {
  color: rgba(255, 255, 255, 0.9);
}

.kb-details {
  flex: 1;
}

.kb-title {
  margin: 0 0 4px 0; /* 减少底部间距 */
  font-size: 20px; /* 减少字体大小 */
  font-weight: 600;
  color: white;
  line-height: 1.2; /* 减少行高 */
}

.kb-description {
  margin: 0 0 8px 0; /* 减少底部间距 */
  font-size: 13px; /* 减少字体大小 */
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.4; /* 减少行高 */
}

.kb-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.create-time {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

.kb-stats {
  display: flex;
  gap: 24px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: white;
  line-height: 1;
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 4px;
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  padding: 24px;
  overflow: hidden;
  min-height: 0;
  background: #fff;
  margin: 0 24px 24px 24px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.main-content .el-tabs {
  height: 100%;
}

/* 确保标签页头部在正确位置 */
.main-content .el-tabs .el-tabs__header {
  margin: 0 0 20px 0;
}

.main-content .el-tabs .el-tabs__content {
  height: calc(100vh - 320px); /* 为标签页内容预留足够空间 */
  overflow: hidden;
}

.main-content .el-tab-pane {
  height: 100%;
  overflow: auto;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }
  
  .kb-info-section {
    padding: 16px;
  }
  
  .kb-info-card {
    flex-direction: column;
    gap: 16px;
  }
  
  .kb-stats {
    justify-content: space-around;
    width: 100%;
  }
  
  .main-content {
    padding: 16px;
  }
}
</style>
