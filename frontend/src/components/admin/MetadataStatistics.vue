<template>
  <div class="metadata-statistics">
    <el-row :gutter="24">
      <!-- 统计卡片 -->
      <el-col :span="6" v-for="stat in statistics" :key="stat.title">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" :class="stat.color">
              <el-icon :size="24">
                <component :is="stat.icon" />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-title">{{ stat.title }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 详细统计表格 -->
    <el-row :gutter="24" style="margin-top: 24px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>学科使用统计</span>
            <el-button
              style="float: right; padding: 3px 0"
              type="text"
              @click="refreshSubjectStats"
              :loading="loadingSubjects"
            >
              刷新
            </el-button>
          </template>
          
          <el-table :data="subjectStats" style="width: 100%" max-height="400">
            <el-table-column prop="name" label="学科名称" width="120" />
            <el-table-column prop="category" label="类别" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.category" type="info" size="small">
                  {{ row.category }}
                </el-tag>
                <span v-else class="text-gray">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="usageCount" label="使用次数" width="100">
              <template #default="{ row }">
                <el-text type="primary">{{ row.usageCount }}</el-text>
              </template>
            </el-table-column>
            <el-table-column prop="isActive" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.isActive ? 'success' : 'danger'" size="small">
                  {{ row.isActive ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>年级使用统计</span>
            <el-button
              style="float: right; padding: 3px 0"
              type="text"
              @click="refreshGradeLevelStats"
              :loading="loadingGradeLevels"
            >
              刷新
            </el-button>
          </template>
          
          <el-table :data="gradeLevelStats" style="width: 100%" max-height="400">
            <el-table-column prop="name" label="年级名称" width="120" />
            <el-table-column prop="category" label="类别" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.category" type="info" size="small">
                  {{ row.category }}
                </el-tag>
                <span v-else class="text-gray">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="usageCount" label="使用次数" width="100">
              <template #default="{ row }">
                <el-text type="primary">{{ row.usageCount }}</el-text>
              </template>
            </el-table-column>
            <el-table-column prop="isActive" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.isActive ? 'success' : 'danger'" size="small">
                  {{ row.isActive ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 类别分布图表 -->
    <el-row :gutter="24" style="margin-top: 24px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>学科类别分布</span>
          </template>
          <div class="category-stats">
            <div
              v-for="category in subjectCategories"
              :key="category.name"
              class="category-item"
            >
              <div class="category-name">{{ category.name || '未分类' }}</div>
              <div class="category-progress">
                <el-progress
                  :percentage="category.percentage"
                  :show-text="false"
                  :stroke-width="8"
                />
                <span class="category-count">{{ category.count }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>年级类别分布</span>
          </template>
          <div class="category-stats">
            <div
              v-for="category in gradeLevelCategories"
              :key="category.name"
              class="category-item"
            >
              <div class="category-name">{{ category.name || '未分类' }}</div>
              <div class="category-progress">
                <el-progress
                  :percentage="category.percentage"
                  :show-text="false"
                  :stroke-width="8"
                />
                <span class="category-count">{{ category.count }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, School, DataAnalysis, Flag } from '@element-plus/icons-vue'
import { subjectApi, gradeLevelApi } from '@/api/metadata'
import type { SubjectResponse, GradeLevelResponse } from '@/types/api'

// 响应式数据
const loadingSubjects = ref(false)
const loadingGradeLevels = ref(false)
const subjectStats = ref<SubjectResponse[]>([])
const gradeLevelStats = ref<GradeLevelResponse[]>([])

// 计算属性
const statistics = computed(() => [
  {
    title: '学科总数',
    value: Array.isArray(subjectStats.value) ? subjectStats.value.length : 0,
    icon: Document,
    color: 'primary'
  },
  {
    title: '年级总数',
    value: Array.isArray(gradeLevelStats.value) ? gradeLevelStats.value.length : 0,
    icon: School,
    color: 'success'
  },
  {
    title: '活跃学科',
    value: Array.isArray(subjectStats.value) ? subjectStats.value.filter(s => s.isActive).length : 0,
    icon: Flag,
    color: 'warning'
  },
  {
    title: '活跃年级',
    value: Array.isArray(gradeLevelStats.value) ? gradeLevelStats.value.filter(g => g.isActive).length : 0,
    icon: DataAnalysis,
    color: 'info'
  }
])

const subjectCategories = computed(() => {
  if (!Array.isArray(subjectStats.value)) return []
  
  const categoryMap = new Map<string, number>()
  subjectStats.value.forEach(subject => {
    const category = subject.category || '未分类'
    categoryMap.set(category, (categoryMap.get(category) || 0) + 1)
  })
  
  const total = subjectStats.value.length || 1
  return Array.from(categoryMap.entries()).map(([name, count]) => ({
    name,
    count,
    percentage: Math.round((count / total) * 100)
  })).sort((a, b) => b.count - a.count)
})

const gradeLevelCategories = computed(() => {
  if (!Array.isArray(gradeLevelStats.value)) return []
  
  const categoryMap = new Map<string, number>()
  gradeLevelStats.value.forEach(gradeLevel => {
    const category = gradeLevel.category || '未分类'
    categoryMap.set(category, (categoryMap.get(category) || 0) + 1)
  })
  
  const total = gradeLevelStats.value.length || 1
  return Array.from(categoryMap.entries()).map(([name, count]) => ({
    name,
    count,
    percentage: Math.round((count / total) * 100)
  })).sort((a, b) => b.count - a.count)
})

// 生命周期
onMounted(() => {
  refreshSubjectStats()
  refreshGradeLevelStats()
})

// 方法
const refreshSubjectStats = async () => {
  try {
    loadingSubjects.value = true
    const response = await subjectApi.getAllSubjectsList()
    // 处理分页响应，取出content数组
    subjectStats.value = response.data?.content || response.data || []
  } catch (error) {
    console.error('Failed to load subject stats:', error)
    ElMessage.error('加载学科统计失败')
    subjectStats.value = []
  } finally {
    loadingSubjects.value = false
  }
}

const refreshGradeLevelStats = async () => {
  try {
    loadingGradeLevels.value = true
    const response = await gradeLevelApi.getAllGradeLevelsList()
    // 处理分页响应，取出content数组
    gradeLevelStats.value = response.data?.content || response.data || []
  } catch (error) {
    console.error('Failed to load grade level stats:', error)
    ElMessage.error('加载年级统计失败')
    gradeLevelStats.value = []
  } finally {
    loadingGradeLevels.value = false
  }
}
</script>

<style scoped>
.metadata-statistics {
  padding: 20px;
}

.stat-card {
  border-radius: 8px;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-icon.primary {
  background: linear-gradient(135deg, #409eff, #79bbff);
}

.stat-icon.success {
  background: linear-gradient(135deg, #67c23a, #95d475);
}

.stat-icon.warning {
  background: linear-gradient(135deg, #e6a23c, #f0c078);
}

.stat-icon.info {
  background: linear-gradient(135deg, #909399, #b1b3b8);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  line-height: 1;
}

.stat-title {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.text-gray {
  color: #c0c4cc;
}

.category-stats {
  padding: 0;
}

.category-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.category-item:last-child {
  border-bottom: none;
}

.category-name {
  font-weight: 500;
  color: #303133;
  min-width: 80px;
}

.category-progress {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  margin-left: 20px;
}

.category-progress :deep(.el-progress) {
  flex: 1;
}

.category-count {
  font-size: 14px;
  color: #606266;
  min-width: 30px;
  text-align: right;
}
</style>
