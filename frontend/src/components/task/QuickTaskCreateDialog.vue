<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="60%"
    :before-close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <!-- 批量生成题目 -->
      <template v-if="taskType === 'batch-generation'">
        <el-form-item label="生成类型" prop="generationType">
          <el-radio-group v-model="formData.generationType">
            <el-radio label="ai-question">AI生成题目</el-radio>
            <el-radio label="paper-generation">试卷生成</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="知识库" prop="knowledgeBaseId">
          <el-select v-model="formData.knowledgeBaseId" placeholder="请选择知识库">
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="题目数量" prop="questionCount">
          <el-input-number
            v-model="formData.questionCount"
            :min="1"
            :max="100"
            placeholder="请输入题目数量"
          />
        </el-form-item>

        <el-form-item label="题型配置">
          <div class="question-types">
            <div v-for="type in questionTypes" :key="type.value" class="type-item">
              <el-checkbox v-model="type.enabled">{{ type.label }}</el-checkbox>
              <el-input-number
                v-if="type.enabled"
                v-model="type.count"
                :min="0"
                :max="50"
                size="small"
              />
            </div>
          </div>
        </el-form-item>

        <el-form-item label="难度分布">
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="简单">
                <el-input-number
                  v-model="formData.difficulty.easy"
                  :min="0"
                  :max="100"
                  size="small"
                />
                <span class="unit">%</span>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="中等">
                <el-input-number
                  v-model="formData.difficulty.medium"
                  :min="0"
                  :max="100"
                  size="small"
                />
                <span class="unit">%</span>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="困难">
                <el-input-number
                  v-model="formData.difficulty.hard"
                  :min="0"
                  :max="100"
                  size="small"
                />
                <span class="unit">%</span>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form-item>

        <el-form-item label="生成要求">
          <el-input
            v-model="formData.requirements"
            type="textarea"
            :rows="3"
            placeholder="描述生成题目的具体要求..."
          />
        </el-form-item>
      </template>

      <!-- 批量评估答案 -->
      <template v-else-if="taskType === 'batch-evaluation'">
        <el-form-item label="评估范围" prop="evaluationScope">
          <el-radio-group v-model="formData.evaluationScope">
            <el-radio label="exam">按考试批量评估</el-radio>
            <el-radio label="question">按题目批量评估</el-radio>
            <el-radio label="answers">按答案ID评估</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item
          v-if="formData.evaluationScope === 'exam'"
          label="选择考试"
          prop="examId"
        >
          <el-select v-model="formData.examId" placeholder="请选择考试">
            <el-option
              v-for="exam in exams"
              :key="exam.id"
              :label="exam.title"
              :value="exam.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item
          v-if="formData.evaluationScope === 'question'"
          label="选择题目"
          prop="questionId"
        >
          <el-select v-model="formData.questionId" placeholder="请选择题目">
            <el-option
              v-for="question in questions"
              :key="question.id"
              :label="question.title"
              :value="question.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item
          v-if="formData.evaluationScope === 'answers'"
          label="答案ID列表"
          prop="answerIds"
        >
          <el-input
            v-model="formData.answerIds"
            type="textarea"
            :rows="3"
            placeholder="请输入答案ID，多个ID用逗号分隔"
          />
        </el-form-item>

        <el-form-item label="AI配置" prop="aiConfigId">
          <el-select v-model="formData.aiConfigId" placeholder="请选择AI配置">
            <el-option
              v-for="config in aiConfigs"
              :key="config.id"
              :label="config.name"
              :value="config.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="评分标准">
          <el-select v-model="formData.rubricId" placeholder="请选择评分标准（可选）">
            <el-option
              v-for="rubric in rubrics"
              :key="rubric.id"
              :label="rubric.name"
              :value="rubric.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="批量设置">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="批量大小">
                <el-input-number
                  v-model="formData.batchSize"
                  :min="1"
                  :max="100"
                  size="small"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="并发数">
                <el-input-number
                  v-model="formData.concurrency"
                  :min="1"
                  :max="10"
                  size="small"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form-item>
      </template>

      <!-- 知识库处理 -->
      <template v-else-if="taskType === 'knowledge-processing'">
        <el-form-item label="处理类型" prop="processingType">
          <el-radio-group v-model="formData.processingType">
            <el-radio label="document-upload">文档上传处理</el-radio>
            <el-radio label="reindex">重新索引</el-radio>
            <el-radio label="cleanup">清理无效数据</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="知识库" prop="knowledgeBaseId">
          <el-select v-model="formData.knowledgeBaseId" placeholder="请选择知识库">
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item
          v-if="formData.processingType === 'document-upload'"
          label="文档文件"
        >
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :file-list="fileList"
            :on-change="handleFileChange"
            drag
            multiple
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 PDF、DOC、DOCX、TXT 格式
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="处理选项">
          <el-checkbox-group v-model="formData.processingOptions">
            <el-checkbox label="extractText">提取文本内容</el-checkbox>
            <el-checkbox label="generateSummary">生成文档摘要</el-checkbox>
            <el-checkbox label="createIndex">创建搜索索引</el-checkbox>
            <el-checkbox label="detectLanguage">检测文档语言</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </template>

      <!-- AI试卷生成 -->
      <template v-else-if="taskType === 'paper-generation'">
        <el-form-item label="试卷标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入试卷标题" />
        </el-form-item>

        <el-form-item label="知识库" prop="knowledgeBaseId">
          <el-select v-model="formData.knowledgeBaseId" placeholder="请选择知识库">
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="试卷配置">
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="考试时长">
                <el-input-number
                  v-model="formData.duration"
                  :min="30"
                  :max="300"
                  size="small"
                />
                <span class="unit">分钟</span>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="总分">
                <el-input-number
                  v-model="formData.totalScore"
                  :min="50"
                  :max="200"
                  size="small"
                />
                <span class="unit">分</span>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="难度">
                <el-select v-model="formData.difficulty" size="small">
                  <el-option label="简单为主" value="EASY" />
                  <el-option label="中等难度" value="MEDIUM" />
                  <el-option label="困难为主" value="HARD" />
                  <el-option label="混合难度" value="MIXED" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form-item>

        <el-form-item label="题型配置">
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="选择题">
                <el-input-number
                  v-model="formData.multipleChoiceCount"
                  :min="0"
                  :max="20"
                  size="small"
                />
                <span class="unit">道</span>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="填空题">
                <el-input-number
                  v-model="formData.fillBlankCount"
                  :min="0"
                  :max="20"
                  size="small"
                />
                <span class="unit">道</span>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="简答题">
                <el-input-number
                  v-model="formData.shortAnswerCount"
                  :min="0"
                  :max="10"
                  size="small"
                />
                <span class="unit">道</span>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form-item>

        <el-form-item label="生成要求">
          <el-input
            v-model="formData.requirements"
            type="textarea"
            :rows="3"
            placeholder="描述试卷的具体要求..."
          />
        </el-form-item>
      </template>

      <!-- 评分标准生成 -->
      <template v-else-if="taskType === 'rubric-generation'">
        <el-form-item label="生成范围" prop="generationScope">
          <el-radio-group v-model="formData.generationScope">
            <el-radio label="exam">整个考试</el-radio>
            <el-radio label="question">单个题目</el-radio>
            <el-radio label="batch">批量题目</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="formData.generationScope === 'exam'" label="选择考试" prop="examId">
          <el-select v-model="formData.examId" placeholder="请选择考试">
            <el-option
              v-for="exam in exams"
              :key="exam.id"
              :label="exam.title"
              :value="exam.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-if="formData.generationScope === 'question'" label="选择题目" prop="questionId">
          <el-select v-model="formData.questionId" placeholder="请选择题目">
            <el-option
              v-for="question in questions"
              :key="question.id"
              :label="question.title"
              :value="question.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-if="formData.generationScope === 'batch'" label="题目ID列表" prop="questionIds">
          <el-input
            v-model="formData.questionIds"
            type="textarea"
            :rows="3"
            placeholder="请输入题目ID，多个ID用逗号分隔"
          />
        </el-form-item>

        <el-form-item label="评分标准类型">
          <el-checkbox-group v-model="formData.rubricTypes">
            <el-checkbox label="knowledge">知识点覆盖</el-checkbox>
            <el-checkbox label="accuracy">答案准确性</el-checkbox>
            <el-checkbox label="completeness">回答完整性</el-checkbox>
            <el-checkbox label="logic">逻辑清晰度</el-checkbox>
            <el-checkbox label="expression">表达规范性</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="分值配置">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="总分">
                <el-input-number
                  v-model="formData.totalScore"
                  :min="1"
                  :max="100"
                  size="small"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="评分档次">
                <el-select v-model="formData.scoreLevels" size="small">
                  <el-option label="3档 (优良差)" value="3" />
                  <el-option label="4档 (优良中差)" value="4" />
                  <el-option label="5档 (A-B-C-D-E)" value="5" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form-item>

        <el-form-item label="生成要求">
          <el-input
            v-model="formData.requirements"
            type="textarea"
            :rows="3"
            placeholder="描述评分标准的具体要求，如评分侧重点、特殊要求等..."
          />
        </el-form-item>
      </template>

      <!-- 任务描述 -->
      <el-form-item label="任务描述">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="2"
          placeholder="可选：为此任务添加描述..."
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="createTask" :loading="creating">
          创建任务
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { evaluationApi } from '@/api/evaluation'
import { questionApi } from '@/api/question'
import { knowledgeBaseApi } from '@/api/knowledge'
import { examApi } from '@/api/exam'
import type { FormInstance, UploadUserFile } from 'element-plus'

