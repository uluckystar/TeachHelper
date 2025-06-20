<template>
  <div class="exam-management">
    <div class="page-header">
      <div class="header-content">
        <h1>考试管理中心</h1>
        <el-button
          v-if="authStore.isTeacher || authStore.isAdmin"
          type="primary"
          @click="$router.push('/exams/create')"
        >
          <el-icon><Plus /></el-icon>
          创建考试
        </el-button>
      </div>
    </div>

    <!-- 教师/管理员视图：包含批阅管理功能 -->
    <template v-if="authStore.isTeacher || authStore.isAdmin">
      <!-- AI批阅功能入口卡片 -->
      <el-row :gutter="24" class="feature-cards">
        <el-col :span="6">
          <el-card class="feature-card" @click="goToPaperGeneration">
            <div class="feature-content">
              <div class="feature-icon">
                <el-icon size="48" color="#722ed1"><MagicStick /></el-icon>
              </div>
              <div class="feature-info">
                <h3>AI试卷生成</h3>
                <p>智能生成试卷题目</p>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="feature-card" @click="goToBatchEvaluation">
            <div class="feature-content">
              <div class="feature-icon">
                <el-icon size="48" color="#409eff"><MagicStick /></el-icon>
              </div>
              <div class="feature-info">
                <h3>批量AI批阅</h3>
                <p>选择考试进行批量AI智能批阅</p>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="feature-card" @click="goToRubricManagement">
            <div class="feature-content">
              <div class="feature-icon">
                <el-icon size="48" color="#67c23a"><DocumentChecked /></el-icon>
              </div>
              <div class="feature-info">
                <h3>批阅标准管理</h3>
                <p>创建和管理AI批阅标准</p>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="feature-card" @click="goToTaskMonitor">
            <div class="feature-content">
              <div class="feature-icon">
                <el-icon size="48" color="#e6a23c"><Monitor /></el-icon>
              </div>
              <div class="feature-info">
                <h3>任务监控</h3>
                <p>查看AI批阅任务状态和进度</p>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- AI批阅任务统计 -->
      <el-row :gutter="24" class="stats-cards">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon size="24" color="#909399"><DataAnalysis /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ taskStats.total || 0 }}</div>
                <div class="stat-label">AI批阅任务总数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card pending">
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon size="24" color="#409eff"><Clock /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ taskStats.pending || 0 }}</div>
                <div class="stat-label">等待批阅</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card running">
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon size="24" color="#e6a23c"><Loading /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ taskStats.running || 0 }}</div>
                <div class="stat-label">正在批阅</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card completed">
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon size="24" color="#67c23a"><CircleCheck /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ taskStats.completed || 0 }}</div>
                <div class="stat-label">批阅完成</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>


    </template>

    <!-- 考试列表 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ authStore.isStudent ? '可参加的考试' : '考试列表' }}</span>
        </div>
      </template>

      <div class="filter-section">
        <el-form :model="filters" inline>
          <el-form-item label="状态">
            <el-select v-model="filters.status" placeholder="全部状态" clearable>
              <el-option label="草稿" value="DRAFT" />
              <el-option label="已发布" value="PUBLISHED" />
              <el-option label="进行中" value="IN_PROGRESS" />
              <el-option label="已结束" value="ENDED" />
              <el-option label="批阅完成" value="EVALUATED" />
              <el-option label="待批阅" value="ENDED" />
              <el-option label="批阅完成" value="EVALUATED" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="搜索">
            <el-input
              v-model="filters.keyword"
              placeholder="搜索考试标题"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table
        v-loading="loading"
        :data="exams"
        style="width: 100%"
        @sort-change="handleSortChange"
      >
        <el-table-column prop="title" label="考试标题" sortable />
        
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="startTime" label="开始时间" width="180" sortable>
          <template #default="{ row }">
            {{ formatDate(row.startTime) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="endTime" label="结束时间" width="180" sortable>
          <template #default="{ row }">
            {{ formatDate(row.endTime) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="duration" label="时长" width="80">
          <template #default="{ row }">
            {{ row.duration ? `${row.duration}分钟` : '不限时' }}
          </template>
        </el-table-column>
        
        <el-table-column prop="questionCount" label="题目数" width="80">
          <template #default="{ row }">
            {{ row.questionCount || 0 }}
          </template>
        </el-table-column>
        
        <el-table-column v-if="authStore.isTeacher || authStore.isAdmin" prop="totalAnswers" label="答案数" width="80">
          <template #default="{ row }">
            {{ row.totalAnswers || 0 }}
          </template>
        </el-table-column>
        
        <el-table-column v-if="authStore.isTeacher || authStore.isAdmin" prop="evaluatedAnswers" label="已批阅" width="80">
          <template #default="{ row }">
            {{ row.evaluatedAnswers || 0 }}
          </template>
        </el-table-column>
        
        <el-table-column prop="createdAt" label="创建时间" width="180" sortable>
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <!-- 查看按钮始终显示 -->
              <el-button
                size="small"
                type="info"
                plain
                @click="viewExam(row.id)"
              >
                <el-icon><View /></el-icon>
                查看
              </el-button>
              
              <!-- 教师/管理员操作 -->
              <template v-if="authStore.isTeacher || authStore.isAdmin">
                <!-- 草稿状态操作 -->
                <template v-if="row.status === 'DRAFT'">
                  <el-button
                    size="small"
                    type="primary"
                    @click="editExam(row.id)"
                  >
                    <el-icon><Edit /></el-icon>
                    编辑
                  </el-button>
                  
                  <el-button
                    size="small"
                    type="success"
                    @click="publishExam(row)"
                  >
                    <el-icon><Promotion /></el-icon>
                    发布
                  </el-button>
                  
                  <el-popconfirm
                    title="确定删除这个考试吗？"
                    @confirm="deleteExam(row.id)"
                  >
                    <template #reference>
                      <el-button
                        size="small"
                        type="danger"
                        plain
                      >
                        <el-icon><Delete /></el-icon>
                      </el-button>
                    </template>
                  </el-popconfirm>
                </template>
                
                <!-- 已发布状态操作 -->
                <template v-if="row.status === 'PUBLISHED'">
                  <el-button
                    size="small"
                    type="warning"
                    @click="endExam(row)"
                  >
                    <el-icon><CircleClose /></el-icon>
                    结束
                  </el-button>
                </template>
                
                <!-- 待批阅/进行中状态操作 -->
                <template v-if="['ENDED', 'IN_PROGRESS'].includes(row.status)">
                  <el-button
                    size="small"
                    type="warning"
                    @click="viewAnswers(row.id)"
                  >
                    <el-icon><Document /></el-icon>
                    答案
                  </el-button>
                  
                  <el-button
                    size="small"
                    type="primary"
                    @click="checkRubricAndStartEvaluation(row.id)"
                  >
                    <el-icon><MagicStick /></el-icon>
                    AI批阅
                  </el-button>
                </template>
                
                <!-- 批阅完成状态操作 -->
                <template v-if="row.status === 'EVALUATED'">
                  <el-button
                    size="small"
                    type="success"
                    @click="viewResults(row.id)"
                  >
                    <el-icon><DataAnalysis /></el-icon>
                    结果
                  </el-button>
                </template>
              </template>
              
              <!-- 学生操作 -->
              <template v-else>
                <el-button
                  v-if="canTakeExam(row)"
                  size="small"
                  type="primary"
                  @click="takeExam(row.id)"
                >
                  <el-icon><CaretRight /></el-icon>
                  开始考试
                </el-button>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Plus, 
  View, 
  Edit, 
  Promotion, 
  Delete, 
  CircleClose, 
  Document, 
  DataAnalysis, 
  CaretRight,
  MagicStick,
  DocumentChecked,
  Monitor,
  Clock,
  Loading,
  CircleCheck
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { examApi } from '@/api/exam'
import { evaluationApi } from '@/api/evaluation'
import { questionApi } from '@/api/question'
import { rubricApi } from '@/api/rubric'
import type { Exam } from '@/types/api'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const exams = ref<Exam[]>([])

const filters = reactive({
  status: '',
  keyword: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const sortConfig = reactive({
  prop: '',
  order: ''
})

const taskStats = reactive({
  total: 0,
  pending: 0,
  running: 0,
  completed: 0
})

onMounted(() => {
  loadExams()
  if (authStore.isTeacher || authStore.isAdmin) {
    loadTaskStats()
  }
})

const loadExams = async () => {
  try {
    loading.value = true
    // 使用 getAllExams 替代不存在的 getMyExams 和 getExams 方法
    const response = await examApi.getAllExams(pagination.page - 1, pagination.size)
    exams.value = response
    pagination.total = response.length // 暂时使用数组长度，后续需要后端支持分页响应
  } catch (error) {
    console.error('Failed to load exams:', error)
    ElMessage.error('加载考试列表失败')
  } finally {
    loading.value = false
  }
}

const loadTaskStats = async () => {
  try {
    const stats = await evaluationApi.getTaskStats()
    Object.assign(taskStats, stats)
  } catch (error) {
    console.error('Failed to load task stats:', error)
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadExams()
}

const handleReset = () => {
  filters.status = ''
  filters.keyword = ''
  pagination.page = 1
  loadExams()
}

const handleSortChange = ({ prop, order }: any) => {
  sortConfig.prop = prop
  sortConfig.order = order
  loadExams()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  loadExams()
}

const handleCurrentChange = (page: number) => {
  pagination.page = page
  loadExams()
}

const getStatusTagType = (status: string) => {
  switch (status) {
    case 'DRAFT': return 'info'
    case 'PUBLISHED': return 'success'
    case 'IN_PROGRESS': return 'warning'
    case 'ENDED': return 'primary'
    case 'EVALUATED': return 'success'
    default: return 'info'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'DRAFT': return '草稿'
    case 'PUBLISHED': return '已发布'
    case 'IN_PROGRESS': return '进行中'
    case 'ENDED': return '待批阅'
    case 'EVALUATED': return '批阅完成'
    default: return status
  }
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN')
}

const viewExam = (examId: number) => {
  router.push(`/exams/${examId}`)
}

const editExam = (examId: number) => {
  router.push(`/exams/${examId}/edit`)
}

const viewAnswers = (examId: number) => {
  router.push(`/exams/${examId}/answers`)
}

const viewResults = (examId: number) => {
  router.push(`/exams/${examId}/results`)
}

const startExamEvaluation = (examId: number) => {
  router.push(`/exams/${examId}/ai-evaluation`)
}

// 检查评分标准并开始批阅
const checkRubricAndStartEvaluation = async (examId: number) => {
  try {
    // 首先获取考试的题目列表
    const questions = await questionApi.getQuestionsByExam(examId)
    
    if (questions.length === 0) {
      ElMessage.warning('该考试暂无题目，无法进行批阅')
      return
    }
    
    // 检查每个题目的评分标准
    const questionsWithoutRubric = []
    const questionsWithIncompleteRubric = []
    
    for (const question of questions) {
      try {
        const rubrics = await rubricApi.getCriteriaByQuestion(question.id)
        
        if (rubrics.length === 0) {
          questionsWithoutRubric.push(question)
        } else {
          // 检查评分标准是否完善
          const incompleteRubrics = rubrics.filter(rubric => 
            !rubric.criterionText || 
            !rubric.points || 
            rubric.points <= 0
          )
          
          if (incompleteRubrics.length > 0) {
            questionsWithIncompleteRubric.push(question)
          }
          
          // 检查总分是否合理
          const totalPoints = rubrics.reduce((sum, rubric) => sum + (rubric.points || 0), 0)
          if (Math.abs(totalPoints - question.maxScore) > 0.1) {
            questionsWithIncompleteRubric.push(question)
          }
        }
      } catch (error) {
        console.error(`Failed to check rubric for question ${question.id}:`, error)
        questionsWithoutRubric.push(question)
      }
    }
    
    // 如果有问题，提示用户
    if (questionsWithoutRubric.length > 0 || questionsWithIncompleteRubric.length > 0) {
      let message = '发现以下评分标准问题：\n'
      
      if (questionsWithoutRubric.length > 0) {
        message += `\n• ${questionsWithoutRubric.length} 个题目未设置评分标准`
      }
      
      if (questionsWithIncompleteRubric.length > 0) {
        message += `\n• ${questionsWithIncompleteRubric.length} 个题目的评分标准不完善`
      }
      
      message += '\n\n建议先完善评分标准再进行批阅。是否继续？'
      
      try {
        const result = await ElMessageBox.confirm(
          message,
          '评分标准检查',
          {
            confirmButtonText: '继续批阅',
            cancelButtonText: '完善标准',
            type: 'warning',
            dangerouslyUseHTMLString: false
          }
        )
        
        if (result === 'confirm') {
          // 评分标准检查通过，开始批阅
          ElMessage.success('评分标准检查通过，开始批阅')
          startExamEvaluation(examId)
        }
      } catch (error) {
        // 用户点击了"完善标准"按钮，跳转到评分标准管理页面
        router.push({
          path: `/exams/${examId}/rubric-management`,
          query: { 
            from: 'exam-list',
            action: 'ai-evaluation'
          }
        })
      }
      return
    }
    
    // 评分标准检查通过，开始批阅
    ElMessage.success('评分标准检查通过，开始批阅')
    startExamEvaluation(examId)
    
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('评分标准检查失败:', error)
      ElMessage.error('评分标准检查失败，无法开始批阅')
    }
  }
}



const publishExam = async (exam: Exam) => {
  try {
    await ElMessageBox.confirm(
      `确定发布考试"${exam.title}"吗？发布后将无法修改。`,
      '确认发布',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await examApi.publishExam(exam.id)
    ElMessage.success('考试发布成功')
    loadExams()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to publish exam:', error)
      ElMessage.error('发布考试失败')
    }
  }
}

const endExam = async (exam: Exam) => {
  try {
    await ElMessageBox.confirm(
      `确定结束考试"${exam.title}"吗？结束后学生将无法继续作答。`,
      '确认结束',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await examApi.endExam(exam.id)
    ElMessage.success('考试结束成功')
    loadExams()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to end exam:', error)
      ElMessage.error('结束考试失败')
    }
  }
}

const deleteExam = async (examId: number) => {
  try {
    await examApi.deleteExam(examId)
    ElMessage.success('删除成功')
    loadExams()
  } catch (error) {
    console.error('Failed to delete exam:', error)
    ElMessage.error('删除考试失败')
  }
}

const canTakeExam = (exam: Exam) => {
  // 暂时返回 true，因为后端还没有实现相关字段
  // 后续当后端添加 status、startTime、endTime 字段后，可以恢复原有的逻辑：
  // const now = new Date()
  // const startTime = new Date(exam.startTime)
  // const endTime = new Date(exam.endTime)
  // return exam.status === 'PUBLISHED' && now >= startTime && now <= endTime
  return true
}

const takeExam = (examId: number) => {
  router.push(`/exams/${examId}/take`)
}

// AI批阅相关方法
const goToPaperGeneration = () => {
  router.push('/paper-generation')
}

const goToBatchEvaluation = () => {
  router.push('/batch-evaluation')
}

const goToRubricManagement = () => {
  router.push('/rubric-management')
}

const goToTaskMonitor = () => {
  router.push('/task-monitor')
}
</script>

<style scoped>
.exam-management {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content h1 {
  margin: 0;
  color: #303133;
  font-size: 24px;
  font-weight: 500;
}

/* 功能卡片样式 */
.feature-cards {
  margin-bottom: 24px;
}

.feature-card {
  cursor: pointer;
  transition: all 0.3s;
  border-radius: 8px;
}

.feature-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.feature-content {
  display: flex;
  align-items: center;
  text-align: center;
  padding: 24px 20px;
  height: 100%;
  justify-content: center;
}

.feature-icon {
  margin-bottom: 16px;
  flex-shrink: 0;
}

.feature-info h3 {
  margin: 0 0 12px 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.3;
}

.feature-info p {
  margin: 0;
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

/* 统计卡片样式 */
.stats-cards {
  margin-bottom: 24px;
}

.stat-card {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  transition: transform 0.3s;
  border: 2px solid transparent;
}

.stat-card:hover {
  transform: translateY(-2px);
  border-color: #409eff;
}

.stat-card.pending {
  border-left: 4px solid #e6a23c;
}

.stat-card.running {
  border-left: 4px solid #409eff;
}

.stat-card.completed {
  border-left: 4px solid #67c23a;
}

.stat-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  line-height: 1;
}

/* 任务列表样式 */
.recent-tasks {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
}

/* 考试列表样式 */
.filter-section {
  margin-bottom: 20px;
}

.action-buttons {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-start;
}

.action-buttons .el-button {
  margin: 0;
  border-radius: 4px;
}

.action-buttons .el-button + .el-button {
  margin-left: 0;
}

.action-buttons .el-button--small {
  padding: 5px 8px;
  font-size: 12px;
}

.action-buttons .el-button--small .el-icon {
  margin-right: 2px;
  font-size: 12px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

/* 响应式样式 */
@media (max-width: 768px) {
  .feature-cards .el-col {
    margin-bottom: 16px;
  }
  
  .stats-cards .el-col {
    margin-bottom: 16px;
  }
  
  .action-buttons {
    flex-direction: column;
    align-items: stretch;
  }
  
  .action-buttons .el-button {
    width: 100%;
    margin-bottom: 4px;
  }
}
</style>
