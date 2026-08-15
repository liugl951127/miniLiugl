/**
 * @file composables/usePageSkeleton.js (V3.5.92+)
 * 骨架屏 composable - 控制是否显示 PageSkeleton
 */
import { ref } from 'vue'

const isLoading = ref(false)
const loadingText = ref('加载中...')

export function usePageSkeleton() {
  function startLoading(text = '加载中...') {
    loadingText.value = text
    isLoading.value = true
  }
  function stopLoading() {
    isLoading.value = false
  }
  return { isLoading, loadingText, startLoading, stopLoading }
}

export default usePageSkeleton
