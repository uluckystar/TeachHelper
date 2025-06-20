<template>
  <el-dialog
    v-model="visible"
    :title="`知识点详情 - ${knowledgePoint?.title || ''}`"
    width="800px"
    :close-on-click-modal="false"
  >
    <div class="knowledge-point-detail" v-if="knowledgePoint">
      <!-- 基本信息 -->
      <el-card class="info-card" shadow="never">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span>基本信息</span>
            <div>
              <el-button size="small" icon="Edit" @click="editKnowledgePoint">编辑</el-button>
              <el-button size="small" icon="Share" @click="manageRelations">管理关联</el-button>
            </div>
          </div>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="标题">
            <div style="display: flex; align-items: center; gap: 8px;">
              <span>{{ knowledgePoint.title }}</span>
              <el-icon v-if="knowledgePoint.isAIGenerated" color="#409eff" title="AI生成">
                <MagicStick />
              </el-icon>
            </div>
          </el-descriptions-item>
          
          <el-descriptions-item label="分类">
            <el-tag :type="getCategoryTagType(knowledgePoint.category)">
              {{ knowledgePoint.category || '未分类' }}
            </el-tag>
          </el-descriptions-item>
          
          <el-descriptions-item label="难度级别">
            <el-tag :type="getDifficultyTagType(knowledgePoint.difficulty)">
              {{ getDifficultyText(knowledgePoint.difficulty) }}
            </el-tag>
          </el-descriptions-item>
          
          <el-descriptions-item label="重要程度">
            <el-rate
              :model-value="knowledgePoint.importance || 3"
              disabled
              show-score
              text-color="#ff9900"
            />
          </el-descriptions-item>
          
          <el-descriptions-item label="掌握程度">
            <div style="display: flex; align-items: center; gap: 8px;">
              <el-progress
                :percentage="knowledgePoint.masteryLevel || 0"
                :stroke-width="8"
                :show-text="false"
                style="width: 100px;"
              />
              <span>{{ knowledgePoint.masteryLevel || 0 }}%</span>
            </div>
          </el-descriptions-item>
          
          <el-descriptions-item label="学习时长">
            {{ knowledgePoint.estimatedTime || 30 }} 分钟
          </el-descriptions-item>
          
          <el-descriptions-item label="创建时间" :span="2">
            {{ formatDate(knowledgePoint.createdAt) }}
          </el-descriptions-item>
          
          <el-descriptions-item label="更新时间" :span="2">
            {{ formatDate(knowledgePoint.updatedAt) }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 详细描述 -->
      <el-card class="description-card" shadow="never">
        <template #header>
          <span>详细描述</span>
        </template>
        
        <div class="description-content">
          <div class="description-text">
            {{ knowledgePoint.description || '暂无描述' }}
          </div>
        </div>
      </el-card>

      <!-- 关键词标签 -->
      <el-card class="keywords-card" shadow="never" v-if="knowledgePoint.keywords && knowledgePoint.keywords.length > 0">
        <template #header>
          <span>关键词标签</span>
        </template>
        
        <div class="keywords-content">
          <el-tag
            v-for="keyword in knowledgePoint.keywords"
            :key="keyword"
            class="keyword-tag"
            type="info"
          >
            {{ keyword }}
          </el-tag>
        </div>
      </el-card>

      <!-- 关联知识点 -->
      <el-card class="relations-card" shadow="never" v-if="knowledgePoint.relations && knowledgePoint.relations.length > 0">
        <template #header>
          <span>关联知识点</span>
        </template>
        
        <div class="relations-content">
          <div
            v-for="relation in knowledgePoint.relations"
            :key="relation.id"
            class="relation-item"
          >
            <div class="relation-info">
              <span class="relation-title">{{ relation.title }}</span>
              <el-tag
                :type="getRelationTagType(relation.type)"
                size="small"
                class="relation-type"
              >
                {{ getRelationTypeText(relation.type) }}
              </el-tag>
            </div>
            <div class="relation-actions">
              <el-button size="small" link @click="viewRelatedPoint(relation)">查看</el-button>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 学习统计 -->
      <el-card class="stats-card" shadow="never">
        <template #header>
          <span>学习统计</span>
        </template>
        
        <div class="stats-content">
          <div class="stat-grid">
            <div class="stat-item">
              <div class="stat-label">练习次数</div>
              <div class="stat-value">{{ knowledgePoint.practiceCount || 0 }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">正确率</div>
              <div class="stat-value">{{ knowledgePoint.accuracyRate || 0 }}%</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">平均用时</div>
              <div class="stat-value">{{ knowledgePoint.averageTime || 0 }}分钟</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">错误次数</div>
              <div class="stat-value">{{ knowledgePoint.errorCount || 0 }}</div>
            </div>
          </div>
          
          <!-- 学习趋势图 -->
          <div class="learning-trend" v-if="learningData.length > 0">
            <h4>学习趋势</h4>
            <div class="trend-chart">
              <!-- 这里可以集成Chart.js或ECharts -->
              <div class="chart-placeholder">
                <el-icon size="48" color="#c0c4cc">
                  <TrendCharts />
                </el-icon>
                <div>学习趋势图</div>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 相关文档 -->
      <el-card class="documents-card" shadow="never" v-if="relatedDocuments.length > 0">
        <template #header>
          <span>相关文档</span>
        </template>
        
        <div class="documents-content">
          <div
            v-for="doc in relatedDocuments"
            :key="doc.id"
            class="document-item"
          >
            <div class="document-info">
              <el-icon class="document-icon">
                <Document />
              </el-icon>
              <div class="document-details">
                <div class="document-title">{{ doc.title }}</div>
                <div class="document-meta">
                  <span>{{ doc.type }}</span>
                  <span>{{ formatFileSize(doc.size) }}</span>
                  <span>{{ formatDate(doc.createdAt) }}</span>
                </div>
              </div>
            </div>
            <div class="document-actions">
              <el-button size="small" link @click="previewDocument(doc)">预览</el-button>
              <el-button size="small" link @click="downloadDocument(doc)">下载</el-button>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 生成的题目 -->
      <el-card class="questions-card" shadow="never" v-if="relatedQuestions.length > 0">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span>生成的题目 ({{ relatedQuestions.length }})</span>
            <el-button size="small" @click="generateMoreQuestions">生成更多</el-button>
          </div>
        </template>
        
        <div class="questions-content">
          <div
            v-for="question in relatedQuestions"
            :key="question.id"
            class="question-item"
          >
            <div class="question-header">
              <el-tag :type="getQuestionTypeTag(question.type)" size="small">
                {{ getQuestionTypeText(question.type) }}
              </el-tag>
              <el-tag :type="getDifficultyTagType(question.difficulty)" size="small">
                {{ question.difficulty }}
              </el-tag>
              <span class="question-score">{{ question.score }}分</span>
            </div>
            <div class="question-content">
              {{ question.content.substring(0, 100) }}{{ question.content.length > 100 ? '...' : '' }}
            </div>
            <div class="question-actions">
              <el-button size="small" link @click="viewQuestion(question)">查看</el-button>
              <el-button size="small" link @click="editQuestion(question)">编辑</el-button>
              <el-button size="small" link @click="practiceQuestion(question)">练习</el-button>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 备注信息 -->
      <el-card class="notes-card" shadow="never" v-if="knowledgePoint.notes">
        <template #header>
          <span>备注信息</span>
        </template>
        
        <div class="notes-content">
          {{ knowledgePoint.notes }}
        </div>
      </el-card>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">关闭</el-button>
        <el-button type="primary" @click="startPractice">开始练习</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  MagicStick,
  Edit,
  Share,
  Document,
  TrendCharts
} from '@element-plus/icons-vue'

// Props
const props = defineProps<{
  modelValue: boolean
  knowledgePoint: any
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

const learningData = ref<any[]>([])
const relatedDocuments = ref<any[]>([])
const relatedQuestions = ref<any[]>([])

// 监听知识点变化
watch(() => props.knowledgePoint, (newPoint) => {
  if (newPoint && visible.value) {
    loadRelatedData()
  }
}, { immediate: true })

// 生命周期
onMounted(() => {
  if (props.knowledgePoint) {
    loadRelatedData()
  }
})

// 方法
const loadRelatedData = async () => {
  try {
    // TODO: 从API加载相关数据
    // 模拟数据
    relatedDocuments.value = [
      {
        id: 1,
        title: '函数概念详解.pdf',
        type: 'PDF',
        size: 2048576,
        createdAt: '2024-01-15T10:30:00Z'
      },
      {
        id: 2,
        title: '函数例题集.docx',
        type: 'Word',
        size: 1024000,
        createdAt: '2024-01-16T14:20:00Z'
      }
    ]
    
    relatedQuestions.value = [
      {
        id: 1,
        type: 'choice',
        difficulty: 'MEDIUM',
        content: '下列关于函数定义的描述中，正确的是？',
        score: 5
      },
      {
        id: 2,
        type: 'blank',
        difficulty: 'EASY',
        content: '函数f(x) = x² 的定义域是____。',
        score: 3
      }
    ]
    
    learningData.value = [
      { date: '2024-01-10', score: 60 },
      { date: '2024-01-12', score: 75 },
      { date: '2024-01-15', score: 85 }
    ]
  } catch (error) {
    console.error('Load related data failed:', error)
  }
}

const editKnowledgePoint = () => {
  ElMessage.info('编辑功能开发中...')
}

const manageRelations = () => {
  ElMessage.info('关联管理功能开发中...')
}

const viewRelatedPoint = (relation: any) => {
  ElMessage.info(`查看关联知识点: ${relation.title}`)
}

const previewDocument = (doc: any) => {
  ElMessage.info(`预览文档: ${doc.title}`)
}

const downloadDocument = (doc: any) => {
  ElMessage.info(`下载文档: ${doc.title}`)
}

const generateMoreQuestions = () => {
  ElMessage.info('生成更多题目功能开发中...')
}

const viewQuestion = (question: any) => {
  ElMessage.info(`查看题目: ${question.content}`)
}

const editQuestion = (question: any) => {
  ElMessage.info(`编辑题目: ${question.content}`)
}

const practiceQuestion = (question: any) => {
  ElMessage.info(`练习题目: ${question.content}`)
}

const startPractice = () => {
  ElMessage.info('开始练习功能开发中...')
}

// 工具方法
const getCategoryTagType = (category: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    '概念定义': 'primary',
    '原理公式': 'success',
    '解题方法': 'warning',
    '实例分析': 'info',
    '综合应用': 'danger'
  }
  return typeMap[category] || 'info'
}

const getDifficultyTagType = (difficulty: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    'EASY': 'success',
    'MEDIUM': 'warning',
    'HARD': 'danger'
  }
  return typeMap[difficulty] || 'info'
}

