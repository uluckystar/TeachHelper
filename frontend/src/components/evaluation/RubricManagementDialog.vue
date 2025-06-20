<template>
  <el-dialog
    v-model="visible"
    title="评分标准管理"
    width="80%"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="rubric-management-dialog" v-loading="loading">
      <!-- 题目信息 -->
      <el-card class="question-info-card" v-if="questionInfo">
        <template #header>
          <span>题目信息</span>
        </template>
        <div class="question-content">
          <h3>{{ questionInfo.title }}</h3>
          <p class="question-text">{{ questionInfo.content }}</p>
          <div class="question-meta">
            <el-tag>{{ getQuestionTypeText(questionInfo.questionType) }}</el-tag>
            <span class="max-score">满分：{{ questionInfo.maxScore }}分</span>
          </div>
        </div>
      </el-card>        <!-- AI生成评分标准 -->
        <el-card class="ai-generation-card">
          <template #header>
            <div class="card-header">
              <span>AI评分标准生成</span>
              <div class="generation-controls">
                <el-button 
                  type="default" 
                  size="small"
                  @click="showCustomPromptDialog"
                  :disabled="aiGenerating"
                  :class="{ 'has-custom-prompt': customPrompt }"
                >
                  {{ customPrompt ? '✓ 自定义提示词' : '自定义提示词' }}
                </el-button>
                <el-button 
                  type="primary" 
                  :icon="MagicStick" 
                  @click="generateAIRubric"
                  :loading="aiGenerating"
                  :disabled="aiGenerating"
                >
                  {{ aiGenerating ? '生成中...' : 'AI生成评分标准建议' }}
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
            
            <!-- 自定义提示词预览 -->
            <div v-if="customPrompt && !aiGenerating" class="custom-prompt-preview">
              <div class="prompt-preview-header">
                <span class="prompt-label">自定义提示词：</span>
                <el-button 
                  type="text" 
                  size="small" 
                  @click="customPrompt = ''"
                  class="clear-prompt-btn"
                >
                  清除
                </el-button>
              </div>
              <div class="prompt-preview-text">{{ customPrompt }}</div>
            </div>
          </template>

          <!-- AI生成进度显示 -->
          <div v-if="aiGenerating" class="generation-progress">
            <div class="progress-header">
              <h4>AI正在生成评分标准...</h4>
              <div class="generation-status">{{ getStatusText(generationStatus) }}</div>
            </div>
            
            <!-- 自定义提示词显示 -->
            <div v-if="customPrompt" class="custom-prompt-display">
              <span class="prompt-label">已应用自定义提示词：</span>
              <span class="prompt-text">{{ customPrompt }}</span>
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

        <!-- 对比模式界面 -->
        <div v-if="showComparisonMode" class="comparison-mode">
          <div class="comparison-header">
            <h4>评分标准对比模式</h4>
            <p class="comparison-description">
              左侧是现有评分标准，右侧是AI生成的新建议。请选择要替换的项目。
            </p>
          </div>
          
          <div class="comparison-content">
            <div class="comparison-side existing-side">
              <h5>
                <el-icon><View /></el-icon>
                现有评分标准
              </h5>
              <div class="criteria-list">
                <div 
                  v-for="(criterion, index) in existingCriteria" 
                  :key="index"
                  class="criterion-item existing"
                >
                  <div class="criterion-content">
                    <div class="criterion-text">{{ criterion.criterionText }}</div>
                    <div class="criterion-points">{{ criterion.points }}分</div>
                  </div>
                </div>
              </div>
              <div class="existing-total">
                总分: {{ existingCriteria.reduce((sum, c) => sum + c.points, 0) }}分
              </div>
            </div>
            
            <div class="comparison-divider">
              <el-icon><MagicStick /></el-icon>
            </div>
            
            <div class="comparison-side new-side">
              <h5>
                <el-icon><MagicStick /></el-icon>
                AI生成建议
              </h5>
              <div class="criteria-list">
                <div 
                  v-for="(suggestion, index) in newSuggestions" 
                  :key="index"
                  class="criterion-item suggestion"
                  :class="{ selected: selectedSuggestions.has(index) }"
                  @click="toggleSuggestionSelection(index)"
                >
                  <div class="criterion-content">
                    <div class="criterion-text">{{ suggestion.criterionText }}</div>
                    <div class="criterion-points">{{ suggestion.points }}分</div>
                  </div>
                  <div class="criterion-actions">
                    <el-checkbox 
                      :model-value="selectedSuggestions.has(index)"
                      @change="toggleSuggestionSelection(index)"
                    />
                    <el-button 
                      type="warning" 
                      size="small"
                      @click.stop="regenerateSingleCriterion(index)"
                      :loading="regeneratingIndex === index"
                    >
                      重新生成
                    </el-button>
                  </div>
                </div>
              </div>
              <div class="suggestions-total">
                已选中: {{ selectedSuggestions.size }}项 | 
                总分: {{ Array.from(selectedSuggestions).reduce((sum, i) => sum + newSuggestions[i]?.points || 0, 0) }}分
              </div>
            </div>
          </div>
          
          <div class="comparison-actions">
            <el-button @click="closeComparisonMode">
              取消
            </el-button>
            <el-button 
              type="primary" 
              @click="applySelectedSuggestions"
              :disabled="selectedSuggestions.size === 0"
              :loading="applyingAI"
            >
              替换选中项目 ({{ selectedSuggestions.size }})
            </el-button>
            <el-button 
              type="danger" 
              @click="replaceAllSuggestions"
              :loading="applyingAI"
            >
              全部替换
            </el-button>
          </div>
        </div>

        <!-- AI生成的建议 -->
        <div v-if="aiSuggestions.length > 0 && !showComparisonMode" class="ai-suggestions">
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

      <!-- 当前评分标准显示 -->
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
                :loading="loading"
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
            <strong>总分：{{ totalPoints }} / {{ questionInfo?.maxScore || 0 }} 分</strong>
            <span v-if="totalPoints !== (questionInfo?.maxScore || 0)" class="points-warning">
              ⚠️ 评分标准总分与题目满分不符
            </span>
          </div>
        </div>

        <div v-else class="no-criteria">
          <el-empty description="暂无评分标准">
            <el-button type="primary" @click="addNewCriterion">添加评分标准</el-button>
          </el-empty>
        </div>
      </el-card>

      <!-- AI建议预览面板 - 暂时禁用以避免递归更新 -->
      <!-- 
      <AISuggestionsPanel
        v-if="aiSuggestions.length > 0"
        :suggestions="aiSuggestions"
        :current-criteria="rubricCriteria"
        :question-max-score="questionInfo?.maxScore || 100"
        @apply-suggestion="applySingleSuggestion"
        @apply-multiple="handleApplyMultipleSuggestions"
        @refresh="generateAIRubric"
        @clear-all="clearAISuggestions"
      />
      -->

      <!-- 增强版评分标准编辑器 - 暂时禁用以避免递归更新 -->
      <!-- 
      <EnhancedRubricEditor
        v-model="rubricCriteria"
        :question-max-score="questionInfo?.maxScore || 100"
        :ai-suggestions="aiSuggestions"
        @save="handleSaveCriteria"
        @refresh="loadRubricCriteria"
      />
      -->
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, unref, nextTick, h } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { 
  MagicStick, 
  Plus, 
  Refresh, 
  Edit, 
  Delete, 
  Check, 
  Close,
  View
} from '@element-plus/icons-vue'

