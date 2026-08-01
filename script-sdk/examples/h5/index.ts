/**
 * H5 端集成示例
 *
 * <p>适用:手机浏览器 / 微信公众号 / 微信 H5
 * <p>业务方代码量:极少
 *
 * @author Mavis
 */

import {
  ScriptSDK,
  Channel,
  ProductType,
  ScriptModel,
  Renderer,
  SDKConfig,
} from '../../src';

// ============================================================
// 1. 业务方获取的话术脚本(从后端 API 拉取)
// ============================================================

const scriptModel: ScriptModel = {
  scriptId: 'INS-LIFE-V3.2',
  version: '3.2.0',
  productType: ProductType.INSURANCE,
  name: 'XX 终身寿险标准话术',
  description: '适用于 18-60 周岁客户的终身寿险双录',
  productIds: ['PROD-LIFE-001'],
  createdAt: '2026-08-01T00:00:00Z',
  updatedAt: '2026-08-01T00:00:00Z',
  estimatedDuration: 600,
  author: '合规部',
  nodes: [
    {
      nodeCode: 'N001',
      nodeName: '开场问候',
      nodeType: 'MANAGER_SPEAK',
      order: 1,
      text: '您好,我是 XX 银行客户经理张三,本次双录将全程录音录像,作为您的投保依据。',
      timeout: 30,
      riskLevel: 'LOW',
    },
    {
      nodeCode: 'N002',
      nodeName: '客户身份核验',
      nodeType: 'CUSTOMER_RESPOND',
      order: 2,
      text: '请问您是 [客户姓名] 本人吗?',
      mustHitKeywords: ['是', '是的', '本人', '对'],
      customerResponseKeywords: ['是', '是的', '本人', '对', '没错'],
      timeout: 30,
      riskLevel: 'LOW',
    },
    {
      nodeCode: 'N003',
      nodeName: '风险揭示',
      nodeType: 'RISK_DISCLOSURE',
      order: 3,
      text: '本产品为终身寿险,您可能面临以下风险:1. 退保损失 2. 现金价值波动 3. 身故给付限制。',
      mustHitKeywords: ['明白', '清楚', '了解'],
      timeout: 60,
      riskLevel: 'HIGH',
    },
    {
      nodeCode: 'N004',
      nodeName: '客户确认',
      nodeType: 'CUSTOMER_RESPOND',
      order: 4,
      text: '您是否已充分了解上述风险?',
      mustHitKeywords: ['了解', '明白', '清楚', '是'],
      timeout: 30,
      riskLevel: 'HIGH',
    },
    {
      nodeCode: 'N005',
      nodeName: '电子签字',
      nodeType: 'E_SIGN',
      order: 5,
      text: '请在下方电子签名区域完成签字。',
      timeout: 60,
      riskLevel: 'CRITICAL',
    },
  ],
};

// ============================================================
// 2. 业务方 H5 页面代码
// ============================================================

async function bootstrap() {
  // 2.1 创建 SDK
  const sdk: ScriptSDK = await ScriptSDK.create({
    defaultChannel: Channel.H5,
    tts: {
      providerId: 'mock', // 生产换 'aliyun' / 'tencent'
      config: {},
    },
    asr: {
      providerId: 'mock',
      config: {},
    },
    renderer: {
      enableTTS: true,
      enableSubtitle: true,
      subtitleMaxLines: 3,
      subtitleFontSize: 18,
      themeColor: '#d90429',
    },
  } as SDKConfig);

  // 2.2 加载脚本
  sdk.loadScript(scriptModel);

  // 2.3 订阅渲染事件,更新 UI
  sdk.onRenderEvent((event) => {
    console.log('[Render]', event.type, event.data);
    // H5 业务方只需更新 DOM
    if (event.type === 'SUBTITLE_UPDATE' && event.data) {
      const data = event.data as { text: string; speaker: string };
      document.getElementById('subtitle')!.innerText = `${data.speaker}: ${data.text}`;
    }
    if (event.type === 'PROGRESS' && event.data) {
      const data = event.data as { percent: number };
      document.getElementById('progress')!.style.width = `${data.percent}%`;
    }
  });

  // 2.4 启动同步(可选)
  // await sdk.startSync({ url: 'wss://sync.bank.com/ws', deviceId: 'h5-uuid', userId: 'M001' });

  // 2.5 开会话
  const session = sdk.startSession({
    sessionId: 'SES20260801001',
    orderId: 'ORD20260801000001',
    customerId: 'C001',
    productId: 'PROD-LIFE-001',
    productType: ProductType.INSURANCE,
    channel: Channel.H5,
    salesUserId: 'M001',
    branchId: '2001',
  });
  console.log('会话已开启', session.sessionId);

  // 2.6 业务方点击"开始双录"
  document.getElementById('btn-start')!.addEventListener('click', async () => {
    // 节点 1:经理播报
    await sdk.executeNode('N001');

    // 节点 2:客户回答(从麦克风录音,这里是 mock 音频)
    const mockAudio = new Uint8Array(16000 * 2); // 1 秒静音
    await sdk.executeNode('N002', {
      customerAudio: mockAudio,
      onCustomerResponse: (text) => {
        console.log('客户说:', text);
      },
    });

    // 节点 3:风险揭示
    await sdk.executeNode('N003');

    // 节点 4:客户确认
    await sdk.executeNode('N004', {
      customerAudio: mockAudio,
    });

    // 节点 5:电子签字(由业务方处理)
    // ...

    // 2.7 结束会话
    const finalSession = sdk.endSession();
    console.log('双录完成', finalSession);
    console.log('Merkle 根:', finalSession.merkleRoot);
    console.log('上链数据:', sdk.getChainPayload());
  });
}

// 页面加载完成后启动
if (typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', () => {
    bootstrap().catch(console.error);
  });
}

export { bootstrap };
