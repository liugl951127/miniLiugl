package com.minimax.ai.intent;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * 文本归一化工具 (V3.5.7 外部化配置版)
 *
 * <h2>与 V3.5.6 区别</h2>
 * <ul>
 *   <li>V3.5.6: 同义词 / 简繁表硬编码 static final</li>
 *   <li>V3.5.7: 通过 IntentConfig 注入 + {@link #setSynonyms(Map)} 运行时换</li>
 * </ul>
 *
 * <h2>关键: LinkedHashMap 保序</h2>
 * setTraditional 必须用 LinkedHashMap 包装, 否则 HashMap 顺序随机,
 * 会先匹配 "訂" (1 字) 再匹配 "訂單" (2 字), 导致 "退訂單" -> "退订單" (错).
 *
 * @author MiniMax
 * @since V3.5.7
 */
public final class TextNormalizer {

    private TextNormalizer() {}

    /** 全角 -> 半角 转换表 (大小 0xFF60, 容纳 0xFF01~0xFF5E) */
    private static final char[] FULL_WIDTH_OFFSETS = new char[0xFF60];
    static {
        Arrays.fill(FULL_WIDTH_OFFSETS, (char) 0);
        for (int i = 0xFF01; i <= 0xFF5E; i++) {
            FULL_WIDTH_OFFSETS[i] = (char) (i - 0xFEE0);
        }
    }

    /** 同义词字典 (可热更新, LinkedHashMap 保序) */
    private static final AtomicReference<Map<String, String>> SYNONYMS = new AtomicReference<>(new LinkedHashMap<>());
    /** 简繁对照表 (可热更新, LinkedHashMap 保序) */
    private static final AtomicReference<Map<String, String>> TRAD = new AtomicReference<>(new LinkedHashMap<>());

    /** 多余空白 (包括全角空格) */
    private static final Pattern EXTRA_SPACE = Pattern.compile("[\\s\u3000]+");
    private static final Pattern REPEATED = Pattern.compile("(.)\\1{2,}");

    /** 设置同义词字典 */
    public static void setSynonyms(Map<String, String> synonyms) {
        SYNONYMS.set(synonyms != null ? new LinkedHashMap<>(synonyms) : new LinkedHashMap<>());
    }

    /** 设置简繁对照表 (LinkedHashMap 保序: 长词在前, 短词在后) */
    public static void setTraditional(Map<String, String> trad) {
        TRAD.set(trad != null ? new LinkedHashMap<>(trad) : new LinkedHashMap<>());
    }

    /** 主入口: 5 步归一化 */
    public static NormalizedResult normalize(String text) {
        if (text == null || text.isBlank()) {
            return new NormalizedResult("", "", List.of());
        }
        String t = text.trim();
        t = fullWidthToHalfWidth(t);
        t = t.toLowerCase(Locale.ROOT);
        t = tradToSimp(t);
        Set<String> expansions = expandSynonyms(t);
        t = EXTRA_SPACE.matcher(t).replaceAll(" ").trim();
        t = REPEATED.matcher(t).replaceAll("$1$1");
        return new NormalizedResult(t, text, new ArrayList<>(expansions));
    }

    /** 全角转半角 (对 ASCII 范围字符有效) */
    static String fullWidthToHalfWidth(String s) {
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c >= 0xFF01 && c <= 0xFF5E) {
                chars[i] = (char) (c - 0xFEE0);
            } else if (c == 0x3000) {
                chars[i] = ' ';
            }
        }
        return new String(chars);
    }

    /** 简繁 (用配置表, LinkedHashMap 顺序敏感: 长词必须先于短词) */
    static String tradToSimp(String s) {
        String result = s;
        for (var e : TRAD.get().entrySet()) {
            result = result.replace(e.getKey(), e.getValue());
        }
        return result;
    }

    /** 同义词扩展 */
    static Set<String> expandSynonyms(String text) {
        Set<String> out = new LinkedHashSet<>();
        for (var e : SYNONYMS.get().entrySet()) {
            if (text.contains(e.getKey())) {
                out.add(e.getValue());
            }
        }
        return out;
    }

    /** 归一化结果 */
    public record NormalizedResult(String normalized, String original, List<String> expansions) {}
}
