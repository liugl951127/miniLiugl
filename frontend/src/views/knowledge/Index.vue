<!-- @file knowledge/Index.vue - 知识中心 V6.9 (tab 化) -->
<template>
  <div class="page-card">
    <el-tabs v-model="activeTab" class="knowledge-tabs">
      <!-- ═══ 知识库 ═══ -->
      <el-tab-pane name="kb">
        <template #label><span>📚 知识库</span></template>

        <!-- 页面头部 -->
        <div class="page-header">
          <h2>知识库管理</h2>
          <el-tooltip content="新建知识库，支持上传文档并自动向量化检索" placement="bottom">
            <el-button type="primary" @click="openCreateKb">
              <el-icon><Plus /></el-icon>新建知识库
            </el-button>
          </el-tooltip>
        </div>

        <!-- 搜索栏 -->
        <el-form inline class="search-bar">
          <el-form-item><el-input v-model="keyword" placeholder="搜索知识库名称" clearable @change="loadKbs" /></el-form-item>
          <el-form-item><el-button @click="loadKbs">搜索</el-button></el-form-item>
        </el-form>

        <!-- 知识库列表 -->
        <el-table :data="kbs" v-loading="loading" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="description" label="描述" show-overflow-tooltip />
          <el-table-column prop="docCount" label="文档数" width="100" align="center">
            <template #default="{ row }"><span>{{ row.docCount || 0 }}</span></template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                {{ row.status === 'ACTIVE' ? '运行中' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" align="center">
            <template #default="{ row }">
              <el-tooltip content="查看该知识库下的所有文档，可上传新文档" placement="top">
                <el-button size="small" @click="viewDocs(row)">文档</el-button>
              </el-tooltip>
              <el-tooltip content="修改知识库名称、描述或向量模型配置" placement="top">
                <el-button size="small" type="primary" @click="editKb(row)">编辑</el-button>
              </el-tooltip>
              <el-tooltip content="删除知识库将同时删除所有文档，此操作不可恢复" placement="top">
                <el-button size="small" type="danger" @click="confirmDeleteKb(row)">删除</el-button>
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="page"
          :page-size="20"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadKbs"
          style="margin-top:12px;justify-content:center"
        />
      </el-tab-pane>

      <!-- ═══ 知识图谱 ═══ -->
      <el-tab-pane name="kg">
        <template #label><span>🕸️ 知识图谱</span></template>
        <div style="padding:40px;text-align:center;color:#909399">
          <div style="font-size:48px;margin-bottom:16px">🕸️</div>
          <div style="font-size:18px;font-weight:600;margin-bottom:8px">知识图谱</div>
          <div style="font-size:13px">基于知识库实体构建可视化图谱，支持关系推理</div>
          <el-button type="primary" size="large" style="margin-top:24px" disabled>即将上线</el-button>
        </div>
      </el-tab-pane>

      <!-- ═══ 记忆中心 ═══ -->
      <el-tab-pane name="memory">
        <template #label><span>🧠 记忆中心</span></template>
        <div style="padding:40px;text-align:center;color:#909399">
          <div style="font-size:48px;margin-bottom:16px">🧠</div>
          <div style="font-size:18px;font-weight:600;margin-bottom:8px">Agent 记忆中心</div>
          <div style="font-size:13px">存储 Agent 长期记忆，支持跨会话上下文恢复</div>
          <el-button type="primary" size="large" style="margin-top:24px" disabled>即将上线</el-button>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- ==================== 文档管理抽屉 ==================== -->
    <el-drawer v-model="docsDrawer" :title="currentKb ? currentKb.name + ' - 文档管理' : '文档管理'" size="560px" direction="rtl">
      <el-tabs v-model="docsTab" class="docs-tabs">
        <!-- ========== 文档列表 tab ========== -->
        <el-tab-pane label="文档列表" name="docs">
          <div class="docs-toolbar">
            <el-button type="primary" size="small" @click="openUploadWizard">
              <el-icon><Upload /></el-icon>上传文档
            </el-button>
            <el-button size="small" :disabled="!selectedDocIds.length" @click="openBatchReindex">
              <el-icon><Refresh /></el-icon>批量重新索引{{ selectedDocIds.length ? ` (${selectedDocIds.length})` : '' }}
            </el-button>
            <el-button size="small" type="danger" :disabled="!selectedDocIds.length" @click="openBatchDelete">
              <el-icon><Delete /></el-icon>批量删除{{ selectedDocIds.length ? ` (${selectedDocIds.length})` : '' }}
            </el-button>
            <el-button size="small" type="success" :disabled="!selectedDocIds.length" @click="openBatchExport">
              <el-icon><Download /></el-icon>导出{{ selectedDocIds.length ? ` (${selectedDocIds.length})` : '' }}
            </el-button>
            <el-button size="small" @click="refreshDocs">
              <el-icon><Refresh /></el-icon>刷新
            </el-button>
          </div>

          <el-table :data="docs" size="small" v-loading="docsLoading" stripe style="margin-top:10px" @selection-change="onDocSelectionChange">
            <el-table-column type="selection" width="40" />
            <el-table-column prop="size" label="大小" width="80" align="center">
              <template #default="{ row }">{{ formatSize(row.size) }}</template>
            </el-table-column>
            <el-table-column prop="chunkCount" label="分块数" width="70" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ row.chunkCount ?? '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="getDocStatusType(row.status)">
                  {{ getDocStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" align="center">
              <template #default="{ row }">
                <!-- Day 49: 预览按钮（在线阅读文档全文，支持移动端） -->
                <el-tooltip content="预览文档内容（在线阅读）" placement="top">
                  <el-button size="small" type="info" @click="openFullContent(row.id)">
                    <el-icon><View /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="编辑内容（修改后重新切片+索引）" placement="top">
                  <el-button size="small" type="primary" @click="openEditDoc(row)">
                    <el-icon><EditPen /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="重命名" placement="top">
                  <el-button size="small" type="default" @click="openRenameDoc(row)">
                    <el-icon><Edit /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <el-button size="small" type="danger" @click="confirmDeleteDoc(row)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-tooltip>
              </template>
            </el-table-column>
          </el-table>

          <!-- 空状态 -->
          <el-empty v-if="!docsLoading && docs.length === 0" description="暂无文档，请上传" :image-size="80" style="margin-top:40px" />
        </el-tab-pane>

        <!-- ========== 检索测试 tab ========== -->
        <el-tab-pane label="检索测试" name="retrieve">
          <div class="retrieve-panel">
            <!-- 检索输入 -->
            <el-form label-width="0" size="default">
              <el-form-item>
                <el-input
                  v-model="retrieveQuery"
                  type="textarea"
                  :rows="3"
                  placeholder="输入检索内容，例如：这句话是什么意思"
                  @keydown.ctrl.enter="doRetrieve"
                  @keydown.meta.enter="doRetrieve"
                />
              </el-form-item>
              <el-form-item label="系统提示模板" style="margin-bottom:8px">
                <el-select v-model="retrievePromptTemplate" placeholder="选择提示模板（可选）" clearable style="width:100%">
                  <el-option label="【默认】简洁检索" value="default" />
                  <el-option label="【详细】带上下文" value="detailed" />
                  <el-option label="【学术】引用文献" value="academic" />
                  <el-option label="【对比】多角度检索" value="multi" />
                </el-select>
              </el-form-item>
              <el-form-item style="margin-bottom:0">
                <el-button type="primary" :loading="retrieveLoading" :disabled="!retrieveQuery.trim()" @click="doRetrieve" style="width:100%">
                  <el-icon v-if="!retrieveLoading"><Search /></el-icon>开始检索
                </el-button>
              </el-form-item>
            </el-form>

            <el-divider />

            <!-- 检索结果 -->
            <div v-if="retrieveResults.length > 0" class="retrieve-results">
              <div class="retrieve-results-header">
                <span>检索结果</span>
                <el-tag size="small" type="info">{{ retrieveResults.length }} 条</el-tag>
              </div>
              <div v-for="(item, idx) in retrieveResults" :key="idx" class="retrieve-item">
                <div class="retrieve-item-header">
                  <!-- Day 50: 来源标注 (docTitle + 文档类型 + chunk 编号) -->
                  <div style="display:flex;align-items:center;gap:6px;flex:1;min-width:0">
                    <span class="retrieve-item-name" :title="item.docTitle">{{ item.docTitle || item.name || item.docName || ('结果 ' + (idx + 1)) }}</span>
                    <!-- Day 50: 文档类型标签 -->
                    <el-tag v-if="item.docSource" size="small" type="info" style="font-size:10px">
                      {{ fileTypeIcon(item.docSource) }} {{ item.docSource?.toUpperCase() }}
                    </el-tag>
                    <!-- Day 50: Chunk 编号 (来自第 N 个切片) -->
                    <el-tag v-if="item.chunkIndex != null" size="small" style="font-size:10px" :style="{ background: 'var(--el-fill-color)', border: 'none' }">
                      切片 {{ (item.chunkIndex + 1) }}
                    </el-tag>
                  </div>
                  <div class="retrieve-score-wrap">
                    <span class="retrieve-score-label">相关度</span>
                    <div class="retrieve-score-bar">
                      <div class="retrieve-score-fill" :style="{ width: ((item.score ?? item.relevance ?? 0) * 100).toFixed(1) + '%' }" />
                    </div>
                    <span class="retrieve-score-value">{{ ((item.score ?? item.relevance ?? 0) * 100).toFixed(1) }}%</span>
                  </div>
                  <el-button v-if="item.docId" size="small" type="primary" link style="margin-left:8px" @click.stop="openFullContent(item.docId)">
                    阅读全文
                  </el-button>
                </div>
                <div class="retrieve-item-excerpt" @click="toggleExcerpt(idx)">
                  <!-- 展开状态：显示完整内容并高亮关键词 -->
                  <div v-if="expandedIdx === idx" class="excerpt-expanded">
                    <!-- Day 49: 复制完整片段按钮 -->
                    <div style="display:flex;justify-content:flex-end;margin-bottom:8px">
                      <el-button size="small" @click.stop="copyChunk(item, idx)">
                        <el-icon><DocumentCopy /></el-icon>复制片段
                      </el-button>
                    </div>
                    <div v-html="item.highlight
                      ? item.highlight
                      : highlightKeyword(item.excerpt || item.content || item.text || '', retrieveQuery)" />
                  </div>
                  <!-- 收起状态：显示摘要 -->
                  <div v-else>
                    <span v-if="item.highlight" v-html="item.highlight" />
                    <span v-else>{{ item.excerpt || item.content || item.text || '（无内容）' }}</span>
                    <el-icon v-if="(item.highlight || item.excerpt || item.content) && (item.highlight || item.excerpt || item.content).length > 120"><ArrowDown /></el-icon>
                  </div>
                </div>
              </div>
            </div>

            <!-- 检索空态 -->
            <el-empty v-else-if="!retrieveLoading && retrieveDone" description="未找到相关结果，请尝试调整检索内容" :image-size="60" style="margin-top:20px" />

            <!-- 初始提示 -->
            <div v-else-if="!retrieveLoading && !retrieveDone" class="retrieve-hint">
              <el-icon size="32" color="#c0d0e0"><Search /></el-icon>
              <p>输入检索内容，点击「开始检索」</p>
              <p style="font-size:12px;color:#999">Ctrl + Enter 快捷检索</p>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-drawer>

    <!-- ==================== 上传向导弹窗 ==================== -->
    <el-dialog v-model="uploadWizardVisible" title="上传文档" width="600px" :close-on-click-modal="uploadStep === 'done'" destroy-on-close>
      <!-- 步骤条 -->
      <el-steps :active="stepIndex" finish-status="success" style="margin-bottom:28px" align-center>
        <el-step title="选择文件" />
        <el-step title="确认配置" />
        <el-step title="上传中" />
        <el-step title="完成" />
      </el-steps>

      <!-- Step 0: 选择文件 -->
      <div v-if="uploadStep === 'select'" class="wizard-step">
        <el-upload
          ref="uploadRef"
          drag
          :auto-upload="false"
          :limit="10"
          :on-change="onFileChange"
          :on-remove="onFileRemove"
          :file-list="uploadFileList"
          accept=".pdf,.doc,.docx,.txt,.md"
          class="upload-drag"
        >
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-text">将文件拖拽到此处，或 <em>点击上传</em></div>
          <template #tip>
            <div class="upload-tip">
              支持 <b>PDF / Word / TXT / Markdown</b>，单文件不超过 50MB，可同时上传多个文件
            </div>
          </template>
        </el-upload>

        <div class="format-tips">
          <div class="format-tips-title"><el-icon><InfoFilled /></el-icon> 支持格式说明</div>
          <ul>
            <li><b>PDF</b> — 学术论文、报告、扫描件（需OCR支持）</li>
            <li><b>Word (.doc/.docx)</b> — 正式文档、合同、规范</li>
            <li><b>TXT</b> — 纯文本，最大 50MB</li>
            <li><b>Markdown (.md)</b> — 格式文档、笔记、README</li>
          </ul>
        </div>

        <div class="wizard-footer">
          <el-button @click="uploadWizardVisible = false">取消</el-button>
          <el-button type="primary" :disabled="uploadFileList.length === 0" @click="goUploadConfig">下一步</el-button>
        </div>
      </div>

      <!-- Step 1: 确认配置 -->
      <div v-else-if="uploadStep === 'config'" class="wizard-step">
        <div class="file-list-review">
          <div class="file-list-title">已选文件（{{ uploadFileList.length }} 个）</div>
          <div v-for="file in uploadFileList" :key="file.uid" class="file-item">
            <el-icon><Document /></el-icon>
            <span class="file-item-name">{{ file.name }}</span>
            <span class="file-item-size">{{ formatSize(file.size) }}</span>
          </div>
        </div>

        <el-form label-width="90px" style="margin-top:20px" size="default">
          <el-form-item label="所属知识库">
            <el-tag>{{ currentKb?.name }}</el-tag>
          </el-form-item>
          <el-form-item label="分块策略">
            <el-select v-model="uploadChunkMode" style="width:100%">
              <el-option label="自动（默认）" value="auto" />
              <el-option label="按段落" value="paragraph" />
              <el-option label="固定长度" value="fixed" />
            </el-select>
          </el-form-item>
          <el-form-item label="标签（可选）">
            <el-select v-model="uploadTags" multiple filterable allow-create default-first-option placeholder="输入标签后回车" style="width:100%" />
          </el-form-item>
        </el-form>

        <div class="wizard-footer">
          <el-button @click="uploadStep = 'select'">上一步</el-button>
          <el-button type="primary" :loading="uploadingAll" @click="startUploadAll">开始上传</el-button>
        </div>
      </div>

      <!-- Step 2: 上传中 -->
      <div v-else-if="uploadStep === 'uploading'" class="wizard-step">
        <div class="upload-progress-list">
          <div v-for="item in uploadProgressList" :key="item.uid" class="upload-progress-item">
            <div class="upload-progress-info">
              <el-icon><Document /></el-icon>
              <span class="upload-progress-name">{{ item.name }}</span>
              <el-tag v-if="item.status === 'done'" size="small" type="success">完成</el-tag>
              <el-tag v-else-if="item.status === 'error'" size="small" type="danger">失败</el-tag>
              <el-tag v-else-if="item.stage === 'RETRYING'" size="small" type="warning">
                重试 {{ item.retryCount }}/4 · {{ Math.round(item.retryDelay/1000) }}s
              </el-tag>
              <span v-else class="upload-pct">{{ stageLabel(item.stage) }} {{ item.progress }}%</span>
            </div>
            <el-progress :percentage="item.progress" :status="item.status === 'done' ? 'success' : item.status === 'error' ? 'exception' : undefined" :show-text="true" :stroke-width="6" />
            <div v-if="item.error" class="upload-error-msg">{{ item.error }}</div>
          </div>
        </div>
      </div>

      <!-- Step 3: 完成 -->
      <div v-else-if="uploadStep === 'done'" class="wizard-step">
        <div class="done-panel">
          <el-icon class="done-icon" color="#67c23a"><CircleCheck /></el-icon>
          <h3>上传完成</h3>
          <p>{{ uploadSuccessCount }} 个文件已成功上传，文档将自动处理并向量化。</p>
          <p style="font-size:13px;color:#999">处理完成后可在「文档列表」中查看状态。</p>
        </div>
        <div class="wizard-footer">
          <el-button @click="resetUploadWizard">继续上传</el-button>
          <el-button type="primary" @click="finishUploadWizard">完成</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- ==================== 新建/编辑知识库弹窗 ==================== -->
    <el-dialog v-model="formVisible" :title="formMode === 'create' ? '新建知识库' : '编辑知识库'" width="500px" destroy-on-close>
      <el-form :model="form" label-width="90px" size="default">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="输入知识库名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="简要描述知识库的用途" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="向量模型">
          <el-select v-model="form.embeddingModel" style="width:100%">
            <el-option label="bge-large-zh（推荐）" value="bge-large-zh" />
            <el-option label="text-embedding-ada-002" value="ada-002" />
            <el-option label="m3e-large" value="m3e-large" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveKb">确定</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 重命名文档弹窗 ==================== -->
    <el-dialog v-model="renameDialogVisible" title="重命名文档" width="420px" destroy-on-close>
      <el-form label-width="70px" size="default">
        <el-form-item label="新名称">
          <el-input v-model="renameDocName" placeholder="输入新文件名" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="renamingDoc" @click="doRenameDoc">确定</el-button>
      </template>
    </el-dialog>

    <!-- 文档全文阅读弹窗 (Day 44 → Day 49: 移动端适配 + 复制按钮 + 改进排版) -->
    <el-dialog
      v-model="fullContentVisible"
      :title="'📖 ' + (fullContentDoc?.title || '文档预览')"
      width="90vw"
      max-width="860px"
      destroy-on-close
    >
      <div v-if="fullContentLoading" style="text-align:center;padding:40px">
        <el-icon class="is-loading" style="font-size:32px;color:#409eff"><Loading /></el-icon>
        <div style="margin-top:8px;color:#909399">加载中...</div>
      </div>
      <div v-else-if="fullContentDoc">
        <!-- 文档基本信息 -->
        <el-descriptions :column="2" border size="small" style="margin-bottom:16px">
          <el-descriptions-item label="文档名">{{ fullContentDoc.title }}</el-descriptions-item>
          <el-descriptions-item label="类型">
            <!-- Day 50: 文件类型语义化图标 -->
            <el-tag size="small" :type="fileTypeTagType(fullContentDoc.sourceType)">
              {{ fileTypeIcon(fullContentDoc.sourceType) }} {{ fullContentDoc.sourceType || '-' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="大小">{{ fullContentDoc.sizeBytes ? (fullContentDoc.sizeBytes / 1024).toFixed(1) + ' KB' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="切片数">{{ fullContentDoc.chunkCount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ fullContentDoc.createdAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="fullContentDoc.status === 'DONE' ? 'success' : fullContentDoc.status === 'ERROR' ? 'danger' : 'info'">
              {{ fullContentDoc.status || '-' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <!-- 正文内容区域 -->
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:10px">
          <div style="font-size:14px;font-weight:600">
            正文内容
            <!-- Day 50: mammoth.js CDN 动态加载提示 (DOCX) / markdown-it 渲染提示 (MD) -->
            <span v-if="fullContentDoc.sourceType?.toUpperCase() === 'DOCX'" style="font-size:11px;font-weight:400;color:#67c23a;margin-left:8px">
              (Word 文档 · 格式化渲染)
            </span>
            <span v-else-if="fullContentDoc.sourceType?.toUpperCase() === 'MD'" style="font-size:11px;font-weight:400;color:#409eff;margin-left:8px">
              (Markdown · 已渲染)
            </span>
            <span v-else-if="fullContentDoc.sourceType?.toUpperCase() === 'PDF'" style="font-size:11px;font-weight:400;color:#e6a23c;margin-left:8px">
              (PDF 提取文本)
            </span>
            <span style="font-size:11px;font-weight:400;color:#909399;margin-left:8px">
              {{ (fullContentDoc.content || '').length }} 字符
            </span>
          </div>
          <el-button size="small" @click="copyDocContent">
            <el-icon><DocumentCopy /></el-icon>复制全文
          </el-button>
        </div>
        <!-- Day 50: 预览容器 — 根据文件类型差异化渲染 -->
        <div class="doc-preview-body">
          <!-- Markdown 渲染 (使用 markdown-it) -->
          <div v-if="fullContentDoc.sourceType?.toUpperCase() === 'MD'" v-html="renderMarkdown(fullContentDoc.content)" class="md-rendered" />
          <!-- Word DOCX — mammoth.js CDN 渲染 -->
          <div v-else-if="fullContentDoc.sourceType?.toUpperCase() === 'DOCX'">
            <div v-if="docxRendering" style="text-align:center;padding:20px">
              <el-icon class="is-loading" style="font-size:20px;color:#67c23a"><Loading /></el-icon>
              <span style="margin-left:8px;color:#67c23a;font-size:13px">加载 Word 渲染引擎…</span>
            </div>
            <div v-else-if="docxHtml" v-html="docxHtml" class="docx-rendered" />
            <div v-else class="doc-preview-plain">{{ fullContentDoc.content || '（无内容）' }}</div>
          </div>
          <!-- 其他类型 (PDF/TXT) 纯文本展示 -->
          <div v-else class="doc-preview-plain">{{ fullContentDoc.content || '（无内容）' }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="fullContentVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 文档在线编辑弹窗 (Day 45) -->
    <el-dialog v-model="editDocVisible" title="在线编辑文档内容" width="860px" destroy-on-close>
      <div v-if="editDocLoading" style="text-align:center;padding:40px">
        <el-icon class="is-loading" style="font-size:32px;color:#409eff"><Loading /></el-icon>
      </div>
      <div v-else-if="editDoc">
        <el-alert type="warning" :closable="false" style="margin-bottom:12px">
          <template #title>
            修改文档内容后将自动 <strong>重新切片</strong> 并 <strong>重新向量化索引</strong>，
            预计耗时与文档长度成正比，请耐心等待完成。
          </template>
        </el-alert>
        <el-descriptions :column="3" border size="small" style="margin-bottom:14px">
          <el-descriptions-item label="文档名">{{ editDoc.title }}</el-descriptions-item>
          <el-descriptions-item label="当前切片数">{{ editDoc.chunkCount || 0 }} 个</el-descriptions-item>
          <el-descriptions-item label="原大小">{{ editDoc.sizeBytes ? (editDoc.sizeBytes / 1024).toFixed(1) + ' KB' : '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-form label-width="0" size="default">
          <el-form-item label="正文内容">
            <el-input
              v-model="editDocContent"
              type="textarea"
              :rows="18"
              placeholder="在此输入文档正文..."
              style="font-family:monospace;font-size:13px"
            />
          </el-form-item>
          <el-form-item style="margin-bottom:0">
            <span style="font-size:12px;color:#909399">
              字数：{{ editDocContent.length }} |
              预计切片：{{ Math.ceil(editDocContent.length / 300) }} 个（以 300 字/片估算）
            </span>
          </el-form-item>
        </el-form>
        <!-- 处理进度 -->
        <div v-if="editDocSaving" style="margin-top:12px">
          <el-progress :percentage="editDocProgress" :status="editDocProgress >= 100 ? 'success' : undefined" :indeterminate="editDocProgress < 20" />
          <div style="text-align:center;font-size:13px;color:#606266;margin-top:4px">{{ editDocProgressMsg }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="editDocVisible = false" :disabled="editDocSaving">取消</el-button>
        <el-button type="primary" :loading="editDocSaving" @click="doSaveEditDoc">
          保存并重新索引
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量重新索引弹窗 (Day 46) -->
    <el-dialog v-model="batchReindexVisible" title="批量重新索引" width="560px" destroy-on-close>
      <div v-if="!batchReindexResult">
        <el-alert type="info" :closable="false" style="margin-bottom:14px">
          确认对 <strong>{{ selectedDocIds.length }}</strong> 个文档执行重新切片 + 重新向量化索引。
          此操作将删除旧切片并重建索引，预计耗时与文档数量和长度成正比。
        </el-alert>
        <div style="margin-bottom:12px;font-size:13px;color:#606266">
          已选择文档 IDs：<code>{{ selectedDocIds.join(', ') }}</code>
        </div>
        <div v-if="batchReindexLoading" style="margin-top:12px">
          <el-progress :percentage="batchReindexProgress" :status="batchReindexProgress >= 100 ? 'success' : undefined" :indeterminate="batchReindexProgress < 20" />
          <div style="text-align:center;font-size:13px;color:#606266;margin-top:4px">{{ batchReindexMsg }}</div>
        </div>
        <div v-else style="text-align:center;font-size:14px;color:#409eff;padding:10px">{{ batchReindexMsg }}</div>
      </div>
      <!-- 结果展示 -->
      <div v-else>
        <el-result icon="success" title="批量重索引完成">
          <template #sub-title>
            <p>成功 <strong style="color:#67c23a">{{ batchReindexResult.succeeded }}</strong> 个文档</p>
            <p v-if="batchReindexResult.failed?.length">失败 <strong style="color:#f56c6c">{{ batchReindexResult.failed.length }}</strong> 个文档</p>
          </template>
        </el-result>
        <div v-if="batchReindexResult.failed?.length" style="max-height:160px;overflow-y:auto;border:1px solid #f5f5f5;border-radius:4px;padding:10px">
          <div style="font-size:13px;font-weight:600;color:#f56c6c;margin-bottom:8px">失败详情</div>
          <div v-for="(f, i) in batchReindexResult.failed" :key="i" style="font-size:12px;color:#606266;margin-bottom:4px">
            <code>docId={{ f.docId }}</code>: {{ f.error }}
          </div>
        </div>
      </div>
      <template #footer>
        <template v-if="!batchReindexResult">
          <el-button @click="batchReindexVisible = false" :disabled="batchReindexLoading">取消</el-button>
          <el-button type="warning" :loading="batchReindexLoading" @click="doBatchReindex">
            确认重新索引
          </el-button>
        </template>
        <el-button v-else type="primary" @click="batchReindexVisible = false; selectedDocIds = []">完成</el-button>
      </template>
    </el-dialog>

    <!-- 批量删除弹窗 (Day 47) -->
    <el-dialog v-model="batchDeleteVisible" title="批量删除文档" width="560px" destroy-on-close>
      <div v-if="!batchDeleteResult">
        <el-alert type="error" :closable="false" style="margin-bottom:14px">
          确认删除 <strong>{{ selectedDocIds.length }}</strong> 个文档？
          此操作不可恢复，文档内容和所有切片将一并被删除！
        </el-alert>
        <div style="margin-bottom:12px;font-size:13px;color:#606266">
          已选择文档 IDs：<code>{{ selectedDocIds.join(', ') }}</code>
        </div>
        <div v-if="batchDeleteLoading" style="margin-top:12px">
          <el-progress :percentage="batchDeleteProgress" :status="batchDeleteProgress >= 100 ? 'success' : undefined" :indeterminate="batchDeleteProgress < 20" />
          <div style="text-align:center;font-size:13px;color:#606266;margin-top:4px">正在删除文档...</div>
        </div>
      </div>
      <!-- 结果展示 -->
      <div v-else>
        <el-result :icon="batchDeleteResult.succeeded > 0 ? 'success' : 'warning'" :title="batchDeleteResult.succeeded > 0 ? '删除完成' : '删除结果'">
          <template #sub-title>
            <p>成功 <strong style="color:#67c23a">{{ batchDeleteResult.succeeded }}</strong> 个文档</p>
            <p v-if="batchDeleteResult.failed?.length">失败 <strong style="color:#f56c6c">{{ batchDeleteResult.failed.length }}</strong> 个文档</p>
          </template>
        </el-result>
        <div v-if="batchDeleteResult.failed?.length" style="max-height:160px;overflow-y:auto;border:1px solid #f5f5f5;border-radius:4px;padding:10px">
          <div style="font-size:13px;font-weight:600;color:#f56c6c;margin-bottom:8px">失败详情</div>
          <div v-for="(f, i) in batchDeleteResult.failed" :key="i" style="font-size:12px;color:#606266;margin-bottom:4px">
            <code>docId={{ f.docId }}</code>: {{ f.error }}
          </div>
        </div>
      </div>
      <template #footer>
        <template v-if="!batchDeleteResult">
          <el-button @click="batchDeleteVisible = false" :disabled="batchDeleteLoading">取消</el-button>
          <el-button type="danger" :loading="batchDeleteLoading" @click="doBatchDelete">确认删除</el-button>
        </template>
        <el-button v-else type="primary" @click="batchDeleteVisible = false; selectedDocIds = []; batchDeleteResult = null">完成</el-button>
      </template>
    </el-dialog>

    <!-- 批量导出弹窗 (Day 48) -->
    <el-dialog v-model="batchExportVisible" title="批量导出文档" width="500px" destroy-on-close>
      <div v-if="!batchExportDone">
        <el-form label-width="80px" size="default">
          <el-form-item label="导出格式">
            <el-radio-group v-model="exportFormat">
              <el-radio label="txt">TXT（纯文本，UTF-8）</el-radio>
              <el-radio label="pdf">PDF（多页排版）</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="已选文档">
            <el-tag type="info">{{ selectedDocIds.length }} 个文档</el-tag>
            <span style="margin-left:8px;font-size:12px;color:#909399">IDs: {{ selectedDocIds.slice(0, 5).join(', ') }}{{ selectedDocIds.length > 5 ? '...' : '' }}</span>
          </el-form-item>
          <el-form-item label="导出说明">
            <span style="font-size:12px;color:#909399">
              导出将合并所有文档内容，{{ exportFormat === 'pdf' ? '生成 PDF 文件' : '生成 TXT 文件' }}下载。
            </span>
          </el-form-item>
        </el-form>
        <div v-if="batchExportLoading" style="margin-top:12px">
          <el-progress :percentage="batchExportProgress" :status="batchExportProgress >= 100 ? 'success' : undefined" />
          <div style="text-align:center;font-size:13px;color:#606266;margin-top:4px">正在生成文件...</div>
        </div>
      </div>
      <div v-else>
        <el-result icon="success" title="导出完成" sub-title="文件已开始下载，如未下载请重试">
          <template #extra>
            <el-button type="primary" @click="batchExportVisible = false; batchExportDone = false; selectedDocIds = []">完成</el-button>
          </template>
        </el-result>
      </div>
      <template #footer>
        <el-button v-if="!batchExportDone" @click="batchExportVisible = false" :disabled="batchExportLoading">取消</el-button>
        <el-button v-if="!batchExportDone" type="success" :loading="batchExportLoading" @click="doBatchExport">
          <el-icon><Download /></el-icon>开始导出
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, shallowRef } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listMyKbs, createKb, updateKb, deleteKb,
  listDocs, uploadDoc, uploadDocStream, deleteDoc, renameDoc,
  retrieve, getDocContent, updateDocContent, batchReindexDocs, batchDeleteDocs, exportDocs
} from '@/api/rag'
import http from '@/api/http'
import { useUserStore } from '@/store/user'
import {
  Plus, Upload, UploadFilled, Refresh, Edit, EditPen, Delete, DocumentCopy,
  Search, ArrowDown, Document, CircleCheck, InfoFilled, Loading, Download, View
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const userId = computed(() => userStore.profile?.id || userStore.userInfo?.id || null)
const route = useRoute()
const activeTab = ref(route.query.tab || 'kb')

// ========== 文档全文阅读 (Day 44) ==========
const fullContentVisible = ref(false)
const fullContentLoading = ref(false)
const fullContentDoc = ref(null)

async function openFullContent(docId) {
  fullContentVisible.value = true
  fullContentLoading.value = true
  fullContentDoc.value = null
  // Day 50: 重置 DOCX 渲染状态
  docxHtml.value = null
  docxRendering.value = false
  try {
    const r = await getDocContent(docId)
    fullContentDoc.value = r.data
    // Day 50: DOCX 文件自动触发 mammoth.js CDN 渲染
    if (r.data?.sourceType?.toUpperCase() === 'DOCX' && r.data?.content) {
      renderDocxContent(r.data.content)
    }
  } catch (e) {
    ElMessage.error('加载文档内容失败: ' + (e.message || ''))
  } finally {
    fullContentLoading.value = false
  }
}

/** Day 50: DOCX 渲染 — 动态加载 mammoth.js CDN */
const docxHtml = ref(null)
const docxRendering = ref(false)

async function renderDocxContent(content) {
  // mammoth.js CDN 地址 (jsDelivr, 稳定可靠)
  const MAMMOTH_CDN = 'https://cdn.jsdelivr.net/npm/mammoth@1.9.0/mammoth.browser.min.js'
  if (!window.mammoth) {
    docxRendering.value = true
    try {
      await new Promise((resolve, reject) => {
        const script = document.createElement('script')
        script.src = MAMMOTH_CDN
        script.onload = resolve
        script.onerror = reject
        document.head.appendChild(script)
      })
    } catch {
      docxRendering.value = false
      return // 加载失败，降级显示纯文本
    }
    docxRendering.value = false
  }
  // mammoth 需要 ArrayBuffer，content 是 base64 或 plain text
  // 尝试 base64 解码；若失败则直接用纯文本
  try {
    const arrayBuffer = Uint8Array.from(atob(content.replace(/\s/g, '')), c => c.charCodeAt(0)).buffer
    const result = await window.mammoth.convertToHtml({ arrayBuffer })
    docxHtml.value = result.value
  } catch {
    docxHtml.value = null // 降级到纯文本
  }
}

/** Day 50: 文件类型图标 */
function fileTypeIcon(sourceType) {
  const t = sourceType?.toUpperCase() || ''
  if (t === 'PDF') return '📄'
  if (t === 'DOCX' || t === 'DOC') return '📝'
  if (t === 'MD') return '📋'
  if (t === 'TXT') return '📃'
  return '📄'
}

/** Day 50: 文件类型标签颜色 */
function fileTypeTagType(sourceType) {
  const t = sourceType?.toUpperCase() || ''
  if (t === 'PDF') return 'danger'
  if (t === 'DOCX' || t === 'DOC') return 'primary'
  if (t === 'MD') return ''
  if (t === 'TXT') return 'info'
  return 'info'
}

/** Day 50: Markdown 渲染 (使用 markdown-it — CDN fallback) */
async function renderMarkdown(content) {
  if (!content) return ''
  try {
    if (!window.markdownitInstance) {
      if (!window.markdownit) {
        await import('markdown-it').then(m => {
          window.markdownit = m.default || m
        })
      }
      window.markdownitInstance = new window.markdownit({ html: false, linkify: true, typographer: true })
    }
    return window.markdownitInstance.render(content)
  } catch {
    return content // 降级：返回原始文本
  }
}

/** Day 49: 复制文档全文内容 */
async function copyDocContent() {
  const text = fullContentDoc.value?.content
  if (!text) {
    ElMessage.warning('文档内容为空，无法复制')
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制 ' + text.length + ' 字符到剪贴板')
  } catch {
    // 降级：创建临时 textarea
    const ta = document.createElement('textarea')
    ta.value = text
    ta.style.cssText = 'position:fixed;top:-999px;left:-999px'
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    ElMessage.success('已复制 ' + text.length + ' 字符到剪贴板')
  }
}

// ========== 数据状态 ==========
const kbs = ref([])
const docs = ref([])
const loading = ref(false)
const saving = ref(false)
const docsLoading = ref(false)
const keyword = ref('')
const page = ref(1)
const total = ref(0)

// ========== 知识库表单 ==========
const formVisible = ref(false)
const formMode = ref('create') // 'create' | 'edit'
const currentKb = ref(null)
const form = ref({ name: '', description: '', embeddingModel: 'bge-large-zh' })

// ========== 文档抽屉 ==========
const docsDrawer = ref(false)
const docsTab = ref('docs')

// ========== 上传向导 ==========
const uploadWizardVisible = ref(false)
const uploadStep = ref('select') // 'select' | 'config' | 'uploading' | 'done'
const uploadRef = ref(null)
const uploadFileList = ref([])
const uploadChunkMode = ref('auto')
const uploadTags = ref([])
const uploadingAll = ref(false)
const uploadProgressList = ref([])
const uploadSuccessCount = ref(0)

const stepIndex = computed(() => {
  const map = { select: 0, config: 1, uploading: 2, done: 3 }
  return map[uploadStep.value] ?? 0
})

// ========== 检索测试 ==========
const retrieveQuery = ref('')
const retrievePromptTemplate = ref('default')
const retrieveLoading = ref(false)
const retrieveResults = ref([])
const retrieveDone = ref(false)
const expandedIdx = ref(-1)  // 当前展开的行索引，同一时间只展开一行

// 高亮关键词（用于展开后的完整内容区域）
function highlightKeyword(text, keyword) {
  if (!text || !keyword?.trim()) return escapeHtml(text || '')
  const escaped = keyword.trim().replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const re = new RegExp(`(${escaped})`, 'gi')
  return escapeHtml(text).replace(re, '<mark>$1</mark>')
}

function escapeHtml(str) {
  if (!str) return ''
  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

// ========== 重命名文档 ==========
const renameDialogVisible = ref(false)
const renameTargetDoc = ref(null)
const renameDocName = ref('')
const renamingDoc = ref(false)

// ========== 在线编辑文档 (Day 45) ==========
const editDocVisible = ref(false)
const editDocLoading = ref(false)
const editDocSaving = ref(false)
const editDoc = ref(null)       // 文档元信息
const editDocContent = ref('')  // 可编辑的正文
const editDocProgress = ref(0)
const editDocProgressMsg = ref('准备中...')

// ========== 批量重新索引 (Day 46) ==========
const batchReindexVisible = ref(false)
const batchReindexLoading = ref(false)
const batchReindexProgress = ref(0)
const batchReindexMsg = ref('')
const batchReindexResult = ref(null)   // { succeeded, failed }
const selectedDocIds = ref([])

// ========== 批量删除 (Day 47) ==========
const batchDeleteVisible = ref(false)
const batchDeleteLoading = ref(false)
const batchDeleteProgress = ref(0)
const batchDeleteResult = ref(null)   // { succeeded, failed }

// ========== 批量导出 (Day 48) ==========
const batchExportVisible = ref(false)
const exportFormat = ref('txt')
const batchExportProgress = ref(0)
const batchExportDone = ref(false)
const batchExportLoading = ref(false)

function openBatchExport() {
  if (!selectedDocIds.value.length) {
    ElMessage.warning('请先勾选要导出的文档')
    return
  }
  batchExportVisible.value = true
  batchExportDone.value = false
  batchExportProgress.value = 0
  batchExportLoading.value = false
  exportFormat.value = 'txt'
}

async function doBatchExport() {
  if (!selectedDocIds.value.length) return
  batchExportLoading.value = true
  batchExportProgress.value = 10

  const timer = setInterval(() => {
    if (batchExportProgress.value < 90) {
      batchExportProgress.value += Math.floor(Math.random() * 12) + 5
      if (batchExportProgress.value > 90) batchExportProgress.value = 90
    }
  }, 600)

  try {
    const r = await exportDocs(userId.value, selectedDocIds.value, exportFormat.value)
    clearInterval(timer)
    batchExportProgress.value = 100
    batchExportDone.value = true
    // r is already the Blob (from responseType: 'blob')
    const blob = r?.bytes || r
    if (blob) {
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `documents-export.${exportFormat.value}`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
      ElMessage.success(`文档已导出为 ${exportFormat.value.toUpperCase()} 文件`)
    }
  } catch (e) {
    clearInterval(timer)
    batchExportProgress.value = 0
    ElMessage.error('导出失败: ' + (e.message || ''))
  } finally {
    batchExportLoading.value = false
  }
}

function openBatchDelete() {
  if (!selectedDocIds.value.length) {
    ElMessage.warning('请先勾选要删除的文档')
    return
  }
  batchDeleteVisible.value = true
  batchDeleteLoading.value = false
  batchDeleteProgress.value = 0
  batchDeleteResult.value = null
}

async function doBatchDelete() {
  if (!selectedDocIds.value.length) return
  batchDeleteLoading.value = true
  batchDeleteProgress.value = 10

  const timer = setInterval(() => {
    if (batchDeleteProgress.value < 90) {
      batchDeleteProgress.value += Math.floor(Math.random() * 15) + 5
      if (batchDeleteProgress.value > 90) batchDeleteProgress.value = 90
    }
  }, 400)

  try {
    const r = await batchDeleteDocs(userId.value, selectedDocIds.value)
    clearInterval(timer)
    batchDeleteProgress.value = 100
    batchDeleteResult.value = r.data || r.result
    ElMessage.success('批量删除完成：成功 ' + (r.data?.succeeded || 0) + ' 个，失败 ' + (r.data?.failed?.length || 0) + ' 个')
    refreshDocs()
    loadKbs()
  } catch (e) {
    clearInterval(timer)
    batchDeleteProgress.value = 0
    ElMessage.error('批量删除失败: ' + (e.message || ''))
  } finally {
    batchDeleteLoading.value = false
  }
}

// ========== 批量重新索引 (Day 46) ==========
function onDocSelectionChange(rows) {
  selectedDocIds.value = rows.map(r => r.id)
}

function openBatchReindex() {
  if (!selectedDocIds.value.length) {
    ElMessage.warning('请先勾选要重新索引的文档')
    return
  }
  batchReindexVisible.value = true
  batchReindexLoading.value = false
  batchReindexProgress.value = 0
  batchReindexMsg.value = '就绪，已选择 ' + selectedDocIds.value.length + ' 个文档'
  batchReindexResult.value = null
}

async function doBatchReindex() {
  if (!selectedDocIds.value.length) return
  batchReindexLoading.value = true
  batchReindexProgress.value = 5
  batchReindexMsg.value = '正在重新索引...'

  const timer = setInterval(() => {
    if (batchReindexProgress.value < 90) {
      batchReindexProgress.value += Math.floor(Math.random() * 10) + 5
      if (batchReindexProgress.value > 90) batchReindexProgress.value = 90
    }
  }, 800)

  try {
    const r = await batchReindexDocs(userId.value, selectedDocIds.value)
    clearInterval(timer)
    batchReindexProgress.value = 100
    batchReindexResult.value = r.data || r.result
    batchReindexMsg.value = '批量重索引完成！'
    ElMessage.success('批量重索引完成：成功 ' + (r.data?.succeeded || 0) + ' 个，失败 ' + (r.data?.failed?.length || 0) + ' 个')
    refreshDocs()
    loadKbs()
  } catch (e) {
    clearInterval(timer)
    batchReindexProgress.value = 0
    batchReindexMsg.value = ''
    ElMessage.error('批量重索引失败: ' + (e.message || ''))
  } finally {
    batchReindexLoading.value = false
  }
}

async function openEditDoc(doc) {
  editDocVisible.value = true
  editDocLoading.value = true
  editDocSaving.value = false
  editDoc.value = null
  editDocContent.value = ''
  editDocProgress.value = 0
  editDocProgressMsg.value = '加载中...'
  try {
    const r = await getDocContent(doc.id)
    editDoc.value = r.data
    editDocContent.value = r.data.content || ''
    editDocProgressMsg.value = '就绪，可以编辑内容后点击「保存并重新索引」'
  } catch (e) {
    ElMessage.error('加载文档内容失败: ' + (e.message || ''))
    editDocVisible.value = false
  } finally {
    editDocLoading.value = false
  }
}

async function doSaveEditDoc() {
  if (!editDocContent.value.trim()) {
    ElMessage.warning('内容不能为空')
    return
  }
  editDocSaving.value = true
  editDocProgress.value = 10
  editDocProgressMsg.value = '正在重新切片和向量化索引...'
  // 模拟进度（实际由后端 SSE 提供，这里先乐观 UI）
  const timer = setInterval(() => {
    if (editDocProgress.value < 90) {
      editDocProgress.value += Math.floor(Math.random() * 8) + 3
      if (editDocProgress.value > 90) editDocProgress.value = 90
      editDocProgressMsg.value = '向量化中 ' + editDocProgress.value + '%...'
    }
  }, 600)

  try {
    await updateDocContent(editDoc.value.id, userId.value, editDocContent.value)
    clearInterval(timer)
    editDocProgress.value = 100
    editDocProgressMsg.value = '处理完成！'
    ElMessage.success('文档内容更新成功，已重新切片索引')
    editDocVisible.value = false
    refreshDocs()
  } catch (e) {
    clearInterval(timer)
    editDocProgress.value = 0
    ElMessage.error('更新失败: ' + (e.message || ''))
  } finally {
    editDocSaving.value = false
  }
}

// ========== 加载知识库列表 ==========
async function loadKbs() {
  loading.value = true
  try {
    const r = await listMyKbs(userId.value)
    let list = r.data || []
    if (keyword.value.trim()) {
      list = list.filter(kb => kb.name && kb.name.includes(keyword.value.trim()))
    }
    kbs.value = list
    total.value = list.length
  } catch {
    kbs.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// ========== 打开新建/编辑知识库 ==========
function openCreateKb() {
  form.value = { name: '', description: '', embeddingModel: 'bge-large-zh' }
  formMode.value = 'create'
  formVisible.value = true
}

function editKb(kb) {
  currentKb.value = kb
  form.value = {
    name: kb.name,
    description: kb.description || '',
    embeddingModel: kb.embeddingModel || 'bge-large-zh'
  }
  formMode.value = 'edit'
  formVisible.value = true
}

async function saveKb() {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入知识库名称')
    return
  }
  saving.value = true
  try {
    if (formMode.value === 'create') {
      await createKb(userId.value, form.value)
      ElMessage.success('知识库创建成功')
    } else {
      await updateKb(currentKb.value.id, userId.value, form.value)
      ElMessage.success('知识库更新成功')
    }
    formVisible.value = false
    loadKbs()
  } catch (e) {
    ElMessage.error('保存失败：' + (e.message || ''))
  } finally {
    saving.value = false
  }
}

async function confirmDeleteKb(kb) {
  try {
    await ElMessageBox.confirm(
      `确认删除知识库「${kb.name}」？此操作不可恢复，关联的所有文档将被一并删除。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await deleteKb(kb.id, userId.value)
    ElMessage.success('已删除')
    loadKbs()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

// ========== 文档抽屉 ==========
async function viewDocs(kb) {
  currentKb.value = kb
  docsDrawer.value = true
  docsTab.value = 'docs'
  await refreshDocs()
}

async function refreshDocs() {
  if (!currentKb.value) return
  docsLoading.value = true
  try {
    const r = await listDocs(currentKb.value.id)
    docs.value = r.data || []
  } catch {
    docs.value = []
  } finally {
    docsLoading.value = false
  }
}

async function confirmDeleteDoc(doc) {
  try {
    await ElMessageBox.confirm(
      `确认删除文档「${doc.name}」？`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await deleteDoc(doc.id, userId.value)
    ElMessage.success('文档已删除')
    refreshDocs()
    // 同步刷新知识库列表
    loadKbs()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

// 文档状态映射
function getDocStatusType(status) {
  const map = { READY: 'success', PROCESSING: 'warning', FAILED: 'danger', UPLOADING: 'info' }
  return map[status] || 'info'
}
function getDocStatusLabel(status) {
  const map = { READY: '就绪', PROCESSING: '处理中', FAILED: '失败', UPLOADING: '上传中' }
  return map[status] || status || '未知'
}

// 文件大小格式化
function formatSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

// ========== 重命名文档 ==========
function openRenameDoc(doc) {
  renameTargetDoc.value = doc
  renameDocName.value = doc.name || ''
  renameDialogVisible.value = true
}

async function doRenameDoc() {
  if (!renameDocName.value.trim()) {
    ElMessage.warning('请输入新名称')
    return
  }
  renamingDoc.value = true
  try {
    await renameDoc(renameTargetDoc.value.id, userId.value, renameDocName.value.trim())
    ElMessage.success('重命名成功')
    renameDialogVisible.value = false
    refreshDocs()
  } catch {
    ElMessage.error('重命名失败')
  } finally {
    renamingDoc.value = false
  }
}

// ========== 上传向导 ==========
function openUploadWizard() {
  resetUploadWizard()
  uploadWizardVisible.value = true
}

function onFileChange(file, list) {
  uploadFileList.value = list
}

function onFileRemove(file, list) {
  uploadFileList.value = list
}

function goUploadConfig() {
  if (uploadFileList.value.length === 0) {
    ElMessage.warning('请先选择文件')
    return
  }
  uploadStep.value = 'config'
}

async function startUploadAll() {
  if (uploadFileList.value.length === 0) return
  uploadingAll.value = true
  uploadStep.value = 'uploading'

  // 初始化进度列表
  uploadProgressList.value = uploadFileList.value.map(f => ({
    uid: f.uid,
    name: f.name,
    progress: 0,
    status: 'pending',
    stage: 'pending',  // pending | UPLOAD | PARSING | CHUNKING | EMBEDDING | INDEXING | DONE | ERROR | RETRYING
    error: '',
    retryCount: 0,
    retryDelay: 0  // 下次重试倒计时(ms)
  }))

  uploadSuccessCount.value = 0
  let successCount = 0

  // 逐个上传（使用 SSE 真实进度）
  for (const file of uploadFileList.value) {
    const progItem = uploadProgressList.value.find(p => p.uid === file.uid)
    if (!progItem) continue

    progItem.status = 'uploading'

    try {
      const tags = uploadTags.value.join(',')
      const { promise, cancel } = uploadDocStream(
        String(userId.value),
        String(currentKb.value.id),
        file.raw,
        {
          tags,
          onProgress(event) {
            // SSE 真实进度: stage + progress + message
            progItem.stage = event.stage
            progItem.progress = event.progress ?? 0
            progItem.status = event.stage === 'ERROR' ? 'error' : 'uploading'
            if (event.error) progItem.error = event.error
          },
          onRetry(attempt, maxRetries, delayMs) {
            // 自动重试中：显示重试状态
            progItem.retryCount = attempt
            progItem.retryDelay = delayMs
            progItem.stage = 'RETRYING'
            progItem.status = 'uploading'
            progItem.error = `网络异常(${attempt}/${maxRetries})，${Math.round(delayMs/1000)}s 后自动重试...`
          }
        }
      )

      await promise
      progItem.progress = 100
      progItem.status = 'done'
      successCount++
    } catch (e) {
      progItem.status = 'error'
      progItem.error = e.message || '上传失败'
    }
  }

  uploadingAll.value = false
  uploadSuccessCount.value = successCount
  uploadStep.value = 'done'

  if (successCount > 0) {
    ElMessage.success(`${successCount} 个文件上传成功，文档正在处理中...`)
  } else {
    ElMessage.error('所有文件上传失败')
  }
}

function resetUploadWizard() {
  uploadStep.value = 'select'
  uploadFileList.value = []
  uploadChunkMode.value = 'auto'
  uploadTags.value = []
  uploadingAll.value = false
  uploadProgressList.value = []
  uploadSuccessCount.value = 0
  if (uploadRef.value) uploadRef.value.clearFiles()
}

function finishUploadWizard() {
  uploadWizardVisible.value = false
  refreshDocs()
  loadKbs()
}

// ========== 检索测试 ==========
async function doRetrieve() {
  const q = retrieveQuery.value.trim()
  if (!q) {
    ElMessage.warning('请输入检索内容')
    return
  }
  if (!currentKb.value) {
    ElMessage.warning('请先选择一个知识库')
    return
  }

  retrieveLoading.value = true
  retrieveDone.value = false
  retrieveResults.value = []

  try {
    const body = {
      query: q,
      kbId: currentKb.value.id,
      topK: 10,
      scoreThreshold: 0.1
    }
    // 根据选择的模板追加 system prompt（如果后端支持）
    if (retrievePromptTemplate.value && retrievePromptTemplate.value !== 'default') {
      body.promptTemplate = retrievePromptTemplate.value
    }

    const r = await retrieve(body)
    // retrieve 返回 { data: [{ name, content, score, ... }] }
    retrieveResults.value = r.data || r.result || []
    retrieveDone.value = true
    if (retrieveResults.value.length === 0) {
      ElMessage.info('未找到相关结果')
    }
  } catch (e) {
    ElMessage.error('检索失败：' + (e.message || ''))
    retrieveDone.value = true
  } finally {
    retrieveLoading.value = false
  }
}

function toggleExcerpt(idx) {
  // 同一时间只展开一行，再次点击已展开行则收起
  expandedIdx.value = expandedIdx.value === idx ? -1 : idx
}

/** Day 49: 复制检索片段内容 */
async function copyChunk(item, idx) {
  const text = item.excerpt || item.content || item.text || ''
  if (!text) {
    ElMessage.warning('片段内容为空，无法复制')
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('片段已复制到剪贴板')
  } catch {
    const ta = document.createElement('textarea')
    ta.value = text
    ta.style.cssText = 'position:fixed;top:-999px;left:-999px'
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    ElMessage.success('片段已复制到剪贴板')
  }
}

// SSE 阶段标签 (Day 41)
function stageLabel(stage) {
  const map = {
    UPLOAD: '📤 上传', PARSING: '📖 解析', CHUNKING: '✂️ 切片',
    EMBEDDING: '🔢 向量', INDEXING: '💾 索引', DONE: '✅ 完成', ERROR: '❌ 错误'
  }
  return map[stage] || '⏳ 处理中'
}

// ========== 生命周期 ==========
onMounted(loadKbs)
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
  h2 { margin: 0; font-size: 16px; }
}
.search-bar { margin-bottom: 12px; }

// ========== 文档抽屉 ==========
.docs-tabs { height: 100%; }
.docs-toolbar {
  display: flex; gap: 8px; align-items: center;
}

// ========== 检索测试 ==========
.retrieve-panel { padding: 4px 0; }
.retrieve-hint {
  text-align: center; padding: 40px 0; color: #c0d0e0;
  p { margin: 8px 0 0; }
}
.retrieve-results { display: flex; flex-direction: column; gap: 12px; }
.retrieve-results-header {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 14px; font-weight: 600; color: var(--el-text-color-primary);
}
.retrieve-item {
  border: 1px solid var(--el-border-color-lighter); border-radius: 6px; padding: 12px;
  background: var(--el-fill-color-lightest);
  &:hover { border-color: var(--el-border-color); }
}
.retrieve-item-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;
}
.retrieve-item-name { font-size: 13px; font-weight: 600; color: var(--el-color-primary); max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.retrieve-score-wrap {
  display: flex; align-items: center; gap: 6px; min-width: 180px;
}
.retrieve-score-label { font-size: 12px; color: var(--el-text-color-secondary); white-space: nowrap; }
.retrieve-score-bar {
  flex: 1; height: 6px; background: var(--el-border-color-lighter); border-radius: 3px; overflow: hidden;
}
.retrieve-score-fill {
  height: 100%; background: linear-gradient(90deg, var(--el-color-primary), #67c23a);
  border-radius: 3px; transition: width 0.3s;
}
.retrieve-score-value { font-size: 12px; color: var(--el-text-color-regular); min-width: 42px; text-align: right; }
.retrieve-item-excerpt {
  font-size: 13px; color: var(--el-text-color-regular); line-height: 1.6;
  cursor: pointer; user-select: none;
  max-height: 80px; overflow: hidden;
  transition: max-height 0.3s;
  display: flex; align-items: flex-end; gap: 4px;
  span { flex: 1; }
  // Day 43: 高亮标签样式
  :deep(mark) {
    background: #fff3bf; color: #d48806; border-radius: 2px;
    padding: 0 2px; font-weight: 600;
  }
}
.excerpt-expanded {
  font-size: 13px; color: var(--el-text-color-regular); line-height: 1.8;
  max-height: none; overflow: visible;
  word-break: break-all;
  :deep(mark) {
    background: #fff3bf; color: #d48806; border-radius: 2px;
    padding: 0 2px; font-weight: 600;
  }
}

// ========== 上传向导 ==========
.wizard-step { min-height: 200px; }

.upload-drag {
  :deep(.el-upload-dragger) {
    padding: 30px 20px; border-radius: 8px;
    background: #f5f8ff; border: 2px dashed #c0d0e0;
    &:hover { border-color: #409eff; background: #eef4ff; }
  }
}
.upload-icon { font-size: 40px; color: #409eff; margin-bottom: 10px; }
.upload-text { color: #606266; font-size: 14px; em { color: #409eff; font-style: normal; } }
.upload-tip { color: #909399; font-size: 12px; margin-top: 8px; text-align: center; b { color: #409eff; } }

.format-tips {
  margin-top: 20px; padding: 12px 16px; background: #f0f7ff; border-radius: 6px;
  border: 1px solid #d9ecff;
}
.format-tips-title { font-size: 13px; font-weight: 600; color: #409eff; margin-bottom: 8px; display: flex; align-items: center; gap: 4px; }
.format-tips ul { margin: 0; padding-left: 20px; li { font-size: 12px; color: #606266; line-height: 1.8; b { color: #303133; } } }

.file-list-review {
  border: 1px solid #ebeef5; border-radius: 6px; padding: 12px;
  max-height: 200px; overflow-y: auto;
}
.file-list-title { font-size: 13px; font-weight: 600; color: #303133; margin-bottom: 8px; }
.file-item {
  display: flex; align-items: center; gap: 8px; padding: 6px 0;
  border-bottom: 1px solid #f5f5f5;
  &:last-child { border-bottom: none; }
}
.file-item-name { flex: 1; font-size: 13px; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-item-size { font-size: 12px; color: #909399; white-space: nowrap; }

.upload-progress-list { display: flex; flex-direction: column; gap: 14px; max-height: 350px; overflow-y: auto; }
.upload-progress-item { }
.upload-progress-info {
  display: flex; align-items: center; gap: 8px; margin-bottom: 6px;
}
.upload-progress-name { flex: 1; font-size: 13px; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.upload-pct { font-size: 12px; color: #606266; min-width: 36px; }
.upload-error-msg { font-size: 12px; color: #f56c6c; margin-top: 4px; }

.done-panel { text-align: center; padding: 20px 0; }
.done-icon { font-size: 56px; margin-bottom: 12px; display: block; }
.done-panel h3 { margin: 0 0 10px; font-size: 18px; color: #303133; }
.done-panel p { margin: 0; color: #606266; font-size: 14px; line-height: 1.6; }

.wizard-footer {
  display: flex; justify-content: flex-end; gap: 10px; margin-top: 24px;
  border-top: 1px solid #ebeef5; padding-top: 16px;
}

// ========== Day 49: 文档预览样式 ==========
.doc-preview-body {
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 13px;
  color: var(--el-text-color-regular);
  border: 1px solid var(--el-border-color-light);
  padding: 16px;
  border-radius: 6px;
  background: var(--el-fill-color-lightest);
  max-height: 55vh;
  overflow-y: auto;
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', monospace;
  // 深色模式适配
  @media (prefers-color-scheme: dark) {
    background: #1e1e1e;
    color: #d4d4d4;
    border-color: #3a3a3a;
  }
}

// Day 50: 纯文本容器
.doc-preview-plain {
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 13px;
  line-height: 1.8;
}

// Day 50: Markdown 渲染样式
.md-rendered {
  font-size: 14px;
  line-height: 1.8;
  :deep(h1), :deep(h2), :deep(h3), :deep(h4) { color: var(--el-text-color-primary); margin: 1em 0 0.5em; font-weight: 600; }
  :deep(h1) { font-size: 20px; border-bottom: 1px solid var(--el-border-color); padding-bottom: 6px; }
  :deep(h2) { font-size: 17px; }
  :deep(h3) { font-size: 15px; }
  :deep(code) { background: var(--el-fill-color); padding: 2px 6px; border-radius: 3px; font-size: 13px; font-family: monospace; }
  :deep(pre) { background: var(--el-fill-color); padding: 12px; border-radius: 6px; overflow-x: auto; }
  :deep(pre code) { background: none; padding: 0; }
  :deep(blockquote) { border-left: 3px solid var(--el-color-primary); margin: 8px 0; padding: 4px 12px; color: var(--el-text-color-secondary); background: var(--el-fill-color-lightest); }
  :deep(table) { border-collapse: collapse; width: 100%; }
  :deep(th), :deep(td) { border: 1px solid var(--el-border-color); padding: 6px 12px; }
  :deep(th) { background: var(--el-fill-color-light); font-weight: 600; }
  :deep(a) { color: var(--el-color-primary); }
  :deep(ul), :deep(ol) { padding-left: 20px; }
  :deep(li) { margin: 4px 0; }
}

// Day 50: DOCX mammoth.js 渲染样式
.docx-rendered {
  font-size: 14px;
  line-height: 1.8;
  :deep(table) { border-collapse: collapse; width: 100%; margin: 8px 0; }
  :deep(th), :deep(td) { border: 1px solid var(--el-border-color); padding: 6px 12px; }
  :deep(th) { background: var(--el-fill-color-light); font-weight: 600; }
  :deep(h1), :deep(h2), :deep(h3) { color: var(--el-text-color-primary); margin: 0.8em 0 0.4em; font-weight: 600; }
  :deep(p) { margin: 6px 0; }
  :deep(ul), :deep(ol) { padding-left: 20px; }
}
</style>
