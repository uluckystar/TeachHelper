<template>
  <div class="exam-rubric-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <el-page-header @back="handleBack">
        <template #content>
          <span class="page-title">评分标准管理</span>
        </template>
      </el-page-header>
    </div>

    <!-- 考试信息栏 -->
    <el-card class="exam-info" v-if="examInfo">
      <div class="exam-basic-info">
        <h3>{{ examInfo.title }}</h3>
        <div class="exam-meta">
          <el-tag :type="getExamStatusType(examInfo.status || 'active') as any" size="small">
            {{ getExamStatusText(examInfo.status || 'active') }}
          </el-tag>
          <span class="meta-text">创建者：{{ examInfo.createdBy }}</span>
          <span class="meta-text">{{ formatDate(examInfo.createdAt) }}</span>
        </div>
      </div>
    </el-card>

    <!-- AI生成进度和结果 -->
    <el-card v-if="aiGenerating || aiResults.length > 0" class="ai-progress-card" style="margin-bottom: 20px;">
      <template #header>
        <div class="card-header">
          <span v-if="aiGenerating">
            <el-icon><MagicStick /></el-icon>
            AI生成进度
          </span>
          <span v-else>
            <el-icon><Check /></el-icon>
            AI生成结果
          </span>
          <el-button 
            v-if="aiGenerating"
            type="danger" 
            size="small"
            @click="cancelAIGeneration"
          >
            取消生成
          </el-button>
          <el-button 
            v-if="!aiGenerating && aiResults.length > 0"
            size="small"
            @click="clearAIResults"
            icon="Close"
          >
            关闭
          </el-button>
        </div>
      </template>
      
      <div class="ai-progress">
        <!-- 进度条（生成中显示） -->
        <div v-if="aiGenerating">
          <el-progress 
            :percentage="aiProgress" 
            :status="aiProgress === 100 ? 'success' : undefined"
          />
          <p class="progress-text">{{ aiProgressText }}</p>
        </div>
        
        <!-- 完成的进度条 -->
        <div v-if="!aiGenerating && aiResults.length > 0" class="completed-progress">
          <el-progress 
            :percentage="100" 
            status="success"
          />
          <p class="progress-text">{{ aiProgressText || `评分标准生成完成！成功生成 ${aiResults.length} 个题目的评分标准` }}</p>
        </div>
        
        <!-- 成功完成提示 -->
        <div v-if="!aiGenerating && aiResults.length > 0" class="success-message">
          <el-alert
            :title="`AI评分标准生成完成！成功生成 ${aiResults.length} 个题目的评分标准`"
            type="success"
            show-icon
            :closable="false"
          />
        </div>
        
        <!-- 生成结果 -->
        <div v-if="aiResults.length > 0" class="ai-results">
          <h4>生成结果预览：</h4>
          <div class="results-list">
            <div v-for="result in aiResults" :key="result.questionId" class="result-item">
              <div class="result-header">
                <h5>{{ getQuestionTitle(result.questionId) }}</h5>
                <el-tag size="small" type="info">{{ result.rubrics.length }}个评分标准</el-tag>
              </div>
              <div class="rubrics">
                <el-tag 
                  v-for="rubric in result.rubrics" 
                  :key="rubric.name"
                  class="rubric-tag"
                  type="success"
                >
                  {{ rubric.name }} ({{ rubric.points }}分)
                </el-tag>
              </div>
            </div>
          </div>
          
          <div class="ai-actions" style="margin-top: 20px; padding: 15px; background: #f8f9fa; border-radius: 8px;">
            <el-alert
              title="请确认生成的评分标准是否符合要求，然后点击应用按钮保存到数据库"
              type="info"
              show-icon
              :closable="false"
              style="margin-bottom: 15px;"
            />
            <div class="action-buttons">
              <el-button 
                type="primary" 
                size="large"
                @click="applyAIResults"
                :loading="applying"
                icon="Check"
              >
                {{ applying ? '应用中...' : '应用所有评分标准' }}
              </el-button>
              <el-button 
                size="large"
                @click="generateAIRubrics"
                :disabled="selectedQuestions.length === 0"
                icon="Refresh"
              >
                重新生成
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 批量操作工具栏 -->
    <el-card class="operations-card" v-if="questions.length > 0">
      <div class="operations">
        <div class="selection-info">
          <el-checkbox 
            v-model="selectAll" 
            @change="handleSelectAll"
            :indeterminate="isIndeterminate"
          >
            已选择 {{ selectedQuestions.length }} / {{ questions.length }} 个题目
          </el-checkbox>
        </div>
        
        <div class="batch-actions">
          <el-button 
            type="primary" 
            icon="Plus"
            @click="showCreateRubricDialog = true"
            :disabled="selectedQuestions.length === 0"
          >
            手动创建评分标准
          </el-button>
          <el-button 
            type="success" 
            icon="MagicStick"
            @click="generateAIRubrics"
            :disabled="selectedQuestions.length === 0"
            :loading="aiGenerating"
          >
            AI生成评分标准
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 题目列表 -->
    <el-card class="questions-card" v-if="questions.length > 0">
      <template #header>
        <div class="card-header">
          <span>题目列表</span>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索题目..."
            prefix-icon="Search"
            size="small"
            style="width: 200px"
            clearable
          />
        </div>
      </template>
      
      <div class="questions-table">
        <el-table 
          :data="paginatedQuestions" 
          @selection-change="handleSelectionChange"
          row-key="id"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column label="题目" min-width="300">
            <template #default="{ row, $index }">
              <div class="question-cell">
                <div class="question-header">
                  <el-tag 
                    :type="getQuestionTypeTag(row.questionType)"
                    size="small"
                  >
                    {{ getQuestionTypeText(row.questionType) }}
                  </el-tag>
                  <span class="question-number">第 {{ $index + 1 }} 题</span>
                </div>
                <div class="question-title">{{ row.title }}</div>
                <div class="question-content" v-if="row.content">
                  {{ row.content.substring(0, 100) }}{{ row.content.length > 100 ? '...' : '' }}
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="满分" width="80" align="center">
            <template #default="{ row }">
              <span class="score-text">{{ row.maxScore }}</span>
            </template>
          </el-table-column>
          <el-table-column label="评分标准" width="120" align="center">
            <template #default="{ row }">
              <el-tag 
                :type="getRubricCount(row.id) > 0 ? 'success' : 'info'"
                size="small"
              >
                {{ getRubricCount(row.id) }} 个
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center" fixed="right">
            <template #default="{ row }">
              <el-button 
                type="primary" 
                size="small"
                @click="manageQuestionRubric(row)"
              >
                管理评分标准
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <!-- 分页 -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[5, 10, 20, 50]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            class="pagination"
          />
        </div>
      </div>
    </el-card>

    <!-- 空状态 -->
    <el-card v-else class="empty-card">
      <el-empty description="该考试暂无题目">
        <el-button type="primary" @click="navigateToAddQuestion">添加题目</el-button>
      </el-empty>
    </el-card>

    <!-- 评分标准总览 -->
    <el-card class="rubrics-overview-card">
      <template #header>
        <div class="card-header">
          <span>评分标准总览</span>
          <el-button 
            size="small" 
            icon="Refresh"
            @click="loadRubricOverview"
            :loading="overviewLoading"
          >
            刷新
          </el-button>
        </div>
      </template>

      <div v-if="overviewLoading" class="loading-state">
        <el-skeleton :rows="4" animated />
      </div>

      <div v-else-if="rubricOverview.length === 0" class="empty-state">
        <el-empty description="暂无评分标准">
          <el-button type="primary" @click="selectAllQuestions">选择题目并创建评分标准</el-button>
        </el-empty>
      </div>

      <div v-else class="rubrics-overview">
        <div 
          v-for="overview in rubricOverview" 
          :key="overview.questionId"
          class="overview-item"
        >
          <div class="overview-header">
            <h4>{{ overview.questionTitle }}</h4>
            <div class="overview-meta">
              <el-tag size="small">{{ overview.questionType }}</el-tag>
              <span>满分: {{ overview.maxScore }}分</span>
              <span>评分标准: {{ overview.rubrics.length }}个</span>
            </div>
          </div>
          
          <div v-if="overview.rubrics.length > 0" class="rubrics-list">
            <div 
              v-for="rubric in overview.rubrics" 
              :key="rubric.id"
              class="rubric-item"
            >
              <span class="rubric-name">{{ rubric.criterion }}</span>
              <span class="rubric-score">{{ rubric.maxScore }}分</span>
              <div class="rubric-actions">
                <el-button 
                  size="small" 
                  icon="Edit"
                  @click="editRubric(rubric)"
                >
                  编辑
                </el-button>
                <el-button 
                  size="small" 
                  type="danger" 
                  icon="Delete"
                  @click="deleteRubric(rubric)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>
          
          <div v-else class="no-rubrics">
            <el-text type="info">暂无评分标准</el-text>
            <el-button 
              size="small" 
              type="primary"
              @click="quickCreateRubric(overview.questionId)"
            >
              快速创建
            </el-button>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 操作栏 -->
    <el-card class="actions-card" style="margin-top: 20px;">
      <div class="page-actions">
        <div class="action-info">
          <el-icon><InfoFilled /></el-icon>
          <span>{{ getActionMessage() }}</span>
        </div>
        <div class="action-buttons">
          <el-button @click="handleReturn" icon="ArrowLeft">
            {{ getReturnButtonText() }}
          </el-button>
          <el-button 
            v-if="shouldShowContinueButton()"
            type="primary" 
            @click="handleContinueAction"
            icon="MagicStick"
            :disabled="!canContinueAction()"
          >
            继续批阅
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 手动创建评分标准对话框 -->
    <el-dialog
      v-model="showCreateRubricDialog"
      title="创建评分标准"
      width="700px"
      destroy-on-close
    >
      <div v-if="selectedQuestions.length === 0" class="no-selection-warning">
        <el-alert
          title="请先选择题目"
          description="您需要先在题目列表中选择要创建评分标准的题目"
          type="warning"
          show-icon
          :closable="false"
        />
      </div>
      
      <div v-else class="create-rubric-form">
        <el-tabs v-model="activeRubricTab" type="border-card">
          <!-- 单个题目创建 -->
          <el-tab-pane label="单个创建" name="single">
            <el-form :model="newRubric" :rules="rubricRules" ref="rubricFormRef" label-width="120px">
              <el-form-item label="选择题目" prop="questionId">
                <el-select 
                  v-model="newRubric.questionId" 
                  placeholder="请选择题目" 
                  style="width: 100%"
                  @change="onQuestionSelected"
                >
                  <el-option
                    v-for="question in selectedQuestionsData"
                    :key="question.id"
                    :label="`题目${getQuestionIndex(question.id)}: ${question.title}`"
                    :value="question.id"
                  />
                </el-select>
              </el-form-item>
              
              <!-- 显示题目信息 -->
              <div v-if="selectedQuestionInfo" class="question-info">
                <h4>题目信息</h4>
                <p><strong>类型：</strong>{{ getQuestionTypeText(selectedQuestionInfo.questionType) }}</p>
                <p><strong>满分：</strong>{{ selectedQuestionInfo.maxScore }} 分</p>
                <p><strong>已有评分标准：</strong>{{ getRubricCount(selectedQuestionInfo.id) }} 个</p>
                <el-divider />
              </div>
              
              <el-form-item label="评分标准名称" prop="criterionText">
                <el-input 
                  v-model="newRubric.criterionText" 
                  placeholder="例如：论点明确、逻辑清晰、计算过程"
                />
              </el-form-item>
              
              <el-form-item label="分数" prop="points">
                <el-input-number 
                  v-model="newRubric.points" 
                  :min="0.5" 
                  :max="getMaxScoreForQuestion(newRubric.questionId)"
                  :step="0.5"
                  placeholder="分数"
                  style="width: 120px"
                />
                <span class="form-hint">
                  建议分数不超过 {{ getMaxScoreForQuestion(newRubric.questionId) }} 分
                </span>
              </el-form-item>
              
              <el-form-item label="详细描述">
                <el-input 
                  v-model="newRubric.description" 
                  type="textarea" 
                  :rows="3"
                  placeholder="详细描述该评分标准的评判要求和标准（可选）"
                />
              </el-form-item>
            </el-form>
          </el-tab-pane>
          
          <!-- 批量创建 -->
          <el-tab-pane label="批量创建" name="batch" v-if="selectedQuestions.length > 1">
            <div class="batch-create-form">
              <el-alert
                title="批量创建提示"
                description="将为所有选中的题目创建相同的评分标准"
                type="info"
                show-icon
                :closable="false"
              />
              
              <el-form :model="batchRubric" :rules="batchRubricRules" ref="batchRubricFormRef" label-width="120px" style="margin-top: 20px;">
                <el-form-item label="评分标准名称" prop="criterionText">
                  <el-input 
                    v-model="batchRubric.criterionText" 
                    placeholder="例如：基础分、答题完整性"
                  />
                </el-form-item>
                
                <el-form-item label="分数分配方式" prop="scoreMode">
                  <el-radio-group v-model="batchRubric.scoreMode">
                    <el-radio value="fixed">固定分数</el-radio>
                    <el-radio value="percentage">按比例分配</el-radio>
                  </el-radio-group>
                </el-form-item>
                
                <el-form-item v-if="batchRubric.scoreMode === 'fixed'" label="固定分数" prop="fixedScore">
                  <el-input-number 
                    v-model="batchRubric.fixedScore" 
                    :min="0.5" 
                    :step="0.5"
                    style="width: 120px"
                  />
                  <span class="form-hint">为每个题目设置相同的分数</span>
                </el-form-item>
                
                <el-form-item v-if="batchRubric.scoreMode === 'percentage'" label="分数比例" prop="percentage">
                  <el-input-number 
                    v-model="batchRubric.percentage" 
                    :min="10" 
                    :max="100"
                    style="width: 120px"
                  />
                  <span class="form-hint">占题目总分的百分比</span>
                </el-form-item>
                
                <el-form-item label="详细描述">
                  <el-input 
                    v-model="batchRubric.description" 
                    type="textarea" 
                    :rows="3"
                    placeholder="详细描述该评分标准的评判要求和标准（可选）"
                  />
                </el-form-item>
              </el-form>
              
              <!-- 预览 -->
              <div class="batch-preview">
                <h4>创建预览</h4>
                <el-table :data="batchPreviewData" size="small" border>
                  <el-table-column label="题目" prop="title" min-width="200" />
                  <el-table-column label="题目总分" prop="maxScore" width="80" align="center" />
                  <el-table-column label="标准分数" prop="calculatedScore" width="80" align="center" />
                </el-table>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCreateRubricDialog = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="activeRubricTab === 'single' ? createRubric() : createBatchRubrics()"
            :loading="creating"
            :disabled="selectedQuestions.length === 0"
          >
            {{ activeRubricTab === 'single' ? '创建' : `批量创建 (${selectedQuestions.length}个)` }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document,
  User,
  Calendar,
  Plus,
  MagicStick,
  Setting,
  Edit,
  Delete,
  Refresh,
  InfoFilled,
  ArrowLeft,
  Check,
  Warning
} from '@element-plus/icons-vue'

