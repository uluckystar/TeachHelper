<template>
  <el-dialog
    v-model="visible"
    title="练习模式"
    width="900px"
    :close-on-click-modal="false"
    @closed="resetPractice"
  >
    <div class="practice-mode">
      <!-- 练习配置 -->
      <div v-if="!practiceStarted" class="practice-config">
        <el-card class="config-card" shadow="never">
          <template #header>
            <span>练习配置</span>
          </template>
          
          <el-form :model="practiceConfig" label-width="120px">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="练习模式">
                  <el-select v-model="practiceConfig.mode" style="width: 100%">
                    <el-option label="顺序练习" value="sequential">
                      <div style="display: flex; justify-content: space-between;">
                        <span>顺序练习</span>
                        <el-text type="info" size="small">按题目顺序依次练习</el-text>
                      </div>
                    </el-option>
                    <el-option label="随机练习" value="random">
                      <div style="display: flex; justify-content: space-between;">
                        <span>随机练习</span>
                        <el-text type="info" size="small">随机顺序练习题目</el-text>
                      </div>
                    </el-option>
                    <el-option label="智能练习" value="adaptive">
                      <div style="display: flex; justify-content: space-between;">
                        <span>智能练习</span>
                        <el-text type="info" size="small">根据答题情况调整难度</el-text>
                      </div>
                    </el-option>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="时间限制">
                  <el-switch
                    v-model="practiceConfig.timeLimited"
                    active-text="启用时间限制"
                    inactive-text="不限时练习"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20" v-if="practiceConfig.timeLimited">
              <el-col :span="12">
                <el-form-item label="每题时间">
                  <el-input-number
                    v-model="practiceConfig.timePerQuestion"
                    :min="30"
                    :max="600"
                    :step="30"
                    style="width: 100%"
                  />
                  <el-text type="info" size="small">秒</el-text>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="总时间">
                  <el-input-number
                    v-model="practiceConfig.totalTime"
                    :min="5"
                    :max="120"
                    :step="5"
                    style="width: 100%"
                  />
                  <el-text type="info" size="small">分钟</el-text>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="答题提示">
                  <el-switch
                    v-model="practiceConfig.showHints"
                    active-text="显示解题提示"
                    inactive-text="不显示提示"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="即时反馈">
                  <el-switch
                    v-model="practiceConfig.immediatefeedback"
                    active-text="答题后立即显示结果"
                    inactive-text="练习结束后统一显示"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-form-item label="练习重点">
              <el-checkbox-group v-model="practiceConfig.focus">
                <el-checkbox label="错题重做">重点练习曾经答错的题目</el-checkbox>
                <el-checkbox label="薄弱知识点">针对薄弱知识点加强练习</el-checkbox>
                <el-checkbox label="高频考点">重点练习常考知识点</el-checkbox>
                <el-checkbox label="新题优先">优先练习未做过的题目</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-form>
        </el-card>
        
        <!-- 题目预览 -->
        <el-card class="preview-card" shadow="never">
          <template #header>
            <div class="preview-header">
              <span>题目预览（{{ practiceQuestions.length }} 题）</span>
              <el-button size="small" @click="shuffleQuestions" v-if="practiceConfig.mode === 'random'">
                <el-icon><Refresh /></el-icon>
                重新排序
              </el-button>
            </div>
          </template>
          
          <div class="questions-preview">
            <div
              v-for="(question, index) in previewQuestions"
              :key="question.id"
              class="question-preview-item"
            >
              <div class="question-number">{{ index + 1 }}</div>
              <div class="question-info">
                <div class="question-title">{{ question.content?.substring(0, 50) }}...</div>
                <div class="question-meta">
                  <el-tag :type="getTypeTagType(question.type)" size="small">
                    {{ getTypeText(question.type) }}
                  </el-tag>
                  <el-tag :type="getDifficultyTagType(question.difficulty)" size="small">
                    {{ getDifficultyText(question.difficulty) }}
                  </el-tag>
                  <span class="question-time">{{ question.estimatedTime || 3 }}分钟</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </div>
      
      <!-- 练习进行中 -->
      <div v-else-if="practiceStarted && !practiceCompleted" class="practice-session">
        <!-- 练习头部 -->
        <div class="practice-header">
          <div class="progress-info">
            <span class="current-question">第 {{ currentQuestionIndex + 1 }} 题 / 共 {{ practiceQuestions.length }} 题</span>
            <el-progress 
              :percentage="practiceProgress" 
              :stroke-width="8"
              :show-text="false"
            />
          </div>
          
          <div class="timer-info" v-if="practiceConfig.timeLimited">
            <div class="total-timer">
              <el-icon><Clock /></el-icon>
              总时间：{{ formatTime(totalTimeRemaining) }}
            </div>
            <div class="question-timer" :class="{ 'time-warning': questionTimeRemaining < 30 }">
              <el-icon><Timer /></el-icon>
              本题：{{ formatTime(questionTimeRemaining) }}
            </div>
          </div>
          
          <div class="practice-actions">
            <el-button size="small" @click="pausePractice" v-if="!paused">
              <el-icon><VideoPause /></el-icon>
              暂停
            </el-button>
            <el-button size="small" @click="resumePractice" v-if="paused">
              <el-icon><VideoPlay /></el-icon>
              继续
            </el-button>
            <el-button size="small" @click="endPractice" type="danger">
              <el-icon><SwitchButton /></el-icon>
              结束练习
            </el-button>
          </div>
        </div>
        
        <!-- 当前题目 -->
        <div class="current-question" v-if="currentQuestion">
          <el-card class="question-card" shadow="never">
            <template #header>
              <div class="question-header">
                <div class="question-meta">
                  <el-tag :type="getTypeTagType(currentQuestion.type)" size="small">
                    {{ getTypeText(currentQuestion.type) }}
                  </el-tag>
                  <el-tag :type="getDifficultyTagType(currentQuestion.difficulty)" size="small">
                    {{ getDifficultyText(currentQuestion.difficulty) }}
                  </el-tag>
                  <span class="question-score">{{ currentQuestion.score || 5 }}分</span>
                </div>
                <div class="question-tools">
                  <el-button size="small" v-if="practiceConfig.showHints" @click="showHint">
                    <el-icon><InfoFilled /></el-icon>
                    提示
                  </el-button>
                  <el-button size="small" @click="toggleBookmark">
                    <el-icon :class="{ 'bookmarked': currentQuestion.bookmarked }">
                      <Star />
                    </el-icon>
                    {{ currentQuestion.bookmarked ? '已收藏' : '收藏' }}
                  </el-button>
                </div>
              </div>
            </template>
            
            <div class="question-content">
              <div class="question-text">{{ currentQuestion.content }}</div>
              
              <!-- 选择题 -->
              <div v-if="currentQuestion.type === 'choice'" class="question-options">
                <el-radio-group v-model="currentAnswer" @change="onAnswerChange">
                  <div
                    v-for="(option, index) in currentQuestion.options"
                    :key="index"
                    class="option-item"
                  >
                    <el-radio :label="index.toString()">
                      <span class="option-label">{{ String.fromCharCode(65 + index) }}.</span>
                      <span class="option-text">{{ option.content }}</span>
                    </el-radio>
                  </div>
                </el-radio-group>
              </div>
              
              <!-- 填空题 -->
              <div v-if="currentQuestion.type === 'blank'" class="question-blanks">
                <div
                  v-for="(blank, index) in currentQuestion.blanks"
                  :key="index"
                  class="blank-item"
                >
                  <span class="blank-label">空{{ index + 1 }}：</span>
                  <el-input
                    v-model="currentAnswer[index]"
                    placeholder="请输入答案..."
                    style="width: 200px"
                    @input="onAnswerChange"
                  />
                </div>
              </div>
              
              <!-- 主观题/计算题 -->
              <div v-if="currentQuestion.type === 'subjective' || currentQuestion.type === 'calculation'" class="question-subjective">
                <el-input
                  v-model="currentAnswer"
                  type="textarea"
                  :rows="6"
                  placeholder="请输入您的答案..."
                  @input="onAnswerChange"
                />
              </div>
            </div>
            
            <!-- 答题反馈 -->
            <div v-if="practiceConfig.immediatefeedback && answerSubmitted" class="answer-feedback">
              <div class="feedback-header">
                <el-icon :class="{ 'correct': isCurrentAnswerCorrect, 'incorrect': !isCurrentAnswerCorrect }">
                  <Check v-if="isCurrentAnswerCorrect" />
                  <Close v-else />
                </el-icon>
                <span class="feedback-text">
                  {{ isCurrentAnswerCorrect ? '回答正确！' : '回答错误' }}
                </span>
              </div>
              
              <div v-if="currentQuestion.explanation" class="explanation">
                <div class="explanation-title">解析：</div>
                <div class="explanation-content">{{ currentQuestion.explanation }}</div>
              </div>
            </div>
          </el-card>
          
          <!-- 答题控制 -->
          <div class="answer-controls">
            <el-button @click="previousQuestion" :disabled="currentQuestionIndex === 0">
              <el-icon><ArrowLeft /></el-icon>
              上一题
            </el-button>
            
            <el-button
              v-if="!answerSubmitted"
              type="primary"
              @click="submitAnswer"
              :disabled="!hasAnswer"
            >
              提交答案
            </el-button>
            
            <el-button
              v-if="currentQuestionIndex < practiceQuestions.length - 1"
              type="primary"
              @click="nextQuestion"
              :disabled="practiceConfig.immediatefeedback && !answerSubmitted"
            >
              下一题
              <el-icon><ArrowRight /></el-icon>
            </el-button>
            
            <el-button
              v-if="currentQuestionIndex === practiceQuestions.length - 1"
              type="success"
              @click="completePractice"
            >
              完成练习
              <el-icon><Check /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
      
      <!-- 练习结果 -->
      <div v-else-if="practiceCompleted" class="practice-results">
        <el-card class="results-card" shadow="never">
          <template #header>
            <div class="results-header">
              <el-icon class="results-icon"><Trophy /></el-icon>
              <span>练习完成</span>
            </div>
          </template>
          
          <!-- 总体统计 -->
          <div class="overall-stats" v-if="practiceResults">
            <div class="stats-grid">
              <div class="stat-item">
                <div class="stat-value">{{ practiceResults.totalQuestions }}</div>
                <div class="stat-label">总题数</div>
              </div>
              <div class="stat-item">
                <div class="stat-value correct">{{ practiceResults.correctCount }}</div>
                <div class="stat-label">正确</div>
              </div>
              <div class="stat-item">
                <div class="stat-value incorrect">{{ practiceResults.wrongCount }}</div>
                <div class="stat-label">错误</div>
              </div>
              <div class="stat-item">
                <div class="stat-value accuracy">{{ practiceResults.accuracy }}%</div>
                <div class="stat-label">正确率</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ practiceResults.totalTime }}</div>
                <div class="stat-label">用时</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ practiceResults.averageTime }}</div>
                <div class="stat-label">平均用时</div>
              </div>
            </div>
          </div>
          
          <!-- 详细结果 -->
          <div class="detailed-results" v-if="practiceResults">
            <div class="results-tabs">
              <el-tabs v-model="resultsTab">
                <el-tab-pane label="答题详情" name="details">
                  <div class="answer-details">
                    <div
                      v-for="(result, index) in practiceResults.details"
                      :key="index"
                      class="answer-detail-item"
                    >
                      <div class="detail-header">
                        <div class="question-info">
                          <span class="question-number">第{{ index + 1 }}题</span>
                          <el-tag :type="getTypeTagType(result.question.type)" size="small">
                            {{ getTypeText(result.question.type) }}
                          </el-tag>
                          <el-tag :type="result.isCorrect ? 'success' : 'danger'" size="small">
                            {{ result.isCorrect ? '正确' : '错误' }}
                          </el-tag>
                        </div>
                        <div class="detail-actions">
                          <el-button size="small" @click="reviewQuestion(result.question)">查看详情</el-button>
                          <el-button size="small" @click="addToErrorBook(result.question)" v-if="!result.isCorrect">
                            加入错题本
                          </el-button>
                        </div>
                      </div>
                      
                      <div class="detail-content">
                        <div class="question-text">{{ result.question.content.substring(0, 80) }}...</div>
                        <div class="answer-comparison">
                          <div class="user-answer">
                            <span class="label">您的答案：</span>
                            <span class="answer">{{ formatAnswer(result.userAnswer, result.question.type) }}</span>
                          </div>
                          <div class="correct-answer">
                            <span class="label">正确答案：</span>
                            <span class="answer">{{ formatCorrectAnswer(result.question) }}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </el-tab-pane>
                
                <el-tab-pane label="知识点分析" name="knowledge">
                  <div class="knowledge-analysis">
                    <div
                      v-for="analysis in practiceResults.knowledgeAnalysis"
                      :key="analysis.knowledgePoint"
                      class="knowledge-item"
                    >
                      <div class="knowledge-header">
                        <span class="knowledge-title">{{ analysis.knowledgePoint }}</span>
                        <el-tag :type="getAccuracyTagType(analysis.accuracy)" size="small">
                          {{ analysis.accuracy }}%
                        </el-tag>
                      </div>
                      <div class="knowledge-progress">
                        <el-progress
                          :percentage="analysis.accuracy"
                          :color="getAccuracyColor(analysis.accuracy)"
                          :stroke-width="8"
                        />
                      </div>
                      <div class="knowledge-details">
                        <span>答对 {{ analysis.correct }} / {{ analysis.total }} 题</span>
                        <span v-if="analysis.accuracy < 60" class="suggestion">
                          建议加强练习
                        </span>
                      </div>
                    </div>
                  </div>
                </el-tab-pane>
                
                <el-tab-pane label="错题回顾" name="errors">
                  <div class="error-review">
                    <div v-if="practiceResults.wrongQuestions.length === 0" class="no-errors">
                      <el-icon size="64"><Select /></el-icon>
                      <h3>太棒了！</h3>
                      <p>本次练习没有错题</p>
                    </div>
                    <div v-else class="error-list">
                      <div
                        v-for="(question, index) in practiceResults.wrongQuestions"
                        :key="question.id"
                        class="error-item"
                      >
                        <div class="error-question">
                          <div class="question-title">第{{ question.originalIndex + 1 }}题：{{ question.content.substring(0, 60) }}...</div>
                          <div class="error-analysis">
                            <span class="error-type">{{ getErrorType(question) }}</span>
                            <el-button size="small" @click="practiceAgain([question])">重新练习</el-button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </el-tab-pane>
              </el-tabs>
            </div>
          </div>
        </el-card>
      </div>
    </div>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false" v-if="!practiceStarted || practiceCompleted">
          关闭
        </el-button>
        <el-button @click="restartPractice" v-if="practiceCompleted">
          重新练习
        </el-button>
        <el-button 
          type="primary" 
          @click="startPractice" 
          v-if="!practiceStarted"
          :disabled="practiceQuestions.length === 0"
        >
          开始练习
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Clock,
  Timer,
  VideoPause,
  VideoPlay,
  SwitchButton,
  InfoFilled,
  Star,
  Check,
  Close,
  ArrowLeft,
  ArrowRight,
  Trophy,
  Select
} from '@element-plus/icons-vue'

