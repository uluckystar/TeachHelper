<template>
  <el-dialog
    v-model="visible"
    :title="`题目预览 - ${getTypeText(question?.type || '')}`"
    width="700px"
    :close-on-click-modal="false"
  >
    <div class="question-preview" v-if="question">
      <!-- 题目信息 -->
      <div class="question-header">
        <div class="question-meta">
          <el-tag :type="getTypeTagType(question.type)" size="small">
            {{ getTypeText(question.type) }}
          </el-tag>
          <el-tag :type="getDifficultyTagType(question.difficulty)" size="small">
            {{ getDifficultyText(question.difficulty) }}
          </el-tag>
          <span class="question-score">{{ question.score }}分</span>
          <span class="question-time">预计{{ question.estimatedTime || 3 }}分钟</span>
        </div>
        <div class="question-actions">
          <el-button size="small" @click="editQuestion">编辑</el-button>
          <el-button size="small" @click="startPractice">练习</el-button>
          <el-dropdown @command="handleAction">
            <el-button size="small" icon="MoreFilled" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="copy">复制题目</el-dropdown-item>
                <el-dropdown-item command="export">导出题目</el-dropdown-item>
                <el-dropdown-item command="share">分享题目</el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除题目</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 题目内容 -->
      <div class="question-content">
        <div class="question-text">
          {{ question.content }}
        </div>

        <!-- 选择题选项 -->
        <div v-if="question.type === 'choice' && question.options" class="question-options">
          <div
            v-for="(option, index) in question.options"
            :key="index"
            class="option-item"
            :class="{ 'correct-option': option.isCorrect }"
          >
            <span class="option-label">{{ String.fromCharCode(65 + index) }}.</span>
            <span class="option-text">{{ option.content }}</span>
            <el-icon v-if="option.isCorrect" class="correct-icon" color="#67c23a">
              <Check />
            </el-icon>
          </div>
        </div>

        <!-- 填空题答案 -->
        <div v-if="question.type === 'blank' && question.blanks" class="question-blanks">
          <div class="blanks-title">参考答案：</div>
          <div class="blanks-list">
            <div
              v-for="(blank, index) in question.blanks"
              :key="index"
              class="blank-item"
            >
              <span class="blank-label">空{{ index + 1 }}：</span>
              <span class="blank-answer">{{ blank.answer }}</span>
            </div>
          </div>
        </div>

        <!-- 主观题/计算题答案 -->
        <div v-if="(question.type === 'subjective' || question.type === 'calculation') && question.referenceAnswer" class="question-answer">
          <div class="answer-title">参考答案：</div>
          <div class="answer-content">{{ question.referenceAnswer }}</div>
        </div>
        <div v-if="(question.type === 'subjective' || question.type === 'calculation') && question.standardAnswer" class="question-answer standard-answer-section">
          <div class="answer-title">标准答案：</div>
          <div class="answer-content">{{ question.standardAnswer }}</div>
        </div>
      </div>

      <!-- 题目解析 -->
      <div v-if="question.explanation" class="question-explanation">
        <div class="explanation-title">
          <el-icon><InfoFilled /></el-icon>
          题目解析
        </div>
        <div class="explanation-content">{{ question.explanation }}</div>
      </div>

      <!-- 关联信息 -->
      <div class="question-relations">
        <!-- 关联知识点 -->
        <div v-if="question.knowledgePoints && question.knowledgePoints.length > 0" class="relation-section">
          <div class="relation-title">
            <el-icon><Collection /></el-icon>
            关联知识点
          </div>
          <div class="knowledge-points">
            <el-tag
              v-for="point in question.knowledgePoints"
              :key="point.id"
              :type="getCategoryTagType(point.category)"
              size="small"
              class="knowledge-point-tag"
            >
              {{ point.title }}
            </el-tag>
          </div>
        </div>

        <!-- 题目标签 -->
        <div v-if="question.tags && question.tags.length > 0" class="relation-section">
          <div class="relation-title">
            <el-icon><PriceTag /></el-icon>
            题目标签
          </div>
          <div class="question-tags">
            <el-tag
              v-for="tag in question.tags"
              :key="tag"
              type="info"
              size="small"
              class="question-tag"
            >
              {{ tag }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- 统计信息 -->
      <div class="question-stats">
        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-label">练习次数</div>
            <div class="stat-value">{{ question.practiceCount || 0 }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">正确率</div>
            <div class="stat-value">{{ question.accuracyRate || 0 }}%</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">平均用时</div>
            <div class="stat-value">{{ question.averageTime || 0 }}分钟</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">难度系数</div>
            <div class="stat-value">{{ question.difficultyCoeff || 0.5 }}</div>
          </div>
        </div>
      </div>

      <!-- 题目历史 -->
      <div class="question-history">
        <el-collapse>
          <el-collapse-item title="题目历史记录" name="history">
            <div class="history-list">
              <div
                v-for="record in questionHistory"
                :key="record.id"
                class="history-item"
              >
                <div class="history-info">
                  <span class="history-action">{{ record.action }}</span>
                  <span class="history-user">{{ record.user }}</span>
                  <span class="history-time">{{ formatDate(record.createdAt) }}</span>
                </div>
                <div v-if="record.note" class="history-note">{{ record.note }}</div>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>

      <!-- 答题记录 -->
      <div v-if="answerRecords.length > 0" class="answer-records">
        <el-collapse>
          <el-collapse-item title="最近答题记录" name="records">
            <div class="records-list">
              <div
                v-for="record in answerRecords"
                :key="record.id"
                class="record-item"
              >
                <div class="record-header">
                  <span class="record-user">{{ record.studentName }}</span>
                  <span class="record-time">{{ formatDate(record.answeredAt) }}</span>
                  <el-tag
                    :type="record.isCorrect ? 'success' : 'danger'"
                    size="small"
                  >
                    {{ record.isCorrect ? '正确' : '错误' }}
                  </el-tag>
                </div>
                <div class="record-answer">
                  <strong>学生答案：</strong>{{ record.answer }}
                </div>
                <div v-if="record.timeSpent" class="record-time-spent">
                  <strong>用时：</strong>{{ record.timeSpent }}秒
                </div>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>

      <!-- 备注信息 -->
      <div v-if="question.notes" class="question-notes">
        <div class="notes-title">
          <el-icon><EditPen /></el-icon>
          备注信息
        </div>
        <div class="notes-content">{{ question.notes }}</div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">关闭</el-button>
        <el-button @click="editQuestion">编辑题目</el-button>
        <el-button type="primary" @click="startPractice">开始练习</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Check,
  InfoFilled,
  Collection,
  PriceTag,
  EditPen,
  MoreFilled
} from '@element-plus/icons-vue'

// Props
const props = defineProps<{
  modelValue: boolean
  question: any
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

const questionHistory = ref<any[]>([])
const answerRecords = ref<any[]>([])

// 监听问题变化
watch(() => props.question, (newQuestion) => {
  if (newQuestion && visible.value) {
    loadQuestionHistory()
    loadAnswerRecords()
  }
}, { immediate: true })

// 方法
const loadQuestionHistory = async () => {
  try {
    // TODO: 从API加载题目历史记录
    // 模拟数据
    questionHistory.value = [
      {
        id: 1,
        action: '创建题目',
        user: '张老师',
        createdAt: '2024-01-15T10:30:00Z',
        note: '基于函数概念创建的练习题'
      },
      {
        id: 2,
        action: '修改题目',
        user: '李老师',
        createdAt: '2024-01-16T14:20:00Z',
        note: '优化了选项描述'
      },
      {
        id: 3,
        action: '添加解析',
        user: '王老师',
        createdAt: '2024-01-17T09:15:00Z',
        note: '补充了详细的解题步骤'
      }
    ]
  } catch (error) {
    console.error('Load question history failed:', error)
  }
}

const loadAnswerRecords = async () => {
  try {
    // TODO: 从API加载答题记录
    // 模拟数据
    answerRecords.value = [
      {
        id: 1,
        studentName: '小明',
        answer: 'A',
        isCorrect: true,
        timeSpent: 45,
        answeredAt: '2024-01-18T10:30:00Z'
      },
      {
        id: 2,
        studentName: '小红',
        answer: 'B',
        isCorrect: false,
        timeSpent: 32,
        answeredAt: '2024-01-18T11:15:00Z'
      },
      {
        id: 3,
        studentName: '小强',
        answer: 'A',
        isCorrect: true,
        timeSpent: 52,
        answeredAt: '2024-01-18T14:20:00Z'
      }
    ]
  } catch (error) {
    console.error('Load answer records failed:', error)
  }
}

const editQuestion = () => {
  ElMessage.info('编辑题目功能开发中...')
}

const startPractice = () => {
  ElMessage.info('开始练习功能开发中...')
}

const handleAction = (command: string) => {
  switch (command) {
    case 'copy':
      copyQuestion()
      break
    case 'export':
      exportQuestion()
      break
    case 'share':
      shareQuestion()
      break
    case 'delete':
      deleteQuestion()
      break
  }
}

const copyQuestion = () => {
  ElMessage.info('复制题目功能开发中...')
}

const exportQuestion = () => {
  ElMessage.info('导出题目功能开发中...')
}

const shareQuestion = () => {
  ElMessage.info('分享题目功能开发中...')
}

const deleteQuestion = () => {
  ElMessage.info('删除题目功能开发中...')
}

// 工具方法
const getTypeTagType = (type: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    'choice': 'primary',
    'blank': 'success',
    'subjective': 'warning',
    'calculation': 'info'
  }
  return typeMap[type] || 'info'
}

const getTypeText = (type: string) => {
  const textMap: Record<string, string> = {
    'choice': '选择题',
    'blank': '填空题',
    'subjective': '主观题',
    'calculation': '计算题'
  }
  return textMap[type] || type
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

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}
</script>

<style scoped>
.question-preview {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #f5f7fa;
}

.question-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.question-score,
.question-time {
  font-size: 13px;
  color: #909399;
}

.question-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.question-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.question-text {
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #ffffff;
}

.question-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fafafa;
  transition: all 0.3s;
}

