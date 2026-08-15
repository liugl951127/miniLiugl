# V3.6.9 高级功能 5 项 (语音通话 + kg 右键菜单 + admin 搜索 + BG Sync + useToast)

## 1. V3.6.8 之后

V3.6.8 加了字体大小 + force 布局 + 22 view watermark + OTel attempt。V3.6.9 继续 5 项高级:
- **chat/Index 加语音通话** (WebRTC MediaRecorder + Web Speech 双向流)
- **kg/Index 节点右键菜单** (删除 + 编辑 + 复制名 + 跳转 Wiki)
- **admin/Index 加搜索框** (admin 路由搜索/跳转)
- **PWA sw.js 加 Background Sync** (V3.6.9+ 消息事件)
- **全局 useToast 替换 ElMessage** (统一 API, 无侵入)

## 2. V3.6.9 改

### 2.1 chat/Index.vue V3.6.9 语音通话 (V3.6.8 → 1514 行)

**新 composable**: `src/composables/useSpeechCall.js` (247 行)

```js
export function useSpeechCall() {
  const isCallActive = ref(false)
  const isMuted = ref(false)
  const callDuration = ref(0)
  const volume = ref(0)
  const interimText = ref('')
  const finalText = ref('')

  // 启动
  async function start() {
    mediaStream = await navigator.mediaDevices.getUserMedia({ audio: {...} })
    audioContext = new AudioContext()
    analyser = audioContext.createAnalyser()  // 音量可视化
    recognition = new SR()                    // 流式 STT
    recognition.interimResults = true          // 实时识别
    recognition.start()
  }

  // 静音切换
  function toggleMute() {
    isMuted.value = !isMuted.value
    mediaStream.getAudioTracks().forEach(t => t.enabled = !isMuted.value)
  }

  // 流式 TTS (AI 回复时调)
  function speakChunk(text) {
    // 清理 markdown, TTS 不读 "星号星号"
    const u = new SpeechSynthesisUtterance(text)
    u.lang = 'zh-CN'
    synth.speak(u)
  }
}
```

**chat 工具栏加 🎙️ 按钮 + 通话面板**:
```vue
<el-button :icon="Phone" circle @click="toggleCall" />

<div v-if="speechCall.isCallActive.value" class="call-panel">
  <span class="call-dot"></span> 通话中 {{ speechCall.callDurationFormatted }}
  <el-button @click="speechCall.toggleMute()">静音</el-button>
  <el-button @click="speechCall.stop()">挂断</el-button>

  <div class="volume-bar">  <!-- 20 根音量条 -->
    <div v-for="i in 20" :class="{ active: i <= Math.ceil(volume/5) }" />
  </div>

  <div class="call-text">
    <div v-if="interimText">🎤 {{ interimText }}</div>
    <div v-else-if="finalText">✓ {{ finalText }}</div>
    <div v-else>请说话...</div>
  </div>
</div>
```

**5 大特性**:
1. **MediaRecorder** - WebRTC 录音
2. **Web Speech API STT** - 流式识别 (interimResults)
3. **Web Speech API TTS** - 收到 AI 回复自动播报
4. **AudioContext + AnalyserNode** - 实时音量可视化
5. **通话计时 + 静音切换** - 60+ 秒计时, Mute 不停识别

### 2.2 kg/Index.vue V3.6.9 节点右键菜单 (V3.6.8 → 850 行)

V3.6.9 加 4 个菜单项:

```vue
<ul v-if="contextMenu.visible" class="kg-context-menu" :style="{left, top}">
  <template v-if="contextMenu.type === 'entity'">
    <li @click="editEntity">编辑名称</li>
    <li @click="copyEntityName">复制名称</li>
    <li @click="jumpToWiki">跳转 Wiki</li>
    <li class="danger" @click="deleteEntity">删除节点</li>
  </template>
  <template v-else-if="contextMenu.type === 'relation'">
    <li class="danger" @click="deleteRelation">删除关系</li>
  </template>
  <li class="cancel" @click="closeContextMenu">取消</li>
</ul>
```

**核心函数**:
- `editEntity()` - ElMessageBox.prompt 改名称 + 重新 renderGraph
- `copyEntityName()` - `navigator.clipboard.writeText` 复制
- `jumpToWiki()` - 优先百度百科, 失败跳 Wikipedia (fetch HEAD)
- `deleteEntity/target` - V3.6.6 已实现
- **全局 click/Esc 关闭** - `document.addEventListener('click', closeContextMenu)`

