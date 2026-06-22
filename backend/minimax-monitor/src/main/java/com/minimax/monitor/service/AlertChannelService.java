package com.minimax.monitor.service;

import com.minimax.monitor.alert.AlertNotifierManager;
import com.minimax.monitor.entity.AlertChannel;
import com.minimax.monitor.mapper.AlertChannelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 告警渠道服务 (V5.33 Day 18).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertChannelService {

    private final AlertChannelMapper channelMapper;
    private final AlertNotifierManager notifierManager;

    public List<AlertChannel> list() {
        return channelMapper.selectList(null);
    }

    public AlertChannel getById(Long id) {
        return channelMapper.selectById(id);
    }

    @Transactional
    public AlertChannel create(AlertChannel ch) {
        if (ch.getPriority() == null) ch.setPriority(0);
        if (ch.getEnabled() == null) ch.setEnabled(1);
        channelMapper.insert(ch);
        notifierManager.refreshChannels();
        return ch;
    }

    @Transactional
    public AlertChannel update(Long id, AlertChannel patch) {
        AlertChannel existing = channelMapper.selectById(id);
        if (existing == null) throw new IllegalArgumentException("channel not found: " + id);
        if (patch.getName() != null)          existing.setName(patch.getName());
        if (patch.getChannelType() != null)   existing.setChannelType(patch.getChannelType());
        if (patch.getConfig() != null)        existing.setConfig(patch.getConfig());
        if (patch.getEnabled() != null)       existing.setEnabled(patch.getEnabled());
        if (patch.getPriority() != null)      existing.setPriority(patch.getPriority());
        channelMapper.updateById(existing);
        notifierManager.refreshChannels();
        return existing;
    }

    @Transactional
    public void delete(Long id) {
        channelMapper.deleteById(id);
        notifierManager.refreshChannels();
    }
}
