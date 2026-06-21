package com.minimax.auth.controller;

import com.minimax.auth.vo.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.minimax.auth.wechat.WechatScanLoginService;
import com.minimax.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

/**
 * 微信扫码登录 Controller (V5).
 *
 * 端点:
 *   GET  /api/v1/auth/wechat/qrcode       生成二维码
 *   GET  /api/v1/auth/wechat/status       轮询状态
 *   GET  /api/v1/auth/wechat/callback     微信回调
 *   GET  /api/v1/auth/wechat/mock-scan    mock 模式模拟扫码
 *   POST /api/v1/auth/wechat/mobile-login 移动端静默登录
 *   GET  /api/v1/auth/wechat/redirect     微信扫完后跳转 (前端用)
 *
 * @since 2026-06
 */
@Slf4j
@Tag(name = "微信扫码登录")
@RestController
@RequestMapping("/auth/wechat")
@RequiredArgsConstructor
public class WechatController {

    private final WechatScanLoginService wechatService;

    @Operation(summary = "生成微信登录二维码")
    @GetMapping("/qrcode")
    public Result<Map<String, Object>> createQrCode(HttpServletRequest req) {
        String ip = getIp(req);
        String ua = req.getHeader("User-Agent");
        return Result.ok(wechatService.createQrCode(ip, ua));
    }

    @Operation(summary = "查询扫码状态")
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus(@RequestParam String ticket) {
        return Result.ok(wechatService.getStatus(ticket));
    }

    /**
     * 微信回调 (微信服务器重定向)
     * URL: GET /api/v1/auth/wechat/callback?code=xxx&state=sceneId
     */
    @Operation(summary = "微信OAuth回调")
    @GetMapping("/callback")
    public Object callback(@RequestParam(required = false) String code,
                           @RequestParam(required = false) String state,
                           @RequestParam(required = false, defaultValue = "/") String redirect) {
        log.info("微信回调: code={} state={}", code, state);
        try {
            Map<String, Object> result = wechatService.handleCallback(code, state);
            // 成功 → 跳转前端, 带 token
            if (Boolean.TRUE.equals(result.get("ok"))) {
                String accessToken = (String) result.get("accessToken");
                String refreshToken = (String) result.get("refreshToken");
                String target = "/wechat-scan-result?ok=1"
                        + "&access_token=" + java.net.URLEncoder.encode(accessToken, java.nio.charset.StandardCharsets.UTF_8)
                        + "&refresh_token=" + java.net.URLEncoder.encode(refreshToken, java.nio.charset.StandardCharsets.UTF_8)
                        + "&user_id=" + result.get("userId");
                return new RedirectView(target);
            }
            String msg = String.valueOf(result.getOrDefault("message", "登录失败"));
            return new RedirectView("/wechat-scan-result?ok=0&msg=" + java.net.URLEncoder.encode(msg, java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("微信回调失败: {}", e.getMessage());
            return new RedirectView("/wechat-scan-result?ok=0&msg=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    /**
     * mock 扫码 (仅 mock 模式可见, 供前端"模拟扫码"按钮)
     * GET /api/v1/auth/wechat/mock-scan?ticket=xxx
     */
    @Operation(summary = "模拟扫码(仅mock模式)")
    @GetMapping("/mock-scan")
    public Result<Map<String, Object>> mockScan(@RequestParam String ticket) {
        return Result.ok(wechatService.mockScan(ticket));
    }

    /**
     * 移动端 (公众号已关注用户 / 小程序) 静默登录
     * POST /api/v1/auth/wechat/mobile-login
     * body: { code, appType: "mp"|"mini" }
     */
    @Operation(summary = "移动端微信静默登录")
    @PostMapping("/mobile-login")
    public Result<LoginResponse> mobileLogin(@RequestBody Map<String, Object> body) {
        String code = (String) body.get("code");
        String appType = (String) body.getOrDefault("appType", "mini");
        return Result.ok(wechatService.mobileLogin(code, appType));
    }

    private String getIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
        return ip;
    }
}
