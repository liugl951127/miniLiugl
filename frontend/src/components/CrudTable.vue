<!--
  @file CrudTable.vue - V6.8.2+ 通用 CRUD 表格
  @description 统一 87 view 的 el-table + 分页 + 操作列 + 选择列
    - 集成 useTable 的所有状态
    - 自动加载/刷新/分页
    - 操作列插槽 (编辑/删除/查看/自定义)
    - 选择列 + 批量操作插槽
    - 列配置化 (columns prop)

  替代 87 view 重复的 100+ 行表格 + 分页 + 操作列代码

  用法:
    <CrudTable
      :table="table"
      :columns="columns"
      @edit="crud.openEdit"
      @delete="crud.doDelete"
    >
      <template #batch-actions="{ selection }">
        <el-button type="danger" @click="crud.doBatchDelete(selection)">批量删除</el-button>
      </template>
    </CrudTable>

  columns 格式:
    [
      { prop: 'name', label: '名称', width: 200, sortable: true },
      { prop: 'status', label: '状态', type: 'tag',
        tags: { active: 'success', disabled: 'info' } },
      { prop: 'createdAt', label: '创建时间', type: 'time', width: 180 },
      { prop: 'amount', label: '金额', type: 'number', align: 'right' },
      { prop: 'actions', label: '操作', type: 'actions', width: 220,
        actions: [
          { label: '查看', icon: 'View', type: 'primary', event: 'view' },
          { label: '编辑', icon: 'Edit', type: 'primary', event: 'edit' },
          { label: '删除', icon: 'Delete', type: 'danger', event: 'delete', confirm: true },
        ]
      }
    ]
-->
<template>
  <div class="crud-table">
    <el-table
      v-loading="table.loading"
      :data="table.data"
      :row-key="rowKey"
      :empty-text="emptyText"
      :size="size"
      :stripe="stripe"
      :border="border"
      :height="height"
      :max-height="maxHeight"
      :default-sort="defaultSort"
      @selection-change="table.onSelectionChange"
      @sort-change="table.onSortChange"
    >
      <!-- 选择列 -->
      <el-table-column v-if="selectable" type="selection" :width="50" :selectable="selectable" />

      <!-- 列配置渲染 -->
      <template v-for="col in columns" :key="col.prop">
        <el-table-column
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :min-width="col.minWidth"
          :align="col.align || 'left'"
          :header-align="col.headerAlign || 'left'"
          :sortable="col.sortable"
          :show-overflow-tooltip="col.overflow !== false"
          :fixed="col.fixed"
        >
          <template v-if="col.type === 'index'" #default="{ $index }">
            {{ (table.page.current - 1) * table.page.size + $index + 1 }}
          </template>

          <template v-else-if="col.type === 'tag'" #default="{ row }">
            <el-tag
              :type="col.tags?.[row[col.prop]] || 'info'"
              :size="col.size || 'small'"
              :effect="col.effect || 'light'"
            >
              {{ col.formatter ? col.formatter(row[col.prop], row) : (row[col.prop] ?? '-') }}
            </el-tag>
          </template>

          <template v-else-if="col.type === 'time'" #default="{ row }">
            <TimeAgo :time="row[col.prop]" :format="col.format || 'YYYY-MM-DD HH:mm'" />
          </template>

          <template v-else-if="col.type === 'date'" #default="{ row }">
            {{ formatDate(row[col.prop], col.format) }}
          </template>

          <template v-else-if="col.type === 'number'" #default="{ row }">
            <span :style="{ color: col.color?.(row[col.prop], row) }">
              {{ formatNumber(row[col.prop], col.precision) }}
            </span>
          </template>

          <template v-else-if="col.type === 'dict'" #default="{ row }">
            {{ col.dict?.[row[col.prop]] ?? row[col.prop] ?? '-' }}
          </template>

          <template v-else-if="col.type === 'switch'" #default="{ row }">
            <el-switch
              v-model="row[col.prop]"
              :disabled="col.disabled?.(row)"
              @change="(v) => $emit('switch-change', { row, prop: col.prop, value: v })"
            />
          </template>

          <template v-else-if="col.type === 'image'" #default="{ row }">
            <el-image
              :src="row[col.prop]"
              :style="col.style || 'width: 40px; height: 40px; border-radius: 4px;'"
              :fit="col.fit || 'cover'"
              :preview-src-list="[row[col.prop]]"
              preview-teleported
            />
          </template>

          <template v-else-if="col.type === 'link'" #default="{ row }">
            <el-link
              :type="col.linkType || 'primary'"
              :underline="col.underline !== false"
              @click="$emit('link-click', { row, prop: col.prop })"
            >
              {{ col.formatter ? col.formatter(row[col.prop], row) : row[col.prop] }}
            </el-link>
          </template>

          <template v-else-if="col.type === 'custom'" #default="{ row }">
            <slot :name="`col-${col.prop}`" :row="row" :col="col" :value="row[col.prop]">
              {{ row[col.prop] ?? '-' }}
            </slot>
          </template>

          <template v-else-if="col.type === 'actions'" #default="{ row }">
            <slot name="row-actions" :row="row">
              <el-button
                v-for="act in col.actions"
                :key="act.event"
                :type="act.type || 'primary'"
                :size="act.size || 'small'"
                :link="act.link !== false"
                :icon="act.icon"
                :disabled="act.disabled?.(row)"
                @click="handleAction(act, row)"
              >
                {{ act.label }}
              </el-button>
            </slot>
          </template>

          <template v-else #default="{ row }">
            <slot :name="`col-${col.prop}`" :row="row" :col="col" :value="row[col.prop]">
              <span>{{ col.formatter ? col.formatter(row[col.prop], row) : (row[col.prop] ?? '-') }}</span>
            </slot>
          </template>
        </el-table-column>
      </template>

      <template #empty>
        <slot name="empty">
          <EmptyState :description="emptyText" />
        </slot>
      </template>
    </el-table>

    <!-- 分页 -->
    <div v-if="showPagination && (table.total > 0 || !hideOnEmpty)" class="crud-pagination">
      <el-pagination
        v-model:current-page="table.page.current"
        v-model:page-size="table.page.size"
        :page-sizes="pageSizes"
        :total="table.total"
        :layout="layout"
        :background="true"
        @size-change="table.load"
        @current-change="table.load"
      />
    </div>

    <!-- 批量操作 -->
    <div v-if="table.hasSelection && $slots['batch-actions']" class="crud-batch-bar">
      <span class="batch-info">已选 {{ table.selectionCount }} 项</span>
      <slot name="batch-actions" :selection="table.selection" />
    </div>
  </div>
