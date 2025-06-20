<template>
  <div class="task-center-view">
    <div class="page-header">
      <h1>任务中心</h1>
      <p class="page-description">统一管理所有后台任务，支持实时监控、进度控制和结果预览</p>
    </div>

    <!-- 任务统计面板 -->
    <el-row :gutter="24" class="stats-cards">
      <el-col :span="6">
        <el-card class="stat-card running">
          <div class="stat-content">
            <div class="stat-number">{{ taskStats.running }}</div>
            <div class="stat-label">运行中</div>
            <el-icon class="stat-icon"><Timer /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card pending">
          <div class="stat-content">
            <div class="stat-number">{{ taskStats.pending }}</div>
            <div class="stat-label">待处理</div>
            <el-icon class="stat-icon"><Clock /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card completed">
          <div class="stat-content">
            <div class="stat-number">{{ taskStats.completed }}</div>
            <div class="stat-label">已完成</div>
            <el-icon class="stat-icon"><Check /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card failed">
          <div class="stat-content">
            <div class="stat-number">{{ taskStats.failed }}</div>
            <div class="stat-label">失败</div>
            <el-icon class="stat-icon"><Close /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快速操作工具栏 -->
    <el-card class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button type="primary" @click="refreshTasks">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button @click="clearCompletedTasks">
            <el-icon><Delete /></el-icon>
            清理已完成
          </el-button>
          <el-button @click="pauseAllTasks" :disabled="!hasRunningTasks">
            <el-icon><VideoPause /></el-icon>
            暂停全部
          </el-button>
        </div>
        <div class="toolbar-right">
          <!-- WebSocket连接状态 -->
          <div class="ws-status">
            <el-badge 
              :type="connectionStatus === 'connected' ? 'success' : 'danger'" 
              is-dot
            >
              <span class="status-text">
                {{ connectionStatus === 'connected' ? '实时连接' : '连接断开' }}
              </span>
            </el-badge>
          </div>
          
          <el-switch
            v-model="autoRefresh"
            active-text="自动刷新"
            @change="toggleAutoRefresh"
          />
          <el-dropdown @command="handleQuickAction">
            <el-button>
              快速创建任务
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="batch-generation">
                  <el-icon><EditPen /></el-icon>
                  批量生成题目
                </el-dropdown-item>
                <el-dropdown-item command="batch-evaluation">
                  <el-icon><ChatLineRound /></el-icon>
                  批量批阅答案
                </el-dropdown-item>
                <el-dropdown-item command="paper-generation">
                  <el-icon><Document /></el-icon>
                  AI试卷生成
                </el-dropdown-item>
                <el-dropdown-item command="knowledge-processing">
                  <el-icon><Collection /></el-icon>
                  知识库处理
                </el-dropdown-item>
                <el-dropdown-item command="rubric-generation">
                  <el-icon><Setting /></el-icon>
                  评分标准生成
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-card>

    <!-- 任务列表 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span>任务列表</span>
          <div class="header-actions">
            <el-radio-group v-model="filterStatus" @change="loadTasks">
              <el-radio-button label="">全部</el-radio-button>
              <el-radio-button label="RUNNING">运行中</el-radio-button>
              <el-radio-button label="PENDING">待处理</el-radio-button>
              <el-radio-button label="COMPLETED">已完成</el-radio-button>
              <el-radio-button label="FAILED">失败</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>

      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="3" animated />
      </div>

      <div v-else-if="!tasks.length" class="empty-state">
        <el-empty description="暂无任务" />
      </div>

      <div v-else class="task-list">
        <div
          v-for="task in tasks"
          :key="task.taskId || task.id"
          class="task-item"
          :class="[`status-${task.status.toLowerCase()}`]"
        >
          <div class="task-header">
            <div class="task-info">
              <h3 class="task-title">{{ getTaskTitle(task) }}</h3>
              <div class="task-meta">
                <el-tag :type="getStatusTagType(task.status)" size="small">
                  {{ getStatusText(task.status) }}
                </el-tag>
                <span class="task-id">ID: {{ task.taskId || task.id }}</span>
                <span class="task-time">{{ formatTime(task.createdAt || task.startTime) }}</span>
              </div>
            </div>
            <div class="task-actions">
              <el-button
                v-if="task.status === 'RUNNING'"
                type="warning"
                size="small"
                @click="pauseTask(task)"
              >
                <el-icon><VideoPause /></el-icon>
                暂停
              </el-button>
              <el-button
                v-if="task.status === 'PAUSED'"
                type="success"
                size="small"
                @click="resumeTask(task)"
              >
                <el-icon><VideoPlay /></el-icon>
                继续
              </el-button>
              <el-button
                v-if="['RUNNING', 'PENDING'].includes(task.status)"
                type="danger"
                size="small"
                @click="cancelTask(task)"
              >
                <el-icon><Close /></el-icon>
                取消
              </el-button>
              <el-button
                type="primary"
                size="small"
                @click="showTaskDetail(task)"
              >
                <el-icon><View /></el-icon>
                详情
              </el-button>
            </div>
          </div>

          <!-- 进度条 -->
          <div v-if="['RUNNING', 'PAUSED'].includes(task.status)" class="task-progress">
            <div class="progress-info">
              <span class="progress-text">{{ task.currentStep || '处理中...' }}</span>
              <span class="progress-percent">{{ task.progress || 0 }}%</span>
            </div>
            <el-progress
              :percentage="task.progress || 0"
              :status="task.status === 'PAUSED' ? 'warning' : undefined"
              :show-text="false"
            />
            <div class="progress-details">
              <span>已处理: {{ task.processedCount || 0 }} / {{ task.totalCount || 0 }}</span>
              <span v-if="task.estimatedTimeLeft">预计剩余: {{ task.estimatedTimeLeft }}</span>
            </div>
          </div>

          <!-- 实时预览 -->
          <div v-if="task.status === 'RUNNING' && task.previewData" class="task-preview">
            <el-collapse>
              <el-collapse-item title="实时预览" name="preview">
                <component
                  :is="getPreviewComponent(task.type)"
                  :task="task"
                  :preview-data="task.previewData"
                />
              </el-collapse-item>
            </el-collapse>
          </div>

          <!-- 结果摘要 -->
          <div v-if="task.status === 'COMPLETED'" class="task-result">
            <div class="result-summary">
              <el-statistic
                v-if="task.result?.successCount"
                title="成功"
                :value="task.result.successCount"
                suffix="项"
              />
              <el-statistic
                v-if="task.result?.failedCount"
                title="失败"
                :value="task.result.failedCount"
                suffix="项"
              />
              <el-statistic
                v-if="task.result?.totalTime"
                title="耗时"
                :value="task.result.totalTime"
                suffix="秒"
              />
            </div>
          </div>

          <!-- 错误信息 -->
          <div v-if="task.status === 'FAILED' && task.error" class="task-error">
            <el-alert
              type="error"
              :title="task.error.message"
              :description="task.error.details"
              show-icon
              :closable="false"
            />
          </div>
        </div>
      </div>
    </el-card>

    <!-- 任务详情对话框 -->
    <TaskDetailDialog
      v-model:visible="detailDialogVisible"
      :task="selectedTask"
      @task-updated="handleTaskUpdated"
    />

    <!-- 快速创建任务对话框 -->
    <QuickTaskCreateDialog
      v-model:visible="createDialogVisible"
      :task-type="createTaskType"
      @task-created="handleTaskCreated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Timer, Clock, Check, Close, Refresh, Delete, VideoPause, VideoPlay,
  ArrowDown, View, EditPen, ChatLineRound, Document, Collection, Setting
} from '@element-plus/icons-vue'
import { taskApi } from '@/api/task'
import { useTaskWebSocket, useTaskUpdates } from '@/utils/taskWebSocket'
import TaskDetailDialog from '@/components/task/TaskDetailDialog.vue'
import QuickTaskCreateDialog from '@/components/task/QuickTaskCreateDialog.vue'

