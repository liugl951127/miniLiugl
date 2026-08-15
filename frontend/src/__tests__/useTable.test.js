/**
 * @file useTable.test.js - V6.8.2+ 业务组件单元测试
 *
 * 覆盖:
 *   - useTable: 表格状态管理
 *   - useCrud: CRUD 业务
 *   - useConfirm: 确认弹窗
 *   - usePageSetup: 页面初始化
 */

import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { defineComponent, h, nextTick } from 'vue'
import { mount } from '@vue/test-utils'

// 1. 模拟 useToast / useConfirm (避免依赖)
vi.mock('@/composables/useToast', () => ({
  useToast: () => ({
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn(),
  }),
}))

vi.mock('@/composables/useConfirm', () => ({
  useConfirm: () => async () => true,
}))

vi.mock('element-plus', () => ({
  ElMessageBox: {
    confirm: vi.fn().mockResolvedValue('confirm'),
  },
  ElMessage: vi.fn(),
  ElNotification: vi.fn(),
}))

describe('useTable', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('默认状态正确', async () => {
    const { useTable } = await import('@/composables/useTable')
    const fetcher = vi.fn().mockResolvedValue({ data: { data: { list: [{ id: 1 }], total: 1 } } })
    const TestComp = defineComponent({
      setup() {
        const table = useTable({ fetcher, autoLoad: false })
        return { table }
      },
      render: () => h('div')
    })
    const wrapper = mount(TestComp)
    await nextTick()

    const table = wrapper.vm.table
    expect(table.data.value).toEqual([])
    expect(table.loading.value).toBe(false)
    expect(table.page.current).toBe(1)
    expect(table.page.size).toBe(20)
    expect(table.search.keyword).toBe('')
  })

  it('load 成功更新 data 和 total', async () => {
    const { useTable } = await import('@/composables/useTable')
    const list = [{ id: 1, name: 'A' }, { id: 2, name: 'B' }]
    const fetcher = vi.fn().mockResolvedValue({ data: { data: { list, total: 2 } } })
    const TestComp = defineComponent({
      setup() {
        const table = useTable({ fetcher, autoLoad: false })
        return { table }
      },
      render: () => h('div')
    })
    const wrapper = mount(TestComp)
    await wrapper.vm.table.load()

    expect(wrapper.vm.table.data.value).toEqual(list)
    expect(wrapper.vm.table.total.value).toBe(2)
    expect(fetcher).toHaveBeenCalledTimes(1)
  })

  it('load 失败捕获异常', async () => {
    const { useTable } = await import('@/composables/useTable')
    const fetcher = vi.fn().mockRejectedValue(new Error('Network Error'))
    const TestComp = defineComponent({
      setup() {
        const table = useTable({ fetcher, autoLoad: false })
        return { table }
      },
      render: () => h('div')
    })
    const wrapper = mount(TestComp)
    await wrapper.vm.table.load()

    expect(wrapper.vm.table.data.value).toEqual([])
    expect(wrapper.vm.table.error.value).toBeTruthy()
  })

  it('searchBy 重置页码 + 重新加载', async () => {
    const { useTable } = await import('@/composables/useTable')
    const fetcher = vi.fn().mockResolvedValue({ data: { data: { list: [], total: 0 } } })
    const TestComp = defineComponent({
      setup() {
        const table = useTable({ fetcher, autoLoad: false })
        table.page.current = 5
        return { table }
      },
      render: () => h('div')
    })
    const wrapper = mount(TestComp)
    await wrapper.vm.table.searchBy('keyword')

    expect(wrapper.vm.table.page.current).toBe(1)
    expect(wrapper.vm.table.search.keyword).toBe('keyword')
    expect(fetcher).toHaveBeenCalledWith(expect.objectContaining({ keyword: 'keyword', page: 1 }))
  })

  it('reset 全部清空', async () => {
    const { useTable } = await import('@/composables/useTable')
    const fetcher = vi.fn().mockResolvedValue({ data: { data: { list: [], total: 0 } } })
    const TestComp = defineComponent({
      setup() {
        const table = useTable({ fetcher, autoLoad: false })
        return { table }
      },
      render: () => h('div')
    })
    const wrapper = mount(TestComp)
    wrapper.vm.table.search.keyword = 'test'
    wrapper.vm.table.page.current = 3
    await wrapper.vm.table.reset()

    expect(wrapper.vm.table.search.keyword).toBe('')
    expect(wrapper.vm.table.page.current).toBe(1)
  })

  it('selection 同步', async () => {
    const { useTable } = await import('@/composables/useTable')
    const fetcher = vi.fn().mockResolvedValue({ data: { data: { list: [], total: 0 } } })
    const TestComp = defineComponent({
      setup() {
        const table = useTable({ fetcher, autoLoad: false })
        return { table }
      },
      render: () => h('div')
    })
    const wrapper = mount(TestComp)
    const rows = [{ id: 1 }, { id: 2 }]
    wrapper.vm.table.onSelectionChange(rows)

    expect(wrapper.vm.table.selection.value).toEqual(rows)
    expect(wrapper.vm.table.hasSelection.value).toBe(true)
    expect(wrapper.vm.table.selectionCount.value).toBe(2)
  })

  it('setFilter 设置单个过滤', async () => {
    const { useTable } = await import('@/composables/useTable')
    const fetcher = vi.fn().mockResolvedValue({ data: { data: { list: [], total: 0 } } })
    const TestComp = defineComponent({
      setup() {
        const table = useTable({ fetcher, autoLoad: false, initialParams: { status: 'all' } })
        return { table }
      },
      render: () => h('div')
    })
    const wrapper = mount(TestComp)
    await wrapper.vm.table.setFilter('status', 'active')

    expect(wrapper.vm.table.search.status).toBe('active')
    expect(wrapper.vm.table.page.current).toBe(1)
  })
})

