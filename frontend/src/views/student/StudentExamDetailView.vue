<template>
  <div class="student-exam-detail">
    <div class="page-header">
      <el-breadcrumb>
        <el-breadcrumb-item :to="{ path: '/my-exams' }">我的考试</el-breadcrumb-item>
        <el-breadcrumb-item>{{ exam?.title || '考试详情' }}</el-breadcrumb-item>
      </el-breadcrumb>
      <h1>{{ exam?.title }}</h1>
    </div>

    <div v-if="loading" class="loading">
      <el-skeleton :rows="8" animated />
    </div>

    <div v-else-if="exam" class="exam-content">
      <el-row :gutter="24">
        <!-- 左侧：考试信息 -->
        <el-col :span="16">
          <!-- 考试基本信息 -->
          <el-card class="exam-info-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span>考试信息</span>
                <el-tag :type="getStatusTagType(exam.status)" effect="dark">
                  {{ getStatusDisplayText(exam.status) }}
                </el-tag>
              </div>
            </template>
            
            <el-descriptions :column="2" border>
              <el-descriptions-item label="考试标题">
                {{ exam.title }}
              </el-descriptions-item>
              <el-descriptions-item label="考试状态">
                <el-tag :type="getStatusTagType(exam.status)" effect="plain">
                  {{ getStatusDisplayText(exam.status) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="考试描述" :span="2">
                {{ exam.description || '暂无描述' }}
              </el-descriptions-item>
              <el-descriptions-item label="开始时间">
                {{ exam.startTime ? formatDate(exam.startTime) : '未设置' }}
              </el-descriptions-item>
              <el-descriptions-item label="结束时间">
                {{ exam.endTime ? formatDate(exam.endTime) : '未设置' }}
              </el-descriptions-item>
              <el-descriptions-item label="考试时长">
                {{ exam.duration ? `${exam.duration} 分钟` : '未设置' }}
              </el-descriptions-item>
              <el-descriptions-item label="题目数量">
                {{ exam.questionCount || 0 }} 题
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 我的答题状态 -->
          <el-card class="my-status-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span>我的答题状态</span>
              </div>
            </template>
            
            <div v-if="submissionInfo">
              <el-alert
                v-if="submissionInfo.hasSubmitted"
                title="您已提交试卷"
                type="success"
                :description="`提交时间：${formatDate(submissionInfo.submittedAt)}`"
                show-icon
                :closable="false"
              />
              <el-alert
                v-else-if="hasStarted"
                title="您已开始答题，但尚未提交"
                type="warning"
                description="请及时完成并提交试卷"
                show-icon
                :closable="false"
              />
              <el-alert
                v-else
                title="您尚未开始答题"
                type="info"
                description="点击下方按钮开始答题"
                show-icon
                :closable="false"
              />

              <!-- 答题进度 -->
              <div v-if="hasStarted && !submissionInfo.hasSubmitted" class="answer-progress">
                <el-divider content-position="left">答题进度</el-divider>
                <div class="progress-info">
                  <span>已答题数：{{ answeredCount }}/{{ exam.questionCount || 0 }}</span>
                  <span class="progress-percentage">{{ progressPercentage }}%</span>
                </div>
                <el-progress 
                  :percentage="progressPercentage" 
                  :status="progressPercentage === 100 ? 'success' : undefined"
                />
              </div>
            </div>
            
            <el-skeleton v-else :rows="3" animated />
          </el-card>

          <!-- 已提交答案预览（如果已提交且允许查看） -->
          <el-card 
            v-if="submissionInfo?.hasSubmitted && canViewAnswers" 
            class="submitted-answers-card" 
            shadow="never"
          >
            <template #header>
              <div class="card-header">
                <span>我的答案</span>
                <el-button size="small" @click="viewDetailedAnswers">查看详细</el-button>
              </div>
            </template>
            
            <div class="submitted-summary">
              <el-descriptions :column="3" border>
                <el-descriptions-item label="提交时间">
                  {{ formatDate(submissionInfo.submittedAt) }}
                </el-descriptions-item>
                <el-descriptions-item label="答题数">
                  {{ submissionInfo.answeredQuestions }}/{{ submissionInfo.totalQuestions }}
                </el-descriptions-item>
                <el-descriptions-item label="得分">
                  {{ submissionInfo.score !== null ? `${submissionInfo.score} 分` : '未评分' }}
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </el-card>
        </el-col>

        <!-- 右侧：操作面板 -->
        <el-col :span="8">
          <el-card class="action-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span>操作</span>
              </div>
            </template>
            
            <div class="action-buttons">
              <!-- 开始/继续答题按钮 -->
              <el-button
                v-if="!submissionInfo?.hasSubmitted && canTakeExam"
                type="primary"
                size="large"
                icon="Edit"
                @click="startExam"
                :loading="actionLoading"
                block
              >
                {{ hasStarted ? '继续答题' : '开始答题' }}
              </el-button>

              <!-- 查看答案按钮 -->
              <el-button
                v-if="submissionInfo?.hasSubmitted && canViewAnswers"
                type="success"
                size="large"
                icon="View"
                @click="viewDetailedAnswers"
                :loading="actionLoading"
                block
              >
                查看我的答案
              </el-button>

              <!-- 查看批改结果按钮 -->
              <el-button
                v-if="submissionInfo?.hasSubmitted && submissionInfo.score !== null"
                type="warning"
                size="large"
                icon="Document"
                @click="viewResult"
                :loading="actionLoading"
                block
              >
                查看批改结果
              </el-button>

              <!-- 考试状态说明 -->
              <div class="exam-status-info">
                <el-divider content-position="left">考试状态</el-divider>
                <div v-if="exam.status === 'DRAFT'">
                  <el-icon><InfoFilled /></el-icon>
                  <span>考试尚未发布</span>
                </div>
                <div v-else-if="exam.status === 'PUBLISHED'">
                  <el-icon color="#67c23a"><CircleCheckFilled /></el-icon>
                  <span>考试进行中，可以答题</span>
                </div>
                <div v-else-if="exam.status === 'ENDED'">
                  <el-icon color="#e6a23c"><Clock /></el-icon>
                  <span>考试已结束</span>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <div v-else class="error-state">
      <el-result
        icon="warning"
        title="考试不存在"
        sub-title="您访问的考试不存在或没有权限查看"
      >
        <template #extra>
          <el-button type="primary" @click="$router.push('/my-exams')">返回我的考试</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document, Edit, View, Clock, CircleCheckFilled, InfoFilled } from '@element-plus/icons-vue'
import { examApi } from '@/api/exam'
import { answerApi } from '@/api/answer'
import type { ExamResponse } from '@/types/api'

const route = useRoute()
const router = useRouter()

// 响应式数据
const loading = ref(true)
const actionLoading = ref(false)
const exam = ref<ExamResponse | null>(null)
const submissionInfo = ref<any>(null)
const hasStarted = ref(false)
const answeredCount = ref(0)

// 计算属性
const examId = computed(() => {
  const id = route.params.examId
  return typeof id === 'string' ? parseInt(id) : (Array.isArray(id) ? parseInt(id[0]) : id as number)
})

const canTakeExam = computed(() => {
  return exam.value?.status === 'PUBLISHED'
})

const canViewAnswers = computed(() => {
  // TODO: 这里需要根据教师的设置来判断是否允许查看答案
  return true
})

const progressPercentage = computed(() => {
  if (!exam.value?.questionCount) return 0
  return Math.round((answeredCount.value / exam.value.questionCount) * 100)
})

// 生命周期
onMounted(async () => {
  await loadExamData()
})

// 方法
const loadExamData = async () => {
  try {
    loading.value = true
    
    // 首先加载考试基本信息
    exam.value = await examApi.getExam(examId.value)
    
    // 加载提交状态
    submissionInfo.value = await checkSubmissionStatus()
    
  } catch (error) {
    console.error('Failed to load exam data:', error)
    ElMessage.error('加载考试数据失败')
  } finally {
    loading.value = false
  }
}

const checkSubmissionStatus = async () => {
  try {
    // 检查是否已提交
    const hasSubmitted = await answerApi.hasCurrentStudentSubmittedExam(examId.value)
    
    if (hasSubmitted) {
      // 如果已提交，获取提交详情
      try {
        const submissionDetail = await answerApi.getSubmissionDetail(examId.value)
        return {
          hasSubmitted: true,
          submittedAt: submissionDetail.submittedAt,
          answeredQuestions: submissionDetail.answeredQuestions,
          totalQuestions: submissionDetail.totalQuestions,
          score: submissionDetail.score
        }
      } catch (error) {
        return { hasSubmitted: true }
      }
    } else {
      // 如果未提交，检查答题进度
      await checkAnswerProgress()
      return { hasSubmitted: false }
    }
  } catch (error) {
    console.error('Failed to check submission status:', error)
    return { hasSubmitted: false }
  }
}

const checkAnswerProgress = async () => {
  try {
    // 获取学生在当前考试的答题情况
    const answers = await answerApi.getMyAnswersByExam(examId.value)
    hasStarted.value = answers.length > 0
    answeredCount.value = answers.filter(answer => 
      answer.answerText && answer.answerText.trim() !== ''
    ).length
  } catch (error) {
    console.error('Failed to check answer progress:', error)
    hasStarted.value = false
    answeredCount.value = 0
  }
}

const startExam = async () => {
  try {
    actionLoading.value = true
    await router.push(`/exams/${examId.value}/take`)
  } catch (error) {
    console.error('Failed to start exam:', error)
    ElMessage.error('进入考试失败')
  } finally {
    actionLoading.value = false
  }
}

const viewDetailedAnswers = async () => {
  try {
    actionLoading.value = true
    await router.push(`/my-exams/${examId.value}/result`)
  } catch (error) {
    console.error('Failed to view answers:', error)
    ElMessage.error('查看答案失败')
  } finally {
    actionLoading.value = false
  }
}

const viewResult = async () => {
  try {
    actionLoading.value = true
    await router.push(`/my-exams/${examId.value}/result`)
  } catch (error) {
    console.error('Failed to view result:', error)
    ElMessage.error('查看结果失败')
  } finally {
    actionLoading.value = false
  }
}

// 辅助方法
const formatDate = (dateString?: string) => {
  if (!dateString) return 'N/A'
  try {
    const date = new Date(dateString)
    if (isNaN(date.getTime()) || date.getFullYear() < 2000) {
      return '未设置'
    }
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  } catch (error) {
    return '时间格式错误'
  }
}

const getStatusDisplayText = (status?: string) => {
  const statusMap = {
    'DRAFT': '草稿',
    'PUBLISHED': '已发布',
    'IN_PROGRESS': '进行中',
    'ENDED': '已结束',
    'CANCELLED': '已取消'
  }
  return statusMap[status as keyof typeof statusMap] || status || '未知'
}

const getStatusTagType = (status?: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    'DRAFT': 'info',
    'PUBLISHED': 'primary',
    'IN_PROGRESS': 'warning',
    'ENDED': 'success',
    'CANCELLED': 'danger'
  }
  return typeMap[status as keyof typeof typeMap] || 'info'
}
</script>

<style scoped>
.student-exam-detail {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 8px 0 0 0;
  color: #303133;
  font-size: 24px;
  font-weight: 500;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.exam-info-card,
.my-status-card,
.submitted-answers-card {
  margin-bottom: 24px;
}

.answer-progress {
  margin-top: 16px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
}

.progress-percentage {
  font-weight: 500;
  color: #409eff;
}

.action-card,
.stats-card {
  margin-bottom: 24px;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.exam-status-info {
  margin-top: 16px;
}

.exam-status-info > div {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
}

.loading,
.error-state {
  padding: 40px 0;
}

.submitted-summary {
  margin-top: 16px;
}
</style>
