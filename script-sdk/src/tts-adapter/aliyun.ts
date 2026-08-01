/**
 * 阿里云 TTS Provider
 *
 * <p>RESTful API 模式,适用于短文本(≤ 300 字符)
 * <p>长文本需用流式接口
 *
 * <p>依赖:WebSocket SDK @alicloud/aliyun-nls-ws (生产环境安装)
 * <p>配置项:
 * <ul>
 *   <li>appKey - 阿里云 AppKey
 *   <li>accessKeyId - 阿里云 AK
 *   <li>accessKeySecret - 阿里云 SK
 *   <li>voice - 语音 ID,默认 'xiaoyun'
 *   <li>format - 音频格式
 * </ul>
 *
 * @author Mavis
 */

import { TTSProvider, TTSConfig, TTSResult, SDKError, ErrorCode } from '../types';
import { sm3Hex } from '../core/hash';

/**
 * 阿里云 TTS Provider
 */
export class AliyunTTSProvider implements TTSProvider {
  readonly id: string = 'aliyun';
  readonly name: string = '阿里云智能语音';
  readonly streaming: boolean = true;

  private appKey: string;
  private accessKeyId: string;
  private accessKeySecret: string;
  private endpoint: string = 'https://nls-gateway-cn-shanghai.aliyuncs.com/stream/v1/tts';

  constructor(config: Record<string, string>) {
    this.appKey = config['appKey'] || '';
    this.accessKeyId = config['accessKeyId'] || '';
    this.accessKeySecret = config['accessKeySecret'] || '';

    if (!this.appKey) {
      throw new SDKError(ErrorCode.CONFIG_ERROR, '阿里云 TTS 缺少 appKey');
    }
    if (!this.accessKeyId || !this.accessKeySecret) {
      throw new SDKError(ErrorCode.CONFIG_ERROR, '阿里云 TTS 缺少 accessKey');
    }
  }

  async synthesize(config: TTSConfig): Promise<TTSResult> {
    if (config.streaming) {
      return this.synthesizeStream(config, () => {});
    }

    // 短文本 RESTful 合成
    const token = await this.getToken();
    const format = config.format || 'mp3';
    const voice = config.voiceId || 'xiaoyun';
    const sampleRate = config.sampleRate || 16000;

    const url = `${this.endpoint}?appkey=${this.appKey}&token=${token}&text=${encodeURIComponent(config.text)}&format=${format}&sample_rate=${sampleRate}&voice=${voice}&speech_rate=${Math.round((config.speed || 1) * 0)}&volume=${config.volume || 5}&pitch_rate=${config.pitch || 0}`;

    try {
      const response = await fetch(url);
      if (!response.ok) {
        throw new SDKError(ErrorCode.TTS_ERROR, `阿里云 TTS 失败: ${response.status}`);
      }
      const buffer = await response.arrayBuffer();
      const audio = new Uint8Array(buffer);
      const audioHash = sm3Hex(audio);

      // 估算时长(mp3 16kbps 下,1 秒 ≈ 2KB)
      const duration = Math.max(500, (audio.length / 2000) * 1000);

      return {
        audio,
        duration,
        audioHash,
        sampleRate,
        format,
        raw: audio,
      };
    } catch (e) {
      if (e instanceof SDKError) throw e;
      throw new SDKError(ErrorCode.TTS_ERROR, `阿里云 TTS 调用失败: ${(e as Error).message}`, {
        cause: e as Error,
      });
    }
  }

  async synthesizeStream(
    config: TTSConfig,
    onChunk: (chunk: Uint8Array) => void
  ): Promise<TTSResult> {
    // 简化:复用短文本接口,长文本由业务侧切分
    // 生产应使用 WebSocket 长连接
    return this.synthesize({ ...config, streaming: false }).then((r) => {
      onChunk(r.audio);
      return r;
    });
  }

  async healthCheck(): Promise<boolean> {
    try {
      const token = await this.getToken();
      return token.length > 0;
    } catch {
      return false;
    }
  }

  /**
   * 获取阿里云 NLS Token
   */
  private async getToken(): Promise<string> {
    // 简化:生产应使用阿里云 STS SDK 签名获取
    // 这里返回固定值,业务需替换
    const url = `https://nls-meta.cn-shanghai.aliyuncs.com/?Action=CreateToken&AccessKeyId=${this.accessKeyId}&AccessKeySecret=${this.accessKeySecret}&Format=JSON&RegionId=cn-shanghai&SignatureMethod=HMAC-SHA1&SignatureNonce=${Date.now()}&SignatureVersion=1.0&Timestamp=${new Date().toISOString()}&Version=2019-02-28`;

    try {
      const response = await fetch(url);
      const data = (await response.json()) as { Token?: { Id?: string } };
      return data.Token?.Id || '';
    } catch {
      // fallback - 生产必须实现
      return '';
    }
  }
}
