/**
 * 双录会话 API 服务
 */
import { get, post, upload } from '@/utils/request';
import type { Session, StartSessionDTO, NodeResult, QualityResult } from '@/types';
import type { UploadVideoChunkDTO } from '@/types';

export const sessionApi = {
  /**
   * 启动新会话
   */
  start(data: StartSessionDTO): Promise<Session> {
    return post<Session>('/v1/session/start', data);
  },

  /**
   * 获取会话详情
   */
  getById(sessionId: number): Promise<Session> {
    return get<Session>(`/v1/session/${sessionId}`);
  },

  /**
   * 上传视频分片
   */
  uploadChunk(data: UploadVideoChunkDTO): Promise<{ etag: string; url: string }> {
    const formData = new FormData();
    formData.append('sessionId', String(data.sessionId));
    formData.append('chunkIndex', String(data.chunkIndex));
    formData.append('totalChunks', String(data.totalChunks));
    formData.append('startTime', String(data.startTime));
    formData.append('endTime', String(data.endTime));
    formData.append('file', data.chunkData, `chunk-${data.chunkIndex}.webm`);
    return upload<{ etag: string; url: string }>('/v1/session/upload', formData);
  },

  /**
   * 合并分片(完成录制时调用)
   */
  merge(sessionId: number): Promise<{ videoUrl: string; videoHash: string; duration: number }> {
    return post<{ videoUrl: string; videoHash: string; duration: number }>(`/v1/session/${sessionId}/merge`, {});
  },

  /**
   * 暂停会话
   */
  pause(sessionId: number, reason?: string): Promise<Session> {
    return post<Session>(`/v1/session/${sessionId}/pause`, { reason });
  },

  /**
   * 恢复会话
   */
  resume(sessionId: number): Promise<Session> {
    return post<Session>(`/v1/session/${sessionId}/resume`, {});
  },

  /**
   * 结束会话
   */
  finish(sessionId: number, nodeResults: NodeResult[]): Promise<Session> {
    return post<Session>(`/v1/session/${sessionId}/finish`, { nodeResults });
  },

  /**
   * 中断会话(异常处理)
   */
  abort(sessionId: number, reason: string, canResume: boolean): Promise<Session> {
    return post<Session>(`/v1/session/${sessionId}/abort`, { reason, canResume });
  },

  /**
   * 续接中断的会话
   */
  resumeFrom(sessionId: number, fromNodeCode: string): Promise<Session> {
    return post<Session>(`/v1/session/${sessionId}/resume-from`, { fromNodeCode });
  },

  /**
   * 触发质检
   */
  triggerQA(sessionId: number): Promise<QualityResult> {
    return post<QualityResult>(`/v1/session/${sessionId}/qa`, {});
  },

  /**
   * 查询质检结果
   */
  getQAResult(qaId: number): Promise<QualityResult> {
    return get<QualityResult>(`/v1/quality/${qaId}`);
  },

  /**
   * 获取视频播放 URL(临时签名)
   */
  getPlayUrl(sessionId: number, expireSeconds = 3600): Promise<{ url: string; expireAt: string }> {
    return get<{ url: string; expireAt: string }>(`/v1/session/${sessionId}/play-url`, { expire: expireSeconds });
  },
};
