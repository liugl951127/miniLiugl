<template>
  <div :class="['stat-card', `stat-${color || 'primary'}`]">
    <div class="stat-icon" v-if="icon">{{ icon }}</div>
    <div class="stat-body">
      <div class="stat-label">{{ label }}</div>
      <div class="stat-value">
        <span class="stat-num">{{ formattedValue }}</span>
        <span v-if="unit" class="stat-unit">{{ unit }}</span>
      </div>
      <div v-if="trend !== undefined" class="stat-trend" :class="trend >= 0 ? 'up' : 'down'">
        {{ trend >= 0 ? '↑' : '↓' }} {{ Math.abs(trend) }}{{ trendUnit || '%' }}
        <span v-if="trendLabel" class="stat-trend-label">{{ trendLabel }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({
  label: String,
  value: { type: [Number, String], default: 0 },
  unit: String,
  icon: String,
  color: { type: String, default: 'primary' },
  trend: Number,
  trendUnit: String,
  trendLabel: String,
  precision: { type: Number, default: 0 }
})
const formattedValue = computed(() => {
  if (typeof props.value === 'string') return props.value
  if (props.precision > 0) return props.value.toFixed(props.precision)
  if (props.value >= 10000) return (props.value / 10000).toFixed(1) + 'w'
  return props.value.toLocaleString()
})
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  transition: all 0.2s;
}
.stat-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.08); transform: translateY(-2px); }
.stat-icon { font-size: 32px; }
.stat-body { flex: 1; }
.stat-label { color: #909399; font-size: 13px; margin-bottom: 4px; }
.stat-value { display: flex; align-items: baseline; gap: 4px; }
.stat-num { font-size: 26px; font-weight: 700; color: #303133; }
.stat-unit { font-size: 13px; color: #909399; }
.stat-trend { font-size: 12px; margin-top: 4px; }
.stat-trend.up { color: #67c23a; }
.stat-trend.down { color: #f56c6c; }
.stat-trend-label { color: #909399; margin-left: 6px; }

.stat-primary { border-left: 4px solid #409EFF; }
.stat-success { border-left: 4px solid #67c23a; }
.stat-warning { border-left: 4px solid #e6a23c; }
.stat-danger { border-left: 4px solid #f56c6c; }
.stat-info { border-left: 4px solid #909399; }
</style>
