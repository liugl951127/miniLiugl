/**
 * 多模态渲染器
 *
 * <p>负责:
 * <ul>
 *   <li>TTS 音频播放(跨端:HTMLAudio / 微信 wx.createInnerAudioContext / Electron)
 *   <li>字幕生成与高亮
 *   <li>节点动画(进度条 / 数字滚动)
 *   <li>事件总线(给业务侧订阅)
 * </ul>
 *
 * <p>支持:浏览器 H5 / 微信小程序 / Electron PC / 移动 App WebView
 *
 * @author Mavis
 */

import {
  RendererConfig,
  RenderEvent,
  RenderEventHandler,
  SubtitleItem,
  ScriptNode,
  NodeType,
  TTSResult,
  TTSProvider,
  SDKError,
} from '../types';
import { getTTSProvider } from '../tts-adapter';

/**
 * 跨端音频播放器抽象
 *
 * <p>不同端需要不同实现
 */
export interface AudioPlayer {
  play(audio: Uint8Array, format: string): Promise<void>;
  pause(): void;
  resume(): void;
  stop(): void;
  setVolume(volume: number): void;
  setSpeed(speed: number): void;
  onEnded(handler: () => void): void;
  onError(handler: (e: Error) => void): void;
}

/**
 * 默认 HTML5 音频播放器
 */
export class HTML5AudioPlayer implements AudioPlayer {
  private audio: HTMLAudioElement | null = null;
  private endedHandler: (() => void) | null = null;
  private errorHandler: ((e: Error) => void) | null = null;

  async play(audio: Uint8Array, format: string): Promise<void> {
    return new Promise((resolve, reject) => {
      this.stop();
      // 创建 ArrayBuffer 副本以兼容 Blob 构造函数
      const buffer = new ArrayBuffer(audio.byteLength);
      new Uint8Array(buffer).set(audio);
      const blob = new Blob([buffer], { type: mimeForFormat(format) });
      const url = URL.createObjectURL(blob);
      this.audio = new Audio(url);
      this.audio.onended = () => {
        URL.revokeObjectURL(url);
        this.endedHandler?.();
        resolve();
      };
      this.audio.onerror = () => {
        URL.revokeObjectURL(url);
        const err = new Error('音频播放失败');
        this.errorHandler?.(err);
        reject(err);
      };
      this.audio.play().catch((e) => {
        URL.revokeObjectURL(url);
        reject(e);
      });
    });
  }

  pause(): void {
    this.audio?.pause();
  }

  resume(): void {
    void this.audio?.play();
  }

  stop(): void {
    if (this.audio) {
      this.audio.pause();
      this.audio.src = '';
      this.audio = null;
    }
  }

  setVolume(volume: number): void {
    if (this.audio) this.audio.volume = Math.max(0, Math.min(1, volume / 10));
  }

  setSpeed(speed: number): void {
    if (this.audio) this.audio.playbackRate = speed;
  }

  onEnded(handler: () => void): void {
    this.endedHandler = handler;
  }

  onError(handler: (e: Error) => void): void {
    this.errorHandler = handler;
  }
}

/**
 * 字幕列表管理
 */
export class SubtitleList {
  private items: SubtitleItem[] = [];
  private maxLines: number;

  constructor(maxLines: number = 3) {
    this.maxLines = maxLines;
  }

  add(item: SubtitleItem): void {
    this.items.push(item);
    if (this.items.length > this.maxLines * 5) {
      this.items.shift();
    }
  }

  current(): SubtitleItem[] {
    const now = Date.now();
    return this.items.filter(
      (i) => now >= i.startTime && now <= i.endTime + 2000
    );
  }

  clear(): void {
    this.items = [];
  }

  toJSON(): SubtitleItem[] {
    return [...this.items];
  }
}

/**
 * 渲染器
 */
export class Renderer {
  private config: RendererConfig;
  private eventHandlers: Set<RenderEventHandler> = new Set();
  private audioPlayer: AudioPlayer;
  private subtitleList: SubtitleList;
  private ttsProvider: TTSProvider;

  constructor(config: RendererConfig, audioPlayer?: AudioPlayer) {
    this.config = config;
    this.audioPlayer = audioPlayer || new HTML5AudioPlayer();
    this.subtitleList = new SubtitleList(config.subtitleMaxLines || 3);
    this.ttsProvider = getTTSProvider(config.ttsProviderId || 'mock');
  }

  // ============================================================
  // 事件订阅
  // ============================================================

  on(handler: RenderEventHandler): () => void {
    this.eventHandlers.add(handler);
    return () => this.eventHandlers.delete(handler);
  }

  private emit(event: RenderEvent): void {
    for (const h of this.eventHandlers) {
      try {
        h(event);
      } catch (e) {
        // 不让单个 handler 异常阻塞其他
        // eslint-disable-next-line no-console
        console.error('[Renderer] event handler error:', e);
      }
    }
  }

  // ============================================================
  // 节点渲染
  // ============================================================

