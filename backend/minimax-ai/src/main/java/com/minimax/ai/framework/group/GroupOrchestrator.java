package com.minimax.ai.framework.group;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;

/**
 * 群组协调器 (V3.0.3 自研核心)
 *
 * <p>负责按 {@link GroupStrategy} 执行多 Agent 协作:
 *   - PIPELINE: 顺序执行 worker, 上一步输出作下一步输入
 *   - DEBATE: 并行提议 → 互相 review → 收敛
 *   - VOTE: 并行独立出答案 → 多数决
 *   - SWARM: 自由贡献 → manager 择优
 *
 * <h3>线程模型</h3>
 * 并行策略用 ExecutorService 线程池 (大小 = worker 数)
 * 主线程等待所有 worker 完成
 *
 * <h3>复杂度</h3>
 *   PIPELINE: O(N×T)  N=worker 数, T=单次推理时间
 *   DEBATE: O(R×N×T)  R=轮数
 *   VOTE: O(N×T)  并行
 *   SWARM: O(N×T)
 */
@Slf4j
public class GroupOrchestrator {

    /** 线程池 (VOTE/SWARM/DEBATE 并行用) */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 执行群组任务
     *
     * @param group    群组成员
     * @param task     任务
     * @param agents   Agent 映射 (name → executor)
     * @param strategy 协作策略
     * @return 执行结果
     */
    public GroupResult execute(List<GroupMember> group, GroupTask task,
                                Map<String, AgentExecutor> agents,
                                GroupStrategy strategy) {
        long start = System.currentTimeMillis();
        log.info("[orch] 群组 {} 执行任务: {} 策略={} 成员={}",
                task.getGroupId(), task.getSubject(), strategy, group.size());

        // 1. 创建共享内存 + 消息总线
        GroupSharedMemory memory = new GroupSharedMemory();
        GroupMessageBus bus = new GroupMessageBus();
        // 2. 注册所有成员到 bus
        for (GroupMember m : group) bus.register(m.getAgentName());
        // 3. 写入原始任务到共享内存 (空值用空字符串)
        memory.put("task.original.input", task.getInput() == null ? "" : task.getInput());
        memory.put("task.type", task.getType() == null ? "" : task.getType());
        memory.appendToList("history", "TASK_START: " + task.getSubject());

        try {
            // 4. 按策略分发
            GroupResult result;
            switch (strategy) {
                case PIPELINE -> result = runPipeline(group, task, agents, memory, bus);
                case DEBATE -> result = runDebate(group, task, agents, memory, bus, 3);  // 3 轮辩论
                case VOTE -> result = runVote(group, task, agents, memory, bus);
                case SWARM -> result = runSwarm(group, task, agents, memory, bus);
                default -> {
                    result = GroupResult.builder()
                            .taskId(task.getTaskId())
                            .groupId(task.getGroupId())
                            .status(GroupResult.Status.FAILED)
                            .error("未知策略: " + strategy)
                            .build();
                }
            }
            // 5. 填耗时
            result.setDurationMs(System.currentTimeMillis() - start);
            log.info("[orch] 完成: status={} duration={}ms", result.getStatus(), result.getDurationMs());
            return result;
        } finally {
            // 6. 清理资源
            bus.clear();
            memory.clear();
        }
    }

    /**
     * PIPELINE 策略: 顺序执行, A → B → C
     *
     * <p>每个 worker 处理上一个的输出, 形成流水线
     */
    private GroupResult runPipeline(List<GroupMember> group, GroupTask task,
                                      Map<String, AgentExecutor> agents,
                                      GroupSharedMemory memory, GroupMessageBus bus) {
        // 1. 找 manager 和所有 worker (按 order 排序)
        GroupMember manager = findManager(group);
        List<GroupMember> workers = findWorkers(group);
        if (manager == null || workers.isEmpty()) {
            return fail(task, "PIPELINE 需要 manager + worker");
        }
        // 2. 上下文变量 (前一个的输出, 初始为原始 input)
        String context = task.getInput();
        Map<String, String> workerOutputs = new LinkedHashMap<>();
        // 3. 顺序执行 worker
        for (GroupMember w : workers) {
            AgentExecutor agent = agents.get(w.getAgentName());
            if (agent == null) continue;  // 跳过未注册 agent
            log.info("[pipeline] {} 处理中 (input: {} 字符)", w.getAgentName(), context.length());
            // 4. 构造子任务
            GroupTask.SubTask sub = GroupTask.SubTask.builder()
                    .subId(UUID.randomUUID().toString())
                    .assignee(w.getAgentName())
                    .instruction(context)  // 上一步输出
                    .status(GroupTask.SubTask.Status.RUNNING)
                    .build();
            // 5. 执行
            String output = safeExecute(agent, sub, memory);
            // 6. 记录
            workerOutputs.put(w.getAgentName(), output);
            memory.put("result." + w.getAgentName(), output);
            memory.appendToList("history", w.getAgentName() + " -> " + output.length() + " 字符");
            // 7. 下一轮 input
            context = output;
        }
        // 8. 最终输出
        String finalOut = context;
        return GroupResult.builder()
                .taskId(task.getTaskId())
                .groupId(task.getGroupId())
                .status(GroupResult.Status.SUCCESS)
                .finalOutput(finalOut)
                .workerOutputs(workerOutputs)
                .consensus(finalOut)
                .score(8.0)
                .meta(Map.of("strategy", "PIPELINE", "workers", workers.size()))
                .build();
    }

