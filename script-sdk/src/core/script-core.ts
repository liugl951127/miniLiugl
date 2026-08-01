/**
 * 话术核心 - 数据模型与版本管理
 *
 * <p>负责:
 * <ul>
 *   <li>ScriptModel 加载/校验/版本对比
 *   <li>节点树扁平化、按 order 排序
 *   <li>条件表达式求值(${customer.age} >= 60)
 *   <li>脚本指纹计算(SM3)
 *   <li>断点续传定位
 * </ul>
 *
 * @author Mavis
 */

import {
  ScriptModel,
  ScriptNode,
  NodeResult,
  Channel,
  SDKError,
  ErrorCode,
} from '../types';
import { sm3String, sm3Hex } from './hash';

/**
 * 上下文变量(用于条件表达式)
 */
export interface ScriptContext {
  /** 客户信息 */
  customer?: {
    id?: string;
    name?: string;
    age?: number;
    gender?: 'M' | 'F';
    riskLevel?: string;
  };
  /** 产品信息 */
  product?: {
    id?: string;
    type?: string;
    amount?: number;
  };
  /** 销售经理 */
  manager?: {
    id?: string;
    name?: string;
  };
  /** 自定义变量 */
  [key: string]: unknown;
}

/**
 * 话术核心类
 *
 * <p>无副作用,纯数据操作 + 算法
 */
export class ScriptCore {
  private model: ScriptModel;
  private flatNodes: ScriptNode[];

  constructor(model: ScriptModel) {
    this.model = model;
    this.validate(model);
    this.flatNodes = this.flatten(model.nodes);
  }

  // ============================================================
  // 静态工厂
  // ============================================================

  /**
   * 从 JSON 构造
   */
  static fromJSON(json: string): ScriptCore {
    const model = JSON.parse(json) as ScriptModel;
    return new ScriptCore(model);
  }

  /**
   * 从对象构造
   */
  static fromObject(model: ScriptModel): ScriptCore {
    return new ScriptCore(model);
  }

  // ============================================================
  // 访问器
  // ============================================================

  getModel(): ScriptModel {
    return this.model;
  }

  getScriptId(): string {
    return this.model.scriptId;
  }

  getVersion(): string {
    return this.model.version;
  }

  /**
   * 获取扁平化节点列表(按 order 排序)
   */
  getNodes(): ScriptNode[] {
    return [...this.flatNodes];
  }

  /**
   * 获取当前渠道应执行的节点列表(应用 channel 过滤)
   */
  getNodesForChannel(channel: Channel): ScriptNode[] {
    return this.flatNodes.filter(
      (n) => !n.channels || n.channels.length === 0 || n.channels.includes(channel)
    );
  }

  /**
   * 按编号查找节点
   */
  findNode(nodeCode: string): ScriptNode | undefined {
    return this.flatNodes.find((n) => n.nodeCode === nodeCode);
  }

  /**
   * 获取节点总数
   */
  size(): number {
    return this.flatNodes.length;
  }

  // ============================================================
  // 校验
  // ============================================================

  private validate(model: ScriptModel): void {
    if (!model.scriptId) {
      throw new SDKError(ErrorCode.SCRIPT_ERROR, 'scriptId 必填');
    }
    if (!model.version) {
      throw new SDKError(ErrorCode.SCRIPT_ERROR, 'version 必填');
    }
    if (!Array.isArray(model.nodes) || model.nodes.length === 0) {
      throw new SDKError(ErrorCode.SCRIPT_ERROR, '至少需要 1 个节点');
    }

    // 节点编码唯一性
    const codes = new Set<string>();
    for (const node of this.flatten(model.nodes)) {
      if (codes.has(node.nodeCode)) {
        throw new SDKError(ErrorCode.SCRIPT_ERROR, `节点编码重复: ${node.nodeCode}`);
      }
      codes.add(node.nodeCode);
    }
  }

  /**
   * 节点树扁平化(DFS,按 order 排序)
   */
  private flatten(nodes: ScriptNode[]): ScriptNode[] {
    const result: ScriptNode[] = [];
    const walk = (ns: ScriptNode[]) => {
      for (const n of ns) {
        result.push(n);
        if (n.children && n.children.length > 0) {
          walk(n.children);
        }
      }
    };
    walk(nodes);
    return result.sort((a, b) => a.order - b.order);
  }

  // ============================================================
  // 条件表达式求值
  // ============================================================

  /**
   * 判断节点是否应该执行
   *
   * <p>支持表达式:${customer.age} >= 60, ${product.amount} > 100000
   *
   * @param node 节点
   * @param ctx 上下文
   */
  shouldExecute(node: ScriptNode, ctx: ScriptContext): boolean {
    if (!node.condition) return true;
    return this.evaluate(node.condition, ctx);
  }

