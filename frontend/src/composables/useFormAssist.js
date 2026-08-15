/**
 * @file useFormAssist.js - V6.3+ 智能填单 composable (含在线学习)
 *
 * 暴露:
 * - assist(formType, context) - 智能填单
 * - preview(formType) - 预览
 * - recommend(formType, field) - 字段推荐
 * - accept(formType, formId, context) - 接受 (强化)
 * - correct(formType, formId, context, newIntent) - 修正
 * - reject(formType, formId, context) - 拒绝
 * - stats() - 学习统计
 * - loading / applying / lastResult / error / confidence
 */
import { ref, computed } from 'vue'
import { autofill, previewForm, recommendField, feedback, getStats } from '@/api/autofill'

export function useFormAssist() {
  const loading = ref(false)
  const applying = ref(false)
  const lastResult = ref(null)
  const lastError = ref(null)
  const confidence = computed(() => lastResult.value?.confidence || 0)
  const modelWeights = computed(() => lastResult.value?.modelWeights || {})

  /** 智能填单 */
  async function assist(formType, context = {}) {
    loading.value = true
    lastError.value = null
    try {
      const resp = await autofill(formType, context)
      lastResult.value = resp.data?.data || resp
      return lastResult.value
    } catch (e) {
      lastError.value = e
      throw e
    } finally {
      loading.value = false
    }
  }

  /** 预览 */
  async function preview(formType) {
    const resp = await previewForm(formType)
    return resp.data?.data || resp
  }

  /** 字段推荐 */
  async function recommend(formType, field) {
    const resp = await recommendField(formType, field)
    return resp.data?.data || resp
  }

  /** 接受推荐 (在线学习: 强化当前权重) */
  async function accept(formType, formId, context) {
    applying.value = true
    try {
      return await feedback(formType, 'accept', { formId, context })
    } finally {
      applying.value = false
    }
  }

  /** 修正 (在线学习: 弱化错, 强化对) */
  async function correct(formType, formId, context, correctedIntent) {
    applying.value = true
    try {
      return await feedback(formType, 'correct', { formId, context, correctedIntent })
    } finally {
      applying.value = false
    }
  }

  /** 拒绝 (在线学习: 弱化) */
  async function reject(formType, formId, context) {
    applying.value = true
    try {
      return await feedback(formType, 'reject', { formId, context })
    } finally {
      applying.value = false
    }
  }

  /** 学习统计 */
  async function stats() {
    const resp = await getStats()
    return resp.data?.data || resp
  }

  return {
    assist, preview, recommend,
    accept, correct, reject, stats,
    loading, applying, lastResult, lastError,
    confidence, modelWeights
  }
}

export default useFormAssist
