<template>
  <el-dialog
    v-model="visible"
    title="智能整理知识点"
    width="900px"
    :close-on-click-modal="false"
    @closed="resetForm"
  >
    <div class="organize-dialog">
      <!-- 整理配置 -->
      <el-card class="config-card" shadow="never">
        <template #header>
          <span>整理配置</span>
        </template>
        
        <el-form :model="organizeConfig" label-width="120px">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="整理策略">
                <el-select v-model="organizeConfig.strategy" style="width: 100%">
                  <el-option label="自动分类" value="auto_category">
                    <div>
                      <div>自动分类</div>
                      <div style="font-size: 12px; color: #999;">AI自动识别并分类知识点</div>
                    </div>
                  </el-option>
                  <el-option label="难度排序" value="difficulty_sort">
                    <div>
                      <div>难度排序</div>
                      <div style="font-size: 12px; color: #999;">按难度级别重新排序</div>
                    </div>
                  </el-option>
                  <el-option label="关联分组" value="relation_group">
                    <div>
                      <div>关联分组</div>
                      <div style="font-size: 12px; color: #999;">根据关联关系进行分组</div>
                    </div>
                  </el-option>
                  <el-option label="学习路径" value="learning_path">
                    <div>
                      <div>学习路径</div>
                      <div style="font-size: 12px; color: #999;">生成推荐的学习顺序</div>
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="AI模型">
                <el-select
                  v-model="organizeConfig.aiConfigId"
                  placeholder="选择AI配置"
                  style="width: 100%"
                >
                  <el-option
                    v-for="config in aiConfigs"
                    :key="config.id"
                    :label="config.name"
                    :value="config.id"
                  >
                    <div style="display: flex; justify-content: space-between;">
                      <span>{{ config.name }}</span>
                      <el-tag size="small">{{ config.provider }}</el-tag>
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="整理选项">
            <el-checkbox-group v-model="organizeConfig.options">
              <el-checkbox value="merge_similar">合并相似知识点</el-checkbox>
              <el-checkbox value="split_complex">拆分复杂知识点</el-checkbox>
              <el-checkbox value="add_relations">添加关联关系</el-checkbox>
              <el-checkbox value="generate_summary">生成分类摘要</el-checkbox>
            </el-checkbox-group>
          </el-form-item>

          <el-form-item label="整理范围">
            <el-radio-group v-model="organizeConfig.scope">
              <el-radio value="all">全部知识点</el-radio>
              <el-radio value="selected">仅选中的知识点</el-radio>
              <el-radio value="category">指定分类</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="目标分类" v-if="organizeConfig.scope === 'category'">
            <el-select
              v-model="organizeConfig.targetCategories"
              multiple
              placeholder="选择要整理的分类"
              style="width: 100%"
            >
              <el-option
                v-for="category in availableCategories"
                :key="category"
                :label="category"
                :value="category"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="附加要求">
            <el-input
              v-model="organizeConfig.additionalRequirements"
              type="textarea"
              :rows="2"
              placeholder="可以输入特殊整理要求，如：重点关注某个主题、保持某种结构等"
            />
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 当前知识点概览 -->
      <el-card class="overview-card" shadow="never">
        <template #header>
          <span>当前知识点概览</span>
        </template>
        
        <div class="overview-content">
          <div class="overview-stats">
            <div class="stat-item">
              <span class="stat-label">总数：</span>
              <span class="stat-value">{{ knowledgePoints.length }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">分类：</span>
              <span class="stat-value">{{ uniqueCategories.length }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">待整理：</span>
              <span class="stat-value">{{ unorganizedCount }}</span>
            </div>
          </div>

          <div class="category-distribution">
            <h4>分类分布</h4>
            <div class="category-list">
              <div
                v-for="category in categoryStats"
                :key="category.name"
                class="category-item"
              >
                <span class="category-name">{{ category.name }}</span>
                <el-progress
                  :percentage="category.percentage"
                  :stroke-width="8"
                  :show-text="false"
                />
                <span class="category-count">{{ category.count }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 整理进度 -->
      <el-card v-if="organizing" class="progress-card" shadow="never">
        <template #header>
          <span>整理进度</span>
        </template>
        
        <div class="progress-content">
          <el-progress :percentage="organizeProgress" :status="organizeStatus" />
          <div class="progress-info">
            <div class="current-step">{{ currentStep }}</div>
            <div class="progress-details">
              <span>已处理: {{ processedCount }}/{{ totalCount }}</span>
              <span>{{ getProgressStats() }}</span>
            </div>
          </div>
        </div>

        <div class="progress-logs" v-if="organizeLogs.length > 0">
          <h4>整理日志</h4>
          <div class="log-container">
            <div
              v-for="log in organizeLogs"
              :key="log.id"
              class="log-item"
            >
              <span class="log-time">{{ formatTime(log.timestamp) }}</span>
              <span class="log-message">{{ log.message }}</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 整理结果 -->
      <el-card v-if="organizeResult" class="results-card" shadow="never">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span>整理结果</span>
            <div>
              <el-button size="small" @click="previewChanges">预览变更</el-button>
              <el-button size="small" @click="exportResult">导出结果</el-button>
            </div>
          </div>
        </template>

        <div class="results-content">
          <!-- 整理摘要 -->
          <div class="result-summary">
            <h4>整理摘要</h4>
            <div class="summary-stats">
              <div class="summary-item">
                <span class="summary-label">新增分类：</span>
                <span class="summary-value">{{ organizeResult.newCategories }}</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">合并知识点：</span>
                <span class="summary-value">{{ organizeResult.mergedPoints }}</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">拆分知识点：</span>
                <span class="summary-value">{{ organizeResult.splitPoints }}</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">新增关联：</span>
                <span class="summary-value">{{ organizeResult.newRelations }}</span>
              </div>
            </div>
          </div>

          <!-- 分类结构 -->
          <div class="category-structure">
            <h4>新的分类结构</h4>
            <el-tree
              :data="organizeResult.categoryTree"
              :props="{ children: 'children', label: 'name' }"
              default-expand-all
              class="category-tree"
            >
              <template #default="{ node, data }">
                <div class="tree-node">
                  <span class="node-label">{{ data.name }}</span>
                  <span class="node-count">({{ data.count }})</span>
                  <el-tag
                    v-if="data.isNew"
                    type="success"
                    size="small"
                    class="new-tag"
                  >
                    新增
                  </el-tag>
                </div>
              </template>
            </el-tree>
          </div>

          <!-- 变更详情 -->
          <div class="change-details">
            <h4>变更详情</h4>
            <el-collapse>
              <el-collapse-item title="合并操作" name="merge" v-if="organizeResult.mergeOperations?.length > 0">
                <div
                  v-for="merge in organizeResult.mergeOperations"
                  :key="merge.id"
                  class="operation-item"
                >
                  <div class="operation-header">
                    <el-tag type="info" size="small">合并</el-tag>
                    <span class="operation-title">{{ merge.targetTitle }}</span>
                  </div>
                  <div class="operation-details">
                    <div>原知识点：{{ merge.sourcePoints.join(', ') }}</div>
                    <div>合并理由：{{ merge.reason }}</div>
                  </div>
                </div>
              </el-collapse-item>

              <el-collapse-item title="拆分操作" name="split" v-if="organizeResult.splitOperations?.length > 0">
                <div
                  v-for="split in organizeResult.splitOperations"
                  :key="split.id"
                  class="operation-item"
                >
                  <div class="operation-header">
                    <el-tag type="warning" size="small">拆分</el-tag>
                    <span class="operation-title">{{ split.sourceTitle }}</span>
                  </div>
                  <div class="operation-details">
                    <div>拆分为：{{ split.targetPoints.join(', ') }}</div>
                    <div>拆分理由：{{ split.reason }}</div>
                  </div>
                </div>
              </el-collapse-item>

              <el-collapse-item title="关联操作" name="relation" v-if="organizeResult.relationOperations?.length > 0">
                <div
                  v-for="relation in organizeResult.relationOperations"
                  :key="relation.id"
                  class="operation-item"
                >
                  <div class="operation-header">
                    <el-tag type="success" size="small">关联</el-tag>
                    <span class="operation-title">{{ relation.fromPoint }} → {{ relation.toPoint }}</span>
                  </div>
                  <div class="operation-details">
                    <div>关联类型：{{ relation.relationType }}</div>
                    <div>关联理由：{{ relation.reason }}</div>
                  </div>
                </div>
              </el-collapse-item>
            </el-collapse>
          </div>
        </div>
      </el-card>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false" :disabled="organizing">取消</el-button>
        <el-button
          v-if="!organizing && !organizeResult"
          type="primary"
          @click="startOrganize"
          :disabled="!canStartOrganize"
        >
          开始整理
        </el-button>
        <el-button
          v-if="organizeResult"
          type="primary"
          @click="applyChanges"
        >
          应用变更
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// Props
const props = defineProps<{
  modelValue: boolean
  knowledgePoints: any[]
}>()

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  organized: []
}>()

// 响应式数据
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 整理配置
const organizeConfig = ref({
  strategy: 'auto_category',
  aiConfigId: 0,
  options: ['merge_similar', 'add_relations'],
  scope: 'all' as 'all' | 'selected' | 'category',
  targetCategories: [] as string[],
  additionalRequirements: ''
})

// 状态管理
const organizing = ref(false)
const organizeProgress = ref(0)
const organizeStatus = ref<'success' | 'exception' | undefined>(undefined)
const currentStep = ref('')
const processedCount = ref(0)
const totalCount = ref(0)

// 数据
const aiConfigs = ref<any[]>([])
const organizeLogs = ref<any[]>([])
const organizeResult = ref<any>(null)

// 计算属性
const canStartOrganize = computed(() => {
  return organizeConfig.value.aiConfigId && props.knowledgePoints.length > 0
})

const uniqueCategories = computed(() => {
  const categories = new Set(props.knowledgePoints.map(point => point.category || '未分类'))
  return Array.from(categories)
})

const availableCategories = computed(() => {
  return uniqueCategories.value
})

const unorganizedCount = computed(() => {
  return props.knowledgePoints.filter(point => !point.category || point.category === '未分类').length
})

const categoryStats = computed(() => {
  const stats = new Map<string, number>()
  props.knowledgePoints.forEach(point => {
    const category = point.category || '未分类'
    stats.set(category, (stats.get(category) || 0) + 1)
  })
  
  return Array.from(stats.entries()).map(([name, count]) => ({
    name,
    count,
    percentage: Math.round((count / props.knowledgePoints.length) * 100)
  })).sort((a, b) => b.count - a.count)
})

// 生命周期
onMounted(() => {
  loadAIConfigs()
})

// 方法
const loadAIConfigs = async () => {
  try {
    // TODO: 从API加载AI配置
    aiConfigs.value = [
      { id: 1, name: 'OpenAI GPT-4', provider: 'OpenAI' },
      { id: 2, name: 'DeepSeek Chat', provider: 'DeepSeek' }
    ]
    
    if (aiConfigs.value.length > 0) {
      organizeConfig.value.aiConfigId = aiConfigs.value[0].id
    }
  } catch (error) {
    console.error('Load AI configs failed:', error)
  }
}

const startOrganize = async () => {
  try {
    organizing.value = true
    organizeProgress.value = 0
    organizeStatus.value = undefined
    organizeLogs.value = []
    organizeResult.value = null
    
    currentStep.value = '准备整理任务...'
    totalCount.value = props.knowledgePoints.length
    processedCount.value = 0
    
    addLog('开始AI智能整理')
    await simulateOrganize()
    
    organizeStatus.value = 'success'
    ElMessage.success('知识点整理完成')
  } catch (error) {
    organizeStatus.value = 'exception'
    ElMessage.error('整理失败，请重试')
    console.error('Organize failed:', error)
  } finally {
    organizing.value = false
  }
}

const simulateOrganize = async () => {
  const steps = [
    '分析知识点关联...',
    '识别相似内容...',
    '生成分类方案...',
    '优化学习路径...',
    '完成整理'
  ]
  
  for (let i = 0; i < steps.length; i++) {
    currentStep.value = steps[i]
    addLog(steps[i])
    
    await new Promise(resolve => setTimeout(resolve, 1500))
    organizeProgress.value = ((i + 1) / steps.length) * 100
    processedCount.value = Math.min(totalCount.value, Math.round(((i + 1) / steps.length) * totalCount.value))
  }
  
  // 生成模拟结果
  organizeResult.value = {
    newCategories: 2,
    mergedPoints: 3,
    splitPoints: 1,
    newRelations: 5,
    categoryTree: [
      {
        name: '基础概念',
        count: 8,
        isNew: false,
        children: [
          { name: '函数定义', count: 3, isNew: false },
          { name: '数学符号', count: 2, isNew: true },
          { name: '基本性质', count: 3, isNew: false }
        ]
      },
      {
        name: '应用方法',
        count: 6,
        isNew: true,
        children: [
          { name: '解题技巧', count: 4, isNew: false },
          { name: '实际应用', count: 2, isNew: false }
        ]
      }
    ],
    mergeOperations: [
      {
        id: 1,
        targetTitle: '函数的基本概念',
        sourcePoints: ['函数定义', '函数性质'],
        reason: '内容相近，可以合并为一个完整的概念'
      }
    ],
    splitOperations: [
      {
        id: 1,
        sourceTitle: '复杂函数应用',
        targetPoints: ['函数图像分析', '函数实际应用'],
        reason: '内容过于复杂，建议拆分为两个独立的知识点'
      }
    ],
    relationOperations: [
      {
        id: 1,
        fromPoint: '函数定义',
        toPoint: '函数性质',
        relationType: '前置关系',
        reason: '学习函数性质需要先掌握函数定义'
      }
    ]
  }
  
  addLog(`整理完成：新增${organizeResult.value.newCategories}个分类，优化${organizeResult.value.mergedPoints + organizeResult.value.splitPoints}个知识点`)
}

const addLog = (message: string) => {
  organizeLogs.value.push({
    id: Date.now(),
    timestamp: new Date(),
    message
  })
  
  // 限制日志数量
  if (organizeLogs.value.length > 20) {
    organizeLogs.value = organizeLogs.value.slice(-10)
  }
}

const getProgressStats = () => {
  if (!organizeResult.value) return ''
  return `新增分类: ${organizeResult.value.newCategories}, 优化知识点: ${organizeResult.value.mergedPoints + organizeResult.value.splitPoints}`
}

const previewChanges = () => {
  ElMessage.info('预览变更功能开发中...')
}

const exportResult = () => {
  ElMessage.info('导出结果功能开发中...')
}

const applyChanges = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要应用这些整理变更吗？此操作会修改现有的知识点结构。',
      '确认应用变更',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // TODO: 调用API应用变更
    console.log('Applying organize changes:', organizeResult.value)
    
    ElMessage.success('知识点整理已应用')
    emit('organized')
    visible.value = false
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('应用变更失败')
      console.error('Apply changes failed:', error)
    }
  }
}

