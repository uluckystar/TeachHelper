<template>
  <el-dialog
    v-model="visible"
    title="上传文档"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="document-upload-dialog">
      <!-- 基本信息 -->
      <el-card class="upload-info-card" shadow="never">
        <template #header>
          <span>上传至知识库</span>
        </template>
        <div class="kb-info" v-if="knowledgeBase">
          <el-descriptions :column="2" size="small">
            <el-descriptions-item label="知识库名称">
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
          </el-descriptions>
        </div>
      </el-card>

      <!-- 文档上传 -->
      <el-card class="upload-card">
        <template #header>
          <div class="card-header">
            <span>选择文档</span>
            <div class="header-info">
              <el-text type="info" size="small">
                支持格式：PDF、Word、PPT、TXT、图片（OCR）
              </el-text>
            </div>
          </div>
        </template>

        <el-upload
          ref="uploadRef"
          class="upload-dragger"
          drag
          multiple
          :auto-upload="false"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :before-upload="beforeUpload"
          :accept="acceptedTypes"
          :file-list="fileList"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持 PDF (.pdf)、Word (.docx, .doc)、PowerPoint (.pptx, .ppt)、文本 (.txt)、图片 (.jpg, .png, .gif) 格式
              <br>
              单个文件大小不超过 50MB，最多同时上传 10 个文件
            </div>
          </template>
        </el-upload>
      </el-card>

      <!-- 上传配置 -->
      <el-card class="config-card" v-if="fileList.length > 0">
        <template #header>
          <span>上传配置</span>
        </template>
        
        <el-form :model="uploadConfig" label-width="120px">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="OCR引擎">
                <el-select v-model="uploadConfig.ocrEngine" placeholder="选择OCR引擎">
                  <el-option label="Tesseract (免费)" value="tesseract" />
                  <el-option label="百度OCR" value="baidu" />
                  <el-option label="腾讯OCR" value="tencent" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="文档语言">
                <el-select v-model="uploadConfig.language" placeholder="选择文档主要语言">
                  <el-option label="中文" value="zh" />
                  <el-option label="英文" value="en" />
                  <el-option label="中英混合" value="zh-en" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-form-item label="知识点提取">
            <el-switch v-model="uploadConfig.extractKnowledgePoints" />
            <el-text type="info" size="small" style="margin-left: 10px">
              自动识别并提取文档中的关键知识点
            </el-text>
          </el-form-item>
          
          <el-form-item label="文档标签">
            <el-select
              v-model="uploadConfig.tags"
              multiple
              filterable
              allow-create
              placeholder="添加文档标签"
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
        </el-form>
      </el-card>

      <!-- 上传进度 -->
      <el-card class="progress-card" v-if="uploading">
        <template #header>
          <span>上传进度</span>
        </template>
        
        <div class="upload-progress">
          <div class="overall-progress">
            <div class="progress-info">
              <span>总体进度：{{ completedFiles }} / {{ totalFiles }}</span>
              <span>{{ Math.round((completedFiles / totalFiles) * 100) }}%</span>
            </div>
            <el-progress 
              :percentage="Math.round((completedFiles / totalFiles) * 100)"
              :status="uploadStatus"
            />
          </div>
          
          <div class="file-progress-list">
            <div 
              v-for="(file, index) in uploadingFiles" 
              :key="index"
              class="file-progress-item"
            >
              <div class="file-info">
                <el-icon><document /></el-icon>
                <span class="file-name">{{ file.name }}</span>
                <el-tag :type="getStatusType(file.status)" size="small">
                  {{ getStatusText(file.status) }}
                </el-tag>
              </div>
              <el-progress 
                v-if="file.status === 'uploading'"
                :percentage="file.progress"
                :show-text="false"
                :stroke-width="4"
              />
              <div v-if="file.error" class="error-message">
                <el-text type="danger" size="small">{{ file.error }}</el-text>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose" :disabled="uploading">取消</el-button>
        <el-button 
          type="primary" 
          @click="startUpload" 
          :loading="uploading"
          :disabled="fileList.length === 0"
        >
          {{ uploading ? '上传中...' : '开始上传' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { UploadFilled, Document } from '@element-plus/icons-vue'
import { documentApi, type KnowledgeBase, type DocumentUploadConfig } from '@/api/knowledge'
import type { UploadFile, UploadFiles, UploadRawFile } from 'element-plus'

interface Props {
  modelValue: boolean
  knowledgeBase?: KnowledgeBase | null
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'uploaded', documents: any[]): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const uploadRef = ref()
const uploading = ref(false)
const fileList = ref<UploadFiles>([])
const uploadingFiles = ref<Array<{
  name: string
  status: 'waiting' | 'uploading' | 'processing' | 'success' | 'error'
  progress: number
  error?: string
}>>([])

// 上传配置
const uploadConfig = reactive({
  ocrEngine: 'tesseract',
  language: 'zh',
  extractKnowledgePoints: true,
  tags: [] as string[]
})

// 常用标签
const commonTags = [
  '教材', '练习题', '考试真题', '课件', '参考资料',
  '基础知识', '重点难点', '专项训练', '综合应用'
]

// 支持的文件类型
const acceptedTypes = '.pdf,.doc,.docx,.ppt,.pptx,.txt,.jpg,.jpeg,.png,.gif'

// 计算属性
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const totalFiles = computed(() => uploadingFiles.value.length)
const completedFiles = computed(() => 
  uploadingFiles.value.filter(f => f.status === 'success' || f.status === 'error').length
)

const uploadStatus = computed(() => {
  if (uploadingFiles.value.every(f => f.status === 'success')) return 'success'
  if (uploadingFiles.value.some(f => f.status === 'error')) return 'exception'
  return undefined
})

// 文件操作
const handleFileChange = (file: UploadFile, files: UploadFiles) => {
  fileList.value = files
}

const handleFileRemove = (file: UploadFile, files: UploadFiles) => {
  fileList.value = files
}

const beforeUpload = (file: UploadRawFile) => {
  // 检查文件大小
  const isLt50M = file.size / 1024 / 1024 < 50
  if (!isLt50M) {
    ElMessage.error(`文件 ${file.name} 大小超过 50MB`)
    return false
  }

  // 检查文件数量
  if (fileList.value.length >= 10) {
    ElMessage.error('最多只能同时上传 10 个文件')
    return false
  }

  return true
}

// 状态辅助函数
const getStatusType = (status: string) => {
  switch (status) {
    case 'success': return 'success'
    case 'error': return 'danger'
    case 'uploading': 
    case 'processing': return 'warning'
    default: return 'info'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'waiting': return '等待中'
    case 'uploading': return '上传中'
    case 'processing': return '处理中'
    case 'success': return '成功'
    case 'error': return '失败'
    default: return '未知'
  }
}

// 轮询文档处理状态
const pollDocumentStatus = async (documentId: number, uploadingFile: any) => {
  const maxAttempts = 30 // 最多轮询30次
  let attempts = 0
  
  const poll = async () => {
    attempts++
    
    try {
      const statusResponse = await documentApi.getDocumentStatus(documentId)
      const { status, progress, error } = statusResponse.data
      
      if (status === 'PROCESSING') {
        uploadingFile.status = 'processing'
        uploadingFile.progress = Math.max(uploadingFile.progress, progress || 0)
        
        if (attempts < maxAttempts) {
          setTimeout(poll, 2000) // 2秒后再次检查
        } else {
          uploadingFile.status = 'error'
          uploadingFile.error = '处理超时'
        }
      } else if (status === 'COMPLETED') {
        uploadingFile.status = 'success'
        uploadingFile.progress = 100
      } else if (status === 'FAILED') {
        uploadingFile.status = 'error'
        uploadingFile.error = error || '处理失败'
      }
    } catch (error) {
      console.error('获取文档状态失败:', error)
      if (attempts < maxAttempts) {
        setTimeout(poll, 2000)
      } else {
        uploadingFile.status = 'error'
        uploadingFile.error = '无法获取处理状态'
      }
    }
  }
  
  // 开始轮询
  setTimeout(poll, 1000) // 1秒后开始第一次检查
}

// 上传处理
const startUpload = async () => {
  if (!props.knowledgeBase?.id) {
    ElMessage.error('请先选择知识库')
    return
  }

  if (fileList.value.length === 0) {
    ElMessage.error('请先选择要上传的文件')
    return
  }

  uploading.value = true
  
  // 初始化上传状态
  uploadingFiles.value = fileList.value.map(file => ({
    name: file.name,
    status: 'waiting' as const,
    progress: 0
  }))

  const uploadedDocuments: any[] = []
  let successCount = 0
  let errorCount = 0

  try {
    // 依次上传每个文件
    for (let i = 0; i < fileList.value.length; i++) {
      const file = fileList.value[i]
      const uploadingFile = uploadingFiles.value[i]
      
      try {
        uploadingFile.status = 'uploading'
        
        // 上传文件 - 使用新的API格式
        const title = file.name.substring(0, file.name.lastIndexOf('.')) || file.name
        const config = {
          ocrEngine: uploadConfig.ocrEngine as 'tesseract' | 'baidu' | 'tencent',
          ocrLanguage: uploadConfig.language === 'zh' ? 'chi_sim' : 'eng',
          extractKnowledgePoints: uploadConfig.extractKnowledgePoints,
          autoClassifyDifficulty: true,
          enableVectorization: true,
          chunkSize: 1000,
          chunkOverlap: 200
        }

        const response = await documentApi.uploadDocument(
          file.raw!,
          props.knowledgeBase!.id,
          title,
          `通过前端上传 - 标签: ${uploadConfig.tags.join(', ')}`,
          config
        )

        uploadingFile.status = 'success'
        uploadingFile.progress = 100
        uploadedDocuments.push(response.data)
        successCount++

        // 开始轮询处理状态
        pollDocumentStatus(response.data.id, uploadingFile)

      } catch (error: any) {
        uploadingFile.status = 'error'
        uploadingFile.error = error.response?.data?.message || '上传失败'
        errorCount++
      }
    }

    // 显示结果
    if (successCount > 0) {
      ElNotification.success({
        title: '上传完成',
        message: `成功上传 ${successCount} 个文件${errorCount > 0 ? `，${errorCount} 个文件失败` : ''}`
      })
      emit('uploaded', uploadedDocuments)
    }

    if (errorCount > 0 && successCount === 0) {
      ElMessage.error('所有文件上传失败')
    }

  } catch (error) {
    console.error('上传过程出错:', error)
    ElMessage.error('上传过程出错')
  } finally {
    uploading.value = false
    
    // 如果全部成功，自动关闭对话框
    if (errorCount === 0) {
      setTimeout(() => {
        handleClose()
      }, 2000)
    }
  }
}

const handleClose = () => {
  if (uploading.value) {
    ElMessage.warning('文件正在上传中，请稍后再关闭')
    return
  }
  
  emit('update:modelValue', false)
  
  // 重置状态
  fileList.value = []
  uploadingFiles.value = []
  uploadConfig.tags = []
  
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}

// 监听对话框打开
watch(() => props.modelValue, (newVal) => {
  if (newVal && !props.knowledgeBase) {
    ElMessage.warning('请先选择知识库')
    emit('update:modelValue', false)
  }
})
</script>

<style scoped>
.document-upload-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.upload-info-card,
.upload-card,
.config-card,
.progress-card {
  border: 1px solid #e4e7ed;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kb-info .el-descriptions {
  margin-top: 10px;
}

.upload-dragger {
  width: 100%;
}

.upload-dragger .el-upload-dragger {
  width: 100%;
  height: 140px;
}

.upload-progress {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.overall-progress {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 14px;
  color: #606266;
}

.file-progress-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.file-progress-item {
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fff;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.file-name {
  flex: 1;
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.error-message {
  margin-top: 5px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
