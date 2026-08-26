/**
 * 字典 API (V8.0.3.1) + Fallback (V8.0.3.2)
 *
 * V8.0.3.1 修致命设计错: common 不是 Spring Boot, 不会注册 controller
 *                路由错: /dict/* 不在 gateway 路由表
 * V8.0.3.2 加 fallback: API 失败时, 用 hardcoded 数组, dropdown 至少不为空
 *                  加 loading 状态: 调用期间显示 loading
 *
 * 用法:
 *   const { data: k8sClusters, loading, error } = useDict('k8sClusters')
 *   <el-option v-for="c in k8sClusters" :key="c.value" :label="c.label" :value="c.value" />
 */
import { ref } from 'vue'
import http from './http'

// V8.0.3.2: hardcoded fallback (API 失败时用)
const FALLBACK = {
  k8sClusters: [
    { value: 'prod-cluster-01', label: 'prod-cluster-01' },
    { value: 'prod-cluster-02', label: 'prod-cluster-02 (新加坡)' },
    { value: 'staging-cluster', label: 'staging-cluster' }
  ],
  agentRoles: [
    { value: '客服', label: '客服', color: 'linear-gradient(135deg, #6366f1, #8b5cf6)' },
    { value: '顾问', label: '顾问', color: 'linear-gradient(135deg, #f59e0b, #ef4444)' },
    { value: '质检', label: '质检', color: 'linear-gradient(135deg, #ec4899, #f43f5e)' },
    { value: '调度', label: '调度', color: 'linear-gradient(135deg, #10b981, #06b6d4)' },
    { value: '专业领域', label: '专业领域', color: 'linear-gradient(135deg, #8b5cf6, #ec4899)' }
  ],
  alertChannels: [
    { value: 'dingtalk', label: '钉钉' },
    { value: 'feishu', label: '飞书' },
    { value: 'wechat_work', label: '企业微信' },
    { value: 'email', label: '邮件' },
    { value: 'webhook', label: 'Webhook' },
    { value: 'sms', label: '短信' }
  ],
  industries: [
    { value: '通用', label: '通用' },
    { value: '法律', label: '法律' },
    { value: '医疗', label: '医疗' },
    { value: '金融', label: '金融' },
    { value: '代码', label: '代码' },
    { value: '教育', label: '教育' },
    { value: '电商', label: '电商' }
  ],
  kbStrategies: [
    { value: 'default', label: '【默认】简洁检索', category: 'retrieval' },
    { value: 'detailed', label: '【详细】带上下文', category: 'retrieval' },
    { value: 'academic', label: '【学术】引用文献', category: 'retrieval' },
    { value: 'multi', label: '【对比】多角度检索', category: 'retrieval' },
    { value: 'auto', label: '自动（默认）', category: 'chunking' },
    { value: 'fixed', label: '固定大小', category: 'chunking' },
    { value: 'semantic', label: '语义切分', category: 'chunking' }
  ],
  models: [
    { value: 'gpt-4o', label: 'GPT-4o' },
    { value: 'claude-3.5-sonnet', label: 'Claude-3.5' },
    { value: 'deepseek-chat', label: 'DeepSeek' },
    { value: 'qwen2.5-72b-instruct', label: 'Qwen2.5-72B' },
    { value: 'qwen2.5-0.5b-instruct', label: 'Qwen2.5-0.5B (本地)' }
  ]
}

const API_PATHS = {
  k8sClusters: '/system/dict/k8s-clusters',
  agentRoles: '/system/dict/agent-roles',
  alertChannels: '/system/dict/alert-channels',
  industries: '/system/dict/industries',
  kbStrategies: '/system/dict/kb-strategies',
  models: '/system/dict/models'
}

/**
 * useDict 组合式 API (V8.0.3.2)
 *
 * @param key - 字典 key (k8sClusters/agentRoles/...)
 * @returns { data: Ref<Array>, loading: Ref<boolean>, error: Ref<string|null> }
 *
 * - 立即返回 fallback 数据, 不阻塞 UI
 * - 后台拉 API, 成功后覆盖 fallback
 * - API 失败时保留 fallback + 设置 error
 */
export function useDict(key) {
  const data = ref([...(FALLBACK[key] || [])])
  const loading = ref(false)
  const error = ref(null)
  const isFallback = ref(true)

  if (!API_PATHS[key]) {
    console.warn(`[dict] 未知 key: ${key}`)
    return { data, loading, error, isFallback }
  }

  loading.value = true
  http.get(API_PATHS[key])
    .then(r => {
      const list = r.data?.data || r.data || []
      if (Array.isArray(list) && list.length > 0) {
        data.value = list
        isFallback.value = false
      } else {
        // 后端返了空, 保留 fallback
        error.value = '后端返空数据, 使用 fallback'
      }
    })
    .catch(e => {
      error.value = e?.message || 'API 调用失败, 使用 fallback'
      console.warn(`[dict] ${key} 失败, 用 fallback:`, e?.message)
    })
    .finally(() => {
      loading.value = false
    })

  return { data, loading, error, isFallback }
}
