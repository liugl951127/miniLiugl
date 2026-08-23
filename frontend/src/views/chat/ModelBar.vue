<!--
  @file chat/ModelBar.vue - 模型选择栏 (V7.8)
-->
<template>
  <div class="model-bar">
    <el-tooltip content="切换对话使用的 AI 模型" placement="bottom">
      <el-select
        v-model="selectedModel"
        placeholder="选择模型"
        style="min-width: 240px"
      >
        <el-option-group label="自研模型">
          <el-option
            v-for="m in selfModels" :key="m.modelCode"
            :label="m.displayName || m.modelCode"
            :value="m.modelCode"
          >
            <span>{{ m.displayName || m.modelCode }}</span>
            <span v-if="m.accuracy" style="float:right;color:var(--el-color-success);font-size:11px">
              {{ (m.accuracy * 100).toFixed(0) }}%
            </span>
          </el-option>
        </el-option-group>
        <el-option-group label="ONNX 本地">
          <el-option
            v-for="m in onnxModels" :key="m.modelCode"
            :label="m.displayName || m.modelCode"
            :value="m.modelCode"
          />
        </el-option-group>
        <el-option-group label="云端模型">
          <el-option
            v-for="m in cloudModels" :key="m.modelCode"
            :label="m.displayName || m.modelCode"
            :value="m.modelCode"
          />
        </el-option-group>
      </el-select>
    </el-tooltip>

    <el-tooltip content="Agent 委托" placement="bottom">
      <el-select
        v-model="selectedAgent"
        placeholder="Agent 委托 (可选)"
        clearable
        style="min-width: 200px"
      >
        <el-option
          v-for="a in agents" :key="a.id"
          :label="a.name || a.id"
          :value="a.id"
        />
      </el-select>
    </el-tooltip>

    <el-tooltip content="清空当前对话" placement="bottom">
      <el-button :icon="DeleteIcon" size="small" @click="$emit('clear')">清空</el-button>
    </el-tooltip>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Delete as DeleteIcon } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  agentId: { type: [String, Number], default: null },
  selfModels: { type: Array, default: () => [] },
  onnxModels: { type: Array, default: () => [] },
  cloudModels: { type: Array, default: () => [] },
  agents: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue', 'update:agentId', 'clear'])

const selectedModel = ref(props.modelValue)
const selectedAgent = ref(props.agentId)

watch(() => props.modelValue, v => { selectedModel.value = v })
watch(() => props.agentId, v => { selectedAgent.value = v })
watch(selectedModel, v => emit('update:modelValue', v))
watch(selectedAgent, v => emit('update:agentId', v))
</script>

<style scoped>
.model-bar {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 16px; border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
}
</style>
