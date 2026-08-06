package com.minimax.ai.nlp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * V6.0 多轮对话管理器 (MultiTurnDialogManager)
 *
 * 功能:
 *   1. **会话状态**: 记住每轮上下文 (user/assistant, 最近 10 轮)
 *   2. **指代消解**: "它"/"这个"/"那个" 替换为上轮主语
 *   3. **话题跟踪**: 检测话题转移, 隔离无关上下文
 *   4. **用户画像**: 累计用户偏好 (风格/兴趣/常问主题)
 *   5. **问题建议**: 基于当前问题推荐 follow-up 问题
 *
 * 用法:
 *   dialogManager.addUserMessage(sessionId, "Java 是什么");
 *   dialogManager.addAssistantMessage(sessionId, "Java 是...");
 *   dialogManager.addUserMessage(sessionId, "它有什么特点?");  // 消解为 "Java 有什么特点"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MultiTurnDialogManager {

    /** sessionId -> DialogContext */
    private final Map<String, DialogContext> contexts = new ConcurrentHashMap<>();

    private final NerExtractor nerExtractor;

    private static final int MAX_TURNS = 10;
    private static final int TOPIC_SHIFT_THRESHOLD = 3;  // 主题相似度低于此判为转移

    /**
     * 添加用户消息
     */
    public void addUserMessage(String sessionId, String text) {
        DialogContext ctx = context(sessionId);
        synchronized (ctx) {
            ctx.turns.add(new Turn("user", text, System.currentTimeMillis()));
            if (ctx.turns.size() > MAX_TURNS * 2) {
                ctx.turns.subList(0, ctx.turns.size() - MAX_TURNS * 2).clear();
            }
            // 话题检测
            updateTopic(ctx, text);
            // 用户画像
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
     * 获取上下文 (返回最近 N 轮)
     */
    public List<Turn> getRecentTurns(String sessionId, int n) {
        DialogContext ctx = context(sessionId);
        synchronized (ctx) {
            int size = ctx.turns.size();
            return new ArrayList<>(ctx.turns.subList(Math.max(0, size - n * 2), size));
        }
    }

    /**
     * 指代消解: 处理 "它" "这个" "那个" 等代词
     */
    public String resolveCoreferences(String sessionId, String text) {
        DialogContext ctx = context(sessionId);
        if (ctx.lastMainEntity == null || ctx.lastMainEntity.isEmpty()) {
            return text;
        }
        // 提取本轮的实体
        List<NerExtractor.Entity> entities = nerExtractor.extract(text);
        // 如果本轮已有主语, 跳过消解
        boolean hasMain = entities.stream().anyMatch(e ->
                e.type.equals("PERSON") || e.type.equals("CITY") ||
                e.type.equals("LANG") || e.type.startsWith("PROVINCE"));
        if (hasMain) return text;

        // 替换代词
        String resolved = text;
        String[] pronouns = {"它", "这个", "那个", "此", "其"};
        for (String p : pronouns) {
            if (resolved.contains(p)) {
                resolved = resolved.replace(p, ctx.lastMainEntity);
            }
        }
        if (!resolved.equals(text)) {
            log.debug("多轮指代消解: '{}' -> '{}'", text, resolved);
        }
        return resolved;
    }

    /**
     * 话题检测: 简单 Jaccard 相似度
     */
    public boolean isTopicShift(String sessionId, String newText) {
        DialogContext ctx = context(sessionId);
        if (ctx.turns.isEmpty()) return true;
        String lastUser = lastUserMessage(ctx);
        if (lastUser == null) return false;
        return jaccard(tokenize(lastUser), tokenize(newText)) < TOPIC_SHIFT_THRESHOLD / 10.0;
    }

    /**
     * 推荐 follow-up 问题
     */
    public List<String> suggestFollowUps(String sessionId, String currentAnswer, int topN) {
        DialogContext ctx = context(sessionId);
        // 简单: 根据当前答案里的关键词生成
        List<String> suggestions = new ArrayList<>();
        Set<String> keywords = tokenize(currentAnswer);
        // 模板化问题
        List<String> templates = Arrays.asList(
                "什么是 %s",
                "%s 的优势",
                "%s 的应用场景",
                "如何学习 %s",
                "%s 有什么坑",
                "%s 和其他技术对比"
        );
        int i = 0;
        for (String kw : keywords) {
            if (kw.length() < 2) continue;
            suggestions.add(String.format(templates.get(i % templates.size()), kw));
            i++;
            if (i >= topN) break;
        }
        return suggestions;
    }

    /**
     * 获取用户画像
     */
    public UserProfile getUserProfile(String sessionId) {
        return context(sessionId).profile;
    }

    /**
     * 清理过期 session (用于定时任务)
     */
    public int cleanup(int maxAgeMinutes) {
        long cutoff = System.currentTimeMillis() - maxAgeMinutes * 60_000L;
        int removed = 0;
        Iterator<Map.Entry<String, DialogContext>> it = contexts.entrySet().iterator();
        while (it.hasNext()) {
            DialogContext ctx = it.next().getValue();
            if (ctx.turns.isEmpty() || ctx.turns.get(ctx.turns.size() - 1).timestamp < cutoff) {
                it.remove();
                removed++;
            }
        }
        return removed;
    }

    public int activeSessionCount() {
        return contexts.size();
    }

    private DialogContext context(String sessionId) {
        return contexts.computeIfAbsent(sessionId == null ? "_default" : sessionId,
                k -> new DialogContext());
    }

    private void updateTopic(DialogContext ctx, String text) {
        // 提取主语
        List<NerExtractor.Entity> entities = nerExtractor.extract(text);
        for (NerExtractor.Entity e : entities) {
            if (e.type.equals("PERSON") || e.type.equals("CITY") ||
                e.type.equals("LANG") || e.type.equals("URL") ||
                e.type.startsWith("PROVINCE")) {
                ctx.lastMainEntity = e.text;
                break;
            }
        }
    }

    private void updateUserProfile(DialogContext ctx, String text) {
        Set<String> tokens = tokenize(text);
        for (String t : tokens) {
            ctx.profile.frequentTopics.merge(t, 1, Integer::sum);
        }
    }

    private String lastUserMessage(DialogContext ctx) {
        for (int i = ctx.turns.size() - 1; i >= 0; i--) {
            Turn t = ctx.turns.get(i);
            if ("user".equals(t.role)) return t.text;
        }
        return null;
    }

    private Set<String> tokenize(String text) {
        if (text == null) return Collections.emptySet();
        Set<String> out = new HashSet<>();
        for (String t : text.split("[\\s\\p{Punct}]+")) {
            if (t.length() > 1) out.add(t);
        }
        return out;
    }

    private double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 0.0;
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0.0 : (double) inter.size() / union.size();
    }

    public static class Turn {
        public final String role;       // "user" / "assistant"
        public final String text;
        public final long timestamp;
        public Turn(String r, String t, long ts) {
            this.role = r;
            this.text = t;
            this.timestamp = ts;
        }
    }

    public static class UserProfile {
        public final Map<String, Integer> frequentTopics = new HashMap<>();
        public String preferredStyle = "neutral";  // formal / casual / neutral
        public int totalMessages = 0;
    }

    public static class DialogContext {
        public final List<Turn> turns = new ArrayList<>();
        public String lastMainEntity;       // 上轮主语
        public String currentTopic;
        public final UserProfile profile = new UserProfile();
    }
}
