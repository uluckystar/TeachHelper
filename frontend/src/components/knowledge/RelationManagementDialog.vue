<template>
  <el-dialog
    v-model="visible"
    :title="`管理关联关系 - ${knowledgePoint?.title || ''}`"
    width="900px"
    :close-on-click-modal="false"
    @closed="resetForm"
  >
    <div class="relation-management" v-if="knowledgePoint">
      <!-- 当前关联关系 -->
      <el-card class="current-relations-card" shadow="never">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span>当前关联关系 ({{ currentRelations.length }})</span>
            <el-button size="small" @click="showAddRelation = true">添加关联</el-button>
          </div>
        </template>

        <div v-if="currentRelations.length === 0" class="empty-state">
          <el-empty description="暂无关联关系" />
        </div>

        <div v-else class="relations-list">
          <div
            v-for="relation in currentRelations"
            :key="relation.id"
            class="relation-item"
          >
            <div class="relation-info">
              <div class="relation-point">
                <span class="point-title">{{ relation.targetPoint.title }}</span>
                <el-tag :type="getCategoryTagType(relation.targetPoint.category)" size="small">
                  {{ relation.targetPoint.category }}
                </el-tag>
              </div>
              <div class="relation-meta">
                <el-tag :type="getRelationTypeTag(relation.type)" size="small">
                  {{ getRelationTypeText(relation.type) }}
                </el-tag>
                <el-tag type="info" size="small">
                  强度: {{ relation.strength }}
                </el-tag>
              </div>
            </div>
            
            <div class="relation-description" v-if="relation.description">
              {{ relation.description }}
            </div>
            
            <div class="relation-actions">
              <el-button size="small" @click="editRelation(relation)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteRelation(relation)">删除</el-button>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 推荐关联 -->
      <el-card class="recommended-relations-card" shadow="never">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span>AI推荐关联 ({{ recommendedRelations.length }})</span>
            <div>
              <el-button size="small" @click="refreshRecommendations">刷新推荐</el-button>
              <el-button size="small" @click="batchAddRecommended">批量添加</el-button>
            </div>
          </div>
        </template>

        <div v-if="loadingRecommendations" class="loading-state">
          <el-skeleton :rows="3" animated />
        </div>

        <div v-else-if="recommendedRelations.length === 0" class="empty-state">
          <el-empty description="暂无推荐关联" />
        </div>

        <div v-else class="recommendations-list">
          <div
            v-for="recommendation in recommendedRelations"
            :key="recommendation.id"
            class="recommendation-item"
          >
            <div class="recommendation-header">
              <el-checkbox
                v-model="recommendation.selected"
                @change="updateRecommendationSelection"
              />
              <div class="recommendation-info">
                <span class="point-title">{{ recommendation.targetPoint.title }}</span>
                <div class="recommendation-meta">
                  <el-tag :type="getRelationTypeTag(recommendation.suggestedType)" size="small">
                    {{ getRelationTypeText(recommendation.suggestedType) }}
                  </el-tag>
                  <el-tag type="success" size="small">
                    置信度: {{ recommendation.confidence }}%
                  </el-tag>
                </div>
              </div>
            </div>
            
            <div class="recommendation-reason">
              <strong>推荐理由：</strong>{{ recommendation.reason }}
            </div>
            
            <div class="recommendation-actions">
              <el-button size="small" @click="addRecommendedRelation(recommendation)">
                添加关联
              </el-button>
              <el-button size="small" @click="ignoreRecommendation(recommendation)">
                忽略
              </el-button>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 关联图谱 -->
      <el-card class="relation-graph-card" shadow="never">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span>关联图谱</span>
            <div>
              <el-button size="small" @click="toggleGraphLayout">切换布局</el-button>
              <el-button size="small" @click="exportGraph">导出图谱</el-button>
            </div>
          </div>
        </template>

        <div class="graph-container" ref="graphContainer">
          <div class="graph-placeholder">
            <el-icon size="64" color="#c0c4cc">
              <Share />
            </el-icon>
            <div>关联图谱</div>
            <div style="font-size: 12px; color: #999;">显示知识点之间的关联关系</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 添加关联对话框 -->
    <el-dialog
      v-model="showAddRelation"
      title="添加关联关系"
      width="600px"
      append-to-body
    >
      <el-form :model="newRelation" :rules="relationRules" ref="relationFormRef" label-width="120px">
        <el-form-item label="目标知识点" prop="targetPointId">
          <el-select
            v-model="newRelation.targetPointId"
            filterable
            placeholder="搜索并选择知识点"
            style="width: 100%"
          >
            <el-option
              v-for="point in availablePoints"
              :key="point.id"
              :label="point.title"
              :value="point.id"
              :disabled="point.id === knowledgePoint?.id"
            >
              <div style="display: flex; justify-content: space-between;">
                <span>{{ point.title }}</span>
                <el-tag :type="getCategoryTagType(point.category)" size="small">
                  {{ point.category }}
                </el-tag>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="关联类型" prop="type">
          <el-select v-model="newRelation.type" placeholder="选择关联类型" style="width: 100%">
            <el-option label="前置知识" value="prerequisite">
              <div>
                <div>前置知识</div>
                <div style="font-size: 12px; color: #999;">学习当前知识点前需要掌握的</div>
              </div>
            </el-option>
            <el-option label="相关知识" value="related">
              <div>
                <div>相关知识</div>
                <div style="font-size: 12px; color: #999;">与当前知识点相关的</div>
              </div>
            </el-option>
            <el-option label="扩展知识" value="extension">
              <div>
                <div>扩展知识</div>
                <div style="font-size: 12px; color: #999;">基于当前知识点的进一步学习</div>
              </div>
            </el-option>
            <el-option label="应用实例" value="application">
              <div>
                <div>应用实例</div>
                <div style="font-size: 12px; color: #999;">当前知识点的具体应用</div>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="关联强度" prop="strength">
          <el-rate
            v-model="newRelation.strength"
            :max="5"
            show-text
            :texts="['很弱', '较弱', '一般', '较强', '很强']"
          />
        </el-form-item>

        <el-form-item label="关联描述">
          <el-input
            v-model="newRelation.description"
            type="textarea"
            :rows="3"
            placeholder="描述这两个知识点之间的关联关系"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showAddRelation = false">取消</el-button>
          <el-button type="primary" @click="addRelation" :loading="addingRelation">
            添加关联
          </el-button>
        </div>
      </template>
    </el-dialog>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">关闭</el-button>
        <el-button type="primary" @click="saveChanges">保存变更</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Share } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

