<template>
  <div class="student-exam-answers">
    <div class="page-header">
      <el-breadcrumb>
        <el-breadcrumb-item :to="{ path: '/my-exams' }">我的考试</el-breadcrumb-item>
        <el-breadcrumb-item>{{ examAnswers?.examTitle || '查看答卷' }}</el-breadcrumb-item>
      </el-breadcrumb>
      <h1>我的答卷</h1>
      <p class="page-description">查看你的答题内容和详细批改结果</p>
    </div>

    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="8" animated />
    </div>

    <div v-else-if="examAnswers">
      <!-- 考试基本信息 -->
      <el-card class="exam-info-card">
        <template #header>
          <div class="card-header">
            <span>考试信息</span>
            <div class="header-actions">
              <el-button type="primary" @click="goToResult">
                <el-icon><TrendCharts /></el-icon>
                查看成绩概览
              </el-button>
              <el-button @click="goBack">
                <el-icon><ArrowLeft /></el-icon>
                返回
              </el-button>
            </div>
          </div>
        </template>
        
        <el-descriptions :column="3" border>
          <el-descriptions-item label="考试标题">{{ examAnswers.examTitle }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDateTime(examAnswers.submitTime) }}</el-descriptions-item>
          <el-descriptions-item label="总得分">{{ examAnswers.totalScore || 0 }} 分</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 答题详情 -->
      <el-card class="answers-card">
        <template #header>
          <div class="card-header">
            <span>答题详情 ({{ examAnswers.answers?.length || 0 }} 题)</span>
            <div class="filter-actions">
              <el-select v-model="filterType" placeholder="筛选题目" style="width: 150px">
                <el-option label="全部题目" value="" />
                <el-option label="已批改" value="evaluated" />
                <el-option label="未批改" value="unevaluated" />
                <el-option label="得满分" value="full-score" />
                <el-option label="有扣分" value="partial-score" />
              </el-select>
            </div>
          </div>
        </template>
        
        <div v-if="filteredAnswers.length === 0" class="empty-state">
          <el-empty description="暂无答题记录" />
        </div>
        
        <div v-else class="answers-list">
          <div 
            v-for="(answer, index) in filteredAnswers" 
            :key="answer.id"
            class="answer-item"
          >
            <el-card class="answer-card" shadow="never">
              <template #header>
                <div class="answer-header">
                  <div class="question-info">
                    <span class="question-number">第 {{ index + 1 }} 题</span>
                    <el-tag :type="getQuestionTypeColor(answer.questionType)" size="small">
                      {{ getQuestionTypeName(answer.questionType) }}
                    </el-tag>
                  </div>
                  <div class="score-info">
                    <span v-if="answer.score !== null" class="score">
                      {{ answer.score }} / {{ answer.maxScore || 0 }} 分
                    </span>
                    <el-tag v-else type="warning" size="small">未批改</el-tag>
                  </div>
                </div>
              </template>
              
              <!-- 题目内容 -->
              <div class="question-content">
                <h4>
                  {{ answer.questionTitle || `第 ${index + 1} 题` }}
                </h4>
                <div class="question-text" v-html="answer.questionContent || answer.questionText || '题目内容未找到'"></div>
                
                <!-- 选择题选项 -->
                <div v-if="answer.questionOptions && answer.questionOptions.length > 0" class="question-options">
                  <div 
                    v-for="(option, optIndex) in answer.questionOptions" 
                    :key="optIndex"
                    class="option-item"
                    :class="{ 
                      'selected': isOptionSelected(answer, option.value),
                      'correct': option.isCorrect 
                    }"
                  >
                    <span class="option-label">{{ String.fromCharCode(65 + optIndex) }}.</span>
                    <span class="option-text">{{ option.text }}</span>
                    <el-icon v-if="option.isCorrect" class="correct-icon" color="#67c23a">
                      <Check />
                    </el-icon>
                  </div>
                </div>
              </div>
              
              <!-- 我的答案 -->
              <div class="my-answer">
                <h4>我的答案</h4>
                <div class="answer-content">
                  <div v-if="answer.answerText" class="answer-text">
                    {{ answer.answerText }}
                  </div>
                  <div v-else class="no-answer">
                    <el-tag type="info" size="small">未作答</el-tag>
                  </div>
                </div>
              </div>
              
              <!-- 批改结果 -->
              <div v-if="answer.score !== null || answer.feedback" class="evaluation-result">
                <h4>批改结果</h4>
                <div class="evaluation-content">
                  <div v-if="answer.score !== null" class="score-detail">
                    <el-progress 
                      :percentage="getScorePercentage(answer)" 
                      :color="getScoreColor(answer)"
                      :stroke-width="8"
                    />
                    <span class="score-text">得分：{{ answer.score }} / {{ answer.maxScore || 0 }}</span>
                  </div>
                  
                  <div v-if="answer.feedback" class="feedback">
                    <h5>批改意见</h5>
                    <div class="feedback-content">{{ answer.feedback }}</div>
                  </div>
                  
                  <div v-if="answer.evaluatedAt" class="evaluation-time">
                    <el-icon><Clock /></el-icon>
                    批改时间：{{ formatDateTime(answer.evaluatedAt) }}
                  </div>
                </div>
              </div>
            </el-card>
          </div>
        </div>
      </el-card>
    </div>
    
    <div v-else class="error-state">
      <el-result
        icon="warning"
        title="无法加载答卷"
        sub-title="未找到你在此考试中的答题记录"
      >
        <template #extra>
          <el-button type="primary" @click="goBack">返回</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  ArrowLeft, TrendCharts, Clock, Check 
} from '@element-plus/icons-vue'
import { examResultApi } from '@/api/examResult'

