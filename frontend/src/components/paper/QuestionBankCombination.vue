<template>
  <div class="question-bank-combination">
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
    >
      <!-- 基本信息 -->
      <el-card class="section-card">
        <template #header>
          <span>基本信息</span>
        </template>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="试卷标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入试卷标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考试时长" prop="duration">
              <el-input-number
                v-model="form.duration"
                :min="30"
                :max="300"
                :step="15"
                style="width: 100%"
              />
              <span style="margin-left: 8px;">分钟</span>
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 题库筛选 -->
      <el-card class="section-card">
        <template #header>
          <div class="section-header">
            <span>题库筛选</span>
            <el-button size="small" @click="showAdvancedFilter = !showAdvancedFilter">
              {{ showAdvancedFilter ? '收起' : '展开' }}高级筛选
            </el-button>
          </div>
        </template>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="学科">
              <el-select v-model="filterForm.subject" placeholder="选择学科" clearable style="width: 100%">
                <el-option
                  v-for="subject in subjects"
                  :key="subject"
                  :label="subject"
                  :value="subject"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="年级">
              <el-select v-model="filterForm.gradeLevel" placeholder="选择年级" clearable style="width: 100%">
                <el-option
                  v-for="grade in gradeLevels"
                  :key="grade"
                  :label="grade"
                  :value="grade"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="题型">
              <el-select v-model="filterForm.questionType" placeholder="选择题型" clearable style="width: 100%">
                <el-option label="单选题" value="SINGLE_CHOICE" />
                <el-option label="多选题" value="MULTIPLE_CHOICE" />
                <el-option label="判断题" value="TRUE_FALSE" />
                <el-option label="填空题" value="FILL_BLANK" />
                <el-option label="简答题" value="SHORT_ANSWER" />
                <el-option label="论述题" value="ESSAY" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <div v-show="showAdvancedFilter" class="advanced-filter">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="难度">
                <el-select v-model="filterForm.difficulty" placeholder="选择难度" clearable style="width: 100%">
                  <el-option label="简单" value="EASY" />
                  <el-option label="中等" value="MEDIUM" />
                  <el-option label="困难" value="HARD" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="知识点">
                <el-select
                  v-model="filterForm.knowledgePoints"
                  multiple
                  placeholder="选择知识点"
                  style="width: 100%"
                >
                  <el-option
                    v-for="point in knowledgePoints"
                    :key="point.id"
                    :label="point.title"
                    :value="point.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="标签">
                <el-select
                  v-model="filterForm.tags"
                  multiple
                  filterable
                  allow-create
                  placeholder="输入或选择标签"
                  style="width: 100%"
                >
                  <el-option
                    v-for="tag in commonTags"
                    :key="tag"
                    :label="tag"
                    :value="tag"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="创建时间">
                <el-date-picker
                  v-model="filterForm.dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="关键词搜索">
                <el-input
                  v-model="filterForm.keyword"
                  placeholder="搜索题目内容"
                  clearable
                  @keyup.enter="searchQuestions"
                >
                  <template #append>
                    <el-button @click="searchQuestions" icon="Search">搜索</el-button>
                  </template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="filter-actions">
          <el-button @click="applyFilter" type="primary">应用筛选</el-button>
          <el-button @click="resetFilter">重置筛选</el-button>
          <el-button @click="saveFilter">保存筛选条件</el-button>
        </div>
      </el-card>

      <!-- 题目选择 -->
      <el-card class="section-card">
        <template #header>
          <div class="section-header">
            <span>题目选择 ({{ selectedQuestions.length }}/{{ totalQuestions }})</span>
            <div class="header-actions">
              <el-button size="small" @click="selectAllQuestions">全选</el-button>
              <el-button size="small" @click="clearSelection">清空</el-button>
              <el-button size="small" @click="smartSelect">智能选择</el-button>
            </div>
          </div>
        </template>

        <div class="question-list-container">
          <div class="list-controls">
            <div class="view-controls">
              <el-radio-group v-model="viewMode" size="small">
                <el-radio-button label="list">列表</el-radio-button>
                <el-radio-button label="card">卡片</el-radio-button>
              </el-radio-group>
            </div>
            
            <div class="sort-controls">
              <el-select v-model="sortBy" placeholder="排序方式" size="small" style="width: 120px">
                <el-option label="创建时间" value="created_at" />
                <el-option label="难度" value="difficulty" />
                <el-option label="使用频率" value="usage_count" />
                <el-option label="题目质量" value="quality_score" />
              </el-select>
              <el-select v-model="sortOrder" size="small" style="width: 80px; margin-left: 8px">
                <el-option label="升序" value="asc" />
                <el-option label="降序" value="desc" />
              </el-select>
            </div>
          </div>

          <div class="question-list" :class="{ 'card-view': viewMode === 'card' }">
            <div
              v-for="question in displayQuestions"
              :key="question.id"
              class="question-item"
              :class="{ selected: selectedQuestionIds.includes(question.id) }"
              @click="toggleQuestionSelection(question)"
            >
              <div class="question-header">
                <el-checkbox
                  :model-value="selectedQuestionIds.includes(question.id)"
                  @change="(checked: any) => handleQuestionCheck(question, !!checked)"
                  @click.stop
                />
                <div class="question-meta">
                  <el-tag size="small">{{ getQuestionTypeText(question.questionType) }}</el-tag>
                  <el-tag
                    size="small"
                    :type="getDifficultyTagType(question.difficulty)"
                  >
                    {{ question.difficulty }}
                  </el-tag>
                  <span class="question-score">{{ question.maxScore }}分</span>
                </div>
                <div class="question-actions">
                  <el-button size="small" text @click.stop="previewQuestion(question)">预览</el-button>
                  <el-button size="small" text @click.stop="editQuestion(question)">编辑</el-button>
                </div>
              </div>
              
              <div class="question-content">
                <div class="question-text">{{ question.content }}</div>
                <div v-if="question.options && question.options.length > 0" class="question-options">
                  <span
                    v-for="(option, index) in question.options.slice(0, 2)"
                    :key="index"
                    class="option-preview"
                  >
                    {{ String.fromCharCode(65 + index) }}. {{ option.substring(0, 20) }}...
                  </span>
                  <span v-if="question.options.length > 2" class="more-options">
                    ...还有{{ question.options.length - 2 }}个选项
                  </span>
                </div>
                <div class="question-tags">
                  <el-tag
                    v-for="tag in question.tags"
                    :key="tag"
                    size="small"
                    type="info"
                  >
                    {{ tag }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="filteredQuestions.length"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handlePageChange"
              @current-change="handlePageChange"
            />
          </div>
        </div>
      </el-card>

      <!-- 组卷配置 -->
      <el-card class="section-card" v-if="selectedQuestions.length > 0">
        <template #header>
          <span>组卷配置</span>
        </template>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="题目顺序">
              <el-select v-model="form.questionOrder" placeholder="选择排序方式" style="width: 100%">
                <el-option label="按题型分组" value="by_type" />
                <el-option label="按难度递增" value="by_difficulty_asc" />
                <el-option label="按难度递减" value="by_difficulty_desc" />
                <el-option label="随机排序" value="random" />
                <el-option label="保持原顺序" value="original" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分值分配">
              <el-select v-model="form.scoreDistribution" placeholder="选择分值分配方式" style="width: 100%">
                <el-option label="保持原分值" value="original" />
                <el-option label="按题型统一" value="by_type" />
                <el-option label="按难度分配" value="by_difficulty" />
                <el-option label="自定义分配" value="custom" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总分设置">
              <el-input-number
                v-model="form.totalScore"
                :min="50"
                :max="200"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="试卷结构">
          <div class="paper-structure">
            <div
              v-for="group in questionGroups"
              :key="group.type"
              class="structure-item"
            >
              <span class="group-name">{{ getQuestionTypeText(group.type) }}</span>
              <span class="group-count">{{ group.count }} 题</span>
              <span class="group-score">{{ group.totalScore }} 分</span>
              <el-progress
                :percentage="(group.count / selectedQuestions.length) * 100"
                :stroke-width="8"
                :show-text="false"
                style="flex: 1; margin: 0 16px"
              />
            </div>
          </div>
        </el-form-item>
      </el-card>

      <!-- 操作按钮 -->
      <div class="form-actions">
        <el-button @click="resetForm">重置</el-button>
        <el-button @click="previewPaper" :disabled="selectedQuestions.length === 0">预览试卷</el-button>
        <el-button @click="saveAsDraft" :disabled="selectedQuestions.length === 0">保存草稿</el-button>
        <el-button
          type="primary"
          @click="handleSubmit"
          :loading="loading"
          :disabled="selectedQuestions.length === 0"
        >
          组合生成试卷
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getQuestionTypeText } from '@/utils/tagTypes'
import { subjectApi, gradeLevelApi } from '@/api/metadata'

