<template>
  <div class="question-rubric">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-row :gutter="24" align="middle">
        <el-col :span="18">
          <el-breadcrumb>
            <el-breadcrumb-item :to="{ path: '/exams' }">考试列表</el-breadcrumb-item>
            <el-breadcrumb-item :to="{ path: `/exams/${examId}` }">
              {{ examTitle || '考试详情' }}
            </el-breadcrumb-item>
            <el-breadcrumb-item>{{ questionTitle || '评分标准' }}</el-breadcrumb-item>
          </el-breadcrumb>
          <h1 v-if="questionInfo" class="page-title">评分标准：{{ questionInfo.title }}</h1>
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
              @click="loadRubricCriteria"
              :loading="criteriaLoading"
            >
              刷新数据
            </el-button>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 主要内容区域 -->
    <el-row :gutter="24" v-if="!loading">
      <!-- 左侧：题目信息 -->
      <el-col :span="10">
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

      <!-- 右侧：AI生成和评分标准管理 -->
      <el-col :span="14">
        <!-- AI生成评分标准 -->
        <el-card class="ai-generation-card" style="margin-bottom: 20px;">
          <template #header>
            <div class="card-header">
              <span>AI评分标准生成</span>
              <div class="generation-controls">
                <el-button 
                  type="primary" 
                  :icon="MagicStick" 
                  @click="generateAIRubric"
                  :loading="aiGenerating"
                  :disabled="aiGenerating"
                >
                  {{ aiGenerating ? '生成中...' : 'AI生成评分标准' }}
                </el-button>
                <el-button 
                  v-if="aiGenerating && currentTaskId"
                  type="danger" 
                  size="small"
                  @click="cancelGeneration"
                >
                  取消生成
                </el-button>
              </div>
            </div>
          </template>

          <!-- AI生成进度显示 -->
          <div v-if="aiGenerating" class="generation-progress">
            <div class="progress-header">
              <h4>AI正在生成评分标准...</h4>
              <div class="generation-status">{{ getStatusText(generationStatus) }}</div>
            </div>
            
            <el-progress
              :percentage="generationProgress"
              :status="generationStatus === 'FAILED' ? 'exception' : undefined"
              :stroke-width="8"
              text-inside
            />
            
            <div class="generation-info">
              <div class="info-item">
                <span class="label">任务ID:</span>
                <span class="value">{{ currentTaskId }}</span>
              </div>
              <div class="info-item" v-if="tokenStats.totalTokens > 0">
                <span class="label">Token使用:</span>
                <span class="value">
                  {{ tokenStats.totalTokens }} 总 
                  ({{ tokenStats.promptTokens }} 输入 + {{ tokenStats.completionTokens }} 输出)
                </span>
              </div>
              <div class="info-item" v-if="processingTime > 0">
                <span class="label">处理时间:</span>
                <span class="value">{{ (processingTime / 1000).toFixed(1) }}秒</span>
              </div>
            </div>
          </div>

          <!-- AI生成的建议 -->
          <div v-if="aiSuggestions.length > 0" class="ai-suggestions">
            <div class="suggestions-header">
              <h4>AI生成的评分标准建议</h4>
              <el-button 
                type="success" 
                size="small"
                @click="applyAISuggestions"
                :loading="applyingAI"
              >
                应用所有建议
              </el-button>
            </div>
            
            <div class="suggestions-list">
              <div 
                v-for="(suggestion, index) in aiSuggestions" 
                :key="index"
                class="suggestion-item"
              >
                <div class="suggestion-content">
                  <div class="criterion-text">{{ suggestion.criterionText }}</div>
                  <div class="criterion-points">{{ suggestion.points }}分</div>
                </div>
                <div class="suggestion-actions">
                  <el-button 
                    type="primary" 
                    size="small"
                    @click="applySingleSuggestion(suggestion)"
                  >
                    应用此建议
                  </el-button>
                </div>
              </div>
            </div>
          </div>

          <div v-else-if="!aiGenerating" class="no-suggestions">
            <el-empty description="暂无AI生成的评分标准建议" />
          </div>
        </el-card>

        <!-- 当前评分标准 -->
        <el-card class="current-rubric-card">
          <template #header>
            <div class="card-header">
              <span>当前评分标准</span>
              <div class="header-actions">
                <el-button 
                  type="success" 
                  :icon="Plus" 
                  @click="addNewCriterion"
                  size="small"
                >
                  添加标准
                </el-button>
                <el-button 
                  :icon="Refresh" 
                  @click="loadRubricCriteria"
                  :loading="criteriaLoading"
                  size="small"
                >
                  刷新
                </el-button>
              </div>
            </div>
          </template>

          <div v-if="rubricCriteria.length > 0" class="criteria-list">
            <div 
              v-for="(criterion, index) in rubricCriteria" 
              :key="criterion.id || index"
              class="criterion-item"
            >
              <div class="criterion-content" v-if="!criterion.editing">
                <div class="criterion-text">{{ criterion.criterionText }}</div>
                <div class="criterion-points">{{ criterion.points }}分</div>
                <div class="criterion-description" v-if="criterion.description">
                  {{ criterion.description }}
                </div>
              </div>

              <!-- 编辑模式 -->
              <div class="criterion-edit" v-else>
                <el-form :model="criterion" label-width="80px" size="small">
                  <el-form-item label="评分标准">
                    <el-input 
                      v-model="criterion.criterionText" 
                      placeholder="请输入评分标准描述"
                    />
                  </el-form-item>
                  <el-form-item label="分数">
                    <el-input-number 
                      v-model="criterion.points" 
                      :min="0" 
                      :max="questionInfo?.maxScore || 100"
                      :precision="1"
                      style="width: 120px"
                    />
                  </el-form-item>
                  <el-form-item label="说明">
                    <el-input 
                      v-model="criterion.description" 
                      type="textarea" 
                      :rows="2"
                      placeholder="评分标准的详细说明（可选）"
                    />
                  </el-form-item>
                </el-form>
              </div>

              <div class="criterion-actions">
                <div v-if="!criterion.editing">
                  <el-button 
                    type="primary" 
                    size="small" 
                    :icon="Edit"
                    @click="editCriterion(criterion, index)"
                  >
                    编辑
                  </el-button>
                  <el-button 
                    type="danger" 
                    size="small" 
                    :icon="Delete"
                    @click="deleteCriterion(criterion, index)"
                  >
                    删除
                  </el-button>
                </div>
                <div v-else>
                  <el-button 
                    type="success" 
                    size="small" 
                    :icon="Check"
                    @click="saveCriterion(criterion, index)"
                    :loading="criterion.saving"
                  >
                    保存
                  </el-button>
                  <el-button 
                    size="small" 
                    :icon="Close"
                    @click="cancelEdit(criterion, index)"
                  >
                    取消
                  </el-button>
                </div>
              </div>
            </div>

            <div class="total-points">
              <strong>总分：{{ totalPoints }}分 / {{ questionInfo?.maxScore || 0 }}分</strong>
              <span 
                v-if="totalPoints !== (questionInfo?.maxScore || 0)"
                class="points-warning"
              >
                (分数不匹配)
              </span>
            </div>
          </div>

          <div v-else class="no-criteria">
            <el-empty description="暂无评分标准，建议先使用AI生成" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { 
  ArrowLeft,
  MagicStick, 
  Plus, 
  Refresh, 
  Edit, 
  Delete, 
  Check, 
  Close
} from '@element-plus/icons-vue'

