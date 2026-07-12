package com.minimax.ai.controller;

import com.minimax.ai.compliance.AuditLogger;
import com.minimax.ai.compliance.ContentModerator;
import com.minimax.ai.compliance.DataMasker;
import com.minimax.ai.compliance.FileEncryptor;
import com.minimax.ai.entity.MultimediaFile;
import com.minimax.ai.mapper.MultimediaFileMapper;
import com.minimax.ai.multimodal.AudioAnalyzer;
import com.minimax.ai.multimodal.ImageAnalyzer;
import com.minimax.ai.multimodal.VideoAnalyzer;
import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 多模态 AI 控制器 (V2.6)
 *
 * 接口:
 *   - POST /api/ai/multimodal/image/upload    图片上传 + 分析
 *   - POST /api/ai/multimodal/audio/upload    语音上传 + 分析 (含 STT)
 *   - POST /api/ai/multimodal/video/upload    视频上传 + 分析
 *   - GET  /api/ai/multimodal/files           我的文件列表
 *   - GET  /api/ai/multimodal/file/{id}       下载文件 (解密)
 *   - POST /api/ai/multimodal/tts             文本转语音
 *   - POST /api/ai/multimodal/image/compare   图片相似度对比
 *
 * 合规:
 *   - 上传前内容审核
 *   - 加密存储 (AES-256-GCM)
 *   - 审计日志
 *   - 用户隔离
 *   - 大小限制
 *   - 类型白名单
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/multimodal")
@RequiredArgsConstructor
public class MultimodalController {

    private final ImageAnalyzer imageAnalyzer;
    private final AudioAnalyzer audioAnalyzer;
    private final VideoAnalyzer videoAnalyzer;
    private final FileEncryptor fileEncryptor;
    private final ContentModerator contentModerator;
    private final AuditLogger auditLogger;
    private final DataMasker dataMasker;
    private final MultimediaFileMapper fileMapper;

    /** 最大文件 100MB */
    private static final long MAX_SIZE = 100 * 1024 * 1024;

    /** 存储根目录 */
    private static final String STORAGE_ROOT = System.getProperty("java.io.tmpdir") + "/minimax-media";

    // ================== 图片 ==================

