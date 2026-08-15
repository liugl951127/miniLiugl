/**
 * @file usePageSetup.js - V6.8.2+ 页面统一初始化
 *
 * 解决 87 个 view 重复的:
 *   - useUserStore() / useToast() / useConfirm() / useRouter() / useRoute()
 *   - onMounted / onUnmounted / onActivated / onDeactivated
 *   - 业务初始化
 *
 * 用法:
 *   const ctx = usePageSetup({
 *     title: '用户管理',
 *     requires: ['admin'],
 *     setup: (ctx) => {
 *       // 业务初始化
 *     }
 *   })
 *
 * 自动提供:
 *   ctx.userStore / ctx.toast / ctx.confirm / ctx.router / ctx.route
 *   ctx.title / ctx.requires
 *   自动设置 document.title
 *   自动鉴权 (requires 不通过 -> 跳 /403)
 */

import { onMounted, onUnmounted, onActivated, onDeactivated, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useToast } from './useToast'
import { useConfirm } from './useConfirm'

export function usePageSetup(options = {}) {
  const {
    title,           // 页面标题
    requires,        // 需要的角色数组
    setup,           // (ctx) => void  业务初始化回调
    onMount,         // (ctx) => void
    onUnmount,       // (ctx) => void
    onActivate,
    onDeactivate,
  } = options

  const userStore = useUserStore()
  const toast = useToast()
  const confirm = useConfirm()
  const route = useRoute()
  const router = useRouter()

  const ctx = {
    userStore, toast, confirm, route, router,
    title, requires,
  }

  // 自动设置 document.title
  if (title) {
    document.title = `${title} · MiniMax`
  }

  // 自动鉴权
  if (requires?.length) {
    const userRoles = userStore.profile?.roles ?? []
    const ok = requires.some(r => userRoles.includes(r) || userRoles.includes('ADMIN') || userRoles.includes('SUPER_ADMIN'))
    if (!ok) {
      // 异步跳转避免 setup 阶段跳路由报警告
      setTimeout(() => router.replace('/403'), 0)
    }
  }

  onMounted(() => {
    onMount && onMount(ctx)
  })

  onUnmounted(() => {
    onUnmount && onUnmount(ctx)
  })

  onActivated(() => {
    onActivate && onActivate(ctx)
  })

  onDeactivated(() => {
    onDeactivate && onDeactivate(ctx)
  })

  if (setup) {
    setup(ctx)
  }

  return ctx
}
