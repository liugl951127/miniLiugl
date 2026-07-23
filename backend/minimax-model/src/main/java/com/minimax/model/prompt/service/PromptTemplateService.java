package com.minimax.model.prompt.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minimax.model.prompt.entity.PromptTemplate;
import com.minimax.model.prompt.mapper.PromptTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Prompt 模板服务.
 * V4.3 新增 — 支持变量占位符 {{variable}} 解析 + 分类管理 + 使用统计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptTemplateService {

    private final PromptTemplateMapper mapper;

    /** 5 个系统内置模板 */
    private static final List<PromptTemplate> BUILTIN_TEMPLATES = List.of(
        createBuiltin("翻译助手", "专业翻译，支持任意语言对", "翻译",
            "你是一个专业的翻译助手。请将以下文本翻译成 {{目标语言}}：\n\n{{原文}}\n\n要求：\n1. 保持原文风格和语气\n2. 专业术语准确\n3. 流畅自然",
            "[{\"name\":\"目标语言\",\"description\":\"目标语言，如中文、英文、日文\",\"required\":true},{\"name\":\"原文\",\"description\":\"需要翻译的原始文本\",\"required\":true}]",
            1L, "系统"),
        createBuiltin("代码审查", "审查代码质量，提出改进建议", "代码",
            "你是一位资深的代码审查专家。请审查以下代码并提供详细反馈：\n\n语言：{{语言}}\n代码：\n{{代码}}\n\n请从以下维度评分并给出建议（1-10分）：\n1. 代码可读性\n2. 性能优化\n3. 安全性\n4. 最佳实践\n5. 整体评分",
            "[{\"name\":\"语言\",\"description\":\"编程语言，如 Java、Python、JavaScript\",\"required\":true},{\"name\":\"代码\",\"description\":\"需要审查的代码\",\"required\":true}]",
            1L, "系统"),
        createBuiltin("会议纪要", "从会议内容提取关键信息，生成结构化纪要", "写作",
            "你是一位专业的行政助理。请根据以下会议内容，生成一份结构化的会议纪要：\n\n会议内容：\n{{会议内容}}\n\n请包含：\n1. 会议主题\n2. 参会人员\n3. 讨论要点\n4. 决策事项\n5. 下一步行动项",
            "[{\"name\":\"会议内容\",\"description\":\"会议录音或文字记录\",\"required\":true}]",
            1L, "系统"),
        createBuiltin("营销文案", "生成吸引人的营销文案", "营销",
            "你是{{公司名}}的专业营销文案专家。请为以下产品生成营销文案：\n\n产品名称：{{产品名称}}\n产品特点：{{产品特点}}\n目标受众：{{目标受众}}\n推广渠道：{{推广渠道}}\n\n要求：\n1. 突出产品核心卖点\n2. 符合目标受众偏好\n3. 适合指定渠道风格\n4. 包含明确的 CTA（行动号召）",
            "[{\"name\":\"公司名\",\"description\":\"公司名称\",\"required\":true},{\"name\":\"产品名称\",\"description\":\"产品名称\",\"required\":true},{\"name\":\"产品特点\",\"description\":\"产品核心卖点\",\"required\":true},{\"name\":\"目标受众\",\"description\":\"目标用户群体\",\"required\":true},{\"name\":\"推广渠道\",\"description\":\"推广渠道，如小红书/抖音/公众号\",\"required\":true}]",
            1L, "系统"),
        createBuiltin("故障排查助手", "分析问题，提供排查步骤", "分析",
            "你是一位经验丰富的 SRE/运维专家。用户报告了以下问题：\n\n问题描述：{{问题描述}}\n环境信息：{{环境信息}}\n相关日志：\n{{相关日志}}\n\n请按以下步骤提供排查方案：\n1. 可能原因分析（按概率排序）\n2. 建议排查步骤（从易到难）\n3. 快速止血方案\n4. 根因定位建议",
            "[{\"name\":\"问题描述\",\"description\":\"问题的详细描述\",\"required\":true},{\"name\":\"环境信息\",\"description\":\"操作系统、版本、中间件版本等\",\"required\":false},{\"name\":\"相关日志\",\"description\":\"错误日志或相关系统日志\",\"required\":false}]",
            1L, "系统")
    );

    private static PromptTemplate createBuiltin(String name, String desc, String category,
            String content, String variables, Long creatorId, String creatorName) {
        PromptTemplate t = new PromptTemplate();
        t.setName(name);
        t.setDescription(desc);
        t.setCategory(category);
        t.setContent(content);
        t.setVariables(variables);
        t.setCreatorId(creatorId);
        t.setCreatorName(creatorName);
        t.setIsPublic(true);
        t.setUseCount(0);
        return t;
    }

    // ---------- CRUD ----------

    /**
     * 分页查询 (公开模板 + 自己创建的).
     */
    public IPage<PromptTemplate> page(Long userId, int current, int size,
            String category, String keyword) {
        LambdaQueryWrapper<PromptTemplate> q = new LambdaQueryWrapper<>();
        q.and(w -> w.eq(PromptTemplate::getIsPublic, true).or().eq(PromptTemplate::getCreatorId, userId));
        if (category != null && !category.isBlank()) {
            q.eq(PromptTemplate::getCategory, category);
        }
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(PromptTemplate::getName, keyword)
                    .or().like(PromptTemplate::getDescription, keyword));
        }
        q.orderByDesc(PromptTemplate::getUseCount, PromptTemplate::getCreatedAt);
        return mapper.selectPage(new Page<>(current, size), q);
    }

    /**
     * 获取详情.
     */
    public PromptTemplate getById(Long id, Long userId) {
        PromptTemplate t = mapper.selectById(id);
        if (t == null || t.getDeleted() == 1) return null;
        if (!t.getIsPublic() && !t.getCreatorId().equals(userId)) return null;
        return t;
    }

    /**
     * 创建模板.
     */
    @Transactional
    public PromptTemplate create(PromptTemplate t, Long userId, String userName) {
        t.setId(null);
        t.setCreatorId(userId);
        t.setCreatorName(userName);
        t.setUseCount(0);
        t.setIsPublic(t.getIsPublic() != null ? t.getIsPublic() : false);
        mapper.insert(t);
        return t;
    }

    /**
     * 更新模板 (仅创建者可更新).
     */
    @Transactional
    public PromptTemplate update(Long id, PromptTemplate updates, Long userId) {
        PromptTemplate t = mapper.selectById(id);
        if (t == null || t.getDeleted() == 1) return null;
        if (!t.getCreatorId().equals(userId)) throw new SecurityException("仅创建者可修改此模板");
        if (updates.getName() != null) t.setName(updates.getName());
        if (updates.getDescription() != null) t.setDescription(updates.getDescription());
        if (updates.getCategory() != null) t.setCategory(updates.getCategory());
        if (updates.getContent() != null) t.setContent(updates.getContent());
        if (updates.getVariables() != null) t.setVariables(updates.getVariables());
        if (updates.getIsPublic() != null) t.setIsPublic(updates.getIsPublic());
        mapper.updateById(t);
        return t;
    }

    /**
     * 删除模板 (仅创建者可删除).
     */
    @Transactional
    public boolean delete(Long id, Long userId) {
        PromptTemplate t = mapper.selectById(id);
        if (t == null || t.getDeleted() == 1) return false;
        if (!t.getCreatorId().equals(userId)) throw new SecurityException("仅创建者可删除此模板");
        mapper.deleteById(id);
        return true;
    }

    /**
     * 使用计数 +1.
     */
    @Transactional
    public void incrementUseCount(Long id) {
        PromptTemplate t = mapper.selectById(id);
        if (t == null) return;
        t.setUseCount(t.getUseCount() == null ? 1 : t.getUseCount() + 1);
        mapper.updateById(t);
    }

    /**
     * 获取所有分类.
     */
    public List<String> categories() {
        LambdaQueryWrapper<PromptTemplate> q = new LambdaQueryWrapper<>();
        q.select(PromptTemplate::getCategory)
                .eq(PromptTemplate::getIsPublic, true)
                .isNotNull(PromptTemplate::getCategory)
                .groupBy(PromptTemplate::getCategory);
        return mapper.selectList(q).stream()
                .map(PromptTemplate::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // ---------- 变量解析 ----------

    /** 提取模板中所有 {{variable}} 占位符变量名 */
    public List<String> extractVariables(String content) {
        if (content == null || content.isBlank()) return Collections.emptyList();
        Pattern pattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
        Matcher matcher = pattern.matcher(content);
        List<String> vars = new ArrayList<>();
        while (matcher.find()) {
            vars.add(matcher.group(1).trim());
        }
        return vars;
    }

    /** 用变量值替换占位符，生成最终 prompt */
    public String resolve(String content, Map<String, String> values) {
        if (content == null || values == null || values.isEmpty()) return content;
        String result = content;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    // ---------- 内置模板初始化 (启动时调用) ----------
    @Transactional
    public void initBuiltin() {
        for (PromptTemplate builtin : BUILTIN_TEMPLATES) {
            LambdaQueryWrapper<PromptTemplate> q = new LambdaQueryWrapper<>();
            q.eq(PromptTemplate::getName, builtin.getName())
                    .eq(PromptTemplate::getCreatorId, 1L);
            long count = mapper.selectCount(q);
            if (count == 0) {
                mapper.insert(builtin);
                log.info("✅ 初始化内置模板: {}", builtin.getName());
            }
        }
    }
}