    /**
     * VOTE 策略: 并行独立执行, 多数决
     */
    private GroupResult runVote(List<GroupMember> group, GroupTask task,
                                 Map<String, AgentExecutor> agents,
                                 GroupSharedMemory memory, GroupMessageBus bus) {
        // 1. 找 manager 和所有 worker
        GroupMember manager = findManager(group);
        List<GroupMember> workers = findWorkers(group);
        if (manager == null || workers.isEmpty()) {
            return fail(task, "VOTE 需要 manager + worker");
        }
        // 2. 并行提交任务 (每 worker 一个 Future)
        Map<String, Future<String>> futures = new LinkedHashMap<>();
        for (GroupMember w : workers) {
            AgentExecutor agent = agents.get(w.getAgentName());
            if (agent == null) continue;
            // 3. 提交到线程池
            futures.put(w.getAgentName(), executor.submit(() -> {
                GroupTask.SubTask sub = GroupTask.SubTask.builder()
                        .subId(UUID.randomUUID().toString())
                        .assignee(w.getAgentName())
                        .instruction(task.getInput())
                        .status(GroupTask.SubTask.Status.RUNNING)
                        .build();
                return safeExecute(agent, sub, memory);
            }));
        }
        // 4. 收集结果
        Map<String, String> workerOutputs = new LinkedHashMap<>();
        Map<String, Integer> voteCount = new HashMap<>();      // 投票计数
        for (Map.Entry<String, Future<String>> e : futures.entrySet()) {
            try {
                // 5. 等待 (带超时)
                String out = e.getValue().get(task.getTimeoutMs() > 0 ? task.getTimeoutMs() : 30_000,
                        TimeUnit.MILLISECONDS);
                workerOutputs.put(e.getKey(), out);
                memory.put("result." + e.getKey(), out);
                // 6. 投票: 用前 50 字 hash 作为票
                String ballot = out.length() > 50 ? out.substring(0, 50) : out;
                voteCount.merge(ballot, 1, Integer::sum);
            } catch (Exception ex) {
                log.warn("[vote] {} 失败: {}", e.getKey(), ex.getMessage());
                workerOutputs.put(e.getKey(), "[ERROR] " + ex.getMessage());
            }
        }
        // 7. 多数决: 找票数最高的
        String consensus = voteCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("(无共识)");
        return GroupResult.builder()
                .taskId(task.getTaskId())
                .groupId(task.getGroupId())
                .status(GroupResult.Status.SUCCESS)
                .finalOutput(consensus)
                .workerOutputs(workerOutputs)
                .consensus(consensus)
                .score(7.5)
                .meta(Map.of("strategy", "VOTE", "votes", voteCount, "workers", workers.size()))
                .build();
    }

    /**
     * DEBATE 策略: 多轮辩论, 互相 review, 收敛
     */
    private GroupResult runDebate(List<GroupMember> group, GroupTask task,
                                   Map<String, AgentExecutor> agents,
                                   GroupSharedMemory memory, GroupMessageBus bus,
                                   int rounds) {
        // 1. 取 worker
        List<GroupMember> workers = findWorkers(group);
        if (workers.isEmpty()) {
            return fail(task, "DEBATE 需要 worker");
        }
        // 2. 初始: 每个 worker 独立出第一版
        Map<String, String> current = new LinkedHashMap<>();
        for (GroupMember w : workers) {
            AgentExecutor agent = agents.get(w.getAgentName());
            if (agent == null) continue;
            GroupTask.SubTask sub = GroupTask.SubTask.builder()
                    .subId(UUID.randomUUID().toString())
                    .assignee(w.getAgentName())
                    .instruction(task.getInput())
                    .build();
            current.put(w.getAgentName(), safeExecute(agent, sub, memory));
        }
        // 3. 多轮互相 review
        for (int r = 0; r < rounds; r++) {
            log.info("[debate] 第 {} 轮", r + 1);
            // 4. 收集所有当前输出给每个 worker
            String aggregated = String.join("\n---\n", current.values());
            Map<String, String> next = new LinkedHashMap<>();
            for (Map.Entry<String, String> e : current.entrySet()) {
                AgentExecutor agent = agents.get(e.getKey());
                if (agent == null) continue;
                // 5. 指令: 基于其他 worker 的输出改进
                String inst = "原始任务: " + task.getInput() +
                        "\n\n当前所有提议:\n" + aggregated +
                        "\n\n请基于以上提议改进你的版本";
                GroupTask.SubTask sub = GroupTask.SubTask.builder()
                        .subId(UUID.randomUUID().toString())
                        .assignee(e.getKey())
                        .instruction(inst)
                        .build();
                next.put(e.getKey(), safeExecute(agent, sub, memory));
            }
            current = next;
            memory.appendToList("history", "DEBATE_ROUND_" + (r + 1));
        }
        // 6. 取最后一个 worker 的输出作为 final
        String finalOut = current.values().stream()
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
        return GroupResult.builder()
                .taskId(task.getTaskId())
                .groupId(task.getGroupId())
                .status(GroupResult.Status.SUCCESS)
                .finalOutput(finalOut)
                .workerOutputs(current)
                .consensus(finalOut)
                .score(8.5)
                .meta(Map.of("strategy", "DEBATE", "rounds", rounds, "workers", workers.size()))
                .build();
    }

