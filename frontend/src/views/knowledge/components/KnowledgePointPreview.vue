<template>
  <div class="knowledge-point-preview">
    <!-- 知识点基本信息 -->
    <el-card class="point-info-card">
      <template #header>
        <div class="point-header">
          <div class="point-title">
            <h3>{{ knowledgePoint?.title || '知识点详情' }}</h3>
            <div class="point-meta">
              <el-tag :type="getDifficultyTagType(knowledgePoint?.difficulty)" size="small">
                {{ getDifficultyText(knowledgePoint?.difficulty) }}
              </el-tag>
              <el-tag v-if="knowledgePoint?.isAIGenerated" type="info" size="small">
                AI生成
              </el-tag>
            </div>
          </div>
          <div class="point-actions">
            <el-button size="small" type="primary" @click="editKnowledgePoint">
              编辑
            </el-button>
            <el-button size="small" @click="generateQuestions">
              生成题目
            </el-button>
          </div>
        </div>
      </template>
      
      <div class="point-content">
        <div v-if="knowledgePoint?.description" class="point-description">
          <h4>描述</h4>
          <p>{{ knowledgePoint.description }}</p>
        </div>
        
        <div v-if="knowledgePoint?.summary" class="point-summary">
          <h4>摘要</h4>
          <p>{{ knowledgePoint.summary }}</p>
        </div>
        
        <div v-if="knowledgePoint?.tags && knowledgePoint.tags.length > 0" class="point-tags">
          <h4>标签</h4>
          <div class="tags-list">
            <el-tag
              v-for="tag in knowledgePoint.tags"
              :key="tag"
              size="small"
              style="margin-right: 8px; margin-bottom: 4px;"
            >
              {{ tag }}
            </el-tag>
          </div>
        </div>
        
        <div class="point-metadata">
          <el-descriptions :column="3" size="small">
            <el-descriptions-item label="创建时间">
              {{ formatDate(knowledgePoint?.createdAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="更新时间">
              {{ formatDate(knowledgePoint?.updatedAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="所属知识库">
              {{ knowledgeBaseName }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-card>

    <!-- 相关内容 -->
    <el-card class="related-content-card" v-loading="loadingRelatedContent">
      <template #header>
        <div class="section-header">
          <span>相关内容</span>
          <el-button size="small" @click="refreshRelatedContent">
            刷新
          </el-button>
        </div>
      </template>
      
      <div v-if="relatedContent.length === 0" class="empty-content">
        <el-empty description="暂无相关内容" />
      </div>
      
      <div v-else class="related-content-list">
        <div
          v-for="(content, index) in relatedContent"
          :key="content.documentId || index"
          class="content-item"
        >
          <div class="content-header">
            <div class="content-meta">
              <el-tag size="small">{{ getContentTypeText(content.metadata?.type || 'document') }}</el-tag>
              <span class="similarity-score">相似度: {{ Math.round(content.similarity * 100) }}%</span>
            </div>
            <div class="content-source">{{ content.source }}</div>
          </div>
          <div class="content-text">{{ content.content }}</div>
        </div>
      </div>
    </el-card>

    <!-- 统计信息 -->
    <el-card class="statistics-card">
      <template #header>
        <span>统计信息</span>
      </template>
      
      <div class="statistics-grid">
        <div class="stat-item">
          <div class="stat-value">{{ statistics.documentCount || 0 }}</div>
          <div class="stat-label">关联文档</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ statistics.questionCount || 0 }}</div>
          <div class="stat-label">相关题目</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ statistics.relatedPointsCount || 0 }}</div>
          <div class="stat-label">关联知识点</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ Math.round((statistics.averageSimilarity || 0) * 100) }}%</div>
          <div class="stat-label">平均相似度</div>
        </div>
      </div>
    </el-card>

    <!-- 操作历史 -->
    <el-card class="history-card" v-if="showHistory">
      <template #header>
        <div class="section-header">
          <span>操作历史</span>
          <el-switch v-model="showHistory" size="small" />
        </div>
      </template>
      
      <el-timeline>
        <el-timeline-item
          v-for="(record, index) in operationHistory"
          :key="index"
          :timestamp="formatDate(record.timestamp)"
          placement="top"
        >
          <div class="history-item">
            <div class="operation-type">{{ record.operation }}</div>
            <div class="operation-description">{{ record.description }}</div>
            <div v-if="record.details" class="operation-details">
              {{ record.details }}
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  knowledgePointApi,
  type KnowledgePointResponse,
  type RetrievedDocument 
} from '@/api/knowledge'

interface Props {
  knowledgePointId?: number | null
  knowledgeBaseName?: string
}

