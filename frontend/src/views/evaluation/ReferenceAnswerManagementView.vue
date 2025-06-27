<template>
    <div class="exam-reference-answer-management">
      <!-- 页面标题 -->
      <div class="page-header">
        <el-page-header @back="handleBack">
          <template #content>
            <span class="page-title">参考答案管理</span>
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
            <p class="progress-text">{{ aiProgressText || `参考答案生成完成！成功生成 ${aiResults.length} 个题目的参考答案` }}</p>
          </div>
          
          <!-- 成功完成提示 -->
          <div v-if="!aiGenerating && aiResults.length > 0" class="success-message">
            <el-alert
              :title="`AI参考答案生成完成！成功生成 ${aiResults.length} 个题目的参考答案`"
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
                  <el-tag size="small" type="info">{{ result.referenceAnswer ? result.referenceAnswer.length : 0 }}字符</el-tag>
                </div>
                <div class="answer-preview">
                  <el-text class="answer-content" truncated>
                    {{ result.referenceAnswer || '暂无内容' }}
                  </el-text>
                </div>
              </div>
            </div>
            
            <div class="ai-actions" style="margin-top: 20px; padding: 15px; background: #f8f9fa; border-radius: 8px;">
              <el-alert
                title="请确认生成的参考答案是否符合要求，然后点击应用按钮保存到数据库"
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
                  {{ applying ? '应用中...' : '应用所有参考答案' }}
                </el-button>
                <el-button 
                  size="large"
                  @click="generateAIReferenceAnswers"
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
              @click="showCreateAnswerDialog = true"
              :disabled="selectedQuestions.length === 0"
            >
              手动创建参考答案
            </el-button>
            <el-button 
              type="success" 
              icon="MagicStick"
              @click="generateAIReferenceAnswers"
              :disabled="selectedQuestions.length === 0"
              :loading="aiGenerating"
            >
              AI生成参考答案
            </el-button>
          </div>
        </div>
      </el-card>
  
      <!-- 题目列表 -->
      <el-card class="questions-card" v-if="questions.length > 0">
        <template #header>
          <div class="card-header">
            <span>题目列表</span>
            <div class="header-actions">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索题目..."
                clearable
                style="width: 200px; margin-right: 10px;"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-button 
                icon="Refresh" 
                @click="loadData"
                :loading="loading"
              >
                刷新
              </el-button>
            </div>
          </div>
        </template>
  
        <div class="questions-table">
          <el-table 
            :data="paginatedQuestions" 
            border 
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column label="题目编号" width="100" align="center">
              <template #default="{ row }">
                <span>题目{{ getQuestionIndex(row.id) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="题目标题" min-width="200" />
            <el-table-column prop="questionType" label="题目类型" width="120" align="center">
              <template #default="{ row }">
                <el-tag size="small">{{ getQuestionTypeText(row.questionType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="maxScore" label="满分" width="80" align="center" />
            <el-table-column label="参考答案" width="120" align="center">
              <template #default="{ row }">
                <el-tag 
                  :type="hasReferenceAnswer(row) ? 'success' : 'info'"
                  size="small"
                >
                  {{ hasReferenceAnswer(row) ? '已设置' : '未设置' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" align="center" fixed="right">
              <template #default="{ row }">
                <el-button 
                  type="primary" 
                  size="small"
                  @click="manageQuestionReferenceAnswer(row)"
                >
                  管理参考答案
                </el-button>
                <el-button 
                  v-if="hasReferenceAnswer(row)"
                  type="success" 
                  size="small"
                  @click="previewReferenceAnswer(row)"
                >
                  预览
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
  

  
      <!-- 创建参考答案对话框 -->
      <el-dialog
        v-model="showCreateAnswerDialog"
        title="创建参考答案"
        width="80%"
        :close-on-click-modal="false"
      >
        <div v-if="selectedQuestions.length === 0" class="no-selection-warning">
          <el-alert
            title="请先选择题目"
            description="您需要先在题目列表中选择要创建参考答案的题目"
            type="warning"
            show-icon
            :closable="false"
          />
        </div>
        
        <div v-else class="create-answer-form">
          <el-tabs v-model="activeAnswerTab" type="border-card">
            <!-- 单个题目创建 -->
            <el-tab-pane label="单个创建" name="single">
              <el-form :model="newAnswer" :rules="answerRules" ref="answerFormRef" label-width="120px">
                <el-form-item label="选择题目" prop="questionId">
                  <el-select 
                    v-model="newAnswer.questionId" 
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
                  <p><strong>题目内容：</strong></p>
                  <div class="question-content">{{ selectedQuestionInfo.content }}</div>
                  <el-divider />
                </div>
                
                <el-form-item label="参考答案" prop="referenceAnswer">
                  <el-input 
                    v-model="newAnswer.referenceAnswer" 
                    type="textarea"
                    :rows="8"
                    placeholder="请输入参考答案..."
                    maxlength="2000"
                    show-word-limit
                  />
                </el-form-item>
              </el-form>
            </el-tab-pane>
            
            <!-- 批量创建 -->
            <el-tab-pane label="批量AI生成" name="batch" v-if="selectedQuestions.length > 1">
              <div class="batch-create-form">
                <el-alert
                  title="批量AI生成提示"
                  description="将为所有选中的题目使用AI生成参考答案"
                  type="info"
                  show-icon
                  :closable="false"
                />
                
                <div class="batch-preview" style="margin-top: 20px;">
                  <h4>生成预览</h4>
                  <el-table :data="selectedQuestionsData" size="small" border>
                    <el-table-column label="题目" prop="title" min-width="200" />
                    <el-table-column label="题目类型" prop="questionType" width="120" align="center">
                      <template #default="{ row }">
                        <el-tag size="small">{{ getQuestionTypeText(row.questionType) }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="满分" prop="maxScore" width="80" align="center" />
                    <el-table-column label="状态" width="100" align="center">
                      <template #default="{ row }">
                        <el-tag 
                          :type="hasReferenceAnswer(row) ? 'warning' : 'success'"
                          size="small"
                        >
                          {{ hasReferenceAnswer(row) ? '将覆盖' : '新建' }}
                        </el-tag>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
        
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="showCreateAnswerDialog = false">取消</el-button>
            <el-button 
              type="primary" 
              @click="activeAnswerTab === 'single' ? createAnswer() : createBatchAnswers()"
              :loading="creating"
              :disabled="selectedQuestions.length === 0"
            >
              {{ activeAnswerTab === 'single' ? '创建' : `批量生成 (${selectedQuestions.length}个)` }}
            </el-button>
          </div>
        </template>
      </el-dialog>
  
      <!-- 预览参考答案对话框 -->
      <el-dialog
        v-model="showPreviewDialog"
        title="参考答案预览"
        width="70%"
      >
        <div v-if="previewAnswer" class="answer-preview-content">
          <div class="question-info">
            <h4>{{ previewAnswer.questionTitle }}</h4>
            <div class="question-meta">
              <el-tag size="small">{{ previewAnswer.questionType }}</el-tag>
              <span>满分: {{ previewAnswer.maxScore }}分</span>
            </div>
          </div>
          <el-divider />
          <div class="answer-content">
            <h5>参考答案：</h5>
            <div class="answer-text">{{ previewAnswer.referenceAnswer }}</div>
          </div>
        </div>
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
    Warning,
    Search,
    View,
    Close
  } from '@element-plus/icons-vue'
  
  import { examApi } from '@/api/exam'
import { questionApi } from '@/api/question'
import type { 
  ExamResponse, 
  QuestionResponse,
  QuestionCreateRequest
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
  const answersOverview = ref<any[]>([])
  
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
  const showCreateAnswerDialog = ref(false)
  const showPreviewDialog = ref(false)
  const activeAnswerTab = ref('single')
  
  // 表单数据
  const newAnswer = reactive({
    questionId: undefined as number | undefined,
    referenceAnswer: ''
  })
  
  const previewAnswer = ref<any>(null)
  
  const answerFormRef = ref()
  
  // 表单验证规则
  const answerRules = {
    questionId: [
      { required: true, message: '请选择题目', trigger: 'change' }
    ],
    referenceAnswer: [
      { required: true, message: '请输入参考答案', trigger: 'blur' },
      { min: 10, message: '参考答案不能少于10个字符', trigger: 'blur' }
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
    if (!newAnswer.questionId) return null
    return questions.value.find(q => q.id === newAnswer.questionId)
  })
  
  // 统计方法
  const getQuestionsWithReferenceAnswers = () => {
    return questions.value.filter(q => q.referenceAnswer && q.referenceAnswer.trim().length > 0).length
  }
  
  const getAnswerCoveragePercentage = () => {
    if (questions.value.length === 0) return 0
    const questionsWithAnswers = getQuestionsWithReferenceAnswers()
    return Math.round((questionsWithAnswers / questions.value.length) * 100)
  }
  
  const getTotalAnswerLength = () => {
    const questionsWithAnswers = questions.value.filter(q => q.referenceAnswer && q.referenceAnswer.trim().length > 0)
    if (questionsWithAnswers.length === 0) return 0
    const totalLength = questionsWithAnswers.reduce((sum, q) => sum + (q.referenceAnswer?.length || 0), 0)
    return Math.round(totalLength / questionsWithAnswers.length)
  }
  
  // 生命周期
  onMounted(async () => {
    await loadData()
  })
  
  // 方法
  const loadData = async () => {
    try {
      loading.value = true
      await Promise.all([
        loadExamInfo(),
        loadQuestions(),
        loadAnswersOverview()
      ])
    } catch (error) {
      console.error('加载数据失败:', error)
      ElMessage.error('加载数据失败')
    } finally {
      loading.value = false
    }
  }
  
  const loadExamInfo = async () => {
    try {
      examInfo.value = await examApi.getExam(examId.value)
    } catch (error) {
      console.error('加载考试信息失败:', error)
    }
  }
  
  const loadQuestions = async () => {
    try {
      const questionsData = await questionApi.getQuestionsByExam(examId.value)
      questions.value = questionsData || []
    } catch (error) {
      console.error('加载题目失败:', error)
      questions.value = []
    }
  }
  
  const loadAnswersOverview = async () => {
    try {
      overviewLoading.value = true
      // 构建参考答案总览数据
      answersOverview.value = questions.value.map(question => ({
        questionId: question.id,
        questionTitle: question.title,
        questionType: getQuestionTypeText(question.questionType),
        maxScore: question.maxScore,
        referenceAnswer: question.referenceAnswer,
        answerLength: question.referenceAnswer?.length || 0
      }))
    } catch (error) {
      console.error('加载参考答案总览失败:', error)
    } finally {
      overviewLoading.value = false
    }
  }
  
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
  
  const handleSelectionChange = (selection: QuestionResponse[]) => {
    selectedQuestions.value = selection.map(q => q.id)
    updateSelectAllState()
  }
  
  const updateSelectAllState = () => {
    if (selectedQuestions.value.length === 0) {
      selectAll.value = false
      isIndeterminate.value = false
    } else if (selectedQuestions.value.length === questions.value.length) {
      selectAll.value = true
      isIndeterminate.value = false
    } else {
      selectAll.value = false
      isIndeterminate.value = true
    }
  }
  
  const handleSelectAll = (checked: any) => {
    if (checked) {
      selectedQuestions.value = questions.value.map(q => q.id)
    } else {
      selectedQuestions.value = []
    }
    isIndeterminate.value = false
  }
  
  const selectAllQuestions = () => {
    selectedQuestions.value = questions.value.map(q => q.id)
    updateSelectAllState()
    showCreateAnswerDialog.value = true
  }
  
  // AI生成参考答案
  const generateAIReferenceAnswers = async () => {
    if (selectedQuestions.value.length === 0) {
      ElMessage.warning('请先选择要生成参考答案的题目')
      return
    }
  
    try {
      aiGenerating.value = true
      aiProgress.value = 0
      aiProgressText.value = '开始生成参考答案...'
      aiResults.value = []
  
      const totalQuestions = selectedQuestions.value.length
      let completedCount = 0
  
      for (const questionId of selectedQuestions.value) {
        try {
          aiProgressText.value = `正在为题目 ${completedCount + 1}/${totalQuestions} 生成参考答案...`
          
          const result = await questionApi.generateReferenceAnswer(questionId)
          
          aiResults.value.push({
            questionId,
            referenceAnswer: result.referenceAnswer,
            success: true
          })
          
          completedCount++
          aiProgress.value = Math.round((completedCount / totalQuestions) * 100)
          
        } catch (error) {
          console.error(`题目 ${questionId} 生成参考答案失败:`, error)
          aiResults.value.push({
            questionId,
            referenceAnswer: null,
            success: false,
            error: error instanceof Error ? error.message : '生成失败'
          })
          completedCount++
          aiProgress.value = Math.round((completedCount / totalQuestions) * 100)
        }
      }
  
      aiProgressText.value = `参考答案生成完成！成功生成 ${aiResults.value.filter(r => r.success).length}/${totalQuestions} 个`
      ElMessage.success('AI参考答案生成完成')
      
    } catch (error) {
      console.error('AI生成参考答案失败:', error)
      ElMessage.error('AI生成参考答案失败')
    } finally {
      aiGenerating.value = false
    }
  }
  
  const applyAIResults = async () => {
    if (aiResults.value.length === 0) return
  
    try {
      applying.value = true
      
      const successResults = aiResults.value.filter(r => r.success && r.referenceAnswer)
      let appliedCount = 0
      
      for (const result of successResults) {
        try {
          // 先获取完整的题目信息
          const question = questions.value.find(q => q.id === result.questionId)
          if (question) {
            const updateRequest: QuestionCreateRequest = {
              title: question.title,
              content: question.content,
              questionType: question.questionType,
              maxScore: question.maxScore,
              examId: question.examId || examId.value,
              referenceAnswer: result.referenceAnswer,
              options: question.options
            }
            await questionApi.updateQuestion(result.questionId, updateRequest)
            appliedCount++
          }
        } catch (error) {
          console.error(`应用题目 ${result.questionId} 的参考答案失败:`, error)
        }
      }
      
      if (appliedCount > 0) {
        ElMessage.success(`成功应用 ${appliedCount} 个参考答案`)
        await loadData() // 重新加载数据
      } else {
        ElMessage.error('没有参考答案被成功应用')
      }
      
      clearAIResults()
      
    } catch (error) {
      console.error('应用AI结果失败:', error)
      ElMessage.error('应用参考答案失败')
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
  
  const createAnswer = async () => {
    try {
      await answerFormRef.value?.validate()
      
      creating.value = true
      
      // 先获取完整的题目信息
      const question = questions.value.find(q => q.id === newAnswer.questionId)
      if (question) {
        const updateRequest: QuestionCreateRequest = {
          title: question.title,
          content: question.content,
          questionType: question.questionType,
          maxScore: question.maxScore,
          examId: question.examId || examId.value,
          referenceAnswer: newAnswer.referenceAnswer,
          options: question.options
        }
        await questionApi.updateQuestion(newAnswer.questionId!, updateRequest)
      }
      
      ElMessage.success('参考答案创建成功')
      showCreateAnswerDialog.value = false
      resetAnswerForm()
      await loadData()
      
    } catch (error) {
      console.error('创建参考答案失败:', error)
      ElMessage.error('创建参考答案失败')
    } finally {
      creating.value = false
    }
  }
  
  const createBatchAnswers = async () => {
    await generateAIReferenceAnswers()
    showCreateAnswerDialog.value = false
  }
  
  const resetAnswerForm = () => {
    newAnswer.questionId = undefined
    newAnswer.referenceAnswer = ''
  }
  
  const onQuestionSelected = (questionId: number) => {
    console.log('Selected question:', questionId)
  }
  
  const manageQuestionReferenceAnswer = (question: QuestionResponse) => {
    router.push(`/questions/${question.id}/reference-answer`)
  }
  
  const previewReferenceAnswer = (question: QuestionResponse) => {
    previewAnswer.value = {
      questionTitle: question.title,
      questionType: getQuestionTypeText(question.questionType),
      maxScore: question.maxScore,
      referenceAnswer: question.referenceAnswer
    }
    showPreviewDialog.value = true
  }
  
  const viewFullAnswer = (overview: any) => {
    previewAnswer.value = overview
    showPreviewDialog.value = true
  }
  
  const editReferenceAnswer = (overview: any) => {
    router.push(`/questions/${overview.questionId}/reference-answer`)
  }
  
  const deleteReferenceAnswer = async (overview: any) => {
    try {
      await ElMessageBox.confirm(
        '确定要删除这个参考答案吗？此操作不可恢复。',
        '确认删除',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      
      // 先获取完整的题目信息
      const question = questions.value.find(q => q.id === overview.questionId)
      if (question) {
        const updateRequest: QuestionCreateRequest = {
          title: question.title,
          content: question.content,
          questionType: question.questionType,
          maxScore: question.maxScore,
          examId: question.examId || examId.value,
          referenceAnswer: '',
          options: question.options
        }
        await questionApi.updateQuestion(overview.questionId, updateRequest)
      }
      
      ElMessage.success('参考答案删除成功')
      await loadData()
      
    } catch (error) {
      if (error !== 'cancel') {
        console.error('删除参考答案失败:', error)
        ElMessage.error('删除参考答案失败')
      }
    }
  }
  
  const quickCreateAnswer = (questionId: number) => {
    selectedQuestions.value = [questionId]
    newAnswer.questionId = questionId
    showCreateAnswerDialog.value = true
  }
  
  const hasReferenceAnswer = (question: QuestionResponse) => {
    return question.referenceAnswer && question.referenceAnswer.trim().length > 0
  }
  
  const getQuestionIndex = (questionId: number) => {
    const index = questions.value.findIndex(q => q.id === questionId)
    return index >= 0 ? index + 1 : 0
  }
  
  const getQuestionTitle = (questionId: number) => {
    const question = questions.value.find(q => q.id === questionId)
    return question ? question.title : `题目${questionId}`
  }
  
  const getQuestionTypeText = (type: string) => {
    const typeMap: Record<string, string> = {
      'SINGLE_CHOICE': '单选题',
      'MULTIPLE_CHOICE': '多选题',
      'SHORT_ANSWER': '简答题',
      'ESSAY': '论述题',
      'CODING': '编程题',
      'CASE_ANALYSIS': '案例分析题',
      'TRUE_FALSE': '判断题',
      'FILL_BLANK': '填空题',
      'CALCULATION': '计算题'
    }
    return typeMap[type] || type
  }
  
  const getExamStatusType = (status: string) => {
    const statusMap: Record<string, string> = {
      'active': 'success',
      'draft': 'info',
      'archived': 'info',
      'deleted': 'danger'
    }
    return statusMap[status] || 'info'
  }
  
  const getExamStatusText = (status: string) => {
    const statusMap: Record<string, string> = {
      'active': '进行中',
      'draft': '草稿',
      'archived': '已归档',
      'deleted': '已删除'
    }
    return statusMap[status] || status
  }
  
  const formatDate = (date: string | Date) => {
    if (!date) return ''
    const d = new Date(date)
    return d.toLocaleDateString('zh-CN') + ' ' + d.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit'
    })
  }
  
  const navigateToAddQuestion = () => {
    router.push(`/exams/${examId.value}/questions/add`)
  }
  
  // 监听选择变化
  watch(selectedQuestions, () => {
    updateSelectAllState()
  }, { deep: true })
  </script>
  
  <style scoped>
  .exam-reference-answer-management {
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
  
  /* 分页样式 */
  .pagination-wrapper {
    display: flex;
    justify-content: center;
    margin-top: 20px;
  }

  /* 操作工具栏样式 */
  .operations-card {
    margin-bottom: 20px;
  }
  
  .operations {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 16px;
  }
  
  .batch-actions {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }
  
  /* 卡片头部样式 */
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .header-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  
  /* AI进度卡片样式 */
  .ai-progress-card {
    margin-bottom: 20px;
  }
  
  .progress-text {
    margin: 8px 0 0 0;
    color: #666;
  }
  
  .ai-results {
    margin-top: 20px;
  }
  
  .results-list {
    max-height: 300px;
    overflow-y: auto;
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    padding: 10px;
  }
  
  .result-item {
    padding: 10px;
    border-bottom: 1px solid #f0f0f0;
  }
  
  .result-item:last-child {
    border-bottom: none;
  }
  
  .result-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
  }
  
  .result-header h5 {
    margin: 0;
    font-size: 14px;
  }
  
  .answer-preview {
    margin-top: 8px;
  }
  
  .answer-content {
    background: #f8f9fa;
    padding: 8px 12px;
    border-radius: 4px;
    font-size: 13px;
    line-height: 1.4;
  }
  
  .action-buttons {
    display: flex;
    gap: 12px;
    justify-content: center;
  }
  
  /* 题目列表样式 */
  .questions-card {
    margin-bottom: 20px;
  }
  
  /* 总览样式 */
  .answers-overview-card {
    margin-bottom: 20px;
  }
  
  .loading-state, .empty-state {
    padding: 40px;
    text-align: center;
  }
  
  .answers-overview {
    max-height: 600px;
    overflow-y: auto;
  }
  
  .overview-item {
    padding: 16px;
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    margin-bottom: 16px;
  }
  
  .overview-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 12px;
  }
  
  .overview-header h4 {
    margin: 0;
    font-size: 16px;
    color: #303133;
  }
  
  .overview-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 12px;
    color: #909399;
  }
  
  .answer-content {
    background: #f8f9fa;
    padding: 12px;
    border-radius: 6px;
  }
  
  .answer-text {
    margin-bottom: 12px;
    line-height: 1.6;
    color: #606266;
  }
  
  .answer-actions {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }
  
  .no-answer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px;
    background: #f8f9fa;
    border-radius: 6px;
    border: 1px dashed #d9d9d9;
  }
  
  /* 对话框样式 */
  .no-selection-warning {
    padding: 20px;
    text-align: center;
  }
  
  .question-info {
    background: #f8f9fa;
    padding: 16px;
    border-radius: 8px;
    margin-bottom: 20px;
  }
  
  .question-info h4 {
    margin: 0 0 12px 0;
    color: #303133;
  }
  
  .question-content {
    background: white;
    padding: 12px;
    border-radius: 4px;
    border: 1px solid #e4e7ed;
    margin-top: 8px;
    line-height: 1.6;
  }
  
  .question-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 8px;
  }
  
  .batch-preview {
    margin-top: 20px;
  }
  
  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
  }
  
  /* 预览对话框样式 */
  .answer-preview-content .question-info {
    margin-bottom: 16px;
  }
  
  .answer-preview-content .answer-content h5 {
    margin: 0 0 12px 0;
    color: #303133;
  }
  
  .answer-preview-content .answer-text {
    background: #f8f9fa;
    padding: 16px;
    border-radius: 8px;
    line-height: 1.8;
    color: #606266;
    white-space: pre-wrap;
  }
  
  /* 空状态样式 */
  .empty-card {
    text-align: center;
    padding: 60px 20px;
  }
  </style>