// Props
const props = defineProps<{
  modelValue: boolean
  questions: any[]
}>()

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'practice-completed': [results: any]
}>()

// 响应式数据
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 练习配置
const practiceConfig = ref({
  mode: 'sequential' as 'sequential' | 'random' | 'adaptive',
  timeLimited: false,
  timePerQuestion: 180, // 秒
  totalTime: 30, // 分钟
  showHints: true,
  immediatefeedback: true,
  focus: [] as string[]
})

// 练习状态
const practiceStarted = ref(false)
const practiceCompleted = ref(false)
const paused = ref(false)
const practiceQuestions = ref<any[]>([])
const currentQuestionIndex = ref(0)
const currentAnswer = ref<any>(null)
const answerSubmitted = ref(false)
const userAnswers = ref<any[]>([])
const practiceStartTime = ref<Date | null>(null)
const questionStartTime = ref<Date | null>(null)
const totalTimeRemaining = ref(0)
const questionTimeRemaining = ref(0)
const resultsTab = ref('details')

// 计时器
let totalTimer: number | null = null
let questionTimer: number | null = null

// 计算属性
const previewQuestions = computed(() => {
  return practiceQuestions.value.slice(0, 10)
})

const currentQuestion = computed(() => {
  return practiceQuestions.value[currentQuestionIndex.value]
})

