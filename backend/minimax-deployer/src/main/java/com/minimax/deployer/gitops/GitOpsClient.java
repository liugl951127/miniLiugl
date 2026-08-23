package com.minimax.deployer.gitops;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Git 操作客户端 (V5.0)
 *
 * 用 JGit 真实做: clone → 写文件 → commit → push
 * 失败时抛 GitOpsException, 不会假装成功。
 *
 * 凭证:
 *  - agent-forge.gitops.username / password (HTTPS basic auth)
 *  - 或 SSH key (V5.0 暂不实现)
 */
@Service
@Slf4j
public class GitOpsClient {

    @Value("${agent-forge.gitops.username:}")
    private String username;

    @Value("${agent-forge.gitops.password:}")
    private String password;

    @Value("${agent-forge.gitops.local-path:/tmp/agent-forge-gitops}")
    private String localPath;

    /**
     * 推送 manifests 到 Git 仓库
     *
     * @param repoUrl  Git URL
     * @param branch   分支
     * @param basePath 仓库内基础路径 (e.g. "agents/v1.0.0")
     * @param files    路径 → 内容
     * @return commit SHA
     */
    public String pushManifests(String repoUrl, String branch, String basePath, Map<String, String> files)
            throws GitOpsException {
        log.info("[GitOps] push: repo={} branch={} path={} files={}", repoUrl, branch, basePath, files.size());

        File workDir = new File(localPath, sanitizeBranch(branch));
        try {
            // 1. Clone (或 pull 如果已存在)
            Git git;
            if (workDir.exists() && new File(workDir, ".git").exists()) {
                log.info("[GitOps] workDir 已存在, pull: {}", workDir.getAbsolutePath());
                git = Git.open(workDir);
                git.checkout().setName(branch).call();
                git.pull().setCredentialsProvider(creds()).call();
            } else {
                log.info("[GitOps] clone: {}", repoUrl);
                Files.createDirectories(workDir.toPath());
                git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setBranch(branch)
                    .setDirectory(workDir)
                    .setCredentialsProvider(creds())
                    .call();
            }

            // 2. 写文件
            Path baseDir = workDir.toPath().resolve(basePath);
            Files.createDirectories(baseDir);
            for (var entry : files.entrySet()) {
                Path filePath = baseDir.resolve(entry.getKey());
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, entry.getValue(), StandardCharsets.UTF_8);
                log.debug("[GitOps] 写入 {}", filePath);
            }

            // 3. Commit + Push
            git.add().addFilepattern(basePath + "/.").call();
            String commitMsg = "agent-forge: deploy " + basePath + " (" + files.size() + " files)";
            var status = git.status().call();
            if (status.getAdded().isEmpty() && status.getModified().isEmpty() && status.getChanged().isEmpty()) {
                log.warn("[GitOps] 无变更, 跳过 commit");
                return "no-changes";
            }
            var rev = git.commit().setMessage(commitMsg).setAuthor("agent-forge", "agent-forge@minimax.io").call();
            log.info("[GitOps] commit: {} {}", rev.getName(), commitMsg);

            git.push().setCredentialsProvider(creds()).call();
            log.info("[GitOps] push 成功: {}", rev.getName());
            return rev.getName();

        } catch (GitAPIException | java.io.IOException e) {
            throw new GitOpsException("Git push 失败: " + e.getMessage(), e);
        }
    }

    private UsernamePasswordCredentialsProvider creds() {
        if (username == null || username.isBlank()) {
            return new UsernamePasswordCredentialsProvider("", "");
        }
        return new UsernamePasswordCredentialsProvider(username, password != null ? password : "");
    }

    private String sanitizeBranch(String branch) {
        return branch.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /** 包装异常, 不暴露 JGit 细节 */
    public static class GitOpsException extends Exception {
        public GitOpsException(String msg, Throwable cause) { super(msg, cause); }
    }
}