interface ExamAnswers {
  examId: number
  examTitle: string
  examDescription?: string
  studentId: number
  studentName: string
  submitTime: string
  totalScore: number
  answers: Array<{
    id: number
    questionId: number
    questionTitle?: string
    questionContent?: string
    questionText?: string  // 保持向后兼容
    questionType: string
    questionOptions?: Array<{
      value: string
      text: string
      isCorrect: boolean
    }>
    answerText: string
    score: number | null
    maxScore: number
    feedback?: string
    evaluatedAt?: string
  }>
}

const route = useRoute()
const router = useRouter()

// 响应式数据
const loading = ref(false)
const examAnswers = ref<ExamAnswers | null>(null)
const filterType = ref('')

// 计算属性
const examId = computed(() => {
  const id = route.params.examId
  return typeof id === 'string' ? parseInt(id) : (Array.isArray(id) ? parseInt(id[0]) : id as number)
})

const filteredAnswers = computed(() => {
  if (!examAnswers.value?.answers) return []
  
  const answers = examAnswers.value.answers
  
  switch (filterType.value) {
    case 'evaluated':
      return answers.filter(a => a.score !== null)
    case 'unevaluated':
      return answers.filter(a => a.score === null)
    case 'full-score':
      return answers.filter(a => a.score !== null && a.score === a.maxScore)
    case 'partial-score':
      return answers.filter(a => a.score !== null && a.score < a.maxScore)
    default:
      return answers
  }
})

// 生命周期
onMounted(async () => {
  await loadExamAnswers()
})

// 方法
const loadExamAnswers = async () => {
  loading.value = true
  try {
    console.log('调用考试答卷API，examId:', examId.value)
    const response = await examResultApi.getStudentExamResult(examId.value)
    examAnswers.value = response.data
  } catch (error: any) {
    console.error('Failed to load exam answers:', error)
    ElMessage.error(error.response?.data?.message || '加载答卷失败')
  } finally {
    loading.value = false
  }
}

