/**
 * 双录 Script SDK - 统一入口
 *
 * <p>7 模块架构:
 * <ol>
 *   <li>types - 所有接口与类型
 *   <li>core - 话术核心(ScriptCore / ScriptSession / KeywordDetector / SM3)
 *   <li>tts-adapter - TTS 多厂商适配(阿里云/腾讯云/Mock)
 *   <li>asr-adapter - ASR 多厂商适配(阿里云/腾讯云/Mock)
 *   <li>renderer - 多模态渲染(TTS+字幕+动画)
 *   <li>sync - WebSocket 同步
 *   <li>index - 统一编排 API(本文件)
 * </ol>
 *
 * <p>设计哲学:业务侧只关心 ScriptModel,SDK 负责渲染、同步、关键词、签名、上链
 * <p>零业务改动跨 H5/PAD/PC 三端
 *
 * @author Mavis
 * @license Apache-2.0
 */

// ============================================================
// 公共类型
// ============================================================
export * from './types';

// ============================================================
// Core
// ============================================================
export {
  ScriptCore,
  ScriptSessionManager,
  KeywordDetector,
  sm3,
  sm3Hex,
  sm3String,
  bytesToHex,
  hexToBytes,
} from './core';

export type { ScriptContext } from './core/script-core';

// ============================================================
// TTS
// ============================================================
export {
  registerTTSProvider,
  getTTSProvider,
  listTTSProviders,
  unregisterTTSProvider,
  AliyunTTSProvider,
  TencentTTSProvider,
  MockTTSProvider,
} from './tts-adapter';

// ============================================================
// ASR
// ============================================================
export {
  registerASRProvider,
  getASRProvider,
  listASRProviders,
  unregisterASRProvider,
  AliyunASRProvider,
  TencentASRProvider,
  MockASRProvider,
} from './asr-adapter';

// ============================================================
// Renderer
// ============================================================
export {
  Renderer,
  HTML5AudioPlayer,
  SubtitleList,
} from './renderer';
export type { AudioPlayer } from './renderer';

// ============================================================
// Sync
// ============================================================
export {
  SyncClient,
  BrowserWebSocket,
} from './sync';
export type { WebSocketLike } from './sync';

// ============================================================
// 编排入口(本文件下半部分)
// ============================================================
import {
  SDKConfig,
  ScriptModel,
  ScriptNode,
  NodeResult,
  NodeResultStatus,
  Channel,
  ProductType,
  RenderEventHandler,
  ScriptSession,
  SDKError,
  ErrorCode,
  KeywordMatchResult,
  RendererConfig,
  ASRProvider,
  TTSProvider,
} from './types';
import { ScriptCore, ScriptSessionManager, KeywordDetector } from './core';
import { registerTTSProvider, getTTSProvider, MockTTSProvider } from './tts-adapter';
import { registerASRProvider, MockASRProvider, getASRProvider } from './asr-adapter';
import { Renderer, AudioPlayer } from './renderer';
import { SyncClient, WebSocketLike } from './sync';

/**
 * 统一编排器 - SDK 入口
 *
 * <p>一次创建,多端复用,业务侧只需要调用:
 * <pre>
 *   const sdk = await ScriptSDK.create({ defaultChannel: Channel.H5, ... });
 *   await sdk.loadScript(scriptModel);
 *   await sdk.startSession({ orderId, customerId, ... });
 *   await sdk.executeNode(nodeCode, customerAudio);
 *   await sdk.endSession();
 * </pre>
 */
export class ScriptSDK {
  private config: SDKConfig;
  private core: ScriptCore | null = null;
  private session: ScriptSessionManager | null = null;
  private renderer: Renderer;
  private sync: SyncClient | null = null;

  private constructor(config: SDKConfig, renderer: Renderer) {
    this.config = config;
    this.renderer = renderer;
  }

  // ============================================================
  // 工厂方法
  // ============================================================

  /**
   * 创建并初始化 SDK
   */
  static async create(config: SDKConfig): Promise<ScriptSDK> {
    // 1. 注册默认 Provider
    ScriptSDK.registerDefaultProviders(config);

    // 2. 创建渲染器
    const rendererConfig: RendererConfig = {
      channel: config.defaultChannel,
      enableTTS: true,
      enableSubtitle: true,
      enableAnimation: true,
      ttsProviderId: config.tts?.providerId,
      asrProviderId: config.asr?.providerId,
      ...config.renderer,
    };
    const renderer = new Renderer(rendererConfig);

    const sdk = new ScriptSDK(config, renderer);

    // 3. 健康检查
    if (config.tts?.providerId) {
      const ok = await getTTSProvider(config.tts.providerId).healthCheck();
      if (!ok) {
        // eslint-disable-next-line no-console
        console.warn(`[ScriptSDK] TTS Provider ${config.tts.providerId} unhealthy, fallback to mock`);
        registerTTSProvider(new MockTTSProvider());
      }
    }

    return sdk;
  }

