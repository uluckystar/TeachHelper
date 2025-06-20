<template>
  <div class="metadata-management">
    <div class="page-header">
      <h1>基础数据管理</h1>
      <p class="page-description">管理系统中的学科和年级基础数据，用于知识库和题目分类</p>
    </div>

    <el-tabs v-model="activeTab" class="metadata-tabs">
      <el-tab-pane label="学科管理" name="subjects">
        <SubjectManagement />
      </el-tab-pane>
      
      <el-tab-pane label="年级管理" name="gradeLevels">
        <GradeLevelManagement />
      </el-tab-pane>
      
      <el-tab-pane label="数据统计" name="statistics">
        <MetadataStatistics />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import SubjectManagement from './SubjectManagementView.vue'
import GradeLevelManagement from './GradeLevelManagementView.vue'
import MetadataStatistics from '@/components/admin/MetadataStatistics.vue'

const activeTab = ref('subjects')
</script>

<style scoped>
.metadata-management {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  color: #303133;
  font-size: 24px;
  font-weight: 500;
}

.page-description {
  margin: 8px 0 0 0;
  color: #909399;
  font-size: 14px;
}

.metadata-tabs {
  margin-top: 20px;
}

:deep(.el-tabs__header) {
  margin-bottom: 20px;
}

:deep(.el-tabs__content) {
  padding: 0;
}
</style>
