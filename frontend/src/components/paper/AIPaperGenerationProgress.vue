<template>
  <div class="ai-paper-generation-progress">
    <el-card>
      <template #header>
        <div class="progress-header">
          <el-icon><MagicStick /></el-icon>
          <span>AI 试卷生成进度</span>
          <el-tag v-if="taskStatus" :type="getTaskStatusType(taskStatus)">
            {{ getTaskStatusText(taskStatus) }}
          </el-tag>
        </div>
      </template>

      <!-- 总体进度 -->
      <div class="generation-progress">
        <el-progress 
          :percentage="progress" 
          :status="progressStatus"
          :stroke-width="12"
        />
        <div class="progress-info">
          <span class="progress-text">{{ progressText }}</span>
          <span class="progress-percentage">{{ progress }}%</span>
        </div>
      </div>

      <!-- 生成步骤 -->
      <div class="generation-steps">
        <el-steps :active="currentStep" finish-status="success">
          <el-step 
            v-for="(step, index) in steps" 
            :key="index"
            :title="step.title"
            :description="step.description"
            :status="getStepStatus(index)"
          />
        </el-steps>
      </div>

      <!-- 生成详情 -->
      <div v-if="generationDetails" class="generation-details">
        <h4>生成详情</h4>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="知识库">
            {{ generationDetails.knowledgeBaseName }}
          </el-descriptions-item>
          <el-descriptions-item label="目标题数">
            {{ generationDetails.targetQuestions }}
          </el-descriptions-item>
          <el-descriptions-item label="已生成题数">
            {{ generationDetails.generatedQuestions }}
          </el-descriptions-item>
          <el-descriptions-item label="预计剩余时间">
            {{ estimatedTimeLeft }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 生成统计 -->
      <div v-if="generationStats" class="generation-stats">
        <h4>题目统计</h4>
        <el-row :gutter="16">
          <el-col v-for="(stat, type) in generationStats.byType" :key="type" :span="6">
            <el-statistic 
              :title="getQuestionTypeText(String(type))" 
              :value="stat.count"
              :suffix="`/ ${stat.target}`"
            />
          </el-col>
        </el-row>
      </div>

      <!-- 错误信息 -->
      <div v-if="errorMessage" class="error-section">
        <el-alert 
          title="生成遇到问题" 
          :description="errorMessage"
          type="error" 
          :closable="false"
          show-icon
        />
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button 
          v-if="taskStatus === 'RUNNING'"
          type="danger" 
          @click="cancelGeneration"
        >
          取消生成
        </el-button>
        <el-button 
          v-if="taskStatus === 'FAILED'"
          type="primary" 
          @click="retryGeneration"
        >
          重新生成
        </el-button>
        <el-button 
          v-if="taskStatus === 'COMPLETED'"
          type="success" 
          @click="viewResult"
        >
          查看结果
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import { paperGenerationApi } from '@/api/paperGeneration'

// Props
interface Props {
  taskId: string
  autoRefresh?: boolean
  refreshInterval?: number
}

const props = withDefaults(defineProps<Props>(), {
  autoRefresh: true,
  refreshInterval: 2000 // 2秒刷新一次
})

// Emits
const emit = defineEmits<{
  completed: [result: any]
  failed: [error: string]
  cancelled: []
}>()

// 响应式数据
const progress = ref(0)
const progressText = ref('准备开始生成...')
const taskStatus = ref<'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'>('PENDING')
const currentStep = ref(0)
const errorMessage = ref('')
const startTime = ref<Date | null>(null)

const generationDetails = reactive({
  knowledgeBaseName: '',
  targetQuestions: 0,
  generatedQuestions: 0,
  paperTitle: ''
})

const generationStats = ref<any>(null)

let refreshTimer: ReturnType<typeof setTimeout> | null = null

// 生成步骤配置
const steps = [
  { title: '分析知识库', description: '分析知识库内容和结构' },
  { title: '提取知识点', description: '提取相关知识点和概念' },
  { title: '生成题目', description: 'AI生成各类型题目' },
  { title: '组织试卷', description: '组织试卷结构和排版' },
  { title: '质量检查', description: '检查题目质量和完整性' }
]

// 计算属性
const progressStatus = computed(() => {
  if (taskStatus.value === 'FAILED') return 'exception'
  if (taskStatus.value === 'COMPLETED') return 'success'
  return undefined
})

const estimatedTimeLeft = computed(() => {
  if (!startTime.value || taskStatus.value !== 'RUNNING') return '--'
  
  const elapsed = Date.now() - startTime.value.getTime()
  const rate = progress.value / elapsed
  const remaining = (100 - progress.value) / rate
  
  const minutes = Math.ceil(remaining / 60000)
  return minutes > 0 ? `约 ${minutes} 分钟` : '即将完成'
})

// 方法
const loadGenerationProgress = async () => {
  try {
    // TODO: 实现API方法后取消注释
    // const progressData = await paperGenerationApi.getGenerationProgress(props.taskId)
    
    // 模拟进度数据
    const progressData = {
      progress: Math.min(progress.value + 10, 100),
      message: '生成中...',
      status: (progress.value >= 100 ? 'COMPLETED' : 'RUNNING') as 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED',
      currentStep: Math.floor(progress.value / 20),
      details: {},
      stats: {},
      error: '',
      result: null
    }
    
    // 更新进度信息
    progress.value = progressData.progress
    progressText.value = progressData.message
    taskStatus.value = progressData.status
    currentStep.value = progressData.currentStep || 0
    
    // 更新生成详情
    if (progressData.details) {
      Object.assign(generationDetails, progressData.details)
    }
    
    // 更新统计信息
    if (progressData.stats) {
      generationStats.value = progressData.stats
    }
    
    // 更新错误信息
    errorMessage.value = progressData.error || ''
    
    // 设置开始时间
    if (!startTime.value && progressData.status === 'RUNNING') {
      startTime.value = new Date()
    }
    
    // 发出事件
    if (progressData.status === 'COMPLETED') {
      emit('completed', progressData.result)
      stopAutoRefresh()
    } else if (progressData.status === 'FAILED') {
      emit('failed', progressData.error || '生成失败')
      stopAutoRefresh()
    }
    
  } catch (error) {
    console.error('加载生成进度失败:', error)
    // 静默失败，避免频繁错误提示
  }
}

const cancelGeneration = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要取消当前的试卷生成任务吗？',
      '确认取消',
      { type: 'warning' }
    )
    
    // TODO: 实现API方法后取消注释
    // await paperGenerationApi.cancelGeneration(props.taskId)
    taskStatus.value = 'CANCELLED'
    emit('cancelled')
    stopAutoRefresh()
    ElMessage.success('已取消生成任务')
    
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('取消生成失败:', error)
      ElMessage.error('取消生成失败')
    }
  }
}