  /**
   * 注册默认 Provider(简化使用)
   */
  private static registerDefaultProviders(config: SDKConfig): void {
    // Mock 默认注册(总是)
    registerTTSProvider(new MockTTSProvider('mock', 'Mock TTS'));
    registerASRProvider(new MockASRProvider('mock', 'Mock ASR'));

    // 业务配置 - 按需懒加载
    if (config.tts?.providerId === 'aliyun') {
      // 业务需自己 import
    }
  }

  // ============================================================
  // 加载脚本
  // ============================================================

  /**
   * 加载话术模型
   */
  loadScript(model: ScriptModel): void {
    this.core = ScriptCore.fromObject(model);
    // 强制计算 hash
    model.hash = model.hash || this.core.computeHash();
  }

  /**
   * 从 JSON 加载
   */
  loadScriptFromJSON(json: string): void {
    const model = JSON.parse(json) as ScriptModel;
    this.loadScript(model);
  }

  // ============================================================
  // 渲染事件订阅
  // ============================================================

  onRenderEvent(handler: RenderEventHandler): () => void {
    return this.renderer.on(handler);
  }

  // ============================================================
  // 会话管理
  // ============================================================

  /**
   * 开启一次双录会话
   */
  startSession(init: {
    sessionId: string;
    orderId: string;
    customerId: string;
    productId: string;
    productType: ProductType;
    channel: Channel;
    salesUserId: string;
    branchId: string;
    customerPublicKey?: string;
    managerPublicKey?: string;
    witnessUserId?: string;
  }): ScriptSession {
    if (!this.core) {
      throw new SDKError(ErrorCode.SCRIPT_ERROR, '未加载脚本');
    }
    this.session = new ScriptSessionManager(this.core, init);
    if (this.sync) {
      this.sync.sendFullSession(init.sessionId, init.orderId, this.session.getSession());
    }
    return this.session.getSession();
  }

  /**
   * 断点续传
   */
  resumeSession(sessionJson: string): ScriptSession {
    if (!this.core) {
      throw new SDKError(ErrorCode.SCRIPT_ERROR, '未加载脚本');
    }
    this.session = ScriptSessionManager.resume(this.core, sessionJson);
    return this.session.getSession();
  }

  /**
   * 结束会话
   */
  endSession(): ScriptSession {
    if (!this.session) {
      throw new SDKError(ErrorCode.SCRIPT_ERROR, '会话未开启');
    }
    this.session.complete();
    if (this.sync) {
      this.sync.sendFullSession(
        this.session.getSession().sessionId,
        this.session.getSession().orderId,
        this.session.getSession()
      );
    }
    this.renderer.emitComplete(this.session.getSession().sessionId);
    return this.session.getSession();
  }

  getSession(): ScriptSession | null {
    return this.session?.getSession() || null;
  }

  // ============================================================
  // 节点执行(核心流程)
  // ============================================================

  /**
   * 执行一个节点(主流程)
   *
   * <p>自动处理:
   * <ol>
   *   <li>经理播报 → TTS + 字幕
   *   <li>客户回答 → ASR + 关键词检测
   *   <li>签名 → 留接口
   *   <li>同步 → WebSocket
   *   <li>结果 → 上链数据
   * </ol>
   */
  async executeNode(
    nodeCode: string,
    options?: {
      customerAudio?: Uint8Array;
      customerResponseKeywords?: string[];
      onCustomerResponse?: (text: string) => void;
    }
  ): Promise<{
    node: ScriptNode;
    result: NodeResult;
    keywordMatch?: KeywordMatchResult;
  }> {
    if (!this.core || !this.session) {
      throw new SDKError(ErrorCode.SCRIPT_ERROR, '脚本/会话未就绪');
    }

    const node = this.core.findNode(nodeCode);
    if (!node) {
      throw new SDKError(ErrorCode.SCRIPT_ERROR, `节点不存在: ${nodeCode}`);
    }

    const session = this.session.getSession();
    const startedAt = new Date().toISOString();
    const startTime = Date.now();

    // 1. 渲染
    await this.renderer.renderNode(node, session.sessionId);

    // 2. 如果是客户响应,调 ASR
    let asrResult: { text: string; confidence: number } | null = null;
    let keywordMatch: KeywordMatchResult | null = null;
    if (node.nodeType === 'CUSTOMER_RESPOND' && options?.customerAudio) {
      const asr = getASRProvider(this.config.asr?.providerId || 'mock');
      asrResult = await asr.recognize({
        audio: options.customerAudio,
        sampleRate: 16000,
        format: 'pcm',
      });

      const detector = new KeywordDetector(
        node.mustHitKeywords || [],
        options.customerResponseKeywords || node.customerResponseKeywords || []
      );
      keywordMatch = detector.detect(asrResult.text);

      if (options.onCustomerResponse) {
        options.onCustomerResponse(asrResult.text);
      }

      // 字幕
      this.renderer.addSubtitle({
        id: `${session.sessionId}-${nodeCode}-${Date.now()}`,
        text: asrResult.text,
        speaker: 'CUSTOMER',
        startTime: Date.now(),
        endTime: Date.now() + 3000,
        highlights: keywordMatch ? keywordMatch.mustHitMatched : [],
      });
    }

    // 3. 构造 NodeResult
    const endedAt = new Date().toISOString();
    const duration = Date.now() - startTime;
    const result: NodeResult = {
      nodeCode,
      nodeName: node.nodeName,
      result:
        node.nodeType === 'CUSTOMER_RESPOND'
          ? keywordMatch && !keywordMatch.passed
            ? NodeResultStatus.FAIL
            : NodeResultStatus.PASS
          : NodeResultStatus.PASS,
      duration,
      customerSaid: asrResult?.text,
      keywordsHit: keywordMatch?.mustHitMatched,
      asrConfidence: asrResult?.confidence,
      audioHash: asrResult ? asrResult.text : undefined, // 简化:实际应是音频 SM3
      startedAt,
      endedAt,
      hash: '', // 待计算
    };
    result.hash = ScriptCore.computeNodeResultHash(result);

    // 4. 记录到会话
    this.session.recordNodeResult(result);

    // 5. 上报进度
    this.renderer.emitProgress(session.sessionId, this.session.getProgress(), nodeCode);
    if (this.sync) {
      this.sync.sendProgress(session.sessionId, session.orderId, this.session.getProgress(), nodeCode);
      this.sync.sendNodeResult(session.sessionId, session.orderId, nodeCode, result);
    }

    return { node, result, keywordMatch: keywordMatch || undefined };
  }

