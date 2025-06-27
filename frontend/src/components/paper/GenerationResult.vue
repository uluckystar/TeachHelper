<template>
  <div class="generation-result">
    <!-- 生成概览 -->
    <div class="result-overview">
      <el-alert
        v-if="result.warnings && result.warnings.length > 0"
        type="warning"
        :closable="false"
        style="margin-bottom: 16px"
      >
        <div class="warnings">
          <h4>注意事项：</h4>
          <ul>
            <li v-for="warning in result.warnings" :key="warning">
              {{ warning }}
            </li>
          </ul>
        </div>
      </el-alert>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-card class="overview-card">
            <div class="stat-item">
              <div class="stat-icon exam">
                <el-icon><Document /></el-icon>
              </div>
              <div class="stat-content">
                <h3>{{ result.examTitle }}</h3>
                <p>{{ result.examDescription || '无描述' }}</p>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card class="overview-card">
            <div class="stat-item">
              <div class="stat-icon success">
                <el-icon><SuccessFilled /></el-icon>
              </div>
              <div class="stat-content">
                <h3>生成成功</h3>
                <p>共生成 {{ result.questions.length }} 道题目</p>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 统计信息 -->
    <el-card class="statistics-card" v-if="result.statistics">
      <template #header>
        <h3>试卷统计</h3>
      </template>
      
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-box">
            <div class="stat-number">{{ result.statistics.totalQuestions }}</div>
            <div class="stat-label">总题数</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-box">
            <div class="stat-number">{{ result.statistics.totalScore }}</div>
            <div class="stat-label">总分值</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-box">
            <div class="stat-number">{{ result.statistics.estimatedTime || '未设置' }}</div>
            <div class="stat-label">预计时间(分钟)</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-box">
            <div class="stat-number">{{ formatDate(result.generatedAt) }}</div>
            <div class="stat-label">生成时间</div>
          </div>
        </el-col>
      </el-row>

      <!-- 题型分布 -->
      <div class="type-distribution" v-if="result.statistics.typeStatistics">
        <h4>题型分布</h4>
        <div class="type-stats">
          <el-tag
            v-for="(count, type) in getTypeStatistics()"
            :key="type"
            :type="getTypeTagType(type as string)"
            class="type-tag"
          >
            {{ getQuestionTypeText(type as string) }}: {{ count }}题
          </el-tag>
        </div>
      </div>

      <!-- 难度分布 -->
      <div class="difficulty-distribution" v-if="result.statistics.difficultyStatistics">
        <h4>难度分布</h4>
        <div class="difficulty-stats">
          <el-tag type="success" class="difficulty-tag">
            简单: {{ result.statistics.difficultyStatistics.easy }}题
          </el-tag>
          <el-tag type="warning" class="difficulty-tag">
            中等: {{ result.statistics.difficultyStatistics.medium }}题
          </el-tag>
          <el-tag type="danger" class="difficulty-tag">
            困难: {{ result.statistics.difficultyStatistics.hard }}题
          </el-tag>
        </div>
      </div>
    </el-card>

    <!-- 题目列表 -->
    <el-card class="questions-card">
      <template #header>
        <div class="questions-header">
          <h3>生成的题目</h3>
          <div class="question-filters">
            <el-select
              v-model="typeFilter"
              placeholder="按题型筛选"
              clearable
              @change="handleFilter"
            >
              <el-option label="单选题" value="SINGLE_CHOICE" />
              <el-option label="多选题" value="MULTIPLE_CHOICE" />
              <el-option label="判断题" value="TRUE_FALSE" />
              <el-option label="简答题" value="SHORT_ANSWER" />
              <el-option label="论述题" value="ESSAY" />
              <el-option label="编程题" value="CODING" />
              <el-option label="案例分析题" value="CASE_ANALYSIS" />
            </el-select>
            <el-select
              v-model="difficultyFilter"
              placeholder="按难度筛选"
              clearable
              @change="handleFilter"
            >
              <el-option label="简单" value="easy" />
              <el-option label="中等" value="medium" />
              <el-option label="困难" value="hard" />
            </el-select>
          </div>
        </div>
      </template>

      <div class="questions-list">
        <div
          v-for="(question, index) in filteredQuestions"
          :key="index"
          class="question-item"
        >
          <el-card class="question-card">
            <div class="question-header">
              <div class="question-meta">
                <span class="question-number">第 {{ index + 1 }} 题</span>
                <el-tag :type="getQuestionTypeTagType(question.questionType)" size="small">
                  {{ getQuestionTypeText(question.questionType) }}
                </el-tag>
                <el-tag :type="getDifficultyTagType(question.difficulty)" size="small">
                  {{ getDifficultyText(question.difficulty) }}
                </el-tag>
                <span class="question-score">{{ question.maxScore }}分</span>
              </div>
            </div>

            <div class="question-content">
              <h4 class="question-title">{{ question.title }}</h4>
              <div class="question-text" v-html="question.content"></div>

              <!-- 选择题选项 -->
              <div
                v-if="question.options && question.options.length > 0"
                class="question-options"
              >
                <div
                  v-for="(option, optionIndex) in question.options"
                  :key="optionIndex"
                  class="option-item"
                  :class="{ 'correct-option': option.isCorrect }"
                >
                  <span class="option-label">{{ String.fromCharCode(65 + optionIndex) }}.</span>
                  <span class="option-content">{{ option.content }}</span>
                  <el-tag v-if="option.isCorrect" type="success" size="small">正确答案</el-tag>
                </div>
              </div>

              <!-- 参考答案 -->
              <div v-if="question.referenceAnswer" class="reference-answer">
                <h5>参考答案：</h5>
                <div class="answer-content">{{ question.referenceAnswer }}</div>
              </div>

              <!-- 知识点 -->
              <div v-if="question.knowledgePoint" class="knowledge-point">
                <el-tag type="info" size="small">
                  <el-icon><Notebook /></el-icon>
                  {{ question.knowledgePoint }}
                </el-tag>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </el-card>

    <!-- 生成总结 -->
    <el-card class="summary-card" v-if="result.generationSummary">
      <template #header>
        <h3>生成总结</h3>
      </template>
      <div class="summary-content">
        <pre>{{ result.generationSummary }}</pre>
      </div>
    </el-card>

    <!-- 操作按钮 -->
    <div class="action-buttons">
      <el-button type="primary" size="large" @click="$emit('view-exam', result.examId)">
        <el-icon><View /></el-icon>
        查看完整试卷
      </el-button>
      <el-button size="large" @click="$emit('regenerate')">
        <el-icon><Refresh /></el-icon>
        重新生成
      </el-button>
      <el-button size="large" @click="exportPaper">
        <el-icon><Download /></el-icon>
        导出试卷
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Document, SuccessFilled, Notebook, View, Refresh, Download
} from '@element-plus/icons-vue'
import type { PaperGenerationResponse } from '@/api/knowledge'

