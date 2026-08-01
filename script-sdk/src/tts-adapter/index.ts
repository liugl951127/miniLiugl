/**
 * TTS Provider 注册中心
 *
 * <p>支持多厂商热插拔:阿里云 / 腾讯云 / 科大讯飞 / Mock
 *
 * @author Mavis
 */

import { TTSProvider, TTSConfig, TTSResult, SDKError, ErrorCode } from '../types';

/**
 * Provider 注册表
 */
const providers = new Map<string, TTSProvider>();

/**
 * 注册 TTS Provider
 */
export function registerTTSProvider(provider: TTSProvider): void {
  providers.set(provider.id, provider);
}

/**
 * 获取 TTS Provider
 */
export function getTTSProvider(id: string): TTSProvider {
  const p = providers.get(id);
  if (!p) {
    throw new SDKError(ErrorCode.TTS_ERROR, `TTS Provider 未注册: ${id}`, {
      context: { availableIds: Array.from(providers.keys()) },
    });
  }
  return p;
}

/**
 * 列出已注册的 Provider
 */
export function listTTSProviders(): string[] {
  return Array.from(providers.keys());
}

/**
 * 取消注册
 */
export function unregisterTTSProvider(id: string): boolean {
  return providers.delete(id);
}

// 类型导出
export type { TTSProvider, TTSConfig, TTSResult };

// 重新导出实现
export { AliyunTTSProvider } from './aliyun';
export { TencentTTSProvider } from './tencent';
export { MockTTSProvider } from './mock';
