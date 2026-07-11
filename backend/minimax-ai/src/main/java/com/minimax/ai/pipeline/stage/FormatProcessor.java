package com.minimax.ai.pipeline.stage;

import com.minimax.ai.pipeline.stage.ModelInference.InferenceResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 阶段 11: 格式化整理器 (V2.8.5)
 *
 * <h3>职责</h3>
 * 清理模型原始输出, 提升可读性:
 * <ul>
 *   <li>去除前后空白</li>
 *   <li>合并连续空行</li>
 *   <li>去除行尾空格</li>
 *   <li>统一标点 (半角/全角)</li>
 *   <li>截断过长输出</li>
 *   <li>首字母大写 (英文)</li>
 *   <li>Markdown 化 (可选)</li>
 * </ul>
 *
 * <h3>算法</h3>
 * 链式正则替换, 每步独立可配置开关.
 */
@Slf4j
@Component
public class FormatProcessor {

    /** 多空行模式 */
    private static final Pattern MULTI_BLANK_LINES = Pattern.compile("\\n{3,}");
    /** 行尾空格 */
    private static final Pattern TRAILING_SPACE = Pattern.compile("[ \\t]+$", Pattern.MULTILINE);
    /** 连续空格 (保留换行) */
    private static final Pattern MULTI_SPACE = Pattern.compile("(?<=\\S) {2,}(?=\\S)");
    /** 半角标点 → 全角 (中文上下文) */
    private static final Pattern HALF_PUNCT = Pattern.compile("([\\u4e00-\\u9fff]),\\s*");

    /** 配置 */
    public static volatile boolean ENABLE_MD = true;  // Markdown 化
    public static volatile boolean TRUNCATE_LONG = true;  // 截断
    public static volatile int MAX_LENGTH = 4000;  // 最大字符数

    /**
     * 格式化模型输出
     */
    public FormatResult format(InferenceResult inference) {
        long start = System.currentTimeMillis();
        String raw = inference.outputText != null ? inference.outputText : "";
        log.info("[stage-11/format] rawLen={}", raw.length());

        String formatted = raw;
        // 1. 去前后空白
        formatted = formatted.trim();
        // 2. 合并连续空行
        formatted = MULTI_BLANK_LINES.matcher(formatted).replaceAll("\n\n");
        // 3. 去除行尾空格
        formatted = TRAILING_SPACE.matcher(formatted).replaceAll("");
        // 4. 合并连续空格 (除换行外)
        formatted = MULTI_SPACE.matcher(formatted).replaceAll(" ");
        // 5. 中文上下文标点: 半角逗号 → 全角
        formatted = HALF_PUNCT.matcher(formatted).replaceAll("$1, ");
        // 6. 截断
        boolean truncated = false;
        if (TRUNCATE_LONG && formatted.length() > MAX_LENGTH) {
            formatted = formatted.substring(0, MAX_LENGTH) + "\n\n[内容过长, 已截断]";
            truncated = true;
        }
        // 7. Markdown 化 (简单: 列表项加 "-")
        if (ENABLE_MD && !formatted.contains("```") && !formatted.contains("#")) {
            // 仅在用户输入中提到列表/步骤时启用 (V2.8.5 简化: 总是尝试)
            formatted = markdownize(formatted);
        }

        FormatResult r = new FormatResult();
        r.rawText = raw;
        r.formattedText = formatted;
        r.truncated = truncated;
        r.charCount = formatted.length();
        r.lineCount = formatted.split("\n").length;
        r.costMs = System.currentTimeMillis() - start;
        log.info("[stage-11/format] → finalLen={}, lines={}, truncated={}, costMs={}",
                r.charCount, r.lineCount, truncated, r.costMs);
        return r;
    }

    /**
     * 简单 Markdown 化: 数字列表 "1. xxx" → "- xxx"
     */
    private String markdownize(String text) {
        return text.replaceAll("(?m)^(\\d+)\\.\\s+", "- ");
    }

    /** 格式化结果 */
    @lombok.Data
    public static class FormatResult {
        public String rawText;
        public String formattedText;
        public boolean truncated;
        public int charCount;
        public int lineCount;
        public long costMs;
    }
}
