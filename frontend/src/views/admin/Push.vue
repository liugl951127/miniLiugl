<!--
  @file views/admin/Push.vue (V3.5.75 标准化模板)
  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js
-->
<!--
  @file views/admin/Push.vue (V3.5.48)
  @description 消息推送 - 多渠道 (APNs/FCM/Web Push/VAPID)
  - 18 端点: subscribe/unsubscribe, subscriptions/all, send/user/platform/broadcast, messages, stats, integration/{auto,apns,fcm,web,health,stats,detect,vapid-public-key}
-->
<template>
  <div class="page-push page">
    <el-card>
      <template #header>
        <div class="header">
          <span>📨 消息推送 <el-tag size="small" type="success">V3.5.48</el-tag></span>
          <div>
            <el-radio-group v-model="tab" size="small">
              <el-radio-button value="subscriptions">订阅</el-radio-button>
              <el-radio-button value="send">发送</el-radio-button>
              <el-radio-button value="messages">历史</el-radio-button>
              <el-radio-button value="integration">集成</el-radio-button>
              <el-radio-button value="stats">统计</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>

      <!-- 订阅管理 -->
      <div v-if="tab === 'subscriptions'" class="tab-section">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-card shadow="never">
              <template #header>添加订阅</template>
              <el-form label-position="top">
                <el-form-item label="Endpoint URL">
                  <el-input v-model="subForm.endpoint" type="textarea" :rows="2" />
                </el-form-item>
                <el-form-item label="Keys (JSON)">
                  <el-input v-model="subForm.keys" type="textarea" :rows="2" placeholder="{&quot;p256dh&quot;:&quot;...&quot;, &quot;auth&quot;:&quot;...&quot;}" />
                </el-form-item>
                <el-form-item label="User ID">
                  <el-input v-model="subForm.userId" />
                </el-form-item>
                <el-button type="primary" @click="onSubscribe">+ 订阅</el-button>
              </el-form>
            </el-card>
          </el-col>
          <el-col :span="16">
            <el-card shadow="never">
              <template #header>
                <div class="header">
                  <span>订阅列表 ({{ subscriptions.length }})</span>
                  <div>
                    <el-button size="small" @click="loadSubscriptions" :icon="Refresh">刷新</el-button>
                    <el-button size="small" @click="loadAllSubscriptions" :icon="Refresh">全部</el-button>
                  </div>
                </div>
              </template>
              <el-table :data="subscriptions" border>
                <el-table-column prop="id" label="ID" width="60" />
                <el-table-column prop="userId" label="User ID" width="100" />
                <el-table-column prop="endpoint" label="Endpoint" show-overflow-tooltip />
                <el-table-column prop="platform" label="平台" width="100" />
                <el-table-column prop="createdAt" label="创建时间" width="180" />
                <el-table-column label="操作" width="100">
                  <template #default="{ row }">
                    <el-button size="small" type="danger" @click="onUnsubscribe(row)">取消</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 发送推送 -->
      <div v-if="tab === 'send'" class="tab-section">
        <el-card shadow="never">
          <el-form label-position="top">
            <el-form-item label="发送对象">
              <el-radio-group v-model="sendForm.target">
                <el-radio-button value="user">指定用户</el-radio-button>
                <el-radio-button value="platform">指定平台</el-radio-button>
                <el-radio-button value="broadcast">广播</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="sendForm.target === 'user'" label="User ID">
              <el-input v-model="sendForm.userId" />
            </el-form-item>
            <el-form-item v-if="sendForm.target === 'platform'" label="平台">
              <el-select v-model="sendForm.platform" style="width: 200px">
                <el-option label="Web" value="web" />
                <el-option label="iOS" value="ios" />
                <el-option label="Android" value="android" />
              </el-select>
            </el-form-item>
            <el-form-item label="标题">
              <el-input v-model="sendForm.title" />
            </el-form-item>
            <el-form-item label="内容">
              <el-input v-model="sendForm.body" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item label="图标 URL">
              <el-input v-model="sendForm.icon" />
            </el-form-item>
            <el-form-item label="点击 URL">
              <el-input v-model="sendForm.clickUrl" />
            </el-form-item>
            <el-button type="primary" size="large" @click="onSend" :loading="sending">📨 发送</el-button>
          </el-form>
        </el-card>
      </div>

      <!-- 历史消息 -->
      <div v-if="tab === 'messages'" class="tab-section">
        <el-table :data="messages" border>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="title" label="标题" width="200" />
          <el-table-column prop="body" label="内容" show-overflow-tooltip />
          <el-table-column prop="target" label="对象" width="120" />
          <el-table-column prop="sentAt" label="发送时间" width="180" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'OK' ? 'success' : 'danger'">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 集成 -->
      <div v-if="tab === 'integration'" class="tab-section">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>环境检测</template>
              <el-form label-position="top" v-loading="detecting">
                <el-form-item label="VAPID Public Key">
                  <el-input v-model="integration.vapidKey" readonly />
                </el-form-item>
                <el-form-item label="支持的平台">
                  <el-checkbox-group v-model="integration.platforms">
                    <el-checkbox value="web">Web Push</el-checkbox>
                    <el-checkbox value="apns">APNs (iOS)</el-checkbox>
                    <el-checkbox value="fcm">FCM (Android)</el-checkbox>
                  </el-checkbox-group>
                </el-form-item>
                <el-button-group>
                  <el-button @click="onAutoDetect" type="primary">🔍 自动检测</el-button>
                  <el-button @click="onHealth">💚 健康检查</el-button>
                  <el-button @click="loadIntegrationStats">📊 统计</el-button>
                </el-button-group>
              </el-form>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>平台配置</template>
              <el-tabs>
                <el-tab-pane label="APNs" name="apns">
                  <el-form label-position="top">
                    <el-form-item label="Team ID"><el-input v-model="integration.apns.teamId" /></el-form-item>
                    <el-form-item label="Key ID"><el-input v-model="integration.apns.keyId" /></el-form-item>
                    <el-form-item label="Bundle ID"><el-input v-model="integration.apns.bundleId" /></el-form-item>
                    <el-button @click="onSaveApns">💾 保存</el-button>
                  </el-form>
                </el-tab-pane>
                <el-tab-pane label="FCM" name="fcm">
                  <el-form label-position="top">
                    <el-form-item label="Project ID"><el-input v-model="integration.fcm.projectId" /></el-form-item>
                    <el-form-item label="Service Account JSON">
                      <el-input v-model="integration.fcm.serviceAccount" type="textarea" :rows="3" />
                    </el-form-item>
                    <el-button @click="onSaveFcm">💾 保存</el-button>
                  </el-form>
                </el-tab-pane>
                <el-tab-pane label="Web" name="web">
                  <el-form label-position="top">
                    <el-form-item label="VAPID Subject"><el-input v-model="integration.web.subject" /></el-form-item>
                    <el-form-item label="VAPID Public Key"><el-input v-model="integration.web.vapidPublicKey" /></el-form-item>
                    <el-form-item label="VAPID Private Key"><el-input v-model="integration.web.vapidPrivateKey" type="password" /></el-form-item>
                    <el-button @click="onSaveWeb">💾 保存</el-button>
                  </el-form>
                </el-tab-pane>
              </el-tabs>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 统计 -->
      <div v-if="tab === 'stats'" class="tab-section">
        <el-row :gutter="16">
          <el-col :span="6"><div class="stat-card"><div class="num">{{ pushStat.totalSent || 0 }}</div><div>总发送数</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="num">{{ pushStat.successRate || 0 }}%</div><div>成功率</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="num">{{ subscriptions.length }}</div><div>活跃订阅</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="num">{{ pushStat.platformCount || 0 }}</div><div>支持平台</div></div></el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, reactive, onMounted, readonly } from 'vue'
