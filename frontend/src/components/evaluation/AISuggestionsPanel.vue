<template>
  <div class="ai-suggestions-panel">
    <el-card class="suggestions-container">
      <template #header>
        <div class="panel-header">
          <div class="header-title">
            <el-icon color="#409eff"><MagicStick /></el-icon>
            <span>AI 评分标准建议</span>
            <el-tag type="primary" size="small">{{ suggestions.length }} 个建议</el-tag>
          </div>
          <div class="header-actions">
            <el-button 
              type="success" 
              :icon="Check" 
              @click="applyAllSuggestions"
              :disabled="suggestions.length === 0"
              size="small"
            >
              应用全部
            </el-button>
            <el-button 
              type="danger" 
              :icon="Delete" 
              @click="clearAllSuggestions"
              :disabled="suggestions.length === 0"
              size="small"
            >
              清空建议
            </el-button>
            <el-button 
              :icon="Refresh" 
              @click="handleRefresh"
              :loading="refreshing"
              size="small"
            >
              重新生成
            </el-button>
          </div>
        </div>
      </template>

      <!-- 统计信息 -->
      <div v-if="suggestions.length > 0" class="suggestions-stats">
        <div class="stat-item">
          <span class="stat-label">建议总分:</span>
          <span class="stat-value" :class="{ 'warning': totalSuggestedScore > questionMaxScore }">
            {{ totalSuggestedScore }} / {{ questionMaxScore }} 分
          </span>
        </div>
        <div class="stat-item">
          <span class="stat-label">平均分值:</span>
          <span class="stat-value">{{ averageScore.toFixed(1) }} 分</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">兼容性:</span>
          <el-tag :type="isCompatible ? 'success' : 'warning'" size="small">
            {{ isCompatible ? '可直接应用' : '需要调整' }}
          </el-tag>
        </div>
      </div>

      <!-- AI建议列表 -->
      <div v-if="suggestions.length === 0" class="empty-suggestions">
        <el-empty description="暂无AI建议">
          <el-button type="primary" @click="handleRefresh" :loading="refreshing">
            生成AI建议
          </el-button>
        </el-empty>
      </div>

      <div v-else class="suggestions-list">
        <transition-group name="suggestion" tag="div" class="suggestions-grid">
          <div 
            v-for="(suggestion, index) in suggestions" 
            :key="suggestion.id || index"
            class="suggestion-wrapper"
          >
            <AISuggestionCard
              :suggestion="suggestion"
              :question-max-score="questionMaxScore"
              :current-total-score="currentTotalScore"
              :generated-at="suggestion.generatedAt"
              @preview="handlePreviewSuggestion"
              @apply="handleApplySuggestion"
              @edit-apply="handleEditApplySuggestion"
              @reject="handleRejectSuggestion(index)"
            />
          </div>
        </transition-group>
      </div>

      <!-- 批量操作面板 -->
      <div v-if="suggestions.length > 1" class="batch-operations">
        <el-divider>批量操作</el-divider>
        <div class="batch-controls">
          <div class="batch-info">
            <el-checkbox 
              v-model="selectAll" 
              @change="handleSelectAllChange"
              :indeterminate="isIndeterminate"
            >
              全选 ({{ selectedSuggestions.length }}/{{ suggestions.length }})
            </el-checkbox>
          </div>
          <div class="batch-actions">
            <el-button 
              type="primary" 
              :disabled="selectedSuggestions.length === 0"
              @click="applySelectedSuggestions"
            >
              应用选中 ({{ selectedSuggestions.length }})
            </el-button>
            <el-button 
              type="warning" 
              :disabled="selectedSuggestions.length === 0"
              @click="previewSelectedSuggestions"
            >
              预览选中
            </el-button>
            <el-button 
              type="danger" 
              :disabled="selectedSuggestions.length === 0"
              @click="removeSelectedSuggestions"
            >
              删除选中
            </el-button>
          </div>
        </div>
        
        <!-- 选择列表 -->
        <div v-if="suggestions.length > 0" class="selection-list">
          <el-checkbox-group v-model="selectedSuggestions">
            <div 
              v-for="(suggestion, index) in suggestions" 
              :key="index"
              class="selection-item"
            >
              <el-checkbox :value="index" class="selection-checkbox">
                <div class="selection-content">
                  <span class="suggestion-name">{{ suggestion.criterionText }}</span>
                  <span class="suggestion-score">{{ suggestion.points }}分</span>
                </div>
              </el-checkbox>
            </div>
          </el-checkbox-group>
        </div>
      </div>
    </el-card>

    <!-- 批量预览对话框 -->
    <el-dialog
      v-model="showBatchPreview"
      title="批量应用预览"
      width="900px"
    >
      <div class="batch-preview-content">
        <el-alert
          :title="`即将应用 ${selectedSuggestions.length} 个AI建议`"
          type="info"
          :closable="false"
          show-icon
        />

        <div class="preview-sections">
          <!-- 当前状态 -->
          <div class="preview-section">
            <h3>当前评分标准</h3>
            <div v-if="currentCriteria.length === 0" class="empty-preview">
              <el-text type="info">暂无评分标准</el-text>
            </div>
            <div v-else class="criteria-preview-list">
              <div 
                v-for="(criterion, index) in currentCriteria" 
                :key="index"
                class="criterion-preview-item"
              >
                <span class="criterion-name">{{ criterion.criterionText }}</span>
                <span class="criterion-score">{{ criterion.maxScore }}分</span>
              </div>
            </div>
            <div class="section-total">
              当前总分: {{ currentTotalScore }} 分
            </div>
          </div>

          <!-- 预览状态 -->
          <div class="preview-section">
            <h3>应用后预览</h3>
            <div class="criteria-preview-list">
              <!-- 保留的现有标准 -->
              <div 
                v-for="(criterion, index) in (applyMode === 'append' ? currentCriteria : [])" 
                :key="`current-${index}`"
                class="criterion-preview-item"
              >
                <span class="criterion-name">{{ criterion.criterionText }}</span>
                <span class="criterion-score">{{ criterion.maxScore }}分</span>
              </div>
              
              <!-- 新增的AI建议 -->
              <div 
                v-for="(suggestion, index) in selectedSuggestionsData" 
                :key="`new-${index}`"
                class="criterion-preview-item new-item"
              >
                <span class="criterion-name">{{ suggestion.criterionText }}</span>
                <span class="criterion-score">{{ suggestion.points }}分</span>
                <el-tag type="success" size="small">新增</el-tag>
              </div>
            </div>
            <div class="section-total" :class="{ 'warning': previewTotalScore > questionMaxScore }">
              预览总分: {{ previewTotalScore }} 分
              <el-tag v-if="previewTotalScore !== currentTotalScore" type="primary" size="small">
                {{ previewTotalScore > currentTotalScore ? '+' : '' }}{{ previewTotalScore - currentTotalScore }}
              </el-tag>
            </div>
          </div>
        </div>

        <!-- 应用模式选择 -->
        <el-card class="apply-mode-card" shadow="never">
          <template #header>
            <h3>应用模式</h3>
          </template>
          <el-radio-group v-model="applyMode" class="mode-selection">
            <el-radio value="append" class="mode-option">
              <div class="mode-content">
                <div class="mode-title">追加模式</div>
                <div class="mode-desc">保留现有评分标准，将AI建议添加为新标准</div>
              </div>
            </el-radio>
            <el-radio value="replace" class="mode-option">
              <div class="mode-content">
                <div class="mode-title">替换模式</div>
                <div class="mode-desc">清除现有评分标准，仅使用选中的AI建议</div>
              </div>
            </el-radio>
          </el-radio-group>
        </el-card>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showBatchPreview = false">取消</el-button>
          <el-button type="primary" @click="confirmBatchApply">
            确认应用 ({{ selectedSuggestions.length }} 个建议)
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  MagicStick, Check, Delete, Refresh 
} from '@element-plus/icons-vue'
import AISuggestionCard from './AISuggestionCard.vue'
import type { RubricSuggestionItem, RubricCriterion } from '@/types/api'

