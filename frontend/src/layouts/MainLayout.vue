<template>
  <el-container class="main-layout" direction="vertical">
    <!-- 认证状态加载中时显示骨架屏 -->
    <div v-if="!authStore.isInitialized" class="loading-container">
      <el-skeleton :rows="3" animated />
    </div>
    
    <!-- 认证状态加载完成后显示正常界面 -->
    <template v-else>
      <el-header class="main-header">
        <div class="header-content">
          <div class="logo">
            <el-icon class="logo-icon"><Reading /></el-icon>
            <span class="logo-text">TeachHelper</span>
          </div>
        
        <el-menu
          :default-active="activeIndex"
          mode="horizontal"
          class="main-nav"
          @select="handleMenuSelect"
        >
          <el-menu-item index="/">
            <el-icon><House /></el-icon>
            <span>首页</span>
          </el-menu-item>
          
          <!-- 教师/管理员考试管理 -->
          <el-sub-menu v-if="authStore.isTeacher || authStore.isAdmin" index="exams">
            <template #title>
              <el-icon><Document /></el-icon>
              <span>考试管理</span>
            </template>
            <el-menu-item index="/exams">考试管理中心</el-menu-item>
            <el-menu-item index="/exams/create">创建考试</el-menu-item>
            <el-menu-item index="/exam-templates">试卷模板</el-menu-item>
            <el-menu-item index="/paper-generation">AI试卷生成</el-menu-item>
          </el-sub-menu>
          
          <!-- 学生考试功能 -->
          <el-sub-menu v-if="authStore.isStudent" index="student">
            <template #title>
              <el-icon><UserFilled /></el-icon>
              <span>学生功能</span>
            </template>
            <el-menu-item index="/exams">参加考试</el-menu-item>
            <el-menu-item index="/my-exams">我的考试</el-menu-item>
          </el-sub-menu>
          
          <el-sub-menu v-if="authStore.isTeacher || authStore.isAdmin" index="questions">
            <template #title>
              <el-icon><EditPen /></el-icon>
              <span>题目管理</span>
            </template>
            <el-menu-item index="/questions">题目库</el-menu-item>
            <el-menu-item index="/questions/create">创建题目</el-menu-item>
          </el-sub-menu>
          
          <el-sub-menu v-if="authStore.isTeacher || authStore.isAdmin" index="knowledge">
            <template #title>
              <el-icon><Collection /></el-icon>
              <span>知识库</span>
            </template>
            <el-menu-item index="/knowledge">知识库管理</el-menu-item>
            <el-menu-item index="/knowledge/dashboard">数据概览</el-menu-item>
            <el-menu-item index="/knowledge/upload">文档上传</el-menu-item>
          </el-sub-menu>
          
          <el-sub-menu v-if="authStore.isTeacher || authStore.isAdmin" index="tasks">
            <template #title>
              <el-icon><Timer /></el-icon>
              <span>任务管理</span>
            </template>
            <el-menu-item index="/task-center">任务中心</el-menu-item>
            <el-menu-item index="/task-monitor">任务监控</el-menu-item>
            <el-menu-item index="/evaluation/center">AI批阅中心</el-menu-item>
            <el-menu-item index="/batch-evaluation">批量批阅</el-menu-item>
          </el-sub-menu>
          
          <el-sub-menu v-if="authStore.isAdmin" index="admin">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/users">用户管理</el-menu-item>
            <el-menu-item index="/metadata">基础数据管理</el-menu-item>
            <el-menu-item index="/system">系统设置</el-menu-item>
            <el-menu-item index="/ai-config">AI配置管理</el-menu-item>
          </el-sub-menu>
          
          <el-menu-item v-if="isDev" index="/dev-tools">
            <el-icon><Tools /></el-icon>
            <span>开发工具</span>
          </el-menu-item>
        </el-menu>
        
        <div class="header-actions">
          <el-dropdown @command="handleUserCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              {{ authStore.user?.username }}
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人资料
                </el-dropdown-item>
                <el-dropdown-item command="ai-config">
                  <el-icon><Cpu /></el-icon>
                  AI配置中心
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>
       <el-main class="main-content">
      <router-view />
    </el-main>
    </template>
  </el-container>
</template>

<script setup lang="ts">
import { computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import {
  House,
  Document,
  ChatLineRound,
  UserFilled,
  EditPen,
  Setting,
  Tools,
  User,
  ArrowDown,
  SwitchButton,
  Reading,
  Cpu,
  Collection,
  Timer
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const activeIndex = computed(() => route.path)
const isDev = computed(() => import.meta.env.DEV)

// 调试信息 - 在开发环境下显示
onMounted(() => {
  if (isDev.value) {
    console.log('MainLayout mounted - Auth state:', {
      isInitialized: authStore.isInitialized,
      isAuthenticated: authStore.isAuthenticated,
      user: authStore.user,
      roles: authStore.user?.roles
    })
  }
})

// 监听认证状态变化，确保导航菜单及时更新
watch(() => authStore.user, (newUser, oldUser) => {
  if (isDev.value) {
    console.log('Auth user changed:', { 
      oldUser: oldUser?.username, 
      newUser: newUser?.username,
      oldRoles: oldUser?.roles,
      newRoles: newUser?.roles
    })
  }
  if (!newUser && route.path !== '/login') {
    router.push('/login')
  }
}, { immediate: false })

watch(() => authStore.isInitialized, (isInitialized) => {
  if (isDev.value) {
    console.log('Auth initialization changed:', isInitialized, {
      isAuthenticated: authStore.isAuthenticated,
      user: authStore.user?.username,
      roles: authStore.user?.roles
    })
  }
}, { immediate: false })

// 监听token变化，确保token失效时能及时处理
watch(() => authStore.token, (newToken, oldToken) => {
  if (isDev.value) {
    console.log('Auth token changed:', { 
      hadToken: !!oldToken, 
      hasToken: !!newToken 
    })
  }
  if (!newToken && oldToken && route.path !== '/login') {
    // token被清除，重定向到登录页
    router.push('/login')
  }
}, { immediate: false })

const handleMenuSelect = (index: string) => {
  if (index === '/dev-tools') {
    // 开发工具页面在新窗口中打开
    window.open(index, '_blank')
  } else {
    router.push(index)
  }
}

const handleUserCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'ai-config':
      router.push('/ai-config')
      break
    case 'logout':
      authStore.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
      break
  }
}
</script>

<style scoped>
.main-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  padding: 40px;
}

.main-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0;
  flex-shrink: 0;
  height: 60px;
}

.header-content {
  display: flex;
  align-items: center;
  height: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.logo {
  display: flex;
  align-items: center;
  margin-right: 30px;
}

.logo-icon {
  font-size: 24px;
  color: #409eff;
  margin-right: 8px;
}

.logo-text {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.main-nav {
  flex: 1;
  border-bottom: none;
}

.header-actions {
  margin-left: auto;
}

.user-info {
  display: flex;
  align-items: center;
  padding: 0 12px;
  cursor: pointer;
  color: #606266;
  font-size: 14px;
}

.user-info:hover {
  color: #409eff;
}

.main-content {
  background: #f5f5f5;
  padding: 20px;
  flex: 1;
  overflow: auto;
}
</style>
