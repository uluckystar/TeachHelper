<template>
  <div class="ai-question-assistant">
    <div class="ai-actions">
      <!-- AI生成题目 -->
      <el-button 
        type="primary" 
        :icon="MagicStick" 
        @click="openAIQuestionDialog"
        :loading="generating"
        size="small"
      >
        AI生成题目
      </el-button>
      
      <!-- AI生成评分标准 -->
      <el-button 
        v-if="showRubricGeneration && questionId"
        type="success" 
        :icon="DocumentChecked" 
        @click="generateRubric"
        :loading="generatingRubric"
        size="small"
      >
        AI生成评分标准
      </el-button>
      
      <!-- AI智能填充 -->
      <el-button 
        v-if="showSmartFill"
        :icon="EditPen" 
        @click="smartFillQuestion"
        :loading="smartFilling"
        size="small"
      >
        AI智能填充
      </el-button>
    </div>

    <!-- AI题目生成对话框 -->
    <AIQuestionGenerationDialog
      v-model="showQuestionDialog"
      :knowledge-base="knowledgeBaseData!"
      v-if="knowledgeBaseData"
      @generated="handleQuestionsGenerated"
    />

    <!-- AI评分标准管理对话框 -->
    <RubricManagementDialog
      v-model="showRubricDialog"
      :question-id="questionId || null"
      @refresh="$emit('rubric-updated')"
    />

    <!-- AI智能填充对话框 -->
    <el-dialog
      v-model="showSmartFillDialog"
      title="AI智能填充"
      width="60%"
      :close-on-click-modal="false"
    >
      <el-form :model="smartFillForm" label-width="120px">
        <el-form-item label="描述需求">
          <el-input
            v-model="smartFillForm.description"
            type="textarea"
            :rows="3"
            placeholder="请描述您想要的题目内容，例如：关于函数的单选题，难度中等..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="题目类型">
          <el-select v-model="smartFillForm.questionType" placeholder="选择题目类型">
            <el-option label="单选题" value="SINGLE_CHOICE" />
            <el-option label="多选题" value="MULTIPLE_CHOICE" />
            <el-option label="判断题" value="TRUE_FALSE" />
            <el-option label="填空题" value="FILL_BLANK" />
            <el-option label="简答题" value="SHORT_ANSWER" />
            <el-option label="论述题" value="ESSAY" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="难度级别">
          <el-select v-model="smartFillForm.difficulty" placeholder="选择难度">
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showSmartFillDialog = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="executeSmartFill"
            :loading="smartFilling"
          >
            生成并填充
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { MagicStick, DocumentChecked, EditPen } from '@element-plus/icons-vue'
import AIQuestionGenerationDialog from '@/components/knowledge/AIQuestionGenerationDialog.vue'
import RubricManagementDialog from '@/components/evaluation/RubricManagementDialog.vue'
import { questionApi } from '@/api/question'

// Props
interface Props {
  knowledgeBaseId?: number
  questionId?: number
  showRubricGeneration?: boolean
  showSmartFill?: boolean
  currentQuestionData?: any
}

const props = withDefaults(defineProps<Props>(), {
  showRubricGeneration: true,
  showSmartFill: true
})

// Emits
const emit = defineEmits<{
  'question-generated': [question: any]
  'question-filled': [data: any]
  'rubric-updated': []
}>()

// 状态
const generating = ref(false)
const generatingRubric = ref(false)
const smartFilling = ref(false)
const showQuestionDialog = ref(false)
const showRubricDialog = ref(false)
const showSmartFillDialog = ref(false)

// 智能填充表单
const smartFillForm = ref({
  description: '',
  questionType: '',
  difficulty: 'MEDIUM'
})

// 计算属性
const hasCurrentQuestion = computed(() => {
  return props.currentQuestionData && (
    props.currentQuestionData.title || 
    props.currentQuestionData.content
  )
})

