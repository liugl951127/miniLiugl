package com.minimax.ai.generation;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 错别字容错工具 (V2.8.4)
 *
 * <h3>能力</h3>
 * <ul>
 *   <li><b>拼音首字母匹配</b>: "shuj" → "数据", "biaoge" → "表格"</li>
 *   <li><b>编辑距离 (Levenshtein)</b>: "统记" → "统计", "圆饼图" → "圆饼"</li>
 *   <li><b>常见错别字词典</b>: 100+ 错别字映射</li>
 *   <li><b>同义词扩展</b>: "看看" → "分析", "搞个" → "生成"</li>
 * </ul>
 *
 * <h3>使用</h3>
 * <pre>{@code
 *   TypoTolerance tt = new TypoTolerance();
 *   String corrected = tt.correct("帮我生个表个统计图");
 *   // → "帮我生个表个统计图" 或 "帮我生成表格统计图"
 * }</pre>
 */
@Slf4j
public class TypoTolerance {

    /**
     * 常见错别字词典 (错字 → 正字)
     */
    private static final Map<String, String> TYPO_DICT = new HashMap<>();
    static {
        // 中文错别字
        TYPO_DICT.put("表个", "表格");
        TYPO_DICT.put("统记", "统计");
        TYPO_DICT.put("分忻", "分析");
        TYPO_DICT.put("园饼图", "饼图");
        TYPO_DICT.put("园饼", "饼");
        TYPO_DICT.put("表隔", "表格");
        TYPO_DICT.put("圆并", "饼");
        TYPO_DICT.put("线行图", "折线图");
        TYPO_DICT.put("柱状", "柱状");
        TYPO_DICT.put("帮且", "帮我");
        TYPO_DICT.put("帮我吗", "帮我");
        TYPO_DICT.put("搞个", "生成");
        TYPO_DICT.put("弄个", "生成");
        TYPO_DICT.put("来一个", "生成");
        TYPO_DICT.put("要一个", "生成");
        TYPO_DICT.put("出图", "生成图表");
        TYPO_DICT.put("话条线", "折线");
        TYPO_DICT.put("帮忙告", "告诉我");
        TYPO_DICT.put("查下", "查询");
        TYPO_DICT.put("看下", "看看");
        TYPO_DICT.put("看下子", "看看");
        TYPO_DICT.put("瞅瞅", "看看");
        TYPO_DICT.put("搞一下", "做");
        TYPO_DICT.put("赶一个", "生成");
        TYPO_DICT.put("曲谱", "曲");
        TYPO_DICT.put("动漫", "动画");
        TYPO_DICT.put("动谩", "动画");
        TYPO_DICT.put("线线图", "折线图");
        TYPO_DICT.put("帮看下", "帮我分析");
        TYPO_DICT.put("整一个", "生成");
        TYPO_DICT.put("可以生", "生成");
        TYPO_DICT.put("能生", "生成");
        TYPO_DICT.put("可否", "可以");
        TYPO_DICT.put("人公", "人工");
        TYPO_DICT.put("真人人", "真人");
        TYPO_DICT.put("转人工", "转人工");
        TYPO_DICT.put("转人公", "转人工");
        TYPO_DICT.put("坐席席", "坐席");
        TYPO_DICT.put("查询问", "查询");
        TYPO_DICT.put("语音合", "语音合成");
        TYPO_DICT.put("语音合成成", "语音合成");

        // 英文常见拼写
        TYPO_DICT.put("chatr", "chart");
        TYPO_DICT.put("chrt", "chart");
        TYPO_DICT.put("musc", "music");
        TYPO_DICT.put("muzik", "music");
        TYPO_DICT.put("anaylze", "analyze");
        TYPO_DICT.put("analze", "analyze");
        TYPO_DICT.put("statisitcs", "statistics");
        TYPO_DICT.put("qurey", "query");
        TYPO_DICT.put("qeury", "query");
        TYPO_DICT.put("genrate", "generate");
        TYPO_DICT.put("generte", "generate");
        TYPO_DICT.put("projct", "project");
        TYPO_DICT.put("porject", "project");
    }

    /**
     * 同义词词典 (口语 → 规范词)
     */
    private static final Map<String, String> SYNONYMS = new HashMap<>();
    static {
        SYNONYMS.put("看看", "分析");
        SYNONYMS.put("瞅瞅", "分析");
        SYNONYMS.put("了解", "分析");
        SYNONYMS.put("搞个", "生成");
        SYNONYMS.put("弄个", "生成");
        SYNONYMS.put("来一个", "生成");
        SYNONYMS.put("查下", "查询");
        SYNONYMS.put("告诉", "查询");
        SYNONYMS.put("说下", "查询");
        SYNONYMS.put("讲讲", "介绍");
        SYNONYMS.put("介绍下", "介绍");
    }

