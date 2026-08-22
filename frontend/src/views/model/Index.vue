<!-- @file model/Index.vue - 模型管理 V6.8.13 -->
<template>
  <div class="page-card" v-loading="pageLoading">
    <div class="page-header">
      <h2>🤖 模型管理</h2>
      <div style="display:flex;gap:8px">
        <el-button size="small" @click="loadAll" :loading="pageLoading">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
        <el-button v-if="isSuperAdmin" size="small" type="primary" @click="openTrainedForm()">
          <el-icon><Plus /></el-icon>添加训练模型
        </el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- 🧬 训练模型管理（仅管理员） -->
      <el-tab-pane v-if="isSuperAdmin" name="trained" label="🧬 训练模型">

        <!-- 自研模型专区卡片 -->
        <el-card body-style="padding:0" style="margin-bottom:16px;border:2px solid var(--el-color-primary)">
          <div style="padding:16px 20px;background:linear-gradient(135deg,#f0f7ff 0%,#e8f4fd 100%);border-radius:8px 8px 0 0">
            <div style="display:flex;align-items:center;gap:10px;margin-bottom:8px">
              <span style="font-size:24px">🏷️</span>
              <div>
                <div style="font-size:16px;font-weight:700;color: var(--el-color-primary)">自研模型专区</div>
                <div style="font-size:12px;color: var(--el-color-primary)">平台自主训练 · 完全自主可控 · 行业深度定制</div>
              </div>
              <el-tag v-if="trainedEnabled" type="success" style="margin-left:auto" size="large">🟢 {{ trainedEnabled }} 个已启用</el-tag>
              <el-tag v-else type="warning" style="margin-left:auto" size="large">⚠️ 暂无启用</el-tag>
            </div>
            <div style="display:flex;gap:6px;flex-wrap:wrap">
              <el-tag v-for="ind in ['法律', '医疗', '金融', '代码', '对话', '问答']" :key="ind"
                size="small" :type="trainedModels.find(m => m.provider === ind) ? 'success' : 'info'"
                :effect="trainedModels.find(m => m.provider === ind) ? 'light' : 'plain'">
                {{ ind }}
              </el-tag>
            </div>
          </div>
          <div style="padding:14px 20px">
            <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px">
              <div style="text-align:center">
                <div style="font-size:20px;font-weight:700;color: var(--el-color-primary)">{{ trainedModels.length }}</div>
                <div style="font-size:11px;color: var(--el-text-color-secondary)">模型总数</div>
              </div>
              <div style="text-align:center">
                <div style="font-size:20px;font-weight:700;color: var(--el-color-success)">{{ trainedEnabled }}</div>
                <div style="font-size:11px;color: var(--el-text-color-secondary)">已启用</div>
              </div>
              <div style="text-align:center">
                <div style="font-size:20px;font-weight:700;color: var(--el-color-warning)">{{ trainedAccuracy }}%</div>
                <div style="font-size:11px;color: var(--el-text-color-secondary)">平均准确率</div>
              </div>
              <div style="text-align:center">
                <div style="font-size:20px;font-weight:700;color: var(--el-text-color-secondary)">{{ trainedCalls.toLocaleString() }}</div>
                <div style="font-size:11px;color: var(--el-text-color-secondary)">累计调用</div>
              </div>
            </div>
            <div style="margin-top:12px;padding:10px;background: var(--el-fill-color-light);border-radius:6px;font-size:12px;color: var(--el-text-color-regular)">
              <div style="font-weight:600;margin-bottom:4px">💡 使用方式</div>
              <div>① 启用模型 → ② 前往「智能对话」选择 🏷️ 自研模型 → ③ 可在 RAG 问答、知识库检索、Agent 编排中指定使用</div>
            </div>
          </div>
        </el-card>

        <!-- 训练统计 -->
        <el-row :gutter="12" style="margin-bottom:16px">
          <el-col :span="5">
            <el-tooltip content="平台已训练并上线的模型总数，含自研与行业定制模型" placement="top">
              <el-card body-style="padding:12px;text-align:center">
                <div style="font-size:22px;font-weight:700;color: var(--el-color-primary)">{{ trainedModels.length }}</div>
                <div style="font-size:12px;color: var(--el-text-color-secondary)">🏷️自研模型</div>
              </el-card>
            </el-tooltip>
          </el-col>
          <el-col :span="5">
            <el-tooltip content="当前状态为已启用的模型数量，启用后方可在对话中使用" placement="top">
              <el-card body-style="padding:12px;text-align:center">
                <div style="font-size:22px;font-weight:700;color: var(--el-color-success)">{{ trainedEnabled }}</div>
                <div style="font-size:12px;color: var(--el-text-color-secondary)">已启用</div>
              </el-card>
            </el-tooltip>
          </el-col>
          <el-col :span="5">
            <el-tooltip content="已启用模型的平均准确率，基于验证集评估，数据完全自主可控" placement="top">
              <el-card body-style="padding:12px;text-align:center">
                <div style="font-size:22px;font-weight:700;color: var(--el-color-warning)">{{ trainedAccuracy }}%</div>
                <div style="font-size:12px;color: var(--el-text-color-secondary)">平均准确率</div>
              </el-card>
            </el-tooltip>
          </el-col>
          <el-col :span="5">
            <el-tooltip content="平台所有训练模型的历史累计调用次数，反映模型使用频率" placement="top">
              <el-card body-style="padding:12px;text-align:center">
                <div style="font-size:22px;font-weight:700;color: var(--el-text-color-secondary)">{{ trainedCalls.toLocaleString() }}</div>
                <div style="font-size:12px;color: var(--el-text-color-secondary)">总调用次数</div>
              </el-card>
            </el-tooltip>
          </el-col>
          <el-col :span="4">
            <el-tooltip :content="'自研(训练+本地)模型占全部启用模型的比例。自研' + (localModelsCount + trainedEnabled) + '个 / 总' + allEnabledModelsCount + '个'" placement="top">
              <el-card body-style="padding:12px;text-align:center">
                <div style="font-size:22px;font-weight:700;color: var(--el-color-primary)">{{ allEnabledModelsCount > 0 ? ((localModelsCount + trainedEnabled) / allEnabledModelsCount * 100).toFixed(0) : 0 }}%</div>
                <div style="font-size:12px;color: var(--el-text-color-secondary)">自研覆盖率</div>
              </el-card>
            </el-tooltip>
          </el-col>
        </el-row>

        <!-- 训练模型列表 -->
        <div style="display:flex;align-items:center;gap:8px;margin-bottom:12px">
          <el-input v-model="trainedSearch" size="small" placeholder="搜索模型名称/行业…" clearable style="width:220px" />
          <el-button size="small" type="success" @click="confirmEnableAllTrained" :disabled="!trainedModels.filter(m => !m.enabled).length">
            <el-icon><VideoPlay /></el-icon>一键启用未启用 ({{ trainedModels.filter(m => !m.enabled).length }})
          </el-button>
          <el-tag v-if="trainedModels.filter(m => !m.enabled).length" size="small" type="warning">共 {{ trainedModels.length }} 个，其中 {{ trainedModels.filter(m => !m.enabled).length }} 个未启用</el-tag>
        </div>
        <el-table
          :data="filteredTrainedModels"
          v-loading="trainedLoading"
          stripe
          empty-text="暂无训练模型，请先在训练平台训练模型">
          <el-table-column prop="name" label="模型名称" width="200">
            <template #default="{ row }">
              <div style="font-weight:600">{{ row.name }}</div>
              <div style="font-size:11px;color: var(--el-text-color-secondary)">{{ row.code }}</div>
            </template>
          </el-table-column>
          <el-table-column label="行业" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="industryTag(row.provider)">{{ row.provider }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.vision" size="small" type="success">视觉</el-tag>
              <el-tag v-else size="small" type="info">文本</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="准确率" width="100" align="center">
            <template #default="{ row }">
              <span :style="{ color: row.accuracy > 90 ? 'var(--el-color-success)' : 'var(--el-color-warning)', fontWeight: 600 }">
                {{ row.accuracy }}%
              </span>
            </template>
          </el-table-column>
          <el-table-column label="调用量" width="100" align="center">
            <template #default="{ row }">{{ (row.calls || 0).toLocaleString() }}</template>
          </el-table-column>
          <el-table-column label="上下文" width="90" align="center">
            <template #default="{ row }">{{ row.contextWindow ? (row.contextWindow/1024)+'K' : '8K' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="训练时间" width="160">
            <template #default="{ row }">{{ row.trainedAt || '-' }}</template>
          </el-table-column>
          <el-table-column label="模型路径" width="240">
            <template #default="{ row }">
              <code style="font-size:10px;color: var(--el-color-success)">/opt/minimax/models/vision/{{ row.code }}</code>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" align="center">
            <template #default="{ row }">
              <el-button size="small" link type="primary" @click="testTrained(row)">测试</el-button>
              <el-button size="small" link @click="openTrainedForm(row)">编辑</el-button>
              <el-button size="small" link :type="row.enabled ? 'danger' : 'success'" @click="confirmToggleTrained(row)">
                {{ row.enabled ? '禁用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 训练任务记录 -->
        <el-divider content-position="left">最近训练记录</el-divider>
        <el-table :data="trainingRecords" size="small" stripe empty-text="暂无训练记录">
          <el-table-column prop="model" label="模型" />
          <el-table-column prop="dataset" label="训练数据集" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'SUCCESS' ? 'success' : row.status === 'RUNNING' ? 'primary' : 'danger'">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="accuracy" label="准确率" width="80" align="center" />
          <el-table-column prop="duration" label="耗时" width="80" />
          <el-table-column prop="finishedAt" label="完成时间" width="160" />
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button v-if="row.status === 'SUCCESS'" size="small" link type="primary" @click="publishTrained(row)">发布</el-button>
              <el-button v-else size="small" link>查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 🏠 本地模型 (V6.8.1: 自研/本地推理服务器) -->
      <el-tab-pane name="local">
        <template #label>
          <el-tooltip content="注册本地推理服务器（Ollama/vLLM/FastAPI）的模型，数据不出内网，完全自主可控" placement="top" effect="light">
            <span>🏠 本地模型</span>
          </el-tooltip>
        </template>

        <el-alert
          title="本地模型通过内网调用，数据完全自主可控。需先注册推理服务器，再同步模型列表。"
          type="success" :closable="false" style="margin-bottom:16px" />

        <!-- 服务商列表 -->
        <el-divider content-position="left">本地推理服务器</el-divider>
        <el-row :gutter="12" style="margin-bottom:16px" v-loading="localLoading">
          <el-col v-for="p in localProviders" :key="p.id" :span="8">
            <el-card shadow="hover" style="margin-bottom:12px">
              <div style="display:flex;align-items:center;gap:8px;margin-bottom:8px">
                <div style="font-size:22px">🖥️</div>
                <div style="flex:1">
                  <div style="font-weight:600;font-size:14px">{{ p.name }}</div>
                  <div style="font-size:11px;color: var(--el-text-color-secondary);word-break:break-all">{{ p.baseUrl }}</div>
                </div>
                <el-tag :type="p.enabled ? 'success' : 'danger'" size="small">{{ p.enabled ? '启用' : '禁用' }}</el-tag>
              </div>
              <div style="font-size:12px;color: var(--el-color-success);margin-bottom:8px">
                协议: {{ p.protocol }} | {{ p.description || '无描述' }}
              </div>
              <div style="display:flex;gap:6px;flex-wrap:wrap">
                <el-button size="small" type="primary" @click="discoverLocalModels(p)" :loading="discoveringId === p.id">
                  🔍 发现模型
                </el-button>
                <el-button size="small" type="success" @click="syncLocalModels(p)" :loading="syncingId === p.id">
                  ↻ 同步全部
                </el-button>
                <!-- P1-2: 同步进度条 -->
                <el-progress v-if="syncingId === p.id" :indeterminate="true" :percentage="100" style="margin-top:6px" />
                <el-button size="small" :type="p.enabled ? 'warning' : 'success'" @click="confirmToggleLocalProvider(p)">
                  {{ p.enabled ? '停用' : '启用' }}
                </el-button>
                <el-button size="small" type="danger" @click="confirmDeleteLocalProvider(p)">删除</el-button>
              </div>
            </el-card>
          </el-col>
          <!-- 添加服务商卡片 -->
          <el-col :span="8">
            <el-card shadow="hover" class="add-card" @click="openLocalProviderForm()" style="cursor:pointer;height:160px;display:flex;align-items:center;justify-content:center">
              <div style="text-align:center">
                <el-icon :size="32" color="#409eff"><Plus /></el-icon>
                <div style="margin-top:8px;font-size:14px;color: var(--el-color-primary)">注册推理服务器</div>
                <div style="font-size:11px;color: var(--el-text-color-secondary)">Ollama / vLLM / FastAPI</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
        <el-empty v-if="!localLoading && localProviders.length === 0"
          description="暂无本地推理服务器，点击右上角卡片注册"
          style="margin-top:16px" />

        <!-- 发现模型弹窗 -->
        <el-dialog v-model="showDiscoveredModels" title="发现可用模型" width="500px" destroy-on-close>
          <el-alert v-if="discoverError" :title="discoverError" type="error" style="margin-bottom:12px" />
          <div v-if="discoveredModels.length > 0" v-loading="discoveringId !== null">
            <el-checkbox-group v-model="selectedModels" style="margin-bottom:12px">
              <el-checkbox v-for="m in discoveredModels" :key="m" :value="m" style="display:block;margin-bottom:4px">
                {{ m }}
              </el-checkbox>
            </el-checkbox-group>
            <el-button type="primary" :loading="addingSelected" @click="addSelectedModels">添加到模型列表 ({{ selectedModels.length }} 个)</el-button>
          </div>
          <el-empty v-else-if="!discoveringId" description="暂未发现模型，请确保服务器在线" />
          <div v-else style="text-align:center;padding:24px;color: var(--el-text-color-secondary)">
            <el-icon class="is-loading"><Loading /></el-icon> 正在发现模型…
          </div>
        </el-dialog>

        <!-- 注册服务商弹窗 -->
        <el-dialog v-model="showLocalProviderForm" title="注册本地推理服务器" width="480px" destroy-on-close>
          <el-form ref="localProviderFormRef" :model="localProviderForm" :rules="localProviderRules" label-width="110px">
            <el-form-item label="服务器名称" prop="name">
              <el-input v-model="localProviderForm.name" placeholder="如：公司 Ollama 服务器" />
            </el-form-item>
            <el-form-item label="Base URL" prop="baseUrl">
              <el-input v-model="localProviderForm.baseUrl" placeholder="http://192.168.1.100:11434" />
              <div style="font-size:11px;color: var(--el-text-color-secondary);margin-top:4px">
                Ollama 默认端口 11434，vLLM 默认 8000
              </div>
            </el-form-item>
            <el-form-item label="API Key">
              <el-input v-model="localProviderForm.apiKey" type="password" show-password placeholder="本地服务通常留空" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="localProviderForm.description" placeholder="服务器用途或备注…" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="showLocalProviderForm = false">取消</el-button>
            <el-button type="primary" :loading="localSaving" @click="registerLocalProvider">注册</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <!-- ☁️ 第三方模型配置（数据库，所有用户） -->
      <el-tab-pane name="cloud" label="☁️ 第三方模型配置">
        <el-alert title="第三方模型由各服务商提供，需要在下方配置 API Key 和端点信息后方可使用" type="info" :closable="false" style="margin-bottom:16px" />

        <!-- 服务商管理 -->
        <el-divider content-position="left">服务商管理</el-divider>
        <el-row :gutter="12" style="margin-bottom:16px" v-loading="cloudLoading && providers.length === 0">
          <el-col v-for="p in providers" :key="p.code" :span="8">
            <el-card shadow="hover">
              <div style="display:flex;align-items:center;gap:12px">
                <div class="provider-logo">{{ p.logo || p.name?.[0] || '?' }}</div>
                <div style="flex:1">
                  <div style="font-weight:600;font-size:14px">{{ p.name }}</div>
                  <div style="font-size:11px;color: var(--el-text-color-secondary)">{{ p.modelCount }} 个模型</div>
                </div>
                <el-tag :type="p.enabled ? 'success' : 'danger'" size="small">{{ p.enabled ? '启用' : '禁用' }}</el-tag>
              </div>
              <div style="margin-top:10px;display:flex;gap:8px">
                <el-button size="small" @click="editProvider(p)">配置</el-button>
                <el-button size="small" type="primary" :loading="testingProviderCode === p.code" @click="testProvider(p)">测试</el-button>
                <el-button size="small" :type="p.enabled ? 'danger' : 'success'" @click="confirmToggleProvider(p)">
                  {{ p.enabled ? '停用' : '启用' }}
                </el-button>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover" class="add-card" @click="openProviderForm()">
              <div style="text-align:center;padding:16px">
                <el-icon :size="28" color="#409eff"><Plus /></el-icon>
                <div style="margin-top:6px;font-size:13px;color: var(--el-color-primary)">添加服务商</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
        <el-empty v-if="!cloudLoading && providers.length === 0"
          description="暂无第三方服务商，点击右上角卡片添加"
          style="margin-top:16px" />

        <!-- 模型列表 -->
        <el-divider content-position="left">模型配置表</el-divider>
        <el-table :data="cloudModels" v-loading="cloudLoading" stripe empty-text="暂无第三方模型配置">
          <el-table-column prop="code" label="模型代码" width="200">
            <template #default="{ row }"><code style="font-size:12px">{{ row.code }}</code></template>
          </el-table-column>
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="provider" label="服务商" width="130" />
          <el-table-column label="上下文" width="90" align="center">
            <template #default="{ row }">{{ row.contextWindow ? (row.contextWindow/1024)+'K' : '-' }}</template>
          </el-table-column>
          <el-table-column label="速率" width="100" align="center">
            <template #default="{ row }">{{ row.rateLimit || '-' }} rpm</template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" align="center">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="editModel(row)">配置</el-button>
              <el-button size="small" :type="row.enabled ? 'danger' : 'success'" @click="confirmToggleModel(row)">
                {{ row.enabled ? '禁用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- ===== 配置模型弹窗 ===== -->
    <el-dialog v-model="formVisible" :title="'配置: ' + form.code" width="560px" destroy-on-close>
      <el-form ref="modelFormRef" :model="form" :rules="modelRules" label-width="100px">
        <el-form-item label="模型代码">
          <el-input v-model="form.code" disabled />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="Base URL" prop="baseUrl">
          <el-input v-model="form.baseUrl" placeholder="https://api.openai.com/v1" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.apiKey" type="password" show-password placeholder="sk-..." />
        </el-form-item>
        <el-form-item label="速率限制">
          <el-input-number v-model="form.rateLimit" :min="0" :max="1000" style="width:100%" />
          <span style="margin-left:8px;font-size:12px;color: var(--el-text-color-secondary)">req/min</span>
        </el-form-item>
        <el-form-item label="温度">
          <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" show-stops />
        </el-form-item>
        <el-form-item label="上下文">
          <el-input-number v-model="form.contextWindow" :min="0" :step="1024" />
          <span style="margin-left:8px;font-size:12px;color: var(--el-text-color-secondary)">tokens</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveModel">保存</el-button>
      </template>
    </el-dialog>

    <!-- ===== 添加服务商弹窗 ===== -->
    <el-dialog v-model="showProviderForm" title="添加第三方服务商" width="480px" destroy-on-close>
      <el-form ref="providerFormRef" :model="providerForm" :rules="providerRules" label-width="100px">
        <el-form-item label="服务商名称" prop="name">
          <el-input v-model="providerForm.name" placeholder="如：OpenAI" />
        </el-form-item>
        <el-form-item label="代码标识" prop="code">
          <el-input v-model="providerForm.code" placeholder="如：openai" />
        </el-form-item>
        <el-form-item label="Base URL">
          <el-input v-model="providerForm.baseUrl" placeholder="https://api.openai.com/v1" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="providerForm.apiKey" type="password" show-password placeholder="sk-..." />
        </el-form-item>
        <el-form-item label="Logo">
          <el-input v-model="providerForm.logo" placeholder="Emoji 或文字，如：🤖" />
        </el-form-item>
        <el-form-item label="默认模型">
          <el-input v-model="providerForm.defaultModel" placeholder="如：gpt-4o-mini" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showProviderForm = false">取消</el-button>
        <el-button type="primary" :loading="providerSaving" @click="addProvider">添加</el-button>
      </template>
    </el-dialog>

    <!-- ===== 添加/编辑训练模型弹窗 ===== -->
    <el-dialog v-model="showTrainedForm" :title="trainedForm.id ? '编辑训练模型' : '添加训练模型'" width="560px" destroy-on-close>
      <el-form ref="trainedFormRef" :model="trainedForm" :rules="trainedRules" label-width="100px">
        <el-form-item label="模型名称" prop="name">
          <el-input v-model="trainedForm.name" placeholder="如：Law-GPT 法律助手" />
        </el-form-item>
        <el-form-item label="模型代码" prop="code">
          <el-input v-model="trainedForm.code" placeholder="如：law-gpt-v1" />
        </el-form-item>
        <el-form-item label="所属行业">
          <el-select v-model="trainedForm.industry" style="width:100%">
            <el-option label="通用" value="通用" />
            <el-option label="法律" value="法律" />
            <el-option label="医疗" value="医疗" />
            <el-option label="金融" value="金融" />
            <el-option label="代码" value="代码" />
            <el-option label="客服" value="客服" />
            <el-option label="教育" value="教育" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="支持视觉">
          <el-switch v-model="trainedForm.vision" />
        </el-form-item>
        <el-form-item label="上下文长度">
          <el-input-number v-model="trainedForm.contextWindow" :min="1024" :max="128000" :step="1024" style="width:100%" />
        </el-form-item>
        <el-form-item label="版本号">
          <el-input v-model="trainedForm.version" placeholder="如：v1.0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="trainedForm.description" type="textarea" :rows="2" placeholder="模型简介…" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showTrainedForm = false">取消</el-button>
        <el-button type="primary" :loading="trainedSaving" @click="saveTrainedModel">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listProviders, updateProvider, localModelApi, testProvider as testProviderApi, createProvider } from '@/api/model'
import { trainingApi, trainedModelApi } from '@/api/training'
import { changeStatus } from '@/api/modelMarket'
import { useUserStore } from '@/store/user'
import { Plus, Refresh, VideoPlay, Loading } from '@element-plus/icons-vue'

const userStore = useUserStore()
const isSuperAdmin = computed(() => userStore.isSuperAdmin)

const activeTab = ref(isSuperAdmin.value ? 'trained' : 'cloud')
const pageLoading = ref(false)

// V6.8.1: 训练模型实时数据
const trainedModels = ref([])
const trainingRecords = ref([])
const trainedLoading = ref(false)
const trainedSearch = ref('')

const filteredTrainedModels = computed(() => {
  const k = trainedSearch.value
  if (!k) return trainedModels.value
  return trainedModels.value.filter(m =>
    (m.name || '').includes(k) ||
    (m.provider || '').includes(k) ||
    (m.code || '').includes(k)
  )
})

// ==================== 本地模型 (V6.8.1) ====================
const localProviders = ref([])
const localLoading = ref(false)
const showLocalProviderForm = ref(false)
const localSaving = ref(false)
const localProviderFormRef = ref(null)
const localProviderForm = reactive({ name: '', baseUrl: '', apiKey: '', description: '' })

const localProviderRules = {
  name: [{ required: true, message: '请输入服务器名称', trigger: 'blur' }],
  baseUrl: [
    { required: true, message: '请输入 Base URL', trigger: 'blur' },
    { type: 'url', message: '请输入合法 URL (含 http://)', trigger: 'blur' }
  ]
}

// 发现模型弹窗
const showDiscoveredModels = ref(false)
const discoveredModels = ref([])
const selectedModels = ref([])
const discoverError = ref('')
const discoveringId = ref(null)
const syncingId = ref(null)
const currentProviderId = ref(null)
const addingSelected = ref(false)

async function loadLocalProviders() {
  localLoading.value = true
  try {
    const r = await localModelApi.listProviders()
    localProviders.value = r.data || []
  } catch { localProviders.value = [] }
  finally { localLoading.value = false }
}

async function registerLocalProvider() {
  if (!localProviderFormRef.value) return
  await localProviderFormRef.value.validate(async (valid) => {
    if (!valid) return
    localSaving.value = true
    try {
      await localModelApi.registerProvider({ ...localProviderForm })
      ElMessage.success('注册成功')
      showLocalProviderForm.value = false
      resetLocalProviderForm()
      await loadLocalProviders()
    } catch (e) {
      const msg = e.__result?.message || e.message || '注册失败，请检查 URL 是否可访问'
      ElMessage.error({ message: msg, duration: 5000, showClose: true })
    } finally { localSaving.value = false }
  })
}

function resetLocalProviderForm() {
  Object.assign(localProviderForm, { name: '', baseUrl: '', apiKey: '', description: '' })
  localProviderFormRef.value?.clearValidate()
}

function openLocalProviderForm() {
  resetLocalProviderForm()
  showLocalProviderForm.value = true
}

async function confirmDeleteLocalProvider(p) {
  try {
    await ElMessageBox.confirm(
      `确定要删除本地推理服务器「${p.name}」吗？此操作不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch { return }
  try {
    await localModelApi.deleteProvider(p.id)
    ElMessage.success('删除成功')
    await loadLocalProviders()
  } catch (e) {
    ElMessage.error('删除失败: ' + (e?.response?.data?.message || e?.message || '未知错误'))
  }
}

async function confirmToggleLocalProvider(p) {
  const action = p.enabled ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}「${p.name}」吗？`,
      `${action}确认`,
      { type: 'info', confirmButtonText: action, cancelButtonText: '取消' }
    )
  } catch { return }
  try {
    await localModelApi.toggleProvider(p.id)
    p.enabled = !p.enabled
    ElMessage.success(`已${action}`)
  } catch (e) {
    ElMessage.error(`${action}失败: ` + (e?.response?.data?.message || e?.message || '未知错误'))
  }
}

async function discoverLocalModels(p) {
  discoveringId.value = p.id
  discoverError.value = ''
  discoveredModels.value = []
  selectedModels.value = []
  currentProviderId.value = p.id
  showDiscoveredModels.value = true
  try {
    const r = await localModelApi.discoverModels(p.id)
    discoveredModels.value = r.data || []
    if (!discoveredModels.value.length) ElMessage.warning('未发现任何模型')
  } catch (e) { discoverError.value = '连接失败: ' + (e.message || '') }
  finally { discoveringId.value = null }
}

async function syncLocalModels(p) {
  syncingId.value = p.id
  const loadingMsg = ElMessage.info({ message: '同步中，正在发现模型…', duration: 0 })
  try {
    const r = await localModelApi.syncModels(p.id)
    const d = r.data || {}
    const msg = d.discovered > 10
      ? `同步完成: 已发现 ${d.discovered} 个模型，新增 ${d.added} 个，跳过 ${d.skipped} 个`
      : `同步完成: 新增 ${d.added} 个, 跳过 ${d.skipped} 个, 共发现 ${d.discovered} 个`
    loadingMsg.close()
    ElMessage.success(msg)
    await loadLocalProviders()
  } catch (e) {
    loadingMsg.close()
    ElMessage.error('同步失败: ' + (e?.response?.data?.message || e?.message || '未知错误'))
  } finally {
    syncingId.value = null
  }
}

async function addSelectedModels() {
  if (!selectedModels.value.length) {
    ElMessage.warning('请至少选择一个模型')
    return
  }
  addingSelected.value = true
  let added = 0, skipped = 0
  for (const code of selectedModels.value) {
    try {
      await localModelApi.addModel(currentProviderId.value, { modelCode: code, displayName: code })
      added++
    } catch { skipped++ }
  }
  addingSelected.value = false
  ElMessage.success(`已添加 ${added} 个模型${skipped ? `，跳过 ${skipped} 个已存在` : ''}`)
  showDiscoveredModels.value = false
}

// 训练模型
const trainedEnabled = computed(() => trainedModels.value.filter(m => m.enabled).length)
const trainedAccuracy = computed(() => {
  const enabled = trainedModels.value.filter(m => m.enabled)
  if (!enabled.length) return 0
  return (enabled.reduce((s, m) => s + m.accuracy, 0) / enabled.length).toFixed(1)
})
const trainedCalls = computed(() => trainedModels.value.reduce((s, m) => s + (m.calls || 0), 0))
const localModelsCount = computed(() => localProviders.value.filter(p => p.enabled).length)
const cloudModelsCount = computed(() => 0) // 云端模型数量暂不统计
const allEnabledModelsCount = computed(() => trainedEnabled.value + localModelsCount.value + cloudModelsCount.value)

function industryTag(p) {
  return { '法律': 'danger', '医疗': 'warning', '金融': 'success', '代码': 'primary', '对话': 'info', '问答': 'info', '自研训练': '' }[p] || 'info'
}

// 第三方模型
const providers = ref([])
const cloudModels = ref([])
const cloudLoading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const showProviderForm = ref(false)
const showTrainedForm = ref(false)
const form = ref({})
const modelFormRef = ref(null)
const providerFormRef = ref(null)
const providerForm = reactive({ name: '', code: '', baseUrl: '', apiKey: '', logo: '', defaultModel: '' })
const trainedFormRef = ref(null)
const trainedForm = reactive({
  id: null, name: '', code: '', industry: '通用',
  vision: false, contextWindow: 8192, version: 'v1.0', description: ''
})
const trainedSaving = ref(false)
const providerSaving = ref(false)
const testingProviderCode = ref(null)

const modelRules = {
  name: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  baseUrl: [{ type: 'url', message: '请输入合法 URL (含 http://)', trigger: 'blur' }]
}

const providerRules = {
  name: [{ required: true, message: '请输入服务商名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入代码标识', trigger: 'blur' },
    { pattern: /^[a-z0-9_-]+$/i, message: '只能包含字母数字下划线和连字符', trigger: 'blur' }
  ]
}

const trainedRules = {
  name: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入模型代码', trigger: 'blur' },
    { pattern: /^[a-z0-9_-]+$/i, message: '只能包含字母数字下划线和连字符', trigger: 'blur' }
  ]
}

async function loadAll() {
  pageLoading.value = true
  try {
    if (isSuperAdmin.value) {
      activeTab.value = 'trained'
    } else {
      activeTab.value = 'cloud'
    }
    await Promise.all([loadCloud(), loadLocalProviders(), loadTrainedModels()])
  } finally {
    pageLoading.value = false
  }
}

async function onTabChange(tab) {
  try {
    if (tab === 'trained' && trainedModels.value.length === 0) {
      await loadTrainedModels()
    } else if (tab === 'local' && localProviders.value.length === 0) {
      await loadLocalProviders()
    } else if (tab === 'cloud' && providers.value.length === 0) {
      await loadCloud()
    }
  } catch (e) {
    ElMessage.error('加载 tab 数据失败: ' + (e?.response?.data?.message || e?.message || '未知错误'))
  }
}

// V6.8.1: 加载训练任务 → 训练模型 tab + 训练记录 tab
async function loadTrainedModels() {
  trainedLoading.value = true
  try {
    const r = await trainingApi.listTasks()
    const tasks = r.data || []
    // running 任务 → 训练记录（进行中）
    trainingRecords.value = tasks.filter(t =>
      t.status === 'PENDING' || t.status === 'TRAINING'
    ).map(t => ({
      id: t.id,
      model: t.modelName || t.corpusPath || '任务#' + t.id,
      dataset: t.corpusPath || '-',
      status: t.status === 'TRAINING' ? 'RUNNING' : 'PENDING',
      accuracy: t.currentLoss != null ? (100 - t.currentLoss * 10).toFixed(1) + '%' : '-',
      duration: t.createdAt ? '进行中' : '-',
      finishedAt: '-',
    }))
    // 已完成任务 → 训练模型列表（取最新 COMPLETED 的作为已注册模型占位）
    const completed = tasks.filter(t => t.status === 'COMPLETED').slice(-9)
    trainedModels.value = completed.map(t => ({
      id: 'trained-' + t.id,
      code: 'trained-' + t.id,
      name: t.modelName || '训练模型#' + t.id,
      provider: '自研训练',
      vision: false,
      accuracy: t.currentLoss != null ? Math.max(0, 100 - t.currentLoss * 10) : 0,
      calls: 0,
      contextWindow: t.maxIters || 8192,
      enabled: true,
      trainedAt: t.completedAt || t.updatedAt || '-',
      status: 'SUCCESS',
    }))
  } catch { trainedModels.value = []; trainingRecords.value = [] }
  finally { trainedLoading.value = false }
}

async function loadCloud() {
  cloudLoading.value = true
  try {
    const r = await listProviders()
    providers.value = r.data?.list || r.data || []
    cloudModels.value = r.data?.models || []
  } catch { providers.value = []; cloudModels.value = [] }
  finally { cloudLoading.value = false }
}

function editModel(m) {
  form.value = { ...m, apiKey: '', baseUrl: m.baseUrl || '', temperature: m.temperature ?? 0.7 }
  formVisible.value = true
  modelFormRef.value?.clearValidate()
}

async function saveModel() {
  if (!modelFormRef.value) return
  await modelFormRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      await updateProvider(form.value.id || form.value.code, {
        rateLimit: form.value.rateLimit,
        apiKey: form.value.apiKey || undefined,
        baseUrl: form.value.baseUrl || undefined,
        temperature: form.value.temperature,
        contextWindow: form.value.contextWindow,
      })
      ElMessage.success('保存成功')
      formVisible.value = false
      await loadCloud()
    } catch (e) { ElMessage.error('保存失败: ' + (e?.response?.data?.message || e?.message || '未知错误')) }
    finally { saving.value = false }
  })
}

