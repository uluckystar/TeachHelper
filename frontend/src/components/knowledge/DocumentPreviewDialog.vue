<template>
  <el-dialog
    v-model="visible"
    :title="document?.fileName || '文档预览'"
    width="80%"
    top="5vh"
    :close-on-click-modal="false"
    class="preview-dialog"
  >
    <div class="preview-container">
      <!-- 预览工具栏 -->
      <div class="preview-toolbar">
        <div class="toolbar-left">
          <el-button-group>
            <el-button 
              :icon="ZoomOut" 
              @click="zoomOut"
              :disabled="scale <= 0.5"
              size="small"
            />
            <el-button size="small" @click="resetZoom">
              {{ Math.round(scale * 100) }}%
            </el-button>
            <el-button 
              :icon="ZoomIn" 
              @click="zoomIn"
              :disabled="scale >= 3"
              size="small"
            />
          </el-button-group>
          
          <el-divider direction="vertical" />
          
          <el-button-group v-if="isPDF">
            <el-button 
              :icon="ArrowLeft" 
              @click="previousPage"
              :disabled="currentPage <= 1"
              size="small"
            />
            <el-button size="small">
              {{ currentPage }} / {{ totalPages }}
            </el-button>
            <el-button 
              :icon="ArrowRight" 
              @click="nextPage"
              :disabled="currentPage >= totalPages"
              size="small"
            />
          </el-button-group>
        </div>
        
        <div class="toolbar-right">
          <el-button :icon="RefreshRight" @click="rotate" size="small" />
          <el-button :icon="FullScreen" @click="toggleFullscreen" size="small" />
          <el-button :icon="Download" @click="downloadDocument" size="small" />
        </div>
      </div>

      <!-- 预览内容区域 -->
      <div class="preview-content" ref="previewContentRef">
        <!-- PDF 预览 -->
        <div v-if="isPDF" class="pdf-preview">
          <canvas 
            ref="pdfCanvasRef"
            :style="{ 
              transform: `scale(${scale}) rotate(${rotation}deg)`,
              transformOrigin: 'center center'
            }"
          />
        </div>

        <!-- 图片预览 -->
        <div v-else-if="isImage" class="image-preview">
          <img 
            :src="imageUrl"
            :style="{ 
              transform: `scale(${scale}) rotate(${rotation}deg)`,
              transformOrigin: 'center center',
              maxWidth: '100%',
              maxHeight: '100%'
            }"
            @load="handleImageLoad"
            @error="handleImageError"
          />
        </div>

        <!-- 文本预览 -->
        <div v-else-if="isText" class="text-preview">
          <div 
            class="text-content"
            :style="{ 
              fontSize: `${14 * scale}px`,
              transform: `rotate(${rotation}deg)`,
              transformOrigin: 'center center'
            }"
          >
            <pre v-if="textContent">{{ textContent }}</pre>
            <div v-else class="loading-text">
              <el-icon class="is-loading"><Loading /></el-icon>
              正在加载文本内容...
            </div>
          </div>
        </div>

        <!-- 不支持的文件类型 -->
        <div v-else class="unsupported-preview">
          <div class="unsupported-content">
            <el-icon size="64" color="#c0c4cc"><DocumentCopy /></el-icon>
            <h3>无法预览此文件类型</h3>
            <p>{{ document?.fileName }}</p>
            <el-button type="primary" @click="downloadDocument">
              下载文件
            </el-button>
          </div>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="preview-loading">
          <el-icon class="is-loading" size="32"><Loading /></el-icon>
          <div>正在加载预览...</div>
        </div>

        <!-- 错误状态 -->
        <div v-if="error" class="preview-error">
          <el-icon size="32" color="#f56c6c"><Warning /></el-icon>
          <div>{{ errorMessage }}</div>
          <el-button @click="retryLoad">重试</el-button>
        </div>
      </div>

      <!-- 文档信息面板 -->
      <div class="document-info-panel" v-if="showInfo">
        <h4>文档信息</h4>
        <div class="info-items">
          <div class="info-item">
            <span class="label">文件名：</span>
            <span class="value">{{ document?.fileName }}</span>
          </div>
          <div class="info-item">
            <span class="label">文件大小：</span>
            <span class="value">{{ formatFileSize(document?.fileSize || 0) }}</span>
          </div>
          <div class="info-item">
            <span class="label">文件类型：</span>
            <span class="value">{{ document?.type }}</span>
          </div>
          <div class="info-item">
            <span class="label">上传时间：</span>
            <span class="value">{{ formatDate(document?.uploadedAt || '') }}</span>
          </div>
          <div class="info-item" v-if="document?.knowledgePointCount">
            <span class="label">知识点数：</span>
            <span class="value">{{ document.knowledgePointCount }} 个</span>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="showInfo = !showInfo">
          {{ showInfo ? '隐藏' : '显示' }}信息
        </el-button>
        <el-button @click="visible = false">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ZoomIn,
  ZoomOut,
  ArrowLeft,
  ArrowRight,
  RefreshRight,
  FullScreen,
  Download,
  DocumentCopy,
  Loading,
  Warning
} from '@element-plus/icons-vue'

