<!--
  @file multimodal/LocalLlm.vue - 本地语言智能 (V7.6)
  路由: /multimodal/local/llm
  包含: BGE embedding + Qwen2.5 对话
-->
<template>
  <div class="local-llm">
    <el-tabs v-model="activeTab" class="feature-tabs">
      <el-tab-pane label="文本 Embedding (BGE)" name="bge">
        <el-form>
          <el-form-item label="输入文本 (一行一条)">
            <el-input v-model="bgeInput" type="textarea" :rows="6"
              placeholder="例如:&#10;今天天气真好&#10;人工智能改变世界" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading.bge" @click="runEmbed">计算</el-button>
          </el-form-item>
        </el-form>
        <div v-if="bgeResult" class="bge-result">
          <h3>{{ bgeResult.length }} 个, {{ bgeDim }} 维</h3>
          <el-table :data="bgeResult" stripe>
            <el-table-column prop="index" label="#" width="60" />
            <el-table-column prop="text" label="文本" />
            <el-table-column label="向量 (前 5 维)">
              <template #default="{ row }">
                <code class="vec">[{{ row.vector.slice(0, 5).map(v => v.toFixed(3)).join(', ') }}...]</code>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="Qwen2.5 对话" name="qwen">
        <el-form>
          <el-form-item label="系统 Prompt">
            <el-input v-model="qwenSystem" placeholder="你是 MiniMax 智能助手" />
          </el-form-item>
          <el-form-item label="用户输入">
            <el-input v-model="qwenInput" type="textarea" :rows="3" placeholder="介绍一下你自己" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading.qwen" @click="runChat">发送</el-button>
          </el-form-item>
        </el-form>
        <div v-if="qwenResult" class="qwen-result">
          <h3>回复 <el-tag size="small">{{ qwenResult.costMs }}ms</el-tag></h3>
          <el-input v-model="qwenResult.text" type="textarea" :rows="6" readonly />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { multimodalApi } from '@/api/multimodal'

const activeTab = ref('bge')
const bgeInput = ref('今天天气真好\n人工智能改变世界\n深度学习是机器学习的一个分支')
const bgeResult = ref(null)
const bgeDim = ref(0)
const qwenSystem = ref('你是 MiniMax 智能助手')
const qwenInput = ref('用一句话介绍你自己')
const qwenResult = ref(null)
const loading = reactive({ bge: false, qwen: false })

async function runEmbed() {
  const texts = bgeInput.value.split('\n').map(s => s.trim()).filter(Boolean)
  if (!texts.length) return ElMessage.warning('请输入文本')
  loading.bge = true
  try {
    const res = await multimodalApi.embedText(texts)
    if (res.code === 0) {
      bgeResult.value = res.data
      bgeDim.value = res.data[0]?.dim || 0
    } else if (res.code === 1001) ElMessage.warning(res.message)
    else ElMessage.error(res.message)
  } finally { loading.bge = false }
}

async function runChat() {
  if (!qwenInput.value.trim()) return ElMessage.warning('请输入内容')
  loading.qwen = true
  try {
    const res = await multimodalApi.chatQwen(qwenInput.value, qwenSystem.value || null)
    if (res.code === 0) qwenResult.value = res.data
    else if (res.code === 1001) ElMessage.warning(res.message)
    else ElMessage.error(res.message)
  } finally { loading.qwen = false }
}
</script>

<style scoped>
.local-llm { background: white; border-radius: 12px; padding: 16px; }
.feature-tabs { background: transparent; }
.bge-result, .qwen-result { margin-top: 16px; }
h3 { margin: 0 0 12px; color: #1e293b; }
.vec { font-size: 0.8em; color: #475569; }
</style>
