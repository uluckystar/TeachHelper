<template>
  <div class="task-result-display">
    <div v-if="taskType === 'evaluation'" class="evaluation-result">
      <div class="result-summary">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-statistic
              title="总答案数"
              :value="result.totalAnswers || 0"
              suffix="个"
            />
          </el-col>
          <el-col :span="6">
            <el-statistic
              title="成功评估"
              :value="result.successfulEvaluations || 0"
              suffix="个"
            />
          </el-col>
          <el-col :span="6">
            <el-statistic
              title="失败数量"
              :value="result.failedEvaluations || 0"
              suffix="个"
            />
          </el-col>
          <el-col :span="6">
            <el-statistic
              title="成功率"
              :value="calculateSuccessRate(result)"
              suffix="%"
            />
          </el-col>
        </el-row>
      </div>

      <div v-if="result.evaluationDetails" class="result-details">
        <h4>评估详情</h4>
        <el-table :data="result.evaluationDetails.slice(0, 10)" stripe>
          <el-table-column prop="answerId" label="答案ID" width="100" />
          <el-table-column prop="score" label="得分" width="80" />
          <el-table-column prop="maxScore" label="满分" width="80" />
          <el-table-column prop="feedback" label="反馈" show-overflow-tooltip />
          <el-table-column prop="duration" label="用时(s)" width="100" />
        </el-table>
      </div>
    </div>

    <div v-else-if="taskType === 'generation'" class="generation-result">
      <div class="result-summary">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-statistic
              title="生成题目数"
              :value="result.totalQuestions || 0"
              suffix="道"
            />
          </el-col>
          <el-col :span="6">
            <el-statistic
              title="成功生成"
              :value="result.successfulQuestions || 0"
              suffix="道"
            />
          </el-col>
          <el-col :span="6">
            <el-statistic
              title="总分值"
              :value="result.totalScore || 0"
              suffix="分"
            />
          </el-col>
          <el-col :span="6">
            <el-statistic
              title="生成耗时"
              :value="result.generationTime || 0"
              suffix="秒"
            />
          </el-col>
        </el-row>
      </div>

      <div v-if="result.questionBreakdown" class="question-breakdown">
        <h4>题型分布</h4>
        <el-table :data="result.questionBreakdown" stripe>
          <el-table-column prop="type" label="题型">
            <template #default="{ row }">
              {{ getQuestionTypeText(row.type) }}
            </template>
          </el-table-column>
          <el-table-column prop="count" label="数量" width="100" />
          <el-table-column prop="totalScore" label="总分" width="100" />
          <el-table-column prop="avgDifficulty" label="平均难度" width="120">
            <template #default="{ row }">
              <el-tag :type="getDifficultyType(row.avgDifficulty)" size="small">
                {{ getDifficultyText(row.avgDifficulty) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-if="result.generatedQuestions" class="generated-questions">
        <h4>生成的题目 (前5道)</h4>
        <div
          v-for="(question, index) in result.generatedQuestions.slice(0, 5)"
          :key="index"
          class="question-item"
        >
          <div class="question-header">
            <span class="question-number">题目 {{ index + 1 }}</span>
            <div class="question-meta">
              <el-tag size="small">{{ getQuestionTypeText(question.type) }}</el-tag>
              <el-tag :type="getDifficultyType(question.difficulty)" size="small">
                {{ getDifficultyText(question.difficulty) }}
              </el-tag>
              <span class="question-score">{{ question.score }}分</span>
            </div>
          </div>
          <div class="question-content">
            <p>{{ question.content }}</p>
            <div v-if="question.options" class="question-options">
              <div v-for="(option, optIndex) in question.options" :key="optIndex" class="option">
                {{ String.fromCharCode(65 + optIndex) }}. {{ option }}
              </div>
            </div>
            <div v-if="question.answer" class="question-answer">
              <strong>参考答案：</strong>{{ question.answer }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="taskType === 'knowledge'" class="knowledge-result">
      <div class="result-summary">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-statistic
              title="处理文档数"
              :value="result.totalDocuments || 0"
              suffix="个"
            />
          </el-col>
          <el-col :span="6">
            <el-statistic
              title="成功处理"
              :value="result.successfulDocuments || 0"
              suffix="个"
            />
          </el-col>
          <el-col :span="6">
            <el-statistic
              title="生成向量数"
              :value="result.totalVectors || 0"
              suffix="个"
            />
          </el-col>
          <el-col :span="6">
            <el-statistic
              title="处理耗时"
              :value="result.processingTime || 0"
              suffix="秒"
            />
          </el-col>
        </el-row>
      </div>

      <div v-if="result.documentDetails" class="document-details">
        <h4>文档处理详情</h4>
        <el-table :data="result.documentDetails" stripe>
          <el-table-column prop="name" label="文档名称" show-overflow-tooltip />
          <el-table-column prop="size" label="文件大小" width="120">
            <template #default="{ row }">
              {{ formatFileSize(row.size) }}
            </template>
          </el-table-column>
          <el-table-column prop="pageCount" label="页数" width="80" />
          <el-table-column prop="vectorCount" label="向量数" width="100" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">
                {{ row.status === 'success' ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div v-else class="unknown-result">
      <el-empty description="暂无结果数据" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  task: any
  result: any
}

const props = defineProps<Props>()

// 计算属性
const taskType = computed(() => props.task?.type || 'unknown')

// 辅助方法
const calculateSuccessRate = (result: any) => {
  if (!result.totalAnswers || result.totalAnswers === 0) return 0
  const successCount = result.successfulEvaluations || 0
  return Math.round((successCount / result.totalAnswers) * 100)
}

const getQuestionTypeText = (type: string) => {
  const typeMap = {
    'multiple_choice': '选择题',
    'fill_blank': '填空题',
    'short_answer': '简答题',
    'essay': '论述题'
  }
  return typeMap[type as keyof typeof typeMap] || type
}

const getDifficultyType = (difficulty: string): 'success' | 'primary' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'success' | 'primary' | 'warning' | 'info' | 'danger'> = {
    'easy': 'success',
    'medium': 'warning',
    'hard': 'danger'
  }
  return typeMap[difficulty] || 'info'
}

const getDifficultyText = (difficulty: string) => {
  const textMap = {
    'easy': '简单',
    'medium': '中等',
    'hard': '困难'
  }
  return textMap[difficulty as keyof typeof textMap] || difficulty
}

const formatFileSize = (bytes: number) => {
  if (!bytes) return '--'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}
</script>

<style scoped>
.task-result-display {
  padding: 16px;
}

.result-summary {
  margin-bottom: 24px;
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 6px;
}

.result-details,
.question-breakdown,
.generated-questions,
.document-details {
  margin-bottom: 24px;
}

.result-details h4,
.question-breakdown h4,
.generated-questions h4,
.document-details h4 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.question-item {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 12px;
  background-color: #fafafa;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.question-number {
  font-weight: 600;
  color: #303133;
}

.question-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.question-score {
  color: #409eff;
  font-weight: 600;
}

.question-content p {
  margin: 0 0 12px 0;
  color: #606266;
  line-height: 1.6;
}

.question-options {
  margin: 12px 0;
}

.option {
  padding: 4px 0;
  color: #606266;
}

.question-answer {
  margin-top: 12px;
  padding: 8px 12px;
  background-color: #e1f3d8;
  border-radius: 4px;
  font-size: 14px;
}

.unknown-result {
  text-align: center;
  padding: 40px 20px;
}
</style>
