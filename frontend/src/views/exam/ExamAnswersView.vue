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
              type="primary" 
              icon="Download" 
              @click="exportAllPapers"
              :loading="exportingAllPapers"
            >
              一键导出所有试卷
            </el-button>
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

        <el-table-column prop="evaluationProgress" label="批阅进度" width="120">
          <template #default="{ row }">
            <div class="evaluation-progress">
              <el-progress 
                :percentage="getEvaluationProgress(row)" 
                :status="getEvaluationProgressStatus(row)"
                :show-text="false"
                :stroke-width="8"
              />
              <span class="progress-text">{{ row.evaluatedAnswers || 0 }}/{{ row.totalQuestions || 0 }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="answeredQuestions" label="答案数量" width="120" sortable="custom">
          <template #default="{ row }">
            <span class="answer-count">{{ row.answeredQuestions || 0 }}/{{ row.totalQuestions || 0 }}</span>
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
        <el-form label-width="80px">
          <el-form-item label="导入类型" style="margin-bottom: 20px">
            <el-radio-group v-model="importType" @change="handleImportTypeChange" class="mode-radio-group">
              <el-radio-button label="file">文件导入</el-radio-button>
              <el-radio-button label="folder_upload">本地文件夹上传</el-radio-button>
              <el-radio-button label="learning">学习通答案</el-radio-button>
              <el-radio-button label="template">基于模板</el-radio-button>
              <el-radio-button label="nested_zip">嵌套压缩包</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-form>

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

        <!-- 本地文件夹上传模式 -->
        <div v-else-if="importType === 'folder_upload'">
          <el-alert
            title="本地文件夹上传说明"
            type="info"
            :closable="false"
            style="margin-bottom: 20px"
          >
            <p>📁 从本地选择文件夹，批量上传学生答案文档：</p>
            <ul>
              <li><strong>选择方式：</strong>点击选择本地文件夹或拖拽文件到上传区域</li>
              <li><strong>支持格式：</strong>Word(.doc/.docx)、PDF、图片(jpg/png)、TXT等</li>
              <li><strong>智能解析：</strong>使用AI自动解析文件名中的学生姓名和学号</li>
              <li><strong>内容处理：</strong>整个文档内容作为学生答案，保留原始格式</li>
              <li><strong>学生匹配：</strong>自动查找现有学生或创建新学生账户</li>
            </ul>
            <p><strong>💡 文件名示例：</strong>张三_20231234.docx、李四-202312345.pdf、王五_学号202309876_期末作业.doc</p>
          </el-alert>

          <el-form label-width="80px">
            <el-form-item label="目标题目" required>
              <el-select
                v-model="folderUploadQuestionId"
                placeholder="请选择要导入答案的题目"
                style="width: 100%"
                :loading="questionsLoading"
              >
                <el-option
                  v-for="question in questions"
                  :key="question.id"
                  :label="`${question.title} (${question.questionType})`"
                  :value="question.id"
                />
              </el-select>
              <div class="el-form-item__tip" style="margin-top: 5px; font-size: 12px; color: #909399;">
                所有文档内容将导入到选中的题目下
              </div>
            </el-form-item>
          </el-form>

          <!-- 文件夹上传组件直接嵌入 -->
          <div style="border: 1px solid #e4e7ed; border-radius: 8px; padding: 20px; margin: 20px 0;">
            <div class="folder-upload-section">
              <!-- 文件上传区域 -->
              <div 
                class="upload-area"
                :class="{ 'drag-over': isDragOver, 'has-files': selectedFolderFiles.length > 0 }"
                @drop="handleDrop"
                @dragover.prevent="handleDragOver"
                @dragleave="handleDragLeave"
                @click="triggerFileInput"
              >
                <input 
                  ref="fileInput"
                  type="file"
                  multiple
                  webkitdirectory
                  directory
                  @change="handleFileSelect"
                  style="display: none"
                />
                
                <div v-if="selectedFolderFiles.length === 0" class="upload-prompt">
                  <div class="upload-icon">📂</div>
                  <p>点击选择文件夹或拖拽文件到此处</p>
                  <p class="upload-hint">支持：Word(.doc/.docx)、PDF、图片(jpg/png)、TXT等格式</p>
                </div>
                
                <div v-else class="file-list">
                  <h4>已选择 {{ selectedFolderFiles.length }} 个文件</h4>
                  <div class="file-items" v-if="!isFolderUploading">
                    <div 
                      v-for="(file, index) in selectedFolderFiles.slice(0, 10)" 
                      :key="index" 
                      class="file-item"
                    >
                      <div class="file-info">
                        <span class="file-name">{{ file.name }}</span>
                        <span class="file-size">{{ formatFileSize(file.size) }}</span>
                      </div>
                      <button class="remove-file" @click="removeFolderFile(index)">✕</button>
                    </div>
                    <div v-if="selectedFolderFiles.length > 10" class="more-files">
                      ... 还有 {{ selectedFolderFiles.length - 10 }} 个文件
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- 上传进度 -->
              <div v-if="isFolderUploading" class="upload-progress">
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: folderUploadProgress + '%' }"></div>
                </div>
                <p>正在处理文件... {{ folderUploadProgress.toFixed(1) }}%</p>
              </div>
              
              <!-- 上传结果 -->
              <div v-if="folderUploadResult" class="upload-result">
                <div class="result-summary" :class="folderUploadResult.success ? 'success' : 'error'">
                  <h4>{{ folderUploadResult.success ? '✅ 上传完成' : '❌ 上传失败' }}</h4>
                  <p>{{ folderUploadResult.message }}</p>
                </div>
                
                <div v-if="folderUploadResult.details && folderUploadResult.details.length > 0" class="result-details">
                  <h5>处理详情：</h5>
                  <div class="details-list">
                    <div 
                      v-for="(detail, index) in folderUploadResult.details.slice(0, showAllFolderDetails ? folderUploadResult.details.length : 5)" 
                      :key="index"
                      class="detail-item"
                      :class="detail.includes('成功') ? 'success' : 'error'"
                    >
                      {{ detail }}
                    </div>
                    <button 
                      v-if="folderUploadResult.details.length > 5 && !showAllFolderDetails"
                      @click="showAllFolderDetails = true"
                      class="show-more-btn"
                    >
                      显示全部 {{ folderUploadResult.details.length }} 条结果
                    </button>
                  </div>
                </div>
              </div>
              
              <!-- 操作按钮 -->
              <div class="folder-upload-actions" style="margin-top: 20px; text-align: center;">
                <el-button @click="clearFolderFiles" :disabled="isFolderUploading">
                  清空文件
                </el-button>
                <el-button 
                  type="primary" 
                  @click="startFolderUpload" 
                  :loading="isFolderUploading"
                  :disabled="!folderUploadQuestionId || selectedFolderFiles.length === 0"
                >
                  {{ isFolderUploading ? '上传中...' : `开始上传 (${selectedFolderFiles.length} 个文件)` }}
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 嵌套压缩包导入模式 -->
        <div v-else-if="importType === 'nested_zip'">
          <el-alert
            title="嵌套压缩包导入说明"
            type="warning"
            :closable="false"
            style="margin-bottom: 20px"
          >
            <p>从嵌套压缩包中批量导入单个题目的学生答案，适用于以下文件结构：</p>
            <ul>
              <li><strong>科目选择：</strong>选择对应的科目</li>
              <li><strong>作业选择：</strong>选择具体的作业或实验</li>
              <li><strong>班级压缩包：</strong>如"2022计科1班-实验一.zip"</li>
              <li><strong>学生压缩包：</strong>如"201902011312-刘亚欣.zip"（学号-姓名格式）</li>
              <li><strong>答案文档：</strong>DOC、DOCX、PDF、TXT等格式的作业文件</li>
            </ul>
            <p><strong>注意：</strong>系统将自动创建不存在的学生账户（学生角色，未激活状态）</p>
          </el-alert>

          <el-form label-width="80px">
            <el-form-item label="选择科目" required>
              <el-select
                v-model="nestedZipSubject"
                placeholder="请选择科目"
                style="width: 100%"
                @change="handleNestedZipSubjectChange"
                :loading="nestedZipSubjectLoading"
              >
                <el-option
                  v-for="subject in nestedZipSubjects"
                  :key="subject"
                  :label="subject"
                  :value="subject"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="选择作业" required v-if="nestedZipSubject">
              <el-select
                v-model="nestedZipAssignment"
                placeholder="请选择作业或实验"
                style="width: 100%"
                :loading="nestedZipAssignmentLoading"
              >
                <el-option
                  v-for="assignment in nestedZipAssignments"
                  :key="assignment"
                  :label="assignment"
                  :value="assignment"
                />
              </el-select>
              <div class="el-form-item__tip" style="margin-top: 5px; font-size: 12px; color: #909399;">
                系统将从选中科目的作业目录中导入答案文件
              </div>
            </el-form-item>

            <el-form-item label="目标题目" required>
              <el-select
                v-model="nestedZipQuestionId"
                placeholder="请选择要导入答案的题目"
                style="width: 100%"
                :loading="questionsLoading"
              >
                <el-option
                  v-for="question in questions"
                  :key="question.id"
                  :label="`${question.title} (${question.questionType})`"
                  :value="question.id"
                />
              </el-select>
              <div class="el-form-item__tip" style="margin-top: 5px; font-size: 12px; color: #909399;">
                所有学生答案将导入到选中的题目下
              </div>
            </el-form-item>
          </el-form>
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
            <p><strong>高精度导入：</strong>使用已验证的试卷模板精确导入学生答案</p>
            <ul>
              <li>基于事先整理好的试卷模板，按题号精确匹配</li>
              <li>避免题目解析错误，提高导入成功率</li>
              <li>支持单个学生失败隔离，不影响其他学生</li>
              <li>详细的导入日志和错误报告</li>
            </ul>
          </el-alert>

          <!-- 模板选择区域 -->
          <div class="template-selection-section" style="margin-bottom: 30px;">
            <div class="section-header" style="display: flex; align-items: center; margin-bottom: 15px;">
              <h4 style="margin: 0; color: #409eff;">
                <el-icon><Document /></el-icon>
                选择试卷模板
              </h4>
              <el-button 
                size="small" 
                type="primary" 
                link 
                @click="loadAvailableTemplates"
                :loading="templateLoading"
                style="margin-left: auto;"
              >
                <el-icon><Refresh /></el-icon>
                刷新模板列表
              </el-button>
            </div>

            <!-- 模板加载状态 -->
            <div v-if="templateLoading" class="loading-templates" style="text-align: center; padding: 40px;">
              <el-icon class="is-loading" size="20"><Refresh /></el-icon>
              <span style="margin-left: 8px;">正在加载可用模板...</span>
            </div>

            <!-- 无模板提示 -->
            <div v-else-if="availableTemplates.length === 0" class="no-templates" style="text-align: center; padding: 40px; background: #f5f7fa; border: 1px dashed #d9ecff; border-radius: 6px;">
              <el-icon size="48" color="#c0c4cc"><Document /></el-icon>
              <p style="margin: 16px 0 8px; color: #909399;">暂无可用的试卷模板</p>
              <p style="margin: 0; color: #c0c4cc; font-size: 14px;">请先创建并配置试卷模板，然后将其标记为"就绪"状态</p>
              <el-button type="primary" style="margin-top: 16px;" @click="$router.push('/templates')">
                前往模板管理
              </el-button>
            </div>

            <!-- 模板选择卡片 - 优化版 -->
            <div v-else class="template-cards-container">
              <el-row :gutter="12">
                <el-col 
                  v-for="template in availableTemplates" 
                  :key="template.id" 
                  :span="8"
                  style="margin-bottom: 12px;"
                >
                  <el-card 
                    :class="{ 'selected-template-card': selectedTemplateId === template.id }"
                    class="template-card template-card-compact"
                    shadow="hover"
                    style="cursor: pointer; transition: all 0.3s; height: 100%;"
                    @click="selectTemplate(template)"
                  >
                    <!-- 卡片内容区域 -->
                    <div style="padding: 0;">
                      <!-- 模板标题和状态 -->
                      <div style="display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 8px;">
                        <div style="flex: 1; min-width: 0;">
                          <div style="display: flex; align-items: center; margin-bottom: 2px;">
                            <el-radio 
                              :value="selectedTemplateId" 
                              :label="template.id"
                              style="margin-right: 6px; flex-shrink: 0;"
                              @change="selectedTemplateId = template.id"
                            />
                            <span style="color: #303133; font-size: 14px; font-weight: 600; line-height: 1.3; word-break: break-all;">
                              {{ template.templateName }}
                            </span>
                          </div>
                          <div style="margin-left: 20px; color: #909399; font-size: 12px; line-height: 1.3;">
                            {{ template.examTitle || template.templateName }}
                          </div>
                        </div>
                        <el-tag 
                          :type="getTemplateStatusTagType(template.status)"
                          size="small"
                          style="margin-left: 8px; flex-shrink: 0;"
                        >
                          {{ getTemplateStatusText(template.status) }}
                        </el-tag>
                      </div>

                      <!-- 基本信息 -->
                      <div style="margin-left: 20px; margin-bottom: 8px;">
                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 6px;">
                          <div style="font-size: 11px;">
                            <span style="color: #c0c4cc;">科目：</span>
                            <span style="color: #606266;">{{ template.subject || '未指定' }}</span>
                          </div>
                          <div style="font-size: 11px;">
                            <span style="color: #c0c4cc;">年级：</span>
                            <span style="color: #606266;">{{ template.gradeLevel || '未指定' }}</span>
                          </div>
                        </div>
                        
                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 8px;">
                          <div style="font-size: 11px;">
                            <span style="color: #c0c4cc;">题目数：</span>
                            <span style="color: #409eff; font-weight: 500;">{{ template.totalQuestions || 0 }} 题</span>
                          </div>
                          <div style="font-size: 11px;">
                            <span style="color: #c0c4cc;">总分：</span>
                            <span style="color: #409eff; font-weight: 500;">{{ template.totalScore || 0 }} 分</span>
                          </div>
                        </div>

                        <div style="display: flex; justify-content: space-between; align-items: center;">
                          <el-tag size="small" type="info" style="font-size: 10px; height: 20px;">
                            {{ getTemplateTypeText(template.templateType) }}
                          </el-tag>
                          <el-button 
                            size="small" 
                            type="primary" 
                            link
                            style="font-size: 11px; padding: 0;"
                            @click.stop="viewTemplateDetails(template)"
                          >
                            查看详情
                          </el-button>
                        </div>
                      </div>

                      <!-- 状态警告 -->
                      <div v-if="template.status !== 'READY'" style="margin-top: 8px; margin-left: 20px;">
                        <div style="background: #fdf6ec; border: 1px solid #faecd8; border-radius: 4px; padding: 6px 8px;">
                          <div style="font-size: 10px; color: #e6a23c;">
                            <el-icon style="margin-right: 4px;"><Warning /></el-icon>
                            模板未就绪，建议先确认配置
                          </div>
                        </div>
                      </div>
                    </div>
                  </el-card>
                </el-col>
              </el-row>
            </div>
          </div>

          <!-- 选中模板的详细信息 -->
          <div v-if="selectedTemplate" class="selected-template-details" style="margin-bottom: 20px;">
            <el-card>
              <template #header>
                <div style="display: flex; align-items: center; justify-content: space-between;">
                  <span style="font-weight: 600; color: #409eff;">
                    <el-icon><List /></el-icon>
                    已选模板：{{ selectedTemplate.templateName }}
                  </span>
                  <div class="template-actions">
                    <el-button 
                      size="small" 
                      @click="viewTemplateDetails(selectedTemplate)"
                    >
                      <el-icon><View /></el-icon>
                      查看完整题目列表
                    </el-button>
                    <el-button 
                      v-if="selectedTemplate.status !== 'READY'"
                      type="warning" 
                      size="small" 
                      @click="markTemplateReady"
                    >
                      <el-icon><MagicStick /></el-icon>
                      标记为就绪
                    </el-button>
                  </div>
                </div>
              </template>

              <el-row :gutter="20">
                <el-col :span="8">
                  <div class="detail-item">
                    <div class="detail-label">考试标题</div>
                    <div class="detail-value">{{ selectedTemplate.examTitle || '未设置' }}</div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="detail-item">
                    <div class="detail-label">科目年级</div>
                    <div class="detail-value">{{ selectedTemplate.subject }} · {{ selectedTemplate.gradeLevel }}</div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="detail-item">
                    <div class="detail-label">题目配置</div>
                    <div class="detail-value">{{ selectedTemplate.totalQuestions }} 题 · {{ selectedTemplate.totalScore }} 分</div>
                  </div>
                </el-col>
              </el-row>

              <!-- 状态警告 -->
              <el-alert
                v-if="selectedTemplate.status !== 'READY'"
                type="warning"
                :closable="false"
                style="margin-top: 16px;"
              >
                <template #title>
                  <el-icon><MagicStick /></el-icon>
                  模板状态提醒
                </template>
                当前模板状态为"{{ getTemplateStatusText(selectedTemplate.status) }}"。为确保导入质量，建议先查看题目列表确认配置无误，然后标记为就绪状态。
              </el-alert>

              <!-- 题目类型统计预览 -->
              <div v-if="selectedTemplate.questions && selectedTemplate.questions.length > 0" class="question-stats" style="margin-top: 16px;">
                <div style="margin-bottom: 8px; font-weight: 500; color: #606266;">题目分布预览：</div>
                <div class="question-type-tags">
                  <el-tag 
                    v-for="(count, type) in getQuestionTypeStats(selectedTemplate.questions)" 
                    :key="type"
                    style="margin-right: 8px; margin-bottom: 4px;"
                    size="small"
                  >
                    {{ getQuestionTypeText(type) }}：{{ count }}题
                  </el-tag>
                </div>
              </div>
                         </el-card>
           </div>

          <!-- 导入配置表单 -->
          <el-form label-width="100px" v-if="selectedTemplate">
            <el-form-item label="选择科目">
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

