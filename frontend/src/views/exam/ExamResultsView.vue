<template>
  <div class="exam-results">
    <div class="page-header">
      <el-breadcrumb>
        <el-breadcrumb-item :to="{ path: '/my-exams' }">我的考试</el-breadcrumb-item>
        <el-breadcrumb-item>{{ exam?.title || '考试结果' }}</el-breadcrumb-item>
      </el-breadcrumb>
      <h1>考试结果</h1>
      <p>查看你的考试答案和批阅结果</p>
    </div>

    <!-- 考试信息 -->
    <el-card v-if="exam" class="exam-info-card">
      <template #header>
        <div class="card-header">
          <span>考试信息</span>
          <el-tag :type="getExamStatusTag(examResult.status) as any">
            {{ getExamStatusText(examResult.status) }}
          </el-tag>
        </div>
      </template>
      
      <el-row :gutter="24">
        <el-col :span="12">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="考试标题">{{ exam.title }}</el-descriptions-item>
            <el-descriptions-item label="考试描述">{{ exam.description || '无描述' }}</el-descriptions-item>
            <el-descriptions-item label="题目数量">{{ exam.totalQuestions || 0 }}</el-descriptions-item>
            <el-descriptions-item label="总分">{{ exam.totalScore || 0 }}</el-descriptions-item>
          </el-descriptions>
        </el-col>
        <el-col :span="12">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="开始时间">
              {{ formatDate(examResult.startTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="提交时间">
              {{ formatDate(examResult.submitTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="用时">
              {{ formatDuration(examResult.duration) }}
            </el-descriptions-item>
            <el-descriptions-item label="我的得分">
              <span class="score-display">
                {{ examResult.totalScore || 0 }} / {{ exam.totalScore || 0 }}
                <span class="score-percentage">
                  ({{ getScorePercentage() }}%)
                </span>
              </span>
            </el-descriptions-item>
          </el-descriptions>
        </el-col>
      </el-row>
    </el-card>

    <!-- 成绩统计卡片 -->
    <el-row :gutter="24" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="32" color="#67c23a"><Trophy /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ examResult.totalScore || 0 }}</div>
              <div class="stat-label">总得分</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="32" color="#409eff"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ answeredQuestions }}</div>
              <div class="stat-label">已答题目</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="32" color="#e6a23c"><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatDuration(examResult.duration) }}</div>
              <div class="stat-label">总用时</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="32" color="#f56c6c"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ getScorePercentage() }}%</div>
              <div class="stat-label">得分率</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 答案详情 -->
    <el-card class="answers-card">
      <template #header>
        <div class="card-header">
          <span>答案详情</span>
          <div>
            <el-button 
              type="primary" 
              icon="Download" 
              @click="downloadReport"
              :loading="downloadLoading"
            >
              下载成绩报告
            </el-button>
          </div>
        </div>
      </template>
      
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>
      
      <div v-else-if="answers.length === 0" class="empty-state">
        <el-empty description="暂无答案数据" />
      </div>
      
      <div v-else class="answers-list">
        <div 
          v-for="(answer, index) in answers" 
          :key="answer.id"
          class="answer-item"
        >
          <div class="question-header">
            <div class="question-info">
              <el-tag 
                :type="getQuestionTypeTag(answer.questionType || '') as any"
                size="small"
              >
                {{ getQuestionTypeText(answer.questionType || '') }}
              </el-tag>
              <span class="question-title">
                题目 {{ index + 1 }}: {{ answer.questionTitle }}
              </span>
              <span class="question-score">
                ({{ answer.maxScore }} 分)
              </span>
            </div>
            <div class="answer-score">
              <el-tag 
                :type="getScoreTag(answer.score ?? null, answer.maxScore ?? 0)"
                size="large"
              >
                {{ answer.score !== null ? `${answer.score} / ${answer.maxScore}` : '未评分' }}
              </el-tag>
            </div>
          </div>
          
          <div class="question-content">
            <h4>题目内容：</h4>
            <div class="content-text" v-html="answer.questionContent"></div>
          </div>
          
          <!-- 选择题选项 -->
          <div v-if="isChoiceQuestion(answer.questionType || '')" class="question-options">
            <h4>选项：</h4>
            <div class="options-list">
              <div 
                v-for="(option, optionIndex) in answer.questionOptions"
                :key="optionIndex"
                class="option-item"
                :class="{
                  'correct-option': option.isCorrect,
                  'selected-option': isOptionSelected(answer.answerText, optionIndex),
                  'wrong-selection': isOptionSelected(answer.answerText, optionIndex) && !option.isCorrect
                }"
              >
                <span class="option-label">{{ getOptionLabel(optionIndex) }}.</span>
                <span class="option-content">{{ option.content }}</span>
                <el-icon v-if="option.isCorrect" class="correct-icon" color="#67c23a">
                  <CircleCheckFilled />
                </el-icon>
                <el-icon v-if="isOptionSelected(answer.answerText, optionIndex) && !option.isCorrect" 
                         class="wrong-icon" color="#f56c6c">
                  <CircleCloseFilled />
                </el-icon>
              </div>
            </div>
          </div>
          
          <div class="my-answer">
            <h4>我的答案：</h4>
            <div class="answer-text">
              {{ answer.answerText || '未作答' }}
            </div>
          </div>
          
          <!-- 参考答案 -->
          <div v-if="answer.referenceAnswer" class="reference-answer">
            <h4>参考答案：</h4>
            <div class="reference-text">
              {{ answer.referenceAnswer }}
            </div>
          </div>
          
          <!-- 批阅反馈 -->
          <div v-if="answer.feedback" class="feedback">
            <h4>批阅反馈：</h4>
            <div class="feedback-text">
              {{ answer.feedback }}
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Trophy, 
  Document, 
  Clock, 
  TrendCharts, 
  Download,
  CircleCheckFilled,
  CircleCloseFilled
} from '@element-plus/icons-vue'
import { examApi } from '@/api/exam'
import { answerApi } from '@/api/answer'
import type { ExamResponse, StudentAnswerResponse } from '@/types/api'

