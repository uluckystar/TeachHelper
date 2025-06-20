<template>
  <div class="template-manager">
    <!-- 搜索和过滤 -->
    <div class="search-filters">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索模板名称"
            @input="handleSearch"
            clearable
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="6">
          <el-select
            v-model="subjectFilter"
            placeholder="筛选科目"
            @change="handleFilter"
            clearable
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
        <el-col :span="6">
          <el-select
            v-model="gradeLevelFilter"
            placeholder="筛选年级"
            @change="handleFilter"
            clearable
          >
            <el-option label="小学" value="小学" />
            <el-option label="初中" value="初中" />
            <el-option label="高中" value="高中" />
            <el-option label="大学" value="大学" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="loadTemplates" :loading="loading">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 模板列表 -->
    <div class="template-list">
      <el-row :gutter="20" v-loading="loading">
        <el-col
          v-for="template in filteredTemplates"
          :key="template.id"
          :span="8"
          style="margin-bottom: 20px"
        >
          <el-card class="template-card" :class="{ 'public-template': !template.createdBy }">
            <template #header>
              <div class="template-header">
                <div class="template-title">
                  <h3>{{ template.name }}</h3>
                  <el-tag
                    v-if="!template.createdBy"
                    type="warning"
                    size="small"
                  >
                    公共模板
                  </el-tag>
                </div>
                <div class="template-actions">
                  <el-dropdown @command="handleTemplateAction">
                    <el-button text>
                      <el-icon><MoreFilled /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item
                          :command="{ action: 'generate', template }"
                          :disabled="loading"
                        >
                          <el-icon><MagicStick /></el-icon>
                          生成试卷
                        </el-dropdown-item>
                        <el-dropdown-item
                          :command="{ action: 'duplicate', template }"
                        >
                          <el-icon><CopyDocument /></el-icon>
                          复制模板
                        </el-dropdown-item>
                        <el-dropdown-item
                          v-if="template.createdBy"
                          :command="{ action: 'edit', template }"
                        >
                          <el-icon><Edit /></el-icon>
                          编辑模板
                        </el-dropdown-item>
                        <el-dropdown-item
                          v-if="template.createdBy"
                          :command="{ action: 'delete', template }"
                          divided
                        >
                          <el-icon><Delete /></el-icon>
                          删除模板
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
                <div class="info-row" v-if="template.timeLimit">
                  <span class="label">时间：</span>
                  <span>{{ template.timeLimit }} 分钟</span>
                </div>
              </div>

              <!-- 题型配置预览 -->
              <div class="question-types-preview" v-if="parsedQuestionConfig(template)">
                <h4>题型配置</h4>
                <div class="question-type-tags">
                  <el-tag
                    v-for="(config, index) in parsedQuestionConfig(template)"
                    :key="index"
                    class="question-type-tag"
                    size="small"
                  >
                    {{ getQuestionTypeText(config.type) }}: {{ config.count }}题/{{ config.scorePerQuestion }}分
                  </el-tag>
                </div>
              </div>

              <div class="template-footer">
                <div class="created-info">
                  <span class="created-time">
                    {{ formatDate(template.createdAt) }}
                  </span>
                </div>
                <div class="quick-actions">
                  <el-button
                    type="primary"
                    size="small"
                    @click="showGenerateDialog(template)"
                    :loading="loading"
                  >
                    <el-icon><MagicStick /></el-icon>
                    生成试卷
                  </el-button>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 空状态 -->
      <el-empty
        v-if="!loading && filteredTemplates.length === 0"
        description="暂无模板，去创建第一个模板吧"
      >
        <el-button type="primary" @click="$router.push('/paper/generation')">
          创建模板
        </el-button>
      </el-empty>
    </div>

    <!-- 生成试卷对话框 -->
    <el-dialog
      v-model="generateDialogVisible"
      title="基于模板生成试卷"
      width="500px"
    >
      <el-form :model="generateForm" label-width="100px">
        <el-form-item label="模板名称">
          <el-input :value="selectedTemplate?.name" disabled />
        </el-form-item>
        <el-form-item label="试卷标题" required>
          <el-input
            v-model="generateForm.title"
            placeholder="请输入试卷标题"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="generateDialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            @click="confirmGenerate"
            :loading="generating"
            :disabled="!generateForm.title"
          >
            生成试卷
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Refresh, MagicStick, CopyDocument, Edit, Delete, MoreFilled
} from '@element-plus/icons-vue'
import { paperGenerationApi, type PaperGenerationTemplate } from '@/api/knowledge'

interface Props {
  loading?: boolean
}