// Props
interface Props {
  suggestions: RubricSuggestionItem[]
  questionMaxScore?: number
  currentCriteria?: RubricCriterion[]
  refreshing?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  questionMaxScore: 100,
  currentCriteria: () => [],
  refreshing: false
})

// Emits
const emit = defineEmits<{
  'apply-suggestion': [suggestion: RubricSuggestionItem]
  'apply-multiple': [suggestions: RubricSuggestionItem[], mode: 'append' | 'replace']
  'preview-suggestion': [suggestion: RubricSuggestionItem]
  'remove-suggestion': [index: number]
  'refresh': []
  'clear-all': []
}>()

// 响应式数据
const selectedSuggestions = ref<number[]>([])
const selectAll = ref(false)
const showBatchPreview = ref(false)
const applyMode = ref<'append' | 'replace'>('append')

// 计算属性
const totalSuggestedScore = computed(() => {
  return props.suggestions.reduce((sum, suggestion) => sum + suggestion.points, 0)
})

const averageScore = computed(() => {
  if (props.suggestions.length === 0) return 0
  return totalSuggestedScore.value / props.suggestions.length
})

const currentTotalScore = computed(() => {
  return props.currentCriteria.reduce((sum, criterion) => sum + (criterion.maxScore || 0), 0)
})