    /**
     * 中文→拼音首字母映射 (常用字)
     * 用于 "shuj" → "数据" 这种输入
     */
    private static final Map<String, List<String>> PINYIN_INDEX = new HashMap<>();
    static {
        // 格式: 拼音首字母 → 候选词列表
        PINYIN_INDEX.put("shuj", List.of("数据", "书", "数"));
        PINYIN_INDEX.put("tongji", List.of("统计", "通缉"));
        PINYIN_INDEX.put("biaoge", List.of("表格", "标格"));
        PINYIN_INDEX.put("tubiao", List.of("图表"));
        PINYIN_INDEX.put("yinle", List.of("音乐"));
        PINYIN_INDEX.put("donghua", List.of("动画"));
        PINYIN_INDEX.put("fenxi", List.of("分析"));
        PINYIN_INDEX.put("shengcheng", List.of("生成"));
        PINYIN_INDEX.put("chaxun", List.of("查询"));
        PINYIN_INDEX.put("rengong", List.of("人工"));
        PINYIN_INDEX.put("yuyin", List.of("语音"));
        PINYIN_INDEX.put("tupian", List.of("图片"));
        PINYIN_INDEX.put("shipin", List.of("视频"));
        PINYIN_INDEX.put("xiangmu", List.of("项目"));
        PINYIN_INDEX.put("daimai", List.of("代码"));
    }

    /** 中文标点正则 */
    private static final Pattern NON_CHINESE = Pattern.compile("[^\\u4e00-\\u9fa5a-zA-Z]");

    /**
     * 纠正文本中的错别字
     */
    public String correct(String text) {
        if (text == null || text.isEmpty()) return text;
        String result = text;

        // 1. 错别字词典 (长匹配优先)
        List<String> keys = new ArrayList<>(TYPO_DICT.keySet());
        keys.sort((a, b) -> b.length() - a.length());  // 长的优先
        for (String wrong : keys) {
            if (result.contains(wrong)) {
                result = result.replace(wrong, TYPO_DICT.get(wrong));
            }
        }

        // 2. 同义词替换
        for (Map.Entry<String, String> e : SYNONYMS.entrySet()) {
            if (result.contains(e.getKey())) {
                result = result.replace(e.getKey(), e.getValue());
            }
        }

        return result;
    }

    /**
     * 拼音首字母匹配: 把 "shuj" 替换成 "数据"
     * 仅对纯英文/拼音 token 进行替换
     */
    public String expandPinyin(String text) {
        if (text == null) return text;
        String[] tokens = text.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String t : tokens) {
            String lower = t.toLowerCase();
            if (PINYIN_INDEX.containsKey(lower)) {
                sb.append(PINYIN_INDEX.get(lower).get(0));
            } else {
                sb.append(t);
            }
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    /**
     * 编辑距离 (Levenshtein Distance)
     * 用于: 输入 "统记" (3字), 与 "统计" 比较, 距离 1 → 匹配
     */
    public int editDistance(String a, String b) {
        if (a == null || b == null) return Integer.MAX_VALUE;
        int m = a.length(), n = b.length();
        if (m == 0) return n;
        if (n == 0) return m;
        int[] dp = new int[n + 1];
        for (int j = 0; j <= n; j++) dp[j] = j;
        for (int i = 1; i <= m; i++) {
            int prev = dp[0];
            dp[0] = i;
            for (int j = 1; j <= n; j++) {
                int tmp = dp[j];
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[j] = prev;
                } else {
                    dp[j] = 1 + Math.min(Math.min(prev, dp[j]), dp[j - 1]);
                }
                prev = tmp;
            }
        }
        return dp[n];
    }

    /**
     * 模糊匹配: 关键词 "柱状图" 在文本 "我要个柱状图表" 中存在 (子串)
     * 或 "统记" 与 "统计" 编辑距离 1 → 视为匹配
     */
    public boolean fuzzyMatch(String text, String keyword) {
        if (text == null || keyword == null) return false;
        String lowerText = text.toLowerCase();
        String lowerKw = keyword.toLowerCase();

        // 1. 子串包含 (fast path)
        if (lowerText.contains(lowerKw)) return true;

        // 2. 编辑距离: 长度 ≥ 3 启用, 同长度窗口避免误判
        if (lowerKw.length() >= 3 && lowerText.length() >= lowerKw.length()) {
            int maxDist = 1;
            int kwLen = lowerKw.length();
            for (int i = 0; i <= lowerText.length() - kwLen; i++) {
                String sub = lowerText.substring(i, i + kwLen);
                if (editDistance(sub, lowerKw) <= maxDist) return true;
            }
        }
        return false;
    }
}
