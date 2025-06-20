<template>
  <div class="exam-evaluation">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">考试批阅</h1>
          <p class="text-gray-600 mt-1" v-if="exam">{{ exam.title }}</p>
        </div>
        <div class="flex gap-3">
          <el-button @click="router.back()" icon="ArrowLeft">返回</el-button>
          <el-button type="primary" @click="exportEvaluation" :loading="exporting">
            <el-icon><Download /></el-icon>
            导出批阅报告
          </el-button>
        </div>
      </div>
    </div>

    <!-- 批阅概览 -->
    <el-row :gutter="20" class="mb-6">
      <el-col :span="6">
        <el-card class="overview-card">
          <div class="stat-item">
            <div class="stat-value">{{ statistics.totalStudents }}</div>
            <div class="stat-label">总参与人数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="overview-card">
          <div class="stat-item">
            <div class="stat-value">{{ statistics.averageScore.toFixed(1) }}分</div>
            <div class="stat-label">平均分</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="overview-card">
          <div class="stat-item">
            <div class="stat-value">{{ statistics.passRate.toFixed(1) }}%</div>
            <div class="stat-label">及格率</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="overview-card">
          <div class="stat-item">
            <div class="stat-value">{{ statistics.completionRate.toFixed(1) }}%</div>
            <div class="stat-label">完成率</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 批阅操作 -->
    <el-card class="mb-6">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-semibold">批量批阅操作</span>
          <el-tag :type="getBatchTaskStatusType()" v-if="batchTaskStatus">
            {{ getBatchTaskStatusText() }}
          </el-tag>
        </div>
      </template>

      <div class="batch-operations">
        <el-row :gutter="20">
          <el-col :span="8">
            <div class="operation-item">
              <h4>AI智能评分</h4>
              <p class="text-gray-600 text-sm mb-3">使用AI自动批阅主观题答案</p>
              <el-button 
                type="primary" 
                @click="startAIEvaluation"
                :loading="startingAI"
                :disabled="batchTaskStatus === 'RUNNING'"
              >
                <el-icon><BrainIcon /></el-icon>
                开始AI批阅
              </el-button>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="operation-item">
              <h4>批量分数调整</h4>
              <p class="text-gray-600 text-sm mb-3">统一调整所有学生的分数</p>
              <el-button @click="batchScoreDialogVisible = true">
                <el-icon><Edit /></el-icon>
                批量调分
              </el-button>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="operation-item">
              <h4>批阅标准设置</h4>
              <p class="text-gray-600 text-sm mb-3">设置评分标准和评分权重</p>
              <el-button @click="rubricDialogVisible = true">
                <el-icon><Setting /></el-icon>
                设置标准
              </el-button>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 问题批阅详情 -->
    <el-card>
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-semibold">题目批阅详情</span>
          <div class="flex gap-2">
            <el-button size="small" @click="refreshQuestionStats">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
            <el-select v-model="selectedQuestionType" placeholder="筛选题型" size="small" clearable>
              <el-option label="单选题" value="SINGLE_CHOICE" />
              <el-option label="多选题" value="MULTIPLE_CHOICE" />
              <el-option label="判断题" value="TRUE_FALSE" />
              <el-option label="简答题" value="SHORT_ANSWER" />
              <el-option label="论述题" value="ESSAY" />
              <el-option label="编程题" value="CODING" />
              <el-option label="案例分析" value="CASE_ANALYSIS" />
              <el-option label="计算题" value="CALCULATION" />
            </el-select>
          </div>
        </div>
      </template>

      <el-table 
        :data="filteredQuestionStats" 
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="questionTitle" label="题目" min-width="200" show-overflow-tooltip />
        <el-table-column prop="questionType" label="题型" width="120">
          <template #default="{ row }">
            <el-tag :type="getQuestionTypeColor(row.questionType)" size="small">
              {{ getQuestionTypeText(row.questionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalAnswers" label="答题人数" width="100" />
        <el-table-column prop="averageScore" label="平均分" width="100">
          <template #default="{ row }">
            {{ row.averageScore?.toFixed(1) || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="correctRate" label="正确率" width="100">
          <template #default="{ row }">
            {{ row.correctRate ? (row.correctRate * 100).toFixed(1) + '%' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="difficulty" label="难度系数" width="100">
          <template #default="{ row }">
            <el-tag 
              :type="row.difficulty > 0.8 ? 'success' : row.difficulty > 0.5 ? 'warning' : 'danger'" 
              size="small"
              v-if="row.difficulty"
            >
              {{ row.difficulty.toFixed(2) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewQuestionDetail(row.questionId)">
              查看详情
            </el-button>
            <el-button size="small" type="primary" @click="evaluateQuestion(row.questionId)">
              手动评分
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 批量调分对话框 -->
    <el-dialog v-model="batchScoreDialogVisible" title="批量分数调整" width="600px">
      <el-form :model="batchScoreForm" label-width="120px">
        <el-form-item label="调整方式">
          <el-radio-group v-model="batchScoreForm.adjustType">
            <el-radio value="add">加分</el-radio>
            <el-radio value="multiply">乘以系数</el-radio>
            <el-radio value="set">设定分数</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="调整值">
          <el-input-number 
            v-model="batchScoreForm.adjustValue" 
            :min="0" 
            :max="100" 
            :precision="1"
            :step="0.1"
          />
          <span class="ml-2 text-gray-500">
            {{ batchScoreForm.adjustType === 'multiply' ? '(倍数)' : '(分数)' }}
          </span>
        </el-form-item>
        <el-form-item label="应用范围">
          <el-checkbox-group v-model="batchScoreForm.questionIds">
            <el-checkbox 
              v-for="question in questionStats" 
              :key="question.questionId"
              :value="question.questionId"
            >
              {{ question.questionTitle }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="batchScoreDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="applyBatchScoreAdjustment" :loading="adjustingScore">
            应用调整
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 评分标准设置对话框 -->
    <el-dialog v-model="rubricDialogVisible" title="评分标准设置" width="800px">
      <div class="rubric-settings">
        <h4>评分权重设置</h4>
        <el-form label-width="150px" class="mb-4">
          <el-form-item label="客观题权重">
            <el-slider v-model="rubricSettings.objectiveWeight" :max="100" show-input />
          </el-form-item>
          <el-form-item label="主观题权重">
            <el-slider v-model="rubricSettings.subjectiveWeight" :max="100" show-input />
          </el-form-item>
          <el-form-item label="及格分数线">
            <el-input-number v-model="rubricSettings.passingScore" :min="0" :max="100" />
          </el-form-item>
        </el-form>

        <h4>AI评分参数</h4>
        <el-form label-width="150px">
          <el-form-item label="评分严格度">
            <el-select v-model="rubricSettings.aiStrictness">
              <el-option label="宽松" value="lenient" />
              <el-option label="标准" value="standard" />
              <el-option label="严格" value="strict" />
            </el-select>
          </el-form-item>
          <el-form-item label="启用语法检查">
            <el-switch v-model="rubricSettings.grammarCheck" />
          </el-form-item>
          <el-form-item label="启用关键词检查">
            <el-switch v-model="rubricSettings.keywordCheck" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="rubricDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveRubricSettings" :loading="savingRubric">
            保存设置
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, ArrowLeft, Edit, Setting, Refresh } from '@element-plus/icons-vue'
import { examApi } from '@/api/exam'
import { evaluationApi } from '@/api/evaluation'
import type { ExamResponse } from '@/types/api'

const route = useRoute()
const router = useRouter()

// 响应式数据
const loading = ref(true)
const exporting = ref(false)
const startingAI = ref(false)
const adjustingScore = ref(false)
const savingRubric = ref(false)
const exam = ref<ExamResponse | null>(null)
const selectedQuestionType = ref<string>('')
const batchTaskStatus = ref<string>('')

// 对话框状态
const batchScoreDialogVisible = ref(false)
const rubricDialogVisible = ref(false)

// 统计数据
const statistics = ref({
  totalStudents: 0,
  averageScore: 0,
  passRate: 0,
  completionRate: 0
})

// 题目统计
const questionStats = ref<any[]>([])

// 批量调分表单
const batchScoreForm = ref({
  adjustType: 'add',
  adjustValue: 0,
  questionIds: [] as number[]
})

// 评分标准设置
const rubricSettings = ref({
  objectiveWeight: 60,
  subjectiveWeight: 40,
  passingScore: 60,
  aiStrictness: 'standard',
  grammarCheck: true,
  keywordCheck: true
})

// 计算属性
const filteredQuestionStats = computed(() => {
  if (!selectedQuestionType.value) return questionStats.value
  return questionStats.value.filter(q => q.questionType === selectedQuestionType.value)
})

// 方法
const loadExamData = async () => {
  try {
    const examId = parseInt(route.params.examId as string, 10)
    if (isNaN(examId)) {
      ElMessage.error('无效的考试ID')
      router.back()
      return
    }

    const [examResponse, statsResponse] = await Promise.all([
      examApi.getExam(examId),
      evaluationApi.getExamStatistics(examId)
    ])

    exam.value = examResponse
    statistics.value = statsResponse.overview
    questionStats.value = statsResponse.questionStats

  } catch (error: any) {
    console.error('Failed to load exam data:', error)
    ElMessage.error('加载考试数据失败')
    router.back()
  } finally {
    loading.value = false
  }
}

const startAIEvaluation = async () => {
  try {
    const confirmed = await ElMessageBox.confirm(
      '确定要开始AI智能批阅吗？这将自动批阅所有主观题答案。',
      '确认批阅',
      {
        confirmButtonText: '开始批阅',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    if (!confirmed) return

    startingAI.value = true
    const examId = parseInt(route.params.examId as string, 10)
    
    const response = await evaluationApi.startBatchEvaluation(examId, {
      evaluationType: 'AI_COMPREHENSIVE',
      questionIds: questionStats.value
        .filter(q => ['SHORT_ANSWER', 'ESSAY', 'CODING', 'CASE_ANALYSIS'].includes(q.questionType))
        .map(q => q.questionId)
    })

    batchTaskStatus.value = 'RUNNING'
    ElMessage.success('AI批阅任务已启动，请稍等...')
    
    // 轮询任务状态
    pollTaskStatus(response.data.taskId)

  } catch (error: any) {
    console.error('Failed to start AI evaluation:', error)
    ElMessage.error('启动AI批阅失败')
  } finally {
    startingAI.value = false
  }
}

const pollTaskStatus = async (taskId: string) => {
  const interval = setInterval(async () => {
    try {
      const response = await evaluationApi.getTaskStatus(taskId)
      batchTaskStatus.value = response.data.status
      
      if (response.data.status === 'COMPLETED') {
        clearInterval(interval)
        ElMessage.success('AI批阅完成')
        refreshQuestionStats()
      } else if (response.data.status === 'FAILED') {
        clearInterval(interval)
        ElMessage.error('AI批阅失败')
        batchTaskStatus.value = ''
      }
    } catch (error) {
      clearInterval(interval)
      batchTaskStatus.value = ''
    }
  }, 3000)
}

const applyBatchScoreAdjustment = async () => {
  try {
    adjustingScore.value = true
    const examId = parseInt(route.params.examId as string, 10)
    
    await evaluationApi.batchAdjustScores(examId, {
      adjustType: batchScoreForm.value.adjustType,
      adjustValue: batchScoreForm.value.adjustValue,
      questionIds: batchScoreForm.value.questionIds
    })

    batchScoreDialogVisible.value = false
    ElMessage.success('批量调分完成')
    refreshQuestionStats()

  } catch (error: any) {
    console.error('Failed to adjust scores:', error)
    ElMessage.error('批量调分失败')
  } finally {
    adjustingScore.value = false
  }
}

const saveRubricSettings = async () => {
  try {
    savingRubric.value = true
    const examId = parseInt(route.params.examId as string, 10)
    
    await evaluationApi.updateRubricSettings(examId, rubricSettings.value)
    
    rubricDialogVisible.value = false
    ElMessage.success('评分标准已保存')

  } catch (error: any) {
    console.error('Failed to save rubric settings:', error)
    ElMessage.error('保存评分标准失败')
  } finally {
    savingRubric.value = false
  }
}

const exportEvaluation = async () => {
  try {
    exporting.value = true
    const examId = parseInt(route.params.examId as string, 10)
    
    const response = await evaluationApi.exportEvaluationReport(examId)
    
    // 创建下载链接
    const blob = new Blob([response.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `考试批阅报告_${exam.value?.title}_${new Date().toLocaleDateString()}.xlsx`
    link.click()
    URL.revokeObjectURL(url)
    
    ElMessage.success('批阅报告导出成功')

  } catch (error: any) {
    console.error('Failed to export evaluation:', error)
    ElMessage.error('导出批阅报告失败')
  } finally {
    exporting.value = false
  }
}

const refreshQuestionStats = async () => {
  try {
    const examId = parseInt(route.params.examId as string, 10)
    const response = await evaluationApi.getExamStatistics(examId)
    
    statistics.value = response.data.overview
    questionStats.value = response.data.questionStats

  } catch (error: any) {
    console.error('Failed to refresh stats:', error)
    ElMessage.error('刷新统计数据失败')
  }
}

const viewQuestionDetail = (questionId: number) => {
  router.push(`/questions/${questionId}`)
}

const evaluateQuestion = (questionId: number) => {
  router.push(`/evaluations/questions/${questionId}`)
}

const getQuestionTypeColor = (type: string): 'success' | 'primary' | 'warning' | 'info' | 'danger' | undefined => {
  const colorMap: Record<string, 'success' | 'primary' | 'warning' | 'info' | 'danger' | undefined> = {
    'SINGLE_CHOICE': undefined,
    'MULTIPLE_CHOICE': 'success',
    'TRUE_FALSE': 'info',
    'SHORT_ANSWER': 'warning',
    'ESSAY': 'danger',
    'CODING': 'primary',
    'CASE_ANALYSIS': 'warning',
    'CALCULATION': 'success'
  }
  return colorMap[type] || undefined
}

const getQuestionTypeText = (type: string): string => {
  const textMap: Record<string, string> = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'TRUE_FALSE': '判断题',
    'SHORT_ANSWER': '简答题',
    'ESSAY': '论述题',
    'CODING': '编程题',
    'CASE_ANALYSIS': '案例分析',
    'CALCULATION': '计算题'
  }
  return textMap[type] || type
}

const getBatchTaskStatusText = (): string => {
  const statusMap: Record<string, string> = {
    'RUNNING': '批阅中',
    'COMPLETED': '批阅完成',
    'FAILED': '批阅失败'
  }
  return statusMap[batchTaskStatus.value] || batchTaskStatus.value
}

const getBatchTaskStatusType = (): 'success' | 'primary' | 'warning' | 'info' | 'danger' | undefined => {
  const typeMap: Record<string, 'success' | 'primary' | 'warning' | 'info' | 'danger' | undefined> = {
    'RUNNING': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger'
  }
  return typeMap[batchTaskStatus.value] || undefined
}

// 生命周期
onMounted(() => {
  loadExamData()
})
</script>

<style scoped>
.exam-evaluation {
  padding: 20px;
}

.page-header {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.overview-card {
  text-align: center;
}

.stat-item {
  padding: 20px 0;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
}

.stat-label {
  color: #666;
  font-size: 14px;
}

.batch-operations {
  padding: 20px 0;
}

.operation-item {
  text-align: center;
  padding: 20px;
  border: 1px solid #eee;
  border-radius: 8px;
  height: 150px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.operation-item h4 {
  margin: 0 0 8px 0;
  color: #333;
}

.rubric-settings h4 {
  margin: 0 0 20px 0;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
  color: #333;
}

.dialog-footer {
  text-align: right;
}

.flex {
  display: flex;
}

.items-center {
  align-items: center;
}

.justify-between {
  justify-content: space-between;
}

.gap-2 {
  gap: 8px;
}

.gap-3 {
  gap: 12px;
}

.mb-4 {
  margin-bottom: 16px;
}

.mb-6 {
  margin-bottom: 24px;
}

.ml-2 {
  margin-left: 8px;
}

.text-2xl {
  font-size: 24px;
}

.font-bold {
  font-weight: bold;
}

.font-semibold {
  font-weight: 600;
}

.text-gray-900 {
  color: #111827;
}

.text-gray-600 {
  color: #6b7280;
}

.text-gray-500 {
  color: #9ca3af;
}

.mt-1 {
  margin-top: 4px;
}

.text-sm {
  font-size: 14px;
}
</style>
