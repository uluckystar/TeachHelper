<template>
  <div class="knowledge-based-generation">
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
    >
      <!-- 知识库选择 -->
      <el-card class="section-card">
        <template #header>
          <span>知识库选择</span>
        </template>

        <el-form-item label="选择知识库" prop="knowledgeBaseIds">
          <el-select
            v-model="form.knowledgeBaseIds"
            multiple
            placeholder="请选择知识库"
            style="width: 100%"
            @change="handleKnowledgeBaseChange"
          >
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            >
              <div class="kb-option">
                <div class="kb-info">
                  <span class="kb-name">{{ kb.name }}</span>
                  <span class="kb-subject">{{ kb.subject }}</span>
                </div>
                <div class="kb-stats">
                  <el-tag size="small">{{ kb.documentCount }} 文档</el-tag>
                  <el-tag size="small" type="success">{{ kb.knowledgePointCount }} 知识点</el-tag>
                </div>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="知识点筛选" v-if="availableKnowledgePoints.length > 0">
          <div class="knowledge-points-selector">
            <div class="selector-header">
              <el-input
                v-model="knowledgePointFilter"
                placeholder="搜索知识点"
                clearable
                style="width: 200px"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <div class="selector-actions">
                <el-button size="small" @click="selectAllKnowledgePoints">全选</el-button>
                <el-button size="small" @click="clearKnowledgePoints">清空</el-button>
              </div>
            </div>
            
            <div class="knowledge-points-tree">
              <el-tree
                ref="knowledgePointTreeRef"
                :data="filteredKnowledgePoints"
                :props="treeProps"
                show-checkbox
                node-key="id"
                @check="handleKnowledgePointCheck"
              >
                <template #default="{ node, data }">
                  <span class="tree-node">
                    <el-icon v-if="data.type === 'chapter'"><FolderOpened /></el-icon>
                    <el-icon v-else><Document /></el-icon>
                    <span>{{ node.label }}</span>
                    <el-tag v-if="data.difficulty" size="small" :type="getDifficultyTagType(data.difficulty)">
                      {{ data.difficulty }}
                    </el-tag>
                  </span>
                </template>
              </el-tree>
            </div>
          </div>
        </el-form-item>
      </el-card>

      <!-- 生成配置 -->
      <el-card class="section-card">
        <template #header>
          <span>生成配置</span>
        </template>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="试卷标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入试卷标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考试时长" prop="duration">
              <el-input-number
                v-model="form.duration"
                :min="30"
                :max="300"
                :step="15"
                style="width: 100%"
              />
              <span style="margin-left: 8px;">分钟</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="生成方式">
          <el-radio-group v-model="form.generationMode">
            <el-radio label="auto">智能生成</el-radio>
            <el-radio label="template">基于模板</el-radio>
            <el-radio label="custom">自定义配置</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="内容来源" v-if="form.generationMode === 'auto'">
          <el-checkbox-group v-model="form.contentSources">
            <el-checkbox label="documents">文档内容</el-checkbox>
            <el-checkbox label="knowledge_points">知识点摘要</el-checkbox>
            <el-checkbox label="ai_extension">AI扩展内容</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="题型权重" v-if="form.generationMode === 'custom'">
          <div class="question-type-weights">
            <div
              v-for="(weight, type) in form.questionTypeWeights"
              :key="type"
              class="weight-item"
            >
              <span class="weight-label">{{ getQuestionTypeText(type) }}</span>
              <el-slider
                v-model="form.questionTypeWeights[type]"
                :min="0"
                :max="100"
                show-tooltip
                style="flex: 1; margin: 0 16px"
              />
              <span class="weight-value">{{ weight }}%</span>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="难度分布">
          <div class="difficulty-distribution">
            <div class="difficulty-item">
              <span>简单</span>
              <el-slider
                v-model="form.difficultyDistribution.easy"
                :max="100"
                style="flex: 1; margin: 0 16px"
              />
              <span>{{ form.difficultyDistribution.easy }}%</span>
            </div>
            <div class="difficulty-item">
              <span>中等</span>
              <el-slider
                v-model="form.difficultyDistribution.medium"
                :max="100"
                style="flex: 1; margin: 0 16px"
              />
              <span>{{ form.difficultyDistribution.medium }}%</span>
            </div>
            <div class="difficulty-item">
              <span>困难</span>
              <el-slider
                v-model="form.difficultyDistribution.hard"
                :max="100"
                style="flex: 1; margin: 0 16px"
              />
              <span>{{ form.difficultyDistribution.hard }}%</span>
            </div>
          </div>
        </el-form-item>
      </el-card>

      <!-- AI配置 -->
      <el-card class="section-card">
        <template #header>
          <span>AI配置</span>
        </template>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="AI模型" prop="aiConfigId">
              <el-select v-model="form.aiConfigId" placeholder="选择AI配置" style="width: 100%">
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
          <el-col :span="12">
            <el-form-item label="生成质量">
              <el-select v-model="form.quality" placeholder="选择生成质量" style="width: 100%">
                <el-option label="标准质量" value="standard" />
                <el-option label="高质量" value="high" />
                <el-option label="最高质量" value="premium" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="特殊要求">
          <el-input
            v-model="form.specialRequirements"
            type="textarea"
            :rows="3"
            placeholder="输入特殊要求，如：偏重某个章节、特定题型要求等"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-card>

      <!-- 预览信息 -->
      <el-card class="section-card" v-if="selectedKnowledgeBases.length > 0">
        <template #header>
          <span>预览信息</span>
        </template>

        <el-descriptions :column="3" border>
          <el-descriptions-item label="选中知识库">
            {{ selectedKnowledgeBases.length }} 个
          </el-descriptions-item>
          <el-descriptions-item label="选中知识点">
            {{ selectedKnowledgePoints.length }} 个
          </el-descriptions-item>
          <el-descriptions-item label="可用文档">
            {{ totalDocuments }} 个
          </el-descriptions-item>
        </el-descriptions>

        <div class="preview-details">
          <div class="detail-section">
            <h4>选中知识库</h4>
            <div class="kb-list">
              <el-tag
                v-for="kb in selectedKnowledgeBases"
                :key="kb.id"
                class="kb-tag"
              >
                {{ kb.name }}
              </el-tag>
            </div>
          </div>

          <div class="detail-section" v-if="selectedKnowledgePoints.length > 0">
            <h4>重点知识点</h4>
            <div class="kp-list">
              <el-tag
                v-for="kp in selectedKnowledgePoints.slice(0, 10)"
                :key="kp.id"
                size="small"
                type="success"
              >
                {{ kp.title }}
              </el-tag>
              <span v-if="selectedKnowledgePoints.length > 10" class="more-indicator">
                ...还有 {{ selectedKnowledgePoints.length - 10 }} 个
              </span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 操作按钮 -->
      <div class="form-actions">
        <el-button @click="resetForm">重置</el-button>
        <el-button @click="previewGeneration" :loading="loading">预览生成</el-button>
        <el-button
          type="primary"
          @click="handleSubmit"
          :loading="loading"
          :disabled="form.knowledgeBaseIds.length === 0"
        >
          基于知识库生成试卷
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { knowledgeBaseApi, knowledgePointApi } from '@/api/knowledge'
import { userAIConfigApi } from '@/api/userAIConfig'
import { getQuestionTypeText } from '@/utils/tagTypes'