import { questionApi } from '@/api/question'
import { rubricApi } from '@/api/rubric'
import { getQuestionTypeText } from '@/utils/tagTypes'
import type { 
  Question, 
  RubricCriterion,
  RubricCriterionResponse,
  RubricSuggestionItem,
  AIGenerationTaskResponse,
  AIGenerationStatusResponse
} from '@/types/api'

// 导入增强组件
import EnhancedRubricEditor from './EnhancedRubricEditor.vue'
import AISuggestionsPanel from './AISuggestionsPanel.vue'

// Props和Emits
interface Props {
  modelValue: boolean
  questionId: number | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  refresh: []
}>()

// 数据状态
const loading = ref(false)
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

// 自定义提示词相关状态
const customPrompt = ref<string>('')
const showPromptDialog = ref(false)

// 对比模式相关状态
const showComparisonMode = ref(false)
const existingCriteria = ref<RubricCriterion[]>([])
const newSuggestions = ref<RubricSuggestionItem[]>([])
const selectedSuggestions = ref<Set<number>>(new Set())
const regeneratingIndex = ref<number | null>(null)

// 计算属性
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const totalPoints = computed(() => {
  if (!rubricCriteria.value || rubricCriteria.value.length === 0) {
    return 0
  }
  return rubricCriteria.value.reduce((sum, criterion) => {
    const points = criterion.points || 0
    return sum + (typeof points === 'number' ? points : 0)
  }, 0)
})

