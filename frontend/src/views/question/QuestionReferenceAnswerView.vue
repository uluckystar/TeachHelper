<template>
    <div class="question-reference-answer">
      <!-- 页面头部 -->
      <div class="page-header">
        <div class="header-left">
          <el-button :icon="ArrowLeft" @click="goBack" plain>返回</el-button>
          <div class="breadcrumb">
            <span>题目详情</span>
            <el-divider direction="vertical" />
            <span>参考答案管理</span>
          </div>
        </div>
        <div class="header-right">
          <el-button 
            type="primary" 
            :icon="MagicStick"
            @click="generateReferenceAnswer"
            :loading="generating"
          >
            AI生成参考答案
          </el-button>
        </div>
      </div>
  
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>
  
      <!-- 题目信息 -->
      <el-card v-if="!loading && question" class="question-info-card">
        <template #header>
          <div class="card-header">
            <span>题目信息</span>
            <el-tag :type="getQuestionTypeTag(question.questionType)" size="large">
              {{ getQuestionTypeText(question.questionType) }}
            </el-tag>
          </div>
        </template>
        
        <div class="question-content">
          <div class="question-title">
            <h3>{{ question.title }}</h3>
          </div>
          
          <div class="question-meta">
            <el-descriptions :column="3" border>
              <el-descriptions-item label="题目类型">
                <el-tag :type="getQuestionTypeTag(question.questionType)">
                  {{ getQuestionTypeText(question.questionType) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="满分">{{ question.maxScore }}分</el-descriptions-item>
              <el-descriptions-item label="关联考试">
                <el-link v-if="question.examId" type="primary" @click="goToExam(question.examId)">
                  查看考试
                </el-link>
                <span v-else>无</span>
              </el-descriptions-item>
            </el-descriptions>
          </div>
  
          <div class="question-text">
            <h4>题目内容：</h4>
            <div class="content-box" v-html="question.content"></div>
          </div>
  
          <!-- 选择题选项 -->
          <div v-if="isChoiceQuestion && question.options && question.options.length > 0" class="question-options">
            <h4>选项：</h4>
            <div class="options-list">
              <div 
                v-for="option in question.options" 
                :key="option.id"
                class="option-item"
                :class="{ 'correct': option.isCorrect }"
              >
                <span class="option-label">{{ String.fromCharCode(65 + (option.optionOrder || 0)) }}</span>
                <span class="option-text">{{ option.content }}</span>
                <el-icon v-if="option.isCorrect" class="correct-icon"><Check /></el-icon>
              </div>
            </div>
          </div>
        </div>
      </el-card>
  
      <!-- 参考答案管理 -->
      <el-card v-if="!loading && question" class="reference-answer-card">
        <template #header>
          <div class="card-header">
            <span>参考答案管理</span>
            <div class="header-actions">
              <el-tag v-if="hasReferenceAnswer" type="success" size="large">已设置</el-tag>
              <el-tag v-else type="info" size="large">未设置</el-tag>
            </div>
          </div>
        </template>
  
        <!-- 参考答案编辑器 -->
        <div class="answer-editor">
          <el-form ref="answerFormRef" :model="answerForm" :rules="answerRules">
            <el-form-item label="参考答案" prop="referenceAnswer">
              <el-input
                v-model="answerForm.referenceAnswer"
                type="textarea"
                :rows="12"
                placeholder="请输入参考答案..."
                maxlength="4000"
                show-word-limit
              />
            </el-form-item>
          </el-form>
  
          <div class="editor-actions">
            <el-button type="primary" @click="saveReferenceAnswer" :loading="saving">
              保存参考答案
            </el-button>
            <el-button @click="resetForm">重置</el-button>
            <el-button 
              v-if="hasReferenceAnswer" 
              type="danger" 
              @click="deleteReferenceAnswer"
              :loading="deleting"
            >
              删除参考答案
            </el-button>
          </div>
        </div>
      </el-card>
  
      <!-- AI生成进度 -->
      <el-card v-if="generating" class="ai-progress-card">
        <template #header>
          <span>AI生成参考答案中...</span>
        </template>
        <div class="ai-progress">
          <el-progress 
            :percentage="aiProgress" 
            :format="formatProgress"
            status="success"
            :indeterminate="true"
          />
          <p class="progress-text">{{ aiProgressText }}</p>
        </div>
      </el-card>
    </div>
  </template>
  
  <script setup lang="ts">
  import { ref, computed, onMounted, reactive } from 'vue'
  import { useRouter, useRoute } from 'vue-router'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import type { FormInstance, FormRules } from 'element-plus'
  import {
    ArrowLeft,
    MagicStick,
    Check,
    Edit,
    Delete,
    Refresh
  } from '@element-plus/icons-vue'
  
  import { questionApi } from '@/api/question'
  import type { QuestionResponse, QuestionCreateRequest } from '@/types/api'
  
  const router = useRouter()
  const route = useRoute()
  const answerFormRef = ref<FormInstance>()
  
  // 数据状态
  const loading = ref(false)
  const saving = ref(false)
  const deleting = ref(false)
  const generating = ref(false)
  const aiProgress = ref(0)
  const aiProgressText = ref('')
  
  const question = ref<QuestionResponse | null>(null)
  
  // 表单数据
  const answerForm = reactive({
    referenceAnswer: ''
  })
  
  // 表单验证规则
  const answerRules: FormRules = {
    referenceAnswer: [
      { required: true, message: '请输入参考答案', trigger: 'blur' },
      { min: 5, message: '参考答案不能少于5个字符', trigger: 'blur' }
    ]
  }
  
  // 计算属性
  const questionId = computed(() => {
    const id = route.params.id
    return typeof id === 'string' ? parseInt(id, 10) : Number(id)
  })
  
  const isChoiceQuestion = computed(() => {
    return question.value && ['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(question.value.questionType)
  })
  
  const isSubjectiveQuestion = computed(() => {
    return question.value && ['SHORT_ANSWER', 'ESSAY', 'CODING', 'CASE_ANALYSIS', 'CALCULATION'].includes(question.value.questionType)
  })
  
  const hasReferenceAnswer = computed(() => {
    return question.value?.referenceAnswer && question.value.referenceAnswer.trim().length > 0
  })
  
  // 方法
  const loadQuestion = async () => {
    try {
      loading.value = true
      question.value = await questionApi.getQuestion(questionId.value)
      
      // 填充表单
      answerForm.referenceAnswer = question.value.referenceAnswer || ''
      
    } catch (error) {
      console.error('Failed to load question:', error)
      ElMessage.error('加载题目详情失败')
      router.back()
    } finally {
      loading.value = false
    }
  }
  
  const generateReferenceAnswer = async () => {
    if (!question.value) return
  
    try {
      generating.value = true
      aiProgress.value = 0
      aiProgressText.value = '正在生成参考答案...'
  
      // 模拟进度
      const progressInterval = setInterval(() => {
        if (aiProgress.value < 90) {
          aiProgress.value += Math.random() * 10
        }
      }, 500)
  
      const response = await questionApi.generateReferenceAnswer(questionId.value)
      
      clearInterval(progressInterval)
      aiProgress.value = 100
      aiProgressText.value = '参考答案生成完成！'
  
      // 更新表单和题目数据
      answerForm.referenceAnswer = response.referenceAnswer
      if (question.value) {
        question.value.referenceAnswer = response.referenceAnswer
      }
  
      ElMessage.success('AI参考答案生成成功')
      
    } catch (error) {
      console.error('Failed to generate reference answer:', error)
      ElMessage.error('AI参考答案生成失败')
    } finally {
      setTimeout(() => {
        generating.value = false
        aiProgress.value = 0
        aiProgressText.value = ''
      }, 1000)
    }
  }
  
  const saveReferenceAnswer = async () => {
    if (!answerFormRef.value || !question.value) return
  
    try {
      await answerFormRef.value.validate()
      
      saving.value = true
      
      // 构造更新请求
      const updateRequest: QuestionCreateRequest = {
        title: question.value.title,
        content: question.value.content,
        questionType: question.value.questionType,
        maxScore: question.value.maxScore,
        examId: question.value.examId || 0,
        referenceAnswer: answerForm.referenceAnswer,
        options: question.value.options
      }
  
      await questionApi.updateQuestion(questionId.value, updateRequest)
      
      // 更新本地数据
      question.value.referenceAnswer = answerForm.referenceAnswer
      
      ElMessage.success('参考答案保存成功')
      
    } catch (error) {
      console.error('Failed to save reference answer:', error)
      ElMessage.error('保存参考答案失败')
    } finally {
      saving.value = false
    }
  }
  
  const deleteReferenceAnswer = async () => {
    if (!question.value) return
  
    try {
      await ElMessageBox.confirm(
        '确定要删除这个参考答案吗？此操作不可恢复。',
        '确认删除',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
  
      deleting.value = true
  
      // 构造更新请求（清空参考答案）
      const updateRequest: QuestionCreateRequest = {
        title: question.value.title,
        content: question.value.content,
        questionType: question.value.questionType,
        maxScore: question.value.maxScore,
        examId: question.value.examId || 0,
        referenceAnswer: '',
        options: question.value.options
      }
  
      await questionApi.updateQuestion(questionId.value, updateRequest)
      
      // 更新本地数据
      question.value.referenceAnswer = ''
      answerForm.referenceAnswer = ''
      
      ElMessage.success('参考答案删除成功')
      
    } catch (error) {
      if (error !== 'cancel') {
        console.error('Failed to delete reference answer:', error)
        ElMessage.error('删除参考答案失败')
      }
    } finally {
      deleting.value = false
    }
  }
  
  const resetForm = () => {
    answerForm.referenceAnswer = question.value?.referenceAnswer || ''
  }
  
  const goBack = () => {
    router.back()
  }
  
  const goToExam = (examId: number) => {
    router.push(`/exams/${examId}`)
  }
  
  const getQuestionTypeText = (type: string) => {
    const typeMap: { [key: string]: string } = {
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
  
  const getQuestionTypeTag = (type: string) => {
    const tagMap: { [key: string]: string } = {
      'SINGLE_CHOICE': 'primary',
      'MULTIPLE_CHOICE': 'success',
      'TRUE_FALSE': 'warning',
      'SHORT_ANSWER': 'info',
      'ESSAY': 'danger',
      'CODING': 'success',
      'CASE_ANALYSIS': 'warning',
      'CALCULATION': 'primary'
    }
    return tagMap[type] || 'info'
  }
  
  const formatProgress = (percentage: number) => {
    return `${percentage.toFixed(0)}%`
  }
  
  // 生命周期
  onMounted(() => {
    loadQuestion()
  })
  </script>
  
  <style scoped>
  .question-reference-answer {
    padding: 20px;
    max-width: 1200px;
    margin: 0 auto;
  }
  
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 1px solid #e4e7ed;
  }
  
  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
  }
  
  .breadcrumb {
    font-size: 16px;
    color: #606266;
    display: flex;
    align-items: center;
    gap: 8px;
  }
  
  .loading-container {
    margin: 20px 0;
  }
  
  .question-info-card,
  .reference-answer-card,
  .ai-progress-card {
    margin-bottom: 20px;
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .question-content {
    padding: 0;
  }
  
  .question-title h3 {
    margin: 0 0 16px 0;
    font-size: 20px;
    font-weight: bold;
    color: #303133;
  }
  
  .question-meta {
    margin-bottom: 20px;
  }
  
  .question-text h4 {
    margin: 16px 0 8px 0;
    color: #606266;
    font-size: 14px;
  }
  
  .content-box {
    padding: 12px;
    background-color: #f5f7fa;
    border-radius: 6px;
    border: 1px solid #e4e7ed;
    line-height: 1.6;
  }
  
  .question-options {
    margin-top: 20px;
  }
  
  .question-options h4 {
    margin: 0 0 12px 0;
    color: #606266;
    font-size: 14px;
  }
  
  .options-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  
  .option-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px;
    background-color: #f5f7fa;
    border-radius: 6px;
    border: 1px solid #e4e7ed;
    transition: all 0.3s;
  }
  
  .option-item.correct {
    background-color: #f0f9ff;
    border-color: #67c23a;
  }
  
  .option-label {
    min-width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #909399;
    color: white;
    border-radius: 50%;
    font-size: 12px;
    font-weight: bold;
  }
  
  .option-item.correct .option-label {
    background-color: #67c23a;
  }
  
  .option-text {
    flex: 1;
    line-height: 1.5;
  }
  
  .correct-icon {
    color: #67c23a;
    font-size: 16px;
  }
  
  .answer-editor {
    padding: 0;
  }
  
  .editor-actions {
    margin-top: 20px;
    text-align: center;
    padding-top: 20px;
    border-top: 1px solid #e4e7ed;
  }
  
  .editor-actions .el-button {
    margin: 0 8px;
  }
  
  .ai-progress {
    text-align: center;
  }
  
  .progress-text {
    margin-top: 12px;
    color: #606266;
    font-size: 14px;
  }
  
  .header-actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  
  /* 响应式设计 */
  @media (max-width: 768px) {
    .question-reference-answer {
      padding: 16px;
    }
    
    .page-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
    }
    
    .header-left {
      width: 100%;
    }
    
    .header-right {
      width: 100%;
    }
    
    .editor-actions {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    
    .editor-actions .el-button {
      margin: 0;
      width: 100%;
    }
  }
  </style>