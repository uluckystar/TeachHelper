<template>
  <div class="document-management">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" icon="Upload" @click="showUploadDialog = true">
          上传文档
        </el-button>
        <el-button icon="FolderAdd" @click="showBatchUploadDialog = true">
          批量上传
        </el-button>
        <el-button 
          icon="MagicStick" 
          @click="processAllDocuments"
          :loading="processingAll"
          v-if="documents.length > 0"
        >
          AI处理所有文档
        </el-button>
      </div>
      
      <div class="toolbar-right">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索文档..."
          prefix-icon="Search"
          style="width: 200px;"
          clearable
        />
        <el-select v-model="statusFilter" placeholder="状态筛选" style="width: 120px;">
          <el-option value="" label="全部状态" />
          <el-option value="waiting" label="等待中" />
          <el-option value="processing" label="处理中" />
          <el-option value="completed" label="已完成" />
          <el-option value="error" label="错误" />
        </el-select>
      </div>
    </div>

    <!-- 文档列表 -->
    <div class="document-list">
      <el-table 
        :data="filteredDocuments" 
        v-loading="loading"
        element-loading-text="加载中..."
        row-key="id"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column label="文档" min-width="200">
          <template #default="{ row }">
            <div class="document-info">
              <div class="document-icon">
                <el-icon size="24">
                  <Document v-if="row.type === 'pdf'" />
                  <DocumentCopy v-else-if="row.type === 'word'" />
                  <Picture v-else-if="row.type === 'image'" />
                  <VideoPlay v-else-if="row.type === 'video'" />
                  <Microphone v-else-if="row.type === 'audio'" />
                  <Files v-else />
                </el-icon>
              </div>
              <div class="document-details">
                <div class="document-name" @click="previewDocument(row)">
                  {{ row.fileName }}
                </div>
                <div class="document-meta">
                  {{ formatFileSize(row.fileSize) }} · {{ formatDate(row.uploadedAt) }}
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag 
              :type="getStatusTagType(row.status)"
              size="small"
            >
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="处理进度" width="150">
          <template #default="{ row }">
            <div v-if="row.status === 'processing'">
              <el-progress 
                :percentage="Math.round((row.processingProgress || 0) * 100)" 
                :stroke-width="6"
                size="small"
              />
            </div>
            <div v-else-if="row.status === 'completed'">
              <el-tag type="success" size="small">
                已提取 {{ row.knowledgePointCount || 0 }} 个知识点
              </el-tag>
            </div>
            <div v-else-if="row.status === 'error'">
              <el-tooltip :content="row.errorMessage || '处理失败'" placement="top">
                <el-tag type="danger" size="small">
                  处理失败
                </el-tag>
              </el-tooltip>
            </div>
            <div v-else-if="row.status === 'waiting'">
              <el-tag type="info" size="small">
                等待处理
              </el-tag>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button 
                size="small" 
                icon="View" 
                @click="previewDocument(row)"
                :disabled="!canPreview(row)"
              >
                预览
              </el-button>
              <el-button 
                size="small" 
                icon="MagicStick" 
                @click="processDocument(row)"
                :loading="row.processing"
                v-if="row.status !== 'processing' && row.status !== 'completed'"
              >
                处理
              </el-button>
              <el-button 
                size="small" 
                icon="Refresh" 
                @click="reprocessDocument(row)"
                v-if="row.status === 'error'"
              >
                重试
              </el-button>
              <el-dropdown @command="(cmd) => handleDocumentAction(cmd, row)">
                <el-button size="small" icon="MoreFilled" />
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="download" icon="Download">下载</el-dropdown-item>
                    <el-dropdown-item command="rename" icon="Edit">重命名</el-dropdown-item>
                    <el-dropdown-item divided command="delete" icon="Delete">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </el-button-group>
          </template>
        </el-table-column>
        
        <!-- 空状态 -->
        <template #empty>
          <div class="empty-state">
            <el-icon size="48" color="#dcdfe6">
              <Document />
            </el-icon>
            <div class="empty-text">
              {{ searchKeyword || statusFilter ? '没有找到匹配的文档' : '暂无文档' }}
            </div>
            <div class="empty-description">
              {{ searchKeyword || statusFilter ? '尝试调整搜索条件' : '点击上方"上传文档"按钮开始添加文档' }}
            </div>
          </div>
        </template>
      </el-table>
    </div>

    <!-- 批量操作栏 -->
    <div class="batch-actions" v-if="selectedDocuments.length > 0">
      <div class="batch-info">
        已选择 {{ selectedDocuments.length }} 个文档
      </div>
      <div class="batch-buttons">
        <el-button icon="MagicStick" @click="batchProcess">
          批量处理
        </el-button>
        <el-button icon="Download" @click="batchDownload">
          批量下载
        </el-button>
        <el-button icon="Delete" @click="batchDelete" type="danger">
          批量删除
        </el-button>
      </div>
    </div>

    <!-- 文档上传对话框 -->
    <DocumentUploadDialog
      v-model="showUploadDialog"
      :knowledge-base-id="knowledgeBaseId"
      @uploaded="handleDocumentUploaded"
    />

    <!-- 批量上传对话框 -->
    <BatchUploadDialog
      v-model="showBatchUploadDialog"
      :knowledge-base-id="knowledgeBaseId"
      @uploaded="handleDocumentUploaded"
    />

    <!-- 文档预览对话框 -->
    <DocumentPreviewDialog
      v-model="showPreviewDialog"
      :document="previewDocumentData"
    />

    <!-- 重命名对话框 -->
    <el-dialog v-model="showRenameDialog" title="重命名文档" width="400px">
      <el-form>
        <el-form-item label="文档名称">
          <el-input v-model="newFileName" placeholder="请输入新的文档名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRenameDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmRename">确定</el-button>
      </template>
    </el-dialog>

    <!-- 处理进度对话框 -->
    <ProcessingProgressDialog
      v-model="showProgressDialog"
      :documents="processingDocuments"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document,
  DocumentCopy,
  Picture,
  VideoPlay,
  Microphone,
  Files,
  Upload,
  FolderAdd,
  MagicStick,
  Search,
  View,
  Refresh,
  MoreFilled,
  Download,
  Edit,
  Delete
} from '@element-plus/icons-vue'
import DocumentUploadDialog from './DocumentUploadDialog.vue'
import BatchUploadDialog from './BatchUploadDialog.vue'
import DocumentPreviewDialog from './DocumentPreviewDialog.vue'
import ProcessingProgressDialog from './ProcessingProgressDialog.vue'
import { documentApi } from '@/api/knowledge'