const route = useRoute()
const router = useRouter()

// 响应式数据
const loading = ref(false)
const downloadLoading = ref(false)
const exam = ref<ExamResponse | null>(null)
const answers = ref<StudentAnswerResponse[]>([])

// 模拟考试结果数据（实际应该从API获取）
const examResult = ref({
  status: 'EVALUATED',
  startTime: new Date().toISOString(),
  submitTime: new Date().toISOString(),
  duration: 3600, // 秒
  totalScore: 0
})

// 计算属性
const examId = computed(() => {
  const id = route.params.examId
  return typeof id === 'string' ? parseInt(id, 10) : Number(id)
})

const answeredQuestions = computed(() => {
  return answers.value.filter(a => a.answerText && a.answerText.trim()).length
})

// 方法
const loadExamData = async () => {
  try {
    loading.value = true
    
    // 加载考试信息
    exam.value = await examApi.getExam(examId.value)
    
    // 加载我的答案
    const myAnswers = await answerApi.getMyAnswersByExam(examId.value)
    answers.value = myAnswers
    
    // 计算总得分
    examResult.value.totalScore = answers.value
      .filter(a => a.score !== null)
      .reduce((sum, a) => sum + (a.score || 0), 0)
    
  } catch (error) {
    console.error('Failed to load exam data:', error)
    ElMessage.error('加载考试数据失败')
  } finally {
    loading.value = false
  }
}

const getScorePercentage = () => {
  if (!exam.value?.totalScore || exam.value.totalScore === 0) return 0
  return Math.round((examResult.value.totalScore / exam.value.totalScore) * 100)
}

const getExamStatusTag = (status: string) => {
  const map: Record<string, string> = {
    'IN_PROGRESS': 'warning',
    'COMPLETED': 'primary',
    'EVALUATED': 'success',
    'ENDED': 'warning'
  }
  return map[status] || 'info'
}

const getExamStatusText = (status: string) => {
  const map: Record<string, string> = {
    'IN_PROGRESS': '进行中',
    'COMPLETED': '已完成',
    'EVALUATED': '已批阅',
    'ENDED': '待批阅'
  }
  return map[status] || status
}

