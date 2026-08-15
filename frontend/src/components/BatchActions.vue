<template>
  <transition name="slide-down">
    <div v-if="count > 0" class="batch-actions" :class="{ sticky }">
      <div class="batch-info">
        <el-icon><Check /></el-icon>
        <span>{{ t('batch.selected', { count }) }}</span>
        <el-button link size="small" @click="$emit('clear')">{{ t('batch.clear') }}</el-button>
      </div>
      <div class="batch-buttons">
        <el-button
          v-for="a in actions"
          :key="a.key"
          :type="a.type || 'default'"
          :icon="a.icon"
          :loading="loadingKey === a.key"
          :disabled="a.requireConfirm && !a.confirmed"
          size="small"
          @click="execute(a)"
        >
          {{ a.label }}
        </el-button>
        <slot :selected="selected" :execute="execute" />
      </div>
    </div>
  </transition>
</template>

<script setup>
import { Check } from '@element-plus/icons-vue'
import { ref } from 'vue'
import { useI18n } from '@/i18n'
import { ElMessageBox } from 'element-plus'

const props = defineProps({
  selected: { type: Array, default: () => [] },
  actions: { type: Array, default: () => [] },
  sticky: { type: Boolean, default: true }
})

const emit = defineEmits(['action', 'clear'])

const { t } = useI18n()
const loadingKey = ref(null)

const count = () => props.selected.length

async function execute(action) {
  if (action.requireConfirm) {
    try {
      await ElMessageBox.confirm(
        action.confirmMessage || t('batch.confirmMessage', { action: action.label, count: count() }),
        action.confirmTitle || t('batch.confirmTitle'),
        { type: action.confirmType || 'warning' }
      )
    } catch {
      return
    }
  }
  
  loadingKey.value = action.key
  try {
    await emit('action', action, props.selected)
  } finally {
    loadingKey.value = null
  }
}
</script>

<style lang="scss" scoped>
.batch-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, var(--el-color-primary-light-9), var(--el-color-primary-light-7));
  border: 1px solid var(--el-color-primary-light-5);
  border-radius: 8px;
  color: var(--el-color-primary);
  
  &.sticky {
    position: sticky;
    top: 16px;
    z-index: 10;
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  }
}

.batch-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.batch-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s;
}

.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-20px);
  max-height: 0;
  margin-bottom: 0;
  padding-top: 0;
  padding-bottom: 0;
}
</style>
