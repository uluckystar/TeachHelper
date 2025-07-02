<template>
  <div class="batch-ai-evaluation">
    <div class="page-header">
      <h1>批量AI批阅</h1>
      <p class="page-description">选择考试和批阅标准，进行批量AI智能批阅</p>
    </div>

    <!-- 步骤指示器 -->
    <el-steps :active="currentStep" finish-status="success" class="steps">
      <el-step title="选择考试" description="选择要批阅的考试" />
      <el-step title="配置参数" description="设置批阅参数和标准" />
      <el-step title="开始批阅" description="执行批量批阅任务" />
      <el-step title="查看结果" description="查看批阅结果" />
    </el-steps>

    <!-- 步骤内容 -->
    <el-card class="step-content">
      <!-- 步骤1: 选择考试 -->
      <div v-if="currentStep === 0" class="step-panel">
        <h3>选择要批阅的考试和题目</h3>
        
        <!-- 考试选择 -->
        <div class="exam-selection">
          <h4>1. 选择考试</h4>
          <el-table 
            :data="exams" 
            v-loading="loading"
            @selection-change="handleExamSelection"
            row-key="id"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column prop="title" label="考试名称" />
            <el-table-column prop="description" label="描述" />
            <el-table-column prop="createdBy" label="创建者" width="120" />
            <el-table-column label="题目数" width="100">
              <template #default="{ row }">
                {{ row.totalQuestions || row.questionCount || 0 }}
              </template>
            </el-table-column>
            <el-table-column label="学生答案数" width="120">
              <template #default="{ row }">
                {{ row.totalAnswers || row.answerCount || 0 }}
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 题目选择 -->
        <div v-if="selectedExams.length > 0" class="question-selection">
          <h4>2. 选择要批阅的题目 (可选)</h4>
          <el-alert 
            title="提示" 
            type="info" 
            :closable="false"
            style="margin-bottom: 16px;"
          >
            如果不选择具体题目，将对所有主观题进行批阅。您也可以选择特定的题目进行批阅。
          </el-alert>
          
          <div v-for="exam in selectedExams" :key="exam.id" class="exam-questions">
            <h5>{{ exam.title }} - 题目列表</h5>
            <el-table 
              :data="examQuestions[exam.id] || []"
              v-loading="loadingQuestions[exam.id]"
              @selection-change="(selection) => handleQuestionSelection(exam.id, selection)"
              row-key="id"
            >
              <el-table-column type="selection" width="55" />
              <el-table-column prop="questionText" label="题目内容" min-width="200">
                <template #default="{ row }">
                  <div class="question-preview">
                    {{ row.questionText?.substring(0, 100) }}{{ row.questionText?.length > 100 ? '...' : '' }}
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="questionType" label="题目类型" width="120">
                <template #default="{ row }">
                  <el-tag :type="getQuestionTypeTag(row.questionType)">
                    {{ getQuestionTypeText(row.questionType) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="maxScore" label="满分" width="80" />
              <el-table-column label="答案数" width="100">
                <template #default="{ row }">
                  {{ row.totalAnswers || 0 }}
                </template>
              </el-table-column>
              <el-table-column label="已批阅" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.evaluatedAnswers > 0 ? 'success' : 'info'">
                    {{ row.evaluatedAnswers || 0 }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </div>

      <!-- 步骤2: 配置参数 -->
      <div v-if="currentStep === 1" class="step-panel">
        <h3>配置批阅参数</h3>
        <el-form :model="evaluationConfig" label-width="140px" class="config-form">
          <el-form-item label="AI配置">
            <el-select v-model="evaluationConfig.aiConfigId" placeholder="选择AI配置">
              <el-option 
                v-for="config in aiConfigs" 
                :key="config.id" 
                :label="`${config.providerDisplayName} - ${config.modelName || '默认模型'}`"
                :value="config.id"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="批阅标准">
            <el-select v-model="evaluationConfig.rubricId" placeholder="选择批阅标准（可选）">
              <el-option label="不使用批阅标准" value="" />
              <el-option 
                v-for="rubric in rubrics" 
                :key="rubric.id" 
                :label="rubric.name"
                :value="rubric.id"
              />
            </el-select>
            <div class="form-tip">
              <strong>批阅标准说明：</strong><br>
              • <strong>不使用</strong>：AI将基于题目内容自动生成评分标准进行批阅<br>
              • <strong>选择标准</strong>：使用预设的系统级评分标准，会覆盖每道题目的具体评分标准<br>
              • <strong>建议</strong>：如果每道题目都已设置了详细的评分标准，可选择"不使用"；如果需要统一的评分风格，可选择系统标准
            </div>
          </el-form-item>

          <el-form-item label="评分模式">
            <el-radio-group v-model="evaluationConfig.evaluationStyle">
              <el-radio value="NORMAL">普通</el-radio>
              <el-radio value="LENIENT">宽松</el-radio>
              <el-radio value="STRICT">严格</el-radio>
            </el-radio-group>
            <div class="form-tip">
              <strong>评分模式说明：</strong><br>
              • <strong>普通</strong>：平衡的评分标准，综合考虑准确性和完整性<br>
              • <strong>宽松</strong>：更加宽容的评分，鼓励学生的积极思考，对部分正确答案酌情给分<br>
              • <strong>严格</strong>：更高的评分要求，对细节和准确性要求更严格
            </div>
          </el-form-item>

          <el-form-item label="批阅类型">
            <el-radio-group v-model="evaluationConfig.evaluationType">
              <el-radio value="FULL_EVALUATION">完整批阅（包含分数和详细反馈）</el-radio>
              <el-radio value="QUICK_EVALUATION">快速批阅（仅打分）</el-radio>
              <el-radio value="RUBRIC_BASED">基于标准批阅</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="批量大小">
            <el-input-number 
              v-model="evaluationConfig.batchSize" 
              :min="1" 
              :max="50" 
              placeholder="每批处理的答案数量"
            />
            <div class="form-tip">建议设置为10-20，避免API限制</div>
          </el-form-item>

          <el-form-item label="并发数">
            <el-input-number 
              v-model="evaluationConfig.concurrency" 
              :min="1" 
              :max="5" 
              placeholder="同时处理的批次数"
            />
            <div class="form-tip">并发数过高可能导致API限制</div>
          </el-form-item>

          <el-form-item label="任务描述">
            <el-input 
              v-model="evaluationConfig.description" 
              type="textarea" 
              :rows="3"
              placeholder="描述此次批阅任务的目的和要求"
            />
          </el-form-item>
        </el-form>
      </div>

      <!-- 步骤3: 开始批阅 -->
      <div v-if="currentStep === 2" class="step-panel">
        <h3>确认批阅信息</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="选中考试数">{{ selectedExams.length }}</el-descriptions-item>
          <el-descriptions-item label="AI配置">
            {{ getAIConfigName(evaluationConfig.aiConfigId) }}
          </el-descriptions-item>
          <el-descriptions-item label="批阅标准">
            {{ getRubricName(evaluationConfig.rubricId) || '不使用' }}
          </el-descriptions-item>
          <el-descriptions-item label="批阅类型">
            {{ getEvaluationTypeName(evaluationConfig.evaluationType) }}
          </el-descriptions-item>
          <el-descriptions-item label="批量大小">{{ evaluationConfig.batchSize }}</el-descriptions-item>
          <el-descriptions-item label="并发数">{{ evaluationConfig.concurrency }}</el-descriptions-item>
        </el-descriptions>

        <div class="selected-exams">
          <h4>选中的考试:</h4>
          <el-tag v-for="exam in selectedExams" :key="exam.id" style="margin-right: 8px; margin-bottom: 8px;">
            {{ exam.title }}
          </el-tag>
        </div>

        <el-alert 
          title="注意事项" 
          type="warning" 
          :closable="false"
          style="margin-top: 16px;"
        >
          <ul>
            <li>批量批阅将消耗AI服务配额，请确认AI配置有足够额度</li>
            <li>批阅过程中请不要关闭浏览器，可在任务监控页面查看进度</li>
            <li>大量答案的批阅可能需要较长时间，请耐心等待</li>
          </ul>
        </el-alert>
      </div>

      <!-- 步骤4: 查看结果 -->
      <div v-if="currentStep === 3" class="step-panel">
        <div class="result-panel">
          <el-result
            icon="success"
            title="批阅任务已创建"
            sub-title="批量AI批阅任务已成功创建并开始执行"
          >
            <template #extra>
              <el-button type="primary" @click="goToTaskMonitor">查看任务进度</el-button>
              <el-button @click="resetWizard">重新开始</el-button>
            </template>
          </el-result>

          <el-card v-if="createdTask" class="task-info">
            <template #header>任务信息</template>
            <el-descriptions :column="2">
              <el-descriptions-item label="任务ID">{{ createdTask.id }}</el-descriptions-item>
              <el-descriptions-item label="任务状态">
                <el-tag :type="getStatusTag(createdTask.status)">
                  {{ getStatusText(createdTask.status) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">
                {{ formatDate(createdTask.createdAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="预计完成时间">
                {{ createdTask.estimatedCompletionTime }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </div>
      </div>
    </el-card>

    <!-- 操作按钮 -->
    <div class="actions">
      <el-button v-if="currentStep > 0" @click="prevStep">上一步</el-button>
      <el-button 
        v-if="currentStep < 3" 
        type="primary" 
        @click="nextStep"
        :disabled="!canProceedToNext"
        :loading="processing"
      >
        {{ currentStep === 2 ? '开始批阅' : '下一步' }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { examApi } from '@/api/exam'
import { evaluationApi } from '@/api/evaluation'
import { userAIConfigApi } from '@/api/userAIConfig'
import { rubricApi } from '@/api/rubric'
import { questionApi } from '@/api/question'
import type { Exam, UserAIConfigResponse, Rubric } from '@/types/api'

const router = useRouter()

// 状态管理
const currentStep = ref(0)
const loading = ref(false)
const processing = ref(false)

// 数据
const exams = ref<Exam[]>([])
const selectedExams = ref<Exam[]>([])
const examQuestions = ref<Record<number, any[]>>({})
const loadingQuestions = ref<Record<number, boolean>>({})
const selectedQuestions = ref<Record<number, any[]>>({})
const aiConfigs = ref<UserAIConfigResponse[]>([])
const rubrics = ref<Rubric[]>([])
const createdTask = ref<any>(null)

// 批阅配置
const evaluationConfig = ref({
  aiConfigId: '',
  rubricId: '',
  evaluationType: 'FULL_EVALUATION',
  batchSize: 10,
  concurrency: 2,
  description: '',
  evaluationStyle: 'NORMAL'
})

// 计算属性
const canProceedToNext = computed(() => {
  switch (currentStep.value) {
    case 0:
      return selectedExams.value.length > 0
    case 1:
      return evaluationConfig.value.aiConfigId !== ''
    case 2:
      return true
    default:
      return false
  }
})

// 方法
const loadExams = async () => {
  loading.value = true
  try {
    const response = await examApi.getExams()
    // examApi.getExams() 已经返回数据本身，不需要再访问 .data 属性
    exams.value = response || []
  } catch (error) {
    console.error('加载考试列表失败:', error)
    ElMessage.error('加载考试列表失败')
  } finally {
    loading.value = false
  }
}

const loadAIConfigs = async () => {
  try {
    const configs = await userAIConfigApi.getConfigs()
    aiConfigs.value = configs.filter(config => config.isActive)
  } catch (error) {
    console.error('加载AI配置失败:', error)
    ElMessage.error('加载AI配置失败')
  }
}

const loadRubrics = async () => {
  try {
    const response = await rubricApi.getAllRubrics()
    rubrics.value = response || []
  } catch (error) {
    console.error('加载批阅标准失败:', error)
  }
}

const handleExamSelection = (selection: Exam[]) => {
  selectedExams.value = selection
  // 当选择考试时，加载考试的题目
  loadExamQuestions()
}

const handleQuestionSelection = (examId: number, selection: any[]) => {
  selectedQuestions.value[examId] = selection
}

const loadExamQuestions = async () => {
  for (const exam of selectedExams.value) {
    try {
      loadingQuestions.value[exam.id] = true
      const questions = await questionApi.getQuestionsByExam(exam.id)
      examQuestions.value[exam.id] = questions || []
    } catch (error) {
      console.error(`加载考试 ${exam.id} 的题目失败:`, error)
      examQuestions.value[exam.id] = []
    } finally {
      loadingQuestions.value[exam.id] = false
    }
  }
}

const getQuestionTypeTag = (type: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    'MULTIPLE_CHOICE': 'primary',
    'TRUE_FALSE': 'success', 
    'SHORT_ANSWER': 'warning',
    'ESSAY': 'danger',
    'CODING': 'info'
  }
  return map[type] || 'info'
}

const getQuestionTypeText = (type: string) => {
  const map: Record<string, string> = {
    'MULTIPLE_CHOICE': '选择题',
    'TRUE_FALSE': '判断题',
    'SHORT_ANSWER': '简答题',
    'ESSAY': '论述题',
    'CODING': '编程题'
  }
  return map[type] || type
}

const nextStep = async () => {
  if (currentStep.value === 2) {
    // 开始批阅
    await startEvaluation()
  } else {
    currentStep.value++
    if (currentStep.value === 1) {
      await loadAIConfigs()
      await loadRubrics()
    }
  }
}

const prevStep = () => {
  currentStep.value--
}

const startEvaluation = async () => {
  processing.value = true
  try {
    // 首先检查选中考试的评分标准
    const examsWithIssues = []
    
    for (const exam of selectedExams.value) {
      try {
        const questions = await questionApi.getQuestionsByExam(exam.id)
        
        if (questions.length === 0) {
          examsWithIssues.push({
            exam,
            issue: '无题目'
          })
          continue
        }
        
        let hasRubricIssues = false
        
        for (const question of questions) {
          try {
            const rubrics = await rubricApi.getCriteriaByQuestion(question.id)
            
            if (rubrics.length === 0) {
              hasRubricIssues = true
              break
            }
            
            // 检查评分标准是否完善（描述字段可选）
            const incompleteRubrics = rubrics.filter(rubric => 
              !rubric.criterionText || 
              !rubric.points || 
              rubric.points <= 0
            )
            
            if (incompleteRubrics.length > 0) {
              hasRubricIssues = true
              break
            }
            
            // 检查总分是否合理
            const totalPoints = rubrics.reduce((sum, rubric) => sum + (rubric.points || 0), 0)
            if (Math.abs(totalPoints - question.maxScore) > 0.1) {
              hasRubricIssues = true
              break
            }
          } catch (error) {
            console.error(`Failed to check rubric for question ${question.id}:`, error)
            hasRubricIssues = true
            break
          }
        }
        
        if (hasRubricIssues) {
          examsWithIssues.push({
            exam,
            issue: '评分标准不完善'
          })
        }
        
      } catch (error) {
        console.error(`Failed to check exam ${exam.id}:`, error)
        examsWithIssues.push({
          exam,
          issue: '检查失败'
        })
      }
    }
    
    // 如果有评分标准问题，提示用户
    if (examsWithIssues.length > 0) {
      let message = '发现以下考试存在问题：\n\n'
      
      examsWithIssues.forEach(({ exam, issue }) => {
        message += `• ${exam.title}: ${issue}\n`
      })
      
      message += '\n建议先完善评分标准再进行批阅。是否继续？'
      
      try {
        const result = await ElMessageBox.confirm(
          message,
          '评分标准检查',
          {
            confirmButtonText: '继续批阅',
            cancelButtonText: '管理标准',
            type: 'warning',
            dangerouslyUseHTMLString: false
          }
        )
        
        if (result === 'confirm') {
          // 用户选择继续批阅，跳过评分标准问题
        }
      } catch (error) {
        // 用户点击了"管理标准"按钮
        if (examsWithIssues.length === 1) {
          router.push({
            path: `/exams/${examsWithIssues[0].exam.id}/rubric-management`,
            query: { 
              from: 'batch-evaluation',
              action: 'ai-evaluation'
            }
          })
        } else {
          // 多个考试有问题，提示用户逐个处理
          ElMessage.info('请先到考试详情页面完善各考试的评分标准，然后再返回进行批量批阅')
        }
        processing.value = false
        return
      }
    }
    
    // 评分标准检查通过或用户选择继续，准备创建批阅任务
    const taskData = {
      examIds: selectedExams.value.map(exam => exam.id),
      aiConfigId: evaluationConfig.value.aiConfigId,
      rubricId: evaluationConfig.value.rubricId || undefined,
      evaluationType: evaluationConfig.value.evaluationType,
      evaluationStyle: evaluationConfig.value.evaluationStyle,
      batchSize: evaluationConfig.value.batchSize,
      concurrency: evaluationConfig.value.concurrency,
      description: evaluationConfig.value.description,
      evaluateAll: false // 默认只评估未评估的答案，如果没有未评估答案会提示用户
    }

    // 先进行预检查
    let preCheckResult
    try {
      preCheckResult = await evaluationApi.precheckBatchTask(taskData)
      console.log('预检查结果:', preCheckResult)
    } catch (error) {
      console.error('预检查失败:', error)
      ElMessage.error('预检查失败，请稍后重试')
      processing.value = false
      return
    }

    // 根据预检查结果决定是否继续
    if (!preCheckResult.canProceed) {
      // 不能继续，显示友好提示
      const { warningLevel, message, suggestion } = preCheckResult
      
      if (warningLevel === 'ERROR') {
        // 没有找到任何答案，询问用户是否要尝试获取所有答案（包括已评估的）
        try {
          await ElMessageBox.confirm(
            `${message}\n\n${suggestion}\n\n可能所有答案都已经被评估过了。是否要尝试获取所有答案并重新评估？`,
            '未找到答案',
            {
              confirmButtonText: '获取所有答案并重新评估',
              cancelButtonText: '取消',
              type: 'warning',
              dangerouslyUseHTMLString: false
            }
          )
          // 用户选择获取所有答案，修改配置
          ;(taskData as any).evaluateAll = true
          
          // 重新进行预检查
          try {
            preCheckResult = await evaluationApi.precheckBatchTask(taskData)
            console.log('重新预检查结果:', preCheckResult)
            
            if (!preCheckResult.canProceed) {
              ElMessage.error(`仍然无法找到答案：${preCheckResult.message}`)
              processing.value = false
              return
            }
            
            ElMessage.success(`找到 ${preCheckResult.totalAnswers} 个答案可以重新评估`)
          } catch (error) {
            ElMessage.error('重新预检查失败，请稍后重试')
            processing.value = false
            return
          }
        } catch (error) {
          // 用户取消
          processing.value = false
          return
        }
      } else {
        // 其他错误情况，直接显示错误信息
        ElMessage.error(`${message}\n\n${suggestion}`)
        processing.value = false
        return
      }
    } else {
      // 可以继续，根据不同情况给出提示
      const { warningLevel, message, suggestion, evaluatedAnswers, unevaluatedAnswers, totalAnswers } = preCheckResult
      
      if (warningLevel === 'WARNING' && unevaluatedAnswers === 0 && evaluatedAnswers > 0) {
        // 所有答案都已评估，提供更详细的选项
        try {
          const result = await ElMessageBox({
            title: '评估选项',
            message: `
              <div style="padding: 16px 0;">
                <p style="margin: 0 0 16px 0; font-weight: 600; color: #409EFF;">
                  检查结果：发现 ${totalAnswers} 个答案，全部已评估完成
                </p>
                <p style="margin: 0 0 16px 0; color: #606266;">
                  ${suggestion}
                </p>
                <div style="background: #f5f7fa; padding: 12px; border-radius: 4px; margin: 16px 0;">
                  <p style="margin: 0 0 8px 0; font-weight: 600;">请选择处理方式：</p>
                  <ul style="margin: 0; padding-left: 20px; color: #606266;">
                    <li>重新评估：将覆盖所有现有评分，重新进行AI评估</li>
                    <li>取消：不进行任何操作，保持现有评分</li>
                  </ul>
                </div>
              </div>
            `,
            confirmButtonText: '重新评估所有答案',
            cancelButtonText: '取消',
            type: 'warning',
            dangerouslyUseHTMLString: true,
            customClass: 'evaluation-choice-dialog'
          })
          
          // 用户选择重新评估，修改配置
          ;(taskData as any).evaluateAll = true
          ElMessage.info(`将重新评估 ${totalAnswers} 个答案`)
        } catch (error) {
          // 用户取消
          processing.value = false
          return
        }
      } else if (warningLevel === 'WARNING' && unevaluatedAnswers > 0 && evaluatedAnswers > 0) {
        // 部分已评估，提供补充评估或完全重新评估的选项
        try {
          const result = await ElMessageBox({
            title: '评估选项',
            message: `
              <div style="padding: 16px 0;">
                <p style="margin: 0 0 16px 0; font-weight: 600; color: #409EFF;">
                  检查结果：发现 ${totalAnswers} 个答案
                </p>
                <div style="display: flex; gap: 16px; margin: 16px 0;">
                  <div style="flex: 1; background: #e7f5ff; padding: 12px; border-radius: 4px; border-left: 4px solid #409EFF;">
                    <div style="font-weight: 600; color: #409EFF; margin-bottom: 4px;">
                      ✓ 已评估：${evaluatedAnswers} 个
                    </div>
                    <div style="font-size: 12px; color: #666;">
                      这些答案已有评分和反馈
                    </div>
                  </div>
                  <div style="flex: 1; background: #fff7e6; padding: 12px; border-radius: 4px; border-left: 4px solid #E6A23C;">
                    <div style="font-weight: 600; color: #E6A23C; margin-bottom: 4px;">
                      ⏳ 待评估：${unevaluatedAnswers} 个
                    </div>
                    <div style="font-size: 12px; color: #666;">
                      这些答案尚未评分
                    </div>
                  </div>
                </div>
                <div style="background: #f5f7fa; padding: 12px; border-radius: 4px; margin: 16px 0;">
                  <p style="margin: 0 0 8px 0; font-weight: 600;">请选择处理方式：</p>
                  <ul style="margin: 0; padding-left: 20px; color: #606266;">
                    <li><strong>补充评估</strong>：只评估未评估的 ${unevaluatedAnswers} 个答案，保留已有评分</li>
                    <li><strong>完全重新评估</strong>：重新评估所有 ${totalAnswers} 个答案，覆盖现有评分</li>
                  </ul>
                </div>
              </div>
            `,
            confirmButtonText: '补充评估',
            cancelButtonText: '取消',
            distinguishCancelAndClose: true,
            showClose: false,
            customClass: 'evaluation-choice-dialog',
            type: 'info',
            dangerouslyUseHTMLString: true,
            beforeClose: (action, instance, done) => {
              if (action === 'confirm') {
                // 补充评估（默认行为）
                ;(taskData as any).evaluateAll = false
                ElMessage.info(`将评估 ${unevaluatedAnswers} 个未评估的答案`)
                done()
              } else if (action === 'cancel') {
                // 用户点击取消
                done()
              }
            }
          })
          
          // 添加额外按钮用于完全重新评估
          const dialogElement = document.querySelector('.evaluation-choice-dialog .el-message-box__btns')
          if (dialogElement) {
            const reEvaluateButton = document.createElement('button')
            reEvaluateButton.className = 'el-button el-button--warning'
            reEvaluateButton.textContent = '完全重新评估'
            reEvaluateButton.style.marginRight = '10px'
            reEvaluateButton.onclick = () => {
              ;(taskData as any).evaluateAll = true
              ElMessage.info(`将重新评估所有 ${totalAnswers} 个答案`)
              // 关闭对话框
              const closeButton = dialogElement.querySelector('.el-button--primary') as HTMLElement
              closeButton?.click()
            }
            dialogElement.insertBefore(reEvaluateButton, dialogElement.firstChild)
          }
          
        } catch (error) {
          // 用户取消
          processing.value = false
          return
        }
      } else if (evaluatedAnswers > 0) {
        // 部分已评估，只显示信息给用户
        ElMessage.info(`找到 ${totalAnswers} 个答案，其中 ${evaluatedAnswers} 个已评估，${unevaluatedAnswers} 个待评估。将评估未评估的答案。`)
      } else {
        // 全部未评估，正常情况
        ElMessage.success(`找到 ${unevaluatedAnswers} 个待评估答案`)
      }
    }

    // 创建批阅任务
    const response = await evaluationApi.createBatchTask(taskData)
    
    // 检查响应格式
    if (response.success) {
      // 新的响应格式
      createdTask.value = {
        id: response.taskId,
        taskId: response.taskId,
        status: 'PENDING',
        createdAt: new Date().toISOString(),
        description: evaluationConfig.value.description || '批量AI批阅任务',
        estimatedCompletionTime: '预计几分钟内完成',
        totalAnswers: response.totalAnswers,
        evaluatedAnswers: response.evaluatedAnswers,
        unevaluatedAnswers: response.unevaluatedAnswers
      }
      
      ElMessage.success(response.message)
    } else {
      // 旧的响应格式或错误
      ElMessage.error(response.message || '创建任务失败')
      processing.value = false
      return
    }
    
    currentStep.value = 3
    ElMessage.success('批量批阅任务创建成功')
    
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('创建批阅任务失败:', error)
      ElMessage.error('创建批阅任务失败')
    }
  } finally {
    processing.value = false
  }
}

const goToTaskMonitor = () => {
  router.push('/task-monitor')
}

const resetWizard = () => {
  currentStep.value = 0
  selectedExams.value = []
  evaluationConfig.value = {
    aiConfigId: '',
    rubricId: '',
    evaluationType: 'FULL_EVALUATION',
    batchSize: 10,
    concurrency: 2,
    description: '',
    evaluationStyle: 'NORMAL'
  }
  createdTask.value = null
}

// 辅助方法
const formatDate = (dateString: string) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

const getAIConfigName = (configId: string) => {
  const config = aiConfigs.value.find(c => c.id.toString() === configId)
  if (!config) return '-'
  const displayName = config.providerDisplayName || config.provider
  const modelName = config.modelName || '默认模型'
  return `${displayName} - ${modelName}`
}

const getRubricName = (rubricId: string) => {
  const rubric = rubrics.value.find(r => r.id.toString() === rubricId)
  return rubric?.name
}

const getEvaluationTypeName = (type: string) => {
  const map: Record<string, string> = {
    'FULL_EVALUATION': '完整批阅',
    'QUICK_EVALUATION': '快速批阅',
    'RUBRIC_BASED': '基于标准批阅'
  }
  return map[type] || type
}

const getStatusTag = (status: string): 'success' | 'warning' | 'danger' | 'info' => {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    'PENDING': 'info',
    'RUNNING': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger',
    'CANCELLED': 'info'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'PENDING': '待处理',
    'RUNNING': '运行中',
    'COMPLETED': '已完成',
    'FAILED': '失败',
    'CANCELLED': '已取消'
  }
  return map[status] || status
}

onMounted(() => {
  loadExams()
})
</script>

<style scoped>
.batch-ai-evaluation {
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

.steps {
  margin-bottom: 24px;
}

.step-content {
  min-height: 400px;
  margin-bottom: 24px;
}

.step-panel h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  font-weight: 600;
}

.config-form {
  max-width: 600px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.selected-exams {
  margin-top: 16px;
}

.selected-exams h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
}

.result-panel {
  text-align: center;
}

.task-info {
  margin-top: 24px;
  text-align: left;
}

.actions {
  text-align: center;
  padding: 16px 0;
}

.actions .el-button {
  margin: 0 8px;
}

/* 评估选择对话框样式 */
:global(.evaluation-choice-dialog) {
  width: 580px !important;
}

:global(.evaluation-choice-dialog .el-message-box__message) {
  line-height: 1.5;
}

:global(.evaluation-choice-dialog .el-message-box__btns) {
  padding: 20px 24px 24px;
  text-align: center;
}

:global(.evaluation-choice-dialog .el-button) {
  margin: 0 6px;
  min-width: 120px;
}

/* 题目选择区域样式 */
.exam-selection,
.question-selection {
  margin-bottom: 24px;
}

.exam-selection h4,
.question-selection h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.exam-questions {
  margin-bottom: 16px;
}

.exam-questions h5 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}

.question-preview {
  word-break: break-all;
  line-height: 1.4;
}
</style>