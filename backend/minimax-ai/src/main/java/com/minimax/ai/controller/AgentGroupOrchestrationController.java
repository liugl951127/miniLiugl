package com.minimax.ai.controller;

import com.minimax.ai.marketplace.AgentGroupMember;
import com.minimax.ai.marketplace.AgentGroupMemberService;
import com.minimax.ai.marketplace.AgentGroupOrchestrator;
import com.minimax.ai.marketplace.orchestrator.DebateStrategy;
import com.minimax.ai.marketplace.orchestrator.GroupStrategy;
import com.minimax.ai.marketplace.orchestrator.ParallelStrategy;
import com.minimax.ai.marketplace.orchestrator.PipelineStrategy;
import com.minimax.common.exception.BizException;
import com.minimax.common.result.Result;
import com.minimax.common.result.ResultCode;
import com.minimax.common.security.JwtAuthenticationFilter.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多智能体群编排 REST API (T1-backend-orchestrator)
 *
 * <p>前缀: {@code /api/v1/agent-group/}
 *
 * <h3>端点</h3>
 * <ul>
 *   <li>GET    /{groupId}/members          — 列出全部成员 (含 disabled)</li>
 *   <li>POST   /{groupId}/members          — 新增成员 (body: agentCode/role/position/configJson)</li>
 *   <li>PUT    /{groupId}/members/{mid}    — 更新成员</li>
 *   <li>DELETE /{groupId}/members/{mid}    — 删除成员</li>
 *   <li>PUT    /{groupId}/members/reorder  — 重排 (body: [{memberId, position}, ...])</li>
 *   <li>POST   /{groupId}/run              — 流式执行 (SSE), body: {goal, tools, strategy}</li>
 *   <li>GET    /strategies                 — 列出可用策略 (PIPELINE/PARALLEL/DEBATE)</li>
 * </ul>
 *
 * @author MiniMax
 * @since T1
 */
@Tag(name = "智能体群编排 (AgentGroup Orchestration)")
@Slf4j
@RestController
@RequestMapping("/api/v1/agent-group")
@RequiredArgsConstructor
public class AgentGroupOrchestrationController {

    private final AgentGroupMemberService memberService;
    private final AgentGroupOrchestrator orchestrator;

    // ---------- 成员 CRUD ----------

    @Operation(summary = "列出群组全部成员")
    @GetMapping("/{groupId}/members")
    public Result<List<AgentGroupMember>> listMembers(@PathVariable Long groupId,
                                                      @AuthenticationPrincipal AuthenticatedUser user) {
        if (user == null) throw new BizException(ResultCode.UNAUTHORIZED, "未登录");
        return Result.ok(memberService.listByGroupId(groupId));
    }

