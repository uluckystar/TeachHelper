<template>
  <div class="document-upload">
    <div class="page-header">
      <h1>文档上传与知识库管理</h1>
      <p>支持多种格式文档上传、OCR识别和智能分类</p>
    </div>

    <el-row :gutter="24">
      <!-- 上传区域 -->
      <el-col :span="16">
        <el-card class="upload-card">
          <template #header>
            <div class="card-header">
              <el-icon><Upload /></el-icon>
              <span>文档上传</span>
              <el-button 
                type="primary" 
                size="small" 
                @click="uploadVisible = true"
                style="margin-left: auto;"
              >
                批量上传
              </el-button>
            </div>
          </template>

          <!-- 拖拽上传区域 -->
          <el-upload
            ref="uploadRef"
            class="upload-dragger"
            drag
            :action="uploadUrl"
            :headers="uploadHeaders"
            :data="uploadData"
            :before-upload="beforeUpload"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :on-progress="handleUploadProgress"
            :file-list="fileList"
            multiple
            :accept="acceptedFileTypes"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持格式：PDF、Word、PowerPoint、TXT、图片(JPG/PNG)等
                <br>
                单个文件不超过50MB，支持OCR文字识别
              </div>
            </template>
          </el-upload>

          <!-- 上传进度 -->
          <div v-if="uploadProgress.length > 0" class="upload-progress">
            <h3>上传进度</h3>
            <div 
              v-for="(progress, index) in uploadProgress" 
              :key="index"
              class="progress-item"
            >
              <div class="progress-info">
                <span class="filename">{{ progress.filename }}</span>
                <span class="status" :class="progress.status">{{ getStatusText(progress.status) }}</span>
              </div>
              <el-progress 
                :percentage="progress.percentage" 
                :status="progress.status"
                :show-text="false"
              />
            </div>
          </div>
        </el-card>

        <!-- 文档处理选项 -->
        <el-card class="processing-options" style="margin-top: 20px;">
          <template #header>
            <div class="card-header">
              <el-icon><Setting /></el-icon>
              <span>处理选项</span>
            </div>
          </template>

          <el-form :model="processingOptions" label-width="120px">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="OCR识别">
                  <el-switch v-model="processingOptions.enableOCR" />
                  <span class="form-tip">对图片和扫描文档进行文字识别</span>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="智能分段">
                  <el-switch v-model="processingOptions.enableSegmentation" />
                  <span class="form-tip">自动将文档分段并提取知识点</span>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="向量化存储">
                  <el-switch v-model="processingOptions.enableVectorization" />
                  <span class="form-tip">生成向量表示用于相似度检索</span>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="自动分类">
                  <el-switch v-model="processingOptions.enableAutoClassification" />
                  <span class="form-tip">基于内容自动分类到知识点</span>
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="默认科目">
              <el-select v-model="processingOptions.defaultSubject" placeholder="请选择科目">
                <el-option 
                  v-for="subject in subjects" 
                  :key="subject.value" 
                  :label="subject.label" 
                  :value="subject.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="默认年级">
              <el-select v-model="processingOptions.defaultGrade" placeholder="请选择年级">
                <el-option 
                  v-for="grade in grades" 
                  :key="grade.value" 
                  :label="grade.label" 
                  :value="grade.value"
                />
              </el-select>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 侧边栏信息 -->
      <el-col :span="8">
        <!-- 统计信息 -->
        <el-card class="stats-card">
          <template #header>
            <div class="card-header">
              <el-icon><DataBoard /></el-icon>
              <span>知识库统计</span>
            </div>
          </template>

          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-number">{{ stats.totalDocuments }}</div>
              <div class="stat-label">总文档数</div>
            </div>
            <div class="stat-item">
              <div class="stat-number">{{ stats.totalSize }}</div>
              <div class="stat-label">总大小</div>
            </div>
            <div class="stat-item">
              <div class="stat-number">{{ stats.totalKnowledgePoints }}</div>
              <div class="stat-label">知识点数</div>
            </div>
            <div class="stat-item">
              <div class="stat-number">{{ stats.vectorizedCount }}</div>
              <div class="stat-label">已向量化</div>
            </div>
          </div>
        </el-card>

        <!-- 最近上传 -->
        <el-card class="recent-uploads" style="margin-top: 20px;">
          <template #header>
            <div class="card-header">
              <el-icon><Clock /></el-icon>
              <span>最近上传</span>
            </div>
          </template>

          <div class="recent-list">
            <div 
              v-for="doc in recentDocuments" 
              :key="doc.id"
              class="recent-item"
            >
              <div class="doc-info">
                <el-icon class="doc-icon" :class="getFileTypeIcon(doc.type)">
                  <Document />
                </el-icon>
                <div class="doc-details">
                  <div class="doc-name">{{ doc.name }}</div>
                  <div class="doc-meta">
                    {{ doc.size }} • {{ formatTime(doc.uploadTime) }}
                  </div>
                </div>
              </div>
              <el-tag 
                :type="getStatusTagType(doc.status)" 
                size="small"
              >
                {{ doc.status }}
              </el-tag>
            </div>
          </div>
        </el-card>

        <!-- 支持的文件格式 -->
        <el-card class="supported-formats" style="margin-top: 20px;">
          <template #header>
            <div class="card-header">
              <el-icon><Files /></el-icon>
              <span>支持格式</span>
            </div>
          </template>

          <div class="format-list">
            <el-tag 
              v-for="format in supportedFormats" 
              :key="format.type"
              :type="format.color"
              class="format-tag"
            >
              {{ format.type }}
            </el-tag>
          </div>

          <div class="format-features">
            <h4>特色功能</h4>
            <ul>
              <li><el-icon><Check /></el-icon> OCR文字识别</li>
              <li><el-icon><Check /></el-icon> 智能内容分析</li>
              <li><el-icon><Check /></el-icon> 向量化存储</li>
              <li><el-icon><Check /></el-icon> 知识点提取</li>
              <li><el-icon><Check /></el-icon> 自动分类</li>
            </ul>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 批量上传对话框 -->
    <BatchUploadDialog 
      v-model="uploadVisible"
      :knowledge-bases="knowledgeBases"
      @upload-success="handleBatchUploadComplete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Upload, 
  UploadFilled, 
  Setting, 
  DataBoard, 
  Clock, 
  Document, 
  Files, 
  Check 
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { knowledgeBaseApi } from '@/api/knowledge'
import BatchUploadDialog from './components/BatchUploadDialog.vue'

