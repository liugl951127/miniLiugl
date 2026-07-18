<!--
  V5.33: API Key 管理页面 (Day 18)
  用户自主管理个人 API Key
  功能: 创建 / 列表 / 复制 / 禁用 / 轮换 / 删除
-->
<!--
  @file views/apikey/Index.vue (入口/列表)
  @version V3.5.12+ (前端注释补全)
  @description 入口/列表
-->
<template>
  <div class="apikey-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h1>🔑 {{ t('apikey.title') }}</h1>
        <p class="sub">{{ t('apikey.myKeys') }}</p>
      </div>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        {{ t('apikey.createKey') }}
      </el-button>
    </div>

    <!-- 密钥列表 -->
    <el-card class="list-card">
      <template #header>
        <span>{{ t('apikey.myKeys') }}</span>
        <el-button text @click="fetchKeys" :icon="Refresh">刷新</el-button>
      </template>

      <!-- 空状态 -->
      <el-empty v-if="keys.length === 0 && !loading" :description="t('apikey.noKeys')" />

      <!-- 密钥表格 -->
      <el-table v-else :data="keys" stripe v-loading="loading">
        <el-table-column :label="t('apikey.colName')" prop="name" min-width="140" />
        <el-table-column :label="t('apikey.colPrefix')" prop="keyPrefix" min-width="120">
          <template #default="{ row }">
            <code class="key-prefix">{{ row.keyPrefix }}</code>
          </template>
        </el-table-column>
        <el-table-column :label="t('apikey.colScopes')" prop="scopes" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="s in (row.scopes || '').split(',')" :key="s" size="small" style="margin:2px">
              {{ s.trim() }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('apikey.colExpires')" width="130">
          <template #default="{ row }">
            {{ row.expiresAt ? row.expiresAt.slice(0,10) : t('apikey.expiresNever') }}
          </template>
        </el-table-column>
        <el-table-column :label="t('apikey.colUseCount')" prop="useCount" width="90" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.useCount ?? 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('apikey.colLastUsed')" width="130">
          <template #default="{ row }">
            {{ row.lastUsedAt ? row.lastUsedAt.slice(0,16) : t('apikey.neverUsed') }}
          </template>
        </el-table-column>
        <el-table-column :label="t('apikey.colStatus')" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
              {{ row.enabled ? t('apikey.enabled') : t('apikey.disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('apikey.colActions')" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              :type="row.enabled ? 'warning' : 'success'"
              @click="toggleKey(row)"
              :icon="row.enabled ? 'Close' : 'Check'"
            >
              {{ row.enabled ? t('apikey.disable') : t('apikey.enable') }}
            </el-button>
            <el-button size="small" type="primary" @click="rotateKey(row)" :icon="Refresh">
              {{ t('apikey.rotate') }}
            </el-button>
            <el-popconfirm :title="t('apikey.confirmDelete')" @confirm="deleteKey(row.id)">
              <template #reference>
                <el-button size="small" type="danger" :icon="Delete" />
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建密钥弹窗 -->
    <el-dialog v-model="createVisible" :title="t('apikey.createKey')" width="460px">
      <el-form label-width="90">
        <el-form-item :label="t('apikey.name')">
          <el-input v-model="createForm.name" :placeholder="t('apikey.namePlaceholder')" clearable />
        </el-form-item>
        <el-form-item :label="t('apikey.scopes')">
          <el-input v-model="createForm.scopes" :placeholder="t('apikey.scopesPlaceholder')" clearable />
        </el-form-item>
        <el-form-item :label="t('apikey.expiresAt')">
          <el-date-picker
            v-model="createForm.expiresAt"
            type="datetime"
            :placeholder="t('apikey.expiresNever')"
            clearable
            style="width:100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="doCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 轮换密钥弹窗 -->
    <el-dialog v-model="rotateVisible" :title="t('apikey.rotate')" width="460px">
      <el-form label-width="90">
        <el-form-item :label="t('apikey.name')">
          <el-input v-model="rotateForm.name" :placeholder="t('apikey.namePlaceholder')" clearable />
        </el-form-item>
        <el-form-item :label="t('apikey.scopes')">
          <el-input v-model="rotateForm.scopes" :placeholder="t('apikey.scopesPlaceholder')" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rotateVisible = false">取消</el-button>
        <el-button type="warning" :loading="rotating" @click="doRotate">确认轮换</el-button>
      </template>
    </el-dialog>

    <!-- rawKey 一次性展示弹窗 -->
    <el-dialog v-model="rawKeyVisible" :title="t('apikey.rawKey')" width="500px">
      <el-alert type="warning" :closable="false" style="margin-bottom:16px">
        {{ t('apikey.rawKeyTip') }}
      </el-alert>
      <div class="raw-key-box">
        <code class="raw-key-text">{{ newRawKey }}</code>
        <el-button type="primary" @click="copyRawKey" :icon="DocumentCopy">
          {{ copied ? t('apikey.copySuccess') : t('apikey.copyKey') }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh, Delete, DocumentCopy, Check, Close } from '@element-plus/icons-vue'
import { t } from '@/i18n'
import { apiKeyApi } from '@/api/apikey'

const keys = ref([])
const loading = ref(false)
const createVisible = ref(false)
const rotateVisible = ref(false)
const rawKeyVisible = ref(false)
const creating = ref(false)
const rotating = ref(false)
const newRawKey = ref('')
const copied = ref(false)
const rotatingId = ref(null)

const createForm = reactive({ name: '', scopes: 'chat:send,chat:stream', expiresAt: null })
const rotateForm = reactive({ name: '', scopes: '' })

async function fetchKeys() {
  loading.value = true
  try {
    const res = await apiKeyApi.list()
    keys.value = res.data || []
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  createForm.name = ''
  createForm.scopes = 'chat:send,chat:stream'
  createForm.expiresAt = null
  createVisible.value = true
}

async function doCreate() {
  creating.value = true
  try {
    const data = { name: createForm.name, scopes: createForm.scopes }
    if (createForm.expiresAt) {
      data.expiresAt = new Date(createForm.expiresAt).toISOString()
    }
    const res = await apiKeyApi.create(data)
    newRawKey.value = res.data?.rawKey || ''
    createVisible.value = false
    rawKeyVisible.value = true
    copied.value = false
    await fetchKeys()
  } catch (e) {
    ElMessage.error(e.message || '创建失败')
  } finally {
    creating.value = false
  }
}

async function toggleKey(row) {
  try {
    await apiKeyApi.toggle(row.id, !row.enabled)
    ElMessage.success(row.enabled ? t('apikey.disabled') : t('apikey.enabled'))
    await fetchKeys()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

function rotateKey(row) {
  rotatingId.value = row.id
  rotateForm.name = row.name
  rotateForm.scopes = row.scopes
  rotateVisible.value = true
}

async function doRotate() {
  rotating.value = true
  try {
    const data = { name: rotateForm.name, scopes: rotateForm.scopes }
    const res = await apiKeyApi.rotate(rotatingId.value, data)
    newRawKey.value = res.data?.rawKey || ''
    rotateVisible.value = false
    rawKeyVisible.value = true
    copied.value = false
    await fetchKeys()
  } catch (e) {
    ElMessage.error(e.message || '轮换失败')
  } finally {
    rotating.value = false
  }
}

async function deleteKey(id) {
  try {
    await apiKeyApi.remove(id)
    ElMessage.success('已删除')
    await fetchKeys()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

async function copyRawKey() {
  try {
    await navigator.clipboard.writeText(newRawKey.value)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

onMounted(fetchKeys)
</script>

<style scoped>
.apikey-page {
  padding: 24px;
  max-width: 1100px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-header h1 { margin: 0 0 4px; font-size: 22px; }
.page-header .sub { margin: 0; color: #666; font-size: 13px; }
.list-card { margin-top: 8px; }
.key-prefix {
  font-family: monospace;
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}
.raw-key-box {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #f5f7fa;
  padding: 12px;
  border-radius: 6px;
}
.raw-key-text {
  flex: 1;
  font-family: monospace;
  font-size: 14px;
  word-break: break-all;
  color: #333;
}
</style>
