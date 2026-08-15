/**
 * @file useConfirm.js - V6.8.2+ 通用确认对话框
 *
 * 替代 87 个 view 散落的 ElMessageBox.confirm
 * 统一确认弹窗的标题/内容/类型/回调
 *
 * 用法:
 *   const confirm = useConfirm()
 *   const ok = await confirm({ title: '删除?', message: '不可恢复', type: 'warning' })
 *   if (ok) { ... }
 */

import { ElMessageBox } from 'element-plus'

export function useConfirm() {
  /**
   * 弹出确认框, 返回 Promise<boolean>
   * @param {Object} options
   * @param {string} options.title
   * @param {string} options.message
   * @param {string} options.type 'success' | 'warning' | 'info' | 'error'
   * @param {string} options.confirmText 默认 '确定'
   * @param {string} options.cancelText  默认 '取消'
   */
  return async function confirm(options = {}) {
    const {
      title = '确认',
      message = '确定要继续吗?',
      type = 'warning',
      confirmText = '确定',
      cancelText = '取消',
    } = options

    try {
      await ElMessageBox.confirm(message, title, {
        confirmButtonText: confirmText,
        cancelButtonText: cancelText,
        type,
        draggable: true,
        closeOnClickModal: false,
      })
      return true
    } catch (e) {
      // ElMessageBox 取消时 throw 'cancel'
      return false
    }
  }
}
