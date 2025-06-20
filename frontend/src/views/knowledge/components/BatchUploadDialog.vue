<template>
  <el-dialog
    v-model="visible"
    title="批量文档上传"
    width="800px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="batch-upload-dialog">
      <!-- 上传区域 -->
      <el-card class="upload-section" shadow="never">
        <template #header>
          <div class="section-header">
            <el-icon><FolderOpened /></el-icon>
            <span>选择文件</span>
          </div>
        </template>

        <el-upload
          ref="batchUploadRef"
          class="batch-uploader"
          drag
          multiple
          :auto-upload="false"
          :file-list="fileList"
          :accept="acceptedTypes"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :limit="50"
          :on-exceed="handleExceed"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">
            拖拽文件到此处，或<em>点击选择文件</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持PDF、Word、PowerPoint、TXT、图片等格式，最多50个文件
            </div>
          </template>
        </el-upload>
      </el-card>

      <!-- 文件列表 -->
      <el-card v-if="fileList.length > 0" class="file-list-section">
        <template #header>
          <div class="section-header">
            <el-icon><Document /></el-icon>
            <span>文件列表 ({{ fileList.length }} 个文件)</span>
            <div class="header-actions">
              <el-button size="small" @click="clearFiles">清空</el-button>
              <el-button size="small" type="primary" @click="selectAll">全选</el-button>
            </div>
          </div>
        </template>

        <div class="file-list">
          <div
            v-for="(file, index) in fileList"
            :key="file.uid"
            class="file-item"
            :class="{ 'selected': selectedFiles.includes(file.uid) }"
          >
            <el-checkbox
              :model-value="selectedFiles.includes(file.uid)"
              @change="(checked) => handleFileSelect(checked as boolean, file.uid)"
            />
            
            <div class="file-info">
              <div class="file-icon">
                <el-icon><Document /></el-icon>
              </div>
              <div class="file-details">
                <div class="file-name">{{ file.name }}</div>
                <div class="file-meta">
                  <span class="file-size">{{ formatFileSize(file.size) }}</span>
                  <span class="file-type">{{ getFileType(file.name) }}</span>
                </div>
              </div>
            </div>

            <div class="file-config">
              <el-form size="small" inline>
                <el-form-item label="知识库">
                  <el-select
                    v-model="fileConfigs[file.uid]?.knowledgeBaseId"
                    placeholder="选择知识库"
                    size="small"
                    style="width: 120px"
                  >
                    <el-option
                      v-for="kb in knowledgeBases"
                      :key="kb.id"
                      :label="kb.name"
                      :value="kb.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="学科">
                  <el-select
                    v-model="fileConfigs[file.uid]?.subject"
                    placeholder="学科"
                    size="small"
                    style="width: 80px"
                  >
                    <el-option label="数学" value="数学" />
                    <el-option label="语文" value="语文" />
                    <el-option label="英语" value="英语" />
                    <el-option label="物理" value="物理" />
                    <el-option label="化学" value="化学" />
                    <el-option label="生物" value="生物" />
                  </el-select>
                </el-form-item>
                <el-form-item label="年级">
                  <el-select
                    v-model="fileConfigs[file.uid]?.gradeLevel"
                    placeholder="年级"
                    size="small"
                    style="width: 100px"
                  >
                    <el-option label="小学" value="小学" />
                    <el-option label="初中" value="初中" />
                    <el-option label="高中" value="高中" />
                    <el-option label="大学" value="大学" />
                  </el-select>
                </el-form-item>
              </el-form>
            </div>

            <div class="file-actions">
              <el-button
                size="small"
                type="danger"
                text
                @click="removeFile(index)"
              >
                移除
              </el-button>
            </div>

            <!-- 上传进度 -->
            <div v-if="uploadProgress[file.uid]" class="upload-progress">
              <el-progress
                :percentage="uploadProgress[file.uid].percentage"
                :status="uploadProgress[file.uid].status"
                :stroke-width="4"
              />
              <span class="progress-text">{{ uploadProgress[file.uid].text }}</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 处理选项 -->
      <el-card v-if="fileList.length > 0" class="options-section">
        <template #header>
          <div class="section-header">
            <el-icon><Setting /></el-icon>
            <span>处理选项</span>
          </div>
        </template>

        <el-form :model="batchOptions" label-width="120px">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="OCR识别">
                <el-switch
                  v-model="batchOptions.enableOCR"
                  active-text="启用"
                  inactive-text="禁用"
                />
                <div class="option-hint">对图片和扫描文档进行文字识别</div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="智能分割">
                <el-switch
                  v-model="batchOptions.enableSegmentation"
                  active-text="启用"
                  inactive-text="禁用"
                />
                <div class="option-hint">按章节或知识点自动分割内容</div>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="向量化存储">
                <el-switch
                  v-model="batchOptions.enableVectorization"
                  active-text="启用"
                  inactive-text="禁用"
                />
                <div class="option-hint">生成向量表示，支持相似度搜索</div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="自动分类">
                <el-switch
                  v-model="batchOptions.autoClassification"
                  active-text="启用"
                  inactive-text="禁用"
                />
                <div class="option-hint">根据内容自动分类到知识点</div>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="默认知识库">
            <el-select
              v-model="batchOptions.defaultKnowledgeBaseId"
              placeholder="选择默认知识库"
              style="width: 200px"
            >
              <el-option
                v-for="kb in knowledgeBases"
                :key="kb.id"
                :label="kb.name"
                :value="kb.id"
              />
            </el-select>
            <div class="option-hint">未指定知识库的文件将使用此默认设置</div>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 上传统计 -->
      <el-card v-if="isUploading || uploadCompleted" class="stats-section">
        <template #header>
          <div class="section-header">
            <el-icon><DataBoard /></el-icon>
            <span>上传统计</span>
          </div>
        </template>

        <el-row :gutter="20">
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-number">{{ uploadStats.total }}</div>
              <div class="stat-label">总文件数</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-number success">{{ uploadStats.success }}</div>
              <div class="stat-label">上传成功</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-number error">{{ uploadStats.failed }}</div>
              <div class="stat-label">上传失败</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-number">{{ uploadStats.processing }}</div>
              <div class="stat-label">处理中</div>
            </div>
          </el-col>
        </el-row>

        <div v-if="isUploading" class="overall-progress">
          <div class="progress-info">
            <span>总体进度：{{ overallProgress }}%</span>
            <span>{{ uploadStats.success + uploadStats.failed }}/{{ uploadStats.total }}</span>
          </div>
          <el-progress
            :percentage="overallProgress"
            :stroke-width="12"
            :show-text="false"
          />
        </div>
      </el-card>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose" :disabled="isUploading">
          {{ isUploading ? '关闭' : '取消' }}
        </el-button>
        <el-button
          type="primary"
          @click="startBatchUpload"
          :loading="isUploading"
          :disabled="selectedFiles.length === 0"
        >
          {{ isUploading ? '上传中...' : `上传选中文件 (${selectedFiles.length})` }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  FolderOpened, Document, UploadFilled, Setting, DataBoard
} from '@element-plus/icons-vue'
import type { UploadFile, UploadFiles } from 'element-plus'

