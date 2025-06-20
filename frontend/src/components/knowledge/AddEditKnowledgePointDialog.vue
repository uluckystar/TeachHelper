<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑知识点' : '添加知识点'"
    width="700px"
    :close-on-click-modal="false"
    @closed="resetForm"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="知识点标题" prop="title">
            <el-input
              v-model="formData.title"
              placeholder="请输入知识点标题"
              maxlength="100"
              show-word-limit
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="分类" prop="category">
            <el-select
              v-model="formData.category"
              placeholder="选择或输入分类"
              filterable
              allow-create
              default-first-option
              style="width: 100%"
            >
              <el-option
                v-for="category in availableCategories"
                :key="category"
                :label="category"
                :value="category"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="知识点描述" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="4"
          placeholder="请输入知识点的详细描述"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="难度级别" prop="difficulty">
            <el-select v-model="formData.difficulty" placeholder="选择难度" style="width: 100%">
              <el-option label="简单" value="EASY">
                <div style="display: flex; justify-content: space-between;">
                  <span>简单</span>
                  <el-tag type="success" size="small">EASY</el-tag>
                </div>
              </el-option>
              <el-option label="中等" value="MEDIUM">
                <div style="display: flex; justify-content: space-between;">
                  <span>中等</span>
                  <el-tag type="warning" size="small">MEDIUM</el-tag>
                </div>
              </el-option>
              <el-option label="困难" value="HARD">
                <div style="display: flex; justify-content: space-between;">
                  <span>困难</span>
                  <el-tag type="danger" size="small">HARD</el-tag>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="重要程度" prop="importance">
            <el-rate
              v-model="formData.importance"
              :max="5"
              show-text
              :texts="['一般', '较低', '普通', '重要', '非常重要']"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="掌握程度" prop="masteryLevel">
            <el-slider
              v-model="formData.masteryLevel"
              :min="0"
              :max="100"
              show-tooltip
              :format-tooltip="formatMasteryTooltip"
            />
            <div class="mastery-hint">{{ getMasteryHint(formData.masteryLevel) }}</div>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="关键词标签">
        <div class="keywords-section">
          <el-tag
            v-for="(keyword, index) in formData.keywords"
            :key="index"
            closable
            @close="removeKeyword(index)"
            style="margin: 2px 4px 2px 0;"
          >
            {{ keyword }}
          </el-tag>
          <el-input
            v-if="showKeywordInput"
            ref="keywordInputRef"
            v-model="newKeyword"
            @keyup.enter="addKeyword"
            @blur="addKeyword"
            size="small"
            style="width: 80px; margin-left: 4px;"
          />
          <el-button
            v-else
            class="button-new-tag"
            size="small"
            @click="showNewKeywordInput"
          >
            + 新标签
          </el-button>
        </div>
      </el-form-item>

      <el-form-item label="关联知识点">
        <el-select
          v-model="formData.relatedPoints"
          multiple
          filterable
          placeholder="选择关联的知识点"
          style="width: 100%"
        >
          <el-option
            v-for="point in allKnowledgePoints"
            :key="point.id"
            :label="point.title"
            :value="point.id"
            :disabled="point.id === formData.id"
          >
            <div style="display: flex; justify-content: space-between;">
              <span>{{ point.title }}</span>
              <el-tag :type="getCategoryTagType(point.category)" size="small">
                {{ point.category }}
              </el-tag>
            </div>
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item label="附加信息">
        <el-collapse>
          <el-collapse-item title="扩展属性" name="extended">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="学习时长(分钟)">
                  <el-input-number
                    v-model="formData.estimatedTime"
                    :min="1"
                    :max="300"
                    placeholder="预估学习时长"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="适用年级">
                  <el-select
                    v-model="formData.gradeLevel"
                    multiple
                    placeholder="选择适用年级"
                    style="width: 100%"
                  >
                    <el-option
                      v-for="grade in availableGrades"
                      :key="grade"
                      :label="grade"
                      :value="grade"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-form-item label="参考资料">
              <el-input
                v-model="formData.references"
                type="textarea"
                :rows="2"
                placeholder="相关参考资料链接或说明"
              />
            </el-form-item>
            
            <el-form-item label="备注">
              <el-input
                v-model="formData.notes"
                type="textarea"
                :rows="2"
                placeholder="备注信息"
              />
            </el-form-item>
          </el-collapse-item>
        </el-collapse>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '更新' : '创建' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

// Props
const props = defineProps<{
  modelValue: boolean
  knowledgeBaseId: number
  editingPoint?: any
}>()

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: []
}>()

