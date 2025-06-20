<template>
  <div class="question-management">
    <div class="page-header">
      <el-breadcrumb>
        <el-breadcrumb-item :to="{ path: '/exams' }">考试列表</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: `/exams/${examId}` }">{{ exam?.title }}</el-breadcrumb-item>
        <el-breadcrumb-item>题目管理</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="header-actions">
        <el-button type="primary" icon="MagicStick" @click="generateQuestions">
          AI生成题目
        </el-button>
        <el-button type="primary" icon="Plus" @click="addQuestion">
          添加题目
        </el-button>
        <el-button icon="Upload" @click="importQuestions">
          导入题目
        </el-button>
        <el-button icon="Download" @click="exportQuestions">
          导出题目
        </el-button>
      </div>
    </div>

    <!-- 考试信息概览 -->
    <el-card v-if="exam" class="exam-info-card">
      <template #header>
        <div class="card-header">
          <span>考试信息</span>
          <el-button link @click="goToExamDetail">查看详情</el-button>
        </div>
      </template>
      
      <el-descriptions :column="3" border>
        <el-descriptions-item label="考试标题">{{ exam.title }}</el-descriptions-item>
        <el-descriptions-item label="题目数量">{{ questions.length }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ totalScore }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(exam.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDate(exam.updatedAt) }}</el-descriptions-item>
        <el-descriptions-item label="创建者">{{ exam.createdBy }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 题目列表 -->
    <el-card class="questions-card">
      <template #header>
        <div class="card-header">
          <span>题目列表 ({{ questions.length }})</span>
          <el-button-group>
            <el-button type="primary" size="small" @click="sortByOrder">
              按顺序排列
            </el-button>
            <el-button size="small" @click="sortByType">
              按类型排列
            </el-button>
          </el-button-group>
        </div>
      </template>

      <div v-loading="loading">
        <el-table 
          :data="sortedQuestions" 
          row-key="id"
          @sort-change="handleSortChange"
        >
          <el-table-column type="index" label="序号" width="60" />
          
          <el-table-column prop="title" label="题目标题" min-width="200" show-overflow-tooltip />
          
          <el-table-column prop="questionType" label="题目类型" width="120">
            <template #default="{ row }">
              <el-tag :type="getQuestionTypeTag(row.questionType) as any">
                {{ getQuestionTypeText(row.questionType) }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="maxScore" label="满分" width="80" sortable />
          
          <el-table-column label="答案统计" width="120">
            <template #default="{ row }">
              <div class="answer-stats">
                <span>{{ row.totalAnswers || 0 }}</span>
                <span class="stats-separator">/</span>
                <span class="evaluated">{{ row.evaluatedAnswers || 0 }}</span>
              </div>
            </template>
          </el-table-column>
          
          <el-table-column prop="createdAt" label="创建时间" width="150" sortable>
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button-group>
                <el-button size="small" icon="View" @click="viewQuestion(row.id)">
                  查看
                </el-button>
                <el-button size="small" type="primary" icon="Edit" @click="editQuestion(row.id)">
                  编辑
                </el-button>
                <el-button size="small" type="success" icon="Document" @click="viewAnswers(row.id)">
                  答案
                </el-button>
                <el-button size="small" type="warning" icon="DataAnalysis" @click="evaluateQuestion(row.id)">
                  评估
                </el-button>
                <el-button size="small" type="danger" icon="Delete" @click="deleteQuestion(row.id)">
                  删除
                </el-button>
              </el-button-group>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="questions.length === 0" class="empty-state">
          <el-empty description="暂无题目">
            <el-button type="primary" @click="addQuestion">添加第一个题目</el-button>
          </el-empty>
        </div>
      </div>
    </el-card>

    <!-- 导入题目对话框 -->
    <el-dialog
      v-model="importDialogVisible"
      title="导入题目"
      width="500px"
    >
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :on-change="handleFileChange"
        :show-file-list="false"
        accept=".xlsx,.xls,.csv"
      >
        <el-button type="primary" icon="Upload">选择文件</el-button>
        <template #tip>
          <div class="el-upload__tip">
            支持 Excel (.xlsx, .xls) 和 CSV (.csv) 格式
          </div>
        </template>
      </el-upload>
      
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmImport" :loading="importing">
          确认导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { questionApi } from '@/api/question'
import { examApi } from '@/api/exam'
import type { QuestionResponse, ExamResponse } from '@/types/api'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const importing = ref(false)
const importDialogVisible = ref(false)
const uploadRef = ref()
const selectedFile = ref<File | null>(null)

const exam = ref<ExamResponse | null>(null)
const questions = ref<QuestionResponse[]>([])
const sortField = ref('')
const sortOrder = ref('')

const examId = computed(() => {
  const id = route.params.id
  return typeof id === 'string' ? parseInt(id, 10) : Number(id)
})

const totalScore = computed(() => {
  return questions.value.reduce((sum, q) => sum + (q.maxScore || 0), 0)
})

const sortedQuestions = computed(() => {
  if (!sortField.value) return questions.value
  
  const sorted = [...questions.value].sort((a, b) => {
    let aValue = a[sortField.value as keyof QuestionResponse]
    let bValue = b[sortField.value as keyof QuestionResponse]
    
    if (typeof aValue === 'string') aValue = aValue.toLowerCase()
    if (typeof bValue === 'string') bValue = bValue.toLowerCase()
    
    if (aValue != null && bValue != null) {
      if (aValue < bValue) return sortOrder.value === 'ascending' ? -1 : 1
      if (aValue > bValue) return sortOrder.value === 'ascending' ? 1 : -1
    }
    return 0
  })
  
  return sorted
})

const loadExam = async () => {
  try {
    exam.value = await examApi.getExam(examId.value)
  } catch (error) {
    console.error('Failed to load exam:', error)
    ElMessage.error('加载考试信息失败')
    router.back()
  }
}

const loadQuestions = async () => {
  try {
    loading.value = true
    questions.value = await questionApi.getQuestionsByExam(examId.value)
  } catch (error) {
    console.error('Failed to load questions:', error)
    ElMessage.error('加载题目列表失败')
  } finally {
    loading.value = false
  }
}

const generateQuestions = () => {
  router.push(`/exams/${examId.value}/generate-questions`)
}

const addQuestion = () => {
  router.push(`/exams/${examId.value}/questions/new`)
}

const viewQuestion = (questionId: number) => {
  router.push(`/questions/${questionId}`)
}

const editQuestion = (questionId: number) => {
  router.push(`/questions/${questionId}/edit`)
}

const viewAnswers = (questionId: number) => {
  router.push(`/questions/${questionId}/answers`)
}

const evaluateQuestion = (questionId: number) => {
  router.push(`/questions/${questionId}/evaluation`)
}

const deleteQuestion = async (questionId: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个题目吗？此操作不可恢复。',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await questionApi.deleteQuestion(questionId)
    ElMessage.success('题目删除成功')
    await loadQuestions()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to delete question:', error)
      ElMessage.error('删除题目失败')
    }
  }
}

