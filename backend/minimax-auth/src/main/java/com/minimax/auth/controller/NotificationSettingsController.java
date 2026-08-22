package com.minimax.auth.controller;

import com.minimax.auth.dto.NotificationSettingsRequest;
import com.minimax.auth.entity.NotificationSettings;
import com.minimax.auth.service.NotificationSettingsService;
import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 通知设置 Controller (T1-backend-apis / P0)
 *
 * 2 个端点 (修复 views/notification/Index.vue saveSettings() 的 mock 行为):
 * <ul>
 *   <li>GET /api/v1/notification/settings  取当前用户设置 (无则返回默认)</li>
 *   <li>PUT /api/v1/notification/settings  保存设置 (upsert by userId)</li>
 * </ul>
 *
 * 当前用户通过 gateway 注入的 X-User-Id header 拿, 与 notification 列表一致。
 *
 * @since V7.2
 */
@Slf4j
@Tag(name = "通知设置 (V7.2 P0)")
@RestController
@RequestMapping("/api/v1/notification/settings")
@RequiredArgsConstructor
public class NotificationSettingsController {

    private final NotificationSettingsService settingsService;

    @Operation(summary = "取当前用户的通知设置 (无则返回默认)")
    @GetMapping
    public Result<NotificationSettings> get(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.ok(settingsService.get(userId));
    }

    @Operation(summary = "保存当前用户的通知设置 (upsert by X-User-Id)")
    @PutMapping
    public Result<NotificationSettings> save(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                              @Valid @RequestBody NotificationSettingsRequest req) {
        return Result.ok(settingsService.save(
                userId, req.getChannels(), req.getEvents(),
                req.getQuietStart(), req.getQuietEnd()));
    }
}
