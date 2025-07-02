<template>
  <el-dialog
    v-model="visible"
    title="题目批阅详情"
    width="80%"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="question-evaluation-dialog" v-loading="loading">
      <!-- 题目信息 -->
      <el-card class="question-info-card" v-if="questionInfo">
        <template #header>
          <span>题目信息</span>
        </template>
        <div class="question-content">
          <h3>{{ questionInfo.title }}</h3>
          <p class="question-text">{{ questionInfo.content }}</p>
          <div class="question-meta">
            <el-tag>{{ getQuestionTypeText(questionInfo.questionType) }}</el-tag>
            <span class="max-score">满分：{{ questionInfo.maxScore }}分</span>
          </div>
        </div>
      </el-card>

      <!-- 批阅统计 -->
      <el-card class="stats-card" v-if="statistics">
        <template #header>
          <div class="card-header">
            <span>批阅统计</span>
            <el-button 
              type="primary" 
              size="small" 
              :icon="Refresh" 
              @click="loadData"
              :loading="loading"
            >
              刷新
            </el-button>
          </div>
        </template>
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ statistics.totalAnswers }}</div>
              <div class="stat-label">总答案数</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ statistics.evaluatedAnswers }}</div>
              <div class="stat-label">已批阅</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ statistics.unevaluatedAnswers }}</div>
              <div class="stat-label">未批阅</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ statistics.averageScore ? statistics.averageScore.toFixed(1) : '-' }}</div>
              <div class="stat-label">平均分</div>
            </div>
          </el-col>
        </el-row>
        <div class="progress-section">
          <div class="progress-label">批阅进度</div>
          <el-progress 
            :percentage="statistics.evaluationProgress" 
            :color="getProgressColor(statistics.evaluationProgress)"
          />
        </div>
      </el-card>

      <!-- 批量操作 -->
      <el-card class="batch-actions-card" v-if="statistics">
        <template #header>
          <div class="card-header">
            <span>批量操作</span>
            <el-tag v-if="statistics.unevaluatedAnswers > 0" type="warning" size="small">
              {{ statistics.unevaluatedAnswers }}个未批阅
            </el-tag>
            <el-tag v-if="statistics.evaluatedAnswers > 0" type="success" size="small">
              {{ statistics.evaluatedAnswers }}个已批阅
            </el-tag>
          </div>
        </template>
        <div class="batch-actions">
          <!-- 第一行：基础批量操作 -->
          <el-row :gutter="12" class="batch-actions-row">
            <el-col :span="12" v-if="statistics.unevaluatedAnswers > 0">
              <el-button 
                type="primary"
                :icon="MagicStick" 
                @click="handleBatchEvaluate"
                :loading="batchLoading"
                block
                size="default"
              >
                AI批阅未批阅 ({{ statistics.unevaluatedAnswers }}个)
              </el-button>
            </el-col>
            <el-col :span="12" v-if="statistics.evaluatedAnswers > 0">
              <el-button 
                type="warning"
                :icon="Refresh" 
                @click="handleBatchReevaluate"
                :loading="batchReevaluateLoading"
                block
                size="default"
              >
                重新批阅已批阅 ({{ statistics.evaluatedAnswers }}个)
              </el-button>
            </el-col>
          </el-row>
          
          <!-- 第二行：高级批量操作 -->
          <el-row :gutter="12" class="batch-actions-row" v-if="statistics.totalAnswers > 0">
            <el-col :span="24">
              <el-button 
                type="danger"
                :icon="MagicStick"
                @click="handleBatchAIEvaluateAll"
                :loading="batchAIEvaluateAllLoading"
                block
                size="default"
              >
                AI重新批阅全部答案 ({{ statistics.totalAnswers }}个)
              </el-button>
            </el-col>
          </el-row>
          
          <!-- 智能批量操作提示 -->
          <div class="batch-tips" style="margin-top: 12px;">
            <el-alert
              v-if="statistics.unevaluatedAnswers === 0 && statistics.evaluatedAnswers > 0"
              title="提示：所有答案已批阅完成"
              type="success"
              :closable="false"
              show-icon
            >
              <template #default>
                您可以使用"重新批阅"功能来改进已有批阅结果，或使用"AI批量重新批阅"来获得更准确的评分。
              </template>
            </el-alert>
            <el-alert
              v-else-if="statistics.unevaluatedAnswers > 0"
              :title="`还有 ${statistics.unevaluatedAnswers} 个答案未批阅`"
              type="warning"
              :closable="false"
              show-icon
            >
              <template #default>
                建议先完成所有答案的初次批阅，然后可以对结果进行重新批阅优化。
              </template>
            </el-alert>
          </div>
        </div>
      </el-card>

      <!-- 学生答案列表 -->
      <el-card class="answers-card">
        <template #header>
          <div class="card-header">
            <span>学生答案列表</span>
            <div class="header-actions">
              <el-select 
                v-model="filterStatus" 
                placeholder="筛选状态" 
                clearable
                style="width: 120px"
              >
                <el-option label="全部" value="" />
                <el-option label="已批阅" value="evaluated" />
                <el-option label="未批阅" value="unevaluated" />
              </el-select>
            </div>
          </div>
        </template>

        <el-table 
          :data="filteredAnswers" 
          v-loading="answersLoading"
          max-height="400"
          :row-class-name="getRowClassName"
        >
          <el-table-column prop="studentName" label="学生" width="120" />
          <el-table-column prop="answerText" label="答案内容" min-width="200">
            <template #default="{ row }">
              <div class="answer-content">
                {{ row.answerText.length > 100 ? row.answerText.substring(0, 100) + '...' : row.answerText }}
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="score" label="得分" width="80" align="center">
            <template #default="{ row }">
              <span v-if="row.evaluated">{{ row.score }}</span>
              <el-tag v-else type="warning" size="small">未批阅</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="evaluated" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.evaluated" type="success" size="small">已批阅</el-tag>
              <el-tag v-else type="warning" size="small">未批阅</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="evaluatedAt" label="批阅时间" width="160">
            <template #default="{ row }">
              <span v-if="row.evaluatedAt">{{ formatDate(row.evaluatedAt) }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="300" fixed="right">
            <template #default="{ row }">
              <el-button-group>
                <!-- AI批阅：对未批阅的答案 -->
                <el-button 
                  v-if="!row.evaluated"
                  type="primary" 
                  size="small" 
                  :icon="MagicStick"
                  @click="evaluateAnswer(row)"
                  :loading="answerLoading[row.id]"
                >
                  AI批阅
                </el-button>
                
                <!-- 重新批阅：对所有答案都显示，但未批阅的会禁用 -->
                <el-button 
                  type="warning" 
                  size="small" 
                  :icon="Refresh"
                  @click="reevaluateAnswer(row)"
                  :loading="answerLoading[row.id]"
                  :disabled="!row.evaluated"
                  :title="row.evaluated ? '重新进行AI批阅' : '请先完成初次批阅'"
                >
                  重新批阅
                </el-button>
                
                <!-- 手动批阅：对所有答案都可用 -->
                <el-button 
                  type="info" 
                  size="small" 
                  :icon="Edit"
                  @click="manuallyEvaluate(row)"
                  :title="row.evaluated ? '修改当前批阅结果' : '手动给出批阅结果'"
                >
                  手动批阅
                </el-button>
              </el-button-group>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 手动批阅对话框 -->
    <el-dialog
      v-model="manualEvaluationVisible"
      title="手动批阅"
      width="600px"
      :close-on-click-modal="false"
    >
      <div v-if="selectedAnswer">
        <div class="manual-evaluation-form">
          <div class="answer-display">
            <h4>学生答案：</h4>
            <div class="answer-text">{{ selectedAnswer.answerText }}</div>
          </div>
          
          <el-form :model="manualEvaluationForm" label-width="80px">
            <el-form-item label="分数">
              <el-input-number 
                v-model="manualEvaluationForm.score" 
                :min="0" 
                :max="questionInfo?.maxScore || 100" 
                :precision="1"
                style="width: 150px"
              />
              <span class="max-score-hint">/ {{ questionInfo?.maxScore || 100 }}分</span>
            </el-form-item>
            <el-form-item label="评语">
              <el-input 
                v-model="manualEvaluationForm.feedback" 
                type="textarea" 
                :rows="4"
                placeholder="请输入批阅反馈..."
              />
            </el-form-item>
          </el-form>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="manualEvaluationVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="submitManualEvaluation"
          :loading="manualEvaluationLoading"
        >
          提交批阅
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量重新评阅评分模式选择对话框 -->
    <el-dialog
      v-model="batchReevaluateDialogVisible"
      title="选择重新评阅模式"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="evaluation-style-selection">
        <div class="selection-info">
          <p>将对该题目的 <strong>{{ statistics?.evaluatedAnswers || 0 }}</strong> 个已批阅答案进行重新评阅</p>
          <p class="style-description">请选择评分模式：</p>
        </div>
        
        <el-radio-group v-model="batchReevaluateStyle" class="style-options">
          <el-radio value="NORMAL" class="style-option">
            <div class="option-content">
              <div class="option-title">普通模式</div>
              <div class="option-desc">平衡的评分标准，综合考虑准确性和完整性</div>
            </div>
          </el-radio>
          
          <el-radio value="LENIENT" class="style-option">
            <div class="option-content">
              <div class="option-title">宽松模式</div>
              <div class="option-desc">更加宽容的评分，鼓励学生的积极思考，对部分正确答案酌情给分</div>
            </div>
          </el-radio>
          
          <el-radio value="STRICT" class="style-option">
            <div class="option-content">
              <div class="option-title">严格模式</div>
              <div class="option-desc">更高的评分要求，对细节和准确性要求更严格</div>
            </div>
          </el-radio>
        </el-radio-group>
      </div>
      
      <template #footer>
        <el-button @click="batchReevaluateDialogVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="confirmBatchReevaluate"
          :loading="batchReevaluateLoading"
        >
          开始重新评阅
        </el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { 
  Refresh, 
  MagicStick, 
  Edit 
} from '@element-plus/icons-vue'

import { questionApi } from '@/api/question'
import { evaluationApi } from '@/api/evaluation'
import { answerApi } from '@/api/answer'
import { getQuestionTypeText } from '@/utils/tagTypes'
import type { 
  Question, 
  EvaluationStatistics, 
  StudentAnswerResponse 
} from '@/types/api'

// Props和Emits
interface Props {
  modelValue: boolean
  questionId: number | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  refresh: []
}>()

// 数据状态
const loading = ref(false)
const answersLoading = ref(false)
const batchLoading = ref(false)
const batchReevaluateLoading = ref(false)
const batchAIEvaluateAllLoading = ref(false)
const questionInfo = ref<Question | null>(null)
const statistics = ref<EvaluationStatistics | null>(null)
const answers = ref<StudentAnswerResponse[]>([])
const answerLoading = reactive<Record<number, boolean>>({})

// 筛选
const filterStatus = ref('')

// 手动批阅
const manualEvaluationVisible = ref(false)
const manualEvaluationLoading = ref(false)
const selectedAnswer = ref<StudentAnswerResponse | null>(null)
const manualEvaluationForm = reactive({
  score: 0,
  feedback: ''
})

// 批量重新评阅
const batchReevaluateDialogVisible = ref(false)
const batchReevaluateStyle = ref('NORMAL')

// 计算属性
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const filteredAnswers = computed(() => {
  if (!filterStatus.value) return answers.value
  
  if (filterStatus.value === 'evaluated') {
    return answers.value.filter(answer => answer.evaluated)
  } else if (filterStatus.value === 'unevaluated') {
    return answers.value.filter(answer => !answer.evaluated)
  }
  
  return answers.value
})

// 监听对话框打开
watch(() => props.modelValue, (newVal) => {
  if (newVal && props.questionId) {
    loadData()
  }
})

// 方法
const loadData = async () => {
  if (!props.questionId) return
  
  loading.value = true
  try {
    await Promise.all([
      loadQuestionInfo(),
      loadStatistics(),
      loadAnswers()
    ])
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const loadQuestionInfo = async () => {
  if (!props.questionId) return
  
  try {
    questionInfo.value = await questionApi.getQuestionById(props.questionId)
  } catch (error) {
    console.error('加载题目信息失败:', error)
  }
}

const loadStatistics = async () => {
  if (!props.questionId) return
  
  try {
    statistics.value = await evaluationApi.getEvaluationStatistics(props.questionId)
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

const loadAnswers = async () => {
  if (!props.questionId) return
  
  answersLoading.value = true
  try {
    answers.value = await answerApi.getAnswersByQuestion(props.questionId)
  } catch (error) {
    console.error('加载答案列表失败:', error)
  } finally {
    answersLoading.value = false
  }
}

const handleBatchEvaluate = async () => {
  if (!props.questionId) return
  
  try {
    const result = await ElMessageBox.confirm(
      `确定要对该题目的 ${statistics.value?.unevaluatedAnswers} 个未批阅答案进行AI批阅吗？`,
      '确认批量批阅',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    if (result === 'confirm') {
      batchLoading.value = true
      const taskResponse = await evaluationApi.evaluateAnswersByQuestion(props.questionId)
      
      ElNotification.success({
        title: '批量批阅已启动',
        message: '正在后台处理，请稍后刷新查看结果'
      })
      
      // 等待几秒后自动刷新
      setTimeout(() => {
        loadData()
        emit('refresh')
      }, 3000)
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

const handleBatchReevaluate = async () => {
  if (!props.questionId) return
  
  // 显示评分模式选择对话框
  batchReevaluateDialogVisible.value = true
}

const confirmBatchReevaluate = async () => {
  if (!props.questionId) return
  
  try {
    batchReevaluateLoading.value = true
    // 使用现有的API，传递评分模式参数
    const taskResponse = await evaluationApi.batchRevaluateAnswersByQuestion(props.questionId, batchReevaluateStyle.value)
    
    ElNotification.success({
      title: '批量重新批阅已启动',
      message: `正在使用${getEvaluationStyleText(batchReevaluateStyle.value)}模式进行重新批阅`
    })
    
    batchReevaluateDialogVisible.value = false
    
    // 等待几秒后自动刷新
    setTimeout(() => {
      loadData()
      emit('refresh')
    }, 3000)
  } catch (error) {
    console.error('批量重新批阅失败:', error)
    ElMessage.error('批量重新批阅启动失败')
  } finally {
    batchReevaluateLoading.value = false
  }
}

const getEvaluationStyleText = (style: string) => {
  switch (style) {
    case 'STRICT': return '严格'
    case 'LENIENT': return '宽松'
    case 'NORMAL': 
    default: return '普通'
  }
}

const handleBatchAIEvaluateAll = async () => {
  if (!props.questionId) return
  
  try {
    const result = await ElMessageBox.confirm(
      `确定要对该题目的 ${statistics.value?.totalAnswers} 个答案进行AI批量重新批阅吗？`,
      '确认AI批量重新批阅',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    if (result === 'confirm') {
      batchAIEvaluateAllLoading.value = true
      const taskResponse = await evaluationApi.batchEvaluateAllAnswersByQuestion(props.questionId)
      
      ElNotification.success({
        title: 'AI批量重新批阅已启动',
        message: '正在后台处理，请稍后刷新查看结果'
      })
      
      // 等待几秒后自动刷新
      setTimeout(() => {
        loadData()
        emit('refresh')
      }, 3000)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('AI批量重新批阅失败:', error)
      ElMessage.error('AI批量重新批阅启动失败')
    }
  } finally {
    batchAIEvaluateAllLoading.value = false
  }
}

const evaluateAnswer = async (answer: StudentAnswerResponse) => {
  answerLoading[answer.id] = true
  try {
    await evaluationApi.evaluateAnswer(answer.id)
    ElMessage.success('AI批阅完成')
    await loadData()
    emit('refresh')
  } catch (error) {
    console.error('批阅失败:', error)
    ElMessage.error('AI批阅失败')
  } finally {
    answerLoading[answer.id] = false
  }
}

const reevaluateAnswer = async (answer: StudentAnswerResponse) => {
  // 检查答案是否已批阅
  if (!answer.evaluated) {
    ElMessage.warning('请先完成初次批阅，再进行重新批阅')
    return
  }

  answerLoading[answer.id] = true
  try {
    await evaluationApi.revaluateAnswer(answer.id)
    ElMessage.success('重新批阅完成')
    await loadData()
    emit('refresh')
  } catch (error) {
    console.error('重新批阅失败:', error)
    ElMessage.error('重新批阅失败')
  } finally {
    answerLoading[answer.id] = false
  }
}

const manuallyEvaluate = (answer: StudentAnswerResponse) => {
  selectedAnswer.value = answer
  manualEvaluationForm.score = answer.score || 0
  manualEvaluationForm.feedback = answer.feedback || ''
  manualEvaluationVisible.value = true
}

const submitManualEvaluation = async () => {
  if (!selectedAnswer.value) return
  
  manualEvaluationLoading.value = true
  try {
    await evaluationApi.manuallyEvaluateAnswer(selectedAnswer.value.id, {
      score: manualEvaluationForm.score,
      feedback: manualEvaluationForm.feedback
    })
    
    ElMessage.success('手动批阅完成')
    manualEvaluationVisible.value = false
    await loadData()
    emit('refresh')
  } catch (error) {
    console.error('手动批阅失败:', error)
    ElMessage.error('手动批阅失败')
  } finally {
    manualEvaluationLoading.value = false
  }
}

const handleClose = () => {
  emit('update:modelValue', false)
}

const getProgressColor = (percentage: number) => {
  if (percentage >= 80) return '#67c23a'
  if (percentage >= 50) return '#409eff'
  return '#f56c6c'
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN')
}

const getRowClassName = ({ row }: { row: StudentAnswerResponse }) => {
  if (row.evaluated) {
    return 'evaluated-row'
  } else {
    return 'unevaluated-row'
  }
}
</script>

<style scoped>
.question-evaluation-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.question-content h3 {
  margin: 0 0 10px 0;
  color: #303133;
}

.question-text {
  color: #606266;
  line-height: 1.6;
  margin: 0 0 15px 0;
}

.question-meta {
  display: flex;
  align-items: center;
  gap: 15px;
}

.max-score {
  color: #909399;
  font-size: 14px;
}

.stat-item {
  text-align: center;
  padding: 15px 0;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.progress-section {
  margin-top: 20px;
}

.progress-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.batch-actions {
  padding: 16px 0;
}

.batch-actions-row {
  margin-bottom: 12px;
}

.batch-actions-row:last-child {
  margin-bottom: 0;
}

.batch-actions .el-button {
  height: 40px;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.answer-content {
  line-height: 1.5;
  word-break: break-word;
}

.manual-evaluation-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.answer-display h4 {
  margin: 0 0 10px 0;
  color: #303133;
}

.answer-text {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 6px;
  line-height: 1.6;
  color: #606266;
  border-left: 3px solid #409eff;
}

.max-score-hint {
  margin-left: 10px;
  color: #909399;
  font-size: 14px;
}

/* 表格行样式 */
:deep(.evaluated-row) {
  background-color: #f0f9ff;
}

:deep(.evaluated-row:hover) {
  background-color: #e1f3ff !important;
}

:deep(.unevaluated-row) {
  background-color: #fffbf0;
}

:deep(.unevaluated-row:hover) {
  background-color: #fff7e6 !important;
}

/* 评分模式选择样式 */
.evaluation-style-selection {
  padding: 10px 0;
}

.selection-info {
  margin-bottom: 20px;
}

.selection-info p {
  margin: 8px 0;
  color: #606266;
}

.style-description {
  font-weight: 600;
  color: #303133;
}

.style-options {
  width: 100%;
}

.style-option {
  display: block;
  width: 100%;
  margin-bottom: 16px;
  margin-right: 0;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.style-option:hover {
  border-color: #409eff;
  background-color: #f5f9ff;
}

:deep(.style-option.is-checked) {
  border-color: #409eff;
  background-color: #f0f8ff;
}

.option-content {
  margin-left: 8px;
}

.option-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.option-desc {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}
</style>
