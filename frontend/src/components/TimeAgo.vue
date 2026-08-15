<template>
  <el-tooltip :content="absolute" placement="top">
    <span class="time-ago" :class="freshness">
      <el-icon v-if="icon" :size="12"><Clock /></el-icon>
      <slot :time="relative">{{ relative }}</slot>
    </span>
  </el-tooltip>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Clock } from '@element-plus/icons-vue'
import { useI18n } from '@/i18n'

const props = defineProps({
  time: { type: [String, Date, Number], required: true },
  refresh: { type: Number, default: 30 }, // 秒
  icon: { type: Boolean, default: false }
})

const { t } = useI18n()
const now = ref(Date.now())
let timer = null

onMounted(() => {
  timer = setInterval(() => { now.value = Date.now() }, props.refresh * 1000)
})
onUnmounted(() => clearInterval(timer))

const timestamp = computed(() => new Date(props.time).getTime())
const diff = computed(() => now.value - timestamp.value)

const relative = computed(() => {
  const d = diff.value
  if (d < 0) return t('time.future')
  const sec = Math.floor(d / 1000)
  if (sec < 5) return t('time.justNow')
  if (sec < 60) return t('time.secondsAgo', { n: sec })
  const min = Math.floor(sec / 60)
  if (min < 60) return t('time.minutesAgo', { n: min })
  const hr = Math.floor(min / 60)
  if (hr < 24) return t('time.hoursAgo', { n: hr })
  const day = Math.floor(hr / 24)
  if (day < 30) return t('time.daysAgo', { n: day })
  const month = Math.floor(day / 30)
  if (month < 12) return t('time.monthsAgo', { n: month })
  return t('time.yearsAgo', { n: Math.floor(month / 12) })
})

const absolute = computed(() => new Date(props.time).toLocaleString())

const freshness = computed(() => {
  const d = diff.value
  if (d < 60 * 1000) return 'fresh'
  if (d < 24 * 60 * 60 * 1000) return 'recent'
  if (d < 7 * 24 * 60 * 60 * 1000) return 'old'
  return 'stale'
})
</script>

<style lang="scss" scoped>
.time-ago {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  cursor: help;
  
  &.fresh { color: var(--el-color-success); }
  &.recent { color: var(--el-text-color-regular); }
  &.old { color: var(--el-text-color-secondary); }
  &.stale { color: var(--el-text-color-placeholder); }
}
</style>
