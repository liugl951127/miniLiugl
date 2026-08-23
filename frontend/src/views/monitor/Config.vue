<!--
  @file monitor/Config.vue - 监控配置 (V7.6)
  路由: /monitor/config
  合并: 通知渠道 + 告警规则 (原 2 个 tab)
-->
<template>
  <div class="config-page">
    <el-tabs v-model="activeTab" class="config-tabs">
      <!-- 通知渠道 -->
      <el-tab-pane label="通知渠道" name="channels">
        <div class="toolbar">
          <el-button type="primary" :icon="Plus" @click="openChannelDialog()">新增渠道</el-button>
          <el-button :icon="Refresh" @click="loadChannels">刷新</el-button>
        </div>
        <el-table :data="channels" v-loading="loadingChannels" stripe>
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="type" label="类型" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ getChannelTypeLabel(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="enabled" label="启用" width="80">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" @change="toggleChannel(row)" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" link type="primary" @click="openChannelDialog(row)">编辑</el-button>
              <el-button size="small" link type="danger" @click="deleteChannel(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <EmptyState
          v-if="!loadingChannels && channels.length === 0"
          title="暂无通知渠道"
          description="添加钉钉/邮件/Webhook 等通知渠道"
          compact
        />
      </el-tab-pane>

      <!-- 告警规则 -->
      <el-tab-pane label="告警规则" name="rules">
        <div class="toolbar">
          <el-button type="primary" :icon="Plus" @click="openRuleDialog()">新增规则</el-button>
          <el-button :icon="Refresh" @click="loadRules">刷新</el-button>
        </div>
        <el-table :data="rules" v-loading="loadingRules" stripe>
          <el-table-column prop="name" label="规则名" />
          <el-table-column prop="metric" label="指标" width="140" />
          <el-table-column label="条件" width="160">
            <template #default="{ row }">{{ row.operator }} {{ row.threshold }}</template>
          </el-table-column>
          <el-table-column prop="level" label="级别" width="100">
            <template #default="{ row }">
              <el-tag :type="getLevelType(row.level)" size="small">{{ getLevelLabel(row.level) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="enabled" label="启用" width="80">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" @change="toggleRule(row)" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" link type="primary" @click="openRuleDialog(row)">编辑</el-button>
              <el-button size="small" link type="danger" @click="deleteRule(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <EmptyState
          v-if="!loadingRules && rules.length === 0"
          title="暂无告警规则"
          description="配置触发条件, 自动检测异常"
          compact
        />
      </el-tab-pane>
    </el-tabs>

    <!-- 渠道编辑对话框 -->
    <el-dialog v-model="channelDialog.visible" :title="channelDialog.id ? '编辑渠道' : '新增渠道'" width="500px">
      <el-form :model="channelDialog.form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="channelDialog.form.name" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="channelDialog.form.type">
            <el-option label="钉钉" value="dingtalk" />
            <el-option label="飞书" value="feishu" />
            <el-option label="企业微信" value="wechat_work" />
            <el-option label="邮件" value="email" />
            <el-option label="Webhook" value="webhook" />
          </el-select>
        </el-form-item>
        <el-form-item label="配置">
          <el-input v-model="channelDialog.form.config" type="textarea" :rows="4" placeholder='{"webhook": "https://..."}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="channelDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveChannel">保存</el-button>
      </template>
    </el-dialog>

    <!-- 规则编辑对话框 -->
    <el-dialog v-model="ruleDialog.visible" :title="ruleDialog.id ? '编辑规则' : '新增规则'" width="500px">
      <el-form :model="ruleDialog.form" label-width="80px">
        <el-form-item label="规则名">
          <el-input v-model="ruleDialog.form.name" />
        </el-form-item>
        <el-form-item label="指标">
          <el-select v-model="ruleDialog.form.metric">
            <el-option label="CPU 使用率" value="cpu" />
            <el-option label="内存使用率" value="memory" />
            <el-option label="磁盘使用率" value="disk" />
            <el-option label="JVM 堆" value="jvm_heap" />
            <el-option label="响应时间" value="rt" />
            <el-option label="错误率" value="error_rate" />
          </el-select>
        </el-form-item>
        <el-form-item label="条件">
          <el-select v-model="ruleDialog.form.operator" style="width: 100px">
            <el-option v-for="op in ['>', '>=', '<', '<=', '==']" :key="op" :label="op" :value="op" />
          </el-select>
          <el-input-number v-model="ruleDialog.form.threshold" :min="0" style="margin-left: 8px" />
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="ruleDialog.form.level">
            <el-option label="严重" value="critical" />
            <el-option label="警告" value="warning" />
            <el-option label="信息" value="info" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import { monitorApi } from '@/api/monitor'

const activeTab = ref('channels')

const loadingChannels = ref(false)
const loadingRules = ref(false)

const channels = ref([])
const rules = ref([])

const channelDialog = reactive({ visible: false, id: null, form: { name: '', type: 'dingtalk', config: '' } })
const ruleDialog = reactive({ visible: false, id: null, form: { name: '', metric: 'cpu', operator: '>', threshold: 80, level: 'warning' } })

function getLevelType(level) {
  return { critical: 'danger', warning: 'warning', info: 'info' }[level] || 'info'
}
function getLevelLabel(level) {
  return { critical: '严重', warning: '警告', info: '信息' }[level] || level
}
function getChannelTypeLabel(type) {
  return { dingtalk: '钉钉', feishu: '飞书', wechat_work: '企业微信', email: '邮件', webhook: 'Webhook' }[type] || type
}

async function loadChannels() {
  loadingChannels.value = true
  try {
    const res = await monitorApi.listChannels()
    if (res.code === 0) channels.value = res.data?.list || res.data || []
  } catch (e) { ElMessage.error('加载渠道失败') }
  finally { loadingChannels.value = false }
}

async function loadRules() {
  loadingRules.value = true
  try {
    const res = await monitorApi.listAlertRules()
    if (res.code === 0) rules.value = res.data?.list || res.data || []
  } catch (e) { ElMessage.error('加载规则失败') }
  finally { loadingRules.value = false }
}

function openChannelDialog(row) {
  channelDialog.id = row?.id || null
  channelDialog.form = row
    ? { ...row }
    : { name: '', type: 'dingtalk', config: '' }
  channelDialog.visible = true
}

async function saveChannel() {
  try {
    if (channelDialog.id) {
      await monitorApi.updateChannel(channelDialog.id, channelDialog.form)
    } else {
      await monitorApi.createChannel(channelDialog.form)
    }
    ElMessage.success('保存成功')
    channelDialog.visible = false
    loadChannels()
  } catch (e) { ElMessage.error('保存失败: ' + e.message) }
}

async function deleteChannel(row) {
  try {
    await ElMessageBox.confirm(`确定删除渠道「${row.name}」?`, '提示', { type: 'warning' })
    await monitorApi.deleteChannel(row.id)
    ElMessage.success('已删除')
    loadChannels()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

async function toggleChannel(row) {
  try {
    await monitorApi.updateChannel(row.id, row)
  } catch (e) {
    row.enabled = !row.enabled
    ElMessage.error('更新失败')
  }
}

function openRuleDialog(row) {
  ruleDialog.id = row?.id || null
  ruleDialog.form = row
    ? { ...row }
    : { name: '', metric: 'cpu', operator: '>', threshold: 80, level: 'warning' }
  ruleDialog.visible = true
}

async function saveRule() {
  try {
    if (ruleDialog.id) {
      await monitorApi.updateAlertRule(ruleDialog.id, ruleDialog.form)
    } else {
      await monitorApi.createAlertRule(ruleDialog.form)
    }
    ElMessage.success('保存成功')
    ruleDialog.visible = false
    loadRules()
  } catch (e) { ElMessage.error('保存失败: ' + e.message) }
}

async function deleteRule(row) {
  try {
    await ElMessageBox.confirm(`确定删除规则「${row.name}」?`, '提示', { type: 'warning' })
    await monitorApi.deleteAlertRule(row.id)
    ElMessage.success('已删除')
    loadRules()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

async function toggleRule(row) {
  try {
    await monitorApi.updateAlertRule(row.id, row)
  } catch (e) {
    row.enabled = !row.enabled
    ElMessage.error('更新失败')
  }
}

onMounted(() => {
  loadChannels()
  loadRules()
})
</script>

<style scoped>
.config-page { background: white; border-radius: 12px; padding: 16px; }
.config-tabs { background: transparent; }
.toolbar { display: flex; gap: 8px; margin-bottom: 12px; }
</style>
