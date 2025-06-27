<template>
  <div class="student-paper-detail">
    <div class="page-header">
      <el-breadcrumb>
        <el-breadcrumb-item :to="{ path: '/exams' }">考试列表</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: `/exams/${examId}` }">{{ exam?.title }}</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: `/exams/${examId}/answers` }">学生答案</el-breadcrumb-item>
        <el-breadcrumb-item>{{ studentPaper?.studentName }}的试卷</el-breadcrumb-item>
      </el-breadcrumb>
      <h1>学生试卷详情</h1>
      <p class="page-description">查看和评阅学生的完整答卷</p>
    </div>

    <!-- 学生信息概览 -->
    <el-card v-if="studentPaper" class="student-info-card">
      <template #header>
        <div class="card-header">
          <span>学生信息 - {{ studentPaper.studentName }}</span>
          <div class="header-actions">
            <el-button-group>
              <el-button type="primary" icon="Document" @click="exportStudentPaper">导出试卷</el-button>
              <el-button 
                type="success" 
                icon="MagicStick" 
                @click="batchEvaluateAll"
                :disabled="!hasUnevaluatedAnswers"
                :loading="batchEvaluating"
              >
                全部AI批阅
              </el-button>
              <el-button icon="Back" @click="goBack">返回列表</el-button>
            </el-button-group>
          </div>
        </div>
      </template>
      
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ studentPaper.studentName }}</div>
            <div class="stat-label">学生姓名</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ studentPaper.studentNumber || studentPaper.studentId }}</div>
            <div class="stat-label">学号</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ evaluatedCount }}/{{ studentPaper.answers?.length || 0 }}</div>
            <div class="stat-label">批阅进度</div>
            <el-progress 
              :percentage="progressPercentage" 
              :color="progressColor"
              size="small"
              :show-text="false"
              style="margin-top: 5px;"
            />
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value" :class="{ 'score-complete': isFullyEvaluated }">
              {{ totalScore?.toFixed(1) || '--' }}
            </div>
            <div class="stat-label">当前总分</div>
            <el-tag :type="isFullyEvaluated ? 'success' : 'warning'" size="small" style="margin-top: 5px;">
              {{ isFullyEvaluated ? '批阅完成' : '批阅中' }}
            </el-tag>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 答题详情 -->
    <el-card class="answers-detail-card">
      <template #header>
        <div class="card-header">
          <span>答题详情 ({{ studentPaper?.answers?.length || 0 }} 题)</span>
          <div class="header-actions">
            <el-radio-group v-model="viewMode" size="small">
              <el-radio-button label="all">全部题目</el-radio-button>
              <el-radio-button label="unevaluated">待批阅</el-radio-button>
              <el-radio-button label="evaluated">已批阅</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>

      <div v-loading="loading" class="answers-container">
        <div 
          v-for="(answer, index) in filteredAnswers" 
          :key="answer.id" 
          class="answer-item"
        >
          <el-card class="answer-card" :class="{ 'evaluated': answer.evaluated }">
            <template #header>
              <div class="answer-header">
                <div class="question-info">
                  <span class="question-number">第{{ getQuestionIndex(answer) }}题</span>
                  <span class="question-title">{{ answer.questionTitle }}</span>
                  <el-tag v-if="answer.questionType" size="small">
                    {{ getQuestionTypeText(answer.questionType) }}
                  </el-tag>
                </div>
                <div class="answer-actions">
                  <el-tag :type="answer.evaluated ? 'success' : 'warning'" size="small">
                    {{ answer.evaluated ? '已批阅' : '待批阅' }}
                  </el-tag>
                </div>
              </div>
            </template>

            <div class="answer-content">
              <!-- 题目内容 -->
              <div class="question-section">
                <h4>题目内容：</h4>
                <div class="question-content" v-html="answer.questionContent"></div>
              </div>

              <!-- 学生答案 -->
              <div class="student-answer-section">
                <div class="student-answer-header">
                  <h4>学生答案：</h4>
                  <div class="answer-edit-actions">
                    <el-button 
                      v-if="!isEditingStudentAnswer(answer.id)"
                      type="primary" 
                      size="small" 
                      icon="Edit"
                      @click="startEditStudentAnswer(answer)"
                      plain
                    >
                      编辑答案
                    </el-button>
                    <div v-else class="answer-edit-action-group">
                      <el-button 
                        type="success" 
                        size="small" 
                        @click="confirmStudentAnswerUpdate(answer)"
                        :loading="updatingStudentAnswers.has(answer.id)"
                      >
                        保存答案
                      </el-button>
                      <el-button 
                        size="small" 
                        @click="cancelStudentAnswerEdit(answer)"
                      >
                        取消
                      </el-button>
                    </div>
                  </div>
                </div>
                <div class="student-answer">
                  <div v-if="!isEditingStudentAnswer(answer.id)" class="answer-display">
                    <div v-if="answer.answerText" class="text-answer">
                      <pre>{{ answer.answerText }}</pre>
                    </div>
                    <div v-if="!answer.answerText" class="no-answer">
                      <el-text type="info">学生未作答</el-text>
                    </div>
                  </div>
                  <div v-else class="answer-edit">
                    <el-input 
                      v-model="editingStudentAnswers[answer.id]"
                      type="textarea" 
                      :rows="8"
                      :autosize="{ minRows: 6, maxRows: 15 }"
                      placeholder="请输入学生答案内容..."
                      resize="vertical"
                      maxlength="5000"
                      show-word-limit
                    />
                  </div>
                </div>
              </div>

              <!-- 批阅结果 -->
              <div v-if="answer.evaluated" class="evaluation-section">
                <div class="evaluation-header">
                  <h4>批阅结果</h4>
                  <div class="evaluation-actions">
                    <el-button 
                      v-if="!isEditingAnswer(answer.id)"
                      type="primary" 
                      size="small" 
                      icon="Edit"
                      @click="startEditAnswer(answer)"
                      class="edit-btn"
                    >
                      修改批阅
                    </el-button>
                    <div v-else class="edit-action-group">
                      <el-button 
                        type="success" 
                        size="small" 
                        @click="confirmAnswerUpdate(answer)"
                        :loading="updatingAnswers.has(answer.id)"
                      >
                        确认保存
                      </el-button>
                      <el-button 
                        size="small" 
                        @click="cancelAnswerEdit(answer)"
                      >
                        取消
                      </el-button>
                    </div>
                  </div>
                </div>
                
                <div class="evaluation-content">
                  <el-row :gutter="16">
                    <el-col :span="8">
                      <div class="score-display-section">
                        <h5>评分</h5>
                        <div v-if="!isEditingAnswer(answer.id)" class="score-display">
                          <div class="score-value">
                            <span class="current-score">{{ answer.score || 0 }}</span>
                            <span class="max-score-text">/ {{ answer.maxScore || 100 }}</span>
                          </div>
                        </div>
                        <div v-else class="score-edit">
                          <el-input-number 
                            v-model="editingScores[answer.id]"
                            :min="0" 
                            :max="answer.maxScore || 100"
                            :precision="1"
                            size="small"
                            style="width: 140px;"
                          />
                          <span class="max-score-text">/ {{ answer.maxScore || 100 }}</span>
                        </div>
                      </div>
                    </el-col>
                    <el-col :span="16">
                      <div class="feedback-display-section">
                        <h5>批阅反馈</h5>
                        <div v-if="!isEditingAnswer(answer.id)" class="feedback-display">
                          <div class="feedback-content" :class="{ 'expandable': isFeedbackLong(answer.feedback) }">
                            <div 
                              v-if="answer.feedback" 
                              class="feedback-text"
                              :class="{ 'collapsed': !expandedFeedbacks[answer.id] && isFeedbackLong(answer.feedback) }"
                            >
                              <pre class="feedback-pre">{{ answer.feedback }}</pre>
                            </div>
                            <div v-else class="no-feedback">
                              <el-text type="info">暂无反馈</el-text>
                            </div>
                          </div>
                          <div v-if="isFeedbackLong(answer.feedback)" class="feedback-toggle">
                            <el-button 
                              type="text" 
                              size="small"
                              @click="toggleFeedbackExpansion(answer.id)"
                            >
                              {{ expandedFeedbacks[answer.id] ? '收起' : '展开全部' }}
                            </el-button>
                          </div>
                        </div>
                        <div v-else class="feedback-edit-section">
                          <el-input 
                            v-model="editingFeedbacks[answer.id]"
                            type="textarea" 
                            :rows="5"
                            :autosize="{ minRows: 4, maxRows: 10 }"
                            placeholder="请输入详细的批阅反馈..."
                            resize="vertical"
                            maxlength="2000"
                            show-word-limit
                          />
                        </div>
                      </div>
                    </el-col>
                  </el-row>
                  <div class="evaluation-meta">
                    <el-text type="info" size="small">
                      批阅时间: {{ formatDateTime(answer.evaluatedAt) }}
                    </el-text>
                    <el-text v-if="isEditingAnswer(answer.id)" type="warning" size="small" style="margin-left: 16px;">
                      正在编辑中，请确认保存或取消
                    </el-text>
                  </div>
                </div>
              </div>
              <div v-else class="not-evaluated">
                <div class="quick-evaluation-section">
                  <h4>快速批阅</h4>
                  <el-row :gutter="16">
                    <el-col :span="8">
                      <div class="quick-score-section">
                        <label>评分:</label>
                        <el-input-number 
                          v-model="answer.score"
                          :min="0" 
                          :max="answer.maxScore || 100"
                          :precision="1"
                          size="small"
                          placeholder="0"
                          style="width: 120px;"
                        />
                        <span class="max-score-text">/ {{ answer.maxScore || 100 }}</span>
                      </div>
                    </el-col>
                    <el-col :span="12">
                      <div class="quick-feedback-section">
                        <label>反馈:</label>
                        <el-input 
                          v-model="answer.feedback"
                          placeholder="请输入批阅反馈..."
                          size="small"
                          maxlength="200"
                        />
                      </div>
                    </el-col>
                    <el-col :span="4">
                      <div class="quick-actions">
                        <el-button-group size="small">
                          <el-button 
                            type="primary"
                            @click="quickSaveEvaluation(answer)"
                            :loading="evaluatingAnswers.has(answer.id)"
                          >
                            保存
                          </el-button>
                          <el-button 
                            type="success"
                            @click="evaluateAnswer(answer)"
                            :loading="evaluatingAnswers.has(answer.id)"
                          >
                            AI批阅
                          </el-button>
                        </el-button-group>
                      </div>
                    </el-col>
                  </el-row>
                </div>
              </div>
            </div>
          </el-card>
        </div>

        <el-empty 
          v-if="!filteredAnswers?.length" 
          :description="getEmptyDescription()" 
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { answerApi } from '@/api/answer'
import { examApi } from '@/api/exam'
import { evaluationApi } from '@/api/evaluation'
import type { StudentAnswerResponse, StudentExamPaperResponse, ExamResponse } from '@/types/api'

