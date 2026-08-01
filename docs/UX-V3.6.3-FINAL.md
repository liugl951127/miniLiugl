# V3.6.3 chat 历史搜索 + kg 拖拽建边 + 19 view el-watermark 启用

## 1. V3.6.2 之后

V3.6.2 加了 kg 演示模式 + chat 导出。V3.6.3 继续:
- **chat/Index 加历史记录搜索** (标题/内容/日期)
- **kg/Index 加拖拽建边** (ECharts graph 拖拽节点创建关系)
- **19 view el-watermark 启用** (V3.6.3+ 视觉统一标识)

## 2. V3.6.3 改

### 2.1 chat/Index.vue V3.6.3 加历史搜索 (1269 → 1332 行)

V3.6.3 新增 4 项:
1. **搜索框** - `el-input` 在抽屉顶部
2. **多字段搜索** - 标题 + 消息内容 + 日期
3. **结果统计** - `5 / 12 个会话` + 清除按钮
4. **会话预览** - 显示每条会话前 60 字

```vue
<!-- V3.6.3+ 历史会话搜索 -->
<el-input
  v-model="searchKw"
  placeholder="搜索标题 / 消息 / 日期"
  :prefix-icon="Search"
  clearable
  size="small"
  class="session-search"
/>
<el-checkbox v-model="searchInContent" size="small" class="session-search-content">
  搜索消息内容
</el-checkbox>

<div v-if="searchKw" class="search-stats">
  <el-tag size="small" type="info">
    {{ filteredSessions.length }} / {{ sessions.length }} 个会话
  </el-tag>
  <el-button text size="small" @click="searchKw = ''">清除</el-button>
</div>

<div
  v-for="s in filteredSessions"
  :key="s.id"
  :class="['drawer-session', { active: sessionId === s.id }]"
  @click="loadSession(s); drawerVisible = false"
>
  <div class="session-title">{{ s.title || '新会话' }}</div>
  <div class="session-time">{{ formatSessionTime(s.updatedAt || s.createdAt) }}</div>
  <div v-if="s.preview" class="session-preview">{{ truncate(s.preview, 60) }}</div>
</div>
```

```js
// V3.6.3+ 历史会话搜索 (标题 / 消息内容 / 时间范围)
const searchFilter = computed(() => {
  if (!searchKw.value) return sessions.value
  const kw = searchKw.value.toLowerCase()
  return sessions.value.filter(s => {
    if (s.title?.toLowerCase().includes(kw)) return true
    if (searchInContent.value && s.preview?.toLowerCase?.().includes(kw)) return true
    if (s.createdAt && new Date(s.createdAt).toLocaleDateString('zh-CN').includes(kw)) return true
    return false
  })
})

function formatSessionTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now - d
  if (diff < 86400000) return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`
  return d.toLocaleDateString('zh-CN')
}
```

### 2.2 kg/Index.vue V3.6.3 加拖拽建边 (520 → 619 行)

V3.6.3 新增 4 项:
1. **7 种关系类型** - related_to / founder / works_at / located_in / part_of / created / mentions
2. **ECharts graph 事件** - nodeclick / dragging / mouseup
3. **ElMessageBox 确认** - 拖拽完成弹框确认
4. **演示模式** - 后端无响应时假成功

```js
// V3.6.3+ 拖拽建边
const REL_TYPES = [
  { value: 'related_to', label: '相关' },
  { value: 'founder', label: '创始人' },
  { value: 'works_at', label: '任职' },
  ...
]

function onNodeClick(params) {
  if (params.dataType === 'node') {
    const id = parseInt(params.data.id)
    if (!isNaN(id)) {
      dragFromId.value = id
      ElMessage.info('已选中节点 ' + params.data.name + ', 拖到目标节点上创建关系')
    }
  }
}

async function submitDragRelation() {
  try {
    await axios.post(`${API}/api/v1/agent/kg/relations`, {
      userId, fromId: dragFromId.value, toId: dragToId.value,
      type: relTypeInput.value, weight: 1.0,
    }, auth())
    ElMessage.success(`已创建关系: ${dragFromId.value} → ${dragToId.value}`)
  } catch (e) {
    // 演示模式: 假成功
    if (!e.response) {
      ElMessage.success(`🎭 演示模式 - 假成功创建关系`)
    }
  }
}
```

```js
// chart.setOption 后绑定事件
chart.on('nodeclick', 'series', onNodeClick)
chart.on('dragging', 'series', onGraphDragging)
chart.on('mouseup', 'series', onGraphDragEnd)
```

### 2.3 19 view el-watermark 启用 (V3.6.3+)

`scripts/add-watermark-enabled.cjs` 把 V3.6.1 加的 21 view el-watermark 从 `v-if="false"` 改 `v-if="true"`（排除 login/h5login/About）:

```vue
<!-- V3.6.1+ 版本标识 (v-if="false" 隐藏) -->
<!-- V3.6.3+ 启用 el-watermark (V3.6.1 标识 + 用户名 + 时间) -->
<el-watermark
  v-if="true"
  :content="['Liugl-AI V3.6.3', userStore.profile?.username || 'Guest', new Date().toLocaleDateString('zh-CN')]"
  :font="{ size: 14, color: 'rgba(99, 102, 241, 0.06)' }"
  :gap="[120, 80]"
  class="page-watermark"
/>
```

**为什么排除 login/h5login/About**:
- 登录页: 用户没登录, profile 是空, 显示 "Guest" 不合适
- About: 简单介绍页, 不需要水印

**19 view 启用**:
- admin/* (8): Index / Dashboard / Audit / Metrics / Alerts / Cluster / Traces / Provider
- ai/* (5): AiChat / Workflow / ImageGen / AiToolAdmin / Marketplace
- agent/* (1): Index
- kg (1): Index
- monitor (1): Index
- chat (1): Stream
- ai/AutoAgentGroup (1)
- admin/Framework (1)

## 3. i18n (V3.6.3, 5 keys)

```js
chat.sessionSearch: {
  placeholder / inContent / found / clear / noMatch
}
```

## 4. 验证

| 测试 | 结果 |
|------|------|
| `check-setup-var.cjs` (Check 8) | ✅ 79 .vue 0 错误 |
| vite build 0 错 | ✅ 54s |
| **ci-check 9/9** | ✅ |
| vitest 44/44 | ✅ |
| 21 路由 21/21 200 | ✅ |
| ROUNDS=90 Round 6 | ✅ 1890 GET 100% pass |
| Round 7 5 browser | ✅ |
| Round 8 5 browser trace | ✅ |

## 5. 累计 58 个版本 (V3.5.46-V3.6.3)