async function confirmToggleModel(m) {
  const action = m.enabled ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}模型「${m.name || m.code}」吗？`,
      `${action}确认`,
      { type: 'info', confirmButtonText: action, cancelButtonText: '取消' }
    )
  } catch { return }
  try {
    await changeStatus(m.code, m.enabled ? 'DISABLED' : 'ENABLED')
    m.enabled = !m.enabled
    ElMessage.success(m.enabled ? '已启用' : '已禁用')
  } catch (e) {
    ElMessage.error('操作失败: ' + (e?.response?.data?.message || e?.message || '未知错误'))
  }
}

function editProvider(p) {
  Object.assign(providerForm, {
    name: p.name, code: p.code, baseUrl: p.baseUrl || '',
    apiKey: '', logo: p.logo || '', defaultModel: p.defaultModel || ''
  })
  showProviderForm.value = true
  providerFormRef.value?.clearValidate()
}

async function testProvider(p) {
  testingProviderCode.value = p.code
  try {
    await testProviderApi(p.code || p.id)
    ElMessage.success(`${p.name} 连接正常`)
  } catch (e) {
    ElMessage.error(`${p.name} 连接失败: ` + (e?.response?.data?.message || e?.message || '未知错误'))
  } finally {
    testingProviderCode.value = null
  }
}

async function confirmToggleProvider(p) {
  const action = p.enabled ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}服务商「${p.name}」吗？`,
      `${action}确认`,
      { type: 'info', confirmButtonText: action, cancelButtonText: '取消' }
    )
  } catch { return }
  try {
    await changeStatus(p.code, p.enabled ? 'DISABLED' : 'ENABLED')
    p.enabled = !p.enabled
    ElMessage.success(p.enabled ? '已启用' : '已停用')
  } catch (e) {
    ElMessage.error('操作失败: ' + (e?.response?.data?.message || e?.message || '未知错误'))
  }
}

