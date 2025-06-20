<template>
  <div class="edit-exam">
    <div class="page-header">
      <el-breadcrumb>
        <el-breadcrumb-item :to="{ path: '/exams' }">考试列表</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: `/exams/${examId}` }">{{ form.title || '考试详情' }}</el-breadcrumb-item>
        <el-breadcrumb-item>编辑考试</el-breadcrumb-item>
      </el-breadcrumb>
      <h1>编辑考试</h1>
      <p class="page-description">修改考试的基本信息</p>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules as any"
      label-width="120px"
      class="exam-form"
      v-loading="loading"
    >
      <el-card class="form-card">
        <template #header>
          <span>基本信息</span>
        </template>
        
        <el-form-item label="考试标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入考试标题"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="考试描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="请输入考试描述（可选）"
            :rows="4"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="考试状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="DRAFT">草稿</el-radio>
            <el-radio label="PUBLISHED">已发布</el-radio>
            <el-radio label="ENDED">已结束</el-radio>
          </el-radio-group>
          <div class="form-help">
            <el-text type="info" size="small">
              草稿状态可以自由编辑，发布后学生可以参加考试，结束后不再接受答案
            </el-text>
          </div>
        </el-form-item>
      </el-card>

      <el-card class="form-card">
        <template #header>
          <span>考试设置</span>
        </template>
        
        <el-form-item label="考试时长" prop="duration">
          <el-input-number
            v-model="form.duration"
            :min="0"
            :max="480"
            placeholder="考试时长（分钟）"
            style="width: 200px"
          />
          <span class="form-suffix">分钟（0表示不限时）</span>
        </el-form-item>
        
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择考试开始时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 300px"
          />
        </el-form-item>
        
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择考试结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 300px"
          />
        </el-form-item>
        
        <el-form-item label="答题设置">
          <el-checkbox-group v-model="form.settings">
            <el-checkbox label="shuffle_questions">随机排列题目</el-checkbox>
            <el-checkbox label="shuffle_options">随机排列选项</el-checkbox>
            <el-checkbox label="show_results">立即显示结果</el-checkbox>
            <el-checkbox label="allow_retake">允许重新答题</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-card>

      <el-card class="form-card">
        <template #header>
          <span>权限设置</span>
        </template>
        
        <el-form-item label="参与学生" prop="accessType">
          <el-radio-group v-model="form.accessType">
            <el-radio label="ALL">所有学生</el-radio>
            <el-radio label="SELECTED">指定学生</el-radio>
            <el-radio label="CLASS">指定班级</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item 
          v-if="form.accessType === 'SELECTED'" 
          label="选择学生"
        >
          <el-select
            v-model="form.selectedStudents"
            multiple
            filterable
            placeholder="请选择学生"
            style="width: 100%"
          >
            <el-option
              v-for="student in students"
              :key="student.id"
              :label="`${student.name} (${student.studentId})`"
              :value="student.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item 
          v-if="form.accessType === 'CLASS'" 
          label="选择班级"
        >
          <el-select
            v-model="form.selectedClasses"
            multiple
            placeholder="请选择班级"
            style="width: 100%"
          >
            <el-option
              v-for="cls in classes"
              :key="cls.id"
              :label="cls.name"
              :value="cls.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="密码保护">
          <el-switch v-model="form.requirePassword" />
          <span class="form-suffix">启用后学生需要输入密码才能参加考试</span>
        </el-form-item>
        
        <el-form-item 
          v-if="form.requirePassword" 
          label="考试密码" 
          prop="password"
        >
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请设置考试密码"
            style="width: 300px"
            show-password
          />
        </el-form-item>
      </el-card>

      <div class="form-actions">
        <el-button @click="goBack" size="large">取消</el-button>
        <el-button 
          type="primary" 
          @click="saveExam" 
          :loading="saving"
          size="large"
        >
          保存修改
        </el-button>
        <el-button 
          v-if="form.status === 'DRAFT'"
          type="success" 
          @click="publishExam" 
          :loading="publishing"
          size="large"
        >
          保存并发布
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { examApi } from '@/api/exam'
import type { ExamResponse, ExamUpdateRequest } from '@/types/api'

interface Student {
  id: number
  name: string
  studentId: string
}

interface Class {
  id: number
  name: string
}

const route = useRoute()
const router = useRouter()
const formRef = ref()

// 响应式数据
const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const students = ref<Student[]>([])
const classes = ref<Class[]>([])

