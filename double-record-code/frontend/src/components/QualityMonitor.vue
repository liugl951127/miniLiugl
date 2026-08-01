<!--
  质检监控组件
  实时显示智能质检进度、分数、问题列表
-->
<template>
  <div class="quality-monitor">
    <a-spin :spinning="loading" tip="智能质检中...">
      <a-card :bordered="false">
        <template #title>
          <a-space>
            <span>智能质检</span>
            <a-tag v-if="result" :color="getVerdictColor(result.verdict)">
              {{ getVerdictName(result.verdict) }}
            </a-tag>
          </a-space>
        </template>

        <template #extra>
          <a-space>
            <a-button
              v-if="!result"
              type="primary"
              :loading="triggering"
              @click="onTrigger"
            >
              触发质检
            </a-button>
            <a-button v-if="result" @click="onRefresh">刷新</a-button>
          </a-space>
        </template>

        <div v-if="result" class="qa-content">
          <!-- 总分 -->
          <div class="total-score">
            <a-progress
              type="dashboard"
              :percent="result.totalScore"
              :stroke-color="getScoreColor(result.totalScore)"
              :format="(val: number) => val.toFixed(1)"
            />
            <div class="score-label">总分(0-100)</div>
          </div>

          <!-- 维度得分 -->
          <a-divider>分维度评分</a-divider>
          <a-row :gutter="16">
            <a-col :span="8">
              <a-statistic title="话术完整度" :value="result.scriptScore" :precision="1" suffix="/ 30" />
              <a-progress :percent="(result.scriptScore / 30) * 100" :show-info="false" />
            </a-col>
            <a-col :span="8">
              <a-statistic title="风险揭示" :value="result.riskScore" :precision="1" suffix="/ 25" />
              <a-progress :percent="(result.riskScore / 25) * 100" :show-info="false" />
            </a-col>
            <a-col :span="8">
              <a-statistic title="客户确认" :value="result.confirmScore" :precision="1" suffix="/ 20" />
              <a-progress :percent="(result.confirmScore / 20) * 100" :show-info="false" />
            </a-col>
            <a-col :span="8">
              <a-statistic title="音视频合规" :value="result.avScore" :precision="1" suffix="/ 15" />
              <a-progress :percent="(result.avScore / 15) * 100" :show-info="false" />
            </a-col>
            <a-col :span="8">
              <a-statistic title="流程完整度" :value="result.flowScore" :precision="1" suffix="/ 10" />
              <a-progress :percent="(result.flowScore / 10) * 100" :show-info="false" />
            </a-col>
            <a-col :span="8">
              <a-statistic title="情感分析" :value="result.sentimentScore || 0" :precision="1" suffix="/ 10" />
              <a-progress :percent="((result.sentimentScore || 0) / 10) * 100" :show-info="false" />
            </a-col>
          </a-row>

          <!-- 问题列表 -->
          <a-divider v-if="result.issues?.length">问题列表</a-divider>
          <a-list
            v-if="result.issues?.length"
            :data-source="result.issues"
            size="small"
          >
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta>
                  <template #title>
                    <a-space>
                      <a-tag :color="getIssueColor(item.level)">{{ item.level }}</a-tag>
                      <a-tag>{{ item.category }}</a-tag>
                      <span v-if="item.nodeCode">节点 {{ item.nodeCode }}</span>
                    </a-space>
                  </template>
                  <template #description>{{ item.message }}</template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>

          <!-- ASR 转写文本(可折叠) -->
          <a-divider v-if="result.asrText">ASR 转写</a-divider>
          <a-collapse v-if="result.asrText" ghost>
            <a-collapse-panel header="查看完整 ASR 转写文本" key="1">
              <pre class="asr-text">{{ result.asrText }}</pre>
            </a-collapse-panel>
          </a-collapse>

          <!-- 复核信息 -->
          <a-divider v-if="result.reviewedAt">人工复核</a-divider>
          <a-descriptions
            v-if="result.reviewedAt"
            :column="2"
            size="small"
            bordered
          >
            <a-descriptions-item label="复核人">ID: {{ result.reviewerId }}</a-descriptions-item>
            <a-descriptions-item label="复核时间">{{ formatTime(result.reviewedAt) }}</a-descriptions-item>
            <a-descriptions-item label="复核意见" :span="2">{{ result.reviewRemark || '(无)' }}</a-descriptions-item>
          </a-descriptions>
        </div>

        <a-empty v-else description="尚未触发质检" />
      </a-card>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { message } from 'ant-design-vue';
