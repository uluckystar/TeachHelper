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

    <!-- 筛选和搜索 -->
    <el-card class="filter-card">
      <el-row :gutter="16">
        <el-col :span="4">
          <el-select v-model="questionIdFilter" placeholder="选择题目" clearable @change="loadAnswers">
            <el-option label="全部题目" value="" />
            <el-option 
              v-for="question in questions" 
              :key="question.id" 
              :label="`题目${question.id}: ${question.title}`" 
              :value="question.id"
            />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="isEvaluatedFilter" placeholder="评估状态" clearable @change="loadAnswers">
            <el-option label="全部状态" value="" />
            <el-option label="已评估" :value="true" />
            <el-option label="未评估" :value="false" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-input 
            v-model="studentKeywordFilter" 
            placeholder="搜索学生姓名/学号"
            @keyup.enter="loadAnswers"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="6">
          <el-button type="primary" icon="Search" @click="loadAnswers">搜索</el-button>
          <el-button icon="Refresh" @click="resetFilters">重置</el-button>
        </el-col>
        <el-col :span="6" style="text-align: right">
          <el-button type="success" icon="Download" @click="exportAnswers">导出答案</el-button>
          <el-button type="warning" icon="Upload" @click="showImportDialog">导入答案</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 答案列表 -->
    <el-card class="answers-card">
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
        :data="answers"
        style="width: 100%"
        @selection-change="handleSelectionChange"
        @sort-change="handleSortChange"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="studentName" label="学生姓名" width="120" sortable />
        
        <el-table-column prop="studentId" label="学号" width="120" />
        
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
        
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button-group>
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
            </el-button-group>
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

    <!-- 答案详情对话框 -->
    <el-dialog
      v-model="answerDetailDialogVisible"
      title="答案详情"
      width="800px"
      destroy-on-close
    >
      <div v-if="currentAnswer" class="answer-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="学生姓名">{{ currentAnswer.studentName }}</el-descriptions-item>
          <el-descriptions-item label="学号">{{ currentAnswer.studentId }}</el-descriptions-item>
          <el-descriptions-item label="题目">{{ currentAnswer.questionTitle }}</el-descriptions-item>
          <el-descriptions-item label="得分">
            <span v-if="currentAnswer.score !== null">{{ currentAnswer.score }}</span>
            <el-tag v-else type="warning" size="small">未评估</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间" :span="2">
            {{ formatDate(currentAnswer.submittedAt) }}
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
  UploadFilled
} from '@element-plus/icons-vue'
import { examApi } from '@/api/exam'
import { answerApi } from '@/api/answer'
import { questionApi } from '@/api/question'
import { evaluationApi } from '@/api/evaluation'
import type { ExamResponse, StudentAnswerResponse, QuestionResponse } from '@/types/api'

const route = useRoute()
const router = useRouter()

// 响应式数据
const loading = ref(false)
const importLoading = ref(false)
const exam = ref<ExamResponse | null>(null)
const questions = ref<QuestionResponse[]>([])
const answers = ref<StudentAnswerResponse[]>([])
const selectedAnswers = ref<StudentAnswerResponse[]>([])
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

const sortConfig = reactive({
  prop: '',
  order: ''
})

// 计算属性
const examId = computed(() => {
  const id = route.params.examId
  return typeof id === 'string' ? parseInt(id, 10) : Number(id)
})

// 方法
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
    
    const response = await answerApi.getAnswersByExam(examId.value)
    answers.value = response || []
    pagination.total = response.length || 0
    
    // 计算统计信息
    statistics.value.totalAnswers = pagination.total
    statistics.value.evaluatedAnswers = answers.value.filter(a => a.evaluated).length
    const scores = answers.value.filter(a => a.score !== null && a.score !== undefined).map(a => a.score!)
    statistics.value.averageScore = scores.length > 0 
      ? Math.round(scores.reduce((sum, score) => sum + (score || 0), 0) / scores.length * 10) / 10
      : 0
      
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
  loadAnswers()
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

.import-section ul {
  margin: 10px 0;
  padding-left: 20px;
}

.import-section li {
  margin: 5px 0;
}
</style>
