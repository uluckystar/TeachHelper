<template>
  <div class="classroom-management">
    <div class="page-header">
      <h1>班级管理</h1>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>
        创建班级
      </el-button>
    </div>

    <!-- 班级列表 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span>班级列表</span>
          <el-input
            v-model="searchQuery"
            placeholder="搜索班级名称或班级代码"
            style="width: 300px"
            clearable
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="filteredClassrooms"
        style="width: 100%"
        stripe
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="班级名称" min-width="150" />
        <el-table-column prop="classCode" label="班级代码" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.classCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="studentCount" label="学生数量" width="100" align="center" />
        <el-table-column prop="examCount" label="考试数量" width="100" align="center" />
        <el-table-column prop="createdBy" label="创建者" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewClassroom(row)">
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <el-button size="small" type="primary" @click="editClassroom(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button size="small" type="danger" @click="deleteClassroom(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建班级对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="创建班级"
      width="500px"
      @close="resetCreateForm"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="80px"
      >
        <el-form-item label="班级名称" prop="name">
          <el-input
            v-model="createForm.name"
            placeholder="请输入班级名称"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="班级描述" prop="description">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入班级描述"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="createLoading">
          创建
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑班级对话框 -->
    <el-dialog
      v-model="showEditDialog"
      title="编辑班级"
      width="500px"
      @close="resetEditForm"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="80px"
      >
        <el-form-item label="班级名称" prop="name">
          <el-input
            v-model="editForm.name"
            placeholder="请输入班级名称"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="班级描述" prop="description">
          <el-input
            v-model="editForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入班级描述"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="班级代码">
          <el-input
            v-model="editForm.classCode"
            readonly
            placeholder="班级代码（只读）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleEdit" :loading="editLoading">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 班级详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      title="班级详情"
      width="800px"
    >
      <div v-if="selectedClassroom">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="班级名称">
            {{ selectedClassroom.name }}
          </el-descriptions-item>
          <el-descriptions-item label="班级代码">
            <el-tag>{{ selectedClassroom.classCode }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="班级描述" :span="2">
            {{ selectedClassroom.description || '无' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建者">
            {{ selectedClassroom.createdBy }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDateTime(selectedClassroom.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="学生数量">
            {{ selectedClassroom.studentCount }} 人
          </el-descriptions-item>
          <el-descriptions-item label="考试数量">
            {{ selectedClassroom.examCount }} 个
          </el-descriptions-item>
        </el-descriptions>

        <el-divider>班级成员</el-divider>
        <el-table
          :data="selectedClassroom.students"
          style="width: 100%"
          size="small"
        >
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="email" label="邮箱" />
          <el-table-column prop="joinedAt" label="加入时间">
            <template #default="{ row }">
              {{ formatDateTime(row.joinedAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button
                size="small"
                type="danger"
                @click="removeStudent(selectedClassroom.id, row.id)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus, Search, View, Edit, Delete } from '@element-plus/icons-vue'
import { classroomApi, type ClassroomResponse, type ClassroomCreateRequest } from '@/api/classroom'

// 响应式数据
const loading = ref(false)
const searchQuery = ref('')
const classrooms = ref<ClassroomResponse[]>([])

// 创建班级相关
const showCreateDialog = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive<ClassroomCreateRequest>({
  name: '',
  description: ''
})

// 编辑班级相关
const showEditDialog = ref(false)
const editLoading = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive({
  id: 0,
  name: '',
  description: '',
  classCode: ''
})

// 班级详情相关
const showDetailDialog = ref(false)
const selectedClassroom = ref<ClassroomResponse | null>(null)

// 表单验证规则
const createRules = {
  name: [
    { required: true, message: '请输入班级名称', trigger: 'blur' },
    { min: 2, max: 50, message: '班级名称长度在 2 到 50 个字符', trigger: 'blur' }
  ]
}

const editRules = {
  name: [
    { required: true, message: '请输入班级名称', trigger: 'blur' },
    { min: 2, max: 50, message: '班级名称长度在 2 到 50 个字符', trigger: 'blur' }
  ]
}

// 计算属性
const filteredClassrooms = computed(() => {
  if (!searchQuery.value) {
    return classrooms.value
  }
  return classrooms.value.filter(classroom =>
    classroom.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
    classroom.classCode.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

// 方法
const loadClassrooms = async () => {
  try {
    loading.value = true
    classrooms.value = await classroomApi.getAllClassrooms()
  } catch (error) {
    console.error('加载班级列表失败:', error)
    ElMessage.error('加载班级列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  // 搜索在计算属性中实现，这里可以添加防抖等优化
}

const viewClassroom = async (classroom: ClassroomResponse) => {
  try {
    selectedClassroom.value = await classroomApi.getClassroom(classroom.id)
    showDetailDialog.value = true
  } catch (error) {
    console.error('获取班级详情失败:', error)
    ElMessage.error('获取班级详情失败')
  }
}

const editClassroom = (classroom: ClassroomResponse) => {
  editForm.id = classroom.id
  editForm.name = classroom.name
  editForm.description = classroom.description || ''
  editForm.classCode = classroom.classCode
  showEditDialog.value = true
}

const deleteClassroom = async (classroom: ClassroomResponse) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除班级"${classroom.name}"吗？此操作不可撤销。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await classroomApi.deleteClassroom(classroom.id)
    ElMessage.success('删除成功')
    loadClassrooms()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除班级失败:', error)
      ElMessage.error('删除班级失败')
    }
  }
}

const handleCreate = async () => {
  if (!createFormRef.value) return
  
  try {
    const valid = await createFormRef.value.validate()
    if (!valid) return
    
    createLoading.value = true
    await classroomApi.createClassroom(createForm)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    resetCreateForm()
    loadClassrooms()
  } catch (error) {
    console.error('创建班级失败:', error)
    ElMessage.error('创建班级失败')
  } finally {
    createLoading.value = false
  }
}

const handleEdit = async () => {
  if (!editFormRef.value) return
  
  try {
    const valid = await editFormRef.value.validate()
    if (!valid) return
    
    editLoading.value = true
    await classroomApi.updateClassroom(editForm.id, {
      name: editForm.name,
      description: editForm.description
    })
    ElMessage.success('更新成功')
    showEditDialog.value = false
    resetEditForm()
    loadClassrooms()
  } catch (error) {
    console.error('更新班级失败:', error)
    ElMessage.error('更新班级失败')
  } finally {
    editLoading.value = false
  }
}

const removeStudent = async (classroomId: number, studentId: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要将该学生从班级中移除吗？',
      '确认移除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await classroomApi.removeStudent(classroomId, studentId)
    ElMessage.success('移除成功')
    
    // 刷新班级详情
    if (selectedClassroom.value) {
      selectedClassroom.value = await classroomApi.getClassroom(classroomId)
    }
    loadClassrooms()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('移除学生失败:', error)
      ElMessage.error('移除学生失败')
    }
  }
}

const resetCreateForm = () => {
  createForm.name = ''
  createForm.description = ''
  createFormRef.value?.resetFields()
}

const resetEditForm = () => {
  editForm.id = 0
  editForm.name = ''
  editForm.description = ''
  editForm.classCode = ''
  editFormRef.value?.resetFields()
}

const formatDateTime = (dateTime: string | Date) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN')
}

// 生命周期
onMounted(() => {
  loadClassrooms()
})
</script>

<style scoped>
.classroom-management {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0;
  color: #303133;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
