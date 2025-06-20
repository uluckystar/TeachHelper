<template>
  <div class="evaluation-overview">
    <div class="page-header">
      <h1>批阅概览</h1>
      <p class="page-description">查看所有考试的批阅状态和统计信息</p>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="24" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="32" color="#409eff"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalExams || 0 }}</div>
              <div class="stat-label">总考试数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="32" color="#67c23a"><EditPen /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalAnswers || 0 }}</div>
              <div class="stat-label">总答案数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="32" color="#e6a23c"><ChatLineRound /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.evaluatedAnswers || 0 }}</div>
              <div class="stat-label">已批阅答案</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="32" color="#f56c6c"><MagicStick /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.aiEvaluatedAnswers || 0 }}</div>
              <div class="stat-label">AI批阅答案</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 考试列表 -->
    <el-card class="exams-card">
      <template #header>
        <div class="card-header">
          <span>考试批阅状态</span>
          <el-button type="primary" icon="Refresh" @click="loadData">刷新</el-button>
        </div>
      </template>

      <el-table :data="exams" v-loading="loading">
        <el-table-column prop="title" label="考试名称" />
        <el-table-column prop="createdBy" label="创建者" width="120" />
        <el-table-column label="题目数" width="100">
          <template #default="{ row }">
            {{ row.questionCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="答案数" width="100">
          <template #default="{ row }">
            {{ row.answerCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="批阅进度" width="150">
          <template #default="{ row }">
            <el-progress 
              :percentage="row.evaluationProgress || 0" 
              :status="row.evaluationProgress === 100 ? 'success' : undefined"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button-group>
              <el-button size="small" @click="goToExamDetail(row.id)">
                详情
              </el-button>
              <el-button size="small" type="primary" @click="goToEvaluation(row.id)">
                批阅
              </el-button>
              <el-button size="small" type="success" @click="goToAIEvaluation(row.id)">
                AI批阅
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Document, EditPen, ChatLineRound, MagicStick } from '@element-plus/icons-vue'
import { examApi } from '@/api/exam'
import { questionApi } from '@/api/question'
import { rubricApi } from '@/api/rubric'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const stats = ref({
  totalExams: 0,
  totalAnswers: 0,
  evaluatedAnswers: 0,
  aiEvaluatedAnswers: 0
})
const exams = ref([])

const loadData = async () => {
  loading.value = true
  try {
    // 加载考试列表
    const examResponse = await examApi.getExams()
    exams.value = examResponse.data || []
    
    // 计算统计信息
    stats.value.totalExams = exams.value.length
    stats.value.totalAnswers = exams.value.reduce((sum: number, exam: any) => sum + (exam.answerCount || 0), 0)
    stats.value.evaluatedAnswers = exams.value.reduce((sum: number, exam: any) => sum + (exam.evaluatedAnswers || 0), 0)
    stats.value.aiEvaluatedAnswers = exams.value.reduce((sum: number, exam: any) => sum + (exam.aiEvaluatedAnswers || 0), 0)
    
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const goToExamDetail = (examId: string) => {
  router.push(`/exams/${examId}`)
}

const goToEvaluation = (examId: string) => {
  router.push(`/exams/${examId}/evaluation`)
}

const goToAIEvaluation = async (examId: string) => {
  try {
    // 首先获取考试的题目列表
    const questions = await questionApi.getQuestionsByExam(parseInt(examId))
    
    if (questions.length === 0) {
      ElMessage.warning('该考试暂无题目，无法进行批阅')
      return
    }
    
    // 检查每个题目的评分标准
    const questionsWithoutRubric = []
    const questionsWithIncompleteRubric = []
    
    for (const question of questions) {
      try {
        const rubrics = await rubricApi.getCriteriaByQuestion(question.id)
        
        if (rubrics.length === 0) {
          questionsWithoutRubric.push(question)
        } else {
          // 检查评分标准是否完善（描述字段可选）
          const incompleteRubrics = rubrics.filter(rubric => 
            !rubric.criterionText || 
            !rubric.points || 
            rubric.points <= 0
          )
          
          if (incompleteRubrics.length > 0) {
            questionsWithIncompleteRubric.push(question)
          }
          
          // 检查总分是否合理
          const totalPoints = rubrics.reduce((sum, rubric) => sum + (rubric.points || 0), 0)
          if (Math.abs(totalPoints - question.maxScore) > 0.1) {
            questionsWithIncompleteRubric.push(question)
          }
        }
      } catch (error) {
        console.error(`Failed to check rubric for question ${question.id}:`, error)
        questionsWithoutRubric.push(question)
      }
    }
    
    // 如果有问题，提示用户
    if (questionsWithoutRubric.length > 0 || questionsWithIncompleteRubric.length > 0) {
      let message = '发现以下评分标准问题：\n'
      
      if (questionsWithoutRubric.length > 0) {
        message += `\n• ${questionsWithoutRubric.length} 个题目未设置评分标准`
      }
      
      if (questionsWithIncompleteRubric.length > 0) {
        message += `\n• ${questionsWithIncompleteRubric.length} 个题目的评分标准不完善`
      }
      
      message += '\n\n建议先完善评分标准再进行批阅。是否继续？'
      
      try {
        const result = await ElMessageBox.confirm(
          message,
          '评分标准检查',
          {
            confirmButtonText: '继续批阅',
            cancelButtonText: '完善标准',
            type: 'warning',
            dangerouslyUseHTMLString: false
          }
        )
        
        if (result === 'confirm') {
          // 评分标准检查通过，跳转到AI批阅页面
          router.push(`/exams/${examId}/ai-evaluation`)
        }
      } catch (error) {
        // 用户点击了"完善标准"按钮，跳转到评分标准管理页面
        router.push({
          path: `/exams/${examId}/rubric-management`,
          query: { 
            from: 'evaluation-overview',
            action: 'ai-evaluation'
          }
        })
      }
      return
    }
    
    // 评分标准检查通过，跳转到AI批阅页面
    router.push(`/exams/${examId}/ai-evaluation`)
    
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('评分标准检查失败:', error)
      ElMessage.error('评分标准检查失败，无法开始批阅')
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.evaluation-overview {
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

.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-content {
  display: flex;
  align-items: center;
  padding: 16px;
}

.stat-icon {
  margin-right: 16px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.exams-card {
  background: white;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
