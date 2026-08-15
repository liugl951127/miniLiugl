<template>
  <div class="quick-actions">
    <div class="qa-header">
      <h3 class="qa-title">
        <el-icon><MagicStick /></el-icon>
        {{ title || t('quickActions.title') }}
      </h3>
      <p v-if="description" class="qa-desc">{{ description }}</p>
    </div>
    <div class="qa-grid" :style="{ gridTemplateColumns: `repeat(${columns}, 1fr)` }">
      <div
        v-for="(action, i) in actions"
        :key="i"
        class="qa-card"
        :class="[`qa-card-${action.color || 'primary'}`, { disabled: action.disabled }]"
        @click="execute(action)"
      >
        <el-icon class="qa-icon" :size="32">
          <component :is="action.icon" />
        </el-icon>
        <div class="qa-info">
          <div class="qa-name">{{ action.label }}</div>
          <div v-if="action.desc" class="qa-desc-small">{{ action.desc }}</div>
        </div>
        <el-icon v-if="action.shortcut" class="qa-shortcut">{{ action.shortcut }}</el-icon>
        <span v-if="action.badge" class="qa-badge">{{ action.badge }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { MagicStick } from '@element-plus/icons-vue'
import { useI18n } from '@/i18n'

defineProps({
  title: String,
  description: String,
  actions: { type: Array, default: () => [] },
  columns: { type: Number, default: 4 }
})

const emit = defineEmits(['action'])
const { t } = useI18n()

function execute(action) {
  if (action.disabled) return
  emit('action', action)
}
</script>

<style lang="scss" scoped>
.quick-actions {
  margin-bottom: 20px;
}

.qa-header {
  margin-bottom: 16px;
}

.qa-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.qa-desc {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.qa-grid {
  display: grid;
  gap: 12px;
}

.qa-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
    background: var(--el-color-primary);
    transform: scaleY(0);
    transition: transform 0.25s;
  }
  
  &:hover:not(.disabled) {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
    border-color: var(--el-color-primary-light-5);
    
    &::before {
      transform: scaleY(1);
    }
    
    .qa-icon {
      transform: scale(1.1);
    }
  }
  
  &.disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  
  &.qa-card-success::before { background: var(--el-color-success); }
  &.qa-card-warning::before { background: var(--el-color-warning); }
  &.qa-card-info::before { background: var(--el-color-info); }
  &.qa-card-danger::before { background: var(--el-color-danger); }
}

.qa-icon {
  flex-shrink: 0;
  color: var(--el-color-primary);
  transition: transform 0.25s;
  
  .qa-card-success & { color: var(--el-color-success); }
  .qa-card-warning & { color: var(--el-color-warning); }
  .qa-card-info & { color: var(--el-color-info); }
  .qa-card-danger & { color: var(--el-color-danger); }
}

.qa-info {
  flex: 1;
  min-width: 0;
}

.qa-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.qa-desc-small {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

.qa-shortcut {
  font-size: 10px;
  padding: 2px 6px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  color: var(--el-text-color-secondary);
  font-family: monospace;
}

.qa-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  font-size: 11px;
  font-weight: 600;
  color: white;
  background: var(--el-color-danger);
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
