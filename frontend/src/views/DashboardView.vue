<template>
  <div class="dashboard">
    <div class="welcome-section">
      <el-card>
        <div class="welcome-content">
          <div class="welcome-info">
            <h1>欢迎回来，{{ authStore.user?.username }}！</h1>
            <p class="welcome-desc">
              您的角色是：
              <el-tag :type="getRoleTagType(authStore.primaryRole)">
                {{ getRoleText(authStore.primaryRole) }}
              </el-tag>
            </p>
          </div>
          <div class="welcome-stats">
            <el-statistic
              v-if="authStore.isTeacher || authStore.isAdmin"
              title="我创建的考试"
              :value="stats.myExamsCount"
              class="stat-item"
            />
            <el-statistic
              v-if="authStore.isStudent"
              title="已参加考试"
              :value="stats.participatedExamsCount"
              class="stat-item"
            />
            <el-statistic
              title="总考试数"
              :value="stats.totalExamsCount"
              class="stat-item"
            />
          </div>
        </div>
      </el-card>
    </div>

    <div class="dashboard-content">
      <!-- 管理员开发工具 -->
      <div v-if="authStore.isAdmin && isDevelopmentMode" class="dev-tools-section mb-4">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>🛠️ 开发工具</span>
              <el-tag type="warning" size="small">仅开发环境</el-tag>
            </div>
          </template>
          <div class="dev-tools-content">
            <p>快速生成示例数据，用于开发和测试：</p>
            <el-button 
              type="primary" 
              :loading="generatingData"
              @click="generateSampleData"
              :disabled="generatingData"
            >
              <el-icon v-if="!generatingData"><Plus /></el-icon>
              {{ generatingData ? '正在生成...' : '生成示例数据' }}
            </el-button>
            <el-text class="ml-2" type="info">
              将生成用户、考试、题目和答案等测试数据
            </el-text>
          </div>
        </el-card>
      </div>

      <!-- 教师/管理员视图 -->
      <div v-if="authStore.isTeacher || authStore.isAdmin" class="teacher-dashboard">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card header="最近创建的考试">
              <div v-if="recentExams.length === 0" class="empty-state">
                <el-empty description="暂无考试数据" />
              </div>
              <div v-else>
                <div
                  v-for="exam in recentExams"
                  :key="exam.id"
                  class="exam-item"
                  @click="goToExam(exam.id)"
                >
                  <div class="exam-info">
                    <h4>{{ exam.title }}</h4>
                    <p class="exam-desc">{{ exam.description }}</p>
                    <div class="exam-meta">
                      <!-- 状态标签暂时隐藏，等后端实现status字段后显示 -->
                      <!-- <el-tag :type="getExamStatusType(exam.status)">
                        {{ getExamStatusText(exam.status) }}
                      </el-tag> -->
                      <span class="exam-time">
                        {{ formatDate(exam.createdAt) }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
          
          <el-col :span="12">
            <el-card header="快速操作">
              <div class="action-buttons">
                <el-button
                  type="primary"
                  size="large"
                  @click="$router.push('/exams/create')"
                >
                  <el-icon><Plus /></el-icon>
                  创建新考试
                </el-button>
                
                <el-button
                  size="large"
                  @click="$router.push('/exams')"
                >
                  <el-icon><Document /></el-icon>
                  管理考试
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 学生视图 -->
      <div v-else class="student-dashboard">
        <el-card header="可参加的考试">
          <div v-if="availableExams.length === 0" class="empty-state">
            <el-empty description="暂无可参加的考试" />
          </div>
          <div v-else>
            <div
              v-for="exam in availableExams"
              :key="exam.id"
              class="exam-item"
            >
              <div class="exam-info">
                <h4>{{ exam.title }}</h4>
                <p class="exam-desc">{{ exam.description }}</p>
                <div class="exam-meta">
                  <span class="exam-time">
                    创建时间：{{ formatDate(exam.createdAt) }}
                  </span>
                  <span class="exam-duration">
                    <!-- 考试时长字段暂时隐藏，等后端实现后显示 -->
                    <!-- 考试时长：{{ exam.duration }} 分钟 -->
                  </span>
                </div>
              </div>
              <div class="exam-actions">
                <el-button
                  v-if="canTakeExam(exam)"
                  type="primary"
                  @click="takeExam(exam.id)"
                >
                  开始考试
                </el-button>
                <el-button
                  v-else
                  disabled
                >
                  {{ getExamButtonText(exam) }}
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Document } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { examApi } from '@/api/exam'
import { devApi } from '@/api/dev'
import type { Exam } from '@/types/api'

const router = useRouter()
const authStore = useAuthStore()

// 开发工具相关状态
const generatingData = ref(false)
const isDevelopmentMode = computed(() => {
  return import.meta.env.DEV
})

const stats = ref({
  myExamsCount: 0,
  participatedExamsCount: 0,
  totalExamsCount: 0
})

