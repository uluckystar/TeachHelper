<template>
  <div class="knowledge-upload-view">
    <div class="page-header">
      <el-breadcrumb>
        <el-breadcrumb-item :to="{ path: '/knowledge' }">知识库管理</el-breadcrumb-item>
        <el-breadcrumb-item>文档上传</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <el-row :gutter="24">
      <!-- 上传区域 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <el-icon class="header-icon"><Upload /></el-icon>
              <span>文档上传</span>
            </div>
          </template>

          <el-upload
            ref="uploadRef"
            class="upload-demo"
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
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 PDF、DOC、DOCX、TXT 格式，单个文件不超过 10MB
              </div>
            </template>
          </el-upload>

          <div v-if="uploading" class="upload-progress">
            <el-progress :percentage="uploadProgress" />
            <p>正在上传文档，请稍候...</p>
          </div>

          <!-- 文档处理进度 -->
          <div v-if="showProcessingProgress" class="processing-progress-section">
            <DocumentProcessingProgress 
              :knowledge-base-id="selectedKnowledgeBaseId"
              :auto-refresh="true"
              @processing-completed="handleProcessingCompleted"
            />
          </div>
        </el-card>

        <!-- 批量配置 -->
        <el-card class="batch-config">
          <template #header>
            <div class="card-header">
              <el-icon class="header-icon"><Setting /></el-icon>
              <span>批量配置</span>
            </div>
          </template>

          <el-form :model="batchConfig" label-width="120px">
            <el-form-item label="知识库分类">
              <el-select v-model="batchConfig.category" placeholder="请选择分类" style="width: 100%">
                <el-option label="数学" value="math" />
                <el-option label="物理" value="physics" />
                <el-option label="化学" value="chemistry" />
                <el-option label="计算机" value="computer" />
                <el-option label="其他" value="other" />
              </el-select>
            </el-form-item>

            <el-form-item label="标签">
              <el-tag
                v-for="tag in batchConfig.tags"
                :key="tag"
                closable
                @close="removeTag(tag)"
                style="margin-right: 8px;"
              >
                {{ tag }}
              </el-tag>
              <el-input
                v-if="tagInputVisible"
                ref="tagInputRef"
                v-model="newTag"
                size="small"
                style="width: 100px;"
                @keyup.enter="addTag"
                @blur="addTag"
              />
              <el-button v-else size="small" @click="showTagInput">+ 添加标签</el-button>
            </el-form-item>

            <el-form-item label="描述">
              <el-input
                v-model="batchConfig.description"
                type="textarea"
                :rows="3"
                placeholder="为这批文档添加描述（可选）"
              />
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 上传状态 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <el-icon class="header-icon"><Document /></el-icon>
              <span>上传状态</span>
            </div>
          </template>

          <div v-if="!uploadedFiles.length" class="empty-state">
            <el-empty description="暂无上传文件" />
          </div>

          <div v-else class="file-list">
            <div
              v-for="file in uploadedFiles"
              :key="file.id"
              class="file-item"
              :class="{ 'success': file.status === 'success', 'error': file.status === 'error' }"
            >
              <div class="file-info">
                <div class="file-name">{{ file.name }}</div>
                <div class="file-meta">
                  <span>{{ formatFileSize(file.size) }}</span>
                  <el-tag :type="getStatusType(file.status)" size="small">
                    {{ getStatusText(file.status) }}
                  </el-tag>
                </div>
              </div>
              <div class="file-actions">
                <el-button
                  v-if="file.status === 'error'"
                  link
                  size="small"
                  @click="retryUpload(file)"
                >
                  重试
                </el-button>
                <el-button
                  link
                  size="small"
                  @click="removeFile(file.id)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>

          <div v-if="uploadedFiles.length" class="upload-summary">
            <el-divider />
            <el-statistic
              title="上传成功"
              :value="successCount"
              suffix="个文件"
            />
            <el-statistic
              title="处理失败"
              :value="errorCount"
              suffix="个文件"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Upload, Setting, Document, UploadFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import type { UploadInstance, UploadProps, UploadUserFile } from 'element-plus'
import DocumentProcessingProgress from '@/components/knowledge/DocumentProcessingProgress.vue'

const router = useRouter()
const authStore = useAuthStore()

// 响应式数据
const uploadRef = ref<UploadInstance>()
const tagInputRef = ref()
const uploading = ref(false)
const uploadProgress = ref(0)
const tagInputVisible = ref(false)
const newTag = ref('')
const showProcessingProgress = ref(false)
const selectedKnowledgeBaseId = ref<number | undefined>(undefined)

const fileList = ref<UploadUserFile[]>([])
const uploadedFiles = ref<any[]>([])

const batchConfig = reactive({
  category: '',
  tags: [] as string[],
  description: ''
})

