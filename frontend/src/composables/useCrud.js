/**
 * @file useCrud.js - V6.8.2+ 通用 CRUD 业务逻辑
 *
 * 解决 87 个 view 重复的:
 *   - 创建/更新/删除的弹窗状态
 *   - 表单数据
 *   - 提交逻辑
 *   - 错误处理
 *
 * 用法:
 *   const crud = useCrud({
 *     createApi: (data) => api.create(data),
 *     updateApi: (id, data) => api.update(id, data),
 *     deleteApi: (id) => api.delete(id),
 *     onSuccess: () => table.refresh(),
 *     refresh: () => table.load(),
 *   })
 *
 *   crud.openCreate()       // 打开新建弹窗
 *   crud.openEdit(row)      // 打开编辑弹窗
 *   crud.doDelete(row)      // 删除
 *   await crud.doSubmit()   // 提交
 */

import { ref, reactive, computed } from 'vue'
import { useToast } from './useToast'
import { useConfirm } from './useConfirm'

export function useCrud(options = {}) {
  const {
    createApi,        // async (data) => any
    updateApi,        // async (id, data) => any
    deleteApi,        // async (id) => any
    getApi,           // async (id) => entity
    refresh,          // () => Promise<void>  - 刷新表格
    defaultForm = {}, // 新建时的默认表单值
    idKey = 'id',     // 实体 id 字段名
    confirmDelete = true,
  } = options

  const toast = useToast()
  const confirm = useConfirm()

  // ============ 状态 ============
  const dialog = reactive({
    visible: false,
    mode: 'create',  // 'create' | 'edit' | 'view'
    title: '',
    submitting: false,
  })

  const form = reactive({ ...defaultForm })
  const editingId = ref(null)

  // ============ 计算 ============
  const isCreate = computed(() => dialog.mode === 'create')
  const isEdit = computed(() => dialog.mode === 'edit')
  const isView = computed(() => dialog.mode === 'view')

  // ============ 打开弹窗 ============
  function openCreate() {
    Object.assign(form, defaultForm)
    editingId.value = null
    dialog.mode = 'create'
    dialog.title = '新建'
    dialog.visible = true
  }

  async function openEdit(row) {
    const id = row?.[idKey]
    if (!id) return toast.error('记录 ID 不存在')
    try {
      if (getApi) {
        const res = await getApi(id)
        Object.assign(form, res?.data ?? res ?? row)
      } else {
        Object.assign(form, row)
      }
      editingId.value = id
      dialog.mode = 'edit'
      dialog.title = '编辑'
      dialog.visible = true
    } catch (e) {
      toast.error('加载失败: ' + e.message)
    }
  }

  function openView(row) {
    Object.assign(form, row)
    dialog.mode = 'view'
    dialog.title = '查看'
    dialog.visible = true
  }

  function close() {
    dialog.visible = false
    dialog.submitting = false
  }

  // ============ 提交 ============
  async function doSubmit() {
    if (dialog.submitting) return
    dialog.submitting = true
    try {
      let res
      if (isCreate.value) {
        if (!createApi) throw new Error('createApi 未配置')
        res = await createApi({ ...form })
        toast.success('创建成功')
      } else if (isEdit.value) {
        if (!updateApi) throw new Error('updateApi 未配置')
        res = await updateApi(editingId.value, { ...form })
        toast.success('更新成功')
      }
      close()
      if (refresh) await refresh()
      return res
    } catch (e) {
      toast.error(e.message || '操作失败')
      throw e
    } finally {
      dialog.submitting = false
    }
  }

  // ============ 删除 ============
  async function doDelete(row, opts = {}) {
    const id = row?.[idKey]
    if (!id) return toast.error('记录 ID 不存在')
    if (confirmDelete && !opts.skipConfirm) {
      const ok = await confirm({
        title: '确认删除',
        message: `确定要删除 "${row.name || row.title || id}" 吗? 此操作不可恢复.`,
        type: 'warning',
      })
      if (!ok) return
    }
    try {
      if (!deleteApi) throw new Error('deleteApi 未配置')
      await deleteApi(id)
      toast.success('删除成功')
      if (refresh) await refresh()
    } catch (e) {
      toast.error(e.message || '删除失败')
    }
  }

  // 批量删除
  async function doBatchDelete(rows) {
    if (!rows?.length) return toast.warning('请先选择要删除的项')
    const ok = await confirm({
      title: '确认批量删除',
      message: `即将删除 ${rows.length} 条记录, 此操作不可恢复.`,
      type: 'warning',
    })
    if (!ok) return
    try {
      await Promise.all(rows.map(r => deleteApi(r[idKey])))
      toast.success(`已删除 ${rows.length} 条`)
      if (refresh) await refresh()
    } catch (e) {
      toast.error(e.message || '批量删除失败')
    }
  }

  return {
    // 状态
    dialog, form, editingId,
    // 计算
    isCreate, isEdit, isView,
    // 方法
    openCreate, openEdit, openView, close,
    doSubmit, doDelete, doBatchDelete,
  }
}
