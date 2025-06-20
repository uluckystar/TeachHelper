<template>
  <div class="generate-questions-library-view">
    <div class="page-header">
      <el-breadcrumb>
        <el-breadcrumb-item :to="{ path: '/questions' }">题目库</el-breadcrumb-item>
        <el-breadcrumb-item>AI生成题目</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <el-row :gutter="24">
      <!-- AI生成快捷入口 -->
      <el-col :span="24" class="quick-actions">
        <el-card>
          <template #header>
            <div class="card-header">
              <el-icon class="header-icon"><MagicStick /></el-icon>
              <span>AI智能生成题目</span>
            </div>
          </template>
          
          <el-row :gutter="16" class="quick-generate">
            <el-col :span="6">
              <el-card class="quick-card" @click="quickGenerate('math')">
                <div class="quick-content">
                  <el-icon size="32" color="#409eff"><Reading /></el-icon>
                  <h4>数学题目</h4>
                  <p>快速生成数学相关题目</p>
                </div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="quick-card" @click="quickGenerate('chinese')">
                <div class="quick-content">
                  <el-icon size="32" color="#67c23a"><Reading /></el-icon>
                  <h4>语文题目</h4>
                  <p>快速生成语文相关题目</p>
                </div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="quick-card" @click="quickGenerate('english')">
                <div class="quick-content">
                  <el-icon size="32" color="#e6a23c"><ChatLineRound /></el-icon>
                  <h4>英语题目</h4>
                  <p>快速生成英语相关题目</p>
                </div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="quick-card" @click="quickGenerate('science')">
                <div class="quick-content">
                  <el-icon size="32" color="#f56c6c"><Document /></el-icon>
                  <h4>理科题目</h4>
                  <p>快速生成理科相关题目</p>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </el-card>
      </el-col>

      <!-- 自定义生成配置 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <el-icon class="header-icon"><Setting /></el-icon>
              <span>自定义生成配置</span>
            </div>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
            <el-form-item label="题目类型" prop="questionType">
              <el-checkbox-group v-model="form.questionTypes">
                <el-checkbox label="MULTIPLE_CHOICE">选择题</el-checkbox>
                <el-checkbox label="TRUE_FALSE">判断题</el-checkbox>
                <el-checkbox label="FILL_BLANK">填空题</el-checkbox>
                <el-checkbox label="SHORT_ANSWER">简答题</el-checkbox>
                <el-checkbox label="ESSAY">论述题</el-checkbox>
              </el-checkbox-group>
            </el-form-item>

            <el-form-item label="学科领域" prop="subject">
              <el-select v-model="form.subject" placeholder="请选择学科" style="width: 100%">
                <el-option label="数学" value="math" />
                <el-option label="语文" value="chinese" />
                <el-option label="英语" value="english" />
                <el-option label="物理" value="physics" />
                <el-option label="化学" value="chemistry" />
                <el-option label="生物" value="biology" />
                <el-option label="历史" value="history" />
                <el-option label="地理" value="geography" />
                <el-option label="政治" value="politics" />
                <el-option label="计算机" value="computer" />
                <el-option label="其他" value="other" />
              </el-select>
            </el-form-item>

            <el-form-item label="年级水平" prop="gradeLevel">
              <el-select v-model="form.gradeLevel" placeholder="请选择年级" style="width: 100%">
                <el-option label="小学" value="primary" />
                <el-option label="初中" value="junior" />
                <el-option label="高中" value="senior" />
                <el-option label="大学" value="university" />
              </el-select>
            </el-form-item>

            <el-form-item label="题目数量" prop="count">
              <el-input-number v-model="form.count" :min="1" :max="50" />
            </el-form-item>

            <el-form-item label="难度级别" prop="difficulty">
              <el-radio-group v-model="form.difficulty">
                <el-radio label="EASY">简单</el-radio>
                <el-radio label="MEDIUM">中等</el-radio>
                <el-radio label="HARD">困难</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="知识点" prop="knowledgePoints">
              <el-select 
                v-model="form.knowledgePoints" 
                multiple 
                placeholder="请选择相关知识点（可选）"
                style="width: 100%"
                filterable
                allow-create
              >
                <el-option
                  v-for="point in knowledgePoints"
                  :key="point.id"
                  :label="point.name"
                  :value="point.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="题目描述" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="4"
                placeholder="请详细描述你想要生成的题目要求，比如：涉及的具体知识点、题目风格、应用场景等"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="generateQuestions" :loading="generating">
                <el-icon><MagicStick /></el-icon>
                开始生成题目
              </el-button>
              <el-button @click="resetForm">重置表单</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 生成结果和历史 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <el-icon class="header-icon"><Document /></el-icon>
              <span>生成结果</span>
            </div>
          </template>

          <div v-if="!generatedQuestions.length && !generating" class="empty-state">
            <el-empty description="暂无生成的题目" />
          </div>

          <div v-if="generating" class="generating-state">
            <el-skeleton :rows="3" animated />
            <p class="generating-text">AI正在生成题目，请稍候...</p>
          </div>

          <div v-if="generatedQuestions.length" class="result-list">
            <div class="result-summary">
              <el-statistic title="生成数量" :value="generatedQuestions.length" suffix="道题目" />
            </div>
            
            <div class="question-previews">
              <div 
                v-for="(question, index) in generatedQuestions.slice(0, 3)" 
                :key="index"
                class="question-preview"
              >
                <div class="preview-header">
                  <el-tag :type="getQuestionTypeColor(question.type) as any" size="small">
                    {{ getQuestionTypeName(question.type) }}
                  </el-tag>
                  <el-tag type="info" size="small">{{ question.difficulty }}</el-tag>
                </div>
                <div class="preview-content">
                  {{ question.content.substring(0, 80) }}...
                </div>
              </div>
              
              <div v-if="generatedQuestions.length > 3" class="more-indicator">
                还有 {{ generatedQuestions.length - 3 }} 道题目...
              </div>
            </div>

            <div class="result-actions">
              <el-button type="success" @click="saveAllQuestions" :loading="saving">
                保存到题目库
              </el-button>
              <el-button @click="previewAll">
                查看全部
              </el-button>
            </div>
          </div>
        </el-card>

        <!-- 生成历史 -->
        <el-card class="history-card">
          <template #header>
            <div class="card-header">
              <el-icon class="header-icon"><Clock /></el-icon>
              <span>生成历史</span>
            </div>
          </template>

          <div v-if="!generateHistory.length" class="empty-state">
            <el-empty description="暂无生成历史" />
          </div>

          <el-timeline v-else>
            <el-timeline-item
              v-for="item in generateHistory"
              :key="item.id"
              :timestamp="formatTime(item.createdAt)"
            >
              <div class="history-item">
                <div class="history-title">{{ item.subject }} - {{ item.count }}道题目</div>
                <div class="history-meta">
                  <el-tag size="small">{{ item.difficulty }}</el-tag>
                  <span class="history-types">{{ item.types.join(', ') }}</span>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  MagicStick, 
  Setting, 
  Document, 
  Clock,
  Reading,
  ChatLineRound
} from '@element-plus/icons-vue'
import { questionApi } from '@/api/question'
import type { FormInstance } from 'element-plus'

