/**
 * @file prompt.js - Pinia 状态管理 (V3.5.12+)
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { promptApi } from '@/api/prompt'

/**
 * Prompt 模板 Store.
 * 管理模板列表、分类、选中模板，以及变量填值状态。
 */
export const usePromptStore = defineStore('prompt', () => {
  const templates = ref([])
  const categories = ref([])
  const selectedTemplate = ref(null)
  const loading = ref(false)

  /** 加载模板列表 */
  async function fetchList(params = {}) {
    loading.value = true
    try {
      const res = await promptApi.list(params)
      templates.value = res.records || res.data?.records || []
    } finally {
      loading.value = false
    }
  }

  /** 加载分类 */
  async function fetchCategories() {
    const res = await promptApi.categories()
    categories.value = res.data || res || []
  }

  /** 选择模板 */
  function select(tpl) {
    selectedTemplate.value = tpl
  }

  /** 取消选择 */
  function clearSelection() {
    selectedTemplate.value = null
  }

  /** 使用计数 +1 */
  async function markUsed(id) {
    await promptApi.use(id)
  }

  return { templates, categories, selectedTemplate, loading, fetchList, fetchCategories, select, clearSelection, markUsed }
})
