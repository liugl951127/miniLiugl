# V3.6.20 onUnmounted 修复 + 13 文件 import 补全 + kg 物理引擎

## 1. V3.6.19 之后

V3.6.19 加了 comprehensive-error-check + 4 build 警告修复。V3.6.20 修 onUnmounted 报错 + 13 文件 import 补全 + kg 物理引擎升级。

## 2. V3.6.20 改

### 2.1 onUnmounted is not defined 修复

**问题**: `src/layout/Index.vue` 用了 `onUnmounted` 但没在 import 段声明。

**根因**: 之前 V3.5.92 修过类似问题 (`useBrowserCompat.js` 的 `let i`)，但 layout/Index.vue 漏了。

**修法** (V3.6.20+):
- `layout/Index.vue`: `import { ref, computed, onMounted, onUnmounted } from 'vue'`

### 2.2 13 文件 import 补全 (V3.6.20+, scripts/fix-missing-imports.cjs)

**自动扫描** + **自动修复** 13 个文件：

| 文件 | 缺的 import |
|------|-------------|
| `ChatMessage.vue` | defineProps, defineEmits |
| `ErrorBoundary.vue` | computed |
| `ErrorState.vue` | defineProps, defineEmits |
| `MarkdownView.vue` | defineProps |
| `StatCard.vue` | defineProps |
| `WechatScanLogin.vue` | defineProps, defineEmits |
| `useBackgroundSync.js` | readonly |
| `admin/Document.vue` | readonly |
| `admin/Push.vue` | readonly |
| `chat/Stream.vue` | inject (function inject() 改名 doInject() 避免重名) |
| `compliance/MaskTool.vue` | readonly |
| `kg/Index.vue` | computed |
| `prompts/Index.vue` | readonly |
| `layout/Index.vue` | onUnmounted |

**脚本** `scripts/fix-missing-imports.cjs`：
- 扫 `src/**/*.vue` 和 `src/**/*.js`
- 检测 32 个 Vue3 API (ref/reactive/computed/watch/onMounted/onUnmounted 等)
- 自动加到 `import { ... } from 'vue'`
- 用 Set 去重, 避免重复

### 2.3 chat/Stream.vue 重复 import 修复

**问题**: `function inject() {}` 跟 `import { inject } from 'vue'` 重名

**修法**: 改名 `function inject() {}` → `function doInject() {}`，并改 `@click="inject()"` → `@click="doInject()"`

## 3. 验证

| 测试 | 结果 |
|------|------|
| onUnmounted 报错 | ✅ 0 |
| 13 文件 import 补全 | ✅ 0 错 |
| chat/Stream inject 重名 | ✅ 改 doInject() |
| vite build 0 错 | ✅ 55s |
| 21 路由 21/21 200 | ✅ |
| 5 关键 .vue 编译 | ✅ 200 |

## 4. 累计 75 个版本 (V3.5.46-V3.6.20)
