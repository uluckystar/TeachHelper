<template>
  <div class="knowledge-base-settings">
    <!-- 设置选项卡 -->
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 基本信息 -->
      <el-tab-pane label="基本信息" name="basic">
        <div class="settings-section">
          <h3>知识库基本信息</h3>
          <el-form 
            :model="basicForm" 
            :rules="basicRules"
            ref="basicFormRef"
            label-width="120px"
            class="settings-form"
          >
            <el-form-item label="知识库名称" prop="name">
              <el-input 
                v-model="basicForm.name" 
                placeholder="请输入知识库名称"
                maxlength="50"
                show-word-limit
              />
            </el-form-item>
            
            <el-form-item label="描述" prop="description">
              <el-input 
                v-model="basicForm.description" 
                type="textarea" 
                :rows="4"
                placeholder="请输入知识库描述"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
            
            <el-form-item label="学科" prop="subject">
              <el-select 
                v-model="basicForm.subject" 
                placeholder="请选择学科"
                style="width: 100%"
              >
                <el-option 
                  v-for="subject in EDUCATION_SUBJECTS"
                  :key="subject"
                  :label="subject"
                  :value="subject"
                />
              </el-select>
            </el-form-item>
            
            <el-form-item label="年级" prop="gradeLevel">
              <el-select 
                v-model="basicForm.gradeLevel" 
                placeholder="请选择年级"
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
            
            <el-form-item label="标签">
              <el-tag
                v-for="tag in basicForm.tags"
                :key="tag"
                closable
                @close="removeTag(tag)"
                style="margin-right: 8px; margin-bottom: 8px;"
              >
                {{ tag }}
              </el-tag>
              <el-input
                v-if="inputVisible"
                ref="inputRef"
                v-model="inputValue"
                size="small"
                style="width: 100px;"
                @keyup.enter="handleInputConfirm"
                @blur="handleInputConfirm"
              />
              <el-button 
                v-else 
                size="small" 
                @click="showInput"
                icon="Plus"
              >
                添加标签
              </el-button>
            </el-form-item>
            
            <el-form-item label="可见性">
              <el-radio-group v-model="basicForm.visibility">
                <el-radio value="private">私有（仅自己可见）</el-radio>
                <el-radio value="public">公开（所有人可见）</el-radio>
                <el-radio value="shared">共享（特定用户可见）</el-radio>
              </el-radio-group>
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" @click="saveBasicInfo" :loading="saving">
                保存基本信息
              </el-button>
              <el-button @click="resetBasicForm">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- AI配置 -->
      <el-tab-pane label="AI配置" name="ai">
        <div class="settings-section">
          <h3>AI处理配置</h3>
          <el-form 
            :model="aiForm" 
            ref="aiFormRef"
            label-width="150px"
            class="settings-form"
          >
            <el-form-item label="自动处理">
              <el-switch 
                v-model="aiForm.autoProcessing"
                active-text="新上传的文档将自动进行AI处理"
                inactive-text="需要手动触发AI处理"
              />
            </el-form-item>
            
            <el-form-item label="处理优先级" v-if="aiForm.autoProcessing">
              <el-radio-group v-model="aiForm.processingPriority">
                <el-radio value="low">低（后台处理，不影响其他任务）</el-radio>
                <el-radio value="normal">正常</el-radio>
                <el-radio value="high">高（优先处理，占用更多资源）</el-radio>
              </el-radio-group>
            </el-form-item>
            
            <el-form-item label="提取选项">
              <el-checkbox-group v-model="aiForm.extractionOptions">
                <el-checkbox value="knowledge_points">知识点</el-checkbox>
                <el-checkbox value="key_concepts">核心概念</el-checkbox>
                <el-checkbox value="examples">例题与案例</el-checkbox>
                <el-checkbox value="formulas">公式定理</el-checkbox>
                <el-checkbox value="definitions">定义术语</el-checkbox>
                <el-checkbox value="relationships">概念关系</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            
            <el-form-item label="题目生成">
              <el-switch 
                v-model="aiForm.autoQuestionGeneration"
                active-text="自动生成练习题目"
                inactive-text="不自动生成题目"
              />
            </el-form-item>
            
            <el-form-item label="题目类型" v-if="aiForm.autoQuestionGeneration">
              <el-checkbox-group v-model="aiForm.questionTypes">
                <el-checkbox value="choice">选择题</el-checkbox>
                <el-checkbox value="blank">填空题</el-checkbox>
                <el-checkbox value="subjective">主观题</el-checkbox>
                <el-checkbox value="calculation">计算题</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            
            <el-form-item label="难度分布" v-if="aiForm.autoQuestionGeneration">
              <div class="difficulty-distribution">
                <div class="difficulty-item">
                  <span>简单：</span>
                  <el-slider 
                    v-model="aiForm.difficultyDistribution.easy" 
                    :max="100" 
                    style="width: 150px;"
                    show-input
                    size="small"
                  />
                  <span>%</span>
                </div>
                <div class="difficulty-item">
                  <span>中等：</span>
                  <el-slider 
                    v-model="aiForm.difficultyDistribution.medium" 
                    :max="100" 
                    style="width: 150px;"
                    show-input
                    size="small"
                  />
                  <span>%</span>
                </div>
                <div class="difficulty-item">
                  <span>困难：</span>
                  <el-slider 
                    v-model="aiForm.difficultyDistribution.hard" 
                    :max="100" 
                    style="width: 150px;"
                    show-input
                    size="small"
                  />
                  <span>%</span>
                </div>
              </div>
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" @click="saveAIConfig" :loading="saving">
                保存AI配置
              </el-button>
              <el-button @click="resetAIForm">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- 权限管理 -->
      <el-tab-pane label="权限管理" name="permissions">
        <div class="settings-section">
          <h3>访问权限设置</h3>
          
          <div class="permission-section">
            <h4>共享用户</h4>
            <div class="shared-users">
              <div 
                v-for="user in sharedUsers" 
                :key="user.id"
                class="user-item"
              >
                <div class="user-info">
                  <el-avatar :src="user.avatar" :size="32">
                    {{ user.name.charAt(0) }}
                  </el-avatar>
                  <div class="user-details">
                    <div class="user-name">{{ user.name }}</div>
                    <div class="user-email">{{ user.email }}</div>
                  </div>
                </div>
                <div class="user-permission">
                  <el-select v-model="user.permission" size="small">
                    <el-option value="read" label="只读" />
                    <el-option value="write" label="编辑" />
                    <el-option value="admin" label="管理员" />
                  </el-select>
                  <el-button 
                    size="small" 
                    icon="Delete" 
                    @click="removeSharedUser(user.id)"
                  />
                </div>
              </div>
              
              <div class="add-user">
                <el-input 
                  v-model="newUserEmail" 
                  placeholder="输入用户邮箱"
                  style="width: 200px;"
                />
                <el-select v-model="newUserPermission" style="width: 100px;">
                  <el-option value="read" label="只读" />
                  <el-option value="write" label="编辑" />
                </el-select>
                <el-button type="primary" @click="addSharedUser">
                  添加用户
                </el-button>
              </div>
            </div>
          </div>
          
          <div class="permission-section">
            <h4>公开设置</h4>
            <el-form label-width="120px">
              <el-form-item label="搜索可见">
                <el-switch 
                  v-model="permissionForm.searchable"
                  active-text="允许在搜索结果中显示"
                  inactive-text="不在搜索结果中显示"
                />
              </el-form-item>
              
              <el-form-item label="复制允许">
                <el-switch 
                  v-model="permissionForm.copyable"
                  active-text="允许其他用户复制此知识库"
                  inactive-text="不允许复制"
                />
              </el-form-item>
              
              <el-form-item label="导出允许">
                <el-switch 
                  v-model="permissionForm.exportable"
                  active-text="允许导出知识库数据"
                  inactive-text="不允许导出"
                />
              </el-form-item>
            </el-form>
          </div>
          
          <el-button type="primary" @click="savePermissions" :loading="saving">
            保存权限设置
          </el-button>
        </div>
      </el-tab-pane>

      <!-- 数据管理 -->
      <el-tab-pane label="数据管理" name="data">
        <div class="settings-section">
          <h3>数据统计</h3>
          <div class="data-stats">
            <div class="stat-card">
              <div class="stat-number">{{ dataStats.documentCount }}</div>
              <div class="stat-label">文档数量</div>
            </div>
            <div class="stat-card">
              <div class="stat-number">{{ dataStats.knowledgePointCount }}</div>
              <div class="stat-label">知识点数量</div>
            </div>
            <div class="stat-card">
              <div class="stat-number">{{ dataStats.questionCount }}</div>
              <div class="stat-label">题目数量</div>
            </div>
            <div class="stat-card">
              <div class="stat-number">{{ formatFileSize(dataStats.totalSize) }}</div>
              <div class="stat-label">总存储大小</div>
            </div>
          </div>
          
          <h3>数据操作</h3>
          <div class="data-actions">
            <div class="action-group">
              <h4>备份与恢复</h4>
              <div class="action-buttons">
                <el-button icon="Download" @click="exportKnowledgeBase">
                  导出知识库
                </el-button>
                <el-button icon="Upload" @click="showImportDialog = true">
                  导入备份
                </el-button>
              </div>
              <p class="action-description">
                导出包含所有文档、知识点和题目的完整备份文件
              </p>
            </div>
            
            <div class="action-group">
              <h4>数据清理</h4>
              <div class="action-buttons">
                <el-button icon="Refresh" @click="rebuildIndex">
                  重建索引
                </el-button>
                <el-button icon="DeleteFilled" @click="cleanupData">
                  清理无效数据
                </el-button>
              </div>
              <p class="action-description">
                重建搜索索引或清理孤立的数据记录
              </p>
            </div>
            
            <div class="action-group danger-zone">
              <h4>危险操作</h4>
              <div class="action-buttons">
                <el-button type="danger" icon="Delete" @click="resetKnowledgeBase">
                  重置知识库
                </el-button>
                <el-button type="danger" icon="DeleteFilled" @click="deleteKnowledgeBase">
                  删除知识库
                </el-button>
              </div>
              <p class="action-description">
                这些操作不可恢复，请谨慎使用
              </p>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 导入备份对话框 -->
    <el-dialog v-model="showImportDialog" title="导入备份" width="500px">
      <el-upload
        drag
        :auto-upload="false"
        :on-change="handleBackupFile"
        accept=".zip,.json"
        :limit="1"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          将备份文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 ZIP 或 JSON 格式的备份文件
          </div>
        </template>
      </el-upload>
      
      <template #footer>
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button type="primary" @click="importBackup">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Download, Upload, UploadFilled, Refresh, DeleteFilled } from '@element-plus/icons-vue'
import { EDUCATION_SUBJECTS, EDUCATION_GRADES, getRecommendedGrades } from '@/utils/educationOptions'
import type { KnowledgeBase } from '@/api/knowledge'

