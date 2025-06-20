<template>
  <div class="generate-questions-view">
    <div class="page-header">
      <el-breadcrumb>
        <el-breadcrumb-item :to="{ path: '/exams' }">考试列表</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: `/exams/${examId}` }">{{ exam?.title }}</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: `/exams/${examId}/questions` }">题目管理</el-breadcrumb-item>
        <el-breadcrumb-item>AI生成题目</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <el-row :gutter="24">
      <!-- 生成配置 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <el-icon class="header-icon"><MagicStick /></el-icon>
              <span>AI题目生成配置</span>
            </div>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
            <el-form-item label="题目类型" prop="questionType">
              <el-select v-model="form.questionType" placeholder="请选择题目类型" style="width: 100%">
                <el-option label="选择题" value="MULTIPLE_CHOICE" />
                <el-option label="判断题" value="TRUE_FALSE" />
                <el-option label="填空题" value="FILL_BLANK" />
                <el-option label="简答题" value="SHORT_ANSWER" />
                <el-option label="论述题" value="ESSAY" />
              </el-select>
            </el-form-item>

            <el-form-item label="题目数量" prop="count">
              <el-input-number v-model="form.count" :min="1" :max="20" />
            </el-form-item>

            <el-form-item label="难度级别" prop="difficulty">
              <el-select v-model="form.difficulty" placeholder="请选择难度级别" style="width: 100%">
                <el-option label="简单" value="EASY" />
                <el-option label="中等" value="MEDIUM" />
                <el-option label="困难" value="HARD" />
              </el-select>
            </el-form-item>

            <el-form-item label="知识点" prop="knowledgePoints">
              <el-select 
                v-model="form.knowledgePoints" 
                multiple 
                placeholder="请选择相关知识点"
                style="width: 100%"
              >
                <el-option
                  v-for="point in knowledgePoints"
                  :key="point.id"
                  :label="point.name"
                  :value="point.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="题目主题" prop="topic">
              <el-input
                v-model="form.topic"
                type="textarea"
                :rows="3"
                placeholder="请描述题目的主题或要求（可选）"
              />
            </el-form-item>

            <el-form-item label="分值" prop="score">
              <el-input-number v-model="form.score" :min="1" :max="100" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="generateQuestions" :loading="generating">
                <el-icon><MagicStick /></el-icon>
                生成题目
              </el-button>
              <el-button @click="resetForm">重置</el-button>
              <el-button @click="goBack">返回</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 生成结果预览 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <el-icon class="header-icon"><Document /></el-icon>
              <span>生成预览</span>
            </div>
          </template>

          <div v-if="!generatedQuestions.length" class="empty-preview">
            <el-empty description="暂无生成的题目" />
          </div>

          <div v-else class="preview-list">
            <div 
              v-for="(question, index) in generatedQuestions" 
              :key="index"
              class="preview-item"
            >
              <div class="question-header">
                <el-tag :type="getQuestionTypeColor(question.type)" size="small">
                  {{ getQuestionTypeName(question.type) }}
                </el-tag>
                <el-tag type="info" size="small">{{ question.difficulty }}</el-tag>
              </div>
              <div class="question-content">
                <strong>题目：</strong>{{ question.content }}
              </div>
              <div v-if="question.options" class="question-options">
                <div v-for="(option, optIndex) in question.options" :key="optIndex">
                  {{ String.fromCharCode(65 + optIndex) }}. {{ option }}
                </div>
              </div>
              <div class="question-answer">
                <strong>答案：</strong>{{ question.answer }}
              </div>
            </div>

            <div class="preview-actions">
              <el-button type="success" @click="saveQuestions" :loading="saving">
                保存所有题目
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, Document } from '@element-plus/icons-vue'
import { examApi } from '@/api/exam'
import { questionApi } from '@/api/question'
import type { FormInstance } from 'element-plus'

const route = useRoute()
const router = useRouter()

const examId = computed(() => Number(route.params.examId))