// 监听对话框打开状态 - 完全避免递归的最简方案
watch(() => props.modelValue, (newVal) => {
  if (newVal && props.questionId) {
    // 使用 requestAnimationFrame 确保在下一个渲染周期执行
    requestAnimationFrame(() => {
      initializeData()
    })
  }
}, { immediate: false })

// 监听题目ID变化，清除AI状态
watch(() => props.questionId, (newQuestionId, oldQuestionId) => {
  // 只在题目真正切换时清除状态（排除初始化情况）
  if (oldQuestionId && newQuestionId && oldQuestionId !== newQuestionId) {
    clearAIStates()
  }
}, { immediate: false })

// 初始化数据 - 使用最简单安全的方式
const initializeData = () => {
  if (loading.value) return // 防止重复加载
  
  const currentQuestionId = props.questionId
  if (!currentQuestionId) return
  
  loading.value = true
  
  // 清除AI相关状态，避免显示上一个题目的数据
  clearAIStates()
  
  // 串行加载，避免并发引起的响应式问题
  loadQuestionInfo()
    .then(() => loadRubricCriteria())
    .catch(error => {
      console.error('初始化数据失败:', error)
      ElMessage.error('加载数据失败')
    })
    .finally(() => {
      if (props.questionId === currentQuestionId) {
        loading.value = false
      }
    })
}

const loadQuestionInfo = async () => {
  if (!props.questionId) return null
  
  try {
    const question = await questionApi.getQuestionById(props.questionId)
    // 直接设置，不进行额外处理
    questionInfo.value = question
    return question
  } catch (error) {
    console.error('加载题目信息失败:', error)
    return null
  }
}

// 加载评分标准 - 简化版本，减少响应式依赖
const loadRubricCriteria = async () => {
  if (!props.questionId) return []
  
  console.log('开始加载评分标准，questionId:', props.questionId)
  
  try {
    const criteria = await rubricApi.getCriteriaByQuestion(props.questionId)
    console.log('API返回的评分标准数据:', criteria)
    
    if (criteria && criteria.length > 0) {
      // 简化数据转换逻辑
      const processedCriteria = criteria.map(criterion => ({
        ...criterion,
        criterionText: criterion.criterionText || '',
        points: criterion.points || 0,
        maxScore: criterion.points || 0,
        editing: false,
        saving: false
      }))
      
      rubricCriteria.value = processedCriteria
      console.log('从API加载评分标准:', processedCriteria)
      return processedCriteria
    } else {
      rubricCriteria.value = []
      console.log('API无评分标准数据，初始化为空数组')
      return []
    }
  } catch (error) {
    console.error('加载评分标准失败:', error)
    rubricCriteria.value = []
    return []
  }
}

const generateAIRubric = async () => {
  if (!props.questionId) return
  
  // 重置状态
  resetGenerationState()
  aiGenerating.value = true
  
  try {
    // 使用智能AI生成功能
    const smartResponse = await questionApi.generateRubricSmart(props.questionId, customPrompt.value || undefined)
    
    if (smartResponse.action === 'DIRECT_GENERATE') {
      // 直接生成，设置任务状态
      currentTaskId.value = smartResponse.taskId
      generationStatus.value = 'PENDING'
      
      ElNotification.info({
        title: '开始AI生成',
        message: smartResponse.message + (customPrompt.value ? '\n(已应用自定义提示词)' : '')
      })
      
      // 开始轮询状态
      startStatusPolling()
    } else if (smartResponse.action === 'CHOICE_REQUIRED') {
      // 需要用户选择，显示选择对话框
      showModeSelectionDialog(smartResponse)
    } else if (smartResponse.action === 'ERROR') {
      throw new Error(smartResponse.error || '检测现有评分标准时发生错误')
    }
  } catch (error) {
    console.error('智能AI生成失败:', error)
    ElMessage.error('AI生成失败: ' + (error as Error).message)
    aiGenerating.value = false
  }
}