interface Props {
  loading?: boolean
}

interface Emits {
  (e: 'generate', data: any): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const formRef = ref()
const knowledgePointTreeRef = ref()
const knowledgeBases = ref<any[]>([])
const availableKnowledgePoints = ref<any[]>([])
const aiConfigs = ref<any[]>([])
const knowledgePointFilter = ref('')

// 表单数据
const form = reactive({
  title: '',
  duration: 120,
  knowledgeBaseIds: [] as number[],
  selectedKnowledgePointIds: [] as number[],
  generationMode: 'auto',
  contentSources: ['documents', 'knowledge_points'],
  questionTypeWeights: {
    SINGLE_CHOICE: 40,
    MULTIPLE_CHOICE: 20,
    TRUE_FALSE: 10,
    FILL_BLANK: 15,
    SHORT_ANSWER: 10,
    ESSAY: 5
  },
  difficultyDistribution: {
    easy: 30,
    medium: 50,
    hard: 20
  },
  aiConfigId: 0,
  quality: 'standard',
  specialRequirements: ''
})

// 树形组件配置
const treeProps = {
  label: 'title',
  children: 'children'
}

// 表单验证规则
const rules = {
  title: [
    { required: true, message: '请输入试卷标题', trigger: 'blur' }
  ],
  knowledgeBaseIds: [
    { required: true, message: '请选择至少一个知识库', trigger: 'change' }
  ],
  aiConfigId: [
    { required: true, message: '请选择AI配置', trigger: 'change' }
  ]
}

// 计算属性
const selectedKnowledgeBases = computed(() => {
  return knowledgeBases.value.filter(kb => form.knowledgeBaseIds.includes(kb.id))
})

const selectedKnowledgePoints = computed(() => {
  return availableKnowledgePoints.value.filter(kp => 
    form.selectedKnowledgePointIds.includes(kp.id)
  )
})

const totalDocuments = computed(() => {
  return selectedKnowledgeBases.value.reduce((sum, kb) => sum + kb.documentCount, 0)
})

const filteredKnowledgePoints = computed(() => {
  if (!knowledgePointFilter.value) {
    return availableKnowledgePoints.value
  }
  
  return availableKnowledgePoints.value.filter(kp =>
    kp.title.toLowerCase().includes(knowledgePointFilter.value.toLowerCase())
  )
})

// 生命周期
onMounted(() => {
  loadKnowledgeBases()
  loadAIConfigs()
})

// 监听知识库选择变化
watch(() => form.knowledgeBaseIds, () => {
  loadKnowledgePoints()
}, { deep: true })

// 方法
const loadKnowledgeBases = async () => {
  try {
    const response = await knowledgeBaseApi.getKnowledgeBases()
    knowledgeBases.value = response.content || response
  } catch (error) {
    console.error('加载知识库失败:', error)
    ElMessage.error('加载知识库失败')
  }
}

const loadKnowledgePoints = async () => {
  if (form.knowledgeBaseIds.length === 0) {
    availableKnowledgePoints.value = []
    return
  }
  
  try {
    availableKnowledgePoints.value = []
    
    // 为每个选中的知识库加载知识点
    for (const knowledgeBaseId of form.knowledgeBaseIds) {
      const response = await knowledgePointApi.getKnowledgePoints({
        knowledgeBaseId: knowledgeBaseId,
        page: 0,
        size: 1000  // 获取所有知识点
      })
      
      const knowledgePoints = response.content || []
      
      // 将知识点按章节/主题分组
      const groupedPoints = groupKnowledgePointsByCategory(knowledgePoints, knowledgeBaseId)
      availableKnowledgePoints.value.push(...groupedPoints)
    }
  } catch (error) {
    console.error('加载知识点失败:', error)
    ElMessage.error('加载知识点失败')
    // 仍然提供一些基本的分组结构作为后备
    availableKnowledgePoints.value = [{
      id: 'error',
      title: '加载失败，请检查网络连接',
      type: 'error',
      children: []
    }]
  }
}

const loadAIConfigs = async () => {
  try {
    aiConfigs.value = await userAIConfigApi.getConfigs()
    if (aiConfigs.value.length > 0 && !form.aiConfigId) {
      form.aiConfigId = aiConfigs.value[0].id
    }
  } catch (error) {
    console.error('加载AI配置失败:', error)
  }
}

const handleKnowledgeBaseChange = () => {
  form.selectedKnowledgePointIds = []
}

const handleKnowledgePointCheck = (data: any, { checkedKeys }: any) => {
  form.selectedKnowledgePointIds = checkedKeys
}

const selectAllKnowledgePoints = () => {
  if (knowledgePointTreeRef.value) {
    const allIds = getAllKnowledgePointIds(availableKnowledgePoints.value)
    knowledgePointTreeRef.value.setCheckedKeys(allIds)
    form.selectedKnowledgePointIds = allIds
  }
}

const clearKnowledgePoints = () => {
  if (knowledgePointTreeRef.value) {
    knowledgePointTreeRef.value.setCheckedKeys([])
    form.selectedKnowledgePointIds = []
  }
}

const getAllKnowledgePointIds = (points: any[]): number[] => {
  let ids: number[] = []
  points.forEach(point => {
    if (point.children) {
      ids = ids.concat(getAllKnowledgePointIds(point.children))
    } else {
      ids.push(point.id)
    }
  })
  return ids
}

const getDifficultyTagType = (difficulty: string) => {
  switch (difficulty) {
    case 'EASY': return 'success'
    case 'MEDIUM': return 'warning'
    case 'HARD': return 'danger'
    default: return 'info'
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    emit('generate', { ...form })
  } catch (error) {
    console.error('表单验证失败:', error)
  }
}

const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  form.selectedKnowledgePointIds = []
  if (knowledgePointTreeRef.value) {
    knowledgePointTreeRef.value.setCheckedKeys([])
  }
}

