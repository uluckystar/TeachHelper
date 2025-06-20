<template>
  <div class="ai-test-page">
    <el-card>
      <template #header>
        <h2>AI配置测试验证</h2>
      </template>
      
      <el-form :model="testForm" label-width="120px">
        <el-form-item label="测试提示">
          <el-input
            v-model="testForm.prompt"
            type="textarea"
            :rows="4"
            placeholder="请输入测试提示..."
          />
        </el-form-item>
        
        <el-form-item label="最大Token数">
          <el-input-number
            v-model="testForm.maxTokens"
            :min="10"
            :max="2000"
            :step="10"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            @click="testAIConfig"
            :loading="testing"
          >
            {{ testing ? '测试中...' : '测试AI配置' }}
          </el-button>
          <el-button @click="clearResult">清空结果</el-button>
        </el-form-item>
      </el-form>
      
      <!-- 测试结果 -->
      <div v-if="testResult" class="test-result">
        <el-divider content-position="left">测试结果</el-divider>
        
        <el-alert
          :type="testResult.success ? 'success' : 'error'"
          :title="testResult.success ? '测试成功' : '测试失败'"
          :description="testResult.message"
          show-icon
          :closable="false"
          style="margin-bottom: 20px"
        />
        
        <div v-if="testResult.success">
          <!-- 基本信息 -->
          <el-descriptions title="响应信息" :column="2" border>
            <el-descriptions-item label="提供商">
              {{ testResult.provider }}
            </el-descriptions-item>
            <el-descriptions-item label="模型">
              {{ testResult.model }}
            </el-descriptions-item>
            <el-descriptions-item label="响应时间">
              {{ testResult.duration }}ms
            </el-descriptions-item>
            <el-descriptions-item label="请求状态">
              <el-tag type="success">成功</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          
          <!-- Token统计 -->
          <el-descriptions title="Token统计" :column="3" border style="margin-top: 20px">
            <el-descriptions-item label="输入Token">
              <el-tag type="info">{{ testResult.inputTokens }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="输出Token">
              <el-tag type="warning">{{ testResult.outputTokens }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="总Token">
              <el-tag type="primary">{{ testResult.totalTokens }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="预估费用" :span="3">
              <el-tag type="danger">${{ testResult.estimatedCost.toFixed(6) }}</el-tag>
              <span style="margin-left: 10px; color: #909399;">
                (约 ¥{{ (testResult.estimatedCost * 7.3).toFixed(4) }})
              </span>
            </el-descriptions-item>
          </el-descriptions>
          
          <!-- AI响应内容 -->
          <div class="ai-response" style="margin-top: 20px">
            <h4>AI响应内容：</h4>
            <el-card class="response-card">
              <div class="response-content">{{ testResult.response }}</div>
            </el-card>
          </div>
        </div>
        
        <div v-else class="error-info">
          <h4>错误信息：</h4>
          <el-alert
            type="error"
            :description="testResult.error"
            show-icon
            :closable="false"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const testing = ref(false)
const testResult = ref<any>(null)

const testForm = reactive({
  prompt: '请简单介绍一下人工智能的定义和应用领域。',
  maxTokens: 500
})

const testAIConfig = async () => {
  testing.value = true
  testResult.value = null
  
  try {
    ElMessage.info('开始测试AI配置，请稍候...')
    
    const response = await fetch('/api/ai/test-config', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token') || sessionStorage.getItem('token')}`
      },
      body: JSON.stringify({
        prompt: testForm.prompt,
        maxTokens: testForm.maxTokens
      })
    })
    
    const result = await response.json()
    
    testResult.value = {
      success: result.success,
      message: result.message,
      response: result.response,
      duration: result.duration,
      provider: result.provider,
      model: result.model,
      inputTokens: result.inputTokens || 0,
      outputTokens: result.outputTokens || 0,
      totalTokens: result.totalTokens || 0,
      estimatedCost: result.estimatedCost || 0,
      error: result.error
    }
    
    if (result.success) {
      ElMessage.success('AI配置测试成功！')
    } else {
      ElMessage.error('AI配置测试失败')
    }
    
  } catch (error: any) {
    console.error('测试失败:', error)
    ElMessage.error('测试请求失败: ' + error.message)
    
    testResult.value = {
      success: false,
      message: '测试请求失败',
      error: error.message
    }
  } finally {
    testing.value = false
  }
}

const clearResult = () => {
  testResult.value = null
}
</script>

<style scoped>
.ai-test-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.test-result {
  margin-top: 20px;
}

.response-card {
  background: #f5f7fa;
}

.response-content {
  white-space: pre-wrap;
  line-height: 1.6;
  color: #303133;
  font-size: 14px;
}

.error-info {
  margin-top: 20px;
}

.error-info h4 {
  margin-bottom: 10px;
  color: #F56C6C;
}
</style>