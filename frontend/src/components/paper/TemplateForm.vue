<template>
  <div class="template-form">
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      @submit.prevent="handleSubmit"
    >
      <!-- 基本信息 -->
      <el-card class="form-section">
        <template #header>
          <div class="section-header">
            <el-icon><Document /></el-icon>
            <span>基本信息</span>
          </div>
        </template>
        
        <el-form-item label="模板名称" prop="name">
          <el-input
            v-model="form.name"
            placeholder="请输入模板名称"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="模板描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入模板描述（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="科目" prop="subject">
              <el-select
                v-model="form.subject"
                placeholder="请选择科目"
                style="width: 100%"
                filterable
                allow-create
              >
                <el-option label="数学" value="数学" />
                <el-option label="语文" value="语文" />
                <el-option label="英语" value="英语" />
                <el-option label="物理" value="物理" />
                <el-option label="化学" value="化学" />
                <el-option label="生物" value="生物" />
                <el-option label="历史" value="历史" />
                <el-option label="地理" value="地理" />
                <el-option label="政治" value="政治" />
                <el-option label="计算机" value="计算机" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年级" prop="gradeLevel">
              <el-select
                v-model="form.gradeLevel"
                placeholder="请选择年级"
                style="width: 100%"
              >
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
        </el-row>
        
        <el-row :gutter="20">
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
          <el-col :span="12">
            <el-form-item label="时间限制" prop="timeLimit">
              <el-input-number
                v-model="form.timeLimit"
                :min="1"
                :max="600"
                style="width: 100%"
                placeholder="分钟"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 题型配置 -->
      <el-card class="form-section">
        <template #header>
          <div class="section-header">
            <el-icon><List /></el-icon>
            <span>题型配置</span>
            <el-button
              type="primary"
              size="small"
              @click="addQuestionType"
              style="margin-left: auto;"
            >
              添加题型
            </el-button>
          </div>
        </template>
        
        <div v-if="questionTypes.length === 0" class="empty-state">
          <el-empty description="暂无题型配置" />
          <el-button type="primary" @click="addQuestionType">
            添加第一个题型
          </el-button>
        </div>
        
        <div
          v-for="(questionType, index) in questionTypes"
          :key="index"
          class="question-type-item"
        >
          <el-card class="question-type-card">
            <template #header>
              <div class="question-type-header">
                <span>题型 {{ index + 1 }}</span>
                <el-button
                  type="danger"
                  size="small"
                  @click="removeQuestionType(index)"
                  :disabled="questionTypes.length === 1"
                >
                  删除
                </el-button>
              </div>
            </template>
            
            <el-form-item label="题型" :prop="`questionTypes.${index}.type`">
              <el-select
                v-model="questionType.type"
                placeholder="请选择题型"
                style="width: 100%"
              >
                <el-option label="单选题" value="SINGLE_CHOICE" />
                <el-option label="多选题" value="MULTIPLE_CHOICE" />
                <el-option label="判断题" value="TRUE_FALSE" />
                <el-option label="简答题" value="SHORT_ANSWER" />
                <el-option label="论述题" value="ESSAY" />
                <el-option label="编程题" value="CODING" />
                <el-option label="案例分析题" value="CASE_ANALYSIS" />
              </el-select>
            </el-form-item>
            
            <el-row :gutter="10">
              <el-col :span="12">
                <el-form-item label="题目数量" :prop="`questionTypes.${index}.count`">
                  <el-input-number
                    v-model="questionType.count"
                    :min="1"
                    :max="50"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="每题分值" :prop="`questionTypes.${index}.scorePerQuestion`">
                  <el-input-number
                    v-model="questionType.scorePerQuestion"
                    :min="1"
                    :max="100"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-form-item label="特殊要求" :prop="`questionTypes.${index}.requirements`">
              <el-input
                v-model="questionType.requirements"
                type="textarea"
                :rows="2"
                placeholder="题型特殊要求（可选）"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
          </el-card>
        </div>
      </el-card>

      <!-- 难度配置 -->
      <el-card class="form-section">
        <template #header>
          <div class="section-header">
            <el-icon><TrendCharts /></el-icon>
            <span>难度配置</span>
          </div>
        </template>
        
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="简单题比例" prop="difficulty.easyRatio">
              <el-slider
                v-model="difficulty.easyRatio"
                :min="0"
                :max="1"
                :step="0.1"
                :format-tooltip="formatPercentage"
                show-input
                :show-input-controls="false"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="中等题比例" prop="difficulty.mediumRatio">
              <el-slider
                v-model="difficulty.mediumRatio"
                :min="0"
                :max="1"
                :step="0.1"
                :format-tooltip="formatPercentage"
                show-input
                :show-input-controls="false"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="困难题比例" prop="difficulty.hardRatio">
              <el-slider
                v-model="difficulty.hardRatio"
                :min="0"
                :max="1"
                :step="0.1"
                :format-tooltip="formatPercentage"
                show-input
                :show-input-controls="false"
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-alert
          v-if="!isDifficultyRatioValid"
          title="难度比例总和必须等于100%"
          type="warning"
          :closable="false"
          style="margin-top: 10px"
        />
      </el-card>

      <!-- 知识库配置 -->
      <el-card class="form-section">
        <template #header>
          <div class="section-header">
            <el-icon><Collection /></el-icon>
            <span>知识库配置（可选）</span>
          </div>
        </template>
        
        <el-form-item label="关联知识库">
          <el-select
            v-model="form.knowledgeBaseIds"
            placeholder="选择要关联的知识库"
            multiple
            style="width: 100%"
            :loading="loadingKnowledgeBases"
          >
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            >
              <div style="display: flex; justify-content: space-between;">
                <span>{{ kb.name }}</span>
                <el-tag size="small" type="info">{{ kb.subject }}</el-tag>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        
        <div class="knowledge-base-hint">
          <el-icon><InfoFilled /></el-icon>
          <span>选择知识库后，生成的题目将基于知识库内容，更加准确和相关</span>
        </div>
      </el-card>

      <!-- 额外配置 -->
      <el-card class="form-section">
        <template #header>
          <div class="section-header">
            <el-icon><Setting /></el-icon>
            <span>额外配置</span>
          </div>
        </template>
        
        <el-form-item label="生成要求">
          <el-input
            v-model="form.customRequirements"
            type="textarea"
            :rows="3"
            placeholder="请输入试卷生成的特殊要求（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="是否公开">
          <el-switch
            v-model="form.isPublic"
            active-text="公开模板"
            inactive-text="私有模板"
          />
          <div class="form-hint">
            <el-icon><InfoFilled /></el-icon>
            <span>公开模板可以被其他用户使用</span>
          </div>
        </el-form-item>
      </el-card>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button
          type="primary"
          size="large"
          @click="handleSubmit"
          :loading="loading"
          :disabled="!isFormValid"
        >
          <el-icon><Check /></el-icon>
          {{ isEditing ? '更新模板' : '保存模板' }}
        </el-button>
        
        <el-button size="large" @click="handleCancel">
          <el-icon><Close /></el-icon>
          取消
        </el-button>
        
        <el-button size="large" @click="resetForm" v-if="!isEditing">
          <el-icon><Refresh /></el-icon>
          重置表单
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Document, List, TrendCharts, Collection, Setting, InfoFilled,
  Check, Close, Refresh
} from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { 
  PaperGenerationTemplate, 
  PaperGenerationTemplateRequest, 
  QuestionTypeConfig, 
  DifficultyConfig,
  KnowledgeBase
} from '@/api/knowledge'
import { knowledgeBaseApi } from '@/api/knowledge'

