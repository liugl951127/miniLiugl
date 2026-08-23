package com.minimax.ai.multimodal.media;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * FFmpeg 进程包装 (V7.3 视频处理)
 *
 * <p>零 Java 依赖的媒体处理 - 通过子进程调用系统 ffmpeg.</p>
 *
 * <h3>功能</h3>
 * <ul>
 *   <li>probeMedia - 探测时长/分辨率/帧率/音轨 (ffprobe)</li>
 *   <li>extractFrames - 抽帧 (JPEGs to dir)</li>
 *   <li>extractAudio - 提取音轨 (16kHz mono WAV)</li>
 * </ul>
 */
@Slf4j
public class FFmpegRunner {

    public static boolean isAvailable() {
        try {
            Process p = new ProcessBuilder("ffmpeg", "-version").redirectErrorStream(true).start();
            return p.waitFor(2, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static MediaInfo probe(String filePath) {
        try {
            Process p = new ProcessBuilder("ffprobe", "-v", "quiet",
                "-print_format", "json", "-show_format", "-show_streams", filePath)
                .redirectErrorStream(true).start();
            StringBuilder out = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) out.append(line);
            }
            boolean ok = p.waitFor(10, TimeUnit.SECONDS);
            if (!ok) p.destroyForcibly();
            return parseProbeJson(out.toString());
        } catch (Exception e) {
            log.warn("[FFmpegRunner] probe 失败: {}", e.getMessage());
            return MediaInfo.empty();
        }
    }

    private static MediaInfo parseProbeJson(String json) {
        // 简化: 不引入 JSON 库, 用正则提取关键字段
        MediaInfo info = new MediaInfo();
        info.durationSec = extractFloat(json, "\"duration\"\\s*:\\s*\"?([0-9.]+)\"?", 0);
        info.bitrate = extractLong(json, "\"bit_rate\"\\s*:\\s*\"?([0-9]+)\"?", 0);
        if (json.contains("\"codec_type\":\"video\"") || json.contains("\"codec_type\": \"video\"")) {
            info.hasVideo = true;
            info.width = extractInt(json, "\"width\"\\s*:\\s*([0-9]+)", 0);
            info.height = extractInt(json, "\"height\"\\s*:\\s*([0-9]+)", 0);
            String fps = extractStr(json, "\"r_frame_rate\"\\s*:\\s*\"([0-9/]+)\"");
            if (fps != null && fps.contains("/")) {
                String[] parts = fps.split("/");
                try {
                    info.fps = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
                } catch (Exception ignored) {}
            }
        }
        if (json.contains("\"codec_type\":\"audio\"") || json.contains("\"codec_type\": \"audio\"")) {
            info.hasAudio = true;
            info.audioSampleRate = extractInt(json, "\"sample_rate\"\\s*:\\s*\"?([0-9]+)\"?", 0);
        }
        return info;
    }

    private static float extractFloat(String s, String regex, float def) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(s);
            if (m.find()) return Float.parseFloat(m.group(1));
        } catch (Exception ignored) {}
        return def;
    }

    private static int extractInt(String s, String regex, int def) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(s);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Exception ignored) {}
        return def;
    }

    private static long extractLong(String s, String regex, long def) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(s);
            if (m.find()) return Long.parseLong(m.group(1));
        } catch (Exception ignored) {}
        return def;
    }

    private static String extractStr(String s, String regex) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(s);
            if (m.find()) return m.group(1);
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 抽帧: 从视频中按 fps 抽帧到 outDir
     *
     * @return 实际抽帧数
     */
    public static int extractFrames(String inputPath, File outDir, double fps) {
        if (!isAvailable()) return 0;
        outDir.mkdirs();
        // fps 抽帧, scale 224 宽 (后续 ResNet50 预处理会 resize)
        String[] cmd = {"ffmpeg", "-y", "-i", inputPath,
            "-vf", String.format("fps=%s,scale=224:-1", fps),
            "-q:v", "2", outDir.getAbsolutePath() + "/frame_%05d.jpg"};
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            // 消费输出, 避免阻塞
            try (var in = p.getInputStream()) { while (in.read() != -1) {} }
            boolean ok = p.waitFor(120, TimeUnit.SECONDS);
            if (!ok) { p.destroyForcibly(); return 0; }
            // 数 jpg
            File[] files = outDir.listFiles((d, n) -> n.endsWith(".jpg"));
            return files == null ? 0 : files.length;
        } catch (Exception e) {
            log.error("[FFmpegRunner] 抽帧失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 提取音轨为 16kHz mono WAV
     *
     * @return 输出的 WAV 文件路径, 失败返回 null
     */
    public static File extractAudio(String inputPath, File outFile) {
        if (!isAvailable()) return null;
        outFile.getParentFile().mkdirs();
        String[] cmd = {"ffmpeg", "-y", "-i", inputPath,
            "-vn", "-acodec", "pcm_s16le", "-ac", "1", "-ar", "16000",
            outFile.getAbsolutePath()};
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            try (var in = p.getInputStream()) { while (in.read() != -1) {} }
            boolean ok = p.waitFor(60, TimeUnit.SECONDS);
            if (!ok || !outFile.exists() || outFile.length() == 0) {
                p.destroyForcibly();
                return null;
            }
            return outFile;
        } catch (Exception e) {
            log.error("[FFmpegRunner] 抽音失败: {}", e.getMessage());
            return null;
        }
    }

    public static class MediaInfo {
        public float durationSec;
        public long bitrate;
        public boolean hasVideo, hasAudio;
        public int width, height, audioSampleRate;
        public double fps;

        public static MediaInfo empty() { return new MediaInfo(); }

        public String summary() {
            return String.format("%.1fs %dx%d@%.1ffps %s%s",
                durationSec, width, height, fps,
                hasVideo ? "V" : "-", hasAudio ? "A" : "-");
        }
    }
}