<style scoped>
/* 模板选择相关样式 */
.template-card {
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.template-card:hover {
  border-color: #409eff;
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(64, 158, 255, 0.15);
}

.selected-template-card {
  border-color: #409eff;
  background-color: #f0f8ff;
}

/* 紧凑模板卡片样式 */
.template-card-compact {
  border-radius: 8px;
  overflow: hidden;
}

.template-card-compact .el-card__body {
  padding: 12px;
}

.template-card-compact:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
}

.template-card-compact.selected-template-card {
  border-color: #409eff;
  background: linear-gradient(135deg, #f0f8ff 0%, #e6f4ff 100%);
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.2);
}

.detail-item {
  margin-bottom: 16px;
}

.detail-label {
  color: #909399;
  font-size: 12px;
  margin-bottom: 4px;
}

.detail-value {
  color: #303133;
  font-size: 14px;
  font-weight: 500;
}

.question-type-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.class-selection-wrapper {
  width: 100%;
}

.select-all-controls {
  margin-bottom: 8px;
}

/* 文件夹上传内联样式 */
.folder-upload-section .upload-area {
  border: 2px dashed #d1d5db;
  border-radius: 8px;
  padding: 40px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 20px;
}

.folder-upload-section .upload-area:hover {
  border-color: #3b82f6;
  background-color: #f8fafc;
}

