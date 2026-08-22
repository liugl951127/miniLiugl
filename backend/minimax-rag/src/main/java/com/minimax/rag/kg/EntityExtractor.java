// =============================================================
// MiniMax - RAG KB 实体抽取服务 (V7.3)
//
// 思路:
//   1) 用 SmartChineseAnalyzer (lucene-analyzers-smartcn) 对 KB 文档分词
//   2) 启发式抽取实体:
//      - 长度 2-6 字符的中文 token
//      - 过滤停用词 / 单字符 / 纯数字
//      - 跨文档 TF >= 阈值 (默认 3) 视为实体
//   3) 同一段/同一句出现 2 个实体, 加 CO_OCCUR 边 (权重 = 共现次数)
//   4) 写库 kb_extracted_entity / kb_extracted_relation
//
// 输入: kbId
// 输出: BuildResult { entities, relations, docCount, elapsedMs }
//
// 关联 kg_entity (按 name 跨 KB 共享): 不在本次 Service 内做 (kg_entity 在 agent 模块),
// 后续可由 AgentKgService 反向同步; 这里只把抽取结果写到本 KB 专用表.
//
// @author general
// @since 2026-08-22
// =============================================================

package com.minimax.rag.kg;

import com.minimax.rag.entity.Document;
import com.minimax.rag.kg.entity.KbExtractedEntity;
import com.minimax.rag.kg.entity.KbExtractedRelation;
import com.minimax.rag.kg.mapper.KbExtractedEntityMapper;
import com.minimax.rag.kg.mapper.KbExtractedRelationMapper;
import com.minimax.rag.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntityExtractor {

    /** SmartChinese 中文分词器 (lucene-analyzers-smartcn 8.11.0) */
    private final Analyzer analyzer = new SmartChineseAnalyzer();

    private final DocumentMapper docMapper;
    private final KbExtractedEntityMapper entityMapper;
    private final KbExtractedRelationMapper relationMapper;

    /** 长度阈值 */
    private static final int MIN_TOKEN_LEN = 2;
    private static final int MAX_TOKEN_LEN = 6;
    /** 最低频率阈值 (低于此频次的 token 不视为实体) */
    private static final int MIN_FREQ = 3;
    /** 句子切分正则 */
    private static final Pattern SENT_SPLIT = Pattern.compile("[。！？!?;；\\n]+");
    /** 段落切分正则 (双换行) */
    private static final Pattern PARA_SPLIT = Pattern.compile("\\n\\s*\\n");

    /** 停用词: 数字 / 单字符 / 标点 / 高频虚词 */
    private static final Set<String> STOPWORDS = Set.of(
            "的", "了", "和", "是", "在", "我", "有", "不", "这", "也", "就", "都",
            "而", "及", "与", "或", "个", "些", "会", "可", "能", "要", "为",
            "由", "从", "到", "向", "对", "把", "让", "使", "给", "其", "之", "者",
            "等", "以", "上", "下", "中", "内", "外", "前", "后", "里", "所", "并",
            "但", "却", "若", "如", "则", "因", "此", "那", "哪", "谁", "何", "怎",
            "为什么", "怎么", "如何", "可以", "应该", "可能", "已经", "正在", "一个",
            "一些", "我们", "你们", "他们", "它们", "自己", "没有", "不是", "就是",
            "但是", "因为", "所以", "如果", "虽然", "然后", "现在", "今天", "明天",
            "去年", "今年", "明年", "一种", "一定", "一直", "一样"
    );

    /**
     * 对单个 KB 跑实体抽取流程, 写库, 返回构建结果.
     *
     * @param kbId   知识库 ID
     * @param config 抽取配置 (可为 null, 使用默认)
     */
    @Transactional
    public BuildResult build(Long kbId, ExtractConfig config) {
        long t0 = System.currentTimeMillis();
        if (kbId == null) throw new IllegalArgumentException("kbId 不能为空");
        ExtractConfig cfg = config == null ? ExtractConfig.defaults() : config;

        // 1) 拉文档
        List<Document> docs = docMapper.selectContentByKb(kbId, 0);
        log.info("[EntityExtractor] kbId={} 文档数={} 配置={}", kbId, docs.size(), cfg);

        if (docs.isEmpty()) {
            // 清空该 KB 旧数据
            relationMapper.deleteByKb(kbId);
            entityMapper.deleteByKb(kbId);
            return new BuildResult(0, 0, 0, System.currentTimeMillis() - t0);
        }

        // 2) 全文分词, 累计 TF
        Map<String, Integer> tf = new HashMap<>();
        // 记录每个 doc 包含的实体 token 集合 (用于共现)
        Map<Long, Set<String>> docTokens = new HashMap<>();
        // 句子级 token (用于 CO_OCCUR 边)
        Map<Long, List<List<String>>> docSentences = new HashMap<>();

        for (Document d : docs) {
            String content = d.getContent();
            if (content == null || content.isBlank()) continue;
            List<String> docTokensList = tokenize(content);
            Set<String> uniq = new HashSet<>(docTokensList);
            for (String t : uniq) {
                tf.merge(t, 1, Integer::sum);
            }
            docTokens.put(d.getId(), uniq);
            docSentences.put(d.getId(), splitSentences(content, docTokensList));
        }

        // 3) 频率阈值过滤 → 实体集
        int minFreq = Math.max(1, cfg.minFreq);
        Map<String, Integer> entityFreq = tf.entrySet().stream()
                .filter(e -> e.getValue() >= minFreq)
                .filter(e -> isValidEntity(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        // 4) 清理旧数据
        relationMapper.deleteByKb(kbId);
        entityMapper.deleteByKb(kbId);

        // 5) 写实体
        List<KbExtractedEntity> savedEntities = new ArrayList<>();
        for (Map.Entry<String, Integer> e : entityFreq.entrySet()) {
            KbExtractedEntity ee = new KbExtractedEntity();
            ee.setKbId(kbId);
            ee.setName(e.getKey());
            ee.setType(inferType(e.getKey()));
            ee.setFreq(e.getValue());
            // 选一个 source doc (该实体出现频次最高的)
            Long srcDoc = pickSourceDoc(e.getKey(), docTokens);
            ee.setSourceDocId(srcDoc);
            entityMapper.insert(ee);
            savedEntities.add(ee);
        }

        // 6) 句子级共现 → CO_OCCUR 边 (同句 2 实体配对)
        Map<String, Integer> edgeWeight = new HashMap<>();
        for (List<List<String>> sentences : docSentences.values()) {
            for (List<String> sent : sentences) {
                // 只保留属于实体集的 token
                List<String> ents = sent.stream()
                        .filter(entityFreq::containsKey)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());
                // 两两配对
                for (int i = 0; i < ents.size(); i++) {
                    for (int j = i + 1; j < ents.size(); j++) {
                        String key = ents.get(i) + "\u0001" + ents.get(j);
                        edgeWeight.merge(key, 1, Integer::sum);
                    }
                }
            }
        }

        // 7) 写关系
        List<KbExtractedRelation> savedRels = new ArrayList<>();
        for (Map.Entry<String, Integer> e : edgeWeight.entrySet()) {
            String[] parts = e.getKey().split("\u0001", 2);
            if (parts.length != 2) continue;
            // 权重阈值: 共现至少 1 次
            if (e.getValue() < 1) continue;
            KbExtractedRelation rr = new KbExtractedRelation();
            rr.setKbId(kbId);
            rr.setSrcEntity(parts[0]);
            rr.setRel("CO_OCCUR");
            rr.setTgtEntity(parts[1]);
            rr.setWeight(e.getValue());
            relationMapper.insert(rr);
            savedRels.add(rr);
        }

        long elapsed = System.currentTimeMillis() - t0;
        log.info("[EntityExtractor] kbId={} 抽取完成: entities={} relations={} elapsed={}ms",
                kbId, savedEntities.size(), savedRels.size(), elapsed);
        return new BuildResult(savedEntities.size(), savedRels.size(), docs.size(), elapsed);
    }

    /**
     * 整句分词 (SmartChineseAnalyzer 过滤停用词 + 长度).
     */
    public List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        try (TokenStream ts = analyzer.tokenStream("kb", new StringReader(text))) {
            CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
            ts.reset();
            while (ts.incrementToken()) {
                String t = term.toString().trim();
                if (isValidEntity(t) && !STOPWORDS.contains(t)) {
                    out.add(t);
                }
            }
            ts.end();
        } catch (IOException e) {
            log.warn("分词失败, 回退到简单字符切分: {}", e.getMessage());
        }
        return out;
    }

    /**
     * 把文档按句子切分, 并对每句独立分词.
     */
    private List<List<String>> splitSentences(String content, List<String> allTokens) {
        if (content == null || content.isEmpty()) return Collections.emptyList();
        String[] sents = SENT_SPLIT.split(content);
        List<List<String>> out = new ArrayList<>();
        for (String s : sents) {
            if (s == null || s.isBlank()) continue;
            List<String> toks = tokenize(s);
            if (!toks.isEmpty()) out.add(toks);
        }
        return out;
    }

    /**
     * 实体判定: 中文 token, 长度 2-6, 不含标点/数字/英文.
     */
    private boolean isValidEntity(String t) {
        if (t == null || t.isEmpty()) return false;
        int len = t.length();
        if (len < MIN_TOKEN_LEN || len > MAX_TOKEN_LEN) return false;
        // 至少含一个中文字符
        boolean hasChinese = false;
        for (int i = 0; i < len; i++) {
            char c = t.charAt(i);
            if (Character.isLetterOrDigit(c) && !isChinese(c)) {
                // 含英文/数字
                return false;
            }
            if (isChinese(c)) hasChinese = true;
        }
        return hasChinese;
    }

    private static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION;
    }

    /**
     * 启发式实体类型推断.
     * 真实场景应使用 NER 模型, 这里用规则兜底 (避免 null).
     */
    private String inferType(String name) {
        if (name == null || name.isEmpty()) return "CONCEPT";
        // 常见 PERSON 关键词
        if (name.endsWith("先生") || name.endsWith("女士")
                || name.endsWith("教授") || name.endsWith("博士")
                || name.endsWith("总统") || name.endsWith("总理")
                || name.endsWith("主席") || name.endsWith("经理")) {
            return "PERSON";
        }
        // 常见 ORG 关键词
        if (name.endsWith("公司") || name.endsWith("集团")
                || name.endsWith("大学") || name.endsWith("学院")
                || name.endsWith("研究所") || name.endsWith("实验室")
                || name.endsWith("医院") || name.endsWith("学校")) {
            return "ORG";
        }
        // 常见 PLACE
        if (name.endsWith("省") || name.endsWith("市")
                || name.endsWith("区") || name.endsWith("县")
                || name.endsWith("国") || name.endsWith("州")) {
            return "PLACE";
        }
        // 常见 PRODUCT
        if (name.endsWith("手机") || name.endsWith("电脑")
                || name.endsWith("汽车") || name.endsWith("产品")
                || name.endsWith("系统") || name.endsWith("软件")) {
            return "PRODUCT";
        }
        return "CONCEPT";
    }

    /**
     * 选一个代表 source doc (该实体在其出现频次最高).
     */
    private Long pickSourceDoc(String name, Map<Long, Set<String>> docTokens) {
        Long best = null;
        int bestCnt = 0;
        for (Map.Entry<Long, Set<String>> e : docTokens.entrySet()) {
            if (e.getValue().contains(name)) {
                // docTokens 是 uniq, 不区分频次, 这里用 "doc 出现 → 选第一个"
                if (best == null) best = e.getKey();
                bestCnt++;
            }
        }
        return best;
    }

    /**
     * 删除某 KB 的全部抽取结果.
     */
    @Transactional
    public ClearResult clear(Long kbId) {
        int rels = relationMapper.deleteByKb(kbId);
        int ents = entityMapper.deleteByKb(kbId);
        log.info("[EntityExtractor] 清空 kbId={} entities={} relations={}", kbId, ents, rels);
        return new ClearResult(ents, rels);
    }

    // ==================== DTO ====================

    /** 抽取配置 */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ExtractConfig {
        /** 最低 TF 阈值 */
        private int minFreq = MIN_FREQ;
        /** 段落级共现 (默认 false → 句子级) */
        private boolean paragraphCooccur;
        /** 限制单 KB 文档数 (0 表示全部) */
        private int docLimit;

        public static ExtractConfig defaults() {
            return new ExtractConfig(MIN_FREQ, false, 0);
        }
    }

    /** 抽取结果 */
    public record BuildResult(int entities, int relations, int docCount, long elapsedMs) {}

    /** 清空结果 */
    public record ClearResult(int entities, int relations) {}
}