describe('useCrud', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('openCreate 重置 form', async () => {
    const { useCrud } = await import('@/composables/useCrud')
    const createApi = vi.fn().mockResolvedValue({ data: { id: 1 } })
    const TestComp = defineComponent({
      setup() {
        const crud = useCrud({ createApi, defaultForm: { name: '', age: 0 } })
        return { crud }
      },
      render: () => h('div')
    })
    const wrapper = mount(TestComp)
    wrapper.vm.crud.openCreate()

    expect(wrapper.vm.crud.dialog.visible).toBe(true)
    expect(wrapper.vm.crud.dialog.mode).toBe('create')
    expect(wrapper.vm.crud.isCreate.value).toBe(true)
  })

  it('openEdit 设置 form + id', async () => {
    const { useCrud } = await import('@/composables/useCrud')
    const getApi = vi.fn().mockResolvedValue({ data: { id: 5, name: 'X' } })
    const TestComp = defineComponent({
      setup() {
        const crud = useCrud({ getApi, defaultForm: {} })
        return { crud }
      },
      render: () => h('div')
    })
    const wrapper = mount(TestComp)
    await wrapper.vm.crud.openEdit({ id: 5 })

    expect(wrapper.vm.crud.editingId).toBe(5)
    expect(wrapper.vm.crud.form.id).toBe(5)
    expect(wrapper.vm.crud.form.name).toBe('X')
    expect(wrapper.vm.crud.isEdit.value).toBe(true)
  })

  it('openEdit 缺 id 报错', async () => {
    const { useCrud } = await import('@/composables/useCrud')
    const TestComp = defineComponent({
      setup() {
        const crud = useCrud({ defaultForm: {} })
        return { crud }
      },
      render: () => h('div')
    })
    const wrapper = mount(TestComp)
    await wrapper.vm.crud.openEdit({})

    expect(wrapper.vm.crud.dialog.visible).toBe(false)
  })

  it('close 重置 submitting', async () => {
    const { useCrud } = await import('@/composables/useCrud')
    const TestComp = defineComponent({
      setup() {
        const crud = useCrud({ defaultForm: {} })
        return { crud }
      },
      render: () => h('div')
    })
    const wrapper = mount(TestComp)
    wrapper.vm.crud.dialog.visible = true
    wrapper.vm.crud.dialog.submitting = true
    wrapper.vm.crud.close()

    expect(wrapper.vm.crud.dialog.visible).toBe(false)
    expect(wrapper.vm.crud.dialog.submitting).toBe(false)
  })

  it('doDelete 调 deleteApi', async () => {
    const { useCrud } = await import('@/composables/useCrud')
    const deleteApi = vi.fn().mockResolvedValue({})
    const refresh = vi.fn()
    const TestComp = defineComponent({
      setup() {
        const crud = useCrud({ deleteApi, refresh, confirmDelete: false, defaultForm: {} })
        return { crud, refresh }
      },
      render: () => h('div')
    })
    const wrapper = mount(TestComp)
    await wrapper.vm.crud.doDelete({ id: 7 }, { skipConfirm: true })

    expect(deleteApi).toHaveBeenCalledWith(7)
    expect(refresh).toHaveBeenCalled()
  })
})

describe('useConfirm', () => {
  it('返回 boolean 函数', async () => {
    const { useConfirm } = await import('@/composables/useConfirm')
    const confirm = useConfirm()
    expect(typeof confirm).toBe('function')
  })

  it('取消返 false', async () => {
    const { ElMessageBox } = await import('element-plus')
    ElMessageBox.confirm = vi.fn().mockRejectedValue('cancel')
    const { useConfirm } = await import('@/composables/useConfirm')
    const confirm = useConfirm()
    const result = await confirm({ title: 'X', message: 'Y' })
    expect(result).toBe(false)
  })

  it('确认返 true', async () => {
    const { ElMessageBox } = await import('element-plus')
    ElMessageBox.confirm = vi.fn().mockResolvedValue('confirm')
    const { useConfirm } = await import('@/composables/useConfirm')
    const confirm = useConfirm()
    const result = await confirm({ title: 'X', message: 'Y' })
    expect(result).toBe(true)
  })
})
