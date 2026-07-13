package com.minimax.ai.raft;

import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Raft 集群控制 REST API (V3.5.0)
 *
 * <p>API:
 *   - GET    /raft/status           集群状态
 *   - POST   /raft/vote             RequestVote RPC (peer 调用)
 *   - POST   /raft/append           AppendEntries RPC (peer 调用)
 *   - POST   /raft/submit           提交业务命令 (客户端)
 *   - GET    /raft/log              日志列表
 *   - POST   /raft/trigger-election 手动触发选举 (调试)
 */
@Tag(name = "Raft 集群")
@RestController
@RequestMapping("/api/v1/ai/raft")
@RequiredArgsConstructor
public class RaftController {

    private final RaftElectionService service;

    @Operation(summary = "Raft 状态")
    @GetMapping("/status")
    public Result<RaftElectionService.RaftStatus> status() {
        return Result.ok(service.status());
    }

    @Operation(summary = "RequestVote RPC")
    @PostMapping("/vote")
    public Result<RaftRpc.VoteResponse> vote(@RequestBody RaftRpc.RequestVote req) {
        return Result.ok(service.handleRequestVote(req));
    }

    @Operation(summary = "AppendEntries RPC")
    @PostMapping("/append")
    public Result<RaftRpc.AppendResponse> append(@RequestBody RaftRpc.AppendEntries req) {
        return Result.ok(service.handleAppendEntries(req));
    }

    @Operation(summary = "提交业务命令 (Leader)")
    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(@RequestBody Map<String, String> body) {
        long idx = service.submit(body.get("command"));
        Map<String, Object> r = new HashMap<>();
        r.put("logIndex", idx);
        r.put("success", idx > 0);
        r.put("status", service.status());
        return Result.ok(r);
    }

    @Operation(summary = "日志列表 (内存)")
    @GetMapping("/log")
    public Result<List<RaftRpc.LogPayload>> log() {
        if (service.getState() == null) return Result.ok(List.of());
        return Result.ok(List.copyOf(service.getState().getLog()));
    }

    @Operation(summary = "手动触发选举 (调试)")
    @PostMapping("/trigger-election")
    public Result<Boolean> trigger() {
        return Result.ok(service.startElection());
    }
}
