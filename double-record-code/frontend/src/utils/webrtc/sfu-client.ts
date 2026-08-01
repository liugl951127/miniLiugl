/**
 * WebRTC SFU 客户端(基于 mediasoup-client 或 LiveKit)
 *
 * <p>适用: PAD 端多人音视频(客户 + 经理 + 见证人可选)
 * <p>SFU 服务器: mediasoup / LiveKit / ZLMediaKit 任选
 *
 * <p>设计:
 * <ul>
 *   <li>本地发布:本端摄像头 + 麦克风
 *   <li>远端订阅:对端音视频流
 *   <li>录制:服务端录制(LoL/合流)
 *   <li>降级:网络差时自动降低码率
 * </ul>
 *
 * @author Mavis
 */

import { MediaRecorderPolyfill } from './media-recorder';

export interface SfuConfig {
  /** SFU WebSocket 地址 */
  url: string;
  /** 房间 ID(通常 = sessionId) */
  roomId: string;
  /** 用户 ID */
  userId: string;
  /** 用户角色 */
  role: 'CUSTOMER' | 'MANAGER' | 'WITNESS';
  /** 是否启用视频 */
  video: boolean;
  /** 是否启用音频 */
  audio: boolean;
  /** 视频分辨率 */
  videoResolution?: '480p' | '720p' | '1080p';
  /** 视频码率(kbps) */
  videoBitrate?: number;
  /** 音频码率 */
  audioBitrate?: number;
}

export interface RemoteStream {
  userId: string;
  role: string;
  stream: MediaStream;
}

/**
 * SFU 客户端(简化版,基于 WebRTC + WebSocket 信令)
 *
 * <p>真实生产可替换为 mediasoup-client / livekit-client
 */
export class SfuClient {
  private ws: WebSocket | null = null;
  private localStream: MediaStream | null = null;
  private peerConnections: Map<string, RTCPeerConnection> = new Map();
  private remoteStreams: Map<string, RemoteStream> = new Map();
  private config: SfuConfig;
  private listeners: Map<string, Set<(payload: any) => void>> = new Map();
  private iceServers: RTCIceServer[] = [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' },
  ];

  constructor(config: SfuConfig) {
    this.config = config;
  }

  // ============================================================
  // 事件订阅
  // ============================================================

  on(event: 'remote-stream' | 'peer-join' | 'peer-leave' | 'error' | 'state', handler: (payload: any) => void): () => void {
    let set = this.listeners.get(event);
    if (!set) {
      set = new Set();
      this.listeners.set(event, set);
    }
    set.add(handler);
    return () => set?.delete(handler);
  }

  private emit(event: string, payload: any) {
    const set = this.listeners.get(event);
    if (set) {
      for (const h of set) {
        try { h(payload); } catch (e) { /* ignore */ }
      }
    }
  }

  // ============================================================
  // 连接管理
  // ============================================================

  /**
   * 加入房间
   */
  async join(): Promise<MediaStream> {
    // 1. 获取本地媒体
    this.localStream = await this.getLocalStream();
    this.emit('state', { state: 'local-ready', stream: this.localStream });

    // 2. 连接信令服务器
    await this.connectSignaling();

    this.emit('state', { state: 'joined' });
    return this.localStream;
  }

  /**
   * 离开房间
   */
  leave(): void {
    // 关闭所有 PeerConnection
    for (const pc of this.peerConnections.values()) {
      pc.close();
    }
    this.peerConnections.clear();
    this.remoteStreams.clear();

    // 停止本地流
    if (this.localStream) {
      this.localStream.getTracks().forEach((t) => t.stop());
      this.localStream = null;
    }

    // 关闭信令
    if (this.ws) {
      this.send({ type: 'leave', roomId: this.config.roomId, userId: this.config.userId });
      this.ws.close();
      this.ws = null;
    }
    this.emit('state', { state: 'left' });
  }

  // ============================================================
  // 内部
  // ============================================================

  private async getLocalStream(): Promise<MediaStream> {
    const constraints: MediaStreamConstraints = {
      video: this.config.video
        ? {
            width: this.getVideoWidth(),
            height: this.getVideoHeight(),
            frameRate: { ideal: 30, max: 30 },
            facingMode: 'user',
          }
        : false,
      audio: this.config.audio
        ? {
            echoCancellation: true,
            noiseSuppression: true,
            autoGainControl: true,
            sampleRate: 48000,
          }
        : false,
    };

    return navigator.mediaDevices.getUserMedia(constraints);
  }

  private getVideoWidth(): number {
    switch (this.config.videoResolution) {
      case '480p': return 640;
      case '720p': return 1280;
      case '1080p': return 1920;
      default: return 1280;
    }
  }

  private getVideoHeight(): number {
    switch (this.config.videoResolution) {
      case '480p': return 480;
      case '720p': return 720;
      case '1080p': return 1080;
      default: return 720;
    }
  }