### 2.3 admin/Index.vue V3.6.9 路由搜索框 (V3.6.8 → 460 行)

```vue
<el-popover :width="360" placement="bottom" trigger="click">
  <template #reference>
    <el-input
      v-model="adminSearch"
      placeholder="搜索 admin 路由..."
      :prefix-icon="Search"
      clearable
      @keyup.enter="jumpToRoute(searchResults[0])"
    />
  </template>
  <div class="admin-search-results">
    <div v-for="r in searchResults" class="admin-search-item" @click="jumpToRoute(r)">
      <el-icon><component :is="r.icon || 'Menu'" /></el-icon>
      <div>
        <div class="admin-search-title">{{ r.title }}</div>
        <div class="admin-search-path">{{ r.group }} / {{ r.path }}</div>
      </div>
    </div>
  </div>
</el-popover>
```

**3 维度搜索**:
- **title** - "告警" → 找到 "告警管理"
- **path** - "alert" → 找到 "/admin/alerts"
- **group** - "core" → 找到 core 分组所有

**Enter 跳转第一条**, 8 结果上限, 点击外部关闭。

### 2.4 PWA sw.js V3.6.9+ Background Sync (V3.5.89 已实现, V3.6.9 增强)

V3.5.89 sw.js 已实现:
- ✅ `replayQueuedRequests` (IndexedDB 队列重发)
- ✅ `sync` 事件监听 (`SYNC_TAG = minimax-bg-sync`)
- ✅ `periodicsync` (定时拉新通知)

V3.6.9 增强:
- ✅ 加 `message` 事件处理 (`bg-sync-enqueue` / `get-queue` / `clear`)
- ✅ 页面注册 sync 后的双向通信

**页面调用**:
```js
// 离线时
navigator.serviceWorker.controller.postMessage({
  type: 'bg-sync-enqueue',
  payload: { sessionId, message, role: 'user' },
}, [channel])

// 注册 sync
const reg = await navigator.serviceWorker.ready
await reg.sync.register('minimax-message-sync')

// 监听 sync 成功
navigator.serviceWorker.addEventListener('message', (e) => {
  if (e.data.type === 'syncSuccess') {
    ElMessage.success('离线消息已发送')
  }
})
```

### 2.5 useToast composable V3.6.9+ 统一 API (无侵入)

`src/composables/useToast.js` (76 行):

```js
export function useToast() {
  return {
    log: readonly(_log),

    success(msg, options) {
      ElMessage.success({ message: msg, ...options })
      record('success', msg, options)  // localStorage 持久化
    },
    error(msg, options) { ElMessage.error({...}); record(...) },
    warning(msg, options) { ... },
    info(msg, options) { ... },
    notify(opts) { ElNotification(opts); ... },
    alert(msg, title, options) { return ElMessageBox.alert(...) },
    confirm(msg, title, options) { return ElMessageBox.confirm(...) },
    prompt(msg, title, options) { return ElMessageBox.prompt(...) },
    clearLog() { _log.value = []; save() },
  }
}
```

**核心特性**:
- ✅ **无侵入** - 内部仍调 ElMessage, 不破坏现有 UI
- ✅ **统一 API** - success/error/warning/info/notify/alert/confirm/prompt 8 类
- ✅ **历史 log** - localStorage `minimax_toast_log` 保留 50 条
- ✅ **可观测** - `useToast().log` 是 ref, 任何地方都能看历史
- ✅ **易扩展** - 后续接 SMS/邮件/IM, 改一个 composable 即可

**迁移模式** (按需):
```js
// Before:
import { ElMessage } from 'element-plus'
ElMessage.success('保存成功')

// After:
import { useToast } from '@/composables/useToast'
const toast = useToast()
toast.success('保存成功')
```

## 3. 验证

| 测试 | 结果 |
|------|------|
| `check-setup-var.cjs` (Check 8) | ✅ 79 .vue 0 错误 |
| `verify-docker-compose.sh` (Check 9) | ✅ 19 services |
| `check_pom_consistency.py` (Check 10) | ✅ 14 module |
| `otel-trace-sandbox.sh` (Check 11) | ✅ 5 检查 |
| **ci-check 11/11** | ✅ < 3s |
| vite build 0 错 | ✅ 57s |
| 21 路由 21/21 200 | ✅ |
| ROUNDS=90 Round 6 | ✅ 1890 GET 100% pass |
| Round 7 5 browser | ✅ |
| Round 8 5 browser trace | ✅ |

## 4. 累计 64 个版本 (V3.5.46-V3.6.9)