import { useToast } from '@/composables/useToast'
import { ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { pushSubscribe, pushUnsubscribe, pushSubscriptions, pushAllSubscriptions, pushSendToUser, pushSendToPlatform, pushBroadcast, pushMessages, pushStats, pushIntegrationAuto, pushIntegrationApns, pushIntegrationFcm, pushIntegrationWeb, pushIntegrationHealth, pushIntegrationStats, pushIntegrationDetect, pushIntegrationVapidKey } from '@/api/ai'

const tab = ref('subscriptions')
const toast = useToast()
const sending = ref(false)
const detecting = ref(false)

const subForm = reactive({ endpoint: '', keys: '', userId: '' })
const sendForm = reactive({ target: 'user', userId: '', platform: 'web', title: '', body: '', icon: '', clickUrl: '' })
const integration = reactive({
  vapidKey: '',
  platforms: [],
  apns: { teamId: '', keyId: '', bundleId: '' },
  fcm: { projectId: '', serviceAccount: '' },
  web: { subject: '', vapidPublicKey: '', vapidPrivateKey: '' }
})

const subscriptions = ref([])
const messages = ref([])
const pushStat = reactive({})

async function loadSubscriptions() {
  try { const r = await pushSubscriptions(); subscriptions.value = r.data || [] } catch (e) {}
}

async function loadAllSubscriptions() {
  try { const r = await pushAllSubscriptions(); subscriptions.value = r.data || []; toast.success(`加载 ${subscriptions.value.length} 条`) } catch (e) {}
}

async function loadMessages() {
  try { const r = await pushMessages({ limit: 50 }); messages.value = r.data || [] } catch (e) {}
}

async function _loadStats() {
  try { const r = await pushStats(); Object.assign(pushStat, r.data || {}) } catch (e) {}
}

async function onSubscribe() {
  try {
    const keysObj = subForm.keys ? JSON.parse(subForm.keys) : {}
    await pushSubscribe({ endpoint: subForm.endpoint, keys: keysObj, userId: subForm.userId })
    toast.success('已订阅')
    subForm.endpoint = ''; subForm.keys = ''; subForm.userId = ''
    loadSubscriptions()
  } catch (e) {}
}

async function onUnsubscribe(row) {
  try {
    await ElMessageBox.confirm('取消订阅?', '提示', { type: 'warning' })
    await pushUnsubscribe({ endpoint: row.endpoint })
    toast.success('已取消')
    loadSubscriptions()
  } catch (e) { if (e !== 'cancel') {} }
}

async function onSend() {
  if (!sendForm.title || !sendForm.body) { toast.warning('请填标题和内容'); return }
  sending.value = true
  try {
    const payload = { title: sendForm.title, body: sendForm.body, icon: sendForm.icon, clickUrl: sendForm.clickUrl }
    if (sendForm.target === 'user') {
      await pushSendToUser({ userId: sendForm.userId, ...payload })
    } else if (sendForm.target === 'platform') {
      await pushSendToPlatform({ platform: sendForm.platform, ...payload })
    } else {
      await pushBroadcast(payload)
    }
    toast.success('已发送')
    loadMessages()
  } catch (e) {} finally { sending.value = false }
}

async function onAutoDetect() {
  detecting.value = true
  try {
    const r = await pushIntegrationAuto()
    Object.assign(integration, r.data || {})
    toast.success('检测完成')
  } catch (e) {} finally { detecting.value = false }
}

async function loadVapidKey() {
  try {
    const r = await pushIntegrationVapidKey()
    integration.vapidKey = r.data?.key || ''
  } catch (e) {}
}

async function onHealth() {
  try {
    const r = await pushIntegrationHealth()
    toast.success(r.data?.message || '健康')
  } catch (e) {}
}

async function loadIntegrationStats() {
  try { const r = await pushIntegrationStats(); Object.assign(pushStat, r.data || {}) } catch (e) {}
}

async function onSaveApns() { try { await pushIntegrationApns(integration.apns); toast.success('APNs 已保存') } catch (e) {} }
async function onSaveFcm() { try { await pushIntegrationFcm(integration.fcm); toast.success('FCM 已保存') } catch (e) {} }
async function onSaveWeb() { try { await pushIntegrationWeb(integration.web); toast.success('Web 已保存') } catch (e) {} }

onMounted(() => {
  loadSubscriptions()
  loadVapidKey()
})
</script>

<style scoped>
.page { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.tab-section { padding: 8px 0; }
.stat-card { padding: 16px; background: #fff; border-radius: 4px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
.stat-card .num { font-size: 24px; font-weight: 600; color: #409eff; margin-bottom: 4px; }
</style>
