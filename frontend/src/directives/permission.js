/**
 * v-permission 指令 (V2.7.9)
 *
 * 用法:
 *   <el-button v-permission="'ai.admin'">删除</el-button>
 *   <div v-permission="['ai.use', 'ai.admin']" mode="any">...</div>
 *
 * 不通过: 移除元素 (display: none)
 * 支持 mode: all (默认) / any
 */
import { usePermission } from '@/composables/usePermission'

function check(perm, mode = 'all') {
  const { hasAll, hasAny } = usePermission()
  if (Array.isArray(perm)) {
    return mode === 'any' ? hasAny(...perm) : hasAll(...perm)
  }
  return hasAll(perm)
}

export default {
  mounted(el, binding) {
    const { value, modifiers } = binding
    const mode = modifiers.any ? 'any' : 'all'
    if (!check(value, mode)) {
      el.style.display = 'none'
      el.setAttribute('data-permission-blocked', 'true')
    }
  },
  updated(el, binding) {
    const { value, modifiers } = binding
    const mode = modifiers.any ? 'any' : 'all'
    if (!check(value, mode)) {
      el.style.display = 'none'
      el.setAttribute('data-permission-blocked', 'true')
    } else {
      el.style.display = ''
      el.removeAttribute('data-permission-blocked')
    }
  }
}
