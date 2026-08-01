/**
 * ASR Provider 注册中心
 *
 * <p>支持阿里云一句话识别 / 腾讯云实时识别 / Mock
 *
 * @author Mavis
 */

import { ASRProvider, ASRConfig, ASRResult, SDKError, ErrorCode } from '../types';

const providers = new Map<string, ASRProvider>();

export function registerASRProvider(provider: ASRProvider): void {
  providers.set(provider.id, provider);
}

export function getASRProvider(id: string): ASRProvider {
  const p = providers.get(id);
  if (!p) {
    throw new SDKError(ErrorCode.ASR_ERROR, `ASR Provider 未注册: ${id}`, {
      context: { availableIds: Array.from(providers.keys()) },
    });
  }
  return p;
}

export function listASRProviders(): string[] {
  return Array.from(providers.keys());
}

export function unregisterASRProvider(id: string): boolean {
  return providers.delete(id);
}

export type { ASRProvider, ASRConfig, ASRResult };

export { AliyunASRProvider } from './aliyun';
export { TencentASRProvider } from './tencent';
export { MockASRProvider } from './mock';
