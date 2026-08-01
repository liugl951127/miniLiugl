/**
 * 双录 Script SDK - 类型定义
 *
 * @description 整个 SDK 的接口、枚举、类型统一在此
 * @author Mavis
 * @license Apache-2.0
 */

// ============================================================
// 基础枚举
// ============================================================

/**
 * 节点类型
 */
export enum NodeType {
  /** 静态文本(只显示不读) */
  TEXT = 'TEXT',
  /** 经理播报(需要 TTS) */
  MANAGER_SPEAK = 'MANAGER_SPEAK',
  /** 客户确认(需要 ASR) */
  CUSTOMER_RESPOND = 'CUSTOMER_RESPOND',
  /** 风险揭示(强制确认) */
  RISK_DISCLOSURE = 'RISK_DISCLOSURE',
  /** 资料上传(影像采集) */
  UPLOAD_DOC = 'UPLOAD_DOC',
  /** 电子签字(CA 认证) */
  E_SIGN = 'E_SIGN',
  /** 视频录制(开始/结束标记) */
  VIDEO_RECORD = 'VIDEO_RECORD',
  /** 信息告知 */
  NOTIFY = 'NOTIFY',
  /** 等待客户阅读 */
  CUSTOMER_READ = 'CUSTOMER_READ',
}

/**
 * 节点执行结果
 */
export enum NodeResultStatus {
  /** 通过 */
  PASS = 'PASS',
  /** 失败 */
  FAIL = 'FAIL',
  /** 跳过(可选节点) */
  SKIP = 'SKIP',
  /** 超时 */
  TIMEOUT = 'TIMEOUT',
  /** 重做 */
  RETRY = 'RETRY',
}

/**
 * 终端类型
 */
export enum Channel {
  H5 = 'H5',
  PAD = 'PAD',
  PC = 'PC',
  MINI_PROGRAM = 'MINI_PROGRAM',
  APP_IOS = 'APP_IOS',
  APP_ANDROID = 'APP_ANDROID',
}

/**
 * 风险等级
 */
export enum RiskLevel {
  LOW = 'LOW',
  MIDDLE = 'MIDDLE',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL',
}

/**
 * 产品类型(与 Java 链码对齐)
 */
export enum ProductType {
  INSURANCE = 'INSURANCE',
  WEALTH = 'WEALTH',
  FUND = 'FUND',
  TRUST = 'TRUST',
  GOLD = 'GOLD',
}

// ============================================================
// 话术节点
// ============================================================

/**
 * 话术节点 - 一次双录会话的最小执行单元
 */
export interface ScriptNode {
  /** 节点编号(全局唯一) */
  nodeCode: string;
  /** 节点名称(中文) */
  nodeName: string;
  /** 节点类型 */
  nodeType: NodeType;
  /** 节点顺序(从 1 开始) */
  order: number;
  /** 节点文本(经理要说的话 / 客户要确认的内容) */
  text: string;
  /** 客户应回答的关键词(任一命中即通过) */
  customerResponseKeywords?: string[];
  /** 必答关键词(必须全部命中) */
  mustHitKeywords?: string[];
  /** 节点超时时间(秒) */
  timeout?: number;
  /** 是否可跳过 */
  skippable?: boolean;
  /** 是否可重做 */
  retryable?: boolean;
  /** 最大重做次数 */
  maxRetries?: number;
  /** 风险等级(触发复核) */
  riskLevel?: RiskLevel;
  /** 关联附件 ID(合同 PDF 等) */
  attachmentId?: string;
  /** 子节点(嵌套流程) */
  children?: ScriptNode[];
  /** 条件触发表达式,如 "${customer.age} >= 60" */
  condition?: string;
  /** 节点额外元数据 */
  metadata?: Record<string, unknown>;
  /** 适用渠道(空=全部) */
  channels?: Channel[];
}

// ============================================================
// 话术脚本
// ============================================================

/**
 * 话术脚本 - 一个产品的完整话术流程
 */
export interface ScriptModel {
  /** 脚本 ID(全局唯一 UUID) */
  scriptId: string;
  /** 脚本版本号(语义化版本) */
  version: string;
  /** 产品类型 */
  productType: ProductType;
  /** 脚本名称 */
  name: string;
  /** 脚本描述 */
  description?: string;
  /** 脚本适用产品 ID(可多个) */
  productIds?: string[];
  /** 节点列表(按 order 排序) */
  nodes: ScriptNode[];
  /** 总预计耗时(秒) */
  estimatedDuration?: number;
  /** 脚本作者 */
  author?: string;
  /** 创建时间 */
  createdAt: string;
  /** 更新时间 */
  updatedAt: string;
  /** 适用范围 */
  scope?: {
    productTypes?: ProductType[];
    riskLevels?: RiskLevel[];
    branches?: string[];
  };
  /** 元数据 */
  metadata?: Record<string, unknown>;
  /** 脚本指纹(SM3) */
  hash?: string;
  /** 兼容最低版本 */
  minSdkVersion?: string;
}