interface Props {
  loading?: boolean
}

interface Emits {
  (e: 'generate', data: any): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const formRef = ref()
const showAdvancedFilter = ref(false)
const viewMode = ref('list')
const sortBy = ref('created_at')
const sortOrder = ref('desc')
const currentPage = ref(1)
const pageSize = ref(20)

const subjects = ref<string[]>([])
const gradeLevels = ref<string[]>([])
const knowledgePoints = ref<any[]>([])
const commonTags = ref(['重点', '易错', '基础', '提高', '综合'])
const questions = ref<any[]>([])
const selectedQuestionIds = ref<number[]>([])

// 表单数据
const form = reactive({
  title: '',
  duration: 120,
  questionOrder: 'by_type',
  scoreDistribution: 'original',
  totalScore: 100
})

// 筛选表单
const filterForm = reactive({
  subject: '',
  gradeLevel: '',
  questionType: '',
  difficulty: '',
  knowledgePoints: [],
  tags: [],
  dateRange: [] as string[],
  keyword: ''
})

// 表单验证规则
const rules = {
  title: [
    { required: true, message: '请输入试卷标题', trigger: 'blur' }
  ]
}

// 计算属性
const selectedQuestions = computed(() => {
  return questions.value.filter(q => selectedQuestionIds.value.includes(q.id))
})

const filteredQuestions = computed(() => {
  let filtered = [...questions.value]
  
  // 应用筛选条件
  if (filterForm.subject) {
    filtered = filtered.filter(q => q.subject === filterForm.subject)
  }
  if (filterForm.gradeLevel) {
    filtered = filtered.filter(q => q.gradeLevel === filterForm.gradeLevel)
  }
  if (filterForm.questionType) {
    filtered = filtered.filter(q => q.questionType === filterForm.questionType)
  }
  if (filterForm.difficulty) {
    filtered = filtered.filter(q => q.difficulty === filterForm.difficulty)
  }
  if (filterForm.keyword) {
    filtered = filtered.filter(q => 
      q.content.toLowerCase().includes(filterForm.keyword.toLowerCase())
    )
  }
  
  // 排序
  filtered.sort((a, b) => {
    let aVal = a[sortBy.value]
    let bVal = b[sortBy.value]
    
    if (sortBy.value === 'created_at') {
      aVal = new Date(aVal).getTime()
      bVal = new Date(bVal).getTime()
    }
    
    if (sortOrder.value === 'asc') {
      return aVal > bVal ? 1 : -1
    } else {
      return aVal < bVal ? 1 : -1
    }
  })
  
  return filtered
})

const displayQuestions = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredQuestions.value.slice(start, end)
})

