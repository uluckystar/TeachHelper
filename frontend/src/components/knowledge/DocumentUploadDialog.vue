<template>
  <el-dialog
    v-model="visible"
    title="上传文档"
    width="600px"
    :close-on-click-modal="false"
    @closed="resetForm"
  >
    <div class="upload-dialog">
      <!-- 拖拽上传区域 -->
      <el-upload
        ref="uploadRef"
        class="upload-dragger"
        drag
        :action="uploadAction"
        :headers="uploadHeaders"
        :data="uploadData"
        :multiple="true"
        :auto-upload="false"
        :on-change="handleFileChange"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :on-progress="handleUploadProgress"
        :before-upload="beforeUpload"
        :accept="acceptedFileTypes"
        :limit="maxFileCount"
        :on-exceed="handleExceed"
      >
        <el-icon class="el-icon--upload">
          <UploadFilled />
        </el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 PDF、Word、PowerPoint、文本、图片等格式，单个文件不超过 {{ maxFileSize }}MB，最多 {{ maxFileCount }} 个文件
          </div>
        </template>
      </el-upload>

      <!-- 文件列表 -->
      <div class="file-list" v-if="fileList.length > 0">
        <h4>待上传文件 ({{ fileList.length }})</h4>
        <div class="file-items">
          <div 
            v-for="(file, index) in fileList" 
            :key="file.uid"
            class="file-item"
            :class="{ 'file-error': file.status === 'fail' }"
          >
            <div class="file-info">
              <el-icon class="file-icon">
                <Document v-if="isPDF(file)" />
                <DocumentCopy v-else-if="isWord(file)" />
                <Picture v-else-if="isImage(file)" />
                <Files v-else />
              </el-icon>
              <div class="file-details">
                <div class="file-name">{{ file.name }}</div>
                <div class="file-meta">
                  {{ formatFileSize(file.size || 0) }}
                  <span v-if="file.status === 'fail'" class="error-message">
                    - 上传失败
                  </span>
                </div>
              </div>
            </div>
            
            <div class="file-status">
              <el-progress 
                v-if="file.status === 'uploading'"
                :percentage="file.percentage || 0"
                :stroke-width="4"
                size="small"
              />
              <el-tag v-else-if="file.status === 'success'" type="success" size="small">
                已上传
              </el-tag>
              <el-tag v-else-if="file.status === 'fail'" type="danger" size="small">
                失败
              </el-tag>
              <el-button 
                v-else
                size="small" 
                icon="Close" 
                circle
                @click="removeFile(index)"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- 上传选项 -->
      <div class="upload-options" v-if="fileList.length > 0">
        <el-form :model="uploadOptions" label-width="120px">
          <el-form-item label="自动处理">
            <el-switch 
              v-model="uploadOptions.autoProcess"
              active-text="上传后自动进行AI处理"
              inactive-text="仅上传，稍后手动处理"
            />
          </el-form-item>
          
          <el-form-item label="处理优先级" v-if="uploadOptions.autoProcess">
            <el-radio-group v-model="uploadOptions.processingPriority">
              <el-radio value="low">低 (较慢，但不影响其他任务)</el-radio>
              <el-radio value="normal">正常</el-radio>
              <el-radio value="high">高 (快速处理，优先级最高)</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="提取选项" v-if="uploadOptions.autoProcess">
            <el-checkbox-group v-model="uploadOptions.extractionOptions">
              <el-checkbox value="knowledge_points">知识点</el-checkbox>
              <el-checkbox value="key_concepts">核心概念</el-checkbox>
              <el-checkbox value="examples">例题与案例</el-checkbox>
              <el-checkbox value="formulas">公式定理</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
        </el-form>
      </div>

      <!-- 上传进度 -->
      <div class="upload-progress" v-if="uploading">
        <el-progress 
          :percentage="overallProgress"
          :stroke-width="6"
          :text-inside="true"
          status="success"
        />
        <div class="progress-text">
          正在上传... {{ completedCount }}/{{ fileList.length }} 个文件
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false" :disabled="uploading">
          取消
        </el-button>
        <el-button 
          type="primary" 
          @click="startUpload"
          :loading="uploading"
          :disabled="fileList.length === 0"
        >
          {{ uploading ? '上传中...' : `开始上传 (${fileList.length})` }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  UploadFilled,
  Document,
  DocumentCopy,
  Picture,
  Files,
  Close
} from '@element-plus/icons-vue'
import type { UploadFile, UploadFiles, UploadProgressEvent } from 'element-plus'

