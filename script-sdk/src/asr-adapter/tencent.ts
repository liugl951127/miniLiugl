/**
 * 腾讯云 ASR Provider
 *
 * <p>一句话识别 API
 *
 * @author Mavis
 */

import { ASRProvider, ASRConfig, ASRResult, SDKError, ErrorCode } from '../types';
import { sm3Hex } from '../core/hash';

export class TencentASRProvider implements ASRProvider {
  readonly id: string = 'tencent';
  readonly name: string = '腾讯云一句话识别';
  readonly streaming: boolean = false;

  private secretId: string;
  private secretKey: string;
  private engineType: string = '16k_zh'; // 16k 中文
  private endpoint: string = 'asr.tencentcloudapi.com';

  constructor(config: Record<string, string>) {
    this.secretId = config['secretId'] || '';
    this.secretKey = config['secretKey'] || '';
    this.engineType = config['engineType'] || '16k_zh';
    if (!this.secretId || !this.secretKey) {
      throw new SDKError(ErrorCode.CONFIG_ERROR, '腾讯云 ASR 缺少 secretId/secretKey');
    }
  }

  async recognize(config: ASRConfig): Promise<ASRResult> {
    // 把 Uint8Array 转 base64
    const audioBase64 = btoa(String.fromCharCode(...config.audio));
    const audioHash = sm3Hex(config.audio);

    const params = {
      EngSerViceType: this.engineType,
      SourceType: 1, // 1=音频数据
      VoiceFormat: config.format === 'pcm' ? 'pcm' : config.format,
      UsrAudioKey: `sdk-${Date.now()}`,
      Data: audioBase64,
      DataLen: config.audio.length,
    };

    try {
      const response = await fetch(`https://${this.endpoint}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-TC-Action': 'SentenceRecognition',
          'X-TC-Version': '2019-06-14',
          // 实际需要 TC3-HMAC-SHA256 签名
        },
        body: JSON.stringify(params),
      });
      if (!response.ok) {
        throw new SDKError(ErrorCode.ASR_ERROR, `腾讯云 ASR 失败: ${response.status}`);
      }
      const data = (await response.json()) as {
        Response?: { Result?: string; AudioDuration?: number };
      };
      const text = data.Response?.Result || '';
      const audioDuration = data.Response?.AudioDuration
        ? data.Response.AudioDuration * 1000
        : 0;

      return {
        text,
        confidence: 0.9,
        sentences: [
          { text, startTime: 0, endTime: audioDuration, confidence: 0.9 },
        ],
        isFinal: true,
        audioDuration,
        audioHash,
      };
    } catch (e) {
      if (e instanceof SDKError) throw e;
      throw new SDKError(ErrorCode.ASR_ERROR, `腾讯云 ASR 调用失败: ${(e as Error).message}`, {
        cause: e as Error,
      });
    }
  }

  async healthCheck(): Promise<boolean> {
    return Boolean(this.secretId) && Boolean(this.secretKey);
  }
}