.folder-upload-section .upload-area.drag-over {
  border-color: #3b82f6;
  background-color: #eff6ff;
}

.folder-upload-section .upload-area.has-files {
  border-style: solid;
  border-color: #10b981;
  background-color: #f0fdf4;
}

.folder-upload-section .upload-prompt .upload-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.folder-upload-section .upload-prompt p {
  margin: 8px 0;
  color: #6b7280;
}

.folder-upload-section .upload-hint {
  font-size: 12px;
  color: #9ca3af;
}

.folder-upload-section .file-list h4 {
  margin: 0 0 16px 0;
  color: #374151;
}

.folder-upload-section .file-items {
  max-height: 200px;
  overflow-y: auto;
}

.folder-upload-section .file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  margin-bottom: 8px;
  background-color: white;
}

.folder-upload-section .file-info {
  flex: 1;
  text-align: left;
}

.folder-upload-section .file-name {
  display: block;
  font-weight: 500;
  color: #374151;
  word-break: break-all;
}

.folder-upload-section .file-size {
  font-size: 12px;
  color: #6b7280;
}

.folder-upload-section .remove-file {
  background: none;
  border: none;
  color: #ef4444;
  cursor: pointer;
  padding: 4px;
  font-size: 14px;
}

.folder-upload-section .remove-file:hover {
  background-color: #fee2e2;
  border-radius: 4px;
}

