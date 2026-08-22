<!--
  @file views/Error.vue (V6.8.10+ 友好错误页)
  @description 403/404/500/网络/业务 错误统一展示 + 操作按钮 + 友好提示
-->
<template>
  <div class="page-error">
    <div class="error-container">
      <el-card shadow="hover" class="error-card" :class="`error-card-${errorType}`">
        <div class="error-icon" :style="{ color: color }">{{ icon }}</div>
        <h1 class="error-title">{{ config.title }}</h1>
        <p class="error-desc">{{ description }}</p>

        <!-- 详细信息 (500 / business 错误时显示) -->
        <div v-if="showDetails" class="error-details">
          <el-alert
            :title="errorDetails"
            type="error"
            :closable="false"
            show-icon
          >
            <template #default>
              <div class="details-content">
                <code>{{ errorDetails }}</code>
              </div>
            </template>
          </el-alert>
        </div>

        <p v-if="fromPath" class="error-from">来自: <code>{{ fromPath }}</code></p>

        <!-- 操作建议 -->
        <div class="error-suggestions" v-if="config.suggestions?.length">
          <div class="suggestion-label">💡 建议</div>
          <ul class="suggestion-list">
            <li v-for="(s, i) in config.suggestions" :key="i">{{ s }}</li>
          </ul>
        </div>

        <div class="error-actions">
          <el-button
            v-for="act in config.actions"
            :key="act.action"
            :type="act.type"
            :icon="act.icon"
            size="large"
            @click="handleAction(act.action)"
          >
            {{ act.label }}
          </el-button>
        </div>

        <div class="error-footer">
          <el-text type="info" size="small">
            错误类型: <el-tag size="small" effect="plain" :type="errorTagType">{{ errorTypeLabel }}</el-tag>
            <span v-if="errorCode" class="error-code">· 错误码: <code>{{ errorCode }}</code></span>
            <span v-if="timestamp" class="error-time">· {{ timestamp }}</span>
          </el-text>
        </div>
      </el-card>

      <!-- 调试信息 (开发模式) -->
      <div v-if="showDebug" class="debug-info">
        <el-collapse>
          <el-collapse-item title="🔧 调试信息" name="debug">
            <pre>{{ debugInfo }}</pre>
          </el-collapse-item>
        </el-collapse>
      </div>
    </div>
  </div>
  <BackToTop />
</template>

