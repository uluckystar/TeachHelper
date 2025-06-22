<template>
  <div class="question-detail">
    <div class="page-header">
      <el-breadcrumb>
        <el-breadcrumb-item :to="{ path: '/questions' }">题目库</el-breadcrumb-item>
        <el-breadcrumb-item>题目详情</el-breadcrumb-item>
      </el-breadcrumb>
      
      <div class="header-actions">
        <el-button type="primary" icon="Edit" @click="editQuestion">
          编辑题目
        </el-button>
        <el-button type="danger" icon="Delete" @click="deleteQuestion">
          删除题目
        </el-button>
        <el-button icon="Back" @click="goBack">
          返回
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="question-content">
      <el-card v-if="question" class="question-card">
        <template #header>
          <div class="card-header">
            <span class="question-title">{{ question.title }}</span>
            <el-tag :type="getQuestionTypeTag(question.questionType) as any" size="large">
              {{ getQuestionTypeText(question.questionType) }}
            </el-tag>
          </div>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="所属考试">
            <el-link 
              v-if="question.examId" 
              type="primary" 
              @click="goToExam(question.examId)"
            >
              {{ question.examTitle }}
            </el-link>
            <span v-else>未关联考试</span>
          </el-descriptions-item>
          <el-descriptions-item label="满分">
            {{ question.maxScore }} 分
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDate(question.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatDate(question.updatedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="答案统计" :span="2">
            总答案数：{{ question.totalAnswers || 0 }}，
            已评估：{{ question.evaluatedAnswers || 0 }}，
            平均分：{{ question.averageScore ? question.averageScore.toFixed(1) : '0.0' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider>题目内容</el-divider>
        <div class="question-content-text">
          <p>{{ question.content }}</p>
        </div>

        <!-- 选择题选项 -->
        <div v-if="isChoiceQuestion" class="question-options">
          <el-divider>选项</el-divider>
          <div class="options-list">
            <div 
              v-for="(option, index) in parsedOptions" 
              :key="index"
              class="option-item"
              :class="{ 'correct-option': option.isCorrect }"
            >
              <span class="option-label">{{ String.fromCharCode(65 + index) }}.</span>
              <span class="option-content">{{ option.content }}</span>
              <el-tag v-if="option.isCorrect" type="success" size="small">正确答案</el-tag>
            </div>
          </div>
        </div>

        <!-- 判断题答案 -->
        <div v-if="question.questionType === 'TRUE_FALSE'" class="true-false-answer">
          <el-divider>正确答案</el-divider>
          <el-tag :type="trueFalseAnswer ? 'success' : 'danger'" size="large">
            {{ trueFalseAnswer ? '正确' : '错误' }}
          </el-tag>
        </div>
      </el-card>

      <!-- 评分标准 -->
      <el-card v-if="question && isSubjectiveQuestion" class="rubric-card">
        <template #header>
          <div class="card-header">
            <span>评分标准</span>
            <el-button type="primary" size="small" @click="openRubricDialog">
              管理评分标准
            </el-button>
          </div>
        </template>

        <div v-if="rubricCriteria.length === 0" class="empty-rubric">
          <el-empty description="暂无评分标准">
            <el-button type="primary" @click="openRubricDialog">添加评分标准</el-button>
          </el-empty>
        </div>

        <div v-else class="rubric-list">
          <div 
            v-for="(criterion, index) in rubricCriteria" 
            :key="criterion.id"
            class="rubric-item"
          >
            <div class="criterion-content">
              <span class="criterion-index">{{ index + 1 }}.</span>
              <span class="criterion-text">{{ criterion.criterionText }}</span>
            </div>
            <div class="criterion-points">
              <el-tag type="primary">{{ criterion.points }} 分</el-tag>
            </div>
          </div>
          <div class="total-points">
            <strong>总分：{{ totalRubricPoints }} / {{ question.maxScore }} 分</strong>
          </div>
        </div>
      </el-card>

      <!-- 答案统计 -->
      <el-card v-if="question" class="stats-card">
        <template #header>
          <span>答案统计</span>
        </template>
        
        <el-row :gutter="20">
          <el-col :span="8">
            <div class="stat-item">
              <div class="stat-value">{{ question.totalAnswers || 0 }}</div>
              <div class="stat-label">总答案数</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-item">
              <div class="stat-value">{{ question.evaluatedAnswers || 0 }}</div>
              <div class="stat-label">已评估</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-item">
              <div class="stat-value">{{ question.averageScore ? question.averageScore.toFixed(1) : '0.0' }}</div>
              <div class="stat-label">平均分</div>
            </div>
          </el-col>
        </el-row>
      </el-card>
    </div>

    <!-- 评分标准管理对话框 -->
    <RubricManagementDialog
      v-model="rubricDialogVisible"
      :question-id="questionId"
      @refresh="loadRubricCriteria"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { questionApi } from '@/api/question'
import RubricManagementDialog from '@/components/evaluation/RubricManagementDialog.vue'
import type { QuestionResponse, RubricCriterionResponse, QuestionOption } from '@/types/api' // Changed RubricCriterion to RubricCriterionResponse

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const question = ref<QuestionResponse | null>(null)
// const rubricCriteria = ref<RubricCriterion[]>([])
const rubricCriteria = ref<RubricCriterionResponse[]>([]) // Changed type to RubricCriterionResponse
const rubricDialogVisible = ref(false)

const questionId = computed(() => {
  const id = route.params.id
  return typeof id === 'string' ? parseInt(id, 10) : Number(id)
})

const isChoiceQuestion = computed(() => {
  return question.value && ['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(question.value.questionType)
})

const isSubjectiveQuestion = computed(() => {
  return question.value && ['SHORT_ANSWER', 'ESSAY', 'CODING', 'CASE_ANALYSIS'].includes(question.value.questionType)
})

const parsedOptions = computed(() => {
  if (!question.value || !isChoiceQuestion.value || !question.value.options) return []
  
  // Assuming question.value.options is already in the correct QuestionOption[] format
  return question.value.options.map(option => ({ ...option })); 
})

const trueFalseAnswer = computed(() => {
  if (!question.value || 
      question.value.questionType !== 'TRUE_FALSE' || 
      typeof question.value.referenceAnswer === 'undefined' ||
      question.value.referenceAnswer === null) {
    return false; // Default to false if not a TRUE_FALSE question or referenceAnswer is missing/null
  }
  return question.value.referenceAnswer.toLowerCase() === 'true'
})

const totalRubricPoints = computed(() => {
  return rubricCriteria.value.reduce((total, criterion) => total + (criterion.points || 0), 0)
})

const loadQuestion = async () => {
  try {
    loading.value = true
    question.value = await questionApi.getQuestion(questionId.value)
  } catch (error) {
    console.error('Failed to load question:', error)
    ElMessage.error('加载题目详情失败')
    router.back()
  } finally {
    loading.value = false
  }
}

const loadRubricCriteria = async () => {
  if (!isSubjectiveQuestion.value) return
  
  try {
    const criteria = await questionApi.getRubricCriteria(questionId.value)
    // 转换为 RubricCriterionResponse 格式
    rubricCriteria.value = criteria.map(criterion => ({
      id: criterion.id || 0,
      criterionText: criterion.criterionText,
      points: criterion.points,
      editing: false,
      saving: false,
      originalData: { ...criterion }
    }))
  } catch (error) {
    console.error('Failed to load rubric criteria:', error)
    ElMessage.error('加载评分标准失败')
  }
}

const editQuestion = () => {
  router.push(`/questions/${questionId.value}/edit`)
}

const deleteQuestion = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个题目吗？此操作不可恢复。',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await questionApi.deleteQuestion(questionId.value)
    ElMessage.success('题目删除成功')
    router.push('/questions')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to delete question:', error)
      ElMessage.error('删除题目失败')
    }
  }
}

const goBack = () => {
  router.back()
}

const goToExam = (examId: number) => {
  router.push(`/exams/${examId}`)
}

const openRubricDialog = () => {
  rubricDialogVisible.value = true
}

const formatDate = (dateString: string) => {
  if (!dateString) return '未知'
  return new Date(dateString).toLocaleString('zh-CN')
}

const getQuestionTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'TRUE_FALSE': '判断题',
    'SHORT_ANSWER': '简答题',
    'ESSAY': '论述题',
    'CODING': '编程题',
    'CASE_ANALYSIS': '案例分析',
    'CALCULATION': '计算题'
  }
  return typeMap[type] || type
}

