<template>
  <div class="exam-paper-template-view">
    <div class="page-header">
      <h1>试卷模板管理</h1>
      <p>创建和管理可重复使用的试卷模板，快速应用到多个考试</p>
    </div>

    <el-card class="main-card">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="left">
          <el-button type="primary" @click="handleCreateTemplate">
            <el-icon><Plus /></el-icon>
            创建模板
          </el-button>
          <el-button @click="handleRefresh">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
        
        <div class="right">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索模板名称"
            style="width: 200px; margin-right: 10px"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          
          <el-select v-model="subjectFilter" placeholder="选择科目" clearable style="width: 120px; margin-right: 10px">
            <el-option label="全部科目" value="" />
            <el-option label="数学" value="数学" />
            <el-option label="语文" value="语文" />
            <el-option label="英语" value="英语" />
            <el-option label="物理" value="物理" />
            <el-option label="化学" value="化学" />
            <el-option label="生物" value="生物" />
            <el-option label="历史" value="历史" />
            <el-option label="地理" value="地理" />
            <el-option label="政治" value="政治" />
          </el-select>
          
          <el-select v-model="statusFilter" placeholder="选择状态" clearable style="width: 120px">
            <el-option label="全部状态" value="" />
            <el-option label="草稿" value="DRAFT" />
            <el-option label="就绪" value="READY" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已归档" value="ARCHIVED" />
          </el-select>
        </div>
      </div>

      <!-- 模板列表 -->
      <el-table
        v-loading="loading"
        :data="filteredTemplates"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="name" label="模板名称" min-width="200">
          <template #default="{ row }">
            <div class="template-name">
              <span class="name">{{ row.name }}</span>
              <el-tag v-if="row.isPublic" size="small" type="success">公开</el-tag>
              <el-tag v-if="row.templateType === 'AI_GENERATED'" size="small" type="warning">AI生成</el-tag>
            </div>
            <div class="template-desc">{{ row.description || '暂无描述' }}</div>
          </template>
        </el-table-column>
        
        <el-table-column prop="subject" label="科目" width="100" />
        
        <el-table-column prop="gradeLevel" label="年级" width="100" />
        
        <el-table-column prop="totalScore" label="总分" width="80" />
        
        <el-table-column prop="duration" label="时长(分钟)" width="100" />
        
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="usageCount" label="使用次数" width="100" />
        
        <el-table-column prop="questionCount" label="题目数" width="100">
          <template #default="{ row }">
            {{ row.configuredQuestionCount }}/{{ row.questionCount }}
          </template>
        </el-table-column>
        
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleViewTemplate(row)">查看</el-button>
            <el-button size="small" type="primary" @click="handleApplyTemplate(row)" :disabled="!row.isUsable">
              应用
            </el-button>
            <el-dropdown @command="(command) => handleCommand(command, row)">
              <el-button size="small">
                更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit" :disabled="!canEdit(row)">编辑</el-dropdown-item>
                  <el-dropdown-item command="duplicate">复制</el-dropdown-item>
                  <el-dropdown-item command="delete" :disabled="!canDelete(row)">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 创建/编辑模板对话框 -->
    <el-dialog
      v-model="templateDialogVisible"
      :title="isEditing ? '编辑模板' : '创建模板'"
      width="80%"
      :close-on-click-modal="false"
    >
      <ExamPaperTemplateForm
        v-if="templateDialogVisible"
        :template="currentTemplate"
        :is-editing="isEditing"
        @save="handleTemplateSave"
        @cancel="templateDialogVisible = false"
        :loading="templateSaving"
      />
    </el-dialog>

    <!-- 应用模板对话框 -->
    <el-dialog
      v-model="applyDialogVisible"
      title="应用模板创建考试"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="applyForm" label-width="100px">
        <el-form-item label="模板名称">
          <el-input :value="selectedTemplate?.name" disabled />
        </el-form-item>
        <el-form-item label="考试标题" required>
          <el-input
            v-model="applyForm.examTitle"
            placeholder="请输入考试标题"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="考试描述">
          <el-input
            v-model="applyForm.examDescription"
            type="textarea"
            placeholder="请输入考试描述"
            maxlength="500"
            show-word-limit
            :rows="3"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="applyDialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            @click="confirmApplyTemplate"
            :loading="applying"
            :disabled="!applyForm.examTitle"
          >
            创建考试
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 模板详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="模板详情"
      width="80%"
      :close-on-click-modal="false"
    >
      <ExamPaperTemplateDetail
        v-if="detailDialogVisible && selectedTemplate"
        :template="selectedTemplate"
        @edit="handleEditTemplate"
        @apply="handleApplyTemplate"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { Plus, Refresh, Search, ArrowDown } from '@element-plus/icons-vue'
import { examPaperTemplateApi, type ExamPaperTemplate, type ExamPaperTemplateRequest } from '@/api/examPaperTemplate'
import ExamPaperTemplateForm from '@/components/template/ExamPaperTemplateForm.vue'
import ExamPaperTemplateDetail from '@/components/template/ExamPaperTemplateDetail.vue'

const router = useRouter()

// 响应式数据
const loading = ref(false)
const templates = ref<ExamPaperTemplate[]>([])
const selectedTemplates = ref<ExamPaperTemplate[]>([])
const searchKeyword = ref('')
const subjectFilter = ref('')
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 对话框相关
const templateDialogVisible = ref(false)
const applyDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const templateSaving = ref(false)
const applying = ref(false)
const isEditing = ref(false)
const currentTemplate = ref<ExamPaperTemplate | null>(null)
const selectedTemplate = ref<ExamPaperTemplate | null>(null)

