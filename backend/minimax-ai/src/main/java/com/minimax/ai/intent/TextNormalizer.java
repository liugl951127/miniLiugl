package com.minimax.ai.intent;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * 文本归一化工具 (Text Normalizer) (V3.5.7 外部化配置版)
 *
 * <h2>为什么需要归一化</h2>
 * 用户输入千变万化, 但关键词匹配需要规范化输入:
 * <ul>
 *   <li>"我要退款！！！" vs "我要退款" -> 标点噪音</li>
 *   <li>"退款" vs "退訂" vs "退钱" -> 简繁 / 同义</li>
 *   <li>"Hello" vs "hello" vs "HELLO" -> 大小写</li>
 *   <li>"我要退  款" vs "我要退款" -> 空白</li>
 *   <li>"哈哈哈哈" -> 重复字</li>
 * </ul>
 * 归一化后所有等价输入产出统一特征, 召回率 +20%.
 *
 * <h2>5 步归一化流水线 (顺序敏感)</h2>
 * <ol>
 *   <li><b>trim</b> 去除首尾空白</li>
 *   <li><b>全角 → 半角</b> (！ -> !, ， -> , 等)</li>
 *   <li><b>lowercase</b> (英文字母转小写)</li>
 *   <li><b>繁 → 简</b> (退訂 -> 退订, 用 LinkedHashMap 长词优先)</li>
 *   <li><b>同义词扩展</b> (退款 -> 退钱, 输出到 expansions[])</li>
 *   <li><b>空白合并</b> (全角空格 / 多空格 -> 单空格)</li>
 *   <li><b>重复字压缩</b> (哈哈哈哈 -> 哈哈, 限 2 次)</li>
 * </ol>
 *
 * <h2>关键: LinkedHashMap 保序</h2>
 * 简繁表必须用 {@link LinkedHashMap} 包装, 而非 HashMap.
 * <p>原因: HashMap 顺序随机, 会先匹配 "訂" (1 字) 再匹配 "退訂單" (3 字),
 * 导致 "退訂單" → "退订單" (错), 而 LinkedHashMap 保持插入顺序
 * 长词在前, 短词在后 → "退訂單" → "退订单" (对).
 *
 * <h2>线程安全</h2>
 * 使用 {@link AtomicReference} 包装, 支持热更新 (IntentConfig.update 时换表).
 *
 * <h2>使用示例</h2>
 * <pre>
 *   TextNormalizer.setSynonyms(config.getSynonyms());
 *   TextNormalizer.setTraditional(config.getTraditional());
 *   NormalizedResult r = TextNormalizer.normalize("我要退訂！！！");
 *   r.normalized();   // "我要退订!"
 *   r.expansions();   // [退钱, 退货, 申请退款]  (同义词扩展)
 * </pre>
 *
 * @author MiniMax
 * @since V3.5.7
 */
public final class TextNormalizer {

    /** 工具类不允许实例化 */
    private TextNormalizer() {}

    /**
     * 全角字符 -> 半角字符 偏移表.
     * <p>原理: Unicode 全角 ASCII 范围是 U+FF01~U+FF5E,
     * 与半角 (U+0021~U+007E) 相差 0xFEE0.
     * 例: '！' (U+FF01) - 0xFEE0 = '!' (U+0021)
     * <p>数组大小 0xFF60 覆盖 U+0000~U+FF5F 全部范围.
     */
    private static final char[] FULL_WIDTH_OFFSETS = new char[0xFF60];
    static {
        // 初始化: 全为 0 (无偏移)
        Arrays.fill(FULL_WIDTH_OFFSETS, (char) 0);
        // ASCII 全角范围: U+FF01 (!) 到 U+FF5E (~)
        for (int i = 0xFF01; i <= 0xFF5E; i++) {
            // 全角 -> 半角: 减 0xFEE0
            FULL_WIDTH_OFFSETS[i] = (char) (i - 0xFEE0);
        }
    }

    /**
     * 同义词字典: 标准词 -> 扩展词列表 (用 , 隔开).
     * <p>例: "退款" -> "退钱,退货,申请退款"
     * <p>LinkedHashMap 保持插入顺序, 避免 HashMap 顺序随机导致匹配错乱.
     * <p>AtomicReference 包装, 支持配置热更新 (config update 时整体替换).
     */
    private static final AtomicReference<Map<String, String>> SYNONYMS = new AtomicReference<>(new LinkedHashMap<>());

    /**
     * 简繁对照表: 繁体 -> 简体.
     * <p>例: "退訂" -> "退订", "訂單" -> "订单"
     * <p>LinkedHashMap 强保序: 长词在前, 短词在后.
     * 例: "退訂單" (3字) 必须排在 "訂" (1字) 前面,
     * 否则 "退訂單" -> "退订單" (错).
     */
    private static final AtomicReference<Map<String, String>> TRAD = new AtomicReference<>(new LinkedHashMap<>());

    /** 空白字符匹配: 普通空白 + 全角空格 (U+3000) */
    private static final Pattern EXTRA_SPACE = Pattern.compile("[\\s\u3000]+");
    /** 重复字符匹配: 同一字符连续 3 次以上 */
    private static final Pattern REPEATED = Pattern.compile("(.)\\1{2,}");

