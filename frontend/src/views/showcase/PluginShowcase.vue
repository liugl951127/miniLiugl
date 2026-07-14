<!--
  插件 SDK (V4.1)
  - 4 类插件: class / url / js / wasm
  - JS 沙箱执行用户上传的插件代码 (Function 构造器)
  - 真实 class 插件调后端 /api/v1/agent/plugins/exec
  - 模板市场 (4 个示例插件)
-->
<template>
  <div class="plugin-sdk">
    <header class="header">
      <h1>🔌 {{ t('showcase.pluginSDK') }}</h1>
      <p class="subtitle">{{ t('showcase.pluginSDKSubtitle') }}</p>
      <div class="badges">
        <span class="badge">Class Plugin</span>
        <span class="badge">URL Plugin</span>
        <span class="badge">JS {{ t('showcase.sandbox') }}</span>
        <span class="badge">WASM ({{ t('showcase.reserved') }})</span>
      </div>
    </header>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- 模板市场 -->
      <el-tab-pane :label="'🛒 ' + t('plugins.market')" name="market">
        <div class="market-grid">
          <div v-for="p in marketPlugins" :key="p.code" class="market-card">
            <div class="mc-icon">{{ p.icon }}</div>
            <h3>{{ p.name }}</h3>
            <p>{{ p.desc }}</p>
            <div class="mc-tags">
              <el-tag size="small" :type="p.type === 'js' ? 'warning' : p.type === 'url' ? 'info' : 'success'">
                {{ p.type }}
              </el-tag>
              <el-tag size="small">{{ p.uses }} 使用</el-tag>
              <el-tag size="small" type="success">★ {{ p.rating.toFixed(1) }}</el-tag>
            </div>
            <el-button type="primary" plain size="small" @click="useTemplate(p)">
              {{ activeTemplate === p.code ? '✓ ' + t('plugins.selected') : t('plugins.useTemplate') }}
            </el-button>
          </div>
        </div>
      </el-tab-pane>

      <!-- JS 沙箱测试 -->
      <el-tab-pane :label="'🧪 ' + t('plugins.jsSandboxTest')" name="js">
        <div class="js-grid">
          <section class="editor-side">
            <h3>{{ t('plugins.pluginCode') }}</h3>
            <el-input v-model="pluginCode" type="textarea" :rows="14"
                      :placeholder="`// 编写插件代码, 必须导出 execute 函数:
