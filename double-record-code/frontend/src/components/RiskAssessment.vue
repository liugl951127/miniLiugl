<!--
  风险评估问卷组件
  支持单选/多选/文本题,实时评分与等级匹配
-->
<template>
  <div class="risk-assessment">
    <a-alert
      :message="`风险评估问卷 - ${questionnaire?.title || '加载中...'}`"
      type="info"
      show-icon
      style="margin-bottom: 16px;"
    />

    <div v-if="loading" class="loading">
      <a-spin tip="加载问卷中..." />
    </div>

    <div v-else-if="questionnaire" class="questionnaire-content">
      <a-steps
        :current="currentIndex"
        size="small"
        :items="questionnaire.questions.map((q: any) => ({ title: q.qid }))"
        style="margin-bottom: 24px;"
      />

      <a-card
        :title="`${questionnaire.questions[currentIndex].qid}. ${questionnaire.questions[currentIndex].text}`"
        :bordered="false"
        class="question-card"
      >
        <template #extra>
          <a-tag color="blue">权重: {{ questionnaire.questions[currentIndex].weight }}</a-tag>
        </template>

        <!-- 单选题 -->
        <a-radio-group
          v-if="questionnaire.questions[currentIndex].type === 'single'"
          v-model:value="answers[questionnaire.questions[currentIndex].qid]"
          style="display: flex; flex-direction: column; gap: 12px;"
        >
          <a-radio
            v-for="(opt, idx) in questionnaire.questions[currentIndex].options"
            :key="idx"
            :value="opt"
            style="font-size: 16px;"
          >
            <span class="option-label">{{ String.fromCharCode(65 + idx) }}.</span>
            {{ opt }}
          </a-radio>
        </a-radio-group>

        <!-- 多选题 -->
        <a-checkbox-group
          v-else-if="questionnaire.questions[currentIndex].type === 'multi'"
          v-model:value="answers[questionnaire.questions[currentIndex].qid]"
          style="display: flex; flex-direction: column; gap: 12px;"
        >
          <a-checkbox
            v-for="(opt, idx) in questionnaire.questions[currentIndex].options"
            :key="idx"
            :value="opt"
            style="font-size: 16px;"
          >
            <span class="option-label">{{ String.fromCharCode(65 + idx) }}.</span>
            {{ opt }}
          </a-checkbox>
        </a-checkbox-group>
      </a-card>

      <!-- 导航 -->
      <div class="nav-buttons">
        <a-button
          :disabled="currentIndex === 0"
          @click="prevQuestion"
        >
          <ArrowLeftOutlined /> 上一题
        </a-button>

        <span class="progress-text">
          {{ currentIndex + 1 }} / {{ questionnaire.questions.length }}
        </span>

        <a-button
          v-if="currentIndex < questionnaire.questions.length - 1"
          type="primary"
          :disabled="!isCurrentAnswered"
          @click="nextQuestion"
        >
          下一题 <ArrowRightOutlined />
        </a-button>

        <a-button
          v-else
          type="primary"
          :loading="submitting"
          :disabled="!allAnswered"
          @click="onSubmit"
        >
          提交评估
        </a-button>
      </div>

      <!-- 实时评分预览 -->
      <a-card title="评估预览" size="small" style="margin-top: 16px;">
        <div class="score-preview">
          <div class="score-item">
            <span>已答题数</span>
            <span class="value">{{ answeredCount }} / {{ questionnaire.questions.length }}</span>
          </div>
          <div class="score-item">
            <span>预估总分</span>
            <span class="value">{{ previewScore.toFixed(1) }}</span>
          </div>
          <div class="score-item">
            <span>预估等级</span>
            <a-tag :color="getLevelColor(previewLevel)" style="font-size: 14px;">
              {{ previewLevel }} · {{ getLevelName(previewLevel) }}
            </a-tag>
          </div>
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue';
import { message } from 'ant-design-vue';
import { ArrowLeftOutlined, ArrowRightOutlined } from '@ant-design/icons-vue';
import { riskApi } from '@/api/risk';
import { useDualRecordStore } from '@/store/dualRecord';
import type { RiskAssess, RiskLevel } from '@/types';

const props = defineProps<{
  orderId: number;
  sessionId: number;
}>();

const emit = defineEmits<{
  (e: 'completed', assess: RiskAssess): void;
}>();

const store = useDualRecordStore();

const loading = ref(true);
const submitting = ref(false);
const currentIndex = ref(0);
const questionnaire = ref<any>(null);
const answers = reactive<Record<string, string | string[]>>({});