interface Props {
  modelValue: boolean
  knowledgeBases?: Array<{ id: string, name: string }>
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'upload-success', files: any[]): void
}

const props = withDefaults(defineProps<Props>(), {
  knowledgeBases: () => []
})

const emit = defineEmits<Emits>()

// 响应式数据
const batchUploadRef = ref()
const fileList = ref<UploadFile[]>([])
const selectedFiles = ref<number[]>([])
const isUploading = ref(false)
const uploadCompleted = ref(false)
const uploadProgress = ref<Record<string, any>>({})

// 文件配置
const fileConfigs = ref<Record<string, {
  knowledgeBaseId?: string
  subject?: string
  gradeLevel?: string
}>>({})

// 批量处理选项
const batchOptions = reactive({
  enableOCR: true,
  enableSegmentation: true,
  enableVectorization: true,
  autoClassification: true,
  defaultKnowledgeBaseId: ''
})

// 上传统计
const uploadStats = reactive({
  total: 0,
  success: 0,
  failed: 0,
  processing: 0
})

// 支持的文件类型
const acceptedTypes = ref([
  '.pdf', '.doc', '.docx', '.ppt', '.pptx', '.txt',
  '.jpg', '.jpeg', '.png', '.gif', '.bmp'
].join(','))

// 计算属性
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const overallProgress = computed(() => {
  if (uploadStats.total === 0) return 0
  return Math.round(((uploadStats.success + uploadStats.failed) / uploadStats.total) * 100)
})

