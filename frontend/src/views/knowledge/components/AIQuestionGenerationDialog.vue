<template>
  <el-dialog
    v-model="visible"
    title="AI智能出题"
    width="800px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="ai-question-generation-dialog">
      <!-- 知识库信息 -->
      <el-card class="kb-info-card" shadow="never">
        <template #header>
          <span>基于知识库生成题目</span>
        </template>
        <div class="kb-info" v-if="knowledgeBase">
          <el-descriptions :column="3" size="small">
            <el-descriptions-item label="知识库">
              {{ knowledgeBase.name }}
            </el-descriptions-item>
            <el-descriptions-item label="学科">
              <el-tag type="primary" size="small">{{ knowledgeBase.subject || '通用' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="年级">
              {{ knowledgeBase.gradeLevel || '通用' }}
            </el-descriptions-item>
            <el-descriptions-item label="文档数量">
              {{ knowledgeBase.documentCount || 0 }} 个
            </el-descriptions-item>
            <el-descriptions-item label="知识点数">
              {{ knowledgeBase.knowledgePointCount || 0 }} 个
            </el-descriptions-item>
            <el-descriptions-item label="更新时间">
              {{ formatDate(knowledgeBase.updatedAt) }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </el-card>

      <!-- 生成配置 -->
      <el-card class="config-card">
        <template #header>
          <span>出题配置</span>
        </template>
        
        <el-form :model="generationConfig" :rules="configRules" ref="configFormRef" label-width="120px">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="题目类型" prop="questionTypes">
                <el-checkbox-group v-model="generationConfig.questionTypes">
                  <el-checkbox label="SINGLE_CHOICE">单选题</el-checkbox>
                  <el-checkbox label="MULTIPLE_CHOICE">多选题</el-checkbox>
                  <el-checkbox label="TRUE_FALSE">判断题</el-checkbox>
                  <el-checkbox label="FILL_BLANK">填空题</el-checkbox>
                  <el-checkbox label="SHORT_ANSWER">简答题</el-checkbox>
                  <el-checkbox label="ESSAY">论述题</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="难度分布" prop="difficultyDistribution">
                <div class="difficulty-config">
                  <div class="difficulty-item">
                    <el-text size="small">简单：</el-text>
                    <el-input-number
                      v-model="generationConfig.difficultyDistribution.EASY"
                      :min="0"
                      :max="20"
                      size="small"
                      style="width: 80px"
                    />
                    <el-text size="small">题</el-text>
                  </div>
                  <div class="difficulty-item">
                    <el-text size="small">中等：</el-text>
                    <el-input-number
                      v-model="generationConfig.difficultyDistribution.MEDIUM"
                      :min="0"
                      :max="20"
                      size="small"
                      style="width: 80px"
                    />
                    <el-text size="small">题</el-text>
                  </div>
                  <div class="difficulty-item">
                    <el-text size="small">困难：</el-text>
                    <el-input-number
                      v-model="generationConfig.difficultyDistribution.HARD"
                      :min="0"
                      :max="20"
                      size="small"
                      style="width: 80px"
                    />
                    <el-text size="small">题</el-text>
                  </div>
                </div>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="知识点范围" prop="knowledgePointIds">
                <el-select
                  v-model="generationConfig.knowledgePointIds"
                  multiple
                  filterable
                  placeholder="选择知识点（空则使用全部）"
                  style="width: 100%"
                >
                  <el-option
                    v-for="point in knowledgePoints"
                    :key="point.id"
                    :label="point.title"
                    :value="point.id"
                  >
                    <div style="display: flex; justify-content: space-between;">
                      <span>{{ point.title }}</span>
                      <el-tag :type="getDifficultyTagType(point.difficulty)" size="small">
                        {{ point.difficulty }}
                      </el-tag>
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="AI模型配置" prop="aiConfigId">
                <el-select
                  v-model="generationConfig.aiConfigId"
                  placeholder="选择AI配置"
                  style="width: 100%"
                >
                  <el-option
                    v-for="config in aiConfigs"
                    :key="config.id"
                    :label="config.name"
                    :value="config.id"
                  >
                    <div style="display: flex; justify-content: space-between;">
                      <span>{{ config.name }}</span>
                      <el-tag size="small">{{ config.provider }}</el-tag>
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="生成策略" prop="generationStrategy">
            <el-radio-group v-model="generationConfig.generationStrategy">
              <el-radio label="KNOWLEDGE_BASED">基于知识点</el-radio>
              <el-radio label="AI_GENERATED">AI自主生成</el-radio>
              <el-radio label="VECTOR_BASED">向量搜索</el-radio>
              <el-radio label="INTERNET_SOURCE">互联网来源</el-radio>
              <el-radio label="MIXED">混合策略</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="题目标签">
            <el-select
              v-model="generationConfig.tags"
              multiple
              filterable
              allow-create
              placeholder="添加题目标签"
              style="width: 100%"
            >
              <el-option
                v-for="tag in commonTags"
                :key="tag"
                :label="tag"
                :value="tag"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="附加要求">
            <el-input
              v-model="generationConfig.additionalRequirements"
              type="textarea"
              :rows="3"
              placeholder="请输入特殊要求，如：侧重某个方面、特定题型要求等..."
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 智能推荐配置 -->
      <el-card class="recommendation-card" v-if="knowledgeBase">
        <template #header>
          <span>智能推荐配置</span>
        </template>
        
        <div class="recommendation-section">
          <el-alert
            title="AI 分析结果"
            type="info"
            :closable="false"
            class="analysis-alert"
          >
            <template #default>
              <div class="analysis-content">
                <div class="analysis-item">
                  <strong>推荐题目类型：</strong>
                  <el-tag v-for="type in recommendedTypes" :key="type" size="small" style="margin-left: 8px;">
                    {{ getQuestionTypeText(type) }}
                  </el-tag>
                </div>
                <div class="analysis-item">
                  <strong>建议难度分布：</strong>
                  <span>简单 30% | 中等 50% | 困难 20%</span>
                </div>
                <div class="analysis-item">
                  <strong>核心知识点：</strong>
                  <el-tag 
                    v-for="point in coreKnowledgePoints" 
                    :key="point.id" 
                    size="small" 
                    type="success"
                    style="margin-left: 8px;"
                  >
                    {{ point.title }}
                  </el-tag>
                </div>
              </div>
            </template>
          </el-alert>

          <div class="recommendation-actions">
            <el-button @click="applyRecommendations" type="primary" icon="MagicStick">
              应用推荐配置
            </el-button>
            <el-button @click="customizeConfiguration" icon="Setting">
              自定义配置
            </el-button>
          </div>
        </div>
      </el-card>

      <!-- 高级选项 -->
      <el-card class="advanced-options-card">
        <template #header>
          <div class="expandable-header" @click="showAdvancedOptions = !showAdvancedOptions">
            <span>高级选项</span>
            <el-icon>
              <ArrowDown v-if="!showAdvancedOptions" />
              <ArrowUp v-else />
            </el-icon>
          </div>
        </template>
        
        <div v-show="showAdvancedOptions" class="advanced-options">
          <el-form :model="advancedConfig" label-width="120px">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="生成温度">
                  <el-slider
                    v-model="advancedConfig.temperature"
                    :min="0"
                    :max="2"
                    :step="0.1"
                    show-tooltip
                  />
                  <div class="slider-hint">{{ getTemperatureHint(advancedConfig.temperature) }}</div>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="创新程度">
                  <el-slider
                    v-model="advancedConfig.creativity"
                    :min="0"
                    :max="1"
                    :step="0.1"
                    show-tooltip
                  />
                  <div class="slider-hint">{{ getCreativityHint(advancedConfig.creativity) }}</div>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="上下文长度">
                  <el-input-number
                    v-model="advancedConfig.contextLength"
                    :min="100"
                    :max="4000"
                    :step="100"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="重试次数">
                  <el-input-number
                    v-model="advancedConfig.maxRetries"
                    :min="1"
                    :max="5"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="质量控制">
              <el-checkbox-group v-model="advancedConfig.qualityChecks">
                <el-checkbox label="grammar">语法检查</el-checkbox>
                <el-checkbox label="logic">逻辑一致性</el-checkbox>
                <el-checkbox label="difficulty">难度匹配</el-checkbox>
                <el-checkbox label="uniqueness">题目去重</el-checkbox>
              </el-checkbox-group>
            </el-form-item>

            <el-form-item label="输出格式">
              <el-radio-group v-model="advancedConfig.outputFormat">
                <el-radio label="standard">标准格式</el-radio>
                <el-radio label="detailed">详细解析</el-radio>
                <el-radio label="minimal">简化格式</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </div>
      </el-card>

      <!-- 生成预览 -->
      <el-card class="preview-card" v-if="totalQuestions > 0">
        <template #header>
          <span>生成预览</span>
        </template>
        
        <div class="generation-summary">
          <div class="summary-item">
            <span class="label">总题目数：</span>
            <span class="value">{{ totalQuestions }} 题</span>
          </div>
          <div class="summary-item">
            <span class="label">题型分布：</span>
            <div class="type-distribution">
              <el-tag
                v-for="type in generationConfig.questionTypes"
                :key="type"
                size="small"
                style="margin-right: 8px"
              >
                {{ getQuestionTypeText(type) }}
              </el-tag>
            </div>
          </div>
          <div class="summary-item">
            <span class="label">难度分布：</span>
            <div class="difficulty-summary">
              <span v-if="generationConfig.difficultyDistribution.EASY > 0" class="difficulty-item">
                简单 {{ generationConfig.difficultyDistribution.EASY }} 题
              </span>
              <span v-if="generationConfig.difficultyDistribution.MEDIUM > 0" class="difficulty-item">
                中等 {{ generationConfig.difficultyDistribution.MEDIUM }} 题
              </span>
              <span v-if="generationConfig.difficultyDistribution.HARD > 0" class="difficulty-item">
                困难 {{ generationConfig.difficultyDistribution.HARD }} 题
              </span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 生成进度 -->
      <el-card class="progress-card" v-if="generating">
        <template #header>
          <div class="progress-header">
            <span>正在生成题目</span>
            <el-button
              size="small"
              type="danger"
              @click="cancelGeneration"
              v-if="currentTaskId"
            >
              取消生成
            </el-button>
          </div>
        </template>
        
        <div class="generation-progress">
          <div class="progress-info">
            <div class="status-text">{{ getGenerationStatusText() }}</div>
            <div class="progress-percentage">{{ generationProgress }}%</div>
          </div>
          
          <el-progress
            :percentage="generationProgress"
            :status="generationStatus === 'FAILED' ? 'exception' : undefined"
            :stroke-width="12"
          />
          
          <div class="generation-stats" v-if="generationStats.totalTokens > 0">
            <div class="stats-row">
              <div class="stat-item">
                <span class="stat-label">Token 使用：</span>
                <span class="stat-value">{{ generationStats.totalTokens }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">处理时间：</span>
                <span class="stat-value">{{ Math.round(generationStats.processingTime / 1000) }}s</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">已生成：</span>
                <span class="stat-value">{{ generationStats.generatedCount }} 题</span>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 生成结果 -->
      <el-card class="results-card" v-if="generatedQuestions.length > 0">
        <template #header>
          <div class="results-header">
            <span>生成结果（{{ generatedQuestions.length }} 题）</span>
            <div class="header-actions">
              <el-button
                size="small"
                type="success"
                @click="acceptAllQuestions"
                :loading="accepting"
              >
                接受全部
              </el-button>
              <el-button
                size="small"
                @click="regenerateQuestions"
                :loading="generating"
              >
                重新生成
              </el-button>
            </div>
          </div>
        </template>
        
        <div class="questions-list">
          <div
            v-for="(question, index) in generatedQuestions"
            :key="index"
            class="question-item"
          >
            <div class="question-header">
              <div class="question-meta">
                <el-tag size="small">{{ getQuestionTypeText(question.questionType) }}</el-tag>
                <el-tag :type="getDifficultyTagType(question.difficulty)" size="small">
                  {{ question.difficulty }}
                </el-tag>
                <span class="question-score">{{ question.maxScore }}分</span>
              </div>
              <div class="question-actions">
                <el-button
                  size="small"
                  type="primary"
                  @click="acceptQuestion(question, index)"
                >
                  接受
                </el-button>
                <el-button
                  size="small"
                  @click="editQuestion(question, index)"
                >
                  编辑
                </el-button>
                <el-button
                  size="small"
                  type="danger"
                  @click="removeQuestion(index)"
                >
                  删除
                </el-button>
              </div>
            </div>
            
            <div class="question-content">
              <div class="question-text">{{ question.content }}</div>
              
              <div v-if="question.options && question.options.length > 0" class="question-options">
                <div
                  v-for="(option, optIndex) in question.options"
                  :key="optIndex"
                  class="option-item"
                  :class="{ 'correct-option': question.correctAnswers?.includes(optIndex.toString()) }"
                >
                  <span class="option-label">{{ String.fromCharCode(65 + optIndex) }}.</span>
                  <span class="option-text">{{ option }}</span>
                </div>
              </div>
              
              <div v-if="question.explanation" class="question-explanation">
                <div class="explanation-title">解析：</div>
                <div class="explanation-text">{{ question.explanation }}</div>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose" :disabled="generating">取消</el-button>
        <el-button
          type="primary"
          @click="startGeneration"
          :loading="generating"
          :disabled="totalQuestions === 0 || !generationConfig.aiConfigId"
        >
          {{ generating ? '生成中...' : '开始生成' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { 
  aiQuestionGenerationApi, 
  knowledgeBaseApi,
  type KnowledgeBase,
  type AIQuestionGenerationRequest 
} from '@/api/knowledge'
import { userAIConfigApi } from '@/api/userAIConfig'
import { getQuestionTypeText } from '@/utils/tagTypes'

interface Props {
  modelValue: boolean
  knowledgeBase?: KnowledgeBase | null
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'generated', questions: any[]): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const configFormRef = ref()
const generating = ref(false)
const accepting = ref(false)
const knowledgePoints = ref<any[]>([])
const aiConfigs = ref<any[]>([])
const generatedQuestions = ref<any[]>([])
const pollInterval = ref<any>(null)

// 生成配置
const generationConfig = reactive<AIQuestionGenerationRequest>({
  knowledgeBaseId: 0,
  questionTypes: ['SINGLE_CHOICE'],
  difficultyDistribution: {
    EASY: 2,
    MEDIUM: 3,
    HARD: 1
  },
  knowledgePointIds: [],
  generationStrategy: 'MIXED',
  aiConfigId: 0,
  tags: [],
  additionalRequirements: ''
})

// 高级配置
const advancedConfig = reactive({
  temperature: 1,
  creativity: 0.5,
  contextLength: 1000,
  maxRetries: 3,
  qualityChecks: ['grammar', 'logic', 'difficulty', 'uniqueness'],
  outputFormat: 'standard'
})

// 生成状态
const currentTaskId = ref<string | null>(null)
const generationStatus = ref<'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'>('PENDING')
const generationProgress = ref(0)
const generationStats = reactive({
  totalTokens: 0,
  processingTime: 0,
  generatedCount: 0
})

// 常用标签
const commonTags = [
  '基础练习', '重点题型', '易错题', '综合应用',
  '课后练习', '单元测试', '期中复习', '期末考试'
]

// 表单验证规则
const configRules = {
  questionTypes: [
    { required: true, message: '请选择至少一种题目类型', trigger: 'change' }
  ],
  aiConfigId: [
    { required: true, message: '请选择AI配置', trigger: 'change' }
  ]
}

// 计算属性
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const totalQuestions = computed(() => {
  return Object.values(generationConfig.difficultyDistribution).reduce((sum, count) => sum + count, 0)
})

// 智能推荐相关
const showAdvancedOptions = ref(false)
const recommendedTypes = ref<string[]>(['SINGLE_CHOICE', 'FILL_BLANK'])
const coreKnowledgePoints = ref<any[]>([])

// 初始化数据
onMounted(async () => {
  await Promise.all([
    loadKnowledgePoints(),
    loadAIConfigs()
  ])
})

const loadKnowledgePoints = async () => {
  if (!props.knowledgeBase?.id) return
  
  try {
    knowledgePoints.value = await knowledgeBaseApi.getKnowledgePoints(props.knowledgeBase.id)
  } catch (error) {
    console.error('加载知识点失败:', error)
  }
}

const loadAIConfigs = async () => {
  try {
    aiConfigs.value = await userAIConfigApi.getConfigs()
    
    // 自动选择第一个可用配置
    if (aiConfigs.value.length > 0 && !generationConfig.aiConfigId) {
      generationConfig.aiConfigId = aiConfigs.value[0].id
    }
  } catch (error) {
    console.error('加载AI配置失败:', error)
  }
}

// 辅助函数
const getDifficultyTagType = (difficulty: string) => {
  switch (difficulty) {
    case 'EASY': return 'success'
    case 'MEDIUM': return 'warning'
    case 'HARD': return 'danger'
    default: return 'info'
  }
}

const getGenerationStatusText = () => {
  switch (generationStatus.value) {
    case 'PENDING': return '准备中...'
    case 'PROCESSING': return '正在生成题目...'
    case 'COMPLETED': return '生成完成'
    case 'FAILED': return '生成失败'
    case 'CANCELLED': return '已取消'
    default: return '未知状态'
  }
}

const formatDate = (dateString?: string) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

// 生成操作
const startGeneration = async () => {
  if (!props.knowledgeBase?.id) {
    ElMessage.error('请先选择知识库')
    return
  }

  if (!configFormRef.value) return

  try {
    await configFormRef.value.validate()
    
    generating.value = true
    generationConfig.knowledgeBaseId = props.knowledgeBase.id
    
    ElNotification.info({
      title: '开始生成',
      message: 'AI正在生成题目，请稍等...'
    })
    
    // 直接调用生成API（同步）
    const response = await aiQuestionGenerationApi.generateQuestions(generationConfig)
    
    if (response.status === 'COMPLETED') {
      generatedQuestions.value = response.questions || []
      generationStatus.value = 'COMPLETED'
      generationProgress.value = 100
      
      // 更新统计信息
      if (response.totalTokens) {
        generationStats.totalTokens = response.totalTokens
      }
      if (response.processingTimeMs) {
        generationStats.processingTime = response.processingTimeMs
      }
      if (response.generatedCount) {
        generationStats.generatedCount = response.generatedCount
      }
      
      ElNotification.success({
        title: '生成完成',
        message: `成功生成了 ${response.generatedCount || response.questions?.length || 0} 道题目`
      })
    } else if (response.status === 'FAILED') {
      generationStatus.value = 'FAILED'
      throw new Error(response.error || '题目生成失败')
    } else {
      // 如果是异步任务，设置任务ID并开始轮询
      currentTaskId.value = response.taskId
      generationStatus.value = response.status
      generationProgress.value = response.progress || 0
      
      // 开始轮询状态
      pollInterval.value = setInterval(pollGenerationStatus, 2000)
    }
    
  } catch (error: any) {
    console.error('开始生成失败:', error)
    ElMessage.error(error.response?.data?.message || error.message || '开始生成失败')
    generationStatus.value = 'FAILED'
  } finally {
    if (generationStatus.value === 'COMPLETED' || generationStatus.value === 'FAILED') {
      generating.value = false
    }
  }
}

const pollGenerationStatus = async () => {
  if (!currentTaskId.value) return
  
  try {
    const status = await aiQuestionGenerationApi.getGenerationStatus(currentTaskId.value)
    
    generationStatus.value = status.status
    generationProgress.value = status.progress || 0
    
    // 更新统计信息
    if (status.totalTokens) {
      generationStats.totalTokens = status.totalTokens
    }
    if (status.processingTimeMs) {
      generationStats.processingTime = status.processingTimeMs
    }
    if (status.generatedCount) {
      generationStats.generatedCount = status.generatedCount
    }
    
    // 检查是否完成
    if (status.status === 'COMPLETED') {
      generating.value = false
      
      if (status.questions) {
        generatedQuestions.value = status.questions
        ElNotification.success({
          title: '生成完成',
          message: `成功生成 ${status.questions.length} 道题目`
        })
      }
    } else if (status.status === 'FAILED') {
      generating.value = false
      ElMessage.error('题目生成失败：' + (status.error || '未知错误'))
    } else if (status.status === 'CANCELLED') {
      generating.value = false
      ElMessage.info('题目生成已取消')
    } else {
      // 继续轮询
      setTimeout(pollGenerationStatus, 2000)
    }
    
  } catch (error) {
    console.error('检查生成状态失败:', error)
    generating.value = false
    ElMessage.error('检查生成状态失败')
  }
}

const cancelGeneration = async () => {
  if (!currentTaskId.value) return
  
  try {
    await aiQuestionGenerationApi.cancelGeneration(currentTaskId.value)
    generating.value = false
    generationStatus.value = 'CANCELLED'
    ElMessage.success('生成已取消')
  } catch (error) {
    console.error('取消生成失败:', error)
    ElMessage.error('取消生成失败')
  }
}

const regenerateQuestions = async () => {
  generatedQuestions.value = []
  await startGeneration()
}

// 题目操作
const acceptQuestion = async (question: any, index: number) => {
  try {
    // TODO: 实现单个题目接受逻辑
    ElMessage.success('题目已接受')
    generatedQuestions.value.splice(index, 1)
  } catch (error) {
    console.error('接受题目失败:', error)
    ElMessage.error('接受题目失败')
  }
}

const acceptAllQuestions = async () => {
  if (generatedQuestions.value.length === 0) return
  
  try {
    accepting.value = true
    
    // TODO: 实现批量接受逻辑
    ElMessage.success(`已接受 ${generatedQuestions.value.length} 道题目`)
    emit('generated', generatedQuestions.value)
    
    generatedQuestions.value = []
    handleClose()
    
  } catch (error) {
    console.error('批量接受失败:', error)
    ElMessage.error('批量接受失败')
  } finally {
    accepting.value = false
  }
}

const editQuestion = (question: any, index: number) => {
  // TODO: 实现题目编辑功能
  ElMessage.info('题目编辑功能开发中...')
}

const removeQuestion = (index: number) => {
  generatedQuestions.value.splice(index, 1)
  ElMessage.success('题目已删除')
}

const handleClose = () => {
  if (generating.value) {
    ElMessage.warning('正在生成中，请稍后再关闭')
    return
  }
  
  emit('update:modelValue', false)
  
  // 重置状态
  generatedQuestions.value = []
  currentTaskId.value = null
  generationProgress.value = 0
  generationStatus.value = 'PENDING'
  Object.assign(generationStats, {
    totalTokens: 0,
    processingTime: 0,
    generatedCount: 0
  })
}

// 监听对话框打开
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    if (!props.knowledgeBase) {
      ElMessage.warning('请先选择知识库')
      emit('update:modelValue', false)
      return
    }
    
    // 重新加载数据
    loadKnowledgePoints()
    loadAIConfigs()
  }
})

// 智能推荐方法
const applyRecommendations = () => {
  // 应用AI推荐的配置
  generationConfig.questionTypes = recommendedTypes.value.filter(type => 
    ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE', 'FILL_BLANK', 'SHORT_ANSWER', 'ESSAY'].includes(type)
  ) as any[]
  
  generationConfig.difficultyDistribution = {
    EASY: Math.round(totalQuestions.value * 0.3),
    MEDIUM: Math.round(totalQuestions.value * 0.5),
    HARD: Math.round(totalQuestions.value * 0.2)
  }
  ElMessage.success('已应用推荐配置')
}

const customizeConfiguration = () => {
  // 显示高级选项
  showAdvancedOptions.value = true
  ElMessage.info('请在高级选项中自定义配置')
}

const getTemperatureHint = (temperature: number) => {
  if (temperature <= 0.3) return '保守 - 结果更稳定'
  if (temperature <= 0.7) return '平衡 - 稳定与创新并重'
  if (temperature <= 1.0) return '标准 - 适中的创新性'
  if (temperature <= 1.5) return '创新 - 结果更有创意'
  return '极富创意 - 结果最多样化'
}

const getCreativityHint = (creativity: number) => {
  if (creativity <= 0.3) return '传统 - 基于现有模式'
  if (creativity <= 0.7) return '适中 - 平衡传统与创新'
  return '创新 - 生成新颖题目'
}

// 清理定时器
const clearPolling = () => {
  if (pollInterval.value) {
    clearInterval(pollInterval.value)
    pollInterval.value = null
  }
}

// 组件卸载时清理
onUnmounted(() => {
  clearPolling()
})
</script>

<style scoped>
.ai-question-generation-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.kb-info-card,
.config-card,
.preview-card,
.progress-card,
.results-card {
  border: 1px solid #e4e7ed;
}

.difficulty-config {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.difficulty-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.generation-summary {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.summary-item .label {
  font-weight: 600;
  color: #606266;
  min-width: 80px;
}

.summary-item .value {
  color: #303133;
}

.type-distribution,
.difficulty-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.generation-progress {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-text {
  font-size: 14px;
  color: #606266;
}

.progress-percentage {
  font-size: 18px;
  font-weight: 600;
  color: #409eff;
}

.generation-stats {
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
}

.stats-row {
  display: flex;
  justify-content: space-around;
  gap: 20px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.questions-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 400px;
  overflow-y: auto;
}

.question-item {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f8f9fa;
  border-bottom: 1px solid #e4e7ed;
}

.question-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.question-score {
  font-size: 14px;
  color: #606266;
}

.question-actions {
  display: flex;
  gap: 8px;
}

.question-content {
  padding: 16px;
}

.question-text {
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
  margin-bottom: 12px;
}

.question-options {
  margin-bottom: 12px;
}

.option-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 6px 0;
  font-size: 14px;
  line-height: 1.5;
}

.option-item.correct-option {
  color: #67c23a;
  font-weight: 600;
}

.option-label {
  min-width: 20px;
  font-weight: 600;
}

.option-text {
  flex: 1;
}

.question-explanation {
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.explanation-title {
  font-weight: 600;
  color: #606266;
  margin-bottom: 6px;
}

.explanation-text {
  font-size: 14px;
  line-height: 1.6;
  color: #909399;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.recommendation-card,
.advanced-options-card {
  border: 1px solid #e4e7ed;
}

.analysis-alert {
  margin-bottom: 16px;
}

.recommendation-actions {
  display: flex;
  gap: 8px;
}

.expandable-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
}

.advanced-options {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.slider-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