const router = useRouter()

// WebSocket实时更新
const { isConnected, connectionStatus } = useTaskWebSocket()
const { taskUpdates, unsubscribe } = useTaskUpdates()

// 响应式数据
const loading = ref(false)
const autoRefresh = ref(true)
const filterStatus = ref('')
const tasks = ref<any[]>([])
const detailDialogVisible = ref(false)
const createDialogVisible = ref(false)
const selectedTask = ref<any>(null)
const createTaskType = ref('')

let refreshTimer: number | null = null

const taskStats = reactive({
  running: 0,
  pending: 0,
  completed: 0,
  failed: 0
})

// 计算属性
const hasRunningTasks = computed(() => taskStats.running > 0)

// 监听WebSocket任务更新
const handleTaskUpdate = (taskId: string, update: any) => {
  const taskIndex = tasks.value.findIndex(t => (t.taskId || t.id) === taskId)
  if (taskIndex > -1) {
    // 更新现有任务
    tasks.value[taskIndex] = { ...tasks.value[taskIndex], ...update }
  } else {
    // 如果是新任务，重新加载任务列表
    loadTasks()
  }
  
  // 更新统计
  updateTaskStats(tasks.value)
}

// 监听WebSocket更新
import { watch } from 'vue'
watch(taskUpdates, (updates) => {
  Object.entries(updates).forEach(([taskId, update]) => {
    handleTaskUpdate(taskId, update)
  })
}, { deep: true })

