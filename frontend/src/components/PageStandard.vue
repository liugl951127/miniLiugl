<!--
  @file PageStandard.vue - V6.8.2+ 企业级标准页面
  @description 统一 87 个 view 的页面骨架:
    1. el-watermark (用户名 + 角色 + 时间)
    2. 页面头 (标题 + 副标题 + 操作区)
    3. 工具栏 (搜索 + 筛选 + 批量操作)
    4. 主体 (插槽, 默认放表格)
    5. 标签页 (可选)
    6. 分页 (可选, 配合 CrudTable)

  替代 87 view 重复的 30 行 page-header + 10 行 watermark 模板代码

  用法:
    <PageStandard title="用户管理" subtitle="系统用户与权限" :tabs="tabs" v-model:tab="tab">
      <template #actions>
        <el-button type="primary" @click="crud.openCreate">新建</el-button>
      </template>
      <template #toolbar>
        <SearchBar v-model="table.search.keyword" @search="table.searchBy" />
      </template>
      <CrudTable :table="table" :columns="columns" />
    </PageStandard>
-->
<template>
  <div class="page-standard" :class="`theme-${theme}`">
    <!-- 1. 增强水印 (用户名 + 角色 + 时间) -->
    <el-watermark
      v-if="watermark"
      :content="watermarkContent"
      :font="{ size: 12, color: 'rgba(99, 102, 241, 0.05)' }"
      :gap="[160, 100]"
      class="page-watermark"
    />

    <!-- 2. 页面头 -->
    <header v-if="title || $slots.header || $slots.actions" class="page-header">
      <div class="page-header-left">
        <h2 v-if="title" class="page-title">
          <span v-if="icon" class="page-icon">{{ icon }}</span>
          {{ title }}
        </h2>
        <p v-if="subtitle" class="page-subtitle">
          <slot name="subtitle">{{ subtitle }}</slot>
        </p>
        <slot name="header" />
      </div>
      <div v-if="$slots.actions" class="page-header-right">
        <slot name="actions" />
      </div>
    </header>

    <!-- 2b. 提示信息区 -->
    <section v-if="$slots.tips || tips" class="page-tips">
      <slot name="tips">
        <el-alert type="info" :closable="false" show-icon>
          {{ tips }}
        </el-alert>
      </slot>
    </section>

    <!-- 3. 标签页 -->
    <section v-if="tabs?.length" class="page-tabs">
      <el-tabs v-model="activeTab" @tab-change="$emit('tab-change', activeTab)">
        <el-tab-pane
          v-for="t in tabs"
          :key="t.name"
          :label="t.label"
          :name="t.name"
        >
          <template #label>
            <slot :name="`tab-${t.name}`" :tab="t">
              {{ t.label }}
              <el-badge v-if="t.badge" :value="t.badge" :max="99" :type="t.badgeType || 'primary'" />
            </slot>
          </template>
        </el-tab-pane>
      </el-tabs>
    </section>

    <!-- 4. 工具栏 -->
    <section v-if="$slots.toolbar || $slots['toolbar-left']" class="page-toolbar">
      <div class="page-toolbar-left">
        <slot name="toolbar-left" />
      </div>
      <div class="page-toolbar-right">
        <slot name="toolbar" />
      </div>
    </section>

    <!-- 5. 主体内容 -->
    <main class="page-body" :class="bodyClass">
      <slot />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/store/user'

defineOptions({ name: 'PageStandard' })

const props = defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  icon: { type: String, default: '' },
  tabs: { type: Array, default: () => [] },
  tab: { type: String, default: '' },
  theme: { type: String, default: 'default' }, // 'default' | 'compact' | 'card'
  bodyClass: { type: String, default: '' },
  watermark: { type: Boolean, default: true },
  watermarkText: { type: String, default: '' },
  tips: { type: String, default: '' },
})

const emit = defineEmits(['update:tab', 'tab-change'])

const userStore = useUserStore()

const activeTab = computed({
  get: () => props.tab,
  set: (v) => emit('update:tab', v),
})

const watermarkContent = computed(() => {
  if (props.watermarkText) {
    return [props.watermarkText]
  }
  return [
    'MiniMax AI V6.8.2',
    userStore.profile?.username || 'Guest',
    (userStore.profile?.roles || ['USER'])[0],
    new Date().toLocaleString('zh-CN'),
  ]
})
</script>

<style scoped>
.page-standard {
  position: relative;
  padding: 16px 20px;
  min-height: 100%;
}
.page-watermark {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
.page-header {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}
.page-header-left { flex: 1; min-width: 0; }
.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 8px;
}
.page-icon { font-size: 24px; }
.page-subtitle {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}
.page-header-right { display: flex; gap: 8px; flex-shrink: 0; }
.page-tips {
  position: relative;
  z-index: 1;
  margin-bottom: 12px;
}

.page-tabs {
  position: relative;
  z-index: 1;
  background: #fff;
  border-radius: 8px;
  padding: 0 16px;
  margin-bottom: 12px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}

.page-toolbar {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
  gap: 12px;
  flex-wrap: wrap;
}
.page-toolbar-left { display: flex; gap: 8px; align-items: center; flex: 1; min-width: 0; }
.page-toolbar-right { display: flex; gap: 8px; align-items: center; }

.page-body {
  position: relative;
  z-index: 1;
}

.theme-compact { padding: 8px 12px; }
.theme-card .page-body {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}
</style>
