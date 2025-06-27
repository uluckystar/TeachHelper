<template>
  <div class="exam-template-container">
    <div class="header">
      <h2>试卷模板管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showExtractDialog = true" icon="Plus">
          提取新模板
        </el-button>
      </div>
    </div>

    <!-- 模板列表 -->
    <div class="template-list">
      <el-table 
        :data="templates" 
        v-loading="loading"
        style="width: 100%"
        @row-click="handleRowClick">
        <el-table-column prop="templateName" label="模板名称" width="200">
          <template #default="{ row }">
            <el-link @click="viewTemplate(row)" type="primary">
              {{ row.templateName }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="subject" label="科目" width="120" />
        <el-table-column prop="examTitle" label="考试标题" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalQuestions" label="题目总数" width="100" />
        <el-table-column prop="matchedQuestions" label="已匹配" width="100">
          <template #default="{ row }">
            <span :class="{ 'text-success': row.matchedQuestions === row.totalQuestions }">
              {{ row.matchedQuestions }}/{{ row.totalQuestions }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="匹配进度" width="120">
          <template #default="{ row }">
            <el-progress 
              :percentage="getMatchingProgress(row)"
              :color="getProgressColor(row)"
              :stroke-width="8"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button size="small" @click="viewTemplate(row)">详情</el-button>
              <el-button 
                size="small" 
                type="success"
                v-if="row.status === 'READY'"
                @click="showApplyTemplateDialog(row)">
                应用
              </el-button>
              <el-button 
                size="small" 
                type="danger"
                @click="deleteTemplate(row)">
                删除
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 提取模板对话框 -->
    <el-dialog
      v-model="showExtractDialog"
      title="从学习通文档提取模板"
      width="600px"
      @close="resetExtractForm">
      <el-form :model="extractForm" :rules="extractRules" ref="extractFormRef" label-width="120px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input 
            v-model="extractForm.templateName" 
            placeholder="请输入模板名称，如：高等数学期末考试"
          />
        </el-form-item>
        <el-form-item label="科目" prop="subject">
          <el-select 
            v-model="extractForm.subject" 
            placeholder="选择科目"
            style="width: 100%"
            filterable
            allow-create
            @change="onSubjectChange">
            <el-option 
              v-for="subject in subjects" 
              :key="subject" 
              :label="subject" 
              :value="subject"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="班级文件夹" prop="classFolders">
          <el-select 
            v-model="extractForm.classFolders" 
            placeholder="选择要分析的班级文件夹"
            style="width: 100%"
            multiple
            filterable>
            <el-option 
              v-for="folder in availableClasses" 
              :key="folder" 
              :label="folder" 
              :value="folder"
            />
          </el-select>
          <div class="form-tip">
            选择多个班级的学习通文档，系统将分析题目结构并创建模板
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showExtractDialog = false">取消</el-button>
        <el-button type="primary" @click="extractTemplate" :loading="extracting">
          开始提取
        </el-button>
      </template>
    </el-dialog>

    <!-- 应用模板对话框 -->
    <el-dialog
      v-model="showApplyDialogVisible"
      title="应用模板导入答案"
      width="500px">
      <div v-if="selectedTemplate">
        <div class="template-info">
          <h4>{{ selectedTemplate.templateName }}</h4>
          <p>科目：{{ selectedTemplate.subject }}</p>
          <p>题目数：{{ selectedTemplate.totalQuestions }}题</p>
          <p>已匹配：{{ selectedTemplate.matchedQuestions }}题</p>
        </div>
        
        <el-form :model="applyForm" :rules="applyRules" ref="applyFormRef" label-width="120px">
          <el-form-item label="目标考试" prop="examId">
            <el-select 
              v-model="applyForm.examId" 
              placeholder="选择要导入答案的考试"
              style="width: 100%"
              filterable>
              <el-option 
                v-for="exam in exams" 
                :key="exam.id" 
                :label="exam.title" 
                :value="exam.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="班级文件夹" prop="classFolder">
            <el-select 
              v-model="applyForm.classFolder" 
              placeholder="选择班级文件夹"
              style="width: 100%"
              filterable>
              <el-option 
                v-for="folder in availableClasses" 
                :key="folder" 
                :label="folder" 
                :value="folder"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showApplyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="applyTemplate" :loading="applying">
          导入答案
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { examTemplateApi, type ExamTemplateResponse } from '@/api/examTemplate'
import { answerApi } from '@/api/answer'
import { examApi } from '@/api/exam'

const router = useRouter()

// 响应式数据
const loading = ref(false)
const extracting = ref(false)
const applying = ref(false)
const templates = ref<ExamTemplateResponse[]>([])
const subjects = ref<string[]>([])
const availableClasses = ref<string[]>([])
const exams = ref<any[]>([])

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 对话框控制
const showExtractDialog = ref(false)
const showApplyDialogVisible = ref(false)
const selectedTemplate = ref<ExamTemplateResponse | null>(null)

// 表单数据
const extractForm = reactive({
  templateName: '',
  subject: '',
  classFolders: [] as string[]
})

const applyForm = reactive({
  examId: null as number | null,
  classFolder: ''
})

// 表单引用
const extractFormRef = ref<FormInstance>()
const applyFormRef = ref<FormInstance>()

// 表单验证规则
const extractRules: FormRules = {
  templateName: [
    { required: true, message: '请输入模板名称', trigger: 'blur' }
  ],
  subject: [
    { required: true, message: '请选择科目', trigger: 'change' }
  ],
  classFolders: [
    { required: true, message: '请选择至少一个班级文件夹', trigger: 'change' }
  ]
}

const applyRules: FormRules = {
  examId: [
    { required: true, message: '请选择目标考试', trigger: 'change' }
  ],
  classFolder: [
    { required: true, message: '请选择班级文件夹', trigger: 'change' }
  ]
}

// 计算属性函数
const getMatchingProgress = (template: ExamTemplateResponse) => {
  if (template.totalQuestions === 0) return 0
  return Math.round((template.matchedQuestions / template.totalQuestions) * 100)
}

const getProgressColor = (template: ExamTemplateResponse) => {
  const progress = getMatchingProgress(template)
  if (progress === 100) return '#67c23a'
  if (progress >= 80) return '#e6a23c'
  return '#f56c6c'
}

const getStatusType = (status: string) => {
  const statusMap: Record<string, string> = {
    'DRAFT': 'info',
    'READY': 'success', 
    'APPLIED': 'warning',
    'ARCHIVED': 'info'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'DRAFT': '草稿',
    'READY': '就绪',
    'APPLIED': '已应用', 
    'ARCHIVED': '已归档'
  }
  return statusMap[status] || status
}

// 方法
const loadTemplates = async () => {
  loading.value = true
  try {
    const response = await examTemplateApi.getUserTemplates(currentPage.value - 1, pageSize.value)
    templates.value = response.data.content
    total.value = response.data.totalElements
  } catch (error) {
    ElMessage.error('加载模板列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadSubjects = async () => {
  try {
    const response = await answerApi.getLearningSubjects()
    subjects.value = response
  } catch (error) {
    console.error('加载科目列表失败:', error)
  }
}

const loadAvailableClasses = async (subject?: string) => {
  if (!subject) {
    availableClasses.value = []
    return
  }
  try {
    const response = await answerApi.getLearningSubjectClasses(subject)
    availableClasses.value = response
  } catch (error) {
    console.error('加载班级列表失败:', error)
  }
}

const loadExams = async () => {
  try {
    const response = await examApi.getExams({ page: 0, size: 100 })
    exams.value = response.data.content
  } catch (error) {
    console.error('加载考试列表失败:', error)
  }
}

const extractTemplate = async () => {
  if (!extractFormRef.value) return
  
  try {
    const valid = await extractFormRef.value.validate()
    if (!valid) return
  } catch {
    return
  }

  extracting.value = true
  try {
    await examTemplateApi.extractTemplate(extractForm)
    ElMessage.success('模板提取成功')
    showExtractDialog.value = false
    resetExtractForm()
    loadTemplates()
  } catch (error) {
    ElMessage.error('模板提取失败')
    console.error(error)
  } finally {
    extracting.value = false
  }
}

const applyTemplate = async () => {
  if (!applyFormRef.value || !selectedTemplate.value) return
  
  try {
    const valid = await applyFormRef.value.validate()
    if (!valid) return
  } catch {
    return
  }

  applying.value = true
  try {
    await examTemplateApi.importAnswersWithTemplate({
      templateId: selectedTemplate.value.id,
      examId: applyForm.examId!,
      subject: selectedTemplate.value.subject,
      classFolder: applyForm.classFolder
    })
    ElMessage.success('答案导入成功')
    showApplyDialogVisible.value = false
    resetApplyForm()
  } catch (error) {
    ElMessage.error('答案导入失败')
    console.error(error)
  } finally {
    applying.value = false
  }
}

const viewTemplate = (template: ExamTemplateResponse) => {
  router.push(`/exam-templates/${template.id}`)
}

const showApplyTemplateDialog = (template: ExamTemplateResponse) => {
  selectedTemplate.value = template
  showApplyDialogVisible.value = true
  loadExams()
  loadAvailableClasses(template.subject)
}

const deleteTemplate = async (template: ExamTemplateResponse) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除模板"${template.templateName}"吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await examTemplateApi.deleteTemplate(template.id)
    ElMessage.success('删除成功')
    loadTemplates()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
      console.error(error)
    }
  }
}

const resetExtractForm = () => {
  extractForm.templateName = ''
  extractForm.subject = ''
  extractForm.classFolders = []
  availableClasses.value = []
  extractFormRef.value?.resetFields()
}

const resetApplyForm = () => {
  applyForm.examId = null
  applyForm.classFolder = ''
  applyFormRef.value?.resetFields()
}

const handleRowClick = (template: ExamTemplateResponse) => {
  viewTemplate(template)
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  loadTemplates()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  loadTemplates()
}

const formatTime = (timeStr: string) => {
  return new Date(timeStr).toLocaleString()
}

// 监听科目变化
const onSubjectChange = () => {
  loadAvailableClasses(extractForm.subject)
}

// 初始化
onMounted(() => {
  loadTemplates()
  loadSubjects()
})
</script>

<style scoped>
.exam-template-container {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.template-list {
  background: white;
  border-radius: 8px;
  overflow: hidden;
}

.pagination {
  padding: 20px;
  text-align: right;
  border-top: 1px solid #ebeef5;
}

.template-info {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 6px;
  margin-bottom: 20px;
}

.template-info h4 {
  margin: 0 0 8px 0;
  color: #303133;
}

.template-info p {
  margin: 4px 0;
  color: #606266;
  font-size: 14px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.text-success {
  color: #67c23a;
  font-weight: bold;
}

:deep(.el-table__row) {
  cursor: pointer;
}

:deep(.el-table__row:hover) {
  background-color: #f5f7fa;
}
</style>