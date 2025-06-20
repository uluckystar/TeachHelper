<template>
  <DevToolsLayout>
    <div class="dev-tools-center">
      <el-card>
        <template #header>
          <div class="page-header">
            <h2>🛠️ 开发工具中心</h2>
            <el-tag type="warning" size="small">仅开发环境</el-tag>
          </div>
        </template>

      <!-- 标签页 -->
      <el-tabs v-model="activeTab" @tab-click="handleTabChange">
        <!-- 数据初始化 -->
        <el-tab-pane label="数据初始化" name="data-init">
          <div class="dev-init">
            <div class="init-container">
              <el-alert
                title="开发环境数据初始化"
                type="info"
                :closable="false"
                show-icon
                style="margin-bottom: 20px"
              >
                <p>这个工具用于快速初始化测试数据，包括用户账号、考试、题目、答案等。</p>
                <p>点击下方按钮将创建默认的用户账号和测试数据。</p>
              </el-alert>
              
              <div class="init-actions">
                <el-button
                  type="primary"
                  size="large"
                  :loading="initializing"
                  :disabled="initializing || initialized"
                  @click="initializeData"
                >
                  <el-icon v-if="!initializing"><Plus /></el-icon>
                  {{ initializing ? '正在初始化...' : initialized ? '已初始化' : '初始化数据' }}
                </el-button>
              </div>
              
              <div v-if="initialized" class="user-accounts">
                <el-divider>默认账号信息</el-divider>
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="管理员">
                    用户名: <el-tag>admin</el-tag> 密码: <el-tag>password</el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="教师1">
                    用户名: <el-tag>teacher1</el-tag> 密码: <el-tag>password</el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="教师2">
                    用户名: <el-tag>teacher2</el-tag> 密码: <el-tag>password</el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="学生1">
                    用户名: <el-tag>student1</el-tag> 密码: <el-tag>password</el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="学生2">
                    用户名: <el-tag>student2</el-tag> 密码: <el-tag>password</el-tag>
                  </el-descriptions-item>
                </el-descriptions>
              </div>
              
              <div v-if="initialized" class="generated-data">
                <el-divider>生成的数据</el-divider>
                <el-alert
                  type="success"
                  :closable="false"
                  show-icon
                >
                  <p>已生成的测试数据包括：</p>
                  <ul>
                    <li>13个用户（1个管理员，4个教师，8个学生）</li>
                    <li>5个考试</li>
                    <li>20个题目和对应的评分标准</li>
                    <li>80个学生答案</li>
                    <li>AI配置和使用统计数据</li>
                    <li>试卷模板和知识库数据</li>
                  </ul>
                </el-alert>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- AI测试 -->
        <el-tab-pane label="AI测试" name="ai-test">
          <div class="ai-test-page">
            <el-alert
              title="AI配置测试验证"
              description="测试当前AI配置的可用性和响应质量"
              type="info"
              :closable="false"
              show-icon
              style="margin-bottom: 20px"
            />
            
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
                <!-- AI响应 -->
                <el-card class="response-card" style="margin-bottom: 20px">
                  <template #header>
                    <h4>AI响应内容</h4>
                  </template>
                  <div class="response-content">
                    {{ testResult.response }}
                  </div>
                </el-card>
                
                <!-- 测试统计 -->
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
          </div>
        </el-tab-pane>

        <!-- 流式测试 -->
        <el-tab-pane label="流式测试" name="streaming-test">
          <div class="streaming-test-page">
            <el-alert
              title="AI配置流式测试验证"
              description="测试AI配置的流式响应功能，观察实时内容生成"
              type="info"
              :closable="false"
              show-icon
              style="margin-bottom: 20px"
            />
            
            <el-form :model="streamingForm" label-width="120px">
              <el-form-item label="测试提示">
                <el-input
                  v-model="streamingForm.prompt"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入测试提示..."
                  :disabled="streamingTesting"
                />
              </el-form-item>
              
              <el-form-item label="最大Token数">
                <el-input-number
                  v-model="streamingForm.maxTokens"
                  :min="10"
                  :max="2000"
                  :step="10"
                  :disabled="streamingTesting"
                />
              </el-form-item>
              
              <el-form-item>
                <el-button
                  type="primary"
                  @click="startStreamingTest"
                  :loading="streamingTesting"
                >
                  {{ streamingTesting ? '测试中...' : '开始流式测试' }}
                </el-button>
                <el-button @click="stopStreamingTest" :disabled="!streamingTesting">停止测试</el-button>
                <el-button @click="clearStreamingResult">清空结果</el-button>
              </el-form-item>
            </el-form>
            
            <!-- 测试状态 -->
            <div v-if="streamingStatus" class="test-status">
              <el-alert
                :type="streamingStatus.type"
                :title="streamingStatus.title"
                :description="streamingStatus.message"
                show-icon
                :closable="false"
                style="margin-bottom: 20px"
              />
            </div>
            
            <!-- 响应内容 -->
            <div v-if="streamingContent || streamingResult" class="response-section">
              <el-divider content-position="left">AI流式响应</el-divider>
              
              <el-card class="response-card">
                <div class="streaming-content">
                  <div v-if="streamingContent" class="streaming-text">
                    {{ streamingContent }}
                    <span v-if="streamingTesting" class="cursor">▋</span>
                  </div>
                </div>
              </el-card>
              
              <!-- 统计信息 -->
              <div v-if="streamingResult" class="streaming-stats" style="margin-top: 20px">
                <el-descriptions title="流式测试统计" :column="3" border>
                  <el-descriptions-item label="提供商">
                    <el-tag>{{ streamingResult.provider }}</el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="模型">
                    {{ streamingResult.model }}
                  </el-descriptions-item>
                  <el-descriptions-item label="响应时间">
                    {{ streamingResult.duration }}ms
                  </el-descriptions-item>
                  <el-descriptions-item label="输入Token">
                    <el-tag type="info">{{ streamingResult.inputTokens }}</el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="输出Token">
                    <el-tag type="warning">{{ streamingResult.outputTokens }}</el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="总Token">
                    <el-tag type="primary">{{ streamingResult.totalTokens }}</el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="预估费用" :span="3">
                    <el-tag type="danger">${{ streamingResult.estimatedCost.toFixed(6) }}</el-tag>
                    <span style="margin-left: 10px; color: #909399;">
                      (约 ¥{{ (streamingResult.estimatedCost * 7.3).toFixed(4) }})
                    </span>
                  </el-descriptions-item>
                </el-descriptions>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 导航测试面板 -->
        <el-tab-pane label="导航测试" name="navigation-test">
          <div class="navigation-test-section">
            <el-alert
              title="导航测试面板"
              description="用于测试不同用户角色的导航权限和路由功能"
              type="info"
              :closable="false"
              show-icon
              style="margin-bottom: 20px"
            />
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-card header="认证状态">
                  <el-descriptions :column="1" border>
                    <el-descriptions-item label="已初始化">
                      {{ authStore.isInitialized ? '✅' : '❌' }}
                    </el-descriptions-item>
                    <el-descriptions-item label="已认证">
                      {{ authStore.isAuthenticated ? '✅' : '❌' }}
                    </el-descriptions-item>
                    <el-descriptions-item label="当前用户">
                      {{ authStore.user?.username || '未登录' }}
                    </el-descriptions-item>
                    <el-descriptions-item label="角色">
                      {{ authStore.user?.roles?.join(', ') || '无' }}
                    </el-descriptions-item>
                    <el-descriptions-item label="当前路由">
                      {{ $route.path }}
                    </el-descriptions-item>
                  </el-descriptions>
                </el-card>
              </el-col>
              
              <el-col :span="12">
                <el-card header="角色权限检查">
                  <el-descriptions :column="1" border>
                    <el-descriptions-item label="管理员权限">
                      {{ authStore.isAdmin ? '✅' : '❌' }}
                    </el-descriptions-item>
                    <el-descriptions-item label="教师权限">
                      {{ authStore.isTeacher ? '✅' : '❌' }}
                    </el-descriptions-item>
                    <el-descriptions-item label="学生权限">
                      {{ authStore.isStudent ? '✅' : '❌' }}
                    </el-descriptions-item>
                    <el-descriptions-item label="开发模式">
                      {{ isDev ? '✅' : '❌' }}
                    </el-descriptions-item>
                  </el-descriptions>
                </el-card>
              </el-col>
            </el-row>

            <el-card header="快速登录测试" style="margin-top: 20px">
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
            </el-card>

            <el-card header="路由快速测试" style="margin-top: 20px">
              <el-space direction="vertical" style="width: 100%">
                <el-space wrap>
                  <el-button @click="testRoute('/')" size="small">首页</el-button>
                  <el-button @click="testRoute('/profile')" size="small">个人资料</el-button>
                  <el-button @click="testRoute('/ai-config')" size="small">AI配置</el-button>
                </el-space>
                
                <el-space wrap v-if="authStore.isTeacher || authStore.isAdmin">
                  <el-button @click="testRoute('/exams')" size="small">考试管理</el-button>
                  <el-button @click="testRoute('/questions')" size="small">题目库</el-button>
                  <el-button @click="testRoute('/knowledge')" size="small">知识库</el-button>
                </el-space>
                
                <el-space wrap v-if="authStore.isTeacher || authStore.isAdmin">
                  <el-button @click="testRoute('/evaluation/overview')" size="small">批阅概览</el-button>
                  <el-button @click="testRoute('/task-center')" size="small">任务中心</el-button>
                </el-space>
                
                <el-space wrap v-if="authStore.isStudent">
                  <el-button @click="testRoute('/exams')" size="small">参加考试</el-button>
                  <el-button @click="testRoute('/my-exams')" size="small">我的考试</el-button>
                </el-space>
                
                <el-space wrap v-if="authStore.isAdmin">
                  <el-button @click="testRoute('/users')" size="small">用户管理</el-button>
                  <el-button @click="testRoute('/system')" size="small">系统设置</el-button>
                </el-space>
              </el-space>
            </el-card>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
    </div>
  </DevToolsLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { devApi } from '../api/dev'
