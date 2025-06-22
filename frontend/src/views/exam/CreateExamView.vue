<template>
  <div class="create-exam">
    <div class="page-header">
      <h1>创建考试</h1>
    </div>

    <el-card>
      <el-form
        ref="examFormRef"
        :model="examForm"
        :rules="examRules"
        label-width="120px"
      >
        <el-form-item label="考试标题" prop="title">
          <el-input
            v-model="examForm.title"
            placeholder="请输入考试标题"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="考试描述" prop="description">
          <el-input
            v-model="examForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入考试描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="examForm.startTime"
            type="datetime"
            placeholder="选择开始时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="examForm.endTime"
            type="datetime"
            placeholder="选择结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="考试时长" prop="duration">
          <el-input-number
            v-model="examForm.duration"
            :min="1"
            :max="480"
            placeholder="分钟"
            style="width: 100%"
          />
          <span class="form-help">分钟（1-480分钟）</span>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">
            创建考试
          </el-button>
          <el-button @click="handleCancel">
            取消
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { examApi } from '@/api/exam'
import type { ExamCreateRequest } from '@/types/api'

// 扩展的表单类型，包含前端需要的额外字段
interface CreateExamForm extends ExamCreateRequest {
  startTime: string
  endTime: string
  duration: number
}

const router = useRouter()

const examFormRef = ref<FormInstance>()
const loading = ref(false)

const examForm = reactive<CreateExamForm>({
  title: '',
  description: '',
  startTime: '',
  endTime: '',
  duration: 60
})

const validateEndTime = (rule: any, value: any, callback: any) => {
  if (!value) {
    callback(new Error('请选择结束时间'))
  } else if (new Date(value) <= new Date(examForm.startTime)) {
    callback(new Error('结束时间必须晚于开始时间'))
  } else {
    callback()
  }
}

const examRules: FormRules = {
  title: [
    { required: true, message: '请输入考试标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入考试描述', trigger: 'blur' },
    { max: 500, message: '描述长度不能超过 500 个字符', trigger: 'blur' }
  ],
  startTime: [
    { required: true, message: '请选择开始时间', trigger: 'change' }
  ],
  endTime: [
    { required: true, validator: validateEndTime, trigger: 'change' }
  ],
  duration: [
    { required: true, message: '请输入考试时长', trigger: 'blur' },
    { type: 'number', min: 1, max: 480, message: '时长必须在 1-480 分钟之间', trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  if (!examFormRef.value) return
  
  try {
    await examFormRef.value.validate()
    loading.value = true
    
    // 创建考试请求，包含时间字段
    const createRequest: ExamCreateRequest = {
      title: examForm.title,
      description: examForm.description,
      duration: examForm.duration,
      startTime: examForm.startTime,
      endTime: examForm.endTime
    }
    
    const exam = await examApi.createExam(createRequest)
    ElMessage.success('考试创建成功')
    router.push(`/exams/${exam.id}`)
  } catch (error) {
    console.error('Failed to create exam:', error)
    ElMessage.error('创建考试失败')
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  router.back()
}
</script>

<style scoped>
.create-exam {
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  color: #303133;
  font-size: 24px;
  font-weight: 500;
}

.form-help {
  color: #909399;
  font-size: 12px;
  margin-left: 8px;
}

.classroom-selection {
  width: 100%;
}

.classroom-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.classroom-name {
  font-weight: 500;
}

.classroom-code {
  color: #909399;
  margin-left: 8px;
}

.classroom-students {
  color: #409eff;
  font-size: 12px;
}

.selected-classrooms {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.classroom-card {
  border: 1px solid #e4e7ed;
}

.classroom-info {
  padding: 8px;
}

.classroom-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.classroom-header h4 {
  margin: 0;
  font-size: 14px;
  color: #303133;
}

.classroom-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
}

.mb-4 {
  margin-bottom: 16px;
}
</style>
