<!-- @file plugins/Index.vue - 插件市场 V6.8.13 (企业级) -->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>🧩 插件市场</h2>
      <div style="display:flex;gap:8px;align-items:center">
        <el-input
          v-model="search"
          placeholder="搜索插件"
          style="width:240px"
          clearable
          :prefix-icon="Search"
          @keyup.enter="loadPlugins"
        />
        <el-select v-model="category" placeholder="全部分类" clearable style="width:140px" @change="loadPlugins">
          <el-option
            v-for="c in categories"
            :key="c"
            :value="c"
            :label="c"
          />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" size="small" @click="loadPlugins">刷新</el-button>
      </div>
    </div>

    <div v-loading="loading" class="plugin-wrapper">
      <el-empty
        v-if="!loading && !filteredPlugins.length"
        :description="emptyDescription"
        :image-size="100"
      >
        <el-button v-if="search" type="primary" @click="clearSearch">清空搜索</el-button>
      </el-empty>

      <el-row v-else :gutter="16">
        <el-col v-for="p in filteredPlugins" :key="p.id" :span="8">
          <el-card shadow="hover" class="plugin-card">
            <div class="plugin-card-header">
              <div>
                <div class="plugin-name">{{ p.name }}</div>
                <div class="plugin-author">作者: {{ p.author }}</div>
              </div>
              <el-tag
                size="small"
                :type="p.installed ? 'success' : 'info'"
                effect="light"
              >{{ p.installed ? '已安装' : '未安装' }}</el-tag>
            </div>
            <div class="plugin-desc">{{ p.description || '暂无描述' }}</div>
            <div class="plugin-tags">
              <el-tag
                v-for="tag in (p.tags || [])"
                :key="tag"
                size="small"
                type="info"
                effect="plain"
              >{{ tag }}</el-tag>
              <span v-if="p.downloads != null" class="plugin-downloads">
                ⬇ {{ formatDownloads(p.downloads) }}
              </span>
            </div>
            <div class="plugin-footer">
              <el-button
                size="small"
                :type="p.installed ? 'default' : 'primary'"
                :loading="installingId === p.id"
                @click="installPlugin(p)"
              >{{ p.installed ? '更新' : '安装' }}</el-button>
              <el-tooltip
                :content="marketplaceApi.uninstallAgent
                  ? '卸载该插件 (需管理员权限)'
                  : '后端未提供卸载接口, 该功能暂不可用'"
                placement="top"
              >
                <el-button
                  v-if="p.installed"
                  size="small"
                  link
                  type="danger"
                  :loading="uninstallingId === p.id"
                  :disabled="!marketplaceApi.uninstallAgent"
                  @click="uninstallPlugin(p)"
                >卸载</el-button>
              </el-tooltip>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { marketplaceApi } from '@/api/marketplace'

const plugins = ref([])
const search = ref('')
const category = ref('')
const loading = ref(false)
const installingId = ref(null)
const uninstallingId = ref(null)
const categories = ref([])

const filteredPlugins = computed(() => {
  if (!search.value) return plugins.value
  const kw = search.value.toLowerCase()
  return plugins.value.filter(p =>
    (p.name || '').toLowerCase().includes(kw) ||
    (p.description || '').toLowerCase().includes(kw) ||
    (p.author || '').toLowerCase().includes(kw)
  )
})

const emptyDescription = computed(() => {
  if (search.value) return `未找到与 ${search.value} 相关的插件`
  return '暂无可用插件'
})

function clearSearch() {
  search.value = ''
  loadPlugins()
}

function formatDownloads(n) {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return n
}

async function loadPlugins() {
  loading.value = true
  try {
    const r = await marketplaceApi.browse({
      q: search.value || undefined,
      category: category.value || undefined,
    })
    plugins.value = (r.data || []).map(item => ({
      ...item,
      author: item.author || '官方',
      installed: item.installed || false,
      description: item.description || item.category || '',
      tags: item.tags || (item.category ? [item.category] : ['agent']),
    }))
    // 提取分类
    const set = new Set()
    plugins.value.forEach(p => (p.tags || []).forEach(t => set.add(t)))
    categories.value = Array.from(set)
  } catch (e) {
    plugins.value = []
    ElMessage.error('加载插件失败：' + (e?.message || '网络异常'))
  } finally {
    loading.value = false
  }
}

async function installPlugin(p) {
  installingId.value = p.id
  try {
    await marketplaceApi.installAgent(p.id)
    p.installed = true
    ElMessage.success(p.installed ? '安装成功' : '已更新')
  } catch (e) {
    ElMessage.error('安装失败：' + (e?.message || '请稍后重试'))
  } finally {
    installingId.value = null
  }
}

async function uninstallPlugin(p) {
  try {
    await ElMessageBox.confirm(
      `确认卸载插件「${p.name}」？`,
      '提示',
      { type: 'warning', confirmButtonText: '卸载', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  uninstallingId.value = p.id
  try {
    if (marketplaceApi.uninstallAgent) {
      await marketplaceApi.uninstallAgent(p.id)
    } else {
      throw new Error('后端未提供卸载接口')
    }
    p.installed = false
    ElMessage.success('已卸载')
  } catch (e) {
    ElMessage.error('卸载失败：' + (e?.message || '请稍后重试'))
  } finally {
    uninstallingId.value = null
  }
}

onMounted(loadPlugins)
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  h2 { margin: 0; font-size: 16px; }
}
.plugin-wrapper {
  min-height: 240px;
}
.plugin-card {
  margin-bottom: 16px;
  transition: transform 0.2s, box-shadow 0.2s;
}
.plugin-card:hover {
  transform: translateY(-2px);
}
.plugin-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}
.plugin-name {
  font-weight: 600;
  font-size: 15px;
  color: var(--el-text-color-primary);
}
.plugin-author {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
.plugin-desc {
  margin-top: 10px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.5;
  min-height: 40px;
}
.plugin-tags {
  margin-top: 10px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  align-items: center;
}
.plugin-downloads {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  margin-left: auto;
}
.plugin-footer {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 4px;
}
</style>
