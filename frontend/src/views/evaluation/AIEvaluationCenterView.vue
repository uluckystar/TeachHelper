<template>
  <div class="ai-evaluation-center">
    <div class="page-header">
      <h1>AI智能批阅中心</h1>
      <p class="page-description">使用AI技术进行智能批改和批阅管理</p>
    </div>

    <!-- 功能入口卡片 -->
    <el-row :gutter="24" class="feature-cards">
      <el-col :span="8">
        <el-card class="feature-card" @click="startNewTask">
          <div class="feature-content">
            <div class="feature-icon">
              <el-icon size="48" color="#409eff"><MagicStick /></el-icon>
            </div>
            <div class="feature-info">
              <h3>批量AI批阅</h3>
              <p>选择考试进行批量AI智能批阅</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="feature-card" @click="goToRubricManagement">
          <div class="feature-content">
            <div class="feature-icon">
              <el-icon size="48" color="#67c23a"><DocumentChecked /></el-icon>
            </div>
            <div class="feature-info">
              <h3>批阅标准管理</h3>
              <p>创建和管理AI批阅标准</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="feature-card" @click="goToTaskMonitor">
          <div class="feature-content">
            <div class="feature-icon">
              <el-icon size="48" color="#e6a23c"><Monitor /></el-icon>
            </div>
            <div class="feature-info">
              <h3>任务监控</h3>
              <p>查看AI批阅任务状态和进度</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 任务统计卡片 -->
    <el-row :gutter="24" class="stats-cards" style="margin: 24px 0;">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-number">{{ taskStats.total || 0 }}</div>
            <div class="stat-label">总任务数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card pending">
          <div class="stat-content">
            <div class="stat-number">{{ taskStats.pending || 0 }}</div>
            <div class="stat-label">待处理</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card running">
          <div class="stat-content">
            <div class="stat-number">{{ taskStats.running || 0 }}</div>
            <div class="stat-label">运行中</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card completed">
          <div class="stat-content">
            <div class="stat-number">{{ taskStats.completed || 0 }}</div>
            <div class="stat-label">已完成</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近的AI批阅任务 -->
    <el-card class="recent-tasks">
      <template #header>
        <div class="card-header">
          <span>最近的AI批阅任务</span>
          <el-button type="primary" icon="Plus" @click="startNewTask">
            新建批阅任务
          </el-button>
        </div>
      </template>

      <el-table :data="recentTasks" v-loading="loading">
        <el-table-column prop="name" label="任务名称" />
        <el-table-column prop="type" label="任务类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTaskTypeTag(row.type)">
              {{ getTaskTypeText(row.type) }}
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
        <el-table-column label="进度" width="150">
          <template #default="{ row }">
            <el-progress 
              :percentage="row.progress || 0" 
              :status="row.status === 'COMPLETED' ? 'success' : row.status === 'FAILED' ? 'exception' : undefined"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button-group>
              <el-button size="small" @click="viewTaskDetail(row.taskId)">
                详情
              </el-button>
              <el-button 
                size="small" 
                type="danger" 
                @click="cancelTask(row.taskId)"
                :disabled="!['PENDING', 'RUNNING'].includes(row.status)"
              >
                取消
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建任务对话框 -->
    <el-dialog v-model="showNewTaskDialog" title="新建AI批阅任务" width="800px" @closed="resetNewTaskForm">
      <el-steps :active="newTaskStep" finish-status="success" align-center style="margin-bottom: 20px;">
        <el-step title="选择考试" />
        <el-step title="选择题目" />
        <el-step title="配置参数" />
        <el-step title="确认创建" />
      </el-steps>

      <!-- 步骤1: 选择考试 -->
      <div v-if="newTaskStep === 0" class="step-content">
        <h3>选择要批阅的考试</h3>
        <el-table 
          :data="availableExams" 
          @selection-change="handleExamSelection"
          ref="examTable"
          v-loading="loadingExams"
          highlight-current-row
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="title" label="考试名称" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        </el-table>
      </div>

      <!-- 步骤2: 选择题目 -->
      <div v-if="newTaskStep === 1" class="step-content">
        <h3>选择要批阅的题目</h3>
        <div class="exam-info" v-if="selectedExam">
          <el-tag type="primary">{{ selectedExam.title }}</el-tag>
          <span class="exam-stats">共 {{ examQuestions.length }} 道题目</span>
        </div>
        
        <el-table 
          :data="examQuestions" 
          @selection-change="handleQuestionSelection"
          ref="questionTable"
          v-loading="loadingQuestions"
          style="margin-top: 16px;"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="content" label="题目内容" min-width="300" show-overflow-tooltip />
          <el-table-column prop="questionType" label="题目类型" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ getQuestionTypeText(row.questionType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="maxScore" label="满分" width="80" />
          <el-table-column prop="unevaluatedCount" label="未批阅" width="100">
             <template #default="{ row }">
                <el-tag :type="row.unevaluatedCount > 0 ? 'warning' : 'success'">
                  {{ row.unevaluatedCount || 0 }}
                </el-tag>
             </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 步骤3: 配置参数 -->
      <div v-if="newTaskStep === 2" class="step-content">
        <h3>配置批阅参数</h3>
        <el-form :model="newTaskForm" label-width="120px">
          <el-form-item label="重复批阅策略">
            <el-radio-group v-model="newTaskForm.reEvaluate">
              <el-radio :value="false">仅批阅未批阅</el-radio>
              <el-radio :value="true">全部重新批阅</el-radio>
            </el-radio-group>
             <div class="el-form-item__description">
              "全部重新批阅"会覆盖之前所有的AI或手动批阅结果。
            </div>
          </el-form-item>
          <el-form-item label="AI并发数">
            <el-slider 
              v-model="newTaskForm.concurrency" 
              :min="1" 
              :max="50" 
              show-input
            />
            <div class="el-form-item__description">
              设置同时执行的AI批阅任务数量。较高的值会加快批阅速度，但会增加服务器和AI服务负载。建议范围5-20。
            </div>
          </el-form-item>
        </el-form>
      </div>

      <!-- 步骤4: 确认 -->
      <div v-if="newTaskStep === 3" class="step-content">
        <h3>确认创建任务</h3>
        <div v-if="preCheckResult" class="precheck-result">
            <el-alert
              :title="preCheckResult.message"
              :type="preCheckResult.warningLevel === 'WARNING' ? 'warning' : preCheckResult.warningLevel === 'ERROR' ? 'error' : 'info'"
              :closable="false"
              show-icon
            >
              <p>总答案数: {{ preCheckResult.totalAnswers }}</p>
              <p>已批阅: {{ preCheckResult.evaluatedAnswers }}</p>
              <p>将处理: {{ preCheckResult.unevaluatedAnswers }} 个答案</p>
              <p v-if="preCheckResult.suggestion"><b>建议: {{ preCheckResult.suggestion }}</b></p>
            </el-alert>
        </div>
        <p style="margin-top: 20px;">确认要创建批量批阅任务吗？</p>
      </div>

      <template #footer>
        <el-button @click="prevStep" v-if="newTaskStep > 0">上一步</el-button>
        <el-button @click="nextStep" v-if="newTaskStep < 3" :disabled="!canProceedToNextStep">下一步</el-button>
        <el-button 
          type="primary" 
          @click="createTask" 
          v-if="newTaskStep === 3" 
          :loading="creatingTask"
          :disabled="preCheckResult && !preCheckResult.canProceed"
        >
          确认创建
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { MagicStick, DocumentChecked, Monitor, Plus } from '@element-plus/icons-vue'
import * as evaluationApi from '@/api/evaluation'
import * as examApi from '@/api/exam'
import * as rubricApi from '@/api/rubric'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Exam } from '@/types/api'
import { formatDate, getStatusTag, getStatusText, getTaskTypeTag, getTaskTypeText as getTaskTypeTextUtil, getQuestionTypeText } from '@/utils/formatters'