import { authApi } from '../api/auth'
import DevToolsLayout from '../layouts/DevToolsLayout.vue'

const router = useRouter()
const authStore = useAuthStore()

// 响应式数据
const activeTab = ref('data-init')
const initializing = ref(false)
const initialized = ref(false)
const testing = ref(false)
const testResult = ref<any>(null)
const streamingTesting = ref(false)
const streamingContent = ref('')
const streamingResult = ref<any>(null)
const streamingStatus = ref<any>(null)
const streamingReader = ref<ReadableStreamDefaultReader<Uint8Array> | null>(null)

const isDev = computed(() => import.meta.env.DEV)

// 表单数据
const testForm = reactive({
  prompt: '请简单介绍一下人工智能的定义。',
  maxTokens: 500
})

const streamingForm = reactive({
  prompt: '请简单介绍一下人工智能的定义和应用领域。',
  maxTokens: 500
})

// 数据初始化方法
const initializeData = async () => {
  initializing.value = true
  try {
    await devApi.initializeData()
    initialized.value = true
    ElMessage.success('数据初始化成功！')
  } catch (error: any) {
    console.error('数据初始化失败:', error)
    ElMessage.error(`数据初始化失败: ${error.message || '未知错误'}`)
  } finally {
    initializing.value = false
  }
}

