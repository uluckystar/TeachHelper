<template>
  <el-dialog
    v-model="visible"
    title="批量上传文档"
    width="700px"
    :close-on-click-modal="false"
    @closed="resetForm"
  >
    <div class="batch-upload-dialog">
      <!-- 上传方式选择 -->
      <div class="upload-methods">
        <h3>选择上传方式</h3>
        <el-radio-group v-model="uploadMethod" class="method-group">
          <el-radio value="folder" class="method-option">
            <div class="method-content">
              <el-icon size="24"><Folder /></el-icon>
              <div class="method-info">
                <div class="method-title">文件夹上传</div>
                <div class="method-desc">选择一个包含多个文档的文件夹</div>
              </div>
            </div>
          </el-radio>
          
          <el-radio value="files" class="method-option">
            <div class="method-content">
              <el-icon size="24"><Document /></el-icon>
              <div class="method-info">
                <div class="method-title">多文件选择</div>
                <div class="method-desc">一次选择多个文档文件</div>
              </div>
            </div>
          </el-radio>
          
          <el-radio value="zip" class="method-option">
            <div class="method-content">
              <el-icon size="24"><Box /></el-icon>
              <div class="method-info">
                <div class="method-title">压缩包上传</div>
                <div class="method-desc">上传ZIP文件，自动解压并处理</div>
              </div>
            </div>
          </el-radio>
        </el-radio-group>
      </div>

      <!-- 文件上传区域 -->
      <div class="upload-area">
        <!-- 文件夹上传 -->
        <div v-if="uploadMethod === 'folder'" class="folder-upload">
          <input
            ref="folderInputRef"
            type="file"
            webkitdirectory
            multiple
            style="display: none"
            @change="handleFolderSelect"
          />
          <div class="upload-zone" @click="selectFolder">
            <el-icon size="48"><FolderAdd /></el-icon>
            <div class="upload-text">
              <div>点击选择文件夹</div>
              <div class="upload-hint">将上传文件夹中的所有支持格式的文档</div>
            </div>
          </div>
        </div>

        <!-- 多文件上传 -->
        <div v-else-if="uploadMethod === 'files'" class="files-upload">
          <el-upload
            ref="filesUploadRef"
            class="upload-dragger"
            drag
            :auto-upload="false"
            :multiple="true"
            :show-file-list="false"
            :on-change="handleFilesSelect"
            :accept="acceptedFileTypes"
            :limit="maxFileCount"
            :on-exceed="handleExceed"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处，或<em>点击选择</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 PDF、Word、PowerPoint、文本等格式，最多 {{ maxFileCount }} 个文件
              </div>
            </template>
          </el-upload>
        </div>

        <!-- ZIP上传 -->
        <div v-else-if="uploadMethod === 'zip'" class="zip-upload">
          <el-upload
            ref="zipUploadRef"
            class="upload-dragger"
            drag
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleZipSelect"
            accept=".zip,.rar,.7z"
            :limit="1"
            :on-exceed="handleZipExceed"
          >
            <el-icon class="el-icon--upload"><Box /></el-icon>
            <div class="el-upload__text">
              拖拽压缩文件到此处，或<em>点击选择</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 ZIP、RAR、7Z 格式，单个文件不超过 100MB
              </div>
            </template>
          </el-upload>
        </div>
      </div>

      <!-- 选中的文件列表 -->
      <div class="selected-files" v-if="selectedFiles.length > 0">
        <div class="files-header">
          <h4>已选择文件 ({{ selectedFiles.length }})</h4>
          <div class="files-actions">
            <el-button size="small" @click="clearAllFiles">
              清空全部
            </el-button>
            <el-button size="small" @click="selectAllFiles">
              {{ allSelected ? '取消全选' : '全选' }}
            </el-button>
          </div>
        </div>

        <div class="files-list">
          <div class="files-stats">
            <div class="stat-item">
              <span class="stat-label">总文件数：</span>
              <span class="stat-value">{{ selectedFiles.length }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">总大小：</span>
              <span class="stat-value">{{ formatFileSize(totalSize) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">支持格式：</span>
              <span class="stat-value">{{ supportedCount }}</span>
            </div>
            <div class="stat-item" v-if="unsupportedCount > 0">
              <span class="stat-label">不支持：</span>
              <span class="stat-value error">{{ unsupportedCount }}</span>
            </div>
          </div>

          <div class="files-container">
            <div 
              v-for="(file, index) in displayFiles" 
              :key="file.path || file.name"
              class="file-item"
              :class="{ 
                'file-unsupported': !file.supported,
                'file-selected': file.selected
              }"
            >
              <el-checkbox 
                v-model="file.selected"
                :disabled="!file.supported"
                @change="updateFileSelection"
              />
              
              <div class="file-info">
                <el-icon class="file-icon">
                  <Document v-if="file.type === 'pdf'" />
                  <DocumentCopy v-else-if="file.type === 'word'" />
                  <Picture v-else-if="file.type === 'image'" />
                  <Files v-else />
                </el-icon>
                <div class="file-details">
                  <div class="file-name" :title="file.name">{{ file.name }}</div>
                  <div class="file-path" v-if="file.path !== file.name" :title="file.path">
                    {{ file.path }}
                  </div>
                  <div class="file-meta">
                    {{ formatFileSize(file.size) }}
                    <span v-if="!file.supported" class="unsupported-label">- 不支持</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="files-pagination" v-if="selectedFiles.length > itemsPerPage">
            <el-pagination
              v-model:current-page="currentPage"
              :page-size="itemsPerPage"
              :total="selectedFiles.length"
              layout="prev, pager, next"
              small
            />
          </div>
        </div>
      </div>

      <!-- 上传配置 -->
      <div class="upload-config" v-if="selectedFiles.length > 0">
        <h4>上传配置</h4>
        <el-form :model="uploadConfig" label-width="120px">
          <el-form-item label="文件组织">
            <el-radio-group v-model="uploadConfig.fileOrganization">
              <el-radio value="flat">扁平化（忽略原文件夹结构）</el-radio>
              <el-radio value="preserve">保持原文件夹结构</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="重名处理">
            <el-radio-group v-model="uploadConfig.duplicateHandling">
              <el-radio value="rename">自动重命名</el-radio>
              <el-radio value="skip">跳过重复文件</el-radio>
              <el-radio value="overwrite">覆盖已有文件</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="自动处理">
            <el-switch 
              v-model="uploadConfig.autoProcess"
              active-text="上传完成后自动进行AI处理"
            />
          </el-form-item>

          <el-form-item label="处理模式" v-if="uploadConfig.autoProcess">
            <el-radio-group v-model="uploadConfig.processingMode">
              <el-radio value="sequential">顺序处理（稳定，较慢）</el-radio>
              <el-radio value="parallel">并行处理（快速，消耗资源多）</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="批量大小" v-if="uploadConfig.autoProcess">
            <el-select v-model="uploadConfig.batchSize" style="width: 200px">
              <el-option value={5} label="5个文件/批次" />
              <el-option value={10} label="10个文件/批次" />
              <el-option value={20} label="20个文件/批次" />
              <el-option value={0} label="不分批（一次性处理）" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <!-- 上传进度 -->
      <div class="upload-progress" v-if="uploading">
        <div class="progress-header">
          <h4>上传进度</h4>
          <div class="progress-actions">
            <el-button size="small" @click="pauseUpload" v-if="!uploadPaused">
              暂停
            </el-button>
            <el-button size="small" @click="resumeUpload" v-if="uploadPaused">
              继续
            </el-button>
            <el-button size="small" @click="cancelUpload" type="danger">
              取消
            </el-button>
          </div>
        </div>

        <div class="overall-progress">
          <el-progress 
            :percentage="overallProgress"
            :stroke-width="8"
            :text-inside="true"
            :status="uploadStatus"
          />
          <div class="progress-text">
            {{ uploadStatusText }}
          </div>
        </div>

        <div class="file-progress-list">
          <div 
            v-for="file in uploadingFiles" 
            :key="file.uid"
            class="file-progress-item"
          >
            <div class="file-progress-info">
              <span class="file-name">{{ file.name }}</span>
              <span class="file-status">{{ getFileStatusText(file.status) }}</span>
            </div>
            <el-progress 
              :percentage="file.percentage || 0"
              :status="getFileProgressStatus(file.status)"
              size="small"
            />
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false" :disabled="uploading">
          {{ uploading ? '上传中...' : '取消' }}
        </el-button>
        <el-button 
          type="primary" 
          @click="startBatchUpload"
          :loading="uploading"
          :disabled="selectedCount === 0"
        >
          开始上传 ({{ selectedCount }})
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Folder,
  Document,
  Box,
  FolderAdd,
  UploadFilled,
  DocumentCopy,
  Picture,
  Files
} from '@element-plus/icons-vue'

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

const uploadMethod = ref<'folder' | 'files' | 'zip'>('files')
const selectedFiles = ref<FileItem[]>([])
const uploading = ref(false)
const uploadPaused = ref(false)
const uploadingFiles = ref<FileItem[]>([])

// 分页
const currentPage = ref(1)
const itemsPerPage = 20

// 上传配置
const uploadConfig = ref({
  fileOrganization: 'flat',
  duplicateHandling: 'rename',
  autoProcess: true,
  processingMode: 'sequential',
  batchSize: 10
})

// 引用
const folderInputRef = ref<HTMLInputElement>()
const filesUploadRef = ref()
const zipUploadRef = ref()

// 常量
const maxFileCount = 100
const maxFileSize = 50 * 1024 * 1024 // 50MB
const acceptedFileTypes = '.pdf,.doc,.docx,.ppt,.pptx,.txt,.md,.jpg,.jpeg,.png,.gif'

// 类型定义
interface FileItem {
  uid: string
  name: string
  path: string
  size: number
  type: string
  supported: boolean
  selected: boolean
  file: File
  status?: 'waiting' | 'uploading' | 'success' | 'error'
  percentage?: number
  errorMessage?: string
}

// 计算属性
const displayFiles = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  const end = start + itemsPerPage
  return selectedFiles.value.slice(start, end)
})

