package com.minimax.deployer.service;

import com.minimax.deployer.entity.AgentTemplate;
import com.minimax.deployer.mapper.AgentTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 智能体模板服务 (V2.0)
 *
 * 提供 6 个预置行业模板:
 *  - 教育客服 / 电商客服 / 代码评审 / 金融风控 / 医疗问诊 / 自定义
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentTemplateService {

    private final AgentTemplateMapper templateMapper;

    /**
     * 列出所有已发布模板
     */
    public List<AgentTemplate> listPublished() {
        return templateMapper.findAllPublished();
    }

    public List<AgentTemplate> listByIndustry(String industry) {
        return templateMapper.findByIndustry(industry);
    }

    public AgentTemplate getById(Long id) {
        return templateMapper.selectById(id);
    }

    public AgentTemplate getByCode(String code) {
        return templateMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AgentTemplate>()
                .eq("code", code)
        );
    }
}
