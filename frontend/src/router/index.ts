import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { requiresGuest: true }
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/auth/RegisterView.vue'),
      meta: { requiresGuest: true }
    },
    {
      path: '/dev-tools',
      name: 'DevTools',
      component: () => import('@/views/DevToolsView.vue'),
      meta: { devOnly: true }
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'Dashboard',
          component: () => import('@/views/DashboardView.vue')
        },
        {
          path: 'exams',
          name: 'ExamList',
          component: () => import('@/views/exam/ExamListView.vue')
        },
        {
          path: 'exams/create',
          name: 'CreateExam',
          component: () => import('@/views/exam/CreateExamView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'exams/:id',
          name: 'ExamDetail',
          component: () => import('@/views/exam/ExamDetailView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'exams/:id/edit',
          name: 'EditExam',
          component: () => import('@/views/exam/EditExamView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'exams/:id/questions',
          name: 'QuestionManagement',
          component: () => import('@/views/question/QuestionManagementView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'exams/:examId/questions/new',
          name: 'AddQuestionToExam',
          component: () => import('@/views/question/AddQuestionToExamView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'exams/:examId/take',
          name: 'TakeExam',
          component: () => import('@/views/student/TakeExamView.vue'),
          props: true,
          meta: { roles: ['STUDENT'] }
        },
        {
          path: 'exams/:examId/evaluation',
          name: 'ExamEvaluation',
          component: () => import('@/views/evaluation/ExamEvaluationView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'exams/:examId/ai-evaluation',
          name: 'AIEvaluation',
          component: () => import('@/views/evaluation/AIEvaluationView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'exams/:examId/rubric-management',
          name: 'ExamRubricManagement',
          component: () => import('@/views/evaluation/ExamRubricManagementView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'exams/:examId/reference-answer-management',
          name: 'ReferenceAnswerManagement',
          component: () => import('@/views/evaluation/ReferenceAnswerManagementView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'exams/:examId/answers',
          name: 'ExamAnswers',
          component: () => import('@/views/exam/ExamAnswersView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'exams/:examId/students/:studentId/paper',
          name: 'StudentPaperDetail',
          component: () => import('@/views/exam/StudentPaperDetailView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'exams/:examId/results',
          name: 'ExamResults',
          component: () => import('@/views/exam/ExamResultsView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'templates',
          name: 'UnifiedTemplateManagement',
          component: () => import('@/views/template/UnifiedTemplateManagementView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'templates/:id',
          name: 'TemplateDetail',
          component: () => import('@/views/template/TemplateDetailView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'templates/:id/edit',
          name: 'TemplateEdit',
          component: () => import('@/views/template/TemplateEditView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'evaluation/overview',
          name: 'EvaluationOverview',
          component: () => import('@/views/evaluation/EvaluationOverviewView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'evaluation/center',
          name: 'AIEvaluationCenter',
          component: () => import('@/views/evaluation/AIEvaluationCenterView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'batch-evaluation',
          name: 'BatchAIEvaluation',
          component: () => import('@/views/evaluation/BatchAIEvaluationView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'rubric-management',
          name: 'RubricManagement', 
          component: () => import('@/views/evaluation/RubricManagementView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'task-center',
          name: 'TaskCenter',
          component: () => import('@/views/task/TaskCenterView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'task-monitor',
          name: 'TaskMonitor',
          component: () => import('@/views/evaluation/TaskMonitorView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'tasks/:taskId',
          name: 'TaskDetail',
          component: () => import('@/views/evaluation/TaskDetailView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'tasks/:taskId/results',
          name: 'TaskResults',
          component: () => import('@/views/evaluation/TaskResultsView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'questions',
          name: 'QuestionLibrary',
          component: () => import('@/views/question/QuestionLibraryView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'questions/create',
          name: 'CreateQuestion',
          component: () => import('@/views/question/CreateQuestionView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'questions/generate',
          name: 'GenerateQuestionsLibrary',
          component: () => import('@/views/question/GenerateQuestionsLibraryView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'questions/:id',
          name: 'QuestionDetail',
          component: () => import('@/views/question/QuestionDetailView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'questions/:id/edit',
          name: 'EditQuestion',
          component: () => import('@/views/question/EditQuestionView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'questions/:id/evaluation',
          name: 'QuestionEvaluation',
          component: () => import('@/views/question/QuestionEvaluationView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'questions/:id/rubric',
          name: 'QuestionRubric',
          component: () => import('@/views/question/QuestionRubricView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'questions/:id/reference-answer',
          name: 'QuestionReferenceAnswer',
          component: () => import('@/views/question/QuestionReferenceAnswerView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'question-banks',
          name: 'QuestionBankManagement',
          component: () => import('@/views/question/QuestionBankManagementView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'question-banks/:id',
          name: 'QuestionBankDetail',
          component: () => import('@/views/question/QuestionBankDetailView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'my-exams',
          name: 'MyExams',
          component: () => import('@/views/student/MyExamsView.vue'),
          meta: { roles: ['STUDENT'] }
        },
        {
          path: 'my-exams/:examId',
          name: 'StudentExamDetail',
          component: () => import('@/views/student/StudentExamDetailView.vue'),
          props: true,
          meta: { roles: ['STUDENT'] }
        },
        {
          path: 'my-exams/:examId/result',
          name: 'StudentExamResult',
          component: () => import('@/views/student/StudentExamResultView.vue'),
          props: true,
          meta: { roles: ['STUDENT'] }
        },
        {
          path: 'my-exams/:examId/answers',
          name: 'StudentExamAnswers',
          component: () => import('@/views/student/StudentExamAnswersView.vue'),
          props: true,
          meta: { roles: ['STUDENT'] }
        },
        {
          path: 'users',
          name: 'UserManagement',
          component: () => import('@/views/admin/UserManagementView.vue'),
          meta: { roles: ['ADMIN'] }
        },
        {
          path: 'system',
          name: 'SystemSettings',
          component: () => import('@/views/admin/SystemSettingsView.vue'),
          meta: { roles: ['ADMIN'] }
        },
        {
          path: 'metadata',
          name: 'MetadataManagement',
          component: () => import('@/views/admin/MetadataManagementView.vue'),
          meta: { roles: ['ADMIN'] }
        },
        {
          path: 'subjects',
          name: 'SubjectManagement',
          component: () => import('@/views/admin/SubjectManagementView.vue'),
          meta: { roles: ['ADMIN'] }
        },
        {
          path: 'grade-levels',
          name: 'GradeLevelManagement',
          component: () => import('@/views/admin/GradeLevelManagementView.vue'),
          meta: { roles: ['ADMIN'] }
        },
        {
          path: 'classrooms',
          name: 'ClassroomManagement',
          component: () => import('@/views/admin/ClassroomManagementView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/views/ProfileView.vue')
        },
        {
          path: 'ai-config',
          name: 'AIConfig',
          component: () => import('@/views/ai-config.vue')
        },
        {
          path: 'knowledge',
          name: 'KnowledgeBase',
          component: () => import('@/views/knowledge/KnowledgeBaseManagement.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'knowledge2',
          name: 'KnowledgeBase2',
          component: () => import('@/views/knowledge/KnowledgeBaseManagementSimplified.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'knowledge/dashboard',
          name: 'KnowledgeDashboard',
          component: () => import('@/views/knowledge/KnowledgeBaseView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'knowledge/upload',
          name: 'KnowledgeUpload',
          component: () => import('@/views/knowledge/KnowledgeUploadView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'knowledge/test',
          name: 'KnowledgeUploadTest',
          component: () => import('@/views/knowledge/KnowledgeUploadTestView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'knowledge/dialog-test',
          name: 'KnowledgeDialogTest',
          component: () => import('@/views/knowledge/DialogTestView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'knowledge/document-upload',
          name: 'DocumentUpload',
          component: () => import('@/views/knowledge/DocumentUploadView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'paper-generation',
          name: 'PaperGeneration',
          component: () => import('@/views/paper/PaperGenerationView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'paper-generation/test',
          name: 'PaperGenerationTest',
          component: () => import('@/views/paper/PaperGenerationTestView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'exams/:examId/generate-questions',
          name: 'GenerateQuestions',
          component: () => import('@/views/question/GenerateQuestionsView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'knowledge/:id',
          name: 'KnowledgeBaseDetail',
          component: () => import('@/views/knowledge/KnowledgeBaseDetailView.vue'),
          props: true,
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        // 学生考试结果路由 - 移除重复的路由定义
        // 正确的路由应该是在学生功能区域: my-exams/:examId/result
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/NotFoundView.vue')
    }
  ]
})

// 路由守卫
router.beforeEach(async (to: any, from: any, next: any) => {
  const authStore = useAuthStore()
  
  if (import.meta.env.DEV) {
    console.log('Route guard:', {
      to: to.path,
      from: from.path,
      isInitialized: authStore.isInitialized,
      isAuthenticated: authStore.isAuthenticated,
      hasToken: !!authStore.token,
      hasUser: !!authStore.user,
      userRoles: authStore.user?.roles
    })
  }
  
  // 开发模式检查
  if (to.meta.devOnly && !import.meta.env.DEV) {
    next('/404')
    return
  }
  
  // 如果是登录页面，直接通过
  if (to.path === '/login') {
    next()
    return
  }
  
  // 确保认证状态已初始化
  if (!authStore.isInitialized) {
    if (import.meta.env.DEV) {
      console.log('Route guard: Initializing auth...')
    }
    await authStore.initAuth()
  }
  
  // 再次检查初始化后的状态
  if (import.meta.env.DEV) {
    console.log('Route guard: After init', {
      isAuthenticated: authStore.isAuthenticated,
      hasToken: !!authStore.token,
      hasUser: !!authStore.user
    })
  }
  
  // 检查是否需要认证
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    if (import.meta.env.DEV) {
      console.log('Route guard: Auth required but not authenticated, redirecting to login')
    }
    next('/login')
    return
  }
  
  // 检查是否需要游客状态
  if (to.meta.requiresGuest && authStore.isAuthenticated) {
    if (import.meta.env.DEV) {
      console.log('Route guard: Guest required but authenticated, redirecting to home')
    }
    next('/')
    return
  }
   // 检查角色权限
  if (to.meta.roles && authStore.user) {
    const hasRole = (to.meta.roles as string[]).some(role => authStore.user?.roles?.includes(role))
    if (!hasRole) {
      if (import.meta.env.DEV) {
        console.log('Route guard: Insufficient role permissions', {
          required: to.meta.roles,
          userRoles: authStore.user?.roles
        })
      }
      next('/')
      return
    }
  }

  // 特殊检查：学生进入考试页面时，检查是否已提交
  if (to.name === 'TakeExam' && authStore.user?.roles?.includes('STUDENT')) {
    const examId = parseInt(to.params.examId as string)
    if (examId) {
      try {
        const { answerApi } = await import('@/api/answer')
        const hasSubmitted = await answerApi.hasCurrentStudentSubmittedExam(examId)
        if (hasSubmitted) {
          if (import.meta.env.DEV) {
            console.log('Route guard: Student has already submitted exam', { examId })
          }
          // 重定向到我的考试页面，显示提示信息
          next({
            path: '/my-exams',
            query: { message: 'already_submitted', examId: examId.toString() }
          })
          return
        }
      } catch (error) {
        console.error('Route guard: Failed to check exam submission status', error)
        // 如果检查失败，允许进入但在页面内处理
      }
    }
  }

  if (import.meta.env.DEV) {
    console.log('Route guard: Navigation allowed')
  }
  next()
})

export default router
