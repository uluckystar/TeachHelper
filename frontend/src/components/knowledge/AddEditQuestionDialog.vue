<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑题目' : '添加题目'"
    width="800px"
    :close-on-click-modal="false"
    @closed="resetForm"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="题目类型" prop="type">
            <el-select v-model="formData.type" @change="handleTypeChange" style="width: 100%">
              <el-option label="单选题" value="choice">
                <div style="display: flex; justify-content: space-between;">
                  <span>单选题</span>
                  <el-tag type="primary" size="small">CHOICE</el-tag>
                </div>
              </el-option>
              <el-option label="填空题" value="blank">
                <div style="display: flex; justify-content: space-between;">
                  <span>填空题</span>
                  <el-tag type="success" size="small">BLANK</el-tag>
                </div>
              </el-option>
              <el-option label="主观题" value="subjective">
                <div style="display: flex; justify-content: space-between;">
                  <span>主观题</span>
                  <el-tag type="warning" size="small">SUBJECTIVE</el-tag>
                </div>
              </el-option>
              <el-option label="计算题" value="calculation">
                <div style="display: flex; justify-content: space-between;">
                  <span>计算题</span>
                  <el-tag type="info" size="small">CALCULATION</el-tag>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="难度级别" prop="difficulty">
            <el-select v-model="formData.difficulty" style="width: 100%">
              <el-option label="简单" value="EASY">
                <div style="display: flex; justify-content: space-between;">
                  <span>简单</span>
                  <el-tag type="success" size="small">EASY</el-tag>
                </div>
              </el-option>
              <el-option label="中等" value="MEDIUM">
                <div style="display: flex; justify-content: space-between;">
                  <span>中等</span>
                  <el-tag type="warning" size="small">MEDIUM</el-tag>
                </div>
              </el-option>
              <el-option label="困难" value="HARD">
                <div style="display: flex; justify-content: space-between;">
                  <span>困难</span>
                  <el-tag type="danger" size="small">HARD</el-tag>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- AI助手工具栏 -->
      <el-form-item>
        <AIQuestionAssistant
          :knowledge-base-id="knowledgeBaseId"
          :current-question-data="formData"
          :show-rubric-generation="isEdit"
          :question-id="isEdit ? formData.id : undefined"
          @question-filled="handleQuestionFilled"
          @question-generated="handleQuestionGenerated"
        />
      </el-form-item>

      <el-form-item label="题目内容" prop="content">
        <el-input
          v-model="formData.content"
          type="textarea"
          :rows="4"
          placeholder="请输入题目内容"
          maxlength="1000"
          show-word-limit
        />
      </el-form-item>

      <!-- 选择题选项 -->
      <div v-if="formData.type === 'choice'" class="options-section">
        <el-form-item label="选项设置">
          <div class="options-container">
            <div
              v-for="(option, index) in formData.options"
              :key="index"
              class="option-item"
            >
              <div class="option-input">
                <span class="option-label">{{ String.fromCharCode(65 + index) }}.</span>
                <el-input
                  v-model="option.content"
                  placeholder="请输入选项内容"
                  style="flex: 1;"
                />
                <el-checkbox
                  v-model="option.isCorrect"
                  @change="handleCorrectOptionChange(index)"
                >
                  正确答案
                </el-checkbox>
                <el-button
                  v-if="formData.options.length > 2"
                  icon="Delete"
                  size="small"
                  type="danger"
                  plain
                  @click="removeOption(index)"
                />
              </div>
            </div>
            <el-button
              v-if="formData.options.length < 6"
              icon="Plus"
              size="small"
              @click="addOption"
            >
              添加选项
            </el-button>
          </div>
        </el-form-item>
      </div>

      <!-- 填空题答案 -->
      <div v-if="formData.type === 'blank'" class="blanks-section">
        <el-form-item label="参考答案">
          <div class="blanks-container">
            <div
              v-for="(blank, index) in formData.blanks"
              :key="index"
              class="blank-item"
            >
              <span class="blank-label">空{{ index + 1 }}:</span>
              <el-input
                v-model="blank.answer"
                placeholder="请输入参考答案"
                style="flex: 1; margin-right: 8px;"
              />
              <el-button
                v-if="formData.blanks.length > 1"
                icon="Delete"
                size="small"
                type="danger"
                plain
                @click="removeBlank(index)"
              />
            </div>
            <el-button
              v-if="formData.blanks.length < 5"
              icon="Plus"
              size="small"
              @click="addBlank"
            >
              添加空位
            </el-button>
          </div>
        </el-form-item>
      </div>

      <!-- 主观题/计算题答案 -->
      <div v-if="formData.type === 'subjective' || formData.type === 'calculation'">
        <el-form-item label="参考答案">
          <el-input
            v-model="formData.referenceAnswer"
            type="textarea"
            :rows="4"
            placeholder="请输入参考答案"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
      </div>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="分值" prop="score">
            <el-input-number
              v-model="formData.score"
              :min="1"
              :max="100"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="预计用时">
            <el-input-number
              v-model="formData.estimatedTime"
              :min="1"
              :max="60"
              style="width: 100%"
            />
            <span style="font-size: 12px; color: #999; margin-left: 8px;">分钟</span>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="题目来源">
            <el-select v-model="formData.source" style="width: 100%">
              <el-option label="原创" value="original" />
              <el-option label="AI生成" value="ai_generated" />
              <el-option label="题库导入" value="imported" />
              <el-option label="其他" value="other" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="关联知识点">
        <el-select
          v-model="formData.knowledgePointIds"
          multiple
          filterable
          placeholder="选择关联的知识点"
          style="width: 100%"
        >
          <el-option
            v-for="point in availableKnowledgePoints"
            :key="point.id"
            :label="point.title"
            :value="point.id"
          >
            <div style="display: flex; justify-content: space-between;">
              <span>{{ point.title }}</span>
              <el-tag :type="getCategoryTagType(point.category)" size="small">
                {{ point.category }}
              </el-tag>
            </div>
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item label="题目标签">
        <div class="tags-section">
          <el-tag
            v-for="(tag, index) in formData.tags"
            :key="index"
            closable
            @close="removeTag(index)"
            style="margin: 2px 4px 2px 0;"
          >
            {{ tag }}
          </el-tag>
          <el-input
            v-if="showTagInput"
            ref="tagInputRef"
            v-model="newTag"
            @keyup.enter="addTag"
            @blur="addTag"
            size="small"
            style="width: 80px; margin-left: 4px;"
          />
          <el-button
            v-else
            class="button-new-tag"
            size="small"
            @click="showNewTagInput"
          >
            + 新标签
          </el-button>
        </div>
      </el-form-item>

      <el-form-item label="题目解析">
        <el-input
          v-model="formData.explanation"
          type="textarea"
          :rows="3"
          placeholder="请输入题目解析（可选）"
          maxlength="1000"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="备注">
        <el-input
          v-model="formData.notes"
          type="textarea"
          :rows="2"
          placeholder="备注信息（可选）"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '更新' : '创建' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import AIQuestionAssistant from '@/components/ai/AIQuestionAssistant.vue'

