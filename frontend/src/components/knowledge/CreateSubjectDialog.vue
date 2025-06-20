<template>
  <el-dialog
    v-model="visible"
    title="创建新学科"
    width="500px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
      <el-form-item label="学科名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入学科名称" />
      </el-form-item>
      
      <el-form-item label="适用年级" prop="applicableGrades">
        <el-select 
          v-model="form.applicableGrades" 
          multiple 
          placeholder="请选择适用年级"
          style="width: 100%;"
        >
          <el-option
            v-for="grade in gradeLevels"
            :key="grade"
            :label="grade"
            :value="grade"
          />
        </el-select>
      </el-form-item>
      
      <el-form-item label="学科描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="请输入学科描述（可选）"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">
          创建
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, reactive, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

interface SubjectForm {
  name: string
  applicableGrades: string[]
  description: string
}

interface Props {
  modelValue: boolean
  gradeLevels: string[]
  saving?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'created', subject: string): void
}

const props = withDefaults(defineProps<Props>(), {
  saving: false
})

const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const formRef = ref<FormInstance>()

const form = reactive<SubjectForm>({
  name: '',
  applicableGrades: [],
  description: ''
})

const formRules: FormRules = {
  name: [
    { required: true, message: '请输入学科名称', trigger: 'blur' },
    { min: 2, max: 20, message: '学科名称长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  applicableGrades: [
    { required: true, message: '请选择适用年级', trigger: 'change' },
    { type: 'array', min: 1, message: '至少选择一个年级', trigger: 'change' }
  ]
}

watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    // 重置表单
    Object.assign(form, {
      name: '',
      applicableGrades: [],
      description: ''
    })
    formRef.value?.resetFields()
  }
})

const handleClose = () => {
  visible.value = false
}

const handleSave = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    emit('created', form.name)
    ElMessage.success('学科创建成功')
  } catch (error) {
    ElMessage.error('请检查表单输入')
  }
}
</script>

<style scoped>
.dialog-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
</style>
