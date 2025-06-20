<template>
  <el-dialog
    v-model="visible"
    title="评估标准预览"
    width="800px"
    @close="handleClose"
  >
    <div class="rubric-preview" v-if="rubricData">
      <!-- 基本信息 -->
      <el-card class="info-section" shadow="never">
        <template #header>
          <h3>基本信息</h3>
        </template>
        <el-descriptions :column="2" size="large">
          <el-descriptions-item label="标准名称">
            <span class="rubric-name">{{ rubricData.name }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="适用学科">
            <el-tag type="primary">{{ rubricData.subject || '通用' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="rubricData.isActive ? 'success' : 'info'">
              {{ rubricData.isActive ? '启用' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="评分标准数量">
            <span class="criteria-count">{{ rubricData.criteria?.length || 0 }} 项</span>
          </el-descriptions-item>
        </el-descriptions>
        
        <div v-if="rubricData.description" class="description-section">
          <h4>标准描述</h4>
          <p class="description-text">{{ rubricData.description }}</p>
        </div>
      </el-card>

      <!-- 评分标准详情 -->
      <el-card class="criteria-section" shadow="never">
        <template #header>
          <div class="section-header">
            <h3>评分标准详情</h3>
            <div class="criteria-summary">
              <span>总权重: {{ totalWeight }}%</span>
              <el-tag :type="totalWeight === 100 ? 'success' : 'warning'" size="small">
                {{ totalWeight === 100 ? '权重配置正确' : '权重需要调整' }}
              </el-tag>
            </div>
          </div>
        </template>

        <div v-if="rubricData.criteria && rubricData.criteria.length > 0" class="criteria-list">
          <div
            v-for="(criterion, index) in rubricData.criteria"
            :key="index"
            class="criterion-item"
          >
            <div class="criterion-header">
              <div class="criterion-title">
                <span class="criterion-index">{{ index + 1 }}.</span>
                <span class="criterion-name">{{ criterion.name }}</span>
              </div>
              <div class="criterion-weight">
                <el-progress
                  type="circle"
                  :percentage="criterion.weight"
                  :width="50"
                  :stroke-width="6"
                  :show-text="false"
                />
                <span class="weight-text">{{ criterion.weight }}%</span>
              </div>
            </div>

            <div v-if="criterion.description" class="criterion-description">
              <p>{{ criterion.description }}</p>
            </div>

            <!-- 评分等级 -->
            <div v-if="criterion.scoreLevels && criterion.scoreLevels.length > 0" class="score-levels">
              <h5>评分等级</h5>
              <div class="levels-grid">
                <div
                  v-for="(level, levelIndex) in criterion.scoreLevels"
                  :key="levelIndex"
                  class="score-level"
                  :class="getScoreLevelClass(level.score)"
                >
                  <div class="level-header">
                    <span class="level-name">{{ level.name }}</span>
                    <span class="level-score">{{ level.score }}分</span>
                  </div>
                  <div v-if="level.description" class="level-description">
                    {{ level.description }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="empty-criteria">
          <el-empty description="暂无评分标准" :image-size="100" />
        </div>
      </el-card>

      <!-- 使用统计（如果有的话） -->
      <el-card v-if="rubricData.usageCount !== undefined" class="stats-section" shadow="never">
        <template #header>
          <h3>使用统计</h3>
        </template>
        <div class="usage-stats">
          <div class="stat-item">
            <el-icon size="20" color="#409eff"><TrendCharts /></el-icon>
            <span class="stat-label">使用次数:</span>
            <span class="stat-value">{{ rubricData.usageCount }}</span>
          </div>
          <div class="stat-item" v-if="rubricData.lastUsed">
            <el-icon size="20" color="#909399"><Clock /></el-icon>
            <span class="stat-label">最后使用:</span>
            <span class="stat-value">{{ formatDate(rubricData.lastUsed) }}</span>
          </div>
        </div>
      </el-card>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
        <el-button type="primary" @click="editRubric" v-if="rubricData">
          编辑标准
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { TrendCharts, Clock } from '@element-plus/icons-vue'
import type { Rubric } from '@/types/api'

interface Props {
  modelValue: boolean
  rubricData?: Rubric | null
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'edit', rubric: Rubric): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 计算总权重
const totalWeight = computed(() => {
  if (!props.rubricData?.criteria) return 0
  return props.rubricData.criteria.reduce((sum, criterion) => sum + (criterion.weight || 0), 0)
})

// 获取评分等级样式类
const getScoreLevelClass = (score: number) => {
  if (score >= 90) return 'excellent'
  if (score >= 80) return 'good'
  if (score >= 70) return 'fair'
  if (score >= 60) return 'pass'
  return 'fail'
}

// 格式化日期
const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

const handleClose = () => {
  visible.value = false
}

const editRubric = () => {
  if (props.rubricData) {
    emit('edit', props.rubricData)
    handleClose()
  }
}
</script>

<style scoped>
.rubric-preview {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 600px;
  overflow-y: auto;
}

.info-section,
.criteria-section,
.stats-section {
  border: 1px solid #e4e7ed;
}

.rubric-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.criteria-count {
  font-weight: 500;
  color: #409eff;
}

.description-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.description-section h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}

.description-text {
  margin: 0;
  line-height: 1.6;
  color: #606266;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-header h3 {
  margin: 0;
}

.criteria-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.criteria-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.criterion-item {
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fafafa;
}

.criterion-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.criterion-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.criterion-index {
  font-weight: 600;
  color: #409eff;
}

.criterion-name {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.criterion-weight {
  display: flex;
  align-items: center;
  gap: 8px;
}

.weight-text {
  font-size: 14px;
  font-weight: 500;
  color: #409eff;
}

.criterion-description {
  margin-bottom: 16px;
}

.criterion-description p {
  margin: 0;
  line-height: 1.5;
  color: #606266;
}

.score-levels h5 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}

.levels-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.score-level {
  padding: 12px;
  border-radius: 6px;
  border-left: 4px solid;
}

.score-level.excellent {
  background: #f0f9ff;
  border-left-color: #67c23a;
}

.score-level.good {
  background: #f0f9ff;
  border-left-color: #409eff;
}

.score-level.fair {
  background: #fdf6ec;
  border-left-color: #e6a23c;
}

.score-level.pass {
  background: #fef0f0;
  border-left-color: #f56c6c;
}

.score-level.fail {
  background: #f4f4f5;
  border-left-color: #909399;
}

.level-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.level-name {
  font-weight: 500;
  color: #303133;
}

.level-score {
  font-weight: 600;
  color: #409eff;
}

.level-description {
  font-size: 13px;
  color: #666;
  line-height: 1.4;
}

.empty-criteria {
  text-align: center;
  padding: 40px 20px;
}

.usage-stats {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-label {
  color: #606266;
  font-size: 14px;
}

.stat-value {
  color: #303133;
  font-weight: 500;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
