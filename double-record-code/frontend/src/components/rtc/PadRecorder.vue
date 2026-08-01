<template>
  <div class="pad-recorder">
    <!-- 视频预览区 -->
    <div class="video-area">
      <!-- 本地视频 -->
      <video
        ref="localVideo"
        autoplay
        muted
        playsinline
        class="local-video"
        :class="{ 'is-hidden': !videoEnabled }"
      />
      <!-- 远端视频(PAD 主屏) -->
      <video
        ref="remoteVideo"
        autoplay
        playsinline
        class="remote-video"
        v-if="remoteStream"
      />
      <!-- 录制中指示 -->
      <div class="recording-badge" v-if="isRecording">
        <span class="rec-dot"></span>
        <span class="rec-text">REC</span>
        <span class="rec-time">{{ formatTime(recordingTime) }}</span>
      </div>
      <!-- 音频电平条 -->
      <div class="audio-meter" v-if="audioLevel > 0">
        <div
          class="audio-meter-fill"
          :style="{ width: `${audioLevel * 100}%` }"
        ></div>
      </div>
    </div>

    <!-- 控制条 -->
    <div class="control-bar">
      <el-button
        type="primary"
        :icon="VideoCamera"
        :class="{ 'is-disabled': !videoEnabled }"
        @click="toggleVideo"
        circle
      />
      <el-button
        type="primary"
        :icon="Microphone"
        :class="{ 'is-disabled': !audioEnabled }"
        @click="toggleAudio"
        circle
      />
      <el-button
        :type="isRecording ? 'danger' : 'success'"
        :icon="isRecording ? VideoPause : VideoPlay"
        @click="toggleRecord"
        circle
        size="large"
      />
      <el-button
        type="warning"
        :icon="Switch"
        @click="switchCamera_"
        circle
        v-if="hasDualCamera"
      />
      <el-button
        type="info"
        :icon="Connection"
        @click="toggleSfu"
        circle
      />
    </div>

    <!-- 网络状态 -->
    <div class="network-info" v-if="sfuConnected">
      <span class="net-quality">RTT: {{ rtt }}ms</span>
      <span class="net-quality">分辨率: {{ videoWidth }}×{{ videoHeight }}</span>
      <span class="net-quality">码率: {{ bitrate }}kbps</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { ElMessage } from 'element-plus';
import {
  VideoCamera,
  VideoPlay,
  VideoPause,
  Microphone,
  Switch,
  Connection,
} from '@element-plus/icons-vue';
import {
  SfuClient,
  MediaRecorderPolyfill,
  AudioLevelMeter,
  type SfuConfig,
} from '@/utils/webrtc';

interface Props {
  sessionId: string;
  orderId: string;
  userId: string;
  userRole: 'CUSTOMER' | 'MANAGER' | 'WITNESS';
  sfuUrl: string;
  /** 录制分片回调 */
  onChunk?: (chunk: Blob, index: number, duration: number, hash: string) => void;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'recording-start'): void;
  (e: 'recording-stop', data: { duration: number; chunks: number }): void;
  (e: 'remote-stream', payload: { userId: string; stream: MediaStream }): void;
  (e: 'error', err: Error): void;
}>();

const localVideo = ref<HTMLVideoElement | null>(null);
const remoteVideo = ref<HTMLVideoElement | null>(null);

const videoEnabled = ref(true);
const audioEnabled = ref(true);
const isRecording = ref(false);
const recordingTime = ref(0);
const audioLevel = ref(0);
const sfuConnected = ref(false);
const remoteStream = ref<MediaStream | null>(null);
const hasDualCamera = ref(false);
const rtt = ref(0);
const videoWidth = ref(0);
const videoHeight = ref(0);
const bitrate = ref(0);

let sfu: SfuClient | null = null;
let recorder: MediaRecorderPolyfill | null = null;
let meter: AudioLevelMeter | null = null;
let recordingTimer: ReturnType<typeof setInterval> | null = null;
let networkTimer: ReturnType<typeof setInterval> | null = null;
let rttStart = 0;

// ============================================================
// 生命周期
// ============================================================
onMounted(async () => {
  await initialize();
});

onBeforeUnmount(() => {
  cleanup();
});