// Props
const props = defineProps<{
  knowledgeBaseId: number
}>()

// Emits
const emit = defineEmits<{
  documentUploaded: []
}>()

// 响应式数据
const documents = ref<DocumentInfo[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const statusFilter = ref('')
const selectedDocuments = ref<DocumentInfo[]>([])
const processingAll = ref(false)
const processingDocuments = ref<DocumentInfo[]>([])

// 对话框状态
const showUploadDialog = ref(false)
const showBatchUploadDialog = ref(false)
const showPreviewDialog = ref(false)
const showRenameDialog = ref(false)
const showProgressDialog = ref(false)

// 重命名相关
const renamingDocument = ref<DocumentInfo | null>(null)
const newFileName = ref('')

// 预览相关
const previewDocumentData = ref<DocumentInfo | null>(null)

// 计算属性
const filteredDocuments = computed(() => {
  let filtered = documents.value

  // 按关键词筛选
  if (searchKeyword.value) {
    filtered = filtered.filter(doc => 
      doc.fileName.toLowerCase().includes(searchKeyword.value.toLowerCase())
    )
  }

  // 按状态筛选
  if (statusFilter.value) {
    filtered = filtered.filter(doc => doc.status === statusFilter.value)
  }

  return filtered
})

// 类型定义
interface DocumentInfo {
  id: number
  fileName: string
  fileSize: number
  type: string
  status: 'waiting' | 'processing' | 'completed' | 'error'
  uploadedAt: string
  processingProgress?: number
  knowledgePointCount?: number
  errorMessage?: string
  processing?: boolean
  url?: string
}

// 生命周期
onMounted(() => {
  loadDocuments()
})

// 监听知识库ID变化
watch(() => props.knowledgeBaseId, () => {
  loadDocuments()
})

// 方法
const loadDocuments = async () => {
  if (!props.knowledgeBaseId) {
    console.warn('No knowledge base ID provided')
    return
  }
  
  loading.value = true
  try {
    console.log('Loading documents for knowledge base:', props.knowledgeBaseId)
    
    // 调用API获取文档列表
    const response = await documentApi.getDocuments(props.knowledgeBaseId)
    console.log('API response:', response)
    
    // 确保response.data是数组
    const documentsData = Array.isArray(response.data) ? response.data : []
    
    // 转换API响应到前端数据格式
    documents.value = documentsData.map((doc: any) => ({
      id: doc.id,
      fileName: doc.fileName || '未命名文档',
      fileSize: doc.fileSize || 0,
      type: getFileTypeFromFileName(doc.fileName || ''),
      status: mapProcessingStatus(doc.processingStatus || 'PENDING'),
      uploadedAt: doc.createdAt || new Date().toISOString(),
      processingProgress: doc.processingProgress || 0,
      knowledgePointCount: doc.extractedKnowledgePointsCount || 0,
      errorMessage: doc.processingError || '',
      processing: false,
      url: doc.filePath || `/uploads/${doc.fileName}` // 使用实际文件路径
    }))
    
    console.log('Processed documents:', documents.value)
    ElMessage.success(`成功加载 ${documents.value.length} 个文档`)
  } catch (error: any) {
    console.error('Load documents failed:', error)
    ElMessage.error(`加载文档列表失败: ${error.message || '网络错误'}`)
    
    // 如果API调用失败，显示空列表
    documents.value = []
  } finally {
    loading.value = false
  }
}

// 从文件名获取文件类型
const getFileTypeFromFileName = (fileName: string): string => {
  if (!fileName) return 'file'
  
  const extension = fileName.toLowerCase().split('.').pop() || ''
  
  const typeMap: Record<string, string> = {
    'pdf': 'pdf',
    'doc': 'word',
    'docx': 'word',
    'txt': 'text',
    'md': 'text',
    'jpg': 'image',
    'jpeg': 'image',
    'png': 'image',
    'gif': 'image',
    'bmp': 'image',
    'webp': 'image',
    'mp4': 'video',
    'avi': 'video',
    'mov': 'video',
    'wmv': 'video',
    'mp3': 'audio',
    'wav': 'audio',
    'flac': 'audio',
    'ppt': 'presentation',
    'pptx': 'presentation',
    'xls': 'spreadsheet',
    'xlsx': 'spreadsheet'
  }
  
  return typeMap[extension] || 'file'
}

// 映射处理状态
const mapProcessingStatus = (status: string): 'waiting' | 'processing' | 'completed' | 'error' => {
  if (!status) return 'error'
  
  const statusMap: Record<string, 'waiting' | 'processing' | 'completed' | 'error'> = {
    'PENDING': 'waiting',
    'PROCESSING': 'processing',
    'COMPLETED': 'completed',
    'FAILED': 'error',
    'ERROR': 'error',
    // 兼容其他可能的状态值
    'UPLOADING': 'waiting',
    'SUCCESS': 'completed'
  }
  
  return statusMap[status.toUpperCase()] || 'error'
}

const handleSelectionChange = (selection: DocumentInfo[]) => {
  selectedDocuments.value = selection
}

const previewDocument = (document: DocumentInfo) => {
  // 所有文档都可以预览，不再检查类型限制
  previewDocumentData.value = document
  showPreviewDialog.value = true
}

const canPreview = (document: DocumentInfo) => {
  // 支持更多文件类型的预览
  const previewableTypes = ['pdf', 'image', 'text', 'word', 'presentation', 'spreadsheet']
  return previewableTypes.includes(document.type)
}

const processDocument = async (document: DocumentInfo) => {
  document.processing = true
  try {
    // TODO: 调用API处理文档
    ElMessage.success('文档处理已开始')
    document.status = 'processing'
    document.processingProgress = 0
    
    // 模拟处理进度
    simulateProcessing(document)
  } catch (error) {
    ElMessage.error('启动文档处理失败')
    console.error('Process document failed:', error)
  } finally {
    document.processing = false
  }
}

const reprocessDocument = async (document: DocumentInfo) => {
  try {
    await ElMessageBox.confirm(
      '确定要重新处理这个文档吗？',
      '确认重新处理',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    processDocument(document)
  } catch (error) {
    // 用户取消
  }
}

const processAllDocuments = async () => {
  const unprocessedDocs = documents.value.filter(doc => 
    doc.status !== 'processing' && doc.status !== 'completed'
  )
  
  if (unprocessedDocs.length === 0) {
    ElMessage.info('没有需要处理的文档')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要处理 ${unprocessedDocs.length} 个文档吗？`,
      '确认批量处理',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    processingAll.value = true
    processingDocuments.value = unprocessedDocs
    showProgressDialog.value = true
    
    // 逐个处理文档
    for (const doc of unprocessedDocs) {
      await processDocument(doc)
    }
    
    ElMessage.success('所有文档处理完成')
  } catch (error) {
    // 用户取消
  } finally {
    processingAll.value = false
    showProgressDialog.value = false
  }
}

const handleDocumentAction = async (command: string, document: DocumentInfo) => {
  switch (command) {
    case 'download':
      downloadDocument(document)
      break
    case 'rename':
      renameDocument(document)
      break
    case 'delete':
      deleteDocument(document)
      break
  }
}

const downloadDocument = async (documentInfo: DocumentInfo) => {
  try {
    // 创建下载链接 - 使用api实例或者正确的URL构建
    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
    const downloadUrl = `${baseUrl}/knowledge/documents/${documentInfo.id}/download`
    
    console.log('Download URL:', downloadUrl) // 调试日志
    
    // 获取用户token
    const token = localStorage.getItem('token')
    
    // 使用fetch下载文件
    const response = await fetch(downloadUrl, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    if (!response.ok) {
      throw new Error(`下载失败: ${response.statusText}`)
    }
    
    // 获取文件blob
    const blob = await response.blob()
    
    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = documentInfo.fileName
    
    // 触发下载
    document.body.appendChild(link)
    link.click()
    
    // 清理
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('文档下载成功')
  } catch (error: any) {
    console.error('Download document failed:', error)
    ElMessage.error(`下载文档失败: ${error.message || '网络错误'}`)
  }
}

const renameDocument = (document: DocumentInfo) => {
  renamingDocument.value = document
  newFileName.value = document.fileName
  showRenameDialog.value = true
}

const confirmRename = async () => {
  if (!renamingDocument.value || !newFileName.value.trim()) return
  
  try {
    // TODO: 调用API重命名文档
    renamingDocument.value.fileName = newFileName.value.trim()
    ElMessage.success('重命名成功')
    showRenameDialog.value = false
  } catch (error) {
    ElMessage.error('重命名失败')
    console.error('Rename document failed:', error)
  }
}

const deleteDocument = async (document: DocumentInfo) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文档"${document.fileName}"吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 调用API删除文档
    await documentApi.deleteDocument(document.id)
    
    // 从列表中移除文档
    const index = documents.value.findIndex(d => d.id === document.id)
    if (index > -1) {
      documents.value.splice(index, 1)
    }
    
    ElMessage.success('文档删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除文档失败')
      console.error('Delete document failed:', error)
    }
  }
}

const batchProcess = async () => {
  const unprocessedDocs = selectedDocuments.value.filter(doc => 
    doc.status !== 'processing' && doc.status !== 'completed'
  )
  
  if (unprocessedDocs.length === 0) {
    ElMessage.info('选中的文档中没有需要处理的')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要处理选中的 ${unprocessedDocs.length} 个文档吗？`,
      '确认批量处理',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    for (const doc of unprocessedDocs) {
      await processDocument(doc)
    }
    
    ElMessage.success('批量处理已开始')
  } catch (error) {
    // 用户取消
  }
}

const batchDownload = async () => {
  if (selectedDocuments.value.length === 0) {
    ElMessage.warning('请先选择要下载的文档')
    return
  }
  
  try {
    // 逐个下载选中的文档
    for (const document of selectedDocuments.value) {
      await downloadDocument(document)
      // 添加小延迟避免并发过多
      await new Promise(resolve => setTimeout(resolve, 200))
    }
    
    ElMessage.success(`成功下载 ${selectedDocuments.value.length} 个文档`)
  } catch (error) {
    console.error('Batch download failed:', error)
    ElMessage.error('批量下载失败')
  }
}

const batchDelete = async () => {
  if (selectedDocuments.value.length === 0) return
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedDocuments.value.length} 个文档吗？此操作不可恢复。`,
      '确认批量删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 调用API批量删除文档
    const deletePromises = selectedDocuments.value.map(doc => 
      documentApi.deleteDocument(doc.id)
    )
    
    try {
      await Promise.all(deletePromises)
      
      // 从列表中移除已删除的文档
      const idsToDelete = selectedDocuments.value.map(doc => doc.id)
      documents.value = documents.value.filter(doc => !idsToDelete.includes(doc.id))
      selectedDocuments.value = []
      
      ElMessage.success('批量删除成功')
    } catch (deleteError) {
      console.error('Some documents failed to delete:', deleteError)
      // 刷新列表以获取最新状态
      loadDocuments()
      ElMessage.warning('部分文档删除失败，已刷新列表')
    }
    
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
      console.error('Batch delete failed:', error)
    }
  }
}

const handleDocumentUploaded = () => {
  console.log('Document uploaded, refreshing list...')
  loadDocuments()
  emit('documentUploaded')
  ElMessage.success('文档上传成功，列表已刷新')
}

// 工具方法
const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString()
}

const getStatusTagType = (status: string): 'success' | 'info' | 'warning' | 'danger' => {
  const typeMap: Record<string, 'success' | 'info' | 'warning' | 'danger'> = {
    waiting: 'info',
    processing: 'warning',
    completed: 'success',
    error: 'danger'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status: string) => {
  const textMap: Record<string, string> = {
    waiting: '等待中',
    processing: '处理中',
    completed: '已完成',
    error: '错误'
  }
  return textMap[status] || '未知'
}

// 模拟处理进度
const simulateProcessing = (document: DocumentInfo) => {
  const interval = setInterval(() => {
    if (document.processingProgress! >= 100) {
      document.status = 'completed'
      document.knowledgePointCount = Math.floor(Math.random() * 30) + 10
      clearInterval(interval)
      return
    }
    
    document.processingProgress = (document.processingProgress || 0) + Math.random() * 10
    if (document.processingProgress > 100) {
      document.processingProgress = 100
    }
  }, 1000)
}
</script>

<style scoped>
.document-management {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  margin-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.toolbar-left {
  display: flex;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 文档列表 */
.document-list {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.document-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.document-icon {
  color: #409eff;
}

.document-details {
  flex: 1;
}

.document-name {
  font-weight: 500;
  color: #303133;
  cursor: pointer;
  transition: color 0.2s;
}

.document-name:hover {
  color: #409eff;
}

.document-meta {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

/* 批量操作栏 */
.batch-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-top: 16px;
}

.batch-info {
  font-size: 14px;
  color: #606266;
}

.batch-buttons {
  display: flex;
  gap: 8px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .toolbar-left,
  .toolbar-right {
    justify-content: center;
  }
  
  .batch-actions {
    flex-direction: column;
    gap: 12px;
    text-align: center;
  }
}
</style>
