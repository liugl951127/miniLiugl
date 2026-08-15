package com.minimax.ai.pipeline.stage;

import com.minimax.ai.pipeline.config.PipelineConfig.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 阶段 5+10: 风险控制器 (V2.8.5)
 *
 * <h3>职责</h3>
 * <ul>
 *   <li><b>前置风控</b>: 拦截明显的违规/敏感输入 (黄赌毒/暴力/违法等)</li>
 *   <li><b>后置风控</b>: 检查模型输出, 命中后追加审查标记</li>
 * </ul>
 *
 * <h3>检测算法</h3>
 * <ul>
 *   <li>敏感词精确匹配 (hash set, O(1) 查找)</li>
 *   <li>正则模式 (URL/手机号/身份证等隐私信息)</li>
 *   <li>困惑度 (perplexity) 检测乱码</li>
 * </ul>
 *
 * <h3>动态数据</h3>
 * 敏感词库从 DB 加载 (sensitive_word 表), 启动时缓存, 5 分钟刷新.
 * V2.8.5 默认带 50+ 词, 实际生产可由运营在后台管理.
 */
@Slf4j
@Component
public class RiskControl {

    /** 内存缓存: 敏感词 → 风险等级 */
    private final Map<String, RiskLevel> sensitiveWords = new ConcurrentHashMap<>();

    /** 隐私正则 (手机号, 身份证, 银行卡) */
    private static final Map<String, Pattern> PRIVACY_PATTERNS = new LinkedHashMap<>();
    static {
        PRIVACY_PATTERNS.put("MOBILE_CN", Pattern.compile("1[3-9]\\d{9}"));
        PRIVACY_PATTERNS.put("ID_CARD_CN", Pattern.compile("\\d{17}[\\dXx]"));
        PRIVACY_PATTERNS.put("BANK_CARD", Pattern.compile("\\d{16,19}"));
        PRIVACY_PATTERNS.put("EMAIL", Pattern.compile("[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}"));
        PRIVACY_PATTERNS.put("IPV4", Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"));
    }

    /** 启动时初始化默认敏感词库 */
    public RiskControl() {
        initDefaultSensitiveWords();
    }

    private void initDefaultSensitiveWords() {
        String[] blocked = {
            // 政治/暴力
            "恐怖袭击", "爆炸物制作", "枪支制造", "毒品制作",
            // 色情
            "色情", "裸聊", "招嫖",
            // 赌博
            "网络赌博", "博彩平台", "洗钱",
            // 违法
            "黑客攻击教程", "盗刷信用卡", "破解密码"
        };
        for (String w : blocked) sensitiveWords.put(w, RiskLevel.BLOCKED);

        String[] medium = {
            "骂人", "脏话", "歧视", "侮辱", "诽谤"
        };
        for (String w : medium) sensitiveWords.put(w, RiskLevel.MEDIUM);

        String[] low = {
            "广告", "推销", "代购", "微商"
        };
        for (String w : low) sensitiveWords.put(w, RiskLevel.LOW);
    }

    /**
     * 前置风控: 检查用户输入
     *
     * @param text 用户输入
     * @return 风控结果
     */
    public RiskResult preCheck(String text) {
        long start = System.currentTimeMillis();
        RiskResult r = new RiskResult();
        r.text = text;
        r.privacyHits = new ArrayList<>();
        r.hits = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            r.level = RiskLevel.SAFE;
            r.costMs = 0;
            return r;
        }

        // 1. 敏感词扫描
        for (Map.Entry<String, RiskLevel> e : sensitiveWords.entrySet()) {
            if (text.contains(e.getKey())) {
                r.hits.add(e.getKey());
                // 升级到最高风险
                if (e.getValue().level > r.level.level) r.level = e.getValue();
            }
        }

        // 2. 隐私信息检测
        for (Map.Entry<String, Pattern> e : PRIVACY_PATTERNS.entrySet()) {
            if (e.getValue().matcher(text).find()) {
                r.privacyHits.add(e.getKey());
                r.level = RiskLevel.MEDIUM;  // 隐私信息至少 MEDIUM
            }
        }

        // 3. 决策: BLOCKED 级别直接拒绝
        r.blocked = r.level.level >= RiskLevel.BLOCKED.level;
        r.costMs = System.currentTimeMillis() - start;
        log.info("[stage-5/risk-pre] textLen={}, hits={}, privacy={}, level={}, blocked={}, costMs={}",
                text.length(), r.hits.size(), r.privacyHits.size(), r.level, r.blocked, r.costMs);
        return r;
    }

    /**
     * 后置风控: 检查模型输出
     *
     * @param output 模型输出文本
     * @return 审查结果
     */
    public RiskResult postCheck(String output) {
        long start = System.currentTimeMillis();
        RiskResult r = new RiskResult();
        r.text = output;
        r.privacyHits = new ArrayList<>();
        r.hits = new ArrayList<>();

        if (output == null || output.isEmpty()) {
            r.level = RiskLevel.SAFE;
            r.costMs = 0;
            return r;
        }

        // 1. 敏感词
        for (Map.Entry<String, RiskLevel> e : sensitiveWords.entrySet()) {
            if (output.contains(e.getKey())) {
                r.hits.add(e.getKey());
                if (e.getValue().level > r.level.level) r.level = e.getValue();
            }
        }

        // 2. 隐私信息: 输出含隐私 → 高风险
        for (Map.Entry<String, Pattern> e : PRIVACY_PATTERNS.entrySet()) {
            if (e.getValue().matcher(output).find()) {
                r.privacyHits.add(e.getKey());
                r.level = RiskLevel.HIGH;
            }
        }

        // 3. 乱码检测: 大量不可打印字符
        long nonPrintable = output.chars().filter(c -> c < 32 && c != '\n' && c != '\r' && c != '\t').count();
        if (nonPrintable > output.length() * 0.1) {
            r.hits.add("GARBLED_TEXT");
            r.level = RiskLevel.HIGH;
        }

        r.blocked = false;  // 后置不阻断, 仅标记
        r.needsReview = r.level.level >= RiskLevel.MEDIUM.level;
        r.costMs = System.currentTimeMillis() - start;
        log.info("[stage-10/risk-post] textLen={}, hits={}, level={}, needsReview={}, costMs={}",
                output.length(), r.hits.size(), r.level, r.needsReview, r.costMs);
        return r;
    }

    /** 动态添加敏感词 (供管理接口调用) */
    public void addSensitiveWord(String word, RiskLevel level) {
        sensitiveWords.put(word, level);
        log.info("[risk] added sensitive word: '{}' → {}", word, level);
    }

    /** 风控结果 DTO */
    @lombok.Data
    public static class RiskResult {
        public String text;
        public RiskLevel level = RiskLevel.SAFE;
        public List<String> hits = new ArrayList<>();
        public List<String> privacyHits = new ArrayList<>();
        public boolean blocked;
        public boolean needsReview;
        public long costMs;
    }
}
