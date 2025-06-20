<template>
  <div class="task-detail">
    <div class="page-header">
      <el-breadcrumb>
        <el-breadcrumb-item :to="{ name: 'ExamList' }">考试管理中心</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ name: 'TaskMonitor' }">任务监控</el-breadcrumb-item>
        <el-breadcrumb-item>任务详情</el-breadcrumb-item>
      </el-breadcrumb>
      <h1>任务详情</h1>
    </div>

    <div v-if="loading" class="loading-container">
      <el-icon :size="40"><Loading /></el-icon>
      <p>加载任务详情中...</p>
    </div>

    <div v-else-if="!task" class="error-container">
      <el-result icon="warning" title="任务不存在" sub-title="指定的任务ID不存在或已被删除">
        <template #extra>
          <el-button type="primary" @click="goBack">返回任务列表</el-button>
        </template>
      </el-result>
    </div>

    <div v-else class="task-content">
      <!-- 任务基本信息 -->
      <el-card class="task-info-card">
        <template #header>
          <div class="card-header">
            <span>任务信息</span>
            <div class="task-actions">
              <el-button 
                v-if="canCancel" 
                type="danger" 
                size="small" 
                @click="handleCancelTask"
                :loading="cancelling"
              >
                取消任务
              </el-button>
              <el-button 
                v-if="task.status === 'COMPLETED'" 
                type="primary" 
                size="small" 
                @click="viewResults"
              >
                查看结果
              </el-button>
              <el-button size="small" @click="refreshTask">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </div>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务ID">{{ task.taskId }}</el-descriptions-item>
          <el-descriptions-item label="任务状态">
            <el-tag :type="getStatusTagType(task.status)">
              {{ getStatusText(task.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="任务类型">{{ getTaskTypeText(task.taskType) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(task.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ formatDate(task.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ formatDate(task.endTime) }}</el-descriptions-item>
          <el-descriptions-item label="总答案数">{{ task.totalAnswers }}</el-descriptions-item>
          <el-descriptions-item label="执行进度">
            <div class="progress-info">
              <el-progress 
                :percentage="getProgress(task)" 
                :status="getProgressStatus(task.status)"
                :show-text="false"
              />
              <span class="progress-text">
                {{ task.successfulEvaluations + task.failedEvaluations }} / {{ task.totalAnswers }}
              </span>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="成功批阅">
            <el-tag type="success">{{ task.successfulEvaluations }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="失败批阅">
            <el-tag type="danger">{{ task.failedEvaluations }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="执行时长" v-if="task.startTime">
            {{ getExecutionDuration(task) }}
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2" v-if="task.description">
            {{ task.description }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 关联考试信息 -->
      <el-card v-if="task.examTitle" class="exam-info-card">
        <template #header>
          <span>关联考试</span>
        </template>
        <el-descriptions :column="1">
          <el-descriptions-item label="考试名称">{{ task.examTitle }}</el-descriptions-item>
          <el-descriptions-item label="考试ID">{{ task.examId }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 错误信息 -->
      <el-card v-if="task.errors && task.errors.length > 0" class="errors-card">
        <template #header>
          <span>错误详情</span>
        </template>
        <el-alert
          v-for="(error, index) in task.errors"
          :key="index"
          :title="error.message || '未知错误'"
          type="error"
          :description="error.details"
          :closable="false"
          style="margin-bottom: 8px;"
        />
      </el-card>

      <!-- 执行日志 -->
      <el-card class="logs-card">
        <template #header>
          <div class="card-header">
            <span>执行日志</span>
            <el-button size="small" @click="loadLogs" :loading="logsLoading">
              <el-icon><Refresh /></el-icon>
              刷新日志
            </el-button>
          </div>
        </template>
        
        <div v-if="logsLoading" class="logs-loading">
          <el-icon><Loading /></el-icon>
          <span>加载日志中...</span>
        </div>
        
        <div v-else-if="logs.length === 0" class="empty-logs">
          <el-empty description="暂无日志记录" />
        </div>
        
        <div v-else class="logs-content">
          <div 
            v-for="log in logs" 
            :key="log.id" 
            class="log-entry"
            :class="getLogClass(log.level)"
          >
            <div class="log-time">{{ formatLogTime(log.timestamp) }}</div>
            <div class="log-level">{{ log.level }}</div>
            <div class="log-message">{{ log.message }}</div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, Refresh } from '@element-plus/icons-vue'
import { evaluationApi } from '@/api/evaluation'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const logsLoading = ref(false)
const cancelling = ref(false)
const task = ref<any>(null)
const logs = ref<any[]>([])
let pollingTimer: number | null = null

const taskId = computed(() => route.params.taskId as string)

const canCancel = computed(() => {
  return task.value && ['PENDING', 'RUNNING'].includes(task.value.status)
})

const loadTask = async () => {
  try {
    loading.value = true
    const result = await evaluationApi.getBatchEvaluationResult(taskId.value)
    task.value = result
  } catch (error) {
    console.error('加载任务详情失败:', error)
    ElMessage.error('加载任务详情失败')
  } finally {
    loading.value = false
  }
}

const loadLogs = async () => {
  try {
    logsLoading.value = true
    // 这里需要实现获取任务日志的API
    // const result = await evaluationApi.getTaskLogs(taskId.value)
    // logs.value = result.data || []
    logs.value = [
      {
        id: 1,
        timestamp: new Date().toISOString(),
        level: 'INFO',
        message: '任务开始执行'
      }
    ]
  } catch (error) {
    console.error('加载日志失败:', error)
  } finally {
    logsLoading.value = false
  }
}

const refreshTask = async () => {
  await loadTask()
}

const handleCancelTask = async () => {
  try {
    await ElMessageBox.confirm('确定要取消此任务吗？取消后无法恢复。', '确认取消', {
      type: 'warning'
    })
    
    cancelling.value = true
    await evaluationApi.cancelTask(taskId.value)
    ElMessage.success('任务已取消')
    await loadTask()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消任务失败:', error)
      ElMessage.error('取消任务失败')
    }
  } finally {
    cancelling.value = false
  }
}

const viewResults = () => {
  router.push(`/tasks/${taskId.value}/results`)
}

const goBack = () => {
  router.push('/task-monitor')
}

const startPolling = () => {
  if (pollingTimer) return
  
  pollingTimer = window.setInterval(() => {
    if (task.value && ['PENDING', 'RUNNING'].includes(task.value.status)) {
      loadTask()
    } else {
      stopPolling()
    }
  }, 3000)
}

const stopPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

// 工具函数
const getStatusTagType = (status: string): 'success' | 'primary' | 'warning' | 'info' | 'danger' => {
  const map: Record<string, 'success' | 'primary' | 'warning' | 'info' | 'danger'> = {
    'PENDING': 'info',
    'RUNNING': 'warning', 
    'COMPLETED': 'success',
    'COMPLETED_WITH_ERRORS': 'warning',
    'FAILED': 'danger',
    'CANCELLED': 'info'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'PENDING': '待处理',
    'RUNNING': '运行中',
    'COMPLETED': '已完成',
    'COMPLETED_WITH_ERRORS': '完成(有错误)',
    'FAILED': '失败',
    'CANCELLED': '已取消'
  }
  return map[status] || status
}

const getTaskTypeText = (type: string) => {
  const map: Record<string, string> = {
    'FULL_EVALUATION': '完整批阅',
    'QUICK_EVALUATION': '快速批阅',
    'RUBRIC_BASED': '基于标准批阅'
  }
  return map[type] || type
}

const getProgress = (task: any) => {
  if (!task.totalAnswers) return 0
  return Math.round(((task.successfulEvaluations + task.failedEvaluations) / task.totalAnswers) * 100)
}

const getProgressStatus = (status: string) => {
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'exception'
  return undefined
}

const getExecutionDuration = (task: any) => {
  if (!task.startTime) return '-'
  const endTime = task.endTime ? new Date(task.endTime) : new Date()
  const startTime = new Date(task.startTime)
  const duration = Math.floor((endTime.getTime() - startTime.getTime()) / 1000)
  
  if (duration < 60) return `${duration}秒`
  if (duration < 3600) return `${Math.floor(duration / 60)}分${duration % 60}秒`
  const hours = Math.floor(duration / 3600)
  const minutes = Math.floor((duration % 3600) / 60)
  return `${hours}小时${minutes}分`
}

const formatDate = (dateString: string) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

const formatLogTime = (timestamp: string) => {
  return new Date(timestamp).toLocaleTimeString('zh-CN')
}

const getLogClass = (level: string) => {
  return `log-${level.toLowerCase()}`
}

onMounted(async () => {
  await loadTask()
  await loadLogs()
  
  // 如果任务还在运行，开始轮询
  if (task.value && ['PENDING', 'RUNNING'].includes(task.value.status)) {
    startPolling()
  }
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.task-detail {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 8px 0 0 0;
  font-size: 24px;
  font-weight: 600;
}

.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.task-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-actions {
  display: flex;
  gap: 8px;
}

.progress-info {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 200px;
}

.progress-text {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
}

.logs-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  color: #666;
}

.empty-logs {
  text-align: center;
  padding: 40px;
}

.logs-content {
  max-height: 400px;
  overflow-y: auto;
}

.log-entry {
  display: flex;
  gap: 12px;
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 12px;
}

.log-entry:last-child {
  border-bottom: none;
}

.log-time {
  color: #666;
  white-space: nowrap;
}

.log-level {
  font-weight: bold;
  min-width: 50px;
}

.log-message {
  flex: 1;
}

.log-info .log-level {
  color: #409eff;
}

.log-warning .log-level {
  color: #e6a23c;
}

.log-error .log-level {
  color: #f56c6c;
}

.log-success .log-level {
  color: #67c23a;
}
</style>
