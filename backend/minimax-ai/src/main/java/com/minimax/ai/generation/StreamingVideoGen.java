package com.minimax.ai.generation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 视频流式生成器 (V2.7.6 - SSE 实时进度推送)
 *
 * <h3>核心特性</h3>
 * <ul>
 *   <li><b>Server-Sent Events</b>: 服务端主动推帧, 客户端实时渲染</li>
 *   <li><b>增量编码</b>: 每生成 N 帧推一次, 不用等完整视频</li>
 *   <li><b>多任务并发</b>: 每个 emitter 一个 taskId, 互不干扰</li>
 *   <li><b>可取消</b>: 客户端断开自动停止, 服务端支持 cancel()</li>
 *   <li><b>进度统计</b>: fps / 总帧 / 已完成 / 预估剩余</li>
 * </ul>
 *
 * <h3>事件类型</h3>
 * <pre>
 *   {event:"start", taskId, totalFrames, fps, ...}
 *   {event:"frame", taskId, index, data, ts}     // 每帧 Base64 PNG
 *   {event:"progress", taskId, done, total, percent, etaMs}
 *   {event:"complete", taskId, durationMs, totalBytes}
 *   {event:"error", taskId, message}
 * </pre>
 *
 * <h3>算法</h3>
 * <p>单线程执行 + ScheduledExecutor 定时心跳 (每 5s 一次), 避免反向代理超时</p>
 * <p>帧之间用 Thread.sleep 模拟生成时间, 真实场景接 GPU/FFmpeg</p>
 */
@Slf4j
@Component
public class StreamingVideoGen {

    public static class StreamConfig {
        public String taskId;
        public int width = 640;
        public int height = 360;
        public int fps = 12;
        public int durationSeconds = 6;
        public String title = "Streaming Demo";
        public int chunkSize = 5;       // 每 N 帧推一次
        public long frameIntervalMs = 80; // 帧间间隔 (模拟生成耗时)
    }

    public static class ProgressInfo {
        public String taskId;
        public int totalFrames;
        public int doneFrames;
        public int percent;
        public long elapsedMs;
        public long etaMs;
        public long totalBytes;
    }

