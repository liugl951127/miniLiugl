package com.minimax.deployer.deploy;

import com.minimax.deployer.entity.ForgeDeployment;
import com.minimax.deployer.entity.ForgeRelease;
import com.minimax.deployer.mapper.ForgeDeploymentMapper;
import com.minimax.deployer.service.DeploymentLogService;
import com.minimax.deployer.state.ReleaseStateMachine;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** K8s 模拟部署 (V4.1) */
@Component
@RequiredArgsConstructor
@Slf4j
public class K8sSimStrategy implements DeploymentStrategy {

    private final ReleaseStateMachine stateMachine;
    private final DeploymentLogService logService;
    private final ForgeDeploymentMapper deploymentMapper;

    @Override public String name() { return "k8s"; }

    @Override
    public void execute(ForgeRelease release, ForgeDeployment deployment) {
        try { Thread.sleep(800); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        logService.append(deployment.getId(), "INFO", "BUILD", "镜像构建完成");
        try { Thread.sleep(600); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        logService.append(deployment.getId(), "INFO", "PUSH", "镜像推送完成");
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        logService.append(deployment.getId(), "INFO", "DEPLOY", "K8s apply 完成");
        try { Thread.sleep(400); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        logService.append(deployment.getId(), "INFO", "HEALTH", "5/5 pods ready");

        stateMachine.fire(release.getId(), ReleaseStateMachine.Event.DEPLOY);
        stateMachine.fire(release.getId(), ReleaseStateMachine.Event.READY);
        stateMachine.fire(release.getId(), ReleaseStateMachine.Event.ACTIVATE);

        deploymentMapper.update(null, new UpdateWrapper<ForgeDeployment>()
            .eq("id", deployment.getId())
            .set("status", "RUNNING")
            .set("running_replicas", deployment.getDesiredReplicas())
            .set("current_stage", "DONE")
            .set("finished_at", java.time.LocalDateTime.now()));
    }
}
