/**
 * @file useTable.js - V6.8.2+ 通用表格状态管理
 *
 * 解决 87 个 view 重复的:
 *   - data / loading / total / currentPage / pageSize
 *   - searchKeyword / filters / sort
 *   - selected / selection
 *   - loadData / refresh / reset
 *
 * 用法:
 *   const table = useTable({
 *     fetcher: async (params) => await userApi.list(params),
 *     defaultPageSize: 20,
 *   })
 *
 *   await table.load()  // 拉第一页
 *   table.refresh()     // 重新拉
 *   table.search('keyword')  // 搜索
 *
 * 模板:
 *   <el-table :data="table.data.value" v-loading="table.loading.value">
 *   <el-pagination v-model:current-page="table.page.current"
 *                  v-model:page-size="table.page.size"
 *                  :total="table.total.value"
 *                  @current-change="table.load"
 *                  @size-change="table.load" />
 */

import { ref, reactive, computed, onMounted } from 'vue'

export function useTable(options = {}) {
  const {
    fetcher,              // 必填: async (params) => { data, total }
    defaultPageSize = 20,
    autoLoad = true,
    initialParams = {},   // 额外的初始查询参数
  } = options

  // ============ 状态 ============
  const data = ref([])
  const loading = ref(false)
  const total = ref(0)
  const error = ref(null)

  // 分页 (reactive, 直接 v-model)
  const page = reactive({
    current: 1,
    size: defaultPageSize,
  })

  // 搜索/筛选
  const search = reactive({
    keyword: '',
    ...initialParams,
  })

  // 排序
  const sort = reactive({
    prop: '',
    order: '',  // 'ascending' | 'descending' | ''
  })

  // 选择 (批量操作)
  const selection = ref([])

  // ============ 计算 ============
  const hasSelection = computed(() => selection.value.length > 0)
  const selectionCount = computed(() => selection.value.length)
  const isEmpty = computed(() => !loading.value && data.value.length === 0)

  // 完整查询参数
  const queryParams = computed(() => ({
    page: page.current,
    size: page.size,
    keyword: search.keyword || undefined,
    ...search,
    sortProp: sort.prop || undefined,
    sortOrder: sort.order || undefined,
  }))

  // ============ 方法 ============
  async function load() {
    if (!fetcher) {
      console.warn('[useTable] fetcher not provided, using mock')
      data.value = []
      total.value = 0
      return
    }
    loading.value = true
    error.value = null
    try {
      const res = await fetcher(queryParams.value)
      // 兼容多种返回格式
      const payload = res?.data ?? res
      const list = payload?.data?.list ?? payload?.data?.records ?? payload?.data ?? payload?.list ?? payload?.records ?? payload ?? []
      const count = payload?.data?.total ?? payload?.total ?? payload?.data?.count ?? list.length
      data.value = Array.isArray(list) ? list : []
      total.value = Number(count) || 0
    } catch (e) {
      console.error('[useTable] load failed:', e)
      error.value = e
      data.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  function refresh() {
    return load()
  }

  function reset() {
    page.current = 1
    Object.keys(search).forEach(k => { if (k !== 'keyword') delete search[k] })
    search.keyword = ''
    sort.prop = ''
    sort.order = ''
    selection.value = []
    return load()
  }

  function searchBy(keyword) {
    search.keyword = keyword
    page.current = 1
    return load()
  }

  function setFilter(key, value) {
    search[key] = value
    page.current = 1
    return load()
  }

  function onSortChange({ prop, order }) {
    sort.prop = prop
    sort.order = order
    return load()
  }

  function onSelectionChange(rows) {
    selection.value = rows
  }

  // ============ 自动加载 ============
  if (autoLoad) {
    onMounted(() => load())
  }

  return {
    // 状态
    data, loading, total, error,
    page, search, sort, selection,
    // 计算
    hasSelection, selectionCount, isEmpty,
    queryParams,
    // 方法
    load, refresh, reset, searchBy, setFilter,
    onSortChange, onSelectionChange,
  }
}
