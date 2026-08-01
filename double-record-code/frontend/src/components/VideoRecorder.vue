<!--
  视频录制组件
  基于 WebRTC + MediaRecorder,支持分片上传、断点续传
-->
<template>
  <div class="video-recorder">
    <div class="recorder-layout">
      <!-- 左侧:视频预览区 -->
      <div class="preview-area">
        <video
          ref="videoEl"
          class="video-preview"
          autoplay
          playsinline
          muted
        ></video>
        <canvas ref="canvasEl" class="hidden-canvas" style="display:none;"></canvas>

        <!-- 录制状态指示 -->
        <div class="recording-overlay" v-if="isRecording">
          <div class="rec-dot" :class="{ paused: isPaused }"></div>
          <span class="rec-time">{{ formatTime(duration) }}</span>
          <span class="trust-time">可信时间: {{ trustTime }}</span>
        </div>

        <!-- 水印 -->
        <div class="watermark">
          {{ customerName }} · {{ orderNo }} · {{ formatTime(Date.now() / 1000, 'YYYY-MM-DD HH:mm:ss') }}
        </div>
      </div>

      <!-- 右侧:控制面板 -->
      <div class="control-panel">
        <a-card title="录制控制" :bordered="false" size="small">
          <a-space direction="vertical" style="width: 100%;">
            <div class="status-row">
              <span class="label">设备状态</span>
              <a-tag :color="deviceReady ? 'success' : 'default'">
                {{ deviceReady ? '已就绪' : '未初始化' }}
              </a-tag>
            </div>

            <div class="status-row">
              <span class="label">录制时长</span>
              <span class="value">{{ formatTime(duration) }}</span>
            </div>

            <div class="status-row">
              <span class="label">已上传分片</span>
              <span class="value">{{ uploadedChunks }} / {{ expectedChunks }}</span>
            </div>

            <div class="status-row">
              <span class="label">上传进度</span>
              <a-progress :percent="uploadProgress" :status="uploadStatus" />
            </div>

            <a-divider style="margin: 12px 0;" />

            <a-space style="width: 100%;">
              <a-button
                v-if="!isRecording && !isPaused"
                type="primary"
                block
                :loading="initializing"
                :disabled="!canStart"
                @click="onStart"
              >
                <VideoCameraOutlined /> 开始录制
              </a-button>

              <a-button
                v-if="isRecording && !isPaused"
                block
                @click="onPause"
              >
                <PauseOutlined /> 暂停
              </a-button>

              <a-button
                v-if="isPaused"
                type="primary"
                block
                @click="onResume"
              >
                <PlayCircleOutlined /> 继续
              </a-button>

              <a-button
                v-if="isRecording || isPaused"
                danger
                block
                @click="onStop"
              >
                <StopOutlined /> 结束录制
              </a-button>
            </a-space>

            <a-button
              v-if="!deviceReady"
              block
              @click="initDevice"
            >
              <CameraOutlined /> 授权摄像头/麦克风
            </a-button>

            <a-alert
              v-if="errorMsg"
              :message="errorMsg"
              type="error"
              show-icon
              closable
              @close="errorMsg = ''"
            />

            <a-alert
              v-if="warningMsg"
              :message="warningMsg"
              type="warning"
              show-icon
              closable
              @close="warningMsg = ''"
            />
          </a-space>
        </a-card>

        <a-card title="录制要求" :bordered="false" size="small" style="margin-top: 12px;">
          <ul class="requirements">
            <li><CheckCircleOutlined /> 双流录制(客户 + 客户经理)</li>
            <li><CheckCircleOutlined /> 720P / 25fps / H.264</li>
            <li><CheckCircleOutlined /> 实时嵌入可信时间戳</li>
            <li><CheckCircleOutlined /> 网络中断自动恢复</li>
            <li><CheckCircleOutlined /> 10 年长期存档</li>
          </ul>
        </a-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { message, Modal } from 'ant-design-vue';
