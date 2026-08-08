/**
 * V6.3+ AI 智能填单 API
 * 
 * 支持:
 * - autofill(formType, context): 智能填单
 * - preview(formType): 一键预览示例
 * - recommend(formType, field): 字段推荐 Top-3
 */
import http from './http'

/** 智能填单: 根据 formType + context 推荐字段值 */
export const autofill = (formType, context = {}) => {
  console.log('%c[AUTOFILL]', 'color: #a855f7', { formType, context })
  return http.post('/ai/autofill', { formType, context })
}

/** 一键预览: 给出完整示例数据 */
export const previewForm = (formType) => {
  console.log('%c[AUTOFILL PREVIEW]', 'color: #a855f7', formType)
  return http.get(`/ai/autofill/preview/${formType}`)
}

/** 字段推荐: Top-3 推荐值 */
export const recommendField = (formType, field) => {
  return http.get(`/ai/autofill/recommend/${formType}/${field}`)
}

export default { autofill, previewForm, recommendField }
