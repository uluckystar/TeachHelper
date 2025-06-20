<template>
  <el-config-provider :locale="locale">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { startAuthMonitoring, stopAuthMonitoring } from '@/utils/authMonitor'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const locale = zhCn
const authStore = useAuthStore()

// 应用启动时初始化认证状态
onMounted(async () => {
  await authStore.initAuth()
  
  // 启动认证状态监控
  startAuthMonitoring()
  
  // 监听localStorage变化（比如在其他标签页中退出登录）
  const handleStorageChange = (e: StorageEvent) => {
    // 只处理token相关的关键变化，避免过度干预
    if (e.key === 'token') {
      if (import.meta.env.DEV) {
        console.log('Storage token change detected:', {
          oldValue: !!e.oldValue,
          newValue: !!e.newValue
        })
      }
      
      // 如果token被外部清除，同步清除认证状态
      if (!e.newValue && authStore.token) {
        console.log('Token removed from localStorage, logging out...')
        authStore.logout()
      }
      
      // 避免在token添加时重复初始化，让定期监控处理
    }
  }
  
  window.addEventListener('storage', handleStorageChange)
  
  // 清理事件监听器
  return () => {
    window.removeEventListener('storage', handleStorageChange)
  }
})

onUnmounted(() => {
  stopAuthMonitoring()
})
</script>

<style>
#app {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  background-color: #f5f5f5;
}
</style>
