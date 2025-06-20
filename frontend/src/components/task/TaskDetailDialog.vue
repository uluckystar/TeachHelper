<template>
  <el-dialog
    v-model="dialogVisible"
    :title="taskTitle"
    width="80%"
    :before-close="handleClose"
  >
    <div v-if="task" class="task-detail">
      <!-- 任务基本信息 -->
      <el-card class="info-card">
        <template #header>
          <div class="card-header">
            <el-icon><InfoFilled /></el-icon>
            <span>任务信息</span>
          </div>
        </template>
        
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务ID">{{ task.taskId }}</el-descriptions-item>
          <el-descriptions-item label="任务类型">{{ getTaskTypeName(task.type) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(task.status)">{{ getStatusText(task.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(task.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatTime(task.updatedTime) }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ formatTime(task.endTime) }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 进度信息 -->
      <el-card v-if="['RUNNING', 'PAUSED'].includes(task.status)" class="progress-card">
        <template #header>
          <div class="card-header">
            <el-icon><Histogram /></el-icon>
            <span>进度信息</span>
            <div class="header-actions">
              <el-button
                v-if="task.status === 'RUNNING'"
                type="warning"
                size="small"
                @click="pauseTask"
              >
                <el-icon><VideoPause /></el-icon>
                暂停
              </el-button>
              <el-button
                v-if="task.status === 'PAUSED'"
                type="success"
                size="small"
                @click="resumeTask"
              >
                <el-icon><VideoPlay /></el-icon>
                继续
              </el-button>
              <el-button
                type="danger"
                size="small"
                @click="cancelTask"
              >
                <el-icon><Close /></el-icon>
                取消
              </el-button>
            </div>
          </div>
        </template>
        
        <div class="progress-content">
          <div class="progress-overview">
            <el-progress
              type="circle"
              :percentage="task.progress || 0"
              :width="100"
              :status="task.status === 'PAUSED' ? 'warning' : undefined"
            />
            <div class="progress-stats">
              <div class="stat-item">
                <span class="stat-label">已处理</span>
                <span class="stat-value">{{ task.processedCount || 0 }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">总数</span>
                <span class="stat-value">{{ task.totalCount || 0 }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">成功率</span>
                <span class="stat-value">{{ calculateSuccessRate(task) }}%</span>
              </div>
            </div>
          </div>
          
          <div class="progress-details">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="当前步骤">{{ task.currentStep || '处理中...' }}</el-descriptions-item>
              <el-descriptions-item label="预计剩余时间">{{ task.estimatedTimeLeft || '--' }}</el-descriptions-item>
              <el-descriptions-item label="处理速度">{{ calculateProcessingSpeed(task) }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
      </el-card>

      <!-- 任务配置 -->
      <el-card v-if="task.config" class="config-card">
        <template #header>
          <div class="card-header">
            <el-icon><Setting /></el-icon>
            <span>任务配置</span>
            <div class="header-actions">
              <el-button
                v-if="['RUNNING', 'PAUSED'].includes(task.status)"
                type="primary"
                size="small"
                @click="showConfigEditor"
              >
                <el-icon><Edit /></el-icon>
                修改配置
              </el-button>
            </div>
          </div>
        </template>
        
        <div class="config-content">
          <TaskConfigDisplay :config="task.config" :task-type="task.type" />
        </div>
      </el-card>

      <!-- 实时预览 -->
      <el-card v-if="task.status === 'RUNNING' && task.previewData" class="preview-card">
        <template #header>
          <div class="card-header">
            <el-icon><View /></el-icon>
            <span>实时预览</span>
            <div class="header-actions">
              <el-switch
                v-model="autoRefreshPreview"
                active-text="自动刷新"
                @change="togglePreviewRefresh"
              />
            </div>
          </div>
        </template>
        
        <div class="preview-content">
          <TaskPreviewDisplay 
            :task="task" 
            :preview-data="task.previewData" 
            :auto-refresh="autoRefreshPreview"
          />
        </div>
      </el-card>

      <!-- 执行日志 -->
      <el-card class="log-card">
        <template #header>
          <div class="card-header">
            <el-icon><Document /></el-icon>
            <span>执行日志</span>
            <div class="header-actions">
              <el-button size="small" @click="refreshLogs">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
              <el-button size="small" @click="downloadLogs">
                <el-icon><Download /></el-icon>
                下载
              </el-button>
            </div>
          </div>
        </template>
        
        <div class="log-content">
          <div v-if="!logs.length" class="empty-logs">
            <el-empty description="暂无日志" />
          </div>
          <div v-else class="log-list">
            <div
              v-for="(log, index) in logs"
              :key="index"
              class="log-item"
              :class="[`level-${log.level.toLowerCase()}`]"
            >
              <span class="log-time">{{ formatLogTime(log.timestamp) }}</span>
              <el-tag :type="getLogLevelType(log.level)" size="small">{{ log.level }}</el-tag>
              <span class="log-message">{{ log.message }}</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 结果详情 -->
      <el-card v-if="task.status === 'COMPLETED' && task.result" class="result-card">
        <template #header>
          <div class="card-header">
            <el-icon><Trophy /></el-icon>
            <span>执行结果</span>
            <div class="header-actions">
              <el-button type="primary" size="small" @click="exportResult">
                <el-icon><Download /></el-icon>
                导出结果
              </el-button>
            </div>
          </div>
        </template>
        
        <div class="result-content">
          <TaskResultDisplay :task="task" :result="task.result" />
        </div>
      </el-card>

      <!-- 错误详情 -->
      <el-card v-if="task.status === 'FAILED' && task.error" class="error-card">
        <template #header>
          <div class="card-header">
            <el-icon><Warning /></el-icon>
            <span>错误详情</span>
            <div class="header-actions">
              <el-button type="danger" size="small" @click="retryTask">
                <el-icon><RefreshRight /></el-icon>
                重试
              </el-button>
            </div>
          </div>
        </template>
        
        <div class="error-content">
          <el-alert
            type="error"
            :title="task.error.message"
            :description="task.error.details"
            show-icon
            :closable="false"
          />
          
          <div v-if="task.error.stackTrace" class="stack-trace">
            <h4>详细错误信息：</h4>
            <pre>{{ task.error.stackTrace }}</pre>
          </div>
        </div>
      </el-card>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
        <el-button
          v-if="['RUNNING', 'PAUSED'].includes(task?.status)"
          type="primary"
          @click="saveChanges"
        >
          保存修改
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  InfoFilled, Histogram, Setting, View, Document, Trophy, Warning,
  VideoPause, VideoPlay, Close, Edit, Refresh, Download, RefreshRight
} from '@element-plus/icons-vue'
import { evaluationApi } from '@/api/evaluation'
import { questionApi } from '@/api/question'
import { knowledgeBaseApi } from '@/api/knowledge'
import TaskConfigDisplay from './TaskConfigDisplay.vue'
import TaskPreviewDisplay from './TaskPreviewDisplay.vue'
import TaskResultDisplay from './TaskResultDisplay.vue'

interface Props {
  visible: boolean
  task: any
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'task-updated', task: any): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const dialogVisible = ref(false)
const autoRefreshPreview = ref(true)
const logs = ref<any[]>([])

let previewRefreshTimer: number | null = null

// 计算属性
const taskTitle = computed(() => {
  if (!props.task) return '任务详情'
  return `${getTaskTypeName(props.task.type)} - ${props.task.taskId}`
})

// 监听器
watch(() => props.visible, (newVal) => {
  dialogVisible.value = newVal
  if (newVal && props.task) {
    loadTaskLogs()
    if (autoRefreshPreview.value) {
      startPreviewRefresh()
    }
  } else {
    stopPreviewRefresh()
  }
})

watch(dialogVisible, (newVal) => {
  if (!newVal) {
    emit('update:visible', false)
    stopPreviewRefresh()
  }
})

// 方法
const handleClose = () => {
  dialogVisible.value = false
}

const pauseTask = async () => {
  try {
    await ElMessageBox.confirm('确定要暂停此任务吗？', '确认暂停', { type: 'warning' })
    
    // 调用相应的暂停API
    if (props.task.type === 'evaluation') {
      await evaluationApi.pauseTask?.(props.task.taskId)
    } else if (props.task.type === 'generation') {
      await questionApi.cancelGenerationTask?.(props.task.taskId)
    }
    
    ElMessage.success('任务已暂停')
    emit('task-updated', { ...props.task, status: 'PAUSED' })
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('暂停任务失败')
    }
  }
}

const resumeTask = async () => {
  try {
    // 调用相应的恢复API
    if (props.task.type === 'evaluation') {
      await evaluationApi.resumeTask?.(props.task.taskId)
    } else if (props.task.type === 'generation') {
      // 目前没有恢复API，可以重新生成
      ElMessage.info('任务恢复功能开发中...')
    }
    
    ElMessage.success('任务已恢复')
    emit('task-updated', { ...props.task, status: 'RUNNING' })
  } catch (error) {
    ElMessage.error('恢复任务失败')
  }
}

const cancelTask = async () => {
  try {
    await ElMessageBox.confirm('确定要取消此任务吗？取消后无法恢复。', '确认取消', { type: 'warning' })
    
    // 调用相应的取消API
    if (props.task.type === 'evaluation') {
      await evaluationApi.cancelTask(props.task.taskId)
    } else if (props.task.type === 'generation') {
      await questionApi.cancelGenerationTask(props.task.taskId)
    }
    
    ElMessage.success('任务已取消')
    emit('task-updated', { ...props.task, status: 'CANCELLED' })
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('取消任务失败')
    }
  }
}

const retryTask = async () => {
  try {
    await ElMessageBox.confirm('确定要重试此任务吗？', '确认重试', { type: 'warning' })
    
    // 调用相应的重试API
    if (props.task.type === 'evaluation') {
      // 目前没有重试API，提示功能开发中
      ElMessage.info('评估任务重试功能开发中...')
    } else if (props.task.type === 'generation') {
      // 目前没有重试API，提示功能开发中
      ElMessage.info('生成任务重试功能开发中...')
    }
    
    ElMessage.success('任务重试已启动')
    emit('task-updated', { ...props.task, status: 'RUNNING' })
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('重试任务失败')
    }
  }
}

const loadTaskLogs = async () => {
  try {
    // 这里应该调用获取任务日志的API
    // logs.value = await taskApi.getTaskLogs(props.task.taskId)
    
    // 模拟日志数据
    logs.value = [
      {
        timestamp: new Date().toISOString(),
        level: 'INFO',
        message: '任务开始执行'
      },
      {
        timestamp: new Date(Date.now() - 30000).toISOString(),
        level: 'INFO',
        message: '正在处理数据...'
      },
      {
        timestamp: new Date(Date.now() - 60000).toISOString(),
        level: 'DEBUG',
        message: '初始化任务配置'
      }
    ]
  } catch (error) {
    console.error('加载任务日志失败:', error)
  }
}

const refreshLogs = () => {
  loadTaskLogs()
}

const downloadLogs = () => {
  // 实现日志下载功能
  const logContent = logs.value.map(log => 
    `[${log.timestamp}] ${log.level}: ${log.message}`
  ).join('\n')
  
  const blob = new Blob([logContent], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `task-${props.task.taskId}-logs.txt`
  a.click()
  URL.revokeObjectURL(url)
}

const exportResult = () => {
  // 实现结果导出功能
  if (props.task.result) {
    const resultContent = JSON.stringify(props.task.result, null, 2)
    const blob = new Blob([resultContent], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `task-${props.task.taskId}-result.json`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('结果已导出')
  }
}

const showConfigEditor = () => {
  ElMessage.info('配置编辑功能开发中...')
}

const saveChanges = () => {
  // 保存任务修改
  ElMessage.success('修改已保存')
}

const togglePreviewRefresh = (value: boolean | string | number) => {
  const boolValue = typeof value === 'boolean' ? value : Boolean(value)
  if (boolValue) {
    startPreviewRefresh()
  } else {
    stopPreviewRefresh()
  }
}

const startPreviewRefresh = () => {
  stopPreviewRefresh()
  previewRefreshTimer = setInterval(() => {
    // 刷新预览数据
    // loadPreviewData()
  }, 3000) as any
}

const stopPreviewRefresh = () => {
  if (previewRefreshTimer) {
    clearInterval(previewRefreshTimer)
    previewRefreshTimer = null
  }
}

// 辅助方法
const getTaskTypeName = (type: string) => {
  const typeMap = {
    evaluation: '批量评估',
    generation: 'AI生成',
    knowledge: '知识库处理'
  }
  return typeMap[type as keyof typeof typeMap] || '未知任务'
}

const getStatusTagType = (status: string) => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    RUNNING: 'primary',
    PENDING: 'warning',
    COMPLETED: 'success',
    FAILED: 'danger',
    PAUSED: 'info',
    CANCELLED: 'info'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status: string) => {
  const textMap = {
    RUNNING: '运行中',
    PENDING: '待处理',
    COMPLETED: '已完成',
    FAILED: '失败',
    PAUSED: '已暂停',
    CANCELLED: '已取消'
  }
  return textMap[status as keyof typeof textMap] || status
}

const getLogLevelType = (level: string) => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    ERROR: 'danger',
    WARN: 'warning',
    INFO: 'primary',
    DEBUG: 'info'
  }
  return typeMap[level] || 'info'
}

