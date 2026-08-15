<template>
  <div class="data-table">
    <div v-if="$slots.toolbar" class="data-table-toolbar">
      <slot name="toolbar" />
    </div>
    
    <el-table
      v-loading="loading"
      :data="paginatedData"
      :row-key="rowKey"
      :empty-text="emptyText"
      :size="size"
      :stripe="stripe"
      :border="border"
      :height="height"
      :max-height="maxHeight"
      :default-sort="defaultSort"
      @selection-change="onSelectionChange"
      @sort-change="onSortChange"
    >
      <slot />
    </el-table>
    
    <div v-if="showPagination && total > 0" class="data-table-pagination">
      <el-pagination
        v-model:current-page="_currentPage"
        v-model:page-size="_pageSize"
        :page-sizes="pageSizes"
        :total="total"
        :layout="layout"
        :background="true"
        @size-change="onSizeChange"
        @current-change="onPageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  total: { type: Number, default: 0 },
  pageSize: { type: Number, default: 20 },
  currentPage: { type: Number, default: 1 },
  pageSizes: { type: Array, default: () => [10, 20, 50, 100] },
  showPagination: { type: Boolean, default: true },
  layout: { type: String, default: 'total, sizes, prev, pager, next, jumper' },
  size: { type: String, default: 'default' },
  stripe: { type: Boolean, default: true },
  border: { type: Boolean, default: false },
  height: { type: [String, Number], default: undefined },
  maxHeight: { type: [String, Number], default: undefined },
  rowKey: { type: String, default: 'id' },
  defaultSort: { type: Object, default: () => ({}) },
  emptyText: { type: String, default: '暂无数据' },
  clientSide: { type: Boolean, default: false }
})

const emit = defineEmits(['selection-change', 'sort-change', 'page-change', 'size-change', 'update:currentPage', 'update:pageSize'])

// 用 computed 包装, 避免直接修改 props
const _currentPage = computed({
  get: () => props.currentPage,
  set: v => emit('update:currentPage', v)
})
const _pageSize = computed({
  get: () => props.pageSize,
  set: v => emit('update:pageSize', v)
})

const paginatedData = computed(() => {
  if (!props.clientSide) return props.data
  const start = (_currentPage.value - 1) * _pageSize.value
  return props.data.slice(start, start + _pageSize.value)
})

function onSelectionChange(rows) { emit('selection-change', rows) }
function onSortChange(sort) { emit('sort-change', sort) }
function onSizeChange(s) { emit('update:pageSize', s); emit('size-change', s) }
function onPageChange(p) { emit('update:currentPage', p); emit('page-change', p) }
</script>

<style lang="scss" scoped>
.data-table {
  background: var(--el-bg-color);
  border-radius: 8px;
  overflow: hidden;
}

.data-table-toolbar {
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
}

.data-table-pagination {
  padding: 16px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
