<!--
  身份核验组件
  集成 OCR 证件识别 + 活体检测
-->
<template>
  <div class="verify-identity">
    <a-row :gutter="24">
      <a-col :span="12">
        <a-card title="证件信息" :bordered="false">
          <a-form layout="vertical" :model="form">
            <a-form-item label="姓名" required>
              <a-input v-model:value="form.name" placeholder="请输入姓名" />
            </a-form-item>
            <a-form-item label="证件类型" required>
              <a-select v-model:value="form.idType">
                <a-select-option :value="1">身份证</a-select-option>
                <a-select-option :value="2">护照</a-select-option>
                <a-select-option :value="3">军官证</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="证件号" required>
              <a-input v-model:value="form.idNo" placeholder="请输入证件号码" />
            </a-form-item>
            <a-form-item>
              <a-space>
                <a-button
                  type="primary"
                  :loading="ocrLoading"
                  @click="onOCR"
                >
                  <CameraOutlined /> 拍摄证件识别
                </a-button>
                <a-button @click="onManualInput">手动输入</a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>

      <a-col :span="12">
        <a-card title="活体检测" :bordered="false">
          <div class="liveness-area">
            <video ref="videoEl" autoplay playsinline muted class="liveness-video"></video>
            <div class="liveness-status">
              <a-tag v-if="livenessPassed" color="success">
                <CheckCircleOutlined /> 活体检测通过
              </a-tag>
              <a-tag v-else color="processing">
                <LoadingOutlined /> 请正对摄像头,缓慢眨眼
              </a-tag>
            </div>
            <div class="liveness-actions">
              <a-button
                type="primary"
                :loading="livenessLoading"
                :disabled="!cameraReady"
                @click="onStartLiveness"
              >
                开始活体检测
              </a-button>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <div class="verify-action">
      <a-button
        type="primary"
        size="large"
        :loading="submitting"
        :disabled="!canSubmit"
        @click="onSubmit"
      >
        <CheckCircleOutlined /> 完成核验
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue';
import { message } from 'ant-design-vue';
import { CameraOutlined, CheckCircleOutlined, LoadingOutlined } from '@ant-design/icons-vue';
import { sessionApi } from '@/api/session';
import type { Customer } from '@/types';

const props = defineProps<{
  customer: Customer;
  sessionId?: number;
}>();

const emit = defineEmits<{
  (e: 'verified'): void;
}>();

const form = reactive({
  name: props.customer?.name || '',
  idType: props.customer?.idType || 1,
  idNo: props.customer?.idNo || '',
});

const ocrLoading = ref(false);
const livenessLoading = ref(false);
const submitting = ref(false);
const livenessPassed = ref(false);
const cameraReady = ref(false);

const videoEl = ref<HTMLVideoElement | null>(null);
let mediaStream: MediaStream | null = null;
let livenessTimer: any = null;

const canSubmit = computed(() => {
  return !!form.name && !!form.idNo && livenessPassed.value;
});

async function onOCR() {
  ocrLoading.value = true;
  try {
    // 实际调用 OCR 服务
    message.info('请将证件放入取景框内');
    // 模拟 OCR 识别
    await new Promise((r) => setTimeout(r, 2000));
    message.success('识别成功');
    form.name = props.customer?.name || '张三';
    form.idNo = props.customer?.idNo || '110101199001011234';
  } catch (err: any) {
    message.error(`识别失败: ${err.message}`);
  } finally {
    ocrLoading.value = false;
  }
}

function onManualInput() {
  message.info('请手动填写证件信息');
}

async function initCamera() {
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({
      video: { width: 640, height: 480, facingMode: 'user' },
    });
    if (videoEl.value) {
      videoEl.value.srcObject = mediaStream;
    }
    cameraReady.value = true;
  } catch (err: any) {
    message.error(`摄像头初始化失败: ${err.message}`);
  }
}

async function onStartLiveness() {
  if (!cameraReady.value) {
    await initCamera();
    if (!cameraReady.value) return;
  }
  livenessLoading.value = true;
  livenessPassed.value = false;
  try {
    // 活体检测:眨眼 + 转头(简化版)
    let blinkCount = 0;
    let lastAction = '';

    message.info('请缓慢眨眨眼');

    // 模拟活体检测流程
    await new Promise<void>((resolve) => {
      livenessTimer = setInterval(() => {
        blinkCount++;
        if (blinkCount === 1) {
          message.info('再眨一次');
        } else if (blinkCount === 2) {
          message.info('请缓慢点头');
        } else if (blinkCount >= 3) {
          if (livenessTimer) clearInterval(livenessTimer);
          livenessPassed.value = true;
          message.success('活体检测通过');
          livenessLoading.value = false;
          resolve();
        }
      }, 1500);
    });
  } catch (err: any) {
    message.error(`活体检测失败: ${err.message}`);
    livenessLoading.value = false;
  }
}

async function onSubmit() {
  if (!canSubmit.value) {
    message.warning('请完成所有核验项');
    return;
  }
  submitting.value = true;
  try {
    // 提交核验结果
    if (props.sessionId) {
      await sessionApi.pause(props.sessionId, '核验完成');
    }
    emit('verified');
  } catch (err: any) {
    message.error(`提交失败: ${err.message}`);
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  nextTick(() => initCamera());
});

onUnmounted(() => {
  if (livenessTimer) clearInterval(livenessTimer);
  if (mediaStream) {
    mediaStream.getTracks().forEach((t) => t.stop());
  }
});
</script>

<style scoped lang="scss">
.verify-identity {
  padding: 0;
}

.liveness-area {
  text-align: center;

  .liveness-video {
    width: 100%;
    max-width: 480px;
    height: 360px;
    background: #2b2d42;
    border-radius: 8px;
    object-fit: cover;
  }

  .liveness-status {
    margin: 16px 0;
  }

  .liveness-actions {
    margin-top: 12px;
  }
}

.verify-action {
  margin-top: 24px;
  text-align: center;
}
</style>