// 方法
const loadTasks = async () => {
  loading.value = true
  try {
    // 并行加载任务和统计数据
    const [tasksResponse, statsResponse] = await Promise.all([
      taskApi.getTasks({
        size: 50,
        sort: 'createdAt,desc'
      }),
      taskApi.getTaskStatistics()
    ])

    const allTasks = tasksResponse.items || tasksResponse

    // 按状态过滤
    tasks.value = filterStatus.value 
      ? allTasks.filter((task: any) => task.status === filterStatus.value)
      : allTasks

    // 更新统计（优先使用API返回的统计数据）
    if (statsResponse) {
      taskStats.running = statsResponse.running || 0
      taskStats.pending = statsResponse.pending || 0
      taskStats.completed = statsResponse.completed || 0
      taskStats.failed = statsResponse.failed || 0
    } else {
      updateTaskStats(allTasks)
    }

  } catch (error) {
    console.error('加载任务失败:', error)
    ElMessage.error('加载任务失败')
  } finally {
    loading.value = false
  }
}

const updateTaskStats = (allTasks: any[]) => {
  taskStats.running = allTasks.filter(t => t.status === 'RUNNING').length
  taskStats.pending = allTasks.filter(t => t.status === 'PENDING').length
  taskStats.completed = allTasks.filter(t => t.status === 'COMPLETED').length
  taskStats.failed = allTasks.filter(t => t.status === 'FAILED').length
}

const refreshTasks = () => {
  loadTasks()
}

const toggleAutoRefresh = (value: string | number | boolean) => {
  const boolValue = Boolean(value)
  autoRefresh.value = boolValue
  if (boolValue) {
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
}

const startAutoRefresh = () => {
  stopAutoRefresh()
  refreshTimer = setInterval(loadTasks, 5000) as any
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

const pauseTask = async (task: any) => {
  try {
    await ElMessageBox.confirm('确定要暂停此任务吗？', '确认暂停', { type: 'warning' })
    
    await taskApi.pauseTask(task.taskId || task.id)
    
    ElMessage.success('任务已暂停')
    loadTasks()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('暂停任务失败')
    }
  }
}

const resumeTask = async (task: any) => {
  try {
    await taskApi.resumeTask(task.taskId || task.id)
    
    ElMessage.success('任务已恢复')
    loadTasks()
  } catch (error) {
    ElMessage.error('恢复任务失败')
  }
}

const cancelTask = async (task: any) => {
  try {
    await ElMessageBox.confirm('确定要取消此任务吗？取消后无法恢复。', '确认取消', { type: 'warning' })
    
    await taskApi.cancelTask(task.taskId || task.id)
    
    ElMessage.success('任务已取消')
    loadTasks()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('取消任务失败')
    }
  }
}

const pauseAllTasks = async () => {
  try {
    await ElMessageBox.confirm('确定要暂停所有运行中的任务吗？', '确认暂停', { type: 'warning' })
    
    await taskApi.batchPauseTasks({ status: 'RUNNING' })
    
    ElMessage.success('所有任务已暂停')
    loadTasks()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('暂停任务失败')
    }
  }
}

const clearCompletedTasks = async () => {
  try {
    await ElMessageBox.confirm('确定要清理所有已完成的任务吗？', '确认清理', { type: 'warning' })
    
    await taskApi.clearCompletedTasks({ status: 'COMPLETED' })
    
    ElMessage.success('已完成任务已清理')
    loadTasks()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('清理任务失败')
    }
  }
}

const showTaskDetail = (task: any) => {
  selectedTask.value = task
  detailDialogVisible.value = true
}

const handleQuickAction = (command: string) => {
  createTaskType.value = command
  createDialogVisible.value = true
}

