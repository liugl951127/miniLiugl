<!--
  @file PageEnhancer.vue - V6.3+ 页面增强器
  用法:
    <PageEnhancer title="..." subtitle="..." icon="..." :gradient="['#a', '#b']">
      <template #actions>
        <el-button>...</el-button>
      </template>
      <main>...页面内容...</main>
    </PageEnhancer>
-->
<template>
  <div class="page-enhancer">
    <!-- 顶部欢迎区 -->
    <header class="enhancer-header" :style="headerStyle">
      <div class="enhancer-bg">
        <div v-for="i in 3" :key="i" class="bg-blob" :class="`bg-blob-${i}`" />
      </div>
      <div class="enhancer-content">
        <div class="enhancer-text">
          <h1 class="enhancer-title">
            <span class="icon-wrap">{{ icon }}</span>
            <span class="gradient-text">{{ title }}</span>
          </h1>
          <p v-if="subtitle" class="enhancer-subtitle">{{ subtitle }}</p>
        </div>
        <div v-if="$slots.actions" class="enhancer-actions">
          <slot name="actions" />
        </div>
      </div>
    </header>

    <!-- 主体内容 -->
    <main class="enhancer-main">
      <slot />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  icon: { type: String, default: '📊' },
  gradient: { type: Array, default: () => ['#667eea', '#764ba2'] }
})

const headerStyle = computed(() => ({
  background: `linear-gradient(135deg, ${props.gradient[0]}, ${props.gradient[1]})`
}))
</script>

<style scoped>
.page-enhancer {
  padding: 0;
  max-width: 1400px;
  margin: 0 auto;
}

.enhancer-header {
  position: relative;
  border-radius: 24px;
  overflow: hidden;
  margin: 16px 0 24px 0;
  color: white;
  padding: 32px 40px;
  min-height: 140px;
}

.enhancer-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
}
.bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px);
  opacity: 0.4;
  animation: blobFloat 8s ease infinite;
}
.bg-blob-1 {
  width: 250px; height: 250px;
  background: #fff;
  top: -80px; left: -80px;
}
.bg-blob-2 {
  width: 200px; height: 200px;
  background: #ec4899;
  top: 30px; right: -50px;
  animation-delay: -3s;
}
.bg-blob-3 {
  width: 180px; height: 180px;
  background: #06b6d4;
  bottom: -60px; left: 30%;
  animation-delay: -6s;
}
@keyframes blobFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(20px, -20px) scale(1.1); }
  66% { transform: translate(-10px, 20px) scale(0.95); }
}

.enhancer-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 20px;
}

.enhancer-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 6px 0;
  display: flex;
  align-items: center;
  gap: 12px;
}
.icon-wrap {
  font-size: 32px;
  display: inline-block;
  animation: emojiWiggle 2s ease infinite;
}
@keyframes emojiWiggle {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(-15deg); }
  75% { transform: rotate(15deg); }
}
.gradient-text {
  background: linear-gradient(90deg, #fff, #dbeafe);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.enhancer-subtitle {
  font-size: 14px;
  opacity: 0.9;
  margin: 0;
}
.enhancer-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.enhancer-actions :deep(.el-button) {
  backdrop-filter: blur(10px);
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
}
.enhancer-actions :deep(.el-button:hover) {
  background: rgba(255, 255, 255, 0.25);
  border-color: rgba(255, 255, 255, 0.5);
}

.enhancer-main {
  padding: 0 24px 24px 24px;
}
</style>
