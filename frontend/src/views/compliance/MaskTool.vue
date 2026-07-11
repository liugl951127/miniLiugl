<template>
  <div class="mask-tool">
    <el-card>
      <template #header>
        <div class="header">
          <span>🛡️ 数据脱敏预览工具</span>
          <el-tag type="success" size="small">个保法 + GDPR 合规</el-tag>
        </div>
      </template>

      <el-row :gutter="16">
        <el-col :span="12">
          <div class="label">📝 输入 (含敏感信息)</div>
          <el-input
            v-model="input"
            type="textarea"
            :rows="12"
            placeholder="粘贴任意含敏感信息的文本, 如: 用户张三 13812345678 邮箱 zhang@example.com 身份证 110101199001011234"
          />
          <div class="hint">支持脱敏: 手机号 / 身份证 / 邮箱 / 银行卡 / 内网IP / 姓名 / JWT / 密码字段</div>
        </el-col>
        <el-col :span="12">
          <div class="label">🔒 脱敏后 (安全)</div>
          <el-input
            v-model="output"
            type="textarea"
            :rows="12"
            readonly
            placeholder="脱敏结果会显示在这里"
          />
          <div class="stats">
            <el-tag v-if="containsMobile" type="danger">📱 手机号</el-tag>
            <el-tag v-if="containsIdCard" type="danger">🆔 身份证</el-tag>
            <el-tag v-if="containsEmail" type="warning">📧 邮箱</el-tag>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="16" style="margin-top: 16px">
        <el-col :span="12">
          <el-button type="primary" @click="doMask">🚀 脱敏</el-button>
          <el-button @click="loadExample">📋 示例</el-button>
          <el-button @click="clearAll">🗑 清空</el-button>
        </el-col>
        <el-col :span="12">
          <el-button @click="copyOutput" :disabled="!output">📋 复制结果</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>📚 脱敏规则说明</template>
      <el-table :data="rules" stripe size="small">
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="before" label="原始" width="220" />
        <el-table-column prop="after" label="脱敏后" width="200" />
        <el-table-column prop="rule" label="规则" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { maskText } from '@/api/ai'

const input = ref('')
const output = ref('')
const containsMobile = ref(false)
const containsIdCard = ref(false)
const containsEmail = ref(false)

const rules = [
  { type: '手机号', before: '13812345678', after: '138****5678', rule: '保留前 3 + 后 4' },
  { type: '身份证', before: '110101199001011234', after: '110**********1234', rule: '保留前 3 + 后 4' },
  { type: '邮箱', before: 'zhang.san@example.com', after: 'z*******@example.com', rule: '保留首字符' },
  { type: '银行卡', before: '6222021234567890', after: '6222 **** **** 7890', rule: '保留前 4 + 后 4' },
  { type: '内网IP', before: '192.168.1.100', after: '192.168.*.*', rule: '10/172.16/192.168 段脱敏' },
  { type: '公网IP', before: '8.8.8.8', after: '8.8.8.8', rule: '公网 IP 保留' },
  { type: '中文姓名', before: '张三先生', after: '张*先生', rule: '依赖上下文 (先生/女士)' },
  { type: 'JWT', before: 'eyJhbGciOiJIUzI1NiJ9.xxx.yyy', after: 'eyJ***.***.***', rule: '全部 token 替换' },
  { type: '密码字段', before: 'password=secret123', after: 'password=******', rule: '匹配 password/token/api_key 等' }
]

async function doMask() {
  if (!input.value.trim()) {
    ElMessage.warning('请输入文本')
    return
  }
  try {
    const res = await maskText(input.value)
    output.value = res.data.masked
    containsMobile.value = res.data.containsMobile === 'true'
    containsIdCard.value = res.data.containsIdCard === 'true'
    containsEmail.value = res.data.containsSensitive === 'true'
  } catch (e) {
    // 前端 fallback (直接调用 DataMasker 逻辑过于复杂, 提示用后端)
    ElMessage.error('脱敏失败: ' + e.message + ', 请确保后端 AI 服务启动')
  }
}

function loadExample() {
  input.value = `用户张三先生 联系电话: 13812345678
备用手机: 13987654321
邮箱: zhang.san@company.com
身份证: 110101199001011234
银行卡: 6222021234567890
服务器: 192.168.1.100 (内网)
密码: password=mySecret123
Token: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature`
}

function clearAll() {
  input.value = ''
  output.value = ''
  containsMobile.value = false
  containsIdCard.value = false
  containsEmail.value = false
}

async function copyOutput() {
  if (!output.value) return
  try {
    await navigator.clipboard.writeText(output.value)
    ElMessage.success('已复制到剪贴板')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}
</script>

<style scoped>
.mask-tool {
  padding: 16px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.label {
  font-weight: bold;
  margin-bottom: 8px;
  color: #555;
}
.hint {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
.stats {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}
</style>
