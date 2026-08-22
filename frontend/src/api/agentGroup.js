/**
 * @file api/agentGroup.js - 智能体群编排 API (T1-frontend-designer)
 *
 * 后端: AgentGroupOrchestrationController → /api/v1/agent-group
 *
 * 端点:
 *   - GET    /{groupId}/members
 *   - POST   /{groupId}/members
 *   - PUT    /{groupId}/members/{memberId}
 *   - DELETE /{groupId}/members/{memberId}
 *   - PUT    /{groupId}/members/reorder
 *   - POST   /{groupId}/run           (SSE)
 *   - GET    /strategies
 *
 * T2+: SSE 流式接口 (runStream) 使用 fetch + reader
 *  - 带 Authorization Bearer (token 鉴权)
 *  - 带 X-User-Id (从 store 读, 兜底 localStorage / '1')
 *  - 401 → 自动调 refresh → 重试 1 次
 */
import http from './http'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import router from '@/router'

const PREFIX = '/agent-group'

// SSE 用: 优先 store.profile → 兜底 localStorage → 最后 '1'
function resolveUserId() {
  try {
    const store = useUserStore()
    const fromStore = store.profile?.userId ?? store.profile?.id
    if (fromStore) return String(fromStore)
  } catch (_) {}
  try {
    const ls = localStorage.getItem('userId')
    if (ls) return ls
  } catch (_) {}
  return '1'
}

// 默认策略 (后端 /strategies 不可用时兜底)
const DEFAULT_STRATEGIES = [
  { value: 'PIPELINE', label: '顺序执行', description: '上一步输出作下一步输入' },
  { value: 'PARALLEL', label: '并行执行', description: '多视角并行, 合并全部结果' },
  { value: 'DEBATE',   label: '辩论',     description: '多方案比较, 选最佳' },
]

// 默认群组 (后端 /list 不可用时兜底)
const DEFAULT_GROUPS = [{ id: 1, name: '默认群' }]

// T2+: 401 refresh 防风暴 (5s 内只跳一次登录)
let sseLast401At = 0

export const agentGroupApi = {
  /**
   * 列出全部群组 (后端 /list 不可用时返回默认群)
   */
  listGroups: () => http.get(`${PREFIX}/list`).catch(() => DEFAULT_GROUPS),

  /**
   * 列出群组全部成员
   * @param {string|number} groupId
   */
  listMembers: (groupId) => http.get(`${PREFIX}/${groupId}/members`),

  /**
   * 新增成员
   * @param {string|number} groupId
   * @param {object} data  { agentCode, role, position, configJson, enabled }
   */
  addMember: (groupId, data) => http.post(`${PREFIX}/${groupId}/members`, data),

  /**
   * 更新成员
   * @param {string|number} groupId
   * @param {string|number} memberId
   * @param {object} data
   */
  updateMember: (groupId, memberId, data) =>
    http.put(`${PREFIX}/${groupId}/members/${memberId}`, data),

  /**
   * 删除成员
   */
  removeMember: (groupId, memberId) =>
    http.delete(`${PREFIX}/${groupId}/members/${memberId}`),

  /**
   * 重排成员 (按入参顺序从 0 开始重写 position)
   * @param {string|number} groupId
   * @param {Array<{memberId, position}>} order
   */
  reorder: (groupId, order) =>
    http.put(`${PREFIX}/${groupId}/members/reorder`, order),

  /**
   * 列出可用策略 (兜底返回 DEFAULT_STRATEGIES)
   */
  getStrategies: () =>
    http.get(`${PREFIX}/strategies`).catch(() => DEFAULT_STRATEGIES),

  /**
   * SSE 流式执行群组任务 (T2+ fetch + reader 鉴权修复)
   * - 内部处理 401: 调 userStore.refreshAccessToken() 拿新 token 后重试 1 次
   * - 自动注入 Authorization + X-User-Id
   * - refresh 失败 → 提示并跳登录 (与 http.js 一致)
   *
   * @param {string|number} groupId
   * @param {object} body { goal, strategy, tools }
   * @returns {Promise<Response>} fetch 的 Response 对象 (调用方负责读 body.getReader())
   */
  runStream: async (groupId, body) => {
    const url = `/api/v1/agent-group/${groupId}/run`
    const doFetch = (token) => fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
        'X-User-Id': resolveUserId(),
        'X-Trace-Id': 'fe-sse-' + Date.now().toString(36) + Math.random().toString(36).slice(2, 8),
        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
      },
      body: JSON.stringify(body)
    })
    // 第一次请求
    const userStore = useUserStore()
    let res = await doFetch(userStore.accessToken)
    if (res.status !== 401) return res
    // 401 → refresh → 重试 1 次
    if (!userStore.refreshToken) return res
    try {
      const newToken = await userStore.refreshAccessToken()
      if (newToken) {
        res = await doFetch(newToken)
        return res
      }
    } catch (_) {
      // refresh 失败, 走下面跳登录
    }
    // 5s 内只跳一次
    const now = Date.now()
    if (now - sseLast401At < 10_000) return res
    sseLast401At = now
    ElMessage.warning('登录已过期, 正在跳转登录页...')
    try { await userStore.logout() } catch (_) {}
    if (router.currentRoute.value.path !== '/login') {
      router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
    }
    return res
  }
}

export default agentGroupApi
