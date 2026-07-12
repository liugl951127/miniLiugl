# Mobile 资源 (V3.1.1)

## 启动屏 (Splash Screen)

iOS: `ios/App/Resources/splash/` 需放 `Default@xxx.png` (各尺寸)
Android: `android/app/src/main/res/drawable*/splash.png`

### 启动屏规范
- 主题色: `#409EFF` (主品牌蓝)
- Logo: SVG `M` 字母 + 渐变背景
- 居中显示
- 各尺寸:
  - iOS: 2732×2732 (iPad Pro 12.9"), 2208×2208, 1668×1668, 1536×1536, 1242×2208, 750×1334, 640×960
  - Android: 192×192 (mdpi), 192×192 (hdpi), 192×192 (xhdpi), 192×192 (xxhdpi), 192×192 (xxxhdpi)

## 图标 (App Icon)

iOS: `ios/App/Resources/AppIcon.appiconset/` 需放 1024×1024 PNG (App Store)
     + 各设备尺寸 (180×180, 167×167, 152×152, 120×120, 80×80, 58×58, 40×40, 29×29)
Android: `android/app/src/main/res/mipmap-*/ic_launcher.png`
       48×48 (mdpi), 72×72 (hdpi), 96×96 (xhdpi), 144×144 (xxhdpi), 192×192 (xxxhdpi)

### 图标生成
```bash
# 用 imagemagick 生成 (需装 imagemagick)
convert icon-source.svg -resize 1024x1024 icon-1024.png
```

## 主题色
- 主色: `#409EFF` (Element Plus 默认蓝)
- 辅色: `#67C23A` (绿), `#E6A23C` (橙), `#F56C6C` (红), `#909399` (灰)

## 字体
- 默认: 系统字体 (iOS: SF Pro, Android: Roboto)
- 国际化: Noto Sans CJK (Android 自带, iOS 用 PingFang SC)

## PWA 兼容
同时支持浏览器 (PWA 离线) + 原生 App (Capacitor)
两套走同一份 `dist/` 代码, 启动屏只是额外体验
