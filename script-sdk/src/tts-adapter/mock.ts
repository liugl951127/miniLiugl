/**
 * Mock TTS Provider
 *
 * <p>用于测试 / 本地开发 / 不连外网的环境
 * <p>生成静音 PCM 流,音频指纹固定
 *
 * @author Mavis
 */

import { TTSProvider, TTSConfig, TTSResult } from '../types';
import { sm3Hex } from '../core/hash';

/**
 * Mock TTS Provider
 */
export class MockTTSProvider implements TTSProvider {
  readonly id: string;
  readonly name: string;
  readonly streaming: boolean = false;

  constructor(id: string = 'mock', name: string = 'Mock TTS') {
    this.id = id;
    this.name = name;
  }

  async synthesize(config: TTSConfig): Promise<TTSResult> {
    // 模拟延迟
    await new Promise((resolve) => setTimeout(resolve, 50));

    // 生成静音 PCM(16k, 16bit, 单声道)
    const sampleRate = config.sampleRate || 16000;
    const durationMs = Math.max(500, config.text.length * 100);
    const numSamples = (sampleRate * durationMs) / 1000;
    const audio = new Uint8Array(numSamples * 2);

    // 写入静音
    for (let i = 0; i < numSamples; i++) {
      audio[i * 2] = 0;
      audio[i * 2 + 1] = 0;
    }

    // 模拟合成:加一点伪随机噪声
    for (let i = 0; i < audio.length; i += 800) {
      audio[i] = Math.floor(Math.random() * 32) & 0xff;
    }

    const audioHash = sm3Hex(audio);

    return {
      audio,
      duration: durationMs,
      audioHash,
      sampleRate,
      format: config.format || 'pcm',
      raw: audio,
    };
  }

  async healthCheck(): Promise<boolean> {
    return true;
  }
}
