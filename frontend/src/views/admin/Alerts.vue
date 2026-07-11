<template>
  <div class="alerts">
    <el-card>
      <template #header>
        <div class="header">
          <span>🔔 告警中心</span>
          <el-button-group>
            <el-button :type="tab === 'firing' ? 'primary' : ''" @click="tab = 'firing'">触发中</el-button>
            <el-button :type="tab === 'rules' ? 'primary' : ''" @click="tab = 'rules'">告警规则</el-button>
            <el-button :type="tab === 'channels' ? 'primary' : ''" @click="tab = 'channels'">通知渠道</el-button>
            <el-button :type="tab === 'history' ? 'primary' : ''" @click="tab = 'history'">历史记录</el-button>
          </el-button-group>
        </div>
      </template>

      <!-- 触发中告警 -->
      <div v-if="tab === 'firing'">
        <div v-for="alert in firing" :key="alert.id" class="alert-card">
          <el-alert
            :type="severityType(alert.severity)"
            :title="alert.name + ' - ' + alert.message"
            show-icon
            :closable="false"
          />
          <div class="alert-meta">
            <span style="color: #999; font-size: 12px">
              触发时间: {{ alert.firedAt }} | 持续: {{ alert.duration }}
            </span>
            <el-button size="small" @click="acknowledge(alert)">确认</el-button>
          </div>
        </div>
        <el-empty v-if="!firing.length" description="没有触发中的告警" />
      </div>

      <!-- 告警规则 -->
      <div v-else-if="tab === 'rules'">
        <el-button type="primary" @click="newRule" style="margin-bottom: 12px">+ 新建规则</el-button>
        <el-table :data="rules" stripe>
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="metric" label="指标" width="160" />
          <el-table-column prop="operator" label="条件" width="100" />
          <el-table-column prop="threshold" label="阈值" width="100" />
          <el-table-column prop="severity" label="严重度" width="100">
            <template #default="scope">
              <el-tag :type="severityType(scope.row.severity)">{{ scope.row.severity }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="enabled" label="启用" width="80">
            <template #default="scope">
              <el-switch v-model="scope.row.enabled" @change="toggleRule(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="scope">
              <el-button size="small" @click="editRule(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteRule(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 通知渠道 -->
      <div v-else-if="tab === 'channels'">
        <el-button type="primary" @click="newChannel" style="margin-bottom: 12px">+ 新建渠道</el-button>
        <el-table :data="channels" stripe>
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="type" label="类型" width="120">
            <template #default="scope">
              <el-tag>{{ scope.row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="target" label="目标" />
          <el-table-column label="操作" width="160">
            <template #default="scope">
              <el-button size="small" @click="testChannel(scope.row)">测试</el-button>
              <el-button size="small" type="danger" @click="deleteChannel(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 历史 -->
      <div v-else-if="tab === 'history'">
        <el-table :data="history" stripe>
          <el-table-column prop="firedAt" label="时间" width="180" />
          <el-table-column prop="name" label="告警" />
          <el-table-column prop="severity" label="严重度" width="100">
            <template #default="scope">
              <el-tag :type="severityType(scope.row.severity)">{{ scope.row.severity }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column prop="duration" label="持续" width="100" />
        </el-table>
      </div>
    </el-card>

    <!-- 新建/编辑规则对话框 -->
    <el-dialog v-model="ruleDialogVisible" :title="editingRule.id ? '编辑规则' : '新建规则'" width="600px">
      <el-form :model="editingRule" label-width="100px">
        <el-form-item label="名称">
          <el-input v-model="editingRule.name" />
        </el-form-item>
        <el-form-item label="指标">
          <el-select v-model="editingRule.metric">
            <el-option label="CPU 使用率" value="cpu_usage" />
            <el-option label="内存使用率" value="memory_usage" />
            <el-option label="磁盘使用率" value="disk_usage" />
            <el-option label="API 错误率" value="api_error_rate" />
            <el-option label="响应时间" value="response_time" />
          </el-select>
        </el-form-item>
        <el-form-item label="条件">
          <el-select v-model="editingRule.operator">
            <el-option label=">" value=">" />
            <el-option label=">=" value=">=" />
            <el-option label="<" value="<" />
            <el-option label="<=" value="<=" />
            <el-option label="==" value="==" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值">
          <el-input-number v-model="editingRule.threshold" />
        </el-form-item>
        <el-form-item label="严重度">
          <el-select v-model="editingRule.severity">
            <el-option label="严重" value="critical" />
            <el-option label="警告" value="warning" />
            <el-option label="信息" value="info" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { monitorApi } from '@/api/monitor'

const tab = ref('firing')
const firing = ref([])
const rules = ref([])
const channels = ref([])
const history = ref([])
const ruleDialogVisible = ref(false)
const editingRule = ref({})

function severityType(s) {
  return { critical: 'danger', warning: 'warning', info: 'info' }[s] || ''
}

async function loadFiring() {
  try {
    const res = await monitorApi.getFiringAlerts()
    firing.value = res.data || []
  } catch (e) {
    // mock 数据
    firing.value = [
      { id: 1, name: 'CPU 高', message: 'CPU > 90% 持续 5 分钟', severity: 'critical', firedAt: '2026-07-12 02:30:00', duration: '15 分钟' },
      { id: 2, name: 'API 错误率', message: '错误率 > 5%', severity: 'warning', firedAt: '2026-07-12 02:45:00', duration: '2 分钟' }
    ]
  }
}

async function loadRules() {
  try {
    const res = await monitorApi.listAlertRules()
    rules.value = res.data || []
  } catch (e) {
    rules.value = [
      { id: 1, name: 'CPU 过高', metric: 'cpu_usage', operator: '>', threshold: 80, severity: 'warning', enabled: true },
      { id: 2, name: '内存满', metric: 'memory_usage', operator: '>', threshold: 90, severity: 'critical', enabled: true },
      { id: 3, name: 'API 慢', metric: 'response_time', operator: '>', threshold: 3000, severity: 'warning', enabled: false }
    ]
  }
}

async function loadChannels() {
  try {
    const res = await monitorApi.listAlertChannels()
    channels.value = res.data || []
  } catch (e) {
    channels.value = [
      { id: 1, name: '钉钉群', type: 'dingtalk', target: 'https://oapi.dingtalk.com/robot/send?access_token=xxx' },
      { id: 2, name: '邮件', type: 'email', target: 'ops@example.com' }
    ]
  }
}

async function loadHistory() {
  try {
    const res = await monitorApi.getAlertHistory()
    history.value = res.data || []
  } catch (e) {
    history.value = [
      { id: 1, firedAt: '2026-07-12 01:00:00', name: 'CPU 高', severity: 'critical', status: '已恢复', duration: '10 分钟' }
    ]
  }
}

function newRule() {
  editingRule.value = { name: '', metric: 'cpu_usage', operator: '>', threshold: 80, severity: 'warning', enabled: true }
  ruleDialogVisible.value = true
}

function editRule(rule) {
  editingRule.value = { ...rule }
  ruleDialogVisible.value = true
}

async function saveRule() {
  try {
    if (editingRule.value.id) {
      await monitorApi.updateAlertRule(editingRule.value.id, editingRule.value)
    } else {
      await monitorApi.createAlertRule(editingRule.value)
    }
    ElMessage.success('保存成功')
    ruleDialogVisible.value = false
    loadRules()
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  }
}

async function deleteRule(rule) {
  await ElMessageBox.confirm(`确定删除规则 "${rule.name}"?`, '确认')
  try {
    await monitorApi.deleteAlertRule(rule.id)
    ElMessage.success('已删除')
    loadRules()
  } catch (e) {
    ElMessage.error('删除失败: ' + e.message)
  }
}

async function toggleRule(rule) {
  try {
    await monitorApi.toggleAlertRule(rule.id, rule.enabled)
    ElMessage.success(rule.enabled ? '已启用' : '已禁用')
  } catch (e) {
    ElMessage.error('操作失败')
    rule.enabled = !rule.enabled
  }
}

async function acknowledge(alert) {
  try {
    await monitorApi.acknowledgeAlert(alert.id)
    ElMessage.success('已确认')
    loadFiring()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

function newChannel() {
  ElMessageBox.prompt('请输入渠道名称', '新建渠道').then(({ value }) => {
    channels.value.push({ id: Date.now(), name: value, type: 'webhook', target: '' })
  })
}

async function testChannel(ch) {
  try {
    await monitorApi.testAlertChannel(ch.id)
    ElMessage.success('测试消息已发送')
  } catch (e) {
    ElMessage.error('发送失败: ' + e.message)
  }
}

async function deleteChannel(ch) {
  await ElMessageBox.confirm(`确定删除渠道 "${ch.name}"?`, '确认')
  channels.value = channels.value.filter(c => c.id !== ch.id)
  ElMessage.success('已删除')
}

onMounted(() => {
  loadFiring()
  loadRules()
  loadChannels()
  loadHistory()
})
</script>

<style scoped>
.alerts {
  padding: 16px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.alert-card {
  margin-bottom: 12px;
  padding: 12px;
  background: #fafafa;
  border-radius: 4px;
}
.alert-meta {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