const knowledgeBaseData = computed(() => {
  if (!props.knowledgeBaseId) return null
  return {
    id: props.knowledgeBaseId,
    name: '当前知识库',
    description: '基于当前知识库生成题目'
  }
})

// 方法
const openAIQuestionDialog = () => {
  if (!props.knowledgeBaseId) {
    ElMessage.warning('请先选择知识库')
    return
  }
  showQuestionDialog.value = true
}

const generateRubric = async () => {
  if (!props.questionId) {
    ElMessage.warning('请先保存题目后再生成评分标准')
    return
  }
  
  showRubricDialog.value = true
}

const smartFillQuestion = () => {
  // 如果有当前题目数据，预填充描述
  if (hasCurrentQuestion.value) {
    smartFillForm.value.description = `基于当前题目改进：${props.currentQuestionData.title || props.currentQuestionData.content}`
  }
  showSmartFillDialog.value = true
}

const executeSmartFill = async () => {
  if (!smartFillForm.value.description.trim()) {
    ElMessage.warning('请输入题目描述')
    return
  }
  
  if (!smartFillForm.value.questionType) {
    ElMessage.warning('请选择题目类型')
    return
  }
  
  try {
    smartFilling.value = true
    
    // 调用AI生成单个题目
    const response = await questionApi.generateQuestions({
      knowledgeBaseId: props.knowledgeBaseId,
      questionTypes: [smartFillForm.value.questionType],
      difficultyDistribution: {
        [smartFillForm.value.difficulty]: 1
      },
      additionalRequirements: smartFillForm.value.description,
      generationStrategy: 'AI_GENERATED'
    })
    
    if (response.taskId) {
      // 轮询获取生成结果
      await pollGenerationResult(response.taskId)
    }
    
  } catch (error: any) {
    console.error('AI智能填充失败:', error)
    ElMessage.error('AI智能填充失败，请稍后重试')
  } finally {
    smartFilling.value = false
  }
}

const pollGenerationResult = async (taskId: string) => {
  const maxAttempts = 30
  let attempts = 0
  
  const poll = async (): Promise<void> => {
    try {
      // Note: For smart fill, we're using a simplified approach
      // In a real implementation, you might need a specific API for smart fill
      // For now, we'll simulate completion after a short delay
      ElMessage.success('AI智能填充功能开发中，请使用AI题目生成功能')
      showSmartFillDialog.value = false
      return
      
      // TODO: Implement proper smart fill API when available
      // const status = await questionApi.getSmartFillStatus(taskId)
      // if (status.status === 'COMPLETED' && status.result) {
      //   const formData = status.result
      //   emit('question-filled', formData)
      //   showSmartFillDialog.value = false
      //   
      //   ElNotification.success({
      //     title: 'AI智能填充成功',
      //     message: '题目内容已自动填充到表单中',
      //     duration: 3000
      //   })
      //   return
      // }
      
    } catch (error: any) {
      ElMessage.error(error.message || 'AI生成失败')
    }
  }
  
  await poll()
}

const handleQuestionsGenerated = (questions: any[]) => {
  if (questions && questions.length > 0) {
    emit('question-generated', questions[0])
    ElNotification.success({
      title: 'AI题目生成成功',
      message: `成功生成 ${questions.length} 道题目`,
      duration: 3000
    })
  }
}

// 重置智能填充表单
const resetSmartFillForm = () => {
  smartFillForm.value = {
    description: '',
    questionType: '',
    difficulty: 'MEDIUM'
  }
}

// 监听对话框关闭
watch(() => showSmartFillDialog.value, (newVal) => {
  if (!newVal) {
    resetSmartFillForm()
  }
})
</script>

<script lang="ts">
import { watch } from 'vue'
export default {
  name: 'AIQuestionAssistant'
}
</script>

<style scoped>
.ai-question-assistant {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ai-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ai-actions {
    flex-direction: column;
  }
  
  .ai-actions .el-button {
    width: 100%;
  }
}
</style>
