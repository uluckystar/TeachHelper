<template>
  <el-dialog
    v-model="visible"
    title="AI智能搜索结果"
    width="1000px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="search-results-container">
      <!-- 搜索统计 -->
      <div class="search-stats" v-if="results.length > 0">
        <el-tag type="info">
          找到 {{ results.length }} 个相关结果，搜索关键词：{{ currentQuery }}
        </el-tag>
      </div>

      <!-- 搜索结果列表 -->
      <div v-if="results.length > 0" class="search-results">
        <div v-for="(result, index) in results" :key="index" class="search-result-item">
          <div class="result-header">
            <div class="result-title">
              <el-tag :type="getResultTypeColor(result.type)" size="small">
                {{ getResultTypeLabel(result.type) }}
              </el-tag>
              <span class="title-text">{{ result.title || '未命名' }}</span>
              <el-tag type="warning" size="small" class="similarity-tag">
                相似度: {{ (result.similarity * 100).toFixed(1) }}%
              </el-tag>
            </div>
            <div class="result-source">
              <el-icon><Document /></el-icon>
              <span>{{ result.source || '未知来源' }}</span>
            </div>
          </div>

          <div class="result-content">
            <!-- 高亮内容展示 -->
            <div v-if="result.highlightedContent" class="highlighted-content" v-html="result.highlightedContent"></div>
            <div v-else class="original-content">{{ result.content }}</div>
            
            <!-- 显示上下文信息 -->
            <div v-if="result.contextBefore || result.contextAfter" class="content-context">
              <div v-if="result.contextBefore" class="context-before">
                <span class="context-label">前文：</span>
                <span class="context-text">{{ result.contextBefore }}</span>
              </div>
              <div v-if="result.contextAfter" class="context-after">
                <span class="context-label">后文：</span>
                <span class="context-text">{{ result.contextAfter }}</span>
              </div>
            </div>
            
            <!-- 匹配的关键词 -->
            <div v-if="result.matchedKeywords && result.matchedKeywords.length > 0" class="matched-keywords">
              <span class="keywords-label">匹配关键词：</span>
              <el-tag 
                v-for="keyword in result.matchedKeywords" 
                :key="keyword" 
                size="small" 
                type="primary"
                class="keyword-tag"
              >
                {{ keyword }}
              </el-tag>
            </div>

            <!-- 关键词位置信息（可展开） -->
            <el-collapse v-if="result.keywordPositions && result.keywordPositions.length > 0" class="keyword-positions">
              <el-collapse-item title="关键词位置信息" name="positions">
                <div class="position-list">
                  <div 
                    v-for="(position, posIndex) in result.keywordPositions" 
                    :key="posIndex" 
                    class="position-item"
                  >
                    <div class="position-header">
                      <el-tag type="success" size="small">{{ position.keyword }}</el-tag>
                      <span class="position-index">位置: {{ position.startIndex }} - {{ position.endIndex }}</span>
                    </div>
                    <div class="position-context" v-if="position.context">
                      "{{ position.context }}"
                    </div>
                  </div>
                </div>
              </el-collapse-item>
            </el-collapse>
            
            <!-- 知识库和文档信息 -->
            <div class="result-metadata" v-if="result.knowledgeBaseName || result.documentId">
              <div v-if="result.knowledgeBaseName" class="metadata-item">
                <el-icon><Collection /></el-icon>
                <span>知识库：{{ result.knowledgeBaseName }}</span>
              </div>
              <div v-if="result.chunkIndex !== undefined" class="metadata-item">
                <el-icon><Files /></el-icon>
                <span>文档块：第 {{ result.chunkIndex + 1 }} 段</span>
              </div>
            </div>
          </div>

          <div class="result-actions">
            <el-button size="small" link @click="viewResultDetail(result)" icon="View">
              查看详情
            </el-button>
            <el-button size="small" link @click="generateFromResult(result)" icon="MagicStick">
              基于此内容出题
            </el-button>
            <el-button size="small" link @click="copyResultContent(result)" icon="CopyDocument">
              复制内容
            </el-button>
          </div>
        </div>
      </div>
      
      <!-- 空状态 -->
      <div v-else class="search-empty-state">
        <el-empty description="未找到相关内容">
          <el-button type="primary" @click="$emit('reopen-search')">
            重新搜索
          </el-button>
        </el-empty>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="exportSearchResults" icon="Download">导出结果</el-button>
        <el-button type="primary" @click="handleClose">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Collection, Files } from '@element-plus/icons-vue'
import type { VectorSearchResult } from '@/types/knowledge'