const practiceProgress = computed(() => {
  if (practiceQuestions.value.length === 0) return 0
  return Math.round(((currentQuestionIndex.value + 1) / practiceQuestions.value.length) * 100)
})

const hasAnswer = computed(() => {
  if (!currentAnswer.value) return false
  
  if (currentQuestion.value?.type === 'blank') {
    return Array.isArray(currentAnswer.value) && currentAnswer.value.some(answer => answer?.trim())
  }
  
  if (typeof currentAnswer.value === 'string') {
    return currentAnswer.value.trim().length > 0
  }
  
  return currentAnswer.value !== null && currentAnswer.value !== undefined
})

const isCurrentAnswerCorrect = computed(() => {
  if (!currentQuestion.value || !answerSubmitted.value) return false
  
  // 这里需要根据题目类型判断答案是否正确
  // 简化实现，实际需要更复杂的逻辑
  return Math.random() > 0.3 // 模拟70%正确率
})

const practiceResults = computed(() => {
  if (!practiceCompleted.value) return null
  
  const correctCount = userAnswers.value.filter(answer => answer.isCorrect).length
  const wrongCount = userAnswers.value.length - correctCount
  const accuracy = userAnswers.value.length > 0 ? Math.round((correctCount / userAnswers.value.length) * 100) : 0
  
  return {
    totalQuestions: userAnswers.value.length,
    correctCount,
    wrongCount,
    accuracy,
    totalTime: formatTime(getTotalPracticeTime()),
    averageTime: formatTime(getTotalPracticeTime() / userAnswers.value.length),
    details: userAnswers.value.map((answer, index) => ({
      question: practiceQuestions.value[index],
      userAnswer: answer.userAnswer,
      isCorrect: answer.isCorrect,
      timeSpent: answer.timeSpent
    })),
    knowledgeAnalysis: generateKnowledgeAnalysis(),
    wrongQuestions: userAnswers.value
      .map((answer, index) => ({ ...practiceQuestions.value[index], originalIndex: index }))
      .filter((_, index) => !userAnswers.value[index].isCorrect)
  }
})

