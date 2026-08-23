<!--
  @file knowledge/Memory.vue - 记忆中心页 (V7.6 from Index.vue 152-215 提取)
  路由: /knowledge/memory
-->
<template>
  <div class="memory-page">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="记忆中心由 Agent 自动维护"
      description="数据存储在 minimax-chat.memory_long_term 表中 (用户级长期记忆 + 会话上下文)。
                   通过聊天会话中的 Agent 工具 (LongTermMemoryService) 写入, 可通过 API 可见。
                   本页面提供只读入口已规划到下个版本。"
      class="intro-alert"
    />

    <el-row :gutter="16">
      <el-col :span="8" v-for="card in cards" :key="card.icon">
        <el-card shadow="hover" class="feature-card">
          <div class="card-icon">{{ card.icon }}</div>
          <div class="card-title">{{ card.title }}</div>
          <div class="card-desc">{{ card.desc }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="access-card">
      <template #header>
        <span class="card-header">📡 数据访问入口</span>
      </template>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="存储表">
          <code>minimax-chat.memory_long_term</code>
        </el-descriptions-item>
        <el-descriptions-item label="服务层">
          <code>LongTermMemoryService</code> (在 <code>minimax-chat</code> 服务)
        </el-descriptions-item>
        <el-descriptions-item label="API (规划中)">
          <code>GET /api/v1/memory/long-term</code> &nbsp;·&nbsp;
          <code>DELETE /api/v1/memory/long-term/{id}</code>
        </el-descriptions-item>
        <el-descriptions-item label="写入触发">
          Agent 对话中调用工具 <code>memory_write</code> / <code>memory_recall</code>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
const cards = [
  { icon: '🧠', title: '自动记忆', desc: 'Agent 在多轮对话中自动提取关键事实/偏好' },
  { icon: '🔍', title: '跨会话恢复', desc: '新会话开启时, Agent 自动加载相关历史记忆' },
  { icon: '📊', title: '数据可观测', desc: '通过 API (memory_long_term) 检索每条记忆的来源与置信度' }
]
</script>

<style scoped>
.memory-page { padding: 0; }
.intro-alert { margin-bottom: 16px; }
.feature-card { text-align: center; padding: 24px 0; }
.card-icon { font-size: 32px; }
.card-title { font-weight: 600; margin: 8px 0 4px; color: #1e293b; }
.card-desc { font-size: 12px; color: #64748b; line-height: 1.5; }
.access-card { margin-top: 16px; }
.card-header { font-size: 14px; font-weight: 600; }
</style>
