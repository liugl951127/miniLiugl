<!--
  @file views/ai/Intent.vue (V3.5.75 标准化模板)
  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js
-->
<!--
  @file views/ai/Intent.vue (V3.5.48)
  @description 意图识别 - 单条/批量预测, 关键词管理, 配置管理
  - 11 端点: predict / predict/batch / keyword / phrase / list / stats / benchmark / config / config (PUT) / config/reset / context/clear
-->
<template>
  <div class="page-intent page">
    <el-card>
      <template #header>
        <div class="header">
          <span>🧠 意图识别 <el-tag size="small" type="success">V3.5.48</el-tag></span>
          <div>
            <el-radio-group v-model="tab" size="small">
              <el-radio-button value="predict">⚡ 预测</el-radio-button>
              <el-radio-button value="keywords">🔑 关键词</el-radio-button>
              <el-radio-button value="config">⚙️ 配置</el-radio-button>
              <el-radio-button value="stats">📊 统计</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>

      <!-- 模式 1: 预测 -->
      <div v-if="tab === 'predict'" class="tab-section">
        <el-tabs v-model="predictMode">
          <el-tab-pane label="单条预测" name="single">
            <el-form label-position="top">
              <el-form-item label="用户输入">
                <el-input v-model="singleForm.text" type="textarea" :rows="3"
                  placeholder="例: 帮我查一下天气 / 给我转 100 元给张三 / 创建一个项目" />
              </el-form-item>
              <el-form-item label="Session ID (上下文)">
                <el-input v-model="singleForm.sessionId" placeholder="可选, 用于上下文关联" style="width: 320px" />
              </el-form-item>
              <el-button type="primary" @click="onPredictSingle" :loading="predicting">🎯 预测</el-button>
            </el-form>
            <div v-if="predictResult.intent" class="result-block">
              <h4>预测结果</h4>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="意图">{{ predictResult.intent }}</el-descriptions-item>
                <el-descriptions-item label="置信度">
                  <el-progress :percentage="(predictResult.confidence || 0) * 100" />
                </el-descriptions-item>
                <el-descriptions-item label="类别">{{ predictResult.category }}</el-descriptions-item>
                <el-descriptions-item label="响应时间">{{ predictResult.duration }}ms</el-descriptions-item>
                <el-descriptions-item label="关键词" :span="2">
                  <el-tag v-for="k in predictResult.keywords" :key="k" size="small" style="margin-right: 4px">{{ k }}</el-tag>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </el-tab-pane>

          <el-tab-pane label="批量预测" name="batch">
            <el-form label-position="top">
              <el-form-item label="批量输入 (一行一个)">
                <el-input v-model="batchForm.texts" type="textarea" :rows="6"
                  placeholder="查天气&#10;转 100 元给张三&#10;创建项目" />
              </el-form-item>
              <el-button type="primary" @click="onPredictBatch" :loading="predicting">🎯 批量预测</el-button>
            </el-form>
            <div v-if="batchResult.length" class="result-block">
              <h4>批量结果 ({{ batchResult.length }} 条)</h4>
              <el-table :data="batchResult" border>
                <el-table-column type="index" label="#" width="60" />
                <el-table-column prop="text" label="输入" />
                <el-table-column prop="intent" label="意图" width="150" />
                <el-table-column prop="confidence" label="置信度" width="120">
                  <template #default="{ row }">
                    <el-progress :percentage="(row.confidence || 0) * 100" :stroke-width="10" />
                  </template>
                </el-table-column>
                <el-table-column prop="category" label="类别" width="100" />
              </el-table>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 模式 2: 关键词管理 -->
      <div v-if="tab === 'keywords'" class="tab-section">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>添加关键词</template>
              <el-form label-position="top">
                <el-form-item label="关键词">
                  <el-input v-model="keywordForm.keyword" placeholder="例: 天气" />
                </el-form-item>
                <el-form-item label="对应意图">
                  <el-select v-model="keywordForm.intent" placeholder="选择意图" filterable style="width: 100%">
                    <el-option v-for="i in intentOptions" :key="i.code" :label="i.name" :value="i.code" />
                  </el-select>
                </el-form-item>
                <el-form-item label="权重">
                  <el-input-number v-model="keywordForm.weight" :min="0.1" :max="10" :step="0.1" />
                </el-form-item>
                <el-button type="primary" @click="onAddKeyword">+ 添加关键词</el-button>
              </el-form>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>添加短语 (V3.5.6+)</template>
              <el-form label-position="top">
                <el-form-item label="短语">
                  <el-input v-model="phraseForm.phrase" placeholder="例: 帮我查一下今天天气" />
                </el-form-item>
                <el-form-item label="对应意图">
                  <el-select v-model="phraseForm.intent" placeholder="选择意图" filterable style="width: 100%">
                    <el-option v-for="i in intentOptions" :key="i.code" :label="i.name" :value="i.code" />
                  </el-select>
                </el-form-item>
                <el-button type="primary" @click="onAddPhrase">+ 添加短语</el-button>
              </el-form>
            </el-card>
          </el-col>
        </el-row>

        <h4 style="margin-top: 16px">📋 意图列表 ({{ intentOptions.length }} 个)</h4>
        <el-table :data="intentOptions" border>
          <el-table-column prop="code" label="意图编码" width="200" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="category" label="类别" width="150" />
          <el-table-column prop="keywords" label="关键词">
            <template #default="{ row }">
              <el-tag v-for="k in (row.keywords || []).slice(0, 5)" :key="k" size="small" style="margin-right: 4px">{{ k }}</el-tag>
              <span v-if="(row.keywords || []).length > 5" style="color: #909399">+{{ row.keywords.length - 5 }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 模式 3: 配置 -->
      <div v-if="tab === 'config'" class="tab-section">
        <el-form label-position="top" v-loading="configLoading">
          <el-form-item label="置信度阈值">
            <el-slider v-model="config.threshold" :min="0" :max="1" :step="0.05" show-input />
          </el-form-item>
          <el-form-item label="Top K">
            <el-input-number v-model="config.topK" :min="1" :max="10" />
          </el-form-item>
          <el-form-item label="启用模型">
            <el-checkbox-group v-model="config.models">
              <el-checkbox value="keyword">关键词</el-checkbox>
              <el-checkbox value="embedding">Embedding</el-checkbox>
              <el-checkbox value="llm">LLM (自研)</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="onSaveConfig">💾 保存配置</el-button>
            <el-button @click="onResetConfig" type="danger">⚠ 重置配置</el-button>
            <el-button @click="onClearContext">🗑 清空上下文</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 模式 4: 统计 -->
      <div v-if="tab === 'stats'" class="tab-section">
        <el-row :gutter="16">
          <el-col :span="6"><div class="stat-card"><div class="num">{{ stats.totalPredict || 0 }}</div><div>总预测数</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="num">{{ stats.avgConfidence ? (stats.avgConfidence * 100).toFixed(1) : 0 }}%</div><div>平均置信度</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="num">{{ stats.intentCount || intentOptions.length }}</div><div>意图数</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="num">{{ stats.avgDuration || 0 }}ms</div><div>平均耗时</div></div></el-col>
        </el-row>
        <h4 style="margin-top: 16px">🏆 Top 意图 (按调用次数)</h4>
        <el-table :data="stats.topIntents" border>
          <el-table-column type="index" label="#" width="60" />
          <el-table-column prop="intent" label="意图" />
          <el-table-column prop="count" label="调用次数" width="150" />
          <el-table-column prop="success" label="成功率" width="150">
            <template #default="{ row }">
              <el-progress :percentage="row.success" />
            </template>
          </el-table-column>
        </el-table>
        <div style="margin-top: 16px">
          <el-button @click="onBenchmark" :loading="benchmarking">🧪 跑基准测试</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, reactive, onMounted } from 'vue'