const isCompatible = computed(() => {
  return (currentTotalScore.value + totalSuggestedScore.value) <= props.questionMaxScore
})

const isIndeterminate = computed(() => {
  const selected = selectedSuggestions.value.length
  return selected > 0 && selected < props.suggestions.length
})

const selectedSuggestionsData = computed(() => {
  return selectedSuggestions.value.map(index => props.suggestions[index])
})

const previewTotalScore = computed(() => {
  const selectedScore = selectedSuggestionsData.value.reduce((sum, s) => sum + s.points, 0)
  if (applyMode.value === 'replace') {
    return selectedScore
  } else {
    return currentTotalScore.value + selectedScore
  }
})

// 监听选择变化
watch(selectedSuggestions, (newVal) => {
  selectAll.value = newVal.length === props.suggestions.length
}, { deep: true })

watch(() => props.suggestions.length, () => {
  selectedSuggestions.value = []
  selectAll.value = false
})

// 方法
const handleSelectAllChange = (checked: any) => {
  if (checked) {
    selectedSuggestions.value = props.suggestions.map((_, index) => index)
  } else {
    selectedSuggestions.value = []
  }
}

const handlePreviewSuggestion = (suggestion: RubricSuggestionItem) => {
  emit('preview-suggestion', suggestion)
}

const handleApplySuggestion = (suggestion: RubricSuggestionItem) => {
  emit('apply-suggestion', suggestion)
}

const handleEditApplySuggestion = (suggestion: RubricSuggestionItem) => {
  emit('apply-suggestion', suggestion)
}

const handleRejectSuggestion = (index: number) => {
  emit('remove-suggestion', index)
  
  // 更新选择状态
  const selectedIndex = selectedSuggestions.value.indexOf(index)
  if (selectedIndex > -1) {
    selectedSuggestions.value.splice(selectedIndex, 1)
  }
  
  // 重新映射索引
  selectedSuggestions.value = selectedSuggestions.value
    .map(i => i > index ? i - 1 : i)
    .filter(i => i >= 0)
}

const handleRefresh = () => {
  emit('refresh')
}

