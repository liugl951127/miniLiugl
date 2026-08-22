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
 */
import http from './http'

const PREFIX = '/agent-group'

// SSE 用: 优先从 localStorage 取 userId, 兜底 '1'
const USER_ID = () => localStorage.getItem('userId') || '1'

// 默认策略 (后端 /strategies 不可用时兜底)
const DEFAULT_STRATEGIES = [
  { value: 'PIPELINE', label: '顺序执行', description: '上一步输出作下一步输入' },
  { value: 'PARALLEL', label: '并行执行', description: '多视角并行, 合并全部结果' },
  { value: 'DEBATE',   label: '辩论',     description: '多方案比较, 选最佳' },
]

// 默认群组 (后端 /list 不可用时兜底)
const DEFAULT_GROUPS = [{ id: 1, name: '默认群' }]

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
   * SSE 流式执行群组任务
   * @param {string|number} groupId
   * @param {object} body { goal, strategy, tools }
   * @returns {Promise<Response>} fetch 的 Response 对象 (调用方负责读 body.getReader())
   */
  runStream: (groupId, body) => {
    const url = `/api/v1/agent-group/${groupId}/run`
    return fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-User-Id': USER_ID() },
      body: JSON.stringify(body)
    })
  }
}

export default agentGroupApi
