<template>
  <div class="generated-questions">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" icon="MagicStick" @click="showGenerateDialog = true">
          AI生成题目
        </el-button>
        <el-button icon="Plus" @click="showAddDialog = true">
          手动添加
        </el-button>
        <el-button icon="Upload" @click="showImportDialog = true">
          导入题库
        </el-button>
      </div>
      
      <div class="toolbar-right">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索题目..."
          prefix-icon="Search"
          style="width: 200px;"
          clearable
        />
        <el-select v-model="typeFilter" placeholder="题型筛选" style="width: 120px;">
          <el-option value="" label="全部题型" />
          <el-option value="choice" label="选择题" />
          <el-option value="blank" label="填空题" />
          <el-option value="subjective" label="主观题" />
          <el-option value="calculation" label="计算题" />
        </el-select>
        <el-select v-model="difficultyFilter" placeholder="难度筛选" style="width: 120px;">
          <el-option value="" label="全部难度" />
          <el-option value="easy" label="简单" />
          <el-option value="medium" label="中等" />
          <el-option value="hard" label="困难" />
        </el-select>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="stats-bar">
      <div class="stat-item">
        <span class="stat-label">总题目：</span>
        <span class="stat-value">{{ questions.length }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">选择题：</span>
        <span class="stat-value">{{ getQuestionCount('choice') }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">填空题：</span>
        <span class="stat-value">{{ getQuestionCount('blank') }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">主观题：</span>
        <span class="stat-value">{{ getQuestionCount('subjective') }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">AI生成：</span>
        <span class="stat-value">{{ aiGeneratedCount }}</span>
      </div>
    </div>

    <!-- 题目列表 -->
    <div class="questions-list">
      <div 
        v-for="question in filteredQuestions" 
        :key="question.id"
        class="question-card"
        :class="{ 'question-selected': selectedQuestions.includes(question.id) }"
        @click="toggleSelection(question.id)"
      >
        <!-- 题目头部 -->
        <div class="question-header">
          <div class="question-meta">
            <el-checkbox 
              :model-value="selectedQuestions.includes(question.id)"
              @change="toggleSelection(question.id)"
              @click.stop
            />
            <el-tag 
              :type="getTypeTagType(question.type)" 
              size="small"
            >
              {{ getTypeText(question.type) }}
            </el-tag>
            <el-tag 
              :type="getDifficultyTagType(question.difficulty)"
              size="small"
            >
              {{ getDifficultyText(question.difficulty) }}
            </el-tag>
            <el-icon v-if="question.isAIGenerated" class="ai-icon" title="AI生成">
              <MagicStick />
            </el-icon>
          </div>
          
          <div class="question-actions">
            <el-button size="small" icon="View" @click.stop="previewQuestion(question)" />
            <el-button size="small" icon="Edit" @click.stop="editQuestion(question)" />
            <el-button 
              size="small" 
              icon="DocumentChecked" 
              @click.stop="manageRubric(question)"
              :type="hasRubric(question) ? 'success' : 'warning'"
            >
              {{ hasRubric(question) ? '管理评分标准' : '创建评分标准' }}
            </el-button>
            <el-dropdown @command="(cmd) => handleQuestionAction(cmd, question)">
              <el-button size="small" icon="MoreFilled" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="practice" icon="Trophy">练习模式</el-dropdown-item>
                  <el-dropdown-item command="export" icon="Download">导出</el-dropdown-item>
                  <el-dropdown-item command="duplicate" icon="DocumentCopy">复制</el-dropdown-item>
                  <el-dropdown-item divided command="delete" icon="Delete">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <!-- 题目内容 -->
        <div class="question-content">
          <div class="question-text" v-html="question.questionText"></div>
          
          <!-- 选择题选项 -->
          <div v-if="question.type === 'choice'" class="question-options">
            <div 
              v-for="(option, index) in question.options" 
              :key="index"
              class="option-item"
              :class="{ 'option-correct': option.isCorrect }"
            >
              <span class="option-label">{{ String.fromCharCode(65 + index) }}.</span>
              <span class="option-text">{{ option.text }}</span>
              <el-icon v-if="option.isCorrect" class="correct-icon">
                <Check />
              </el-icon>
            </div>
          </div>

          <!-- 填空题答案 -->
          <div v-else-if="question.type === 'blank'" class="question-answer">
            <div class="answer-label">参考答案：</div>
            <div class="answer-content">{{ question.correctAnswer }}</div>
          </div>

          <!-- 主观题评分标准 -->
          <div v-else-if="question.type === 'subjective'" class="question-rubric">
            <div class="rubric-label">评分标准：</div>
            <div class="rubric-content">
              <div 
                v-for="(criterion, index) in question.scoringCriteria" 
                :key="index"
                class="criterion-item"
              >
                <span class="criterion-score">{{ criterion.score }}分</span>
                <span class="criterion-description">{{ criterion.description }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 题目底部信息 -->
        <div class="question-footer">
          <div class="question-info">
            <span class="info-item">
              <el-icon><User /></el-icon>
              {{ question.createdBy || 'AI生成' }}
            </span>
            <span class="info-item">
              <el-icon><Clock /></el-icon>
              {{ formatDate(question.createdAt) }}
            </span>
            <span class="info-item" v-if="question.knowledgePoints?.length">
              <el-icon><Collection /></el-icon>
              关联 {{ question.knowledgePoints.length }} 个知识点
            </span>
          </div>
          
          <div class="question-stats">
            <el-tooltip content="练习次数">
              <span class="stat-item">
                <el-icon><Trophy /></el-icon>
                {{ question.practiceCount || 0 }}
              </span>
            </el-tooltip>
            <el-tooltip content="正确率">
              <span class="stat-item">
                <el-icon><TrendCharts /></el-icon>
                {{ question.correctRate ? `${question.correctRate}%` : 'N/A' }}
              </span>
            </el-tooltip>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredQuestions.length === 0" class="empty-state">
        <el-icon size="64"><DocumentCopy /></el-icon>
        <h3>暂无题目</h3>
        <p>点击上方按钮开始生成或添加题目</p>
      </div>
    </div>

    <!-- 批量操作栏 -->
    <div class="batch-actions" v-if="selectedQuestions.length > 0">
      <div class="batch-info">
        已选择 {{ selectedQuestions.length }} 道题目
      </div>
      <div class="batch-buttons">
        <el-button icon="Trophy" @click="batchPractice">
          组成练习
        </el-button>
        <el-button icon="Download" @click="batchExport">
          批量导出
        </el-button>
        <el-button icon="Edit" @click="batchEdit">
          批量编辑
        </el-button>
        <el-button icon="Delete" @click="batchDelete" type="danger">
          批量删除
        </el-button>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-container" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
      />
    </div>

    <!-- AI生成题目对话框 -->
    <AIQuestionGenerationDialog
      v-model="showGenerateDialog"
      :knowledgeBase="{ id: knowledgeBaseId, name: '', description: '' }"
      @generated="handleQuestionsGenerated"
    />

    <!-- 添加/编辑题目对话框 -->
    <AddEditQuestionDialog
      v-model="showAddDialog"
      :knowledgeBaseId="knowledgeBaseId"
      :editingQuestion="editingQuestion"
      @saved="handleQuestionSaved"
    />

    <!-- 导入题库对话框 -->
    <ImportQuestionDialog
      v-model="showImportDialog"
      :knowledgeBaseId="knowledgeBaseId"
      @imported="handleQuestionsImported"
    />

    <!-- 题目预览对话框 -->
    <QuestionPreviewDialog
      v-model="showPreviewDialog"
      :question="previewingQuestion"
    />

    <!-- 练习模式对话框 -->
    <PracticeModeDialog
      v-model="showPracticeDialog"
      :questions="practiceQuestions"
    />

    <!-- 评分标准管理对话框 -->
    <RubricManagementDialog
      v-model="showRubricDialog"
      :question-id="currentRubricQuestion?.id || null"
      @refresh="handleRubricSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  MagicStick,
  Plus,
  Upload,
  Search,
  View,
  Edit,
  MoreFilled,
  Trophy,
  Download,
  DocumentCopy,
  Delete,
  Check,
  User,
  Clock,
  Collection,
  TrendCharts,
  DocumentChecked
} from '@element-plus/icons-vue'
import AIQuestionGenerationDialog from './AIQuestionGenerationDialog.vue'
import AddEditQuestionDialog from './AddEditQuestionDialog.vue'
import ImportQuestionDialog from './ImportQuestionDialog.vue'
import QuestionPreviewDialog from './QuestionPreviewDialog.vue'
import PracticeModeDialog from './PracticeModeDialog.vue'
import RubricManagementDialog from '../evaluation/RubricManagementDialog.vue'
import { questionApi } from '@/api/question'
import api from '@/utils/request'

// Props
const props = defineProps<{
  knowledgeBaseId: number
}>()

// 响应式数据
const questions = ref<Question[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const typeFilter = ref('')
const difficultyFilter = ref('')
const selectedQuestions = ref<number[]>([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 对话框状态
const showGenerateDialog = ref(false)
const showAddDialog = ref(false)
const showImportDialog = ref(false)
const showPreviewDialog = ref(false)
const showPracticeDialog = ref(false)
const showRubricDialog = ref(false)

// 编辑状态
const editingQuestion = ref<Question | null>(null)
const previewingQuestion = ref<Question | null>(null)
const practiceQuestions = ref<Question[]>([])
const currentRubricQuestion = ref<Question | null>(null)

// 类型定义
interface Question {
  id: number
  questionText: string
  type: 'choice' | 'blank' | 'subjective' | 'calculation'
  difficulty: 'easy' | 'medium' | 'hard'
  isAIGenerated: boolean
  options?: QuestionOption[]
  correctAnswer?: string
  scoringCriteria?: ScoringCriterion[]
  knowledgePoints?: { id: number; title: string }[]
  createdBy?: string
  createdAt: string
  practiceCount?: number
  correctRate?: number
}

interface QuestionOption {
  text: string
  isCorrect: boolean
}

interface ScoringCriterion {
  score: number
  description: string
}

// 计算属性
const aiGeneratedCount = computed(() => {
  return questions.value.filter(q => q.isAIGenerated).length
})

const filteredQuestions = computed(() => {
  let filtered = questions.value

  // 按关键词筛选
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    filtered = filtered.filter(q => 
      q.questionText.toLowerCase().includes(keyword)
    )
  }

  // 按题型筛选
  if (typeFilter.value) {
    filtered = filtered.filter(q => q.type === typeFilter.value)
  }

  // 按难度筛选
  if (difficultyFilter.value) {
    filtered = filtered.filter(q => q.difficulty === difficultyFilter.value)
  }

  // 分页
  const start = (currentPage.value - 1) * pageSize.value
  total.value = filtered.length
  return filtered.slice(start, start + pageSize.value)
})

// 生命周期
onMounted(() => {
  loadQuestions()
})

// 监听知识库ID变化
watch(() => props.knowledgeBaseId, () => {
  loadQuestions()
})

// 方法
const loadQuestions = async () => {
  loading.value = true
  try {
    console.log('Loading questions for knowledge base:', props.knowledgeBaseId)
    
    // 使用正确的 axios 实例调用 API
    const response = await api.get(`/knowledge-bases/${props.knowledgeBaseId}/questions?page=0&size=1000`)
    
    console.log('Questions API response:', response.data)
    
    // 转换API响应到前端数据格式
    const questionsData = response.data.content || []
    questions.value = questionsData.map((q: any) => ({
      id: q.id,
      questionText: q.questionText || q.content,
      type: q.type,
      difficulty: q.difficulty,
      isAIGenerated: q.isAIGenerated || false,
      options: q.options || [],
      correctAnswer: q.correctAnswer,
      scoringCriteria: q.scoringCriteria || [],
      knowledgePoints: q.knowledgePoints || [],
      createdBy: q.createdBy,
      createdAt: q.createdAt,
      practiceCount: q.practiceCount || 0,
      correctRate: q.correctRate || 0
    }))
    
    console.log('Processed questions:', questions.value)
    if (questions.value.length > 0) {
      ElMessage.success(`成功加载 ${questions.value.length} 道题目`)
    } else {
      ElMessage.info('当前知识库暂无题目，点击"AI生成题目"开始创建')
    }
  } catch (error: any) {
    console.error('Load questions failed:', error)
    ElMessage.error(`加载题目失败: ${error.message || '网络错误'}`)
    questions.value = []
  } finally {
    loading.value = false
  }
}

const getQuestionCount = (type: string) => {
  return questions.value.filter(q => q.type === type).length
}

const toggleSelection = (questionId: number) => {
  const index = selectedQuestions.value.indexOf(questionId)
  if (index > -1) {
    selectedQuestions.value.splice(index, 1)
  } else {
    selectedQuestions.value.push(questionId)
  }
}

const previewQuestion = (question: Question) => {
  previewingQuestion.value = question
  showPreviewDialog.value = true
}

const editQuestion = (question: Question) => {
  editingQuestion.value = question
  showAddDialog.value = true
}

const manageRubric = (question: Question) => {
  currentRubricQuestion.value = question
  showRubricDialog.value = true
}

const handleQuestionAction = (command: string, question: Question) => {
  switch (command) {
    case 'practice':
      startPractice([question])
      break
    case 'export':
      exportQuestion(question)
      break
    case 'duplicate':
      duplicateQuestion(question)
      break
    case 'delete':
      deleteQuestion(question)
      break
  }
}

const startPractice = (questions: Question[]) => {
  practiceQuestions.value = questions
  showPracticeDialog.value = true
}

const exportQuestion = (question: Question) => {
  // TODO: 实现题目导出功能
  ElMessage.info('导出功能开发中...')
}

const duplicateQuestion = async (question: Question) => {
  try {
    // TODO: 调用API复制题目
    const newQuestion = {
      ...question,
      id: Date.now(),
      questionText: question.questionText + ' (副本)',
      createdAt: new Date().toISOString(),
      practiceCount: 0,
      correctRate: undefined
    }
    
    questions.value.unshift(newQuestion)
    ElMessage.success('题目复制成功')
  } catch (error) {
    ElMessage.error('复制题目失败')
    console.error('Duplicate question failed:', error)
  }
}

const deleteQuestion = async (question: Question) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除这道题目吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // TODO: 调用API删除题目
    const index = questions.value.findIndex(q => q.id === question.id)
    if (index > -1) {
      questions.value.splice(index, 1)
    }
    
    ElMessage.success('题目删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除题目失败')
      console.error('Delete question failed:', error)
    }
  }
}

const batchPractice = () => {
  const selectedQuestionObjects = questions.value.filter(q => 
    selectedQuestions.value.includes(q.id)
  )
  startPractice(selectedQuestionObjects)
}

const batchExport = () => {
  // TODO: 实现批量导出功能
  ElMessage.info('批量导出功能开发中...')
}

const batchEdit = () => {
  // TODO: 实现批量编辑功能
  ElMessage.info('批量编辑功能开发中...')
}

const batchDelete = async () => {
  if (selectedQuestions.value.length === 0) return
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedQuestions.value.length} 道题目吗？此操作不可恢复。`,
      '确认批量删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // TODO: 调用API批量删除题目
    questions.value = questions.value.filter(q => 
      !selectedQuestions.value.includes(q.id)
    )
    selectedQuestions.value = []
    
    ElMessage.success('批量删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
      console.error('Batch delete failed:', error)
    }
  }
}

const handleQuestionsGenerated = () => {
  loadQuestions()
  ElMessage.success('AI题目生成完成')
}

const handleQuestionSaved = () => {
  loadQuestions()
  editingQuestion.value = null
}

const handleQuestionsImported = () => {
  loadQuestions()
  ElMessage.success('题库导入完成')
}

const handleRubricSaved = () => {
  // 重新加载题目数据以获取最新的评分标准
  loadQuestions()
  ElMessage.success('评分标准保存成功')
}

// 工具方法
const getTypeTagType = (type: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    choice: 'primary',
    blank: 'success',
    subjective: 'warning',
    calculation: 'info'
  }
  return typeMap[type] || 'info'
}

const getTypeText = (type: string) => {
  const textMap: Record<string, string> = {
    choice: '选择题',
    blank: '填空题',
    subjective: '主观题',
    calculation: '计算题'
  }
  return textMap[type] || '未知'
}

const getDifficultyTagType = (difficulty: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    easy: 'success',
    medium: 'warning',
    hard: 'danger'
  }
  return typeMap[difficulty] || 'info'
}

const getDifficultyText = (difficulty: string) => {
  const textMap: Record<string, string> = {
    easy: '简单',
    medium: '中等',
    hard: '困难'
  }
  return textMap[difficulty] || '未知'
}

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString()
}

// 评分标准相关方法
const hasRubric = (question: Question) => {
  return question.scoringCriteria && question.scoringCriteria.length > 0
}
</script>

<style scoped>
.generated-questions {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  margin-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.toolbar-left {
  display: flex;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 统计栏 */
.stats-bar {
  display: flex;
  gap: 24px;
  padding: 12px 0;
  margin-bottom: 16px;
  font-size: 14px;
}

.stat-item {
  display: flex;
  align-items: center;
}

.stat-label {
  color: #909399;
  margin-right: 4px;
}

.stat-value {
  color: #303133;
  font-weight: 500;
}

/* 题目列表 */
.questions-list {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 16px;
}

.question-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.question-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}

.question-card.question-selected {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.question-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-icon {
  color: #409eff;
  font-size: 14px;
}

.question-actions {
  display: flex;
  gap: 4px;
}

.question-content {
  margin-bottom: 12px;
}

.question-text {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
  margin-bottom: 12px;
}

.question-options {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 4px;
  font-size: 13px;
  transition: background-color 0.2s;
}

.option-item:hover {
  background-color: #f5f7fa;
}

.option-item.option-correct {
  background-color: #f0f9ff;
  color: #409eff;
}

.option-label {
  font-weight: 500;
  min-width: 20px;
}

.option-text {
  flex: 1;
}

.correct-icon {
  color: #67c23a;
  font-size: 14px;
}

.question-answer,
.question-rubric {
  padding: 8px 12px;
  background-color: #f8f9fa;
  border-radius: 4px;
  font-size: 13px;
}

.answer-label,
.rubric-label {
  font-weight: 500;
  color: #606266;
  margin-bottom: 4px;
}

.answer-content {
  color: #303133;
}

.rubric-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.criterion-item {
  display: flex;
  gap: 8px;
}

.criterion-score {
  color: #409eff;
  font-weight: 500;
  min-width: 40px;
}

.criterion-description {
  color: #303133;
}

.question-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f5f7fa;
  font-size: 12px;
  color: #909399;
}

.question-info {
  display: flex;
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.question-stats {
  display: flex;
  gap: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: default;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #909399;
  text-align: center;
}

.empty-state h3 {
  margin: 16px 0 8px 0;
  color: #606266;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

/* 批量操作栏 */
.batch-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 16px;
}

.batch-info {
  font-size: 14px;
  color: #606266;
}

.batch-buttons {
  display: flex;
  gap: 8px;
}

/* 分页 */
.pagination-container {
  display: flex;
  justify-content: center;
  padding: 16px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .toolbar-left,
  .toolbar-right {
    justify-content: center;
  }
  
  .stats-bar {
    flex-wrap: wrap;
    gap: 12px;
  }
  
  .question-header {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }
  
  .question-footer {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }
  
  .batch-actions {
    flex-direction: column;
    gap: 12px;
    text-align: center;
  }
}
</style>