const handleTaskCreated = (task: any) => {
  loadTasks()
  ElMessage.success('任务创建成功')
}

const handleTaskUpdated = (task: any) => {
  loadTasks()
}

// 辅助方法
const getTaskTitle = (task: any) => {
  // 优先使用任务的名称，如果没有则根据类型生成
  if (task.name) {
    return task.name
  }
  
  const titleMap = {
    BATCH_EVALUATION: '批量批阅任务',
    AI_GENERATION: 'AI生成任务',
    KNOWLEDGE_PROCESSING: '知识库处理任务',
    PAPER_GENERATION: '试卷生成任务',
    RUBRIC_GENERATION: '评分标准生成任务',
    QUESTION_GENERATION: '题目生成任务'
  }
  return titleMap[task.type as keyof typeof titleMap] || '未知任务'
}

const getStatusTagType = (status: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    RUNNING: 'primary',
    PENDING: 'warning',
    COMPLETED: 'success',
    FAILED: 'danger',
    PAUSED: 'info'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status: string) => {
  const textMap = {
    RUNNING: '运行中',
    PENDING: '待处理',
    COMPLETED: '已完成',
    FAILED: '失败',
    PAUSED: '已暂停'
  }
  return textMap[status as keyof typeof textMap] || status
}

const getPreviewComponent = (taskType: string) => {
  // 动态返回不同类型任务的预览组件
  const componentMap = {
    BATCH_EVALUATION: 'EvaluationPreview',
    AI_GENERATION: 'GenerationPreview', 
    KNOWLEDGE_PROCESSING: 'KnowledgePreview'
  }
  return componentMap[taskType as keyof typeof componentMap] || 'div'
}

const formatTime = (time: string) => {
  if (!time) return '--'
  return new Date(time).toLocaleString()
}

// 生命周期
onMounted(() => {
  loadTasks()
  if (autoRefresh.value) {
    startAutoRefresh()
  }
})

onUnmounted(() => {
  stopAutoRefresh()
  // 取消WebSocket订阅
  if (unsubscribe) {
    unsubscribe()
  }
})
</script>

<style scoped>
.task-center-view {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
}

.page-description {
  margin: 0;
  color: #666;
}

.stats-cards {
  margin-bottom: 24px;
}

.stat-card {
  text-align: center;
  border: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.stat-content {
  position: relative;
  padding: 20px;
}

.stat-number {
  font-size: 36px;
  font-weight: bold;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.stat-icon {
  position: absolute;
  top: 16px;
  right: 16px;
  font-size: 24px;
  opacity: 0.3;
}

.stat-card.running .stat-number { color: #409eff; }
.stat-card.pending .stat-number { color: #e6a23c; }
.stat-card.completed .stat-number { color: #67c23a; }
.stat-card.failed .stat-number { color: #f56c6c; }

.toolbar-card {
  margin-bottom: 24px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ws-status {
  display: flex;
  align-items: center;
  gap: 4px;
}

.status-text {
  font-size: 12px;
  color: #666;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.loading-state,
.empty-state {
  padding: 40px 20px;
  text-align: center;
}

.task-list {
  max-height: 800px;
  overflow-y: auto;
}

.task-item {
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 16px;
  transition: all 0.3s;
}

.task-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.task-item.status-running {
  border-left: 4px solid #409eff;
}

.task-item.status-completed {
  border-left: 4px solid #67c23a;
}

.task-item.status-failed {
  border-left: 4px solid #f56c6c;
}

.task-item.status-pending {
  border-left: 4px solid #e6a23c;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.task-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #666;
}

.task-actions {
  display: flex;
  gap: 8px;
}

.task-progress {
  margin-bottom: 16px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.progress-text {
  color: #666;
  font-size: 14px;
}

.progress-percent {
  font-weight: 600;
  color: #409eff;
}

.progress-details {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}

.task-preview {
  margin-bottom: 16px;
}

.task-result {
  margin-bottom: 16px;
}

.result-summary {
  display: flex;
  gap: 24px;
}

.task-error {
  margin-bottom: 16px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .task-center-view {
    padding: 16px;
  }
  
  .toolbar {
    flex-direction: column;
    gap: 16px;
  }
  
  .toolbar-left,
  .toolbar-right {
    flex-wrap: wrap;
  }
  
  .task-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .task-actions {
    align-self: stretch;
    justify-content: flex-end;
  }
  
  .result-summary {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
