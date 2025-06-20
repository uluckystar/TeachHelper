<template>
  <div class="ai-suggestion-card">
    <el-card class="suggestion-card" shadow="hover">
      <template #header>
        <div class="suggestion-header">
          <div class="header-info">
            <el-icon color="#409eff"><MagicStick /></el-icon>
            <span class="ai-label">AI 建议</span>
            <el-tag type="primary" size="small">{{ suggestion.points }}分</el-tag>
          </div>
          <div class="header-actions">
            <el-button 
              type="primary" 
              size="small"
              @click="handlePreview"
            >
              预览应用
            </el-button>
            <el-button 
              type="success" 
              size="small"
              @click="handleQuickApply"
            >
              快速应用
            </el-button>
          </div>
        </div>
      </template>

      <div class="suggestion-content">
        <!-- 标准名称 -->
        <div class="content-section">
          <h4 class="section-title">
            <el-icon><Document /></el-icon>
            评分标准
          </h4>
          <div class="criterion-text">{{ suggestion.criterionText }}</div>
        </div>

        <!-- 建议描述（如果有） -->
        <div v-if="suggestion.description" class="content-section">
          <h4 class="section-title">
            <el-icon><EditPen /></el-icon>
            详细说明
          </h4>
          <div class="criterion-description">{{ suggestion.description }}</div>
        </div>

        <!-- 分数信息 -->
        <div class="content-section">
          <h4 class="section-title">
            <el-icon><Trophy /></el-icon>
            分数配置
          </h4>
          <div class="score-info">
            <div class="score-item">
              <span class="label">建议分数:</span>
              <span class="value primary">{{ suggestion.points }} 分</span>
            </div>
            <div v-if="questionMaxScore" class="score-item">
              <span class="label">题目满分:</span>
              <span class="value">{{ questionMaxScore }} 分</span>
            </div>
            <div class="score-item">
              <span class="label">分数占比:</span>
              <span class="value" :class="getPercentageClass()">
                {{ getPercentage() }}%
              </span>
            </div>
          </div>
        </div>

        <!-- 应用预览 -->
        <div class="content-section">
          <h4 class="section-title">
            <el-icon><View /></el-icon>
            应用效果预览
          </h4>
          <div class="preview-section">
            <div class="preview-item">
              <el-icon><Plus /></el-icon>
              <span>新增评分标准: {{ suggestion.criterionText }}</span>
            </div>
            <div class="preview-item">
              <el-icon><Edit /></el-icon>
              <span>分数变化: +{{ suggestion.points }}分</span>
            </div>
            <div v-if="showCompatibilityWarning" class="preview-item warning">
              <el-icon><Warning /></el-icon>
              <span>注意: 应用后总分将超过题目满分，建议调整</span>
            </div>
          </div>
        </div>

        <!-- 智能提示 -->
        <div v-if="smartTips.length > 0" class="content-section">
          <h4 class="section-title">
            <el-icon><InfoFilled /></el-icon>
            智能提示
          </h4>
          <div class="tips-list">
            <div 
              v-for="(tip, index) in smartTips" 
              :key="index"
              class="tip-item"
              :class="tip.type"
            >
              <el-icon>
                <InfoFilled v-if="tip.type === 'info'" />
                <Warning v-else-if="tip.type === 'warning'" />
                <SuccessFilled v-else />
              </el-icon>
              <span>{{ tip.message }}</span>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="suggestion-footer">
          <div class="footer-info">
            <el-text type="info" size="small">
              <el-icon><Clock /></el-icon>
              AI生成时间: {{ formatTime(generatedAt) }}
            </el-text>
          </div>
          <div class="footer-actions">
            <el-button size="small" @click="handleEdit">
              <el-icon><Edit /></el-icon>
              编辑后应用
            </el-button>
            <el-button type="danger" size="small" @click="handleReject">
              <el-icon><Close /></el-icon>
              拒绝建议
            </el-button>
          </div>
        </div>
      </template>
    </el-card>

    <!-- 编辑建议对话框 -->
    <el-dialog
      v-model="showEditDialog"
      title="编辑AI建议"
      width="600px"
      @close="resetEditForm"
    >
      <el-form :model="editForm" label-width="100px" ref="editFormRef">
        <el-form-item label="标准名称" required>
          <el-input 
            v-model="editForm.criterionText" 
            placeholder="请输入评分标准名称"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="分数" required>
          <el-input-number 
            v-model="editForm.points" 
            :min="1" 
            :max="questionMaxScore || 100"
            :precision="1"
            style="width: 200px"
          />
          <span class="form-tip">分</span>
        </el-form-item>
        
        <el-form-item label="详细描述">
          <el-input 
            v-model="editForm.description" 
            type="textarea" 
            :rows="3"
            placeholder="请输入评分标准的详细描述和评判要求"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="权重分配">
          <el-input-number 
            v-model="editForm.weight" 
            :min="0" 
            :max="100"
            :precision="1"
            style="width: 200px"
          />
          <span class="form-tip">%</span>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="resetEditForm">取消</el-button>
          <el-button type="primary" @click="applyEditedSuggestion">
            应用修改后的建议
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  MagicStick, Document, EditPen, Trophy, View, Plus, Edit, Warning,
  InfoFilled, SuccessFilled, Clock, Close
} from '@element-plus/icons-vue'
import type { RubricSuggestionItem } from '@/types/api'

// Props
interface Props {
  suggestion: RubricSuggestionItem
  questionMaxScore?: number
  currentTotalScore?: number
  generatedAt?: string
}

const props = withDefaults(defineProps<Props>(), {
  questionMaxScore: 100,
  currentTotalScore: 0,
  generatedAt: () => new Date().toISOString()
})