import { examApi } from '@/api/exam'
import { questionApi } from '@/api/question'
import { rubricApi } from '@/api/rubric'
import type { 
  ExamResponse, 
  QuestionResponse,
  RubricCriterionResponse
} from '@/types/api'

const router = useRouter()
const route = useRoute()

// 数据状态
const loading = ref(false)
const aiGenerating = ref(false)
const applying = ref(false)
const creating = ref(false)
const overviewLoading = ref(false)

const examInfo = ref<ExamResponse | null>(null)
const questions = ref<QuestionResponse[]>([])
const selectedQuestions = ref<number[]>([])
const rubricOverview = ref<any[]>([])

// 新增的响应式变量
const searchKeyword = ref('')
const selectAll = ref(false)
const isIndeterminate = ref(false)

// 分页相关
const currentPage = ref(1)
const pageSize = ref(10)

// AI生成相关
const aiProgress = ref(0)
const aiProgressText = ref('')
const aiResults = ref<any[]>([])

// 对话框状态
const showCreateRubricDialog = ref(false)
const activeRubricTab = ref('single')

// 表单数据
const newRubric = reactive({
  questionId: undefined as number | undefined,
  criterionText: '',
  points: 1,
  description: ''
})

// 批量创建表单数据
const batchRubric = reactive({
  criterionText: '',
  scoreMode: 'fixed' as 'fixed' | 'percentage',
  fixedScore: 1,
  percentage: 20,
  description: ''
})

