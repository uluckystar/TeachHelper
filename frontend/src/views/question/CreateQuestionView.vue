<template>
  <div class="create-question">
    <div class="page-header">
      <el-breadcrumb>
        <el-breadcrumb-item :to="{ path: '/questions' }">题目库</el-breadcrumb-item>
        <el-breadcrumb-item>创建题目</el-breadcrumb-item>
      </el-breadcrumb>
      <h1>创建题目</h1>
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
            <span>基本信息</span>
            <!-- AI助手组件 -->
            <AIQuestionAssistant
              :knowledge-base-id="form.examId"
              :current-question-data="form"
              :show-rubric-generation="false"
              @question-filled="handleQuestionFilled"
              @question-generated="handleQuestionGenerated"
            />
          </div>
        </template>
        
        <el-form-item label="所属考试" prop="examId">
          <el-select v-model="form.examId" placeholder="请选择考试" style="width: 100%">
            <el-option 
              v-for="exam in exams" 
              :key="exam.id" 
              :label="exam.title" 
              :value="exam.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="题目类型" prop="questionType">
          <el-radio-group v-model="form.questionType" @change="handleTypeChange">
            <el-radio label="SINGLE_CHOICE">单选题</el-radio>
            <el-radio label="MULTIPLE_CHOICE">多选题</el-radio>
            <el-radio label="TRUE_FALSE">判断题</el-radio>
            <el-radio label="SHORT_ANSWER">简答题</el-radio>
            <el-radio label="ESSAY">论述题</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="题目标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入题目标题" />
        </el-form-item>

        <el-form-item label="题目内容" prop="content">
          <el-input 
            v-model="form.content" 
            type="textarea" 
            :rows="6"
            placeholder="请输入题目内容"
          />
        </el-form-item>

        <el-form-item label="满分" prop="maxScore">
          <el-input-number 
            v-model="form.maxScore" 
            :min="1" 
            :max="100"
            placeholder="请输入满分"
          />
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
                placeholder="请输入选项内容"
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Delete } from '@element-plus/icons-vue'
import { questionApi } from '@/api/question'
import { examApi } from '@/api/exam'
import { ElMessage } from 'element-plus'
import AIQuestionAssistant from '@/components/ai/AIQuestionAssistant.vue'
import type { QuestionCreateRequest } from '@/types/api'

const router = useRouter()
const formRef = ref()
const saving = ref(false)
const exams = ref<{ id: number; title: string }[]>([])
const correctOptionIndex = ref(0)

const form = ref({
  examId: 0,
  questionType: 'SINGLE_CHOICE' as 'ESSAY' | 'SHORT_ANSWER' | 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'CODING' | 'CASE_ANALYSIS',
  title: '',
  content: '',
  maxScore: 10,
  options: [
    { content: '', isCorrect: false },
    { content: '', isCorrect: false }
  ],
  blankAnswers: [
    { content: '' }
  ],
  gradingCriteria: '',
  referenceAnswer: ''
})

const rules = {
  examId: [
    { required: true, message: '请选择考试', trigger: 'change' }
  ],
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
  return ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE'].includes(form.value.questionType)
})

const isSubjectiveQuestion = computed(() => {
  return ['SHORT_ANSWER', 'ESSAY'].includes(form.value.questionType)
})

const loadExams = async () => {
  try {
    const response = await examApi.getExams()
    exams.value = response.data || []
  } catch (error) {
    console.error('加载考试列表失败:', error)
    ElMessage.error('加载考试列表失败')
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
    form.value.options = [
      { content: '正确', isCorrect: true },
      { content: '错误', isCorrect: false }
    ]
    correctOptionIndex.value = 0
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
      const trueFalseAnswer = form.value.options.find(option => option.isCorrect)
      questionRequest.referenceAnswer = trueFalseAnswer?.content === '正确' ? 'true' : 'false'
    }
    
    await questionApi.createQuestion(questionRequest)
    ElMessage.success('题目创建成功')
    router.push('/questions')
    
  } catch (error) {
    console.error('保存题目失败:', error)
    ElMessage.error('保存题目失败')
  } finally {
    saving.value = false
  }
}

const goBack = () => {
  router.back()
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
  loadExams()
})
</script>

<style scoped>
.create-question {
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 8px 0 0 0;
  font-size: 24px;
  font-weight: 600;
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

.option-item,
.blank-answer-item {
  margin-bottom: 16px;
}

.option-item:last-child,
.blank-answer-item:last-child {
  margin-bottom: 0;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 24px 0;
}
</style>
