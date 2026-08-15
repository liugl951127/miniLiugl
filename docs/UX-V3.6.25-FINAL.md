# V3.6.25 5 项升级 - UX 报告

## 1. useToast 实际替换 (Alerts.vue 16 处)

**实施迁移示范**:
- Alerts.vue 16 个 ElMessage → toast
- 7 × success, 7 × error, 2 × warning
- useToast import + `const toast = useToast()`
- 移除 ElMessage import

**核心 API** (8 个):
```js
toast.success(msg)    // 绿色 ✅
toast.error(msg)      // 红色 ❌
toast.warning(msg)    // 黄色 ⚠️
toast.info(msg)       // 灰色 ℹ️
toast.notify(msg)     // 通知
toast.alert(msg)      // 弹窗
toast.confirm(msg)    // 确认
toast.prompt(msg)     // 输入
```

## 2. kg/Index 节点拖动 + force 物理引擎升级

**V3.6.8 vs V3.6.25**:
- V3.6.8: 4 个 slider 调 repulsion/gravity/edgeLength/friction
- V3.6.25: 4 预设 (compact/cluster/spread/tree) + save/restore/reset

**4 预设**:
- 📦 紧凑 (compact): repulsion 50, gravity 0.15, edgeLength 50
- 🌀 聚类 (cluster): repulsion 200, gravity 0.03, edgeLength 100
- 🌐 分散 (spread): repulsion 400, gravity 0.02, edgeLength 150
- 🌳 树形 (tree): repulsion 150, gravity 0.08, edgeLength 80

**save/restore/reset**:
- `saveLayout()`: 当前节点位置 → localStorage
- `restoreLayout()`: 还原 + 节点 fixed (不参与 force)
- `resetLayout()`: 清除位置, 重新 force 计算

## 3. admin/Dashboard TopN 请求接口表

**横向条形图** (10 mock 接口):
```
/api/v1/chat/send          4521  245ms  0.5%
/api/v1/auth/login         3892   89ms  0.2%
/api/v1/kg/entities/search 2145  312ms  1.2%
/api/v1/rag/query          1876  567ms  0.8%
/api/v1/agent/execute      1534 1234ms  2.1%  ← 红
/api/v1/models/list        1289   56ms  0.1%
/api/v1/memory/store        967  178ms  0.4%
/api/v1/admin/users         654  234ms  0.0%
/api/v1/prompts/list        432   89ms  0.0%
/api/v1/audit/logs          387  156ms  0.0%
```

**3 段色阶** (按 errorRate):
- 红 (>1.5%): #ef4444
- 黄 (0.5%-1.5%): #f59e0b
- 绿 (<0.5%): #10b981

**el-table 详细** (路径/请求数/平均ms/错误率):
- 平均ms > 500 红 / 200-500 黄 / <200 绿
- refresh 按钮: 随机微调

## 4. chat/Index 字体大小 el-segmented

**4 段** (V3.6.25 整合 V3.6.8):
- 小 (small): 12px
- 中 (medium): 14px
- 大 (large): 16px
- 超大 (xlarge): 18px

**localStorage 持久化**: `minimax_chat_fontsize`

## 5. 后端 14 module mvn install attempt

**沙箱限制**: 2GB 内存 + 无 Java/Maven/Docker
- 报告: `reports/V3.6.25-MVN-DOCKER-ATTEMPT.md`
- 入口: `scripts/mvn-docker.sh` (Vite 沙箱友好)

**14 module 列表**:
| # | 模块 | 端口 |
|---|------|------|
| 1 | minimax-gateway | 8080 |
| 2 | minimax-auth | 9001 |
| 3 | minimax-chat | 9005 |
| 4 | minimax-model | 9003 |
| 5 | minimax-agent | 9010 |
| 6 | minimax-rag | 9006 |
| 7 | minimax-multimodal | 9088 |
| 8 | minimax-pipeline | 9085 |
| 9 | minimax-monitor | 9089 |
| 10 | minimax-admin | 9087 |
| 11 | minimax-analytics | 9090 |
| 12 | minimax-audit | 9091 |
| 13 | minimax-ai | (合并) |
| 14 | minimax-common | (共享) |

**3 替代方案**:
- CI 端到端 (V3.5.65+ backend job)
- 本地开发 (apt-get install)
- 静态检查 (Check 10: pom.xml 一致性)

## 累计 80 个版本 (V3.5.46-V3.6.25)