const router = useRouter()

// 响应式数据
const formRef = ref<FormInstance>()
const generating = ref(false)
const saving = ref(false)
const knowledgePoints = ref<any[]>([])
const generatedQuestions = ref<any[]>([])
const generateHistory = ref<any[]>([])

const form = reactive({
  questionTypes: ['MULTIPLE_CHOICE'],
  subject: '',
  gradeLevel: '',
  count: 10,
  difficulty: 'MEDIUM',
  knowledgePoints: [] as string[],
  description: ''
})

const rules = {
  questionTypes: [
    { required: true, message: '请选择至少一种题目类型', trigger: 'change' }
  ],
  subject: [
    { required: true, message: '请选择学科领域', trigger: 'change' }
  ],
  gradeLevel: [
    { required: true, message: '请选择年级水平', trigger: 'change' }
  ]
}

// 方法
const quickGenerate = (subject: string) => {
  form.subject = subject
  form.questionTypes = ['MULTIPLE_CHOICE', 'TRUE_FALSE']
  form.count = 10
  form.difficulty = 'MEDIUM'
  
  // 根据学科设置默认描述
  const descriptions: Record<string, string> = {
    math: '生成涵盖基础运算、代数、几何等知识点的数学题目',
    chinese: '生成包含阅读理解、文言文、作文等类型的语文题目',
    english: '生成涵盖语法、词汇、阅读理解等的英语题目',
    science: '生成物理、化学、生物相关的理科综合题目'
  }
  
  form.description = descriptions[subject] || ''
  generateQuestions()
}

