<template>
  <div class="ai-evaluation-view">
    <!-- 页面标题 -->
    <div class="page-header">
      <el-page-header @back="handleBack">
        <template #content>
          <span class="page-title">AI智能批阅管理</span>
        </template>
      </el-page-header>
    </div>

    <!-- 考试信息卡片 -->
    <el-card class="exam-info-card" v-if="examInfo">
      <template #header>
        <div class="card-header">
          <span>考试信息</span>
          <el-tag :type="getExamStatusType(examInfo.status || 'active') as any">>
            {{ getExamStatusText(examInfo.status || 'active') }}
          </el-tag>
        </div>
      </template>
      <div class="exam-info">
        <h3>{{ examInfo.title }}</h3>
        <p class="exam-description">{{ examInfo.description }}</p>
        <div class="exam-meta">
          <el-row :gutter="16">
            <el-col :span="8">
              <div class="meta-item">
                <el-icon><Document /></el-icon>
                <span>题目数量：{{ questions.length }}</span>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="meta-item">
                <el-icon><User /></el-icon>
                <span>创建者：{{ examInfo.createdBy }}</span>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="meta-item">
                <el-icon><Calendar /></el-icon>
                <span>创建时间：{{ formatDate(examInfo.createdAt) }}</span>
              </div>
            </el-col>
          </el-row>
        </div>
      </div>
    </el-card>

    <!-- 批量操作工具栏 -->
    <el-card class="batch-operations-card">
      <template #header>
        <div class="card-header">
          <span>批量操作</span>
          <div class="header-actions">
            <el-button 
              type="primary" 
              :icon="MagicStick" 
              @click="handleBatchEvaluateExam"
              :loading="batchLoading"
              :disabled="!hasUnevaluatedAnswers"
            >
              一键批阅整个考试
            </el-button>
            <el-button 
              :icon="Refresh" 
              @click="refreshData"
              :loading="loading"
            >
              刷新数据
            </el-button>
          </div>
        </div>
      </template>

      <div class="batch-operations">
        <!-- 题目选择区域 -->
        <div class="question-selection-area">
          <h4>选择要批阅的题目：</h4>
          <div class="selection-controls">
            <el-button 
              size="small" 
              @click="selectAllQuestions"
              :disabled="questions.length === 0"
            >
              全选
            </el-button>
            <el-button 
              size="small" 
              @click="clearQuestionSelection"
              :disabled="selectedQuestions.length === 0"
            >
              清空
            </el-button>
            <el-button 
              size="small" 
              type="primary"
              @click="batchEvaluateSelected"
              :loading="batchLoading"
              :disabled="selectedQuestions.length === 0"
            >
              批阅选中题目
            </el-button>
            <span class="selection-info">
              已选择 {{ selectedQuestions.length }} / {{ questions.length }} 个题目
            </span>
          </div>
          
          <div class="question-checkboxes">
            <el-checkbox-group v-model="selectedQuestions">
              <el-checkbox 
                v-for="(question, index) in questions" 
                :key="question.id"
                :label="question.id"
                :disabled="questionStats[question.id]?.unevaluatedAnswers === 0"
              >
                <span class="question-checkbox-label">
                  题目{{ index + 1 }}: {{ question.title }}
                  <el-tag 
                    v-if="questionStats[question.id]?.unevaluatedAnswers === 0"
                    type="success" 
                    size="small"
                  >
                    已完成
                  </el-tag>
                  <el-tag 
                    v-else-if="questionStats[question.id]?.unevaluatedAnswers > 0"
                    type="warning" 
                    size="small"
                  >
                    待批阅: {{ questionStats[question.id]?.unevaluatedAnswers }}
                  </el-tag>
                </span>
              </el-checkbox>
            </el-checkbox-group>
          </div>
        </div>

        <el-divider />

        <el-alert
          v-if="!hasUnevaluatedAnswers"
          title="所有答案已批阅完成"
          type="success"
          :closable="false"
          show-icon
        />
        <el-alert
          v-else
          :title="`还有 ${totalUnevaluatedAnswers} 个答案未批阅`"
          type="warning"
          :closable="false"
          show-icon
        />

        <!-- 批量批阅任务状态 -->
        <div v-if="activeBatchTask" class="batch-task-status">
          <el-card>
            <template #header>
              <div class="task-header">
                <span>批量批阅进行中</span>
                <el-button 
                  link
                  :icon="Close" 
                  @click="clearBatchTask"
                  size="small"
                />
              </div>
            </template>
            <div class="task-content">
              <div class="task-info">
                <p><strong>任务ID:</strong> {{ activeBatchTask.taskId }}</p>
                <p><strong>状态:</strong> 
                  <el-tag :type="getTaskStatusType(activeBatchTask.status) as any">>
                    {{ getTaskStatusText(activeBatchTask.status) }}
                  </el-tag>
                </p>
                <p><strong>进度:</strong> {{ activeBatchTask.successfulEvaluations + activeBatchTask.failedEvaluations }} / {{ activeBatchTask.totalAnswers }}</p>
              </div>
              <div class="task-progress">
                <el-progress 
                  :percentage="getTaskProgress(activeBatchTask)"
                  :status="getProgressStatus(activeBatchTask.status)"
                />
              </div>
              <div class="task-actions" v-if="activeBatchTask.status === 'COMPLETED' || activeBatchTask.status === 'COMPLETED_WITH_ERRORS'">
                <el-button type="success" @click="viewTaskResult" size="small">
                  查看结果详情
                </el-button>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </el-card>

    <!-- 题目列表 -->
    <el-card class="questions-card">
      <template #header>
        <div class="card-header">
          <span>题目批阅状态</span>
          <div class="header-actions">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索题目..."
              :prefix-icon="Search"
              clearable
              style="width: 200px"
            />
          </div>
        </div>
      </template>

      <div class="questions-list" v-loading="loading">
        <div 
          v-for="question in filteredQuestions" 
          :key="question.id" 
          class="question-item"
        >
          <el-card shadow="hover">
            <div class="question-header">
              <div class="question-title">
                <h4>{{ question.title }}</h4>
                <el-tag 
                  :type="getQuestionTypeColor(question.questionType) as any"
                  size="small"
                >
                  {{ getQuestionTypeText(question.questionType) }}
                </el-tag>
              </div>
              <div class="question-actions">
                <el-button 
                  type="primary" 
                  size="small"
                  @click="openQuestionEvaluation(question)"
                >
                  管理批阅
                </el-button>
              </div>
            </div>

            <div class="question-content">
              <p class="content-preview">{{ getContentPreview(question.content) }}</p>
              <div class="question-meta">
                <span class="max-score">满分：{{ question.maxScore }}分</span>
              </div>
            </div>

            <!-- 批阅统计 -->
            <div class="evaluation-stats" v-if="questionStats[question.id]">
              <el-row :gutter="16">
                <el-col :span="6">
                  <div class="stat-item">
                    <div class="stat-number">{{ questionStats[question.id].totalAnswers }}</div>
                    <div class="stat-label">总答案</div>
                  </div>
                </el-col>
                <el-col :span="6">
                  <div class="stat-item">
                    <div class="stat-number">{{ questionStats[question.id].evaluatedAnswers }}</div>
                    <div class="stat-label">已批阅</div>
                  </div>
                </el-col>
                <el-col :span="6">
                  <div class="stat-item">
                    <div class="stat-number">{{ questionStats[question.id].unevaluatedAnswers }}</div>
                    <div class="stat-label">未批阅</div>
                  </div>
                </el-col>
                <el-col :span="6">
                  <div class="stat-item">
                    <div class="stat-number">{{ formatScore(questionStats[question.id].averageScore) }}</div>
                    <div class="stat-label">平均分</div>
                  </div>
                </el-col>
              </el-row>

              <!-- 进度条 -->
              <div class="progress-section">
                <el-progress 
                  :percentage="questionStats[question.id].evaluationProgress"
                  :color="getProgressColor(questionStats[question.id].evaluationProgress)"
                />
              </div>

              <!-- 快速操作 -->
              <div class="quick-actions">
                <el-button-group>
                  <el-button 
                    size="small"
                    :icon="MagicStick"
                    :disabled="questionStats[question.id].unevaluatedAnswers === 0"
                    @click="handleBatchEvaluateQuestion(question.id)"
                    :loading="questionLoading[question.id]"
                  >
                    AI批阅未批阅答案
                  </el-button>
                  <el-button 
                    size="small"
                    :icon="Setting"
                    @click="manageRubric(question.id)"
                  >
                    评分标准
                  </el-button>
                </el-button-group>
              </div>
            </div>
          </el-card>
        </div>

        <!-- 空状态 -->
        <el-empty 
          v-if="filteredQuestions.length === 0 && !loading"
          description="没有找到题目"
        />
      </div>
    </el-card>

    <!-- 题目批阅详情对话框 -->
    <QuestionEvaluationDialog
      v-model="evaluationDialogVisible"
      :question-id="selectedQuestionId"
      @refresh="refreshQuestionStats"
    />

    <!-- 评分标准管理对话框 -->
    <RubricManagementDialog
      v-model="rubricDialogVisible"
      :question-id="selectedQuestionId"
      @refresh="refreshData"
    />

    <!-- 批量任务结果对话框 -->
    <BatchTaskResultDialog
      v-model="taskResultDialogVisible"
      :task-result="activeBatchTask"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import {
  Document,
  User,
  Calendar,
  MagicStick,
  Refresh,
  Search,
  Setting,
  Close
} from '@element-plus/icons-vue'

