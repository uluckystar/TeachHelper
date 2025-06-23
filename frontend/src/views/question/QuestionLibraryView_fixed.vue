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
          <div class="search-box">
            <div class="search-icon-wrapper">
              <el-icon class="search-icon"><Search /></el-icon>
            </div>
            <el-input
              v-model="searchForm.keyword"
              placeholder="搜索题目标题、内容..."
              clearable
              size="large"
              @keyup.enter="searchQuestions"
              @clear="searchQuestions"
              class="search-input"
            />
            <el-button 
              type="primary" 
              @click="searchQuestions" 
              class="search-btn"
              :loading="loading"
            >
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
          </div>
        </div>
        
        <!-- 快速筛选标签 -->
        <div class="quick-filters">
          <div class="filter-group">
            <span class="filter-group-label">快速筛选：</span>
            <el-tag 
              type="info"
              effect="plain"
              class="filter-tag"
              @click="handleQuickFilter('')"
            >
              全部题目
            </el-tag>
            <el-tag 
              type="primary"
              effect="plain"
              class="filter-tag"
              @click="handleQuickFilter('SELF_CREATED')"
            >
              自创题目
            </el-tag>
            <el-tag 
              type="success"
              effect="plain"
              class="filter-tag"
              @click="handleQuickFilter('INTERNET')"
            >
              互联网题目
            </el-tag>
            <el-tag 
              type="warning"
              effect="plain"
              class="filter-tag"
              @click="handleQuickFilter('AI_GENERATED')"
            >
              AI生成
            </el-tag>
            <el-tag 
              type="danger"
              effect="plain"
              class="filter-tag"
              @click="handleQuickFilter('AI_ORGANIZED')"
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
                  :value="exam.id.toString()"
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
      <div class="quick-actions">
        <div class="view-controls">
          <el-radio-group v-model="viewMode" class="view-mode-switcher">
            <el-radio-button label="list">
              <el-icon><List /></el-icon>
              列表视图
            </el-radio-button>
            <el-radio-button label="card">
              <el-icon><Grid /></el-icon>
              卡片视图
            </el-radio-button>
          </el-radio-group>
        </div>
        
        <div class="action-buttons">
          <el-button type="success" icon="Plus" @click="createQuestion">
            创建题目
          </el-button>
          <el-button icon="MagicStick" @click="generateWithAI">
            AI生成题目
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 题目列表 -->
    <el-card class="content-card" shadow="never">
      <div v-loading="loading" class="questions-container">
        <el-row v-if="viewMode === 'card'" :gutter="16">
          <el-col 
            v-for="question in questions" 
            :key="question.id" 
            :xs="24" 
            :sm="12" 
            :md="8" 
            :lg="6"
            class="question-card-col"
          >
            <el-card class="question-card" shadow="hover">
              <div class="question-header">
                <el-tag :type="getQuestionTypeTag(question.questionType)" size="small">
                  {{ getQuestionTypeText(question.questionType) }}
                </el-tag>
                <span class="question-score">{{ question.maxScore }}分</span>
              </div>
              
              <div class="question-title">{{ question.title }}</div>
              
              <div class="question-content" v-html="question.content"></div>
              
              <div class="question-meta">
                <div class="meta-item">
                  <el-icon><DataLine /></el-icon>
                  <span>{{ getSourceText(question.sourceType) }}</span>
                </div>
                <div class="meta-item" v-if="question.examTitle">
                  <el-icon><Document /></el-icon>
                  <span>{{ question.examTitle }}</span>
                </div>
              </div>
              
              <div class="question-actions">
                <el-button size="small" type="primary" @click="editQuestion(question.id)">
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
import { Search, List, Grid, Plus, Refresh, MagicStick, FolderOpened, ArrowDown, Filter, Collection, DataLine, Edit, Link, Reading, School, Document, Delete } from '@element-plus/icons-vue'
import { questionApi } from '@/api/question'
import { questionBankApi } from '@/api/questionBank'
import { knowledgeBaseApi } from '@/api/knowledge'
import { examApi } from '@/api/exam'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { QuestionResponse, ExamResponse, QuestionBank } from '@/types/api'
import type { KnowledgeBase } from '@/api/knowledge'
import RubricManagementDialog from '@/components/evaluation/RubricManagementDialog.vue'

const router = useRouter()

