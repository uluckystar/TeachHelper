<template>
  <div class="exam-rubric-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <el-page-header @back="handleBack">
        <template #content>
          <span class="page-title">AI评分标准管理（可手动可AI）</span>
        </template>
      </el-page-header>
    </div>

    <!-- 考试信息卡片 -->
    <el-card class="exam-info-card" v-if="examInfo">
      <template #header>
        <div class="card-header">
          <span>考试信息</span>
          <el-tag :type="getExamStatusType(examInfo.status || 'active') as any">
            {{ getExamStatusText(examInfo.status || 'active') }}
          </el-tag>
        </div>
      </template>
      <div class="exam-info">
        <h3>{{ examInfo.title }}</h3>
        <p class="exam-description">{{ examInfo.description }}</p>
        <div class="exam-meta">
          <el-row :gutter="16">
            <el-col :span="8">
              <div class="meta-item">
                <el-icon><Document /></el-icon>
                <span>题目数量：{{ questions.length }}</span>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="meta-item">
                <el-icon><User /></el-icon>
                <span>创建者：{{ examInfo.createdBy }}</span>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="meta-item">
                <el-icon><Calendar /></el-icon>
                <span>创建时间：{{ formatDate(examInfo.createdAt) }}</span>
              </div>
            </el-col>
          </el-row>
        </div>
      </div>
    </el-card>

    <!-- 题目选择和操作工具栏 -->
    <el-card class="operations-card">
      <template #header>
        <div class="card-header">
          <span>评分标准管理</span>
          <div class="header-actions">
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
      </template>

      <!-- 题目选择 -->
      <div class="question-selection">
        <div class="selection-header">
          <h4>选择要管理评分标准的题目：</h4>
          <div class="selection-controls">
            <el-button 
              size="small" 
              @click="selectAllQuestions"
              :disabled="questions.length === 0"
            >
              全选
            </el-button>
            <el-button 
              size="small" 
              @click="clearSelection"
              :disabled="selectedQuestions.length === 0"
            >
              清空
            </el-button>
            <span class="selection-info">
              已选择 {{ selectedQuestions.length }} / {{ questions.length }} 个题目
            </span>
          </div>
        </div>

        <div v-if="questions.length === 0" class="empty-state">
          <el-empty description="该考试暂无题目">
            <el-button type="primary" @click="navigateToAddQuestion">添加题目</el-button>
          </el-empty>
        </div>

        <div v-else class="questions-list">
          <el-checkbox-group v-model="selectedQuestions" class="questions-grid">
            <div 
              v-for="(question, index) in questions" 
              :key="question.id"
              class="question-item"
              :class="{ 'selected': selectedQuestions.includes(question.id) }"
            >
              <el-checkbox :label="question.id" class="question-checkbox">
                <div class="question-content">
                  <div class="question-header">
                    <el-tag 
                      :type="getQuestionTypeTag(question.questionType)"
                      size="small"
                    >
                      {{ getQuestionTypeText(question.questionType) }}
                    </el-tag>
                    <span class="question-title">题目 {{ index + 1 }}: {{ question.title }}</span>
                  </div>
                  <div class="question-meta">
                    <span>满分: {{ question.maxScore }} 分</span>
                    <span>评分标准: {{ getRubricCount(question.id) }} 个</span>
                  </div>
                  <div class="question-actions">
                    <el-button 
                      size="small" 
                      type="primary" 
                      icon="Setting"
                      @click.stop="manageQuestionRubric(question)"
                    >
                      管理评分标准
                    </el-button>
                  </div>
                </div>
              </el-checkbox>
            </div>
          </el-checkbox-group>
        </div>
      </div>
    </el-card>

    <!-- AI生成进度 -->
    <el-card v-if="aiGenerating" class="ai-progress-card">
      <template #header>
        <div class="card-header">
          <span>AI生成进度</span>
          <el-button 
            type="danger" 
            size="small"
            @click="cancelAIGeneration"
          >
            取消生成
          </el-button>
        </div>
      </template>
      
      <div class="ai-progress">
        <el-progress 
          :percentage="aiProgress" 
          :status="aiProgress === 100 ? 'success' : undefined"
        />
        <p class="progress-text">{{ aiProgressText }}</p>
        
        <div v-if="aiResults.length > 0" class="ai-results">
          <h4>生成结果预览：</h4>
          <div class="results-list">
            <div v-for="result in aiResults" :key="result.questionId" class="result-item">
              <h5>{{ getQuestionTitle(result.questionId) }}</h5>
              <div class="rubrics">
                <el-tag 
                  v-for="rubric in result.rubrics" 
                  :key="rubric.criterion"
                  class="rubric-tag"
                >
                  {{ rubric.criterion }} ({{ rubric.points }}分)
                </el-tag>
              </div>
            </div>
          </div>
          
          <div class="ai-actions">
            <el-button 
              type="primary" 
              @click="applyAIResults"
              :loading="applying"
            >
              应用所有生成的评分标准
            </el-button>
            <el-button @click="clearAIResults">
              重新生成
            </el-button>
          </div>
        </div>
      </div>
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
      width="600px"
      destroy-on-close
    >
      <div class="create-rubric-form">
        <el-form :model="newRubric" :rules="rubricRules" ref="rubricFormRef" label-width="100px">
          <el-form-item label="选择题目" prop="questionId">
            <el-select v-model="newRubric.questionId" placeholder="请选择题目" style="width: 100%">
              <el-option
                v-for="question in selectedQuestionsData"
                :key="question.id"
                :label="`题目${getQuestionIndex(question.id)}: ${question.title}`"
                :value="question.id"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="评分标准" prop="criterionText">
            <el-input 
              v-model="newRubric.criterionText" 
              placeholder="请输入评分标准名称"
            />
          </el-form-item>
          
          <el-form-item label="分数" prop="points">
            <el-input-number 
              v-model="newRubric.points" 
              :min="1" 
              :max="getMaxScoreForQuestion(newRubric.questionId)"
              placeholder="分数"
            />
          </el-form-item>
          
          <el-form-item label="详细描述" prop="description">
            <el-input 
              v-model="newRubric.description" 
              type="textarea" 
              :rows="3"
              placeholder="请输入评分标准的详细描述"
            />
          </el-form-item>
        </el-form>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCreateRubricDialog = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="createRubric"
            :loading="creating"
          >
            创建
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
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
  ArrowLeft
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

