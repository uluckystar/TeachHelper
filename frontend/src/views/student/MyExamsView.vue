<template>
  <div class="my-exams">
    <div class="page-header">
      <div class="header-content">
        <h1>我的考试</h1>
        <p class="page-description">查看和管理你的考试记录</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="goToExamList">
          <el-icon><Edit /></el-icon>
          参加新考试
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="24" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="32" color="#409eff"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalExams || 0 }}</div>
              <div class="stat-label">参与考试</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="32" color="#67c23a"><CircleCheckFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.completedExams || 0 }}</div>
              <div class="stat-label">已完成</div>
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
              <div class="stat-value">{{ stats.pendingExams || 0 }}</div>
              <div class="stat-label">待完成</div>
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
              <div class="stat-value">{{ stats.averageScore || 0 }}</div>
              <div class="stat-label">平均分</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 考试列表 -->
    <el-card class="exams-card">
      <template #header>
        <div class="card-header">
          <span>考试列表</span>
          <el-select v-model="statusFilter" placeholder="筛选状态" style="width: 150px" @change="loadExams">
            <el-option label="全部" value="" />
            <el-option label="待完成" value="PENDING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已评估" value="EVALUATED" />
          </el-select>
        </div>
      </template>

      <div v-if="exams.length === 0" class="empty-state">
        <el-empty description="暂无考试记录">
          <el-button type="primary" @click="goToExamList">浏览可参加的考试</el-button>
        </el-empty>
      </div>
      
      <div v-else class="exams-list">
        <div 
          v-for="exam in exams" 
          :key="exam.id"
          class="exam-item"
          :class="{ 'exam-submitted': exam.hasSubmitted }"
        >
          <!-- 已提交标识 -->
          <div v-if="exam.hasSubmitted" class="submitted-badge">
            <el-icon size="20"><CircleCheckFilled /></el-icon>
            <span>已提交</span>
          </div>
          
          <div class="exam-header">
            <div class="exam-info">
              <h3 class="exam-title">{{ exam.title }}</h3>
              <p class="exam-description">{{ exam.description || '暂无描述' }}</p>
            </div>
            <div class="exam-status">
              <el-tag :type="getStatusTag(exam)">
                {{ getStatusText(exam) }}
              </el-tag>
            </div>
          </div>
          
          <div class="exam-details">
            <el-row :gutter="24">
              <el-col :span="8">
                <div class="detail-item">
                  <span class="label">题目数量:</span>
                  <span class="value">{{ exam.questionCount || 0 }}</span>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="detail-item">
                  <span class="label">考试时长:</span>
                  <span class="value">{{ exam.duration || '不限时' }}</span>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="detail-item">
                  <span class="label">总分:</span>
                  <span class="value">{{ exam.totalScore || 0 }}</span>
                </div>
              </el-col>
            </el-row>
          </div>

          <div class="exam-progress" v-if="exam.status === 'COMPLETED' || exam.status === 'EVALUATED'">
            <el-row :gutter="24">
              <el-col :span="8">
                <div class="progress-item">
                  <span class="label">得分:</span>
                  <span class="value score">{{ exam.myScore || '-' }}</span>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="progress-item">
                  <span class="label">完成时间:</span>
                  <span class="value">{{ formatDate(exam.completedAt || '') }}</span>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="progress-item">
                  <span class="label">用时:</span>
                  <span class="value">{{ exam.timeUsed || '-' }}</span>
                </div>
              </el-col>
            </el-row>
          </div>

          <div class="exam-actions">
            <el-button-group>
              <!-- 主要操作按钮：参加考试/查看结果 -->
              <el-button 
                v-if="canTakeExam(exam)"
                type="primary" 
                icon="Edit"
                @click="takeExam(exam.id.toString())"
              >
                {{ getExamActionText(exam) }}
              </el-button>
              
              <!-- 已提交状态按钮（禁用） -->
              <el-button 
                v-if="exam.hasSubmitted && !canViewResult(exam)"
                type="success" 
                icon="CircleCheckFilled"
                disabled
              >
                已提交
              </el-button>
              
              <!-- 查看结果按钮 -->
              <el-button 
                v-if="canViewResult(exam)"
                type="success"
                icon="View"
                @click="viewResults(exam.id.toString())"
              >
                查看成绩
              </el-button>
              
              <!-- 查看答案按钮：仅在教师允许时显示 -->
              <el-button 
                v-if="canViewAnswers(exam)"
                type="info"
                icon="Document"
                @click="viewMyAnswers(exam.id.toString())"
              >
                查看答卷
              </el-button>
              
              <!-- 考试信息按钮：改为更明确的名称 -->
              <el-button 
                icon="InfoFilled"
                @click="viewExamDetail(exam.id.toString())"
              >
                考试信息
              </el-button>
            </el-button-group>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  Document, 
  CircleCheckFilled, 
  Clock, 
  TrendCharts,
  Edit,
  View,
  InfoFilled
} from '@element-plus/icons-vue'
import { examApi } from '@/api/exam'
import { ElMessage } from 'element-plus'
import type { ExamResponse } from '@/types/api'

