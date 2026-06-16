<template>
  <div class="plugins-container">
    <div class="plugins-header">
      <h1>🧩 插件市场 <span class="badge">V2.0</span></h1>
      <p class="sub">下载即用 · 用户可发布 · 支持评分/启停</p>
    </div>

    <el-card>
      <el-row :gutter="12" align="middle">
        <el-col :span="6">
          <el-select v-model="filterCategory" placeholder="全部分类" clearable @change="loadList" style="width:100%">
            <el-option label="UI 组件" value="ui" />
            <el-option label="导出工具" value="export" />
            <el-option label="增强" value="enhance" />
            <el-option label="通用" value="general" />
          </el-select>
        </el-col>
        <el-col :span="18" style="text-align:right">
          <el-button type="primary" @click="showPublish = true">📤 发布我的插件</el-button>
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
              {{ p.scope === 'system' ? '系统' : '用户' }}
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
    <el-dialog v-model="showPublish" title="发布插件" width="500px">
      <el-form>
        <el-form-item label="插件 ID"><el-input v-model="pub.name" placeholder="唯一名称" /></el-form-item>
        <el-form-item label="显示名"><el-input v-model="pub.displayName" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="pub.category">
            <el-option label="UI 组件" value="ui" />
            <el-option label="导出工具" value="export" />
            <el-option label="增强" value="enhance" />
            <el-option label="通用" value="general" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="pub.pluginType">
            <el-option label="Java 类" value="class" />
            <el-option label="HTTP" value="url" />
            <el-option label="JS 脚本" value="js" />
          </el-select>
        </el-form-item>
        <el-form-item label="入口"><el-input v-model="pub.entry" placeholder="类全名 / URL / JS 路径" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="pub.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPublish = false">取消</el-button>
        <el-button type="primary" @click="doPublish">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const API = import.meta.env.VITE_API_BASE || 'http://localhost'
const token = localStorage.getItem('access_token') || ''
const userId = localStorage.getItem('user_id') || '1'

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
    ElMessage.success('评分成功')
    loadList()
  } catch (e: any) { ElMessage.error(e?.message) }
}

async function doPublish() {
  if (!pub.name || !pub.entry) { ElMessage.warning('请填写完整'); return }
  try {
    await axios.post(`${API}/api/v1/agent/plugins?ownerId=${userId}`, pub, auth())
    ElMessage.success('已发布')
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
