<template>
  <div class="exam-answers">
    <div class="page-header">
      <el-breadcrumb>
        <el-breadcrumb-item :to="{ path: '/exams' }">考试列表</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: `/exams/${examId}` }">{{ exam?.title }}</el-breadcrumb-item>
        <el-breadcrumb-item>学生答案</el-breadcrumb-item>
      </el-breadcrumb>
      <h1>学生答案管理</h1>
      <p class="page-description">查看和管理考试的所有学生答案</p>
    </div>

    <!-- 考试信息概览 -->
    <el-card v-if="exam" class="exam-info-card">
      <template #header>
        <div class="card-header">
          <span>考试信息</span>
          <el-button link @click="goToExamDetail">返回考试详情</el-button>
        </div>
      </template>
      
      <el-descriptions :column="4" border>
        <el-descriptions-item label="考试标题">{{ exam.title }}</el-descriptions-item>
        <el-descriptions-item label="题目数量">{{ exam.totalQuestions || 0 }}</el-descriptions-item>
        <el-descriptions-item label="答案总数">{{ statistics.totalAnswers || 0 }}</el-descriptions-item>
        <el-descriptions-item label="已评估答案">{{ statistics.evaluatedAnswers || 0 }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 查看模式切换 - 显眼的顶部标签 -->
    <el-card class="view-mode-card">
      <div class="view-mode-switcher">
        <h3>查看模式</h3>
        <el-radio-group v-model="viewMode" size="large" @change="handleViewModeChange" class="mode-radio-group">
          <el-radio-button label="answers" size="large">
            <el-icon><List /></el-icon>
            按答案查看
          </el-radio-button>
          <el-radio-button label="papers" size="large">
            <el-icon><Document /></el-icon>
            按学生试卷查看
          </el-radio-button>
        </el-radio-group>
      </div>
    </el-card>

    <!-- 筛选和搜索 -->
    <el-card class="filter-card">
      <el-row :gutter="16">
        <el-col :span="3" v-if="viewMode === 'answers'">
          <el-select v-model="questionIdFilter" placeholder="选择题目" clearable @change="loadData">
            <el-option label="全部题目" value="" />
            <el-option 
              v-for="question in questions" 
              :key="question.id" 
              :label="`题目${question.id}: ${question.title}`" 
              :value="question.id"
            />
          </el-select>
        </el-col>
        <el-col :span="3" v-if="viewMode === 'answers'">
          <el-select v-model="isEvaluatedFilter" placeholder="评估状态" clearable @change="loadData">
            <el-option label="全部状态" value="" />
            <el-option label="已评估" :value="true" />
            <el-option label="未评估" :value="false" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-input 
            v-model="studentKeywordFilter" 
            :placeholder="viewMode === 'answers' ? '搜索学生姓名/学号' : '搜索学生姓名/学号'"
            @keyup.enter="loadData"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="viewMode === 'answers' ? 6 : 12">
          <el-button type="primary" icon="Search" @click="loadData">搜索</el-button>
          <el-button icon="Refresh" @click="resetFilters">重置</el-button>
        </el-col>
        <el-col :span="5" style="text-align: right">
          <el-button type="success" icon="Download" @click="exportAnswers">导出答案</el-button>
          <el-button type="warning" icon="Upload" @click="showImportDialog">导入答案</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 答案列表 - 按答案查看模式 -->
    <el-card v-if="viewMode === 'answers'" class="answers-card">
      <template #header>
        <div class="card-header">
          <span>学生答案列表 ({{ pagination.total }})</span>
          <div>
            <el-button 
              type="primary" 
              icon="MagicStick" 
              @click="batchEvaluate"
              :disabled="selectedAnswers.length === 0"
            >
              批量AI评估 ({{ selectedAnswers.length }})
            </el-button>
          </div>
        </div>
      </template>
      
      <el-table
        v-loading="loading"
        :data="filteredAnswers"
        style="width: 100%"
        @selection-change="handleSelectionChange"
        @sort-change="handleSortChange"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="student.name" label="学生姓名" width="120" sortable />
        
        <el-table-column prop="student.studentNumber" label="学号" width="120" />
        
        <el-table-column prop="questionTitle" label="题目" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click="viewQuestion(row.questionId)">
              {{ row.questionTitle }}
            </el-link>
          </template>
        </el-table-column>
        
        <el-table-column prop="answerText" label="答案内容" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="answer-content">
              {{ row.answerText || '-' }}
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="score" label="得分" width="100" sortable>
          <template #default="{ row }">
            <span v-if="row.score !== null" class="score">{{ row.score }}</span>
            <el-tag v-else type="warning" size="small">未评估</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="evaluated" label="评估状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.evaluated ? 'success' : 'warning'" size="small">
              {{ row.evaluated ? '已评估' : '未评估' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="submittedAt" label="提交时间" width="160" sortable>
          <template #default="{ row }">
            {{ formatDate(row.submittedAt) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" icon="View" @click="viewAnswerDetail(row)">
                查看
              </el-button>
              <el-button 
                size="small" 
                type="primary" 
                icon="MagicStick"
                @click="evaluateAnswer(row)"
                :disabled="row.evaluated"
              >
                评估
              </el-button>
              <el-button 
                size="small" 
                type="danger" 
                icon="Delete"
                @click="deleteAnswer(row.id)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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

    <!-- 学生试卷列表 - 按学生试卷查看模式 -->
    <el-card v-else-if="viewMode === 'papers'" class="papers-card">
      <template #header>
        <div class="card-header">
          <span>学生试卷列表 ({{ paperPagination.total }})</span>
          <div>
            <el-button 
              type="success" 
              icon="Document" 
              @click="batchExportPapers"
              :disabled="selectedPapers.length === 0"
            >
              批量导出试卷 ({{ selectedPapers.length }})
            </el-button>
          </div>
        </div>
      </template>
      
      <el-table
        v-loading="loading"
        :data="papersList"
        style="width: 100%"
        @selection-change="handlePaperSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="studentName" label="学生姓名" width="120" />
        
        <el-table-column prop="studentNumber" label="学号" width="120" />
        
        <el-table-column label="答题进度" width="120">
          <template #default="{ row }">
            <el-progress 
              :percentage="Math.round((row.answeredQuestions / row.totalQuestions) * 100)"
              :status="row.answeredQuestions === row.totalQuestions ? 'success' : 'warning'"
            />
            <div class="progress-text">
              {{ row.answeredQuestions }}/{{ row.totalQuestions }}
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="评估进度" width="120">
          <template #default="{ row }">
            <el-progress 
              :percentage="Math.round((row.evaluatedAnswers / row.totalQuestions) * 100)"
              :status="row.evaluatedAnswers >= row.totalQuestions ? 'success' : 'warning'"
            />
            <div class="progress-text">
              {{ row.evaluatedAnswers }}/{{ row.totalQuestions }}
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="得分" width="120">
          <template #default="{ row }">
            <div v-if="row.totalScore !== null && row.maxPossibleScore">
              <span class="score">{{ row.totalScore?.toFixed(1) }}</span>
              <span class="max-score"> / {{ row.maxPossibleScore?.toFixed(1) }}</span>
              <div class="score-percentage">
                ({{ row.scorePercentage?.toFixed(1) }}%)
              </div>
            </div>
            <el-tag v-else type="warning" size="small">未评估</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="submittedAt" label="提交时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.submittedAt) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" icon="View" @click="viewStudentPaper(row)">
                查看试卷
              </el-button>
              <el-button 
                size="small" 
                type="primary" 
                icon="Edit"
                @click="editStudentPaper(row)"
              >
                评阅
              </el-button>
              <el-dropdown @command="(command) => handlePaperAction(command, row)">
                <el-button size="small" icon="Download">
                  导出<el-icon class="el-icon--right"><arrow-down /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="exportPdf">导出PDF</el-dropdown-item>
                    <el-dropdown-item command="exportExcel">导出Excel</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="paperPagination.page"
          v-model:page-size="paperPagination.size"
          :total="paperPagination.total"
          :page-sizes="[10, 20, 50, 100]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handlePaperSizeChange"
          @current-change="handlePaperCurrentChange"
        />
      </div>
    </el-card>

    <!-- 答案详情对话框 -->
    <el-dialog
      v-model="answerDetailDialogVisible"
      title="答案详情"
      width="800px"
      destroy-on-close
    >
      <div v-if="currentAnswer" class="answer-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="学生姓名">{{ currentAnswer.student?.name }}</el-descriptions-item>
          <el-descriptions-item label="学号">{{ currentAnswer.student?.studentNumber }}</el-descriptions-item>
          <el-descriptions-item label="题目">{{ currentAnswer.questionTitle }}</el-descriptions-item>
          <el-descriptions-item label="得分">
            <span v-if="currentAnswer.score !== null">{{ currentAnswer.score }}</span>
            <el-tag v-else type="warning" size="small">未评估</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间" :span="2">
            {{ currentAnswer.submittedAt ? formatDate(currentAnswer.submittedAt) : '未知' }}
          </el-descriptions-item>
        </el-descriptions>
        
        <div class="answer-content-section">
          <h4>答案内容：</h4>
          <div class="answer-text">
            {{ currentAnswer.answerText || '无答案内容' }}
          </div>
        </div>
        
        <div v-if="currentAnswer.feedback" class="feedback-section">
          <h4>评估反馈：</h4>
          <div class="feedback-text">
            {{ currentAnswer.feedback }}
          </div>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="answerDetailDialogVisible = false">关闭</el-button>
          <el-button 
            v-if="currentAnswer && !currentAnswer.evaluated"
            type="primary" 
            @click="evaluateCurrentAnswer"
          >
            评估此答案
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 导入对话框 -->
    <el-dialog
      v-model="importDialogVisible"
      title="导入学生答案"
      width="600px"
    >
      <div class="import-section">
        <el-alert
          title="导入说明"
          type="info"
          :closable="false"
          style="margin-bottom: 20px"
        >
          <p>支持导入Excel格式的学生答案文件，文件需要包含以下列：</p>
          <ul>
            <li>学生姓名</li>
            <li>学号</li>
            <li>题目ID</li>
            <li>答案内容</li>
          </ul>
        </el-alert>
        
        <el-upload
          ref="uploadRef"
          :before-upload="beforeUpload"
          :on-change="handleFileChange"
          :auto-upload="false"
          accept=".xlsx,.xls,.csv"
          drag
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              只能上传 xlsx/xls/csv 文件
            </div>
          </template>
        </el-upload>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="importDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="confirmImport"
            :loading="importLoading"
            :disabled="!selectedFile"
          >
            确认导入
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Search, 
  Refresh, 
  Download, 
  Upload, 
  MagicStick, 
  View, 
  Delete,
  Edit,
  Document,
  ArrowDown,
  UploadFilled,
  List
} from '@element-plus/icons-vue'
import { examApi } from '@/api/exam'
import { answerApi } from '@/api/answer'
import { questionApi } from '@/api/question'
import { evaluationApi } from '@/api/evaluation'
import type { 
  ExamResponse, 
  StudentAnswerResponse, 
  StudentExamPaperResponse,
  QuestionResponse, 
  PageResponse 
} from '@/types/api'

const route = useRoute()
const router = useRouter()

// 响应式数据
const loading = ref(false)
const importLoading = ref(false)
const viewMode = ref<'answers' | 'papers'>('papers') // 查看模式：按答案查看 or 按学生试卷查看，默认按学生试卷查看
const exam = ref<ExamResponse | null>(null)
const questions = ref<QuestionResponse[]>([])
const answers = ref<StudentAnswerResponse[]>([])
const selectedAnswers = ref<StudentAnswerResponse[]>([])
const papersList = ref<StudentExamPaperResponse[]>([])
const selectedPapers = ref<StudentExamPaperResponse[]>([])
const currentAnswer = ref<StudentAnswerResponse | null>(null)
const selectedFile = ref<File | null>(null)
const answerDetailDialogVisible = ref(false)
const importDialogVisible = ref(false)

const statistics = ref({
  totalAnswers: 0,
  evaluatedAnswers: 0,
  averageScore: 0
})

// 使用 ref 来解决 el-select 类型问题
const questionIdFilter = ref<number | string>('')
const isEvaluatedFilter = ref<boolean | string>('')
const studentKeywordFilter = ref('')

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 学生试卷分页配置
const paperPagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const sortConfig = reactive({
  prop: '',
  order: ''
})

// 计算属性
const examId = computed(() => {
  const id = route.params.examId
  return typeof id === 'string' ? parseInt(id, 10) : Number(id)
})

// 筛选后的答案列表 - 现在直接使用API返回的数据
const answersPageData = ref<PageResponse<StudentAnswerResponse> | null>(null)

const filteredAnswers = computed(() => {
  return answersPageData.value?.content || []
})

// 方法
// 查看模式切换
const handleViewModeChange = () => {
  // 切换查看模式时重置分页
  if (viewMode.value === 'answers') {
    pagination.page = 1
  } else {
    paperPagination.page = 1
  }
  loadData()
}

// 统一的数据加载方法
const loadData = () => {
  if (viewMode.value === 'answers') {
    loadAnswers()
  } else {
    loadPapers()
  }
}

// 加载学生试卷数据
const loadPapers = async () => {
  try {
    loading.value = true
    console.log('Loading papers for exam:', examId.value, 'page:', paperPagination.page, 'size:', paperPagination.size)
    
    const pageData = await answerApi.getExamPapers(
      examId.value,
      paperPagination.page,
      paperPagination.size,
      studentKeywordFilter.value || undefined
    )
    
    console.log('Papers loaded:', pageData)
    papersList.value = pageData.content
    paperPagination.total = pageData.totalElements
    
  } catch (error) {
    console.error('Failed to load papers:', error)
    ElMessage.error('加载学生试卷失败')
  } finally {
    loading.value = false
  }
}

// 学生试卷相关的事件处理
const handlePaperSelectionChange = (selection: StudentExamPaperResponse[]) => {
  selectedPapers.value = selection
}

const handlePaperSizeChange = (size: number) => {
  paperPagination.size = size
  paperPagination.page = 1
  loadData()
}

const handlePaperCurrentChange = (page: number) => {
  paperPagination.page = page
  loadData()
}

// 查看学生试卷
const viewStudentPaper = (paper: StudentExamPaperResponse) => {
  router.push(`/exams/${examId.value}/students/${paper.studentId}/paper`)
}

// 编辑/评阅学生试卷
const editStudentPaper = (paper: StudentExamPaperResponse) => {
  // 跳转到学生试卷详情页面，可以在那里进行评阅
  router.push(`/exams/${examId.value}/students/${paper.studentId}/paper`)
}

// 试卷操作处理
const handlePaperAction = async (command: string, paper: StudentExamPaperResponse) => {
  if (command === 'exportPdf') {
    await exportSinglePaper(paper, 'pdf')
  } else if (command === 'exportExcel') {
    await exportSinglePaper(paper, 'excel')
  }
}

// 导出单个学生试卷
const exportSinglePaper = async (paper: StudentExamPaperResponse, format: string) => {
  try {
    const blob = await answerApi.exportStudentPaper(examId.value, paper.studentId, format)
    
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `学生试卷_${paper.studentName}_${exam.value?.title}.${format === 'pdf' ? 'pdf' : 'xlsx'}`
    link.click()
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('试卷导出成功')
  } catch (error) {
    console.error('Failed to export paper:', error)
    ElMessage.error('试卷导出失败')
  }
}

// 批量导出试卷
const batchExportPapers = async () => {
  if (selectedPapers.value.length === 0) {
    ElMessage.warning('请选择要导出的试卷')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要导出选中的 ${selectedPapers.value.length} 份试卷吗？`,
      '批量导出确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 逐个导出试卷
    for (const paper of selectedPapers.value) {
      await exportSinglePaper(paper, 'pdf')
      // 添加小延迟避免请求过快
      await new Promise(resolve => setTimeout(resolve, 500))
    }
    
    ElMessage.success('所有试卷导出完成')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to batch export papers:', error)
      ElMessage.error('批量导出失败')
    }
  }
}

const loadExamInfo = async () => {
  try {
    exam.value = await examApi.getExam(examId.value)
    questions.value = await questionApi.getQuestionsByExam(examId.value)
  } catch (error) {
    console.error('Failed to load exam info:', error)
    ElMessage.error('加载考试信息失败')
  }
}

const loadAnswers = async () => {
  try {
    loading.value = true
    
    // 使用新的分页筛选API
    const pageData = await answerApi.getAnswersByExamWithFilters(
      examId.value,
      pagination.page,
      pagination.size,
      questionIdFilter.value ? Number(questionIdFilter.value) : undefined,
      isEvaluatedFilter.value !== '' ? Boolean(isEvaluatedFilter.value) : undefined,
      studentKeywordFilter.value || undefined
    )
    
    answersPageData.value = pageData
    pagination.total = pageData.totalElements
    
    // 同时获取原始数据用于统计信息计算
    if (pagination.page === 1 && !questionIdFilter.value && isEvaluatedFilter.value === '' && !studentKeywordFilter.value) {
      // 只在第一页且无筛选时更新统计信息
      answers.value = pageData.content
      statistics.value.totalAnswers = pageData.totalElements
      statistics.value.evaluatedAnswers = pageData.content.filter(a => a.evaluated).length
      const scores = pageData.content.filter(a => a.score !== null && a.score !== undefined).map(a => a.score!)
      statistics.value.averageScore = scores.length > 0 
        ? Math.round(scores.reduce((sum, score) => sum + (score || 0), 0) / scores.length * 10) / 10
        : 0
    }
      
  } catch (error) {
    console.error('Failed to load answers:', error)
    ElMessage.error('加载答案列表失败')
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  questionIdFilter.value = ''
  isEvaluatedFilter.value = ''
  studentKeywordFilter.value = ''
  pagination.page = 1
  loadAnswers()
}

const handleSelectionChange = (selection: StudentAnswerResponse[]) => {
  selectedAnswers.value = selection
}

const handleSortChange = ({ prop, order }: any) => {
  sortConfig.prop = prop
  sortConfig.order = order
  // TODO: 实现排序逻辑，目前先重新加载
  loadAnswers()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  loadAnswers()
}

const handleCurrentChange = (page: number) => {
  pagination.page = page
  loadAnswers()
}

const viewQuestion = (questionId: number) => {
  router.push(`/questions/${questionId}`)
}

const viewAnswerDetail = (answer: StudentAnswerResponse) => {
  currentAnswer.value = answer
  answerDetailDialogVisible.value = true
}

const evaluateAnswer = async (answer: StudentAnswerResponse) => {
  try {
    await evaluationApi.evaluateAnswer(answer.id)
    ElMessage.success('答案评估成功')
    loadAnswers()
  } catch (error) {
    console.error('Failed to evaluate answer:', error)
    ElMessage.error('答案评估失败')
  }
}

const evaluateCurrentAnswer = async () => {
  if (currentAnswer.value) {
    await evaluateAnswer(currentAnswer.value)
    answerDetailDialogVisible.value = false
  }
}

const batchEvaluate = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要对选中的 ${selectedAnswers.value.length} 个答案进行AI评估吗？`,
      '批量评估确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const answerIds = selectedAnswers.value.map(a => a.id)
    await evaluationApi.batchEvaluateAnswers(answerIds)
    ElMessage.success('批量评估任务已开始')
    loadAnswers()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to batch evaluate:', error)
      ElMessage.error('批量评估失败')
    }
  }
}

const deleteAnswer = async (answerId: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个答案吗？此操作不可恢复。',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await answerApi.deleteAnswer(answerId)
    ElMessage.success('答案删除成功')
    loadAnswers()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to delete answer:', error)
      ElMessage.error('删除答案失败')
    }
  }
}

const exportAnswers = async () => {
  try {
    const questionId = typeof questionIdFilter.value === 'number' ? questionIdFilter.value : undefined
    const isEvaluated = typeof isEvaluatedFilter.value === 'boolean' ? isEvaluatedFilter.value : undefined
    
    await answerApi.exportAnswers(examId.value, questionId, isEvaluated)
    ElMessage.success('答案导出成功')
  } catch (error) {
    console.error('Failed to export answers:', error)
    ElMessage.error('答案导出失败')
  }
}

const showImportDialog = () => {
  importDialogVisible.value = true
  selectedFile.value = null
}

const beforeUpload = (file: File) => {
  const isValidType = ['application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 
                      'application/vnd.ms-excel', 
                      'text/csv'].includes(file.type)
  if (!isValidType) {
    ElMessage.error('只能上传 Excel 或 CSV 文件!')
    return false
  }
  
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB!')
    return false
  }
  
  return false // 阻止自动上传
}