interface Props {
  modelValue: boolean
  results: VectorSearchResult[]
  currentQuery: string
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'reopen-search'): void
  (e: 'view-detail', result: VectorSearchResult): void
  (e: 'generate-question', result: VectorSearchResult): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const handleClose = () => {
  visible.value = false
}

const getResultTypeColor = (type: string) => {
  switch (type) {
    case 'document': return 'primary'
    case 'knowledge_point': return 'success'
    case 'question': return 'warning'
    default: return 'info'
  }
}

const getResultTypeLabel = (type: string) => {
  switch (type) {
    case 'document': return '文档'
    case 'knowledge_point': return '知识点'
    case 'question': return '题目'
    case 'chunk': return '文档片段'
    default: return type || '未知'
  }
}

const viewResultDetail = (result: VectorSearchResult) => {
  emit('view-detail', result)
}

const generateFromResult = (result: VectorSearchResult) => {
  emit('generate-question', result)
}

const copyResultContent = async (result: VectorSearchResult) => {
  try {
    // 复制高亮内容或原始内容
    const contentToCopy = result.highlightedContent
      ? result.highlightedContent.replace(/<[^>]*>/g, '') // 移除HTML标签
      : result.content

    await navigator.clipboard.writeText(contentToCopy)
    ElMessage.success('内容已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败，请手动选择文本复制')
  }
}

const exportSearchResults = () => {
  if (props.results.length === 0) {
    ElMessage.warning('没有搜索结果可以导出')
    return
  }
  
  // 创建导出内容
  const exportData = props.results.map((result, index) => ({
    序号: index + 1,
    标题: result.title || '未命名',
    类型: getResultTypeLabel(result.type),
    相似度: (result.similarity * 100).toFixed(1) + '%',
    匹配关键词: result.matchedKeywords ? result.matchedKeywords.join(', ') : '',
    内容: result.content,
    来源: result.source || '未知来源'
  }))
  
  // 简单的CSV导出
  const csvContent = [
    Object.keys(exportData[0]).join(','),
    ...exportData.map(row => Object.values(row).map(val => `"${val}"`).join(','))
  ].join('\n')
  
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.setAttribute('href', url)
  link.setAttribute('download', `搜索结果_${props.currentQuery}_${new Date().toLocaleDateString()}.csv`)
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  
  ElMessage.success('搜索结果已导出')
}
</script>

<style scoped>
.search-results-container {
  max-height: 600px;
  overflow-y: auto;
}

.search-stats {
  margin-bottom: 16px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
}

.search-results {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-result-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.search-result-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}

.result-header {
  padding: 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #e4e7ed;
}

.result-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.title-text {
  font-weight: 600;
  font-size: 16px;
  color: #303133;
  flex: 1;
}

.similarity-tag {
  margin-left: auto;
}

.result-source {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 12px;
}

.result-content {
  padding: 16px;
}

.highlighted-content {
  line-height: 1.6;
  margin-bottom: 12px;
}

.highlighted-content :deep(mark) {
  background: linear-gradient(135deg, #ffeb3b 0%, #ffc107 100%);
  color: #333;
  padding: 2px 4px;
  border-radius: 3px;
  font-weight: 600;
  animation: highlight-pulse 2s ease-in-out;
}

@keyframes highlight-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.8; }
}

.original-content {
  color: #606266;
  line-height: 1.6;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  overflow: hidden;
}

.matched-keywords {
  margin: 12px 0;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.keywords-label {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}

.keyword-tag {
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.keyword-positions {
  margin-top: 12px;
}

.position-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.position-item {
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  border-left: 3px solid #67c23a;
}

.position-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.position-index {
  font-size: 12px;
  color: #909399;
}

.position-context {
  font-size: 12px;
  color: #666;
  font-style: italic;
  background: white;
  padding: 4px 8px;
  border-radius: 3px;
  margin-top: 4px;
}

.result-actions {
  padding: 12px 16px;
  background: #fafafa;
  border-top: 1px solid #e4e7ed;
  display: flex;
  gap: 8px;
}

.search-empty-state {
  text-align: center;
  padding: 40px 20px;
}

.dialog-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

/* 上下文内容展示 */
.content-context {
  margin-top: 12px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  border-left: 3px solid #409eff;
}

.context-before, .context-after {
  margin-bottom: 6px;
}

.context-before:last-child, .context-after:last-child {
  margin-bottom: 0;
}

.context-label {
  font-weight: 500;
  color: #409eff;
  margin-right: 6px;
}

.context-text {
  color: #606266;
  font-size: 13px;
  line-height: 1.4;
}

/* 知识库和文档信息 */
.result-metadata {
  margin-top: 12px;
  padding: 8px 12px;
  background: #f0f9ff;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.metadata-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}

.metadata-item .el-icon {
  color: #409eff;
  font-size: 14px;
}
</style>
