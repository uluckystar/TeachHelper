<template>
  <div class="dialog-test-page">
    <el-card class="test-header" shadow="never">
      <template #header>
        <span class="test-title">知识库系统 Dialog 组件测试页面</span>
      </template>
      
      <el-alert
        title="测试说明"
        description="本页面用于测试所有知识库相关的对话框组件是否能正常打开和运行"
        type="info"
        show-icon
        :closable="false"
      />
    </el-card>

    <el-row :gutter="20" class="test-buttons">
      <!-- 知识点管理对话框 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>知识点管理对话框</span>
          </template>
          
          <div class="button-grid">
            <el-button type="primary" @click="showAddEditKnowledgePoint = true">
              AddEditKnowledgePointDialog
            </el-button>
            <el-button type="success" @click="showAIExtract = true">
              AIExtractDialog
            </el-button>
            <el-button type="warning" @click="showOrganize = true">
              OrganizeDialog
            </el-button>
            <el-button type="info" @click="showKnowledgePointDetail = true">
              KnowledgePointDetailDialog
            </el-button>
            <el-button type="danger" @click="showRelationManagement = true">
              RelationManagementDialog
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 题目管理对话框 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>题目管理对话框</span>
          </template>
          
          <div class="button-grid">
            <el-button type="primary" @click="showAddEditQuestion = true">
              AddEditQuestionDialog
            </el-button>
            <el-button type="success" @click="showImportQuestion = true">
              ImportQuestionDialog
            </el-button>
            <el-button type="warning" @click="showQuestionPreview = true">
              QuestionPreviewDialog
            </el-button>
            <el-button type="info" @click="showPracticeMode = true">
              PracticeModeDialog
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 状态显示 -->
    <el-card class="test-status" shadow="never">
      <template #header>
        <span>测试状态</span>
      </template>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <div class="status-item">
            <span class="status-label">已测试的对话框:</span>
            <el-tag v-for="dialog in testedDialogs" :key="dialog" type="success" class="status-tag">
              {{ dialog }}
            </el-tag>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="status-item">
            <span class="status-label">测试进度:</span>
            <el-progress :percentage="testProgress" :stroke-width="8" />
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 对话框组件 -->
    <AddEditKnowledgePointDialog
      v-model="showAddEditKnowledgePoint"
      :knowledge-base-id="1"
      @saved="onDialogTested('AddEditKnowledgePoint')"
    />

    <AIExtractDialog
      v-model="showAIExtract"
      :knowledge-base-id="1"
      @extracted="onDialogTested('AIExtract')"
    />

    <OrganizeDialog
      v-model="showOrganize"
      :knowledge-base-id="1"
      :knowledge-points="[]"
      @organized="onDialogTested('Organize')"
    />

    <KnowledgePointDetailDialog
      v-model="showKnowledgePointDetail"
      :knowledge-point="{ id: 1, name: 'Test Point', content: '', knowledge_base_id: 1 }"
      @updated="onDialogTested('KnowledgePointDetail')"
    />

    <RelationManagementDialog
      v-model="showRelationManagement"
      :knowledge-point="{ id: 1, name: 'Test Point', content: '', knowledge_base_id: 1 }"
      :all-points="[]"
      @updated="onDialogTested('RelationManagement')"
    />

    <AddEditQuestionDialog
      v-model="showAddEditQuestion"
      :knowledge-base-id="1"
      @saved="onDialogTested('AddEditQuestion')"
    />

    <ImportQuestionDialog
      v-model="showImportQuestion"
      :knowledge-base-id="1"
      @imported="onDialogTested('ImportQuestion')"
    />

    <QuestionPreviewDialog
      v-model="showQuestionPreview"
      :question="{ id: 1, content: 'Test Question', type: 'single_choice', difficulty: 'medium' }"
      @closed="onDialogTested('QuestionPreview')"
    />

    <PracticeModeDialog
      v-model="showPracticeMode"
      :questions="mockQuestions"
      @practice-completed="onDialogTested('PracticeMode')"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

// 导入所有对话框组件
import AddEditKnowledgePointDialog from '@/components/knowledge/AddEditKnowledgePointDialog.vue'
import AIExtractDialog from '@/components/knowledge/AIExtractDialog.vue'
import OrganizeDialog from '@/components/knowledge/OrganizeDialog.vue'
import KnowledgePointDetailDialog from '@/components/knowledge/KnowledgePointDetailDialog.vue'
import RelationManagementDialog from '@/components/knowledge/RelationManagementDialog.vue'
import AddEditQuestionDialog from '@/components/knowledge/AddEditQuestionDialog.vue'
import ImportQuestionDialog from '@/components/knowledge/ImportQuestionDialog.vue'
import QuestionPreviewDialog from '@/components/knowledge/QuestionPreviewDialog.vue'
import PracticeModeDialog from '@/components/knowledge/PracticeModeDialog.vue'

// 对话框显示状态
const showAddEditKnowledgePoint = ref(false)
const showAIExtract = ref(false)
const showOrganize = ref(false)
const showKnowledgePointDetail = ref(false)
const showRelationManagement = ref(false)
const showAddEditQuestion = ref(false)
const showImportQuestion = ref(false)
const showQuestionPreview = ref(false)
const showPracticeMode = ref(false)

// 测试状态
const testedDialogs = ref<string[]>([])
const totalDialogs = 9

// 模拟题目数据
const mockQuestions = ref([
  {
    id: 1,
    type: 'choice',
    difficulty: 'easy',
    content: '测试选择题',
    options: [
      { content: '选项A', isCorrect: true },
      { content: '选项B', isCorrect: false },
      { content: '选项C', isCorrect: false },
      { content: '选项D', isCorrect: false }
    ]
  },
  {
    id: 2,
    type: 'blank',
    difficulty: 'medium',
    content: '测试填空题：2+2=____',
    blanks: [{ answer: '4' }]
  }
])

// 计算属性
const testProgress = computed(() => {
  return Math.round((testedDialogs.value.length / totalDialogs) * 100)
})

// 方法
const onDialogTested = (dialogName: string) => {
  if (!testedDialogs.value.includes(dialogName)) {
    testedDialogs.value.push(dialogName)
    ElMessage.success(`${dialogName} 对话框测试成功！`)
  }
  
  if (testedDialogs.value.length === totalDialogs) {
    ElMessage.success('🎉 所有对话框组件测试完成！')
  }
}
</script>

<style scoped>
.dialog-test-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.test-header {
  margin-bottom: 20px;
}

.test-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.test-buttons {
  margin-bottom: 20px;
}

.button-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.button-grid .el-button {
  width: 100%;
  height: 40px;
}

.test-status {
  margin-top: 20px;
}

.status-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.status-label {
  font-weight: 600;
  color: #606266;
}

.status-tag {
  margin-right: 8px;
  margin-bottom: 4px;
}

.el-card {
  margin-bottom: 16px;
}

.el-card:hover {
  box-shadow: 0 4px 20px rgba(64, 158, 255, 0.15);
}
</style>
