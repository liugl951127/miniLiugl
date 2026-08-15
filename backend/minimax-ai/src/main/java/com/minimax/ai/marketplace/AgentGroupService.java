package com.minimax.ai.marketplace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.ai.entity.AgentGroup;
import com.minimax.ai.framework.group.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AgentGroup 服务 (V3.0.3)
 *
 * <p>提供:
 *   - 群组定义 CRUD
 *   - 群组执行 (调用 GroupOrchestrator)
 *   - 内置 Agent 注册 (SimpleEchoAgent, 后续可接 LLM)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentGroupService {

    /** Mapper */
    private final AgentGroupMapper mapper;
    /** JSON */
    private final ObjectMapper json = new ObjectMapper();
    /** 协调器 (单例) */
    private final GroupOrchestrator orchestrator = new GroupOrchestrator();
    /** 内置 Agent 注册表 (name → executor) */
    private final Map<String, AgentExecutor> agents = new ConcurrentHashMap<>();

    /**
     * 初始化内置 Agent
     */
    public void init() {
        // 注册 SimpleEchoAgent
        registerAgent(new SimpleEchoAgent("echo-analyzer", "analyzer"));
        registerAgent(new SimpleEchoAgent("echo-writer", "writer"));
        registerAgent(new SimpleEchoAgent("echo-coder", "coder"));
        registerAgent(new SimpleEchoAgent("echo-reviewer", "reviewer"));
        registerAgent(new SimpleEchoAgent("echo-summarizer", "summarizer"));
        registerAgent(new SimpleEchoAgent("echo-translator", "translator"));
        log.info("[group-svc] 内置 {} 个 Agent 已注册", agents.size());
    }

    /**
     * 注册 Agent
     */
    public void registerAgent(AgentExecutor agent) {
        agents.put(agent.name(), agent);
    }

    /**
     * 获取所有可用 Agent
     */
    public Map<String, AgentExecutor> getAgents() {
        return new HashMap<>(agents);
    }

    /**
     * 创建群组
     */
    public AgentGroup createGroup(String name, String description, String strategy,
                                    List<GroupMember> members, Long ownerId, String tags) {
        // 1. 构造实体
        AgentGroup group = new AgentGroup();
        group.setGroupId(UUID.randomUUID().toString());
        group.setName(name);
        group.setDescription(description);
        group.setStrategy(strategy);
        group.setOwnerId(ownerId);
        group.setTags(tags);
        group.setStatus("CREATED");
        group.setRunCount(0);
        // 2. 序列化 members
        try {
            group.setMembersJson(json.writeValueAsString(members));
        } catch (Exception e) {
            throw new RuntimeException("序列化 members 失败", e);
        }
        // 3. 入库
        mapper.insert(group);
        log.info("[group-svc] 创建群组: {} ({}), 成员={}", group.getGroupId(), name, members.size());
        return group;
    }

    /**
     * 列出所有群组
     */
    public List<AgentGroup> listAll() {
        return mapper.selectList(null);
    }

    /**
     * 按 ID 查
     */
    public AgentGroup findByGroupId(String groupId) {
        return mapper.findByGroupId(groupId);
    }

    /**
     * 执行任务
     */
    public GroupResult executeTask(String groupId, String subject, String input) {
        // 1. 查群组
        AgentGroup group = mapper.findByGroupId(groupId);
        if (group == null) {
            return GroupResult.builder()
                    .status(GroupResult.Status.FAILED)
                    .error("群组不存在: " + groupId)
                    .build();
        }
        // 2. 反序列化 members
        List<GroupMember> members;
        try {
            members = json.readValue(group.getMembersJson(), new TypeReference<List<GroupMember>>() {});
        } catch (Exception e) {
            return GroupResult.builder()
                    .status(GroupResult.Status.FAILED)
                    .error("解析 members 失败: " + e.getMessage())
                    .build();
        }
        // 3. 构造任务
        GroupTask task = GroupTask.builder()
                .taskId(UUID.randomUUID().toString())
                .groupId(groupId)
                .subject(subject == null ? group.getName() : subject)
                .input(input)
                .timeoutMs(60_000)
                .build();
        // 4. 更新状态
        mapper.updateStatus(groupId, "RUNNING");
        mapper.incrementRunCount(groupId);
        // 5. 执行
        GroupStrategy strategy = GroupStrategy.valueOf(group.getStrategy());
        GroupResult result = orchestrator.execute(members, task, agents, strategy);
        // 6. 更新状态
        mapper.updateStatus(groupId, result.isSuccess() ? "COMPLETED" : "FAILED");
        return result;
    }

    /**
     * 列出策略
     */
    public List<Map<String, String>> listStrategies() {
        List<Map<String, String>> list = new ArrayList<>();
        for (GroupStrategy s : GroupStrategy.values()) {
            Map<String, String> info = new LinkedHashMap<>();
            info.put("name", s.name());
            info.put("description", strategyDesc(s));
            list.add(info);
        }
        return list;
    }

    /**
     * 列出可用 Agent
     */
    public List<Map<String, String>> listAgents() {
        List<Map<String, String>> list = new ArrayList<>();
        for (AgentExecutor a : agents.values()) {
            Map<String, String> info = new LinkedHashMap<>();
            info.put("name", a.name());
            info.put("capability", a.capability());
            list.add(info);
        }
        return list;
    }

    private String strategyDesc(GroupStrategy s) {
        return switch (s) {
            case PIPELINE -> "流水线: 顺序执行, 上一步输出作下一步输入";
            case DEBATE -> "辩论: 多 Agent 互相 review, 迭代收敛";
            case VOTE -> "投票: 多 Agent 并行独立, 多数决";
            case SWARM -> "群智: 多 Agent 自由贡献, manager 择优";
        };
    }
}