interface Props {
  result: PaperGenerationResponse
}

interface Emits {
  (e: 'view-exam', examId: number): void
  (e: 'regenerate'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const typeFilter = ref('')
const difficultyFilter = ref('')

// 计算属性
const filteredQuestions = computed(() => {
  return props.result.questions.filter(question => {
    const matchesType = !typeFilter.value || question.questionType === typeFilter.value
    const matchesDifficulty = !difficultyFilter.value || question.difficulty === difficultyFilter.value
    return matchesType && matchesDifficulty
  })
})

// 方法
const handleFilter = () => {
  // 过滤逻辑由计算属性处理
}

const getTypeStatistics = () => {
  const stats = props.result.statistics.typeStatistics
  const result: Record<string, number> = {}
  
  if (stats.singleChoice) result.SINGLE_CHOICE = stats.singleChoice
  if (stats.multipleChoice) result.MULTIPLE_CHOICE = stats.multipleChoice
  if (stats.trueFalse) result.TRUE_FALSE = stats.trueFalse
  if (stats.shortAnswer) result.SHORT_ANSWER = stats.shortAnswer
  if (stats.essay) result.ESSAY = stats.essay
  if (stats.coding) result.CODING = stats.coding
  if (stats.caseAnalysis) result.CASE_ANALYSIS = stats.caseAnalysis
  
  return result
}

const getQuestionTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'TRUE_FALSE': '判断题',
    'SHORT_ANSWER': '简答题',
    'ESSAY': '论述题',
    'CODING': '编程题',
    'CASE_ANALYSIS': '案例分析题',
    'CALCULATION': '计算题'
  }
  return typeMap[type] || type
}