// 监听题目变化
watch(() => props.questions, (newQuestions) => {
  if (newQuestions && newQuestions.length > 0) {
    practiceQuestions.value = [...newQuestions]
    if (practiceConfig.value.mode === 'random') {
      shuffleQuestions()
    }
  }
}, { immediate: true })

// 组件挂载和卸载
onMounted(() => {
  // 初始化
})

onUnmounted(() => {
  clearTimers()
})

// 方法
const startPractice = () => {
  if (practiceQuestions.value.length === 0) {
    ElMessage.warning('没有可练习的题目')
    return
  }
  
  practiceStarted.value = true
  practiceCompleted.value = false
  currentQuestionIndex.value = 0
  userAnswers.value = []
  practiceStartTime.value = new Date()
  
  // 根据配置准备题目
  if (practiceConfig.value.mode === 'random') {
    shuffleQuestions()
  } else if (practiceConfig.value.mode === 'adaptive') {
    // TODO: 实现自适应逻辑
  }
  
  // 初始化计时器
  if (practiceConfig.value.timeLimited) {
    totalTimeRemaining.value = practiceConfig.value.totalTime * 60
    questionTimeRemaining.value = practiceConfig.value.timePerQuestion
    startTimers()
  }
  
  // 加载第一题
  loadQuestion(0)
  
  ElMessage.success('练习开始！')
}

