<template>
  <div class="m-me">
    <van-nav-bar title="我的" fixed :border="false" />

    <div class="content">
      <!-- 头部 Profile Card -->
      <div class="profile">
        <van-image
          round
          :src="userStore.profile?.avatar || 'https://img.yzcdn.cn/vant/cat.jpeg'"
          width="80"
          height="80"
          class="profile-avatar"
        />
        <div class="profile-info">
          <h2>{{ userStore.profile?.nickname || '未登录' }}</h2>
          <p class="profile-username">@{{ userStore.profile?.username || 'guest' }}</p>
          <div class="profile-badges">
            <van-tag v-if="userStore.isSuperAdmin" type="danger" effect="dark" size="medium">
              👑 SUPER_ADMIN
            </van-tag>
            <van-tag v-else-if="userStore.isAdmin" type="success" effect="dark" size="medium">
              ADMIN
            </van-tag>
            <van-tag v-else effect="plain" size="medium">USER</van-tag>
          </div>
        </div>
      </div>

      <!-- 账户信息 -->
      <van-cell-group inset title="📋 账户信息">
        <van-cell title="租户" :value="tenantLabel" icon="cluster-o">
          <template #icon>
            <van-icon name="cluster-o" class="cell-icon" />
          </template>
        </van-cell>
        <van-cell title="邮箱" :value="userStore.profile?.email || '—'" icon="envelop-o">
          <template #icon>
            <van-icon name="envelop-o" class="cell-icon" />
          </template>
        </van-cell>
        <van-cell title="用户ID" :value="String(userStore.profile?.id || '—')" icon="idcard-o">
          <template #icon>
            <van-icon name="idcard-o" class="cell-icon" />
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 功能菜单 -->
      <van-cell-group inset title="🛠 功能">
        <van-cell title="🌗 切换桌面版" is-link @click="goDesktop" icon="desktop-o">
          <template #icon>
            <van-icon name="desktop-o" class="cell-icon" />
          </template>
        </van-cell>
        <van-cell title="📋 API 文档" is-link @click="showApi" icon="documentation-o">
          <template #icon>
            <van-icon name="documentation-o" class="cell-icon" />
          </template>
        </van-cell>
        <van-cell title="🔔 通知设置" is-link @click="showNotif = true" icon="bell-o">
          <template #icon>
            <van-icon name="bell-o" class="cell-icon" />
          </template>
        </van-cell>
        <van-cell title="ℹ️ 关于" is-link @click="showAbout = true" icon="info-o">
          <template #icon>
            <van-icon name="info-o" class="cell-icon" />
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 关于弹窗 -->
      <van-popup v-model:show="showAbout" position="bottom" round :style="{ height: '60%' }">
        <div class="about">
          <div class="about-logo">🚀</div>
          <h2>MiniMax Platform</h2>
          <p class="about-version">大模型企业级平台 v2.0</p>
          <p class="about-mobile">移动端 H5 适配 · Vant 组件</p>
          <van-divider />
          <div class="about-stats">
            <div class="stat"><span class="snum">12</span><span class="slabel">微服务</span></div>
            <div class="stat"><span class="snum">116+</span><span class="slabel">API</span></div>
            <div class="stat"><span class="snum">135</span><span class="slabel">测试</span></div>
          </div>
          <p class="about-link">github.com/liugl951127/miniLiugl</p>
        </div>
      </van-popup>

      <!-- 通知设置 -->
      <van-popup v-model:show="showNotif" position="bottom" round :style="{ height: '40%' }">
        <div class="notif-settings">
          <h3>🔔 通知设置</h3>
          <van-cell-group inset>
            <van-cell title="消息通知">
              <template #right-icon>
                <van-switch v-model="notifMsg" size="20" />
              </template>
            </van-cell>
            <van-cell title="Agent 完成提醒">
              <template #right-icon>
                <van-switch v-model="notifAgent" size="20" />
              </template>
            </van-cell>
          </van-cell-group>
        </div>
      </van-popup>

      <!-- 退出登录 -->
      <div class="logout-area">
        <van-button
          type="danger"
          plain
          block
          icon="logout"
          @click="logout"
          class="logout-btn"
        >
          退出登录
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const router = useRouter()
const showAbout = ref(false)
const showNotif = ref(false)
const notifMsg = ref(true)
const notifAgent = ref(false)

const tenantLabel = computed(() => {
  if (userStore.isSuperAdmin) return '👑 平台所有者'
  return 'default'
})

function goDesktop() {
  router.push('/')
}

function showApi() {
  window.open('https://github.com/liugl951127/miniLiugl/blob/main/API.md', '_blank')
}

async function logout() {
  try {
    await showConfirmDialog({ title: '退出确认', message: '确认退出当前账号?' })
    await userStore.logout()
    showToast({ message: '已退出', position: 'bottom' })
    router.push('/login')
  } catch (e) {}
}
</script>

<style scoped>
.m-me { min-height: 100vh; background: #f5f7fa; }
.content { padding: 50px 0 80px; }
.profile {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 32px 16px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
}
.profile-avatar { border: 3px solid rgba(255,255,255,0.3); }
.profile-info h2 { margin: 0 0 4px; font-size: 20px; }
.profile-username { margin: 0 0 8px; font-size: 13px; opacity: 0.8; }
.profile-badges { display: flex; gap: 6px; }
.cell-icon { margin-right: 8px; color: #909399; }
.about { padding: 24px 16px; text-align: center; }
.about-logo { font-size: 60px; margin-bottom: 8px; }
.about h2 { margin: 0 0 6px; font-size: 22px; }
.about-version { margin: 0 0 4px; color: #409eff; font-size: 14px; }
.about-mobile { margin: 0 0 16px; color: #67c23a; font-size: 13px; }
.about-stats { display: flex; justify-content: space-around; margin: 16px 0; }
.stat { display: flex; flex-direction: column; align-items: center; }
.snum { font-size: 20px; font-weight: 700; color: #303133; }
.slabel { font-size: 11px; color: #909399; }
.about-link { font-size: 12px; color: #409eff; word-break: break-all; }
.notif-settings { padding: 16px; }
.notif-settings h3 { margin: 0 0 12px; }
.logout-area { padding: 16px; margin-top: 16px; }
.logout-btn { border-radius: 8px; }
</style>
