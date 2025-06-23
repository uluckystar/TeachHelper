<template>
  <div class="question-bank-detail">
    <div class="page-header">
      <el-breadcrumb>
        <el-breadcrumb-item :to="{ path: '/question-banks' }">题目库管理</el-breadcrumb-item>
        <el-breadcrumb-item>{{ questionBank?.name || '题目库详情' }}</el-breadcrumb-item>
      </el-breadcrumb>
      
      <div class="header-actions">
        <el-button type="primary" icon="Edit" @click="editQuestionBank" v-if="canEdit">
          编辑题目库
        </el-button>
        <el-button type="success" icon="Plus" @click="addQuestionsDialog = true" v-if="canEdit">
          添加题目
        </el-button>
        <el-button icon="Back" @click="goBack">
          返回
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="content">
      <!-- 题目库信息 -->
      <el-card v-if="questionBank" class="bank-info-card">
        <div class="bank-info">
          <div class="bank-header">
            <h2 class="bank-title">{{ questionBank.name }}</h2>
            <el-tag :type="questionBank.isPublic ? 'success' : 'info'">
              {{ questionBank.isPublic ? '公开' : '私有' }}
            </el-tag>
          </div>
          
          <p class="bank-description">{{ questionBank.description || '暂无描述' }}</p>
          
          <div class="bank-meta">
            <div class="meta-item" v-if="questionBank.subject">
              <strong>学科：</strong>{{ questionBank.subject }}
            </div>
            <div class="meta-item" v-if="questionBank.gradeLevel">
              <strong>年级：</strong>{{ questionBank.gradeLevel }}
            </div>
            <div class="meta-item">
              <strong>题目数量：</strong>{{ total }}
            </div>
            <div class="meta-item">
              <strong>创建时间：</strong>{{ formatDate(questionBank.createdAt) }}
            </div>
          </div>
        </div>
      </el-card>

      <!-- 题目列表 -->
      <el-card class="questions-card">
        <template #header>
          <div class="card-header">
            <span>题目列表 ({{ total }})</span>
            <div class="header-actions">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索题目..."
                style="width: 300px"
                @keyup.enter="searchQuestions"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-button icon="Refresh" @click="loadQuestions">
                刷新
              </el-button>
            </div>
          </div>
        </template>

        <el-table 
          :data="questions" 
          v-loading="questionsLoading"
          empty-text="暂无题目"
        >
          <el-table-column prop="title" label="题目标题" min-width="250" show-overflow-tooltip />
          <el-table-column prop="questionType" label="题目类型" width="120">
            <template #default="{ row }">
              <el-tag :type="getQuestionTypeTag(row.questionType) as any">
                {{ getQuestionTypeText(row.questionType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="maxScore" label="满分" width="80" />
          <el-table-column prop="examTitle" label="所属考试" width="180" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="创建时间" width="150">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" v-if="canEdit">
            <template #default="{ row }">
              <el-button-group>
                <el-button size="small" @click="viewQuestion(row.id)">
                  查看
                </el-button>
                <el-button size="small" type="primary" @click="editQuestion(row.id)">
                  编辑
                </el-button>
                <el-button size="small" type="danger" @click="removeQuestion(row.id)">
                  移除
                </el-button>
              </el-button-group>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-container">
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
    </div>

    <!-- 添加题目对话框 -->
    <el-dialog
      v-model="addQuestionsDialog"
      title="添加题目到题目库"
      width="800px"
    >
      <div class="add-questions-content">
        <el-input
          v-model="questionSearchKeyword"
          placeholder="搜索题目..."
          @keyup.enter="searchAvailableQuestions"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-table
          :data="availableQuestions"
          v-loading="availableQuestionsLoading"
          style="margin-top: 16px"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="title" label="题目标题" min-width="200" show-overflow-tooltip />
          <el-table-column prop="questionType" label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="getQuestionTypeTag(row.questionType) as any" size="small">
                {{ getQuestionTypeText(row.questionType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="examTitle" label="所属考试" width="150" show-overflow-tooltip />
          <el-table-column prop="maxScore" label="满分" width="80" />
        </el-table>

        <div class="pagination-container">
          <el-pagination
            v-model:current-page="availableCurrentPage"
            v-model:page-size="availablePageSize"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            :total="availableTotal"
            @size-change="handleAvailableSizeChange"
            @current-change="handleAvailableCurrentChange"
          />
        </div>
      </div>

      <template #footer>
        <el-button @click="addQuestionsDialog = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="addSelectedQuestions" 
          :loading="adding"
          :disabled="selectedQuestions.length === 0"
        >
          添加选中的题目 ({{ selectedQuestions.length }})
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Edit, Plus, Back, Refresh } from '@element-plus/icons-vue'
import { questionBankApi } from '@/api/questionBank'
import { questionApi } from '@/api/question'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { QuestionBank, Question, QuestionResponse } from '@/types/api'

const route = useRoute()
const router = useRouter()

const questionBankId = computed(() => Number(route.params.id))

const loading = ref(false)
const questionsLoading = ref(false)
const availableQuestionsLoading = ref(false)
const adding = ref(false)

const questionBank = ref<QuestionBank | null>(null)
const questions = ref<QuestionResponse[]>([])
const searchKeyword = ref('')
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

// 添加题目相关
const addQuestionsDialog = ref(false)
const availableQuestions = ref<QuestionResponse[]>([])
const selectedQuestions = ref<QuestionResponse[]>([])
const questionSearchKeyword = ref('')
const availableTotal = ref(0)
const availableCurrentPage = ref(1)
const availablePageSize = ref(10)

// 权限检查 - 简化版本，实际应该检查用户权限
const canEdit = computed(() => true) // TODO: 实际权限检查

const loadQuestionBank = async () => {
  try {
    loading.value = true
    questionBank.value = await questionBankApi.getQuestionBank(questionBankId.value)
  } catch (error) {
    console.error('加载题目库失败:', error)
    ElMessage.error('加载题目库失败')
    router.back()
  } finally {
    loading.value = false
  }
}

const loadQuestions = async () => {
  try {
    questionsLoading.value = true
    const response = await questionBankApi.getQuestionBankQuestions(
      questionBankId.value,
      currentPage.value - 1,
      pageSize.value
    )
    questions.value = (response.content || []) as QuestionResponse[]
    total.value = response.totalElements || 0
  } catch (error) {
    console.error('加载题目列表失败:', error)
    ElMessage.error('加载题目列表失败')
  } finally {
    questionsLoading.value = false
  }
}

const loadAvailableQuestions = async () => {
  try {
    availableQuestionsLoading.value = true
    const response = await questionApi.getQuestions({
      page: availableCurrentPage.value - 1,
      size: availablePageSize.value,
      keyword: questionSearchKeyword.value,
      // 排除已在当前题目库中的题目
      excludeQuestionBankId: questionBankId.value
    })
    availableQuestions.value = response.data?.content || []
    availableTotal.value = response.data?.totalElements || 0
  } catch (error) {
    console.error('加载可用题目失败:', error)
    ElMessage.error('加载可用题目失败')
  } finally {
    availableQuestionsLoading.value = false
  }
}

const searchQuestions = () => {
  currentPage.value = 1
  loadQuestions()
}

const searchAvailableQuestions = () => {
  availableCurrentPage.value = 1
  loadAvailableQuestions()
}

const editQuestionBank = () => {
  router.push(`/question-banks/${questionBankId.value}/edit`)
}

const viewQuestion = (questionId: number) => {
  router.push(`/questions/${questionId}`)
}

const editQuestion = (questionId: number) => {
  router.push(`/questions/${questionId}/edit`)
}

const removeQuestion = async (questionId: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要从题目库中移除这个题目吗？题目本身不会被删除。',
      '确认移除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await questionBankApi.removeQuestionFromBank(questionBankId.value, questionId)
    ElMessage.success('题目移除成功')
    loadQuestions()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('移除题目失败:', error)
      ElMessage.error('移除题目失败')
    }
  }
}

const handleSelectionChange = (selection: QuestionResponse[]) => {
  selectedQuestions.value = selection
}

const addSelectedQuestions = async () => {
  if (selectedQuestions.value.length === 0) {
    ElMessage.warning('请选择要添加的题目')
    return
  }

  try {
    adding.value = true
    
    for (const question of selectedQuestions.value) {
      await questionBankApi.addQuestionToBank(questionBankId.value, question.id)
    }

    ElMessage.success(`成功添加 ${selectedQuestions.value.length} 个题目`)
    addQuestionsDialog.value = false
    selectedQuestions.value = []
    loadQuestions()
  } catch (error) {
    console.error('添加题目失败:', error)
    ElMessage.error('添加题目失败')
  } finally {
    adding.value = false
  }
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  loadQuestions()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  loadQuestions()
}

const handleAvailableSizeChange = (size: number) => {
  availablePageSize.value = size
  loadAvailableQuestions()
}

const handleAvailableCurrentChange = (page: number) => {
  availableCurrentPage.value = page
  loadAvailableQuestions()
}

const goBack = () => {
  router.back()
}

const getQuestionTypeTag = (type: string) => {
  const map: Record<string, string> = {
    'SINGLE_CHOICE': 'primary',
    'MULTIPLE_CHOICE': 'success',
    'TRUE_FALSE': 'warning',
    'SHORT_ANSWER': 'info',
    'ESSAY': 'danger',
    'CODING': 'primary',
    'CASE_ANALYSIS': 'success'
  }
  return map[type] || 'info'
}

const getQuestionTypeText = (type: string) => {
  const map: Record<string, string> = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'TRUE_FALSE': '判断题',
    'SHORT_ANSWER': '简答题',
    'ESSAY': '论述题',
    'CODING': '编程题',
    'CASE_ANALYSIS': '案例分析'
  }
  return map[type] || type
}

const formatDate = (dateString: string) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

// 监听添加题目对话框打开
const handleAddQuestionsDialogOpen = () => {
  if (addQuestionsDialog.value) {
    loadAvailableQuestions()
  }
}

// 监听对话框状态
const unwatchDialog = ref()
onMounted(() => {
  loadQuestionBank()
  loadQuestions()
  
  unwatchDialog.value = addQuestionsDialog.value && handleAddQuestionsDialogOpen()
})
</script>

<style scoped>
.question-bank-detail {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.bank-info-card,
.questions-card {
  margin-bottom: 24px;
}

.bank-info {
  /* 题目库信息样式 */
}

.bank-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.bank-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  flex: 1;
  margin-right: 16px;
}

.bank-description {
  margin: 0 0 20px 0;
  color: #606266;
  font-size: 16px;
  line-height: 1.6;
}

.bank-meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.meta-item {
  font-size: 14px;
  color: #606266;
}

.meta-item strong {
  color: #303133;
  margin-right: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.add-questions-content {
  max-height: 500px;
}

.add-questions-content .el-table {
  max-height: 300px;
}
</style>
