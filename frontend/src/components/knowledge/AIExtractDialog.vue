<template>
  <el-dialog
    v-model="visible"
    title="AI知识点提取"
    width="800px"
    :close-on-click-modal="false"
    @closed="resetForm"
  >
    <div class="ai-extract-dialog">
      <!-- 提取配置 -->
      <el-card class="config-card" shadow="never">
        <template #header>
          <span>提取配置</span>
        </template>
        
        <el-form :model="extractConfig" label-width="120px">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="提取源">
                <el-radio-group v-model="extractConfig.source">
                  <el-radio value="documents">现有文档</el-radio>
                  <el-radio value="text">自定义文本</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="AI模型">
                <el-select
                  v-model="extractConfig.aiConfigId"
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

          <!-- 文档选择 -->
          <el-form-item label="选择文档" v-if="extractConfig.source === 'documents'">
            <el-select
              v-model="extractConfig.documentIds"
              multiple
              placeholder="选择要提取知识点的文档"
              style="width: 100%"
            >
              <el-option
                v-for="doc in availableDocuments"
                :key="doc.id"
                :label="doc.title"
                :value="doc.id"
              >
                <div style="display: flex; justify-content: space-between;">
                  <span>{{ doc.title }}</span>
                  <el-tag :type="getDocTypeTag(doc.type)" size="small">
                    {{ doc.type }}
                  </el-tag>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <!-- 自定义文本 -->
          <el-form-item label="输入文本" v-if="extractConfig.source === 'text'">
            <el-input
              v-model="extractConfig.customText"
              type="textarea"
              :rows="6"
              placeholder="请输入要提取知识点的文本内容"
              maxlength="5000"
              show-word-limit
            />
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="提取策略">
                <el-select v-model="extractConfig.strategy" style="width: 100%">
                  <el-option label="智能提取" value="intelligent">
                    <div>
                      <div>智能提取</div>
                      <div style="font-size: 12px; color: #999;">AI自动识别重要知识点</div>
                    </div>
                  </el-option>
                  <el-option label="完整提取" value="comprehensive">
                    <div>
                      <div>完整提取</div>
                      <div style="font-size: 12px; color: #999;">提取所有可能的知识点</div>
                    </div>
                  </el-option>
                  <el-option label="核心提取" value="core">
                    <div>
                      <div>核心提取</div>
                      <div style="font-size: 12px; color: #999;">只提取核心重点知识</div>
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="难度偏好">
                <el-checkbox-group v-model="extractConfig.difficultyFilter">
                  <el-checkbox value="EASY">简单</el-checkbox>
                  <el-checkbox value="MEDIUM">中等</el-checkbox>
                  <el-checkbox value="HARD">困难</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="高级选项">
            <el-collapse>
              <el-collapse-item title="详细配置" name="advanced">
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="最大提取数量">
                      <el-input-number
                        v-model="extractConfig.maxPoints"
                        :min="1"
                        :max="100"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="自动分类">
                      <el-switch v-model="extractConfig.autoCategory" />
                    </el-form-item>
                  </el-col>
                </el-row>
                
                <el-form-item label="提取类型">
                  <el-checkbox-group v-model="extractConfig.extractTypes">
                    <el-checkbox value="concept">概念定义</el-checkbox>
                    <el-checkbox value="principle">原理公式</el-checkbox>
                    <el-checkbox value="method">解题方法</el-checkbox>
                    <el-checkbox value="example">实例分析</el-checkbox>
                  </el-checkbox-group>
                </el-form-item>
                
                <el-form-item label="附加要求">
                  <el-input
                    v-model="extractConfig.additionalRequirements"
                    type="textarea"
                    :rows="2"
                    placeholder="可以输入特殊要求，如：重点关注某个主题、排除某些内容等"
                  />
                </el-form-item>
              </el-collapse-item>
            </el-collapse>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 提取进度 -->
      <el-card v-if="extracting" class="progress-card" shadow="never">
        <template #header>
          <span>提取进度</span>
        </template>
        
        <div class="progress-content">
          <el-progress :percentage="extractProgress" :status="extractStatus" />
          <div class="progress-info">
            <div class="current-step">{{ currentStep }}</div>
            <div class="progress-details">
              <span>已处理: {{ processedCount }}/{{ totalCount }}</span>
              <span>已提取: {{ extractedCount }} 个知识点</span>
            </div>
          </div>
        </div>

        <div class="progress-logs" v-if="extractionLogs.length > 0">
          <h4>处理日志</h4>
          <div class="log-container">
            <div
              v-for="log in extractionLogs"
              :key="log.id"
              class="log-item"
            >
              <span class="log-time">{{ formatTime(log.timestamp) }}</span>
              <span class="log-message">{{ log.message }}</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 提取结果 -->
      <el-card v-if="extractedPoints.length > 0" class="results-card" shadow="never">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span>提取结果 ({{ extractedPoints.length }} 个知识点)</span>
            <div>
              <el-button size="small" @click="selectAll">全选</el-button>
              <el-button size="small" @click="selectNone">取消全选</el-button>
              <el-button size="small" @click="previewSelected">预览选中</el-button>
            </div>
          </div>
        </template>

        <div class="results-content">
          <div
            v-for="point in extractedPoints"
            :key="point.id"
            class="result-item"
          >
            <div class="result-header">
              <el-checkbox
                v-model="point.selected"
                @change="updateSelection"
              />
              <div class="point-title">{{ point.title }}</div>
              <div class="point-meta">
                <el-tag :type="getDifficultyTagType(point.difficulty)" size="small">
                  {{ point.difficulty }}
                </el-tag>
                <el-tag type="info" size="small">{{ point.category }}</el-tag>
                <span class="confidence">置信度: {{ point.confidence }}%</span>
              </div>
            </div>
            
            <div class="result-content">
              <div class="point-description">{{ point.description }}</div>
              <div class="point-keywords" v-if="point.keywords && point.keywords.length > 0">
                <strong>关键词: </strong>
                <el-tag
                  v-for="keyword in point.keywords"
                  :key="keyword"
                  size="small"
                  class="keyword-tag"
                >
                  {{ keyword }}
                </el-tag>
              </div>
            </div>
            
            <div class="result-actions">
              <el-button size="small" @click="editPoint(point)">编辑</el-button>
              <el-button size="small" type="danger" @click="removePoint(point.id)">删除</el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false" :disabled="extracting">取消</el-button>
        <el-button
          v-if="!extracting && extractedPoints.length === 0"
          type="primary"
          @click="startExtraction"
          :disabled="!canStartExtraction"
        >
          开始提取
        </el-button>
        <el-button
          v-if="extractedPoints.length > 0"
          type="primary"
          @click="saveSelectedPoints"
          :disabled="selectedPointsCount === 0"
        >
          保存选中的知识点 ({{ selectedPointsCount }})
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// Props
const props = defineProps<{
  modelValue: boolean
  knowledgeBaseId: number
}>()

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  extracted: []
}>()