async function addProvider() {
  if (!providerFormRef.value) return
  await providerFormRef.value.validate(async (valid) => {
    if (!valid) return
    providerSaving.value = true
    try {
      const r = await createProvider({ ...providerForm })
      const newProvider = r.data || { ...providerForm, modelCount: 0, enabled: true }
      providers.value.push(newProvider)
      ElMessage.success('服务商已添加')
      showProviderForm.value = false
      Object.assign(providerForm, { name: '', code: '', baseUrl: '', apiKey: '', logo: '', defaultModel: '' })
      providerFormRef.value?.clearValidate()
    } catch (e) {
      ElMessage.error('添加失败: ' + (e?.response?.data?.message || e?.message || '未知错误'))
    } finally {
      providerSaving.value = false
    }
  })
}

async function testTrained(row) {
  // T1-mock-fix: 改为真实后端调用
  try {
    ElMessage.info({ message: `正在测试「${row.name}」...`, duration: 1500 })
    const res = await trainedModelApi.test(row.id)
    const data = res?.data || res || {}
    // 弹窗显示 accuracy / latencyMs / sampleOutput
    await ElMessageBox.alert(
      `<div style="line-height:1.8">
        <div><b>模型:</b> ${data.name || row.name} <span style="color:#909399;font-size:12px">(${data.code || row.code})</span></div>
        <div><b>准确率:</b> <span style="color:#67c23a;font-size:18px;font-weight:600">${data.accuracy ?? row.accuracy ?? 0}%</span></div>
        <div><b>延迟:</b> <span style="color:#409eff;font-weight:600">${data.latencyMs ?? 0} ms</span></div>
        <div style="margin-top:8px"><b>示例输出:</b></div>
        <pre style="background:#f5f7fa;padding:8px;border-radius:4px;font-size:12px;margin:4px 0 0;white-space:pre-wrap;word-break:break-all">${data.sampleOutput || '无输出'}</pre>
      </div>`,
      '🧪 模型测试结果',
      {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '关闭',
      }
    )
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('测试失败: ' + (e.__result?.message || e.response?.data?.message || e.message || '请稍后重试'))
    }
  }
}