const totalSize = computed(() => {
  return selectedFiles.value.reduce((sum, file) => sum + file.size, 0)
})

const supportedCount = computed(() => {
  return selectedFiles.value.filter(file => file.supported).length
})

const unsupportedCount = computed(() => {
  return selectedFiles.value.filter(file => !file.supported).length
})

const selectedCount = computed(() => {
  return selectedFiles.value.filter(file => file.selected && file.supported).length
})

const allSelected = computed(() => {
  const supportedFiles = selectedFiles.value.filter(file => file.supported)
  return supportedFiles.length > 0 && supportedFiles.every(file => file.selected)
})

const overallProgress = computed(() => {
  if (uploadingFiles.value.length === 0) return 0
  
  const totalPercentage = uploadingFiles.value.reduce((sum, file) => {
    return sum + (file.percentage || 0)
  }, 0)
  
  return Math.round(totalPercentage / uploadingFiles.value.length)
})

const uploadStatus = computed(() => {
  if (uploadPaused.value) return 'warning'
  if (uploading.value) return undefined
  return 'success'
})

const uploadStatusText = computed(() => {
  if (uploadPaused.value) return '上传已暂停'
  if (uploading.value) {
    const completed = uploadingFiles.value.filter(f => f.status === 'success' || f.status === 'error').length
    return `正在上传... ${completed}/${uploadingFiles.value.length} 个文件完成`
  }
  return '上传完成'
})

