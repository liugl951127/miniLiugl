/**
 * V6.7+ useEnhancer 集成 UX 增强
 * - 全局快捷键
 * - 主题持久化
 * - 自动刷新
 * - 操作统计
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

export function useEnhancer() {
  const router = useRouter()
  
  // 1. 全局快捷键
  function handleKey(e) {
    if (['INPUT', 'TEXTAREA'].includes(document.activeElement?.tagName)) return
    
    // Ctrl+K 搜索
    if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
      e.preventDefault()
      // 触发搜索 (全局)
      document.querySelector('.search-bar input')?.focus()
    }
    
    // Ctrl+/ 帮助
    if ((e.ctrlKey || e.metaKey) && e.key === '/') {
      e.preventDefault()
      // 触发快捷键弹窗
      document.dispatchEvent(new CustomEvent('open-shortcuts'))
    }
    
    // Esc 返回
    if (e.key === 'Escape') {
      router.back()
    }
    
    // g d 跳到 dashboard
    if (e.key === 'g') {
      window._nextKey = 'g'
      setTimeout(() => { window._nextKey = null }, 500)
    } else if (window._nextKey === 'g' && e.key === 'd') {
      e.preventDefault()
      router.push('/dashboard')
      window._nextKey = null
    }
  }
  
  // 2. 自动刷新
  const refreshInterval = ref(null)
  function startAutoRefresh(callback, interval = 30000) {
    if (refreshInterval.value) clearInterval(refreshInterval.value)
    refreshInterval.value = setInterval(callback, interval)
  }
  
  function stopAutoRefresh() {
    if (refreshInterval.value) {
      clearInterval(refreshInterval.value)
      refreshInterval.value = null
    }
  }
  
  // 3. 操作统计
  const stats = ref({ clicks: 0, navigation: 0, actions: 0 })
  function trackAction(type) {
    stats.value[type] = (stats.value[type] || 0) + 1
  }
  
  onMounted(() => {
    document.addEventListener('keydown', handleKey)
  })
  onUnmounted(() => {
    document.removeEventListener('keydown', handleKey)
    stopAutoRefresh()
  })
  
  return {
    startAutoRefresh,
    stopAutoRefresh,
    trackAction,
    stats
  }
}