// 监听文件变化，自动选中
watch(fileList, (newFiles) => {
  // 自动选中新添加的文件
  const newUids = newFiles.map(file => file.uid).filter(uid => !selectedFiles.value.includes(uid))
  selectedFiles.value.push(...newUids)
  
  // 为新文件初始化配置
  newUids.forEach(uid => {
    if (!fileConfigs.value[uid]) {
      fileConfigs.value[uid] = {
        knowledgeBaseId: batchOptions.defaultKnowledgeBaseId,
        subject: '',
        gradeLevel: ''
      }
    }
  })
}, { deep: true })

// 文件选择处理
const handleFileSelect = (checked: boolean, fileUid: number) => {
  if (checked) {
    if (!selectedFiles.value.includes(fileUid)) {
      selectedFiles.value.push(fileUid)
    }
  } else {
    selectedFiles.value = selectedFiles.value.filter(uid => uid !== fileUid)
  }
}

// 文件处理方法
const handleFileChange = (uploadFile: UploadFile, uploadFiles: UploadFiles) => {
  fileList.value = uploadFiles
}

const handleFileRemove = (uploadFile: UploadFile, uploadFiles: UploadFiles) => {
  fileList.value = uploadFiles
  selectedFiles.value = selectedFiles.value.filter(uid => uid !== uploadFile.uid)
  delete fileConfigs.value[uploadFile.uid]
  delete uploadProgress.value[uploadFile.uid]
}

const handleExceed = () => {
  ElMessage.warning('最多只能上传50个文件')
}

const removeFile = (index: number) => {
  const file = fileList.value[index]
  fileList.value.splice(index, 1)
  selectedFiles.value = selectedFiles.value.filter(uid => uid !== file.uid)
  delete fileConfigs.value[file.uid]
  delete uploadProgress.value[file.uid]
}

const clearFiles = () => {
  fileList.value = []
  selectedFiles.value = []
  fileConfigs.value = {}
  uploadProgress.value = {}
  resetUploadStats()
}

const selectAll = () => {
  selectedFiles.value = fileList.value.map(file => file.uid)
}

// 工具方法
const formatFileSize = (size?: number) => {
  if (!size) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let unitIndex = 0
  let fileSize = size
  
  while (fileSize >= 1024 && unitIndex < units.length - 1) {
    fileSize /= 1024
    unitIndex++
  }
  
  return `${fileSize.toFixed(1)} ${units[unitIndex]}`
}

const getFileType = (filename: string) => {
  const ext = filename.split('.').pop()?.toLowerCase()
  const typeMap: Record<string, string> = {
    'pdf': 'PDF',
    'doc': 'Word',
    'docx': 'Word',
    'ppt': 'PowerPoint',
    'pptx': 'PowerPoint',
    'txt': 'Text',
    'jpg': 'Image',
    'jpeg': 'Image',
    'png': 'Image',
    'gif': 'Image',
    'bmp': 'Image'
  }
  return typeMap[ext || ''] || 'Unknown'
}

const resetUploadStats = () => {
  uploadStats.total = 0
  uploadStats.success = 0
  uploadStats.failed = 0
  uploadStats.processing = 0
  uploadCompleted.value = false
}