</template>

<script setup>
import { TimeAgo, EmptyState } from './'
import { formatDate, formatNumber } from '@/utils/format'

defineOptions({ name: 'CrudTable' })

const props = defineProps({
  table: { type: Object, required: true },          // useTable() 返回值
  columns: { type: Array, required: true },         // 列配置
  selectable: { type: [Boolean, Function], default: false },
  rowKey: { type: String, default: 'id' },
  size: { type: String, default: 'default' },
  stripe: { type: Boolean, default: true },
  border: { type: Boolean, default: false },
  height: { type: [String, Number], default: undefined },
  maxHeight: { type: [String, Number], default: undefined },
  defaultSort: { type: Object, default: () => ({}) },
  emptyText: { type: String, default: '暂无数据' },

  // 分页
  showPagination: { type: Boolean, default: true },
  hideOnEmpty: { type: Boolean, default: false },
  pageSizes: { type: Array, default: () => [10, 20, 50, 100] },
  layout: { type: String, default: 'total, sizes, prev, pager, next, jumper' },
})

const emit = defineEmits([
  'refresh', 'action', 'link-click', 'switch-change',
])

function handleAction(act, row) {
  if (act.confirm && !window.confirm(act.confirmMessage || `确定要${act.label}?`)) return
  emit('action', { event: act.event, row, action: act })
}

defineExpose({
  refresh: () => props.table.refresh(),
  reset: () => props.table.reset(),
})
</script>

<style scoped>
.crud-table { position: relative; }
.crud-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.crud-batch-bar {
  position: sticky;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 10;
  margin-top: 8px;
  padding: 10px 16px;
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  border: 1px solid #f59e0b;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.2);
}
.batch-info {
  font-weight: 600;
  color: #92400e;
  font-size: 13px;
}
</style>
