<template>
  <div class="system-settings">
    <div class="page-header">
      <h1>系统设置</h1>
      <p class="page-description">管理系统配置和参数</p>
    </div>

    <el-row :gutter="24">
      <!-- 基本设置 -->
      <el-col :span="12">
        <el-card class="settings-card">
          <template #header>
            <span>基本设置</span>
          </template>
          
          <el-form :model="basicSettings" label-width="120px">
            <el-form-item label="系统名称">
              <el-input v-model="basicSettings.systemName" />
            </el-form-item>
            <el-form-item label="系统描述">
              <el-input 
                v-model="basicSettings.systemDescription" 
                type="textarea" 
                :rows="3"
              />
            </el-form-item>
            <el-form-item label="管理员邮箱">
              <el-input v-model="basicSettings.adminEmail" />
            </el-form-item>
            <el-form-item label="允许注册">
              <el-switch v-model="basicSettings.allowRegistration" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveBasicSettings" :loading="saving.basic">
                保存基本设置
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- AI 设置 -->
      <el-col :span="12">
        <el-card class="settings-card">
          <template #header>
            <span>AI 设置</span>
          </template>
          
          <el-form :model="aiSettings" label-width="120px">
            <el-form-item label="启用AI评估">
              <el-switch v-model="aiSettings.enableAI" />
            </el-form-item>
            <el-form-item label="AI模型">
              <el-select v-model="aiSettings.aiModel" style="width: 100%">
                <el-option label="GPT-3.5 Turbo" value="gpt-3.5-turbo" />
                <el-option label="GPT-4" value="gpt-4" />
                <el-option label="Claude-3" value="claude-3" />
              </el-select>
            </el-form-item>
            <el-form-item label="API密钥">
              <el-input 
                v-model="aiSettings.apiKey" 
                type="password" 
                show-password
                placeholder="请输入AI服务API密钥"
              />
            </el-form-item>
            <el-form-item label="请求超时(秒)">
              <el-input-number 
                v-model="aiSettings.requestTimeout" 
                :min="10" 
                :max="300"
              />
            </el-form-item>
            <el-form-item label="最大重试次数">
              <el-input-number 
                v-model="aiSettings.maxRetries" 
                :min="1" 
                :max="10"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveAISettings" :loading="saving.ai">
                保存AI设置
              </el-button>
              <el-button @click="testAIConnection" :loading="testing">
                测试连接
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="24" style="margin-top: 24px;">
      <!-- 评估设置 -->
      <el-col :span="12">
        <el-card class="settings-card">
          <template #header>
            <span>评估设置</span>
          </template>
          
          <el-form :model="evaluationSettings" label-width="120px">
            <el-form-item label="默认评分方式">
              <el-radio-group v-model="evaluationSettings.defaultGradingMethod">
                <el-radio label="AI_ONLY">仅AI评估</el-radio>
                <el-radio label="MANUAL_ONLY">仅人工评估</el-radio>
                <el-radio label="AI_FIRST">AI优先</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="AI评估阈值">
              <el-slider 
                v-model="evaluationSettings.aiConfidenceThreshold" 
                :min="0.5" 
                :max="1.0" 
                :step="0.1"
                show-stops
                show-input
              />
            </el-form-item>
            <el-form-item label="批量处理大小">
              <el-input-number 
                v-model="evaluationSettings.batchSize" 
                :min="1" 
                :max="100"
              />
            </el-form-item>
            <el-form-item label="启用人工复核">
              <el-switch v-model="evaluationSettings.enableManualReview" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveEvaluationSettings" :loading="saving.evaluation">
                保存评估设置
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 系统状态 -->
      <el-col :span="12">
        <el-card class="settings-card">
          <template #header>
            <div class="card-header">
              <span>系统状态</span>
              <el-button size="small" icon="Refresh" @click="loadSystemStatus">
                刷新
              </el-button>
            </div>
          </template>
          
          <div class="status-list">
            <div class="status-item">
              <span class="status-label">数据库连接:</span>
              <el-tag :type="systemStatus.database ? 'success' : 'danger'">
                {{ systemStatus.database ? '正常' : '异常' }}
              </el-tag>
            </div>
            <div class="status-item">
              <span class="status-label">AI服务:</span>
              <el-tag :type="systemStatus.aiService ? 'success' : 'danger'">
                {{ systemStatus.aiService ? '正常' : '异常' }}
              </el-tag>
            </div>
            <div class="status-item">
              <span class="status-label">系统版本:</span>
              <span class="status-value">{{ systemStatus.version || 'v1.0.0' }}</span>
            </div>
            <div class="status-item">
              <span class="status-label">运行时间:</span>
              <span class="status-value">{{ systemStatus.uptime || '0天' }}</span>
            </div>
            <div class="status-item">
              <span class="status-label">内存使用:</span>
              <span class="status-value">{{ systemStatus.memoryUsage || '0MB' }}</span>
            </div>
            <div class="status-item">
              <span class="status-label">活跃用户:</span>
              <span class="status-value">{{ systemStatus.activeUsers || 0 }}</span>
            </div>
          </div>

          <div class="system-actions">
            <el-button type="warning" @click="clearCache" :loading="clearing">
              清理缓存
            </el-button>
            <el-button type="info" @click="exportLogs">
              导出日志
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

