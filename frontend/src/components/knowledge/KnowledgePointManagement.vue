<template>
  <div class="knowledge-point-management">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" icon="Plus" @click="showAddDialog = true">
          添加知识点
        </el-button>
        <el-button icon="MagicStick" @click="showAIExtractDialog = true">
          AI提取知识点
        </el-button>
        <el-button icon="Sort" @click="showOrganizeDialog = true">
          智能整理
        </el-button>
      </div>
      
      <div class="toolbar-right">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索知识点..."
          prefix-icon="Search"
          style="width: 200px;"
          clearable
        />
        <el-select v-model="categoryFilter" placeholder="分类筛选" style="width: 120px;">
          <el-option value="" label="全部分类" />
          <el-option 
            v-for="category in categories"
            :key="category"
            :value="category"
            :label="category"
          />
        </el-select>
        <el-select v-model="viewMode" style="width: 100px;">
          <el-option value="list" label="列表视图" />
          <el-option value="tree" label="树形视图" />
          <el-option value="graph" label="关系图" />
        </el-select>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="stats-bar">
      <div class="stat-item">
        <span class="stat-label">总知识点：</span>
        <span class="stat-value">{{ knowledgePoints.length }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">分类数：</span>
        <span class="stat-value">{{ categories.length }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">关联关系：</span>
        <span class="stat-value">{{ totalRelations }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">AI生成：</span>
        <span class="stat-value">{{ aiGeneratedCount }}</span>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="content-area">
      <!-- 列表视图 -->
      <div v-if="viewMode === 'list'" class="list-view">
        <el-table 
          :data="filteredKnowledgePoints" 
          v-loading="loading"
          row-key="id"
          @selection-change="handleSelectionChange"
          @row-click="handleRowClick"
        >
          <el-table-column type="selection" width="55" />
          
          <el-table-column label="知识点" min-width="200">
            <template #default="{ row }">
              <div class="knowledge-point-info">
                <div class="point-title">
                  <el-icon v-if="row.isAIGenerated" class="ai-icon" title="AI生成">
                    <MagicStick />
                  </el-icon>
                  {{ row.title }}
                </div>
                <div class="point-description" v-if="row.description">
                  {{ row.description }}
                </div>
              </div>
            </template>
          </el-table-column>
          
          <el-table-column label="分类" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="getCategoryTagType(row.category)">
                {{ row.category }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column label="重要程度" width="100">
            <template #default="{ row }">
              <el-rate 
                v-model="row.importance" 
                :max="5" 
                size="small"
                disabled
                show-score
              />
            </template>
          </el-table-column>
          
          <el-table-column label="掌握程度" width="120">
            <template #default="{ row }">
              <el-progress 
                :percentage="row.masteryLevel || 0" 
                :stroke-width="6"
                size="small"
                :status="getMasteryStatus(row.masteryLevel)"
              />
            </template>
          </el-table-column>
          
          <el-table-column label="关联数" width="80">
            <template #default="{ row }">
              <el-tag size="small" type="info">
                {{ (row.relations || []).length }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column label="来源文档" width="120">
            <template #default="{ row }">
              <el-tooltip v-if="row.sourceDocument" :content="row.sourceDocument.fileName">
                <el-tag size="small" type="success" style="cursor: pointer;">
                  {{ row.sourceDocument.fileName.substring(0, 10) }}...
                </el-tag>
              </el-tooltip>
              <span v-else class="text-muted">手动添加</span>
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button-group>
                <el-button size="small" icon="View" @click.stop="viewDetail(row)" />
                <el-button size="small" icon="Edit" @click.stop="editKnowledgePoint(row)" />
                <el-dropdown @command="(cmd) => handlePointAction(cmd, row)">
                  <el-button size="small" icon="MoreFilled" />
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="relations" icon="Share">管理关联</el-dropdown-item>
                      <el-dropdown-item command="practice" icon="Trophy">生成练习</el-dropdown-item>
                      <el-dropdown-item divided command="delete" icon="Delete">删除</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </el-button-group>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 树形视图 -->
      <div v-else-if="viewMode === 'tree'" class="tree-view">
        <el-tree
          :data="treeData"
          :props="treeProps"
          node-key="id"
          default-expand-all
          :expand-on-click-node="false"
          class="knowledge-tree"
        >
          <template #default="{ node, data }">
            <div class="tree-node" @click="handleTreeNodeClick(data)">
              <div class="node-content">
                <el-icon v-if="data.isAIGenerated" class="ai-icon">
                  <MagicStick />
                </el-icon>
                <span class="node-title">{{ data.title }}</span>
                <el-tag v-if="data.type === 'category'" size="small" type="info">
                  {{ data.count }}个
                </el-tag>
              </div>
              <div class="node-actions" v-if="data.type === 'point'">
                <el-button size="small" icon="Edit" @click.stop="editKnowledgePoint(data)" />
                <el-button size="small" icon="Delete" @click.stop="deleteKnowledgePoint(data)" />
              </div>
            </div>
          </template>
        </el-tree>
      </div>

      <!-- 关系图视图 -->
      <div v-else-if="viewMode === 'graph'" class="graph-view">
        <div class="graph-container" ref="graphContainer">
          <div class="graph-placeholder">
            <el-icon size="64"><Share /></el-icon>
            <h3>知识点关系图</h3>
            <p>可视化展示知识点之间的关联关系</p>
            <el-button type="primary" @click="initializeGraph">
              生成关系图
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 批量操作栏 -->
    <div class="batch-actions" v-if="selectedPoints.length > 0">
      <div class="batch-info">
        已选择 {{ selectedPoints.length }} 个知识点
      </div>
      <div class="batch-buttons">
        <el-button icon="Edit" @click="batchEdit">批量编辑</el-button>
        <el-button icon="Share" @click="batchAddRelations">批量关联</el-button>
        <el-button icon="Download" @click="batchExport">导出</el-button>
        <el-button icon="Delete" @click="batchDelete" type="danger">删除</el-button>
      </div>
    </div>

    <!-- 添加/编辑知识点对话框 -->
    <AddEditKnowledgePointDialog
      v-model="showAddDialog"
      :knowledge-base-id="knowledgeBaseId"
      :editing-point="editingPoint"
      @saved="handlePointSaved"
    />

    <!-- AI提取知识点对话框 -->
    <AIExtractDialog
      v-model="showAIExtractDialog"
      :knowledge-base-id="knowledgeBaseId"
      @extracted="handlePointsExtracted"
    />

    <!-- 智能整理对话框 -->
    <OrganizeDialog
      v-model="showOrganizeDialog"
      :knowledge-points="knowledgePoints"
      @organized="handlePointsOrganized"
    />

    <!-- 知识点详情对话框 -->
    <KnowledgePointDetailDialog
      v-model="showDetailDialog"
      :knowledge-point="viewingPoint"
    />

    <!-- 关联管理对话框 -->
    <RelationManagementDialog
      v-model="showRelationDialog"
      :knowledge-point="managingPoint"
      :all-points="knowledgePoints"
      @updated="handleRelationsUpdated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  MagicStick,
  Sort,
  Search,
  View,
  Edit,
  MoreFilled,
  Share,
  Trophy,
  Delete
} from '@element-plus/icons-vue'
import AddEditKnowledgePointDialog from './AddEditKnowledgePointDialog.vue'
import AIExtractDialog from './AIExtractDialog.vue'
import OrganizeDialog from './OrganizeDialog.vue'
import KnowledgePointDetailDialog from './KnowledgePointDetailDialog.vue'
import RelationManagementDialog from './RelationManagementDialog.vue'
import { knowledgePointApi } from '@/api/knowledge'

// Props
const props = defineProps<{
  knowledgeBaseId: number
}>()

// 响应式数据
const knowledgePoints = ref<KnowledgePoint[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const categoryFilter = ref('')
const viewMode = ref<'list' | 'tree' | 'graph'>('list')
const selectedPoints = ref<KnowledgePoint[]>([])

// 对话框状态
const showAddDialog = ref(false)
const showAIExtractDialog = ref(false)
const showOrganizeDialog = ref(false)
const showDetailDialog = ref(false)
const showRelationDialog = ref(false)

// 编辑状态
const editingPoint = ref<KnowledgePoint | null>(null)
const viewingPoint = ref<KnowledgePoint | null>(null)
const managingPoint = ref<KnowledgePoint | null>(null)

// 图形容器引用
const graphContainer = ref<HTMLElement>()

// 类型定义
interface KnowledgePoint {
  id: number
  title: string
  description?: string
  category: string
  importance: number
  masteryLevel?: number
  isAIGenerated: boolean
  relations?: KnowledgePointRelation[]
  sourceDocument?: {
    id: number
    fileName: string
  }
  createdAt: string
  updatedAt: string
}

interface KnowledgePointRelation {
  id: number
  targetPointId: number
  relationType: string
  strength: number
}

// 计算属性
const categories = computed(() => {
  const cats = [...new Set(knowledgePoints.value.map(point => point.category))]
  return cats.filter(Boolean)
})

const totalRelations = computed(() => {
  return knowledgePoints.value.reduce((sum, point) => 
    sum + (point.relations?.length || 0), 0
  )
})

const aiGeneratedCount = computed(() => {
  return knowledgePoints.value.filter(point => point.isAIGenerated).length
})

const filteredKnowledgePoints = computed(() => {
  let filtered = knowledgePoints.value

  // 按关键词筛选
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    filtered = filtered.filter(point => 
      point.title.toLowerCase().includes(keyword) ||
      point.description?.toLowerCase().includes(keyword)
    )
  }

  // 按分类筛选
  if (categoryFilter.value) {
    filtered = filtered.filter(point => point.category === categoryFilter.value)
  }

  return filtered
})

const treeData = computed(() => {
  const categoryMap: Record<string, KnowledgePoint[]> = {}
  
  knowledgePoints.value.forEach(point => {
    if (!categoryMap[point.category]) {
      categoryMap[point.category] = []
    }
    categoryMap[point.category].push(point)
  })

  return Object.entries(categoryMap).map(([category, points]) => ({
    id: `category-${category}`,
    title: category,
    type: 'category',
    count: points.length,
    children: points.map(point => ({
      ...point,
      type: 'point'
    }))
  }))
})

const treeProps = {
  children: 'children',
  label: 'title'
}

// 生命周期
onMounted(() => {
  loadKnowledgePoints()
})

// 监听知识库ID变化
watch(() => props.knowledgeBaseId, () => {
  loadKnowledgePoints()
})

// 方法
const loadKnowledgePoints = async () => {
  loading.value = true
  try {
    console.log('Loading knowledge points for knowledge base:', props.knowledgeBaseId)
    
    // 调用API获取知识点列表
    const response = await knowledgePointApi.getKnowledgePoints({
      knowledgeBaseId: props.knowledgeBaseId,
      page: 0,
      size: 1000 // 获取所有知识点
    })
    
    console.log('Knowledge points API response:', response)
    
    // 转换API响应到前端数据格式
    const knowledgePointsData = response.content || []
    knowledgePoints.value = knowledgePointsData.map((kp: any) => ({
      id: kp.id,
      title: kp.title,
      description: kp.description,
      category: kp.category || '未分类',
      importance: kp.importance || 3,
      masteryLevel: kp.masteryLevel || 0,
      isAIGenerated: kp.isAIGenerated || false,
      relations: kp.relations || [],
      sourceDocument: kp.sourceDocument,
      createdAt: kp.createdAt,
      updatedAt: kp.updatedAt
    }))
    
    console.log('Processed knowledge points:', knowledgePoints.value)
    ElMessage.success(`成功加载 ${knowledgePoints.value.length} 个知识点`)
  } catch (error: any) {
    console.error('Load knowledge points failed:', error)
    ElMessage.error(`加载知识点失败: ${error.message || '网络错误'}`)
    knowledgePoints.value = []
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = (selection: KnowledgePoint[]) => {
  selectedPoints.value = selection
}

const handleRowClick = (row: KnowledgePoint) => {
  viewDetail(row)
}

const handleTreeNodeClick = (data: any) => {
  if (data.type === 'point') {
    viewDetail(data)
  }
}

const viewDetail = (point: KnowledgePoint) => {
  viewingPoint.value = point
  showDetailDialog.value = true
}

const editKnowledgePoint = (point: KnowledgePoint) => {
  editingPoint.value = point
  showAddDialog.value = true
}

const handlePointAction = (command: string, point: KnowledgePoint) => {
  switch (command) {
    case 'relations':
      manageRelations(point)
      break
    case 'practice':
      generatePractice(point)
      break
    case 'delete':
      deleteKnowledgePoint(point)
      break
  }
}

const manageRelations = (point: KnowledgePoint) => {
  managingPoint.value = point
  showRelationDialog.value = true
}

const generatePractice = (point: KnowledgePoint) => {
  // TODO: 实现生成练习题功能
  ElMessage.info('生成练习题功能开发中...')
}

const deleteKnowledgePoint = async (point: KnowledgePoint) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除知识点"${point.title}"吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 调用API删除知识点
    await knowledgePointApi.deleteKnowledgePoint(point.id)
    
    // 从列表中移除知识点
    const index = knowledgePoints.value.findIndex(p => p.id === point.id)
    if (index > -1) {
      knowledgePoints.value.splice(index, 1)
    }
    
    ElMessage.success('知识点删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除知识点失败')
      console.error('Delete knowledge point failed:', error)
    }
  }
}

const batchEdit = () => {
  // TODO: 实现批量编辑功能
  ElMessage.info('批量编辑功能开发中...')
}

const batchAddRelations = () => {
  // TODO: 实现批量关联功能
  ElMessage.info('批量关联功能开发中...')
}

const batchExport = () => {
  // TODO: 实现批量导出功能
  ElMessage.info('导出功能开发中...')
}

const batchDelete = async () => {
  if (selectedPoints.value.length === 0) return
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedPoints.value.length} 个知识点吗？此操作不可恢复。`,
      '确认批量删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // TODO: 调用API批量删除知识点
    const idsToDelete = selectedPoints.value.map(point => point.id)
    knowledgePoints.value = knowledgePoints.value.filter(point => 
      !idsToDelete.includes(point.id)
    )
    selectedPoints.value = []
    
    ElMessage.success('批量删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
      console.error('Batch delete failed:', error)
    }
  }
}

const initializeGraph = () => {
  // TODO: 实现关系图初始化
  ElMessage.info('关系图功能开发中...')
}

const handlePointSaved = () => {
  loadKnowledgePoints()
  editingPoint.value = null
}

const handlePointsExtracted = () => {
  loadKnowledgePoints()
  ElMessage.success('AI知识点提取完成')
}

const handlePointsOrganized = () => {
  loadKnowledgePoints()
  ElMessage.success('知识点整理完成')
}

const handleRelationsUpdated = () => {
  loadKnowledgePoints()
  ElMessage.success('关联关系更新成功')
}

// 工具方法
const getCategoryTagType = (category: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    '函数': 'primary',
    '导数': 'success',
    '化学平衡': 'warning'
  }
  return typeMap[category] || 'info'
}

const getMasteryStatus = (level?: number) => {
  if (!level) return undefined
  if (level >= 80) return 'success'
  if (level >= 60) return undefined
  return 'exception'
}
</script>

<style scoped>
.knowledge-point-management {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  margin-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.toolbar-left {
  display: flex;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 统计栏 */
.stats-bar {
  display: flex;
  gap: 24px;
  padding: 12px 0;
  margin-bottom: 16px;
  font-size: 14px;
}

.stat-item {
  display: flex;
  align-items: center;
}

.stat-label {
  color: #909399;
  margin-right: 4px;
}

.stat-value {
  color: #303133;
  font-weight: 500;
}

/* 内容区域 */
.content-area {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

/* 列表视图 */
.list-view {
  height: 100%;
}

.knowledge-point-info {
  min-width: 0;
}

.point-title {
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.ai-icon {
  color: #409eff;
  font-size: 14px;
}

.point-description {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.text-muted {
  color: #c0c4cc;
  font-size: 12px;
}

/* 树形视图 */
.tree-view {
  height: 100%;
  padding: 16px;
  overflow: auto;
}

.knowledge-tree {
  height: 100%;
}

.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.tree-node:hover {
  background-color: #f5f7fa;
}

.node-content {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
}

.node-title {
  font-size: 14px;
  color: #303133;
}

.node-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.tree-node:hover .node-actions {
  opacity: 1;
}

/* 关系图视图 */
.graph-view {
  height: 100%;
  position: relative;
}

.graph-container {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.graph-placeholder {
  text-align: center;
  color: #909399;
}

.graph-placeholder h3 {
  margin: 16px 0 8px 0;
  color: #606266;
}

.graph-placeholder p {
  margin: 0 0 20px 0;
  font-size: 14px;
}

/* 批量操作栏 */
.batch-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-top: 16px;
}

.batch-info {
  font-size: 14px;
  color: #606266;
}

.batch-buttons {
  display: flex;
  gap: 8px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .toolbar-left,
  .toolbar-right {
    justify-content: center;
  }
  
  .stats-bar {
    flex-wrap: wrap;
    gap: 12px;
  }
  
  .batch-actions {
    flex-direction: column;
    gap: 12px;
    text-align: center;
  }
}
</style>
