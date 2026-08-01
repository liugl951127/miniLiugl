# V3.6.8 深度优化 4 项 (字体 + force + watermark + OTel attempt)

## 1. V3.6.7 之后

V3.6.7 加了 kg 节点搜索 + chat 响应深度 + mvn 沙箱。V3.6.8 继续深度优化:
- **chat/Index 加字体大小** (V3.6.8+ 移动端可读性)
- **kg/Index 加 force 布局调整** (V3.6.8+ ECharts graph 实时调参)
- **22 view el-watermark 自定义** (V3.6.8+ 用户名 + 角色 + 时间)
- **V3.5.91 otel-trace workflow 真跑 attempt** (沙箱无 docker, 给报告)

## 2. V3.6.8 改

### 2.1 chat/Index.vue V3.6.8 字体大小 (V3.6.7 → 1453 行)

V3.6.8 新增 3 项:
1. **fontSize 单选** - 小 / 中 / 大 / 超大
2. **localStorage 持久化** - 跨会话记住
3. **动态 class** - `.font-small/.font-medium/.font-large/.font-xlarge`

```vue
<el-segmented
  v-model="fontSize"
  :options="fontSizeOptions"
  size="small"
  class="font-size-segmented"
/>
```

```js
const fontSize = ref(localStorage.getItem('minimax_chat_fontsize') || 'medium')
const fontSizeOptions = computed(() => [
  { label: '小', value: 'small' },
  { label: '中', value: 'medium' },
  { label: '大', value: 'large' },
  { label: '超大', value: 'xlarge' },
])
watch(fontSize, (v) => localStorage.setItem('minimax_chat_fontsize', v))
```

```css
.font-small .msg-content { font-size: 12px; }
.font-medium .msg-content { font-size: 14px; }
.font-large .msg-content { font-size: 16px; }
.font-xlarge .msg-content { font-size: 18px; }
```

### 2.2 kg/Index.vue V3.6.8 Force 布局调整 (V3.6.7 → 841 行)

V3.6.8 新增 4 项:
1. **forceConfig 状态** - 斥力 / 引力 / 边长 / 摩擦力
2. **el-slider 实时调参** - 4 个 slider
3. **applyForceLayout** - ECharts 重新计算布局
4. **centerGraph** - 居中缩放

```vue
<div class="kg-force-panel">
  <el-space :size="8" wrap>
    <span>斥力: {{ forceConfig.repulsion }}</span>
    <el-slider v-model="forceConfig.repulsion" :min="20" :max="500" :step="10" style="width: 100px" />
    <span>引力: {{ forceConfig.gravity.toFixed(2) }}</span>
    <el-slider v-model="forceConfig.gravity" :min="0.01" :max="0.3" :step="0.01" style="width: 100px" />
    <span>边长: {{ forceConfig.edgeLength }}</span>
    <el-slider v-model="forceConfig.edgeLength" :min="30" :max="200" :step="10" style="width: 100px" />
    <el-button size="small" type="primary" :icon="Refresh" @click="applyForceLayout">应用</el-button>
    <el-button size="small" :icon="Position" @click="centerGraph">居中</el-button>
  </el-space>
</div>
```

```js
function applyForceLayout() {
  chart.setOption({
    series: [{
      type: 'graph',
      force: {
        repulsion: forceConfig.value.repulsion,
        gravity: forceConfig.value.gravity,
        edgeLength: forceConfig.value.edgeLength,
        friction: forceConfig.value.friction,
      },
    }],
  })
  ElMessage.success('已应用 force 布局')
}
```

### 2.3 22 view el-watermark V3.6.8+ 增强 (用户名 + 角色 + 时间)

`scripts/add-watermark-custom.cjs` 把 V3.6.3 启用的 19 view watermark 升级:

```vue
<!-- V3.6.8+ 增强 el-watermark (用户名 + 角色 + 时间) -->
<el-watermark
  v-if="true"
  :content="[
    'Liugl-AI V3.6.8',
    userStore.profile?.username || 'Guest',
    (userStore.profile?.roles || ['USER'])[0],
    new Date().toLocaleString('zh-CN')
  ]"
  :font="{ size: 12, color: 'rgba(99, 102, 241, 0.05)' }"
  :gap="[160, 100]"
/>
```

**4 行水印**:
1. **品牌版本**: "Liugl-AI V3.6.8"
2. **用户名**: userStore.profile?.username (无登录显示 Guest)
3. **角色**: `roles[0]` (SUPER_ADMIN / ADMIN / USER 等)
4. **时间**: 当前本地时间

**透明度 0.05**: 视觉上不打扰, 但截图时能看见 (反截图溯源)

### 2.4 V3.5.91 otel-trace workflow 真跑 attempt

**结果**: ❌ 沙箱无 Docker, 无法起 otel-collector + jaeger

**替代**:
- ✅ `scripts/otel-trace-sandbox.sh` (Check 11) 静态验证 OTel 配置
- ✅ 报告 `reports/V3.6.8-OTEL-TRACE-ATTEMPT.md` 详细说明
- CI 真跑 (V3.5.91 workflow) 需 GitHub Actions runner

## 3. 验证

| 测试 | 结果 |
|------|------|
| `check-setup-var.cjs` (Check 8) | ✅ 79 .vue 0 错误 |
| `verify-docker-compose.sh` (Check 9) | ✅ 19 services |
| `check_pom_consistency.py` (Check 10) | ✅ 14 module |
| `otel-trace-sandbox.sh` (Check 11) | ✅ 5 检查 |
| **ci-check 11/11** | ✅ < 3s |
| vite build 0 错 | ✅ 1m 2s |
| 21 路由 21/21 200 | ✅ |
| ROUNDS=90 Round 6 | ✅ 1890 GET 100% pass |
| Round 7 5 browser | ✅ |
| Round 8 5 browser trace | ✅ |

## 4. 累计 63 个版本 (V3.5.46-V3.6.8)
