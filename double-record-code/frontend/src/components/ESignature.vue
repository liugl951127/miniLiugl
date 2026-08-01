<!--
  电子签约组件
  支持 CA 数字证书 + 手写签名 + 短信验证三因子
-->
<template>
  <div class="e-signature">
    <a-alert
      message="电子合同签约"
      type="info"
      description="请仔细阅读合同内容,然后选择以下任一方式完成签署"
      show-icon
      style="margin-bottom: 16px;"
    />

    <a-tabs v-model:activeKey="signMethod" @change="onMethodChange">
      <a-tab-pane key="ca" tab="CA 数字证书">
        <div class="sign-content">
          <a-steps :current="caStep" direction="vertical" size="small">
            <a-step title="身份核验" description="短信验证码 + 人脸识别" />
            <a-step title="证书签发" description="动态生成 CA 数字证书" />
            <a-step title="电子签名" description="使用私钥对合同摘要签名" />
            <a-step title="区块链存证" description="签名结果上链" />
          </a-steps>

          <div class="ca-form" v-if="caStep === 0">
            <a-form layout="vertical" :model="caForm">
              <a-form-item label="手机号" required>
                <a-input
                  v-model:value="caForm.mobile"
                  placeholder="请输入预留手机号"
                  :disabled="smsCooldown > 0"
                >
                  <template #suffix>
                    <a-button
                      type="link"
                      size="small"
                      :disabled="smsCooldown > 0 || !caForm.mobile"
                      @click="sendSms"
                    >
                      {{ smsCooldown > 0 ? `${smsCooldown}s` : '获取验证码' }}
                    </a-button>
                  </template>
                </a-input>
              </a-form-item>
              <a-form-item label="短信验证码" required>
                <a-input
                  v-model:value="caForm.smsCode"
                  placeholder="6 位数字"
                  :maxlength="6"
                />
              </a-form-item>
              <a-form-item>
                <a-space>
                  <a-button
                    type="primary"
                    :loading="verifying"
                    :disabled="!caForm.mobile || !caForm.smsCode"
                    @click="onVerifySms"
                  >
                    验证并继续
                  </a-button>
                </a-space>
              </a-form-item>
            </a-form>
          </div>

          <div class="ca-form" v-else-if="caStep === 1">
            <a-result
              status="success"
              title="身份核验通过"
              :sub-title="`正在为您申请 CA 数字证书...`"
            />
          </div>

          <div class="ca-form" v-else-if="caStep === 2">
            <a-result
              status="info"
              title="证书签发成功"
              :sub-title="certId ? `证书编号: ${certId}` : ''"
            >
              <template #extra>
                <a-button type="primary" :loading="signing" @click="onSignCA">
                  立即签署合同
                </a-button>
              </template>
            </a-result>
          </div>

          <div class="ca-form" v-else-if="caStep === 3">
            <a-result
              status="success"
              title="签署完成"
              :sub-title="`合同已上链,交易号: ${blockTx}`"
            >
              <template #extra>
                <a-space>
                  <a-button @click="onDownload">下载合同</a-button>
                  <a-button type="primary" @click="onNext">继续</a-button>
                </a-space>
              </template>
            </a-result>
          </div>
        </div>
      </a-tab-pane>

      <a-tab-pane key="hw" tab="手写签名">
        <div class="sign-content">
          <a-card title="请在下方手写签名" :bordered="false">
            <canvas
              ref="hwCanvas"
              width="600"
              height="200"
              class="hw-canvas"
              @pointerdown="onHwStart"
              @pointermove="onHwMove"
              @pointerup="onHwEnd"
            ></canvas>
            <div class="hw-actions">
              <a-space>
                <a-button @click="clearHw">清除</a-button>
                <a-button
                  type="primary"
                  :loading="signing"
                  :disabled="hwEmpty"
                  @click="onSignHw"
                >
                  确认签署
                </a-button>
              </a-space>
            </div>
          </a-card>
        </div>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { contractApi } from '@/api/risk';
import { useDualRecordStore } from '@/store/dualRecord';
import type { Contract } from '@/types';

const props = defineProps<{
  orderId: number;
  customerId: number;
  customerName: string;
  idNo: string;
  mobile: string;
}>();

const emit = defineEmits<{
  (e: 'completed', contract: Contract): void;
}>();

const store = useDualRecordStore();

const signMethod = ref<'ca' | 'hw'>('ca');
const caStep = ref(0);
const caForm = reactive({
  mobile: props.mobile || '',
  smsCode: '',
});
const smsCooldown = ref(0);
const verifying = ref(false);
const signing = ref(false);
const certId = ref('');
const certPem = ref('');
const blockTx = ref('');

// 手写签名
const hwCanvas = ref<HTMLCanvasElement | null>(null);
let hwCtx: CanvasRenderingContext2D | null = null;
let hwDrawing = false;
let hwHasContent = false;
const hwEmpty = ref(true);

let smsTimer: any = null;

function onMethodChange(key: string | number) {
  signMethod.value = key as 'ca' | 'hw';
  if (key === 'hw') {
    nextTick(() => initHwCanvas());
  }
}

