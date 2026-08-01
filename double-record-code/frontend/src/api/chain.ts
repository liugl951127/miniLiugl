/**
 * 区块链存证 API - 与 Fabric 链码交互
 *
 * @author Mavis
 */

import request from '../utils/request';

export interface ChainEvidence {
  orderId: string;
  customerId: string;
  productType: string;
  productName: string;
  amount: number;
  videoHash: string;
  audioHash: string;
  videoSm3Hash: string;
  contractHash: string;
  scriptHash: string;
  scriptVersion: string;
  sessionId: string;
  channel: string;
  salesUserId: string;
  branchId: string;
  customerSm2Signature: string;
  managerSm2Signature: string;
  trustTimestamp: string;
  nodeResultsMerkle: string;
}

export interface ChainVerifyResult {
  orderId: string;
  videoValid: boolean;
  audioValid: boolean;
  contractValid: boolean;
  verified: boolean;
  txId: string;
  blockNum: number;
  createdAt: string;
  trustTimestamp: string;
  state: string;
  signatures: {
    customer: string;
    manager: string;
    witness: string;
  };
}

/**
 * 提交证据上链
 */
export const submitEvidenceToChain = (evidence: ChainEvidence) =>
  request.post<{ orderId: string; txId: string }>('/api/chain/evidence/submit', evidence);

/**
 * 查询链上证据
 */
export const queryChainEvidence = (orderId: string) =>
  request.get<ChainEvidence>(`/api/chain/evidence/${orderId}`);

/**
 * 验证链上证据(司法取证)
 */
export const verifyChainEvidence = (orderId: string, videoHash: string, audioHash: string, contractHash: string) =>
  request.post<ChainVerifyResult>('/api/chain/evidence/verify', {
    orderId,
    videoHash,
    audioHash,
    contractHash,
  });

/**
 * 查询链上证据历史
 */
export const getChainEvidenceHistory = (orderId: string) =>
  request.get<Array<{ txId: string; timestamp: string; value: string }>>(
    `/api/chain/evidence/${orderId}/history`
  );

/**
 * 提交合同存证
 */
export const submitContractToChain = (contract: {
  contractId: string;
  orderId: string;
  contractNo: string;
  contractType: number;
  fileUrl: string;
  fileHash: string;
  fileSize: number;
}) =>
  request.post<{ contractId: string; txId: string }>('/api/chain/contract/submit', contract);

/**
 * 签署合同
 */
export const signContractOnChain = (contractId: string, signMethod: number, signCert: string, sm2Sig: string) =>
  request.post<{ ok: boolean }>('/api/chain/contract/sign', {
    contractId,
    signMethod,
    signCert,
    sm2Sig,
  });

/**
 * 查询审计历史
 */
export const getChainAuditHistory = (orderId: string) =>
  request.get<Array<{
    id: string;
    orderId: string;
    action: string;
    operator: string;
    operatorOrg: string;
    oldState: string;
    newState: string;
    reason: string;
    timestamp: string;
    txId: string;
  }>>(`/api/chain/audit/${orderId}`);

/**
 * 注册公钥(管理员)
 */
export const registerPublicKey = (partyType: string, partyId: string, publicKeyHex: string) =>
  request.post<{ ok: boolean }>('/api/chain/publickey/register', {
    partyType,
    partyId,
    publicKeyHex,
  });

/**
 * 获取链码健康状态
 */
export const getChainHealth = () =>
  request.get<{
    status: 'UP' | 'DOWN';
    channel: string;
    chaincode: string;
    version: string;
    peers: Array<{ name: string; status: string }>;
  }>('/api/chain/health');
