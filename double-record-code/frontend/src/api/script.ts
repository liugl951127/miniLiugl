/**
 * 话术 API 服务
 */
import { get, post } from '@/utils/request';
import type { Script, ScriptNode, NodeResult, SubmitNodeResultDTO } from '@/types';

export const scriptApi = {
  /**
   * 拉取话术模板(从服务端获取最新版本)
   */
  pull(productType: number, productId?: number): Promise<Script> {
    return get<Script>('/v1/script/pull', { productType, productId });
  },

  /**
   * 按 ID 查询话术详情
   */
  getById(scriptId: number): Promise<Script> {
    return get<Script>(`/v1/script/${scriptId}`);
  },

  /**
   * 校验本地话术文件完整性(MD5 校验)
   */
  verifyIntegrity(scriptId: number, localHash: string): Promise<{ valid: boolean; remoteHash: string }> {
    return get<{ valid: boolean; remoteHash: string }>(`/v1/script/${scriptId}/verify`, { localHash });
  },

  /**
   * 提交节点执行结果
   */
  submitNodeResult(data: SubmitNodeResultDTO): Promise<NodeResult> {
    return post<NodeResult>('/v1/script/node/submit', data);
  },

  /**
   * 批量提交节点结果(用于断网重连)
   */
  submitBatchResults(sessionId: number, results: NodeResult[]): Promise<void> {
    return post<void>('/v1/script/node/batch-submit', { sessionId, results });
  },

  /**
   * 查询话术的所有节点
   */
  listNodes(scriptId: number): Promise<ScriptNode[]> {
    return get<ScriptNode[]>(`/v1/script/${scriptId}/nodes`);
  },

  /**
   * 查询话术的版本历史
   */
  listVersions(scriptCode: string): Promise<Script[]> {
    return get<Script[]>('/v1/script/versions', { scriptCode });
  },
};
