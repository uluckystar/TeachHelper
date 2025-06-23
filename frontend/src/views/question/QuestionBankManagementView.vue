<template>
  <div class="question-bank-management">
    <div class="page-header">
      <h1>题目库管理</h1>
      <p class="page-description">管理题目库，将题目按学科、年级等分类组织</p>
    </div>

    <!-- 操作区域 -->
    <el-card class="action-card">
      <el-row :gutter="16">
        <el-col :span="16">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索题目库名称或描述"
            @keyup.enter="searchQuestionBanks"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="8">
          <div class="action-buttons">
            <el-button type="primary" icon="Plus" @click="showCreateDialog">
              创建题目库
            </el-button>
            <el-button icon="Refresh" @click="loadQuestionBanks">
              刷新
            </el-button>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 题目库列表 -->
    <el-card class="banks-card">
      <template #header>
        <div class="card-header">
          <span>题目库列表 ({{ total }})</span>
          <el-button-group>
            <el-button 
              :type="viewMode === 'list' ? 'primary' : 'default'"
              icon="List"
              @click="viewMode = 'list'"
            >
              列表视图
            </el-button>
            <el-button 
              :type="viewMode === 'card' ? 'primary' : 'default'"
              icon="Grid"
              @click="viewMode = 'card'"
            >
              卡片视图
            </el-button>
          </el-button-group>
        </div>
      </template>

      <!-- 列表视图 -->
      <el-table 
        v-if="viewMode === 'list'"
        :data="questionBanks" 
        v-loading="loading"
      >
        <el-table-column prop="name" label="题目库名称" min-width="200" />
        <el-table-column prop="description" label="描述" min-width="250" show-overflow-tooltip />
        <el-table-column prop="subject" label="学科" width="120" />
        <el-table-column prop="gradeLevel" label="年级" width="120" />
        <el-table-column prop="questionCount" label="题目数量" width="100">
          <template #default="{ row }">
            <el-tag type="info">{{ row.questionCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isPublic" label="可见性" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isPublic ? 'success' : 'info'">
              {{ row.isPublic ? '公开' : '私有' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="150">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300">
          <template #default="{ row }">
            <el-button-group>
              <el-button size="small" @click="viewQuestionBank(row.id)">
                查看
              </el-button>
              <el-button size="small" type="primary" @click="manageQuestions(row.id)">
                管理题目
              </el-button>
              <el-button size="small" type="success" @click="editQuestionBank(row)">
                编辑
              </el-button>
              <el-button size="small" type="danger" @click="deleteQuestionBank(row.id)">
                删除
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- 卡片视图 -->
      <div v-else class="card-view" v-loading="loading">
        <el-row :gutter="16">
          <el-col :span="8" v-for="bank in questionBanks" :key="bank.id">
            <el-card class="bank-card" @click="viewQuestionBank(bank.id)">
              <div class="bank-header">
                <h4 class="bank-title">{{ bank.name }}</h4>
                <el-tag :type="bank.isPublic ? 'success' : 'info'" size="small">
                  {{ bank.isPublic ? '公开' : '私有' }}
                </el-tag>
              </div>
              <p class="bank-description">{{ bank.description || '暂无描述' }}</p>
              <div class="bank-meta">
                <span v-if="bank.subject" class="meta-item">
                  <el-icon><Reading /></el-icon>
                  {{ bank.subject }}
                </span>
                <span v-if="bank.gradeLevel" class="meta-item">
                  <el-icon><User /></el-icon>
                  {{ bank.gradeLevel }}
                </span>
                <span class="meta-item">
                  <el-icon><Document /></el-icon>
                  {{ bank.questionCount || 0 }} 道题目
                </span>
              </div>
              <div class="bank-actions" @click.stop>
                <el-button size="small" type="primary" @click="manageQuestions(bank.id)">
                  管理题目
                </el-button>
                <el-button size="small" type="success" @click="editQuestionBank(bank)">
                  编辑
                </el-button>
                <el-button size="small" type="danger" @click="deleteQuestionBank(bank.id)">
                  删除
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

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

    <!-- 创建/编辑题目库对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingBank ? '编辑题目库' : '创建题目库'"
      width="600px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="题目库名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入题目库名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入题目库描述（可选）"
          />
        </el-form-item>
        <el-form-item label="学科" prop="subject">
          <el-select v-model="form.subject" placeholder="请选择学科" clearable style="width: 100%">
            <el-option
              v-for="subject in subjects"
              :key="subject"
              :label="subject"
              :value="subject"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="年级" prop="gradeLevel">
          <el-select v-model="form.gradeLevel" placeholder="请选择年级" clearable style="width: 100%">
            <el-option
              v-for="grade in gradeLevels"
              :key="grade"
              :label="grade"
              :value="grade"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="可见性">
          <el-switch
            v-model="form.isPublic"
            active-text="公开"
            inactive-text="私有"
            :active-value="true"
            :inactive-value="false"
          />
          <div class="form-help-text">
            公开题目库可被其他用户查看和使用
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveQuestionBank" :loading="saving">
          {{ editingBank ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { 
  Search, 
  Plus, 
  Refresh, 
  List, 
  Grid, 
  Reading, 
  User, 
  Document 
} from '@element-plus/icons-vue'
import { questionBankApi } from '@/api/questionBank'
import { subjectApi, gradeLevelApi } from '@/api/metadata'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import type { QuestionBank } from '@/types/api'

const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const viewMode = ref<'list' | 'card'>('card')
const searchKeyword = ref('')
const questionBanks = ref<QuestionBank[]>([])
const subjects = ref<string[]>([])
const gradeLevels = ref<string[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)

// 对话框相关
const dialogVisible = ref(false)
const editingBank = ref<QuestionBank | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  description: '',
  subject: '',
  gradeLevel: '',
  isPublic: false
})

const rules = {
  name: [
    { required: true, message: '请输入题目库名称', trigger: 'blur' },
    { min: 2, max: 100, message: '名称长度应在 2-100 个字符', trigger: 'blur' }
  ]
}

const loadQuestionBanks = async () => {
  loading.value = true
  try {
    const response = await questionBankApi.getMyQuestionBanks(currentPage.value - 1, pageSize.value)
    questionBanks.value = response.content || []
    total.value = response.totalElements || 0

    // 为每个题目库获取题目数量
    for (const bank of questionBanks.value) {
      try {
        const stats = await questionBankApi.getQuestionBankStatistics(bank.id)
        bank.questionCount = stats.questionCount
      } catch (error) {
        console.warn(`Failed to load statistics for bank ${bank.id}:`, error)
        bank.questionCount = 0
      }
    }
  } catch (error) {
    console.error('加载题目库失败:', error)
    ElMessage.error('加载题目库失败')
  } finally {
    loading.value = false
  }
}

const loadSubjects = async () => {
  try {
    const response = await subjectApi.getAllSubjectNames()
    subjects.value = response || []
  } catch (error) {
    console.error('加载学科列表失败:', error)
    // 如果API失败，使用默认学科列表
    subjects.value = ['数学', '语文', '英语', '物理', '化学', '生物', '历史', '地理', '政治', 'Java编程', '数据库', 'Web开发', '计算机网络', '算法', '操作系统', '软件工程']
  }
}

const loadGradeLevels = async () => {
  try {
    const response = await gradeLevelApi.getAllGradeLevelNames()
    gradeLevels.value = response || []
  } catch (error) {
    console.error('加载年级列表失败:', error)
    // 如果API失败，使用默认年级列表
    gradeLevels.value = ['小学一年级', '小学二年级', '小学三年级', '小学四年级', '小学五年级', '小学六年级', '初一', '初二', '初三', '高一', '高二', '高三', '本科', '研究生']
  }
}

const searchQuestionBanks = () => {
  currentPage.value = 1
  loadQuestionBanks()
}

const showCreateDialog = () => {
  editingBank.value = null
  Object.assign(form, {
    name: '',
    description: '',
    subject: '',
    gradeLevel: '',
    isPublic: false
  })
  dialogVisible.value = true
}

const editQuestionBank = (bank: QuestionBank) => {
  editingBank.value = bank
  Object.assign(form, {
    name: bank.name,
    description: bank.description || '',
    subject: bank.subject || '',
    gradeLevel: bank.gradeLevel || '',
    isPublic: bank.isPublic
  })
  dialogVisible.value = true
}

const saveQuestionBank = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    saving.value = true

    if (editingBank.value) {
      // 更新题目库
      await questionBankApi.updateQuestionBank(editingBank.value.id, form)
      ElMessage.success('题目库更新成功')
    } else {
      // 创建题目库
      await questionBankApi.createQuestionBank(form)
      ElMessage.success('题目库创建成功')
    }

    dialogVisible.value = false
    loadQuestionBanks()
  } catch (error: any) {
    console.error('保存题目库失败:', error)
    ElMessage.error(error.message || '保存题目库失败')
  } finally {
    saving.value = false
  }
}

const deleteQuestionBank = async (id: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个题目库吗？删除后其中的题目将变为无分类状态。',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await questionBankApi.deleteQuestionBank(id)
    ElMessage.success('题目库删除成功')
    loadQuestionBanks()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除题目库失败:', error)
      ElMessage.error('删除题目库失败')
    }
  }
}

const viewQuestionBank = (id: number) => {
  router.push(`/question-banks/${id}`)
}

const manageQuestions = (id: number) => {
  router.push(`/question-banks/${id}/questions`)
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  loadQuestionBanks()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  loadQuestionBanks()
}

const formatDate = (dateString: string) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

onMounted(() => {
  loadQuestionBanks()
  loadSubjects()
  loadGradeLevels()
})
</script>

<style scoped>
.question-bank-management {
  max-width: 1200px;
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

.action-card,
.banks-card {
  margin-bottom: 24px;
}

.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-view {
  min-height: 200px;
}

.bank-card {
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.bank-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.bank-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.bank-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  flex: 1;
  margin-right: 12px;
}

.bank-description {
  margin: 0 0 16px 0;
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
}

.bank-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #666;
}

.meta-item .el-icon {
  font-size: 14px;
}

.bank-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.form-help-text {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
