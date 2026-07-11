package com.minimax.ai.pipeline.stage;

import com.minimax.ai.pipeline.config.PipelineConfig.InputModality;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 阶段 2: 网关分发器 (V2.8.5)
 *
 * <h3>职责</h3>
 * 根据输入模态 + 用户路由偏好, 决定走哪个处理分支.
 * 类似 API Gateway 的路由层, 业务核心是把请求分到对应处理器.
 *
 * <h3>路由规则</h3>
 * <ul>
 *   <li>TEXT  → 直接进入文本处理流</li>
 *   <li>IMAGE → 走 OCR → 文本处理流</li>
 *   <li>AUDIO → 走 ASR → 文本处理流</li>
 *   <li>VIDEO → 抽关键帧 OCR + 音轨 ASR → 文本处理流</li>
 *   <li>FILE  → 解析器 (PDF/Word/Excel) → 文本处理流</li>
 * </ul>
 *
 * <h3>动态数据</h3>
 * 路由决策不依赖硬编码, 而是根据输入特征 (文件后缀, MIME, base64 头) 实时判断.
 */
@Slf4j
@Component
public class GatewayDispatcher {

    /**
     * 网关分发
     *
     * @param rawInput 用户原始输入
     * @return 分发结果 (含模态, 路由, 转发参数)
     */
    public DispatchResult dispatch(RawInput rawInput) {
        long start = System.currentTimeMillis();
        log.info("[stage-2/gateway] input: modality={}, hasText={}, hasFile={}, fileName={}",
                rawInput.modality, rawInput.text != null, rawInput.fileData != null, rawInput.fileName);

        DispatchResult result = new DispatchResult();
        result.originalInput = rawInput;
        result.route = new LinkedHashMap<>();
        result.timestamp = System.currentTimeMillis();

        // 1. 自动检测模态 (如果调用方没指定)
        InputModality actualModality = rawInput.modality != null
                ? rawInput.modality
                : autoDetectModality(rawInput);

        // 2. 根据模态规划路由链
        switch (actualModality) {
            case TEXT:
                result.route.put("text-process", "DIRECT");
                break;
            case IMAGE:
                result.route.put("ocr", "ENABLED");
                result.route.put("text-process", "AFTER_OCR");
                break;
            case AUDIO:
                result.route.put("asr", "ENABLED");
                result.route.put("text-process", "AFTER_ASR");
                break;
            case VIDEO:
                result.route.put("video-keyframe-extract", "ENABLED");
                result.route.put("ocr", "ON_FRAMES");
                result.route.put("asr", "ON_AUDIO");
                result.route.put("text-process", "MERGED_RESULT");
                break;
            case FILE:
                String parser = detectFileParser(rawInput.fileName);
                result.route.put("file-parser", parser);
                result.route.put("text-process", "AFTER_PARSER");
                break;
        }

        // 3. 用户偏好覆盖 (例如 "直接给我结果, 不要走 RAG")
        if (rawInput.preferences != null) {
            rawInput.preferences.forEach((k, v) -> result.route.put("user-pref:" + k, String.valueOf(v)));
        }

        result.detectedModality = actualModality;
        result.costMs = System.currentTimeMillis() - start;

        log.info("[stage-2/gateway] → modality={}, route={}, costMs={}",
                actualModality, result.route, result.costMs);
        return result;
    }

    /**
     * 自动检测输入模态 (基于文件后缀 + data URI 前缀)
     */
    private InputModality autoDetectModality(RawInput input) {
        if (input.text != null && !input.text.isEmpty()) return InputModality.TEXT;
        if (input.fileName == null) return InputModality.TEXT;

        String name = input.fileName.toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                || name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".webp")) {
            return InputModality.IMAGE;
        }
        if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a")
                || name.endsWith(".ogg") || name.endsWith(".flac")) {
            return InputModality.AUDIO;
        }
        if (name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mov")
                || name.endsWith(".mkv") || name.endsWith(".webm")) {
            return InputModality.VIDEO;
        }
        return InputModality.FILE;
    }

    /**
     * 根据文件后缀选择解析器
     */
    private String detectFileParser(String fileName) {
        if (fileName == null) return "RAW_TEXT";
        String name = fileName.toLowerCase();
        if (name.endsWith(".pdf")) return "PDF_PARSER";
        if (name.endsWith(".doc") || name.endsWith(".docx")) return "WORD_PARSER";
        if (name.endsWith(".xls") || name.endsWith(".xlsx")) return "EXCEL_PARSER";
        if (name.endsWith(".ppt") || name.endsWith(".pptx")) return "PPT_PARSER";
        if (name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".log")) return "TEXT_READER";
        if (name.endsWith(".json")) return "JSON_PARSER";
        if (name.endsWith(".xml") || name.endsWith(".html") || name.endsWith(".htm")) return "MARKUP_PARSER";
        return "GENERIC_FILE";
    }

    /** 原始输入 DTO */
    @lombok.Data
    public static class RawInput {
        /** 文本输入 (可空, 与 file 二选一) */
        public String text;
        /** 输入模态 (为空时自动检测) */
        public InputModality modality;
        /** 文件二进制 (Base64) */
        public String fileData;
        /** 文件名 (含后缀) */
        public String fileName;
        /** 用户偏好 (如 {skipRag: true, language: "zh"}) */
        public Map<String, Object> preferences;
        /** 客户端信息 (用于审计) */
        public String clientIp;
        public String userAgent;
    }

    /** 分发结果 DTO */
    @lombok.Data
    public static class DispatchResult {
        public RawInput originalInput;
        public InputModality detectedModality;
        public Map<String, String> route;
        public long timestamp;
        public long costMs;
    }
}
