<template>
  <div class="enhanced-rubric-editor">
    <!-- 评分标准编辑器 -->
    <el-card class="editor-card">
      <template #header>
        <div class="editor-header">
          <div class="header-title">
            <el-icon><Edit /></el-icon>
            <span>评分标准编辑器</span>
          </div>
          <div class="header-actions">
            <el-button 
              type="primary" 
              :icon="Plus" 
              @click="addCriterion"
              size="small"
            >
              添加标准
            </el-button>
            <el-button 
              type="success" 
              :icon="View" 
              @click="showPreview = true"
              :disabled="criteria.length === 0"
              size="small"
            >
              预览
            </el-button>
          </div>
        </div>
      </template>

      <div v-if="criteria.length === 0" class="empty-state">
        <el-empty description="暂无评分标准">
          <el-button type="primary" @click="addCriterion">添加第一个标准</el-button>
        </el-empty>
      </div>

      <div v-else class="criteria-editor">
        <transition-group name="criterion" tag="div">
          <div 
            v-for="(criterion, index) in criteria" 
            :key="criterion.tempId || criterion.id"
            class="criterion-editor-item"
          >
            <el-card class="criterion-card" shadow="hover">
              <template #header>
                <div class="criterion-header">
                  <div class="criterion-title">
                    <el-tag :type="getTypeColor(index)" size="small">标准 {{ index + 1 }}</el-tag>
                    <span class="criterion-status">
                      <el-icon v-if="criterion.isNew" color="#67c23a"><Plus /></el-icon>
                      <el-icon v-else-if="criterion.isModified" color="#e6a23c"><Edit /></el-icon>
                      <el-icon v-else color="#909399"><Check /></el-icon>
                    </span>
                  </div>
                  <div class="criterion-actions">
                    <el-button 
                      type="danger" 
                      size="small" 
                      text
                      @click="removeCriterion(index)"
                    >
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </div>
              </template>

              <el-form :model="criterion" label-width="100px" class="criterion-form">
                <el-row :gutter="16">
                  <el-col :span="12">
                    <el-form-item label="标准名称" required>
                      <el-input 
                        v-model="criterion.criterionText" 
                        placeholder="请输入评分标准名称"
                        @input="markAsModified(criterion)"
                        maxlength="100"
                        show-word-limit
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="6">
                    <el-form-item label="分数" required>
                      <el-input-number 
                        v-model="criterion.maxScore" 
                        :min="0" 
                        :max="questionMaxScore || 100"
                        :precision="1"
                        @change="markAsModified(criterion)"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="6">
                    <el-form-item label="权重">
                      <el-input-number 
                        v-model="criterion.weight" 
                        :min="0" 
                        :max="100"
                        :precision="1"
                        @change="markAsModified(criterion)"
                        style="width: 100%"
                      />
                      <span class="weight-unit">%</span>
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-form-item label="详细描述">
                  <el-input 
                    v-model="criterion.description" 
                    type="textarea" 
                    :rows="3"
                    placeholder="请详细描述此评分标准的评判要求和评分细则"
                    @input="markAsModified(criterion)"
                    maxlength="500"
                    show-word-limit
                  />
                </el-form-item>

                <!-- 评分等级定义 -->
                <el-form-item label="评分等级">
                  <div class="score-levels-editor">
                    <div v-if="!criterion.scoreLevels || criterion.scoreLevels.length === 0" class="no-levels">
                      <el-text type="info">暂未设置评分等级</el-text>
                      <el-button 
                        type="primary" 
                        size="small" 
                        text
                        @click="addScoreLevel(criterion)"
                      >
                        添加等级
                      </el-button>
                    </div>
                    
                    <div v-else class="levels-list">
                      <div 
                        v-for="(level, levelIndex) in criterion.scoreLevels" 
                        :key="levelIndex"
                        class="level-item"
                      >
                        <el-input 
                          v-model="level.name" 
                          placeholder="等级名称"
                          @input="markAsModified(criterion)"
                          style="width: 100px;"
                        />
                        <el-input-number 
                          v-model="level.score" 
                          :min="0" 
                          :max="criterion.maxScore || 100"
                          :precision="1"
                          @change="markAsModified(criterion)"
                          style="width: 100px;"
                        />
                        <el-input 
                          v-model="level.description" 
                          placeholder="等级描述"
                          @input="markAsModified(criterion)"
                          style="flex: 1; margin: 0 8px;"
                        />
                        <el-button 
                          type="danger" 
                          size="small" 
                          text
                          @click="removeScoreLevel(criterion, levelIndex)"
                        >
                          <el-icon><Delete /></el-icon>
                        </el-button>
                      </div>
                      <el-button 
                        type="primary" 
                        size="small" 
                        text
                        @click="addScoreLevel(criterion)"
                      >
                        <el-icon><Plus /></el-icon>
                        添加等级
                      </el-button>
                    </div>
                  </div>
                </el-form-item>
              </el-form>
            </el-card>
          </div>
        </transition-group>

        <!-- 统计信息 -->
        <el-card class="summary-card" shadow="never">
          <div class="summary-content">
            <div class="summary-item">
              <span class="label">标准数量:</span>
              <span class="value">{{ criteria.length }} 个</span>
            </div>
            <div class="summary-item">
              <span class="label">总分:</span>
              <span class="value" :class="{ 'warning': totalScore !== questionMaxScore }">
                {{ totalScore }} / {{ questionMaxScore }} 分
              </span>
            </div>
            <div class="summary-item">
              <span class="label">总权重:</span>
              <span class="value" :class="{ 'warning': totalWeight !== 100 }">
                {{ totalWeight }}%
              </span>
            </div>
            <div class="summary-item">
              <span class="label">状态:</span>
              <el-tag :type="isValid ? 'success' : 'warning'" size="small">
                {{ isValid ? '配置正确' : '需要调整' }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </div>
    </el-card>

    <!-- AI建议预览面板 -->
    <el-dialog
      v-model="showAISuggestionPreview"
      title="AI建议预览"
      width="800px"
      @close="closeAISuggestionPreview"
    >
      <div v-if="currentAISuggestion" class="ai-suggestion-preview">
        <!-- 建议概览 -->
        <el-card class="suggestion-overview" shadow="never">
          <template #header>
            <h3>AI 建议概览</h3>
          </template>
          <el-descriptions :column="2" size="large">
            <el-descriptions-item label="建议标准">
              {{ currentAISuggestion.criterionText }}
            </el-descriptions-item>
            <el-descriptions-item label="建议分数">
              {{ currentAISuggestion.points }} 分
            </el-descriptions-item>
            <el-descriptions-item label="应用方式">
              <el-tag type="primary">{{ applyMode === 'replace' ? '替换现有' : '追加添加' }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 应用前后对比 -->
        <el-card class="comparison-card" shadow="never">
          <template #header>
            <h3>应用前后对比</h3>
          </template>
          <el-row :gutter="20">
            <!-- 当前状态 -->
            <el-col :span="12">
              <div class="comparison-section">
                <h4 class="section-title">
                  <el-icon><Document /></el-icon>
                  当前评分标准
                </h4>
                <div v-if="criteria.length === 0" class="empty-state-small">
                  <el-text type="info">暂无评分标准</el-text>
                </div>
                <div v-else class="criteria-list-small">
                  <div 
                    v-for="(criterion, index) in criteria" 
                    :key="index"
                    class="criterion-item-small"
                  >
                    <div class="criterion-content">
                      <span class="criterion-name">{{ criterion.criterionText }}</span>
                      <span class="criterion-score">{{ criterion.maxScore }}分</span>
                    </div>
                  </div>
                  <div class="total-score">
                    总分: {{ totalScore }} 分
                  </div>
                </div>
              </div>
            </el-col>

            <!-- 应用后状态 -->
            <el-col :span="12">
              <div class="comparison-section">
                <h4 class="section-title">
                  <el-icon><MagicStick /></el-icon>
                  应用后预览
                </h4>
                <div class="criteria-list-small">
                  <div 
                    v-for="(criterion, index) in previewCriteria" 
                    :key="index"
                    class="criterion-item-small"
                    :class="{ 'new-criterion': criterion.isNew }"
                  >
                    <div class="criterion-content">
                      <span class="criterion-name">{{ criterion.criterionText }}</span>
                      <span class="criterion-score">{{ criterion.maxScore }}分</span>
                    </div>
                    <el-tag v-if="criterion.isNew" type="success" size="small">新增</el-tag>
                  </div>
                  <div class="total-score" :class="{ 'changed': previewTotalScore !== totalScore }">
                    总分: {{ previewTotalScore }} 分
                    <el-tag v-if="previewTotalScore !== totalScore" type="warning" size="small">
                      {{ previewTotalScore > totalScore ? '+' : '' }}{{ previewTotalScore - totalScore }}
                    </el-tag>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <!-- 应用选项 -->
        <el-card class="apply-options" shadow="never">
          <template #header>
            <h3>应用选项</h3>
          </template>
          <el-radio-group v-model="applyMode" class="apply-mode-group">
            <el-radio value="append" class="apply-mode-option">
              <div class="option-content">
                <div class="option-title">追加添加</div>
                <div class="option-desc">将AI建议作为新的评分标准添加到现有标准之后</div>
              </div>
            </el-radio>
            <el-radio value="replace" class="apply-mode-option">
              <div class="option-content">
                <div class="option-title">替换现有</div>
                <div class="option-desc">清除现有评分标准，仅保留AI建议的标准</div>
              </div>
            </el-radio>
          </el-radio-group>

          <!-- 高级选项 -->
          <el-divider>高级选项</el-divider>
          <el-form label-width="120px">
            <el-form-item label="自动调整分数">
              <el-switch 
                v-model="autoAdjustScore"
                active-text="自动调整AI建议的分数以匹配题目满分"
              />
            </el-form-item>
            <el-form-item label="权重分配">
              <el-switch 
                v-model="autoAssignWeight"
                active-text="自动为评分标准分配权重"
              />
            </el-form-item>
          </el-form>
        </el-card>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeAISuggestionPreview">取消</el-button>
          <el-button type="primary" @click="applyAISuggestionWithPreview">
            确认应用
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 完整预览对话框 -->
    <el-dialog
      v-model="showPreview"
      title="评分标准预览"
      width="900px"
    >
      <RubricPreviewDialog
        v-if="showPreview"
        :model-value="true"
        :rubric-data="previewData"
        @update:model-value="showPreview = false"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Plus, Edit, Delete, View, Check, Document, MagicStick 
} from '@element-plus/icons-vue'
import RubricPreviewDialog from './RubricPreviewDialog.vue'
import type { RubricCriterion, RubricSuggestionItem } from '@/types/api'

// Props
interface Props {
  modelValue: RubricCriterion[]
  questionMaxScore?: number
  aiSuggestions?: RubricSuggestionItem[]
}

const props = withDefaults(defineProps<Props>(), {
  questionMaxScore: 100,
  aiSuggestions: () => []
})

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: RubricCriterion[]]
  'save': [criteria: RubricCriterion[]]
  'applySuggestion': [suggestion: RubricSuggestionItem, mode: string]
}>()

