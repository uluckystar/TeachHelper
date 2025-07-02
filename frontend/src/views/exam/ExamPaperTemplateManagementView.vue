<template>
  <div class="exam-paper-template-management">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1>试卷模板管理</h1>
        <p>创建和管理可重用的试卷模板，快速应用到多个考试</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="showCreateDialog = true" icon="Plus">
          创建模板
        </el-button>
        <el-button @click="loadTemplates" :loading="loading" icon="Refresh">
          刷新
        </el-button>
      </div>
    </div>

    <!-- 搜索和过滤 -->
    <el-card class="search-card">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索模板名称"
            clearable
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="4">
          <el-select
            v-model="subjectFilter"
            placeholder="科目"
            clearable
            @change="handleFilter"
          >
            <el-option label="数学" value="数学" />
            <el-option label="语文" value="语文" />
            <el-option label="英语" value="英语" />
            <el-option label="物理" value="物理" />
            <el-option label="化学" value="化学" />
            <el-option label="生物" value="生物" />
            <el-option label="历史" value="历史" />
            <el-option label="地理" value="地理" />
            <el-option label="政治" value="政治" />
            <el-option label="计算机" value="计算机" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select
            v-model="gradeLevelFilter"
            placeholder="年级"
            clearable
            @change="handleFilter"
          >
            <el-option label="小学" value="小学" />
            <el-option label="初中" value="初中" />
            <el-option label="高中" value="高中" />
            <el-option label="大学" value="大学" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select
            v-model="statusFilter"
            placeholder="状态"
            clearable
            @change="handleFilter"
          >
            <el-option label="草稿" value="DRAFT" />
            <el-option label="就绪" value="READY" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已归档" value="ARCHIVED" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-button-group>
            <el-button 
              :type="viewMode === 'grid' ? 'primary' : ''" 
              @click="viewMode = 'grid'"
              icon="Grid"
            >
              网格视图
            </el-button>
            <el-button 
              :type="viewMode === 'table' ? 'primary' : ''" 
              @click="viewMode = 'table'"
              icon="List"
            >
              列表视图
            </el-button>
          </el-button-group>
        </el-col>
      </el-row>
    </el-card>

    <!-- 模板列表 -->
    <div class="template-content">
      <!-- 网格视图 -->
      <div v-if="viewMode === 'grid'" class="grid-view">
        <el-row :gutter="20" v-loading="loading">
          <el-col
            v-for="template in filteredTemplates"
            :key="template.id"
            :span="8"
            style="margin-bottom: 20px"
          >
            <el-card class="template-card" :class="{ 'public-template': template.isPublic }">
              <template #header>
                <div class="template-header">
                  <div class="template-title">
                    <h3>{{ template.name }}</h3>
                    <el-tag
                      v-if="template.isPublic"
                      type="warning"
                      size="small"
                    >
                      公开模板
                    </el-tag>
                  </div>
                  <div class="template-actions">
                    <el-dropdown @command="handleTemplateAction">
                      <el-button type="text" icon="MoreFilled" />
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item :command="{ action: 'view', template }">
                            <el-icon><View /></el-icon>查看详情
                          </el-dropdown-item>
                          <el-dropdown-item :command="{ action: 'edit', template }">
                            <el-icon><Edit /></el-icon>编辑
                          </el-dropdown-item>
                          <el-dropdown-item :command="{ action: 'duplicate', template }">
                            <el-icon><CopyDocument /></el-icon>复制
                          </el-dropdown-item>
                          <el-dropdown-item :command="{ action: 'apply', template }">
                            <el-icon><MagicStick /></el-icon>应用创建考试
                          </el-dropdown-item>
                          <el-dropdown-item 
                            :command="{ action: 'delete', template }"
                            divided
                            class="danger-item"
                          >
                            <el-icon><Delete /></el-icon>删除
                          </el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                </div>
              </template>

              <div class="template-content">
                <p class="template-description">
                  {{ template.description || '暂无描述' }}
                </p>

                <div class="template-info">
                  <div class="info-row">
                    <span class="label">科目：</span>
                    <el-tag size="small">{{ template.subject }}</el-tag>
                  </div>
                  <div class="info-row">
                    <span class="label">年级：</span>
                    <el-tag size="small" type="success">{{ template.gradeLevel }}</el-tag>
                  </div>
                  <div class="info-row">
                    <span class="label">总分：</span>
                    <span>{{ template.totalScore }} 分</span>
                  </div>
                  <div class="info-row" v-if="template.duration">
                    <span class="label">时长：</span>
                    <span>{{ template.duration }} 分钟</span>
                  </div>
                </div>

                <div class="template-stats">
                  <div class="stat-item">
                    <span class="number">{{ template.questionCount }}</span>
                    <span class="label">题目数</span>
                  </div>
                  <div class="stat-item">
                    <span class="number">{{ template.usageCount }}</span>
                    <span class="label">使用次数</span>
                  </div>
                  <div class="stat-item">
                    <span class="number">{{ template.configuredQuestionCount }}</span>
                    <span class="label">已配置</span>
                  </div>
                </div>

                <div class="template-status">
                  <el-tag :type="getStatusType(template.status)" size="small">
                    {{ getStatusText(template.status) }}
                  </el-tag>
                  <el-tag :type="getTemplateTypeColor(template.templateType)" size="small">
                    {{ getTemplateTypeText(template.templateType) }}
                  </el-tag>
                </div>

                <div class="template-footer">
                  <span class="creator">创建者：{{ template.creatorName }}</span>
                  <span class="date">{{ formatDate(template.createdAt) }}</span>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 列表视图 -->
      <div v-else class="table-view">
        <el-table 
          :data="filteredTemplates" 
          v-loading="loading"
          style="width: 100%"
          @row-click="handleRowClick"
        >
          <el-table-column prop="name" label="模板名称" min-width="200">
            <template #default="{ row }">
              <el-link @click="viewTemplate(row)" type="primary">
                {{ row.name }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column prop="subject" label="科目" width="100" />
          <el-table-column prop="gradeLevel" label="年级" width="100" />
          <el-table-column prop="totalScore" label="总分" width="80" align="center" />
          <el-table-column prop="duration" label="时长" width="100" align="center">
            <template #default="{ row }">
              {{ row.duration ? `${row.duration}分钟` : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="questionCount" label="题目数" width="100" align="center" />
          <el-table-column prop="usageCount" label="使用次数" width="100" align="center" />
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="templateType" label="类型" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="getTemplateTypeColor(row.templateType)" size="small">
                {{ getTemplateTypeText(row.templateType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="creatorName" label="创建者" width="120" />
          <el-table-column prop="createdAt" label="创建时间" width="160">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button-group>
                <el-button size="small" @click="viewTemplate(row)" icon="View">
                  查看
                </el-button>
                <el-button size="small" @click="editTemplate(row)" icon="Edit">
                  编辑
                </el-button>
                <el-button size="small" @click="duplicateTemplate(row)" icon="CopyDocument">
                  复制
                </el-button>
                <el-button 
                  size="small" 
                  type="primary" 
                  @click="applyTemplate(row)" 
                  icon="MagicStick"
                  :disabled="!template.isUsable"
                >
                  应用
                </el-button>
                <el-button 
                  size="small" 
                  type="danger" 
                  @click="deleteTemplate(row)" 
                  icon="Delete"
                >
                  删除
                </el-button>
              </el-button-group>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="viewMode === 'table'">
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
    </div>

    <!-- 创建/编辑模板对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="isEditing ? '编辑模板' : '创建模板'"
      width="80%"
      :close-on-click-modal="false"
    >
      <ExamPaperTemplateForm
        v-if="showCreateDialog"
        :template="editingTemplate"
        :is-editing="isEditing"
        @save="handleTemplateSave"
        @cancel="showCreateDialog = false"
        :loading="saving"
      />
    </el-dialog>

    <!-- 应用模板对话框 -->
    <el-dialog
      v-model="showApplyDialog"
      title="应用模板创建考试"
      width="500px"
    >
      <el-form :model="applyForm" :rules="applyRules" ref="applyFormRef" label-width="100px">
        <el-form-item label="模板名称">
          <el-input :value="selectedTemplate?.name" disabled />
        </el-form-item>
        <el-form-item label="考试标题" prop="examTitle">
          <el-input
            v-model="applyForm.examTitle"
            placeholder="请输入考试标题"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="考试描述" prop="examDescription">
          <el-input
            v-model="applyForm.examDescription"
            type="textarea"
            placeholder="请输入考试描述（可选）"
            :rows="3"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showApplyDialog = false">取消</el-button>
          <el-button
            type="primary"
            @click="confirmApply"
            :loading="applying"
            :disabled="!applyForm.examTitle"
          >
            创建考试
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  Search, Refresh, MagicStick, CopyDocument, Edit, Delete, MoreFilled,
  View, Grid, List
} from '@element-plus/icons-vue'
import { examPaperTemplateApi, type ExamPaperTemplate } from '@/api/examPaperTemplate'
import ExamPaperTemplateForm from '@/components/exam/ExamPaperTemplateForm.vue'

const router = useRouter()

// 响应式数据
const loading = ref(false)
const saving = ref(false)
const applying = ref(false)
const templates = ref<ExamPaperTemplate[]>([])
const viewMode = ref<'grid' | 'table'>('grid')

// 搜索和过滤
const searchKeyword = ref('')
const subjectFilter = ref('')
const gradeLevelFilter = ref('')
const statusFilter = ref('')

// 分页
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 对话框状态
const showCreateDialog = ref(false)
const showApplyDialog = ref(false)
const isEditing = ref(false)
const editingTemplate = ref<ExamPaperTemplate | null>(null)
const selectedTemplate = ref<ExamPaperTemplate | null>(null)

// 应用表单
const applyFormRef = ref<FormInstance>()
const applyForm = reactive({
  examTitle: '',
  examDescription: ''
})

const applyRules: FormRules = {
  examTitle: [
    { required: true, message: '请输入考试标题', trigger: 'blur' },
    { min: 2, max: 200, message: '考试标题长度在2-200个字符', trigger: 'blur' }
  ]
}

// 计算属性
const filteredTemplates = computed(() => {
  return templates.value.filter(template => {
    const matchesKeyword = !searchKeyword.value || 
      template.name.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      (template.description && template.description.toLowerCase().includes(searchKeyword.value.toLowerCase()))
    
    const matchesSubject = !subjectFilter.value || 
      template.subject === subjectFilter.value
    
    const matchesGradeLevel = !gradeLevelFilter.value || 
      template.gradeLevel === gradeLevelFilter.value
    
    const matchesStatus = !statusFilter.value || 
      template.status === statusFilter.value
    
    return matchesKeyword && matchesSubject && matchesGradeLevel && matchesStatus
  })
})

// 方法
const loadTemplates = async () => {
  try {
    loading.value = true
    // 获取用户自己的模板和公开模板
    const [userTemplatesResponse, publicTemplatesResponse] = await Promise.all([
      examPaperTemplateApi.getUserTemplates(),
      examPaperTemplateApi.getPublicTemplates()
    ])
    
    const userTemplates = userTemplatesResponse.data
    const publicTemplates = publicTemplatesResponse.data
    
    // 合并并去重模板（避免用户的公开模板重复显示）
    const templateMap = new Map()
    
    // 先添加用户模板
    userTemplates.forEach((template: ExamPaperTemplate) => {
      templateMap.set(template.id, template)
    })
    
    // 再添加公开模板（如果不存在的话）
    publicTemplates.forEach((template: ExamPaperTemplate) => {
      if (!templateMap.has(template.id)) {
        templateMap.set(template.id, template)
      }
    })
    
    // 转换为数组并按创建时间倒序排序
    templates.value = Array.from(templateMap.values()).sort((a, b) => 
      new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
    
    total.value = templates.value.length
  } catch (error) {
    console.error('加载模板失败:', error)
    ElMessage.error('加载模板失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  // 实时搜索，由计算属性处理
}

const handleFilter = () => {
  // 实时过滤，由计算属性处理
}

const handleTemplateAction = async (command: { action: string, template: ExamPaperTemplate }) => {
  const { action, template } = command
  
  switch (action) {
    case 'view':
      viewTemplate(template)
      break
    case 'edit':
      editTemplate(template)
      break
    case 'duplicate':
      await duplicateTemplate(template)
      break
    case 'apply':
      applyTemplate(template)
      break
    case 'delete':
      await deleteTemplate(template)
      break
  }
}

const viewTemplate = (template: ExamPaperTemplate) => {
  router.push(`/exam-paper-templates/${template.id}`)
}

const editTemplate = (template: ExamPaperTemplate) => {
  editingTemplate.value = template
  isEditing.value = true
  showCreateDialog.value = true
}

const duplicateTemplate = async (template: ExamPaperTemplate) => {
  try {
    await examPaperTemplateApi.duplicateTemplate(template.id)
    ElMessage.success('模板复制成功')
    await loadTemplates()
  } catch (error) {
    console.error('复制模板失败:', error)
    ElMessage.error('复制模板失败')
  }
}

const applyTemplate = (template: ExamPaperTemplate) => {
  selectedTemplate.value = template
  applyForm.examTitle = `${template.name} - ${formatDate(new Date())}`
  applyForm.examDescription = template.description || ''
  showApplyDialog.value = true
}

const confirmApply = async () => {
  if (!applyFormRef.value || !selectedTemplate.value) return
  
  try {
    const valid = await applyFormRef.value.validate()
    if (!valid) return
  } catch {
    return
  }

  applying.value = true
  try {
    const response = await examPaperTemplateApi.applyTemplateToExam(
      selectedTemplate.value.id,
      applyForm.examTitle,
      applyForm.examDescription
    )
    const exam = response.data
    ElMessage.success('考试创建成功')
    showApplyDialog.value = false
    resetApplyForm()
    router.push(`/exams/${exam.id}`)
  } catch (error) {
    console.error('应用模板失败:', error)
    ElMessage.error('应用模板失败')
  } finally {
    applying.value = false
  }
}

const deleteTemplate = async (template: ExamPaperTemplate) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除模板"${template.name}"吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    await examPaperTemplateApi.deleteTemplate(template.id)
    ElMessage.success('模板删除成功')
    await loadTemplates()
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除模板失败:', error)
      ElMessage.error('删除模板失败')
    }
  }
}

const handleTemplateSave = async (template: ExamPaperTemplate) => {
  saving.value = true
  try {
    if (isEditing.value && editingTemplate.value) {
      await examPaperTemplateApi.updateTemplate(editingTemplate.value.id, template)
      ElMessage.success('模板更新成功')
    } else {
      await examPaperTemplateApi.createTemplate(template)
      ElMessage.success('模板创建成功')
    }
    
    showCreateDialog.value = false
    resetForm()
    await loadTemplates()
    
  } catch (error) {
    console.error('保存模板失败:', error)
    ElMessage.error('保存模板失败')
  } finally {
    saving.value = false
  }
}

const handleRowClick = (template: ExamPaperTemplate) => {
  viewTemplate(template)
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  // 重新加载数据
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  // 重新加载数据
}

const resetForm = () => {
  editingTemplate.value = null
  isEditing.value = false
}

const resetApplyForm = () => {
  applyForm.examTitle = ''
  applyForm.examDescription = ''
  selectedTemplate.value = null
  applyFormRef.value?.resetFields()
}

// 工具方法
const getStatusType = (status: string): string => {
  const statusMap: Record<string, string> = {
    'DRAFT': 'info',
    'READY': 'success',
    'PUBLISHED': 'primary',
    'ARCHIVED': 'warning'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status: string): string => {
  const statusMap: Record<string, string> = {
    'DRAFT': '草稿',
    'READY': '就绪',
    'PUBLISHED': '已发布',
    'ARCHIVED': '已归档'
  }
  return statusMap[status] || status
}

const getTemplateTypeColor = (type: string): string => {
  const typeMap: Record<string, string> = {
    'MANUAL': 'primary',
    'AI_GENERATED': 'success',
    'DOCUMENT_EXTRACTED': 'warning',
    'COPIED': 'info'
  }
  return typeMap[type] || 'info'
}

const getTemplateTypeText = (type: string): string => {
  const typeMap: Record<string, string> = {
    'MANUAL': '手动创建',
    'AI_GENERATED': 'AI生成',
    'DOCUMENT_EXTRACTED': '文档提取',
    'COPIED': '复制'
  }
  return typeMap[type] || type
}

const formatDate = (dateString: string | Date): string => {
  const date = typeof dateString === 'string' ? new Date(dateString) : dateString
  return date.toLocaleDateString('zh-CN')
}

// 生命周期
onMounted(() => {
  loadTemplates()
})
</script>

<style scoped>
.exam-paper-template-management {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.header-content h1 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 24px;
}

.header-content p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.search-card {
  margin-bottom: 20px;
}

.template-content {
  min-height: 400px;
}

.grid-view {
  margin-bottom: 20px;
}

.template-card {
  height: 100%;
  transition: all 0.3s;
}

.template-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.public-template {
  border: 2px solid #e6a23c;
}

.template-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.template-title h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #303133;
}

.template-actions {
  display: flex;
  gap: 5px;
}

.template-content {
  padding: 0;
}

.template-description {
  color: #606266;
  font-size: 14px;
  margin-bottom: 15px;
  line-height: 1.5;
}

.template-info {
  margin-bottom: 15px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.info-row .label {
  color: #909399;
  font-size: 12px;
}

.template-stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 15px;
  padding: 10px 0;
  background: #f8f9fa;
  border-radius: 4px;
}

.stat-item {
  text-align: center;
}

.stat-item .number {
  display: block;
  font-size: 18px;
  font-weight: bold;
  color: #409eff;
}

.stat-item .label {
  font-size: 12px;
  color: #909399;
}

.template-status {
  display: flex;
  gap: 8px;
  margin-bottom: 15px;
}

.template-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
  border-top: 1px solid #ebeef5;
  padding-top: 10px;
}

.table-view {
  margin-bottom: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

:deep(.danger-item) {
  color: #f56c6c;
}

:deep(.danger-item:hover) {
  background-color: #fef0f0;
}
</style> 