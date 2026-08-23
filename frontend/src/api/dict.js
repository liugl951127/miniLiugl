/**
 * 字典 API (V8.0.3) — 统一下拉数据源
 *
 * 之前各页 hardcoded <el-option>, 现在统一从 /api/v1/dict/{type} 拉
 */
import http from './http'

export const dictApi = {
  k8sClusters: () => http.get('/dict/k8s-clusters'),
  agentRoles: () => http.get('/dict/agent-roles'),
  alertChannels: () => http.get('/dict/alert-channels'),
  industries: () => http.get('/dict/industries'),
  kbStrategies: () => http.get('/dict/kb-strategies'),
  models: () => http.get('/dict/models')
}
