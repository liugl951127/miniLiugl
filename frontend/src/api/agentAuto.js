/**
 * 智能体群自动生成 API (V3.4.2)
 * 后端: AutoAgentGroupController → /api/v1/ai/agent-group/auto-impl/*
 */
import http from './http'

/** 一句话生成群组 */
export const generateFromOneLiner = (oneLiner) =>
  http.post('/ai/agent-group/auto-impl/generate', { oneLiner })

/** 按模板生成群组 */
export const generateFromTemplate = (template, description) =>
  http.post('/ai/agent-group/auto-impl/template', { template, description })

/** 列出所有内置模板 */
export const listTemplates = () =>
  http.get('/ai/agent-group/auto-impl/templates')

/** 创建群组到数据库 */
export const createGroup = (data) =>
  http.post('/ai/group/create', data)

/** 列出已保存的群组 */
export const listGroups = () =>
  http.get('/ai/group/list')

/** 执行群组任务 */
export const runGroup = (groupId, subject) =>
  http.post(`/ai/group/${groupId}/run`, { subject })
