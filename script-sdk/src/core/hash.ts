/**
 * SM3 哈希 - 浏览器/Node 通用实现
 *
 * <p>纯 JS 实现的国密 SM3 摘要算法(GM/T 0004-2012)
 * <p>不依赖任何外部库,可独立运行
 * <p>对应 Java 链码中的 SM3Util.hashHex
 */

/**
 * SM3 上下文
 */
class SM3Context {
  private state: Uint32Array;
  private buffer: Uint8Array;
  private bufferLen: number = 0;
  private totalLen: number = 0;

  // 初始值 IV
  private static readonly IV = new Uint32Array([
    0x7380166f, 0x4914b2b9, 0x172442d7, 0xda8a0600,
    0xa96f30bc, 0x163138aa, 0xe38dee4d, 0xb0fb0e4e,
  ]);

  constructor() {
    this.state = new Uint32Array(SM3Context.IV);
    this.buffer = new Uint8Array(64);
  }

  update(data: Uint8Array): void {
    let dataPos = 0;
    this.totalLen += data.length;

    if (this.bufferLen > 0) {
      const need = 64 - this.bufferLen;
      const take = Math.min(need, data.length);
      this.buffer.set(data.subarray(0, take), this.bufferLen);
      this.bufferLen += take;
      dataPos += take;

      if (this.bufferLen === 64) {
        this.compress(this.buffer, 0);
        this.bufferLen = 0;
      }
    }

    while (dataPos + 64 <= data.length) {
      this.compress(data, dataPos);
      dataPos += 64;
    }

    if (dataPos < data.length) {
      this.bufferLen = data.length - dataPos;
      this.buffer.set(data.subarray(dataPos), 0);
    }
  }

  finalize(): Uint8Array {
    const pad = new Uint8Array(64);
    pad[0] = 0x80;
    let padLen = (56 - (this.bufferLen + 1) + 64) % 64;
    if (this.bufferLen + 1 + padLen + 8 > 64) {
      padLen += 64 - ((this.bufferLen + 1 + padLen) % 64);
    }

    const finalBuffer = new Uint8Array(this.bufferLen + 1 + padLen + 8);
    finalBuffer.set(this.buffer.subarray(0, this.bufferLen), 0);
    finalBuffer[this.bufferLen] = 0x80;
    if (padLen > 0) {
      // 填充 0
    }

    // 长度(大端 64 位)
    const bitLen = this.totalLen * 8;
    for (let i = 0; i < 8; i++) {
      finalBuffer[finalBuffer.length - 1 - i] = (bitLen >>> (8 * i)) & 0xff;
    }

    for (let i = 0; i < finalBuffer.length; i += 64) {
      this.compress(finalBuffer, i);
    }

    const out = new Uint8Array(32);
    for (let i = 0; i < 8; i++) {
      out[i * 4] = (this.state[i] >>> 24) & 0xff;
      out[i * 4 + 1] = (this.state[i] >>> 16) & 0xff;
      out[i * 4 + 2] = (this.state[i] >>> 8) & 0xff;
      out[i * 4 + 3] = this.state[i] & 0xff;
    }
    return out;
  }

  private compress(block: Uint8Array, offset: number): void {
    const W = new Uint32Array(68);
    const W1 = new Uint32Array(64);

    for (let i = 0; i < 16; i++) {
      W[i] =
        (block[offset + i * 4] << 24) |
        (block[offset + i * 4 + 1] << 16) |
        (block[offset + i * 4 + 2] << 8) |
        block[offset + i * 4 + 3];
    }

    for (let i = 16; i < 68; i++) {
      W[i] = SM3Context.P1(W[i - 16] ^ W[i - 9] ^ SM3Context.rotL(W[i - 3], 15)) ^
        SM3Context.rotL(W[i - 13], 7) ^
        W[i - 6];
    }

    for (let i = 0; i < 64; i++) {
      W1[i] = W[i] ^ W[i + 4];
    }

    let A = this.state[0];
    let B = this.state[1];
    let C = this.state[2];
    let D = this.state[3];
    let E = this.state[4];
    let F = this.state[5];
    let G = this.state[6];
    let H = this.state[7];

    for (let j = 0; j < 64; j++) {
      const T = j < 16 ? 0x79cc4519 : 0x7a879d8a;
      const SS1 = SM3Context.rotL(
        SM3Context.rotL(A, 12) + E + SM3Context.rotL(T, j % 32),
        7
      );
      const SS2 = SS1 ^ SM3Context.rotL(A, 12);
      const TT1 =
        (SM3Context.FF(A, B, C, j) + D + SS2 + W1[j]) | 0;
      const TT2 =
        (SM3Context.GG(E, F, G, j) + H + SS1 + W[j]) | 0;
      D = C;
      C = SM3Context.rotL(B, 9);
      B = A;
      A = TT1;
      H = G;
      G = SM3Context.rotL(F, 19);
      F = E;
      E = SM3Context.P0(TT2);
    }

    this.state[0] ^= A;
    this.state[1] ^= B;
    this.state[2] ^= C;
    this.state[3] ^= D;
    this.state[4] ^= E;
    this.state[5] ^= F;
    this.state[6] ^= G;
    this.state[7] ^= H;
  }

  private static rotL(x: number, n: number): number {
    return ((x << n) | (x >>> (32 - n))) | 0;
  }

  private static P0(x: number): number {
    return x ^ SM3Context.rotL(x, 9) ^ SM3Context.rotL(x, 17);
  }

  private static P1(x: number): number {
    return x ^ SM3Context.rotL(x, 15) ^ SM3Context.rotL(x, 23);
  }

  private static FF(x: number, y: number, z: number, j: number): number {
    if (j < 16) return x ^ y ^ z;
    return (x & y) | (x & z) | (y & z);
  }

  private static GG(x: number, y: number, z: number, j: number): number {
    if (j < 16) return x ^ y ^ z;
    return (x & y) | (~x & z);
  }
}

/**
 * 计算 SM3 摘要
 */
export function sm3(data: Uint8Array): Uint8Array {
  const ctx = new SM3Context();
  ctx.update(data);
  return ctx.finalize();
}

/**
 * SM3 摘要 - 十六进制输出
 */
export function sm3Hex(data: Uint8Array): string {
  const hash = sm3(data);
  return bytesToHex(hash);
}

/**
 * SM3 摘要 - 字符串输入
 */
export function sm3String(text: string): string {
  const encoder = typeof TextEncoder !== 'undefined' ? new TextEncoder() : null;
  const data = encoder ? encoder.encode(text) : new Uint8Array(Buffer.from(text, 'utf-8'));
  return sm3Hex(data);
}

/**
 * 字节数组转十六进制
 */
export function bytesToHex(bytes: Uint8Array): string {
  let hex = '';
  for (let i = 0; i < bytes.length; i++) {
    hex += (bytes[i] >>> 4).toString(16);
    hex += (bytes[i] & 0x0f).toString(16);
  }
  return hex;
}

/**
 * 十六进制转字节数组
 */
export function hexToBytes(hex: string): Uint8Array {
  const len = hex.length / 2;
  const out = new Uint8Array(len);
  for (let i = 0; i < len; i++) {
    out[i] = parseInt(hex.substr(i * 2, 2), 16);
  }
  return out;
}
