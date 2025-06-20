<template>
  <div class="take-exam">
    <!-- 考试信息头部 -->
    <el-card v-if="exam" class="exam-header">
      <div class="exam-info">
        <h1>{{ exam.title }}</h1>
        <div class="exam-meta">
          <span class="questions-count">
            <el-icon><Document /></el-icon>
            题目数量: {{ questions.length }}题
          </span>
          <span class="created-at">
            <el-icon><Clock /></el-icon>
            创建时间: {{ formatDate(exam.createdAt) }}
          </span>
        </div>
        <p v-if="exam.description" class="exam-description">{{ exam.description }}</p>
      </div>
      
      <!-- 考试状态和倒计时 -->
      <div class="exam-status">
        <div v-if="!examStarted" class="pre-exam">
          <el-button type="primary" size="large" @click="startExam" :loading="starting">
            <el-icon><VideoPlay /></el-icon>
            开始考试
          </el-button>
        </div>
        <div v-else-if="!examFinished" class="exam-timer">
          <div class="countdown">
            <el-icon><Clock /></el-icon>
            考试进行中 - 请完成所有题目
          </div>
        </div>
        <div v-else class="exam-completed">
          <el-tag type="success" size="large">考试已完成</el-tag>
        </div>
      </div>
    </el-card>

    <!-- 考试进行中 -->
    <div v-if="examStarted && !examFinished" class="exam-content">
      <!-- 题目导航 -->
      <el-card class="question-nav">
        <div class="nav-header">
          <span>题目导航</span>
          <el-button type="primary" @click="() => submitExam()">
            <el-icon><Check /></el-icon>
            提交试卷
          </el-button>
        </div>
        <div class="question-grid">
          <div
            v-for="(question, index) in questions"
            :key="question.id"
            class="question-nav-item"
            :class="{
              'current': currentQuestionIndex === index,
              'answered': isQuestionAnswered(index),
              'flagged': flaggedQuestions.has(index)
            }"
            @click="goToQuestion(index)"
          >
            {{ index + 1 }}
            <el-icon v-if="flaggedQuestions.has(index)" class="flag-icon"><Star /></el-icon>
          </div>
        </div>
        <div class="nav-stats">
          <span>已答: {{ answeredCount }}/{{ questions.length }}</span>
          <span>标记: {{ flaggedQuestions.size }}</span>
        </div>
      </el-card>

      <!-- 当前题目 -->
      <el-card v-if="currentQuestion" class="current-question">
        <div class="question-header">
          <div class="question-info">
            <span class="question-number">第 {{ currentQuestionIndex + 1 }} 题</span>
            <el-tag :type="getQuestionTypeColor(currentQuestion.questionType)">
              {{ getQuestionTypeName(currentQuestion.questionType) }}
            </el-tag>
            <span v-if="currentQuestion.maxScore" class="points">
              {{ currentQuestion.maxScore }} 分
            </span>
          </div>
          <div class="question-actions">
            <el-button
              :type="flaggedQuestions.has(currentQuestionIndex) ? 'warning' : 'default'"
              @click="toggleFlag(currentQuestionIndex)"
            >
              <el-icon><Star /></el-icon>
              {{ flaggedQuestions.has(currentQuestionIndex) ? '取消标记' : '标记题目' }}
            </el-button>
          </div>
        </div>

        <div class="question-content">
          <div class="question-text" v-html="currentQuestion.content"></div>
          
          <!-- 单选题 -->
          <div v-if="currentQuestion.questionType === 'SINGLE_CHOICE'" class="question-options">
            <el-radio-group 
              v-model="currentAnswer.selectedOption"
              @change="saveCurrentAnswer"
            >
              <el-radio label="A" class="option-item">选项 A</el-radio>
              <el-radio label="B" class="option-item">选项 B</el-radio>
              <el-radio label="C" class="option-item">选项 C</el-radio>
              <el-radio label="D" class="option-item">选项 D</el-radio>
            </el-radio-group>
          </div>

          <!-- 多选题 -->
          <div v-else-if="currentQuestion.questionType === 'MULTIPLE_CHOICE'" class="question-options">
            <el-checkbox-group 
              v-model="currentAnswer.selectedOptions"
              @change="saveCurrentAnswer"
            >
              <el-checkbox label="A" class="option-item">选项 A</el-checkbox>
              <el-checkbox label="B" class="option-item">选项 B</el-checkbox>
              <el-checkbox label="C" class="option-item">选项 C</el-checkbox>
              <el-checkbox label="D" class="option-item">选项 D</el-checkbox>
            </el-checkbox-group>
          </div>

          <!-- 判断题 -->
          <div v-else-if="currentQuestion.questionType === 'TRUE_FALSE'" class="question-options">
            <el-radio-group 
              v-model="currentAnswer.selectedOption"
              @change="saveCurrentAnswer"
            >
              <el-radio label="true" class="option-item">正确</el-radio>
              <el-radio label="false" class="option-item">错误</el-radio>
            </el-radio-group>
          </div>

          <!-- 简答题/论述题/编程题/案例分析题/计算题 -->
          <div v-else class="question-text-input">
            <el-input
              v-model="currentAnswer.textAnswer"
              type="textarea"
              :rows="8"
              placeholder="请输入你的答案..."
              @blur="saveCurrentAnswer"
              class="text-answer"
            />
          </div>
        </div>

        <!-- 题目导航按钮 -->
        <div class="question-navigation">
          <el-button 
            @click="previousQuestion" 
            :disabled="currentQuestionIndex === 0"
          >
            <el-icon><ArrowLeft /></el-icon>
            上一题
          </el-button>
          <el-button 
            type="primary"
            @click="nextQuestion" 
            :disabled="currentQuestionIndex === questions.length - 1"
          >
            下一题
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 考试完成页面 -->
    <el-card v-if="examFinished" class="exam-result">
      <el-result
        icon="success"
        title="考试已提交"
        sub-title="你的答案已经成功提交，请等待老师批阅结果"
      >
        <template #extra>
          <el-button type="primary" @click="goToMyExams">返回我的考试</el-button>
        </template>
      </el-result>
    </el-card>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Clock, Document, VideoPlay, Check, Star, 
  ArrowLeft, ArrowRight 
} from '@element-plus/icons-vue'
import { examApi } from '@/api/exam'
import { questionApi } from '@/api/question'
import { studentAnswerApi } from '@/api/answer'
import type { ExamResponse, QuestionResponse, StudentAnswerSubmitRequest } from '@/types/api'

