<!--
  话术执行组件
  强制按节点顺序执行,必须完成必读/必答/必确认
-->
<template>
  <div class="script-runner">
    <!-- 顶部进度条 -->
    <div class="progress-header">
      <div class="progress-info">
        <span class="step-tag">步骤 {{ currentNodeIndex + 1 }} / {{ script?.totalNodes || 0 }}</span>
        <span class="script-code">{{ script?.scriptCode }} · {{ script?.version }}</span>
      </div>
      <a-progress
        :percent="progress"
        :stroke-color="{ from: '#2b2d42', to: '#d90429' }"
        :show-info="true"
      />
    </div>

    <!-- 当前节点内容 -->
    <div class="node-card" v-if="currentNode">
      <div class="node-header">
        <a-tag :color="getTagColor(currentNode.nodeType)">
          {{ currentNode.nodeCode }} · {{ getNodeTypeName(currentNode.nodeType) }}
        </a-tag>
        <span class="node-name">{{ currentNode.nodeName }}</span>
        <span class="risk-tag" v-if="currentNode.riskLevel">
          风险等级 R{{ currentNode.riskLevel }}
        </span>
      </div>

      <div class="node-content" v-if="readDone">
        <p class="script-text">{{ currentNode.content }}</p>

        <!-- 关键词检测(实时显示) -->
        <div class="keywords-tracker" v-if="currentNode.keywords?.length">
          <div class="tracker-label">必含关键词:</div>
          <a-tag
            v-for="kw in currentNode.keywords"
            :key="kw"
            :color="detectedKeywords.includes(kw) ? 'success' : 'default'"
            style="margin: 2px;"
          >
            <CheckCircleOutlined v-if="detectedKeywords.includes(kw)" />
            <MinusCircleOutlined v-else />
            {{ kw }}
          </a-tag>
        </div>

        <!-- 客户确认区 -->
        <div class="confirm-area" v-if="currentNode.requireConfirm">
          <a-alert
            :message="`请客户口头确认「${currentNode.nodeName}」`"
            type="warning"
            show-icon
            style="margin-bottom: 12px;"
          />
          <a-space>
            <a-button
              type="primary"
              size="large"
              :loading="confirming"
              :disabled="!customerSpeakDetected"
              @click="onCustomerConfirm(true)"
            >
              <CheckOutlined /> 客户已确认
            </a-button>
            <a-button
              danger
              size="large"
              :disabled="!customerSpeakDetected"
              @click="onCustomerConfirm(false)"
            >
              <CloseOutlined /> 客户拒绝
            </a-button>
          </a-space>
          <div class="hint" v-if="!customerSpeakDetected">
            等待客户说话中... ({{ speakingCountdown }}s)
          </div>
        </div>

        <!-- 仅需读完(无需确认) -->
        <div class="read-only-area" v-else>
          <a-button
            type="primary"
            size="large"
            :loading="submitting"
            :disabled="!readTimeEnough"
            @click="onReadOnly"
          >
            {{ readTimeEnough ? '继续下一步' : `请完整阅读(${readingCountdown}s)` }}
          </a-button>
        </div>
      </div>

      <div class="node-content-placeholder" v-else>
        <a-spin tip="加载中..." />
      </div>
    </div>

    <!-- 节点导航 -->
    <div class="node-nav">
      <a-button
        :disabled="currentNodeIndex === 0"
        @click="prevNode"
      >
        <ArrowLeftOutlined /> 上一步
      </a-button>
      <div class="node-dots">
        <span
          v-for="(node, idx) in script?.nodes"
          :key="node.nodeId"
          :class="['dot', {
            active: idx === currentNodeIndex,
            done: idx < currentNodeIndex,
            failed: store.nodeResults[idx]?.result === 'FAIL'
          }]"
          :title="`${node.nodeCode} ${node.nodeName}`"
          @click="jumpTo(idx)"
        />
      </div>
      <a-button
        type="primary"
        :disabled="!store.canProceed"
        @click="nextNode"
      >
        下一步 <ArrowRightOutlined />
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { message } from 'ant-design-vue';
import {
  CheckCircleOutlined,
  MinusCircleOutlined,
  CheckOutlined,
  CloseOutlined,
  ArrowLeftOutlined,
  ArrowRightOutlined,
} from '@ant-design/icons-vue';
import { useDualRecordStore } from '@/store/dualRecord';
import { scriptApi } from '@/api/script';
import { ASRClient, checkKeywords } from '@/utils/asr';
import type { ScriptNode, NodeType } from '@/types';

