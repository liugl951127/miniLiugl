<!--
  @file Stats.vue - V6.8.13+ 企业级
  @description API Key 统计 (总览 / 调用 / 失败率 / 趋势)
-->
<template>
  <PageStandard title="🔑 API Key 统计" subtitle="调用次数 / 限额 / 失败率">
    <template #actions>
      <el-button :icon="Refresh" :loading="table.loading.value" @click="table.refresh">刷新</el-button>
    </template>

    <StatCardGroup :stats="kpiStats" />

    <section class="section">
      <h3 class="section-title">调用趋势（近 7 天）</h3>
      <el-skeleton v-if="trendLoading" :rows="4" animated />
      <el-empty
        v-else-if="!trend.length"
        description="暂无趋势数据"
        :image-size="80"
      />
      <div v-else class="trend-bar">
        <div
          v-for="(item, idx) in trend"
          :key="idx"
          class="trend-bar-col"
          :title="`${item.date}: ${item.count} 次`"
        >
          <div
            class="trend-bar-fill"
            :style="{ height: trendHeight(item.count) + '%' }"
          />
          <div class="trend-bar-label">{{ item.date.slice(5) }}</div>
          <div class="trend-bar-value">{{ item.count }}</div>
        </div>
      </div>
    </section>

    <section class="section">
      <h3 class="section-title">Key 明细</h3>
      <CrudTable
        :table="table"
        :columns="columns"
        @action="onAction"
      />
    </section>
  </PageStandard>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { useTable } from '@/composables/useTable'
import { usePageSetup } from '@/composables/usePageSetup'
import { useToast } from '@/composables/useToast'
import { ElMessageBox } from 'element-plus'
import { apiKeyApi } from '@/api/apikey'

usePageSetup({ title: 'API Key 统计' })
const toast = useToast()

// 调用 /auth/apikeys，返回 List<ApiKeyResponse>
const table = useTable({
  fetcher: async (params) => {
    const r = await apiKeyApi.list()
    const list = r.data || []
    return { data: list, total: list.length }
  },
  defaultPageSize: 20,
})

const trendLoading = ref(false)
const trend = ref([])

async function loadTrend() {
  trendLoading.value = true
  try {
    if (apiKeyApi.adminTrend) {
      const r = await apiKeyApi.adminTrend(7)
      trend.value = r.data || []
    } else {
      trend.value = []
    }
  } catch (e) {
    trend.value = []
    toast.error('加载趋势失败：' + (e?.message || ''))
  } finally {
    trendLoading.value = false
  }
}

const maxTrend = computed(() => Math.max(1, ...trend.value.map(t => t.count || 0)))
function trendHeight(count) {
  if (!maxTrend.value) return 0
  return Math.max(2, Math.round((count / maxTrend.value) * 100))
}

const kpiStats = computed(() => {
  const list = table.data.value || []
  const total = list.length
  const enabled = list.filter(k => k.enabled).length
  const used = list.reduce((s, k) => s + (k.used || 0), 0)
  const quota = list.reduce((s, k) => s + (k.quota || 0), 0)
  return [
    { key: 'total', label: '总 Key 数', value: total, icon: 'Key', color: '#409eff' },
    { key: 'enabled', label: '启用中', value: enabled, icon: 'CircleCheck', color: '#67c23a' },
    { key: 'used', label: '总调用量', value: used.toLocaleString(), icon: 'DataLine', color: '#e6a23c' },
    { key: 'quota', label: '总限额', value: quota ? quota.toLocaleString() : '无限', icon: 'Histogram', color: '#909399' },
  ]
})

const columns = [
  { prop: 'id', label: 'ID', width: 80, type: 'number' },
  { prop: 'name', label: '名称', minWidth: 200, sortable: true },
  { prop: 'keyPrefix', label: 'Key 前缀', minWidth: 180 },
  {
    prop: 'enabled',
    label: '状态',
    width: 100,
    align: 'center',
    formatter: (v) => v ? '启用' : '禁用',
    tag: (v) => v ? 'success' : 'info',
  },
  { prop: 'used', label: '已用', width: 100, align: 'right' },
  { prop: 'quota', label: '限额', width: 100, align: 'right' },
  { prop: 'createdAt', label: '创建时间', type: 'time', width: 170 },
  {
    prop: 'actions', label: '操作', type: 'actions', width: 180, fixed: 'right',
    actions: [
      { label: '查看', icon: 'View', event: 'view' },
      { label: '启用/禁用', icon: 'Switch', event: 'toggle' },
      { label: '删除', icon: 'Delete', event: 'remove', type: 'danger' },
    ]
  },
]

async function onAction({ event, row }) {
  try {
    if (event === 'view') {
      toast.info(`查看 Key: ${row.name} (${row.keyPrefix || row.id})`)
    } else if (event === 'toggle') {
      await apiKeyApi.toggle(row.id, !row.enabled)
      toast.success(row.enabled ? '已禁用' : '已启用')
      table.refresh()
    } else if (event === 'remove') {
      await ElMessageBox.confirm(
        `确认删除 Key「${row.name}」？该操作不可恢复。`,
        '警告',
        { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
      )
      await apiKeyApi.remove(row.id)
      toast.success('已删除')
      table.refresh()
    }
  } catch (e) {
    if (e === 'cancel') return
    toast.error('操作失败：' + (e?.message || '请稍后重试'))
  }
}

onMounted(() => {
  table.load()
  loadTrend()
})
</script>

<style lang="scss" scoped>
.section { margin-top: 20px; }
.section-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.trend-bar {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 180px;
  padding: 12px;
  background: #fafafa;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}
.trend-bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  min-width: 0;
}
.trend-bar-fill {
  width: 100%;
  background: linear-gradient(180deg, #409eff 0%, #79bbff 100%);
  border-radius: 4px 4px 0 0;
  margin-top: auto;
  transition: height 0.3s;
  min-height: 2px;
}
.trend-bar-label {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
.trend-bar-value {
  font-size: 11px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
</style>