const loading = ref(false)
const viewMode = ref<'list' | 'card'>('card')
const questions = ref<QuestionResponse[]>([])
const exams = ref<ExamResponse[]>([])
const questionBanks = ref<QuestionBank[]>([])
const knowledgeBases = ref<KnowledgeBase[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const filtersExpanded = ref(false)

// 搜索表单
const searchForm = ref({
  keyword: '',
  questionType: '',
  examId: '',
  source: ''
})

// 快速筛选方法
const handleQuickFilter = (source: string) => {
  searchForm.value.source = source
  searchQuestions()
}

// 切换高级筛选显示
const toggleFilters = () => {
  filtersExpanded.value = !filtersExpanded.value
}

// 清空所有筛选条件
const clearAllFilters = () => {
  searchForm.value = {
    keyword: '',
    questionType: '',
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

const loadExams = async () => {
  try {
    const examData = await examApi.getAllExams()
    exams.value = examData || []
  } catch (error) {
    console.error('加载考试列表失败:', error)
  }
}

const loadQuestionBanks = async () => {
  try {
    const response = await questionBankApi.getAccessibleQuestionBanks()
    questionBanks.value = response.content || []
  } catch (error) {
    console.error('加载题目库列表失败:', error)
  }
}

const loadKnowledgeBases = async () => {
  try {
    const response = await knowledgeBaseApi.getKnowledgeBases()
    knowledgeBases.value = response.content || []
  } catch (error) {
    console.error('加载知识库列表失败:', error)
  }
}

// 搜索方法
const searchQuestions = () => {
  currentPage.value = 1
  loadQuestions()
}

// 重置搜索
const resetSearch = () => {
  searchForm.value = {
    keyword: '',
    questionType: '',
    examId: '',
    source: ''
  }
  searchQuestions()
}

const getQuestionTypeTag = (type: string): 'primary' | 'success' | 'info' | 'warning' | 'danger' => {
  const map: Record<string, 'primary' | 'success' | 'info' | 'warning' | 'danger'> = {
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

const getSourceText = (sourceType: string | undefined) => {
  if (!sourceType) return '未知'
  const map: Record<string, string> = {
    'SELF_CREATED': '自创',
    'INTERNET': '互联网',
    'AI_GENERATED': 'AI生成',
    'AI_ORGANIZED': 'AI整理'
  }
  return map[sourceType] || '未知'
}

// 评分标准管理
const rubricDialogVisible = ref(false)
const selectedQuestionId = ref<number>(0)

const manageRubric = (questionId: number) => {
  selectedQuestionId.value = questionId
  rubricDialogVisible.value = true
}

// 题目操作
const createQuestion = () => {
  router.push('/question/create')
}

const editQuestion = (questionId: number) => {
  router.push(`/question/edit/${questionId}`)
}

const generateWithAI = () => {
  router.push('/question/ai-generate')
}

const deleteQuestion = async (questionId: string) => {
  try {
    await ElMessageBox.confirm('确定要删除这个题目吗？', '确认删除', {
      type: 'warning'
    })
    
    await questionApi.deleteQuestion(parseInt(questionId))
    ElMessage.success('题目删除成功')
    loadQuestions()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除题目失败:', error)
      ElMessage.error('删除题目失败')
    }
  }
}

// 分页处理
const handleSizeChange = (newSize: number) => {
  pageSize.value = newSize
  loadQuestions()
}

const handleCurrentChange = (newPage: number) => {
  currentPage.value = newPage
  loadQuestions()
}

// 组件挂载时加载数据
onMounted(() => {
  loadQuestions()
  loadExams()
  loadQuestionBanks()
  loadKnowledgeBases()
})
</script>

<style scoped lang="scss">
.question-library {
  padding: 20px;
  background-color: #f8f9fa;
  min-height: 100vh;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
  
  h1 {
    font-size: 32px;
    font-weight: bold;
    color: #2c3e50;
    margin-bottom: 10px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
  
  .page-description {
    font-size: 16px;
    color: #666;
    margin: 0;
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
  
  .search-box {
    display: flex;
    align-items: center;
    background: white;
    border-radius: 28px;
    padding: 4px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
    border: 2px solid transparent;
    transition: all 0.3s ease;
    
    &:hover, &:focus-within {
      border-color: #409eff;
      box-shadow: 0 6px 30px rgba(64, 158, 255, 0.15);
      transform: translateY(-1px);
    }
  }
  
  .search-icon-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 48px;
    height: 48px;
    border-radius: 24px;
    background: linear-gradient(135deg, #f0f9ff, #e0f2fe);
    margin-right: 12px;
    
    .search-icon {
      color: #409eff;
      font-size: 20px;
    }
  }
  
  .search-input {
    flex: 1;
    margin-right: 12px;
    
    :deep(.el-input__wrapper) {
      border: none;
      box-shadow: none;
      background: transparent;
      padding: 12px 0;
      
      &:hover, &.is-focus {
        box-shadow: none;
      }
    }
    
    :deep(.el-input__inner) {
      font-size: 16px;
      font-weight: 400;
      color: #303133;
      
      &::placeholder {
        color: #a8abb2;
        font-weight: 400;
      }
    }
    
    :deep(.el-input__suffix) {
      display: flex;
      align-items: center;
    }
  }
  
  .search-btn {
    border-radius: 24px;
    padding: 12px 24px;
    font-weight: 600;
    background: linear-gradient(135deg, #409eff, #66b1ff);
    border: none;
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
    height: 48px;
    white-space: nowrap;
    
    &:hover {
      background: linear-gradient(135deg, #337ecc, #5a9ff8);
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
    }
    
    .el-icon {
      margin-right: 6px;
    }
  }
}

.quick-filters {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-group-label {
  font-weight: 600;
  color: #606266;
  white-space: nowrap;
}

.filter-tag {
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 20px;
  padding: 8px 16px;
  font-weight: 500;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }
}

.toggle-btn {
  font-weight: 500;
  
  .toggle-icon {
    transition: transform 0.3s ease;
    margin-right: 6px;
    
    &.rotated {
      transform: rotate(180deg);
    }
  }
}

.advanced-filters {
  padding: 24px;
  background: #ffffff;
  border-top: 1px solid #e4e7ed;
}

.filters-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  font-size: 16px;
  font-weight: 600;
  color: #606266;
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  color: #606266;
  font-size: 14px;
}

.filter-select {
  width: 100%;
}

.option-content {
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.filter-actions-left {
  display: flex;
  gap: 12px;
}

.action-btn {
  border-radius: 8px;
  font-weight: 500;
  
  &.primary {
    background: linear-gradient(135deg, #409eff, #66b1ff);
    border: none;
    
    &:hover {
      background: linear-gradient(135deg, #337ecc, #5a9ff8);
    }
  }
}

.clear-btn {
  color: #909399;
  
  &:hover {
    color: #606266;
  }
}

.quick-actions {
  padding: 20px 24px;
  background: linear-gradient(135deg, #f8fbff 0%, #f0f9ff 100%);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.view-mode-switcher {
  :deep(.el-radio-button__inner) {
    border-radius: 8px;
    margin: 0 2px;
    border: 1px solid #dcdfe6;
    
    &:hover {
      color: #409eff;
    }
  }
  
  :deep(.el-radio-button:first-child .el-radio-button__inner) {
    border-left: 1px solid #dcdfe6;
  }
  
  :deep(.el-radio-button__input:checked + .el-radio-button__inner) {
    background: #409eff;
    border-color: #409eff;
    color: white;
  }
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.content-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.questions-container {
  min-height: 400px;
}

.question-card {
  height: 100%;
  border-radius: 12px;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  }
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.question-score {
  font-weight: 600;
  color: #f56c6c;
}

.question-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.question-content {
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.question-meta {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
}

.question-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

// 响应式设计
@media (max-width: 768px) {
  .question-library {
    padding: 12px;
  }
  
  .search-box {
    flex-direction: column;
    padding: 16px;
    border-radius: 16px;
    gap: 12px;
    
    .search-icon-wrapper {
      order: -1;
      margin-right: 0;
      margin-bottom: 8px;
    }
    
    .search-input {
      margin-right: 0;
      margin-bottom: 8px;
    }
    
    .search-btn {
      width: 100%;
      justify-content: center;
    }
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
    width: 100%;
    justify-content: center;
  }
  
  .quick-actions {
    flex-direction: column;
    align-items: stretch;
    text-align: center;
  }
  
  .action-buttons {
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .page-header h1 {
    font-size: 24px;
  }
  
  .search-main,
  .advanced-filters,
  .quick-actions {
    padding: 16px;
  }
  
  .filter-group {
    flex-direction: column;
    align-items: center;
  }
  
  .filter-actions-left {
    flex-direction: column;
    gap: 8px;
  }
  
  .action-buttons {
    flex-direction: column;
    width: 100%;
  }
}
</style>
