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
        <!-- 学生统计卡片 -->
        <div class="stats-section mb-4">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-card class="stat-card">
                <el-statistic
                  title="可参加的考试"
                  :value="availableExams.length"
                  class="stat-content"
                >
                  <template #suffix>
                    <el-icon class="stat-icon available">
                      <Document />
                    </el-icon>
                  </template>
                </el-statistic>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card class="stat-card">
                <el-statistic
                  title="我的所有考试"
                  :value="allMyExams.length"
                  class="stat-content"
                >
                  <template #suffix>
                    <el-icon class="stat-icon total">
                      <Folder />
                    </el-icon>
                  </template>
                </el-statistic>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card class="stat-card">
                <el-statistic
                  title="已完成考试"
                  :value="completedExamsCount"
                  class="stat-content"
                >
                  <template #suffix>
                    <el-icon class="stat-icon completed">
                      <Check />
                    </el-icon>
                  </template>
                </el-statistic>
              </el-card>
            </el-col>
          </el-row>
        </div>

        <!-- 考试区域 -->
        <el-row :gutter="20">
          <!-- 可参加的考试 -->
          <el-col :span="12">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>📝 可参加的考试</span>
                  <el-button 
                    type="primary" 
                    size="small" 
                    @click="$router.push('/exams')"
                  >
                    查看全部
                  </el-button>
                </div>
              </template>
              <div v-if="availableExams.length === 0" class="empty-state">
                <el-empty 
                  description="暂无可参加的考试" 
                  :image-size="80"
                />
              </div>
              <div v-else class="exam-list">
                <div
                  v-for="exam in availableExams.slice(0, 3)"
                  :key="exam.id"
                  class="exam-item available-exam"
                >
                  <div class="exam-info">
                    <h4>{{ exam.title }}</h4>
                    <p class="exam-desc">{{ truncateText(exam.description, 60) }}</p>
                    <div class="exam-meta">
                      <el-tag type="success" size="small">可参加</el-tag>
                      <span class="exam-time">
                        {{ formatDate(exam.createdAt) }}
                      </span>
                    </div>
                  </div>
                  <div class="exam-actions">
                    <el-button
                      type="primary"
                      size="small"
                      @click="takeExam(exam.id)"
                    >
                      开始考试
                    </el-button>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>

          <!-- 我的所有考试 -->
          <el-col :span="12">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>📚 我的所有考试</span>
                  <el-button 
                    type="default" 
                    size="small" 
                    @click="$router.push('/my-exams')"
                  >
                    查看全部
                  </el-button>
                </div>
              </template>
              <div v-if="allMyExams.length === 0" class="empty-state">
                <el-empty 
                  description="暂无考试记录" 
                  :image-size="80"
                />
              </div>
              <div v-else class="exam-list">
                <div
                  v-for="exam in allMyExams.slice(0, 3)"
                  :key="exam.id"
                  class="exam-item my-exam"
                  :class="{ 'submitted': (exam as any).hasSubmitted }"
                  @click="goToMyExam(exam)"
                >
                  <div class="exam-info">
                    <h4>{{ exam.title }}</h4>
                    <p class="exam-desc">{{ truncateText(exam.description, 60) }}</p>
                    <div class="exam-meta">
                      <el-tag :type="getExamStatusTagType(exam)" size="small">
                        {{ getExamStatusText(exam) }}
                      </el-tag>
                      <span class="exam-time">
                        {{ formatDate(exam.createdAt) }}
                      </span>
                    </div>
                  </div>
                  <div class="exam-actions">
                    <el-button
                      :type="getExamActionButtonType(exam)"
                      size="small"
                      :disabled="(exam as any).hasSubmitted"
                      @click.stop="handleExamAction(exam)"
                    >
                      {{ getExamActionText(exam) }}
                    </el-button>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Document, Folder, Check } from '@element-plus/icons-vue'
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
const allMyExams = ref<Exam[]>([])

