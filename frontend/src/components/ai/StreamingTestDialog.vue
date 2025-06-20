<template>
  <el-dialog
    v-model="visible"
    title="AI配置流式测试"
    width="80%"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form :model="testForm" label-width="120px" ref="formRef">
      <el-form-item label="测试提示" prop="prompt" :rules="[{ required: true, message: '请输入测试提示' }]">
        <el-input
          v-model="testForm.prompt"
          type="textarea"
          :rows="4"
          placeholder="请输入测试提示..."
          :disabled="testing"
        />
      </el-form-item>
      
      <el-form-item label="最大Token数" prop="maxTokens">
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
          @click="startStreamTest"
          :loading="testing"
          :disabled="testing"
        >
          {{ testing ? '测试中...' : '开始流式测试' }}
        </el-button>
        <el-button @click="stopTest" :disabled="!testing">停止测试</el-button>
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
    
    <!-- 流式响应内容 -->
    <div v-if="streamingContent || testResult" class="streaming-section">
      <el-divider content-position="left">AI响应内容</el-divider>
      
      <el-card class="response-card">
        <div class="streaming-content">
          <!-- 流式内容显示 -->
          <div v-if="streamingContent" class="markdown-content" v-html="renderedContent"></div>
          
          <!-- 打字机效果指示器 -->
          <span v-if="testing" class="typing-indicator">▋</span>
        </div>
      </el-card>
      
      <!-- 测试结果统计 -->
      <div v-if="testResult" class="test-result-stats" style="margin-top: 20px">
        <el-descriptions title="测试统计" :column="3" border>
                     <el-descriptions-item label="提供商">
             <el-tag :type="getProviderTagType(testResult.provider) as any">
               {{ testResult.provider }}
             </el-tag>
           </el-descriptions-item>
          <el-descriptions-item label="模型">
            {{ testResult.model }}
          </el-descriptions-item>
          <el-descriptions-item label="响应时间">
            {{ testResult.duration }}ms
          </el-descriptions-item>
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
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

// Props
interface Props {
  modelValue: boolean
  configId?: number
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

// 响应式数据
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const formRef = ref()
const testing = ref(false)
const reader = ref<ReadableStreamDefaultReader<Uint8Array> | null>(null)
const streamingContent = ref('')
const testResult = ref<any>(null)
const testStatus = ref<any>(null)

const testForm = reactive({
  prompt: '请简单介绍一下人工智能的定义和应用领域。',
  maxTokens: 500
})

// 配置marked
marked.setOptions({
  breaks: true,
  gfm: true
})

// 计算渲染后的markdown内容
const renderedContent = computed(() => {
  if (!streamingContent.value) return ''
  try {
    return marked(streamingContent.value)
  } catch (error) {
    console.error('Markdown parsing error:', error)
    return streamingContent.value.replace(/\n/g, '<br>')
  }
})

// 获取提供商标签类型
const getProviderTagType = (provider: string) => {
  const types: Record<string, string> = {
    'OPENAI': 'success',
    'DEEPSEEK': 'primary',
    'CLAUDE': 'warning',
    'GEMINI': 'info'
  }
  return types[provider] || 'default'
}

// 开始流式测试
const startStreamTest = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    
    testing.value = true
    streamingContent.value = ''
    testResult.value = null
    testStatus.value = {
      type: 'info',
      title: '测试进行中',
      message: '正在连接AI服务，请稍候...'
    }

    const token = localStorage.getItem('token') || sessionStorage.getItem('token')

    // 发送测试参数（通过POST请求）
    const response = await fetch('/api/ai/test-config', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        'Accept': 'text/event-stream'
      },
      body: JSON.stringify({
        configId: props.configId,
        prompt: testForm.prompt,
        maxTokens: testForm.maxTokens
      })
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    if (response.body) {
      reader.value = response.body.getReader()
    }
    const decoder = new TextDecoder()
    
    const readStream = async () => {
      try {
        while (testing.value && reader.value) {
          const { done, value } = await reader.value.read()
          
          if (done) {
            testing.value = false
            break
          }
          
          const chunk = decoder.decode(value, { stream: true })
          const lines = chunk.split('\n')
          
          for (const line of lines) {
            if (line.trim() === '') continue
            
            if (line.startsWith('data: ')) {
              try {
                const jsonStr = line.slice(6).trim()
                if (jsonStr === '') continue
                
                const data = JSON.parse(jsonStr)
                handleStreamEvent(data)
              } catch (e) {
                console.error('Failed to parse SSE data:', line, e)
              }
            } else if (line.startsWith('event: ')) {
              // 处理事件类型
              const eventType = line.slice(7).trim()
              console.log('Event type:', eventType)
            }
          }
        }
      } catch (error) {
        console.error('Stream reading error:', error)
        handleStreamError(error)
      }
    }
    
