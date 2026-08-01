/**
 * 业务管理 API - 客户/订单/会话/质检 完整接口
 */

import request from '../utils/request';

// ============================================================
// 客户
// ============================================================

export interface Customer {
  customerId: number;
  customerNo: string;
  name: string;
  idType: number;
  idNo: string;
  mobile: string;
  riskLevel: string;
  riskScore: number;
  kycStatus: number;
  customerType: number;
  vipLevel: number;
}

export const getCustomer = (id: number) =>
  request.get<Customer>(`/api/customers/${id}`);

export const searchCustomers = (keyword: string, page = 1, size = 20) =>
  request.get<{ items: Customer[]; total: number }>('/api/customers', {
    params: { keyword, page, size },
  });

export const updateCustomerRisk = (id: number, riskLevel: string, riskScore: number) =>
  request.put<{ ok: boolean }>(`/api/customers/${id}/risk`, { riskLevel, riskScore });

// ============================================================
// 订单
// ============================================================

export interface Order {
  orderId: number;
  orderNo: string;
  customerId: number;
  customerName: string;
  productId: number;
  productType: number;
  productName: string;
  amount: number;
  state: number;
  stateName: string;
  channel: number;
  salesUserId: number;
  branchId: number;
  startedAt: string;
  completedAt: string;
  createdAt: string;
}

export interface OrderListQuery {
  customerId?: number;
  salesUserId?: number;
  branchId?: number;
  state?: number;
  productType?: number;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}

export const createOrder = (data: Partial<Order>) =>
  request.post<Order>('/api/orders', data);

export const getOrder = (id: number) =>
  request.get<Order>(`/api/orders/${id}`);

export const searchOrders = (query: OrderListQuery) =>
  request.get<{ items: Order[]; total: number }>('/api/orders', { params: query });

export const cancelOrder = (id: number, reason: string) =>
  request.post<{ ok: boolean }>(`/api/orders/${id}/cancel`, { reason });

// ============================================================
// 会话
// ============================================================

export interface ScriptSession {
  sessionId: number;
  orderId: number;
  sessionSeq: number;
  channel: number;
  state: number;
  videoUrl: string;
  videoHash: string;
  videoDuration: number;
  startAt: string;
  endAt: string;
  blockChainTx: string;
  merkleRoot: string;
  nodeResults?: NodeResult[];
}

export interface NodeResult {
  nodeCode: string;
  nodeName: string;
  result: number;
  durationMs: number;
  customerSaid: string;
  keywordsHit: string[];
  asrConfidence: number;
  audioHash: string;
  startedAt: string;
  endedAt: string;
  resultHash: string;
}

export const createSession = (orderId: number, data: Partial<ScriptSession>) =>
  request.post<ScriptSession>(`/api/orders/${orderId}/sessions`, data);

export const getSession = (id: number) =>
  request.get<ScriptSession>(`/api/sessions/${id}`);

export const listOrderSessions = (orderId: number) =>
  request.get<ScriptSession[]>(`/api/orders/${orderId}/sessions`);

export const uploadNodeResult = (sessionId: number, result: NodeResult) =>
  request.post<{ ok: boolean; merkleRoot: string }>(`/api/sessions/${sessionId}/nodes`, result);

export const completeSession = (sessionId: number) =>
  request.post<{ ok: boolean; merkleRoot: string }>(`/api/sessions/${sessionId}/complete`);

// ============================================================
// 风险评估
// ============================================================

export interface RiskAssess {
  assessId: number;
  orderId: number;
  customerId: number;
  sessionId: number;
  answers: Record<string, any>;
  totalScore: number;
  riskLevel: string;
  productMatch: boolean;
  validUntil: string;
}

export const submitRiskAssess = (data: Partial<RiskAssess>) =>
  request.post<RiskAssess>('/api/risk-assessments', data);

export const getRiskAssess = (id: number) =>
  request.get<RiskAssess>(`/api/risk-assessments/${id}`);

// ============================================================
// 质检
// ============================================================

export interface QualityReport {
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
  verdict: string;
  issues: string[];
  asrText: string;
  reviewerId: number;
  reviewRemark: string;
  reviewedAt: string;
  createdAt: string;
}

export const getQualityReport = (sessionId: number) =>
  request.get<QualityReport>(`/api/sessions/${sessionId}/quality`);

export const submitAppeal = (qaId: number, reason: string) =>
  request.post<{ ok: boolean }>(`/api/quality/${qaId}/appeal`, { reason });

// ============================================================
// 合同
// ============================================================

export interface Contract {
  contractId: number;
  orderId: number;
  customerId: number;
  contractNo: string;
  contractType: number;
  fileUrl: string;
  fileHash: string;
  fileSize: number;
  signMethod: number;
  status: number;
  signTime: string;
  blockChainTx: string;
  signedAt: string;
}

export const listOrderContracts = (orderId: number) =>
  request.get<Contract[]>(`/api/orders/${orderId}/contracts`);

export const downloadContract = (id: number) =>
  request.get<Blob>(`/api/contracts/${id}/download`, { responseType: 'blob' });

// ============================================================
// 话术模板
// ============================================================

export interface ScriptTemplate {
  scriptId: number;
  scriptCode: string;
  scriptName: string;
  productType: number;
  productIds: number[];
  version: string;
  isActive: boolean;
  effectiveDate: string;
  expireDate: string;
  totalNodes: number;
  estimatedMinutes: number;
}

export const getActiveScript = (productType: number, productId?: number) =>
  request.get<ScriptTemplate>('/api/scripts/active', {
    params: { productType, productId },
  });

export const listScriptVersions = (scriptCode: string) =>
  request.get<ScriptTemplate[]>(`/api/scripts/${scriptCode}/versions`);

// ============================================================
// 报告
// ============================================================

export const getDailyReport = (date: string, branchId?: number) =>
  request.get<{
    reportDate: string;
    branchId: number;
    totalOrders: number;
    completedOrders: number;
    failedOrders: number;
    totalAmount: number;
    highPassCount: number;
    passCount: number;
    reviewCount: number;
    failCount: number;
  }>('/api/reports/daily', { params: { date, branchId } });

export const exportReport = (startDate: string, endDate: string, format: 'xlsx' | 'pdf') =>
  request.get<Blob>('/api/reports/export', {
    params: { startDate, endDate, format },
    responseType: 'blob',
  });