interface Props {
  visible: boolean
  taskType: string
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'task-created', task: any): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const dialogVisible = ref(false)
const creating = ref(false)
const formRef = ref<FormInstance>()
const uploadRef = ref()
const fileList = ref<UploadUserFile[]>([])

// 数据选项
const knowledgeBases = ref<any[]>([])
const exams = ref<any[]>([])
const questions = ref<any[]>([])
const aiConfigs = ref<any[]>([])
const rubrics = ref<any[]>([])

// 表单数据
const formData = reactive({
  // 通用字段
  description: '',
  
  // 生成任务字段
  generationType: 'ai-question',
  knowledgeBaseId: '' as string | number,
  questionCount: 10,
  requirements: '',
  difficulty: {
    easy: 30,
    medium: 50,
    hard: 20
  },
  
  // 评估任务字段
  evaluationScope: 'exam',
  examId: '' as string | number,
  questionId: '' as string | number,
  answerIds: '',
  aiConfigId: '' as string | number,
  rubricId: '' as string | number,
  batchSize: 10,
  concurrency: 3,
  
  // 知识库处理字段
  processingType: 'document-upload',
  processingOptions: ['extractText', 'createIndex'],
  
  // 试卷生成字段
  title: '',
  duration: 120,
  totalScore: 100,
  multipleChoiceCount: 10,
  fillBlankCount: 5,
  shortAnswerCount: 3,
  
  // 评分标准生成字段
  generationScope: 'exam',
  questionIds: '',
  rubricTypes: ['knowledge', 'accuracy'],
  scoreLevels: '4'
})

