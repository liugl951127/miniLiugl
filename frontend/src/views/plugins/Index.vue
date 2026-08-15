<!-- @file plugins/Index.vue - 插件市场 V6.8 -->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>插件市场</h2>
      <el-input v-model="search" placeholder="搜索插件" style="width:200px" clearable @change="loadPlugins" />
    </div>

    <el-row :gutter="16">
      <el-col v-for="p in plugins" :key="p.id" :span="8">
        <el-card shadow="hover" class="plugin-card">
          <div style="display:flex;justify-content:space-between;align-items:flex-start">
            <div>
              <div style="font-weight:600;font-size:15px">{{ p.name }}</div>
              <div style="font-size:12px;color:#999;margin-top:4px">作者: {{ p.author }}</div>
            </div>
            <el-tag size="small" :type="p.installed ? 'success' : 'info'">{{ p.installed ? '已安装' : '未安装' }}</el-tag>
          </div>
          <div style="margin-top:10px;font-size:13px;color:#666;line-height:1.5">{{ p.description }}</div>
          <div style="margin-top:10px;display:flex;gap:8px;flex-wrap:wrap">
            <el-tag v-for="tag in (p.tags || [])" :key="tag" size="small" type="info">{{ tag }}</el-tag>
          </div>
          <div style="margin-top:12px;display:flex;justify-content:flex-end">
            <el-button size="small" type="primary" @click="installPlugin(p)">{{ p.installed ? '更新' : '安装' }}</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { marketplaceApi } from '@/api/marketplace'

const plugins = ref([])
const search = ref('')

async function loadPlugins() {
  try {
    const r = await marketplaceApi.browse()
    // V6.8.1 fix: 后端返回 {id, name, category, price, downloads}
    // 前端需要 {id, name, author, installed, description, tags}
    plugins.value = (r.data || []).map(item => ({
      ...item,
      author: item.author || '官方',
      installed: item.installed || false,
      description: item.description || item.category || '',
      tags: item.tags || [item.category || 'agent'].filter(Boolean),
    }))
  } catch { plugins.value = [] }
}

async function installPlugin(p) {
  try {
    await marketplaceApi.installAgent(p.id)
    p.installed = true
    ElMessage.success('安装成功')
  } catch { ElMessage.error('安装失败') }
}

onMounted(loadPlugins)
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; h2 { margin: 0; font-size: 16px; } }
.plugin-card { margin-bottom: 12px; }
</style>
