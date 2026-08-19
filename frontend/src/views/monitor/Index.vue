<!-- @file monitor/Index.vue - 系统监控 V6.8 + Day 41 告警详情 -->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>系统监控</h2>
      <el-button size="small" @click="loadData">
        <el-icon><Refresh /></el-icon>刷新
      </el-button>
    </div>

    <el-row :gutter="12" style="margin-bottom:16px">
      <el-col v-for="s in services" :key="s.name" :span="6">
        <el-card shadow="hover">
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span style="font-weight:600">{{ s.name }}</span>
            <el-tag size="small" :type="s.status === 'UP' ? 'success' : 'danger'">{{ s.status }}</el-tag>
          </div>
          <div style="margin-top:8px;font-size:12px;color:#666">
            延迟: <span :style="{color: s.latency > 1000 ? '#ef4444' : '#10b981'}">{{ s.latency }}ms</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="JVM 指标" name="jvm">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="堆内存使用">{{ jvm.heapUsed }}/{{ jvm.heapMax }}</el-descriptions-item>
          <el-descriptions-item label="非堆内存">{{ jvm.nonHeapUsed }}</el-descriptions-item>
          <el-descriptions-item label="GC 次数">{{ jvm.gcCount }}</el-descriptions-item>
          <el-descriptions-item label="线程数">{{ jvm.threads }}</el-descriptions-item>
          <el-descriptions-item label="Uptime">{{ jvm.uptime }}</el-descriptions-item>
          <el-descriptions-item label="CPU">{{ jvm.cpuUsage }}</el-descriptions-item>
        </el-descriptions>
        <!-- P1-5: JVM堆内存趋势图 -->
        <div ref="jvmChartRef" style="height:240px;margin-top:16px"></div>
      </el-tab-pane>

      <el-tab-pane label="告警历史" name="alerts">
        <el-table :data="alerts" stripe size="small" @row-click="openAlertDetail" style="cursor:pointer">
          <el-table-column prop="firedAt" label="触发时间" width="170">
            <template #default="{ row }">{{ formatTime(row.firedAt) }}</template>
          </el-table-column>
          <el-table-column prop="severity" label="级别" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="severityType(row.severity)">{{ row.severity }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="ruleName" label="规则" width="150" />
          <el-table-column prop="metricName" label="指标" width="130" />
          <el-table-column prop="metricValue" label="当前值" width="100">
            <template #default="{ row }">{{ row.metricValue }}</template>
          </el-table-column>
          <el-table-column prop="threshold" label="阈值" width="100" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="告警信息" show-overflow-tooltip />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.sessionId" size="small" type="success" @click.stop="jumpToChat(row)">
                💬 跳转到对话
              </el-button>
              <el-button size="small" type="primary" @click.stop="openAlertDetail(row)">
                <el-icon><View /></el-icon>详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ========== 通知渠道管理 (Day 42) ========== -->
      <el-tab-pane label="通知渠道" name="channels">
        <div class="channel-toolbar">
          <el-button type="primary" size="small" @click="openChannelForm(null)">
            <el-icon><Plus /></el-icon>新建渠道
          </el-button>
          <el-button size="small" @click="loadChannels">
            <el-icon><Refresh /></el-icon>刷新
          </el-button>
        </div>

        <el-table :data="channels" stripe size="small" v-loading="channelsLoading" style="margin-top:12px">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="名称" width="160" />
          <el-table-column prop="channelType" label="类型" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="channelTypeTag(row.channelType)">
                {{ channelTypeLabel(row.channelType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="target" label="目标地址" min-width="200" show-overflow-tooltip />
          <el-table-column prop="priority" label="优先级" width="80" align="center">
            <template #default="{ row }">{{ row.priority ?? 1 }}</template>
          </el-table-column>
          <el-table-column prop="enabled" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.enabled ? 'success' : 'info'">
                {{ row.enabled ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-tooltip content="发送测试消息验证配置是否正确" placement="top">
                <el-button size="small" type="success" @click="testChannel(row)" :loading="testingId === row.id">
                  测试
                </el-button>
              </el-tooltip>
              <el-button size="small" type="primary" @click="openChannelForm(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteChannel(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="!channelsLoading && channels.length === 0" style="text-align:center;padding:40px;color:#909399">
          暂无通知渠道，点击「新建渠道」添加
        </div>
      </el-tab-pane>

      <!-- ========== 告警规则 (Day 45) ========== -->
      <el-tab-pane label="告警规则" name="rules">
        <div class="channel-toolbar">
          <el-button type="primary" size="small" @click="openRuleForm(null)">
            <el-icon><Plus /></el-icon>新建规则
          </el-button>
          <el-button size="small" @click="loadRules">
            <el-icon><Refresh /></el-icon>刷新
          </el-button>
        </div>

        <el-table :data="rules" stripe size="small" v-loading="rulesLoading" style="margin-top:12px">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="规则名称" width="160" />
          <el-table-column prop="metricName" label="指标" width="130" />
          <el-table-column prop="operator" label="条件" width="70" align="center">
            <template #default="{ row }"><code>{{ row.operator }}</code></template>
          </el-table-column>
          <el-table-column prop="threshold" label="阈值" width="80" align="center" />
          <el-table-column prop="severity" label="级别" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="severityType(row.severity)">{{ row.severity }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="升级策略" width="200">
            <template #default="{ row }">
              <span v-if="row.escalateAfterMinutes" style="font-size:12px;color:#e6a23c">
                ⏱ 超时 {{ row.escalateAfterMinutes }}min 升级
              </span>
              <span v-else style="font-size:12px;color:#909399">无</span>
            </template>
          </el-table-column>
          <el-table-column prop="enabled" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="openRuleForm(row)">
                <el-icon><Edit /></el-icon>编辑
              </el-button>
              <el-button size="small" :type="row.enabled ? 'warning' : 'success'" @click="toggleRule(row)">
                {{ row.enabled ? '禁用' : '启用' }}
              </el-button>
              <el-button size="small" type="danger" @click="deleteRule(row)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ========== 告警统计概览 (Day 47) ========== -->
      <el-tab-pane label="统计概览" name="stats">
        <div class="sla-toolbar">
          <el-select v-model="statsWindow" size="small" style="width:120px" @change="loadStats">
            <el-option label="近 7 天" :value="7" />
            <el-option label="近 30 天" :value="30" />
            <el-option label="近 90 天" :value="90" />
          </el-select>
          <el-button size="small" @click="loadStats">
            <el-icon><Refresh /></el-icon>刷新
          </el-button>
        </div>

        <!-- 核心指标卡片 -->
        <el-row :gutter="12" style="margin-top:12px" v-loading="statsLoading">
          <el-col :span="4">
            <el-card shadow="hover" body-style="text-align:center;padding:16px">
              <div style="font-size:12px;color:#909399;margin-bottom:8px">总告警数</div>
              <div style="font-size:32px;font-weight:700">{{ stats.total || 0 }}</div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card shadow="hover" body-style="text-align:center;padding:16px">
              <div style="font-size:12px;color:#909399;margin-bottom:8px">活跃</div>
              <div style="font-size:32px;font-weight:700;color:#f56c6c">{{ stats.active || 0 }}</div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card shadow="hover" body-style="text-align:center;padding:16px">
              <div style="font-size:12px;color:#909399;margin-bottom:8px">已恢复</div>
              <div style="font-size:32px;font-weight:700;color:#67c23a">{{ stats.resolved || 0 }}</div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card shadow="hover" body-style="text-align:center;padding:16px">
              <div style="font-size:12px;color:#909399;margin-bottom:8px">平均持续</div>
              <div style="font-size:24px;font-weight:700;color:#409eff">{{ stats.avgDurationMinutes != null ? stats.avgDurationMinutes.toFixed(1) + 'min' : '-' }}</div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card shadow="hover" body-style="text-align:center;padding:16px">
              <div style="font-size:12px;color:#909399;margin-bottom:8px">已确认</div>
              <div style="font-size:32px;font-weight:700;color:#e6a23c">{{ stats.acked || 0 }}</div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card shadow="hover" body-style="text-align:center;padding:16px">
              <div style="font-size:12px;color:#909399;margin-bottom:8px">进行中</div>
              <div style="font-size:32px;font-weight:700;color:#f56c6c">{{ stats.firing || 0 }}</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 按严重程度 + Top 规则 -->
        <el-row :gutter="12" style="margin-top:12px" v-loading="statsLoading">
          <el-col :span="12">
            <el-card shadow="hover" body-style="padding:16px">
              <template #header>
                <span>按严重程度分布</span>
                <el-button size="small" link type="primary" style="float:right" @click="exportStatsImg('pieChart')">导出图片</el-button>
              </template>
              <!-- ECharts 饼图 (Day 48) -->
              <div ref="pieChartRef" style="height:220px"></div>
              <!-- 备用描述 -->
              <el-descriptions :column="3" border style="margin-top:8px" v-if="stats.total > 0">
                <el-descriptions-item label="CRITICAL">
                  <el-tag type="danger" size="small">{{ stats.critical || 0 }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="WARNING">
                  <el-tag type="warning" size="small">{{ stats.warning || 0 }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="INFO">
                  <el-tag type="info" size="small">{{ stats.info || 0 }}</el-tag>
                </el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="hover" body-style="padding:16px">
              <template #header>
                <span>告警趋势（每日）</span>
                <el-button size="small" link type="primary" style="float:right" @click="exportStatsImg('barChart')">导出图片</el-button>
              </template>
              <!-- ECharts 柱状图 (Day 48) -->
              <div ref="barChartRef" style="height:220px"></div>
            </el-card>
          </el-col>
        </el-row>

        <div v-if="!statsLoading && stats.total === 0" style="text-align:center;padding:40px;color:#909399">
          暂无告警数据
        </div>
      </el-tab-pane>

      <!-- ========== SLA 统计 (Day 43) ========== -->
      <el-tab-pane label="SLA 统计" name="sla">
        <div class="sla-toolbar">
          <el-select v-model="slaWindow" size="small" style="width:120px" @change="loadSla">
            <el-option label="近 7 天" :value="7" />
            <el-option label="近 30 天" :value="30" />
            <el-option label="近 90 天" :value="90" />
          </el-select>
          <el-button size="small" @click="loadSla">
            <el-icon><Refresh /></el-icon>刷新
          </el-button>
        </div>

        <!-- SLA 指标卡片 -->
        <el-row :gutter="12" style="margin-top:12px" v-loading="slaLoading">
          <el-col :span="6">
            <el-card shadow="hover" body-style="text-align:center;padding:16px">
              <div style="font-size:12px;color:#909399;margin-bottom:8px">SLA 等级</div>
              <div style="font-size:36px;font-weight:700" :style="{ color: slaGradeColor }">{{ sla.grade || '-' }}</div>
              <div style="font-size:12px;color:#909399;margin-top:4px">近 {{ sla.windowDays || 0 }} 天</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" body-style="text-align:center;padding:16px">
              <div style="font-size:12px;color:#909399;margin-bottom:8px">可用率</div>
              <div style="font-size:28px;font-weight:700;color:#67c23a">{{ sla.availabilityPct != null ? sla.availabilityPct.toFixed(4) + '%' : '-' }}</div>
              <el-progress :percentage="Math.min(100, (sla.availabilityPct || 0))" :color="availabilityColor" style="margin-top:8px" :show-text="false" />
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" body-style="text-align:center;padding:16px">
              <div style="font-size:12px;color:#909399;margin-bottom:8px">MTBF</div>
              <div style="font-size:28px;font-weight:700;color:#409eff">{{ sla.mtbfHours != null ? sla.mtbfHours + 'h' : '-' }}</div>
              <div style="font-size:12px;color:#909399;margin-top:4px">平均故障间隔</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" body-style="text-align:center;padding:16px">
              <div style="font-size:12px;color:#909399;margin-bottom:8px">MTTR</div>
              <div style="font-size:28px;font-weight:700;color:#e6a23c">{{ sla.mttrMinutes != null ? sla.mttrMinutes + 'min' : '-' }}</div>
              <div style="font-size:12px;color:#909399;margin-top:4px">平均恢复时间</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 告警统计概览 -->
        <el-row :gutter="12" style="margin-top:12px" v-loading="slaLoading">
          <el-col :span="8">
            <el-card shadow="hover" body-style="text-align:center;padding:16px">
              <div style="font-size:12px;color:#909399;margin-bottom:8px">总告警数</div>
              <div style="font-size:28px;font-weight:700">{{ sla.totalAlerts || 0 }}</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover" body-style="text-align:center;padding:16px">
              <div style="font-size:12px;color:#909399;margin-bottom:8px">活跃告警</div>
              <div style="font-size:28px;font-weight:700;color:#f56c6c">{{ sla.activeAlerts || 0 }}</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover" body-style="text-align:center;padding:16px">
              <div style="font-size:12px;color:#909399;margin-bottom:8px">已恢复</div>
              <div style="font-size:28px;font-weight:700;color:#67c23a">{{ sla.resolvedAlerts || 0 }}</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 按严重程度统计 -->
        <el-card shadow="hover" style="margin-top:12px" v-loading="slaLoading" body-style="padding:16px">
          <template #header><span>按严重程度分布</span></template>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="CRITICAL">
              <el-tag type="danger" size="small">{{ sla.severity?.CRITICAL || 0 }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="WARNING">
              <el-tag type="warning" size="small">{{ sla.severity?.WARNING || 0 }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="INFO">
              <el-tag type="info" size="small">{{ sla.severity?.INFO || 0 }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <div v-if="!slaLoading && sla.totalAlerts === 0" style="text-align:center;padding:40px;color:#909399">
          暂无告警数据，无法计算 SLA
        </div>
      </el-tab-pane>

      <!-- ========== 告警趋势 (Day 44) ========== -->
      <el-tab-pane label="告警趋势" name="trend">
        <div style="display:flex;align-items:center;gap:12px;margin-bottom:12px">
          <span style="font-size:13px;color:#606266">时间范围：</span>
          <el-radio-group v-model="trendDays" size="small" @change="loadAlertTrend">
            <el-radio-button value="7">近7天</el-radio-button>
            <el-radio-button value="14">近14天</el-radio-button>
            <el-radio-button value="30">近30天</el-radio-button>
          </el-radio-group>
        </div>
        <div ref="trendChartRef" style="height:320px" v-loading="trendLoading"></div>
        <el-row :gutter="12" style="margin-top:16px" v-loading="trendLoading">
          <el-col :span="6">
            <el-statistic title="告警总数" :value="trendTotal" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="日均告警" :value="trendAvg" :precision="1" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="最高单日" :value="trendMax" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="峰值日期" :value="trendMaxDate || '-'" />
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>

    <!-- ========== 告警详情弹窗 (Day 41) ========== -->
    <el-dialog v-model="detailVisible" title="告警详情" width="680px" destroy-on-close>
      <div v-if="detailLoading" style="text-align:center;padding:40px">
        <el-icon class="is-loading" style="font-size:32px;color:#409eff"><Loading /></el-icon>
        <p style="color:#909399;margin-top:12px">正在加载 RCA 分析...</p>
      </div>

      <div v-else-if="alertDetail">
        <!-- 基础信息 -->
        <el-descriptions :column="2" border style="margin-bottom:16px">
          <el-descriptions-item label="告警 ID">{{ alertDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="statusType(alertDetail.status)">{{ statusLabel(alertDetail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="规则名称">{{ alertDetail.ruleName }}</el-descriptions-item>
          <el-descriptions-item label="触发时间">{{ formatTime(alertDetail.firedAt) }}</el-descriptions-item>
          <el-descriptions-item label="指标名称">{{ alertDetail.metricName }}</el-descriptions-item>
          <el-descriptions-item label="持续时间">{{ alertDetail.duration ? (alertDetail.duration + 's') : '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前值">{{ alertDetail.metricValue }}</el-descriptions-item>
          <el-descriptions-item label="告警阈值">{{ alertDetail.threshold }}</el-descriptions-item>
          <el-descriptions-item label="级别" :span="2">
            <el-tag size="small" :type="severityType(alertDetail.severity)">{{ alertDetail.severity }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="告警信息" :span="2">{{ alertDetail.message }}</el-descriptions-item>
        </el-descriptions>

        <!-- RCA 根因分析 (Day 41) -->
        <div class="rca-section">
          <div class="rca-header">
            <span>🔍 RCA 根因分析</span>
            <el-tag v-if="alertDetail.rca" size="small" type="info">
              置信度: {{ ((alertDetail.rca.confidence || 0) * 100).toFixed(0) }}%
            </el-tag>
          </div>
          <div v-if="alertDetail.rca">
            <div class="rca-category">
              <span class="rca-label">根因类别</span>
              <el-tag size="small" type="warning">{{ alertDetail.rca.category || 'UNKNOWN' }}</el-tag>
            </div>
            <div class="rca-cause">
              <div class="rca-label">可能原因</div>
              <div class="rca-content">{{ alertDetail.rca.rootCause || '分析中...' }}</div>
            </div>
            <div v-if="alertDetail.rca.suggestions?.length" class="rca-suggestions">
              <div class="rca-label">建议操作</div>
              <ul>
                <li v-for="(s, i) in alertDetail.rca.suggestions" :key="i">{{ s }}</li>
              </ul>
            </div>
          </div>
          <div v-else style="color:#909399;font-size:13px;padding:8px 0">
            暂无 RCA 分析结果
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="acknowledgeAlertFromDetail" :loading="acking">
          确认告警
        </el-button>
      </template>
    </el-dialog>

    <!-- ========== 通知渠道编辑弹窗 (Day 42) ========== -->
    <el-dialog v-model="channelFormVisible" :title="channelFormMode === 'edit' ? '编辑渠道' : '新建渠道'" width="520px" destroy-on-close>
      <el-form :model="channelForm" label-width="90px">
        <el-form-item label="渠道名称" required>
          <el-input v-model="channelForm.name" placeholder="例如：生产告警-钉钉" />
        </el-form-item>
        <el-form-item label="渠道类型" required>
          <el-select v-model="channelForm.channelType" placeholder="选择类型" style="width:100%">
            <el-option label="📧 邮件 (SMTP)" value="email" />
            <el-option label="🔔 钉钉 Webhook" value="dingtalk" />
            <el-option label="💬 企业微信" value="wechat" />
            <el-option label="🌐 通用 Webhook" value="webhook" />
            <el-option label="📱 SMS" value="sms" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标地址" required>
          <el-input v-model="channelForm.target" :placeholder="targetPlaceholder" />
        </el-form-item>
        <el-form-item label="SMTP/配置" v-if="channelForm.channelType === 'email'">
          <el-input v-model="channelForm.config" type="textarea" :rows="3"
            placeholder='{"host":"smtp.example.com","port":465,"username":"...","password":"...","from":"alert@example.com"}' />
        </el-form-item>
        <el-form-item label="通知模板">
          <el-input v-model="channelForm.template" type="textarea" :rows="3"
            placeholder='${ruleName} 触发 ${severity} 告警，指标 ${metricName} = ${metricValue}，阈值 ${threshold}&#10;详情：${message}' />
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="channelForm.priority" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="channelForm.enabled" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="channelForm.description" placeholder="可选备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="channelFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingChannel" @click="saveChannel">
          {{ channelFormMode === 'edit' ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 告警规则编辑弹窗 (Day 45) -->
    <el-dialog v-model="ruleFormVisible" :title="ruleFormMode === 'edit' ? '编辑告警规则' : '新建告警规则'" width="580px" destroy-on-close>
      <el-form :model="ruleForm" label-width="120px" size="default">
        <el-form-item label="规则名称" required>
          <el-input v-model="ruleForm.name" placeholder="如：CPU 过高告警" maxlength="100" />
        </el-form-item>
        <el-form-item label="指标名">
          <el-select v-model="ruleForm.metricName" placeholder="选择指标" style="width:100%">
            <el-option label="cpu_usage (CPU使用率)" value="cpu_usage" />
            <el-option label="memory_usage (内存使用率)" value="memory_usage" />
            <el-option label="disk_usage (磁盘使用率)" value="disk_usage" />
            <el-option label="jvm_heap_usage (JVM堆内存)" value="jvm_heap_usage" />
            <el-option label="http_5xx_rate (5xx错误率)" value="http_5xx_rate" />
            <el-option label="error_rate (错误率)" value="error_rate" />
            <el-option label="chat_messages_total (对话消息数)" value="chat_messages_total" />
            <el-option label="tool_calls_total (工具调用数)" value="tool_calls_total" />
          </el-select>
        </el-form-item>
        <el-form-item label="条件">
          <el-select v-model="ruleForm.operator" style="width:100px">
            <el-option label=">" value=">" />
            <el-option label=">=" value=">=" />
            <el-option label="<" value="<" />
            <el-option label="<=" value="<=" />
            <el-option label="=" value="=" />
            <el-option label="!=" value="!=" />
          </el-select>
          <el-input-number v-model="ruleForm.threshold" :min="0" :max="100" style="margin-left:8px;width:120px" />
        </el-form-item>
        <el-form-item label="严重级别">
          <el-select v-model="ruleForm.severity" style="width:100%">
            <el-option label="INFO" value="info" />
            <el-option label="WARNING" value="warning" />
            <el-option label="CRITICAL" value="critical" />
          </el-select>
        </el-form-item>
        <el-form-item label="通知渠道">
          <el-select v-model="ruleForm.notifyChannel" placeholder="选择渠道（可选）" clearable style="width:100%">
            <el-option v-for="ch in channels" :key="ch.id" :label="ch.name + ' (' + ch.channelType + ')'" :value="String(ch.id)" />
          </el-select>
        </el-form-item>

        <!-- 升级策略 (Day 45) -->
        <el-divider content-position="left" style="font-size:13px">⚠️ 升级策略（仅 CRITICAL 生效）</el-divider>
        <el-form-item label="升级等待时间">
          <el-input-number v-model="ruleForm.escalateAfterMinutes" :min="0" :max="1440" :step="5" style="width:140px" />
          <span style="margin-left:8px;color:#909399;font-size:13px">分钟（0=不升级）</span>
        </el-form-item>
        <el-form-item label="升级通知渠道">
          <el-select v-model="ruleForm.escalationChannel" placeholder="留空则使用原渠道" clearable multiple style="width:100%">
            <el-option v-for="ch in channels" :key="ch.id" :label="ch.name + ' (' + ch.channelType + ')'" :value="ch.channelType" />
          </el-select>
        </el-form-item>
        <el-form-item label="自动恢复时间">
          <el-input-number v-model="ruleForm.autoResolveMinutes" :min="0" :max="10080" :step="10" style="width:140px" />
          <span style="margin-left:8px;color:#909399;font-size:13px">分钟（0=不自动恢复）</span>
        </el-form-item>

        <el-form-item label="启用">
          <el-switch v-model="ruleForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleFormVisible = false" :disabled="savingRule">取消</el-button>
        <el-button type="primary" :loading="savingRule" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, View, Loading, Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  getMonitorHealth, getJvmHealth, getFiringAlerts, rcaAnalysis, acknowledgeAlert,
  listAlertChannels, createAlertChannel, updateAlertChannel, deleteAlertChannel, testAlertChannel,
  getAlertSla, getAlertTrend, getAlertStatistics, getAlertTimeSeries,
  getAllAlertRules, createAlertRule, updateAlertRule, deleteAlertRule, toggleAlertRule
} from '@/api/monitor'
import http from '@/api/http'
import * as echarts from 'echarts'

const activeTab = ref('jvm')
const services = ref([])
const jvm = ref({ heapUsed: '-', heapMax: '-', nonHeapUsed: '-', gcCount: '-', threads: '-', uptime: '-', cpuUsage: '-' })
const alerts = ref([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const acking = ref(false)
const alertDetail = ref(null)

// ========== 通知渠道 (Day 42) ==========
const channels = ref([])
const channelsLoading = ref(false)
const channelFormVisible = ref(false)
const channelFormMode = ref('create') // 'create' | 'edit'
const savingChannel = ref(false)
const testingId = ref(null)
const channelForm = ref({
  id: null, name: '', channelType: 'dingtalk', target: '', config: '',
  template: '${ruleName} 触发 ${severity} 告警，指标 ${metricName} = ${metricValue}，阈值 ${threshold}\n详情：${message}',
  priority: 1, enabled: true, description: ''
})

const targetPlaceholder = computed(() => {
  const map = {
    email: 'alert@example.com（多个用逗号分隔）',
    dingtalk: 'https://oapi.dingtalk.com/robot/send?access_token=xxx',
    wechat: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx',
    webhook: 'https://your-webhook-url.example.com/notify',
    sms: '手机号，多个用逗号分隔'
  }
  return map[channelForm.value.channelType] || '目标地址'
})

function channelTypeLabel(type) {
  return { email: '邮件', dingtalk: '钉钉', wechat: '企微', webhook: 'Webhook', sms: 'SMS' }[type] || type || '未知'
}
function channelTypeTag(type) {
  return { email: '', dingtalk: 'success', wechat: 'success', webhook: 'warning', sms: '' }[type] || 'info'
}

// ========== SLA 统计 (Day 43) ==========
const slaWindow = ref(30)
const sla = ref({})
const slaLoading = ref(false)

// ========== 告警统计概览 (Day 47) ==========
const statsWindow = ref(30)
const statsLoading = ref(false)
const stats = ref({})

// ========== ECharts 可视化 (Day 48) ==========
const pieChartRef = ref(null)
const barChartRef = ref(null)
let pieChart = null
let barChart = null

function initPieChart(data) {
  if (!pieChartRef.value) return
  if (!pieChart) pieChart = echarts.init(pieChartRef.value)
  const critical = data.critical || 0
  const warning = data.warning || 0
  const info = data.info || 0
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { formatter: '{b}\n{c}', fontSize: 11 },
      data: [
        { value: critical, name: 'CRITICAL', itemStyle: { color: '#f56c6c' } },
        { value: warning, name: 'WARNING', itemStyle: { color: '#e6a23c' } },
        { value: info, name: 'INFO', itemStyle: { color: '#909399' } }
      ].filter(d => d.value > 0)
    }]
  })
}

function initBarChart(seriesData) {
  if (!barChartRef.value) return
  if (!barChart) barChart = echarts.init(barChartRef.value)
  const dates = seriesData.map(d => d.date)
  const totalData = seriesData.map(d => d.total)
  const criticalData = seriesData.map(d => d.critical)
  const warningData = seriesData.map(d => d.warning)

  barChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['总计', 'CRITICAL', 'WARNING'], bottom: 0 },
    grid: { top: 8, right: 16, bottom: 40, left: 40 },
    xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 10, rotate: 30 } },
    yAxis: { type: 'value', name: '告警数', axisLabel: { fontSize: 10 } },
    series: [
      { name: '总计', type: 'bar', data: totalData, itemStyle: { color: '#409eff' } },
      { name: 'CRITICAL', type: 'bar', stack: 'severity', data: criticalData, itemStyle: { color: '#f56c6c' } },
      { name: 'WARNING', type: 'bar', stack: 'severity', data: warningData, itemStyle: { color: '#e6a23c' } }
    ]
  })
}

async function loadStats() {
  statsLoading.value = true
  try {
    const [rStats, rSeries] = await Promise.all([
      getAlertStatistics(statsWindow.value),
      getAlertTimeSeries(statsWindow.value)
    ])
    stats.value = rStats.data || rStats.result || {}
    const seriesData = (rSeries.data || rSeries.result || [])

    // 更新饼图
    initPieChart(stats.value)
    initBarChart(seriesData)
  } catch (e) {
    console.error('[Monitor] loadStats failed:', e)
    stats.value = {}
  } finally {
    statsLoading.value = false
  }
}

function exportStatsImg(chartName) {
  const chart = chartName === 'pieChart' ? pieChart : barChart
  if (!chart) return
  const url = chart.getDataURL({ type: 'png', pixelRatio: 2, backgroundColor: '#fff' })
  const a = document.createElement('a')
  a.href = url
  a.download = `monitor-${chartName}-${Date.now()}.png`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

// ========== 告警趋势 (Day 44) ==========
const trendChartRef = ref(null)
const trendDays = ref(7)
const trendLoading = ref(false)
const trendTotal = ref(0)
const trendAvg = ref(0)
const trendMax = ref(0)
const trendMaxDate = ref('')
let trendChart = null

// ========== P1-5: JVM堆内存趋势图 ==========
const jvmChartRef = ref(null)
const jvmHistory = ref([]) // { time, used, free } 单位 MB
const MAX_JVM_POINTS = 30
let jvmChart = null
let jvmTimer = null

const slaGradeColor = computed(() => {
  const g = sla.value.grade
  if (g === 'A+' || g === 'A') return '#67c23a'
  if (g === 'B') return '#409eff'
  if (g === 'C') return '#e6a23c'
  return '#f56c6c'
})
const availabilityColor = computed(() => {
  const p = sla.value.availabilityPct || 0
  if (p >= 99.5) return '#67c23a'
  if (p >= 99.0) return '#409eff'
  if (p >= 95.0) return '#e6a23c'
  return '#f56c6c'
})

async function loadSla() {
  slaLoading.value = true
  try {
    const r = await getAlertSla(slaWindow.value)
    sla.value = r.data || {}
    sla.value.grade = sla.value.slaGrade
  } catch (e) {
    console.warn('[Monitor] SLA load failed:', e)
  } finally {
    slaLoading.value = false
  }
}

async function loadAlertTrend() {
  trendLoading.value = true
  try {
    const r = await getAlertTrend({ days: trendDays.value })
    const data = r.data || []
    if (!trendChart) {
      const echarts = (await import('echarts')).default
      trendChart = echarts.init(trendChartRef.value)
    }
    const dates = data.map(d => d.date)
    const critical = data.map(d => d.CRITICAL || 0)
    const warning = data.map(d => d.WARNING || 0)
    const info = data.map(d => d.INFO || 0)
    const totals = data.map(d => d.total || 0)
    trendChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { data: ['CRITICAL', 'WARNING', 'INFO', '总计'], top: 0 },
      grid: { top: 36, left: 48, right: 16, bottom: 32 },
      xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 11 } },
      yAxis: { type: 'value', name: '告警数', minInterval: 1 },
      series: [
        { name: 'CRITICAL', type: 'line', data: critical, smooth: true, lineStyle: { width: 2 }, itemStyle: { color: '#f56c6c' } },
        { name: 'WARNING',  type: 'line', data: warning,  smooth: true, lineStyle: { width: 2 }, itemStyle: { color: '#e6a23c' } },
        { name: 'INFO',     type: 'line', data: info,     smooth: true, lineStyle: { width: 2 }, itemStyle: { color: '#909399' } },
        { name: '总计',     type: 'bar',  data: totals,  barMaxWidth: 40, itemStyle: { color: 'rgba(64,158,255,0.3)' } }
      ]
    })
    // 统计
    const sum = totals.reduce((a, b) => a + b, 0)
    trendTotal.value = sum
    trendAvg.value = dates.length > 0 ? sum / dates.length : 0
    const maxVal = Math.max(...totals, 0)
    trendMax.value = maxVal
    const maxIdx = totals.indexOf(maxVal)
    trendMaxDate.value = maxIdx >= 0 ? dates[maxIdx] : ''
  } catch (e) {
    console.warn('[Monitor] Alert trend load failed:', e)
  } finally {
    trendLoading.value = false
  }
}

// Day 40: 自动刷新
let refreshTimer = null
onMounted(() => {
  loadData()
  refreshTimer = setInterval(loadData, 30_000)
})
onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (trendChart) { trendChart.dispose(); trendChart = null }
  if (jvmChart) { jvmChart.dispose(); jvmChart = null }
  if (jvmTimer) clearInterval(jvmTimer)
  if (pieChart) { pieChart.dispose(); pieChart = null }
  if (barChart) { barChart.dispose(); barChart = null }
  window.removeEventListener('resize', handleChartResize)
})

// ECharts 窗口 resize 自动适配
function handleChartResize() {
  if (pieChart) pieChart.resize()
  if (barChart) barChart.resize()
}
window.addEventListener('resize', handleChartResize)

// Day 42: tab 切换时加载渠道数据
// Day 43: SLA tab 也懒加载
// P1-5: JVM tab 懒加载图表
watch(activeTab, (tab) => {
  if (tab === 'channels' && channels.value.length === 0) {
    loadChannels()
  }
  if (tab === 'sla' && !sla.value.windowDays) {
    loadSla()
  }
  if (tab === 'stats' && !stats.value.windowDays) {
    loadStats()
  }
  // Day 48: stats tab 切换回来时刷新图表
  if (tab === 'stats') {
    setTimeout(() => {
      if (pieChart) pieChart.resize()
      if (barChart) barChart.resize()
    }, 50)
  }
  if (tab === 'trend') {
    loadAlertTrend()
  }
  if (tab === 'rules' && rules.value.length === 0) {
    loadRules()
  }
  if (tab === 'jvm') {
    initJvmChart()
    startJvmTimer()
  }
})

async function loadData() {
  try {
    const [h, j, a] = await Promise.all([getMonitorHealth(), getJvmHealth(), getFiringAlerts()])
    services.value = Object.entries(h.data || {}).map(([name, v]) => ({
      name, status: v?.status || 'DOWN', latency: v?.latency || 0
    }))
    jvm.value = j.data || jvm.value
    alerts.value = a.data || []
    // P1-5: JVM堆内存数据记录到历史
    if (jvm.value.heapUsed) {
      const usedMB = parseFloat(jvm.value.heapUsed) || 0
      const maxMB = parseFloat(jvm.value.heapMax) || 0
      const freeMB = Math.max(0, maxMB - usedMB)
      jvmHistory.value.push({
        time: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
        used: usedMB,
        free: freeMB
      })
      if (jvmHistory.value.length > MAX_JVM_POINTS) {
        jvmHistory.value.shift()
      }
      updateJvmChart()
    }
  } catch {}
}

function severityType(s) {
  return { CRITICAL: 'danger', HIGH: 'danger', WARNING: 'warning', INFO: 'info', LOW: 'info' }[s] || 'info'
}
function statusType(s) {
  return { firing: 'danger', acked: 'warning', resolved: 'success' }[s] || 'info'
}
function statusLabel(s) {
  return { firing: '触发中', acked: '已确认', resolved: '已解决' }[s] || s || '未知'
}

function formatTime(ts) {
  if (!ts) return '-'
  if (typeof ts === 'string') return ts.replace('T', ' ').substring(0, 19)
  return String(ts)
}

// P1-5: JVM堆内存趋势图
async function initJvmChart() {
  if (!jvmChartRef.value) return
  const echarts = await import('echarts')
  if (jvmChart) jvmChart.dispose()
  jvmChart = echarts.init(jvmChartRef.value)
  updateJvmChart()
}

function updateJvmChart() {
  if (!jvmChart || !jvmHistory.value.length) return
  const times = jvmHistory.value.map(d => d.time)
  const usedData = jvmHistory.value.map(d => d.used)
  const freeData = jvmHistory.value.map(d => d.free)
  jvmChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['已用', '空闲'], top: 0 },
    grid: { top: 36, left: 48, right: 16, bottom: 32 },
    xAxis: { type: 'category', data: times, name: '时间', axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value', name: 'MB', minInterval: 1 },
    series: [
      { name: '已用', type: 'line', data: usedData, smooth: true, itemStyle: { color: '#f56c6c' } },
      { name: '空闲', type: 'line', data: freeData, smooth: true, itemStyle: { color: '#67c23a' } }
    ]
  })
}

function startJvmTimer() {
  if (jvmTimer) clearInterval(jvmTimer)
  jvmTimer = setInterval(() => {
    if (activeTab.value === 'jvm') {
      loadJvmData()
    }
  }, 10000)
}

async function loadJvmData() {
  try {
    const j = await getJvmHealth()
    jvm.value = j.data || jvm.value
    if (jvm.value.heapUsed) {
      const usedMB = parseFloat(jvm.value.heapUsed) || 0
      const maxMB = parseFloat(jvm.value.heapMax) || 0
      const freeMB = Math.max(0, maxMB - usedMB)
      jvmHistory.value.push({
        time: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
        used: usedMB,
        free: freeMB
      })
      if (jvmHistory.value.length > MAX_JVM_POINTS) {
        jvmHistory.value.shift()
      }
      updateJvmChart()
    }
  } catch {}
}

// Day 41: 打开告警详情
async function openAlertDetail(row) {
  alertDetail.value = null
  detailLoading.value = true
  detailVisible.value = true
  try {
    const r = await http.get(`/monitor/alerts/${row.id}`)
    alertDetail.value = r.data || r
  } catch (e) {
    ElMessage.error('加载告警详情失败：' + (e.message || ''))
  } finally {
    detailLoading.value = false
  }
}

// V7.0 Flow⑤: 跳转到对话页面
function jumpToChat(row) {
  if (!row.sessionId) { ElMessage.warning('该告警无关联会话'); return }
  window.location.href = '/chat?sessionId=' + encodeURIComponent(row.sessionId)
}

// Day 41: 从详情页确认告警
async function acknowledgeAlertFromDetail() {
  if (!alertDetail.value?.id) return
  acking.value = true
  try {
    await acknowledgeAlert(alertDetail.value.id)
    ElMessage.success('告警已确认')
    if (alertDetail.value) alertDetail.value.status = 'acked'
    const idx = alerts.value.findIndex(a => a.id === alertDetail.value?.id)
    if (idx !== -1) alerts.value[idx].status = 'acked'
  } catch (e) {
    ElMessage.error('确认失败：' + (e.message || ''))
  } finally {
    acking.value = false
  }
}

// ========== 通知渠道管理 (Day 42) ==========

async function loadChannels() {
  channelsLoading.value = true
  try {
    const r = await listAlertChannels()
    channels.value = r.data || []
  } catch (e) {
    ElMessage.error('加载渠道失败：' + (e.message || ''))
  } finally {
    channelsLoading.value = false
  }
}

function openChannelForm(row) {
  if (row) {
    channelFormMode.value = 'edit'
    channelForm.value = {
      id: row.id, name: row.name, channelType: row.channelType || row.type,
      target: row.target || '', config: row.config || '',
      template: row.template || '${ruleName} 触发 ${severity} 告警，指标 ${metricName} = ${metricValue}，阈值 ${threshold}\n详情：${message}',
      priority: row.priority ?? 1, enabled: !!row.enabled, description: row.description || ''
    }
  } else {
    channelFormMode.value = 'create'
    channelForm.value = {
      id: null, name: '', channelType: 'dingtalk', target: '', config: '',
      template: '${ruleName} 触发 ${severity} 告警，指标 ${metricName} = ${metricValue}，阈值 ${threshold}\n详情：${message}',
      priority: 1, enabled: true, description: ''
    }
  }
  channelFormVisible.value = true
}

async function saveChannel() {
  if (!channelForm.value.name?.trim()) {
    ElMessage.warning('请输入渠道名称')
    return
  }
  if (!channelForm.value.target?.trim()) {
    ElMessage.warning('请输入目标地址')
    return
  }
  savingChannel.value = true
  try {
    const payload = { ...channelForm.value }
    if (channelFormMode.value === 'edit') {
      await updateAlertChannel(payload.id, payload)
      ElMessage.success('渠道更新成功')
    } else {
      await createAlertChannel(payload)
      ElMessage.success('渠道创建成功')
    }
    channelFormVisible.value = false
    await loadChannels()
  } catch (e) {
    ElMessage.error((channelFormMode.value === 'edit' ? '更新' : '创建') + '失败：' + (e.message || ''))
  } finally {
    savingChannel.value = false
  }
}

async function testChannel(row) {
  testingId.value = row.id
  try {
    await testAlertChannel(row.id)
    ElMessage.success('测试消息发送成功！请检查 ' + channelTypeLabel(row.channelType) + ' 是否收到通知')
  } catch (e) {
    ElMessage.error('测试发送失败：' + (e.message || '请检查配置是否正确'))
  } finally {
    testingId.value = null
  }
}

async function deleteChannel(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除通知渠道「${row.name}」吗？删除后该渠道将不再收到告警通知。`,
      '删除渠道', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await deleteAlertChannel(row.id)
    ElMessage.success('渠道已删除')
    await loadChannels()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败：' + (e.message || ''))
  }
}

// ========== 告警规则 (Day 45) ==========
const rules = ref([])
const rulesLoading = ref(false)
const ruleFormVisible = ref(false)
const ruleFormMode = ref('create') // 'create' | 'edit'
const savingRule = ref(false)
const ruleForm = ref({
  id: null, name: '', metricName: 'cpu_usage', operator: '>', threshold: 80,
  severity: 'warning', notifyChannel: '', enabled: true,
  escalateAfterMinutes: 0, escalationChannel: '', autoResolveMinutes: 0,
  cooldownMinutes: 15
})

async function loadRules() {
  rulesLoading.value = true
  try {
    const r = await getAllAlertRules()
    rules.value = r.data || []
  } catch {
    rules.value = []
  } finally {
    rulesLoading.value = false
  }
}

function openRuleForm(row) {
  if (row) {
    ruleFormMode.value = 'edit'
    ruleForm.value = {
      id: row.id, name: row.name, metricName: row.metricName || 'cpu_usage',
      operator: row.operator || '>', threshold: row.threshold || 80,
      severity: row.severity || 'warning', notifyChannel: row.notifyChannel || '',
      enabled: !!row.enabled,
      escalateAfterMinutes: row.escalateAfterMinutes || 0,
      escalationChannel: row.escalationChannel || '',
      autoResolveMinutes: row.autoResolveMinutes || 0,
      cooldownMinutes: row.cooldownMinutes || 15
    }
  } else {
    ruleFormMode.value = 'create'
    ruleForm.value = {
      id: null, name: '', metricName: 'cpu_usage', operator: '>', threshold: 80,
      severity: 'warning', notifyChannel: '', enabled: true,
      escalateAfterMinutes: 0, escalationChannel: '', autoResolveMinutes: 0,
      cooldownMinutes: 15
    }
  }
  ruleFormVisible.value = true
}

async function saveRule() {
  if (!ruleForm.value.name.trim()) {
    ElMessage.warning('请输入规则名称')
    return
  }
  savingRule.value = true
  try {
    const payload = { ...ruleForm.value }
    // 去掉 id 字段（创建时不需要 id）
    delete payload.id
    if (ruleFormMode.value === 'edit') {
      await updateAlertRule(ruleForm.value.id, payload)
      ElMessage.success('规则更新成功')
    } else {
      await createAlertRule(payload)
      ElMessage.success('规则创建成功')
    }
    ruleFormVisible.value = false
    await loadRules()
  } catch (e) {
    ElMessage.error('保存失败：' + (e.message || ''))
  } finally {
    savingRule.value = false
  }
}

async function toggleRule(row) {
  try {
    await toggleAlertRule(row.id, !row.enabled)
    ElMessage.success(row.enabled ? '规则已禁用' : '规则已启用')
    await loadRules()
  } catch (e) {
    ElMessage.error('操作失败：' + (e.message || ''))
  }
}

async function deleteRule(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除规则「${row.name}」吗？该操作不可恢复。`,
      '删除规则', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await deleteAlertRule(row.id)
    ElMessage.success('规则已删除')
    await loadRules()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败：' + (e.message || ''))
  }
}
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
  h2 { margin: 0; font-size: 16px; }
}

.rca-section {
  background: #f5f8ff; border: 1px solid #d9ecff; border-radius: 8px; padding: 16px;
}
.rca-header {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 14px; font-weight: 600; color: #409eff; margin-bottom: 12px;
}
.rca-category {
  display: flex; align-items: center; gap: 8px; margin-bottom: 10px;
}
.rca-label {
  font-size: 12px; color: #909399; margin-bottom: 4px;
}
.rca-cause {
  margin-bottom: 10px;
  .rca-content { font-size: 13px; color: #303133; line-height: 1.6; }
}
.rca-suggestions {
  ul {
    margin: 0; padding-left: 20px;
    li { font-size: 13px; color: #303133; line-height: 1.8; }
  }
}

.channel-toolbar {
  display: flex; gap: 8px; align-items: center;
}
.sla-toolbar {
  display: flex; gap: 8px; align-items: center;
}
</style>
