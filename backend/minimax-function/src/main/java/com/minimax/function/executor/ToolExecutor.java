package com.minimax.function.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.function.entity.FunctionCallLog;
import com.minimax.function.entity.FunctionTool;
import com.minimax.function.mapper.FunctionCallLogMapper;
import com.minimax.function.mapper.FunctionToolMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * 工具执行器。
 *
 * 流程:
 *  1) 按 name 查 FunctionTool
 *  2) 解析 arguments JSON
 *  3) 找到对应的 ToolFunction bean
 *  4) 执行 + 异常隔离
 *  5) 写审计日志
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolExecutor {

    private final FunctionToolMapper toolMapper;
    private final FunctionCallLogMapper logMapper;
    private final List<ToolFunction> builtinFunctions;
    private final ObjectMapper json = new ObjectMapper();

    /**
     * 直接调用 (无 LLM, 纯 API)
     */
    @Transactional
    public ToolResult invoke(Long userId, Long sessionId, String toolName,
                              String argsJson, String ip, String userAgent) {
        FunctionTool tool = toolMapper.selectByName(toolName);
        if (tool == null) {
            return ToolResult.error("tool not found: " + toolName);
        }
        if (tool.getEnabled() == null || tool.getEnabled() != 1) {
            return ToolResult.error("tool disabled: " + toolName);
        }

        Map<String, Object> args;
        try {
            if (argsJson == null || argsJson.isBlank()) args = Map.of();
            else args = json.readValue(argsJson, Map.class);
        } catch (Exception e) {
            return ToolResult.error("invalid arguments JSON: " + e.getMessage());
        }

        long t0 = System.currentTimeMillis();
        String result;
        String status = "ok";
        String errMsg = null;
        try {
            result = dispatch(tool, args);
        } catch (Exception e) {
            log.warn("工具执行失败: name={} err={}", toolName, e.getMessage());
            result = "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
            status = "error";
            errMsg = e.getMessage();
        }
        int dur = (int) (System.currentTimeMillis() - t0);

        // 审计
        FunctionCallLog logEntry = new FunctionCallLog();
        logEntry.setUserId(userId);
        logEntry.setSessionId(sessionId);
        logEntry.setToolName(toolName);
        logEntry.setArguments(argsJson);
        logEntry.setResult(truncate(result, 2000));
        logEntry.setStatus(status);
        logEntry.setErrorMsg(truncate(errMsg, 500));
        logEntry.setDurationMs(dur);
        logEntry.setIp(ip);
        logEntry.setUserAgent(truncate(userAgent, 250));
        try { logMapper.insert(logEntry); } catch (Exception ignore) {}

        return status.equals("ok") ? ToolResult.ok(result, dur) : ToolResult.error(result);
    }

    /**
     * 在 LLM chat 循环中调用 (不带审计, 由 FunctionCallService 统一审计)
     */
    public String executeForChat(String toolName, Map<String, Object> args) throws Exception {
        FunctionTool tool = toolMapper.selectByName(toolName);
        if (tool == null) throw new IllegalArgumentException("tool not found: " + toolName);
        if (tool.getEnabled() == null || tool.getEnabled() != 1) {
            throw new IllegalStateException("tool disabled: " + toolName);
        }
        return dispatch(tool, args);
    }

    private String dispatch(FunctionTool tool, Map<String, Object> args) throws Exception {
        // 内置工具: 找同名 bean
        for (ToolFunction fn : builtinFunctions) {
            if (fn.name().equals(tool.getName())) {
                return fn.execute(args);
            }
        }
        // 自定义工具: HTTP 调用
        if ("user".equals(tool.getScope()) && tool.getEndpoint() != null) {
            return httpCall(tool, args);
        }
        throw new UnsupportedOperationException(
                "no executor for tool: " + tool.getName() + " (endpoint=" + tool.getEndpoint() + ")");
    }

    private String httpCall(FunctionTool tool, Map<String, Object> args) throws Exception {
        // 自定义 HTTP 工具: POST JSON 到 endpoint
        String body = json.writeValueAsString(args);
        java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create(tool.getEndpoint()))
                .timeout(java.time.Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build();
        java.net.http.HttpResponse<String> resp = java.net.http.HttpClient.newHttpClient()
                .send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
        return "{\"http_status\":" + resp.statusCode() + ",\"body\":" + json.writeValueAsString(resp.body()) + "}";
    }

    private String truncate(String s, int n) {
        if (s == null) return null;
        return s.length() > n ? s.substring(0, n) : s;
    }

    public record ToolResult(boolean ok, String result, Integer durationMs) {
        public static ToolResult ok(String r, int d) { return new ToolResult(true, r, d); }
        public static ToolResult error(String r) { return new ToolResult(false, r, 0); }
    }
}
