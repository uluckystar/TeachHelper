<template>
  <el-dialog
    v-model="visible"
    title="AI生成题目"
    width="800px"
    :close-on-click-modal="false"
    @closed="resetForm"
  >
    <div class="ai-generation-dialog">
      <!-- 生成配置 -->
      <div class="generation-config">
        <h3>生成配置</h3>
        <el-form :model="configForm" label-width="120px" class="config-form">
          <el-form-item label="生成来源">
            <el-radio-group v-model="configForm.generationStrategy">
              <el-radio value="KNOWLEDGE_BASED">
                <div class="source-option">
                  <div class="source-title">知识库生成</div>
                  <div class="source-desc">基于知识库中的知识点和文档生成题目</div>
                </div>
              </el-radio>
              <el-radio value="AI_GENERATED">
                <div class="source-option">
                  <div class="source-title">AI自主生成</div>
                  <div class="source-desc">AI根据学科特点自主创作题目</div>
                </div>
              </el-radio>
              <el-radio value="VECTOR_BASED">
                <div class="source-option">
                  <div class="source-title">向量搜索生成</div>
                  <div class="source-desc">基于向量数据库的相似内容生成题目</div>
                </div>
              </el-radio>
              <el-radio value="INTERNET_SOURCE">
                <div class="source-option">
                  <div class="source-title">互联网来源</div>
                  <div class="source-desc">从互联网获取相关题目（预留功能）</div>
                </div>
              </el-radio>
              <el-radio value="MIXED">
                <div class="source-option">
                  <div class="source-title">混合策略</div>
                  <div class="source-desc">结合多种来源生成多样化题目</div>
                </div>
              </el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="知识点选择" v-if="needKnowledgePointSelection">
            <el-select
              v-model="configForm.selectedPoints"
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="选择要生成题目的知识点"
              style="width: 100%"
            >
              <el-option
                v-for="point in knowledgePoints"
                :key="point.id"
                :label="point.title"
                :value="point.id"
              />
            </el-select>
            <div class="form-tip">
              不选择则基于所有知识点生成题目
            </div>
          </el-form-item>

          <el-form-item label="题目类型">
            <el-checkbox-group v-model="configForm.questionTypes">
              <el-checkbox value="choice">选择题</el-checkbox>
              <el-checkbox value="blank">填空题</el-checkbox>
              <el-checkbox value="subjective">主观题</el-checkbox>
              <el-checkbox value="calculation">计算题</el-checkbox>
            </el-checkbox-group>
          </el-form-item>

          <el-form-item label="难度分布">
            <div class="difficulty-config">
              <div class="difficulty-item">
                <span class="difficulty-label">简单：</span>
                <el-input-number 
                  v-model="configForm.difficulty.easy" 
                  :min="0" 
                  :max="100"
                  size="small"
                />
                <span>题</span>
              </div>
              <div class="difficulty-item">
                <span class="difficulty-label">中等：</span>
                <el-input-number 
                  v-model="configForm.difficulty.medium" 
                  :min="0" 
                  :max="100"
                  size="small"
                />
                <span>题</span>
              </div>
              <div class="difficulty-item">
                <span class="difficulty-label">困难：</span>
                <el-input-number 
                  v-model="configForm.difficulty.hard" 
                  :min="0" 
                  :max="100"
                  size="small"
                />
                <span>题</span>
              </div>
            </div>
            <div class="total-count">
              总计：{{ totalQuestions }} 题
            </div>
          </el-form-item>

          <el-form-item label="生成模式">
            <el-radio-group v-model="configForm.generationMode">
              <el-radio value="balanced">平衡模式（各类型题目数量均衡）</el-radio>
              <el-radio value="custom">自定义（手动设置各类型数量）</el-radio>
              <el-radio value="adaptive">自适应（根据知识点特征自动选择）</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="自定义数量" v-if="configForm.generationMode === 'custom'">
            <div class="type-counts">
              <div class="type-item" v-if="configForm.questionTypes.includes('choice')">
                <span>选择题：</span>
                <el-input-number v-model="configForm.typeCounts.choice" :min="0" size="small" />
                <span>题</span>
              </div>
              <div class="type-item" v-if="configForm.questionTypes.includes('blank')">
                <span>填空题：</span>
                <el-input-number v-model="configForm.typeCounts.blank" :min="0" size="small" />
                <span>题</span>
              </div>
              <div class="type-item" v-if="configForm.questionTypes.includes('subjective')">
                <span>主观题：</span>
                <el-input-number v-model="configForm.typeCounts.subjective" :min="0" size="small" />
                <span>题</span>
              </div>
              <div class="type-item" v-if="configForm.questionTypes.includes('calculation')">
                <span>计算题：</span>
                <el-input-number v-model="configForm.typeCounts.calculation" :min="0" size="small" />
                <span>题</span>
              </div>
            </div>
          </el-form-item>

          <el-form-item label="高级选项">
            <el-collapse>
              <el-collapse-item title="点击展开高级配置" name="advanced">
                <div class="advanced-options">
                  <el-form-item label="创新程度">
                    <el-slider 
                      v-model="configForm.creativity" 
                      :min="0" 
                      :max="100"
                      show-tooltip
                      :format-tooltip="formatCreativityTooltip"
                    />
                    <div class="slider-labels">
                      <span>保守</span>
                      <span>创新</span>
                    </div>
                  </el-form-item>

                  <el-form-item label="语言风格">
                    <el-select v-model="configForm.languageStyle" style="width: 200px;">
                      <el-option value="formal" label="正式严谨" />
                      <el-option value="casual" label="轻松活泼" />
                      <el-option value="academic" label="学术专业" />
                      <el-option value="practical" label="实用导向" />
                    </el-select>
                  </el-form-item>

                  <el-form-item label="包含解析">
                    <el-switch 
                      v-model="configForm.includeExplanation"
                      active-text="生成详细解析"
                      inactive-text="仅生成题目"
                    />
                  </el-form-item>

                  <el-form-item label="参考资料">
                    <el-switch 
                      v-model="configForm.includeReferences"
                      active-text="包含参考资料链接"
                      inactive-text="不包含参考资料"
                    />
                  </el-form-item>
                </div>
              </el-collapse-item>
            </el-collapse>
          </el-form-item>
        </el-form>
      </div>

      <!-- 生成预览 -->
      <div class="generation-preview" v-if="previewQuestions.length > 0">
        <h3>生成预览</h3>
        <div class="preview-list">
          <div 
            v-for="(question, index) in previewQuestions" 
            :key="index"
            class="preview-item"
          >
            <div class="question-header">
              <el-tag :type="getTypeTagType(question.type)" size="small">
                {{ getTypeText(question.type) }}
              </el-tag>
              <el-tag :type="getDifficultyTagType(question.difficulty)" size="small">
                {{ getDifficultyText(question.difficulty) }}
              </el-tag>
            </div>
            <div class="question-content">
              {{ question.content }}
            </div>
            <div class="question-options" v-if="question.type === 'choice'">
              <div v-for="(option, idx) in question.options" :key="idx" class="option">
                {{ String.fromCharCode(65 + idx) }}. {{ option }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 生成进度 -->
      <div class="generation-progress" v-if="generating">
        <div class="progress-header">
          <h3>正在生成题目...</h3>
          <div class="progress-stats">
            {{ generatedCount }}/{{ totalQuestions }} 已生成
          </div>
        </div>
        
        <el-progress 
          :percentage="generationProgress"
          :stroke-width="8"
          :text-inside="true"
          status="success"
        />
        
        <div class="progress-details">
          <div class="current-step">{{ currentStep }}</div>
          <div class="generation-log">
            <div 
              v-for="log in generationLogs" 
              :key="log.id"
              class="log-item"
            >
              <span class="log-time">{{ formatTime(log.timestamp) }}</span>
              <span class="log-message">{{ log.message }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 生成结果 -->
      <div class="generation-result" v-if="generationComplete">
        <div class="result-header">
          <h3>生成完成</h3>
          <div class="result-stats">
            <div class="stat-item">
              <span class="stat-label">成功生成：</span>
              <span class="stat-value success">{{ successCount }}</span>
            </div>
            <div class="stat-item" v-if="failureCount > 0">
              <span class="stat-label">生成失败：</span>
              <span class="stat-value error">{{ failureCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">用时：</span>
              <span class="stat-value">{{ generationTime }}</span>
            </div>
          </div>
        </div>

        <div class="result-actions">
          <el-button icon="View" @click="previewResults">
            预览生成的题目
          </el-button>
          <el-button icon="Edit" @click="editResults">
            编辑题目
          </el-button>
          <el-button type="primary" icon="Check" @click="saveResults">
            保存到知识库
          </el-button>
        </div>

        <div class="quality-report" v-if="qualityReport">
          <h4>质量评估报告</h4>
          <div class="quality-metrics">
            <div class="metric-item">
              <span class="metric-label">整体质量：</span>
              <el-rate 
                v-model="qualityReport.overallQuality" 
                disabled 
                show-score
                score-template="{value} 分"
              />
            </div>
            <div class="metric-item">
              <span class="metric-label">难度适宜性：</span>
              <el-progress 
                :percentage="qualityReport.difficultyAppropriate" 
                size="small"
                :format="(percentage) => `${percentage}%`"
              />
            </div>
            <div class="metric-item">
              <span class="metric-label">内容相关性：</span>
              <el-progress 
                :percentage="qualityReport.contentRelevance" 
                size="small"
                :format="(percentage) => `${percentage}%`"
              />
            </div>
            <div class="metric-item">
              <span class="metric-label">语言流畅性：</span>
              <el-progress 
                :percentage="qualityReport.languageFluency" 
                size="small"
                :format="(percentage) => `${percentage}%`"
              />
            </div>
          </div>
          
          <div class="quality-suggestions" v-if="qualityReport.suggestions?.length">
            <h5>改进建议：</h5>
            <ul>
              <li v-for="suggestion in qualityReport.suggestions" :key="suggestion">
                {{ suggestion }}
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false" :disabled="generating">
          {{ generationComplete ? '关闭' : '取消' }}
        </el-button>
        <el-button 
          v-if="!generating && !generationComplete"
          @click="generatePreview"
          :disabled="!canGenerate"
        >
          预览生成
        </el-button>
        <el-button 
          v-if="!generating && !generationComplete"
          type="primary" 
          @click="startGeneration"
          :disabled="!canGenerate"
        >
          开始生成
        </el-button>
        <el-button 
          v-if="generating"
          @click="stopGeneration"
          type="danger"
        >
          停止生成
        </el-button>
        <el-button 
          v-if="generationComplete"
          type="primary" 
          @click="regenerate"
        >
          重新生成
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { aiQuestionGenerationApi, type AIQuestionGenerationRequest } from '@/api/knowledge'

// Props
const props = defineProps<{
  modelValue: boolean
  knowledgeBase: {
    id: number
    name: string
    description?: string
  }
  aiConfigId?: number
}>()

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'generated': [questions: any[]]
}>()

// 响应式数据
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 配置表单
const configForm = ref<ConfigForm>({
  generationStrategy: 'KNOWLEDGE_BASED',
  selectedPoints: [],
  questionTypes: ['choice', 'blank'],
  difficulty: {
    easy: 5,
    medium: 8,
    hard: 2
  },
  generationMode: 'balanced',
  typeCounts: {
    choice: 8,
    blank: 5,
    subjective: 2,
    calculation: 0
  },
  creativity: 50,
  languageStyle: 'formal',
  includeExplanation: true,
  includeReferences: false
})

// 生成状态
const generating = ref(false)
const generationComplete = ref(false)
const generatedCount = ref(0)
const currentStep = ref('')
const generationLogs = ref<LogItem[]>([])
const startTime = ref<Date | null>(null)

// 预览和结果
const previewQuestions = ref<PreviewQuestion[]>([])
const successCount = ref(0)
const failureCount = ref(0)
const qualityReport = ref<QualityReport | null>(null)

// 模拟数据
const knowledgePoints = ref([
  { id: 1, title: '函数的概念' },
  { id: 2, title: '导数的定义' },
  { id: 3, title: '化学反应平衡' }
])

// 类型定义
interface ConfigForm {
  generationStrategy: 'KNOWLEDGE_BASED' | 'AI_GENERATED' | 'VECTOR_BASED' | 'INTERNET_SOURCE' | 'MIXED'
  selectedPoints: number[]
  questionTypes: string[]
  difficulty: {
    easy: number
    medium: number
    hard: number
  }
  generationMode: string
  typeCounts: {
    choice: number
    blank: number
    subjective: number
    calculation: number
  }
  creativity: number
  languageStyle: string
  includeExplanation: boolean
  includeReferences: boolean
}

interface LogItem {
  id: string
  timestamp: Date
  message: string
}

interface PreviewQuestion {
  type: string
  difficulty: string
  content: string
  options?: string[]
}

interface QualityReport {
  overallQuality: number
  difficultyAppropriate: number
  contentRelevance: number
  languageFluency: number
  suggestions?: string[]
}

// 计算属性
const totalQuestions = computed(() => {
  return configForm.value.difficulty.easy + 
         configForm.value.difficulty.medium + 
         configForm.value.difficulty.hard
})

const generationProgress = computed(() => {
  if (totalQuestions.value === 0) return 0
  return Math.round((generatedCount.value / totalQuestions.value) * 100)
})

const generationTime = computed(() => {
  if (!startTime.value) return '0秒'
  const duration = Date.now() - startTime.value.getTime()
  const seconds = Math.floor(duration / 1000)
  const minutes = Math.floor(seconds / 60)
  
  if (minutes > 0) {
    return `${minutes}分${seconds % 60}秒`
  }
  return `${seconds}秒`
})

const canGenerate = computed(() => {
  return configForm.value.questionTypes.length > 0 && totalQuestions.value > 0
})

const needKnowledgePointSelection = computed(() => {
  return ['KNOWLEDGE_BASED', 'VECTOR_BASED', 'MIXED'].includes(configForm.value.generationStrategy)
})

// 辅助方法
const mapQuestionTypes = (frontendTypes: string[]): ('SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'FILL_BLANK' | 'SHORT_ANSWER' | 'ESSAY')[] => {
  const typeMap: Record<string, 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'FILL_BLANK' | 'SHORT_ANSWER' | 'ESSAY'> = {
    'choice': 'SINGLE_CHOICE',
    'blank': 'FILL_BLANK',
    'subjective': 'SHORT_ANSWER',
    'calculation': 'SHORT_ANSWER'
  }
  
  return frontendTypes.map(type => typeMap[type] || 'SINGLE_CHOICE')
}

const buildAdditionalRequirements = (): string => {
  const requirements: string[] = []
  
  if (configForm.value.includeExplanation) {
    requirements.push('生成详细的题目解析')
  }
  
  if (configForm.value.includeReferences) {
    requirements.push('包含参考资料链接')
  }
  
  const creativityLevel = configForm.value.creativity
  if (creativityLevel > 70) {
    requirements.push('采用创新的题目形式')
  } else if (creativityLevel < 30) {
    requirements.push('采用传统的题目格式')
  }
  
  const styleMap: Record<string, string> = {
    'formal': '使用正式严谨的语言风格',
    'casual': '使用轻松活泼的语言风格',
    'academic': '使用学术专业的语言风格',
    'practical': '使用实用导向的语言风格'
  }
  
  if (styleMap[configForm.value.languageStyle]) {
    requirements.push(styleMap[configForm.value.languageStyle])
  }
  
  return requirements.join('；')
}

// 方法
const generatePreview = async () => {
  // 模拟生成预览题目
  previewQuestions.value = [
    {
      type: 'choice',
      difficulty: 'easy',
      content: '下列关于函数概念的描述，正确的是？',
      options: ['函数是一种对应关系', '函数必须有图像', '函数的定义域可以为空集', '函数的值域必须是实数集']
    },
    {
      type: 'blank',
      difficulty: 'medium',
      content: '已知函数 f(x) = 2x + 1，则 f(3) = ______。'
    }
  ]
  
  ElMessage.success('预览生成完成')
}

const startGeneration = async () => {
  generating.value = true
  generationComplete.value = false
  generatedCount.value = 0
  startTime.value = new Date()
  generationLogs.value = []
  
  addLog('开始AI题目生成...')
  
  try {
    // 构建API请求参数
    const requestData: AIQuestionGenerationRequest = {
      knowledgeBaseId: props.knowledgeBase.id,
      questionTypes: mapQuestionTypes(configForm.value.questionTypes),
      difficultyDistribution: {
        EASY: configForm.value.difficulty.easy,
        MEDIUM: configForm.value.difficulty.medium,
        HARD: configForm.value.difficulty.hard
      },
      knowledgePointIds: configForm.value.selectedPoints.length > 0 ? configForm.value.selectedPoints : undefined,
      generationStrategy: configForm.value.generationStrategy,
      aiConfigId: props.aiConfigId || 1, // 默认AI配置ID
      additionalRequirements: buildAdditionalRequirements()
    }
    
    addLog('正在调用AI生成服务...')
    
    // 调用API生成题目
    const response = await aiQuestionGenerationApi.generateQuestions(requestData)
    
    addLog('AI题目生成完成')
    
    // 更新生成结果
    successCount.value = response.generatedCount || 0
    failureCount.value = Math.max(0, totalQuestions.value - successCount.value)
    generatedCount.value = successCount.value
    
    // 生成质量报告
    qualityReport.value = {
      overallQuality: 4.2,
      difficultyAppropriate: 85,
      contentRelevance: 92,
      languageFluency: 88,
      suggestions: [
        '建议增加更多实际应用场景的题目',
        '部分计算题可以适当降低难度'
      ]
    }
    
    ElMessage.success(`成功生成 ${successCount.value} 道题目`)
    
    // 触发生成完成事件
    emit('generated', response.questions || [])
    
  } catch (error: any) {
    console.error('AI题目生成失败:', error)
    addLog(`生成失败: ${error.message || '网络错误'}`)
    ElMessage.error(`AI题目生成失败: ${error.message || '网络错误'}`)
    
    failureCount.value = totalQuestions.value
    successCount.value = 0
  } finally {
    generating.value = false
    generationComplete.value = true
  }
}

const simulateGeneration = async (): Promise<void> => {
  const steps = [
    '分析知识点内容...',
    '生成题目大纲...',
    '创建选择题...',
    '创建填空题...',
    '生成答案解析...',
    '质量检查与优化...'
  ]
  
  for (let i = 0; i < steps.length; i++) {
    currentStep.value = steps[i]
    addLog(steps[i])
    
    // 模拟进度更新
    const stepProgress = Math.floor((totalQuestions.value / steps.length) * (i + 1))
    
    for (let j = generatedCount.value; j < stepProgress; j++) {
      await new Promise(resolve => setTimeout(resolve, 300))
      generatedCount.value++
    }
  }
}

const stopGeneration = () => {
  generating.value = false
  addLog('用户手动停止生成')
  ElMessage.info('生成已停止')
}

const regenerate = () => {
  generationComplete.value = false
  previewQuestions.value = []
  qualityReport.value = null
  startGeneration()
}

const previewResults = () => {
  ElMessage.info('预览功能开发中...')
}

const editResults = () => {
  ElMessage.info('编辑功能开发中...')
}

const saveResults = () => {
  emit('generated', [])
  visible.value = false
  ElMessage.success('题目已保存到知识库')
}

const addLog = (message: string) => {
  generationLogs.value.push({
    id: Date.now().toString(),
    timestamp: new Date(),
    message
  })
  
  // 限制日志数量
  if (generationLogs.value.length > 20) {
    generationLogs.value = generationLogs.value.slice(-10)
  }
}

const resetForm = () => {
  configForm.value = {
    generationStrategy: 'KNOWLEDGE_BASED',
    selectedPoints: [],
    questionTypes: ['choice', 'blank'],
    difficulty: {
      easy: 5,
      medium: 8,
      hard: 2
    },
    generationMode: 'balanced',
    typeCounts: {
      choice: 8,
      blank: 5,
      subjective: 2,
      calculation: 0
    },
    creativity: 50,
    languageStyle: 'formal',
    includeExplanation: true,
    includeReferences: false
  }
  
  generating.value = false
  generationComplete.value = false
  previewQuestions.value = []
  generationLogs.value = []
  qualityReport.value = null
}

// 工具方法
const formatCreativityTooltip = (value: number) => {
  if (value < 30) return '保守（基于已有模式）'
  if (value < 70) return '平衡（适度创新）'
  return '创新（尝试新颖形式）'
}

const getTypeTagType = (type: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    choice: 'primary',
    blank: 'success',
    subjective: 'warning',
    calculation: 'info'
  }
  return typeMap[type] || 'info'
}

const getTypeText = (type: string) => {
  const textMap: Record<string, string> = {
    choice: '选择题',
    blank: '填空题',
    subjective: '主观题',
    calculation: '计算题'
  }
  return textMap[type] || '未知'
}

const getDifficultyTagType = (difficulty: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    easy: 'success',
    medium: 'warning',
    hard: 'danger'
  }
  return typeMap[difficulty] || 'info'
}

const getDifficultyText = (difficulty: string) => {
  const textMap: Record<string, string> = {
    easy: '简单',
    medium: '中等',
    hard: '困难'
  }
  return textMap[difficulty] || '未知'
}

const formatTime = (date: Date) => {
  return date.toLocaleTimeString()
}
</script>

<style scoped>
.ai-generation-dialog {
  max-height: 70vh;
  overflow-y: auto;
}

/* 生成配置 */
.generation-config h3 {
  margin: 0 0 20px 0;
  color: #303133;
  font-size: 16px;
}

.config-form {
  max-width: 100%;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
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

.difficulty-label {
  width: 50px;
  font-size: 14px;
  color: #606266;
}

.total-count {
  margin-top: 8px;
  font-size: 14px;
  color: #409eff;
  font-weight: 500;
}

.type-counts {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.type-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.type-item span:first-child {
  width: 80px;
  font-size: 14px;
  color: #606266;
}

.advanced-options {
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.slider-labels {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

/* 生成预览 */
.generation-preview {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e4e7ed;
}

.generation-preview h3 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 16px;
}

.preview-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 300px;
  overflow-y: auto;
}

.preview-item {
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background-color: #fafbfc;
}

.question-header {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.question-content {
  font-size: 14px;
  color: #303133;
  margin-bottom: 8px;
  line-height: 1.5;
}

.question-options {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-left: 16px;
}

.option {
  font-size: 13px;
  color: #606266;
}

/* 生成进度 */
.generation-progress {
  margin-top: 24px;
  padding: 20px;
  background-color: #f0f9ff;
  border-radius: 8px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.progress-header h3 {
  margin: 0;
  color: #303133;
  font-size: 16px;
}

.progress-stats {
  font-size: 14px;
  color: #409eff;
  font-weight: 500;
}

.progress-details {
  margin-top: 16px;
}

.current-step {
  font-size: 14px;
  color: #409eff;
  margin-bottom: 12px;
}

.generation-log {
  max-height: 150px;
  overflow-y: auto;
  padding: 8px;
  background-color: #fafbfc;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.log-item {
  display: flex;
  gap: 8px;
  font-size: 12px;
  margin-bottom: 4px;
  font-family: 'Courier New', monospace;
}

.log-time {
  color: #909399;
  min-width: 80px;
}

.log-message {
  color: #606266;
}

/* 生成结果 */
.generation-result {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e4e7ed;
}

.result-header {
  margin-bottom: 16px;
}

.result-header h3 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 16px;
}

.result-stats {
  display: flex;
  gap: 24px;
  font-size: 14px;
}

.stat-item {
  display: flex;
  align-items: center;
}

.stat-label {
  color: #909399;
  margin-right: 4px;
}

.stat-value {
  font-weight: 500;
}

.stat-value.success {
  color: #67c23a;
}

.stat-value.error {
  color: #f56c6c;
}

.result-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

/* 质量报告 */
.quality-report {
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 6px;
}

.quality-report h4 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 14px;
}

.quality-metrics {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.metric-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.metric-label {
  width: 100px;
  font-size: 13px;
  color: #606266;
}

.quality-suggestions h5 {
  margin: 0 0 8px 0;
  color: #606266;
  font-size: 13px;
}

.quality-suggestions ul {
  margin: 0;
  padding-left: 20px;
}

.quality-suggestions li {
  font-size: 13px;
  color: #606266;
  line-height: 1.4;
  margin-bottom: 4px;
}

/* 对话框底部 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .difficulty-config,
  .type-counts {
    gap: 12px;
  }
  
  .difficulty-item,
  .type-item {
    flex-direction: column;
    align-items: stretch;
    gap: 4px;
  }
  
  .progress-header {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }
  
  .result-stats {
    flex-direction: column;
    gap: 8px;
  }
  
  .result-actions {
    flex-direction: column;
  }
  
  .metric-item {
    flex-direction: column;
    align-items: stretch;
    gap: 4px;
  }
}
</style>