// Props
const props = defineProps<{
  modelValue: boolean
  knowledgeBaseId: number
  editingQuestion?: any
}>()

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: []
}>()

// 响应式数据
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const formRef = ref<FormInstance>()
const submitting = ref(false)
const showTagInput = ref(false)
const newTag = ref('')
const tagInputRef = ref()

const isEdit = computed(() => !!props.editingQuestion?.id)

// 表单数据
const formData = ref({
  id: 0,
  type: 'choice',
  difficulty: 'MEDIUM' as 'EASY' | 'MEDIUM' | 'HARD',
  content: '',
  options: [
    { content: '', isCorrect: false },
    { content: '', isCorrect: false },
    { content: '', isCorrect: false },
    { content: '', isCorrect: false }
  ],
  blanks: [
    { answer: '' }
  ],
  referenceAnswer: '',
  score: 5,
  estimatedTime: 3,
  source: 'original',
  knowledgePointIds: [] as number[],
  tags: [] as string[],
  explanation: '',
  notes: ''
})

// 验证规则
const formRules: FormRules = {
  type: [
    { required: true, message: '请选择题目类型', trigger: 'change' }
  ],
  difficulty: [
    { required: true, message: '请选择难度级别', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入题目内容', trigger: 'blur' },
    { min: 10, max: 1000, message: '题目内容长度在 10 到 1000 个字符', trigger: 'blur' }
  ],
  score: [
    { required: true, message: '请设置题目分值', trigger: 'blur' }
  ]
}

// 可选数据
const availableKnowledgePoints = ref<any[]>([
  // Mock data - 在实际应用中从API获取
  { id: 1, title: '函数的概念', category: '概念定义' },
  { id: 2, title: '导数的定义', category: '微积分' },
  { id: 3, title: '化学反应平衡', category: '化学平衡' }
])

// 监听编辑数据变化
watch(() => props.editingQuestion, (question) => {
  if (question && visible.value) {
    formData.value = {
      id: question.id || 0,
      type: question.type || 'choice',
      difficulty: question.difficulty || 'MEDIUM',
      content: question.content || '',
      options: question.options || [
        { content: '', isCorrect: false },
        { content: '', isCorrect: false },
        { content: '', isCorrect: false },
        { content: '', isCorrect: false }
      ],
      blanks: question.blanks || [{ answer: '' }],
      referenceAnswer: question.referenceAnswer || '',
      score: question.score || 5,
      estimatedTime: question.estimatedTime || 3,
      source: question.source || 'original',
      knowledgePointIds: question.knowledgePointIds || [],
      tags: question.tags || [],
      explanation: question.explanation || '',
      notes: question.notes || ''
    }
  }
}, { immediate: true })

// 方法
const resetForm = () => {
  formData.value = {
    id: 0,
    type: 'choice',
    difficulty: 'MEDIUM',
    content: '',
    options: [
      { content: '', isCorrect: false },
      { content: '', isCorrect: false },
      { content: '', isCorrect: false },
      { content: '', isCorrect: false }
    ],
    blanks: [{ answer: '' }],
    referenceAnswer: '',
    score: 5,
    estimatedTime: 3,
    source: 'original',
    knowledgePointIds: [],
    tags: [],
    explanation: '',
    notes: ''
  }
  
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

const handleTypeChange = () => {
  // 题目类型改变时重置相关数据
  if (formData.value.type === 'choice') {
    formData.value.options = [
      { content: '', isCorrect: false },
      { content: '', isCorrect: false },
      { content: '', isCorrect: false },
      { content: '', isCorrect: false }
    ]
  } else if (formData.value.type === 'blank') {
    formData.value.blanks = [{ answer: '' }]
  }
}

const addOption = () => {
  if (formData.value.options.length < 6) {
    formData.value.options.push({ content: '', isCorrect: false })
  }
}

const removeOption = (index: number) => {
  if (formData.value.options.length > 2) {
    formData.value.options.splice(index, 1)
  }
}

const handleCorrectOptionChange = (index: number) => {
  // 单选题只能有一个正确答案
  if (formData.value.options[index].isCorrect) {
    formData.value.options.forEach((option, i) => {
      if (i !== index) {
        option.isCorrect = false
      }
    })
  }
}

const addBlank = () => {
  if (formData.value.blanks.length < 5) {
    formData.value.blanks.push({ answer: '' })
  }
}

const removeBlank = (index: number) => {
  if (formData.value.blanks.length > 1) {
    formData.value.blanks.splice(index, 1)
  }
}

const showNewTagInput = () => {
  showTagInput.value = true
  nextTick(() => {
    tagInputRef.value?.focus()
  })
}

const addTag = () => {
  if (newTag.value.trim() && !formData.value.tags.includes(newTag.value.trim())) {
    formData.value.tags.push(newTag.value.trim())
  }
  newTag.value = ''
  showTagInput.value = false
}

const removeTag = (index: number) => {
  formData.value.tags.splice(index, 1)
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    // 验证题目类型特定的必填项
    if (formData.value.type === 'choice') {
      const hasCorrectOption = formData.value.options.some(option => option.isCorrect)
      if (!hasCorrectOption) {
        ElMessage.error('请至少设置一个正确答案')
        return
      }
      
      const hasEmptyOption = formData.value.options.some(option => !option.content.trim())
      if (hasEmptyOption) {
        ElMessage.error('请填写所有选项内容')
        return
      }
    } else if (formData.value.type === 'blank') {
      const hasEmptyBlank = formData.value.blanks.some(blank => !blank.answer.trim())
      if (hasEmptyBlank) {
        ElMessage.error('请填写所有空位的参考答案')
        return
      }
    } else if ((formData.value.type === 'subjective' || formData.value.type === 'calculation') && !formData.value.referenceAnswer.trim()) {
      ElMessage.error('请填写参考答案')
      return
    }

    submitting.value = true

    // TODO: 调用API保存题目
    console.log('Saving question:', formData.value)
    
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    ElMessage.success(isEdit.value ? '题目更新成功' : '题目创建成功')
    emit('saved')
    visible.value = false
  } catch (error) {
    console.error('Submit failed:', error)
    ElMessage.error('保存失败，请检查输入信息')
  } finally {
    submitting.value = false
  }
}

// 工具方法
const getCategoryTagType = (category: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    '概念定义': 'primary',
    '原理公式': 'success',
    '解题方法': 'warning',
    '实例分析': 'info',
    '综合应用': 'danger'
  }
  return typeMap[category] || 'info'
}

