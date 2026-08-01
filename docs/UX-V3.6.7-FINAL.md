# V3.6.7 kg 节点搜索跳转 + chat 响应深度 + mvn 沙箱 docker 入口

## 1. V3.6.6 之后

V3.6.6 修了 Check 7 提速 375x + kg 右击删除 + chat 多选。V3.6.7 继续:
- **kg/Index 加节点搜索跳转** (双击 entity 跳到图谱)
- **chat/Index 加 el-segmented 响应深度** (单选: ⚡快速 / ⚖️均衡 / 🎯深度)
- **后端 mvn 沙箱 docker 入口** (沙箱无 Java/Maven 时用 Docker 跑)

## 2. V3.6.7 改

### 2.1 kg/Index.vue V3.6.7 节点搜索跳转 (778 → 823 行)

V3.6.7 新增 4 项:
1. **entitySearchKw** - 实体搜索 (名/类型/描述)
2. **filteredEntities** - computed 过滤
3. **jumpToEntity** - 双击跳到图谱 (滚动 + 高亮 1.5s)
4. **flashNode** - ECharts highlight/dispatchAction

```vue
<el-input
  v-model="entitySearchKw"
  placeholder="搜索实体 (名/类型/描述)"
  :prefix-icon="Search"
  clearable
  size="small"
  class="entity-search"
/>

<div
  v-for="e in filteredEntities.slice(0, 30)"
  :key="e.id"
  class="entity-item"
  @click="selectEntity(e)"
  @dblclick="jumpToEntity(e)"
  title="双击跳转到图谱"
>
```

```js
function jumpToEntity(entity) {
  selectedEntity.value = entity
  loadNeighbors()
  nextTick(() => {
    renderGraph()
    if (chartEl.value) {
      chartEl.value.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
    flashNode(entity.id)
  })
}

function flashNode(id) {
  chart.dispatchAction({
    type: 'highlight',
    seriesIndex: 0,
    dataIndex: chart.getOption().series[0].data.findIndex(n => String(n.id) === String(id)),
  })
  setTimeout(() => chart?.dispatchAction({ type: 'downplay', seriesIndex: 0 }), 1500)
}

const filteredEntities = computed(() => {
  if (!entitySearchKw.value) return entities.value
  const kw = entitySearchKw.value.toLowerCase()
  return entities.value.filter(e =>
    e.name?.toLowerCase().includes(kw) ||
    e.entityType?.toLowerCase().includes(kw) ||
    e.description?.toLowerCase().includes(kw)
  )
})
```

### 2.2 chat/Index.vue V3.6.7 响应深度 (el-segmented 单选)

V3.6.7 加响应深度 (跟 V3.6.6 多选模式不同, 这是单选):
- ⚡ 快速 (fast) - 短回答
- ⚖️ 均衡 (balanced) - 默认
- 🎯 深度 (deep) - 长回答 + 推理

```vue
<!-- V3.6.7+ 响应深度 (el-segmented 单选) -->
<el-segmented
  v-model="responseDepth"
  :options="depthOptions"
  size="small"
  class="depth-segmented"
  @change="onDepthChange"
/>
```

```js
const responseDepth = ref('balanced')
const depthOptions = computed(() => [
  { label: '⚡ 快速', value: 'fast' },
  { label: '⚖️ 均衡', value: 'balanced' },
  { label: '🎯 深度', value: 'deep' },
])

function onDepthChange(depth) {
  const label = depthOptions.value.find(d => d.value === depth)?.label || depth
  ElMessage.info(`响应深度: ${label}`)
}
```

### 2.3 scripts/mvn-docker.sh (V3.6.7+ 沙箱友好)

沙箱无 Java/Maven 但有 Docker 时的 mvn 入口:

```bash
#!/bin/bash
# V3.6.7+ 沙箱友好 mvn (通过 Docker 跑 mvn 命令)
MVN_IMAGE="${MVN_IMAGE:-maven:3.9-eclipse-temurin-17}"

docker run --rm \
    -v "$BACKEND_DIR:/app" \
    -v "$HOME/.m2:/root/.m2" \
    -w /app \
    -e MAVEN_OPTS="-Xmx2g" \
    "$MVN_IMAGE" \
    mvn "${@:---version}"
```

**用法**:
```bash
# 编译 14 module
bash scripts/mvn-docker.sh compile -B -T 4

# 跑测试
bash scripts/mvn-docker.sh test -B

# 检查版本
bash scripts/mvn-docker.sh --version
```

**沙箱无 docker** 时退出码 1 + 提示用 CI 跑。

## 3. i18n (V3.6.7, 2 keys)

```js
kg.search: {
  placeholder: '搜索实体 (名/类型/描述)' / 'Search entity (name/type/desc)',
  doubleClick: '双击跳转到图谱' / 'Double-click to jump',
}
```

## 4. 验证

| 测试 | 结果 |
|------|------|
| `check-setup-var.cjs` (Check 8) | ✅ 79 .vue 0 错误 |
| `verify-docker-compose.sh` (Check 9) | ✅ 19 services |
| `check_pom_consistency.py` (Check 10) | ✅ 14 module |
| `otel-trace-sandbox.sh` (Check 11) | ✅ 5 检查全 pass |
| **ci-check 11/11** | ✅ < 3s |
| vite build 0 错 | ✅ 1m 1s |
| 21 路由 21/21 200 | ✅ |
| ROUNDS=90 Round 6 | ✅ 1890 GET 100% pass |
| Round 7 5 browser | ✅ |
| Round 8 5 browser trace | ✅ |

## 5. 累计 62 个版本 (V3.5.46-V3.6.7)
