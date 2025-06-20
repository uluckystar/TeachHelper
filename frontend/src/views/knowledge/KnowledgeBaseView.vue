<template>
  <div class="knowledge-base-view">
    <div class="page-header">
      <h1>数据概览</h1>
      <p class="page-description">智能教学助手数据分析与统计报告</p>
    </div>

    <!-- 数据统计卡片 -->
    <el-row :gutter="24" class="stats-section">
      <el-col :span="6">
        <el-card class="stat-card">
          <el-statistic
            title="知识库总数"
            :value="stats.knowledgeBaseCount"
            :precision="0"
          >
            <template #suffix>
              <el-icon class="stat-icon"><Collection /></el-icon>
            </template>
          </el-statistic>
          <div class="stat-trend">
            <span class="trend-text">较上周</span>
            <span class="trend-value positive">+12%</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card">
          <el-statistic
            title="文档总数"
            :value="stats.documentCount"
            :precision="0"
          >
            <template #suffix>
              <el-icon class="stat-icon"><Document /></el-icon>
            </template>
          </el-statistic>
          <div class="stat-trend">
            <span class="trend-text">较上周</span>
            <span class="trend-value positive">+8%</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card">
          <el-statistic
            title="AI生成试卷"
            :value="stats.generatedPaperCount"
            :precision="0"
          >
            <template #suffix>
              <el-icon class="stat-icon"><Tickets /></el-icon>
            </template>
          </el-statistic>
          <div class="stat-trend">
            <span class="trend-text">较上周</span>
            <span class="trend-value positive">+15%</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card">
          <el-statistic
            title="存储空间"
            :value="stats.totalSize"
            :precision="1"
            suffix="MB"
          >
            <template #suffix>
              <el-icon class="stat-icon"><Coin /></el-icon>
            </template>
          </el-statistic>
          <div class="stat-trend">
            <span class="trend-text">较上周</span>
            <span class="trend-value positive">+5%</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 数据图表区域 -->
    <el-row :gutter="24" class="charts-section">
      <!-- 使用趋势图表 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>使用趋势分析</span>
              <el-radio-group v-model="trendPeriod" size="small">
                <el-radio-button label="7days">近7天</el-radio-button>
                <el-radio-button label="30days">近30天</el-radio-button>
                <el-radio-button label="90days">近90天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div class="chart-container">
            <div class="chart-placeholder">
              <el-icon><TrendCharts /></el-icon>
              <p>使用趋势图表</p>
              <el-button type="text" @click="loadTrendData">加载数据</el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 学科分布图表 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>学科分布统计</span>
              <el-dropdown @command="handleChartAction">
                <el-button text>
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="export">导出图表</el-dropdown-item>
                    <el-dropdown-item command="fullscreen">全屏查看</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
          <div class="chart-container">
            <div class="chart-placeholder">
              <el-icon><PieChart /></el-icon>
              <p>学科分布饼图</p>
              <el-button type="text" @click="loadSubjectData">加载数据</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- AI使用统计 -->
    <el-row :gutter="24" class="ai-stats-section">
      <el-col :span="24">
        <el-card class="ai-usage-card">
          <template #header>
            <div class="card-header">
              <span>AI功能使用统计</span>
              <div class="header-actions">
                <el-select v-model="aiStatsPeriod" size="small" style="width: 120px;">
                  <el-option label="今日" value="today" />
                  <el-option label="本周" value="week" />
                  <el-option label="本月" value="month" />
                </el-select>
                <el-button size="small" @click="refreshAIStats">
                  <el-icon><Refresh /></el-icon>
                </el-button>
              </div>
            </div>
          </template>
          
          <el-row :gutter="16">
            <el-col :span="6">
              <div class="ai-stat-item">
                <div class="ai-stat-icon">
                  <el-icon color="#409EFF"><ChatRound /></el-icon>
                </div>
                <div class="ai-stat-content">
                  <div class="ai-stat-value">{{ aiStats.totalCalls || 0 }}</div>
                  <div class="ai-stat-label">AI调用次数</div>
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="ai-stat-item">
                <div class="ai-stat-icon">
                  <el-icon color="#67C23A"><Cpu /></el-icon>
                </div>
                <div class="ai-stat-content">
                  <div class="ai-stat-value">{{ formatNumber(aiStats.totalTokens) || 0 }}</div>
                  <div class="ai-stat-label">消耗Token</div>
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="ai-stat-item">
                <div class="ai-stat-icon">
                  <el-icon color="#E6A23C"><Money /></el-icon>
                </div>
                <div class="ai-stat-content">
                  <div class="ai-stat-value">¥{{ formatCurrency(aiStats.totalCost) }}</div>
                  <div class="ai-stat-label">总费用</div>
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="ai-stat-item">
                <div class="ai-stat-icon">
                  <el-icon color="#F56C6C"><Timer /></el-icon>
                </div>
                <div class="ai-stat-content">
                  <div class="ai-stat-value">{{ formatDuration(aiStats.avgResponseTime) }}</div>
                  <div class="ai-stat-label">平均响应时间</div>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近活动 -->
    <el-card class="recent-activity">
      <template #header>
        <div class="card-header">
          <span>
            最近活动
            <el-tag v-if="authStore.isAdmin" type="info" size="small" style="margin-left: 8px;">
              管理员视图
            </el-tag>
            <el-tag v-else-if="authStore.isTeacher" type="success" size="small" style="margin-left: 8px;">
              教师视图
            </el-tag>
            <el-tag v-else type="primary" size="small" style="margin-left: 8px;">
              学生视图
            </el-tag>
          </span>
          <div class="header-actions">
            <el-select v-model="activityFilter" size="small" style="width: 120px; margin-right: 8px;">
              <el-option label="全部活动" value="all" />
              <el-option label="文档上传" value="upload" />
              <el-option label="试卷生成" value="generate" />
              <el-option label="AI评估" value="evaluation" />
              <el-option label="知识库操作" value="knowledge" />
            </el-select>
            <el-button link @click="refreshActivity">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="3" animated />
      </div>

      <div v-else-if="activities.length === 0" class="empty-state">
        <el-empty description="暂无活动记录" />
      </div>

      <div v-else class="activity-list">
        <!-- 简化显示模式 (当活动过多时) -->
        <div v-if="activities.length > 20 && !showDetailedActivities" class="activity-summary">
          <div class="summary-stats">
            <el-row :gutter="16">
              <el-col :span="6">
                <div class="summary-item">
                  <div class="summary-number">{{ activitySummary.upload }}</div>
                  <div class="summary-label">文档上传</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="summary-item">
                  <div class="summary-number">{{ activitySummary.generate }}</div>
                  <div class="summary-label">试卷生成</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="summary-item">
                  <div class="summary-number">{{ activitySummary.evaluation }}</div>
                  <div class="summary-label">AI评估</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="summary-item">
                  <div class="summary-number">{{ activitySummary.knowledge }}</div>
                  <div class="summary-label">知识库操作</div>
                </div>
              </el-col>
            </el-row>
          </div>
          <div class="summary-actions">
            <el-button type="text" @click="showDetailedActivities = true">
              查看详细活动列表
            </el-button>
          </div>
        </div>

        <!-- 详细活动列表 -->
        <el-timeline v-else>
          <el-timeline-item
            v-for="activity in paginatedActivities"
            :key="activity.id"
            :timestamp="formatTime(activity.createdAt)"
            :type="getActivityType(activity.type)"
            :size="activity.important ? 'large' : 'normal'"
          >
            <div class="activity-content">
              <div class="activity-header">
                <h4>{{ activity.title }}</h4>
                <div class="activity-meta">
                  <el-tag v-if="activity.tag" :type="getTagType(activity.type)" size="small">
                    {{ activity.tag }}
                  </el-tag>
                  <span v-if="authStore.isAdmin && activity.userId !== authStore.user?.id" class="user-info">
                    by {{ activity.userName || '未知用户' }}
                  </span>
                </div>
              </div>
              <p class="activity-description">{{ activity.description }}</p>
              <div v-if="activity.details" class="activity-details">
                <el-button type="text" size="small" @click="showActivityDetail(activity)">
                  查看详情
                </el-button>
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>

        <!-- 分页控制 -->
        <div v-if="showDetailedActivities && totalActivities > pageSize" class="activity-pagination">
          <el-pagination
            v-model:current-page="currentActivityPage"
            :page-size="pageSize"
            :total="totalActivities"
            layout="prev, pager, next"
            @current-change="handleActivityPageChange"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { taskApi } from '@/api/task'