  /**
   * 跳到下一节点(无客户回答)
   */
  async next(): Promise<{
    node: ScriptNode;
    result: NodeResult;
  } | null> {
    if (!this.session) {
      throw new SDKError(ErrorCode.SCRIPT_ERROR, '会话未开启');
    }
    const next = this.session.getNextNode();
    if (!next) return null;
    const r = await this.executeNode(next.nodeCode);
    return { node: r.node, result: r.result };
  }

  /**
   * 整体执行(自动遍历所有节点)
   *
   * <p>注意:需要业务侧提供客户回答输入
   */
  async executeAll(inputResolver: (node: ScriptNode) => Promise<Uint8Array | undefined>): Promise<NodeResult[]> {
    if (!this.session) {
      throw new SDKError(ErrorCode.SCRIPT_ERROR, '会话未开启');
    }
    const results: NodeResult[] = [];
    while (true) {
      const next = this.session.getNextNode();
      if (!next) break;
      let audio: Uint8Array | undefined;
      if (next.nodeType === 'CUSTOMER_RESPOND') {
        audio = await inputResolver(next);
      }
      const r = await this.executeNode(next.nodeCode, { customerAudio: audio });
      results.push(r.result);
    }
    return results;
  }

  // ============================================================
  // 关键词检测(对外暴露)
  // ============================================================

  detectKeywords(text: string, mustHit: string[], optional: string[]): KeywordMatchResult {
    return new KeywordDetector(mustHit, optional).detect(text);
  }

  // ============================================================
  // 同步
  // ============================================================

  /**
   * 启动 WebSocket 同步
   */
  async startSync(config: {
    url: string;
    deviceId: string;
    userId: string;
    authToken?: string;
  }): Promise<void> {
    this.sync = new SyncClient({
      url: config.url,
      deviceId: config.deviceId,
      userId: config.userId,
      authToken: config.authToken,
      heartbeatInterval: 30000,
      reconnectInterval: 5000,
      maxReconnects: 10,
      autoReconnect: true,
    });
    await this.sync.connect();
  }

  stopSync(): void {
    this.sync?.disconnect();
    this.sync = null;
  }

  // ============================================================
  // 跨端适配
  // ============================================================

  /**
   * 注入自定义音频播放器
   */
  setAudioPlayer(player: AudioPlayer): void {
    this.renderer.setAudioPlayer(player);
  }

  /**
   * 注入自定义 WebSocket(用于 Node / 微信小程序)
   */
  setWebSocket(socket: WebSocketLike): void {
    if (this.sync) this.sync.setSocket(socket);
  }

  /**
   * 注入自定义 TTS Provider
   */
  setTTSProvider(provider: TTSProvider): void {
    registerTTSProvider(provider);
    this.renderer.setTTSProvider(provider);
  }

  /**
   * 注入自定义 ASR Provider
   */
  setASRProvider(provider: ASRProvider): void {
    registerASRProvider(provider);
  }

  // ============================================================
  // 上链数据导出
  // ============================================================

  /**
   * 获取可上链的会话数据(Merkle 根已算)
   */
  getChainPayload(): unknown {
    return this.session?.toReportPayload() || null;
  }

  // ============================================================
  // 工具
  // ============================================================

  getScriptCore(): ScriptCore | null {
    return this.core;
  }

  getRenderer(): Renderer {
    return this.renderer;
  }

  getConfig(): SDKConfig {
    return this.config;
  }
}

// 默认导出
export default ScriptSDK;
