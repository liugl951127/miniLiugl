/**
 * ASR 语音识别封装
 * 用于实时转写客户回答,验证关键词命中
 */
import { AVException } from '@/types';

export interface ASRConfig {
  sessionId: number;
  language: 'zh-CN' | 'en-US';
  sampleRate: number;
  onResult: (text: string, isFinal: boolean) => void;
  onError: (err: Error) => void;
}

export class ASRClient {
  private recognition: any = null;
  private isRunning = false;

  constructor(private config: ASRConfig) {}

  /**
   * 启动浏览器原生 ASR(基于 Web Speech API)
   * 实际生产建议使用阿里云 ASR SDK
   */
  start(): void {
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) {
      this.config.onError(new AVException('当前浏览器不支持语音识别'));
      return;
    }

    this.recognition = new SpeechRecognition();
    this.recognition.continuous = true;
    this.recognition.interimResults = true;
    this.recognition.lang = this.config.language;
    this.recognition.maxAlternatives = 1;

    this.recognition.onresult = (event: any) => {
      let finalText = '';
      let interimText = '';
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const result = event.results[i];
        if (result.isFinal) {
          finalText += result[0].transcript;
        } else {
          interimText += result[0].transcript;
        }
      }
      if (finalText) {
        this.config.onResult(finalText, true);
      } else if (interimText) {
        this.config.onResult(interimText, false);
      }
    };

    this.recognition.onerror = (event: any) => {
      const errorMessages: Record<string, string> = {
        'no-speech': '未检测到语音',
        'audio-capture': '麦克风不可用',
        'not-allowed': '麦克风权限被拒绝',
        network: '网络错误',
      };
      const msg = errorMessages[event.error] || `ASR 错误: ${event.error}`;
      this.config.onError(new AVException(msg));
    };

    this.recognition.onend = () => {
      // 浏览器会自动停止,需要重连
      if (this.isRunning) {
        try {
          this.recognition.start();
        } catch {
          // ignore
        }
      }
    };

    try {
      this.recognition.start();
      this.isRunning = true;
    } catch (err) {
      this.config.onError(new AVException(`启动 ASR 失败: ${(err as Error).message}`));
    }
  }

  stop(): void {
    this.isRunning = false;
    if (this.recognition) {
      try {
        this.recognition.stop();
      } catch {}
      this.recognition = null;
    }
  }
}

/**
 * 验证文本是否包含所有必需关键词
 */
export function checkKeywords(text: string, keywords: string[]): { matched: string[]; missed: string[] } {
  const normalized = text.toLowerCase().replace(/\s+/g, '');
  const matched: string[] = [];
  const missed: string[] = [];
  for (const kw of keywords) {
    if (normalized.includes(kw.toLowerCase().replace(/\s+/g, ''))) {
      matched.push(kw);
    } else {
      missed.push(kw);
    }
  }
  return { matched, missed };
}

/**
 * 使用阿里云一句话识别 REST API(生产推荐)
 * 需要 token,可走后端代理
 */
export async function callAliYunASR(audioBase64: string, token: string): Promise<string> {
  const { post } = await import('@/utils/request');
  return post<string>('/v1/asr/recognize', { audio: audioBase64, token });
}