const recentExams = ref<Exam[]>([])
const availableExams = ref<Exam[]>([])

onMounted(async () => {
  await loadDashboardData()
})

const loadDashboardData = async () => {
  try {
    if (authStore.isTeacher || authStore.isAdmin) {
      // 加载教师数据 - 使用 getAllExams 替代 getMyExams
      const myExamsResponse = await examApi.getAllExams(0, 5)
      recentExams.value = myExamsResponse
      stats.value.myExamsCount = myExamsResponse.length
    } else {
      // 加载学生数据 - 使用 getAllExams 替代 getExams
      const examsResponse = await examApi.getAllExams(0, 10)
      // 暂时显示所有考试，因为后端还没有status字段
      availableExams.value = examsResponse
    }
    
    // 加载总统计 - 使用 getAllExams 替代 getExams
    const totalExamsResponse = await examApi.getAllExams(0, 1)
    stats.value.totalExamsCount = totalExamsResponse.length
  } catch (error) {
    console.error('Failed to load dashboard data:', error)
  }
}

// 生成示例数据
const generateSampleData = async () => {
  try {
    generatingData.value = true
    await devApi.generateSampleData()
    ElMessage.success('示例数据生成成功！页面将刷新以显示新数据。')
    // 刷新页面数据
    await loadDashboardData()
    // 可选：刷新整个页面来确保所有数据更新
    setTimeout(() => {
      window.location.reload()
    }, 1000)
  } catch (error: any) {
    console.error('Failed to generate sample data:', error)
    ElMessage.error(error.response?.data?.message || '生成示例数据失败，请稍后重试')
  } finally {
    generatingData.value = false
  }
}

const getRoleTagType = (role?: string | null) => {
  switch (role) {
    case 'ADMIN': return 'danger'
    case 'TEACHER': return 'warning'
    case 'STUDENT': return 'success'
    default: return 'info'
  }
}

const getRoleText = (role?: string | null) => {
  switch (role) {
    case 'ADMIN': return '管理员'
    case 'TEACHER': return '教师'
    case 'STUDENT': return '学生'
    default: return '未知'
  }
}

const getExamStatusType = (status: string) => {
  switch (status) {
    case 'PUBLISHED': return 'success'
    case 'DRAFT': return 'info'
    case 'ENDED': return 'danger'
    default: return 'info'
  }
}

const getExamStatusText = (status: string) => {
  switch (status) {
    case 'DRAFT': return '草稿'
    case 'PUBLISHED': return '已发布'
    case 'IN_PROGRESS': return '进行中'
    case 'ENDED': return '待评估'
    case 'EVALUATED': return '评估完成'
    default: return status
  }
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN')
}

const goToExam = (examId: number) => {
  router.push(`/exams/${examId}`)
}

const canTakeExam = (exam: Exam) => {
  // 暂时返回 true，因为后端还没有实现 status、startTime、endTime 字段
  // 后续当后端添加这些字段后，可以恢复原有的逻辑：
  // const now = new Date()
  // const startTime = new Date(exam.startTime)
  // const endTime = new Date(exam.endTime)
  // return exam.status === 'PUBLISHED' && now >= startTime && now <= endTime
  return true
}

const getExamButtonText = (exam: Exam) => {
  // 暂时返回默认文本，因为后端还没有实现相关字段
  // 后续当后端添加这些字段后，可以恢复原有的逻辑：
  // const now = new Date()
  // const startTime = new Date(exam.startTime)
  // const endTime = new Date(exam.endTime)
  // if (exam.status !== 'PUBLISHED') return '未发布'
  // if (now < startTime) return '未开始'
  // if (now > endTime) return '已结束'
  return '开始考试'
}

const takeExam = (examId: number) => {
  router.push(`/exams/${examId}/take`)
}
</script>

<style scoped>
.dashboard {
  max-width: 1200px;
  margin: 0 auto;
}

.welcome-section {
  margin-bottom: 24px;
}

.welcome-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome-info h1 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 28px;
  font-weight: 500;
}

.welcome-desc {
  color: #606266;
  font-size: 16px;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.welcome-stats {
  display: flex;
  gap: 32px;
}

.stat-item {
  text-align: center;
}

.exam-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.exam-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.exam-info h4 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 16px;
}

.exam-desc {
  margin: 0 0 12px 0;
  color: #606266;
  font-size: 14px;
}

.exam-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.exam-time, .exam-duration {
  display: flex;
  align-items: center;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.action-buttons .el-button {
  width: 100%;
  height: 48px;
}

.empty-state {
  padding: 32px 0;
}

/* 开发工具样式 */
.dev-tools-section {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dev-tools-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dev-tools-content p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.dev-tools-content .el-button {
  align-self: flex-start;
}

.ml-2 {
  margin-left: 8px;
}
</style>