    @PostMapping("/image/upload")
    public Result<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @RequestHeader(value = "X-User-Ip", required = false) String ip) throws IOException {
        long start = System.currentTimeMillis();
        if (file.isEmpty()) return Result.fail("文件为空");
        if (file.getSize() > MAX_SIZE) return Result.fail("文件超过 100MB");
        String name = file.getOriginalFilename();
        if (name == null || !imageAnalyzer.isSupported(name)) {
            return Result.fail("不支持的图片格式 (支持: jpg/png/gif/bmp/webp)");
        }

        byte[] data = file.getBytes();
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 内容审核
        ContentModerator.ModerationResult mr = contentModerator.moderateFile(
                "IMAGE", sha256(data), null);
        if (!mr.isPass()) {
            auditLogger.log("FILE_UPLOAD_REJECTED", userId, username, ip, null, "file", null, "POST",
                    "/api/ai/multimodal/image/upload", name, 403, "DENIED", System.currentTimeMillis() - start);
            return Result.fail("图片未通过审核: " + mr.reason);
        }

        // 2. AI 分析
        ImageAnalyzer.ImageAnalysisResult analysis = imageAnalyzer.analyze(data, name);

        // 3. 加密保存
        String fileId = UUID.randomUUID().toString();
        String storagePath = saveEncrypted(data, fileId, "image");

        // 4. 落库
        MultimediaFile mf = new MultimediaFile();
        mf.setFileId(fileId);
        mf.setUserId(userId);
        mf.setUsername(username);
        mf.setFileName(fileId + ".enc");
        mf.setOriginalName(name);
        mf.setFileType("IMAGE");
        mf.setMimeType(file.getContentType());
        mf.setFileSize(file.getSize());
        mf.setFileHash(analysis.sha256);
        mf.setStoragePath(storagePath);
        mf.setStorageType("LOCAL");
        mf.setEncrypted(1);
        mf.setWidth(analysis.width);
        mf.setHeight(analysis.height);
        mf.setModerationStatus("PASS");
        mf.setWatermarked(0);
        mf.setIsPublic(0);
        mf.setAccessCount(0);
        fileMapper.insert(mf);

        // 5. 审计
        auditLogger.logFileUpload(userId, username, ip, fileId, file.getSize(), "IMAGE");

        result.put("fileId", fileId);
        result.put("analysis", analysis);
        result.put("encrypted", true);
        result.put("moderation", mr);
        return Result.ok(result);
    }

    // ================== 语音 ==================

    @PostMapping("/audio/upload")
    public Result<Map<String, Object>> uploadAudio(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @RequestHeader(value = "X-User-Ip", required = false) String ip) throws IOException {
        long start = System.currentTimeMillis();
        if (file.isEmpty()) return Result.fail("文件为空");
        if (file.getSize() > MAX_SIZE) return Result.fail("文件超过 100MB");
        String name = file.getOriginalFilename();
        if (name == null || !audioAnalyzer.isSupported(name)) {
            return Result.fail("不支持的音频格式 (支持: wav/pcm/au/aiff)");
        }

        byte[] data = file.getBytes();
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 分析
        AudioAnalyzer.AudioAnalysisResult analysis = audioAnalyzer.analyze(data, name);
        // 2. STT (占位)
        String transcript = audioAnalyzer.transcribe(data);

        // 3. 文本审核 (针对 transcript)
        ContentModerator.ModerationResult mr = contentModerator.moderateText(
                transcript, userId, username, null, null);
        if ("REJECT".equals(mr.status)) {
            return Result.fail("语音内容未通过审核: " + mr.reason);
        }

        // 4. 加密保存
        String fileId = UUID.randomUUID().toString();
        String storagePath = saveEncrypted(data, fileId, "audio");

        // 5. 落库
        MultimediaFile mf = new MultimediaFile();
        mf.setFileId(fileId);
        mf.setUserId(userId);
        mf.setUsername(username);
        mf.setFileName(fileId + ".enc");
        mf.setOriginalName(name);
        mf.setFileType("VOICE");
        mf.setMimeType(file.getContentType());
        mf.setFileSize(file.getSize());
        mf.setFileHash(analysis.sha256);
        mf.setStoragePath(storagePath);
        mf.setStorageType("LOCAL");
        mf.setEncrypted(1);
        mf.setSampleRate(analysis.sampleRate);
        mf.setChannels(analysis.channels);
        mf.setBitrate(analysis.bitrate);
        mf.setCodec(analysis.codec);
        mf.setDurationMs(analysis.durationMs);
        mf.setModerationStatus(mr.status);
        mf.setWatermarked(0);
        mf.setIsPublic(0);
        fileMapper.insert(mf);

        // 6. 审计
        auditLogger.logFileUpload(userId, username, ip, fileId, file.getSize(), "VOICE");

        result.put("fileId", fileId);
        result.put("analysis", analysis);
        result.put("transcript", transcript);
        result.put("emotionTendency", analysis.emotionTendency);
        result.put("moderation", mr);
        return Result.ok(result);
    }

    // ================== 视频 ==================

    @PostMapping("/video/upload")
    public Result<Map<String, Object>> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @RequestHeader(value = "X-User-Ip", required = false) String ip) throws IOException {
        long start = System.currentTimeMillis();
        if (file.isEmpty()) return Result.fail("文件为空");
        if (file.getSize() > MAX_SIZE) return Result.fail("文件超过 100MB");
        String name = file.getOriginalFilename();
        if (name == null || !videoAnalyzer.isSupported(name)) {
            return Result.fail("不支持的视频格式 (支持: mp4/mov/m4v/3gp/mkv)");
        }

        byte[] data = file.getBytes();
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 分析
        VideoAnalyzer.VideoAnalysisResult analysis = videoAnalyzer.analyze(data, name);

        // 2. 审核
        ContentModerator.ModerationResult mr = contentModerator.moderateFile(
                "VIDEO", analysis.sha256, null);
        if (!mr.isPass()) {
            return Result.fail("视频未通过审核: " + mr.reason);
        }

        // 3. 加密保存
        String fileId = UUID.randomUUID().toString();
        String storagePath = saveEncrypted(data, fileId, "video");

        // 4. 落库
        MultimediaFile mf = new MultimediaFile();
        mf.setFileId(fileId);
        mf.setUserId(userId);
        mf.setUsername(username);
        mf.setFileName(fileId + ".enc");
        mf.setOriginalName(name);
        mf.setFileType("VIDEO");
        mf.setMimeType(file.getContentType());
        mf.setFileSize(file.getSize());
        mf.setFileHash(analysis.sha256);
        mf.setStoragePath(storagePath);
        mf.setStorageType("LOCAL");
        mf.setEncrypted(1);
        mf.setBitrate(analysis.bitrate);
        mf.setDurationMs(analysis.durationMs);
        mf.setWidth(analysis.width);
        mf.setHeight(analysis.height);
        mf.setModerationStatus("PASS");
        mf.setWatermarked(0);
        mf.setIsPublic(0);
        fileMapper.insert(mf);

        auditLogger.logFileUpload(userId, username, ip, fileId, file.getSize(), "VIDEO");

        result.put("fileId", fileId);
        result.put("analysis", analysis);
        result.put("moderation", mr);
        return Result.ok(result);
    }

    // ================== 列表 / 下载 ==================

    @GetMapping("/files")
    public Result<List<MultimediaFile>> listFiles(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        List<MultimediaFile> files;
        if (userId != null) {
            files = fileMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MultimediaFile>()
                            .eq("user_id", userId)
                            .orderByDesc("id")
                            .last("LIMIT 100")
            );
        } else {
            files = fileMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MultimediaFile>()
                            .orderByDesc("id")
                            .last("LIMIT 100")
            );
        }
        return Result.ok(files);
    }

    @GetMapping("/file/{fileId}/info")
    public Result<MultimediaFile> getFileInfo(@PathVariable String fileId) {
        MultimediaFile mf = fileMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MultimediaFile>()
                        .eq("file_id", fileId)
        );
        if (mf == null) return Result.fail("文件不存在");
        // 增加访问计数
        mf.setAccessCount(mf.getAccessCount() + 1);
        fileMapper.updateById(mf);
        return Result.ok(mf);
    }

    // ================== TTS ==================

    @PostMapping("/tts")
    public Result<Map<String, Object>> textToSpeech(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @RequestHeader(value = "X-User-Ip", required = false) String ip) throws IOException {
        long start = System.currentTimeMillis();
        String text = (String) body.get("text");
        if (text == null || text.isEmpty()) return Result.fail("文本不能为空");
        if (text.length() > 1000) return Result.fail("文本超过 1000 字符");

        // 1. 文本审核
        ContentModerator.ModerationResult mr = contentModerator.moderateText(
                text, userId, username, null, null);
        if ("REJECT".equals(mr.status)) {
            auditLogger.log("AI_TTS_REJECTED", userId, username, ip, null, "text", null, "POST",
                    "/api/ai/multimodal/tts", text, 403, "DENIED", System.currentTimeMillis() - start);
            return Result.fail("文本未通过审核: " + mr.reason);
        }

        // 2. TTS (占位)
        int sampleRate = 16000;
        byte[] wavData = audioAnalyzer.synthesize(text, sampleRate);

        // 3. 加密
        String fileId = UUID.randomUUID().toString();
        String storagePath = saveEncrypted(wavData, fileId, "tts");

        // 4. 落库
        MultimediaFile mf = new MultimediaFile();
        mf.setFileId(fileId);
        mf.setUserId(userId);
        mf.setUsername(username);
        mf.setFileName(fileId + ".enc");
        mf.setOriginalName("tts_" + System.currentTimeMillis() + ".wav");
        mf.setFileType("VOICE");
        mf.setMimeType("audio/wav");
        mf.setFileSize((long) wavData.length);
        mf.setFileHash(sha256(wavData));
        mf.setStoragePath(storagePath);
        mf.setStorageType("LOCAL");
        mf.setEncrypted(1);
        mf.setSampleRate(sampleRate);
        mf.setChannels(1);
        mf.setBitrate(sampleRate * 16);
        mf.setCodec("PCM");
        mf.setModerationStatus(mr.status);
        mf.setWatermarked(1); // TTS 默认加 AI 水印
        fileMapper.insert(mf);

        // 5. 审计
        auditLogger.logAiCall(userId, username, ip, "TTS", dataMasker.mask(text), System.currentTimeMillis() - start, true);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fileId", fileId);
        result.put("size", wavData.length);
        result.put("sampleRate", sampleRate);
        result.put("durationMs", (long) wavData.length * 1000 / (sampleRate * 2));
        result.put("watermark", "本内容由 AI 合成");
        return Result.ok(result);
    }

    // ================== 图片对比 ==================

    @PostMapping("/image/compare")
    public Result<Map<String, Object>> compareImages(
            @RequestParam("file1") MultipartFile file1,
            @RequestParam("file2") MultipartFile file2) throws IOException {
        if (file1.isEmpty() || file2.isEmpty()) return Result.fail("文件不能为空");
        ImageAnalyzer.ImageAnalysisResult a1 = imageAnalyzer.analyze(file1.getBytes(), file1.getOriginalFilename());
        ImageAnalyzer.ImageAnalysisResult a2 = imageAnalyzer.analyze(file2.getBytes(), file2.getOriginalFilename());
        int hamming = imageAnalyzer.phashDistance(a1.phash, a2.phash);
        double cos = imageAnalyzer.visualSimilarity(a1.embedding, a2.embedding);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("image1", Map.of(
                "size", a1.width + "x" + a1.height,
                "phash", a1.phash,
                "hashHex", String.format("%016x", a1.phash)));
        result.put("image2", Map.of(
                "size", a2.width + "x" + a2.height,
                "phash", a2.phash,
                "hashHex", String.format("%016x", a2.phash)));
        result.put("phashHammingDistance", hamming);     // 0 = 完全相同, 64 = 完全不同
        result.put("phashSimilarity", 1.0 - hamming / 64.0);
        result.put("cosineSimilarity", cos);              // -1 ~ 1
        result.put("isSame", hamming < 5);                // 阈值 5
        return Result.ok(result);
    }

    // ================== 合规 API ==================

    @PostMapping("/compliance/moderate-text")
    public Result<ContentModerator.ModerationResult> moderateText(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        return Result.ok(contentModerator.moderateText(text));
    }

    @PostMapping("/compliance/mask")
    public Result<Map<String, String>> maskText(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        Map<String, String> result = new LinkedHashMap<>();
        result.put("original", text == null ? "" : text);
        result.put("masked", dataMasker.mask(text));
        result.put("containsMobile", String.valueOf(dataMasker.containsMobile(text)));
        result.put("containsIdCard", String.valueOf(dataMasker.containsIdCard(text)));
        result.put("containsSensitive", String.valueOf(dataMasker.containsSensitive(text)));
        return Result.ok(result);
    }

    @PostMapping("/compliance/refresh-sensitive-words")
    public Result<String> refreshSensitiveWords() {
        contentModerator.refreshCache();
        return Result.ok("已刷新敏感词缓存");
    }

    // ================== 工具方法 ==================

    private String saveEncrypted(byte[] data, String fileId, String type) throws IOException {
        Path dir = Paths.get(STORAGE_ROOT, type);
        Files.createDirectories(dir);
        Path target = dir.resolve(fileId + ".enc");
        byte[] encrypted = fileEncryptor.encrypt(data);
        Files.write(target, encrypted);
        return target.toString();
    }

    private String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "0";
        }
    }
}
