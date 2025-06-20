<template>
  <el-dialog
    v-model="visible"
    title="文档处理进度"
    width="600px"
    :close-on-click-modal="false"
    :show-close="!processing"
  >
    <div class="processing-dialog">
      <!-- 总体进度 -->
      <div class="overall-progress">
        <div class="progress-header">
          <h3>AI文档处理</h3>
          <div class="progress-stats">
            {{ completedCount }}/{{ documents.length }} 已完成
          </div>
        </div>
        
        <el-progress 
          :percentage="overallProgress"
          :stroke-width="8"
          :text-inside="true"
          :status="progressStatus"
        />
        
        <div class="progress-info">
          <div class="time-info">
            <span v-if="processing">预计剩余时间：{{ estimatedTimeLeft }}</span>
            <span v-else-if="completed">总用时：{{ totalTime }}</span>
          </div>
          <div class="status-info">
            {{ statusText }}
          </div>
        </div>
      </div>

      <!-- 处理阶段指示器 -->
      <div class="processing-stages" v-if="processing">
        <div class="stage-header">
          <h4>处理阶段</h4>
        </div>
        <el-steps :active="currentStageIndex" align-center>
          <el-step 
            v-for="(stage, index) in processingStages"
            :key="stage.key"
            :title="stage.title"
            :description="stage.description"
            :status="getStageStatus(index)"
          />
        </el-steps>
      </div>

      <!-- 文档列表 -->
      <div class="documents-list">
        <div class="list-header">
          <h4>文档详情</h4>
          <div class="list-actions">
            <el-button 
              size="small" 
              @click="toggleShowCompleted"
              v-if="completedCount > 0"
            >
              {{ showCompleted ? '隐藏' : '显示' }}已完成
            </el-button>
            <el-button 
              size="small" 
              @click="toggleShowDetails"
            >
              {{ showDetails ? '隐藏' : '显示' }}详情
            </el-button>
          </div>
        </div>

        <div class="documents-container">
          <div 
            v-for="document in filteredDocuments" 
            :key="document.id"
            class="document-item"
            :class="{ 'document-processing': document.processing }"
          >
            <!-- 文档基本信息 -->
            <div class="document-header">
              <div class="document-info">
                <el-icon class="document-icon">
                  <Document v-if="document.type === 'pdf'" />
                  <DocumentCopy v-else-if="document.type === 'word'" />
                  <Picture v-else-if="document.type === 'image'" />
                  <Files v-else />
                </el-icon>
                <div class="document-details">
                  <div class="document-name">{{ document.fileName }}</div>
                  <div class="document-meta">
                    {{ formatFileSize(document.fileSize) }} · {{ document.type.toUpperCase() }}
                  </div>
                </div>
              </div>
              
              <div class="document-status">
                <el-tag 
                  :type="getDocumentStatusType(document.status)"
                  size="small"
                >
                  {{ getDocumentStatusText(document.status) }}
                </el-tag>
              </div>
            </div>

            <!-- 处理进度 -->
            <div class="document-progress" v-if="document.processing || document.status === 'processing'">
              <el-progress 
                :percentage="document.processingProgress || 0"
                :stroke-width="6"
                size="small"
                :status="document.status === 'error' ? 'exception' : undefined"
              />
              <div class="progress-text">
                {{ document.currentStage || '正在处理...' }}
              </div>
            </div>

            <!-- 处理结果 -->
            <div class="document-results" v-if="showDetails && document.status === 'completed'">
              <div class="results-grid">
                <div class="result-item">
                  <span class="result-label">知识点：</span>
                  <span class="result-value">{{ document.extractedKnowledgePoints || 0 }}</span>
                </div>
                <div class="result-item">
                  <span class="result-label">核心概念：</span>
                  <span class="result-value">{{ document.extractedConcepts || 0 }}</span>
                </div>
                <div class="result-item">
                  <span class="result-label">例题：</span>
                  <span class="result-value">{{ document.extractedExamples || 0 }}</span>
                </div>
                <div class="result-item">
                  <span class="result-label">公式：</span>
                  <span class="result-value">{{ document.extractedFormulas || 0 }}</span>
                </div>
              </div>
            </div>

            <!-- 错误信息 -->
            <div class="document-error" v-if="document.status === 'error'">
              <el-alert 
                type="error" 
                :title="document.errorMessage || '处理失败'"
                :closable="false"
                size="small"
              />
              <div class="error-actions">
                <el-button 
                  size="small" 
                  type="primary" 
                  @click="retryDocument(document)"
                >
                  重试
                </el-button>
                <el-button 
                  size="small" 
                  @click="skipDocument(document)"
                >
                  跳过
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 实时日志 -->
      <div class="processing-logs" v-if="showLogs && logs.length > 0">
        <div class="logs-header">
          <h4>处理日志</h4>
          <el-button size="small" @click="clearLogs">清空</el-button>
        </div>
        <div class="logs-container">
          <div 
            v-for="log in logs" 
            :key="log.id"
            class="log-item"
            :class="`log-${log.level}`"
          >
            <span class="log-time">{{ formatTime(log.timestamp) }}</span>
            <span class="log-message">{{ log.message }}</span>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="toggleShowLogs" v-if="logs.length > 0">
          {{ showLogs ? '隐藏' : '显示' }}日志
        </el-button>
        <el-button 
          @click="pauseProcessing" 
          v-if="processing"
          type="warning"
        >
          {{ paused ? '继续' : '暂停' }}
        </el-button>
        <el-button 
          @click="stopProcessing" 
          v-if="processing"
          type="danger"
        >
          停止处理
        </el-button>
        <el-button 
          @click="visible = false"
          :disabled="processing"
          type="primary"
        >
          {{ processing ? '处理中...' : '完成' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document,
  DocumentCopy,
  Picture,
  Files
} from '@element-plus/icons-vue'

// Props
const props = defineProps<{
  modelValue: boolean
  documents: DocumentItem[]
}>()

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  retry: [document: DocumentItem]
  skip: [document: DocumentItem]
}>()