  /**
   * 渲染一个节点
   *
   * <p>智能分发:TTS / 字幕 / 动画
   */
  async renderNode(
    node: ScriptNode,
    sessionId: string,
    speakText?: string
  ): Promise<void> {
    this.emit({
      type: 'NODE_START',
      sessionId,
      nodeCode: node.nodeCode,
      timestamp: new Date().toISOString(),
      data: { node },
    });

    try {
      // 不同节点类型,不同渲染策略
      switch (node.nodeType) {
        case NodeType.MANAGER_SPEAK:
        case NodeType.RISK_DISCLOSURE:
        case NodeType.NOTIFY:
          await this.renderManagerSpeak(node, sessionId, speakText || node.text);
          break;

        case NodeType.TEXT:
        case NodeType.CUSTOMER_READ:
          this.renderTextOnly(node, sessionId);
          break;

        case NodeType.UPLOAD_DOC:
        case NodeType.E_SIGN:
        case NodeType.VIDEO_RECORD:
        case NodeType.CUSTOMER_RESPOND:
          // 这些由业务侧处理,只发事件
          this.emit({
            type: 'SUBTITLE_UPDATE',
            sessionId,
            nodeCode: node.nodeCode,
            timestamp: new Date().toISOString(),
            data: { text: node.text, type: node.nodeType },
          });
          break;
      }

      this.emit({
        type: 'NODE_END',
        sessionId,
        nodeCode: node.nodeCode,
        timestamp: new Date().toISOString(),
      });
    } catch (e) {
      this.emit({
        type: 'ERROR',
        sessionId,
        nodeCode: node.nodeCode,
        timestamp: new Date().toISOString(),
        data: { error: (e as Error).message },
      });
      throw e;
    }
  }

  /**
   * 渲染经理说话(TTS + 字幕)
   */
  private async renderManagerSpeak(
    node: ScriptNode,
    sessionId: string,
    text: string
  ): Promise<void> {
    if (!this.config.enableTTS) {
      // 仅字幕
      this.emit({
        type: 'SUBTITLE_UPDATE',
        sessionId,
        nodeCode: node.nodeCode,
        timestamp: new Date().toISOString(),
        data: { text, speaker: 'MANAGER' },
      });
      return;
    }

    // 1. 字幕先行
    this.emit({
      type: 'SUBTITLE_UPDATE',
      sessionId,
      nodeCode: node.nodeCode,
      timestamp: new Date().toISOString(),
      data: { text, speaker: 'MANAGER' },
    });

    // 2. TTS 合成
    this.emit({
      type: 'TTS_START',
      sessionId,
      nodeCode: node.nodeCode,
      timestamp: new Date().toISOString(),
    });

    const ttsResult: TTSResult = await this.ttsProvider.synthesize({
      text,
      voiceId: 'female-yujie',
      speed: 1.0,
      format: 'mp3',
    });

    this.emit({
      type: 'TTS_END',
      sessionId,
      nodeCode: node.nodeCode,
      timestamp: new Date().toISOString(),
      data: { audioHash: ttsResult.audioHash, duration: ttsResult.duration },
    });

    // 3. 播放音频
    await this.audioPlayer.play(ttsResult.audio, ttsResult.format);
  }

  /**
   * 仅文本渲染
   */
  private renderTextOnly(node: ScriptNode, sessionId: string): void {
    if (!this.config.enableSubtitle) return;
    this.emit({
      type: 'SUBTITLE_UPDATE',
      sessionId,
      nodeCode: node.nodeCode,
      timestamp: new Date().toISOString(),
      data: { text: node.text, speaker: 'SYSTEM' },
    });
  }

  // ============================================================
  // 字幕管理
  // ============================================================

  getSubtitleList(): SubtitleList {
    return this.subtitleList;
  }

  /**
   * 手动添加字幕(供 ASR 流式回调使用)
   */
  addSubtitle(item: SubtitleItem): void {
    this.subtitleList.add(item);
    this.emit({
      type: 'SUBTITLE_UPDATE',
      sessionId: item.id,
      timestamp: new Date().toISOString(),
      data: item,
    });
  }

  // ============================================================
  // 音频控制
  // ============================================================

  pause(): void {
    this.audioPlayer.pause();
  }

  resume(): void {
    this.audioPlayer.resume();
  }

  stop(): void {
    this.audioPlayer.stop();
  }

  setVolume(volume: number): void {
    this.audioPlayer.setVolume(volume);
  }

  setSpeed(speed: number): void {
    this.audioPlayer.setSpeed(speed);
  }

  /**
   * 替换音频播放器(用于跨端)
   */
  setAudioPlayer(player: AudioPlayer): void {
    this.audioPlayer = player;
  }

  /**
   * 替换 TTS Provider
   */
  setTTSProvider(provider: TTSProvider): void {
    this.ttsProvider = provider;
  }

  // ============================================================
  // 进度事件
  // ============================================================

  emitProgress(sessionId: string, percent: number, nodeCode?: string): void {
    this.emit({
      type: 'PROGRESS',
      sessionId,
      nodeCode,
      timestamp: new Date().toISOString(),
      data: { percent },
    });
  }

  emitComplete(sessionId: string): void {
    this.emit({
      type: 'COMPLETE',
      sessionId,
      timestamp: new Date().toISOString(),
    });
  }

  emitError(sessionId: string, error: SDKError, nodeCode?: string): void {
    this.emit({
      type: 'ERROR',
      sessionId,
      nodeCode,
      timestamp: new Date().toISOString(),
      data: { error: error.message, code: error.code },
    });
  }
}

function mimeForFormat(format: string): string {
  switch (format) {
    case 'mp3':
      return 'audio/mpeg';
    case 'wav':
      return 'audio/wav';
    case 'pcm':
      return 'audio/pcm';
    default:
      return 'application/octet-stream';
  }
}
