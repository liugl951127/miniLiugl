# V3.0.0 端到端测试报告

> **完整功能验证** · 17 微服务 · 297 单元测试 · 60+ 接口链路测试 · 准生产级 E2E

## 一、测试总览

| 指标 | 数值 | 备注 |
|------|------|------|
| **总测试数** | 297 单元 + 60+ E2E | 单元 + 接口链路 |
| **微服务数** | 17 | 16 业务 + minimax-ai 自研 |
| **测试模块** | 25+ | 工具/算法/控制器 |
| **代码行数** | 105K+ | Java + Vue |
| **总耗时** | < 60s | 全部测试套件 |

## 二、测试维度

### 2.1 单元测试覆盖

| 模块 | 测试数 | 覆盖范围 |
|------|--------|----------|
| **minimax-ai** | 165 | 算法/工具/框架/管线/TB/市场/Webhook |
| **minimax-ws** | 20 | CRDT/WebSocket |
| **minimax-admin** | 7 | 治理 |
| 其它业务 | 105 | 登录/RBAC/支付/文件/统计 |

### 2.2 接口链路 E2E (V3.0.0)

`scripts/test-e2e-v300.sh` 覆盖 **15 大类 60+ 接口**:

1. **基础健康**: gateway/auth/ai/admin/ws 5 个端口
2. **登录流程**: 超级管理员 / 普通用户 / 错误密码
3. **AI 平台**: 意图识别/Pipeline/多模态/工具/框架/TB (24 接口)
4. **Model Market**: 上传/下载/评分/统计 (4 接口)
5. **Agent Marketplace**: 浏览/详情/上传 (3 接口)
6. **Webhook**: CRUD/投递/重试/测试 (4 接口)
7. **治理**: 概览/时间线/异常/合规 (4 接口)
8. **PWA**: manifest/sw.js/offline (3 资源)
9. **性能 SLA**: 5 关键接口 P95 < 500ms
10. **浏览器兼容**: useBrowserCompat + polyfill (6 检查)
11. **SQL 单文件**: init.sql 62 表无重复
12. **路径标准化**: /api/v1 前缀覆盖 >= 80%
13. **AI 算法注释**: 5 核心算法文件详细注释
14. **文档完整性**: 5 企业文档存在
15. **K8s 清理**: 完全去除

### 2.3 AI 算法可观测性

| 算法 | 文件 | 验证方式 |
|------|------|----------|
| **Top-K + Top-P 采样** | ModelInference.java | sampleTopKTopP 单元测试 |
| **EMA 平滑** | TrainingTracker.java | ema 单元测试 |
| **Haversine 距离** | GeoUtils.java | GeoUtils 单元测试 |
| **CRDT 排序** | CrdtEngine.java | renderText 单元测试 |
| **关键词 TF** | KeywordEngine.java | recognize 单元测试 |

### 2.4 浏览器兼容性 (V3.0.0)

| 浏览器 | 最低版本 | 特性 |
|--------|----------|------|
| **Chrome** | 63+ | 现代 ES2017+ |
| **Edge** | 79+ (Chromium) | 同 Chrome |
| **Firefox** | 60+ | 现代 ES2017+ |
| **Safari** | 12+ | 现代 ES2017+ |
| **iOS Safari** | 12+ | 移动端 |
| **Android Chrome** | 8+ | 移动端 |

**降级策略**: useBrowserCompat.js 7 类 polyfill
- structuredClone / crypto.randomUUID
- Array.flat / Object.fromEntries
- String.replaceAll / requestIdleCallback
- AbortController

## 三、测试运行

```bash
# 单元测试
mvn -pl minimax-ai,minimax-ws,minimax-admin,minimax-auth test

# E2E 准生产
bash scripts/test-e2e-v300.sh

# 性能 SLA
bash scripts/test-perf.sh

# 浏览器兼容 (Playwright)
npx playwright test
```

## 四、关键性能 SLA

| 接口 | P95 | 测试方式 |
|------|-----|----------|
| Gateway /actuator/health | < 100ms | curl + time |
| Auth /api/v1/auth/login | < 1000ms | 含 BCrypt 验证 |
| AI intent recognize | < 500ms | 纯 CPU 算法 |
| TB experiments | < 500ms | 内存检索 |
| Model market list | < 500ms | 分页查询 |

## 五、失败兜底

- 所有微服务未启动时, `test-e2e-v300.sh` 自动 SKIP 网络请求
- 仅检查**本地可验证**的指标: 文件存在性/算法注释/SQL 完整性/路径标准化
- 启动后 297 单元 + 60+ E2E 全部通过

## 六、报告生成

```bash
python3 scripts/gen-test-report.py
# → docs/TEST_REPORT.md (本文件)
```
