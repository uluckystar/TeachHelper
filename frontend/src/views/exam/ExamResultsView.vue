<template>
  <div class="exam-results">
    <div class="page-header">
      <el-breadcrumb>
        <el-breadcrumb-item :to="{ path: '/exams' }">考试列表</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: `/exams/${examId}` }">{{ exam?.title }}</el-breadcrumb-item>
        <el-breadcrumb-item>考试结果统计</el-breadcrumb-item>
      </el-breadcrumb>
      <h1>
        考试结果统计
        <el-tag v-if="isAdmin" type="danger" size="small" style="margin-left: 10px;">
          管理员视图
        </el-tag>
        <el-tag v-else-if="isTeacher" type="primary" size="small" style="margin-left: 10px;">
          教师视图
        </el-tag>
      </h1>
      <p class="page-description">
        查看整体考试数据、成绩分布和批阅进度
        <span v-if="isAdmin" class="admin-note">（管理员可查看所有班级和考试数据）</span>
      </p>
    </div>

    <!-- 调试信息 (开发环境显示) -->
    <el-card v-if="isDev" class="debug-card" style="background: #f5f5f5; margin-bottom: 20px;">
      <template #header>
        <span style="color: #666;">调试信息 (仅开发环境显示)</span>
      </template>
      <div style="font-size: 12px; color: #666;">
        <p>用户角色: {{ authStore.user?.roles?.join(', ') }}</p>
        <p>isAdmin: {{ isAdmin }}, isTeacher: {{ isTeacher }}</p>
        <p>examId: {{ examId }}</p>
        <p>statistics 数据: {{ statistics ? '已加载' : '未加载' }}</p>
        <p>studentResults 长度: {{ studentResults.length }}</p>
        <p>filteredResults 长度: {{ filteredResults.length }}</p>
      </div>
    </el-card>

    <!-- 考试统计概览 -->
    <el-card v-if="statistics" class="statistics-card">
      <template #header>
        <div class="card-header">
          <span>考试统计概览</span>
          <div class="header-actions">
            <el-button-group>
              <el-button type="primary" icon="Download" @click="exportResults">
                导出成绩
              </el-button>
              <el-button v-if="isAdmin" type="success" icon="Download" @click="exportDetailedResults">
                详细导出
              </el-button>
            </el-button-group>
          </div>
        </div>
      </template>
      
      <el-row :gutter="20">
        <el-col :span="6">
          <el-statistic title="参考学生数" :value="statistics.totalStudents" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="已提交数" :value="statistics.studentsSubmitted" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="已批阅数" :value="statistics.studentsEvaluated" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="平均分" :value="statistics.averageScore || 0" suffix="分" />
        </el-col>
      </el-row>
    </el-card>

    <!-- 批阅进度 -->
    <el-card class="progress-card">
      <template #header>
        <span>批阅进度</span>
      </template>
      <el-progress 
        v-if="statistics"
        :percentage="evaluationProgress" 
        :color="progressColor"
        :stroke-width="20"
        text-inside
      />
    </el-card>

    <!-- 学生成绩列表 -->
    <el-card class="results-card">
      <template #header>
        <div class="card-header">
          <span>学生成绩详情</span>
          <div class="header-actions">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索学生姓名或ID"
              style="width: 200px"
              prefix-icon="Search"
              @input="handleSearch"
            />
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="filteredResults" stripe>
        <el-table-column prop="studentName" label="学生姓名" width="120" />
        <el-table-column prop="studentId" label="学号" width="120" />
        <el-table-column v-if="isAdmin" prop="userId" label="用户ID" width="80" />
        <el-table-column label="分数" width="120">
          <template #default="{ row }">
            <span v-if="row.obtainedScore !== null" :class="getScoreClass(row.obtainedScore)">
              {{ row.obtainedScore?.toFixed(2) }} / {{ row.totalScore }}
            </span>
            <el-tag v-else type="warning" size="small">未批阅</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'EVALUATED' ? 'success' : 'warning'" size="small">
              {{ row.status === 'EVALUATED' ? '已完成' : '批阅中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.submitTime) }}
          </template>
        </el-table-column>
        <el-table-column v-if="isAdmin" prop="attemptId" label="尝试ID" width="80" />
        <el-table-column label="操作" :width="isAdmin ? 220 : 180">
          <template #default="{ row }">
            <el-button-group size="small">
              <el-button 
                type="primary" 
                icon="View"
                @click="viewStudentDetail(row)"
              >
                查看详情
              </el-button>
              <el-button 
                v-if="isAdmin" 
                type="warning" 
                icon="Edit"
                @click="adminEditStudent(row)"
              >
                管理
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无学生成绩数据">
            <template #description>
              <span>{{ studentResults.length === 0 ? '该考试暂无学生提交答卷' : '搜索无结果' }}</span>
            </template>
          </el-empty>
        </template>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { examResultApi } from '@/api/examResult'
import { examApi } from '@/api/exam'
import { useAuthStore } from '@/stores/auth'
import type { ExamStatistics, ExamStudentResultSummary, ExamResponse } from '@/types/api'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 路由参数
const examId = computed(() => parseInt(route.params.examId as string))

// 用户角色检查
const isAdmin = computed(() => authStore.user?.roles?.includes('ADMIN') || false)
const isTeacher = computed(() => authStore.user?.roles?.includes('TEACHER') || false)

// 开发环境检查
const isDev = computed(() => import.meta.env.DEV)

// 响应式数据
const loading = ref(false)
const exam = ref<ExamResponse>()
const statistics = ref<ExamStatistics>()
const studentResults = ref<ExamStudentResultSummary[]>([])
const searchKeyword = ref('')

// 计算属性
const evaluationProgress = computed(() => {
  if (!statistics.value || statistics.value.totalStudents === 0) return 0
  return Math.round((statistics.value.studentsEvaluated / statistics.value.totalStudents) * 100)
})

const progressColor = computed(() => {
  const progress = evaluationProgress.value
  if (progress === 100) return '#67c23a'
  if (progress >= 60) return '#e6a23c'
  return '#f56c6c'
})

const filteredResults = computed(() => {
  if (!searchKeyword.value) return studentResults.value
  const keyword = searchKeyword.value.toLowerCase()
  return studentResults.value.filter((result: ExamStudentResultSummary) => 
    result.studentName.toLowerCase().includes(keyword) ||
    result.studentId.toString().includes(keyword)
  )
})

// 方法
const formatDateTime = (datetime: string | undefined) => {
  if (!datetime) return '未提交'
  return new Date(datetime).toLocaleString('zh-CN')
}

const getScoreClass = (score: number) => {
  if (score >= 90) return 'score-excellent'
  if (score >= 80) return 'score-good'
  if (score >= 60) return 'score-pass'
  return 'score-fail'
}

const loadExam = async () => {
  try {
    exam.value = await examApi.getExam(examId.value)
  } catch (error) {
    console.error('加载考试信息失败:', error)
    ElMessage.error('加载考试信息失败')
  }
}

const loadStatistics = async () => {
  try {
    console.log('加载统计数据 - examId:', examId.value, 'isAdmin:', isAdmin.value)
    let response
    if (isAdmin.value) {
      // 管理员使用管理员专用API
      console.log('调用管理员统计API')
      response = await examResultApi.getExamStatisticsForAdmin(examId.value)
    } else {
      // 教师使用教师专用API
      console.log('调用教师统计API')
      response = await examResultApi.getExamStatisticsForTeacher(examId.value)
    }
    console.log('统计数据响应:', response.data)
    statistics.value = response.data
  } catch (error) {
    console.error('加载统计数据失败:', error)
    ElMessage.error('加载统计数据失败')
  }
}

const loadStudentResults = async () => {
  loading.value = true
  try {
    console.log('加载学生成绩 - examId:', examId.value, 'isAdmin:', isAdmin.value)
    let response
    if (isAdmin.value) {
      // 管理员使用管理员专用API
      console.log('调用管理员学生成绩API')
      response = await examResultApi.getAllStudentResultsForAdmin(examId.value)
      console.log('管理员API响应:', response.data)
      // 管理员API返回 List<ExamResultResponse>
      studentResults.value = mapExamResultToSummary(response.data || [])
    } else {
      // 教师使用教师专用API
      console.log('调用教师学生成绩API')
      response = await examResultApi.getExamResultsOverviewForTeacher(examId.value)
      console.log('教师API响应:', response.data)
      // 教师API也返回 List<ExamResultResponse>
      studentResults.value = mapExamResultToSummary(response.data || [])
    }
    console.log('最终studentResults:', studentResults.value)
  } catch (error) {
    console.error('加载学生成绩失败:', error)
    ElMessage.error('加载学生成绩失败')
  } finally {
    loading.value = false
  }
}

// 新增：将ExamResultResponse映射为ExamStudentResultSummary格式
const mapExamResultToSummary = (examResults: any[]): ExamStudentResultSummary[] => {
  console.log('原始数据:', examResults)
  const mapped = examResults.map(result => {
    console.log('映射单个结果:', result)
    return {
      studentId: result.studentId,
      studentName: result.studentName,
      userId: result.studentId, // 用studentId作为userId
      obtainedScore: result.totalScore,
      totalScore: result.totalPossibleScore || 100,
      submitTime: result.submitTime,
      status: result.status || 'SUBMITTED',
      attemptId: result.examId // 用examId作为attemptId
    }
  })
  console.log('映射后数据:', mapped)
  return mapped
}

const handleSearch = () => {
  // 搜索逻辑已在计算属性中处理
}

const viewStudentDetail = (student: ExamStudentResultSummary) => {
  router.push(`/exams/${examId.value}/students/${student.studentId}/paper`)
}

const adminEditStudent = (student: ExamStudentResultSummary) => {
  // TODO: 实现管理员学生管理功能
  ElMessage.info(`管理学生 ${student.studentName} 的功能开发中...`)
}

const exportResults = () => {
  // TODO: 实现导出功能
  ElMessage.info('导出功能开发中...')
}

const exportDetailedResults = () => {
  // TODO: 实现管理员详细导出功能
  ElMessage.info('管理员详细导出功能开发中...')
}

// 生命周期
onMounted(() => {
  loadExam()
  loadStatistics()
  loadStudentResults()
})
</script>

<style scoped>
.exam-results {
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

.admin-note {
  color: #f56c6c;
  font-weight: 500;
}

.statistics-card,
.progress-card,
.results-card {
  margin-bottom: 20px;
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

.score-excellent {
  color: #67c23a;
  font-weight: bold;
}

.score-good {
  color: #409eff;
  font-weight: bold;
}

.score-pass {
  color: #e6a23c;
  font-weight: bold;
}

.score-fail {
  color: #f56c6c;
  font-weight: bold;
}
</style>