// 题型配置
const questionTypes = reactive([
  { value: 'multiple_choice', label: '选择题', enabled: true, count: 5 },
  { value: 'fill_blank', label: '填空题', enabled: true, count: 3 },
  { value: 'short_answer', label: '简答题', enabled: true, count: 2 },
  { value: 'essay', label: '论述题', enabled: false, count: 0 }
])

// 计算属性
const dialogTitle = computed(() => {
  const titleMap = {
    'batch-generation': '批量生成题目',
    'batch-evaluation': '批量评估答案',
    'knowledge-processing': '知识库处理',
    'paper-generation': 'AI试卷生成',
    'rubric-generation': '评分标准生成'
  }
  return titleMap[props.taskType as keyof typeof titleMap] || '创建任务'
})

// 表单验证规则
const formRules = computed(() => {
  const baseRules = {
    knowledgeBaseId: [
      { required: true, message: '请选择知识库', trigger: 'change' }
    ]
  }
  
  if (props.taskType === 'batch-generation') {
    return {
      ...baseRules,
      generationType: [
        { required: true, message: '请选择生成类型', trigger: 'change' }
      ],
      questionCount: [
        { required: true, message: '请输入题目数量', trigger: 'blur' }
      ]
    }
  } else if (props.taskType === 'batch-evaluation') {
    return {
      evaluationScope: [
        { required: true, message: '请选择评估范围', trigger: 'change' }
      ],
      aiConfigId: [
        { required: true, message: '请选择AI配置', trigger: 'change' }
      ]
    }
  } else if (props.taskType === 'paper-generation') {
    return {
      ...baseRules,
      title: [
        { required: true, message: '请输入试卷标题', trigger: 'blur' }
      ]
    }
  } else if (props.taskType === 'rubric-generation') {
    return {
      generationScope: [
        { required: true, message: '请选择生成范围', trigger: 'change' }
      ],
      rubricTypes: [
        { required: true, message: '请选择评分标准类型', trigger: 'change' }
      ],
      scoreLevels: [
        { required: true, message: '请选择评分档次', trigger: 'change' }
      ]
    }
  }
  
  return baseRules
})

// 监听器
watch(() => props.visible, (newVal) => {
  dialogVisible.value = newVal
  if (newVal) {
    loadOptions()
  }
})

watch(dialogVisible, (newVal) => {
  if (!newVal) {
    emit('update:visible', false)
    resetForm()
  }
})

// 方法
const handleClose = () => {
  dialogVisible.value = false
}

const loadOptions = async () => {
  try {
    // 加载知识库列表
    if (['batch-generation', 'knowledge-processing', 'paper-generation'].includes(props.taskType)) {
      const response = await knowledgeBaseApi.getKnowledgeBases()
      knowledgeBases.value = response.content || []
    }
    
    // 加载考试列表
    if (['batch-evaluation', 'rubric-generation'].includes(props.taskType)) {
      exams.value = await examApi.getExams()
      // 加载AI配置和评分标准
      // aiConfigs.value = await userAIConfigApi.getConfigs()
      // rubrics.value = await rubricApi.getRubrics()
    }
    
    // 加载题目列表
    if (props.taskType === 'rubric-generation') {
      questions.value = await questionApi.getQuestions()
    }
  } catch (error) {
    console.error('加载选项失败:', error)
  }
}