    /** 任务状态: 用于 cancel 时的查询 */
    public static class StreamStatus {
        public String taskId;
        public String state;          // PENDING/RUNNING/COMPLETED/FAILED/CANCELLED
        public ProgressInfo progress;
        public String error;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("taskId", taskId);
            m.put("state", state);
            m.put("progress", progress);
            m.put("error", error);
            return m;
        }
    }

    private final Map<String, StreamStatus> tasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeat =
            Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "sse-heartbeat");
                t.setDaemon(true);
                return t;
            });

    public Map<String, StreamStatus> tasks() { return tasks; }

    /**
     * 注册一个 emitter 并启动流式生成
     */
    public SseEmitter stream(StreamConfig cfg) {
        if (cfg.taskId == null) cfg.taskId = "stream-" + System.currentTimeMillis();

        SseEmitter emitter = new SseEmitter(0L);  // 无超时
        StreamStatus status = new StreamStatus();
        status.taskId = cfg.taskId;
        status.state = "PENDING";
        status.progress = new ProgressInfo();
        tasks.put(cfg.taskId, status);

        int totalFrames = cfg.fps * cfg.durationSeconds;
        status.progress.totalFrames = totalFrames;
        status.progress.taskId = cfg.taskId;

        emitter.onCompletion(() -> markCancelled(cfg.taskId));
        emitter.onTimeout(() -> markCancelled(cfg.taskId));
        emitter.onError(t -> markFailed(cfg.taskId, t.getMessage()));

        // 心跳 (反向代理超时保护)
        ScheduledFuture<?> hb = heartbeat.scheduleAtFixedRate(() -> {
            if (status.state.equals("RUNNING")) {
                try { emitter.send(SseEmitter.event().name("heartbeat").data("ping")); } catch (Exception ignored) {}
            }
        }, 5, 5, TimeUnit.SECONDS);

        // 异步生成
        new Thread(() -> {
            try {
                long start = System.currentTimeMillis();
                status.state = "RUNNING";
                sendStart(emitter, cfg, totalFrames);

                long totalBytes = 0;
                boolean cancelled = false;
                for (int i = 0; i < totalFrames; i++) {
                    if (!"RUNNING".equals(status.state)) {
                        log.info("Task {} cancelled at frame {}/{}", cfg.taskId, i, totalFrames);
                        cancelled = true;
                        break;
                    }
                    // 1. 生成单帧
                    String frameBase64 = renderFrame(cfg, i, totalFrames);
                    totalBytes += frameBase64.length();

                    // 2. 每 chunkSize 帧推一次 (省带宽)
                    boolean isLast = (i == totalFrames - 1);
                    if ((i + 1) % cfg.chunkSize == 0 || isLast) {
                        sendFrame(emitter, cfg.taskId, i, frameBase64, isLast);
                    }

                    // 3. 更新进度
                    long elapsed = System.currentTimeMillis() - start;
                    long eta = i > 0 ? (elapsed * (totalFrames - i - 1) / (i + 1)) : 0;
                    status.progress.doneFrames = i + 1;
                    status.progress.percent = (int) ((i + 1) * 100 / totalFrames);
                    status.progress.elapsedMs = elapsed;
                    status.progress.etaMs = eta;
                    status.progress.totalBytes = totalBytes;

                    // 4. 进度事件 (每 10%)
                    if ((i + 1) % Math.max(1, totalFrames / 10) == 0 || isLast) {
                        sendProgress(emitter, status.progress);
                    }

                    // 5. 模拟生成耗时
                    Thread.sleep(cfg.frameIntervalMs);
                }

                // 完成
                long total = System.currentTimeMillis() - start;
                if (!cancelled) {
                    status.state = "COMPLETED";
                    sendComplete(emitter, cfg.taskId, total, totalBytes);
                } else {
                    try { emitter.complete(); } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                log.error("Stream failed", e);
                markFailed(cfg.taskId, e.getMessage());
                try { emitter.send(SseEmitter.event().name("error").data(Map.of("message", e.getMessage()))); } catch (IOException ignored) {}
                emitter.completeWithError(e);
            } finally {
                hb.cancel(false);
            }
        }, "stream-gen-" + cfg.taskId).start();

        return emitter;
    }

    /**
     * 取消任务
     */
    public boolean cancel(String taskId) {
        StreamStatus s = tasks.get(taskId);
        if (s == null) return false;
        s.state = "CANCELLED";
        return true;
    }

    private void markCancelled(String taskId) {
        StreamStatus s = tasks.get(taskId);
        if (s != null && s.state.equals("RUNNING")) s.state = "CANCELLED";
    }

    private void markFailed(String taskId, String err) {
        StreamStatus s = tasks.get(taskId);
        if (s != null) {
            s.state = "FAILED";
            s.error = err;
        }
    }

    // === 渲染单帧 (简化, 用 AWT 画布) ===
    private String renderFrame(StreamConfig cfg, int idx, int total) {
        try {
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                    cfg.width, cfg.height, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = img.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

            // 背景色按帧渐变 (clamp 0-255)
            float ratio = (float) idx / total;
            int r = Math.max(0, Math.min(255, (int) (15 + 50 * Math.sin(ratio * Math.PI))));
            int gr = Math.max(0, Math.min(255, (int) (23 + 80 * Math.sin(ratio * Math.PI * 2))));
            int b = Math.max(0, Math.min(255, (int) (42 + 100 * Math.cos(ratio * Math.PI))));
            g.setColor(new java.awt.Color(r, gr, b));
            g.fillRect(0, 0, cfg.width, cfg.height);

            // 标题
            g.setColor(java.awt.Color.WHITE);
            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, cfg.width / 16));
            g.drawString(cfg.title, 30, 50);

            // 帧编号
            g.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, cfg.width / 24));
            g.drawString(String.format("Frame %d / %d (%.1f%%)", idx + 1, total, ratio * 100),
                    30, cfg.height - 30);

            // 进度条
            int barW = cfg.width - 60;
            int barH = 8;
            int barY = cfg.height / 2;
            g.setColor(new java.awt.Color(255, 255, 255, 60));
            g.fillRect(30, barY, barW, barH);
            g.setColor(new java.awt.Color(59, 130, 246));
            g.fillRect(30, barY, (int) (barW * ratio), barH);

            g.dispose();

            // 编码 PNG -> Base64
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "png", baos);
            return java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("Render frame failed", e);
            return "";
        }
    }

    // === SSE 事件发送 ===
    private void sendStart(SseEmitter e, StreamConfig cfg, int total) throws IOException {
        e.send(SseEmitter.event().name("start").data(Map.of(
                "taskId", cfg.taskId,
                "totalFrames", total,
                "fps", cfg.fps,
                "durationSeconds", cfg.durationSeconds,
                "width", cfg.width,
                "height", cfg.height,
                "title", cfg.title
        )));
    }

    private void sendFrame(SseEmitter e, String taskId, int idx, String base64, boolean last) throws IOException {
        e.send(SseEmitter.event().name("frame").data(Map.of(
                "taskId", taskId,
                "index", idx,
                "data", base64,
                "isLast", last
        )));
    }

    private void sendProgress(SseEmitter e, ProgressInfo p) throws IOException {
        e.send(SseEmitter.event().name("progress").data(Map.of(
                "taskId", p.taskId,
                "done", p.doneFrames,
                "total", p.totalFrames,
                "percent", p.percent,
                "elapsedMs", p.elapsedMs,
                "etaMs", p.etaMs,
                "totalBytes", p.totalBytes
        )));
    }

    private void sendComplete(SseEmitter e, String taskId, long dur, long bytes) throws IOException {
        e.send(SseEmitter.event().name("complete").data(Map.of(
                "taskId", taskId,
                "durationMs", dur,
                "totalBytes", bytes
        )));
    }
}