<script setup lang="ts">
/**
 * V6.8.10+ 升级:
 *  - 明确区分 404/500/403/网络/业务/未授权
 *  - 友好操作建议
 *  - 错误码 + 时间戳记录
 *  - 开发模式调试信息
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, HomeFilled, Refresh, Position, ChatDotRound, Connection } from '@element-plus/icons-vue'
import BackToTop from '@/components/BackToTop.vue'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const errorType = computed(() => (route.query.type as string) || 'notfound')
const fromPath = computed(() => (route.query.from as string) || '')
const errorCode = computed(() => (route.query.code as string) || '')
const errorDetails = computed(() => (route.query.message as string) || '')

// 静态元信息
const appVersion = '6.8.10'
const timestamp = ref('')

// V3.7.10+ 6 类错误细分配置 (V6.8.10+ 增强: 加建议 + 图标)
const ERROR_CONFIGS: Record<string, { icon: string; color: string; title: string; desc: string; suggestions: string[]; actions: any[] }> = {
  // 401 - 未授权
  unauthorized: {
    icon: '🔐', color: '#f59e0b', title: '请先登录',
    desc: '您尚未登录或登录已过期，请重新登录后继续操作',
    suggestions: [
      '点击下方"重新登录"按钮',
      '检查浏览器是否禁用了 Cookie',
      '如有疑问，请联系管理员'
    ],
    actions: [
      { label: '重新登录', type: 'primary', icon: Refresh, action: 'login' },
      { label: '返回首页', type: 'default', icon: HomeFilled, action: 'home' },
    ],
  },
  // 403 - 权限不足
  forbidden: {
    icon: '🔒', color: '#f59e0b', title: '403 - 权限不足',
    desc: '您没有权限访问此页面或资源',
    suggestions: [
      '联系管理员申请对应权限',
      '确认您使用的是正确的账号',
      '如认为这是误判，请提交 Issue'
    ],
    actions: [
      { label: '申请权限', type: 'primary', icon: ChatDotRound, action: 'request' },
      { label: '返回首页', type: 'default', icon: HomeFilled, action: 'home' },
    ],
  },
  // 404 - 页面不存在
  notfound: {
    icon: '🔍', color: '#6b7280', title: '404 - 页面走丢了',
    desc: '抱歉，您访问的页面不存在或已被删除',
    suggestions: [
      '检查 URL 是否输入正确',
      '点击"返回上页"回到上一级',
      '返回首页浏览其他内容'
    ],
    actions: [
      { label: '返回上页', type: 'primary', icon: Back, action: 'back' },
      { label: '返回首页', type: 'default', icon: HomeFilled, action: 'home' },
    ],
  },
  // 408 - 请求超时
  timeout: {
    icon: '⏰', color: '#e6a23c', title: '408 - 请求超时',
    desc: '请求处理时间过长，请稍后重试',
    suggestions: [
      '检查网络连接是否稳定',
      '点击"重试"重新发起请求',
      '如频繁超时，请联系管理员'
    ],
    actions: [
      { label: '重试', type: 'primary', icon: Refresh, action: 'retry' },
      { label: '返回上页', type: 'default', icon: Back, action: 'back' },
    ],
  },
  // 500 - 服务异常
  server: {
    icon: '💥', color: '#ef4444', title: '500 - 服务异常',
    desc: '抱歉，服务器开小差了，请稍后再试',
    suggestions: [
      '点击"重试"刷新页面',
      '稍等几分钟后再次尝试',
      '紧急情况请联系技术支持'
    ],
    actions: [
      { label: '重试', type: 'primary', icon: Refresh, action: 'retry' },
      { label: '联系支持', type: 'default', icon: ChatDotRound, action: 'support' },
      { label: '返回首页', type: 'default', icon: HomeFilled, action: 'home' },
    ],
  },
  // 502/503/504 - 网关错误
  gateway: {
    icon: '🚧', color: '#ef4444', title: '服务暂不可用',
    desc: '后端服务暂时无法响应，请稍后重试',
    suggestions: [
      '服务正在维护或部署中',
      '稍后自动恢复，请耐心等待',
      '紧急情况请联系管理员'
    ],
    actions: [
      { label: '重试', type: 'primary', icon: Refresh, action: 'retry' },
      { label: '返回首页', type: 'default', icon: HomeFilled, action: 'home' },
    ],
  },
  // 网络错误
  network: {
    icon: '📡', color: '#06b6d4', title: '网络不可用',
    desc: '网络连接失败，请检查您的网络设置',
    suggestions: [
      '检查 WiFi 或网线是否正常',
      '刷新页面或重启路由器',
      '若使用 VPN，请尝试关闭后重试'
    ],
    actions: [
      { label: '重新加载', type: 'primary', icon: Refresh, action: 'reload' },
      { label: '检查网络', type: 'default', icon: Connection, action: 'network' },
    ],
  },
  // 业务错误
  business: {
    icon: '⚠️', color: '#f59e0b', title: '业务处理出错',
    desc: '业务逻辑处理失败，请根据下方提示操作',
    suggestions: [
      '查看错误详情了解具体原因',
      '调整操作后重新尝试',
      '如频繁出现，请联系管理员'
    ],
    actions: [
      { label: '返回', type: 'primary', icon: Back, action: 'back' },
      { label: '首页', type: 'default', icon: HomeFilled, action: 'home' },
    ],
  },
  // 未知错误
  unknown: {
    icon: '❓', color: '#9ca3af', title: '未知错误',
    desc: '抱歉，发生了未预期的错误',
    suggestions: [
      '刷新页面重试',
      '清除浏览器缓存后再试',
      '如问题持续，请联系技术支持'
    ],
    actions: [
      { label: '重新加载', type: 'primary', icon: Refresh, action: 'reload' },
      { label: '返回首页', type: 'default', icon: HomeFilled, action: 'home' },
    ],
  },
}

const config = computed(() => ERROR_CONFIGS[errorType.value] || ERROR_CONFIGS.unknown)
const description = computed(() => config.value.desc)
const icon = computed(() => config.value.icon)
const color = computed(() => config.value.color)

const showDetails = computed(() => ['server', 'gateway', 'business'].includes(errorType.value) && !!errorDetails.value)
const errorTypeLabel = computed(() => {
  const map: Record<string, string> = {
    unauthorized: '未授权 (401)',
    forbidden: '禁止访问 (403)',
    notfound: '未找到 (404)',
    timeout: '请求超时 (408)',
    server: '服务异常 (500)',
    gateway: '网关错误 (502/503/504)',
    network: '网络错误',
    business: '业务错误',
    unknown: '未知错误'
  }
  return map[errorType.value] || errorType.value
})
const errorTagType = computed(() => {
  if (['forbidden', 'unauthorized'].includes(errorType.value)) return 'warning'
  if (['server', 'gateway'].includes(errorType.value)) return 'danger'
  if (errorType.value === 'network') return 'info'
  return 'info'
})

// 调试信息 (仅开发模式显示)
const showDebug = ref(import.meta.env?.DEV || false)
const debugInfo = computed(() => {
  return {
    type: errorType.value,
    code: errorCode.value || 'N/A',
    from: fromPath.value || 'N/A',
    message: errorDetails.value || 'N/A',
    url: window.location.href,
    userAgent: navigator.userAgent,
    timestamp: timestamp.value,
    appVersion
  }
})

function handleAction(action: string) {
  switch (action) {
    case 'back':
      if (window.history.length > 1) {
        router.back()
      } else {
        router.push('/admin/dashboard')
      }
      break
    case 'home':
      router.push('/admin/dashboard')
      break
    case 'retry':
    case 'reload':
      window.location.reload()
      break
    case 'login':
      router.push({ path: '/login', query: { redirect: route.fullPath } })
      break
    case 'request':
      toast.info('请联系管理员申请权限 (admin@minimax.io)')
      break
    case 'support':
      toast.info('请加微信: liugl951127 或邮件 admin@minimax.io')
      break
    case 'network':
      toast.info('请检查 WiFi / 网线连接状态')
      break
    default:
      toast.info(`操作: ${action}`)
  }
}

onMounted(() => {
  timestamp.value = new Date().toLocaleString('zh-CN')
  document.title = `${config.value.title} - Liugl-AI v${appVersion}`

  // 根据错误类型显示 toast
  if (['server', 'gateway'].includes(errorType.value)) {
    ElMessage.error(`${config.value.title} - ${config.value.desc}`)
  } else if (errorType.value === 'forbidden') {
    ElMessage.warning(`${config.value.title}`)
  }
})
</script>

<style lang="scss" scoped>
.page-error {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color-light, #f5f7fa);
  padding: 20px;
}

.error-container {
  max-width: 640px;
  width: 100%;
}

.error-card {
  padding: 40px 24px;
  text-align: center;
  border-radius: 16px;
  border: 1px solid var(--el-border-color-lighter, #e2e8f0);
  background: var(--el-bg-color, #fff);
  transition: all 0.3s;
}

.error-card-forbidden,
.error-card-unauthorized { border-top: 4px solid #f59e0b; }
.error-card-notfound { border-top: 4px solid #6b7280; }
.error-card-server,
.error-card-gateway { border-top: 4px solid #ef4444; }
.error-card-network { border-top: 4px solid #06b6d4; }
.error-card-business,
.error-card-timeout { border-top: 4px solid #e6a23c; }
.error-card-unknown { border-top: 4px solid #9ca3af; }

.error-icon {
  font-size: 80px;
  line-height: 1;
  margin-bottom: 16px;
  animation: bounce 2s ease-in-out infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.error-title {
  font-size: 26px;
  font-weight: 700;
  margin: 16px 0 8px;
  color: var(--el-text-color-primary, #1e293b);
}

.error-desc {
  font-size: 15px;
  color: var(--el-text-color-secondary, #64748b);
  margin: 12px 0;
  line-height: 1.6;
}

.error-from {
  font-size: 12px;
  color: var(--el-text-color-placeholder, #94a3b8);
  margin: 8px 0 16px;
}
.error-from code {
  background: var(--el-fill-color-light, #f1f5f9);
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 11px;
}

/* 错误详情 */
.error-details {
  margin: 16px 0;
  text-align: left;
}
.details-content code {
  font-size: 12px;
  word-break: break-all;
}

