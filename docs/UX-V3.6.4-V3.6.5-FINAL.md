# V3.6.4-V3.6.5 el-text/el-space + 拖拽虚线 + 后端试跑 + OTel 沙箱版 + Notification

## 1. V3.6.3 之后

V3.6.3 加了 chat 历史搜索 + kg 拖拽建边 + 19 view watermark。V3.6.4-6.5 继续:
- **V3.6.4 chat/Index 加 el-text / el-space 替换** (EP 2.4 视觉统一)
- **V3.6.4 kg/Index 拖拽虚线箭头** (ECharts markLine)
- **V3.6.4 后端 mvn install 沙箱试跑** (失败, 给 attempt 报告)
- **V3.6.4 OTel Trace 沙箱友好版** (无需 docker, 静态验证)
- **V3.6.5 chat Notification API** (PWA 浏览器通知)

## 2. V3.6.4 改

### 2.1 chat/Index.vue V3.6.4 el-text / el-space (1332 → 1341 行)

V3.6.4 替换 3 处散乱 `<p>/<div>`:
1. **page-subtitle** `<p class="page-subtitle">` → `<el-text type="info" size="small" truncated>`
2. **hint** `<span class="hint">` → `<el-text type="info" size="small">`
3. **input-toolbar** `<div class="input-toolbar">` → `<el-space :size="8" wrap>`

```vue
<!-- V3.6.4+ el-text 替换 <p> -->
<el-text type="info" size="small" class="page-subtitle" truncated>
  {{ modelLabel }} · {{ sessionId || '新会话' }}
</el-text>

<!-- V3.6.4+ el-space 替换 <div> -->
<el-space :size="8" wrap class="input-toolbar">
  <el-checkbox v-model="useStream">流式</el-checkbox>
  <el-checkbox v-model="useTools">{{ t('chat.tools') }}</el-checkbox>
  ...
</el-space>
```

### 2.2 kg/Index.vue V3.6.4 拖拽虚线箭头 (619 → 690 行)

V3.6.4 新增 4 项:
1. **dragLine state** - 虚线两点
2. **updateDragLine** - 拖拽时实时更新
3. **ECharts markLine** - `type: 'dashed'`, `symbol: ['none', 'arrow']`, `color: '#3b82f6'`
4. **MessageBox 内嵌 el-select** - 关系类型选择 UI

```js
const dragLine = ref(null)
const dragLinePoints = ref<[number, number][]>([])

function updateDragLine(fromX, fromY, toX, toY) {
  dragLinePoints.value = fromX && toX ? [[fromX, fromY], [toX, toY]] : []
}
```

```js
// chart.setOption 加 markLine
chart.setOption({
  series: [{
    type: 'graph',
    markLine: {
      silent: true,
      symbol: ['none', 'arrow'],
      lineStyle: { type: 'dashed', color: '#3b82f6', width: 2, curveness: 0.2 },
      data: dragLinePoints.value.length === 2
        ? [{ coord: dragLinePoints.value[0] }, { coord: dragLinePoints.value[1] }]
        : [],
    },
  }],
})
```

### 2.3 V3.6.4 后端 mvn install 沙箱试跑 (Attempt)

**结果**: ❌ 沙箱无 Java/Maven/Docker

```bash
$ which java mvn
(not found)

$ apt-get install -y openjdk-17-jdk-headless maven
E: Unable to locate package openjdk-17-jdk-headless
E: Unable to locate package maven
```

**替代**: 
- ✅ `scripts/check_pom_consistency.py` 静态检查 14 module pom.xml (Check 10)
- ✅ CI 跑真 mvn install (V3.5.65+ backend job)
- 报告: `reports/V3.6.4-MVN-INSTALL-ATTEMPT.md`

### 2.4 V3.6.4 OTel Trace 沙箱友好版 (Check 11)

`scripts/otel-trace-sandbox.sh` 5 项检查 (无需 docker):

| 检查 | 结果 |
|------|------|
| sw.js 含 withTraceparent 函数 | ✅ 4 处 |
| docker-compose otel-collector service | ✅ 存在 |
| docker-compose jaeger service | ✅ 存在 |
| 12 module depends_on otel-collector | ✅ |
| OTEL_* env vars (jvm-env) | ✅ 9 个 |
| otel-collector-config.yaml 4317 + jaeger | ✅ |
| 沙箱模拟 5 browser trace | ✅ |

## 3. V3.6.5 chat Notification API (1341 → 1412 行)

V3.6.5 新增 5 项:
1. **checkNotificationSupport** - `'Notification' in window`
2. **requestNotificationPermission** - `Notification.requestPermission()` async
3. **showNotification** - `new Notification(title, { body, icon, tag })`
4. **onDone 加通知** - AI 答完触发
5. **🔔 工具栏按钮** - 开启/关闭通知

```js
// V3.6.5+ 浏览器通知
function checkNotificationSupport() {
  if (typeof window === 'undefined') return
  notificationSupported.value = 'Notification' in window
  if (notificationSupported.value) {
    notificationPermission = Notification.permission
    notificationEnabled.value = notificationPermission === 'granted'
  }
}

async function requestNotificationPermission() {
  const permission = await Notification.requestPermission()
  notificationPermission = permission
  notificationEnabled.value = permission === 'granted'
}

function showNotification(title, body) {
  if (!notificationEnabled.value) return
  if (document.visibilityState === 'visible') return  // 页面可见不通知
  new Notification(title, {
    body: body.slice(0, 100),
    icon: '/icons/icon-192.svg',
    tag: 'minimax-chat',
  })
}
```

```vue
<!-- V3.6.5+ 通知按钮 -->
<el-button
  v-if="notificationSupported"
  :icon="notificationEnabled ? BellFilled : Bell"
  :type="notificationEnabled ? 'success' : 'default'"
  @click="requestNotificationPermission"
>
  {{ notificationEnabled ? '🔔' : '🔕' }}
</el-button>
```

## 4. ci-check 11/11 (V3.6.4+)

| Check | 名称 | 工具 |
|-------|------|------|
| 1-7 | (V3.5.65-V3.5.87 之前的 7 项) | bash + python |
| **8** | `<script setup>` 防御性自检 | **node (V3.5.95)** |
| **9** | docker-compose 静态验证 (19 services) | **python (V3.5.95)** |
| **10** | pom.xml 一致性 (14 module) | **python (V3.6.4)** |
| **11** | OTel Trace 沙箱友好 (5 检查) | **bash (V3.6.4)** |

## 5. 验证

| 测试 | 结果 |
|------|------|
| `check-setup-var.cjs` (Check 8) | ✅ 79 .vue 0 错误 |
| `verify-docker-compose.sh` (Check 9) | ✅ 19 services 干净 |
| `check_pom_consistency.py` (Check 10) | ✅ 14 module |
| `otel-trace-sandbox.sh` (Check 11) | ✅ 5 检查全 pass |
| vite build 0 错 | ✅ 56s |
| 21 路由 21/21 200 | ✅ |
| ROUNDS=90 Round 6 | ✅ 1890 GET 100% pass |
| Round 7 5 browser | ✅ |
| Round 8 5 browser trace | ✅ |

## 6. 累计 60 个版本 (V3.5.46-V3.6.5)
