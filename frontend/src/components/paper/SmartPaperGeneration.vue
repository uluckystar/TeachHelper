<template>
  <div class="smart-paper-generation">
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      @submit.prevent="handleSubmit"
    >
      <!-- 基本信息 -->
      <el-card class="section-card">
        <template #header>
          <span>基本信息</span>
        </template>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="试卷标题" prop="title">
              <el-input
                v-model="form.title"
                placeholder="请输入试卷标题"
                clearable
              />
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

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学科" prop="subject">
              <el-select v-model="form.subject" placeholder="请选择学科" style="width: 100%">
                <el-option
                  v-for="subject in subjects"
                  :key="subject"
                  :label="subject"
                  :value="subject"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年级" prop="gradeLevel">
              <el-select v-model="form.gradeLevel" placeholder="请选择年级" style="width: 100%">
                <el-option
                  v-for="grade in gradeLevels"
                  :key="grade"
                  :label="grade"
                  :value="grade"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 智能配置 -->
      <el-card class="section-card">
        <template #header>
          <div class="section-header">
            <span>智能配置</span>
            <el-tooltip content="基于AI分析自动优化试卷结构和难度分布">
              <el-icon><QuestionFilled /></el-icon>
            </el-tooltip>
          </div>
        </template>

        <el-form-item label="题目来源">
          <el-checkbox-group v-model="form.questionSources">
            <el-checkbox label="knowledge_base">知识库内容</el-checkbox>
            <el-checkbox label="question_bank">现有题库</el-checkbox>
            <el-checkbox label="ai_generation">AI生成</el-checkbox>
            <el-checkbox label="manual_selection">手动选择</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="智能优化">
          <el-checkbox-group v-model="form.optimizations">
            <el-checkbox label="difficulty_balance">难度平衡</el-checkbox>
            <el-checkbox label="knowledge_coverage">知识点覆盖</el-checkbox>
            <el-checkbox label="time_estimation">时间估算</el-checkbox>
            <el-checkbox label="score_distribution">分值分布</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="生成策略">
          <el-radio-group v-model="form.strategy">
            <el-radio label="balanced">平衡策略</el-radio>
            <el-radio label="knowledge_focused">知识点导向</el-radio>
            <el-radio label="difficulty_focused">难度导向</el-radio>
            <el-radio label="custom">自定义策略</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-card>

      <!-- 题型配置 -->
      <el-card class="section-card">
        <template #header>
          <span>题型配置</span>
        </template>

        <div class="question-types-config">
          <div
            v-for="(typeConfig, index) in form.questionTypes"
            :key="index"
            class="question-type-item"
          >
            <div class="type-header">
              <el-select
                v-model="typeConfig.type"
                placeholder="选择题型"
                style="width: 150px"
              >
                <el-option label="单选题" value="SINGLE_CHOICE" />
                <el-option label="多选题" value="MULTIPLE_CHOICE" />
                <el-option label="判断题" value="TRUE_FALSE" />
                <el-option label="填空题" value="FILL_BLANK" />
                <el-option label="简答题" value="SHORT_ANSWER" />
                <el-option label="论述题" value="ESSAY" />
              </el-select>
              
              <el-input-number
                v-model="typeConfig.count"
                :min="1"
                :max="50"
                style="width: 100px; margin: 0 10px"
              />
              <span>题</span>

              <el-input-number
                v-model="typeConfig.scorePerQuestion"
                :min="1"
                :max="20"
                :precision="1"
                style="width: 100px; margin: 0 10px"
              />
              <span>分/题</span>

              <el-button
                type="danger"
                size="small"
                @click="removeQuestionType(index)"
                :disabled="form.questionTypes.length === 1"
              >
                删除
              </el-button>
            </div>

            <div class="type-details">
              <el-row :gutter="20">
                <el-col :span="8">
                  <el-form-item label="难度分布" size="small">
                    <div class="difficulty-sliders">
                      <div class="difficulty-item">
                        <span>简单：</span>
                        <el-slider
                          v-model="typeConfig.difficulty.easy"
                          :max="100"
                          style="flex: 1"
                        />
                        <span>{{ typeConfig.difficulty.easy }}%</span>
                      </div>
                      <div class="difficulty-item">
                        <span>中等：</span>
                        <el-slider
                          v-model="typeConfig.difficulty.medium"
                          :max="100"
                          style="flex: 1"
                        />
                        <span>{{ typeConfig.difficulty.medium }}%</span>
                      </div>
                      <div class="difficulty-item">
                        <span>困难：</span>
                        <el-slider
                          v-model="typeConfig.difficulty.hard"
                          :max="100"
                          style="flex: 1"
                        />
                        <span>{{ typeConfig.difficulty.hard }}%</span>
                      </div>
                    </div>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="知识点" size="small">
                    <el-select
                      v-model="typeConfig.knowledgePoints"
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
                  <el-form-item label="标签" size="small">
                    <el-select
                      v-model="typeConfig.tags"
                      multiple
                      filterable
                      allow-create
                      placeholder="添加标签"
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
            </div>
          </div>

          <el-button
            @click="addQuestionType"
            style="width: 100%; margin-top: 10px; border: 2px dashed #dcdfe6; background: transparent;"
          >
            <el-icon><Plus /></el-icon>
            添加题型
          </el-button>
        </div>
      </el-card>

      <!-- 生成预览 -->
      <el-card class="section-card">
        <template #header>
          <span>生成预览</span>
        </template>

        <div class="generation-preview">
          <el-descriptions :column="4" border>
            <el-descriptions-item label="总题数">
              {{ totalQuestions }} 题
            </el-descriptions-item>
            <el-descriptions-item label="总分值">
              {{ totalScore }} 分
            </el-descriptions-item>
            <el-descriptions-item label="预计时长">
              {{ form.duration }} 分钟
            </el-descriptions-item>
            <el-descriptions-item label="难度系数">
              {{ difficultyCoefficient }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="preview-charts" v-if="totalQuestions > 0">
            <div class="chart-item">
              <h4>题型分布</h4>
              <!-- 这里可以添加图表组件 -->
              <div class="chart-placeholder">
                <div
                  v-for="typeConfig in form.questionTypes"
                  :key="typeConfig.type"
                  class="type-bar"
                >
                  <span>{{ getQuestionTypeText(typeConfig.type) }}</span>
                  <div class="bar">
                    <div
                      class="bar-fill"
                      :style="{ width: (typeConfig.count / totalQuestions * 100) + '%' }"
                    ></div>
                  </div>
                  <span>{{ typeConfig.count }}</span>
                </div>
              </div>
            </div>

            <div class="chart-item">
              <h4>分值分布</h4>
              <div class="score-distribution">
                <div
                  v-for="typeConfig in form.questionTypes"
                  :key="typeConfig.type"
                  class="score-item"
                >
                  <span>{{ getQuestionTypeText(typeConfig.type) }}</span>
                  <span>{{ typeConfig.count * typeConfig.scorePerQuestion }} 分</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 操作按钮 -->
      <div class="form-actions">
        <el-button @click="resetForm">重置</el-button>
        <el-button @click="previewPaper" :loading="loading">预览试卷</el-button>
        <el-button @click="saveAsTemplate">保存为模板</el-button>
        <el-button
          type="primary"
          @click="handleSubmit"
          :loading="loading"
          :disabled="totalQuestions === 0"
        >
          智能生成试卷
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getQuestionTypeText } from '@/utils/tagTypes'