const router = useRouter()

const loading = ref(false)
const creatingTask = ref(false)
const showNewTaskDialog = ref(false)
const recentTasks = ref<any[]>([])
const availableExams = ref<Exam[]>([])
const taskStats = ref({
  total: 0,
  pending: 0,
  running: 0,
  completed: 0,
  failed: 0
})

// 新任务向导相关状态
const newTaskStep = ref(0)
const loadingExams = ref(false)
const loadingQuestions = ref(false)
const selectedExam = ref<any>(null)
const examQuestions = ref<any[]>([])
const selectedQuestions = ref<any[]>([])
const availableRubrics = ref<any[]>([])
const preCheckResult = ref<any>(null)

interface TaskForm {
  examIds: number[]
  questionIds: number[]
  taskType: string
  rubricId: string | null
  reEvaluate: boolean
  concurrency: number
}

const newTaskForm = ref<TaskForm>({
  examIds: [],
  questionIds: [],
  taskType: 'FULL_EVALUATION',
  rubricId: null,
  reEvaluate: false,
  concurrency: 5
})

const loadRecentTasks = async () => {
  loading.value = true
  try {
    const data = await evaluationApi.getRecentTasks()
    recentTasks.value = data || []
  } catch (error) {
    console.error('加载任务失败:', error)
    ElMessage.error('加载任务失败')
  } finally {
    loading.value = false
  }
}