// 响应式数据
const formRef = ref<FormInstance>()
const generating = ref(false)
const saving = ref(false)
const exam = ref<any>(null)
const knowledgePoints = ref<any[]>([])
const generatedQuestions = ref<any[]>([])

const form = reactive({
  questionType: '',
  count: 5,
  difficulty: 'MEDIUM',
  knowledgePoints: [] as number[],
  topic: '',
  score: 10
})

const rules = {
  questionType: [
    { required: true, message: '请选择题目类型', trigger: 'change' }
  ],
  count: [
    { required: true, message: '请输入题目数量', trigger: 'blur' }
  ],
  difficulty: [
    { required: true, message: '请选择难度级别', trigger: 'change' }
  ]
}

// 方法
const generateQuestions = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    
    generating.value = true
    
    // 调用AI生成题目API
    const response = await questionApi.generateQuestions({
      examId: examId.value,
      ...form
    })
    
    generatedQuestions.value = response.data
    ElMessage.success(`成功生成 ${response.data.length} 道题目`)
    
  } catch (error: any) {
    console.error('生成题目失败:', error)
    ElMessage.error(error.message || '生成题目失败')
  } finally {
    generating.value = false
  }
}

const saveQuestions = async () => {
  if (!generatedQuestions.value.length) {
    ElMessage.warning('没有可保存的题目')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要保存这 ${generatedQuestions.value.length} 道题目到考试中吗？`,
      '确认保存',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    saving.value = true

    await questionApi.saveGeneratedQuestions({
      examId: examId.value,
      questions: generatedQuestions.value
    })

    ElMessage.success('题目保存成功')
    router.push(`/exams/${examId.value}/questions`)

  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('保存题目失败:', error)
      ElMessage.error(error.message || '保存题目失败')
    }
  } finally {
    saving.value = false
  }
}

const resetForm = () => {
  formRef.value?.resetFields()
  generatedQuestions.value = []
}

const goBack = () => {
  router.push(`/exams/${examId.value}/questions`)
}

const getQuestionTypeName = (type: string) => {
  const types: Record<string, string> = {
    'MULTIPLE_CHOICE': '选择题',
    'TRUE_FALSE': '判断题',
    'FILL_BLANK': '填空题',
    'SHORT_ANSWER': '简答题',
    'ESSAY': '论述题'
  }
  return types[type] || type
}

const getQuestionTypeColor = (type: string): 'success' | 'primary' | 'warning' | 'info' | 'danger' => {
  const colors: Record<string, 'success' | 'primary' | 'warning' | 'info' | 'danger'> = {
    'MULTIPLE_CHOICE': 'primary',
    'TRUE_FALSE': 'success',
    'FILL_BLANK': 'warning',
    'SHORT_ANSWER': 'info',
    'ESSAY': 'danger'
  }
  return colors[type] || 'info'
}

// 初始化
onMounted(async () => {
  try {
    // 加载考试信息
    const examResponse = await examApi.getExam(examId.value)
    exam.value = examResponse

    // 加载知识点列表
    const knowledgeResponse = await questionApi.getKnowledgePoints()
    knowledgePoints.value = knowledgeResponse.data
    
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  }
})
</script>

<style scoped>
.generate-questions-view {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.header-icon {
  color: #409eff;
  font-size: 18px;
}

.empty-preview {
  padding: 40px 20px;
  text-align: center;
}

.preview-list {
  max-height: 600px;
  overflow-y: auto;
}

.preview-item {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 16px;
  background: #fafafa;
}

.question-header {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.question-content {
  margin-bottom: 12px;
  line-height: 1.6;
}

.question-options {
  margin-bottom: 12px;
  padding-left: 16px;
}

.question-options div {
  margin: 4px 0;
  color: #606266;
}

.question-answer {
  color: #67c23a;
  font-weight: 500;
}

.preview-actions {
  padding: 16px 0;
  border-top: 1px solid #ebeef5;
  text-align: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .generate-questions-view {
    padding: 16px;
  }
  
  .el-row {
    flex-direction: column;
  }
  
  .el-col {
    width: 100% !important;
    margin-bottom: 16px;
  }
}
</style>