function execute(input) {
  // input 形如 { user: 'admin', message: '...', ctx: {} }
  return {
    success: true,
    output: 'Hello, ' + input.user
  };
}`"
                      style="font-family: 'SF Mono', Menlo, monospace; font-size: 13px" />
            <div class="form-item">
              <label>{{ t('plugins.testInput') }}</label>
              <el-input v-model="testInput" type="textarea" :rows="4" />
            </div>
            <el-button type="primary" :loading="executing" @click="executePlugin" block size="large">
              {{ executing ? t('plugins.executing') + '...' : '▶ ' + t('plugins.executePlugin') }}
            </el-button>
          </section>
          <section class="output-side">
            <h3>{{ t('plugins.execResult') }}</h3>
            <div v-if="execResult" class="exec-result">
              <div class="exec-status">
                <el-tag :type="execResult.success ? 'success' : 'danger'">
                  {{ execResult.success ? '✓ 成功' : '✗ 失败' }}
                </el-tag>
                <span>耗时: {{ execResult.latencyMs }}ms</span>
              </div>
              <h4>{{ t('common.output') }}</h4>
              <pre>{{ JSON.stringify(execResult.output, null, 2) }}</pre>
              <div v-if="execResult.logs && execResult.logs.length" class="logs">
                <h4>{{ t('common.logs') }}</h4>
                <div v-for="(l, i) in execResult.logs" :key="i" class="log-line">
                  <span class="log-time">[{{ l.time }}]</span>
                  <span :class="`log-${l.level}`">[{{ l.level }}]</span>
                  <span>{{ l.msg }}</span>
                </div>
              </div>
              <div v-if="execResult.error" class="error">
                <h4>{{ t('common.error') }}</h4>
                <pre>{{ execResult.error }}</pre>
              </div>
            </div>
            <el-empty v-else :description="t('plugins.clickToExecute')" />
          </section>
        </div>
      </el-tab-pane>

      <!-- Class 插件 (后端) -->
      <el-tab-pane :label="'☕ ' + t('plugins.classPlugin')" name="class">
        <h3>{{ t('plugins.classPluginDesc') }}</h3>
        <el-table :data="classPlugins" stripe>
          <el-table-column prop="code" label="插件 code" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="description" label="描述" />
          <el-table-column :label="t('common.call')" width="200">
            <template #default="{ row }">
              <el-button size="small" @click="callClassPlugin(row)">{{ t('common.test') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="classResult" class="class-result">
          <h4>{{ t('plugins.execResult') }}</h4>
          <pre>{{ JSON.stringify(classResult, null, 2) }}</pre>
        </div>
      </el-tab-pane>

      <!-- 上传我的插件 -->
      <el-tab-pane :label="'📤 ' + t('plugins.uploadMyPlugin')" name="upload">
        <el-form :model="uploadForm" label-width="120px">
          <el-form-item :label="t('plugins.pluginName')">
            <el-input v-model="uploadForm.name" :placeholder="t('plugins.pluginNamePlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('plugins.pluginCode')">
            <el-input v-model="uploadForm.code" :placeholder="t('plugins.pluginCodePlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('plugins.type')">
            <el-select v-model="uploadForm.type" style="width: 100%">
              <el-option :label="t('plugins.jsSandboxType')" value="js" />
              <el-option :label="t('plugins.urlType')" value="url" />
              <el-option :label="t('plugins.classType')" value="class" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('common.description')">
            <el-input v-model="uploadForm.desc" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item :label="t('plugins.codeOrUrl')">
            <el-input v-model="uploadForm.content" type="textarea" :rows="10"
                      :placeholder="uploadForm.type === 'url' ? 'https://example.com/api' : 'function execute(input) { ... }'" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submitPlugin">{{ t('plugins.publishPlugin') }}</el-button>
            <el-button @click="uploadForm = { name:'', code:'', type:'js', desc:'', content:'' }">{{ t('common.reset') }}</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '@/api/http'
import { t } from '@/i18n'

const activeTab = ref('market')
const activeTemplate = ref(null)

// ===== 模板市场 =====
const marketPlugins = [
  {
    code: 'weather-widget', name: '🌤 天气小组件', icon: '☀️',
    desc: '输入城市名, 显示实时天气',
    type: 'url', uses: 1234, rating: 4.7,
    code_body: 'fetch("https://wttr.in/" + encodeURIComponent(input.city) + "?format=j1")\n  .then(r => r.json())\n  .then(d => ({ temp: d.current_condition[0].temp_C, desc: d.current_condition[0].weatherDesc[0].value }));'
  },
  {
    code: 'markdown-export', name: '📝 Markdown 导出', icon: '📄',
    desc: '把当前会话导出成 .md 文件',
    type: 'js', uses: 856, rating: 4.5,
    code_body: 'function execute(input) { return { content: "# " + input.title + "\\n\\n" + input.messages.map(m => "**" + m.role + "**: " + m.content).join("\\n\\n") }; }'
  },
  {
    code: 'code-formatter', name: '🎨 代码格式化', icon: '🎨',
    desc: '把代码块用 prettier 风格格式化',
    type: 'js', uses: 567, rating: 4.6,
    code_body: 'function execute(input) { const formatted = input.code.replace(/\\t/g, "  ").replace(/\\s+\\n/g, "\\n"); return { formatted }; }'
  },
  {
    code: 'translator', name: '🌐 中英互译', icon: '🌐',
    desc: '调用硅基流动翻译接口',
    type: 'url', uses: 432, rating: 4.4,
    code_body: 'POST https://api.siliconflow.cn/v1/chat/completions\\n{ model: "Qwen/Qwen2.5-72B-Instruct", messages: [...] }'
  },
]

function useTemplate(p) {
  activeTemplate.value = p.code
  activeTab.value = 'js'
  pluginCode.value = p.code_body
  testInput.value = JSON.stringify({ user: 'admin', message: 'Hello from template: ' + p.name }, null, 2)
  ElMessage.success(t('plugins.templateLoaded') + p.name)
}

// ===== JS 沙箱 =====
const pluginCode = ref(`function execute(input) {
  return {
    success: true,
    output: {
      greeting: 'Hello, ' + input.user,
      length: input.message.length,
      upper: input.message.toUpperCase()
    }
  };
}`)
const testInput = ref(JSON.stringify({
  user: 'adminLiugl',
  message: 'Liugl-AI 平台真棒',
  ctx: { time: Date.now() }
}, null, 2))
const executing = ref(false)
const execResult = ref(null)

async function executePlugin() {
  executing.value = true
  execResult.value = null
  const t0 = Date.now()
  try {
    // 解析插件代码
    const fn = new Function('input', 'console', `
      const logs = [];
      const _log = (level, msg) => logs.push({ level, msg, time: new Date().toISOString().slice(11,19) });
      const console = { log: (...a) => _log('info', a.join(' ')), warn: (...a) => _log('warn', a.join(' ')), error: (...a) => _log('error', a.join(' ')) };
      try {
        ${pluginCode.value}
        const result = execute(${testInput.value});
        return { success: true, output: result, logs };
      } catch (e) {
        return { success: false, error: e.message, logs };
      }
    `)
    const input = JSON.parse(testInput.value)
    const result = fn(input)
    execResult.value = {
      ...result,
      latencyMs: Date.now() - t0
    }
    ElMessage.success(result.success ? t('plugins.execSuccess') : t('plugins.execFailed'))
  } catch (e) {
    execResult.value = { success: false, error: e.message, logs: [], latencyMs: Date.now() - t0 }
    ElMessage.error(t('plugins.execFailed') + e.message)
  } finally {
    executing.value = false
  }
}

// ===== Class 插件 =====
const classPlugins = [
  { code: 'calculator', name: '计算器', description: '四则运算: 1+2*3' },
  { code: 'time', name: '时间', description: '返回当前时间' },
  { code: 'http_get', name: 'HTTP GET', description: 'HTTP 请求' },
  { code: 'random', name: '随机数', description: 'min-max 随机整数' },
]
const classResult = ref(null)

async function callClassPlugin(p) {
  try {
    let input = { expr: '123 * 456 - 789' }
    if (p.code === 'time') input = { tz: 'Asia/Shanghai' }
    if (p.code === 'random') input = { min: 1, max: 100 }
    if (p.code === 'http_get') input = { url: 'https://api.github.com/zen' }

    const r = await http.post(`/api/v1/agent/plugins/${p.code}/call`, input)
    classResult.value = r.data || r
    ElMessage.success(t('plugins.callSuccess'))
  } catch (e) {
    classResult.value = { error: e.message }
    ElMessage.error(t('plugins.callFailed') + e.message)
  }
}

// ===== 上传 =====
const uploadForm = ref({ name: '', code: '', type: 'js', desc: '', content: '' })
async function submitPlugin() {
  if (!uploadForm.value.name || !uploadForm.value.code) {
    ElMessage.warning(t('common.fillComplete'))
    return
  }
  // 简化: 存 localStorage (实际生产写后端)
  const list = JSON.parse(localStorage.getItem('minimax_my_plugins') || '[]')
  list.push({ ...uploadForm.value, createdAt: new Date().toISOString() })
  localStorage.setItem('minimax_my_plugins', JSON.stringify(list))
  ElMessage.success(t('plugins.publishedToMy'))
  uploadForm.value = { name: '', code: '', type: 'js', desc: '', content: '' }
}
</script>

<style scoped>
.plugin-sdk { max-width: 1400px; margin: 0 auto; padding: 24px; }
.header h1 { font-size: 32px; margin: 0 0 8px; }
.subtitle { color: #64748b; margin: 0 0 12px; }
.badge { display: inline-block; padding: 2px 10px; margin-right: 6px;
  background: linear-gradient(135deg, #06b6d4, #0891b2); color: #fff; border-radius: 12px; font-size: 12px; }
.market-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; margin-top: 16px; }
.market-card { background: #fff; border-radius: 12px; padding: 20px; text-align: center;
  border: 2px solid #e2e8f0; transition: all .15s; }
.market-card:hover { border-color: #06b6d4; transform: translateY(-2px); }
.mc-icon { font-size: 48px; margin-bottom: 8px; }
.market-card h3 { margin: 8px 0; }
.market-card p { color: #64748b; font-size: 13px; margin: 8px 0 12px; min-height: 40px; }
.mc-tags { display: flex; gap: 6px; justify-content: center; margin-bottom: 12px; flex-wrap: wrap; }

.js-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-top: 16px; }
.editor-side, .output-side { background: #f8fafc; border-radius: 12px; padding: 20px; }
h3 { margin: 0 0 16px; }
.form-item { margin: 16px 0; }
label { display: block; font-weight: 600; margin-bottom: 6px; color: #334155; font-size: 13px; }
.exec-result { background: #fff; padding: 16px; border-radius: 8px; }
.exec-status { display: flex; gap: 12px; align-items: center; margin-bottom: 12px; }
.exec-result h4 { margin: 12px 0 8px; }
.exec-result pre { background: #0f172a; color: #e2e8f0; padding: 12px; border-radius: 6px;
  font-family: 'SF Mono', monospace; font-size: 12px; max-height: 240px; overflow-y: auto;
  white-space: pre-wrap; word-break: break-word; margin: 0; }
.logs { margin-top: 12px; }
.log-line { padding: 4px 0; font-family: monospace; font-size: 12px; border-bottom: 1px solid #f1f5f9; }
.log-time { color: #64748b; margin-right: 8px; }
.log-info { color: #0ea5e9; margin-right: 8px; }
.log-warn { color: #f59e0b; margin-right: 8px; }
.log-error { color: #ef4444; margin-right: 8px; }
.error pre { background: #fee2e2; color: #b91c1c; }
.class-result { background: #0f172a; color: #e2e8f0; padding: 16px; border-radius: 8px; margin-top: 16px; }
.class-result pre { margin: 0; font-family: monospace; font-size: 13px; }
</style>