interface StudentExam extends Omit<ExamResponse, 'status'> {
  status?: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'EVALUATED' | 'DRAFT' | 'PUBLISHED' | 'ENDED' | string
  questionCount?: number
  duration?: number
  totalScore?: number
  myScore?: number
  completedAt?: string
  timeUsed?: string
  hasSubmitted?: boolean  // 新增：是否已提交
}

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const statusFilter = ref('')
const exams = ref<StudentExam[]>([])
const stats = ref({
  totalExams: 0,
  completedExams: 0,
  pendingExams: 0,
  averageScore: 0
})

const loadExams = async () => {
  loading.value = true
  try {
    // 使用学生专用的考试列表API
    const response = await examApi.getStudentExams()
    exams.value = response || []
    
    // 为每个考试检查提交状态
    const { studentAnswerApi } = await import('@/api/answer')
    const submissionChecks = exams.value.map(async (exam: StudentExam) => {
      try {
        exam.hasSubmitted = await studentAnswerApi.hasCurrentStudentSubmittedExam(exam.id)
      } catch (error) {
        console.error('检查考试提交状态失败:', error)
        exam.hasSubmitted = false
      }
    })
    await Promise.all(submissionChecks)
    
    // 如果有状态筛选，在前端过滤
    if (statusFilter.value) {
      exams.value = exams.value.filter((exam: any) => exam.status === statusFilter.value)
    }
    
    // 计算统计信息
    stats.value.totalExams = exams.value.length
    stats.value.completedExams = exams.value.filter((exam: any) => 
      ['COMPLETED', 'EVALUATED', 'ENDED'].includes(exam.status) || exam.hasSubmitted
    ).length
    stats.value.pendingExams = exams.value.filter((exam: any) => 
      exam.status === 'PUBLISHED' && !exam.hasSubmitted
    ).length
    
    const scores = exams.value
      .filter((exam: any) => exam.myScore !== null && exam.myScore !== undefined)
      .map((exam: any) => exam.myScore)
    stats.value.averageScore = scores.length > 0 
      ? Math.round(scores.reduce((sum: number, score: number) => sum + score, 0) / scores.length)
      : 0
      
  } catch (error) {
    console.error('加载考试列表失败:', error)
    ElMessage.error('加载考试列表失败')
  } finally {
    loading.value = false
  }
}

const goToExamList = () => {
  router.push('/exams')
}

const takeExam = (examId: string) => {
  router.push(`/exams/${examId}/take`)
}

const viewResults = (examId: string) => {
  router.push(`/my-exams/${examId}/result`)
}

const viewMyAnswers = (examId: string) => {
  router.push(`/my-exams/${examId}/answers`)
}

const viewExamDetail = (examId: string) => {
  // 使用学生专用的考试详情页面
  router.push(`/my-exams/${examId}`)
}

