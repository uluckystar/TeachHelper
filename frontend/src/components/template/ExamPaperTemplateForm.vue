<template>
  <div class="exam-paper-template-form">
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      class="template-form"
    >
      <!-- 基本信息 -->
      <el-card class="form-card">
        <template #header>
          <div class="card-header">
            <span>基本信息</span>
          </div>
        </template>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="模板名称" prop="name">
              <el-input
                v-model="form.name"
                placeholder="请输入模板名称"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="科目" prop="subject">
              <el-select v-model="form.subject" placeholder="请选择科目" style="width: 100%">
                <el-option label="数学" value="数学" />
                <el-option label="语文" value="语文" />
                <el-option label="英语" value="英语" />
                <el-option label="物理" value="物理" />
                <el-option label="化学" value="化学" />
                <el-option label="生物" value="生物" />
                <el-option label="历史" value="历史" />
                <el-option label="地理" value="地理" />
                <el-option label="政治" value="政治" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="年级" prop="gradeLevel">
              <el-select v-model="form.gradeLevel" placeholder="请选择年级" style="width: 100%">
                <el-option label="小学一年级" value="小学一年级" />
                <el-option label="小学二年级" value="小学二年级" />
                <el-option label="小学三年级" value="小学三年级" />
                <el-option label="小学四年级" value="小学四年级" />
                <el-option label="小学五年级" value="小学五年级" />
                <el-option label="小学六年级" value="小学六年级" />
                <el-option label="初中一年级" value="初中一年级" />
                <el-option label="初中二年级" value="初中二年级" />
                <el-option label="初中三年级" value="初中三年级" />
                <el-option label="高中一年级" value="高中一年级" />
                <el-option label="高中二年级" value="高中二年级" />
                <el-option label="高中三年级" value="高中三年级" />
                <el-option label="大学" value="大学" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="总分" prop="totalScore">
              <el-input-number
                v-model="form.totalScore"
                :min="1"
                :max="1000"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="考试时长(分钟)" prop="duration">
              <el-input-number
                v-model="form.duration"
                :min="1"
                :max="600"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
                <el-option label="草稿" value="DRAFT" />
                <el-option label="就绪" value="READY" />
                <el-option label="已发布" value="PUBLISHED" />
                <el-option label="已归档" value="ARCHIVED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="模板描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="请输入模板描述"
            maxlength="1000"
            show-word-limit
            :rows="3"
          />
        </el-form-item>
        
        <el-form-item label="是否公开">
          <el-switch v-model="form.isPublic" />
          <span class="form-tip">公开的模板可以被其他用户使用</span>
        </el-form-item>
      </el-card>

      <!-- 题目配置 -->
      <el-card class="form-card">
        <template #header>
          <div class="card-header">
            <span>题目配置</span>
            <el-button type="primary" size="small" @click="addQuestion">
              <el-icon><Plus /></el-icon>
              添加题目
            </el-button>
          </div>
        </template>
        
        <div v-if="form.questions && form.questions.length > 0">
          <div
            v-for="(question, index) in form.questions"
            :key="index"
            class="question-item"
          >
            <div class="question-header">
              <span class="question-title">题目 {{ index + 1 }}</span>
              <el-button
                type="danger"
                size="small"
                @click="removeQuestion(index)"
              >
                删除
              </el-button>
            </div>
            
            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item :label="`题目${index + 1}类型`" :prop="`questions.${index}.questionType`">
                  <el-select v-model="question.questionType" placeholder="请选择题目类型" style="width: 100%">
                    <el-option label="单选题" value="SINGLE_CHOICE" />
                    <el-option label="多选题" value="MULTIPLE_CHOICE" />
                    <el-option label="判断题" value="TRUE_FALSE" />
                    <el-option label="填空题" value="FILL_BLANK" />
                    <el-option label="简答题" value="SHORT_ANSWER" />
                    <el-option label="论述题" value="ESSAY" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="`题目${index + 1}分数`" :prop="`questions.${index}.score`">
                  <el-input-number
                    v-model="question.score"
                    :min="0"
                    :max="100"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="`题目${index + 1}序号`" :prop="`questions.${index}.questionOrder`">
                  <el-input-number
                    v-model="question.questionOrder"
                    :min="1"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-form-item :label="`题目${index + 1}内容`" :prop="`questions.${index}.questionContent`">
              <el-input
                v-model="question.questionContent"
                type="textarea"
                placeholder="请输入题目内容"
                :rows="3"
              />
            </el-form-item>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item :label="`题目${index + 1}难度`">
                  <el-select v-model="question.difficultyLevel" placeholder="请选择难度" style="width: 100%">
                    <el-option label="简单" value="EASY" />
                    <el-option label="中等" value="MEDIUM" />
                    <el-option label="困难" value="HARD" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="`题目${index + 1}必答`">
                  <el-switch v-model="question.isRequired" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </div>
        
        <div v-else class="empty-questions">
          <el-empty description="暂无题目，请添加题目" />
        </div>
      </el-card>

      <!-- 高级配置 -->
      <el-card class="form-card">
        <template #header>
          <div class="card-header">
            <span>高级配置</span>
          </div>
        </template>
        
        <el-form-item label="题型配置">
          <el-input
            v-model="form.questionTypeConfig"
            type="textarea"
            placeholder="题型配置（JSON格式）"
            :rows="4"
          />
        </el-form-item>
        
        <el-form-item label="难度配置">
          <el-input
            v-model="form.difficultyConfig"
            type="textarea"
            placeholder="难度配置（JSON格式）"
            :rows="4"
          />
        </el-form-item>
        
        <el-form-item label="知识库配置">
          <el-input
            v-model="form.knowledgeBaseConfig"
            type="textarea"
            placeholder="知识库配置（JSON格式）"
            :rows="4"
          />
        </el-form-item>
        
        <el-form-item label="标签">
          <el-input
            v-model="form.tags"
            placeholder="请输入标签，多个标签用逗号分隔"
          />
        </el-form-item>
      </el-card>
    </el-form>

    <!-- 操作按钮 -->
    <div class="form-actions">
      <el-button @click="handleCancel" :disabled="loading">取消</el-button>
      <el-button type="primary" @click="handleSave" :loading="loading">
        {{ isEditing ? '更新' : '创建' }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { ExamPaperTemplate, ExamPaperTemplateRequest, ExamPaperTemplateQuestionRequest } from '@/api/examPaperTemplate'

interface Props {
  template?: ExamPaperTemplate | null
  isEditing?: boolean
  loading?: boolean
}

interface Emits {
  (e: 'save', template: ExamPaperTemplateRequest): void
  (e: 'cancel'): void
}

const props = withDefaults(defineProps<Props>(), {
  template: null,
  isEditing: false,
  loading: false
})

const emit = defineEmits<Emits>()

const formRef = ref<FormInstance>()

// 表单数据
const form = reactive<ExamPaperTemplateRequest>({
  name: '',
  description: '',
  subject: '',
  gradeLevel: '',
  totalScore: 100,
  duration: 120,
  status: 'DRAFT',
  templateType: 'MANUAL',
  questionTypeConfig: '',
  difficultyConfig: '',
  knowledgeBaseConfig: '',
  tags: '',
  isPublic: false,
  questions: []
})

// 表单验证规则
const rules: FormRules = {
  name: [
    { required: true, message: '请输入模板名称', trigger: 'blur' },
    { max: 200, message: '模板名称不能超过200个字符', trigger: 'blur' }
  ],
  subject: [
    { required: true, message: '请选择科目', trigger: 'change' }
  ],
  gradeLevel: [
    { required: true, message: '请选择年级', trigger: 'change' }
  ],
  totalScore: [
    { required: true, message: '请输入总分', trigger: 'blur' },
    { type: 'number', min: 1, max: 1000, message: '总分必须在1-1000之间', trigger: 'blur' }
  ],
  duration: [
    { type: 'number', min: 1, max: 600, message: '考试时长必须在1-600分钟之间', trigger: 'blur' }
  ]
}

// 监听模板数据变化
watch(() => props.template, (newTemplate) => {
  if (newTemplate) {
    Object.assign(form, {
      name: newTemplate.name,
      description: newTemplate.description,
      subject: newTemplate.subject,
      gradeLevel: newTemplate.gradeLevel,
      totalScore: newTemplate.totalScore,
      duration: newTemplate.duration,
      status: newTemplate.status,
      templateType: newTemplate.templateType,
      questionTypeConfig: newTemplate.questionTypeConfig,
      difficultyConfig: newTemplate.difficultyConfig,
      knowledgeBaseConfig: newTemplate.knowledgeBaseConfig,
      tags: newTemplate.tags,
      isPublic: newTemplate.isPublic,
      questions: newTemplate.questions?.map(q => ({
        id: q.id,
        questionOrder: q.questionOrder,
        questionType: q.questionType,
        questionContent: q.questionContent,
        questionId: q.questionId,
        score: q.score,
        difficultyLevel: q.difficultyLevel,
        knowledgeTags: q.knowledgeTags,
        questionConfig: q.questionConfig,
        isRequired: q.isRequired,
        status: q.status
      })) || []
    })
  } else {
    // 重置表单
    Object.assign(form, {
      name: '',
      description: '',
      subject: '',
      gradeLevel: '',
      totalScore: 100,
      duration: 120,
      status: 'DRAFT',
      templateType: 'MANUAL',
      questionTypeConfig: '',
      difficultyConfig: '',
      knowledgeBaseConfig: '',
      tags: '',
      isPublic: false,
      questions: []
    })
  }
}, { immediate: true })

// 添加题目
const addQuestion = () => {
  const newQuestion: ExamPaperTemplateQuestionRequest = {
    questionOrder: (form.questions?.length || 0) + 1,
    questionType: 'SINGLE_CHOICE',
    questionContent: '',
    score: 5,
    difficultyLevel: 'MEDIUM',
    isRequired: true,
    status: 'DRAFT'
  }
  
  if (!form.questions) {
    form.questions = []
  }
  form.questions.push(newQuestion)
}

// 删除题目
const removeQuestion = (index: number) => {
  if (form.questions) {
    form.questions.splice(index, 1)
    // 重新排序
    form.questions.forEach((q, i) => {
      q.questionOrder = i + 1
    })
  }
}

// 保存表单
const handleSave = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    emit('save', { ...form })
  } catch (error) {
    console.error('表单验证失败:', error)
  }
}

// 取消
const handleCancel = () => {
  emit('cancel')
}

onMounted(() => {
  // 初始化时添加一个默认题目
  if (!props.template && (!form.questions || form.questions.length === 0)) {
    addQuestion()
  }
})
</script>

<style scoped>
.exam-paper-template-form {
  max-width: 100%;
}

.form-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.question-item {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 20px;
  margin-bottom: 20px;
  background-color: #fafafa;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e4e7ed;
}

.question-title {
  font-weight: 500;
  color: #303133;
}

.empty-questions {
  text-align: center;
  padding: 40px 0;
}

.form-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}
</style> 