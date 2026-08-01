<!--
  @file views/Error.vue (V3.7.9+ 错误页)
  @description 403/404/500 等错误统一展示页
-->
<template>
  <div class="page-error">
    <div class="error-container">
      <ErrorState :errorType="errorType" :title="title" :description="description" @retry="goBack" @home="goHome" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ErrorState from '@/components/ErrorState.vue'

const route = useRoute()
const router = useRouter()

const errorType = computed(() => (route.query.type as string) || 'notfound')
const title = computed(() => {
  const map: Record<string, string> = {
    forbidden: '🔒 权限不足',
    notfound: '🔍 页面不存在',
    server: '💥 服务异常',
    network: '📡 网络错误',
    business: '⚠️ 业务错误',
  }
  return map[errorType.value] || '页面出错了'
})
const description = computed(() => {
  const map: Record<string, string> = {
    forbidden: '您没有权限访问此页面, 请联系管理员',
    notfound: '请求的资源不存在或已被删除',
    server: '服务异常, 请稍后重试',
    network: '网络连接失败, 请检查网络',
    business: '业务处理出错, 请稍后重试',
  }
  return map[errorType.value] || '未知错误'
})

function goBack() { router.back() }
function goHome() { router.push('/admin/dashboard') }

onMounted(() => {
  document.title = `${title.value} - Liugl-AI`
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
</style>