const store = useDualRecordStore();

const props = defineProps<{
  script: any;
  sessionId: number;
}>();

const emit = defineEmits<{
  (e: 'nodeCompleted', result: any): void;
  (e: 'allCompleted'): void;
}>();

// 当前节点
const currentNode = computed<ScriptNode | null>(() => store.currentNode);
const currentNodeIndex = computed(() => store.currentNodeIndex);
const progress = computed(() => store.progress);

// 状态
const readDone = ref(false);
const readTimeEnough = ref(false);
const readingCountdown = ref(0);
const confirming = ref(false);
const submitting = ref(false);
const customerSpeakDetected = ref(false);
const speakingCountdown = ref(0);
const detectedKeywords = ref<string[]>([]);
const asrText = ref('');

let readingTimer: any = null;
let speakingTimer: any = null;
let asrClient: ASRClient | null = null;

watch(currentNode, (newNode) => {
  if (newNode) {
    initNode(newNode);
  }
});

function initNode(node: ScriptNode) {
  readDone.value = false;
  readTimeEnough.value = false;
  customerSpeakDetected.value = false;
  detectedKeywords.value = [];
  asrText.value = '';
  confirming.value = false;
  submitting.value = false;

  // TTS 播放(可选,实际用浏览器 SpeechSynthesis)
  if ('speechSynthesis' in window) {
    const utter = new SpeechSynthesisUtterance(node.content);
    utter.lang = 'zh-CN';
    utter.rate = 0.9;
    speechSynthesis.speak(utter);
  }

  // 设置最短阅读时间
  const minRead = node.minReadSeconds || 5;
  readingCountdown.value = minRead;
  readingTimer = setInterval(() => {
    readingCountdown.value--;
    if (readingCountdown.value <= 0) {
      readTimeEnough.value = true;
      readDone.value = true;
      clearInterval(readingTimer!);
    }
  }, 1000);

  // 启动 ASR 监听(用于关键词检测)
  startASR(node);
}

function startASR(node: ScriptNode) {
  asrClient = new ASRClient({
    sessionId: props.sessionId,
    language: 'zh-CN',
    sampleRate: 16000,
    onResult: (text, isFinal) => {
      asrText.value = text;
      if (text.trim().length > 0) {
        customerSpeakDetected.value = true;
        resetSpeakingTimer();
      }
      if (isFinal && node.keywords?.length) {
        const { matched } = checkKeywords(text, node.keywords);
        for (const kw of matched) {
          if (!detectedKeywords.value.includes(kw)) {
            detectedKeywords.value.push(kw);
          }
        }
      }
    },
    onError: (err) => {
      console.warn('ASR 错误:', err.message);
    },
  });
  asrClient.start();
}

function resetSpeakingTimer() {
  if (speakingTimer) clearTimeout(speakingTimer);
  speakingCountdown.value = 5;
  speakingTimer = setTimeout(() => {
    customerSpeakDetected.value = false;
  }, 5000);
}

async function onCustomerConfirm(confirmed: boolean) {
  if (!currentNode.value) return;
  confirming.value = true;
  try {
    const result = {
      nodeCode: currentNode.value.nodeCode,
      result: confirmed ? 'PASS' : 'FAIL' as const,
      duration: currentNode.value.minReadSeconds - readingCountdown.value,
      customerConfirmed: confirmed,
      asrText: asrText.value,
      detectedKeywords: detectedKeywords.value,
      startedAt: new Date(Date.now() - (currentNode.value.minReadSeconds - readingCountdown.value) * 1000).toISOString(),
      endedAt: new Date().toISOString(),
    };
    await scriptApi.submitNodeResult({
      sessionId: props.sessionId,
      ...result,
    });
    store.setNodeResult(result);
    emit('nodeCompleted', result);
    confirming.value = false;
    if (confirmed) {
      // 自动进入下一节点
      if (!store.isLastNode) {
        setTimeout(() => nextNode(), 500);
      } else {
        emit('allCompleted');
      }
    }
  } catch (err: any) {
    message.error(`提交失败: ${err.message}`);
    confirming.value = false;
  }
}

