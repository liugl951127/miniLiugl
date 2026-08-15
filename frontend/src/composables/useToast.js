/**
 * V3.6.9+ 统一 toast API
 * 替代 ElMessage 直接调用, 统一 6 类通知
 * 无侵入: 同时也通过 Element Plus ElMessage 输出
 * - 集中管理 (便于接 SMS/邮件/IM)
 * - 历史 (localStorage minimax_toast_log, 保留 50 条)
 */
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { ref, readonly } from 'vue'

const TOAST_LOG_KEY = 'minimax_toast_log'
const MAX_LOG = 50

const _log = ref([])

// 加载 log
try {
  const saved = localStorage.getItem(TOAST_LOG_KEY)
  if (saved) _log.value = JSON.parse(saved).slice(-MAX_LOG)
} catch (e) { /* */ }

function save() {
  try {
    localStorage.setItem(TOAST_LOG_KEY, JSON.stringify(_log.value.slice(-MAX_LOG)))
  } catch (e) { /* quota */ }
}

function record(type, message, options = {}) {
  _log.value.push({
    type,
    message,
    duration: options.duration,
    timestamp: Date.now(),
  })
  if (_log.value.length > MAX_LOG) {
    _log.value = _log.value.slice(-MAX_LOG)
  }
  save()
}

export function useToast() {
  return {
    log: readonly(_log),

    success(msg, options = {}) {
      ElMessage.success({ message: msg, ...options })
      record('success', msg, options)
    },
    error(msg, options = {}) {
      ElMessage.error({ message: msg, ...options })
      record('error', msg, options)
    },
    warning(msg, options = {}) {
      ElMessage.warning({ message: msg, ...options })
      record('warning', msg, options)
    },
    info(msg, options = {}) {
      ElMessage.info({ message: msg, ...options })
      record('info', msg, options)
    },
    notify(opts) {
      ElNotification(opts)
      record('notification', opts.message, opts)
    },
    alert(msg, title = '提示', options = {}) {
      return ElMessageBox.alert(msg, title, { confirmButtonText: '确定', ...options })
    },
    confirm(msg, title = '确认', options = {}) {
      return ElMessageBox.confirm(msg, title, {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
        ...options,
      })
    },
    prompt(msg, title = '输入', options = {}) {
      return ElMessageBox.prompt(msg, title, {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        ...options,
      })
    },

    clearLog() {
      _log.value = []
      save()
    },
  }
}