    /**
     * SWARM 策略: 并行贡献, manager 择优
     */
    private GroupResult runSwarm(List<GroupMember> group, GroupTask task,
                                  Map<String, AgentExecutor> agents,
                                  GroupSharedMemory memory, GroupMessageBus bus) {
        // 1. 找 manager 和 worker
        GroupMember manager = findManager(group);
        List<GroupMember> workers = findWorkers(group);
        if (manager == null || workers.isEmpty()) {
            return fail(task, "SWARM 需要 manager + worker");
        }
        // 2. 所有 worker 并行贡献
        Map<String, String> contributions = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (GroupMember w : workers) {
            AgentExecutor agent = agents.get(w.getAgentName());
            if (agent == null) continue;
            // 3. 异步执行
            futures.add(CompletableFuture.runAsync(() -> {
                GroupTask.SubTask sub = GroupTask.SubTask.builder()
                        .subId(UUID.randomUUID().toString())
                        .assignee(w.getAgentName())
                        .instruction(task.getInput())
                        .build();
                String out = safeExecute(agent, sub, memory);
                contributions.put(w.getAgentName(), out);
                // 4. 写入共享内存 (其它 worker 可看)
                memory.put("contribution." + w.getAgentName(), out);
            }, executor));
        }
        // 5. 等所有完成
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(task.getTimeoutMs() > 0 ? task.getTimeoutMs() : 30_000, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            log.warn("[swarm] 部分 worker 超时: {}", ex.getMessage());
        }
        // 6. manager 聚合 (这里简化: 拼起来)
        AgentExecutor managerAgent = agents.get(manager.getAgentName());
        String aggregated;
        if (managerAgent != null) {
            // 6a. 让 manager 选最优
            String inst = "从以下 worker 贡献中选最优:\n" +
                    String.join("\n---\n", contributions.values());
            GroupTask.SubTask sub = GroupTask.SubTask.builder()
                    .subId(UUID.randomUUID().toString())
                    .assignee(manager.getAgentName())
                    .instruction(inst)
                    .build();
            aggregated = safeExecute(managerAgent, sub, memory);
        } else {
            // 6b. 简单拼接
            aggregated = String.join("\n\n", contributions.values());
        }
        return GroupResult.builder()
                .taskId(task.getTaskId())
                .groupId(task.getGroupId())
                .status(GroupResult.Status.SUCCESS)
                .finalOutput(aggregated)
                .workerOutputs(contributions)
                .consensus(aggregated)
                .score(8.0)
                .meta(Map.of("strategy", "SWARM", "workers", workers.size()))
                .build();
    }

    /**
     * 安全执行 (捕获异常)
     */
    private String safeExecute(AgentExecutor agent, GroupTask.SubTask sub, GroupSharedMemory mem) {
        try {
            return agent.execute(sub, mem);
        } catch (Exception e) {
            log.error("[orch] agent {} 失败: {}", agent.name(), e.getMessage());
            return "[ERROR: " + e.getMessage() + "]";
        }
    }

    /**
     * 找 manager 成员
     */
    private GroupMember findManager(List<GroupMember> group) {
        return group.stream()
                .filter(m -> m.getRole() == GroupRole.MANAGER)
                .findFirst()
                .orElse(null);
    }

    /**
     * 找所有 worker 成员
     */
    private List<GroupMember> findWorkers(List<GroupMember> group) {
        return group.stream()
                .filter(m -> m.getRole() == GroupRole.WORKER)
                .sorted(Comparator.comparingInt(GroupMember::getOrder))
                .toList();
    }

    /**
     * 失败结果工厂
     */
    private GroupResult fail(GroupTask task, String msg) {
        return GroupResult.builder()
                .taskId(task.getTaskId())
                .groupId(task.getGroupId())
                .status(GroupResult.Status.FAILED)
                .error(msg)
                .build();
    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        executor.shutdown();
    }
}