import { useToast } from '@/composables/useToast'
import { ElMessageBox } from 'element-plus'
import { intentPredict, intentPredictBatch, intentAddKeyword, intentAddPhrase, intentList, intentStats, intentBenchmark, intentGetConfig, intentUpdateConfig, intentResetConfig, intentClearContext } from '@/api/ai'

const tab = ref('predict')
const toast = useToast()
const predictMode = ref('single')
const predicting = ref(false)
const configLoading = ref(false)
const benchmarking = ref(false)
const intentOptions = ref([])

const singleForm = reactive({ text: '', sessionId: '' })
const batchForm = reactive({ texts: '查天气\n转 100 元给张三\n创建项目' })
const keywordForm = reactive({ keyword: '', intent: '', weight: 1.0 })
const phraseForm = reactive({ phrase: '', intent: '' })

const predictResult = reactive({ intent: '', confidence: 0, category: '', duration: 0, keywords: [] })
const batchResult = ref([])
const stats = reactive({ totalPredict: 0, avgConfidence: 0, intentCount: 0, avgDuration: 0, topIntents: [] })
const config = reactive({ threshold: 0.6, topK: 3, models: ['keyword', 'embedding'] })

async function loadIntents() {
  try {
    const r = await intentList()
    intentOptions.value = r.data || []
  } catch (e) {}
}

