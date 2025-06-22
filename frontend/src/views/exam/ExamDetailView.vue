<template>
  <div class="exam-detail">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-row :gutter="24" align="middle">
        <el-col :span="16">
          <el-breadcrumb>
            <el-breadcrumb-item :to="{ path: '/exams' }">考试列表</el-breadcrumb-item>
            <el-breadcrumb-item>{{ exam?.title || '考试详情' }}</el-breadcrumb-item>
          </el-breadcrumb>
          <h1 v-if="exam" class="page-title">{{ exam.title }}</h1>
          <el-skeleton v-else animated>
            <template #template>
              <el-skeleton-item variant="h1" style="width: 400px" />
            </template>
          </el-skeleton>
        </el-col>
        <el-col :span="8" class="header-actions-col" v-if="authStore.isTeacher || authStore.isAdmin">
          <div class="header-actions">
            <el-button 
              type="primary" 
              icon="DocumentAdd"
              @click="navigateToAddQuestion"
              :disabled="loading"
              size="default"
            >
              添加题目
            </el-button>
            <el-button 
              type="success" 
              icon="MagicStick"
              @click="navigateToAIEvaluation"
              :disabled="loading"
              size="default"
            >
              批阅
            </el-button>
            <el-button 
              type="info" 
              icon="Setting"
              @click="navigateToRubricManagement"
              :disabled="loading"
              size="default"
            >
              评分标准
            </el-button>
            <el-dropdown @command="handleDropdownCommand">
              <el-button type="default" icon="More" size="default">
                更多操作<el-icon class="el-icon--right"><arrow-down /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="publish" icon="VideoPlay" v-if="exam?.status === 'DRAFT'">发布考试</el-dropdown-item>
                  <el-dropdown-item command="unpublish" icon="VideoPause" v-if="exam?.status === 'PUBLISHED'">撤销发布</el-dropdown-item>
                  <el-dropdown-item command="end" icon="VideoOff" v-if="exam?.status === 'PUBLISHED' || exam?.status === 'IN_PROGRESS'">结束考试</el-dropdown-item>
                  <el-dropdown-item command="classrooms" icon="School" divided>调整班级</el-dropdown-item>
                  <el-dropdown-item command="edit" icon="Edit">编辑考试</el-dropdown-item>
                  <el-dropdown-item command="export" icon="Download">导出考试</el-dropdown-item>
                  <el-dropdown-item command="delete" icon="Delete" divided>删除考试</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 主要内容区域 -->
    <el-row :gutter="24" v-if="!loading">
      <!-- 左侧：考试信息 -->
      <el-col :span="16">
        <!-- 考试基本信息 -->
        <el-card class="info-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>考试信息</span>
            </div>
          </template>
          
          <el-descriptions :column="2" border>
            <el-descriptions-item label="考试标题">
              {{ exam?.title }}
            </el-descriptions-item>
            <el-descriptions-item label="考试状态">
              <el-tag :type="getStatusTagType(exam?.status)" effect="dark">
                {{ getStatusDisplayText(exam?.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="考试描述" :span="2">
              {{ exam?.description || '暂无描述' }}
            </el-descriptions-item>
            <el-descriptions-item label="开始时间">
              {{ exam?.startTime ? formatDate(exam.startTime) : '未设置' }}
            </el-descriptions-item>
            <el-descriptions-item label="结束时间">
              {{ exam?.endTime ? formatDate(exam.endTime) : '未设置' }}
            </el-descriptions-item>
            <el-descriptions-item label="考试时长">
              {{ exam?.duration ? `${exam.duration} 分钟` : '未设置' }}
            </el-descriptions-item>
            <el-descriptions-item label="创建者">
              {{ exam?.createdBy }}
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              {{ formatDate(exam?.createdAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="最后更新">
              {{ formatDate(exam?.updatedAt) }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 考试班级信息 -->
        <el-card class="classrooms-card" shadow="never" v-if="examClassrooms && examClassrooms.length > 0">
          <template #header>
            <div class="card-header">
              <span>考试班级</span>
              <el-button size="small" icon="School" @click="handleDropdownCommand('classrooms')">管理班级</el-button>
            </div>
          </template>
          
          <div class="classrooms-list">
            <div 
              v-for="classroom in examClassrooms" 
              :key="classroom.id"
              class="classroom-item"
            >
              <div class="classroom-info">
                <div class="classroom-header">
                  <h4>{{ classroom.name }}</h4>
                  <el-tag size="small">{{ classroom.classCode }}</el-tag>
                </div>
                <div class="classroom-stats">
                  <span>学生数: {{ classroom.studentCount || 0 }}</span>
                  <el-button 
                    size="small" 
                    type="primary" 
                    link
                    @click="showClassroomMembers(classroom)"
                  >
                    查看成员
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 题目列表 -->
        <el-card class="questions-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>题目列表 ({{ questions.length }})</span>
              <el-button 
                type="primary" 
                size="small" 
                icon="Plus"
                @click="navigateToAddQuestion"
              >
                添加题目
              </el-button>
            </div>
          </template>
          
          <div v-if="questions.length === 0" class="empty-state">
            <el-empty description="暂无题目">
              <el-button type="primary" @click="navigateToAddQuestion">添加第一个题目</el-button>
            </el-empty>
          </div>
          
          <div v-else class="questions-list">
            <div 
              v-for="(question, index) in questions" 
              :key="question.id"
              class="question-item"
            >
              <div class="question-header">
                <el-tag 
                  :type="getQuestionTypeTag(question.questionType)"
                  size="small"
                >
                  {{ getQuestionTypeText(question.questionType) }}
                </el-tag>
                <span class="question-title">题目 {{ index + 1 }}: {{ question.title }}</span>
                <div class="question-actions">
                  <el-button-group>
                    <el-button 
                      size="small" 
                      icon="View"
                      @click="viewQuestion(question.id)"
                    >
                      查看
                    </el-button>
                    <el-button 
                      size="small" 
                      icon="Edit"
                      @click="editQuestion(question.id)"
                    >
                      编辑
                    </el-button>
                    <el-button 
                      size="small" 
                      type="success"
                      icon="MagicStick"
                      @click="evaluateQuestion(question.id)"
                    >
                      批阅本题
                    </el-button>
                    <el-button 
                      size="small" 
                      type="warning"
                      icon="Setting"
                      @click="manageQuestionRubric(question.id)"
                    >
                      评分标准
                    </el-button>
                    <el-button 
                      size="small" 
                      type="danger" 
                      icon="Delete"
                      @click="deleteQuestion(question.id)"
                    >
                      删除
                    </el-button>
                  </el-button-group>
                </div>
              </div>
              <div class="question-meta">
                <span>满分: {{ question.maxScore }} 分</span>
                <span>答案数: {{ getAnswerCount(question.id) }}</span>
                <span>已评估: {{ getEvaluatedCount(question.id) }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：统计信息 - 仅教师/管理员可见 -->
      <el-col :span="8" v-if="authStore.isTeacher || authStore.isAdmin">
        <!-- 考试统计 -->
        <el-card class="stats-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>考试统计</span>
              <el-button 
                size="small" 
                icon="Refresh"
                @click="refreshStatistics"
                :loading="statisticsLoading"
              >
                刷新
              </el-button>
            </div>
          </template>
          
          <div v-if="statistics" class="statistics">
            <div class="stat-item">
              <div class="stat-value">{{ statistics.totalQuestions }}</div>
              <div class="stat-label">题目总数</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ statistics.totalAnswers }}</div>
              <div class="stat-label">答案总数</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ statistics.evaluatedAnswers }}</div>
              <div class="stat-label">已评估答案</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ statistics.totalStudents }}</div>
              <div class="stat-label">参与学生</div>
            </div>
            
            <!-- 评估进度 -->
            <div class="progress-section">
              <div class="progress-header">
                <span>评估进度</span>
                <span class="progress-text">{{ statistics.evaluationProgress.toFixed(1) }}%</span>
              </div>
              <el-progress 
                :percentage="statistics.evaluationProgress" 
                :status="statistics.evaluationProgress === 100 ? 'success' : undefined"
              />
            </div>
            
            <!-- 分数统计 -->
            <div v-if="statistics.averageScore !== null" class="score-stats">
              <el-divider content-position="left">分数统计</el-divider>
              <div class="score-item">
                <span>平均分:</span>
                <span class="score-value">{{ statistics.averageScore?.toFixed(2) || 'N/A' }}</span>
              </div>
              <div class="score-item" v-if="statistics.maxScore">
                <span>最高分:</span>
                <span class="score-value">{{ statistics.maxScore }}</span>
              </div>
              <div class="score-item" v-if="statistics.minScore">
                <span>最低分:</span>
                <span class="score-value">{{ statistics.minScore }}</span>
              </div>
            </div>
          </div>
          
          <el-skeleton v-else animated :rows="4" />
        </el-card>

        <!-- 快捷操作 -->
        <el-card class="actions-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>快捷操作</span>
            </div>
          </template>
          
          <div class="quick-actions">
            <el-button 
              type="success" 
              icon="View"
              @click="viewAnswers"
              block
            >
              查看学生答案
            </el-button>
            <el-button 
              type="warning" 
              icon="Upload"
              @click="showImportDialog"
              block
            >
              导入学生答案
            </el-button>
            <el-button 
              type="info" 
              icon="Download"
              @click="exportExamData"
              block
            >
              导出考试数据
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 加载状态 -->
    <div v-else class="loading-container">
      <el-skeleton animated>
        <template #template>
          <el-skeleton-item variant="h1" style="width: 40%" />
          <el-skeleton-item variant="text" style="width: 100%" />
          <el-skeleton-item variant="text" style="width: 60%" />
          <div style="margin-top: 20px">
            <el-skeleton-item variant="rect" style="width: 100%; height: 200px" />
          </div>
        </template>
      </el-skeleton>
    </div>

    <!-- 导入对话框 -->
    <el-dialog
      v-model="importDialogVisible"
      title="导入学生答案"
      width="500px"
    >
      <el-upload
        class="upload-demo"
        drag
        action="#"
        :auto-upload="false"
        :on-change="handleFileChange"
        accept=".xlsx,.xls,.csv"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            只能上传 xlsx/xls/csv 文件
          </div>
        </template>
      </el-upload>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="importDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="confirmImport"
            :loading="importLoading"
            :disabled="!selectedFile"
          >
            确认导入
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 班级调整/发布对话框 -->
    <el-dialog
      v-model="showClassroomDialog"
      :title="isPublishMode ? '发布考试 - 选择班级' : '调整考试班级'"
      width="700px"
      @close="resetClassroomDialog"
    >
      <div class="classroom-management">
        <el-form label-width="80px">
          <el-form-item label="目标班级">
            <el-select
              v-model="selectedClassroomIds"
              multiple
              placeholder="选择要发布考试的班级（可多选）"
              style="width: 100%"
              :loading="classroomLoading"
              filterable
              collapse-tags
              collapse-tags-tooltip
            >
              <el-option
                v-for="classroom in availableClassrooms"
                :key="classroom.id"
                :label="`${classroom.name} (${classroom.classCode})`"
                :value="classroom.id"
              >
                <div class="classroom-option">
                  <span class="classroom-name">{{ classroom.name }}</span>
                  <span class="classroom-code">({{ classroom.classCode }})</span>
                  <span class="classroom-students">{{ classroom.studentCount }}人</span>
                </div>
              </el-option>
            </el-select>
            <div class="form-help">
              不选择班级时，考试将对所有用户可见（仅管理员）
            </div>
          </el-form-item>
        </el-form>

        <!-- 可选班级列表 -->
        <el-divider>所有可选班级</el-divider>
        <div v-if="availableClassrooms.length === 0" class="empty-state">
          <el-empty description="暂无可选班级" size="small" />
        </div>
        <div v-else class="available-classrooms">
          <el-card
            v-for="classroom in availableClassrooms"
            :key="classroom.id"
            class="classroom-card selectable-classroom"
            shadow="hover"
            :class="{ selected: selectedClassroomIds.includes(classroom.id) }"
            @click="toggleClassroomSelection(classroom.id)"
          >
            <div class="classroom-info">
              <div class="classroom-header">
                <el-checkbox 
                  :model-value="selectedClassroomIds.includes(classroom.id)"
                  @change="toggleClassroomSelection(classroom.id)"
                  @click.stop
                >
                  <h4>{{ classroom.name }}</h4>
                </el-checkbox>
                <el-tag size="small">{{ classroom.classCode }}</el-tag>
              </div>
              <div class="classroom-stats">
                <span>学生数量: {{ classroom.studentCount }}</span>
                <el-button
                  size="small"
                  type="primary"
                  link
                  @click.stop="showClassroomMembers(classroom)"
                >
                  查看成员
                </el-button>
              </div>
            </div>
          </el-card>
        </div>

        <!-- 当前班级列表 -->
        <el-divider>当前考试班级</el-divider>
        <div v-if="examClassrooms.length === 0" class="empty-state">
          <el-empty description="未设置目标班级" size="small" />
        </div>
        <div v-else class="current-classrooms">
          <el-card
            v-for="classroom in examClassrooms"
            :key="classroom.id"
            class="classroom-card"
            shadow="hover"
          >
            <div class="classroom-info">
              <div class="classroom-header">
                <h4>{{ classroom.name }}</h4>
                <el-tag size="small">{{ classroom.classCode }}</el-tag>
              </div>
              <div class="classroom-stats">
                <span>学生数量: {{ classroom.studentCount }}</span>
                <el-button
                  size="small"
                  type="primary"
                  link
                  @click="showClassroomMembers(classroom)"
                >
                  查看成员
                </el-button>
              </div>
            </div>
          </el-card>
        </div>
      </div>
      <template #footer>
        <el-button @click="showClassroomDialog = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="updateExamClassrooms" 
          :loading="updateClassroomLoading"
        >
          {{ isPublishMode ? '发布考试' : '保存' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 班级成员查看对话框 -->
    <el-dialog
      v-model="showMembersDialog"
      title="班级成员"
      width="600px"
    >
      <div v-if="selectedClassroomForMembers">
        <el-descriptions :column="2" border class="mb-4">
          <el-descriptions-item label="班级名称">
            {{ selectedClassroomForMembers.name }}
          </el-descriptions-item>
          <el-descriptions-item label="班级代码">
            <el-tag>{{ selectedClassroomForMembers.classCode }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="学生数量" :span="2">
            {{ selectedClassroomForMembers.studentCount }} 人
          </el-descriptions-item>
        </el-descriptions>

        <el-table
          :data="selectedClassroomForMembers.students"
          style="width: 100%"
          size="small"
          max-height="300"
        >
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="email" label="邮箱" />
          <el-table-column prop="joinedAt" label="加入时间">
            <template #default="{ row }">
              {{ new Date(row.joinedAt).toLocaleDateString('zh-CN') }}
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="showMembersDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  DocumentAdd, 
  MagicStick, 
  More, 
  ArrowDown, 
  Edit, 
  Download, 
  Delete,
  Plus,
  View,
  Refresh,
  Upload,
  UploadFilled,
  Setting
} from '@element-plus/icons-vue'
import { examApi } from '@/api/exam'
import { questionApi } from '@/api/question'
import { answerApi } from '@/api/answer'
import { rubricApi } from '@/api/rubric'
import { classroomApi, type ClassroomResponse } from '@/api/classroom'
import { useAuthStore } from '@/stores/auth'
import type { ExamResponse, ExamStatistics, QuestionResponse } from '@/types/api'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 响应式数据
const loading = ref(true)
const statisticsLoading = ref(false)
const importLoading = ref(false)
const exam = ref<ExamResponse | null>(null)
const statistics = ref<ExamStatistics | null>(null)
const questions = ref<QuestionResponse[]>([])
const importDialogVisible = ref(false)
const selectedFile = ref<File | null>(null)

// 班级管理相关
const showClassroomDialog = ref(false)
const showMembersDialog = ref(false)
const classroomLoading = ref(false)
const updateClassroomLoading = ref(false)
const availableClassrooms = ref<ClassroomResponse[]>([])
const examClassrooms = ref<ClassroomResponse[]>([])
const selectedClassroomIds = ref<number[]>([])
const selectedClassroomForMembers = ref<ClassroomResponse | null>(null)
const isPublishMode = ref(false) // 是否处于发布模式

// 计算属性
const examId = computed(() => {
  const id = route.params.id
  if (typeof id === 'string') {
    return parseInt(id)
  }
  if (Array.isArray(id)) {
    return parseInt(id[0])
  }
  return id as number
})

// 生命周期
onMounted(async () => {
  // 权限检查：如果是学生角色，重定向到学生专用页面
  if (authStore.isStudent) {
    ElMessage.info('正在跳转到学生考试详情页面...')
    router.replace(`/my-exams/${examId.value}`)
    return
  }
  
  await loadExamData()
})

// 方法
const loadExamData = async () => {
  try {
    loading.value = true
    
    // 加载基础数据（考试详情和题目列表）
    const [examData, questionsData] = await Promise.all([
      examApi.getExam(examId.value),
      questionApi.getQuestionsByExam(examId.value)
    ])
    
    exam.value = examData
    questions.value = questionsData
    
    // 只有教师和管理员才加载统计信息和班级信息
    if (authStore.isTeacher || authStore.isAdmin) {
      try {
        const [statisticsData, classroomsData] = await Promise.all([
          examApi.getExamStatistics(examId.value).catch(() => null),
          examApi.getExamClassrooms(examId.value).catch(() => [])
        ])
        statistics.value = statisticsData
        examClassrooms.value = classroomsData
      } catch (error) {
        console.warn('Failed to load statistics or classroom data:', error)
        // 这些数据加载失败不影响主要功能
      }
    }
    
  } catch (error) {
    console.error('Failed to load exam data:', error)
    ElMessage.error('加载考试数据失败')
  } finally {
    loading.value = false
  }
}

const refreshStatistics = async () => {
  // 只有教师和管理员可以刷新统计信息
  if (!authStore.isTeacher && !authStore.isAdmin) {
    ElMessage.warning('没有权限查看统计信息')
    return
  }
  
  try {
    statisticsLoading.value = true
    statistics.value = await examApi.getExamStatistics(examId.value)
    ElMessage.success('统计信息已刷新')
  } catch (error) {
    console.error('Failed to refresh statistics:', error)
    ElMessage.error('刷新统计信息失败')
  } finally {
    statisticsLoading.value = false
  }
}

const navigateToAIEvaluation = async () => {
  try {
    // 检查评分标准
    if (questions.value.length === 0) {
      ElMessage.warning('该考试暂无题目，无法进行批阅')
      return
    }
    
    // 检查每个题目的评分标准
    const questionsWithoutRubric = []
    const questionsWithIncompleteRubric = []
    
    for (const question of questions.value) {
      try {
        const rubrics = await rubricApi.getQuestionRubrics(question.id)
        
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
          // 评分标准检查通过，开始批阅
          ElMessage.success('评分标准检查通过，开始批阅')
          router.push(`/exams/${examId.value}/ai-evaluation`)
        }
      } catch (error) {
        // 用户点击了"完善标准"按钮，跳转到评分标准管理页面
        navigateToRubricManagement()
      }
      return
    }
    
    // 评分标准检查通过，开始批阅
    ElMessage.success('评分标准检查通过，开始批阅')
    router.push(`/exams/${examId.value}/ai-evaluation`)
    
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('评分标准检查失败:', error)
      ElMessage.error('评分标准检查失败，无法开始批阅')
    }
  }
}

const navigateToRubricManagement = () => {
  // 传递来源页面信息
  router.push({
    path: `/exams/${examId.value}/rubric-management`,
    query: { 
      from: 'exam-detail',
      action: 'ai-evaluation' // 用户想要进行的操作
    }
  })
}

const navigateToAddQuestion = () => {
  router.push(`/exams/${examId.value}/questions/new`)
}

const viewQuestion = (questionId: number) => {
  router.push(`/questions/${questionId}`)
}

const editQuestion = (questionId: number) => {
  router.push(`/questions/${questionId}/edit`)
}

const viewAnswers = () => {
  router.push(`/exams/${examId.value}/answers`)
}

const deleteQuestion = async (questionId: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个题目吗？此操作不可恢复。',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await questionApi.deleteQuestion(questionId)
    ElMessage.success('题目删除成功')
    
    // 重新加载题目列表
    questions.value = await questionApi.getQuestionsByExam(examId.value)
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to delete question:', error)
      ElMessage.error('删除题目失败')
    }
  }
}

