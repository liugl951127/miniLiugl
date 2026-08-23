<!--
  @file settings/System.vue - 系统设置 + 监控 (V8.0, admin only)
  路由: /settings/system
-->
<template>
  <div>
    <div class="page-header">
      <h3>⚡ 系统设置</h3>
    </div>

    <el-tabs v-model="activeSection" class="section-tabs">
      <el-tab-pane label="系统设置" name="system">
        <el-card>
          <template #header>
            <span>系统配置</span>
          </template>
          <el-form :model="config" label-width="140px" v-loading="configLoading">
            <el-form-item label="站点名称">
              <el-input v-model="config.siteName" />
            </el-form-item>
            <el-form-item label="站点 Logo">
              <el-input v-model="config.siteLogo" placeholder="Emoji 或 URL" />
            </el-form-item>
            <el-form-item label="默认模型">
              <el-input v-model="config.defaultModel" />
            </el-form-item>
            <el-form-item label="注册开关">
              <el-switch v-model="config.allowRegister" />
            </el-form-item>
            <el-form-item label="维护模式">
              <el-switch v-model="config.maintenance" />
            </el-form-item>
            <el-form-item label="最大调用/分">
              <el-input-number v-model="config.rateLimit" :min="10" :max="10000" :step="10" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="saveConfig">保存</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="系统监控" name="monitor">
        <el-row :gutter="12" style="margin-bottom:12px">
          <el-col :span="6">
            <el-card body-style="padding:14px" shadow="hover">
              <div style="font-size:12px;color:var(--el-text-color-secondary)">CPU</div>
              <div style="font-size:24px;font-weight:700;color:var(--el-color-primary)">{{ health.cpu || 0 }}%</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card body-style="padding:14px" shadow="hover">
              <div style="font-size:12px;color:var(--el-text-color-secondary)">内存</div>
              <div style="font-size:24px;font-weight:700;color:var(--el-color-success)">{{ health.memory || 0 }}%</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card body-style="padding:14px" shadow="hover">
              <div style="font-size:12px;color:var(--el-text-color-secondary)">磁盘</div>
              <div style="font-size:24px;font-weight:700;color:var(--el-color-warning)">{{ health.disk || 0 }}%</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card body-style="padding:14px" shadow="hover">
              <div style="font-size:12px;color:var(--el-text-color-secondary)">活跃告警</div>
              <div style="font-size:24px;font-weight:700;color:var(--el-color-danger)">{{ health.alerts || 0 }}</div>
            </el-card>
          </el-col>
        </el-row>

        <el-card>
          <template #header><span>微服务健康</span></template>
          <el-table :data="services" stripe v-loading="healthLoading">
            <el-table-column prop="name" label="服务" min-width="160" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'UP' ? 'success' : 'danger'" size="small">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="port" label="端口" width="100" />
            <el-table-column prop="uptime" label="运行时长" min-width="120" />
            <el-table-column prop="lastCheck" label="最后检查" min-width="180" />
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { systemApi } from '@/api/system'
import { getAdminHealth } from '@/api/admin'
import { getFiringAlerts } from '@/api/monitor'

const activeSection = ref('system')
const config = reactive({ siteName: 'Liugl-AI', siteLogo: '🤖', defaultModel: 'chat', allowRegister: true, maintenance: false, rateLimit: 60 })
const configLoading = ref(false)
const saving = ref(false)

const health = ref({ cpu: 0, memory: 0, disk: 0, alerts: 0 })
const services = ref([])
const healthLoading = ref(false)

async function loadConfig() {
  configLoading.value = true
  try {
    const res = await systemApi.getConfig()
    Object.assign(config, res.data || {})
  } catch (e) { /* defaults */ }
  finally { configLoading.value = false }
}

async function saveConfig() {
  saving.value = true
  try {
    await systemApi.saveConfig(config)
    ElMessage.success('已保存')
  } catch (e) { ElMessage.error('保存失败') }
  finally { saving.value = false }
}

async function loadHealth() {
  healthLoading.value = true
  try {
    const [h, a] = await Promise.all([
      getAdminHealth().catch(() => ({})),
      getFiringAlerts().catch(() => [])
    ])
    const data = h.data?.data ?? h.data ?? h ?? {}
    health.value = { cpu: data.cpu || 0, memory: data.memory || 0, disk: data.disk || 0, alerts: (a.data?.data || a.data || []).length }
    services.value = data.services || []
  } catch (e) {
    health.value = { cpu: 0, memory: 0, disk: 0, alerts: 0 }
  } finally { healthLoading.value = false }
}

onMounted(() => { loadConfig(); loadHealth() })
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; font-size: 18px; }
.section-tabs { background: white; padding: 8px; border-radius: 8px; }
</style>
