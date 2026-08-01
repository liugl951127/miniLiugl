<!--
  V3.6.10+ 统一空态组件
  替代 22 view 散乱的 <el-empty> 跟 <el-empty :description="...">
  支持自定义图标 / 标题 / 描述 / 操作按钮 / 演示模式
-->
<template>
  <div class="empty-state" :class="{ 'is-compact': compact }">
    <div class="empty-state-icon">
      <el-icon v-if="icon" :size="iconSize">
        <component :is="icon" />
      </el-icon>
      <div v-else class="empty-state-default-icon">📭</div>
    </div>
    <h3 v-if="title" class="empty-state-title">{{ title }}</h3>
    <p v-if="description" class="empty-state-desc">{{ description }}</p>
    <div v-if="$slots.default || actionText" class="empty-state-actions">
      <slot>
        <el-button v-if="actionText" type="primary" @click="$emit('action')">
          {{ actionText }}
        </el-button>
      </slot>
    </div>
    <div v-if="isDemo" class="empty-state-demo-hint">
      <el-icon><InfoFilled /></el-icon> 演示模式 - 暂无真实数据
    </div>
  </div>
</template>

<script setup>
import { InfoFilled } from '@element-plus/icons-vue'

defineProps({
  icon: { type: [String, Object], default: '' },
  iconSize: { type: Number, default: 64 },
  title: { type: String, default: '暂无数据' },
  description: { type: String, default: '' },
  actionText: { type: String, default: '' },
  compact: { type: Boolean, default: false },
  isDemo: { type: Boolean, default: false },
})

defineEmits(['action'])
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  min-height: 240px;
}
.empty-state.is-compact {
  min-height: 120px;
  padding: 20px;
}
.empty-state-icon {
  color: #cbd5e1;
  margin-bottom: 12px;
}
.empty-state-default-icon {
  font-size: 64px;
  line-height: 1;
}
.empty-state-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 500;
  color: #475569;
}
.empty-state-desc {
  margin: 0 0 16px;
  font-size: 13px;
  color: #94a3b8;
  max-width: 320px;
  line-height: 1.5;
}
.empty-state-actions {
  margin-top: 8px;
}
.empty-state-demo-hint {
  margin-top: 12px;
  padding: 4px 10px;
  background: #f1f5f9;
  border-radius: 4px;
  font-size: 12px;
  color: #64748b;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
</style>
