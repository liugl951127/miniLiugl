/**
 * 阿里云 ASR Provider
 *
 * <p>一句话识别 RESTful API
 * <p>适用:短音频(≤ 60s)
 *
 * <p>配置:
 * <ul>
 *   <li>appKey
 *   <li>accessKeyId
 *   <li>accessKeySecret
 * </ul>
 *
 * @author Mavis
 */

import { ASRProvider, ASRConfig, ASRResult, SDKError, ErrorCode } from '../types';
import { sm3Hex } from '../core/hash';

export class AliyunASRProvider implements ASRProvider {
  readonly id: string = 'aliyun';
  readonly name: string = '阿里云一句话识别';
  readonly streaming: boolean = false;

  private appKey: string;
  private accessKeyId: string;
  private accessKeySecret: string;
  private endpoint: string = 'https://nls-gateway-cn-shanghai.aliyuncs.com/stream/v1/asr';

  constructor(config: Record<string, string>) {
    this.appKey = config['appKey'] || '';
    this.accessKeyId = config['accessKeyId'] || '';
    this.accessKeySecret = config['accessKeySecret'] || '';
    if (!this.appKey) {
      throw new SDKError(ErrorCode.CONFIG_ERROR, '阿里云 ASR 缺少 appKey');
    }
  }

  async recognize(config: ASRConfig): Promise<ASRResult> {
    const token = await this.getToken();
    const format = config.format;
    const sampleRate = config.sampleRate;

    // 把 Uint8Array 转 base64
    const audioBase64 = btoa(String.fromCharCode(...config.audio));

    const body = {
      appkey: this.appKey,
      token,
      format,
      sample_rate: sampleRate,
      language: config.language || 'zh-CN',
      enable_punctuation_prediction: config.enablePunctuation !== false,
      enable_inverse_text_normalization: true,
      speech: { data: audioBase64, length: config.audio.length },
    };

    try {
      const response = await fetch(this.endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      if (!response.ok) {
        throw new SDKError(ErrorCode.ASR_ERROR, `阿里云 ASR 失败: ${response.status}`);
      }
      const data = (await response.json()) as {
        Result?: { Sentences?: Array<{ Text: string; BeginTime: number; EndTime: number; ChannelId: number }> };
        status: number;
        message?: string;
      };
      if (data.status !== 20000000) {
        throw new SDKError(ErrorCode.ASR_ERROR, `阿里云 ASR 错误: ${data.message || 'unknown'}`);
      }
      const sentences = (data.Result?.Sentences || []).map((s) => ({
        text: s.Text,
        startTime: s.BeginTime,
        endTime: s.EndTime,
        confidence: 0.9,
      }));
      const text = sentences.map((s) => s.text).join('');
      const audioHash = sm3Hex(config.audio);
      const audioDuration = sentences[sentences.length - 1]?.endTime || 0;

      return {
        text,
        confidence: 0.9,
        sentences,
        isFinal: true,
        audioDuration,
        audioHash,
      };
    } catch (e) {
      if (e instanceof SDKError) throw e;
      throw new SDKError(ErrorCode.ASR_ERROR, `阿里云 ASR 调用失败: ${(e as Error).message}`, {
        cause: e as Error,
      });
    }
  }

  async healthCheck(): Promise<boolean> {
    try {
      const t = await this.getToken();
      return t.length > 0;
    } catch {
      return false;
    }
  }

  private async getToken(): Promise<string> {
    const url = `https://nls-meta.cn-shanghai.aliyuncs.com/?Action=CreateToken&AccessKeyId=${this.accessKeyId}&AccessKeySecret=${this.accessKeySecret}&Format=JSON&RegionId=cn-shanghai&SignatureMethod=HMAC-SHA1&SignatureNonce=${Date.now()}&SignatureVersion=1.0&Timestamp=${new Date().toISOString()}&Version=2019-02-28`;
    const r = await fetch(url);
    const d = (await r.json()) as { Token?: { Id?: string } };
    return d.Token?.Id || '';
  }
}