const isCurrentAnswered = computed(() => {
  if (!questionnaire.value) return false;
  const qid = questionnaire.value.questions[currentIndex.value].qid;
  const ans = answers[qid];
  if (Array.isArray(ans)) return ans.length > 0;
  return !!ans;
});

const allAnswered = computed(() => {
  if (!questionnaire.value) return false;
  return questionnaire.value.questions.every((q: any) => {
    const ans = answers[q.qid];
    if (Array.isArray(ans)) return ans.length > 0;
    return !!ans;
  });
});

const answeredCount = computed(() => {
  if (!questionnaire.value) return 0;
  return questionnaire.value.questions.filter((q: any) => {
    const ans = answers[q.qid];
    if (Array.isArray(ans)) return ans.length > 0;
    return !!ans;
  }).length;
});

const previewScore = computed(() => {
  if (!questionnaire.value) return 0;
  let total = 0;
  let max = 0;
  for (const q of questionnaire.value.questions) {
    const ans = answers[q.qid];
    const score = calcQuestionScore(q, ans);
    total += score;
    max += q.weight * 20;  // 假设每题权重 * 20 为满分
  }
  // 归一化到 100
  return max > 0 ? (total / max) * 100 : 0;
});

const previewLevel = computed<RiskLevel>(() => {
  const s = previewScore.value;
  if (s >= 81) return 'C5';
  if (s >= 61) return 'C4';
  if (s >= 41) return 'C3';
  if (s >= 21) return 'C2';
  return 'C1';
});

function calcQuestionScore(q: any, ans: string | string[] | undefined): number {
  if (!ans) return 0;
  // 简化的评分逻辑:基于选项位置
  const opts = q.options;
  if (!opts) return 0;

  if (q.type === 'single' && typeof ans === 'string') {
    const idx = opts.indexOf(ans);
    if (idx === -1) return 0;
    // 越靠后(越激进)分数越高
    return ((idx + 1) / opts.length) * q.weight * 20;
  }

  if (q.type === 'multi' && Array.isArray(ans)) {
    if (ans.length === 0) return 0;
    let sum = 0;
    for (const a of ans) {
      const idx = opts.indexOf(a);
      if (idx >= 0) sum += (idx + 1) / opts.length;
    }
    return (sum / ans.length) * q.weight * 20;
  }

  return 0;
}

function getLevelColor(level: RiskLevel): string {
  const map: Record<RiskLevel, string> = {
    C1: 'green',
    C2: 'cyan',
    C3: 'blue',
    C4: 'orange',
    C5: 'red',
  };
  return map[level];
}

function getLevelName(level: RiskLevel): string {
  const map: Record<RiskLevel, string> = {
    C1: '保守型',
    C2: '稳健型',
    C3: '平衡型',
    C4: '成长型',
    C5: '进取型',
  };
  return map[level];
}

function prevQuestion() {
  if (currentIndex.value > 0) currentIndex.value--;
}

function nextQuestion() {
  if (currentIndex.value < (questionnaire.value?.questions.length || 0) - 1) {
    currentIndex.value++;
  }
}

async function onSubmit() {
  if (!questionnaire.value) return;
  submitting.value = true;
  try {
    const assess = await riskApi.submit({
      orderId: props.orderId,
      sessionId: props.sessionId,
      answers: { ...answers },
    });
    store.setRiskAssess(assess);
    message.success(`评估完成,您的风险等级为 ${assess.riskLevel} (${getLevelName(assess.riskLevel)})`);
    emit('completed', assess);
  } catch (err: any) {
    message.error(`提交失败: ${err.message}`);
  } finally {
    submitting.value = false;
  }
}

onMounted(async () => {
  try {
    questionnaire.value = await riskApi.getQuestionnaire();
  } catch (err: any) {
    message.error(`加载问卷失败: ${err.message}`);
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped lang="scss">
.risk-assessment {
  padding: 0;
}

.loading {
  text-align: center;
  padding: 60px 0;
}

.question-card {
  margin-bottom: 16px;
  min-height: 280px;
}

.option-label {
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  background: #edf2f4;
  border-radius: 50%;
  margin-right: 8px;
  font-weight: 600;
  color: #2b2d42;
}

.nav-buttons {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 24px 0;
  padding: 16px 24px;
  background: #fff;
  border-radius: 8px;

  .progress-text {
    color: #8d99ae;
    font-size: 14px;
  }
}

.score-preview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;

  .score-item {
    text-align: center;
    padding: 12px;
    background: #f8f9fa;
    border-radius: 6px;

    span:first-child {
      display: block;
      color: #8d99ae;
      font-size: 12px;
      margin-bottom: 6px;
    }
    .value {
      color: #2b2d42;
      font-size: 20px;
      font-weight: 600;
    }
  }
}
</style>