interface Emits {
  (e: 'edit', knowledgePoint: KnowledgePointResponse): void
  (e: 'generate-questions', knowledgePoint: KnowledgePointResponse): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const knowledgePoint = ref<KnowledgePointResponse | null>(null)
const relatedContent = ref<RetrievedDocument[]>([])
const loadingRelatedContent = ref(false)
const showHistory = ref(false)

// 统计信息
const statistics = reactive({
  documentCount: 0,
  questionCount: 0,
  relatedPointsCount: 0,
  averageSimilarity: 0
})

// 操作历史
const operationHistory = ref([
  {
    operation: '创建知识点',
    description: '从文档中提取并创建了此知识点',
    timestamp: new Date().toISOString(),
    details: '来源: 数学课件.pdf'
  },
  {
    operation: 'AI增强',
    description: 'AI为知识点生成了摘要和标签',
    timestamp: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
    details: '添加了3个标签，生成了120字摘要'
  }
])

// 监听知识点ID变化
watch(() => props.knowledgePointId, (newId) => {
  if (newId) {
    loadKnowledgePoint(newId)
  } else {
    knowledgePoint.value = null
    relatedContent.value = []
  }
}, { immediate: true })

// 加载知识点详情
const loadKnowledgePoint = async (id: number) => {
  try {
    knowledgePoint.value = await knowledgePointApi.getKnowledgePoint(id)
    
    // 同时加载相关内容
    await loadRelatedContent(id)
    
  } catch (error) {
    console.error('加载知识点失败:', error)
    ElMessage.error('加载知识点失败')
  }
}

// 加载相关内容
const loadRelatedContent = async (id: number) => {
  try {
    loadingRelatedContent.value = true
    
    const response = await knowledgePointApi.getRelatedContent(id, { limit: 10 })
    relatedContent.value = response
    
    // 计算统计信息
    calculateStatistics()
    
  } catch (error) {
    console.error('加载相关内容失败:', error)
  } finally {
    loadingRelatedContent.value = false
  }
}

// 计算统计信息
const calculateStatistics = () => {
  if (relatedContent.value.length === 0) return
  
  const contentTypes = relatedContent.value.reduce((acc, content) => {
    acc[content.metadata?.type || 'unknown'] = (acc[content.metadata?.type || 'unknown'] || 0) + 1
    return acc
  }, {} as Record<string, number>)
  
  statistics.documentCount = contentTypes.document || 0
  statistics.questionCount = contentTypes.question || 0
  statistics.relatedPointsCount = contentTypes.knowledge_point || 0
  statistics.averageSimilarity = relatedContent.value.reduce((sum, content) => sum + content.similarity, 0) / relatedContent.value.length
}

// 辅助函数
const getDifficultyTagType = (difficulty?: string) => {
  switch (difficulty) {
    case 'EASY': return 'success'
    case 'MEDIUM': return 'warning'
    case 'HARD': return 'danger'
    default: return 'info'
  }
}

const getDifficultyText = (difficulty?: string) => {
  switch (difficulty) {
    case 'EASY': return '简单'
    case 'MEDIUM': return '中等'
    case 'HARD': return '困难'
    default: return '未知'
  }
}

const getContentTypeText = (type?: string) => {
  switch (type) {
    case 'document': return '文档'
    case 'question': return '题目'
    case 'knowledge_point': return '知识点'
    default: return '内容'
  }
}

const formatDate = (dateString?: string) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

// 操作方法
const editKnowledgePoint = () => {
  if (knowledgePoint.value) {
    emit('edit', knowledgePoint.value)
  }
}

const generateQuestions = () => {
  if (knowledgePoint.value) {
    emit('generate-questions', knowledgePoint.value)
  }
}

const refreshRelatedContent = async () => {
  if (props.knowledgePointId) {
    await loadRelatedContent(props.knowledgePointId)
    ElMessage.success('相关内容已刷新')
  }
}
</script>

<style scoped>
.knowledge-point-preview {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
}

.point-info-card,
.related-content-card,
.statistics-card,
.history-card {
  border: 1px solid #e4e7ed;
}

.point-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.point-title {
  flex: 1;
}

.point-title h3 {
  margin: 0 0 8px 0;
  color: #303133;
}

.point-meta {
  display: flex;
  gap: 8px;
  align-items: center;
}

.point-actions {
  display: flex;
  gap: 8px;
}

.point-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.point-description,
.point-summary {
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
}

.point-description h4,
.point-summary h4,
.point-tags h4 {
  margin: 0 0 8px 0;
  color: #606266;
  font-size: 14px;
  font-weight: 600;
}

.point-description p,
.point-summary p {
  margin: 0;
  line-height: 1.6;
  color: #303133;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.empty-content {
  padding: 40px 0;
}

.related-content-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.content-item {
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fafafa;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.content-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.similarity-score {
  font-size: 12px;
  color: #909399;
}

.content-source {
  font-size: 12px;
  color: #606266;
}

.content-text {
  font-size: 14px;
  line-height: 1.5;
  color: #303133;
  max-height: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.statistics-card {
  min-height: 120px;
}

.statistics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-item {
  text-align: center;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #606266;
}

.history-item {
  padding: 8px 0;
}

.operation-type {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.operation-description {
  font-size: 14px;
  color: #606266;
  margin-bottom: 2px;
}

.operation-details {
  font-size: 12px;
  color: #909399;
}

@media (max-width: 768px) {
  .point-header {
    flex-direction: column;
    gap: 12px;
  }
  
  .statistics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