interface Props {
  loading?: boolean
}

interface Emits {
  (e: 'generate', data: any): void
  (e: 'save-template', data: any): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const formRef = ref()
const subjects = ref(['数学', '语文', '英语', '物理', '化学', '生物', '历史', '地理', '政治'])
const gradeLevels = ref(['小学', '初一', '初二', '初三', '高一', '高二', '高三'])
const knowledgePoints = ref<any[]>([])
const commonTags = ref(['重点', '易错', '基础', '提高', '综合'])

// 表单数据
const form = reactive({
  title: '',
  duration: 120,
  subject: '',
  gradeLevel: '',
  questionSources: ['knowledge_base', 'ai_generation'],
  optimizations: ['difficulty_balance', 'knowledge_coverage'],
  strategy: 'balanced',
  questionTypes: [
    {
      type: 'SINGLE_CHOICE',
      count: 10,
      scorePerQuestion: 2,
      difficulty: { easy: 30, medium: 50, hard: 20 },
      knowledgePoints: [],
      tags: []
    },
    {
      type: 'SHORT_ANSWER',
      count: 5,
      scorePerQuestion: 8,
      difficulty: { easy: 20, medium: 60, hard: 20 },
      knowledgePoints: [],
      tags: []
    }
  ]
})

// 表单验证规则
const rules = {
  title: [
    { required: true, message: '请输入试卷标题', trigger: 'blur' }
  ],
  subject: [
    { required: true, message: '请选择学科', trigger: 'change' }
  ],
  gradeLevel: [
    { required: true, message: '请选择年级', trigger: 'change' }
  ]
}

// 计算属性
const totalQuestions = computed(() => {
  return form.questionTypes.reduce((sum, type) => sum + type.count, 0)
})

const totalScore = computed(() => {
  return form.questionTypes.reduce((sum, type) => sum + (type.count * type.scorePerQuestion), 0)
})

const difficultyCoefficient = computed(() => {
  let totalDifficulty = 0
  let totalCount = 0
  
  form.questionTypes.forEach(type => {
    totalDifficulty += type.count * (
      type.difficulty.easy * 0.3 +
      type.difficulty.medium * 0.6 +
      type.difficulty.hard * 1.0
    ) / 100
    totalCount += type.count
  })
  
  return totalCount > 0 ? (totalDifficulty / totalCount).toFixed(2) : '0.00'
})

// 生命周期
onMounted(() => {
  loadKnowledgePoints()
})

// 方法
const loadKnowledgePoints = async () => {
  try {
    // 模拟加载知识点数据
    knowledgePoints.value = [
      { id: 1, title: '一元二次方程' },
      { id: 2, title: '函数性质' },
      { id: 3, title: '三角函数' },
      { id: 4, title: '立体几何' }
    ]
  } catch (error) {
    console.error('加载知识点失败:', error)
  }
}

const addQuestionType = () => {
  form.questionTypes.push({
    type: 'SINGLE_CHOICE',
    count: 5,
    scorePerQuestion: 2,
    difficulty: { easy: 30, medium: 50, hard: 20 },
    knowledgePoints: [],
    tags: []
  })
}

const removeQuestionType = (index: number) => {
  if (form.questionTypes.length > 1) {
    form.questionTypes.splice(index, 1)
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    emit('generate', { ...form })
  } catch (error) {
    console.error('表单验证失败:', error)
  }
}

const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  
  form.questionTypes = [
    {
      type: 'SINGLE_CHOICE',
      count: 10,
      scorePerQuestion: 2,
      difficulty: { easy: 30, medium: 50, hard: 20 },
      knowledgePoints: [],
      tags: []
    }
  ]
}

const previewPaper = () => {
  ElMessage.info('预览功能开发中...')
}

const saveAsTemplate = () => {
  emit('save-template', { ...form })
}
</script>

<style scoped>
.smart-paper-generation {
  padding: 20px;
}

.section-card {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.question-types-config {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.question-type-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  background: #f8f9fa;
}

.type-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.difficulty-sliders {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.difficulty-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.difficulty-item span:first-child {
  min-width: 40px;
}

.difficulty-item span:last-child {
  min-width: 40px;
  text-align: right;
}

.generation-preview {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.preview-charts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.chart-item h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #606266;
}

.chart-placeholder {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.type-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.type-bar span:first-child {
  min-width: 60px;
  font-size: 12px;
}

.type-bar span:last-child {
  min-width: 30px;
  font-size: 12px;
  text-align: right;
}

.bar {
  flex: 1;
  height: 16px;
  background: #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: #409eff;
  transition: width 0.3s;
}

.score-distribution {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.score-item {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 30px;
}
</style>