const getQuestionTypeTagType = (type: string): 'success' | 'primary' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'success' | 'primary' | 'warning' | 'info' | 'danger'> = {
    'SINGLE_CHOICE': 'primary',
    'MULTIPLE_CHOICE': 'success',
    'TRUE_FALSE': 'warning',
    'SHORT_ANSWER': 'info',
    'ESSAY': 'danger',
    'CODING': 'info',
    'CASE_ANALYSIS': 'success',
    'CALCULATION': 'primary'
  }
  return typeMap[type] || 'info'
}

const getTypeTagType = (type: string): 'success' | 'primary' | 'warning' | 'info' | 'danger' => {
  return getQuestionTypeTagType(type)
}

const getDifficultyText = (difficulty: string) => {
  const difficultyMap: Record<string, string> = {
    'easy': '简单',
    'medium': '中等',
    'hard': '困难'
  }
  return difficultyMap[difficulty] || difficulty
}

const getDifficultyTagType = (difficulty: string): 'success' | 'primary' | 'warning' | 'info' | 'danger' => {
  const difficultyMap: Record<string, 'success' | 'primary' | 'warning' | 'info' | 'danger'> = {
    'easy': 'success',
    'medium': 'warning',
    'hard': 'danger'
  }
  return difficultyMap[difficulty] || 'info'
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN')
}

const exportPaper = () => {
  // TODO: 实现试卷导出功能
  ElMessage.info('导出功能开发中...')
}
</script>

<style scoped>
.generation-result {
  max-width: 100%;
}

.result-overview {
  margin-bottom: 20px;
}

.warnings h4 {
  margin: 0 0 8px 0;
  color: #e6a23c;
}

.warnings ul {
  margin: 0;
  padding-left: 20px;
}

.overview-card {
  height: 100%;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
}

.stat-icon.exam {
  background-color: #409eff;
}

.stat-icon.success {
  background-color: #67c23a;
}

.stat-content h3 {
  margin: 0 0 4px 0;
  color: #303133;
}

.stat-content p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.statistics-card {
  margin-bottom: 20px;
}

.stat-box {
  text-align: center;
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
}

.stat-label {
  color: #606266;
  font-size: 14px;
}

.type-distribution,
.difficulty-distribution {
  margin-top: 20px;
}

.type-distribution h4,
.difficulty-distribution h4 {
  margin: 0 0 12px 0;
  color: #303133;
}

.type-stats,
.difficulty-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.type-tag,
.difficulty-tag {
  margin: 0;
}

.questions-card {
  margin-bottom: 20px;
}

.questions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.questions-header h3 {
  margin: 0;
}

.question-filters {
  display: flex;
  gap: 10px;
}

.question-filters .el-select {
  width: 150px;
}

.questions-list {
  max-height: 600px;
  overflow-y: auto;
}

.question-item {
  margin-bottom: 16px;
}

.question-card {
  border-left: 4px solid #409eff;
}

.question-header {
  margin-bottom: 12px;
}

.question-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.question-number {
  font-weight: bold;
  color: #303133;
}

.question-score {
  margin-left: auto;
  color: #f56c6c;
  font-weight: bold;
}

.question-title {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 16px;
}

.question-text {
  margin-bottom: 12px;
  line-height: 1.6;
  color: #606266;
}

.question-options {
  margin-bottom: 12px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  margin-bottom: 4px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.correct-option {
  background-color: #f0f9ff;
  border-color: #67c23a;
}

.option-label {
  font-weight: bold;
  color: #409eff;
  min-width: 20px;
}

.option-content {
  flex-grow: 1;
}

.reference-answer {
  margin-bottom: 12px;
  padding: 12px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.reference-answer h5 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 14px;
}

.answer-content {
  color: #606266;
  line-height: 1.5;
}

.knowledge-point {
  margin-top: 8px;
}

.summary-card {
  margin-bottom: 20px;
}

.summary-content pre {
  white-space: pre-wrap;
  margin: 0;
  color: #606266;
  line-height: 1.6;
}

.action-buttons {
  text-align: center;
  padding: 20px 0;
}

.action-buttons .el-button {
  margin: 0 10px;
}

:deep(.el-card__header) {
  background-color: #fafafa;
  border-bottom: 1px solid #ebeef5;
}
</style>