const loadTaskStats = async () => {
  try {
    const data = await evaluationApi.getTaskStats()
    taskStats.value = data || {
      total: 0,
      pending: 0,
      running: 0,
      completed: 0,
      failed: 0
    }
  } catch (error) {
    console.error('加载任务统计失败:', error)
    // 不显示错误消息，避免干扰用户体验
  }
}

const loadAvailableExams = async () => {
  loadingExams.value = true
  try {
    const response = await examApi.getExams({ page: 1, pageSize: 100 })
    availableExams.value = response.data
  } catch (error) {
    console.error('加载考试列表失败:', error)
    ElMessage.error('加载考试列表失败')
    // 提供模拟数据作为后备
    availableExams.value = [
      {
        id: 1,
        title: '人工智能基础考试',
        description: '人工智能基础知识考试，涵盖机器学习、深度学习等核心概念',
        createdBy: 'teacher1',
        createdAt: '2024-01-15T10:00:00',
        updatedAt: '2024-01-15T10:00:00',
        status: 'PUBLISHED' as const
      },
      {
        id: 2,
        title: '机器学习期末考试',
        description: '机器学习期末测试，包含理论知识和实践应用',
        createdBy: 'teacher1',
        createdAt: '2024-01-20T14:30:00',
        updatedAt: '2024-01-20T14:30:00',
        status: 'PUBLISHED' as const
      }
    ]
  } finally {
    loadingExams.value = false
  }
}

const goToBatchEvaluation = () => {
  router.push('/ai-evaluation/batch')
}

const goToRubricManagement = () => {
  router.push('/ai-evaluation/rubrics')
}

const goToTaskMonitor = () => {
  router.push('/ai-evaluation/tasks')
}

const startNewTask = () => {
  showNewTaskDialog.value = true
  loadAvailableExams()
}

const createTask = async () => {
  if (preCheckResult.value && !preCheckResult.value.canProceed) {
    ElMessage.error('预检查不通过，无法创建任务。')
    return
  }

  creatingTask.value = true
  try {
    const config = {
      ...newTaskForm.value,
    }

    const res = await evaluationApi.createBatchTask(config)
    
    if (res.success) {
      ElMessage.success(res.message || '批量批阅任务创建成功！')
      showNewTaskDialog.value = false
      fetchRecentTasks()
      fetchTaskStats()
    } else {
      ElMessage.error(res.message || '创建任务失败')
    }
  } catch (error: any) {
    console.error('创建任务失败:', error)
    ElMessage.error(error.message || '创建任务时发生错误')
  } finally {
    creatingTask.value = false
  }
}