// 响应式数据
const criteria = ref<RubricCriterion[]>([])
const showPreview = ref(false)
const showAISuggestionPreview = ref(false)
const currentAISuggestion = ref<RubricSuggestionItem | null>(null)
const applyMode = ref<'append' | 'replace'>('append')
const autoAdjustScore = ref(true)
const autoAssignWeight = ref(true)

// 生成临时ID的计数器
let tempIdCounter = 0

// 计算属性
const totalScore = computed(() => {
  return criteria.value.reduce((sum, criterion) => sum + (criterion.maxScore || 0), 0)
})

const totalWeight = computed(() => {
  return criteria.value.reduce((sum, criterion) => sum + (criterion.weight || 0), 0)
})

const isValid = computed(() => {
  return totalScore.value === props.questionMaxScore && 
         totalWeight.value === 100 &&
         criteria.value.every(c => c.criterionText && (c.maxScore ?? 0) > 0)
})

const previewCriteria = computed(() => {
  if (!currentAISuggestion.value) return criteria.value

  const suggestion = currentAISuggestion.value
  let adjustedScore = suggestion.points

  // 自动调整分数
  if (autoAdjustScore.value && applyMode.value === 'replace') {
    adjustedScore = props.questionMaxScore
  }

  const newCriterion: RubricCriterion = {
    id: 0,
    criterionText: suggestion.criterionText,
    maxScore: adjustedScore,
    points: adjustedScore,
    description: '',
    weight: autoAssignWeight.value ? 100 : 0,
    isNew: true
  }

  if (applyMode.value === 'replace') {
    return [newCriterion]
  } else {
    return [...criteria.value, newCriterion]
  }
})

