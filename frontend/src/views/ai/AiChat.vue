<template>
  <div class="ai-chat">
    <el-card>
      <template #header>
        <div class="header">
          <span>🤖 AI 智能助手 (V2.7)</span>
          <el-tag size="small" type="success">自研 · 0 外部依赖</el-tag>
        </div>
      </template>

      <div class="quick-actions">
        <el-button-group>
          <el-button @click="fillExample('统计 user 表前 10 条, 柱状图')" size="small">📊 统计图表</el-button>
          <el-button @click="fillExample('生成 C 大调 120bpm 8 小节的音乐')" size="small">🎵 音乐生成</el-button>
          <el-button @click="fillExample('生成一个 Spring Boot 项目, 叫 demo')" size="small">💻 代码生成</el-button>
          <el-button @click="fillExample('转人工')" size="small">🙋 转人工</el-button>
          <el-button @click="fillExample('画一个产品销量饼图, 苹果香蕉橙子, 占比 50/30/20')" size="small">🥧 饼图</el-button>
        </el-button-group>
      </div>

      <el-input
        v-model="userInput"
        type="textarea"
        :rows="3"
        placeholder="试试输入: 画一个统计 user 表的柱状图 / 生成一段 8 小节 C 大调音乐 / 转人工 / 生成 Spring Boot 项目"
        @keydown.ctrl.enter="handleSend"
      />
      <div class="actions">
        <el-button type="primary" :loading="loading" @click="handleSend">
          🚀 智能路由 ({{ shortcut }})
        </el-button>
        <el-button @click="clearAll">清空</el-button>
      </div>

      <div v-if="lastResult" class="result">
        <el-alert
          :type="lastResult.intent === 'UNKNOWN' ? 'warning' : 'success'"
          :closable="false"
          show-icon
        >
          <template #title>
            意图识别: <b>{{ lastResult.intent }}</b>
            <span style="margin-left: 12px">处理函数: <code>{{ lastResult.handler }}</code></span>
          </template>
        </el-alert>

        <div v-if="lastResult.params && Object.keys(lastResult.params).length" class="params">
          <h4>提取的参数:</h4>
          <el-tag v-for="(v, k) in lastResult.params" :key="k" style="margin: 4px">
            {{ k }} = {{ v }}
          </el-tag>
        </div>

        <!-- 图表预览 -->
        <div v-if="chartUrl" class="chart-preview">
          <h4>📊 图表预览</h4>
          <img :src="chartUrl" alt="chart" />
        </div>

        <!-- 音乐预览 -->
        <div v-if="musicUrl" class="music-preview">
          <h4>🎵 音乐生成结果</h4>
          <audio :src="musicUrl" controls></audio>
          <el-link :href="musicUrl" download="music.mid">下载 MIDI</el-link>
        </div>

        <!-- 错误 -->
        <el-alert v-if="lastResult.error" :title="lastResult.error" type="error" :closable="false" />
      </div>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>📚 支持的意图</template>
      <el-table :data="intents" stripe size="small">
        <el-table-column prop="intent" label="意图" width="200" />
        <el-table-column prop="keywords" label="触发关键词" />
        <el-table-column prop="handler" label="处理函数" width="200" />
        <el-table-column prop="api" label="对应接口" width="260" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { dispatchPrompt, renderChart, generateMusic, nl2chart } from '@/api/ai'

const userInput = ref('')
const loading = ref(false)
const lastResult = ref(null)
const chartUrl = ref('')
const musicUrl = ref('')
const shortcut = ref('Ctrl+Enter')

