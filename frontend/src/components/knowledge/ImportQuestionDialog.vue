<template>
  <el-dialog
    v-model="visible"
    title="导入题库"
    width="800px"
    :close-on-click-modal="false"
    @closed="resetForm"
  >
    <div class="import-question-dialog">
      <!-- 导入方式选择 -->
      <el-card class="import-method-card" shadow="never">
        <template #header>
          <span>导入方式</span>
        </template>
        
        <el-radio-group v-model="importMethod" class="method-options">
          <el-radio value="file" class="method-option">
            <div class="method-content">
              <el-icon size="24"><DocumentCopy /></el-icon>
              <div class="method-text">
                <div class="method-title">文件导入</div>
                <div class="method-desc">支持 Excel、Word、TXT 等格式</div>
              </div>
            </div>
          </el-radio>
          
          <el-radio value="template" class="method-option">
            <div class="method-content">
              <el-icon size="24"><Download /></el-icon>
              <div class="method-text">
                <div class="method-title">模板导入</div>
                <div class="method-desc">下载标准模板，填写后导入</div>
              </div>
            </div>
          </el-radio>
          
          <el-radio value="text" class="method-option">
            <div class="method-content">
              <el-icon size="24"><EditPen /></el-icon>
              <div class="method-text">
                <div class="method-title">文本导入</div>
                <div class="method-desc">直接粘贴题目文本内容</div>
              </div>
            </div>
          </el-radio>
          
          <el-radio value="database" class="method-option">
            <div class="method-content">
              <el-icon size="24"><Coin /></el-icon>
              <div class="method-text">
                <div class="method-title">题库导入</div>
                <div class="method-desc">从现有题库中选择导入</div>
              </div>
            </div>
          </el-radio>
        </el-radio-group>
      </el-card>

      <!-- 文件上传 -->
      <el-card v-if="importMethod === 'file'" class="upload-card" shadow="never">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span>文件上传</span>
            <el-button size="small" @click="downloadTemplate">下载模板</el-button>
          </div>
        </template>
        
        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          :show-file-list="true"
          :accept="'.xlsx,.xls,.docx,.doc,.txt'"
          :on-change="handleFileChange"
          :before-upload="beforeUpload"
          drag
          multiple
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持 Excel (.xlsx, .xls)、Word (.docx, .doc)、文本 (.txt) 格式
            </div>
          </template>
        </el-upload>

        <div v-if="uploadedFiles.length > 0" class="uploaded-files">
          <h4>已选文件</h4>
          <div
            v-for="file in uploadedFiles"
            :key="file.name"
            class="file-item"
          >
            <el-icon><DocumentCopy /></el-icon>
            <span class="file-name">{{ file.name }}</span>
            <span class="file-size">{{ formatFileSize(file.size) }}</span>
            <el-button size="small" type="danger" link @click="removeFile(file)">
              删除
            </el-button>
          </div>
        </div>
      </el-card>

      <!-- 模板下载 -->
      <el-card v-if="importMethod === 'template'" class="template-card" shadow="never">
        <template #header>
          <span>模板下载</span>
        </template>
        
        <div class="template-options">
          <div class="template-item" v-for="template in templates" :key="template.type">
            <div class="template-info">
              <el-icon size="32" :color="template.color">
                <component :is="template.icon" />
              </el-icon>
              <div class="template-details">
                <div class="template-title">{{ template.title }}</div>
                <div class="template-desc">{{ template.description }}</div>
              </div>
            </div>
            <el-button @click="downloadTemplateFile(template.type)">下载模板</el-button>
          </div>
        </div>
      </el-card>

      <!-- 文本导入 -->
      <el-card v-if="importMethod === 'text'" class="text-import-card" shadow="never">
        <template #header>
          <span>文本导入</span>
        </template>
        
        <div class="text-import-content">
          <el-form :model="textImportForm" label-width="120px">
            <el-form-item label="文本格式">
              <el-select v-model="textImportForm.format" style="width: 200px;">
                <el-option label="标准格式" value="standard" />
                <el-option label="自定义格式" value="custom" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="题目内容">
              <el-input
                v-model="textImportForm.content"
                type="textarea"
                :rows="12"
                placeholder="请按照以下格式输入题目：&#10;&#10;1. 题目内容&#10;A. 选项1&#10;B. 选项2&#10;C. 选项3&#10;D. 选项4&#10;答案：A&#10;解析：题目解析内容&#10;&#10;2. 下一道题目..."
                maxlength="10000"
                show-word-limit
              />
            </el-form-item>
            
            <el-form-item label="导入设置">
              <el-checkbox-group v-model="textImportForm.options">
                <el-checkbox value="auto_detect_type">自动识别题型</el-checkbox>
                <el-checkbox value="auto_set_difficulty">自动设置难度</el-checkbox>
                <el-checkbox value="extract_knowledge_points">提取关联知识点</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-form>
        </div>
      </el-card>

      <!-- 题库选择 -->
      <el-card v-if="importMethod === 'database'" class="database-card" shadow="never">
        <template #header>
          <span>题库选择</span>
        </template>
        
        <div class="database-content">
          <el-form :model="databaseForm" label-width="120px">
            <el-form-item label="源题库">
              <el-select
                v-model="databaseForm.sourceKnowledgeBaseId"
                placeholder="选择源题库"
                style="width: 100%"
                @change="loadDatabaseQuestions"
              >
                <el-option
                  v-for="kb in availableKnowledgeBases"
                  :key="kb.id"
                  :label="kb.name"
                  :value="kb.id"
                >
                  <div style="display: flex; justify-content: space-between;">
                    <span>{{ kb.name }}</span>
                    <el-tag size="small">{{ kb.questionCount || 0 }} 题</el-tag>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
            
            <el-form-item label="筛选条件">
              <el-row :gutter="20">
                <el-col :span="8">
                  <el-select v-model="databaseForm.typeFilter" placeholder="题目类型" multiple>
                    <el-option label="选择题" value="choice" />
                    <el-option label="填空题" value="blank" />
                    <el-option label="主观题" value="subjective" />
                    <el-option label="计算题" value="calculation" />
                  </el-select>
                </el-col>
                <el-col :span="8">
                  <el-select v-model="databaseForm.difficultyFilter" placeholder="难度级别" multiple>
                    <el-option label="简单" value="EASY" />
                    <el-option label="中等" value="MEDIUM" />
                    <el-option label="困难" value="HARD" />
                  </el-select>
                </el-col>
                <el-col :span="8">
                  <el-input
                    v-model="databaseForm.searchKeyword"
                    placeholder="搜索关键词"
                    clearable
                  />
                </el-col>
              </el-row>
            </el-form-item>
          </el-form>

          <div v-if="databaseQuestions.length > 0" class="question-list">
            <div class="list-header">
              <el-checkbox
                v-model="selectAll"
                @change="handleSelectAll"
                :indeterminate="isIndeterminate"
              >
                全选 (已选择 {{ selectedQuestions.length }} / {{ databaseQuestions.length }})
              </el-checkbox>
            </div>
            
            <div class="question-items">
              <el-checkbox-group v-model="selectedQuestions">
                <div
                  v-for="question in databaseQuestions"
                  :key="question.id"
                  class="question-item"
                >
                  <el-checkbox
                    :label="question.id"
                    class="question-checkbox"
                  />
                  <div class="question-content">
                  <div class="question-header">
                    <el-tag :type="getTypeTagType(question.type)" size="small">
                      {{ getTypeText(question.type) }}
                    </el-tag>
                    <el-tag :type="getDifficultyTagType(question.difficulty)" size="small">
                      {{ question.difficulty }}
                    </el-tag>
                    <span class="question-score">{{ question.score }}分</span>
                  </div>
                  <div class="question-text">{{ question.content }}</div>
                </div>
                </div>
              </el-checkbox-group>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 导入进度 -->
      <el-card v-if="importing" class="progress-card" shadow="never">
        <template #header>
          <span>导入进度</span>
        </template>
        
        <div class="progress-content">
          <el-progress :percentage="importProgress" :status="importStatus" />
          <div class="progress-info">
            <div class="current-step">{{ currentStep }}</div>
            <div class="progress-details">
              <span>已处理: {{ processedCount }}/{{ totalCount }}</span>
              <span>成功: {{ successCount }}, 失败: {{ failureCount }}</span>
            </div>
          </div>
        </div>

        <div class="import-logs" v-if="importLogs.length > 0">
          <h4>导入日志</h4>
          <div class="log-container">
            <div
              v-for="log in importLogs"
              :key="log.id"
              class="log-item"
              :class="{ 'log-error': log.type === 'error', 'log-warning': log.type === 'warning' }"
            >
              <span class="log-time">{{ formatTime(log.timestamp) }}</span>
              <span class="log-message">{{ log.message }}</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 导入结果 -->
      <el-card v-if="importResult" class="result-card" shadow="never">
        <template #header>
          <span>导入结果</span>
        </template>
        
        <div class="result-content">
          <div class="result-summary">
            <div class="summary-item">
              <span class="summary-label">总题目数：</span>
              <span class="summary-value">{{ importResult.total }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">成功导入：</span>
              <span class="summary-value success">{{ importResult.success }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">导入失败：</span>
              <span class="summary-value error">{{ importResult.failed }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">重复题目：</span>
              <span class="summary-value warning">{{ importResult.duplicates }}</span>
            </div>
          </div>

          <div v-if="importResult.errors && importResult.errors.length > 0" class="error-details">
            <h4>错误详情</h4>
            <div
              v-for="error in importResult.errors"
              :key="error.id"
              class="error-item"
            >
              <span class="error-line">第{{ error.line }}行：</span>
              <span class="error-message">{{ error.message }}</span>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false" :disabled="importing">取消</el-button>
        <el-button
          v-if="!importing && !importResult"
          type="primary"
          @click="startImport"
          :disabled="!canStartImport"
        >
          开始导入
        </el-button>
        <el-button
          v-if="importResult"
          type="primary"
          @click="confirmImport"
        >
          确认导入
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  DocumentCopy,
  Download,
  EditPen,
  Coin,
  UploadFilled
} from '@element-plus/icons-vue'

// Props
const props = defineProps<{
  modelValue: boolean
  knowledgeBaseId: number
}>()

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  imported: []
}>()

// 响应式数据
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const importMethod = ref('file')
const uploadRef = ref()
const uploadedFiles = ref<File[]>([])
const importing = ref(false)
const importProgress = ref(0)
const importStatus = ref<'success' | 'exception' | undefined>(undefined)
const currentStep = ref('')
const processedCount = ref(0)
const totalCount = ref(0)
const successCount = ref(0)
const failureCount = ref(0)

// 表单数据
const textImportForm = ref({
  format: 'standard',
  content: '',
  options: ['auto_detect_type', 'auto_set_difficulty']
})

const databaseForm = ref({
  sourceKnowledgeBaseId: 0,
  typeFilter: [] as string[],
  difficultyFilter: [] as string[],
  searchKeyword: ''
})

// 数据
const templates = ref([
  {
    type: 'excel',
    title: 'Excel 模板',
    description: '适用于大量题目的批量导入',
    icon: 'DocumentCopy',
    color: '#67c23a'
  },
  {
    type: 'word',
    title: 'Word 模板',
    description: '适用于包含图片的题目导入',
    icon: 'DocumentCopy',
    color: '#409eff'
  },
  {
    type: 'text',
    title: '文本模板',
    description: '简单的纯文本格式模板',
    icon: 'DocumentCopy',
    color: '#909399'
  }
])

const availableKnowledgeBases = ref<any[]>([])
const databaseQuestions = ref<any[]>([])
const selectedQuestions = ref<number[]>([])
const importLogs = ref<any[]>([])
const importResult = ref<any>(null)

// 计算属性
const canStartImport = computed(() => {
  switch (importMethod.value) {
    case 'file':
      return uploadedFiles.value.length > 0
    case 'text':
      return textImportForm.value.content.trim().length > 0
    case 'database':
      return selectedQuestions.value.length > 0
    default:
      return false
  }
})

const selectAll = computed({
  get: () => selectedQuestions.value.length === databaseQuestions.value.length && databaseQuestions.value.length > 0,
  set: (value) => {
    if (value) {
      selectedQuestions.value = databaseQuestions.value.map(q => q.id)
    } else {
      selectedQuestions.value = []
    }
  }
})

const isIndeterminate = computed(() => {
  return selectedQuestions.value.length > 0 && selectedQuestions.value.length < databaseQuestions.value.length
})

// 生命周期
onMounted(() => {
  loadAvailableKnowledgeBases()
})

// 方法
const loadAvailableKnowledgeBases = async () => {
  try {
    // TODO: 从API加载可用的知识库
    availableKnowledgeBases.value = [
      { id: 1, name: '数学基础题库', questionCount: 150 },
      { id: 2, name: '化学实验题库', questionCount: 80 },
      { id: 3, name: '物理概念题库', questionCount: 120 }
    ]
  } catch (error) {
    console.error('Load knowledge bases failed:', error)
  }
}

const handleFileChange = (file: any) => {
  uploadedFiles.value = [file.raw]
}

const beforeUpload = () => {
  return false // 阻止自动上传
}

const removeFile = (file: File) => {
  const index = uploadedFiles.value.indexOf(file)
  if (index > -1) {
    uploadedFiles.value.splice(index, 1)
  }
}

const downloadTemplate = () => {
  ElMessage.info('下载模板功能开发中...')
}

const downloadTemplateFile = (type: string) => {
  ElMessage.info(`下载${type}模板功能开发中...`)
}

const loadDatabaseQuestions = async () => {
  if (!databaseForm.value.sourceKnowledgeBaseId) return
  
  try {
    // TODO: 从API加载题库题目
    databaseQuestions.value = [
      {
        id: 1,
        type: 'choice',
        difficulty: 'MEDIUM',
        content: '函数f(x) = x² + 1的定义域是？',
        score: 5
      },
      {
        id: 2,
        type: 'blank',
        difficulty: 'EASY',
        content: '函数y = 2x + 3在x = 1处的函数值是____。',
        score: 3
      },
      {
        id: 3,
        type: 'subjective',
        difficulty: 'HARD',
        content: '证明函数f(x) = x³在实数范围内单调递增。',
        score: 10
      }
    ]
    selectedQuestions.value = []
  } catch (error) {
    console.error('Load database questions failed:', error)
  }
}

const handleSelectAll = (value: any) => {
  selectAll.value = Boolean(value)
}

const startImport = async () => {
  try {
    importing.value = true
    importProgress.value = 0
    importStatus.value = undefined
    importLogs.value = []
    importResult.value = null
    
    currentStep.value = '准备导入...'
    addLog('开始导入题目')
    
    await simulateImport()
    
    importStatus.value = 'success'
    ElMessage.success('题目导入完成')
  } catch (error) {
    importStatus.value = 'exception'
    ElMessage.error('导入失败，请重试')
    console.error('Import failed:', error)
  } finally {
    importing.value = false
  }
}

const simulateImport = async () => {
  const steps = [
    '解析文件内容...',
    '验证题目格式...',
    '检查重复题目...',
    '保存到数据库...',
    '完成导入'
  ]
  
  switch (importMethod.value) {
    case 'file':
      totalCount.value = uploadedFiles.value.length * 10 // 模拟文件中的题目数
      break
    case 'text':
      totalCount.value = textImportForm.value.content.split('\n\n').length
      break
    case 'database':
      totalCount.value = selectedQuestions.value.length
      break
    default:
      totalCount.value = 0
  }
  
  for (let i = 0; i < steps.length; i++) {
    currentStep.value = steps[i]
    addLog(steps[i])
    
    await new Promise(resolve => setTimeout(resolve, 1000))
    importProgress.value = ((i + 1) / steps.length) * 100
    processedCount.value = Math.min(totalCount.value, Math.round(((i + 1) / steps.length) * totalCount.value))
  }
  
  // 生成模拟结果
  successCount.value = Math.floor(totalCount.value * 0.9)
  failureCount.value = totalCount.value - successCount.value
  
  importResult.value = {
    total: totalCount.value,
    success: successCount.value,
    failed: failureCount.value,
    duplicates: Math.floor(totalCount.value * 0.1),
    errors: failureCount.value > 0 ? [
      { id: 1, line: 5, message: '选项格式不正确' },
      { id: 2, line: 12, message: '缺少正确答案' }
    ] : []
  }
  
  addLog(`导入完成：成功${successCount.value}题，失败${failureCount.value}题`)
}

const addLog = (message: string, type: 'info' | 'error' | 'warning' = 'info') => {
  importLogs.value.push({
    id: Date.now(),
    timestamp: new Date(),
    message,
    type
  })
  
  // 限制日志数量
  if (importLogs.value.length > 20) {
    importLogs.value = importLogs.value.slice(-10)
  }
}

const confirmImport = () => {
  emit('imported')
  visible.value = false
  ElMessage.success('题目导入确认完成')
}

const resetForm = () => {
  importMethod.value = 'file'
  uploadedFiles.value = []
  textImportForm.value = {
    format: 'standard',
    content: '',
    options: ['auto_detect_type', 'auto_set_difficulty']
  }
  databaseForm.value = {
    sourceKnowledgeBaseId: 0,
    typeFilter: [],
    difficultyFilter: [],
    searchKeyword: ''
  }
  selectedQuestions.value = []
  importLogs.value = []
  importResult.value = null
  importing.value = false
  importProgress.value = 0
}

// 工具方法
const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatTime = (date: Date) => {
  return date.toLocaleTimeString()
}

const getTypeTagType = (type: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    'choice': 'primary',
    'blank': 'success',
    'subjective': 'warning',
    'calculation': 'info'
  }
  return typeMap[type] || 'info'
}

