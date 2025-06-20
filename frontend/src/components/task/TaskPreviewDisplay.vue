<template>
  <div class="task-preview-display">
    <div v-if="taskType === 'evaluation'" class="evaluation-preview">
      <h4>评估进度预览</h4>
      <div v-if="previewData.recentEvaluations" class="recent-items">
        <div
          v-for="(item, index) in previewData.recentEvaluations.slice(0, 5)"
          :key="index"
          class="preview-item"
        >
          <div class="item-header">
            <span class="item-title">答案 #{{ item.answerId }}</span>
            <el-tag :type="item.status === 'success' ? 'success' : 'danger'" size="small">
              {{ item.status === 'success' ? '成功' : '失败' }}
            </el-tag>
          </div>
          <div class="item-content">
            <p class="item-text">{{ item.content?.substring(0, 100) }}...</p>
            <div class="item-meta">
              <span>评分: {{ item.score || '--' }}</span>
              <span>用时: {{ item.duration || '--' }}s</span>
            </div>
          </div>
        </div>
      </div>
      
      <div v-if="previewData.currentBatch" class="current-batch">
        <h5>当前批次</h5>
        <el-progress
          :percentage="previewData.currentBatch.progress || 0"
          :show-text="false"
        />
        <p>正在处理: {{ previewData.currentBatch.currentItem || '--' }}</p>
      </div>
    </div>

    <div v-else-if="taskType === 'generation'" class="generation-preview">
      <h4>生成进度预览</h4>
      <div v-if="previewData.generatedQuestions" class="recent-items">
        <div
          v-for="(question, index) in previewData.generatedQuestions.slice(0, 3)"
          :key="index"
          class="preview-item"
        >
          <div class="item-header">
            <span class="item-title">{{ getQuestionTypeText(question.type) }}</span>
            <el-tag :type="getDifficultyType(question.difficulty)" size="small">
              {{ getDifficultyText(question.difficulty) }}
            </el-tag>
          </div>
          <div class="item-content">
            <p class="item-text">{{ question.content?.substring(0, 120) }}...</p>
            <div class="item-meta">
              <span>分值: {{ question.score || '--' }}</span>
              <span>知识点: {{ question.knowledgePoint || '--' }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <div v-if="previewData.currentStep" class="current-step">
        <h5>当前步骤</h5>
        <p>{{ previewData.currentStep.description }}</p>
        <el-progress
          :percentage="previewData.currentStep.progress || 0"
          :show-text="false"
        />
      </div>
    </div>

    <div v-else-if="taskType === 'knowledge'" class="knowledge-preview">
      <h4>处理进度预览</h4>
      <div v-if="previewData.processedDocuments" class="recent-items">
        <div
          v-for="(doc, index) in previewData.processedDocuments.slice(0, 4)"
          :key="index"
          class="preview-item"
        >
          <div class="item-header">
            <span class="item-title">{{ doc.name }}</span>
            <el-tag :type="doc.status === 'completed' ? 'success' : 'warning'" size="small">
              {{ doc.status === 'completed' ? '完成' : '处理中' }}
            </el-tag>
          </div>
          <div class="item-content">
            <div class="item-meta">
              <span>大小: {{ formatFileSize(doc.size) }}</span>
              <span>页数: {{ doc.pageCount || '--' }}</span>
              <span>向量数: {{ doc.vectorCount || '--' }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <div v-if="previewData.currentProcessing" class="current-processing">
        <h5>当前处理</h5>
        <p>{{ previewData.currentProcessing.fileName }}</p>
        <el-progress
          :percentage="previewData.currentProcessing.progress || 0"
          :show-text="false"
        />
        <p class="processing-step">{{ previewData.currentProcessing.step || '处理中...' }}</p>
      </div>
    </div>

    <div v-else class="unknown-preview">
      <el-empty description="暂无预览数据" />
    </div>

    <!-- 自动刷新指示器 -->
    <div v-if="autoRefresh" class="refresh-indicator">
      <el-icon class="spinning"><Loading /></el-icon>
      <span>实时更新中...</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Loading } from '@element-plus/icons-vue'

interface Props {
  task: any
  previewData: any
  autoRefresh?: boolean
}

const props = defineProps<Props>()

// 计算属性
const taskType = computed(() => props.task?.type || 'unknown')

// 辅助方法
const getQuestionTypeText = (type: string) => {
  const typeMap = {
    'multiple_choice': '选择题',
    'fill_blank': '填空题',
    'short_answer': '简答题',
    'essay': '论述题'
  }
  return typeMap[type as keyof typeof typeMap] || type
}

const getDifficultyType = (difficulty: string): 'success' | 'primary' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'success' | 'primary' | 'warning' | 'info' | 'danger'> = {
    'easy': 'success',
    'medium': 'warning',
    'hard': 'danger'
  }
  return typeMap[difficulty] || 'info'
}

const getDifficultyText = (difficulty: string) => {
  const textMap = {
    'easy': '简单',
    'medium': '中等',
    'hard': '困难'
  }
  return textMap[difficulty as keyof typeof textMap] || difficulty
}

const formatFileSize = (bytes: number) => {
  if (!bytes) return '--'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}
</script>

<style scoped>
.task-preview-display {
  padding: 16px;
  position: relative;
}

.recent-items {
  margin-bottom: 20px;
}

.preview-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 8px;
  background-color: #fafafa;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.item-title {
  font-weight: 600;
  color: #303133;
}

.item-content {
  font-size: 14px;
}

.item-text {
  margin: 0 0 8px 0;
  color: #606266;
  line-height: 1.4;
}

.item-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
}

.current-batch,
.current-step,
.current-processing {
  padding: 16px;
  background-color: #f0f9ff;
  border-radius: 6px;
  border-left: 4px solid #409eff;
}

.current-batch h5,
.current-step h5,
.current-processing h5 {
  margin: 0 0 12px 0;
  color: #409eff;
  font-size: 14px;
}

.current-batch p,
.current-step p,
.current-processing p {
  margin: 8px 0;
  color: #606266;
}

.processing-step {
  font-size: 12px;
  color: #909399;
  font-style: italic;
}

.unknown-preview {
  text-align: center;
  padding: 40px 20px;
}

.refresh-indicator {
  position: absolute;
  top: 16px;
  right: 16px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #409eff;
  background-color: rgba(64, 158, 255, 0.1);
  padding: 4px 8px;
  border-radius: 4px;
}

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
