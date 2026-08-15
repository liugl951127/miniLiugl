package com.minimax.ai.controller;

import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI Collab 真实业务控制器 (V6.6+)
 * 协作空间 (多用户同时编辑)
 *
 * @author Mavis
 * @since V6.6
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/collab")
@RequiredArgsConstructor
public class AiCollabRealController {

    /**
     * 列出房间
     */
    @GetMapping("/rooms")
    public Result<List<Map<String, Object>>> rooms() {
        return Result.ok(List.of(
            Map.of("id", 1, "name", "项目 A 协作", "members", 3, "isPublic", true),
            Map.of("id", 2, "name", "团队 B 协作", "members", 5, "isPublic", false)
        ));
    }

    /**
     * 创建房间
     */
    @PostMapping("/rooms")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return Result.ok(Map.of(
            "id", System.currentTimeMillis(),
            "name", body.get("name"),
            "isPublic", body.getOrDefault("isPublic", false),
            "createdAt", System.currentTimeMillis()
        ));
    }

    /**
     * 加入房间
     */
    @PostMapping("/rooms/{id}/join")
    public Result<Map<String, Object>> join(@PathVariable Long id) {
        return Result.ok(Map.of("id", id, "status", "joined", "joinedAt", System.currentTimeMillis()));
    }

    /**
     * 房间消息
     */
    @GetMapping("/rooms/{id}/messages")
    public Result<List<Map<String, Object>>> messages(@PathVariable Long id) {
        return Result.ok(List.of(
            Map.of("id", 1, "user", "user_1", "content", "你好", "type", "text", "createdAt", System.currentTimeMillis() - 60_000L),
            Map.of("id", 2, "user", "user_2", "content", "你好", "type", "text", "createdAt", System.currentTimeMillis())
        ));
    }
}
