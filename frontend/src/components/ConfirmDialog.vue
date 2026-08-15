<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    :align-center="true"
    :close-on-click-modal="!loading"
    :close-on-press-escape="!loading"
    :show-close="!loading"
    :class="['confirm-dialog', type]"
  >
    <div class="confirm-content">
      <el-icon class="confirm-icon" :size="48">
        <component :is="iconComponent" />
      </el-icon>
      <div class="confirm-message">
        <h4 v-if="message" class="confirm-title">{{ message }}</h4>
        <p v-if="description" class="confirm-desc">{{ description }}</p>
        <slot />
      </div>
    </div>
    <template #footer>
      <el-button @click="onCancel" :disabled="loading">{{ cancelText || t('common.cancel') }}</el-button>
      <el-button :type="type" :loading="loading" @click="onConfirm">
        {{ confirmText || t('common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Warning, QuestionFilled, CircleCheck, CircleClose, InfoFilled } from '@element-plus/icons-vue'
import { useI18n } from '@/i18n'

const props = defineProps({
  modelValue: Boolean,
  title: { type: String, default: '' },
  message: { type: String, default: '' },
  description: { type: String, default: '' },
  type: { type: String, default: 'warning' },
  width: { type: String, default: '420px' },
  confirmText: String,
  cancelText: String,
  loading: Boolean
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: v => emit('update:modelValue', v)
})

const iconComponent = computed(() => {
  const map = {
    warning: Warning,
    success: CircleCheck,
    error: CircleClose,
    info: InfoFilled,
    question: QuestionFilled
  }
  return map[props.type] || QuestionFilled
})

function onConfirm() { emit('confirm') }
function onCancel() { emit('cancel'); visible.value = false }
</script>

<style lang="scss" scoped>
.confirm-content {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  padding: 8px 0;
}

.confirm-icon {
  flex-shrink: 0;
  
  .confirm-dialog.warning & { color: var(--el-color-warning); }
  .confirm-dialog.success & { color: var(--el-color-success); }
  .confirm-dialog.error & { color: var(--el-color-danger); }
  .confirm-dialog.info & { color: var(--el-color-info); }
  .confirm-dialog.question & { color: var(--el-color-primary); }
}

.confirm-message {
  flex: 1;
}

.confirm-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.confirm-desc {
  margin: 0;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}
</style>