// Props
const props = defineProps<{
  knowledgeBase: KnowledgeBase | null
}>()

// Emits
const emit = defineEmits<{
  updated: []
}>()

// 响应式数据
const activeTab = ref('basic')
const saving = ref(false)
const showImportDialog = ref(false)

// 表单引用
const basicFormRef = ref()
const aiFormRef = ref()
const inputRef = ref()

// 基本信息表单
const basicForm = ref({
  name: '',
  description: '',
  subject: '',
  gradeLevel: '',
  tags: [] as string[],
  visibility: 'private'
})

// 标签输入
const inputVisible = ref(false)
const inputValue = ref('')

// AI配置表单
const aiForm = ref({
  autoProcessing: true,
  processingPriority: 'normal',
  extractionOptions: ['knowledge_points', 'key_concepts'],
  autoQuestionGeneration: false,
  questionTypes: ['choice', 'blank'],
  difficultyDistribution: {
    easy: 40,
    medium: 40,
    hard: 20
  }
})

// 权限管理
const sharedUsers = ref<SharedUser[]>([])
const newUserEmail = ref('')
const newUserPermission = ref('read')
const permissionForm = ref({
  searchable: true,
  copyable: true,
  exportable: true
})

// 数据统计
const dataStats = ref({
  documentCount: 0,
  knowledgePointCount: 0,
  questionCount: 0,
  totalSize: 0
})

