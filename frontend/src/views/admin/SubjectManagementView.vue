<template>
  <div class="subject-management">
    <div class="page-header">
      <h1>学科管理</h1>
      <p class="page-description">管理系统中的学科信息，用于知识库和题目分类</p>
      <div class="header-actions">
        <el-button type="primary" @click="showCreateDialog" :icon="Plus">
          新增学科
        </el-button>
        <el-button @click="loadSubjects" :icon="Refresh" :loading="loading">
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
            placeholder="搜索学科名称..."
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
            共 {{ total }} 个学科
          </el-text>
        </el-col>
      </el-row>
    </el-card>

    <!-- 学科列表 -->
    <el-card>
      <el-table
        v-loading="loading"
        :data="subjects"
        style="width: 100%"
        row-key="id"
      >
        <el-table-column prop="name" label="学科名称" min-width="150">
          <template #default="{ row }">
            <div class="subject-name">
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
              @click="editSubject(row)"
              :icon="Edit"
            >
              编辑
            </el-button>
            <el-button
              size="small"
              type="danger"
              @click="deleteSubject(row)"
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
      :title="isEditing ? '编辑学科' : '新增学科'"
      width="600px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="subjectForm"
        :rules="formRules"
        label-width="80px"
      >
        <el-form-item label="学科名称" prop="name">
          <el-input
            v-model="subjectForm.name"
            placeholder="请输入学科名称"
          />
        </el-form-item>
        
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="subjectForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入学科描述"
          />
        </el-form-item>
        
        <el-form-item label="类别" prop="category">
          <el-select
            v-model="subjectForm.category"
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
        </el-form-item>
        
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number
            v-model="subjectForm.sortOrder"
            :min="0"
            :max="9999"
            placeholder="排序值（数字越小越靠前）"
            style="width: 100%"
          />
        </el-form-item>
        
        <!-- 年级关联配置 -->
        <el-form-item label="适用年级">
          <div class="grade-mapping-section">
            <div class="section-header">
              <span class="section-title">选择该学科适用的年级：</span>
              <el-button
                v-if="isEditing"
                size="small"
                type="primary"
                @click="loadGradeMappings"
                :loading="loadingGrades"
              >
                刷新关联
              </el-button>
            </div>
            
            <div v-loading="loadingGrades" class="grade-selection">
              <div v-for="category in gradeCategories" :key="category" class="grade-category">
                <div class="category-title">{{ category }}</div>
                <div class="grade-checkboxes">
                  <el-checkbox
                    v-for="grade in getGradesByCategory(category)"
                    :key="grade.id"
                    v-model="selectedGrades"
                    :label="grade.id"
                    :value="grade.id"
                    @change="onGradeSelectionChange"
                  >
                    {{ grade.name }}
                  </el-checkbox>
                </div>
              </div>
            </div>
            
            <div v-if="selectedGrades.length > 0" class="selected-grades-summary">
              <el-text type="info">
                已选择 {{ selectedGrades.length }} 个年级
              </el-text>
            </div>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            @click="saveSubject"
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
import { subjectApi, gradeLevelApi, subjectGradeMappingApi } from '@/api/metadata'
import type { SubjectResponse, SubjectRequest } from '@/types/api'

// 响应式数据
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEditing = ref(false)
const subjects = ref<SubjectResponse[]>([])
const categories = ref<string[]>([])
const searchKeyword = ref('')
const selectedCategory = ref<string>('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const formRef = ref()
const editingSubjectId = ref<number>()

// 年级关联相关数据
const loadingGrades = ref(false)
const allGradeLevels = ref<any[]>([])
const gradeCategories = ref<string[]>([])
const selectedGrades = ref<number[]>([])
const originalGrades = ref<number[]>([]) // 用于记录原始的年级关联

// 表单数据
const subjectForm = reactive<SubjectRequest>({
  name: '',
  description: '',
  category: '',
  sortOrder: 0
})

// 表单验证规则
const formRules = {
  name: [
    { required: true, message: '请输入学科名称', trigger: 'blur' },
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
  loadSubjects()
  loadCategories()
  loadAllGradeLevels()
})

// 方法
const loadSubjects = async () => {
  try {
    loading.value = true
    const response = await subjectApi.getAllSubjects()
    // 处理分页响应，取出content数组
    const data = response.data?.content || response.data || []
    subjects.value = data
    total.value = response.data?.totalElements || data.length || 0
  } catch (error) {
    console.error('Failed to load subjects:', error)
    ElMessage.error('加载学科列表失败')
    subjects.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    categories.value = await subjectApi.getAllCategories()
  } catch (error) {
    console.error('Failed to load categories:', error)
  }
}

const loadAllGradeLevels = async () => {
  try {
    loadingGrades.value = true
    const response = await gradeLevelApi.getAllGradeLevels()
    const grades = response.data?.content || response.data || []
    allGradeLevels.value = grades
    
    // 提取年级类别
    const categories = [...new Set(grades.map((grade: any) => grade.category).filter(Boolean))]
    gradeCategories.value = categories
  } catch (error) {
    console.error('Failed to load grade levels:', error)
    ElMessage.error('加载年级列表失败')
  } finally {
    loadingGrades.value = false
  }
}

const handleSearch = async () => {
  if (searchKeyword.value.trim()) {
    try {
      loading.value = true
      subjects.value = await subjectApi.searchSubjects(searchKeyword.value.trim())
      total.value = subjects.value.length
    } catch (error) {
      console.error('Failed to search subjects:', error)
      ElMessage.error('搜索失败')
    } finally {
      loading.value = false
    }
  } else {
    loadSubjects()
  }
}

const handleCategoryFilter = async () => {
  if (selectedCategory.value) {
    try {
      loading.value = true
      subjects.value = await subjectApi.getSubjectsByCategory(selectedCategory.value)
      total.value = subjects.value.length
    } catch (error) {
      console.error('Failed to filter by category:', error)
      ElMessage.error('筛选失败')
    } finally {
      loading.value = false
    }
  } else {
    loadSubjects()
  }
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  loadSubjects()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  loadSubjects()
}

const showCreateDialog = () => {
  isEditing.value = false
  dialogVisible.value = true
  resetForm()
}

const editSubject = (subject: SubjectResponse) => {
  isEditing.value = true
  editingSubjectId.value = subject.id
  dialogVisible.value = true
  
  // 填充表单数据
  subjectForm.name = subject.name
  subjectForm.description = subject.description || ''
  subjectForm.category = subject.category || ''
  subjectForm.sortOrder = subject.sortOrder || 0
}

const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  subjectForm.name = ''
  subjectForm.description = ''
  subjectForm.category = ''
  subjectForm.sortOrder = 0
  editingSubjectId.value = undefined
}