const previewTotalScore = computed(() => {
  return previewCriteria.value.reduce((sum, criterion) => sum + (criterion.maxScore || 0), 0)
})

const previewData = computed(() => {
  return {
    id: 0, // 添加id字段
    name: '评分标准预览',
    totalScore: totalScore.value, // 添加totalScore字段
    criteria: criteria.value.map((c, index) => ({
      id: index + 1, // 添加SystemRubricCriterion需要的id字段
      name: c.criterionText,
      description: c.description || '',
      weight: c.weight || 0,
      scoreLevels: c.scoreLevels || []
    })),
    isActive: true,
    createdAt: new Date().toISOString(), // 添加createdAt字段
    updatedAt: new Date().toISOString()  // 添加updatedAt字段
  }
})

// 监听props变化
watch(() => props.modelValue, (newValue) => {
  criteria.value = [...newValue].map(criterion => ({
    ...criterion,
    tempId: criterion.id === 0 ? ++tempIdCounter : undefined
  }))
}, { immediate: true })

watch(criteria, () => {
  emit('update:modelValue', criteria.value)
}, { deep: true })

// 方法
const addCriterion = () => {
  const newCriterion: RubricCriterion = {
    id: 0,
    tempId: ++tempIdCounter,
    criterionText: '',
    maxScore: 0,
    points: 0,
    description: '',
    weight: 0,
    isNew: true,
    scoreLevels: []
  }
  criteria.value.push(newCriterion)
}

