<!--
  @file kg/KgStats.vue - 知识图谱统计卡片 (V8.0)
-->
<template>
  <el-row :gutter="12" style="margin-bottom:12px">
    <el-col :span="6">
      <el-card shadow="hover" body-style="text-align:center;padding:12px">
        <div style="font-size:24px;font-weight:700;color:var(--el-color-primary)">{{ stats.entities }}</div>
        <div style="font-size:12px;color:var(--el-text-color-secondary);margin-top:4px">实体数</div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover" body-style="text-align:center;padding:12px">
        <div style="font-size:24px;font-weight:700;color:var(--el-color-success)">{{ stats.edges }}</div>
        <div style="font-size:12px;color:var(--el-text-color-secondary);margin-top:4px">关系数</div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover" body-style="text-align:center;padding:12px">
        <div style="font-size:24px;font-weight:700;color:var(--el-color-warning)">{{ stats.types }}</div>
        <div style="font-size:12px;color:var(--el-text-color-secondary);margin-top:4px">类型数</div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover" body-style="text-align:center;padding:12px">
        <div style="font-size:24px;font-weight:700;color:var(--el-color-info)">{{ stats.queries || '-' }}</div>
        <div style="font-size:12px;color:var(--el-text-color-secondary);margin-top:4px">查询次数</div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
defineProps({
  stats: { type: Object, required: true }
})
</script>
