<template>
  <div class="grade-level-management">
    <div class="page-header">
      <h1>年级管理</h1>
      <p class="page-description">管理系统中的年级信息，用于知识库和题目分类</p>
      <div class="header-actions">
        <el-button type="primary" @click="showCreateDialog" :icon="Plus">
          新增年级
        </el-button>
        <el-button @click="loadGradeLevels" :icon="Refresh" :loading="loading">
          刷新
        </el-button>
      </div>
    </div>

    <!-- 搜索和过滤 -->
    <el-card class="filter-card" style="margin-bottom: 20px;">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索年级名称..."
            clearable
            @input="handleSearch"
            :prefix-icon="Search"
          />
        </el-col>
        <el-col :span="6">
          <el-select
            v-model="selectedCategory"
            placeholder="选择类别"
            clearable
            @change="handleCategoryFilter"
          >
            <el-option
              v-for="category in categories"
              :key="category"
              :label="category"
              :value="category"
            />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-text class="filter-info">
            共 {{ total }} 个年级
          </el-text>
        </el-col>
      </el-row>
    </el-card>

    <!-- 年级列表 -->
    <el-card>
      <el-table
        v-loading="loading"
        :data="gradeLevels"
        style="width: 100%"
        row-key="id"
      >
        <el-table-column prop="name" label="年级名称" min-width="150">
          <template #default="{ row }">
            <div class="grade-level-name">
              <span class="name">{{ row.name }}</span>
              <el-tag v-if="!row.isActive" type="danger" size="small">已禁用</el-tag>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        
        <el-table-column prop="category" label="类别" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.category" type="info" size="small">
              {{ row.category }}
            </el-tag>
            <span v-else class="text-gray">-</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="sortOrder" label="排序" width="80" />
        
        <el-table-column prop="usageCount" label="使用次数" width="100">
          <template #default="{ row }">
            <el-text type="primary">{{ row.usageCount }}</el-text>
          </template>
        </el-table-column>
        
        <el-table-column prop="createdBy" label="创建者" width="120" />
        
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              @click="editGradeLevel(row)"
              :icon="Edit"
            >
              编辑
            </el-button>
            <el-button
              size="small"
              type="danger"
              @click="deleteGradeLevel(row)"
              :disabled="row.usageCount > 0"
              :icon="Delete"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container" style="margin-top: 20px;">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑年级' : '新增年级'"
      width="600px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="gradeLevelForm"
        :rules="formRules"
        label-width="80px"
      >
        <el-form-item label="年级名称" prop="name">
          <el-input
            v-model="gradeLevelForm.name"
            placeholder="请输入年级名称"
          />
        </el-form-item>
        
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="gradeLevelForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入年级描述"
          />
        </el-form-item>
        
        <el-form-item label="类别" prop="category">
          <el-select
            v-model="gradeLevelForm.category"
            placeholder="选择或输入类别"
            filterable
            allow-create
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="category in categories"
              :key="category"
              :label="category"
              :value="category"
            />
          </el-select>
          <div class="form-help-text">
            常见类别：小学、初中、高中、本科、研究生等
          </div>
        </el-form-item>
        
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number
            v-model="gradeLevelForm.sortOrder"
            :min="0"
            :max="9999"
            placeholder="排序值（数字越小越靠前）"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            @click="saveGradeLevel"
            :loading="saving"
          >
            {{ isEditing ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search, Edit, Delete } from '@element-plus/icons-vue'
import { gradeLevelApi } from '@/api/metadata'
import type { GradeLevelResponse, GradeLevelRequest } from '@/types/api'

// 响应式数据
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEditing = ref(false)
const gradeLevels = ref<GradeLevelResponse[]>([])
const categories = ref<string[]>([])
const searchKeyword = ref('')
const selectedCategory = ref<string>('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const formRef = ref()
const editingGradeLevelId = ref<number>()

// 表单数据
const gradeLevelForm = reactive<GradeLevelRequest>({
  name: '',
  description: '',
  category: '',
  sortOrder: 0
})

// 表单验证规则
const formRules = {
  name: [
    { required: true, message: '请输入年级名称', trigger: 'blur' },
    { min: 1, max: 100, message: '长度在 1 到 100 个字符', trigger: 'blur' }
  ],
  description: [
    { max: 500, message: '长度不能超过 500 个字符', trigger: 'blur' }
  ],
  category: [
    { max: 50, message: '长度不能超过 50 个字符', trigger: 'blur' }
  ],
  sortOrder: [
    { type: 'number', min: 0, max: 9999, message: '排序值必须在 0-9999 之间', trigger: 'blur' }
  ]
}

// 生命周期
onMounted(() => {
  loadGradeLevels()
  loadCategories()
})

// 方法
const loadGradeLevels = async () => {
  try {
    loading.value = true
    const response = await gradeLevelApi.getAllGradeLevels()
    // 处理分页响应，取出content数组
    const data = response.data?.content || response.data || []
    gradeLevels.value = data
    total.value = response.data?.totalElements || data.length || 0
  } catch (error) {
    console.error('Failed to load grade levels:', error)
    ElMessage.error('加载年级列表失败')
    gradeLevels.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    categories.value = await gradeLevelApi.getAllGradeLevelCategories()
  } catch (error) {
    console.error('Failed to load categories:', error)
  }
}

const handleSearch = async () => {
  if (searchKeyword.value.trim()) {
    try {
      loading.value = true
      gradeLevels.value = await gradeLevelApi.searchGradeLevels(searchKeyword.value.trim())
      total.value = gradeLevels.value.length
    } catch (error) {
      console.error('Failed to search grade levels:', error)
      ElMessage.error('搜索失败')
    } finally {
      loading.value = false
    }
  } else {
    loadGradeLevels()
  }
}

const handleCategoryFilter = async () => {
  if (selectedCategory.value) {
    try {
      loading.value = true
      gradeLevels.value = await gradeLevelApi.getGradeLevelsByCategory(selectedCategory.value)
      total.value = gradeLevels.value.length
    } catch (error) {
      console.error('Failed to filter by category:', error)
      ElMessage.error('筛选失败')
    } finally {
      loading.value = false
    }
  } else {
    loadGradeLevels()
  }
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  loadGradeLevels()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  loadGradeLevels()
}

const showCreateDialog = () => {
  isEditing.value = false
  dialogVisible.value = true
  resetForm()
}

const editGradeLevel = (gradeLevel: GradeLevelResponse) => {
  isEditing.value = true
  editingGradeLevelId.value = gradeLevel.id
  dialogVisible.value = true
  
  // 填充表单数据
  gradeLevelForm.name = gradeLevel.name
  gradeLevelForm.description = gradeLevel.description || ''
  gradeLevelForm.category = gradeLevel.category || ''
  gradeLevelForm.sortOrder = gradeLevel.sortOrder || 0
}

const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  gradeLevelForm.name = ''
  gradeLevelForm.description = ''
  gradeLevelForm.category = ''
  gradeLevelForm.sortOrder = 0
  editingGradeLevelId.value = undefined
}

const saveGradeLevel = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    saving.value = true
    
    if (isEditing.value && editingGradeLevelId.value) {
      await gradeLevelApi.updateGradeLevel(editingGradeLevelId.value, gradeLevelForm)
      ElMessage.success('年级更新成功')
    } else {
      await gradeLevelApi.createGradeLevel(gradeLevelForm)
      ElMessage.success('年级创建成功')
    }
    
    dialogVisible.value = false
    loadGradeLevels()
    loadCategories() // 重新加载类别列表
  } catch (error) {
    console.error('Failed to save grade level:', error)
    ElMessage.error(isEditing.value ? '更新年级失败' : '创建年级失败')
  } finally {
    saving.value = false
  }
}

const deleteGradeLevel = async (gradeLevel: GradeLevelResponse) => {
  if (gradeLevel.usageCount > 0) {
    ElMessage.warning('该年级下还有相关数据，无法删除')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除年级 "${gradeLevel.name}" 吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await gradeLevelApi.deleteGradeLevel(gradeLevel.id)
    ElMessage.success('年级删除成功')
    loadGradeLevels()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to delete grade level:', error)
      ElMessage.error('删除年级失败')
    }
  }
}

const formatDate = (dateString: string) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}
</script>

<style scoped>
.grade-level-management {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  color: #303133;
  font-size: 24px;
  font-weight: 500;
}

.page-description {
  margin: 8px 0 0 0;
  color: #909399;
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.filter-card {
  border-radius: 8px;
}

.filter-info {
  line-height: 32px;
  color: #909399;
  font-size: 14px;
}

.grade-level-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.grade-level-name .name {
  font-weight: 500;
}

.text-gray {
  color: #c0c4cc;
}

.pagination-container {
  display: flex;
  justify-content: center;
}

.dialog-footer {
  text-align: right;
}

.form-help-text {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
