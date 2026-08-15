<template>
  <el-dialog
    v-model="visible"
    :title="t('shortcuts.title')"
    width="640px"
    align-center
  >
    <div class="shortcuts-container">
      <el-input
        v-model="search"
        :placeholder="t('shortcuts.search')"
        clearable
        class="search-input"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      
      <div class="shortcuts-list">
        <div v-for="(group, key) in filteredGroups" :key="key" class="group">
          <h4 class="group-title">{{ group.title }}</h4>
          <div v-for="(s, i) in group.items" :key="i" class="shortcut-item">
            <div class="action">
              <el-icon v-if="s.icon"><component :is="s.icon" /></el-icon>
              <span>{{ s.label }}</span>
            </div>
            <div class="keys">
              <template v-for="(k, j) in s.keys" :key="j">
                <kbd class="key">{{ k }}</kbd>
                <span v-if="j < s.keys.length - 1" class="plus">+</span>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { useI18n } from '@/i18n'

const props = defineProps({
  modelValue: Boolean,
  groups: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:modelValue'])
const { t } = useI18n()
const search = ref('')

const visible = computed({
  get: () => props.modelValue,
  set: v => emit('update:modelValue', v)
})

const filteredGroups = computed(() => {
  if (!search.value) return props.groups
  const k = search.value.toLowerCase()
  return props.groups.map(g => ({
    ...g,
    items: g.items.filter(it => it.label.toLowerCase().includes(k))
  })).filter(g => g.items.length)
})
</script>

<style lang="scss" scoped>
.shortcuts-container {
  max-height: 60vh;
  display: flex;
  flex-direction: column;
}

.search-input {
  margin-bottom: 16px;
}

.shortcuts-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 8px;
}

.group {
  margin-bottom: 24px;
  
  &:last-child { margin-bottom: 0; }
}

.group-title {
  margin: 0 0 8px 0;
  font-size: 13px;
  font-weight: 600;
  text-transform: uppercase;
  color: var(--el-text-color-secondary);
  letter-spacing: 0.5px;
}

.shortcut-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 6px;
  
  &:hover {
    background: var(--el-fill-color-light);
  }
}

.action {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-regular);
}

.keys {
  display: flex;
  align-items: center;
  gap: 4px;
}

.key {
  display: inline-block;
  min-width: 24px;
  padding: 3px 8px;
  font-size: 12px;
  font-family: monospace;
  text-align: center;
  color: var(--el-text-color-primary);
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  border-bottom-width: 2px;
  border-radius: 4px;
}

.plus {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
</style>