// 判断是否可以参加考试
const canTakeExam = (exam: StudentExam) => {
  // 如果已经提交过，不能再参加
  if (exam.hasSubmitted) return false
  
  return exam.status === 'PUBLISHED' || exam.status === 'IN_PROGRESS' || exam.status === 'PENDING'
}

// 判断是否可以查看结果
const canViewResult = (exam: StudentExam) => {
  return exam.status === 'COMPLETED' || exam.status === 'EVALUATED' || exam.status === 'ENDED'
}

// 判断是否可以查看答案
const canViewAnswers = (exam: StudentExam) => {
  // 只有已完成且教师允许查看答案时才显示，避免与查看成绩重复
  return (exam.status === 'COMPLETED' || exam.status === 'EVALUATED' || exam.status === 'ENDED') &&
         exam.myScore !== null && exam.myScore !== undefined
}

// 获取考试操作按钮文本
const getExamActionText = (exam: StudentExam) => {
  if (exam.hasSubmitted) return '已提交'
  if (exam.status === 'PENDING') return '开始考试'
  if (exam.status === 'IN_PROGRESS') return '继续考试'
  if (exam.status === 'PUBLISHED') return '参加考试'
  return '查看考试'
}

const getStatusTag = (exam: StudentExam): 'success' | 'info' | 'warning' | 'danger' | 'primary' => {
  if (exam.hasSubmitted) return 'success'
  
  const map: Record<string, 'success' | 'info' | 'warning' | 'danger' | 'primary'> = {
    'PENDING': 'info',
    'IN_PROGRESS': 'warning',
    'COMPLETED': 'primary',
    'EVALUATED': 'success',
    'DRAFT': 'info',
    'PUBLISHED': 'warning',
    'ENDED': 'danger'
  }
  return map[exam.status || ''] || 'info'
}

const getStatusText = (exam: StudentExam) => {
  if (exam.hasSubmitted) return '已提交'
  
  const map: Record<string, string> = {
    'PENDING': '待开始',
    'IN_PROGRESS': '进行中',
    'COMPLETED': '已完成',
    'EVALUATED': '已评估',
    'DRAFT': '草稿',
    'PUBLISHED': '可参加',
    'ENDED': '已结束'
  }
  return map[exam.status || ''] || exam.status || '未知'
}

const formatDate = (dateString: string) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

onMounted(() => {
  // 检查是否有来自路由守卫的提示信息
  if (route.query.message === 'already_submitted') {
    ElMessage.warning('你已经提交过该考试，不能重复参加')
  }
  
  loadExams()
})
</script>

<style scoped>
.my-exams {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-content h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.page-description {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.header-actions {
  flex-shrink: 0;
}

.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-content {
  display: flex;
  align-items: center;
  padding: 16px;
}

.stat-icon {
  margin-right: 16px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.exams-card {
  background: white;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
}

.exams-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.exam-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  transition: all 0.3s;
  position: relative;
}

.exam-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

/* 已提交的考试样式 */
.exam-item.exam-submitted {
  background-color: #f0f9ff;
  border-color: #67c23a;
}

.exam-item.exam-submitted:hover {
  border-color: #67c23a;
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.1);
}

/* 已提交标识 */
.submitted-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background: #67c23a;
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  z-index: 10;
}

.exam-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.exam-info {
  flex: 1;
}

.exam-title {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
}

.exam-description {
  margin: 0;
  color: #666;
  font-size: 14px;
  line-height: 1.4;
}

.exam-details,
.exam-progress {
  margin-bottom: 16px;
}

.detail-item,
.progress-item {
  display: flex;
  align-items: center;
}

.label {
  color: #666;
  font-size: 14px;
  margin-right: 8px;
}

.value {
  font-weight: 500;
}

.value.score {
  color: #e6a23c;
  font-size: 16px;
  font-weight: 600;
}

.exam-actions {
  display: flex;
  justify-content: flex-end;
}
</style>
