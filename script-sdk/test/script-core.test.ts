/**
 * 话术核心单元测试
 */
import { ScriptCore } from '../src/core/script-core';
import { NodeType, ProductType, Channel } from '../src/types';

const sampleScript = {
  scriptId: 'TEST-1',
  version: '1.0.0',
  productType: ProductType.INSURANCE,
  name: '测试话术',
  createdAt: '2026-08-01T00:00:00Z',
  updatedAt: '2026-08-01T00:00:00Z',
  nodes: [
    {
      nodeCode: 'N001',
      nodeName: '开场',
      nodeType: NodeType.MANAGER_SPEAK,
      order: 1,
      text: '您好',
    },
    {
      nodeCode: 'N002',
      nodeName: '客户确认',
      nodeType: NodeType.CUSTOMER_RESPOND,
      order: 2,
      text: '您是本人吗?',
      mustHitKeywords: ['是', '本人'],
    },
    {
      nodeCode: 'N003',
      nodeName: '风险提示',
      nodeType: NodeType.RISK_DISCLOSURE,
      order: 3,
      text: '本产品有风险',
      condition: '${customer.age} >= 18',
    },
    {
      nodeCode: 'N004',
      nodeName: '仅 H5',
      nodeType: NodeType.MANAGER_SPEAK,
      order: 4,
      text: '仅 H5 显示',
      channels: [Channel.H5],
    },
  ],
};

describe('ScriptCore', () => {
  test('fromObject 构造', () => {
    const core = ScriptCore.fromObject(sampleScript);
    expect(core.getScriptId()).toBe('TEST-1');
    expect(core.getVersion()).toBe('1.0.0');
  });

  test('fromJSON 构造', () => {
    const json = JSON.stringify(sampleScript);
    const core = ScriptCore.fromJSON(json);
    expect(core.size()).toBe(4);
  });

  test('size 返回节点数', () => {
    const core = ScriptCore.fromObject(sampleScript);
    expect(core.size()).toBe(4);
  });

  test('getNodes 按 order 排序', () => {
    const core = ScriptCore.fromObject(sampleScript);
    const nodes = core.getNodes();
    expect(nodes[0].nodeCode).toBe('N001');
    expect(nodes[1].nodeCode).toBe('N002');
    expect(nodes[2].nodeCode).toBe('N003');
  });

  test('findNode 按 code 查找', () => {
    const core = ScriptCore.fromObject(sampleScript);
    const n = core.findNode('N002');
    expect(n?.nodeName).toBe('客户确认');
    expect(core.findNode('NOT_EXIST')).toBeUndefined();
  });

  test('节点编码重复报错', () => {
    const bad = {
      ...sampleScript,
      nodes: [
        ...sampleScript.nodes,
        { ...sampleScript.nodes[0] },
      ],
    };
    expect(() => ScriptCore.fromObject(bad)).toThrow();
  });

  test('节点编码缺失报错', () => {
    const bad = { ...sampleScript, scriptId: '' };
    expect(() => ScriptCore.fromObject(bad)).toThrow();
  });

  test('条件表达式 - 客户年龄', () => {
    const core = ScriptCore.fromObject(sampleScript);
    const n3 = core.findNode('N003')!;
    expect(core.shouldExecute(n3, { customer: { age: 25 } })).toBe(true);
    expect(core.shouldExecute(n3, { customer: { age: 10 } })).toBe(false);
  });

  test('条件表达式 - 字符串', () => {
    const customScript = {
      ...sampleScript,
      nodes: [
        {
          nodeCode: 'X001',
          nodeName: 'VIP',
          nodeType: NodeType.MANAGER_SPEAK,
          order: 1,
          text: 'VIP 专属',
          condition: '${customer.level} == "VIP"',
        },
      ],
    };
    const core = ScriptCore.fromObject(customScript);
    const n = core.findNode('X001')!;
    expect(core.shouldExecute(n, { customer: { level: 'VIP' } })).toBe(true);
    expect(core.shouldExecute(n, { customer: { level: 'NORMAL' } })).toBe(false);
  });

  test('无 condition 永远执行', () => {
    const core = ScriptCore.fromObject(sampleScript);
    const n1 = core.findNode('N001')!;
    expect(core.shouldExecute(n1, {})).toBe(true);
  });

  test('按渠道过滤', () => {
    const core = ScriptCore.fromObject(sampleScript);
    const h5Nodes = core.getNodesForChannel(Channel.H5);
    const padNodes = core.getNodesForChannel(Channel.PAD);
    // N004 仅 H5
    expect(h5Nodes.map((n) => n.nodeCode)).toContain('N004');
    expect(padNodes.map((n) => n.nodeCode)).not.toContain('N004');
  });

  test('computeHash 输出 64 字符', () => {
    const core = ScriptCore.fromObject(sampleScript);
    const hash = core.computeHash();
    expect(hash).toHaveLength(64);
  });

  test('版本比较 - 大于', () => {
    expect(ScriptCore.compareVersion('2.0.0', '1.0.0')).toBe(1);
    expect(ScriptCore.compareVersion('1.1.0', '1.0.0')).toBe(1);
    expect(ScriptCore.compareVersion('1.0.1', '1.0.0')).toBe(1);
  });

  test('版本比较 - 小于', () => {
    expect(ScriptCore.compareVersion('1.0.0', '2.0.0')).toBe(-1);
  });

  test('版本比较 - 相等', () => {
    expect(ScriptCore.compareVersion('1.0.0', '1.0.0')).toBe(0);
  });

  test('Merkle 根 - 单元素', () => {
    const h = 'a'.repeat(64);
    expect(ScriptCore.computeMerkleRoot([h])).toBe(h);
  });

  test('Merkle 根 - 空列表', () => {
    expect(ScriptCore.computeMerkleRoot([])).toBe('');
  });

  test('Merkle 根 - 确定性', () => {
    const hashes = ['a'.repeat(64), 'b'.repeat(64), 'c'.repeat(64)];
    const r1 = ScriptCore.computeMerkleRoot(hashes);
    const r2 = ScriptCore.computeMerkleRoot(hashes);
    expect(r1).toBe(r2);
  });

  test('Merkle 根 - 奇数项', () => {
    const hashes = ['a'.repeat(64), 'b'.repeat(64), 'c'.repeat(64)];
    const root = ScriptCore.computeMerkleRoot(hashes);
    expect(root).toHaveLength(64);
  });

  test('isCompatible 缺省时返回 true', () => {
    const core = ScriptCore.fromObject(sampleScript);
    expect(core.isCompatible()).toBe(true);
  });
});
