/**
 * 双录业务全局状态(Pinia)
 */
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type {
  Order,
  Script,
  Session,
  ScriptNode,
  NodeResult,
  RiskAssess,
  Channel,
  ProductType,
  Customer,
  QualityResult,
  ExceptionType,
} from '@/types';

export const useDualRecordStore = defineStore('dualRecord', () => {
  // ============== 状态 ==============
  const customer = ref<Customer | null>(null);
  const order = ref<Order | null>(null);
  const script = ref<Script | null>(null);
  const session = ref<Session | null>(null);
  const riskAssess = ref<RiskAssess | null>(null);
  const qualityResult = ref<QualityResult | null>(null);

  // 当前正在执行的节点
  const currentNodeIndex = ref(0);
  const nodeResults = ref<NodeResult[]>([]);

  // UI 状态
  const isRecording = ref(false);
  const isPaused = ref(false);
  const currentStep = ref<'idle' | 'verify' | 'risk' | 'script' | 'record' | 'sign' | 'qa' | 'done'>('idle');
  const errorMessage = ref<string | null>(null);
  const exceptionType = ref<ExceptionType | null>(null);

  // ============== 计算属性 ==============
  const currentNode = computed<ScriptNode | null>(() => {
    if (!script.value) return null;
    return script.value.nodes[currentNodeIndex.value] || null;
  });

  const progress = computed(() => {
    if (!script.value || script.value.totalNodes === 0) return 0;
    return Math.round((currentNodeIndex.value / script.value.totalNodes) * 100);
  });

  const isLastNode = computed(() => {
    if (!script.value) return false;
    return currentNodeIndex.value >= script.value.nodes.length - 1;
  });

  const canProceed = computed(() => {
    return !!(currentNode.value && currentNode.value.requireConfirm
      ? nodeResults.value[currentNodeIndex.value]?.customerConfirmed
      : nodeResults.value[currentNodeIndex.value]?.result === 'PASS');
  });

  const orderStateName = computed(() => {
    const map: Record<number, string> = {
      [-2]: '已失败', [-1]: '已取消',
      0: '已预约', 1: '已核验', 2: '话术执行中',
      3: '视频录制中', 4: '电子签约', 5: '质检通过', 6: '订单完成',
    };
    return order.value ? (map[order.value.state] || '未知') : '';
  });

  // ============== Actions ==============
  function setCustomer(c: Customer) {
    customer.value = c;
  }

  function setOrder(o: Order) {
    order.value = o;
  }

  function setScript(s: Script) {
    script.value = s;
    currentNodeIndex.value = 0;
    nodeResults.value = [];
  }

  function setSession(s: Session) {
    session.value = s;
  }

  function setRiskAssess(r: RiskAssess) {
    riskAssess.value = r;
  }

  function setQualityResult(q: QualityResult) {
    qualityResult.value = q;
  }

  function setCurrentStep(step: typeof currentStep.value) {
    currentStep.value = step;
  }

  function nextNode() {
    if (script.value && currentNodeIndex.value < script.value.nodes.length - 1) {
      currentNodeIndex.value++;
    }
  }

  function setNodeResult(result: NodeResult) {
    nodeResults.value[currentNodeIndex.value] = result;
  }

  function setRecording(r: boolean) {
    isRecording.value = r;
  }

  function setPaused(p: boolean) {
    isPaused.value = p;
  }

  function setError(msg: string | null, type: ExceptionType | null = null) {
    errorMessage.value = msg;
    exceptionType.value = type;
  }

  function reset() {
    customer.value = null;
    order.value = null;
    script.value = null;
    session.value = null;
    riskAssess.value = null;
    qualityResult.value = null;
    currentNodeIndex.value = 0;
    nodeResults.value = [];
    isRecording.value = false;
    isPaused.value = false;
    currentStep.value = 'idle';
    errorMessage.value = null;
    exceptionType.value = null;
  }

  return {
    // state
    customer, order, script, session, riskAssess, qualityResult,
    currentNodeIndex, nodeResults, isRecording, isPaused,
    currentStep, errorMessage, exceptionType,
    // getters
    currentNode, progress, isLastNode, canProceed, orderStateName,
    // actions
    setCustomer, setOrder, setScript, setSession, setRiskAssess, setQualityResult,
    setCurrentStep, nextNode, setNodeResult,
    setRecording, setPaused, setError, reset,
  };
});
