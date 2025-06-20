<template>
  <div class="document-processing-progress">
    <el-card>
      <template #header>
        <div class="progress-header">
          <el-icon><DataAnalysis /></el-icon>
          <span>文档处理进度</span>
          <el-button size="small" @click="refreshProgress" :loading="refreshing">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <!-- 总体进度 -->
      <div class="overall-progress">
        <h4>总体处理进度</h4>
        <el-progress 
          :percentage="overallProgress" 
          :status="overallStatus"
          :stroke-width="8"
        />
        <div class="progress-stats">
          <span>已处理：{{ stats.completedDocuments }}/{{ stats.totalDocuments }}</span>
          <span>失败：{{ stats.failedDocuments }}</span>
        </div>
      </div>

      <!-- 文档列表 -->
      <div class="documents-list">
        <h4>文档处理详情</h4>
        <el-table :data="processingDocuments" v-loading="loading">
          <el-table-column prop="fileName" label="文件名" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="processingProgress" label="进度" width="200">
            <template #default="{ row }">
              <el-progress 
                v-if="row.status === 'PROCESSING'"
                :percentage="row.processingProgress || 0" 
                :stroke-width="6"
                :show-text="false"
              />
              <span v-else-if="row.status === 'COMPLETED'" class="progress-text">
                ✅ 完成
              </span>
              <span v-else-if="row.status === 'FAILED'" class="progress-text error">
                ❌ {{ row.processingError || '处理失败' }}
              </span>
              <span v-else class="progress-text">
                ⏳ 等待中...
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button 
                v-if="row.status === 'FAILED'"
                size="small" 
                type="primary"
                @click="retryProcessing(row.id)"
              >
                重试
              </el-button>
              <el-button 
                v-else-if="row.status === 'COMPLETED'"
                size="small"
                type="success"
                @click="viewDocument(row.id)"
              >
                查看
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 处理统计 -->
      <div class="processing-stats">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-statistic title="总文档数" :value="stats.totalDocuments" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="处理中" :value="stats.processingDocuments" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="已完成" :value="stats.completedDocuments" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="失败" :value="stats.failedDocuments" />
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { DataAnalysis, Refresh } from '@element-plus/icons-vue'
import { knowledgeBaseApi, documentApi } from '@/api/knowledge'

// Props
interface Props {
  knowledgeBaseId?: number
  autoRefresh?: boolean
  refreshInterval?: number
}

const props = withDefaults(defineProps<Props>(), {
  autoRefresh: true,
  refreshInterval: 3000 // 3秒刷新一次
})

// 响应式数据
const loading = ref(false)
const refreshing = ref(false)
const processingDocuments = ref<any[]>([])
const stats = reactive({
  totalDocuments: 0,
  pendingDocuments: 0,
  processingDocuments: 0,
  completedDocuments: 0,
  failedDocuments: 0,
  totalFileSize: 0,
  averageProcessingTime: 0
})

let refreshTimer: ReturnType<typeof setTimeout> | null = null

// 计算属性
const overallProgress = computed(() => {
  if (stats.totalDocuments === 0) return 0
  return Math.round((stats.completedDocuments + stats.failedDocuments) / stats.totalDocuments * 100)
})

const overallStatus = computed(() => {
  if (stats.failedDocuments > 0) return 'warning'
  if (stats.processingDocuments > 0) return undefined
  return 'success'
})

// 方法
const loadProcessingProgress = async () => {
  try {
    loading.value = true
    
    // 暂时注释掉统计信息获取，因为API不存在
    // const statsData = await knowledgeBaseApi.getProcessingStatistics(props.knowledgeBaseId)
    // Object.assign(stats, statsData)
    
    if (props.knowledgeBaseId) {
      // 使用存在的API方法获取文档列表
      const response = await documentApi.getDocuments(props.knowledgeBaseId)
      const documentsData = response.data
      processingDocuments.value = documentsData.filter((doc: any) => 
        ['PENDING', 'PROCESSING', 'FAILED'].includes(doc.processingStatus) ||
        (doc.processingStatus === 'COMPLETED' && isRecentlyProcessed(doc))
      )
    }
    
  } catch (error) {
    console.error('加载处理进度失败:', error)
    ElMessage.error('加载处理进度失败')
  } finally {
    loading.value = false
  }
}

const refreshProgress = async () => {
  refreshing.value = true
  try {
    await loadProcessingProgress()
  } finally {
    refreshing.value = false
  }
}

const retryProcessing = async (documentId: number) => {
  try {
    await documentApi.reprocessDocument(documentId)
    ElMessage.success('重新处理已开始')
    await refreshProgress()
  } catch (error) {
    console.error('重新处理失败:', error)
    ElMessage.error('重新处理失败')
  }
}

const viewDocument = (documentId: number) => {
  // 跳转到文档详情页
  window.open(`/knowledge/documents/${documentId}`, '_blank')
}

const getStatusType = (status: string) => {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'PROCESSING': return 'warning'
    case 'FAILED': return 'danger'
    case 'PENDING': return 'info'
    default: return 'info'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'PENDING': return '等待处理'
    case 'PROCESSING': return '处理中'
    case 'COMPLETED': return '已完成'
    case 'FAILED': return '处理失败'
    default: return status
  }
}

const isRecentlyProcessed = (doc: any) => {
  if (!doc.updatedAt) return false
  const updatedTime = new Date(doc.updatedAt).getTime()
  const now = Date.now()
  return (now - updatedTime) < 5 * 60 * 1000 // 5分钟内
}

const startAutoRefresh = () => {
  if (props.autoRefresh && !refreshTimer) {
    refreshTimer = setInterval(() => {
      if (!refreshing.value) {
        loadProcessingProgress()
      }
    }, props.refreshInterval)
  }
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

// 生命周期
onMounted(() => {
  loadProcessingProgress()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})

// 暴露方法给父组件
defineExpose({
  refresh: refreshProgress,
  startAutoRefresh,
  stopAutoRefresh
})
</script>

<style scoped>
.document-processing-progress {
  width: 100%;
}

.progress-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.progress-header > span {
  font-weight: 600;
  margin-left: 8px;
}

.overall-progress {
  margin-bottom: 24px;
}

.overall-progress h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #303133;
}

.progress-stats {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 14px;
  color: #666;
}

.documents-list {
  margin-bottom: 24px;
}

.documents-list h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #303133;
}

.progress-text {
  font-size: 14px;
}

.progress-text.error {
  color: #f56c6c;
}

.processing-stats {
  border-top: 1px solid #ebeef5;
  padding-top: 20px;
}
</style>
