<template>
  <div class="exam-answers">
    <div class="page-header">
      <el-breadcrumb>
        <el-breadcrumb-item :to="{ path: '/exams' }">考试列表</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: `/exams/${examId}` }">{{ exam?.title }}</el-breadcrumb-item>
        <el-breadcrumb-item>学生答案</el-breadcrumb-item>
      </el-breadcrumb>
      <h1>学生答案管理</h1>
      <p class="page-description">查看和管理考试的所有学生答案</p>
    </div>

    <!-- 考试信息概览 -->
    <el-card v-if="exam" class="exam-info-card">
      <template #header>
        <div class="card-header">
          <span>考试信息</span>
          <el-button link @click="goToExamDetail">返回考试详情</el-button>
        </div>
      </template>
      
      <el-descriptions :column="4" border>
        <el-descriptions-item label="考试标题">{{ exam.title }}</el-descriptions-item>
        <el-descriptions-item label="题目数量">{{ exam.totalQuestions || 0 }}</el-descriptions-item>
        <el-descriptions-item label="答案总数">{{ statistics.totalAnswers || 0 }}</el-descriptions-item>
        <el-descriptions-item label="已评估答案">{{ statistics.evaluatedAnswers || 0 }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 查看模式切换 - 显眼的顶部标签 -->
    <el-card class="view-mode-card">
      <div class="view-mode-switcher">
        <h3>查看模式</h3>
        <el-radio-group v-model="viewMode" size="large" @change="handleViewModeChange" class="mode-radio-group">
          <el-radio-button label="answers" size="large">
            <el-icon><List /></el-icon>
            按答案查看
          </el-radio-button>
          <el-radio-button label="papers" size="large">
            <el-icon><Document /></el-icon>
            按学生试卷查看
          </el-radio-button>
        </el-radio-group>
      </div>
    </el-card>

    <!-- 筛选和搜索 -->
    <el-card class="filter-card">
      <el-row :gutter="16">
        <el-col :span="3" v-if="viewMode === 'answers'">
          <el-select v-model="questionIdFilter" placeholder="选择题目" clearable @change="loadData">
            <el-option label="全部题目" value="" />
            <el-option 
              v-for="question in questions" 
              :key="question.id" 
              :label="`题目${question.id}: ${question.title}`" 
              :value="question.id"
            />
          </el-select>
        </el-col>
        <el-col :span="3" v-if="viewMode === 'answers'">
          <el-select v-model="isEvaluatedFilter" placeholder="评估状态" clearable @change="loadData">
            <el-option label="全部状态" value="" />
            <el-option label="已评估" :value="true" />
            <el-option label="未评估" :value="false" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-input 
            v-model="studentKeywordFilter" 
            :placeholder="viewMode === 'answers' ? '搜索学生姓名/学号' : '搜索学生姓名/学号'"
            @keyup.enter="loadData"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="viewMode === 'answers' ? 6 : 12">
          <el-button type="primary" icon="Search" @click="loadData">搜索</el-button>
          <el-button icon="Refresh" @click="resetFilters">重置</el-button>
        </el-col>
        <el-col :span="5" style="text-align: right">
          <el-button type="success" icon="Download" @click="exportAnswers">导出答案</el-button>
          <el-button type="warning" icon="Upload" @click="showImportDialog">导入答案</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 答案列表 - 按答案查看模式 -->
    <el-card v-if="viewMode === 'answers'" class="answers-card">
      <template #header>
        <div class="card-header">
          <span>学生答案列表 ({{ pagination.total }})</span>
          <div>
            <el-button 
              type="primary" 
              icon="MagicStick" 
              @click="batchEvaluate"
              :disabled="selectedAnswers.length === 0"
            >
              批量AI评估 ({{ selectedAnswers.length }})
            </el-button>
            <el-button 
              type="danger" 
              icon="Delete" 
              @click="batchDeleteAnswers"
              :disabled="selectedAnswers.length === 0"
            >
              批量删除 ({{ selectedAnswers.length }})
            </el-button>
          </div>
        </div>
      </template>
      
      <el-table
        v-loading="loading"
        :data="filteredAnswers"
        style="width: 100%"
        @selection-change="handleSelectionChange"
        @sort-change="handleSortChange"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="student.name" label="学生姓名" width="120" sortable />
        
        <el-table-column prop="student.studentNumber" label="学号" width="120" />
        
        <el-table-column prop="questionTitle" label="题目" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click="viewQuestion(row.questionId)">
              {{ row.questionTitle }}
            </el-link>
          </template>
        </el-table-column>
        
        <el-table-column prop="answerText" label="答案内容" show-overflow-tooltip>
          <template #default="{ row }">
            <div v-if="row.answerText === '[未解析到答案]'" class="unparsed-answer">
              <el-tag type="danger">未解析到答案</el-tag>
              <el-button 
                link 
                type="primary" 
                icon="Edit"
                @click="openEditAnswerDialog(row)"
                style="margin-left: 8px;"
              >
                手动补充
              </el-button>
            </div>
            <div v-else class="answer-content">
              {{ row.answerText || '-' }}
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="score" label="得分" width="100" sortable>
          <template #default="{ row }">
            <span v-if="row.score !== null" class="score">{{ row.score }}</span>
            <el-tag v-else type="warning" size="small">未评估</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="evaluated" label="评估状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.evaluated ? 'success' : 'warning'" size="small">
              {{ row.evaluated ? '已评估' : '未评估' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="submittedAt" label="提交时间" width="160" sortable>
          <template #default="{ row }">
            {{ formatDate(row.submittedAt) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" icon="View" @click="viewAnswerDetail(row)">
                查看
              </el-button>
              <el-button 
                size="small" 
                type="primary" 
                icon="MagicStick"
                @click="evaluateAnswer(row)"
                :disabled="row.evaluated"
              >
                评估
              </el-button>
              <el-button 
                size="small" 
                type="danger" 
                icon="Delete"
                @click="deleteAnswer(row.id)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 学生试卷列表 - 按学生试卷查看模式 -->
    <el-card v-else-if="viewMode === 'papers'" class="papers-card">
      <template #header>
        <div class="card-header">
          <span>学生试卷列表 ({{ paperPagination.total }})</span>
          <div>
            <el-button 
              type="success" 
              icon="Document" 
              @click="batchExportPapers"
              :disabled="selectedPapers.length === 0"
            >
              批量导出试卷 ({{ selectedPapers.length }})
            </el-button>
            <el-button 
              type="danger" 
              icon="Delete" 
              @click="batchDeletePapers"
              :disabled="selectedPapers.length === 0"
            >
              批量删除试卷 ({{ selectedPapers.length }})
            </el-button>
          </div>
        </div>
      </template>
      
      <el-table
        v-loading="loading"
        :data="papersList"
        style="width: 100%"
        @selection-change="handlePaperSelectionChange"
        @sort-change="handleSortChange"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="studentName" label="学生姓名" width="120" sortable="custom" />
        
        <el-table-column prop="studentNumber" label="学号" width="120" />
        
        <el-table-column prop="className" label="班级" width="150" />
        
        <el-table-column prop="totalScore" label="总分" width="100" sortable="custom">
          <template #default="{ row }">
            <span v-if="row.totalScore !== null" class="score">{{ row.totalScore.toFixed(1) }}</span>
            <el-tag v-else type="info" size="small">未计算</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="completionStatus" label="完成状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getCompletionTagType(row.completionStatus)" size="small">
              {{ getCompletionStatusText(row.completionStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="submissionStatus" label="提交状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.submissionStatus === 'SUBMITTED' ? 'success' : 'warning'" size="small">
              {{ row.submissionStatus === 'SUBMITTED' ? '已提交' : '未提交' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="submittedAt" label="最后提交时间" width="160" sortable="custom">
          <template #default="{ row }">
            {{ row.submittedAt ? formatDate(row.submittedAt) : '-' }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" icon="View" @click="viewStudentPaper(row)">
                查看
              </el-button>
              <el-button 
                size="small" 
                type="primary" 
                icon="Edit"
                @click="editStudentPaper(row)"
              >
                评阅
              </el-button>
              <el-dropdown @command="(command) => handlePaperAction(command, row)" :loading="exportingPaper === row.studentId">
                <el-button size="small" icon="Download">
                  导出<el-icon class="el-icon--right"><arrow-down /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="exportPdf">导出PDF</el-dropdown-item>
                    <el-dropdown-item command="exportWord">导出Word</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button 
                size="small" 
                type="danger" 
                icon="Delete"
                @click="deleteStudentPaper(row)"
              >
                删除试卷
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="paperPagination.page"
          v-model:page-size="paperPagination.size"
          :total="paperPagination.total"
          :page-sizes="[10, 20, 50, 100]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handlePaperSizeChange"
          @current-change="handlePaperCurrentChange"
        />
      </div>
    </el-card>

    <!-- 手动补充答案对话框 -->
    <el-dialog
      v-model="editAnswerDialogVisible"
      title="手动补充答案"
      width="600px"
      @close="closeEditDialog"
    >
      <div v-if="editingAnswer">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="学生">{{ editingAnswer.student?.name }} ({{ editingAnswer.student?.studentNumber }})</el-descriptions-item>
          <el-descriptions-item label="题目">{{ editingAnswer.questionTitle }}</el-descriptions-item>
        </el-descriptions>
        <el-form-item label="答案内容" style="margin-top: 20px;">
          <el-input
            v-model="editedAnswerText"
            type="textarea"
            :rows="5"
            placeholder="请输入学生答案"
          />
        </el-form-item>
      </div>
      <template #footer>
        <el-button @click="closeEditDialog">取消</el-button>
        <el-button type="primary" @click="saveEditedAnswer" :loading="isSavingAnswer">
          保存答案
        </el-button>
      </template>
    </el-dialog>

    <!-- 答案详情对话框 -->
    <el-dialog
      v-model="answerDetailDialogVisible"
      title="答案详情"
      width="800px"
      destroy-on-close
    >
      <div v-if="currentAnswer" class="answer-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="学生姓名">{{ currentAnswer.student?.name }}</el-descriptions-item>
          <el-descriptions-item label="学号">{{ currentAnswer.student?.studentNumber }}</el-descriptions-item>
          <el-descriptions-item label="题目">{{ currentAnswer.questionTitle }}</el-descriptions-item>
          <el-descriptions-item label="得分">
            <span v-if="currentAnswer.score !== null">{{ currentAnswer.score }}</span>
            <el-tag v-else type="warning" size="small">未评估</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间" :span="2">
            {{ currentAnswer.submittedAt ? formatDate(currentAnswer.submittedAt) : '未知' }}
          </el-descriptions-item>
        </el-descriptions>
        
        <div class="answer-content-section">
          <h4>答案内容：</h4>
          <div class="answer-text">
            {{ currentAnswer.answerText || '无答案内容' }}
          </div>
        </div>
        
        <div v-if="currentAnswer.feedback" class="feedback-section">
          <h4>评估反馈：</h4>
          <div class="feedback-text">
            {{ currentAnswer.feedback }}
          </div>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="answerDetailDialogVisible = false">关闭</el-button>
          <el-button 
            v-if="currentAnswer && !currentAnswer.evaluated"
            type="primary" 
            @click="evaluateCurrentAnswer"
          >
            评估此答案
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 导入对话框 -->
    <el-dialog
      v-model="importDialogVisible"
      title="导入学生答案"
      width="700px"
    >
      <div class="import-section">
        <!-- 导入类型选择 -->
        <el-form-item label="导入类型" style="margin-bottom: 20px">
          <el-radio-group v-model="importType" @change="handleImportTypeChange">
            <el-radio-button label="file">文件导入</el-radio-button>
            <el-radio-button label="learning">学习通答案</el-radio-button>
            <el-radio-button label="template">基于模板</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <!-- 文件导入模式 -->
        <div v-if="importType === 'file'">
          <el-alert
            title="文件导入说明"
            type="info"
            :closable="false"
            style="margin-bottom: 20px"
          >
            <p>支持导入Excel格式的学生答案文件，文件需要包含以下列：</p>
            <ul>
              <li>学生姓名</li>
              <li>学号</li>
              <li>题目ID</li>
              <li>答案内容</li>
            </ul>
          </el-alert>
          
          <el-upload
            ref="uploadRef"
            :before-upload="beforeUpload"
            :on-change="handleFileChange"
            :auto-upload="false"
            accept=".xlsx,.xls,.csv"
            drag
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                只能上传 xlsx/xls/csv 文件
              </div>
            </template>
          </el-upload>
        </div>

        <!-- 学习通答案导入模式 -->
        <div v-else-if="importType === 'learning'">
          <el-alert
            title="学习通答案导入说明"
            type="info"
            :closable="false"
            style="margin-bottom: 20px"
          >
            <p>从学习通答案文件夹中批量导入学生答案，系统将自动解析Word文档内容。</p>
            <ul>
              <li>自动识别学生信息（姓名、学号等）</li>
              <li>自动解析题目和答案内容</li>
              <li>未注册学生将自动创建账户（未激活状态）</li>
              <li>系统支持多种解析策略，包括POI、Tika、LibreOffice转换等</li>
            </ul>
            
            <!-- 系统能力提示 -->
            <div v-if="systemCapabilities" style="margin-top: 10px; padding: 8px; background: #f8f9fa; border-radius: 4px; font-size: 12px;">
              <span style="font-weight: 500;">系统解析能力：</span>
              <el-tag v-if="systemCapabilities?.libreOffice?.available" type="success" size="small" style="margin-left: 4px;">LibreOffice增强</el-tag>
              <el-tag v-if="systemCapabilities?.apacheTika" type="success" size="small" style="margin-left: 4px;">Apache Tika</el-tag>
              <el-tag v-if="systemCapabilities?.apachePOI" type="success" size="small" style="margin-left: 4px;">Apache POI</el-tag>
              <el-tag v-if="systemCapabilities?.capable" type="info" size="small" style="margin-left: 4px;">可用</el-tag>
            </div>
          </el-alert>

          <el-form label-width="80px">
            <el-form-item label="选择科目">
              <el-select 
                v-model="selectedSubject" 
                placeholder="请选择科目"
                style="width: 100%"
                @change="handleSubjectChange"
                :loading="subjectLoading"
              >
                <el-option
                  v-for="subject in availableSubjects"
                  :key="subject"
                  :label="subject"
                  :value="subject"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="选择班级" v-if="selectedSubject">
              <div class="class-selection-wrapper">
                <!-- 全选控制 -->
                <div class="select-all-controls" style="margin-bottom: 8px">
                  <el-checkbox 
                    v-model="selectAllClasses"
                    :indeterminate="isClassesIndeterminate"
                    @change="handleSelectAllClasses"
                    style="font-weight: 500"
                  >
                    全选 ({{ selectedClasses.length }}/{{ availableClasses.length }})
                  </el-checkbox>
                </div>
                
                <!-- 班级选择器 -->
                <el-select 
                  v-model="selectedClasses" 
                  multiple 
                  placeholder="请选择要导入的班级"
                  style="width: 100%"
                  :loading="classLoading"
                  @change="handleClassSelectionChange"
                >
                  <el-option
                    v-for="classFolder in availableClasses"
                    :key="classFolder"
                    :label="classFolder"
                    :value="classFolder"
                  />
                </el-select>
              </div>
            </el-form-item>
          </el-form>
        </div>

        <!-- 基于模板导入模式 -->
        <div v-else-if="importType === 'template'">
          <el-alert
            title="基于模板导入说明"
            type="success"
            :closable="false"
            style="margin-bottom: 20px"
          >
            <p><strong>第二阶段：</strong>使用已验证的试卷模板精确导入学生答案</p>
            <ul>
              <li>基于事先整理好的试卷模板，按题号精确匹配</li>
              <li>避免题目解析错误，提高导入成功率</li>
              <li>支持单个学生失败隔离，不影响其他学生</li>
              <li>详细的导入日志和错误报告</li>
            </ul>
          </el-alert>

          <el-form label-width="100px">
            <el-form-item label="选择模板">

              
              <el-select 
                v-model="selectedTemplateId" 
                placeholder="请选择试卷模板"
                style="width: 100%"
                @change="handleTemplateChange"
                :loading="templateLoading"
                filterable
              >
                <el-option
                  v-for="template in availableTemplates"
                  :key="template.id"
                  :label="template.templateName"
                  :value="template.id"
                >
                  <div style="display: flex; justify-content: space-between;">
                    <span>{{ template.templateName }}</span>
                    <span style="color: #8492a6; font-size: 12px;">
                      {{ template.subject }} | {{ template.totalQuestions }}题
                    </span>
                  </div>
                </el-option>
              </el-select>
              

            </el-form-item>

            <!-- 模板信息展示 -->
            <div v-if="selectedTemplate" class="template-info" style="margin-bottom: 20px; padding: 15px; background: #f8f9fa; border-radius: 6px;">
              <el-row :gutter="20">
                <el-col :span="12">
                  <div class="info-item">
                    <span class="label">模板名称：</span>
                    <span>{{ selectedTemplate.templateName }}</span>
                  </div>
                  <div class="info-item">
                    <span class="label">考试标题：</span>
                    <span>{{ selectedTemplate.examTitle || '未设置' }}</span>
                  </div>
                </el-col>
                <el-col :span="12">
                  <div class="info-item">
                    <span class="label">总题数：</span>
                    <span>{{ selectedTemplate.totalQuestions }}</span>
                  </div>
                  <div class="info-item">
                    <span class="label">模板状态：</span>
                    <el-tag 
                      :type="selectedTemplate.status === 'READY' ? 'success' : 'warning'"
                      size="small"
                    >
                      {{ getTemplateStatusText(selectedTemplate.status) }}
                    </el-tag>
                    <el-button 
                      v-if="selectedTemplate.status !== 'READY'"
                      type="primary" 
                      size="small" 
                      @click="markTemplateReady"
                      style="margin-left: 10px;"
                    >
                      标记为就绪
                    </el-button>
                  </div>
                </el-col>
              </el-row>
              
              <!-- 状态警告 -->
              <el-alert
                v-if="selectedTemplate.status !== 'READY'"
                type="warning"
                :closable="false"
                style="margin-top: 15px;"
              >
                <template #title>
                  模板状态说明
                </template>
                当前模板状态为"{{ getTemplateStatusText(selectedTemplate.status) }}"。建议先前往模板详情页面确认题目内容无误，然后标记为就绪状态后再使用。
              </el-alert>
            </div>

            <el-form-item label="选择科目" v-if="selectedTemplate">
              <el-select 
                v-model="templateSubject" 
                placeholder="请选择科目"
                style="width: 100%"
                @change="handleTemplateSubjectChange"
                :loading="subjectLoading"
              >
                <el-option
                  v-for="subject in availableSubjects"
                  :key="subject"
                  :label="subject"
                  :value="subject"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="选择班级" v-if="templateSubject">
              <div class="class-selection-wrapper">
                <!-- 全选控制 -->
                <div class="select-all-controls" style="margin-bottom: 8px">
                  <el-checkbox 
                    v-model="selectAllTemplateClasses"
                    :indeterminate="isTemplateClassesIndeterminate"
                    @change="handleSelectAllTemplateClasses"
                    style="font-weight: 500"
                  >
                    全选 ({{ selectedTemplateClasses.length }}/{{ availableClasses.length }})
                  </el-checkbox>
                </div>
                
                <!-- 班级选择器 -->
                <el-select 
                  v-model="selectedTemplateClasses" 
                  multiple 
                  placeholder="请选择要导入的班级"
                  style="width: 100%"
                  :loading="classLoading"
                  @change="handleTemplateClassSelectionChange"
                >
                  <el-option
                    v-for="classFolder in availableClasses"
                    :key="classFolder"
                    :label="classFolder"
                    :value="classFolder"
                  />
                </el-select>
              </div>
            </el-form-item>
          </el-form>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="importDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="confirmImport"
            :loading="importLoading"
            :disabled="isImportDisabled"
          >
            确认导入
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Search, 
  Refresh, 
  Download, 
  Upload, 
  MagicStick, 
  View, 
  Delete,
  Edit,
  Document,
  ArrowDown,
  UploadFilled,
  List
} from '@element-plus/icons-vue'
import { examApi } from '@/api/exam'
import { answerApi } from '@/api/answer'
import { questionApi } from '@/api/question'
import { evaluationApi } from '@/api/evaluation'
import { examTemplateApi } from '@/api/examTemplate'
import type { 
  ExamResponse, 
  StudentAnswerResponse, 
  StudentExamPaperResponse,
  QuestionResponse, 
  PageResponse 
} from '@/types/api'

const route = useRoute()
const router = useRouter()

// 响应式数据
const loading = ref(false)
const importLoading = ref(false)
const viewMode = ref<'answers' | 'papers'>('papers') // 查看模式：按答案查看 or 按学生试卷查看，默认按学生试卷查看
const exam = ref<ExamResponse | null>(null)
const questions = ref<QuestionResponse[]>([])
const answers = ref<StudentAnswerResponse[]>([])
const selectedAnswers = ref<StudentAnswerResponse[]>([])
const papersList = ref<StudentExamPaperResponse[]>([])
const selectedPapers = ref<StudentExamPaperResponse[]>([])
const currentAnswer = ref<StudentAnswerResponse | null>(null)
const selectedFile = ref<File | null>(null)
const answerDetailDialogVisible = ref(false)
const importDialogVisible = ref(false)

// 手动补充答案相关
const editAnswerDialogVisible = ref(false)
const editingAnswer = ref<StudentAnswerResponse | null>(null)
const editedAnswerText = ref('')
const isSavingAnswer = ref(false)

// 学习通导入相关变量
const importType = ref<'file' | 'learning' | 'template'>('file')
const availableSubjects = ref<string[]>([])
const availableClasses = ref<string[]>([])
const selectedSubject = ref('')
const selectedClasses = ref<string[]>([])
const subjectLoading = ref(false)
const classLoading = ref(false)
const isImporting = ref(false)
const importingMessage = ref('')

// 全选功能相关
const selectAllClasses = ref(false)

// 模板导入相关变量
const selectedTemplateId = ref<number | string>('')
const availableTemplates = ref<any[]>([])
const selectedTemplate = ref<any | null>(null)
const templateLoading = ref(false)
const templateSubject = ref('')
const selectedTemplateClasses = ref<string[]>([])
const selectAllTemplateClasses = ref(false)

// 计算属性：全选状态
const isClassesIndeterminate = computed(() => {
  const selected = selectedClasses.value.length
  const total = availableClasses.value.length
  return selected > 0 && selected < total
})

// 模板相关计算属性
const isTemplateClassesIndeterminate = computed(() => {
  const selected = selectedTemplateClasses.value.length
  const total = availableClasses.value.length
  return selected > 0 && selected < total
})

// 系统能力状态
const systemCapabilities = ref<any>(null)

const statistics = ref({
  totalAnswers: 0,
  evaluatedAnswers: 0,
  averageScore: 0
})

// 使用 ref 来解决 el-select 类型问题
const questionIdFilter = ref<number | string>('')
const isEvaluatedFilter = ref<boolean | string>('')
const studentKeywordFilter = ref('')

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 学生试卷分页配置
const paperPagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const sortConfig = reactive({
  prop: '',
  order: ''
})

// 计算属性
const examId = computed(() => {
  const id = route.params.examId
  return typeof id === 'string' ? parseInt(id, 10) : Number(id)
})

// 筛选后的答案列表 - 现在直接使用API返回的数据
const answersPageData = ref<PageResponse<StudentAnswerResponse> | null>(null)

const filteredAnswers = computed(() => {
  return answersPageData.value?.content || []
})

// 计算是否可以导入
const isImportDisabled = computed(() => {
  if (importType.value === 'file') {
    return !selectedFile.value
  } else if (importType.value === 'learning') {
    return !selectedSubject.value || selectedClasses.value.length === 0
  } else if (importType.value === 'template') {
    return !selectedTemplateId.value || !templateSubject.value || selectedTemplateClasses.value.length === 0
  }
  return true
})

// 方法
// 查看模式切换
const handleViewModeChange = () => {
  // 切换查看模式时重置分页
  if (viewMode.value === 'answers') {
    pagination.page = 1
  } else {
    paperPagination.page = 1
  }
  loadData()
}

// 统一的数据加载方法
const loadData = () => {
  if (viewMode.value === 'answers') {
    loadAnswers()
  } else {
    loadPapers()
  }
}

// 加载学生试卷数据
const loadPapers = async () => {
  try {
    loading.value = true
    console.log('Loading papers for exam:', examId.value, 'page:', paperPagination.page, 'size:', paperPagination.size)
    
    const pageData = await answerApi.getExamPapers(
      examId.value,
      paperPagination.page,
      paperPagination.size,
      studentKeywordFilter.value || undefined
    )
    
    console.log('Papers loaded:', pageData)
    papersList.value = pageData.content
    paperPagination.total = pageData.totalElements
    
  } catch (error) {
    console.error('Failed to load papers:', error)
    ElMessage.error('加载学生试卷失败')
  } finally {
    loading.value = false
  }
}

// 学生试卷相关的事件处理
const handlePaperSelectionChange = (selection: StudentExamPaperResponse[]) => {
  selectedPapers.value = selection
}

const handlePaperSizeChange = (size: number) => {
  paperPagination.size = size
  paperPagination.page = 1
  loadData()
}

const handlePaperCurrentChange = (page: number) => {
  paperPagination.page = page
  loadData()
}

// 查看学生试卷
const viewStudentPaper = (paper: StudentExamPaperResponse) => {
  router.push(`/exams/${examId.value}/students/${paper.studentId}/paper`)
}

// 编辑/评阅学生试卷
const editStudentPaper = (paper: StudentExamPaperResponse) => {
  // 跳转到学生试卷详情页面，可以在那里进行评阅
  router.push(`/exams/${examId.value}/students/${paper.studentId}/paper`)
}

// 试卷操作处理
const handlePaperAction = async (command: string, paper: StudentExamPaperResponse) => {
  if (command === 'exportPdf' || command === 'exportWord') {
    const format = command === 'exportPdf' ? 'pdf' : 'docx';
    const mimeType = command === 'exportPdf' ? 'application/pdf' : 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
    const fileExtension = command === 'exportPdf' ? 'pdf' : 'docx';

    try {
      exportingPaper.value = paper.studentId;
      ElNotification.info({
        title: '正在导出',
        message: `正在为学生 ${paper.studentName} 生成 ${format.toUpperCase()} 试卷文件...`,
      });

      const response = await examApi.exportStudentPaper(examId.value, paper.studentId, format);
      
      const blob = new Blob([response], { type: mimeType });
      const link = document.createElement('a');
      link.href = window.URL.createObjectURL(blob);
      link.download = `${exam.value?.title}_${paper.studentName}_试卷.${fileExtension}`;
      
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(link.href);

      ElNotification.success({
        title: '导出成功',
        message: `${paper.studentName}的试卷已开始下载。`,
      });

    } catch (error) {
      console.error(`导出学生 ${paper.studentId} 试卷失败:`, error);
      ElMessage.error('导出试卷失败');
    } finally {
      exportingPaper.value = null;
    }
  }
};

// 批量导出试卷
const batchExportPapers = async () => {
  if (selectedPapers.value.length === 0) {
    ElMessage.warning('请至少选择一份试卷');
    return;
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要导出选中的 ${selectedPapers.value.length} 份试卷吗？`,
      '批量导出确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 逐个导出试卷
    for (const paper of selectedPapers.value) {
      await handlePaperAction('exportPdf', paper)
      // 添加小延迟避免请求过快
      await new Promise(resolve => setTimeout(resolve, 500))
    }
    
    ElMessage.success('所有试卷导出完成')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to batch export papers:', error)
      ElMessage.error('批量导出失败')
    }
  }
}

const batchDeletePapers = async () => {
  if (selectedPapers.value.length === 0) {
    ElMessage.warning('请选择要删除的试卷')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedPapers.value.length} 份学生试卷吗？此操作将删除这些学生在该场考试下的所有答案，且不可恢复。`,
      '批量删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        dangerouslyUseHTMLString: true
      }
    )
    
    const studentIds = selectedPapers.value.map(p => p.studentId)
    const result = await answerApi.batchDeleteExamAnswers(examId.value, {
      deleteType: 'students',
      studentIds
    })
    
    ElMessage.success(result.message)
    selectedPapers.value = [] // 清空选中
    loadPapers() // 重新加载列表
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to batch delete papers:', error)
      ElMessage.error('批量删除试卷失败')
    }
  }
}

const loadExamInfo = async () => {
  try {
    exam.value = await examApi.getExam(examId.value)
    questions.value = await questionApi.getQuestionsByExam(examId.value)
  } catch (error) {
    console.error('Failed to load exam info:', error)
    ElMessage.error('加载考试信息失败')
  }
}

const loadAnswers = async () => {
  try {
    loading.value = true
    
    // 使用新的分页筛选API
    const pageData = await answerApi.getAnswersByExamWithFilters(
      examId.value,
      pagination.page,
      pagination.size,
      questionIdFilter.value ? Number(questionIdFilter.value) : undefined,
      isEvaluatedFilter.value !== '' ? Boolean(isEvaluatedFilter.value) : undefined,
      studentKeywordFilter.value || undefined
    )
    
    answersPageData.value = pageData
    pagination.total = pageData.totalElements
    
    // 同时获取原始数据用于统计信息计算
    if (pagination.page === 1 && !questionIdFilter.value && isEvaluatedFilter.value === '' && !studentKeywordFilter.value) {
      // 只在第一页且无筛选时更新统计信息
      answers.value = pageData.content
      statistics.value.totalAnswers = pageData.totalElements
      statistics.value.evaluatedAnswers = pageData.content.filter(a => a.evaluated).length
      const scores = pageData.content.filter(a => a.score !== null && a.score !== undefined).map(a => a.score!)
      statistics.value.averageScore = scores.length > 0 
        ? Math.round(scores.reduce((sum, score) => sum + (score || 0), 0) / scores.length * 10) / 10
        : 0
    }
      
  } catch (error) {
    console.error('Failed to load answers:', error)
    ElMessage.error('加载答案列表失败')
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  questionIdFilter.value = ''
  isEvaluatedFilter.value = ''
  studentKeywordFilter.value = ''
  pagination.page = 1
  loadAnswers()
}

const handleSelectionChange = (selection: StudentAnswerResponse[]) => {
  selectedAnswers.value = selection
}

const handleSortChange = ({ prop, order }: any) => {
  sortConfig.prop = prop
  sortConfig.order = order
  // 实现前端排序逻辑
  if (order && prop) {
    filteredAnswers.value.sort((a: any, b: any) => {
      let valA = a
      let valB = b
      for (const key of prop.split('.')) {
        valA = valA?.[key]
        valB = valB?.[key]
      }
      if (valA == null) return 1
      if (valB == null) return -1
      if (order === 'ascending') return valA > valB ? 1 : -1
      if (order === 'descending') return valA < valB ? 1 : -1
      return 0
    })
  } else {
    // 无排序，重新加载
    loadAnswers()
  }
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  loadAnswers()
}

const handleCurrentChange = (page: number) => {
  pagination.page = page
  loadAnswers()
}

const viewQuestion = (questionId: number) => {
  router.push(`/questions/${questionId}`)
}

const viewAnswerDetail = (answer: StudentAnswerResponse) => {
  currentAnswer.value = answer
  answerDetailDialogVisible.value = true
}

const evaluateAnswer = async (answer: StudentAnswerResponse) => {
  try {
    await evaluationApi.evaluateAnswer(answer.id)
    ElMessage.success('答案评估成功')
    loadAnswers()
  } catch (error) {
    console.error('Failed to evaluate answer:', error)
    ElMessage.error('答案评估失败')
  }
}

const evaluateCurrentAnswer = async () => {
  if (currentAnswer.value) {
    await evaluateAnswer(currentAnswer.value)
    answerDetailDialogVisible.value = false
  }
}

const batchEvaluate = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要对选中的 ${selectedAnswers.value.length} 个答案进行AI评估吗？`,
      '批量评估确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const answerIds = selectedAnswers.value.map(a => a.id)
    await evaluationApi.batchEvaluateAnswers(answerIds)
    ElMessage.success('批量评估任务已开始')
    loadAnswers()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to batch evaluate:', error)
      ElMessage.error('批量评估失败')
    }
  }
}

const batchDeleteAnswers = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedAnswers.value.length} 个答案吗？此操作不可恢复。`,
      '批量删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    
    const answerIds = selectedAnswers.value.map(a => a.id)
    const result = await answerApi.batchDeleteExamAnswers(examId.value, {
      deleteType: 'answers',
      answerIds
    })
    
    ElMessage.success(result.message)
    selectedAnswers.value = [] // 清空选中状态
    loadAnswers() // 重新加载数据
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to batch delete answers:', error)
      ElMessage.error('批量删除失败')
    }
  }
}

const deleteAnswer = async (answerId: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个答案吗？此操作不可恢复。',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await answerApi.deleteAnswer(answerId)
    ElMessage.success('答案删除成功')
    loadAnswers()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to delete answer:', error)
      ElMessage.error('删除答案失败')
    }
  }
}

const deleteStudentPaper = async (paper: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除学生"${paper.studentName}"的整份试卷吗？这将删除该学生在此考试中的所有答案，此操作不可恢复。`,
      '确认删除学生试卷',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        dangerouslyUseHTMLString: true
      }
    )
    
    await answerApi.deleteStudentExamAnswers(paper.studentId, examId.value)
    ElMessage.success(`学生"${paper.studentName}"的试卷删除成功`)
    loadPapers()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to delete student paper:', error)
      ElMessage.error('删除学生试卷失败')
    }
  }
}

const exportAnswers = async () => {
  try {
    const questionId = typeof questionIdFilter.value === 'number' ? questionIdFilter.value : undefined
    const isEvaluated = typeof isEvaluatedFilter.value === 'boolean' ? isEvaluatedFilter.value : undefined
    
    await answerApi.exportAnswers(examId.value, questionId, isEvaluated)
    ElMessage.success('答案导出成功')
  } catch (error) {
    console.error('Failed to export answers:', error)
    ElMessage.error('答案导出失败')
  }
}

const showImportDialog = () => {
  importDialogVisible.value = true
  selectedFile.value = null
  importType.value = 'file'
  selectedSubject.value = ''
  selectedClasses.value = []
  // 重置模板相关字段
  selectedTemplateId.value = ''
  selectedTemplate.value = null
  templateSubject.value = ''
  selectedTemplateClasses.value = []
  selectAllTemplateClasses.value = false
}

const beforeUpload = (file: File) => {
  const isValidType = ['application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 
                      'application/vnd.ms-excel', 
                      'text/csv'].includes(file.type)
  if (!isValidType) {
    ElMessage.error('只能上传 Excel 或 CSV 文件!')
    return false
  }
  
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB!')
    return false
  }
  
  return false // 阻止自动上传
}

const handleFileChange = (file: any) => {
  selectedFile.value = file.raw
}

// 导入类型变化处理
const handleImportTypeChange = () => {
  selectedFile.value = null
  selectedSubject.value = ''
  selectedClasses.value = []
  selectAllClasses.value = false
  // 重置模板相关字段
  selectedTemplateId.value = ''
  selectedTemplate.value = null
  templateSubject.value = ''
  selectedTemplateClasses.value = []
  selectAllTemplateClasses.value = false
  
  if (importType.value === 'learning') {
    loadAvailableSubjects()
    loadSystemCapabilities()
  } else if (importType.value === 'template') {
    loadAvailableTemplates()
    // 基于模板导入也需要加载科目，因为使用的是相同的学习通答案格式
    loadAvailableSubjects()
  }
}

// 加载系统能力状态
const loadSystemCapabilities = async () => {
  try {
    const response = await fetch('/api/debug/document-parsing', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
        'Content-Type': 'application/json'
      }
    })
    
    if (response.ok) {
      systemCapabilities.value = await response.json()
    }
  } catch (error) {
    console.warn('获取系统能力状态失败:', error)
    // 不显示错误，这是非关键功能
  }
}

// 科目变化处理
const handleSubjectChange = async () => {
  selectedClasses.value = []
  if (selectedSubject.value) {
    await loadSubjectClasses()
  }
}

// 加载可用科目
const loadAvailableSubjects = async () => {
  try {
    subjectLoading.value = true
    availableSubjects.value = await answerApi.getLearningSubjects()
  } catch (error) {
    console.error('Failed to load subjects:', error)
    ElMessage.error('加载科目列表失败')
  } finally {
    subjectLoading.value = false
  }
}

// 加载科目下的班级
const loadSubjectClasses = async () => {
  try {
    classLoading.value = true
    availableClasses.value = await answerApi.getLearningSubjectClasses(selectedSubject.value)
    // 重置全选状态
    selectAllClasses.value = false
    selectedClasses.value = []
  } catch (error) {
    console.error('Failed to load classes:', error)
    ElMessage.error('加载班级列表失败')
  } finally {
    classLoading.value = false
  }
}

// 全选/取消全选班级
const handleSelectAllClasses = (val: any) => {
  const checked = Boolean(val)
  if (checked) {
    selectedClasses.value = [...availableClasses.value]
  } else {
    selectedClasses.value = []
  }
  // 更新全选状态
  selectAllClasses.value = checked
}

// 班级选择变化处理
const handleClassSelectionChange = () => {
  const selected = selectedClasses.value.length
  const total = availableClasses.value.length
  selectAllClasses.value = selected === total && total > 0
}

const confirmImport = async () => {
  try {
    importLoading.value = true
    
    if (importType.value === 'file') {
      if (!selectedFile.value) {
        ElMessage.error('请选择要导入的文件')
        return
      }
      
      // 检查文件类型，如果是学习通答案文档，使用异步导入
      const fileName = selectedFile.value.name.toLowerCase()
      const isLearningDoc = fileName.includes('miniprogram') || 
                           fileName.includes('学习通')
      
      console.log('文件检测:', fileName, '是否为学习通文档:', isLearningDoc)
      
      if (isLearningDoc) {
        // 使用异步导入学习通答案文件，确保传递examId
        console.log('导入学习通答案文件到考试:', examId.value)
        const response = await answerApi.importLearningAnswerFile(selectedFile.value, examId.value)
        ElMessage.success(`学习通答案导入任务已启动，任务ID: ${response.taskId}`)
        ElMessage.info('题目将自动关联到当前考试，请在任务中心查看导入进度')
        
        // 可选：导航到任务中心
        setTimeout(() => {
          router.push('/task-center')
        }, 2000)
        
        importDialogVisible.value = false
        return // 异步导入直接返回，不刷新页面
      } else {
        // 使用同步导入其他格式文件
        await answerApi.importAnswersToExam(examId.value, selectedFile.value)
        ElMessage.success('答案导入成功')
      }
    } else if (importType.value === 'learning') {
      if (!selectedSubject.value || selectedClasses.value.length === 0) {
        ElMessage.error('请选择科目和班级')
        return
      }
      
      // 使用异步导入API，立即返回任务ID
      await importLearningAnswers()
    } else if (importType.value === 'template') {
      if (!selectedTemplateId.value || !templateSubject.value || selectedTemplateClasses.value.length === 0) {
        ElMessage.error('请选择模板、科目和班级')
        return
      }
      
      // 使用基于模板的导入
      await importWithTemplate()
    }
    
    importDialogVisible.value = false
    loadAnswers() // 同步导入完成后刷新页面
  } catch (error: any) {
    console.error('Failed to import answers:', error)
    ElMessage.error('答案导入失败: ' + (error.message || '未知错误'))
  } finally {
    importLoading.value = false
  }
}

const importLearningAnswers = async () => {
  if (!selectedSubject.value || selectedClasses.value.length === 0) {
    ElMessage.error('请选择科目和班级');
    return;
  }

  try {
    isImporting.value = true;
    importingMessage.value = '正在创建导入任务...';
    
    // 使用异步导入API，立即返回任务ID
    const response = await answerApi.importLearningAnswersAsync(
      selectedSubject.value,
      selectedClasses.value,
      examId.value
    );
    
    if (response.data.success) {
      ElMessage.success(`✅ ${response.data.message}`);
      
      // 显示任务信息和引导用户到任务中心
      ElMessageBox.alert(
        `导入任务已创建！任务ID: ${response.data.taskId}\n\n您可以在任务中心查看导入进度和结果。`,
        '异步导入任务已启动',
        {
          confirmButtonText: '前往任务中心',
          type: 'success'
        }
      ).then(() => {
        // 导航到任务中心
        router.push('/task-center');
      }).catch(() => {
        // 用户点击了取消，刷新当前页面的数据
        loadAnswers();
      });
      
      // 关闭导入对话框
      importDialogVisible.value = false;
      resetImportForm();
    } else {
      ElMessage.error(response.data.message || '导入失败');
    }
  } catch (error: any) {
    console.error('Failed to import answers:', error);
    
    if (error.code === 'ECONNABORTED') {
      ElMessage.error('导入请求超时，请尝试使用异步导入功能');
    } else {
      ElMessage.error(error.response?.data?.message || '导入失败');
    }
  } finally {
    isImporting.value = false;
    importingMessage.value = '';
  }
};

// 加载可用模板列表
const loadAvailableTemplates = async () => {
  try {
    templateLoading.value = true
    const templatesResponse = await examTemplateApi.getUserTemplates(0, 100) // 获取前100个模板
    
    const allTemplates = templatesResponse.data.content
    const readyTemplates = allTemplates.filter((template: any) => template.status === 'READY')
    
    // 优先显示READY状态的模板，如果没有则显示所有模板
    const availableTemplatesForUse = readyTemplates.length > 0 ? readyTemplates : allTemplates
    availableTemplates.value = availableTemplatesForUse
    
    if (readyTemplates.length === 0 && allTemplates.length > 0) {
      ElMessage.warning(`找到 ${allTemplates.length} 个模板，但没有READY状态的模板。已显示所有模板供选择。`)
    } else {
      ElMessage.success(`加载到 ${availableTemplatesForUse.length} 个可用模板`)
    }
  } catch (error) {
    console.error('Failed to load templates:', error)
    ElMessage.error('加载模板列表失败')
  } finally {
    templateLoading.value = false
  }
}

// 模板相关方法
const handleTemplateChange = async () => {
  if (!selectedTemplateId.value) {
    selectedTemplate.value = null
    return
  }
  try {
    // 实际调用API获取模板详情
    selectedTemplate.value = await examTemplateApi.getTemplateById(Number(selectedTemplateId.value))
  } catch (error) {
    console.error('Failed to load template details:', error)
    ElMessage.error('加载模板详情失败')
  }
}

const getTemplateStatusText = (status: string) => {
  const texts: Record<string, string> = {
    'DRAFT': '草稿',
    'READY': '就绪',
    'APPLIED': '已应用',
    'ARCHIVED': '已归档'
  }
  return texts[status] || status
}

const handleTemplateSubjectChange = async () => {
  if (!templateSubject.value) {
    availableClasses.value = []
    return
  }
  
  classLoading.value = true
  try {
    availableClasses.value = await answerApi.getLearningSubjectClasses(templateSubject.value)
    selectedTemplateClasses.value = []
    selectAllTemplateClasses.value = false
  } catch (error) {
    console.error('加载班级列表失败:', error)
    ElMessage.error('加载班级列表失败')
  } finally {
    classLoading.value = false
  }
}

const handleSelectAllTemplateClasses = (checked: any) => {
  if (checked) {
    selectedTemplateClasses.value = [...availableClasses.value]
  } else {
    selectedTemplateClasses.value = []
  }
}

const handleTemplateClassSelectionChange = () => {
  const selected = selectedTemplateClasses.value.length
  const total = availableClasses.value.length
  selectAllTemplateClasses.value = selected === total
}

const importWithTemplate = async () => {
  try {
    isImporting.value = true
    importingMessage.value = '正在使用模板导入学生答案...'
    // 实际调用基于模板的导入API
    const response = await answerApi.importLearningAnswersWithTemplate(
      templateSubject.value,
      selectedTemplateClasses.value,
      Number(selectedTemplateId.value),
      examId.value
    )
    if (response.success) {
      ElMessage.success(response.message || '学生答案导入成功！')
      // 可选：刷新答案列表
      await loadData()
    } else {
      ElMessage.error(response.message || '学生答案导入失败')
    }
  } catch (error: any) {
    console.error('基于模板导入失败:', error)
    ElMessage.error('基于模板导入失败: ' + (error.message || '未知错误'))
  } finally {
    isImporting.value = false
    importingMessage.value = ''
  }
}

// 标记模板为就绪状态
const markTemplateReady = async () => {
  if (!selectedTemplate.value) {
    return
  }
  
  try {
    await ElMessageBox.confirm(
      '确定要将此模板标记为就绪状态吗？标记后可用于答案导入。',
      '确认标记',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    templateLoading.value = true
    await examTemplateApi.markTemplateReady(selectedTemplate.value.id)
    
    // 更新本地状态
    selectedTemplate.value.status = 'READY'
    
    // 重新加载模板列表
    await loadAvailableTemplates()
    
    ElMessage.success('模板已标记为就绪状态')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('标记模板就绪失败:', error)
      ElMessage.error('标记模板就绪失败: ' + (error.message || '未知错误'))
    }
  } finally {
    templateLoading.value = false
  }
}

// 重置导入表单
const resetImportForm = () => {
  selectedFile.value = null;
  selectedSubject.value = '';
  selectedClasses.value = [];
  selectAllClasses.value = false;
  // 重置模板相关表单
  selectedTemplateId.value = '';
  selectedTemplate.value = null;
  templateSubject.value = '';
  selectedTemplateClasses.value = [];
  selectAllTemplateClasses.value = false;
  importType.value = 'file';
};

const goToExamDetail = () => {
  router.push(`/exams/${examId.value}`)
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN')
}

// 手动补充答案方法
const openEditAnswerDialog = (answer: StudentAnswerResponse) => {
  editingAnswer.value = answer
  editedAnswerText.value = '' // 清空，等待用户输入
  editAnswerDialogVisible.value = true
}

const closeEditDialog = () => {
  editAnswerDialogVisible.value = false
  editingAnswer.value = null
  editedAnswerText.value = ''
}

const saveEditedAnswer = async () => {
  if (!editingAnswer.value || !editedAnswerText.value.trim()) {
    ElMessage.warning('答案内容不能为空')
    return
  }
  
  try {
    isSavingAnswer.value = true
    await answerApi.updateAnswer(editingAnswer.value.id, {
      answerText: editedAnswerText.value.trim()
    })
    ElMessage.success('答案补充成功')
    closeEditDialog()
    // 刷新当前页数据
    await loadData()
  } catch (error) {
    console.error('Failed to save answer:', error)
    ElMessage.error('保存答案失败')
  } finally {
    isSavingAnswer.value = false
  }
}

const exportingPaper = ref<number | null>(null)

const getCompletionStatusText = (status: string) => {
  const map: { [key: string]: string } = {
    NOT_STARTED: '未开始',
    IN_PROGRESS: '进行中',
    COMPLETED: '已完成',
  };
  return map[status] || '未知';
};

const getCompletionTagType = (status: string) => {
  const map: { [key: string]: 'warning' | 'primary' | 'success' } = {
    NOT_STARTED: 'warning',
    IN_PROGRESS: 'primary',
    COMPLETED: 'success',
  };
  return map[status] || 'info';
};

onMounted(() => {
  loadExamInfo()
  loadData() // 使用统一的数据加载方法，根据当前模式加载数据
})
</script>

<style scoped>
.exam-answers {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 8px 0 4px 0;
  font-size: 24px;
  font-weight: 600;
}

.page-description {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.exam-info-card,
.filter-card,
.answers-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
}

.answer-content {
  max-width: 300px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.action-buttons {
  display: flex;
  gap: 4px;
  flex-wrap: nowrap;
}

.action-buttons .el-button {
  margin: 0;
  padding: 5px 8px;
  font-size: 12px;
}

.score {
  font-weight: 600;
  color: #67c23a;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.answer-detail {
  margin-bottom: 20px;
}

.answer-content-section,
.feedback-section {
  margin-top: 20px;
}

.answer-text,
.feedback-text {
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  min-height: 100px;
  white-space: pre-wrap;
  line-height: 1.6;
}

/* 查看模式切换样式 */
.view-mode-card {
  margin-bottom: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.view-mode-card :deep(.el-card__body) {
  padding: 20px 24px;
}

.view-mode-switcher {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.view-mode-switcher h3 {
  color: white;
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.mode-radio-group {
  gap: 12px;
}

.mode-radio-group :deep(.el-radio-button__inner) {
  padding: 12px 24px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.mode-radio-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: #409eff;
  border-color: #409eff;
  color: white;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  transform: translateY(-1px);
}

.mode-radio-group :deep(.el-radio-button__inner:hover) {
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 模板信息展示样式 */
.template-info .info-item {
  margin-bottom: 8px;
  font-size: 14px;
}

.template-info .label {
  color: #606266;
  font-weight: 500;
  margin-right: 8px;
}

.class-selection-wrapper .select-all-controls {
  margin-bottom: 8px;
}
</style>