// 扩展的表单数据接口
interface ExtendedFormData extends PaperGenerationTemplateRequest {
  customRequirements?: string
  isPublic?: boolean
  knowledgeBaseIds?: number[]
}

interface Props {
  template?: PaperGenerationTemplate | null
  isEditing?: boolean
  loading?: boolean
}

interface Emits {
  (e: 'save', template: PaperGenerationTemplateRequest): void
  (e: 'cancel'): void
}

const props = withDefaults(defineProps<Props>(), {
  template: null,
  isEditing: false,
  loading: false
})

const emit = defineEmits<Emits>()

const formRef = ref<FormInstance>()
const loadingKnowledgeBases = ref(false)
const knowledgeBases = ref<KnowledgeBase[]>([])

// 表单数据
const form = reactive<ExtendedFormData>({
  name: '',
  description: '',
  subject: '',
  gradeLevel: '',
  totalScore: 100,
  timeLimit: 120,
  questionConfig: '',
  difficultyConfig: '',
  knowledgeBaseConfig: '',
  customRequirements: '',
  isPublic: false,
  knowledgeBaseIds: []
})

const questionTypes = ref<QuestionTypeConfig[]>([])

const difficulty = reactive<DifficultyConfig>({
  easyRatio: 0.3,
  mediumRatio: 0.5,
  hardRatio: 0.2
})

// 表单验证规则
const rules: FormRules = {
  name: [
    { required: true, message: '请输入模板名称', trigger: 'blur' },
    { min: 2, max: 100, message: '模板名称长度在2-100个字符', trigger: 'blur' }
  ],
  subject: [
    { required: true, message: '请选择科目', trigger: 'change' }
  ],
  gradeLevel: [
    { required: true, message: '请选择年级', trigger: 'change' }
  ],
  totalScore: [
    { required: true, message: '请输入总分', trigger: 'blur' },
    { type: 'number', min: 1, max: 1000, message: '总分范围为1-1000', trigger: 'blur' }
  ]
}

// 计算属性
const isDifficultyRatioValid = computed(() => {
  const total = difficulty.easyRatio + difficulty.mediumRatio + difficulty.hardRatio
  return Math.abs(total - 1.0) < 0.001
})