const loadQuestion = (index: number) => {
  if (index < 0 || index >= practiceQuestions.value.length) return
  
  currentQuestionIndex.value = index
  currentAnswer.value = getInitialAnswer(practiceQuestions.value[index])
  answerSubmitted.value = false
  questionStartTime.value = new Date()
  
  if (practiceConfig.value.timeLimited) {
    questionTimeRemaining.value = practiceConfig.value.timePerQuestion
  }
}

const getInitialAnswer = (question: any) => {
  switch (question.type) {
    case 'choice':
      return null
    case 'blank':
      return question.blanks ? new Array(question.blanks.length).fill('') : ['']
    case 'subjective':
    case 'calculation':
      return ''
    default:
      return null
  }
}

const onAnswerChange = () => {
  // 答案变化时的处理
}

const submitAnswer = () => {
  if (!hasAnswer.value) {
    ElMessage.warning('请先作答')
    return
  }
  
  const questionEndTime = new Date()
  const timeSpent = questionEndTime.getTime() - (questionStartTime.value?.getTime() || 0)
  
  // 判断答案是否正确
  const isCorrect = checkAnswer(currentAnswer.value, currentQuestion.value)
  
  // 保存答案
  userAnswers.value.push({
    questionId: currentQuestion.value.id,
    userAnswer: currentAnswer.value,
    isCorrect,
    timeSpent: Math.round(timeSpent / 1000)
  })
  
  answerSubmitted.value = true
  
  if (practiceConfig.value.immediatefeedback) {
    if (isCorrect) {
      ElMessage.success('回答正确！')
    } else {
      ElMessage.error('回答错误')
    }
  }
}