// 类型定义
interface SharedUser {
  id: string
  name: string
  email: string
  avatar?: string
  permission: 'read' | 'write' | 'admin'
}

// 表单验证规则
const basicRules = {
  name: [
    { required: true, message: '请输入知识库名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  subject: [
    { required: true, message: '请选择学科', trigger: 'change' }
  ],
  gradeLevel: [
    { required: true, message: '请选择年级', trigger: 'change' }
  ]
}

// 计算属性
const availableGrades = computed(() => {
  if (!basicForm.value.subject) return EDUCATION_GRADES
  return getRecommendedGrades(basicForm.value.subject)
})

// 生命周期
onMounted(() => {
  loadSettings()
})

// 方法
const loadSettings = () => {
  if (props.knowledgeBase) {
    basicForm.value = {
      name: props.knowledgeBase.name,
      description: props.knowledgeBase.description || '',
      subject: props.knowledgeBase.subject || '',
      gradeLevel: props.knowledgeBase.gradeLevel || '',
      tags: [], // 暂时为空数组，因为接口中没有这个属性
      visibility: 'private' // 默认私有，因为接口中没有这个属性
    }

    // 加载数据统计
    dataStats.value = {
      documentCount: props.knowledgeBase.documentCount || 0,
      knowledgePointCount: props.knowledgeBase.knowledgePointCount || 0,
      questionCount: 0, // 暂时为0，因为接口中没有这个属性
      totalSize: 0 // 暂时为0，因为接口中没有这个属性
    }
  }
}

// 标签管理
const removeTag = (tag: string) => {
  const index = basicForm.value.tags.indexOf(tag)
  if (index > -1) {
    basicForm.value.tags.splice(index, 1)
  }
}

const showInput = () => {
  inputVisible.value = true
  nextTick(() => {
    inputRef.value?.focus()
  })
}

const handleInputConfirm = () => {
  if (inputValue.value && !basicForm.value.tags.includes(inputValue.value)) {
    basicForm.value.tags.push(inputValue.value)
  }
  inputVisible.value = false
  inputValue.value = ''
}

// 保存基本信息
const saveBasicInfo = async () => {
  try {
    await basicFormRef.value?.validate()
    
    saving.value = true
    
    // TODO: 调用API保存基本信息
    ElMessage.success('基本信息保存成功')
    emit('updated')
  } catch (error) {
    console.error('Save basic info failed:', error)
  } finally {
    saving.value = false
  }
}

const resetBasicForm = () => {
  loadSettings()
}

// 保存AI配置
const saveAIConfig = async () => {
  // 验证难度分布总和为100%
  const total = aiForm.value.difficultyDistribution.easy + 
                aiForm.value.difficultyDistribution.medium + 
                aiForm.value.difficultyDistribution.hard
  
  if (Math.abs(total - 100) > 1) {
    ElMessage.warning('难度分布总和必须为100%')
    return
  }

  saving.value = true
  try {
    // TODO: 调用API保存AI配置
    ElMessage.success('AI配置保存成功')
  } catch (error) {
    ElMessage.error('保存AI配置失败')
    console.error('Save AI config failed:', error)
  } finally {
    saving.value = false
  }
}

const resetAIForm = () => {
  aiForm.value = {
    autoProcessing: true,
    processingPriority: 'normal',
    extractionOptions: ['knowledge_points', 'key_concepts'],
    autoQuestionGeneration: false,
    questionTypes: ['choice', 'blank'],
    difficultyDistribution: {
      easy: 40,
      medium: 40,
      hard: 20
    }
  }
}

// 权限管理
const addSharedUser = async () => {
  if (!newUserEmail.value) {
    ElMessage.warning('请输入用户邮箱')
    return
  }

  // 检查用户是否已存在
  if (sharedUsers.value.some(user => user.email === newUserEmail.value)) {
    ElMessage.warning('该用户已在共享列表中')
    return
  }

  try {
    // TODO: 调用API添加共享用户
    const newUser: SharedUser = {
      id: Date.now().toString(),
      name: newUserEmail.value.split('@')[0],
      email: newUserEmail.value,
      permission: newUserPermission.value as any
    }
    
    sharedUsers.value.push(newUser)
    newUserEmail.value = ''
    newUserPermission.value = 'read'
    
    ElMessage.success('用户添加成功')
  } catch (error) {
    ElMessage.error('添加用户失败')
    console.error('Add shared user failed:', error)
  }
}

const removeSharedUser = async (userId: string) => {
  try {
    await ElMessageBox.confirm('确定要移除该用户的访问权限吗？', '确认移除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    // TODO: 调用API移除共享用户
    const index = sharedUsers.value.findIndex(user => user.id === userId)
    if (index > -1) {
      sharedUsers.value.splice(index, 1)
    }
    
    ElMessage.success('用户移除成功')
  } catch (error) {
    // 用户取消
  }
}

const savePermissions = async () => {
  saving.value = true
  try {
    // TODO: 调用API保存权限设置
    ElMessage.success('权限设置保存成功')
  } catch (error) {
    ElMessage.error('保存权限设置失败')
    console.error('Save permissions failed:', error)
  } finally {
    saving.value = false
  }
}

// 数据管理
const exportKnowledgeBase = () => {
  // TODO: 实现知识库导出
  ElMessage.info('导出功能开发中...')
}

const handleBackupFile = (file: any) => {
  // TODO: 处理备份文件
  console.log('Backup file:', file)
}

const importBackup = () => {
  // TODO: 实现备份导入
  ElMessage.info('导入功能开发中...')
  showImportDialog.value = false
}

const rebuildIndex = async () => {
  try {
    await ElMessageBox.confirm('重建索引可能需要较长时间，确定要继续吗？', '确认重建', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })

    // TODO: 调用API重建索引
    ElMessage.success('索引重建已开始，请稍后查看进度')
  } catch (error) {
    // 用户取消
  }
}

const cleanupData = async () => {
  try {
    await ElMessageBox.confirm('数据清理将删除无效的记录，确定要继续吗？', '确认清理', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    // TODO: 调用API清理数据
    ElMessage.success('数据清理完成')
  } catch (error) {
    // 用户取消
  }
}

const resetKnowledgeBase = async () => {
  try {
    await ElMessageBox.confirm('重置将删除所有文档、知识点和题目，但保留基本设置，此操作不可恢复！', '确认重置', {
      confirmButtonText: '确定重置',
      cancelButtonText: '取消',
      type: 'error'
    })

    // TODO: 调用API重置知识库
    ElMessage.success('知识库重置成功')
    emit('updated')
  } catch (error) {
    // 用户取消
  }
}

const deleteKnowledgeBase = async () => {
  try {
    await ElMessageBox.confirm('删除知识库将永久删除所有数据，此操作不可恢复！请确认操作。', '确认删除', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'error'
    })

    // TODO: 调用API删除知识库
    ElMessage.success('知识库删除成功')
    // 跳转到知识库列表页面
    window.location.href = '/knowledge'
  } catch (error) {
    // 用户取消
  }
}

// 工具方法
const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}
</script>