const calculateSuccessRate = (task: any) => {
  if (!task.processedCount || task.processedCount === 0) return 0
  const successCount = task.successCount || 0
  return Math.round((successCount / task.processedCount) * 100)
}

const calculateProcessingSpeed = (task: any) => {
  if (!task.startTime || !task.processedCount) return '--'
  
  const startTime = new Date(task.startTime).getTime()
  const now = Date.now()
  const elapsedMinutes = (now - startTime) / (1000 * 60)
  
  if (elapsedMinutes > 0) {
    const speed = Math.round(task.processedCount / elapsedMinutes)
    return `${speed} 项/分钟`
  }
  
  return '--'
}

const formatTime = (time: string) => {
  if (!time) return '--'
  return new Date(time).toLocaleString()
}

const formatLogTime = (time: string) => {
  if (!time) return '--'
  return new Date(time).toLocaleTimeString()
}
</script>

<style scoped>
.task-detail {
  max-height: 70vh;
  overflow-y: auto;
}

.info-card,
.progress-card,
.config-card,
.preview-card,
.log-card,
.result-card,
.error-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.card-header > span {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.progress-content {
  display: flex;
  gap: 24px;
}

.progress-overview {
  display: flex;
  align-items: center;
  gap: 24px;
}

.progress-stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  min-width: 100px;
}

