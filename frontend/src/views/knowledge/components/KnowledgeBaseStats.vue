<template>
  <div class="knowledge-base-stats">
    <el-row :gutter="16">
      <!-- 基本统计 -->
      <el-col :xl="6" :lg="12" :md="12" :sm="24" :xs="24">
        <el-card class="stat-card document-stat">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="32"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.documentCount || 0 }}</div>
              <div class="stat-label">文档数量</div>
              <div class="stat-trend" :class="{ positive: documentTrend > 0, negative: documentTrend < 0 }">
                <el-icon v-if="documentTrend > 0"><ArrowUp /></el-icon>
                <el-icon v-if="documentTrend < 0"><ArrowDown /></el-icon>
                <span>{{ Math.abs(documentTrend) }}%</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xl="6" :lg="12" :md="12" :sm="24" :xs="24">
        <el-card class="stat-card knowledge-stat">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="32"><DataBoard /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.knowledgePointCount || 0 }}</div>
              <div class="stat-label">知识点数量</div>
              <div class="stat-trend" :class="{ positive: knowledgeTrend > 0, negative: knowledgeTrend < 0 }">
                <el-icon v-if="knowledgeTrend > 0"><ArrowUp /></el-icon>
                <el-icon v-if="knowledgeTrend < 0"><ArrowDown /></el-icon>
                <span>{{ Math.abs(knowledgeTrend) }}%</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xl="6" :lg="12" :md="12" :sm="24" :xs="24">
        <el-card class="stat-card question-stat">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="32"><QuestionFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.questionCount || 0 }}</div>
              <div class="stat-label">题目数量</div>
              <div class="stat-trend" :class="{ positive: questionTrend > 0, negative: questionTrend < 0 }">
                <el-icon v-if="questionTrend > 0"><ArrowUp /></el-icon>
                <el-icon v-if="questionTrend < 0"><ArrowDown /></el-icon>
                <span>{{ Math.abs(questionTrend) }}%</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xl="6" :lg="12" :md="12" :sm="24" :xs="24">
        <el-card class="stat-card ai-stat">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="32"><Cpu /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.aiGeneratedCount || 0 }}</div>
              <div class="stat-label">AI生成内容</div>
              <div class="stat-trend positive">
                <el-icon><ArrowUp /></el-icon>
                <span>{{ aiGenerationRate }}%</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 详细统计图表 -->
    <el-row :gutter="16" style="margin-top: 16px;">
      <!-- 难度分布 -->
      <el-col :xl="8" :lg="12" :md="24" :sm="24" :xs="24">
        <el-card class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>难度分布</span>
              <el-button size="small" text @click="refreshDifficultyChart">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>
          </template>
          <div class="difficulty-chart">
            <div class="difficulty-item">
              <div class="difficulty-label">
                <el-tag type="success" size="small">简单</el-tag>
                <span class="difficulty-count">{{ difficultyStats.EASY || 0 }}</span>
              </div>
              <el-progress 
                :percentage="getDifficultyPercentage('EASY')" 
                :show-text="false"
                color="#67c23a"
              />
            </div>
            <div class="difficulty-item">
              <div class="difficulty-label">
                <el-tag type="warning" size="small">中等</el-tag>
                <span class="difficulty-count">{{ difficultyStats.MEDIUM || 0 }}</span>
              </div>
              <el-progress 
                :percentage="getDifficultyPercentage('MEDIUM')" 
                :show-text="false"
                color="#e6a23c"
              />
            </div>
            <div class="difficulty-item">
              <div class="difficulty-label">
                <el-tag type="danger" size="small">困难</el-tag>
                <span class="difficulty-count">{{ difficultyStats.HARD || 0 }}</span>
              </div>
              <el-progress 
                :percentage="getDifficultyPercentage('HARD')" 
                :show-text="false"
                color="#f56c6c"
              />
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 标签云 -->
      <el-col :xl="8" :lg="12" :md="24" :sm="24" :xs="24">
        <el-card class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>热门标签</span>
              <el-button size="small" text @click="refreshTagCloud">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>
          </template>
          <div class="tag-cloud">
            <el-tag
              v-for="tag in topTags"
              :key="tag.name"
              :size="getTagSize(tag.count)"
              :type="getTagType(tag.count)"
              style="margin: 4px;"
            >
              {{ tag.name }} ({{ tag.count }})
            </el-tag>
          </div>
        </el-card>
      </el-col>

      <!-- 活动时间线 -->
      <el-col :xl="8" :lg="24" :md="24" :sm="24" :xs="24">
        <el-card class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>最近活动</span>
              <el-button size="small" text @click="refreshActivity">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>
          </template>
          <div class="activity-timeline">
            <div
              v-for="(activity, index) in recentActivities"
              :key="index"
              class="activity-item"
            >
              <div class="activity-time">{{ formatTime(activity.timestamp) }}</div>
              <div class="activity-content">
                <el-tag :type="getActivityType(activity.type)" size="small">
                  {{ activity.type }}
                </el-tag>
                <span class="activity-description">{{ activity.description }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 进度和健康度 -->
    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="24">
        <el-card class="health-card">
          <template #header>
            <span>知识库健康度</span>
          </template>
          <div class="health-content">
            <div class="health-metrics">
              <div class="metric-item">
                <div class="metric-label">完整性</div>
                <el-progress :percentage="healthMetrics.completeness" :color="getHealthColor(healthMetrics.completeness)" />
                <div class="metric-description">基于文档和知识点的覆盖率</div>
              </div>
              <div class="metric-item">
                <div class="metric-label">质量</div>
                <el-progress :percentage="healthMetrics.quality" :color="getHealthColor(healthMetrics.quality)" />
                <div class="metric-description">基于AI评估和用户反馈</div>
              </div>
              <div class="metric-item">
                <div class="metric-label">活跃度</div>
                <el-progress :percentage="healthMetrics.activity" :color="getHealthColor(healthMetrics.activity)" />
                <div class="metric-description">基于最近的更新和使用频率</div>
              </div>
              <div class="metric-item">
                <div class="metric-label">AI增强</div>
                <el-progress :percentage="healthMetrics.aiEnhancement" :color="getHealthColor(healthMetrics.aiEnhancement)" />
                <div class="metric-description">AI生成和优化的内容比例</div>
              </div>
            </div>
            <div class="health-summary">
              <div class="overall-score">
                <div class="score-value">{{ overallHealthScore }}</div>
                <div class="score-label">总体健康度</div>
              </div>
              <div class="health-suggestions">
                <h4>建议</h4>
                <ul>
                  <li v-for="(suggestion, index) in healthSuggestions" :key="index">
                    {{ suggestion }}
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { 
  Document, 
  DataBoard, 
  QuestionFilled, 
  Cpu, 
  Refresh, 
  ArrowUp, 
  ArrowDown 
} from '@element-plus/icons-vue'
import { knowledgePointApi, knowledgeBaseApi } from '@/api/knowledge'

interface Props {
  knowledgeBaseId?: number | null
}

const props = defineProps<Props>()

// 基本统计数据
const stats = reactive({
  documentCount: 0,
  knowledgePointCount: 0,
  questionCount: 0,
  aiGeneratedCount: 0
})

// 趋势数据
const documentTrend = ref(12) // 相比上周的百分比变化
const knowledgeTrend = ref(8)
const questionTrend = ref(-3)

// 难度分布
const difficultyStats = reactive({
  EASY: 0,
  MEDIUM: 0,
  HARD: 0
})

// 标签统计
const topTags = ref([
  { name: '基础概念', count: 45 },
  { name: '重点知识', count: 32 },
  { name: '难点分析', count: 28 },
  { name: '实例应用', count: 21 },
  { name: '综合练习', count: 18 }
])

// 最近活动
const recentActivities = ref([
  {
    type: '文档上传',
    description: '上传了新的课件文档',
    timestamp: new Date(Date.now() - 1000 * 60 * 30) // 30分钟前
  },
  {
    type: 'AI生成',
    description: '生成了15个新的练习题',
    timestamp: new Date(Date.now() - 1000 * 60 * 60 * 2) // 2小时前
  },
  {
    type: '知识点提取',
    description: '从文档中提取了8个新知识点',
    timestamp: new Date(Date.now() - 1000 * 60 * 60 * 5) // 5小时前
  },
  {
    type: '标签更新',
    description: '更新了知识点标签分类',
    timestamp: new Date(Date.now() - 1000 * 60 * 60 * 24) // 1天前
  }
])

// 健康度指标
const healthMetrics = reactive({
  completeness: 85,
  quality: 78,
  activity: 92,
  aiEnhancement: 68
})

// 计算属性
const aiGenerationRate = computed(() => {
  const total = stats.knowledgePointCount + stats.questionCount
  return total > 0 ? Math.round((stats.aiGeneratedCount / total) * 100) : 0
})

const overallHealthScore = computed(() => {
  const scores = Object.values(healthMetrics)
  return Math.round(scores.reduce((sum, score) => sum + score, 0) / scores.length)
})

const healthSuggestions = computed(() => {
  const suggestions = []
  
  if (healthMetrics.completeness < 80) {
    suggestions.push('建议上传更多相关文档以提高知识覆盖率')
  }
  if (healthMetrics.quality < 75) {
    suggestions.push('可以使用AI工具优化现有内容质量')
  }
  if (healthMetrics.activity < 70) {
    suggestions.push('建议增加知识库的使用和更新频率')
  }
  if (healthMetrics.aiEnhancement < 60) {
    suggestions.push('可以使用更多AI功能来增强知识库内容')
  }
  
  if (suggestions.length === 0) {
    suggestions.push('知识库状态良好，继续保持！')
  }
  
  return suggestions
})

// 监听知识库ID变化
watch(() => props.knowledgeBaseId, (newId) => {
  if (newId) {
    loadStatistics(newId)
  }
}, { immediate: true })

// 加载统计数据
const loadStatistics = async (knowledgeBaseId: number) => {
  try {
    // 加载基本统计
    const knowledgeBase = await knowledgeBaseApi.getKnowledgeBase(knowledgeBaseId)
    stats.documentCount = knowledgeBase.documentCount || 0
    stats.knowledgePointCount = knowledgeBase.knowledgePointCount || 0
    
    // 加载知识点统计
    const pointStats = await knowledgePointApi.getKnowledgePointStatistics(knowledgeBaseId)
    Object.assign(difficultyStats, pointStats.difficultyDistribution)
    stats.aiGeneratedCount = pointStats.aiGeneratedCount || 0
    
    // 更新标签数据
    const tagDistribution = pointStats.tagDistribution || {}
    topTags.value = Object.entries(tagDistribution)
      .map(([name, count]) => ({ name, count: count as number }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 8)
    
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 辅助函数
const getDifficultyPercentage = (difficulty: string) => {
  const total = Object.values(difficultyStats).reduce((sum, count) => sum + count, 0)
  if (total === 0) return 0
  return Math.round((difficultyStats[difficulty as keyof typeof difficultyStats] / total) * 100)
}

const getTagSize = (count: number) => {
  if (count > 30) return 'large'
  if (count > 15) return 'default'
  return 'small'
}

const getTagType = (count: number) => {
  if (count > 30) return 'danger'
  if (count > 20) return 'warning'
  if (count > 10) return 'success'
  return 'info'
}

const getActivityType = (type: string) => {
  switch (type) {
    case '文档上传': return 'primary'
    case 'AI生成': return 'success'
    case '知识点提取': return 'warning'
    case '标签更新': return 'info'
    default: return 'info'
  }
}

const getHealthColor = (percentage: number) => {
  if (percentage >= 90) return '#67c23a'
  if (percentage >= 70) return '#e6a23c'
  if (percentage >= 50) return '#f56c6c'
  return '#909399'
}

const formatTime = (date: Date) => {
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  const minutes = Math.floor(diff / (1000 * 60))
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days > 0) return `${days}天前`
  if (hours > 0) return `${hours}小时前`
  if (minutes > 0) return `${minutes}分钟前`
  return '刚刚'
}

// 刷新方法
const refreshDifficultyChart = () => {
  if (props.knowledgeBaseId) {
    loadStatistics(props.knowledgeBaseId)
  }
}

const refreshTagCloud = () => {
  if (props.knowledgeBaseId) {
    loadStatistics(props.knowledgeBaseId)
  }
}

const refreshActivity = () => {
  // 模拟刷新活动数据
  console.log('刷新活动数据')
}
</script>

<style scoped>
.knowledge-base-stats {
  padding: 16px;
}

.stat-card {
  height: 120px;
  border: 1px solid #e4e7ed;
  transition: all 0.3s ease;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.stat-content {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 16px;
}

.stat-icon {
  margin-right: 16px;
  color: #409eff;
}

.document-stat .stat-icon {
  color: #67c23a;
}

.knowledge-stat .stat-icon {
  color: #e6a23c;
}

.question-stat .stat-icon {
  color: #f56c6c;
}

.ai-stat .stat-icon {
  color: #909399;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 4px;
}

.stat-trend {
  display: flex;
  align-items: center;
  font-size: 12px;
  gap: 2px;
}

.stat-trend.positive {
  color: #67c23a;
}

.stat-trend.negative {
  color: #f56c6c;
}

.chart-card {
  border: 1px solid #e4e7ed;
  height: 280px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.difficulty-chart {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px 0;
}

.difficulty-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.difficulty-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.difficulty-count {
  font-weight: 600;
  color: #303133;
}

.tag-cloud {
  padding: 16px;
  min-height: 200px;
  display: flex;
  flex-wrap: wrap;
  align-content: flex-start;
}

.activity-timeline {
  padding: 16px 0;
  max-height: 200px;
  overflow-y: auto;
}

.activity-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-time {
  font-size: 12px;
  color: #909399;
}

.activity-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.activity-description {
  font-size: 14px;
  color: #303133;
}

.health-card {
  border: 1px solid #e4e7ed;
}

.health-content {
  display: flex;
  gap: 24px;
}

.health-metrics {
  flex: 2;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.metric-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.metric-label {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.metric-description {
  font-size: 12px;
  color: #909399;
}

.health-summary {
  flex: 1;
  border-left: 1px solid #e4e7ed;
  padding-left: 24px;
}

.overall-score {
  text-align: center;
  margin-bottom: 24px;
}

.score-value {
  font-size: 48px;
  font-weight: 600;
  color: #409eff;
  line-height: 1;
}

.score-label {
  font-size: 14px;
  color: #606266;
  margin-top: 8px;
}

.health-suggestions h4 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 14px;
}

.health-suggestions ul {
  margin: 0;
  padding-left: 16px;
  list-style-type: disc;
}

.health-suggestions li {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
  margin-bottom: 4px;
}

@media (max-width: 768px) {
  .health-content {
    flex-direction: column;
  }
  
  .health-summary {
    border-left: none;
    border-top: 1px solid #e4e7ed;
    padding-left: 0;
    padding-top: 24px;
  }
  
  .health-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