const form = reactive<ExamUpdateRequest & { 
  settings: string[];
  accessType: string;
  selectedStudents: number[];
  selectedClasses: number[];
  requirePassword: boolean;
}>({
  title: '',
  description: '',
  status: 'DRAFT',
  duration: 0,
  startTime: '',
  endTime: '',
  settings: [],
  accessType: 'ALL',
  selectedStudents: [],
  selectedClasses: [],
  requirePassword: false,
  password: ''
})

// 计算属性
const examId = computed(() => {
  const id = route.params.id
  return typeof id === 'string' ? parseInt(id, 10) : Number(id)
})

// 表单验证规则
const rules = {
  title: [
    { required: true, message: '请输入考试标题', trigger: 'blur' },
    { min: 1, max: 100, message: '标题长度在 1 到 100 个字符', trigger: 'blur' }
  ],
  description: [
    { max: 500, message: '描述长度不能超过 500 个字符', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择考试状态', trigger: 'change' }
  ],
  duration: [
    { type: 'number', min: 0, max: 480, message: '时长范围在 0 到 480 分钟', trigger: 'blur' }
  ],
  password: [
    { 
      validator: (rule: any, value: string, callback: Function) => {
        if (form.requirePassword && (!value || value.length < 4)) {
          callback(new Error('密码长度至少为 4 位'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ]
}

// 方法
const loadExamData = async () => {
  try {
    loading.value = true
    const exam = await examApi.getExam(examId.value)
    
    // 填充表单数据
    form.title = exam.title
    form.description = exam.description || ''
    form.status = exam.status || 'DRAFT'
    form.duration = exam.duration || 0
    form.startTime = exam.startTime || ''
    form.endTime = exam.endTime || ''
    
    // 解析设置（如果有的话）
    if (exam.settings) {
      form.settings = Array.isArray(exam.settings) ? exam.settings : []
    }
    
  } catch (error) {
    console.error('Failed to load exam:', error)
    ElMessage.error('加载考试信息失败')
    router.back()
  } finally {
    loading.value = false
  }
}

const loadStudentsAndClasses = async () => {
  try {
    // 这里应该调用相应的API加载学生和班级数据
    // students.value = await userApi.getStudents()
    // classes.value = await classApi.getClasses()
    
    // 临时模拟数据
    students.value = []
    classes.value = []
  } catch (error) {
    console.error('Failed to load students and classes:', error)
  }
}

const validateForm = async () => {
  return await formRef.value.validate()
}

const saveExam = async () => {
  try {
    await validateForm()
    saving.value = true
    
    const updateData: ExamUpdateRequest = {
      title: form.title,
      description: form.description,
      status: form.status,
      duration: form.duration,
      startTime: form.startTime || undefined,
      endTime: form.endTime || undefined
    }
    
    await examApi.updateExam(examId.value, updateData)
    ElMessage.success('考试修改成功')
    router.push(`/exams/${examId.value}`)
    
  } catch (error) {
    console.error('Failed to save exam:', error)
    ElMessage.error('保存考试失败')
  } finally {
    saving.value = false
  }
}

const publishExam = async () => {
  try {
    await validateForm()
    
    await ElMessageBox.confirm(
      '确定要发布这个考试吗？发布后学生将可以参加考试。',
      '确认发布',
      {
        confirmButtonText: '确定发布',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    publishing.value = true
    
    const updateData: ExamUpdateRequest = {
      title: form.title,
      description: form.description,
      status: 'PUBLISHED',
      duration: form.duration,
      startTime: form.startTime || undefined,
      endTime: form.endTime || undefined
    }
    
    await examApi.updateExam(examId.value, updateData)
    ElMessage.success('考试发布成功')
    router.push(`/exams/${examId.value}`)
    
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to publish exam:', error)
      ElMessage.error('发布考试失败')
    }
  } finally {
    publishing.value = false
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadExamData()
  loadStudentsAndClasses()
})
</script>

<style scoped>
.edit-exam {
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 8px 0 4px 0;
  font-size: 24px;
  font-weight: 600;
}

.page-description {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.form-card {
  margin-bottom: 20px;
}

.exam-form {
  margin-bottom: 24px;
}

.form-help {
  margin-top: 8px;
}

.form-suffix {
  margin-left: 8px;
  color: #909399;
  font-size: 14px;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 24px 0;
  border-top: 1px solid #e4e7ed;
}
</style>