.option-item.correct-option {
  border-color: #67c23a;
  background: #f0f9ff;
  color: #67c23a;
}

.option-label {
  min-width: 24px;
  font-weight: 600;
  color: #606266;
}

.option-text {
  flex: 1;
  line-height: 1.6;
}

.correct-icon {
  font-size: 18px;
}

.question-blanks,
.question-answer {
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #f5f7fa;
}

.blanks-title,
.answer-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.blanks-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.blank-item {
  display: flex;
  gap: 8px;
}

.blank-label {
  min-width: 60px;
  font-weight: 500;
  color: #606266;
}

.blank-answer {
  color: #409eff;
  font-weight: 600;
}

.answer-content {
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
}

.question-explanation {
  padding: 16px;
  border: 1px solid #409eff;
  border-radius: 8px;
  background: #f0f9ff;
}

.explanation-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 12px;
}

.explanation-content {
  line-height: 1.8;
  color: #606266;
}

.question-relations {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.relation-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.relation-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #606266;
}

.knowledge-points,
.question-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.knowledge-point-tag,
.question-tag {
  margin: 2px;
}

.question-stats {
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #f5f7fa;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 16px;
}

.stat-item {
  text-align: center;
  padding: 12px;
  background: #ffffff;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #409eff;
}

.question-history,
.answer-records {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.history-list,
.records-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-item,
.record-item {
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  background: #fafafa;
}

.history-info,
.record-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.history-action {
  font-weight: 600;
  color: #409eff;
}

.history-user,
.record-user {
  color: #606266;
}

.history-time,
.record-time {
  font-size: 12px;
  color: #909399;
}

.history-note,
.record-answer,
.record-time-spent {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.question-notes {
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #f5f7fa;
}

.notes-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 12px;
}

.notes-content {
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.standard-answer-section {
  background-color: #f0f9eb;
  border-left: 4px solid #67c23a;
  padding: 10px;
  border-radius: 4px;
}
</style>