const applyAllSuggestions = async () => {
  if (props.suggestions.length === 0) return

  const totalScore = currentTotalScore.value + totalSuggestedScore.value
  
  if (totalScore > props.questionMaxScore) {
    const result = await ElMessageBox.confirm(
      `应用所有建议后总分将为 ${totalScore} 分，超过题目满分 ${props.questionMaxScore} 分。是否继续？`,
      '确认应用',
      {
        confirmButtonText: '继续应用',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).catch(() => false)
    
    if (!result) return
  }

  emit('apply-multiple', props.suggestions, 'append')
}

const clearAllSuggestions = async () => {
  const result = await ElMessageBox.confirm(
    '确定要清空所有AI建议吗？此操作不可恢复。',
    '确认清空',
    {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).catch(() => false)
  
  if (result) {
    emit('clear-all')
    selectedSuggestions.value = []
  }
}

const applySelectedSuggestions = () => {
  if (selectedSuggestions.value.length === 0) return
  
  showBatchPreview.value = true
}

const previewSelectedSuggestions = () => {
  if (selectedSuggestions.value.length === 0) return
  
  showBatchPreview.value = true
}

const removeSelectedSuggestions = async () => {
  if (selectedSuggestions.value.length === 0) return

  const result = await ElMessageBox.confirm(
    `确定要删除选中的 ${selectedSuggestions.value.length} 个建议吗？`,
    '确认删除',
    {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).catch(() => false)
  
  if (result) {
    // 从后往前删除，避免索引错乱
    const sortedIndexes = [...selectedSuggestions.value].sort((a, b) => b - a)
    sortedIndexes.forEach(index => {
      emit('remove-suggestion', index)
    })
    selectedSuggestions.value = []
  }
}

const confirmBatchApply = () => {
  const selectedData = selectedSuggestionsData.value
  emit('apply-multiple', selectedData, applyMode.value)
  showBatchPreview.value = false
  selectedSuggestions.value = []
}
</script>

<style scoped>
.ai-suggestions-panel {
  width: 100%;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.suggestions-stats {
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 16px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-radius: 8px;
  margin-bottom: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-label {
  font-size: 14px;
  color: #606266;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.stat-value.warning {
  color: #e6a23c;
}

.empty-suggestions {
  text-align: center;
  padding: 40px 0;
}

.suggestions-list {
  margin-bottom: 20px;
}

.suggestions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 16px;
}

.suggestion-wrapper {
  transition: all 0.3s ease;
}

.suggestion-enter-active,
.suggestion-leave-active {
  transition: all 0.3s ease;
}

.suggestion-enter-from,
.suggestion-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

.batch-operations {
  border-top: 1px solid #e4e7ed;
  padding-top: 16px;
}

.batch-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.batch-actions {
  display: flex;
  gap: 8px;
}

.selection-list {
  max-height: 200px;
  overflow-y: auto;
  padding: 8px;
  background: #fafafa;
  border-radius: 6px;
}

.selection-item {
  margin-bottom: 8px;
}

.selection-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.suggestion-name {
  flex: 1;
  font-weight: 500;
}

.suggestion-score {
  color: #409eff;
  font-weight: 600;
}

/* 批量预览样式 */
.batch-preview-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.preview-sections {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.preview-section {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 16px;
}

.preview-section h3 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 16px;
}

.empty-preview {
  text-align: center;
  padding: 20px;
  color: #909399;
}

.criteria-preview-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.criterion-preview-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.criterion-preview-item.new-item {
  background: #f0f9ff;
  border-left: 3px solid #67c23a;
}

.criterion-name {
  font-weight: 500;
  color: #303133;
}

.criterion-score {
  color: #409eff;
  font-weight: 600;
}

.section-total {
  padding: 8px;
  text-align: center;
  background: #409eff;
  color: white;
  border-radius: 4px;
  font-weight: 600;
}

.section-total.warning {
  background: #e6a23c;
}

.apply-mode-card {
  background: #fafafa;
}

.mode-selection {
  width: 100%;
}

.mode-option {
  display: block;
  width: 100%;
  margin: 0 0 12px 0;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.mode-option:hover {
  border-color: #409eff;
  background: #f0f9ff;
}

.mode-content {
  margin-left: 20px;
}

.mode-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.mode-desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.4;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
