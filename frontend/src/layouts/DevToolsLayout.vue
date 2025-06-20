<template>
  <el-container class="dev-tools-layout" direction="vertical">
    <el-header class="dev-header">
      <div class="header-content">
        <div class="logo">
          <el-icon class="logo-icon"><Tools /></el-icon>
          <span class="logo-text">TeachHelper 开发工具</span>
        </div>
        
        <div class="header-actions">
          <el-tag type="warning" size="small">开发环境</el-tag>
          <el-button 
            size="small" 
            @click="goBack"
            :icon="ArrowLeft"
          >
            返回主页面
          </el-button>
          <el-dropdown @command="handleUserCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              {{ authStore.user?.username || '未登录' }}
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
    
    <el-main class="dev-main">
      <slot />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import {
  Tools,
  ArrowLeft,
  User,
  ArrowDown,
  SwitchButton,
  Cpu
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const goBack = () => {
  // 尝试关闭窗口，如果不能关闭（比如不是弹出窗口），则导航到主页
  if (window.opener) {
    window.close()
  } else {
    window.location.href = '/'
  }
}

const handleUserCommand = (command: string) => {
  switch (command) {
    case 'profile':
      // 在父窗口中打开个人资料页面
      if (window.opener) {
        window.opener.location.href = '/profile'
      } else {
        router.push('/profile')
      }
      break
    case 'ai-config':
      // 在父窗口中打开AI配置页面
      if (window.opener) {
        window.opener.location.href = '/ai-config'
      } else {
        router.push('/ai-config')
      }
      break
    case 'logout':
      authStore.logout()
      ElMessage.success('已退出登录')
      // 关闭当前窗口或导航到登录页
      if (window.opener) {
        window.close()
        window.opener.location.href = '/login'
      } else {
        router.push('/login')
      }
      break
  }
}
</script>

<style scoped>
.dev-tools-layout {
  height: 100vh;
  background: #f5f5f5;
}

.dev-header {
  background: #fff;
  border-bottom: 2px solid #409eff;
  padding: 0;
  height: 60px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
}

.logo {
  display: flex;
  align-items: center;
}

.logo-icon {
  font-size: 24px;
  color: #409eff;
  margin-right: 8px;
}

.logo-text {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
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

.dev-main {
  padding: 0;
  background: #f5f5f5;
  overflow: auto;
}
</style>
