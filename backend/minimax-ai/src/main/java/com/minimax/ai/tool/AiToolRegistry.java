package com.minimax.ai.tool;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.minimax.ai.entity.AiTool;
import com.minimax.ai.entity.AiToolInvocation;
import com.minimax.ai.mapper.AiToolInvocationMapper;
import com.minimax.ai.mapper.AiToolMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiToolRegistry {

    private final AiToolMapper toolMapper;
    private final AiToolInvocationMapper invocationMapper;
    private final Map<String, AiToolExecutor> executors = new ConcurrentHashMap<>();
    private final Map<String, int[]> rateLimits = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private List<AiToolExecutor> executorBeans;

    @PostConstruct
    public void init() {
        if (executorBeans != null) {
            for (AiToolExecutor exec : executorBeans) {
                executors.put(exec.getCode(), exec);
                log.info("注册 AI 工具: code={}", exec.getCode());
            }
        }
        try {
            List<AiTool> dbTools = toolMapper.selectList(null);
            for (AiTool tool : dbTools) {
                if (tool.getRateLimit() != null && tool.getRateLimit() > 0) {
                    rateLimits.put(tool.getCode(), new int[]{tool.getRateLimit(), 0});
                }
            }
            log.info("从数据库加载 {} 个 AI 工具", dbTools.size());
        } catch (Exception e) {
            log.warn("加载 AI 工具失败: {}", e.getMessage());
        }
    }

    public ToolResult invoke(String code, Map<String, Object> input) {
        return invoke(code, input, null, null, null);
    }

    public ToolResult invoke(String code, Map<String, Object> input,
                              Long userId, String username, Long dataSourceId) {
        long start = System.currentTimeMillis();
        String status = "SUCCESS";
        String error = null;
        Object output = null;

        AiToolExecutor executor = executors.get(code);
        AiTool toolDef = null;
        try {
            toolDef = toolMapper.selectOne(new QueryWrapper<AiTool>().eq("code", code));
        } catch (Exception ignore) {}

        try {
            if (executor != null) {
                output = executor.execute(toolDef, input);
            } else if (toolDef != null) {
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

        return "SUCCESS".equals(status) ? ToolResult.ok(output, duration) : ToolResult.fail("EXEC_FAILED", error);
    }

    private Object executeByImpl(AiTool tool, Map<String, Object> input) {
        return Map.of(
                "code", tool.getCode(),
                "name", tool.getName(),
                "implType", tool.getImplType(),
                "message", "内置实现, 实际执行需要 " + tool.getImplValue(),
                "input", input
        );
    }

    public void register(AiToolExecutor executor) {
        executors.put(executor.getCode(), executor);
    }

    public Set<String> getAllCodes() {
        return executors.keySet();
    }

    public static class ToolResult {
        public boolean success;
        public String code;
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
