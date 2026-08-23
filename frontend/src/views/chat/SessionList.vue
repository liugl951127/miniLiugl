<!--
  @file chat/SessionList.vue - 会话列表侧边栏 (V7.8)
-->
<template>
  <div class="session-list">
    <div class="session-header">
      <h3>💬 会话</h3>
      <el-button type="primary" size="small" :icon="Plus" @click="$emit('create')">新建</el-button>
    </div>
    <el-scrollbar>
      <el-empty
        v-if="!sessions.length"
        description="还没有会话"
        :image-size="80"
        style="padding: 20px 8px"
      >
        <el-button type="primary" size="small" @click="$emit('create')">创建第一个会话</el-button>
      </el-empty>
      <el-menu v-else :default-active="activeId" class="session-menu">
        <el-menu-item
          v-for="s in sessions" :key="s.id"
          @click="$emit('switch', s)"
        >
          <span class="session-title">{{ s.title || '新会话' }}</span>
          <el-tag v-if="s.kbId" size="small" type="success" style="margin-left:4px" title="知识库模式">📚</el-tag>
          <el-tag v-if="s.agentId" size="small" type="warning" style="margin-left:2px" title="Agent模式">🤖</el-tag>
          <el-tag size="small" type="info">{{ s.model || 'chat' }}</el-tag>
          <el-button :icon="EditPen" size="small" link style="margin-left:4px;padding:2px"
            title="重命名" @click.stop="$emit('rename', s)" />
          <el-button :icon="Delete" size="small" link style="padding:2px;color: var(--el-color-danger)"
            title="删除" @click.stop="$emit('remove', s)" />
        </el-menu-item>
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { Plus, EditPen, Delete } from '@element-plus/icons-vue'

defineProps({
  sessions: { type: Array, default: () => [] },
  activeId: { type: [String, Number], default: null }
})
defineEmits(['create', 'switch', 'rename', 'remove'])
</script>

<style scoped>
.session-list { display: flex; flex-direction: column; height: 100%; }
.session-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; border-bottom: 1px solid #e2e8f0;
}
.session-header h3 { margin: 0; font-size: 14px; }
.session-menu { border: none; }
.session-title { max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
