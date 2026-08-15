# V3.5.78+ verify-deploy.sh CI 集成

## 1. 概述

`scripts/verify-deploy.sh` 集成到 GitHub Actions, push 触发跨浏览器 5 project E2E.

## 2. CI 流水线 (9 jobs)

| Job | 作用 | 触发 |
|-----|------|------|
| backend | 后端 14 module 编译 + test | push / PR |
| frontend | 前端 build | push / PR |
| frontend-unit | vitest 单元测试 | push / PR |
| frontend-e2e | Playwright E2E (chromium) | push / PR |
| docker | Docker 镜像构建 (main 分支) | push main |
| integration | 后端集成测试 | push main |
| ci-check | 6 项硬性检查 (schema/JDBC/Driver/Dockerfile/mapper/seed-data) | push / PR |
| **verify-deploy** (V3.5.78+) | **跨浏览器 5 project E2E (chromium/webkit/firefox)** | **push / PR** |
| notify | 总结状态 | always |

## 3. verify-deploy job 步骤

```yaml
verify-deploy:
  name: V3.5.78 跨浏览器 5 browser E2E 验证
  runs-on: ubuntu-latest
  timeout-minutes: 30

  steps:
    - Checkout
    - Set up JDK 17 (后端编译用)
    - Set up Node.js 18 (前端 + playwright)
    - npm ci (前端依赖)
    - mvn dependency:go-offline (后端依赖缓存)
    - npx playwright install --with-deps chromium webkit firefox (装 3 浏览器 binary)
    - bash scripts/verify-deploy.sh chromium (主验证, 5 browser 全跑)
    - bash scripts/verify-deploy.sh webkit (Safari 兼容)
    - bash scripts/verify-deploy.sh firefox (Firefox 兼容)
    - Upload reports/verify-deploy/ + e2e-report/ (artifact 14 days)
```

## 4. verify-deploy.sh 6 步

1. **Node 版本检查** - 必须 >=16.20.0 (V3.5.74+ engines 锁)
2. **后端 14 module 编译** - `mvn install -DskipTests -T 4`
3. **前端依赖检查** - `node_modules` 存在
4. **前端 build** - `npx vite build` (~55s)
5. **启动 dev server** - `npx vite dev --port 5188`
6. **跨浏览器 E2E** - `npx playwright test --project={browser}`

## 5. 用法

```bash
# 本地跑
bash scripts/verify-deploy.sh chromium    # 默认
bash scripts/verify-deploy.sh webkit
bash scripts/verify-deploy.sh firefox
bash scripts/verify-deploy.sh all         # 5 browser

# 沙箱 OOM 保护
SKIP_BACKEND=1 bash scripts/verify-deploy.sh chromium

# CI 跑 (自动, push 触发)
# GitHub Actions → verify-deploy job → 3 步跑 3 浏览器
```

## 6. 报告

输出: `reports/verify-deploy/verify-{timestamp}.log`

CI 跑时上传:
- `reports/verify-deploy/` (verify-deploy 日志)
- `frontend/e2e-report/` (playwright HTML 报告)
- retention 14 days

下载: GitHub Actions → verify-deploy job → Artifacts → verify-deploy-reports

## 7. 跨浏览器 E2E 覆盖

| 浏览器 | Playwright project | 状态 |
|--------|-------------------|------|
| Desktop Chrome | chromium | ✓ 主 |
| Desktop Safari | webkit | ✓ V3.5.77+ |
| Desktop Firefox | firefox | ✓ V3.5.77+ |
| iPhone 13 Safari | mobile-safari | 配好待跑 |
| Pixel 5 Chrome | mobile-chrome | 配好待跑 |

12 case cross-browser spec:
- 9 polyfill: localStorage / CSS 变量 / async-await / ?. / ?? / fetch / AbortController / IntersectionObserver
- 3 响应式: 桌面 1280 / 平板 768 / 手机 375

## 8. 跟 V3.5.65 ci-check 区别

| 维度 | V3.5.65 ci-check | V3.5.78 verify-deploy |
|------|------------------|---------------------|
| 跑什么 | shell + Python 脚本 | Playwright 跨浏览器 |
| 时长 | 几秒 | 3-10 分钟 |
| 失败影响 | 立即 fail PR | 失败 PR fail |
| 沙箱保护 | 无需 | SKIP_BACKEND=1 |
| 主要拦 | 错位 (schema/yml/mapper/seed) | 浏览器兼容 + UI 行为 |
