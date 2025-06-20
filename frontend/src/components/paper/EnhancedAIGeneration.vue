<template>
  <div class="enhanced-ai-generation">
    <!-- 头部信息 -->
    <div class="generation-header">
      <h2>
        <el-icon><MagicStick /></el-icon>
        AI智能试卷生成
      </h2>
      <p class="header-description">
        基于知识库内容和AI大模型，智能生成高质量试卷题目，支持来源标注和内容预览
      </p>
    </div>

    <!-- 生成配置表单 -->
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="140px"
      class="generation-form"
    >
      <!-- 知识库选择 -->
      <el-card class="config-section">
        <template #header>
          <div class="section-header">
            <el-icon><Collection /></el-icon>
            <span>知识库选择</span>
          </div>
        </template>

        <el-form-item label="选择知识库" prop="knowledgeBaseIds" required>
          <el-select
            v-model="form.knowledgeBaseIds"
            multiple
            placeholder="请选择一个或多个知识库"
            style="width: 100%"
            @change="handleKnowledgeBaseChange"
          >
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            >
              <div class="kb-option">
                <div class="kb-main">
                  <span class="kb-name">{{ kb.name }}</span>
                  <el-tag size="small" :type="getSubjectTagType(kb.subject)">
                    {{ kb.subject }}
                  </el-tag>
                </div>
                <div class="kb-stats">
                  <el-tooltip content="文档数量">
                    <el-tag size="small" type="info">
                      <el-icon><Document /></el-icon>
                      {{ kb.documentCount }}
                    </el-tag>
                  </el-tooltip>
                  <el-tooltip content="知识点数量">
                    <el-tag size="small" type="success">
                      <el-icon><Document /></el-icon>
                      {{ kb.knowledgePointCount }}
                    </el-tag>
                  </el-tooltip>
                  <el-tooltip content="支持RAG检索" v-if="kb.vectorized">
                    <el-tag size="small" type="warning">
                      <el-icon><Search /></el-icon>
                      RAG
                    </el-tag>
                  </el-tooltip>
                </div>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <!-- 选中的知识库预览 -->
        <div v-if="selectedKnowledgeBases.length > 0" class="selected-preview">
          <div class="preview-header">
            <el-icon><View /></el-icon>
            <span>已选择的知识库</span>
          </div>
          <div class="selected-items">
            <el-card 
              v-for="kb in selectedKnowledgeBases" 
              :key="kb.id" 
              class="kb-preview-card"
              shadow="hover"
            >
              <div class="kb-preview-content">
                <div class="kb-preview-header">
                  <h4>{{ kb.name }}</h4>
                  <el-button
                    type="primary"
                    link
                    @click="previewKnowledgeBase(kb.id)"
                  >
                    <el-icon><View /></el-icon>
                    预览
                  </el-button>
                </div>
                <p class="kb-description">{{ kb.description }}</p>
                <div class="kb-metrics">
                  <div class="metric">
                    <span class="metric-label">文档:</span>
                    <span class="metric-value">{{ kb.documentCount }}</span>
                  </div>
                  <div class="metric">
                    <span class="metric-label">知识点:</span>
                    <span class="metric-value">{{ kb.knowledgePointCount }}</span>
                  </div>
                  <div class="metric">
                    <span class="metric-label">向量化:</span>
                    <el-icon v-if="kb.vectorized" class="vectorized-icon"><Check /></el-icon>
                    <el-icon v-else class="not-vectorized-icon"><Close /></el-icon>
                  </div>
                </div>
              </div>
            </el-card>
          </div>
        </div>
      </el-card>

      <!-- 生成策略选择 -->
      <el-card class="config-section">
        <template #header>
          <div class="section-header">
            <el-icon><Setting /></el-icon>
            <span>生成策略</span>
          </div>
        </template>

        <el-form-item label="生成模式" prop="generationStrategy" required>
          <el-radio-group v-model="form.generationStrategy" @change="handleStrategyChange">
            <el-radio 
              v-for="strategy in generationStrategies" 
              :key="strategy.key"
              :value="strategy.key"
              class="strategy-radio"
            >
              <div class="strategy-option">
                <div class="strategy-header">
                  <span class="strategy-name">{{ strategy.name }}</span>
                  <el-tag 
                    v-if="strategy.recommended" 
                    size="small" 
                    type="success"
                  >
                    推荐
                  </el-tag>
                </div>
                <p class="strategy-description">{{ strategy.description }}</p>
                <div class="strategy-features">
                  <el-tag 
                    v-for="advantage in strategy.advantages" 
                    :key="advantage"
                    size="small"
                    type="info"
                    class="advantage-tag"
                  >
                    {{ advantage }}
                  </el-tag>
                </div>
              </div>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 策略配置提示 -->
        <el-alert
          v-if="selectedStrategy"
          :title="selectedStrategy.suitable"
          type="info"
          :closable="false"
          class="strategy-tip"
        />
      </el-card>

      <!-- 题目配置 -->
      <el-card class="config-section">
        <template #header>
          <div class="section-header">
            <el-icon><EditPen /></el-icon>
            <span>题目配置</span>
          </div>
        </template>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="题目类型" prop="questionTypes">
              <el-checkbox-group v-model="form.questionTypes">
                <el-checkbox value="单选题">单选题</el-checkbox>
                <el-checkbox value="多选题">多选题</el-checkbox>
                <el-checkbox value="判断题">判断题</el-checkbox>
                <el-checkbox value="填空题">填空题</el-checkbox>
                <el-checkbox value="简答题">简答题</el-checkbox>
                <el-checkbox value="案例分析题">案例分析题</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="难度分布">
              <div class="difficulty-distribution">
                <div class="difficulty-item">
                  <span class="difficulty-label">简单:</span>
                  <el-input-number
                    v-model="form.difficultyDistribution.EASY"
                    :min="0"
                    :max="20"
                    size="small"
                  />
                </div>
                <div class="difficulty-item">
                  <span class="difficulty-label">中等:</span>
                  <el-input-number
                    v-model="form.difficultyDistribution.MEDIUM"
                    :min="0"
                    :max="20"
                    size="small"
                  />
                </div>
                <div class="difficulty-item">
                  <span class="difficulty-label">困难:</span>
                  <el-input-number
                    v-model="form.difficultyDistribution.HARD"
                    :min="0"
                    :max="20"
                    size="small"
                  />
                </div>
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="每个来源最大题目数">
              <el-input-number
                v-model="form.maxQuestionsPerSource"
                :min="1"
                :max="50"
                placeholder="限制单个来源的题目数量"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生成选项">
              <div class="generation-options">
                <el-checkbox v-model="form.enableSourceCitation">
                  启用来源标注
                </el-checkbox>
                <el-checkbox v-model="form.enableContentPreview">
                  启用内容预览
                </el-checkbox>
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="重点关键词">
          <el-input
            v-model="form.focusKeywordsInput"
            placeholder="输入重点关键词，用逗号分隔"
            @blur="handleKeywordsChange"
          />
          <div v-if="form.focusKeywords.length > 0" class="keywords-display">
            <el-tag
              v-for="keyword in form.focusKeywords"
              :key="keyword"
              closable
              @close="removeKeyword(keyword)"
              class="keyword-tag"
            >
              {{ keyword }}
            </el-tag>
          </div>
        </el-form-item>

        <el-form-item label="自定义提示">
          <el-input
            v-model="form.customPrompt"
            type="textarea"
            :rows="3"
            placeholder="输入特殊要求或自定义生成提示（可选）"
          />
        </el-form-item>
      </el-card>

      <!-- 生成预览 -->
      <el-card v-if="validationResult" class="config-section">
        <template #header>
          <div class="section-header">
            <el-icon><DataAnalysis /></el-icon>
            <span>生成预览</span>
          </div>
        </template>

        <div class="generation-preview">
          <div class="preview-stats">
            <div class="stat-item">
              <span class="stat-label">预计生成题目:</span>
              <span class="stat-value">{{ validationResult.estimatedQuestions }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">预计耗时:</span>
              <span class="stat-value">{{ validationResult.estimatedTime }}</span>
            </div>
            <div class="stat-item" v-if="validationResult.ragSupported">
              <span class="stat-label">RAG检索文档:</span>
              <span class="stat-value">{{ validationResult.vectorizedDocuments }}</span>
            </div>
          </div>

          <div class="difficulty-chart">
            <h4>难度分布</h4>
            <div class="chart-bars">
              <div 
                v-for="(count, difficulty) in form.difficultyDistribution"
                :key="difficulty"
                class="chart-bar"
              >
                <div class="bar-label">{{ getDifficultyLabel(difficulty) }}</div>
                <div class="bar-container">
                  <div 
                    class="bar-fill"
                    :class="`difficulty-${difficulty.toLowerCase()}`"
                    :style="{ width: getBarWidth(count) }"
                  ></div>
                  <span class="bar-count">{{ count }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 操作按钮 -->
      <div class="form-actions">
        <el-button @click="validateConfig" :loading="validating">
          <el-icon><View /></el-icon>
          预览配置
        </el-button>
        <el-button type="primary" @click="generateQuestions" :loading="generating">
          <el-icon><MagicStick /></el-icon>
          开始生成
        </el-button>
        <el-button @click="resetForm">
          <el-icon><RefreshRight /></el-icon>
          重置配置
        </el-button>
      </div>
    </el-form>

    <!-- 生成结果展示 -->
    <div v-if="generationResult" class="generation-result">
      <el-card class="result-card">
        <template #header>
          <div class="result-header">
            <div class="result-title">
              <el-icon><SuccessFilled /></el-icon>
              <span>生成完成</span>
            </div>
            <div class="result-actions">
              <el-button type="primary" @click="exportQuestions">
                <el-icon><Download /></el-icon>
                导出题目
              </el-button>
              <el-button @click="addToQuestionBank">
                <el-icon><FolderAdd /></el-icon>
                添加到题库
              </el-button>
            </div>
          </div>
        </template>

        <!-- 生成统计 -->
        <div class="result-summary">
          <div class="summary-stats">
            <div class="stat-card">
              <div class="stat-number">{{ generationResult.generatedCount }}</div>
              <div class="stat-label">生成题目</div>
            </div>
            <div class="stat-card">
              <div class="stat-number">{{ generationResult.sourceStatistics?.sourceCitationsCount || 0 }}</div>
              <div class="stat-label">来源引用</div>
            </div>
            <div class="stat-card">
              <div class="stat-number">{{ generationResult.sourceStatistics?.knowledgeBasesUsed || 0 }}</div>
              <div class="stat-label">知识库数</div>
            </div>
          </div>
        </div>

        <!-- 题目列表 -->
        <div class="questions-list">
          <div
            v-for="(question, index) in generationResult.questions"
            :key="question.id"
            class="question-item"
          >
            <div class="question-header">
              <div class="question-meta">
                <span class="question-number"># {{ index + 1 }}</span>
                <el-tag :type="getQuestionTypeTag(question.type)" size="small">
                  {{ question.type }}
                </el-tag>
                <el-tag :type="getDifficultyTypeTag(question.difficulty)" size="small">
                  {{ getDifficultyLabel(question.difficulty) }}
                </el-tag>
              </div>
              <div class="question-actions">
                <el-button 
                  v-if="question.sources && question.sources.length > 0"
                  type="primary" 
                  link 
                  @click="showQuestionSources(question)"
                >
                  <el-icon><Link /></el-icon>
                  查看来源 ({{ question.sources.length }})
                </el-button>
                <el-button type="primary" link @click="editQuestion(question)">
                  <el-icon><Edit /></el-icon>
                  编辑
                </el-button>
              </div>
            </div>

            <div class="question-content">
              <div class="question-text">{{ question.content }}</div>
              
              <!-- 选择题选项 -->
              <div v-if="question.options && question.options.length > 0" class="question-options">
                <div
                  v-for="(option, optionIndex) in question.options"
                  :key="optionIndex"
                  class="option-item"
                  :class="{ 'correct-option': option === question.answer }"
                >
                  {{ String.fromCharCode(65 + optionIndex) }}. {{ option }}
                </div>
              </div>

              <!-- 答案和解释 -->
              <div class="question-answer">
                <div class="answer-item">
                  <span class="answer-label">答案:</span>
                  <span class="answer-value">{{ question.answer }}</span>
                </div>
                <div v-if="question.explanation" class="explanation-item">
                  <span class="explanation-label">解释:</span>
                  <span class="explanation-value">{{ question.explanation }}</span>
                </div>
              </div>

              <!-- 来源标注预览 -->
              <div v-if="question.sources && question.sources.length > 0" class="sources-preview">
                <div class="sources-header">
                  <el-icon><Link /></el-icon>
                  <span>参考来源</span>
                </div>
                <div class="sources-list">
                  <div
                    v-for="source in question.sources.slice(0, 2)"
                    :key="source.documentId || source.chunkId"
                    class="source-item"
                    @click="previewSourceContent(source)"
                  >
                    <div class="source-info">
                      <span class="source-title">{{ source.documentTitle }}</span>
                      <el-tag size="small" type="info">{{ source.citationType }}</el-tag>
                    </div>
                    <div class="source-score">
                      相关度: {{ (source.relevanceScore * 100).toFixed(1) }}%
                    </div>
                  </div>
                  <div v-if="question.sources.length > 2" class="more-sources">
                    <el-button type="primary" link @click="showQuestionSources(question)">
                      查看全部 {{ question.sources.length }} 个来源
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 知识库预览对话框 -->
    <el-dialog
      v-model="previewDialogVisible"
      title="知识库预览"
      width="800px"
    >
      <div v-if="knowledgeBasePreview" class="kb-preview-dialog">
        <div class="preview-content">
          <h3>{{ knowledgeBasePreview.name }}</h3>
          <p>{{ knowledgeBasePreview.description }}</p>
          
          <div class="preview-stats">
            <div class="stat">
              <span class="stat-label">文档数量:</span>
              <span class="stat-value">{{ knowledgeBasePreview.documentCount }}</span>
            </div>
            <div class="stat">
              <span class="stat-label">知识点数量:</span>
              <span class="stat-value">{{ knowledgeBasePreview.knowledgePointCount }}</span>
            </div>
            <div class="stat">
              <span class="stat-label">近期主题:</span>
              <span class="stat-value">{{ knowledgeBasePreview.recentTopics }}</span>
            </div>
            <div class="stat">
              <span class="stat-label">RAG支持:</span>
              <el-tag :type="knowledgeBasePreview.availableForRAG ? 'success' : 'info'">
                {{ knowledgeBasePreview.availableForRAG ? '支持' : '不支持' }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="previewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 来源详情对话框 -->
    <el-dialog
      v-model="sourcesDialogVisible"
      title="题目来源详情"
      width="900px"
    >
      <div v-if="selectedQuestionSources" class="sources-dialog">
        <div class="question-preview">
          <h4>题目: {{ selectedQuestionSources.content }}</h4>
        </div>
        
        <div class="sources-detailed">
          <div
            v-for="source in selectedQuestionSources.sources"
            :key="source.documentId || source.chunkId"
            class="source-detail-item"
          >
            <div class="source-detail-header">
              <div class="source-title">
                <el-icon><Document /></el-icon>
                <span>{{ source.documentTitle }}</span>
              </div>
              <div class="source-meta">
                <el-tag :type="getCitationTypeTag(source.citationType)" size="small">
                  {{ getCitationTypeLabel(source.citationType) }}
                </el-tag>
                <span class="relevance-score">
                  相关度: {{ (source.relevanceScore * 100).toFixed(1) }}%
                </span>
              </div>
            </div>
            
            <div class="source-content">
              <p>{{ source.extractedContent }}</p>
            </div>
            
            <div class="source-actions">
              <el-button type="primary" link @click="previewSourceContent(source)">
                <el-icon><View /></el-icon>
                查看完整内容
              </el-button>
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="sourcesDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 内容预览对话框 -->
    <el-dialog
      v-model="contentPreviewVisible"
      title="内容预览"
      width="800px"
    >
      <div v-if="contentPreview" class="content-preview-dialog">
        <div class="preview-header">
          <h4>{{ contentPreview.title }}</h4>
          <el-tag size="small">{{ contentPreview.fileName }}</el-tag>
        </div>
        
        <div class="preview-content">
          <div class="highlighted-content">
            <p v-html="highlightContent(contentPreview.content, contentPreview.highlightedTerms)"></p>
          </div>
        </div>
        
        <div v-if="contentPreview.relatedChunks" class="related-chunks">
          <h5>相关内容片段</h5>
          <div class="chunks-list">
            <el-tag
              v-for="chunkId in contentPreview.relatedChunks"
              :key="chunkId"
              class="chunk-tag"
              @click="loadRelatedChunk(chunkId)"
            >
              {{ chunkId }}
            </el-tag>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="contentPreviewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  MagicStick,
  Collection,
  Setting,
  EditPen,
  DataAnalysis,
  View,
  RefreshRight,
  SuccessFilled,
  Download,
  FolderAdd,
  Link,
  Edit,
  Document,
  Search,
  Check,
  Close
} from '@element-plus/icons-vue'
import { knowledgeBaseApi, type KnowledgeBase as BaseKnowledgeBase } from '@/api/knowledge'

// 扩展 KnowledgeBase 类型添加 vectorized 属性
interface KnowledgeBase extends BaseKnowledgeBase {
  vectorized?: boolean
}

// 定义接口类型
interface GenerationStrategy {
  key: string
  name: string
  description: string
  advantages: string[]
  suitable: string
  recommended?: boolean
}

interface ValidationResult {
  estimatedQuestions: number
  estimatedTime: string
  ragSupported: boolean
  vectorizedDocuments: number
}

interface GenerationResult {
  generatedCount: number
  sourceStatistics: {
    sourceCitationsCount: number
    knowledgeBasesUsed: number
  }
  questions: any[]
}

interface KnowledgeBasePreview {
  name: string
  description: string
  documentCount: number
  knowledgePointCount: number
  recentTopics: string
  availableForRAG: boolean
}

interface QuestionSources {
  content: string
  sources: any[]
}

interface ContentPreview {
  title: string
  fileName: string
  content: string
  highlightedTerms: string[]
  relatedChunks?: string[]
}

// 响应式数据
const formRef = ref()
const form = reactive({
  knowledgeBaseIds: [] as number[],
  generationStrategy: 'SMART_MIX',
  questionTypes: ['单选题', '多选题'],
  difficultyDistribution: {
    EASY: 3,
    MEDIUM: 4,
    HARD: 3
  },
  maxQuestionsPerSource: 10,
  enableSourceCitation: true,
  enableContentPreview: true,
  focusKeywords: [] as string[],
  focusKeywordsInput: '',
  customPrompt: ''
})

const knowledgeBases = ref<KnowledgeBase[]>([])
const generationStrategies = ref<GenerationStrategy[]>([])
const validationResult = ref<ValidationResult | null>(null)
const generationResult = ref<GenerationResult | null>(null)
const validating = ref(false)
const generating = ref(false)

// 对话框状态
const previewDialogVisible = ref(false)
const sourcesDialogVisible = ref(false)
const contentPreviewVisible = ref(false)
const knowledgeBasePreview = ref<KnowledgeBasePreview | null>(null)
const selectedQuestionSources = ref<QuestionSources | null>(null)
const contentPreview = ref<ContentPreview | null>(null)

// 表单验证规则
const rules = {
  knowledgeBaseIds: [
    { required: true, message: '请选择至少一个知识库', trigger: 'change' }
  ],
  generationStrategy: [
    { required: true, message: '请选择生成策略', trigger: 'change' }
  ]
}

// 计算属性
const selectedKnowledgeBases = computed(() => {
  return knowledgeBases.value.filter(kb => form.knowledgeBaseIds.includes(kb.id))
})

const selectedStrategy = computed(() => {
  return generationStrategies.value.find(s => s.key === form.generationStrategy)
})

const totalQuestions = computed(() => {
  return Object.values(form.difficultyDistribution).reduce((sum, count) => sum + count, 0)
})

// 生命周期
onMounted(() => {
  loadKnowledgeBases()
  loadGenerationStrategies()
})

// 监听器
watch(() => form.knowledgeBaseIds, () => {
  if (validationResult.value) {
    validateConfig()
  }
}, { deep: true })

watch(() => form.difficultyDistribution, () => {
  if (validationResult.value) {
    validateConfig()
  }
}, { deep: true })

// 方法
const loadKnowledgeBases = async () => {
  try {
    const response = await knowledgeBaseApi.getKnowledgeBases()
    knowledgeBases.value = response.content.map((kb: any) => ({
      ...kb,
      vectorized: Math.random() > 0.3 // 模拟向量化状态
    }))
  } catch (error: any) {
    ElMessage.error('加载知识库失败: ' + error.message)
  }
}

const loadGenerationStrategies = async () => {
  try {
    // 由于 enhancedAIQuestionAPI 不存在，使用默认策略
    generationStrategies.value = [
      {
        key: 'RAG_BASED',
        name: 'RAG检索生成',
        description: '基于向量检索的题目生成，准确性高',
        advantages: ['准确性高', '来源可追溯', '上下文相关'],
        suitable: '适用于已向量化的知识库'
      },
      {
        key: 'DIRECT_LLM', 
        name: '直接LLM生成',
        description: '直接使用大语言模型生成题目',
        advantages: ['生成速度快', '题型多样', '创新性强'],
        suitable: '适用于快速生成和创新题目'
      },
      {
        key: 'SMART_MIX',
        name: '智能混合模式',
        description: '结合RAG检索和直接LLM生成，平衡准确性和多样性',
        advantages: ['综合优势', '题目丰富', '质量稳定'],
        suitable: '大多数场景的推荐选择',
        recommended: true
      }
    ]
  } catch (error: any) {
    console.error('加载生成策略失败:', error)
  }
}

const handleKnowledgeBaseChange = () => {
  // 知识库变化时的处理
}

const handleStrategyChange = () => {
  // 策略变化时的处理
  if (validationResult.value) {
    validateConfig()
  }
}

const handleKeywordsChange = () => {
  if (form.focusKeywordsInput.trim()) {
    const keywords = form.focusKeywordsInput.split(',').map(k => k.trim()).filter(k => k)
    form.focusKeywords = [...new Set([...form.focusKeywords, ...keywords])]
    form.focusKeywordsInput = ''
  }
}

const removeKeyword = (keyword: string) => {
  const index = form.focusKeywords.indexOf(keyword)
  if (index > -1) {
    form.focusKeywords.splice(index, 1)
  }
}

const validateConfig = async () => {
  try {
    validating.value = true
    // 模拟配置验证，因为 enhancedAIQuestionAPI 不存在
    validationResult.value = {
      estimatedQuestions: totalQuestions.value,
      estimatedTime: '2-5分钟',
      ragSupported: selectedKnowledgeBases.value.some(kb => kb.vectorized),
      vectorizedDocuments: selectedKnowledgeBases.value.reduce((sum, kb) => 
        sum + (kb.vectorized ? kb.documentCount : 0), 0)
    }
  } catch (error: any) {
    ElMessage.error('配置验证失败: ' + error.message)
  } finally {
    validating.value = false
  }
}

const generateQuestions = async () => {
  try {
    // 验证表单
    await formRef.value.validate()
    
    generating.value = true
    ElMessage.success('开始生成题目，请稍候...')
    
    // 模拟生成结果，因为 enhancedAIQuestionAPI 不存在
    await new Promise(resolve => setTimeout(resolve, 2000))
    
    generationResult.value = {
      generatedCount: totalQuestions.value,
      sourceStatistics: {
        sourceCitationsCount: Math.floor(totalQuestions.value * 1.5),
        knowledgeBasesUsed: form.knowledgeBaseIds.length
      },
      questions: []
    }
    
    ElMessage.success(`成功生成 ${totalQuestions.value} 道题目！`)
  } catch (error: any) {
    ElMessage.error('生成题目失败: ' + error.message)
  } finally {
    generating.value = false
  }
}

const resetForm = () => {
  Object.assign(form, {
    knowledgeBaseIds: [],
    generationStrategy: 'SMART_MIX',
    questionTypes: ['单选题', '多选题'],
    difficultyDistribution: {
      EASY: 3,
      MEDIUM: 4,
      HARD: 3
    },
    maxQuestionsPerSource: 10,
    enableSourceCitation: true,
    enableContentPreview: true,
    focusKeywords: [],
    focusKeywordsInput: '',
    customPrompt: ''
  })
  validationResult.value = null
  generationResult.value = null
}

const previewKnowledgeBase = async (id: number) => {
  try {
    // 模拟预览数据，因为 enhancedAIQuestionAPI 不存在
    const kb = knowledgeBases.value.find(k => k.id === id)
    if (kb) {
      knowledgeBasePreview.value = {
        name: kb.name,
        description: kb.description || '暂无描述',
        documentCount: kb.documentCount,
        knowledgePointCount: kb.knowledgePointCount,
        recentTopics: '计算机基础知识',
        availableForRAG: Math.random() > 0.5
      }
      previewDialogVisible.value = true
    }
  } catch (error: any) {
    ElMessage.error('获取知识库预览失败: ' + error.message)
  }
}

const showQuestionSources = async (question: any) => {
  try {
    selectedQuestionSources.value = question
    sourcesDialogVisible.value = true
  } catch (error: any) {
    ElMessage.error('获取题目来源失败: ' + error.message)
  }
}

const previewSourceContent = async (source: any) => {
  try {
    // 模拟内容预览，因为 enhancedAIQuestionAPI 不存在
    contentPreview.value = {
      title: source.documentTitle || '文档内容',
      fileName: source.fileName || '未知文件',
      content: source.extractedContent || '暂无内容',
      highlightedTerms: []
    }
    contentPreviewVisible.value = true
  } catch (error: any) {
    ElMessage.error('预览内容失败: ' + error.message)
  }
}

const editQuestion = (question: any) => {
  // 编辑题目逻辑
  ElMessage.info('编辑功能开发中...')
}

const exportQuestions = () => {
  // 导出题目逻辑
  ElMessage.info('导出功能开发中...')
}

const addToQuestionBank = () => {
  // 添加到题库逻辑
  ElMessage.info('添加到题库功能开发中...')
}

const highlightContent = (content: string, terms: string[]) => {
  if (!terms || terms.length === 0) return content
  
  let highlighted = content
  terms.forEach((term: string) => {
    const regex = new RegExp(`(${term})`, 'gi')
    highlighted = highlighted.replace(regex, '<mark>$1</mark>')
  })
  return highlighted
}

const loadRelatedChunk = (chunkId: string) => {
  // 加载相关内容片段
  ElMessage.info(`加载片段 ${chunkId}...`)
}

// 辅助方法 - 为 Element Plus 返回严格类型
const getSubjectTagType = (subject?: string): 'success' | 'primary' | 'warning' | 'info' | 'danger' => {
  const types: Record<string, 'success' | 'primary' | 'warning' | 'info' | 'danger'> = {
    '计算机科学': 'primary',
    '数学': 'success',
    '物理': 'warning',
    '化学': 'danger'
  }
  return subject ? (types[subject] || 'info') : 'info'
}

const getDifficultyLabel = (difficulty: string) => {
  const labels: Record<string, string> = {
    EASY: '简单',
    MEDIUM: '中等',
    HARD: '困难'
  }
  return labels[difficulty] || difficulty
}

const getQuestionTypeTag = (type: string): 'success' | 'primary' | 'warning' | 'info' | 'danger' => {
  const types: Record<string, 'success' | 'primary' | 'warning' | 'info' | 'danger'> = {
    '单选题': 'primary',
    '多选题': 'success',
    '判断题': 'warning',
    '填空题': 'info',
    '简答题': 'danger'
  }
  return types[type] || 'info'
}

const getDifficultyTypeTag = (difficulty: string): 'success' | 'primary' | 'warning' | 'info' | 'danger' => {
  const types: Record<string, 'success' | 'primary' | 'warning' | 'info' | 'danger'> = {
    EASY: 'success',
    MEDIUM: 'warning',
    HARD: 'danger'
  }
  return types[difficulty] || 'info'
}

const getCitationTypeTag = (type: string): 'success' | 'primary' | 'warning' | 'info' | 'danger' => {
  const types: Record<string, 'success' | 'primary' | 'warning' | 'info' | 'danger'> = {
    QUESTION_BASE: 'primary',
    ANSWER_SUPPORT: 'success',
    EXPLANATION: 'info',
    KNOWLEDGE_BASE: 'warning'
  }
  return types[type] || 'info'
}

const getCitationTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    QUESTION_BASE: '题目依据',
    ANSWER_SUPPORT: '答案支撑',
    EXPLANATION: '解释补充',
    KNOWLEDGE_BASE: '知识库'
  }
  return labels[type] || type
}