const route = useRoute()
const router = useRouter()

// 路由参数
const examId = computed(() => route.params.examId as string)
const studentId = computed(() => route.params.studentId as string)

// 响应式数据
const loading = ref(false)
const batchEvaluating = ref(false)
const saving = ref(false)
const exam = ref<ExamResponse>()
const studentPaper = ref<StudentExamPaperResponse>()
const evaluatingAnswers = ref<Set<number>>(new Set())
const viewMode = ref('all') // 新增：视图模式

// 编辑相关状态（移除对话框，只保留必要状态）
// const editDialogVisible = ref(false) // 已移除
// const editingAnswer = ref<StudentAnswerResponse>() // 已移除

// 新增：批阅编辑相关状态
const editingScores = ref<Record<number, number | undefined>>({})
const editingFeedbacks = ref<Record<number, string>>({})
const expandedFeedbacks = ref<Record<number, boolean>>({})
const updatingScores = ref<Set<number>>(new Set())
const updatingFeedbacks = ref<Set<number>>(new Set())
const editingAnswers = ref<Set<number>>(new Set()) // 新增：正在编辑的答案ID集合
const updatingAnswers = ref<Set<number>>(new Set()) // 新增：正在更新的答案ID集合

// 新增：学生答案编辑相关状态
const editingStudentAnswers = ref<Record<number, string>>({})
const updatingStudentAnswers = ref<Set<number>>(new Set())
const editingStudentAnswerIds = ref<Set<number>>(new Set())