import { sessionApi } from '@/api/session';
import type { QualityResult, QAVerdict } from '@/types';

const props = defineProps<{
  sessionId: number;
  qaId?: number;
  autoTrigger?: boolean;
}>();

const emit = defineEmits<{
  (e: 'completed', result: QualityResult): void;
  (e: 'failed', result: QualityResult): void;
}>();

const loading = ref(false);
const triggering = ref(false);
const result = ref<QualityResult | null>(null);
let pollTimer: any = null;

async function onTrigger() {
  triggering.value = true;
  try {
    loading.value = true;
    const qa = await sessionApi.triggerQA(props.sessionId);
    result.value = qa;
    startPolling(qa.qaId);
    handleVerdict(qa);
  } catch (err: any) {
    message.error(`触发失败: ${err.message}`);
  } finally {
    triggering.value = false;
    loading.value = false;
  }
}

function startPolling(qaId: number) {
  if (pollTimer) clearInterval(pollTimer);
  pollTimer = setInterval(async () => {
    try {
      const qa = await sessionApi.getQAResult(qaId);
      result.value = qa;
      if (qa.qaStatus !== 0) {
        if (pollTimer) clearInterval(pollTimer);
        handleVerdict(qa);
      }
    } catch (err) {
      console.warn('轮询失败:', err);
    }
  }, 2000);
}

function handleVerdict(qa: QualityResult) {
  if (qa.verdict === 'HIGH_PASS' || qa.verdict === 'PASS') {
    message.success(`质检通过,分数 ${qa.totalScore.toFixed(1)}`);
    emit('completed', qa);
  } else {
    message.warning(`质检未通过,分数 ${qa.totalScore.toFixed(1)}`);
    emit('failed', qa);
  }
}

async function onRefresh() {
  if (result.value) {
    loading.value = true;
    try {
      result.value = await sessionApi.getQAResult(result.value.qaId);
    } finally {
      loading.value = false;
    }
  }
}

function getVerdictColor(verdict: QAVerdict): string {
  const map: Record<QAVerdict, string> = {
    HIGH_PASS: 'green',
    PASS: 'blue',
    REVIEW: 'orange',
    FAIL: 'red',
  };
  return map[verdict];
}

function getVerdictName(verdict: QAVerdict): string {
  const map: Record<QAVerdict, string> = {
    HIGH_PASS: '高分通过',
    PASS: '通过',
    REVIEW: '需复检',
    FAIL: '未通过',
  };
  return map[verdict];
}

function getScoreColor(score: number): string {
  if (score >= 90) return '#2a9d8f';
  if (score >= 70) return '#2b2d42';
  if (score >= 50) return '#e9c46a';
  return '#d90429';
}

function getIssueColor(level: string): string {
  const map: Record<string, string> = {
    ERROR: 'red',
    WARN: 'orange',
    INFO: 'blue',
  };
  return map[level] || 'default';
}

function formatTime(iso: string): string {
  if (!iso) return '';
  return new Date(iso).toLocaleString('zh-CN');
}

onMounted(() => {
  if (props.autoTrigger) {
    setTimeout(() => onTrigger(), 1000);
  } else if (props.qaId) {
    sessionApi.getQAResult(props.qaId).then((r) => (result.value = r));
  }
});

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer);
});
</script>

<style scoped lang="scss">
.quality-monitor {
  :deep(.ant-card) {
    border-radius: 8px;
  }
}

.total-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 0;

  :deep(.ant-progress) {
    width: 200px;
  }

  .score-label {
    margin-top: 12px;
    color: #8d99ae;
    font-size: 14px;
  }
}

.asr-text {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  max-height: 300px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
  color: #2b2d42;
}
</style>
