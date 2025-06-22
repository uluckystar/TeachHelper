<template>
  <div class="register-container">
    <el-card class="register-card">
      <template #header>
        <div class="card-header">
          <el-icon class="header-icon"><Reading /></el-icon>
          <h2>TeachHelper 注册</h2>
        </div>
      </template>
      
      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        label-width="80px"
        @submit.prevent="handleRegister"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="请输入用户名"
            clearable
          />
        </el-form-item>
        
        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="registerForm.email"
            type="email"
            placeholder="请输入邮箱地址"
            clearable
          />
        </el-form-item>
        
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
            clearable
          />
        </el-form-item>
        
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            show-password
            clearable
          />
        </el-form-item>
        
        <el-form-item label="角色" prop="roles">
          <el-select v-model="registerForm.roles" placeholder="请选择角色" style="width: 100%" multiple :multiple-limit="1">
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
          </el-select>
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            style="width: 100%"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form-item>
        
        <el-form-item>
          <div class="form-footer">
            <span>已有账号？</span>
            <el-link type="primary" @click="goToLogin">立即登录</el-link>
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import type { RegisterRequest } from '@/types/api'
import { validateRegistration, securityChecks } from '@/utils/registerSecurity'

const router = useRouter()
const authStore = useAuthStore()

const registerFormRef = ref<FormInstance>()
const loading = ref(false)

// 获取可用的角色选项
const availableRoles = computed(() => {
  return securityChecks.getAvailableRoles(null) // null表示公开注册
})

// 本地表单对象（包含确认密码字段）
const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  roles: [] as string[]
})

// 监听密码字段变化，自动重新验证确认密码
watch(() => registerForm.password, () => {
  if (registerForm.confirmPassword && registerFormRef.value) {
    registerFormRef.value.validateField('confirmPassword')
  }
})

const validatePassword = (rule: any, value: any, callback: any) => {
  const error = validateRegistration.password(value)
  if (error) {
    callback(new Error(error))
  } else {
    // 如果确认密码已经输入，重新验证确认密码
    if (registerForm.confirmPassword && registerFormRef.value) {
      registerFormRef.value.validateField('confirmPassword')
    }
    callback()
  }
}

const validateUsername = (rule: any, value: any, callback: any) => {
  const error = validateRegistration.username(value)
  if (error) {
    callback(new Error(error))
  } else {
    callback()
  }
}

const validateEmail = (rule: any, value: any, callback: any) => {
  const error = validateRegistration.email(value)
  if (error) {
    callback(new Error(error))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const validateRoles = (rule: any, value: any, callback: any) => {
  const error = validateRegistration.roles(value)
  if (error) {
    callback(new Error(error))
  } else {
    callback()
  }
}

const registerRules: FormRules = {
  username: [
    { required: true, validator: validateUsername, trigger: 'blur' }
  ],
  email: [
    { required: true, validator: validateEmail, trigger: 'blur' }
  ],
  password: [
    { required: true, validator: validatePassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ],
  roles: [
    { required: true, validator: validateRoles, trigger: 'change' }
  ]
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  try {
    await registerFormRef.value.validate()
    loading.value = true
    
    // 验证角色选择
    if (!registerForm.roles || registerForm.roles.length === 0 || !['STUDENT', 'TEACHER'].includes(registerForm.roles[0])) {
      ElMessage.error('请选择有效的角色')
      return
    }
    
    // 构建注册请求对象
    const registerRequest: RegisterRequest = {
      username: registerForm.username,
      email: registerForm.email,
      password: registerForm.password,
      roles: registerForm.roles
    }
    
    const success = await authStore.register(registerRequest)
    if (success) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } else {
      ElMessage.error('注册失败，请重试')
    }
  } catch (error: any) {
    console.error('Register validation failed:', error)
    // 显示具体的错误信息
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else if (error.message) {
      ElMessage.error(error.message)
    } else {
      ElMessage.error('注册失败，请重试')
    }
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.register-card {
  width: 480px;
  max-width: 90vw;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  overflow: visible;
}

/* 确保表单项有足够的间距和错误提示显示空间 */
:deep(.el-form-item) {
  margin-bottom: 28px;
}

/* 确保错误提示完整显示，不被遮挡 */
:deep(.el-form-item__error) {
  position: static !important;
  padding-top: 4px;
  line-height: 1.4;
  word-wrap: break-word;
  white-space: normal;
  max-width: 100%;
  font-size: 12px;
  z-index: 1000;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 4px;
  padding: 4px 8px;
  margin-top: 2px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
}

.header-icon {
  font-size: 48px;
  color: #409eff;
  margin-bottom: 10px;
}

.card-header h2 {
  margin: 0;
  color: #303133;
  font-weight: 500;
}

.form-footer {
  text-align: center;
  width: 100%;
}

.form-footer span {
  color: #909399;
  font-size: 14px;
  margin-right: 8px;
}
</style>