// 响应式数据
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const processing = ref(false)
const paused = ref(false)
const completed = ref(false)
const startTime = ref<Date | null>(null)
const endTime = ref<Date | null>(null)
const currentStageIndex = ref(0)
const showCompleted = ref(true)
const showDetails = ref(false)
const showLogs = ref(false)
const logs = ref<LogItem[]>([])

// 处理阶段定义
const processingStages = [
  {
    key: 'upload',
    title: '文档上传',
    description: '上传文档到服务器'
  },
  {
    key: 'parse',
    title: '文档解析',
    description: '解析文档内容和结构'
  },
  {
    key: 'extract',
    title: '知识提取',
    description: 'AI提取知识点和概念'
  },
  {
    key: 'organize',
    title: '结构化整理',
    description: '整理和分类提取的知识'
  },
  {
    key: 'complete',
    title: '处理完成',
    description: '保存结果并生成索引'
  }
]

// 类型定义
interface DocumentItem {
  id: number
  fileName: string
  fileSize: number
  type: string
  status: 'waiting' | 'processing' | 'completed' | 'error'
  processing?: boolean
  processingProgress?: number
  currentStage?: string
  errorMessage?: string
  extractedKnowledgePoints?: number
  extractedConcepts?: number
  extractedExamples?: number
  extractedFormulas?: number
}

interface LogItem {
  id: string
  timestamp: Date
  level: 'info' | 'success' | 'warning' | 'error'
  message: string
}

// 计算属性
const completedCount = computed(() => {
  return props.documents.filter(doc => 
    doc.status === 'completed' || doc.status === 'error'
  ).length
})

const overallProgress = computed(() => {
  if (props.documents.length === 0) return 0
  
  const totalProgress = props.documents.reduce((sum, doc) => {
    if (doc.status === 'completed') return sum + 100
    if (doc.status === 'error') return sum + 100
    return sum + (doc.processingProgress || 0)
  }, 0)
  
  return Math.round(totalProgress / props.documents.length)
})

const progressStatus = computed(() => {
  if (paused.value) return 'warning'
  if (completed.value) return 'success'
  return undefined
})

const statusText = computed(() => {
  if (paused.value) return '处理已暂停'
  if (completed.value) return '所有文档处理完成'
  if (processing.value) return '正在进行AI文档处理...'
  return '准备开始处理'
})

const estimatedTimeLeft = computed(() => {
  if (!processing.value || !startTime.value) return '计算中...'
  
  const elapsed = Date.now() - startTime.value.getTime()
  const progressPercentage = overallProgress.value / 100
  
  if (progressPercentage <= 0) return '计算中...'
  
  const totalEstimated = elapsed / progressPercentage
  const remaining = totalEstimated - elapsed
  
  return formatDuration(remaining)
})

const totalTime = computed(() => {
  if (!startTime.value || !endTime.value) return '未知'
  const duration = endTime.value.getTime() - startTime.value.getTime()
  return formatDuration(duration)
})

const filteredDocuments = computed(() => {
  if (showCompleted.value) {
    return props.documents
  }
  return props.documents.filter(doc => 
    doc.status !== 'completed' && doc.status !== 'error'
  )
})

// 监听文档变化
watch(() => props.documents, (newDocs) => {
  if (newDocs.length > 0 && !processing.value) {
    startProcessing()
  }
  
  checkProcessingComplete()
  updateCurrentStage()
}, { deep: true })

// 监听对话框显示
watch(visible, (show) => {
  if (show && props.documents.length > 0) {
    startProcessing()
  }
})

