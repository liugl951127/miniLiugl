# V3.5.97-V3.5.98 H5Login 重写 + ROUNDS=200 极限压测 + chat/Index RAG/Agent 切换

## 1. V3.5.96 之后

V3.5.96 接了 GH Actions + admin/Index 重写。V3.5.97-98 继续:
- **V3.5.97 H5Login.vue** 重写（V3.5.93 排队到现在）
- **V3.5.97 ROUNDS=200** 极限压测
- **V3.5.98 chat/Index.vue** 加 RAG 知识库选择器 + Agent 模式切换

## 2. V3.5.97 改

### 2.1 H5Login.vue V3.5.97 重写 (313 → 448 行)

V3.5.97 新增:
1. **演示模式 tag** (顶栏 + footer 双重提示)
2. **5 演示账号卡片** (admin/adminLiugl/operator/auditor/user) - 移动端 2×2 网格
3. **一键登录** (onDemoLogin) - 模拟 300ms 延迟 + 直接跳 /chat
4. **isDemo 响应式** (onMounted 读 localStorage)
5. **i18n** - h5login.demo.title 键
6. **演示模式 badge** - footer 提示

```vue
<!-- 5 段样板 + 演示账号 卡片 -->
<el-row :gutter="8">
  <el-col v-for="d in demoAccounts" :key="d.username" :xs="12" :sm="8">
    <el-card class="demo-card" @click="onDemoLogin(d)">
      <el-avatar :style="{ background: d.color }">{{ d.avatar }}</el-avatar>
      <el-tag :type="d.tagType">{{ d.role }}</el-tag>
      <div class="demo-name">{{ d.name }}</div>
      <el-button type="primary" plain :icon="Promotion">一键登录</el-button>
    </el-card>
  </el-col>
</el-row>
```

### 2.2 ROUNDS=200 极限压测 (V3.5.97+)

```bash
FRONTEND_PORT=3000 ROUNDS=200 bash scripts/e2e-multiround.sh
# Round 6 总结: 200 轮 × 21 路由 = 4200 HTTP GET
# 耗时 46s, 平均 0.23s/round
# 通过率 100%, Flaky 0
```

**结果**:
- Pass: 200/200 (100%)
- Flaky: 0/200 (0%)
- Fail: 0/200
- 总请求: 4200
- 总耗时: 46s

## 3. V3.5.98 改

### 3.1 chat/Index.vue V3.5.98 加 RAG + Agent 选择器 (704 → 747 行)

V3.5.98 新增:
1. **RAG 知识库选择器** - 5 知识库 (产品手册 128 / 技术文档 256 / 用户 FAQ 64 / 行业知识 512 / 代码片段库 1024)
2. **Agent 模式切换** - 4 模式 (💬 普通对话 / 🤖 Agent 编排 / 📚 RAG 检索 / 🔀 Flow 流程)
3. **showRag 控制** - 默认 true
4. **onRagChange / onAgentModeChange** - 切换时 ElMessage 提示
5. **Document / Share icon 导入** - 给 Agent 模式用

```vue
<!-- V3.5.98+ 工具栏加 2 个选择器 -->
<el-select v-if="showRag" v-model="ragId" size="small" placeholder="知识库" clearable>
  <el-option v-for="k in knowledgeBases" :key="k.id" :label="k.name" :value="k.id">
    <span style="float: left">{{ k.name }}</span>
    <el-tag size="small" type="info" style="float: right">{{ k.docCount }} 文档</el-tag>
  </el-option>
</el-select>

<el-select v-model="agentMode" size="small" @change="onAgentModeChange">
  <el-option v-for="m in agentModes" :key="m.key" :label="m.label" :value="m.key" />
</el-select>
```

```js
// === V3.5.98+ RAG 知识库 ===
const knowledgeBases = ref([
  { id: 1, name: '产品手册', docCount: 128 },
  { id: 2, name: '技术文档', docCount: 256 },
  ...
])

// === V3.5.98+ Agent 模式 ===
const agentModes = ref([
  { key: 'chat',  label: '💬 普通对话', icon: 'ChatDotRound' },
  { key: 'agent', label: '🤖 Agent 编排', icon: 'MagicStick' },
  { key: 'rag',   label: '📚 RAG 检索', icon: 'Document' },
  { key: 'flow',  label: '🔀 Flow 流程', icon: 'Share' },
])
```

## 4. i18n (V3.5.97)

```js
h5login: {
  demo: { title: '演示账号' / 'Demo Accounts' }
}
```

## 5. 验证

| 测试 | 结果 |
|------|------|
| **H5Login.vue** 5 段样板 + 5 演示账号 | ✅ 313 → 448 行 |
| vite build 0 错 | ✅ 50s |
| **check-setup-var.cjs** (Check 8) | ✅ 79 .vue 0 错误 |
| **ci-check 9/9** | ✅ |
| vitest 44/44 | ✅ |
| 21 路由 21/21 200 | ✅ |
| **ROUNDS=200** Round 6 | ✅ 4200 GET 100% pass (46s) |
| Round 7 5 browser | ✅ |
| Round 8 5 browser trace | ✅ |

## 6. 累计 53 个版本 (V3.5.46-V3.5.98)
