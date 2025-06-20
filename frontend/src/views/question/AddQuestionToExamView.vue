<template>
  <div class="add-question-to-exam">
    <div class="page-header">
      <el-breadcrumb>
        <el-breadcrumb-item :to="{ path: '/exams' }">考试列表</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: `/exams/${examId}` }">
          {{ exam?.title || '考试详情' }}
        </el-breadcrumb-item>
        <el-breadcrumb-item>添加题目</el-breadcrumb-item>
      </el-breadcrumb>
      <h1>向考试添加题目</h1>
      <p v-if="exam" class="exam-info">考试：{{ exam.title }}</p>
    </div>

    <el-form 
      ref="formRef"
      :model="form" 
      :rules="rules"
      label-width="120px"
      class="question-form"
    >
      <el-card class="form-card">
        <template #header>
          <div class="card-header">
            <span>题目信息</span>
            <!-- AI助手组件 -->
            <AIQuestionAssistant
              :knowledge-base-id="examId"
              :current-question-data="form"
              :show-rubric-generation="false"
              @question-filled="handleQuestionFilled"
              @question-generated="handleQuestionGenerated"
            />
          </div>
        </template>
        
        <el-form-item label="题目类型" prop="questionType">
          <el-radio-group v-model="form.questionType" @change="handleTypeChange">
            <el-radio label="SINGLE_CHOICE">单选题</el-radio>
            <el-radio label="MULTIPLE_CHOICE">多选题</el-radio>
            <el-radio label="TRUE_FALSE">判断题</el-radio>
            <el-radio label="SHORT_ANSWER">简答题</el-radio>
            <el-radio label="ESSAY">论述题</el-radio>
            <el-radio label="CODING">编程题</el-radio>
            <el-radio label="CASE_ANALYSIS">案例分析</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="题目标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入题目标题"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="题目内容" prop="content">
          <el-input 
            v-model="form.content" 
            type="textarea" 
            :rows="6"
            placeholder="请输入题目内容"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="满分" prop="maxScore">
          <el-input-number 
            v-model="form.maxScore" 
            :min="1" 
            :max="100"
            :precision="1"
            placeholder="请输入满分"
          />
          <span class="form-help">分</span>
        </el-form-item>
      </el-card>

      <!-- 选择题选项 -->
      <el-card v-if="isChoiceQuestion" class="form-card">
        <template #header>
          <div class="card-header">
            <span>选项设置</span>
            <el-button type="primary" size="small" icon="Plus" @click="addOption">
              添加选项
            </el-button>
          </div>
        </template>

        <div v-for="(option, index) in form.options" :key="index" class="option-item">
          <el-row :gutter="16" align="middle">
            <el-col :span="2">
              <el-checkbox 
                v-if="form.questionType === 'MULTIPLE_CHOICE'"
                v-model="option.isCorrect"
              />
              <el-radio 
                v-else
                v-model="correctOptionIndex"
                :label="index"
              />
            </el-col>
            <el-col :span="20">
              <el-input 
                v-model="option.content" 
                :placeholder="`选项 ${String.fromCharCode(65 + index)}`"
              />
            </el-col>
            <el-col :span="2">
              <el-button 
                type="danger" 
                size="small" 
                icon="Delete"
                @click="removeOption(index)"
                :disabled="form.options.length <= 2"
              />
            </el-col>
          </el-row>
        </div>
      </el-card>

      <!-- 判断题选项 -->
      <el-card v-if="form.questionType === 'TRUE_FALSE'" class="form-card">
        <template #header>
          <span>正确答案</span>
        </template>
        
        <el-form-item>
          <el-radio-group v-model="form.trueFalseAnswer">
            <el-radio :label="true">正确</el-radio>
            <el-radio :label="false">错误</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-card>

      <!-- 主观题评分标准 -->
      <el-card v-if="isSubjectiveQuestion" class="form-card">
        <template #header>
          <span>评分标准</span>
        </template>
        
        <el-form-item label="评分标准">
          <el-input 
            v-model="form.gradingCriteria" 
            type="textarea" 
            :rows="4"
            placeholder="请输入评分标准，用于AI评估参考"
          />
        </el-form-item>

        <el-form-item label="参考答案">
          <el-input 
            v-model="form.referenceAnswer" 
            type="textarea" 
            :rows="4"
            placeholder="请输入参考答案"
          />
        </el-form-item>
      </el-card>

      <div class="form-actions">
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" @click="saveQuestion" :loading="saving">
          保存题目
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Plus, Delete } from '@element-plus/icons-vue'
import { questionApi } from '@/api/question'
import { examApi } from '@/api/exam'
import { ElMessage } from 'element-plus'
import type { QuestionCreateRequest, ExamResponse } from '@/types/api'
import AIQuestionAssistant from '@/components/ai/AIQuestionAssistant.vue'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const saving = ref(false)
const exam = ref<ExamResponse | null>(null)
const correctOptionIndex = ref(0)

// 从路由参数获取考试ID
const examId = computed(() => {
  const id = route.params.examId
  const parsedId = typeof id === 'string' ? parseInt(id, 10) : Number(id)
  return isNaN(parsedId) ? 0 : parsedId
})

const form = ref<QuestionCreateRequest & { 
  options: Array<{ content: string; isCorrect: boolean }>;
  trueFalseAnswer: boolean;
  gradingCriteria: string;
  referenceAnswer: string;
}>({
  examId: 0, // 先设置为0，后面通过watcher更新
  questionType: 'SINGLE_CHOICE',
  title: '',
  content: '',
  maxScore: 10,
  options: [
    { content: '', isCorrect: false },
    { content: '', isCorrect: false }
  ],
  trueFalseAnswer: true,
  gradingCriteria: '',
  referenceAnswer: ''
})

