/**
 * Agent API (V6.4 画布运行, V7.0 Flow②)
 */
import http from './http'

// V7.0: 列出可用的 Agent (沙箱模式)
export const listAgents = () => http.get('/agent/external/agents')

export const trainingApi = {
  // LLM 训练 (V6.5+)
  llmStart(req) {
    return http.post('/ai/training/llm/start', req)
  },
  llmStatus(taskId) {
    return http.get(`/ai/training/llm/status/${taskId}`)
  },
  llmHistory(taskId) {
    return http.get(`/ai/training/llm/history/${taskId}`)
  },
  llmList() {
    return http.get('/ai/training/llm/list')
  },
  llmFeedback(req) {
    return http.post('/ai/training/llm/feedback', req)
  }
}

export const agentApi = {
  // 执行 Agent 任务 (LLM plan 生成 + 执行)
  execute(plan) {
    return http.post('/agent/run', plan)
  },
  // V6.8.1 fix: 工作流存储 — 列出我的工作流 (原 GET /agent/plan → 错误)
  list(params) {
    return http.get('/agent/workflows', { params })
  },
  // V6.8.1 fix: 保存工作流 (Canvas 节点+连线) — 原来误用 LLM plan 接口
  save(workflow) {
    return http.post('/agent/workflows', workflow)
  },
  // V6.8.1 fix: 获取单个工作流
  get(id) {
    return http.get(`/agent/workflows/${id}`)
  },
  // V6.8.1 fix: 删除工作流
  remove(id) {
    return http.delete(`/agent/workflows/${id}`)
  },
  // 部署 Plan (LLM plan → run-plan)
  deploy(id) {
    return http.post('/agent/run-plan', { id })
  },
  // 停止运行中的任务
  stop(id) {
    return http.post('/agent/stop', { id })
  }
}

// ==================== Skill 审批 API (V6.8.1) ====================
export const skillApprovalApi = {
  /** 我的待审批 */
  getMyPending(userId) {
    return http.get('/skill-approval/pending', { params: { userId } })
  },
  /** 所有待审批 (管理员) */
  getAllPending() {
    return http.get('/skill-approval/pending/all')
  },
  /** 查任务审批状态 */
  getByTask(taskId) {
    return http.get(`/skill-approval/task/${taskId}`)
  },
  /** 提交审批请求 */
  submit(data) {
    return http.post('/skill-approval/submit', data)
  },
  /** 审批通过 */
  approve(id, data = {}) {
    return http.post(`/skill-approval/${id}/approve`, data)
  },
  /** 审批拒绝 */
  reject(id, data = {}) {
    return http.post(`/skill-approval/${id}/reject`, data)
  },
  /** 我的审批历史 */
  getHistory(params = {}) {
    return http.get('/skill-approval/history', { params })
  },
}

// ==================== 外部系统 API（通过 API Key 鉴权） ====================
/**
 * 外部系统调用 Agent 编排的 API。
 * 鉴权方式: Header: Authorization: Bearer <api_key>
 *
 * _skipAuth: true 保证拦截器不覆盖外部 API Key。
 *
 * 示例:
 *   const headers = { Authorization: 'Bearer ' + apiKey }
 *   await fetch('/api/v1/agent/external/run', { method: 'POST', headers, body: JSON.stringify({...}) })
 */
export const externalAgentApi = {
  /** 同步运行 Agent（立即返回结果） */
  run(apiKey, req) {
    return http.post('/agent/external/run', req, { _skipAuth: true, headers: { Authorization: `Bearer ${apiKey}` } })
  },
  /** 异步运行（立即返回 taskId，结果通过 Webhook 回调） */
  runAsync(apiKey, req) {
    return http.post('/agent/external/run-async', req, { _skipAuth: true, headers: { Authorization: `Bearer ${apiKey}` } })
  },
  /** 查询任务状态 */
  getTask(apiKey, taskId) {
    return http.get(`/agent/external/tasks/${taskId}`, { _skipAuth: true, headers: { Authorization: `Bearer ${apiKey}` } })
  },
  /** SSE 流式运行 */
  runStream(apiKey, req) {
    return http.post('/agent/external/run-stream', req, { _skipAuth: true, headers: { Authorization: `Bearer ${apiKey}` } })
  },
  /** 列出可调用的 Agent */
  listAgents(apiKey) {
    return http.get('/agent/external/agents', { _skipAuth: true, headers: { Authorization: `Bearer ${apiKey}` } })
  },
  /** 注册 Webhook */
  registerWebhook(apiKey, req) {
    return http.post('/agent/external/webhook', req, { _skipAuth: true, headers: { Authorization: `Bearer ${apiKey}` } })
  },
  /** 列出 Webhook */
  listWebhooks(apiKey) {
    return http.get('/agent/external/webhooks', { _skipAuth: true, headers: { Authorization: `Bearer ${apiKey}` } })
  },
  /** 删除 Webhook */
  deleteWebhook(apiKey, id) {
    return http.delete(`/agent/external/webhook/${id}`, { _skipAuth: true, headers: { Authorization: `Bearer ${apiKey}` } })
  },
  /** 测试 Webhook 连通性 */
  pingWebhook(apiKey, url) {
    return http.get('/agent/external/webhook/ping', { _skipAuth: true, params: { url }, headers: { Authorization: `Bearer ${apiKey}` } })
  },
}

export default agentApi
