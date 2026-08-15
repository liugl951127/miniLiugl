package com.minimax.ai.nlp;

// RequiredArgsConstructor: Lombok 自动构造器
import lombok.RequiredArgsConstructor;
// Slf4j: Lombok 日志
import lombok.extern.slf4j.Slf4j;
// Component: Spring Bean
import org.springframework.stereotype.Component;

// ArrayList: 动态数组
import java.util.ArrayList;
// Arrays: 数组工具
import java.util.Arrays;
// Collections: 集合工具
import java.util.Collections;
// HashMap: 普通 Map
import java.util.HashMap;
// HashSet: 哈希集合
import java.util.HashSet;
// Iterator: 迭代器
import java.util.Iterator;
// List: 列表
import java.util.List;
// Map: 键值对
import java.util.Map;
// Set: 集合
import java.util.Set;
// ConcurrentHashMap: 线程安全 Map
import java.util.concurrent.ConcurrentHashMap;

/**
 * V6.0 多轮对话管理器 (MultiTurnDialogManager)
 *
 * <h2>核心功能</h2>
 * <ol>
 *   <li><b>会话状态</b>: 记住每轮上下文 (最近 10 轮)</li>
 *   <li><b>指代消解</b>: "它"/"这个" → 上轮主语</li>
 *   <li><b>话题跟踪</b>: Jaccard 相似度检测话题转移</li>
 *   <li><b>用户画像</b>: 累计用户偏好 (主题/风格)</li>
 *   <li><b>follow-up 建议</b>: 基于当前答案推荐后续问题</li>
 * </ol>
 *
 * <h2>指代消解 (Coreference Resolution) 算法</h2>
 * <pre>
 *   轮 1: "Java 是什么" → 主语 = "Java"
 *   轮 2: "它有什么特点" → 替换: "Java有什么特点"
 * </pre>
 * 简单规则: 检测 "它"/"这个"/"那个"/"此"/"其" → 替换为上轮主语 (前 6 字)
 *
 * <h2>话题跟踪 (Topic Tracking) 算法</h2>
 * 用 Jaccard 相似度:
 * <pre>
 *   Jaccard(A, B) = |A ∩ B| / |A ∪ B|
 * </pre>
 * 例: A = {"Java", "什么", "是"}, B = {"Java", "特点"}
 *   A ∩ B = {"Java"}  → 1
 *   A ∪ B = {"Java", "什么", "是", "特点"} → 4
 *   Jaccard = 1/4 = 0.25
 * 阈值 < 0.3 视为话题转移
 *
 * <h2>用户画像 (User Profile)</h2>
 * 累计每个用户的:
 *   - frequentTopics: 主题词频次 (Map<String, Integer>)
 *   - preferredStyle: 风格偏好 (formal/casual/neutral)
 *   - totalMessages: 消息总数
 *
 * 应用: 个性化推荐,风格适配
 */
@Slf4j
@Component
/**
 * MultiTurnDialogManager (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * NLP 处理 - MultiTurnDialogManager.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 MultiTurnDialogManager 的业务能力</li>
 *   <li>参与 AI 平台整体架构</li>
 *   <li>支持 Spring 依赖注入</li>
 * </ul>
 *
 * <h2>依赖</h2>
 * <ul>
 *   <li>Spring Framework (自动注入)</li>
 *   <li>Lombok (简化代码)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V6.1
 */
@RequiredArgsConstructor
public class MultiTurnDialogManager {

    // ============== 依赖 ==============
    // NerExtractor: 命名实体识别,用于话题跟踪
    private final NerExtractor nerExtractor;

    // ============== 状态存储 ==============
    /** sessionId → 对话上下文 (线程安全) */
    private final Map<String, DialogContext> contexts = new ConcurrentHashMap<>();

    // ============== 配置 ==============
    /** 最大保留轮数 (超过截断老的) */
    private static final int MAX_TURNS = 10;
    /** 话题转移阈值 (Jaccard 低于此值视为转移) */
    private static final double TOPIC_SHIFT_THRESHOLD = 0.3;

    // ============== 对话管理 API ==============
    /**
     * 添加用户消息
     * @param sessionId 会话 ID
     * @param text 消息内容
     */
    public void addUserMessage(String sessionId, String text) {
        DialogContext ctx = context(sessionId);
        synchronized (ctx) {
            // 添加到 turns
            ctx.turns.add(new Turn("user", text, System.currentTimeMillis()));
            // 超过最大轮数,截断老的
            if (ctx.turns.size() > MAX_TURNS * 2) {
                // 保留最新的 MAX_TURNS * 2 条 (一轮 = user + assistant)
                ctx.turns.subList(0, ctx.turns.size() - MAX_TURNS * 2).clear();
            }
            // 更新话题 (提取主语)
            updateTopic(ctx, text);
            // 更新用户画像
            updateUserProfile(ctx, text);
        }
    }

