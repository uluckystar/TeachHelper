<template>
  <el-dialog
    v-model="visible"
    title="高级搜索"
    width="800px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-tabs v-model="activeTab" @tab-click="handleTabChange">
      <!-- 基础搜索 -->
      <el-tab-pane label="基础搜索" name="basic">
        <el-form :model="searchForm" inline>
          <el-form-item label="知识库名称">
            <el-input
              v-model="searchForm.name"
              placeholder="请输入知识库名称"
              clearable
              @keyup.enter="performSearch"
            />
          </el-form-item>
          <el-form-item label="学科">
            <el-select v-model="searchForm.subject" placeholder="请选择学科" clearable>
              <el-option
                v-for="subject in subjects"
                :key="subject"
                :label="subject"
                :value="subject"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="年级">
            <el-select v-model="searchForm.gradeLevel" placeholder="请选择年级" clearable>
              <el-option
                v-for="grade in gradeLevels"
                :key="grade"
                :label="grade"
                :value="grade"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="创建时间">
            <el-date-picker
              v-model="searchForm.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
              <el-option label="已发布" value="published" />
              <el-option label="草稿" value="draft" />
              <el-option label="已归档" value="archived" />
            </el-select>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- AI智能搜索 -->
      <el-tab-pane label="AI智能搜索" name="smart">
        <div class="smart-search-container">
          <div class="search-input-section">
            <el-input
              v-model="smartSearchQuery"
              type="textarea"
              :rows="4"
              placeholder="请输入您要查找的内容描述，AI会为您智能匹配最相关的知识库内容..."
              @keyup.ctrl.enter="performSmartSearch"
            />
            <div class="search-tips">
              <el-icon><InfoFilled /></el-icon>
              <span>支持自然语言描述，如"关于数学函数的练习题"、"高中物理力学相关内容"等</span>
            </div>
          </div>

          <div class="search-options">
            <el-form :model="smartSearchOptions" inline>
              <el-form-item label="搜索范围">
                <el-checkbox-group v-model="smartSearchOptions.searchScope">
                  <el-checkbox label="documents">文档内容</el-checkbox>
                  <el-checkbox label="knowledge_points">知识点</el-checkbox>
                  <el-checkbox label="questions">题目</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item label="相似度阈值">
                <el-slider
                  v-model="smartSearchOptions.similarityThreshold"
                  :min="0"
                  :max="1"
                  :step="0.1"
                  show-input
                  style="width: 200px;"
                />
              </el-form-item>
              <el-form-item label="结果数量">
                <el-input-number
                  v-model="smartSearchOptions.maxResults"
                  :min="1"
                  :max="100"
                  style="width: 120px;"
                />
              </el-form-item>
            </el-form>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="resetSearch">重置</el-button>
        <el-button @click="handleClose">取消</el-button>
        <el-button 
          type="primary" 
          @click="performSearch"
          :loading="searching"
          icon="Search"
        >
          搜索
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'

interface SearchForm {
  name: string
  subject: string
  gradeLevel: string
  dateRange: [string, string] | null
  status: string
}

interface SmartSearchOptions {
  searchScope: string[]
  similarityThreshold: number
  maxResults: number
}

interface Props {
  modelValue: boolean
  subjects: string[]
  gradeLevels: string[]
  searching?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'search', params: { type: 'basic' | 'smart', data: any }): void
  (e: 'tab-change', tab: string): void
}

const props = withDefaults(defineProps<Props>(), {
  searching: false
})

const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const activeTab = ref('basic')
const smartSearchQuery = ref('')

const searchForm = ref<SearchForm>({
  name: '',
  subject: '',
  gradeLevel: '',
  dateRange: null,
  status: ''
})

const smartSearchOptions = ref<SmartSearchOptions>({
  searchScope: ['documents', 'knowledge_points', 'questions'],
  similarityThreshold: 0.7,
  maxResults: 20
})

const handleClose = () => {
  visible.value = false
}

const handleTabChange = (pane: any) => {
  emit('tab-change', pane.name)
}

const performSearch = () => {
  if (activeTab.value === 'basic') {
    emit('search', {
      type: 'basic',
      data: { ...searchForm.value }
    })
  } else {
    performSmartSearch()
  }
}

const performSmartSearch = () => {
  if (!smartSearchQuery.value.trim()) {
    return
  }
  
  emit('search', {
    type: 'smart',
    data: {
      query: smartSearchQuery.value,
      options: { ...smartSearchOptions.value }
    }
  })
}

const resetSearch = () => {
  if (activeTab.value === 'basic') {
    searchForm.value = {
      name: '',
      subject: '',
      gradeLevel: '',
      dateRange: null,
      status: ''
    }
  } else {
    smartSearchQuery.value = ''
    smartSearchOptions.value = {
      searchScope: ['documents', 'knowledge_points', 'questions'],
      similarityThreshold: 0.7,
      maxResults: 20
    }
  }
}

// 监听搜索状态变化，搜索完成后关闭对话框
watch(() => props.searching, (newVal, oldVal) => {
  if (oldVal && !newVal) {
    visible.value = false
  }
})
</script>

<style scoped>
.smart-search-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.search-input-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.search-tips {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.search-options {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
}

.dialog-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

:deep(.el-form-item) {
  margin-bottom: 16px;
}

:deep(.el-form--inline .el-form-item) {
  margin-right: 16px;
}

:deep(.el-slider) {
  margin: 0 12px;
}
</style>
