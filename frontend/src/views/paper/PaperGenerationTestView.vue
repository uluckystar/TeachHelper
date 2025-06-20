<template>
  <div class="paper-generation-test">
    <div class="page-header">
      <h1>AI试卷生成</h1>
      <p class="page-description">基于知识库内容和AI技术自动生成试卷</p>
    </div>

    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <el-icon class="header-icon"><MagicStick /></el-icon>
          <span>智能试卷生成器</span>
        </div>
      </template>

      <el-row :gutter="24">
        <!-- 基础配置 -->
        <el-col :span="16">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
            <el-form-item label="试卷标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入试卷标题" />
            </el-form-item>

            <el-form-item label="知识库选择" prop="knowledgeBaseId">
              <el-select v-model="form.knowledgeBaseId" placeholder="请选择知识库" style="width: 100%">
                <el-option
                  v-for="kb in knowledgeBases"
                  :key="kb.id"
                  :label="kb.name"
                  :value="kb.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="题目类型配置">
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="选择题" prop="multipleChoiceCount">
                    <el-input-number v-model="form.multipleChoiceCount" :min="0" :max="20" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="填空题" prop="fillBlankCount">
                    <el-input-number v-model="form.fillBlankCount" :min="0" :max="20" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="简答题" prop="shortAnswerCount">
                    <el-input-number v-model="form.shortAnswerCount" :min="0" :max="10" />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form-item>

            <el-form-item label="难度分布" prop="difficulty">
              <el-radio-group v-model="form.difficulty">
                <el-radio value="EASY">简单为主</el-radio>
                <el-radio value="MEDIUM">中等难度</el-radio>
                <el-radio value="HARD">困难为主</el-radio>
                <el-radio value="MIXED">混合难度</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="考试时长" prop="duration">
              <el-input-number v-model="form.duration" :min="30" :max="300" /> 分钟
            </el-form-item>

            <el-form-item label="总分" prop="totalScore">
              <el-input-number v-model="form.totalScore" :min="50" :max="200" />
            </el-form-item>

            <el-form-item label="生成要求" prop="requirements">
              <el-input
                v-model="form.requirements"
                type="textarea"
                :rows="4"
                placeholder="描述试卷的具体要求，比如重点考察的知识点、题目风格等..."
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="generatePaper" :loading="generating">
                <el-icon><MagicStick /></el-icon>
                生成试卷
              </el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-col>

        <!-- 生成结果预览 -->
        <el-col :span="8">
          <el-card>
            <template #header>
              <div class="card-header">
                <el-icon class="header-icon"><Document /></el-icon>
                <span>生成结果</span>
              </div>
            </template>

            <div v-if="!generationResult && !generating" class="empty-state">
              <el-empty description="请配置试卷参数并开始生成" />
            </div>

            <div v-if="generating" class="generating-state">
              <div class="generation-loading">
                <el-icon class="is-loading"><Loading /></el-icon>
                <h3>{{ progressText }}</h3>
                <p>AI正在智能分析知识库内容并生成试卷，请耐心等待...</p>
              </div>
            </div>

            <div v-if="generationResult" class="result-preview">
              <div class="result-header">
                <h3>{{ generationResult.title }}</h3>
                <el-tag type="success">生成成功</el-tag>
              </div>
              
              <div class="result-stats">
                <el-statistic title="题目总数" :value="generationResult.totalQuestions" />
                <el-statistic title="总分" :value="generationResult.totalScore" />
                <el-statistic title="考试时长" :value="generationResult.duration" suffix="分钟" />
              </div>

              <div class="question-types">
                <div v-for="type in generationResult.questionTypes" :key="type.type" class="type-item">
                  <span>{{ getQuestionTypeName(type.type) }}</span>
                  <el-tag size="small">{{ type.count }}题</el-tag>
                </div>
              </div>

              <div class="result-actions">
                <el-button type="success" @click="savePaper">
                  保存试卷
                </el-button>
                <el-button @click="previewPaper">
                  预览详情
                </el-button>
                <el-button @click="regenerate">
                  重新生成
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MagicStick, Document, Loading } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import { knowledgeBaseApi } from '@/api/knowledge'
import { paperGenerationApi } from '@/api/paperGeneration'

const router = useRouter()

