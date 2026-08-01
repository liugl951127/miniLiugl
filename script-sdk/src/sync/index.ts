/**
 * WebSocket 同步层
 *
 * <p>负责:
 * <ul>
 *   <li>断点续传:客户端重连后从服务端拉取最新会话状态
 *   <li>多端协同:H5 / PAD / PC 同时接入,数据实时同步
 *   <li>心跳保活
 *   <li>自动重连
 * </ul>
 *
 * <p>支持浏览器 WebSocket / Node ws / 微信小程序 wx.connectSocket
 *
 * @author Mavis
 */

import {
  SyncConfig,
  SyncMessage,
  SyncMessageType,
  SDKError,
  ErrorCode,
  ScriptSession,
} from '../types';

/**
 * 跨端 WebSocket 抽象
 */
export interface WebSocketLike {
  send(data: string): void;
  close(): void;
  onOpen(handler: () => void): void;
  onMessage(handler: (data: string) => void): void;
  onClose(handler: (code: number, reason: string) => void): void;
  onError(handler: (err: Error) => void): void;
  readyState(): number;
}

/**
 * 浏览器原生 WebSocket 适配
 */
export class BrowserWebSocket implements WebSocketLike {
  private ws: WebSocket;

  constructor(url: string, protocols?: string | string[]) {
    this.ws = new WebSocket(url, protocols);
  }

  send(data: string): void {
    this.ws.send(data);
  }

  close(): void {
    this.ws.close();
  }

  onOpen(handler: () => void): void {
    this.ws.onopen = () => handler();
  }

  onMessage(handler: (data: string) => void): void {
    this.ws.onmessage = (e) => handler(typeof e.data === 'string' ? e.data : '');
  }

  onClose(handler: (code: number, reason: string) => void): void {
    this.ws.onclose = (e) => handler(e.code, e.reason);
  }

  onError(handler: (err: Error) => void): void {
    this.ws.onerror = () => handler(new Error('WebSocket error'));
  }

  readyState(): number {
    return this.ws.readyState;
  }
}

/**
 * 同步客户端
 */
export class SyncClient {
  private config: SyncConfig;
  private socket: WebSocketLike | null = null;
  private listeners: Map<SyncMessageType, Set<(msg: SyncMessage) => void>> = new Map();
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private reconnectAttempts: number = 0;
  private isOpen: boolean = false;
  private pendingQueue: SyncMessage[] = [];

  constructor(config: SyncConfig) {
    this.config = config;
  }