const getQuestionTypeTag = (type: string) => {
  const map: Record<string, string> = {
    'SINGLE_CHOICE': 'primary',
    'MULTIPLE_CHOICE': 'success',
    'TRUE_FALSE': 'warning',
    'SHORT_ANSWER': 'info',
    'ESSAY': 'danger',
    'CODING': 'primary'
  }
  return map[type] || 'info'
}

const getQuestionTypeText = (type: string) => {
  const map: Record<string, string> = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'TRUE_FALSE': '判断题',
    'SHORT_ANSWER': '简答题',
    'ESSAY': '论述题',
    'CODING': '编程题',
    'CASE_ANALYSIS': '案例分析题',
    'CALCULATION': '计算题'
  }
  return map[type] || type
}

const getScoreTag = (score: number | null, maxScore: number) => {
  if (score === null) return 'info'
  const percentage = (score / maxScore) * 100
  if (percentage >= 90) return 'success'
  if (percentage >= 70) return 'primary'
  if (percentage >= 60) return 'warning'
  return 'danger'
}

const isChoiceQuestion = (type: string) => {
  return ['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(type)
}

const isOptionSelected = (answerText: string | null, optionIndex: number) => {
  if (!answerText) return false
  const selectedOptions = answerText.split(',').map(s => s.trim())
  return selectedOptions.includes(getOptionLabel(optionIndex))
}

const getOptionLabel = (index: number) => {
  return String.fromCharCode(65 + index) // A, B, C, D...
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN')
}

const formatDuration = (seconds: number) => {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60
  
  if (hours > 0) {
    return `${hours}小时${minutes}分钟${secs}秒`
  } else if (minutes > 0) {
    return `${minutes}分钟${secs}秒`
  } else {
    return `${secs}秒`
  }
}

const downloadReport = async () => {
  try {
    downloadLoading.value = true
    await answerApi.downloadExamReport(examId.value)
    ElMessage.success('成绩报告下载成功')
  } catch (error) {
    console.error('Failed to download report:', error)
    ElMessage.error('下载成绩报告失败')
  } finally {
    downloadLoading.value = false
  }
}

onMounted(() => {
  loadExamData()
})
</script>

<style scoped>
.exam-results {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 8px 0 4px 0;
  font-size: 24px;
  font-weight: 600;
}

.page-description {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.exam-info-card,
.answers-card {
  margin-bottom: 20px;
}

.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  cursor: default;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  flex-shrink: 0;
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
}

.score-display {
  font-size: 20px;
  font-weight: 600;
  color: #67c23a;
}

.score-percentage {
  font-size: 16px;
  color: #909399;
  margin-left: 8px;
}

.answers-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.answer-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  background-color: #fafafa;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.question-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.question-title {
  font-weight: 600;
  font-size: 16px;
}

.question-score {
  color: #909399;
  font-size: 14px;
}

.answer-score {
  flex-shrink: 0;
}

.question-content,
.question-options,
.my-answer,
.reference-answer,
.feedback {
  margin-bottom: 16px;
}

.question-content h4,
.question-options h4,
.my-answer h4,
.reference-answer h4,
.feedback h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}

.content-text,
.answer-text,
.reference-text,
.feedback-text {
  padding: 12px;
  background-color: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  min-height: 40px;
  line-height: 1.6;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background-color: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  transition: all 0.3s;
}

.option-item.correct-option {
  background-color: #f0f9ff;
  border-color: #67c23a;
}

.option-item.selected-option {
  background-color: #e1f3d8;
  border-color: #67c23a;
}

.option-item.wrong-selection {
  background-color: #fef0f0;
  border-color: #f56c6c;
}

.option-label {
  font-weight: 600;
  flex-shrink: 0;
  width: 20px;
}

.option-content {
  flex: 1;
}

.correct-icon,
.wrong-icon {
  flex-shrink: 0;
}

.loading-container {
  padding: 20px;
}

.empty-state {
  text-align: center;
  padding: 40px;
}
</style>
