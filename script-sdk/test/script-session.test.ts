/**
 * 会话管理测试
 */
import { ScriptCore } from '../src/core/script-core';
import { ScriptSessionManager } from '../src/core/script-session';
import { NodeResultStatus, NodeType, ProductType, Channel } from '../src/types';
import { ScriptCore as _SC } from '../src/core/script-core';

const script = {
  scriptId: 'TEST-1',
  version: '1.0.0',
  productType: ProductType.INSURANCE,
  name: '测试',
  createdAt: '2026-08-01T00:00:00Z',
  updatedAt: '2026-08-01T00:00:00Z',
  nodes: [
    { nodeCode: 'N001', nodeName: '节点1', nodeType: NodeType.MANAGER_SPEAK, order: 1, text: 'A' },
    { nodeCode: 'N002', nodeName: '节点2', nodeType: NodeType.CUSTOMER_RESPOND, order: 2, text: 'B' },
    { nodeCode: 'N003', nodeName: '节点3', nodeType: NodeType.MANAGER_SPEAK, order: 3, text: 'C' },
  ],
};

function makeSession() {
  const core = ScriptCore.fromObject(script);
  return new ScriptSessionManager(core, {
    sessionId: 'SES-001',
    orderId: 'ORD-001',
    customerId: 'C001',
    productId: 'P001',
    productType: ProductType.INSURANCE,
    channel: Channel.H5,
    salesUserId: 'M001',
    branchId: '2001',
  });
}

describe('ScriptSessionManager', () => {
  test('startSession 构造', () => {
    const m = makeSession();
    const s = m.getSession();
    expect(s.sessionId).toBe('SES-001');
    expect(s.completed).toBe(false);
    expect(s.currentNodeIndex).toBe(0);
  });

  test('getNextNode 推进', () => {
    const m = makeSession();
    expect(m.getNextNode()?.nodeCode).toBe('N001');
  });

  test('getProgress 初始 0', () => {
    const m = makeSession();
    expect(m.getProgress()).toBe(0);
  });

  test('recordNodeResult 推进', () => {
    const m = makeSession();
    m.recordNodeResult({
      nodeCode: 'N001',
      result: NodeResultStatus.PASS,
      duration: 100,
      startedAt: '2026-08-01T00:00:00Z',
      endedAt: '2026-08-01T00:00:01Z',
      hash: 'a'.repeat(64),
    });
    expect(m.getSession().currentNodeIndex).toBe(1);
    expect(m.getNextNode()?.nodeCode).toBe('N002');
  });

  test('Merkle 根随结果更新', () => {
    const m = makeSession();
    expect(m.getSession().merkleRoot).toBe('');
    m.recordNodeResult({
      nodeCode: 'N001',
      result: NodeResultStatus.PASS,
      duration: 100,
      startedAt: '2026-08-01T00:00:00Z',
      endedAt: '2026-08-01T00:00:01Z',
      hash: 'a'.repeat(64),
    });
    expect(m.getSession().merkleRoot).toBe('a'.repeat(64));
    m.recordNodeResult({
      nodeCode: 'N002',
      result: NodeResultStatus.PASS,
      duration: 100,
      startedAt: '2026-08-01T00:00:00Z',
      endedAt: '2026-08-01T00:00:01Z',
      hash: 'b'.repeat(64),
    });
    expect(m.getSession().merkleRoot).toHaveLength(64);
  });

  test('同 nodeCode 覆盖(幂等)', () => {
    const m = makeSession();
    m.recordNodeResult({
      nodeCode: 'N001',
      result: NodeResultStatus.PASS,
      duration: 100,
      startedAt: '2026-08-01T00:00:00Z',
      endedAt: '2026-08-01T00:00:01Z',
      hash: 'a'.repeat(64),
    });
    m.recordNodeResult({
      nodeCode: 'N001',
      result: NodeResultStatus.FAIL,
      duration: 100,
      startedAt: '2026-08-01T00:00:00Z',
      endedAt: '2026-08-01T00:00:01Z',
      hash: 'b'.repeat(64),
    });
    const r = m.getSession().nodeResults;
    expect(r).toHaveLength(1);
    expect(r[0].result).toBe(NodeResultStatus.FAIL);
  });

  test('complete 标记完成', () => {
    const m = makeSession();
    m.complete();
    expect(m.getSession().completed).toBe(true);
    expect(m.getSession().endedAt).toBeDefined();
  });

  test('rewindTo 回滚', () => {
    const m = makeSession();
    m.recordNodeResult({
      nodeCode: 'N001', result: NodeResultStatus.PASS, duration: 0,
      startedAt: '2026-08-01T00:00:00Z', endedAt: '2026-08-01T00:00:00Z',
      hash: 'a'.repeat(64),
    });
    m.recordNodeResult({
      nodeCode: 'N002', result: NodeResultStatus.PASS, duration: 0,
      startedAt: '2026-08-01T00:00:00Z', endedAt: '2026-08-01T00:00:00Z',
      hash: 'b'.repeat(64),
    });
    m.rewindTo('N001');
    expect(m.getSession().currentNodeIndex).toBe(0);
    expect(m.getSession().nodeResults).toHaveLength(0);
  });

  test('rewindTo 节点不存在报错', () => {
    const m = makeSession();
    expect(() => m.rewindTo('XXX')).toThrow();
  });

  test('toJSON / resume 断点续传', () => {
    const m = makeSession();
    m.recordNodeResult({
      nodeCode: 'N001', result: NodeResultStatus.PASS, duration: 100,
      startedAt: '2026-08-01T00:00:00Z', endedAt: '2026-08-01T00:00:01Z',
      hash: 'a'.repeat(64),
    });
    const json = m.toJSON();

    const core = ScriptCore.fromObject(script);
    const m2 = ScriptSessionManager.resume(core, json);
    expect(m2.getSession().sessionId).toBe('SES-001');
    expect(m2.getSession().nodeResults).toHaveLength(1);
    expect(m2.getSession().currentNodeIndex).toBe(1);
  });

  test('toReportPayload 输出', () => {
    const m = makeSession();
    const payload = m.toReportPayload() as { sessionId: string; completed: boolean };
    expect(payload.sessionId).toBe('SES-001');
    expect(payload.completed).toBe(false);
  });
});

// 抑制 lint
void _SC;
