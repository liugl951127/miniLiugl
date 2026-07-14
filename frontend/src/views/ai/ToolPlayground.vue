<template>
  <PageContainer title="AI 工具演练场" subtitle="9 大类 19 工具 · 无需代码即可使用" icon="🛠">
    <el-row :gutter="16">
      <!-- 左侧: 工具选择 -->
      <el-col :span="6">
        <el-card shadow="never" class="tool-list-card">
          <template #header>
            <span>📦 工具库</span>
            <el-input v-model="searchTool" size="small" placeholder="搜索工具" clearable style="margin-top: 8px" />
          </template>
          <div v-for="(tools, cat) in groupedTools" :key="cat" class="category">
            <div class="category-name">{{ categoryLabel(cat) }} ({{ tools.length }})</div>
            <div
              v-for="t in tools"
              :key="t.code"
              :class="['tool-item', { active: currentTool?.code === t.code }]"
              @click="selectTool(t)"
            >
              <div class="tool-title">{{ t.name }}</div>
              <div class="tool-desc">{{ t.description }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 中间: 参数配置 -->
      <el-col :span="9">
        <el-card v-if="currentTool" shadow="never">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>⚙️ 参数配置: {{ currentTool.name }}</span>
              <el-tag :type="categoryColor(currentTool.category)">{{ currentTool.category }}</el-tag>
            </div>
          </template>

          <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
            <template #title>
              <span style="font-size: 13px">{{ currentTool.description }}</span>
            </template>
          </el-alert>

          <!-- 动态参数表单 -->
          <el-form :model="inputParams" label-width="140px" label-position="left">
            <el-form-item
              v-for="field in currentTool.fields"
              :key="field.name"
              :label="field.label"
            >
              <component
                :is="field.component"
                v-model="inputParams[field.name]"
                v-bind="field.props"
              >
                <el-option v-for="o in field.options" :key="o" :label="o" :value="o" v-if="field.type === 'select'" />
              </component>
            </el-form-item>
          </el-form>

          <div class="action-row">
            <el-button @click="loadExample">📋 填入示例</el-button>
            <el-button type="primary" :loading="running" @click="invoke">🚀 调用</el-button>
          </div>
        </el-card>
        <el-empty v-else description="选择左侧工具开始" :image-size="100" />
      </el-col>

      <!-- 右侧: 结果 -->
      <el-col :span="9">
        <el-card shadow="never">
          <template #header>
            <span>📊 结果</span>
            <el-tag v-if="result?.success" type="success" size="small">✓ 成功</el-tag>
            <el-tag v-else-if="result && !result.success" type="danger" size="small">✗ 失败</el-tag>
          </template>

          <StateBlock v-if="running" type="loading" message="调用中..." />
          <div v-else-if="result">
            <el-alert v-if="!result.success" type="error" :closable="false" show-icon>
              <template #title>{{ result.message || '调用失败' }}</template>
            </el-alert>
            <div v-else>
              <el-descriptions :column="2" border size="small" style="margin-bottom: 12px">
                <el-descriptions-item v-for="(v, k) in summaryFields" :key="k" :label="k">
                  <span v-if="typeof v === 'number'">{{ v.toFixed(2) }}</span>
                  <span v-else-if="typeof v === 'object'">{{ JSON.stringify(v).slice(0, 50) }}</span>
                  <span v-else>{{ v }}</span>
                </el-descriptions-item>
              </el-descriptions>

              <el-tabs v-model="resultTab" type="border-card">
                <el-tab-pane label="📄 完整结果" name="all">
                  <pre class="result-pre">{{ JSON.stringify(result, null, 2) }}</pre>
                </el-tab-pane>
                <el-tab-pane v-if="result.imageBase64" label="🖼️ 图片" name="image">
                  <img :src="`data:image/png;base64,${result.imageBase64}`" style="max-width: 100%" />
                </el-tab-pane>
                <el-tab-pane v-if="result.midiBase64" label="🎵 MIDI" name="midi">
                  <el-button @click="downloadMidi" type="success">💾 下载 MIDI</el-button>
                  <p style="font-size: 12px; color: #909399; margin-top: 8px">
                    {{ result.sizeBytes }} 字节 · {{ result.style }} · {{ result.bpm }} BPM
                  </p>
                </el-tab-pane>
              </el-tabs>
            </div>
          </div>
          <el-empty v-else description="执行后显示结果" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>
  </PageContainer>
</template>

<script setup>
import { ref, computed, watch, markRaw } from 'vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'
import StateBlock from '@/components/StateBlock.vue'
import { ElInput, ElInputNumber, ElSelect, ElOption, ElSwitch } from 'element-plus'
import {
  analyzeText, analyzeVision, analyzeAudio, convertFile,
  analyzeCorrelation, predictData, timeConvert,
  generateImageTool, generateChartTool, generateMusicTool,
  generateJavaProject, downloadJavaProject, downloadJavaProjectFromBase64,
  listTools
} from '@/api/ai'

const Input = markRaw(ElInput)
const NumberInput = markRaw(ElInputNumber)
const Select = markRaw(ElSelect)
const Switch = markRaw(ElSwitch)

// 工具定义 (前端, 与后端 AI 工具对应)
const toolDefs = [
  {
    code: 'text.analyze', name: '文本分析', description: '摘要/情感/实体/关键词', category: 'text',
    fields: [
      { name: 'text', label: '文本', type: 'input', component: Input, props: { type: 'textarea', rows: 4, placeholder: '输入要分析的文本' } },
      { name: 'task', label: '任务', type: 'select', component: Select, options: ['all', 'summary', 'sentiment', 'entities', 'keywords'], props: { placeholder: 'all' } },
      { name: 'topK', label: '关键词数', type: 'number', component: NumberInput, props: { min: 1, max: 50 } }
    ],
    example: { text: 'Liugl-AI 是企业级 AI 平台, 性能优秀, 体验好! 我们都喜欢它.', task: 'all', topK: 5 },
    invoke: analyzeText
  },
  {
    code: 'data.analyze.correlation', name: '相关性分析', description: 'Pearson/Spearman 相关系数', category: 'data',
    fields: [
      { name: 'dataSourceId', label: '数据源 ID', type: 'number', component: NumberInput, props: { min: 1 } },
      { name: 'table', label: '表名', type: 'input', component: Input, props: { placeholder: 'user' } },
      { name: 'columns', label: '列名 (逗号分隔)', type: 'input', component: Input, props: { placeholder: 'age,score,balance' } },
      { name: 'method', label: '方法', type: 'select', component: Select, options: ['pearson', 'spearman'], props: {} }
    ],
    example: { dataSourceId: 1, table: 'user', columns: 'age,score', method: 'pearson' },
    invoke: analyzeCorrelation
  },
  {
    code: 'data.predict.linear', name: '线性预测', description: '线性回归 + 移动平均 + 指数平滑', category: 'data',
    fields: [
      { name: 'values', label: '历史值 (逗号分隔)', type: 'input', component: Input, props: { placeholder: '10,20,30,40,50' } },
      { name: 'method', label: '方法', type: 'select', component: Select, options: ['linear', 'ma3', 'ma5', 'ma7', 'exp'], props: {} },
      { name: 'periods', label: '预测期数', type: 'number', component: NumberInput, props: { min: 1, max: 30 } }
    ],
    example: { values: '10,20,30,40,50,60,70,80', method: 'linear', periods: 5 },
    invoke: predictData
  },
  {
    code: 'time.convert', name: '时间工具', description: '格式转换/计算/时区', category: 'time',
    fields: [
      { name: 'op', label: '操作', type: 'select', component: Select, options: ['now', 'parse', 'format', 'add', 'diff', 'zones'], props: {} },
      { name: 'text', label: '时间字符串', type: 'input', component: Input, props: { placeholder: '2026-07-12 12:00:00' } },
      { name: 'epochMillis', label: '时间戳(ms)', type: 'number', component: NumberInput, props: {} },
      { name: 'timezone', label: '时区', type: 'input', component: Input, props: { placeholder: 'Asia/Shanghai' } }
    ],
    example: { op: 'now', timezone: 'Asia/Shanghai' },
    invoke: timeConvert
  },
  {
    code: 'file.convert', name: '文件转换', description: 'JSON/YAML/CSV/Base64 互转', category: 'file',
    fields: [
      { name: 'op', label: '操作', type: 'select', component: Select, options: ['format', 'text2csv', 'csv2text', 'json2yaml', 'yaml2json', 'json2csv', 'text2base64', 'base642text'], props: {} },
      { name: 'text', label: '内容', type: 'input', component: Input, props: { type: 'textarea', rows: 5 } }
    ],
    example: { op: 'format', text: '{"name":"张三","age":25}' },
    invoke: convertFile
  },
  {
    code: 'image.generate', name: 'AIGC 图片', description: '7 种类型程序化图像', category: 'image',
    fields: [
      { name: 'prompt', label: '描述', type: 'input', component: Input, props: { placeholder: '蓝色渐变背景' } },
      { name: 'type', label: '类型 (留空自动)', type: 'select', component: Select, options: ['', 'abstract', 'gradient', 'pattern', 'text', 'scene', 'logo', 'infographic'], props: { clearable: true } },
      { name: 'width', label: '宽度', type: 'number', component: NumberInput, props: { min: 100, max: 4096, step: 100 } },
      { name: 'height', label: '高度', type: 'number', component: NumberInput, props: { min: 100, max: 4096, step: 100 } },
      { name: 'seed', label: '种子', type: 'number', component: NumberInput, props: { min: 0 } }
    ],
    example: { prompt: '蓝色渐变背景', type: 'gradient', width: 512, height: 512, seed: 42 },
    invoke: generateImageTool
  },
  {
    code: 'chart.generate', name: 'AI 图表', description: '7 种图表 (柱/折/饼/散/雷达/热力/桑基)', category: 'chart',
    fields: [
      { name: 'type', label: '类型', type: 'select', component: Select, options: ['BAR', 'LINE', 'PIE', 'SCATTER', 'RADAR', 'HEATMAP', 'SANKEY'], props: {} },
      { name: 'title', label: '标题', type: 'input', component: Input, props: {} },
      { name: 'seriesJson', label: '数据 (JSON)', type: 'input', component: Input, props: { type: 'textarea', rows: 4, placeholder: '[{"name":"A","values":[1,2,3]}]' } }
    ],
    example: { type: 'BAR', title: '示例', seriesJson: '[{"name":"销售","values":[120,200,150,80,70,110,130]}]' },
    invoke: generateChartTool
  },
  {
    code: 'music.generate', name: 'AI 音乐', description: '6 风格 7 调式 MIDI', category: 'music',
    fields: [
      { name: 'style', label: '风格', type: 'select', component: Select, options: ['POP', 'CLASSICAL', 'ROCK', 'JAZZ', 'FOLK', 'ELECTRONIC'], props: {} },
      { name: 'key', label: '调式', type: 'select', component: Select, options: ['C', 'D', 'E', 'F', 'G', 'A', 'B'], props: {} },
      { name: 'scale', label: '大小调', type: 'select', component: Select, options: ['major', 'minor'], props: {} },
      { name: 'bpm', label: 'BPM', type: 'number', component: NumberInput, props: { min: 60, max: 240 } },
      { name: 'bars', label: '小节数', type: 'number', component: NumberInput, props: { min: 1, max: 64 } }
    ],
    example: { style: 'POP', key: 'C', scale: 'major', bpm: 120, bars: 8 },
    invoke: generateMusicTool
  },
  {
    code: 'vision.analyze', name: '视觉分析', description: '颜色直方图 + pHash 相似度', category: 'vision',
    fields: [
      { name: 'task', label: '任务', type: 'select', component: Select, options: ['analyze', 'compare'], props: {} },
      { name: 'imageBase64', label: '图片 (Base64)', type: 'input', component: Input, props: { type: 'textarea', rows: 3, placeholder: 'iVBORw0KGgo...' } }
    ],
    example: { task: 'analyze', imageBase64: '' },
    invoke: analyzeVision
  },
  {
    code: 'java.project.gen', name: 'Java 企业项目', description: '生成完整 Spring Boot 项目 ZIP, 含 Docker/K8s/SQL/运维', category: 'code',
    fields: [
      { name: 'projectName', label: '项目名', type: 'input', component: Input, props: { placeholder: 'minimax-erp' } },
      { name: 'version', label: '版本', type: 'input', component: Input, props: { placeholder: '1.0.0' } },
      { name: 'type', label: '项目类型', type: 'select', component: Select, options: ['spring-boot', 'vue', 'react', 'python-flask', 'node-express', 'html'], props: {} },
      { name: 'packageName', label: '包名', type: 'input', component: Input, props: { placeholder: 'com.minimax.erp' } },
      { name: 'database', label: '数据库', type: 'select', component: Select, options: ['mysql', 'postgresql', 'h2', 'none'], props: {} }
    ],
    example: { projectName: 'minimax-erp', version: '1.0.0', type: 'spring-boot', packageName: 'com.minimax.erp', database: 'mysql' },
    invoke: generateJavaProject,
    isProject: true
  }
]

const searchTool = ref('')
const currentTool = ref(null)
const inputParams = ref({})
const running = ref(false)
const result = ref(null)
const resultTab = ref('all')

const groupedTools = computed(() => {
  const filtered = searchTool.value
      ? toolDefs.filter(t => (t.name + t.description + t.code).toLowerCase().includes(searchTool.value.toLowerCase()))
      : toolDefs
  const map = {}
  for (const t of filtered) {
    if (!map[t.category]) map[t.category] = []
    map[t.category].push(t)
  }
  return map
})

const summaryFields = computed(() => {
  if (!result.value) return {}
  const r = result.value
  const s = {}
  for (const k of Object.keys(r).filter(k => !['success', 'costMs', 'imageBase64', 'midiBase64'].includes(k)).slice(0, 6)) {
    s[k] = r[k]
  }
  return s
})

watch(currentTool, (t) => {
  if (t) {
    inputParams.value = {}
    result.value = null
  }
})

function categoryLabel(c) {
  return { text: '📝 文本', data: '📊 数据', time: '⏰ 时间', file: '📁 文件', image: '🎨 图像', chart: '📈 图表', music: '🎵 音乐', vision: '👁️ 视觉' }[c] || c
}
function categoryColor(c) {
  return { text: '', data: 'success', time: 'warning', file: 'info', image: 'danger', chart: 'primary', music: '', vision: 'success' }[c] || ''
}

function selectTool(t) {
  currentTool.value = t
  inputParams.value = JSON.parse(JSON.stringify(t.example || {}))
  result.value = null
}

function loadExample() {
  if (currentTool.value) inputParams.value = JSON.parse(JSON.stringify(currentTool.value.example || {}))
}

async function invoke() {
  if (!currentTool.value) return
  running.value = true
  result.value = null
  try {
    // 处理 seriesJson -> series
    const params = { ...inputParams.value }
    if (params.seriesJson) {
      try { params.series = JSON.parse(params.seriesJson) } catch { ElMessage.error('series JSON 格式错误'); running.value = false; return }
      delete params.seriesJson
    }
    // 处理 columns 字符串
    if (typeof params.columns === 'string') {
      params.columns = params.columns.split(',').map(s => s.trim()).filter(Boolean)
    }
    // 处理 values 字符串 -> 数字数组
    if (typeof params.values === 'string') {
      params.values = params.values.split(',').map(s => parseFloat(s.trim())).filter(n => !isNaN(n))
    }
    const res = await currentTool.value.invoke(params)
    result.value = res.data || res
    if (result.value.success !== false) ElMessage.success('调用成功')
    else ElMessage.error(result.value.message || '调用失败')

    // Java 项目生成: 自动弹出下载
    if (currentTool.value.isProject && result.value.zipBase64) {
      downloadJavaProjectFromBase64(result.value.zipBase64, result.value.downloadName)
      ElMessage.success(`项目已生成 (${result.value.fileCount} 个文件, ${result.value.sizeKB}KB), 下载已开始`)
    }
  } catch (e) {
    result.value = { success: false, message: e.message || '调用失败' }
    ElMessage.error(e.message || '调用失败')
  } finally {
    running.value = false
  }
}

function downloadMidi() {
  if (!result.value?.midiBase64) return
  const bytes = Uint8Array.from(atob(result.value.midiBase64), c => c.charCodeAt(0))
  const blob = new Blob([bytes], { type: 'audio/midi' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `minimax-${result.value.style || 'music'}.mid`
  link.click()
  URL.revokeObjectURL(link.href)
}

selectTool(toolDefs[0])
</script>

<style scoped>
.tool-list-card { max-height: 720px; overflow-y: auto; }
.category { margin-bottom: 12px; }
.category-name {
  font-size: 12px;
  font-weight: 600;
  color: #909399;
  padding: 4px 0;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 6px;
}
.tool-item {
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: all 0.2s;
}
.tool-item:hover { background: #f5f7fa; }
.tool-item.active { background: #ecf5ff; border-left: 3px solid #409EFF; }
.tool-title { font-size: 13px; font-weight: 600; color: #303133; }
.tool-desc { font-size: 11px; color: #909399; margin-top: 2px; }
.action-row { display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px; }
.result-pre {
  background: #fafafa;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  max-height: 400px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
</style>
