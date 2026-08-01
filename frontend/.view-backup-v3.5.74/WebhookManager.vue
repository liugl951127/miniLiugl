<!--
  @file views/ai/WebhookManager.vue (WebhookManager 页面)
  @version V3.5.12+ (前端注释补全)
  @description WebhookManager 页面
-->
<template>
  <div class="webhook-container">
    <div class="header">
      <h1>🔗 Webhook 集成 <span class="badge">V2.9.1</span></h1>
      <p class="sub">事件订阅 / HMAC 签名 / 异步投递 / 重试</p>
    </div>

    <el-row :gutter="16" v-if="stats">
      <el-col :span="8">
        <el-card class="kpi">
          <div class="kpi-label">总订阅</div>
          <div class="kpi-value">{{ stats.webhookCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="kpi success">
          <div class="kpi-label">活跃</div>
          <div class="kpi-value">{{ stats.activeWebhooks }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="kpi primary">
          <div class="kpi-label">已发事件</div>
          <div class="kpi-value">{{ totalEvents }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px">
      <template #header>
        <span>📡 我的 Webhook</span>
        <el-button type="primary" size="small" @click="showCreate = true" style="float:right">+ 创建</el-button>
      </template>
      <el-table :data="webhooks" size="small" stripe>
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="url" label="URL" show-overflow-tooltip />
        <el-table-column prop="events" label="订阅事件" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-for="e in (row.events || '').split(',').filter(x => x)" :key="e" size="small" style="margin-right: 4px">{{ e }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? '已启用' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deliveryCount" label="投递" width="100" />
        <el-table-column label="成功/失败" width="120">
          <template #default="{ row }">
            <span style="color: #67c23a">{{ row.successCount }}</span> /
            <span style="color: #f56c6c">{{ row.failCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="最后状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.lastStatus" :type="row.lastStatus >= 200 && row.lastStatus < 300 ? 'success' : 'danger'" size="small">
              {{ row.lastStatus }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button size="small" @click="test(row)">🧪 测试</el-button>
            <el-button size="small" @click="toggleEnabled(row)">
              {{ row.enabled ? '停用' : '启用' }}
            </el-button>
            <el-button size="small" @click="showDeliveries(row)">📜 日志</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 事件类型 -->
    <el-card style="margin-top: 16px">
      <template #header><span>📢 支持事件</span></template>
      <el-row :gutter="12">
        <el-col :span="6" v-for="ev in eventTypes" :key="ev" style="margin-bottom: 8px">
          <el-tag size="small">{{ ev }}</el-tag>
          <span style="margin-left: 8px; color: #909399; font-size: 12px">{{ eventCounters[ev] || 0 }} 次</span>
        </el-col>
      </el-row>
    </el-card>

    <!-- 投递日志 -->
    <el-card style="margin-top: 16px" v-if="deliveries.length">
      <template #header>
        <span>📜 投递日志 ({{ deliveries.length }})</span>
      </template>
      <el-table :data="deliveries" size="small" stripe>
        <el-table-column prop="eventType" label="事件" width="160" />
        <el-table-column prop="eventId" label="事件ID" width="180" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="responseStatus" label="HTTP" width="80" />
        <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
        <el-table-column prop="retryCount" label="重试" width="60" />
        <el-table-column prop="createdAt" label="时间">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建对话框 -->
    <el-dialog v-model="showCreate" title="🔗 创建 Webhook" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="例: Slack 通知" />
        </el-form-item>
        <el-form-item label="URL" required>
          <el-input v-model="form.url" placeholder="https://hooks.slack.com/..." />
        </el-form-item>
        <el-form-item label="事件">
          <el-select v-model="form.events" multiple placeholder="选事件 (空=全部)" style="width:100%">
            <el-option v-for="ev in eventTypes" :key="ev" :value="ev" :label="ev" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" @click="createWebhook" :loading="creating">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { webhookApi } from '@/api/webhook'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const webhooks = ref([])
const stats = ref(null)
const eventTypes = ref([])
const eventCounters = ref({})
const deliveries = ref([])

const showCreate = ref(false)
const creating = ref(false)
const form = reactive({ name: '', url: '', events: [], description: '' })

const totalEvents = computed(() => {
  return Object.values(eventCounters.value).reduce((a, b) => a + b, 0)
})

const formatTime = (iso) => {
  if (!iso) return '-'
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

const loadAll = async () => {
  try {
    const [list, st, types] = await Promise.all([
      webhookApi.list(userStore.profile?.id),
      webhookApi.stats(),
      webhookApi.eventTypes()
    ])
    webhooks.value = list.data || []
    stats.value = st.data
    eventCounters.value = st.data?.eventCounters || {}
    eventTypes.value = types.data || []
  } catch (e) {
    console.error(e)
  }
}

const createWebhook = async () => {
  if (!form.name || !form.url) {
    ElMessage.warning('请填写名称和 URL')
    return
  }
  creating.value = true
  try {
    const res = await webhookApi.create({
      ...form,
      events: form.events.join(','),
      ownerId: userStore.profile?.id || 0
    })
    if (res.data?.code === 0) {
      ElMessage.success('创建成功, secret 已生成')
      showCreate.value = false
      form.name = ''
      form.url = ''
      form.events = []
      form.description = ''
      loadAll()
    } else {
      ElMessage.error(res.data?.message)
    }
  } catch (e) {
    ElMessage.error('创建失败: ' + e.message)
  } finally {
    creating.value = false
  }
}

const test = async (wh) => {
  try {
    const res = await webhookApi.test(wh.webhookId)
    if (res.data?.code === 0) {
      const d = res.data.data
      ElMessage.success(`测试投递 ${d.status} (${d.responseStatus || 'N/A'})`)
      loadAll()
      showDeliveries(wh)
    } else {
      ElMessage.error('测试失败')
    }
  } catch (e) {
    ElMessage.error('测试失败: ' + e.message)
  }
}

const toggleEnabled = async (wh) => {
  try {
    await webhookApi.update(wh.webhookId, { enabled: wh.enabled ? 0 : 1 })
    ElMessage.success('已更新')
    loadAll()
  } catch (e) {
    ElMessage.error('更新失败: ' + e.message)
  }
}

const remove = async (wh) => {
  try {
    await ElMessageBox.confirm('确定删除此 Webhook?', '确认', { type: 'warning' })
    await webhookApi.delete(wh.webhookId)
    ElMessage.success('已删除')
    loadAll()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const showDeliveries = async (wh) => {
  try {
    const res = await webhookApi.deliveries(wh.webhookId, 50)
    deliveries.value = res.data || []
  } catch (e) {
    console.warn(e)
  }
}

onMounted(loadAll)
</script>

<style scoped>
.webhook-container { padding: 20px; }
.header h1 { margin: 0 0 4px 0; font-size: 24px; }
.badge { background: #9b59b6; color: #fff; font-size: 12px; padding: 2px 8px; border-radius: 4px; margin-left: 8px; }
.sub { color: #909399; margin: 0 0 16px 0; font-size: 13px; }
.kpi { text-align: center; }
.kpi-label { color: #909399; font-size: 12px; }
.kpi-value { font-size: 22px; font-weight: 600; margin-top: 4px; }
.kpi.success { border-left: 3px solid #67c23a; }
.kpi.primary { border-left: 3px solid #409eff; }
</style>
