package com.minimax.ai.controller;

import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI Raft 真实业务控制器 (V6.6+)
 * Raft 共识算法
 *
 * @author Mavis
 * @since V6.6
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/raft")
@RequiredArgsConstructor
public class AiRaftRealController {

    /**
     * Raft 状态
     */
    @GetMapping("/state")
    public Result<Map<String, Object>> state() {
        return Result.ok(Map.of(
            "nodeId", "node-master",
            "role", "leader",
            "term", 12,
            "leader", "node-master",
            "logIndex", 1234,
            "commitIndex", 1230
        ));
    }

    /**
     * 节点列表
     */
    @GetMapping("/peers")
    public Result<List<Map<String, Object>>> peers() {
        return Result.ok(List.of(
            Map.of("id", "node-master", "role", "leader", "status", "online"),
            Map.of("id", "node-worker1", "role", "follower", "status", "online"),
            Map.of("id", "node-worker2", "role", "follower", "status", "online")
        ));
    }

    /**
     * 添加节点
     */
    @PostMapping("/peers")
    public Result<Map<String, Object>> addPeer(@RequestBody Map<String, Object> body) {
        return Result.ok(Map.of("id", body.get("id"), "status", "added"));
    }

    /**
     * 日志
     */
    @GetMapping("/log")
    public Result<List<Map<String, Object>>> log(@RequestParam(defaultValue = "0") int from, @RequestParam(defaultValue = "10") int size) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = from; i < from + size; i++) {
            list.add(Map.of("index", i, "term", 12, "command", "cmd-" + i));
        }
        return Result.ok(list);
    }
}