// 响应式数据
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 提取配置
const extractConfig = ref({
  source: 'documents' as 'documents' | 'text',
  documentIds: [] as number[],
  customText: '',
  aiConfigId: 0,
  strategy: 'intelligent',
  difficultyFilter: ['EASY', 'MEDIUM', 'HARD'],
  maxPoints: 20,
  autoCategory: true,
  extractTypes: ['concept', 'principle', 'method', 'example'],
  additionalRequirements: ''
})

// 状态管理
const extracting = ref(false)
const extractProgress = ref(0)
const extractStatus = ref<'success' | 'exception' | undefined>(undefined)
const currentStep = ref('')
const processedCount = ref(0)
const totalCount = ref(0)
const extractedCount = ref(0)

// 数据
const aiConfigs = ref<any[]>([])
const availableDocuments = ref<any[]>([])
const extractedPoints = ref<any[]>([])
const extractionLogs = ref<any[]>([])

// 计算属性
const canStartExtraction = computed(() => {
  return extractConfig.value.aiConfigId && (
    (extractConfig.value.source === 'documents' && extractConfig.value.documentIds.length > 0) ||
    (extractConfig.value.source === 'text' && extractConfig.value.customText.trim().length > 10)
  )
})

const selectedPointsCount = computed(() => {
  return extractedPoints.value.filter(point => point.selected).length
})

