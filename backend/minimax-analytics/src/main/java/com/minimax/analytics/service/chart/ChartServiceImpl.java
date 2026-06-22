package com.minimax.analytics.service.chart;

import com.minimax.analytics.vo.QueryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 图表服务实现 (V5.31) - ECharts option JSON
 */
@Service
@RequiredArgsConstructor
public class ChartServiceImpl implements ChartService {

    private final ChartTypeDecider decider;

    @Override
    public Map<String, Object> autoChart(QueryResult result) {
        ChartTypeDecider.Decision d = decider.decide(result);
        switch (d.type) {
            case BAR: return buildBar(result, d);
            case LINE: return buildLine(result, d);
            case PIE: return buildPie(result, d);
            default: return buildTable(result);
        }
    }

    @Override
    public List<Map<String, Object>> autoCharts(List<QueryResult> results) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (QueryResult r : results) out.add(autoChart(r));
        return out;
    }

    private Map<String, Object> buildBar(QueryResult result, ChartTypeDecider.Decision d) {
        List<String> xData = new ArrayList<>();
        List<Object> yData = new ArrayList<>();
        for (Map<String, Object> row : result.getRows()) {
            xData.add(String.valueOf(row.get(d.xField)));
            yData.add(row.get(d.yField));
        }
        Map<String, Object> option = new HashMap<>();
        option.put("title", Map.of("text", d.title != null ? d.title : d.yField));
        option.put("tooltip", Map.of("trigger", "axis"));
        option.put("xAxis", Map.of("type", "category", "data", xData));
        option.put("yAxis", Map.of("type", "value"));
        option.put("series", List.of(Map.of(
                "name", d.yField, "type", "bar", "data", yData
        )));
        return Map.of("type", "bar", "option", option);
    }

    private Map<String, Object> buildLine(QueryResult result, ChartTypeDecider.Decision d) {
        List<String> xData = new ArrayList<>();
        for (Map<String, Object> row : result.getRows()) xData.add(String.valueOf(row.get(d.xField)));

        List<Map<String, Object>> series = new ArrayList<>();
        List<String> yFields = d.yFields != null ? d.yFields : List.of(d.yField);
        for (String y : yFields) {
            List<Object> yData = new ArrayList<>();
            for (Map<String, Object> row : result.getRows()) yData.add(row.get(y));
            series.add(Map.of("name", y, "type", "line", "data", yData));
        }
        Map<String, Object> option = new HashMap<>();
        option.put("title", Map.of("text", d.title != null ? d.title : String.join(",", yFields)));
        option.put("tooltip", Map.of("trigger", "axis"));
        option.put("legend", Map.of("data", yFields));
        option.put("xAxis", Map.of("type", "category", "data", xData));
        option.put("yAxis", Map.of("type", "value"));
        option.put("series", series);
        return Map.of("type", "line", "option", option);
    }

    private Map<String, Object> buildPie(QueryResult result, ChartTypeDecider.Decision d) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (Map<String, Object> row : result.getRows()) {
            data.add(Map.of("name", String.valueOf(row.get(d.xField)), "value", row.get(d.yField)));
        }
        Map<String, Object> option = new HashMap<>();
        option.put("title", Map.of("text", d.title != null ? d.title : d.yField));
        option.put("series", List.of(Map.of("type", "pie", "radius", "50%", "data", data)));
        return Map.of("type", "pie", "option", option);
    }

    private Map<String, Object> buildTable(QueryResult result) {
        return Map.of("type", "table", "columns", result.getColumns(), "rows", result.getRows());
    }
}