// 方法
const selectFolder = () => {
  folderInputRef.value?.click()
}

const handleFolderSelect = (event: Event) => {
  const input = event.target as HTMLInputElement
  const files = input.files
  if (!files) return

  const fileItems: FileItem[] = []
  
  for (let i = 0; i < files.length; i++) {
    const file = files[i]
    const fileItem = createFileItem(file, file.webkitRelativePath || file.name)
    fileItems.push(fileItem)
  }

  selectedFiles.value = fileItems
  updateFileSelection()
  
  ElMessage.success(`已选择 ${files.length} 个文件`)
}

const handleFilesSelect = (file: any, fileList: any[]) => {
  const fileItems = fileList.map(uploadFile => 
    createFileItem(uploadFile.raw, uploadFile.name)
  )
  
  selectedFiles.value = fileItems
  updateFileSelection()
}

const handleZipSelect = (file: any) => {
  const fileItem = createFileItem(file.raw, file.name)
  selectedFiles.value = [fileItem]
  updateFileSelection()
  
  ElMessage.success('已选择压缩文件，上传时将自动解压')
}

const createFileItem = (file: File, path: string): FileItem => {
  const extension = file.name.split('.').pop()?.toLowerCase() || ''
  const supported = isSupportedFileType(extension)
  
  return {
    uid: `${Date.now()}-${Math.random()}`,
    name: file.name,
    path: path,
    size: file.size,
    type: getFileType(extension),
    supported,
    selected: supported,
    file
  }
}

