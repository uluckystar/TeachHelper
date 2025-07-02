<template>
  <el-dialog
    v-model="visible"
    title="编辑模板"
    width="600px"
    :before-close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      @submit.prevent
    >
      <el-form-item label="模板名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入模板名称" />
      </el-form-item>
      
      <el-form-item label="科目" prop="subject">
        <el-select v-model="form.subject" placeholder="请选择科目" style="width: 100%">
          <el-option label="语文" value="语文" />
          <el-option label="数学" value="数学" />
          <el-option label="英语" value="英语" />
          <el-option label="物理" value="物理" />
          <el-option label="化学" value="化学" />
          <el-option label="生物" value="生物" />
          <el-option label="历史" value="历史" />
          <el-option label="地理" value="地理" />
          <el-option label="政治" value="政治" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="年级" prop="gradeLevel">
        <el-select v-model="form.gradeLevel" placeholder="请选择年级" style="width: 100%">
          <el-option label="一年级" value="一年级" />
          <el-option label="二年级" value="二年级" />
          <el-option label="三年级" value="三年级" />
          <el-option label="四年级" value="四年级" />
          <el-option label="五年级" value="五年级" />
          <el-option label="六年级" value="六年级" />
          <el-option label="初一" value="初一" />
          <el-option label="初二" value="初二" />
          <el-option label="初三" value="初三" />
          <el-option label="高一" value="高一" />
          <el-option label="高二" value="高二" />
          <el-option label="高三" value="高三" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="总分" prop="totalScore">
        <el-input-number
          v-model="form.totalScore"
          :min="1"
          :max="200"
          placeholder="请输入总分"
          style="width: 100%"
        />
      </el-form-item>
      
      <el-form-item label="考试时长" prop="duration">
        <el-input-number
          v-model="form.duration"
          :min="1"
          :max="300"
          placeholder="请输入考试时长（分钟）"
          style="width: 100%"
        />
      </el-form-item>
      
      <el-form-item label="描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="请输入模板描述"
        />
      </el-form-item>
      
      <el-form-item label="是否公开">
        <el-switch v-model="form.isPublic" />
        <span style="margin-left: 8px; color: #666; font-size: 12px;">
          公开的模板可以被其他用户使用
        </span>
      </el-form-item>
    </el-form>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">
          保存
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { examPaperTemplateApi, type ExamPaperTemplate, type ExamPaperTemplateRequest } from '@/api/examPaperTemplate'

interface Props {
  modelValue: boolean
  template?: ExamPaperTemplate | null
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'saved'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ExamPaperTemplateRequest>({
  name: '',
  subject: '',
  gradeLevel: '',
  totalScore: 100,
  duration: 120,
  description: '',
  isPublic: false
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入模板名称', trigger: 'blur' },
    { min: 1, max: 50, message: '模板名称长度在 1 到 50 个字符', trigger: 'blur' }
  ],
  subject: [
    { required: true, message: '请选择科目', trigger: 'change' }
  ],
  gradeLevel: [
    { required: true, message: '请选择年级', trigger: 'change' }
  ],
  totalScore: [
    { required: true, message: '请输入总分', trigger: 'blur' },
    { type: 'number', min: 1, max: 200, message: '总分必须在 1 到 200 之间', trigger: 'blur' }
  ],
  duration: [
    { required: true, message: '请输入考试时长', trigger: 'blur' },
    { type: 'number', min: 1, max: 300, message: '考试时长必须在 1 到 300 分钟之间', trigger: 'blur' }
  ]
}

// 监听 modelValue 变化
watch(() => props.modelValue, (newVal) => {
  visible.value = newVal
  if (newVal && props.template) {
    initForm()
  }
})

// 监听 visible 变化
watch(visible, (newVal) => {
  emit('update:modelValue', newVal)
})

// 初始化表单数据
const initForm = () => {
  if (!props.template) return
  
  form.name = props.template.name
  form.subject = props.template.subject
  form.gradeLevel = props.template.gradeLevel
  form.totalScore = props.template.totalScore
  form.duration = props.template.duration
  form.description = props.template.description || ''
  form.isPublic = props.template.isPublic
}

// 保存模板
const handleSave = async () => {
  if (!formRef.value || !props.template) return
  
  try {
    await formRef.value.validate()
    saving.value = true
    
    await examPaperTemplateApi.updateTemplate(props.template.id, form)
    
    ElMessage.success('模板更新成功')
    emit('saved')
    handleClose()
  } catch (error: any) {
    if (error.message) {
      ElMessage.error(error.message)
    } else {
      ElMessage.error('保存失败，请重试')
    }
  } finally {
    saving.value = false
  }
}

// 关闭对话框
const handleClose = () => {
  visible.value = false
}
</script>

<style scoped>
.dialog-footer {
  text-align: right;
}
</style> 