interface Emits {
  (e: 'generate-from-template', template: PaperGenerationTemplate, title: string): void
  (e: 'edit-template', template: PaperGenerationTemplate): void
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<Emits>()

// 响应式数据
const templates = ref<PaperGenerationTemplate[]>([])
const loading = ref(false)
const generating = ref(false)
const searchKeyword = ref('')
const subjectFilter = ref('')
const gradeLevelFilter = ref('')
const generateDialogVisible = ref(false)
const selectedTemplate = ref<PaperGenerationTemplate | null>(null)

const generateForm = reactive({
  title: ''
})

// 计算属性
const filteredTemplates = computed(() => {
  return templates.value.filter(template => {
    const matchesKeyword = !searchKeyword.value || 
      template.name.toLowerCase().includes(searchKeyword.value.toLowerCase())
    
    const matchesSubject = !subjectFilter.value || 
      template.subject === subjectFilter.value
    
    const matchesGradeLevel = !gradeLevelFilter.value || 
      template.gradeLevel.includes(gradeLevelFilter.value)
    
    return matchesKeyword && matchesSubject && matchesGradeLevel
  })
})

// 方法
const loadTemplates = async () => {
  try {
    loading.value = true
    const response = await paperGenerationApi.getTemplates()
    templates.value = response.data
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

const handleTemplateAction = async (command: { action: string, template: PaperGenerationTemplate }) => {
  const { action, template } = command
  
  switch (action) {
    case 'generate':
      showGenerateDialog(template)
      break
      
    case 'duplicate':
      await duplicateTemplate(template)
      break
      
    case 'edit':
      emit('edit-template', template)
      break
      
    case 'delete':
      await deleteTemplate(template)
      break
  }
}

const showGenerateDialog = (template: PaperGenerationTemplate) => {
  selectedTemplate.value = template
  generateForm.title = `${template.name} - ${formatDate(new Date())}`
  generateDialogVisible.value = true
}

const confirmGenerate = () => {
  if (!selectedTemplate.value || !generateForm.title) {
    return
  }
  
  emit('generate-from-template', selectedTemplate.value, generateForm.title)
  generateDialogVisible.value = false
}

const duplicateTemplate = async (template: PaperGenerationTemplate) => {
  try {
    await paperGenerationApi.duplicateTemplate(template.id)
    ElMessage.success('模板复制成功')
    await loadTemplates()
  } catch (error) {
    console.error('复制模板失败:', error)
    ElMessage.error('复制模板失败')
  }
}

const deleteTemplate = async (template: PaperGenerationTemplate) => {
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
    
    await paperGenerationApi.deleteTemplate(template.id)
    ElMessage.success('模板删除成功')
    await loadTemplates()
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除模板失败:', error)
      ElMessage.error('删除模板失败')
    }
  }
}

const parsedQuestionConfig = (template: PaperGenerationTemplate) => {
  try {
    return JSON.parse(template.questionConfig)
  } catch (error) {
    return null
  }
}

const getQuestionTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'TRUE_FALSE': '判断题',
    'SHORT_ANSWER': '简答题',
    'ESSAY': '论述题',
    'CODING': '编程题',
    'CASE_ANALYSIS': '案例分析题'
  }
  return typeMap[type] || type
}

const formatDate = (dateString: string | Date) => {
  const date = typeof dateString === 'string' ? new Date(dateString) : dateString
  return date.toLocaleDateString('zh-CN')
}

// 生命周期
onMounted(() => {
  loadTemplates()
})
</script>

<style scoped>
.template-manager {
  max-width: 100%;
}

.search-filters {
  margin-bottom: 20px;
  padding: 20px;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.template-list {
  min-height: 400px;
}

.template-card {
  height: 100%;
  transition: all 0.3s ease;
  cursor: pointer;
}

.template-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.public-template {
  border-left: 4px solid #f39c12;
}

.template-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.template-title h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.template-actions {
  flex-shrink: 0;
}

.template-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.template-description {
  color: #606266;
  font-size: 14px;
  margin-bottom: 16px;
  line-height: 1.5;
  flex-grow: 0;
}

.template-info {
  margin-bottom: 16px;
}

.info-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
}

.info-row .label {
  color: #909399;
  min-width: 50px;
  margin-right: 8px;
}

.question-types-preview {
  margin-bottom: 16px;
  flex-grow: 1;
}

.question-types-preview h4 {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #606266;
}

.question-type-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.question-type-tag {
  font-size: 12px;
}

.template-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.created-info {
  font-size: 12px;
  color: #c0c4cc;
}

.quick-actions {
  flex-shrink: 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

:deep(.el-card__header) {
  padding: 16px 20px;
  background-color: #fafafa;
}

:deep(.el-card__body) {
  padding: 20px;
  height: calc(100% - 60px);
}
</style>
