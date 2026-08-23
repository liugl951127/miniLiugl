package com.minimax.deployer.service;

import com.minimax.deployer.entity.ForgeDeploymentLog;
import com.minimax.deployer.mapper.ForgeDeploymentLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 部署日志服务 (V4.1)
 *
 * V4.1 抽出: 4 个 Strategy 都调用, 不再各自 new
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeploymentLogService {

    private final ForgeDeploymentLogMapper logMapper;

    public void append(Long deploymentId, String level, String stage, String message) {
        logMapper.insert(ForgeDeploymentLog.builder()
            .deploymentId(deploymentId)
            .level(level)
            .stage(stage)
            .message(message)
            .createdAt(LocalDateTime.now())
            .build());
        log.info("[Deploy {}] [{}] [{}] {}", deploymentId, level, stage, message);
    }
}