async function loadStats() {
  try {
    const r = await intentStats()
    Object.assign(stats, r.data || {})
  } catch (e) {}
}

async function loadConfig() {
  configLoading.value = true
  try {
    const r = await intentGetConfig()
    Object.assign(config, r.data || {})
  } catch (e) {} finally { configLoading.value = false }
}

async function onPredictSingle() {
  if (!singleForm.text) { toast.warning('请输入'); return }
  predicting.value = true
  try {
    const r = await intentPredict({ text: singleForm.text, sessionId: singleForm.sessionId })
    Object.assign(predictResult, r.data || {})
  } catch (e) {} finally { predicting.value = false }
}

async function onPredictBatch() {
  if (!batchForm.texts) { toast.warning('请输入'); return }
  const texts = batchForm.texts.split('\n').filter(s => s.trim())
  predicting.value = true
  try {
    const r = await intentPredictBatch({ texts })
    batchResult.value = r.data || []
    toast.success(`批量预测 ${batchResult.value.length} 条`)
  } catch (e) {} finally { predicting.value = false }
}

async function onAddKeyword() {
  if (!keywordForm.keyword || !keywordForm.intent) { toast.warning('请填完整'); return }
  try {
    await intentAddKeyword(keywordForm)
    toast.success('关键词已添加')
    keywordForm.keyword = ''
    loadIntents()
  } catch (e) {}
}

async function onAddPhrase() {
  if (!phraseForm.phrase || !phraseForm.intent) { toast.warning('请填完整'); return }
  try {
    await intentAddPhrase(phraseForm)
    toast.success('短语已添加')
    phraseForm.phrase = ''
    loadIntents()
  } catch (e) {}
}

async function onSaveConfig() {
  try {
    await intentUpdateConfig(config)
    toast.success('配置已保存')
  } catch (e) {}
}

async function onResetConfig() {
  try {
    await ElMessageBox.confirm('重置为默认配置?', '警告', { type: 'warning' })
    await intentResetConfig()
    toast.success('已重置')
    loadConfig()
  } catch (e) { if (e !== 'cancel') {} }
}

async function onClearContext() {
  try {
    await ElMessageBox.confirm('清空所有意图上下文?', '警告', { type: 'warning' })
    await intentClearContext({})
    toast.success('上下文已清空')
  } catch (e) { if (e !== 'cancel') {} }
}

async function onBenchmark() {
  benchmarking.value = true
  try {
    const r = await intentBenchmark({})
    toast.success(`基准测试完成, 平均准确率: ${(r.data?.accuracy * 100).toFixed(1)}%`)
  } catch (e) {} finally { benchmarking.value = false }
}

onMounted(() => {
  loadIntents()
  loadStats()
  loadConfig()
})
</script>

<style scoped>
.page { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.tab-section { padding: 8px 0; }
.result-block { margin-top: 16px; padding: 16px; background: #f5f7fa; border-radius: 4px; }
.stat-card { padding: 16px; background: #fff; border-radius: 4px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
.stat-card .num { font-size: 28px; font-weight: 600; color: #409eff; margin-bottom: 4px; }
</style>
