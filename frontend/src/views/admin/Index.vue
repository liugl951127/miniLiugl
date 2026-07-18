<!--
  Admin 后台容器 (V5.24)
  - 提供侧边栏子导航 + 主内容 router-view
  - 子页: Dashboard / Metrics / Traces / Monitor / Provider / Leaderboard / ApiKeyStats
-->
<!--
  @file views/admin/Index.vue (入口/列表)
  @version V3.5.12+ (前端注释补全)
  @description 入口/列表
-->
<template>
  <div class="admin-container">
    <el-aside width="220px" class="admin-aside">
      <div class="aside-header">
        <el-icon :size="24" color="#5b8def"><Setting /></el-icon>
        <span>系统管理</span>
      </div>
      <el-menu :default-active="activeMenu" router class="admin-menu">
        <el-menu-item index="/admin">
          <el-icon><DataLine /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/admin/metrics">
          <el-icon><TrendCharts /></el-icon>
          <span>实时指标 <el-tag size="small" type="info">V5.10</el-tag></span>
        </el-menu-item>
        <el-menu-item index="/admin/traces">
          <el-icon><Connection /></el-icon>
          <span>分布式追踪 <el-tag size="small" type="info">V5.14</el-tag></span>
        </el-menu-item>
        <el-menu-item index="/admin/monitor">
          <el-icon><Monitor /></el-icon>
          <span>系统监控</span>
        </el-menu-item>
        <el-menu-item index="/admin/provider">
          <el-icon><Cpu /></el-icon>
          <span>模型 Provider <el-tag size="small" type="success">V5.24</el-tag></span>
        </el-menu-item>
        <el-menu-item index="/admin/leaderboard">
          <el-icon><Trophy /></el-icon>
          <span>模型排行榜 <el-tag size="small" type="success">V5.24</el-tag></span>
        </el-menu-item>
        <el-menu-item index="/admin/apikey-stats">
          <el-icon><Key /></el-icon>
          <span>API Key 配额 <el-tag size="small" type="success">Day 20</el-tag></span>
        </el-menu-item>
        <el-menu-item index="/admin/wechat">
          <el-icon><ChatDotRound /></el-icon>
          <span>微信绑定管理</span>
        </el-menu-item>
      </el-menu>

      <div class="aside-footer">
        <el-alert type="info" :closable="false" size="small">
          <template #title>
            <span style="font-size: 12px">快捷入口</span>
          </template>
          <div style="font-size: 12px; line-height: 1.6">
            • <router-link to="/knowledge">知识库</router-link><br>
            • <router-link to="/memory">记忆管理</router-link><br>
            • <router-link to="/prompts">Prompt 模板</router-link><br>
            • <router-link to="/profile/wechat">我的微信</router-link>
          </div>
        </el-alert>
      </div>
    </el-aside>

    <el-main class="admin-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Setting, DataLine, TrendCharts, Connection, Monitor, Cpu, Trophy, ChatDotRound } from '@element-plus/icons-vue'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped>
.admin-container {
  display: flex;
  height: calc(100vh - 60px);
  margin: -16px;
  background: #f5f7fa;
}
.admin-aside {
  background: #fff;
  border-right: 1px solid #e6e8eb;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.aside-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 20px 16px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #f0f2f5;
}
.admin-menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
}
.admin-menu :deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
}
.aside-footer {
  padding: 12px;
  border-top: 1px solid #f0f2f5;
}
.aside-footer a {
  color: #5b8def;
  text-decoration: none;
}
.aside-footer a:hover {
  text-decoration: underline;
}
.admin-main {
  padding: 20px;
  overflow-y: auto;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>