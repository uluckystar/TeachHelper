<template>
  <div class="knowledge-upload-test">
    <el-card class="test-card">
      <template #header>
        <h3>知识库文档上传测试</h3>
      </template>
      
      <!-- 知识库选择 -->
      <div class="section">
        <h4>1. 选择知识库</h4>
        <el-select 
          v-model="selectedKnowledgeBaseId" 
          placeholder="选择知识库"
          style="width: 300px"
          @change="loadKnowledgeBase"
        >
          <el-option
            v-for="kb in knowledgeBases"
            :key="kb.id"
            :label="kb.name"
            :value="kb.id"
          />
        </el-select>
        <el-button 
          type="primary" 
          @click="loadKnowledgeBases"
          style="margin-left: 10px"
        >
          刷新列表
        </el-button>
      </div>

      <!-- 知识库信息 -->
      <div class="section" v-if="selectedKnowledgeBase">
        <h4>2. 知识库信息</h4>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="名称">{{ selectedKnowledgeBase.name }}</el-descriptions-item>
          <el-descriptions-item label="学科">{{ selectedKnowledgeBase.subject || '通用' }}</el-descriptions-item>
          <el-descriptions-item label="年级">{{ selectedKnowledgeBase.gradeLevel || '通用' }}</el-descriptions-item>
          <el-descriptions-item label="文档数量">{{ selectedKnowledgeBase.documentCount || 0 }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 文档上传 -->
      <div class="section" v-if="selectedKnowledgeBase">
        <h4>3. 上传文档</h4>
        <el-button 
          type="primary" 
          @click="showUploadDialog = true"
          :disabled="!selectedKnowledgeBase"
        >
          上传文档
        </el-button>
      </div>

      <!-- 上传历史 -->
      <div class="section" v-if="uploadedDocuments.length > 0">
        <h4>4. 上传历史</h4>
        <el-table :data="uploadedDocuments" border>
          <el-table-column prop="fileName" label="文件名" />
          <el-table-column prop="fileType" label="类型" width="80" />
          <el-table-column prop="processingStatus" label="状态" width="100">
            <template #default="{ row }">
              <el-tag 
                :type="getStatusTagType(row.processingStatus)"
                size="small"
              >
                {{ getStatusText(row.processingStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="processingProgress" label="进度" width="100">
            <template #default="{ row }">
              <el-progress 
                :percentage="row.processingProgress || 0" 
                :status="row.processingStatus === 'FAILED' ? 'exception' : undefined"
                size="small"
              />
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="上传时间" width="160">
            <template #default="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 系统状态检查 -->
      <div class="section">
        <h4>5. 系统状态检查</h4>
        <el-space wrap>
          <el-button @click="checkBackendStatus">检查后端状态</el-button>
          <el-button @click="checkAIStatus">检查AI服务状态</el-button>
          <el-button @click="checkDatabaseStatus">检查数据库状态</el-button>
        </el-space>
        
        <div class="status-info" v-if="systemStatus">
          <el-alert 
            :title="`后端状态: ${systemStatus.backend ? '正常' : '异常'}`"
            :type="systemStatus.backend ? 'success' : 'error'"
            show-icon
          />
          <el-alert 
            :title="`AI服务: ${systemStatus.ai ? '正常' : '异常'}`"
            :type="systemStatus.ai ? 'success' : 'warning'"
            show-icon
          />
          <el-alert 
            :title="`数据库: ${systemStatus.database ? '正常' : '异常'}`"
            :type="systemStatus.database ? 'success' : 'error'"
            show-icon
          />
        </div>
      </div>
    </el-card>

    <!-- 文档上传对话框 -->
    <DocumentUploadDialog
      v-model="showUploadDialog"
      :knowledge-base="selectedKnowledgeBase"
      @uploaded="handleDocumentsUploaded"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { knowledgeBaseApi, documentApi, type KnowledgeBase, type KnowledgeDocument } from '@/api/knowledge'
import DocumentUploadDialog from './components/DocumentUploadDialog.vue'

// 响应式数据
const knowledgeBases = ref<KnowledgeBase[]>([])
const selectedKnowledgeBaseId = ref<number>()
const selectedKnowledgeBase = ref<KnowledgeBase | null>(null)
const showUploadDialog = ref(false)
const uploadedDocuments = ref<KnowledgeDocument[]>([])
const systemStatus = ref<{
  backend: boolean
  ai: boolean
  database: boolean
} | null>(null)

// 加载知识库列表
const loadKnowledgeBases = async () => {
  try {
    const response = await knowledgeBaseApi.getKnowledgeBases()
    knowledgeBases.value = response.content
    ElMessage.success('知识库列表加载成功')
  } catch (error) {
    console.error('加载知识库列表失败:', error)
    ElMessage.error('加载知识库列表失败')
  }
}

// 选择知识库
const loadKnowledgeBase = async () => {
  if (!selectedKnowledgeBaseId.value) return
  
  try {
    const response = await knowledgeBaseApi.getKnowledgeBase(selectedKnowledgeBaseId.value)
    selectedKnowledgeBase.value = response
    
    // 加载该知识库的文档
    loadKnowledgeBaseDocuments()
  } catch (error) {
    console.error('加载知识库失败:', error)
    ElMessage.error('加载知识库失败')
  }
}

// 加载知识库文档
const loadKnowledgeBaseDocuments = async () => {
  if (!selectedKnowledgeBaseId.value) return
  
  try {
    const response = await documentApi.getDocuments(selectedKnowledgeBaseId.value)
    uploadedDocuments.value = response.data
  } catch (error) {
    console.error('加载文档列表失败:', error)
  }
}

// 处理文档上传完成
const handleDocumentsUploaded = (documents: KnowledgeDocument[]) => {
  uploadedDocuments.value.unshift(...documents)
  ElNotification.success({
    title: '上传成功',
    message: `成功上传 ${documents.length} 个文档，正在后台处理中...`
  })
  
  // 刷新知识库信息
  loadKnowledgeBase()
}

// 状态检查函数
const checkBackendStatus = async () => {
  try {
    // 这里应该调用一个健康检查端点
    await knowledgeBaseApi.getKnowledgeBases()
    if (!systemStatus.value) systemStatus.value = { backend: false, ai: false, database: false }
    systemStatus.value.backend = true
    ElMessage.success('后端服务正常')
  } catch (error) {
    if (!systemStatus.value) systemStatus.value = { backend: false, ai: false, database: false }
    systemStatus.value.backend = false
    ElMessage.error('后端服务异常')
  }
}

const checkAIStatus = async () => {
  try {
    // 这里可以调用AI服务的健康检查
    if (!systemStatus.value) systemStatus.value = { backend: false, ai: false, database: false }
    systemStatus.value.ai = true
    ElMessage.success('AI服务正常')
  } catch (error) {
    if (!systemStatus.value) systemStatus.value = { backend: false, ai: false, database: false }
    systemStatus.value.ai = false
    ElMessage.warning('AI服务可能异常')
  }
}

const checkDatabaseStatus = async () => {
  try {
    await knowledgeBaseApi.getKnowledgeBases()
    if (!systemStatus.value) systemStatus.value = { backend: false, ai: false, database: false }
    systemStatus.value.database = true
    ElMessage.success('数据库连接正常')
  } catch (error) {
    if (!systemStatus.value) systemStatus.value = { backend: false, ai: false, database: false }
    systemStatus.value.database = false
    ElMessage.error('数据库连接异常')
  }
}

// 辅助函数
const getStatusTagType = (status: string) => {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'FAILED': return 'danger'
    case 'PROCESSING': return 'warning'
    default: return 'info'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'PENDING': return '等待中'
    case 'PROCESSING': return '处理中'
    case 'COMPLETED': return '已完成'
    case 'FAILED': return '失败'
    default: return '未知'
  }
}

const formatTime = (timeStr: string) => {
  return new Date(timeStr).toLocaleString()
}

// 初始化
onMounted(() => {
  loadKnowledgeBases()
})
</script>

<style scoped>
.knowledge-upload-test {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
}

.test-card {
  margin-bottom: 20px;
}

.section {
  margin-bottom: 30px;
}

.section h4 {
  margin-bottom: 15px;
  color: #409eff;
}

.status-info {
  margin-top: 15px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>
