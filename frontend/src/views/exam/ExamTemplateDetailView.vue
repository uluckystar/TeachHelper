<template>
  <div class="exam-template-detail">
    <!-- 模板基本信息 -->
    <div class="template-header">
      <div class="header-content">
        <div class="title-section">
          <h1>{{ template?.templateName || '模板详情' }}</h1>
          <div class="status-badges">
            <el-tag :type="getStatusType(template?.status)" size="large">
              {{ getStatusText(template?.status) }}
            </el-tag>
            <el-tag type="info" size="large">
              {{ template?.subject || '未设置科目' }}
            </el-tag>
          </div>
        </div>
        
        <!-- 进度信息 -->
        <div class="progress-section">
          <div class="progress-item">
            <span class="label">确认进度</span>
            <div class="progress-bar">
              <el-progress 
                :percentage="confirmProgress" 
                :status="confirmProgress === 100 ? 'success' : undefined">
                <template #default="{ percentage }">
                  {{ confirmedCount }}/{{ totalCount }} ({{ percentage }}%)
                </template>
              </el-progress>
            </div>
          </div>
          
          <div class="progress-stats">
            <div class="stat-item">
              <span class="number">{{ totalCount }}</span>
              <span class="label">总题目</span>
            </div>
            <div class="stat-item">
              <span class="number">{{ confirmedCount }}</span>
              <span class="label">已确认</span>
            </div>
            <div class="stat-item">
              <span class="number">{{ unconfirmedCount }}</span>
              <span class="label">待确认</span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 操作按钮 -->
      <div class="header-actions">
        <el-button 
          v-if="template?.status !== 'APPLIED' && confirmedCount > 0"
          type="warning" 
          @click="cancelAllConfirmations"
          :loading="isCancelingAll">
          一键取消确认
        </el-button>
        <el-button 
          v-if="template?.status === 'DRAFT' && unconfirmedCount > 0"
          type="success" 
          @click="confirmAllQuestions"
          :loading="isConfirmingAll">
          一键确认
        </el-button>
        <el-button 
          v-if="template?.status === 'DRAFT' && confirmProgress === 100"
          type="primary" 
          @click="markReady"
          :loading="isMarkingReady">
          标记就绪
        </el-button>
        <el-button 
          v-if="template?.status === 'READY'"
          type="success" 
          @click="openApplyDialog">
          应用模板
        </el-button>
        <el-button @click="$router.back()">返回</el-button>
      </div>
    </div>

    <!-- 题目列表 -->
    <div class="questions-section">
      <el-table 
        :data="questions" 
        stripe 
        style="width: 100%"
        v-loading="isLoading">
        <el-table-column prop="questionNumber" label="题号" width="80" align="center"/>
        <el-table-column prop="sectionHeader" label="段落" width="120"/>
        <el-table-column prop="questionType" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getQuestionTypeColor(row.questionType)">
              {{ getQuestionTypeText(row.questionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="questionContent" label="题目内容" min-width="300">
          <template #default="{ row }">
            <div class="question-content">
              {{ truncateText(row.questionContent, 100) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分值" width="80" align="center"/>
        <el-table-column label="确认状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag 
              :type="row.isConfirmed ? 'success' : 'warning'" 
              size="small">
              {{ row.isConfirmed ? '已确认' : '待确认' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button-group>
              <el-button 
                type="primary" 
                size="small" 
                @click="editQuestion(row)"
                :disabled="template?.status !== 'DRAFT'">
                编辑
              </el-button>
              <el-button 
                v-if="!row.isConfirmed"
                type="success" 
                size="small" 
                @click="confirmQuestion(row)"
                :disabled="template?.status !== 'DRAFT'"
                :loading="row.confirming">
                确认
              </el-button>
              <el-button 
                v-else
                type="warning" 
                size="small" 
                @click="unconfirmQuestion(row)"
                :disabled="template?.status === 'APPLIED'"
                :loading="row.unconfirming">
                取消确认
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 题目编辑对话框 -->
    <el-dialog 
      v-model="showEditDialog" 
      title="编辑题目" 
      width="80%" 
      :before-close="handleEditDialogClose">
      <el-form 
        :model="editForm" 
        :rules="editRules" 
        ref="editFormRef" 
        label-width="100px">
        
        <el-form-item label="段落标题" prop="sectionHeader">
          <el-input v-model="editForm.sectionHeader" placeholder="如：一、选择题"/>
        </el-form-item>
        
        <el-form-item label="题目类型" prop="questionType">
          <el-select v-model="editForm.questionType" placeholder="请选择题目类型">
            <el-option label="单选题" value="SINGLE_CHOICE"/>
            <el-option label="多选题" value="MULTIPLE_CHOICE"/>
            <el-option label="判断题" value="JUDGE"/>
            <el-option label="填空题" value="FILL_BLANK"/>
            <el-option label="简答题" value="ESSAY"/>
            <el-option label="计算题" value="CALCULATION"/>
          </el-select>
        </el-form-item>
        
        <el-form-item label="题目内容" prop="questionContent">
          <el-input 
            v-model="editForm.questionContent" 
            type="textarea" 
            :rows="4" 
            placeholder="请输入题目内容"/>
        </el-form-item>
        
        <el-form-item label="选项" prop="options" v-if="needsOptions">
          <el-input 
            v-model="editForm.options" 
            type="textarea" 
            :rows="3" 
            placeholder="请输入选项，每行一个选项，如：A. 选项一"/>
        </el-form-item>
        
        <el-form-item label="分值" prop="score">
          <el-input-number 
            v-model="editForm.score" 
            :min="0" 
            :max="100" 
            :step="0.5" 
            controls-position="right"/>
        </el-form-item>
        
        <el-form-item label="正确答案" prop="correctAnswer">
          <el-input 
            v-model="editForm.correctAnswer" 
            placeholder="请输入正确答案"/>
        </el-form-item>
        
        <el-form-item label="解释说明" prop="explanation">
          <el-input 
            v-model="editForm.explanation" 
            type="textarea" 
            :rows="2" 
            placeholder="可选：题目解释或答题要点"/>
        </el-form-item>
        
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleEditDialogClose">取消</el-button>
          <el-button type="primary" @click="saveQuestion" :loading="isSaving">
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 应用模板对话框 -->
    <el-dialog 
      v-model="showApplyDialog" 
      title="应用模板" 
      width="60%">
      <el-form 
        :model="applyForm" 
        :rules="applyRules" 
        ref="applyFormRef" 
        label-width="120px">
        
        <el-form-item label="选择考试" prop="examId">
          <el-select 
            v-model="applyForm.examId" 
            placeholder="请选择要导入答案的考试" 
            style="width: 100%">
            <el-option 
              v-for="exam in availableExams" 
              :key="exam.id" 
              :label="exam.title" 
              :value="exam.id"/>
          </el-select>
        </el-form-item>
        
        <el-form-item label="选择班级" prop="classroomId">
          <el-select 
            v-model="applyForm.classroomId" 
            placeholder="请选择班级" 
            style="width: 100%">
            <el-option 
              v-for="classroom in availableClassrooms" 
              :key="classroom.id" 
              :label="classroom.name" 
              :value="classroom.id"/>
          </el-select>
        </el-form-item>
        
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showApplyDialog = false">取消</el-button>
          <el-button type="primary" @click="applyTemplate" :loading="isApplying">
            开始导入
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getTemplateById,
  getTemplateQuestions,
  updateTemplateQuestion,
  confirmTemplateQuestion,
  unconfirmTemplateQuestion,
  markTemplateReady,
  importAnswersFromTemplate,
  type ExamTemplate,
  type ExamTemplateQuestion,
  type ExamTemplateQuestionUpdateRequest
} from '@/api/examTemplate'
import { examApi } from '@/api/exam'
import { classroomApi } from '@/api/classroom'
import type { TagType } from '@/utils/tagTypes'
import { getQuestionTypeColor as getGlobalQuestionTypeColor, getQuestionTypeText as getGlobalQuestionTypeText } from '@/utils/tagTypes'

const route = useRoute()
const router = useRouter()

interface FullExamTemplateQuestionUpdateRequest extends ExamTemplateQuestionUpdateRequest {
  id?: number;
  questionNumber?: number;
}

// 响应式数据
const template = ref<ExamTemplate | null>(null)
const questions = ref<ExamTemplateQuestion[]>([])
const isLoading = ref(false)
const isMarkingReady = ref(false)
const isSaving = ref(false)
const isApplying = ref(false)
const isConfirmingAll = ref(false)
const isCancelingAll = ref(false)

// 对话框状态
const showEditDialog = ref(false)
const showApplyDialog = ref(false)

// 编辑表单
const editFormRef = ref<FormInstance>()
const currentEditingQuestion = ref<ExamTemplateQuestion | null>(null)
const editForm: FullExamTemplateQuestionUpdateRequest = reactive({
  id: undefined,
  questionNumber: undefined,
  sectionHeader: '',
  questionContent: '',
  questionType: '',
  score: undefined,
  options: '',
  correctAnswer: '',
  explanation: '',
})

const editRules: FormRules = {
  questionContent: [
    { required: true, message: '请输入题目内容', trigger: 'blur' }
  ],
  questionType: [
    { required: true, message: '请选择题目类型', trigger: 'change' }
  ],
  score: [
    { required: true, message: '请输入分值', trigger: 'blur' }
  ]
}

// 应用模板表单
const applyFormRef = ref<FormInstance>()
const applyForm = reactive({
  examId: '',
  classroomId: '',
})

const applyRules: FormRules = {
  examId: [
    { required: true, message: '请选择考试', trigger: 'change' }
  ]
}

const availableExams = ref<{ id: number; title: string }[]>([])
const availableClassrooms = ref<{ id: number; name: string }[]>([])

// 计算属性
const totalCount = computed(() => questions.value.length)
const confirmedCount = computed(() => questions.value.filter(q => q.isConfirmed).length)
const unconfirmedCount = computed(() => totalCount.value - confirmedCount.value)
const confirmProgress = computed(() => totalCount.value === 0 ? 0 : Math.round((confirmedCount.value / totalCount.value) * 100))

const needsOptions = computed(() => {
  return ['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(editForm.questionType as string)
})

// 方法
const loadTemplate = async () => {
  isLoading.value = true
  try {
    const idParam = route.params.id;
    if (!idParam) {
      ElMessage.error('模板ID缺失');
      router.back();
      return;
    }
    const id = Array.isArray(idParam) ? idParam[0] : idParam;
    const templateResponse = await getTemplateById(Number(id));
    template.value = templateResponse;
    if (template.value) {
      await loadTemplateQuestions(template.value.id);
    }
  } catch (error) {
    console.error('加载模板失败:', error);
    ElMessage.error('加载模板失败');
    router.back();
  } finally {
    isLoading.value = false;
  }
};

const loadTemplateQuestions = async (templateId: number) => {
  isLoading.value = true;
  try {
    const response = await getTemplateQuestions(templateId);
    questions.value = response;
  } catch (error) {
    console.error('加载题目失败:', error);
    ElMessage.error('加载题目失败');
  } finally {
    isLoading.value = false;
  }
};

const editQuestion = (question: ExamTemplateQuestion) => {
  currentEditingQuestion.value = { ...question };
  editForm.id = question.id;
  editForm.questionNumber = question.questionNumber;
  editForm.sectionHeader = question.sectionHeader || '';
  editForm.questionContent = question.questionContent || '';
  editForm.questionType = convertQuestionTypeToEnum(question.questionType || '');
  editForm.score = question.score;
  editForm.options = question.options || '';
  editForm.correctAnswer = question.correctAnswer || '';
  editForm.explanation = question.explanation || '';
  showEditDialog.value = true;
};

const saveQuestion = async () => {
  try {
    await editFormRef.value?.validate();
    if (!template.value || currentEditingQuestion.value?.id === undefined) {
      ElMessage.error('模板信息或题目ID缺失');
      return;
    }
    isSaving.value = true;

    const updateData: ExamTemplateQuestionUpdateRequest = {
      sectionHeader: editForm.sectionHeader,
      questionContent: editForm.questionContent,
      questionType: editForm.questionType,
      score: editForm.score,
      options: editForm.options,
      correctAnswer: editForm.correctAnswer,
      explanation: editForm.explanation,
    };

    const updatedQuestion: ExamTemplateQuestion = await updateTemplateQuestion(template.value.id, currentEditingQuestion.value.id, updateData);

    const index = questions.value.findIndex(q => q.id === updatedQuestion.id);
    if (index !== -1) {
      questions.value[index] = updatedQuestion;
      ElMessage.success('题目更新成功');
    } else {
      ElMessage.warning('未能找到更新的题目');
    }
    showEditDialog.value = false;
  } catch (error) {
    console.error('保存题目失败:', error);
    ElMessage.error('保存题目失败');
  } finally {
    isSaving.value = false;
  }
};

const confirmQuestion = async (question: ExamTemplateQuestion) => {
  if (!template.value) return;
  question.confirming = true;
  try {
    await confirmTemplateQuestion(template.value.id, question.id);
    question.isConfirmed = true;
    ElMessage.success(`题目 ${question.questionNumber} 已确认`);
  } catch (error) {
    console.error(`确认题目 ${question.questionNumber} 失败:`, error);
    ElMessage.error(`确认题目 ${question.questionNumber} 失败`);
  } finally {
    question.confirming = false;
  }
};

const unconfirmQuestion = async (question: ExamTemplateQuestion) => {
  if (!template.value) return;
  question.unconfirming = true;
  try {
    await unconfirmTemplateQuestion(template.value.id, question.id);
    question.isConfirmed = false;
    ElMessage.success(`题目 ${question.questionNumber} 已取消确认`);
  } catch (error) {
    console.error(`取消确认题目 ${question.questionNumber} 失败:`, error);
    ElMessage.error(`取消确认题目 ${question.questionNumber} 失败`);
  } finally {
    question.unconfirming = false;
  }
};

const confirmAllQuestions = async () => {
  if (!template.value) {
    ElMessage.error('模板信息缺失，无法执行一键确认');
    return;
  }

  isConfirmingAll.value = true;
  try {
    const unconfirmedQuestions = questions.value.filter(q => !q.isConfirmed);
    if (unconfirmedQuestions.length === 0) {
      ElMessage.info('所有题目都已确认，无需再次操作。');
      return;
    }

    await ElMessageBox.confirm(
      `确定要一键确认所有 ${unconfirmedQuestions.length} 道未确认的题目吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );

    let successCount = 0;
    let failCount = 0;
    
    for (const question of unconfirmedQuestions) {
      try {
        await confirmTemplateQuestion(template.value.id, question.id);
        question.isConfirmed = true;
        successCount++;
      } catch (error) {
        console.error(`一键确认题目 ${question.questionNumber} 失败:`, error);
        failCount++;
      }
    }
    
    ElMessage.success(`一键确认完成：成功 ${successCount} 道，失败 ${failCount} 道。`);
    await loadTemplateQuestions(template.value.id);

  } catch (error) {
    if (error !== 'cancel') {
      console.error('一键确认操作失败:', error);
      ElMessage.error('一键确认操作失败');
    }
  } finally {
    isConfirmingAll.value = false;
  }
};

const cancelAllConfirmations = async () => {
  if (!template.value) {
    ElMessage.error('模板信息缺失，无法执行操作');
    return;
  }

  const confirmedQuestions = questions.value.filter(q => q.isConfirmed);
  if (confirmedQuestions.length === 0) {
    ElMessage.info('没有已确认的题目，无需操作。');
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确定要一键取消所有 ${confirmedQuestions.length} 道已确认的题目吗？这可能会将模板状态退回至草稿。`,
      '重要提示',
      {
        confirmButtonText: '确定取消',
        cancelButtonText: '再想想',
        type: 'warning',
      }
    );

    isCancelingAll.value = true;
    let successCount = 0;
    let failCount = 0;
    
    for (const question of confirmedQuestions) {
      try {
        await unconfirmTemplateQuestion(template.value.id, question.id);
        question.isConfirmed = false;
        successCount++;
      } catch (error) {
        console.error(`一键取消题目 ${question.questionNumber} 失败:`, error);
        failCount++;
      }
    }
    
    ElMessage.success(`一键取消完成：成功 ${successCount} 道，失败 ${failCount} 道。`);
    // 操作完成后，重新加载整个模板数据，因为状态可能已改变
    await loadTemplate();

  } catch (error) {
    if (error !== 'cancel') {
      console.error('一键取消操作失败:', error);
      ElMessage.error('一键取消操作失败');
    }
  } finally {
    isCancelingAll.value = false;
  }
};

const markReady = async () => {
  if (!template.value || confirmProgress.value < 100) {
    ElMessage.warning('所有题目必须先确认才能标记为就绪');
    return;
  }
  isMarkingReady.value = true;
  try {
    await markTemplateReady(template.value.id);
    template.value.status = 'READY';
    ElMessage.success('模板已标记为就绪');
  } catch (error) {
    console.error('标记就绪失败:', error);
    ElMessage.error('标记就绪失败');
  } finally {
    isMarkingReady.value = false;
  }
};

const applyTemplate = async () => {
  try {
    await applyFormRef.value?.validate();
    if (!template.value) {
      ElMessage.error('模板信息缺失');
      return;
    }
    isApplying.value = true;

    const subject = template.value.subject;
    // 班级ID是可选的
    const classroomId = applyForm.classroomId || null; 

    if (!subject) {
      ElMessage.error('无法获取科目信息');
      isApplying.value = false;
      return;
    }

    // 构造请求参数
    const params = {
      templateId: template.value.id,
      examId: Number(applyForm.examId),
      subject: subject,
      // 如果 classFolder 需要，则传递 classroomId
      classFolder: classroomId ? String(classroomId) : undefined, 
    };

    await importAnswersFromTemplate(params);

    ElMessage.success('模板应用成功，正在后台导入学生答案...');
    showApplyDialog.value = false;
  } catch (error) {
    if (error instanceof Error && error.message.includes('cancel')) {
       // 用户取消操作，不显示错误消息
    } else {
      console.error('应用模板失败:', error);
      ElMessage.error( (error instanceof Error) ? `应用模板失败: ${error.message}`: '应用模板失败');
    }
  } finally {
    isApplying.value = false;
  }
};

const openApplyDialog = async () => {
  if (!template.value) return;
  try {
    // 重置表单
    applyForm.examId = '';
    applyForm.classroomId = '';
    
    // 加载可用考试 - 使用为教师/管理员设计的接口
    const examsResponse = await examApi.getAllExams(0, 1000); // 获取前1000条，相当于不分页
    availableExams.value = examsResponse.map(exam => ({
      id: exam.id,
      title: exam.title
    }));
    
    // 加载可用班级
    const classroomsResponse = await classroomApi.getAllClassrooms();
    availableClassrooms.value = classroomsResponse.map(cls => ({
      id: cls.id,
      name: cls.name
    }));

    if (availableExams.value.length === 0) {
      ElMessage.warning('当前没有可用的考试。请先创建考试。');
    }

    showApplyDialog.value = true;
  } catch (error) {
    console.error('加载可用考试和班级失败:', error);
    ElMessage.error('加载可用考试和班级失败');
  }
};

const handleEditDialogClose = () => {
  editFormRef.value?.resetFields();
  showEditDialog.value = false;
  currentEditingQuestion.value = null;
};

// 工具方法
const getStatusType = (status: string | undefined): TagType => {
  switch (status) {
    case 'DRAFT': return 'info';
    case 'READY': return 'success';
    case 'APPLIED': return 'primary';
    default: return 'info';
  }
};

const getStatusText = (status: string | undefined): string => {
  switch (status) {
    case 'DRAFT': return '草稿';
    case 'READY': return '就绪';
    case 'APPLIED': return '已应用';
    default: return '未知';
  }
};

const getQuestionTypeColor = (type: string | undefined): TagType => {
  return getGlobalQuestionTypeColor(type || '');
};

const getQuestionTypeText = (type: string | undefined): string => {
  return getGlobalQuestionTypeText(type || '');
};

const truncateText = (text: string | undefined, maxLength: number) => {
  if (!text) return '';
  if (text.length <= maxLength) return text;
  return text.substring(0, maxLength) + '...';
};

const convertQuestionTypeToEnum = (type: string | undefined): string => {
  switch (type) {
    case '单选题': return 'SINGLE_CHOICE';
    case '多选题': return 'MULTIPLE_CHOICE';
    case '判断题': return 'JUDGE';
    case '填空题': return 'FILL_BLANK';
    case '简答题': return 'ESSAY';
    case '计算题': return 'CALCULATION';
    case '论述题': return 'ESSAY';
    case '分析题': return 'ESSAY';
    default: return type || '';
  }
};

onMounted(() => {
  loadTemplate();
});
</script>

<style scoped lang="scss">
.exam-template-detail {
  padding: 20px;
  background-color: #f5f5f5;
  min-height: 100vh;

  .template-header {
    background: white;
    border-radius: 8px;
    padding: 24px;
    margin-bottom: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    display: flex;
    justify-content: space-between;
    align-items: flex-start;

    .header-content {
      flex: 1;

      .title-section {
        margin-bottom: 20px;

        h1 {
          font-size: 24px;
          color: #303133;
          margin: 0 0 12px 0;
        }

        .status-badges {
          display: flex;
          gap: 12px;
        }
      }

      .progress-section {
        .progress-item {
          margin-bottom: 16px;

          .label {
            display: block;
            color: #606266;
            margin-bottom: 8px;
            font-weight: 500;
          }

          .progress-bar {
            max-width: 400px;
          }
        }

        .progress-stats {
          display: flex;
          gap: 24px;

          .stat-item {
            text-align: center;

            .number {
              display: block;
              font-size: 24px;
              font-weight: bold;
              color: #409EFF;
              margin-bottom: 4px;
            }

            .label {
              font-size: 14px;
              color: #909399;
            }
          }
        }
      }
    }

    .header-actions {
      display: flex;
      gap: 12px;
      flex-shrink: 0;
    }
  }

  .questions-section {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

    .question-content {
      line-height: 1.5;
      color: #303133;
    }
  }

  .dialog-footer {
    text-align: right;
  }
}

@media (max-width: 768px) {
  .exam-template-detail {
    padding: 12px;

    .template-header {
      flex-direction: column;
      gap: 20px;

      .header-actions {
        width: 100%;
        justify-content: flex-end;
      }
    }

    .progress-stats {
      flex-direction: column;
      gap: 12px;
    }
  }
}
</style> 