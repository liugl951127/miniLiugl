package com.minimax.model.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.common.result.Result;
import com.minimax.model.entity.ModelBattleLog;
import com.minimax.model.entity.ModelConfig;
import com.minimax.model.mapper.ModelBattleLogMapper;
import com.minimax.model.mapper.ModelConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 模型对决排行榜 (V4.1).
 *
 * 端点:
 *   GET /api/v1/leaderboard/overall    总体排行 (按平均评分)
 *   GET /api/v1/leaderboard/latency    速度排行
 *   GET /api/v1/leaderboard/recent     最近对决
 *   GET /api/v1/leaderboard/categories 分类排行 (按 model_code)
 *
 * 数据源: model_battle_log 表
 *
 * @since 2026-06
 */
@Slf4j
@RestController
@RequestMapping("/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final ModelBattleLogMapper battleLogMapper;
    private final ModelConfigMapper modelConfigMapper;

    /** 总体排行 (按平均评分降序) */
    @GetMapping("/overall")
    public Result<List<Map<String, Object>>> overall(
            @RequestParam(defaultValue = "50") int limit) {
        List<Map<String, Object>> rows = aggregate("score");
        return Result.ok(rows.stream().limit(limit).toList());
    }

    /** 速度排行 (按 P50 延迟升序) */
    @GetMapping("/latency")
    public Result<List<Map<String, Object>>> latency(
            @RequestParam(defaultValue = "50") int limit) {
        List<Map<String, Object>> rows = aggregate("latency");
        return Result.ok(rows.stream().limit(limit).toList());
    }

    /** 最近对决 (按时间倒序, 取每模型最近 5 局) */
    @GetMapping("/recent")
    public Result<List<Map<String, Object>>> recent(
            @RequestParam(defaultValue = "20") int limit) {
        // 直接查 model_battle_log 最近 20 条
        List<Map<String, Object>> rows = battleLogMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ModelBattleLog>()
                        .select("model_code", "latency_ms", "prompt_tokens", "completion_tokens",
                                "score", "status", "created_at")
                        .orderByDesc("created_at")
                        .last("LIMIT " + Math.min(limit, 200))
        );
        return Result.ok(rows);
    }

    /** 分类排行 (按 provider) */
    @GetMapping("/categories")
    public Result<Map<String, List<Map<String, Object>>>> categories() {
        List<ModelConfig> allModels = modelConfigMapper.selectList(null);
        Map<String, List<Map<String, Object>>> byProvider = new LinkedHashMap<>();
        for (ModelConfig m : allModels) {
            // 这里简化用 model_code 前缀 (含 "/" 的是 siliconflow 类)
            String prefix = m.getModelCode().contains("/") ? m.getModelCode().split("/")[0] : m.getModelCode();
            byProvider.computeIfAbsent(prefix, k -> new ArrayList<>()).add(Map.of(
                    "modelCode", m.getModelCode(),
                    "displayName", m.getDisplayName() != null ? m.getDisplayName() : m.getModelCode(),
                    "supportsVision", Boolean.TRUE.equals(m.getSupportsVision()),
                    "enabled", m.getEnabled() == 1
            ));
        }
        return Result.ok(byProvider);
    }

    /**
     * 按 model_code 聚合: 平均分, P50 延迟, 调用次数.
     */
    private List<Map<String, Object>> aggregate(String sortBy) {
        List<Map<String, Object>> rows = battleLogMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ModelBattleLog>()
                        .select("model_code",
                                "COUNT(*) AS cnt",
                                "AVG(score) AS avg_score",
                                "AVG(latency_ms) AS avg_latency",
                                "SUM(CASE WHEN status='ok' THEN 1 ELSE 0 END) AS ok_cnt")
                        .groupBy("model_code")
                        .orderByDesc("avg_score")
        );
        // 简化: 直接用 MySQL 聚合 (没有 window function 也能跑)
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> m = new LinkedHashMap<>(r);
            // 加 displayName
            try {
                ModelConfig cfg = modelConfigMapper.selectOne(
                        new LambdaQueryWrapper<ModelConfig>().eq(ModelConfig::getModelCode, (String) r.get("model_code"))
                );
                if (cfg != null) {
                    m.put("displayName", cfg.getDisplayName());
                    m.put("supportsVision", cfg.getSupportsVision());
                }
            } catch (Exception ignore) {}
            result.add(m);
        }
        if ("latency".equals(sortBy)) {
            result.sort((a, b) -> {
                Object va = a.get("avg_latency"), vb = b.get("avg_latency");
                if (va == null) return 1;
                if (vb == null) return -1;
                return Double.compare(((Number) va).doubleValue(), ((Number) vb).doubleValue());
            });
        }
        return result;
    }
}