    readStream()

  } catch (error: any) {
    console.error('测试启动失败:', error)
    ElMessage.error('测试启动失败: ' + error.message)
    testing.value = false
    handleStreamError(error)
  }
}

// 处理流式事件
const handleStreamEvent = (data: any) => {
  console.log('Received stream event:', data)
  
  // 检查是否有事件类型字段
  const eventType = data.event || (data.content ? 'token' : 'unknown')
  
  switch (eventType) {
    case 'start':
      testStatus.value = {
        type: 'info',
        title: '开始测试',
        message: data.message || '开始测试AI配置...'
      }
      break
      
    case 'token':
      if (data.content) {
        streamingContent.value += data.content
      }
      break
      
    case 'complete':
      testing.value = false
      testResult.value = {
        success: data.success,
        message: data.message,
        duration: data.duration,
        provider: data.provider,
        model: data.model,
        inputTokens: data.inputTokens || 0,
        outputTokens: data.outputTokens || 0,
        totalTokens: data.totalTokens || 0,
        estimatedCost: data.estimatedCost || 0
      }
      
      testStatus.value = {
        type: 'success',
        title: '测试完成',
        message: `测试成功完成，用时 ${data.duration}ms`
      }
      
      ElMessage.success('AI配置测试成功完成！')
      break
      
    case 'error':
      testing.value = false
      testStatus.value = {
        type: 'error',
        title: '测试失败',
        message: data.message || '测试过程中发生错误'
      }
      
      ElMessage.error('AI配置测试失败: ' + (data.error || data.message))
      break
      
    default:
      // 如果没有明确的事件类型，但有内容，当作token处理
      if (data.content) {
        streamingContent.value += data.content
      } else {
        console.log('Unknown event type or data:', eventType, data)
      }
      break
  }
}

// 处理流式错误
const handleStreamError = (error: any) => {
  testing.value = false
  testStatus.value = {
    type: 'error',
    title: '连接错误',
    message: '无法连接到AI服务，请检查网络连接'
  }
  
  ElMessage.error('流式连接失败: ' + error.message)
}

// 停止测试
const stopTest = () => {
  if (reader.value) {
    reader.value.cancel()
    reader.value = null
  }
  
  testing.value = false
  testStatus.value = {
    type: 'warning',
    title: '测试已停止',
    message: '用户手动停止了测试'
  }
  
  ElMessage.info('测试已停止')
}

// 清空结果
const clearResult = () => {
  streamingContent.value = ''
  testResult.value = null
  testStatus.value = null
}

// 关闭对话框
const handleClose = () => {
  if (testing.value) {
    ElMessageBox.confirm('测试正在进行中，确定要关闭吗？', '确认关闭', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      stopTest()
      visible.value = false
    }).catch(() => {
      // 用户取消关闭
    })
  } else {
    visible.value = false
  }
}

// 监听对话框关闭，清理资源
watch(visible, (newVal) => {
  if (!newVal) {
    stopTest()
    clearResult()
  }
})
</script>

<style scoped>
.test-status {
  margin-bottom: 20px;
}

.streaming-section {
  margin-top: 20px;
}

.response-card {
  background: #f8f9fa;
  min-height: 200px;
}

.streaming-content {
  position: relative;
  min-height: 150px;
  padding: 16px;
}

.markdown-content {
  line-height: 1.6;
  color: #303133;
  font-size: 14px;
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) {
  margin: 16px 0 8px 0;
  font-weight: 600;
}

.markdown-content :deep(p) {
  margin: 8px 0;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}

.markdown-content :deep(li) {
  margin: 4px 0;
}

.markdown-content :deep(code) {
  background: #f1f3f4;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
}

.markdown-content :deep(pre) {
  background: #f6f8fa;
  border: 1px solid #e1e4e8;
  border-radius: 6px;
  padding: 16px;
  overflow-x: auto;
  margin: 16px 0;
}

.markdown-content :deep(pre code) {
  background: none;
  padding: 0;
}

.markdown-content :deep(blockquote) {
  border-left: 4px solid #dfe2e5;
  padding-left: 16px;
  margin: 16px 0;
  color: #6a737d;
}

.markdown-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 16px 0;
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  border: 1px solid #dfe2e5;
  padding: 8px 12px;
  text-align: left;
}

.markdown-content :deep(th) {
  background: #f6f8fa;
  font-weight: 600;
}

.typing-indicator {
  animation: blink 1s infinite;
  font-weight: bold;
  color: #409eff;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.test-result-stats {
  margin-top: 20px;
}
</style> 