// 定义类型
interface UploadProgress {
  filename: string
  percentage: number
  status: '' | 'success' | 'exception' | 'warning'
}

interface KnowledgeBase {
  id: string
  name: string
  subject?: string
  gradeLevel?: string
}

interface RecentDocument {
  id: number
  name: string
  type: string
  size: string
  status: string
  uploadTime: string
}

const authStore = useAuthStore()

// 响应式数据
const uploadRef = ref()
const uploadVisible = ref(false)
const fileList = ref([])
const uploadProgress = ref<UploadProgress[]>([])
const knowledgeBases = ref<KnowledgeBase[]>([])
const recentDocuments = ref<RecentDocument[]>([])

// 处理选项
const processingOptions = reactive({
  enableOCR: true,
  enableSegmentation: true,
  enableVectorization: true,
  enableAutoClassification: true,
  defaultSubject: '',
  defaultGrade: ''
})

// 统计数据
const stats = reactive({
  totalDocuments: 0,
  totalSize: '0 MB',
  totalKnowledgePoints: 0,
  vectorizedCount: 0
})

// 配置数据
const subjects = [
  { label: '语文', value: 'chinese' },
  { label: '数学', value: 'math' },
  { label: '英语', value: 'english' },
  { label: '物理', value: 'physics' },
  { label: '化学', value: 'chemistry' },
  { label: '生物', value: 'biology' },
  { label: '历史', value: 'history' },
  { label: '地理', value: 'geography' },
  { label: '政治', value: 'politics' }
]

const grades = [
  { label: '小学一年级', value: 'grade1' },
  { label: '小学二年级', value: 'grade2' },
  { label: '小学三年级', value: 'grade3' },
  { label: '小学四年级', value: 'grade4' },
  { label: '小学五年级', value: 'grade5' },
  { label: '小学六年级', value: 'grade6' },
  { label: '初中一年级', value: 'grade7' },
  { label: '初中二年级', value: 'grade8' },
  { label: '初中三年级', value: 'grade9' },
  { label: '高中一年级', value: 'grade10' },
  { label: '高中二年级', value: 'grade11' },
  { label: '高中三年级', value: 'grade12' }
]

const supportedFormats: Array<{ type: string; color: 'success' | 'primary' | 'warning' | 'info' | 'danger' }> = [
  { type: 'PDF', color: 'danger' },
  { type: 'DOC/DOCX', color: 'primary' },
  { type: 'PPT/PPTX', color: 'warning' },
  { type: 'TXT', color: 'info' },
  { type: 'JPG/PNG', color: 'success' },
  { type: 'RTF', color: 'primary' },
  { type: 'XLS/XLSX', color: 'success' }
]

// 计算属性
const uploadUrl = computed(() => '/api/knowledge/documents/upload')
const uploadHeaders = computed(() => ({
  'Authorization': `Bearer ${authStore.token}`
}))
const uploadData = computed(() => ({
  ...processingOptions
}))
const acceptedFileTypes = computed(() => 
  '.pdf,.doc,.docx,.ppt,.pptx,.txt,.jpg,.jpeg,.png,.gif,.bmp,.rtf,.xls,.xlsx'
)

// 方法
const beforeUpload = (file: File) => {
  const maxSize = 50 * 1024 * 1024 // 50MB
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过50MB')
    return false
  }

  // 添加到进度列表
  uploadProgress.value.push({
    filename: file.name,
    percentage: 0,
    status: ''
  })

  return true
}

const handleUploadProgress = (event: any, file: any) => {
  const progress = uploadProgress.value.find(p => p.filename === file.name)
  if (progress) {
    progress.percentage = Math.round(event.percent)
  }
}

