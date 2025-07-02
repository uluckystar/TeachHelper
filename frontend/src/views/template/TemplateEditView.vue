<template>
  <div class="template-edit-view">
    <h2>编辑模板</h2>
    <el-form v-if="form" :model="form" label-width="100px" @submit.prevent>
      <el-form-item label="名称">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="form.templateType">
          <el-option label="手动创建" value="MANUAL" />
          <el-option label="AI生成" value="AI_GENERATED" />
          <el-option label="文档提取" value="DOCUMENT_EXTRACTED" />
          <el-option label="复制" value="COPIED" />
        </el-select>
      </el-form-item>
      <el-form-item label="科目">
        <el-input v-model="form.subject" />
      </el-form-item>
      <el-form-item label="总分">
        <el-input-number v-model="form.totalScore" :min="0" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="save">保存</el-button>
        <el-button @click="goBack">取消</el-button>
      </el-form-item>
    </el-form>
    <el-skeleton v-else rows="4" animated />
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { examPaperTemplateApi, type ExamPaperTemplate, type ExamPaperTemplateRequest } from '@/api/examPaperTemplate'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const form = ref<ExamPaperTemplateRequest | null>(null)

const loadTemplate = async () => {
  const id = route.params.id
  if (!id) return
  const res = await examPaperTemplateApi.getTemplate(Number(id))
  const t = res.data
  form.value = {
    name: t.name,
    templateType: t.templateType,
    subject: t.subject,
    totalScore: t.totalScore,
    description: t.description,
    gradeLevel: t.gradeLevel || '',
    status: t.status,
    isPublic: t.isPublic
  }
}

const save = async () => {
  if (!form.value) return
  const id = route.params.id
  await examPaperTemplateApi.updateTemplate(Number(id), form.value)
  ElMessage.success('保存成功')
  router.push(`/templates/${id}`)
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadTemplate()
})
</script>
<style scoped>
.template-edit-view {
  padding: 24px;
  max-width: 600px;
}
</style> 