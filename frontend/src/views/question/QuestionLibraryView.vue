<template>
  <div class="question-library">
    <div class="page-header">
      <h1>题目库</h1>
      <p class="page-description">管理和浏览所有题目</p>
    </div>

    <!-- 搜索和筛选 -->
    <el-card class="search-card">
      <el-row :gutter="16">
        <el-col :span="6">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索题目标题或内容"
            @keyup.enter="searchQuestions"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="4">
          <el-select v-model="searchForm.questionType" placeholder="题目类型" clearable>
            <el-option label="选择题" value="SINGLE_CHOICE" />
            <el-option label="多选题" value="MULTIPLE_CHOICE" />
            <el-option label="填空题" value="FILL_BLANK" />
            <el-option label="简答题" value="SHORT_ANSWER" />
            <el-option label="论述题" value="ESSAY" />
          </el-select>
        </el-col>
        <el-col :span="3">
          <el-select v-model="searchForm.subject" placeholder="学科" clearable>
            <el-option 
              v-for="subject in subjects" 
              :key="subject" 
              :label="subject" 
              :value="subject"
            />
          </el-select>
        </el-col>
        <el-col :span="3">
          <el-select v-model="searchForm.gradeLevel" placeholder="年级" clearable>
            <el-option 
              v-for="grade in gradeLevels" 
              :key="grade" 
              :label="grade" 
              :value="grade"
            />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="searchForm.examId" placeholder="所属考试" clearable>
            <el-option 
              v-for="exam in exams" 
              :key="exam.id" 
              :label="exam.title" 
              :value="exam.id"
            />
          </el-select>
        </el-col>
        <el-col :span="10">
          <div class="action-buttons">
            <el-button type="primary" icon="Search" @click="searchQuestions">
              搜索
            </el-button>
            <el-button icon="Refresh" @click="resetSearch">
              重置
            </el-button>
            <el-divider direction="vertical" />
            <el-button type="success" icon="MagicStick" @click="generateQuestions">
              AI生成题目
            </el-button>
            <el-button type="primary" icon="Plus" @click="createQuestion">
              新建题目
            </el-button>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 题目列表 -->
    <el-card class="questions-card">
      <template #header>
        <div class="card-header">
          <span>题目列表 ({{ total }})</span>
          <el-button-group>
            <el-button 
              :type="viewMode === 'list' ? 'primary' : 'default'"
              icon="List"
              @click="viewMode = 'list'"
            >
              列表视图
            </el-button>
            <el-button 
              :type="viewMode === 'card' ? 'primary' : 'default'"
              icon="Grid"
              @click="viewMode = 'card'"
            >
              卡片视图
            </el-button>
          </el-button-group>
        </div>
      </template>

      <!-- 列表视图 -->
      <el-table 
        v-if="viewMode === 'list'"
        :data="questions" 
        v-loading="loading"
      >
        <el-table-column prop="title" label="题目标题" />
        <el-table-column prop="questionType" label="题目类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getQuestionTypeTag(row.questionType) as any">
              {{ getQuestionTypeText(row.questionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="examTitle" label="所属考试" width="150" />
        <el-table-column prop="maxScore" label="满分" width="80" />
        <el-table-column prop="answerCount" label="答案数" width="80" />
        <el-table-column prop="createdAt" label="创建时间" width="150">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button-group>
              <el-button size="small" @click="viewQuestion(row.id)">
                查看
              </el-button>
              <el-button size="small" type="primary" @click="editQuestion(row.id)">
                编辑
              </el-button>
              <el-button size="small" type="success" @click="manageRubric(row.id)">
                评分标准
              </el-button>
              <el-button size="small" type="danger" @click="deleteQuestion(row.id)">
                删除
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- 卡片视图 -->
      <div v-else class="card-view" v-loading="loading">
        <el-row :gutter="16">
          <el-col :span="8" v-for="question in questions" :key="question.id">
            <el-card class="question-card" @click="viewQuestion(question.id.toString())">
              <div class="question-header">
                <el-tag :type="getQuestionTypeTag(question.questionType) as any" size="small">
                  {{ getQuestionTypeText(question.questionType) }}
                </el-tag>
                <span class="max-score">{{ question.maxScore }}分</span>
              </div>
              <h4 class="question-title">{{ question.title }}</h4>
              <p class="question-content">{{ question.content }}</p>
              <div class="question-meta">
                <span>{{ question.examTitle }}</span>
                <span>{{ (question as any).answerCount || 0 }} 个答案</span>
              </div>
              <div class="question-actions" @click.stop>
                <el-button size="small" type="primary" @click="editQuestion(question.id.toString())">
                  编辑
                </el-button>
                <el-button size="small" type="success" @click="manageRubric(question.id)">
                  评分标准
                </el-button>
                <el-button size="small" type="danger" @click="deleteQuestion(question.id.toString())">
                  删除
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 评分标准管理对话框 -->
    <RubricManagementDialog
      v-model="rubricDialogVisible"
      :question-id="selectedQuestionId"
      @refresh="loadQuestions"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, List, Grid, Plus, Refresh, MagicStick } from '@element-plus/icons-vue'
import { questionApi } from '@/api/question'
import { examApi } from '@/api/exam'
import { subjectApi, gradeLevelApi } from '@/api/metadata'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { QuestionResponse, ExamResponse } from '@/types/api'
import RubricManagementDialog from '@/components/evaluation/RubricManagementDialog.vue'

const router = useRouter()

const loading = ref(false)
const viewMode = ref<'list' | 'card'>('list')
const questions = ref<QuestionResponse[]>([])
const exams = ref<ExamResponse[]>([])
const subjects = ref<string[]>([])
const gradeLevels = ref<string[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

// 评分标准管理相关状态
const rubricDialogVisible = ref(false)
const selectedQuestionId = ref<number | null>(null)

const searchForm = ref({
  keyword: '',
  questionType: '',
  subject: '',
  gradeLevel: '',
  examId: ''
})

const loadQuestions = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
      ...searchForm.value
    }
    const response = await questionApi.getQuestions(params)
    questions.value = response.data?.content || []
    total.value = response.data?.totalElements || 0
  } catch (error) {
    console.error('加载题目失败:', error)
    ElMessage.error('加载题目失败')
  } finally {
    loading.value = false
  }
}