// 计算属性：已完成考试数量
const completedExamsCount = computed(() => {
  // 考虑已提交状态和考试状态
  return allMyExams.value.filter((exam: any) => {
    return (exam as any).hasSubmitted || 
           exam.status === 'EVALUATED' || 
           exam.status === 'ENDED'
  }).length
})

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
      
      // 加载总统计
      const totalExamsResponse = await examApi.getAllExams(0, 1)
      stats.value.totalExamsCount = totalExamsResponse.length
    } else {
      // 加载学生数据
      console.log('Loading student dashboard data...')
      
      // 获取可参加的考试（仅已发布状态）
      try {
        const availableResponse = await examApi.getAvailableExams()
        
        // 为每个可参加的考试检查提交状态，过滤掉已提交的
        const { studentAnswerApi } = await import('@/api/answer')
        const availableWithSubmissionStatus = await Promise.all(
          availableResponse.map(async (exam: any) => {
            try {
              const hasSubmitted = await studentAnswerApi.hasCurrentStudentSubmittedExam(exam.id)
              return { ...exam, hasSubmitted }
            } catch (error) {
              console.error('检查考试提交状态失败:', error)
              return { ...exam, hasSubmitted: false }
            }
          })
        )
        
        // 只保留未提交的考试
        availableExams.value = availableWithSubmissionStatus.filter((exam: any) => !exam.hasSubmitted)
        console.log('Available exams loaded (filtered):', availableExams.value.length)
      } catch (error) {
        console.error('Failed to load available exams:', error)
        availableExams.value = []
      }
      
      // 获取所有相关考试（包括已结束、已评阅等）
      try {
        const allMyExamsResponse = await examApi.getStudentExams()
        allMyExams.value = allMyExamsResponse
        console.log('All my exams loaded:', allMyExamsResponse.length)
        
        // 为每个考试检查提交状态
        const { studentAnswerApi } = await import('@/api/answer')
        const submissionChecks = allMyExams.value.map(async (exam: any) => {
          try {
            ;(exam as any).hasSubmitted = await studentAnswerApi.hasCurrentStudentSubmittedExam(exam.id)
            console.log(`Exam ${exam.id} submission status:`, (exam as any).hasSubmitted)
          } catch (error) {
            console.error('检查考试提交状态失败:', error)
            ;(exam as any).hasSubmitted = false
          }
        })
        await Promise.all(submissionChecks)
        
        // 更新统计
        stats.value.totalExamsCount = allMyExamsResponse.length
        stats.value.participatedExamsCount = allMyExamsResponse.filter((exam: any) => 
          exam.status === 'EVALUATED' || exam.status === 'ENDED' || exam.hasSubmitted
        ).length
      } catch (error) {
        console.error('Failed to load student exams:', error)
        allMyExams.value = []
      }
    }
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

const getExamStatusText = (exam: any) => {
  // 首先检查是否已提交
  if ((exam as any).hasSubmitted) {
    return '已提交'
  }
  
  const status = exam.status || 'DRAFT'
  switch (status) {
    case 'DRAFT': return '草稿'
    case 'PUBLISHED': return '可参加'
    case 'IN_PROGRESS': return '进行中'
    case 'ENDED': return '待评估'
    case 'EVALUATED': return '评估完成'
    default: return status
  }
}

// 学生端专用方法
const getExamStatusTagType = (exam: any) => {
  // 首先检查是否已提交
  if ((exam as any).hasSubmitted) {
    return 'success'
  }
  
  const status = exam.status || 'DRAFT'
  switch (status) {
    case 'PUBLISHED': return 'warning'
    case 'IN_PROGRESS': return 'warning'
    case 'ENDED': return 'info'
    case 'EVALUATED': return 'success'
    case 'DRAFT': return 'info'
    default: return 'info'
  }
}

const getExamActionButtonType = (exam: any) => {
  // 首先检查是否已提交
  if ((exam as any).hasSubmitted) {
    return 'success'
  }
  
  const status = exam.status || 'DRAFT'
  switch (status) {
    case 'PUBLISHED': return 'primary'
    case 'EVALUATED': return 'success'
    case 'ENDED': return 'info'
    default: return 'default'
  }
}