    /**
     * 添加助手消息
     */
    public void addAssistantMessage(String sessionId, String text) {
        DialogContext ctx = context(sessionId);
        synchronized (ctx) {
            ctx.turns.add(new Turn("assistant", text, System.currentTimeMillis()));
            if (ctx.turns.size() > MAX_TURNS * 2) {
                ctx.turns.subList(0, ctx.turns.size() - MAX_TURNS * 2).clear();
            }
        }
    }

    /**
     * 获取最近 N 轮对话
     * @param sessionId 会话 ID
     * @param n 轮数 (每轮 = user + assistant 2 条)
     * @return 最近的 turns 列表
     */
    public List<Turn> getRecentTurns(String sessionId, int n) {
        DialogContext ctx = context(sessionId);
        synchronized (ctx) {
            int size = ctx.turns.size();
            // 取最近 2n 条 (n 轮 = 2n 条 turn)
            return new ArrayList<>(ctx.turns.subList(Math.max(0, size - n * 2), size));
        }
    }

    // ============== 指代消解 ==============
    /**
     * 把代词替换为上轮主语
     *
     * <h2>算法</h2>
     * <ol>
     *   <li>取上轮主语 (lastMainEntity)</li>
     *   <li>提取本轮实体 (NER)</li>
     *   <li>如果本轮已有主语 (PERSON/CITY/LANG/PROVINCE), 跳过消解</li>
     *   <li>否则, 把 "它"/"这个"/"那个"/"此"/"其" 替换为上轮主语前 6 字</li>
     * </ol>
     *
     * @param sessionId 会话 ID
     * @param text 当前用户输入
     * @return 消解后的文本
     */
    public String resolveCoreferences(String sessionId, String text) {
        DialogContext ctx = context(sessionId);
        // 没上轮主语,无法消解
        if (ctx.lastMainEntity == null || ctx.lastMainEntity.isEmpty()) {
            return text;
        }
        // 提取本轮的实体
        List<NerExtractor.Entity> entities = nerExtractor.extract(text);
        // 如果本轮已经有主语, 跳过消解 (避免覆盖)
        boolean hasMain = entities.stream().anyMatch(e ->
                e.type.equals("PERSON") || e.type.equals("CITY") ||
                e.type.equals("LANG") || e.type.startsWith("PROVINCE"));
        if (hasMain) return text;

        // 替换代词
        String resolved = text;
        String[] pronouns = {"它", "这个", "那个", "此", "其"};
        for (String p : pronouns) {
            if (resolved.contains(p)) {
                // 取上轮主语前 6 字 (避免太长)
                String key = ctx.lastMainEntity.length() > 6
                        ? ctx.lastMainEntity.substring(0, 6)
                        : ctx.lastMainEntity;
                resolved = resolved.replace(p, key);
            }
        }
        // 日志记录
        if (!resolved.equals(text)) {
            log.debug("多轮指代消解: '{}' -> '{}'", text, resolved);
        }
        return resolved;
    }

    // ============== 话题跟踪 ==============
    /**
     * 检测话题是否转移
     *
     * <h2>算法: Jaccard 相似度</h2>
     * <pre>
     *   Jaccard(A, B) = |A ∩ B| / |A ∪ B|
     * </pre>
     * 值域 [0, 1], 1 = 完全相同, 0 = 完全不相关
     * 阈值 0.3, 低于视为话题转移
     *
     * @param sessionId 会话 ID
     * @param newText 新一轮用户输入
     * @return true = 话题转移, false = 话题延续
     */
    public boolean isTopicShift(String sessionId, String newText) {
        DialogContext ctx = context(sessionId);
        if (ctx.turns.isEmpty()) return true;  // 第一次问, 必然"转移"
        // 取最近一条 user 消息
        String lastUser = lastUserMessage(ctx);
        if (lastUser == null) return false;
        // 计算 Jaccard 相似度
        return jaccard(tokenize(lastUser), tokenize(newText)) < TOPIC_SHIFT_THRESHOLD;
    }

    // ============== Follow-up 建议 ==============
    /**
     * 基于当前答案推荐 follow-up 问题
     *
     * <h2>算法</h2>
     * 从当前答案中提取关键词,用模板生成 6 类问题:
     *   - "什么是 X"
     *   - "X 的优势"
     *   - "X 的应用场景"
     *   - "如何学习 X"
     *   - "X 有什么坑"
     *   - "X 和其他技术对比"
     *
     * @param sessionId 会话 ID (暂未使用)
     * @param currentAnswer 当前 AI 回答
     * @param topN 建议数量
     * @return 建议问题列表
     */
    public List<String> suggestFollowUps(String sessionId, String currentAnswer, int topN) {
        // 模板化问题
        List<String> suggestions = new ArrayList<>();
        Set<String> keywords = tokenize(currentAnswer);
        // 6 种模板
        List<String> templates = Arrays.asList(
                "什么是 %s",
                "%s 的优势",
                "%s 的应用场景",
                "如何学习 %s",
                "%s 有什么坑",
                "%s 和其他技术对比"
        );
        // 循环填充模板
        int i = 0;
        for (String kw : keywords) {
            if (kw.length() < 2) continue;  // 过滤单字
            suggestions.add(String.format(templates.get(i % templates.size()), kw));
            i++;
            if (i >= topN) break;
        }
        return suggestions;
    }