/* 操作建议 */
.error-suggestions {
  background: var(--el-fill-color-lightest, #f8fafc);
  border-radius: 8px;
  padding: 12px 16px;
  margin: 16px 0;
  text-align: left;
  border: 1px dashed var(--el-border-color-lighter, #e2e8f0);
}
.suggestion-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-regular, #1e293b);
  margin-bottom: 6px;
}
.suggestion-list {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  color: var(--el-text-color-secondary, #64748b);
  line-height: 1.7;
}
.suggestion-list li { margin: 2px 0; }

.error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 24px;
  flex-wrap: wrap;
}

.error-footer {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px dashed var(--el-border-color-lighter, #e2e8f0);
  font-size: 12px;
}
.error-code, .error-time { margin-left: 8px; color: var(--el-text-color-placeholder, #94a3b8); }
.error-code code {
  background: var(--el-fill-color-light, #f1f5f9);
  padding: 1px 4px;
  border-radius: 3px;
  font-family: monospace;
  font-size: 11px;
}

/* 调试信息 */
.debug-info {
  margin-top: 16px;
}
.debug-info pre {
  background: var(--el-fill-color-light, #f1f5f9);
  border-radius: 6px;
  padding: 12px;
  font-size: 12px;
  line-height: 1.6;
  text-align: left;
  overflow-x: auto;
}
</style>