// 生命周期
onMounted(() => {
  if (props.documents.length > 0) {
    startProcessing()
  }
})

onUnmounted(() => {
  if (processing.value) {
    stopProcessing()
  }
})

// 方法
const startProcessing = () => {
  processing.value = true
  completed.value = false
  startTime.value = new Date()
  endTime.value = null
  currentStageIndex.value = 0
  
  addLog('info', '开始处理文档...')
  
  // 模拟处理进度
  simulateProcessing()
}

const simulateProcessing = () => {
  const interval = setInterval(() => {
    if (paused.value || !processing.value) return
    
    // 更新文档处理进度
    props.documents.forEach(doc => {
      if (doc.status === 'processing' || doc.status === 'waiting') {
        if (doc.status === 'waiting') {
          doc.status = 'processing'
          doc.processing = true
          doc.processingProgress = 0
          addLog('info', `开始处理：${doc.fileName}`)
        }
        
        doc.processingProgress = (doc.processingProgress || 0) + Math.random() * 5
        
        // 更新当前阶段
        const progress = doc.processingProgress || 0
        if (progress < 20) {
          doc.currentStage = '解析文档结构...'
        } else if (progress < 40) {
          doc.currentStage = '提取文本内容...'
        } else if (progress < 60) {
          doc.currentStage = 'AI分析知识点...'
        } else if (progress < 80) {
          doc.currentStage = '整理知识结构...'
        } else {
          doc.currentStage = '生成知识索引...'
        }
        
        if (progress >= 100) {
          doc.status = Math.random() > 0.1 ? 'completed' : 'error'
          doc.processing = false
          doc.processingProgress = 100
          
          if (doc.status === 'completed') {
            // 模拟提取结果
            doc.extractedKnowledgePoints = Math.floor(Math.random() * 20) + 5
            doc.extractedConcepts = Math.floor(Math.random() * 15) + 3
            doc.extractedExamples = Math.floor(Math.random() * 10) + 1
            doc.extractedFormulas = Math.floor(Math.random() * 8) + 1
            
            addLog('success', `处理完成：${doc.fileName}`)
          } else {
            doc.errorMessage = '文档格式解析失败，请检查文档是否损坏'
            addLog('error', `处理失败：${doc.fileName} - ${doc.errorMessage}`)
          }
        }
      }
    })
    
    checkProcessingComplete()
    updateCurrentStage()
    
    if (completed.value) {
      clearInterval(interval)
    }
  }, 500)
}

const checkProcessingComplete = () => {
  const allProcessed = props.documents.every(doc => 
    doc.status === 'completed' || doc.status === 'error'
  )
  
  if (allProcessed && processing.value) {
    processing.value = false
    completed.value = true
    endTime.value = new Date()
    currentStageIndex.value = processingStages.length - 1
    
    const successCount = props.documents.filter(doc => doc.status === 'completed').length
    const errorCount = props.documents.filter(doc => doc.status === 'error').length
    
    addLog('success', `处理完成！成功：${successCount}，失败：${errorCount}`)
    
    if (successCount > 0) {
      ElMessage.success(`成功处理 ${successCount} 个文档`)
    }
    if (errorCount > 0) {
      ElMessage.warning(`${errorCount} 个文档处理失败`)
    }
  }
}

const updateCurrentStage = () => {
  const progress = overallProgress.value
  
  if (progress < 20) {
    currentStageIndex.value = 0
  } else if (progress < 40) {
    currentStageIndex.value = 1
  } else if (progress < 60) {
    currentStageIndex.value = 2
  } else if (progress < 80) {
    currentStageIndex.value = 3
  } else {
    currentStageIndex.value = 4
  }
}

const pauseProcessing = () => {
  paused.value = !paused.value
  
  if (paused.value) {
    addLog('warning', '处理已暂停')
    ElMessage.warning('处理已暂停')
  } else {
    addLog('info', '处理已继续')
    ElMessage.info('处理已继续')
  }
}

const stopProcessing = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要停止处理吗？未完成的文档将保持当前状态。',
      '确认停止',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    processing.value = false
    paused.value = false
    endTime.value = new Date()
    
    addLog('warning', '处理已停止')
    ElMessage.info('处理已停止')
    
  } catch (error) {
    // 用户取消
  }
}

const retryDocument = (document: DocumentItem) => {
  document.status = 'waiting'
  document.processing = false
  document.processingProgress = 0
  document.errorMessage = undefined
  
  addLog('info', `重试处理：${document.fileName}`)
  emit('retry', document)
}

const skipDocument = (document: DocumentItem) => {
  // 将文档标记为已完成但结果为空
  document.status = 'completed'
  document.processing = false
  document.processingProgress = 100
  document.extractedKnowledgePoints = 0
  document.extractedConcepts = 0
  document.extractedExamples = 0
  document.extractedFormulas = 0
  
  addLog('warning', `跳过处理：${document.fileName}`)
  emit('skip', document)
}

