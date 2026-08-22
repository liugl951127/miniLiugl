/**
 * @file kg API 调用层 (V7.0+)
 *
 * 对应后端模块: minimax-agent
 * 接口覆盖: 知识图谱实体 / 关系 CRUD + 搜索 + 邻居 + 路径
 *
 *   POST   /api/v1/agent/kg/entities          upsertEntity
 *   GET    /api/v1/agent/kg/entities/{id}     getEntity
 *   GET    /api/v1/agent/kg/entities/search   searchEntities
 *   DELETE /api/v1/agent/kg/entities/{id}     deleteEntity
 *   POST   /api/v1/agent/kg/relations         createRelation
 *   GET    /api/v1/agent/kg/entities/{id}/neighbors  neighbors
 *   GET    /api/v1/agent/kg/entities/{id}/2hop  twoHopNeighbors
 *   GET    /api/v1/agent/kg/path              shortestPath
 */
import http from './http'

// ==================== 实体 CRUD ====================

/**
 * 创建或更新实体
 * @param {object} body { userId?, name, type, description?, aliases?, importance? }
 */
export const kgUpsertEntity = (body) => {
  return http.post('/agent/kg/entities', body)
}

/** 获取实体详情 */
export const kgGetEntity = (id, userId) =>
  http.get(`/agent/kg/entities/${id}`, { params: { userId } })

/** 搜索实体 */
export const kgSearchEntities = (userId, keyword, limit = 20) =>
  http.get('/agent/kg/entities/search', { params: { userId, keyword, limit } })

/** 删除实体 */
export const kgDeleteEntity = (id, userId) =>
  http.delete(`/agent/kg/entities/${id}`, { params: { userId } })

// ==================== 关系 CRUD ====================

/**
 * 创建实体关系
 * @param {object} body { userId?, fromId, toId, type, description?, weight? }
 */
export const kgCreateRelation = (body) => {
  return http.post('/agent/kg/relations', body)
}

// ==================== 图遍历 ====================

/** 1 跳邻居 */
export const kgNeighbors = (id, userId) =>
  http.get(`/agent/kg/entities/${id}/neighbors`, { params: { userId } })

/** 2 跳邻居 */
export const kgTwoHop = (id, userId) =>
  http.get(`/agent/kg/entities/${id}/2hop`, { params: { userId } })

/** 最短路径 */
export const kgPath = (userId, from, to) =>
  http.get('/agent/kg/path', { params: { userId, from, to } })

// ==================== T2: KB 知识图谱 (基于文档抽取) ====================

/**
 * 从知识库文档中构建图谱 (实体+关系抽取)
 * @param {number} kbId
 */
export const buildKg = (kbId) =>
  http.post(`/rag/kb/${kbId}/kg/build`)

/**
 * 获取知识库全量图谱 (实体+关系)
 * @param {number} kbId
 * @returns {Promise<{entities:Array, relations:Array}>}
 */
export const getKg = (kbId) =>
  http.get(`/rag/kb/${kbId}/kg`)

/**
 * 获取图谱统计
 * @param {number} kbId
 * @returns {Promise<{entities:number, relations:number, types:number}>}
 */
export const getKgStats = (kbId) =>
  http.get(`/rag/kb/${kbId}/kg/stats`)

/**
 * 在图谱中搜索实体
 * @param {number} kbId
 * @param {string} kw
 */
export const searchKg = (kbId, kw) =>
  http.get(`/rag/kb/${kbId}/kg/search`, { params: { kw } })

/**
 * 关系推理: 找两个实体之间的路径
 * @param {string} src
 * @param {string} tgt
 * @returns {Promise<{paths:Array<{path:string[],hops:number}>}>}
 */
export const reasonKg = (src, tgt) =>
  http.get('/rag/kg/reason', { params: { src, tgt } })

/**
 * 清除知识库图谱
 * @param {number} kbId
 */
export const clearKg = (kbId) =>
  http.delete(`/rag/kb/${kbId}/kg`)

// ==================== 批量导入 ====================

/**
 * 批量导入实体 (后端暂无独立 batch 接口, 此处逐条 upsert)
 * 失败时抛错, 调用方应捕获并展示错误
 *
 * @param {number|null} userId
 * @param {Array<{name:string, type:string, description?:string, aliases?:string}>} entities
 * @returns {Promise<{succeeded:number, failed:Array<{entity:object, error:string}>}>}
 */
export const kgBatchImportEntities = async (userId, entities) => {
  const result = { succeeded: 0, failed: [] }
  if (!Array.isArray(entities) || entities.length === 0) return result

  for (const entity of entities) {
    try {
      await kgUpsertEntity({
        userId,
        name: entity.name,
        type: entity.type || 'OTHER',
        description: entity.description || '',
        aliases: entity.aliases || '',
        importance: entity.importance || null
      })
      result.succeeded++
    } catch (e) {
      result.failed.push({
        entity,
        error: e?.message || '未知错误'
      })
    }
  }
  return result
}

/**
 * 批量导入关系
 * @param {number|null} userId
 * @param {Array<{fromId:number, toId:number, type:string, description?:string, weight?:number}>} relations
 * @returns {Promise<{succeeded:number, failed:Array<{relation:object, error:string}>}>}
 */
export const kgBatchImportRelations = async (userId, relations) => {
  const result = { succeeded: 0, failed: [] }
  if (!Array.isArray(relations) || relations.length === 0) return result

  for (const relation of relations) {
    try {
      await kgCreateRelation({
        userId,
        fromId: relation.fromId,
        toId: relation.toId,
        type: relation.type || relation.label || '关联',
        description: relation.description || '',
        weight: relation.weight || 1.0
      })
      result.succeeded++
    } catch (e) {
      result.failed.push({
        relation,
        error: e?.message || '未知错误'
      })
    }
  }
  return result
}

// 默认导出
const kgApi = {
  kgUpsertEntity,
  kgGetEntity,
  kgSearchEntities,
  kgDeleteEntity,
  kgCreateRelation,
  kgNeighbors,
  kgTwoHop,
  kgPath,
  kgBatchImportEntities,
  kgBatchImportRelations,
  // T2: KB 知识图谱
  buildKg,
  getKg,
  getKgStats,
  searchKg,
  reasonKg,
  clearKg
}
export default kgApi
export { kgApi }
