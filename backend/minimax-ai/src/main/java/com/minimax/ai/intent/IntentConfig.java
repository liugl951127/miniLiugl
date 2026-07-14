package com.minimax.ai.intent;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 意图识别外部化配置 (V3.5.7)
 *
 * <h2>设计目标</h2>
 * 把 IntentPredictionService 里所有硬编码常量从代码搬到 yml, 支持:
 * <ul>
 *   <li>不重新编译就能改业务规则 (运营可调)</li>
 *   <li>不同环境用不同 yml (test / staging / prod)</li>
 *   <li>热更新 (改 yml 不重启服务生效, 需配 Nacos / Spring Cloud Config)</li>
 *   <li>A/B 测试 (不同 yml profile 对比效果)</li>
 * </ul>
 *
 * <h2>YAML 路径</h2>
 * {@code application.yml} 里 {@code intent.*} 段
 *
 * <h2>默认值</h2>
 * {@link #defaults()} 返回 V3.5.6 硬编码的全部数据, 防止 yml 缺配置时 NPE
 *
 * <h2>数据规模</h2>
 * <ul>
 *   <li>9 个意图, 每个 5-15 关键词</li>
 *   <li>30+ 关键短语</li>
 *   <li>40+ 同义词</li>
 *   <li>20 个 benchmark 测试用例 (可扩到 100+)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V3.5.7
 */
@Data
@Component
@ConfigurationProperties(prefix = "intent")
public class IntentConfig {

    /** 算法版本 */
    private String algorithm = "v3.5.6-weighted-voting";
    /** 4 模型权重 (TF / N-gram / 同义词 / 上下文) */
    private double[] weights = {0.4, 0.3, 0.2, 0.1};
    /** sigmoid 置信度缩放因子 */
    private double confidenceScale = 5.0;

    /** Negation 配置 */
    private Negation negation = new Negation();
    /** 紧急度配置 */
    private Urgent urgent = new Urgent();
    /** 情感配置 */
    private Sentiment sentiment = new Sentiment();
    /** Agent 推荐映射 */
    private Map<String, String> agents = new LinkedHashMap<>();
    /** 关键词表 (intent -> { word -> weight }) */
    private Map<String, Map<String, Double>> keywords = new LinkedHashMap<>();
    /** 短语表 (intent -> { phrase -> weight }) */
    private Map<String, Map<String, Double>> phrases = new LinkedHashMap<>();
    /** 同义词字典 (word -> canonical) */
    private Map<String, String> synonyms = new LinkedHashMap<>();
    /** 简繁对照表 */
    private Map<String, String> traditional = new LinkedHashMap<>();
    /** Benchmark 测试用例 */
    private List<BenchmarkCase> benchmark = new ArrayList<>();

    @Data
    public static class Negation {
        /** 作用域大小 (字符) */
        private int scope = 5;
        /** 否定前缀 */
        private List<String> prefixes = List.of(
                "不", "没", "未", "别", "无", "非", "勿", "无法", "不能",
                "no", "not", "n't", "never", "without"
        );
    }

    @Data
    public static class Urgent {
        /** 紧急词 */
        private List<String> words = new ArrayList<>();
        /** 紧急程度副词 (额外加权) */
        private List<String> degree = new ArrayList<>();
    }

    @Data
    public static class Sentiment {
        /** 正面词 */
        private List<String> positive = new ArrayList<>();
        /** 负面词 */
        private List<String> negative = new ArrayList<>();
        /** 程度副词 (word -> 权重系数) */
        private Map<String, Double> degreeWords = new LinkedHashMap<>();
    }

    @Data
    public static class BenchmarkCase {
        private String text;
        private String expected;
    }

    // ═══════════════════════════════════════════════════════════
    // 默认值 (V3.5.6 全部硬编码数据, 防止 NPE)
    // ═══════════════════════════════════════════════════════════

    /**
     * 加载默认值 (跟 V3.5.6 硬编码完全一致, 零行为变化)
     */
    public static IntentConfig defaults() {
        IntentConfig cfg = new IntentConfig();

        // Negation
        cfg.negation = new Negation();
        cfg.negation.setScope(5);
        cfg.negation.setPrefixes(new ArrayList<>(List.of(
                "不", "没", "未", "别", "无", "非", "勿", "无法", "不能",
                "no", "not", "n't", "never", "without"
        )));

        // Urgent
        cfg.urgent = new Urgent();
        cfg.urgent.setWords(new ArrayList<>(List.of(
                "急", "紧急", "马上", "立即", "立刻", "尽快", "asap", "urgent",
                "火", "爆炸", "故障", "宕机", "崩溃", "挂", "卡死", "卡住"
        )));
        cfg.urgent.setDegree(new ArrayList<>(List.of(
                "非常", "特别", "极其", "巨", "超", "超级"
        )));

        // Sentiment
        cfg.sentiment = new Sentiment();
        cfg.sentiment.setPositive(new ArrayList<>(List.of(
                "好", "棒", "赞", "感谢", "谢谢", "满意", "喜欢", "推荐", "优秀", "完美",
                "不错", "给力", "漂亮", "贴心", "迅速"
        )));
        cfg.sentiment.setNegative(new ArrayList<>(List.of(
                "差", "烂", "垃圾", "失望", "愤怒", "投诉", "退款", "没用",
                "坏了", "不对", "错误", "卡顿", "卡", "慢", "渣", "坑", "忽悠"
        )));
        Map<String, Double> deg = new LinkedHashMap<>();
        deg.put("非常", 1.5); deg.put("特别", 1.5);
        deg.put("极其", 1.8); deg.put("巨", 1.5);
        deg.put("很", 1.2); deg.put("挺", 1.1);
        deg.put("有点", 0.7); deg.put("稍微", 0.5);
        deg.put("略", 0.5); deg.put("不太", 0.6);
        cfg.sentiment.setDegreeWords(deg);

        // Agents
        cfg.agents = new LinkedHashMap<>();
        cfg.agents.put("query", "echo-analyzer");
        cfg.agents.put("order", "echo-writer");
        cfg.agents.put("complaint", "echo-reviewer");
        cfg.agents.put("consult", "echo-translator");
        cfg.agents.put("cancel", "echo-writer");
        cfg.agents.put("feedback", "echo-summarizer");
        cfg.agents.put("pay", "echo-reviewer");
        cfg.agents.put("login", "echo-coder");
        cfg.agents.put("register", "echo-coder");

        // Keywords (9 意图)
        cfg.keywords = defaultKeywords();
        // Phrases (9 意图)
        cfg.phrases = defaultPhrases();
        // Synonyms (40+)
        cfg.synonyms = defaultSynonyms();
        // Trad (简繁)
        cfg.traditional = defaultTrad();
        // Benchmark (20 默认, 可在 yml 扩到 100+)
        cfg.benchmark = defaultBenchmark();

        return cfg;
    }

    private static Map<String, Map<String, Double>> defaultKeywords() {
        Map<String, Map<String, Double>> out = new LinkedHashMap<>();
        // query
        Map<String, Double> q = new LinkedHashMap<>();
        q.put("查询", 10.0); q.put("查", 8.0); q.put("看看", 6.0); q.put("显示", 5.0);
        q.put("多少", 7.0); q.put("几个", 5.0); q.put("哪些", 5.0); q.put("有没有", 6.0);
        q.put("select", 5.0); q.put("find", 5.0); q.put("get", 4.0);
        out.put("query", q);
        // order
        Map<String, Double> o = new LinkedHashMap<>();
        o.put("下单", 10.0); o.put("订购", 9.0); o.put("买", 8.0); o.put("购买", 9.0);
        o.put("要", 5.0); o.put("order", 8.0); o.put("buy", 7.0); o.put("purchase", 8.0);
        out.put("order", o);
        // complaint
        Map<String, Double> c = new LinkedHashMap<>();
        c.put("投诉", 10.0); c.put("差评", 9.0); c.put("退款", 8.0); c.put("退货", 8.0);
        c.put("complain", 8.0); c.put("refund", 7.0);
        out.put("complaint", c);
        // consult
        Map<String, Double> co = new LinkedHashMap<>();
        co.put("咨询", 10.0); co.put("请问", 8.0); co.put("问", 5.0); co.put("怎么", 5.0);
        co.put("如何", 5.0); co.put("help", 5.0); co.put("?", 3.0); co.put("？", 3.0);
        out.put("consult", co);
        // cancel
        Map<String, Double> cc = new LinkedHashMap<>();
        cc.put("取消", 10.0); cc.put("撤销", 9.0); cc.put("作废", 8.0); cc.put("不要了", 7.0);
        cc.put("cancel", 8.0); cc.put("abort", 7.0);
        out.put("cancel", cc);
        // feedback
        Map<String, Double> fb = new LinkedHashMap<>();
        fb.put("反馈", 10.0); fb.put("建议", 8.0); fb.put("意见", 7.0); fb.put("希望", 5.0);
        fb.put("feedback", 8.0); fb.put("suggest", 7.0);
        out.put("feedback", fb);
        // pay
        Map<String, Double> p = new LinkedHashMap<>();
        p.put("付款", 10.0); p.put("支付", 10.0); p.put("转账", 8.0); p.put("结账", 9.0);
        p.put("充值", 7.0); p.put("pay", 8.0); p.put("checkout", 8.0);
        out.put("pay", p);
        // login
        Map<String, Double> l = new LinkedHashMap<>();
        l.put("登录", 10.0); l.put("登入", 9.0);
        l.put("signup", 7.0); l.put("login", 8.0);
        out.put("login", l);
        // register
        Map<String, Double> reg = new LinkedHashMap<>();
        reg.put("注册", 10.0); reg.put("开账号", 9.0); reg.put("创建账号", 9.0);
        reg.put("register", 8.0);
        out.put("register", reg);
        return out;
    }

    private static Map<String, Map<String, Double>> defaultPhrases() {
        Map<String, Map<String, Double>> out = new LinkedHashMap<>();
        addPh(out, "order", Map.of(
                "我要买", 12.0, "我想买", 12.0, "帮我买", 11.0,
                "买一下", 9.0, "帮我下", 10.0, "帮我订", 11.0
        ));
        addPh(out, "complaint", Map.of(
                "我要退款", 13.0, "我想退款", 13.0, "退款!", 11.0,
                "退货!", 11.0, "差评!", 11.0, "申请退款", 12.0
        ));
        addPh(out, "query", Map.of(
                "帮我查", 10.0, "帮我看看", 9.0, "有多少", 8.0,
                "查一下", 10.0, "看一下", 7.0
        ));
        addPh(out, "consult", Map.of(
                "怎么用", 9.0, "怎么办", 9.0, "如何用", 9.0, "怎么操作", 10.0
        ));
        addPh(out, "cancel", Map.of(
                "不要了", 12.0, "算了吧", 10.0, "取消吧", 11.0, "我撤销", 11.0
        ));
        addPh(out, "pay", Map.of(
                "我付款", 11.0, "去支付", 11.0, "完成支付", 12.0, "去结账", 11.0
        ));
        addPh(out, "login", Map.of(
                "我要登录", 12.0, "怎么登录", 11.0, "无法登录", 13.0
        ));
        addPh(out, "register", Map.of(
                "我要注册", 12.0, "怎么注册", 11.0, "新账号", 10.0
        ));
        return out;
    }

    private static void addPh(Map<String, Map<String, Double>> out, String intent, Map<String, Double> data) {
        out.put(intent, new LinkedHashMap<>(data));
    }

    private static Map<String, String> defaultSynonyms() {
        Map<String, String> out = new LinkedHashMap<>();
        // 退款
        out.put("退钱", "退款"); out.put("返钱", "退款");
        out.put("退回", "退款"); out.put("退订", "退款");
        out.put("refund", "退款"); out.put("chargeback", "退款");
        // 订单
        out.put("dingdan", "订单"); out.put("order", "订单");
        // 支付
        out.put("结账", "支付"); out.put("付款", "支付");
        out.put("pay", "支付"); out.put("checkout", "支付");
        out.put("转账", "支付"); out.put("充值", "支付");
        // 投诉
        out.put("差评", "投诉"); out.put("抱怨", "投诉");
        out.put("complain", "投诉"); out.put("举报", "投诉");
        // 怎么
        out.put("咋办", "怎么办"); out.put("咋整", "怎么办");
        out.put("how to", "怎么"); out.put("how", "怎么");
        // 下单
        out.put("买", "下单"); out.put("买一下", "下单");
        out.put("buy", "下单"); out.put("purchase", "下单");
        // 取消
        out.put("不要了", "取消"); out.put("算了吧", "取消");
        out.put("撤销", "取消"); out.put("abort", "取消");
        // 登录
        out.put("登入", "登录"); out.put("登陆", "登录");
        out.put("signin", "登录"); out.put("signup", "注册");
        // 注册
        out.put("注册账号", "注册"); out.put("创建账号", "注册");
        out.put("开账号", "注册"); out.put("register", "注册");
        return out;
    }

    private static Map<String, String> defaultTrad() {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("訊息", "信息"); out.put("訊", "信");
        out.put("檔案", "文件"); out.put("檔", "文");
        out.put("網路", "网络"); out.put("網", "网");
        out.put("資料", "数据"); out.put("資", "资");
        out.put("訂單", "订单"); out.put("訂", "订");
        out.put("付款", "付款"); out.put("賬", "账");
        out.put("個", "个"); out.put("貨", "货");
        out.put("銀", "银"); out.put("聯", "联");
        return out;
    }

    private static List<BenchmarkCase> defaultBenchmark() {
        List<BenchmarkCase> out = new ArrayList<>();
        String[][] cases = {
            {"查询一下订单状态", "query"},
            {"我想买 10 台服务器", "order"},
            {"我要退款, 差评, 紧急!", "complaint"},
            {"紧急! 马上修复!!", "complaint"},
            {"13800138000 邮箱 test@example.com 支付 100 元, 明天发货", "pay"},
            {"非常棒, 谢谢!", "feedback"},
            {"我要付款 100 元", "pay"},
            {"查询订单状态", "query"},
            {"怎么登录?", "consult"},
            {"我要注册账号", "register"},
            {"帮我取消订单", "cancel"},
            {"退货!", "complaint"},
            {"购买 5 个 iPhone", "order"},
            {"不满意, 退款", "complaint"},
            {"非常感谢, 帮大忙了", "feedback"},
            {"dingdan zhuangtai", "query"},
            {"i want refund", "complaint"},
            {"无法登录怎么办?", "login"},
            {"帮我看看数据", "query"},
            {"下单 100 元", "order"}
        };
        for (String[] c : cases) {
            BenchmarkCase bc = new BenchmarkCase();
            bc.setText(c[0]);
            bc.setExpected(c[1]);
            out.add(bc);
        }
        return out;
    }
}
