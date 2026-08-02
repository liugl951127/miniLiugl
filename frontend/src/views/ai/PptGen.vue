<!--
  @file views/ai/PptGen.vue (V3.5.75 标准化模板)
  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js
-->
<!--
  @file views/ai/PptGen.vue (V3.5.48)
  @description PPT 自动生成 - 用户目标能力之一
  - 4 端点: generate / auto / themes / parse
  - 大纲输入 → 4 套主题选择 → 生成 base64 → 浏览器下载
-->
<template>
  <div class="page-ppt-gen page">
    <el-card>
      <template #header>
        <div class="header">
          <span>📊 PPT 自动生成 <el-tag size="small" type="success">V3.5.48</el-tag></span>
          <div>
            <el-radio-group v-model="mode" size="small">
              <el-radio-button value="manual">📝 大纲模式</el-radio-button>
              <el-radio-button value="auto">⚡ 一句话模式</el-radio-button>
              <el-radio-button value="parse">🔍 解析预览</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>

      <!-- 模式 1: 大纲模式 -->
      <div v-if="mode === 'manual'" class="mode-section">
        <el-form label-position="top">
          <el-form-item label="PPT 大纲 (Markdown 格式)">
            <el-input
              v-model="manualForm.outline"
              type="textarea"
              :rows="10"
              placeholder="# 主标题&#10;## 第一部分&#10;- 要点 1&#10;- 要点 2&#10;## 第二部分&#10;- 要点 3"
            />
          </el-form-item>
          <el-form-item label="主题">
            <el-select v-model="manualForm.theme" placeholder="选择主题" style="width: 200px">
              <el-option v-for="t in themes" :key="t.code" :label="t.name" :value="t.code" />
            </el-select>
          </el-form-item>
          <el-form-item label="页数">
            <el-input-number v-model="manualForm.pageCount" :min="3" :max="30" />
          </el-form-item>
          <el-button type="primary" @click="onGenerateManual" :loading="generating">🎨 生成 PPT</el-button>
        </el-form>
      </div>

      <!-- 模式 2: 一句话模式 -->
      <div v-if="mode === 'auto'" class="mode-section">
        <el-form label-position="top">
          <el-form-item label="主题 (一句话)">
            <el-input v-model="autoForm.title" placeholder="例: 2026 AI 发展趋势预测" />
          </el-form-item>
          <el-form-item label="主题">
            <el-select v-model="autoForm.theme" placeholder="选择主题" style="width: 200px">
              <el-option v-for="t in themes" :key="t.code" :label="t.name" :value="t.code" />
            </el-select>
          </el-form-item>
          <el-form-item label="页数">
            <el-input-number v-model="autoForm.pageCount" :min="3" :max="30" />
          </el-form-item>
          <el-button type="success" @click="onGenerateAuto" :loading="generating">⚡ 一键生成</el-button>
        </el-form>
      </div>

      <!-- 模式 3: 解析预览 -->
      <div v-if="mode === 'parse'" class="mode-section">
        <el-form label-position="top">
          <el-form-item label="大纲 (Markdown)">
            <el-input
              v-model="parseForm.outline"
              type="textarea"
              :rows="8"
              placeholder="输入大纲, 只解析预览不生成"
            />
          </el-form-item>
          <el-button @click="onParse" :loading="parsing">🔍 解析</el-button>
        </el-form>
        <div v-if="parseResult.slides" class="parse-result">
          <h3>解析结果 ({{ parseResult.slideCount }} 页)</h3>
          <el-table :data="parseResult.slides" border>
            <el-table-column prop="index" label="#" width="60" />
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column prop="title" label="标题" />
            <el-table-column prop="subtitle" label="副标题" />
            <el-table-column prop="bullets" label="要点">
              <template #default="{ row }">
                <div v-for="(b, i) in row.bullets" :key="i" class="bullet">• {{ b }}</div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-card>

    <!-- 生成结果 -->
    <el-card v-if="result.size" class="result-card">
      <template #header>
        <div class="header">
          <span>✅ 生成成功</span>
          <el-button type="primary" @click="downloadPpt">📥 下载 PPT</el-button>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="文件名">{{ result.filename }}</el-descriptions-item>
        <el-descriptions-item label="幻灯片数">{{ result.slideCount }} 页</el-descriptions-item>
        <el-descriptions-item label="主题">{{ result.theme }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ formatSize(result.size) }}</el-descriptions-item>
      </el-descriptions>
      <div class="slide-preview">
        <h4>幻灯片预览</h4>
        <div v-for="(s, i) in result.slides" :key="i" class="slide-item">
          <div class="slide-num">#{{ i + 1 }}</div>
          <div class="slide-content">
            <div class="slide-title">{{ s.title }}</div>
            <div v-if="s.subtitle" class="slide-subtitle">{{ s.subtitle }}</div>
            <ul v-if="s.bullets">
              <li v-for="(b, j) in s.bullets" :key="j">{{ b }}</li>
            </ul>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, reactive, onMounted } from 'vue'
