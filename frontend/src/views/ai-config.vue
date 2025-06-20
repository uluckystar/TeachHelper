<template>
  <div class="ai-config-center">
    <el-card>
      <template #header>
        <div class="page-header">
          <h2>AI配置中心</h2>
          <el-button 
            type="primary" 
            @click="showCreateDialog = true"
            :icon="Plus"
          >
            添加配置
          </el-button>
        </div>
      </template>

      <!-- 标签页 -->
      <el-tabs v-model="activeTab" @tab-click="handleTabChange">
        <!-- 配置管理 -->
        <el-tab-pane label="配置管理" name="management">
          <div v-if="loading" style="text-align: center; padding: 20px;">
            <el-icon :size="30"><Loading /></el-icon>
            <p>加载中...</p>
          </div>

          <div v-else-if="configs.length === 0" class="empty-state">
            <el-empty description="暂无AI配置">
              <el-button type="primary" @click="showCreateDialog = true">
                创建第一个配置
              </el-button>
            </el-empty>
          </div>

          <div v-else class="configs-list">
            <el-row :gutter="20">
              <el-col 
                v-for="config in configs" 
                :key="config.id" 
                :xs="24" 
                :sm="12" 
                :md="8" 
                :lg="6"
              >
                <el-card class="config-card" shadow="hover">
                  <template #header>
                    <div class="config-header">
                      <div class="provider-info">
                        <el-tag 
                          :type="getProviderTagType(config.provider)"
                          size="small"
                        >
                          {{ config.providerDisplayName || getProviderName(config.provider) }}
                        </el-tag>
                        <el-switch
                          v-model="config.isActive"
                          @change="toggleConfig(config)"
                          :loading="config.toggleLoading"
                          size="small"
                        />
                      </div>
                    </div>
                  </template>

                  <div class="config-content">
                    <p class="model-name">
                      <el-icon><Cpu /></el-icon>
                      {{ config.modelName || '未设置' }}
                    </p>
                    
                    <p class="api-key" v-if="config.maskedApiKey">
                      <el-icon><Key /></el-icon>
                      <span class="key-text">{{ config.maskedApiKey }}</span>
                    </p>

                    <!-- 使用统计 -->
                    <div v-if="config.usageStats" class="usage-stats">
                      <el-row :gutter="10">
                        <el-col :span="12">
                          <div class="stat-item">
                            <span class="stat-label">请求次数</span>
                            <span class="stat-value">{{ config.usageStats.totalRequests }}</span>
                          </div>
                        </el-col>
                        <el-col :span="12">
                          <div class="stat-item">
                            <span class="stat-label">总消耗</span>
                            <span class="stat-value">¥{{ config.usageStats.totalCost.toFixed(4) }}</span>
                          </div>
                        </el-col>
                      </el-row>
                    </div>

                    <!-- 操作按钮 -->
                    <div class="config-actions">
                      <el-button
                        size="small"
                        @click="quickTestConfig(config)"
                        :loading="config.testLoading"
                      >
                        快速测试
                      </el-button>
                      <el-button
                        size="small"
                        @click="editConfig(config)"
                      >
                        编辑
                      </el-button>
                      <el-button
                        size="small"
                        type="danger"
                        @click="deleteConfig(config)"
                      >
                        删除
                      </el-button>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>

        <!-- 数据仪表板 -->
        <el-tab-pane label="数据仪表板" name="dashboard">
          <div v-if="loading" style="text-align: center; padding: 20px;">
            <el-icon :size="30"><Loading /></el-icon>
            <p>加载中...</p>
          </div>

          <div v-else>
            <!-- 统计概览 -->
            <el-row :gutter="20" class="stats-overview">
              <el-col :xs="24" :sm="12" :md="6">
                <el-card class="stat-card">
                  <el-statistic
                    title="总配置数"
                    :value="configs.length"
                    suffix="个"
                  >
                    <template #prefix>
                      <el-icon style="vertical-align: -0.125em">
                        <Setting />
                      </el-icon>
                    </template>
                  </el-statistic>
                </el-card>
              </el-col>
              <el-col :xs="24" :sm="12" :md="6">
                <el-card class="stat-card">
                  <el-statistic
                    title="激活配置"
                    :value="activeConfigs.length"
                    suffix="个"
                  >
                    <template #prefix>
                      <el-icon style="vertical-align: -0.125em" color="#67C23A">
                        <CircleCheck />
                      </el-icon>
                    </template>
                  </el-statistic>
                </el-card>
              </el-col>
              <el-col :xs="24" :sm="12" :md="6">
                <el-card class="stat-card">
                  <el-statistic
                    title="总请求数"
                    :value="totalRequests"
                    suffix="次"
                  >
                    <template #prefix>
                      <el-icon style="vertical-align: -0.125em" color="#409EFF">
                        <DataLine />
                      </el-icon>
                    </template>
                  </el-statistic>
                </el-card>
              </el-col>
              <el-col :xs="24" :sm="12" :md="6">
                <el-card class="stat-card">
                  <el-statistic
                    title="总费用"
                    :value="totalCost"
                    :precision="4"
                    prefix="¥"
                  >
                    <template #prefix>
                      <el-icon style="vertical-align: -0.125em" color="#F56C6C">
                        <Money />
                      </el-icon>
                    </template>
                  </el-statistic>
                </el-card>
              </el-col>
            </el-row>

            <!-- 详细数据表格 -->
            <el-card class="table-card">
              <template #header>
                <div class="card-header">
                  <h3>配置详细信息</h3>
                  <div class="table-actions">
                    <el-button 
                      size="small" 
                      @click="refreshStats"
                      :loading="statsLoading"
                      :icon="Refresh"
                    >
                      刷新统计
                    </el-button>
                  </div>
                </div>
              </template>

              <el-table :data="configs" stripe>
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column label="提供商" width="120">
                  <template #default="{ row }">
                    <el-tag :type="getProviderTagType(row.provider)" size="small">
                      {{ row.providerDisplayName || getProviderName(row.provider) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="modelName" label="模型" />
                <el-table-column label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="row.isActive ? 'success' : 'info'" size="small">
                      {{ row.isActive ? '激活' : '停用' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="默认" width="80">
                  <template #default="{ row }">
                    <el-icon v-if="row.isDefault" color="#409EFF"><Star /></el-icon>
                  </template>
                </el-table-column>
                <el-table-column label="请求次数" width="100">
                  <template #default="{ row }">
                    {{ row.usageStats?.totalRequests || 0 }}
                  </template>
                </el-table-column>
                <el-table-column label="总费用" width="120">
                  <template #default="{ row }">
                    ¥{{ (row.usageStats?.totalCost || 0).toFixed(4) }}
                  </template>
                </el-table-column>
                <el-table-column prop="createdAt" label="创建时间" width="180">
                  <template #default="{ row }">
                    {{ formatDateTime(row.createdAt) }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="200" fixed="right">
                  <template #default="{ row }">
                    <el-button-group size="small">
                      <el-button @click="quickTestConfig(row)" :loading="row.testLoading">
                        测试
                      </el-button>
                      <el-button @click="editConfig(row)">编辑</el-button>
                      <el-button 
                        type="danger" 
                        @click="deleteConfig(row)"
                      >
                        删除
                      </el-button>
                    </el-button-group>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>
        </el-tab-pane>

        <!-- 测试中心 -->
        <el-tab-pane label="测试中心" name="testing">
          <el-row :gutter="20">
            <!-- 配置选择 -->
            <el-col :span="24">
              <el-card>
                <template #header>
                  <h3>AI配置测试</h3>
                </template>
                
                <el-form label-width="120px">
                  <el-form-item label="选择配置">
                    <el-select v-model="selectedConfigId" placeholder="请选择要测试的配置">
                      <el-option
                        v-for="config in activeConfigs"
                        :key="config.id"
                        :label="`${getProviderName(config.provider)} - ${config.modelName || '未设置'}`"
                        :value="config.id"
                      />
                    </el-select>
                  </el-form-item>
                  
                  <el-form-item label="测试类型">
                    <el-radio-group v-model="testType">
                      <el-radio label="quick">快速测试</el-radio>
                      <el-radio label="streaming">流式测试</el-radio>
                    </el-radio-group>
                  </el-form-item>
                </el-form>

                <!-- 快速测试 -->
                <div v-if="testType === 'quick'">
                  <el-divider content-position="left">快速测试</el-divider>
                  <el-form :model="quickTestForm" label-width="120px">
                    <el-form-item label="测试提示">
                      <el-input
                        v-model="quickTestForm.prompt"
                        type="textarea"
                        :rows="4"
                        placeholder="请输入测试提示..."
                      />
                    </el-form-item>
                    <el-form-item label="最大Token数">
                      <el-input-number
                        v-model="quickTestForm.maxTokens"
                        :min="10"
                        :max="2000"
                        :step="10"
                      />
                    </el-form-item>
                    <el-form-item>
                      <el-button
                        type="primary"
                        @click="executeQuickTest"
                        :loading="quickTesting"
                        :disabled="!selectedConfigId"
                      >
                        开始测试
                      </el-button>
                    </el-form-item>
                  </el-form>

                  <!-- 快速测试结果 -->
                  <div v-if="quickTestResult" class="test-result">
                    <el-divider content-position="left">测试结果</el-divider>
                    <el-alert
                      :type="quickTestResult.success ? 'success' : 'error'"
                      :title="quickTestResult.success ? '测试成功' : '测试失败'"
                      :description="quickTestResult.success ? '配置工作正常' : quickTestResult.error"
                      show-icon
                      style="margin-bottom: 20px"
                    />
                    
                    <div v-if="quickTestResult.success">
                      <el-card class="response-card">
                        <div class="response-content">
                          {{ quickTestResult.response }}
                        </div>
                      </el-card>
                      
                      <el-descriptions title="测试统计" :column="3" border style="margin-top: 20px">
                        <el-descriptions-item label="响应时间">
                          {{ quickTestResult.latencyMs }}ms
                        </el-descriptions-item>
                        <el-descriptions-item label="输入Token">
                          {{ quickTestResult.inputTokens || 0 }}
                        </el-descriptions-item>
                        <el-descriptions-item label="输出Token">
                          {{ quickTestResult.outputTokens || 0 }}
                        </el-descriptions-item>
                        <el-descriptions-item label="总Token">
                          {{ quickTestResult.tokensUsed || 0 }}
                        </el-descriptions-item>
                        <el-descriptions-item label="预估费用" :span="2">
                          ¥{{ quickTestResult.cost.toFixed(6) }}
                        </el-descriptions-item>
                      </el-descriptions>
                    </div>
                  </div>
                </div>

                <!-- 流式测试 -->
                <div v-if="testType === 'streaming'">
                  <el-divider content-position="left">流式测试</el-divider>
                  <el-form :model="streamingTestForm" label-width="120px">
                    <el-form-item label="测试提示">
                      <el-input
                        v-model="streamingTestForm.prompt"
                        type="textarea"
                        :rows="4"
                        placeholder="请输入测试提示..."
                        :disabled="streamingTesting"
                      />
                    </el-form-item>
                    <el-form-item label="最大Token数">
                      <el-input-number
                        v-model="streamingTestForm.maxTokens"
                        :min="10"
                        :max="2000"
                        :step="10"
                        :disabled="streamingTesting"
                      />
                    </el-form-item>
                    <el-form-item>
                      <el-button
                        type="primary"
                        @click="startStreamTest"
                        :loading="streamingTesting"
                        :disabled="!selectedConfigId || streamingTesting"
                      >
                        {{ streamingTesting ? '测试中...' : '开始流式测试' }}
                      </el-button>
                      <el-button @click="stopStreamTest" :disabled="!streamingTesting">
                        停止测试
                      </el-button>
                      <el-button @click="clearStreamResult">
                        清空结果
                      </el-button>
                    </el-form-item>
                  </el-form>

                  <!-- 流式测试状态 -->
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

                  <!-- 流式响应内容 -->
                  <div v-if="streamingContent || streamingResult" class="streaming-section">
                    <el-divider content-position="left">AI响应内容</el-divider>
                    
                    <el-card class="response-card">
                      <div class="streaming-content">
                        <div v-if="streamingContent" class="markdown-content" v-html="renderedStreamingContent"></div>
                        <span v-if="streamingTesting" class="typing-indicator">▋</span>
                      </div>
                    </el-card>
                    
                    <!-- 流式测试结果统计 -->
                    <div v-if="streamingResult" class="test-result-stats" style="margin-top: 20px">
                      <el-descriptions title="流式测试统计" :column="3" border>
                        <el-descriptions-item label="提供商">
                          <el-tag :type="getProviderTagType(streamingResult.provider)">
                            {{ streamingResult.provider }}
                          </el-tag>
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
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 创建/编辑配置对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="editingConfig ? '编辑AI配置' : '创建AI配置'"
      width="60%"
      :close-on-click-modal="false"
    >
      <el-form
        :model="configForm"
        :rules="configRules"
        ref="configFormRef"
        label-width="120px"
      >
        <el-form-item label="配置名称" prop="name">
          <el-input
            v-model="configForm.name"
            placeholder="请输入配置名称"
          />
        </el-form-item>

        <el-form-item label="AI提供商" prop="provider">
          <el-select v-model="configForm.provider" placeholder="请选择AI提供商">
            <el-option
              v-for="provider in availableProviders"
              :key="provider.provider"
              :label="provider.name"
              :value="provider.provider"
              :disabled="!provider.enabled"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="API密钥" prop="apiKey">
          <el-input
            v-model="configForm.apiKey"
            type="password"
            placeholder="请输入API密钥"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="API端点" prop="apiUrl">
          <el-input
            v-model="configForm.apiUrl"
            placeholder="API端点URL（可选）"
          />
        </el-form-item>
        
        <el-form-item label="模型名称" prop="model">
          <el-input
            v-model="configForm.model"
            placeholder="模型名称"
          />
        </el-form-item>
        
        <el-form-item label="最大Token数" prop="maxTokens">
          <el-input-number
            v-model="configForm.maxTokens"
            :min="1"
            :max="8000"
            :step="100"
          />
        </el-form-item>
        
        <el-form-item label="温度参数" prop="temperature">
          <el-input-number
            v-model="configForm.temperature"
            :min="0"
            :max="2"
            :step="0.1"
            :precision="1"
          />
        </el-form-item>
        
        <el-form-item label="设为默认">
          <el-switch v-model="configForm.isDefault" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="cancelConfigEdit">取消</el-button>
        <el-button type="primary" @click="saveConfig" :loading="configSaving">
          {{ editingConfig ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Plus, Loading, Cpu, Key, Setting, CircleCheck, DataLine, Money, 
  Refresh, Star 
} from '@element-plus/icons-vue'
import { marked } from 'marked'
import { userAIConfigApi } from '../api/userAIConfig'
import type { 
  UserAIConfigResponse, 
  AIProvider,
  UserAIConfigRequest,
  AIConfigTestRequest,
  AIConfigTestResponse,
  AIProviderInfo
} from '../types/api'

// 响应式数据
const loading = ref(true)
const configs = ref<(UserAIConfigResponse & { toggleLoading?: boolean; testLoading?: boolean })[]>([])
const activeTab = ref('management')
const statsLoading = ref(false)

// 配置创建/编辑
const showCreateDialog = ref(false)
const editingConfig = ref<UserAIConfigResponse | null>(null)
const configSaving = ref(false)
const configFormRef = ref()
const availableProviders = ref<AIProviderInfo[]>([])

// 测试相关
const selectedConfigId = ref<number>()
const testType = ref('quick')
const quickTesting = ref(false)
const quickTestResult = ref<AIConfigTestResponse | null>(null)

// 流式测试
const streamingTesting = ref(false)
const streamingContent = ref('')
const streamingResult = ref<any>(null)
const streamingStatus = ref<any>(null)
const streamingReader = ref<ReadableStreamDefaultReader<Uint8Array> | null>(null)

// 表单数据
const configForm = reactive({
  provider: '' as AIProvider,
  apiKey: '',
  apiUrl: '',
  model: '',
  maxTokens: 1000,
  temperature: 0.7,
  isDefault: false,
  name: ''
})

const quickTestForm = reactive({
  prompt: '请简单介绍一下人工智能的定义。',
  maxTokens: 500
})

const streamingTestForm = reactive({
  prompt: '请简单介绍一下人工智能的定义和应用领域。',
  maxTokens: 500
})

// 表单验证规则
const configRules = {
  provider: [{ required: true, message: '请选择AI提供商', trigger: 'change' }],
  apiKey: [{ required: true, message: '请输入API密钥', trigger: 'blur' }],
  model: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  maxTokens: [{ required: true, message: '请输入最大Token数', trigger: 'blur' }],
  temperature: [{ required: true, message: '请输入温度参数', trigger: 'blur' }],
  name: [{ required: true, message: '请输入配置名称', trigger: 'blur' }]
}

// 计算属性
const activeConfigs = computed(() => configs.value.filter(config => config.isActive))

const totalRequests = computed(() => {
  return configs.value.reduce((total, config) => {
    return total + (config.usageStats?.totalRequests || 0)
  }, 0)
})

const totalCost = computed(() => {
  return configs.value.reduce((total, config) => {
    return total + (config.usageStats?.totalCost || 0)
  }, 0)
})

const renderedStreamingContent = computed(() => {
  if (!streamingContent.value) return ''
  try {
    return marked(streamingContent.value)
  } catch (error) {
    console.error('Markdown parsing error:', error)
    return streamingContent.value.replace(/\n/g, '<br>')
  }
})

// 生命周期
onMounted(async () => {
  await Promise.all([
    loadConfigs(),
    loadProviders()
  ])
  loading.value = false
})

// 方法
const loadConfigs = async () => {
  try {
    configs.value = await userAIConfigApi.getConfigs()
  } catch (error) {
    console.error('加载AI配置失败:', error)
    ElMessage.error('加载AI配置失败')
  }
}

const loadProviders = async () => {
  try {
    availableProviders.value = await userAIConfigApi.getProviders()
  } catch (error) {
    console.error('加载提供商信息失败:', error)
  }
}

const refreshStats = async () => {
  statsLoading.value = true
  try {
    await loadConfigs()
    ElMessage.success('统计数据已刷新')
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    statsLoading.value = false
  }
}

const handleTabChange = (tab: any) => {
  if (tab.paneName === 'testing' && configs.value.length > 0 && activeConfigs.value.length > 0) {
    selectedConfigId.value = activeConfigs.value[0].id
  }
}

// 配置管理方法
const toggleConfig = async (config: UserAIConfigResponse & { toggleLoading?: boolean }) => {
  config.toggleLoading = true
  try {
    await userAIConfigApi.toggleConfig(config.id, config.isActive)
    ElMessage.success(config.isActive ? '配置已激活' : '配置已停用')
  } catch (error) {
    console.error('切换配置状态失败:', error)
    ElMessage.error('操作失败')
    config.isActive = !config.isActive
  } finally {
    config.toggleLoading = false
  }
}

const quickTestConfig = async (config: UserAIConfigResponse & { testLoading?: boolean }) => {
  config.testLoading = true
  try {
    const testRequest: AIConfigTestRequest = {
      prompt: '这是一个测试消息，请简单回复。',
      maxTokens: 100
    }
    const result = await userAIConfigApi.testConfig(config.id, testRequest)
    
    if (result.success) {
      ElMessage.success('配置测试成功')
    } else {
      ElMessage.error(`配置测试失败: ${result.error}`)
    }
  } catch (error) {
    console.error('测试配置失败:', error)
    ElMessage.error('测试失败')
  } finally {
    config.testLoading = false
  }
}

const editConfig = (config: UserAIConfigResponse) => {
  editingConfig.value = config
  configForm.provider = config.provider
  configForm.apiKey = ''  // 不显示原始密钥
  configForm.apiUrl = config.apiEndpoint || ''
  configForm.model = config.modelName || ''
  configForm.maxTokens = config.maxTokens
  configForm.temperature = config.temperature
  configForm.isDefault = config.isDefault
  configForm.name = config.name || ''
  showCreateDialog.value = true
}

const deleteConfig = async (config: UserAIConfigResponse) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除配置 "${config.providerDisplayName || getProviderName(config.provider)} - ${config.modelName || '未设置'}" 吗？`,
      '确认删除',
      { type: 'warning' }
    )
    
    await userAIConfigApi.deleteConfig(config.id)
    await loadConfigs()
    ElMessage.success('配置已删除')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除配置失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const saveConfig = async () => {
  if (!configFormRef.value) return
  
  try {
    await configFormRef.value.validate()
    configSaving.value = true
    
    const request: UserAIConfigRequest = {
      provider: configForm.provider,
      apiKey: configForm.apiKey,
      apiUrl: configForm.apiUrl,
      model: configForm.model,
      maxTokens: configForm.maxTokens,
      temperature: configForm.temperature,
      name: configForm.name
    }
    
    if (editingConfig.value) {
      await userAIConfigApi.updateConfig(editingConfig.value.id, request)
      ElMessage.success('配置已更新')
    } else {
      await userAIConfigApi.createConfig(request)
      ElMessage.success('配置已创建')
    }
    
    await loadConfigs()
    cancelConfigEdit()
  } catch (error) {
    console.error('保存配置失败:', error)
    ElMessage.error('保存失败')
  } finally {
    configSaving.value = false
  }
}

const cancelConfigEdit = () => {
  showCreateDialog.value = false
  editingConfig.value = null
  configFormRef.value?.resetFields()
}

// 快速测试方法
const executeQuickTest = async () => {
  if (!selectedConfigId.value) {
    ElMessage.error('请选择要测试的配置')
    return
  }
  
  quickTesting.value = true
  quickTestResult.value = null
  
  try {
    const testRequest: AIConfigTestRequest = {
      prompt: quickTestForm.prompt,
      maxTokens: quickTestForm.maxTokens
    }
    
    const result = await userAIConfigApi.testConfig(selectedConfigId.value, testRequest)
    quickTestResult.value = result
    
    if (result.success) {
      ElMessage.success('快速测试完成')
    } else {
      ElMessage.error('快速测试失败')
    }
  } catch (error) {
    console.error('快速测试失败:', error)
    ElMessage.error('快速测试失败')
    quickTestResult.value = {
      success: false,
      error: '网络错误或服务不可用',
      latencyMs: 0,
      tokensUsed: 0,
      cost: 0
    }
  } finally {
    quickTesting.value = false
  }
}

// 流式测试方法
const startStreamTest = async () => {
  if (!selectedConfigId.value) {
    ElMessage.error('请选择要测试的配置')
    return
  }
  
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
        configId: selectedConfigId.value,
        prompt: streamingTestForm.prompt,
        maxTokens: streamingTestForm.maxTokens
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

const stopStreamTest = () => {
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

const clearStreamResult = () => {
  streamingContent.value = ''
  streamingResult.value = null
  streamingStatus.value = null
}

// 工具方法
const getProviderTagType = (provider: AIProvider): 'success' | 'primary' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<AIProvider, 'success' | 'primary' | 'warning' | 'info' | 'danger'> = {
    'OPENAI': 'success',
    'DEEPSEEK': 'primary',
    'CLAUDE': 'warning',
    'ALIBABA_TONGYI': 'info',
    'BAIDU_ERNIE': 'danger',
    'TENCENT_HUNYUAN': 'success',
    'CUSTOM': 'info'
  }
  return typeMap[provider] || 'info'
}

const getProviderName = (provider: AIProvider) => {
  const nameMap: Record<AIProvider, string> = {
    'OPENAI': 'OpenAI',
    'DEEPSEEK': 'DeepSeek',
    'CLAUDE': 'Claude',
    'ALIBABA_TONGYI': '阿里通义',
    'BAIDU_ERNIE': '百度文心',
    'TENCENT_HUNYUAN': '腾讯混元',
    'CUSTOM': '自定义'
  }
  return nameMap[provider] || provider
}

const formatDateTime = (dateTime: string) => {
  return new Date(dateTime).toLocaleString('zh-CN')
}
</script>

<style scoped>
.ai-config-center {
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

/* 配置管理样式 */
.configs-list {
  margin-top: 20px;
}

.config-card {
  margin-bottom: 20px;
  transition: transform 0.2s;
}

.config-card:hover {
  transform: translateY(-2px);
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.provider-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.config-content {
  padding: 10px 0;
}

.model-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold;
  margin-bottom: 10px;
}

.api-key {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
}

.key-text {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
}

.usage-stats {
  background: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 15px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.stat-label {
  color: #909399;
}

.stat-value {
  font-weight: bold;
  color: #303133;
}

.config-actions {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
}

/* 仪表板样式 */
.stats-overview {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.table-card .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-card h3 {
  margin: 0;
}

.table-actions {
  display: flex;
  gap: 10px;
}

/* 测试中心样式 */
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

.test-status {
  margin-bottom: 20px;
}

.streaming-section {
  margin-top: 20px;
}

.test-result-stats {
  margin-top: 20px;
}
</style>