// Props
const props = defineProps<{
  modelValue: boolean
  knowledgePoint: any
  allPoints: any[]
}>()

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  updated: []
}>()

// 响应式数据
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const currentRelations = ref<any[]>([])
const recommendedRelations = ref<any[]>([])
const loadingRecommendations = ref(false)
const showAddRelation = ref(false)
const addingRelation = ref(false)
const relationFormRef = ref<FormInstance>()
const graphContainer = ref<HTMLElement>()

// 新建关联表单
const newRelation = ref({
  targetPointId: 0,
  type: '',
  strength: 3,
  description: ''
})

// 表单验证规则
const relationRules: FormRules = {
  targetPointId: [
    { required: true, message: '请选择目标知识点', trigger: 'change' }
  ],
  type: [
    { required: true, message: '请选择关联类型', trigger: 'change' }
  ],
  strength: [
    { required: true, message: '请设置关联强度', trigger: 'change' }
  ]
}

// 计算属性
const availablePoints = computed(() => {
  return props.allPoints.filter(point => point.id !== props.knowledgePoint?.id)
})

// 监听知识点变化
watch(() => props.knowledgePoint, (newPoint) => {
  if (newPoint && visible.value) {
    loadRelations()
    loadRecommendations()
  }
}, { immediate: true })

// 生命周期
onMounted(() => {
  if (props.knowledgePoint) {
    loadRelations()
    loadRecommendations()
  }
})

