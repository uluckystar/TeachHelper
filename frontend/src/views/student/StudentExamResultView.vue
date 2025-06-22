<template>
  <div class="student-exam-result">
    <div class="page-header">
      <el-breadcrumb>
        <el-breadcrumb-item :to="{ path: '/my-exams' }">我的考试</el-breadcrumb-item>
        <el-breadcrumb-item>{{ examResult?.examTitle || '考试结果' }}</el-breadcrumb-item>
      </el-breadcrumb>
      <h1>我的考试结果</h1>
      <p class="page-description">查看你的考试答案和评估结果</p>
    </div>

    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="8" animated />
    </div>

    <div v-else-if="examResult">
      <!-- 考试基本信息 -->
      <el-card class="exam-info-card">
        <template #header>
          <div class="card-header">
            <span>考试信息</span>
            <div class="header-actions">
              <el-button type="primary" @click="goToAnswers">
                <el-icon><Document /></el-icon>
                查看答卷详情
              </el-button>
              <el-button @click="goBack">
                <el-icon><ArrowLeft /></el-icon>
                返回
              </el-button>
            </div>
          </div>
        </template>
        
        <el-row :gutter="24">
          <el-col :span="12">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="考试标题">{{ examResult.examTitle }}</el-descriptions-item>
              <el-descriptions-item label="考试描述">{{ examResult.examDescription || '无描述' }}</el-descriptions-item>
              <el-descriptions-item label="题目数量">{{ examResult.answeredQuestions }} 道</el-descriptions-item>
            </el-descriptions>
          </el-col>
          <el-col :span="12">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="开始时间">
                {{ formatDateTime(examResult.startTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="提交时间">
                {{ formatDateTime(examResult.submitTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="用时">
                {{ formatDuration(examResult.duration) }}
              </el-descriptions-item>
            </el-descriptions>
          </el-col>
        </el-row>
      </el-card>

      <!-- 成绩概览 -->
      <el-card class="score-overview-card">
        <template #header>
          <span>成绩概览</span>
        </template>
        
        <div class="score-display">
          <div class="score-circle">
            <el-progress 
              type="circle" 
              :percentage="getScorePercentage()" 
              :width="120"
              :stroke-width="8"
              :color="getScoreColor()"
            >
              <template #default>
                <span class="score-text">
                  {{ examResult.totalScore || 0 }}
                </span>
              </template>
            </el-progress>
          </div>
          <div class="score-details">
            <div class="score-item">
              <span class="label">我的得分：</span>
              <span class="value">{{ examResult.totalScore || 0 }} 分</span>
            </div>
            <div class="score-item">
              <span class="label">总分：</span>
              <span class="value">{{ getTotalPossibleScore() }} 分</span>
            </div>
            <div class="score-item">
              <span class="label">得分率：</span>
              <span class="value">{{ getScorePercentage() }}%</span>
            </div>
            <div class="score-item">
              <span class="label">成绩等级：</span>
              <span class="value" :class="getGradeClass()">{{ getGrade() }}</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 答题详情 -->
      <el-card class="answers-card">
        <template #header>
          <div class="card-header">
            <span>答题详情</span>
            <div class="header-actions">
              <el-button 
                type="primary" 
                icon="Download" 
                @click="downloadResultPDF"
                :loading="downloadLoading"
              >
                下载成绩单
              </el-button>
            </div>
          </div>
        </template>

        <div class="answers-list">
          <div 
            v-for="(answer, index) in examResult.answers" 
            :key="answer.id"
            class="answer-item"
          >
            <div class="question-header">
              <div class="question-info">
                <span class="question-number">第 {{ index + 1 }} 题</span>
                <el-tag size="small" :type="getQuestionTypeTag(answer.questionType || 'ESSAY')">
                  {{ getQuestionTypeText(answer.questionType || 'ESSAY') }}
                </el-tag>
                <el-tag 
                  size="small" 
                  :type="answer.evaluated ? 'success' : 'warning'"
                >
                  {{ answer.evaluated ? '已评分' : '待评分' }}
                </el-tag>
              </div>
              <div class="score-info" v-if="answer.evaluated">
                <span class="score">
                  {{ answer.score || 0 }} / {{ answer.maxScore || 0 }} 分
                </span>
              </div>
            </div>

            <div class="question-content">
              <h4>题目内容：</h4>
              <p>{{ answer.questionContent }}</p>
            </div>

            <div class="answer-content">
              <h4>我的答案：</h4>
              <div class="answer-text">{{ answer.answerText || '未作答' }}</div>
            </div>

            <div v-if="answer.feedback && answer.evaluated" class="feedback-content">
              <h4>评价反馈：</h4>
              <div class="feedback-text">
                <el-alert 
                  :type="getFeedbackType(answer.score || 0, answer.maxScore || 0)"
                  :closable="false"
                >
                  {{ answer.feedback }}
                </el-alert>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <div v-else-if="!loading">
      <!-- 未参加考试的状态 -->
      <div v-if="examResult && (examResult as any).status === 'NOT_STARTED'" class="not-started-container">
        <el-result
          icon="info"
          title="尚未参加考试"
          sub-title="您还没有参加这个考试，或者还没有提交答案"
        >
          <template #extra>
            <el-button type="primary" @click="goToExam">参加考试</el-button>
            <el-button @click="goBack">返回我的考试</el-button>
          </template>
        </el-result>
      </div>
      
      <!-- 已提交但未评分的状态 -->
      <div v-else-if="examResult && (examResult as any).status === 'SUBMITTED'" class="submitted-container">
        <el-result
          icon="success"
          title="答案已提交"
          sub-title="您的答案已成功提交，请等待老师评分"
        >
          <template #extra>
            <el-button type="primary" @click="refreshResult">刷新结果</el-button>
            <el-button @click="goBack">返回我的考试</el-button>
          </template>
        </el-result>
        
        <!-- 显示基本信息 -->
        <el-card v-if="examResult" class="exam-info-card">
          <template #header>
            <div class="card-header">
              <span>考试信息</span>
            </div>
          </template>
          
          <el-descriptions :column="2" border>
            <el-descriptions-item label="考试标题">{{ examResult.examTitle }}</el-descriptions-item>
            <el-descriptions-item label="考试描述">{{ examResult.examDescription || '无描述' }}</el-descriptions-item>
            <el-descriptions-item label="已答题数">{{ examResult.answers?.length || 0 }} 道</el-descriptions-item>
            <el-descriptions-item label="提交状态">已提交，等待评分</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </div>
      
      <!-- 完全没有数据的情况 -->
      <div v-else class="no-data-container">
        <el-empty description="未找到考试结果">
          <el-button type="primary" @click="goBack">返回我的考试</el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document, ArrowLeft } from '@element-plus/icons-vue'
import { examResultApi } from '@/api/examResult'
import type { ExamResult } from '@/types/api'

const route = useRoute()
const router = useRouter()
const examId = parseInt(route.params.examId as string)

const loading = ref(false)
const downloadLoading = ref(false)
const examResult = ref<ExamResult | null>(null)

const loadExamResult = async () => {
  loading.value = true
  try {
    console.log('调用考试结果API，examId:', examId)
    console.log('请求URL:', `/api/exam-results/student/exam/${examId}`)
    const response = await examResultApi.getStudentExamResult(examId)
    console.log('API响应:', response)
    examResult.value = response.data
  } catch (error: any) {
    console.error('Failed to load exam result:', error)
    console.error('错误详情:', {
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data,
      url: error.config?.url
    })
    ElMessage.error(error.response?.data?.message || '加载考试结果失败')
  } finally {
    loading.value = false
  }
}

const getTotalPossibleScore = () => {
  // 优先使用后端提供的满分数据
  if (examResult.value?.totalPossibleScore !== undefined) {
    return examResult.value.totalPossibleScore
  }
  // 后备方案：从答案中计算
  if (!examResult.value?.answers) return 0
  return examResult.value.answers.reduce((total, answer) => total + (answer.maxScore || 0), 0)
}

const getScorePercentage = () => {
  // 优先使用后端提供的得分率
  if (examResult.value?.scorePercentage !== undefined) {
    return Math.round(examResult.value.scorePercentage)
  }
  // 后备方案：自行计算
  const totalPossible = getTotalPossibleScore()
  if (totalPossible === 0) return 0
  return Math.round(((examResult.value?.totalScore || 0) / totalPossible) * 100)
}

const getScoreColor = () => {
  const percentage = getScorePercentage()
  if (percentage >= 90) return '#67c23a'
  if (percentage >= 80) return '#e6a23c'
  if (percentage >= 60) return '#f56c6c'
  return '#909399'
}

const getGrade = () => {
  // 优先使用后端提供的成绩等级
  if (examResult.value?.grade) {
    return examResult.value.grade
  }
  // 后备方案：根据得分率计算
  const percentage = getScorePercentage()
  if (percentage >= 90) return '优秀'
  if (percentage >= 80) return '良好'
  if (percentage >= 70) return '中等'
  if (percentage >= 60) return '及格'
  return '不及格'
}

const getGradeClass = () => {
  const percentage = getScorePercentage()
  if (percentage >= 90) return 'grade-excellent'
  if (percentage >= 80) return 'grade-good'
  if (percentage >= 70) return 'grade-average'
  if (percentage >= 60) return 'grade-pass'
  return 'grade-fail'
}

const getStatusTagType = (status: string) => {
  switch (status) {
    case 'EVALUATED': return 'success'
    case 'IN_PROGRESS': return 'warning'
    case 'ENDED': return 'info'
    default: return 'info'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'EVALUATED': return '已评分'
    case 'IN_PROGRESS': return '评分中'
    case 'ENDED': return '已结束'
    default: return '未知状态'
  }
}

const getQuestionTypeTag = (type: string) => {
  switch (type) {
    case 'SINGLE_CHOICE': return 'primary'
    case 'MULTIPLE_CHOICE': return 'success'
    case 'SHORT_ANSWER': return 'warning'
    case 'ESSAY': return 'danger'
    default: return 'info'
  }
}

const getQuestionTypeText = (type: string) => {
  switch (type) {
    case 'SINGLE_CHOICE': return '单选题'
    case 'MULTIPLE_CHOICE': return '多选题'
    case 'SHORT_ANSWER': return '简答题'
    case 'ESSAY': return '论述题'
    case 'FILL_BLANK': return '填空题'
    default: return '未知题型'
  }
}

const getFeedbackType = (score: number, maxScore: number) => {
  const percentage = (score / maxScore) * 100
  if (percentage >= 90) return 'success'
  if (percentage >= 70) return 'warning'
  return 'error'
}

const formatDateTime = (dateString: string) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

const formatDuration = (seconds: number) => {
  if (!seconds) return '0分钟'
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

const downloadResultPDF = async () => {
  downloadLoading.value = true
  try {
    // TODO: 实现PDF下载功能
    ElMessage.success('成绩单下载功能开发中...')
  } catch (error) {
    console.error('Failed to download PDF:', error)
    ElMessage.error('下载失败')
  } finally {
    downloadLoading.value = false
  }
}

const goToAnswers = () => {
  router.push(`/my-exams/${examId}/answers`)
}

const goToExam = () => {
  router.push(`/exams/${examId}/take`)
}

const goBack = () => {
  router.push('/my-exams')
}

const refreshResult = () => {
  loadExamResult()
}

onMounted(() => {
  loadExamResult()
})
</script>

<style scoped>
.student-exam-result {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
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
.score-overview-card,
.answers-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.score-display {
  display: flex;
  align-items: center;
  gap: 40px;
  padding: 20px 0;
}

.score-circle {
  flex-shrink: 0;
}

.score-text {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.score-details {
  flex: 1;
}

.score-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.score-item .label {
  color: #606266;
  font-weight: 500;
}

.score-item .value {
  font-weight: 600;
  color: #303133;
}

.grade-excellent { color: #67c23a; }
.grade-good { color: #e6a23c; }
.grade-average { color: #409eff; }
.grade-pass { color: #f56c6c; }
.grade-fail { color: #909399; }

.answers-list {
  gap: 16px;
}

.answer-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
  background: #fafafa;
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
  gap: 8px;
}

.question-number {
  font-weight: 600;
  color: #303133;
}

.score-info .score {
  font-size: 16px;
  font-weight: 600;
  color: #67c23a;
}

.question-content,
.answer-content,
.feedback-content {
  margin-bottom: 16px;
}

.question-content h4,
.answer-content h4,
.feedback-content h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.question-content p {
  margin: 0;
  line-height: 1.6;
  color: #606266;
}

.answer-text {
  padding: 12px;
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  line-height: 1.6;
  color: #303133;
  min-height: 60px;
}

.feedback-text {
  margin-top: 8px;
}

.loading-container {
  padding: 40px;
}
</style>
