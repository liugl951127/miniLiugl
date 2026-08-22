package com.minimax.system.controller;

import com.minimax.common.result.Result;
import com.minimax.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * System 控制器 (T-system-module).
 *
 * 路径前缀: /api/v1/system
 * - GET /api/v1/system/menu          左侧菜单 (树形)
 * - GET /api/v1/system/info          平台信息
 * - GET /api/v1/system/announcements 公告列表
 * - GET /api/v1/system/health        健康状态
 * - GET /api/v1/system/ping          心跳
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@Tag(name = "System", description = "系统通用接口 (菜单/信息/公告/健康/心跳)")
public class SystemController {

    private final SystemService systemService;

    @GetMapping("/menu")
    @Operation(summary = "获取左侧菜单")
    public Result<List<Map<String, Object>>> menu() {
        return Result.success(systemService.getMenu());
    }

    @GetMapping("/info")
    @Operation(summary = "获取平台信息")
    public Result<Map<String, Object>> info() {
        return Result.success(systemService.getPlatformInfo());
    }

    @GetMapping("/announcements")
    @Operation(summary = "获取公告列表")
    public Result<List<Map<String, Object>>> announcements() {
        return Result.success(systemService.getAnnouncements());
    }

    @GetMapping("/health")
    @Operation(summary = "健康检查 (服务列表)")
    public Result<Map<String, Object>> health() {
        return Result.success(systemService.getHealth());
    }

    @GetMapping("/ping")
    @Operation(summary = "心跳 (ping/pong)")
    public Result<Map<String, Object>> ping() {
        return Result.success(systemService.ping());
    }
}