const removeCriterion = (index: number) => {
  criteria.value.splice(index, 1)
}

const markAsModified = (criterion: RubricCriterion) => {
  if (!criterion.isNew) {
    criterion.isModified = true
  }
}

const addScoreLevel = (criterion: RubricCriterion) => {
  if (!criterion.scoreLevels) {
    criterion.scoreLevels = []
  }
  criterion.scoreLevels.push({
    id: Date.now(),
    level: '', // 添加level字段
    name: '',
    score: 0,
    description: ''
  })
  markAsModified(criterion)
}

const removeScoreLevel = (criterion: RubricCriterion, levelIndex: number) => {
  if (criterion.scoreLevels) {
    criterion.scoreLevels.splice(levelIndex, 1)
    markAsModified(criterion)
  }
}

const getTypeColor = (index: number): 'primary' | 'success' | 'warning' | 'danger' | 'info' => {
  const colors: ('primary' | 'success' | 'warning' | 'danger' | 'info')[] = ['primary', 'success', 'warning', 'danger', 'info']
  return colors[index % colors.length]
}

// AI建议预览相关方法
const previewAISuggestion = (suggestion: RubricSuggestionItem) => {
  currentAISuggestion.value = suggestion
  applyMode.value = 'append'
  showAISuggestionPreview.value = true
}

const closeAISuggestionPreview = () => {
  showAISuggestionPreview.value = false
  currentAISuggestion.value = null
  applyMode.value = 'append'
  autoAdjustScore.value = true
  autoAssignWeight.value = true
}

const applyAISuggestionWithPreview = () => {
  if (!currentAISuggestion.value) return

  emit('applySuggestion', currentAISuggestion.value, applyMode.value)
  closeAISuggestionPreview()
}

// 暴露方法给父组件
defineExpose({
  previewAISuggestion,
  addCriterion,
  isValid
})
</script>

<style scoped>
.enhanced-rubric-editor {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.editor-card {
  min-height: 400px;
}

.editor-header {
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

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.criteria-editor {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.criterion-editor-item {
  transition: all 0.3s ease;
}

.criterion-enter-active,
.criterion-leave-active {
  transition: all 0.3s ease;
}

.criterion-enter-from,
.criterion-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}

.criterion-card {
  border: 2px solid transparent;
  transition: all 0.3s ease;
}

.criterion-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.criterion-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.criterion-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.criterion-status {
  display: flex;
  align-items: center;
}

.criterion-form {
  padding-top: 8px;
}

.weight-unit {
  margin-left: 4px;
  color: #909399;
}

.score-levels-editor {
  width: 100%;
}

.no-levels {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.levels-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.level-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: #fafafa;
  border-radius: 4px;
}

.summary-card {
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  border: none;
}

.summary-content {
  display: flex;
  justify-content: space-around;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.summary-item .label {
  font-size: 14px;
  color: #909399;
}

.summary-item .value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.summary-item .value.warning {
  color: #e6a23c;
}

/* AI建议预览样式 */
.ai-suggestion-preview {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.suggestion-overview {
  background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
}

.comparison-card {
  border: 1px solid #e4e7ed;
}

.comparison-section {
  height: 100%;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.empty-state-small {
  text-align: center;
  padding: 20px;
  color: #909399;
}

.criteria-list-small {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.criterion-item-small {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  border-left: 3px solid #409eff;
}

.criterion-item-small.new-criterion {
  background: #f0f9ff;
  border-left-color: #67c23a;
  animation: highlight 0.6s ease;
}

@keyframes highlight {
  0% { background: #e1f3d8; }
  100% { background: #f0f9ff; }
}

.criterion-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.criterion-name {
  font-weight: 500;
  color: #303133;
}

.criterion-score {
  font-size: 14px;
  color: #409eff;
  font-weight: 600;
}

.total-score {
  padding: 12px;
  text-align: center;
  background: #409eff;
  color: white;
  border-radius: 6px;
  font-weight: 600;
  margin-top: 8px;
}

.total-score.changed {
  background: #e6a23c;
}

.apply-options {
  border: 1px solid #e4e7ed;
}

.apply-mode-group {
  width: 100%;
}

.apply-mode-option {
  display: block;
  width: 100%;
  margin: 0 0 16px 0;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.apply-mode-option:hover {
  border-color: #409eff;
  background: #f0f9ff;
}

.apply-mode-option.is-checked {
  border-color: #409eff;
  background: #e6f7ff;
}

.option-content {
  margin-left: 24px;
}

.option-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.option-desc {
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