const rubricFormRef = ref()
const batchRubricFormRef = ref()

// 表单验证规则
const rubricRules = {
  questionId: [
    { required: true, message: '请选择题目', trigger: 'change' }
  ],
  criterionText: [
    { required: true, message: '请输入评分标准名称', trigger: 'blur' }
  ],
  points: [
    { required: true, message: '请输入分数', trigger: 'blur' }
  ]
}

const batchRubricRules = {
  criterionText: [
    { required: true, message: '请输入评分标准名称', trigger: 'blur' }
  ],
  scoreMode: [
    { required: true, message: '请选择分数分配方式', trigger: 'change' }
  ],
  fixedScore: [
    { required: true, message: '请输入固定分数', trigger: 'blur' }
  ],
  percentage: [
    { required: true, message: '请输入分数比例', trigger: 'blur' }
  ]
}

// 计算属性
const examId = computed(() => {
  const id = route.params.examId
  if (typeof id === 'string') {
    return parseInt(id)
  }
  if (Array.isArray(id)) {
    return parseInt(id[0])
  }
  return id as number
})

const selectedQuestionsData = computed(() => {
  return questions.value.filter(q => selectedQuestions.value.includes(q.id))
})

// 过滤后的题目列表
const filteredQuestions = computed(() => {
  if (!searchKeyword.value) {
    return questions.value
  }
  return questions.value.filter(q => 
    q.title.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
    q.content?.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

// 分页后的题目列表
const paginatedQuestions = computed(() => {
  const filtered = filteredQuestions.value
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filtered.slice(start, end)
})

// 总页数
const total = computed(() => filteredQuestions.value.length)

// 选中的题目信息
const selectedQuestionInfo = computed(() => {
  if (!newRubric.questionId) return null
  return questions.value.find(q => q.id === newRubric.questionId)
})

// 批量创建预览数据
const batchPreviewData = computed(() => {
  return selectedQuestionsData.value.map(question => {
    let calculatedScore = 0
    if (batchRubric.scoreMode === 'fixed') {
      calculatedScore = batchRubric.fixedScore
    } else {
      calculatedScore = Math.round((question.maxScore * batchRubric.percentage / 100) * 10) / 10
    }
    
    return {
      id: question.id,
      title: question.title,
      maxScore: question.maxScore,
      calculatedScore
    }
  })
})

// 生命周期
onMounted(async () => {
  await loadData()
})

// 方法
const handleBack = () => {
  const from = route.query.from as string
  const examId = route.params.examId
  
  // 根据来源返回不同页面
  switch (from) {
    case 'exam-list':
      router.push('/exams')
      break
    case 'evaluation-overview':
      router.push('/evaluation/overview')
      break
    case 'batch-evaluation':
      router.push('/evaluation/batch')
      break
    case 'exam-detail':
    default:
      router.push(`/exams/${examId}`)
      break
  }
}

// 智能返回方法
const handleReturn = () => {
  handleBack()
}

// 获取操作提示信息
const getActionMessage = () => {
  const action = route.query.action as string
  const from = route.query.from as string
  
  if (action === 'ai-evaluation') {
    return '完善评分标准后，您可以返回原页面或继续进行AI批阅操作'
  }
  return '评分标准管理完成后，您可以返回原页面'
}

// 获取返回按钮文本
const getReturnButtonText = () => {
  const from = route.query.from as string
  
  switch (from) {
    case 'exam-list':
      return '返回考试列表'
    case 'evaluation-overview':
      return '返回批阅概览'
    case 'batch-evaluation':
      return '返回批量批阅'
    case 'exam-detail':
    default:
      return '返回考试详情'
  }
}

// 是否显示继续按钮
const shouldShowContinueButton = () => {
  const action = route.query.action as string
  return action === 'ai-evaluation'
}

// 是否可以继续操作
const canContinueAction = () => {
  // 检查是否有足够的评分标准
  return rubricOverview.value.some((overview: any) => overview.rubrics && overview.rubrics.length > 0)
}

// 处理继续操作
const handleContinueAction = async () => {
  const from = route.query.from as string
  const examId = route.params.examId
  
  try {
    // 根据来源页面跳转到相应的批阅页面
    switch (from) {
      case 'exam-list':
      case 'exam-detail':
      case 'evaluation-overview':
        router.push(`/exams/${examId}/ai-evaluation`)
        break
      case 'batch-evaluation':
        router.push('/evaluation/batch')
        break
      default:
        router.push(`/exams/${examId}/ai-evaluation`)
        break
    }
  } catch (error) {
    console.error('跳转失败:', error)
    ElMessage.error('跳转失败')
  }
}

const loadData = async () => {
  try {
    loading.value = true
    
    // 并行加载考试详情和题目列表
    const [examData, questionsData] = await Promise.all([
      examApi.getExam(examId.value),
      questionApi.getQuestionsByExam(examId.value)
    ])
    
    examInfo.value = examData
    questions.value = questionsData
    
    // 加载评分标准总览
    await loadRubricOverview()
    
  } catch (error) {
    console.error('Failed to load data:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const loadRubricOverview = async () => {
  try {
    overviewLoading.value = true
    const overview = []
    
    for (const question of questions.value) {
      try {
        const rubrics = await rubricApi.getQuestionRubrics(question.id)
        overview.push({
          questionId: question.id,
          questionTitle: question.title,
          questionType: getQuestionTypeText(question.questionType),
          maxScore: question.maxScore,
          rubrics
        })
      } catch (error) {
        // 如果某个题目的评分标准加载失败，继续加载其他题目
        overview.push({
          questionId: question.id,
          questionTitle: question.title,
          questionType: getQuestionTypeText(question.questionType),
          maxScore: question.maxScore,
          rubrics: []
        })
      }
    }
    
    rubricOverview.value = overview
  } catch (error) {
    console.error('Failed to load rubric overview:', error)
    ElMessage.error('加载评分标准总览失败')
  } finally {
    overviewLoading.value = false
  }
}

const selectAllQuestions = () => {
  selectedQuestions.value = questions.value.map(q => q.id)
}

const clearSelection = () => {
  selectedQuestions.value = []
}

const generateAIRubrics = async () => {
  if (selectedQuestions.value.length === 0) {
    ElMessage.warning('请先选择要生成评分标准的题目')
    return
  }

  try {
    aiGenerating.value = true
    aiProgress.value = 0
    aiResults.value = []
    
    const total = selectedQuestions.value.length
    let completed = 0
    
    aiProgressText.value = `正在并发生成 ${total} 个题目的评分标准...`
    
    // 并发生成所有题目的评分标准
    const promises = selectedQuestions.value.map(async (questionId) => {
      try {
        const suggestions = await questionApi.generateRubric(questionId)
        
        // 更新进度
        completed++
        aiProgress.value = Math.round((completed / total) * 100)
        aiProgressText.value = `已完成 ${completed}/${total} 个题目的评分标准生成`
        
        return {
          questionId,
          rubrics: suggestions.map((s: any) => ({
            name: s.criterionText,  // 使用 name 字段保持一致性
            points: s.points
          })),
          success: true
        }
      } catch (error) {
        console.error(`Failed to generate rubric for question ${questionId}:`, error)
        ElMessage.warning(`题目 ${getQuestionIndex(questionId)} 评分标准生成失败`)
        
        completed++
        aiProgress.value = Math.round((completed / total) * 100)
        
        return {
          questionId,
          rubrics: [],
          success: false,
          error: error
        }
      }
    })
    
    // 等待所有并发请求完成
    const results = await Promise.all(promises)
    
    // 只保存成功的结果
    aiResults.value = results.filter(result => result.success && result.rubrics.length > 0)
    
    const successCount = aiResults.value.length
    const failedCount = total - successCount
    
    if (successCount > 0) {
      aiProgressText.value = `评分标准生成完成！成功 ${successCount} 个，失败 ${failedCount} 个`
      ElMessage.success(`AI评分标准生成完成，成功生成 ${successCount} 个题目的评分标准`)
    } else {
      aiProgressText.value = '所有题目的评分标准生成都失败了'
      ElMessage.error('所有题目的评分标准生成都失败了')
    }
    
  } catch (error) {
    console.error('Failed to generate AI rubrics:', error)
    ElMessage.error('AI生成评分标准失败')
  } finally {
    aiGenerating.value = false
  }
}

const applyAIResults = async () => {
  try {
    applying.value = true
    
    let successCount = 0
    let failedCount = 0
    
    console.log('开始应用AI评分标准...', aiResults.value)
    
    // 并发应用所有评分标准
    const promises = aiResults.value.map(async (result) => {
      try {
        console.log(`应用题目 ${result.questionId} 的评分标准:`, result.rubrics)
        
        // 修正数据结构：AI返回的是 {name, points}，需要转换为 {criterionText, points}
        const suggestions = result.rubrics.map((r: any) => ({
          criterionText: r.name,  // AI返回的字段名是 'name'，不是 'criterion'
          points: r.points
        }))
        
        console.log(`转换后的建议:`, suggestions)
        
        await questionApi.applyRubricSuggestions(result.questionId, suggestions)
        successCount++
        return { questionId: result.questionId, success: true }
      } catch (error) {
        console.error(`Failed to apply rubric for question ${result.questionId}:`, error)
        failedCount++
        return { questionId: result.questionId, success: false, error }
      }
    })
    
    await Promise.all(promises)
    
    if (successCount > 0) {
      ElMessage.success(`评分标准应用完成！成功 ${successCount} 个，失败 ${failedCount} 个`)
    } else {
      ElMessage.error('所有评分标准应用都失败了')
    }
    
    clearAIResults()
    await loadRubricOverview()
    
  } catch (error) {
    console.error('Failed to apply AI results:', error)
    ElMessage.error('应用评分标准失败')
  } finally {
    applying.value = false
  }
}

const clearAIResults = () => {
  aiResults.value = []
  aiProgress.value = 0
  aiProgressText.value = ''
}

const cancelAIGeneration = () => {
  aiGenerating.value = false
  clearAIResults()
  ElMessage.info('已取消AI生成')
}

const createRubric = async () => {
  try {
    await rubricFormRef.value?.validate()
    
    creating.value = true
    
    await rubricApi.createCriterion({
      questionId: newRubric.questionId!,
      criterionText: newRubric.criterionText,
      points: newRubric.points,
      description: newRubric.description
    })
    
    ElMessage.success('评分标准创建成功')
    showCreateRubricDialog.value = false
    resetRubricForm()
    await loadRubricOverview()
    
  } catch (error) {
    console.error('Failed to create rubric:', error)
    ElMessage.error('创建评分标准失败')
  } finally {
    creating.value = false
  }
}

const resetRubricForm = () => {
  newRubric.questionId = undefined
  newRubric.criterionText = ''
  newRubric.points = 1
  newRubric.description = ''
}

// 题目选择变化处理
const onQuestionSelected = (questionId: number) => {
  // 题目选择变化时的处理逻辑
  console.log('Selected question:', questionId)
}

// 批量创建评分标准
const createBatchRubrics = async () => {
  try {
    await batchRubricFormRef.value?.validate()
    
    creating.value = true
    
    const promises = selectedQuestionsData.value.map(async (question) => {
      let score = 0
      if (batchRubric.scoreMode === 'fixed') {
        score = batchRubric.fixedScore
      } else {
        score = Math.round((question.maxScore * batchRubric.percentage / 100) * 10) / 10
      }
      
      return rubricApi.createCriterion({
        questionId: question.id,
        criterionText: batchRubric.criterionText,
        points: score,
        description: batchRubric.description || `批量创建的评分标准 - ${batchRubric.criterionText}`
      })
    })
    
    await Promise.all(promises)
    
    ElMessage.success(`批量创建评分标准成功，共创建 ${selectedQuestionsData.value.length} 个`)
    showCreateRubricDialog.value = false
    resetBatchRubricForm()
    await loadRubricOverview()
    
  } catch (error) {
    console.error('Failed to create batch rubrics:', error)
    ElMessage.error('批量创建评分标准失败')
  } finally {
    creating.value = false
  }
}

const resetBatchRubricForm = () => {
  batchRubric.criterionText = ''
  batchRubric.scoreMode = 'fixed'
  batchRubric.fixedScore = 1
  batchRubric.percentage = 20
  batchRubric.description = ''
}

const manageQuestionRubric = (question: QuestionResponse) => {
  router.push(`/questions/${question.id}/rubric`)
}

const quickCreateRubric = (questionId: number) => {
  selectedQuestions.value = [questionId]
  newRubric.questionId = questionId
  showCreateRubricDialog.value = true
}

const editRubric = (rubric: RubricCriterionResponse) => {
  router.push(`/questions/rubric/${rubric.id}/edit`)
}

const deleteRubric = async (rubric: RubricCriterionResponse) => {
  if (!rubric.id) {
    ElMessage.error('评分标准ID不存在')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除评分标准"${rubric.criterionText}"吗？`,
      '确认删除',
      { type: 'warning' }
    )
    
    await rubricApi.deleteQuestionRubric(rubric.id)
    ElMessage.success('评分标准删除成功')
    await loadRubricOverview()
    
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to delete rubric:', error)
      ElMessage.error('删除评分标准失败')
    }
  }
}

const navigateToAddQuestion = () => {
  router.push(`/exams/${examId.value}/questions/new`)
}

// 辅助方法
const formatDate = (dateString?: string) => {
  if (!dateString) return 'N/A'
  return new Date(dateString).toLocaleString('zh-CN')
}

const getExamStatusType = (status: string) => {
  const statusMap = {
    'active': 'success',
    'draft': 'info',
    'archived': 'warning',
    'deleted': 'danger'
  }
  return statusMap[status as keyof typeof statusMap] || 'info'
}

const getExamStatusText = (status: string) => {
  const statusMap = {
    'active': '进行中',
    'draft': '草稿',
    'archived': '已归档',
    'deleted': '已删除'
  }
  return statusMap[status as keyof typeof statusMap] || status
}

// 选择相关方法
const handleSelectAll = (checked: any) => {
  if (checked) {
    selectedQuestions.value = questions.value.map(q => q.id)
    selectAll.value = true
    isIndeterminate.value = false
  } else {
    selectedQuestions.value = []
    selectAll.value = false
    isIndeterminate.value = false
  }
}

const handleSelectionChange = (selection: any[]) => {
  selectedQuestions.value = selection.map(item => item.id)
  updateSelectAllState()
}

const updateSelectAllState = () => {
  const totalCount = questions.value.length
  const selectedCount = selectedQuestions.value.length
  
  if (selectedCount === 0) {
    selectAll.value = false
    isIndeterminate.value = false
  } else if (selectedCount === totalCount) {
    selectAll.value = true
    isIndeterminate.value = false
  } else {
    selectAll.value = false
    isIndeterminate.value = true
  }
}

// 监听选择变化
watch(selectedQuestions, updateSelectAllState)

const getQuestionTypeText = (type: string) => {
  const typeMap = {
    'ESSAY': '论述题',
    'SHORT_ANSWER': '简答题',
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'TRUE_FALSE': '判断题',
    'CODING': '编程题',
    'CASE_ANALYSIS': '案例分析题',
    'CALCULATION': '计算题'
  }
  return typeMap[type as keyof typeof typeMap] || type
}

const getQuestionTypeTag = (type: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const tagMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    'ESSAY': 'primary',
    'SHORT_ANSWER': 'success',
    'SINGLE_CHOICE': 'warning',
    'MULTIPLE_CHOICE': 'warning',
    'TRUE_FALSE': 'info',
    'CODING': 'danger',
    'CASE_ANALYSIS': 'primary',
    'CALCULATION': 'primary'
  }
  return tagMap[type] || 'primary'
}

const getRubricCount = (questionId: number) => {
  const overview = rubricOverview.value.find(o => o.questionId === questionId)
  return overview ? overview.rubrics.length : 0
}

const getQuestionTitle = (questionId: number) => {
  const question = questions.value.find(q => q.id === questionId)
  return question ? `题目${getQuestionIndex(questionId)}: ${question.title}` : '未知题目'
}

const getQuestionIndex = (questionId: number) => {
  const index = questions.value.findIndex(q => q.id === questionId)
  return index >= 0 ? index + 1 : '?'
}

const getMaxScoreForQuestion = (questionId: number | undefined) => {
  if (!questionId) return 100
  const question = questions.value.find(q => q.id === questionId)
  return question ? question.maxScore : 100
}

// 获取评分标准覆盖率百分比
const getRubricCoveragePercentage = () => {
  if (questions.value.length === 0) return 0
  const questionsWithRubrics = getQuestionsWithRubrics()
  return Math.round((questionsWithRubrics / questions.value.length) * 100)
}

// 获取已设置评分标准的题目数量
const getQuestionsWithRubrics = () => {
  return rubricOverview.value.filter(overview => overview.rubrics && overview.rubrics.length > 0).length
}

// 获取评分标准总数
const getTotalRubrics = () => {
  return rubricOverview.value.reduce((total, overview) => {
    return total + (overview.rubrics ? overview.rubrics.length : 0)
  }, 0)
}
</script>

<style scoped>
.exam-rubric-management {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

/* 考试信息样式 */
.exam-info {
  margin-bottom: 20px;
}

.overview-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
}

.exam-basic-info {
  flex: 1;
}

.exam-basic-info h3 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 18px;
}

.exam-desc {
  color: #606266;
  margin: 8px 0 12px 0;
  line-height: 1.5;
}

.exam-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-text {
  color: #909399;
  font-size: 14px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  min-width: 200px;
}

.stat-item {
  text-align: center;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

/* 操作工具栏样式 */
.operations-card,
.questions-card,
.empty-card,
.ai-progress-card {
  margin-bottom: 20px;
}

.operations {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.selection-info {
  font-size: 14px;
  color: #606266;
}

.batch-actions {
  display: flex;
  gap: 12px;
}

/* 题目表格样式 */
.questions-table {
  margin-top: 16px;
}

/* 分页样式 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.question-cell {
  padding: 8px 0;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.question-number {
  font-size: 12px;
  color: #909399;
}

.question-title {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.question-content {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.score-text {
  font-weight: 500;
  color: #67c23a;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.header-badges {
  display: flex;
  gap: 8px;
  align-items: center;
}

.exam-info {
  text-align: left;
}

.exam-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.exam-info h3 {
  margin: 0;
  color: #303133;
  flex: 1;
}

.quick-stats {
  display: flex;
  gap: 24px;
  align-items: center;
}

.stat-item {
  text-align: center;
}

.exam-description {
  color: #606266;
  margin-bottom: 16px;
  text-align: left;
}

.exam-meta {
  margin-top: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
}

.question-selection {
  margin-top: 16px;
}

.selection-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.selection-header h4 {
  margin: 0;
  color: #303133;
}

.selection-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.selection-info {
  font-size: 14px;
  color: #606266;
}

.questions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 16px;
  width: 100%;
}

.question-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
  transition: all 0.3s ease;
}

.question-item:hover {
  border-color: #409eff;
}

.question-item.selected {
  border-color: #409eff;
  background: #f0f9ff;
}

.question-checkbox {
  width: 100%;
}

.question-content {
  width: 100%;
  margin-left: 8px;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.question-title {
  flex: 1;
  font-weight: 500;
  color: #303133;
}

.question-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.question-actions {
  display: flex;
  justify-content: flex-end;
}

.ai-progress {
  text-align: center;
}

.progress-text {
  margin: 16px 0;
  color: #606266;
}

.ai-results {
  margin-top: 24px;
  text-align: left;
}

.results-list {
  margin: 16px 0;
}

.result-item {
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
}

.result-item h5 {
  margin: 0 0 8px 0;
  color: #303133;
}

.rubrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.rubric-tag {
  margin: 0;
}

.ai-actions {
  margin-top: 16px;
  display: flex;
  justify-content: center;
  gap: 12px;
}

.ai-actions .action-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.result-header h5 {
  margin: 0;
  flex: 1;
}

.success-message {
  margin-bottom: 20px;
}

.rubrics-overview {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(500px, 1fr));
  gap: 20px;
}

.overview-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
}

.overview-header {
  margin-bottom: 12px;
}

.overview-header h4 {
  margin: 0 0 8px 0;
  color: #303133;
}

.overview-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 14px;
  color: #606266;
}

.rubrics-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rubric-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: white;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.rubric-name {
  flex: 1;
  font-weight: 500;
}

.rubric-score {
  color: #409eff;
  font-weight: 500;
  margin-right: 12px;
}

.rubric-actions {
  display: flex;
  gap: 8px;
}

.no-rubrics {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background: white;
  border-radius: 4px;
  border: 1px dashed #e4e7ed;
}

.empty-state,
.loading-state {
  text-align: center;
  padding: 40px 0;
}

.create-rubric-form {
  padding: 16px 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* 操作栏样式 */
.actions-card {
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  border: none;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.page-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
}

.action-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 14px;
}

.action-info .el-icon {
  color: #409eff;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.action-buttons .el-button {
  border-radius: 20px;
  padding: 8px 20px;
}

.action-buttons .el-button[type="primary"] {
  background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
  border: none;
}

.action-buttons .el-button[type="primary"]:hover {
  background: linear-gradient(135deg, #337ecc 0%, #529b2e 100%);
}
</style>