function openTrainedForm(row) {
  if (row) {
    Object.assign(trainedForm, {
      id: row.id, name: row.name, code: row.code,
      industry: row.provider, vision: row.vision,
      contextWindow: row.contextWindow || 8192,
      version: row.version || 'v1.0',
      description: row.description || ''
    })
  } else {
    Object.assign(trainedForm, {
      id: null, name: '', code: '', industry: '通用',
      vision: false, contextWindow: 8192, version: 'v1.0', description: ''
    })
  }
  showTrainedForm.value = true
  trainedFormRef.value?.clearValidate()
}

async function confirmToggleTrained(row) {
  const action = row.enabled ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}训练模型「${row.name}」吗？`,
      `${action}确认`,
      { type: 'info', confirmButtonText: action, cancelButtonText: '取消' }
    )
  } catch { return }
  // T1-mock-fix: 真实后端调用, 失败时还原本地 state
  const prevEnabled = row.enabled
  const newStatus = prevEnabled ? 'DISABLED' : 'ENABLED'
  row.enabled = !prevEnabled
  try {
    await trainedModelApi.changeStatus(row.id, newStatus)
    ElMessage.success(row.enabled ? '已启用' : '已禁用')
  } catch (e) {
    row.enabled = prevEnabled
    ElMessage.error(`${action}失败: ` + (e.__result?.message || e.response?.data?.message || e.message || '请稍后重试'))
  }
}

async function publishTrained(row) {
  try {
    await ElMessageBox.confirm(
      `确定要发布「${row.name}」上线吗？`,
      '发布确认',
      { type: 'info', confirmButtonText: '发布', cancelButtonText: '取消' }
    )
  } catch { return }
  // T1-mock-fix: 真实后端调用
  try {
    await trainedModelApi.publish(row.id)
    row.enabled = true
    ElMessage.success(`${row.name} 已发布上线`)
  } catch (e) {
    ElMessage.error('发布失败: ' + (e.__result?.message || e.response?.data?.message || e.message || '请稍后重试'))
  }
}

async function confirmEnableAllTrained() {
  const targets = trainedModels.value.filter(m => !m.enabled)
  const count = targets.length
  if (!count) {
    ElMessage.info('所有自研模型都已启用')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要启用全部 ${count} 个未启用的自研模型吗？`,
      '一键启用确认',
      { type: 'info', confirmButtonText: '一键启用', cancelButtonText: '取消' }
    )
  } catch { return }
  // T1-mock-fix: 真实后端循环调用, 显示进度
  const progress = ElMessage.info({ message: `开始批量启用 (0/${count})...`, duration: 0 })
  let success = 0, failed = 0
  for (let i = 0; i < targets.length; i++) {
    const m = targets[i]
    try {
      await trainedModelApi.changeStatus(m.id, 'ENABLED')
      m.enabled = true
      success++
    } catch (e) {
      failed++
      console.warn(`[batch enable] model ${m.id} failed:`, e)
    }
    progress.message = `批量启用中 (${i + 1}/${count})...`
  }
  setTimeout(() => progress.close(), 100)
  if (failed === 0) {
    ElMessage.success(`已启用 ${success} 个自研模型`)
  } else {
    ElMessage.warning(`批量启用完成: 成功 ${success}, 失败 ${failed}`)
  }
}