async function onReadOnly() {
  if (!currentNode.value) return;
  submitting.value = true;
  try {
    const result = {
      nodeCode: currentNode.value.nodeCode,
      result: 'PASS' as const,
      duration: currentNode.value.minReadSeconds - readingCountdown.value,
      customerConfirmed: true,
      asrText: asrText.value,
      detectedKeywords: detectedKeywords.value,
      startedAt: new Date(Date.now() - (currentNode.value.minReadSeconds - readingCountdown.value) * 1000).toISOString(),
      endedAt: new Date().toISOString(),
    };
    await scriptApi.submitNodeResult({
      sessionId: props.sessionId,
      ...result,
    });
    store.setNodeResult(result);
    emit('nodeCompleted', result);
    submitting.value = false;
    if (!store.isLastNode) {
      setTimeout(() => nextNode(), 500);
    } else {
      emit('allCompleted');
    }
  } catch (err: any) {
    message.error(`提交失败: ${err.message}`);
    submitting.value = false;
  }
}

function nextNode() {
  if (asrClient) asrClient.stop();
  if (readingTimer) clearInterval(readingTimer);
  if (speakingTimer) clearTimeout(speakingTimer);
  store.nextNode();
}

function prevNode() {
  if (store.currentNodeIndex > 0) {
    store.currentNodeIndex--;
  }
}

function jumpTo(idx: number) {
  // 只允许跳到已完成的节点
  if (idx <= store.currentNodeIndex) {
    store.currentNodeIndex = idx;
  }
}

function getTagColor(type: NodeType): string {
  const map: Record<NodeType, string> = {
    GREETING: 'blue',
    PRODUCT: 'cyan',
    RISK_DISCLOSURE: 'red',
    SUITABILITY: 'orange',
    COOLING_PERIOD: 'purple',
    CONFIRMATION: 'green',
  };
  return map[type] || 'default';
}

function getNodeTypeName(type: NodeType): string {
  const map: Record<NodeType, string> = {
    GREETING: '问候',
    PRODUCT: '产品',
    RISK_DISCLOSURE: '风险揭示',
    SUITABILITY: '适当性',
    COOLING_PERIOD: '犹豫期',
    CONFIRMATION: '确认',
  };
  return map[type] || type;
}

onMounted(() => {
  if (currentNode.value) {
    initNode(currentNode.value);
  }
});

onUnmounted(() => {
  if (asrClient) asrClient.stop();
  if (readingTimer) clearInterval(readingTimer);
  if (speakingTimer) clearTimeout(speakingTimer);
  if ('speechSynthesis' in window) {
    speechSynthesis.cancel();
  }
});
</script>

<style scoped lang="scss">
.script-runner {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
}

.progress-header {
  background: #fff;
  padding: 12px 20px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);

  .progress-info {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
    font-size: 14px;
    color: #2b2d42;

    .step-tag {
      font-weight: 600;
      color: #d90429;
    }
    .script-code {
      color: #8d99ae;
      font-family: 'Courier New', monospace;
    }
  }
}

.node-card {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  flex: 1;
  display: flex;
  flex-direction: column;
}

.node-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #edf2f4;

  .node-name {
    font-size: 20px;
    font-weight: 600;
    color: #2b2d42;
  }
  .risk-tag {
    margin-left: auto;
    color: #d90429;
    font-weight: 500;
  }
}

.node-content {
  flex: 1;
  display: flex;
  flex-direction: column;

  .script-text {
    font-size: 22px;
    line-height: 1.8;
    color: #2b2d42;
    background: #f8f9fa;
    padding: 24px;
    border-radius: 8px;
    border-left: 4px solid #d90429;
    margin-bottom: 20px;
    text-indent: 2em;
  }
}

.keywords-tracker {
  margin-bottom: 20px;

  .tracker-label {
    font-size: 13px;
    color: #8d99ae;
    margin-bottom: 8px;
  }
}

.confirm-area, .read-only-area {
  margin-top: auto;
  padding-top: 20px;
  border-top: 1px dashed #edf2f4;
  text-align: center;

  .hint {
    margin-top: 8px;
    color: #8d99ae;
    font-size: 13px;
  }
}

.node-content-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.node-nav {
  background: #fff;
  padding: 12px 20px;
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);

  .node-dots {
    display: flex;
    gap: 8px;
  }
  .dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background: #edf2f4;
    cursor: pointer;
    transition: all 0.2s;

    &.done { background: #2a9d8f; }
    &.active { background: #d90429; transform: scale(1.3); }
    &.failed { background: #8d99ae; }
  }
}
</style>
