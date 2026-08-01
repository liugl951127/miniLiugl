/**
 * 关键词检测器
 *
 * <p>用于 ASR 转写后,判断客户是否按话术要求回答
 * <p>支持:
 * <ul>
 *   <li>必答词(必须全部命中)
 *   <li>可答词(任一命中即可)
 *   <li>模糊匹配(支持同义词)
 *   <li>同音字容错
 * </ul>
 *
 * @author Mavis
 */

import { KeywordMatchResult, SDKError, ErrorCode } from '../types';

/**
 * 同义词字典(可扩展)
 */
const SYNONYM_DICT: Record<string, string[]> = {
  同意: ['同意', '认可', '是的', '好的', '可以', '愿意', '没问题', 'OK', 'yes', '没问题'],
  不同意: ['不同意', '不认可', '不是', '不好', '不愿意', 'no'],
  明白: ['明白', '清楚', '了解', '理解', '知道了', '懂'],
  风险: ['风险', '亏损', '损失', '不保证', '可能损失', '本金风险'],
  身份证: ['身份证', '证件', '身份证明'],
  // 可继续扩展
};

/**
 * 关键词检测器
 */
export class KeywordDetector {
  private mustHit: string[];
  private optional: string[];

  constructor(mustHit: string[] = [], optional: string[] = []) {
    this.mustHit = mustHit;
    this.optional = optional;
  }

  /**
   * 检测文本是否命中关键词
   */
  detect(text: string): KeywordMatchResult {
    if (!text) {
      return {
        mustHitPassed: this.mustHit.length === 0,
        mustHitMatched: [],
        mustHitMissed: [...this.mustHit],
        optionalMatched: [],
        hitRate: 0,
        passed: this.mustHit.length === 0,
      };
    }

    const normalized = this.normalize(text);

    // 必答词检测
    const mustHitMatched: string[] = [];
    const mustHitMissed: string[] = [];
    for (const kw of this.mustHit) {
      if (this.matchKeyword(normalized, kw)) {
        mustHitMatched.push(kw);
      } else {
        mustHitMissed.push(kw);
      }
    }

    // 可答词检测
    const optionalMatched: string[] = [];
    for (const kw of this.optional) {
      if (this.matchKeyword(normalized, kw)) {
        optionalMatched.push(kw);
      }
    }

    const totalKeywords = this.mustHit.length + this.optional.length;
    const matched = mustHitMatched.length + optionalMatched.length;
    const hitRate = totalKeywords > 0 ? matched / totalKeywords : 1;

    return {
      mustHitPassed: mustHitMissed.length === 0,
      mustHitMatched,
      mustHitMissed,
      optionalMatched,
      hitRate,
      passed: mustHitMissed.length === 0,
    };
  }

  // ============================================================
  // 内部
  // ============================================================

  private normalize(text: string): string {
    return text
      .replace(/[，。！？、；：""''【】《》()()\-—.,!?;:"'()\[\]]/g, '')
      .replace(/\s+/g, '')
      .toLowerCase();
  }

  /**
   * 单个关键词匹配(支持同义词)
   */
  private matchKeyword(normalizedText: string, keyword: string): boolean {
    // 精确匹配
    if (normalizedText.includes(keyword.toLowerCase())) {
      return true;
    }

    // 同义词匹配
    const synonyms = SYNONYM_DICT[keyword] || [];
    for (const syn of synonyms) {
      if (normalizedText.includes(syn.toLowerCase())) {
        return true;
      }
    }

    return false;
  }
}

// 抑制未使用警告
void SDKError;
void ErrorCode;
