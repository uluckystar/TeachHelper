<template>
  <div class="question-library">
    <div class="page-header">
      <h1>题目库</h1>
      <p class="page-description">管理和浏览所有题目</p>
    </div>

    <!-- 搜索和筛选 -->
    <el-card class="search-card" shadow="hover">
      <!-- 搜索栏区域（始终显示） -->
      <div class="search-main">
        <div class="search-input-wrapper">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索题目标题、内容、学科、年级..."
            clearable
            size="large"
            @keyup.enter="searchQuestions"
            @clear="searchQuestions"
            class="search-input"
          >
            <template #prefix>
              <el-icon class="search-icon"><Search /></el-icon>
            </template>
            <template #append>
              <el-button type="primary" @click="searchQuestions" class="search-btn">
                <el-icon><Search /></el-icon>
                搜索
              </el-button>
            </template>
          </el-input>
        </div>
        
        <!-- 快速筛选标签 -->
        <div class="quick-filters">
          <div class="filter-group">
            <span class="filter-group-label">快速筛选：</span>
            <el-tag 
              type="info"
              effect="plain"
              class="filter-tag"
              @click="searchForm.source = ''; searchQuestions()"
            >
              全部题目
            </el-tag>
            <el-tag 
              type="primary"
              effect="plain"
              class="filter-tag"
              @click="searchForm.source = 'SELF_CREATED'; searchQuestions()"
            >
              自创题目
            </el-tag>
            <el-tag 
              type="success"
              effect="plain"
              class="filter-tag"
              @click="searchForm.source = 'INTERNET'; searchQuestions()"
            >
              互联网题目
            </el-tag>
            <el-tag 
              type="warning"
              effect="plain"
              class="filter-tag"
              @click="searchForm.source = 'AI_GENERATED'; searchQuestions()"
            >
              AI生成
            </el-tag>
            <el-tag 
              type="danger"
              effect="plain"
              class="filter-tag"
              @click="searchForm.source = 'AI_ORGANIZED'; searchQuestions()"
            >
              AI整理
            </el-tag>
          </div>
          
          <!-- 高级筛选切换 -->
          <div class="advanced-filter-toggle">
            <el-button 
              text 
              type="primary" 
              @click="toggleFilters"
              class="toggle-btn"
            >
              <el-icon class="toggle-icon" :class="{ 'rotated': filtersExpanded }">
                <ArrowDown />
              </el-icon>
              {{ filtersExpanded ? '收起高级筛选' : '展开高级筛选' }}
            </el-button>
          </div>
        </div>
      </div>

      <!-- 高级筛选区域（可折叠） -->
      <el-collapse-transition>
        <div v-show="filtersExpanded" class="advanced-filters">
          <div class="filters-header">
            <el-icon><Filter /></el-icon>
            <span>高级筛选</span>
          </div>
          
          <!-- 筛选条件网格 -->
          <div class="filters-grid">
            <div class="filter-item">
              <label class="filter-label">
                <el-icon><Collection /></el-icon>
                题目类型
              </label>
              <el-select 
                v-model="searchForm.questionType" 
                placeholder="选择题目类型" 
                clearable
                class="filter-select"
              >
                <el-option label="单选题" value="SINGLE_CHOICE" />
                <el-option label="多选题" value="MULTIPLE_CHOICE" />
                <el-option label="判断题" value="TRUE_FALSE" />
                <el-option label="填空题" value="FILL_BLANK" />
                <el-option label="简答题" value="SHORT_ANSWER" />
                <el-option label="论述题" value="ESSAY" />
                <el-option label="编程题" value="CODING" />
                <el-option label="案例分析题" value="CASE_ANALYSIS" />
                <el-option label="计算题" value="CALCULATION" />
              </el-select>
            </div>
            
            <div class="filter-item">
              <label class="filter-label">
                <el-icon><DataLine /></el-icon>
                题目来源
              </label>
              <el-select 
                v-model="searchForm.source" 
                placeholder="选择题目来源" 
                clearable
                class="filter-select"
              >
                <el-option label="全部" value="" />
                <el-option label="自创" value="SELF_CREATED">
                  <template #default>
                    <span class="option-content">
                      <el-icon><Edit /></el-icon>
                      自创
                    </span>
                  </template>
                </el-option>
                <el-option label="互联网" value="INTERNET">
                  <template #default>
                    <span class="option-content">
                      <el-icon><Link /></el-icon>
                      互联网
                    </span>
                  </template>
                </el-option>
                <el-option label="AI生成" value="AI_GENERATED">
                  <template #default>
                    <span class="option-content">
                      <el-icon><MagicStick /></el-icon>
                      AI生成
                    </span>
                  </template>
                </el-option>
                <el-option label="AI整理" value="AI_ORGANIZED">
                  <template #default>
                    <span class="option-content">
                      <el-icon><FolderOpened /></el-icon>
                      AI整理
                    </span>
                  </template>
                </el-option>
              </el-select>
            </div>

            <div class="filter-item">
              <label class="filter-label">
                <el-icon><Reading /></el-icon>
                学科
              </label>
              <el-select 
                v-model="searchForm.subject" 
                placeholder="选择学科" 
                clearable
                class="filter-select"
              >
                <el-option 
                  v-for="subject in subjects" 
                  :key="subject" 
                  :label="subject" 
                  :value="subject"
                />
              </el-select>
            </div>

            <div class="filter-item">
              <label class="filter-label">
                <el-icon><School /></el-icon>
                年级
              </label>
              <el-select 
                v-model="searchForm.gradeLevel" 
                placeholder="选择年级" 
                clearable
                class="filter-select"
              >
                <el-option 
                  v-for="grade in gradeLevels" 
                  :key="grade" 
                  :label="grade" 
                  :value="grade"
                />
              </el-select>
            </div>

            <div class="filter-item">
              <label class="filter-label">
                <el-icon><Document /></el-icon>
                所属考试
              </label>
              <el-select 
                v-model="searchForm.examId" 
                placeholder="选择考试" 
                clearable
                class="filter-select"
              >
                <el-option 
                  v-for="exam in exams" 
                  :key="exam.id" 
                  :label="exam.title" 
                  :value="exam.id"
                />
              </el-select>
            </div>
          </div>

          <!-- 筛选操作按钮 -->
          <div class="filter-actions">
            <div class="filter-actions-left">
              <el-button type="primary" icon="Search" @click="searchQuestions" class="action-btn primary">
                应用筛选
              </el-button>
              <el-button icon="Refresh" @click="resetSearch" class="action-btn">
                重置筛选
              </el-button>
            </div>
            <div class="filter-actions-right">
              <el-button text type="info" @click="clearAllFilters" class="clear-btn">
                <el-icon><Delete /></el-icon>
                清空所有筛选条件
              </el-button>
            </div>
          </div>
        </div>
      </el-collapse-transition>

      <!-- 快捷操作区域 -->
      <div class="quick-actions-section">
        <div class="actions-header">
          <span class="actions-label">快捷操作</span>
        </div>
        <div class="quick-actions">
          <el-button type="info" icon="FolderOpened" @click="viewQuestionBanks" class="quick-action-btn">
            题目库管理
          </el-button>
          <el-button type="success" icon="MagicStick" @click="generateQuestions" class="quick-action-btn">
            AI生成题目
          </el-button>
          <el-button type="primary" icon="Plus" @click="createQuestion" class="quick-action-btn">
            新建题目
          </el-button>
        </div>
      </div>
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
        <el-table-column prop="sourceInfo" label="题目来源" width="200">
          <template #default="{ row }">
            <div class="source-info">
              <el-tag :type="getSourceTypeTag(row.sourceType) as any" size="small">
                {{ getSourceTypeName(row.sourceType, row) }}
              </el-tag>
              <el-tag 
                v-if="row.sourceType === 'AI_ORGANIZED' && !row.isConfirmed" 
                type="danger" 
                size="small" 
                style="margin-left: 4px;"
              >
                待确认
              </el-tag>
              <el-tag 
                v-if="row.sourceType === 'AI_ORGANIZED' && row.isConfirmed" 
                type="success" 
                size="small" 
                style="margin-left: 4px;"
              >
                已确认
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="maxScore" label="满分" width="80" />
        <el-table-column prop="answerCount" label="答案数" width="80" />
        <el-table-column prop="createdAt" label="创建时间" width="150">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320">
          <template #default="{ row }">
            <el-button-group>
              <el-button size="small" @click="viewQuestion(row.id)">
                查看
              </el-button>
              <el-button size="small" type="primary" @click="editQuestion(row.id)">
                编辑
              </el-button>
              <el-button 
                v-if="row.sourceType === 'AI_ORGANIZED' && !row.isConfirmed"
                size="small" 
                type="success" 
                @click="confirmAIOrganizedQuestion(row.id)"
              >
                确认整理
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
                <div class="question-source">
                  <div class="source-tags">
                    <el-tag :type="getSourceTypeTag((question as any).sourceType) as any" size="small">
                      {{ getSourceTypeName((question as any).sourceType, question) }}
                    </el-tag>
                    <el-tag 
                      v-if="(question as any).sourceType === 'AI_ORGANIZED' && !(question as any).isConfirmed" 
                      type="danger" 
                      size="small" 
                      style="margin-left: 4px;"
                    >
                      待确认
                    </el-tag>
                    <el-tag 
                      v-if="(question as any).sourceType === 'AI_ORGANIZED' && (question as any).isConfirmed" 
                      type="success" 
                      size="small" 
                      style="margin-left: 4px;"
                    >
                      已确认
                    </el-tag>
                  </div>
                  <span class="max-score">{{ question.maxScore }}分</span>
                </div>
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
import { Search, List, Grid, Plus, Refresh, MagicStick, FolderOpened, ArrowUp, ArrowDown, Filter, Collection, DataLine, Edit, Link, Reading, School, Document, Delete } from '@element-plus/icons-vue'
import { questionApi } from '@/api/question'
import { questionBankApi } from '@/api/questionBank'
import { knowledgeBaseApi } from '@/api/knowledge'
import { examApi } from '@/api/exam'
import { subjectApi, gradeLevelApi } from '@/api/metadata'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { QuestionResponse, ExamResponse, QuestionBank } from '@/types/api'
import type { KnowledgeBase } from '@/api/knowledge'
import RubricManagementDialog from '@/components/evaluation/RubricManagementDialog.vue'

