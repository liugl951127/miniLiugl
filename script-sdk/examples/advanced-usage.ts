/**
 * 进阶用法 - 自定义 Provider / 完整业务集成
 *
 * <p>展示:
 * <ul>
 *   <li>自定义 TTS Provider
 *   <li>自定义 ASR 流式
 *   <li>断点续传场景
 *   <li>事件总线订阅
 *   <li>上链数据准备
 * </ul>
 *
 * @author Mavis
 */

import {
  ScriptSDK,
  Channel,
  ProductType,
  TTSProvider,
  TTSConfig,
  TTSResult,
  ASRProvider,
  ASRConfig,
  ASRResult,
  ScriptModel,
  sm3Hex,
} from '../src';

// ============================================================
// 自定义 TTS Provider(对接公司内部 TTS 服务)
// ============================================================

class InternalTTSProvider implements TTSProvider {
  readonly id = 'internal';
  readonly name = '银行内部 TTS';
  readonly streaming = false;
  private endpoint: string;

  constructor(endpoint: string) {
    this.endpoint = endpoint;
  }

  async synthesize(config: TTSConfig): Promise<TTSResult> {
    const response = await fetch(this.endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        text: config.text,
        voice: config.voiceId || 'bank-manager-male',
        speed: config.speed || 1.0,
      }),
    });
    const data = (await response.json()) as { audio: string; duration: number };
    const audio = Uint8Array.from(atob(data.audio), (c) => c.charCodeAt(0));
    const audioHash = sm3Hex(audio);

    return {
      audio,
      duration: data.duration,
      audioHash,
      sampleRate: 16000,
      format: 'mp3',
      raw: audio,
    };
  }

  async healthCheck(): Promise<boolean> {
    try {
      const r = await fetch(`${this.endpoint}/health`);
      return r.ok;
    } catch {
      return false;
    }
  }
}

// ============================================================
// 自定义 ASR Provider(流式)
// ============================================================

class InternalASRProvider implements ASRProvider {
  readonly id = 'internal';
  readonly name = '银行内部 ASR';
  readonly streaming = true;

  async recognize(config: ASRConfig): Promise<ASRResult> {
    const response = await fetch('https://asr.internal.bank.com/recognize', {
      method: 'POST',
      body: config.audio as unknown as BodyInit,
    });
    return (await response.json()) as ASRResult;
  }

  async recognizeStream(
    config: ASRConfig,
    onResult: (result: ASRResult) => void
  ): Promise<ASRResult> {
    // WebSocket 方式
    const ws = new WebSocket('wss://asr.internal.bank.com/stream');
    let finalResult: ASRResult | null = null;

    return new Promise((resolve, reject) => {
      ws.onopen = () => {
        ws.send(config.audio as unknown as ArrayBuffer);
      };
      ws.onmessage = (e) => {
        const r = JSON.parse(e.data) as ASRResult;
        onResult(r);
        if (r.isFinal) {
          finalResult = r;
          ws.close();
        }
      };
      ws.onerror = reject;
      ws.onclose = () => {
        if (finalResult) resolve(finalResult);
        else reject(new Error('流式识别未完成'));
      };
    });
  }

  async healthCheck(): Promise<boolean> {
    return true;
  }
}

// ============================================================
// 完整业务集成示例
// ============================================================

async function fullExample() {
  const sdk: ScriptSDK = await ScriptSDK.create({
    defaultChannel: Channel.PAD,
    tts: { providerId: 'internal', config: {} },
    asr: { providerId: 'internal', config: {} },
  });

  // 注入自定义 Provider
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const { registerTTSProvider, registerASRProvider } = await import('../src');
  registerTTSProvider(new InternalTTSProvider('https://tts.internal.bank.com'));
  registerASRProvider(new InternalASRProvider());

  // 加载脚本
  const script: ScriptModel = {
    scriptId: 'WEALTH-PREMIUM',
    version: '3.0.0',
    productType: ProductType.WEALTH,
    name: '高净值理财双录',
    createdAt: '2026-08-01T00:00:00Z',
    updatedAt: '2026-08-01T00:00:00Z',
    nodes: [
      {
        nodeCode: 'N001',
        nodeName: '开场',
        nodeType: 'MANAGER_SPEAK',
        order: 1,
        text: '您好,我是专属客户经理。',
        timeout: 30,
      },
      {
        nodeCode: 'N002',
        nodeName: '客户身份核验',
        nodeType: 'CUSTOMER_RESPOND',
        order: 2,
        text: '请确认您是 [姓名] 先生/女士本人',
        mustHitKeywords: ['是', '本人', '对'],
        customerResponseKeywords: ['是', '是的', '本人'],
        timeout: 30,
      },
      {
        nodeCode: 'N003',
        nodeName: '产品介绍',
        nodeType: 'MANAGER_SPEAK',
        order: 3,
        text: '本产品为中高风险理财,年化收益 4.5%~6.2%。',
        timeout: 60,
        skippable: false,
      },
      {
        nodeCode: 'N004',
        nodeName: '风险揭示',
        nodeType: 'RISK_DISCLOSURE',
        order: 4,
        text: '本理财不保证本金,可能损失全部本金',
        mustHitKeywords: ['明白', '了解', '清楚'],
        timeout: 60,
        riskLevel: 'CRITICAL',
      },
      {
        nodeCode: 'N005',
        nodeName: '客户确认',
        nodeType: 'CUSTOMER_RESPOND',
        order: 5,
        text: '是否已阅读并理解所有风险?',
        mustHitKeywords: ['是', '了解', '明白'],
        timeout: 30,
        riskLevel: 'CRITICAL',
      },
      {
        nodeCode: 'N006',
        nodeName: '电子签字',
        nodeType: 'E_SIGN',
        order: 6,
        text: '请完成电子签字',
        timeout: 60,
        riskLevel: 'CRITICAL',
      },
    ],
  };
  sdk.loadScript(script);

  // 启动同步
  await sdk.startSync({
    url: 'wss://sync.bank.com/ws',
    deviceId: 'pad-m003-001',
    userId: 'M003',
    authToken: 'eyJhbGciOiJIUzI1NiJ9...',
  });

  // 启动会话
  sdk.startSession({
    sessionId: 'SES20260801003',
    orderId: 'ORD20260801000003',
    customerId: 'C003',
    productId: 'PROD-WEALTH-HIGH-001',
    productType: ProductType.WEALTH,
    channel: Channel.PAD,
    salesUserId: 'M003',
    branchId: '2001',
  });

  // 监听所有渲染事件
  const events: string[] = [];
  sdk.onRenderEvent((event) => {
    events.push(`[${event.timestamp}] ${event.type}`);
  });

  // 顺序执行
  await sdk.executeNode('N001');
  await sdk.executeNode('N002', {
    customerAudio: new Uint8Array(16000 * 2), // 1秒静音
  });
  await sdk.executeNode('N003');
  await sdk.executeNode('N004', {
    customerAudio: new Uint8Array(16000 * 2),
  });
  await sdk.executeNode('N005', {
    customerAudio: new Uint8Array(16000 * 2),
  });
  // N006 E_SIGN 由业务方处理(画板)

  // 完成会话
  const finalSession = sdk.endSession();

  // 获取上链数据
  const chainPayload = sdk.getChainPayload();
  console.log('=== 上链数据 ===');
  console.log(JSON.stringify(chainPayload, null, 2));

  // 中途断网重连场景(模拟)
  // await sdk.resumeSession(JSON.stringify(savedSession));

  return finalSession;
}

export { fullExample, InternalTTSProvider, InternalASRProvider };