const route = useRoute()
const router = useRouter()

// 响应式数据
const loading = ref(true)
const starting = ref(false)
const submitting = ref(false)
const exam = ref<ExamResponse | null>(null)
const questions = ref<QuestionResponse[]>([])
const answers = ref<Map<number, any>>(new Map())
const currentQuestionIndex = ref(0)
const examStarted = ref(false)
const examFinished = ref(false)
const flaggedQuestions = ref<Set<number>>(new Set())

// 计算属性
const currentQuestion = computed(() => {
  return questions.value[currentQuestionIndex.value] || null
})

const currentAnswer = computed({
  get: () => {
    const questionId = currentQuestion.value?.id
    if (!questionId) return { selectedOption: '', selectedOptions: [], textAnswer: '' }
    
    return answers.value.get(questionId) || {
      selectedOption: '',
      selectedOptions: [],
      textAnswer: ''
    }
  },
  set: (value) => {
    const questionId = currentQuestion.value?.id
    if (questionId) {
      answers.value.set(questionId, value)
    }
  }
})

const answeredCount = computed(() => {
  return questions.value.filter(q => isQuestionAnswered(questions.value.indexOf(q))).length
})

// 方法
const loadExamData = async () => {
  try {
    const examId = parseInt(route.params.examId as string, 10)
    if (isNaN(examId)) {
      ElMessage.error('无效的考试ID')
      router.push('/my-exams')
      return
    }

    const [examResponse, questionsResponse] = await Promise.all([
      examApi.getExam(examId),
      questionApi.getQuestionsByExam(examId)
    ])

    exam.value = examResponse
    questions.value = questionsResponse

    // 初始化答案
    questions.value.forEach(question => {
      answers.value.set(question.id, {
        selectedOption: '',
        selectedOptions: [],
        textAnswer: ''
      })
    })

  } catch (error: any) {
    console.error('Failed to load exam data:', error)
    ElMessage.error('加载考试数据失败')
    router.push('/my-exams')
  } finally {
    loading.value = false
  }
}

