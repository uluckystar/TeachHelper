<template>
  <div class="unified-template-management">
    <div class="page-header">
      <h1>试卷模板管理</h1>
      <p>统一管理试卷模板，支持多种创建方式和模板重用</p>
      
      <div class="header-actions">
        <el-button type="primary" @click="createTemplate">
          <el-icon><Plus /></el-icon>
          创建模板
        </el-button>
        <el-button type="success" @click="createFromDocument">
          <el-icon><Upload /></el-icon>
          从文档创建
        </el-button>
        <el-button @click="loadTemplates" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 搜索和过滤 -->
    <div class="search-filters">
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
            placeholder="筛选科目"
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
            v-model="templateTypeFilter"
            placeholder="筛选类型"
            clearable
            @change="handleFilter"
          >
            <el-option label="手动创建" value="MANUAL" />
            <el-option label="AI生成" value="AI_GENERATED" />
            <el-option label="文档提取" value="DOCUMENT_EXTRACTED" />
            <el-option label="复制" value="COPIED" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select
            v-model="statusFilter"
            placeholder="筛选状态"
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
              :type="viewMode === 'grid' ? 'primary' : 'default'"
              @click="viewMode = 'grid'"
            >
              <el-icon><Grid /></el-icon>
              网格视图
            </el-button>
            <el-button 
              :type="viewMode === 'table' ? 'primary' : 'default'"
              @click="viewMode = 'table'"
            >
              <el-icon><List /></el-icon>
              列表视图
            </el-button>
          </el-button-group>
        </el-col>
      </el-row>
    </div>

    <!-- 模板内容 -->
    <div class="template-content">
      <!-- 公开模板 -->
      <div class="template-section">
        <div class="section-header">
          <h2>公开模板</h2>
          <p>系统提供的标准模板，所有用户都可以使用</p>
        </div>
        
        <div v-if="viewMode === 'grid'" class="grid-view">
          <el-row :gutter="20" v-loading="loading">
            <el-col
              v-for="template in filteredPublicTemplates"
              :key="template.id"
              :span="8"
              style="margin-bottom: 20px"
            >
              <el-card class="template-card public-template" @click="viewTemplate(template)" style="cursor:pointer;">
                <template #header>
                  <div class="template-header">
                    <div class="template-title">
                      <h3>{{ template.name }}</h3>
                      <el-tag type="warning" size="small">公开模板</el-tag>
                    </div>
                    <div class="template-actions" @click.stop>
                      <el-dropdown @command="(command) => handleTemplateAction(command, template)">
                        <el-button type="text" size="small">
                          <el-icon><MoreFilled /></el-icon>
                        </el-button>
                        <template #dropdown>
                          <el-dropdown-menu>
                            <el-dropdown-item command="view">查看详情</el-dropdown-item>
                            <el-dropdown-item command="apply">应用到考试</el-dropdown-item>
                            <el-dropdown-item command="duplicate">复制模板</el-dropdown-item>
                          </el-dropdown-menu>
                        </template>
                      </el-dropdown>
                    </div>
                  </div>
                </template>
                
                <div class="template-info">
                  <p class="template-description">{{ template.description || '暂无描述' }}</p>
                  <div class="template-meta">
                    <el-tag :type="getTemplateTypeTag(template.templateType)" size="small">
                      {{ getTemplateTypeText(template.templateType) }}
                    </el-tag>
                    <el-tag :type="getStatusTag(template.status)" size="small">
                      {{ getStatusText(template.status) }}
                    </el-tag>
                  </div>
                  <div class="template-stats">
                    <span>题目数: {{ template.questionCount }}</span>
                    <span>总分: {{ template.totalScore }}</span>
                    <span>使用次数: {{ template.usageCount }}</span>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
        
        <div v-else class="table-view">
          <el-table :data="filteredPublicTemplates" v-loading="loading" style="width: 100%">
            <el-table-column prop="name" label="模板名称" min-width="200">
              <template #default="{ row }">
                <div class="template-name-cell">
                  <span>{{ row.name }}</span>
                  <el-tag type="warning" size="small">公开</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="subject" label="科目" width="100" />
            <el-table-column prop="templateType" label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getTemplateTypeTag(row.templateType)" size="small">
                  {{ getTemplateTypeText(row.templateType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusTag(row.status)" size="small">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="questionCount" label="题目数" width="80" />
            <el-table-column prop="totalScore" label="总分" width="80" />
            <el-table-column prop="usageCount" label="使用次数" width="100" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button type="text" size="small" @click="viewTemplate(row)">查看</el-button>
                <el-button type="text" size="small" @click="applyTemplate(row)">应用</el-button>
                <el-button type="text" size="small" @click="duplicateTemplate(row)">复制</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 私有模板 -->
      <div class="template-section">
        <div class="section-header">
          <h2>私有模板</h2>
          <p>您创建的私有模板，只有您可以管理</p>
        </div>
        
        <div v-if="viewMode === 'grid'" class="grid-view">
          <el-row :gutter="20" v-loading="loading">
            <el-col
              v-for="template in filteredPrivateTemplates"
              :key="template.id"
              :span="8"
              style="margin-bottom: 20px"
            >
              <el-card class="template-card private-template" @click="viewTemplate(template)" style="cursor:pointer;">
                <template #header>
                  <div class="template-header">
                    <div class="template-title">
                      <h3>{{ template.name }}</h3>
                      <el-tag type="info" size="small">私有模板</el-tag>
                    </div>
                    <div class="template-actions" @click.stop>
                      <el-dropdown @command="(command) => handleTemplateAction(command, template)">
                        <el-button type="text" size="small">
                          <el-icon><MoreFilled /></el-icon>
                        </el-button>
                        <template #dropdown>
                          <el-dropdown-menu>
                            <el-dropdown-item command="view">查看详情</el-dropdown-item>
                            <el-dropdown-item command="edit">编辑模板</el-dropdown-item>
                            <el-dropdown-item command="apply">应用到考试</el-dropdown-item>
                            <el-dropdown-item command="duplicate">复制模板</el-dropdown-item>
                            <el-dropdown-item command="delete" divided>删除模板</el-dropdown-item>
                          </el-dropdown-menu>
                        </template>
                      </el-dropdown>
                    </div>
                  </div>
                </template>
                
                <div class="template-info">
                  <p class="template-description">{{ template.description || '暂无描述' }}</p>
                  <div class="template-meta">
                    <el-tag :type="getTemplateTypeTag(template.templateType)" size="small">
                      {{ getTemplateTypeText(template.templateType) }}
                    </el-tag>
                    <el-tag :type="getStatusTag(template.status)" size="small">
                      {{ getStatusText(template.status) }}
                    </el-tag>
                  </div>
                  <div class="template-stats">
                    <span>题目数: {{ template.questionCount }}</span>
                    <span>总分: {{ template.totalScore }}</span>
                    <span>使用次数: {{ template.usageCount }}</span>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
        
        <div v-else class="table-view">
          <el-table :data="filteredPrivateTemplates" v-loading="loading" style="width: 100%">
            <el-table-column prop="name" label="模板名称" min-width="200">
              <template #default="{ row }">
                <div class="template-name-cell">
                  <span>{{ row.name }}</span>
                  <el-tag type="info" size="small">私有</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="subject" label="科目" width="100" />
            <el-table-column prop="templateType" label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getTemplateTypeTag(row.templateType)" size="small">
                  {{ getTemplateTypeText(row.templateType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusTag(row.status)" size="small">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="questionCount" label="题目数" width="80" />
            <el-table-column prop="totalScore" label="总分" width="80" />
            <el-table-column prop="usageCount" label="使用次数" width="100" />
            <el-table-column label="操作" width="250" fixed="right">
              <template #default="{ row }">
                <el-button type="text" size="small" @click="viewTemplate(row)">查看</el-button>
                <el-button type="text" size="small" @click="editTemplate(row)">编辑</el-button>
                <el-button type="text" size="small" @click="applyTemplate(row)">应用</el-button>
                <el-button type="text" size="small" @click="duplicateTemplate(row)">复制</el-button>
                <el-button type="text" size="small" @click="deleteTemplate(row)" style="color: #f56c6c;">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <!-- 应用模板对话框 -->
    <el-dialog v-model="applyDialogVisible" title="应用模板到考试" width="500px">
      <el-form :model="applyForm" ref="applyFormRef" :rules="applyRules" label-width="100px">
        <el-form-item label="考试标题" prop="examTitle">
          <el-input v-model="applyForm.examTitle" placeholder="请输入考试标题" />
        </el-form-item>
        <el-form-item label="考试描述" prop="examDescription">
          <el-input 
            v-model="applyForm.examDescription" 
            type="textarea" 
            :rows="3"
            placeholder="请输入考试描述（可选）" 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmApply" :loading="applying">确认应用</el-button>
      </template>
    </el-dialog>

    <!-- 从文档创建模板对话框 -->
    <el-dialog v-model="createFromDocumentDialogVisible" title="从学习通文档创建试卷模板" width="700px">
      <div class="create-from-document-form">
        <el-alert
          title="文档要求"
          type="info"
          :closable="false"
          style="margin-bottom: 20px"
        >
          <p>支持上传学习通导出的Word文档（.doc/.docx格式），系统将自动解析题目内容并生成试卷模板。</p>
          <p>请确保文档包含完整的题目信息，解析后的模板可以进一步编辑和完善。</p>
        </el-alert>

        <el-form :model="createFromDocumentForm" ref="createFromDocumentFormRef" :rules="createFromDocumentRules" label-width="120px">
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="选择文档" prop="file">
                <el-upload
                  ref="uploadRef"
                  :before-upload="beforeDocumentUpload"
                  :on-change="handleDocumentChange"
                  :auto-upload="false"
                  accept=".doc,.docx"
                  drag
                  class="document-upload"
                >
                  <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                  <div class="el-upload__text">
                    将学习通文档拖到此处，或<em>点击上传</em>
                  </div>
                  <template #tip>
                    <div class="el-upload__tip">
                      支持 .doc/.docx 格式，文件大小不超过 10MB
                    </div>
                  </template>
                </el-upload>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="模板名称" prop="templateName">
                <el-input v-model="createFromDocumentForm.templateName" placeholder="请输入模板名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="科目" prop="subject">
                <el-select v-model="createFromDocumentForm.subject" placeholder="请选择科目" style="width: 100%">
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
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="年级" prop="gradeLevel">
                <el-select v-model="createFromDocumentForm.gradeLevel" placeholder="请选择年级" style="width: 100%">
                  <el-option label="小学" value="小学" />
                  <el-option label="初中" value="初中" />
                  <el-option label="高中" value="高中" />
                  <el-option label="本科" value="本科" />
                  <el-option label="研究生" value="研究生" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="总分" prop="totalScore">
                <el-input-number 
                  v-model="createFromDocumentForm.totalScore" 
                  :min="1" 
                  :max="1000" 
                  :precision="0"
                  style="width: 100%" 
                  placeholder="请输入总分"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="考试时长(分钟)" prop="duration">
                <el-input-number 
                  v-model="createFromDocumentForm.duration" 
                  :min="1" 
                  :max="600" 
                  :precision="0"
                  style="width: 100%" 
                  placeholder="请输入考试时长"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="模板描述" prop="description">
                <el-input 
                  v-model="createFromDocumentForm.description" 
                  placeholder="请输入模板描述（可选）" 
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createFromDocumentDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmCreateFromDocument" :loading="creatingFromDocument">
            <el-icon v-if="!creatingFromDocument"><Plus /></el-icon>
            {{ creatingFromDocument ? '正在创建...' : '确认创建' }}
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
import { 
  Plus, Refresh, Search, Grid, List, MoreFilled, Upload, UploadFilled 
} from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { examPaperTemplateApi, type ExamPaperTemplate } from '@/api/examPaperTemplate'

const router = useRouter()

// 响应式数据
const loading = ref(false)
const applying = ref(false)
const creatingFromDocument = ref(false)
const viewMode = ref<'grid' | 'table'>('grid')

// 搜索和过滤
const searchKeyword = ref('')
const subjectFilter = ref('')
const templateTypeFilter = ref('')
const statusFilter = ref('')

// 模板数据
const publicTemplates = ref<ExamPaperTemplate[]>([])
const privateTemplates = ref<ExamPaperTemplate[]>([])

// 对话框状态
const applyDialogVisible = ref(false)
const createFromDocumentDialogVisible = ref(false)
const selectedTemplate = ref<ExamPaperTemplate | null>(null)
const applyFormRef = ref<FormInstance>()
const createFromDocumentFormRef = ref<FormInstance>()
const uploadRef = ref()
const selectedDocumentFile = ref<File | null>(null)

// 应用表单
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

// 从文档创建表单
const createFromDocumentForm = reactive({
  file: null,
  templateName: '',
  subject: '',
  gradeLevel: '',
  description: '',
  totalScore: 100,
  duration: 60
})

const createFromDocumentRules: FormRules = {
  file: [
    { required: true, message: '请选择要上传的文档', trigger: 'change' }
  ],
  templateName: [
    { required: true, message: '请输入模板名称', trigger: 'blur' },
    { min: 2, max: 200, message: '模板名称长度在2-200个字符', trigger: 'blur' }
  ],
  subject: [
    { required: true, message: '请选择科目', trigger: 'change' }
  ],
  gradeLevel: [
    { required: true, message: '请选择年级', trigger: 'change' }
  ],
  totalScore: [
    { required: true, message: '请输入总分', trigger: 'blur' },
    { 
      validator: (rule, value, callback) => {
        if (value === null || value === undefined) {
          callback(new Error('请输入总分'))
        } else if (value < 1 || value > 1000) {
          callback(new Error('总分应在1-1000之间'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ],
  duration: [
    { required: true, message: '请输入考试时长', trigger: 'blur' },
    { 
      validator: (rule, value, callback) => {
        if (value === null || value === undefined) {
          callback(new Error('请输入考试时长'))
        } else if (value < 1 || value > 600) {
          callback(new Error('考试时长应在1-600分钟之间'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ]
}

// 计算属性
const filteredPublicTemplates = computed(() => {
  return filterTemplates(publicTemplates.value)
})

const filteredPrivateTemplates = computed(() => {
  return filterTemplates(privateTemplates.value)
})

const filterTemplates = (templates: ExamPaperTemplate[]) => {
  return templates.filter(template => {
    const matchesKeyword = !searchKeyword.value || 
      template.name.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      template.description?.toLowerCase().includes(searchKeyword.value.toLowerCase())
    
    const matchesSubject = !subjectFilter.value || 
      template.subject === subjectFilter.value
    
    const matchesType = !templateTypeFilter.value || 
      template.templateType === templateTypeFilter.value
    
    const matchesStatus = !statusFilter.value || 
      template.status === statusFilter.value
    
    return matchesKeyword && matchesSubject && matchesType && matchesStatus
  })
}

// 方法
const loadTemplates = async () => {
  try {
    loading.value = true
    const [publicResponse, privateResponse] = await Promise.all([
      examPaperTemplateApi.getPublicTemplates(),
      examPaperTemplateApi.getUserTemplates()
    ])
    
    publicTemplates.value = publicResponse.data
    privateTemplates.value = privateResponse.data
  } catch (error) {
    console.error('加载模板失败:', error)
    ElMessage.error('加载模板失败')
  } finally {
    loading.value = false
  }
}

const createTemplate = () => {
  router.push('/templates/create')
}

const createFromDocument = () => {
  createFromDocumentDialogVisible.value = true
  resetCreateFromDocumentForm()
}

const beforeDocumentUpload = (file: File) => {
  const isValidType = ['application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'].includes(file.type)
  if (!isValidType) {
    ElMessage.error('只能上传 .doc 或 .docx 格式的文件!')
    return false
  }
  
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB!')
    return false
  }
  
  return false // 阻止自动上传
}

const handleDocumentChange = (file: any) => {
  selectedDocumentFile.value = file.raw
  createFromDocumentForm.file = file.raw
}

const confirmCreateFromDocument = async () => {
  if (!createFromDocumentFormRef.value || !selectedDocumentFile.value) return
  
  try {
    const valid = await createFromDocumentFormRef.value.validate()
    if (!valid) return
  } catch {
    return
  }

  creatingFromDocument.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedDocumentFile.value)
    formData.append('templateName', createFromDocumentForm.templateName)
    formData.append('subject', createFromDocumentForm.subject)
    formData.append('gradeLevel', createFromDocumentForm.gradeLevel)
    formData.append('description', createFromDocumentForm.description)
    formData.append('totalScore', String(createFromDocumentForm.totalScore))
    formData.append('duration', String(createFromDocumentForm.duration))
    
    const response = await examPaperTemplateApi.createFromDocument(formData)
    ElMessage.success('模板创建成功')
    createFromDocumentDialogVisible.value = false
    resetCreateFromDocumentForm()
    await loadTemplates()
    
    // 跳转到模板详情页
    router.push(`/templates/${response.data.id}`)
  } catch (error) {
    console.error('从文档创建模板失败:', error)
    ElMessage.error('从文档创建模板失败')
  } finally {
    creatingFromDocument.value = false
  }
}

const resetCreateFromDocumentForm = () => {
  createFromDocumentForm.templateName = ''
  createFromDocumentForm.subject = ''
  createFromDocumentForm.gradeLevel = ''
  createFromDocumentForm.description = ''
  createFromDocumentForm.totalScore = 100
  createFromDocumentForm.duration = 60
  selectedDocumentFile.value = null
  createFromDocumentFormRef.value?.resetFields()
  uploadRef.value?.clearFiles()
}

const viewTemplate = (template: ExamPaperTemplate) => {
  router.push(`/templates/${template.id}`)
}

const editTemplate = (template: ExamPaperTemplate) => {
  router.push(`/templates/${template.id}/edit`)
}

const applyTemplate = (template: ExamPaperTemplate) => {
  selectedTemplate.value = template
  applyForm.examTitle = `${template.name} - ${formatDate(new Date())}`
  applyForm.examDescription = template.description || ''
  applyDialogVisible.value = true
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

const handleTemplateAction = async (command: string, template: ExamPaperTemplate) => {
  switch (command) {
    case 'view':
      viewTemplate(template)
      break
    case 'edit':
      editTemplate(template)
      break
    case 'apply':
      applyTemplate(template)
      break
    case 'duplicate':
      await duplicateTemplate(template)
      break
    case 'delete':
      await deleteTemplate(template)
      break
  }
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
    applyDialogVisible.value = false
    resetApplyForm()
    router.push(`/exams/${exam.id}`)
  } catch (error) {
    console.error('应用模板失败:', error)
    ElMessage.error('应用模板失败')
  } finally {
    applying.value = false
  }
}

const resetApplyForm = () => {
  applyForm.examTitle = ''
  applyForm.examDescription = ''
  applyFormRef.value?.resetFields()
}

const handleSearch = () => {
  // 实时搜索，由计算属性处理
}

const handleFilter = () => {
  // 实时过滤，由计算属性处理
}

// 工具方法
const getTemplateTypeTag = (type: string): 'primary' | 'success' | 'warning' | 'info' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info'> = {
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

const getStatusTag = (status: string): 'info' | 'success' | 'primary' | 'warning' => {
  const statusMap: Record<string, 'info' | 'success' | 'primary' | 'warning'> = {
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

const formatDate = (date: Date): string => {
  return date.toLocaleDateString('zh-CN')
}

// 生命周期
onMounted(() => {
  loadTemplates()
})
</script>

<style scoped>
.unified-template-management {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
  text-align: center;
}

.page-header h1 {
  margin-bottom: 8px;
  color: #303133;
}

.page-header p {
  color: #606266;
  margin-bottom: 16px;
}

.header-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.search-filters {
  margin-bottom: 20px;
  padding: 20px;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.template-content {
  margin-top: 20px;
}

.template-section {
  margin-bottom: 40px;
}

.section-header {
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #e4e7ed;
}

.section-header h2 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 18px;
}

.section-header p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.grid-view {
  min-height: 200px;
}

.template-card {
  height: 100%;
  transition: all 0.3s ease;
}

.template-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.template-card.public-template {
  border-left: 4px solid #e6a23c;
}

.template-card.private-template {
  border-left: 4px solid #409eff;
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

.template-info {
  padding: 12px 0;
}

.template-description {
  margin: 0 0 12px 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.template-meta {
  margin-bottom: 12px;
}

.template-meta .el-tag {
  margin-right: 8px;
}

.template-stats {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}

.template-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.table-view {
  background: white;
  border-radius: 8px;
  overflow: hidden;
}

.template-card { transition: box-shadow 0.2s; }
.template-card:hover { box-shadow: 0 2px 12px #aaa; }

.create-from-document-form {
  max-height: 70vh;
  overflow-y: auto;
}

.document-upload {
  width: 100%;
}

.document-upload .el-upload-dragger {
  width: 100%;
  height: 120px;
}

.dialog-footer {
  text-align: right;
}

.el-form-item__content .el-input-number {
  width: 100%;
}
</style> 