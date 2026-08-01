/**
 * Mock ASR Provider
 *
 * <p>用于测试,固定返回指定文本
 *
 * @author Mavis
 */

import { ASRProvider, ASRConfig, ASRResult } from '../types';
import { sm3Hex } from '../core/hash';

export class MockASRProvider implements ASRProvider {
  readonly id: string;
  readonly name: string;
  readonly streaming: boolean = false;

  private mockText: string;
  private mockConfidence: number;

  constructor(
    id: string = 'mock',
    name: string = 'Mock ASR',
    options?: { text?: string; confidence?: number }
  ) {
    this.id = id;
    this.name = name;
    this.mockText = options?.text || '我已了解所有风险,愿意购买该理财产品';
    this.mockConfidence = options?.confidence ?? 0.95;
  }

  async recognize(config: ASRConfig): Promise<ASRResult> {
    await new Promise((resolve) => setTimeout(resolve, 80));

    const audioHash = sm3Hex(config.audio);
    // 估算音频时长(16k,16bit 单声道)
    const bytesPerMs = 32;
    const audioDuration = Math.max(500, config.audio.length / bytesPerMs);

    return {
      text: this.mockText,
      confidence: this.mockConfidence,
      sentences: [
        {
          text: this.mockText,
          startTime: 0,
          endTime: audioDuration,
          confidence: this.mockConfidence,
        },
      ],
      isFinal: true,
      audioDuration,
      audioHash,
    };
  }

  async healthCheck(): Promise<boolean> {
    return true;
  }

  setMockText(text: string, confidence: number = 0.95): void {
    this.mockText = text;
    this.mockConfidence = confidence;
  }
}