const resetForm = () => {
  organizeConfig.value = {
    strategy: 'auto_category',
    aiConfigId: aiConfigs.value[0]?.id || 0,
    options: ['merge_similar', 'add_relations'],
    scope: 'all',
    targetCategories: [],
    additionalRequirements: ''
  }
  
  organizeLogs.value = []
  organizeResult.value = null
  organizing.value = false
  organizeProgress.value = 0
}

// 工具方法
const formatTime = (date: Date) => {
  return date.toLocaleTimeString()
}
</script>

<style scoped>
.organize-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.config-card,
.overview-card,
.progress-card,
.results-card {
  border: 1px solid #e4e7ed;
}

.overview-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.overview-stats {
  display: flex;
  gap: 24px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-label {
  color: #909399;
  font-size: 14px;
}

.stat-value {
  color: #303133;
  font-weight: 600;
}

.category-distribution h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #606266;
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.category-item {
  display: grid;
  grid-template-columns: 120px 1fr 40px;
  align-items: center;
  gap: 12px;
}

.category-name {
  font-size: 13px;
  color: #606266;
}

.category-count {
  font-size: 13px;
  color: #909399;
  text-align: right;
}

.progress-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.current-step {
  font-weight: 600;
  color: #409eff;
}

.progress-details {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: #909399;
}

.progress-logs {
  margin-top: 16px;
}

.progress-logs h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #606266;
}

.log-container {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 8px;
  background: #fafafa;
}

.log-item {
  display: flex;
  gap: 12px;
  padding: 4px 0;
  font-size: 13px;
}

.log-time {
  color: #909399;
  min-width: 80px;
}

.log-message {
  color: #606266;
}

.results-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.result-summary h4,
.category-structure h4,
.change-details h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #606266;
}

.summary-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.summary-label {
  font-size: 13px;
  color: #606266;
}

.summary-value {
  font-weight: 600;
  color: #409eff;
}

.category-tree {
  background: #f8f9fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.node-label {
  flex: 1;
}

.node-count {
  font-size: 12px;
  color: #909399;
}

.new-tag {
  margin-left: 4px;
}

.operation-item {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
  margin-bottom: 8px;
  background: #fafafa;
}

.operation-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.operation-title {
  font-weight: 600;
  color: #303133;
}

.operation-details {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.operation-details > div {
  margin-bottom: 4px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
