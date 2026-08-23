<!--
  @file builder/Analysis.vue - AI 解析页 (V1.0)
  路由: /builder/analysis
  动画解析 + 提取关键需求 + 推荐智能体角色
-->
<template>
  <div class="analysis-page">
    <!-- 解析动画 -->
    <div v-if="analyzing" class="analyzing-card">
      <div class="brain-wrap">
        <div class="brain">🧠</div>
        <div class="brain-pulse"></div>
        <div class="brain-pulse delay-1"></div>
        <div class="brain-pulse delay-2"></div>
      </div>
      <h2>AI 正在分析需求...</h2>
      <p class="analyzing-sub">基于 {{ totalTokens }} tokens 的输入, 多模型协同解析中</p>
      <div class="progress-line">
        <div class="progress-fill" :style="{ width: progress + '%' }"></div>
      </div>
      <div class="progress-text">{{ currentStep }} ({{ progress }}%)</div>
    </div>

    <!-- 解析结果 -->
    <div v-else class="result-content">
      <!-- 上: 提取的需求 -->
      <el-row :gutter="16">
        <el-col :span="14">
          <el-card shadow="never" class="req-card">
            <template #header>
              <div class="card-header">
                <span>📋 提取的关键需求</span>
                <el-button size="small" link :icon="EditPen" @click="editing = !editing">
                  {{ editing ? '保存' : '编辑' }}
                </el-button>
              </div>
            </template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="项目类型">
                <el-tag effect="plain" round>{{ extracted.projectType }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="业务场景">{{ extracted.scenario }}</el-descriptions-item>
              <el-descriptions-item label="核心功能">
                <el-tag v-for="f in extracted.features" :key="f" effect="plain" size="small" style="margin: 2px">
                  {{ f }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="预期规模">
                {{ extracted.scale }}
              </el-descriptions-item>
              <el-descriptions-item label="合规要求">
                <el-tag v-for="c in extracted.compliance" :key="c" type="warning" effect="plain" size="small" style="margin: 2px">
                  {{ c }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="集成需求">
                <el-tag v-for="i in extracted.integrations" :key="i" type="info" effect="plain" size="small" style="margin: 2px">
                  {{ i }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <el-col :span="10">
          <el-card shadow="never" class="overview-card">
            <template #header><span>📊 项目总览</span></template>
            <div class="overview-grid">
              <div class="overview-item">
                <div class="oi-num">{{ extracted.agents?.length || 0 }}</div>
                <div class="oi-label">推荐智能体</div>
              </div>
              <div class="overview-item">
                <div class="oi-num">{{ extracted.tools?.length || 0 }}</div>
                <div class="oi-label">需要工具</div>
              </div>
              <div class="overview-item">
                <div class="oi-num">{{ extracted.models?.length || 0 }}</div>
                <div class="oi-label">使用模型</div>
              </div>
              <div class="overview-item">
                <div class="oi-num">{{ extracted.estimatedCost || '¥2.3K/月' }}</div>
                <div class="oi-label">预估成本</div>
              </div>
            </div>
            <el-divider />
            <div class="complexity-meter">
              <div style="display:flex;justify-content:space-between;font-size:12px;color:#64748b;margin-bottom:4px">
                <span>复杂度</span><span>{{ complexityLevel }}</span>
              </div>
              <el-progress :percentage="complexity" :color="complexityColor" :show-text="false" />
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 中: 智能体角色推荐 -->
      <el-card shadow="never" class="agents-card">
        <template #header>
          <div class="card-header">
            <span>👥 推荐的智能体角色</span>
            <span class="card-sub">{{ extracted.agents?.length }} 个角色 · 可调整</span>
          </div>
        </template>
        <div class="agent-grid">
          <div v-for="a in extracted.agents" :key="a.name" class="agent-card">
            <div class="ac-avatar" :style="{ background: a.color }">{{ a.emoji }}</div>
            <div class="ac-body">
              <div class="ac-name">{{ a.name }}</div>
              <div class="ac-role">{{ a.role }}</div>
              <div class="ac-desc">{{ a.desc }}</div>
              <div class="ac-tools">
                <el-tag v-for="t in a.tools" :key="t" size="small" effect="plain" round>{{ t }}</el-tag>
              </div>
              <div class="ac-model">模型: {{ a.model }}</div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 下: 工作流预览 -->
      <el-card shadow="never" class="flow-card">
        <template #header><span>🔄 协作流程预览</span></template>
        <div class="flow-canvas">
          <div v-for="(step, i) in flow" :key="i" class="flow-step">
            <div class="flow-bubble">
              <div class="fb-num">{{ i + 1 }}</div>
              <div class="fb-name">{{ step.name }}</div>
            </div>
            <div v-if="i < flow.length - 1" class="flow-arrow">→</div>
          </div>
        </div>
      </el-card>

      <!-- 导航 -->
      <div class="action-bar">
        <el-button size="large" round :icon="RefreshLeft" @click="$router.push('/builder/requirements')">返回需求</el-button>
        <el-button size="large" round type="primary" :icon="ArrowRight" @click="$router.push('/builder/designer')">
          下一步: 团队设计 →
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { EditPen, RefreshLeft, ArrowRight } from '@element-plus/icons-vue'

const router = useRouter()
const analyzing = ref(true)
const progress = ref(0)
const currentStep = ref('初始化解析引擎')
const totalTokens = ref('3,247')
const editing = ref(false)

const steps = [
  '初始化解析引擎', '分词与实体识别', '提取项目类型',
  '识别业务场景', '匹配合规要求', '推荐智能体角色',
  '生成工作流'
]
let stepIdx = 0

onMounted(() => {
  const interval = setInterval(() => {
    progress.value += 5
    if (progress.value % 14 === 0 && stepIdx < steps.length - 1) {
      stepIdx++
      currentStep.value = steps[stepIdx]
    }
    if (progress.value >= 100) {
      progress.value = 100
      clearInterval(interval)
      setTimeout(() => { analyzing.value = false }, 300)
    }
  }, 100)
})

const extracted = ref({
  projectType: '教育行业 · 智能客服',
  scenario: '在线教育平台 7×24 小时智能客服, 处理学员咨询、课程推荐、退费流程、学习指导等',
  features: ['课程咨询', '退费处理', '学习规划', '知识问答', '情感安抚', '人工转接'],
  scale: '日均 5,000+ 会话, 峰值 200 并发',
  compliance: ['个人信息保护法', '教育行业规范', '未成年保护'],
  integrations: ['CRM 系统', '工单系统', '支付系统', '课程数据库', '微信生态'],
  agents: [
    { name: '小课', role: '课程顾问', emoji: '📚', color: 'linear-gradient(135deg, #6366f1, #8b5cf6)', desc: '回答课程相关问题, 推荐合适课程', tools: ['课程搜索', '价格查询', '试听预约'], model: 'Qwen2.5-7B' },
    { name: '小助', role: '退费专员', emoji: '💰', color: 'linear-gradient(135deg, #f59e0b, #ef4444)', desc: '处理退费流程, 解释政策', tools: ['订单查询', '工单创建', '支付接口'], model: 'Qwen2.5-7B' },
    { name: '小导', role: '学习规划师', emoji: '🎯', color: 'linear-gradient(135deg, #10b981, #06b6d4)', desc: '基于学员情况定制学习计划', tools: ['用户画像', '课程匹配', '进度跟踪'], model: 'Qwen2.5-7B' },
    { name: '小审', role: '质检员', emoji: '🔍', color: 'linear-gradient(135deg, #ec4899, #f43f5e)', desc: '监控对话质量, 标记异常', tools: ['情感分析', '敏感词检测', '满意度调查'], model: 'Qwen2.5-0.5B' }
  ],
  tools: ['课程搜索', '订单查询', '工单系统', '用户画像', '情感分析'],
  models: ['Qwen2.5-7B', 'Qwen2.5-0.5B', 'BGE-Embedding'],
  estimatedCost: '¥2.3K/月'
})

const flow = ref([
  { name: '用户提问' },
  { name: '意图识别' },
  { name: '路由分发' },
  { name: '小课/小助/小导 处理' },
  { name: '小审 质检' },
  { name: '回复用户' }
])

const complexity = computed(() => 72)
const complexityLevel = computed(() => complexity.value < 40 ? '低' : complexity.value < 70 ? '中' : '高')
const complexityColor = computed(() => complexity.value < 40 ? '#10b981' : complexity.value < 70 ? '#f59e0b' : '#ef4444')
</script>

<style scoped>
.analysis-page { max-width: 1200px; margin: 0 auto; }

/* 解析动画 */
.analyzing-card {
  text-align: center; padding: 60px 20px;
  background: white; border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}
.brain-wrap {
  position: relative; display: inline-block; margin-bottom: 20px;
}
.brain {
  font-size: 80px; position: relative; z-index: 2;
  filter: drop-shadow(0 4px 12px rgba(99, 102, 241, 0.3));
}
.brain-pulse {
  position: absolute; inset: 0; border-radius: 50%;
  border: 2px solid #6366f1; animation: pulse 2s infinite;
}
.brain-pulse.delay-1 { animation-delay: 0.6s; }
.brain-pulse.delay-2 { animation-delay: 1.2s; }
@keyframes pulse {
  0% { transform: scale(0.6); opacity: 1; }
  100% { transform: scale(1.4); opacity: 0; }
}
.analyzing-card h2 { color: #1e293b; margin: 0 0 8px; }
.analyzing-sub { color: #64748b; font-size: 14px; margin: 0 0 24px; }
.progress-line {
  max-width: 360px; height: 6px; background: #f1f5f9;
  border-radius: 3px; margin: 0 auto 8px; overflow: hidden;
}
.progress-fill {
  height: 100%; background: linear-gradient(90deg, #6366f1, #ec4899);
  transition: width 0.3s; border-radius: 3px;
}
.progress-text { font-size: 13px; color: #64748b; }

/* 结果 */
.result-content { display: flex; flex-direction: column; gap: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-sub { font-size: 12px; color: #94a3b8; }

.req-card, .overview-card, .agents-card, .flow-card {
  border-radius: 14px; background: white;
}

/* 总览 */
.overview-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.overview-item {
  text-align: center; padding: 16px 8px;
  background: linear-gradient(135deg, #fafbfc 0%, #f1f5f9 100%);
  border-radius: 10px;
}
.oi-num { font-size: 22px; font-weight: 700; color: #6366f1; }
.oi-label { font-size: 11px; color: #64748b; margin-top: 4px; }

/* 智能体卡 */
.agent-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.agent-card {
  display: flex; gap: 12px; padding: 16px;
  background: #fafbfc; border-radius: 12px;
  border: 1px solid #f1f5f9; transition: all 0.2s;
}
.agent-card:hover { background: white; box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
.ac-avatar {
  width: 56px; height: 56px; border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  font-size: 28px; flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.ac-body { flex: 1; }
.ac-name { font-weight: 600; color: #1e293b; }
.ac-role { font-size: 12px; color: #6366f1; font-weight: 500; margin: 2px 0 6px; }
.ac-desc { font-size: 13px; color: #64748b; line-height: 1.5; }
.ac-tools { display: flex; gap: 4px; flex-wrap: wrap; margin-top: 8px; }
.ac-model { font-size: 11px; color: #94a3b8; margin-top: 6px; }

/* 流程 */
.flow-canvas {
  display: flex; align-items: center; flex-wrap: wrap; gap: 8px;
  padding: 16px; background: #fafbfc; border-radius: 12px;
}
.flow-step { display: flex; align-items: center; gap: 8px; }
.flow-bubble {
  padding: 10px 16px; background: white; border-radius: 10px;
  display: flex; align-items: center; gap: 8px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}
.fb-num {
  width: 22px; height: 22px; border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #ec4899);
  color: white; display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700;
}
.fb-name { font-size: 13px; font-weight: 500; color: #1e293b; }
.flow-arrow { color: #94a3b8; font-size: 20px; }

.action-bar { display: flex; justify-content: space-between; margin-top: 8px; }
</style>
