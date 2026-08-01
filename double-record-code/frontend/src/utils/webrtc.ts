/**
 * WebRTC 音视频采集与录制封装
 * 支持断点续传、分片上传
 */
import { AVException } from '@/types';
import { upload } from '@/utils/request';

export interface RecorderConfig {
  sessionId: number;
  videoBitsPerSecond: number;     // 视频码率(bps)
  audioBitsPerSecond: number;     // 音频码率(bps)
  videoCodec: 'vp8' | 'vp9' | 'h264';
  chunkDurationMs: number;        // 每个分片时长
  onChunkReady: (chunk: Blob, index: number) => Promise<void>;
  onError: (err: Error) => void;
  onStop: (info: { duration: number; size: number }) => void;
}

export class DualRecordRecorder {
  private mediaStream: MediaStream | null = null;
  private mediaRecorder: MediaRecorder | null = null;
  private chunkIndex = 0;
  private startedAt = 0;
  private totalSize = 0;
  private paused = false;
  private stopped = false;
  private trustTime: string = '';

  constructor(private config: RecorderConfig) {}

  /**
   * 初始化:获取音视频流
   */
  async init(): Promise<{ videoTrack: MediaStreamTrack; audioTrack: MediaStreamTrack }> {
    try {
      this.trustTime = await this.fetchTrustTime();
      this.mediaStream = await navigator.mediaDevices.getUserMedia({
        video: {
          width: { ideal: 1280 },
          height: { ideal: 720 },
          frameRate: { ideal: 25 },
          facingMode: 'user',
        },
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
          sampleRate: 16000,
        },
      });
      const videoTrack = this.mediaStream.getVideoTracks()[0];
      const audioTrack = this.mediaStream.getAudioTracks()[0];
      if (!videoTrack || !audioTrack) {
        throw new AVException('无法获取摄像头或麦克风');
      }
      return { videoTrack, audioTrack };
    } catch (err) {
      if (err instanceof AVException) throw err;
      if ((err as Error).name === 'NotAllowedError') {
        throw new AVException('用户拒绝了摄像头/麦克风权限');
      }
      if ((err as Error).name === 'NotFoundError') {
        throw new AVException('未检测到摄像头或麦克风设备');
      }
      throw new AVException(`设备初始化失败: ${(err as Error).message}`);
    }
  }

  /**
   * 获取国家授时中心时间(用于可信时间戳)
   */
  private async fetchTrustTime(): Promise<string> {
    // 实际生产应该调用后端 NTP 接口
    // 这里先用本地时间 + NTP 校准
    try {
      const response = await fetch('https://api.example.com/ntp/time');
      const data = await response.json();
      return data.serverTime || new Date().toISOString();
    } catch {
      return new Date().toISOString();
    }
  }

  /**
   * 启动录制
   */
  async start(): Promise<void> {
    if (!this.mediaStream) {
      throw new AVException('请先调用 init() 初始化');
    }
    if (this.stopped) {
      throw new AVException('录制已停止,无法重新启动');
    }

    const options: MediaRecorderOptions = {
      videoBitsPerSecond: this.config.videoBitsPerSecond,
      audioBitsPerSecond: this.config.audioBitsPerSecond,
      mimeType: this.getSupportedMimeType(),
    };

    this.mediaRecorder = new MediaRecorder(this.mediaStream, options);
    this.chunkIndex = 0;
    this.totalSize = 0;
    this.startedAt = Date.now();
    this.paused = false;
    this.stopped = false;

    this.mediaRecorder.ondataavailable = async (event) => {
      if (event.data && event.data.size > 0 && !this.paused) {
        this.totalSize += event.data.size;
        const currentIndex = this.chunkIndex++;
        try {
          await this.config.onChunkReady(event.data, currentIndex);
        } catch (err) {
          this.config.onError(new AVException(`分片上传失败: ${(err as Error).message}`));
        }
      }
    };

    this.mediaRecorder.onerror = (event) => {
      this.config.onError(new AVException(`录制错误: ${(event as ErrorEvent).message || '未知'}`));
    };

    this.mediaRecorder.onstop = () => {
      this.stopped = true;
      const duration = Math.round((Date.now() - this.startedAt) / 1000);
      this.config.onStop({ duration, size: this.totalSize });
    };

    // 启动,按配置分片
    this.mediaRecorder.start(this.config.chunkDurationMs);
  }

  /**
   * 暂停录制
   */
  pause(): void {
    if (this.mediaRecorder && this.mediaRecorder.state === 'recording') {
      this.mediaRecorder.pause();
      this.paused = true;
    }
  }

  /**
   * 恢复录制
   */
  resume(): void {
    if (this.mediaRecorder && this.mediaRecorder.state === 'paused') {
      this.mediaRecorder.resume();
      this.paused = false;
    }
  }

  /**
   * 停止录制
   */
  stop(): Promise<void> {
    return new Promise((resolve) => {
      if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
        this.mediaRecorder.onstop = () => {
          this.stopped = true;
          const duration = Math.round((Date.now() - this.startedAt) / 1000);
          this.config.onStop({ duration, size: this.totalSize });
          resolve();
        };
        this.mediaRecorder.stop();
      } else {
        resolve();
      }
    });
  }

  /**
   * 释放资源
   */
  release(): void {
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach((t) => t.stop());
      this.mediaStream = null;
    }
    if (this.mediaRecorder) {
      try {
        this.mediaRecorder.stop();
      } catch {}
      this.mediaRecorder = null;
    }
  }

  /**
   * 抓拍当前画面(用于电子签字核身)
   */
  async snapshot(): Promise<Blob> {
    if (!this.mediaStream) throw new AVException('无视频流');
    const track = this.mediaStream.getVideoTracks()[0];
    const imageCapture = new (window as any).ImageCapture(track);
    const blob = await imageCapture.takePhoto();
    return blob;
  }

  /**
   * 浏览器支持的 MIME 类型检测
   */
  private getSupportedMimeType(): string {
    const candidates = [
      `video/${this.config.videoCodec};codecs=opus`,
      'video/webm;codecs=vp9,opus',
      'video/webm;codecs=vp8,opus',
      'video/mp4;codecs=h264,aac',
      'video/webm',
    ];
    for (const mime of candidates) {
      if (MediaRecorder.isTypeSupported(mime)) {
        return mime;
      }
    }
    return 'video/webm';
  }

  get currentDuration(): number {
    if (this.startedAt === 0) return 0;
    return Math.round((Date.now() - this.startedAt) / 1000);
  }

  get currentTrustTime(): string {
    return this.trustTime;
  }
}

/**
 * 上传视频分片到 OSS(经过后端中转)
 */
export async function uploadVideoChunk(
  sessionId: number,
  chunkIndex: number,
  chunk: Blob,
): Promise<{ etag: string; url: string }> {
  const formData = new FormData();
  formData.append('sessionId', String(sessionId));
  formData.append('chunkIndex', String(chunkIndex));
  formData.append('file', chunk, `chunk-${chunkIndex}.webm`);
  return upload<{ etag: string; url: string }>('/v1/session/upload', formData);
}

/**
 * 合并分片为完整视频
 */
export async function mergeVideoChunks(sessionId: number): Promise<{ videoUrl: string; videoHash: string }> {
  const { post } = await import('@/utils/request');
  return post<{ videoUrl: string; videoHash: string }>('/v1/session/merge', { sessionId });
}
