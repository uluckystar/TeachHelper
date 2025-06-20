<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑评估标准' : '创建评估标准'"
    width="800px"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      class="rubric-form"
    >
      <el-form-item label="标准名称" prop="name">
        <el-input
          v-model="form.name"
          placeholder="请输入评估标准名称"
          maxlength="100"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="适用学科" prop="subject">
        <el-select v-model="form.subject" placeholder="选择适用学科">
          <el-option label="通用" value="" />
          <el-option label="语文" value="语文" />
          <el-option label="数学" value="数学" />
          <el-option label="英语" value="英语" />
          <el-option label="物理" value="物理" />
          <el-option label="化学" value="化学" />
          <el-option label="生物" value="生物" />
          <el-option label="历史" value="历史" />
          <el-option label="地理" value="地理" />
          <el-option label="政治" value="政治" />
          <el-option label="其他" value="其他" />
        </el-select>
      </el-form-item>

      <el-form-item label="标准描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="请描述此评估标准的用途和特点"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="评分标准" prop="criteria">
        <div class="criteria-section">
          <div class="criteria-header">
            <span>评分标准（共 {{ form.criteria.length }} 项）</span>
            <el-button type="primary" size="small" @click="addCriterion">
              <el-icon><Plus /></el-icon>
              添加标准
            </el-button>
          </div>

          <div v-if="form.criteria.length === 0" class="empty-criteria">
            <el-empty description="暂无评分标准" :image-size="100">
              <el-button type="primary" @click="addCriterion">添加第一个标准</el-button>
            </el-empty>
          </div>

          <div v-else class="criteria-list">
            <el-card
              v-for="(criterion, index) in form.criteria"
              :key="index"
              class="criterion-card"
              shadow="never"
            >
              <template #header>
                <div class="criterion-header">
                  <span class="criterion-index">标准 {{ index + 1 }}</span>
                  <el-button
                    type="danger"
                    size="small"
                    text
                    @click="removeCriterion(index)"
                  >
                    <el-icon><Delete /></el-icon>
                    删除
                  </el-button>
                </div>
              </template>

              <el-form-item :label="`标准名称`" :prop="`criteria.${index}.name`" :rules="criterionRules.name">
                <el-input
                  v-model="criterion.name"
                  placeholder="例如：内容准确性"
                  maxlength="50"
                />
              </el-form-item>

              <el-form-item :label="`标准描述`" :prop="`criteria.${index}.description`">
                <el-input
                  v-model="criterion.description"
                  type="textarea"
                  :rows="2"
                  placeholder="描述此标准的评判要求"
                  maxlength="200"
                />
              </el-form-item>

              <el-form-item :label="`权重`" :prop="`criteria.${index}.weight`">
                <el-input-number
                  v-model="criterion.weight"
                  :min="0"
                  :max="100"
                  :step="5"
                  placeholder="0-100"
                />
                <span class="weight-tip">权重百分比，所有标准权重之和应为100%</span>
              </el-form-item>

              <el-form-item :label="`评分等级`">
                <div class="score-levels">
                  <div
                    v-for="(level, levelIndex) in criterion.scoreLevels"
                    :key="levelIndex"
                    class="score-level"
                  >
                    <el-input
                      v-model="level.name"
                      placeholder="等级名称"
                      style="width: 120px;"
                    />
                    <el-input-number
                      v-model="level.score"
                      :min="0"
                      :max="100"
                      style="width: 120px;"
                      placeholder="分数"
                    />
                    <el-input
                      v-model="level.description"
                      placeholder="等级描述"
                      style="flex: 1; margin: 0 8px;"
                    />
                    <el-button
                      type="danger"
                      size="small"
                      text
                      @click="removeScoreLevel(index, levelIndex)"
                      :disabled="!criterion.scoreLevels || criterion.scoreLevels.length <= 1"
                    >
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                  <el-button
                    type="primary"
                    size="small"
                    text
                    @click="addScoreLevel(index)"
                  >
                    <el-icon><Plus /></el-icon>
                    添加等级
                  </el-button>
                </div>
              </el-form-item>
            </el-card>
          </div>

          <div v-if="form.criteria.length > 0" class="weight-summary">
            <span>总权重: {{ totalWeight }}%</span>
            <el-tag :type="totalWeight === 100 ? 'success' : 'warning'">
              {{ totalWeight === 100 ? '权重分配正确' : '权重需要调整为100%' }}
            </el-tag>
          </div>
        </div>
      </el-form-item>

      <el-form-item label="状态">
        <el-switch
          v-model="form.isActive"
          active-text="启用"
          inactive-text="禁用"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">
        {{ isEdit ? '更新' : '创建' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { rubricApi } from '@/api/rubric'
import type { Rubric, RubricCriterion, ScoreLevel } from '@/types/api'

interface Props {
  modelValue: boolean
  rubricData?: Rubric | null
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const formRef = ref<FormInstance>()
const submitting = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const isEdit = computed(() => !!props.rubricData?.id)

// 表单数据
const defaultForm = () => ({
  name: '',
  subject: '',
  description: '',
  criteria: [] as RubricCriterion[],
  isActive: true
})

const form = ref(defaultForm())

// 计算权重总和
const totalWeight = computed(() => {
  return form.value.criteria.reduce((sum, criterion) => sum + (criterion.weight || 0), 0)
})

// 表单验证规则
const rules: FormRules = {
  name: [
    { required: true, message: '请输入标准名称', trigger: 'blur' },
    { min: 1, max: 100, message: '长度在 1 到 100 个字符', trigger: 'blur' }
  ],
  subject: [
    { required: true, message: '请选择适用学科', trigger: 'change' }
  ],
  criteria: [
    { required: true, message: '请至少添加一个评分标准', trigger: 'change' }
  ]
}

const criterionRules = {
  name: [
    { required: true, message: '请输入标准名称', trigger: 'blur' }
  ],
  weight: [
    { required: true, message: '请输入权重', trigger: 'blur' },
    { type: 'number', min: 0, max: 100, message: '权重应在0-100之间', trigger: 'blur' }
  ]
}

// 监听rubricData变化，初始化表单
watch(() => props.rubricData, (newData) => {
  if (newData) {
    form.value = {
      name: newData.name || '',
      subject: newData.subject || '',
      description: newData.description || '',
      criteria: newData.criteria ? newData.criteria.map(c => ({
        id: c.id,
        criterionText: c.name || '',
        points: c.weight || 0,
        name: c.name,
        description: c.description,
        weight: c.weight,
        scoreLevels: c.scoreLevels || []
      } as RubricCriterion)) : [],
      isActive: newData.isActive !== false
    }
  } else {
    form.value = defaultForm()
  }
}, { immediate: true })

// 方法
const addCriterion = () => {
  const newCriterion: RubricCriterion = {
    criterionText: '',
    points: 0,
    name: '',
    description: '',
    weight: 0,
    scoreLevels: [
      { id: 1, level: '优秀', name: '优秀', score: 90, description: '完全符合要求' },
      { id: 2, level: '良好', name: '良好', score: 70, description: '基本符合要求' },
      { id: 3, level: '及格', name: '及格', score: 60, description: '部分符合要求' },
      { id: 4, level: '不及格', name: '不及格', score: 30, description: '不符合要求' }
    ]
  }
  form.value.criteria.push(newCriterion)
}

const removeCriterion = (index: number) => {
  form.value.criteria.splice(index, 1)
}

const addScoreLevel = (criterionIndex: number) => {
  const newLevel: ScoreLevel = {
    id: Date.now(), // 临时ID
    level: '',
    name: '',
    score: 0,
    description: ''
  }
  form.value.criteria[criterionIndex].scoreLevels?.push(newLevel)
}

const removeScoreLevel = (criterionIndex: number, levelIndex: number) => {
  form.value.criteria[criterionIndex].scoreLevels?.splice(levelIndex, 1)
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    
    if (totalWeight.value !== 100) {
      ElMessage.warning('所有标准权重之和必须为100%')
      return
    }

    if (form.value.criteria.length === 0) {
      ElMessage.warning('请至少添加一个评分标准')
      return
    }

    submitting.value = true

    // TODO: 修复类型兼容性问题
    console.warn('RubricDialog保存功能暂时禁用 - 需要修复类型兼容性')
    ElMessage.warning('此功能正在开发中')
    
    // const data = {
    //   ...form.value,
    //   id: props.rubricData?.id
    // }

    // if (isEdit.value) {
    //   await rubricApi.updateRubric(data.id!, data)
    //   ElMessage.success('评估标准更新成功')
    // } else {
    //   await rubricApi.createRubric(data)
    //   ElMessage.success('评估标准创建成功')
    // }

    emit('success')
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

const handleClose = () => {
  visible.value = false
  if (formRef.value) {
    formRef.value.resetFields()
  }
  form.value = defaultForm()
}
</script>

<style scoped>
.rubric-form {
  max-height: 600px;
  overflow-y: auto;
}

.criteria-section {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 16px;
}

.criteria-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-weight: 600;
}

.empty-criteria {
  text-align: center;
  padding: 20px;
}

.criteria-list {
  gap: 16px;
}

.criterion-card {
  margin-bottom: 16px;
  border: 1px solid #e4e7ed;
}

.criterion-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.criterion-index {
  font-weight: 600;
  color: #409eff;
}

.weight-tip {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}

.score-levels {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
  background: #fafafa;
}

.score-level {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.score-level:last-child {
  margin-bottom: 0;
}

.weight-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 14px;
}
</style>
