<!--
  个人中心 (V5)
  - 头像/昵称/邮箱
  - 我的微信 (集成 MyWechat)
  - 退出登录
-->
<template>
  <div class="profile-page">
    <header class="header">
      <h1>👤 个人中心</h1>
    </header>

    <el-row :gutter="20">
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" header="基本信息">
          <div class="user-info" v-if="profile">
            <el-avatar :size="64" :src="profile.avatar || ''">
              <el-icon><User /></el-icon>
            </el-avatar>
            <div class="meta">
              <h3>{{ profile.nickname }}</h3>
              <p class="username">@{{ profile.username }}</p>
              <p class="email">{{ profile.email || '未设置邮箱' }}</p>
              <p class="roles" v-if="profile.roles && profile.roles.length">
                <el-tag v-for="r in profile.roles" :key="r" size="small" style="margin-right: 4px">{{ r }}</el-tag>
                <el-tag v-if="profile.superAdmin" type="danger" size="small">SUPER_ADMIN</el-tag>
              </p>
            </div>
          </div>
          <el-skeleton v-else :rows="3" />
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card shadow="hover" header="微信绑定">
          <MyWechat />
        </el-card>
      </el-col>
    </el-row>

    <el-card class="actions-card" shadow="never">
      <template #header><span>⚙️ 操作</span></template>
      <el-space>
        <el-button type="warning" @click="changePassword">修改密码</el-button>
        <el-button @click="goWechatScan">扫码绑定</el-button>
        <el-button type="danger" @click="logout">退出登录</el-button>
      </el-space>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import http from '@/api/http'
import MyWechat from '@/components/MyWechat.vue'

const router = useRouter()
const userStore = useUserStore()
const profile = ref(null)

async function load() {
  try {
    const r = await http.get('/auth/me')
    profile.value = r.data || r
  } catch (e) {
    console.warn('加载用户信息失败:', e.message)
  }
}

async function changePassword() {
  ElMessage.info('修改密码 (生产场景)')
}

function goWechatScan() {
  router.push('/wechat')
}

async function logout() {
  try {
    await ElMessageBox.confirm('退出登录?', '确认', { type: 'warning' })
  } catch { return }
  userStore.logout()
  router.push('/login')
}

onMounted(load)
</script>

<style scoped>
.profile-page { max-width: 1200px; margin: 0 auto; padding: 24px; }
.header h1 { margin: 0 0 16px; font-size: 28px; }

.user-info { display: flex; gap: 16px; align-items: center; }
.meta h3 { margin: 0 0 4px; }
.meta p { margin: 4px 0; color: #64748b; font-size: 14px; }
.username { color: #94a3b8; font-size: 12px !important; }

.actions-card { margin-top: 20px; }
</style>