const goToExamDetail = () => {
  router.push(`/exams/${examId.value}`)
}

const sortByOrder = () => {
  sortField.value = 'createdAt'
  sortOrder.value = 'ascending'
}

const sortByType = () => {
  sortField.value = 'questionType'
  sortOrder.value = 'ascending'
}

const handleSortChange = ({ prop, order }: { prop: string; order: string }) => {
  sortField.value = prop
  sortOrder.value = order
}

const importQuestions = () => {
  importDialogVisible.value = true
}

const exportQuestions = async () => {
  try {
    // TODO: 实现导出功能
    ElMessage.info('导出功能开发中...')
  } catch (error) {
    console.error('Failed to export questions:', error)
    ElMessage.error('导出失败')
  }
}

const handleFileChange = (file: UploadFile) => {
  selectedFile.value = file.raw || null
}

const confirmImport = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请选择要导入的文件')
    return
  }
  
  try {
    importing.value = true
    // TODO: 实现文件导入功能
    ElMessage.info('导入功能开发中...')
    importDialogVisible.value = false
    selectedFile.value = null
  } catch (error) {
    console.error('Failed to import questions:', error)
    ElMessage.error('导入失败')
  } finally {
    importing.value = false
  }
}

const getQuestionTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'TRUE_FALSE': '判断题',
    'SHORT_ANSWER': '简答题',
    'ESSAY': '论述题',
    'CODING': '编程题',
    'CASE_ANALYSIS': '案例分析',
    'CALCULATION': '计算题'
  }
  return typeMap[type] || type
}

const getQuestionTypeTag = (type: string) => {
  const tagMap: Record<string, string> = {
    'SINGLE_CHOICE': 'primary',
    'MULTIPLE_CHOICE': 'success',
    'TRUE_FALSE': 'warning',
    'SHORT_ANSWER': 'info',
    'ESSAY': 'danger',
    'CODING': '',
    'CASE_ANALYSIS': 'success',
    'CALCULATION': 'primary'
  }
  return tagMap[type] || ''
}

const formatDate = (dateString: string) => {
  if (!dateString) return '未知'
  return new Date(dateString).toLocaleString('zh-CN')
}

onMounted(() => {
  loadExam()
  loadQuestions()
})
</script>

<style scoped>
.question-management {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.exam-info-card,
.questions-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.answer-stats {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stats-separator {
  color: #c0c4cc;
}

.evaluated {
  color: #67c23a;
}

.empty-state {
  padding: 40px 0;
  text-align: center;
}

.el-upload__tip {
  margin-top: 8px;
  color: #606266;
  font-size: 12px;
}
</style>
