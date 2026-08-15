<template>
  <teleport to="body">
    <div v-if="currentStep" class="feature-tour-overlay" @click.self="onSkip">
      <div
        class="feature-tour-mask"
        :style="maskStyle"
      ></div>
      
      <div
        class="feature-tour-popover"
        :style="popoverStyle"
      >
        <div class="tour-header">
          <span class="tour-step-count">
            {{ currentIndex + 1 }} / {{ steps.length }}
          </span>
          <el-button link size="small" @click="onSkip">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        
        <h3 class="tour-title">{{ currentStep.title }}</h3>
        <p class="tour-content">{{ currentStep.content }}</p>
        
        <div class="tour-footer">
          <el-checkbox v-if="!required" v-model="neverShow">
            {{ t('tour.neverShow') }}
          </el-checkbox>
          <div class="tour-spacer"></div>
          <el-button @click="onPrev" :disabled="currentIndex === 0">
            {{ t('common.prev') }}
          </el-button>
          <el-button type="primary" @click="onNext">
            {{ isLast ? t('tour.finish') : t('common.next') }}
            <el-icon v-if="!isLast"><ArrowRight /></el-icon>
            <el-icon v-else><Check /></el-icon>
          </el-button>
        </div>
        
        <div class="tour-progress">
          <div
            v-for="(s, i) in steps"
            :key="i"
            class="tour-dot"
            :class="{ active: i === currentIndex, done: i < currentIndex }"
            @click="goTo(i)"
          ></div>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { Close, ArrowRight, Check } from '@element-plus/icons-vue'
import { useI18n } from '@/i18n'

const props = defineProps({
  modelValue: Boolean,
  steps: { type: Array, default: () => [] },
  required: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'finish', 'skip'])
const { t } = useI18n()

const currentIndex = ref(0)
const neverShow = ref(false)
const targetRect = ref(null)

const visible = computed({
  get: () => props.modelValue,
  set: v => emit('update:modelValue', v)
})

const currentStep = computed(() => props.steps[currentIndex.value])
const isLast = computed(() => currentIndex.value === props.steps.length - 1)

const maskStyle = computed(() => {
  if (!targetRect.value) return {}
  const r = targetRect.value
  return {
    clipPath: `polygon(
      0% 0%, 0% 100%, ${r.left}px 100%, ${r.left}px ${r.top}px,
      ${r.right}px ${r.top}px, ${r.right}px ${r.bottom}px,
      ${r.left}px ${r.bottom}px, ${r.left}px 100%, 100% 100%, 100% 0%
    )`
  }
})

const popoverStyle = computed(() => {
  if (!targetRect.value) return {}
  const r = targetRect.value
  const position = currentStep.value?.position || 'bottom'
  const margin = 16
  let top, left
  
  if (position === 'bottom') {
    top = r.bottom + margin
    left = r.left + r.width / 2 - 200
  } else if (position === 'top') {
    top = r.top - margin - 200
    left = r.left + r.width / 2 - 200
  } else if (position === 'left') {
    top = r.top + r.height / 2 - 100
    left = r.left - margin - 400
  } else {
    top = r.top + r.height / 2 - 100
    left = r.right + margin
  }
  
  // 边界检查
  top = Math.max(margin, Math.min(top, window.innerHeight - 280))
  left = Math.max(margin, Math.min(left, window.innerWidth - 400))
  
  return { top: `${top}px`, left: `${left}px` }
})

async function updateTarget() {
  if (!currentStep.value?.target) {
    targetRect.value = null
    return
  }
  await nextTick()
  const el = document.querySelector(currentStep.value.target)
  if (el) {
    const r = el.getBoundingClientRect()
    targetRect.value = {
      top: r.top, left: r.left, right: r.right, bottom: r.bottom,
      width: r.width, height: r.height
    }
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

function onNext() {
  if (isLast.value) {
    onFinish()
  } else {
    currentIndex.value++
    updateTarget()
  }
}

function onPrev() {
  if (currentIndex.value > 0) {
    currentIndex.value--
    updateTarget()
  }
}

function goTo(i) {
  currentIndex.value = i
  updateTarget()
}

function onSkip() {
  if (neverShow.value) {
    localStorage.setItem('minimax-tour-skip', Date.now().toString())
  }
  visible.value = false
  emit('skip')
}

function onFinish() {
  visible.value = false
  emit('finish')
}

onMounted(() => {
  if (visible.value) updateTarget()
  window.addEventListener('resize', updateTarget)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateTarget)
})
</script>

<style lang="scss" scoped>
.feature-tour-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
}

.feature-tour-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  transition: clip-path 0.3s;
}

.feature-tour-popover {
  position: absolute;
  width: 380px;
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  transition: top 0.3s, left 0.3s;
}

.tour-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.tour-step-count {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

.tour-title {
  margin: 0 0 8px 0;
  font-size: 17px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.tour-content {
  margin: 0;
  color: var(--el-text-color-regular);
  line-height: 1.6;
  font-size: 14px;
}

.tour-footer {
  display: flex;
  align-items: center;
  margin-top: 20px;
  gap: 8px;
}

.tour-spacer {
  flex: 1;
}

.tour-progress {
  display: flex;
  gap: 6px;
  margin-top: 12px;
  justify-content: center;
}

.tour-dot {
  width: 8px;
  height: 8px;
  background: var(--el-border-color);
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s;
  
  &.active {
    width: 24px;
    border-radius: 4px;
    background: var(--el-color-primary);
  }
  
  &.done {
    background: var(--el-color-success);
  }
}
</style>