// 响应式数据
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const formRef = ref<FormInstance>()
const submitting = ref(false)
const showKeywordInput = ref(false)
const newKeyword = ref('')
const keywordInputRef = ref()

const isEdit = computed(() => !!props.editingPoint?.id)

// 表单数据
const formData = ref({
  id: 0,
  title: '',
  description: '',
  category: '',
  difficulty: 'MEDIUM' as 'EASY' | 'MEDIUM' | 'HARD',
  importance: 3,
  masteryLevel: 0,
  keywords: [] as string[],
  relatedPoints: [] as number[],
  estimatedTime: 30,
  gradeLevel: [] as string[],
  references: '',
  notes: ''
})

// 验证规则
const formRules: FormRules = {
  title: [
    { required: true, message: '请输入知识点标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入知识点描述', trigger: 'blur' },
    { min: 10, max: 500, message: '描述长度在 10 到 500 个字符', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择或输入分类', trigger: 'blur' }
  ],
  difficulty: [
    { required: true, message: '请选择难度级别', trigger: 'change' }
  ]
}

// 可选数据
const availableCategories = ref([
  '概念定义',
  '原理公式',
  '解题方法',
  '实例分析',
  '综合应用',
  '历史背景',
  '实验操作',
  '技能训练'
])

const availableGrades = ref([
  '小学一年级', '小学二年级', '小学三年级', '小学四年级', '小学五年级', '小学六年级',
  '初中一年级', '初中二年级', '初中三年级',
  '高中一年级', '高中二年级', '高中三年级'
])

const allKnowledgePoints = ref<any[]>([
  // Mock data - 在实际应用中从API获取
  { id: 1, title: '函数的概念', category: '函数' },
  { id: 2, title: '导数的定义', category: '微积分' },
  { id: 3, title: '化学反应平衡', category: '化学平衡' }
])

// 监听编辑数据变化
watch(() => props.editingPoint, (point) => {
  if (point && visible.value) {
    formData.value = {
      id: point.id || 0,
      title: point.title || '',
      description: point.description || '',
      category: point.category || '',
      difficulty: point.difficulty || 'MEDIUM',
      importance: point.importance || 3,
      masteryLevel: point.masteryLevel || 0,
      keywords: point.keywords || [],
      relatedPoints: point.relatedPoints || [],
      estimatedTime: point.estimatedTime || 30,
      gradeLevel: point.gradeLevel || [],
      references: point.references || '',
      notes: point.notes || ''
    }
  }
}, { immediate: true })

// 方法
const resetForm = () => {
  formData.value = {
    id: 0,
    title: '',
    description: '',
    category: '',
    difficulty: 'MEDIUM',
    importance: 3,
    masteryLevel: 0,
    keywords: [],
    relatedPoints: [],
    estimatedTime: 30,
    gradeLevel: [],
    references: '',
    notes: ''
  }
  
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    submitting.value = true

    // TODO: 调用API保存知识点
    console.log('Saving knowledge point:', formData.value)
    
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    ElMessage.success(isEdit.value ? '知识点更新成功' : '知识点创建成功')
    emit('saved')
    visible.value = false
  } catch (error) {
    console.error('Submit failed:', error)
    ElMessage.error('保存失败，请检查输入信息')
  } finally {
    submitting.value = false
  }
}

const showNewKeywordInput = () => {
  showKeywordInput.value = true
  nextTick(() => {
    keywordInputRef.value?.focus()
  })
}

const addKeyword = () => {
  if (newKeyword.value.trim() && !formData.value.keywords.includes(newKeyword.value.trim())) {
    formData.value.keywords.push(newKeyword.value.trim())
  }
  newKeyword.value = ''
  showKeywordInput.value = false
}

const removeKeyword = (index: number) => {
  formData.value.keywords.splice(index, 1)
}

const formatMasteryTooltip = (value: number) => {
  return `${value}%`
}

const getMasteryHint = (value: number) => {
  if (value < 30) return '需要加强学习'
  if (value < 60) return '基本掌握'
  if (value < 80) return '掌握良好'
  return '完全掌握'
}

const getCategoryTagType = (category: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    '概念定义': 'primary',
    '原理公式': 'success',
    '解题方法': 'warning',
    '实例分析': 'info',
    '综合应用': 'danger'
  }
  return typeMap[category] || 'info'
}
</script>

<style scoped>
.keywords-section {
  min-height: 32px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.button-new-tag {
  border: 1px dashed #d9d9d9;
  color: #999;
  background: #fafafa;
}

.button-new-tag:hover {
  border-color: #409eff;
  color: #409eff;
}

.mastery-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