const getTypeText = (type: string) => {
  const textMap: Record<string, string> = {
    'choice': '选择题',
    'blank': '填空题',
    'subjective': '主观题',
    'calculation': '计算题'
  }
  return textMap[type] || type
}

const getDifficultyTagType = (difficulty: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    'EASY': 'success',
    'MEDIUM': 'warning',
    'HARD': 'danger'
  }
  return typeMap[difficulty] || 'info'
}
</script>

<style scoped>
.import-question-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.import-method-card,
.upload-card,
.template-card,
.text-import-card,
.database-card,
.progress-card,
.result-card {
  border: 1px solid #e4e7ed;
}

.method-options {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.method-option {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  transition: all 0.3s;
}

.method-option:hover {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.method-option.is-checked {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.method-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.method-text {
  flex: 1;
}

.method-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.method-desc {
  font-size: 12px;
  color: #909399;
}

.uploaded-files {
  margin-top: 16px;
}

.uploaded-files h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #606266;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fafafa;
  margin-bottom: 8px;
}

.file-name {
  flex: 1;
  color: #303133;
}

.file-size {
  font-size: 12px;
  color: #909399;
}

.template-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.template-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fafafa;
}

.template-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.template-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.template-title {
  font-weight: 600;
  color: #303133;
}

.template-desc {
  font-size: 14px;
  color: #606266;
}

.question-list {
  margin-top: 16px;
}

.list-header {
  padding: 12px;
  border-bottom: 1px solid #e4e7ed;
  background: #f5f7fa;
}

.question-items {
  max-height: 400px;
  overflow-y: auto;
}

.question-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.question-item:hover {
  background-color: #f5f7fa;
}

.question-checkbox {
  margin-top: 4px;
}

.question-content {
  flex: 1;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.question-score {
  font-size: 12px;
  color: #909399;
}

.question-text {
  color: #606266;
  line-height: 1.6;
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

.import-logs {
  margin-top: 16px;
}

.import-logs h4 {
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

.log-item.log-error {
  color: #f56c6c;
}

.log-item.log-warning {
  color: #e6a23c;
}

.log-time {
  color: #909399;
  min-width: 80px;
}

.log-message {
  color: inherit;
}

.result-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.result-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 6px;
  background: #f5f7fa;
}

.summary-label {
  font-size: 14px;
  color: #606266;
}

.summary-value {
  font-weight: 600;
  font-size: 16px;
}

.summary-value.success {
  color: #67c23a;
}

.summary-value.error {
  color: #f56c6c;
}

.summary-value.warning {
  color: #e6a23c;
}

.error-details h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #606266;
}

.error-item {
  display: flex;
  gap: 12px;
  padding: 8px 12px;
  border-left: 3px solid #f56c6c;
  background: #fef0f0;
  margin-bottom: 8px;
  border-radius: 4px;
}

.error-line {
  font-weight: 600;
  color: #f56c6c;
  min-width: 80px;
}

.error-message {
  color: #606266;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
