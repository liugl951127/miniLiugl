package com.minimax.ai.framework.group;

import lombok.AllArgsConstructor;

/**
 * 简单回声 Agent (V3.0.3 自研, 无外部依赖)
 *
 * <p>用于测试和演示, 不调任何 LLM, 直接返回格式化结果
 * <p>策略:
 *   - 含 "PIPELINE" 标记: 输出 "[A] 输入长度=X"
 *   - 含 "DEBATE" 标记: 输出 "[A] 提议版本 N"
 *   - 含 "VOTE" 标记: 输出 "[A] 选项 A"
 *   - 默认: 输出 "[name] 处理了: ..."
 *
 * <p>支持能力标签 (capability) 自定义输出
 */
@AllArgsConstructor
public class SimpleEchoAgent implements AgentExecutor {

    /** Agent 名 */
    private final String name;
    /** 能力描述 */
    private final String capability;

    @Override
    public String name() {
        return name;
    }

    @Override
    public String capability() {
        return capability;
    }

    @Override
    public String execute(GroupTask.SubTask task, GroupSharedMemory memory) {
        // 1. 取输入
        String input = task.getInstruction() == null ? "" : task.getInstruction();
        // 2. 截断 (避免过长)
        String preview = input.length() > 80 ? input.substring(0, 80) + "..." : input;
        // 3. 根据 capability 生成不同输出
        String tag = capability == null ? "generic" : capability;
        // 4. 模拟"思考过程"
        return "[" + name + "/" + tag + "] 处理 (输入 " + input.length() + " 字符): " + preview;
    }
}