  private async connectSignaling(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.ws = new WebSocket(this.config.url);
      this.ws.onopen = () => {
        // 发送 join 消息
        this.send({
          type: 'join',
          roomId: this.config.roomId,
          userId: this.config.userId,
          role: this.config.role,
        });
        resolve();
      };
      this.ws.onerror = (e) => {
        this.emit('error', e);
        reject(e);
      };
      this.ws.onmessage = (e) => this.onSignalingMessage(e.data);
      this.ws.onclose = () => this.emit('state', { state: 'disconnected' });
    });
  }

  private onSignalingMessage(data: string): void {
    try {
      const msg = JSON.parse(data);
      switch (msg.type) {
        case 'peer-joined':
          this.handlePeerJoined(msg);
          break;
        case 'peer-left':
          this.handlePeerLeft(msg);
          break;
        case 'offer':
          this.handleOffer(msg);
          break;
        case 'answer':
          this.handleAnswer(msg);
          break;
        case 'ice-candidate':
          this.handleIceCandidate(msg);
          break;
      }
    } catch (e) {
      console.error('[SfuClient] 解析信令失败:', e);
    }
  }

  private send(payload: any): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(payload));
    }
  }

  private async handlePeerJoined(msg: any): Promise<void> {
    const pc = this.createPeerConnection(msg.userId);
    this.peerConnections.set(msg.userId, pc);

    // 添加本地流
    if (this.localStream) {
      for (const track of this.localStream.getTracks()) {
        pc.addTrack(track, this.localStream);
      }
    }

    // 创建 offer
    const offer = await pc.createOffer();
    await pc.setLocalDescription(offer);
    this.send({
      type: 'offer',
      to: msg.userId,
      from: this.config.userId,
      sdp: offer.sdp,
    });

    this.emit('peer-join', { userId: msg.userId, role: msg.role });
  }

  private handlePeerLeft(msg: any): void {
    const pc = this.peerConnections.get(msg.userId);
    if (pc) {
      pc.close();
      this.peerConnections.delete(msg.userId);
    }
    this.remoteStreams.delete(msg.userId);
    this.emit('peer-leave', { userId: msg.userId });
  }

  private async handleOffer(msg: any): Promise<void> {
    const pc = this.createPeerConnection(msg.from);
    this.peerConnections.set(msg.from, pc);

    if (this.localStream) {
      for (const track of this.localStream.getTracks()) {
        pc.addTrack(track, this.localStream);
      }
    }

    await pc.setRemoteDescription({ type: 'offer', sdp: msg.sdp });
    const answer = await pc.createAnswer();
    await pc.setLocalDescription(answer);
    this.send({
      type: 'answer',
      to: msg.from,
      from: this.config.userId,
      sdp: answer.sdp,
    });
  }

  private async handleAnswer(msg: any): Promise<void> {
    const pc = this.peerConnections.get(msg.from);
    if (pc) {
      await pc.setRemoteDescription({ type: 'answer', sdp: msg.sdp });
    }
  }

  private async handleIceCandidate(msg: any): Promise<void> {
    const pc = this.peerConnections.get(msg.from);
    if (pc && msg.candidate) {
      try {
        await pc.addIceCandidate(msg.candidate);
      } catch (e) {
        console.error('[SfuClient] ICE 候选失败:', e);
      }
    }
  }

  private createPeerConnection(remoteUserId: string): RTCPeerConnection {
    const pc = new RTCPeerConnection({ iceServers: this.iceServers });

    pc.onicecandidate = (e) => {
      if (e.candidate) {
        this.send({
          type: 'ice-candidate',
          to: remoteUserId,
          from: this.config.userId,
          candidate: e.candidate.toJSON(),
        });
      }
    };

    pc.ontrack = (e) => {
      const [stream] = e.streams;
      this.remoteStreams.set(remoteUserId, {
        userId: remoteUserId,
        role: 'REMOTE',
        stream,
      });
      this.emit('remote-stream', { userId: remoteUserId, stream });
    };

    pc.onconnectionstatechange = () => {
      if (pc.connectionState === 'failed' || pc.connectionState === 'disconnected') {
        console.warn(`[SfuClient] Peer ${remoteUserId} 连接异常: ${pc.connectionState}`);
      }
    };

    return pc;
  }

  // ============================================================
  // 设备控制
  // ============================================================

  /**
   * 切换摄像头
   */
  async switchCamera(facingMode: 'user' | 'environment'): Promise<MediaStream> {
    if (!this.localStream) throw new Error('本地流未就绪');
    const newStream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode },
      audio: false,
    });
    const newVideoTrack = newStream.getVideoTracks()[0];
    const oldVideoTrack = this.localStream.getVideoTracks()[0];
    if (oldVideoTrack) {
      this.localStream.removeTrack(oldVideoTrack);
      oldVideoTrack.stop();
    }
    this.localStream.addTrack(newVideoTrack);
    // 替换所有 PeerConnection 的 track
    for (const pc of this.peerConnections.values()) {
      const sender = pc.getSenders().find((s) => s.track?.kind === 'video');
      if (sender) await sender.replaceTrack(newVideoTrack);
    }
    return this.localStream;
  }

  /**
   * 静音
   */
  mute(muted: boolean): void {
    if (this.localStream) {
      this.localStream.getAudioTracks().forEach((t) => (t.enabled = !muted));
    }
  }

  /**
   * 关闭视频
   */
  disableVideo(disabled: boolean): void {
    if (this.localStream) {
      this.localStream.getVideoTracks().forEach((t) => (t.enabled = !disabled));
    }
  }
}