// Props
const props = defineProps<{
  modelValue: boolean
  document?: {
    id: number
    fileName: string
    fileSize: number
    type: string
    uploadedAt: string
    knowledgePointCount?: number
    url?: string
  } | null
}>()

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

// 响应式数据
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const loading = ref(false)
const error = ref(false)
const errorMessage = ref('')
const scale = ref(1)
const rotation = ref(0)
const showInfo = ref(false)

// PDF 相关
const currentPage = ref(1)
const totalPages = ref(0)
const pdfCanvasRef = ref<HTMLCanvasElement>()
const pdfDocument = ref<any>(null)

// 图片相关
const imageUrl = ref('')

// 文本相关
const textContent = ref('')

// 预览容器引用
const previewContentRef = ref<HTMLElement>()

// 计算属性
const isPDF = computed(() => props.document?.type === 'pdf')
const isImage = computed(() => props.document?.type === 'image')
const isText = computed(() => props.document?.type === 'text')

// 监听文档变化
watch(() => props.document, (newDoc) => {
  if (newDoc && visible.value) {
    loadPreview()
  }
}, { immediate: true })

watch(visible, (newVisible) => {
  if (newVisible && props.document) {
    loadPreview()
  } else if (!newVisible) {
    resetPreview()
  }
})

// 生命周期
onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})

// 方法
const loadPreview = async () => {
  if (!props.document) return

  loading.value = true
  error.value = false
  errorMessage.value = ''

  try {
    if (isPDF.value) {
      await loadPDFPreview()
    } else if (isImage.value) {
      await loadImagePreview()
    } else if (isText.value) {
      await loadTextPreview()
    }
  } catch (err: any) {
    error.value = true
    errorMessage.value = err.message || '加载预览失败'
    console.error('Load preview failed:', err)
  } finally {
    loading.value = false
  }
}

const loadPDFPreview = async () => {
  // TODO: 实现 PDF 预览
  // 这里需要使用 PDF.js 库
  ElMessage.info('PDF 预览功能开发中...')
}

const loadImagePreview = async () => {
  if (!props.document?.url) {
    throw new Error('图片URL不存在')
  }
  
  imageUrl.value = props.document.url
}

const loadTextPreview = async () => {
  if (!props.document?.url) {
    throw new Error('文本文件URL不存在')
  }

  try {
    const response = await fetch(props.document.url)
    if (!response.ok) {
      throw new Error('获取文本内容失败')
    }
    textContent.value = await response.text()
  } catch (err) {
    throw new Error('加载文本内容失败')
  }
}

const handleImageLoad = () => {
  loading.value = false
}

const handleImageError = () => {
  error.value = true
  errorMessage.value = '图片加载失败'
  loading.value = false
}

const retryLoad = () => {
  loadPreview()
}

const resetPreview = () => {
  scale.value = 1
  rotation.value = 0
  currentPage.value = 1
  totalPages.value = 0
  imageUrl.value = ''
  textContent.value = ''
  loading.value = false
  error.value = false
  errorMessage.value = ''
  showInfo.value = false
}

// 缩放控制
const zoomIn = () => {
  if (scale.value < 3) {
    scale.value = Math.min(scale.value + 0.25, 3)
  }
}