import {
  VideoCameraOutlined,
  PauseOutlined,
  PlayCircleOutlined,
  StopOutlined,
  CameraOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons-vue';
import { DualRecordRecorder, uploadVideoChunk, mergeVideoChunks } from '@/utils/webrtc';
import { sessionApi } from '@/api/session';

const props = defineProps<{
  sessionId: number;
  orderNo: string;
  customerName: string;
  productType: number;
  /** 节点模式: 话术节点内的视频录制 */
  nodeCode?: string;
}>();

const emit = defineEmits<{
  (e: 'recordingStarted'): void;
  (e: 'recordingPaused'): void;
  (e: 'recordingResumed'): void;
  (e: 'recordingStopped', data: { videoUrl: string; videoHash: string; duration: number; size: number }): void;
  (e: 'error', err: Error): void;
}>();

// DOM 引用
const videoEl = ref<HTMLVideoElement | null>(null);
const canvasEl = ref<HTMLCanvasElement | null>(null);

// 状态
const initializing = ref(false);
const isRecording = ref(false);
const isPaused = ref(false);
const deviceReady = ref(false);
const duration = ref(0);
const uploadedChunks = ref(0);
const expectedChunks = ref(0);
const uploadProgress = ref(0);
const uploadStatus = ref<'normal' | 'success' | 'exception' | 'active'>('normal');
const errorMsg = ref('');
const warningMsg = ref('');
const trustTime = ref('');

let recorder: DualRecordRecorder | null = null;
let durationTimer: any = null;

const canStart = computed(() => deviceReady.value && !isRecording.value);

function formatTime(seconds: number, format = 'HH:mm:ss'): string {
  if (typeof seconds !== 'number' || isNaN(seconds)) return '--:--:--';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = Math.floor(seconds % 60);
  const pad = (n: number) => String(n).padStart(2, '0');
  if (format === 'HH:mm:ss') {
    return `${pad(h)}:${pad(m)}:${pad(s)}`;
  }
  // YYYY-MM-DD HH:mm:ss
  const d = new Date(seconds * 1000);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

async function initDevice() {
  initializing.value = true;
  errorMsg.value = '';
  try {
    recorder = new DualRecordRecorder({
      sessionId: props.sessionId,
      videoBitsPerSecond: 2_000_000,    // 2 Mbps
      audioBitsPerSecond: 128_000,      // 128 kbps
      videoCodec: 'h264',
      chunkDurationMs: 3000,            // 3 秒一个分片
      onChunkReady: handleChunkReady,
      onError: handleError,
      onStop: handleStop,
    });

    const { videoTrack, audioTrack } = await recorder.init();
    if (videoEl.value) {
      videoEl.value.srcObject = new MediaStream([videoTrack, audioTrack]);
      await videoEl.value.play();
    }
    trustTime.value = recorder.currentTrustTime;
    deviceReady.value = true;
    message.success('设备初始化成功');
  } catch (err: any) {
    errorMsg.value = err.message;
    deviceReady.value = false;
  } finally {
    initializing.value = false;
  }
}

async function onStart() {
  if (!recorder) {
    await initDevice();
    if (!recorder) return;
  }
  try {
    await recorder.start();
    isRecording.value = true;
    isPaused.value = false;
    startDurationTimer();
    emit('recordingStarted');
  } catch (err: any) {
    errorMsg.value = err.message;
  }
}

function onPause() {
  if (!recorder) return;
  recorder.pause();
  isPaused.value = true;
  stopDurationTimer();
  emit('recordingPaused');
}

async function onResume() {
  if (!recorder) return;
  recorder.resume();
  isPaused.value = false;
  startDurationTimer();
  emit('recordingResumed');
}

async function onStop() {
  if (!recorder) return;
  Modal.confirm({
    title: '确认结束录制?',
    content: '结束后视频将自动合并并上传,请确保所有节点已录制完成。',
    okText: '确认结束',
    cancelText: '继续录制',
    onOk: async () => {
      await recorder!.stop();
      isRecording.value = false;
      isPaused.value = false;
      stopDurationTimer();
    },
  });
}

async function handleChunkReady(chunk: Blob, index: number): Promise<void> {
  expectedChunks.value = index + 1;
  uploadStatus.value = 'active';
  try {
    // 尝试上传,失败重试 3 次
    let lastError: any;
    for (let i = 0; i < 3; i++) {
      try {
        await uploadVideoChunk(props.sessionId, index, chunk);
        uploadedChunks.value = index + 1;
        uploadProgress.value = Math.round((uploadedChunks.value / expectedChunks.value) * 100);
        return;
      } catch (err) {
        lastError = err;
        await new Promise((r) => setTimeout(r, 1000 * Math.pow(2, i)));
      }
    }
    throw lastError;
  } catch (err: any) {
    uploadStatus.value = 'exception';
    errorMsg.value = `分片 ${index} 上传失败: ${err.message}`;
    throw err;
  }
}

function handleError(err: Error) {
  errorMsg.value = err.message;
  emit('error', err);
}

async function handleStop(info: { duration: number; size: number }) {
  uploadStatus.value = 'normal';
  try {
    message.loading('正在合并视频并计算指纹...', 0);
    const result = await mergeVideoChunks(props.sessionId);
    message.destroy();
    message.success('视频录制完成');
    emit('recordingStopped', {
      videoUrl: result.videoUrl,
      videoHash: result.videoHash,
      duration: info.duration,
      size: info.size,
    });
  } catch (err: any) {
    message.destroy();
    errorMsg.value = `视频合并失败: ${err.message}`;
  }
}

function startDurationTimer() {
  durationTimer = setInterval(() => {
    if (recorder) duration.value = recorder.currentDuration;
  }, 1000);
}

function stopDurationTimer() {
  if (durationTimer) {
    clearInterval(durationTimer);
    durationTimer = null;
  }
}

watch(() => props.sessionId, (newId) => {
  // 会话变化时,清理旧资源
  if (recorder) {
    recorder.release();
    recorder = null;
  }
  deviceReady.value = false;
  isRecording.value = false;
  isPaused.value = false;
  duration.value = 0;
  uploadedChunks.value = 0;
});

onMounted(() => {
  // 自动请求设备权限
  setTimeout(() => {
    initDevice();
  }, 500);
});

onUnmounted(() => {
  stopDurationTimer();
  if (recorder) {
    recorder.release();
  }
  if (videoEl.value?.srcObject) {
    (videoEl.value.srcObject as MediaStream).getTracks().forEach((t) => t.stop());
  }
});
</script>

<style scoped lang="scss">
.video-recorder {
  height: 100%;
  background: #2b2d42;
  border-radius: 8px;
  padding: 16px;
}

.recorder-layout {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  height: 100%;
}

.preview-area {
  position: relative;
  background: #1a1c2e;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.recording-overlay {
  position: absolute;
  top: 16px;
  left: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 13px;

  .rec-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: #d90429;
    animation: pulse 1.5s infinite;
    &.paused {
      background: #8d99ae;
      animation: none;
    }
  }
  .rec-time {
    font-family: 'Courier New', monospace;
    font-weight: 600;
  }
  .trust-time {
    color: #2a9d8f;
    font-size: 12px;
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.watermark {
  position: absolute;
  bottom: 16px;
  right: 16px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  padding: 4px 8px;
  font-size: 11px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
}

.control-panel {
  display: flex;
  flex-direction: column;
}

.status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;

  .label {
    color: #8d99ae;
    font-size: 13px;
  }
  .value {
    color: #2b2d42;
    font-weight: 500;
    font-family: 'Courier New', monospace;
  }
}

.requirements {
  list-style: none;
  padding: 0;
  margin: 0;
  color: #2b2d42;
  font-size: 13px;

  li {
    padding: 4px 0;
    display: flex;
    align-items: center;
    gap: 6px;
    :deep(.anticon) { color: #2a9d8f; }
  }
}
</style>
