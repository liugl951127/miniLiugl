/**
 * @file format.js - V6.8.2+ 统一格式化工具
 *
 * 87 view 重复用到的:
 *   - formatDate / formatTime / formatDateTime (用 Date 而非 dayjs, 测试友好)
 *   - formatNumber (千分位)
 *   - formatFileSize
 *   - truncate
 *   - formatRelativeTime
 */

function pad(n) { return String(n).padStart(2, '0') }

export function formatDate(value, format = 'YYYY-MM-DD') {
  if (!value) return '-'
  const d = new Date(value)
  if (isNaN(d.getTime())) return value
  if (format === 'YYYY-MM-DD') {
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  }
  if (format === 'YYYY-MM-DD HH:mm:ss') {
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  }
  if (format === 'HH:mm:ss') {
    return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  }
  return d.toISOString()
}

export const formatTime = formatDate
export const formatDateTime = formatDate

export function formatNumber(value, precision) {
  if (value === null || value === undefined || value === '') return '-'
  const n = Number(value)
  if (isNaN(n)) return value
  const fixed = precision !== undefined ? n.toFixed(precision) : n
  return Number(fixed).toLocaleString('en-US')
}

export function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return `${(bytes / Math.pow(1024, i)).toFixed(1)} ${units[i]}`
}

export function truncate(str, n = 50) {
  if (!str) return ''
  return str.length > n ? str.slice(0, n) + '...' : str
}

export function formatRelativeTime(value) {
  if (!value) return '-'
  const diff = Date.now() - new Date(value).getTime()
  const sec = Math.floor(diff / 1000)
  if (sec < 60) return `${sec} 秒前`
  const min = Math.floor(sec / 60)
  if (min < 60) return `${min} 分钟前`
  const hr = Math.floor(min / 60)
  if (hr < 24) return `${hr} 小时前`
  const day = Math.floor(hr / 24)
  if (day < 30) return `${day} 天前`
  return formatDate(value)
}