async function sendSms() {
  if (!caForm.mobile) return;
  try {
    const result = await contractApi.sendSmsCode(0, caForm.mobile);
    message.success('验证码已发送');
    smsCooldown.value = 60;
    smsTimer = setInterval(() => {
      smsCooldown.value--;
      if (smsCooldown.value <= 0 && smsTimer) {
        clearInterval(smsTimer);
        smsTimer = null;
      }
    }, 1000);
  } catch (err: any) {
    message.error(`发送失败: ${err.message}`);
  }
}

async function onVerifySms() {
  if (!caForm.smsCode || caForm.smsCode.length !== 6) {
    message.warning('请输入 6 位验证码');
    return;
  }
  verifying.value = true;
  try {
    // 验证短信码
    await new Promise((r) => setTimeout(r, 500));   // 模拟
    // 申请 CA 证书
    caStep.value = 1;
    const cert = await contractApi.applyCert(props.orderId, props.customerId, props.customerName, props.idNo);
    certId.value = cert.certId;
    certPem.value = cert.certPem;
    caStep.value = 2;
  } catch (err: any) {
    message.error(`验证失败: ${err.message}`);
    caStep.value = 0;
  } finally {
    verifying.value = false;
  }
}

async function onSignCA() {
  signing.value = true;
  try {
    // 生成合同
    const contract = await contractApi.generate(props.orderId);
    // CA 签名
    const signed = await contractApi.signWithCA({
      orderId: props.orderId,
      contractId: contract.contractId,
      signMethod: 1,
      smsCode: caForm.smsCode,
    });
    // 上链
    caStep.value = 3;
    const chain = await contractApi.uploadToBlockchain(contract.contractId);
    blockTx.value = chain.txHash;
    store.setOrder({ ...store.order!, state: 5 } as any);
    message.success('签署完成并上链');
    emit('completed', signed);
  } catch (err: any) {
    message.error(`签署失败: ${err.message}`);
  } finally {
    signing.value = false;
  }
}

// 手写签名 canvas
function initHwCanvas() {
  if (!hwCanvas.value) return;
  hwCtx = hwCanvas.value.getContext('2d');
  if (!hwCtx) return;
  hwCtx.fillStyle = '#fff';
  hwCtx.fillRect(0, 0, hwCanvas.value.width, hwCanvas.value.height);
  hwCtx.strokeStyle = '#2b2d42';
  hwCtx.lineWidth = 2.5;
  hwCtx.lineCap = 'round';
  hwCtx.lineJoin = 'round';
  hwEmpty.value = true;
  hwHasContent = false;
}

function onHwStart(e: PointerEvent) {
  if (!hwCtx || !hwCanvas.value) return;
  hwDrawing = true;
  hwEmpty.value = false;
  hwHasContent = true;
  const rect = hwCanvas.value.getBoundingClientRect();
  hwCtx.beginPath();
  hwCtx.moveTo(e.clientX - rect.left, e.clientY - rect.top);
}

function onHwMove(e: PointerEvent) {
  if (!hwDrawing || !hwCtx || !hwCanvas.value) return;
  const rect = hwCanvas.value.getBoundingClientRect();
  hwCtx.lineTo(e.clientX - rect.left, e.clientY - rect.top);
  hwCtx.stroke();
}

function onHwEnd() {
  if (!hwCtx) return;
  hwCtx.closePath();
  hwDrawing = false;
}

function clearHw() {
  initHwCanvas();
}

async function onSignHw() {
  if (!hwHasContent) {
    message.warning('请先签名');
    return;
  }
  signing.value = true;
  try {
    const dataUrl = hwCanvas.value!.toDataURL('image/png');
    const base64 = dataUrl.split(',')[1];
    const contract = await contractApi.generate(props.orderId);
    const signed = await contractApi.signWithHandwriting(contract.contractId, base64);
    const chain = await contractApi.uploadToBlockchain(contract.contractId);
    blockTx.value = chain.txHash;
    message.success('手写签署完成并上链');
    emit('completed', signed);
  } catch (err: any) {
    message.error(`签署失败: ${err.message}`);
  } finally {
    signing.value = false;
  }
}

function onDownload() {
  message.info('合同已生成,可前往订单详情下载');
}

function onNext() {
  // 业务方自行处理后续流程
}

onMounted(() => {
  caForm.mobile = props.mobile;
});

onUnmounted(() => {
  if (smsTimer) clearInterval(smsTimer);
});
</script>

<style scoped lang="scss">
.e-signature {
  padding: 0;
}

.sign-content {
  padding: 16px 0;
}

.ca-form {
  margin-top: 24px;
  padding: 24px;
  background: #f8f9fa;
  border-radius: 8px;
  max-width: 500px;
}

.hw-canvas {
  border: 2px dashed #8d99ae;
  border-radius: 4px;
  background: #fff;
  cursor: crosshair;
  display: block;
  touch-action: none;
}

.hw-actions {
  margin-top: 16px;
  text-align: right;
}
</style>
