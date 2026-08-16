/**
 * Agent API (V6.4 画布运行, V7.0 Flow②)
 */
import http from './http'
import { useUserStore } from '@/store/user'

// V6.9: 统一 API 基础路径 → vite proxy → backend:8090
const AGENT_BASE = '/api/v1/agent'
const SKILL_BASE = '/api/v1/skill-approval'

// V7.0: 列出可用的 Agent (沙箱模式)
export const listAgents = () => http.get(`${AGENT_BASE}/external/agents`)

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

// ==================== 多智能体协作 API (V6.9) ====================
export const multiAgentApi = {
  /** 同步执行：Planner → Executor → Critic 三角色协作 */
  run(params) {
    return http.post(`${AGENT_BASE}/multi/run`, params)
  },
  /**
   * XHR 流式（POST + text/event-stream）
   * 事件类型: multi-agent-start / planner-start / planner-plan /
   *          executor-step / executor-result / critic-eval /
   *          critic-result / critic-retry / final / done / error
   */
  xhrStream(params, onEvent) {
    // V6.9: 用 Pinia userStore（和 axios 拦截器一致），不用 localStorage.getItem('token')
    const userStore = useUserStore()
    const token = userStore.accessToken || ''
    return fetch(`${AGENT_BASE}/multi/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
      body: JSON.stringify(params)
    }).then(r => {
      if (!r.ok) throw new Error(`HTTP ${r.status} ${r.statusText}`)
      const reader = r.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      // 当前累积的 event name（来自 "event: xxx" 行）
      let currentEvent = ''
      function pump() {
        return reader.read().then(({ done, value }) => {
          if (done) return
          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''
          for (const line of lines) {
            const trimmed = line.trim()
            if (trimmed.startsWith('event:')) {
              // SSE event: 行，事件名
              currentEvent = trimmed.slice(6).trim()
            } else if (trimmed.startsWith('data:')) {
              const data = trimmed.slice(5).trim()
              if (!data || data === '[DONE]') continue
              try {
                const json = JSON.parse(data)
                // Spring @SendTo produces {event="xxx", data={...}} in the JSON
                const eventName = json.event || currentEvent || 'message'
                const eventData = json.data !== undefined ? json.data : json
                onEvent(eventName, eventData, json)
              } catch {
                // 非 JSON 原始数据，直接当 message 事件
                onEvent(currentEvent || 'message', data, {})
              }
              currentEvent = ''
            }
          }
          return pump()
        })
      }
      return pump()
    })
  },
  /** 单独 Planner：生成执行计划 */
  plan(params) {
    return http.post(`${AGENT_BASE}/multi/plan`, params)
  },
  /** 单独 Critic：评估执行结果 */
  critic(params) {
    return http.post(`${AGENT_BASE}/multi/critic`, params)
  }
}

export const agentApi = {
  // 执行 Agent 任务 (LLM plan 生成 + 执行)
  execute(plan) {
    return http.post(`${AGENT_BASE}/run`, plan)
  },
  // V6.8.1 fix: 工作流存储 — 列出我的工作流 (原 GET /agent/plan → 错误)
  list(params) {
    return http.get(`${AGENT_BASE}/workflows`, { params })
  },
  // V6.8.1 fix: 保存工作流 (Canvas 节点+连线) — 原来误用 LLM plan 接口
  save(workflow) {
    return http.post(`${AGENT_BASE}/workflows`, workflow)
  },
  // V6.8.1 fix: 获取单个工作流
  get(id) {
    return http.get(`${AGENT_BASE}/workflows/${id}`)
  },
  // V6.8.1 fix: 删除工作流
  remove(id) {
    return http.delete(`${AGENT_BASE}/workflows/${id}`)
  },
  // 部署 Plan (LLM plan → run-plan)
  deploy(id) {
    return http.post(`${AGENT_BASE}/run-plan`, { id })
  },
  // 停止运行中的任务
  stop(id) {
    return http.post(`${AGENT_BASE}/stop`, { id })
  }
}

// ==================== Skill 审批 API (V6.8.1) ====================
export const skillApprovalApi = {
  /** 我的待审批 */
  getMyPending(userId) {
    return http.get(`${SKILL_BASE}/pending`, { params: { userId } })
  },
  /** 所有待审批 (管理员) */
  getAllPending() {
    return http.get(`${SKILL_BASE}/pending/all`)
  },
  /** 查任务审批状态 */
  getByTask(taskId) {
    return http.get(`${SKILL_BASE}/task/${taskId}`)
  },
  /** 提交审批请求 */
  submit(data) {
    return http.post(`${SKILL_BASE}/submit`, data)
  },
  /** 审批通过 */
  approve(id, data = {}) {
    return http.post(`${SKILL_BASE}/${id}/approve`, data)
  },
  /** 审批拒绝 */
  reject(id, data = {}) {
    return http.post(`${SKILL_BASE}/${id}/reject`, data)
  },
  /** 我的审批历史 */
  getHistory(params = {}) {
    return http.get(`${SKILL_BASE}/history`, { params })
  },
}

// ==================== 外部系统 API（通过 API Key 鉴权） ====================
export const externalAgentApi = {
  run(apiKey, req) {
    return http.post(`${AGENT_BASE}/external/run`, req, { _skipAuth: true, headers: { Authorization: `Bearer ${apiKey}` } })
  },
  runAsync(apiKey, req) {
    return http.post(`${AGENT_BASE}/external/run-async`, req, { _skipAuth: true, headers: { Authorization: `Bearer ${apiKey}` } })
  },
  getTask(apiKey, taskId) {
    return http.get(`${AGENT_BASE}/external/tasks/${taskId}`, { _skipAuth: true, headers: { Authorization: `Bearer ${apiKey}` } })
  },
  runStream(apiKey, req) {
    return http.post(`${AGENT_BASE}/external/run-stream`, req, { _skipAuth: true, headers: { Authorization: `Bearer ${apiKey}` } })
  },
  listAgents(apiKey) {
    return http.get(`${AGENT_BASE}/external/agents`, { _skipAuth: true, headers: { Authorization: `Bearer ${apiKey}` } })
  },
  registerWebhook(apiKey, req) {
    return http.post(`${AGENT_BASE}/external/webhook`, req, { _skipAuth: true, headers: { Authorization: `Bearer ${apiKey}` } })
  },
  listWebhooks(apiKey) {
    return http.get(`${AGENT_BASE}/external/webhooks`, { _skipAuth: true, headers: { Authorization: `Bearer ${apiKey}` } })
  },
  deleteWebhook(apiKey, id) {
    return http.delete(`${AGENT_BASE}/external/webhook/${id}`, { _skipAuth: true, headers: { Authorization: `Bearer ${apiKey}` } })
  },
  pingWebhook(apiKey, url) {
    return http.get(`${AGENT_BASE}/external/webhook/ping`, { _skipAuth: true, params: { url }, headers: { Authorization: `Bearer ${apiKey}` } })
  },
}

export default agentApi
