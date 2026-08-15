# MiniMax 移动端 (V3.1.1)

> Vue 3 dist → iOS / Android 原生 App via Capacitor

## 架构

```
┌────────────────┐
│  浏览器 (PWA)  │  ← http://localhost:4173
└────────────────┘
        │
        │ 同一份 dist/
        ▼
┌────────────────┐
│  iOS / Android │  ← Capacitor 封装
│  原生 App      │     App ID: com.minimax.platform
└────────────────┘
```

## 技术栈

- **Capacitor 6.x**: 跨平台原生运行时
- **@capacitor/preferences**: 跨平台偏好 (替代 localStorage)
- **@capacitor/splash-screen**: 启动屏
- **@capacitor/status-bar**: 状态栏主题
- **@capacitor/haptics**: 触觉反馈
- **@capacitor/keyboard**: 键盘控制
- **@capacitor/network**: 网络状态
- **@capacitor/app**: App 生命周期

## 文件结构

```
frontend/
├── capacitor.config.ts      ← Capacitor 配置
├── src/
│   └── composables/
│       ├── useCapacitor.js  ← 移动端能力封装 (统一 API)
│       └── useSafeArea.js   ← 安全区域 (iOS 刘海/底部)
├── ios/                      ← iOS 原生项目 (cap add ios)
└── android/                  ← Android 原生项目 (cap add android)

scripts/
├── build-mobile.sh          ← 一键构建脚本
└── gen_onnx_test_model.py   ← ONNX 测试模型生成

resources/                    ← 启动屏/图标资源说明
```

## 一键构建

### 准备
```bash
# 1. iOS (需 macOS + Xcode)
xcode-select --install
sudo gem install cocoapods

# 2. Android (需 JDK 17 + Android Studio)
#    设置 ANDROID_HOME / ANDROID_SDK_ROOT
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

### 首次添加平台
```bash
cd frontend
npm install  # 装 @capacitor/* 依赖
npx cap add ios       # 生成 ios/ 目录
npx cap add android   # 生成 android/ 目录
```

### 构建
```bash
# 一键 (推荐)
./scripts/build-mobile.sh both

# 单独
./scripts/build-mobile.sh ios
./scripts/build-mobile.sh android
./scripts/build-mobile.sh sync  # 只同步, 不调原生工具
```

## 业务代码使用

```js
import { useCapacitor } from '@/composables/useCapacitor'
import { useSafeArea } from '@/composables/useSafeArea'

// 1. 平台检测
const { isNative, platform, isIOS, isAndroid } = useCapacitor()

// 2. 偏好存储 (自动选 localStorage / @capacitor/preferences)
const { preferences } = useCapacitor()
await preferences.set('user', 'alice')
const user = await preferences.get('user')

// 3. 触觉反馈
const { haptics } = useCapacitor()
await haptics.impact('MEDIUM')   // 点按
await haptics.notify('SUCCESS')  // 成功提示

// 4. 启动屏 (Vite 加载慢时手动控制)
const { splash } = useCapacitor()
await splash.show()
// 业务加载完
await splash.hide()

// 5. 安全区域
const { insets, paddingStyle } = useSafeArea()
const headerStyle = paddingStyle()  // 自动避开刘海
```

## 业务兼容

- Web 端: 用 `localStorage` / `navigator.onLine` / `navigator.vibrate` / PWA Service Worker
- iOS/Android: 用 `@capacitor/*` 插件
- 业务代码统一调 `useCapacitor()`, 无需判断平台

## 平台差异

| 能力 | Web | iOS | Android |
|------|-----|-----|---------|
| 偏好存储 | localStorage | @capacitor/preferences | @capacitor/preferences |
| 网络检测 | navigator.onLine | @capacitor/network | @capacitor/network |
| 触觉 | navigator.vibrate | Taptic Engine | Vibrator |
| 启动屏 | browser default | @capacitor/splash-screen | @capacitor/splash-screen |
| 状态栏 | n/a | @capacitor/status-bar | @capacitor/status-bar |
| 键盘 | n/a | @capacitor/keyboard | @capacitor/keyboard |
| 推送 | Web Push API | APNs | FCM |
| 扫码 | BarcodeDetector | Camera | Camera |

## PWA vs 原生

- PWA: 浏览器访问, 自动 Service Worker 离线, 无需安装
- 原生: 应用商店分发, 完整原生 API (推送/扫码/NFC), 离线更彻底

两者共享同一份 `dist/` 代码, 部署独立:
- PWA 部署: nginx 静态托管
- 原生打包: `cap sync` + Xcode/Gradle

## 常见问题

### Q: 启动屏一闪而过?
A: 调整 `capacitor.config.ts` 的 `SplashScreen.launchShowDuration`, 改为 3000+ ms

### Q: 状态栏颜色不对?
A: iOS 用 LIGHT/DARK, Android 用 `setBackgroundColor({ color: '#xxx' })` (hex 8 位含 alpha)

### Q: iOS 上 `env(safe-area-inset-*)` 不生效?
A: Capacitor 自动注入, 但需在 `<meta name="viewport" content="viewport-fit=cover">` 中声明

### Q: Android 网络请求 HTTPS 失败?
A: 在 `android/app/src/main/AndroidManifest.xml` 加 `android:usesCleartextTraffic="true"` (开发)

### Q: cap sync 后 ios 目录没更新?
A: 删 `ios/App/Pods` 重跑 `pod install`
