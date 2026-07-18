<!--
  @file views/plugins/Index.vue (入口/列表)
  @version V3.5.12+ (前端注释补全)
  @description 入口/列表
-->
<template>
  <div class="plugins-container">
    <div class="plugins-header">
      <h1>🧩 {{ t('plugins.title') }} <span class="badge">V2.0</span></h1>
      <p class="sub">{{ t('plugins.subtitle') }}</p>
    </div>

    <el-card>
      <el-row :gutter="12" align="middle">
        <el-col :span="6">
          <el-select v-model="filterCategory" :placeholder="t('plugins.allCategories')" clearable @change="loadList" style="width:100%">
            <el-option :label="t('plugins.uiComponent')" value="ui" />
            <el-option :label="t('plugins.exportTool')" value="export" />
            <el-option :label="t('plugins.enhance')" value="enhance" />
            <el-option :label="t('plugins.general')" value="general" />
          </el-select>
        </el-col>
        <el-col :span="18" style="text-align:right">
          <el-button type="primary" @click="showPublish = true">{{ t('plugins.publishMy') }}</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col v-for="p in plugins" :key="p.id" :span="8" style="margin-bottom:20px">
        <el-card shadow="hover" class="plugin-card">
          <div class="plugin-head">
            <span class="plugin-icon">{{ iconOf(p.category) }}</span>
            <div>
              <h3>{{ p.displayName }}</h3>
              <span class="meta">v{{ p.version }} · {{ p.author || '匿名' }}</span>
            </div>
            <el-tag :type="p.scope === 'system' ? 'danger' : 'success'" size="small">
              {{ p.scope === 'system' ? t('plugins.system') : t('plugins.user') }}
            </el-tag>
          </div>
          <p class="desc">{{ p.description || '—' }}</p>
          <div class="tags">
            <el-tag size="small">{{ p.category }}</el-tag>
            <el-tag size="small" type="info">{{ p.pluginType }}</el-tag>
          </div>
          <el-divider style="margin: 12px 0" />
          <div class="actions">
            <span class="stat">⭐ {{ p.rating || 0 }}</span>
            <span class="stat">📥 {{ p.downloads || 0 }}</span>
            <div style="flex:1"></div>
            <el-rate v-model="ratingDraft[p.id]" :max="5" allow-half show-text
                     @change="(v: number) => rate(p.id, v)" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 发布对话框 -->
    <el-dialog v-model="showPublish" :title="t('plugins.publishDialog')" width="500px">
      <el-form>
        <el-form-item :label="t('plugins.pluginId')"><el-input v-model="pub.name" :placeholder="t('plugins.uniqueName')" /></el-form-item>
        <el-form-item :label="t('plugins.displayName')"><el-input v-model="pub.displayName" /></el-form-item>
        <el-form-item :label="t('common.category')">
          <el-select v-model="pub.category">
            <el-option :label="t('plugins.uiComponent')" value="ui" />
            <el-option :label="t('plugins.exportTool')" value="export" />
            <el-option :label="t('plugins.enhance')" value="enhance" />
            <el-option :label="t('plugins.general')" value="general" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('plugins.type')">
          <el-select v-model="pub.pluginType">
            <el-option :label="t('plugins.javaClass')" value="class" />
            <el-option :label="t('plugins.http')" value="url" />
            <el-option :label="t('plugins.jsScript')" value="js" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('plugins.entry')"><el-input v-model="pub.entry" :placeholder="t('plugins.entryPlaceholder')" /></el-form-item>
        <el-form-item :label="t('common.description')"><el-input v-model="pub.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPublish = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="doPublish">{{ t('plugins.publish') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
// ───── 依赖导入 ─────
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { t } from '@/i18n'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const API = import.meta.env.VITE_API_BASE || 'http://localhost'
const token = userStore.accessToken || ''
const userId = String(userStore.profile?.id || 1)

const plugins = ref<any[]>([])
const filterCategory = ref('')
const ratingDraft = reactive<Record<number, number>>({})
const showPublish = ref(false)
const pub = reactive({
  name: '', displayName: '', category: 'general', pluginType: 'class',
  entry: '', description: '', version: '1.0.0', author: 'me'
})

function auth() { return { headers: { Authorization: `Bearer ${token}` } } }

async function loadList() {
  try {
    const { data } = await axios.get(`${API}/api/v1/agent/plugins`, {
      params: { category: filterCategory.value || undefined }, ...auth()
    })
    plugins.value = data.data || []
  } catch (e: any) { ElMessage.error(e?.message) }
}

async function rate(id: number, score: number) {
  try {
    await axios.post(`${API}/api/v1/agent/plugins/${id}/rate?score=${score}`, {}, auth())
    ElMessage.success(t('plugins.rateSuccess'))
    loadList()
  } catch (e: any) { ElMessage.error(e?.message) }
}

async function doPublish() {
  if (!pub.name || !pub.entry) { ElMessage.warning(t('common.fillComplete')); return }
  try {
    await axios.post(`${API}/api/v1/agent/plugins?ownerId=${userId}`, pub, auth())
    ElMessage.success(t('plugins.published'))
    showPublish.value = false
    loadList()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || e?.message) }
}

function iconOf(cat: string) {
  return ({ ui: '🎨', export: '📤', enhance: '✨', general: '🧩' } as any)[cat] || '🧩'
}

onMounted(loadList)
</script>

<style scoped>
.plugins-container { padding: 20px; max-width: 1200px; margin: 0 auto; }
.plugins-header h1 { display:flex; align-items:center; gap:10px; }
.badge {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 600;
}
.sub { color: #666; margin-bottom: 20px; }
.plugin-card { transition: transform 0.2s; }
.plugin-card:hover { transform: translateY(-4px); }
.plugin-head { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.plugin-icon { font-size: 32px; }
.plugin-head h3 { margin: 0; font-size: 16px; }
.meta { color: #999; font-size: 12px; }
.desc { color: #666; min-height: 40px; font-size: 13px; }
.tags { display: flex; gap: 4px; }
.actions { display: flex; align-items: center; gap: 12px; }
.stat { color: #666; font-size: 13px; }
</style>