// ============================================================
// 初始化
// ============================================================
async function initialize() {
  // 1. 加入 SFU
  sfu = new SfuClient({
    url: props.sfuUrl,
    roomId: props.sessionId,
    userId: props.userId,
    role: props.userRole,
    video: true,
    audio: true,
    videoResolution: '720p',
  } as SfuConfig);

  // 订阅远端流
  sfu.on('remote-stream', (payload) => {
    remoteStream.value = payload.stream;
    if (remoteVideo.value) {
      remoteVideo.value.srcObject = payload.stream;
    }
    emit('remote-stream', payload);
  });

  sfu.on('error', (err) => {
    ElMessage.error(`RTC 错误: ${err.message || err}`);
    emit('error', err);
  });

  sfu.on('state', (s) => {
    if (s.state === 'joined') sfuConnected.value = true;
    if (s.state === 'disconnected' || s.state === 'left') sfuConnected.value = false;
  });

  // 2. 加入房间
  try {
    const localStream = await sfu.join();
    if (localVideo.value) {
      localVideo.value.srcObject = localStream;
    }
    // 探测双摄
    if (navigator.mediaDevices.enumerateDevices) {
      const devices = await navigator.mediaDevices.enumerateDevices();
      hasDualCamera.value = devices.filter((d) => d.kind === 'videoinput').length > 1;
    }
    // 启动音频电平
    meter = new AudioLevelMeter((level) => {
      audioLevel.value = level;
    });
    meter.start(localStream);
  } catch (e) {
    ElMessage.error(`加入房间失败: ${(e as Error).message}`);
    emit('error', e as Error);
  }
}

// ============================================================
// 控制
// ============================================================
function toggleVideo() {
  videoEnabled.value = !videoEnabled.value;
  sfu?.disableVideo(!videoEnabled.value);
}

function toggleAudio() {
  audioEnabled.value = !audioEnabled.value;
  sfu?.mute(!audioEnabled.value);
}

async function switchCamera_() {
  if (!sfu || !localVideo.value) return;
  const newStream = await sfu.switchCamera(
    videoEnabled.value ? 'environment' : 'user'
  );
  localVideo.value.srcObject = newStream;
}

function toggleSfu() {
  if (sfuConnected.value) {
    sfu?.leave();
    ElMessage.info('已断开 SFU 连接');
  } else {
    initialize();
  }
}

async function toggleRecord() {
  if (isRecording.value) {
    stopRecord();
  } else {
    startRecord();
  }
}

function startRecord() {
  if (!localVideo.value?.srcObject) {
    ElMessage.warning('请先开启视频');
    return;
  }
  recorder = new MediaRecorderPolyfill({ chunkDuration: 3000 });
  recorder.start(
    localVideo.value.srcObject as MediaStream,
    (chunk, index, duration, hash) => {
      // 上传分片到后端
      props.onChunk?.(chunk, index, duration, hash);
    },
    (err) => {
      ElMessage.error(`录制错误: ${err.message}`);
      emit('error', err);
    }
  );
  isRecording.value = true;
  recordingTime.value = 0;
  recordingTimer = setInterval(() => {
    recordingTime.value = Date.now() - rttStart;
  }, 1000);
  rttStart = Date.now();
  emit('recording-start');
}

function stopRecord() {
  recorder?.stop();
  isRecording.value = false;
  if (recordingTimer) clearInterval(recordingTimer);
  emit('recording-stop', { duration: recordingTime.value, chunks: recorder ? 0 : 0 });
}

function formatTime(ms: number): string {
  const s = Math.floor(ms / 1000);
  const m = Math.floor(s / 60);
  return `${String(m).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;
}

function cleanup() {
  if (recordingTimer) clearInterval(recordingTimer);
  if (networkTimer) clearInterval(networkTimer);
  recorder?.stop();
  meter?.stop();
  sfu?.leave();
}
</script>

<style scoped lang="scss">
.pad-recorder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #000;
  border-radius: 8px;
  color: #fff;
}

.video-area {
  position: relative;
  width: 100%;
  max-width: 720px;
  aspect-ratio: 16/9;
  background: #1a1a1a;
  border-radius: 4px;
  overflow: hidden;
}

.local-video,
.remote-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.local-video {
  transform: scaleX(-1); /* 镜像 */
}

.recording-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 4px;
  font-size: 14px;

  .rec-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: #d90429;
    animation: pulse 1.2s ease-in-out infinite;
  }
  .rec-text { font-weight: 600; }
  .rec-time { opacity: 0.8; }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.audio-meter {
  position: absolute;
  bottom: 12px;
  left: 12px;
  right: 12px;
  height: 4px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
  overflow: hidden;

  .audio-meter-fill {
    height: 100%;
    background: #4ade80;
    transition: width 0.1s linear;
  }
}

.control-bar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.is-disabled {
  opacity: 0.5;
}

.network-info {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);

  .net-quality {
    padding: 2px 8px;
    background: rgba(255, 255, 255, 0.1);
    border-radius: 2px;
  }
}
</style>
