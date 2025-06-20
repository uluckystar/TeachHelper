<template>
  <div class="knowledge-base-management">
    <!-- 顶部工具栏 -->
    <div class="top-toolbar">
      <div class="toolbar-left">
        <div class="breadcrumb-area">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>
              <el-icon><House /></el-icon>
              知识库
            </el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentPath">{{ currentPath }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="view-actions">
          <el-button-group>
            <el-button 
              :type="viewMode === 'grid' ? 'primary' : ''" 
              @click="viewMode = 'grid'" 
              icon="Grid"
              size="small"
            />
            <el-button 
              :type="viewMode === 'list' ? 'primary' : ''" 
              @click="viewMode = 'list'" 
              icon="List"
              size="small"
            />
          </el-button-group>
        </div>
      </div>
      
      <div class="toolbar-right">
        <!-- 增强的快速搜索区域 -->
        <div class="enhanced-search-area">
          <el-input
            v-model="quickSearchText"
            :placeholder="searchPlaceholder"
            :prefix-icon="Search"
            clearable
            size="small"
            style="width: 280px; margin-right: 8px;"
            @input="debouncedQuickSearch"
            @clear="clearQuickSearch"
            @keyup.enter="handleQuickSearch"
          />
          
          <!-- 搜索模式切换 -->
          <div class="search-mode-toggle" style="margin-right: 12px;">
            <el-button-group>
              <el-button 
                :type="searchMode === 'basic' ? 'primary' : ''" 
                @click="setSearchMode('basic')"
                size="small"
                class="search-mode-btn"
                :title="'基础搜索：按知识库名称、学科、年级等条件搜索'"
              >
                <el-icon><Search /></el-icon>
                <span>基础搜索</span>
              </el-button>
              <el-button 
                :type="searchMode === 'smart' ? 'primary' : ''" 
                @click="setSearchMode('smart')"
                size="small"
                class="search-mode-btn"
                :title="'AI智能搜索：根据语义理解查找最相关的内容'"
              >
                <el-icon><MagicStick /></el-icon>
                <span>AI智能搜索</span>
              </el-button>
            </el-button-group>
          </div>
          
          <!-- 快速筛选下拉菜单 -->
          <el-dropdown trigger="click" @command="handleQuickFilter">
            <el-button size="small" style="margin-right: 12px;">
              筛选 <el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item divided>
                  <span style="font-weight: bold;">学科筛选</span>
                </el-dropdown-item>
                <el-dropdown-item command="subject:">全部学科</el-dropdown-item>
                <el-dropdown-item 
                  v-for="subject in subjects" 
                  :key="subject"
                  :command="`subject:${subject}`"
                >
                  {{ subject }}
                </el-dropdown-item>
                <el-dropdown-item divided>
                  <span style="font-weight: bold;">年级筛选</span>
                </el-dropdown-item>
                <el-dropdown-item command="grade:">全部年级</el-dropdown-item>
                <el-dropdown-item 
                  v-for="grade in gradeLevels" 
                  :key="grade"
                  :command="`grade:${grade}`"
                >
                  {{ grade }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        
        <el-button type="primary" icon="Plus" @click="showCreateDialog = true">
          创建知识库
        </el-button>
        <el-button icon="Upload" @click="showBatchUploadDialog = true">
          批量上传
        </el-button>
        <el-dropdown @command="handleToolbarAction">
          <el-button icon="MoreFilled" />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="refresh" icon="Refresh">刷新</el-dropdown-item>
              <el-dropdown-item command="export" icon="Download">导出数据</el-dropdown-item>
              <el-dropdown-item command="import" icon="Upload">导入数据</el-dropdown-item>
              <el-dropdown-item divided command="settings" icon="Setting">设置</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 搜索状态提示条 -->
    <div class="search-status-bar" v-if="hasActiveSearch">
      <div class="search-status-content">
        <div class="search-info">
          <span class="search-mode-indicator">
            <el-icon>
              <MagicStick v-if="searchMode === 'smart'" />
              <Search v-else />
            </el-icon>
            <span class="mode-text">
              {{ searchMode === 'smart' ? 'AI智能搜索' : '基础搜索' }}
            </span>
          </span>
          <span v-if="quickSearchText" class="search-keyword">
            关键词: "{{ quickSearchText }}"
          </span>
          <span v-if="activeSubjectFilter" class="search-filter">
            <el-icon><Reading /></el-icon>
            学科: {{ activeSubjectFilter }}
          </span>
          <span v-if="activeGradeFilter" class="search-filter">
            <el-icon><School /></el-icon>
            年级: {{ activeGradeFilter }}
          </span>
        </div>
        <div class="search-actions">
          <span class="result-count">
            <el-icon><Document /></el-icon>
            共找到 {{ total }} 个知识库
          </span>
          <el-button 
            v-if="searchMode === 'smart' && vectorSearchResults.length > 0" 
            size="small" 
            link 
            @click="showVectorSearchResults = true"
            class="action-btn"
          >
            <el-icon><View /></el-icon>
            查看AI搜索详情
          </el-button>
          <el-button 
            size="small" 
            link 
            @click="clearAllSearch"
            class="action-btn clear-btn"
          >
            <el-icon><RefreshLeft /></el-icon>
            清除所有搜索
          </el-button>
        </div>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="main-content">
      <!-- 侧边栏 -->
      <div class="sidebar">
        <div class="sidebar-section">
          <h4>快速访问</h4>
          <ul class="quick-access-list">
            <li class="access-item" :class="{ active: activeFilter === 'all' }" @click="setFilter('all')">
              <el-icon><Collection /></el-icon>
              <span>全部知识库</span>
              <span class="count">{{ knowledgeBases.length }}</span>
            </li>
            <li class="access-item" :class="{ active: activeFilter === 'recent' }" @click="setFilter('recent')">
              <el-icon><Timer /></el-icon>
              <span>最近使用</span>
            </li>
            <li class="access-item" :class="{ active: activeFilter === 'starred' }" @click="setFilter('starred')">
              <el-icon><Star /></el-icon>
              <span>已收藏</span>
            </li>
            <li class="access-item" :class="{ active: activeFilter === 'mine' }" @click="setFilter('mine')">
              <el-icon><User /></el-icon>
              <span>我创建的</span>
            </li>
          </ul>
        </div>
        
        <div class="sidebar-section">
          <h4>按学科分类</h4>
          <ul class="category-list">
            <li 
              v-for="subject in subjects" 
              :key="subject"
              class="category-item"
              :class="{ active: activeSubject === subject }"
              @click="setSubjectFilter(subject)"
            >
              <el-icon><FolderOpened /></el-icon>
              <span>{{ subject }}</span>
            </li>
          </ul>
        </div>

        <div class="sidebar-section">
          <h4>高级搜索</h4>
          <el-button type="primary" size="small" @click="showAdvancedSearch = true" style="width: 100%;">
            <el-icon><Search /></el-icon>
            智能搜索
          </el-button>
        </div>
      </div>

      <!-- 内容区域 -->
      <div class="content-area">
        <!-- 操作栏 -->
        <div class="action-bar" v-if="selectedItems.length > 0">
          <div class="selected-info">
            已选择 {{ selectedItems.length }} 个知识库
          </div>
          <div class="batch-actions">
            <el-button size="small" @click="batchDownload">批量下载</el-button>
            <el-button size="small" @click="batchDelete" type="danger">批量删除</el-button>
            <el-button size="small" @click="selectedItems = []">取消选择</el-button>
          </div>
        </div>

        <!-- 排序和筛选栏 -->
        <div class="sort-filter-bar">
          <div class="left-controls">
            <el-checkbox 
              :model-value="selectedItems.length === knowledgeBases.length && knowledgeBases.length > 0"
              :indeterminate="selectedItems.length > 0 && selectedItems.length < knowledgeBases.length"
              @change="selectAll"
            >
              全选
            </el-checkbox>
            <el-divider direction="vertical" />
            <span class="item-count">
              <el-icon><Collection /></el-icon>
              共 {{ knowledgeBases.length }} 个知识库
            </span>
          </div>
          
          <div class="right-controls">
            <div class="sort-label">排序方式：</div>
            <el-select 
              v-model="sortBy" 
              size="small" 
              @change="applySorting"
              style="width: 140px; margin-right: 12px;"
            >
              <el-option label="按名称" value="name" />
              <el-option label="按创建时间" value="createdAt" />
              <el-option label="按文档数量" value="documentCount" />
              <el-option label="按知识点数量" value="knowledgePointCount" />
            </el-select>
            <div class="sort-order-controls">
              <el-button-group size="small">
                <el-button 
                  :type="sortOrder === 'asc' ? 'primary' : ''" 
                  @click="setSortOrder('asc')"
                  class="sort-btn"
                  :title="'升序排列'"
                >
                  <el-icon><Top /></el-icon>
                  <span>升序</span>
                </el-button>
                <el-button 
                  :type="sortOrder === 'desc' ? 'primary' : ''" 
                  @click="setSortOrder('desc')"
                  class="sort-btn"
                  :title="'降序排列'"
                >
                  <el-icon><Bottom /></el-icon>
                  <span>降序</span>
                </el-button>
              </el-button-group>
            </div>
          </div>
        </div>

        <!-- 知识库网格视图 -->
        <div v-if="viewMode === 'grid'" class="grid-container" v-loading="loading">
          <div class="kb-grid">
            <div
              v-for="kb in filteredKnowledgeBases"
              :key="kb.id"
              class="kb-card"
              :class="{ selected: isSelected(kb) }"
              @click="handleCardClick(kb, $event)"
              @dblclick="viewKnowledgeBase(kb)"
            >
              <!-- 选择框 -->
              <div class="card-checkbox" @click.stop="selectItem(kb)">
                <el-checkbox :model-value="isSelected(kb)" />
              </div>
              
              <!-- 卡片头部 -->
              <div class="kb-card-header">
                <!-- 知识库图标 -->
                <div class="kb-icon">
                  <el-icon><Collection /></el-icon>
                </div>
                <!-- 收藏状态 -->
                <div class="favorite-indicator" v-if="kb.isFavorited">
                  <el-icon><StarFilled /></el-icon>
                </div>
              </div>
              
              <!-- 知识库信息 -->
              <div class="kb-info">
                <div class="kb-name" :title="kb.name">{{ kb.name }}</div>
                <div class="kb-description" v-if="kb.description" :title="kb.description">
                  {{ kb.description }}
                </div>
                <div class="kb-meta">
                  <span v-if="kb.subject" class="subject">{{ kb.subject }}</span>
                  <span v-if="kb.gradeLevel" class="grade">{{ kb.gradeLevel }}</span>
                </div>
                <div class="kb-stats">
                  <div class="stat">
                    <div class="stat-number">{{ kb.documentCount || 0 }}</div>
                    <div class="stat-label">文档</div>
                  </div>
                  <div class="stat">
                    <div class="stat-number">{{ kb.knowledgePointCount || 0 }}</div>
                    <div class="stat-label">知识点</div>
                  </div>
                  <div class="stat">
                    <div class="stat-number">{{ kb.questionCount || 0 }}</div>
                    <div class="stat-label">题目</div>
                  </div>
                </div>
                <div class="kb-footer">
                  <div class="kb-time">
                    <el-icon><Timer /></el-icon>
                    {{ formatDate(kb.createdAt) }}
                  </div>
                  <!-- 操作按钮 -->
                  <div class="card-actions" @click.stop>
                    <el-dropdown @command="(cmd) => handleCardAction({ action: cmd, kb })">
                      <el-button link icon="MoreFilled" size="small" class="action-trigger" />
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="view" icon="View">查看详情</el-dropdown-item>
                          <el-dropdown-item command="edit" icon="Edit">编辑</el-dropdown-item>
                          <el-dropdown-item command="upload" icon="Upload">上传文档</el-dropdown-item>
                          <el-dropdown-item command="generate" icon="MagicStick">AI出题</el-dropdown-item>
                          <el-dropdown-item command="star" :icon="kb.isFavorited ? 'StarFilled' : 'Star'">
                            {{ kb.isFavorited ? '取消收藏' : '收藏' }}
                          </el-dropdown-item>
                          <el-dropdown-item divided command="delete" icon="Delete">删除</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 知识库列表视图 -->
        <div v-else class="list-container" v-loading="loading">
          <el-table 
            :data="filteredKnowledgeBases" 
            @selection-change="handleSelectionChange"
            @row-dblclick="viewKnowledgeBase"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column prop="name" label="名称" min-width="200">
              <template #default="{ row }">
                <div class="name-cell">
                  <el-icon class="file-icon"><Collection /></el-icon>
                  <span class="name-text">{{ row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="subject" label="学科" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.subject" size="small">{{ row.subject }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="gradeLevel" label="年级" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.gradeLevel" size="small" type="success">{{ row.gradeLevel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="documentCount" label="文档数" width="100" />
            <el-table-column prop="knowledgePointCount" label="知识点" width="100" />
            <el-table-column prop="createdAt" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button link @click="viewKnowledgeBase(row)">查看</el-button>
                <el-button link @click="editKnowledgeBase(row)">编辑</el-button>
                <el-button link @click="showUploadDialog(row)">上传</el-button>
                <el-dropdown @command="(command) => handleTableAction(command, row)">
                  <el-button link>更多<el-icon><MoreFilled /></el-icon></el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="generate">AI出题</el-dropdown-item>
                      <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 空状态 -->
        <div v-if="filteredKnowledgeBases.length === 0 && !loading" class="empty-state">
          <el-empty description="暂无知识库">
            <el-button type="primary" @click="showCreateDialog = true">创建第一个知识库</el-button>
          </el-empty>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="total > pageSize">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[20, 50, 100, 200]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadKnowledgeBases"
            @current-change="loadKnowledgeBases"
          />
        </div>
      </div>
    </div>

    <!-- 高级搜索对话框 -->
    <el-dialog v-model="showAdvancedSearch" title="高级搜索" width="800px">
      <el-tabs v-model="searchTab" @tab-click="handleSearchTabChange">
        <!-- 基础搜索 -->
        <el-tab-pane label="基础搜索" name="basic">
          <el-form :model="searchForm" inline>
            <el-form-item label="知识库名称">
              <el-input
                v-model="searchForm.name"
                placeholder="请输入知识库名称"
                clearable
                @keyup.enter="searchKnowledgeBases"
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
            <el-form-item>
              <el-button type="primary" @click="searchKnowledgeBases">搜索</el-button>
              <el-button @click="resetSearch">重置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 向量搜索 -->
        <el-tab-pane label="智能搜索" name="vector">
          <el-form :model="vectorSearchForm" @submit.prevent="performVectorSearch">
            <el-form-item label="搜索内容">
              <el-input
                v-model="vectorSearchForm.query"
                type="textarea"
                :rows="3"
                placeholder="输入您要搜索的问题或知识点，AI会帮您找到最相关的内容"
                show-word-limit
                maxlength="500"
              />
            </el-form-item>
            <el-form-item label="搜索范围">
              <el-checkbox-group v-model="vectorSearchForm.searchScope">
                <el-checkbox label="documents">文档内容</el-checkbox>
                <el-checkbox label="knowledge_points">知识点</el-checkbox>
                <el-checkbox label="questions">题目内容</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="相似度阈值">
              <el-slider
                v-model="vectorSearchForm.similarityThreshold"
                :min="0.1"
                :max="1.0"
                :step="0.1"
                show-stops
                show-tooltip
              />
              <span class="similarity-hint">{{ getSimilarityHint(vectorSearchForm.similarityThreshold) }}</span>
            </el-form-item>
            <el-form-item>
              <el-button 
                type="primary" 
                @click="performVectorSearch"
                :loading="vectorSearchLoading"
                icon="Search"
              >
                智能搜索
              </el-button>
              <el-button @click="clearVectorSearch">清空</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 知识点分类 -->
        <el-tab-pane label="知识点分类" name="knowledge_points">
          <div class="knowledge-points-browser">
            <el-row :gutter="20">
              <el-col :span="8">
                <div class="category-tree">
                  <h4>知识点分类</h4>
                  <el-tree
                    :data="knowledgePointTree"
                    :props="treeProps"
                    node-key="id"
                    default-expand-all
                    @node-click="handleKnowledgePointSelect"
                  >
                    <template #default="{ node, data }">
                      <span class="tree-node">
                        <el-icon v-if="data.type === 'subject'"><FolderOpened /></el-icon>
                        <el-icon v-else-if="data.type === 'chapter'"><Collection /></el-icon>
                        <el-icon v-else><Document /></el-icon>
                        <span>{{ data.label }}</span>
                        <el-tag size="small" type="info" v-if="data.count">{{ data.count }}</el-tag>
                      </span>
                    </template>
                  </el-tree>
                </div>
              </el-col>
              <el-col :span="16">
                <div class="knowledge-point-content">
                  <div v-if="selectedKnowledgePoint" class="knowledge-point-details">
                    <h4>{{ selectedKnowledgePoint.label }}</h4>
                    <el-descriptions :column="2" size="small">
                      <el-descriptions-item label="类型">{{ selectedKnowledgePoint.type }}</el-descriptions-item>
                      <el-descriptions-item label="难度级别">
                        <el-rate v-model="selectedKnowledgePoint.difficulty" disabled />
                      </el-descriptions-item>
                      <el-descriptions-item label="相关文档">{{ selectedKnowledgePoint.documentCount }}</el-descriptions-item>
                      <el-descriptions-item label="生成题目">{{ selectedKnowledgePoint.questionCount }}</el-descriptions-item>
                    </el-descriptions>
                    
                    <div class="knowledge-point-actions">
                      <el-button type="primary" @click="() => generateQuestionsFromKnowledgePoint(selectedKnowledgePoint)" icon="MagicStick">
                        基于此知识点出题
                      </el-button>
                      <el-button @click="() => viewRelatedDocuments(selectedKnowledgePoint)" icon="Document">
                        查看相关文档
                      </el-button>
                    </div>

                    <!-- 相关内容预览 -->
                    <div class="related-content" v-if="relatedContent.length">
                      <h5>相关内容片段</h5>
                      <div class="content-snippets">
                        <div 
                          v-for="snippet in relatedContent" 
                          :key="snippet.id"
                          class="content-snippet"
                        >
                          <div class="snippet-header">
                            <span class="source">{{ snippet.source }}</span>
                            <el-tag size="small">相似度: {{ (snippet.similarity * 100).toFixed(1) }}%</el-tag>
                          </div>
                          <div class="snippet-content">{{ snippet.content }}</div>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div v-else class="no-selection">
                    <el-empty description="请在左侧选择知识点查看详细信息" />
                  </div>
                </div>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 向量搜索结果 -->
    <el-dialog v-model="showVectorSearchResults" title="AI智能搜索结果" width="1000px">
      <div class="search-results-header">
        <div class="header-left">
          <span class="results-count">搜索结果 ({{ vectorSearchResults.length }} 条)</span>
          <el-tag v-if="currentSearchQuery" size="small" type="primary" class="search-query-tag">
            <el-icon><Search /></el-icon>
            {{ currentSearchQuery }}
          </el-tag>
        </div>
        <div class="header-actions">
          <el-button size="small" @click="exportSearchResults" icon="Download">导出结果</el-button>
          <el-button size="small" link @click="clearVectorSearchResults">清空结果</el-button>
        </div>
      </div>
      
      <div class="search-results" v-if="vectorSearchResults.length > 0">
        <div 
          v-for="result in vectorSearchResults" 
          :key="result.id"
          class="search-result-item"
        >
          <div class="result-header">
            <div class="result-title-section">
              <div class="result-title">{{ result.title || '未命名文档' }}</div>
              <div class="result-meta">
                <el-tag size="small" :type="getResultTypeColor(result.type)">
                  {{ getResultTypeLabel(result.type) }}
                </el-tag>
                <span class="similarity-score">
                  <el-icon><Star /></el-icon>
                  相似度: {{ (result.similarity * 100).toFixed(1) }}%
                </span>
                <span v-if="result.knowledgeBaseId || result.knowledgeBaseName" class="kb-info">
                  <el-icon><Collection /></el-icon>
                  {{ result.knowledgeBaseName || `知识库ID: ${result.knowledgeBaseId}` }}
                </span>
                <span v-if="result.chunkIndex !== undefined" class="chunk-info">
                  <el-icon><Files /></el-icon>
                  第 {{ result.chunkIndex + 1 }} 段
                </span>
              </div>
            </div>
          </div>
          
          <!-- 关键词高亮内容显示 -->
          <div class="result-content-section">
            <div class="content-display">
              <!-- 如果有高亮内容，优先显示高亮版本 -->
              <div 
                v-if="result.highlightedContent" 
                class="highlighted-content"
                v-html="result.highlightedContent"
              ></div>
              <!-- 否则显示原始内容 -->
              <div v-else class="original-content">{{ result.content }}</div>
              
              <!-- 显示上下文信息 -->
              <div v-if="result.contextBefore || result.contextAfter" class="content-context">
                <div v-if="result.contextBefore" class="context-before">
                  <span class="context-label">前文：</span>
                  <span class="context-text">{{ result.contextBefore }}</span>
                </div>
                <div v-if="result.contextAfter" class="context-after">
                  <span class="context-label">后文：</span>
                  <span class="context-text">{{ result.contextAfter }}</span>
                </div>
              </div>
            </div>
            
            <!-- 匹配的关键词展示 -->
            <div v-if="result.matchedKeywords && result.matchedKeywords.length > 0" class="matched-keywords">
              <span class="keywords-label">
                <el-icon><PriceTag /></el-icon>
                匹配关键词:
              </span>
              <el-tag 
                v-for="keyword in result.matchedKeywords" 
                :key="keyword"
                size="small" 
                type="success" 
                class="keyword-tag"
              >
                {{ keyword }}
              </el-tag>
            </div>
            
            <!-- 关键词位置信息（可展开） -->
            <div v-if="result.keywordPositions && result.keywordPositions.length > 0" class="keyword-positions">
              <el-collapse size="small">
                <el-collapse-item name="positions">
                  <template #title>
                    <span class="positions-title">
                      <el-icon><Location /></el-icon>
                      关键词位置信息 ({{ result.keywordPositions.length }} 处)
                    </span>
                  </template>
                  <div class="positions-list">
                    <div 
                      v-for="(position, index) in result.keywordPositions" 
                      :key="index"
                      class="position-item"
                    >
                      <div class="position-header">
                        <el-tag size="small" type="info">{{ position.keyword }}</el-tag>
                        <span class="position-range">位置: {{ position.startIndex }}-{{ position.endIndex }}</span>
                      </div>
                      <div class="position-context">{{ position.context }}</div>
                    </div>
                  </div>
                </el-collapse-item>
              </el-collapse>
            </div>
          </div>
          
          <div class="result-footer">
            <div class="result-source">
              <el-icon><Document /></el-icon>
              来源: {{ result.source || '未知来源' }}
              <span v-if="result.knowledgeBaseName" class="kb-name">
                | 知识库: {{ result.knowledgeBaseName }}
              </span>
              <span v-if="result.documentId && result.chunkIndex !== undefined" class="chunk-info">
                | 文档块: {{ result.chunkIndex + 1 }}
              </span>
            </div>
            <div class="result-actions">
              <el-button size="small" link @click="viewResultDetail(result)" icon="View">
                查看详情
              </el-button>
              <el-button size="small" link @click="generateFromResult(result)" icon="MagicStick">
                基于此内容出题
              </el-button>
              <el-button size="small" link @click="copyResultContent(result)" icon="CopyDocument">
                复制内容
              </el-button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 空状态 -->
      <div v-else class="search-empty-state">
        <el-empty description="未找到相关内容">
          <el-button type="primary" @click="showAdvancedSearch = true">
            重新搜索
          </el-button>
        </el-empty>
      </div>
    </el-dialog>

    <!-- 创建/编辑知识库对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="editingKb ? '编辑知识库' : '创建知识库'"
      width="600px"
    >
      <el-form ref="kbFormRef" :model="kbForm" :rules="kbFormRules" label-width="100px">
        <el-form-item label="知识库名称" prop="name">
          <el-input v-model="kbForm.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="kbForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入知识库描述"
          />
        </el-form-item>
        <el-form-item label="年级" prop="gradeLevel">
          <el-select v-model="kbForm.gradeLevel" placeholder="请先选择年级" clearable @change="onGradeLevelChange">
            <el-option
              v-for="grade in availableGrades"
              :key="grade"
              :label="grade"
              :value="grade"
            />
          </el-select>
          <div v-if="selectedGradeCategory" class="grade-category-info">
            <el-tag size="small" type="info">{{ selectedGradeCategory }}</el-tag>
          </div>
        </el-form-item>
        <el-form-item label="学科" prop="subject">
          <div class="subject-input-container">
            <el-select 
              v-model="kbForm.subject" 
              placeholder="请选择学科或输入新学科" 
              clearable
              filterable
              allow-create
              default-first-option
              style="flex: 1;"
              @change="onSubjectChange"
            >
              <el-option-group
                v-if="recommendedSubjects.length > 0 && kbForm.gradeLevel"
                label="推荐学科"
              >
                <el-option
                  v-for="subject in recommendedSubjects"
                  :key="subject"
                  :label="subject"
                  :value="subject"
                >
                  <span>{{ subject }}</span>
                  <el-tag size="small" type="success" style="margin-left: 8px;">推荐</el-tag>
                </el-option>
              </el-option-group>
              <el-option-group
                v-if="otherSubjects.length > 0 && kbForm.gradeLevel"
                label="其他学科"
              >
                <el-option
                  v-for="subject in otherSubjects"
                  :key="subject"
                  :label="subject"
                  :value="subject"
                />
              </el-option-group>
              <!-- 如果没有选择年级，显示所有学科 -->
              <template v-if="!kbForm.gradeLevel">
                <el-option
                  v-for="subject in subjects"
                  :key="subject"
                  :label="subject"
                  :value="subject"
                />
              </template>
            </el-select>
            <el-button 
              type="text" 
              icon="Plus" 
              @click="showCreateSubjectDialog = true"
              style="margin-left: 8px;"
              title="创建新学科"
            >
              新增
            </el-button>
          </div>
          <div v-if="!kbForm.gradeLevel" class="form-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>请先选择年级，系统将为您推荐合适的学科</span>
          </div>
          <div v-else-if="selectedGradeCategory" class="form-tip">
            <el-icon><Star /></el-icon>
            <span>已为{{ selectedGradeCategory }}阶段推荐{{ recommendedSubjects.length }}个常用学科</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCreateDialog = false">取消</el-button>
          <el-button type="primary" @click="saveKnowledgeBase" :loading="saving">
            {{ editingKb ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 文档上传对话框 -->
    <DocumentUploadDialog
      v-model="showUploadDocDialog"
      :knowledge-base="selectedKb"
      @uploaded="handleDocumentUploaded"
    />

    <!-- AI出题对话框 -->
    <AIQuestionGenerationDialog
      v-model="showAIQuestionDialog"
      :knowledge-base="selectedKb"
      @generated="handleQuestionsGenerated"
    />

    <!-- 创建新学科对话框 -->
    <el-dialog
      v-model="showCreateSubjectDialog"
      title="创建新学科"
      width="500px"
    >
      <el-form ref="newSubjectFormRef" :model="newSubjectForm" :rules="newSubjectFormRules" label-width="100px">
        <el-form-item label="学科名称" prop="name">
          <el-input v-model="newSubjectForm.name" placeholder="请输入学科名称" />
        </el-form-item>
        <el-form-item label="适用年级" prop="applicableGrades">
          <el-select 
            v-model="newSubjectForm.applicableGrades" 
            multiple 
            placeholder="选择适用的年级（可多选）"
            style="width: 100%;"
          >
            <el-option
              v-for="grade in gradeLevels"
              :key="grade"
              :label="grade"
              :value="grade"
            />
          </el-select>
          <div class="form-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>选择该学科适用的年级，有助于系统智能推荐</span>
          </div>
        </el-form-item>
        <el-form-item label="学科描述">
          <el-input
            v-model="newSubjectForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入学科描述（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancelCreateSubject">取消</el-button>
          <el-button type="primary" @click="saveNewSubject" :loading="saving">
            创建学科
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Search, 
  House, 
  Grid, 
  List, 
  MoreFilled, 
  Plus, 
  Upload, 
  Refresh, 
  Download, 
  Setting,
  FolderOpened,
  Collection,
  Document,
  MagicStick,
  View,
  Edit,
  Delete,
  Star,
  StarFilled,
  Timer,
  User,
  ArrowDown,
  Sort,
  SortDown,
  InfoFilled,
  Close,
  Reading,
  School,
  RefreshLeft,
  Top,
  Bottom,
  PriceTag,
  Location,
  CopyDocument,
  Files
} from '@element-plus/icons-vue'
import { knowledgeBaseApi, type KnowledgeBase, type KnowledgeBaseCreateRequest } from '@/api/knowledge'
import { subjectApi, gradeLevelApi } from '@/api/metadata'
import DocumentUploadDialog from './components/DocumentUploadDialog.vue'
import AIQuestionGenerationDialog from './components/AIQuestionGenerationDialog.vue'
import { useRouter } from 'vue-router'

// 响应式数据
const loading = ref(false)
const saving = ref(false)
const knowledgeBases = ref<KnowledgeBase[]>([])
const viewMode = ref<'grid' | 'list'>('grid')
const showCreateDialog = ref(false)
const showUploadDocDialog = ref(false)
const showAIQuestionDialog = ref(false)
const showBatchUploadDialog = ref(false)
const showCreateSubjectDialog = ref(false)
const editingKb = ref<KnowledgeBase | null>(null)
const selectedKb = ref<KnowledgeBase | null>(null)

// 新增的UI状态
const currentPath = ref('')
const quickSearchText = ref('')
const selectedItems = ref<KnowledgeBase[]>([])
const sortBy = ref('name')
const sortOrder = ref<'asc' | 'desc'>('asc')

// 分页
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 搜索相关
const searchTab = ref('basic')
const vectorSearchLoading = ref(false)
const showAdvancedSearch = ref(false)
const showVectorSearchResults = ref(false)

// 新增筛选状态
const activeFilter = ref('all')
const activeSubject = ref('')

// 增强搜索状态
const searchMode = ref<'basic' | 'smart'>('basic')
const activeSubjectFilter = ref('')
const activeGradeFilter = ref('')

// 搜索表单
const searchForm = reactive({
  name: '',
  subject: '',
  gradeLevel: ''
})

// 向量搜索表单
const vectorSearchForm = reactive({
  query: '',
  searchScope: ['documents', 'knowledge_points', 'questions'],
  similarityThreshold: 0.7
})

// 知识库表单
const kbForm = reactive<KnowledgeBaseCreateRequest>({
  name: '',
  description: '',
  subject: '',
  gradeLevel: ''
})

const kbFormRef = ref()
const kbFormRules = {
  name: [
    { required: true, message: '请输入知识库名称', trigger: 'blur' },
    { min: 2, max: 100, message: '名称长度在 2 到 100 个字符', trigger: 'blur' }
  ]
}

// 新增学科表单
const newSubjectForm = reactive({
  name: '',
  description: '',
  applicableGrades: [] as string[]
})

const newSubjectFormRef = ref()
const newSubjectFormRules = {
  name: [
    { required: true, message: '请输入学科名称', trigger: 'blur' },
    { min: 2, max: 50, message: '学科名称长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  applicableGrades: [
    { type: 'array' as const, min: 1, message: '请至少选择一个适用年级', trigger: 'change' }
  ]
}

// 选项数据
const subjects = ref<string[]>([])
const gradeLevels = ref<string[]>([])
const recommendedGrades = ref<string[]>([])

// 新增：年级学科智能推荐相关数据
const selectedGradeCategory = ref<string>('')
const recommendedSubjects = ref<string[]>([])
const otherSubjects = ref<string[]>([])
const allSubjects = ref<string[]>([])

// 计算属性 - 可用年级列表
const availableGrades = computed(() => {
  return gradeLevels.value
})

// 搜索结果
const vectorSearchResults = ref<any[]>([])
const currentSearchQuery = ref('')

// 知识点相关
const knowledgePointTree = ref<any[]>([])
const selectedKnowledgePoint = ref<any>(null)
const relatedContent = ref<any[]>([])
const treeProps = {
  label: 'label',
  children: 'children'
}

// 路由
const router = useRouter()

// 生命周期
onMounted(() => {
  loadKnowledgeBases()
  loadSubjects()
  loadGradeLevels()
  loadKnowledgePointTree()
})

// 组件初始化
onMounted(async () => {
  try {
    // 并行加载数据
    await Promise.all([
      loadKnowledgeBases(),
      loadSubjects(),
      loadGradeLevels()
    ])
  } catch (error) {
    console.error('Component initialization failed:', error)
    ElMessage.error('初始化失败，请刷新页面重试')
  }
})

// 方法
const loadKnowledgeBases = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
      ...searchForm,
      // 将快速搜索文本作为名称搜索参数传递
      name: quickSearchText.value || searchForm.name,
      // 添加排序参数
      sort: sortBy.value,
      direction: sortOrder.value
    }
    
    // 如果有学科筛选，也添加到搜索参数中
    if (activeSubject.value) {
      params.subject = activeSubject.value
    }
    
    const response = await knowledgeBaseApi.getKnowledgeBases(params)
    knowledgeBases.value = response.content
    total.value = response.totalElements
  } catch (error) {
    ElMessage.error('加载知识库列表失败')
  } finally {
    loading.value = false
  }
}

// 新增方法
const handleQuickSearch = async () => {
  // 快速搜索：处理基础搜索和智能搜索
  currentPage.value = 1
  
  if (searchMode.value === 'smart' && quickSearchText.value.trim()) {
    // 执行智能搜索
    await performSmartSearch()
  } else {
    // 执行基础搜索
    loadKnowledgeBases()
  }
}

// 防抖版本的快速搜索
let searchTimeout: number | null = null
const debouncedQuickSearch = () => {
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
  searchTimeout = setTimeout(() => {
    handleQuickSearch()
  }, 300) // 300ms防抖
}

const clearQuickSearch = () => {
  quickSearchText.value = ''
  // 清空后重新搜索
  handleQuickSearch()
}

// 新增的增强搜索方法
const setSearchMode = (mode: 'basic' | 'smart') => {
  searchMode.value = mode
  // 清空向量搜索结果
  vectorSearchResults.value = []
  showVectorSearchResults.value = false
  // 重新执行搜索
  if (quickSearchText.value.trim()) {
    handleQuickSearch()
  }
}

const handleQuickFilter = (command: string) => {
  const [type, value] = command.split(':')
  
  if (type === 'subject') {
    activeSubjectFilter.value = value
    searchForm.subject = value
  } else if (type === 'grade') {
    activeGradeFilter.value = value
    searchForm.gradeLevel = value
  }
  
  // 重新搜索
  currentPage.value = 1
  loadKnowledgeBases()
}

const clearAllSearch = () => {
  quickSearchText.value = ''
  activeSubjectFilter.value = ''
  activeGradeFilter.value = ''
  searchMode.value = 'basic'
  
  // 重置搜索表单
  Object.assign(searchForm, {
    name: '',
    subject: '',
    gradeLevel: ''
  })
  
  // 重新加载数据
  currentPage.value = 1
  loadKnowledgeBases()
}

// 计算属性：是否有活跃的搜索
const hasActiveSearch = computed(() => {
  return !!(quickSearchText.value || activeSubjectFilter.value || activeGradeFilter.value)
})

// 计算属性：动态搜索提示文字
const searchPlaceholder = computed(() => {
  if (searchMode.value === 'smart') {
    return '🧠 AI智能搜索：描述您要找的内容，如"高一数学函数的应用题"、"初中物理力学概念"等'
  } else {
    return '🔍 基础搜索：输入知识库名称、关键词进行快速查找'
  }
})

const handleToolbarAction = (command: string) => {
  switch (command) {
    case 'refresh':
      loadKnowledgeBases()
      break
    case 'export':
      exportData()
      break
    case 'import':
      importData()
      break
    case 'settings':
      showSettings()
      break
  }
}

const exportData = () => {
  ElMessage.info('导出功能开发中...')
}

const importData = () => {
  ElMessage.info('导入功能开发中...')
}

const showSettings = () => {
  ElMessage.info('设置功能开发中...')
}

const selectItem = (item: KnowledgeBase) => {
  const index = selectedItems.value.findIndex(i => i.id === item.id)
  if (index > -1) {
    selectedItems.value.splice(index, 1)
  } else {
    selectedItems.value.push(item)
  }
}

const selectAll = () => {
  if (selectedItems.value.length === knowledgeBases.value.length) {
    selectedItems.value = []
  } else {
    selectedItems.value = [...knowledgeBases.value]
  }
}

const isSelected = (item: KnowledgeBase) => {
  return selectedItems.value.some(i => i.id === item.id)
}

// 新增的筛选和排序方法
const setFilter = (filter: string) => {
  activeFilter.value = filter
  if (filter === 'starred') {
    loadFavoriteKnowledgeBases()
  } else {
    loadKnowledgeBases()
  }
}

const setSubjectFilter = (subject: string) => {
  if (activeSubject.value === subject) {
    // 如果点击的是当前选中的学科，则取消筛选
    activeSubject.value = ''
  } else {
    // 否则设置新的学科筛选
    activeSubject.value = subject
  }
  
  // 重置页码并重新加载数据
  currentPage.value = 1
  loadKnowledgeBases()
}

const setSortOrder = (order: 'asc' | 'desc') => {
  sortOrder.value = order
  applySorting()
}

const applySorting = () => {
  // 实现排序逻辑 - 传递排序参数给后端
  loadKnowledgeBases()
}

const handleCardClick = (kb: KnowledgeBase, event: MouseEvent) => {
  // 如果是多选模式（按住Ctrl/Cmd键），处理选择
  if (event.ctrlKey || event.metaKey) {
    selectItem(kb)
  } else {
    // 否则查看知识库详情
    viewKnowledgeBase(kb)
  }
}

const handleCardAction = async ({ action, kb }: { action: string, kb: KnowledgeBase }) => {
  switch (action) {
    case 'view':
      viewKnowledgeBase(kb)
      break
    case 'edit':
      editKnowledgeBase(kb)
      break
    case 'upload':
      selectedKb.value = kb
      showUploadDocDialog.value = true
      break
    case 'generate':
      selectedKb.value = kb
      showAIQuestionDialog.value = true
      break
    case 'star':
      await toggleKnowledgeBaseFavorite(kb)
      break
    case 'delete':
      await deleteKnowledgeBase(kb)
      break
    default:
      console.warn('Unknown action:', action)
  }
}

const handleSelectionChange = (selection: KnowledgeBase[]) => {
  selectedItems.value = selection
}

const batchDownload = () => {
  ElMessage.info('批量下载功能开发中...')
}

const batchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedItems.value.length} 个知识库吗？此操作不可恢复。`,
      '确认批量删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 批量删除逻辑
    for (const kb of selectedItems.value) {
      try {
        await knowledgeBaseApi.deleteKnowledgeBase(kb.id)
      } catch (error) {
        console.error(`删除知识库 ${kb.name} 失败:`, error)
      }
    }
    
    ElMessage.success(`批量删除完成`)
    selectedItems.value = []
    loadKnowledgeBases()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

// 导航方法
const goToUpload = () => {
  router.push('/knowledge/upload')
}

const goToKnowledgePoints = () => {
  // 切换到知识点分类标签页
  searchTab.value = 'knowledge_points'
  showAdvancedSearch.value = true
}

const goToIntelligentSearch = () => {
  // 切换到智能搜索标签页
  searchTab.value = 'vector'
  showAdvancedSearch.value = true
}

// 计算属性
const filteredKnowledgeBases = computed(() => {
  // 搜索和排序现在在后端处理，这里只处理特殊的前端筛选逻辑
  let filtered = [...knowledgeBases.value]

  // 根据activeFilter筛选（这些需要在前端处理）
  switch (activeFilter.value) {
    case 'recent':
      // 最近使用的逻辑 - 简单按更新时间排序
      filtered = filtered.sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
      break
    case 'starred':
      // 收藏的逻辑
      filtered = filtered.filter(kb => kb.isFavorited)
      break
    case 'mine':
      // 我创建的逻辑 - 暂时显示所有，实际应该根据当前用户ID筛选
      break
  }

  // 排序已在后端处理，不需要前端再排序
  // 除非是特殊筛选（如收藏、最近使用）才需要前端重新排序
  
  return filtered
})

const loadSubjects = async () => {
  try {
    // 优先从新的元数据API获取学科数据
    const apiSubjects = await subjectApi.getAllSubjectNames()
    subjects.value = apiSubjects
    allSubjects.value = apiSubjects // 保存所有学科列表
  } catch (error) {
    console.warn('Failed to load subjects from metadata API, trying knowledge base API:', error)
    try {
      // 如果元数据API失败，尝试知识库API
      const apiSubjects = await knowledgeBaseApi.getSubjects()
      subjects.value = apiSubjects
      allSubjects.value = apiSubjects
    } catch (fallbackError) {
      console.error('Failed to load subjects from both APIs:', fallbackError)
      // 如果都失败，设置为空数组
      subjects.value = []
      allSubjects.value = []
      ElMessage.warning('获取学科列表失败，请检查网络连接')
    }
  }
}

const loadGradeLevels = async () => {
  try {
    // 优先从新的元数据API获取年级数据
    const apiGrades = await gradeLevelApi.getAllGradeLevelNames()
    gradeLevels.value = apiGrades
  } catch (error) {
    console.warn('Failed to load grade levels from metadata API, trying knowledge base API:', error)
    try {
      // 如果元数据API失败，尝试知识库API
      const apiGrades = await knowledgeBaseApi.getGradeLevels()
      gradeLevels.value = apiGrades
    } catch (fallbackError) {
      console.error('Failed to load grade levels from both APIs:', fallbackError)
      // 如果都失败，设置为空数组
      gradeLevels.value = []
      ElMessage.warning('获取年级列表失败，请检查网络连接')
    }
  }
}

const searchKnowledgeBases = () => {
  currentPage.value = 1
  loadKnowledgeBases()
}

const resetSearch = () => {
  Object.assign(searchForm, {
    name: '',
    subject: '',
    gradeLevel: ''
  })
  searchKnowledgeBases()
}

const viewKnowledgeBase = (kb: KnowledgeBase) => {
  // 跳转到知识库详情页
  router.push(`/knowledge/${kb.id}`)
}

const editKnowledgeBase = (kb: KnowledgeBase) => {
  editingKb.value = kb
  Object.assign(kbForm, {
    name: kb.name,
    description: kb.description,
    subject: kb.subject,
    gradeLevel: kb.gradeLevel
  })
  
  // 如果有年级，触发年级变化事件加载推荐学科
  if (kb.gradeLevel) {
    onGradeLevelChange(kb.gradeLevel)
  }
  
  showCreateDialog.value = true
}

const saveKnowledgeBase = async () => {
  if (!kbFormRef.value) return
  
  try {
    await kbFormRef.value.validate()
    saving.value = true
    
    if (editingKb.value) {
      await knowledgeBaseApi.updateKnowledgeBase(editingKb.value.id, kbForm)
      ElMessage.success('知识库更新成功')
    } else {
      await knowledgeBaseApi.createKnowledgeBase(kbForm)
      ElMessage.success('知识库创建成功')
    }
    
    showCreateDialog.value = false
    resetKbForm()
    loadKnowledgeBases()
  } catch (error) {
    ElMessage.error(editingKb.value ? '更新知识库失败' : '创建知识库失败')
  } finally {
    saving.value = false
  }
}

// 新学科创建相关方法
const saveNewSubject = async () => {
  if (!newSubjectFormRef.value) return
  
  try {
    await newSubjectFormRef.value.validate()
    saving.value = true
    
    // 调用教师创建学科API（简化参数）
    await subjectApi.teacherCreateSubject({
      name: newSubjectForm.name,
      description: newSubjectForm.description,
      sortOrder: 0  // 新学科默认排序为0
    })
    
    ElMessage.success(`学科"${newSubjectForm.name}"创建成功`)
    
    // 自动选择新创建的学科
    kbForm.subject = newSubjectForm.name
    
    // 刷新学科列表
    await loadSubjects()
    
    // 如果当前选择了年级，重新加载推荐学科
    if (kbForm.gradeLevel) {
      await onGradeLevelChange(kbForm.gradeLevel)
    }
    
    // 关闭对话框并重置表单
    showCreateSubjectDialog.value = false
    resetNewSubjectForm()
    
  } catch (error) {
    console.error('Failed to create subject:', error)
    ElMessage.error('创建学科失败，请重试')
  } finally {
    saving.value = false
  }
}

const cancelCreateSubject = () => {
  showCreateSubjectDialog.value = false
  resetNewSubjectForm()
}

const resetNewSubjectForm = () => {
  newSubjectForm.name = ''
  newSubjectForm.description = ''
  newSubjectForm.applicableGrades = []
  newSubjectFormRef.value?.clearValidate()
}

const deleteKnowledgeBase = async (kb: KnowledgeBase) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除知识库"${kb.name}"吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await knowledgeBaseApi.deleteKnowledgeBase(kb.id)
    ElMessage.success('知识库删除成功')
    loadKnowledgeBases()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除知识库失败')
    }
  }
}

const showUploadDialog = (kb: KnowledgeBase) => {
  selectedKb.value = kb
  showUploadDocDialog.value = true
}

const showAIQuestionDialogAction = (kb: KnowledgeBase) => {
  selectedKb.value = kb
  showAIQuestionDialog.value = true
}

const handleTableAction = (command: string, kb: KnowledgeBase) => {
  switch (command) {
    case 'generate':
      showAIQuestionDialogAction(kb)
      break
    case 'delete':
      deleteKnowledgeBase(kb)
      break
  }
}

const resetKbForm = () => {
  Object.assign(kbForm, {
    name: '',
    description: '',
    subject: '',
    gradeLevel: ''
  })
  editingKb.value = null
}

const handleDocumentUploaded = () => {
  loadKnowledgeBases() // 刷新统计信息
  ElMessage.success('文档上传成功')
}

const handleQuestionsGenerated = () => {
  ElMessage.success('题目生成成功')
}

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString()
}

// 关闭对话框时重置表单
const handleCreateDialogClose = () => {
  if (kbFormRef.value) {
    kbFormRef.value.resetFields()
  }
  resetKbForm()
}

const loadKnowledgePointTree = async () => {
  try {
    // 暂时使用模拟数据，后续可以调用真实API
    knowledgePointTree.value = [
      {
        id: 'math',
        label: '数学',
        type: 'subject',
        count: 120,
        children: [
          {
            id: 'algebra',
            label: '代数',
            type: 'chapter',
            count: 45,
            children: [
              {
                id: 'linear_equations',
                label: '一元一次方程',
                type: 'knowledge_point',
                count: 15,
                difficulty: 2,
                documentCount: 8,
                questionCount: 25
              },
              {
                id: 'quadratic_equations',
                label: '二次方程',
                type: 'knowledge_point',
                count: 20,
                difficulty: 3,
                documentCount: 12,
                questionCount: 35
              }
            ]
          },
          {
            id: 'geometry',
            label: '几何',
            type: 'chapter',
            count: 35,
            children: [
              {
                id: 'triangles',
                label: '三角形',
                type: 'knowledge_point',
                count: 18,
                difficulty: 2,
                documentCount: 10,
                questionCount: 30
              }
            ]
          }
        ]
      },
      {
        id: 'physics',
        label: '物理',
        type: 'subject',
        count: 80,
        children: [
          {
            id: 'mechanics',
            label: '力学',
            type: 'chapter',
            count: 40,
            children: [
              {
                id: 'newton_laws',
                label: '牛顿定律',
                type: 'knowledge_point',
                count: 20,
                difficulty: 3,
                documentCount: 15,
                questionCount: 40
              }
            ]
          }
        ]
      }
    ]
  } catch (error) {
    console.error('Failed to load knowledge point tree:', error)
    knowledgePointTree.value = []
  }
}

// 执行智能搜索（集成到快速搜索中）
const performSmartSearch = async () => {
  if (!quickSearchText.value.trim()) {
    ElMessage.warning('请输入搜索内容')
    return
  }
  
  loading.value = true
  vectorSearchLoading.value = true
  
  try {
    // 保存当前搜索查询
    currentSearchQuery.value = quickSearchText.value
    
    // 使用智能搜索查询
    const vectorSearchRequest = {
      query: quickSearchText.value,
      searchScope: ['documents', 'knowledge_points', 'questions'],
      similarityThreshold: 0.6
    }
    
    const response = await knowledgeBaseApi.vectorSearch(vectorSearchRequest)
    let searchResults = []
    
    // 处理不同的响应格式
    if (response.data) {
      searchResults = response.data.results || []
    } else {
      searchResults = (response as any).results || []
    }
    
    if (searchResults.length === 0) {
      ElMessage.info('未找到相关内容，将显示所有知识库')
      // 如果没有找到相关内容，显示所有知识库
      loadKnowledgeBases()
      return
    }
    
    // 从搜索结果中提取知识库ID
    const knowledgeBaseIds = new Set<number>()
    searchResults.forEach((result: any) => {
      if (result.metadata?.knowledgeBaseId) {
        knowledgeBaseIds.add(result.metadata.knowledgeBaseId)
      }
    })
    
    if (knowledgeBaseIds.size === 0) {
      ElMessage.info('未找到相关知识库，将显示所有知识库')
      loadKnowledgeBases()
      return
    }
    
    // 获取相关的知识库
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
      // 添加其他筛选条件
      subject: activeSubjectFilter.value || searchForm.subject,
      gradeLevel: activeGradeFilter.value || searchForm.gradeLevel
    }
    
    const kbResponse = await knowledgeBaseApi.getKnowledgeBases(params)
    
    // 过滤出包含相关内容的知识库
    const filteredKnowledgeBases = kbResponse.content.filter((kb: any) => 
      knowledgeBaseIds.has(kb.id)
    )
    
    knowledgeBases.value = filteredKnowledgeBases
    total.value = filteredKnowledgeBases.length
    
    // 存储搜索结果以便后续显示详情
    vectorSearchResults.value = searchResults
    
    // 自动显示搜索结果详情对话框
    if (searchResults.length > 0) {
      showVectorSearchResults.value = true
    }
    
    ElMessage.success(`基于AI搜索找到 ${filteredKnowledgeBases.length} 个相关知识库，${searchResults.length} 个相关内容`)
    
  } catch (error) {
    console.error('Smart search failed:', error)
    ElMessage.warning('智能搜索服务暂时不可用，将显示模拟搜索结果')
    
    // 即使API失败，也显示模拟搜索结果供演示
    vectorSearchResults.value = [
      {
        id: '1',
        title: '一元一次方程的解法',
        content: '一元一次方程是只含有一个未知数，且未知数的最高次数为1的整式方程。解一元一次方程的基本思路是通过移项、合并同类项等方法，将方程化为x=a的形式。',
        highlightedContent: `一元一次<mark>${quickSearchText.value}</mark>是只含有一个未知数，且未知数的最高次数为1的整式<mark>方程</mark>。解一元一次<mark>方程</mark>的基本思路是通过移项、合并同类项等方法，将<mark>方程</mark>化为x=a的形式。`,
        type: 'document' as const,
        source: '数学教材第三章',
        similarity: 0.95,
        matchedKeywords: [quickSearchText.value, '方程', '一元一次'],
        keywordPositions: [
          {
            keyword: quickSearchText.value,
            startIndex: 4,
            endIndex: 4 + quickSearchText.value.length,
            context: `一元一次${quickSearchText.value}是只含有一个未知数...`
          },
          {
            keyword: '方程',
            startIndex: 25,
            endIndex: 27,
            context: '...未知数的最高次数为1的整式方程。解一元一次方程的基本思路...'
          }
        ],
        knowledgeBaseId: 'kb_001',
        knowledgeBaseName: '初中数学知识库',
        documentId: 'doc_001',
        chunkIndex: 0,
        contextBefore: '在学习代数基础时，我们需要掌握各种方程的解法。',
        contextAfter: '掌握了一元一次方程的解法后，我们可以进一步学习二元一次方程组的解法。'
      },
      {
        id: '2',
        title: '方程的基本性质',
        content: '方程两边同时加上或减去同一个数，方程的解不变。方程两边同时乘以或除以同一个非零数，方程的解不变。',
        highlightedContent: `<mark>方程</mark>两边同时加上或减去同一个数，<mark>方程</mark>的解不变。<mark>方程</mark>两边同时乘以或除以同一个非零数，<mark>方程</mark>的解不变。关于<mark>${quickSearchText.value}</mark>的更多内容。`,
        type: 'knowledge_point' as const,
        source: '知识点总结',
        similarity: 0.87,
        matchedKeywords: [quickSearchText.value, '方程'],
        keywordPositions: [
          {
            keyword: '方程',
            startIndex: 0,
            endIndex: 2,
            context: '方程两边同时加上或减去同一个数...'
          },
          {
            keyword: quickSearchText.value,
            startIndex: 45,
            endIndex: 45 + quickSearchText.value.length,
            context: `...的解不变。关于${quickSearchText.value}的更多内容。`
          }
        ],
        knowledgeBaseId: 'kb_001',
        knowledgeBaseName: '初中数学知识库',
        documentId: 'doc_002',
        chunkIndex: 1,
        contextBefore: '在解方程时，我们需要了解方程的基本性质。',
        contextAfter: '利用这些性质，我们可以通过等价变换来求解方程。'
      }
    ]
    
    // 显示搜索结果
    showVectorSearchResults.value = true
    ElMessage.success(`找到 ${vectorSearchResults.value.length} 个相关模拟结果`)
    
    // 继续显示知识库列表（基础搜索）
    loadKnowledgeBases()
  } finally {
    loading.value = false
    vectorSearchLoading.value = false
  }
}

const performVectorSearch = async () => {
  if (!vectorSearchForm.query.trim()) {
    ElMessage.warning('请输入搜索内容')
    return
  }
  
  vectorSearchLoading.value = true
  try {
    // 保存当前搜索查询
    currentSearchQuery.value = vectorSearchForm.query
    
    const response = await knowledgeBaseApi.vectorSearch(vectorSearchForm)
    // 处理不同的响应格式
    if (response.data) {
      vectorSearchResults.value = response.data.results || []
    } else {
      vectorSearchResults.value = (response as any).results || []
    }
    showVectorSearchResults.value = true
    ElMessage.success(`找到 ${vectorSearchResults.value.length} 个相关结果`)
  } catch (error) {
    ElMessage.error('向量搜索失败')
    console.error('Vector search failed:', error)
    // 使用模拟数据作为后备
    vectorSearchResults.value = [
      {
        id: '1',
        title: '一元一次方程的解法',
        content: '一元一次方程是只含有一个未知数，且未知数的最高次数为1的整式方程。解一元一次方程的基本思路是通过移项、合并同类项等方法，将方程化为x=a的形式。',
        highlightedContent: '一元一次<mark>方程</mark>是只含有一个未知数，且未知数的最高次数为1的整式<mark>方程</mark>。解一元一次<mark>方程</mark>的基本思路是通过移项、合并同类项等方法，将<mark>方程</mark>化为x=a的形式。',
        type: 'document' as const,
        source: '数学教材第三章',
        similarity: 0.95,
        matchedKeywords: ['方程', '一元一次'],
        keywordPositions: [
          {
            keyword: '方程',
            startIndex: 25,
            endIndex: 27,
            context: '...未知数的最高次数为1的整式方程。解一元一次方程的基本思路...'
          },
          {
            keyword: '一元一次',
            startIndex: 0,
            endIndex: 4,
            context: '一元一次方程是只含有一个未知数...'
          }
        ],
        knowledgeBaseId: 'kb_001',
        knowledgeBaseName: '初中数学知识库',
        documentId: 'doc_001',
        chunkIndex: 0,
        contextBefore: '在学习代数基础时，我们需要掌握各种方程的解法。',
        contextAfter: '掌握了一元一次方程的解法后，我们可以进一步学习二元一次方程组的解法。'
      },
      {
        id: '2',
        title: '方程的基本性质',
        content: '方程两边同时加上或减去同一个数，方程的解不变。方程两边同时乘以或除以同一个非零数，方程的解不变。',
        highlightedContent: '<mark>方程</mark>两边同时加上或减去同一个数，<mark>方程</mark>的解不变。<mark>方程</mark>两边同时乘以或除以同一个非零数，<mark>方程</mark>的解不变。',
        type: 'knowledge_point' as const,
        source: '知识点总结',
        similarity: 0.87,
        matchedKeywords: ['方程'],
        keywordPositions: [
          {
            keyword: '方程',
            startIndex: 0,
            endIndex: 2,
            context: '方程两边同时加上或减去同一个数...'
          },
          {
            keyword: '方程',
            startIndex: 15,
            endIndex: 17,
            context: '...减去同一个数，方程的解不变。方程两边...'
          }
        ],
        knowledgeBaseId: 'kb_001',
        knowledgeBaseName: '初中数学知识库',
        documentId: 'doc_002',
        chunkIndex: 1,
        contextBefore: '在解方程时，我们需要了解方程的基本性质。',
        contextAfter: '利用这些性质，我们可以通过等价变换来求解方程。'
      },
      {
        id: '3',
        title: '解方程的步骤',
        content: '解方程的一般步骤：1. 去分母；2. 去括号；3. 移项；4. 合并同类项；5. 系数化为1。',
        highlightedContent: '解<mark>方程</mark>的一般步骤：1. 去分母；2. 去括号；3. 移项；4. 合并同类项；5. 系数化为1。',
        type: 'knowledge_point' as const,
        source: '解题方法总结',
        similarity: 0.82,
        matchedKeywords: ['方程'],
        keywordPositions: [
          {
            keyword: '方程',
            startIndex: 1,
            endIndex: 3,
            context: '解方程的一般步骤：1. 去分母...'
          }
        ],
        knowledgeBaseId: 'kb_001',
        knowledgeBaseName: '初中数学知识库',
        documentId: 'doc_003',
        chunkIndex: 0,
        contextBefore: '当我们遇到复杂的方程时，需要按照一定的步骤来解决。',
        contextAfter: '按照这些步骤，大部分一元一次方程都能得到正确的解。'
      },
      {
        id: '4',
        title: '方程的应用题',
        content: '方程在实际生活中有广泛应用，如年龄问题、行程问题、工程问题等。解应用题的关键是找到等量关系，建立方程。',
        highlightedContent: '<mark>方程</mark>在实际生活中有广泛应用，如年龄问题、行程问题、工程问题等。解应用题的关键是找到等量关系，建立<mark>方程</mark>。',
        type: 'document' as const,
        source: '数学应用专题',
        similarity: 0.78,
        matchedKeywords: ['方程'],
        keywordPositions: [
          {
            keyword: '方程',
            startIndex: 0,
            endIndex: 2,
            context: '方程在实际生活中有广泛应用...'
          },
          {
            keyword: '方程',
            startIndex: 42,
            endIndex: 44,
            context: '...找到等量关系，建立方程。'
          }
        ],
        knowledgeBaseId: 'kb_002',
        knowledgeBaseName: '数学应用题库',
        documentId: 'doc_004',
        chunkIndex: 2,
        contextBefore: '数学不仅仅是抽象的理论，更重要的是能够解决实际问题。',
        contextAfter: '通过大量的练习，学生可以熟练掌握用方程解决实际问题的方法。'
      }
    ]
    showVectorSearchResults.value = true
  } finally {
    vectorSearchLoading.value = false
  }
}

const clearVectorSearch = () => {
  Object.assign(vectorSearchForm, {
    query: '',
    searchScope: ['documents', 'knowledge_points', 'questions'],
    similarityThreshold: 0.7
  })
  vectorSearchResults.value = []
}

const clearVectorSearchResults = () => {
  vectorSearchResults.value = []
  showVectorSearchResults.value = false
}

const handleSearchTabChange = (tab: any) => {
  if (tab.name === 'basic') {
    // 切换到基础搜索时重置
    Object.assign(searchForm, {
      name: '',
      subject: '',
      gradeLevel: ''
    })
  } else if (tab.name === 'vector') {
    clearVectorSearch()
  }
}

const getSimilarityHint = (threshold: number) => {
  if (threshold >= 0.8) return '非常相关'
  if (threshold >= 0.6) return '相关'
  if (threshold >= 0.4) return '一般相关'
  return '弱相关'
}

const getResultTypeColor = (type: string) => {
  switch (type) {
    case 'document': return 'primary'
    case 'knowledge_point': return 'success'
    case 'question': return 'warning'
    default: return 'info'
  }
}

const getResultTypeLabel = (type: string) => {
  switch (type) {
    case 'document': return '文档'
    case 'knowledge_point': return '知识点'
    case 'question': return '题目'
    case 'chunk': return '文档片段'
    default: return type || '未知'
  }
}

const copyResultContent = async (result: any) => {
  try {
    // 复制高亮内容或原始内容
    const contentToCopy = result.highlightedContent 
      ? result.highlightedContent.replace(/<mark>/g, '').replace(/<\/mark>/g, '') 
      : result.content
    
    await navigator.clipboard.writeText(contentToCopy)
    ElMessage.success('内容已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败，请手动复制')
    console.error('Copy failed:', error)
  }
}

const exportSearchResults = () => {
  try {
    const dataToExport = vectorSearchResults.value.map(result => ({
      标题: result.title || '未命名',
      类型: getResultTypeLabel(result.type),
      内容: result.content,
      相似度: (result.similarity * 100).toFixed(1) + '%',
      来源: result.source || '未知来源',
      匹配关键词: result.matchedKeywords?.join(', ') || '无'
    }))
    
    const csvContent = [
      Object.keys(dataToExport[0]).join(','),
      ...dataToExport.map(row => Object.values(row).map(val => `"${val}"`).join(','))
    ].join('\n')
    
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `AI搜索结果_${new Date().toLocaleDateString()}.csv`
    link.click()
    
    ElMessage.success('搜索结果已导出')
  } catch (error) {
    ElMessage.error('导出失败')
    console.error('Export failed:', error)
  }
}

const viewResultDetail = (result: any) => {
  ElMessage.info('查看详情功能开发中...')
}

const generateFromResult = (result: any) => {
  ElMessage.info('基于内容出题功能开发中...')
}

const handleKnowledgePointSelect = async (node: any) => {
  selectedKnowledgePoint.value = node
  if (node.id) {
    try {
      const response = await knowledgeBaseApi.getRelatedContent(node.id)
      relatedContent.value = response.data || response
    } catch (error) {
      console.error('Failed to load related content:', error)
      relatedContent.value = []
    }
  }
}

const generateQuestionsFromKnowledgePoint = (knowledgePoint: any) => {
  ElMessage.info('基于知识点出题功能开发中...')
}

const viewRelatedDocuments = (knowledgePoint: any) => {
  ElMessage.info('查看相关文档功能开发中...')
}



// 年级变化处理方法 - 修复后的逻辑
const onGradeLevelChange = async (gradeLevel: string) => {
  if (gradeLevel) {
    try {
      // 获取年级类别
      const categoryResponse = await gradeLevelApi.getGradeCategory(gradeLevel)
      selectedGradeCategory.value = categoryResponse.data || ''
      
      // 获取所有学科（已按重要性排序：推荐的在前，其他的在后）
      const subjectsResponse = await gradeLevelApi.getRecommendedSubjectsByGrade(gradeLevel)
      const allSortedSubjects = subjectsResponse.data || []
      
      // 根据年级类别确定推荐学科数量（这些是排在前面的）
      const gradeCategory = selectedGradeCategory.value
      let recommendedCount = 0
      
      switch (gradeCategory) {
        case '小学':
          recommendedCount = 10
          break
        case '初中':
        case '高中':
          recommendedCount = 13
          break
        case '大学':
          recommendedCount = 16
          break
        case '职业':
        case '成人':
          recommendedCount = 7
          break
        case '学前':
          recommendedCount = 8
          break
        default:
          recommendedCount = 8
      }
      
      // 分离推荐学科和其他学科（但用户可以选择所有学科）
      recommendedSubjects.value = allSortedSubjects.slice(0, recommendedCount)
      otherSubjects.value = allSortedSubjects.slice(recommendedCount)
      
      // 验证当前选择的学科是否在完整列表中
      if (kbForm.subject && !allSortedSubjects.includes(kbForm.subject)) {
        ElMessage.warning(`"${kbForm.subject}"不在可用学科列表中，建议重新选择`)
      }
      
      ElMessage.success(`已加载${allSortedSubjects.length}个学科，为您优先推荐${gradeCategory}阶段的${recommendedCount}个常用学科`)
    } catch (error) {
      console.error('Failed to load recommended subjects:', error)
      ElMessage.warning('获取推荐学科失败，显示所有学科')
      // 发生错误时显示所有学科
      recommendedSubjects.value = []
      otherSubjects.value = allSubjects.value
      selectedGradeCategory.value = ''
    }
  } else {
    // 清空年级时重置推荐状态
    selectedGradeCategory.value = ''
    recommendedSubjects.value = []
    otherSubjects.value = []
    kbForm.subject = '' // 清空学科选择
  }
}

// 原有的学科变化处理方法（现在简化）
const onSubjectChange = (subject: string) => {
  // 保留原有逻辑作为备用，现在主要由年级驱动学科选择
  if (subject && !kbForm.gradeLevel) {
    // 如果选择了学科但没有选择年级，给出提示
    ElMessage.info('建议先选择年级，系统会为您推荐合适的学科')
  }
}

// 收藏相关方法
const toggleKnowledgeBaseFavorite = async (kb: KnowledgeBase) => {
  try {
    const newStatus = await knowledgeBaseApi.toggleFavorite(kb.id)
    
    // 更新本地状态
    const index = knowledgeBases.value.findIndex(item => item.id === kb.id)
    if (index > -1) {
      knowledgeBases.value[index].isFavorited = newStatus
      if (newStatus) {
        knowledgeBases.value[index].favoriteCount = (knowledgeBases.value[index].favoriteCount || 0) + 1
      } else {
        knowledgeBases.value[index].favoriteCount = Math.max((knowledgeBases.value[index].favoriteCount || 1) - 1, 0)
      }
    }
    
    ElMessage.success(newStatus ? '收藏成功' : '取消收藏成功')
    
    // 如果当前在收藏筛选页面，需要刷新列表
    if (activeFilter.value === 'starred') {
      loadKnowledgeBases()
    }
  } catch (error) {
    ElMessage.error('操作失败，请重试')
    console.error('Toggle favorite failed:', error)
  }
}

const loadFavoriteKnowledgeBases = async () => {
  try {
    const favorites = await knowledgeBaseApi.getFavoriteKnowledgeBases()
    knowledgeBases.value = favorites
    total.value = favorites.length
  } catch (error) {
    ElMessage.error('加载收藏知识库失败')
    console.error('Load favorites failed:', error)
  }
}
</script>

<style scoped>
.knowledge-base-management {
  padding: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 顶部工具栏样式 */
.top-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.breadcrumb-area {
  font-size: 14px;
}

.view-actions {
  margin-left: 20px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 主内容区域 */
.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 表单相关样式 */
.subject-input-container {
  display: flex;
  align-items: center;
  width: 100%;
}

.form-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.grade-category-info {
  margin-top: 8px;
}

.grade-category-info .el-tag {
  font-size: 12px;
}

/* 侧边栏样式 */
.sidebar {
  width: 260px;
  background: #f8f9fa;
  border-right: 1px solid #e4e7ed;
  padding: 20px 0;
  overflow-y: auto;
}

.sidebar-section {
  margin-bottom: 24px;
  padding: 0 20px;
}

.sidebar-section h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.quick-access-list,
.category-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.access-item,
.category-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
  margin-bottom: 4px;
  font-size: 14px;
}

.access-item:hover,
.category-item:hover {
  background: #e9ecef;
}

.access-item.active,
.category-item.active {
  background: #409eff;
  color: #fff;
}

.access-item .count {
  margin-left: auto;
  background: #e9ecef;
  color: #606266;
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 12px;
}

.access-item.active .count {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

/* 内容区域 */
.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 操作栏 */
.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #e3f2fd;
  border-bottom: 1px solid #bbdefb;
}

.selected-info {
  font-size: 14px;
  color: #1976d2;
  font-weight: 500;
}

.batch-actions {
  display: flex;
  gap: 8px;
}

/* 排序和筛选栏 */
.sort-filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.left-controls {
  display: flex;
  align-items: center;
  gap: 16px;
}

.item-count {
  font-size: 14px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}

.item-count .el-icon {
  font-size: 16px;
  color: #409eff;
}

.right-controls {
  display: flex;
  align-items: center;
  gap: 16px; /* 增加间距 */
}

.sort-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
  margin-right: 8px; /* 增加右边距 */
  white-space: nowrap; /* 防止文字换行 */
}

.sort-order-controls {
  display: flex;
  align-items: center;
}

.sort-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px !important; /* 增加左右内边距 */
  font-size: 13px;
  font-weight: 500;
  min-width: 80px; /* 增加最小宽度 */
  justify-content: center;
  transition: all 0.3s ease;
}

.sort-btn .el-icon {
  font-size: 14px;
}

.sort-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.sort-btn.el-button--primary {
  background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%);
  border-color: #409eff;
}

.sort-btn.el-button--primary:hover {
  background: linear-gradient(135deg, #3a8ee6 0%, #337ecc 100%);
  border-color: #3a8ee6;
}

/* 网格容器 - 优化布局 */
.grid-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: #f8f9fa;
}

.kb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

/* 知识库卡片 - 现代化设计 */
.kb-card {
  position: relative;
  background: #fff;
  border: 1px solid #e8e9ea;
  border-radius: 16px;
  padding: 0;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  height: 340px;
  display: flex;
  flex-direction: column;
}

.kb-card:hover {
  border-color: #409eff;
  box-shadow: 0 8px 32px rgba(64, 158, 255, 0.12);
  transform: translateY(-4px);
}

.kb-card.selected {
  border-color: #409eff;
  background: linear-gradient(135deg, #f0f8ff 0%, #e6f4ff 100%);
  box-shadow: 0 8px 32px rgba(64, 158, 255, 0.15);
}

.card-checkbox {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 3;
}

/* 卡片头部区域 */
.kb-card-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  color: white;
  position: relative;
  overflow: hidden;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kb-card-header::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 70%);
  transform: rotate(45deg);
}

.kb-icon {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  color: rgba(255, 255, 255, 0.9);
  z-index: 2;
}

.kb-icon .el-icon {
  font-size: 48px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
}

/* 收藏指示器 */
.favorite-indicator {
  position: relative;
  z-index: 2;
  color: #ffd700;
  font-size: 20px;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.2));
}

/* 卡片内容区域 */
.kb-info {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.kb-name {
  font-size: 16px;
  font-weight: 700;
  color: #2c3e50;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-align: left;
  margin: 0;
}

/* 知识库描述 */
.kb-description {
  font-size: 13px;
  color: #6c757d;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0;
}

.kb-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-start;
}

.kb-meta .subject {
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  color: #1976d2;
  border: 1px solid #90caf9;
}

.kb-meta .grade {
  background: linear-gradient(135deg, #f3e5f5 0%, #e1bee7 100%);
  color: #7b1fa2;
  border: 1px solid #ce93d8;
}

.kb-meta .subject,
.kb-meta .grade {
  font-size: 12px;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: 12px;
  transition: all 0.2s;
}

.kb-meta .subject:hover,
.kb-meta .grade:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.kb-stats {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 12px;
  border: 1px solid #e9ecef;
}

/* 统计信息样式优化 */
.kb-stats {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 8px;
  padding: 12px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 50%, #f8f9fa 100%);
  border-radius: 8px;
  border: 1px solid #dee2e6;
  margin: 0 4px;
}

.kb-stats .stat {
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 4px;
  border-radius: 6px;
  transition: all 0.2s ease;
  background: rgba(255, 255, 255, 0.7);
}

.kb-stats .stat:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.stat-number {
  font-size: 18px;
  font-weight: 700;
  color: #409eff;
  line-height: 1;
}

.stat-label {
  font-size: 12px;
  color: #6c757d;
  font-weight: 500;
}

/* 卡片底部区域 */
.kb-footer {
  margin-top: auto;
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #f0f0f0;
  background: rgba(248, 249, 250, 0.5);
}

.kb-time {
  font-size: 12px;
  color: #8c8c8c;
  display: flex;
  align-items: center;
  gap: 4px;
}

.kb-time .el-icon {
  font-size: 14px;
}

.card-actions {
  opacity: 0.7;
  transition: opacity 0.2s;
}

.kb-card:hover .card-actions {
  opacity: 1;
}

.action-trigger {
  color: #8c8c8c;
  font-size: 16px;
  padding: 4px;
}

.action-trigger:hover {
  color: #409eff;
  background: rgba(64, 158, 255, 0.1);
  border-radius: 4px;
}

/* 列表容器 */
.list-container {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.list-container .el-table {
  flex: 1;
}

.name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  color: #409eff;
  font-size: 16px;
}

.name-text {
  font-weight: 500;
}

/* 空状态 */
.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 分页 */
.pagination-wrapper {
  padding: 16px 20px;
  border-top: 1px solid #e4e7ed;
  background: #fff;
  display: flex;
  justify-content: center;
}

/* 对话框 */
.dialog-footer {
  text-align: right;
}

/* 年级推荐样式 */
.grade-recommendation {
  margin-top: 8px;
  padding: 8px 12px;
  background: #f0f9ff;
  border-radius: 4px;
  border-left: 3px solid #409eff;
}

.recommendation-label {
  font-size: 12px;
  color: #606266;
  margin-right: 8px;
}

.recommendation-tag {
  margin-right: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.recommendation-tag:hover {
  background-color: #409eff;
  color: white;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .main-content {
    flex-direction: column;
 
  }
  
  .sidebar {
    width: 100%;
    height: auto;
    max-height: 200px;
  }
  
  .toolbar-left {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .toolbar-right {
    flex-wrap: wrap;
  }
  
  .kb-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 12px;
  }
}

/* 搜索卡片保留样式 */
.search-card {
  margin: 20px;
  margin-bottom: 0;
}

.knowledge-points-browser {
  padding: 20px;
  background: #f9f9f9;
  border-radius: 8px;
}

.category-tree {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.knowledge-point-content {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.knowledge-point-details {
  margin-bottom: 16px;
}

.knowledge-point-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.related-content {
  margin-top: 16px;
}

.content-snippets {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 16px;
}

.content-snippet {
  background: #f0f0f0;
  padding: 12px;
  border-radius: 8px;
}

.snippet-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.snippet-content {
  color: #333;
  font-size: 14px;
  line-height: 1.5;
}

.search-results-card {
  margin: 20px;
  margin-top: 0;
}

.search-results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-results {
  margin-top: 16px;
}

.search-result-item {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.result-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.result-meta {
  display: flex;
  gap: 8px;
}

.similarity-score {
  font-size: 12px;
  color: #999;
}

.result-content {
  color: #333;
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 8px;
}

.result-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}

/* 年级类别信息样式 */
.grade-category-info {
  margin-top: 8px;
}

/* 表单提示样式 */
.form-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.form-tip .el-icon {
  color: #409eff;
}

/* 增强搜索区域样式 */
.enhanced-search-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-status-bar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 12px 20px;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.search-status-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.search-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.search-mode-label {
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}

.search-mode-label .el-icon {
  font-size: 14px;
}

.search-keyword {
  background: rgba(255, 255, 255, 0.3);
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
}

.search-filter {
  background: rgba(255, 255, 255, 0.15);
  padding: 3px 6px;
  border-radius: 3px;
  font-size: 12px;
}

.search-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.result-count {
  font-size: 13px;
  opacity: 0.9;
}

.search-actions .el-button {
  color: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.search-actions .el-button:hover {
  color: white;
  border-color: rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-1px);
}

/* 搜索模式切换按钮样式 */
.search-mode-toggle .el-button-group {
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border-radius: 6px;
  overflow: hidden;
}

.search-mode-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px !important;
  border: none !important;
  background: #f8f9fa;
  color: #6c757d;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.3s ease;
  min-width: 100px;
  justify-content: center;
  position: relative;
}

.search-mode-btn:hover {
  background: #e9ecef;
  color: #495057;
  transform: translateY(-1px);
}

.search-mode-btn.el-button--primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.search-mode-btn.el-button--primary:hover {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.search-mode-btn .el-icon {
  font-size: 14px;
}

.search-mode-btn:first-child {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
  border-right: 1px solid #dee2e6;
}

.search-mode-btn:last-child {
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
}

/* 搜索状态条样式改进 */
.search-status-bar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 14px 20px;
  border-radius: 10px;
  margin-bottom: 16px;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  backdrop-filter: blur(10px);
}

.search-status-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.search-info {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  flex-wrap: wrap;
}

.search-mode-indicator {
  background: rgba(255, 255, 255, 0.25);
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: fit-content;
}

.search-mode-indicator .el-icon {
  font-size: 15px;
}

.mode-text {
  white-space: nowrap;
}

.search-keyword {
  background: rgba(255, 255, 255, 0.3);
  padding: 5px 10px;
  border-radius: 5px;
  font-size: 13px;
  font-weight: 500;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.search-filter {
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: fit-content;
}

.search-filter .el-icon {
  font-size: 12px;
}

.search-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.result-count {
  font-size: 13px;
  opacity: 0.9;
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: fit-content;
}

.result-count .el-icon {
  font-size: 14px;
}

.action-btn {
  color: rgba(255, 255, 255, 0.9) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  background: rgba(255, 255, 255, 0.1) !important;
  backdrop-filter: blur(10px);
  border-radius: 6px !important;
  padding: 6px 10px !important;
  font-size: 12px !important;
  font-weight: 500 !important;
  transition: all 0.3s ease !important;
  min-width: fit-content;
}

.action-btn:hover {
  color: white !important;
  border-color: rgba(255, 255, 255, 0.5) !important;
  background: rgba(255, 255, 255, 0.2) !important;
  transform: translateY(-1px);
}

.clear-btn:hover {
  background: rgba(255, 107, 107, 0.3) !important;
  border-color: rgba(255, 107, 107, 0.5) !important;
}

/* 旧版本的搜索类型切换按钮样式 */
.enhanced-search-area .el-button-group .el-button {
  border-radius: 4px;
  transition: all 0.3s ease;
}

.enhanced-search-area .el-button-group .el-button:first-child {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}

.enhanced-search-area .el-button-group .el-button:last-child {
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
}

.enhanced-search-area .el-button-group .el-button.is-disabled {
  opacity: 0.6;
}

/* 智能搜索结果特殊样式 */
.search-result-item[data-search-mode="smart"] {
  border-left: 4px solid #667eea;
  background: linear-gradient(135deg, #f8f9ff 0%, #f0f2ff 100%);
}

.search-result-item[data-search-mode="smart"] .result-title {
  color: #667eea;
}

/* 加载状态样式 */
.vector-search-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #667eea;
  font-size: 14px;
}

.vector-search-loading .el-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ==================== 关键词高亮功能样式 ==================== */

/* 高亮内容容器 */
.highlighted-content {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 16px;
  margin: 12px 0;
  line-height: 1.6;
  font-size: 14px;
  color: #495057;
}

/* 关键词高亮标记样式 */
.highlighted-content mark {
  background: linear-gradient(135deg, #ffeb3b 0%, #ffc107 100%);
  color: #2c3e50;
  padding: 2px 4px;
  border-radius: 3px;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(255, 193, 7, 0.3);
  animation: highlight-pulse 2s ease-in-out;
}

@keyframes highlight-pulse {
  0% { background: linear-gradient(135deg, #ffeb3b 0%, #ffc107 100%); }
  50% { background: linear-gradient(135deg, #ffc107 0%, #ff9800 100%); }
  100% { background: linear-gradient(135deg, #ffeb3b 0%, #ffc107 100%); }
}

/* 匹配关键词标签容器 */
.matched-keywords {
  margin: 12px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.matched-keywords-title {
  font-size: 13px;
  color: #6c757d;
  font-weight: 500;
  margin-right: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.matched-keywords .el-tag {
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  color: #1976d2;
  border: 1px solid #90caf9;
  border-radius: 16px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.3s ease;
  cursor: pointer;
}

.matched-keywords .el-tag:hover {
  background: linear-gradient(135deg, #bbdefb 0%, #90caf9 100%);
  color: #0d47a1;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(25, 118, 210, 0.2);
}

/* 关键词位置信息 */
.keyword-positions {
  margin: 12px 0;
}

.keyword-positions .el-collapse {
  border: 1px solid #e9ecef;
  border-radius: 8px;
  overflow: hidden;
}

.keyword-positions .el-collapse-item__header {
  background: #f8f9fa;
  padding: 12px 16px;
  font-size: 13px;
  font-weight: 500;
  color: #495057;
  border-bottom: 1px solid #e9ecef;
}

.keyword-positions .el-collapse-item__content {
  padding: 16px;
  background: #fff;
}

.position-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  margin-bottom: 8px;
  transition: all 0.3s ease;
}

.position-item:hover {
  background: #e3f2fd;
  border-color: #90caf9;
}

.position-keyword {
  background: #fff3cd;
  color: #856404;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  min-width: fit-content;
}

.position-context {
  flex: 1;
  font-size: 13px;
  color: #6c757d;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.position-index {
  color: #adb5bd;
  font-size: 12px;
  min-width: fit-content;
}

/* 搜索结果对话框增强样式 */
.vector-search-dialog .el-dialog__body {
  padding: 20px;
  max-height: 70vh;
  overflow-y: auto;
}

.search-result-enhanced {
  border: 1px solid #e9ecef;
  border-radius: 12px;
  margin-bottom: 16px;
  overflow: hidden;
  transition: all 0.3s ease;
  background: #fff;
}

.search-result-enhanced:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.1);
  transform: translateY(-2px);
}

.search-result-enhanced:last-child {
  margin-bottom: 0;
}

.result-header-enhanced {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  padding: 16px 20px;
  border-bottom: 1px solid #e9ecef;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-title-enhanced {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.result-title-enhanced .el-icon {
  color: #409eff;
}

.result-meta-enhanced {
  display: flex;
  gap: 12px;
  align-items: center;
}

.result-type-tag {
  background: #e3f2fd;
  color: #1976d2;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.similarity-score-enhanced {
  background: #fff3cd;
  color: #856404;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.result-content-enhanced {
  padding: 20px;
}

.original-content {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 16px;
  margin: 12px 0;
  line-height: 1.6;
  font-size: 14px;
  color: #495057;
}

.content-preview {
  max-height: 120px;
  overflow: hidden;
  position: relative;
}

.content-preview::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 30px;
  background: linear-gradient(transparent, #f8f9fa);
}

.result-actions {
  padding: 16px 20px;
  background: #f8f9fa;
  border-top: 1px solid #e9ecef;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-info {
  font-size: 12px;
  color: #6c757d;
  display: flex;
  align-items: center;
  gap: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.info-item .el-icon {
  font-size: 14px;
  color: #adb5bd;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.action-btn-small {
  padding: 6px 12px;
  font-size: 12px;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.action-btn-small:hover {
  transform: translateY(-1px);
}

/* 搜索结果导出和复制功能样式 */
.search-results-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.results-count {
  font-size: 14px;
  color: #495057;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}

.results-count .el-icon {
  color: #409eff;
}

.results-actions {
  display: flex;
  gap: 8px;
}

.export-btn, .copy-all-btn {
  padding: 6px 12px;
  font-size: 12px;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.export-btn:hover, .copy-all-btn:hover {
  transform: translateY(-1px);
}

/* 复制成功提示样式 */
.copy-success {
  color: #67c23a;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: 8px;
  animation: fadeInOut 2s ease-in-out;
}

@keyframes fadeInOut {
  0% { opacity: 0; transform: translateY(10px); }
  20% { opacity: 1; transform: translateY(0); }
  80% { opacity: 1; transform: translateY(0); }
  100% { opacity: 0; transform: translateY(-10px); }
}

/* 搜索状态和统计信息样式 */
.search-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 8px;
  margin-bottom: 16px;
}

.search-query-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.current-query {
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
}

.search-timing {
  font-size: 12px;
  opacity: 0.8;
}

/* 响应式设计增强 */
@media (max-width: 768px) {
  .vector-search-dialog .el-dialog {
    width: 95%;
    margin: 20px auto;
  }
  
  .result-header-enhanced {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .result-meta-enhanced {
    align-self: stretch;
    justify-content: space-between;
  }
  
  .result-actions {
    flex-direction: column;
    gap: 12px;
  }
  
  .action-buttons {
    width: 100%;
    justify-content: space-between;
  }
  
  .matched-keywords {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .matched-keywords-title {
    margin-right: 0;
    margin-bottom: 4px;
  }
  
  .search-stats {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .search-query-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }
}

/* 加载动画增强 */
.highlight-loading {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #409eff;
  font-size: 12px;
}

.highlight-loading .el-icon {
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* 无搜索结果状态 */
.no-results-state {
  text-align: center;
  padding: 40px 20px;
  color: #6c757d;
}

.no-results-state .el-icon {
  font-size: 48px;
  color: #adb5bd;
  margin-bottom: 16px;
}

.no-results-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
  color: #495057;
}

.no-results-desc {
  font-size: 14px;
  line-height: 1.5;
}

/* 搜索结果关键词统计信息 */
.keyword-stats {
  background: #e8f4fd;
  border: 1px solid #b3d9f7;
  border-radius: 6px;
  padding: 8px 12px;
  margin: 8px 0;
  font-size: 12px;
  color: #1976d2;
  display: flex;
  align-items: center;
  gap: 6px;
}

.keyword-stats .el-icon {
  color: #409eff;
}

/* 上下文内容展示 */
.content-context {
  margin-top: 12px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  border-left: 3px solid #409eff;
}

.context-before, .context-after {
  margin-bottom: 6px;
}

.context-before:last-child, .context-after:last-child {
  margin-bottom: 0;
}

.context-label {
  font-weight: 500;
  color: #409eff;
  margin-right: 6px;
}

.context-text {
  color: #606266;
  font-size: 13px;
  line-height: 1.4;
}

/* 知识库名称和块信息 */
.kb-name, .chunk-info {
  color: #909399;
  margin-left: 8px;
}

.kb-info, .chunk-info {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 12px;
}

/* 增强高亮内容样式 */
.highlighted-content {
  line-height: 1.6;
}

.highlighted-content ::v-deep(mark), 
.highlighted-content ::v-deep(.highlight) {
  background: linear-gradient(120deg, #a8e6cf 0%, #ffd3a5 100%);
  padding: 2px 4px;
  border-radius: 3px;
  font-weight: 500;
  color: #2c3e50;
}

.original-content {
  line-height: 1.6;
  color: #606266;
  background: #fafafa;
  padding: 12px;
  border-radius: 6px;
  border-left: 3px solid #e4e7ed;
}

/* 搜索结果排序控制 */
.search-sort-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.sort-label {
  font-size: 13px;
  color: #495057;
  font-weight: 500;
}

.sort-options {
  display: flex;
  gap: 8px;
}

.sort-option {
  padding: 4px 8px;
  font-size: 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #e9ecef;
  background: #fff;
  color: #6c757d;
}

.sort-option:hover {
  border-color: #409eff;
  color: #409eff;
}

.sort-option.active {
  background: #409eff;
  color: white;
  border-color: #409eff;
}
</style>
