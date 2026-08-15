import { onMounted, onUnmounted } from 'vue'

const handlers = new Map()
let listenerInstalled = false

function handleKey(e) {
  // 跳过输入框
  if (['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement?.tagName) ||
      document.activeElement?.isContentEditable) {
    return
  }
  
  const parts = []
  if (e.ctrlKey || e.metaKey) parts.push('mod')
  if (e.shiftKey) parts.push('shift')
  if (e.altKey) parts.push('alt')
  parts.push(e.key.toLowerCase())
  const combo = parts.join('+')
  
  const handler = handlers.get(combo)
  if (handler) {
    e.preventDefault()
    handler(e)
  }
}

export function useKeyboardShortcuts(map) {
  onMounted(() => {
    Object.entries(map).forEach(([key, fn]) => {
      handlers.set(key.toLowerCase(), fn)
    })
    if (!listenerInstalled) {
      document.addEventListener('keydown', handleKey)
      listenerInstalled = true
    }
  })
  
  onUnmounted(() => {
    Object.keys(map).forEach(k => handlers.delete(k.toLowerCase()))
  })
}

export function useGlobalShortcut(key, fn) {
  return useKeyboardShortcuts({ [key]: fn })
}