.folder-upload-section .more-files {
  text-align: center;
  padding: 8px;
  color: #6b7280;
  font-style: italic;
}

.folder-upload-section .upload-progress {
  margin: 20px 0;
}

.folder-upload-section .progress-bar {
  width: 100%;
  height: 8px;
  background-color: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 8px;
}

.folder-upload-section .progress-fill {
  height: 100%;
  background-color: #3b82f6;
  transition: width 0.3s ease;
}

.folder-upload-section .upload-result {
  margin-top: 20px;
  padding: 16px;
  border-radius: 8px;
}

.folder-upload-section .result-summary.success {
  background-color: #f0fdf4;
  border: 1px solid #bbf7d0;
}

.folder-upload-section .result-summary.error {
  background-color: #fef2f2;
  border: 1px solid #fecaca;
}

.folder-upload-section .result-summary h4 {
  margin: 0 0 8px 0;
}

.folder-upload-section .result-summary p {
  margin: 0;
  color: #6b7280;
}

.folder-upload-section .result-details {
  margin-top: 16px;
}

.folder-upload-section .result-details h5 {
  margin: 0 0 12px 0;
  color: #374151;
}

.folder-upload-section .details-list {
  max-height: 200px;
  overflow-y: auto;
}

.folder-upload-section .detail-item {
  padding: 6px 8px;
  margin-bottom: 4px;
  border-radius: 4px;
  font-size: 13px;
}

