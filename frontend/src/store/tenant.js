/**
 * V3.1: 多租户 Store
 * adminLiugl (SUPER_ADMIN) 可用
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import tenantApi from '@/api/tenant'

export const useTenantStore = defineStore('tenant', () => {
  const tenants = ref([])
  const loading = ref(false)

  // 加载全部租户
  async function fetchTenants() {
    loading.value = true
    try {
      const res = await tenantApi.listTenants()
      tenants.value = res.data || []
    } finally {
      loading.value = false
    }
  }

  // 创建租户
  async function createTenant(data) {
    const res = await tenantApi.createTenant(data)
    await fetchTenants()
    return res
  }

  // 启/停租户
  async function toggleStatus(id, status) {
    await tenantApi.setTenantStatus(id, status)
    const t = tenants.value.find(t => t.id === id)
    if (t) t.status = status
  }

  // 调整配额
  async function setQuota(id, quota) {
    await tenantApi.updateTenantQuota(id, quota)
    const t = tenants.value.find(t => t.id === id)
    if (t) t.monthlyQuota = quota
  }

  // 删除租户
  async function removeTenant(id) {
    await tenantApi.deleteTenant(id)
    tenants.value = tenants.value.filter(t => t.id !== id)
  }

  // 获取租户用户列表
  async function fetchTenantUsers(id) {
    const res = await tenantApi.listTenantUsers(id)
    return res.data || []
  }

  return {
    tenants,
    loading,
    fetchTenants,
    createTenant,
    toggleStatus,
    setQuota,
    removeTenant,
    fetchTenantUsers,
  }
})
