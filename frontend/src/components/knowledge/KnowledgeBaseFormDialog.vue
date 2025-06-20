<template>
  <el-dialog
    v-model="visible"
    :title="editingKb ? '编辑知识库' : '创建知识库'"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
      <el-form-item label="知识库名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入知识库名称" />
      </el-form-item>
      
      <el-form-item label="描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="请输入知识库描述"
        />
      </el-form-item>
      
      <el-form-item label="年级" prop="gradeLevel">
        <el-select v-model="form.gradeLevel" placeholder="请选择年级" @change="handleGradeChange">
          <el-option
            v-for="grade in gradeLevels"
            :key="grade"
            :label="grade"
            :value="grade"
          />
        </el-select>
      </el-form-item>
      
      <el-form-item label="学科" prop="subject">
        <div class="subject-input-container">
          <el-select
            v-model="form.subject"
            placeholder="请选择学科"
            filterable
            allow-create
            @change="handleSubjectChange"
          >
            <el-option
              v-for="subject in availableSubjects"
              :key="subject"
              :label="subject"
              :value="subject"
            />
          </el-select>
          <el-button @click="showCreateSubjectDialog = true" type="primary" link>
            <el-icon><Plus /></el-icon>
            新增学科
          </el-button>
        </div>
        <div v-if="!form.gradeLevel" class="form-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>请先选择年级，系统将为您推荐合适的学科</span>
        </div>
        <div v-else-if="selectedGradeCategory" class="form-tip">
          <el-icon><Star /></el-icon>
          <span>已为{{ selectedGradeCategory }}阶段推荐{{ recommendedSubjects.length }}个常用学科</span>
        </div>
      </el-form-item>

      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio label="draft">草稿</el-radio>
          <el-radio label="published">已发布</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="标签" prop="tags">
        <el-select
          v-model="form.tags"
          multiple
          filterable
          allow-create
          placeholder="请选择或创建标签"
        >
          <el-option
            v-for="tag in availableTags"
            :key="tag"
            :label="tag"
            :value="tag"
          />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">
          {{ editingKb ? '更新' : '创建' }}
        </el-button>
      </div>
    </template>

    <!-- 创建新学科对话框 -->
    <CreateSubjectDialog
      v-model="showCreateSubjectDialog"
      @created="handleSubjectCreated"
    />
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, reactive } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus, InfoFilled, Star } from '@element-plus/icons-vue'
import CreateSubjectDialog from './CreateSubjectDialog.vue'

// 导入统一的类型定义
import type { KnowledgeBase as BaseKnowledgeBase } from '@/api/knowledge'

// 扩展类型定义
interface KnowledgeBase extends Partial<BaseKnowledgeBase> {
  name: string
  description?: string
  gradeLevel?: string
  subject?: string
  status?: 'draft' | 'published'
  tags?: string[]
}

interface Props {
  modelValue: boolean
  knowledgeBase?: KnowledgeBase | null
  gradeLevels: string[]
  subjects: string[]
  availableTags: string[]
  saving?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', data: KnowledgeBase): void
  (e: 'subject-created', subject: string): void
}

const props = withDefaults(defineProps<Props>(), {
  saving: false
})

const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const formRef = ref<FormInstance>()
const showCreateSubjectDialog = ref(false)

const editingKb = computed(() => !!props.knowledgeBase?.id)

// 表单数据
const form = reactive<KnowledgeBase>({
  name: '',
  description: '',
  gradeLevel: '',
  subject: '',
  status: 'draft',
  tags: []
})

// 年级分类映射
const gradeCategories = {
  '小学': ['一年级', '二年级', '三年级', '四年级', '五年级', '六年级'],
  '初中': ['初一', '初二', '初三'],
  '高中': ['高一', '高二', '高三']
}

// 学科推荐映射
const subjectRecommendations = {
  '小学': ['语文', '数学', '英语', '科学', '道德与法治', '音乐', '美术', '体育'],
  '初中': ['语文', '数学', '英语', '物理', '化学', '生物', '历史', '地理', '道德与法治', '音乐', '美术', '体育'],
  '高中': ['语文', '数学', '英语', '物理', '化学', '生物', '历史', '地理', '政治', '音乐', '美术', '体育', '信息技术']
}

const selectedGradeCategory = computed(() => {
  if (!form.gradeLevel) return null
  for (const [category, grades] of Object.entries(gradeCategories)) {
    if (grades.includes(form.gradeLevel)) {
      return category
    }
  }
  return null
})

const recommendedSubjects = computed(() => {
  if (!selectedGradeCategory.value) return []
  return subjectRecommendations[selectedGradeCategory.value as keyof typeof subjectRecommendations] || []
})

const availableSubjects = computed(() => {
  // 合并推荐学科和已有学科
  const recommended = recommendedSubjects.value
  const existing = props.subjects.filter(s => !recommended.includes(s))
  return [...recommended, ...existing]
})

// 表单验证规则
const formRules: FormRules = {
  name: [
    { required: true, message: '请输入知识库名称', trigger: 'blur' },
    { min: 2, max: 50, message: '名称长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  description: [
    { max: 200, message: '描述不能超过 200 个字符', trigger: 'blur' }
  ],
  gradeLevel: [
    { required: true, message: '请选择年级', trigger: 'change' }
  ],
  subject: [
    { required: true, message: '请选择学科', trigger: 'change' }
  ]
}

// 监听知识库数据变化，重置表单
watch(() => props.knowledgeBase, (newKb) => {
  if (newKb) {
    Object.assign(form, { ...newKb })
  } else {
    // 重置表单
    Object.assign(form, {
      name: '',
      description: '',
      gradeLevel: '',
      subject: '',
      status: 'draft',
      tags: []
    })
  }
}, { immediate: true })

const handleClose = () => {
  visible.value = false
  formRef.value?.resetFields()
}

const handleSave = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    emit('save', { ...form })
  } catch (error) {
    ElMessage.error('请检查表单输入')
  }
}

const handleGradeChange = () => {
  // 年级变化时，如果当前学科不在推荐列表中，清空学科选择
  if (form.subject && !availableSubjects.value.includes(form.subject)) {
    form.subject = ''
  }
}

const handleSubjectChange = (value: string) => {
  if (value && !props.subjects.includes(value)) {
    // 新创建的学科，通知父组件
    emit('subject-created', value)
  }
}

const handleSubjectCreated = (subject: string) => {
  showCreateSubjectDialog.value = false
  form.subject = subject
  emit('subject-created', subject)
}
</script>

<style scoped>
.subject-input-container {
  display: flex;
  align-items: center;
  width: 100%;
}

.subject-input-container .el-select {
  flex: 1;
  margin-right: 8px;
}

.form-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.dialog-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
</style>
