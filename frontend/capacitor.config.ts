import type { CapacitorConfig } from '@capacitor/cli';

/**
 * Capacitor 移动端配置 (V3.1.1)
 *
 * <p>打包 Vue3 dist 为 iOS/Android 原生 App, 走 PWA 同套代码, 离线优先
 *
 * <h3>关键点</h3>
 *   - webDir: 指向 vite build 后的 dist/
 *   - bundledWebRuntime: false 让 Web 资源走本地 Capacitor 协议
 *   - server.cleartext: 允许 http (生产需改 https)
 *   - plugins.SplashScreen: 启动屏配置
 */
const config: CapacitorConfig = {
  appId: 'com.minimax.platform',
  appName: 'MiniMax',
  webDir: 'dist',
  bundledWebRuntime: false,
  // 日志级别
  loggingLevel: 'INFO',
  // 服务端配置 (开发期可用, 生产走真实域名)
  server: {
    androidScheme: 'https',
    cleartext: true,  // 允许 http (开发 + 内网)
    errorPath: 'error.html',
    // 生产环境:
    // url: 'https://app.minimax.com',
    // cleartext: false,
  },
  // Android 专属
  android: {
    allowMixedContent: true,  // 允许 https 内嵌 http (开发期)
    captureInput: true,        // 启用软键盘 capture
    webContentsDebuggingEnabled: true,
    backgroundColor: '#ffffff',
  },
  // iOS 专属
  ios: {
    contentInset: 'automatic',
    backgroundColor: '#ffffff',
    // WKWebView 配置
    limitsNavigationsToAppBoundDomains: false,
  },
  // 插件配置
  plugins: {
    // 启动屏
    SplashScreen: {
      launchShowDuration: 2000,           // 启动屏显示 2s
      launchAutoHide: true,               // 自动隐藏
      backgroundColor: '#409EFF',         // 主题蓝
      androidSplashResourceName: 'splash', // Android 资源名
      androidScaleType: 'CENTER_CROP',
      showSpinner: true,                   // 显示加载圈
      androidSpinnerStyle: 'small',
      spinnerColor: '#ffffff',
      iosSpinnerStyle: 'small',
    },
    // 状态栏
    StatusBar: {
      style: 'LIGHT',          // LIGHT/DARK
      backgroundColor: '#409EFF',
      overlaysWebView: false,   // 不覆盖 WebView
    },
    // 偏好 (代替 localStorage, 跨平台)
    Preferences: {
      group: 'minimax.storage',
    },
  },
};

export default config;