// 计算属性
const evaluatedCount = computed(() => {
  return studentPaper.value?.answers?.filter(answer => answer.evaluated).length || 0
})

const totalScore = computed(() => {
  const answers = studentPaper.value?.answers || []
  const evaluatedAnswers = answers.filter(answer => answer.evaluated && answer.score !== null)
  if (evaluatedAnswers.length === 0) return null
  return evaluatedAnswers.reduce((sum, answer) => sum + (answer.score || 0), 0)
})

const progressPercentage = computed(() => {
  const total = studentPaper.value?.answers?.length || 0
  if (total === 0) return 0
  return Math.round((evaluatedCount.value / total) * 100)
})

const progressColor = computed(() => {
  const percentage = progressPercentage.value
  if (percentage === 100) return '#67c23a'
  if (percentage >= 60) return '#e6a23c'
  return '#f56c6c'
})

const hasUnevaluatedAnswers = computed(() => {
  return studentPaper.value?.answers?.some(answer => !answer.evaluated) || false
})

// 添加前端计算的完全评估状态
const isFullyEvaluated = computed(() => {
  const answers = studentPaper.value?.answers || []
  if (answers.length === 0) return false
  return answers.every(answer => answer.evaluated)
})

// 新增：根据视图模式过滤答案
const filteredAnswers = computed(() => {
  const answers = studentPaper.value?.answers || []
  switch (viewMode.value) {
    case 'unevaluated':
      return answers.filter(answer => !answer.evaluated)
    case 'evaluated':
      return answers.filter(answer => answer.evaluated)
    default:
      return answers
  }
})