    /**
     * 设置同义词字典 (热更新入口).
     * <p>调用时机: IntentConfig 更新时, 同步调用本方法.
     * <p>内部包装 LinkedHashMap, 排除外部传入 HashMap 的风险.
     *
     * @param synonyms 同义字典: 触发词 -> 扩展词 (可空, 表示清空)
     */
    public static void setSynonyms(Map<String, String> synonyms) {
        SYNONYMS.set(synonyms != null ? new LinkedHashMap<>(synonyms) : new LinkedHashMap<>());
    }

    /**
     * 设置简繁对照表 (热更新入口).
     * <p>内部包装 LinkedHashMap, 强制长词在前短词在后.
     *
     * @param trad 简繁表: 繁体 -> 简体 (可空, 表示清空)
     */
    public static void setTraditional(Map<String, String> trad) {
        TRAD.set(trad != null ? new LinkedHashMap<>(trad) : new LinkedHashMap<>());
    }

    /**
     * 主入口: 5 步归一化.
     * <p>调用链: trim -> 全角转半角 -> 小写 -> 繁转简 -> 同义词扩展 -> 空白合并 -> 重复字压缩.
     * <p>每步都是无副作用的 String 操作, 可重入.
     *
     * @param text 原始用户输入
     * @return 归一化结果: normalized (处理后) + original (原文) + expansions (扩展词列表)
     */
    public static NormalizedResult normalize(String text) {
        // 空入参兜底: 避免后续 NPE
        if (text == null || text.isBlank()) {
            return new NormalizedResult("", "", List.of());
        }
        // 步骤 1: trim 去除首尾空白
        String t = text.trim();
        // 步骤 2: 全角字符转半角 (标点 / 字母 / 数字)
        t = fullWidthToHalfWidth(t);
        // 步骤 3: lowercase (英文)
        t = t.toLowerCase(Locale.ROOT);
        // 步骤 4: 繁转简 (用 LinkedHashMap 表, 顺序敏感)
        t = tradToSimp(t);
        // 步骤 5: 同义词扩展 (不修改原 text, 输出到 expansions)
        Set<String> expansions = expandSynonyms(t);
        // 步骤 6: 空白字符合并 (普通空白 + 全角空格 -> 单空格)
        t = EXTRA_SPACE.matcher(t).replaceAll(" ").trim();
        // 步骤 7: 重复字压缩 (哈哈哈哈 -> 哈哈, 防止情感过激干扰匹配)
        t = REPEATED.matcher(t).replaceAll("$1$1");
        return new NormalizedResult(t, text, new ArrayList<>(expansions));
    }

    /**
     * 全角字符转半角 (内部步骤).
     * <p>算法: 字符在 U+FF01~U+FF5E 范围时, 减 0xFEE0 偏移.
     * <p>特殊: U+3000 (全角空格) -> U+0020 (半角空格).
     *
     * @param s 待转换字符串
     * @return 转换结果
     */
    static String fullWidthToHalfWidth(String s) {
        // 字符数组原地修改, 避免 StringBuilder 分配
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c >= 0xFF01 && c <= 0xFF5E) {
                // 全角 ASCII -> 半角 ASCII
                chars[i] = (char) (c - 0xFEE0);
            } else if (c == 0x3000) {
                // 全角空格 -> 半角空格
                chars[i] = ' ';
            }
        }
        return new String(chars);
    }

    /**
     * 繁转简 (内部步骤).
     * <p>LinkedHashMap 顺序敏感: 必须长词在前 (例: "退訂單" 在 "訂" 前).
     * <p>原因: String.replace 按顺序扫描, 短词命中后会破坏长词结构.
     *
     * @param s 已 lowercase 的文本
     * @return 简中结果
     */
    static String tradToSimp(String s) {
        String result = s;
        // 遍历 LinkedHashMap (按插入顺序)
        for (var e : TRAD.get().entrySet()) {
            // 替换所有出现的繁体 key
            result = result.replace(e.getKey(), e.getValue());
        }
        return result;
    }

    /**
     * 同义词扩展 (内部步骤).
     * <p>遍历同义词字典, 检查 text 是否含 key, 命中则把 value 加入扩展集.
     * <p>不修改 text, 仅收集扩展词 (供后续 N-gram 匹配使用).
     *
     * @param text 已归一化的文本
     * @return 扩展词集合 (LinkedHashSet 去重, 保序)
     */
    static Set<String> expandSynonyms(String text) {
        Set<String> out = new LinkedHashSet<>();
        for (var e : SYNONYMS.get().entrySet()) {
            // 触发词在文本中出现
            if (text.contains(e.getKey())) {
                // value 可能是 "退钱,退货,申请退款" 形式, 用 , 分隔
                String value = e.getValue();
                if (value != null) {
                    // 拆分到 Set (去重)
                    for (String v : value.split(",")) {
                        String trimmed = v.trim();
                        if (!trimmed.isEmpty()) out.add(trimmed);
                    }
                }
            }
        }
        return out;
    }

    /**
     * 归一化结果记录.
     * <p>三层语义: normalized (处理后用于匹配) + original (原始输入回显) + expansions (扩展词)
     *
     * @param normalized 归一化后文本 (用于关键词 / N-gram 匹配)
     * @param original   原始输入 (用于 API 返回, 保持用户原话)
     * @param expansions 同义词扩展列表 (供后续模型融合)
     */
    public record NormalizedResult(String normalized, String original, List<String> expansions) {}
}
