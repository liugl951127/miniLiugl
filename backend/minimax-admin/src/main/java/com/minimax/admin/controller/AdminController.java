package com.minimax.admin.controller;

import com.minimax.admin.entity.AdminAuditLog;
import com.minimax.admin.service.AuditService;
import com.minimax.admin.service.HealthAggregator;
import com.minimax.admin.service.ModelMgmtService;
import com.minimax.admin.service.StatsService;
import com.minimax.admin.service.UserMgmtService;
import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin 后台 控制器 (Day 10).
 *
 * 用户管理 (UserMgmtService - 代理 auth):
 *   GET    /admin/users                       列表
 *   GET    /admin/users/{id}                  详情
 *   POST   /admin/users                       注册
 *   POST   /admin/users/{id}/reset-password   重置密码
 *   PUT    /admin/users/{id}/status           启停
 *
 * 模型管理 (ModelMgmtService - 代理 model):
 *   GET    /admin/models/providers            列出 Provider
 *   GET    /admin/models                      列出 Config
 *   PUT    /admin/models/{code}/rate-limit    调限流
 *
 * 统计:
 *   GET    /admin/stats/ops                   操作统计
 *   GET    /admin/stats/dashboard             一页 dashboard
 *
 * 监控:
 *   GET    /admin/health                      跨服务 health
 *   GET    /admin/ping                        心跳
 *
 * 审计:
 *   GET    /admin/audit/recent                最近审计
 *   GET    /admin/audit/by-actor/{id}         按操作人
 */
@Tag(name = "系统管理")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserMgmtService userMgmt;
    private final ModelMgmtService modelMgmt;
    private final StatsService stats;
    private final HealthAggregator health;
    private final AuditService audit;

    // ---------- 用户管理 ----------

    @Operation(summary = "用户列表")
    @GetMapping("/users")
    public Result<String> listUsers(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return Result.ok(userMgmt.listUsers(page, size));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/users/{id}")
    public Result<String> getUser(@PathVariable Long id) {
        return Result.ok(userMgmt.getUser(id));
    }

    @Operation(summary = "创建用户")
    @PostMapping("/users")
    public Result<String> createUser(@RequestParam Long actorId,
                                      @RequestParam(required = false) String actorName,
                                      @RequestBody Map<String, Object> body,
                                      HttpServletRequest req) {
        return Result.ok(userMgmt.createUser(actorId, actorName, body, req));
    }

    @Operation(summary = "重置用户密码")
    @PostMapping("/users/{id}/reset-password")
    public Result<String> resetPassword(@PathVariable("id") Long userId,
                                          @RequestParam Long actorId,
                                          @RequestParam(required = false) String actorName,
                                          @RequestBody Map<String, String> body,
                                          HttpServletRequest req) {
        return Result.ok(userMgmt.resetPassword(actorId, actorName, userId, body.get("newPassword"), req));
    }

    @Operation(summary = "启用/禁用用户")
    @PutMapping("/users/{id}/status")
    public Result<String> toggleUser(@PathVariable("id") Long userId,
                                      @RequestParam Long actorId,
                                      @RequestParam(required = false) String actorName,
                                      @RequestParam boolean enabled,
                                      HttpServletRequest req) {
        return Result.ok(userMgmt.toggleUser(actorId, actorName, userId, enabled, req));
    }

    // ---------- 模型管理 ----------

    @Operation(summary = "列出模型提供商")
    @GetMapping("/models/providers")
    public Result<String> listProviders() {
        return Result.ok(modelMgmt.listProviders());
    }

    @Operation(summary = "列出模型配置")
    @GetMapping("/models")
    public Result<String> listConfigs() {
        return Result.ok(modelMgmt.listConfigs());
    }

    @Operation(summary = "更新模型限流配置")
    @PutMapping("/models/{code}/rate-limit")
    public Result<String> updateRateLimit(@PathVariable("code") String code,
                                           @RequestParam Long actorId,
                                           @RequestParam(required = false) String actorName,
                                           @RequestBody Map<String, Object> body,
                                           HttpServletRequest req) {
        int capacity = ((Number) body.getOrDefault("capacity", 10)).intValue();
        int refill = ((Number) body.getOrDefault("refillPerMinute", 60)).intValue();
        return Result.ok(modelMgmt.updateRateLimit(actorId, actorName, code, capacity, refill, req));
    }

    // ---------- 统计 ----------

    @Operation(summary = "操作统计")
    @GetMapping("/stats/ops")
    public Result<Map<String, Object>> opsStats() {
        return Result.ok(stats.opsStats());
    }

    @Operation(summary = "仪表盘统计")
    @GetMapping("/stats/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(stats.dashboard());
    }

    // ---------- 监控 ----------

    @Operation(summary = "跨服务健康检查")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.ok(health.aggregate());
    }

    @Operation(summary = "心跳检测")
    @GetMapping("/ping")
    public Result<Map<String, String>> ping() {
        return Result.ok(Map.of("pong", "ok", "ts", String.valueOf(System.currentTimeMillis())));
    }

    // ---------- 审计 ----------

    @Operation(summary = "最近审计日志")
    @GetMapping("/audit/recent")
    public Result<List<AdminAuditLog>> recentAudit(@RequestParam(defaultValue = "50") int limit) {
        return Result.ok(audit.recent(limit));
    }

    @Operation(summary = "按操作人查审计日志")
    @GetMapping("/audit/by-actor/{id}")
    public Result<List<AdminAuditLog>> auditByActor(@PathVariable("id") Long actorId,
                                                     @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(audit.byActor(actorId, limit));
    }
}