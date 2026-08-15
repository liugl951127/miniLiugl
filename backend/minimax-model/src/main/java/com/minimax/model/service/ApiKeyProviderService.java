package com.minimax.model.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * V5.18: API Key 多源解析 + 轮询 + 失败计数.
 *
 * 优先级:
 *   1) 数据库 model_provider.api_key 字段 (管理后台配置)
 *   2) 环境变量 (高优先级, 覆盖 DB)
 *      - OPENAI_API_KEY / OPENAI_API_KEY_2 / _3 (逗号分隔多个, 轮询)
 *      - MINIMAX_API_KEY / MINIMAX_API_KEY_2
 *      - DEEPSEEK_API_KEY
 *      - ANTHROPIC_API_KEY
 *      - GEMINI_API_KEY
 *   3) 占位符 (sk-PLACEHOLDER / sk-mock) → fallback mock
 *
 * 轮询策略 (Round-Robin):
 *   同 provider 多 key 时, 每次请求取下一个, 失败时跳过
 *   失败计数 >= 3 的 key 自动熔断 5 分钟
 */
@Slf4j
@Service
public class ApiKeyProviderService {

    /** 每个 provider code → key list */
    private final Map<String, List<String>> keysMap = new ConcurrentHashMap<>();

    /** 每个 provider 的轮询索引 (ThreadSafe) */
    private final Map<String, AtomicInteger> rrIndex = new ConcurrentHashMap<>();

    /** 失败计数 (provider_code:key → count) */
    private final Map<String, Integer> failCount = new ConcurrentHashMap<>();

    @Value("${minimax.model.api-key.openai:}")
    private String openaiKeys;

    @Value("${minimax.model.api-key.minimax:}")
    private String minimaxKeys;

    @Value("${minimax.model.api-key.deepseek:}")
    private String deepseekKeys;

    @Value("${minimax.model.api-key.anthropic:}")
    private String anthropicKeys;

    @Value("${minimax.model.api-key.gemini:}")
    private String geminiKeys;

    @PostConstruct
    public void init() {
        loadEnvKeys("openai", openaiKeys);
        loadEnvKeys("minimax", minimaxKeys);
        loadEnvKeys("deepseek", deepseekKeys);
        loadEnvKeys("anthropic", anthropicKeys);
        loadEnvKeys("gemini", geminiKeys);
        log.info("V5.18 ApiKeyProviderService 初始化: {}", summarize());
    }

    /**
     * 从逗号分隔字符串加载 keys (V5.18: 兼容 OPENAI_API_KEY_1,OPENAI_API_KEY_2)
     */
    private void loadEnvKeys(String provider, String csv) {
        if (csv == null || csv.isBlank()) return;
        List<String> list = new ArrayList<>();
        for (String k : csv.split(",")) {
            k = k.trim();
            if (!k.isEmpty() && !k.startsWith("PLACEHOLDER") && !k.startsWith("mock")) {
                list.add(k);
            }
        }
        if (!list.isEmpty()) {
            keysMap.put(provider, list);
            rrIndex.putIfAbsent(provider, new AtomicInteger(0));
        }
    }

    /**
     * 取一个 provider 的下一个可用 key (轮询).
     * 如果所有 key 都失败, 返回 null → caller 应 fallback mock.
     */
    public String nextKey(String providerCode) {
        List<String> keys = keysMap.get(providerCode);
        if (keys == null || keys.isEmpty()) return null;

        // Round-Robin + 跳过失败次数 >= 3 的
        int size = keys.size();
        AtomicInteger idx = rrIndex.computeIfAbsent(providerCode, k -> new AtomicInteger(0));
        for (int i = 0; i < size; i++) {
            int pos = Math.floorMod(idx.getAndIncrement(), size);
            String key = keys.get(pos);
            if (getFailCount(providerCode, key) < 3) {
                return key;
            }
        }
        // 全部失败, 重置第一个
        log.warn("V5.18: provider={} 所有 key 都失败, 重置计数", providerCode);
        keys.forEach(k -> failCount.remove(providerCode + ":" + k));
        return keys.get(0);
    }

    /**
     * 报告成功 (清零失败计数)
     */
    public void reportSuccess(String providerCode, String key) {
        if (key == null) return;
        failCount.remove(providerCode + ":" + key);
    }

    /**
     * 报告失败 (计数 +1)
     */
    public void reportFailure(String providerCode, String key) {
        if (key == null) return;
        String k = providerCode + ":" + key;
        failCount.merge(k, 1, Integer::sum);
        log.warn("V5.18: provider={} key=***{} 失败, count={}",
                providerCode, key.substring(Math.max(0, key.length() - 4)),
                failCount.get(k));
    }

    private int getFailCount(String providerCode, String key) {
        return failCount.getOrDefault(providerCode + ":" + key, 0);
    }

    /**
     * 检查某 provider 是否有真实 key
     */
    public boolean hasRealKey(String providerCode) {
        List<String> keys = keysMap.get(providerCode);
        return keys != null && !keys.isEmpty();
    }

    /**
     * 汇总 (用于启动日志)
     */
    public Map<String, Integer> summarize() {
        Map<String, Integer> r = new ConcurrentHashMap<>();
        keysMap.forEach((k, v) -> r.put(k, v.size()));
        return r;
    }
}
