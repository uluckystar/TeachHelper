<template>
  <div class="user-management">
    <div class="page-header">
      <h1>用户管理</h1>
      <p class="page-description">管理系统用户和权限</p>
    </div>

    <!-- 搜索和筛选 -->
    <el-card class="search-card">
      <el-row :gutter="16">
        <el-col :span="6">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索用户名或邮箱"
            @keyup.enter="searchUsers"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="4">
          <el-select v-model="searchForm.role" placeholder="用户角色" clearable>
            <el-option label="管理员" value="ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="searchForm.status" placeholder="用户状态" clearable>
            <el-option label="启用" value="ENABLED" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-button type="primary" icon="Search" @click="searchUsers">
            搜索
          </el-button>
          <el-button icon="Refresh" @click="resetSearch">
            重置
          </el-button>
          <el-button type="success" icon="Plus" @click="showCreateDialog">
            新建用户
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 用户列表 -->
    <el-card class="users-card">
      <template #header>
        <div class="card-header">
          <span>用户列表 ({{ total }})</span>
        </div>
      </template>

      <el-table :data="users" v-loading="loading">
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="realName" label="真实姓名" />
        <el-table-column prop="roles" label="角色" width="120">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role" :type="getRoleTag(role)" size="small">
              {{ getRoleText(role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
              {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="150">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button-group>
              <el-button size="small" @click="editUser(row)">
                编辑
              </el-button>
              <el-button 
                size="small" 
                :type="row.status === 'ENABLED' ? 'warning' : 'success'"
                @click="toggleUserStatus(row)"
              >
                {{ row.status === 'ENABLED' ? '禁用' : '启用' }}
              </el-button>
              <el-button size="small" type="danger" @click="deleteUser(row)">
                删除
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 创建/编辑用户对话框 -->
    <el-dialog 
      v-model="showDialog" 
      :title="editingUser ? '编辑用户' : '创建用户'" 
      width="600px"
    >
      <el-form :model="userForm" :rules="userRules" ref="userFormRef" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="!!editingUser" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="userForm.realName" />
        </el-form-item>
        <el-form-item v-if="!editingUser" label="密码" prop="password">
          <el-input v-model="userForm.password" type="password" />
        </el-form-item>
        <el-form-item label="角色" prop="roles">
          <el-checkbox-group v-model="userForm.roles">
            <el-checkbox label="ADMIN">管理员</el-checkbox>
            <el-checkbox label="TEACHER">教师</el-checkbox>
            <el-checkbox label="STUDENT">学生</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio label="ENABLED">启用</el-radio>
            <el-radio label="DISABLED">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="saveUser" :loading="saving">
          {{ editingUser ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search, Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
// 添加用户类型接口
interface UserInfo {
  id: number
  username: string
  email: string
  realName: string
  roles: string[]
  status: string
  createdAt: string
}

interface UserForm {
  username: string
  email: string
  realName: string
  password: string
  roles: string[]
  status: string
}

// 模拟 API 调用
const mockApi = {
  async getUsers(params: any) {
    // 模拟数据
    return {
      data: {
        content: [
          {
            id: 1,
            username: 'admin',
            email: 'admin@example.com',
            realName: '系统管理员',
            roles: ['ADMIN'],
            status: 'ENABLED',
            createdAt: '2024-01-01T00:00:00'
          },
          {
            id: 2,
            username: 'teacher1',
            email: 'teacher1@example.com',
            realName: '张老师',
            roles: ['TEACHER'],
            status: 'ENABLED',
            createdAt: '2024-01-02T00:00:00'
          }
        ],
        totalElements: 2
      }
    }
  },
  async createUser(data: any) {
    console.log('Creating user:', data)
    return { data: { ...data, id: Date.now() } }
  },
  async updateUser(id: number, data: any) {
    console.log('Updating user:', id, data)
    return { data: { ...data, id } }
  },
  async deleteUser(id: number) {
    console.log('Deleting user:', id)
  }
}

const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const editingUser = ref<UserInfo | null>(null)
const users = ref<UserInfo[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const userFormRef = ref()

const searchForm = ref({
  keyword: '',
  role: '',
  status: ''
})

const userForm = ref<UserForm>({
  username: '',
  email: '',
  realName: '',
  password: '',
  roles: [],
  status: 'ENABLED'
})

const userRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email' as const, message: '邮箱格式不正确', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  roles: [
    { required: true, message: '请选择用户角色', trigger: 'change' }
  ]
}

const loadUsers = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
      ...searchForm.value
    }
    const response = await mockApi.getUsers(params)
    users.value = response.data?.content || []
    total.value = response.data?.totalElements || 0
  } catch (error) {
    console.error('加载用户失败:', error)
    ElMessage.error('加载用户失败')
  } finally {
    loading.value = false
  }
}

const searchUsers = () => {
  currentPage.value = 1
  loadUsers()
}

const resetSearch = () => {
  searchForm.value = {
    keyword: '',
    role: '',
    status: ''
  }
  searchUsers()
}

const showCreateDialog = () => {
  editingUser.value = null
  userForm.value = {
    username: '',
    email: '',
    realName: '',
    password: '',
    roles: [],
    status: 'ENABLED'
  }
  showDialog.value = true
}

const editUser = (user: any) => {
  editingUser.value = user
  userForm.value = {
    username: user.username,
    email: user.email,
    realName: user.realName,
    password: '',
    roles: [...user.roles],
    status: user.status
  }
  showDialog.value = true
}

const saveUser = async () => {
  if (!userFormRef.value) return
  
  try {
    await userFormRef.value.validate()
    saving.value = true
    
    if (editingUser.value) {
      await mockApi.updateUser(editingUser.value.id, userForm.value)
      ElMessage.success('用户更新成功')
    } else {
      await mockApi.createUser(userForm.value)
      ElMessage.success('用户创建成功')
    }
    
    showDialog.value = false
    loadUsers()
    
  } catch (error) {
    console.error('保存用户失败:', error)
    ElMessage.error('保存用户失败')
  } finally {
    saving.value = false
  }
}

const toggleUserStatus = async (user: any) => {
  try {
    const newStatus = user.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
    await mockApi.updateUser(user.id, { ...user, status: newStatus })
    ElMessage.success(`用户已${newStatus === 'ENABLED' ? '启用' : '禁用'}`)
    loadUsers()
  } catch (error) {
    console.error('更新用户状态失败:', error)
    ElMessage.error('更新用户状态失败')
  }
}

const deleteUser = async (user: any) => {
  try {
    await ElMessageBox.confirm(`确定要删除用户 ${user.username} 吗？`, '确认删除', {
      type: 'warning'
    })
    
    await mockApi.deleteUser(user.id)
    ElMessage.success('用户删除成功')
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除用户失败:', error)
      ElMessage.error('删除用户失败')
    }
  }
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  loadUsers()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  loadUsers()
}

const getRoleTag = (role: string): 'success' | 'primary' | 'warning' | 'info' | 'danger' | undefined => {
  const map: Record<string, 'success' | 'primary' | 'warning' | 'info' | 'danger' | undefined> = {
    'ADMIN': 'danger',
    'TEACHER': 'warning',
    'STUDENT': 'primary'
  }
  return map[role] || 'info'
}

const getRoleText = (role: string) => {
  const map: Record<string, string> = {
    'ADMIN': '管理员',
    'TEACHER': '教师',
    'STUDENT': '学生'
  }
  return map[role] || role
}

const formatDate = (dateString: string) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.user-management {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
}

.page-description {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.search-card,
.users-card {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
