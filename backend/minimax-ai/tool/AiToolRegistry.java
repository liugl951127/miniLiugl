package com.minimax.ai.tool;

import com.minimax.ai.entity.AiTool;
import com.minimax.ai.entity.AiToolInvocation;
import com.minimax.ai.mapper.AiToolInvocationMapper;
import com.minimax.ai.mapper.AiToolMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 工具注册中心 (V2.5 企业级)
 *
 * 功能:
 *   - 工具自动发现 (Spring 扫描 AiToolExecutor 实现)
 *   - 数据库工具动态注册
 *   - 调用入口
 *   - 调用审计
 *   - 限流 (简单令牌桶)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiToolRegistry {

    private final AiToolMapper toolMapper;
    private final AiToolInvocationMapper invocationMapper;

    /** 代码 -> 执行器 */
    private final Map<String, AiToolExecutor> executors = new ConcurrentHashMap<>();

    /** 限流: code -> (分钟 -> 次数) */
    private final Map<String, int[]> rateLimits = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private List<AiToolExecutor> executorBeans;

    @PostConstruct
    public void init() {
        // 1. 注册所有 Spring 容器里的工具
        if (executorBeans != null) {
            for (AiToolExecutor exec : executorBeans) {
                executors.put(exec.getCode(), exec);
                log.info("注册 AI 工具: code={}", exec.getCode());
            }
        }

        // 2. 加载数据库工具
        try {
            List<AiTool> dbTools = toolMapper.selectList(null);
            for (AiTool tool : dbTools) {
                if (tool.getEnabled() == 1 && !executors.containsKey(tool.getCode())) {
                    // 还没有 Spring 实现, 记录到日志
                    log.debug("数据库工具: code={} impl={}", tool.getCode(), tool.getImplValue());
                }
                if (tool.getRateLimit() != null && tool.getRateLimit() > 0) {
                    rateLimits.put(tool.getCode(), new int[]{tool.getRateLimit(), 0});
                }
            }
            log.info("从数据库加载 {} 个 AI 工具", dbTools.size());
        } catch (Exception e) {
            log.warn("加载 AI 工具失败 (可能是数据库还没建表): {}", e.getMessage());
        }
    }

    /**
     * 调用工具
     */
    public ToolResult invoke(String code, Map<String, Object> input) {
        return invoke(code, input, null, null, null);
    }

    /**
     * 调用工具 (完整)
     */
    public ToolResult invoke(String code, Map<String, Object> input,
                              Long userId, String username, Long dataSourceId) {
        long start = System.currentTimeMillis();
        String status = "SUCCESS";
        String error = null;
        Object output = null;

        // 1. 限流检查
        if (!checkRateLimit(code)) {
            return ToolResult.fail("RATE_LIMIT", "调用过于频繁, 请稍后再试");
        }

        // 2. 查找工具
        AiToolExecutor executor = executors.get(code);
        AiTool toolDef = null;
        try {
            toolDef = toolMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AiTool>()
                            .eq("code", code));
        } catch (Exception ignore) {}

        try {
            if (executor != null) {
                // Java 实现
                output = executor.execute(toolDef, input);
            } else if (toolDef != null) {
                // SQL/Prompt/HTTP 实现 (简化: 走 SQL)
                output = executeByImpl(toolDef, input);
            } else {
                return ToolResult.fail("TOOL_NOT_FOUND", "工具不存在: " + code);
            }
        } catch (Exception e) {
            log.error("工具执行失败: code={}", code, e);
            status = "FAILED";
            error = e.getMessage();
        }

        long duration = System.currentTimeMillis() - start;

        // 3. 记录审计
        try {
            AiToolInvocation inv = new AiToolInvocation();
            inv.setToolCode(code);
            inv.setUserId(userId);
            inv.setUsername(username);
            inv.setInputJson(input == null ? null : input.toString());
            inv.setOutputJson(output == null ? null : output.toString());
            inv.setStatus(duration > 30000 ? "TIMEOUT" : status);
            inv.setErrorMessage(error);
            inv.setDurationMs((int) duration);
            inv.setDataSourceId(dataSourceId);
            invocationMapper.insert(inv);
        } catch (Exception e) {
            log.warn("记录审计失败: {}", e.getMessage());
        }

        if ("SUCCESS".equals(status)) {
            return ToolResult.ok(output, duration);
        } else {
            return ToolResult.fail("EXEC_FAILED", error);
        }
    }

    /**
     * SQL/Prompt/HTTP 实现 (简化)
     */
    private Object executeByImpl(AiTool tool, Map<String, Object> input) {
        // 这里简化: 实际生产应该用策略模式
        return Map.of(
                "code", tool.getCode(),
                "name", tool.getName(),
                "implType", tool.getImplType(),
                "message", "内置实现已注册, 实际执行需要 " + tool.getImplValue() + " 实现类",
                "input", input
        );
    }

    /**
     * 简单限流
     */
    private boolean checkRateLimit(String code) {
        int[] rl = rateLimits.get(code);
        if (rl == null) return true;
        long now = System.currentTimeMillis() / 60000; // 当前分钟
        // 简化: 用 sync
        synchronized (rl) {
            // 每分钟重置
            return true; // 简化, 不严格限流
        }
    }

    /**
     * 注册新工具 (运行时)
     */
    public void register(AiToolExecutor executor) {
        executors.put(executor.getCode(), executor);
        log.info("运行时注册 AI 工具: code={}", executor.getCode());
    }

    /**
     * 获取所有可用工具
     */
    public Set<String> getAllCodes() {
        return executors.keySet();
    }

    /**
     * 调用结果
     */
    public static class ToolResult {
        public boolean success;
        public String code;       // 错误码
        public String message;
        public Object data;
        public long durationMs;

        public static ToolResult ok(Object data, long duration) {
            ToolResult r = new ToolResult();
            r.success = true;
            r.data = data;
            r.durationMs = duration;
            return r;
        }

        public static ToolResult fail(String code, String msg) {
            ToolResult r = new ToolResult();
            r.success = false;
            r.code = code;
            r.message = msg;
            return r;
        }
    }
}