    @Operation(summary = "新增成员")
    @PostMapping("/{groupId}/members")
    public Result<AgentGroupMember> addMember(@PathVariable Long groupId,
                                              @RequestBody Map<String, Object> body,
                                              @AuthenticationPrincipal AuthenticatedUser user) {
        if (user == null) throw new BizException(ResultCode.UNAUTHORIZED, "未登录");
        try {
            AgentGroupMember m = new AgentGroupMember();
            m.setAgentCode((String) body.get("agentCode"));
            m.setRole((String) body.getOrDefault("role", "WORKER"));
            if (body.get("position") != null) {
                m.setPosition(((Number) body.get("position")).intValue());
            }
            m.setConfigJson((String) body.getOrDefault("configJson", ""));
            if (body.get("enabled") != null) {
                m.setEnabled(((Number) body.get("enabled")).intValue());
            }
            return Result.ok(memberService.addMember(groupId, m));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        } catch (Exception e) {
            log.error("[orch-ctrl] addMember 异常", e);
            return Result.fail(500, "新增成员失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新成员")
    @PutMapping("/{groupId}/members/{memberId}")
    public Result<AgentGroupMember> updateMember(@PathVariable Long groupId,
                                                 @PathVariable Long memberId,
                                                 @RequestBody Map<String, Object> body,
                                                 @AuthenticationPrincipal AuthenticatedUser user) {
        if (user == null) throw new BizException(ResultCode.UNAUTHORIZED, "未登录");
        try {
            AgentGroupMember m = new AgentGroupMember();
            if (body.get("agentCode") != null) m.setAgentCode((String) body.get("agentCode"));
            if (body.get("role") != null) m.setRole((String) body.get("role"));
            if (body.get("position") != null) m.setPosition(((Number) body.get("position")).intValue());
            if (body.get("configJson") != null) m.setConfigJson((String) body.get("configJson"));
            if (body.get("enabled") != null) m.setEnabled(((Number) body.get("enabled")).intValue());
            AgentGroupMember updated = memberService.updateMember(groupId, memberId, m);
            if (updated == null) {
                return Result.fail(404, "成员不存在或 groupId 不匹配");
            }
            return Result.ok(updated);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        } catch (Exception e) {
            log.error("[orch-ctrl] updateMember 异常", e);
            return Result.fail(500, "更新成员失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除成员")
    @DeleteMapping("/{groupId}/members/{memberId}")
    public Result<Boolean> removeMember(@PathVariable Long groupId,
                                        @PathVariable Long memberId,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
        if (user == null) throw new BizException(ResultCode.UNAUTHORIZED, "未登录");
        boolean ok = memberService.removeMember(groupId, memberId);
        if (!ok) {
            return Result.fail(404, "成员不存在或 groupId 不匹配");
        }
        return Result.ok(true);
    }

    @Operation(summary = "批量重排 (按入参顺序从 0 开始重写 position)")
    @PutMapping("/{groupId}/members/reorder")
    public Result<Map<String, Object>> reorderMembers(@PathVariable Long groupId,
                                                       @RequestBody List<Map<String, Object>> orders,
                                                       @AuthenticationPrincipal AuthenticatedUser user) {
        if (user == null) throw new BizException(ResultCode.UNAUTHORIZED, "未登录");
        if (orders == null || orders.isEmpty()) {
            return Result.fail(400, "orders 不能为空");
        }
        int n = memberService.reorder(groupId, orders);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("updated", n);
        data.put("total",   orders.size());
        return Result.ok(data);
    }

    // ---------- 流式执行 ----------

    @Operation(summary = "流式执行群任务 (SSE), strategy 默认 PIPELINE")
    @PostMapping(value = "/{groupId}/run", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter run(@PathVariable Long groupId,
                          @RequestBody(required = false) Map<String, Object> body,
                          @AuthenticationPrincipal AuthenticatedUser user) {
        // T1-backend-auth-audit: 强制鉴权, SSE 需登录用户才能运行
        if (user == null) throw new BizException(ResultCode.UNAUTHORIZED, "未登录, SSE 需鉴权");
        String goal = body == null ? null : (String) body.get("goal");
        @SuppressWarnings("unchecked")
        List<String> tools = body == null ? new ArrayList<>()
                : (List<String>) body.getOrDefault("tools", new ArrayList<>());
        String strategy = body == null ? null : (String) body.get("strategy");
        log.info("[orch-ctrl] run groupId={} userId={} strategy={} goal-len={} tools={}",
                groupId, user.id(), strategy, goal == null ? 0 : goal.length(), tools);
        return orchestrator.runWithStrategy(user.id(), groupId, goal, tools, strategy);
    }

    // ---------- 元信息 ----------

    @Operation(summary = "列出可用编排策略")
    @GetMapping("/strategies")
    public Result<List<Map<String, Object>>> strategies() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(describe(PipelineStrategy.NAME, "顺序执行, 上一步输出作下一步输入",
                "适合串行工作流: 分析 → 写作 → 审核"));
        list.add(describe(ParallelStrategy.NAME, "并行执行, 合并全部结果",
                "适合多视角并行: 多个 worker 同一问题"));
        list.add(describe(DebateStrategy.NAME, "MANAGER 提问 → N WORKER 回答 → CRITIC 评分 → 选最佳",
                "适合需要多方案比较的决策"));
        return Result.ok(list);
    }

    private Map<String, Object> describe(String name, String desc, String usage) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name",        name);
        m.put("description", desc);
        m.put("usage",       usage);
        return m;
    }

    // ---------- 工具: GroupStrategy 选择校验 (供扩展) ----------
    @SuppressWarnings("unused")
    private GroupStrategy coerce(String s) {
        if (s == null) return null;
        return switch (s.toUpperCase()) {
            case "PIPELINE" -> null; // bean via constructor
            case "PARALLEL" -> null;
            case "DEBATE"   -> null;
            default -> null;
        };
    }
}