// ============================================================
// 节点执行结果
// ============================================================

/**
 * 节点执行结果 - 上链存证用
 */
export interface NodeResult {
  /** 节点编号 */
  nodeCode: string;
  /** 节点名称 */
  nodeName?: string;
  /** 结果 */
  result: NodeResultStatus;
  /** 耗时(毫秒) */
  duration: number;
  /** 客户实际回答文本(ASR 转写) */
  customerSaid?: string;
  /** 命中的关键词 */
  keywordsHit?: string[];
  /** ASR 置信度(0-1) */
  asrConfidence?: number;
  /** 客户原声音频指纹(SM3) */
  audioHash?: string;
  /** 客户原声视频指纹(分片,SHA-256) */
  videoHash?: string;
  /** TTS 合成音频指纹 */
  ttsAudioHash?: string;
  /** 经理签发时间(ISO 8601) */
  startedAt: string;
  /** 节点结束时间(ISO 8601) */
  endedAt: string;
  /** 节点指纹(SM3) - 用于 Merkle 树 */
  hash: string;
  /** 重做次数 */
  retries?: number;
  /** 错误信息 */
  errorMessage?: string;
  /** 附件文件指纹 */
  attachmentHashes?: Record<string, string>;
}

/**
 * 话术会话 - 一次完整双录的执行结果
 */
export interface ScriptSession {
  /** 会话 ID(全局唯一) */
  sessionId: string;
  /** 订单号(双录业务系统传入) */
  orderId: string;
  /** 客户编号 */
  customerId: string;
  /** 产品 ID */
  productId: string;
  /** 产品类型 */
  productType: ProductType;
  /** 渠道 */
  channel: Channel;
  /** 使用的脚本 */
  scriptId: string;
  /** 脚本版本 */
  scriptVersion: string;
  /** 脚本指纹 */
  scriptHash: string;
  /** 各节点执行结果(按 order 排序) */
  nodeResults: NodeResult[];
  /** Merkle 根(所有 nodeResult.hash 聚合) */
  merkleRoot: string;
  /** 会话开始时间 */
  startedAt: string;
  /** 会话结束时间 */
  endedAt?: string;
  /** 总会话耗时(毫秒) */
  totalDuration?: number;
  /** 会话是否完成 */
  completed: boolean;
  /** 当前节点序号(用于断点续传) */
  currentNodeIndex: number;
  /** 销售经理 ID */
  salesUserId: string;
  /** 网点编号 */
  branchId: string;
  /** 见证人 ID(可选) */
  witnessUserId?: string;
  /** 客户 SM2 公钥 */
  customerPublicKey?: string;
  /** 经理 SM2 公钥 */
  managerPublicKey?: string;
  /** 客户 SM2 签名 */
  customerSignature?: string;
  /** 经理 SM2 签名 */
  managerSignature?: string;
  /** 会话元数据 */
  metadata?: Record<string, unknown>;
}

// ============================================================
// 关键词检测
// ============================================================

/**
 * 关键词检测结果
 */
export interface KeywordMatchResult {
  /** 是否命中必答词 */
  mustHitPassed: boolean;
  /** 命中必答关键词 */
  mustHitMatched: string[];
  /** 未命中必答关键词 */
  mustHitMissed: string[];
  /** 命中可答关键词 */
  optionalMatched: string[];
  /** 总命中率 */
  hitRate: number;
  /** 检测是否通过(无必答词或全部命中) */
  passed: boolean;
}

// ============================================================
// TTS 适配
// ============================================================

/**
 * TTS 语音合成参数
 */
export interface TTSConfig {
  /** 文本 */
  text: string;
  /** 语音 ID(具体由 Provider 决定) */
  voiceId?: string;
  /** 语速(0.5-2.0,1.0 正常) */
  speed?: number;
  /** 音量(0-10) */
  volume?: number;
  /** 音调(-12 ~ 12) */
  pitch?: number;
  /** 情感(happy/sad/angry/neutral) */
  emotion?: string;
  /** 采样率 */
  sampleRate?: 8000 | 16000 | 24000;
  /** 音频格式 */
  format?: 'mp3' | 'wav' | 'pcm';
  /** 是否流式 */
  streaming?: boolean;
}

/**
 * TTS 合成结果
 */
export interface TTSResult {
  /** 音频数据(Buffer/Uint8Array) */
  audio: Uint8Array;
  /** 时长(毫秒) */
  duration: number;
  /** 音频指纹(SM3) */
  audioHash: string;
  /** 采样率 */
  sampleRate: number;
  /** 格式 */
  format: string;
  /** 原始数据(供存储) */
  raw?: Uint8Array;
  /** 任务 ID(异步场景) */
  taskId?: string;
}

