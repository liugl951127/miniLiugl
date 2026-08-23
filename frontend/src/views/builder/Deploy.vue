<!--
  @file builder/Deploy.vue - 远程部署配置 (V1.0)
  路由: /builder/deploy
  4 目标: 本地 Docker / Kubernetes / 云厂商 / 边缘设备
-->
<template>
  <div class="deploy-page">
    <!-- 1. 目标选择 -->
    <el-card shadow="never" class="target-card">
      <template #header><span>🎯 选择部署目标</span></template>
      <el-row :gutter="12">
        <el-col v-for="t in targets" :key="t.key" :span="6">
          <div class="target-card-item" :class="{ active: target === t.key }" @click="target = t.key">
            <div class="tci-icon" :style="{ background: t.bg }">{{ t.icon }}</div>
            <div class="tci-name">{{ t.name }}</div>
            <div class="tci-desc">{{ t.desc }}</div>
            <div class="tci-cost">≈ {{ t.cost }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="16" style="margin-top:16px">
      <!-- 2. 集群/凭证 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>🔌 集群配置</span>
              <el-button size="small" :icon="Refresh">刷新</el-button>
            </div>
          </template>

          <!-- K8s -->
          <div v-if="target === 'k8s'" class="target-form">
            <el-form label-width="120px" :model="config.k8s">
              <el-form-item label="Kubeconfig">
                <el-input v-model="config.k8s.kubeconfig" type="textarea" :rows="3" placeholder="粘贴 Kubeconfig YAML" />
              </el-form-item>
              <el-form-item label="命名空间">
                <el-input v-model="config.k8s.namespace" />
              </el-form-item>
              <el-form-item label="集群">
                <el-select v-model="config.k8s.cluster" style="width:100%">
                  <el-option label="prod-cluster-01" value="prod-cluster-01" />
                  <el-option label="prod-cluster-02 (新加坡)" value="prod-cluster-02" />
                  <el-option label="staging-cluster" value="staging-cluster" />
                </el-select>
              </el-form-item>
              <el-form-item label="镜像仓库">
                <el-input v-model="config.k8s.registry" placeholder="registry.minimax.io/agent-forge" />
              </el-form-item>
            </el-form>
          </div>

          <!-- Docker 本地 -->
          <div v-else-if="target === 'docker'" class="target-form">
            <el-alert type="info" :closable="false" show-icon
              title="本地 Docker 模式"
              description="将在当前机器上运行所有智能体容器, 适合测试和小规模场景" />
            <el-form label-width="120px" :model="config.docker" style="margin-top:16px">
              <el-form-item label="Docker Socket">
                <el-input v-model="config.docker.socket" placeholder="/var/run/docker.sock" />
              </el-form-item>
              <el-form-item label="网络模式">
                <el-select v-model="config.docker.network" style="width:100%">
                  <el-option label="bridge (默认)" value="bridge" />
                  <el-option label="host" value="host" />
                </el-select>
              </el-form-item>
              <el-form-item label="数据卷">
                <el-input v-model="config.docker.volume" placeholder="/data/agents" />
              </el-form-item>
            </el-form>
          </div>

          <!-- 云厂商 -->
          <div v-else-if="target === 'cloud'" class="target-form">
            <el-form label-width="120px" :model="config.cloud">
              <el-form-item label="云厂商">
                <el-select v-model="config.cloud.provider" style="width:100%">
                  <el-option label="阿里云 ACK" value="aliyun" />
                  <el-option label="腾讯云 TKE" value="tencent" />
                  <el-option label="AWS EKS" value="aws" />
                  <el-option label="Azure AKS" value="azure" />
                </el-select>
              </el-form-item>
              <el-form-item label="AccessKey">
                <el-input v-model="config.cloud.ak" placeholder="AccessKey ID" />
              </el-form-item>
              <el-form-item label="SecretKey">
                <el-input v-model="config.cloud.sk" type="password" show-password />
              </el-form-item>
              <el-form-item label="区域">
                <el-select v-model="config.cloud.region" style="width:100%">
                  <el-option label="华东1 (杭州)" value="cn-hangzhou" />
                  <el-option label="华北2 (北京)" value="cn-beijing" />
                  <el-option label="华南1 (深圳)" value="cn-shenzhen" />
                </el-select>
              </el-form-item>
              <el-form-item label="VPC">
                <el-input v-model="config.cloud.vpc" placeholder="vpc-xxxxxx" />
              </el-form-item>
            </el-form>
          </div>

          <!-- 边缘 -->
          <div v-else-if="target === 'edge'" class="target-form">
            <el-form label-width="120px" :model="config.edge">
              <el-form-item label="设备列表">
                <el-input v-model="config.edge.devices" type="textarea" :rows="3" placeholder="设备地址, 一行一个, 如: 192.168.1.100:22" />
              </el-form-item>
              <el-form-item label="SSH 用户">
                <el-input v-model="config.edge.user" />
              </el-form-item>
              <el-form-item label="认证方式">
                <el-radio-group v-model="config.edge.auth">
                  <el-radio value="password">密码</el-radio>
                  <el-radio value="key">私钥</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item v-if="config.edge.auth === 'password'" label="SSH 密码">
                <el-input v-model="config.edge.password" type="password" show-password />
              </el-form-item>
              <el-form-item v-else label="SSH 私钥">
                <el-input v-model="config.edge.key" type="textarea" :rows="3" />
              </el-form-item>
            </el-form>
          </div>
        </el-card>
      </el-col>

      <!-- 3. 资源 + 安全 -->
      <el-col :span="10">
        <el-card shadow="never" class="resource-card">
          <template #header><span>⚙️ 资源配置</span></template>
          <el-form label-width="80px" label-position="top" size="default">
            <el-form-item label="副本数">
              <el-input-number v-model="config.resources.replicas" :min="1" :max="20" style="width:100%" />
            </el-form-item>
            <el-form-item label="每副本 CPU">
              <el-slider v-model="config.resources.cpu" :min="100" :max="4000" :step="100" show-stops />
              <div style="font-size:11px;color:#64748b">{{ config.resources.cpu }}m (毫核)</div>
            </el-form-item>
            <el-form-item label="每副本内存">
              <el-slider v-model="config.resources.memory" :min="128" :max="8192" :step="128" show-stops />
              <div style="font-size:11px;color:#64748b">{{ config.resources.memory }} MiB</div>
            </el-form-item>
            <el-form-item label="自动伸缩">
              <el-switch v-model="config.resources.autoscale" />
            </el-form-item>
            <el-form-item v-if="config.resources.autoscale" label="范围">
              <el-input-number v-model="config.resources.min" :min="1" :max="10" style="width:48%" />
              <span style="margin:0 4px">~</span>
              <el-input-number v-model="config.resources.max" :min="2" :max="20" style="width:48%" />
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" style="margin-top:12px" class="cost-card">
          <template #header><span>💰 预估成本</span></template>
          <div class="cost-row">
            <span>计算</span><span>¥{{ costs.compute }}/月</span>
          </div>
          <div class="cost-row">
            <span>存储</span><span>¥{{ costs.storage }}/月</span>
          </div>
          <div class="cost-row">
            <span>网络</span><span>¥{{ costs.network }}/月</span>
          </div>
          <el-divider style="margin:12px 0" />
          <div class="cost-row total">
            <span>合计</span>
            <span>¥{{ totalCost }}/月</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 4. 部署按钮 -->
    <div class="deploy-bar">
      <el-button size="large" round :icon="RefreshLeft" @click="$router.push('/builder/designer')">返回</el-button>
      <div class="bar-info">
        <span>📦 {{ nodeCount }} 智能体</span>
        <span>🔁 {{ config.resources.replicas }} 副本</span>
        <span>🎯 {{ currentTarget.name }}</span>
      </div>
      <el-button size="large" round type="primary" :loading="deploying"
        :icon="Promotion" @click="startDeploy">
        开始部署
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, RefreshLeft, Promotion } from '@element-plus/icons-vue'
import { triggerDeploy, pollArgoCdStatus } from '@/api/forge'

