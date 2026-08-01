/**
 * TTS / ASR Mock Provider 测试
 */
import { MockTTSProvider } from '../src/tts-adapter/mock';
import { MockASRProvider } from '../src/asr-adapter/mock';

describe('MockTTSProvider', () => {
  test('synthesize 返回音频', async () => {
    const tts = new MockTTSProvider();
    const r = await tts.synthesize({ text: '你好' });
    expect(r.audio).toBeInstanceOf(Uint8Array);
    expect(r.audio.length).toBeGreaterThan(0);
    expect(r.audioHash).toHaveLength(64);
    expect(r.duration).toBeGreaterThan(0);
  });

  test('healthCheck 返回 true', async () => {
    const tts = new MockTTSProvider();
    expect(await tts.healthCheck()).toBe(true);
  });

  test('ID 与 Name 正确', () => {
    const tts = new MockTTSProvider();
    expect(tts.id).toBe('mock');
    expect(tts.name).toBe('Mock TTS');
  });
});

describe('MockASRProvider', () => {
  test('recognize 返回文本', async () => {
    const asr = new MockASRProvider('mock', 'Mock', { text: '测试文本' });
    const r = await asr.recognize({
      audio: new Uint8Array(16000),
      sampleRate: 16000,
      format: 'pcm',
    });
    expect(r.text).toBe('测试文本');
    expect(r.confidence).toBeGreaterThan(0);
    expect(r.audioHash).toHaveLength(64);
    expect(r.isFinal).toBe(true);
  });

  test('setMockText 修改返回', async () => {
    const asr = new MockASRProvider();
    asr.setMockText('新文本', 0.8);
    const r = await asr.recognize({
      audio: new Uint8Array(8000),
      sampleRate: 16000,
      format: 'pcm',
    });
    expect(r.text).toBe('新文本');
    expect(r.confidence).toBe(0.8);
  });
});
