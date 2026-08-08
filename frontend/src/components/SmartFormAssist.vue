<!--
  V6.3+ 智能填单助手组件
  - 提供"✨ 智能填充"和"👁 一键预览"按钮
  - 调用 /api/v1/ai/autofill 推荐字段值
  - 显示 AI 推荐标签, 可接受/拒绝
-->
<template>
  <div class="smart-form-assist">
    <div class="assist-bar">
      <el-button type="primary" :icon="MagicStick" @click="onAutofill" :loading="loading" size="small">
        智能填充
      </el-button>
      <el-button :icon="View" @click="onPreview" size="small">一键预览</el-button>
      <span v-if="lastConfidence" class="confidence">
        置信度: {{ (lastConfidence * 100).toFixed(0) }}%
      </span>
    </div>
    <transition name="el-fade-in">
      <div v-if="recommendations" class="recommend-panel">
        <div class="recommend-header">
          <el-icon><MagicStick /></el-icon>
          <span>AI 推荐字段值</span>
          <el-button text @click="recommendations = null" :icon="Close" size="small" />
        </div>
        <div class="recommend-list">
          <div v-for="(value, key) in displayRecs" :key="key" class="recommend-item">
            <div class="rec-key">{{ key }}</div>
            <div class="rec-value">
              <code>{{ formatValue(value) }}</code>
            </div>
            <el-button type="primary" size="small" @click="applyOne(key, value)">应用</el-button>
          </div>
        </div>
        <div class="recommend-actions">
          <el-button type="primary" @click="applyAll" size="small">全部应用</el-button>
          <el-button @click="recommendations = null" size="small">取消</el-button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, View, Close } from '@element-plus/icons-vue'
import { autofill, previewForm } from '@/api/autofill'

const props = defineProps({
  formType: { type: String, required: true },  // user / apiKey / dataSource / pipeline / workflow
  context: { type: Object, default: () => ({}) },
  excludeFields: { type: Array, default: () => [] }  // 不显示推荐的字段 (如密码)
})

const emit = defineEmits(['apply'])

const loading = ref(false)
const recommendations = ref(null)
const lastConfidence = ref(0)

const displayRecs = computed(() => {
  if (!recommendations.value) return {}
  const result = {}
  for (const [k, v] of Object.entries(recommendations.value)) {
    if (k.startsWith('_')) continue  // 跳过元数据
    if (props.excludeFields.includes(k)) continue
    result[k] = v
  }
  return result
})

async function onAutofill() {
  loading.value = true
  try {
    const res = await autofill(props.formType, props.context)
    if (res?.data) {
      recommendations.value = res.data
      lastConfidence.value = res.data._confidence || 0
      ElMessage.success(`已生成 ${Object.keys(displayRecs.value).length} 个推荐字段`)
    }
  } catch (e) {
    ElMessage.warning('智能填充失败, 请手动填写')
    console.warn('autofill fail:', e.message)
  } finally {
    loading.value = false
  }
}

async function onPreview() {
  loading.value = true
  try {
    const res = await previewForm(props.formType)
    if (res?.data) {
      recommendations.value = res.data
      lastConfidence.value = 1.0
      ElMessage.success('已加载示例数据')
    }
  } catch (e) {
    ElMessage.warning('预览失败')
  } finally {
    loading.value = false
  }
}

function applyOne(key, value) {
  emit('apply', { [key]: value })
  ElMessage.success(`已应用: ${key}`)
}

function applyAll() {
  emit('apply', displayRecs.value)
  ElMessage.success('已应用全部推荐')
  recommendations.value = null
}

function formatValue(v) {
  if (v === null || v === undefined) return 'null'
  if (Array.isArray(v)) return `[${v.length} 项]`
  if (typeof v === 'object') return '{...}'
  if (typeof v === 'string' && v.length > 30) return v.slice(0, 30) + '...'
  return String(v)
}
</script>

<style lang="scss" scoped>
.smart-form-assist {
  margin-bottom: 16px;
}
.assist-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #faf5ff 0%, #f3e8ff 100%);
  border: 1px solid #e9d5ff;
  border-radius: 10px;
  
  .confidence {
    font-size: 12px;
    color: #7c3aed;
    margin-left: auto;
  }
}
.recommend-panel {
  margin-top: 12px;
  padding: 16px;
  background: #fff;
  border: 1px solid #e9d5ff;
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(168, 85, 247, 0.1);
}
.recommend-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #7c3aed;
  margin-bottom: 12px;
}
.recommend-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}
.recommend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #faf5ff;
  border-radius: 8px;
  
  .rec-key {
    font-weight: 500;
    color: #6b21a8;
    min-width: 100px;
  }
  .rec-value {
    flex: 1;
    code {
      font-size: 12px;
      color: #581c87;
      background: rgba(168, 85, 247, 0.1);
      padding: 2px 6px;
      border-radius: 4px;
    }
  }
}
.recommend-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
</style>