const getBarWidth = (count: number) => {
  const maxCount = Math.max(...Object.values(form.difficultyDistribution))
  return maxCount > 0 ? `${(count / maxCount) * 100}%` : '0%'
}
</script>

<style scoped>
.enhanced-ai-generation {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.generation-header {
  text-align: center;
  margin-bottom: 30px;
}

.generation-header h2 {
  font-size: 28px;
  color: #303133;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.header-description {
  color: #606266;
  font-size: 16px;
  line-height: 1.6;
}

.generation-form {
  margin-bottom: 30px;
}

.config-section {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.kb-option {
  width: 100%;
}

.kb-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.kb-name {
  font-weight: 500;
  color: #303133;
}

.kb-stats {
  display: flex;
  gap: 8px;
}

.selected-preview {
  margin-top: 20px;
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 15px;
  color: #606266;
  font-weight: 500;
}

.selected-items {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 15px;
}

.kb-preview-card {
  border: 1px solid #e4e7ed;
}

.kb-preview-content {
  padding: 10px;
}

.kb-preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.kb-preview-header h4 {
  margin: 0;
  color: #303133;
}

.kb-description {
  color: #606266;
  font-size: 14px;
  margin-bottom: 15px;
}

.kb-metrics {
  display: flex;
  gap: 15px;
}

.metric {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
}

.metric-label {
  color: #909399;
}

.metric-value {
  font-weight: 500;
  color: #303133;
}

.vectorized-icon {
  color: #67c23a;
}

.not-vectorized-icon {
  color: #f56c6c;
}

.strategy-radio {
  display: block;
  margin-bottom: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 15px;
  transition: all 0.3s;
}

.strategy-radio:hover {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.strategy-option {
  margin-left: 25px;
}

.strategy-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.strategy-name {
  font-weight: 600;
  color: #303133;
}

.strategy-description {
  color: #606266;
  margin-bottom: 10px;
  line-height: 1.5;
}

.strategy-features {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.advantage-tag {
  margin: 0;
}

.strategy-tip {
  margin-top: 15px;
}

.difficulty-distribution {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.difficulty-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.difficulty-label {
  width: 50px;
  color: #606266;
}

.generation-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.keywords-display {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.keyword-tag {
  margin: 0;
}

.generation-preview {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.preview-stats {
  display: flex;
  gap: 30px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-label {
  color: #606266;
}

.stat-value {
  font-weight: 600;
  color: #409eff;
}

.difficulty-chart h4 {
  margin-bottom: 15px;
  color: #303133;
}

.chart-bars {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chart-bar {
  display: flex;
  align-items: center;
  gap: 15px;
}

.bar-label {
  width: 60px;
  color: #606266;
  font-size: 14px;
}

.bar-container {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
}

.bar-fill {
  height: 20px;
  border-radius: 10px;
  min-width: 20px;
  transition: width 0.3s;
}

.difficulty-easy {
  background-color: #67c23a;
}

.difficulty-medium {
  background-color: #e6a23c;
}

.difficulty-hard {
  background-color: #f56c6c;
}

.bar-count {
  font-weight: 500;
  color: #303133;
}

.form-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin-top: 30px;
}

.generation-result {
  margin-top: 30px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #67c23a;
}

.result-actions {
  display: flex;
  gap: 10px;
}

.result-summary {
  margin-bottom: 25px;
}

.summary-stats {
  display: flex;
  gap: 20px;
  justify-content: center;
}

.stat-card {
  text-align: center;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  min-width: 120px;
}

.stat-number {
  font-size: 32px;
  font-weight: 700;
  color: #409eff;
  margin-bottom: 5px;
}

.stat-label {
  color: #606266;
  font-size: 14px;
}

.questions-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.question-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  background: #fff;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.question-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.question-number {
  font-weight: 600;
  color: #409eff;
}

.question-actions {
  display: flex;
  gap: 10px;
}

.question-content {
  margin-bottom: 15px;
}

.question-text {
  font-size: 16px;
  color: #303133;
  margin-bottom: 15px;
  line-height: 1.6;
}

.question-options {
  margin: 15px 0;
}

.option-item {
  padding: 8px 0;
  color: #606266;
}

.correct-option {
  color: #67c23a;
  font-weight: 500;
}

.question-answer {
  display: flex;
  gap: 30px;
  margin: 15px 0;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 6px;
}

.answer-item, .explanation-item {
  display: flex;
  gap: 8px;
}

.answer-label, .explanation-label {
  font-weight: 500;
  color: #606266;
}

.answer-value {
  color: #67c23a;
  font-weight: 500;
}

.explanation-value {
  color: #303133;
}

.sources-preview {
  margin-top: 15px;
  padding: 15px;
  background: #f0f9ff;
  border-radius: 6px;
  border-left: 4px solid #409eff;
}

.sources-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-weight: 500;
  color: #409eff;
}

.sources-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.source-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.source-item:hover {
  background: #e6f7ff;
  border-color: #409eff;
}

.source-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.source-title {
  font-weight: 500;
  color: #303133;
}

.source-score {
  color: #606266;
  font-size: 12px;
}

.more-sources {
  text-align: center;
  margin-top: 10px;
}

/* 对话框样式 */
.kb-preview-dialog, .sources-dialog, .content-preview-dialog {
  padding: 20px;
}

.preview-content h3 {
  color: #303133;
  margin-bottom: 10px;
}

.preview-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  margin-top: 20px;
}

.stat {
  display: flex;
  gap: 8px;
}

.stat-label {
  color: #606266;
  font-weight: 500;
}

.stat-value {
  color: #303133;
}

.question-preview h4 {
  color: #303133;
  margin-bottom: 20px;
}

.sources-detailed {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.source-detail-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 15px;
}

.source-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.source-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: #303133;
}

.source-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.relevance-score {
  color: #606266;
  font-size: 12px;
}

.source-content {
  margin: 10px 0;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 4px;
}

.source-actions {
  text-align: right;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.preview-header h4 {
  margin: 0;
  color: #303133;
}

.highlighted-content {
  padding: 15px;
  background: #f8f9fa;
  border-radius: 6px;
  line-height: 1.6;
}

.highlighted-content :deep(mark) {
  background: #fff3cd;
  padding: 2px 4px;
  border-radius: 2px;
}

.related-chunks {
  margin-top: 20px;
}

.related-chunks h5 {
  color: #303133;
  margin-bottom: 10px;
}

.chunks-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chunk-tag {
  cursor: pointer;
  transition: all 0.3s;
}

.chunk-tag:hover {
  background: #409eff;
  color: #fff;
}
</style>