const isFormValid = computed(() => {
  return form.name && form.subject && form.gradeLevel && 
         questionTypes.value.length > 0 && isDifficultyRatioValid.value
})

// 监听难度比例变化，自动调整
watch([
  () => difficulty.easyRatio,
  () => difficulty.mediumRatio
], ([easyRatio, mediumRatio]) => {
  const remaining = 1.0 - easyRatio - mediumRatio
  if (remaining >= 0 && remaining <= 1) {
    difficulty.hardRatio = Math.round(remaining * 10) / 10
  }
})

// 格式化百分比显示
const formatPercentage = (value: number) => {
  return `${Math.round(value * 100)}%`
}

// 添加题型
const addQuestionType = () => {
  questionTypes.value.push({
    type: 'SINGLE_CHOICE',
    count: 5,
    scorePerQuestion: 5,
    requirements: ''
  })
}

// 删除题型
const removeQuestionType = (index: number) => {
  if (questionTypes.value.length > 1) {
    questionTypes.value.splice(index, 1)
  }
}

// 加载知识库列表
const loadKnowledgeBases = async () => {
  try {
    loadingKnowledgeBases.value = true
    const response = await knowledgeBaseApi.getKnowledgeBases({
      page: 0,
      size: 100
    })
    knowledgeBases.value = response.content || []
  } catch (error) {
    console.error('加载知识库失败:', error)
  } finally {
    loadingKnowledgeBases.value = false
  }
}

// 初始化表单数据
const initializeForm = () => {
  if (props.template) {
    // 编辑模式，填充现有数据
    Object.assign(form, {
      name: props.template.name,
      description: props.template.description || '',
      subject: props.template.subject,
      gradeLevel: props.template.gradeLevel,
      totalScore: props.template.totalScore,
      timeLimit: props.template.timeLimit || 120,
      customRequirements: (props.template as any).customRequirements || '',
      isPublic: (props.template as any).isPublic || false,
      knowledgeBaseIds: []
    })
    
    // 解析题型配置
    try {
      const parsedQuestionConfig = JSON.parse(props.template.questionConfig)
      questionTypes.value = parsedQuestionConfig || []
    } catch (error) {
      console.error('解析题型配置失败:', error)
      questionTypes.value = []
    }
    
    // 解析难度配置
    try {
      const parsedDifficultyConfig = JSON.parse(props.template.difficultyConfig)
      if (parsedDifficultyConfig) {
        Object.assign(difficulty, parsedDifficultyConfig)
      }
    } catch (error) {
      console.error('解析难度配置失败:', error)
    }
    
    // 解析知识库配置
    try {
      const parsedKnowledgeConfig = JSON.parse(props.template.knowledgeBaseConfig || '[]')
      form.knowledgeBaseIds = parsedKnowledgeConfig || []
    } catch (error) {
      console.error('解析知识库配置失败:', error)
      form.knowledgeBaseIds = []
    }
  } else {
    // 新建模式，设置默认值
    if (questionTypes.value.length === 0) {
      addQuestionType()
    }
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    
    if (!isDifficultyRatioValid.value) {
      ElMessage.warning('请确保难度比例总和为100%')
      return
    }
    
    if (questionTypes.value.length === 0) {
      ElMessage.warning('请至少添加一个题型配置')
      return
    }
    
    // 构建保存数据
    const templateData: PaperGenerationTemplateRequest = {
      ...form,
      questionConfig: JSON.stringify(questionTypes.value),
      difficultyConfig: JSON.stringify(difficulty),
      knowledgeBaseConfig: JSON.stringify(form.knowledgeBaseIds || [])
    }
    
    emit('save', templateData)
    
  } catch (error) {
    console.error('表单验证失败:', error)
    ElMessage.error('请检查表单填写是否正确')
  }
}

// 取消操作
const handleCancel = () => {
  emit('cancel')
}

// 重置表单
const resetForm = () => {
  formRef.value?.resetFields()
  questionTypes.value = []
  Object.assign(difficulty, {
    easyRatio: 0.3,
    mediumRatio: 0.5,
    hardRatio: 0.2
  })
  form.knowledgeBaseIds = []
  ElMessage.success('表单已重置')
  
  // 重新添加默认题型
  addQuestionType()
}

// 生命周期
onMounted(() => {
  loadKnowledgeBases()
  initializeForm()
})
</script>

<style scoped>
.template-form {
  max-width: 100%;
}

.form-section {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.question-type-item {
  margin-bottom: 15px;
}

.question-type-card {
  border: 1px solid #e4e7ed;
}

.question-type-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
}

.knowledge-base-hint,
.form-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.action-buttons {
  text-align: center;
  padding: 20px 0;
  border-top: 1px solid #e4e7ed;
  margin-top: 20px;
}

.action-buttons .el-button {
  margin: 0 10px;
}

:deep(.el-slider__input) {
  width: 80px;
}

:deep(.el-card__header) {
  padding: 15px 20px;
  background-color: #fafafa;
}

:deep(.el-select .el-tag) {
  margin: 2px;
}
</style>
