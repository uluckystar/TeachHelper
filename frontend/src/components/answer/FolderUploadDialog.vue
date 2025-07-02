<template>
  <div v-if="visible" class="folder-upload-overlay" @click="handleOverlayClick">
    <div class="folder-upload-dialog" @click.stop>
      <div class="dialog-header">
        <h3>📁 文件夹批量上传答案</h3>
        <button class="close-btn" @click="closeDialog">✕</button>
      </div>
      
      <div class="dialog-content">
        <!-- 题目选择 -->
        <div class="form-group">
          <label>选择题目：</label>
          <select v-model="selectedQuestionId" required>
            <option value="">请选择题目</option>
            <option v-for="question in questions" :key="question.id" :value="question.id">
              {{ question.title }}
            </option>
          </select>
        </div>
        
        <!-- 文件上传区域 -->
        <div 
          class="upload-area"
          :class="{ 'drag-over': isDragOver, 'has-files': selectedFiles.length > 0 }"
          @drop="handleDrop"
          @dragover.prevent="handleDragOver"
          @dragleave="handleDragLeave"
          @click="triggerFileInput"
        >
          <input 
            ref="fileInput"
            type="file"
            multiple
            webkitdirectory
            directory
            @change="handleFileSelect"
            style="display: none"
          />
          
          <div v-if="selectedFiles.length === 0" class="upload-prompt">
            <div class="upload-icon">📂</div>
            <p>点击选择文件夹或拖拽文件到此处</p>
            <p class="upload-hint">支持：Word(.doc/.docx)、PDF、图片(jpg/png)、TXT等格式</p>
          </div>
          
          <div v-else class="file-list">
            <h4>已选择 {{ selectedFiles.length }} 个文件</h4>
            <div class="file-items" v-if="!isUploading">
              <div 
                v-for="(file, index) in selectedFiles.slice(0, 10)" 
                :key="index" 
                class="file-item"
              >
                <div class="file-info">
                  <span class="file-name">{{ file.name }}</span>
                  <span class="file-size">{{ formatFileSize(file.size) }}</span>
                </div>
                <button class="remove-file" @click="removeFile(index)">✕</button>
              </div>
              <div v-if="selectedFiles.length > 10" class="more-files">
                ... 还有 {{ selectedFiles.length - 10 }} 个文件
              </div>
            </div>
          </div>
        </div>
        
        <!-- 上传进度 -->
        <div v-if="isUploading" class="upload-progress">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
          </div>
          <p>正在处理文件... {{ uploadProgress.toFixed(1) }}%</p>
        </div>
        
        <!-- 上传结果 -->
        <div v-if="uploadResult" class="upload-result">
          <div class="result-summary" :class="uploadResult.success ? 'success' : 'error'">
            <h4>{{ uploadResult.success ? '✅ 上传完成' : '❌ 上传失败' }}</h4>
            <p>{{ uploadResult.message }}</p>
          </div>
          
          <div v-if="uploadResult.details && uploadResult.details.length > 0" class="result-details">
            <h5>处理详情：</h5>
            <div class="details-list">
              <div 
                v-for="(detail, index) in uploadResult.details.slice(0, showAllDetails ? uploadResult.details.length : 5)" 
                :key="index"
                class="detail-item"
                :class="detail.includes('成功') ? 'success' : 'error'"
              >
                {{ detail }}
              </div>
              <button 
                v-if="uploadResult.details.length > 5 && !showAllDetails"
                @click="showAllDetails = true"
                class="show-more-btn"
              >
                显示全部 {{ uploadResult.details.length }} 条结果
              </button>
            </div>
          </div>
        </div>
      </div>
      
      <div class="dialog-footer">
        <button @click="clearFiles" :disabled="isUploading" class="btn-secondary">
          清空文件
        </button>
        <button @click="closeDialog" :disabled="isUploading" class="btn-secondary">
          取消
        </button>
        <button 
          @click="startUpload" 
          :disabled="!canUpload || isUploading" 
          class="btn-primary"
        >
          {{ isUploading ? '上传中...' : '开始上传' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { answerApi } from '@/api/answer'

export default {
  name: 'FolderUploadDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    questions: {
      type: Array,
      default: () => []
    }
  },
  emits: ['close', 'upload-success'],
  data() {
    return {
      selectedQuestionId: '',
      selectedFiles: [],
      isDragOver: false,
      isUploading: false,
      uploadProgress: 0,
      uploadResult: null,
      showAllDetails: false
    }
  },
  computed: {
    canUpload() {
      return this.selectedQuestionId && this.selectedFiles.length > 0
    }
  },
  methods: {
    handleOverlayClick() {
      if (!this.isUploading) {
        this.closeDialog()
      }
    },
    
    closeDialog() {
      if (this.isUploading) return
      this.$emit('close')
      this.resetDialog()
    },
    
    resetDialog() {
      this.selectedQuestionId = ''
      this.selectedFiles = []
      this.isDragOver = false
      this.isUploading = false
      this.uploadProgress = 0
      this.uploadResult = null
      this.showAllDetails = false
    },
    
    triggerFileInput() {
      if (this.isUploading) return
      this.$refs.fileInput.click()
    },
    
    handleFileSelect(event) {
      const files = Array.from(event.target.files)
      this.addFiles(files)
    },
    
    handleDrop(event) {
      event.preventDefault()
      this.isDragOver = false
      
      if (this.isUploading) return
      
      const files = Array.from(event.dataTransfer.files)
      this.addFiles(files)
    },
    
    handleDragOver() {
      this.isDragOver = true
    },
    
    handleDragLeave() {
      this.isDragOver = false
    },
    
    addFiles(files) {
      // 过滤支持的文件类型
      const supportedExtensions = ['.doc', '.docx', '.pdf', '.txt', '.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.rtf', '.odt']
      const validFiles = files.filter(file => {
        const extension = '.' + file.name.split('.').pop().toLowerCase()
        return supportedExtensions.includes(extension)
      })
      
      // 添加到选择的文件列表，避免重复
      const existingNames = this.selectedFiles.map(f => f.name)
      const newFiles = validFiles.filter(file => !existingNames.includes(file.name))
      
      this.selectedFiles.push(...newFiles)
      
      if (validFiles.length < files.length) {
        this.$message.warning(`已过滤 ${files.length - validFiles.length} 个不支持的文件类型`)
      }
    },
    
    removeFile(index) {
      this.selectedFiles.splice(index, 1)
    },
    
    clearFiles() {
      this.selectedFiles = []
      this.uploadResult = null
    },
    
    formatFileSize(bytes) {
      if (bytes === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    },
    
    async startUpload() {
      if (!this.canUpload) return
      
      this.isUploading = true
      this.uploadProgress = 0
      this.uploadResult = null
      
      try {
        // 创建FormData
        const formData = new FormData()
        this.selectedFiles.forEach(file => {
          formData.append('files', file)
        })
        formData.append('questionId', this.selectedQuestionId)
        
        // 模拟进度更新
        const progressInterval = setInterval(() => {
          if (this.uploadProgress < 90) {
            this.uploadProgress += Math.random() * 10
          }
        }, 500)
        
        // 上传文件
        const response = await answerApi.uploadFolderAnswers(formData)
        
        clearInterval(progressInterval)
        this.uploadProgress = 100
        
        // 处理响应
        this.uploadResult = response
        
        if (response.success) {
          this.$message.success('文件上传成功！')
          this.$emit('upload-success', response)
        } else {
          this.$message.error('部分文件处理失败，请查看详情')
        }
        
      } catch (error) {
        console.error('上传失败:', error)
        this.uploadResult = {
          success: false,
          message: '上传失败: ' + (error.response?.data?.message || error.message),
          details: []
        }
        this.$message.error('上传失败')
      } finally {
        this.isUploading = false
      }
    }
  }
}
</script>

<style scoped>
.folder-upload-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.folder-upload-dialog {
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  width: 90%;
  max-width: 700px;
  max-height: 90vh;
  overflow-y: auto;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
}

.dialog-header h3 {
  margin: 0;
  color: #374151;
  font-size: 18px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #6b7280;
  padding: 4px;
}

.close-btn:hover {
  color: #374151;
}

.dialog-content {
  padding: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #374151;
}

.form-group select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
}

