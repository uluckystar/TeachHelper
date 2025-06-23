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

      <el-card class="form-card" ref="timeSettingsCard">
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
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 300px"
          />
        </el-form-item>
        
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择考试结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
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
          <div class="form-help">
            <el-text type="info" size="small">
              选择要发布考试的班级，学生只能看到其所在班级的考试
            </el-text>
          </div>
        </el-form-item>
        
        <!-- 已选班级的成员预览 -->
        <el-form-item 
          v-if="form.accessType === 'CLASS' && form.selectedClasses.length > 0" 
          label="已选班级"
        >
          <div class="selected-classes-preview">
            <el-card 
              v-for="classId in form.selectedClasses" 
              :key="classId"
              class="class-preview-card"
              shadow="hover"
            >
              <template #header>
                <div class="class-preview-header">
                  <span>{{ getClassNameById(classId) }}</span>
                  <el-button 
                    size="small" 
                    type="primary" 
                    link
                    @click="showClassMembers(classId)"
                  >
                    查看成员
                  </el-button>
                </div>
              </template>
              <div class="class-preview-content">
                <span>学生人数: {{ getClassStudentCount(classId) }}</span>
              </div>
            </el-card>
          </div>
        </el-form-item>
        
        <!-- 所有可选班级 -->
        <el-form-item 
          v-if="form.accessType === 'CLASS'" 
          label="所有班级"
        >
          <div class="all-classes-preview">
            <el-card 
              v-for="cls in classes" 
              :key="cls.id"
              class="class-preview-card selectable-class"
              shadow="hover"
              :class="{ selected: form.selectedClasses.includes(cls.id) }"
              @click="toggleClassSelection(cls.id)"
            >
              <template #header>
                <div class="class-preview-header">
                  <el-checkbox 
                    :model-value="form.selectedClasses.includes(cls.id)"
                    @change="toggleClassSelection(cls.id)"
                    @click.stop
                  >
                    <span>{{ cls.name }}</span>
                  </el-checkbox>
                  <el-button 
                    size="small" 
                    type="primary" 
                    link
                    @click.stop="showClassMembers(cls.id)"
                  >
                    查看成员
                  </el-button>
                </div>
              </template>
              <div class="class-preview-content">
                <span>学生人数: {{ getClassStudentCount(cls.id) }}</span>
              </div>
            </el-card>
          </div>
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

    <!-- 班级成员查看对话框 -->
    <el-dialog
      v-model="showMembersDialog"
      title="班级成员"
      width="600px"
    >
      <div v-if="selectedClassroomForMembers">
        <el-descriptions :column="2" border class="mb-4">
          <el-descriptions-item label="班级名称">
            {{ selectedClassroomForMembers.name }}
          </el-descriptions-item>
          <el-descriptions-item label="班级代码">
            <el-tag>{{ selectedClassroomForMembers.classCode }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="学生数量" :span="2">
            {{ selectedClassroomForMembers.studentCount }} 人
          </el-descriptions-item>
        </el-descriptions>

        <el-divider>学生列表</el-divider>
        <div v-if="selectedClassroomForMembers.students && selectedClassroomForMembers.students.length > 0">
          <el-table :data="selectedClassroomForMembers.students" stripe>
            <el-table-column prop="username" label="姓名" />
            <el-table-column prop="email" label="邮箱" />
            <el-table-column label="角色">
              <template #default="scope">
                <el-tag 
                  v-for="role in scope.row.roles" 
                  :key="role" 
                  size="small"
                  :type="role === 'STUDENT' ? 'primary' : 'info'"
                >
                  {{ role === 'STUDENT' ? '学生' : role }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div v-else class="empty-state">
          <el-empty description="该班级暂无学生" size="small" />
        </div>
      </div>
      <template #footer>
        <el-button @click="showMembersDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { examApi } from '@/api/exam'
import { classroomApi, type ClassroomResponse, type StudentResponse } from '@/api/classroom'
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
const timeSettingsCard = ref()

// 响应式数据
const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const students = ref<Student[]>([])
const classes = ref<Class[]>([])
const showMembersDialog = ref(false)
const selectedClassroomForMembers = ref<ClassroomResponse | null>(null)

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
  startTime: [
    {
      validator: (rule: any, value: string, callback: Function) => {
        if (value && form.endTime) {
          const start = new Date(value)
          const end = new Date(form.endTime)
          if (start >= end) {
            callback(new Error('开始时间必须早于结束时间'))
          } else {
            callback()
          }
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  endTime: [
    {
      validator: (rule: any, value: string, callback: Function) => {
        if (value) {
          const endTime = new Date(value)
          const now = new Date()
          
          if (form.startTime) {
            const startTime = new Date(form.startTime)
            if (endTime <= startTime) {
              callback(new Error('结束时间必须晚于开始时间'))
              return
            }
          }
          
          // 如果要发布考试，检查结束时间不能是过去时间
          if (form.status === 'PUBLISHED' && endTime <= now) {
            callback(new Error('发布考试的结束时间不能是过去时间'))
            return
          }
          
          callback()
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
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
    // 加载班级数据
    const classroomsData = await classroomApi.getAllClassrooms()
    classes.value = classroomsData.map(classroom => ({
      id: classroom.id,
      name: classroom.name
    }))
    
    // TODO: 加载学生数据 - 需要实现学生API
    // 暂时从班级中获取学生信息
    const allStudents: Student[] = []
    classroomsData.forEach(classroom => {
      if (classroom.students) {
        classroom.students.forEach(student => {
          if (!allStudents.find(s => s.id === student.id)) {
            allStudents.push({
              id: student.id,
              name: student.username,
              studentId: student.email || student.username
            })
          }
        })
      }
    })
    students.value = allStudents
    
  } catch (error) {
    console.error('Failed to load students and classes:', error)
    ElMessage.error('加载班级和学生信息失败')
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
    
    // 检查考试结束时间
    let timeWarningMessage = ''
    let timeWarningType: 'info' | 'warning' | 'error' = 'info'
    const now = new Date()
    
    if (form.endTime) {
      const endTime = new Date(form.endTime)
      if (endTime <= now) {
        // 考试已过期，阻止发布并提供编辑选项
        try {
          await ElMessageBox.confirm(
            '⚠️ 考试结束时间已过期，无法发布此考试！\n\n是否立即修改考试时间？',
            '考试时间已过期',
            {
              confirmButtonText: '修改时间',
              cancelButtonText: '取消发布',
              type: 'error'
            }
          )
          // 用户选择修改时间，滚动到时间设置区域
          scrollToTimeSettings()
          return
        } catch {
          // 用户取消，直接返回
          return
        }
      } else {
        const timeDiff = endTime.getTime() - now.getTime()
        const hoursLeft = Math.floor(timeDiff / (1000 * 60 * 60))
        const minutesLeft = Math.floor((timeDiff % (1000 * 60 * 60)) / (1000 * 60))
        
        if (hoursLeft < 1) {
          timeWarningMessage = `⚠️ 警告：考试将在 ${minutesLeft}分钟后结束，时间非常紧急！\n\n`
          timeWarningType = 'warning'
        } else if (hoursLeft < 24) {
          timeWarningMessage = `⚠️ 提醒：考试将在 ${hoursLeft}小时${minutesLeft > 0 ? minutesLeft + '分钟' : ''}后结束\n\n`
          timeWarningType = 'warning'
        }
      }
    } else {
      timeWarningMessage = '⚠️ 提醒：未设置考试结束时间，学生可以无限制地参加考试\n\n'
      timeWarningType = 'warning'
    }
    
    // 如果选择了指定班级，需要先选择班级
    if (form.accessType === 'CLASS') {
      if (form.selectedClasses.length === 0) {
        ElMessage.warning('请先选择要发布考试的班级')
        return
      }
      
      await ElMessageBox.confirm(
        `${timeWarningMessage}确定要发布考试到所选的 ${form.selectedClasses.length} 个班级吗？发布后学生将可以参加考试。`,
        '确认发布',
        {
          confirmButtonText: '确定发布',
          cancelButtonText: '取消',
          type: timeWarningType
        }
      )
    } else {
      await ElMessageBox.confirm(
        `${timeWarningMessage}确定要发布这个考试吗？发布后学生将可以参加考试。`,
        '确认发布',
        {
          confirmButtonText: '确定发布',
          cancelButtonText: '取消',
          type: timeWarningType
        }
      )
    }
    
    publishing.value = true
    
    // 先保存考试基本信息
    const updateData: ExamUpdateRequest = {
      title: form.title,
      description: form.description,
      status: 'DRAFT', // 先保持草稿状态
      duration: form.duration,
      startTime: form.startTime || undefined,
      endTime: form.endTime || undefined
    }
    
    await examApi.updateExam(examId.value, updateData)
    
    // 如果选择了班级，更新考试班级
    if (form.accessType === 'CLASS' && form.selectedClasses.length > 0) {
      await examApi.updateExamClassrooms(examId.value, form.selectedClasses)
    }
    
    // 最后发布考试
    const publishResponse = await examApi.publishExam(examId.value)
    
    // 检查是否有时间提醒消息
    if (publishResponse.timeMessage) {
      // 显示时间提醒消息
      if (publishResponse.timeMessage.includes('过期')) {
        ElMessage.warning({
          message: `考试发布成功！${publishResponse.timeMessage}`,
          duration: 5000
        })
      } else {
        ElMessage.info({
          message: `考试发布成功！${publishResponse.timeMessage}`,
          duration: 4000
        })
      }
    } else {
      ElMessage.success('考试发布成功')
    }
    router.push(`/exams/${examId.value}`)
    
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to publish exam:', error)
      
      // 处理特定的验证错误
      if (error.code === 422) {
        ElMessage.warning(error.message || '考试必须包含至少一个题目才能发布')
      } else {
        ElMessage.error(error.message || '发布考试失败')
      }
    }
  } finally {
    publishing.value = false
  }
}

const scrollToTimeSettings = () => {
  if (timeSettingsCard.value) {
    timeSettingsCard.value.$el.scrollIntoView({
      behavior: 'smooth',
      block: 'center'
    })
    // 高亮显示时间设置区域
    timeSettingsCard.value.$el.style.transition = 'box-shadow 0.3s ease'
    timeSettingsCard.value.$el.style.boxShadow = '0 0 0 2px #f56c6c'
    setTimeout(() => {
      timeSettingsCard.value.$el.style.boxShadow = ''
    }, 2000)
  }
}

const goBack = () => {
  router.back()
}

// 班级相关方法
const getClassNameById = (classId: number) => {
  const cls = classes.value.find(c => c.id === classId)
  return cls ? cls.name : '未知班级'
}

const getClassStudentCount = (classId: number) => {
  // 从学生列表中统计属于该班级的学生数量
  // 这里简化处理，实际应该从班级的学生列表中获取
  return students.value.filter(student => 
    // 这里需要根据实际的数据结构来判断学生属于哪个班级
    // 暂时返回总学生数的估算值
    true
  ).length
}

const showClassMembers = async (classId: number) => {
  try {
    const classroom = await classroomApi.getClassroom(classId)
    selectedClassroomForMembers.value = classroom
    showMembersDialog.value = true
  } catch (error) {
    console.error('获取班级成员失败:', error)
    ElMessage.error('获取班级成员失败')
  }
}

// 切换班级选择状态
const toggleClassSelection = (classId: number) => {
  const index = form.selectedClasses.indexOf(classId)
  if (index > -1) {
    form.selectedClasses.splice(index, 1)
  } else {
    form.selectedClasses.push(classId)
  }
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

.selected-classes-preview {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.all-classes-preview {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.class-preview-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.selectable-class {
  cursor: pointer;
}

.selectable-class:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px 0 rgba(64, 158, 255, 0.15);
}

.selectable-class.selected {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.class-preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
}

.class-preview-content {
  color: #666;
  font-size: 14px;
}

.empty-state {
  text-align: center;
  padding: 20px 0;
}

.mb-4 {
  margin-bottom: 16px;
}
</style>