const viewTaskDetail = (taskId: string) => {
  router.push(`/ai-evaluation/tasks/${taskId}`)
}

const viewResults = (taskId: string) => {
  router.push(`/ai-evaluation/tasks/${taskId}/results`)
}

const cancelTask = async (taskId: string) => {
  try {
    await ElMessageBox.confirm('确定要取消此任务吗？', '确认取消', {
      type: 'warning'
    })
    
    await evaluationApi.cancelTask(taskId)
    ElMessage.success('任务已取消')
    loadRecentTasks()
    loadTaskStats() // 更新统计数据
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消任务失败:', error)
      ElMessage.error('取消任务失败')
    }
  }
}

const getTaskTypeTag = (type: string) => {
  const map: Record<string, string> = {
    'FULL_EVALUATION': 'primary',
    'QUICK_EVALUATION': 'success',
    'RUBRIC_BASED': 'warning'
  }
  return map[type] || 'info'
}

const getTaskTypeText = (type: string) => {
  if (!type) return '未知类型'
  if (type.includes('EVALUATION')) return '批量批阅'
  return getTaskTypeTextUtil(type)
}

const getStatusTag = (status: string) => {
  const map: Record<string, string> = {
    'PENDING': 'info',
    'RUNNING': 'warning',
    'COMPLETED': 'success',
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
    'FAILED': '失败',
    'CANCELLED': '已取消'
  }
  return map[status] || status
}

const formatDate = (dateString: string) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

// 新任务向导相关方法
const resetNewTaskForm = () => {
  newTaskStep.value = 0
  selectedExam.value = null
  selectedQuestions.value = []
  preCheckResult.value = null
  newTaskForm.value = {
    examIds: [],
    questionIds: [],
    taskType: 'FULL_EVALUATION',
    rubricId: null,
    reEvaluate: false,
    concurrency: 5
  }
}

const handleExamSelection = (selection: any[]) => {
  selectedExam.value = selection.length > 0 ? selection[0] : null
  if (selectedExam.value) {
    newTaskForm.value.examIds = [selectedExam.value.id]
  } else {
    newTaskForm.value.examIds = []
  }
}

const loadExamQuestions = async (examId: string) => {
  loadingQuestions.value = true
  try {
    // 模拟加载题目数据，实际应该调用相应的API
    // const response = await examApi.getExamQuestions(examId)
    // 暂时使用模拟数据
    await new Promise(resolve => setTimeout(resolve, 1000)) // 模拟网络延迟
    examQuestions.value = [
      {
        id: '1',
        content: '请解释人工智能的基本概念',
        questionType: 'ESSAY',
        maxScore: 20,
        answerCount: 50,
        unevaluatedCount: 15
      },
      {
        id: '2',
        content: '机器学习与深度学习的区别是什么？',
        questionType: 'SHORT_ANSWER',
        maxScore: 15,
        answerCount: 50,
        unevaluatedCount: 8
      },
      {
        id: '3',
        content: '以下哪些属于监督学习算法？',
        questionType: 'MULTIPLE_CHOICE',
        maxScore: 10,
        answerCount: 50,
        unevaluatedCount: 0
      }
    ]
  } catch (error) {
    console.error('加载题目失败:', error)
    ElMessage.error('加载题目失败')
  } finally {
    loadingQuestions.value = false
  }
}

const handleQuestionSelection = (selection: any[]) => {
  selectedQuestions.value = selection
  newTaskForm.value.questionIds = selection.map(q => q.id)
}

const selectAllQuestions = () => {
  // 假设我们有对table的引用
  selectedQuestions.value = [...examQuestions.value]
}

const selectUnevaluatedQuestions = () => {
  selectedQuestions.value = examQuestions.value.filter(q => q.unevaluatedCount > 0)
}

const clearQuestionSelection = () => {
  selectedQuestions.value = []
}