const isOptionSelected = (answer: any, optionValue: string): boolean => {
  if (!answer.answerText) return false
  
  // 处理单选
  if (answer.questionType === 'SINGLE_CHOICE') {
    return answer.answerText === optionValue
  }
  
  // 处理多选
  if (answer.questionType === 'MULTIPLE_CHOICE') {
    const selectedOptions = answer.answerText.split(',')
    return selectedOptions.includes(optionValue)
  }
  
  return false
}

const getScorePercentage = (answer: any): number => {
  if (answer.score === null || !answer.maxScore) return 0
  return Math.round((answer.score / answer.maxScore) * 100)
}

const getScoreColor = (answer: any): string => {
  const percentage = getScorePercentage(answer)
  if (percentage >= 90) return '#67c23a'
  if (percentage >= 70) return '#e6a23c'
  return '#f56c6c'
}

const getQuestionTypeName = (type: string): string => {
  const typeMap: Record<string, string> = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'TRUE_FALSE': '判断题',
    'SHORT_ANSWER': '简答题',
    'ESSAY': '论述题',
    'CODING': '编程题',
    'CASE_ANALYSIS': '案例分析题',
    'CALCULATION': '计算题'
  }
  return typeMap[type] || type
}

const getQuestionTypeColor = (type: string): 'success' | 'info' | 'warning' | 'danger' | 'primary' => {
  const colorMap: Record<string, 'success' | 'info' | 'warning' | 'danger' | 'primary'> = {
    'SINGLE_CHOICE': 'primary',
    'MULTIPLE_CHOICE': 'success',
    'TRUE_FALSE': 'warning',
    'SHORT_ANSWER': 'info',
    'ESSAY': 'danger',
    'CODING': 'primary',
    'CASE_ANALYSIS': 'warning',
    'CALCULATION': 'success'
  }
  return colorMap[type] || 'info'
}

const formatDateTime = (dateString?: string): string => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

const goToResult = () => {
  router.push(`/my-exams/${examId.value}/result`)
}

const goBack = () => {
  router.push('/my-exams')
}
</script>

<style scoped>
.student-exam-answers {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 8px 0;
  font-size: 24px;
  font-weight: 600;
}

.page-description {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.loading-container {
  padding: 40px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.exam-info-card {
  margin-bottom: 24px;
}

.answers-card {
  margin-bottom: 24px;
}

.filter-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.answers-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.answer-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.answer-card {
  border: none;
  box-shadow: none;
}

.answer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.question-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.question-number {
  font-weight: 600;
  font-size: 16px;
}

.score-info .score {
  font-weight: 600;
  font-size: 16px;
  color: #409eff;
}

.question-content,
.my-answer,
.evaluation-result {
  margin-bottom: 20px;
}

.question-content h4,
.my-answer h4,
.evaluation-result h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.question-text {
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
  margin-bottom: 12px;
  line-height: 1.6;
}

.question-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  background: #fff;
  transition: all 0.3s;
}

.option-item.selected {
  background: #e1f3d8;
  border-color: #95d475;
}

.option-item.correct {
  background: #f0f9ff;
  border-color: #409eff;
}

.option-label {
  font-weight: 600;
  min-width: 20px;
}

.option-text {
  flex: 1;
}

.correct-icon {
  margin-left: auto;
}

.answer-content {
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
  min-height: 60px;
}

.answer-text {
  line-height: 1.6;
  white-space: pre-wrap;
}

.no-answer {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  color: #999;
}

.evaluation-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.score-detail {
  display: flex;
  align-items: center;
  gap: 16px;
}

.score-text {
  font-weight: 600;
  white-space: nowrap;
}

.feedback h5 {
  margin: 0 0 8px 0;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

.feedback-content {
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
  line-height: 1.6;
}

.evaluation-time {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #909399;
}

.empty-state,
.error-state {
  padding: 40px;
  text-align: center;
}

@media (max-width: 768px) {
  .student-exam-answers {
    padding: 0 16px;
  }
  
  .answer-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .score-detail {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
