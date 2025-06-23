<template>
  <div class="exam-management">
    <div class="page-header">
      <div class="header-content">
        <h1 v-if="authStore.isTeacher || authStore.isAdmin">考试管理中心</h1>
        <div v-else>
          <h1>参加考试</h1>
          <p class="page-description">浏览和参加当前可用的考试</p>
        </div>
        <div class="header-actions">
          <el-button
            v-if="authStore.isTeacher || authStore.isAdmin"
            type="primary"
            @click="$router.push('/exams/create')"
          >
            <el-icon><Plus /></el-icon>
            创建考试
          </el-button>
          <el-button
            v-if="authStore.isStudent"
            type="success"
            plain
            @click="$router.push('/my-exams')"
          >
            <el-icon><User /></el-icon>
            我的考试
          </el-button>
        </div>
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
              <!-- 学生查看操作 -->
              <template v-if="authStore.isStudent">
                <!-- 学生根据考试状态显示不同按钮 -->
                <el-button
                  v-if="row.canTake"
                  size="small"
                  type="primary"
                  @click="takeExam(row.id)"
                >
                  <el-icon><Edit /></el-icon>
                  参加考试
                </el-button>
                <el-button
                  v-else-if="row.status === 'PUBLISHED' && row.canTake === false"
                  size="small"
                  type="success"
                  disabled
                >
                  <el-icon><Check /></el-icon>
                  已提交
                </el-button>
                <el-button
                  v-else-if="row.status === 'ENDED' || row.status === 'EVALUATED'"
                  size="small"
                  type="info"
                  @click="viewStudentResult(row.id)"
                >
                  <el-icon><View /></el-icon>
                  查看结果
                </el-button>
                <el-button
                  v-else
                  size="small"
                  type="info"
                  plain
                  disabled
                >
                  <el-icon><View /></el-icon>
                  未开始
                </el-button>
                
                <!-- 学生专用的考试详情按钮 -->
                <el-button
                  size="small"
                  type="default"
                  plain
                  @click="viewStudentExamDetail(row.id)"
                >
                  <el-icon><InfoFilled /></el-icon>
                  查看详情
                </el-button>
              </template>

              <!-- 教师/管理员的查看按钮 -->
              <el-button
                v-else
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

    <!-- 发布考试 - 班级选择对话框 -->
    <el-dialog
      v-model="showPublishDialog"
      title="发布考试 - 选择班级"
      width="700px"
    >
      <div v-if="currentExam" class="publish-exam-dialog">
        <el-alert
          :title="`正在发布考试：${currentExam.title}`"
          type="info"
          :closable="false"
          style="margin-bottom: 20px;"
        />
        
        <el-form label-width="80px">
          <el-form-item label="目标班级" required>
            <el-select
              v-model="selectedClassroomIds"
              multiple
              placeholder="选择要发布考试的班级（可多选）"
              style="width: 100%"
              :loading="classroomLoading"
              filterable
              collapse-tags
              collapse-tags-tooltip
            >
              <el-option
                v-for="classroom in availableClassrooms"
                :key="classroom.id"
                :label="`${classroom.name} (${classroom.classCode})`"
                :value="classroom.id"
              >
                <div class="classroom-option">
                  <span class="classroom-name">{{ classroom.name }}</span>
                  <span class="classroom-code">({{ classroom.classCode }})</span>
                  <span class="classroom-students">{{ classroom.studentCount }}人</span>
                </div>
              </el-option>
            </el-select>
            <div class="form-help">
              选择要发布考试的班级，只有这些班级的学生能看到考试
            </div>
          </el-form-item>
        </el-form>

        <!-- 所有可选班级 -->
        <el-divider>所有可选班级</el-divider>
        <div v-if="availableClassrooms.length === 0" class="empty-state">
          <el-empty description="暂无可选班级" size="small" />
        </div>
        <div v-else class="available-classrooms">
          <el-card
            v-for="classroom in availableClassrooms"
            :key="classroom.id"
            class="classroom-card selectable-classroom"
            shadow="hover"
            :class="{ selected: selectedClassroomIds.includes(classroom.id) }"
            @click="toggleClassroomSelection(classroom.id)"
          >
            <div class="classroom-info">
              <div class="classroom-header">
                <el-checkbox 
                  :model-value="selectedClassroomIds.includes(classroom.id)"
                  @change="toggleClassroomSelection(classroom.id)"
                  @click.stop
                >
                  <h4>{{ classroom.name }}</h4>
                </el-checkbox>
                <el-tag size="small">{{ classroom.classCode }}</el-tag>
              </div>
              <div class="classroom-stats">
                <span>学生数量: {{ classroom.studentCount }}</span>
                <el-button
                  size="small"
                  type="primary"
                  link
                  @click.stop="showClassroomMembers(classroom)"
                >
                  查看成员
                </el-button>
              </div>
            </div>
          </el-card>
        </div>

        <!-- 已选班级预览 -->
        <div v-if="selectedClassroomIds.length > 0">
          <el-divider>已选班级 ({{ selectedClassroomIds.length }})</el-divider>
          <div class="selected-classrooms">
            <el-card
              v-for="classroomId in selectedClassroomIds"
              :key="classroomId"
              class="classroom-card"
              shadow="hover"
            >
              <div class="classroom-info">
                <div class="classroom-header">
                  <h4>{{ getClassroomById(classroomId)?.name }}</h4>
                  <el-tag size="small">{{ getClassroomById(classroomId)?.classCode }}</el-tag>
                </div>
                <div class="classroom-stats">
                  <span>学生数量: {{ getClassroomById(classroomId)?.studentCount }}</span>
                  <el-button
                    size="small"
                    type="primary"
                    link
                    @click="showClassroomMembers(getClassroomById(classroomId)!)"
                  >
                    查看成员
                  </el-button>
                </div>
              </div>
            </el-card>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="showPublishDialog = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="confirmPublishExam" 
          :loading="publishLoading"
          :disabled="selectedClassroomIds.length === 0"
        >
          发布考试
        </el-button>
      </template>
    </el-dialog>

    <!-- 班级成员查看对话框 -->
    <el-dialog
      v-model="showMembersDialog"
      title="班级成员"
      width="600px"
    >
      <div v-if="selectedClassroomForMembers">
        <el-descriptions :column="2" border class="mb-4">
          <el-descriptions-item label="班级名称">
            {{ selectedClassroomForMembers.name }}
          </el-descriptions-item>
          <el-descriptions-item label="班级代码">
            <el-tag>{{ selectedClassroomForMembers.classCode }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="学生数量" :span="2">
            {{ selectedClassroomForMembers.studentCount }} 人
          </el-descriptions-item>
        </el-descriptions>

        <el-divider>学生列表</el-divider>
        <div v-if="selectedClassroomForMembers.students && selectedClassroomForMembers.students.length > 0">
          <el-table :data="selectedClassroomForMembers.students" stripe>
            <el-table-column prop="username" label="姓名" />
            <el-table-column prop="email" label="邮箱" />
            <el-table-column label="角色">
              <template #default="scope">
                <el-tag 
                  v-for="role in scope.row.roles" 
                  :key="role" 
                  size="small"
                  :type="role === 'STUDENT' ? 'primary' : 'info'"
                >
                  {{ role === 'STUDENT' ? '学生' : role }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div v-else class="empty-state">
          <el-empty description="该班级暂无学生" size="small" />
        </div>
      </div>
      <template #footer>
        <el-button @click="showMembersDialog = false">关闭</el-button>
      </template>
    </el-dialog>
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
  CircleCheck,
  Check,
  InfoFilled,
  User
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { examApi } from '@/api/exam'
import { classroomApi, type ClassroomResponse } from '@/api/classroom'
import { evaluationApi } from '@/api/evaluation'
import { questionApi } from '@/api/question'
import { rubricApi } from '@/api/rubric'
import { studentAnswerApi } from '@/api/answer'
import type { Exam } from '@/types/api'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const exams = ref<Exam[]>([])

// 班级选择相关数据
const showPublishDialog = ref(false)
const publishLoading = ref(false)
const classroomLoading = ref(false)
const currentExam = ref<Exam | null>(null)
const availableClassrooms = ref<ClassroomResponse[]>([])
const selectedClassroomIds = ref<number[]>([])
const showMembersDialog = ref(false)
const selectedClassroomForMembers = ref<ClassroomResponse | null>(null)

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
    
    console.log('=== loadExams Debug ===')
    console.log('authStore.isStudent:', authStore.isStudent)
    console.log('authStore.isTeacher:', authStore.isTeacher)
    console.log('authStore.isAdmin:', authStore.isAdmin)
    console.log('authStore.user:', authStore.user)
    console.log('pagination:', pagination)
    
    if (authStore.isStudent) {
      console.log('学生端：调用 examApi.getAvailableExams()')
      // 学生使用专用的可参加考试API，只获取PUBLISHED状态的考试
      const response = await examApi.getAvailableExams()
      
      console.log('学生端：getAvailableExams 响应:', response)
      
      // 检查每个考试的提交状态
      const examsWithStatus = await Promise.all(
        response.map(async (exam) => {
          try {
            const hasSubmitted = await studentAnswerApi.hasCurrentStudentSubmittedExam(exam.id)
            return {
              ...exam,
              canTake: exam.status === 'PUBLISHED' && !hasSubmitted
            }
          } catch (error) {
            console.error(`检查考试 ${exam.id} 提交状态失败:`, error)
            return {
              ...exam,
              canTake: false // 出错时保守处理
            }
          }
        })
      )
      exams.value = examsWithStatus
      pagination.total = examsWithStatus.length
    } else {
      console.log('教师/管理员端：调用 examApi.getAllExams()', pagination.page - 1, pagination.size)
      // 教师/管理员使用全量考试API
      const response = await examApi.getAllExams(pagination.page - 1, pagination.size)
      exams.value = response
      pagination.total = response.length
    }
    
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
  if (!dateString) {
    return '未设定'
  }
  try {
    const date = new Date(dateString)
    // 检查日期是否有效（不是1970年）
    if (isNaN(date.getTime()) || date.getFullYear() < 2000) {
      return '未设定'
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
    console.error('Date formatting error:', error, dateString)
    return '时间格式错误'
  }
}

const viewExam = (examId: number) => {
  router.push(`/exams/${examId}`)
}

// 学生参加考试
const takeExam = (examId: number) => {
  router.push(`/exams/${examId}/take`)
}

// 学生查看考试结果
const viewStudentResult = (examId: number) => {
  router.push(`/my-exams/${examId}/result`)
}

// 学生查看考试详情
const viewStudentExamDetail = (examId: number) => {
  router.push(`/my-exams/${examId}`)
}

// 教师/管理员操作方法
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



const publishExam = async (exam: any) => {
  try {
    // 检查考试结束时间
    if (exam.endTime) {
      const endTime = new Date(exam.endTime)
      const now = new Date()
      
      if (endTime <= now) {
        // 考试已过期，阻止发布并提供编辑选项
        try {
          await ElMessageBox.confirm(
            `⚠️ 考试"${exam.title}"的结束时间已过期，无法发布！\n\n是否立即编辑考试修改时间？`,
            '考试时间已过期',
            {
              confirmButtonText: '编辑考试',
              cancelButtonText: '取消',
              type: 'error'
            }
          )
          // 用户选择编辑，跳转到编辑页面
          editExam(exam.id)
          return
        } catch {
          // 用户取消，直接返回
          return
        }
      }
    }
    
    currentExam.value = exam
    
    // 加载班级数据
    classroomLoading.value = true
    const classrooms = await classroomApi.getAllClassrooms()
    availableClassrooms.value = classrooms
    selectedClassroomIds.value = []
    
    // 显示班级选择对话框
    showPublishDialog.value = true
  } catch (error) {
    console.error('Failed to load classrooms:', error)
    ElMessage.error('加载班级信息失败')
  } finally {
    classroomLoading.value = false
  }
}

const confirmPublishExam = async () => {
  if (!currentExam.value) return
  
  try {
    if (selectedClassroomIds.value.length === 0) {
      ElMessage.warning('请选择至少一个班级')
      return
    }
    
    await ElMessageBox.confirm(
      `确定发布考试"${currentExam.value.title}"到所选的 ${selectedClassroomIds.value.length} 个班级吗？发布后学生将可以参加考试。`,
      '确认发布',
      {
        confirmButtonText: '确定发布',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    publishLoading.value = true
    
    // 先更新班级，再发布考试
    await examApi.updateExamClassrooms(currentExam.value.id, selectedClassroomIds.value)
    const publishResponse = await examApi.publishExam(currentExam.value.id)
    
    // 检查是否有时间提醒消息
    if (publishResponse.timeMessage) {
      // 显示时间提醒消息
      if (publishResponse.timeMessage.includes('过期')) {
        ElMessage.warning({
          message: `考试发布成功！${publishResponse.timeMessage}`,
          duration: 5000
        })
      } else {
        ElMessage.info({
          message: `考试发布成功！${publishResponse.timeMessage}`,
          duration: 4000
        })
      }
    } else {
      ElMessage.success('考试发布成功')
    }
    
    showPublishDialog.value = false
    loadExams()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to publish exam:', error)
      
      // 处理特定的验证错误
      if (error.code === 422) {
        ElMessage.warning(error.message || '考试必须包含至少一个题目才能发布')
      } else {
        ElMessage.error(error.message || '发布考试失败')
      }
    }
  } finally {
    publishLoading.value = false
  }
}

const showClassroomMembers = async (classroom: ClassroomResponse) => {
  try {
    selectedClassroomForMembers.value = await classroomApi.getClassroom(classroom.id)
    showMembersDialog.value = true
  } catch (error) {
    console.error('获取班级成员失败:', error)
    ElMessage.error('获取班级成员失败')
  }
}

const getClassroomById = (classroomId: number) => {
  return availableClassrooms.value.find(c => c.id === classroomId)
}

// 切换班级选择状态
const toggleClassroomSelection = (classroomId: number) => {
  const index = selectedClassroomIds.value.indexOf(classroomId)
  if (index > -1) {
    selectedClassroomIds.value.splice(index, 1)
  } else {
    selectedClassroomIds.value.push(classroomId)
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
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.header-content {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  flex: 1;
}

.header-content h1 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 24px;
  font-weight: 500;
}

.page-description {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 12px;
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

/* 发布对话框样式 */
.publish-exam-dialog {
  .form-help {
    margin-top: 8px;
    color: #666;
    font-size: 14px;
  }
}

.classroom-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.classroom-name {
  font-weight: 500;
}

.classroom-code {
  color: #666;
  margin-left: 8px;
}

.classroom-students {
  color: #909399;
  font-size: 12px;
  margin-left: auto;
}

.selected-classrooms {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.available-classrooms {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  max-height: 400px;
  overflow-y: auto;
}

.classroom-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.selectable-classroom {
  cursor: pointer;
}

.selectable-classroom:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px 0 rgba(64, 158, 255, 0.15);
}

.selectable-classroom.selected {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.classroom-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.classroom-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.classroom-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.classroom-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #666;
  font-size: 14px;
}

.empty-state {
  text-align: center;
  padding: 20px 0;
}

.mb-4 {
  margin-bottom: 16px;
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
