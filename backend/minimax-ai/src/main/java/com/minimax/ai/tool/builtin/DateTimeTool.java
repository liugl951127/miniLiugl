package com.minimax.ai.tool.builtin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 时间工具 (V2.8.3) - 格式转换/计算/时区
 */
@Slf4j
@Component
public class DateTimeTool extends AbstractSimpleTool {

    private static final List<String> COMMON_FORMATS = List.of(
            "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd",
            "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd",
            "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy年MM月dd日", "HH:mm:ss", "HH:mm"
    );

    @Override
    public String getCode() { return "time.convert"; }

    @Override
    public String getName() { return "时间工具"; }

    @Override
    public String getDescription() { return "时间格式转换/计算/时区"; }

    @Override
    public String getCategory() { return "time"; }

    @Override
    protected Map<String, Object> doExecute(Map<String, Object> input) {
        String op = (String) input.getOrDefault("op", "now");
        String tz = (String) input.getOrDefault("timezone", "Asia/Shanghai");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("op", op);
        result.put("timezone", tz);
        ZoneId zone = ZoneId.of(tz);
        switch (op) {
            case "now" -> {
                Instant now = Instant.now();
                result.put("instant", now.toString());
                result.put("epochMillis", now.toEpochMilli());
                result.put("local", now.atZone(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                result.put("formatted", now.atZone(zone).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            case "parse" -> {
                String text = (String) input.get("text");
                if (text == null) throw new IllegalArgumentException("需要 text");
                LocalDateTime ldt = parse(text);
                Instant instant = ldt.atZone(zone).toInstant();
                result.put("text", text);
                result.put("instant", instant.toString());
                result.put("epochMillis", instant.toEpochMilli());
                result.put("local", ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            case "format" -> {
                long ts = ((Number) input.get("epochMillis")).longValue();
                String pattern = (String) input.getOrDefault("pattern", "yyyy-MM-dd HH:mm:ss");
                String text = Instant.ofEpochMilli(ts).atZone(zone).format(DateTimeFormatter.ofPattern(pattern));
                result.put("epochMillis", ts);
                result.put("formatted", text);
            }
            case "add" -> {
                long ts = ((Number) input.get("epochMillis")).longValue();
                int days = ((Number) input.getOrDefault("days", 0)).intValue();
                int hours = ((Number) input.getOrDefault("hours", 0)).intValue();
                int minutes = ((Number) input.getOrDefault("minutes", 0)).intValue();
                Instant r = Instant.ofEpochMilli(ts).plus(Duration.ofDays(days).plusHours(hours).plusMinutes(minutes));
                result.put("epochMillis", ts);
                result.put("added", days + "d " + hours + "h " + minutes + "m");
                result.put("resultEpochMillis", r.toEpochMilli());
                result.put("resultLocal", r.atZone(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }
            case "diff" -> {
                long a = ((Number) input.get("epochMillisA")).longValue();
                long b = ((Number) input.get("epochMillisB")).longValue();
                Duration d = Duration.ofMillis(Math.abs(b - a));
                result.put("epochMillisA", a); result.put("epochMillisB", b);
                result.put("diffMillis", d.toMillis());
                result.put("diffSeconds", d.getSeconds());
                result.put("diffHuman", humanize(d));
            }
            case "convert" -> {
                String from = (String) input.get("from");
                String to = (String) input.get("to");
                long ts = ((Number) input.get("epochMillis")).longValue();
                Instant instant = Instant.ofEpochMilli(ts);
                result.put("from", instant.atZone(ZoneId.of(from)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                result.put("to", instant.atZone(ZoneId.of(to)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }
            case "formats" -> result.put("formats", COMMON_FORMATS);
            case "zones" -> {
                @SuppressWarnings("unchecked")
                List<String> z = (List<String>) input.get("filter");
                List<String> all = new ArrayList<>(ZoneId.getAvailableZoneIds());
                if (z != null && !z.isEmpty()) all.retainAll(z);
                else all = all.subList(0, Math.min(30, all.size()));
                Collections.sort(all);
                result.put("zones", all);
            }
            default -> throw new IllegalArgumentException("不支持的 op: " + op);
        }
        return result;
    }

    private LocalDateTime parse(String text) {
        for (String fmt : COMMON_FORMATS) {
            try { return LocalDateTime.parse(text, DateTimeFormatter.ofPattern(fmt)); } catch (Exception ignored) {}
        }
        try { return LocalDateTime.parse(text); } catch (Exception ignored) {}
        try { return LocalDate.parse(text).atStartOfDay(); } catch (Exception ignored) {}
        throw new IllegalArgumentException("无法解析时间: " + text);
    }

    private String humanize(Duration d) {
        long s = d.getSeconds();
        if (s < 60) return s + " 秒";
        if (s < 3600) return (s / 60) + " 分 " + (s % 60) + " 秒";
        if (s < 86400) return (s / 3600) + " 时 " + ((s % 3600) / 60) + " 分";
        return (s / 86400) + " 天 " + ((s % 86400) / 3600) + " 时";
    }
}