const generateQuestions = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    
    generating.value = true
    
    // 调用AI生成题目API
    const response = await questionApi.generateQuestions({
      ...form,
      questionType: form.questionTypes[0] // 暂时只支持单个类型
    })
    
    generatedQuestions.value = response.data
    
    // 添加到历史记录
    generateHistory.value.unshift({
      id: Date.now(),
      subject: getSubjectName(form.subject),
      count: form.count,
      difficulty: getDifficultyName(form.difficulty),
      types: form.questionTypes.map(type => getQuestionTypeName(type)),
      createdAt: new Date().toISOString()
    })
    
    ElMessage.success(`成功生成 ${response.data.length} 道题目`)
    
  } catch (error: any) {
    console.error('生成题目失败:', error)
    ElMessage.error(error.message || '生成题目失败')
  } finally {
    generating.value = false
  }
}

const saveAllQuestions = async () => {
  if (!generatedQuestions.value.length) {
    ElMessage.warning('没有可保存的题目')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要将这 ${generatedQuestions.value.length} 道题目保存到题目库吗？`,
      '确认保存',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    saving.value = true

    await questionApi.saveGeneratedQuestions({
      questions: generatedQuestions.value
    })

    ElMessage.success('题目保存成功')
    router.push('/questions')

  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('保存题目失败:', error)
      ElMessage.error(error.message || '保存题目失败')
    }
  } finally {
    saving.value = false
  }
}

const previewAll = () => {
  // 这里可以打开一个对话框展示所有生成的题目
  ElMessage.info('题目预览功能开发中...')
}

const resetForm = () => {
  formRef.value?.resetFields()
  generatedQuestions.value = []
}

// 工具方法
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

const getQuestionTypeColor = (type: string) => {
  const colors: Record<string, string> = {
    'MULTIPLE_CHOICE': 'primary',
    'TRUE_FALSE': 'success',
    'FILL_BLANK': 'warning',
    'SHORT_ANSWER': 'info',
    'ESSAY': 'danger'
  }
  return colors[type] || 'info'
}

const getSubjectName = (subject: string) => {
  const subjects: Record<string, string> = {
    math: '数学',
    chinese: '语文',
    english: '英语',
    physics: '物理',
    chemistry: '化学',
    biology: '生物',
    history: '历史',
    geography: '地理',
    politics: '政治',
    computer: '计算机'
  }
  return subjects[subject] || subject
}

const getDifficultyName = (difficulty: string) => {
  const difficulties: Record<string, string> = {
    'EASY': '简单',
    'MEDIUM': '中等',
    'HARD': '困难'
  }
  return difficulties[difficulty] || difficulty
}

const formatTime = (timeStr: string) => {
  const time = new Date(timeStr)
  return time.toLocaleString('zh-CN')
}

// 初始化
onMounted(async () => {
  try {
    // 加载知识点列表
    const response = await questionApi.getKnowledgePoints()
    knowledgePoints.value = response.data || []
  } catch (error) {
    console.error('加载知识点失败:', error)
  }
})
</script>

<style scoped>
.generate-questions-library-view {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
}

.quick-actions {
  margin-bottom: 24px;
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

.quick-generate {
  margin-top: 16px;
}

.quick-card {
  cursor: pointer;
  transition: all 0.3s;
  text-align: center;
  border: 1px solid #ebeef5;
}

.quick-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #c6e2ff;
}

.quick-content {
  padding: 16px;
}

.quick-content h4 {
  margin: 12px 0 8px 0;
  color: #303133;
  font-size: 16px;
}

.quick-content p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.empty-state {
  padding: 40px 20px;
  text-align: center;
}

.generating-state {
  padding: 20px;
}

.generating-text {
  text-align: center;
  color: #606266;
  margin-top: 16px;
}

.result-summary {
  margin-bottom: 16px;
  text-align: center;
}

.question-previews {
  margin: 16px 0;
}

.question-preview {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 8px;
  background: #fafafa;
}

.preview-header {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.preview-content {
  color: #606266;
  font-size: 14px;
  line-height: 1.4;
}

.more-indicator {
  text-align: center;
  color: #909399;
  font-size: 14px;
  padding: 8px;
}

.result-actions {
  margin-top: 16px;
  text-align: center;
}

.result-actions .el-button {
  margin: 0 4px;
}

.history-card {
  margin-top: 24px;
}

.history-item {
  padding: 8px 0;
}

.history-title {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.history-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.history-types {
  color: #909399;
  font-size: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .generate-questions-library-view {
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