const checkAnswer = (userAnswer: any, question: any): boolean => {
  // 简化的答案检查逻辑
  // 实际应用中需要根据题目类型进行详细检查
  switch (question.type) {
    case 'choice':
      return question.options?.some((option: any, index: number) => 
        option.isCorrect && userAnswer === index.toString()
      ) || false
    case 'blank':
      return question.blanks?.every((blank: any, index: number) => 
        blank.answer.toLowerCase().trim() === (userAnswer[index] || '').toLowerCase().trim()
      ) || false
    default:
      // 对于主观题，这里简化为随机判断
      return Math.random() > 0.4
  }
}

const nextQuestion = () => {
  if (currentQuestionIndex.value < practiceQuestions.value.length - 1) {
    loadQuestion(currentQuestionIndex.value + 1)
  }
}

const previousQuestion = () => {
  if (currentQuestionIndex.value > 0) {
    loadQuestion(currentQuestionIndex.value - 1)
  }
}

const completePractice = () => {
  practiceCompleted.value = true
  practiceStarted.value = false
  clearTimers()
  
  // 保存练习结果
  const results = practiceResults.value
  emit('practice-completed', results)
  
  ElMessage.success('练习完成！')
}

const endPractice = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要结束当前练习吗？未完成的题目将不会被记录。',
      '确认结束练习',
      {
        confirmButtonText: '确定结束',
        cancelButtonText: '继续练习',
        type: 'warning'
      }
    )
    
    completePractice()
  } catch {
    // 用户取消
  }
}

const pausePractice = () => {
  paused.value = true
  clearTimers()
  ElMessage.info('练习已暂停')
}

const resumePractice = () => {
  paused.value = false
  if (practiceConfig.value.timeLimited) {
    startTimers()
  }
  ElMessage.info('练习已恢复')
}

const restartPractice = () => {
  resetPractice()
  startPractice()
}

const resetPractice = () => {
  practiceStarted.value = false
  practiceCompleted.value = false
  paused.value = false
  currentQuestionIndex.value = 0
  currentAnswer.value = null
  answerSubmitted.value = false
  userAnswers.value = []
  practiceStartTime.value = null
  questionStartTime.value = null
  totalTimeRemaining.value = 0
  questionTimeRemaining.value = 0
  resultsTab.value = 'details'
  clearTimers()
}

const shuffleQuestions = () => {
  practiceQuestions.value = [...practiceQuestions.value].sort(() => Math.random() - 0.5)
}

const startTimers = () => {
  if (totalTimer) clearInterval(totalTimer)
  if (questionTimer) clearInterval(questionTimer)
  
  // 总计时器
  totalTimer = setInterval(() => {
    if (totalTimeRemaining.value > 0) {
      totalTimeRemaining.value--
    } else {
      ElMessage.warning('练习时间已到')
      completePractice()
    }
  }, 1000)
  
  // 单题计时器
  questionTimer = setInterval(() => {
    if (questionTimeRemaining.value > 0) {
      questionTimeRemaining.value--
    } else {
      ElMessage.warning('本题时间已到，自动跳转下一题')
      if (!answerSubmitted.value) {
        submitAnswer()
      }
      setTimeout(() => {
        if (currentQuestionIndex.value < practiceQuestions.value.length - 1) {
          nextQuestion()
        } else {
          completePractice()
        }
      }, 1500)
    }
  }, 1000)
}