// 监听examId变化，确保form中的examId正确设置
watch(examId, (newExamId) => {
  if (newExamId && newExamId > 0) {
    form.value.examId = newExamId
    console.log('Form examId updated to:', newExamId)
  }
}, { immediate: true })

const rules = {
  questionType: [
    { required: true, message: '请选择题目类型', trigger: 'change' }
  ],
  title: [
    { required: true, message: '请输入题目标题', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入题目内容', trigger: 'blur' }
  ],
  maxScore: [
    { required: true, message: '请输入满分', trigger: 'blur' }
  ]
}

const isChoiceQuestion = computed(() => {
  return ['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(form.value.questionType)
})

const isSubjectiveQuestion = computed(() => {
  return ['SHORT_ANSWER', 'ESSAY', 'CODING', 'CASE_ANALYSIS'].includes(form.value.questionType)
})

const loadExam = async () => {
  try {
    exam.value = await examApi.getExam(examId.value)
  } catch (error) {
    console.error('加载考试信息失败:', error)
    ElMessage.error('加载考试信息失败')
  }
}

const handleTypeChange = () => {
  // 重置相关字段
  if (isChoiceQuestion.value) {
    form.value.options = [
      { content: '', isCorrect: false },
      { content: '', isCorrect: false }
    ]
    correctOptionIndex.value = 0
  } else if (form.value.questionType === 'TRUE_FALSE') {
    form.value.trueFalseAnswer = true
  }
}

const addOption = () => {
  form.value.options.push({ content: '', isCorrect: false })
}

const removeOption = (index: number) => {
  if (form.value.options.length > 2) {
    form.value.options.splice(index, 1)
    // 调整正确答案索引
    if (correctOptionIndex.value >= index && correctOptionIndex.value > 0) {
      correctOptionIndex.value--
    }
  }
}

const saveQuestion = async () => {
  try {
    await formRef.value.validate()
    
    saving.value = true
    
    console.log('Submitting form with data:', {
      examId: form.value.examId,
      routeExamId: examId.value,
      title: form.value.title,
      questionType: form.value.questionType
    })
    
    // 验证examId
    if (!form.value.examId || form.value.examId <= 0) {
      ElMessage.error('考试ID无效，请刷新页面重试')
      return
    }
    
    // 处理选择题正确答案
    if (form.value.questionType === 'SINGLE_CHOICE') {
      form.value.options.forEach((option, index) => {
        option.isCorrect = index === correctOptionIndex.value
      })
    }
    
    // 验证选择题至少有一个正确答案
    if (isChoiceQuestion.value) {
      const hasCorrectAnswer = form.value.options.some(option => option.isCorrect)
      if (!hasCorrectAnswer) {
        ElMessage.warning('请至少选择一个正确答案')
        return
      }
      
      // 验证选项内容不为空
      const hasEmptyOption = form.value.options.some(option => !option.content.trim())
      if (hasEmptyOption) {
        ElMessage.warning('请填写所有选项内容')
        return
      }
    }
    
    // 准备提交的数据
    const questionData: QuestionCreateRequest = {
      examId: form.value.examId,
      title: form.value.title,
      content: form.value.content,
      questionType: form.value.questionType,
      maxScore: form.value.maxScore
    }
    
    console.log('Final question data to submit:', questionData)
    
    await questionApi.createQuestion(questionData)
    ElMessage.success('题目添加成功')
    router.push(`/exams/${examId.value}`)
    
  } catch (error) {
    console.error('保存题目失败:', error)
    ElMessage.error('保存题目失败')
  } finally {
    saving.value = false
  }
}

const goBack = () => {
  router.push(`/exams/${examId.value}`)
}

// AI助手事件处理
const handleQuestionFilled = (questionData: any) => {
  // 将AI生成的题目数据填充到表单中
  if (questionData.title) form.value.title = questionData.title
  if (questionData.content) form.value.content = questionData.content
  if (questionData.questionType) {
    form.value.questionType = questionData.questionType
    handleTypeChange() // 触发类型变化处理
  }
  if (questionData.maxScore) form.value.maxScore = questionData.maxScore
  if (questionData.referenceAnswer) form.value.referenceAnswer = questionData.referenceAnswer
  
  // 处理选择题选项
  if (questionData.options && questionData.options.length > 0) {
    form.value.options = questionData.options.map((opt: any) => ({
      content: opt.content || opt.text || '',
      isCorrect: opt.isCorrect || false
    }))
  }
  
  ElMessage.success('AI智能填充完成，请检查并完善内容')
}

const handleQuestionGenerated = (questionData: any) => {
  // 处理完整的AI生成题目
  handleQuestionFilled(questionData)
  ElMessage.success('AI题目生成完成，已自动填充到表单')
}

onMounted(() => {
  loadExam()
})
</script>

<style scoped>
.add-question-to-exam {
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 8px 0 4px 0;
  font-size: 24px;
  font-weight: 600;
}

.exam-info {
  color: #606266;
  font-size: 14px;
  margin: 0;
}

.question-form {
  margin-bottom: 24px;
}

.form-card {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.option-item {
  margin-bottom: 16px;
}

.option-item:last-child {
  margin-bottom: 0;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 24px 0;
}

.form-help {
  color: #909399;
  font-size: 12px;
  margin-left: 8px;
}
</style>