// AI助手事件处理
const handleQuestionFilled = (questionData: any) => {
  // 将AI生成的题目数据填充到表单中
  if (questionData.title) formData.value.content = questionData.title + '\n' + (formData.value.content || '')
  if (questionData.content) formData.value.content = questionData.content
  if (questionData.type) {
    formData.value.type = questionData.type
    handleTypeChange() // 触发类型变化处理
  }
  if (questionData.difficulty) formData.value.difficulty = questionData.difficulty
  if (questionData.score) formData.value.score = questionData.score
  if (questionData.referenceAnswer) formData.value.referenceAnswer = questionData.referenceAnswer
  
  // 处理选择题选项
  if (questionData.options && questionData.options.length > 0) {
    formData.value.options = questionData.options.map((opt: any) => ({
      content: opt.content || opt.text || '',
      isCorrect: opt.isCorrect || false
    }))
  }
  
  // 处理填空题
  if (questionData.blanks && questionData.blanks.length > 0) {
    formData.value.blanks = questionData.blanks.map((blank: any) => ({
      answer: blank.answer || blank.text || ''
    }))
  }
  
  ElMessage.success('AI智能填充完成，请检查并完善内容')
}

const handleQuestionGenerated = (questionData: any) => {
  // 处理完整的AI生成题目
  handleQuestionFilled(questionData)
  ElMessage.success('AI题目生成完成，已自动填充到表单')
}
</script>

<style scoped>
.options-section,
.blanks-section {
  margin-bottom: 16px;
}

.options-container,
.blanks-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item,
.blank-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fafafa;
}

.option-input {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.option-label,
.blank-label {
  min-width: 24px;
  font-weight: 600;
  color: #606266;
}

.tags-section {
  min-height: 32px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.button-new-tag {
  border: 1px dashed #d9d9d9;
  color: #999;
  background: #fafafa;
}

.button-new-tag:hover {
  border-color: #409eff;
  color: #409eff;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
