/**
 * 话术会话管理
 *
 * <p>负责:
 * <ul>
 *   <li>会话状态机:创建 → 节点执行 → 完成
 *   <li>断点续传定位
 *   <li>NodeResult 累积
 *   <li>Merkle 根实时计算
 *   <li>会话快照(序列化/反序列化)
 * </ul>
 *
 * @author Mavis
 */

import {
  ScriptSession,
  NodeResult,
  Channel,
  ScriptNode,
  SDKError,
  ErrorCode,
} from '../types';
import { ScriptCore } from './script-core';

/**
 * 会话管理类
 */
export class ScriptSessionManager {
  private session: ScriptSession;
  private core: ScriptCore;

  constructor(
    core: ScriptCore,
    init: {
      sessionId: string;
      orderId: string;
      customerId: string;
      productId: string;
      productType: ScriptSession['productType'];
      channel: Channel;
      salesUserId: string;
      branchId: string;
      customerPublicKey?: string;
      managerPublicKey?: string;
      witnessUserId?: string;
    }
  ) {
    this.core = core;
    const now = new Date().toISOString();
    const model = core.getModel();

    this.session = {
      sessionId: init.sessionId,
      orderId: init.orderId,
      customerId: init.customerId,
      productId: init.productId,
      productType: init.productType,
      channel: init.channel,
      scriptId: model.scriptId,
      scriptVersion: model.version,
      scriptHash: model.hash || core.computeHash(),
      nodeResults: [],
      merkleRoot: '',
      startedAt: now,
      completed: false,
      currentNodeIndex: 0,
      salesUserId: init.salesUserId,
      branchId: init.branchId,
      customerPublicKey: init.customerPublicKey,
      managerPublicKey: init.managerPublicKey,
      witnessUserId: init.witnessUserId,
    };

    this.recomputeMerkle();
  }

  // ============================================================
  // 静态工厂
  // ============================================================

  /**
   * 从 JSON 恢复会话(断点续传)
   */
  static resume(core: ScriptCore, sessionJson: string): ScriptSessionManager {
    const data = JSON.parse(sessionJson) as ScriptSession;
    const mgr = new ScriptSessionManager(core, {
      sessionId: data.sessionId,
      orderId: data.orderId,
      customerId: data.customerId,
      productId: data.productId,
      productType: data.productType,
      channel: data.channel,
      salesUserId: data.salesUserId,
      branchId: data.branchId,
      customerPublicKey: data.customerPublicKey,
      managerPublicKey: data.managerPublicKey,
      witnessUserId: data.witnessUserId,
    });
    mgr.session = data;
    mgr.recomputeMerkle();
    return mgr;
  }

  // ============================================================
  // 访问器
  // ============================================================

  getSession(): ScriptSession {
    return { ...this.session };
  }

  /**
   * 下一个待执行节点
   */
  getNextNode(): ScriptNode | null {
    if (this.session.completed) return null;
    const nodes = this.core.getNodes();
    if (this.session.currentNodeIndex >= nodes.length) {
      this.complete();
      return null;
    }
    return nodes[this.session.currentNodeIndex];
  }

  /**
   * 进度百分比
   */
  getProgress(): number {
    const total = this.core.size();
    if (total === 0) return 100;
    return Math.round((this.session.currentNodeIndex / total) * 100);
  }

  // ============================================================
  // 节点执行
  // ============================================================

  /**
   * 记录节点结果
   */
  recordNodeResult(result: NodeResult): void {
    // 幂等性:同 nodeCode 覆盖
    const existingIdx = this.session.nodeResults.findIndex(
      (r) => r.nodeCode === result.nodeCode
    );
    if (existingIdx >= 0) {
      this.session.nodeResults[existingIdx] = result;
    } else {
      this.session.nodeResults.push(result);
    }

    // 更新 Merkle 根
    this.recomputeMerkle();

    // 推进当前节点
    const expected = this.core.getNodes()[this.session.currentNodeIndex];
    if (expected && expected.nodeCode === result.nodeCode) {
      this.session.currentNodeIndex++;
    }
  }

  /**
   * 批量回滚
   */
  rewindTo(nodeCode: string): void {
    const idx = this.core.getNodes().findIndex((n) => n.nodeCode === nodeCode);
    if (idx < 0) {
      throw new SDKError(ErrorCode.SCRIPT_ERROR, `节点不存在: ${nodeCode}`);
    }
    this.session.currentNodeIndex = idx;
    this.session.nodeResults = this.session.nodeResults.filter(
      (r) => this.core.getNodes().findIndex((n) => n.nodeCode === r.nodeCode) < idx
    );
    this.recomputeMerkle();
  }

  /**
   * 标记会话完成
   */
  complete(): void {
    this.session.completed = true;
    this.session.endedAt = new Date().toISOString();
    this.session.totalDuration =
      new Date(this.session.endedAt).getTime() - new Date(this.session.startedAt).getTime();
  }

  // ============================================================
  // 序列化(断点续传)
  // ============================================================

  toJSON(): string {
    return JSON.stringify(this.session);
  }

  /**
   * 序列化给后端上报的精简版
   */
  toReportPayload(): unknown {
    return {
      sessionId: this.session.sessionId,
      orderId: this.session.orderId,
      scriptId: this.session.scriptId,
      scriptVersion: this.session.scriptVersion,
      scriptHash: this.session.scriptHash,
      merkleRoot: this.session.merkleRoot,
      startedAt: this.session.startedAt,
      endedAt: this.session.endedAt,
      totalDuration: this.session.totalDuration,
      completed: this.session.completed,
      nodeResults: this.session.nodeResults,
    };
  }

  // ============================================================
  // 内部
  // ============================================================

  private recomputeMerkle(): void {
    const hashes = this.session.nodeResults.map((r) => r.hash);
    this.session.merkleRoot = ScriptCore.computeMerkleRoot(hashes);
  }
}
