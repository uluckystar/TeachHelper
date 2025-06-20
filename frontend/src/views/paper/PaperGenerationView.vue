<template>
  <div class="paper-generation">
    <div class="page-header">
      <h1>智能试卷生成</h1>
      <p>基于知识库内容和AI技术自动生成试卷</p>
    </div>

    <el-card class="main-card">
      <el-tabs v-model="activeTab" @tab-click="handleTabClick" class="generation-tabs">
        <!-- 快速生成 -->
        <el-tab-pane label="快速生成" name="quick">
          <QuickGenerationForm 
            @generate="handleGenerate"
            @save-template="handleSaveTemplate"
            :loading="generating"
          />
        </el-tab-pane>

        <!-- 智能组卷 -->
        <el-tab-pane label="智能组卷" name="smart">
          <SmartPaperGeneration 
            @generate="handleSmartGenerate"
            @save-template="handleSaveTemplate"
            :loading="generating"
          />
        </el-tab-pane>

        <!-- 知识库出题 -->
        <el-tab-pane label="知识库出题" name="knowledge">
          <KnowledgeBasedGeneration 
            @generate="handleKnowledgeGenerate"
            :loading="generating"
          />
        </el-tab-pane>

        <!-- 增强版AI生成 -->
        <el-tab-pane name="enhanced-ai">
          <template #label>
            <span style="display: flex; align-items: center; gap: 6px;">
              ✨ AI智能生成
              <el-tag size="small" type="warning">Enhanced</el-tag>
            </span>
          </template>
          <EnhancedAIGeneration 
            @generate="handleEnhancedAIGenerate"
            :loading="generating"
          />
        </el-tab-pane>

        <!-- 题库组合 -->
        <el-tab-pane label="题库组合" name="bank">
          <QuestionBankCombination 
            @generate="handleBankGenerate"
            :loading="generating"
          />
        </el-tab-pane>

        <!-- 模板管理 -->
        <el-tab-pane label="模板管理" name="templates">
          <TemplateManager 
            @generate-from-template="handleGenerateFromTemplate"
            @edit-template="handleEditTemplate"
            :loading="generating"
          />
        </el-tab-pane>

        <!-- 生成历史 -->
        <el-tab-pane label="生成历史" name="history">
          <GenerationHistory />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 生成结果对话框 -->
    <el-dialog
      v-model="resultDialogVisible"
      title="试卷生成结果"
      width="80%"
      :close-on-click-modal="false"
    >
      <GenerationResult 
        v-if="generationResult"
        :result="generationResult"
        @view-exam="handleViewExam"
        @regenerate="handleRegenerate"
      />
    </el-dialog>

    <!-- 模板编辑对话框 -->
    <el-dialog
      v-model="templateDialogVisible"
      :title="isEditingTemplate ? '编辑模板' : '保存为模板'"
      width="70%"
    >
      <TemplateForm
        v-if="templateDialogVisible"
        :template="currentTemplate"
        :is-editing="isEditingTemplate"
        @save="handleTemplateSave"
        @cancel="templateDialogVisible = false"
        :loading="templateSaving"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { paperGenerationApi, aiQuestionGenerationApi, type PaperGenerationRequest, type PaperGenerationResponse, type PaperGenerationTemplate, type PaperGenerationTemplateRequest } from '@/api/knowledge'
import QuickGenerationForm from '@/components/paper/QuickGenerationForm.vue'
import SmartPaperGeneration from '@/components/paper/SmartPaperGeneration.vue'
import KnowledgeBasedGeneration from '@/components/paper/KnowledgeBasedGeneration.vue'
import EnhancedAIGeneration from '@/components/paper/EnhancedAIGeneration.vue'
import QuestionBankCombination from '@/components/paper/QuestionBankCombination.vue'
import TemplateManager from '@/components/paper/TemplateManager.vue'
import GenerationHistory from '@/components/paper/GenerationHistory.vue'
import GenerationResult from '@/components/paper/GenerationResult.vue'
import TemplateForm from '@/components/paper/TemplateForm.vue'