const router = useRouter()

const loading = ref(false)
const viewMode = ref<'list' | 'card'>('list')
const questions = ref<QuestionResponse[]>([])
const exams = ref<ExamResponse[]>([])
const questionBanks = ref<QuestionBank[]>([])
const knowledgeBases = ref<KnowledgeBase[]>([])
const subjects = ref<string[]>([])
const gradeLevels = ref<string[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

// 搜索筛选相关状态
const filtersExpanded = ref(false)

// 快速筛选相关数据
const quickFilters = ref([
  { key: 'all', label: '全部题目', value: '' },
  { key: 'selfCreated', label: '自创题目', value: 'SELF_CREATED' },
  { key: 'internet', label: '互联网题目', value: 'INTERNET' },
  { key: 'aiGenerated', label: 'AI生成', value: 'AI_GENERATED' },
  { key: 'aiOrganized', label: 'AI整理', value: 'AI_ORGANIZED' },
  { key: 'pending', label: '待确认题目', value: 'AI_ORGANIZED_PENDING' }
})

// 评分标准管理相关状态
const rubricDialogVisible = ref(false)
const selectedQuestionId = ref<number | null>(null)

const searchForm = ref({
  keyword: '',
  questionType: '',
  subject: '',
  gradeLevel: '',
  examId: '',
  source: ''
})

// 切换筛选区域展开/收起
const toggleFilters = () => {
  filtersExpanded.value = !filtersExpanded.value
}

// 清空所有筛选条件
const clearAllFilters = () => {
  searchForm.value = {
    keyword: '',
    questionType: '',
    subject: '',
    gradeLevel: '',
    examId: '',
    source: ''
  }
  searchQuestions()
}

const loadQuestions = async () => {
  loading.value = true
  try {
    // 根据新的来源分类加载题目
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
      keyword: searchForm.value.keyword || undefined,
      questionType: searchForm.value.questionType || undefined,
      subject: searchForm.value.subject || undefined,
      gradeLevel: searchForm.value.gradeLevel || undefined,
      examId: searchForm.value.examId ? parseInt(searchForm.value.examId) : undefined,
      source: (searchForm.value.source as 'SELF_CREATED' | 'INTERNET' | 'AI_GENERATED') || undefined
    }
    
    const response = await questionApi.getQuestionsWithPagination(params)
    questions.value = response.content || []
    total.value = response.totalElements || 0
  } catch (error) {
    console.error('加载题目失败:', error)
    ElMessage.error('加载题目失败')
  } finally {
    loading.value = false
  }
}

