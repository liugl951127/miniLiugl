package com.minimax.ai.tool;

import com.minimax.ai.entity.AiTool;
import java.util.Map;

public interface AiToolExecutor {
    String getCode();
    Object execute(AiTool tool, Map<String, Object> input) throws Exception;
}