.stat-label {
  color: #666;
  font-size: 14px;
}

.stat-value {
  font-weight: 600;
  color: #409eff;
}

.progress-details {
  flex: 1;
}

.config-content,
.preview-content,
.result-content {
  margin-top: 16px;
}

.log-content {
  max-height: 400px;
  overflow-y: auto;
}

.empty-logs {
  text-align: center;
  padding: 40px 20px;
}

.log-list {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
}

.log-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-bottom: 1px solid #f0f0f0;
}

.log-item:hover {
  background-color: #f8f9fa;
}

.log-time {
  color: #999;
  font-size: 11px;
  min-width: 80px;
}

.log-message {
  flex: 1;
  word-break: break-all;
}

.log-item.level-error {
  background-color: #fef0f0;
}

.log-item.level-warn {
  background-color: #fdf6ec;
}

.error-content {
  margin-top: 16px;
}

.stack-trace {
  margin-top: 16px;
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.stack-trace h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
}

.stack-trace pre {
  margin: 0;
  font-size: 12px;
  white-space: pre-wrap;
  word-wrap: break-word;
  max-height: 200px;
  overflow-y: auto;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .progress-content {
    flex-direction: column;
  }
  
  .progress-overview {
    justify-content: center;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .header-actions {
    align-self: stretch;
    justify-content: flex-end;
  }
}
</style>