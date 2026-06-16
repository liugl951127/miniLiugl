package com.minimax.function.controller;

import com.minimax.common.result.Result;
import com.minimax.function.entity.FunctionCallLog;
import com.minimax.function.entity.FunctionTool;
import com.minimax.function.executor.ToolExecutor;
import com.minimax.function.mapper.FunctionCallLogMapper;
import com.minimax.function.service.FunctionCallService;
import com.minimax.function.service.FunctionToolService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Function Calling 控制器 (Day 9 完整版).
 *
 * 工具 CRUD:
 *   GET    /function/tools                       列所有启用工具
 *   GET    /function/tools/category/{cat}        按分类
 *   GET    /function/tools/{id}                  详情
 *   GET    /function/tools/by-name/{name}        按 name
 *   POST   /function/tools                       注册自定义工具
 *   PUT    /function/tools/{id}                  更新
 *   DELETE /function/tools/{id}                  删除
 *
 * 工具调用:
 *   POST   /function/invoke/{name}               直接调用 (无 LLM)
 *   GET    /function/logs                        我的调用历史
 *
 * Chat (LLM + tool 循环):
 *   POST   /function/chat                        单轮 chat, 可选 enableTools
 */
@RestController
@RequestMapping("/function")
@RequiredArgsConstructor
public class FunctionController {

    private final FunctionToolService toolService;
    private final FunctionCallService fcService;
    private final ToolExecutor executor;
    private final FunctionCallLogMapper logMapper;

    // ---------- Tool CRUD ----------

    @GetMapping("/tools")
    public Result<List<FunctionTool>> listTools(@RequestParam(required = false) String category) {
        return Result.ok(toolService.listByCategory(category));
    }

    @GetMapping("/tools/category/{category}")
    public Result<List<FunctionTool>> listByCategory(@PathVariable String category) {
        return Result.ok(toolService.listByCategory(category));
    }

    @GetMapping("/tools/{id}")
    public Result<FunctionTool> getTool(@PathVariable Long id) {
        return Result.ok(toolService.get(id));
    }

    @GetMapping("/tools/by-name/{name}")
    public Result<FunctionTool> getByName(@PathVariable String name) {
        return Result.ok(toolService.getByName(name));
    }

    @PostMapping("/tools")
    public Result<Long> createTool(@RequestParam Long ownerId,
                                    @RequestBody Map<String, Object> body) {
        Long id = toolService.createUserTool(ownerId,
                (String) body.get("name"),
                (String) body.get("displayName"),
                (String) body.get("description"),
                (String) body.get("parameters"),
                (String) body.get("endpoint"),
                (String) body.get("httpMethod"),
                (String) body.get("tags"));
        return Result.ok(id);
    }

    @PutMapping("/tools/{id}")
    public Result<Boolean> updateTool(@PathVariable Long id,
                                       @RequestParam Long ownerId,
                                       @RequestBody Map<String, Object> body) {
        return Result.ok(toolService.update(id, ownerId,
                (String) body.get("displayName"),
                (String) body.get("description"),
                (String) body.get("parameters"),
                (String) body.get("endpoint"),
                body.get("enabled") == null ? null : ((Number) body.get("enabled")).intValue()));
    }

    @DeleteMapping("/tools/{id}")
    public Result<Boolean> deleteTool(@PathVariable Long id, @RequestParam Long ownerId) {
        return Result.ok(toolService.delete(id, ownerId));
    }

    // ---------- Tool 直接调用 ----------

    @PostMapping("/invoke/{name}")
    public Result<ToolExecutor.ToolResult> invoke(@PathVariable String name,
                                                   @RequestParam Long userId,
                                                   @RequestParam(required = false) Long sessionId,
                                                   @RequestBody(required = false) Map<String, Object> body,
                                                   HttpServletRequest req) {
        String argsJson;
        try {
            argsJson = body == null ? "{}" : new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(body);
        } catch (Exception e) { argsJson = "{}"; }
        String ip = req == null ? null : req.getRemoteAddr();
        String ua = req == null ? null : req.getHeader("User-Agent");
        return Result.ok(executor.invoke(userId, sessionId, name, argsJson, ip, ua));
    }

    @GetMapping("/logs")
    public Result<List<FunctionCallLog>> myLogs(@RequestParam Long userId,
                                                  @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(logMapper.selectByUser(userId, Math.min(limit, 200)));
    }

    // ---------- Chat with tools ----------

    @PostMapping("/chat")
    public Result<FunctionCallService.ChatResult> chat(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        Long sessionId = body.get("sessionId") == null ? null : ((Number) body.get("sessionId")).longValue();
        String message = (String) body.get("message");
        Boolean enableTools = (Boolean) body.getOrDefault("enableTools", true);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) body.get("history");
        @SuppressWarnings("unchecked")
        List<String> toolNames = (List<String>) body.get("toolNames");
        return Result.ok(fcService.chatWithTools(userId, sessionId, message, history, toolNames));
    }
}
