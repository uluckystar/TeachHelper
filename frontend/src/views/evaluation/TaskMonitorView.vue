<template>
  <div class="task-monitor">
    <div class="page-header">
      <h1>任务监控</h1>
      <p class="page-description">查看和管理AI批阅任务的执行状态</p>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="32" color="#409eff"><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pending || 0 }}</div>
              <div class="stat-label">待处理</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="32" color="#e6a23c"><Loading /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.running || 0 }}</div>
              <div class="stat-label">运行中</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="32" color="#67c23a"><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.completed || 0 }}</div>
              <div class="stat-label">已完成</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="32" color="#f56c6c"><CircleClose /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.failed || 0 }}</div>
              <div class="stat-label">失败</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 过滤器和操作 -->
    <el-card class="filter-card">
      <el-row :gutter="16">
        <el-col :span="6">
          <el-select v-model="filters.status" placeholder="任务状态" clearable @change="loadTasks">
            <el-option label="全部" value="" />
            <el-option label="待处理" value="PENDING" />
            <el-option label="运行中" value="RUNNING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="失败" value="FAILED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="filters.taskType" placeholder="任务类型" clearable @change="loadTasks">
            <el-option label="全部" value="" />
            <el-option label="完整批阅" value="FULL_EVALUATION" />
            <el-option label="快速批阅" value="QUICK_EVALUATION" />
            <el-option label="标准批阅" value="RUBRIC_BASED" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            @change="loadTasks"
          />
        </el-col>
        <el-col :span="6">
          <div class="filter-actions">
            <el-button type="primary" @click="loadTasks" :loading="loading">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
            <el-button @click="clearFilters">清除筛选</el-button>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 任务列表 -->
    <el-card class="tasks-card">
      <template #header>
        <div class="card-header">
          <span>批阅任务列表</span>
          <el-button type="primary" @click="goToCreateTask">
            <el-icon><Plus /></el-icon>
            新建任务
          </el-button>
        </div>
      </template>

      <el-table :data="tasks" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="任务ID" width="100" />
        <el-table-column prop="examTitle" label="考试名称">
          <template #default="{ row }">
            {{ row.examTitle || row.name || row.description || '未知任务' }}
          </template>
        </el-table-column>
        <el-table-column prop="taskType" label="任务类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTaskTypeTag(row.taskType || row.type)">
              {{ getTaskTypeText(row.taskType || row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="200">
          <template #default="{ row }">
            <div class="progress-info">
              <el-progress 
                :percentage="row.progress || 0" 
                :status="getProgressStatus(row.status)"
                size="small"
              />
              <span class="progress-text">
                {{ row.processedCount || row.successfulEvaluations || 0 }} / {{ row.totalCount || row.totalAnswers || 0 }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="用时" width="100">
          <template #default="{ row }">
            {{ getDuration(row.createdAt, row.completedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button size="small" @click="viewTaskDetail(row)">
                详情
              </el-button>
              <el-button 
                size="small" 
                type="primary" 
                @click="viewResults(row)"
                :disabled="row.status !== 'COMPLETED'"
              >
                结果
              </el-button>
              <el-button 
                size="small" 
                type="warning" 
                @click="pauseTask(row)"
                :disabled="!['PENDING', 'RUNNING'].includes(row.status)"
                v-if="row.status === 'RUNNING'"
              >
                暂停
              </el-button>
              <el-button 
                size="small" 
                type="success" 
                @click="resumeTask(row)"
                :disabled="row.status !== 'PAUSED'"
                v-if="row.status === 'PAUSED'"
              >
                恢复
              </el-button>
              <el-button 
                size="small" 
                type="danger" 
                @click="cancelTask(row)"
                :disabled="!['PENDING', 'RUNNING', 'PAUSED'].includes(row.status)"
              >
                取消
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadTasks"
          @current-change="loadTasks"
        />
      </div>
    </el-card>

    <!-- 任务详情对话框 -->
    <el-dialog v-model="showDetailDialog" title="任务详情" width="800px">
      <div v-if="selectedTask" class="task-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务ID">{{ selectedTask.id }}</el-descriptions-item>
          <el-descriptions-item label="任务状态">
            <el-tag :type="getStatusTag(selectedTask.status)">
              {{ getStatusText(selectedTask.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="任务类型">
            {{ getTaskTypeText(selectedTask.taskType) }}
          </el-descriptions-item>
          <el-descriptions-item label="关联考试">{{ selectedTask.examTitle }}</el-descriptions-item>
          <el-descriptions-item label="AI配置">{{ selectedTask.aiConfigName }}</el-descriptions-item>
          <el-descriptions-item label="批阅标准">{{ selectedTask.rubricName || '无' }}</el-descriptions-item>
          <el-descriptions-item label="批量大小">{{ selectedTask.batchSize }}</el-descriptions-item>
          <el-descriptions-item label="并发数">{{ selectedTask.concurrency }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(selectedTask.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ formatDate(selectedTask.startedAt) }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ formatDate(selectedTask.completedAt) }}</el-descriptions-item>
          <el-descriptions-item label="总用时">
            {{ getDuration(selectedTask.createdAt, selectedTask.completedAt) }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="progress-section" v-if="selectedTask.status === 'RUNNING'">
          <h4>执行进度</h4>
          <el-progress 
            :percentage="selectedTask.progress || 0" 
            :status="getProgressStatus(selectedTask.status)"
          />
          <p>已处理: {{ selectedTask.processedCount || 0 }} / {{ selectedTask.totalCount || 0 }}</p>
        </div>

        <div class="description-section" v-if="selectedTask.description">
          <h4>任务描述</h4>
          <p>{{ selectedTask.description }}</p>
        </div>

        <div class="error-section" v-if="selectedTask.errorMessage">
          <h4>错误信息</h4>
          <el-alert type="error" :closable="false">
            {{ selectedTask.errorMessage }}
          </el-alert>
        </div>

        <div class="logs-section" v-if="selectedTask.logs && selectedTask.logs.length > 0">
          <h4>执行日志</h4>
          <div class="logs-container">
            <div v-for="log in selectedTask.logs" :key="log.id" class="log-item">
              <span class="log-time">{{ formatDate(log.timestamp) }}</span>
              <span :class="['log-level', `log-${log.level.toLowerCase()}`]">{{ log.level }}</span>
              <span class="log-message">{{ log.message }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Clock, Loading, CircleCheck, CircleClose, 
  Refresh, Plus 
} from '@element-plus/icons-vue'
import { taskApi } from '@/api/task'
import { useTaskWebSocket, useTaskUpdates } from '@/utils/taskWebSocket'

const router = useRouter()

// WebSocket实时更新
const { isConnected } = useTaskWebSocket()
const { taskUpdates, unsubscribe } = useTaskUpdates()

// 状态管理
const loading = ref(false)
const showDetailDialog = ref(false)
const selectedTask = ref<any>(null)

// 数据
const tasks = ref<any[]>([])
const stats = ref({
  pending: 0,
  running: 0,
  completed: 0,
  failed: 0
})

// 过滤器
const filters = reactive({
  status: '',
  taskType: '',
  dateRange: [] as [Date, Date] | []
})

// 分页
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 自动刷新定时器
let refreshTimer: number | null = null

// 方法
const loadTasks = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1,
      size: pagination.size,
      status: filters.status || undefined,
      type: filters.taskType || undefined,
      startDate: filters.dateRange.length > 0 ? filters.dateRange[0]?.toISOString() : undefined,
      endDate: filters.dateRange.length > 0 ? filters.dateRange[1]?.toISOString() : undefined
    }

    const response = await taskApi.getTasks(params)
    tasks.value = response?.content || response || []
    pagination.total = response?.totalElements || tasks.value.length
  } catch (error) {
    console.error('加载任务列表失败:', error)
    // 使用mock数据作为后备
    tasks.value = [
      {
        id: 'task-001',
        examTitle: '人工智能基础考试',
        taskType: 'FULL_EVALUATION',
        status: 'COMPLETED',
        progress: 100,
        processedCount: 45,
        totalCount: 45,
        createdAt: new Date(Date.now() - 3600000).toISOString(),
        completedAt: new Date().toISOString()
      },
      {
        id: 'task-002',
        examTitle: '机器学习中级考试',
        taskType: 'QUICK_EVALUATION',
        status: 'RUNNING',
        progress: 65,
        processedCount: 26,
        totalCount: 40,
        createdAt: new Date(Date.now() - 1800000).toISOString()
      },
      {
        id: 'task-003',
        examTitle: '数据结构期末考试',
        taskType: 'RUBRIC_BASED',
        status: 'PENDING',
        progress: 0,
        processedCount: 0,
        totalCount: 80,
        createdAt: new Date(Date.now() - 300000).toISOString()
      }
    ]
    pagination.total = tasks.value.length
    ElMessage.warning('使用模拟数据，请确保后端服务正常运行')
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    const response = await taskApi.getTaskStatistics()
    stats.value = response || stats.value
  } catch (error) {
    console.error('加载统计信息失败:', error)
    // 使用mock数据
    stats.value = {
      pending: 3,
      running: 2,
      completed: 15,
      failed: 1
    }
  }
}

const clearFilters = () => {
  filters.status = ''
  filters.taskType = ''
  filters.dateRange = []
  loadTasks()
}

const viewTaskDetail = async (task: any) => {
  try {
    const taskId = task.taskId || task.id
    if (!taskId || taskId === 'undefined') {
      ElMessage.error('任务ID无效')
      return
    }
    
    const response = await taskApi.getTaskDetail(taskId)
    selectedTask.value = response
    showDetailDialog.value = true
  } catch (error) {
    console.error('加载任务详情失败:', error)
    ElMessage.error('加载任务详情失败')
  }
}

const viewResults = (task: any) => {
  const taskId = task.taskId || task.id
  if (!taskId || taskId === 'undefined') {
    ElMessage.error('任务ID无效')
    return
  }
  router.push(`/tasks/${taskId}/results`)
}

const pauseTask = async (task: any) => {
  try {
    await ElMessageBox.confirm('确定要暂停此任务吗？', '确认暂停', {
      type: 'warning'
    })
    
    const taskId = task.taskId || task.id
    await taskApi.pauseTask(taskId)
    ElMessage.success('任务已暂停')
    // 移除loadTasks()调用，依赖实时更新
    loadStats()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('暂停任务失败:', error)
      ElMessage.error('暂停任务失败')
    }
  }
}

const resumeTask = async (task: any) => {
  try {
    const taskId = task.taskId || task.id
    await taskApi.resumeTask(taskId)
    ElMessage.success('任务已恢复')
    // 移除loadTasks()调用，依赖实时更新
    loadStats()
  } catch (error) {
    console.error('恢复任务失败:', error)
    ElMessage.error('恢复任务失败')
  }
}

const cancelTask = async (task: any) => {
  try {
    await ElMessageBox.confirm('确定要取消此任务吗？', '确认取消', {
      type: 'warning'
    })
    
    const taskId = task.taskId || task.id
    await taskApi.cancelTask(taskId)
    ElMessage.success('任务已取消')
    // 移除loadTasks()调用，依赖实时更新
    loadStats()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消任务失败:', error)
      ElMessage.error('取消任务失败')
    }
  }
}

const goToCreateTask = () => {
  router.push('/batch-evaluation')
}

// 辅助方法
const getTaskTypeTag = (type: string) => {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    'FULL_EVALUATION': 'primary',
    'QUICK_EVALUATION': 'success',
    'RUBRIC_BASED': 'warning',
    'BATCH_EVALUATION': 'primary',
    'BATCH_EVALUATION_ANSWERS': 'primary',
    'BATCH_EVALUATION_QUESTION': 'info',
    'BATCH_EVALUATION_ALL_QUESTION': 'warning',
    'BATCH_REVALUATION_QUESTION': 'danger'
  }
  return map[type] || 'info'
}

const getTaskTypeText = (type: string) => {
  const map: Record<string, string> = {
    'FULL_EVALUATION': '完整批阅',
    'QUICK_EVALUATION': '快速批阅',
    'RUBRIC_BASED': '标准批阅',
    'BATCH_EVALUATION': '批量批阅',
    'BATCH_EVALUATION_ANSWERS': '批量批阅答案',
    'BATCH_EVALUATION_QUESTION': '批阅题目答案',
    'BATCH_EVALUATION_ALL_QUESTION': '批阅题目所有答案',
    'BATCH_REVALUATION_QUESTION': '重新批阅题目'
  }
  return map[type] || type
}

const getStatusTag = (status: string) => {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    'PENDING': 'info',
    'RUNNING': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger',
    'CANCELLED': 'info',
    'PAUSED': 'warning'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'PENDING': '待处理',
    'RUNNING': '运行中',
    'COMPLETED': '已完成',
    'FAILED': '失败',
    'CANCELLED': '已取消',
    'PAUSED': '已暂停'
  }
  return map[status] || status
}

const getProgressStatus = (status: string) => {
  const map: Record<string, 'success' | 'exception' | undefined> = {
    'COMPLETED': 'success',
    'FAILED': 'exception'
  }
  return map[status]
}

const formatDate = (dateString: string) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

const getDuration = (startTime: string, endTime?: string) => {
  if (!startTime) return '-'
  
  const start = new Date(startTime).getTime()
  const end = endTime ? new Date(endTime).getTime() : Date.now()
  const duration = Math.floor((end - start) / 1000)
  
  if (duration < 60) return `${duration}秒`
  if (duration < 3600) return `${Math.floor(duration / 60)}分钟`
  return `${Math.floor(duration / 3600)}小时${Math.floor((duration % 3600) / 60)}分钟`
}

// 监听WebSocket任务更新
const handleTaskUpdate = (taskId: string, update: any) => {
  const taskIndex = tasks.value.findIndex(t => (t.taskId || t.id) === taskId)
  if (taskIndex > -1) {
    // 更新现有任务
    tasks.value[taskIndex] = { 
      ...tasks.value[taskIndex], 
      ...update,
      updatedAt: new Date().toISOString()
    }
  }
  
  // 实时更新统计
  loadStats()
}

// 监听WebSocket更新
watch(taskUpdates, (updates) => {
  Object.entries(updates).forEach(([taskId, update]) => {
    handleTaskUpdate(taskId, update)
  })
}, { deep: true })

// 设置自动刷新 - 降低频率，主要依赖WebSocket
const startAutoRefresh = () => {
  refreshTimer = window.setInterval(() => {
    loadTasks()
    loadStats()
  }, 15000) // 改为每15秒刷新一次，主要用于兜底
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => {
  loadTasks()
  loadStats()
  startAutoRefresh()
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
.task-monitor {
  max-width: 1400px;
  margin: 0 auto;
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
  font-size: 14px;
}

.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  border: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.stat-content {
  display: flex;
  align-items: center;
  padding: 20px;
}

.stat-icon {
  margin-right: 16px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.filter-card {
  margin-bottom: 24px;
  position: relative;
  z-index: 1;
}

.filter-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.filter-actions .el-button {
  margin: 0;
}

.tasks-card {
  background: white;
  position: relative;
  z-index: 1;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.progress-text {
  font-size: 12px;
  color: #666;
  text-align: center;
}

.pagination {
  margin-top: 16px;
  text-align: right;
}

.task-detail {
  max-height: 600px;
  overflow-y: auto;
}

.progress-section,
.description-section,
.error-section,
.logs-section {
  margin-top: 24px;
}

.progress-section h4,
.description-section h4,
.error-section h4,
.logs-section h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
}

.logs-container {
  max-height: 200px;
  overflow-y: auto;
  background: #f5f7fa;
  border-radius: 4px;
  padding: 12px;
}

.log-item {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
}

.log-time {
  color: #909399;
  min-width: 140px;
}

.log-level {
  min-width: 60px;
  font-weight: 600;
}

.log-info { color: #409eff; }
.log-warn { color: #e6a23c; }
.log-error { color: #f56c6c; }
.log-debug { color: #909399; }

.log-message {
  flex: 1;
  color: #303133;
}
</style>