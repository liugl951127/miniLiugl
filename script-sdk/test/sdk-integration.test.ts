/**
 * ScriptSDK 集成测试
 */
import { ScriptSDK, Channel, ProductType, NodeType, NodeResultStatus, AudioPlayer } from '../src';
import { registerTTSProvider, MockTTSProvider } from '../src/tts-adapter';
import { registerASRProvider, MockASRProvider } from '../src/asr-adapter';

/**
 * 无头音频播放器(Node 测试用)
 */
class HeadlessAudioPlayer implements AudioPlayer {
  async play(_audio: Uint8Array, _format: string): Promise<void> {
    // 立即完成
  }
  pause(): void {}
  resume(): void {}
  stop(): void {}
  setVolume(_v: number): void {}
  setSpeed(_s: number): void {}
  onEnded(_h: () => void): void {}
  onError(_h: (e: Error) => void): void {}
}

const sampleScript = {
  scriptId: 'TEST-1',
  version: '1.0.0',
  productType: ProductType.INSURANCE,
  name: '测试',
  createdAt: '2026-08-01T00:00:00Z',
  updatedAt: '2026-08-01T00:00:00Z',
  nodes: [
    { nodeCode: 'N001', nodeName: '开场', nodeType: NodeType.MANAGER_SPEAK, order: 1, text: '您好' },
    {
      nodeCode: 'N002',
      nodeName: '客户确认',
      nodeType: NodeType.CUSTOMER_RESPOND,
      order: 2,
      text: '您是本人吗?',
      mustHitKeywords: ['是', '本人'],
    },
    { nodeCode: 'N003', nodeName: '签字', nodeType: NodeType.E_SIGN, order: 3, text: '签字' },
  ],
};

describe('ScriptSDK 集成', () => {
  test('create 创建 SDK', async () => {
    const sdk = await ScriptSDK.create({
      defaultChannel: Channel.H5,
      tts: { providerId: 'mock', config: {} },
      asr: { providerId: 'mock', config: {} },
    });
    expect(sdk).toBeDefined();
  });

  test('loadScript 加载', async () => {
    const sdk = await ScriptSDK.create({
      defaultChannel: Channel.H5,
      tts: { providerId: 'mock', config: {} },
      asr: { providerId: 'mock', config: {} },
    });
    sdk.loadScript(sampleScript);
    expect(sdk.getScriptCore()).not.toBeNull();
  });

  test('startSession 启动会话', async () => {
    const sdk = await ScriptSDK.create({
      defaultChannel: Channel.H5,
      tts: { providerId: 'mock', config: {} },
      asr: { providerId: 'mock', config: {} },
    });
    sdk.loadScript(sampleScript);
    const s = sdk.startSession({
      sessionId: 'S1',
      orderId: 'O1',
      customerId: 'C1',
      productId: 'P1',
      productType: ProductType.INSURANCE,
      channel: Channel.H5,
      salesUserId: 'M1',
      branchId: 'B1',
    });
    expect(s.sessionId).toBe('S1');
  });

  test('executeNode 执行经理说话', async () => {
    const sdk = await ScriptSDK.create({
      defaultChannel: Channel.H5,
      tts: { providerId: 'mock', config: {} },
      asr: { providerId: 'mock', config: {} },
    });
    sdk.setAudioPlayer(new HeadlessAudioPlayer());
    sdk.loadScript(sampleScript);
    sdk.startSession({
      sessionId: 'S1',
      orderId: 'O1',
      customerId: 'C1',
      productId: 'P1',
      productType: ProductType.INSURANCE,
      channel: Channel.H5,
      salesUserId: 'M1',
      branchId: 'B1',
    });
    const r = await sdk.executeNode('N001');
    expect(r.node.nodeCode).toBe('N001');
    expect(r.result.result).toBe(NodeResultStatus.PASS);
  });

  test('executeNode 客户响应 - 命中关键词', async () => {
    const sdk = await ScriptSDK.create({
      defaultChannel: Channel.H5,
      tts: { providerId: 'mock', config: {} },
      asr: { providerId: 'mock', config: {} },
    });
    sdk.setAudioPlayer(new HeadlessAudioPlayer());
    sdk.loadScript(sampleScript);
    sdk.startSession({
      sessionId: 'S1', orderId: 'O1', customerId: 'C1', productId: 'P1',
      productType: ProductType.INSURANCE, channel: Channel.H5,
      salesUserId: 'M1', branchId: 'B1',
    });
    const r = await sdk.executeNode('N002', {
      customerAudio: new Uint8Array(16000 * 2),
    });
    expect(r.keywordMatch).toBeDefined();
    expect(r.keywordMatch!.passed).toBe(false);
  });

  test('endSession 结束会话', async () => {
    const sdk = await ScriptSDK.create({
      defaultChannel: Channel.H5,
      tts: { providerId: 'mock', config: {} },
      asr: { providerId: 'mock', config: {} },
    });
    sdk.setAudioPlayer(new HeadlessAudioPlayer());
    sdk.loadScript(sampleScript);
    sdk.startSession({
      sessionId: 'S1', orderId: 'O1', customerId: 'C1', productId: 'P1',
      productType: ProductType.INSURANCE, channel: Channel.H5,
      salesUserId: 'M1', branchId: 'B1',
    });
    await sdk.executeNode('N001');
    const final = sdk.endSession();
    expect(final.completed).toBe(true);
    expect(final.endedAt).toBeDefined();
  });

  test('getChainPayload 输出', async () => {
    const sdk = await ScriptSDK.create({
      defaultChannel: Channel.H5,
      tts: { providerId: 'mock', config: {} },
      asr: { providerId: 'mock', config: {} },
    });
    sdk.setAudioPlayer(new HeadlessAudioPlayer());
    sdk.loadScript(sampleScript);
    sdk.startSession({
      sessionId: 'S1', orderId: 'O1', customerId: 'C1', productId: 'P1',
      productType: ProductType.INSURANCE, channel: Channel.H5,
      salesUserId: 'M1', branchId: 'B1',
    });
    await sdk.executeNode('N001');
    const payload = sdk.getChainPayload() as { merkleRoot: string; nodeResults: unknown[] };
    expect(payload.merkleRoot).toHaveLength(64);
    expect(payload.nodeResults).toHaveLength(1);
  });

  test('resumeSession 断点续传', async () => {
    const sdk = await ScriptSDK.create({
      defaultChannel: Channel.H5,
      tts: { providerId: 'mock', config: {} },
      asr: { providerId: 'mock', config: {} },
    });
    sdk.setAudioPlayer(new HeadlessAudioPlayer());
    sdk.loadScript(sampleScript);
    sdk.startSession({
      sessionId: 'S1', orderId: 'O1', customerId: 'C1', productId: 'P1',
      productType: ProductType.INSURANCE, channel: Channel.H5,
      salesUserId: 'M1', branchId: 'B1',
    });
    await sdk.executeNode('N001');
    const saved = sdk.getSession();
    const json = JSON.stringify(saved);

    const sdk2 = await ScriptSDK.create({
      defaultChannel: Channel.H5,
      tts: { providerId: 'mock', config: {} },
      asr: { providerId: 'mock', config: {} },
    });
    sdk2.loadScript(sampleScript);
    const resumed = sdk2.resumeSession(json);
    expect(resumed.sessionId).toBe('S1');
    expect(resumed.currentNodeIndex).toBe(1);
  });
});

// 注册测试用 mock
registerTTSProvider(new MockTTSProvider());
registerASRProvider(new MockASRProvider());
