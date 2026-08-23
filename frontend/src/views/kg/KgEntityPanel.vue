<!--
  @file kg/KgEntityPanel.vue - 实体管理面板 (V8.0)
  搜索 + 添加 + 列表 + 邻居
-->
<template>
  <el-card style="margin-top:12px">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px">
        <span>📚 实体管理</span>
        <div style="display:flex;gap:8px;align-items:center">
          <el-input v-model="searchKw" size="small" placeholder="搜索实体" clearable style="width:200px" @keyup.enter="onSearch" />
          <el-button size="small" :icon="Search" @click="onSearch">搜索</el-button>
          <el-button size="small" type="primary" link @click="$emit('add')">+ 添加</el-button>
        </div>
      </div>
    </template>

    <div v-if="showAddForm" style="background:#f8fafc;padding:12px;border-radius:8px;margin-bottom:12px">
      <el-form :model="addForm" inline>
        <el-form-item label="名称"><el-input v-model="addForm.name" size="small" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="addForm.type" size="small" style="width:120px">
            <el-option v-for="t in entityTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="small" :loading="addEntityLoading" @click="$emit('add-entity', addForm)">添加</el-button>
          <el-button size="small" @click="$emit('reset-add')">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table :data="entities" stripe v-loading="loading" max-height="400">
      <el-table-column prop="name" label="名称" min-width="120" />
      <el-table-column prop="type" label="类型" width="100" />
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link :loading="loadingNeighborsId === row.id" @click="$emit('view-neighbors', row)">邻居</el-button>
          <el-button size="small" link type="primary" @click="$emit('select', row)">图谱</el-button>
          <el-button size="small" link type="danger" :loading="deletingEntityId === row.id" @click="$emit('delete', row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { Search } from '@element-plus/icons-vue'

defineProps({
  entities: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  showAddForm: { type: Boolean, default: false },
  addEntityLoading: { type: Boolean, default: false },
  loadingNeighborsId: { type: [String, Number, null], default: null },
  deletingEntityId: { type: [String, Number, null], default: null }
})

const emit = defineEmits([
  'search', 'add', 'add-entity', 'reset-add',
  'view-neighbors', 'select', 'delete'
])

const searchKw = ref('')
const addForm = reactive({ name: '', type: 'Concept' })
const entityTypes = ['Person', 'Organization', 'Concept', 'Event', 'Product', 'Location', 'Other']

function onSearch() {
  emit('search', searchKw.value)
}
</script>
