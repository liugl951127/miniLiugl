package com.minimax.deployer.deploy;

import com.minimax.deployer.entity.ForgeDeployment;
import com.minimax.deployer.entity.ForgeRelease;
import com.minimax.deployer.mapper.ForgeDeploymentMapper;
import com.minimax.deployer.service.DeploymentLogService;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Docker 本地部署 (V4.1) */
@Component
@RequiredArgsConstructor
@Slf4j
public class DockerStrategy implements DeploymentStrategy {

    private final DeploymentLogService logService;
    private final ForgeDeploymentMapper deploymentMapper;

    @Override public String name() { return "docker"; }

    @Override
    public void execute(ForgeRelease release, ForgeDeployment deployment) {
        sleep(500);
        logService.append(deployment.getId(), "INFO", "BUILD", "Docker build " + release.getImageTag());
        sleep(300);
        logService.append(deployment.getId(), "INFO", "RUN", "Container started");

        deploymentMapper.update(null, new UpdateWrapper<ForgeDeployment>()
            .eq("id", deployment.getId())
            .set("status", "RUNNING")
            .set("running_replicas", 1)
            .set("current_stage", "DONE")
            .set("finished_at", LocalDateTime.now()));
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
