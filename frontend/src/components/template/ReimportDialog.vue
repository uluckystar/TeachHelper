<template>
  <el-dialog
    v-model="visible"
    title="重新导入学习通文档"
    width="600px"
    :before-close="handleClose"
  >
    <div class="reimport-content">
      <el-alert
        type="warning"
        :closable="false"
        style="margin-bottom: 20px;"
      >
        <template #title>
          重新导入说明
        </template>
        <div>
          <p>重新导入将：</p>
          <ul>
            <li>清空当前模板的所有题目</li>
            <li>重新解析上传的学习通文档</li>
            <li>生成新的题目列表</li>
            <li>保持模板的基本信息（名称、科目、年级等）</li>
          </ul>
          <p><strong>注意：此操作不可恢复，请谨慎操作！</strong></p>
        </div>
      </el-alert>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        @submit.prevent
      >
        <el-form-item label="文档文件" prop="file">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :file-list="fileList"
            :limit="1"
            accept=".doc,.docx,.pdf"
            drag
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 .doc、.docx、.pdf 格式的学习通文档
              </div>
            </template>
          </el-upload>
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
      </el-form>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleReimport"
          :loading="reimporting"
          :disabled="!form.file"
        >
          开始重新导入
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadFile } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { examPaperTemplateApi } from '@/api/examPaperTemplate'

interface Props {
  modelValue: boolean
  templateId?: number
  templateName?: string
  templateSubject?: string
  templateGradeLevel?: string
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'reimported'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = ref(false)
const reimporting = ref(false)
const formRef = ref<FormInstance>()
const uploadRef = ref()
const fileList = ref<UploadFile[]>([])

const form = reactive({
  file: null as File | null,
  totalScore: 100,
  duration: 120,
  description: ''
})

const rules: FormRules = {
  file: [
    { required: true, message: '请选择要导入的文档文件', trigger: 'change' }
  ],
  totalScore: [
    { required: true, message: '请输入总分', trigger: 'blur' },
    { type: 'number', min: 1, max: 200, message: '总分必须在1-200之间', trigger: 'blur' }
  ],
  duration: [
    { required: true, message: '请输入考试时长', trigger: 'blur' },
    { type: 'number', min: 1, max: 300, message: '考试时长必须在1-300分钟之间', trigger: 'blur' }
  ]
}

watch(() => props.modelValue, (newVal) => {
  visible.value = newVal
  if (newVal) {
    // 重置表单
    form.file = null
    form.totalScore = 100
    form.duration = 120
    form.description = ''
    fileList.value = []
  }
})

watch(visible, (newVal) => {
  emit('update:modelValue', newVal)
})

const handleClose = () => {
  visible.value = false
}

const handleFileChange = (file: UploadFile) => {
  if (file.raw) {
    form.file = file.raw
  }
}

const handleFileRemove = () => {
  form.file = null
}

const handleReimport = async () => {
  if (!formRef.value || !props.templateId || !form.file) return

  try {
    const valid = await formRef.value.validate()
    if (!valid) return
  } catch {
    return
  }

  reimporting.value = true
  try {
    const formData = new FormData()
    formData.append('file', form.file)
    formData.append('totalScore', form.totalScore.toString())
    formData.append('duration', form.duration.toString())
    formData.append('description', form.description)

    await examPaperTemplateApi.reimportFromDocument(props.templateId, formData)
    
    ElMessage.success('重新导入成功')
    visible.value = false
    emit('reimported')
  } catch (error: any) {
    console.error('重新导入失败:', error)
    
    // 处理不同类型的错误
    if (error.response?.status === 400) {
      const errorMessage = error.response?.data?.message || error.response?.data
      if (errorMessage && errorMessage.includes('无权限')) {
        ElMessage.error('您没有权限修改此模板，请确认您是该模板的创建者')
      } else if (errorMessage && errorMessage.includes('文件格式')) {
        ElMessage.error('文件格式不支持，请上传 .doc、.docx 或 .pdf 格式的文件')
      } else if (errorMessage && errorMessage.includes('解析失败')) {
        ElMessage.error('文档解析失败，请检查文件内容是否正确')
      } else if (errorMessage) {
        ElMessage.error(`重新导入失败：${errorMessage}`)
      } else {
        ElMessage.error('请求参数错误，请检查输入内容')
      }
    } else if (error.response?.status === 403) {
      ElMessage.error('权限不足，无法执行此操作')
    } else if (error.response?.status === 404) {
      ElMessage.error('模板不存在或已被删除')
    } else if (error.response?.status >= 500) {
      ElMessage.error('服务器错误，请稍后重试')
    } else {
      ElMessage.error('重新导入失败，请检查网络连接')
    }
  } finally {
    reimporting.value = false
  }
}
</script>

<style scoped>
.reimport-content {
  padding: 20px 0;
}

.dialog-footer {
  text-align: right;
}

.el-upload__tip {
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
}
</style> 