const evaluateQuestion = (questionId: number) => {
  router.push(`/questions/${questionId}/evaluation`)
}

const manageQuestionRubric = (questionId: number) => {
  router.push(`/questions/${questionId}/rubric`)
}

const handleDropdownCommand = async (command: string) => {
  switch (command) {
    case 'publish':
      await publishExam()
      break
    case 'unpublish':
      await unpublishExam()
      break
    case 'end':
      await endExam()
      break
    case 'classrooms':
      await openClassroomDialog()
      break
    case 'edit':
      router.push(`/exams/${examId.value}/edit`)
      break
    case 'export':
      exportExamData()
      break
    case 'delete':
      deleteExam()
      break
  }
}

const deleteExam = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除考试"${exam.value?.title}"吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await examApi.deleteExam(examId.value)
    ElMessage.success('考试删除成功')
    router.push('/exams')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to delete exam:', error)
      ElMessage.error('删除考试失败')
    }
  }
}

const exportExamData = async () => {
  try {
    // 这里应该调用导出API，目前暂时模拟
    ElMessage.info('导出功能正在开发中')
  } catch (error) {
    console.error('Failed to export exam:', error)
    ElMessage.error('导出失败')
  }
}

const showImportDialog = () => {
  importDialogVisible.value = true
  selectedFile.value = null
}

