/**
 * V6.3+ 表单智能助手 composable
 * 包装 autofill API, 提供响应式状态管理
 * 
 * 用法:
 *   const { assist, applying, lastResult, error, applyTo } = useFormAssist('user')
 *   await assist({ username: 'admin' })  // 拿 AI 推荐
 *   applyTo(formRef)  // 应用到表单
 */
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { autofill, previewForm, recommendField } from '@/api/autofill'

export function useFormAssist(formType) {
  const loading = ref(false)
  const applying = ref(false)
  const lastResult = ref(null)
  const error = ref(null)
  const confidence = ref(0)

  /**
   * 智能填单
   * @param {object} context 已有数据
   * @returns {Promise<object|null>} 推荐字段值
   */
  async function assist(context = {}) {
    loading.value = true
    error.value = null
    try {
      const res = await autofill(formType, context)
      if (res?.data && !res.__notFound) {
        lastResult.value = res.data
        confidence.value = res.data._confidence || 0
        return res.data
      }
      return null
    } catch (e) {
      error.value = e
      console.warn('[useFormAssist] assist 失败:', e.message)
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * 一键预览示例
   */
  async function preview() {
    loading.value = true
    error.value = null
    try {
      const res = await previewForm(formType)
      if (res?.data) {
        lastResult.value = res.data
        confidence.value = 1.0
        return res.data
      }
      return null
    } catch (e) {
      error.value = e
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * 应用推荐到表单对象
   * @param {object} target 目标表单 ref
   * @param {object} source 推荐数据 (默认用 lastResult)
   */
  function applyTo(target, source) {
    const data = source || lastResult.value
    if (!data) {
      ElMessage.warning('没有可应用的推荐')
      return false
    }
    applying.value = true
    try {
      // 过滤掉元数据字段 (_xxx)
      for (const [k, v] of Object.entries(data)) {
        if (k.startsWith('_')) continue
        if (target && typeof target === 'object') {
          if (k in target) {
            target[k] = v
          }
        }
      }
      ElMessage.success(`已应用 ${Object.keys(data).filter(k => !k.startsWith('_')).length} 个推荐字段`)
      return true
    } finally {
      applying.value = false
    }
  }

  /**
   * 字段推荐
   */
  async function recommend(field) {
    try {
      const res = await recommendField(formType, field)
      if (res?.data) return res.data
    } catch (e) {
      console.warn('[useFormAssist] recommend 失败:', e.message)
    }
    return []
  }

  return {
    loading,
    applying,
    lastResult,
    error,
    confidence,
    assist,
    preview,
    applyTo,
    recommend
  }
}

export default useFormAssist