// 显示模式选择对话框
const showModeSelectionDialog = async (smartResponse: any) => {
  aiGenerating.value = false // 暂停生成状态
  
  try {
    const modes = Object.entries(smartResponse.modeOptions)
    const modeTexts = modes.map(([mode, desc]) => 
      `${mode === 'OVERWRITE' ? '覆盖模式' : '补全模式'}: ${desc}`
    ).join('\n\n')
    
    const existingInfo = `检测到现有评分标准 (${smartResponse.currentTotal}/${smartResponse.maxScore}分):\n` +
      smartResponse.existingCriteria.map((c: any) => `• ${c.criterionText}: ${c.points}分`).join('\n') +
      `\n\n请选择生成模式:\n\n${modeTexts}`
    
    const result = await ElMessageBox.confirm(
      existingInfo,
      '智能AI生成模式选择',
      {
        confirmButtonText: '覆盖模式',
        cancelButtonText: '补全模式',
        type: 'warning',
        distinguishCancelAndClose: true
      }
    )
    
    // 根据用户选择执行对应模式
    const selectedMode = result ? 'OVERWRITE' : 'COMPLEMENT'
    executeAIGenerationWithMode(selectedMode, smartResponse)
    
  } catch (error) {
    if (error === 'close') {
      // 用户点击了X按钮，不执行任何操作
      return
    } else if (error === 'cancel') {
      // 用户选择了补全模式
      executeAIGenerationWithMode('COMPLEMENT', smartResponse)
    }
  }
}

