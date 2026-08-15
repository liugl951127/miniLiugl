<template>
  <transition name="fade-up">
    <button
      v-if="visible"
      class="back-to-top"
      :style="customStyle"
      :title="t('common.backToTop')"
      @click="scrollToTop"
    >
      <el-icon :size="20"><CaretTop /></el-icon>
      <span v-if="showPercent" class="percent">{{ percent }}%</span>
    </button>
  </transition>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { CaretTop } from '@element-plus/icons-vue'
import { useI18n } from '@/i18n'

const props = defineProps({
  threshold: { type: Number, default: 300 },
  showPercent: { type: Boolean, default: false },
  bottom: { type: Number, default: 40 },
  right: { type: Number, default: 40 }
})

const { t } = useI18n()
const visible = ref(false)
const percent = ref(0)

const customStyle = computed(() => ({
  bottom: `${props.bottom}px`,
  right: `${props.right}px`
}))

function handleScroll() {
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  const scrollHeight = document.documentElement.scrollHeight - window.innerHeight
  percent.value = Math.round((scrollTop / Math.max(scrollHeight, 1)) * 100)
  visible.value = scrollTop > props.threshold
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
})
onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style lang="scss" scoped>
.back-to-top {
  position: fixed;
  z-index: 99;
  width: 44px;
  height: 44px;
  border: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color);
  color: var(--el-color-primary);
  border-radius: 22px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
    color: white;
    background: var(--el-color-primary);
  }
  
  &:active {
    transform: translateY(0);
  }
}

.percent {
  font-size: 9px;
  line-height: 1;
  margin-top: 2px;
}

.fade-up-enter-active,
.fade-up-leave-active {
  transition: all 0.3s;
}

.fade-up-enter-from,
.fade-up-leave-to {
  opacity: 0;
  transform: translateY(20px);
}
</style>
