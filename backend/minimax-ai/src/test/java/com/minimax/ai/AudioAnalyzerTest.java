package com.minimax.ai;

import com.minimax.ai.multimodal.AudioAnalyzer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 音频分析器测试 (V2.6 多模态)
 */
class AudioAnalyzerTest {

    /**
     * 创建一个测试用 WAV (1 秒 440Hz 正弦波, 16kHz, 单声道)
     */
    private byte[] createTestWav() {
        AudioAnalyzer analyzer = new AudioAnalyzer();
        int sampleRate = 16000;
        int samples = sampleRate; // 1 秒
        byte[] pcm = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            double t = (double) i / sampleRate;
            short s = (short) (Math.sin(2 * Math.PI * 440 * t) * 8000);
            pcm[i * 2] = (byte) (s & 0xFF);
            pcm[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return analyzer.wrapAsWav(pcm, sampleRate, 1, 16);
    }

    @Test
    void testAnalyzeWav() {
        AudioAnalyzer analyzer = new AudioAnalyzer();
        byte[] wav = createTestWav();
        AudioAnalyzer.AudioAnalysisResult r = analyzer.analyze(wav, "test.wav");

        assertEquals("wav", r.format);
        assertEquals(16000, r.sampleRate);
        assertEquals(1, r.channels);
        assertTrue(r.durationMs >= 900 && r.durationMs <= 1100, "1秒音频: " + r.durationMs);
        assertEquals(16, r.bitsPerSample);
        assertEquals("PCM", r.codec);
    }

    @Test
    void testRmsForSine() {
        AudioAnalyzer analyzer = new AudioAnalyzer();
        byte[] wav = createTestWav();
        AudioAnalyzer.AudioAnalysisResult r = analyzer.analyze(wav, "test.wav");

        // 440Hz @ 8000/32768 振幅 -> RMS 约 8000/32768/sqrt(2) ≈ 0.17
        assertTrue(r.rms > 0.1 && r.rms < 0.3, "RMS 在合理范围: " + r.rms);
    }

    @Test
    void testSpectrum() {
        AudioAnalyzer analyzer = new AudioAnalyzer();
        byte[] wav = createTestWav();
        AudioAnalyzer.AudioAnalysisResult r = analyzer.analyze(wav, "test.wav");

        assertEquals(8, r.spectrum.length);
        for (double v : r.spectrum) {
            assertTrue(v >= 0, "频谱能量非负: " + v);
        }
    }

    @Test
    void testTranscribe() {
        AudioAnalyzer analyzer = new AudioAnalyzer();
        byte[] wav = createTestWav();
        String result = analyzer.transcribe(wav);
        assertNotNull(result);
        assertTrue(result.contains("STT"), "STT 占位");
    }

    @Test
    void testSynthesize() {
        AudioAnalyzer analyzer = new AudioAnalyzer();
        byte[] wav = analyzer.synthesize("Hello World", 16000);
        assertTrue(wav.length > 44, "WAV 文件至少有 44 字节头");
        // 检查 RIFF 头
        assertEquals('R', wav[0]);
        assertEquals('I', wav[1]);
        assertEquals('F', wav[2]);
        assertEquals('F', wav[3]);
    }

    @Test
    void testWrapAsWav() {
        AudioAnalyzer analyzer = new AudioAnalyzer();
        byte[] pcm = new byte[100 * 2];
        byte[] wav = analyzer.wrapAsWav(pcm, 16000, 1, 16);
        assertEquals(44 + 200, wav.length, "WAV 头 44 字节 + PCM 200 字节");
    }

    @Test
    void testIsSupported() {
        AudioAnalyzer analyzer = new AudioAnalyzer();
        assertTrue(analyzer.isSupported("a.wav"));
        assertTrue(analyzer.isSupported("a.pcm"));
        assertFalse(analyzer.isSupported("a.mp3"));
        assertFalse(analyzer.isSupported("a.exe"));
    }

    @Test
    void testRejectEmpty() {
        AudioAnalyzer analyzer = new AudioAnalyzer();
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyze(new byte[0], "a.wav"));
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyze(null, "a.wav"));
    }
}
