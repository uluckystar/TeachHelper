<template>
  <div v-if="isDev" class="navigation-test-panel">
    <el-card>
      <template #header>
        <div class="test-header">
          <el-icon><Tools /></el-icon>
          <span>导航测试面板</span>
        </div>
      </template>
      
      <div class="auth-status">
        <h4>认证状态</h4>
        <p><strong>已初始化:</strong> {{ authStore.isInitialized ? '✅' : '❌' }}</p>
        <p><strong>已认证:</strong> {{ authStore.isAuthenticated ? '✅' : '❌' }}</p>
        <p><strong>当前用户:</strong> {{ authStore.user?.username || '未登录' }}</p>
        <p><strong>角色:</strong> {{ authStore.user?.roles?.join(', ') || '无' }}</p>
        <p><strong>当前路由:</strong> {{ $route.path }}</p>
      </div>
      
      <div class="role-checks">
        <h4>角色检查</h4>
        <p><strong>管理员:</strong> {{ authStore.isAdmin ? '✅' : '❌' }}</p>
        <p><strong>教师:</strong> {{ authStore.isTeacher ? '✅' : '❌' }}</p>
        <p><strong>学生:</strong> {{ authStore.isStudent ? '✅' : '❌' }}</p>
      </div>
      
      <div class="quick-login">
        <h4>快速登录测试</h4>
        <el-space wrap>
          <el-button @click="quickLogin('admin', 'password')" type="danger" size="small">
            登录管理员
          </el-button>
          <el-button @click="quickLogin('teacher1', 'password')" type="primary" size="small">
            登录教师
          </el-button>
          <el-button @click="quickLogin('student1', 'password')" type="success" size="small">
            登录学生
          </el-button>
          <el-button @click="logout" type="info" size="small">
            退出登录
          </el-button>
        </el-space>
      </div>
      
      <div class="navigation-items">
        <h4>导航项可见性</h4>
        <ul class="nav-check-list">
          <li>首页: ✅ (总是显示)</li>
          <li>考试管理: ✅ (总是显示)</li>
          <li>学生功能: {{ authStore.isStudent ? '✅' : '❌' }} (仅学生)</li>
          <li>评估管理: {{ authStore.isTeacher || authStore.isAdmin ? '✅' : '❌' }} (教师/管理员)</li>
          <li>题目管理: {{ authStore.isTeacher || authStore.isAdmin ? '✅' : '❌' }} (教师/管理员)</li>
          <li>知识库: {{ authStore.isTeacher || authStore.isAdmin ? '✅' : '❌' }} (教师/管理员)</li>
          <li>系统管理: {{ authStore.isAdmin ? '✅' : '❌' }} (仅管理员)</li>
          <li>开发工具: {{ isDev ? '✅' : '❌' }} (开发模式)</li>
        </ul>
      </div>
      
      <div class="route-tests">
        <h4>路由快速测试</h4>
        <el-space direction="vertical" style="width: 100%">
          <el-button-group>
            <el-button @click="testRoute('/')" size="small">首页</el-button>
            <el-button @click="testRoute('/profile')" size="small">个人资料</el-button>
            <el-button @click="testRoute('/ai-config')" size="small">AI配置</el-button>
          </el-button-group>
          
          <el-button-group v-if="authStore.isTeacher || authStore.isAdmin">
            <el-button @click="testRoute('/exams')" size="small">考试管理</el-button>
            <el-button @click="testRoute('/questions')" size="small">题目库</el-button>
            <el-button @click="testRoute('/knowledge')" size="small">知识库</el-button>
          </el-button-group>
          
          <el-button-group v-if="authStore.isTeacher || authStore.isAdmin">
            <el-button @click="testRoute('/evaluation/overview')" size="small">批阅概览</el-button>
            <el-button @click="testRoute('/task-center')" size="small">任务中心</el-button>
            <el-button @click="testRoute('/ai-config/dashboard')" size="small">AI仪表板</el-button>
          </el-button-group>
          
          <el-button-group v-if="authStore.isStudent">
            <el-button @click="testRoute('/my-exams')" size="small">我的考试</el-button>
          </el-button-group>
          
          <el-button-group v-if="authStore.isAdmin">
            <el-button @click="testRoute('/users')" size="small">用户管理</el-button>
            <el-button @click="testRoute('/system')" size="small">系统设置</el-button>
          </el-button-group>
          
          <el-button-group v-if="isDev">
            <el-button @click="testRoute('/ai-test')" size="small">AI测试</el-button>
            <el-button @click="testRoute('/streaming-test')" size="small">流式测试</el-button>
          </el-button-group>
        </el-space>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { Tools } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const isDev = computed(() => import.meta.env.DEV)

const quickLogin = async (username: string, password: string) => {
  try {
    const response = await authApi.login({ username, password })
    authStore.setAuth(response)
    ElMessage.success(`已登录为 ${username}`)
    
    // 重新导航到首页以触发导航更新
    if (router.currentRoute.value.path !== '/') {
      router.push('/')
    }
  } catch (error) {
    console.error('Quick login failed:', error)
    ElMessage.error('登录失败')
  }
}

const logout = () => {
  authStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

const testRoute = (path: string) => {
  router.push(path).catch(error => {
    console.error('Navigation failed:', error)
    ElMessage.error(`导航到 ${path} 失败`)
  })
}
</script>

<style scoped>
.navigation-test-panel {
  position: fixed;
  top: 20px;
  right: 20px;
  width: 350px;
  z-index: 9999;
  max-height: 80vh;
  overflow-y: auto;
}

.test-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold;
}

.auth-status,
.role-checks,
.quick-login,
.navigation-items {
  margin-bottom: 16px;
}

.auth-status h4,
.role-checks h4,
.quick-login h4,
.navigation-items h4 {
  margin: 0 0 8px 0;
  color: #409eff;
  font-size: 14px;
}

.auth-status p,
.role-checks p {
  margin: 4px 0;
  font-size: 12px;
  font-family: monospace;
}

.nav-check-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.nav-check-list li {
  font-size: 12px;
  font-family: monospace;
  margin: 2px 0;
}
</style>
