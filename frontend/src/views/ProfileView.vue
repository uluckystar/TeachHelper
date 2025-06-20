<template>
  <div class="profile">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>个人资料</h2>
        </div>
      </template>
      
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名">
          {{ authStore.user?.username }}
        </el-descriptions-item>
        
        <el-descriptions-item label="邮箱">
          {{ authStore.user?.email }}
        </el-descriptions-item>
        
        <el-descriptions-item label="角色">
          <el-tag 
            v-for="role in authStore.user?.roles" 
            :key="role" 
            :type="getRoleTagType(role)"
            style="margin-right: 8px;"
          >
            {{ getRoleText(role) }}
          </el-tag>
        </el-descriptions-item>
        
        <el-descriptions-item label="注册时间">
          {{ formatDate(authStore.user?.createdAt) }}
        </el-descriptions-item>
        
        <el-descriptions-item label="最后更新">
          {{ formatDate(authStore.user?.updatedAt) }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 快捷操作 -->
      <div class="quick-actions" style="margin-top: 20px;">
        <el-button type="primary" @click="goToAIConfig" :icon="Cpu">
          管理AI配置
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { Cpu } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

// 导航到AI配置管理
const goToAIConfig = () => {
  router.push('/ai-config')
}

const getRoleTagType = (role?: string) => {
  switch (role) {
    case 'ADMIN': return 'danger'
    case 'TEACHER': return 'warning'
    case 'STUDENT': return 'success'
    default: return 'info'
  }
}

const getRoleText = (role?: string) => {
  switch (role) {
    case 'ADMIN': return '管理员'
    case 'TEACHER': return '教师'
    case 'STUDENT': return '学生'
    default: return '未知'
  }
}

const formatDate = (dateString?: string) => {
  return dateString ? new Date(dateString).toLocaleString('zh-CN') : '未知'
}
</script>

<style scoped>
.profile {
  max-width: 800px;
  margin: 0 auto;
}

.card-header h2 {
  margin: 0;
  color: #303133;
}
</style>