const getExamActionText = (exam: any) => {
  // 首先检查是否已提交
  if ((exam as any).hasSubmitted) {
    return '已提交'
  }
  
  const status = exam.status || 'DRAFT'
  switch (status) {
    case 'PUBLISHED': return '开始考试'
    case 'EVALUATED': return '查看成绩'
    case 'ENDED': return '查看答卷'
    default: return '查看详情'
  }
}

const handleExamAction = (exam: any) => {
  // 首先检查是否已提交
  if ((exam as any).hasSubmitted) {
    // 已提交的考试，禁用操作或显示结果
    ElMessage.info('该考试已提交，无法重复参加')
    return
  }
  
  const status = exam.status || 'DRAFT'
  switch (status) {
    case 'PUBLISHED':
      takeExam(exam.id)
      break
    case 'EVALUATED':
      router.push(`/my-exams/${exam.id}/result`)
      break
    case 'ENDED':
      router.push(`/my-exams/${exam.id}/answers`)
      break
    default:
      router.push(`/my-exams/${exam.id}`)
  }
}

const goToMyExam = (exam: any) => {
  // 根据考试状态跳转到相应页面
  handleExamAction(exam)
}

const truncateText = (text: string, maxLength: number) => {
  if (!text) return ''
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
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

/* 学生端统计卡片样式 */
.stats-section {
  margin-bottom: 24px;
}

.stat-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 12px;
  transition: transform 0.3s, box-shadow 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.3);
}

.stat-card :deep(.el-card__body) {
  padding: 20px;
}

.stat-content {
  color: white;
}

.stat-content :deep(.el-statistic__head) {
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  margin-bottom: 8px;
}

.stat-content :deep(.el-statistic__content) {
  color: white;
  font-size: 28px;
  font-weight: bold;
}

.stat-icon {
  margin-left: 8px;
  opacity: 0.8;
}

.stat-icon.available {
  color: #67c23a;
}

.stat-icon.total {
  color: #409eff;
}

.stat-icon.completed {
  color: #f56c6c;
}

/* 卡片头部样式 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
  color: #303133;
}

/* 考试列表样式 */
.exam-list {
  max-height: 400px;
  overflow-y: auto;
}

.exam-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 12px;
  transition: all 0.3s;
}

.exam-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.exam-item.available-exam:hover {
  border-color: #67c23a;
  box-shadow: 0 2px 12px rgba(103, 194, 58, 0.2);
}

.exam-item.my-exam {
  cursor: pointer;
}

.exam-item.my-exam:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.2);
}

/* 已提交考试样式 */
.exam-item.submitted {
  background-color: #f0f9f0;
  border-color: #b3e5b3;
  position: relative;
}

.exam-item.submitted:hover {
  border-color: #67c23a;
  box-shadow: 0 2px 12px rgba(103, 194, 58, 0.15);
}

.exam-item.submitted .exam-info h4 {
  color: #67c23a;
}

.exam-item.submitted::before {
  content: '✓';
  position: absolute;
  top: 8px;
  right: 8px;
  width: 20px;
  height: 20px;
  background-color: #67c23a;
  color: white;
  border-radius: 50%;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

.exam-info {
  flex: 1;
  min-width: 0;
}

.exam-info h4 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 500;
}

.exam-desc {
  margin: 0 0 12px 0;
  color: #606266;
  font-size: 14px;
  word-break: break-word;
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

.exam-actions {
  margin-left: 16px;
  flex-shrink: 0;
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
  text-align: center;
}

/* 开发工具样式 */
.dev-tools-section {
  margin-bottom: 24px;
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

.mb-4 {
  margin-bottom: 24px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .welcome-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .welcome-stats {
    width: 100%;
    justify-content: space-around;
  }
  
  .exam-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .exam-actions {
    margin-left: 0;
    width: 100%;
  }
  
  .exam-actions .el-button {
    width: 100%;
  }
}
</style>
