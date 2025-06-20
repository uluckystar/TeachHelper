<template>
  <div class="quick-generation-form">
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      @submit.prevent="handleSubmit"
    >
      <el-row :gutter="20">
        <!-- 基本信息 -->
        <el-col :span="12">
          <el-card class="form-section">
            <template #header>
              <div class="section-header">
                <el-icon><Document /></el-icon>
                <span>基本信息</span>
              </div>
            </template>
            
            <el-form-item label="试卷标题" prop="title">
              <el-input
                v-model="form.title"
                placeholder="请输入试卷标题"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
            
            <el-form-item label="试卷描述" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="3"
                placeholder="请输入试卷描述（可选）"
                maxlength="1000"
                show-word-limit
              />
            </el-form-item>
            
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
            
            <el-row :gutter="10">
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
        </el-col>

        <!-- 题型配置 -->
        <el-col :span="12">
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
            
            <div v-if="form.questionTypes.length === 0" class="empty-state">
              <el-empty description="暂无题型配置" />
              <el-button type="primary" @click="addQuestionType">
                添加第一个题型
              </el-button>
            </div>
            
            <div
              v-for="(questionType, index) in form.questionTypes"
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
                      :disabled="form.questionTypes.length === 1"
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
        </el-col>
      </el-row>

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
                v-model="form.difficulty!.easyRatio"
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
                v-model="form.difficulty!.mediumRatio"
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
                v-model="form.difficulty!.hardRatio"
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

      <!-- 额外配置 -->
      <el-card class="form-section">
        <template #header>
          <div class="section-header">
            <el-icon><Setting /></el-icon>
            <span>额外配置</span>
          </div>
        </template>
        
        <el-form-item label="自定义要求">
          <el-input
            v-model="form.customRequirements"
            type="textarea"
            :rows="3"
            placeholder="请输入试卷生成的特殊要求（可选）"
            maxlength="500"
            show-word-limit
          />
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
          <el-icon><MagicStick /></el-icon>
          生成试卷
        </el-button>
        
        <el-button
          size="large"
          @click="handleSaveTemplate"
          :disabled="!isFormValid"
        >
          <el-icon><Document /></el-icon>
          保存为模板
        </el-button>
        
        <el-button size="large" @click="resetForm">
          <el-icon><Refresh /></el-icon>
          重置表单
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, List, TrendCharts, Setting, MagicStick, Refresh } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { PaperGenerationRequest, QuestionTypeConfig, DifficultyConfig } from '@/api/knowledge'

interface Props {
  loading?: boolean
}

interface Emits {
  (e: 'generate', request: PaperGenerationRequest): void
  (e: 'save-template', template: any): void
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<Emits>()

const formRef = ref<FormInstance>()

// 表单数据
const form = reactive<PaperGenerationRequest>({
  title: '',
  description: '',
  subject: '',
  gradeLevel: '',
  totalScore: 100,
  timeLimit: 120,
  questionTypes: [],
  difficulty: {
    easyRatio: 0.3,
    mediumRatio: 0.5,
    hardRatio: 0.2
  },
  customRequirements: ''
})

// 表单验证规则
const rules: FormRules = {
  title: [
    { required: true, message: '请输入试卷标题', trigger: 'blur' },
    { min: 2, max: 200, message: '标题长度在2-200个字符', trigger: 'blur' }
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
  if (!form.difficulty) return false
  const total = form.difficulty.easyRatio + form.difficulty.mediumRatio + form.difficulty.hardRatio
  return Math.abs(total - 1.0) < 0.001
})

const isFormValid = computed(() => {
  return form.title && form.subject && form.gradeLevel && 
         form.questionTypes.length > 0 && isDifficultyRatioValid.value
})

// 监听难度比例变化，自动调整
watch([
  () => form.difficulty?.easyRatio,
  () => form.difficulty?.mediumRatio
], ([easyRatio, mediumRatio]) => {
  if (form.difficulty && easyRatio !== undefined && mediumRatio !== undefined) {
    const remaining = 1.0 - easyRatio - mediumRatio
    if (remaining >= 0 && remaining <= 1) {
      form.difficulty.hardRatio = Math.round(remaining * 10) / 10
    }
  }
})

// 格式化百分比显示
const formatPercentage = (value: number) => {
  return `${Math.round(value * 100)}%`
}

// 添加题型
const addQuestionType = () => {
  form.questionTypes.push({
    type: 'SINGLE_CHOICE',
    count: 5,
    scorePerQuestion: 5,
    requirements: ''
  })
}

// 删除题型
const removeQuestionType = (index: number) => {
  if (form.questionTypes.length > 1) {
    form.questionTypes.splice(index, 1)
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
    
    emit('generate', { ...form })
    
  } catch (error) {
    console.error('表单验证失败:', error)
    ElMessage.error('请检查表单填写是否正确')
  }
}

// 保存为模板
const handleSaveTemplate = () => {
  if (!isFormValid.value) {
    ElMessage.warning('请先完善表单信息')
    return
  }
  
  const template = {
    name: form.title + ' - 模板',
    description: form.description,
    subject: form.subject,
    gradeLevel: form.gradeLevel,
    totalScore: form.totalScore,
    timeLimit: form.timeLimit,
    questionConfig: JSON.stringify(form.questionTypes),
    difficultyConfig: JSON.stringify(form.difficulty),
    knowledgeBaseConfig: JSON.stringify([])
  }
  
  emit('save-template', template)
}

// 重置表单
const resetForm = () => {
  formRef.value?.resetFields()
  
  form.questionTypes = []
  form.difficulty = {
    easyRatio: 0.3,
    mediumRatio: 0.5,
    hardRatio: 0.2
  }
  
  ElMessage.success('表单已重置')
}

// 初始化添加一个题型
if (form.questionTypes.length === 0) {
  addQuestionType()
}
</script>

<style scoped>
.quick-generation-form {
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
  background-color: #f8f9fa;
}
</style>