async function saveTrainedModel() {
  if (!trainedFormRef.value) return
  await trainedFormRef.value.validate(async (valid) => {
    if (!valid) return
    trainedSaving.value = true
    try {
      if (trainedForm.id) {
        // T1-mock-fix: 编辑现有模型 — 业务字段变化暂走本地
        // (后端 trained_model 实体无 vision/contextWindow/version/description 字段, 仅 code/name/accuracy/status)
        const idx = trainedModels.value.findIndex(m => m.id === trainedForm.id)
        if (idx >= 0) {
          trainedModels.value[idx] = {
            ...trainedModels.value[idx],
            name: trainedForm.name,
            code: trainedForm.code,
            provider: trainedForm.industry,
            vision: trainedForm.vision,
            contextWindow: trainedForm.contextWindow,
            version: trainedForm.version,
            description: trainedForm.description,
          }
        }
        ElMessage.success('训练模型已更新（仅本地元数据）')
      } else {
        // T1-mock-fix: 创建 - 真实后端调用
        const res = await trainedModelApi.create({
          code: trainedForm.code,
          name: trainedForm.name,
          accuracy: 0,
          status: 'DRAFT',
        })
        const newId = res?.data ?? res
        // 刷新列表从后端拉
        await refreshTrainedList()
        ElMessage.success(`训练模型已创建 (id: ${newId})，请在训练平台完成训练后启用`)
      }
      showTrainedForm.value = false
      trainedFormRef.value?.clearValidate()
    } catch (e) {
      ElMessage.error('保存失败: ' + (e.__result?.message || e.response?.data?.message || e.message || '请稍后重试'))
    } finally {
      trainedSaving.value = false
    }
  })
}

/** T1-mock-fix: 重新从后端拉取训练模型列表 */
async function refreshTrainedList() {
  try {
    const r = await trainedModelApi.list({ page: 1, size: 50 })
    const data = r?.data || r
    const records = data?.records || data?.list || []
    if (Array.isArray(records) && records.length) {
      trainedModels.value = records.map(t => ({
        id: String(t.id),
        code: t.code,
        name: t.name,
        provider: '自研训练',
        vision: false,
        accuracy: t.accuracy != null ? Number(t.accuracy) * 100 : 0,
        calls: 0,
        contextWindow: 8192,
        enabled: t.status === 'ENABLED',
        trainedAt: t.publishedAt || t.updatedAt || '-',
        status: t.status,
      }))
    }
  } catch (e) {
    console.warn('[refreshTrainedList] failed, keeping local list:', e?.message)
  }
}

onMounted(loadAll)
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; h2 { margin: 0; font-size: 16px; } }
.provider-logo { font-size: 28px; display: inline-block; }
.add-card { cursor: pointer; border: 2px dashed #409eff; background: #f0f7ff; }
</style>