/**
 * TTS Provider 接口
 */
export interface TTSProvider {
  /** Provider ID */
  readonly id: string;
  /** Provider 名称 */
  readonly name: string;
  /** 是否流式 */
  readonly streaming: boolean;
  /** 同步合成 */
  synthesize(config: TTSConfig): Promise<TTSResult>;
  /** 流式合成(可选) */
  synthesizeStream?(config: TTSConfig, onChunk: (chunk: Uint8Array) => void): Promise<TTSResult>;
  /** 健康检查 */
  healthCheck(): Promise<boolean>;
}

// ============================================================
// ASR 适配
// ============================================================

/**
 * ASR 识别参数
 */
export interface ASRConfig {
  /** 音频数据 */
  audio: Uint8Array;
  /** 采样率(8000/16000) */
  sampleRate: 8000 | 16000;
  /** 音频格式 */
  format: 'pcm' | 'wav' | 'mp3' | 'opus';
  /** 语言(中文 zh-CN / 英文 en-US) */
  language?: string;
  /** 是否流式 */
  streaming?: boolean;
  /** 领域(金融/通用) */
  domain?: 'finance' | 'general' | 'insurance';
  /** 是否启用标点 */
  enablePunctuation?: boolean;
}

/**
 * ASR 识别结果
 */
export interface ASRResult {
  /** 识别文本 */
  text: string;
  /** 置信度(0-1) */
  confidence: number;
  /** 句子级别结果 */
  sentences?: ASRSentence[];
  /** 是否完成 */
  isFinal: boolean;
  /** 音频时长(毫秒) */
  audioDuration: number;
  /** 任务 ID */
  taskId?: string;
  /** 音频指纹 */
  audioHash?: string;
}

/**
 * ASR 句子级别结果
 */
export interface ASRSentence {
  /** 句子文本 */
  text: string;
  /** 开始时间(毫秒) */
  startTime: number;
  /** 结束时间(毫秒) */
  endTime: number;
  /** 置信度 */
  confidence: number;
  /** 词级别结果 */
  words?: ASRWord[];
}

/**
 * ASR 词级别结果
 */
export interface ASRWord {
  text: string;
  startTime: number;
  endTime: number;
  confidence: number;
}

/**
 * ASR Provider 接口
 */
export interface ASRProvider {
  /** Provider ID */
  readonly id: string;
  /** Provider 名称 */
  readonly name: string;
  /** 是否流式 */
  readonly streaming: boolean;
  /** 一次性识别 */
  recognize(config: ASRConfig): Promise<ASRResult>;
  /** 流式识别(可选) */
  recognizeStream?(config: ASRConfig, onResult: (result: ASRResult) => void): Promise<ASRResult>;
  /** 健康检查 */
  healthCheck(): Promise<boolean>;
}

// ============================================================
// 渲染层
// ============================================================

/**
 * 渲染事件
 */
export interface RenderEvent {
  /** 事件类型 */
  type:
    | 'NODE_START'
    | 'NODE_END'
    | 'TTS_START'
    | 'TTS_END'
    | 'ASR_START'
    | 'ASR_RESULT'
    | 'ASR_END'
    | 'SUBTITLE_UPDATE'
    | 'PROGRESS'
    | 'ERROR'
    | 'COMPLETE';
  /** 会话 ID */
  sessionId: string;
  /** 节点编号 */
  nodeCode?: string;
  /** 事件时间 */
  timestamp: string;
  /** 事件数据 */
  data?: unknown;
}

/**
 * 渲染事件回调
 */
export type RenderEventHandler = (event: RenderEvent) => void;

/**
 * 字幕项
 */
export interface SubtitleItem {
  /** 字幕 ID */
  id: string;
  /** 文本 */
  text: string;
  /** 说话人(经理/客户) */
  speaker: 'MANAGER' | 'CUSTOMER' | 'SYSTEM';
  /** 开始时间(毫秒) */
  startTime: number;
  /** 结束时间(毫秒) */
  endTime: number;
  /** 高亮关键词 */
  highlights?: string[];
}

/**
 * 渲染层配置
 */
export interface RendererConfig {
  /** 终端 */
  channel: Channel;
  /** 是否启用 TTS 播放 */
  enableTTS: boolean;
  /** 是否启用字幕 */
  enableSubtitle: boolean;
  /** 是否启用动画 */
  enableAnimation: boolean;
  /** TTS Provider ID(默认 aliyun) */
  ttsProviderId?: string;
  /** ASR Provider ID(默认 aliyun) */
  asrProviderId?: string;
  /** 字幕最大显示行数 */
  subtitleMaxLines?: number;
  /** 自定义字幕字体大小 */
  subtitleFontSize?: number;
  /** 自定义背景色 */
  backgroundColor?: string;
  /** 自定义主题色 */
  themeColor?: string;
  /** 渲染目标(可选 DOM 元素) */
  target?: HTMLElement;
}

