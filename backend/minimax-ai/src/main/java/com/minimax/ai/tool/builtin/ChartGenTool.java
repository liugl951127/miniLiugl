package com.minimax.ai.tool.builtin;

import com.minimax.ai.generation.ChartGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 图表生成工具 (V2.8.3)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChartGenTool extends AbstractSimpleTool {

    private final ChartGenerator chartGenerator;

    @Override
    public String getCode() { return "chart.generate"; }

    @Override
    public String getName() { return "AI 图表"; }

    @Override
    public String getDescription() { return "7 种图表 (柱/折/饼/散/雷达/热力/桑基)"; }

    @Override
    public String getCategory() { return "chart"; }

    @Override
    protected Map<String, Object> doExecute(Map<String, Object> input) {
        String typeStr = (String) input.getOrDefault("type", "BAR");
        ChartGenerator.ChartType type;
        try { type = ChartGenerator.ChartType.valueOf(typeStr.toUpperCase()); }
        catch (Exception e) { throw new IllegalArgumentException("不支持图表类型: " + typeStr); }

        String title = (String) input.getOrDefault("title", "Chart");
        ChartGenerator.ChartData data = new ChartGenerator.ChartData();
        data.type = type;
        data.title = title;
        data.series = new ArrayList<>();
        data.points = new ArrayList<>();
        data.flows = new ArrayList<>();
        data.heatmapRowLabels = new String[0];
        data.heatmapColLabels = new String[0];

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> seriesList = (List<Map<String, Object>>) input.get("series");
        if (seriesList != null) {
            for (Map<String, Object> s : seriesList) {
                String name = (String) s.getOrDefault("name", "Series");
                @SuppressWarnings("unchecked")
                List<Number> values = (List<Number>) s.get("values");
                if (values != null) {
                    List<Double> vd = values.stream().map(Number::doubleValue).collect(Collectors.toList());
                    data.series.add(new ChartGenerator.Series(name, vd));
                }
            }
        }

        byte[] bytes = chartGenerator.render(data);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type.name());
        result.put("title", title);
        result.put("imageBase64", Base64.getEncoder().encodeToString(bytes));
        result.put("sizeBytes", bytes.length);
        result.put("mime", "image/png");
        return result;
    }
}