const handleFileChange = (file: any, fileList: UploadUserFile[]) => {
  // 处理文件变更
}

const createTask = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    creating.value = true
    
    let task
    
    if (props.taskType === 'batch-generation') {
      task = await createGenerationTask()
    } else if (props.taskType === 'batch-evaluation') {
      task = await createEvaluationTask()
    } else if (props.taskType === 'knowledge-processing') {
      task = await createKnowledgeTask()
    } else if (props.taskType === 'paper-generation') {
      task = await createPaperTask()
    } else if (props.taskType === 'rubric-generation') {
      task = await createRubricTask()
    }
    
    emit('task-created', task)
    dialogVisible.value = false
    ElMessage.success('任务创建成功')
    
  } catch (error) {
    console.error('创建任务失败:', error)
    ElMessage.error('创建任务失败')
  } finally {
    creating.value = false
  }
}

const createGenerationTask = async () => {
  const enabledTypes = questionTypes.filter(t => t.enabled)
  const typeConfigs = enabledTypes.map(t => ({
    type: t.value,
    count: t.count
  }))
  
  // 这里调用实际的生成API
  return {
    taskId: 'gen-' + Date.now(),
    type: 'generation',
    status: 'PENDING',
    config: {
      generationType: formData.generationType,
      knowledgeBaseId: formData.knowledgeBaseId,
      questionCount: formData.questionCount,
      questionTypes: typeConfigs,
      difficulty: formData.difficulty,
      requirements: formData.requirements
    }
  }
}

const createEvaluationTask = async () => {
  const taskData = {
    evaluationScope: formData.evaluationScope,
    examId: formData.examId,
    questionId: formData.questionId,
    answerIds: formData.answerIds ? formData.answerIds.split(',').map(id => parseInt(id.trim())) : [],
    aiConfigId: formData.aiConfigId,
    rubricId: formData.rubricId,
    batchSize: formData.batchSize,
    concurrency: formData.concurrency,
    description: formData.description
  }
  
  // 调用实际的评估API
  const response = await evaluationApi.createBatchTask(taskData)
  return response
}

const createKnowledgeTask = async () => {
  // 这里调用实际的知识库处理API
  return {
    taskId: 'kb-' + Date.now(),
    type: 'knowledge',
    status: 'PENDING',
    config: {
      processingType: formData.processingType,
      knowledgeBaseId: formData.knowledgeBaseId,
      processingOptions: formData.processingOptions,
      fileCount: fileList.value.length
    }
  }
}

const createPaperTask = async () => {
  // 这里调用实际的试卷生成API
  return {
    taskId: 'paper-' + Date.now(),
    type: 'generation',
    status: 'PENDING',
    config: {
      title: formData.title,
      knowledgeBaseId: formData.knowledgeBaseId,
      duration: formData.duration,
      totalScore: formData.totalScore,
      difficulty: formData.difficulty,
      questionTypes: {
        multipleChoice: formData.multipleChoiceCount,
        fillBlank: formData.fillBlankCount,
        shortAnswer: formData.shortAnswerCount
      },
      requirements: formData.requirements
    }
  }
}

const createRubricTask = async () => {
  const taskData = {
    generationScope: formData.generationScope,
    examId: formData.examId,
    questionId: formData.questionId,
    questionIds: formData.questionIds ? formData.questionIds.split(',').map(id => parseInt(id.trim())) : [],
    rubricTypes: formData.rubricTypes,
    totalScore: formData.totalScore,
    scoreLevels: parseInt(formData.scoreLevels),
    requirements: formData.requirements,
    description: formData.description
  }
  
  // 这里调用实际的评分标准生成API
  return {
    taskId: 'rubric-' + Date.now(),
    type: 'rubric-generation',
    status: 'PENDING',
    config: taskData
  }
}

const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  fileList.value = []
  questionTypes.forEach(type => {
    type.enabled = ['multiple_choice', 'fill_blank', 'short_answer'].includes(type.value)
    type.count = type.value === 'multiple_choice' ? 5 : 
                 type.value === 'fill_blank' ? 3 : 
                 type.value === 'short_answer' ? 2 : 0
  })
}

// 生命周期
onMounted(() => {
  resetForm()
})
</script>

<style scoped>
.question-types {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 12px;
}

.type-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.type-item:last-child {
  margin-bottom: 0;
}

.unit {
  margin-left: 8px;
  color: #909399;
  font-size: 14px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}

:deep(.el-upload-dragger) {
  width: 100%;
}
</style>