const zoomOut = () => {
  if (scale.value > 0.5) {
    scale.value = Math.max(scale.value - 0.25, 0.5)
  }
}

const resetZoom = () => {
  scale.value = 1
}

// 旋转控制
const rotate = () => {
  rotation.value = (rotation.value + 90) % 360
}

// PDF 页面控制
const previousPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    renderPDFPage()
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    renderPDFPage()
  }
}

const renderPDFPage = async () => {
  // TODO: 实现 PDF 页面渲染
}

// 全屏控制
const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    previewContentRef.value?.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

// 下载文档
const downloadDocument = () => {
  if (!props.document?.url) {
    ElMessage.error('文档下载链接不存在')
    return
  }

  const link = document.createElement('a')
  link.href = props.document.url
  link.download = props.document.fileName
  link.click()
}

// 键盘事件处理
const handleKeydown = (event: KeyboardEvent) => {
  if (!visible.value) return

  switch (event.key) {
    case 'Escape':
      visible.value = false
      break
    case 'ArrowLeft':
      if (isPDF.value) previousPage()
      break
    case 'ArrowRight':
      if (isPDF.value) nextPage()
      break
    case '+':
    case '=':
      event.preventDefault()
      zoomIn()
      break
    case '-':
      event.preventDefault()
      zoomOut()
      break
    case '0':
      event.preventDefault()
      resetZoom()
      break
    case 'r':
    case 'R':
      event.preventDefault()
      rotate()
      break
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

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString()
}
</script>

<style scoped>
.preview-dialog {
  --el-dialog-margin-top: 5vh;
}

.preview-dialog :deep(.el-dialog) {
  height: 90vh;
  display: flex;
  flex-direction: column;
}

.preview-dialog :deep(.el-dialog__body) {
  flex: 1;
  padding: 0;
  overflow: hidden;
}

.preview-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 预览工具栏 */
.preview-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 预览内容区域 */
.preview-content {
  flex: 1;
  position: relative;
  overflow: auto;
  background: #f9f9f9;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* PDF 预览 */
.pdf-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.pdf-preview canvas {
  max-width: 100%;
  max-height: 100%;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: transform 0.3s ease;
}

/* 图片预览 */
.image-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.image-preview img {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: transform 0.3s ease;
}

/* 文本预览 */
.text-preview {
  width: 100%;
  height: 100%;
  padding: 20px;
}

.text-content {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  max-width: 800px;
  margin: 0 auto;
  transition: transform 0.3s ease;
}

.text-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'Courier New', monospace;
  line-height: 1.6;
}

.loading-text {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #909399;
  font-size: 14px;
}

/* 不支持的文件类型 */
.unsupported-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.unsupported-content {
  text-align: center;
  color: #909399;
}

.unsupported-content h3 {
  margin: 16px 0 8px 0;
  color: #606266;
}

.unsupported-content p {
  margin: 0 0 20px 0;
  font-size: 14px;
}

/* 加载和错误状态 */
.preview-loading,
.preview-error {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #909399;
  font-size: 14px;
}

.preview-error {
  color: #f56c6c;
}

/* 文档信息面板 */
.document-info-panel {
  padding: 16px;
  background: white;
  border-top: 1px solid #e4e7ed;
  max-height: 200px;
  overflow-y: auto;
}

.document-info-panel h4 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 14px;
}

.info-items {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.info-item {
  display: flex;
  font-size: 13px;
}

.info-item .label {
  color: #909399;
  margin-right: 8px;
  min-width: 80px;
}

.info-item .value {
  color: #303133;
  flex: 1;
}

/* 对话框底部 */
.dialog-footer {
  display: flex;
  justify-content: space-between;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .preview-toolbar {
    flex-direction: column;
    gap: 8px;
  }
  
  .toolbar-left,
  .toolbar-right {
    width: 100%;
    justify-content: center;
  }
  
  .text-content {
    padding: 12px;
  }
  
  .info-items {
    grid-template-columns: 1fr;
  }
}

/* 全屏模式 */
.preview-content:fullscreen {
  background: black;
}

.preview-content:fullscreen .pdf-preview canvas,
.preview-content:fullscreen .image-preview img {
  max-width: 100vw;
  max-height: 100vh;
}
</style>
