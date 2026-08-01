/**
 * 关键词检测器测试
 */
import { KeywordDetector } from '../src/core/keyword-detector';

describe('KeywordDetector', () => {
  test('无必答词 - 任何文本都通过', () => {
    const d = new KeywordDetector([], ['是', '好']);
    expect(d.detect('任意文本').passed).toBe(true);
  });

  test('必答词命中', () => {
    const d = new KeywordDetector(['是', '本人']);
    const r = d.detect('是的,我是本人');
    expect(r.passed).toBe(true);
    expect(r.mustHitMatched).toEqual(['是', '本人']);
    expect(r.mustHitMissed).toEqual([]);
  });

  test('必答词未命中', () => {
    const d = new KeywordDetector(['是', '本人']);
    const r = d.detect('不是');
    expect(r.passed).toBe(false);
    expect(r.mustHitMissed).toContain('本人');
  });

  test('部分命中', () => {
    const d = new KeywordDetector(['是', '本人', '身份证']);
    const r = d.detect('是的,本人');
    expect(r.passed).toBe(false);
    expect(r.mustHitMatched).toEqual(['是', '本人']);
    expect(r.mustHitMissed).toEqual(['身份证']);
  });

  test('同义词 - 同意', () => {
    const d = new KeywordDetector(['同意']);
    expect(d.detect('好的').passed).toBe(true);
    expect(d.detect('可以').passed).toBe(true);
    expect(d.detect('不行').passed).toBe(false);
  });

  test('空文本 - 必答词全部 miss', () => {
    const d = new KeywordDetector(['是']);
    const r = d.detect('');
    expect(r.passed).toBe(false);
    expect(r.mustHitMissed).toEqual(['是']);
  });

  test('hitRate 计算', () => {
    const d = new KeywordDetector(['A', 'B'], ['C']);
    // 命中 A、C,B miss
    const r = d.detect('A and C');
    expect(r.hitRate).toBeCloseTo(0.666, 2);
  });

  test('标点不影响匹配', () => {
    const d = new KeywordDetector(['是']);
    expect(d.detect('是的,!。').passed).toBe(true);
  });
});
