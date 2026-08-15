package com.minimax.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 前端日志异步刷盘服务 (V6.8+)
 *
 * 工作机制:
 *   1. 接收前端批量日志 (Batch<LogEntry>)
 *   2. 入队到 BlockingQueue (缓冲 100 条或 3s 强制 flush)
 *   3. 异步线程定时刷写到 logs/client-{date}.log
 *
 * 文件格式 (JSONL — 每行一条):
 *   {"level":"error","msg":"...","url":"...","userId":1,"traceId":"...","time":"ISO","extra":{...}}
 */
@Slf4j
@Service
public class ClientLogService {

    private static final String LOG_DIR = "logs/client";
    private static final int BATCH_SIZE = 50;
    private static final int FLUSH_INTERVAL_MS = 3000;

    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(5000);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "client-log-flusher");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean started = false;

    public ClientLogService() {
        startFlusher();
    }

    private void startFlusher() {
        if (started) return;
        started = true;
        // 每 3s 强制 flush
        scheduler.scheduleAtFixedRate(this::flushAll, FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * 接收前端批量日志
     * @param logs [{level, msg, url, userId, traceId, time, stack, extra}]
     */
    @Async
    public void receiveBatch(List<Map<String, Object>> logs) {
        for (Map<String, Object> entry : logs) {
            try {
                queue.put(formatEntry(entry));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // 队列 > BATCH_SIZE 时触发额外 flush
        if (queue.size() >= BATCH_SIZE) {
            flushAll();
        }
    }

    private String formatEntry(Map<String, Object> e) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"level\":\"").append(esc(e.getOrDefault("level", "info")))
          .append("\",\"msg\":\"").append(esc(e.getOrDefault("msg", "")))
          .append("\",\"url\":\"").append(esc(e.getOrDefault("url", "")))
          .append("\",\"userId\":").append(e.getOrDefault("userId", "null"))
          .append(",\"traceId\":\"").append(esc(String.valueOf(e.getOrDefault("traceId", ""))))
          .append("\",\"time\":\"").append(e.getOrDefault("time", ""))
          .append("\"");
        if (e.get("stack") != null) {
            sb.append(",\"stack\":\"").append(esc(String.valueOf(e.get("stack")))).append("\"");
        }
        if (e.get("extra") != null) {
            sb.append(",\"extra\":").append(e.get("extra"));
        }
        sb.append("}\n");
        return sb.toString();
    }

    private void flushAll() {
        if (queue.isEmpty()) return;
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        Path logFile = Paths.get(LOG_DIR, "client-" + today + ".log");
        List<String> drained = new ArrayList<>(BATCH_SIZE);
        queue.drainTo(drained, BATCH_SIZE);
        if (drained.isEmpty()) return;
        String batch = String.join("", drained);
        try {
            Files.createDirectories(logFile.getParent());
            Files.writeString(logFile, batch,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("[ClientLog] 刷盘失败: {}", e.getMessage());
        }
    }

    /** 下线时 flush 剩余日志 */
    public void shutdown() {
        flushAll();
        scheduler.shutdown();
    }

    private static String esc(Object v) {
        if (v == null) return "";
        return String.valueOf(v)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