const router = useRouter()
const target = ref('k8s')
const deploying = ref(false)
const nodeCount = ref(3)
const currentReleaseId = ref(null)  // V5.0: 从路由或 props 传入

const targets = [
  { key: 'docker', name: '本地 Docker', icon: '🐳', bg: 'linear-gradient(135deg, #2496ed, #1d7bb8)', desc: '单机容器化部署', cost: '¥0/月 (本地资源)' },
  { key: 'k8s',    name: 'Kubernetes', icon: '☸️', bg: 'linear-gradient(135deg, #326ce5, #1f5dc9)', desc: '生产级集群',     cost: '¥2.5K/月起' },
  { key: 'gitops', name: 'GitOps ⭐ V5.0', icon: '🚀', bg: 'linear-gradient(135deg, #6366f1, #8b5cf6)', desc: 'JGit 推送 + ArgoCD 同步 (真)', cost: '需配置 git/argocd' },
  { key: 'cloud',  name: '云厂商',      icon: '☁️', bg: 'linear-gradient(135deg, #ff6a00, #ee0979)', desc: '阿里云/AWS/腾讯云', cost: '¥3K/月起' },
  { key: 'edge',   name: '边缘设备',   icon: '📡', bg: 'linear-gradient(135deg, #10b981, #06b6d4)', desc: 'IoT/边缘服务器', cost: '¥0.5K/月' }
]
const currentTarget = computed(() => targets.find(t => t.key === target.value))

