<!-- @file agent/Index.vue - Agent 编排页面 V6.9 -->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>🤖 Agent 编排</h2>
      <div style="display:flex;gap:8px">
        <el-button size="small" @click="loadHistory">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>新建任务
        </el-button>
      </div>
    </div>

    <!-- Tab 导航 (V6.9) -->
    <el-tabs v-model="activeTab" class="agent-tabs" style="margin-bottom:0">
      <el-tab-pane label="🗂 任务编排" name="tasks">
        <span slot="label"><el-icon><List /></el-icon> 任务编排</span>
      </el-tab-pane>
      <el-tab-pane label="🎨 Agent 画布" name="canvas">
        <span slot="label"><el-icon><Brush /></el-icon> 画布</span>
        <!-- 内嵌摘要：最近 5 个工作流 -->
        <div class="tab-summary">
          <div class="tab-summary-header">
            <span class="tab-summary-title">🎨 Agent 画布 · 最近工作流</span>
            <el-button type="primary" size="small" @click="openCanvas">
              打开完整页面
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
          <el-table :data="canvasSummary" v-loading="canvasSummaryLoading" stripe size="small" empty-text="暂无工作流">
            <el-table-column prop="name" label="工作流名称" min-width="200" show-overflow-tooltip />
            <el-table-column label="节点数" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ (row.nodes || []).length }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="连线数" width="80" align="center">
              <template #default="{ row }">{{ (row.edges || []).length }}</template>
            </el-table-column>
            <el-table-column label="更新时间" width="180">
              <template #default="{ row }">{{ row.updatedAt || row.createdAt || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <el-button size="small" type="primary" link @click="openCanvas">在画布打开</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
      <el-tab-pane label="🔀 多智能体" name="multi">
        <span slot="label"><el-icon><Connection /></el-icon> 多智能体</span>
        <!-- 内嵌摘要：最近 5 个多智能体执行 -->
        <div class="tab-summary">
          <div class="tab-summary-header">
            <span class="tab-summary-title">🔀 多智能体 · 最近协作</span>
            <el-button type="primary" size="small" @click="openMulti">
              打开完整页面
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
          <el-table :data="multiSummary" v-loading="multiSummaryLoading" stripe size="small" empty-text="暂无执行记录">
            <el-table-column label="任务目标" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                <div style="font-weight:500">{{ row.goal || row.name || '-' }}</div>
                <div style="font-size:11px;color: var(--el-text-color-secondary)">ID: {{ row.id || row.taskId }}</div>
              </template>
            </el-table-column>
            <el-table-column label="轮次" width="80" align="center">
              <template #default="{ row }">{{ row.rounds ?? row.round ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="耗时" width="100" align="center">
              <template #default="{ row }">{{ row.totalDurationMs ? (row.totalDurationMs/1000).toFixed(1) + 's' : (row.duration || '-') }}</template>
            </el-table-column>
            <el-table-column label="Critic" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.criticPassed ? 'success' : 'danger'">
                  {{ row.criticPassed ? '✅ 通过' : '⚠️ 未通过' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="时间" width="160">
              <template #default="{ row }">{{ row.time || row.createdAt || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default>
                <el-button size="small" type="primary" link @click="openMulti">查看详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
      <el-tab-pane label="📈 训练可视化" name="training">
        <span slot="label"><el-icon><TrendCharts /></el-icon> 训练</span>
        <!-- 内嵌摘要：最近 5 个训练任务 -->
        <div class="tab-summary">
          <div class="tab-summary-header">
            <span class="tab-summary-title">📈 训练 · 最近任务</span>
            <el-button type="primary" size="small" @click="openTraining">
              打开完整页面
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
          <el-table :data="trainingSummary" v-loading="trainingSummaryLoading" stripe size="small" empty-text="暂无训练任务">
            <el-table-column prop="taskId" label="任务ID" width="120" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'completed' ? 'success' : row.status === 'failed' ? 'danger' : 'primary'">
                  {{ {running:'进行中',completed:'已完成',failed:'失败'}[row.status] || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Epoch" width="80" align="center">
              <template #default="{ row }">{{ row.epochs ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="样本数" width="100" align="right">
              <template #default="{ row }">{{ row.totalSamples ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="意图数" width="100" align="right">
              <template #default="{ row }">{{ row.totalIntents ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="开始时间" width="180">
              <template #default="{ row }">{{ row.startedAt || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default>
                <el-button size="small" type="primary" link @click="openTraining">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
      <el-tab-pane label="✅ Skill 审批" name="approval">
        <span slot="label"><el-icon><CircleCheck /></el-icon> 审批</span>
        <!-- 内嵌摘要：最近 5 个审批请求 -->
        <div class="tab-summary">
          <div class="tab-summary-header">
            <span class="tab-summary-title">✅ Skill 审批 · 最近请求</span>
            <el-button type="primary" size="small" @click="openApproval">
              打开完整页面
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
          <el-table :data="approvalSummary" v-loading="approvalSummaryLoading" stripe size="small" empty-text="暂无审批请求">
            <el-table-column prop="username" label="申请人" width="120" />
            <el-table-column prop="toolName" label="工具" width="180" show-overflow-tooltip />
            <el-table-column label="风险" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.riskLevel === 'CRITICAL' ? 'danger' : row.riskLevel === 'HIGH' ? 'warning' : 'info'">
                  {{ row.riskLevel }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'APPROVED' ? 'success' : row.status === 'REJECTED' ? 'danger' : 'warning'">
                  {{ {APPROVED:'已通过',REJECTED:'已拒绝',PENDING:'待审批'}[row.status] || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="执行目的" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">{{ row.goal || '-' }}</template>
            </el-table-column>
            <el-table-column label="时间" width="180">
              <template #default="{ row }">{{ row.createdAt || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default>
                <el-button size="small" type="primary" link @click="openApproval">前往审批</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 主内容：任务编排 -->
    <div v-if="activeTab === 'tasks'">

    <!-- 统计卡片 -->
    <el-row :gutter="12" style="margin-bottom:16px">
      <el-col :span="6">
        <el-tooltip :content="getStatTip('total')" placement="top" effect="light">
          <el-card shadow="hover" body-style="text-align:center;padding:12px;cursor:help">
            <div style="font-size:24px;font-weight:700;color: var(--el-color-primary)">{{ stats.total }}</div>
            <div style="font-size:12px;color: var(--el-text-color-secondary);margin-top:4px">总任务数</div>
          </el-card>
        </el-tooltip>
      </el-col>
      <el-col :span="6">
        <el-tooltip :content="getStatTip('success')" placement="top" effect="light">
          <el-card shadow="hover" body-style="text-align:center;padding:12px;cursor:help">
            <div style="font-size:24px;font-weight:700;color: var(--el-color-success)">{{ stats.success }}</div>
            <div style="font-size:12px;color: var(--el-text-color-secondary);margin-top:4px">成功</div>
          </el-card>
        </el-tooltip>
      </el-col>
      <el-col :span="6">
        <el-tooltip :content="getStatTip('failed')" placement="top" effect="light">
          <el-card shadow="hover" body-style="text-align:center;padding:12px;cursor:help">
            <div style="font-size:24px;font-weight:700;color: var(--el-color-danger)">{{ stats.failed }}</div>
            <div style="font-size:12px;color: var(--el-text-color-secondary);margin-top:4px">失败</div>
          </el-card>
        </el-tooltip>
      </el-col>
      <el-col :span="6">
        <el-tooltip :content="getStatTip('running')" placement="top" effect="light">
          <el-card shadow="hover" body-style="text-align:center;padding:12px;cursor:help">
            <div style="font-size:24px;font-weight:700;color: var(--el-color-warning)">{{ stats.running }}</div>
            <div style="font-size:12px;color: var(--el-text-color-secondary);margin-top:4px">运行中</div>
          </el-card>
        </el-tooltip>
      </el-col>
    </el-row>

    <!-- ===== 🤖 AI 一句话生成智能体群 ===== -->
    <el-card class="ai-generate-card" body-style="padding:0" style="margin-bottom:16px">
      <template #header>
        <div style="display:flex;align-items:center;justify-content:space-between">
          <span>🧬 AI 一句话生成智能体群</span>
          <el-tag size="small" type="success">Beta</el-tag>
        </div>
      </template>
      <div style="padding:16px">
        <el-row :gutter="16">
          <!-- 左侧：输入区 -->
          <el-col :span="14">
            <div style="margin-bottom:12px">
              <div style="display:flex;align-items:center;gap:8px">
                <span style="font-size:13px;font-weight:600;margin-bottom:8px;color: var(--el-text-color-primary)">
                  描述你想要的智能体群：
                </span>
                <el-tooltip content="详细描述你的需求，AI 会自动设计 Agent 角色、工作流和工具配置" placement="right" effect="light">
                  <el-icon style="cursor:help;color: var(--el-text-color-secondary);margin-bottom:8px"><QuestionFilled /></el-icon>
                </el-tooltip>
              </div>
              <el-input
                v-model="genDesc"
                type="textarea"
                :rows="4"
                placeholder="例如：我想创建一个可以帮我分析股票数据、生成报告并发送邮件通知的智能体群，包含数据采集、分析、报告生成三个子Agent"
              />
            </div>

            <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap;margin-bottom:12px">
              <span style="font-size:12px;color: var(--el-text-color-secondary)">生成模式：</span>
              <el-radio-group v-model="genMode" size="small">
                <el-radio value="auto">🚀 自动编排</el-radio>
                <el-radio value="expert">🎯 专家模式</el-radio>
              </el-radio-group>
              <el-select v-model="genModel" size="small" style="width:140px;margin-left:auto" placeholder="生成模型">
                <el-option label="MiniMax-01" value="minimax-01" />
                <el-option label="GPT-4o" value="gpt-4o" />
                <el-option label="DeepSeek" value="deepseek-chat" />
              </el-select>
            </div>

            <!-- 快捷生成示例 -->
            <div style="margin-bottom:12px">
              <div style="font-size:12px;color: var(--el-text-color-secondary);margin-bottom:6px">💡 快速示例：</div>
              <div style="display:flex;flex-wrap:wrap;gap:6px">
                <el-tag
                  v-for="eg in examples" :key="eg.label"
                  size="small" style="cursor:pointer" @click="genDesc = eg.desc"
                >{{ eg.label }}</el-tag>
              </div>
            </div>

            <div style="display:flex;gap:8px">
              <el-button
                type="primary"
                size="large"
                :loading="generating"
                :disabled="!genDesc.trim()"
                @click="generateAgentGroup"
                style="flex:1"
              >
                <el-icon v-if="!generating"><MagicStick /></el-icon>
                {{ generating ? '生成中…' : '🪄 生成智能体群' }}
              </el-button>
              <el-button size="large" @click="genDesc = ''; generatedGroup = null" :disabled="generating">
                清空
              </el-button>
            </div>
          </el-col>

          <!-- 右侧：生成结果预览 -->
          <el-col :span="10">
            <div v-if="generating" style="text-align:center;padding:40px 0">
              <el-icon class="generating-icon" :size="40"><Loading /></el-icon>
              <div style="margin-top:12px;color: var(--el-text-color-secondary);font-size:13px">正在根据描述生成智能体群配置…</div>
              <div style="margin-top:6px;color: var(--el-text-color-placeholder);font-size:12px">分析任务 → 设计 Agent → 配置工具 → 构建工作流</div>
            </div>

            <div v-else-if="generatedGroup" class="generated-preview">
              <div style="font-weight:600;font-size:13px;margin-bottom:10px;color: var(--el-text-color-primary)">
                ✅ 已生成：{{ generatedGroup.name }}
              </div>

              <!-- 智能体列表 -->
              <div v-for="(agent, i) in generatedGroup.agents" :key="i" class="agent-chip">
                <el-icon><UserFilled /></el-icon>
                <span style="font-weight:600">{{ agent.name }}</span>
                <el-tag size="small" type="info">{{ agent.role }}</el-tag>
              </div>

              <!-- 工具列表 -->
              <div v-if="generatedGroup.tools?.length" style="margin-top:8px">
                <div style="font-size:12px;color: var(--el-text-color-secondary);margin-bottom:4px">🧰 工具集：{{ generatedGroup.tools.join(', ') }}</div>
              </div>

              <!-- 工作流 -->
              <div v-if="generatedGroup.workflow?.length" style="margin-top:8px">
                <div style="font-size:12px;color: var(--el-text-color-secondary);margin-bottom:4px">🔀 工作流：</div>
                <div style="display:flex;align-items:center;gap:4px;flex-wrap:wrap">
                  <span v-for="(step, i) in generatedGroup.workflow" :key="i" style="font-size:11px">
                    <span style="background: var(--el-color-primary-light-9);padding:2px 6px;border-radius:4px;color: var(--el-color-primary)">{{ step }}</span>
                    <span v-if="i < generatedGroup.workflow.length - 1" style="color: var(--el-text-color-placeholder);margin:0 2px">→</span>
                  </span>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div style="margin-top:12px;display:flex;gap:6px;flex-wrap:wrap">
                <el-button size="small" type="primary" @click="testGenerated">
                  <el-icon><VideoPlay /></el-icon>测试
                </el-button>
                <el-button size="small" @click="exportGenerated">
                  <el-icon><Download /></el-icon>导出
                </el-button>
                <!-- P1-3: 复制配置按钮 -->
                <el-button size="small" @click="copyConfig">
                  <el-icon><CopyDocument /></el-icon>复制配置
                </el-button>
                <el-button size="small" type="success" @click="runGenerated">
                  <el-icon><CaretRight /></el-icon>运行
                </el-button>
                <!-- P1-3: 查看详情按钮 -->
                <el-button size="small" link @click="showDetail = !showDetail">
                  {{ showDetail ? '收起详情' : '查看详情' }}
                </el-button>
                <el-button size="small" link type="warning" @click="regenerate">
                  重新生成
                </el-button>
              </div>
              <!-- P1-3: 详情展开区 -->
              <div v-if="showDetail" style="margin-top:10px;background: var(--el-fill-color-light);border-radius:6px;padding:8px">
                <div style="font-size:11px;color: var(--el-text-color-secondary);margin-bottom:4px">完整配置 JSON：</div>
                <pre style="font-size:11px;margin:0;max-height:200px;overflow:auto">{{ JSON.stringify(generatedGroup, null, 2) }}</pre>
              </div>
            </div>

            <div v-else style="text-align:center;padding:40px 0;color: var(--el-text-color-placeholder);font-size:13px">
              <el-icon :size="40"><ChatDotSquare /></el-icon>
              <div style="margin-top:8px">输入描述后点击「生成智能体群」</div>
              <div style="margin-top:4px;font-size:11px">AI 将自动设计 Agent 配置和工具集</div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 快捷模板 -->
    <div class="template-grid">
      <el-tooltip
        v-for="tpl in templates" :key="tpl.type"
        :content="`${tpl.name}: ${tpl.desc} | 难度: ${tpl.difficulty === 'hard' ? '高' : tpl.difficulty === 'medium' ? '中' : '低'}`"
        placement="top" effect="light"
      >
        <el-card shadow="hover" class="tpl-card" @click="runTemplate(tpl)">
          <div style="text-align:center">
            <div style="font-size:32px;margin-bottom:8px">{{ tpl.icon }}</div>
            <div style="font-weight:600;font-size:13px;margin-bottom:4px">{{ tpl.name }}</div>
            <div style="font-size:11px;color: var(--el-text-color-secondary);margin-bottom:8px">{{ tpl.desc }}</div>
            <el-tag size="small" :type="tpl.difficulty === 'hard' ? 'danger' : tpl.difficulty === 'medium' ? 'warning' : 'success'">
              {{ tpl.difficulty === 'hard' ? '复杂' : tpl.difficulty === 'medium' ? '中等' : '简单' }}
            </el-tag>
          </div>
        </el-card>
      </el-tooltip>
    </div>

    <!-- 任务历史 -->
    <el-divider content-position="left">执行历史 ({{ historyList.length }})</el-divider>
    <el-table :data="historyList" v-loading="historyLoading" stripe>
      <el-empty v-if="!historyLoading && !historyList.length" description="暂无任务记录">
        <el-button type="primary" @click="openCreate">创建第一个 Agent 任务</el-button>
      </el-empty>
      <el-table-column type="index" width="50" label="#" />
      <el-table-column label="任务描述" min-width="200">
        <template #default="{ row }">
          <div style="font-weight:500">{{ row.description || row.name || row.goal || row.task || '任务 #' + row.id }}</div>
          <div style="font-size:11px;color: var(--el-text-color-secondary)">ID: {{ row.id }}</div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag
            :type="row.status === 'success' || row.status === 'SUCCESS' || row.status === 'completed' ? 'success'
                  : row.status === 'failed' || row.status === 'FAILED' || row.status === 'error' ? 'danger'
                  : row.status === 'running' || row.status === 'RUNNING' || row.status === 'pending' ? 'warning' : 'info'"
            size="small"
          >
            {{ {success:'成功',failed:'失败',running:'运行中',pending:'等待中',completed:'完成',error:'错误'}[(row.status || '').toLowerCase()] || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Agent" width="110">
        <template #default="{ row }">
          <el-tooltip :content="`Agent 类型: ${row.agentType || 'general'}`" placement="top" effect="light">
            <el-tag size="small" type="info" style="cursor:help">{{ row.agentType || 'general' }}</el-tag>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="步数" width="70" align="center">
        <template #default="{ row }">{{ row.steps?.length ?? row.stepCount ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="耗时" width="90" align="center">
        <template #default="{ row }">{{ row.duration ? row.duration + 'ms' : '-' }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="160">
        <template #default="{ row }">{{ row.createdAt || row.created_at || row.createTime || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="viewHistory(row)">查看</el-button>
          <el-button size="small" link type="danger" @click="deleteHistory(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建任务弹窗 -->
    <el-dialog v-model="createVisible" :title="'新建 Agent 任务'" width="600px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="任务描述" required>
          <el-input v-model="form.prompt" type="textarea" :rows="4"
            placeholder="描述你想让 Agent 完成的任务，越详细效果越好…" />
        </el-form-item>
        <el-form-item label="Agent 类型">
          <el-select v-model="form.agentType" style="width:100%">
            <el-option label="🤖 通用助手" value="general" />
            <el-option label="💻 代码助手" value="code" />
            <el-option label="📊 数据分析" value="analytics" />
            <el-option label="📚 RAG 问答" value="rag" />
            <el-option label="🧠 多步推理" value="reasoning" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="最大步数">
              <el-input-number v-model="form.maxSteps" :min="1" :max="30" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="温度参数">
              <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" show-input />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="工具集">
          <el-checkbox-group v-model="form.tools">
            <el-checkbox label="web-search">🌐 网页搜索</el-checkbox>
            <el-checkbox label="code-exec">💻 代码执行</el-checkbox>
            <el-checkbox label="file-read">📄 文件读取</el-checkbox>
            <el-checkbox label="file-write">📝 文件写入</el-checkbox>
            <el-checkbox label="calculator">🧮 计算器</el-checkbox>
            <el-checkbox label="api-call">🔗 API 调用</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="使用模型">
          <el-select v-model="form.model" style="width:100%" clearable placeholder="默认模型">
            <el-option label="GPT-4o" value="gpt-4o" />
            <el-option label="GPT-4o-mini" value="gpt-4o-mini" />
            <el-option label="DeepSeek Chat" value="deepseek-chat" />
            <el-option label="ChatGLM" value="chatglm" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitTask">
          <el-icon><VideoPlay /></el-icon>执行任务
        </el-button>
      </template>
    </el-dialog>

    <!-- 执行结果抽屉 -->
    <el-drawer v-model="resultVisible" title="任务执行详情" size="65%" destroy-on-close>
      <template #header>
        <span>执行详情</span>
        <el-tag size="small" :type="statusTag(detailResult?.status)" style="margin-left:8px">
          {{ detailResult?.status }}
        </el-tag>
      </template>
      <div v-if="detailResult" class="result-detail">
        <el-descriptions :column="2" border size="small" style="margin-bottom:16px">
          <el-descriptions-item label="任务 ID">{{ detailResult.id }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ detailResult.duration }}ms</el-descriptions-item>
          <el-descriptions-item label="Agent 类型">{{ detailResult.agentType }}</el-descriptions-item>
          <el-descriptions-item label="步数">{{ detailResult.steps?.length || 0 }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="detailResult.goal" style="margin-bottom:16px">
          <div style="font-weight:600;font-size:13px;margin-bottom:6px">🎯 任务目标</div>
          <el-card size="small">{{ detailResult.goal }}</el-card>
        </div>
        <div v-if="detailResult.result" style="margin-bottom:16px">
          <div style="font-weight:600;font-size:13px;margin-bottom:6px">✅ 执行结果</div>
          <el-card size="small" style="background: var(--el-color-success-light-9)">
            <pre style="white-space:pre-wrap;word-break:break-word;font-size:13px">{{ detailResult.result }}</pre>
          </el-card>
        </div>
        <div v-if="detailResult.steps?.length">
          <div style="font-weight:600;font-size:13px;margin-bottom:8px">📋 执行步骤 ({{ detailResult.steps.length }})</div>
          <el-timeline>
            <el-timeline-item
              v-for="(step, i) in detailResult.steps" :key="i"
              :color="step.status === 'error' ? '#f56c6c' : '#67c23a'"
              :hollow="step.status === 'running'"
            >
              <div class="step-item">
                <div class="step-header">
                  <span class="step-tool">{{ step.tool || step.node || '步骤 ' + (i+1) }}</span>
                  <el-tag size="small" :type="step.status === 'error' ? 'danger' : 'success'" style="margin-left:8px">
                    {{ step.status }}
                  </el-tag>
                </div>
                <div v-if="step.thought" class="step-thought">💭 {{ step.thought }}</div>
                <div v-if="step.result" class="step-result">
                  <pre>{{ typeof step.result === 'string' ? step.result : JSON.stringify(step.result, null, 2) }}</pre>
                </div>
                <div v-if="step.error" class="step-error">❌ {{ step.error }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </el-drawer>

    <!-- 测试结果抽屉 -->
    <el-drawer v-model="testVisible" title="🧪 测试生成结果" size="65%" destroy-on-close>
      <div style="padding:0 16px">
        <div style="margin-bottom:12px">
          <el-tag type="success">测试环境 · 沙箱模式</el-tag>
          <span style="margin-left:8px;font-size:12px;color: var(--el-text-color-secondary)">测试输入将在沙箱中执行，不会影响生产环境</span>
        </div>
        <el-input v-model="testInput" type="textarea" :rows="3" placeholder="输入测试内容…" style="margin-bottom:12px" />
        <el-button type="primary" :loading="testing" @click="runTest" style="margin-bottom:16px">
          <el-icon><VideoPlay /></el-icon>执行测试
        </el-button>

        <div v-if="testResult" style="margin-bottom:16px">
          <div style="font-weight:600;font-size:13px;margin-bottom:8px">📤 测试输出</div>
          <el-card body-style="padding:12px" style="background: var(--el-color-success-light-9)">
            <pre style="white-space:pre-wrap;font-size:13px">{{ testResult }}</pre>
          </el-card>
        </div>

        <div v-if="testSteps.length">
          <div style="font-weight:600;font-size:13px;margin-bottom:8px">📋 执行步骤</div>
          <el-timeline>
            <el-timeline-item v-for="(s, i) in testSteps" :key="i" :color="s.ok ? '#67c23a' : '#f56c6c'">
              <div style="font-size:13px;font-weight:600">{{ s.name }}</div>
              <div v-if="s.output" style="font-size:12px;color: var(--el-color-success);margin-top:4px">{{ s.output }}</div>
              <div v-if="s.error" style="font-size:12px;color: var(--el-color-danger);margin-top:4px">{{ s.error }}</div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
      <template #footer>
        <div style="display:flex;gap:8px;justify-content:flex-end">
          <el-button @click="testVisible = false">关闭</el-button>
          <el-button v-if="testResult" type="success" @click="exportGenerated">
            <el-icon><Download /></el-icon>导出模型文件
          </el-button>
          <el-button v-if="testResult" type="primary" @click="runGenerated">
            <el-icon><CaretRight /></el-icon>正式运行
          </el-button>
        </div>
      </template>
    </el-drawer>
    </div><!-- end activeTab === 'tasks' -->
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ElMessageBox } from 'element-plus'
import { agentApi } from '@/api/agent'
import { autoAgentGroupGenerate } from '@/api/ai'
import http from '@/api/http'
import { useClipboard } from '@/composables/useClipboard'
import {
  Plus, Refresh, VideoPlay, MagicStick, Download, CaretRight,
  ChatDotSquare, Loading, UserFilled, QuestionFilled, CopyDocument,
  List, Brush, Connection, TrendCharts, CircleCheck, ArrowRight,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// clipboard composable (textarea 降级支持)
const { copy: copyToClipboard } = useClipboard({ successMsg: '配置已复制', failMsg: '复制失败' })

// V6.9 Tab 导航 (支持旧路由 redirect)
const activeTab = ref(route.query.tab || 'tasks')

function openCanvas() { router.push('/agent/canvas') }
function openMulti() { router.push('/agent/multi') }
function openTraining() { router.push('/agent/training') }
function openApproval() { router.push('/agent/approval') }

// ===== Tab 摘要数据 (子页面真实数据预览) =====
const canvasSummary = ref([])
const canvasSummaryLoading = ref(false)
const multiSummary = ref([])
const multiSummaryLoading = ref(false)
const trainingSummary = ref([])
const trainingSummaryLoading = ref(false)
const approvalSummary = ref([])
const approvalSummaryLoading = ref(false)

async function loadCanvasSummary() {
  canvasSummaryLoading.value = true
  try {
    const r = await agentApi.list({ limit: 5 })
    canvasSummary.value = r.data?.list || r.data || []
  } catch { canvasSummary.value = [] }
  finally { canvasSummaryLoading.value = false }
}

async function loadMultiSummary() {
  multiSummaryLoading.value = true
  try {
    // 多智能体执行历史（与 Multi.vue 同一接口）
    const r = await http.get('/agent/multi/history', { params: { limit: 5 } }).catch(() => null)
    if (r?.data) {
      const data = r.data
      multiSummary.value = Array.isArray(data) ? data : (data.list || data)
    } else {
      multiSummary.value = []
    }
  } catch { multiSummary.value = [] }
  finally { multiSummaryLoading.value = false }
}

async function loadTrainingSummary() {
  trainingSummaryLoading.value = true
  try {
    const r = await http.get('/ai/training/llm/list')
    const list = r.data || []
    trainingSummary.value = list.slice(0, 5)
  } catch { trainingSummary.value = [] }
  finally { trainingSummaryLoading.value = false }
}

async function loadApprovalSummary() {
  approvalSummaryLoading.value = true
  try {
    // 同时拉取待审批和历史，合并去重
    const [pendingR, historyR] = await Promise.all([
      http.get('/skill-approval/pending/all').catch(() => ({ data: [] })),
      http.get('/skill-approval/history', { params: { page: 1, size: 5 } }).catch(() => ({ data: [] })),
    ])
    const merged = [...(pendingR.data || []), ...(historyR.data || [])]
    // 按 createdAt 倒序
    merged.sort((a, b) => String(b.createdAt || '').localeCompare(String(a.createdAt || '')))
    approvalSummary.value = merged.slice(0, 5)
  } catch { approvalSummary.value = [] }
  finally { approvalSummaryLoading.value = false }
}

const templates = [
  { type: 'code', name: '代码审查', icon: '🔍', desc: '分析代码质量，查找 Bug 和优化点', agentType: 'code', difficulty: 'medium', prompt: '请帮我审查以下代码的质量，查找潜在的 Bug、安全问题和性能优化点。' },
  { type: 'data', name: '数据分析', icon: '📊', desc: '从文本或表格数据生成摘要报告', agentType: 'analytics', difficulty: 'medium', prompt: '请分析以下数据，生成摘要报告，包括关键发现和趋势分析。' },
  { type: 'rag', name: 'RAG 问答', icon: '📚', desc: '基于知识库回答问题', agentType: 'rag', difficulty: 'easy', prompt: '请基于提供的知识库内容回答用户的问题。' },
  { type: 'reasoning', name: '多步推理', icon: '🧠', desc: '复杂问题的链式推理分析', agentType: 'reasoning', difficulty: 'hard', prompt: '请逐步分析以下问题，展示推理过程，最后给出结论。' },
  { type: 'web', name: '网页搜索', icon: '🌐', desc: '搜索网络获取最新信息', agentType: 'general', difficulty: 'easy', tools: ['web-search'], prompt: '请搜索最新的相关信息并给出回答。' },
  { type: 'multi', name: '批量任务', icon: '⚡', desc: '并行处理多个子任务', agentType: 'general', difficulty: 'hard', prompt: '请将以下任务拆分为子任务并行处理：' },
]

// ===== 统计卡片 helper =====
function getStatTip(key) {
  const tips = {
    total: '平台累计创建的 Agent 任务总数',
    success: '执行结果为成功/完成的 Agent 任务数',
    failed: '执行失败或异常的 Agent 任务数（需排查）',
    running: '当前正在执行中的 Agent 任务数',
  }
  return tips[key] || ''
}

// ===== AI 生成状态 =====
const genDesc = ref('')
const genMode = ref('auto')
const genModel = ref('minimax-01')
const generating = ref(false)
const generatedGroup = ref(null)
const testVisible = ref(false)
const testInput = ref('')
const testing = ref(false)
const testResult = ref('')
const testSteps = ref([])
const showDetail = ref(false) // P1-3: 查看详情开关

const examples = [
  { label: '📊 数据分析+报告', desc: '创建一个数据分析Agent群，包含数据采集、数据清洗、报告生成三个子Agent，支持Excel和CSV导入' },
  { label: '📧 邮件助手', desc: '智能邮件处理Agent群，能自动分类邮件、生成回复草稿、定时发送，支持附件处理' },
  { label: '🔍 代码审查', desc: '代码审查Agent群，自动分析PR代码、检测安全漏洞、生成审查意见' },
  { label: '💬 客服机器人', desc: '多轮对话客服Agent，理解用户意图、查询知识库、生成回复、记录工单' },
  { label: '📝 文档处理', desc: '文档处理Agent群，支持PDF解析、摘要生成、翻译、关键信息提取' },
]

// 从描述生成智能体群（调用真实 AI 接口）
async function generateAgentGroup() {
  if (!genDesc.value.trim()) { ElMessage.warning('请输入描述'); return }
  generating.value = true
  generatedGroup.value = null

  try {
    // V6.8+: 调用真实后端接口自动生成 Agent Group
    const r = await autoAgentGroupGenerate({
      description: genDesc.value,
      mode: genMode.value,       // 'auto' | 'expert'
      model: genModel.value,
    })

    const config = r.data?.agents || r.data?.group || r.data
    if (config?.agents) {
      generatedGroup.value = config
      ElMessage.success(`生成完成！包含 ${config.agents.length} 个 Agent，${config.tools?.length || 0} 个工具`)
    } else {
      // 后端未返回有效结构，降级到模板
      throw new Error('invalid response')
    }
  } catch (e) {
    // 接口不可用时降级到模板生成（原有兜底逻辑）
    const config = buildFromTemplate(genDesc.value)
    generatedGroup.value = config
    ElMessage.warning('使用模板生成（后端接口暂不可用）')
  } finally {
    generating.value = false
  }
}

// 模板匹配生成（接口不可用时的本地兜底）
function buildFromTemplate(desc) {
  const d = desc.toLowerCase()
  const name = '智能体群-' + Date.now()

  // 数据分析场景
  if (d.includes('分析') || d.includes('数据') || d.includes('报表') || d.includes('chart') || d.includes('chart')) {
    return {
      name: '📊 数据分析智能体群',
      description: '包含数据采集、数据清洗、报告生成三个子 Agent',
      agents: [
        { name: '数据采集Agent', role: '数据采集', prompt: '从各种数据源采集数据，支持CSV、Excel、数据库、API', tools: ['file-read', 'api-call'], model: 'minimax-01' },
        { name: '数据清洗Agent', role: '数据处理', prompt: '对采集的数据进行清洗、格式化、去重和校验', tools: ['code-exec'], model: 'minimax-01' },
        { name: '报告生成Agent', role: '报告生成', prompt: '基于清洗后的数据生成分析报告，支持图表和文字总结', tools: ['file-write'], model: 'minimax-01' },
      ],
      tools: ['file-read', 'file-write', 'api-call', 'code-exec'],
      workflow: ['数据采集Agent', '数据清洗Agent', '报告生成Agent'],
    }
  }

  // 邮件场景
  if (d.includes('邮件') || d.includes('email')) {
    return {
      name: '📧 邮件处理智能体群',
      description: '智能邮件处理，支持分类、回复、发送',
      agents: [
        { name: '邮件分类Agent', role: '邮件分类', prompt: '分析邮件内容，判断邮件类型（重要/普通/垃圾）和意图', tools: ['api-call'], model: 'minimax-01' },
        { name: '回复生成Agent', role: '回复生成', prompt: '根据邮件内容和上下文，生成专业得体的回复草稿', tools: [], model: 'minimax-01' },
        { name: '邮件发送Agent', role: '邮件发送', prompt: '定时或批量发送邮件，支持附件处理', tools: ['api-call'], model: 'minimax-01' },
      ],
      tools: ['api-call'],
      workflow: ['邮件分类Agent', '回复生成Agent', '邮件发送Agent'],
    }
  }

  // 代码审查场景
  if (d.includes('代码') || d.includes('审查') || d.includes('review') || d.includes('bug')) {
    return {
      name: '🔍 代码审查智能体群',
      description: '自动审查代码质量、检测漏洞、生成改进建议',
      agents: [
        { name: '代码扫描Agent', role: '代码扫描', prompt: '扫描代码文件，识别语法错误和代码风格问题', tools: ['file-read', 'code-exec'], model: 'minimax-01' },
        { name: '安全检测Agent', role: '安全分析', prompt: '检测潜在的安全漏洞，如SQL注入、XSS等', tools: ['file-read'], model: 'minimax-01' },
        { name: '优化建议Agent', role: '优化建议', prompt: '根据代码质量分析结果，生成具体的优化建议', tools: ['file-write'], model: 'minimax-01' },
      ],
      tools: ['file-read', 'file-write', 'code-exec'],
      workflow: ['代码扫描Agent', '安全检测Agent', '优化建议Agent'],
    }
  }

  // 客服场景
  if (d.includes('客服') || d.includes('对话') || d.includes('问答')) {
    return {
      name: '💬 智能客服智能体群',
      description: '多轮对话客服，意图识别、知识库查询、回复生成、工单记录',
      agents: [
        { name: '意图识别Agent', role: '意图识别', prompt: '理解用户输入，判断用户意图和情感', tools: [], model: 'minimax-01' },
        { name: '知识库查询Agent', role: '知识检索', prompt: '从知识库中检索相关信息和答案', tools: ['web-search', 'api-call'], model: 'minimax-01' },
        { name: '回复生成Agent', role: '回复生成', prompt: '生成自然流畅的客服回复', tools: [], model: 'minimax-01' },
        { name: '工单记录Agent', role: '工单处理', prompt: '记录无法解答的问题，生成工单并跟踪', tools: ['file-write', 'api-call'], model: 'minimax-01' },
      ],
      tools: ['web-search', 'api-call', 'file-write'],
      workflow: ['意图识别Agent', '知识库查询Agent', '回复生成Agent', '工单记录Agent'],
    }
  }

  // 文档处理
  if (d.includes('文档') || d.includes('pdf') || d.includes('摘要') || d.includes('翻译')) {
    return {
      name: '📝 文档处理智能体群',
      description: 'PDF解析、摘要生成、翻译、关键信息提取',
      agents: [
        { name: '文档解析Agent', role: '文档解析', prompt: '解析PDF、Word等文档，提取文本内容', tools: ['file-read'], model: 'minimax-01' },
        { name: '摘要生成Agent', role: '摘要生成', prompt: '对文档内容生成简洁准确的摘要', tools: [], model: 'minimax-01' },
        { name: '翻译Agent', role: '翻译', prompt: '将文档翻译为目标语言', tools: ['api-call'], model: 'minimax-01' },
      ],
      tools: ['file-read', 'file-write', 'api-call'],
      workflow: ['文档解析Agent', '摘要生成Agent', '翻译Agent'],
    }
  }

  // 默认：通用助手
  return {
    name: '🤖 通用助手智能体群',
    description: '基于描述自动生成的通用智能体群',
    agents: [
      { name: '主控Agent', role: '协调', prompt: '理解用户需求，协调子Agent完成任务', tools: ['web-search'], model: 'minimax-01' },
      { name: '执行Agent', role: '执行', prompt: '执行具体任务，调用工具完成工作', tools: ['code-exec', 'file-read', 'file-write'], model: 'minimax-01' },
    ],
    tools: ['web-search', 'code-exec', 'file-read', 'file-write'],
    workflow: ['主控Agent', '执行Agent'],
  }
}

// 测试生成结果
async function testGenerated() {
  if (!generatedGroup.value) return
  testVisible.value = true
  testResult.value = ''
  testSteps.value = []
  testInput.value = generatedGroup.value.description || '你好，请介绍一下你的功能'
}

async function runTest() {
  if (!testInput.value.trim()) { ElMessage.warning('请输入测试内容'); return }
  testing.value = true
  testResult.value = ''
  testSteps.value = []

  try {
    const agents = generatedGroup.value.agents || []
    let lastOutput = testInput.value

    for (let i = 0; i < agents.length; i++) {
      const agent = agents[i]
      testSteps.value.push({ name: agent.name, output: '执行中…', ok: true })
      try {
        const output = await genAgentResponse(agent, lastOutput, testInput.value)
        testSteps.value[i].output = output
        lastOutput = output
      } catch (e) {
        testSteps.value[i].output = '❌ ' + (e.message || '执行失败')
        testSteps.value[i].ok = false
        lastOutput = `上一步出错: ${e.message}`
      }
    }

    testResult.value = lastOutput
    ElMessage.success('测试完成！')
  } catch (e) {
    testResult.value = '测试执行失败：' + (e.message || '')
  } finally {
    testing.value = false
  }
}

/**
 * V7.1: 真实调用后端 AI 接口
 * 调 /api/v1/agent/run (非流式)，传入 agent 的 prompt + 多 Agent 上下文
 */
async function genAgentResponse(agent, input, _origInput) {
  // 1. 构造系统 prompt（agent 的角色和能力）
  const systemPrompt = agent.prompt ||
    `你是 ${agent.name}，角色: ${agent.role || '通用助手'}。\n请根据你的职责处理以下输入，输出简洁有用的结果。`

  // 2. 调后端 (agentApi.execute 对接 /agent/run)
  const r = await agentApi.execute({
    goal: input,
    systemPrompt,
    agentType: agent.role || 'general',
    model: genModel.value || 'minimax-01',
    // 多 Agent 串联：把原始输入也传过去，便于后端理解完整上下文
    originalInput: _origInput || input,
  })

  // 3. 解析返回（兼容多种格式）
  const output = r?.data?.result ||
                r?.data?.content ||
                r?.data?.message ||
                r?.data?.text ||
                r?.result ||
                r?.message ||
                JSON.stringify(r?.data || r) ||
                ''

  return output || `✅ ${agent.name} 已处理`
}
let lastOutput = ''

// 导出 JSON 模型文件
function exportGenerated() {
  if (!generatedGroup.value) return
  const json = JSON.stringify(generatedGroup.value, null, 2)
  const blob = new Blob([json], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${generatedGroup.value.name.replace(/\s+/g, '_')}_model.json`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('模型文件已导出！')
}

// P1-3: 复制配置到剪贴板
function copyConfig() {
  if (!generatedGroup.value) return
  copyToClipboard(JSON.stringify(generatedGroup.value, null, 2))
}

// 运行智能体群（调用真实 Agent 编排接口）
async function runGenerated() {
  if (!generatedGroup.value) return
  const name = generatedGroup.value.name
  const goal = await ElMessageBox.prompt(
    '确认运行此智能体群，请输入运行任务描述：',
    `▶ 运行 ${name}`,
    { confirmButtonText: '确认运行', cancelButtonText: '取消', inputValue: generatedGroup.value.description }
  ).catch(() => null)
  if (!goal) return

  try {
    // 调用后端 Agent 执行接口，传入生成的 Group 配置 + 任务目标
    const r = await agentApi.execute({
      goal,
      agentType: 'general',
      maxSteps: 10,
      group: generatedGroup.value,  // 后端可识别 generatedGroup 结构
    })
    ElMessage.success(`运行成功！任务 ID: ${r.data?.id || '完成'}`)
    testVisible.value = false
    loadHistory()
  } catch (e) {
    ElMessage.error('运行失败：' + (e.message || ''))
  }
}

// 重新生成
async function regenerate() {
  await generateAgentGroup()
}

// ===== 原有功能 =====
const history = ref([])
const loading = ref(false)
// 兼容别名（tooltip 章节统一命名）
const historyList = history
const historyLoading = loading
const submitting = ref(false)
const createVisible = ref(false)
const resultVisible = ref(false)
const detailResult = ref(null)

const form = reactive({
  prompt: '', agentType: 'general', maxSteps: 8,
  temperature: 0.7, tools: [], model: '',
})

const stats = computed(() => {
  const list = history.value
  return {
    total: list.length,
    success: list.filter(h => h.status === 'SUCCESS' || h.status === 'success' || h.status === 'completed').length,
    failed: list.filter(h => h.status === 'FAILED' || h.status === 'failed' || h.status === 'error').length,
    running: list.filter(h => h.status === 'RUNNING' || h.status === 'running' || h.status === 'pending').length,
  }
})

function statusTag(s) {
  const map = {
    RUNNING: 'primary', SUCCESS: 'success', FAILED: 'danger', PENDING: 'info',
    running: 'warning', success: 'success', failed: 'danger', pending: 'info',
    completed: 'success', error: 'danger',
  }
  return map[s] || 'info'
}

async function loadHistory() {
  loading.value = true
  try {
    const r = await agentApi.list({ limit: 50 })
    history.value = r.data?.list || r.data || []
  } catch { history.value = [] }
  finally { loading.value = false }
}

function openCreate() {
  form.prompt = ''; form.agentType = 'general'; form.maxSteps = 8
  form.temperature = 0.7; form.tools = []; form.model = ''
  createVisible.value = true
}

function runTemplate(tpl) {
  form.prompt = tpl.prompt; form.agentType = tpl.agentType
  form.tools = tpl.tools || []; form.maxSteps = 8
  createVisible.value = true
}

async function submitTask() {
  if (!form.prompt.trim()) { ElMessage.warning('请输入任务描述'); return }
  submitting.value = true
  try {
    const payload = {
      goal: form.prompt, agentType: form.agentType, maxSteps: form.maxSteps,
      temperature: form.temperature, tools: form.tools, model: form.model || undefined,
    }
    const r = await agentApi.execute(payload)
    ElMessage.success('任务已提交！ID: ' + (r.data?.id || '完成'))
    createVisible.value = false
    await loadHistory()
  } catch (e) {
    ElMessage.error('提交失败：' + (e.message || '网络错误，请检查后端服务'))
  } finally {
    submitting.value = false
  }
}

async function viewResult(row) {
  try {
    const r = await agentApi.get(row.id)
    detailResult.value = r.data || row
  } catch { detailResult.value = row }
  resultVisible.value = true
}

// 查看历史记录（别名，兼容新表格）
function viewHistory(row) {
  viewResult(row)
}

// 删除历史记录
async function deleteHistory(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除任务「${row.goal || row.description || row.name || '记录 #' + row.id}」吗？`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    // 优先使用 agentApi.delete，若不存在则尝试 http.delete
    if (agentApi.delete) {
      await agentApi.delete(row.id)
    } else {
      await import('@/api/http').then(m => m.default.delete(`/api/v1/agent/history/${row.id}`))
    }
    ElMessage.success('已删除')
    loadHistory()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

async function retryTask(row) {
  try {
    await agentApi.execute({ goal: row.goal || row.prompt, agentType: row.agentType })
    ElMessage.success('任务已重新执行')
    loadHistory()
  } catch { ElMessage.error('重试失败') }
}

async function stopTask(row) {
  try {
    await agentApi.stop(row.id)
    ElMessage.success('任务已停止')
    loadHistory()
  } catch { ElMessage.error('停止失败') }
}

async function saveAsTemplate(row) {
  try {
    const name = await ElMessageBox.prompt('请输入模板名称', '存为模板', {
      confirmButtonText: '保存', cancelButtonText: '取消',
      inputValue: row.goal?.slice(0, 20) || '',
    })
    ElMessage.success('已保存为模板: ' + name)
  } catch {}
}

onMounted(() => {
  loadHistory()
  // 加载 4 个子模块的摘要数据，让 tab 切换有真实内容
  loadCanvasSummary()
  loadMultiSummary()
  loadTrainingSummary()
  loadApprovalSummary()
})
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; h2 { margin: 0; font-size: 16px; } }
.agent-tabs { margin-bottom: 0; }

/* ===== V7.0: Tab 内嵌摘要样式 ===== */
.tab-summary {
  padding: 12px 0 0;
}
.tab-summary-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding: 0 4px;
}
.tab-summary-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.ai-generate-card {
  border: 2px solid #dbeafe;
  :deep(.el-card__header) { background: #f0f9ff; padding: 10px 16px; }
}

.generating-icon {
  animation: spin 1s linear infinite;
  color: var(--el-color-primary);
}
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.generated-preview {
  background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 8px; padding: 12px;
  height: 100%; min-height: 200px;
}
.agent-chip {
  display: flex; align-items: center; gap: 6px; padding: 6px 10px;
  background: #fff; border: 1px solid #e5e7eb; border-radius: 20px; margin-bottom: 6px;
  font-size: 13px;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}
.tpl-card {
  cursor: pointer; transition: transform 0.15s, box-shadow 0.15s;
  &:hover { transform: translateY(-3px); box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
}

.step-item { padding: 4px 0; }
.step-header { display: flex; align-items: center; margin-bottom: 4px; }
.step-tool { font-weight: 600; font-size: 13px; }
.step-thought { font-size: 12px; color: var(--el-text-color-secondary); margin-bottom: 4px; font-style: italic; }
.step-result { font-size: 12px; color: var(--el-color-success); background: var(--el-color-success-light-9); padding: 6px; border-radius: 4px; margin-top: 4px;
  pre { margin: 0; white-space: pre-wrap; word-break: break-word; } }
.step-error { font-size: 12px; color: var(--el-color-danger); margin-top: 4px; }
.result-detail { padding: 0 4px; }
</style>
