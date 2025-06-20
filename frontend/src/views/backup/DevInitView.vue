<template>
  <div class="dev-init">
    <div class="init-container">
      <el-card class="init-card">
        <template #header>
          <div class="card-header">
            <h2>🛠️ 开发环境初始化</h2>
            <el-tag type="warning">仅开发环境</el-tag>
          </div>
        </template>
        
        <div class="init-content">
          <el-alert
            title="欢迎使用 TeachHelper 开发环境"
            type="info"
            :closable="false"
            show-icon
          >
            <p>这是一个开发工具页面，用于快速初始化测试数据。</p>
            <p>点击下方按钮将创建默认的用户账号和测试数据。</p>
          </el-alert>
          
          <div class="init-actions">
            <el-button
              type="primary"
              size="large"
              :loading="initializing"
              :disabled="initializing || initialized"
              @click="initializeData"
            >
              <el-icon v-if="!initializing"><Plus /></el-icon>
              {{ initializing ? '正在初始化...' : initialized ? '已初始化' : '初始化数据' }}
            </el-button>
          </div>
          
          <div v-if="initialized" class="user-accounts">
            <el-divider>默认账号信息</el-divider>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="管理员">
                用户名: <el-tag>admin</el-tag> 密码: <el-tag>password</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="教师1">
                用户名: <el-tag>teacher1</el-tag> 密码: <el-tag>password</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="教师2">
                用户名: <el-tag>teacher2</el-tag> 密码: <el-tag>password</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="学生1">
                用户名: <el-tag>student1</el-tag> 密码: <el-tag>password</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="学生2">
                用户名: <el-tag>student2</el-tag> 密码: <el-tag>password</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="学生3">
                用户名: <el-tag>student3</el-tag> 密码: <el-tag>password</el-tag>
              </el-descriptions-item>
            </el-descriptions>
            
            <div class="login-action">
              <el-button
                type="success"
                @click="goToLogin"
              >
                前往登录页面
              </el-button>
            </div>
          </div>
          
          <div v-if="initError" class="error-info">
            <el-alert
              :title="initError"
              type="error"
              show-icon
              :closable="false"
            />
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { devApi } from '@/api/dev'

const router = useRouter()

const initializing = ref(false)
const initialized = ref(false)
const initError = ref('')

const initializeData = async () => {
  try {
    initializing.value = true
    initError.value = ''
    
    await devApi.initializeData()
    
    initialized.value = true
    ElMessage.success('数据初始化成功！现在可以使用默认账号登录了。')
  } catch (error: any) {
    console.error('Failed to initialize data:', error)
    initError.value = error.response?.data?.message || error.message || '初始化失败，请检查后端服务是否正常运行'
    ElMessage.error('数据初始化失败')
  } finally {
    initializing.value = false
  }
}

const goToLogin = () => {
  router.push('/auth/login')
}
</script>

<style scoped>
.dev-init {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.init-container {
  width: 100%;
  max-width: 600px;
}

.init-card {
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border-radius: 16px;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.init-content {
  padding: 20px 0;
}

.init-actions {
  text-align: center;
  margin: 32px 0;
}

.init-actions .el-button {
  width: 200px;
  height: 48px;
  font-size: 16px;
}

.user-accounts {
  margin-top: 24px;
}

.user-accounts .el-tag {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  background-color: #f0f2f5;
  color: #606266;
  border: 1px solid #dcdfe6;
}

.login-action {
  text-align: center;
  margin-top: 24px;
}

.error-info {
  margin-top: 20px;
}

.el-alert {
  margin-bottom: 20px;
}
</style>