const loadQuestionsFromQuestionBanks = async () => {
  try {
    let allQuestions: QuestionResponse[] = []
    
    for (const bank of questionBanks.value) {
      try {
        const bankQuestions = await questionBankApi.getQuestionBankQuestions(bank.id, 0, 100)
        const questions = bankQuestions.content?.map((q: any) => ({
          ...q,
          sourceType: 'QUESTION_BANK',
          sourceInfo: bank.name,
          questionBankName: bank.name
        })) || []
        allQuestions.push(...questions)
      } catch (error) {
        console.warn(`获取题目库 ${bank.name} 的题目失败:`, error)
      }
    }

    // 应用筛选
    if (searchForm.value.keyword) {
      const keyword = searchForm.value.keyword.toLowerCase()
      allQuestions = allQuestions.filter(q => 
        q.title?.toLowerCase().includes(keyword) || 
        q.content?.toLowerCase().includes(keyword)
      )
    }

    if (searchForm.value.questionType) {
      allQuestions = allQuestions.filter(q => q.questionType === searchForm.value.questionType)
    }

    // 分页
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    questions.value = allQuestions.slice(start, end)
    total.value = allQuestions.length
  } catch (error) {
    console.error('从题目库加载题目失败:', error)
    ElMessage.error('从题目库加载题目失败')
  }
}

