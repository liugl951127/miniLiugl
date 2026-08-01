<!--
  异常处理组件
  统一处理网络/设备/合规等异常,提供恢复路径
-->
<template>
  <div class="exception-handler">
    <a-alert
      v-if="visible"
      :type="alertType"
      :message="title"
      :description="description"
      show-icon
      :closable="closable"
      style="margin-bottom: 16px;"
    >
      <template #action>
        <a-space>
          <a-button
            v-for="action in actions"
            :key="action.key"
            :type="action.primary ? 'primary' : 'default'"
            :danger="action.danger"
            :loading="loading === action.key"
            size="small"
            @click="onAction(action)"
          >
            {{ action.label }}
          </a-button>
        </a-space>
      </template>
    </a-alert>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { Modal, message } from 'ant-design-vue';
import { useDualRecordStore } from '@/store/dualRecord';
import { sessionApi } from '@/api/session';
import type { ExceptionType } from '@/types';

interface Action {
  key: string;
  label: string;
  primary?: boolean;
  danger?: boolean;
  handler: () => Promise<void>;
}

const props = defineProps<{
  exceptionType: ExceptionType;
  sessionId?: number;
  description?: string;
  closable?: boolean;
}>();

const emit = defineEmits<{
  (e: 'recovered'): void;
  (e: 'escalate', to: string): void;
  (e: 'abort'): void;
}>();

const store = useDualRecordStore();
const loading = ref<string | null>(null);

const visible = computed(() => !!props.exceptionType);

const alertType = computed(() => {
  const map: Record<ExceptionType, 'error' | 'warning' | 'info'> = {
    NETWORK: 'warning',
    DEVICE: 'error',
    AV_ERROR: 'error',
    UPLOAD_ERROR: 'warning',
    CUSTOMER_REFUSE: 'warning',
    SCRIPT_INTERRUPT: 'warning',
    CUSTOMER_LEAVE: 'info',
    COMPLIANCE_FAIL: 'error',
  };
  return map[props.exceptionType] || 'warning';
});

const title = computed(() => {
  const map: Record<ExceptionType, string> = {
    NETWORK: '网络异常',
    DEVICE: '设备故障',
    AV_ERROR: '音视频异常',
    UPLOAD_ERROR: '视频上传失败',
    CUSTOMER_REFUSE: '客户拒答',
    SCRIPT_INTERRUPT: '话术中断',
    CUSTOMER_LEAVE: '客户中途离席',
    COMPLIANCE_FAIL: '合规检查未通过',
  };
  return map[props.exceptionType] || '未知异常';
});

const actions = computed<Action[]>(() => {
  if (!props.exceptionType) return [];

  switch (props.exceptionType) {
    case 'NETWORK':
      return [
        {
          key: 'retry',
          label: '重试连接',
          primary: true,
          handler: async () => {
            if (!navigator.onLine) {
              message.error('当前仍无网络连接');
              return;
            }
            message.success('网络已恢复,继续执行');
            emit('recovered');
          },
        },
        {
          key: 'cache',
          label: '本地缓存继续',
          handler: async () => {
            message.info('已切换到本地缓存模式,网络恢复后自动同步');
            emit('recovered');
          },
        },
        {
          key: 'transfer',
          label: '转线下继续',
          handler: async () => {
            emit('escalate', 'OFFLINE');
          },
        },
      ];

    case 'DEVICE':
    case 'AV_ERROR':
      return [
        {
          key: 'reinit',
          label: '重新初始化设备',
          primary: true,
          handler: async () => {
            emit('recovered');
          },
        },
        {
          key: 'switch',
          label: '切换终端',
          handler: async () => {
            Modal.confirm({
              title: '切换终端',
              content: '当前会话将自动保存,可在其他终端继续',
              onOk: () => emit('escalate', 'SWITCH'),
            });
          },
        },
        {
          key: 'abort',
          label: '终止本次办理',
          danger: true,
          handler: async () => {
            await abortSession('设备故障');
            emit('abort');
          },
        },
      ];

    case 'UPLOAD_ERROR':
      return [
        {
          key: 'retry_upload',
          label: '重新上传',
          primary: true,
          handler: async () => {
            message.info('正在重试上传分片...');
            emit('recovered');
          },
        },
        {
          key: 'manual',
          label: '人工处理',
          danger: true,
          handler: async () => {
            emit('escalate', 'MANUAL');
          },
        },
      ];

    case 'CUSTOMER_REFUSE':
      return [
        {
          key: 'redo',
          label: '重新执行当前节点',
          primary: true,
          handler: async () => {
            message.info('已重新进入当前节点');
            emit('recovered');
          },
        },
        {
          key: 'manager',
          label: '客户经理介入',
          handler: async () => {
            emit('escalate', 'MANAGER');
          },
        },
        {
          key: 'abort',
          label: '终止办理',
          danger: true,
          handler: async () => {
            await abortSession('客户拒答');
            emit('abort');
          },
        },
      ];

    case 'SCRIPT_INTERRUPT':
      return [
        {
          key: 'continue',
          label: '继续当前节点',
          primary: true,
          handler: async () => {
            emit('recovered');
          },
        },
        {
          key: 'restart_node',
          label: '重新开始当前节点',
          handler: async () => {
            emit('recovered');
          },
        },
      ];

    case 'CUSTOMER_LEAVE':
      return [
        {
          key: 'wait',
          label: '等待客户返回',
          primary: true,
          handler: async () => {
            message.info('当前会话已暂停,客户返回后继续');
            await sessionApi.pause(props.sessionId!, '客户离席');
          },
        },
        {
          key: 'rebook',
          label: '7 天内重新预约',
          handler: async () => {
            emit('escalate', 'REBOOK');
          },
        },
      ];

    case 'COMPLIANCE_FAIL':
      return [
        {
          key: 'redo',
          label: '重新执行合规节点',
          primary: true,
          handler: async () => {
            emit('recovered');
          },
        },
        {
          key: 'review',
          label: '升级人工审核',
          handler: async () => {
            emit('escalate', 'COMPLIANCE_REVIEW');
          },
        },
        {
          key: 'abort',
          label: '终止办理',
          danger: true,
          handler: async () => {
            await abortSession('合规检查未通过');
            emit('abort');
          },
        },
      ];

    default:
      return [];
  }
});

async function onAction(action: Action) {
  loading.value = action.key;
  try {
    await action.handler();
  } catch (err: any) {
    message.error(`操作失败: ${err.message}`);
  } finally {
    loading.value = null;
  }
}

async function abortSession(reason: string) {
  if (!props.sessionId) return;
  try {
    await sessionApi.abort(props.sessionId, reason, false);
  } catch (err: any) {
    console.error('终止会话失败:', err);
  }
}

watch(() => props.exceptionType, (newType) => {
  if (newType) {
    store.setError(title.value, newType);
  }
});
</script>

<style scoped lang="scss">
.exception-handler {
  :deep(.ant-alert) {
    border-radius: 8px;
  }
  :deep(.ant-alert-action) {
    margin-left: 16px;
  }
}
</style>