// 计算属性
const uploadUrl = computed(() => '/api/knowledge/upload')
const uploadHeaders = computed(() => ({
  'Authorization': `Bearer ${authStore.token}`
}))
const uploadData = computed(() => ({
  category: batchConfig.category,
  tags: batchConfig.tags.join(','),
  description: batchConfig.description
}))

const successCount = computed(() => 
  uploadedFiles.value.filter(f => f.status === 'success').length
)
const errorCount = computed(() => 
  uploadedFiles.value.filter(f => f.status === 'error').length
)

// 方法
const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isValidType = ['application/pdf', 'application/msword', 
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'text/plain'].includes(file.type)
  
  if (!isValidType) {
    ElMessage.error('只能上传 PDF、DOC、DOCX、TXT 格式文件')
    return false
  }

  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB')
    return false
  }

  // 添加到上传列表
  uploadedFiles.value.push({
    id: Date.now() + Math.random(),
    name: file.name,
    size: file.size,
    status: 'uploading'
  })

  return true
}

const handleUploadProgress = (evt: any) => {
  uploadProgress.value = Math.round(evt.percent)
  uploading.value = evt.percent < 100
}

const handleUploadSuccess = (response: any, file: any) => {
  const uploadedFile = uploadedFiles.value.find(f => f.name === file.name)
  if (uploadedFile) {
    uploadedFile.status = 'success'
    uploadedFile.id = response.data?.id || uploadedFile.id
  }
  
  ElMessage.success(`${file.name} 上传成功`)
  uploading.value = false
  
  // 如果上传成功，显示处理进度
  if (response.data?.knowledgeBaseId) {
    selectedKnowledgeBaseId.value = response.data.knowledgeBaseId
    showProcessingProgress.value = true
  }
}

const handleUploadError = (error: any, file: any) => {
  const uploadedFile = uploadedFiles.value.find(f => f.name === file.name)
  if (uploadedFile) {
    uploadedFile.status = 'error'
    uploadedFile.error = error.message
  }
  
  ElMessage.error(`${file.name} 上传失败`)
  uploading.value = false
}

const retryUpload = (file: any) => {
  // 重新上传逻辑
  ElMessage.info('重新上传功能开发中...')
}

const removeFile = (fileId: string) => {
  const index = uploadedFiles.value.findIndex(f => f.id === fileId)
  if (index > -1) {
    uploadedFiles.value.splice(index, 1)
  }
}

const addTag = () => {
  if (newTag.value && !batchConfig.tags.includes(newTag.value)) {
    batchConfig.tags.push(newTag.value)
    newTag.value = ''
  }
  tagInputVisible.value = false
}

const removeTag = (tag: string) => {
  const index = batchConfig.tags.indexOf(tag)
  if (index > -1) {
    batchConfig.tags.splice(index, 1)
  }
}

const showTagInput = () => {
  tagInputVisible.value = true
  nextTick(() => {
    tagInputRef.value?.focus()
  })
}

const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const getStatusType = (status: string) => {
  switch (status) {
    case 'success': return 'success'
    case 'error': return 'danger'
    case 'uploading': return 'warning'
    default: return 'info'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'success': return '成功'
    case 'error': return '失败'
    case 'uploading': return '上传中'
    default: return '未知'
  }
}

// 文档处理进度相关方法
const handleProcessingCompleted = (result: any) => {
  ElMessage.success('文档处理完成！')
  showProcessingProgress.value = false
  
  // 可以在这里处理完成后的逻辑，比如刷新文档列表
  console.log('处理结果:', result)
}
</script>

<style scoped>
.knowledge-upload-view {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.header-icon {
  color: #409eff;
  font-size: 18px;
}

.batch-config {
  margin-top: 24px;
}

.upload-demo {
  margin-bottom: 20px;
}

.upload-progress {
  margin-top: 20px;
  text-align: center;
}

.upload-progress p {
  margin-top: 8px;
  color: #606266;
}

.empty-state {
  padding: 40px 20px;
  text-align: center;
}

.file-list {
  max-height: 400px;
  overflow-y: auto;
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 8px;
  transition: all 0.3s;
}

.file-item.success {
  border-color: #67c23a;
  background-color: #f0f9ff;
}

.file-item.error {
  border-color: #f56c6c;
  background-color: #fef0f0;
}

.file-info {
  flex: 1;
}

.file-name {
  font-weight: 500;
  margin-bottom: 4px;
}

.file-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #909399;
}

.file-actions {
  display: flex;
  gap: 8px;
}

.upload-summary {
  padding-top: 16px;
}

.upload-summary .el-statistic {
  margin-bottom: 16px;
}

.processing-progress-section {
  margin-top: 24px;
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .knowledge-upload-view {
    padding: 16px;
  }
  
  .el-row {
    flex-direction: column;
  }
  
  .el-col {
    width: 100% !important;
    margin-bottom: 16px;
  }
}
</style>