    // ============== 用户画像 ==============
    /**
     * 获取用户画像
     */
    public UserProfile getUserProfile(String sessionId) {
        return context(sessionId).profile;
    }

    /**
     * 清理过期 session (用于定时任务)
     * @param maxAgeMinutes 最大存活分钟数
     * @return 清理数量
     */
    public int cleanup(int maxAgeMinutes) {
        // cutoff 时间戳
        long cutoff = System.currentTimeMillis() - maxAgeMinutes * 60_000L;
        int removed = 0;
        // 遍历所有 session
        Iterator<Map.Entry<String, DialogContext>> it = contexts.entrySet().iterator();
        while (it.hasNext()) {
            DialogContext ctx = it.next().getValue();
            // 空或最后消息早于 cutoff, 删除
            if (ctx.turns.isEmpty() || ctx.turns.get(ctx.turns.size() - 1).timestamp < cutoff) {
                it.remove();
                removed++;
            }
        }
        return removed;
    }

    /**
     * 当前活跃 session 数
     */
    public int activeSessionCount() {
        return contexts.size();
    }

    // ============== 内部工具 ==============
    /**
     * 获取或创建 session 上下文
     */
    private DialogContext context(String sessionId) {
        // 默认 session "_default" 用于无 sessionId 情况
        return contexts.computeIfAbsent(sessionId == null ? "_default" : sessionId,
                k -> new DialogContext());
    }

    /**
     * 更新话题主语 (从最新消息中提取)
     */
    private void updateTopic(DialogContext ctx, String text) {
        // NER 提取
        List<NerExtractor.Entity> entities = nerExtractor.extract(text);
        for (NerExtractor.Entity e : entities) {
            // 优先取 PERSON/CITY/LANG/PROVINCE/URL
            if (e.type.equals("PERSON") || e.type.equals("CITY") ||
                e.type.equals("LANG") || e.type.equals("URL") ||
                e.type.startsWith("PROVINCE")) {
                ctx.lastMainEntity = e.text;
                break;  // 只取第一个
            }
        }
    }

    /**
     * 更新用户画像 (累计主题频次)
     */
    private void updateUserProfile(DialogContext ctx, String text) {
        Set<String> tokens = tokenize(text);
        for (String t : tokens) {
            // 频次 +1
            ctx.profile.frequentTopics.merge(t, 1, Integer::sum);
        }
    }

    /**
     * 取最近一条 user 消息
     */
    private String lastUserMessage(DialogContext ctx) {
        // 倒序遍历
        for (int i = ctx.turns.size() - 1; i >= 0; i--) {
            Turn t = ctx.turns.get(i);
            if ("user".equals(t.role)) return t.text;
        }
        return null;
    }

    /**
     * 简单分词: 按标点/空白切分
     */
    private Set<String> tokenize(String text) {
        if (text == null) return Collections.emptySet();
        Set<String> out = new HashSet<>();
        for (String t : text.split("[\\s\\p{Punct}]+")) {
            if (t.length() > 1) out.add(t);  // 过滤单字
        }
        return out;
    }

    /**
     * Jaccard 相似度
     * 公式: |A ∩ B| / |A ∪ B|
     */
    private double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 0.0;  // 双方都空, 视为不相关
        // 交集
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        // 并集
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        // Jaccard
        return union.isEmpty() ? 0.0 : (double) inter.size() / union.size();
    }

    // ============== 数据类 ==============
    /**
     * 单条对话
     */
    public static class Turn {
        public final String role;        // "user" / "assistant"
        public final String text;
        public final long timestamp;
        public Turn(String r, String t, long ts) {
            this.role = r;
            this.text = t;
            this.timestamp = ts;
        }
    }

    /**
     * 用户画像
     */
    public static class UserProfile {
        // 主题频次统计
        public final Map<String, Integer> frequentTopics = new HashMap<>();
        // 偏好风格: formal / casual / neutral
        public String preferredStyle = "neutral";
        // 消息总数
        public int totalMessages = 0;
    }

    /**
     * 对话上下文
     */
    public static class DialogContext {
        // 所有轮次
        public final List<Turn> turns = new ArrayList<>();
        // 上轮主语 (供指代消解)
        public String lastMainEntity;
        // 当前话题
        public String currentTopic;
        // 用户画像
        public final UserProfile profile = new UserProfile();
    }
}