const getQuestionTypeTag = (type: string) => {
  const tagMap: Record<string, string> = {
    'SINGLE_CHOICE': 'primary',
    'MULTIPLE_CHOICE': 'success',
    'TRUE_FALSE': 'warning',
    'SHORT_ANSWER': 'info',
    'ESSAY': 'danger',
    'CODING': '',
    'CASE_ANALYSIS': 'success',
    'CALCULATION': 'primary'
  }
  return tagMap[type] || ''
}

onMounted(() => {
  loadQuestion()
  loadRubricCriteria()
})
</script>

<style scoped>
.question-detail {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.question-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.question-card,
.rubric-card,
.stats-card {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.question-title {
  font-size: 18px;
  font-weight: bold;
}

.question-content-text {
  padding: 20px 0;
  font-size: 16px;
  line-height: 1.6;
}

.question-options {
  margin-top: 20px;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  transition: all 0.3s;
}

.option-item.correct-option {
  background-color: #f0f9ff;
  border-color: #67c23a;
}

.option-label {
  font-weight: bold;
  color: #606266;
  min-width: 20px;
}

.option-content {
  flex: 1;
  font-size: 14px;
}

.true-false-answer {
  margin-top: 20px;
  text-align: center;
}

.empty-rubric {
  text-align: center;
  padding: 40px 0;
}

.rubric-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rubric-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
}

.criterion-content {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.criterion-index {
  font-weight: bold;
  color: #909399;
  min-width: 20px;
}

.criterion-text {
  font-size: 14px;
  line-height: 1.5;
}

.total-points {
  padding: 12px;
  text-align: right;
  background-color: #f5f7fa;
  border-radius: 6px;
  margin-top: 12px;
}

.stats-card .stat-item {
  text-align: center;
  padding: 20px 0;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}
</style>