// 新增：批阅编辑相关方法
const isEditingAnswer = (answerId: number): boolean => {
  return editingAnswers.value.has(answerId)
}

const startEditAnswer = (answer: StudentAnswerResponse) => {
  editingAnswers.value.add(answer.id)
  editingScores.value[answer.id] = answer.score || 0
  editingFeedbacks.value[answer.id] = answer.feedback || ''
}

const cancelAnswerEdit = (answer: StudentAnswerResponse) => {
  editingAnswers.value.delete(answer.id)
  delete editingScores.value[answer.id]
  delete editingFeedbacks.value[answer.id]
}

const confirmAnswerUpdate = async (answer: StudentAnswerResponse) => {
  const newScore = editingScores.value[answer.id]
  const newFeedback = editingFeedbacks.value[answer.id]
  
  if (newScore === undefined || newFeedback === undefined) {
    cancelAnswerEdit(answer)
    return
  }

  // 检查是否有实际修改
  const hasChanges = newScore !== answer.score || newFeedback !== answer.feedback
  if (!hasChanges) {
    cancelAnswerEdit(answer)
    return
  }

  const confirmResult = await ElMessageBox.confirm(
    '确定要保存对此题批阅的修改吗？',
    '确认保存修改',
    {
      confirmButtonText: '确定保存',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).catch(() => false)

  if (!confirmResult) {
    cancelAnswerEdit(answer)
    return
  }

  updatingAnswers.value.add(answer.id)
  try {
    await answerApi.manuallyEvaluateAnswer(
      answer.id,
      newScore,
      newFeedback
    )
    
    await loadStudentPaper()
    cancelAnswerEdit(answer)
    ElMessage.success('批阅修改已保存')
  } catch (error) {
    console.error('保存批阅修改失败:', error)
    ElMessage.error('保存批阅修改失败')
  } finally {
    updatingAnswers.value.delete(answer.id)
  }
}

const isFeedbackLong = (feedback: string | undefined): boolean => {
  return (feedback?.length || 0) > 100
}

const hasScoreChanged = (answer: StudentAnswerResponse): boolean => {
  const editingScore = editingScores.value[answer.id]
  return editingScore !== undefined && editingScore !== answer.score
}

const toggleFeedbackExpansion = (answerId: number) => {
  expandedFeedbacks.value[answerId] = !expandedFeedbacks.value[answerId]
}

const startEditFeedback = (answer: StudentAnswerResponse) => {
  editingFeedbacks.value[answer.id] = answer.feedback || ''
}

const cancelScoreEdit = (answer: StudentAnswerResponse) => {
  editingScores.value[answer.id] = undefined
}

const cancelFeedbackEdit = (answer: StudentAnswerResponse) => {
  delete editingFeedbacks.value[answer.id]
}

const confirmScoreUpdate = async (answer: StudentAnswerResponse) => {
  const newScore = editingScores.value[answer.id]
  if (newScore === undefined || newScore === answer.score) {
    cancelScoreEdit(answer)
    return
  }

  const confirmResult = await ElMessageBox.confirm(
    `确定要将分数从 ${answer.score || 0} 分修改为 ${newScore} 分吗？`,
    '确认修改分数',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).catch(() => false)

  if (!confirmResult) {
    cancelScoreEdit(answer)
    return
  }

  updatingScores.value.add(answer.id)
  try {
    await answerApi.manuallyEvaluateAnswer(
      answer.id,
      newScore,
      answer.feedback || ''
    )
    
    await loadStudentPaper()
    editingScores.value[answer.id] = undefined
    ElMessage.success('分数已更新')
  } catch (error) {
    console.error('更新分数失败:', error)
    ElMessage.error('更新分数失败')
  } finally {
    updatingScores.value.delete(answer.id)
  }
}

const confirmFeedbackUpdate = async (answer: StudentAnswerResponse) => {
  const newFeedback = editingFeedbacks.value[answer.id]
  if (newFeedback === undefined || newFeedback === answer.feedback) {
    cancelFeedbackEdit(answer)
    return
  }

  const confirmResult = await ElMessageBox.confirm(
    '确定要保存修改后的批阅反馈吗？',
    '确认修改反馈',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).catch(() => false)

  if (!confirmResult) {
    cancelFeedbackEdit(answer)
    return
  }

  updatingFeedbacks.value.add(answer.id)
  try {
    await answerApi.manuallyEvaluateAnswer(
      answer.id,
      answer.score || 0,
      newFeedback
    )
    
    await loadStudentPaper()
    delete editingFeedbacks.value[answer.id]
    ElMessage.success('反馈已更新')
  } catch (error) {
    console.error('更新反馈失败:', error)
    ElMessage.error('更新反馈失败')
  } finally {
    updatingFeedbacks.value.delete(answer.id)
  }
}

// 新增：学生答案编辑相关方法
const isEditingStudentAnswer = (answerId: number): boolean => {
  return editingStudentAnswerIds.value.has(answerId)
}

const startEditStudentAnswer = (answer: StudentAnswerResponse) => {
  editingStudentAnswerIds.value.add(answer.id)
  editingStudentAnswers.value[answer.id] = answer.answerText || ''
}

const cancelStudentAnswerEdit = (answer: StudentAnswerResponse) => {
  editingStudentAnswerIds.value.delete(answer.id)
  delete editingStudentAnswers.value[answer.id]
}

const confirmStudentAnswerUpdate = async (answer: StudentAnswerResponse) => {
  const newAnswerText = editingStudentAnswers.value[answer.id]
  
  if (newAnswerText === undefined) {
    cancelStudentAnswerEdit(answer)
    return
  }

  // 检查是否有实际修改
  const hasChanges = newAnswerText !== (answer.answerText || '')
  if (!hasChanges) {
    cancelStudentAnswerEdit(answer)
    return
  }

  const confirmResult = await ElMessageBox.confirm(
    '确定要保存对学生答案的修改吗？',
    '确认保存答案修改',
    {
      confirmButtonText: '确定保存',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).catch(() => false)

  if (!confirmResult) {
    cancelStudentAnswerEdit(answer)
    return
  }

  updatingStudentAnswers.value.add(answer.id)
  try {
    await answerApi.updateAnswer(answer.id, {
      answerText: newAnswerText
    })
    
    await loadStudentPaper()
    cancelStudentAnswerEdit(answer)
    ElMessage.success('学生答案已更新')
  } catch (error) {
    console.error('更新学生答案失败:', error)
    ElMessage.error('更新学生答案失败')
  } finally {
    updatingStudentAnswers.value.delete(answer.id)
  }
}

// 方法
const progressFormat = (percentage: number) => {
  return `${evaluatedCount.value}/${studentPaper.value?.answers?.length || 0}`
}

const formatDateTime = (datetime: string | undefined) => {
  if (!datetime) return '未知'
  return new Date(datetime).toLocaleString('zh-CN')
}

const getQuestionTypeText = (type: string) => {
  switch (type) {
    case 'SINGLE_CHOICE': return '单选题'
    case 'MULTIPLE_CHOICE': return '多选题'
    case 'TRUE_FALSE': return '判断题'
    case 'SHORT_ANSWER': return '简答题'
    case 'ESSAY': return '论述题'
    case 'FILE_UPLOAD': return '文件上传题'
    default: return type
  }
}

// 新增：获取题目在原始数组中的索引
const getQuestionIndex = (answer: StudentAnswerResponse) => {
  const allAnswers = studentPaper.value?.answers || []
  const originalIndex = allAnswers.findIndex(a => a.id === answer.id)
  return originalIndex + 1
}

// 新增：获取空状态描述
const getEmptyDescription = () => {
  switch (viewMode.value) {
    case 'unevaluated':
      return '所有题目已批阅完成'
    case 'evaluated':
      return '暂无已批阅的题目'
    default:
      return '暂无答题记录'
  }
}

const loadExam = async () => {
  try {
    exam.value = await examApi.getExam(parseInt(examId.value))
  } catch (error) {
    console.error('加载考试信息失败:', error)
    ElMessage.error('加载考试信息失败')
  }
}

const loadStudentPaper = async () => {
  loading.value = true
  try {
    studentPaper.value = await answerApi.getStudentExamPaper(parseInt(examId.value), parseInt(studentId.value))
  } catch (error) {
    console.error('加载学生试卷失败:', error)
    ElMessage.error('加载学生试卷失败')
  } finally {
    loading.value = false
  }
}

const evaluateAnswer = async (answer: StudentAnswerResponse) => {
  evaluatingAnswers.value.add(answer.id)
  try {
    ElMessage.info('正在提交AI批阅请求...')
    
    // 调用单个答案批阅API，返回的是带有任务信息的对象
    const result = await evaluationApi.evaluateAnswer(answer.id)
    
    // 检查返回结果，evaluateAnswer返回的是对象格式 {success: boolean, taskId: string, message: string}
    if (result && typeof result === 'object' && 'taskId' in result) {
      const taskId = (result as any).taskId
      
      if (taskId) {
        ElMessage.success('AI批阅任务已提交，正在处理中...')
        
        // 为单个答案启动简化的轮询
        startSingleAnswerPolling(taskId, answer.id)
      } else {
        throw new Error('无法获取任务ID')
      }
    } else if (typeof result === 'string') {
      // 如果返回的是字符串格式的任务信息
      const taskIdMatch = (result as string).match(/ID:\s*([^\s]+)/)
      const taskId = taskIdMatch ? taskIdMatch[1] : null
      
      if (taskId) {
        ElMessage.success('AI批阅任务已提交，正在处理中...')
        startSingleAnswerPolling(taskId, answer.id)
      } else {
        throw new Error('无法解析任务ID')
      }
    } else {
      // 直接返回了批阅结果
      ElMessage.success('AI批阅完成')
      await loadStudentPaper()
    }
    
  } catch (error: any) {
    console.error('AI批阅失败:', error)
    ElMessage.error('AI批阅失败: ' + (error?.message || '未知错误'))
    evaluatingAnswers.value.delete(answer.id)
  }
}

// 新增：单个答案轮询
const startSingleAnswerPolling = (taskId: string, answerId: number) => {
  let pollCount = 0
  const maxPolls = 60 // 最多轮询1分钟
  
  const pollInterval = setInterval(async () => {
    try {
      const taskStatus = await evaluationApi.getTaskStatus(taskId)
      
      if (taskStatus.status === 'COMPLETED') {
        clearInterval(pollInterval)
        evaluatingAnswers.value.delete(answerId)
        ElMessage.success('AI批阅完成')
        await loadStudentPaper()
        
      } else if (taskStatus.status === 'FAILED' || taskStatus.status === 'CANCELLED') {
        clearInterval(pollInterval)
        evaluatingAnswers.value.delete(answerId)
        ElMessage.error('AI批阅失败')
      }
      
      pollCount++
      if (pollCount >= maxPolls) {
        clearInterval(pollInterval)
        evaluatingAnswers.value.delete(answerId)
        ElMessage.warning('批阅超时，请刷新页面查看结果')
      }
      
    } catch (error) {
      console.error('轮询单个答案状态失败:', error)
      pollCount++
      if (pollCount >= maxPolls) {
        clearInterval(pollInterval)
        evaluatingAnswers.value.delete(answerId)
        ElMessage.error('批阅状态检查失败')
      }
    }
  }, 1000)
}

const batchEvaluateAll = async () => {
  const unevaluatedAnswers = studentPaper.value?.answers?.filter(answer => !answer.evaluated) || []
  if (unevaluatedAnswers.length === 0) {
    ElMessage.warning('所有题目已批阅完成')
    return
  }

  const confirmResult = await ElMessageBox.confirm(
    `确定要对所有未批阅的 ${unevaluatedAnswers.length} 道题目进行AI批阅吗？批阅过程可能需要一些时间，请耐心等待。`,
    '批量AI批阅确认',
    {
      confirmButtonText: '确定批阅',
      cancelButtonText: '取消',
      type: 'warning',
      dangerouslyUseHTMLString: true,
      message: `
        <div style="margin: 10px 0;">
          <p>即将对以下 <strong>${unevaluatedAnswers.length}</strong> 道题目进行AI批阅：</p>
          <ul style="margin: 10px 0; padding-left: 20px; max-height: 200px; overflow-y: auto;">
            ${unevaluatedAnswers.map((answer, index) => 
              `<li>第${getQuestionIndex(answer)}题: ${answer.questionTitle || '无标题'}</li>`
            ).join('')}
          </ul>
          <p style="color: #e6a23c; font-size: 14px;">
            <i class="el-icon-warning"></i> 注意：AI批阅可能需要几分钟时间，请不要关闭页面
          </p>
        </div>
      `
    }
  ).catch(() => false)

  if (!confirmResult) return

  batchEvaluating.value = true
  
  try {
    // 调用批量AI批阅API - 使用新的学生特定API
    ElNotification.info({
      title: '开始AI批阅',
      message: `正在为学生 ${studentPaper.value?.studentName} 批阅 ${unevaluatedAnswers.length} 道题目，请稍候...`,
      duration: 0 // 不自动关闭
    })
    
    // 使用新的API端点批量评估特定学生的答案
    const taskResult = await evaluationApi.batchEvaluateStudentAnswers(
      parseInt(examId.value), 
      parseInt(studentId.value)
    )
    
    // 解析任务ID
    const taskIdMatch = taskResult.match(/ID:\s*([^\s]+)/)
    const taskId = taskIdMatch ? taskIdMatch[1] : null
    
    if (taskId) {
      ElNotification.success({
        title: '批阅任务已启动',
        message: `任务ID: ${taskId}。正在后台处理，请稍候查看结果。`,
        duration: 5000
      })
      
      // 开始轮询任务状态
      startTaskPolling(taskId)
    } else {
      throw new Error('无法获取任务ID')
    }
    
  } catch (error: any) {
    console.error('批量AI批阅失败:', error)
    ElMessage.error('批量AI批阅启动失败: ' + (error?.message || '未知错误'))
    batchEvaluating.value = false
  }
}

// 新增：任务轮询功能
const taskPollingTimer = ref<number | null>(null)
const pollingCount = ref(0)
const maxPollingAttempts = 180 // 最多轮询3分钟 (180 * 1秒)

const startTaskPolling = (taskId: string) => {
  pollingCount.value = 0
  
  const pollTaskStatus = async () => {
    try {
      const taskStatus = await evaluationApi.getTaskStatus(taskId)
      
      console.log(`轮询任务状态 (${pollingCount.value + 1}/${maxPollingAttempts}):`, taskStatus)
      
      if (taskStatus.status === 'COMPLETED') {
        // 任务完成
        clearTaskPolling()
        batchEvaluating.value = false
        
        ElNotification.success({
          title: '批阅完成',
          message: `学生 ${studentPaper.value?.studentName} 的答案批阅已完成！`,
          duration: 5000
        })
        
        // 重新加载数据
        await loadStudentPaper()
        
      } else if (taskStatus.status === 'FAILED' || taskStatus.status === 'CANCELLED') {
        // 任务失败或取消
        clearTaskPolling()
        batchEvaluating.value = false
        
        ElNotification.error({
          title: '批阅失败',
          message: `批阅任务${taskStatus.status === 'FAILED' ? '失败' : '被取消'}，请检查错误信息或重试。`,
          duration: 10000
        })
        
      } else if (taskStatus.status === 'RUNNING') {
        // 任务正在运行，显示进度
        const progress = taskStatus.progress || 0
        ElNotification.info({
          title: '批阅进行中',
          message: `正在批阅中... 进度: ${Math.round(progress)}%`,
          duration: 2000
        })
        
      }
      
      pollingCount.value++
      
      // 检查是否超过最大轮询次数
      if (pollingCount.value >= maxPollingAttempts) {
        clearTaskPolling()
        batchEvaluating.value = false
        ElMessage.warning('批阅超时，请手动刷新页面查看结果')
      }
      
    } catch (error: any) {
      console.error('轮询任务状态失败:', error)
      pollingCount.value++
      
      if (pollingCount.value >= maxPollingAttempts) {
        clearTaskPolling()
        batchEvaluating.value = false
        ElMessage.error('无法获取批阅状态，请手动刷新页面查看结果')
      }
    }
  }
  
  // 立即执行一次
  pollTaskStatus()
  
  // 设置定时轮询（每秒一次）
  taskPollingTimer.value = window.setInterval(pollTaskStatus, 1000)
}

const clearTaskPolling = () => {
  if (taskPollingTimer.value) {
    clearInterval(taskPollingTimer.value)
    taskPollingTimer.value = null
  }
}

const quickSaveEvaluation = async (answer: StudentAnswerResponse) => {
  if (!answer.score && answer.score !== 0) {
    ElMessage.warning('请输入分数')
    return
  }

  const confirmResult = await ElMessageBox.confirm(
    `确定要保存此题的批阅结果吗？\n分数：${answer.score} 分\n反馈：${answer.feedback || '无'}`,
    '确认保存批阅',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).catch(() => false)

  if (!confirmResult) return

  evaluatingAnswers.value.add(answer.id)
  try {
    await answerApi.manuallyEvaluateAnswer(
      answer.id,
      answer.score || 0,
      answer.feedback || ''
    )
    
    await loadStudentPaper()
    ElMessage.success('批阅已保存')
  } catch (error: any) {
    console.error('保存批阅失败:', error)
    ElMessage.error('保存批阅失败')
  } finally {
    evaluatingAnswers.value.delete(answer.id)
  }
}

const exportStudentPaper = async () => {
  try {
    await answerApi.exportStudentPaper(parseInt(examId.value), parseInt(studentId.value))
    ElMessage.success('试卷导出成功')
  } catch (error) {
    console.error('导出试卷失败:', error)
    ElMessage.error('导出试卷失败')
  }
}

const goBack = () => {
  router.push(`/exams/${examId.value}/answers`)
}

// 生命周期
onMounted(() => {
  loadExam()
  loadStudentPaper()
})

// 在组件卸载时清理定时器
onBeforeUnmount(() => {
  clearTaskPolling()
})
</script>

<style scoped>
.student-paper-detail {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 10px 0 5px 0;
  color: #303133;
}

.page-description {
  color: #909399;
  margin: 0;
}

.student-info-card {
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
  padding: 10px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.stat-value.score-complete {
  color: #67c23a;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.answers-detail-card {
  margin-bottom: 20px;
}

.answers-container {
  max-height: 800px;
  overflow-y: auto;
}

.answer-item {
  margin-bottom: 20px;
}

.answer-card {
  transition: all 0.3s ease;
}

.answer-card.evaluated {
  border-left: 4px solid #67c23a;
}

.answer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.question-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.question-number {
  font-weight: bold;
  color: #409eff;
}

.question-title {
  font-weight: 500;
}

.answer-actions {
  display: flex;
  gap: 10px;
}

.answer-content {
  padding: 10px 0;
}

.question-section,
.student-answer-section,
.evaluation-section {
  margin-bottom: 20px;
}

.question-section h4,
.student-answer-section h4,
.evaluation-section h4 {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 14px;
}

.question-content {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 4px;
  border-left: 4px solid #409eff;
}

.student-answer-section {
  margin-bottom: 20px;
}

.student-answer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.student-answer-header h4 {
  margin: 0;
  color: #303133;
  font-size: 14px;
}

.answer-edit-actions {
  display: flex;
  gap: 8px;
}

.answer-edit-action-group {
  display: flex;
  gap: 8px;
}

.student-answer {
  background: #fafafa;
  padding: 15px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}

.answer-display {
  min-height: 40px;
}

.answer-edit {
  background: #fff;
  border-radius: 4px;
}

.text-answer pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
  font-family: inherit;
}

.file-answer a {
  text-decoration: none;
  color: inherit;
}

.no-answer {
  text-align: center;
  padding: 20px;
}

.evaluation-section {
  background: #f0f9ff;
  padding: 15px;
  border-radius: 4px;
  border: 1px solid #b3d8ff;
}

.evaluation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e1ecf4;
}

.evaluation-header h4 {
  margin: 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.evaluation-actions {
  display: flex;
  gap: 5px;
}

.edit-inline-btn {
  color: #409eff;
  padding: 2px 8px;
}

.edit-inline-btn:hover {
  background: #ecf5ff;
}

.evaluation-content {
  margin-bottom: 10px;
}

.score-display-section h5,
.feedback-edit-section h5 {
  margin: 0 0 8px 0;
  color: #606266;
  font-size: 14px;
  font-weight: 500;
}

.score-input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.max-score-text {
  color: #909399;
  font-size: 14px;
}

.evaluation-meta {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid #e1ecf4;
}

.quick-evaluation-section {
  background: #fff7e6;
  padding: 15px;
  border-radius: 4px;
  border: 1px solid #ffd591;
}

.quick-evaluation-section h4 {
  margin: 0 0 15px 0;
  color: #d48806;
  font-size: 16px;
  font-weight: 600;
}

.quick-score-section,
.quick-feedback-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quick-score-section label,
.quick-feedback-section label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.evaluation-section .el-row {
  margin-bottom: 10px;
}

/* 新增：编辑状态样式 */
.edit-btn {
  border-radius: 4px;
}

.edit-action-group {
  display: flex;
  gap: 8px;
}

.score-display {
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.score-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.current-score {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.score-edit {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 优化反馈显示 */
.feedback-pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
  font-family: inherit;
  line-height: 1.5;
}

.feedback-content {
  position: relative;
}

.feedback-text.collapsed {
  max-height: 80px;
  overflow: hidden;
  position: relative;
}

.feedback-text.collapsed::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 20px;
  background: linear-gradient(transparent, white);
}

.feedback-toggle {
  margin-top: 8px;
  text-align: center;
}

.feedback-edit-section {
  margin-top: 8px;
}

.evaluation-meta {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e1ecf4;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.evaluation-section .el-col h5 {
  margin: 0 0 8px 0;
  color: #606266;
  font-size: 14px;
  font-weight: 500;
}

.max-score {
  color: #909399;
}

/* 新增：批阅编辑相关样式 */
.current-score-display {
  margin-top: 5px;
}

.feedback-display-section h5 {
  margin: 0 0 8px 0;
  color: #606266;
  font-size: 14px;
  font-weight: 500;
}

.feedback-display {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  padding: 12px;
}

.feedback-content.expandable {
  position: relative;
}

.feedback-text {
  white-space: pre-wrap;
  word-wrap: break-word;
  line-height: 1.5;
  margin: 0;
}

.feedback-text.collapsed {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.no-feedback {
  text-align: center;
  color: #909399;
  font-style: italic;
}

.feedback-toggle {
  margin-top: 8px;
  text-align: center;
}

.feedback-actions {
  margin-top: 8px;
  text-align: right;
}

.feedback-edit-section {
  margin-top: 8px;
}

.feedback-edit-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
</style>