import { questionApi } from '@/api/question'
import { rubricApi } from '@/api/rubric'
import type { 
  Question, 
  RubricCriterion,
  RubricSuggestionItem,
  AIGenerationTaskResponse,
  AIGenerationStatusResponse
} from '@/types/api'

const route = useRoute()
const router = useRouter()

// 响应式数据
const loading = ref(true)
const criteriaLoading = ref(false)
const aiGenerating = ref(false)
const applyingAI = ref(false)
const questionInfo = ref<Question | null>(null)
const rubricCriteria = ref<(RubricCriterion & { editing?: boolean, saving?: boolean, originalData?: any })[]>([])
const aiSuggestions = ref<RubricSuggestionItem[]>([])

// 流式生成相关状态
const currentTaskId = ref<string | null>(null)
const generationStatus = ref<'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'>('PENDING')
const generationProgress = ref(0)
const tokenStats = ref({
  totalTokens: 0,
  promptTokens: 0,
  completionTokens: 0
})
const processingTime = ref(0)
const statusPollingInterval = ref<number | null>(null)

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

const totalPoints = computed(() => {
  return rubricCriteria.value.reduce((sum, criterion) => sum + (criterion.points || 0), 0)
})

// 生命周期
onMounted(async () => {
  await loadData()
})

// 方法
const loadData = async () => {
  try {
    loading.value = true
    await Promise.all([
      loadQuestionInfo(),
      loadRubricCriteria()
    ])
  } catch (error) {
    console.error('加载数据失败:', error)
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

const loadRubricCriteria = async () => {
  try {
    criteriaLoading.value = true
    const criteria = await rubricApi.getCriteriaByQuestion(questionId.value)
    rubricCriteria.value = criteria.map(criterion => ({
      ...criterion,
      editing: false,
      saving: false
    }))
  } catch (error) {
    console.error('加载评分标准失败:', error)
  } finally {
    criteriaLoading.value = false
  }
}

const generateAIRubric = async () => {
  // 重置状态
  resetGenerationState()
  aiGenerating.value = true
  
  try {
    console.log('🚀 开始AI生成评分标准...')
    
    // 首先尝试异步生成
    try {
      const taskResponse = await questionApi.generateRubricAsync(questionId.value)
      currentTaskId.value = taskResponse.taskId
      generationStatus.value = 'PENDING'
      
      console.log('✅ 异步任务创建成功:', {
        taskId: taskResponse.taskId,
        questionId: questionId.value
      })
      
      ElNotification.info({
        title: '开始AI生成',
        message: `任务已创建，ID: ${taskResponse.taskId}`
      })
      
      // 开始轮询状态
      startStatusPolling()
      
    } catch (asyncError) {
      console.warn('❌ 异步生成失败，尝试同步生成:', asyncError)
      
      // 如果异步生成失败，尝试同步生成
      try {
        ElNotification.info({
          title: '正在生成评分标准',
          message: '使用备用方案，请稍候...'
        })
        
        const suggestions = await questionApi.generateRubric(questionId.value)
        
        if (suggestions && suggestions.length > 0) {
          aiSuggestions.value = suggestions
          ElNotification.success({
            title: 'AI生成完成',
            message: `生成了 ${suggestions.length} 个评分标准建议`,
            duration: 3000
          })
          console.log('✅ 同步生成成功:', suggestions)
        } else {
          throw new Error('生成结果为空')
        }
        
      } catch (syncError) {
        console.error('❌ 同步生成也失败:', syncError)
        throw syncError
      }
    }
    
  } catch (error) {
    console.error('❌ AI生成评分标准失败:', error)
    ElMessage.error('AI生成评分标准失败: ' + (error as Error).message)
    aiGenerating.value = false
  }
}

// 重置生成状态
const resetGenerationState = () => {
  currentTaskId.value = null
  generationStatus.value = 'PENDING'
  generationProgress.value = 0
  tokenStats.value = {
    totalTokens: 0,
    promptTokens: 0,
    completionTokens: 0
  }
  processingTime.value = 0
  stopStatusPolling()
}

// 开始状态轮询
const startStatusPolling = () => {
  if (!currentTaskId.value) return
  
  statusPollingInterval.value = setInterval(async () => {
    await checkGenerationStatus()
  }, 2000) // 每2秒检查一次
}

// 停止状态轮询
const stopStatusPolling = () => {
  if (statusPollingInterval.value) {
    clearInterval(statusPollingInterval.value)
    statusPollingInterval.value = null
  }
}

// 检查生成状态
const checkGenerationStatus = async () => {
  if (!currentTaskId.value) return
  
  try {
    const status = await questionApi.getGenerationStatus(currentTaskId.value)
    
    console.log('🔍 检查AI生成状态:', {
      taskId: currentTaskId.value,
      status: status.status,
      progress: status.progress,
      suggestions: status.suggestions ? status.suggestions.length : 0,
      error: status.error
    })
    
    generationStatus.value = status.status
    generationProgress.value = status.progress || 0
    
    // 更新token统计
    if (status.totalTokens) {
      tokenStats.value.totalTokens = status.totalTokens
      tokenStats.value.promptTokens = status.promptTokens || 0
      tokenStats.value.completionTokens = status.completionTokens || 0
    }
    
    // 更新处理时间
    if (status.processingTimeMs) {
      processingTime.value = status.processingTimeMs
    }
    
    // 检查是否完成
    if (status.status === 'COMPLETED') {
      stopStatusPolling()
      aiGenerating.value = false
      
      console.log('✅ AI生成完成，检查建议:', {
        hasSuggestions: !!status.suggestions,
        suggestionsLength: status.suggestions ? status.suggestions.length : 0,
        statusObject: status
      })
      
      if (status.suggestions && status.suggestions.length > 0) {
        aiSuggestions.value = status.suggestions
        ElNotification.success({
          title: 'AI生成完成',
          message: `生成了 ${aiSuggestions.value.length} 个评分标准建议`,
          duration: 3000
        })
      } else {
        console.warn('⚠️ AI生成完成但没有建议数据')
        // 尝试使用备用API直接获取建议
        try {
          console.log('🔄 尝试使用备用API获取建议...')
          const backupSuggestions = await questionApi.generateRubric(questionId.value)
          if (backupSuggestions && backupSuggestions.length > 0) {
            aiSuggestions.value = backupSuggestions
            ElNotification.success({
              title: 'AI生成完成',
              message: `生成了 ${aiSuggestions.value.length} 个评分标准建议`,
              duration: 3000
            })
          } else {
            ElNotification.warning({
              title: 'AI生成完成',
              message: '但未能生成有效的评分标准建议，请重试',
              duration: 5000
            })
          }
        } catch (backupError) {
          console.error('备用API也失败:', backupError)
          ElNotification.warning({
            title: 'AI生成完成',
            message: '但获取建议数据失败，请刷新页面重试',
            duration: 5000
          })
        }
      }
    } else if (status.status === 'FAILED' || status.status === 'CANCELLED') {
      stopStatusPolling()
      aiGenerating.value = false
      
      const message = status.status === 'FAILED' ? 'AI生成失败' : 'AI生成已取消'
      ElNotification.error({
        title: message,
        message: status.error || '请稍后重试',
        duration: 5000
      })
    }
  } catch (error: any) {
    console.error('检查生成状态失败:', error)
    // 如果是网络错误，继续轮询
    // 如果是其他错误，停止轮询
    if (!error.toString().includes('network')) {
      stopStatusPolling()
      aiGenerating.value = false
      ElMessage.error('检查生成状态失败')
    }
  }
}

// 取消生成
const cancelGeneration = async () => {
  if (!currentTaskId.value) return
  
  try {
    await questionApi.cancelGenerationTask(currentTaskId.value)
    stopStatusPolling()
    aiGenerating.value = false
    generationStatus.value = 'CANCELLED'
    
    ElMessage.success('AI生成已取消')
  } catch (error) {
    console.error('取消生成失败:', error)
    ElMessage.error('取消生成失败')
  }
}

// 获取状态文本
const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'PENDING': '等待中...',
    'PROCESSING': '处理中...',
    'COMPLETED': '已完成',
    'FAILED': '生成失败',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || status
}

const applyAISuggestions = async () => {
  if (aiSuggestions.value.length === 0) return
  
  try {
    const result = await ElMessageBox.confirm(
      `确定要应用所有 ${aiSuggestions.value.length} 个AI建议吗？这将替换当前的评分标准。`,
      '确认应用AI建议',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    if (result === 'confirm') {
      applyingAI.value = true
      await questionApi.applyRubricSuggestions(questionId.value, aiSuggestions.value)
      
      ElMessage.success('AI建议应用成功')
      await loadRubricCriteria()
      
      // 清空AI建议
      aiSuggestions.value = []
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('应用AI建议失败:', error)
      ElMessage.error('应用AI建议失败')
    }
  } finally {
    applyingAI.value = false
  }
}

const applySingleSuggestion = async (suggestion: RubricSuggestionItem) => {
  try {
    await questionApi.applyRubricSuggestions(questionId.value, [suggestion])
    
    ElMessage.success('单个建议应用成功')
    await loadRubricCriteria()
    
    // 从建议列表中移除已应用的建议
    const index = aiSuggestions.value.findIndex((s) => 
      s.criterionText === suggestion.criterionText && s.points === suggestion.points
    )
    if (index > -1) {
      aiSuggestions.value.splice(index, 1)
    }
  } catch (error) {
    console.error('应用单个建议失败:', error)
    ElMessage.error('应用建议失败')
  }
}

const addNewCriterion = () => {
  const newCriterion: RubricCriterion = {
    id: 0, // 新创建的标准ID为0
    criterionText: '',
    points: 0,
    description: '',
    editing: true,
    saving: false,
    questionId: questionId.value
  }
  rubricCriteria.value.push(newCriterion)
}

const editCriterion = (criterion: any, index: number) => {
  // 保存原始数据用于取消编辑
  criterion.originalData = {
    criterionText: criterion.criterionText,
    points: criterion.points,
    description: criterion.description
  }
  criterion.editing = true
}

const cancelEdit = (criterion: any, index: number) => {
  if (criterion.id === 0) {
    // 新创建的标准，直接删除
    rubricCriteria.value.splice(index, 1)
  } else {
    // 恢复原始数据
    if (criterion.originalData) {
      criterion.criterionText = criterion.originalData.criterionText
      criterion.points = criterion.originalData.points
      criterion.description = criterion.originalData.description
    }
    criterion.editing = false
    criterion.originalData = null
  }
}

const saveCriterion = async (criterion: any, index: number) => {
  if (!criterion.criterionText || criterion.points <= 0) {
    ElMessage.error('请填写完整的评分标准信息')
    return
  }
  
  criterion.saving = true
  try {
    if (criterion.id === 0) {
      // 新创建的标准
      const newCriterion = await rubricApi.createCriterion({
        questionId: questionId.value,
        criterionText: criterion.criterionText,
        points: criterion.points,
        description: criterion.description
      })
      
      // 更新本地数据
      Object.assign(criterion, newCriterion)
    } else {
      // 更新现有标准
      const updatedCriterion = await rubricApi.updateCriterion(criterion.id, {
        criterionText: criterion.criterionText,
        points: criterion.points,
        description: criterion.description
      })
      
      Object.assign(criterion, updatedCriterion)
    }
    
    criterion.editing = false
    criterion.originalData = null
    ElMessage.success('评分标准保存成功')
  } catch (error) {
    console.error('保存评分标准失败:', error)
    ElMessage.error('保存评分标准失败')
  } finally {
    criterion.saving = false
  }
}

const deleteCriterion = async (criterion: any, index: number) => {
  try {
    const result = await ElMessageBox.confirm(
      '确定要删除这个评分标准吗？',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    if (result === 'confirm') {
      if (criterion.id === 0) {
        // 新创建的标准，直接从数组中删除
        rubricCriteria.value.splice(index, 1)
      } else {
        // 已存在的标准，调用API删除
        await rubricApi.deleteCriterion(criterion.id)
        rubricCriteria.value.splice(index, 1)
        ElMessage.success('评分标准删除成功')
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除评分标准失败:', error)
      ElMessage.error('删除评分标准失败')
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

// 辅助方法
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

// 清理资源
onUnmounted(() => {
  stopStatusPolling()
  resetGenerationState()
})
</script>

<style scoped>
.question-rubric {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
}

.page-header h1,
.page-title {
  margin: 8px 0 0 0;
  color: #303133;
  font-size: 24px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-actions-col {
  display: flex;
  justify-content: flex-end;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  white-space: nowrap;
}

.question-info-card,
.ai-generation-card,
.current-rubric-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
}

.generation-controls {
  display: flex;
  gap: 10px;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.question-content h3 {
  margin: 0 0 10px 0;
  color: #303133;
}

.question-text {
  color: #606266;
  line-height: 1.6;
  margin: 0 0 15px 0;
}

.question-meta {
  display: flex;
  align-items: center;
  gap: 15px;
}

.max-score {
  color: #909399;
  font-size: 14px;
}

.generation-progress {
  margin-bottom: 20px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.progress-header h4 {
  margin: 0;
  color: #303133;
}

.generation-status {
  font-size: 14px;
  color: #409eff;
  font-weight: 500;
}

.generation-info {
  margin-top: 15px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-item {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.info-item .label {
  color: #909399;
  margin-right: 8px;
  min-width: 80px;
}

.info-item .value {
  color: #303133;
  font-weight: 500;
}

.ai-suggestions {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.suggestions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.suggestions-header h4 {
  margin: 0;
  color: #303133;
}

.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.suggestion-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 6px;
  border-left: 3px solid #67c23a;
}

.suggestion-content {
  flex: 1;
}

.criterion-text {
  font-weight: 500;
  color: #303133;
  margin-bottom: 5px;
}

.criterion-points {
  color: #409eff;
  font-weight: 600;
}

.suggestion-actions {
  margin-left: 15px;
}

.no-suggestions {
  text-align: center;
  padding: 40px 0;
}

.criteria-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.criterion-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #ffffff;
}

.criterion-content {
  flex: 1;
}

.criterion-edit {
  flex: 1;
}

.criterion-text {
  font-weight: 500;
  color: #303133;
  margin-bottom: 8px;
  line-height: 1.5;
}

.criterion-points {
  color: #409eff;
  font-weight: 600;
  margin-bottom: 8px;
}

.criterion-description {
  color: #666;
  font-size: 14px;
  line-height: 1.4;
}

.criterion-actions {
  margin-left: 20px;
  display: flex;
  gap: 8px;
}

.total-points {
  margin-top: 20px;
  padding: 15px;
  background: #f0f9ff;
  border-radius: 6px;
  text-align: center;
  border: 1px solid #b3d8ff;
}

.points-warning {
  color: #e6a23c;
  margin-left: 10px;
}

.no-criteria {
  text-align: center;
  padding: 40px 0;
}

.loading-container {
  padding: 40px;
}
</style>
