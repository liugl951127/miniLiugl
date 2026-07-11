<template>
  <div class="discover-page">
    <van-search v-model="keyword" placeholder="搜索应用 / 工具 / 模型" @search="onSearch" />

    <!-- Banner -->
    <van-swipe :autoplay="4000" indicator-color="white" style="margin: 12px">
      <van-swipe-item v-for="b in banners" :key="b.title">
        <div class="banner" :style="{ background: b.bg }">
          <div class="banner-emoji">{{ b.emoji }}</div>
          <div class="banner-title">{{ b.title }}</div>
          <div class="banner-sub">{{ b.sub }}</div>
        </div>
      </van-swipe-item>
    </van-swipe>

    <!-- 快捷功能 -->
    <van-grid :column-num="4" :gutter="8" style="margin: 12px">
      <van-grid-item v-for="f in quickEntries" :key="f.label" :icon="f.icon" :text="f.label" @click="onQuick(f)" />
    </van-grid>

    <!-- 推荐应用 -->
    <div class="section">
      <div class="section-title">
        <span>🔥 热门应用</span>
        <van-tag plain type="primary">实时</van-tag>
      </div>
      <van-card
        v-for="app in filteredApps" :key="app.name"
        :title="app.name" :desc="app.desc"
        :thumb="app.thumb" :price="app.tag" centered
        style="margin-bottom: 8px"
      >
        <template #tags>
          <van-tag v-for="t in app.tags" :key="t" plain type="primary" style="margin-right:4px">{{ t }}</van-tag>
        </template>
        <template #footer>
          <van-button size="mini" type="primary" @click="onTry(app)">试用</van-button>
        </template>
      </van-card>
      <van-empty v-if="!filteredApps.length" description="无匹配结果" />
    </div>

    <div style="text-align:center; padding:24px; color:#999; font-size:12px">
      V2.7.5 · MiniMax Platform
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { showToast } from 'vant'

const keyword = ref('')

const banners = [
  { emoji: '🚀', title: 'MiniMax AI 平台', sub: '自研 · 多模态 · 企业级', bg: 'linear-gradient(135deg, #667eea, #764ba2)' },
  { emoji: '🤖', title: '智能 Agent', sub: 'ReAct 推理 + 多工具协同', bg: 'linear-gradient(135deg, #f093fb, #f5576c)' },
  { emoji: '📊', title: 'AI 报表', sub: '7 种图表 · 自然语言生成', bg: 'linear-gradient(135deg, #4facfe, #00f2fe)' },
  { emoji: '🎨', title: 'AIGC 创作', sub: '图片 / 音乐 / 视频', bg: 'linear-gradient(135deg, #43e97b, #38f9d7)' }
]

const quickEntries = [
  { label: 'AI 聊天', icon: 'chat-o' },
  { label: '智能体', icon: 'aiming' },
  { label: '知识库', icon: 'cluster-o' },
  { label: '插件', icon: 'apps-o' },
  { label: '报表', icon: 'chart-trending-o' },
  { label: '训练', icon: 'fire-o' },
  { label: '应用', icon: 'gem-o' },
  { label: '设置', icon: 'setting-o' }
]

const apps = [
  { name: 'SQL 自然语言查询', desc: '用自然语言查数据库, 自动生成 SQL', thumb: 'https://fastly.jsdelivr.net/npm/@vant/assets/ipad.jpeg', tag: '免费', tags: ['SQL', 'NLP', '数据库'] },
  { name: 'AI 翻译官', desc: '支持 12 种语言, 上下文理解', thumb: 'https://fastly.jsdelivr.net/npm/@vant/assets/apple-1.jpeg', tag: '免费', tags: ['翻译', '多语言'] },
  { name: '数据分析师', desc: '统计 / 趋势 / 异常检测 / 分布', thumb: 'https://fastly.jsdelivr.net/npm/@vant/assets/apple-2.jpeg', tag: 'PRO', tags: ['分析', '可视化'] },
  { name: '代码生成器', desc: '从数据库表结构生成 Spring Boot 项目', thumb: 'https://fastly.jsdelivr.net/npm/@vant/assets/apple-3.jpeg', tag: 'PRO', tags: ['代码', 'Spring Boot'] },
  { name: '图像识别', desc: 'pHash + 颜色直方图 + 风格分析', thumb: 'https://fastly.jsdelivr.net/npm/@vant/assets/apple-4.jpeg', tag: '免费', tags: ['图像', 'CV'] },
  { name: '语音分析', desc: 'RMS + 频谱 + 情绪倾向', thumb: 'https://fastly.jsdelivr.net/npm/@vant/assets/apple-5.jpeg', tag: '免费', tags: ['音频', 'STT'] }
]

const filteredApps = computed(() => {
  if (!keyword.value) return apps
  const k = keyword.value.toLowerCase()
  return apps.filter(a => a.name.toLowerCase().includes(k) || a.desc.toLowerCase().includes(k) || a.tags.some(t => t.toLowerCase().includes(k)))
})

function onSearch() {
  showToast({ message: '搜索: ' + keyword.value, position: 'bottom' })
}

function onQuick(f: any) {
  showToast({ message: f.label, position: 'bottom' })
}

function onTry(app: any) {
  showToast({ message: '启动 ' + app.name, position: 'bottom' })
}
</script>

<style scoped>
.discover-page { padding-bottom: 20px; }
.banner {
  height: 140px;
  border-radius: 12px;
  color: white;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 4px;
}
.banner-emoji { font-size: 36px; }
.banner-title { font-size: 18px; font-weight: 700; }
.banner-sub { font-size: 12px; opacity: 0.85; }
.section { padding: 0 12px; }
.section-title { font-size: 16px; font-weight: 600; margin: 12px 0; display: flex; align-items: center; gap: 8px; }
</style>
