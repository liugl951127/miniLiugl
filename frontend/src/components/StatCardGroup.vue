<template>
  <div class="stat-card-group" :class="`layout-${layout}`">
    <el-tooltip
      v-for="(stat, i) in stats"
      :key="i"
      :content="stat.tip || ''"
      placement="top"
      :disabled="!stat.tip"
    >
      <template #content>
        <span v-if="stat.tip">{{ stat.tip }}</span>
      </template>
      <div
        class="stat-card"
        :class="[`variant-${stat.variant || 'default'}`, { clickable: stat.clickable }]"
        @click="stat.clickable && $emit('click', stat)"
      >
        <div class="stat-header">
          <div class="stat-icon" :style="{ background: stat.bgColor }">
            <el-icon :size="20" v-if="stat.icon">
              <component :is="stat.icon" />
            </el-icon>
          </div>
          <div v-if="stat.trend" class="stat-trend" :class="stat.trend.direction">
            <el-icon :size="12">
              <component :is="stat.trend.direction === 'up' ? 'CaretTop' : 'CaretBottom'" />
            </el-icon>
            {{ stat.trend.value }}
          </div>
        </div>

        <div class="stat-value">
          <span class="value">{{ stat.prefix || '' }}{{ formatNumber(stat.value) }}{{ stat.suffix || '' }}</span>
          <span v-if="stat.loading" class="loading-dots">
            <span></span><span></span><span></span>
          </span>
        </div>

        <div class="stat-label">{{ stat.label }}</div>

        <div v-if="stat.subtitle" class="stat-subtitle">
          <TimeAgo v-if="stat.subtitleTime" :time="stat.subtitleTime" />
          <template v-else>{{ stat.subtitle }}</template>
        </div>

        <el-progress
          v-if="stat.progress !== undefined"
          :percentage="stat.progress"
          :stroke-width="4"
          :color="stat.progressColor"
          class="stat-progress"
        />
      </div>
    </el-tooltip>
  </div>
</template>

<script setup>
import TimeAgo from './TimeAgo.vue'

const _props = defineProps({
  stats: { type: Array, default: () => [] },
  layout: { type: String, default: 'row' } // row | grid
})

defineEmits(['click'])

function formatNumber(v) {
  if (typeof v !== 'number') return v
  if (v >= 1e9) return (v / 1e9).toFixed(1) + 'B'
  if (v >= 1e6) return (v / 1e6).toFixed(1) + 'M'
  if (v >= 1e3) return (v / 1e3).toFixed(1) + 'K'
  return v.toString()
}
</script>

<style lang="scss" scoped>
.stat-card-group {
  display: grid;
  gap: 16px;
  
  &.layout-row {
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  }
  
  &.layout-grid {
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  }
}

.stat-card {
  position: relative;
  padding: 20px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  transition: all 0.3s;
  overflow: hidden;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
    border-color: var(--el-color-primary-light-5);
  }
  
  &.clickable {
    cursor: pointer;
  }
  
  &.variant-primary {
    background: linear-gradient(135deg, var(--el-color-primary-light-9), var(--el-bg-color));
    border-color: var(--el-color-primary-light-5);
  }
  
  &.variant-success {
    background: linear-gradient(135deg, var(--el-color-success-light-9), var(--el-bg-color));
    border-color: var(--el-color-success-light-5);
  }
  
  &.variant-warning {
    background: linear-gradient(135deg, var(--el-color-warning-light-9), var(--el-bg-color));
    border-color: var(--el-color-warning-light-5);
  }
  
  &.variant-danger {
    background: linear-gradient(135deg, var(--el-color-danger-light-9), var(--el-bg-color));
    border-color: var(--el-color-danger-light-5);
  }
}

.stat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  
  &.up {
    color: var(--el-color-success);
    background: var(--el-color-success-light-9);
  }
  
  &.down {
    color: var(--el-color-danger);
    background: var(--el-color-danger-light-9);
  }
}

.stat-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
  
  .value {
    font-size: 28px;
    font-weight: 700;
    color: var(--el-text-color-primary);
    line-height: 1.2;
  }
}

.stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.stat-subtitle {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  margin-top: 8px;
}

.stat-progress {
  margin-top: 12px;
}

.loading-dots {
  display: inline-flex;
  gap: 3px;
  
  span {
    width: 6px;
    height: 6px;
    background: var(--el-color-primary);
    border-radius: 50%;
    animation: dot 1.4s infinite ease-in-out both;
    
    &:nth-child(1) { animation-delay: -0.32s; }
    &:nth-child(2) { animation-delay: -0.16s; }
  }
}

@keyframes dot {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
</style>
