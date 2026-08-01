/**
 * 风评与签约 API 服务
 */
import { get, post } from '@/utils/request';
import type { RiskAssess, SubmitRiskDTO, Contract, SignContractDTO } from '@/types';

export const riskApi = {
  /**
   * 获取风评问卷模板
   */
  getQuestionnaire(version = 'V6.0'): Promise<{
    questionnaireId: number;
    title: string;
    questions: any[];
    levelMapping: Record<string, [number, number, string]>;
  }> {
    return get('/v1/risk/questionnaire', { version });
  },

  /**
   * 提交风评答案并自动评分
   */
  submit(data: SubmitRiskDTO): Promise<RiskAssess> {
    return post<RiskAssess>('/v1/risk/submit', data);
  },

  /**
   * 客户最近一次有效评估
   */
  getLatest(customerId: number, productType?: number): Promise<RiskAssess> {
    return get<RiskAssess>('/v1/risk/latest', { customerId, productType });
  },

  /**
   * 客户评估历史
   */
  listHistory(customerId: number, page = 1, size = 10): Promise<{
    total: number;
    records: RiskAssess[];
  }> {
    return get(`/v1/risk/history/${customerId}`, { page, size });
  },
};

export const contractApi = {
  /**
   * 生成电子合同
   */
  generate(orderId: number): Promise<Contract> {
    return post<Contract>('/v1/contract/generate', { orderId });
  },

  /**
   * 查询合同详情
   */
  getById(contractId: number): Promise<Contract> {
    return get<Contract>(`/v1/contract/${contractId}`);
  },

  /**
   * 申请数字证书(用于 CA 签名)
   */
  applyCert(orderId: number, customerId: number, customerName: string, idNo: string): Promise<{ certId: string; certPem: string }> {
    return post('/v1/contract/cert/apply', { orderId, customerId, customerName, idNo });
  },

  /**
   * 发送短信验证码
   */
  sendSmsCode(contractId: number, mobile: string): Promise<{ requestId: string; expireAt: string }> {
    return post('/v1/contract/sms/send', { contractId, mobile });
  },

  /**
   * 验证短信码
   */
  verifySmsCode(requestId: string, code: string): Promise<{ valid: boolean }> {
    return post('/v1/contract/sms/verify', { requestId, code });
  },

  /**
   * CA 数字签名
   */
  signWithCA(data: SignContractDTO): Promise<Contract> {
    return post<Contract>('/v1/contract/sign/ca', data);
  },

  /**
   * 手写电子签名(上传签名图)
   */
  signWithHandwriting(contractId: number, signatureImageBase64: string): Promise<Contract> {
    return post<Contract>('/v1/contract/sign/handwriting', { contractId, signatureImageBase64 });
  },

  /**
   * 合同上链
   */
  uploadToBlockchain(contractId: number): Promise<{ txHash: string; blockHeight: number }> {
    return post('/v1/contract/blockchain/upload', { contractId });
  },

  /**
   * 合同作废
   */
  void(contractId: number, reason: string): Promise<void> {
    return post<void>(`/v1/contract/${contractId}/void`, { reason });
  },

  /**
   * 下载合同 PDF
   */
  download(contractId: number): Promise<{ url: string; filename: string }> {
    return get(`/v1/contract/${contractId}/download`);
  },
};