import { examApi } from '@/api/exam'
import { questionApi } from '@/api/question'
import { evaluationApi } from '@/api/evaluation'
import type { 
  Exam, 
  Question, 
  EvaluationStatistics,
  BatchEvaluationResult
} from '@/types/api'

import QuestionEvaluationDialog from '@/components/evaluation/QuestionEvaluationDialog.vue'
import RubricManagementDialog from '@/components/evaluation/RubricManagementDialog.vue'
import BatchTaskResultDialog from '@/components/evaluation/BatchTaskResultDialog.vue'

const router = useRouter()
const route = useRoute()

// 数据状态
const loading = ref(false)
const batchLoading = ref(false)
const examInfo = ref<Exam | null>(null)
const questions = ref<Question[]>([])
const questionStats = reactive<Record<number, EvaluationStatistics>>({})
const questionLoading = reactive<Record<number, boolean>>({})

// 搜索
const searchKeyword = ref('')

// 题目选择
const selectedQuestions = ref<number[]>([])

// 对话框状态
const evaluationDialogVisible = ref(false)
const rubricDialogVisible = ref(false)
const taskResultDialogVisible = ref(false)
const selectedQuestionId = ref<number | null>(null)

// 批量任务状态
const activeBatchTask = ref<BatchEvaluationResult | null>(null)
const taskPollingTimer = ref<number | null>(null)

