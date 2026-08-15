<!--
  @file components/PageSkeleton.vue (V3.5.92+)
  通用骨架屏 - 用于页面加载 / 数据 fetch 等待状态

  使用:
    <PageSkeleton :rows="5" :show-header="true" />
-->
<template>
  <div class="page-skeleton">
    <!-- 1. 页面 header 骨架 -->
    <div v-if="showHeader" class="skeleton-header">
      <el-skeleton-item variant="h1" style="width: 30%; height: 28px;" />
      <el-skeleton-item variant="text" style="width: 50%; margin-top: 8px;" />
    </div>

    <!-- 2. 主体内容骨架 (N 行) -->
    <div class="skeleton-body">
      <el-skeleton :rows="rows" animated v-for="(group, gi) in groups" :key="gi">
        <template #template>
          <div v-for="r in rows" :key="r" class="skeleton-row">
            <el-skeleton-item variant="text" :style="{ width: 60 + (r * 5) + '%' }" />
          </div>
        </template>
      </el-skeleton>
    </div>
  </div>
</template>

<script setup>
defineProps({
  rows: { type: Number, default: 5 },
  showHeader: { type: Boolean, default: true },
  groups: { type: Number, default: 1 }
})
</script>

<style lang="scss" scoped>
.page-skeleton {
  padding: 24px 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.skeleton-header {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--liugl-border, #e2e8f0);
}

.skeleton-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-row {
  margin-bottom: 8px;
}
</style>