const previewGeneration = () => {
  ElMessage.info('预览功能开发中...')
}

// 将知识点按分类分组的辅助方法
const groupKnowledgePointsByCategory = (knowledgePoints: any[], knowledgeBaseId: number) => {
  // 获取知识库名称
  const knowledgeBase = knowledgeBases.value.find(kb => kb.id === knowledgeBaseId)
  const baseName = knowledgeBase?.name || `知识库${knowledgeBaseId}`
  
  // 按难度级别分组
  const grouped = knowledgePoints.reduce((acc, point) => {
    const difficulty = point.difficulty || point.difficultyLevel || 'MEDIUM'
    if (!acc[difficulty]) {
      acc[difficulty] = []
    }
    acc[difficulty].push({
      id: point.id,
      title: point.title,
      difficulty: difficulty,
      content: point.content || point.summary,
      keywords: point.keywords,
      tags: point.tags
    })
    return acc
  }, {})
  
  // 构建树形结构
  const result: any[] = []
  
  // 按难度分组
  Object.keys(grouped).forEach(difficulty => {
    const difficultyLabel = {
      'EASY': '基础知识',
      'MEDIUM': '进阶知识', 
      'HARD': '高级知识'
    }[difficulty] || '其他知识'
    
    result.push({
      id: `${knowledgeBaseId}-${difficulty}`,
      title: `${baseName} - ${difficultyLabel}`,
      type: 'category',
      children: grouped[difficulty]
    })
  })
  
  return result
}
</script>

<style scoped>
.knowledge-based-generation {
  padding: 20px;
}

.section-card {
  margin-bottom: 20px;
}

.kb-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.kb-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.kb-name {
  font-weight: 600;
}

.kb-subject {
  font-size: 12px;
  color: #909399;
}

.kb-stats {
  display: flex;
  gap: 4px;
}

.knowledge-points-selector {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.selector-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #dcdfe6;
}

.selector-actions {
  display: flex;
  gap: 8px;
}

.knowledge-points-tree {
  max-height: 300px;
  overflow-y: auto;
  padding: 16px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.question-type-weights {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.weight-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.weight-label {
  min-width: 80px;
  font-size: 14px;
}

.weight-value {
  min-width: 40px;
  text-align: right;
  font-size: 14px;
  color: #606266;
}

.difficulty-distribution {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.difficulty-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.difficulty-item span:first-child {
  min-width: 40px;
}

.difficulty-item span:last-child {
  min-width: 40px;
  text-align: right;
}

.preview-details {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-section h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #606266;
}

.kb-list,
.kp-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.kb-tag {
  margin: 0;
}

.more-indicator {
  color: #909399;
  font-size: 12px;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 30px;
}
</style>
