<template>
  <div class="generation-history">
    <div class="history-header">
      <h3>生成历史</h3>
      <div class="header-actions">
        <el-button 
          @click="refreshHistory" 
          :loading="loading"
          icon="Refresh"
          size="small"
        >
          刷新
        </el-button>
        <el-button 
          @click="clearHistory" 
          type="danger" 
          size="small"
          icon="Delete"
        >
          清空历史
        </el-button>
      </div>
    </div>

    <div class="filter-section">
      <el-form :model="filters" inline size="small">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            @change="loadHistory"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select 
            v-model="filters.status" 
            placeholder="全部状态"
            clearable
            @change="loadHistory"
          >
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
            <el-option label="生成中" value="GENERATING" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识库">
          <el-select 
            v-model="filters.knowledgeBaseId" 
            placeholder="全部知识库"
            clearable
            @change="loadHistory"
          >
            <el-option 
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <div class="history-list" v-loading="loading">
      <el-empty v-if="!loading && historyList.length === 0" description="暂无生成历史" />
      
      <div 
        v-for="item in historyList" 
        :key="item.id" 
        class="history-item"
        @click="viewDetails(item)"
      >
        <div class="item-header">
          <div class="item-title">
            <h4>{{ item.paperName || '未命名试卷' }}</h4>
            <el-tag 
              :type="getStatusType(item.status)" 
              size="small"
            >
              {{ getStatusText(item.status) }}
            </el-tag>
          </div>
          <div class="item-actions">
            <el-button 
              v-if="item.status === 'SUCCESS'"
              @click.stop="downloadPaper(item)"
              size="small"
              type="primary"
              icon="Download"
            >
              下载
            </el-button>
            <el-button 
              v-if="item.status === 'SUCCESS'"
              @click.stop="regeneratePaper(item)"
              size="small"
              icon="Refresh"
            >
              重新生成
            </el-button>
            <el-button 
              @click.stop="deleteHistoryItem(item)"
              size="small"
              type="danger"
              icon="Delete"
            >
              删除
            </el-button>
          </div>
        </div>

        <div class="item-content">
          <div class="item-info">
            <span class="info-item">
              <el-icon><Calendar /></el-icon>
              {{ formatDate(item.createdAt) }}
            </span>
            <span class="info-item">
              <el-icon><Collection /></el-icon>
              {{ item.knowledgeBaseName || '未知知识库' }}
            </span>
            <span class="info-item">
              <el-icon><Document /></el-icon>
              {{ item.totalQuestions || 0 }} 道题目
            </span>
            <span v-if="item.difficulty" class="info-item">
              <el-icon><Star /></el-icon>
              难度: {{ getDifficultyText(item.difficulty) }}
            </span>
          </div>

          <div class="item-progress" v-if="item.status === 'GENERATING'">
            <el-progress 
              :percentage="item.progress || 0" 
              :show-text="false"
              :stroke-width="4"
            />
            <span class="progress-text">{{ item.progressText || '生成中...' }}</span>
          </div>

          <div class="item-summary" v-if="item.status === 'SUCCESS' && item.summary">
            <p>{{ item.summary }}</p>
          </div>

          <div class="item-error" v-if="item.status === 'FAILED' && item.errorMessage">
            <el-alert 
              :title="item.errorMessage" 
              type="error" 
              show-icon 
              :closable="false"
            />
          </div>
        </div>
      </div>
    </div>

    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadHistory"
        @current-change="loadHistory"
      />
    </div>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="生成详情"
      width="60%"
      destroy-on-close
    >
      <div v-if="selectedItem" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="试卷名称">
            {{ selectedItem.paperName || '未命名试卷' }}
          </el-descriptions-item>
          <el-descriptions-item label="生成状态">
            <el-tag :type="getStatusType(selectedItem.status)">
              {{ getStatusText(selectedItem.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="知识库">
            {{ selectedItem.knowledgeBaseName }}
          </el-descriptions-item>
          <el-descriptions-item label="题目数量">
            {{ selectedItem.totalQuestions || 0 }} 道
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDate(selectedItem.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="完成时间" v-if="selectedItem.completedAt">
            {{ formatDate(selectedItem.completedAt) }}
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="selectedItem.questionTypes && selectedItem.questionTypes.length > 0" class="question-types">
          <h4>题型分布</h4>
          <el-tag 
            v-for="type in selectedItem.questionTypes" 
            :key="type.type"
            style="margin-right: 8px; margin-bottom: 4px;"
          >
            {{ getQuestionTypeText(type.type) }}: {{ type.count }}道
          </el-tag>
        </div>

        <div v-if="selectedItem.generationConfig" class="generation-config">
          <h4>生成配置</h4>
          <pre>{{ JSON.stringify(selectedItem.generationConfig, null, 2) }}</pre>
        </div>

        <div v-if="selectedItem.status === 'FAILED' && selectedItem.errorMessage" class="error-details">
          <h4>错误信息</h4>
          <el-alert 
            :title="selectedItem.errorMessage" 
            type="error" 
            show-icon 
            :closable="false"
          />
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button 
            v-if="selectedItem?.status === 'SUCCESS'"
            type="primary" 
            @click="downloadPaper(selectedItem)"
          >
            下载试卷
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Collection, Document, Star, Refresh, Delete, Download } from '@element-plus/icons-vue'
import { paperGenerationApi, knowledgeBaseApi } from '@/api/knowledge'

interface GenerationHistoryItem {
  id: string
  paperName?: string
  knowledgeBaseId: string
  knowledgeBaseName?: string
  status: 'GENERATING' | 'SUCCESS' | 'FAILED'
  progress?: number
  progressText?: string
  totalQuestions?: number
  difficulty?: string
  summary?: string
  errorMessage?: string
  questionTypes?: Array<{ type: string; count: number }>
  generationConfig?: any
  createdAt: string
  completedAt?: string
}

interface KnowledgeBase {
  id: string
  name: string
}

const loading = ref(false)
const historyList = ref<GenerationHistoryItem[]>([])
const knowledgeBases = ref<KnowledgeBase[]>([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const detailDialogVisible = ref(false)
const selectedItem = ref<GenerationHistoryItem | null>(null)

const filters = reactive({
  dateRange: null as any,
  status: '',
  knowledgeBaseId: ''
})

// 加载生成历史
const loadHistory = async () => {
  try {
    loading.value = true
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
      ...(filters.dateRange && {
        startDate: filters.dateRange[0],
        endDate: filters.dateRange[1]
      }),
      ...(filters.status && { status: filters.status }),
      ...(filters.knowledgeBaseId && { knowledgeBaseId: filters.knowledgeBaseId })
    }

    const response = await paperGenerationApi.getHistory(params)
    historyList.value = response.data.content || []
    total.value = response.data.totalElements || 0
  } catch (error) {
    console.error('加载生成历史失败:', error)
    ElMessage.error('加载生成历史失败')
  } finally {
    loading.value = false
  }
}

// 加载知识库列表
const loadKnowledgeBases = async () => {
  try {
    const response = await knowledgeBaseApi.getKnowledgeBases({ page: 0, size: 1000 })
    knowledgeBases.value = (response.content || []).map(kb => ({
      id: kb.id.toString(),
      name: kb.name
    }))
  } catch (error) {
    console.error('加载知识库列表失败:', error)
  }
}

// 刷新历史
const refreshHistory = () => {
  loadHistory()
}

// 清空历史
const clearHistory = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清空所有生成历史吗？此操作不可恢复。',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await paperGenerationApi.clearHistory()
    ElMessage.success('已清空生成历史')
    await loadHistory()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清空历史失败:', error)
      ElMessage.error('清空历史失败')
    }
  }
}

// 查看详情
const viewDetails = (item: GenerationHistoryItem) => {
  selectedItem.value = item
  detailDialogVisible.value = true
}

// 下载试卷
const downloadPaper = async (item: GenerationHistoryItem) => {
  try {
    const response = await paperGenerationApi.download(item.id)
    
    // 创建下载链接
    const blob = new Blob([response.data], { type: 'application/octet-stream' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${item.paperName || '试卷'}.docx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('试卷下载成功')
  } catch (error) {
    console.error('下载试卷失败:', error)
    ElMessage.error('下载试卷失败')
  }
}

// 重新生成试卷
const regeneratePaper = async (item: GenerationHistoryItem) => {
  try {
    await ElMessageBox.confirm(
      '确定要基于相同配置重新生成试卷吗？',
      '确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    await paperGenerationApi.regenerate(item.id)
    ElMessage.success('已开始重新生成试卷')
    await loadHistory()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重新生成失败:', error)
      ElMessage.error('重新生成失败')
    }
  }
}

// 删除历史记录
const deleteHistoryItem = async (item: GenerationHistoryItem) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这条生成历史吗？',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await paperGenerationApi.deleteHistory(item.id)
    ElMessage.success('删除成功')
    await loadHistory()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 获取状态类型
const getStatusType = (status: string) => {
  switch (status) {
    case 'SUCCESS': return 'success'
    case 'FAILED': return 'danger'
    case 'GENERATING': return 'warning'
    default: return 'info'
  }
}

// 获取状态文本
const getStatusText = (status: string) => {
  switch (status) {
    case 'SUCCESS': return '生成成功'
    case 'FAILED': return '生成失败'
    case 'GENERATING': return '生成中'
    default: return '未知状态'
  }
}

// 获取难度文本
const getDifficultyText = (difficulty: string) => {
  switch (difficulty) {
    case 'EASY': return '简单'
    case 'MEDIUM': return '中等'
    case 'HARD': return '困难'
    default: return difficulty
  }
}

// 获取题型文本
const getQuestionTypeText = (type: string) => {
  switch (type) {
    case 'SINGLE_CHOICE': return '单选题'
    case 'MULTIPLE_CHOICE': return '多选题'
    case 'TRUE_FALSE': return '判断题'
    case 'FILL_BLANK': return '填空题'
    case 'SHORT_ANSWER': return '简答题'
    case 'ESSAY': return '论述题'
    default: return type
  }
}

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(() => {
  loadHistory()
  loadKnowledgeBases()
})
</script>

<style scoped>
.generation-history {
  padding: 20px;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.history-header h3 {
  margin: 0;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.filter-section {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.history-list {
  min-height: 300px;
}

.history-item {
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.history-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.item-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-title h4 {
  margin: 0;
  color: #303133;
  font-size: 16px;
}

.item-actions {
  display: flex;
  gap: 8px;
}

.item-content {
  color: #606266;
}

.item-info {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 8px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
}

.item-progress {
  margin: 12px 0;
}

.progress-text {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}

.item-summary {
  margin-top: 8px;
  padding: 8px;
  background: #f0f9ff;
  border-radius: 4px;
  font-size: 14px;
  line-height: 1.4;
}

.item-error {
  margin-top: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.detail-content {
  max-height: 60vh;
  overflow-y: auto;
}

.question-types,
.generation-config,
.error-details {
  margin-top: 20px;
}

.question-types h4,
.generation-config h4,
.error-details h4 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 14px;
}

.generation-config pre {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  overflow-x: auto;
}

.dialog-footer {
  display: flex;
  gap: 10px;
}
</style>