// Emits
const emit = defineEmits<{
  'preview': [suggestion: RubricSuggestionItem]
  'apply': [suggestion: RubricSuggestionItem]
  'edit-apply': [suggestion: RubricSuggestionItem]
  'reject': [suggestion: RubricSuggestionItem]
}>()

// 响应式数据
const showEditDialog = ref(false)
const editFormRef = ref()
const editForm = ref({
  criterionText: '',
  points: 0,
  description: '',
  weight: 0
})

// 计算属性
const getPercentage = () => {
  if (!props.questionMaxScore) return 0
  return Math.round((props.suggestion.points / props.questionMaxScore) * 100)
}

const getPercentageClass = () => {
  const percentage = getPercentage()
  if (percentage > 50) return 'high'
  if (percentage > 25) return 'medium'
  return 'low'
}

const showCompatibilityWarning = computed(() => {
  return (props.currentTotalScore + props.suggestion.points) > props.questionMaxScore
})

const smartTips = computed(() => {
  const tips = []
  
  // 分数占比提示
  const percentage = getPercentage()
  if (percentage > 70) {
    tips.push({
      type: 'warning',
      message: `此标准分数较高(${percentage}%)，请确认是否为主要评分要点`
    })
  } else if (percentage < 10) {
    tips.push({
      type: 'info',
      message: `此标准分数较低(${percentage}%)，可考虑与其他标准合并`
    })
  }
  
  // 总分超限提示
  if (showCompatibilityWarning.value) {
    tips.push({
      type: 'warning',
      message: '应用此建议将导致总分超过题目满分，建议调整现有标准分数'
    })
  }
  
  // 标准质量提示
  if (props.suggestion.criterionText.length < 10) {
    tips.push({
      type: 'info',
      message: '建议标准名称可以更加详细和具体'
    })
  }
  
  // 积极提示
  if (!showCompatibilityWarning.value && percentage >= 10 && percentage <= 50) {
    tips.push({
      type: 'success',
      message: '此建议的分数配置合理，可以直接应用'
    })
  }
  
  return tips
})

// 方法
const handlePreview = () => {
  emit('preview', props.suggestion)
}

const handleQuickApply = () => {
  if (showCompatibilityWarning.value) {
    ElMessageBox.confirm(
      '应用此建议将导致总分超过题目满分，是否继续？',
      '确认应用',
      {
        confirmButtonText: '继续应用',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(() => {
      emit('apply', props.suggestion)
    }).catch(() => {
      // 用户取消
    })
  } else {
    emit('apply', props.suggestion)
  }
}

const handleEdit = () => {
  editForm.value = {
    criterionText: props.suggestion.criterionText,
    points: props.suggestion.points,
    description: props.suggestion.description || '',
    weight: 0
  }
  showEditDialog.value = true
}

const handleReject = () => {
  ElMessageBox.confirm(
    '确定要拒绝这个AI建议吗？',
    '确认拒绝',
    {
      confirmButtonText: '拒绝',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    emit('reject', props.suggestion)
    ElMessage.success('已拒绝AI建议')
  }).catch(() => {
    // 用户取消
  })
}

const resetEditForm = () => {
  showEditDialog.value = false
  editForm.value = {
    criterionText: '',
    points: 0,
    description: '',
    weight: 0
  }
}

const applyEditedSuggestion = () => {
  if (!editForm.value.criterionText || editForm.value.points <= 0) {
    ElMessage.warning('请填写完整的标准名称和分数')
    return
  }
  
  const editedSuggestion: RubricSuggestionItem = {
    ...props.suggestion,
    criterionText: editForm.value.criterionText,
    points: editForm.value.points,
    description: editForm.value.description
  }
  
  emit('edit-apply', editedSuggestion)
  resetEditForm()
}

const formatTime = (timeString: string) => {
  return new Date(timeString).toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.ai-suggestion-card {
  width: 100%;
}

.suggestion-card {
  border: 2px solid transparent;
  transition: all 0.3s ease;
  background: linear-gradient(135deg, #f8f9ff 0%, #e6f7ff 100%);
}

.suggestion-card:hover {
  border-color: #409eff;
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(64, 158, 255, 0.15);
}

.suggestion-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-label {
  font-weight: 600;
  color: #409eff;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.suggestion-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.content-section {
  padding: 12px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 6px;
  border-left: 3px solid #409eff;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
}

.criterion-text {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  line-height: 1.5;
}

.criterion-description {
  color: #606266;
  line-height: 1.5;
}

.score-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.score-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.score-item .label {
  color: #606266;
  font-size: 14px;
}

.score-item .value {
  font-weight: 600;
}

.score-item .value.primary {
  color: #409eff;
  font-size: 16px;
}

.score-item .value.high {
  color: #e6a23c;
}

.score-item .value.medium {
  color: #409eff;
}

.score-item .value.low {
  color: #909399;
}

.preview-section {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.preview-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #606266;
}

.preview-item.warning {
  color: #e6a23c;
}

.preview-item .el-icon {
  font-size: 16px;
}

.tips-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tip-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 8px;
  border-radius: 4px;
  font-size: 13px;
  line-height: 1.4;
}

.tip-item.info {
  background: #e6f7ff;
  color: #409eff;
}

.tip-item.warning {
  background: #fff7e6;
  color: #e6a23c;
}

.tip-item.success {
  background: #f0f9ff;
  color: #67c23a;
}

.suggestion-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
}

.footer-info {
  display: flex;
  align-items: center;
  gap: 4px;
}

.footer-actions {
  display: flex;
  gap: 8px;
}

.form-tip {
  margin-left: 8px;
  color: #909399;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