const loadRubrics = async () => {
  try {
    // 模拟加载评分标准数据，实际应该调用相应的API
    // const response = await evaluationApi.getRubrics()
    // 暂时使用模拟数据
    await new Promise(resolve => setTimeout(resolve, 500))
    availableRubrics.value = [
      { id: '1', name: '标准评分标准' },
      { id: '2', name: '严格评分标准' },
      { id: '3', name: '宽松评分标准' }
    ]
  } catch (error) {
    console.error('加载评分标准失败:', error)
  }
}

const nextStep = async () => {
  if (newTaskStep.value < 3) {
    if (newTaskStep.value === 2) {
      // 进入最后一步前进行预检查
      await performPreCheck()
    }
    newTaskStep.value++
  }
}

const prevStep = () => {
  if (newTaskStep.value > 0) {
    newTaskStep.value--
  }
}

const canProceedToNextStep = computed(() => {
  if (newTaskStep.value === 0) {
    return selectedExam.value !== null
  }
  if (newTaskStep.value === 1) {
    return selectedQuestions.value.length > 0
  }
  return true
})

const performPreCheck = async () => {
  const config = {
    examIds: newTaskForm.value.examIds,
    questionIds: newTaskForm.value.questionIds,
    evaluateAll: newTaskForm.value.reEvaluate
  }
  try {
    preCheckResult.value = await evaluationApi.precheckBatchTask(config)
  } catch (error) {
    console.error('预检查失败:', error)
    ElMessage.error('预检查失败')
  }
}

// 辅助函数
const getQuestionTypeText = (type: string) => {
  const map: Record<string, string> = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'SHORT_ANSWER': '简答题',
    'ESSAY': '论述题',
    'FILL_BLANK': '填空题'
  }
  return map[type] || type
}

onMounted(() => {
  loadRecentTasks()
  loadTaskStats()
})
</script>

<style scoped>
.ai-evaluation-center {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: calc(100vh - 120px); /* 确保有足够的高度 */
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

.feature-cards {
  margin-bottom: 24px;
}

.stats-cards {
  margin: 32px 0; /* 增加上下间距 */
  min-height: 120px; /* 确保卡片有最小高度 */
}

.stat-card {
  text-align: center;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-card.pending {
  border-color: #e6a23c;
}

.stat-card.running {
  border-color: #409eff;
}

.stat-card.completed {
  border-color: #67c23a;
}

.stat-content {
  padding: 20px;
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #606266;
  margin: 0;
}

.feature-card {
  cursor: pointer;
  transition: all 0.3s;
  min-height: 180px;
  height: auto;
}

.feature-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.feature-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 24px 20px;
  height: 100%;
  justify-content: center;
}

.feature-icon {
  margin-bottom: 16px;
  flex-shrink: 0;
}

.feature-info h3 {
  margin: 0 0 12px 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.3;
}

.feature-info p {
  margin: 0;
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.stats-cards {
  margin: 24px 0;
}

.stat-card {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-number {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.stat-label {
  color: #666;
  font-size: 14px;
}

.recent-tasks {
  background: white;
  margin-bottom: 30px;
  min-height: 400px; /* 确保任务表格有足够的高度 */
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.step-content {
  margin: 20px 0;
}

.step-content h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.exam-info {
  margin-bottom: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.exam-stats {
  color: #666;
  font-size: 14px;
}

.selection-actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}

.form-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 确保对话框内容不会被遮挡 */
:deep(.el-dialog) {
  margin-top: 5vh !important;
}

:deep(.el-dialog__body) {
  max-height: 70vh;
  overflow-y: auto;
}

/* 确保表格在对话框中正常显示 */
:deep(.el-table) {
  margin-top: 0;
}

:deep(.el-table th) {
  background-color: #fafafa;
}

/* 步骤条样式优化 */
:deep(.el-steps) {
  margin-bottom: 20px;
}

/* 表单样式优化 */
:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-descriptions) {
  margin-bottom: 16px;
}
</style>