// AI生成相关
const aiProgress = ref(0)
const aiProgressText = ref('')
const aiResults = ref<any[]>([])

// 对话框状态
const showCreateRubricDialog = ref(false)

// 表单数据
const newRubric = reactive({
  questionId: undefined as number | undefined,
  criterionText: '',
  points: 1,
  description: ''
})

const rubricFormRef = ref()

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
    
    for (const questionId of selectedQuestions.value) {
      try {
        aiProgressText.value = `正在为题目 ${getQuestionIndex(questionId)} 生成评分标准...`
        
        const suggestions = await questionApi.generateRubric(questionId)
        
        aiResults.value.push({
          questionId,
          rubrics: suggestions.map((s: any) => ({
            criterion: s.criterionText,
            points: s.points
          }))
        })
        
        completed++
        aiProgress.value = Math.round((completed / total) * 100)
        
      } catch (error) {
        console.error(`Failed to generate rubric for question ${questionId}:`, error)
        ElMessage.warning(`题目 ${getQuestionIndex(questionId)} 评分标准生成失败`)
      }
    }
    
    aiProgressText.value = '所有评分标准生成完成！'
    ElMessage.success('AI评分标准生成完成')
    
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
    
    for (const result of aiResults.value) {
      const suggestions = result.rubrics.map((r: any) => ({
        criterionText: r.criterion,
        points: r.points
      }))
      
      await questionApi.applyRubricSuggestions(result.questionId, suggestions)
    }
    
    ElMessage.success('所有评分标准应用成功')
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

const getQuestionTypeText = (type: string) => {
  const typeMap = {
    'ESSAY': '论述题',
    'SHORT_ANSWER': '简答题',
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'TRUE_FALSE': '判断题',
    'CODING': '编程题',
    'CASE_ANALYSIS': '案例分析题'
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
    'CASE_ANALYSIS': 'primary'
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

.exam-info-card,
.operations-card,
.ai-progress-card,
.rubrics-overview-card {
  margin-bottom: 20px;
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

.exam-info {
  text-align: center;
}

.exam-info h3 {
  margin: 0 0 8px 0;
  color: #303133;
}

.exam-description {
  color: #606266;
  margin-bottom: 16px;
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