const handleFileChange = (file: any) => {
  selectedFile.value = file.raw
}

const confirmImport = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请选择要导入的文件')
    return
  }
  
  try {
    importLoading.value = true
    await answerApi.importAnswers(selectedFile.value)
    ElMessage.success('导入成功')
    importDialogVisible.value = false
    selectedFile.value = null
    
    // 刷新统计信息
    await refreshStatistics()
  } catch (error) {
    console.error('Failed to import answers:', error)
    ElMessage.error('导入失败')
  } finally {
    importLoading.value = false
  }
}

// 考试状态管理方法
const publishExam = async () => {
  try {
    // 设置为发布模式并打开班级选择对话框
    isPublishMode.value = true
    await openClassroomDialog()
  } catch (error: any) {
    isPublishMode.value = false
    if (error !== 'cancel') {
      console.error('发布考试失败:', error)
      ElMessage.error('发布考试失败')
    }
  }
}

const unpublishExam = async () => {
      try {
        await ElMessageBox.confirm(
          '确定要撤销发布此考试吗？撤销后考试将变为草稿状态。',
          '确认撤销',
          {
            confirmButtonText: '撤销',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
    
    const updatedExam = await examApi.unpublishExam(examId.value)
    exam.value = updatedExam
    ElMessage.success('撤销发布成功')
    loadExamData()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('撤销发布失败:', error)
      ElMessage.error('撤销发布失败')
    }
  }
}

const endExam = async () => {
      try {
        await ElMessageBox.confirm(
          '确定要结束此考试吗？结束后将不再接受新的答案。',
          '确认结束',
          {
            confirmButtonText: '结束',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
    
    const updatedExam = await examApi.endExam(examId.value)
    exam.value = updatedExam
    ElMessage.success('考试已结束')
    loadExamData()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('结束考试失败:', error)
      ElMessage.error('结束考试失败')
    }
  }
}

const openClassroomDialog = async () => {
  try {
    classroomLoading.value = true
    
    // 并行加载班级列表和当前考试班级
    const [allClassrooms, currentClassrooms] = await Promise.all([
      classroomApi.getAllClassrooms(),
      examApi.getExamClassrooms(examId.value)
    ])
    
    availableClassrooms.value = allClassrooms
    examClassrooms.value = currentClassrooms
    selectedClassroomIds.value = currentClassrooms.map(c => c.id)
    
    showClassroomDialog.value = true
  } catch (error) {
    console.error('加载班级信息失败:', error)
    ElMessage.error('加载班级信息失败')
  } finally {
    classroomLoading.value = false
  }
}

const updateExamClassrooms = async () => {
  try {
    updateClassroomLoading.value = true
    
    if (isPublishMode.value) {
      // 发布模式：检查是否选择了班级
      if (selectedClassroomIds.value.length === 0) {
        ElMessage.warning('请选择至少一个班级')
        return
      }
      
      // 确认发布
      await ElMessageBox.confirm(
        `确定要发布此考试到所选的 ${selectedClassroomIds.value.length} 个班级吗？发布后学生将可以参加考试。`,
        '确认发布',
        {
          confirmButtonText: '发布',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )

      // 先更新班级，再发布考试
      await examApi.updateExamClassrooms(examId.value, selectedClassroomIds.value)
      const updatedExam = await examApi.publishExam(examId.value)
      exam.value = updatedExam
      ElMessage.success('考试发布成功')
    } else {
      // 普通调整班级模式
      await examApi.updateExamClassrooms(examId.value, selectedClassroomIds.value)
      exam.value = await examApi.getExam(examId.value)
      ElMessage.success('班级调整成功')
    }
    
    // 重新加载考试班级信息
    examClassrooms.value = await examApi.getExamClassrooms(examId.value)
    
    showClassroomDialog.value = false
    isPublishMode.value = false
    loadExamData()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error(isPublishMode.value ? '发布考试失败:' : '调整班级失败:', error)
      
      // 处理特定的验证错误
      if (error.code === 422 && isPublishMode.value) {
        ElMessage.warning(error.message || '考试必须包含至少一个题目才能发布')
      } else {
        ElMessage.error(error.message || (isPublishMode.value ? '发布考试失败' : '调整班级失败'))
      }
    }
    isPublishMode.value = false
  } finally {
    updateClassroomLoading.value = false
  }
}

// 切换班级选择状态
const toggleClassroomSelection = (classroomId: number) => {
  const index = selectedClassroomIds.value.indexOf(classroomId)
  if (index > -1) {
    selectedClassroomIds.value.splice(index, 1)
  } else {
    selectedClassroomIds.value.push(classroomId)
  }
}

const showClassroomMembers = async (classroom: ClassroomResponse) => {
  try {
    selectedClassroomForMembers.value = await classroomApi.getClassroom(classroom.id)
    showMembersDialog.value = true
  } catch (error) {
    console.error('获取班级成员失败:', error)
    ElMessage.error('获取班级成员失败')
  }
}

const resetClassroomDialog = () => {
  selectedClassroomIds.value = []
  examClassrooms.value = []
  availableClassrooms.value = []
  isPublishMode.value = false
}

// 辅助方法
const formatDate = (dateString?: string) => {
  if (!dateString) return 'N/A'
  return new Date(dateString).toLocaleString('zh-CN')
}

const getStatusDisplayText = (status?: string) => {
  const statusMap = {
    'DRAFT': '草稿',
    'PUBLISHED': '已发布',
    'IN_PROGRESS': '进行中',
    'ENDED': '已结束',
    'CANCELLED': '已取消'
  }
  return statusMap[status as keyof typeof statusMap] || status || '未知'
}

const getStatusTagType = (status?: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    'DRAFT': 'info',
    'PUBLISHED': 'primary',
    'IN_PROGRESS': 'warning',
    'ENDED': 'success',
    'CANCELLED': 'danger'
  }
  return typeMap[status as keyof typeof typeMap] || 'info'
}

const getQuestionTypeText = (type: string) => {
  const typeMap = {
    'ESSAY': '论述题',
    'SHORT_ANSWER': '简答题',
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'TRUE_FALSE': '判断题',
    'CODING': '编程题',
    'CASE_ANALYSIS': '案例分析题'
  }
  return typeMap[type as keyof typeof typeMap] || type
}

const getQuestionTypeTag = (type: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const tagMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    'ESSAY': 'primary',
    'SHORT_ANSWER': 'success',
    'SINGLE_CHOICE': 'warning',
    'MULTIPLE_CHOICE': 'warning',
    'TRUE_FALSE': 'info',
    'CODING': 'danger',
    'CASE_ANALYSIS': 'primary'
  }
  return tagMap[type] || 'primary'
}