const router = useRouter()

// 响应式数据
const activeTab = ref('quick')
const generating = ref(false)
const resultDialogVisible = ref(false)
const templateDialogVisible = ref(false)
const templateSaving = ref(false)
const isEditingTemplate = ref(false)

const generationResult = ref<PaperGenerationResponse | null>(null)
const currentTemplate = ref<PaperGenerationTemplate | null>(null)

// 处理标签页切换
const handleTabClick = (tab: any) => {
  console.log('切换到标签页:', tab.name)
}

// 处理快速生成
const handleGenerate = async (request: PaperGenerationRequest) => {
  try {
    generating.value = true
    ElMessage.info('正在生成试卷，请稍候...')
    
    const response = await paperGenerationApi.generatePaper(request)
    generationResult.value = response.data
    resultDialogVisible.value = true
    
    ElMessage.success('试卷生成完成！')
    
  } catch (error) {
    console.error('生成试卷失败:', error)
    ElMessage.error('生成试卷失败，请重试')
  } finally {
    generating.value = false
  }
}

// 处理智能组卷生成
const handleSmartGenerate = async (request: PaperGenerationRequest) => {
  try {
    generating.value = true
    ElMessage.info('正在智能组卷，请稍候...')
    
    const response = await paperGenerationApi.smartGenerate(request)
    generationResult.value = response.data
    resultDialogVisible.value = true
    
    ElMessage.success('智能组卷完成！')
    
  } catch (error) {
    console.error('智能组卷失败:', error)
    ElMessage.error('智能组卷失败，请重试')
  } finally {
    generating.value = false
  }
}

// 处理知识库出题生成
const handleKnowledgeGenerate = async (request: PaperGenerationRequest) => {
  try {
    generating.value = true
    ElMessage.info('正在知识库出题，请稍候...')
    
    const response = await paperGenerationApi.knowledgeGenerate(request)
    generationResult.value = response.data
    resultDialogVisible.value = true
    
    ElMessage.success('知识库出题完成！')
    
  } catch (error) {
    console.error('知识库出题失败:', error)
    ElMessage.error('知识库出题失败，请重试')
  } finally {
    generating.value = false
  }
}

// 处理题库组合生成
const handleBankGenerate = async (request: PaperGenerationRequest) => {
  try {
    generating.value = true
    ElMessage.info('正在题库组合，请稍候...')
    
    const response = await paperGenerationApi.bankGenerate(request)
    generationResult.value = response.data
    resultDialogVisible.value = true
    
    ElMessage.success('题库组合完成！')
    
  } catch (error) {
    console.error('题库组合失败:', error)
    ElMessage.error('题库组合失败，请重试')
  } finally {
    generating.value = false
  }
}

