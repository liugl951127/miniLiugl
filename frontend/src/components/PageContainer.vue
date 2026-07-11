<template>
  <div class="page-container">
    <div class="page-header" v-if="title || $slots.header">
      <div class="page-header-left">
        <h2 v-if="title" class="page-title">
          <span v-if="icon" class="page-icon">{{ icon }}</span>
          {{ title }}
        </h2>
        <p v-if="subtitle" class="page-subtitle">{{ subtitle }}</p>
        <slot name="header"></slot>
      </div>
      <div class="page-header-right" v-if="$slots.actions">
        <slot name="actions"></slot>
      </div>
    </div>

    <el-breadcrumb v-if="breadcrumbs && breadcrumbs.length" separator="/" class="page-breadcrumb">
      <el-breadcrumb-item v-for="(b, i) in breadcrumbs" :key="i" :to="b.to">
        {{ b.label }}
      </el-breadcrumb-item>
    </el-breadcrumb>

    <div class="page-body">
      <slot></slot>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: String,
  subtitle: String,
  icon: String,
  breadcrumbs: { type: Array, default: () => [] }
})
</script>

<style scoped>
.page-container { padding: 16px 20px; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}
.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}
.page-icon { font-size: 24px; }
.page-subtitle { color: #909399; font-size: 13px; margin: 4px 0 0; }
.page-header-right { display: flex; gap: 8px; }
.page-breadcrumb { margin-bottom: 12px; font-size: 13px; }
.page-body { min-height: 200px; }
</style>