<style scoped>
.knowledge-base-settings {
  height: 100%;
}

.settings-section {
  padding: 20px;
}

.settings-section h3 {
  margin: 0 0 20px 0;
  color: #303133;
  font-size: 18px;
  font-weight: 500;
}

.settings-form {
  max-width: 600px;
}

/* 难度分布 */
.difficulty-distribution {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.difficulty-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.difficulty-item span:first-child {
  width: 50px;
  font-size: 14px;
  color: #606266;
}

.difficulty-item span:last-child {
  width: 20px;
  font-size: 14px;
  color: #606266;
}

/* 权限管理 */
.permission-section {
  margin-bottom: 30px;
}

.permission-section h4 {
  margin: 0 0 16px 0;
  color: #606266;
  font-size: 16px;
}

.shared-users {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.user-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f5f7fa;
}

.user-item:last-child {
  border-bottom: none;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-details {
  flex: 1;
}

.user-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.user-email {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.user-permission {
  display: flex;
  align-items: center;
  gap: 8px;
}

.add-user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background-color: #f8f9fa;
}

/* 数据统计 */
.data-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 30px;
}

.stat-card {
  text-align: center;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: white;
}

.stat-number {
  font-size: 28px;
  font-weight: 600;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
}

/* 数据操作 */
.data-actions {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.action-group {
  padding: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.action-group h4 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 16px;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
}

.action-description {
  margin: 0;
  font-size: 13px;
  color: #909399;
  line-height: 1.4;
}

.danger-zone {
  border-color: #f56c6c;
  background-color: #fef0f0;
}

.danger-zone h4 {
  color: #f56c6c;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .settings-section {
    padding: 16px;
  }
  
  .difficulty-item {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
  
  .user-item {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .add-user {
    flex-direction: column;
    gap: 12px;
  }
  
  .data-stats {
    grid-template-columns: 1fr;
  }
  
  .action-buttons {
    flex-direction: column;
  }
}
</style>
