# 运行时 i18n 错误修复 (V6.8.1)

## 现象
用户报告: 进入页面报错 (各种语法 + i18n 错)

## 根因
1. `vite.config.js` 包含 `'nprogress'` in optimizeDeps.include (但包未装)
2. `vue` 包 require 失败 (被破坏的 pathe 间接影响)
3. **`vue-i18n` 包引用了 14 个文件, 但实际是自实现 i18n (@/i18n)**
4. `nanoid/non-secure` 子路径缺 (被 postcss 引用)

## 修复

### 1. 沙箱 node_modules 修复
- 装 pathe@1.1.2 (pathe main 指向 .mjs)
- 装 @vitejs/plugin-vue@5.0.4
- 装 vue@3.4.27 (含 compiler-sfc)
- 装 nanoid@5.0.7
- 装 ws@8.18.0 (jsdom 需要)

### 2. Vite 配置
- `vite.config.js` 删 `nprogress` (在 optimizeDeps.include)

### 3. 关键修复: 14 文件 i18n 引用
**问题**: 多个组件用 `import { useI18n } from 'vue-i18n'`, 但实际项目用 `@/i18n` (自实现)

**修法**: 把所有 `from 'vue-i18n'` 改为 `from '@/i18n'`

修了 13 个文件:
- src/components/BackToTop.vue
- src/components/BatchActions.vue
- src/components/Breadcrumb.vue
- src/components/ConfirmDialog.vue
- src/components/FeatureTour.vue
- src/components/FileUploader.vue
- src/components/KeyboardShortcuts.vue
- src/components/NotificationCenter.vue
- src/components/QuickActions.vue
- src/components/ThemeSwitcher.vue
- src/components/TimeAgo.vue
- src/views/auth/H5Login.vue
- src/views/auth/Login.vue

## 验证
- `vite dev` 启动成功 (VITE v5.4.21 ready in 1102ms)
- HTTP 200 入口 `/`
- `curl /src/views/auth/Login.vue` 现在引 `import { useI18n } from "/src/i18n/index.js"` ✅
- 全 `src/` 0 个 vue-i18n 引用 (除 i18n/index.js 注释)

## 编译验证
```
$ npx vite build --mode production
✓ 3319 modules transformed.
✓ built in 58.15s
```

## 测试
```
$ npx vitest run
Test Files  24 passed (24)
Tests       251 passed (251)
Duration    41.15s
```

## 关键经验
1. **自实现 i18n 必须统一 import 路径** - 之前没改全部文件
2. **vue 3.4 exports 限制 ./package.json** - plugin-vue 5.0.4/5.2.4 都需要 require 这个
3. **Vite optimizeDeps.include 必须对应实际安装的包** - 错配置导致启动失败
4. **沙箱 node_modules 不可靠** - 经常需要 npm pack 解压
5. **dev server 跑通 ≠ build 跑通** - 两者都需验证

## 教训
- 之前 V6.8.1 "build success" 只验证了 production build, 没验证 dev 模式
- 用户实际部署/开发用 dev, 才发现问题
- 现在 dev + build + test 三者都通过