const toggleShowCompleted = () => {
  showCompleted.value = !showCompleted.value
}

const toggleShowDetails = () => {
  showDetails.value = !showDetails.value
}

const toggleShowLogs = () => {
  showLogs.value = !showLogs.value
}

const clearLogs = () => {
  logs.value = []
}

const addLog = (level: 'info' | 'success' | 'warning' | 'error', message: string) => {
  logs.value.push({
    id: `${Date.now()}-${Math.random()}`,
    timestamp: new Date(),
    level,
    message
  })
  
  // 限制日志数量
  if (logs.value.length > 100) {
    logs.value = logs.value.slice(-50)
  }
}

const getStageStatus = (index: number) => {
  if (index < currentStageIndex.value) return 'finish'
  if (index === currentStageIndex.value && processing.value) return 'process'
  return 'wait'
}

const getDocumentStatusType = (status: string): 'success' | 'primary' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'success' | 'primary' | 'warning' | 'info' | 'danger'> = {
    waiting: 'info',
    processing: 'warning',
    completed: 'success',
    error: 'danger'
  }
  return typeMap[status] || 'info'
}

const getDocumentStatusText = (status: string) => {
  const textMap: Record<string, string> = {
    waiting: '等待中',
    processing: '处理中',
    completed: '已完成',
    error: '失败'
  }
  return textMap[status] || '未知'
}

// 工具方法
const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatDuration = (milliseconds: number) => {
  const seconds = Math.floor(milliseconds / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  
  if (hours > 0) {
    return `${hours}小时${minutes % 60}分钟`
  } else if (minutes > 0) {
    return `${minutes}分钟${seconds % 60}秒`
  } else {
    return `${seconds}秒`
  }
}

const formatTime = (date: Date) => {
  return date.toLocaleTimeString()
}
</script>

<style scoped>
.processing-dialog {
  max-height: 70vh;
  overflow-y: auto;
}

/* 总体进度 */
.overall-progress {
  margin-bottom: 24px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.progress-header h3 {
  margin: 0;
  color: #303133;
  font-size: 16px;
}

.progress-stats {
  font-size: 14px;
  color: #409eff;
  font-weight: 500;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  font-size: 13px;
  color: #606266;
}

/* 处理阶段 */
.processing-stages {
  margin-bottom: 24px;
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.stage-header h4 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 14px;
}

/* 文档列表 */
.documents-list {
  margin-bottom: 24px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.list-header h4 {
  margin: 0;
  color: #303133;
  font-size: 14px;
}

.list-actions {
  display: flex;
  gap: 8px;
}

.documents-container {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  max-height: 400px;
  overflow-y: auto;
}

.document-item {
  padding: 12px;
  border-bottom: 1px solid #f5f7fa;
  transition: background-color 0.2s;
}

.document-item:last-child {
  border-bottom: none;
}

.document-item.document-processing {
  background-color: #f0f9ff;
}

.document-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.document-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.document-icon {
  color: #409eff;
  font-size: 16px;
}

.document-details {
  flex: 1;
}

.document-name {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
  margin-bottom: 2px;
}

.document-meta {
  font-size: 12px;
  color: #909399;
}

.document-progress {
  margin-bottom: 8px;
}

.progress-text {
  font-size: 12px;
  color: #409eff;
  margin-top: 4px;
}

.document-results {
  margin-top: 8px;
  padding: 8px;
  background-color: #f0f9ff;
  border-radius: 4px;
}

.results-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.result-item {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.result-label {
  color: #909399;
}

.result-value {
  color: #303133;
  font-weight: 500;
}

.document-error {
  margin-top: 8px;
}

.error-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

/* 处理日志 */
.processing-logs {
  margin-top: 24px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.logs-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.logs-header h4 {
  margin: 0;
  color: #303133;
  font-size: 13px;
}

.logs-container {
  max-height: 200px;
  overflow-y: auto;
  background-color: #fafbfc;
}

.log-item {
  display: flex;
  gap: 8px;
  padding: 4px 12px;
  font-size: 12px;
  font-family: 'Courier New', monospace;
  border-bottom: 1px solid #f0f0f0;
}

.log-time {
  color: #909399;
  min-width: 80px;
}

.log-message {
  flex: 1;
}

.log-info .log-message {
  color: #606266;
}

.log-success .log-message {
  color: #67c23a;
}

.log-warning .log-message {
  color: #e6a23c;
}

.log-error .log-message {
  color: #f56c6c;
}

/* 对话框底部 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .progress-header,
  .list-header {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }
  
  .progress-info {
    flex-direction: column;
    gap: 4px;
  }
  
  .results-grid {
    grid-template-columns: 1fr;
  }
  
  .document-header {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
}
</style>
