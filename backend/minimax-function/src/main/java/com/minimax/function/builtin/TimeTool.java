package com.minimax.function.builtin;

import com.minimax.function.executor.ToolFunction;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class TimeTool implements ToolFunction {
    @Override public String name() { return "get_current_time"; }

    @Override
    public String execute(Map<String, Object> args) {
        String tz = args == null ? "Asia/Shanghai" : (String) args.getOrDefault("timezone", "Asia/Shanghai");
        try {
            ZoneId zone = ZoneId.of(tz);
            LocalDateTime now = LocalDateTime.now(zone);
            return String.format("{\"timezone\":\"%s\",\"datetime\":\"%s\",\"timestamp\":%d}",
                    tz,
                    now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    now.atZone(zone).toInstant().toEpochMilli());
        } catch (Exception e) {
            return "{\"error\":\"invalid timezone: " + tz + "\"}";
        }
    }
}
