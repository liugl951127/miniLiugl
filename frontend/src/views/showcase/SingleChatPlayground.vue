<!--
  实时单模型 PlayGround (V4)
  - 选模型 + 写 prompt → 立即流式调用
  - 显示延迟/token/原始响应
  - 调 raw HTTP, 无中间层
-->
<!--
  @file views/showcase/SingleChatPlayground.vue (AI 聊天对话 (SingleChatPlayground))
  @version V3.5.12+ (前端注释补全)
  @description AI 聊天对话 (SingleChatPlayground)
-->
<template>
  <div class="playground">
    <header class="header">
      <h1>⚡ 单模型 PlayGround</h1>
      <p class="subtitle">直接打 model 服务, 验证每个模型真实可用</p>
    </header>

    <div class="pg-grid">
      <!-- 左侧: 配置 -->
      <aside class="pg-config">
        <h3>配置</h3>

        <div class="form-item">
          <label>模型</label>
          <el-select v-model="form.model" placeholder="选择模型" filterable>
            <el-option-group label="OpenAI">
              <el-option label="GPT-4o" value="gpt-4o" />
              <el-option label="GPT-4o mini" value="gpt-4o-mini" />
            </el-option-group>
            <el-option-group label="Minimax-M3">
              <el-option label="Liugl-AI-Text-01" value="Liugl-AI-Text-01" />
              <el-option label="Liugl-AI-VL-01" value="Liugl-AI-VL-01" />
            </el-option-group>
            <el-option-group label="SiliconFlow">
              <el-option label="Qwen2.5 72B" value="Qwen/Qwen2.5-72B-Instruct" />
              <el-option label="DeepSeek V3" value="deepseek-ai/DeepSeek-V3" />
            </el-option-group>
            <el-option-group label="DeepSeek">
              <el-option label="DeepSeek V3" value="deepseek-chat" />
              <el-option label="DeepSeek R1 推理" value="deepseek-reasoner" />
            </el-option-group>
            <el-option-group label="Ollama (本地)">
              <el-option label="Llama 3 8B" value="llama3:8b" />
              <el-option label="Qwen 2.5 7B" value="qwen2.5:7b" />
            </el-option-group>
            <el-option-group label="Mock (无 key 演示)">
              <el-option label="Mock Adapter" value="mock" />
            </el-option-group>
          </el-select>
        </div>

        <div class="form-item">
          <label>系统 Prompt (可选)</label>
          <el-input v-model="form.systemPrompt" type="textarea" :rows="3" placeholder="你是一个 {角色}" />
        </div>

        <div class="form-item">
          <label>用户 Prompt</label>
          <el-input v-model="form.prompt" type="textarea" :rows="6" placeholder="输入要问的问题" />
        </div>

        <div class="form-row">
          <div class="form-item half">
            <label>Temperature: {{ form.temperature }}</label>
            <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" />
          </div>
          <div class="form-item half">
            <label>Max Tokens: {{ form.maxTokens }}</label>
            <el-slider v-model="form.maxTokens" :min="64" :max="4096" :step="64" />
          </div>
        </div>

        <div class="form-item">
          <el-button type="primary" :loading="running" :icon="Promotion" :disabled="!form.prompt.trim()" @click="run" block>
            {{ running ? '⚡ 正在调用' : '⚡ 立即调用' }}
          </el-button>
          <el-button v-if="running" type="warning" :icon="VideoPause" @click="stop" block style="margin-top: 8px">
            停止
          </el-button>
        </div>

        <div class="form-item">
          <el-button text @click="fillExample(1)">📝 例1: 自我介绍</el-button>
          <el-button text @click="fillExample(2)">📝 例2: 代码生成</el-button>
          <el-button text @click="fillExample(3)">📝 例3: 翻译</el-button>
          <el-button text @click="fillExample(4)">📝 例4: 推理</el-button>
        </div>
      </aside>

      <!-- 右侧: 响应 -->
      <main class="pg-result">
        <div class="result-header">
          <h3>响应</h3>
          <div class="meta">
            <el-tag v-if="result.status === 'pending'" type="info">⏳ 调用中…</el-tag>
            <el-tag v-else-if="result.status === 'ok'" type="success">✓ 成功</el-tag>
            <el-tag v-else-if="result.status === 'error'" type="danger">✗ 失败</el-tag>
            <el-tag v-else type="info">就绪</el-tag>
            <span v-if="result.latencyMs" class="meta-text">延迟: {{ result.latencyMs }}ms</span>
            <span v-if="result.totalTokens" class="meta-text">总: {{ result.totalTokens }} tokens</span>
            <span v-if="result.finishReason" class="meta-text">结束: {{ result.finishReason }}</span>
          </div>
        </div>

        <div class="result-body">
          <pre v-if="result.content">{{ result.content }}<span v-if="streaming" class="cursor">▊</span></pre>
          <el-empty v-else description="点 ⚡ 立即调用 发起请求" />
        </div>

        <details v-if="result.raw" class="result-raw">
          <summary>📦 原始响应 (raw JSON)</summary>
          <pre>{{ JSON.stringify(result.raw, null, 2) }}</pre>
        </details>

        <details v-if="result.error" class="result-error">
          <summary>❌ 错误</summary>
          <pre>{{ result.error }}</pre>
        </details>
      </main>
    </div>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Promotion, VideoPause } from '@element-plus/icons-vue'
