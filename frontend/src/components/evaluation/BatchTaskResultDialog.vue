<template>
  <el-dialog
    v-model="visible"
    title="批量评估结果详情"
    width="70%"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="batch-task-result-dialog" v-if="taskResult">
      <!-- 任务概览 -->
      <el-card class="task-overview-card">
        <template #header>
          <span>任务概览</span>
        </template>
        <div class="task-overview">
          <el-row :gutter="20">
            <el-col :span="6">
              <div class="overview-item">
                <div class="item-label">任务ID</div>
                <div class="item-value">{{ taskResult.taskId }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="overview-item">
                <div class="item-label">任务状态</div>
                <div class="item-value">
                  <el-tag :type="(getStatusTag(taskResult.status) as any)">
                    {{ getStatusText(taskResult.status) }}
                  </el-tag>
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="overview-item">
                <div class="item-label">开始时间</div>
                <div class="item-value">{{ taskResult.startTime ? formatDate(taskResult.startTime) : '-' }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="overview-item">
                <div class="item-label">结束时间</div>
                <div class="item-value">
                  {{ taskResult.endTime ? formatDate(taskResult.endTime) : '-' }}
                </div>
              </div>
            </el-col>
          </el-row>
          
          <el-row :gutter="20" style="margin-top: 20px">
            <el-col :span="6">
              <div class="overview-item">
                <div class="item-label">总答案数</div>
                <div class="item-value highlight">{{ taskResult.totalAnswers }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="overview-item">
                <div class="item-label">成功评估</div>
                <div class="item-value success">{{ taskResult.successfulEvaluations }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="overview-item">
                <div class="item-label">失败评估</div>
                <div class="item-value error">{{ taskResult.failedEvaluations }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="overview-item">
                <div class="item-label">成功率</div>
                <div class="item-value">{{ taskResult.successRate ? taskResult.successRate.toFixed(1) : '0' }}%</div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-card>

      <!-- 进度可视化 -->
      <el-card class="progress-card">
        <template #header>
          <span>评估进度</span>
        </template>
        <div class="progress-visualization">
          <div class="progress-info">
            <div class="progress-label">
              完成进度：{{ taskResult.successfulEvaluations + taskResult.failedEvaluations }} / {{ taskResult.totalAnswers }}
            </div>
            <div class="progress-percentage">
              {{ getTaskProgress(taskResult) }}%
            </div>
          </div>
          <el-progress 
            :percentage="getTaskProgress(taskResult)"
            :status="getProgressStatus(taskResult.status)"
            :stroke-width="20"
          />
          
          <div class="progress-breakdown">
            <div class="breakdown-item success">
              <div class="breakdown-color"></div>
              <span>成功：{{ taskResult.successfulEvaluations }}</span>
            </div>
            <div class="breakdown-item error">
              <div class="breakdown-color"></div>
              <span>失败：{{ taskResult.failedEvaluations }}</span>
            </div>
            <div class="breakdown-item pending">
              <div class="breakdown-color"></div>
              <span>待处理：{{ taskResult.totalAnswers - taskResult.successfulEvaluations - taskResult.failedEvaluations }}</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 性能统计 -->
      <el-card class="performance-card" v-if="taskResult.endTime">
        <template #header>
          <span>性能统计</span>
        </template>
        <div class="performance-stats">
          <el-row :gutter="20">
            <el-col :span="8">
              <div class="stat-item">
                <div class="stat-label">总耗时</div>
                <div class="stat-value">{{ taskResult.durationInMillis ? formatDuration(taskResult.durationInMillis) : '-' }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-item">
                <div class="stat-label">平均耗时</div>
                <div class="stat-value">
                  {{ taskResult.totalAnswers > 0 && taskResult.durationInMillis ? formatDuration(taskResult.durationInMillis / taskResult.totalAnswers) : '-' }}
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-item">
                <div class="stat-label">评估速度</div>
                <div class="stat-value">
                  {{ taskResult.durationInMillis && taskResult.durationInMillis > 0 ? (taskResult.totalAnswers / (taskResult.durationInMillis / 1000 / 60)).toFixed(1) : '-' }} 个/分钟
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-card>

      <!-- 错误信息 -->
      <el-card 
        class="errors-card" 
        v-if="taskResult.errors && taskResult.errors.length > 0"
      >
        <template #header>
          <div class="card-header">
            <span>错误信息 ({{ taskResult.errors.length }})</span>
            <el-button 
              type="warning" 
              size="small"
              @click="exportErrors"
            >
              导出错误日志
            </el-button>
          </div>
        </template>
        <div class="errors-list">
          <div 
            v-for="(error, index) in taskResult.errors" 
            :key="index"
            class="error-item"
          >
            <div class="error-index">#{{ index + 1 }}</div>
            <div class="error-message">{{ error }}</div>
          </div>
        </div>
      </el-card>

      <!-- 操作建议 -->
      <el-card class="suggestions-card">
        <template #header>
          <span>操作建议</span>
        </template>
        <div class="suggestions">
          <div v-if="taskResult.status === 'COMPLETED'" class="suggestion-item success">
            <el-icon><SuccessFilled /></el-icon>
            <span>所有答案评估完成！可以查看评估结果或进行进一步分析。</span>
          </div>
          
          <div v-else-if="taskResult.status === 'COMPLETED_WITH_ERRORS'" class="suggestion-item warning">
            <el-icon><WarningFilled /></el-icon>
            <span>
              大部分答案已完成评估，但有 {{ taskResult.failedEvaluations }} 个答案评估失败。
              建议检查错误信息并手动处理失败的答案。
            </span>
          </div>
          
          <div v-else-if="taskResult.status === 'FAILED'" class="suggestion-item error">
            <el-icon><CircleCloseFilled /></el-icon>
            <span>批量评估任务失败。建议检查错误信息并重新尝试评估。</span>
          </div>
          
          <div v-else-if="taskResult.status === 'IN_PROGRESS'" class="suggestion-item info">
            <el-icon><Loading /></el-icon>
            <span>任务正在进行中，请耐心等待。可以关闭此对话框，任务会在后台继续执行。</span>
          </div>
          
          <div class="suggestion-actions">
            <el-button 
              v-if="taskResult.status === 'COMPLETED' || taskResult.status === 'COMPLETED_WITH_ERRORS'"
              type="primary"
              @click="viewDetailedResults"
            >
              查看详细结果
            </el-button>
            <el-button 
              v-if="taskResult.failedEvaluations > 0"
              type="warning"
              @click="retryFailedEvaluations"
            >
              重试失败的评估
            </el-button>
          </div>
        </div>
      </el-card>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  SuccessFilled, 
  WarningFilled, 
  CircleCloseFilled, 
  Loading 
} from '@element-plus/icons-vue'

import { getStatusTag } from '@/utils/tagTypes'
import type { BatchEvaluationResult } from '@/types/api'

// Props和Emits
interface Props {
  modelValue: boolean
  taskResult: BatchEvaluationResult | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

// 计算属性
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 方法
const handleClose = () => {
  emit('update:modelValue', false)
}

const getStatusType = (status: string) => {
  const statusMap: Record<string, string> = {
    PENDING: 'info',
    IN_PROGRESS: 'warning',
    COMPLETED: 'success',
    COMPLETED_WITH_ERRORS: 'warning',
    FAILED: 'danger'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    PENDING: '等待中',
    IN_PROGRESS: '评估中',
    COMPLETED: '已完成',
    COMPLETED_WITH_ERRORS: '完成(有错误)',
    FAILED: '失败'
  }
  return statusMap[status] || status
}

const getTaskProgress = (task: BatchEvaluationResult) => {
  if (task.totalAnswers === 0) return 100
  return Math.round(((task.successfulEvaluations + task.failedEvaluations) / task.totalAnswers) * 100)
}

const getProgressStatus = (status: string) => {
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'exception'
  if (status === 'COMPLETED_WITH_ERRORS') return 'warning'
  return undefined
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN')
}

const formatDuration = (milliseconds: number) => {
  const seconds = Math.floor(milliseconds / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  
  if (hours > 0) {
    return `${hours}小时${minutes % 60}分钟${seconds % 60}秒`
  } else if (minutes > 0) {
    return `${minutes}分钟${seconds % 60}秒`
  } else {
    return `${seconds}秒`
  }
}

const exportErrors = () => {
  if (!props.taskResult?.errors) return
  
  const errorText = props.taskResult.errors.join('\n')
  const blob = new Blob([errorText], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  
  link.href = url
  link.download = `batch_evaluation_errors_${props.taskResult.taskId}.txt`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  
  ElMessage.success('错误日志已导出')
}

const viewDetailedResults = () => {
  ElMessage.info('详细结果查看功能开发中...')
}

const retryFailedEvaluations = () => {
  ElMessage.info('重试失败评估功能开发中...')
}
</script>

<style scoped>
.batch-task-result-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-overview {
  padding: 10px 0;
}

.overview-item {
  text-align: center;
  padding: 10px 0;
}

.item-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.item-value {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.item-value.highlight {
  color: #409eff;
  font-size: 18px;
  font-weight: 600;
}

.item-value.success {
  color: #67c23a;
  font-weight: 600;
}

.item-value.error {
  color: #f56c6c;
  font-weight: 600;
}

.progress-visualization {
  padding: 10px 0;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.progress-label {
  font-size: 14px;
  color: #606266;
}

.progress-percentage {
  font-size: 18px;
  font-weight: 600;
  color: #409eff;
}

.progress-breakdown {
  display: flex;
  justify-content: center;
  gap: 30px;
  margin-top: 20px;
}

.breakdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.breakdown-color {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.breakdown-item.success .breakdown-color {
  background-color: #67c23a;
}

.breakdown-item.error .breakdown-color {
  background-color: #f56c6c;
}

.breakdown-item.pending .breakdown-color {
  background-color: #e4e7ed;
}

.performance-stats {
  padding: 10px 0;
}

.stat-item {
  text-align: center;
  padding: 15px 0;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.errors-list {
  max-height: 300px;
  overflow-y: auto;
}

.error-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: #fef0f0;
  border-left: 3px solid #f56c6c;
  margin-bottom: 8px;
  border-radius: 4px;
}

.error-index {
  font-weight: 600;
  color: #f56c6c;
  min-width: 30px;
}

.error-message {
  flex: 1;
  color: #606266;
  line-height: 1.5;
  word-break: break-word;
}

.suggestions {
  padding: 10px 0;
}

.suggestion-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 15px;
  border-radius: 6px;
  margin-bottom: 15px;
  line-height: 1.6;
}

.suggestion-item.success {
  background: #f0f9ff;
  border-left: 3px solid #67c23a;
  color: #67c23a;
}

.suggestion-item.warning {
  background: #fdf6ec;
  border-left: 3px solid #e6a23c;
  color: #e6a23c;
}

.suggestion-item.error {
  background: #fef0f0;
  border-left: 3px solid #f56c6c;
  color: #f56c6c;
}

.suggestion-item.info {
  background: #f4f4f5;
  border-left: 3px solid #909399;
  color: #909399;
}

.suggestion-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}
</style>