const loadExams = async () => {
  try {
    const response = await examApi.getExams()
    exams.value = response.data || []
  } catch (error) {
    console.error('加载考试列表失败:', error)
  }
}

const loadSubjects = async () => {
  try {
    const response = await subjectApi.getAllSubjectNames()
    subjects.value = response || []
  } catch (error) {
    console.error('加载学科列表失败:', error)
    // 如果新API失败，使用默认学科列表
    subjects.value = ['数学', '语文', '英语', '物理', '化学', '生物', '历史', '地理', '政治']
  }
}

const loadGradeLevels = async () => {
  try {
    const response = await gradeLevelApi.getAllGradeLevelNames()
    gradeLevels.value = response || []
  } catch (error) {
    console.error('加载年级列表失败:', error)
    // 如果新API失败，使用默认年级列表
    gradeLevels.value = ['小学一年级', '小学二年级', '小学三年级', '小学四年级', '小学五年级', '小学六年级',
                        '初一', '初二', '初三', '高一', '高二', '高三']
  }
}

const searchQuestions = () => {
  currentPage.value = 1
  loadQuestions()
}

const resetSearch = () => {
  searchForm.value = {
    keyword: '',
    questionType: '',
    subject: '',
    gradeLevel: '',
    examId: ''
  }
  searchQuestions()
}

const createQuestion = () => {
  router.push('/questions/create')
}

const generateQuestions = () => {
  router.push('/questions/generate')
}

const viewQuestion = (questionId: string) => {
  router.push(`/questions/${questionId}`)
}

const editQuestion = (questionId: string) => {
  router.push(`/questions/${questionId}/edit`)
}

const deleteQuestion = async (questionId: string) => {
  try {
    await ElMessageBox.confirm('确定要删除此题目吗？', '确认删除', {
      type: 'warning'
    })
    
    await questionApi.deleteQuestion(parseInt(questionId))
    ElMessage.success('题目删除成功')
    loadQuestions()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除题目失败:', error)
      ElMessage.error('删除题目失败')
    }
  }
}

const manageRubric = (questionId: number) => {
  selectedQuestionId.value = questionId
  rubricDialogVisible.value = true
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  loadQuestions()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  loadQuestions()
}

const getQuestionTypeTag = (type: string) => {
  const map: Record<string, string> = {
    'SINGLE_CHOICE': 'primary',
    'MULTIPLE_CHOICE': 'success',
    'FILL_BLANK': 'warning',
    'SHORT_ANSWER': 'info',
    'ESSAY': 'danger'
  }
  return map[type] || 'info'
}

const getQuestionTypeText = (type: string) => {
  const map: Record<string, string> = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'FILL_BLANK': '填空题',
    'SHORT_ANSWER': '简答题',
    'ESSAY': '论述题'
  }
  return map[type] || type
}

const formatDate = (dateString: string) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

onMounted(() => {
  loadQuestions()
  loadExams()
  loadSubjects()
  loadGradeLevels()
})
</script>

<style scoped>
.question-library {
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

.search-card,
.questions-card {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-view {
  min-height: 200px;
}

.question-card {
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.question-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.max-score {
  font-weight: 600;
  color: #e6a23c;
}

.question-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
}

.question-content {
  margin: 0 0 12px 0;
  color: #666;
  font-size: 14px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.question-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 12px;
  color: #999;
}

.question-actions {
  display: flex;
  gap: 8px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-buttons .el-divider {
  margin: 0 4px;
  height: 20px;
}

.action-buttons .el-button {
  border-radius: 6px;
  font-weight: 500;
  transition: all 0.3s;
}

.action-buttons .el-button--success {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.3);
}

.action-buttons .el-button--success:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.4);
}

.action-buttons .el-button--primary {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

.action-buttons .el-button--primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
}
</style>
