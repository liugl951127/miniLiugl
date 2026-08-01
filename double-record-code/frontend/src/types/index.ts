/**
 * 全局类型定义
 */

// ============================================================
// 通用类型
// ============================================================
export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
  traceId: string;
  timestamp: number;
}

export interface PageResult<T> {
  total: number;
  page: number;
  size: number;
  records: T[];
}

// ============================================================
// 枚举常量
// ============================================================
export enum ProductType {
  INSURANCE = 1,    // 保险
  WEALTH = 2,       // 理财
  FUND = 3,         // 基金
  TRUST = 4,        // 信托
  GOLD = 5,         // 贵金属
}

export enum OrderState {
  RESERVED = 0,           // 已预约
  VERIFIED = 1,           // 已核验
  SCRIPT_RUNNING = 2,     // 话术执行中
  RECORDING = 3,          // 视频录制中
  SIGNING = 4,            // 电子签约
  QA_PASSED = 5,          // 质检通过
  COMPLETED = 6,          // 订单完成
  CANCELLED = -1,         // 已取消
  FAILED = -2,            // 已失败
}

export enum Channel {
  H5 = 1,           // 线上 H5
  MINI_PROGRAM = 2, // 小程序
  ATM = 3,          // 线下一体机
  PAD = 4,          // PAD
  PC = 5,           // 网点 PC
}

export enum RiskLevel {
  C1 = 'C1', // 保守型
  C2 = 'C2', // 稳健型
  C3 = 'C3', // 平衡型
  C4 = 'C4', // 成长型
  C5 = 'C5', // 进取型
}

export enum SessionState {
  NOT_STARTED = 0,
  RUNNING = 1,
  COMPLETED = 2,
  PAUSED = 3,
  FAILED = 4,
}

export enum QAVerdict {
  HIGH_PASS = 'HIGH_PASS',
  PASS = 'PASS',
  REVIEW = 'REVIEW',
  FAIL = 'FAIL',
}

export enum NodeType {
  GREETING = 'GREETING',
  PRODUCT = 'PRODUCT',
  RISK_DISCLOSURE = 'RISK_DISCLOSURE',
  SUITABILITY = 'SUITABILITY',
  COOLING_PERIOD = 'COOLING_PERIOD',
  CONFIRMATION = 'CONFIRMATION',
}

export enum ExceptionType {
  NETWORK = 'NETWORK',
  DEVICE = 'DEVICE',
  AV_ERROR = 'AV_ERROR',
  UPLOAD_ERROR = 'UPLOAD_ERROR',
  CUSTOMER_REFUSE = 'CUSTOMER_REFUSE',
  SCRIPT_INTERRUPT = 'SCRIPT_INTERRUPT',
  CUSTOMER_LEAVE = 'CUSTOMER_LEAVE',
  COMPLIANCE_FAIL = 'COMPLIANCE_FAIL',
}

// ============================================================
// 实体接口
// ============================================================
export interface Customer {
  customerId: number;
  customerNo: string;
  name: string;
  idType: number;
  idNo: string;
  mobile: string;
  riskLevel: RiskLevel;
  riskScore: number;
  kycStatus: number;
  vipLevel: number;
}

export interface Order {
  orderId: number;
  orderNo: string;
  customerId: number;
  productId: number;
  productType: ProductType;
  productName: string;
  amount: number;
  currency: string;
  state: OrderState;
  stateName?: string;
  channel: Channel;
  salesUserId?: number;
  reserveAt?: string;
  startedAt?: string;
  completedAt?: string;
  createdAt: string;
}

export interface ScriptNode {
  nodeId: number;
  scriptId: number;
  nodeSeq: number;
  nodeCode: string;          // N1, N2, ...
  nodeType: NodeType;
  nodeName: string;
  content: string;           // 标准话术文本
  isRequired: boolean;
  requireConfirm: boolean;
  minReadSeconds: number;
  keywords: string[];
  skipAllowed: boolean;
  riskLevel?: number;
  nextNodeRule?: string;
}