.folder-upload-section .detail-item.success {
  background-color: #f0fdf4;
  color: #166534;
}

.folder-upload-section .detail-item.error {
  background-color: #fef2f2;
  color: #dc2626;
}

.folder-upload-section .show-more-btn {
  background: none;
  border: none;
  color: #3b82f6;
  cursor: pointer;
  padding: 4px 0;
  font-size: 13px;
  text-decoration: underline;
}
</style>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
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
  List,
  FolderOpened
} from '@element-plus/icons-vue'
import { examApi } from '@/api/exam'
import { answerApi } from '@/api/answer'
import { questionApi } from '@/api/question'
import { evaluationApi } from '@/api/evaluation'
import { examPaperTemplateApi } from '@/api/examPaperTemplate'

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
const importType = ref<'file' | 'folder_upload' | 'learning' | 'template' | 'nested_zip'>('file')
const availableSubjects = ref<string[]>([])
const availableClasses = ref<string[]>([])
const selectedSubject = ref('')
const selectedClasses = ref<string[]>([])
const subjectLoading = ref(false)
const classLoading = ref(false)
const isImporting = ref(false)
const importingMessage = ref('')

// 嵌套压缩包导入相关变量
const nestedZipSubject = ref('')
const nestedZipAssignment = ref('')
const nestedZipQuestionId = ref<number | string>('')
const nestedZipSubjects = ref<string[]>([])
const nestedZipAssignments = ref<string[]>([])
const nestedZipSubjectLoading = ref(false)
const nestedZipAssignmentLoading = ref(false)
const questionsLoading = ref(false)

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