const saveSubject = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    saving.value = true
    
    if (isEditing.value && editingSubjectId.value) {
      await subjectApi.updateSubject(editingSubjectId.value, subjectForm)
      ElMessage.success('学科更新成功')
    } else {
      await subjectApi.createSubject(subjectForm)
      ElMessage.success('学科创建成功')
    }
    
    dialogVisible.value = false
    loadSubjects()
    loadCategories() // 重新加载类别列表
  } catch (error) {
    console.error('Failed to save subject:', error)
    ElMessage.error(isEditing.value ? '更新学科失败' : '创建学科失败')
  } finally {
    saving.value = false
  }
}

const deleteSubject = async (subject: SubjectResponse) => {
  if (subject.usageCount > 0) {
    ElMessage.warning('该学科下还有相关数据，无法删除')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除学科 "${subject.name}" 吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await subjectApi.deleteSubject(subject.id)
    ElMessage.success('学科删除成功')
    loadSubjects()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to delete subject:', error)
      ElMessage.error('删除学科失败')
    }
  }
}

const formatDate = (dateString: string) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

const getGradesByCategory = (category: string) => {
  return allGradeLevels.value.filter(grade => grade.category === category && grade.isActive)
}

const loadGradeMappings = async () => {
  if (!editingSubjectId.value) return
  
  try {
    loadingGrades.value = true
    const response = await subjectGradeMappingApi.getGradesBySubjectId(editingSubjectId.value)
    const grades = response.data || []
    selectedGrades.value = grades.map((grade: any) => grade.id)
    originalGrades.value = [...selectedGrades.value]
  } catch (error) {
    console.error('Failed to load grade mappings:', error)
    ElMessage.error('加载年级关联失败')
  } finally {
    loadingGrades.value = false
  }
}

const onGradeSelectionChange = () => {
  // 可以在这里添加实时保存逻辑，或者等到保存学科时一起保存
  console.log('Grade selection changed:', selectedGrades.value)
}

const saveGradeMappings = async (subjectId: number) => {
  try {
    // 获取需要添加和删除的年级
    const toAdd = selectedGrades.value.filter(id => !originalGrades.value.includes(id))
    const toRemove = originalGrades.value.filter(id => !selectedGrades.value.includes(id))
    
    // 删除不再关联的年级
    for (const gradeId of toRemove) {
      const mappings = await subjectGradeMappingApi.getGradesBySubjectId(subjectId)
      const mapping = mappings.data?.find((m: any) => m.gradeLevelId === gradeId)
      if (mapping) {
        await subjectGradeMappingApi.deleteSubjectGradeMapping(mapping.id)
      }
    }
    
    // 添加新关联的年级
    for (const gradeId of toAdd) {
      await subjectGradeMappingApi.createSubjectGradeMapping({
        subjectId,
        gradeLevelId: gradeId
      })
    }
    
    // 更新原始年级列表
    originalGrades.value = [...selectedGrades.value]
  } catch (error) {
    console.error('Failed to save grade mappings:', error)
    throw error
  }
}
</script>

<style scoped>
.subject-management {
  padding: 20px;
}

.operation-bar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}

.search-filters {
  display: flex;
  gap: 12px;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  text-align: center;
}

.grade-mapping {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}

.grade-category {
  margin-bottom: 16px;
}

.grade-category h4 {
  margin: 0 0 8px 0;
  color: #606266;
  font-size: 14px;
}

.grade-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
