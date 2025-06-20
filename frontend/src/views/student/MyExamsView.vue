<template>
  <div class="my-exams">
    <div class="page-header">
      <h1>我的考试</h1>
      <p class="page-description">查看和管理你的考试记录</p>
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
        >
          <div class="exam-header">
            <div class="exam-info">
              <h3 class="exam-title">{{ exam.title }}</h3>
              <p class="exam-description">{{ exam.description || '暂无描述' }}</p>
            </div>
            <div class="exam-status">
              <el-tag :type="getStatusTag(exam.status || '') as any">
                {{ getStatusText(exam.status || '') }}
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
              <el-button 
                v-if="exam.status === 'PENDING'"
                type="primary" 
                @click="startExam(exam.id.toString())"
              >
                开始考试
              </el-button>
              <el-button 
                v-if="exam.status === 'IN_PROGRESS'"
                type="warning" 
                @click="continueExam(exam.id.toString())"
              >
                继续考试
              </el-button>
              <el-button 
                v-if="exam.status === 'COMPLETED' || exam.status === 'EVALUATED'"
                @click="viewResults(exam.id.toString())"
              >
                查看结果
              </el-button>
              <el-button @click="viewExamDetail(exam.id.toString())">
                考试详情
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
import { useRouter } from 'vue-router'
import { 
  Document, 
  CircleCheckFilled, 
  Clock, 
  TrendCharts 
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
}

const router = useRouter()

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
    const params = statusFilter.value ? { status: statusFilter.value } : {}
    const response = await examApi.getMyExams(params)
    exams.value = response.data || []
    
    // 计算统计信息
    stats.value.totalExams = exams.value.length
    stats.value.completedExams = exams.value.filter((exam: any) => 
      ['COMPLETED', 'EVALUATED'].includes(exam.status)
    ).length
    stats.value.pendingExams = exams.value.filter((exam: any) => 
      exam.status === 'PENDING'
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

const startExam = (examId: string) => {
  router.push(`/exams/${examId}/take`)
}

const continueExam = (examId: string) => {
  router.push(`/exams/${examId}/take`)
}

const viewResults = (examId: string) => {
  router.push(`/exams/${examId}/results`)
}

const viewExamDetail = (examId: string) => {
  router.push(`/exams/${examId}`)
}

const getStatusTag = (status: string) => {
  const map: Record<string, string> = {
    'PENDING': 'info',
    'IN_PROGRESS': 'warning',
    'COMPLETED': 'primary',
    'EVALUATED': 'success',
    'DRAFT': 'info',
    'PUBLISHED': 'success',
    'ENDED': 'warning'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'PENDING': '待开始',
    'IN_PROGRESS': '进行中',
    'COMPLETED': '已完成',
    'EVALUATED': '已评估',
    'DRAFT': '草稿',
    'PUBLISHED': '可参加',
    'ENDED': '已结束'
  }
  return map[status] || status
}

const formatDate = (dateString: string) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

onMounted(() => {
  loadExams()
})
</script>

<style scoped>
.my-exams {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
}

.page-description {
  margin: 0;
  color: #666;
  font-size: 14px;
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
}

.exam-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
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
