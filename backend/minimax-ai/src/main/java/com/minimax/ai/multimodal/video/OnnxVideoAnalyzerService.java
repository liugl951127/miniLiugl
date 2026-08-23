package com.minimax.ai.multimodal.video;

import com.minimax.ai.multimodal.audio.OnnxWhisperService;
import com.minimax.ai.multimodal.audio.WavReader;
import com.minimax.ai.multimodal.media.FFmpegRunner;
import com.minimax.ai.multimodal.onnx.OnnxResNet50Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

/**
 * ONNX 视频智能分析 (V7.3)
 *
 * <p>复用现有模型: 关键帧 → ResNet50 分类, 音轨 → Whisper 转写, 可选 VAD.</p>
 *
 * <h3>分析流程</h3>
 * <ol>
 *   <li>ffprobe 探测媒体信息 (时长/分辨率/帧率/音轨)</li>
 *   <li>ffmpeg 按 1fps 抽帧 → ResNet50 分类 (取每帧 top-1)</li>
 *   <li>ffmpeg 提取音轨 16kHz mono WAV → Whisper 转写</li>
 *   <li>汇总: 时间轴 + 关键分类 + 完整文本</li>
 * </ol>
 *
 * <h3>依赖</h3>
 * <ul>
 *   <li>ResNet50 ONNX (图片分类)</li>
 *   <li>Whisper-tiny ONNX (语音转写)</li>
 *   <li>系统 ffmpeg/ffprobe (零 Java 依赖)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnnxVideoAnalyzerService {

    private final OnnxResNet50Service resnet;
    private final OnnxWhisperService whisper;

    @Value("${minimax.onnx-vision.video-fps:1.0}")
    private double frameFps;

    @Value("${minimax.onnx-vision.video-max-frames:30}")
    private int maxFrames;

    @Value("${minimax.onnx-vision.video-tmp-dir:/tmp/minimax-video}")
    private String tmpDir;

    public boolean isAvailable() {
        return FFmpegRunner.isAvailable() && (resnet.isReady() || whisper.isReady());
    }

    public String getTmpDir() { return tmpDir; }

    /**
     * 完整分析
     *
     * @param videoBytes 视频文件二进制
     * @return 视频分析结果
     */
    public VideoAnalysisResult analyze(byte[] videoBytes) {
        if (!FFmpegRunner.isAvailable()) {
            return VideoAnalysisResult.error("系统 ffmpeg 不可用");
        }
        if (!resnet.isReady() && !whisper.isReady()) {
            return VideoAnalysisResult.error("ResNet50 + Whisper 模型都未就绪, 至少需要 1 个");
        }
        long start = System.currentTimeMillis();
        File workDir = new File(tmpDir, "vid-" + System.currentTimeMillis());
        workDir.mkdirs();
        try {
            // 1. 写入临时文件
            File videoFile = new File(workDir, "input.mp4");
            try (FileOutputStream fos = new FileOutputStream(videoFile)) {
                fos.write(videoBytes);
            }
            // 2. 探测
            FFmpegRunner.MediaInfo info = FFmpegRunner.probe(videoFile.getAbsolutePath());
            log.info("[VideoAnalyzer] 媒体: {}", info.summary());
            if (!info.hasVideo && !info.hasAudio) {
                return VideoAnalysisResult.error("无法识别视频/音频流");
            }
            // 3. 抽帧 + 分类
            List<FrameAnalysis> frames = new ArrayList<>();
            if (info.hasVideo && resnet.isReady()) {
                frames = analyzeFrames(videoFile, info);
            }
            // 4. 抽音 + 转写
            String transcript = "";
            long asrCostMs = 0;
            if (info.hasAudio && whisper.isReady()) {
                long t0 = System.currentTimeMillis();
                File wavFile = FFmpegRunner.extractAudio(videoFile.getAbsolutePath(),
                    new File(workDir, "audio.wav"));
                if (wavFile != null && wavFile.length() > 0) {
                    float[] pcm = WavReader.readAsMonoFloat16k(wavFile);
                    if (pcm.length > 0) {
                        // 简化: 假设中文
                        OnnxWhisperService.TranscribeResult r = whisper.transcribe(pcm, "zh");
                        transcript = r.text();
                        asrCostMs = r.costMs();
                    }
                }
            }
            long cost = System.currentTimeMillis() - start;
            return new VideoAnalysisResult(true, null, info, frames, transcript, asrCostMs, cost);
        } catch (Exception e) {
            log.error("[VideoAnalyzer] 分析失败", e);
            return VideoAnalysisResult.error("分析失败: " + e.getMessage());
        } finally {
            // 清理临时目录
            try { deleteRecursively(workDir); } catch (Exception ignored) {}
        }
    }

    /**
     * 抽帧 + ResNet50 分类
     */
    private List<FrameAnalysis> analyzeFrames(File videoFile, FFmpegRunner.MediaInfo info) {
        File framesDir = new File(videoFile.getParentFile(), "frames");
        int extracted = FFmpegRunner.extractFrames(videoFile.getAbsolutePath(), framesDir, frameFps);
        if (extracted == 0) {
            log.warn("[VideoAnalyzer] 抽帧失败");
            return Collections.emptyList();
        }
        // 限帧
        File[] files = framesDir.listFiles((d, n) -> n.endsWith(".jpg"));
        if (files == null) return Collections.emptyList();
        Arrays.sort(files, Comparator.comparing(File::getName));
        if (files.length > maxFrames) {
            // 均匀采样
            int step = files.length / maxFrames;
            List<File> sampled = new ArrayList<>();
            for (int i = 0; i < files.length && sampled.size() < maxFrames; i += step) {
                sampled.add(files[i]);
            }
            files = sampled.toArray(new File[0]);
        }
        List<FrameAnalysis> result = new ArrayList<>(files.length);
        double intervalSec = 1.0 / frameFps;
        for (int i = 0; i < files.length; i++) {
            try {
                BufferedImage img = ImageIO.read(files[i]);
                if (img == null) continue;
                List<OnnxResNet50Service.ClassificationResult> cls = resnet.classify(img, 3);
                if (!cls.isEmpty()) {
                    OnnxResNet50Service.ClassificationResult top = cls.get(0);
                    result.add(new FrameAnalysis(
                        i * intervalSec,
                        top.labelEn(),
                        top.labelCn(),
                        top.probability()
                    ));
                }
            } catch (Exception e) {
                log.warn("[VideoAnalyzer] 帧 {} 分类失败: {}", files[i].getName(), e.getMessage());
            }
        }
        return result;
    }

    private void deleteRecursively(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) deleteRecursively(c);
        }
        Files.deleteIfExists(f.toPath());
    }

    // ─── DTO ────────────────────────────────────────────

    public record FrameAnalysis(double timestampSec, String labelEn, String labelCn, float confidence) {
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("t", timestampSec);
            m.put("labelEn", labelEn);
            m.put("labelCn", labelCn);
            m.put("confidence", confidence);
            return m;
        }
    }

    public record VideoAnalysisResult(
        boolean success,
        String error,
        FFmpegRunner.MediaInfo media,
        List<FrameAnalysis> frames,
        String transcript,
        long asrCostMs,
        long costMs
    ) {
        public static VideoAnalysisResult error(String msg) {
            return new VideoAnalysisResult(false, msg, FFmpegRunner.MediaInfo.empty(),
                Collections.emptyList(), "", 0, 0);
        }

        /**
         * 聚合 frames: 同类合并为时间区间
         */
        public List<Map<String, Object>> timeline() {
            if (frames == null || frames.isEmpty()) return Collections.emptyList();
            List<Map<String, Object>> tl = new ArrayList<>();
            FrameAnalysis prev = frames.get(0);
            double segStart = prev.timestampSec;
            for (int i = 1; i < frames.size(); i++) {
                FrameAnalysis cur = frames.get(i);
                if (!cur.labelEn().equals(prev.labelEn())) {
                    tl.add(segMap(segStart, prev));
                    segStart = cur.timestampSec;
                }
                prev = cur;
            }
            tl.add(segMap(segStart, prev));
            return tl;
        }

        private Map<String, Object> segMap(double start, FrameAnalysis f) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("labelEn", f.labelEn());
            m.put("labelCn", f.labelCn());
            m.put("start", start);
            m.put("end", f.timestampSec());
            m.put("duration", f.timestampSec() - start);
            m.put("confidence", f.confidence());
            return m;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("success", success);
            m.put("error", error);
            m.put("transcript", transcript);
            m.put("asrCostMs", asrCostMs);
            m.put("costMs", costMs);
            m.put("media", Map.of(
                "durationSec", media.durationSec,
                "width", media.width, "height", media.height,
                "fps", media.fps,
                "hasVideo", media.hasVideo, "hasAudio", media.hasAudio
            ));
            m.put("frames", frames.stream().map(FrameAnalysis::toMap).collect(Collectors.toList()));
            m.put("timeline", timeline());
            return m;
        }
    }
}