const config = reactive({
  k8s: { kubeconfig: '', namespace: 'agent-forge', cluster: 'prod-cluster-01', registry: 'registry.minimax.io/agent-forge' },
  docker: { socket: '/var/run/docker.sock', network: 'bridge', volume: '/data/agents' },
  cloud: { provider: 'aliyun', ak: '', sk: '', region: 'cn-hangzhou', vpc: 'vpc-xxxxxx' },
  edge: { devices: '', user: 'root', auth: 'key', password: '', key: '' },
  resources: { replicas: 2, cpu: 500, memory: 1024, autoscale: true, min: 2, max: 8 }
})

const costs = computed(() => ({
  compute: (config.resources.replicas * config.resources.cpu * 0.002).toFixed(2),
  storage: (config.resources.replicas * 30).toFixed(2),
  network: (config.resources.replicas * 15).toFixed(2)
}))
const totalCost = computed(() => {
  return Object.values(costs.value).reduce((a, b) => a + parseFloat(b), 0).toFixed(2)
})

async function startDeploy() {
  await ElMessageBox.confirm(
    `即将部署到 ${currentTarget.value.name}。\n智能体: ${nodeCount.value} 个\n副本: ${config.resources.replicas} 个\n预计耗时: 3-5 分钟`,
    '确认部署',
    { type: 'info', confirmButtonText: '开始部署' }
  )
  deploying.value = true
  // V5.0: GitOps 走真 API (JGit + ArgoCD), 其他 target 还是 V1.0 mock
  if (target.value === 'gitops') {
    try {
      ElMessage.info('🚀 GitOps 启动: JGit 推送 + ArgoCD 同步...')
      await triggerDeploy(currentReleaseId.value || 1)  // fallback to 1 if not set
      // 轮询 ArgoCD (前端每 5s 一次, 最多 60s)
      const appName = 'agent-' + Date.now()
      for (let i = 0; i < 12; i++) {
        await new Promise(r => setTimeout(r, 5000))
        try {
          const resp = await pollArgoCdStatus(appName)
          ElMessage.success(`📊 ArgoCD: health=${resp.data.health} sync=${resp.data.syncStatus}`)
          if (resp.data.health === 'Healthy' && resp.data.syncStatus === 'Synced') break
        } catch (e) { console.warn('ArgoCD poll failed', e) }
      }
    } catch (e) {
      ElMessage.error('GitOps 部署失败: ' + e.message)
    } finally {
      deploying.value = false
    }
  } else {
    // V1.0 mock 保留给 docker/k8s/cloud/edge
    ElMessage.info('开始部署, 跳转到监控...')
    setTimeout(() => {
      deploying.value = false
      router.push('/builder/monitor')
    }, 1500)
  }
}
</script>

<style scoped>
.deploy-page { max-width: 1200px; margin: 0 auto; }

.target-card { border-radius: 14px; }
.target-card-item {
  padding: 20px 16px; background: #fafbfc; border-radius: 12px;
  text-align: center; cursor: pointer; transition: all 0.2s;
  border: 2px solid transparent; height: 160px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
}
.target-card-item:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(0,0,0,0.08); }
.target-card-item.active { border-color: #6366f1; background: linear-gradient(180deg, #f5f7ff 0%, #fafbff 100%); }
.tci-icon {
  width: 48px; height: 48px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; margin-bottom: 8px;
  color: white; box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.tci-name { font-weight: 600; color: #1e293b; }
.tci-desc { font-size: 11px; color: #64748b; margin: 4px 0; }
.tci-cost { font-size: 12px; color: #6366f1; font-weight: 500; }

.target-form { padding-top: 8px; }
.resource-card, .cost-card { border-radius: 14px; }

.cost-row { display: flex; justify-content: space-between; padding: 4px 0; font-size: 13px; color: #475569; }
.cost-row.total { font-size: 15px; font-weight: 700; color: #1e293b; }

.deploy-bar {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: 20px; padding: 14px 20px; background: white;
  border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}
.bar-info { display: flex; gap: 16px; font-size: 13px; color: #475569; }
</style>