const startExam = async () => {
  try {
    starting.value = true
    
    const confirmed = await ElMessageBox.confirm(
      '开始考试后将开始答题，确定要开始吗？',
      '确认开始考试',
      {
        confirmButtonText: '开始考试',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    if (confirmed) {
      examStarted.value = true
    }
  } catch {
    // 用户取消
  } finally {
    starting.value = false
  }
}

const submitExam = async () => {
  try {
    const confirmed = await ElMessageBox.confirm(
      `你已完成 ${answeredCount.value}/${questions.value.length} 题，确定要提交试卷吗？`,
      '确认提交',
      {
        confirmButtonText: '提交试卷',
        cancelButtonText: '继续答题',
        type: 'warning'
      }
    )
    if (!confirmed) return

    submitting.value = true

    // 准备提交数据 - 只提交有答案的题目
    const submitPromises: Promise<any>[] = []
    
    questions.value.forEach(question => {
      const answer = answers.value.get(question.id)
      if (answer && (answer.textAnswer?.trim() || answer.selectedOption || answer.selectedOptions?.length > 0)) {
        const submitData: StudentAnswerSubmitRequest = {
          questionId: question.id,
          answerText: answer.textAnswer || answer.selectedOption || answer.selectedOptions?.join(',') || '',
          studentId: 'STUDENT_001' // 这里应该从认证状态获取
        }
        submitPromises.push(studentAnswerApi.submitAnswer(submitData))
      }
    })

    await Promise.all(submitPromises)
    examFinished.value = true
    ElMessage.success('试卷提交成功')

  } catch (error: any) {
    console.error('Failed to submit exam:', error)
    ElMessage.error('提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

const saveCurrentAnswer = () => {
  // 答案会通过计算属性自动保存
}

const isQuestionAnswered = (index: number): boolean => {
  const question = questions.value[index]
  if (!question) return false
  
  const answer = answers.value.get(question.id)
  if (!answer) return false

  if (question.questionType === 'SINGLE_CHOICE' || question.questionType === 'TRUE_FALSE') {
    return !!answer.selectedOption
  } else if (question.questionType === 'MULTIPLE_CHOICE') {
    return answer.selectedOptions && answer.selectedOptions.length > 0
  } else {
    return !!answer.textAnswer?.trim()
  }
}

const goToQuestion = (index: number) => {
  currentQuestionIndex.value = index
}

const previousQuestion = () => {
  if (currentQuestionIndex.value > 0) {
    currentQuestionIndex.value--
  }
}

const nextQuestion = () => {
  if (currentQuestionIndex.value < questions.value.length - 1) {
    currentQuestionIndex.value++
  }
}

const toggleFlag = (index: number) => {
  if (flaggedQuestions.value.has(index)) {
    flaggedQuestions.value.delete(index)
  } else {
    flaggedQuestions.value.add(index)
  }
}

const formatDate = (dateStr: string): string => {
  return new Date(dateStr).toLocaleDateString('zh-CN')
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

const goToMyExams = () => {
  router.push('/my-exams')
}

// 生命周期
onMounted(() => {
  loadExamData()
})

// 监听路由变化防止意外离开
watch(() => route.path, (newPath, oldPath) => {
  if (examStarted.value && !examFinished.value && newPath !== oldPath) {
    ElMessageBox.confirm(
      '考试正在进行中，离开页面将丢失未保存的答案，确定要离开吗？',
      '确认离开',
      {
        confirmButtonText: '确认离开',
        cancelButtonText: '继续考试',
        type: 'warning'
      }
    ).catch(() => {
      // 阻止路由跳转
      router.replace(oldPath)
    })
  }
})
</script>

<style scoped>
.take-exam {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.exam-header {
  margin-bottom: 20px;
}

.exam-info h1 {
  margin: 0 0 10px 0;
  color: #303133;
}

.exam-meta {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 10px;
}

.exam-meta span {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #606266;
}

.exam-description {
  color: #909399;
  margin: 10px 0 0 0;
}

.exam-status {
  margin-top: 20px;
}

.pre-exam {
  text-align: center;
}

.exam-timer {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.countdown {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: bold;
  color: #409eff;
}

.exam-content {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 20px;
}

.question-nav {
  height: fit-content;
}

.nav-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  font-weight: bold;
}

.question-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  margin-bottom: 15px;
}

.question-nav-item {
  position: relative;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
}

.question-nav-item:hover {
  border-color: #409eff;
}

.question-nav-item.current {
  background-color: #409eff;
  border-color: #409eff;
  color: white;
}

.question-nav-item.answered {
  background-color: #67c23a;
  border-color: #67c23a;
  color: white;
}

.question-nav-item.flagged {
  border-color: #e6a23c;
}

.flag-icon {
  position: absolute;
  top: -5px;
  right: -5px;
  color: #e6a23c;
  font-size: 12px;
}

.nav-stats {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #606266;
}

.current-question {
  min-height: 500px;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.question-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.question-number {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.points {
  color: #e6a23c;
  font-weight: bold;
}

.question-content {
  margin-bottom: 30px;
}

.question-text {
  font-size: 16px;
  line-height: 1.6;
  margin-bottom: 20px;
  color: #303133;
}

.question-options {
  margin-top: 15px;
}

.option-item {
  display: block;
  margin-bottom: 15px;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  transition: all 0.3s;
}

.option-item:hover {
  border-color: #c6e2ff;
  background-color: #f5f7fa;
}

.question-text-input {
  margin-top: 15px;
}

.text-answer {
  width: 100%;
}

.question-navigation {
  display: flex;
  justify-content: space-between;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.exam-result {
  text-align: center;
}

.loading-container {
  padding: 40px;
}

@media (max-width: 768px) {
  .exam-content {
    grid-template-columns: 1fr;
  }
  
  .question-grid {
    grid-template-columns: repeat(6, 1fr);
  }
  
  .question-nav-item {
    width: 35px;
    height: 35px;
  }
}
</style>

<style scoped>
.take-exam {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.exam-header {
  margin-bottom: 20px;
}

.exam-info h1 {
  margin: 0 0 10px 0;
  color: #303133;
}

.exam-meta {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 10px;
}

.exam-meta span {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #606266;
}

.exam-description {
  color: #909399;
  margin: 10px 0 0 0;
}

.exam-status {
  margin-top: 20px;
}

.pre-exam {
  text-align: center;
}

.exam-timer {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.countdown {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: bold;
  color: #409eff;
}

.exam-content {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 20px;
}

.question-nav {
  height: fit-content;
}

.nav-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  font-weight: bold;
}

.question-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  margin-bottom: 15px;
}

.question-nav-item {
  position: relative;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
}

.question-nav-item:hover {
  border-color: #409eff;
}

.question-nav-item.current {
  background-color: #409eff;
  border-color: #409eff;
  color: white;
}

.question-nav-item.answered {
  background-color: #67c23a;
  border-color: #67c23a;
  color: white;
}

.question-nav-item.flagged {
  border-color: #e6a23c;
}

.flag-icon {
  position: absolute;
  top: -5px;
  right: -5px;
  color: #e6a23c;
  font-size: 12px;
}

.nav-stats {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #606266;
}

.current-question {
  min-height: 500px;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.question-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.question-number {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.points {
  color: #e6a23c;
  font-weight: bold;
}

.question-content {
  margin-bottom: 30px;
}

.question-text {
  font-size: 16px;
  line-height: 1.6;
  margin-bottom: 20px;
  color: #303133;
}

.option-item {
  display: block;
  margin-bottom: 15px;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  transition: all 0.3s;
}

.option-item:hover {
  border-color: #c6e2ff;
  background-color: #f5f7fa;
}

.option-content {
  margin-left: 10px;
}

.text-answer {
  margin-top: 10px;
}

.question-navigation {
  display: flex;
  justify-content: space-between;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.exam-result {
  text-align: center;
}

.loading-container {
  padding: 40px;
}

@media (max-width: 768px) {
  .exam-content {
    grid-template-columns: 1fr;
  }
  
  .question-grid {
    grid-template-columns: repeat(6, 1fr);
  }
  
  .question-nav-item {
    width: 35px;
    height: 35px;
  }
}
</style>