const loadQuestionsFromKnowledgeBases = async () => {
  try {
    let allQuestions: QuestionResponse[] = []
    
    for (const kb of knowledgeBases.value) {
      try {
        const kbQuestions = await knowledgeBaseApi.getKnowledgeBaseQuestions(kb.id, 0, 100)
        const questions = kbQuestions.content?.map((q: any) => ({
          ...q,
          sourceType: 'KNOWLEDGE_BASE',
          sourceInfo: kb.name,
          knowledgeBaseName: kb.name
        })) || []
        allQuestions.push(...questions)
      } catch (error) {
        console.warn(`获取知识库 ${kb.name} 的题目失败:`, error)
      }
    }

    // 应用筛选
    if (searchForm.value.keyword) {
      const keyword = searchForm.value.keyword.toLowerCase()
      allQuestions = allQuestions.filter(q => 
        q.title?.toLowerCase().includes(keyword) || 
        q.content?.toLowerCase().includes(keyword)
      )
    }

    if (searchForm.value.questionType) {
      allQuestions = allQuestions.filter(q => q.questionType === searchForm.value.questionType)
    }

    // 分页
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    questions.value = allQuestions.slice(start, end)
    total.value = allQuestions.length
  } catch (error) {
    console.error('从知识库加载题目失败:', error)
    ElMessage.error('从知识库加载题目失败')
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

const loadQuestionBanks = async () => {
  try {
    const response = await questionBankApi.getAccessibleQuestionBanks(0, 100)
    questionBanks.value = response.content || []
  } catch (error) {
    console.error('加载题目库列表失败:', error)
  }
}

const loadKnowledgeBases = async () => {
  try {
    const response = await knowledgeBaseApi.getKnowledgeBases({ page: 0, size: 100 })
    knowledgeBases.value = response.content || []
  } catch (error) {
    console.error('加载知识库列表失败:', error)
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
    examId: '',
    source: ''
  }
  searchQuestions()
}

const createQuestion = () => {
  router.push('/questions/create')
}

const generateQuestions = () => {
  router.push('/questions/generate')
}

const viewQuestionBanks = () => {
  router.push('/question-banks')
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

// 确认AI整理的题目
const confirmAIOrganizedQuestion = async (questionId: number) => {
  try {
    await ElMessageBox.confirm(
      '确认这个AI整理的题目内容正确吗？确认后将标记为已验证。',
      '确认AI整理',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    // 调用API确认题目
    await questionApi.confirmAIOrganizedQuestion(questionId)
    ElMessage.success('题目已确认')
    loadQuestions() // 重新加载题目列表
  } catch (error) {
    if (error !== 'cancel') {
      console.error('确认题目失败:', error)
      ElMessage.error('确认题目失败')
    }
  }
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  loadQuestions()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  loadQuestions()
}

// 快速筛选相关方法
const isFilterActive = (filter: any) => {
  if (filter.key === 'all') {
    return !searchForm.value.source
  }
  if (filter.key === 'pending') {
    return searchForm.value.source === 'AI_ORGANIZED' // 这里可以根据具体需求调整
  }
  return searchForm.value.source === filter.value
}

const toggleQuickFilter = (filter: any) => {
  if (filter.key === 'all') {
    searchForm.value.source = ''
  } else if (filter.key === 'pending') {
    searchForm.value.source = 'AI_ORGANIZED'
    // 这里可以添加额外的逻辑来筛选待确认的题目
  } else {
    searchForm.value.source = filter.value
  }
  searchQuestions()
}

const getQuestionTypeTag = (type: string) => {
  const map: Record<string, string> = {
    'SINGLE_CHOICE': 'primary',
    'MULTIPLE_CHOICE': 'success',
    'TRUE_FALSE': 'info',
    'FILL_BLANK': 'warning',
    'SHORT_ANSWER': 'info',
    'ESSAY': 'danger',
    'CODING': 'primary',
    'CASE_ANALYSIS': 'warning',
    'CALCULATION': 'success'
  }
  return map[type] || 'info'
}

const getQuestionTypeText = (type: string) => {
  const map: Record<string, string> = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'TRUE_FALSE': '判断题',
    'FILL_BLANK': '填空题',
    'SHORT_ANSWER': '简答题',
    'ESSAY': '论述题',
    'CODING': '编程题',
    'CASE_ANALYSIS': '案例分析题',
    'CALCULATION': '计算题'
  }
  return map[type] || type
}

const getSourceTypeTag = (sourceType: string) => {
  const map: Record<string, string> = {
    'SELF_CREATED': 'primary',
    'INTERNET': 'success', 
    'AI_GENERATED': 'warning',
    'AI_ORGANIZED': 'info'
  }
  return map[sourceType] || 'default'
}

const getSourceTypeName = (sourceType: string, row: any) => {
  if (!sourceType) {
    // 如果没有明确的来源类型，根据其他字段推断
    if (row.aiGenerationPrompt || row.keywords?.includes('AI')) {
      return 'AI生成'
    }
    if (row.questionBankName || row.examTitle) {
      return '自创'
    }
    return '自创' // 默认为自创
  }
  
  const map: Record<string, string> = {
    'SELF_CREATED': '自创',
    'INTERNET': '互联网',
    'AI_GENERATED': 'AI生成',
    'AI_ORGANIZED': 'AI整理'
  }
  return map[sourceType] || '自创'
}

const formatDate = (dateString: string) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

onMounted(() => {
  loadQuestions()
  loadExams()
  loadQuestionBanks()
  loadKnowledgeBases() 
  loadSubjects()
  loadGradeLevels()
})
</script>

<style scoped lang="scss">
.question-library {
  padding: 20px;
  background-color: #f8f9fa;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 24px;
  text-align: center;
  
  h1 {
    margin: 0 0 8px 0;
    font-size: 32px;
    font-weight: 700;
    background: linear-gradient(135deg, #409eff, #67c23a);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
  
  .page-description {
    margin: 0;
    color: #666;
    font-size: 16px;
  }
}

.search-card {
  margin-bottom: 24px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  
  &:hover {
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  }
}

.search-main {
  padding: 24px;
  background: linear-gradient(135deg, #f8fbff 0%, #f0f9ff 100%);
  border-bottom: 1px solid #e4e7ed;
}

.search-input-wrapper {
  margin-bottom: 20px;
  
  .search-input {
    :deep(.el-input__wrapper) {
      border-radius: 24px;
      padding: 12px 20px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      border: 2px solid transparent;
      transition: all 0.3s ease;
      
      &:hover, &.is-focus {
        border-color: #409eff;
        box-shadow: 0 4px 16px rgba(64, 158, 255, 0.2);
      }
    }
    
    :deep(.el-input__inner) {
      font-size: 16px;
      font-weight: 400;
    }
  }
  
  .search-icon {
    color: #409eff;
    font-size: 18px;
  }
  
  .search-btn {
    border-radius: 0 20px 20px 0;
    padding: 12px 24px;
    font-weight: 600;
    background: linear-gradient(135deg, #409eff, #66b1ff);
    border: none;
    box-shadow: none;
    
    &:hover {
      background: linear-gradient(135deg, #337ecc, #5a9ff8);
      transform: translateY(-1px);
    }
  }
}

.quick-filters {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-group {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  
  .filter-group-label {
    font-weight: 600;
    color: #606266;
    font-size: 14px;
  }
  
  .filter-tag {
    cursor: pointer;
    border-radius: 16px;
    padding: 6px 12px;
    font-size: 12px;
    font-weight: 500;
    transition: all 0.3s ease;
    border: 1px solid #e4e7ed;
    
    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }
    
    &.el-tag--primary {
      background: linear-gradient(135deg, #409eff, #66b1ff);
      border-color: #409eff;
      color: white;
      box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
    }
  }
}

.advanced-filter-toggle {
  .toggle-btn {
    font-size: 14px;
    font-weight: 500;
    border-radius: 20px;
    padding: 8px 16px;
    background: rgba(64, 158, 255, 0.1);
    border: 1px solid rgba(64, 158, 255, 0.2);
    transition: all 0.3s ease;
    
    &:hover {
      background: rgba(64, 158, 255, 0.15);
      transform: translateY(-1px);
    }
    
    .toggle-icon {
      transition: transform 0.3s ease;
      
      &.rotated {
        transform: rotate(180deg);
      }
    }
  }
}

.advanced-filters {
  padding: 24px;
  background: #fafbfc;
  border-top: 1px solid #e4e7ed;
}

.filters-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  color: #606266;
  font-weight: 600;
  font-size: 16px;
  
  .el-icon {
    color: #409eff;
  }
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.filter-item {
  .filter-label {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 8px;
    font-weight: 600;
    color: #606266;
    font-size: 14px;
    
    .el-icon {
      color: #409eff;
      font-size: 16px;
    }
  }
  
  .filter-select {
    width: 100%;
    
    :deep(.el-input__wrapper) {
      border-radius: 8px;
      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
      border: 1px solid #e4e7ed;
      transition: all 0.3s ease;
      
      &:hover, &.is-focus {
        border-color: #409eff;
        box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
      }
    }
  }
}

.option-content {
  display: flex;
  align-items: center;
  gap: 6px;
  
  .el-icon {
    font-size: 14px;
  }
}

.filter-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
}

.filter-actions-left {
  display: flex;
  gap: 12px;
  
  .action-btn {
    border-radius: 8px;
    padding: 10px 20px;
    font-weight: 600;
    transition: all 0.3s ease;
    
    &:hover {
      transform: translateY(-1px);
    }
    
    &.primary {
      background: linear-gradient(135deg, #409eff, #66b1ff);
      border: none;
      box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
      
      &:hover {
        box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
      }
    }
  }
}

.filter-actions-right {
  .clear-btn {
    color: #f56c6c;
    border-radius: 20px;
    padding: 6px 12px;
    font-size: 13px;
    
    &:hover {
      background: rgba(245, 108, 108, 0.1);
    }
  }
}

.quick-actions-section {
  padding: 20px 24px;
  background: linear-gradient(135deg, #f0f9ff 0%, #f8fbff 100%);
  border-top: 1px solid #e4e7ed;
}

.actions-header {
  margin-bottom: 12px;
  
  .actions-label {
    font-weight: 600;
    color: #606266;
    font-size: 14px;
  }
}

.quick-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  
  .quick-action-btn {
    border-radius: 8px;
    padding: 10px 16px;
    font-weight: 500;
    transition: all 0.3s ease;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }
    
    &.el-button--info {
      background: linear-gradient(135deg, #909399, #a6a9ad);
      border: none;
      color: white;
    }
    
    &.el-button--success {
      background: linear-gradient(135deg, #67c23a, #85ce61);
      border: none;
      color: white;
    }
    
    &.el-button--primary {
      background: linear-gradient(135deg, #409eff, #66b1ff);
      border: none;
      color: white;
    }
  }
}

.questions-card {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #303133;
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

.question-source {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.source-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.source-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
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

// 响应式设计
@media (max-width: 768px) {
  .question-library {
    padding: 12px;
  }
  
  .filters-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .quick-filters {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .filter-group {
    justify-content: center;
  }
  
  .advanced-filter-toggle {
    align-self: center;
  }
  
  .filter-actions {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .filter-actions-left,
  .filter-actions-right {
    justify-content: center;
  }
  
  .quick-actions {
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .page-header h1 {
    font-size: 24px;
  }
  
  .search-input-wrapper {
    .search-input {
      :deep(.el-input-group__append) {
        .search-btn {
          padding: 8px 16px;
          font-size: 14px;
        }
      }
    }
  }
  
  .filter-tag {
    font-size: 11px;
    padding: 4px 8px;
  }
  
  .quick-action-btn {
    font-size: 13px;
    padding: 8px 12px;
  }
}
</style>
