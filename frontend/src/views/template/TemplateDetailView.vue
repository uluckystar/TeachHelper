<template>
  <div class="template-detail-view">
    <div class="page-header">
      <el-breadcrumb>
        <el-breadcrumb-item :to="{ path: '/templates' }">模板管理</el-breadcrumb-item>
        <el-breadcrumb-item>模板详情</el-breadcrumb-item>
      </el-breadcrumb>
      
      <div class="header-actions">
        <el-button 
          v-if="template?.templateType === 'DOCUMENT_EXTRACTED'"
          type="warning" 
          @click="showReimportDialog = true"
        >
          <el-icon><Refresh /></el-icon>
          重新导入
        </el-button>
        <el-button type="primary" @click="applyTemplate" :disabled="!template?.isUsable">
          应用到考试
        </el-button>
        <el-button type="success" @click="applyToExistingExam" :disabled="!template?.isUsable">
          应用到已有考试
        </el-button>
        <el-button @click="showEditDialog = true">编辑模板</el-button>
        <el-button @click="goBack">返回</el-button>
      </div>
    </div>

    <div v-loading="loading">
      <div v-if="template" class="template-content">
        <!-- 模板基本信息 -->
        <el-card class="template-info-card">
          <template #header>
            <div class="card-header">
              <h2>{{ template.name }}</h2>
              <div class="template-tags">
                <el-tag :type="getTemplateTypeTag(template.templateType) as any" size="small">
                  {{ getTemplateTypeText(template.templateType) }}
                </el-tag>
                <el-tag :type="getStatusTag(template.status) as any" size="small">
                  {{ getStatusText(template.status) }}
                </el-tag>
                <el-tag v-if="template.isPublic" type="success" size="small">公开</el-tag>
                <el-button 
                  v-if="template.status === 'DRAFT' && template.isComplete"
                  type="primary" 
                  size="small" 
                  @click="markTemplateReady"
                  :loading="updatingStatus"
                  style="margin-left: 8px;"
                >
                  标记为就绪
                </el-button>
                <el-button 
                  v-if="template.status === 'READY'"
                  type="success" 
                  size="small" 
                  @click="markTemplatePublished"
                  :loading="updatingStatus"
                  style="margin-left: 8px;"
                >
                  公开模板
                </el-button>
                <el-button 
                  v-if="template.status === 'PUBLISHED'"
                  type="warning" 
                  size="small" 
                  @click="cancelTemplatePublished"
                  :loading="updatingStatus"
                  style="margin-left: 8px;"
                >
                  取消公开
                </el-button>
              </div>
            </div>
          </template>
          
          <el-descriptions :column="3" border>
            <el-descriptions-item label="科目">{{ template.subject }}</el-descriptions-item>
            <el-descriptions-item label="年级">{{ template.gradeLevel }}</el-descriptions-item>
            <el-descriptions-item label="总分">{{ template.totalScore }} 分</el-descriptions-item>
            <el-descriptions-item label="考试时长">{{ template.duration }} 分钟</el-descriptions-item>
            <el-descriptions-item label="题目数量">{{ template.questionCount }} 题</el-descriptions-item>
            <el-descriptions-item label="使用次数">{{ template.usageCount }} 次</el-descriptions-item>
            <el-descriptions-item label="创建者">{{ template.creatorName }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDate(template.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="最后使用">{{ template.lastUsedAt ? formatDate(template.lastUsedAt) : '未使用' }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="3">{{ template.description || '暂无描述' }}</el-descriptions-item>
          </el-descriptions>

          <!-- 状态提示 -->
          <el-alert
            v-if="template.status === 'DRAFT'"
            type="warning"
            :closable="false"
            style="margin-top: 16px;"
          >
            <template #title>
              模板状态说明
            </template>
            当前模板状态为"草稿"。请确认题目内容无误后，点击"标记为就绪"按钮将模板状态变更为就绪状态，然后即可应用到考试。
          </el-alert>

          <el-alert
            v-if="template.status === 'READY'"
            type="success"
            :closable="false"
            style="margin-top: 16px;"
          >
            <template #title>
              模板已就绪
            </template>
            模板状态为"就绪"，可以应用到考试。如需公开此模板供其他用户使用，可点击"公开模板"按钮。
          </el-alert>

          <el-alert
            v-if="template.status === 'PUBLISHED'"
            type="info"
            :closable="false"
            style="margin-top: 16px;"
          >
            <template #title>
              模板已公开
            </template>
            模板状态为"公开"，所有用户都可以使用此模板。
          </el-alert>

          <!-- 学习通导入提示 -->
          <el-alert
            v-if="template.templateType === 'DOCUMENT_EXTRACTED'"
            type="info"
            :closable="false"
            style="margin-top: 16px;"
          >
            <template #title>
              <el-icon><Refresh /></el-icon>
              学习通文档导入
            </template>
            <div>
              此模板是从学习通文档导入生成的。如果原始文档有更新，您可以点击页面顶部的"重新导入"按钮，上传新的文档来更新模板内容。
              <br>
              <strong>注意：</strong>重新导入将清空当前所有题目，请谨慎操作。
            </div>
          </el-alert>
        </el-card>

        <!-- 题目列表 -->
        <el-card class="questions-card">
          <template #header>
            <div class="card-header">
              <h3>题目列表 ({{ template.questions?.length || 0 }} 题)</h3>
              <div class="header-actions">
                <el-button 
                  v-if="template.status === 'DRAFT' && hasDraftQuestions"
                  type="success" 
                  size="small" 
                  @click="markAllDraftQuestionsReady"
                  :loading="updatingQuestionStatus"
                  :disabled="!selectedQuestions.length"
                >
                  <el-icon><Check /></el-icon>
                  批量标记选中题目为就绪 ({{ selectedQuestions.length }})
                </el-button>
                <el-button 
                  type="danger" 
                  size="small" 
                  @click="showBatchDeleteDialog"
                  :disabled="!selectedQuestions.length"
                >
                  <el-icon><Delete /></el-icon>
                  批量删除 ({{ selectedQuestions.length }})
                </el-button>
                <el-button type="primary" size="small" @click="addQuestion">
                  <el-icon><Plus /></el-icon>
                  添加题目
                </el-button>
                <el-button type="success" size="small" @click="addFromQuestionBank">
                  <el-icon><Collection /></el-icon>
                  从题库选择
                </el-button>
              </div>
            </div>
          </template>

          <div v-if="!template.questions || template.questions.length === 0" class="empty-state">
            <el-empty description="暂无题目">
              <el-button type="primary" @click="addQuestion">添加第一个题目</el-button>
            </el-empty>
          </div>

          <div v-else class="questions-list">
            <!-- 全选操作栏 -->
            <div class="select-all-bar">
              <el-checkbox 
                :model-value="isAllSelected"
                :indeterminate="isIndeterminate"
                @update:model-value="toggleSelectAll"
              >
                全选
              </el-checkbox>
              <div class="select-actions">
                <el-button 
                  v-if="selectedQuestions.length > 0"
                  type="text" 
                  size="small" 
                  @click="clearSelection"
                >
                  取消全选
                </el-button>
                <span v-if="selectedQuestions.length > 0" class="selected-count">
                  已选择 {{ selectedQuestions.length }} 道题目
                </span>
              </div>
            </div>

            <div v-for="(question, index) in template.questions" :key="question.id" class="question-item">
              <div class="question-header">
                <div class="question-info">
                  <el-checkbox 
                    :model-value="selectedQuestions.includes(question.id)"
                    @update:model-value="(val) => toggleQuestionSelection(question.id, val)"
                  />
                  <span class="question-order">第{{ question.questionOrder }}题</span>
                  <el-tag :type="getQuestionTypeTag(question.questionType) as any" size="small">
                    {{ getQuestionTypeText(question.questionType) }}
                  </el-tag>
                  <span class="question-score">{{ Number(question.score) }} 分</span>
                  <el-tag :type="getQuestionStatusTag(question.status) as any" size="small">
                    {{ getQuestionStatusText(question.status) }}
                  </el-tag>
                </div>
                <div class="question-actions">
                  <el-button type="primary" size="small" @click="editQuestion(question)">
                    <el-icon><Edit /></el-icon>
                    编辑
                  </el-button>
                  <el-button type="danger" size="small" @click="deleteQuestion(question)">
                    <el-icon><Delete /></el-icon>
                    删除
                  </el-button>
                </div>
              </div>
              <div class="question-content">
                <div v-if="question.questionContent" class="content-text">
                  <div class="question-text">
                    {{ question.questionContent }}
                  </div>
                  
                  <!-- 选择题选项展示 -->
                  <div v-if="hasOptions(question)" class="question-options">
                    <div class="options-title">选项：</div>
                    <div class="options-list">
                      <div 
                        v-for="(option, optionIndex) in getQuestionOptions(question)"
                        :key="optionIndex"
                        class="option-item"
                        :class="{ 'correct-option': isCorrectOption(question, option) }"
                      >
                        <span class="option-label">{{ option }}</span>
                        <el-tag v-if="isCorrectOption(question, option)" type="success" size="small">
                          正确答案
                        </el-tag>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 参考答案展示 -->
                  <div v-if="getCorrectAnswer(question)" class="correct-answer">
                    <div class="answer-title">参考答案：</div>
                    <div class="answer-content">
                      <el-tag type="success" size="small">{{ getCorrectAnswer(question) }}</el-tag>
                    </div>
                  </div>
                </div>
                <div v-else class="content-placeholder">
                  <el-text type="info">题目内容待配置</el-text>
                </div>
              </div>
              <div v-if="question.referencedQuestion" class="referenced-info">
                <el-text type="success" size="small">
                  <el-icon><Link /></el-icon>
                  引用题库题目：{{ question.referencedQuestion.content?.substring(0, 50) }}...
                </el-text>
              </div>
            </div>
          </div>
        </el-card>
      </div>
      
      <div v-else class="loading-placeholder">
        <el-skeleton :rows="4" animated />
      </div>
    </div>

    <!-- 编辑模板对话框 -->
    <TemplateEditDialog
      v-model="showEditDialog"
      :template="template"
      @saved="handleTemplateSaved"
    />

    <!-- 批量删除确认对话框 -->
    <el-dialog
      v-model="showBatchDeleteConfirm"
      title="批量删除题目"
      width="400px"
    >
      <div class="batch-delete-content">
        <el-icon class="warning-icon" color="#E6A23C"><Warning /></el-icon>
        <p>确定要删除选中的 {{ selectedQuestions.length }} 道题目吗？</p>
        <p class="warning-text">此操作不可恢复，请谨慎操作。</p>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showBatchDeleteConfirm = false">取消</el-button>
          <el-button type="danger" @click="confirmBatchDelete" :loading="deletingQuestions">
            确认删除
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 应用模板对话框 -->
    <el-dialog v-model="applyDialogVisible" title="应用模板创建考试" width="500px">
      <el-form :model="applyForm" ref="applyFormRef" :rules="applyRules" label-width="100px">
        <el-form-item label="模板名称">
          <el-input :value="template?.name" disabled />
        </el-form-item>
        <el-form-item label="考试标题" prop="examTitle">
          <el-input v-model="applyForm.examTitle" placeholder="请输入考试标题" />
        </el-form-item>
        <el-form-item label="考试描述" prop="examDescription">
          <el-input 
            v-model="applyForm.examDescription" 
            type="textarea" 
            :rows="3"
            placeholder="请输入考试描述（可选）" 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmApply" :loading="applying">确认应用</el-button>
      </template>
    </el-dialog>

    <!-- 应用模板到已有考试对话框 -->
    <el-dialog v-model="applyToExistingDialogVisible" title="应用模板到已有考试" width="600px">
      <el-form :model="applyToExistingForm" ref="applyToExistingFormRef" :rules="applyToExistingRules" label-width="120px">
        <el-form-item label="模板名称">
          <el-input :value="template?.name" disabled />
        </el-form-item>
        <el-form-item label="选择考试" prop="examId">
          <el-select 
            v-model="applyToExistingForm.examId" 
            placeholder="请选择要应用模板的考试"
            style="width: 100%"
            :loading="loadingExams"
            filterable
          >
            <el-option
              v-for="exam in applicableExams"
              :key="exam.id"
              :label="`${exam.title} (${getExamStatusText(exam.status)})`"
              :value="exam.id"
            >
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span>{{ exam.title }}</span>
                <div>
                  <el-tag :type="getExamStatusTag(exam.status)" size="small">
                    {{ getExamStatusText(exam.status) }}
                  </el-tag>
                  <span style="margin-left: 8px; color: #909399; font-size: 12px;">
                    {{ exam.questionCount }}题
                  </span>
                </div>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="应用方式" prop="replaceExisting">
          <el-radio-group v-model="applyToExistingForm.replaceExisting">
            <el-radio :label="false">追加题目</el-radio>
            <el-radio :label="true">替换现有题目</el-radio>
          </el-radio-group>
          <div class="form-help">
            <el-text type="info" size="small">
              <template v-if="!applyToExistingForm.replaceExisting">
                将模板题目追加到现有题目后面
              </template>
              <template v-else>
                清空现有题目，完全使用模板题目（此操作不可恢复）
              </template>
            </el-text>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyToExistingDialogVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="confirmApplyToExisting" 
          :loading="applyingToExisting"
          :disabled="!applyToExistingForm.examId"
        >
          确认应用
        </el-button>
      </template>
    </el-dialog>

    <!-- 题目编辑对话框 -->
    <el-dialog v-model="questionDialogVisible" :title="isEditingQuestion ? '编辑题目' : '添加题目'" width="800px">
      <el-form :model="questionForm" ref="questionFormRef" :rules="questionRules" label-width="100px">
        <el-form-item label="题目序号" prop="questionOrder">
          <el-input-number v-model="questionForm.questionOrder" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="题目类型" prop="questionType">
          <el-select v-model="questionForm.questionType" placeholder="请选择题目类型" style="width: 100%">
            <el-option label="单选题" value="SINGLE_CHOICE" />
            <el-option label="多选题" value="MULTIPLE_CHOICE" />
            <el-option label="判断题" value="TRUE_FALSE" />
            <el-option label="填空题" value="FILL_BLANK" />
            <el-option label="简答题" value="SHORT_ANSWER" />
            <el-option label="论述题" value="ESSAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目分数" prop="score">
          <el-input-number v-model="questionForm.score" :min="0" :max="100" :precision="1" />
        </el-form-item>
        <el-form-item label="题目内容" prop="questionContent">
          <el-input 
            v-model="questionForm.questionContent" 
            type="textarea" 
            :rows="4"
            placeholder="请输入题目内容" 
          />
        </el-form-item>
        
        <!-- 选择题和判断题的选项配置 -->
        <div v-if="needsOptions(questionForm.questionType)" class="options-config">
          <el-form-item label="题目选项">
            <div class="options-manager">
              <div 
                v-for="(option, index) in questionOptions" 
                :key="index"
                class="option-input-row"
              >
                <span class="option-prefix">{{ getOptionPrefix(index) }}</span>
                <el-input 
                  v-model="questionOptions[index]" 
                  placeholder="请输入选项内容"
                  style="flex: 1; margin: 0 8px;"
                />
                <el-radio 
                  v-model="correctOptionIndex" 
                  :label="index"
                  style="margin-right: 8px;"
                >
                  正确
                </el-radio>
                <el-button 
                  type="danger" 
                  size="small" 
                  :icon="Delete"
                  @click="removeOption(index)"
                  :disabled="questionOptions.length <= 2"
                />
              </div>
              <el-button 
                type="primary" 
                size="small" 
                :icon="Plus"
                @click="addOption"
                :disabled="questionOptions.length >= 6"
                style="margin-top: 8px;"
              >
                添加选项
              </el-button>
            </div>
          </el-form-item>
        </div>
        
        <!-- 非选择题的参考答案 -->
        <el-form-item v-else label="参考答案">
          <el-input 
            v-model="questionForm.referenceAnswer" 
            type="textarea" 
            :rows="2"
            placeholder="请输入参考答案（可选）" 
          />
        </el-form-item>
        
        <el-form-item label="难度等级">
          <el-select v-model="questionForm.difficultyLevel" placeholder="请选择难度等级" style="width: 100%">
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识标签">
          <el-input v-model="questionForm.knowledgeTags" placeholder="请输入知识标签，用逗号分隔" />
        </el-form-item>
        <el-form-item label="是否必答">
          <el-switch v-model="questionForm.isRequired" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="questionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveQuestion" :loading="savingQuestion">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 从题库选择题目对话框 -->
    <el-dialog v-model="questionBankDialogVisible" title="从题库选择题目" width="1200px">
      <div class="question-bank-search">
        <el-form :model="questionBankSearchForm" inline>
          <el-form-item label="关键词">
            <el-input v-model="questionBankSearchForm.keyword" placeholder="搜索题目内容" style="width: 200px" />
          </el-form-item>
          <el-form-item label="题目类型">
            <el-select v-model="questionBankSearchForm.questionType" placeholder="全部类型" clearable style="width: 150px">
              <el-option label="单选题" value="SINGLE_CHOICE" />
              <el-option label="多选题" value="MULTIPLE_CHOICE" />
              <el-option label="判断题" value="TRUE_FALSE" />
              <el-option label="填空题" value="FILL_BLANK" />
              <el-option label="简答题" value="SHORT_ANSWER" />
              <el-option label="论述题" value="ESSAY" />
            </el-select>
          </el-form-item>
          <el-form-item label="来源类型">
            <el-select v-model="questionBankSearchForm.sourceType" placeholder="全部来源" clearable style="width: 150px">
              <el-option label="手动创建" value="SELF_CREATED" />
              <el-option label="网络获取" value="INTERNET" />
              <el-option label="AI生成" value="AI_GENERATED" />
              <el-option label="AI整理" value="AI_ORGANIZED" />
              <el-option label="模板解析" value="TEMPLATE_CONFIRM" />
              <el-option label="学习通导入" value="LEARNING_IMPORT" />
              <el-option label="题库导入" value="QUESTION_BANK" />
            </el-select>
          </el-form-item>
          <el-form-item label="创建时间">
            <el-date-picker
              v-model="questionBankSearchForm.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="width: 240px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="searchQuestionBank" :loading="searchingQuestionBank">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
            <el-button @click="resetQuestionBankSearch">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table 
        :data="questionBankQuestions" 
        v-loading="searchingQuestionBank"
        @selection-change="handleQuestionBankSelection"
        style="width: 100%"
        height="400"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="content" label="题目内容" min-width="300">
          <template #default="{ row }">
            <div class="question-content-cell">
              {{ row.content?.substring(0, 100) }}{{ row.content?.length > 100 ? '...' : '' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="questionType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getQuestionTypeTag(row.questionType) as any" size="small">
              {{ getQuestionTypeText(row.questionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maxScore" label="分数" width="80">
          <template #default="{ row }">
            {{ row.maxScore }}
          </template>
        </el-table-column>
        <el-table-column prop="difficulty" label="难度" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.difficulty" :type="getDifficultyTag(row.difficulty) as any" size="small">
              {{ getDifficultyText(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sourceType" label="来源" width="100">
          <template #default="{ row }">
            <el-tag :type="getSourceTypeTag(row.sourceType) as any" size="small">
              {{ getSourceTypeText(row.sourceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="120">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="questionBankPagination.currentPage"
          v-model:page-size="questionBankPagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="questionBankPagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handlePageSizeChange"
          @current-change="handleCurrentPageChange"
        />
      </div>

      <template #footer>
        <div class="dialog-footer">
          <div class="selection-info">
            已选择 {{ selectedQuestionBankQuestions.length }} 道题目
          </div>
          <div class="footer-actions">
            <el-button @click="questionBankDialogVisible = false">取消</el-button>
            <el-button 
              type="primary" 
              @click="addSelectedQuestions" 
              :loading="addingQuestions"
              :disabled="selectedQuestionBankQuestions.length === 0"
            >
              添加选中题目 ({{ selectedQuestionBankQuestions.length }})
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 重新导入对话框 -->
    <ReimportDialog
      v-model="showReimportDialog"
      :template-id="template?.id"
      :template-name="template?.name"
      :template-subject="template?.subject"
      :template-grade-level="template?.gradeLevel"
      @reimported="loadTemplate"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { examPaperTemplateApi, type ExamPaperTemplate } from '@/api/examPaperTemplate'
import { Plus, Collection, Edit, Delete, Link, Search, Refresh, Check, Warning } from '@element-plus/icons-vue'
import { questionApi } from '@/api/question'
import { getQuestionTypeText, getQuestionTypeTag, getSourceTypeText, getSourceTypeTag } from '@/utils/tagTypes'
import TemplateEditDialog from '@/components/template/TemplateEditDialog.vue'
import ReimportDialog from '@/components/template/ReimportDialog.vue'

const route = useRoute()
const router = useRouter()
const template = ref<ExamPaperTemplate | null>(null)
const loading = ref(false)
const applying = ref(false)
const updatingStatus = ref(false)
const updatingQuestionStatus = ref(false)

// 计算属性 - 检查是否有可以标记为就绪的题目（DRAFT或CONFIGURED状态）
const hasDraftQuestions = computed(() => {
  return template.value?.questions?.some(q => 
    (q.status === 'DRAFT' || q.status === 'CONFIGURED') && q.isConfigured && q.questionContent
  ) || false
})

// 应用模板相关
const applyDialogVisible = ref(false)
const applyFormRef = ref<FormInstance>()
const applyForm = reactive({
  examTitle: '',
  examDescription: ''
})

const applyRules: FormRules = {
  examTitle: [
    { required: true, message: '请输入考试标题', trigger: 'blur' },
    { min: 2, max: 200, message: '考试标题长度在2-200个字符', trigger: 'blur' }
  ]
}

// 应用模板到已有考试相关
const applyToExistingDialogVisible = ref(false)
const applyToExistingFormRef = ref<FormInstance>()
const applyToExistingForm = reactive({
  examId: undefined as number | undefined,
  replaceExisting: false
})
const applyingToExisting = ref(false)
const loadingExams = ref(false)
const applicableExams = ref<any[]>([])

const applyToExistingRules: FormRules = {
  examId: [
    { required: true, message: '请选择要应用模板的考试', trigger: 'change' }
  ]
}

// 题目管理相关
const questionDialogVisible = ref(false)
const questionFormRef = ref<FormInstance>()
const isEditingQuestion = ref(false)
const currentQuestionId = ref<number | null>(null)
const savingQuestion = ref(false)

const questionForm = reactive({
  questionOrder: 1,
  questionType: 'SINGLE_CHOICE',
  questionContent: '',
  score: 10,
  difficultyLevel: '',
  knowledgeTags: '',
  isRequired: true,
  referenceAnswer: ''
})

// 题目选项管理
const questionOptions = ref<string[]>(['', ''])
const correctOptionIndex = ref(0)

const questionRules: FormRules = {
  questionOrder: [
    { required: true, message: '请输入题目序号', trigger: 'blur' },
    { type: 'number', min: 1, message: '题目序号必须大于0', trigger: 'blur' }
  ],
  questionType: [
    { required: true, message: '请选择题目类型', trigger: 'change' }
  ],
  questionContent: [
    { required: true, message: '请输入题目内容', trigger: 'blur' }
  ],
  score: [
    { required: true, message: '请输入题目分数', trigger: 'blur' },
    { type: 'number', min: 0, message: '分数不能小于0', trigger: 'blur' }
  ]
}

// 题库选择相关
const questionBankDialogVisible = ref(false)
const questionBankSearchForm = reactive({
  keyword: '',
  questionType: '',
  sourceType: '',
  dateRange: []
})
const questionBankQuestions = ref<any[]>([])
const selectedQuestionBankQuestions = ref<any[]>([])
const searchingQuestionBank = ref(false)
const addingQuestions = ref(false)
const questionBankPagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 批量删除相关
const selectedQuestions = ref<number[]>([])
const showBatchDeleteConfirm = ref(false)
const deletingQuestions = ref(false)

// 全选相关计算属性
const isAllSelected = computed(() => {
  if (!template.value?.questions || template.value.questions.length === 0) {
    return false
  }
  return selectedQuestions.value.length === template.value.questions.length
})

const isIndeterminate = computed(() => {
  if (!template.value?.questions || template.value.questions.length === 0) {
    return false
  }
  return selectedQuestions.value.length > 0 && selectedQuestions.value.length < template.value.questions.length
})

// 全选相关方法
const toggleSelectAll = (selected: any) => {
  const isSelected = Boolean(selected)
  if (isSelected) {
    // 全选所有题目
    if (template.value?.questions) {
      selectedQuestions.value = template.value.questions.map(q => q.id)
    }
  } else {
    // 取消全选
    selectedQuestions.value = []
  }
}

const clearSelection = () => {
  selectedQuestions.value = []
}

const getTemplateTypeText = (type: string) => {
  const map: Record<string, string> = {
    'MANUAL': '手动创建',
    'AI_GENERATED': 'AI生成',
    'DOCUMENT_EXTRACTED': '文档提取',
    'COPIED': '复制'
  }
  return map[type] || type
}

const getTemplateTypeTag = (type: string): string => {
  const typeMap: Record<string, string> = {
    'MANUAL': 'primary',
    'AI_GENERATED': 'success',
    'DOCUMENT_EXTRACTED': 'warning',
    'COPIED': 'info'
  }
  return typeMap[type] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'DRAFT': '草稿',
    'READY': '就绪',
    'PUBLISHED': '公开',
    'ARCHIVED': '已归档'
  }
  return map[status] || status
}

const getStatusTag = (status: string): string => {
  const statusMap: Record<string, string> = {
    'DRAFT': 'info',
    'READY': 'success',
    'PUBLISHED': 'primary',
    'ARCHIVED': 'warning'
  }
  return statusMap[status] || 'info'
}

const getQuestionStatusText = (status: string) => {
  const map: Record<string, string> = {
    'DRAFT': '草稿',
    'CONFIGURED': '已配置',
    'READY': '就绪'
  }
  return map[status] || status
}

const getQuestionStatusTag = (status: string): string => {
  const statusMap: Record<string, string> = {
    'DRAFT': 'info',
    'CONFIGURED': 'warning',
    'READY': 'success'
  }
  return statusMap[status] || 'info'
}

const formatDate = (dateString: string) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

const getExamStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'DRAFT': '草稿',
    'PUBLISHED': '已发布',
    'IN_PROGRESS': '进行中',
    'ENDED': '已结束',
    'EVALUATED': '已评估'
  }
  return statusMap[status] || status
}

const getExamStatusTag = (status: string): 'success' | 'primary' | 'warning' | 'info' | 'danger' => {
  const statusMap: Record<string, 'success' | 'primary' | 'warning' | 'info' | 'danger'> = {
    'DRAFT': 'info',
    'PUBLISHED': 'success',
    'IN_PROGRESS': 'warning',
    'ENDED': 'warning',
    'EVALUATED': 'primary'
  }
  return statusMap[status] || 'info'
}

const getDifficultyText = (difficulty: string) => {
  const map: Record<string, string> = {
    'EASY': '简单',
    'MEDIUM': '中等',
    'HARD': '困难'
  }
  return map[difficulty] || difficulty
}

const getDifficultyTag = (difficulty: string): string => {
  const map: Record<string, string> = {
    'EASY': 'success',
    'MEDIUM': 'warning',
    'HARD': 'danger'
  }
  return map[difficulty] || 'info'
}

const loadTemplate = async () => {
  const id = route.params.id
  if (!id) return
  
  try {
    loading.value = true
    const res = await examPaperTemplateApi.getTemplate(Number(id))
    template.value = res.data
  } catch (error) {
    console.error('加载模板失败:', error)
    ElMessage.error('加载模板失败')
  } finally {
    loading.value = false
  }
}

const editTemplate = () => {
  if (template.value) {
    router.push(`/templates/${template.value.id}/edit`)
  }
}

const applyTemplate = () => {
  if (template.value) {
    applyForm.examTitle = `${template.value.name} - ${formatDate(new Date().toISOString())}`
    applyForm.examDescription = template.value.description || ''
    applyDialogVisible.value = true
  }
}

const confirmApply = async () => {
  if (!applyFormRef.value || !template.value) return
  
  try {
    const valid = await applyFormRef.value.validate()
    if (!valid) return
  } catch {
    return
  }

  applying.value = true
  try {
    const response = await examPaperTemplateApi.applyTemplateToExam(
      template.value.id,
      applyForm.examTitle,
      applyForm.examDescription
    )
    const exam = response.data
    ElMessage.success('考试创建成功')
    applyDialogVisible.value = false
    resetApplyForm()
    router.push(`/exams/${exam.id}`)
  } catch (error) {
    console.error('应用模板失败:', error)
    ElMessage.error('应用模板失败')
  } finally {
    applying.value = false
  }
}

const resetApplyForm = () => {
  applyForm.examTitle = ''
  applyForm.examDescription = ''
  applyFormRef.value?.resetFields()
}

const goBack = () => {
  router.back()
}

const applyToExistingExam = async () => {
  if (template.value) {
    loadingExams.value = true
    try {
      const response = await examPaperTemplateApi.getApplicableExams()
      applicableExams.value = response.data
      applyToExistingForm.examId = undefined
      applyToExistingForm.replaceExisting = false
      applyToExistingDialogVisible.value = true
    } catch (error) {
      console.error('加载可应用考试失败:', error)
      ElMessage.error('加载可应用考试失败')
    } finally {
      loadingExams.value = false
    }
  }
}

const confirmApplyToExisting = async () => {
  if (!applyToExistingFormRef.value || !template.value || !applyToExistingForm.examId) return
  
  try {
    const valid = await applyToExistingFormRef.value.validate()
    if (!valid) return
  } catch {
    return
  }

  // 如果是替换模式，需要确认
  if (applyToExistingForm.replaceExisting) {
    try {
      await ElMessageBox.confirm(
        '确定要替换现有题目吗？此操作将清空当前考试的所有题目，不可恢复！',
        '确认替换',
        {
          confirmButtonText: '确定替换',
          cancelButtonText: '取消',
          type: 'warning',
        }
      )
    } catch {
      return
    }
  }

  applyingToExisting.value = true
  try {
    const response = await examPaperTemplateApi.applyTemplateToExistingExam(
      template.value.id,
      applyToExistingForm.examId,
      applyToExistingForm.replaceExisting
    )
    const exam = response.data
    ElMessage.success('模板应用成功')
    applyToExistingDialogVisible.value = false
    resetApplyToExistingForm()
    router.push(`/exams/${exam.id}`)
  } catch (error) {
    console.error('应用模板到已有考试失败:', error)
    ElMessage.error('应用模板失败')
  } finally {
    applyingToExisting.value = false
  }
}

const resetApplyToExistingForm = () => {
  applyToExistingForm.examId = undefined
  applyToExistingForm.replaceExisting = false
  applyToExistingFormRef.value?.resetFields()
}

// 题目管理方法
const addQuestion = () => {
  isEditingQuestion.value = false
  currentQuestionId.value = null
  resetQuestionForm()
  questionForm.questionOrder = (template.value?.questions?.length || 0) + 1
  questionDialogVisible.value = true
}

const editQuestion = (question: any) => {
  isEditingQuestion.value = true
  currentQuestionId.value = question.id
  questionForm.questionOrder = question.questionOrder
  questionForm.questionType = question.questionType
  questionForm.questionContent = question.questionContent || ''
  questionForm.score = question.score || 10
  questionForm.difficultyLevel = question.difficultyLevel || ''
  questionForm.knowledgeTags = question.knowledgeTags || ''
  questionForm.isRequired = question.isRequired !== false
  questionForm.referenceAnswer = ''
  
  // 初始化选项配置
  initializeQuestionOptions(question)
  
  questionDialogVisible.value = true
}

const deleteQuestion = async (question: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除第${question.questionOrder}题吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await examPaperTemplateApi.deleteTemplateQuestion(template.value!.id, question.id)
    ElMessage.success('题目删除成功')
    loadTemplate()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除题目失败:', error)
      
      // 处理不同类型的错误
      if (error.response?.status === 400) {
        const errorMessage = error.response?.data?.message || error.response?.data
        if (errorMessage && errorMessage.includes('无权限')) {
          ElMessage.error('您没有权限修改此模板，请确认您是该模板的创建者')
        } else if (errorMessage) {
          ElMessage.error(`删除失败：${errorMessage}`)
        } else {
          ElMessage.error('删除题目失败，请检查操作权限')
        }
      } else if (error.response?.status === 403) {
        ElMessage.error('权限不足，无法删除此题目')
      } else if (error.response?.status === 404) {
        ElMessage.error('题目不存在或已被删除')
      } else {
        ElMessage.error('删除题目失败')
      }
    }
  }
}

const saveQuestion = async () => {
  if (!questionFormRef.value || !template.value) return
  
  try {
    const valid = await questionFormRef.value.validate()
    if (!valid) return
  } catch {
    return
  }

  savingQuestion.value = true
  try {
    // 构建题目配置
    const questionData: any = { ...questionForm }
    
    if (needsOptions(questionForm.questionType)) {
      // 有选项的题目类型，构建配置JSON
      const options = questionOptions.value
        .filter(option => option.trim())
        .map((option, index) => getOptionPrefix(index) + option.trim())
      
      const correctAnswer = getOptionPrefix(correctOptionIndex.value).replace('、', '')
      
      questionData.questionConfig = JSON.stringify({
        correctAnswer,
        options
      })
    } else {
      // 非选择题，使用参考答案
      if (questionForm.referenceAnswer) {
        questionData.questionConfig = JSON.stringify({
          correctAnswer: questionForm.referenceAnswer
        })
      }
    }
    
    if (isEditingQuestion.value && currentQuestionId.value) {
      await examPaperTemplateApi.updateTemplateQuestion(
        template.value.id,
        currentQuestionId.value,
        questionData
      )
      ElMessage.success('题目更新成功')
    } else {
      await examPaperTemplateApi.addTemplateQuestion(template.value.id, questionData)
      ElMessage.success('题目添加成功')
    }
    
    questionDialogVisible.value = false
    loadTemplate()
  } catch (error: any) {
    console.error('保存题目失败:', error)
    
    // 处理不同类型的错误
    if (error.response?.status === 400) {
      const errorMessage = error.response?.data?.message || error.response?.data
      if (errorMessage && errorMessage.includes('无权限')) {
        ElMessage.error('您没有权限修改此模板，请确认您是该模板的创建者')
      } else if (errorMessage) {
        ElMessage.error(`操作失败：${errorMessage}`)
      } else {
        ElMessage.error('请求参数错误，请检查输入内容')
      }
    } else if (error.response?.status === 403) {
      ElMessage.error('权限不足，无法执行此操作')
    } else if (error.response?.status === 404) {
      ElMessage.error('模板或题目不存在')
    } else if (error.response?.status >= 500) {
      ElMessage.error('服务器错误，请稍后重试')
    } else {
      ElMessage.error('保存题目失败，请检查网络连接')
    }
  } finally {
    savingQuestion.value = false
  }
}

const resetQuestionForm = () => {
  questionForm.questionOrder = 1
  questionForm.questionType = 'SINGLE_CHOICE'
  questionForm.questionContent = ''
  questionForm.score = 10
  questionForm.difficultyLevel = ''
  questionForm.knowledgeTags = ''
  questionForm.isRequired = true
  questionFormRef.value?.resetFields()
}

// 题库选择方法
const addFromQuestionBank = () => {
  questionBankSearchForm.keyword = ''
  questionBankSearchForm.questionType = ''
  questionBankSearchForm.sourceType = ''
  questionBankSearchForm.dateRange = []
  questionBankQuestions.value = []
  selectedQuestionBankQuestions.value = []
  questionBankPagination.currentPage = 1
  questionBankPagination.pageSize = 10
  questionBankPagination.total = 0
  questionBankDialogVisible.value = true
  searchQuestionBank()
}

const searchQuestionBank = async () => {
  searchingQuestionBank.value = true
  try {
    const params: any = {
      page: questionBankPagination.currentPage - 1, // 后端从0开始
      size: questionBankPagination.pageSize
    }
    
    if (questionBankSearchForm.keyword) {
      params.keyword = questionBankSearchForm.keyword
    }
    if (questionBankSearchForm.questionType) {
      params.questionType = questionBankSearchForm.questionType
    }
    if (questionBankSearchForm.sourceType) {
      params.sourceType = questionBankSearchForm.sourceType
    }
    if (questionBankSearchForm.dateRange && questionBankSearchForm.dateRange.length === 2) {
      params.startDate = questionBankSearchForm.dateRange[0]
      params.endDate = questionBankSearchForm.dateRange[1]
    }
    
    const response = await questionApi.getQuestionsWithPagination(params)
    questionBankQuestions.value = response.content || []
    questionBankPagination.total = response.totalElements || 0
  } catch (error) {
    console.error('搜索题库失败:', error)
    ElMessage.error('搜索题库失败')
  } finally {
    searchingQuestionBank.value = false
  }
}

const handleQuestionBankSelection = (selection: any[]) => {
  selectedQuestionBankQuestions.value = selection
}

const addSelectedQuestions = async () => {
  if (selectedQuestionBankQuestions.value.length === 0 || !template.value) return
  
  addingQuestions.value = true
  try {
    const questions = selectedQuestionBankQuestions.value.map((q, index) => ({
      questionOrder: (template.value!.questions?.length || 0) + index + 1,
      questionType: q.questionType,
      questionContent: q.content,
      questionId: q.id,
      score: q.maxScore,
      difficultyLevel: q.difficulty,
      knowledgeTags: '',
      isRequired: true
    }))
    
    await examPaperTemplateApi.addTemplateQuestions(template.value.id, questions)
    ElMessage.success(`成功添加 ${questions.length} 道题目`)
    questionBankDialogVisible.value = false
    loadTemplate()
  } catch (error) {
    console.error('添加题目失败:', error)
    ElMessage.error('添加题目失败')
  } finally {
    addingQuestions.value = false
  }
}

const handlePageSizeChange = (newPageSize: number) => {
  questionBankPagination.pageSize = newPageSize
  searchQuestionBank()
}

const handleCurrentPageChange = (newPage: number) => {
  questionBankPagination.currentPage = newPage
  searchQuestionBank()
}

const resetQuestionBankSearch = () => {
  questionBankSearchForm.keyword = ''
  questionBankSearchForm.questionType = ''
  questionBankSearchForm.sourceType = ''
  questionBankSearchForm.dateRange = []
  questionBankPagination.currentPage = 1
  searchQuestionBank()
}

const markTemplateReady = async () => {
  if (!template.value) return
  
  updatingStatus.value = true
  try {
    await examPaperTemplateApi.markTemplateReady(template.value.id)
    ElMessage.success('模板标记为就绪成功')
    loadTemplate()
  } catch (error: any) {
    console.error('标记模板为就绪失败:', error)
    
    // 处理不同类型的错误
    if (error.response?.status === 400) {
      const errorMessage = error.response?.data?.message || error.response?.data
      if (errorMessage && errorMessage.includes('无权限')) {
        ElMessage.error('您没有权限修改此模板，请确认您是该模板的创建者')
      } else if (errorMessage && errorMessage.includes('题目')) {
        ElMessage.error('请确保所有题目都已配置完成后再标记为就绪')
      } else if (errorMessage) {
        ElMessage.error(`标记失败：${errorMessage}`)
      } else {
        ElMessage.error('标记模板为就绪失败')
      }
    } else if (error.response?.status === 403) {
      ElMessage.error('权限不足，无法修改此模板')
    } else {
      ElMessage.error('标记模板为就绪失败')
    }
  } finally {
    updatingStatus.value = false
  }
}

const markTemplatePublished = async () => {
  if (!template.value) return
  
  updatingStatus.value = true
  try {
    await examPaperTemplateApi.markTemplatePublished(template.value.id)
    ElMessage.success('模板发布成功')
    loadTemplate()
  } catch (error: any) {
    console.error('发布模板失败:', error)
    
    // 处理不同类型的错误
    if (error.response?.status === 400) {
      const errorMessage = error.response?.data?.message || error.response?.data
      if (errorMessage && errorMessage.includes('无权限')) {
        ElMessage.error('您没有权限修改此模板，请确认您是该模板的创建者')
      } else if (errorMessage) {
        ElMessage.error(`发布失败：${errorMessage}`)
      } else {
        ElMessage.error('发布模板失败')
      }
    } else if (error.response?.status === 403) {
      ElMessage.error('权限不足，无法发布此模板')
    } else {
      ElMessage.error('发布模板失败')
    }
  } finally {
    updatingStatus.value = false
  }
}

const cancelTemplatePublished = async () => {
  if (!template.value) return
  
  updatingStatus.value = true
  try {
    await examPaperTemplateApi.cancelTemplatePublished(template.value.id)
    ElMessage.success('模板取消公开成功')
    loadTemplate()
  } catch (error: any) {
    console.error('取消模板公开失败:', error)
    
    // 处理不同类型的错误
    if (error.response?.status === 400) {
      const errorMessage = error.response?.data?.message || error.response?.data
      if (errorMessage && errorMessage.includes('无权限')) {
        ElMessage.error('您没有权限修改此模板，请确认您是该模板的创建者')
      } else if (errorMessage) {
        ElMessage.error(`取消公开失败：${errorMessage}`)
      } else {
        ElMessage.error('取消模板公开失败')
      }
    } else if (error.response?.status === 403) {
      ElMessage.error('权限不足，无法修改此模板')
    } else {
      ElMessage.error('取消模板公开失败')
    }
  } finally {
    updatingStatus.value = false
  }
}

const markAllDraftQuestionsReady = async () => {
  if (!template.value?.questions || selectedQuestions.value.length === 0) return
  
  updatingQuestionStatus.value = true
  try {
    // 筛选选中的题目（包括DRAFT和CONFIGURED状态）
    const questions = template.value.questions.filter(q => 
      selectedQuestions.value.includes(q.id) && (q.status === 'DRAFT' || q.status === 'CONFIGURED')
    )
    
    if (questions.length === 0) {
      ElMessage.info('选中的题目中没有可标记为就绪的题目，或所有题目都已标记为就绪')
      return
    }
    
    await ElMessageBox.confirm(
      `确定要将选中的${questions.length}道题目标记为就绪吗？此操作不可恢复。`,
      '确认标记',
      {
        confirmButtonText: '确定标记',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    await Promise.all(questions.map(q => examPaperTemplateApi.markQuestionReady(template.value!.id, q.id)))
    ElMessage.success(`成功将${questions.length}道题目标记为就绪`)
    selectedQuestions.value = [] // 清空选择
    loadTemplate()
  } catch (error: any) {
    console.error('批量标记草稿题目为就绪失败:', error)
    
    // 处理不同类型的错误
    if (error.response?.status === 400) {
      const errorMessage = error.response?.data?.message || error.response?.data
      if (errorMessage && errorMessage.includes('无权限')) {
        ElMessage.error('您没有权限修改此模板，请确认您是该模板的创建者')
      } else if (errorMessage) {
        ElMessage.error(`批量标记失败：${errorMessage}`)
      } else {
        ElMessage.error('批量标记题目失败')
      }
    } else if (error.response?.status === 403) {
      ElMessage.error('权限不足，无法修改此模板')
    } else {
      ElMessage.error('批量标记草稿题目为就绪失败')
    }
  } finally {
    updatingQuestionStatus.value = false
  }
}

const showBatchDeleteDialog = () => {
  if (selectedQuestions.value.length === 0) {
    ElMessage.warning('请先选择要删除的题目')
    return
  }
  showBatchDeleteConfirm.value = true
}

const confirmBatchDelete = async () => {
  if (selectedQuestions.value.length === 0 || !template.value) return
  
  deletingQuestions.value = true
  try {
    await Promise.all(selectedQuestions.value.map((id: number) => 
      examPaperTemplateApi.deleteTemplateQuestion(template.value!.id, id)
    ))
    ElMessage.success('选中的题目删除成功')
    showBatchDeleteConfirm.value = false
    selectedQuestions.value = []
    loadTemplate()
  } catch (error) {
    console.error('批量删除题目失败:', error)
    ElMessage.error('批量删除题目失败')
  } finally {
    deletingQuestions.value = false
  }
}

const showEditDialog = ref(false)
const handleTemplateSaved = () => {
  loadTemplate()
}

const toggleQuestionSelection = (questionId: number, selected: any) => {
  const isSelected = Boolean(selected)
  if (isSelected) {
    if (!selectedQuestions.value.includes(questionId)) {
      selectedQuestions.value.push(questionId)
    }
  } else {
    const index = selectedQuestions.value.indexOf(questionId)
    if (index > -1) {
      selectedQuestions.value.splice(index, 1)
    }
  }
}

const showReimportDialog = ref(false)

// 题目展示相关方法
function hasOptions(question: any) {
  if (!question.questionConfig) return false
  try {
    const config = JSON.parse(question.questionConfig)
    return config.options && Array.isArray(config.options) && config.options.length > 0
  } catch {
    return false
  }
}

function getQuestionOptions(question: any) {
  if (!question.questionConfig) return []
  try {
    const config = JSON.parse(question.questionConfig)
    return config.options || []
  } catch {
    return []
  }
}

function getCorrectAnswer(question: any) {
  if (!question.questionConfig) return ''
  try {
    const config = JSON.parse(question.questionConfig)
    return config.correctAnswer || ''
  } catch {
    return ''
  }
}

function isCorrectOption(question: any, option: string) {
  const correctAnswer = getCorrectAnswer(question)
  if (!correctAnswer) return false
  
  // 提取选项标识符 (A、B、C、D)
  const optionMatch = option.match(/^([A-Z])、/)
  if (optionMatch && optionMatch[1] === correctAnswer) {
    return true
  }
  
  // 直接匹配选项内容
  const optionContent = option.replace(/^[A-Z]、\s*/, '')
  return optionContent === correctAnswer || option === correctAnswer
}

// 题目编辑相关方法
function needsOptions(questionType: string) {
  return ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE'].includes(questionType)
}

function getOptionPrefix(index: number) {
  return String.fromCharCode(65 + index) + '、'
}

function addOption() {
  if (questionOptions.value.length < 6) {
    questionOptions.value.push('')
  }
}

function removeOption(index: number) {
  if (questionOptions.value.length > 2) {
    questionOptions.value.splice(index, 1)
    // 如果删除的是当前正确答案选项，重置为第一个选项
    if (correctOptionIndex.value === index) {
      correctOptionIndex.value = 0
    } else if (correctOptionIndex.value > index) {
      correctOptionIndex.value--
    }
  }
}

function initializeQuestionOptions(question: any) {
  if (needsOptions(question.questionType) && question.questionConfig) {
    try {
      const config = JSON.parse(question.questionConfig)
      if (config.options && Array.isArray(config.options)) {
        // 提取选项内容（去除A、B、C等前缀）
        questionOptions.value = config.options.map((option: string) => 
          option.replace(/^[A-Z]、\s*/, '')
        )
        
        // 设置正确答案索引
        if (config.correctAnswer) {
          const correctIndex = config.correctAnswer.charCodeAt(0) - 65 // A=0, B=1, C=2...
          if (correctIndex >= 0 && correctIndex < questionOptions.value.length) {
            correctOptionIndex.value = correctIndex
          }
        }
      } else {
        // 重置为默认选项
        questionOptions.value = ['', '']
        correctOptionIndex.value = 0
      }
    } catch {
      questionOptions.value = ['', '']
      correctOptionIndex.value = 0
    }
  } else {
    // 非选择题，初始化为空选项
    questionOptions.value = ['', '']
    correctOptionIndex.value = 0
    
    // 如果有参考答案，设置到referenceAnswer字段
    if (question.questionConfig) {
      try {
        const config = JSON.parse(question.questionConfig)
        if (config.correctAnswer) {
          questionForm.referenceAnswer = config.correctAnswer
        }
      } catch {
        // 配置解析失败，保持默认值
      }
    }
  }
}

onMounted(() => {
  loadTemplate()
})
</script>

<style scoped>
.template-detail-view {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.template-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.template-info-card {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  color: #303133;
}

.card-header h3 {
  margin: 0;
  color: #303133;
}

.template-tags {
  display: flex;
  align-items: center;
  gap: 8px;
}

.questions-card {
  margin-bottom: 24px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.questions-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.question-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.question-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.question-order {
  font-weight: 600;
  color: #303133;
}

.question-score {
  color: #409eff;
  font-weight: 600;
}

.question-actions {
  display: flex;
  gap: 8px;
}

.question-content {
  margin-bottom: 8px;
}

.content-text {
  color: #606266;
  line-height: 1.6;
}

.content-placeholder {
  color: #c0c4cc;
  font-style: italic;
}

.referenced-info {
  margin-top: 8px;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
}

.loading-placeholder {
  padding: 40px 0;
}

.dialog-footer {
  text-align: right;
}

.batch-delete-content {
  text-align: center;
  padding: 20px 0;
}

.warning-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.warning-text {
  color: #e6a23c;
  font-size: 14px;
  margin-top: 8px;
}

.select-all-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 16px;
  border: 1px solid #e4e7ed;
}

.select-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.selected-count {
  margin-left: 8px;
  color: #409eff;
  font-weight: 500;
  font-size: 14px;
}

/* 题目展示优化样式 */
.question-text {
  margin-bottom: 12px;
  font-size: 15px;
  line-height: 1.6;
  color: #303133;
}

.question-options {
  margin: 12px 0;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
  border-left: 3px solid #409eff;
}

.options-title {
  font-weight: 600;
  color: #409eff;
  margin-bottom: 8px;
  font-size: 14px;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.option-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: white;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  transition: all 0.2s;
}

.option-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 4px rgba(64, 158, 255, 0.1);
}

.option-item.correct-option {
  background: #f0f9ff;
  border-color: #67c23a;
}

.option-label {
  flex: 1;
  color: #606266;
  font-size: 14px;
}

.correct-answer {
  margin: 12px 0;
  padding: 10px 12px;
  background: #f0f9ff;
  border-radius: 6px;
  border-left: 3px solid #67c23a;
}

.answer-title {
  font-weight: 600;
  color: #67c23a;
  margin-bottom: 6px;
  font-size: 14px;
}

.answer-content {
  display: flex;
  align-items: center;
}

/* 题目编辑对话框选项配置样式 */
.options-config {
  margin: 16px 0;
}

.options-manager {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 12px;
  background: #fafafa;
}

.option-input-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.option-input-row:last-child {
  margin-bottom: 0;
}

.option-prefix {
  font-weight: 600;
  color: #409eff;
  width: 30px;
  text-align: center;
}
</style> 