/**
 * @file forge.js - Agent Forge API 调用层 (V2.0)
 *
 * 对应后端模块: minimax-deployer
 * Base URL: /api/v1/forge
 *
 * V2.0 新增 API:
 *  - POST   /projects               - 创建项目 (含需求解析)
 *  - GET    /projects               - 列出我的项目
 *  - GET    /projects/{id}          - 项目详情
 *  - DELETE /projects/{id}          - 删除项目
 *
 *  - POST   /releases               - 创建 release
 *  - GET    /releases/{id}          - release 详情
 *  - GET    /releases?projectId=X   - 项目的所有 release
 *  - POST   /releases/{id}/deploy   - 触发部署
 *  - POST   /releases/{id}/rollback/{targetId} - 回滚
 *  - GET    /releases/{from}/diff/{to} - 差异
 *
 *  - GET    /templates              - 全部模板
 *  - GET    /templates?industry=xxx - 按行业
 *  - GET    /templates/code/{code}  - 按 code
 *
 *  - GET    /deployments/{id}/stream - SSE 实时状态
 */
import http from './http'

// ============ 项目 ============
export const createForgeProject = (data) => http.post('/forge/projects', data)

export const listForgeProjects = (params) => http.get('/forge/projects', { params })

export const getForgeProject = (id) => http.get(`/forge/projects/${id}`)

export const deleteForgeProject = (id) => http.delete(`/forge/projects/${id}`)

// ============ Release ============
export const createRelease = (data) => http.post('/forge/releases', data)

export const getRelease = (id) => http.get(`/forge/releases/${id}`)

export const listReleases = (projectId) => http.get('/forge/releases', { params: { projectId } })

export const triggerDeploy = (id) => http.post(`/forge/releases/${id}/deploy`)

export const rollbackRelease = (id, targetId) => http.post(`/forge/releases/${id}/rollback/${targetId}`)

export const diffReleases = (fromId, toId) => http.get(`/forge/releases/${fromId}/diff/${toId}`)

// ============ 模板 ============
export const listTemplates = (industry) => http.get('/forge/templates', { params: industry ? { industry } : {} })

export const getTemplate = (id) => http.get(`/forge/templates/${id}`)

export const getTemplateByCode = (code) => http.get(`/forge/templates/code/${code}`)

// ============ SSE 部署状态 ============
export const subscribeDeployment = (deploymentId, onEvent) => {
  const url = `/api/v1/forge/deployments/${deploymentId}/stream`
  const eventSource = new EventSource(url, { withCredentials: true })
  const events = ['stage_start', 'stage_done', 'log', 'done']
  events.forEach(evt => {
    eventSource.addEventListener(evt, (e) => {
      try { onEvent(evt, JSON.parse(e.data)) }
      catch (err) { onEvent(evt, e.data) }
    })
  })
  return () => eventSource.close()
}