// Props
const props = defineProps<{
  modelValue: boolean
  knowledgeBaseId: number
}>()

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  uploaded: []
}>()

// 响应式数据
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const uploadRef = ref()
const fileList = ref<UploadFile[]>([])
const uploading = ref(false)
const uploadOptions = ref({
  autoProcess: true,
  processingPriority: 'normal',
  extractionOptions: ['knowledge_points', 'key_concepts']
})

// 上传配置
const maxFileSize = 50 // MB
const maxFileCount = 10
const acceptedFileTypes = '.pdf,.doc,.docx,.ppt,.pptx,.txt,.md,.jpg,.jpeg,.png,.gif'

const uploadAction = computed(() => {
  return `/api/knowledge/documents/upload`
})

const uploadHeaders = computed(() => {
  // TODO: 添加认证头
  return {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
})

const uploadData = computed(() => {
  return {
    knowledgeBaseId: props.knowledgeBaseId,
    autoProcess: uploadOptions.value.autoProcess,
    processingPriority: uploadOptions.value.processingPriority,
    extractionOptions: uploadOptions.value.extractionOptions.join(',')
  }
})

// 上传进度计算
const completedCount = computed(() => {
  return fileList.value.filter(file => 
    file.status === 'success' || file.status === 'fail'
  ).length
})

const overallProgress = computed(() => {
  if (fileList.value.length === 0) return 0
  
  const totalPercentage = fileList.value.reduce((sum, file) => {
    return sum + (file.percentage || 0)
  }, 0)
  
  return Math.round(totalPercentage / fileList.value.length)
})

// 方法
const handleFileChange = (file: UploadFile, files: UploadFiles) => {
  fileList.value = files
}

const beforeUpload = (file: File) => {
  // 检查文件大小
  if (file.size > maxFileSize * 1024 * 1024) {
    ElMessage.error(`文件大小不能超过 ${maxFileSize}MB`)
    return false
  }

  // 检查文件类型
  const isValidType = checkFileType(file)
  if (!isValidType) {
    ElMessage.error('不支持的文件类型')
    return false
  }

  return true
}

const checkFileType = (file: File) => {
  const validTypes = [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-powerpoint',
    'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    'text/plain',
    'text/markdown',
    'image/jpeg',
    'image/jpg',
    'image/png',
    'image/gif'
  ]
  
  return validTypes.includes(file.type)
}

const handleExceed = () => {
  ElMessage.warning(`最多只能上传 ${maxFileCount} 个文件`)
}

const startUpload = () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请先选择要上传的文件')
    return
  }

  uploading.value = true
  uploadRef.value.submit()
}

const handleUploadProgress = (event: UploadProgressEvent, file: UploadFile) => {
  file.percentage = Math.round(event.percent || 0)
}

const handleUploadSuccess = (response: any, file: UploadFile) => {
  file.status = 'success'
  console.log('Upload success:', response)
  ElMessage.success(`${file.name} 上传成功`)
  
  // 检查是否所有文件都上传完成
  checkUploadComplete()
}

const handleUploadError = (error: any, file: UploadFile) => {
  file.status = 'fail'
  ElMessage.error(`${file.name} 上传失败: ${error.message || '网络错误'}`)
  
  // 检查是否所有文件都处理完成
  checkUploadComplete()
}