const retryGeneration = async () => {
  try {
    // TODO: 实现API方法后取消注释
    // const newTaskId = await paperGenerationApi.retryGeneration(props.taskId)
    // 可以通过事件通知父组件切换到新的taskId
    ElMessage.success('重新生成已开始')
  } catch (error) {
    console.error('重新生成失败:', error)
    ElMessage.error('重新生成失败')
  }
}

const viewResult = () => {
  // 跳转到生成结果页面
  window.open(`/paper-generation/result/${props.taskId}`, '_blank')
}

const getTaskStatusType = (status: string) => {
  switch (status) {
    case 'RUNNING': return 'warning'
    case 'COMPLETED': return 'success'
    case 'FAILED': return 'danger'
    case 'CANCELLED': return 'info'
    default: return 'info'
  }
}

const getTaskStatusText = (status: string) => {
  switch (status) {
    case 'PENDING': return '等待中'
    case 'RUNNING': return '生成中'
    case 'COMPLETED': return '已完成'
    case 'FAILED': return '生成失败'
    case 'CANCELLED': return '已取消'
    default: return status
  }
}

const getStepStatus = (stepIndex: number) => {
  if (stepIndex < currentStep.value) return 'finish'
  if (stepIndex === currentStep.value && taskStatus.value === 'RUNNING') return 'process'
  if (stepIndex === currentStep.value && taskStatus.value === 'FAILED') return 'error'
  return 'wait'
}

const getQuestionTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    'MULTIPLE_CHOICE': '选择题',
    'SHORT_ANSWER': '简答题',
    'ESSAY': '论述题',
    'TRUE_FALSE': '判断题',
    'CODING': '编程题'
  }
  return typeMap[type] || type
}

const startAutoRefresh = () => {
  if (props.autoRefresh && !refreshTimer) {
    refreshTimer = setInterval(() => {
      if (['PENDING', 'RUNNING'].includes(taskStatus.value)) {
        loadGenerationProgress()
      }
    }, props.refreshInterval)
  }
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

// 生命周期
onMounted(() => {
  if (props.taskId) {
    loadGenerationProgress()
    startAutoRefresh()
  }
})

onUnmounted(() => {
  stopAutoRefresh()
})

// 监听 taskId 变化
watch(() => props.taskId, (newTaskId) => {
  if (newTaskId) {
    // 重置状态
    progress.value = 0
    taskStatus.value = 'PENDING'
    currentStep.value = 0
    errorMessage.value = ''
    startTime.value = null
    
    // 重新开始监控
    loadGenerationProgress()
    startAutoRefresh()
  }
})

// 暴露方法给父组件
defineExpose({
  refresh: loadGenerationProgress,
  cancel: cancelGeneration,
  startAutoRefresh,
  stopAutoRefresh
})
</script>

<style scoped>
.ai-paper-generation-progress {
  width: 100%;
}

.progress-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.progress-header > span {
  font-weight: 600;
  margin-left: 8px;
}

.generation-progress {
  margin-bottom: 24px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.progress-text {
  font-size: 14px;
  color: #666;
}

.progress-percentage {
  font-size: 16px;
  font-weight: 600;
  color: #409eff;
}

.generation-steps {
  margin-bottom: 24px;
}

.generation-details,
.generation-stats {
  margin-bottom: 24px;
}

.generation-details h4,
.generation-stats h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #303133;
}

.error-section {
  margin-bottom: 24px;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 12px;
  border-top: 1px solid #ebeef5;
  padding-top: 20px;
}
</style>