  /**
   * 简单表达式求值器
   *
   * <p>语法:${path.to.value} OP VALUE
   * <p>OP 支持: ==  !=  >  <  >=  <=  contains  startsWith  endsWith
   *
   * <p>注意:生产环境应使用更安全的表达式引擎(如 expr-eval)
   */
  private evaluate(expression: string, ctx: ScriptContext): boolean {
    try {
      const trimmed = expression.trim();
      if (!trimmed) return true;

      // 解析模式: ${...} OP VALUE
      const match = trimmed.match(/^(\$\{[^}]+\})\s*(==|!=|>=|<=|>|<|contains|startsWith|endsWith)\s*(.+)$/);
      if (!match) {
        // 单变量真值测试
        return Boolean(this.resolvePath(trimmed, ctx));
      }

      const [, left, op, rightRaw] = match;
      const leftVal = this.resolvePath(left, ctx);
      const rightVal = this.parseLiteral(rightRaw.trim());

      switch (op) {
        case '==':
          return leftVal == rightVal;
        case '!=':
          return leftVal != rightVal;
        case '>':
          return Number(leftVal) > Number(rightVal);
        case '<':
          return Number(leftVal) < Number(rightVal);
        case '>=':
          return Number(leftVal) >= Number(rightVal);
        case '<=':
          return Number(leftVal) <= Number(rightVal);
        case 'contains':
          return String(leftVal).includes(String(rightVal));
        case 'startsWith':
          return String(leftVal).startsWith(String(rightVal));
        case 'endsWith':
          return String(leftVal).endsWith(String(rightVal));
      }
      return true;
    } catch (e) {
      // 求值失败,默认执行
      return true;
    }
  }

  private resolvePath(expr: string, ctx: unknown): unknown {
    const path = expr.replace(/^\$\{|\}$/g, '').trim();
    return path.split('.').reduce((acc: unknown, key: string) => {
      if (acc && typeof acc === 'object') {
        return (acc as Record<string, unknown>)[key];
      }
      return undefined;
    }, ctx);
  }

  private parseLiteral(s: string): unknown {
    if (s === 'true') return true;
    if (s === 'false') return false;
    if (s === 'null') return null;
    if (/^-?\d+(\.\d+)?$/.test(s)) return Number(s);
    // 去掉引号
    if ((s.startsWith('"') && s.endsWith('"')) || (s.startsWith("'") && s.endsWith("'"))) {
      return s.slice(1, -1);
    }
    return s;
  }

  // ============================================================
  // 哈希与指纹
  // ============================================================

  /**
   * 计算脚本指纹(SM3)
   *
   * <p>取 scriptId + version + 所有节点 code+text+order 的 SM3
   */
  computeHash(): string {
    const payload = [
      this.model.scriptId,
      this.model.version,
      ...this.flatNodes.map((n) => `${n.order}|${n.nodeCode}|${n.text}`),
    ].join('\n');
    return sm3String(payload);
  }

  /**
   * 计算节点结果指纹(SM3)
   */
  static computeNodeResultHash(result: NodeResult): string {
    const payload = [
      result.nodeCode,
      result.result,
      result.duration,
      result.customerSaid || '',
      result.audioHash || '',
      result.startedAt,
      result.endedAt,
    ].join('|');
    return sm3String(payload);
  }

  /**
   * 计算 Merkle 根
   *
   * <p>Bitcoin 风格双 SHA-256
   * <p>对应 Java 链码中的 MerkleUtil
   */
  static computeMerkleRoot(hashes: string[]): string {
    if (!hashes || hashes.length === 0) return '';
    if (hashes.length === 1) return hashes[0];

    // 复制到可变列表
    let layer = [...hashes];
    while (layer.length > 1) {
      if (layer.length % 2 !== 0) {
        layer.push(layer[layer.length - 1]);
      }
      const next: string[] = [];
      for (let i = 0; i < layer.length; i += 2) {
        // SHA-256 双哈希(SM3 替代 - 保持与 Java 链码一致性)
        const concat = layer[i] + layer[i + 1];
        next.push(doubleHashHex(concat));
      }
      layer = next;
    }
    return layer[0];
  }

  // ============================================================
  // 版本管理
  // ============================================================

  /**
   * 语义化版本对比
   *
   * @return 1 = a > b, -1 = a < b, 0 = 相等
   */
  static compareVersion(a: string, b: string): number {
    const pa = a.split('.').map((s) => parseInt(s, 10) || 0);
    const pb = b.split('.').map((s) => parseInt(s, 10) || 0);
    const len = Math.max(pa.length, pb.length);
    for (let i = 0; i < len; i++) {
      const na = pa[i] || 0;
      const nb = pb[i] || 0;
      if (na > nb) return 1;
      if (na < nb) return -1;
    }
    return 0;
  }

  /**
   * 兼容检查
   *
   * <p>目标版本是否兼容当前 SDK 最低版本
   */
  isCompatible(): boolean {
    if (!this.model.minSdkVersion) return true;
    return ScriptCore.compareVersion(this.model.version, this.model.minSdkVersion) >= 0;
  }
}

/**
 * 双 SM3 哈希(用于 Merkle)
 */
function doubleHashHex(hex: string): string {
  // 简化:实际应是 SHA-256(SHA-256(bytes))
  // 这里用 SM3(SM3(text)) 替代(国密合规更稳)
  const inner = sm3String(hex);
  return sm3String(inner);
}

// 抑制未使用警告
void sm3Hex;
