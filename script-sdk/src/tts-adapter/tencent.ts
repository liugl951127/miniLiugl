/**
 * 腾讯云 TTS Provider
 *
 * <p>支持长文本 RESTful 接口
 *
 * <p>配置:
 * <ul>
 *   <li>secretId
 *   <li>secretKey
 *   <li>voiceType - 音色 ID,默认 1001(智瑜)
 *   <li>region - 默认 ap-guangzhou
 * </ul>
 *
 * @author Mavis
 */

import { TTSProvider, TTSConfig, TTSResult, SDKError, ErrorCode } from '../types';
import { sm3Hex } from '../core/hash';

export class TencentTTSProvider implements TTSProvider {
  readonly id: string = 'tencent';
  readonly name: string = '腾讯云智聆';
  readonly streaming: boolean = false;

  private secretId: string;
  private secretKey: string;
  private region: string;
  private endpoint: string = 'tts.tencentcloudapi.com';

  constructor(config: Record<string, string>) {
    this.secretId = config['secretId'] || '';
    this.secretKey = config['secretKey'] || '';
    this.region = config['region'] || 'ap-guangzhou';

    if (!this.secretId || !this.secretKey) {
      throw new SDKError(ErrorCode.CONFIG_ERROR, '腾讯云 TTS 缺少 secretId/secretKey');
    }
  }

  async synthesize(config: TTSConfig): Promise<TTSResult> {
    const voiceType = parseInt(config.voiceId || '1001', 10);
    const sampleRate = config.sampleRate || 16000;
    const codec = config.format || 'mp3';

    const params = {
      Text: config.text,
      SessionId: `sdk-${Date.now()}`,
      ModelType: 1,
      VoiceType: voiceType,
      Codec: codec,
      SampleRate: sampleRate,
      SpeechRate: Math.round((config.speed || 1) * 0), // 0 正常
      Volume: config.volume || 5,
      PitchRate: config.pitch || 0,
    };

    // 简化:生产应使用 TC3-HMAC-SHA256 签名
    try {
      const response = await fetch(`https://${this.endpoint}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-TC-Action': 'TextToVoice',
          'X-TC-Version': '2019-08-23',
          'X-TC-Region': this.region,
          // 实际应携带 Authorization 头,需要服务端 SDK
        },
        body: JSON.stringify(params),
      });

      if (!response.ok) {
        throw new SDKError(ErrorCode.TTS_ERROR, `腾讯云 TTS 失败: ${response.status}`);
      }

      const buffer = await response.arrayBuffer();
      const audio = new Uint8Array(buffer);
      const audioHash = sm3Hex(audio);
      const duration = Math.max(500, (audio.length / 2000) * 1000);

      return {
        audio,
        duration,
        audioHash,
        sampleRate,
        format: codec,
        raw: audio,
      };
    } catch (e) {
      if (e instanceof SDKError) throw e;
      throw new SDKError(ErrorCode.TTS_ERROR, `腾讯云 TTS 调用失败: ${(e as Error).message}`, {
        cause: e as Error,
      });
    }
  }

  async healthCheck(): Promise<boolean> {
    return Boolean(this.secretId) && Boolean(this.secretKey);
  }
}
