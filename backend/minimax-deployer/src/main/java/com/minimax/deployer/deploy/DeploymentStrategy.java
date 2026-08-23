package com.minimax.deployer.deploy;

import com.minimax.deployer.entity.ForgeDeployment;
import com.minimax.deployer.entity.ForgeRelease;

/**
 * 部署策略接口 (V4.1)
 *
 * V4.1 拆 DeploymentService 为 Strategy 模式:
 *  - k8s:    K8sSimStrategy (模拟)
 *  - gitops: GitOpsStrategy (渲染 ArgoCD CRD)
 *  - edge:   EdgeStrategy
 *  - docker: DockerStrategy
 *
 * 每个 strategy 独立实现, 互不重复。
 */
public interface DeploymentStrategy {

    /** 策略名 (对应 release.deploy_target 字段) */
    String name();

    /**
     * 执行部署
     * @param release   release 主记录
     * @param deployment 新建的 deployment 记录
     */
    void execute(ForgeRelease release, ForgeDeployment deployment);
}