// 方法
const loadRelations = async () => {
  try {
    // TODO: 从API加载当前关联关系
    // 模拟数据
    currentRelations.value = [
      {
        id: 1,
        targetPoint: {
          id: 2,
          title: '函数的性质',
          category: '原理公式'
        },
        type: 'extension',
        strength: 4,
        description: '掌握函数定义后可以进一步学习函数性质'
      },
      {
        id: 2,
        targetPoint: {
          id: 3,
          title: '数学集合',
          category: '概念定义'
        },
        type: 'prerequisite',
        strength: 5,
        description: '函数的定义需要集合的概念作为基础'
      }
    ]
  } catch (error) {
    console.error('Load relations failed:', error)
  }
}

const loadRecommendations = async () => {
  try {
    loadingRecommendations.value = true
    
    // TODO: 调用AI API获取推荐关联
    await new Promise(resolve => setTimeout(resolve, 1500)) // 模拟API调用
    
    recommendedRelations.value = [
      {
        id: 1,
        targetPoint: {
          id: 4,
          title: '函数图像',
          category: '解题方法'
        },
        suggestedType: 'related',
        confidence: 92,
        reason: '函数定义和函数图像密切相关，理解定义有助于分析图像特征',
        selected: false
      },
      {
        id: 2,
        targetPoint: {
          id: 5,
          title: '复合函数',
          category: '综合应用'
        },
        suggestedType: 'extension',
        confidence: 88,
        reason: '复合函数是函数概念的扩展应用，需要先掌握基本函数定义',
        selected: false
      },
      {
        id: 3,
        targetPoint: {
          id: 6,
          title: '函数运算',
          category: '解题方法'
        },
        suggestedType: 'application',
        confidence: 85,
        reason: '函数运算是函数定义的具体应用，包括加减乘除等操作',
        selected: false
      }
    ]
  } catch (error) {
    console.error('Load recommendations failed:', error)
    ElMessage.error('加载推荐关联失败')
  } finally {
    loadingRecommendations.value = false
  }
}

const refreshRecommendations = async () => {
  await loadRecommendations()
  ElMessage.success('推荐关联已刷新')
}

const updateRecommendationSelection = () => {
  // 更新选择状态
}

