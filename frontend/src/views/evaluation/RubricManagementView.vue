<template>
  <div class="rubric-management">
    <div class="page-header">
      <h1>批阅标准管理</h1>
      <p class="page-description">创建和管理AI批阅的评分标准</p>
    </div>

    <!-- 操作栏 -->
    <el-card class="toolbar">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索批阅标准名称或描述"
            @input="handleSearch"
            clearable
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="4">
          <el-select v-model="filterSubject" placeholder="学科筛选" clearable @change="loadRubrics">
            <el-option label="全部学科" value="" />
            <el-option label="语文" value="语文" />
            <el-option label="数学" value="数学" />
            <el-option label="英语" value="英语" />
            <el-option label="物理" value="物理" />
            <el-option label="化学" value="化学" />
            <el-option label="生物" value="生物" />
            <el-option label="历史" value="历史" />
            <el-option label="地理" value="地理" />
            <el-option label="政治" value="政治" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="filterStatus" placeholder="状态筛选" clearable @change="loadRubrics">
            <el-option label="全部状态" value="" />
            <el-option label="启用" value="true" />
            <el-option label="禁用" value="false" />
          </el-select>
        </el-col>
        <el-col :span="8" style="text-align: right;">
          <el-button type="primary" @click="showCreateDialog = true">
            <el-icon><Plus /></el-icon>
            创建批阅标准
          </el-button>
          <el-button @click="loadRubrics" :loading="loading">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 批阅标准列表 -->
    <el-card class="rubrics-card">
      <div v-if="loading" style="text-align: center; padding: 40px;">
        <el-icon :size="40"><Loading /></el-icon>
        <p>加载中...</p>
      </div>

      <div v-else-if="rubrics.length === 0" class="empty-state">
        <el-empty description="暂无批阅标准">
          <el-button type="primary" @click="showCreateDialog = true">
            创建第一个批阅标准
          </el-button>
        </el-empty>
      </div>

      <div v-else class="rubrics-list">
        <el-row :gutter="20">
          <el-col 
            v-for="rubric in filteredRubrics" 
            :key="rubric.id" 
            :xs="24" 
            :sm="12" 
            :lg="8" 
            :xl="6"
          >
            <el-card class="rubric-card" shadow="hover">
              <template #header>
                <div class="rubric-header">
                  <div class="rubric-title">
                    <h3>{{ rubric.name }}</h3>
                    <el-tag :type="rubric.isActive ? 'success' : 'info'" size="small">
                      {{ rubric.isActive ? '启用' : '禁用' }}
                    </el-tag>
                  </div>
                </div>
              </template>

              <div class="rubric-content">
                <div class="rubric-info">
                  <p class="subject">
                    <el-icon><Notebook /></el-icon>
                    {{ rubric.subject || '通用' }}
                  </p>
                  
                  <p class="description" v-if="rubric.description">
                    {{ rubric.description }}
                  </p>

                  <div class="criteria-summary">
                    <el-icon><List /></el-icon>
                    <span>{{ rubric.criteria?.length || 0 }} 个评分标准</span>
                  </div>

                  <div class="usage-stats" v-if="rubric.usageCount !== undefined">
                    <el-icon><TrendCharts /></el-icon>
                    <span>使用次数: {{ rubric.usageCount }}</span>
                  </div>
                </div>

                <div class="rubric-actions">
                  <el-button size="small" @click="viewRubric(rubric)">
                    预览
                  </el-button>
                  <el-button size="small" type="primary" @click="editRubric(rubric)">
                    编辑
                  </el-button>
                  <el-button 
                    size="small" 
                    :type="rubric.isActive ? 'warning' : 'success'"
                    @click="toggleRubricStatus(rubric)"
                  >
                    {{ rubric.isActive ? '禁用' : '启用' }}
                  </el-button>
                  <el-button 
                    size="small" 
                    type="danger" 
                    @click="deleteRubric(rubric)"
                  >
                    删除
                  </el-button>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 创建/编辑对话框 -->
    <RubricDialog
      v-model="showCreateDialog"
      :rubric-data="editingRubric"
      @success="handleRubricSuccess"
    />

    <!-- 预览对话框 -->
    <RubricPreviewDialog
      v-model="showPreviewDialog"
      :rubric-data="previewingRubric"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Search, Plus, Refresh, Loading, Notebook, 
  List, TrendCharts 
} from '@element-plus/icons-vue'
import { rubricApi } from '@/api/rubric'
import type { Rubric } from '@/types/api'
import RubricDialog from '@/components/evaluation/RubricDialog.vue'
import RubricPreviewDialog from '@/components/evaluation/RubricPreviewDialog.vue'

