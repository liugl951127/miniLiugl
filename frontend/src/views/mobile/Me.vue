<template>
  <div class="m-me">
    <van-nav-bar title="我的" fixed :border="false" />

    <div class="content">
      <!-- 头部 -->
      <div class="profile">
        <van-image
          round
          :src="userStore.profile?.avatar || 'https://img.yzcdn.cn/vant/cat.jpeg'"
          width="80"
          height="80"
        />
        <h2>{{ userStore.profile?.nickname || '未登录' }}</h2>
        <p>@{{ userStore.profile?.username }}</p>
        <van-tag v-if="userStore.isSuperAdmin" type="danger" effect="dark">👑 SUPER_ADMIN</van-tag>
        <van-tag v-else-if="userStore.isAdmin" type="success" effect="dark">ADMIN</van-tag>
        <van-tag v-else effect="plain">USER</van-tag>
      </div>

      <van-cell-group inset>
        <van-cell title="租户" :value="tenantLabel" />
        <van-cell title="邮箱" :value="userStore.profile?.email || '—'" />
        <van-cell title="ID" :value="String(userStore.profile?.id || '—')" />
      </van-cell-group>

      <van-cell-group inset>
        <van-cell title="🌗 切换桌面版" is-link @click="goDesktop" />
        <van-cell title="📋 API 文档" is-link @click="showApi" />
        <van-cell title="ℹ️ 关于" is-link @click="showAbout = true" />
      </van-cell-group>

      <van-popup v-model:show="showAbout" position="bottom" round :style="{ height: '60%' }">
        <div class="about">
          <h2>🚀 MiniMax Platform</h2>
          <p>大模型企业级平台 v2.0</p>
          <p>移动端: V3.0 H5 适配</p>
          <p>GitHub: github.com/liugl951127/miniLiugl</p>
          <van-divider />
          <p style="color:#909399;font-size:12px">
            12 微服务 · 116+ 端点 · 135 测试
          </p>
        </div>
      </van-popup>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const router = useRouter()
const showAbout = ref(false)

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
</script>

<style scoped>
.m-me { min-height: 100vh; background: #f5f7fa; }
.content { padding: 50px 0 60px; }
.profile {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 32px 16px 24px;
  text-align: center;
}
.profile h2 { margin: 8px 0 4px; font-size: 20px; }
.profile p { margin: 0 0 8px; opacity: 0.9; }
.about { padding: 24px 16px; }
.about h2 { margin: 0 0 12px; }
.about p { margin: 6px 0; color: #606266; font-size: 14px; }
</style>
