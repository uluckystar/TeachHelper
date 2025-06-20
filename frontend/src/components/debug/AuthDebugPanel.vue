<template>
  <div v-if="isDev" class="auth-debug-panel">
    <el-card header="认证状态调试" size="small" style="margin: 10px; max-width: 400px;">
      <div class="debug-info">
        <div class="status-item">
          <strong>认证状态:</strong>
          <el-tag :type="authStore.isAuthenticated ? 'success' : 'danger'" size="small">
            {{ authStore.isAuthenticated ? '已认证' : '未认证' }}
          </el-tag>
        </div>
        
        <div class="status-item">
          <strong>初始化状态:</strong>
          <el-tag :type="authStore.isInitialized ? 'success' : 'warning'" size="small">
            {{ authStore.isInitialized ? '已初始化' : '未初始化' }}
          </el-tag>
        </div>
        
        <div class="status-item">
          <strong>用户:</strong>
          <span>{{ authStore.user?.username || '无' }}</span>
        </div>
        
        <div class="status-item">
          <strong>角色:</strong>
          <span>{{ authStore.user?.roles?.join(', ') || '无' }}</span>
        </div>
        
        <div class="status-item">
          <strong>Token:</strong>
          <el-tag :type="!!authStore.token ? 'success' : 'danger'" size="small">
            {{ authStore.token ? '存在' : '无' }}
          </el-tag>
        </div>
        
        <div class="status-item">
          <strong>Token长度:</strong>
          <span>{{ authStore.token?.length || 0 }}</span>
        </div>
        
        <div class="status-item">
          <strong>Token过期时间:</strong>
          <span>{{ tokenExpiration || '无法解析' }}</span>
        </div>
        
        <div class="status-item">
          <strong>LocalStorage Token:</strong>
          <el-tag :type="!!localStorageToken ? 'success' : 'danger'" size="small">
            {{ localStorageToken ? '存在' : '无' }}
          </el-tag>
        </div>
        
        <div class="status-item">
          <strong>LocalStorage User:</strong>
          <el-tag :type="!!localStorageUser ? 'success' : 'danger'" size="small">
            {{ localStorageUser ? '存在' : '无' }}
          </el-tag>
        </div>
        
        <div class="status-item">
          <strong>状态同步:</strong>
          <el-tag :type="isInSync ? 'success' : 'warning'" size="small">
            {{ isInSync ? '同步' : '不同步' }}
          </el-tag>
        </div>
      </div>
      
      <div class="debug-actions" style="margin-top: 10px;">
        <el-button-group>
          <el-button size="small" @click="refreshInfo">刷新</el-button>
          <el-button size="small" @click="reinitAuth">重新初始化</el-button>
          <el-button size="small" type="danger" @click="clearAuth">清除认证</el-button>
        </el-button-group>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getTokenExpiration } from '@/utils/authMonitor'

const isDev = computed(() => import.meta.env.DEV)
const authStore = useAuthStore()

const localStorageToken = ref<string | null>(null)
const localStorageUser = ref<string | null>(null)
const updateInterval = ref<number | null>(null)

const tokenExpiration = computed(() => {
  if (!authStore.token) return null
  const exp = getTokenExpiration(authStore.token)
  return exp ? exp.toLocaleString('zh-CN') : null
})

const isInSync = computed(() => {
  const tokenSync = (!!authStore.token) === (!!localStorageToken.value)
  const userSync = (!!authStore.user) === (!!localStorageUser.value)
  return tokenSync && userSync
})

const refreshInfo = () => {
  localStorageToken.value = localStorage.getItem('token')
  localStorageUser.value = localStorage.getItem('user')
}

const reinitAuth = async () => {
  await authStore.initAuth()
  refreshInfo()
}

const clearAuth = () => {
  authStore.logout()
  refreshInfo()
}

onMounted(() => {
  refreshInfo()
  
  // 每秒更新localStorage状态
  updateInterval.value = window.setInterval(refreshInfo, 1000)
})

onUnmounted(() => {
  if (updateInterval.value) {
    clearInterval(updateInterval.value)
  }
})
</script>

<style scoped>
.auth-debug-panel {
  position: fixed;
  top: 60px;
  right: 10px;
  z-index: 1000;
  font-size: 12px;
}

.debug-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 2px 0;
}

.status-item strong {
  margin-right: 8px;
  min-width: 80px;
}
</style>
