# 双录 Script SDK

> TypeScript 7 模块架构 · 业务零改动跨 H5/PAD/PC 三端
> 话术渲染 + TTS/ASR 多厂商适配 + 关键词检测 + 断点续传 + 上链数据准备

## 📋 目录

- [快速开始](#快速开始)
- [核心特性](#核心特性)
- [架构设计](#架构设计)
- [使用示例](#使用示例)
- [跨端适配](#跨端适配)
- [TTS / ASR Provider](#tts--asr-provider)
- [断点续传](#断点续传)
- [上链数据](#上链数据)
- [测试](#测试)
- [附录](#附录)

---

## 快速开始

### 安装

```bash
npm install @bank/dual-record-script-sdk
```

### 最简使用

```typescript
import { ScriptSDK, Channel, ProductType } from '@bank/dual-record-script-sdk';

// 1. 创建 SDK
const sdk = await ScriptSDK.create({
  defaultChannel: Channel.H5,
  tts: { providerId: 'aliyun', config: { appKey: '...', accessKeyId: '...', accessKeySecret: '...' } },
  asr: { providerId: 'aliyun', config: { appKey: '...', accessKeyId: '...', accessKeySecret: '...' } },
});

// 2. 加载话术脚本(从后端 API 获取)
sdk.loadScript(scriptModel);

// 3. 开启会话
sdk.startSession({
  sessionId: 'SES20260801001',
  orderId: 'ORD20260801000001',
  customerId: 'C001',
  productId: 'PROD-LIFE-001',
  productType: ProductType.INSURANCE,
  channel: Channel.H5,
  salesUserId: 'M001',
  branchId: '2001',
});

// 4. 业务方驱动节点(响应按钮点击)
await sdk.executeNode('N001');           // 经理播报
await sdk.executeNode('N002', {          // 客户回答
  customerAudio: recordedAudio,
  onCustomerResponse: (text) => console.log('客户说:', text),
});

// 5. 结束 + 上链
const finalSession = sdk.endSession();
const chainPayload = sdk.getChainPayload();
await fetch('/api/chain/submit', { method: 'POST', body: JSON.stringify(chainPayload) });
```

**完成!** 业务方完全不用关心 TTS/ASR/字幕/同步/上链数据格式。

---

## 核心特性

| 特性 | 说明 |
|------|------|
| **零业务改动** | 业务方只定义 ScriptModel,SDK 处理一切 |
| **三端同构** | H5 / PAD / PC 一份代码,只换 Player |
| **多厂商适配** | TTS/ASR Provider 模式,Aliyun/Tencent/Mock 任意切换 |
| **断点续传** | toJSON / resume 实现网络中断无缝续录 |
| **跨端同步** | WebSocket 抽象,Node ws / 浏览器 / 微信 / Electron |
| **Merkle 根** | 实时计算,直接上链 |
| **国密哈希** | 内置 SM3,JS 纯实现,无外部依赖 |
| **完整类型** | 严格 TypeScript 5,无 any 泄露 |

### 细节点 · 设计哲学(13 条注解)

1. **零业务改动跨三端** - 业务方只写一份 ScriptModel,H5/PAD/PC 自动适配
2. **Provider 模式** - TTS/ASR 抽象接口,Aliyun/Tencent/Mock 任意切换,业务不绑定厂商
3. **WebSocket 抽象** - BrowserWebSocket 默认,业务可注入 Node ws / 微信 wx.connectSocket
4. **Merkle 根实时** - 每个 NodeResult 落盘即重算,断网也不丢
5. **SM3 纯 JS** - 不依赖任何 native module,浏览器/Node/小程序通用
6. **同义词容错** - "同意" 匹配 "好的/可以/yes",容错 ASR 转写错误
7. **断点续传** - toJSON() / resume() 实现网络中断无缝续录
8. **条件表达式** - `${customer.age} >= 18`,支持 ==  !=  >  <  contains 等 9 种操作符
9. **渠道过滤** - 节点可选 channels,自动按 Channel.H5/PAD/PC 过滤
10. **节点幂等** - 同 nodeCode 重复 recordNodeResult 自动覆盖
11. **可回滚** - rewindTo(nodeCode) 支持中途修改
12. **事件总线** - 渲染器发射 8 类事件,业务侧 UI 响应零侵入
13. **上链开箱** - getChainPayload() 直接产出符合 Fabric 链码的 JSON

---

## 架构设计

### 7 模块分层

```
┌─────────────────────────────────────────────────────────────┐
│  index.ts (ScriptSDK) - 统一入口,业务方只调这里               │
├─────────────────────────────────────────────────────────────┤
│  core/         - ScriptCore / SessionManager / Keyword / SM3 │
│  tts-adapter/  - Aliyun / Tencent / Mock                     │
│  asr-adapter/  - Aliyun / Tencent / Mock                     │
│  renderer/     - Renderer / HTML5AudioPlayer / Subtitle      │
│  sync/         - SyncClient / BrowserWebSocket               │
│  types/        - 所有接口与类型                               │
└─────────────────────────────────────────────────────────────┘
```

### 数据流

```
┌────────────┐
│ 业务方定义  │ ScriptModel (话术)
│  ScriptModel│
└──────┬─────┘
       │ loadScript()
       ▼
┌────────────┐
│ ScriptCore │ 校验/排序/条件/版本
└──────┬─────┘
       │ startSession()
       ▼
┌────────────────────┐
│ ScriptSessionManager│ 状态机 + Merkle 根
└──────┬─────────────┘
       │ executeNode(code, audio?)
       ▼
┌────────────────────┐
│     Renderer       │ TTS 播放 + 字幕 + 动画
│  (TTS+ASR 自动)     │
└──────┬─────────────┘
       │
       ├──► TTSProvider.synthesize()
       │     └─► audio (Uint8Array)
       │
       ├──► ASRProvider.recognize()  (if customerAudio)
       │     └─► {text, confidence}
       │
       └──► KeywordDetector.detect()
             └─► {mustHitPassed, hitRate}

       │ recordNodeResult()
       ▼
┌────────────────────┐
│ NodeResult + Merkle│
└──────┬─────────────┘
       │ endSession()
       ▼
┌────────────────────┐
│ getChainPayload()  │ 上链数据(Merkle 根 + 所有 NodeResult)
└────────────────────┘
```

---

## 使用示例

### 1. 极简模式(Mock Provider)

```typescript
import { ScriptSDK, Channel, ProductType } from '@bank/dual-record-script-sdk';

const sdk = await ScriptSDK.create({
  defaultChannel: Channel.H5,
  tts: { providerId: 'mock', config: {} },
  asr: { providerId: 'mock', config: {} },
});

sdk.loadScript(scriptModel);
sdk.startSession({ /* ... */ });
await sdk.executeNode('N001');
```

### 2. 生产模式(阿里云)

```typescript
const sdk = await ScriptSDK.create({
  defaultChannel: Channel.PAD,
  tts: {
    providerId: 'aliyun',
    config: {
      appKey: process.env.ALIYUN_TTS_APPKEY!,
      accessKeyId: process.env.ALIYUN_AK!,
      accessKeySecret: process.env.ALIYUN_SK!,
    },
  },
  asr: {
    providerId: 'aliyun',
    config: {
      appKey: process.env.ALIYUN_ASR_APPKEY!,
      accessKeyId: process.env.ALIYUN_AK!,
      accessKeySecret: process.env.ALIYUN_SK!,
    },
  },
});
```

### 3. 事件订阅(更新 UI)

```typescript
const unsub = sdk.onRenderEvent((event) => {
  switch (event.type) {
    case 'NODE_START':
      // 节点高亮
      break;
    case 'TTS_START':
      // 显示"经理播报中..."
      break;
    case 'SUBTITLE_UPDATE':
      // 更新字幕 DOM
      break;
    case 'PROGRESS':
      // 进度条
      break;
    case 'COMPLETE':
      // 跳转结果页
      break;
  }
});

// 取消订阅
unsub();
```

### 4. 同步多端(WebSocket)

```typescript
await sdk.startSync({
  url: 'wss://sync.bank.com/ws',
  deviceId: 'pad-m001-001',
  userId: 'M001',
  authToken: 'eyJhbGc...',
});

// 中途断网重连:自动重连机制
sdk.stopSync();
```

### 5. 断点续传

```typescript
// 第 1 次:网络中断
const saved = sdk.getSession();
localStorage.setItem('session', JSON.stringify(saved));

// 重新打开页面
const sdk2 = await ScriptSDK.create({ /* ... */ });
sdk2.loadScript(scriptModel);
const json = localStorage.getItem('session')!;
sdk2.resumeSession(json);
// 从断点继续
await sdk2.next();
```

---

## 跨端适配

### H5 端(浏览器)

```typescript
// SDK 默认用 HTML5AudioPlayer
// 无需特殊配置
```

### PAD 端(微信小程序)

```typescript
import { ScriptSDK, AudioPlayer } from '@bank/dual-record-script-sdk';

class MiniProgramAudioPlayer implements AudioPlayer { /* ... */ }

const sdk = await ScriptSDK.create({ /* ... */ });
sdk.setAudioPlayer(new MiniProgramAudioPlayer());
sdk.setWebSocket(new MiniProgramWebSocket());
```

完整示例见 `examples/pad/index.ts`

### PC 端(Electron / 大屏)

```typescript
const sdk = await ScriptSDK.create({
  defaultChannel: Channel.PC,
  renderer: {
    enableTTS: false,        // PC 端可关 TTS
    enableSubtitle: true,    // 字幕
    enableAnimation: true,   // 复杂动画
    subtitleFontSize: 24,
    themeColor: '#2b2d42',
  },
});
```

完整示例见 `examples/pc/index.ts`

---

## TTS / ASR Provider

### 内置 Provider

| Provider | 用途 |
|---------|------|
| `mock` | 测试 / 本地开发 / 无外网环境 |
| `aliyun` | 阿里云智能语音(一句话识别 + 短文本 TTS) |
| `tencent` | 腾讯云智聆(同等能力) |

### 自定义 Provider

```typescript
import { TTSProvider, TTSConfig, TTSResult } from '@bank/dual-record-script-sdk';

class MyTTSProvider implements TTSProvider {
  readonly id = 'my-tts';
  readonly name = 'My Custom TTS';
  readonly streaming = false;

  async synthesize(config: TTSConfig): Promise<TTSResult> {
    // 业务方实现
    return { audio, duration, audioHash, sampleRate: 16000, format: 'mp3' };
  }

  async healthCheck(): Promise<boolean> {
    return true;
  }
}

// 注册
import { registerTTSProvider } from '@bank/dual-record-script-sdk';
registerTTSProvider(new MyTTSProvider());
```

---

## 上链数据

```typescript
// 会话结束后
const chainPayload = sdk.getChainPayload();
/* 输出结构:
{
  sessionId: 'SES20260801001',
  orderId: 'ORD20260801000001',
  scriptId: 'INS-LIFE-V3.2',
  scriptVersion: '3.2.0',
  scriptHash: '...64 字符 SM3...',
  merkleRoot: '...64 字符 SM3...',  // 链上存这个就够
  startedAt: '2026-08-01T...',
  endedAt: '2026-08-01T...',
  totalDuration: 600000,
  completed: true,
  nodeResults: [
    {
      nodeCode: 'N001',
      result: 'PASS',
      duration: 5000,
      customerSaid: '...',
      keywordsHit: ['是', '本人'],
      asrConfidence: 0.95,
      audioHash: '...',
      startedAt: '...',
      endedAt: '...',
      hash: '... 节点指纹 ...',
    },
    ...
  ]
}
*/

// 提交到 Java 链码(对应 EvidenceContract.submitEvidence)
await fetch('/api/dual-record/chain/submit', {
  method: 'POST',
  body: JSON.stringify(chainPayload),
});
```

---

## 测试

```bash
npm test                  # 跑所有 62 个测试
npm test -- --coverage    # 覆盖率
npm run typecheck         # tsc --noEmit
```

**当前测试覆盖**:

| 模块 | 用例 |
|------|------|
| SM3 哈希 | 10 |
| ScriptCore | 17 |
| ScriptSession | 11 |
| KeywordDetector | 8 |
| TTS Mock | 3 |
| ASR Mock | 2 |
| ScriptSDK 集成 | 11 |
| **合计** | **62** |

---

## 附录

### A. ScriptModel 完整字段

```typescript
interface ScriptModel {
  scriptId: string;              // UUID
  version: string;                // 语义化版本
  productType: ProductType;       // INSURANCE/WEALTH/...
  name: string;
  description?: string;
  productIds?: string[];
  nodes: ScriptNode[];            // 节点列表
  estimatedDuration?: number;
  author?: string;
  createdAt: string;
  updatedAt: string;
  scope?: {                       // 适用范围
    productTypes?: ProductType[];
    riskLevels?: RiskLevel[];
    branches?: string[];
  };
  metadata?: Record<string, unknown>;
  hash?: string;                  // SM3 指纹
  minSdkVersion?: string;         // 兼容最低 SDK
}
```

### B. ScriptNode 完整字段

```typescript
interface ScriptNode {
  nodeCode: string;                       // 节点编码
  nodeName: string;
  nodeType: NodeType;                     // 8 种类型
  order: number;                          // 执行顺序
  text: string;                           // 文本
  customerResponseKeywords?: string[];    // 可答词
  mustHitKeywords?: string[];             // 必答词
  timeout?: number;                       // 超时(秒)
  skippable?: boolean;
  retryable?: boolean;
  maxRetries?: number;
  riskLevel?: RiskLevel;
  attachmentId?: string;
  children?: ScriptNode[];                // 子节点
  condition?: string;                     // 条件表达式
  metadata?: Record<string, unknown>;
  channels?: Channel[];                   // 适用渠道
}
```

### C. 错误码

| 错误码 | 含义 |
|--------|------|
| `CONFIG_ERROR` | 配置错误 |
| `NETWORK_ERROR` | 网络错误 |
| `TTS_ERROR` | TTS 调用失败 |
| `ASR_ERROR` | ASR 调用失败 |
| `SCRIPT_ERROR` | 脚本错误 |
| `KEYWORD_MISS` | 关键词未命中 |
| `TIMEOUT` | 超时 |
| `RENDER_ERROR` | 渲染失败 |
| `SYNC_ERROR` | 同步失败 |

### D. 浏览器兼容

- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+
- 微信内置浏览器
- Node.js 16+

### E. 许可证

Apache-2.0

### F. 维护团队

- 架构设计: Mavis / 区块链团队
- 国密适配: 信息安全部
- 业务对接: 双录业务部

---

**版本**: 1.0.0
**最后更新**: 2026-08-01
**状态**: 生产就绪 ✅
