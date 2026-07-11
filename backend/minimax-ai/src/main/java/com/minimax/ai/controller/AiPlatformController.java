package com.minimax.ai.controller;

import com.minimax.ai.codegen.ProjectCodeGenerator;
import com.minimax.ai.compliance.AuditLogger;
import com.minimax.ai.compliance.DataMasker;
import com.minimax.ai.datasource.DynamicDataSource;
import com.minimax.ai.entity.AiTool;
import com.minimax.ai.entity.DbDataSource;
import com.minimax.ai.entity.MultimediaFile;
import com.minimax.ai.entity.AiGenerationLog;
import com.minimax.ai.generation.AnimationGenerator;
import com.minimax.ai.generation.ChartGenerator;
import com.minimax.ai.generation.DashboardBuilder;
import com.minimax.ai.generation.KeywordEngine;
import com.minimax.ai.generation.MusicGenerator;
import com.minimax.ai.generation.Nl2Chart;
import com.minimax.ai.generation.VideoComposer;
import com.minimax.ai.mapper.AiGenerationLogMapper;
import com.minimax.ai.mapper.AiToolMapper;
import com.minimax.ai.mapper.DataSourceMapper;
import com.minimax.ai.mapper.MultimediaFileMapper;
import com.minimax.ai.tool.AiToolExecutor;
import com.minimax.ai.tool.AiToolRegistry;
import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * AI 平台统一接口 (V2.7)
 *
 * 提供前端调用的所有 AI 能力:
 *   - 报表生成 (chart/render)
 *   - 音乐生成 (music/generate)
 *   - 动画生成 (animation/*)
 *   - 视频合成 (video/compose)
 *   - 数据看板 (dashboard/render)
 *   - 关键词路由 (route)
 *   - NL2Chart (nl2chart)
 *   - 代码生成 (codegen)
 *
 * 所有接口统一前缀 /api/ai
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiPlatformController {

    private final ChartGenerator chartGenerator;
    private final MusicGenerator musicGenerator;
    private final AnimationGenerator animationGenerator;
    private final VideoComposer videoComposer;
    private final DashboardBuilder dashboardBuilder;
    private final Nl2Chart nl2Chart;
    private final KeywordEngine keywordEngine;
    private final ProjectCodeGenerator codeGenerator;
    private final DynamicDataSource dynamicDataSource;
    private final AiToolRegistry toolRegistry;
    private final AiToolMapper toolMapper;
    private final DataSourceMapper dataSourceMapper;
    private final MultimediaFileMapper fileMapper;
    private final AiGenerationLogMapper generationLogMapper;
    private final AuditLogger auditLogger;
    private final DataMasker dataMasker;

    // ==================== 图表 (PNG) ====================

    /**
     * 渲染图表
     * POST /api/ai/chart/render
     * Body: ChartData JSON
     * Response: image/png
     */
    @PostMapping(value = "/chart/render", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> renderChart(@RequestBody ChartGenerator.ChartData data) {
        long start = System.currentTimeMillis();
        try {
            byte[] png = chartGenerator.render(data);
            auditLogger.log("AI_CHART", null, null, null, null, "ai", null, "POST",
                    "/api/ai/chart/render", data.title, 200, "SUCCESS", System.currentTimeMillis() - start);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(png.length);
            headers.set("Content-Disposition", "inline; filename=\"chart.png\"");
            return new ResponseEntity<>(png, headers, 200);
        } catch (Exception e) {
            log.error("Chart render failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== 音乐 (MIDI) ====================

    /**
     * 生成音乐 (MIDI)
     * POST /api/ai/music/generate
     * Body: MusicConfig JSON
     * Response: audio/midi
     */
    @PostMapping(value = "/music/generate", produces = "audio/midi")
    public ResponseEntity<byte[]> generateMusic(@RequestBody MusicGenerator.MusicConfig config) {
        long start = System.currentTimeMillis();
        try {
            byte[] midi = musicGenerator.generate(config);
            auditLogger.log("AI_MUSIC", null, null, null, null, "ai", null, "POST",
                    "/api/ai/music/generate", config.key, 200, "SUCCESS", System.currentTimeMillis() - start);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/midi"));
            headers.setContentLength(midi.length);
            headers.set("Content-Disposition", "attachment; filename=\"music.mid\"");
            return new ResponseEntity<>(midi, headers, 200);
        } catch (Exception e) {
            log.error("Music generate failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== 动画 (GIF) ====================

    /**
     * 生成文字淡入 GIF
     * POST /api/ai/animation/text-fade
     */
    @PostMapping(value = "/animation/text-fade", produces = "image/gif")
    public ResponseEntity<byte[]> generateTextFade(@RequestBody Map<String, Object> body) {
        try {
            String text = (String) body.get("text");
            int frames = body.get("frames") != null ? ((Number) body.get("frames")).intValue() : 30;
            int delay = body.get("delay") != null ? ((Number) body.get("delay")).intValue() : 100;
            int width = body.get("width") != null ? ((Number) body.get("width")).intValue() : 600;
            int height = body.get("height") != null ? ((Number) body.get("height")).intValue() : 400;
            AnimationGenerator.AnimationConfig cfg = AnimationGenerator.AnimationConfig.builder()
                    .size(width, height).text(text).frames(frames).frameDelayMs(delay).build();
            byte[] gif = animationGenerator.generateTextFadeIn(cfg);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("image/gif"));
            headers.setContentLength(gif.length);
            return new ResponseEntity<>(gif, headers, 200);
        } catch (Exception e) {
            log.error("Animation failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 生成进度条 GIF
     */
    @PostMapping(value = "/animation/progress", produces = "image/gif")
    public ResponseEntity<byte[]> generateProgressBar(@RequestBody Map<String, Object> body) {
        try {
            int frames = body.get("frames") != null ? ((Number) body.get("frames")).intValue() : 50;
            int width = body.get("width") != null ? ((Number) body.get("width")).intValue() : 600;
            int height = body.get("height") != null ? ((Number) body.get("height")).intValue() : 200;
            AnimationGenerator.AnimationConfig cfg = AnimationGenerator.AnimationConfig.builder()
                    .size(width, height).frames(frames).build();
            byte[] gif = animationGenerator.generateProgressBar(cfg);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("image/gif"));
            headers.setContentLength(gif.length);
            return new ResponseEntity<>(gif, headers, 200);
        } catch (Exception e) {
            log.error("Progress bar failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== 视频 (帧流 ZIP) ====================

    /**
     * 视频合成 (返回 ZIP 包含所有 PNG 帧)
     * POST /api/ai/video/compose
     */
    @PostMapping(value = "/video/compose")
    public ResponseEntity<byte[]> composeVideo(@RequestBody VideoComposer.VideoConfig config) {
        long start = System.currentTimeMillis();
        try {
            List<java.awt.image.BufferedImage> frames = videoComposer.renderAllFrames(config);
            // 打包成 ZIP
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                for (int i = 0; i < frames.size(); i++) {
                    String name = String.format("frame_%04d.png", i);
                    zos.putNextEntry(new ZipEntry(name));
                    javax.imageio.ImageIO.write(frames.get(i), "png", zos);
                    zos.closeEntry();
                }
            }
            byte[] zip = baos.toByteArray();
            auditLogger.log("AI_VIDEO", null, null, null, null, "ai", null, "POST",
                    "/api/ai/video/compose", config.title, 200, "SUCCESS", System.currentTimeMillis() - start);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/zip"));
            headers.setContentLength(zip.length);
            headers.set("Content-Disposition", "attachment; filename=\"video_frames.zip\"");
            return new ResponseEntity<>(zip, headers, 200);
        } catch (Exception e) {
            log.error("Video compose failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 视频合成 (从真实数据库)
     * POST /api/ai/video/from-data
     * Body: { dataSourceId, table, metricColumn, groupColumn, title }
     */
    @PostMapping(value = "/video/from-data")
    public ResponseEntity<byte[]> videoFromData(@RequestBody Map<String, Object> body) {
        try {
            Long dataSourceId = ((Number) body.get("dataSourceId")).longValue();
            String table = (String) body.get("table");
            String metric = (String) body.get("metricColumn");
            String group = (String) body.get("groupColumn");
            String title = (String) body.getOrDefault("title", "数据视频");
            VideoComposer.VideoConfig cfg = VideoComposer.VideoConfig.builder()
                    .size(1280, 720).fps(30).duration(5)
                    .title(title).dataSource(table).build();
            List<java.awt.image.BufferedImage> frames =
                    videoComposer.renderFromData(dataSourceId, table, metric, group, cfg);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                for (int i = 0; i < frames.size(); i++) {
                    zos.putNextEntry(new ZipEntry(String.format("frame_%04d.png", i)));
                    javax.imageio.ImageIO.write(frames.get(i), "png", zos);
                    zos.closeEntry();
                }
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/zip"));
            return new ResponseEntity<>(baos.toByteArray(), headers, 200);
        } catch (Exception e) {
            log.error("Video from data failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== 数据看板 (PNG) ====================

    /**
     * 渲染数据看板
     * POST /api/ai/dashboard/render
     */
    @PostMapping(value = "/dashboard/render", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> renderDashboard(@RequestBody DashboardBuilder.DashboardConfig config) {
        try {
            byte[] png = dashboardBuilder.render(config);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(png.length);
            return new ResponseEntity<>(png, headers, 200);
        } catch (Exception e) {
            log.error("Dashboard render failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 从真实数据生成看板
     * POST /api/ai/dashboard/from-data
     * Body: { dataSourceId, table, title }
     */
    @PostMapping(value = "/dashboard/from-data", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> dashboardFromData(@RequestBody Map<String, Object> body) {
        try {
            Long dataSourceId = ((Number) body.get("dataSourceId")).longValue();
            String table = (String) body.get("table");
            String title = (String) body.getOrDefault("title", table + " 看板");
            byte[] png = dashboardBuilder.renderFromData(dataSourceId, table, title);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(png.length);
            return new ResponseEntity<>(png, headers, 200);
        } catch (Exception e) {
            log.error("Dashboard from data failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== NL2Chart (自然语言 -> 图表) ====================

    /**
     * 自然语言生成图表
     * POST /api/ai/nl2chart
     * Body: { dataSourceId, question }
     * Response: image/png
     */
    @PostMapping(value = "/nl2chart", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> nl2chart(@RequestBody Map<String, Object> body) {
        try {
            Long dataSourceId = body.get("dataSourceId") != null ?
                    ((Number) body.get("dataSourceId")).longValue() : null;
            String question = (String) body.get("question");
            Nl2Chart.ChartResult result = nl2Chart.generateFromText(dataSourceId, question);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(result.pngBytes.length);
            // Header 中返回 SQL, 方便前端展示
            if (result.sql != null) {
                headers.set("X-Generated-SQL", result.sql);
            }
            headers.set("X-Chart-Type", String.valueOf(result.chartType));
            return new ResponseEntity<>(result.pngBytes, headers, 200);
        } catch (Exception e) {
            log.error("NL2Chart failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== 关键词路由 ====================

    /**
     * 关键词智能路由 (前端 AI 工具调用的入口)
     * POST /api/ai/route
     * Body: { text: "..." }
     * Response: { intent, params, handler }
     */
    @PostMapping("/route")
    public Result<KeywordEngine.RouteResult> routeByKeyword(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        KeywordEngine.RouteResult r = keywordEngine.route(text, null);
        return Result.ok(r);
    }

    /**
     * 识别意图 (纯识别, 不路由)
     */
    @PostMapping("/route/recognize")
    public Result<Map<String, Object>> recognize(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        KeywordEngine.Intent intent = keywordEngine.recognize(text);
        Map<String, String> params = keywordEngine.extractParams(text);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("intent", intent);
        result.put("params", params);
        result.put("text", text);
        return Result.ok(result);
    }

    /**
     * 智能分发 (V2.7 核心功能)
     * 接收用户提示词, 自动判断意图, 调用对应工具, 返回结果
     *
     * POST /api/ai/dispatch
     * Body: { text, userId, context }
     * Response: { intent, tool, result, fileUrl }
     */
    @PostMapping("/dispatch")
    public Result<Map<String, Object>> dispatch(@RequestBody Map<String, Object> body) {
        long start = System.currentTimeMillis();
        String text = (String) body.get("text");
        Long userId = body.get("userId") != null ? ((Number) body.get("userId")).longValue() : null;

        // 1. 识别意图
        KeywordEngine.Intent intent = keywordEngine.recognize(text);
        Map<String, String> params = keywordEngine.extractParams(text);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("intent", intent);
        result.put("params", params);
        result.put("text", text);

        // 2. 根据意图路由
        try {
            switch (intent) {
                case GENERATE_CHART: {
                    // 走 NL2Chart (无 dataSourceId 时返回 mock)
                    result.put("handler", "Nl2Chart");
                    result.put("action", "需要指定 dataSourceId, 请用 /api/ai/nl2chart 接口");
                    result.put("suggestedApi", "/api/ai/nl2chart");
                    break;
                }
                case GENERATE_MUSIC: {
                    MusicGenerator.MusicConfig cfg = parseMusicConfig(params);
                    byte[] midi = musicGenerator.generate(cfg);
                    result.put("handler", "MusicGenerator");
                    result.put("size", midi.length);
                    result.put("downloadUrl", "/api/ai/music/generate");
                    break;
                }
                case CHAT: {
                    // 走自研 AI 文本生成
                    result.put("handler", "TextGenerator");
                    result.put("suggestedApi", "/api/ai/generate");
                    break;
                }
                case TTS: {
                    result.put("handler", "AudioAnalyzer.synthesize");
                    result.put("suggestedApi", "/api/ai/multimodal/tts");
                    break;
                }
                case GENERATE_CODE: {
                    result.put("handler", "ProjectCodeGenerator");
                    result.put("suggestedApi", "/api/ai/admin/codegen");
                    break;
                }
                case QUERY_DATA: {
                    result.put("handler", "Nl2SqlTool");
                    result.put("suggestedApi", "/api/ai/admin/tools/sql.query/invoke");
                    break;
                }
                case ANALYZE_DATA: {
                    result.put("handler", "DataAnalyzer");
                    result.put("suggestedApi", "/api/ai/admin/tools/data.analyze.stats/invoke");
                    break;
                }
                case TRANSFER_HUMAN: {
                    result.put("handler", "TransferToHumanEvent");
                    result.put("action", "publish event");
                    break;
                }
                default: {
                    result.put("handler", "unknown");
                    result.put("action", "intent not recognized");
                }
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
            log.error("Dispatch failed", e);
        }

        // 3. 审计
        auditLogger.log("AI_DISPATCH", userId, null, null, null, "ai", null, "POST",
                "/api/ai/dispatch", dataMasker.mask(text), 200, "SUCCESS", System.currentTimeMillis() - start);

        // 4. 记录生成日志
        try {
            AiGenerationLog log = new AiGenerationLog();
            log.setGenerationId(UUID.randomUUID().toString());
            log.setUserId(userId);
            log.setModality(intent.name());
            log.setModelName("minimax-self-v1");
            log.setModelVersion("2.7");
            log.setPrompt(text);
            log.setStatus("SUCCESS");
            log.setDurationMs((int) (System.currentTimeMillis() - start));
            log.setWatermarked(1);
            log.setWatermarkText("本内容由 MiniMax AI 生成");
            generationLogMapper.insert(log);
        } catch (Exception ignore) {}

        return Result.ok(result);
    }

    /** 从 params 构建 MusicConfig */
    private MusicGenerator.MusicConfig parseMusicConfig(Map<String, String> params) {
        MusicGenerator.MusicConfig cfg = MusicGenerator.MusicConfig.builder().build();
        if (params.containsKey("key")) cfg.key = params.get("key").replaceAll("(大调|小调|major|minor)", "").trim();
        if (params.containsKey("scale")) cfg.scale = params.get("scale");
        if (params.containsKey("bpm")) cfg.bpm = Integer.parseInt(params.get("bpm"));
        if (params.containsKey("bars")) cfg.bars = Integer.parseInt(params.get("bars"));
        if (params.containsKey("style")) {
            try { cfg.style = MusicGenerator.Style.valueOf(params.get("style").toUpperCase()); } catch (Exception ignore) {}
        }
        return cfg;
    }

    // ==================== 工具调用 (通用) ====================

    /**
     * 列出所有可用工具
     */
    @GetMapping("/tools")
    public Result<List<AiTool>> listTools() {
        List<AiTool> tools = toolMapper.selectList(null);
        return Result.ok(tools);
    }

    /**
     * 调用工具 (按 code)
     */
    @PostMapping("/tools/{code}/invoke")
    public Result<Object> invokeTool(@PathVariable String code, @RequestBody Map<String, Object> input) {
        try {
            AiToolRegistry.ToolResult result = toolRegistry.invoke(code, input);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail("工具调用失败: " + e.getMessage());
        }
    }

    // ==================== 数据源管理 ====================

    @GetMapping("/datasources")
    public Result<List<DbDataSource>> listDataSources() {
        return Result.ok(dataSourceMapper.selectList(null));
    }

    @PostMapping("/datasources")
    public Result<DbDataSource> createDataSource(@RequestBody DbDataSource ds) {
        dataSourceMapper.insert(ds);
        return Result.ok(ds);
    }

    @PostMapping("/datasources/{id}/test")
    public Result<Map<String, Object>> testDataSource(@PathVariable Long id) {
        DbDataSource ds = dataSourceMapper.selectById(id);
        if (ds == null) return Result.fail("数据源不存在");
        try {
            Map<String, List<String>> schema = dynamicDataSource.loadSchema(id);
            return Result.ok(Map.of("success", true, "tables", schema.size(), "schema", schema));
        } catch (Exception e) {
            return Result.fail("连接失败: " + e.getMessage());
        }
    }

    @GetMapping("/datasources/{id}/schema")
    public Result<Map<String, List<String>>> getSchema(@PathVariable Long id) {
        return Result.ok(dynamicDataSource.loadSchema(id));
    }

    @PostMapping("/datasources/{id}/query")
    public Result<List<Map<String, Object>>> query(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String table = (String) body.get("table");
        String where = (String) body.get("where");
        Integer limit = body.get("limit") != null ? ((Number) body.get("limit")).intValue() : 1000;
        List<Map<String, Object>> rows = dynamicDataSource.query(id, table, limit, where);
        return Result.ok(rows);
    }

    // ==================== 文件管理 ====================

    @GetMapping("/files")
    public Result<List<MultimediaFile>> listFiles(@RequestParam(required = false) Long userId) {
        return Result.ok(fileMapper.selectList(null));
    }
}