const clearTimers = () => {
  if (totalTimer) {
    clearInterval(totalTimer)
    totalTimer = null
  }
  if (questionTimer) {
    clearInterval(questionTimer)
    questionTimer = null
  }
}

const showHint = () => {
  // TODO: 实现提示功能
  ElMessage.info('提示功能开发中...')
}

const toggleBookmark = () => {
  if (currentQuestion.value) {
    currentQuestion.value.bookmarked = !currentQuestion.value.bookmarked
    ElMessage.success(currentQuestion.value.bookmarked ? '已收藏' : '已取消收藏')
  }
}

const reviewQuestion = (question: any) => {
  ElMessage.info('查看题目详情功能开发中...')
}

const addToErrorBook = (question: any) => {
  ElMessage.success('已加入错题本')
}

const practiceAgain = (questions: any[]) => {
  practiceQuestions.value = questions
  resetPractice()
  startPractice()
}

// 工具方法
const formatTime = (seconds: number): string => {
  const minutes = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

const getTotalPracticeTime = (): number => {
  if (!practiceStartTime.value) return 0
  return Math.round((Date.now() - practiceStartTime.value.getTime()) / 1000)
}

const generateKnowledgeAnalysis = () => {
  // 模拟知识点分析
  return [
    { knowledgePoint: '函数概念', total: 3, correct: 2, accuracy: 67 },
    { knowledgePoint: '导数应用', total: 2, correct: 2, accuracy: 100 },
    { knowledgePoint: '积分计算', total: 1, correct: 0, accuracy: 0 }
  ]
}

const getErrorType = (question: any): string => {
  // 简化的错误类型分析
  const types = ['计算错误', '概念理解', '审题不清', '方法选择']
  return types[Math.floor(Math.random() * types.length)]
}

const formatAnswer = (answer: any, type: string): string => {
  if (type === 'choice') {
    return answer ? String.fromCharCode(65 + parseInt(answer)) : '未作答'
  }
  if (type === 'blank') {
    return Array.isArray(answer) ? answer.join(', ') : '未作答'
  }
  return answer || '未作答'
}

const formatCorrectAnswer = (question: any): string => {
  switch (question.type) {
    case 'choice':
      const correctIndex = question.options?.findIndex((opt: any) => opt.isCorrect)
      return correctIndex >= 0 ? String.fromCharCode(65 + correctIndex) : '未知'
    case 'blank':
      return question.blanks?.map((blank: any) => blank.answer).join(', ') || '未知'
    default:
      return question.referenceAnswer || '参考答案'
  }
}

const getTypeTagType = (type: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    'choice': 'primary',
    'blank': 'success',
    'subjective': 'warning',
    'calculation': 'info'
  }
  return typeMap[type] || 'info'
}

const getTypeText = (type: string) => {
  const textMap: Record<string, string> = {
    'choice': '选择题',
    'blank': '填空题',
    'subjective': '主观题',
    'calculation': '计算题'
  }
  return textMap[type] || type
}

const getDifficultyTagType = (difficulty: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    'easy': 'success',
    'medium': 'warning',
    'hard': 'danger'
  }
  return typeMap[difficulty] || 'info'
}

const getDifficultyText = (difficulty: string) => {
  const textMap: Record<string, string> = {
    'easy': '简单',
    'medium': '中等',
    'hard': '困难'
  }
  return textMap[difficulty] || difficulty
}

const getAccuracyTagType = (accuracy: number) => {
  if (accuracy >= 80) return 'success'
  if (accuracy >= 60) return 'warning'
  return 'danger'
}

const getAccuracyColor = (accuracy: number) => {
  if (accuracy >= 80) return '#67c23a'
  if (accuracy >= 60) return '#e6a23c'
  return '#f56c6c'
}
</script>

<style scoped>
.practice-mode {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-height: 70vh;
  overflow-y: auto;
}

