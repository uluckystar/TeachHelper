<template>
  <div class="edit-question">
    <div class="page-header">
      <el-breadcrumb>
        <el-breadcrumb-item :to="{ path: '/questions' }">题目库</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: `/questions/${questionId}` }">题目详情</el-breadcrumb-item>
        <el-breadcrumb-item>编辑题目</el-breadcrumb-item>
      </el-breadcrumb>
      <h1>编辑题目</h1>
    </div>

    <div v-loading="loading" class="edit-content">
      <el-form 
        ref="formRef"
        :model="form" 
        :rules="rules"
        label-width="120px"
        class="question-form"
      >
        <el-card class="form-card">        <template #header>
          <div class="card-header">
            <span>题目信息</span>
            <!-- AI助手组件 -->
            <AIQuestionAssistant
              :knowledge-base-id="form.examId"
              :question-id="questionId"
              :current-question-data="form"
              @question-filled="handleQuestionFilled"
              @rubric-updated="handleRubricUpdated"
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

          <el-form-item v-if="form.examId" label="所属考试">
            <el-input v-model="examTitle" disabled />
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
                >
                </el-button>
              </el-col>
            </el-row>
          </div>

          <div class="form-help">
            <p v-if="form.questionType === 'SINGLE_CHOICE'">请选择一个正确答案</p>
            <p v-else-if="form.questionType === 'MULTIPLE_CHOICE'">可以选择多个正确答案</p>
          </div>
        </el-card>

        <!-- 判断题答案 -->
        <el-card v-if="form.questionType === 'TRUE_FALSE'" class="form-card">
          <template #header>
            <span>正确答案</span>
          </template>
          
          <el-form-item label="答案" prop="trueFalseAnswer">
            <el-radio-group v-model="form.trueFalseAnswer">
              <el-radio :label="true">正确</el-radio>
              <el-radio :label="false">错误</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-card>

        <!-- 主观题参考答案 -->
        <el-card v-if="isSubjectiveQuestion" class="form-card">
          <template #header>
            <span>参考答案</span>
          </template>
          
          <el-form-item label="参考答案" prop="referenceAnswer">
            <el-input 
              v-model="form.referenceAnswer" 
              type="textarea" 
              :rows="4"
              placeholder="请输入参考答案（可选）"
              maxlength="1000"
              show-word-limit
            />
          </el-form-item>
        </el-card>

        <div class="form-actions">
          <el-button type="primary" :loading="submitting" @click="submitForm">
            保存修改
          </el-button>
          <el-button @click="cancelEdit">
            取消
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { questionApi } from '@/api/question'
import { examApi } from '@/api/exam'
import AIQuestionAssistant from '@/components/ai/AIQuestionAssistant.vue'
import type { QuestionCreateRequest, QuestionResponse } from '@/types/api'

// 扩展表单接口以包含所有需要的属性
interface EditQuestionForm {
  title: string
  content: string
  questionType: 'ESSAY' | 'SHORT_ANSWER' | 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'CODING' | 'CASE_ANALYSIS'
  maxScore: number
  examId: number
  options: { content: string; isCorrect: boolean }[]
  trueFalseAnswer: boolean
  referenceAnswer: string
}

const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)
const examTitle = ref('')

const questionId = computed(() => {
  const id = route.params.id
  return typeof id === 'string' ? parseInt(id, 10) : Number(id)
})

// 表单数据
const form = ref<EditQuestionForm>({
  examId: 0,
  questionType: 'SINGLE_CHOICE',
  title: '',
  content: '',
  maxScore: 10,
  options: [
    { content: '', isCorrect: true },
    { content: '', isCorrect: false }
  ],
  trueFalseAnswer: true,
  referenceAnswer: ''
})

// 单选题正确答案索引
const correctOptionIndex = ref(0)

