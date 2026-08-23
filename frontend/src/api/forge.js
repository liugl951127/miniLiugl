/**
 * @file forge.js - Agent Forge API 调用层 (V4.0)
 *
 * V4.0 清理:
 *  - 删 /deploy-gitops (V3.0 假功能), 统一 /deploy 按 deploy_target 路由
 *  - 删 /argocd/applications/{name} (V3.0 假查询), 状态走 /deployments/{id}
 *  - 删 SSE EventSource (V2.0 模拟推送), 改为 /deployments/{id}/logs 分页拉取
 *  - LLM 模型选择简化为 3 个真实可用项
 */
import http from './http'

// ============ 项目 ============
export const createForgeProject = (data) => http.post('/forge/projects', data)
export const listForgeProjects = (params) => http.get('/forge/projects', { params })
export const getForgeProject = (id) => http.get(`/forge/projects/${id}`)
export const deleteForgeProject = (id) => http.delete(`/forge/projects/${id}`)
export const getProjectAgents = (id) => http.get(`/forge/projects/${id}/agents`)
export const getProjectWorkflow = (id) => http.get(`/forge/projects/${id}/workflow`)

// ============ Release ============
export const createRelease = (data) => http.post('/forge/releases', data)
export const getRelease = (id) => http.get(`/forge/releases/${id}`)
export const listReleases = (projectId) => http.get('/forge/releases', { params: { projectId } })
export const getReleaseManifests = (id) => http.get(`/forge/releases/${id}/manifests`)
export const triggerDeploy = (id) => http.post(`/forge/releases/${id}/deploy`)
export const rollbackRelease = (id, targetId) => http.post(`/forge/releases/${id}/rollback/${targetId}`)
export const diffReleases = (fromId, toId) => http.get(`/forge/releases/${fromId}/diff/${toId}`)

// ============ Deployment ============
export const getDeployment = (id) => http.get(`/forge/deployments/${id}`)
export const getDeploymentLogs = (id, params) => http.get(`/forge/deployments/${id}/logs`, { params })

// ============ 模板 ============
export const listTemplates = (industry) => http.get('/forge/templates', { params: industry ? { industry } : {} })
export const getTemplate = (id) => http.get(`/forge/templates/${id}`)
export const getTemplateByCode = (code) => http.get(`/forge/templates/code/${code}`)

// ============ V4.0 LLM 模型 (3 个真实可用) ============
export const PARSER_MODELS = [
  { code: 'qwen2.5-0.5b-instruct', name: 'Qwen2.5-0.5B (本地 ONNX)',  desc: 'minimax-ai, 488MB, ~1s' },
  { code: 'qwen2.5-7b-instruct',   name: 'Qwen2.5-7B (云端)',         desc: 'minimax-ai, ~3s' },
  { code: 'rule-engine',           name: '规则引擎 (离线 fallback)', desc: '无需 LLM, 关键词 + 行业模板' }
]