const getDifficultyText = (difficulty: string) => {
  const textMap: Record<string, string> = {
    'EASY': '简单',
    'MEDIUM': '中等',
    'HARD': '困难'
  }
  return textMap[difficulty] || difficulty
}

const getRelationTagType = (type: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    'prerequisite': 'primary',
    'related': 'success',
    'extension': 'warning'
  }
  return typeMap[type] || 'info'
}

const getRelationTypeText = (type: string) => {
  const textMap: Record<string, string> = {
    'prerequisite': '前置知识',
    'related': '相关知识',
    'extension': '扩展知识'
  }
  return textMap[type] || type
}

const getQuestionTypeTag = (type: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    'choice': 'primary',
    'blank': 'success',
    'subjective': 'warning',
    'calculation': 'info'
  }
  return typeMap[type] || 'info'
}

const getQuestionTypeText = (type: string) => {
  const textMap: Record<string, string> = {
    'choice': '选择题',
    'blank': '填空题',
    'subjective': '主观题',
    'calculation': '计算题'
  }
  return textMap[type] || type
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}
</script>

<style scoped>
.knowledge-point-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-card,
.description-card,
.keywords-card,
.relations-card,
.stats-card,
.documents-card,
.questions-card,
.notes-card {
  border: 1px solid #e4e7ed;
}

.description-content {
  padding: 4px 0;
}

.description-text {
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
}

.keywords-content {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.keyword-tag {
  margin: 2px;
}

.relations-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.relation-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fafafa;
}

.relation-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.relation-title {
  font-weight: 500;
  color: #303133;
}

.relation-type {
  margin-left: 8px;
}

.stats-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 16px;
}

.stat-item {
  text-align: center;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #409eff;
}

.learning-trend h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #606266;
}

.trend-chart {
  height: 200px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fafafa;
}

.chart-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #c0c4cc;
  font-size: 14px;
}

.documents-content,
.questions-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.document-item,
.question-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fafafa;
}

.document-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.document-icon {
  color: #409eff;
  font-size: 24px;
}

.document-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.document-title {
  font-weight: 500;
  color: #303133;
}

.document-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.question-score {
  font-size: 12px;
  color: #909399;
}

.question-content {
  flex: 1;
  color: #606266;
  line-height: 1.6;
  margin-right: 12px;
}

.question-actions,
.document-actions {
  display: flex;
  gap: 8px;
}

.notes-content {
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
  background: #f5f7fa;
  padding: 12px;
  border-radius: 6px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
