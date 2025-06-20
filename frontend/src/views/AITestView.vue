<template>
  <div class="ai-test-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>统一AI服务测试</h2>
          <el-tag type="info">基于Spring AI的统一服务</el-tag>
        </div>
      </template>

      <el-tabs v-model="activeTab" type="border-card">
        <!-- 基础聊天测试 -->
        <el-tab-pane label="基础聊天" name="chat">
          <el-form :model="chatForm" label-width="120px">
            <el-form-item label="提示内容">
              <el-input
                v-model="chatForm.prompt"
                type="textarea"
                :rows="4"
                placeholder="请输入要测试的提示内容..."
                maxlength="1000"
                show-word-limit
              />
            </el-form-item>
            
            <el-form-item>
              <el-button
                type="primary"
                @click="testChat"
                :loading="chatLoading"
                :disabled="!chatForm.prompt.trim()"
              >
                {{ chatLoading ? '测试中...' : '测试聊天' }}
              </el-button>
              <el-button @click="clearChatResult">清空结果</el-button>
            </el-form-item>
          </el-form>

          <!-- 聊天结果 -->
          <div v-if="chatResult" class="test-result">
            <el-divider content-position="left">测试结果</el-divider>
            
            <el-alert
              :type="chatResult.success ? 'success' : 'error'"
              :title="chatResult.success ? '测试成功' : '测试失败'"
              :description="chatResult.success ? '' : chatResult.errorMessage"
              show-icon
              :closable="false"
              style="margin-bottom: 15px"
            />

            <div v-if="chatResult.success && chatResult.content" class="ai-response">
              <h4>AI响应内容：</h4>
              <el-card class="response-card">
                <div class="response-content">{{ chatResult.content }}</div>
              </el-card>
            </div>
          </div>
        </el-tab-pane>

        <!-- 配置测试 -->
        <el-tab-pane label="配置测试" name="config">
          <el-button
            type="primary"
            @click="testConfig"
            :loading="configLoading"
            size="large"
          >
            {{ configLoading ? '测试中...' : '测试AI配置' }}
          </el-button>

          <div v-if="configResult" class="test-result">
            <el-divider content-position="left">配置测试结果</el-divider>
            
            <el-alert
              :type="configResult.success ? 'success' : 'error'"
              :title="configResult.success ? '配置测试成功' : '配置测试失败'"
              :description="configResult.message"
              show-icon
              :closable="false"
              style="margin-bottom: 15px"
            />

            <div v-if="configResult.success" class="result-details">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="响应时间">
                  {{ configResult.duration }}ms
                </el-descriptions-item>
                <el-descriptions-item label="响应内容">
                  {{ configResult.response }}
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const activeTab = ref('chat')

// 聊天测试
const chatLoading = ref(false)
const chatResult = ref<any>(null)
const chatForm = reactive({
  prompt: '你好，请介绍一下你自己。'
})

// 配置测试
const configLoading = ref(false)
const configResult = ref<any>(null)

// 获取认证token
const getAuthToken = () => {
  return localStorage.getItem('token') || sessionStorage.getItem('token')
}

// 通用API调用函数
const callAPI = async (url: string, data?: any) => {
  const token = getAuthToken()
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: data ? JSON.stringify(data) : undefined
  })

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`)
  }

  return await response.json()
}

// 测试聊天
const testChat = async () => {
  chatLoading.value = true
  chatResult.value = null

  try {
    const result = await callAPI('/api/ai/chat/sync', chatForm)
    chatResult.value = result
    
    if (result.success) {
      ElMessage.success('聊天测试成功')
    } else {
      ElMessage.warning('聊天测试完成，但AI响应失败')
    }
  } catch (error: any) {
    console.error('聊天测试失败:', error)
    ElMessage.error(`聊天测试失败: ${error.message}`)
    chatResult.value = {
      success: false,
      errorMessage: error.message
    }
  } finally {
    chatLoading.value = false
  }
}

// 清空聊天结果
const clearChatResult = () => {
  chatResult.value = null
}

// 测试配置
const testConfig = async () => {
  configLoading.value = true
  configResult.value = null

  try {
    const result = await callAPI('/api/ai/test-config')
    configResult.value = result
    
    if (result.success) {
      ElMessage.success('AI配置测试成功')
    } else {
      ElMessage.warning('AI配置测试失败')
    }
  } catch (error: any) {
    console.error('配置测试失败:', error)
    ElMessage.error(`配置测试失败: ${error.message}`)
    configResult.value = {
      success: false,
      message: error.message
    }
  } finally {
    configLoading.value = false
  }
}
</script>

<style scoped>
.ai-test-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  color: #303133;
}

.test-result {
  margin-top: 20px;
}

.result-details {
  margin-top: 15px;
}

.ai-response {
  margin-top: 20px;
}

.ai-response h4 {
  margin-bottom: 10px;
  color: #303133;
}

.response-card {
  background: #f5f7fa;
}

.response-content {
  white-space: pre-wrap;
  line-height: 1.6;
  color: #303133;
}
</style>