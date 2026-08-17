/**
 * useClipboard — 跨环境 clipboard 写（带 textarea 降级）
 * 优先使用 navigator.clipboard API；沙箱/受限环境 fallback 到 textarea + execCommand。
 */
import { ElMessage } from 'element-plus'

/**
 * @param {string} text
 * @param {{ successMsg?: string | ((text: string) => string), failMsg?: string }} opts
 */
export function useClipboard({ successMsg = '已复制', failMsg = '复制失败' } = {}) {
  function copy(text) {
    const msg = typeof successMsg === 'function' ? successMsg(text) : successMsg
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(text).then(() => {
        ElMessage.success({ message: msg, duration: 1500 })
      }).catch(() => fallbackCopy(text, msg))
    } else {
      fallbackCopy(text, msg)
    }
  }

  function fallbackCopy(text, msg) {
    try {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.style.cssText = 'position:fixed;top:-9999px;left:-9999px;opacity:0'
      document.body.appendChild(ta)
      ta.focus()
      ta.select()
      const ok = document.execCommand('copy')
      document.body.removeChild(ta)
      if (ok) {
        ElMessage.success({ message: msg, duration: 1500 })
      } else {
        ElMessage.error(failMsg)
      }
    } catch {
      ElMessage.error(failMsg)
    }
  }

  return { copy }
}