/* 配置阶段 */
.practice-config {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.config-card,
.preview-card {
  border: 1px solid #e4e7ed;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.questions-preview {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 300px;
  overflow-y: auto;
}

.question-preview-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fafafa;
}

.question-number {
  min-width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #409eff;
  color: white;
  border-radius: 50%;
  font-weight: 600;
}

.question-info {
  flex: 1;
}

.question-title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 8px;
}

.question-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.question-time {
  font-size: 12px;
  color: #909399;
}

/* 练习阶段 */
.practice-session {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.practice-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #f5f7fa;
}

.progress-info {
  flex: 1;
  margin-right: 20px;
}

.current-question {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.timer-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-right: 20px;
}

.total-timer,
.question-timer {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: #606266;
}

.question-timer.time-warning {
  color: #f56c6c;
  font-weight: 600;
}

.practice-actions {
  display: flex;
  gap: 8px;
}

.question-card {
  border: 1px solid #e4e7ed;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.question-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.question-score {
  font-size: 13px;
  color: #909399;
}

.question-tools {
  display: flex;
  gap: 8px;
}

.bookmarked {
  color: #f7ba2a;
}

.question-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.question-text {
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
}

.question-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 12px;
  transition: all 0.3s;
}

.option-item:hover {
  border-color: #409eff;
  background: #f0f9ff;
}

.option-label {
  font-weight: 600;
  margin-right: 8px;
}

.question-blanks {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.blank-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.blank-label {
  min-width: 60px;
  font-weight: 600;
  color: #606266;
}

.answer-feedback {
  margin-top: 20px;
  padding: 16px;
  border-radius: 6px;
  background: #f0f9ff;
  border-left: 4px solid #409eff;
}

.feedback-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.feedback-header .correct {
  color: #67c23a;
}

.feedback-header .incorrect {
  color: #f56c6c;
}

.feedback-text {
  font-weight: 600;
}

.explanation {
  margin-top: 12px;
}

.explanation-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.explanation-content {
  line-height: 1.6;
  color: #606266;
}

.answer-controls {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 20px;
  border-top: 1px solid #e4e7ed;
  background: #fafafa;
}

/* 结果阶段 */
.practice-results {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.results-card {
  border: 1px solid #e4e7ed;
}

.results-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.results-icon {
  font-size: 24px;
  color: #f7ba2a;
}

.overall-stats {
  margin-bottom: 24px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 20px;
}

.stat-item {
  text-align: center;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fafafa;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.stat-value.correct {
  color: #67c23a;
}

.stat-value.incorrect {
  color: #f56c6c;
}

.stat-value.accuracy {
  color: #409eff;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.detailed-results {
  margin-top: 20px;
}

.answer-details {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.answer-detail-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #e4e7ed;
}

.question-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.question-number {
  font-weight: 600;
  color: #303133;
}

.detail-actions {
  display: flex;
  gap: 8px;
}

.detail-content {
  padding: 16px;
}

.question-text {
  font-size: 14px;
  color: #303133;
  margin-bottom: 12px;
}

.answer-comparison {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-answer,
.correct-answer {
  display: flex;
  gap: 8px;
}

.label {
  min-width: 80px;
  font-weight: 600;
  color: #606266;
}

.answer {
  color: #303133;
}

.knowledge-analysis {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.knowledge-item {
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fafafa;
}

.knowledge-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.knowledge-title {
  font-weight: 600;
  color: #303133;
}

.knowledge-progress {
  margin-bottom: 8px;
}

.knowledge-details {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #606266;
}

.suggestion {
  color: #e6a23c;
  font-weight: 600;
}

.error-review {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.no-errors {
  text-align: center;
  padding: 40px;
  color: #909399;
}

.no-errors h3 {
  margin: 16px 0 8px 0;
  color: #67c23a;
}

.error-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.error-item {
  padding: 16px;
  border: 1px solid #f56c6c;
  border-radius: 8px;
  background: #fef0f0;
}

.error-question {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.question-title {
  flex: 1;
  font-size: 14px;
  color: #303133;
}

.error-analysis {
  display: flex;
  align-items: center;
  gap: 12px;
}

.error-type {
  font-size: 12px;
  color: #f56c6c;
  background: white;
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #f56c6c;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