// 上传处理
const startBatchUpload = async () => {
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请选择要上传的文件')
    return
  }

  try {
    isUploading.value = true
    resetUploadStats()
    uploadStats.total = selectedFiles.value.length

    // 模拟批量上传过程
    for (const fileUid of selectedFiles.value) {
      const file = fileList.value.find(f => f.uid === fileUid)
      if (!file) continue

      uploadStats.processing++
      uploadProgress.value[fileUid] = {
        percentage: 0,
        status: 'active',
        text: '准备上传...'
      }

      try {
        // 模拟上传过程
        await simulateUpload(fileUid, file)
        uploadStats.success++
        uploadStats.processing--
        
        uploadProgress.value[fileUid] = {
          percentage: 100,
          status: 'success',
          text: '上传成功'
        }
      } catch (error) {
        uploadStats.failed++
        uploadStats.processing--
        
        uploadProgress.value[fileUid] = {
          percentage: 100,
          status: 'exception',
          text: '上传失败'
        }
      }
    }

    uploadCompleted.value = true
    ElMessage.success(`批量上传完成！成功：${uploadStats.success}，失败：${uploadStats.failed}`)
    
    // 触发成功事件
    emit('upload-success', fileList.value.filter(f => selectedFiles.value.includes(f.uid)))

  } catch (error) {
    console.error('批量上传失败:', error)
    ElMessage.error('批量上传失败')
  } finally {
    isUploading.value = false
  }
}

const simulateUpload = (fileUid: number, file: UploadFile): Promise<void> => {
  return new Promise((resolve, reject) => {
    let progress = 0
    const config = fileConfigs.value[fileUid]
    
    const updateProgress = () => {
      progress += Math.random() * 20
      if (progress > 100) progress = 100
      
      uploadProgress.value[fileUid] = {
        percentage: Math.round(progress),
        status: 'active',
        text: progress < 30 ? '上传中...' : 
              progress < 60 ? 'OCR识别中...' : 
              progress < 90 ? '向量化处理中...' : '完成处理...'
      }
      
      if (progress >= 100) {
        // 模拟偶尔的失败
        if (Math.random() < 0.1) {
          reject(new Error('模拟上传失败'))
        } else {
          resolve()
        }
      } else {
        setTimeout(updateProgress, 200 + Math.random() * 300)
      }
    }
    
    updateProgress()
  })
}

const handleClose = () => {
  if (isUploading.value) {
    ElMessageBox.confirm('正在上传中，确定要关闭吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      visible.value = false
    }).catch(() => {
      // 用户取消
    })
  } else {
    visible.value = false
  }
}
</script>

<style scoped>
.batch-upload-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 70vh;
  overflow-y: auto;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.batch-uploader {
  width: 100%;
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 300px;
  overflow-y: auto;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  transition: all 0.2s;
}

.file-item:hover {
  border-color: #409eff;
}

.file-item.selected {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 200px;
}

.file-icon {
  font-size: 24px;
  color: #606266;
}

.file-details {
  flex: 1;
}

.file-name {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.file-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.file-config {
  flex: 1;
}

.file-actions {
  margin-left: auto;
}

.upload-progress {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.95);
  border-top: 1px solid #e4e7ed;
}

.progress-text {
  font-size: 12px;
  color: #606266;
  margin-left: 8px;
}

.option-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.stat-item {
  text-align: center;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
}

.stat-number {
  font-size: 24px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 4px;
}

.stat-number.success {
  color: #67c23a;
}

.stat-number.error {
  color: #f56c6c;
}

.stat-label {
  font-size: 14px;
  color: #606266;
}

.overall-progress {
  margin-top: 16px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
}

.dialog-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

:deep(.el-card__header) {
  background-color: #fafafa;
  border-bottom: 1px solid #ebeef5;
}

:deep(.el-upload-dragger) {
  border: 2px dashed #d9d9d9;
  border-radius: 6px;
  width: 100%;
  height: 180px;
  text-align: center;
  background: #fafafa;
  transition: all 0.2s;
}

:deep(.el-upload-dragger:hover) {
  border-color: #409eff;
}

:deep(.el-upload__text) {
  color: #606266;
  font-size: 14px;
  text-align: center;
}

:deep(.el-upload__text em) {
  color: #409eff;
  font-style: normal;
}
</style>
