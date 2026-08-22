<!-- @file super/Index.vue - 超级管理 V6.8 (SUPER_ADMIN only) -->
<template>
  <div class="page-card">
    <div class="page-header"><h2>👑 超级管理控制台</h2></div>

    <el-alert title="超级管理员拥有系统最高权限，请谨慎操作" type="warning" :closable="false" style="margin-bottom:16px" />

    <el-row :gutter="12" style="margin-bottom:20px">
      <el-col v-for="s in stats" :key="s.label" :span="6">
        <el-card shadow="hover">
          <div style="font-size:12px;color: var(--el-text-color-secondary)">{{ s.label }}</div>
          <div style="font-size:28px;font-weight:700;color: var(--el-color-danger);margin-top:4px">{{ s.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-tabs>
      <el-tab-pane label="系统设置" name="settings">
        <el-form label-width="140px" style="max-width:600px">
          <el-form-item label="站点名称"><el-input v-model="settings.siteName" /></el-form-item>
          <el-form-item label="维护模式">
            <el-switch v-model="settings.maintenance" />
          </el-form-item>
          <el-form-item label="注册开放">
            <el-switch v-model="settings.allowRegister" />
          </el-form-item>
          <el-form-item label="LLM 默认模型">
            <el-select v-model="settings.defaultModel" style="width:100%">
              <el-option label="gpt-4o-mini" value="gpt-4o-mini" />
              <el-option label="deepseek-chat" value="deepseek-chat" />
            </el-select>
          </el-form-item>
          <el-form-item><el-button type="primary" @click="saveSettings">保存设置</el-button></el-form-item>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="全局审计" name="audit">
        <el-table :data="auditLogs" stripe>
          <el-table-column prop="user" label="用户" />
          <el-table-column prop="action" label="操作" />
          <el-table-column prop="detail" label="详情" show-overflow-tooltip />
          <el-table-column prop="ip" label="IP" width="130" />
          <el-table-column prop="timestamp" label="时间" width="160" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getRecentAudit, getOpsStats } from '@/api/admin'

const stats = ref([
  { label: '总用户', value: '-' },
  { label: '活跃租户', value: '-' },
  { label: 'API 调用', value: '-' },
  { label: '系统状态', value: '正常' },
])
const settings = ref({ siteName: 'Liugl-AI Platform', maintenance: false, allowRegister: true, defaultModel: 'onnx' })
const auditLogs = ref([])
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    const [ops, audit] = await Promise.allSettled([
      getOpsStats(),
      getRecentAudit(50),
    ])
    if (ops.status === 'fulfilled') {
      const o = ops.value.data || {}
      stats.value = [
        { label: '总用户', value: o.totalUsers ?? o.users ?? '-' },
        { label: '活跃租户', value: o.activeTenants ?? o.tenants ?? '-' },
        { label: 'API 调用', value: o.apiCalls ?? '-' },
        { label: '系统状态', value: '正常' },
      ]
    }
    if (audit.status === 'fulfilled') {
      auditLogs.value = audit.value.data || []
    }
  } catch {
    // silently fail, keep defaults
  } finally {
    loading.value = false
  }
}

function saveSettings() {
  // V7.2: 后端尚未提供 /system/settings 持久化接口，配置仅保存在浏览器本地
  // 如需云端同步，可联系后端实现此接口 (建议路径: minimax-system 模块)
  try {
    localStorage.setItem('superSettings', JSON.stringify(settings.value))
    ElMessage.success('系统设置已保存（仅本地生效）')
  } catch (e) {
    ElMessage.error('保存失败：' + (e?.message || e))
  }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; h2 { margin: 0; font-size: 16px; } }
</style>
