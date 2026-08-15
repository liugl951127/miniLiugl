<!--
  @file StatCard.vue - V6.3+ 统计卡片
  玻璃拟态 + 渐变 sparkline
-->
<template>
  <div class="stat-card" :class="color">
    <div class="stat-icon">{{ icon }}</div>
    <div class="stat-info">
      <div class="stat-label">{{ label }}</div>
      <div class="stat-value">
        <span class="value-num">{{ value }}</span>
        <span v-if="unit" class="value-unit">{{ unit }}</span>
      </div>
      <div v-if="trend" class="stat-trend" :class="trendType">
        <el-icon><CaretTop v-if="trendType === 'up'" /><CaretBottom v-else /></el-icon>
        <span>{{ trend }}</span>
      </div>
    </div>
    <div class="stat-spark" :style="sparkStyle" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { CaretTop, CaretBottom } from '@element-plus/icons-vue'

const props = defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], required: true },
  unit: { type: String, default: '' },
  icon: { type: String, default: '📊' },
  color: { type: String, default: 'blue' },
  trend: { type: String, default: '' },
  trendType: { type: String, default: 'up' }
})

const sparkColors = {
  blue: 'linear-gradient(135deg, #dbeafe, #bfdbfe)',
  green: 'linear-gradient(135deg, #d1fae5, #a7f3d0)',
  purple: 'linear-gradient(135deg, #f3e8ff, #e9d5ff)',
  orange: 'linear-gradient(135deg, #ffedd5, #fed7aa)',
  red: 'linear-gradient(135deg, #fee2e2, #fecaca)',
  pink: 'linear-gradient(135deg, #fce7f3, #fbcfe8)'
}

const sparkStyle = computed(() => ({ background: sparkColors[props.color] || sparkColors.blue }))
</script>

<style scoped>
.stat-card {
  position: relative;
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  display: flex;
  gap: 16px;
  align-items: center;
  overflow: hidden;
  transition: all 0.3s;
  cursor: pointer;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}
.stat-icon {
  font-size: 40px;
  flex-shrink: 0;
}
.stat-info {
  flex: 1;
  min-width: 0;
}
.stat-label {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
}
.stat-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 6px;
}
.value-num {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a1a;
}
.value-unit {
  font-size: 13px;
  color: #999;
}
.stat-trend {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
}
.stat-trend.up { color: #67c23a; }
.stat-trend.down { color: #f56c6c; }
.stat-spark {
  position: absolute;
  right: -40px;
  top: -40px;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  opacity: 0.3;
}
</style>