const isSupportedFileType = (extension: string): boolean => {
  const supportedTypes = [
    'pdf', 'doc', 'docx', 'ppt', 'pptx', 
    'txt', 'md', 'jpg', 'jpeg', 'png', 'gif'
  ]
  return supportedTypes.includes(extension)
}

const getFileType = (extension: string): string => {
  const typeMap: Record<string, string> = {
    pdf: 'pdf',
    doc: 'word',
    docx: 'word',
    ppt: 'powerpoint',
    pptx: 'powerpoint',
    txt: 'text',
    md: 'text',
    jpg: 'image',
    jpeg: 'image',
    png: 'image',
    gif: 'image'
  }
  return typeMap[extension] || 'unknown'
}

const updateFileSelection = () => {
  // 触发重新计算选中数量
}

const selectAllFiles = () => {
  const supportedFiles = selectedFiles.value.filter(file => file.supported)
  const newSelectedState = !allSelected.value
  
  supportedFiles.forEach(file => {
    file.selected = newSelectedState
  })
}

const clearAllFiles = () => {
  selectedFiles.value = []
  currentPage.value = 1
  
  // 清空上传组件
  if (filesUploadRef.value) {
    filesUploadRef.value.clearFiles()
  }
  if (zipUploadRef.value) {
    zipUploadRef.value.clearFiles()
  }
}

const handleExceed = () => {
  ElMessage.warning(`最多只能选择 ${maxFileCount} 个文件`)
}

const handleZipExceed = () => {
  ElMessage.warning('只能选择一个压缩文件')
}

