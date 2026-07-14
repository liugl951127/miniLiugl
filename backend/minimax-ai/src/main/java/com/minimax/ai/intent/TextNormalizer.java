package com.minimax.ai.intent;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 文本归一化工具 (V3.5.6 算法升级新增)
 *
 * <h2>为什么需要归一化</h2>
 * 用户输入千奇百怪, 直接做关键词匹配会大量漏检:
 * <ul>
 *   <li>全角 vs 半角: "ＡＢＣ" vs "ABC"</li>
 *   <li>简繁: "信息" vs "訊息"</li>
 *   <li>同义词: "退款" vs "退钱" vs "返钱" vs "退回"</li>
 *   <li>口语化: "咋办" vs "怎么办"</li>
 *   <li>拼写错误: "dingdan" vs "订单"</li>
 * </ul>
 * 归一化 = 把这些变体映射到统一表示, 让下游匹配能命中。
 *
 * <h2>5 步归一化流水线</h2>
 * <ol>
 *   <li>大小写归一 (lowercase)</li>
 *   <li>全角 → 半角 (全角字符 ASCII 范围内)</li>
 *   <li>简繁 → 简 (OpenCC 风格常用字对照表)</li>
 *   <li>同义词扩展 (查表)</li>
 *   <li>去除冗余空格/标点</li>
 * </ol>
 *
 * <h2>复杂度</h2>
 * O(N*L) 其中 N=文本长度, L=同义词表每词查表次数
 *
 * @author MiniMax
 * @since V3.5.6
 */
public final class TextNormalizer {

    private TextNormalizer() {}

    /** 全角 -> 半角 转换表 (ASCII 范围 0x21~0x7E) */
    private static final char[] FULL_WIDTH_OFFSETS = new char[0xFF60];
    static {
        Arrays.fill(FULL_WIDTH_OFFSETS, (char) 0);
        for (int i = 0xFF01; i <= 0xFF5E; i++) {
            FULL_WIDTH_OFFSETS[i] = (char) (i - 0xFEE0);
        }
    }

    /** 简繁对照表 (精简常用字, 避免引入 1MB+ OpenCC 字典) */
    private static final Map<String, String> TRAD_TO_SIMP = Map.ofEntries(
        Map.entry("訊息", "信息"), Map.entry("訊", "信"),
        Map.entry("檔案", "文件"), Map.entry("檔", "文"),
        Map.entry("網路", "网络"), Map.entry("網", "网"),
        Map.entry("資料", "数据"), Map.entry("資", "资"),
        Map.entry("訂單", "订单"), Map.entry("訂", "订"),
        Map.entry("付款", "付款"), Map.entry("賬", "账"),
        Map.entry("個", "个"), Map.entry("貨", "货"),
        Map.entry("銀", "银"), Map.entry("聯", "联")
    );

    /** 同义词字典: 词 -> 标准词 */
    private static final Map<String, String> SYNONYMS = Map.ofEntries(
        // 退款
        Map.entry("退钱", "退款"), Map.entry("返钱", "退款"),
        Map.entry("退回", "退款"), Map.entry("退订", "退款"),
        Map.entry("refund", "退款"), Map.entry("chargeback", "退款"),
        // 订单
        Map.entry("dingdan", "订单"), Map.entry("order", "订单"),
        // 支付
        Map.entry("结账", "支付"), Map.entry("付款", "支付"),
        Map.entry("pay", "支付"), Map.entry("checkout", "支付"),
        Map.entry("转账", "支付"), Map.entry("充值", "支付"),
        // 投诉
        Map.entry("差评", "投诉"), Map.entry("抱怨", "投诉"),
        Map.entry("complain", "投诉"), Map.entry("举报", "投诉"),
        // 怎么
        Map.entry("咋办", "怎么办"), Map.entry("咋整", "怎么办"),
        Map.entry("how to", "怎么"), Map.entry("how", "怎么"),
        // 下单
        Map.entry("买", "下单"), Map.entry("买一下", "下单"),
        Map.entry("buy", "下单"), Map.entry("purchase", "下单"),
        // 取消
        Map.entry("不要了", "取消"), Map.entry("算了吧", "取消"),
        Map.entry("撤销", "取消"), Map.entry("abort", "取消"),
        // 登录
        Map.entry("登入", "登录"), Map.entry("登陆", "登录"),
        Map.entry("signin", "登录"), Map.entry("signup", "注册"),
        // 注册
        Map.entry("注册账号", "注册"), Map.entry("创建账号", "注册"),
        Map.entry("开账号", "注册"), Map.entry("register", "注册")
    );

    /** 多余空白 (包括全角空格) */
    private static final Pattern EXTRA_SPACE = Pattern.compile("[\\s\\u3000]+");
    /** 标点 */
    private static final Pattern PUNCT = Pattern.compile("[\\p{Punct}，。！？、；：]");
    private static final Pattern REPEATED = Pattern.compile("(.)\\1{2,}"); // aaa -> a

    /**
     * 主入口: 5 步归一化
     *
     * @param text 原始文本
     * @return 归一化后文本 (以及同义词扩展列表)
     */
    public static NormalizedResult normalize(String text) {
        if (text == null || text.isBlank()) {
            return new NormalizedResult("", "", List.of());
        }
        String t = text.trim();

        // 1. 全角 -> 半角
        t = fullWidthToHalfWidth(t);
        // 2. 大小写归一
        t = t.toLowerCase(Locale.ROOT);
        // 3. 简繁 -> 简
        t = tradToSimp(t);
        // 4. 同义词扩展
        Set<String> expansions = expandSynonyms(t);
        // 5. 清理多余字符
        t = EXTRA_SPACE.matcher(t).replaceAll(" ").trim();
        // 6. 折叠重复字符 (哈哈哈哈哈 -> 哈哈)
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
            } else if (c == 0x3000) {  // 全角空格
                chars[i] = ' ';
            }
        }
        return new String(chars);
    }

    /** 简繁 (用简易对照表) */
    static String tradToSimp(String s) {
        String result = s;
        for (var e : TRAD_TO_SIMP.entrySet()) {
            result = result.replace(e.getKey(), e.getValue());
        }
        return result;
    }

    /** 同义词扩展: 把同义词也加入候选 token 集 */
    static Set<String> expandSynonyms(String text) {
        Set<String> out = new LinkedHashSet<>();
        for (var e : SYNONYMS.entrySet()) {
            if (text.contains(e.getKey())) {
                out.add(e.getValue());
            }
        }
        return out;
    }

    /** 归一化结果: 含原文 + 归一化 + 扩展词集合 */
    public record NormalizedResult(String normalized, String original, List<String> expansions) {}
}
