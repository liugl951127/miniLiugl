package com.minimax.ai.generation;

import com.minimax.ai.tool.AiToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 关键词驱动引擎 (V2.7 自研)
 *
 * <p>本类是 MiniMax AI 的"大脑路由", 根据用户输入的提示词/关键词/表名,
 * 自动判断用户想做什么, 然后路由到对应的功能模块.</p>
 *
 * <h3>设计目标</h3>
 * 解决: "AI 怎么用" 问题. 用户不需要记 API, 只要说人话.
 *
 * <h3>支持意图 (持续扩展)</h3>
 * <table border="1">
 *   <tr><th>意图</th><th>关键词示例</th><th>触发动作</th></tr>
 *   <tr><td>生成图表</td><td>图表/统计/可视化/柱状图/折线图/饼图</td><td>调用 ChartGenerator</td></tr>
 *   <tr><td>生成音乐</td><td>音乐/旋律/MIDI/曲子</td><td>调用 MusicGenerator</td></tr>
 *   <tr><td>生成动画</td><td>动画/GIF/动图/进度条</td><td>调用 AnimationGenerator</td></tr>
 *   <tr><td>查询数据</td><td>查询/SQL/SELECT/数据</td><td>调用 NL2SQL</td></tr>
 *   <tr><td>数据统计</td><td>统计/平均/求和/最大值/分组</td><td>调用 DataAnalyzer</td></tr>
 *   <tr><td>生成代码</td><td>代码/spring boot/vue/python</td><td>调用 CodeGenerator</td></tr>
 *   <tr><td>聊天对话</td><td>聊天/问答/AI/你叫什么</td><td>调用 TextGenerator</td></tr>
 *   <tr><td>语音合成</td><td>语音/TTS/说话/读</td><td>调用 TTS</td></tr>
 *   <tr><td>转人工</td><td>人工/真人/转接</td><td>触发事件</td></tr>
 * </table>
 *
 * <h3>匹配算法</h3>
 * <ol>
 *   <li>关键词权重匹配 (TF-IDF 简化版)</li>
 *   <li>正则表达式匹配 (高级模式)</li>
 *   <li>表名识别 (从 schema 自动发现)</li>
 *   <li>意图优先级: 正则 > 关键词 > 默认</li>
 * </ol>
 *
 * @author MiniMax Team
 * @since V2.7
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeywordEngine {

    /** 意图定义 */
    public enum Intent {
        GENERATE_CHART,        // 生成图表
        GENERATE_MUSIC,        // 生成音乐
        GENERATE_ANIMATION,    // 生成动画
        QUERY_DATA,            // 查询数据 (NL2SQL)
        ANALYZE_DATA,          // 数据分析
        GENERATE_CODE,         // 生成代码
        CHAT,                  // 普通聊天
        TTS,                   // 语音合成
        STT,                   // 语音识别
        TRANSFER_HUMAN,        // 转人工
        IMAGE_ANALYZE,         // 图片分析
        VIDEO_ANALYZE,         // 视频分析
        AUDIO_ANALYZE,         // 音频分析
        UNKNOWN                // 未知
    }

    /**
     * 关键词到意图的映射 (带权重)
     * 权重越高, 匹配越优先
     */
    private static final Map<Intent, List<String>> KEYWORDS = new LinkedHashMap<>();
    static {
        KEYWORDS.put(Intent.GENERATE_CHART, List.of(
                "图表", "统计图", "可视化", "柱状图", "折线图", "饼图", "雷达图", "热力图", "散点图", "桑基图",
                "chart", "graph", "plot", "bar", "line", "pie", "radar", "heatmap", "scatter"));
        KEYWORDS.put(Intent.GENERATE_MUSIC, List.of(
                "音乐", "旋律", "曲子", "MIDI", "作曲", "和弦", "节拍",
                "music", "melody", "song", "compose", "tune"));
        KEYWORDS.put(Intent.GENERATE_ANIMATION, List.of(
                "动画", "GIF", "动图", "进度条动画", "过渡动画",
                "animation", "animate", "loop"));
        KEYWORDS.put(Intent.QUERY_DATA, List.of(
                "查询", "SELECT", "FROM", "SQL", "数据", "记录", "表",
                "query", "select", "from", "where", "fetch"));
        KEYWORDS.put(Intent.ANALYZE_DATA, List.of(
                "统计", "平均", "求和", "最大值", "最小值", "分组", "聚合", "趋势", "异常",
                "analyze", "statistics", "avg", "sum", "max", "min", "group by"));
        KEYWORDS.put(Intent.GENERATE_CODE, List.of(
                "代码", "Spring Boot", "Vue", "React", "Python", "Flask", "Node", "Express",
                "code", "generate project", "scaffold", "template"));

        // V2.8.3 新增
        KEYWORDS.put(Intent.CHAT, List.of(
                "你好", "你是谁", "叫什么", "介绍", "你好啊", "能做什么", "幫我",
                "hello", "hi", "what", "who", "help", "introduce"));
        KEYWORDS.put(Intent.IMAGE_ANALYZE, List.of(
                "分析图片", "图片识别", "看图", "这张图", "图片里", "图像",
                "analyze image", "image analysis", "what is in"));
        KEYWORDS.put(Intent.AUDIO_ANALYZE, List.of(
                "分析音频", "音频识别", "语音", "听", "声音", "这段音",
                "analyze audio", "speech", "voice"));
        KEYWORDS.put(Intent.VIDEO_ANALYZE, List.of(
                "分析视频", "视频识别", "这个视频", "视频里",
                "analyze video", "video analysis"));
        KEYWORDS.put(Intent.TTS, List.of(
                "语音合成", "TTS", "读出来", "朗读", "声音说", "说话",
                "text to speech", "tts", "speak"));
        KEYWORDS.put(Intent.STT, List.of(
                "语音识别", "STT", "转文字", "语音转文字", "录音转文字",
                "speech to text", "stt", "transcribe"));
        KEYWORDS.put(Intent.TRANSFER_HUMAN, List.of(
                "转人工", "真人", "人工", "客服", "人工服务", "坐席",
                "transfer", "human", "agent", "operator"));
        KEYWORDS.put(Intent.CHAT, List.of(
                "你好", "请问", "什么是", "怎么", "如何", "为什么", "介绍", "讲讲",
                "hello", "hi", "what", "how", "why", "tell me", "explain"));
        KEYWORDS.put(Intent.TTS, List.of(
                "朗读", "语音播报", "TTS", "读出来", "发声", "合成语音", "text to speech"));
        KEYWORDS.put(Intent.STT, List.of(
                "听写", "语音识别", "STT", "speech to text", "transcribe"));
        KEYWORDS.put(Intent.TRANSFER_HUMAN, List.of(
                "转人工", "真人", "人工客服", "转接", "human", "agent"));
        KEYWORDS.put(Intent.IMAGE_ANALYZE, List.of(
                "分析图片", "看图", "识别图片", "analyze image", "image description"));
        KEYWORDS.put(Intent.VIDEO_ANALYZE, List.of(
                "分析视频", "看视频", "识别视频", "analyze video", "video description"));
        KEYWORDS.put(Intent.AUDIO_ANALYZE, List.of(
                "分析音频", "分析声音", "听声音", "analyze audio"));
    }

    /** 高级正则模式 (优先级最高) */
    private static final Map<Intent, List<Pattern>> REGEX_PATTERNS = new LinkedHashMap<>();
    static {
        REGEX_PATTERNS.put(Intent.GENERATE_CHART, List.of(
                Pattern.compile("生成.*?(柱状图|折线图|饼图|雷达图|热力图|散点图|桑基图)"),
                Pattern.compile("(统计|分析|展示).*?(数据|数值)"),
                Pattern.compile("(show|generate|plot).*?(chart|graph|bar|line|pie)")));
        REGEX_PATTERNS.put(Intent.GENERATE_MUSIC, List.of(
                Pattern.compile("(生成|创作|作).*?(音乐|旋律|曲子|歌)"),
                Pattern.compile("(compose|generate).*?(music|melody|song)")));
        REGEX_PATTERNS.put(Intent.GENERATE_CODE, List.of(
                Pattern.compile("(生成|创建).*?(Spring Boot|Vue|React|Flask|Express).*?项目"),
                Pattern.compile("(create|generate).*?(project|app)")));
        REGEX_PATTERNS.put(Intent.QUERY_DATA, List.of(
                Pattern.compile("(查询|统计|列出|显示).*?(前\\s*\\d+|最近|所有).*?(条|个|记录)"),
                Pattern.compile("(select|show|list).*?(from|in)\\s+\\w+")));
        REGEX_PATTERNS.put(Intent.TRANSFER_HUMAN, List.of(
                Pattern.compile("(转|找|叫|来)\\s*(人工|真人|客服)")));
    }

    private final AiToolRegistry toolRegistry;

    /**
     * 识别用户意图
     *
     * @param text 用户输入 (中文 / 英文)
     * @return Intent 枚举
     */
    public Intent recognize(String text) {
        if (text == null || text.trim().isEmpty()) return Intent.UNKNOWN;
        String lower = text.toLowerCase();

        // 1. 正则匹配 (高精度)
        for (Map.Entry<Intent, List<Pattern>> entry : REGEX_PATTERNS.entrySet()) {
            for (Pattern p : entry.getValue()) {
                if (p.matcher(text).find()) {
                    log.debug("Intent matched by regex: {} -> {}", p.pattern(), entry.getKey());
                    return entry.getKey();
                }
            }
        }

        // 2. 关键词匹配 (TF 简化: 计每个意图命中数)
        Map<Intent, Integer> scores = new HashMap<>();
        for (Map.Entry<Intent, List<String>> entry : KEYWORDS.entrySet()) {
            int count = 0;
            for (String kw : entry.getValue()) {
                if (lower.contains(kw.toLowerCase())) count++;
            }
            if (count > 0) scores.put(entry.getKey(), count);
        }

        // 取最高分
        if (!scores.isEmpty()) {
            Intent best = scores.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .get().getKey();
            log.debug("Intent matched by keyword: {} (score {})", best, scores.get(best));
            return best;
        }

        return Intent.CHAT;  // 默认聊天
    }

    /**
     * 提取参数 (从文本中提取配置参数)
     */
    public Map<String, String> extractParams(String text) {
        Map<String, String> params = new HashMap<>();
        if (text == null) return params;

        // 提取调式 (C/D/E/F/G/A/B + major/minor)
        Matcher m = Pattern.compile("[CcDdEeFfGgAaBb][#b]?\\s*(大调|小调|major|minor)?").matcher(text);
        if (m.find()) params.put("key", m.group().trim());

        // 提取 BPM
        m = Pattern.compile("(\\d{2,3})\\s*bpm", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) params.put("bpm", m.group(1));

        // 提取小节数
        m = Pattern.compile("(\\d+)\\s*(小节|bar|小节数)").matcher(text);
        if (m.find()) params.put("bars", m.group(1));

        // 提取风格
        for (String style : new String[]{"pop", "classical", "rock", "electronic", "folk", "jazz", "流行", "古典", "摇滚", "电子", "民谣", "爵士"}) {
            if (text.toLowerCase().contains(style)) {
                params.put("style", style);
                break;
            }
        }

        // 提取图表类型
        for (String type : new String[]{"柱状图", "折线图", "饼图", "雷达图", "热力图", "散点图", "桑基图"}) {
            if (text.contains(type)) {
                params.put("chartType", type);
                break;
            }
        }

        // 提取表名 (FROM table / 查询 user 表)
        m = Pattern.compile("(?:FROM|from|查询|统计)\\s+([a-zA-Z_][a-zA-Z0-9_]*)").matcher(text);
        if (m.find()) params.put("table", m.group(1));

        return params;
    }

    /**
     * 路由到处理函数
     */
    public RouteResult route(String text, Map<String, Object> context) {
        Intent intent = recognize(text);
        Map<String, String> params = extractParams(text);

        RouteResult result = new RouteResult();
        result.intent = intent;
        result.params = params;
        result.originalText = text;

        // 推荐处理函数
        result.handler = suggestHandler(intent, params, context);

        return result;
    }

    private String suggestHandler(Intent intent, Map<String, String> params, Map<String, Object> context) {
        switch (intent) {
            case GENERATE_CHART: return "ChartGenerator.render";
            case GENERATE_MUSIC: return "MusicGenerator.generate";
            case GENERATE_ANIMATION: return "AnimationGenerator.generateTextFadeIn";
            case QUERY_DATA: return "Nl2SqlTool.execute";
            case ANALYZE_DATA: return "StatsTool.execute";
            case GENERATE_CODE: return "ProjectCodeGenerator.generate";
            case CHAT: return "TextGenerator.generate";
            case TTS: return "AudioAnalyzer.synthesize";
            case TRANSFER_HUMAN: return "TransferToHumanEvent.publish";
            case IMAGE_ANALYZE: return "ImageAnalyzer.analyze";
            default: return "TextGenerator.generate";
        }
    }

    /**
     * 路由结果
     */
    public static class RouteResult {
        public Intent intent;
        public Map<String, String> params;
        public String originalText;
        public String handler;
        public double confidence = 0.0;

        @Override
        public String toString() {
            return "RouteResult{intent=" + intent + ", params=" + params + ", handler='" + handler + "'}";
        }
    }
}