.upload-area {
  border: 2px dashed #d1d5db;
  border-radius: 8px;
  padding: 40px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 20px;
}

.upload-area:hover {
  border-color: #3b82f6;
  background-color: #f8fafc;
}

.upload-area.drag-over {
  border-color: #3b82f6;
  background-color: #eff6ff;
}

.upload-area.has-files {
  border-style: solid;
  border-color: #10b981;
  background-color: #f0fdf4;
}

.upload-prompt .upload-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.upload-prompt p {
  margin: 8px 0;
  color: #6b7280;
}

.upload-hint {
  font-size: 12px;
  color: #9ca3af;
}

.file-list h4 {
  margin: 0 0 16px 0;
  color: #374151;
}

.file-items {
  max-height: 200px;
  overflow-y: auto;
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  margin-bottom: 8px;
  background-color: white;
}

.file-info {
  flex: 1;
  text-align: left;
}

.file-name {
  display: block;
  font-weight: 500;
  color: #374151;
  word-break: break-all;
}

.file-size {
  font-size: 12px;
  color: #6b7280;
}

.remove-file {
  background: none;
  border: none;
  color: #ef4444;
  cursor: pointer;
  padding: 4px;
  font-size: 14px;
}

.remove-file:hover {
  background-color: #fee2e2;
  border-radius: 4px;
}

.more-files {
  text-align: center;
  padding: 8px;
  color: #6b7280;
  font-style: italic;
}

.upload-progress {
  margin: 20px 0;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background-color: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-fill {
  height: 100%;
  background-color: #3b82f6;
  transition: width 0.3s ease;
}

.upload-result {
  margin-top: 20px;
  padding: 16px;
  border-radius: 8px;
}

.result-summary.success {
  background-color: #f0fdf4;
  border: 1px solid #bbf7d0;
}

.result-summary.error {
  background-color: #fef2f2;
  border: 1px solid #fecaca;
}

.result-summary h4 {
  margin: 0 0 8px 0;
}

.result-summary p {
  margin: 0;
  color: #6b7280;
}

.result-details {
  margin-top: 16px;
}

.result-details h5 {
  margin: 0 0 12px 0;
  color: #374151;
}

.details-list {
  max-height: 200px;
  overflow-y: auto;
}

.detail-item {
  padding: 6px 8px;
  margin-bottom: 4px;
  border-radius: 4px;
  font-size: 13px;
}

.detail-item.success {
  background-color: #f0fdf4;
  color: #166534;
}

.detail-item.error {
  background-color: #fef2f2;
  color: #dc2626;
}

.show-more-btn {
  background: none;
  border: none;
  color: #3b82f6;
  cursor: pointer;
  padding: 4px 0;
  font-size: 13px;
  text-decoration: underline;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid #e5e7eb;
}

.btn-primary, .btn-secondary {
  padding: 8px 16px;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
}

.btn-primary {
  background-color: #3b82f6;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background-color: #2563eb;
}

.btn-primary:disabled {
  background-color: #9ca3af;
  cursor: not-allowed;
}

.btn-secondary {
  background-color: #f3f4f6;
  color: #374151;
}

.btn-secondary:hover:not(:disabled) {
  background-color: #e5e7eb;
}

.btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style> 