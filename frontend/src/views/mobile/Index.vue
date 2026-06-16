<template>
  <div class="mobile-app">
    <!-- 顶部安全区 -->
    <div class="mobile-header">
      <div class="header-left">
        <el-avatar :size="32" style="background:#409eff">
          {{ userStore.profile?.nickname?.[0] || 'U' }}
        </el-avatar>
        <div class="header-info">
          <div class="user-name">
            {{ userStore.profile?.nickname || '未登录' }}
            <el-tag v-if="userStore.isSuperAdmin" type="danger" size="small" effect="dark">👑</el-tag>
          </div>
          <div class="tenant-info">
            {{ tenantLabel }}
          </div>
        </div>
      </div>
      <el-button text @click="logout" size="small">
        <el-icon><SwitchButton /></el-icon>
      </el-button>
    </div>

    <!-- 页面内容 -->
    <div class="mobile-content">
      <router-view v-slot="{ Component }">
        <transition name="slide" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>

    <!-- 底部 Tab 栏 -->
    <van-tabbar v-model="activeTab" route safe-area-inset-bottom>
      <van-tabbar-item to="/m/chat" icon="comment-o">对话</van-tabbar-item>
      <van-tabbar-item to="/m/agent" icon="aiming">Agent</van-tabbar-item>
      <van-tabbar-item to="/m/kg" icon="cluster-o">图谱</van-tabbar-item>
      <van-tabbar-item to="/m/plugins" icon="apps-o">插件</van-tabbar-item>
      <van-tabbar-item to="/m/me" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const router = useRouter()
const activeTab = ref(0)

const tenantLabel = computed(() => {
  if (userStore.isSuperAdmin) return '👑 平台所有者'
  return '租户: default'
})

async function logout() {
  try {
    await ElMessageBox.confirm('确认退出登录?', '提示', { type: 'warning' })
    await userStore.logout()
    ElMessage.success('已退出')
    router.push('/login')
  } catch (e) {
    // cancel
  }
}
</script>

<style scoped>
.mobile-app {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f7fa;
  max-width: 480px;
  margin: 0 auto;
  position: relative;
}
.mobile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: white;
  border-bottom: 1px solid #ebeef5;
  padding-top: env(safe-area-inset-top);
}
.header-left { display: flex; align-items: center; gap: 10px; }
.header-info { display: flex; flex-direction: column; }
.user-name { font-size: 14px; font-weight: 600; color: #303133; }
.tenant-info { font-size: 11px; color: #909399; }
.mobile-content {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 60px; /* 给 tabbar 留位置 */
  -webkit-overflow-scrolling: touch;
}
.slide-enter-active, .slide-leave-active {
  transition: transform 0.2s, opacity 0.2s;
}
.slide-enter-from { transform: translateX(100%); opacity: 0; }
.slide-leave-to { transform: translateX(-100%); opacity: 0; }

@media (min-width: 768px) {
  .mobile-app {
    border-left: 1px solid #ebeef5;
    border-right: 1px solid #ebeef5;
    box-shadow: 0 0 20px rgba(0,0,0,0.05);
  }
}
</style>
