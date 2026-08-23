package com.minimax.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.admin.service.ClientLogService;
import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 前端日志上报控制器 (V6.8+)
 *
 * 前端通过劫持 console.log/warn/error，将日志批量 POST 到此端点，
 * 后端直接 append 写入 logs/client-{date}.log (JSONL 格式)。
 *
 * 端点:
 *   POST /api/v1/logs/client   — 接收批量日志 (走队列，异步写)
 *   POST /api/v1/logs/save     — 直接 append 写文件（无队列，立即落盘）
 *   GET  /api/v1/logs/client/file — 读取当日日志文件 (ADMIN)
 *   GET  /api/v1/logs/client/files — 列出可用日志文件 (ADMIN)
 */
@Slf4j
@Tag(name = "前端日志")
@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class ClientLogController {

    private static final String LOG_DIR = "logs/client";
    private static final ObjectMapper OM = new ObjectMapper();
    private final ClientLogService clientLogService;

    /**
     * V6.8.1+: 直接 append 写文件（无队列，每次立即落盘）
     * 对应前端 useClientLog 的 _logApi = '/logs/save'
     *
     * 特点:
     *   - 不过 ClientLogService 队列，直接同步写
     *   - 不走 Result 包装，返回纯文本 "ok" 或 "error:xxx"
     *   - 不需要 JWT auth，前端带 _skipAuth: true
     */
    @PostMapping("/save")
    public String saveDirect(@RequestBody List<Map<String, Object>> logs,
                             @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        if (logs == null || logs.isEmpty()) return "ok";
        Long userId = parseUserId(userIdStr);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        Path file = Paths.get(LOG_DIR, "client-" + dateStr + ".log");
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException ignored) {}

        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> entry : logs) {
            if (entry.get("userId") == null && userId != null) {
                entry.put("userId", userId);
            }
            entry.put("_ts", System.currentTimeMillis());
            try {
                sb.append(OM.writeValueAsString(entry)).append('\n');
            } catch (Exception e) {
                sb.append("{\"msg\":\"").append(entry.get("msg") == null ? "" : entry.get("msg"))
                  .append("\",\"level\":\"error\",\"_ts\":").append(System.currentTimeMillis())
                  .append(",\"_err\":\"").append(e.getMessage()).append("\"}\n");
            }
        }

        try {
            Files.writeString(file, sb.toString(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.warn("[LogSave] write failed: {}", e.getMessage());
            return "error:" + e.getMessage();
        }
        return "ok";
    }

    @Operation(summary = "接收前端批量日志 (异步写文件)")
    @PostMapping("/client")
    public Result<Void> receiveClientLogs(@RequestBody List<Map<String, Object>> logs,
                                         @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        if (logs == null || logs.isEmpty()) {
            return Result.ok();
        }
        Long userId = parseUserId(userIdStr);
        for (Map<String, Object> entry : logs) {
            if (entry.get("userId") == null && userId != null) {
                entry.put("userId", userId);
            }
        }
        clientLogService.receiveBatch(logs);
        return Result.ok();
    }

    @Operation(summary = "列出可用日志文件")
    @GetMapping("/client/files")
    @PreAuthorize("hasRole('ADMIN')")  // V6.8.2: 列出日志文件仅管理员
    public Result<List<Map<String, Object>>> listFiles() {
        List<Map<String, Object>> files = new ArrayList<>();
        Path dir = Paths.get(LOG_DIR);
        if (Files.exists(dir)) {
            try (var list = Files.list(dir)) {
                list.filter(p -> p.toString().endsWith(".log"))
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .limit(30)
                    .forEach(p -> {
                        try {
                            var attr = Files.readAttributes(p, BasicFileAttributes.class);
                            files.add(Map.of(
                                "name", p.getFileName().toString(),
                                "size", attr.size(),
                                "modifiedAt", attr.lastModifiedTime().toString(),
                                "path", p.toAbsolutePath().toString()
                            ));
                        } catch (IOException ignored) {}
                    });
            } catch (IOException e) {
                log.warn("列出日志文件失败: {}", e.getMessage());
            }
        }
        return Result.ok(files);
    }

    @Operation(summary = "读取日志文件内容 (最后 N 行)")
    @GetMapping("/client/file")
    @PreAuthorize("hasRole('ADMIN')")  // V6.8.2: 读取日志文件仅管理员
    public Result<Map<String, Object>> readFile(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "200") int lines) {
        String fileDate = (date != null && !date.isBlank()) ? date
                : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        Path file = Paths.get(LOG_DIR, "client-" + fileDate + ".log");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("file", file.getFileName().toString());
        result.put("date", fileDate);
        if (!Files.exists(file)) {
            result.put("content", ""); result.put("lineCount", 0);
            return Result.ok(result);
        }
        try {
            List<String> all = Files.readAllLines(file);
            int from = Math.max(0, all.size() - lines);
            List<String> tail = all.subList(from, all.size());
            result.put("content", String.join("\n", tail));
            result.put("lineCount", all.size());
            result.put("showing", tail.size());
            result.put("fromLine", from + 1);
        } catch (IOException e) {
            result.put("content", "读取失败: " + e.getMessage());
            result.put("lineCount", 0);
        }
        return Result.ok(result);
    }

    /** 安全解析用户 ID，处理 anonymous 等非数字字符串 */
    private Long parseUserId(String userIdStr) {
        if (userIdStr == null || userIdStr.isBlank() || "anonymous".equalsIgnoreCase(userIdStr)) {
            return null;
        }
        try {
            return Long.valueOf(userIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