// 处理增强版AI生成
const handleEnhancedAIGenerate = async (request: any) => {
  try {
    generating.value = true
    ElMessage.info('正在使用增强版AI生成试卷，请稍候...')
    
    const response = await aiQuestionGenerationApi.generateQuestions(request)
    
    // 转换增强版响应为标准试卷格式
    const convertedResult = {
      examId: 0, // 临时ID，等待后端分配
      examTitle: '增强版AI生成试卷',
      examDescription: `使用AI策略生成的智能试卷`,
      questions: response.questions?.map((q: any, index: number) => ({
        id: q.id,
        title: `题目${index + 1}`,
        content: q.content,
        questionType: q.type,
        maxScore: 5, // 默认分值
        difficulty: q.difficulty,
        referenceAnswer: q.answer,
        knowledgePoint: q.sources && q.sources.length > 0 ? q.sources[0].extractedContent : undefined,
        sourceDocument: q.sources && q.sources.length > 0 ? q.sources[0].documentTitle : undefined,
        options: q.options ? q.options.map((option: string, optIndex: number) => ({
          optionId: optIndex + 1,
          content: option,
          isCorrect: false, // 需要根据答案设置
          optionOrder: optIndex + 1
        })) : undefined
      })) || [],
      warnings: [],
      generationSummary: `成功生成 ${response.generatedCount || 0} 道题目`,
      statistics: {
        totalQuestions: response.generatedCount || 0,
        totalScore: (response.generatedCount || 0) * 5,
        typeStatistics: {
          singleChoice: 0,
          multipleChoice: 0,
          trueFalse: 0,
          shortAnswer: 0,
          essay: 0,
          coding: 0,
          caseAnalysis: 0,
          calculation: 0
        },
        difficultyStatistics: {
          easy: 0,
          medium: 0,
          hard: 0
        }
      },
      generatedAt: new Date().toISOString(),
      enhanced: true, // 增强版标记
      sourceStrategy: 'AI_GENERATED',
      sourceStatistics: {}
    }
    
    generationResult.value = convertedResult
    resultDialogVisible.value = true
    
    ElMessage.success(`增强版AI生成完成！共生成 ${response.generatedCount || 0} 道题目`)
    
  } catch (error) {
    console.error('增强版AI生成失败:', error)
    ElMessage.error('增强版AI生成失败：' + (error as any).message)
  } finally {
    generating.value = false
  }
}

// 处理保存为模板
const handleSaveTemplate = (template: PaperGenerationTemplateRequest) => {
  currentTemplate.value = null
  isEditingTemplate.value = false
  templateDialogVisible.value = true
  
  // 将传入的数据设置为当前模板
  setTimeout(() => {
    if (currentTemplate.value) {
      Object.assign(currentTemplate.value, template)
    }
  }, 100)
}

// 处理基于模板生成
const handleGenerateFromTemplate = async (template: PaperGenerationTemplate, title: string) => {
  try {
    generating.value = true
    ElMessage.info('正在基于模板生成试卷...')
    
    const response = await paperGenerationApi.generateFromTemplate(template.id, title)
    generationResult.value = response.data
    resultDialogVisible.value = true
    
    ElMessage.success('基于模板生成试卷完成！')
    
  } catch (error) {
    console.error('基于模板生成试卷失败:', error)
    ElMessage.error('基于模板生成试卷失败，请重试')
  } finally {
    generating.value = false
  }
}

// 处理编辑模板
const handleEditTemplate = (template: PaperGenerationTemplate) => {
  currentTemplate.value = { ...template }
  isEditingTemplate.value = true
  templateDialogVisible.value = true
}

// 处理模板保存
const handleTemplateSave = async (template: PaperGenerationTemplateRequest) => {
  try {
    templateSaving.value = true
    
    if (isEditingTemplate.value && currentTemplate.value) {
      await paperGenerationApi.updateTemplate(currentTemplate.value.id, template)
      ElMessage.success('模板更新成功')
    } else {
      await paperGenerationApi.createTemplate(template)
      ElMessage.success('模板保存成功')
    }
    
    templateDialogVisible.value = false
    
    // 刷新模板列表
    if (activeTab.value === 'templates') {
      // 触发模板管理组件刷新
    }
    
  } catch (error) {
    console.error('保存模板失败:', error)
    ElMessage.error('保存模板失败，请重试')
  } finally {
    templateSaving.value = false
  }
}

// 处理查看考试
const handleViewExam = (examId: number) => {
  router.push(`/exams/${examId}`)
}

// 处理重新生成
const handleRegenerate = () => {
  resultDialogVisible.value = false
  activeTab.value = 'quick'
}

onMounted(() => {
  console.log('试卷生成页面已加载')
})
</script>

<style scoped>
.paper-generation {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-header h1 {
  color: #303133;
  font-size: 28px;
  margin-bottom: 10px;
}

.page-header p {
  color: #606266;
  font-size: 16px;
}

.main-card {
  min-height: 600px;
}

:deep(.el-tabs__content) {
  padding-top: 20px;
}

:deep(.el-dialog__body) {
  max-height: 70vh;
  overflow-y: auto;
}
</style>
