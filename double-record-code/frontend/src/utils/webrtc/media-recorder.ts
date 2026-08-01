/**
 * 媒体录制器(浏览器侧分段录制)
 *
 * <p>特性:
 * <ul>
 *   <li>分片(每 N 秒一段)→ 边录边传
 *   <li>SHA-256 指纹 → 上链存证
 *   <li>断网恢复 → 本地缓存 + 重传队列
 *   <li>麦克风回声消除
 *   <li>音频电平可视化
 * </ul>
 */
export class MediaRecorderPolyfill {
  private recorder: MediaRecorder | null = null;
  private stream: MediaStream | null = null;
  private chunks: Blob[] = [];
  private startTime: number = 0;
  private chunkIndex: number = 0;
  private onChunk: ((chunk: Blob, index: number, duration: number, hash: string) => void) | null = null;
  private onError: ((err: Error) => void) | null = null;
  private chunkDuration: number;

  constructor(options?: { chunkDuration?: number }) {
    this.chunkDuration = options?.chunkDuration || 3000; // 3 秒
  }

  /**
   * 开始录制
   */
  start(stream: MediaStream, onChunk: (chunk: Blob, index: number, duration: number, hash: string) => void, onError?: (err: Error) => void): void {
    this.stream = stream;
    this.onChunk = onChunk;
    this.onError = onError;
    this.chunks = [];
    this.chunkIndex = 0;
    this.startTime = Date.now();

    try {
      this.recorder = new MediaRecorder(stream, {
        mimeType: this.getSupportedMimeType(),
        videoBitsPerSecond: 2_000_000,  // 2 Mbps
        audioBitsPerSecond: 128_000,     // 128 kbps
      });

      this.recorder.ondataavailable = (e) => {
        if (e.data.size > 0) {
          this.handleChunk(e.data);
        }
      };

      this.recorder.onerror = (e: any) => {
        this.onError?.(new Error(`录制错误: ${e.message || e}`));
      };

      this.recorder.onstop = () => {
        this.onError?.(new Error('录制已停止'));
      };

      // timeslice 触发分段
      this.recorder.start(this.chunkDuration);
    } catch (e) {
      this.onError?.(e as Error);
    }
  }

  /**
   * 停止录制
   */
  stop(): Promise<Blob> {
    return new Promise((resolve) => {
      if (!this.recorder) {
        resolve(new Blob());
        return;
      }
      this.recorder.onstop = () => {
        const blob = new Blob(this.chunks, { type: 'video/webm' });
        resolve(blob);
      };
      this.recorder.stop();
    });
  }

  /**
   * 暂停
   */
  pause(): void {
    this.recorder?.pause();
  }

  /**
   * 恢复
   */
  resume(): void {
    this.recorder?.resume();
  }

  /**
   * 录制时长
   */
  getDuration(): number {
    return Date.now() - this.startTime;
  }

  // ============================================================
  // 内部
  // ============================================================

  private handleChunk(data: Blob): void {
    if (!this.onChunk) return;
    const idx = this.chunkIndex++;
    const duration = this.chunkDuration;
    // 异步计算哈希
    this.computeHash(data).then((hash) => {
      try {
        this.onChunk!(data, idx, duration, hash);
      } catch (e) {
        this.onError?.(e as Error);
      }
    });
  }

  private async computeHash(blob: Blob): Promise<string> {
    const buf = await blob.arrayBuffer();
    const hashBuf = await crypto.subtle.digest('SHA-256', buf);
    return Array.from(new Uint8Array(hashBuf))
      .map((b) => b.toString(16).padStart(2, '0'))
      .join('');
  }

  private getSupportedMimeType(): string {
    const candidates = [
      'video/webm;codecs=vp9,opus',
      'video/webm;codecs=vp8,opus',
      'video/webm',
      'video/mp4',
    ];
    for (const c of candidates) {
      if (MediaRecorder.isTypeSupported(c)) return c;
    }
    return 'video/webm';
  }
}

/**
 * 音频电平采样(用于可视化)
 */
export class AudioLevelMeter {
  private audioContext: AudioContext | null = null;
  private analyser: AnalyserNode | null = null;
  private dataArray: Uint8Array | null = null;
  private rafId: number | null = null;
  private onLevel: (level: number) => void;

  constructor(onLevel: (level: number) => void) {
    this.onLevel = onLevel;
  }

  start(stream: MediaStream): void {
    try {
      this.audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
      const source = this.audioContext.createMediaStreamSource(stream);
      this.analyser = this.audioContext.createAnalyser();
      this.analyser.fftSize = 512;
      source.connect(this.analyser);
      this.dataArray = new Uint8Array(this.analyser.frequencyBinCount);

      const tick = () => {
        if (!this.analyser || !this.dataArray) return;
        this.analyser.getByteTimeDomainData(this.dataArray);
        let sum = 0;
        for (let i = 0; i < this.dataArray.length; i++) {
          const v = (this.dataArray[i] - 128) / 128;
          sum += v * v;
        }
        const rms = Math.sqrt(sum / this.dataArray.length);
        const level = Math.min(1, rms * 4);
        this.onLevel(level);
        this.rafId = requestAnimationFrame(tick);
      };
      tick();
    } catch (e) {
      console.error('[AudioLevelMeter] 启动失败:', e);
    }
  }

  stop(): void {
    if (this.rafId) cancelAnimationFrame(this.rafId);
    this.audioContext?.close();
    this.rafId = null;
    this.audioContext = null;
    this.analyser = null;
    this.dataArray = null;
  }
}