export interface Script {
  scriptId: number;
  scriptCode: string;
  scriptName: string;
  productType: ProductType;
  productIds: number[];
  version: string;
  isActive: boolean;
  isGray: boolean;
  effectiveDate: string;
  expireDate?: string;
  totalNodes: number;
  estimatedMinutes: number;
  nodes: ScriptNode[];
}

export interface NodeResult {
  nodeCode: string;
  result: 'PASS' | 'FAIL' | 'SKIP';
  duration: number;          // 实际阅读时长(秒)
  customerConfirmed: boolean;
  asrText?: string;          // ASR 转写
  detectedKeywords: string[];
  startedAt: string;
  endedAt: string;
}

export interface Session {
  sessionId: number;
  orderId: number;
  sessionSeq: number;
  channel: Channel;
  scriptId: number;
  scriptVersion: string;
  state: SessionState;
  videoUrl?: string;
  videoHash?: string;
  videoSize?: number;
  videoDuration?: number;
  startAt?: string;
  endAt?: string;
  pauseCount: number;
  interruptReason?: string;
  nodeResults: NodeResult[];
  trustTime?: string;
  blockChainTx?: string;
}

export interface RiskAssess {
  assessId: number;
  orderId: number;
  customerId: number;
  assessType: number;
  answers: Record<string, string | string[]>;
  totalScore: number;
  riskLevel: RiskLevel;
  productMatch: boolean;
  validUntil: string;
  createdAt: string;
}

export interface QualityResult {
  qaId: number;
  sessionId: number;
  orderId: number;
  qaType: number;
  qaStatus: number;
  totalScore: number;
  scriptScore: number;
  riskScore: number;
  confirmScore: number;
  avScore: number;
  flowScore: number;
  issues: QAIssue[];
  asrText?: string;
  sentimentScore?: number;
  verdict: QAVerdict;
  reviewerId?: number;
  reviewRemark?: string;
  reviewedAt?: string;
}

export interface QAIssue {
  level: 'ERROR' | 'WARN' | 'INFO';
  category: string;
  message: string;
  nodeCode?: string;
  timestamp: number;
}

export interface Contract {
  contractId: number;
  orderId: number;
  contractNo: string;
  contractType: number;
  fileUrl: string;
  fileHash: string;
  signMethod: number;
  signSerial?: string;
  signTime?: string;
  blockChainTx?: string;
  status: number;
}

// ============================================================
// 请求 DTO
// ============================================================
export interface CreateOrderDTO {
  customerId: number;
  productId: number;
  amount: number;
  channel: Channel;
  reserveAt?: string;
  salesUserId?: number;
  branchId?: number;
  terminalId?: string;
}

export interface StartSessionDTO {
  orderId: number;
  channel: Channel;
  terminalId: string;
  ipAddress?: string;
  location?: string;
}

export interface SubmitNodeResultDTO {
  sessionId: number;
  nodeCode: string;
  result: 'PASS' | 'FAIL' | 'SKIP';
  duration: number;
  customerConfirmed: boolean;
  asrText?: string;
  detectedKeywords: string[];
}

export interface SubmitRiskDTO {
  orderId: number;
  sessionId: number;
  answers: Record<string, string | string[]>;
}

export interface SignContractDTO {
  orderId: number;
  contractId: number;
  signMethod: number;
  smsCode?: string;
  faceImageBase64?: string;
}

export interface UploadVideoChunkDTO {
  sessionId: number;
  chunkIndex: number;
  totalChunks: number;
  chunkData: Blob;
  startTime: number;
  endTime: number;
}

// ============================================================
// 业务异常
// ============================================================
export class DualRecordError extends Error {
  constructor(
    public code: number,
    public message: string,
    public traceId?: string,
  ) {
    super(message);
    this.name = 'DualRecordError';
  }
}

export class NetworkException extends DualRecordError {
  constructor(message: string) {
    super(1001, message);
    this.name = 'NetworkException';
  }
}

export class AVException extends DualRecordError {
  constructor(message: string) {
    super(2001, message);
    this.name = 'AVException';
  }
}

export class ComplianceException extends DualRecordError {
  constructor(message: string, public issue?: string) {
    super(3001, `${message}${issue ? ': ' + issue : ''}`);
    this.name = 'ComplianceException';
  }
}
