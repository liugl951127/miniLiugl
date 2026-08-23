package com.minimax.ai.multimodal.audio;

import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

/**
 * WAV/PCM 音频读取 + 重采样到 16kHz 单声道 float (V7.2)
 *
 * <p>使用 JDK 内置 javax.sound.sampled, 零依赖. 支持:</p>
 * <ul>
 *   <li>WAV/PCM (8/16/24/32 bit)</li>
 *   <li>自动重采样到 16kHz</li>
 *   <li>自动转单声道</li>
 *   <li>归一化到 [-1, 1] float</li>
 * </ul>
 *
 * <p>不支持 MP3/AAC (V1), 需用 ffmpeg 预转码.</p>
 */
@Slf4j
public class WavReader {

    public static float[] readAsMonoFloat16k(File f) {
        try {
            AudioInputStream raw = AudioSystem.getAudioInputStream(f);
            return convertToMono16k(raw);
        } catch (Exception e) {
            log.error("[WavReader] 读取失败: {} - {}", f.getName(), e.getMessage());
            return new float[0];
        }
    }

    public static float[] readAsMonoFloat16k(byte[] bytes) {
        try {
            AudioInputStream raw = AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes));
            return convertToMono16k(raw);
        } catch (UnsupportedAudioFileException | IOException e) {
            log.error("[WavReader] 解析失败: {}", e.getMessage());
            return new float[0];
        }
    }

    private static float[] convertToMono16k(AudioInputStream raw) throws IOException {
        AudioFormat src = raw.getFormat();
        AudioFormat target = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            16000, 16, 1, 2, 16000, false);

        AudioInputStream converted = AudioSystem.isConversionSupported(target, src)
            ? AudioSystem.getAudioInputStream(target, raw)
            : raw;

        // 读取全部
        byte[] data = converted.readAllBytes();
        converted.close();
        raw.close();
        return pcm16BytesToFloat(data);
    }

    private static float[] pcm16BytesToFloat(byte[] data) {
        int n = data.length / 2;
        float[] out = new float[n];
        for (int i = 0; i < n; i++) {
            int lo = data[i * 2] & 0xFF;
            int hi = data[i * 2 + 1];  // signed
            short s = (short) ((hi << 8) | lo);
            out[i] = s / 32768f;
        }
        return out;
    }
}