  // ============================================================
  // 连接管理
  // ============================================================

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        this.socket = this.createSocket();
        this.socket.onOpen(() => {
          this.isOpen = true;
          this.reconnectAttempts = 0;
          this.startHeartbeat();
          this.flushPending();
          resolve();
        });
        this.socket.onMessage((data) => this.handleMessage(data));
        this.socket.onClose((_code, _reason) => {
          this.isOpen = false;
          this.stopHeartbeat();
          if (this.config.autoReconnect !== false && this.shouldReconnect()) {
            this.scheduleReconnect();
          }
        });
        this.socket.onError((err) => {
          if (!this.isOpen) reject(err);
        });
      } catch (e) {
        reject(new SDKError(ErrorCode.NETWORK_ERROR, `WebSocket 连接失败: ${(e as Error).message}`, {
          cause: e as Error,
        }));
      }
    });
  }

  disconnect(): void {
    this.config.autoReconnect = false;
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.stopHeartbeat();
    this.socket?.close();
    this.socket = null;
  }

  /**
   * 注入自定义 WebSocket(用于 Node.js / 微信小程序)
   */
  setSocket(socket: WebSocketLike): void {
    this.socket = socket;
    this.bindSocket();
  }

  // ============================================================
  // 消息发送
  // ============================================================

  /**
   * 发送消息
   */
  send(type: SyncMessageType, payload: unknown, sessionId: string, orderId: string): void {
    const msg: SyncMessage = {
      msgId: generateMsgId(),
      type,
      sessionId,
      orderId,
      deviceId: this.config.deviceId,
      timestamp: Date.now(),
      payload,
    };
    const json = JSON.stringify(msg);

    if (this.isOpen && this.socket) {
      this.socket.send(json);
    } else {
      this.pendingQueue.push(msg);
    }
  }

  /**
   * 上报节点开始
   */
  sendNodeStart(sessionId: string, orderId: string, nodeCode: string): void {
    this.send(SyncMessageType.NODE_START, { nodeCode }, sessionId, orderId);
  }

  /**
   * 上报节点结束
   */
  sendNodeEnd(sessionId: string, orderId: string, nodeCode: string, result: unknown): void {
    this.send(SyncMessageType.NODE_END, { nodeCode, result }, sessionId, orderId);
  }

  /**
   * 上报节点结果
   */
  sendNodeResult(sessionId: string, orderId: string, nodeCode: string, result: unknown): void {
    this.send(SyncMessageType.NODE_RESULT, { nodeCode, result }, sessionId, orderId);
  }

  /**
   * 上报进度
   */
  sendProgress(sessionId: string, orderId: string, percent: number, currentNode: string): void {
    this.send(SyncMessageType.PROGRESS, { percent, currentNode }, sessionId, orderId);
  }

  /**
   * 上报完整会话
   */
  sendFullSession(sessionId: string, orderId: string, session: ScriptSession): void {
    this.send(SyncMessageType.SYNC_SESSION, session, sessionId, orderId);
  }

  /**
   * 请求断点续传
   */
  requestResume(sessionId: string, orderId: string): void {
    this.send(SyncMessageType.RESUME, { deviceId: this.config.deviceId }, sessionId, orderId);
  }

  // ============================================================
  // 消息订阅
  // ============================================================

  on(type: SyncMessageType, handler: (msg: SyncMessage) => void): () => void {
    let set = this.listeners.get(type);
    if (!set) {
      set = new Set();
      this.listeners.set(type, set);
    }
    set.add(handler);
    return () => set?.delete(handler);
  }

  // ============================================================
  // 内部
  // ============================================================

  private createSocket(): WebSocketLike {
    if (this.socket) return this.socket;
    return new BrowserWebSocket(this.config.url, this.config.protocols);
  }

  private bindSocket(): void {
    if (!this.socket) return;
    this.socket.onOpen(() => {
      this.isOpen = true;
      this.reconnectAttempts = 0;
      this.startHeartbeat();
      this.flushPending();
    });
    this.socket.onMessage((data) => this.handleMessage(data));
    this.socket.onClose((_code, _reason) => {
      this.isOpen = false;
      this.stopHeartbeat();
      if (this.config.autoReconnect !== false && this.shouldReconnect()) {
        this.scheduleReconnect();
      }
    });
    this.socket.onError((err) => {
      // eslint-disable-next-line no-console
      console.error('[SyncClient] error:', err);
    });
  }

  private handleMessage(data: string): void {
    try {
      const msg = JSON.parse(data) as SyncMessage;
      const set = this.listeners.get(msg.type);
      if (set) {
        for (const h of set) h(msg);
      }
      // 广播给通配订阅
      const allSet = this.listeners.get('*' as SyncMessageType);
      if (allSet) {
        for (const h of allSet) h(msg);
      }
    } catch (e) {
      // eslint-disable-next-line no-console
      console.error('[SyncClient] message parse error:', e);
    }
  }

  private startHeartbeat(): void {
    this.stopHeartbeat();
    const interval = this.config.heartbeatInterval || 30000;
    this.heartbeatTimer = setInterval(() => {
      this.send(SyncMessageType.HEARTBEAT, { ts: Date.now() }, '', '');
    }, interval);
  }

  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimer) return;
    const interval = this.config.reconnectInterval || 5000;
    const max = this.config.maxReconnects || 10;
    if (this.reconnectAttempts >= max) return;

    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.reconnectAttempts++;
      this.socket = null;
      void this.connect().catch(() => {
        // 失败会再次触发 onClose,递归重连
      });
    }, interval);
  }

  private shouldReconnect(): boolean {
    return this.reconnectAttempts < (this.config.maxReconnects || 10);
  }

  private flushPending(): void {
    while (this.pendingQueue.length > 0 && this.isOpen && this.socket) {
      const msg = this.pendingQueue.shift()!;
      this.socket.send(JSON.stringify(msg));
    }
  }
}

function generateMsgId(): string {
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}
