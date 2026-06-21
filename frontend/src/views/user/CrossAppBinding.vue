<template>
  <div class="cross-app-page">
    <div class="page-header">
      <h2>🔗 跨应用绑定 (UnionID)</h2>
      <el-button @click="loadMyBinding"><el-icon><Refresh /></el-icon></el-button>
    </div>

    <el-alert type="info" :closable="false" style="margin-bottom: 16px">
      <template #title>💡 UnionID 机制</template>
      同一微信用户在多个应用 (公众号/小程序/开放平台) 下用同一 UnionID, 可实现账号互通 (V5.1 实现, V5.2 跨平台 OAuth 升级)
    </el-alert>

    <!-- 当前绑定状态 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>📱 当前绑定</span>
              <el-tag v-if="myBinding?.unionid" type="success">已绑定</el-tag>
              <el-tag v-else type="info">未绑定</el-tag>
            </div>
          </template>

          <div v-if="myBinding" class="binding-info">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="UnionID">
                <code>{{ myBinding.unionid || '未设置' }}</code>
              </el-descriptions-item>
              <el-descriptions-item label="OpenID (公众号)">
                <code>{{ myBinding.mpOpenid || '-' }}</code>
              </el-descriptions-item>
              <el-descriptions-item label="OpenID (小程序)">
                <code>{{ myBinding.miniOpenid || '-' }}</code>
              </el-descriptions-item>
              <el-descriptions-item label="AppID (开放平台)">
                <code>{{ myBinding.appOpenid || '-' }}</code>
              </el-descriptions-item>
              <el-descriptions-item label="昵称">{{ myBinding.nickname || '-' }}</el-descriptions-item>
              <el-descriptions-item label="头像">
                <img v-if="myBinding.headimgurl" :src="myBinding.headimgurl" style="width: 48px; height: 48px; border-radius: 50%" />
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="绑定时间">{{ myBinding.boundAt || '-' }}</el-descriptions-item>
            </el-descriptions>

            <div class="binding-actions">
              <el-button type="danger" @click="handleUnbind" :disabled="!myBinding.unionid">
                <el-icon><Unlock /></el-icon> 解除绑定
              </el-button>
            </div>
          </div>
          <el-empty v-else description="加载中..." />
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>🔧 跨应用操作</span>
          </template>

          <el-steps direction="vertical" :active="step" finish-status="success">
            <el-step title="1. 扫码关注公众号" description="获取 mpOpenid" />
            <el-step title="2. 使用小程序登录" description="获取 miniOpenid" />
            <el-step title="3. 系统自动合并" description="UnionID 相同则自动绑定" />
            <el-step title="4. 跨应用登录" description="公众号/小程序任一可登录" />
          </el-steps>

          <div class="qr-section">
            <el-button type="primary" @click="showBindQr = true" :disabled="!!myBinding?.unionid">
              <el-icon><FullScreen /></el-icon> 扫码绑定新应用
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 历史绑定记录 -->
    <el-card style="margin-top: 16px">
      <template #header>
        <span>📜 UnionID 关联记录</span>
      </template>
      <el-table :data="historyList" v-loading="loading.history" stripe>
        <el-table-column label="UnionID" prop="unionid" min-width="240">
          <template #default="{ row }"><code>{{ row.unionid }}</code></template>
        </el-table-column>
        <el-table-column label="用户 ID" prop="userId" width="100" />
        <el-table-column label="应用类型" width="120">
          <template #default="{ row }">
            <el-tag v-for="t in row.apps" :key="t" size="small" style="margin-right: 4px">{{ appLabel(t) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="绑定时间" prop="boundAt" width="170" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'" size="small">{{ row.active ? '活跃' : '解绑' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 扫码绑定对话框 -->
    <el-dialog v-model="showBindQr" title="扫码绑定新应用" width="480px">
      <div class="bind-qr-content">
        <el-icon :size="120" color="#67c23a"><FullScreen /></el-icon>
        <p>请使用微信扫描下方二维码</p>
        <div class="fake-qr">
          <div class="qr-pattern"></div>
        </div>
        <p class="hint">扫码后系统将自动合并 UnionID, 无需手动操作</p>
        <el-alert type="warning" :closable="false">
          实际部署时, 此处显示公众号/小程序二维码
        </el-alert>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Unlock, FullScreen } from '@element-plus/icons-vue'
import { getMyBinding, unbindMyself } from '@/api/wechat'

const myBinding = ref(null)
const historyList = ref([])
const showBindQr = ref(false)
const step = ref(2)
const loading = ref({ history: false })

function appLabel(t) {
  const m = { mp: '公众号', mini: '小程序', app: 'App', web: 'Web' }
  return m[t] || t
}

async function loadMyBinding() {
  try {
    const res = await getMyBinding()
    myBinding.value = res.data?.data || res.data || {}
    if (myBinding.value?.unionid) {
      historyList.value = [{ ...myBinding.value, apps: collectApps(myBinding.value), active: true }]
    } else {
      historyList.value = []
    }
  } catch (e) {
    ElMessage.error('加载绑定失败: ' + (e.response?.data?.message || e.message))
  }
}

function collectApps(b) {
  const apps = []
  if (b.mpOpenid) apps.push('mp')
  if (b.miniOpenid) apps.push('mini')
  if (b.appOpenid) apps.push('app')
  return apps
}

async function handleUnbind() {
  await ElMessageBox.confirm(
    '解除绑定后将无法用微信登录, 确认解绑?',
    '警告',
    { type: 'warning' }
  )
  try {
    await unbindMyself()
    ElMessage.success('已解绑')
    await loadMyBinding()
  } catch (e) {
    ElMessage.error('解绑失败')
  }
}

onMounted(loadMyBinding)
</script>

<style scoped>
.cross-app-page { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.binding-info { padding: 8px 0; }
.binding-actions { margin-top: 16px; text-align: right; }
.qr-section { margin-top: 20px; text-align: center; }
.bind-qr-content { text-align: center; padding: 20px 0; }
.bind-qr-content p { margin: 12px 0; color: #606266; }
.fake-qr {
  width: 180px; height: 180px; margin: 16px auto;
  background: linear-gradient(45deg, #f0f0f0 25%, #fff 25%, #fff 50%, #f0f0f0 50%, #f0f0f0 75%, #fff 75%);
  background-size: 16px 16px;
  border: 2px solid #5b8def;
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
}
.qr-pattern { font-size: 14px; color: #5b8def; font-weight: 600; }
.hint { font-size: 13px; color: #909399; }
code { font-family: 'JetBrains Mono', monospace; color: #d63384; padding: 2px 6px; background: #f5f7fa; border-radius: 3px; word-break: break-all; }
</style>