const batchAddRecommended = async () => {
  const selected = recommendedRelations.value.filter(rec => rec.selected)
  if (selected.length === 0) {
    ElMessage.warning('请先选择要添加的推荐关联')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要添加 ${selected.length} 个推荐关联吗？`,
      '批量添加关联',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    // TODO: 调用API批量添加关联
    for (const rec of selected) {
      const newRel = {
        id: Date.now() + Math.random(),
        targetPoint: rec.targetPoint,
        type: rec.suggestedType,
        strength: 4,
        description: rec.reason
      }
      currentRelations.value.push(newRel)
    }
    
    // 移除已添加的推荐
    recommendedRelations.value = recommendedRelations.value.filter(rec => !rec.selected)
    
    ElMessage.success(`成功添加 ${selected.length} 个关联关系`)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量添加失败')
      console.error('Batch add failed:', error)
    }
  }
}

const addRecommendedRelation = async (recommendation: any) => {
  try {
    // TODO: 调用API添加关联
    const newRelation = {
      id: Date.now(),
      targetPoint: recommendation.targetPoint,
      type: recommendation.suggestedType,
      strength: 4,
      description: recommendation.reason
    }
    
    currentRelations.value.push(newRelation)
    
    // 从推荐列表中移除
    const index = recommendedRelations.value.findIndex(rec => rec.id === recommendation.id)
    if (index > -1) {
      recommendedRelations.value.splice(index, 1)
    }
    
    ElMessage.success('关联关系添加成功')
  } catch (error) {
    ElMessage.error('添加关联失败')
    console.error('Add recommended relation failed:', error)
  }
}

const ignoreRecommendation = (recommendation: any) => {
  const index = recommendedRelations.value.findIndex(rec => rec.id === recommendation.id)
  if (index > -1) {
    recommendedRelations.value.splice(index, 1)
    ElMessage.info('已忽略此推荐')
  }
}

const addRelation = async () => {
  if (!relationFormRef.value) return

  try {
    await relationFormRef.value.validate()
    addingRelation.value = true

    const targetPoint = availablePoints.value.find(point => point.id === newRelation.value.targetPointId)
    if (!targetPoint) {
      ElMessage.error('目标知识点不存在')
      return
    }

    // TODO: 调用API添加关联
    const relation = {
      id: Date.now(),
      targetPoint,
      type: newRelation.value.type,
      strength: newRelation.value.strength,
      description: newRelation.value.description
    }

    currentRelations.value.push(relation)
    showAddRelation.value = false
    resetNewRelation()
    
    ElMessage.success('关联关系添加成功')
  } catch (error) {
    console.error('Add relation failed:', error)
    ElMessage.error('添加关联失败')
  } finally {
    addingRelation.value = false
  }
}

const editRelation = (relation: any) => {
  ElMessage.info('编辑关联功能开发中...')
}

const deleteRelation = async (relation: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除与"${relation.targetPoint.title}"的关联关系吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const index = currentRelations.value.findIndex(rel => rel.id === relation.id)
    if (index > -1) {
      currentRelations.value.splice(index, 1)
      ElMessage.success('关联关系删除成功')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除关联失败')
      console.error('Delete relation failed:', error)
    }
  }
}

const toggleGraphLayout = () => {
  ElMessage.info('切换布局功能开发中...')
}

const exportGraph = () => {
  ElMessage.info('导出图谱功能开发中...')
}

const saveChanges = async () => {
  try {
    // TODO: 调用API保存所有变更
    console.log('Saving relation changes:', currentRelations.value)
    
    ElMessage.success('关联关系保存成功')
    emit('updated')
    visible.value = false
  } catch (error) {
    ElMessage.error('保存失败')
    console.error('Save changes failed:', error)
  }
}

const resetForm = () => {
  resetNewRelation()
  showAddRelation.value = false
}

const resetNewRelation = () => {
  newRelation.value = {
    targetPointId: 0,
    type: '',
    strength: 3,
    description: ''
  }
  
  if (relationFormRef.value) {
    relationFormRef.value.resetFields()
  }
}

// 工具方法
const getCategoryTagType = (category: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    '概念定义': 'primary',
    '原理公式': 'success',
    '解题方法': 'warning',
    '实例分析': 'info',
    '综合应用': 'danger'
  }
  return typeMap[category] || 'info'
}

const getRelationTypeTag = (type: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    'prerequisite': 'primary',
    'related': 'success',
    'extension': 'warning',
    'application': 'info'
  }
  return typeMap[type] || 'info'
}

const getRelationTypeText = (type: string) => {
  const textMap: Record<string, string> = {
    'prerequisite': '前置知识',
    'related': '相关知识',
    'extension': '扩展知识',
    'application': '应用实例'
  }
  return textMap[type] || type
}
</script>

<style scoped>
.relation-management {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.current-relations-card,
.recommended-relations-card,
.relation-graph-card {
  border: 1px solid #e4e7ed;
}

.empty-state {
  padding: 20px;
  text-align: center;
}

.loading-state {
  padding: 20px;
}

.relations-list,
.recommendations-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.relation-item,
.recommendation-item {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 16px;
  background: #fafafa;
}

.relation-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.relation-point {
  display: flex;
  align-items: center;
  gap: 8px;
}

.point-title {
  font-weight: 500;
  color: #303133;
}

.relation-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.relation-description {
  color: #606266;
  line-height: 1.6;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.relation-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.recommendation-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 8px;
}

.recommendation-info {
  flex: 1;
}

.recommendation-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.recommendation-reason {
  color: #606266;
  line-height: 1.6;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #f0f9ff;
  border-radius: 4px;
  border-left: 3px solid #409eff;
}

.recommendation-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.graph-container {
  height: 300px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fafafa;
}

.graph-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #c0c4cc;
  font-size: 14px;
  gap: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