const totalQuestions = computed(() => {
  return filteredQuestions.value.length
})

interface QuestionGroup {
  type: string
  count: number
  totalScore: number
}

const questionGroups = computed((): QuestionGroup[] => {
  const groups: Record<string, QuestionGroup> = {}
  
  selectedQuestions.value.forEach(q => {
    if (!groups[q.questionType]) {
      groups[q.questionType] = {
        type: q.questionType,
        count: 0,
        totalScore: 0
      }
    }
    groups[q.questionType].count++
    groups[q.questionType].totalScore += q.maxScore || 0
  })
  
  return Object.values(groups)
})

// 生命周期
onMounted(() => {
  loadQuestions()
  loadKnowledgePoints()
  loadSubjects()
  loadGradeLevels()
})

// 方法
const loadQuestions = async () => {
  try {
    // 模拟题库数据
    questions.value = [
      {
        id: 1,
        content: '下列关于函数的说法正确的是？',
        questionType: 'SINGLE_CHOICE',
        difficulty: 'MEDIUM',
        maxScore: 5,
        subject: '数学',
        gradeLevel: '高一',
        options: ['函数必须有反函数', '函数的定义域可以为空集', '函数值域是定义域的子集', '一个函数只能有一个解析式'],
        tags: ['函数', '基础'],
        createdAt: '2023-01-01'
      },
      {
        id: 2,
        content: '计算：$\\lim_{x \\to 0} \\frac{\\sin x}{x} = $____',
        questionType: 'FILL_BLANK',
        difficulty: 'HARD',
        maxScore: 8,
        subject: '数学',
        gradeLevel: '高二',
        options: [],
        tags: ['极限', '提高'],
        createdAt: '2023-01-02'
      }
      // 更多模拟数据...
    ]
  } catch (error) {
    console.error('加载题目失败:', error)
  }
}