// 生命周期
onMounted(() => {
  loadAIConfigs()
  loadDocuments()
})

// 方法
const loadAIConfigs = async () => {
  try {
    // TODO: 从API加载AI配置
    aiConfigs.value = [
      { id: 1, name: 'OpenAI GPT-4', provider: 'OpenAI' },
      { id: 2, name: 'DeepSeek Chat', provider: 'DeepSeek' }
    ]
    
    if (aiConfigs.value.length > 0) {
      extractConfig.value.aiConfigId = aiConfigs.value[0].id
    }
  } catch (error) {
    console.error('Load AI configs failed:', error)
  }
}

const loadDocuments = async () => {
  try {
    // TODO: 从API加载文档列表
    availableDocuments.value = [
      { id: 1, title: '函数基础概念', type: 'PDF' },
      { id: 2, title: '导数应用实例', type: 'Word' },
      { id: 3, title: '化学反应原理', type: 'Text' }
    ]
  } catch (error) {
    console.error('Load documents failed:', error)
  }
}

const startExtraction = async () => {
  try {
    extracting.value = true
    extractProgress.value = 0
    extractStatus.value = undefined
    extractedPoints.value = []
    extractionLogs.value = []
    
    currentStep.value = '准备提取任务...'
    totalCount.value = extractConfig.value.source === 'documents' 
      ? extractConfig.value.documentIds.length 
      : 1
    processedCount.value = 0
    extractedCount.value = 0
    
    addLog('开始AI知识点提取')
    await simulateExtraction()
    
    extractStatus.value = 'success'
    ElMessage.success('知识点提取完成')
  } catch (error) {
    extractStatus.value = 'exception'
    ElMessage.error('提取失败，请重试')
    console.error('Extraction failed:', error)
  } finally {
    extracting.value = false
  }
}

const simulateExtraction = async () => {
  // 模拟提取过程
  const steps = [
    '分析文档内容...',
    '调用AI模型...',
    '解析提取结果...',
    '生成知识点...',
    '完成提取'
  ]
  
  for (let i = 0; i < steps.length; i++) {
    currentStep.value = steps[i]
    addLog(steps[i])
    
    await new Promise(resolve => setTimeout(resolve, 1000))
    extractProgress.value = ((i + 1) / steps.length) * 100
  }
  
  // 生成模拟结果
  extractedPoints.value = [
    {
      id: 1,
      title: '函数的定义',
      description: '函数是一种特殊的对应关系，对于定义域内的每一个输入值，都有唯一的输出值与之对应。',
      category: '概念定义',
      difficulty: 'MEDIUM',
      keywords: ['函数', '定义域', '值域', '对应关系'],
      confidence: 95,
      selected: true
    },
    {
      id: 2,
      title: '函数的性质',
      description: '函数具有单调性、奇偶性、周期性等重要性质，这些性质是分析函数的重要工具。',
      category: '原理公式',
      difficulty: 'HARD',
      keywords: ['单调性', '奇偶性', '周期性'],
      confidence: 88,
      selected: true
    },
    {
      id: 3,
      title: '函数图像分析',
      description: '通过函数图像可以直观地观察函数的性质，包括增减性、极值点、渐近线等。',
      category: '解题方法',
      difficulty: 'EASY',
      keywords: ['图像', '极值', '渐近线'],
      confidence: 92,
      selected: true
    }
  ]
  
  extractedCount.value = extractedPoints.value.length
  processedCount.value = totalCount.value
  addLog(`成功提取 ${extractedCount.value} 个知识点`)
}

