# V3.6.2 kg 知识图谱可视化 + chat 导出 (Markdown/JSON/纯文本)

## 1. V3.6.1 之后

V3.6.1 加了 chat OCR + el-segmented + 22 view watermark。V3.6.2 继续:
- **kg/Index 加 演示模式 mock 数据** — 10 实体 + 5 关系 (无后端也能可视化)
- **chat/Index 加 导出按钮** — Markdown / JSON / 纯文本 (一键下载会话)

## 2. V3.6.2 改

### 2.1 kg/Index.vue V3.6.2 加 演示模式 (476 → 520 行)

V3.6.2 新增 4 项:
1. **10 实体 mock** — 李彦宏/百度/北京/AI/深度学习/Transformer/OpenAI/Sam Altman/GPT-4/硅谷
2. **5 关系 mock** — founder/main_product/headquarters/core_tech/competitor
3. **3 秒兜底** — 后端无响应时自动加载 mock
4. **演示模式提示** — `🎭 演示模式 - 已加载 10 实体 + 5 关系 (无后端)`

```js
// V3.6.2+ 演示模式 (无后端) 加载 mock 数据
const MOCK_ENTITIES = [
  { id: 1, name: '李彦宏', entityType: 'person', ... },
  { id: 2, name: '百度', entityType: 'org', ... },
  ...
]

const MOCK_NEIGHBORS = [
  { entity: MOCK_ENTITIES[1], hop: 1, via: 'founder' },
  { entity: MOCK_ENTITIES[3], hop: 1, via: 'main_product' },
  ...
]

function loadMockData() {
  entities.value = MOCK_ENTITIES
  if (!selectedEntity.value) {
    selectedEntity.value = MOCK_ENTITIES[0]
    neighbors.value = MOCK_NEIGHBORS
  }
  ElMessage.info('🎭 演示模式 - 已加载 10 实体 + 5 关系 (无后端)')
}

onMounted(async () => {
  await nextTick()
  if (chartEl.value && !chart) chart = echarts.init(chartEl.value)
  // 3 秒后无响应自动加载 mock
  const timeoutId = setTimeout(() => {
    if (entities.value.length === 0) {
      loadMockData()
      nextTick(() => renderGraph())
    }
  }, 3000)
  try {
    await doSearch()
    clearTimeout(timeoutId)
  } catch {
    loadMockData()
    nextTick(() => renderGraph())
  }
})
```

### 2.2 chat/Index.vue V3.6.2 加 导出按钮 (1186 → 1269 行)

V3.6.2 新增 5 项:
1. **导出下拉** — 顶部清空按钮旁加 `<el-dropdown>`
2. **3 种格式** — Markdown (含工具调用 + 来源) / JSON (结构化) / 纯文本
3. **自动文件名** — `chat-2026-08-01T12-30-00.md` (ISO 8601)
4. **Blob + URL.createObjectURL** — 纯前端下载, 不需要后端
5. **i18n** — chat.export 6 键

```vue
<!-- V3.6.2+ 导出按钮 -->
<el-dropdown @command="onExport" size="small">
  <el-button :icon="Download" plain>
    导出 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
  </el-button>
  <template #dropdown>
    <el-dropdown-menu>
      <el-dropdown-item command="markdown">📝 Markdown</el-dropdown-item>
      <el-dropdown-item command="json">📋 JSON</el-dropdown-item>
      <el-dropdown-item command="txt">📄 纯文本</el-dropdown-item>
    </el-dropdown-menu>
  </template>
</el-dropdown>
```

```js
// V3.6.2+ 导出 (Markdown / JSON / 纯文本)
function onExport(format) {
  if (!messages.value.length) {
    ElMessage.warning('暂无消息可导出')
    return
  }
  // ... 3 种格式生成
  // Blob + URL.createObjectURL + a.click() 触发下载
}
```

**Markdown 输出示例**:
```markdown
## 用户

*2026-08-01 12:30:00*

什么是 Transformer?

---

## AI

*2026-08-01 12:30:15*

Transformer 是 2017 年 Google 提出的...

<details><summary>工具调用 (1)</summary>

- **search** (ok) - 120ms
</details>

> 来源:
> 1. Transformer 论文
```

## 3. i18n (V3.6.2, 6 keys)

```js
chat.export: {
  title / markdown / json / txt / empty / success
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

## 5. 累计 57 个版本 (V3.5.46-V3.6.2)