// 模拟 API 调用
const mockApi = {
  async getSystemSettings() {
    return {
      data: {
        basic: {
          systemName: 'TeachHelper',
          systemDescription: '智能教学辅助系统',
          adminEmail: 'admin@teachhelper.com',
          allowRegistration: true
        },
        ai: {
          enableAI: true,
          aiModel: 'gpt-3.5-turbo',
          apiKey: '****',
          requestTimeout: 60,
          maxRetries: 3
        },
        evaluation: {
          defaultGradingMethod: 'AI_FIRST',
          aiConfidenceThreshold: 0.8,
          batchSize: 10,
          enableManualReview: true
        }
      }
    }
  },
  async saveSettings(type: string, data: any) {
    console.log(`Saving ${type} settings:`, data)
    return { data: 'success' }
  },
  async getSystemStatus() {
    return {
      data: {
        database: true,
        aiService: true,
        version: 'v1.0.0',
        uptime: '5天12小时',
        memoryUsage: '128MB',
        activeUsers: 25
      }
    }
  },
  async testAIConnection() {
    return { data: { success: true } }
  }
}

const saving = ref({
  basic: false,
  ai: false,
  evaluation: false
})
const testing = ref(false)
const clearing = ref(false)

const basicSettings = ref({
  systemName: '',
  systemDescription: '',
  adminEmail: '',
  allowRegistration: false
})

const aiSettings = ref({
  enableAI: false,
  aiModel: '',
  apiKey: '',
  requestTimeout: 60,
  maxRetries: 3
})

const evaluationSettings = ref({
  defaultGradingMethod: 'AI_FIRST',
  aiConfidenceThreshold: 0.8,
  batchSize: 10,
  enableManualReview: true
})

const systemStatus = ref({
  database: true,
  aiService: true,
  version: '',
  uptime: '',
  memoryUsage: '',
  activeUsers: 0
})

const loadSettings = async () => {
  try {
    const response = await mockApi.getSystemSettings()
    const settings = response.data
    
    basicSettings.value = settings.basic
    aiSettings.value = settings.ai
    evaluationSettings.value = settings.evaluation
  } catch (error) {
    console.error('加载设置失败:', error)
    ElMessage.error('加载设置失败')
  }
}

const loadSystemStatus = async () => {
  try {
    const response = await mockApi.getSystemStatus()
    systemStatus.value = response.data
  } catch (error) {
    console.error('加载系统状态失败:', error)
    ElMessage.error('加载系统状态失败')
  }
}

const saveBasicSettings = async () => {
  saving.value.basic = true
  try {
    await mockApi.saveSettings('basic', basicSettings.value)
    ElMessage.success('基本设置保存成功')
  } catch (error) {
    console.error('保存基本设置失败:', error)
    ElMessage.error('保存基本设置失败')
  } finally {
    saving.value.basic = false
  }
}

const saveAISettings = async () => {
  saving.value.ai = true
  try {
    await mockApi.saveSettings('ai', aiSettings.value)
    ElMessage.success('AI设置保存成功')
  } catch (error) {
    console.error('保存AI设置失败:', error)
    ElMessage.error('保存AI设置失败')
  } finally {
    saving.value.ai = false
  }
}

const saveEvaluationSettings = async () => {
  saving.value.evaluation = true
  try {
    await mockApi.saveSettings('evaluation', evaluationSettings.value)
    ElMessage.success('评估设置保存成功')
  } catch (error) {
    console.error('保存评估设置失败:', error)
    ElMessage.error('保存评估设置失败')
  } finally {
    saving.value.evaluation = false
  }
}

const testAIConnection = async () => {
  testing.value = true
  try {
    const response = await mockApi.testAIConnection()
    if (response.data.success) {
      ElMessage.success('AI服务连接正常')
    } else {
      ElMessage.error('AI服务连接失败')
    }
  } catch (error) {
    console.error('测试AI连接失败:', error)
    ElMessage.error('测试AI连接失败')
  } finally {
    testing.value = false
  }
}

const clearCache = async () => {
  clearing.value = true
  try {
    // 模拟清理缓存
    await new Promise(resolve => setTimeout(resolve, 2000))
    ElMessage.success('缓存清理完成')
  } catch (error) {
    console.error('清理缓存失败:', error)
    ElMessage.error('清理缓存失败')
  } finally {
    clearing.value = false
  }
}

const exportLogs = () => {
  ElMessage.info('日志导出功能开发中...')
}

onMounted(() => {
  loadSettings()
  loadSystemStatus()
})
</script>

<style scoped>
.system-settings {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
}

.page-description {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.settings-card {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-list {
  margin-bottom: 24px;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.status-item:last-child {
  border-bottom: none;
}

.status-label {
  font-weight: 500;
  color: #666;
}

.status-value {
  color: #303133;
}

.system-actions {
  display: flex;
  gap: 12px;
}
</style>
