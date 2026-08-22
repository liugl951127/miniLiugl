<!--
  @file components/EntityDrawer.vue (V3.7.0+ 通用实体 drawer)
  @description 统一抽屉式详情面板 - 适用 kg/agent/model 等
  - 基本信息 (ID/名称/类型/重要性)
  - 描述 (静态 + 编辑切换)
  - 关联关系 (el-table)
  - 操作按钮 (编辑/刷新/删除)
  - 自定义插槽 (extra-fields / extra-actions)
-->
<template>
  <el-drawer
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    :title="title || (entity ? `${entityName}详情` : '详情')"
    direction="rtl"
    size="420px"
    :destroy-on-close="true"
  >
    <div v-if="entity" class="entity-drawer-content">
      <slot name="header" :entity="entity">
        <section class="entity-section">
          <h4 class="section-title">📋 基本信息</h4>
          <el-form :model="editForm" :rules="entityRules" ref="entityFormRef" label-position="top" size="small">
            <el-descriptions-item label="ID">{{ entity.id }}</el-descriptions-item>
            <slot name="fields" :entity="entity" :editing="editing" :form="editForm">
              <el-descriptions-item label="名称">
                <el-input v-if="editing" v-model="editForm.name" size="small" />
                <span v-else>{{ entity.name || entity.title }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="类型">
                <el-tag size="small">{{ entity.type || '未分类' }}</el-tag>
              </el-descriptions-item>
            </slot>
            <slot name="extra-fields" :entity="entity" :editing="editing" :form="editForm" />
            <el-descriptions-item v-if="entity.createdAt" label="创建时间">
              {{ entity.createdAt }}
            </el-descriptions-item>
          </el-form>
        </section>
      </slot>

      <section class="entity-section">
        <h4 class="section-title">📝 描述</h4>
        <el-input
          v-if="editing"
          v-model="editForm.description"
          type="textarea"
          :rows="4"
          placeholder="描述"
        />
        <div v-else class="description-text">
          {{ entity.description || '暂无描述' }}
        </div>
      </section>

      <slot name="relations" :entity="entity">
        <section v-if="relations.length" class="entity-section">
          <h4 class="section-title">🔗 关联关系 ({{ relations.length }})</h4>
          <el-table :data="relations" stripe size="small" max-height="240">
            <el-table-column prop="relation" label="关系" width="100" />
            <el-table-column prop="targetName" label="目标" show-overflow-tooltip />
            <el-table-column prop="weight" label="权重" width="70">
              <template #default="{ row }">
                <el-tag :type="row.weight > 7 ? 'success' : 'info'" size="small">
                  {{ row.weight || 1 }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </slot>

      <section class="entity-section">
        <div class="drawer-actions">
          <template v-if="!editing">
            <slot name="view-actions" :entity="entity">
              <el-button type="primary" :icon="Edit" @click="startEdit">编辑</el-button>
              <el-button v-if="relations.length" :icon="Refresh" @click="$emit('refresh-relations', entity)">刷新关系</el-button>
              <el-button type="danger" :icon="Delete" @click="confirmDelete">删除</el-button>
            </slot>
          </template>
          <template v-else>
            <slot name="edit-actions" :entity="entity" :form="editForm">
              <el-button type="primary" :icon="Check" @click="saveEdit">保存</el-button>
              <el-button :icon="Close" @click="cancelEdit">取消</el-button>
            </slot>
          </template>
        </div>
        <slot name="extra-actions" :entity="entity" :editing="editing" />
      </section>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Edit, Refresh, Delete, Check, Close } from '@element-plus/icons-vue'

const props = defineProps<{
  visible: boolean
  entity: any
  relations?: any[]
  title?: string
  entityName?: string
}>()

const emit = defineEmits<{
  'update:visible': [v: boolean]
  'update': [form: any]
  'delete': [entity: any]
  'refresh-relations': [entity: any]
}>()

const editing = ref(false)

// V3.7.4+ 保存草稿 / 取消恢复
const savedDraft = ref<any>(null)

function getDraftKey() {
  return 'entity-draft-' + (props.entity?.id || 'new')
}

function loadDraft() {
  if (!props.entity) return
  const saved = localStorage.getItem(getDraftKey())
  if (saved) {
    try {
      savedDraft.value = JSON.parse(saved)
      editForm.value = { ...editForm.value, ...savedDraft.value }
    } catch (e) {
      console.warn('[EntityDrawer] 草稿数据损坏:', e)
    }
  }
}

function saveDraft() {
  if (!props.entity) return
  localStorage.setItem(getDraftKey(), JSON.stringify(editForm.value))
}

function _clearDraft() {
  if (!props.entity) return
  localStorage.removeItem(getDraftKey())
  savedDraft.value = null
}

const editForm = ref<any>({})

watch(editForm, () => {
  if (editing.value) saveDraft()
}, { deep: true })

watch(() => props.entity, () => loadDraft(), { immediate: true })

const _defaultEntityName = computed(() => props.entityName || '实体')

watch(
  () => props.entity,
  (e) => {
    if (e) {
      editing.value = false
      editForm.value = {
        name: e.name || e.title || '',
        description: e.description || '',
        importance: e.importance || 5,
        ...e,
      }
    }
  },
  { immediate: true }
)

function startEdit() {
  editing.value = true
  if (props.entity) {
    editForm.value = {
      name: props.entity.name || props.entity.title || '',
      description: props.entity.description || '',
      importance: props.entity.importance || 5,
      ...props.entity,
    }
  }
}

function cancelEdit() {
  if (savedDraft.value) {
    editForm.value = { ...editForm.value, ...savedDraft.value }
  } else {
    editing.value = false
  }
}

async function saveEdit() {
  emit('update', editForm.value)
}

async function confirmDelete() {
  if (!props.entity) return
  try {
    await ElMessageBox.confirm(
      `确认删除 "${props.entity.name || props.entity.title}" 吗?`,
      '提示',
      { type: 'warning' }
    )
    emit('delete', props.entity)
  } catch (e) {
    // 取消
  }
}
</script>

<style lang="scss" scoped>
.entity-drawer-content { padding: 0 8px; }
.entity-section { margin-bottom: 24px; }
.entity-section .section-title {
  font-size: 14px; font-weight: 600;
  margin-bottom: 12px; color: var(--el-text-color-primary);
}
.description-text {
  padding: 8px 12px; background: var(--el-fill-color-light);
  border-radius: 4px; line-height: 1.6; min-height: 40px;
}
.drawer-actions {
  display: flex; gap: 8px; flex-wrap: wrap;
}
</style>