// 计算属性
const filteredQuestions = computed(() => {
  if (!searchKeyword.value) return questions.value
  return questions.value.filter(q => 
    q.title.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
    q.content.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

const totalUnevaluatedAnswers = computed(() => {
  return Object.values(questionStats).reduce((total, stats) => 
    total + stats.unevaluatedAnswers, 0
  )
})

const hasUnevaluatedAnswers = computed(() => totalUnevaluatedAnswers.value > 0)

// 生命周期
onMounted(() => {
  loadData()
})

// 监听路由变化
watch(() => route.params.examId, () => {
  loadData()
})

// 方法
const handleBack = () => {
  const examId = route.params.examId
  // 返回到具体的考试详情页面，而不是考试列表
  router.push(`/exams/${examId}`)
}

// 题目选择相关方法
const selectAllQuestions = () => {
  selectedQuestions.value = questions.value
    .filter(q => questionStats[q.id]?.unevaluatedAnswers > 0)
    .map(q => q.id)
}

const clearQuestionSelection = () => {
  selectedQuestions.value = []
}

const batchEvaluateSelected = async () => {
  if (selectedQuestions.value.length === 0) {
    ElMessage.warning('请先选择要批阅的题目')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批阅选中的 ${selectedQuestions.value.length} 个题目吗？`,
      '确认批阅',
      { type: 'warning' }
    )

    batchLoading.value = true
    
    // 为每个选中的题目启动批量批阅
    const results = []
    for (const questionId of selectedQuestions.value) {
      try {
        const taskId = await evaluationApi.evaluateAnswersByQuestion(questionId)
        results.push({ questionId, taskId })
      } catch (error) {
        console.error(`题目 ${questionId} 批阅失败:`, error)
      }
    }
    
    if (results.length > 0) {
      // 这里可以追踪第一个任务的状态，或者显示所有任务的进度
      // 暂时使用第一个任务ID
      if (results[0].taskId) {
        const taskResult = await evaluationApi.getTaskStatus(results[0].taskId)
        activeBatchTask.value = taskResult
        startTaskPolling(results[0].taskId)
      }
      
      ElMessage.success(`已为 ${results.length} 个题目启动批量批阅`)
    } else {
      ElMessage.error('批量批阅启动失败')
    }
    
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('批量批阅失败:', error)
      ElMessage.error('批量批阅失败')
    }
  } finally {
    batchLoading.value = false
  }
}

const loadData = async () => {
  const examId = Number(route.params.examId)
  if (!examId) return

  loading.value = true
  try {
    // 加载考试信息
    examInfo.value = await examApi.getExam(examId)
    
    // 加载题目列表
    questions.value = await questionApi.getQuestionsByExam(examId)
    
    // 加载每个题目的批阅统计
    await loadQuestionStats()
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const loadQuestionStats = async () => {
  for (const question of questions.value) {
    try {
      questionStats[question.id] = await evaluationApi.getEvaluationStatistics(question.id)
    } catch (error) {
      console.error(`加载题目 ${question.id} 统计失败:`, error)
    }
  }
}

const refreshData = () => {
  loadData()
}

const refreshQuestionStats = () => {
  loadQuestionStats()
}

const handleBatchEvaluateExam = async () => {
  const examId = Number(route.params.examId)
  
  try {
    const result = await ElMessageBox.confirm(
      `确定要对整个考试进行AI批阅吗？这将批阅所有未批阅的答案。`,
      '确认批量批阅',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    if (result === 'confirm') {
      batchLoading.value = true
      const taskResponse = await evaluationApi.evaluateAnswersByExam(examId)
      const taskId = extractTaskId(taskResponse)
      
      if (taskId) {
        startTaskPolling(taskId)
        ElNotification.success({
          title: '批量批阅已启动',
          message: `任务ID: ${taskId}`
        })
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量批阅失败:', error)
      ElMessage.error('批量批阅启动失败')
    }
  } finally {
    batchLoading.value = false
  }
}

const handleBatchEvaluateQuestion = async (questionId: number) => {
  try {
    const result = await ElMessageBox.confirm(
      `确定要对该题目的所有未批阅答案进行AI批阅吗？`,
      '确认批量批阅',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    if (result === 'confirm') {
      questionLoading[questionId] = true
      const taskResponse = await evaluationApi.evaluateAnswersByQuestion(questionId)
      const taskId = extractTaskId(taskResponse)
      
      if (taskId) {
        startTaskPolling(taskId)
        ElNotification.success({
          title: '批量批阅已启动',
          message: `题目批量批阅任务ID: ${taskId}`
        })
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('题目批量批阅失败:', error)
      ElMessage.error('批量批阅启动失败')
    }
  } finally {
    questionLoading[questionId] = false
  }
}

const openQuestionEvaluation = (question: Question) => {
  selectedQuestionId.value = question.id
  evaluationDialogVisible.value = true
}

const manageRubric = (questionId: number) => {
  selectedQuestionId.value = questionId
  rubricDialogVisible.value = true
}

// 任务轮询
const startTaskPolling = (taskId: string) => {
  // 先获取初始任务状态
  pollTaskStatus(taskId)
  
  // 设置轮询
  taskPollingTimer.value = window.setInterval(() => {
    pollTaskStatus(taskId)
  }, 2000) // 每2秒轮询一次
}

const pollTaskStatus = async (taskId: string) => {
  try {
    const result = await evaluationApi.getBatchEvaluationResult(taskId)
    activeBatchTask.value = result
    
    // 如果任务完成，停止轮询并刷新数据
    if (result.status === 'COMPLETED' || result.status === 'COMPLETED_WITH_ERRORS' || result.status === 'FAILED') {
      stopTaskPolling()
      refreshQuestionStats()
      
      if (result.status === 'COMPLETED') {
        ElNotification.success({
          title: '批量批阅完成',
          message: `成功批阅 ${result.successfulEvaluations} 个答案`
        })
      } else if (result.status === 'COMPLETED_WITH_ERRORS') {
        ElNotification.warning({
          title: '批量批阅完成(有错误)',
          message: `成功 ${result.successfulEvaluations} 个，失败 ${result.failedEvaluations} 个`
        })
      } else {
        ElNotification.error({
          title: '批量批阅失败',
          message: '任务执行失败，请查看详情'
        })
      }
    }
  } catch (error) {
    console.error('轮询任务状态失败:', error)
    stopTaskPolling()
  }
}

const stopTaskPolling = () => {
  if (taskPollingTimer.value) {
    clearInterval(taskPollingTimer.value)
    taskPollingTimer.value = null
  }
}

const clearBatchTask = () => {
  activeBatchTask.value = null
  stopTaskPolling()
}

const viewTaskResult = () => {
  taskResultDialogVisible.value = true
}

// 工具函数
const extractTaskId = (response: string): string | null => {
  const match = response.match(/task ID: ([a-f0-9-]+)/)
  return match ? match[1] : null
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN')
}

const formatScore = (score: number | null | undefined) => {
  return score?.toFixed(1) || 'N/A'
}

const getContentPreview = (content: string) => {
  return content.length > 100 ? content.substring(0, 100) + '...' : content
}

const getExamStatusType = (status: string) => {
  const statusMap: Record<string, string> = {
    active: 'success',
    inactive: 'info',
    draft: 'warning'
  }
  return statusMap[status] || 'info'
}

const getExamStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    active: '进行中',
    inactive: '已结束',
    draft: '草稿'
  }
  return statusMap[status] || '未知'
}

const getQuestionTypeColor = (type: string) => {
  const colorMap: Record<string, string> = {
    ESSAY: 'primary',
    SHORT_ANSWER: 'success',
    SINGLE_CHOICE: 'info',
    MULTIPLE_CHOICE: 'warning',
    TRUE_FALSE: 'danger',
    CODING: 'primary',
    CASE_ANALYSIS: 'warning'
  }
  return colorMap[type] || 'info'
}

const getQuestionTypeText = (type: string) => {
  const textMap: Record<string, string> = {
    ESSAY: '论述题',
    SHORT_ANSWER: '简答题',
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    CODING: '编程题',
    CASE_ANALYSIS: '案例分析'
  }
  return textMap[type] || type
}

const getProgressColor = (percentage: number) => {
  if (percentage === 100) return '#67c23a'
  if (percentage >= 80) return '#e6a23c'
  if (percentage >= 50) return '#409eff'
  return '#f56c6c'
}

const getTaskStatusType = (status: string) => {
  const statusMap: Record<string, string> = {
    PENDING: 'info',
    IN_PROGRESS: 'warning',
    COMPLETED: 'success',
    COMPLETED_WITH_ERRORS: 'warning',
    FAILED: 'danger'
  }
  return statusMap[status] || 'info'
}

const getTaskStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    PENDING: '等待中',
    IN_PROGRESS: '批阅中',
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
</script>

<style scoped>
.ai-evaluation-view {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
}

.exam-info-card,
.batch-operations-card,
.questions-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.exam-info h3 {
  margin: 0 0 10px 0;
  color: #303133;
}

.exam-description {
  color: #606266;
  margin: 0 0 15px 0;
  line-height: 1.6;
}

.exam-meta {
  margin-top: 15px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 14px;
}

.batch-operations {
  margin-top: 15px;
}

/* 题目选择样式 */
.question-selection-area {
  margin-bottom: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.question-selection-area h4 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.selection-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.selection-info {
  font-size: 14px;
  color: #606266;
  margin-left: auto;
}

.question-checkboxes {
  max-height: 300px;
  overflow-y: auto;
  padding: 8px 0;
}

.question-checkboxes .el-checkbox {
  display: block;
  margin-bottom: 12px;
  margin-right: 0;
}

.question-checkbox-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  font-size: 14px;
  color: #303133;
}

.question-checkbox-label .el-tag {
  margin-left: 8px;
}

.batch-task-status {
  margin-top: 15px;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-content {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.task-info p {
  margin: 0;
  line-height: 1.6;
}

.task-actions {
  display: flex;
  gap: 10px;
}

.questions-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.question-item {
  transition: all 0.3s ease;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.question-title {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.question-title h4 {
  margin: 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.question-content {
  margin-bottom: 15px;
}

.content-preview {
  color: #606266;
  line-height: 1.6;
  margin: 0 0 10px 0;
}

.question-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.max-score {
  color: #909399;
  font-size: 14px;
}

.evaluation-stats {
  border-top: 1px solid #ebeef5;
  padding-top: 15px;
}

.stat-item {
  text-align: center;
}

.stat-number {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  line-height: 1;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.progress-section {
  margin: 15px 0;
}

.quick-actions {
  display: flex;
  justify-content: center;
  margin-top: 15px;
}
</style>