import {
  Upload,
  Collection,
  Document,
  Tickets,
  Coin,
  Refresh,
  Search,
  TrendCharts,
  PieChart,
  MoreFilled,
  ChatRound,
  Cpu,
  Money,
  Timer
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

// 响应式数据
const loading = ref(false)
const stats = ref({
  knowledgeBaseCount: 0,
  documentCount: 0,
  generatedPaperCount: 0,
  totalSize: 0
})

// 新增的响应式数据
const trendPeriod = ref('7days')
const aiStatsPeriod = ref('today')
const activityFilter = ref('all')
const showDetailedActivities = ref(false)
const currentActivityPage = ref(1)
const pageSize = ref(10)

const activities = ref<any[]>([])
const aiStats = ref({
  totalCalls: 0,
  totalTokens: 0,
  totalCost: 0,
  avgResponseTime: 0
})

// 计算属性
const activitySummary = computed(() => {
  const summary = {
    upload: 0,
    generate: 0,
    evaluation: 0,
    knowledge: 0
  }
  
  activities.value.forEach(activity => {
    switch (activity.type) {
      case 'upload':
        summary.upload++
        break
      case 'generate':
        summary.generate++
        break
      case 'evaluation':
        summary.evaluation++
        break
      case 'knowledge':
        summary.knowledge++
        break
    }
  })
  
  return summary
})

const totalActivities = computed(() => activities.value.length)

const paginatedActivities = computed(() => {
  const start = (currentActivityPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return activities.value.slice(start, end)
})

// 导航方法
const goToUpload = () => {
  router.push('/knowledge/upload')
}

const goToKnowledgePoints = () => {
  ElMessage.info('知识点管理功能开发中...')
}

const goToDocumentSearch = () => {
  ElMessage.info('智能搜索功能开发中...')
}

// 图表相关方法
const loadTrendData = () => {
  ElMessage.info('正在加载趋势数据...')
}

const loadSubjectData = () => {
  ElMessage.info('正在加载学科分布数据...')
}

const handleChartAction = (command: string) => {
  switch (command) {
    case 'export':
      ElMessage.info('导出图表功能开发中...')
      break
    case 'fullscreen':
      ElMessage.info('全屏查看功能开发中...')
      break
  }
}

const refreshAIStats = async () => {
  // 这里可以调用实际的API来获取AI统计数据
  // await userAIConfigApi.getUserStats()
  ElMessage.success('AI统计数据已刷新')
}

// 活动相关方法
const refreshActivity = async () => {
  loading.value = true
  try {
    let activitiesData: any[] = []
    
    if (authStore.isAdmin) {
      // 管理员看到所有用户的活动
      const recentTasks = await taskApi.getRecentTasks(50)
      activitiesData = recentTasks.map((task: any) => ({
        id: task.taskId || task.id,
        title: task.name || '任务执行',
        description: task.description || `${task.type} 任务`,
        type: mapTaskTypeToActivityType(task.type),
        tag: task.type,
        createdAt: task.createdAt,
        userId: task.createdBy,
        userName: task.createdByName || '未知用户',
        important: task.priority === 'HIGH'
      }))
    } else {
      // 教师和学生只看到自己的活动
      const recentTasks = await taskApi.getRecentTasks(30)
      // 过滤出当前用户的任务
      const userTasks = recentTasks.filter((task: any) => 
        task.createdBy === authStore.user?.id
      )
      activitiesData = userTasks.map((task: any) => ({
        id: task.taskId || task.id,
        title: task.name || '我的任务',
        description: task.description || `${task.type} 任务`,
        type: mapTaskTypeToActivityType(task.type),
        tag: task.type,
        createdAt: task.createdAt,
        important: task.priority === 'HIGH'
      }))
    }
    
    // 如果没有真实数据，使用模拟数据
    if (activitiesData.length === 0) {
      activitiesData = generateMockActivities()
    }
    
    activities.value = activitiesData
  } catch (error) {
    console.error('获取活动数据失败:', error)
    // 使用模拟数据作为后备
    activities.value = generateMockActivities()
  } finally {
    loading.value = false
  }
}

const mapTaskTypeToActivityType = (taskType: string) => {
  switch (taskType) {
    case 'KNOWLEDGE_PROCESSING':
    case 'DOCUMENT_UPLOAD':
      return 'upload'
    case 'AI_GENERATION':
    case 'PAPER_GENERATION':
      return 'generate'
    case 'BATCH_EVALUATION':
    case 'AI_EVALUATION':
      return 'evaluation'
    default:
      return 'knowledge'
  }
}

const generateMockActivities = () => {
  const mockActivities = []
  const activityTypes = ['upload', 'generate', 'evaluation', 'knowledge']
  const currentUserId = authStore.user?.id
  
  for (let i = 0; i < (authStore.isAdmin ? 25 : 15); i++) {
    const type = activityTypes[Math.floor(Math.random() * activityTypes.length)]
    const isCurrentUser = authStore.isAdmin ? Math.random() > 0.7 : true
    
    mockActivities.push({
      id: i + 1,
      title: getMockActivityTitle(type),
      description: getMockActivityDescription(type),
      type,
      tag: getMockActivityTag(type),
      createdAt: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
      userId: isCurrentUser ? currentUserId : Math.floor(Math.random() * 100) + 1,
      userName: isCurrentUser ? authStore.user?.username : `用户${Math.floor(Math.random() * 100) + 1}`,
      important: Math.random() > 0.8
    })
  }
  
  return mockActivities.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
}

const getMockActivityTitle = (type: string) => {
  switch (type) {
    case 'upload':
      return '上传了新文档'
    case 'generate':
      return '生成试卷'
    case 'evaluation':
      return 'AI批量评估'
    case 'knowledge':
      return '创建知识库'
    default:
      return '系统操作'
  }
}

const getMockActivityDescription = (type: string) => {
  switch (type) {
    case 'upload':
      return '《高等数学教学大纲》已成功上传到数学知识库'
    case 'generate':
      return '基于线性代数知识库生成了期中考试试卷'
    case 'evaluation':
      return '完成了50份试卷的AI自动评估'
    case 'knowledge':
      return '新建了"计算机网络"知识库'
    default:
      return '执行了系统操作'
  }
}

const getMockActivityTag = (type: string) => {
  switch (type) {
    case 'upload':
      return '文档管理'
    case 'generate':
      return '试卷生成'
    case 'evaluation':
      return 'AI评估'
    case 'knowledge':
      return '知识库'
    default:
      return '系统'
  }
}

const showActivityDetail = (activity: any) => {
  ElMessage.info(`查看活动详情: ${activity.title}`)
}

const handleActivityPageChange = (page: number) => {
  currentActivityPage.value = page
}

// 工具方法
const formatTime = (timeStr: string) => {
  const time = new Date(timeStr)
  return time.toLocaleString('zh-CN')
}

const formatNumber = (num: number) => {
  if (num >= 1000000) {
    return (num / 1000000).toFixed(1) + 'M'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'K'
  }
  return num.toString()
}

const formatCurrency = (amount: number) => {
  return amount.toFixed(2)
}

const formatDuration = (milliseconds: number) => {
  if (milliseconds < 1000) {
    return `${milliseconds}ms`
  }
  const seconds = Math.floor(milliseconds / 1000)
  if (seconds < 60) {
    return `${seconds}s`
  }
  const minutes = Math.floor(seconds / 60)
  return `${minutes}m ${seconds % 60}s`
}

const getActivityType = (type: string) => {
  switch (type) {
    case 'upload': return 'primary'
    case 'generate': return 'success'
    case 'evaluation': return 'warning'
    case 'knowledge': return 'info'
    default: return 'info'
  }
}

const getTagType = (type: string) => {
  switch (type) {
    case 'upload': return 'primary'
    case 'generate': return 'success'
    case 'evaluation': return 'warning'
    case 'knowledge': return 'info'
    default: return 'info'
  }
}

// 初始化数据
onMounted(async () => {
  loading.value = true
  try {
    // 模拟获取统计数据
    await new Promise(resolve => setTimeout(resolve, 500))
    stats.value = {
      knowledgeBaseCount: 5,
      documentCount: 128,
      generatedPaperCount: 23,
      totalSize: 1024.5
    }
    
    // 模拟获取AI统计数据
    aiStats.value = {
      totalCalls: 156,
      totalTokens: 45230,
      totalCost: 12.45,
      avgResponseTime: 2300
    }
    
    await refreshActivity()
  } catch (error) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.knowledge-base-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: calc(100vh - 120px);
}

.page-header {
  margin-bottom: 32px;
}

.page-header h1 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 24px;
  font-weight: 500;
}

.page-description {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.feature-cards-section {
  margin-bottom: 40px;
  z-index: 1;
  position: relative;
  clear: both;
}

.feature-card {
  cursor: pointer;
  transition: all 0.3s;
  height: 180px;
  margin-bottom: 24px;
  background: #fff;
  border-radius: 8px;
  overflow: visible;
  border: 1px solid #ebeef5;
}

.feature-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
  border-color: #c6e2ff;
}

.feature-card .el-card__body {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.feature-icon {
  font-size: 20px;
}

.card-content {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  flex: 1;
  min-height: 80px;
}

.card-content p {
  margin: 0 0 16px 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
  flex: 1;
}

.card-content .el-tag {
  align-self: flex-start;
  margin-top: auto;
}

.tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: auto;
}

.tag-group .el-tag {
  font-size: 12px;
  margin: 0;
}

.stats-section {
  margin: 24px 0;
}

.stats-section .el-card {
  text-align: center;
}

.recent-activity {
  margin-top: 24px;
}

.recent-activity .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.loading-container {
  padding: 20px;
}

.empty-state {
  padding: 40px 20px;
  text-align: center;
}

.activity-content h4 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 14px;
  font-weight: 500;
}

.activity-content p {
  margin: 0 0 8px 0;
  color: #606266;
  font-size: 13px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .knowledge-base-view {
    padding: 16px;
  }
  
  .stats-section .el-col {
    margin-bottom: 16px;
  }
  
  .feature-card {
    height: auto;
    margin-bottom: 16px;
  }
}
</style>
