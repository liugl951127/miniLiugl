<!--
  双录主页面 - 编排整个双录流程
  集成: 身份核验 → 风评 → 话术 → 视频录制 → 签约 → 质检
-->
<template>
  <div class="dual-record-page">
    <!-- 顶部进度条 -->
    <div class="page-header">
      <a-page-header
        :title="`双录办理 - 订单 ${order?.orderNo || ''}`"
        :sub-title="`${order?.productName} · 金额 ¥${(order?.amount || 0) / 100}`"
        @back="() => $router.back()"
      >
        <template #extra>
          <a-space>
            <a-tag color="blue">{{ getChannelName(order?.channel) }}</a-tag>
            <a-tag :color="getStateColor(order?.state)">{{ store.orderStateName }}</a-tag>
            <a-button @click="showExceptionModal = true" danger>
              <WarningOutlined /> 异常上报
            </a-button>
          </a-space>
        </template>
      </a-page-header>

      <a-steps
        :current="currentStepIndex"
        :items="steps"
        style="margin: 16px 24px;"
        size="small"
      />
    </div>

    <!-- 异常提示 -->
    <div class="exception-area" v-if="store.exceptionType">
      <ExceptionHandler
        :exception-type="store.exceptionType"
        :session-id="session?.sessionId"
        @recovered="onRecovered"
        @escalate="onEscalate"
        @abort="onAbort"
      />
    </div>

    <!-- 主内容区 - 根据步骤切换 -->
    <div class="main-content">
      <a-card :bordered="false">
        <!-- 步骤 1: 身份核验 -->
        <div v-show="currentStep === 'verify'" class="step-content">
          <h3>身份核验</h3>
          <VerifyIdentity
            v-if="customer"
            :customer="customer"
            :session-id="session?.sessionId"
            @verified="onVerified"
          />
        </div>

        <!-- 步骤 2: 风险评估 -->
        <div v-show="currentStep === 'risk'" class="step-content">
          <h3>风险评估</h3>
          <RiskAssessment
            v-if="order && session"
            :order-id="order.orderId"
            :session-id="session.sessionId"
            @completed="onRiskCompleted"
          />
        </div>

        <!-- 步骤 3 & 4: 话术执行 + 视频录制(合并) -->
        <div v-show="currentStep === 'script' || currentStep === 'record'" class="step-content">
          <a-row :gutter="16">
            <a-col :span="14">
              <h3>话术执行</h3>
              <ScriptRunner
                v-if="session && script"
                :script="script"
                :session-id="session.sessionId"
                @node-completed="onNodeCompleted"
                @all-completed="onAllNodesCompleted"
              />
            </a-col>
            <a-col :span="10">
              <h3>视频录制</h3>
              <VideoRecorder
                v-if="session"
                ref="recorderRef"
                :session-id="session.sessionId"
                :order-no="order?.orderNo || ''"
                :customer-name="customer?.name || ''"
                :product-type="order?.productType || 1"
                @recording-stopped="onRecordingStopped"
                @error="onRecorderError"
              />
            </a-col>
          </a-row>
        </div>

        <!-- 步骤 5: 电子签约 -->
        <div v-show="currentStep === 'sign'" class="step-content">
          <h3>电子签约</h3>
          <ESignature
            v-if="order && customer"
            :order-id="order.orderId"
            :customer-id="customer.customerId"
            :customer-name="customer.name"
            :id-no="customer.idNo"
            :mobile="customer.mobile"
            @completed="onSignCompleted"
          />
        </div>

        <!-- 步骤 6: 质检 -->
        <div v-show="currentStep === 'qa'" class="step-content">
          <h3>智能质检</h3>
          <QualityMonitor
            v-if="session"
            :session-id="session.sessionId"
            :auto-trigger="true"
            @completed="onQAPassed"
            @failed="onQAFailed"
          />
        </div>

        <!-- 步骤 7: 完成 -->
        <div v-show="currentStep === 'done'" class="step-content">
          <a-result
            status="success"
            title="双录业务办理完成"
            :sub-title="`订单 ${order?.orderNo} 已归档,可前往订单中心查询详情`"
          >
            <template #extra>
              <a-space>
                <a-button @click="onDownloadAll">下载完整资料</a-button>
                <a-button type="primary" @click="onNewRecord">办理下一笔</a-button>
              </a-space>
            </template>
          </a-result>
        </div>
      </a-card>
    </div>

    <!-- 异常上报弹窗 -->
    <a-modal
      v-model:open="showExceptionModal"
      title="异常上报"
      @ok="onSubmitException"
    >
      <a-form layout="vertical">
        <a-form-item label="异常类型" required>
          <a-select v-model:value="exceptionForm.type" placeholder="请选择">
            <a-select-option value="NETWORK">网络异常</a-select-option>
            <a-select-option value="DEVICE">设备故障</a-select-option>
            <a-select-option value="AV_ERROR">音视频异常</a-select-option>
            <a-select-option value="UPLOAD_ERROR">上传失败</a-select-option>
            <a-select-option value="CUSTOMER_REFUSE">客户拒答</a-select-option>
            <a-select-option value="CUSTOMER_LEAVE">客户离席</a-select-option>
            <a-select-option value="COMPLIANCE_FAIL">合规异常</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="异常描述" required>
          <a-textarea
            v-model:value="exceptionForm.description"
            :rows="4"
            placeholder="请详细描述异常情况..."
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, reactive } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message, Modal } from 'ant-design-vue';
import { WarningOutlined } from '@ant-design/icons-vue';
import { useDualRecordStore } from '@/store/dualRecord';
import { orderApi } from '@/api/order';
import { sessionApi } from '@/api/session';
import { scriptApi } from '@/api/script';
import { contractApi } from '@/api/risk';
import { orderApi as _orderApi } from '@/api/order';
import ScriptRunner from '@/components/ScriptRunner.vue';
import VideoRecorder from '@/components/VideoRecorder.vue';
import RiskAssessment from '@/components/RiskAssessment.vue';
import ESignature from '@/components/ESignature.vue';
import QualityMonitor from '@/components/QualityMonitor.vue';
import ExceptionHandler from '@/components/ExceptionHandler.vue';
import VerifyIdentity from '@/components/VerifyIdentity.vue';
import type {
  Order,
  Script,
  Session,
  RiskAssess,
  Customer,
  Channel,
  OrderState,
  ExceptionType,
} from '@/types';

