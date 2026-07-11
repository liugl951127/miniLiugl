<template>
  <div class="market-page">
    <div class="header">
      <h2>🛒 AI 工具市场</h2>
      <p class="sub">V2.7.5 · 9 工具 · 持续更新</p>
    </div>

    <van-tabs v-model:active="activeTab" sticky animated>
      <van-tab title="数据清洗" name="clean">
        <ToolCard v-for="t in categoryTools('data.clean')" :key="t.code" :tool="t" />
      </van-tab>
      <van-tab title="数据分析" name="analyze">
        <ToolCard v-for="t in categoryTools('data.analyze')" :key="t.code" :tool="t" />
      </van-tab>
      <van-tab title="代码生成" name="codegen">
        <ToolCard v-for="t in categoryTools('code.gen')" :key="t.code" :tool="t" />
      </van-tab>
      <van-tab title="对话助手" name="chat">
        <ToolCard v-for="t in categoryTools('chat')" :key="t.code" :tool="t" />
      </van-tab>
    </van-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, h, defineComponent } from 'vue'
import { showToast } from 'vant'

const activeTab = ref('analyze')

const tools = [
  { code: 'data.clean.missing', category: 'data.clean', name: '缺失值处理', desc: '均值/中位数/众数填充或删除', tag: '免费' },
  { code: 'data.clean.deduplicate', category: 'data.clean', name: '去重', desc: '按主键去重, HashMap 内存索引', tag: '免费' },
  { code: 'data.analyze.stats', category: 'data.analyze', name: '基础统计', desc: '均值/方差/分位数/极值', tag: '免费' },
  { code: 'data.analyze.trend', category: 'data.analyze', name: '趋势分析', desc: '线性回归 + R² 拟合优度', tag: 'PRO' },
  { code: 'data.analyze.anomaly', category: 'data.analyze', name: '异常检测', desc: 'Z-Score / IQR / 移动平均', tag: 'PRO' },
  { code: 'data.analyze.distribution', category: 'data.analyze', name: '分布分析', desc: '直方图 + 偏度 + 峰度', tag: 'PRO' },
  { code: 'sql.query', category: 'data.analyze', name: 'NL2SQL', desc: '自然语言转 SQL 查询', tag: '免费' },
  { code: 'code.gen.from-schema', category: 'code.gen', name: '代码生成', desc: '数据库表 → Spring Boot 项目 ZIP', tag: 'PRO' },
  { code: 'chat.assistant', category: 'chat', name: '对话助手', desc: '通用 AI 对话, 多轮记忆', tag: '免费' }
]

function categoryTools(cat: string) {
  return tools.filter(t => t.category === cat)
}

const ToolCard = defineComponent({
  props: ['tool'],
  setup(props) {
    return () => h('div', { class: 'tool-card', onClick: () => showToast({ message: '启动: ' + props.tool.name, position: 'bottom' }) }, [
      h('div', { class: 'tool-icon' }, props.tool.name.substring(0, 1)),
      h('div', { class: 'tool-body' }, [
        h('div', { class: 'tool-name' }, [
          h('span', props.tool.name),
          h('van-tag', { plain: true, type: props.tool.tag === 'PRO' ? 'danger' : 'success', size: 'small' }, () => props.tool.tag)
        ]),
        h('div', { class: 'tool-desc' }, props.tool.desc),
        h('div', { class: 'tool-code' }, props.tool.code)
      ])
    ])
  }
})
</script>

<style scoped>
.market-page { padding-bottom: 20px; }
.header { padding: 16px 16px 8px; background: linear-gradient(135deg, #667eea, #764ba2); color: white; }
.header h2 { margin: 0; font-size: 20px; }
.sub { margin: 4px 0 0; font-size: 12px; opacity: 0.85; }
:deep(.tool-card) {
  display: flex;
  background: white;
  border-radius: 12px;
  padding: 12px;
  margin: 8px 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  align-items: center;
  gap: 12px;
}
:deep(.tool-icon) {
  width: 48px; height: 48px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; font-weight: 700;
}
:deep(.tool-body) { flex: 1; }
:deep(.tool-name) { font-size: 14px; font-weight: 600; display: flex; align-items: center; gap: 6px; }
:deep(.tool-desc) { font-size: 12px; color: #666; margin: 2px 0; }
:deep(.tool-code) { font-size: 10px; color: #999; font-family: monospace; }
</style>
