<!--
  @file WechatScanPageV2.vue - V6.8.6+ 批量重构版
  @description 微信扫码
-->
<template>
  <PageStandard title="📱 微信扫码" subtitle="扫码登录 / 公众号绑定 / UnionID 识别">
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
import { Refresh, ChatDotRound } from '@element-plus/icons-vue'
import { useTable } from '@/composables/useTable'
import { usePageSetup } from '@/composables/usePageSetup'
import { useToast } from '@/composables/useToast'
import http from '@/api/http'

usePageSetup({ title: '微信扫码' })
const toast = useToast()

const table = useTable({
  fetcher: async (params) => await http.get('/auth/wechat-scan', { params }),
  defaultPageSize: 20,
})

const kpiStats = computed(() => [
  { key: 'total', label: '总数', value: table.total.value, icon: 'ChatDotRound', color: '#409eff' },
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
