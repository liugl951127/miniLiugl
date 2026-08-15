<template>
  <div class="search-bar" :class="{ expanded: focused }">
    <el-input
      v-model="value"
      :placeholder="placeholder"
      :clearable="clearable"
      :size="size"
      @input="onInput"
      @focus="focused = true"
      @blur="focused = false"
      @clear="onClear"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
      <template v-if="shortcut" #suffix>
        <kbd class="kbd-hint">{{ shortcut }}</kbd>
      </template>
    </el-input>
    
    <div v-if="filters && filters.length" class="filter-chips">
      <el-tag
        v-for="f in filters"
        :key="f.key"
        :type="activeFilter === f.key ? 'primary' : 'info'"
        effect="plain"
        class="filter-chip"
        @click="toggleFilter(f.key)"
      >
        {{ f.label }}
        <span v-if="f.count" class="filter-count">{{ f.count }}</span>
      </el-tag>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { Search } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '搜索...' },
  clearable: { type: Boolean, default: true },
  size: { type: String, default: 'default' },
  shortcut: { type: String, default: '/' },
  filters: { type: Array, default: () => [] },
  debounce: { type: Number, default: 300 }
})

const emit = defineEmits(['update:modelValue', 'search', 'filter-change', 'clear'])

const value = ref(props.modelValue)
const focused = ref(false)
const activeFilter = ref(null)
let timer = null

watch(() => props.modelValue, v => value.value = v)

function onInput() {
  clearTimeout(timer)
  timer = setTimeout(() => {
    emit('update:modelValue', value.value)
    emit('search', value.value)
  }, props.debounce)
}

function onClear() {
  value.value = ''
  emit('update:modelValue', '')
  emit('search', '')
  emit('clear')
}

function toggleFilter(key) {
  activeFilter.value = activeFilter.value === key ? null : key
  emit('filter-change', activeFilter.value)
}

function focus() {
  document.querySelector('.search-bar input')?.focus()
}

function handleShortcut(e) {
  if (e.key === props.shortcut && document.activeElement.tagName !== 'INPUT' && document.activeElement.tagName !== 'TEXTAREA') {
    e.preventDefault()
    focus()
  }
}

onMounted(() => {
  if (props.shortcut) {
    document.addEventListener('keydown', handleShortcut)
  }
})
onUnmounted(() => {
  document.removeEventListener('keydown', handleShortcut)
  clearTimeout(timer)
})

defineExpose({ focus })
</script>

<style lang="scss" scoped>
.search-bar {
  position: relative;
  width: 100%;
  transition: all 0.3s;
  
  &.expanded {
    :deep(.el-input__wrapper) {
      box-shadow: 0 0 0 2px var(--el-color-primary-light-5);
    }
  }
}

.kbd-hint {
  display: inline-block;
  padding: 2px 6px;
  font-size: 11px;
  font-family: monospace;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  line-height: 1;
}

.filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.filter-chip {
  cursor: pointer;
  transition: all 0.2s;
  
  &:hover {
    transform: translateY(-1px);
  }
}

.filter-count {
  margin-left: 4px;
  font-size: 11px;
  opacity: 0.7;
}
</style>