const checkUploadComplete = () => {
  const pendingFiles = fileList.value.filter(file => 
    file.status !== 'success' && file.status !== 'fail'
  )
  
  if (pendingFiles.length === 0) {
    uploading.value = false
    
    const successCount = fileList.value.filter(file => file.status === 'success').length
    const errorCount = fileList.value.filter(file => file.status === 'fail').length
    
    if (successCount > 0) {
      ElMessage.success(`成功上传 ${successCount} 个文件`)
      emit('uploaded')
      
      // 如果启用了自动处理，显示处理进度提示
      if (uploadOptions.value.autoProcess) {
        ElMessage.info('文档上传完成，正在进行AI处理...')
      }
    }
    
    if (errorCount > 0) {
      ElMessage.warning(`${errorCount} 个文件上传失败`)
    }
    
    // 如果全部成功，关闭对话框
    if (errorCount === 0 && successCount > 0) {
      setTimeout(() => {
        visible.value = false
      }, 1500)
    }
  }
}

const removeFile = (index: number) => {
  fileList.value.splice(index, 1)
  uploadRef.value.clearFiles()
  
  // 重新添加剩余文件
  setTimeout(() => {
    fileList.value.forEach(file => {
      if (file.raw) {
        uploadRef.value.handleStart(file.raw)
      }
    })
  })
}

const resetForm = () => {
  fileList.value = []
  uploading.value = false
  uploadOptions.value = {
    autoProcess: true,
    processingPriority: 'normal',
    extractionOptions: ['knowledge_points', 'key_concepts']
  }
  
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}

// 文件类型判断
const isPDF = (file: UploadFile) => {
  return file.raw?.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf')
}

const isWord = (file: UploadFile) => {
  const wordTypes = [
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  ]
  return wordTypes.includes(file.raw?.type || '') || 
         /\.(doc|docx)$/i.test(file.name)
}

const isImage = (file: UploadFile) => {
  return file.raw?.type?.startsWith('image/') || 
         /\.(jpg|jpeg|png|gif)$/i.test(file.name)
}

// 工具方法
const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}
</script>

<style scoped>
.upload-dialog {
  max-height: 70vh;
  overflow-y: auto;
}

.upload-dragger {
  width: 100%;
}

.upload-dragger .el-upload-dragger {
  width: 100%;
  height: 180px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  text-align: center;
  background-color: #fafbfc;
  transition: all 0.3s;
}

.upload-dragger .el-upload-dragger:hover {
  border-color: #409eff;
  background-color: #f5f9ff;
}

.upload-dragger .el-icon--upload {
  margin: 40px 0 16px;
  color: #c0c4cc;
  font-size: 48px;
}

.upload-dragger .el-upload__text {
  color: #606266;
  font-size: 14px;
  margin: 0;
}

.upload-dragger .el-upload__text em {
  color: #409eff;
  font-style: normal;
}

.upload-dragger .el-upload__tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
  line-height: 1.4;
}

/* 文件列表 */
.file-list {
  margin-top: 24px;
}

.file-list h4 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 14px;
  font-weight: 500;
}

.file-items {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid #f5f7fa;
  transition: background-color 0.2s;
}

.file-item:last-child {
  border-bottom: none;
}

.file-item:hover {
  background-color: #f5f7fa;
}

.file-item.file-error {
  background-color: #fef0f0;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.file-icon {
  color: #409eff;
  font-size: 18px;
}

.file-details {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 13px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-meta {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.error-message {
  color: #f56c6c;
}

.file-status {
  flex-shrink: 0;
  width: 80px;
  text-align: right;
}

/* 上传选项 */
.upload-options {
  margin-top: 24px;
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 6px;
}

.upload-options .el-form-item {
  margin-bottom: 16px;
}

.upload-options .el-form-item:last-child {
  margin-bottom: 0;
}

.upload-options .el-checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* 上传进度 */
.upload-progress {
  margin-top: 20px;
  padding: 16px;
  background-color: #f0f9ff;
  border-radius: 6px;
  text-align: center;
}

.progress-text {
  margin-top: 8px;
  font-size: 13px;
  color: #409eff;
}

/* 对话框底部 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .upload-dragger .el-upload-dragger {
    height: 120px;
  }
  
  .upload-dragger .el-icon--upload {
    margin: 20px 0 12px;
    font-size: 32px;
  }
  
  .file-items {
    max-height: 150px;
  }
  
  .upload-options .el-checkbox-group {
    flex-direction: row;
    flex-wrap: wrap;
  }
}
</style>
