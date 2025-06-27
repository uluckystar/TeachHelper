<template>
  <div class="question-evaluation">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-row :gutter="24" align="middle">
        <el-col :span="18">
          <el-breadcrumb>
            <el-breadcrumb-item :to="{ path: '/exams' }">考试列表</el-breadcrumb-item>
            <el-breadcrumb-item :to="{ path: `/exams/${examId}` }">
              {{ examTitle || '考试详情' }}
            </el-breadcrumb-item>
            <el-breadcrumb-item>{{ questionTitle || '题目批阅' }}</el-breadcrumb-item>
          </el-breadcrumb>
          <h1 v-if="questionInfo" class="page-title">批阅题目：{{ questionInfo.title }}</h1>
          <el-skeleton v-else animated>
            <template #template>
              <el-skeleton-item variant="h1" style="width: 400px" />
            </template>
          </el-skeleton>
        </el-col>
        <el-col :span="6" class="header-actions-col">
          <div class="header-actions">
            <el-button @click="handleBack" icon="ArrowLeft">返回</el-button>
            <el-button 
              type="primary" 
              icon="Refresh"
              @click="loadData"
              :loading="loading"
            >
              刷新页面
            </el-button>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 主要内容区域 -->
    <el-row :gutter="24" v-if="!loading" class="main-content-row">
      <!-- 上部分：题目信息 -->
      <el-col :span="24" class="question-info-col">
        <el-card class="question-info-card" v-if="questionInfo">
          <template #header>
            <span>题目信息</span>
          </template>
          <div class="question-content">
            <h3>{{ questionInfo.title }}</h3>
            <div class="question-text" v-html="questionInfo.content"></div>
            <div class="question-meta">
              <el-tag>{{ getQuestionTypeText(questionInfo.questionType) }}</el-tag>
              <span class="max-score">满分：{{ questionInfo.maxScore }}分</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 下部分：批阅统计、操作和答案列表 -->
      <el-col :span="24" class="evaluation-details-col">
        <!-- 批阅统计 -->
        <el-card class="stats-card" style="margin-bottom: 20px;">
          <template #header>
            <div class="card-header">
              <span>批阅统计</span>
              <el-button 
                size="small" 
                icon="Refresh"
                @click="loadStatistics"
                :loading="statsLoading"
              >
                刷新
              </el-button>
            </div>
          </template>
          
          <div v-if="statistics" class="statistics">
            <el-row :gutter="16">
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-value">{{ statistics.totalAnswers }}</div>
                  <div class="stat-label">答案总数</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-value">{{ statistics.evaluatedAnswers }}</div>
                  <div class="stat-label">已批阅</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-value">{{ statistics.unevaluatedAnswers }}</div>
                  <div class="stat-label">未批阅</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-value">{{ statistics.averageScore ? statistics.averageScore.toFixed(1) : '-' }}</div>
                  <div class="stat-label">平均分</div>
                </div>
              </el-col>
            </el-row>
            
            <div class="progress-section" style="margin-top: 16px;">
              <div class="progress-label">批阅进度</div>
              <el-progress 
                :percentage="statistics.evaluationProgress" 
                :color="getProgressColor(statistics.evaluationProgress)"
              />
            </div>
          </div>
          
          <el-skeleton v-else animated :rows="2" />
        </el-card>

        <!-- 批量操作 -->
        <el-card class="batch-actions-card" style="margin-bottom: 20px;">
          <template #header>
            <span>批量操作</span>
          </template>
          
          <div class="batch-actions">
            <!-- 第一行：基础操作 -->
            <el-row :gutter="12" class="batch-actions-row">
              <el-col :span="12">
                <el-button 
                  type="warning" 
                  icon="DocumentChecked"
                  @click="checkRubricBeforeEvaluation"
                  block
                  size="default"
                >
                  检查评分标准
                </el-button>
              </el-col>
              <el-col :span="12">
                <el-button 
                  type="info" 
                  icon="Setting"
                  @click="showRubricManageDialog"
                  block
                  size="default"
                >
                  管理评分标准
                </el-button>
              </el-col>
            </el-row>
            
            <!-- 第二行：批量批阅操作 -->
            <el-row :gutter="12" class="batch-actions-row">
              <el-col :span="12">
                <el-button 
                  type="primary" 
                  icon="MagicStick"
                  @click="handleBatchEvaluation"
                  :disabled="!statistics || statistics.unevaluatedAnswers === 0"
                  block
                  size="default"
                >
                  AI并发批阅 ({{ statistics?.unevaluatedAnswers || 0 }}个)
                </el-button>
              </el-col>
              <el-col :span="12">
                <el-button 
                  type="success" 
                  icon="Check"
                  @click="markAllAsEvaluated"
                  :disabled="!statistics || statistics.unevaluatedAnswers === 0"
                  block
                  size="default"
                >
                  标记全部已批阅
                </el-button>
              </el-col>
            </el-row>
          </div>
        </el-card>

        <!-- 答案列表 -->
        <el-card class="answers-card">
          <template #header>
            <div class="card-header">
              <span>学生答案列表</span>
              <div class="header-actions">
                <el-select
                  v-model="filterStatus"
                  placeholder="筛选状态"
                  size="small"
                  style="width: 120px"
                  @change="filterAnswers"
                >
                  <el-option label="全部" value="all" />
                  <el-option label="已批阅" value="evaluated" />
                  <el-option label="未批阅" value="unevaluated" />
                </el-select>
                <el-button 
                  size="small" 
                  icon="Refresh"
                  @click="loadAnswers"
                  :loading="answersLoading"
                >
                  刷新
                </el-button>
              </div>
            </div>
          </template>
          
          <div v-if="filteredAnswers.length === 0" class="empty-state">
            <el-empty description="暂无答案" />
          </div>
          
          <div v-else class="answers-list">
            <div 
              v-for="answer in filteredAnswers" 
              :key="answer.id"
              class="answer-item"
              :class="{ 'evaluated': answer.evaluated }"
            >
              <div class="answer-header">
                <div class="student-info">
                  <span class="student-name">{{ answer.student?.name || '未知学生' }}</span>
                  <el-tag 
                    :type="answer.evaluated ? 'success' : 'warning'" 
                    size="small"
                  >
                    {{ answer.evaluated ? '已批阅' : '未批阅' }}
                  </el-tag>
                </div>
                <div class="answer-actions" v-if="!answer.evaluated">
                  <el-button 
                    size="small" 
                    type="primary"
                    @click="handleManualEvaluation(answer)"
                  >
                    手动批阅
                  </el-button>
                  <el-button 
                    size="small" 
                    type="success"
                    @click="handleAiEvaluation(answer)"
                    :loading="aiEvaluating === answer.id"
                  >
                    AI批阅
                  </el-button>
                </div>
                <div class="answer-actions" v-else>
                  <div class="score-display">
                    <span class="score">{{ answer.score || 0 }} / {{ questionInfo?.maxScore || 0 }} 分</span>
                  </div>
                  <div class="re-evaluation-actions">
                    <el-button 
                      size="small" 
                      type="warning"
                      @click="handleReEvaluation(answer)"
                      :loading="aiEvaluating === answer.id"
                    >
                      重新批阅
                    </el-button>
                    <el-button 
                      size="small" 
                      type="info"
                      @click="handleManualEvaluation(answer)"
                    >
                      手动修改
                    </el-button>
                  </div>
                </div>
              </div>
              
              <div class="answer-content">
                <div class="answer-text">{{ answer.answerText || '无答案内容' }}</div>
                <div class="submit-time">提交时间：{{ formatDate(answer.submittedAt) }}</div>
              </div>
              
              <div v-if="answer.feedback && answer.evaluated" class="feedback-content">
                <h4>评价反馈：</h4>
                <div class="feedback-text">{{ answer.feedback }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 加载状态 -->
    <div v-else class="loading-container">
      <el-skeleton animated>
        <template #template>
          <el-skeleton-item variant="h1" style="width: 40%" />
          <el-skeleton-item variant="text" style="width: 100%" />
          <el-skeleton-item variant="text" style="width: 60%" />
          <div style="margin-top: 20px">
            <el-skeleton-item variant="rect" style="width: 100%; height: 200px" />
          </div>
        </template>
      </el-skeleton>
    </div>

    <!-- 手动批阅对话框 -->
    <el-dialog
      v-model="evaluationDialogVisible"
      title="手动批阅答案"
      width="600px"
      :close-on-click-modal="false"
    >
      <div v-if="currentAnswer" class="evaluation-dialog">
        <div class="answer-info">
          <h4>学生答案：</h4>
          <div class="answer-text">{{ currentAnswer.answerText }}</div>
        </div>
        
        <el-form :model="evaluationForm" label-width="80px">
          <el-form-item label="分数">
            <el-input-number
              v-model="evaluationForm.score"
              :min="0"
              :max="questionInfo?.maxScore || 100"
              :precision="1"
              style="width: 200px"
            />
            <span class="score-range">/ {{ questionInfo?.maxScore || 0 }} 分</span>
          </el-form-item>
          <el-form-item label="反馈">
            <el-input
              v-model="evaluationForm.feedback"
              type="textarea"
              :rows="4"
              placeholder="请输入评价反馈..."
            />
          </el-form-item>
        </el-form>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="evaluationDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="confirmEvaluation"
            :loading="evaluationSubmitting"
          >
            确认批阅
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 批量批阅对话框 -->
    <el-dialog
      v-model="batchDialogVisible"
      title="AI并发批量批阅"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="batch-evaluation">
        <el-alert
          title="并发批量批阅说明"
          description="将为每个未批阅的答案并发起AI自动批阅请求。这可能会在短时间内消耗较多资源。请确认是否继续。"
          type="warning"
          :closable="false"
          style="margin-bottom: 20px"
        />
        
        <div class="batch-info">
          <p>待批阅答案数量：<strong>{{ statistics?.unevaluatedAnswers || 0 }}</strong></p>
          <p>处理方式：<strong>前端并发请求</strong></p>
        </div>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="batchDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="startBatchEvaluation"
            :loading="batchEvaluating"
          >
            开始并发批阅
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 评分标准检查对话框 -->
    <el-dialog
      v-model="rubricCheckVisible"
      title="评分标准检查"
      width="700px"
      :close-on-click-modal="false"
    >
      <div v-if="hasRubric && rubricCriteria.length > 0" class="rubric-check">
        <el-alert
          title="评分标准状态"
          :description="getRubricStatusMessage()"
          :type="getRubricStatusType()"
          :closable="false"
          style="margin-bottom: 16px"
        />
        
        <h4>当前评分标准：</h4>
        <el-table :data="rubricCriteria" border style="margin-top: 12px;">
          <el-table-column prop="criterionText" label="评分项" min-width="150">
            <template #default="{ row }">
              <span :class="{ 'text-error': !row.criterionText }">
                {{ row.criterionText || '未设置' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="200">
            <template #default="{ row }">
              <span :class="{ 'text-error': !row.description }">
                {{ row.description || '未设置' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="points" label="分数" width="100" align="center">
            <template #default="{ row }">
              <span :class="{ 'text-error': !row.points || row.points <= 0 }">
                {{ row.points || 0 }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag 
                :type="isRubricCriterionComplete(row) ? 'success' : 'danger'" 
                size="small"
              >
                {{ isRubricCriterionComplete(row) ? '完整' : '不完整' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        
        <div class="rubric-summary" style="margin-top: 16px;">
          <el-row :gutter="16">
            <el-col :span="8">
              <div class="summary-item">
                <div class="summary-label">评分标准数量</div>
                <div class="summary-value">{{ rubricCriteria.length }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="summary-item">
                <div class="summary-label">总分</div>
                <div class="summary-value" :class="{ 'text-error': getTotalPoints() !== (questionInfo?.maxScore || 0) }">
                  {{ getTotalPoints() }} / {{ questionInfo?.maxScore || 0 }}
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="summary-item">
                <div class="summary-label">完整度</div>
                <div class="summary-value">
                  {{ getCompleteCount() }} / {{ rubricCriteria.length }}
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
      </div>
      
      <div v-else class="no-rubric">
        <el-empty description="当前题目尚未设置评分标准">
          <el-button 
            type="primary" 
            @click="showRubricManageDialog"
          >
            立即设置
          </el-button>
        </el-empty>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="rubricCheckVisible = false">关闭</el-button>
          <el-button 
            type="primary" 
            @click="showRubricManageDialog"
            v-if="hasRubric"
          >
            管理评分标准
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 评分标准管理对话框 -->
    <el-dialog
      v-model="rubricManageVisible"
      title="管理评分标准"
      width="800px"
      :close-on-click-modal="false"
    >
      <div class="rubric-manage">
        <!-- 现有评分标准列表 -->
        <div v-if="rubricCriteria.length > 0" class="existing-rubrics">
          <h4>现有评分标准</h4>
          <el-table :data="rubricCriteria" border style="margin-bottom: 20px;">
            <el-table-column prop="criterionText" label="评分项" min-width="150" />
            <el-table-column prop="description" label="描述" min-width="200" />
            <el-table-column prop="points" label="分数" width="100" align="center" />
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row, $index }">
                <el-button 
                  type="primary" 
                  size="small" 
                  @click="editRubricCriterion(row, $index)"
                >
                  编辑
                </el-button>
                <el-button 
                  type="danger" 
                  size="small" 
                  @click="deleteRubricCriterion(row, $index)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        
        <!-- 添加新的评分标准 -->
        <div class="add-rubric">
          <h4>{{ editingIndex >= 0 ? '编辑评分标准' : '添加新评分标准' }}</h4>
          <el-form :model="rubricForm" label-width="100px">
            <el-form-item label="评分项" required>
              <el-input 
                v-model="rubricForm.criterion" 
                placeholder="请输入评分项名称"
                maxlength="100"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="描述" required>
              <el-input 
                v-model="rubricForm.description" 
                type="textarea" 
                :rows="3" 
                placeholder="请输入评分项的详细描述和评分要求"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="分数" required>
              <el-input-number
                v-model="rubricForm.score"
                :min="0"
                :max="questionInfo?.maxScore || 100"
                :precision="1"
                style="width: 200px"
              />
              <span class="score-range">/ {{ questionInfo?.maxScore || 0 }} 分</span>
            </el-form-item>
          </el-form>
          
          <div class="form-actions" style="margin-top: 20px;">
            <el-button 
              type="primary"
              @click="saveRubric"
              :loading="rubricLoading"
              :disabled="!isRubricFormValid"
            >
              {{ editingIndex >= 0 ? '更新评分标准' : '添加评分标准' }}
            </el-button>
            <el-button 
              v-if="editingIndex >= 0"
              @click="cancelEditRubric"
            >
              取消编辑
            </el-button>
          </div>
        </div>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeRubricManageDialog">关闭</el-button>
          <el-button 
            type="success" 
            @click="generateAIRubric"
            :loading="aiGenerating"
          >
            AI生成建议
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { 
  ArrowLeft,
  Refresh,
  MagicStick,
  Check,
  DocumentChecked,
  Setting
} from '@element-plus/icons-vue'

import { questionApi } from '@/api/question'
import { answerApi } from '@/api/answer'
import { evaluationApi } from '@/api/evaluation'
import { examApi } from '@/api/exam'
import { rubricApi } from '@/api/rubric'
import type { 
  Question, 
  StudentAnswerResponse, 
  EvaluationStatistics,
  ManualEvaluationRequest,
  RubricCriterion
} from '@/types/api'

const route = useRoute()
const router = useRouter()

// 响应式数据
const loading = ref(true)
const statsLoading = ref(false)
const answersLoading = ref(false)
const evaluationSubmitting = ref(false)
const batchEvaluating = ref(false)
const aiEvaluating = ref<number | null>(null)
const questionInfo = ref<Question | null>(null)
const statistics = ref<EvaluationStatistics | null>(null)
const answers = ref<StudentAnswerResponse[]>([])
const filterStatus = ref('all')
const evaluationDialogVisible = ref(false)
const batchDialogVisible = ref(false)
const currentAnswer = ref<StudentAnswerResponse | null>(null)

// 评分标准相关状态
const rubricCriteria = ref<RubricCriterion[]>([])
const rubricCheckVisible = ref(false)
const rubricManageVisible = ref(false)
const hasRubric = ref(false)
const rubricLoading = ref(false)
const editingIndex = ref(-1) // -1表示新增，>=0表示编辑现有项
const aiGenerating = ref(false)

// 表单数据
const evaluationForm = reactive({
  score: 0,
  feedback: ''
})

const rubricForm = reactive({
  criterion: '',
  description: '',
  score: 0
})

// 计算属性
const questionId = computed(() => {
  const id = route.params.id
  if (typeof id === 'string') {
    return parseInt(id, 10)
  } else if (Array.isArray(id) && id.length > 0 && typeof id[0] === 'string') {
    return parseInt(id[0], 10)
  } else {
    return Number(id) || 0
  }
})

const examId = computed(() => {
  // 优先从题目信息中获取考试ID
  if (questionInfo.value?.examId) {
    return questionInfo.value.examId
  }
  
  // 如果题目信息还没加载，尝试从路由参数获取
  const id = route.params.examId || route.query.examId
  if (typeof id === 'string') {
    return parseInt(id, 10)
  } else if (Array.isArray(id) && id.length > 0 && typeof id[0] === 'string') {
    return parseInt(id[0], 10)
  } else {
    return Number(id) || 0
  }
})

const examTitle = computed(() => questionInfo.value?.examTitle || '')
const questionTitle = computed(() => questionInfo.value?.title || '')

const filteredAnswers = computed(() => {
  if (filterStatus.value === 'all') return answers.value
  if (filterStatus.value === 'evaluated') return answers.value.filter(a => a.evaluated)
  if (filterStatus.value === 'unevaluated') return answers.value.filter(a => !a.evaluated)
  return answers.value
})

// 表单验证
const isRubricFormValid = computed(() => {
  return !!(rubricForm.criterion && rubricForm.description && rubricForm.score > 0)
})

// 生命周期
onMounted(async () => {
  await loadData()
})

// 方法
const loadData = async () => {
  try {
    loading.value = true
    console.log('🔄 开始加载数据...')
    console.log('📝 当前题目ID:', questionId.value)
    console.log('📚 当前考试ID (初始):', examId.value)
    console.log('🔗 路由参数:', route.params)
    console.log('🔗 路由查询参数:', route.query)
    
    await Promise.all([
      loadQuestionInfo(),
      loadAnswers(),
      loadStatistics(),
      loadRubricCriteria()
    ])
    
    console.log('✅ 数据加载完成')
    console.log('📚 最终考试ID:', examId.value)
    console.log('📝 题目信息:', questionInfo.value)
  } catch (error) {
    console.error('❌ 加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const loadQuestionInfo = async () => {
  try {
    questionInfo.value = await questionApi.getQuestionById(questionId.value)
  } catch (error) {
    console.error('加载题目信息失败:', error)
  }
}

const loadAnswers = async () => {
  try {
    answersLoading.value = true
    answers.value = await answerApi.getAnswersByQuestion(questionId.value)
  } catch (error) {
    console.error('加载答案失败:', error)
    ElMessage.error('加载答案失败')
  } finally {
    answersLoading.value = false
  }
}

const loadStatistics = async () => {
  try {
    statsLoading.value = true
    statistics.value = await evaluationApi.getEvaluationStatistics(questionId.value)
  } catch (error) {
    console.error('加载统计信息失败:', error)
  } finally {
    statsLoading.value = false
  }
}

const loadRubricCriteria = async () => {
  try {
    hasRubric.value = false
    rubricLoading.value = true
    const criteria = await rubricApi.getCriteriaByQuestion(questionId.value)
    rubricCriteria.value = criteria
    
    if (criteria.length > 0) {
      hasRubric.value = true
    }
  } catch (error) {
    console.error('加载评分标准失败:', error)
  } finally {
    rubricLoading.value = false
  }
}

const filterAnswers = () => {
  // 筛选逻辑在computed中处理
}

const evaluateAnswer = (answer: StudentAnswerResponse) => {
  currentAnswer.value = answer
  evaluationForm.score = answer.score || 0
  evaluationForm.feedback = answer.feedback || ''
  evaluationDialogVisible.value = true
}

const confirmEvaluation = async () => {
  if (!currentAnswer.value) return
  
  try {
    evaluationSubmitting.value = true
    const request: ManualEvaluationRequest = {
      score: evaluationForm.score,
      feedback: evaluationForm.feedback
    }
    
    await evaluationApi.manuallyEvaluateAnswer(currentAnswer.value.id, request)
    ElMessage.success('批阅完成')
    
    // 更新本地数据
    const index = answers.value.findIndex(a => a.id === currentAnswer.value!.id)
    if (index > -1) {
      answers.value[index] = {
        ...answers.value[index],
        score: evaluationForm.score,
        feedback: evaluationForm.feedback,
        evaluated: true,
        evaluatedAt: new Date().toISOString()
      }
    }
    
    evaluationDialogVisible.value = false
    await loadStatistics()
  } catch (error) {
    console.error('批阅失败:', error)
    ElMessage.error('批阅失败')
  } finally {
    evaluationSubmitting.value = false
  }
}

const aiEvaluateAnswer = async (answer: StudentAnswerResponse) => {
  try {
    aiEvaluating.value = answer.id
    await evaluationApi.aiEvaluateAnswer(answer.id)
    ElMessage.success('AI批阅完成')
    
    // 重新加载数据
    await loadAnswers()
    await loadStatistics()
  } catch (error) {
    console.error('AI批阅失败:', error)
    ElMessage.error('AI批阅失败')
  } finally {
    aiEvaluating.value = null
  }
}

// 重新批阅答案
const handleReEvaluation = async (answer: StudentAnswerResponse) => {
  try {
    await ElMessageBox.confirm(
      '确定要重新批阅这个答案吗？这将覆盖当前的批阅结果。',
      '确认重新批阅',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    aiEvaluating.value = answer.id
    await evaluationApi.revaluateAnswer(answer.id)
    ElMessage.success('重新批阅完成')
    
    // 重新加载数据
    await loadAnswers()
    await loadStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重新批阅失败:', error)
      ElMessage.error('重新批阅失败')
    }
  } finally {
    aiEvaluating.value = null
  }
}

const showBatchEvaluationDialog = () => {
  batchDialogVisible.value = true
}

const startBatchEvaluation = async () => {
  batchDialogVisible.value = false;
  batchEvaluating.value = true;

  const unevaluatedAnswers = answers.value.filter(a => !a.evaluated);
  const total = unevaluatedAnswers.length;

  if (total === 0) {
    ElMessage.info('没有需要批阅的答案');
    batchEvaluating.value = false;
    return;
  }

  ElNotification({
    title: '批量批阅开始',
    message: `开始并发处理 ${total} 个答案...`,
    type: 'info',
    duration: 3000
  });

  const evaluationPromises = unevaluatedAnswers.map(answer => 
    evaluationApi.aiEvaluateAnswer(answer.id)
  );

  const results = await Promise.allSettled(evaluationPromises);

  let successCount = 0;
  let failureCount = 0;

  results.forEach(result => {
    if (result.status === 'fulfilled') {
      successCount++;
    } else {
      failureCount++;
      console.error('一个AI批阅请求失败:', result.reason);
    }
  });

  ElNotification({
    title: '批量批阅完成',
    message: `成功: ${successCount}, 失败: ${failureCount}. 总共: ${total}.`,
    type: failureCount === 0 ? 'success' : 'warning',
    duration: 4000
  });

  // Refresh data
  await loadAnswers();
  await loadStatistics();

  batchEvaluating.value = false;
}

const markAllAsEvaluated = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要将所有未批阅答案标记为已批阅吗？',
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 这里需要实现批量标记API
    ElMessage.info('批量标记功能正在开发中')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量标记失败:', error)
      ElMessage.error('批量标记失败')
    }
  }
}

const handleBack = () => {
  if (examId.value) {
    router.push(`/exams/${examId.value}`)
  } else {
    router.back()
  }
}

const showRubricManageDialog = () => {
  rubricManageVisible.value = true
}

// 检查评分标准是否存在且完善
const checkRubricBeforeEvaluation = async () => {
  await loadRubricCriteria()
  
  if (rubricCriteria.value.length === 0) {
    ElMessage.warning('当前题目尚未设置评分标准')
    rubricCheckVisible.value = true
    return false
  }
  
  // 检查评分标准是否完善（描述字段可选）
  const incompleteCriteria = rubricCriteria.value.filter(criterion => 
    !criterion.criterionText || 
    !criterion.points || 
    criterion.points <= 0
  )
  
  if (incompleteCriteria.length > 0) {
    ElMessage.warning('评分标准不完善，建议完善后再进行批阅')
    rubricCheckVisible.value = true
    return false
  }
  
  // 检查总分是否合理
  const totalPoints = rubricCriteria.value.reduce((sum, criterion) => sum + (criterion.points || 0), 0)
  const maxScore = questionInfo.value?.maxScore || 0
  
  if (Math.abs(totalPoints - maxScore) > 0.1) {
    const result = await ElMessageBox.confirm(
      `评分标准总分 (${totalPoints}) 与题目满分 (${maxScore}) 不一致，是否继续？`,
      '评分标准检查',
      {
        confirmButtonText: '继续批阅',
        cancelButtonText: '完善标准',
        type: 'warning'
      }
    ).catch(() => false)
    
    if (!result) {
      rubricCheckVisible.value = true
      return false
    }
  }
  
  ElMessage.success('评分标准检查通过')
  return true
}

// 处理批量批阅，先检查评分标准
const handleBatchEvaluation = async () => {
  const canProceed = await checkRubricBeforeEvaluation()
  if (canProceed) {
    showBatchEvaluationDialog()
  }
}

// 处理手动批阅，先检查评分标准
const handleManualEvaluation = async (answer: StudentAnswerResponse) => {
  const canProceed = await checkRubricBeforeEvaluation()
  if (canProceed) {
    evaluateAnswer(answer)
  }
}

// 处理AI批阅，先检查评分标准
const handleAiEvaluation = async (answer: StudentAnswerResponse) => {
  const canProceed = await checkRubricBeforeEvaluation()
  if (canProceed) {
    aiEvaluateAnswer(answer)
  }
}

const saveRubric = async () => {
  try {
    rubricLoading.value = true
    
    if (editingIndex.value >= 0) {
      // 编辑现有标准
      const criterion = rubricCriteria.value[editingIndex.value]
      await rubricApi.updateCriterion(criterion.id!, {
        criterionText: rubricForm.criterion,
        points: rubricForm.score,
        description: rubricForm.description
      })
      ElMessage.success('评分标准更新成功')
    } else {
      // 创建新标准
      await rubricApi.createCriterion({
        questionId: questionId.value,
        criterionText: rubricForm.criterion,
        points: rubricForm.score,
        description: rubricForm.description
      })
      ElMessage.success('评分标准保存成功')
    }
    
    // 清空表单
    resetRubricForm()
    
    // 重新加载评分标准
    await loadRubricCriteria()
  } catch (error) {
    console.error('保存评分标准失败:', error)
    ElMessage.error('保存评分标准失败')
  } finally {
    rubricLoading.value = false
  }
}

// 重置表单
const resetRubricForm = () => {
  Object.assign(rubricForm, {
    criterion: '',
    description: '',
    score: 0
  })
  editingIndex.value = -1
}

// 编辑评分标准
const editRubricCriterion = (criterion: RubricCriterion, index: number) => {
  editingIndex.value = index
  Object.assign(rubricForm, {
    criterion: criterion.criterionText,
    description: criterion.description,
    score: criterion.points
  })
}

// 取消编辑
const cancelEditRubric = () => {
  resetRubricForm()
}

// 删除评分标准
const deleteRubricCriterion = async (criterion: RubricCriterion, index: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个评分标准吗？',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    if (criterion.id) {
      await rubricApi.deleteCriterion(criterion.id)
      ElMessage.success('删除成功')
      await loadRubricCriteria()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除评分标准失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 关闭管理对话框
const closeRubricManageDialog = () => {
  resetRubricForm()
  rubricManageVisible.value = false
}

// 生成AI评分标准建议
const generateAIRubric = async () => {
  try {
    aiGenerating.value = true
    const response = await rubricApi.generateRubric(questionId.value)
    
    if (response && response.criteria && response.criteria.length > 0) {
      ElNotification.success({
        title: 'AI建议生成成功',
        message: `生成了 ${response.criteria.length} 个评分标准建议，请查看并应用`
      })
      
      // 应用第一个建议作为示例
      const firstCriterion = response.criteria[0]
      Object.assign(rubricForm, {
        criterion: firstCriterion.criterionText,
        description: firstCriterion.criterionText,
        score: firstCriterion.points
      })
    } else {
      ElMessage.info('未能生成有效的评分标准建议')
    }
  } catch (error) {
    console.error('生成AI建议失败:', error)
    ElMessage.error('生成AI建议失败')
  } finally {
    aiGenerating.value = false
  }
}

// 辅助方法
const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN')
}

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

const getProgressColor = (percentage: number) => {
  if (percentage < 30) return '#f56c6c'
  if (percentage < 70) return '#e6a23c'
  return '#67c23a'
}

// 评分标准相关的辅助方法
const isRubricCriterionComplete = (criterion: RubricCriterion) => {
  return !!(criterion.criterionText && criterion.description && criterion.points && criterion.points > 0)
}

const getTotalPoints = () => {
  return rubricCriteria.value.reduce((sum, criterion) => sum + (criterion.points || 0), 0)
}

const getCompleteCount = () => {
  return rubricCriteria.value.filter(isRubricCriterionComplete).length
}

const getRubricStatusMessage = () => {
  const total = rubricCriteria.value.length
  const complete = getCompleteCount()
  const totalPoints = getTotalPoints()
  const maxScore = questionInfo.value?.maxScore || 0
  
  if (complete === total && Math.abs(totalPoints - maxScore) <= 0.1) {
    return '评分标准完整且分数配置正确，可以开始批阅'
  } else if (complete < total) {
    return `有 ${total - complete} 个评分标准不完整，建议完善后再批阅`
  } else if (Math.abs(totalPoints - maxScore) > 0.1) {
    return `评分标准总分 (${totalPoints}) 与题目满分 (${maxScore}) 不一致`
  }
  return '评分标准配置正常'
}

const getRubricStatusType = () => {
  const total = rubricCriteria.value.length
  const complete = getCompleteCount()
  const totalPoints = getTotalPoints()
  const maxScore = questionInfo.value?.maxScore || 0
  
  if (complete === total && Math.abs(totalPoints - maxScore) <= 0.1) {
    return 'success'
  } else if (complete < total || Math.abs(totalPoints - maxScore) > 0.1) {
    return 'warning'
  }
  return 'info'
}
</script>

<style scoped>
.question-evaluation {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 600;
}

.header-actions-col {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.main-content-row {
  display: flex;
  flex-direction: column; /* 改为垂直布局 */
}

.question-info-col {
  margin-bottom: 24px; /* 题目信息和批阅详情之间的间距 */
}

.question-info-card .question-content h3 {
  margin-top: 0;
  margin-bottom: 12px;
  font-size: 18px;
}

.question-text {
  margin-bottom: 12px;
  line-height: 1.6;
}

.question-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: #606266;
}

.stats-card .card-header,
.batch-actions-card .card-header,
.answers-card .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 批量操作样式 */
.batch-actions {
  padding: 16px 0;
}

.batch-actions-row {
  margin-bottom: 16px;
}

.batch-actions-row:last-child {
  margin-bottom: 0;
}

.batch-actions .el-button {
  height: 40px;
  font-size: 14px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.batch-actions .el-button:hover:not(.is-disabled) {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.progress-label {
  font-size: 14px;
  margin-bottom: 8px;
  color: #606266;
}

.answers-list {
  max-height: 600px; /* 可以根据需要调整 */
  overflow-y: auto;
}

.answer-item {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 16px;
  margin-bottom: 16px;
  transition: box-shadow 0.3s;
}

.answer-item:hover {
  box-shadow: 0 2px 12px 0 rgba(0,0,0,.1);
}

.answer-item.evaluated {
  background-color: #f5f7fa;
}

.answer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.student-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.student-name {
  font-weight: 500;
}

.answer-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.re-evaluation-actions {
  display: flex;
  gap: 8px;
  margin-left: 12px;
}

.score-display {
  display: flex;
  align-items: center;
}

.score-display .score {
  font-weight: bold;
  color: #409eff;
}

.answer-content .answer-text {
  margin-bottom: 8px;
  line-height: 1.5;
  color: #303133;
}

.answer-content .submit-time {
  font-size: 12px;
  color: #909399;
}

.feedback-content {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #dcdfe6;
}

.feedback-content h4 {
  margin-top: 0;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
}

.feedback-text {
  font-size: 14px;
  color: #606266;
  white-space: pre-wrap; /* 保留换行和空格 */
}

.loading-container {
  padding: 20px;
}

.evaluation-dialog .answer-info {
  margin-bottom: 20px;
}

.evaluation-dialog .answer-info h4 {
  margin-top: 0;
  margin-bottom: 8px;
}

.evaluation-dialog .answer-text {
  background-color: #f8f9fa;
  padding: 10px;
  border-radius: 4px;
  min-height: 80px;
}

.score-range {
  margin-left: 10px;
  color: #909399;
}

.batch-evaluation .batch-info p {
  margin: 8px 0;
}

.rubric-check .text-error {
  color: #f56c6c;
  font-weight: bold;
}

.rubric-check .summary-item {
  text-align: center;
  padding: 8px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.rubric-check .summary-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.rubric-check .summary-value {
  font-size: 16px;
  font-weight: bold;
}

.no-rubric {
  text-align: center;
  padding: 20px;
}

.rubric-manage .existing-rubrics h4,
.rubric-manage .add-rubric h4 {
  margin-top: 0;
  margin-bottom: 16px;
}

.rubric-manage .form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 响应式调整，如果需要的话 */
@media (max-width: 768px) {
  .header-actions-col {
    justify-content: flex-start;
    margin-top: 12px;
  }
  /* 在小屏幕上堆叠统计项 */
  .statistics .el-row > .el-col {
    margin-bottom: 16px;
  }
  .statistics .el-row > .el-col:last-child {
    margin-bottom: 0;
  }
}
</style>