// 文件夹上传相关变量
const folderUploadQuestionId = ref<number | string>('')
const folderUploadDialogVisible = ref(false)
const selectedFolderFiles = ref<File[]>([])
const isDragOver = ref(false)
const isFolderUploading = ref(false)
const folderUploadProgress = ref(0)
const folderUploadResult = ref<any>(null)
const showAllFolderDetails = ref(false)

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
  } else if (importType.value === 'folder_upload') {
    return !folderUploadQuestionId.value
  } else if (importType.value === 'learning') {
    return !selectedSubject.value || selectedClasses.value.length === 0
  } else if (importType.value === 'template') {
    return !selectedTemplateId.value || !templateSubject.value || selectedTemplateClasses.value.length === 0
  } else if (importType.value === 'nested_zip') {
    return !nestedZipSubject.value.trim() || !nestedZipAssignment.value.trim() || !nestedZipQuestionId.value
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
const handleImportTypeChange = async (val: string | number | boolean | undefined) => {
  const type = String(val)
  console.log('[导入类型切换]', type)
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
    } else if (importType.value === 'nested_zip') {
      if (!nestedZipSubject.value.trim() || !nestedZipAssignment.value.trim() || !nestedZipQuestionId.value) {
        ElMessage.error('请选择科目、作业和目标题目')
        return
      }
      
      // 调用基于科目和作业的嵌套压缩包导入API
      console.log('开始嵌套压缩包导入:', {
        subject: nestedZipSubject.value,
        assignment: nestedZipAssignment.value,
        questionId: nestedZipQuestionId.value
      })
      
      const result = await answerApi.importNestedZipAnswersBySubject(
        nestedZipSubject.value.trim(),
        nestedZipAssignment.value.trim(),
        Number(nestedZipQuestionId.value)
      )
      
      // 显示导入结果
      const successMsg = `✅ 导入完成！成功导入 ${result.successCount} 个答案`
      const failureMsg = result.failureCount > 0 ? `，失败 ${result.failureCount} 个` : ''
      ElMessage.success(successMsg + failureMsg)
      
      // 如果有详细信息，显示通知
      if (result.details && result.details.length > 0) {
        ElMessageBox.alert(
          result.details.join('\n'),
          '导入详情',
          {
            confirmButtonText: '确定',
            type: 'info'
          }
        )
      }
      
      // 如果有错误信息，显示警告
      if (result.errors && result.errors.length > 0) {
        ElMessageBox.alert(
          result.errors.join('\n'),
          '导入错误',
          {
            confirmButtonText: '确定',
            type: 'warning'
          }
        )
      }
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
    const templatesResponse = await examPaperTemplateApi.getUserTemplates() // 获取用户模板
    
    const allTemplates = templatesResponse.data
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
    const response = await examPaperTemplateApi.getTemplate(Number(selectedTemplateId.value))
    selectedTemplate.value = response.data
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

// 获取模板状态标签类型
const getTemplateStatusTagType = (status: string): 'success' | 'primary' | 'warning' | 'info' | 'danger' => {
  const types: Record<string, 'success' | 'primary' | 'warning' | 'info' | 'danger'> = {
    'DRAFT': 'info',
    'READY': 'success',
    'APPLIED': 'primary',
    'ARCHIVED': 'warning'
  }
  return types[status] || 'info'
}

// 获取模板类型文本
const getTemplateTypeText = (type: string) => {
  const texts: Record<string, string> = {
    'MANUAL': '手动创建',
    'AI_GENERATED': 'AI生成',
    'DOCUMENT_EXTRACTED': '文档提取',
    'COPIED': '复制创建'
  }
  return texts[type] || type
}

// 选择模板
const selectTemplate = (template: any) => {
  selectedTemplateId.value = template.id
  handleTemplateChange()
}

// 查看模板详情
const viewTemplateDetails = (template: any) => {
  if (template.id) {
    // 在新标签页中打开模板详情页面
    const routeUrl = router.resolve(`/templates/${template.id}`)
    window.open(routeUrl.href, '_blank')
  }
}

// 获取题目类型统计
const getQuestionTypeStats = (questions: any[]) => {
  const stats: Record<string, number> = {}
  questions.forEach(question => {
    const type = question.questionType || 'UNKNOWN'
    stats[type] = (stats[type] || 0) + 1
  })
  return stats
}

// 获取题目类型文本
const getQuestionTypeText = (type: string) => {
  const texts: Record<string, string> = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'TRUE_FALSE': '判断题',
    'FILL_BLANK': '填空题',
    'SHORT_ANSWER': '简答题',
    'ESSAY': '论述题',
    'UNKNOWN': '未知类型'
  }
  return texts[type] || type
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
    await examPaperTemplateApi.markTemplateReady(selectedTemplate.value.id)
    
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
const exportingAllPapers = ref(false)

// 一键导出所有试卷
const exportAllPapers = async () => {
  if (paperPagination.total === 0) {
    ElMessage.warning('没有找到任何学生试卷');
    return;
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要导出考试中所有 ${paperPagination.total} 份学生试卷吗？将生成PDF格式的ZIP压缩包文件。`,
      '一键导出确认',
      {
        confirmButtonText: '确定导出',
        cancelButtonText: '取消',
        type: 'info'
      }
    );
    
    exportingAllPapers.value = true;
    
    ElNotification.info({
      title: '正在导出',
      message: `正在导出所有学生试卷为PDF格式，请稍候...`,
      duration: 5000
    });

    const response = await examApi.exportAllStudentPapers(examId.value, 'pdf');
    
    const blob = new Blob([response], { type: 'application/zip' });
    const link = document.createElement('a');
    link.href = window.URL.createObjectURL(blob);
    
    const timestamp = new Date().toISOString().slice(0, 19).replace(/[-:]/g, '').replace('T', '_');
    link.download = `所有试卷_${exam.value?.title || '考试'}_${timestamp}.zip`;
    
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(link.href);

    ElNotification.success({
      title: '导出成功',
      message: `所有学生试卷已成功导出为ZIP文件！`,
      duration: 4000
    });

  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('导出所有试卷失败:', error);
      ElMessage.error('导出所有试卷失败，请重试');
    }
  } finally {
    exportingAllPapers.value = false;
  }
};

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

// 计算批阅进度百分比
const getEvaluationProgress = (row: any) => {
  const evaluatedAnswers = row.evaluatedAnswers || 0;
  const totalQuestions = row.totalQuestions || 0;
  
  if (totalQuestions === 0) {
    return 0;
  }
  
  return Math.round((evaluatedAnswers / totalQuestions) * 100);
};

// 获取批阅进度状态
const getEvaluationProgressStatus = (row: any) => {
  const progress = getEvaluationProgress(row);
  
  if (progress === 100) {
    return 'success';
  } else if (progress > 0) {
    return undefined; // 默认蓝色
  } else {
    return 'exception'; // 红色表示未开始
  }
};

onMounted(() => {
  loadExamInfo()
  loadData() // 使用统一的数据加载方法，根据当前模式加载数据
})

// 嵌套压缩包科目变化处理
const handleNestedZipSubjectChange = async () => {
  nestedZipAssignment.value = ''
  nestedZipAssignments.value = []
  if (nestedZipSubject.value) {
    await loadNestedZipAssignments()
  }
}

// 加载嵌套压缩包可用科目
const loadNestedZipSubjects = async () => {
  try {
    nestedZipSubjectLoading.value = true
    nestedZipSubjects.value = await answerApi.getNestedZipSubjects()
  } catch (error) {
    console.error('Failed to load nested zip subjects:', error)
    ElMessage.error('加载科目列表失败')
  } finally {
    nestedZipSubjectLoading.value = false
  }
}

// 加载指定科目下的作业/实验列表
const loadNestedZipAssignments = async () => {
  try {
    nestedZipAssignmentLoading.value = true
    nestedZipAssignments.value = await answerApi.getNestedZipAssignments(nestedZipSubject.value)
  } catch (error) {
    console.error('Failed to load nested zip assignments:', error)
    ElMessage.error('加载作业列表失败')
  } finally {
    nestedZipAssignmentLoading.value = false
  }
}



// 文件夹上传方法
const openFolderUploadDialog = () => {
  if (!folderUploadQuestionId.value) {
    ElMessage.warning('请先选择目标题目')
    return
  }
  
  folderUploadDialogVisible.value = true
}

const handleFolderUploadSuccess = (result: any) => {
  folderUploadDialogVisible.value = false
  ElMessage.success('文件夹上传成功！')
  // 刷新答案列表
  loadData()
}

// 文件夹上传内联方法
const triggerFileInput = () => {
  if (isFolderUploading.value) return
  const fileInput = document.querySelector('input[type="file"][webkitdirectory]') as HTMLInputElement
  if (fileInput) {
    fileInput.click()
  }
}

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files) {
    const files = Array.from(target.files)
    addFolderFiles(files)
  }
}

const handleDrop = (event: DragEvent) => {
  event.preventDefault()
  isDragOver.value = false
  
  if (isFolderUploading.value) return
  
  if (event.dataTransfer?.files) {
    const files = Array.from(event.dataTransfer.files)
    addFolderFiles(files)
  }
}

const handleDragOver = () => {
  isDragOver.value = true
}

const handleDragLeave = () => {
  isDragOver.value = false
}

const addFolderFiles = (files: File[]) => {
  // 过滤支持的文件类型
  const supportedExtensions = ['.doc', '.docx', '.pdf', '.txt', '.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.rtf', '.odt']
  const validFiles = files.filter(file => {
    const extension = '.' + file.name.split('.').pop()?.toLowerCase()
    return supportedExtensions.includes(extension)
  })
  
  // 添加到选择的文件列表，避免重复
  const existingNames = selectedFolderFiles.value.map(f => f.name)
  const newFiles = validFiles.filter(file => !existingNames.includes(file.name))
  
  selectedFolderFiles.value.push(...newFiles)
  
  if (validFiles.length < files.length) {
    ElMessage.warning(`已过滤 ${files.length - validFiles.length} 个不支持的文件类型`)
  }
}

const removeFolderFile = (index: number) => {
  selectedFolderFiles.value.splice(index, 1)
}

const clearFolderFiles = () => {
  selectedFolderFiles.value = []
  folderUploadResult.value = null
  showAllFolderDetails.value = false
}

const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const startFolderUpload = async () => {
  if (!folderUploadQuestionId.value || selectedFolderFiles.value.length === 0) return
  
  isFolderUploading.value = true
  folderUploadProgress.value = 0
  folderUploadResult.value = null
  
  try {
    // 创建FormData
    const formData = new FormData()
    selectedFolderFiles.value.forEach(file => {
      formData.append('files', file)
    })
    formData.append('questionId', String(folderUploadQuestionId.value))
    
    // 模拟进度更新
    const progressInterval = setInterval(() => {
      if (folderUploadProgress.value < 90) {
        folderUploadProgress.value += Math.random() * 10
      }
    }, 500)
    
    // 上传文件
    const response = await answerApi.uploadFolderAnswers(formData)
    
    clearInterval(progressInterval)
    folderUploadProgress.value = 100
    
    // 处理响应
    folderUploadResult.value = response
    
    if (response.success) {
      ElMessage.success('文件夹上传成功！')
      // 刷新答案列表
      loadData()
      // 可选：关闭导入对话框
      // importDialogVisible.value = false
    } else {
      ElMessage.error('部分文件处理失败，请查看详情')
    }
    
  } catch (error: any) {
    console.error('上传失败:', error)
    folderUploadResult.value = {
      success: false,
      message: '上传失败: ' + (error.response?.data?.message || error.message),
      details: []
    }
    ElMessage.error('上传失败')
  } finally {
    isFolderUploading.value = false
  }
}

// 监听导入弹窗打开，自动加载科目
watch(importDialogVisible, async (visible) => {
  // 暂时移除大作业导入的自动加载功能
})

// 全选checkbox的indeterminate状态
const isClassesIndeterminate = computed(() => {
  return selectedClasses.value.length > 0 && selectedClasses.value.length < availableClasses.value.length
})
const isTemplateClassesIndeterminate = computed(() => {
  return selectedTemplateClasses.value.length > 0 && selectedTemplateClasses.value.length < availableClasses.value.length
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

/* 答案数量样式 */
.answer-count {
  font-weight: 600;
  color: #409eff;
}

/* 批阅进度样式 */
.evaluation-progress {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.evaluation-progress .el-progress {
  width: 80px;
}

.progress-text {
  font-size: 12px;
  color: #606266;
  font-weight: 500;
}
</style>