const startBatchUpload = async () => {
  const filesToUpload = selectedFiles.value.filter(file => file.selected && file.supported)
  
  if (filesToUpload.length === 0) {
    ElMessage.warning('请选择要上传的文件')
    return
  }

  // 检查文件大小
  const oversizedFiles = filesToUpload.filter(file => file.size > maxFileSize)
  if (oversizedFiles.length > 0) {
    ElMessage.error(`以下文件超过大小限制：${oversizedFiles.map(f => f.name).join(', ')}`)
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要上传 ${filesToUpload.length} 个文件吗？`,
      '确认批量上传',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    uploading.value = true
    uploadingFiles.value = filesToUpload.map(file => ({ ...file, status: 'waiting', percentage: 0 }))

    // 开始上传
    await performBatchUpload(filesToUpload)

  } catch (error) {
    // 用户取消
  }
}

const performBatchUpload = async (files: FileItem[]) => {
  const batchSize = uploadConfig.value.batchSize || files.length
  
  try {
    if (batchSize === 0 || files.length <= batchSize) {
      // 一次性上传所有文件
      await uploadFileBatch(files)
    } else {
      // 分批上传
      for (let i = 0; i < files.length; i += batchSize) {
        if (uploadPaused.value) {
          await waitForResume()
        }
        
        const batch = files.slice(i, i + batchSize)
        await uploadFileBatch(batch)
      }
    }

    const successCount = uploadingFiles.value.filter(f => f.status === 'success').length
    const errorCount = uploadingFiles.value.filter(f => f.status === 'error').length

    if (successCount > 0) {
      ElMessage.success(`成功上传 ${successCount} 个文件`)
      emit('uploaded')
    }

    if (errorCount > 0) {
      ElMessage.warning(`${errorCount} 个文件上传失败`)
    }

    // 如果全部成功，关闭对话框
    if (errorCount === 0 && successCount > 0) {
      setTimeout(() => {
        visible.value = false
      }, 2000)
    }

  } catch (error) {
    ElMessage.error('批量上传失败')
    console.error('Batch upload failed:', error)
  } finally {
    uploading.value = false
    uploadPaused.value = false
  }
}

const uploadFileBatch = async (files: FileItem[]) => {
  const promises = files.map(file => uploadSingleFile(file))
  
  if (uploadConfig.value.processingMode === 'sequential') {
    // 顺序上传
    for (const promise of promises) {
      await promise
    }
  } else {
    // 并行上传
    await Promise.all(promises)
  }
}

const uploadSingleFile = async (file: FileItem) => {
  const fileInList = uploadingFiles.value.find(f => f.uid === file.uid)
  if (!fileInList) return

  fileInList.status = 'uploading'
  fileInList.percentage = 0

  try {
    const formData = new FormData()
    formData.append('file', file.file)
    formData.append('knowledgeBaseId', props.knowledgeBaseId.toString())
    formData.append('autoProcess', uploadConfig.value.autoProcess.toString())
    formData.append('fileOrganization', uploadConfig.value.fileOrganization)
    formData.append('duplicateHandling', uploadConfig.value.duplicateHandling)

    // 模拟上传进度
    const uploadPromise = simulateFileUpload(fileInList)
    await uploadPromise

    fileInList.status = 'success'
    fileInList.percentage = 100

  } catch (error: any) {
    fileInList.status = 'error'
    fileInList.errorMessage = error.message || '上传失败'
    console.error('Upload file failed:', error)
  }
}

const simulateFileUpload = (file: FileItem): Promise<void> => {
  return new Promise((resolve, reject) => {
    const interval = setInterval(() => {
      if (uploadPaused.value) return
      
      file.percentage = (file.percentage || 0) + Math.random() * 15
      
      if (file.percentage >= 100) {
        file.percentage = 100
        clearInterval(interval)
        
        // 模拟成功/失败
        if (Math.random() > 0.1) { // 90% 成功率
          resolve()
        } else {
          reject(new Error('网络错误'))
        }
      }
    }, 200)
  })
}

const pauseUpload = () => {
  uploadPaused.value = true
  ElMessage.info('上传已暂停')
}

const resumeUpload = () => {
  uploadPaused.value = false
  ElMessage.info('上传已继续')
}

const cancelUpload = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要取消批量上传吗？已上传的文件不会被删除。',
      '确认取消',
      {
        confirmButtonText: '确定',
        cancelButtonText: '继续上传',
        type: 'warning'
      }
    )

    uploading.value = false
    uploadPaused.value = false
    uploadingFiles.value = []
    
    ElMessage.info('批量上传已取消')
  } catch (error) {
    // 用户选择继续上传
  }
}

const waitForResume = (): Promise<void> => {
  return new Promise((resolve) => {
    const checkResume = () => {
      if (!uploadPaused.value) {
        resolve()
      } else {
        setTimeout(checkResume, 100)
      }
    }
    checkResume()
  })
}

const getFileStatusText = (status?: string): string => {
  const statusMap: Record<string, string> = {
    waiting: '等待中',
    uploading: '上传中',
    success: '已完成',
    error: '失败'
  }
  return statusMap[status || 'waiting'] || '未知'
}

const getFileProgressStatus = (status?: string) => {
  if (status === 'success') return 'success'
  if (status === 'error') return 'exception'
  return undefined
}

const resetForm = () => {
  uploadMethod.value = 'files'
  selectedFiles.value = []
  uploading.value = false
  uploadPaused.value = false
  uploadingFiles.value = []
  currentPage.value = 1
  
  uploadConfig.value = {
    fileOrganization: 'flat',
    duplicateHandling: 'rename',
    autoProcess: true,
    processingMode: 'sequential',
    batchSize: 10
  }

  // 清空所有上传组件
  if (folderInputRef.value) {
    folderInputRef.value.value = ''
  }
  if (filesUploadRef.value) {
    filesUploadRef.value.clearFiles()
  }
  if (zipUploadRef.value) {
    zipUploadRef.value.clearFiles()
  }
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
.batch-upload-dialog {
  max-height: 80vh;
  overflow-y: auto;
}

/* 上传方式选择 */
.upload-methods h3 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 16px;
}

.method-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.method-option {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  transition: all 0.3s;
  cursor: pointer;
}

.method-option:hover {
  border-color: #409eff;
  background-color: #f5f9ff;
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

.method-info {
  flex: 1;
}

.method-title {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.method-desc {
  font-size: 13px;
  color: #909399;
}

/* 上传区域 */
.upload-area {
  margin: 24px 0;
}

.upload-zone {
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  padding: 40px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  background-color: #fafbfc;
}

.upload-zone:hover {
  border-color: #409eff;
  background-color: #f5f9ff;
}

.upload-text {
  margin-top: 16px;
}

.upload-text > div:first-child {
  font-size: 16px;
  color: #303133;
  margin-bottom: 8px;
}

.upload-hint {
  font-size: 13px;
  color: #909399;
}

.upload-dragger .el-upload-dragger {
  width: 100%;
  height: 160px;
}

/* 文件列表 */
.selected-files {
  margin: 24px 0;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.files-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.files-header h4 {
  margin: 0;
  color: #303133;
  font-size: 14px;
}

.files-actions {
  display: flex;
  gap: 8px;
}

.files-stats {
  display: flex;
  gap: 20px;
  padding: 12px 16px;
  background-color: #fafbfc;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
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
  color: #303133;
  font-weight: 500;
}

.stat-value.error {
  color: #f56c6c;
}

.files-container {
  max-height: 300px;
  overflow-y: auto;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  border-bottom: 1px solid #f5f7fa;
  transition: background-color 0.2s;
}

.file-item:hover {
  background-color: #f5f7fa;
}

.file-item.file-unsupported {
  background-color: #fef0f0;
  opacity: 0.7;
}

.file-item.file-selected {
  background-color: #f0f9ff;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.file-icon {
  color: #409eff;
  font-size: 16px;
  flex-shrink: 0;
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
  margin-bottom: 2px;
}

.file-path {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;
}

.file-meta {
  font-size: 12px;
  color: #909399;
}

.unsupported-label {
  color: #f56c6c;
}

.files-pagination {
  padding: 12px;
  text-align: center;
  border-top: 1px solid #e4e7ed;
}

/* 上传配置 */
.upload-config {
  margin: 24px 0;
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.upload-config h4 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 14px;
}

/* 上传进度 */
.upload-progress {
  margin: 24px 0;
  padding: 16px;
  background-color: #f0f9ff;
  border-radius: 8px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.progress-header h4 {
  margin: 0;
  color: #303133;
  font-size: 14px;
}

.progress-actions {
  display: flex;
  gap: 8px;
}

.overall-progress {
  margin-bottom: 16px;
}

.progress-text {
  text-align: center;
  margin-top: 8px;
  font-size: 13px;
  color: #409eff;
}

.file-progress-list {
  max-height: 200px;
  overflow-y: auto;
}

.file-progress-item {
  margin-bottom: 12px;
  padding: 8px;
  background-color: white;
  border-radius: 4px;
}

.file-progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  font-size: 13px;
}

.file-progress-info .file-name {
  color: #303133;
  font-weight: 500;
}

.file-progress-info .file-status {
  color: #909399;
}

/* 对话框底部 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .method-group {
    gap: 8px;
  }
  
  .method-option {
    padding: 12px;
  }
  
  .files-stats {
    flex-direction: column;
    gap: 8px;
  }
  
  .files-header {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .upload-config {
    padding: 12px;
  }
}
</style>