const handleFileChange = (file: any) => {
  selectedFile.value = file.raw
}

const confirmImport = async () => {
  if (!selectedFile.value) {
    ElMessage.error('请选择要导入的文件')
    return
  }
  
  try {
    importLoading.value = true
    await answerApi.importAnswersToExam(examId.value, selectedFile.value)
    ElMessage.success('答案导入成功')
    importDialogVisible.value = false
    loadAnswers()
  } catch (error) {
    console.error('Failed to import answers:', error)
    ElMessage.error('答案导入失败')
  } finally {
    importLoading.value = false
  }
}

const goToExamDetail = () => {
  router.push(`/exams/${examId.value}`)
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN')
}

onMounted(() => {
  loadExamInfo()
  loadData() // 使用统一的数据加载方法，根据当前模式加载数据
})
</script>

<style scoped>
.exam-answers {
  max-width: 1400px;
  margin: 0 auto;
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
.filter-card,
.answers-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
}

.answer-content {
  max-width: 300px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.action-buttons {
  display: flex;
  gap: 4px;
  flex-wrap: nowrap;
}

.action-buttons .el-button {
  margin: 0;
  padding: 5px 8px;
  font-size: 12px;
}

.score {
  font-weight: 600;
  color: #67c23a;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.answer-detail {
  margin-bottom: 20px;
}

.answer-content-section,
.feedback-section {
  margin-top: 20px;
}

.answer-text,
.feedback-text {
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  min-height: 100px;
  white-space: pre-wrap;
  line-height: 1.6;
}

/* 查看模式切换样式 */
.view-mode-card {
  margin-bottom: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.view-mode-card :deep(.el-card__body) {
  padding: 20px 24px;
}

.view-mode-switcher {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.view-mode-switcher h3 {
  color: white;
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.mode-radio-group {
  gap: 12px;
}

.mode-radio-group :deep(.el-radio-button__inner) {
  padding: 12px 24px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.mode-radio-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: #409eff;
  border-color: #409eff;
  color: white;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  transform: translateY(-1px);
}

.mode-radio-group :deep(.el-radio-button__inner:hover) {
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
</style>