const addLog = (message: string) => {
  extractionLogs.value.push({
    id: Date.now(),
    timestamp: new Date(),
    message
  })
  
  // 限制日志数量
  if (extractionLogs.value.length > 20) {
    extractionLogs.value = extractionLogs.value.slice(-10)
  }
}

const selectAll = () => {
  extractedPoints.value.forEach(point => {
    point.selected = true
  })
}

const selectNone = () => {
  extractedPoints.value.forEach(point => {
    point.selected = false
  })
}

const previewSelected = () => {
  const selected = extractedPoints.value.filter(point => point.selected)
  if (selected.length === 0) {
    ElMessage.warning('请先选择要预览的知识点')
    return
  }
  ElMessage.info(`已选择 ${selected.length} 个知识点`)
}

const updateSelection = () => {
  // 更新选择状态
}

const editPoint = (point: any) => {
  ElMessage.info('编辑功能开发中...')
}

const removePoint = (id: number) => {
  const index = extractedPoints.value.findIndex(point => point.id === id)
  if (index > -1) {
    extractedPoints.value.splice(index, 1)
    ElMessage.success('知识点已删除')
  }
}

const saveSelectedPoints = async () => {
  const selected = extractedPoints.value.filter(point => point.selected)
  if (selected.length === 0) {
    ElMessage.warning('请选择要保存的知识点')
    return
  }
  
  try {
    // TODO: 调用API保存知识点
    console.log('Saving points:', selected)
    
    ElMessage.success(`成功保存 ${selected.length} 个知识点`)
    emit('extracted')
    visible.value = false
  } catch (error) {
    ElMessage.error('保存失败，请重试')
    console.error('Save failed:', error)
  }
}

const resetForm = () => {
  extractConfig.value = {
    source: 'documents',
    documentIds: [],
    customText: '',
    aiConfigId: aiConfigs.value[0]?.id || 0,
    strategy: 'intelligent',
    difficultyFilter: ['EASY', 'MEDIUM', 'HARD'],
    maxPoints: 20,
    autoCategory: true,
    extractTypes: ['concept', 'principle', 'method', 'example'],
    additionalRequirements: ''
  }
  
  extractedPoints.value = []
  extractionLogs.value = []
  extracting.value = false
  extractProgress.value = 0
}

// 工具方法
const getDocTypeTag = (type: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    'PDF': 'primary',
    'Word': 'success',
    'Text': 'info',
    'Image': 'warning'
  }
  return typeMap[type] || 'info'
}

const getDifficultyTagType = (difficulty: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    'EASY': 'success',
    'MEDIUM': 'warning',
    'HARD': 'danger'
  }
  return typeMap[difficulty] || 'info'
}

const formatTime = (date: Date) => {
  return date.toLocaleTimeString()
}
</script>

<style scoped>
.ai-extract-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.config-card,
.progress-card,
.results-card {
  border: 1px solid #e4e7ed;
}

.progress-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.current-step {
  font-weight: 600;
  color: #409eff;
}

.progress-details {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: #909399;
}

.progress-logs {
  margin-top: 16px;
}

.progress-logs h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #606266;
}

.log-container {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 8px;
  background: #fafafa;
}

.log-item {
  display: flex;
  gap: 12px;
  padding: 4px 0;
  font-size: 13px;
}

.log-time {
  color: #909399;
  min-width: 80px;
}

.log-message {
  color: #606266;
}

.results-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-item {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 16px;
  background: #fafafa;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.point-title {
  flex: 1;
  font-weight: 600;
  color: #303133;
}

.point-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.confidence {
  font-size: 12px;
  color: #909399;
}

.result-content {
  margin-bottom: 12px;
}

.point-description {
  color: #606266;
  line-height: 1.6;
  margin-bottom: 8px;
}

.point-keywords {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.keyword-tag {
  margin: 2px;
}

.result-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