const getAnswerCount = (questionId: number) => {
  // 这里应该从统计数据中获取，暂时返回0
  return 0
}

const getEvaluatedCount = (questionId: number) => {
  // 这里应该从统计数据中获取，暂时返回0
  return 0
}
</script>

<style scoped>
.exam-detail {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
}

.page-header h1,
.page-title {
  margin: 8px 0 0 0;
  color: #303133;
  font-size: 24px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-actions-col {
  display: flex;
  justify-content: flex-end;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  white-space: nowrap;
}

.info-card, 
.questions-card, 
.stats-card, 
.actions-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
}

.questions-list {
  max-height: 600px;
  overflow-y: auto;
}

.question-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  background: #fafafa;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.question-title {
  flex: 1;
  font-weight: 500;
  color: #303133;
}

.question-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
}

.statistics {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.progress-section {
  grid-column: span 2;
  margin-top: 16px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.progress-text {
  font-weight: 500;
  color: #409eff;
}

.score-stats {
  grid-column: span 2;
  margin-top: 16px;
}

.score-item {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
}

.score-value {
  font-weight: 500;
  color: #409eff;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quick-actions .el-button {
  margin-left: 0 !important;
  width: 100%;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
}

.loading-container {
  padding: 40px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.upload-demo {
  margin: 20px 0;
}

/* 班级管理相关样式 */
.classroom-management {
  padding: 16px 0;
}

.form-help {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}

.classroom-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.classroom-name {
  font-weight: 500;
}

.classroom-code {
  color: #909399;
  margin-left: 8px;
}

.classroom-students {
  color: #409eff;
  font-size: 12px;
}

.current-classrooms {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.available-classrooms {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  max-height: 400px;
  overflow-y: auto;
}

.classroom-card {
  border: 1px solid #e4e7ed;
  transition: all 0.3s ease;
}

.selectable-classroom {
  cursor: pointer;
}

.selectable-classroom:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px 0 rgba(64, 158, 255, 0.15);
}

.selectable-classroom.selected {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.classroom-info {
  padding: 8px;
}

.classroom-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.classroom-header h4 {
  margin: 0;
  font-size: 16px;
  color: #303133;
  font-weight: 500;
}

.classroom-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #606266;
}

.mb-4 {
  margin-bottom: 16px;
}

.empty-state {
  text-align: center;
  padding: 20px;
}

/* 班级信息卡片样式 */
.classrooms-card {
  margin-bottom: 24px;
}

.classrooms-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.classroom-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  background-color: #fafafa;
  transition: all 0.3s ease;
}

.classroom-item:hover {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.classroom-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.classroom-header h4 {
  margin: 0;
  font-size: 16px;
  color: #303133;
  font-weight: 500;
}

.classroom-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #606266;
}
</style>