const intents = [
  { intent: 'GENERATE_CHART', keywords: '图表, 柱状图, 折线图, 饼图, 雷达图, 热力图, 散点图, 桑基图, chart, graph, bar, line, pie, radar', handler: 'ChartGenerator.render', api: '/api/ai/chart/render' },
  { intent: 'GENERATE_MUSIC', keywords: '音乐, 旋律, 曲子, MIDI, 作曲, 和弦, 节拍, music, melody, song', handler: 'MusicGenerator.generate', api: '/api/ai/music/generate' },
  { intent: 'GENERATE_ANIMATION', keywords: '动画, GIF, 动图, 进度条动画, 过渡动画, animation', handler: 'AnimationGenerator.generate', api: '/api/ai/animation/*' },
  { intent: 'GENERATE_CODE', keywords: '代码, Spring Boot, Vue, React, Python, Flask, code, scaffold', handler: 'ProjectCodeGenerator.generate', api: '/api/ai/admin/codegen' },
  { intent: 'QUERY_DATA', keywords: '查询, SELECT, FROM, SQL, 数据, 记录, 表, query, select', handler: 'Nl2SqlTool.execute', api: '/api/ai/admin/tools/sql.query/invoke' },
  { intent: 'ANALYZE_DATA', keywords: '统计, 平均, 求和, 最大, 最小, 分组, 趋势, 异常, analyze', handler: 'DataAnalyzer.execute', api: '/api/ai/admin/tools/data.analyze.stats/invoke' },
  { intent: 'TTS', keywords: '朗读, 语音播报, TTS, 读出来, 发声, 合成语音', handler: 'AudioAnalyzer.synthesize', api: '/api/ai/multimodal/tts' },
  { intent: 'STT', keywords: '听写, 语音识别, STT, transcribe', handler: 'AudioAnalyzer.transcribe', api: '/api/ai/multimodal/audio/upload' },
  { intent: 'TRANSFER_HUMAN', keywords: '转人工, 真人, 人工客服, 转接, human, agent', handler: 'TransferToHumanEvent', api: '/api/ai/route (transfer event)' },
  { intent: 'IMAGE_ANALYZE', keywords: '分析图片, 看图, 识别图片, analyze image', handler: 'ImageAnalyzer.analyze', api: '/api/ai/multimodal/image/upload' },
  { intent: 'CHAT', keywords: '你好, 请问, 什么是, 怎么, 如何, hello, hi, what, how', handler: 'TextGenerator.generate', api: '/api/ai/generate' }
]

function fillExample(text) {
  userInput.value = text
}

function clearAll() {
  userInput.value = ''
  lastResult.value = null
  chartUrl.value = ''
  musicUrl.value = ''
}

async function handleSend() {
  if (!userInput.value.trim()) {
    ElMessage.warning('请输入提示词')
    return
  }
  loading.value = true
  chartUrl.value = ''
  musicUrl.value = ''
  try {
    // 1. 智能分发
    const res = await dispatchPrompt({ text: userInput.value })
    lastResult.value = res.data

    // 2. 如果是图表, 自动生成预览
    if (res.data.intent === 'GENERATE_CHART') {
      // 简单示例数据
      const chartData = {
        type: getChartType(userInput.value),
        title: '示例图表',
        categories: ['A', 'B', 'C', 'D', 'E'],
        series: [{ name: '数值', values: [12, 25, 18, 30, 22] }]
      }
      try {
        const chart = await renderChart(chartData)
        chartUrl.value = chart.blobUrl
      } catch (e) {
        console.warn('Chart render failed', e)
      }
    } else if (res.data.intent === 'GENERATE_MUSIC') {
      // 解析音乐配置
      const music = await generateMusic({
        key: 'C',
        bpm: 120,
        bars: 4,
        style: 'pop'
      })
      musicUrl.value = music.blobUrl
    }
  } catch (e) {
    ElMessage.error('处理失败: ' + (e.message || '未知错误'))
    lastResult.value = { intent: 'ERROR', error: e.message }
  } finally {
    loading.value = false
  }
}

function getChartType(text) {
  if (text.includes('饼图')) return 'PIE'
  if (text.includes('折线图') || text.includes('趋势')) return 'LINE'
  if (text.includes('雷达图')) return 'RADAR'
  if (text.includes('热力图')) return 'HEATMAP'
  if (text.includes('散点图')) return 'SCATTER'
  if (text.includes('桑基图')) return 'SANKEY'
  return 'BAR'
}
</script>

<style scoped>
.ai-chat {
  padding: 16px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.quick-actions {
  margin-bottom: 12px;
}
.actions {
  margin-top: 12px;
  text-align: right;
}
.result {
  margin-top: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}
.params {
  margin-top: 12px;
  padding: 8px;
  background: #fff;
  border-radius: 4px;
}
.params h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
}
.chart-preview img {
  max-width: 100%;
  margin-top: 8px;
  border: 1px solid #eee;
  border-radius: 4px;
}
.music-preview audio {
  width: 100%;
  margin-top: 8px;
}
</style>
