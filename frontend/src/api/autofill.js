/**
 * @file autofill.js - V6.3+ AI 智能填单 API
 *
 * 支持:
 * - autofill(formType, context): 智能填单 (LLM + 在线学习)
 * - previewForm(formType): 一键预览
 * - recommendField(formType, field): 字段推荐
 * - feedback(formType, feedback, ...): 用户反馈 (新增)
 * - getStats(): 学习统计 (新增)
 */
import http from './http'

/** 智能填单 */
export const autofill = (formType, context = {}) => {
  console.log('%c[AUTOFILL]', 'color: #a855f7', { formType, context })
  return http.post('/ai/autofill', { formType, context })
}

/** 一键预览 */
export const previewForm = (formType) => {
  console.log('%c[AUTOFILL PREVIEW]', 'color: #a855f7', formType)
  return http.get(`/ai/autofill/preview/${formType}`)
}

/** 字段推荐 */
export const recommendField = (formType, field) => {
  return http.get(`/ai/autofill/recommend/${formType}/${field}`)
}

/** V6.3+ 用户反馈 (在线学习) */
export const feedback = (formType, feedback, options = {}) => {
  console.log('%c[AUTOFILL FEEDBACK]', 'color: #a855f7', { formType, feedback, options })
  return http.post('/ai/autofill/feedback', {
    formType,
    formId: options.formId,
    context: JSON.stringify(options.context || {}),
    feedback,
    correctedIntent: options.correctedIntent
  })
}

/** V6.3+ 学习统计 */
export const getStats = () => {
  return http.get('/ai/autofill/stats')
}

export default { autofill, previewForm, recommendField, feedback, getStats }