const applyForm = reactive({
  examTitle: '',
  examDescription: ''
})

// 计算属性
const filteredTemplates = computed(() => {
  let result = templates.value

  if (searchKeyword.value) {
    result = result.filter(t => 
      t.name.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      t.description?.toLowerCase().includes(searchKeyword.value.toLowerCase())
    )
  }

  if (subjectFilter.value) {
    result = result.filter(t => t.subject === subjectFilter.value)
  }

  if (statusFilter.value) {
    result = result.filter(t => t.status === statusFilter.value)
  }

  return result
})

// 方法
const loadTemplates = async () => {
  try {
    loading.value = true
    const response = await examPaperTemplateApi.getUserTemplates()
    templates.value = response.data
    total.value = templates.value.length
  } catch (error) {
    console.error('加载模板列表失败:', error)
    ElMessage.error('加载模板列表失败')
  } finally {
    loading.value = false
  }
}

const handleCreateTemplate = () => {
  currentTemplate.value = null
  isEditing.value = false
  templateDialogVisible.value = true
}

const handleEditTemplate = (template: ExamPaperTemplate) => {
  currentTemplate.value = { ...template }
  isEditing.value = true
  templateDialogVisible.value = true
}

const handleViewTemplate = (template: ExamPaperTemplate) => {
  selectedTemplate.value = template
  detailDialogVisible.value = true
}

const handleApplyTemplate = (template: ExamPaperTemplate) => {
  selectedTemplate.value = template
  applyForm.examTitle = ''
  applyForm.examDescription = ''
  applyDialogVisible.value = true
}

const handleTemplateSave = async (template: ExamPaperTemplateRequest) => {
  try {
    templateSaving.value = true
    
    if (isEditing.value && currentTemplate.value) {
      await examPaperTemplateApi.updateTemplate(currentTemplate.value.id, template)
      ElMessage.success('模板更新成功')
    } else {
      await examPaperTemplateApi.createTemplate(template)
      ElMessage.success('模板创建成功')
    }
    
    templateDialogVisible.value = false
    loadTemplates()
    
  } catch (error) {
    console.error('保存模板失败:', error)
    ElMessage.error('保存模板失败，请重试')
  } finally {
    templateSaving.value = false
  }
}

const confirmApplyTemplate = async () => {
  if (!selectedTemplate.value) return
  
  try {
    applying.value = true
    const response = await examPaperTemplateApi.applyTemplateToExam(
      selectedTemplate.value.id,
      applyForm.examTitle,
      applyForm.examDescription
    )
    
    ElMessage.success('考试创建成功')
    applyDialogVisible.value = false
    
    // 跳转到考试详情页
    router.push(`/exams/${response.data.id}`)
    
  } catch (error) {
    console.error('应用模板失败:', error)
    ElMessage.error('应用模板失败，请重试')
  } finally {
    applying.value = false
  }
}

const handleCommand = async (command: string, template: ExamPaperTemplate) => {
  switch (command) {
    case 'edit':
      handleEditTemplate(template)
      break
    case 'duplicate':
      try {
        await examPaperTemplateApi.duplicateTemplate(template.id)
        ElMessage.success('模板复制成功')
        loadTemplates()
      } catch (error) {
        console.error('复制模板失败:', error)
        ElMessage.error('复制模板失败')
      }
      break
    case 'delete':
      try {
        await ElMessageBox.confirm(
          `确定要删除模板"${template.name}"吗？此操作不可恢复。`,
          '确认删除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        await examPaperTemplateApi.deleteTemplate(template.id)
        ElMessage.success('模板删除成功')
        loadTemplates()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除模板失败:', error)
          ElMessage.error('删除模板失败')
        }
      }
      break
  }
}

const handleSelectionChange = (selection: ExamPaperTemplate[]) => {
  selectedTemplates.value = selection
}

const handleSearch = () => {
  currentPage.value = 1
}

const handleRefresh = () => {
  loadTemplates()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
}

const canEdit = (template: ExamPaperTemplate) => {
  // 只有创建者可以编辑
  return template.createdBy === getCurrentUserId()
}

const canDelete = (template: ExamPaperTemplate) => {
  // 只有创建者可以删除，且模板未被使用
  return template.createdBy === getCurrentUserId() && template.usageCount === 0
}

const getCurrentUserId = () => {
  // 从用户状态获取当前用户ID
  return 1 // 临时返回，实际应该从用户状态获取
}

const getStatusType = (status: string) => {
  switch (status) {
    case 'DRAFT': return 'info'
    case 'READY': return 'success'
    case 'PUBLISHED': return 'primary'
    case 'ARCHIVED': return 'warning'
    default: return 'info'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'DRAFT': return '草稿'
    case 'READY': return '就绪'
    case 'PUBLISHED': return '已发布'
    case 'ARCHIVED': return '已归档'
    default: return status
  }
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN')
}

onMounted(() => {
  loadTemplates()
})
</script>

<style scoped>
.exam-paper-template-view {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-header h1 {
  color: #303133;
  font-size: 28px;
  margin-bottom: 10px;
}

.page-header p {
  color: #606266;
  font-size: 16px;
}

.main-card {
  min-height: 600px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.toolbar .left {
  display: flex;
  gap: 10px;
}

.toolbar .right {
  display: flex;
  align-items: center;
}

.template-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.template-name .name {
  font-weight: 500;
}

.template-desc {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.dialog-footer {
  text-align: right;
}
</style> 