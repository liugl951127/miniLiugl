<!--
  @file StatsV2.vue - V6.8.6+ 批量重构版
  @description API Key 统计
-->
<template>
  <PageStandard title="🔑 API Key 统计" subtitle="调用次数 / 限额 / 失败率">
    <template #actions>
      <el-button :icon="Refresh" @click="table.refresh">刷新</el-button>
    </template>

    <StatCardGroup :stats="kpiStats" />

    <section class="section">
      <CrudTable
        :table="table"
        :columns="columns"
        @action="onAction"
      />
    </section>
  </PageStandard>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Refresh, Key } from '@element-plus/icons-vue'
import { useTable } from '@/composables/useTable'
import { usePageSetup } from '@/composables/usePageSetup'
import { useToast } from '@/composables/useToast'
import { apiKeyApi } from '@/api/apikey'

usePageSetup({ title: 'API Key 统计' })
const toast = useToast()

// V6.8.1 fix: /apikey/stats 不存在，改用 /auth/apikeys (AuthController)
// 返回 List<ApiKeyResponse>，包装成 useTable 期望格式
const table = useTable({
  fetcher: async (params) => {
    const r = await apiKeyApi.list()
    return { data: r.data || [], total: (r.data || []).length }
  },
  defaultPageSize: 20,
})

const kpiStats = computed(() => [
  { key: 'total', label: '总数', value: table.total.value, icon: 'Key', color: '#409eff' },
])

const columns = [
  { prop: 'id', label: 'ID', width: 80, type: 'number' },
  { prop: 'name', label: '名称', minWidth: 200, sortable: true },
  { prop: 'createdAt', label: '创建时间', type: 'time', width: 160 },
  { prop: 'actions', label: '操作', type: 'actions', width: 160, fixed: 'right',
    actions: [
      { label: '查看', icon: 'View', event: 'view' },
    ]
  },
]

function onAction({ event, row }) {
  toast.info(`${event}: ${row.name || row.id}`)
}
</script>