// 执行指定模式的AI生成
const executeAIGenerationWithMode = async (mode: string, smartResponse: any) => {
  aiGenerating.value = true
  
  try {
    let targetScore: number | undefined = undefined
    if (mode === 'COMPLEMENT' && smartResponse.remainingScore > 0) {
      targetScore = smartResponse.maxScore
    }
    
    const taskResponse = await questionApi.generateRubricWithMode(
      props.questionId!,
      mode,
      targetScore,
      undefined,
      customPrompt.value || undefined
    )
    
    if (taskResponse.status === 'CONFIRMATION_REQUIRED') {
      // 需要确认
      try {
        await ElMessageBox.confirm(
          taskResponse.message,
          '确认继续',
          {
            confirmButtonText: '继续生成',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        // 重新提交，带确认标志
        const confirmedResponse = await questionApi.generateRubricWithMode(
          props.questionId!,
          mode,
          targetScore,
          true,
          customPrompt.value || undefined
        )
        currentTaskId.value = confirmedResponse.taskId
        generationStatus.value = 'PENDING'
        
        ElNotification.info({
          title: '开始AI生成',
          message: confirmedResponse.message + (customPrompt.value ? '\n(已应用自定义提示词)' : '')
        })
        
        startStatusPolling()
      } catch (error) {
        aiGenerating.value = false
      }
    } else {
      // 正常开始生成
      currentTaskId.value = taskResponse.taskId
      generationStatus.value = 'PENDING'
      
      ElNotification.info({
        title: '开始AI生成',
        message: taskResponse.message + (customPrompt.value ? '\n(已应用自定义提示词)' : '')
      })
      
      startStatusPolling()
    }
  } catch (error) {
    console.error('执行AI生成失败:', error)
    ElMessage.error('AI生成失败')
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
      
      if (status.suggestions) {
        // 检查是否是对比模式
        if (status.existingCriteria && status.comparisonMode) {
          // 对比模式：显示新旧对比界面
          existingCriteria.value = status.existingCriteria
          newSuggestions.value = status.suggestions
          selectedSuggestions.value = new Set() // 清空选择
          showComparisonMode.value = true
          
          ElNotification.success({
            title: '评分标准生成完成',
            message: `生成了 ${status.suggestions.length} 个新评分标准，请选择要替换的项目`,
            duration: 5000
          })
        } else {
          // 普通模式：直接显示建议
          aiSuggestions.value = status.suggestions
          ElNotification.success({
            title: 'AI生成完成',
            message: `生成了 ${aiSuggestions.value.length} 个评分标准建议`,
            duration: 3000
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
  if (!props.questionId || aiSuggestions.value.length === 0) return
  
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
      
      // 使用临时变量存储建议，避免在操作过程中访问响应式数据
      const suggestions = [...aiSuggestions.value]
      
      // 先清空AI建议，避免后续操作触发响应式循环
      aiSuggestions.value = []
      
      // 使用新的替换API而不是旧的应用API，确保删除原有评分标准
      const response = await questionApi.replaceRubricsSelective(props.questionId, {
        selectedSuggestions: suggestions,
        replaceAll: true
      })
      
      if (response.success) {
        ElMessage.success(`成功替换了所有评分标准，共 ${response.appliedCount} 个`)
      } else {
        throw new Error(response.error || '替换失败')
      }
      
      // 重新加载评分标准
      const newCriteria = await loadRubricCriteria()
      
      // 通知父组件刷新
      emit('refresh')
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
  if (!props.questionId) return
  
  try {
    // 先复制suggestion，避免使用响应式数据
    const suggestionCopy = { ...suggestion }
    
    // 应用建议
    await questionApi.applyRubricSuggestions(props.questionId, [suggestionCopy])
    
    ElMessage.success('单个建议应用成功')
    
    // 重新加载评分标准
    const newCriteria = await loadRubricCriteria()
    
    // 通知父组件刷新
    emit('refresh')
    
    // 从建议列表中移除已应用的建议 - 使用非响应式方式
    const suggestions = [...aiSuggestions.value]
    const index = suggestions.findIndex((s) => 
      s.criterionText === suggestionCopy.criterionText && s.points === suggestionCopy.points
    )
    if (index > -1) {
      suggestions.splice(index, 1)
      aiSuggestions.value = suggestions
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
    questionId: props.questionId!
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
        questionId: props.questionId!,
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
    emit('refresh')
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
        emit('refresh')
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除评分标准失败:', error)
      ElMessage.error('删除评分标准失败')
    }
  }
}

const handleClose = () => {
  // 检查是否有未保存的编辑
  const hasUnsavedChanges = rubricCriteria.value.some(criterion => criterion.editing)
  
  if (hasUnsavedChanges) {
    ElMessageBox.confirm(
      '有未保存的更改，确定要关闭吗？',
      '确认关闭',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(() => {
      cleanup()
      emit('update:modelValue', false)
    }).catch(() => {
      // 用户取消关闭
    })
  } else {
    cleanup()
    emit('update:modelValue', false)
  }
}

// 清理资源
const cleanup = () => {
  clearAIStates()
  resetGenerationState()
}

// 清除AI相关状态（切换题目时使用）
const clearAIStates = () => {
  // 清除AI建议
  aiSuggestions.value = []
  
  // 清除对比模式状态
  showComparisonMode.value = false
  existingCriteria.value = []
  newSuggestions.value = []
  selectedSuggestions.value = new Set()
  regeneratingIndex.value = null
  
  // 清除生成状态（但保留自定义提示词）
  aiGenerating.value = false
  applyingAI.value = false
  currentTaskId.value = null
  generationStatus.value = 'PENDING'
  generationProgress.value = 0
  tokenStats.value = {
    totalTokens: 0,
    promptTokens: 0,
    completionTokens: 0
  }
  processingTime.value = 0
  
  // 停止状态轮询
  stopStatusPolling()
}

// 对比模式相关方法
const toggleSuggestionSelection = (index: number) => {
  if (selectedSuggestions.value.has(index)) {
    selectedSuggestions.value.delete(index)
  } else {
    selectedSuggestions.value.add(index)
  }
}

const closeComparisonMode = () => {
  showComparisonMode.value = false
  existingCriteria.value = []
  newSuggestions.value = []
  selectedSuggestions.value = new Set()
  regeneratingIndex.value = null
}

const applySelectedSuggestions = async () => {
  if (!props.questionId || selectedSuggestions.value.size === 0) return
  
  try {
    applyingAI.value = true
    
    const selectedItems = Array.from(selectedSuggestions.value).map(index => newSuggestions.value[index])
    
    // 调用后端API进行选择性替换
    const response = await questionApi.replaceRubricsSelective(props.questionId, {
      selectedSuggestions: selectedItems,
      replaceAll: false
    })
    
    if (response.success) {
      ElMessage.success(`成功替换了 ${response.appliedCount} 个评分标准`)
      closeComparisonMode()
      await loadRubricCriteria()
      emit('refresh')
    } else {
      throw new Error(response.error || '替换失败')
    }
  } catch (error) {
    console.error('选择性替换失败:', error)
    ElMessage.error('替换失败: ' + (error as Error).message)
  } finally {
    applyingAI.value = false
  }
}

const replaceAllSuggestions = async () => {
  if (!props.questionId || newSuggestions.value.length === 0) return
  
  try {
    const result = await ElMessageBox.confirm(
      `确定要替换所有现有评分标准吗？这将删除现有的 ${existingCriteria.value.length} 个标准，并添加 ${newSuggestions.value.length} 个新标准。`,
      '确认全部替换',
      {
        confirmButtonText: '确定替换',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    if (result === 'confirm') {
      applyingAI.value = true
      
      // 调用后端API进行全部替换
      const response = await questionApi.replaceRubricsSelective(props.questionId, {
        selectedSuggestions: newSuggestions.value,
        replaceAll: true
      })
      
      if (response.success) {
        ElMessage.success(`成功替换了所有评分标准，共 ${response.appliedCount} 个`)
        closeComparisonMode()
        await loadRubricCriteria()
        emit('refresh')
      } else {
        throw new Error(response.error || '替换失败')
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('全部替换失败:', error)
      ElMessage.error('替换失败: ' + (error as Error).message)
    }
  } finally {
    applyingAI.value = false
  }
}

const regenerateSingleCriterion = async (index: number) => {
  if (!props.questionId || index >= newSuggestions.value.length) return
  
  try {
    regeneratingIndex.value = index
    
    const response = await questionApi.regenerateSingleRubric(props.questionId, {
      criterionIndex: (index + 1).toString(),
      customPrompt: customPrompt.value
    })
    
    // 替换这一项
    newSuggestions.value[index] = response
    ElMessage.success('重新生成完成')
    
  } catch (error) {
    console.error('重新生成失败:', error)
    ElMessage.error('重新生成失败: ' + (error as Error).message)
  } finally {
    regeneratingIndex.value = null
  }
}

// 处理保存评分标准（从增强编辑器调用）
const handleSaveCriteria = async (criteria: RubricCriterion[]) => {
  if (!props.questionId) return
  
  try {
    // 这里可以批量保存评分标准
    await loadRubricCriteria() // 重新加载以确保数据同步
    emit('refresh')
    ElMessage.success('评分标准保存成功')
  } catch (error) {
    console.error('保存评分标准失败:', error)
    ElMessage.error('保存评分标准失败')
  }
}

// 处理批量应用AI建议
const handleApplyMultipleSuggestions = async (suggestions: RubricSuggestionItem[], mode: 'append' | 'replace') => {
  if (!props.questionId || suggestions.length === 0) return
  
  try {
    const result = await ElMessageBox.confirm(
      `确定要${mode === 'replace' ? '替换现有标准并' : ''}应用选中的 ${suggestions.length} 个AI建议吗？`,
      '确认批量应用',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    if (result === 'confirm') {
      applyingAI.value = true
      
      // 复制建议数组，避免操作响应式数据
      const suggestionsCopy = [...suggestions]
      
      // 先清空AI建议，避免响应式循环
      aiSuggestions.value = []
      
      // 应用建议
      await questionApi.applyRubricSuggestions(props.questionId, suggestionsCopy)
      
      ElMessage.success(`成功应用 ${suggestionsCopy.length} 个AI建议`)
      
      // 重新加载评分标准
      const newCriteria = await loadRubricCriteria()
      
      // 通知父组件刷新
      emit('refresh')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量应用AI建议失败:', error)
      ElMessage.error('批量应用AI建议失败')
    }
  } finally {
    applyingAI.value = false
  }
}

// 清空AI建议
const clearAISuggestions = () => {
  aiSuggestions.value = []
}

// 显示自定义提示词对话框
const showCustomPromptDialog = async () => {
  try {
    const { value: prompt } = await ElMessageBox.prompt(
      '请输入您希望AI生成评分标准时考虑的特殊要求或提示词：',
      '自定义AI提示词',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPlaceholder: '例如：注重逻辑思维能力、突出创新性、考虑实践应用等...',
        inputType: 'textarea',
        inputValidator: (value: string) => {
          if (value && value.length > 500) {
            return '提示词不能超过500个字符'
          }
          return true
        }
      }
    )
    
    if (prompt) {
      customPrompt.value = prompt.trim()
      ElMessage.success('自定义提示词已设置')
    }
  } catch (error) {
    // 用户取消，不执行任何操作
  }
}
</script>

<style scoped>
.rubric-management-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.generation-controls {
  display: flex;
  gap: 10px;
  align-items: center;
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

.ai-suggestions {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.suggestions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
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

/* 模式选择对话框样式 */
.mode-selection-dialog {
  max-width: 600px;
  font-family: inherit;
}

.existing-info {
  margin-bottom: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

.existing-info h4 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.existing-criteria {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.criterion-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: white;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.criterion-text {
  flex: 1;
  font-weight: 500;
  color: #303133;
}

.criterion-points {
  color: #409eff;
  font-weight: 600;
  margin-left: 12px;
}

.score-summary {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.score-summary .remaining {
  color: #e6a23c;
}

.score-summary .complete {
  color: #67c23a;
}

.mode-options {
  margin-top: 20px;
}

.mode-options h4 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  position: relative;
  padding: 16px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.option-item:hover {
  border-color: #409eff;
  background: #f0f9ff;
}

.option-item input[type="radio"] {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.option-item label {
  display: block;
  cursor: pointer;
  margin-right: 30px;
}

.option-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.option-desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}

.option-item.overwrite {
  border-left: 4px solid #e6a23c;
}

.option-item.complement {
  border-left: 4px solid #67c23a;
}

.option-item input[type="radio"]:checked + label {
  color: #409eff;
}

.option-item input[type="radio"]:checked + label .option-title {
  color: #409eff;
}

/* 自定义提示词相关样式 */
.has-custom-prompt {
  background-color: #f0f9ff !important;
  border-color: #409eff !important;
  color: #409eff !important;
}

.custom-prompt-preview {
  margin-top: 12px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
  border-left: 3px solid #409eff;
}

.prompt-preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.prompt-label {
  font-size: 12px;
  font-weight: 600;
  color: #409eff;
}

.prompt-preview-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.4;
}

.clear-prompt-btn {
  color: #f56c6c !important;
  padding: 0 !important;
}

.custom-prompt-display {
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #f0f9ff;
  border-radius: 4px;
  border-left: 3px solid #409eff;
}

.custom-prompt-display .prompt-label {
  font-size: 12px;
  font-weight: 600;
  color: #409eff;
  margin-right: 8px;
}

.custom-prompt-display .prompt-text {
  font-size: 12px;
  color: #606266;
}

/* 对比模式相关样式 */
.comparison-mode {
  margin-top: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.comparison-header {
  padding: 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #e4e7ed;
}

.comparison-header h4 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.comparison-description {
  margin: 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.4;
}

.comparison-content {
  display: flex;
  min-height: 400px;
}

.comparison-side {
  flex: 1;
  padding: 16px;
  border-right: 1px solid #e4e7ed;
}

.comparison-side:last-child {
  border-right: none;
}

.comparison-side h5 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.existing-side {
  background: #fafbfc;
}

.new-side {
  background: #f0f9ff;
}

.comparison-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  background: #f8f9fa;
  border-left: 1px solid #e4e7ed;
  border-right: 1px solid #e4e7ed;
  color: #409eff;
  font-size: 18px;
}

.criteria-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.criterion-item {
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: white;
  cursor: pointer;
  transition: all 0.3s ease;
}

.criterion-item.existing {
  border-left: 3px solid #909399;
}

.criterion-item.suggestion {
  border-left: 3px solid #409eff;
}

.criterion-item.suggestion.selected {
  background: #e6f7ff;
  border-color: #409eff;
}

.criterion-item:hover {
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.criterion-content {
  flex: 1;
}

.criterion-text {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
  font-size: 14px;
  line-height: 1.4;
}

.criterion-points {
  color: #409eff;
  font-weight: 600;
  font-size: 13px;
}

.existing-total,
.suggestions-total {
  margin-top: 12px;
  padding: 8px 12px;
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  text-align: center;
  font-weight: 500;
  color: #606266;
}

.comparison-actions {
  padding: 16px;
  background: #f8f9fa;
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
