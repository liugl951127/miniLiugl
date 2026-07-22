package com.minimax.ai.generation.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 在线学习引擎 (V3.5.16+)
 *
 * <h3>原理</h3>
 * 用户反馈 (识别对了/错了) 转化为权重调整信号, 用 SGD 在线调 4 个模型的权重.
 *
 * <h3>反馈类型</h3>
 * <ul>
 *   <li><b>accept</b>: 用户没修改, 说明当前 intent 正确, 强化当前权重组合</li>
 *   <li><b>correct</b>: 用户改 intent X → Y, 弱化 X 路径, 强化 Y 路径</li>
 *   <li><b>reject</b>: 用户标记错误, 大幅弱化各路径</li>
 * </ul>
 *
 * <h3>SGD 公式</h3>
 * \`\`\`
 *   收到 accept:   weights[predicted_model] += learning_rate × (1 - hit)
 *   收到 correct:  weights[predicted_model] -= learning_rate
 *                  weights[corrected_model] += learning_rate
 *   收到 reject:   weights[*] -= learning_rate / N
 * \`\`\`
 *
 * <h3>持久化</h3>
 * 内存保存 (重启丢). V3.5.17+ 可存 DB.
 *
 * @author MiniMax
 * @since V3.5.16
 */
@Slf4j
@Component
public class OnlineLearningEngine {

    /**
     * 当前 4 个模型权重 (动态调整)
     */
    private final Map<Model, Double> weights = new ConcurrentHashMap<>();

    /**
     * 模型路径: 哪个模型投了哪个 intent (用于 accept/correct 时追溯)
     */
    private final Map<String, ModelVotes> lastVotes = new ConcurrentHashMap<>();

    /**
     * 累计接受 / 纠正 / 拒绝数
     */
    private final AtomicLong acceptCount = new AtomicLong(0);
    private final AtomicLong correctCount = new AtomicLong(0);
    private final AtomicLong rejectCount = new AtomicLong(0);

    /**
     * 学习率 (默认 0.05)
     */
    private double learningRate = 0.05;

    /**
     * 权重上下限 (避免单边)
     */
    private static final double MIN_W = 0.05;
    private static final double MAX_W = 0.8;

    public OnlineLearningEngine() {
        // 初始权重与 application.yml 对齐
        weights.put(Model.TF, 0.4);
        weights.put(Model.NGRAM, 0.3);
        weights.put(Model.SYNONYM, 0.2);
        weights.put(Model.CONTEXT, 0.1);
    }

    /**
     * 记录一次识别结果 (供后续 feedback 调用)
     */
    public void recordVote(String sessionId, String query, String predictedIntent, Map<Model, Double> voteScores) {
        lastVotes.put(key(sessionId, query), new ModelVotes(predictedIntent, voteScores));
    }

    /**
     * 反馈: 用户接受 (没修改)
     */
    public void accept(String sessionId, String query) {
        ModelVotes vote = lastVotes.get(key(sessionId, query));
        if (vote == null) return;
        acceptCount.incrementAndGet();

        // 强化最投票模型的权重
        Model top = topModel(vote.scores);
        if (top != null) {
            adjustWeight(top, +learningRate);
            log.info("[online] accept: session={} query='{}' intent={} boost={}",
                    sessionId, query, vote.intent, top);
        }
    }

    /**
     * 反馈: 用户纠正 (改 intent)
     */
    public void correct(String sessionId, String query, String correctedIntent) {
        ModelVotes vote = lastVotes.get(key(sessionId, query));
        if (vote == null) return;
        correctCount.incrementAndGet();

        // 弱化投票最高的模型, 强化未投中目标 intent 的模型
        Model top = topModel(vote.scores);
        if (top != null) {
            adjustWeight(top, -learningRate);
        }
        // 简化: 把所有其它模型轻微提升 (实际可计算哪个模型给 correctIntent 投了最高)
        for (Model m : Model.values()) {
            if (m != top) {
                adjustWeight(m, +learningRate / 4.0);
            }
        }
        log.info("[online] correct: session={} query='{}' from={} to={}",
                sessionId, query, vote.intent, correctedIntent);
    }

    /**
     * 反馈: 用户拒绝 (完全错)
     */
    public void reject(String sessionId, String query) {
        ModelVotes vote = lastVotes.get(key(sessionId, query));
        if (vote == null) return;
        rejectCount.incrementAndGet();

        // 全部权重轻微降低 (但不归零)
        for (Model m : Model.values()) {
            adjustWeight(m, -learningRate / 4.0);
        }
        log.info("[online] reject: session={} query='{}'", sessionId, query);
    }

    /**
     * 调整权重 (clamp 到 [MIN_W, MAX_W])
     */
    private void adjustWeight(Model m, double delta) {
        weights.compute(m, (k, v) -> {
            if (v == null) v = 0.25;
            double nv = Math.max(MIN_W, Math.min(MAX_W, v + delta));
            return nv;
        });
    }

    /**
     * 当前最优模型 (投票得分最高)
     */
    private Model topModel(Map<Model, Double> scores) {
        if (scores == null || scores.isEmpty()) return null;
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * 获取当前权重 (供 IntentService 用)
     */
    public Map<Model, Double> getWeights() {
        return new EnumMap<>(weights);
    }

    /**
     * 统计
     */
    public Map<String, Object> stats() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("weights", weights);
        s.put("acceptCount", acceptCount.get());
        s.put("correctCount", correctCount.get());
        s.put("rejectCount", rejectCount.get());
        s.put("learningRate", learningRate);
        return s;
    }

    public void setLearningRate(double lr) {
        this.learningRate = Math.max(0.001, Math.min(0.5, lr));
    }

    /**
     * 重置权重
     */
    public void resetWeights() {
        weights.put(Model.TF, 0.4);
        weights.put(Model.NGRAM, 0.3);
        weights.put(Model.SYNONYM, 0.2);
        weights.put(Model.CONTEXT, 0.1);
    }

    private String key(String sessionId, String query) {
        return (sessionId == null ? "default" : sessionId) + "|" + query;
    }

    /**
     * 4 个模型枚举
     */
    public enum Model { TF, NGRAM, SYNONYM, CONTEXT }

    /**
     * 单次投票快照
     */
    private record ModelVotes(String intent, Map<Model, Double> scores) {}
}