const route = useRoute();
const router = useRouter();
const store = useDualRecordStore();

// 业务数据
const order = computed(() => store.order);
const script = computed(() => store.script);
const session = computed(() => store.session);
const customer = computed(() => store.customer);

const currentStep = ref<'verify' | 'risk' | 'script' | 'record' | 'sign' | 'qa' | 'done'>('verify');

const steps = [
  { title: '身份核验' },
  { title: '风险评估' },
  { title: '话术执行' },
  { title: '电子签约' },
  { title: '智能质检' },
  { title: '完成' },
];

const currentStepIndex = computed(() => {
  const map: Record<string, number> = {
    verify: 0, risk: 1, script: 2, record: 2, sign: 3, qa: 4, done: 5,
  };
  return map[currentStep.value] || 0;
});

const showExceptionModal = ref(false);
const exceptionForm = reactive<{ type: ExceptionType; description: string }>({
  type: 'NETWORK',
  description: '',
});

const recorderRef = ref<InstanceType<typeof VideoRecorder> | null>(null);

function getChannelName(channel?: Channel): string {
  const map: Record<Channel, string> = {
    1: '线上 H5', 2: '小程序', 3: '线下一体机', 4: 'PAD', 5: 'PC',
  };
  return channel ? map[channel] : '';
}

function getStateColor(state?: OrderState): string {
  if (state === undefined) return 'default';
  if (state === 6) return 'green';
  if (state < 0) return 'red';
  if (state >= 4) return 'blue';
  return 'cyan';
}

// ========== 流程回调 ==========
async function onVerified() {
  message.success('身份核验通过');
  await sessionApi.advanceState(session.value!.sessionId);
  currentStep.value = 'risk';
}