// AI测试方法
const testAIConfig = async () => {
  testing.value = true
  testResult.value = null
  
  try {
    // 模拟AI配置测试 - 这里需要根据实际API调整
    const response = await fetch('/api/ai/test-simple', {
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
    
    if (response.ok) {
      testResult.value = await response.json()
      ElMessage.success('AI测试完成')
    } else {
      throw new Error(`HTTP ${response.status}`)
    }
  } catch (error: any) {
    console.error('AI测试失败:', error)
    ElMessage.error('AI测试失败')
    testResult.value = {
      success: false,
      message: '网络错误或服务不可用',
      error: error.message || '未知错误'
    }
  } finally {
    testing.value = false
  }
}

const clearResult = () => {
  testResult.value = null
}

// 流式测试方法
const startStreamingTest = async () => {
  streamingTesting.value = true
  streamingContent.value = ''
  streamingResult.value = null
  streamingStatus.value = {
    type: 'info',
    title: '测试进行中',
    message: '正在连接AI服务，请稍候...'
  }

  try {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')

    const response = await fetch('/api/ai/test-config', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        'Accept': 'text/event-stream'
      },
      body: JSON.stringify({
        prompt: streamingForm.prompt,
        maxTokens: streamingForm.maxTokens
      })
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    if (response.body) {
      streamingReader.value = response.body.getReader()
    }
    const decoder = new TextDecoder()
    
    const readStream = async () => {
      try {
        while (streamingTesting.value && streamingReader.value) {
          const { done, value } = await streamingReader.value.read()
          
          if (done) {
            streamingTesting.value = false
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
    console.error('流式测试启动失败:', error)
    ElMessage.error('流式测试启动失败: ' + error.message)
    streamingTesting.value = false
    handleStreamError(error)
  }
}

const handleStreamEvent = (data: any) => {
  const eventType = data.event || (data.content ? 'token' : 'unknown')
  
  switch (eventType) {
    case 'start':
      streamingStatus.value = {
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
      streamingTesting.value = false
      streamingResult.value = {
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
      
      streamingStatus.value = {
        type: 'success',
        title: '测试完成',
        message: `测试成功完成，用时 ${data.duration}ms`
      }
      
      ElMessage.success('AI配置流式测试成功完成！')
      break
      
    case 'error':
      streamingTesting.value = false
      streamingStatus.value = {
        type: 'error',
        title: '测试失败',
        message: data.message || '测试过程中发生错误'
      }
      
      ElMessage.error('AI配置流式测试失败: ' + (data.error || data.message))
      break
      
    default:
      if (data.content) {
        streamingContent.value += data.content
      }
      break
  }
}

const handleStreamError = (error: any) => {
  streamingTesting.value = false
  streamingStatus.value = {
    type: 'error',
    title: '连接错误',
    message: '无法连接到AI服务，请检查网络连接'
  }
  
  ElMessage.error('流式连接失败: ' + error.message)
}

const stopStreamingTest = () => {
  if (streamingReader.value) {
    streamingReader.value.cancel()
    streamingReader.value = null
  }
  
  streamingTesting.value = false
  streamingStatus.value = {
    type: 'warning',
    title: '测试已停止',
    message: '用户手动停止了测试'
  }
  
  ElMessage.info('流式测试已停止')
}

const clearStreamingResult = () => {
  streamingContent.value = ''
  streamingResult.value = null
  streamingStatus.value = null
}

// 导航测试方法
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

const handleTabChange = (tab: any) => {
  console.log('Tab changed to:', tab.paneName)
}
</script>

<style scoped>
.dev-tools-center {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-header h2 {
  margin: 0;
  color: #303133;
}

/* 数据初始化样式 */
.init-container {
  max-width: 800px;
  margin: 0 auto;
}

.init-actions {
  text-align: center;
  margin: 30px 0;
}

.user-accounts {
  margin-top: 30px;
}

.generated-data {
  margin-top: 20px;
}

.generated-data ul {
  margin: 10px 0;
  padding-left: 20px;
}

.generated-data li {
  margin: 5px 0;
}

/* AI测试样式 */
.test-result {
  margin-top: 20px;
}

.response-card {
  background: #f8f9fa;
  min-height: 100px;
}

.response-content {
  padding: 16px;
  line-height: 1.6;
  color: #303133;
  white-space: pre-wrap;
}

/* 流式测试样式 */
.streaming-content {
  position: relative;
  min-height: 150px;
  padding: 16px;
}

.streaming-text {
  line-height: 1.6;
  color: #303133;
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

.test-status {
  margin-bottom: 20px;
}

.response-section {
  margin-top: 20px;
}

.streaming-stats {
  margin-top: 20px;
}

/* 导航测试样式 */
.navigation-test-section {
  max-width: 1200px;
  margin: 0 auto;
}

.el-card {
  margin-bottom: 20px;
}

.el-space {
  width: 100%;
}
</style>