const loadKnowledgePoints = async () => {
  try {
    knowledgePoints.value = [
      { id: 1, title: '函数' },
      { id: 2, title: '极限' },
      { id: 3, title: '导数' }
    ]
  } catch (error) {
    console.error('加载知识点失败:', error)
  }
}

const loadSubjects = async () => {
  try {
    const response = await subjectApi.getAllSubjectNames()
    subjects.value = response || []
  } catch (error) {
    console.error('加载学科列表失败:', error)
    // 如果新API失败，使用默认学科列表
    subjects.value = ['数学', '语文', '英语', '物理', '化学', '生物', '历史', '地理', '政治']
  }
}

const loadGradeLevels = async () => {
  try {
    const response = await gradeLevelApi.getAllGradeLevelNames()
    gradeLevels.value = response || []
  } catch (error) {
    console.error('加载年级列表失败:', error)
    // 如果新API失败，使用默认年级列表
    gradeLevels.value = ['小学一年级', '小学二年级', '小学三年级', '小学四年级', '小学五年级', '小学六年级',
                        '初一', '初二', '初三', '高一', '高二', '高三']
  }
}

const applyFilter = () => {
  currentPage.value = 1
  ElMessage.success('筛选条件已应用')
}

const resetFilter = () => {
  Object.assign(filterForm, {
    subject: '',
    gradeLevel: '',
    questionType: '',
    difficulty: '',
    knowledgePoints: [],
    tags: [],
    dateRange: null,
    keyword: ''
  })
  currentPage.value = 1
}

const saveFilter = () => {
  ElMessage.success('筛选条件已保存')
}

const searchQuestions = () => {
  applyFilter()
}

const selectAllQuestions = () => {
  selectedQuestionIds.value = filteredQuestions.value.map(q => q.id)
}

const clearSelection = () => {
  selectedQuestionIds.value = []
}

const smartSelect = () => {
  ElMessage.info('智能选择功能开发中...')
}

const toggleQuestionSelection = (question: any) => {
  const index = selectedQuestionIds.value.indexOf(question.id)
  if (index > -1) {
    selectedQuestionIds.value.splice(index, 1)
  } else {
    selectedQuestionIds.value.push(question.id)
  }
}

const handleQuestionCheck = (question: any, checked: boolean) => {
  if (checked) {
    if (!selectedQuestionIds.value.includes(question.id)) {
      selectedQuestionIds.value.push(question.id)
    }
  } else {
    const index = selectedQuestionIds.value.indexOf(question.id)
    if (index > -1) {
      selectedQuestionIds.value.splice(index, 1)
    }
  }
}

const handlePageChange = () => {
  // 分页变化处理
}

const previewQuestion = (question: any) => {
  ElMessage.info('题目预览功能开发中...')
}

const editQuestion = (question: any) => {
  ElMessage.info('题目编辑功能开发中...')
}

const getDifficultyTagType = (difficulty: string) => {
  switch (difficulty) {
    case 'EASY': return 'success'
    case 'MEDIUM': return 'warning'
    case 'HARD': return 'danger'
    default: return 'info'
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    emit('generate', {
      ...form,
      selectedQuestions: selectedQuestions.value,
      questionIds: selectedQuestionIds.value
    })
  } catch (error) {
    console.error('表单验证失败:', error)
  }
}

const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  selectedQuestionIds.value = []
}

const previewPaper = () => {
  ElMessage.info('试卷预览功能开发中...')
}

const saveAsDraft = () => {
  ElMessage.success('草稿已保存')
}
</script>

<style scoped>
.question-bank-combination {
  padding: 20px;
}

.section-card {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.advanced-filter {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.filter-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}

.question-list-container {
  max-height: 600px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.list-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.view-controls,
.sort-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.question-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 8px;
}

.question-list.card-view {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 16px;
}

.question-item {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.question-item:hover {
  border-color: #c6e2ff;
  background-color: #f5f9ff;
}

.question-item.selected {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.question-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.question-score {
  font-size: 12px;
  color: #909399;
}

.question-actions {
  display: flex;
  gap: 8px;
}

.question-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.question-text {
  font-size: 14px;
  line-height: 1.5;
  color: #303133;
}

.question-options {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #606266;
}

.option-preview {
  display: block;
}

.more-options {
  color: #909399;
  font-style: italic;
}

.question-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

.paper-structure {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.structure-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
}

.group-name {
  min-width: 80px;
  font-weight: 600;
}

.group-count,
.group-score {
  min-width: 60px;
  font-size: 12px;
  color: #606266;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 30px;
}
</style>