async function onRiskCompleted(assess: RiskAssess) {
  message.success('风险评估完成');
  await sessionApi.advanceState(session.value!.sessionId);
  // 检查产品匹配
  if (!assess.productMatch) {
    Modal.warning({
      title: '风险等级与产品不匹配',
      content: `您的风险等级为 ${assess.riskLevel},不适合该产品,建议改选其他产品。`,
    });
    return;
  }
  currentStep.value = 'script';
}

async function onNodeCompleted() {
  // 节点完成时静默处理
}

async function onAllNodesCompleted() {
  message.success('所有话术节点执行完成');
  // 停止录制
  if (recorderRef.value) {
    // 由 VideoRecorder 自己处理 onStop
  }
  await sessionApi.advanceState(session.value!.sessionId);
  currentStep.value = 'sign';
}

async function onRecordingStopped(data: { videoUrl: string; videoHash: string; duration: number }) {
  await sessionApi.merge(session.value!.sessionId);
  message.success('视频录制完成,准备签约');
}

async function onRecorderError(err: Error) {
  store.setError(err.message, 'AV_ERROR');
}

async function onSignCompleted() {
  message.success('合同签署完成');
  await sessionApi.advanceState(session.value!.sessionId);
  currentStep.value = 'qa';
}

async function onQAPassed() {
  await sessionApi.advanceState(session.value!.sessionId);
  // 完成订单
  await orderApi.advanceState(order.value!.orderId, 6, '双录完成');
  currentStep.value = 'done';
}

async function onQAFailed() {
  Modal.error({
    title: '质检未通过',
    content: '请检查问题列表后重新办理或转人工处理。',
    onOk: () => router.push(`/order/${order.value!.orderId}`),
  });
}

function onRecovered() {
  store.setError(null);
}

function onEscalate(to: string) {
  message.info(`已升级到: ${to}`);
  // 实际业务中根据不同类型处理
}

function onAbort() {
  message.warning('本次办理已终止');
  router.push('/orders');
}

function onSubmitException() {
  if (!exceptionForm.description) {
    message.warning('请填写异常描述');
    return;
  }
  store.setError(exceptionForm.description, exceptionForm.type);
  showExceptionModal.value = false;
  exceptionForm.description = '';
  message.success('异常已上报,处理人员将尽快介入');
}

function onDownloadAll() {
  message.info('正在准备下载包...');
}

function onNewRecord() {
  store.reset();
  router.push('/order/create');
}

// ========== 初始化 ==========
onMounted(async () => {
  const orderId = Number(route.params.orderId);
  try {
    // 加载订单
    const o = await orderApi.getById(orderId);
    store.setOrder(o);

    // 启动会话
    const s = await sessionApi.start({
      orderId,
      channel: o.channel,
      terminalId: 'WEB-' + Date.now(),
      ipAddress: '',  // 由后端获取
      location: '',
    });
    store.setSession(s);

    // 加载话术
    const sc = await scriptApi.pull(o.productType, o.productId);
    store.setScript(sc);

    // 加载客户信息(实际应从订单获取或单独接口)
    // const c = await customerApi.getById(o.customerId);
    // store.setCustomer(c);

    currentStep.value = 'verify';
  } catch (err: any) {
    message.error(`初始化失败: ${err.message}`);
  }
});

onUnmounted(() => {
  // 离开页面时清理
});
</script>

<style scoped lang="scss">
.dual-record-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 24px;
}

.page-header {
  background: #fff;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  margin-bottom: 16px;
}

.exception-area {
  margin: 0 24px 16px;
}

.main-content {
  margin: 0 24px;

  :deep(.ant-card) {
    border-radius: 8px;
  }
}

.step-content {
  padding: 16px 0;
  min-height: 600px;

  h3 {
    color: #2b2d42;
    margin-bottom: 16px;
    font-size: 18px;
    font-weight: 600;
    border-left: 4px solid #d90429;
    padding-left: 12px;
  }
}
</style>
