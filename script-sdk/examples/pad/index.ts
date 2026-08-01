/**
 * PAD 端集成示例
 *
 * <p>适用:银行 PAD 设备(Android/iOS)、微信小程序
 * <p>需要替换音频播放器为原生实现
 *
 * @author Mavis
 */

import {
  ScriptSDK,
  Channel,
  ProductType,
  ScriptModel,
  AudioPlayer,
  WebSocketLike,
  SDKError,
} from '../../src';

// ============================================================
// 1. 微信小程序音频播放器适配
// ============================================================

class MiniProgramAudioPlayer implements AudioPlayer {
  private innerAudioContext: any; // wx.InnerAudioContext
  private endedHandler: (() => void) | null = null;
  private errorHandler: ((e: Error) => void) | null = null;
  private volume: number = 1.0;
  private speed: number = 1.0;

  constructor() {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    this.innerAudioContext = (globalThis as any).wx?.createInnerAudioContext?.();
    if (this.innerAudioContext) {
      this.innerAudioContext.onEnded(() => this.endedHandler?.());
      this.innerAudioContext.onError((e: unknown) => {
        this.errorHandler?.(new Error(String(e)));
      });
    }
  }

  async play(audio: Uint8Array, format: string): Promise<void> {
    if (!this.innerAudioContext) {
      throw new SDKError('RENDER_ERROR' as any, '微信 InnerAudioContext 不可用');
    }
    // 微信小程序只能播放 URL,需要先上传到 OSS
    // 实际生产:先 uploadBuffer 到 OSS,获得 URL 再 play
    // 这里示意
    return new Promise((resolve, reject) => {
      this.innerAudioContext.src = `data:audio/${format};base64,${arrayBufferToBase64(audio)}`;
      this.innerAudioContext.play();
      this.innerAudioContext.onEnded(() => resolve());
      this.innerAudioContext.onError((e: unknown) => reject(new Error(String(e))));
    });
  }

  pause(): void {
    this.innerAudioContext?.pause();
  }

  resume(): void {
    this.innerAudioContext?.play();
  }

  stop(): void {
    this.innerAudioContext?.stop();
  }

  setVolume(volume: number): void {
    this.volume = volume;
    if (this.innerAudioContext) this.innerAudioContext.volume = volume / 10;
  }

  setSpeed(speed: number): void {
    this.speed = speed;
    if (this.innerAudioContext) this.innerAudioContext.playbackRate = speed;
  }

  onEnded(handler: () => void): void {
    this.endedHandler = handler;
  }

  onError(handler: (e: Error) => void): void {
    this.errorHandler = handler;
  }
}

function arrayBufferToBase64(buffer: Uint8Array): string {
  let binary = '';
  for (let i = 0; i < buffer.byteLength; i++) {
    binary += String.fromCharCode(buffer[i]);
  }
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return (globalThis as any).wx?.arrayBufferToBase64?.(buffer) || btoa(binary);
}

// ============================================================
// 2. PAD 端主流程
// ============================================================

async function bootstrapPad() {
  const sdk: ScriptSDK = await ScriptSDK.create({
    defaultChannel: Channel.PAD,
    tts: { providerId: 'mock', config: {} },
    asr: { providerId: 'mock', config: {} },
    renderer: {
      enableTTS: true,
      enableSubtitle: true,
    },
  });

  // 2.1 注入原生音频播放器
  sdk.setAudioPlayer(new MiniProgramAudioPlayer());

  // 2.2 加载脚本(同 H5)
  const script: ScriptModel = {
    scriptId: 'WEALTH-3.2',
    version: '3.2.0',
    productType: ProductType.WEALTH,
    name: '理财双录话术',
    createdAt: '2026-08-01T00:00:00Z',
    updatedAt: '2026-08-01T00:00:00Z',
    nodes: [
      { nodeCode: 'N001', nodeName: '开场', nodeType: 'MANAGER_SPEAK', order: 1, text: '您好,欢迎来到 XX 银行。' },
      { nodeCode: 'N002', nodeName: '风险提示', nodeType: 'RISK_DISCLOSURE', order: 2, text: '理财有风险,投资需谨慎。' },
    ],
  };
  sdk.loadScript(script);

  // 2.3 启动会话 + 启动 WebSocket(注入原生)
  await sdk.startSync({
    url: 'wss://sync.bank.com/ws',
    deviceId: 'pad-001',
    userId: 'M002',
  });

  sdk.startSession({
    sessionId: 'SES-PAD-001',
    orderId: 'ORD20260801000002',
    customerId: 'C002',
    productId: 'PROD-WEALTH-001',
    productType: ProductType.WEALTH,
    channel: Channel.PAD,
    salesUserId: 'M002',
    branchId: '2001',
  });

  // 2.4 PAD 客户通过触屏交互
  // ... (业务方实现录音/签字 UI)

  return sdk;
}

export { bootstrapPad, MiniProgramAudioPlayer };
