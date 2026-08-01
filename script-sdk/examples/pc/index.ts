/**
 * PC 端集成示例
 *
 * <p>适用:Electron / 桌面浏览器 / 网银
 * <p>PC 端能力最强:多窗口 + 大屏渲染 + 复杂动画
 *
 * @author Mavis
 */

import {
  ScriptSDK,
  Channel,
  ProductType,
  ScriptModel,
} from '../../src';

// ============================================================
// PC 端:网银 / 信贷系统 / 内部办公自动化
// ============================================================

async function bootstrapPC() {
  const sdk: ScriptSDK = await ScriptSDK.create({
    defaultChannel: Channel.PC,
    tts: { providerId: 'mock', config: {} },
    asr: { providerId: 'mock', config: {} },
    renderer: {
      enableTTS: false, // PC 端通常 TTS 不开启(现场经理讲话)
      enableSubtitle: true,
      enableAnimation: true,
      subtitleFontSize: 24,
      themeColor: '#2b2d42',
      backgroundColor: '#ffffff',
    },
  });

  // 加载话术
  const script: ScriptModel = {
    scriptId: 'FUND-V3.2',
    version: '3.2.0',
    productType: ProductType.FUND,
    name: '基金双录话术',
    createdAt: '2026-08-01T00:00:00Z',
    updatedAt: '2026-08-01T00:00:00Z',
    nodes: [
      {
        nodeCode: 'N001',
        nodeName: '身份核验',
        nodeType: 'CUSTOMER_RESPOND',
        order: 1,
        text: '请确认您是本人办理业务',
        mustHitKeywords: ['是', '本人', '确认'],
      },
      {
        nodeCode: 'N002',
        nodeName: '录像开始',
        nodeType: 'VIDEO_RECORD',
        order: 2,
        text: '录像开始',
      },
      {
        nodeCode: 'N003',
        nodeName: '产品介绍',
        nodeType: 'MANAGER_SPEAK',
        order: 3,
        text: '本基金为混合型基金,主要投资于...',
        skippable: false,
      },
      {
        nodeCode: 'N004',
        nodeName: '风险揭示',
        nodeType: 'RISK_DISCLOSURE',
        order: 4,
        text: '基金有风险,可能损失本金',
        mustHitKeywords: ['了解', '明白'],
        riskLevel: 'CRITICAL',
      },
    ],
  };
  sdk.loadScript(script);

  // 开启会话
  const session = sdk.startSession({
    sessionId: 'SES-PC-001',
    orderId: 'ORD20260801000003',
    customerId: 'C003',
    productId: 'PROD-FUND-001',
    productType: ProductType.FUND,
    channel: Channel.PC,
    salesUserId: 'M003',
    branchId: '2002',
  });

  // PC 端:大屏展示 + 多窗口
  sdk.onRenderEvent((event) => {
    if (event.type === 'SUBTITLE_UPDATE' && event.data) {
      // 大屏展示
      // window.open(`/subtitle?text=${encodeURIComponent(data.text)}`);
    }
    if (event.type === 'NODE_START' && event.data) {
      // 进度条 + 节点高亮
      // highlightNode(node.nodeCode);
    }
  });

  console.log('PC 端会话已启动:', session.sessionId);

  // PC 端特点:可后台批量执行质检、回放、合规检查
  // 实际业务:
  // 1. 实时录制(RTC:Agora / 声网 / ZLMediaKit)
  // 2. 实时字幕(Web Speech API + 兜底 ASR)
  // 3. AI 质检(并行:异常手势 / 离座 / 多人)
  // 4. 多窗口:主屏 + 客户屏 + 经理屏

  return sdk;
}

export { bootstrapPC };
