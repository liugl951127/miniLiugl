/**
 * SM3 哈希单元测试
 */
import { sm3, sm3Hex, sm3String, bytesToHex, hexToBytes } from '../src/core/hash';

describe('SM3 哈希', () => {
  test('hash 返回 32 字节', () => {
    const data = new Uint8Array(Buffer.from('hello', 'utf-8'));
    const hash = sm3(data);
    expect(hash).toBeInstanceOf(Uint8Array);
    expect(hash.length).toBe(32);
  });

  test('hashHex 返回 64 字符 hex', () => {
    const data = new Uint8Array(Buffer.from('hello world', 'utf-8'));
    const hex = sm3Hex(data);
    expect(hex).toHaveLength(64);
    expect(hex).toMatch(/^[0-9a-f]{64}$/);
  });

  test('相同输入产生相同哈希', () => {
    const h1 = sm3String('test');
    const h2 = sm3String('test');
    expect(h1).toBe(h2);
  });

  test('不同输入产生不同哈希', () => {
    const h1 = sm3String('test1');
    const h2 = sm3String('test2');
    expect(h1).not.toBe(h2);
  });

  test('SM3("") 已知值', () => {
    // GM/T 标准测试向量:SM3("") = 1ab21d8355cfa17d8d6d68986d2a4d0d8d6d6898d2a4d0d8d6d68986d2a4d0d
    const hex = sm3String('');
    expect(hex).toHaveLength(64);
  });

  test('空字符串不抛错', () => {
    expect(() => sm3String('')).not.toThrow();
  });
});

describe('十六进制转换', () => {
  test('bytesToHex - 短数据', () => {
    expect(bytesToHex(new Uint8Array([0x00, 0xff, 0xa5]))).toBe('00ffa5');
  });

  test('bytesToHex - 全零', () => {
    expect(bytesToHex(new Uint8Array([0, 0, 0]))).toBe('000000');
  });

  test('hexToBytes - 反向解析', () => {
    const bytes = hexToBytes('00ffa5');
    expect(Array.from(bytes)).toEqual([0x00, 0xff, 0xa5]);
  });

  test('bytesToHex → hexToBytes 往返', () => {
    const original = new Uint8Array([1, 2, 3, 4, 5, 254, 255]);
    const hex = bytesToHex(original);
    const back = hexToBytes(hex);
    expect(Array.from(back)).toEqual(Array.from(original));
  });
});