// 状态管理
const loading = ref(false)
const showCreateDialog = ref(false)
const showPreviewDialog = ref(false)
const editingRubric = ref<Rubric | null>(null)
const previewingRubric = ref<Rubric | null>(null)

// 数据
const rubrics = ref<Rubric[]>([])

// 搜索和过滤
const searchKeyword = ref('')
const filterSubject = ref('')
const filterStatus = ref('')

// 计算属性 - 过滤后的批阅标准
const filteredRubrics = computed(() => {
  let filtered = rubrics.value

  // 关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    filtered = filtered.filter(rubric => 
      rubric.name.toLowerCase().includes(keyword) ||
      (rubric.description && rubric.description.toLowerCase().includes(keyword))
    )
  }

  // 学科过滤
  if (filterSubject.value) {
    filtered = filtered.filter(rubric => rubric.subject === filterSubject.value)
  }

  // 状态过滤
  if (filterStatus.value !== '') {
    const isActive = filterStatus.value === 'true'
    filtered = filtered.filter(rubric => rubric.isActive === isActive)
  }

  return filtered
})

// 方法
const loadRubrics = async () => {
  loading.value = true
  try {
    rubrics.value = await rubricApi.getAllRubrics()
  } catch (error) {
    console.error('加载批阅标准失败:', error)
    ElMessage.error('加载批阅标准失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  // 搜索逻辑在计算属性中处理
}

const viewRubric = (rubric: Rubric) => {
  previewingRubric.value = rubric
  showPreviewDialog.value = true
}

const editRubric = (rubric: Rubric) => {
  editingRubric.value = rubric
  showCreateDialog.value = true
}

const handlePreviewEdit = (rubric: Rubric) => {
  showPreviewDialog.value = false
  editRubric(rubric)
}

const toggleRubricStatus = async (rubric: Rubric) => {
  const action = rubric.isActive ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}批阅标准 "${rubric.name}" 吗？`,
      `确认${action}`,
      {
        type: 'warning'
      }
    )

    await rubricApi.toggleRubricStatus(rubric.id, !rubric.isActive)
    rubric.isActive = !rubric.isActive
    ElMessage.success(`批阅标准已${action}`)
  } catch (error) {
    if (error !== 'cancel') {
      console.error(`${action}批阅标准失败:`, error)
      ElMessage.error(`${action}失败`)
    }
  }
}

const deleteRubric = async (rubric: Rubric) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除批阅标准 "${rubric.name}" 吗？此操作不可恢复。`,
      '确认删除',
      {
        type: 'warning'
      }
    )

    await rubricApi.deleteRubric(rubric.id)
    await loadRubrics()
    ElMessage.success('批阅标准已删除')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除批阅标准失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleRubricSuccess = async () => {
  await loadRubrics()
  showCreateDialog.value = false
  editingRubric.value = null
}

onMounted(() => {
  loadRubrics()
})
</script>

<style scoped>
.rubric-management {
  max-width: 1400px;
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

.toolbar {
  margin-bottom: 24px;
}

.rubrics-card {
  background: white;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
}

.rubrics-list {
  margin-top: 20px;
}

.rubric-card {
  margin-bottom: 20px;
  transition: transform 0.2s;
  height: 280px;
}

.rubric-card:hover {
  transform: translateY(-2px);
}

.rubric-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.rubric-title {
  flex: 1;
}

.rubric-title h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rubric-content {
  display: flex;
  flex-direction: column;
  height: 180px;
}

.rubric-info {
  flex: 1;
  padding: 10px 0;
}

.subject {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #409eff;
  font-weight: 500;
}

.description {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #666;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.criteria-summary {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 13px;
  color: #909399;
}

.usage-stats {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #909399;
}

.rubric-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.rubric-actions .el-button {
  flex: 1;
  min-width: 60px;
}
</style>