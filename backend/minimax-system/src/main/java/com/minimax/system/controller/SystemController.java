package com.minimax.system.controller;

import com.minimax.common.result.Result;
import com.minimax.system.dto.SystemSettingsRequest;
import com.minimax.system.entity.SystemSettings;
import com.minimax.system.service.SystemService;
import com.minimax.system.service.SystemSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * System 控制器 (T-system-module).
 *
 * 路径前缀: /api/v1/system
 * - GET  /api/v1/system/menu          左侧菜单 (树形)
 * - GET  /api/v1/system/info          平台信息
 * - GET  /api/v1/system/announcements 公告列表
 * - GET  /api/v1/system/health        健康状态
 * - GET  /api/v1/system/ping          心跳
 * - GET  /api/v1/system/settings      全局系统设置 (T1-backend-apis / P0)
 * - PUT  /api/v1/system/settings      保存全局系统设置 (T1-backend-apis / P0)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@Tag(name = "System", description = "系统通用接口 (菜单/信息/公告/健康/心跳/设置)")
public class SystemController {

    private final SystemService systemService;
    private final SystemSettingsService systemSettingsService;

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

    // ===== T1-backend-apis / P0: 系统全局设置 (修复 views/settings/Index.vue 的 mock 行为) =====

    @GetMapping("/settings")
    @Operation(summary = "取全局系统设置 (单行, id=1)")
    public Result<SystemSettings> getSettings() {
        return Result.success(systemSettingsService.get());
    }

    @PutMapping("/settings")
    @Operation(summary = "保存全局系统设置 (upsert, id=1)")
    public Result<SystemSettings> saveSettings(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody SystemSettingsRequest req) {
        // DTO -> Entity (Service 层用 Entity 即可, 字段名一致)
        SystemSettings patch = new SystemSettings();
        patch.setSiteName(req.getSiteName());
        patch.setSiteLogo(req.getSiteLogo());
        patch.setMaintenanceMode(req.getMaintenanceMode());
        patch.setAllowRegister(req.getAllowRegister());
        patch.setDefaultModelCode(req.getDefaultModelCode());
        patch.setDescription(req.getDescription());
        patch.setContactEmail(req.getContactEmail());
        return Result.success(systemSettingsService.upsert(patch, userId));
    }
}