import { useToast } from '@/composables/useToast'

import { pptGenerate, pptAuto, pptThemes, pptParse } from '@/api/ai'

const mode = ref('manual')
const toast = useToast()
const generating = ref(false)
const parsing = ref(false)
const themes = ref([])

const manualForm = reactive({
  outline: '# 我的演讲\n## 第一部分\n- 要点 1\n- 要点 2\n## 第二部分\n- 要点 3',
  theme: 'BUSINESS_BLUE',
  pageCount: 6
})

const autoForm = reactive({
  title: '2026 AI 发展趋势预测',
  theme: 'BUSINESS_BLUE',
  pageCount: 6
})

const parseForm = reactive({
  outline: ''
})

const result = reactive({
  base64: '',
  size: 0,
  slideCount: 0,
  theme: '',
  filename: '',
  slides: []
})

const parseResult = reactive({
  slideCount: 0,
  slides: null
})

async function loadThemes() {
  try {
    const r = await pptThemes()
    themes.value = r.data || []
    if (themes.value.length && !manualForm.theme) {
      manualForm.theme = themes.value[0].code
      autoForm.theme = themes.value[0].code
    }
  } catch (e) {
    // 静默失败
  }
}

async function onGenerateManual() {
  if (!manualForm.outline) {
    toast.warning('请输入大纲')
    return
  }
  generating.value = true
  try {
    const r = await pptGenerate({
      outline: manualForm.outline,
      theme: manualForm.theme,
      pageCount: manualForm.pageCount
    })
    const data = r.data || {}
    Object.assign(result, data)
    toast.success(`PPT 生成成功 (${data.slideCount} 页)`)
  } catch (e) {
    // 错误已统一处理
  } finally {
    generating.value = false
  }
}

async function onGenerateAuto() {
  if (!autoForm.title) {
    toast.warning('请输入主题')
    return
  }
  generating.value = true
  try {
    const r = await pptAuto({
      title: autoForm.title,
      theme: autoForm.theme,
      pageCount: autoForm.pageCount
    })
    const data = r.data || {}
    Object.assign(result, data)
    toast.success(`PPT 自动生成成功 (${data.slideCount} 页)`)
  } catch (e) {
  } finally {
    generating.value = false
  }
}

async function onParse() {
  if (!parseForm.outline) {
    toast.warning('请输入大纲')
    return
  }
  parsing.value = true
  try {
    const r = await pptParse({ outline: parseForm.outline })
    Object.assign(parseResult, r.data || {})
    toast.success(`解析完成 (${parseResult.slideCount} 页)`)
  } catch (e) {
  } finally {
    parsing.value = false
  }
}

function downloadPpt() {
  if (!result.base64) {
    toast.warning('请先生成 PPT')
    return
  }
  const link = document.createElement('a')
  link.href = `data:application/vnd.openxmlformats-officedocument.presentationml.presentation;base64,${result.base64}`
  link.download = result.filename || 'presentation.pptx'
  link.click()
}

function formatSize(b) {
  if (b < 1024) return b + ' B'
  if (b < 1024 * 1024) return (b / 1024).toFixed(1) + ' KB'
  return (b / 1024 / 1024).toFixed(2) + ' MB'
}

onMounted(loadThemes)
</script>

<style scoped>
.page { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.mode-section { padding: 8px 0; }
.result-card { margin-top: 16px; }
.parse-result { margin-top: 16px; }
.parse-result h3 { margin-bottom: 12px; }
.bullet { font-size: 12px; line-height: 1.4; }
.slide-preview { margin-top: 16px; }
.slide-preview h4 { margin-bottom: 12px; }
.slide-item {
  display: flex;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 12px;
  margin-bottom: 8px;
  background: #fafafa;
}
.slide-num {
  font-weight: 600;
  color: #409eff;
  margin-right: 12px;
  min-width: 40px;
}
.slide-content { flex: 1; }
.slide-title { font-weight: 600; font-size: 14px; margin-bottom: 4px; }
.slide-subtitle { color: #909399; font-size: 13px; margin-bottom: 4px; }
.slide-content ul { margin: 4px 0 0 16px; padding: 0; }
.slide-content li { font-size: 13px; line-height: 1.5; }
</style>