// 响应式数据
const formRef = ref<FormInstance>()
const generating = ref(false)
const progressText = ref('')
const knowledgeBases = ref<any[]>([])
const generationResult = ref<any>(null)

const form = reactive({
  title: '',
  knowledgeBaseId: '',
  multipleChoiceCount: 10,
  fillBlankCount: 5,
  shortAnswerCount: 3,
  difficulty: 'MEDIUM',
  duration: 120,
  totalScore: 100,
  requirements: ''
})

const rules = {
  title: [
    { required: true, message: '请输入试卷标题', trigger: 'blur' }
  ],
  knowledgeBaseId: [
    { required: true, message: '请选择知识库', trigger: 'change' }
  ]
}

// 方法
const generatePaper = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    
    generating.value = true
    progressText.value = '正在生成试卷，请稍候...'
    
    // 调用后端API生成试卷（同步）
    const result = await paperGenerationApi.generatePaper({
      title: form.title,
      description: form.requirements,
      knowledgeBaseId: form.knowledgeBaseId,
      multipleChoiceCount: form.multipleChoiceCount,
      fillBlankCount: form.fillBlankCount,
      shortAnswerCount: form.shortAnswerCount,
      difficulty: form.difficulty as 'EASY' | 'MEDIUM' | 'HARD',
      duration: form.duration,
      totalScore: form.totalScore,
      requirements: form.requirements
    })
    
    // 设置生成结果
    generationResult.value = result
    ElMessage.success('AI试卷生成完成！')
    
  } catch (error: any) {
    console.error('试卷生成失败:', error)
    ElMessage.error(error.message || '试卷生成失败')
  } finally {
    generating.value = false
    progressText.value = ''
  }
}

const resetForm = () => {
  formRef.value?.resetFields()
  generationResult.value = null
}

const savePaper = () => {
  ElMessage.success('试卷保存成功！')
  router.push('/exams')
}

const previewPaper = () => {
  ElMessage.info('试卷预览功能开发中...')
}

const regenerate = () => {
  generationResult.value = null
  generatePaper()
}

const getQuestionTypeName = (type: string) => {
  const types: Record<string, string> = {
    'MULTIPLE_CHOICE': '选择题',
    'TRUE_FALSE': '判断题',
    'FILL_BLANK': '填空题',
    'SHORT_ANSWER': '简答题',
    'ESSAY': '论述题'
  }
  return types[type] || type
}

// 初始化
onMounted(async () => {
  try {
    // 获取知识库列表
    const response = await knowledgeBaseApi.getKnowledgeBases({ page: 0, size: 100 })
    knowledgeBases.value = response.content || []
  } catch (error) {
    console.error('加载知识库列表失败:', error)
    ElMessage.error('加载知识库列表失败')
    // 使用模拟数据作为备选
    knowledgeBases.value = [
      { id: '1', name: '高等数学知识库' },
      { id: '2', name: '线性代数知识库' },
      { id: '3', name: '概率论知识库' },
      { id: '4', name: '计算机网络知识库' },
      { id: '5', name: '数据结构知识库' }
    ]
  }
})
</script>

<style scoped>
.paper-generation-test {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 24px;
  font-weight: 500;
}

.page-description {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.main-card {
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.header-icon {
  color: #409eff;
  font-size: 18px;
}

.empty-state {
  padding: 60px 20px;
  text-align: center;
}

.generating-state {
  text-align: center;
  padding: 40px;
}

.generation-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.generation-loading .el-icon {
  font-size: 48px;
  color: #409eff;
}

.generation-loading h3 {
  margin: 0;
  color: #303133;
  font-size: 18px;
}

.generation-loading p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.result-preview {
  padding: 16px 0;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.result-header h3 {
  margin: 0;
  color: #303133;
  font-size: 16px;
}

.result-stats {
  margin-bottom: 16px;
}

.result-stats .el-statistic {
  margin-bottom: 12px;
}

.question-types {
  margin-bottom: 20px;
}

.type-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.type-item:last-child {
  border-bottom: none;
}

.result-actions {
  text-align: center;
}

.result-actions .el-button {
  margin: 0 4px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .paper-generation-test {
    padding: 16px;
  }
  
  .el-row {
    flex-direction: column;
  }
  
  .el-col {
    width: 100% !important;
    margin-bottom: 16px;
  }
}
</style>
