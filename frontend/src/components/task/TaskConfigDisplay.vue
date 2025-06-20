<template>
  <div class="task-config-display">
    <el-descriptions v-if="taskType === 'evaluation'" :column="2" border>
      <el-descriptions-item label="评估类型">{{ config.evaluationType || '--' }}</el-descriptions-item>
      <el-descriptions-item label="AI配置">{{ config.aiConfigName || '--' }}</el-descriptions-item>
      <el-descriptions-item label="评分标准">{{ config.rubricName || '--' }}</el-descriptions-item>
      <el-descriptions-item label="批量大小">{{ config.batchSize || '--' }}</el-descriptions-item>
      <el-descriptions-item label="并发数">{{ config.concurrency || '--' }}</el-descriptions-item>
      <el-descriptions-item label="任务描述" :span="2">{{ config.description || '--' }}</el-descriptions-item>
    </el-descriptions>

    <el-descriptions v-else-if="taskType === 'generation'" :column="2" border>
      <el-descriptions-item label="生成类型">{{ config.generationType || '--' }}</el-descriptions-item>
      <el-descriptions-item label="题目数量">{{ config.questionCount || '--' }}</el-descriptions-item>
      <el-descriptions-item label="知识库">{{ config.knowledgeBaseName || '--' }}</el-descriptions-item>
      <el-descriptions-item label="难度分布">{{ formatDifficulty(config.difficulty) }}</el-descriptions-item>
      <el-descriptions-item label="题型配置">{{ formatQuestionTypes(config.questionTypes) }}</el-descriptions-item>
      <el-descriptions-item label="生成要求" :span="2">{{ config.requirements || '--' }}</el-descriptions-item>
    </el-descriptions>

    <el-descriptions v-else-if="taskType === 'knowledge'" :column="2" border>
      <el-descriptions-item label="处理类型">{{ config.processingType || '--' }}</el-descriptions-item>
      <el-descriptions-item label="文档数量">{{ config.documentCount || '--' }}</el-descriptions-item>
      <el-descriptions-item label="知识库">{{ config.knowledgeBaseName || '--' }}</el-descriptions-item>
      <el-descriptions-item label="向量化配置">{{ config.vectorConfig || '--' }}</el-descriptions-item>
      <el-descriptions-item label="分块策略">{{ config.chunkStrategy || '--' }}</el-descriptions-item>
      <el-descriptions-item label="处理选项" :span="2">{{ formatProcessingOptions(config.options) }}</el-descriptions-item>
    </el-descriptions>

    <div v-else class="unknown-config">
      <el-alert
        title="未知的任务类型"
        type="warning"
        :closable="false"
        show-icon
      />
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  config: any
  taskType: string
}

const props = defineProps<Props>()

// 格式化方法
const formatDifficulty = (difficulty: any) => {
  if (!difficulty) return '--'
  if (typeof difficulty === 'string') return difficulty
  
  const levels = []
  if (difficulty.easy) levels.push(`简单(${difficulty.easy}%)`)
  if (difficulty.medium) levels.push(`中等(${difficulty.medium}%)`)
  if (difficulty.hard) levels.push(`困难(${difficulty.hard}%)`)
  
  return levels.join(', ') || '--'
}

const formatQuestionTypes = (types: any) => {
  if (!types || !Array.isArray(types)) return '--'
  
  return types.map(type => {
    const typeNames = {
      'multiple_choice': '选择题',
      'fill_blank': '填空题',
      'short_answer': '简答题',
      'essay': '论述题'
    }
    const typeName = typeNames[type.type as keyof typeof typeNames] || type.type
    return `${typeName}(${type.count}道)`
  }).join(', ')
}

const formatProcessingOptions = (options: any) => {
  if (!options) return '--'
  if (typeof options === 'string') return options
  
  const optionList = []
  if (options.extractText) optionList.push('文本提取')
  if (options.generateSummary) optionList.push('摘要生成')
  if (options.createIndex) optionList.push('索引创建')
  if (options.detectLanguage) optionList.push('语言检测')
  
  return optionList.join(', ') || '--'
}
</script>

<style scoped>
.task-config-display {
  padding: 16px;
}

.unknown-config {
  padding: 20px;
  text-align: center;
}
</style>
