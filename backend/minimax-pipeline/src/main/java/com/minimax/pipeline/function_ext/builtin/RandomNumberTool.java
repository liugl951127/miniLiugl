package com.minimax.pipeline.function_ext.builtin;

import com.minimax.pipeline.function_ext.executor.ToolFunction;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class RandomNumberTool implements ToolFunction {
    @Override public String name() { return "random_number"; }

    @Override
    public String execute(Map<String, Object> args) {
        int min = args == null ? 1 : ((Number) args.getOrDefault("min", 1)).intValue();
        int max = args == null ? 100 : ((Number) args.getOrDefault("max", 100)).intValue();
        if (min > max) { int t = min; min = max; max = t; }
        int n = ThreadLocalRandom.current().nextInt(min, max + 1);
        return "{\"min\":" + min + ",\"max\":" + max + ",\"result\":" + n + "}";
    }
}