// 计算属性
const isChoiceQuestion = computed(() => {
  return ['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(form.value.questionType)
})

const isSubjectiveQuestion = computed(() => {
  return ['SHORT_ANSWER', 'ESSAY', 'CODING', 'CASE_ANALYSIS', 'CALCULATION'].includes(form.value.questionType)
})

// 表单验证规则
const rules: FormRules = {
  questionType: [
    { required: true, message: '请选择题目类型', trigger: 'change' }
  ],
  title: [
    { required: true, message: '请输入题目标题', trigger: 'blur' },
    { min: 2, max: 200, message: '标题长度在 2 到 200 个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入题目内容', trigger: 'blur' },
    { min: 5, max: 2000, message: '内容长度在 5 到 2000 个字符', trigger: 'blur' }
  ],
  maxScore: [
    { required: true, message: '请输入满分', trigger: 'blur' },
    { type: 'number', min: 1, max: 100, message: '满分必须在 1-100 分之间', trigger: 'blur' }
  ]
}

// 监听单选题正确答案变化
watch(correctOptionIndex, (newIndex) => {
  if (form.value.questionType === 'SINGLE_CHOICE') {
    form.value.options.forEach((option, index) => {
      option.isCorrect = index === newIndex
    })
  }
})

// 加载题目数据
const loadQuestion = async () => {
  try {
    loading.value = true
    const question: QuestionResponse = await questionApi.getQuestion(questionId.value)
    
    // 填充表单数据
    form.value = {
      examId: question.examId || 0,
      questionType: question.questionType,
      title: question.title,
      content: question.content,
      maxScore: question.maxScore,
      options: [],
      trueFalseAnswer: true,
      referenceAnswer: question.referenceAnswer || ''
    }

    // 根据题目类型处理特殊数据
    if (isChoiceQuestion.value) {
      // 加载选项数据
      if (question.options && question.options.length > 0) {
        form.value.options = question.options.map(option => ({
          content: option.content,
          isCorrect: option.isCorrect
        }))
        // 设置单选题的正确答案索引
        if (question.questionType === 'SINGLE_CHOICE') {
          const correctIndex = question.options.findIndex(option => option.isCorrect)
          correctOptionIndex.value = correctIndex >= 0 ? correctIndex : 0
        }
      } else {
        // 如果没有选项数据，创建默认选项
        form.value.options = [
          { content: '', isCorrect: true },
          { content: '', isCorrect: false }
        ]
        correctOptionIndex.value = 0
      }
    } else if (question.questionType === 'TRUE_FALSE') {
      // 解析判断题答案
      form.value.trueFalseAnswer = question.referenceAnswer?.toLowerCase() === 'true'
    }

    // 如果有关联考试，加载考试信息
    if (question.examId) {
      try {
        const exam = await examApi.getExam(question.examId)
        examTitle.value = exam.title
      } catch (error) {
        console.error('Failed to load exam info:', error)
      }
    }

    if (import.meta.env.DEV) {
      console.log('Loaded question for editing:', question)
    }
  } catch (error) {
    console.error('Failed to load question:', error)
    ElMessage.error('加载题目数据失败')
    router.back()
  } finally {
    loading.value = false
  }
}

// 题目类型变化处理
const handleTypeChange = (newType: string | number | boolean | undefined) => {
  const type = newType as string
  if (import.meta.env.DEV) {
    console.log('Question type changed to:', type)
  }

  if (type === 'SINGLE_CHOICE' || type === 'MULTIPLE_CHOICE') {
    if (form.value.options.length === 0) {
      form.value.options = [
        { content: '', isCorrect: true },
        { content: '', isCorrect: false }
      ]
    }
    if (type === 'SINGLE_CHOICE') {
      correctOptionIndex.value = 0
      form.value.options.forEach((option, index) => {
        option.isCorrect = index === 0
      })
    }
  } else {
    form.value.options = []
  }
}

// 添加选项
const addOption = () => {
  form.value.options.push({ content: '', isCorrect: false })
}

// 删除选项
const removeOption = (index: number) => {
  if (form.value.options.length <= 2) {
    ElMessage.warning('至少需要保留两个选项')
    return
  }

  form.value.options.splice(index, 1)
  
  // 如果删除的是当前选中的正确答案，重新设置
  if (form.value.questionType === 'SINGLE_CHOICE' && correctOptionIndex.value >= index) {
    correctOptionIndex.value = Math.max(0, correctOptionIndex.value - 1)
  }
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    // 验证选择题是否有正确答案
    if (isChoiceQuestion.value) {
      const hasCorrectAnswer = form.value.options.some(option => option.isCorrect)
      if (!hasCorrectAnswer) {
        ElMessage.error('请设置至少一个正确答案')
        return
      }

      // 验证选项内容
      const hasEmptyOption = form.value.options.some(option => !option.content.trim())
      if (hasEmptyOption) {
        ElMessage.error('请填写所有选项内容')
        return
      }
    }

    submitting.value = true

    if (import.meta.env.DEV) {
      console.log('Updating question with data:', form.value)
    }

    // 构造符合 QuestionCreateRequest 的请求对象
    const questionRequest: QuestionCreateRequest = {
      title: form.value.title,
      content: form.value.content,
      questionType: form.value.questionType,
      maxScore: form.value.maxScore,
      examId: form.value.examId,
      referenceAnswer: form.value.referenceAnswer
    }
    
    // 添加选择题选项
    if (isChoiceQuestion.value) {
      questionRequest.options = form.value.options.map((option, index) => ({
        content: option.content,
        isCorrect: option.isCorrect,
        optionOrder: index
      }))
    } else if (form.value.questionType === 'TRUE_FALSE') {
      // 对于判断题，将答案存储在referenceAnswer中
      questionRequest.referenceAnswer = form.value.trueFalseAnswer ? 'true' : 'false'
    }

    await questionApi.updateQuestion(questionId.value, questionRequest)
    
    ElMessage.success('题目更新成功')
    router.push(`/questions/${questionId.value}`)
  } catch (error: any) {
    console.error('Failed to update question:', error)
    if (error.response?.data?.message) {
      ElMessage.error(`更新失败：${error.response.data.message}`)
    } else {
      ElMessage.error('题目更新失败')
    }
  } finally {
    submitting.value = false
  }
}

// 取消编辑
const cancelEdit = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要取消编辑吗？未保存的修改将丢失。',
      '确认取消',
      {
        confirmButtonText: '确定',
        cancelButtonText: '继续编辑',
        type: 'warning'
      }
    )
    router.back()
  } catch {
    // 用户取消操作
  }
}

// AI助手事件处理
const handleQuestionFilled = (questionData: any) => {
  // 将AI生成的题目数据填充到表单中
  if (questionData.title) form.value.title = questionData.title
  if (questionData.content) form.value.content = questionData.content
  if (questionData.questionType) form.value.questionType = questionData.questionType
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

const handleRubricUpdated = () => {
  ElMessage.success('评分标准已更新')
}

onMounted(() => {
  loadQuestion()
})
</script>

<style scoped>
.edit-question {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 10px 0;
  font-size: 24px;
  font-weight: bold;
}

.edit-content {
  max-width: 1000px;
}

.question-form {
  width: 100%;
}

.form-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.option-item {
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background-color: #fafafa;
}

.form-help {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.form-actions {
  text-align: center;
  padding: 20px 0;
  border-top: 1px solid #e4e7ed;
  margin-top: 20px;
}

.form-actions .el-button {
  margin: 0 10px;
  min-width: 100px;
}
</style>
