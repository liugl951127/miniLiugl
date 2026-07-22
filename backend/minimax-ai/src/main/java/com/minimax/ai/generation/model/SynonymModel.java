package com.minimax.ai.generation.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 同义词扩展模型 (V3.5.15+)
 *
 * <h3>原理</h3>
 * query "搞个统计图" 扩展为 ["搞个", "做个", "生成", "创建", "画一个"] × ["统计图", "图表", "可视化"]
 * 然后用扩展后的 query 跑 TF 匹配
 *
 * <h3>2 种词典</h3>
 * <ul>
 *   <li><b>动词同义</b>: 生成 = 搞个 = 做个 = 画一个 = 创建 = 弄个 = 来一个</li>
 *   <li><b>名词同义</b>: 图表 = 统计图 = 可视化 = visualization = chart = plot</li>
 * </ul>
 *
 * <h3>性能</h3>
 * <ul>
 *   <li>扩展: O(query tokens × synonyms) ≈ O(20 × 5) = 100 次</li>
 *   <li>TF 匹配: O(扩展 query length × keywords)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V3.5.15
 */
@Slf4j
@Component
public class SynonymModel {

    /**
     * 同义词词典
     * <p>结构: 词 → 它的同义词集合 (含自身)
     */
    private final Map<String, Set<String>> synonymDict = new ConcurrentHashMap<>();

    /**
     * 初始化默认同义词 (200+ 条, 中英双语)
     */
    public SynonymModel() {
        initDefault();
    }

    private void initDefault() {
        // ── 动作同义 (生成类) ──
        addSynonyms("生成", "搞个", "做个", "弄个", "来一个", "要一个", "整一个", "画一个", "创建", "produce", "generate", "create", "make", "build");
        addSynonyms("分析", "看看", "瞅瞅", "评估", "研究", "analyze", "examine", "review");
        addSynonyms("查询", "查一下", "查下", "搜一下", "搜下", "查", "找", "query", "fetch", "search", "find");
        addSynonyms("统计", "算", "汇总", "聚合", "count", "sum", "aggregate");
        addSynonyms("朗读", "读出来", "读一下", "读", "speak", "read", "say");
        addSynonyms("识别", "看看", "认", "detect", "recognize", "identify");

        // ── 名词同义 (图表类) ──
        addSynonyms("图表", "统计图", "可视化", "图", "chart", "graph", "plot", "visualization");
        addSynonyms("柱状图", "柱形图", "条形图", "bar", "bar chart", "column chart");
        addSynonyms("折线图", "线图", "趋势图", "line", "line chart", "trend");
        addSynonyms("饼图", "圆饼图", "饼状图", "pie", "pie chart");
        addSynonyms("雷达图", "蜘蛛图", "radar", "spider", "radar chart");
        addSynonyms("热力图", "热图", "heatmap", "heat map");
        addSynonyms("散点图", "散布图", "scatter", "scatter plot");

        // ── 名词同义 (音乐/动画) ──
        addSynonyms("音乐", "曲子", "旋律", "歌", "BGM", "music", "melody", "song", "tune");
        addSynonyms("动画", "动图", "GIF", "animation", "animate", "gif");
        addSynonyms("代码", "程序", "脚本", "code", "script", "program");
        addSynonyms("项目", "工程", "project", "app", "application");

        // ── 名词同义 (多媒体) ──
        addSynonyms("图片", "图像", "照片", "图", "image", "picture", "photo");
        addSynonyms("视频", "录像", "短片", "video", "movie", "clip");
        addSynonyms("音频", "录音", "声音", "语音", "audio", "sound", "voice", "recording");

        // ── 行为同义 (人工服务) ──
        addSynonyms("人工", "真人", "客服", "坐席", "human", "agent", "operator", "support");
        addSynonyms("转人工", "转接", "转客服", "找人工", "transfer", "connect agent");

        log.info("[synonym] initialized: {} synonym groups, {} total entries",
                synonymDict.size(),
                synonymDict.values().stream().mapToInt(Set::size).sum());
    }

    /**
     * 互为同义: 把 group 里所有词绑成同义 (传递闭包)
     */
    private void addSynonyms(String... group) {
        if (group == null || group.length < 2) return;
        // 每个词 → 全部同义 (含自身)
        Set<String> all = new HashSet<>(Arrays.asList(group));
        for (String word : group) {
            synonymDict.compute(word.toLowerCase(), (k, existing) -> {
                if (existing == null) {
                    return new HashSet<>(all);
                }
                existing.addAll(all);
                return existing;
            });
        }
    }

    /**
     * 扩展 query: 返回所有可能的同义变体 (含原 query)
     *
     * <p>例: "搞个统计图" → ["搞个统计图", "做个统计图", "画一个统计图", "搞个图表", "做个图表", ...]
     */
    public Set<String> expand(String query) {
        Set<String> result = new HashSet<>();
        if (query == null || query.trim().isEmpty()) return result;
        result.add(query);

        // 找 query 里所有同义词的扩展
        // 简单做法: 按空格 / 标点 split, 逐个 token 找同义
        // 复杂: 用最大匹配 (贪心)
        String lower = query.toLowerCase();
        for (Map.Entry<String, Set<String>> entry : synonymDict.entrySet()) {
            String word = entry.getKey();
            if (lower.contains(word)) {
                // 替换 word 为每个同义, 生成新 query
                for (String synonym : entry.getValue()) {
                    if (synonym.equalsIgnoreCase(word)) continue;  // 跳过自己
                    result.add(lower.replace(word, synonym));
                }
            }
        }
        return result;
    }

    /**
     * 单词级扩展 (用于 token 后的同义替换)
     */
    public Set<String> getSynonyms(String word) {
        if (word == null) return Collections.emptySet();
        Set<String> syns = synonymDict.get(word.toLowerCase());
        return syns != null ? new HashSet<>(syns) : Collections.singleton(word);
    }
}