// ============================================================
// 同步层
// ============================================================

/**
 * 同步消息类型
 */
export enum SyncMessageType {
  /** 心跳 */
  HEARTBEAT = 'HEARTBEAT',
  /** 节点开始 */
  NODE_START = 'NODE_START',
  /** 节点结束 */
  NODE_END = 'NODE_END',
  /** 节点结果 */
  NODE_RESULT = 'NODE_RESULT',
  /** 进度同步 */
  PROGRESS = 'PROGRESS',
  /** 断点续传 */
  RESUME = 'RESUME',
  /** 同步会话 */
  SYNC_SESSION = 'SYNC_SESSION',
  /** ACK */
  ACK = 'ACK',
  /** 错误 */
  ERROR = 'ERROR',
  /** 终止 */
  TERMINATE = 'TERMINATE',
}

/**
 * 同步消息
 */
export interface SyncMessage {
  /** 消息 ID(UUID) */
  msgId: string;
  /** 消息类型 */
  type: SyncMessageType;
  /** 会话 ID */
  sessionId: string;
  /** 订单号 */
  orderId: string;
  /** 设备 ID */
  deviceId: string;
  /** 时间戳(毫秒) */
  timestamp: number;
  /** 消息载荷 */
  payload: unknown;
  /** 签名(可选,SM2) */
  signature?: string;
}

/**
 * 同步配置
 */
export interface SyncConfig {
  /** WebSocket 服务地址 */
  url: string;
  /** 设备 ID */
  deviceId: string;
  /** 用户 ID */
  userId: string;
  /** 心跳间隔(毫秒) */
  heartbeatInterval?: number;
  /** 重连间隔(毫秒) */
  reconnectInterval?: number;
  /** 最大重连次数 */
  maxReconnects?: number;
  /** 超时(毫秒) */
  timeout?: number;
  /** 是否自动重连 */
  autoReconnect?: boolean;
  /** 协议子协议 */
  protocols?: string | string[];
  /** 鉴权 token */
  authToken?: string;
}

// ============================================================
// SDK 配置
// ============================================================

/**
 * SDK 全局配置
 */
export interface SDKConfig {
  /** 日志级别 */
  logLevel?: 'debug' | 'info' | 'warn' | 'error';
  /** 默认渠道 */
  defaultChannel: Channel;
  /** TTS 适配器配置 */
  tts?: {
    /** Provider ID(aliyun / tencent / iflytek / mock) */
    providerId: string;
    /** Provider 私有配置(API Key 等) */
    config: Record<string, string>;
  };
  /** ASR 适配器配置 */
  asr?: {
    providerId: string;
    config: Record<string, string>;
  };
  /** 渲染层默认配置 */
  renderer?: Partial<RendererConfig>;
  /** 同步层默认配置 */
  sync?: Partial<SyncConfig>;
  /** API 网关地址(用于脚本下发/结果上报) */
  apiBaseUrl?: string;
  /** SM3 哈希算法实现(浏览器/Node 不同) */
  hashImpl?: 'js-sha256' | 'crypto-js' | 'subtle' | 'manual';
}

// ============================================================
// 错误
// ============================================================

/**
 * SDK 错误码
 */
export enum ErrorCode {
  /** 配置错误 */
  CONFIG_ERROR = 'CONFIG_ERROR',
  /** 网络错误 */
  NETWORK_ERROR = 'NETWORK_ERROR',
  /** TTS 错误 */
  TTS_ERROR = 'TTS_ERROR',
  /** ASR 错误 */
  ASR_ERROR = 'ASR_ERROR',
  /** 脚本错误 */
  SCRIPT_ERROR = 'SCRIPT_ERROR',
  /** 关键词未命中 */
  KEYWORD_MISS = 'KEYWORD_MISS',
  /** 超时 */
  TIMEOUT = 'TIMEOUT',
  /** 权限拒绝 */
  PERMISSION_DENIED = 'PERMISSION_DENIED',
  /** 渲染错误 */
  RENDER_ERROR = 'RENDER_ERROR',
  /** 同步错误 */
  SYNC_ERROR = 'SYNC_ERROR',
  /** 未知 */
  UNKNOWN = 'UNKNOWN',
}

/**
 * SDK 错误
 */
export class SDKError extends Error {
  public readonly code: ErrorCode;
  public readonly cause?: Error;
  public readonly context?: Record<string, unknown>;

  constructor(
    code: ErrorCode,
    message: string,
    options?: { cause?: Error; context?: Record<string, unknown> }
  ) {
    super(message);
    this.name = 'SDKError';
    this.code = code;
    this.cause = options?.cause;
    this.context = options?.context;
  }
}
