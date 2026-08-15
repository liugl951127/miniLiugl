<template>
  <el-breadcrumb separator="/" class="app-breadcrumb">
    <transition-group name="breadcrumb">
      <el-breadcrumb-item v-for="(item, i) in items" :key="item.path">
        <span v-if="i === items.length - 1" class="current">
          <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
          {{ item.title }}
        </span>
        <a v-else @click.prevent="goTo(item)">
          <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
          {{ item.title }}
        </a>
      </el-breadcrumb-item>
    </transition-group>
  </el-breadcrumb>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { House } from '@element-plus/icons-vue'
import { useI18n } from '@/i18n'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const items = computed(() => {
  const list = [{ path: '/dashboard', title: t('nav.home'), icon: House }]
  if (route.meta?.title) {
    const parts = route.path.split('/').filter(Boolean)
    let path = ''
    for (const p of parts) {
      path += '/' + p
      const matched = router.getRoutes().find(r => r.path === path)
      if (matched?.meta?.title) {
        list.push({ path, title: matched.meta.title, icon: matched.meta.icon })
      }
    }
  }
  return list
})

function goTo(item) {
  router.push(item.path)
}
</script>

<style lang="scss" scoped>
.app-breadcrumb {
  display: inline-block;
  font-size: 14px;
  line-height: 32px;
  margin-left: 8px;
  
  :deep(a) {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: var(--el-text-color-secondary);
    text-decoration: none;
    transition: color 0.2s;
    
    &:hover {
      color: var(--el-color-primary);
    }
  }
  
  .current {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: var(--el-text-color-primary);
    font-weight: 600;
  }
}

.breadcrumb-enter-active,
.breadcrumb-leave-active {
  transition: all 0.3s;
}

.breadcrumb-enter-from,
.breadcrumb-leave-to {
  opacity: 0;
  transform: translateX(10px);
}

.breadcrumb-leave-active {
  position: absolute;
}
</style>