import http from '@/api/http'

const form = reactive({
  model: 'mock',
  systemPrompt: '',
  prompt: '你好, 请用一句话介绍你自己, 包含名字和能力',
  temperature: 0.7,
  maxTokens: 1024,
})

const running = ref(false)
const streaming = ref(false)
const abortCtrl = ref(null)
const result = reactive({
  status: 'idle', content: '', error: '', latencyMs: 0, totalTokens: 0, finishReason: '',
  promptTokens: 0, completionTokens: 0, raw: null,
})

const examples = {
  1: { systemPrompt: '你是一个友好的助手, 叫 Liugl-AI, 由 Liugl-AI 公司开发', prompt: '你好, 请介绍一下你自己' },
  2: { systemPrompt: '你是一个资深 Python 工程师', prompt: '写一个装饰器, 统计函数执行耗时, 支持异步函数' },
  3: { systemPrompt: '你是专业翻译', prompt: '把下面这段话翻译成英文: "大模型平台是企业 AI 转型的核心基础设施, 决定了上层应用的天花板"' },
  4: { systemPrompt: '你是一个逻辑推理专家, 一步步思考', prompt: '一个房间里, 有 3 个红帽子, 2 个蓝帽子. 3 个人依次进去戴帽子出来, 每个人能看到前面人的帽子. 已知他们看到的和说的话, 推理他们各戴的什么.' },
}
function fillExample(i) {
  const ex = examples[i]
  form.systemPrompt = ex.systemPrompt
  form.prompt = ex.prompt
}

async function run() {
  if (!form.prompt.trim()) return
  running.value = true
  streaming.value = true
  result.status = 'pending'
  result.content = ''
  result.error = ''
  result.latencyMs = 0
  result.totalTokens = 0
  result.finishReason = ''
  result.raw = null

  const t0 = Date.now()

  // 用 /api/v1/test/single (非流式, 简单可靠)
  try {
    const r = await http.post('/api/v1/test/single', {
      model: form.model,
      prompt: form.prompt,
      systemPrompt: form.systemPrompt || undefined,
      temperature: form.temperature,
      maxTokens: form.maxTokens,
    })
    if (r && r.data && r.data.content !== undefined) {
      result.status = 'ok'
      result.content = r.data.content
      result.latencyMs = r.data.latencyMs || (Date.now() - t0)
      result.promptTokens = r.data.promptTokens
      result.completionTokens = r.data.completionTokens
      result.totalTokens = r.data.totalTokens
      result.finishReason = r.data.finishReason
      result.raw = r.data.raw || null
      ElMessage.success(`调用成功! ${result.latencyMs}ms / ${result.totalTokens} tokens`)
    } else {
      throw new Error('响应无 content 字段: ' + JSON.stringify(r))
    }
  } catch (e) {
    result.status = 'error'
    result.error = e.message
    ElMessage.error('调用失败: ' + e.message)
  } finally {
    running.value = false
    streaming.value = false
  }
}

function stop() {
  if (abortCtrl.value) abortCtrl.value.abort()
  running.value = false
  streaming.value = false
  result.status = 'idle'
  result.content += '\n[已停止]'
}
</script>

<style scoped>
.playground { max-width: 1400px; margin: 0 auto; padding: 24px; }
.header h1 { font-size: 28px; margin: 0 0 4px; }
.subtitle { color: #64748b; margin: 0; }

.pg-grid {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 20px;
  margin-top: 20px;
}

.pg-config, .pg-result {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,.1);
}
.pg-config h3, .pg-result h3 { margin: 0 0 16px; font-size: 16px; }

.form-item { margin-bottom: 14px; }
.form-row { display: flex; gap: 12px; }
.form-item.half { flex: 1; }
label { display: block; font-size: 13px; font-weight: 600; margin-bottom: 6px; color: #334155; }

.result-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #e2e8f0; }
.result-header h3 { margin: 0; flex: 1; }
.meta { display: flex; align-items: center; gap: 8px; }
.meta-text { font-size: 12px; color: #64748b; }

.result-body {
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  min-height: 400px;
  max-height: 600px;
  overflow-y: auto;
}
.result-body pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  font-size: 13px;
  line-height: 1.7;
}
.cursor { animation: blink 1s step-start infinite; color: #fbbf24; }
@keyframes blink { 50% { opacity: 0; } }

.result-raw, .result-error {
  margin-top: 12px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
  font-size: 12px;
}
.result-raw summary, .result-error summary { cursor: pointer; font-weight: 600; }
.result-raw pre, .result-error pre { max-height: 240px; overflow-y: auto; }
</style>