const handleUploadSuccess = (response: any, file: any) => {
  const progress = uploadProgress.value.find(p => p.filename === file.name)
  if (progress) {
    progress.status = 'success'
    progress.percentage = 100
  }

  ElMessage.success(`${file.name} 上传成功`)
  loadRecentDocuments()
  loadStats()

  // 3秒后移除进度项
  setTimeout(() => {
    const index = uploadProgress.value.findIndex(p => p.filename === file.name)
    if (index > -1) {
      uploadProgress.value.splice(index, 1)
    }
  }, 3000)
}

const handleUploadError = (error: any, file: any) => {
  const progress = uploadProgress.value.find(p => p.filename === file.name)
  if (progress) {
    progress.status = 'exception'
  }

  ElMessage.error(`${file.name} 上传失败`)
}

const handleBatchUploadComplete = () => {
  uploadVisible.value = false
  loadRecentDocuments()
  loadStats()
}

const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'uploading': '上传中',
    'success': '成功',
    'exception': '失败'
  }
  return statusMap[status] || status
}

const getFileTypeIcon = (type: string) => {
  const iconMap: Record<string, string> = {
    'pdf': 'file-pdf',
    'doc': 'file-word',
    'docx': 'file-word',
    'ppt': 'file-powerpoint',
    'pptx': 'file-powerpoint',
    'txt': 'file-text',
    'jpg': 'file-image',
    'jpeg': 'file-image',
    'png': 'file-image'
  }
  return iconMap[type.toLowerCase()] || 'file'
}

const getStatusTagType = (status: string): 'success' | 'primary' | 'warning' | 'info' | 'danger' => {
  const tagMap: Record<string, 'success' | 'primary' | 'warning' | 'info' | 'danger'> = {
    'processing': 'warning',
    'completed': 'success',
    'failed': 'danger'
  }
  return tagMap[status] || 'info'
}

const formatTime = (time: string) => {
  return new Date(time).toLocaleString('zh-CN')
}

const loadStats = async () => {
  try {
    // 模拟统计数据，因为API还未实现
    stats.totalDocuments = 128
    stats.totalSize = '256 MB'
    stats.totalKnowledgePoints = 45
    stats.vectorizedCount = 98
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

const loadRecentDocuments = async () => {
  try {
    // 模拟最近文档数据
    recentDocuments.value = [
      {
        id: 1,
        name: '高等数学第一章.pdf',
        type: 'pdf',
        size: '2.5 MB',
        status: 'completed',
        uploadTime: new Date().toISOString()
      },
      {
        id: 2,
        name: '线性代数习题集.docx',
        type: 'docx',
        size: '1.8 MB',
        status: 'processing',
        uploadTime: new Date(Date.now() - 3600000).toISOString()
      }
    ]
  } catch (error) {
    console.error('加载最近文档失败:', error)
  }
}

const loadKnowledgeBases = async () => {
  try {
    const response = await knowledgeBaseApi.getKnowledgeBases({ page: 0, size: 100 })
    knowledgeBases.value = (response.content || []).map(kb => ({
      id: kb.id.toString(),
      name: kb.name,
      subject: kb.subject,
      gradeLevel: kb.gradeLevel
    }))
  } catch (error) {
    console.error('加载知识库列表失败:', error)
    // 提供默认知识库选项
    knowledgeBases.value = [
      { id: '1', name: '数学知识库', subject: '数学', gradeLevel: '高中' },
      { id: '2', name: '物理知识库', subject: '物理', gradeLevel: '高中' },
      { id: '3', name: '化学知识库', subject: '化学', gradeLevel: '高中' }
    ]
  }
}

// 生命周期
onMounted(() => {
  loadStats()
  loadRecentDocuments()
})
</script>

<style scoped>
.document-upload {
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  color: #303133;
}

.page-header p {
  margin: 8px 0 0 0;
  color: #606266;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.upload-dragger {
  width: 100%;
}

.upload-progress {
  margin-top: 20px;
}

.progress-item {
  margin-bottom: 12px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.filename {
  font-weight: 500;
}

.status {
  font-size: 12px;
}

.status.uploading {
  color: #409eff;
}

.status.success {
  color: #67c23a;
}

.status.exception {
  color: #f56c6c;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.stat-item {
  text-align: center;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.recent-list {
  max-height: 300px;
  overflow-y: auto;
}

.recent-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.recent-item:last-child {
  border-bottom: none;
}

.doc-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.doc-icon {
  font-size: 20px;
  color: #409eff;
}

.doc-details {
  flex: 1;
}

.doc-name {
  font-weight: 500;
  margin-bottom: 2px;
}

.doc-meta {
  font-size: 12px;
  color: #909399;
}

.format-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.format-tag {
  margin: 0;
}

.format-features h4 {
  margin: 0 0 8px 0;
  color: #303133;
}

.format-features ul {
  margin: 0;
  padding: 0;
  list-style: none;
}

.format-features li {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 4px;
  font-size: 12px;
}

.format-features li .el-icon {
  color: #67c23a;
}
</style>
