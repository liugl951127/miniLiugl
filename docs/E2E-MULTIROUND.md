# V3.5.85+ E2E 多轮压测 (90 轮稳定性 + 5 browser matrix)

## 1. 脚本入口

```bash
# 默认 90 轮
bash scripts/e2e-multiround.sh

# 自定义轮数
ROUNDS=10 bash scripts/e2e-multiround.sh

# 完整 5 browser matrix (需 4GB+ 内存)
FULL_MATRIX=1 ROUNDS=10 bash scripts/e2e-multiround.sh
```

## 2. 7 轮测试架构

| Round | 名称 | 方式 | 沙箱友好 |
|-------|------|------|----------|
| 1 | 服务健康检查 (13 微服务) | curl | ✅ |
| 2 | 用户登录 (5 账号 BCrypt) | curl | ✅ |
| 3 | 业务核心接口 (CRUD + AI + Admin) | curl | ✅ |
| 4 | 多轮意图识别 (上下文继承) | curl | ✅ |
| 5 | 接口覆盖率扫描 (actuator/health) | curl | ✅ |
| 6 | **HTTP curl 稳定性压测 (90 轮 × 21 路由 = 1890 GET)** | curl | ✅ |
| 7 | **HTTP 5 browser matrix 模拟 (chromium/webkit/firefox/mobile-safari/mobile-chrome)** | curl + UA 模拟 | ✅ |

## 3. Round 6 90 轮稳定性

### 3.1 21 路由 (5 段样板 P0-P3 + 容器 + misc)

```
/                       /login                  /h5login
/ai/chat                /monitor                /admin/dashboard
/admin/audit            /admin/metrics          /admin/alerts
/admin/cluster          /admin/traces           /admin/provider
/chat                   /chat/stream            /ai/workflow
/ai/image-gen           /ai/tool-admin          /ai/marketplace
/kg                     /agent                  /admin/framework
```

### 3.2 沙箱 vs CI

| 环境 | 方式 | 轮数 | 内存 |
|------|------|------|------|
| 沙箱 (2GB) | HTTP curl 21 路由 | 90 | 50 MB |
| CI (4GB+) | Playwright 21 case | 90 | 1.5 GB |
| CI (4GB+ FULL_MATRIX) | Playwright 5 browser × 21 case | 90 | 4 GB |

### 3.3 沙箱 90 轮结果示例

```
轮数:      90
每轮:      21 路由 HTTP GET
总请求数:  1890
总耗时:    23s (平均 0s/round)
结果:
  Pass:  90/90 (100%)
  Flaky: 0/90
  Fail:  0/90
```

## 4. Round 7 5 browser matrix

### 4.1 HTTP UA 模拟 (沙箱友好)

| 模拟 browser | User-Agent | 头 |
|--------------|-----------|-----|
| chromium | 隐含 (curl default) | 默认 |
| webkit | Mozilla/5.0 (Macintosh...) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Safari/605.1.15 | Cache-Control: no-cache |
| firefox | Mozilla/5.0 (X11; Linux x86_64; rv:120.0) Gecko/20100101 Firefox/120.0 | --compressed (gzip) |
| mobile-safari | iPhone 16_0 + Safari 604.1 | 默认 |
| mobile-chrome | Pixel 5 + Chrome 120.0 | 默认 |

### 4.2 真 browser matrix (CI 用)

`FULL_MATRIX=1` 时跑 5 真 playwright project:

```yaml
# .github/workflows/ci.yml verify-deploy-summary (V3.5.82+)
verify-deploy:
  strategy:
    matrix:
      browser: [chromium, webkit, firefox, mobile-safari, mobile-chrome]
  steps:
    - npx playwright test --project=${{ matrix.browser }}
```

## 5. 报告输出

```bash
# 默认位置
reports/e2e-multiround/
├── round6-stability.log    # 90 轮稳定性
└── round7-matrix.log       # 5 browser matrix
```

格式:
```
========================================
  V3.5.85+ Round 6 HTTP 稳定性压测报告
  2026-08-01 08:33:02
========================================

  轮数:      90
  每轮:      21 路由 HTTP GET
  总请求数:  1890
  目标:      http://localhost:5173 (frontend dev server)
  总耗时:    23s
  平均:      0s/round

  结果:
    Pass (全 200 + 全 < 3s):  90 / 90
    Flaky (全 200 + 部分慢):   0 / 90
    Fail (有 1+ 路由错):       0 / 90

  通过率: 100%
  Flaky 率: 0%
```

## 6. 验证范围

| 验证项 | Round | 沙箱能跑? |
|--------|-------|----------|
| 13 微服务 actuator/health | 1 + 5 | ✅ 后端需起 |
| 5 账号 BCrypt 登录 | 2 | ✅ 后端需起 |
| AI 意图识别 (5 文本) | 3 | ✅ 后端需起 |
| 业务 CRUD (memory/chat/model/rag) | 3 | ✅ 后端需起 |
| 多轮上下文意图 (3 turn) | 4 | ✅ 后端需起 |
| 21 路由 HTTP GET (V3.5.85 新) | 6 | ✅ 前端需起 |
| 5 browser UA 模拟 (V3.5.85 新) | 7 | ✅ 前端需起 |
| 5 真 browser playwright (V3.5.85 FULL_MATRIX) | 7 | ❌ 4GB+ 内存 |
| 90 轮 playwright 21 case (V3.5.85 FULL) | 6 | ❌ 4GB+ 内存 |

## 7. 集成到 CI

```yaml
# .github/workflows/ci.yml e2e-multiround job
e2e-multiround:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-node@v4
      with: { node-version: '16.20.2' }
    - run: cd frontend && npm ci
    - run: nohup npx vite dev --port 5173 &
    - run: cd .. && ROUNDS=90 FULL_MATRIX=1 bash scripts/e2e-multiround.sh
    - uses: actions/upload-artifact@v4
      with:
        name: e2e-multiround-reports
        path: reports/e2e-multiround/
```

## 8. 累计 40 个版本 (V3.5.46-V3.5.85)
