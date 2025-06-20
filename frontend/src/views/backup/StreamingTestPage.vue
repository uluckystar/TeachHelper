<template>
  <div class="streaming-test-page">
    <el-card>
      <template #header>
        <h2>AI配置流式测试验证</h2>
      </template>
      
      <el-form :model="testForm" label-width="120px">
        <el-form-item label="测试提示">
          <el-input
            v-model="testForm.prompt"
            type="textarea"
            :rows="4"
            placeholder="请输入测试提示..."
            :disabled="testing"
          />
        </el-form-item>
        
        <el-form-item label="最大Token数">
          <el-input-number
            v-model="testForm.maxTokens"
            :min="10"
            :max="2000"
            :step="10"
            :disabled="testing"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            @click="startTest"
            :loading="testing"
          >
            {{ testing ? '测试中...' : '开始测试' }}
          </el-button>
          <el-button @click="clearResult">清空结果</el-button>
        </el-form-item>
      </el-form>
      
      <!-- 测试状态 -->
      <div v-if="testStatus" class="test-status">
        <el-alert
          :type="testStatus.type"
          :title="testStatus.title"
          :description="testStatus.message"
          show-icon
          :closable="false"
          style="margin-bottom: 20px"
        />
      </div>
      
      <!-- 响应内容 -->
      <div v-if="responseContent || testResult" class="response-section">
        <el-divider content-position="left">AI响应</el-divider>
        
        <el-card class="response-card">
          <div class="response-content">
            <div v-if="responseContent" class="streaming-text">
              {{ responseContent }}
              <span v-if="testing" class="cursor">▋</span>
            </div>
          </div>
        </el-card>
        
        <!-- 统计信息 -->
        <div v-if="testResult" class="stats-section" style="margin-top: 20px">
          <el-descriptions title="测试统计" :column="3" border>
            <el-descriptions-item label="提供商">
              {{ testResult.provider }}
            </el-descriptions-item>
            <el-descriptions-item label="模型">
              {{ testResult.model }}
            </el-descriptions-item>
            <el-descriptions-item label="响应时间">
              {{ testResult.duration }}ms
            </el-descriptions-item>
            <el-descriptions-item label="输入Token">
              {{ testResult.inputTokens }}
            </el-descriptions-item>
            <el-descriptions-item label="输出Token">
              {{ testResult.outputTokens }}
            </el-descriptions-item>
            <el-descriptions-item label="总Token">
              {{ testResult.totalTokens }}
            </el-descriptions-item>
            <el-descriptions-item label="预估费用" :span="3">
              ${{ testResult.estimatedCost.toFixed(6) }}
              (约 ¥{{ (testResult.estimatedCost * 7.3).toFixed(4) }})
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const testing = ref(false)
const responseContent = ref('')
const testResult = ref<any>(null)
const testStatus = ref<any>(null)

const testForm = reactive({
  prompt: '请简单介绍一下人工智能的定义和应用领域。',
  maxTokens: 500
})

const startTest = async () => {
  testing.value = true
  responseContent.value = ''
  testResult.value = null
  testStatus.value = {
    type: 'info',
    title: '测试进行中',
    message: '正在连接AI服务...'
  }

  try {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    
    // 使用同步版本进行测试
    const response = await fetch('/api/ai/test-config-sync', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        prompt: testForm.prompt,
        maxTokens: testForm.maxTokens
      })
    })

    const result = await response.json()
    
    if (result.success) {
      // 模拟流式显示效果
      const content = result.response
      let index = 0
      
      const showNextChar = () => {
        if (index < content.length) {
          responseContent.value += content[index]
          index++
          setTimeout(showNextChar, 50) // 50ms间隔显示下一个字符
        } else {
          // 显示完成，设置结果
          testing.value = false
          testResult.value = {
            provider: result.provider,
            model: result.model,
            duration: result.duration,
            inputTokens: result.inputTokens || 0,
            outputTokens: result.outputTokens || 0,
            totalTokens: result.totalTokens || 0,
            estimatedCost: result.estimatedCost || 0
          }
          
          testStatus.value = {
            type: 'success',
            title: '测试完成',
            message: `测试成功完成，用时 ${result.duration}ms`
          }
          
          ElMessage.success('AI配置测试成功！')
        }
      }
      
      showNextChar()
      
    } else {
      testing.value = false
      testStatus.value = {
        type: 'error',
        title: '测试失败',
        message: result.message || '测试失败'
      }
      ElMessage.error('测试失败: ' + result.message)
    }
    
  } catch (error: any) {
    testing.value = false
    testStatus.value = {
      type: 'error',
      title: '连接错误',
      message: '无法连接到AI服务'
    }
    ElMessage.error('测试失败: ' + error.message)
  }
}

const clearResult = () => {
  responseContent.value = ''
  testResult.value = null
  testStatus.value = null
}
</script>

<style scoped>
.streaming-test-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.test-status {
  margin-bottom: 20px;
}

.response-section {
  margin-top: 20px;
}

.response-card {
  background: #f8f9fa;
  min-height: 200px;
}

.response-content {
  padding: 16px;
  min-height: 150px;
}

.streaming-text {
  line-height: 1.6;
  color: #303133;
  font-size: 14px;
  white-space: pre-wrap;
}

.cursor {
  animation: blink 1s infinite;
  font-weight: bold;
  color: #409eff;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.stats-section {
  margin-top: 20px;
}
</style> 