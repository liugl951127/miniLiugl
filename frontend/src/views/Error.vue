<!--
  @file views/Error.vue (V3.7.10+ 5 类错误细分页)
  @description 403/404/500/网络/业务 错误统一展示 + 操作按钮
-->
<template>
  <div class="page-error">
    <div class="error-container">
      <el-card shadow="hover" class="error-card">
        <div class="error-icon" :style="{ color: color }">{{ icon }}</div>
        <h1 class="error-title">{{ config.title }}</h1>
        <p class="error-desc">{{ description }}</p>
        <p v-if="fromPath" class="error-from">来自: <code>{{ fromPath }}</code></p>
        <div class="error-actions">
          <el-button
            v-for="act in config.actions"
            :key="act.action"
            :type="act.type"
            size="large"
            @click="handleAction(act.action)"
          >
            {{ act.label }}
          </el-button>
        </div>
        <div class="error-footer">
          <el-text type="info" size="small">
            错误类型: <el-tag size="small" effect="plain">{{ errorType }}</el-tag>
          </el-text>
        </div>
      </el-card>
    </div>
  </div>
  <BackToTop />
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import BackToTop from '@/components/BackToTop.vue'

const route = useRoute()
const router = useRouter()

const errorType = computed(() => (route.query.type as string) || 'notfound')
const fromPath = computed(() => (route.query.from as string) || '')

// V3.7.10+ 5 类错误细分配置
const ERROR_CONFIGS: Record<string, { icon: string; color: string; title: string; desc: string; actions: any[] }> = {
  forbidden: {
    icon: '🔒', color: '#f59e0b', title: '权限不足',
    desc: '您没有权限访问此页面, 请联系管理员申请',
    actions: [
      { label: '申请权限', type: 'primary', action: 'request' },
      { label: '返回首页', type: 'default', action: 'home' },
    ],
  },
  notfound: {
    icon: '🔍', color: '#6b7280', title: '页面走丢了',
    desc: '请求的资源不存在或已被删除',
    actions: [
      { label: '返回上页', type: 'primary', action: 'back' },
      { label: '返回首页', type: 'default', action: 'home' },
    ],
  },
  server: {
    icon: '💥', color: '#ef4444', title: '服务异常',
    desc: '后端服务异常, 请稍后重试',
    actions: [
      { label: '重试', type: 'primary', action: 'retry' },
      { label: '联系支持', type: 'default', action: 'support' },
    ],
  },
  network: {
    icon: '📡', color: '#06b6d4', title: '网络不可用',
    desc: '网络连接失败, 请检查 WiFi 或网线',
    actions: [
      { label: '重新加载', type: 'primary', action: 'reload' },
      { label: '检查网络', type: 'default', action: 'network' },
    ],
  },
  business: {
    icon: '⚠️', color: '#f59e0b', title: '业务错误',
    desc: '业务处理出错, 请稍后重试',
    actions: [
      { label: '返回', type: 'primary', action: 'back' },
      { label: '首页', type: 'default', action: 'home' },
    ],
  },
  unknown: {
    icon: '❓', color: '#9ca3af', title: '未知错误',
    desc: '请稍后重试或联系支持',
    actions: [{ label: '返回首页', type: 'primary', action: 'home' }],
  },
}

const config = computed(() => ERROR_CONFIGS[errorType.value] || ERROR_CONFIGS.unknown)
const description = computed(() => config.value.desc)
const icon = computed(() => config.value.icon)
const color = computed(() => config.value.color)

function handleAction(action: string) {
  if (action === 'back') router.back()
  else if (action === 'home') router.push('/admin/dashboard')
  else if (action === 'retry' || action === 'reload') window.location.reload()
  else if (action === 'request') ElMessage.info('请联系管理员申请权限')
  else if (action === 'support') ElMessage.info('请加微信: liugl951127')
  else if (action === 'network') ElMessage.info('请检查 WiFi / 网线连接')
}

onMounted(() => {
  document.title = `${config.value.title} - Liugl-AI`
})
</script>

<style lang="scss" scoped>
.page-error {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color-light);
  padding: 20px;
}
.error-container { max-width: 600px; width: 100%; }
.error-card { padding: 40px 24px; text-align: center; }
.error-icon { font-size: 80px; line-height: 1; margin-bottom: 16px; }
.error-title { font-size: 28px; font-weight: 600; margin: 16px 0 8px; color: var(--el-text-color-primary); }
.error-desc { font-size: 15px; color: var(--el-text-color-secondary); margin: 12px 0; line-height: 1.6; }
.error-from { font-size: 12px; color: var(--el-text-color-placeholder); margin: 8px 0; }
.error-from code { background: var(--el-fill-color-light); padding: 2px 6px; border-radius: 3px; font-size: 11px; }
.error-actions { display: flex; gap: 12px; justify-content: center; margin-top: 24px; flex-wrap: wrap; }
.error-footer { margin-top: 24px; padding-top: 16px; border-top: 1px dashed var(--el-border-color-lighter); }
</style>
