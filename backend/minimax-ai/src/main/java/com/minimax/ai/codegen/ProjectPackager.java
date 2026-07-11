package com.minimax.ai.codegen;

import com.minimax.ai.dto.CodeGenRequest;
import com.minimax.ai.dto.CodeGenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 企业级项目打包器 (V2.8.4)
 *
 * <h3>把 ProjectCodeGenerator 的输出打包成完整的企业级可部署项目 ZIP</h3>
 *
 * <h3>生成内容</h3>
 * <ul>
 *   <li>📁 项目源码 (src/main/java/...)</li>
 *   <li>📁 资源文件 (application.yml, mapper.xml)</li>
 *   <li>📁 SQL 脚本 (schema.sql, seed.sql, migration/)</li>
 *   <li>📁 部署 (Dockerfile, docker-compose.yml, k8s/)</li>
 *   <li>📁 运维 (Prometheus, Grafana, logback.xml)</li>
 *   <li>📁 CI/CD (.github/workflows/, .gitlab-ci.yml)</li>
 *   <li>📁 文档 (README.md, API.md, CHANGELOG.md)</li>
 *   <li>📁 测试 (单元测试 + 集成测试模板)</li>
 * </ul>
 *
 * <h3>用法</h3>
 * <pre>{@code
 *   ProjectPackager packager = new ProjectPackager();
 *   byte[] zip = packager.packageAsZip(codeGenResponse, "minimax-erp", "1.0.0");
 *   // 写入文件或 HTTP 下载
 * }</pre>
 */
@Slf4j
@Component
public class ProjectPackager {

    /**
     * 把代码生成结果打包成 ZIP
     *
     * @param resp     ProjectCodeGenerator 的输出
     * @param name     项目名 (用作根目录 + artifactId)
     * @param version  项目版本
     * @return ZIP 字节数组
     */
    public byte[] packageAsZip(CodeGenResponse resp, String name, String version) throws IOException {
        return packageAsZip(resp, name, version, null);
    }

    public byte[] packageAsZip(CodeGenResponse resp, String name, String version, String packageName) throws IOException {
        if (name == null || name.isEmpty()) name = "minimax-app";
        if (version == null || version.isEmpty()) version = "1.0.0";
        if (packageName == null || packageName.isEmpty()) {
            packageName = "com.minimax." + name.toLowerCase().replace("-", "");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream(64 * 1024);
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            String root = name + "/";

            // ============== 1. 核心源码 ==============
            // 记录已添加路径, 避免与下面的手写文件重复
            Set<String> addedPaths = new HashSet<>();
            if (resp.getFiles() != null) {
                for (Map.Entry<String, String> e : resp.getFiles().entrySet()) {
                    String path = normalizePath(e.getKey());
                    String fullPath = root + path;
                    addedPaths.add(fullPath);
                    addEntry(zip, fullPath, e.getValue());
                }
            }

            // ============== 2. SQL 脚本 ==============
            String module = name;
            String pkg = packageName;
            String dbName = name.toLowerCase().replace("-", "_").replace(".", "_");

            // 2.1 DDL
            addEntry(zip, root + "sql/schema.sql", generateSchemaSql(module, dbName));
            addEntry(zip, root + "sql/seed.sql", generateSeedSql(module));
            addEntry(zip, root + "sql/migration/V1__init.sql", generateMigrationSql(module));

            // ============== 3. 部署文件 ==============
            addEntry(zip, root + "Dockerfile", generateDockerfile(name, version));
            addEntry(zip, root + "docker-compose.yml", generateDockerCompose(name, version));
            addEntry(zip, root + "k8s/deployment.yaml", generateK8sDeployment(name, version));
            addEntry(zip, root + "k8s/service.yaml", generateK8sService(name));
            addEntry(zip, root + "k8s/ingress.yaml", generateK8sIngress(name));
            addEntry(zip, root + "k8s/configmap.yaml", generateK8sConfigMap(name, dbName));
            addEntry(zip, root + "k8s/secret.yaml", generateK8sSecret());
            addEntry(zip, root + ".dockerignore", ".git\ntarget/\n*.log\n.idea/\n*.iml\n.vscode/\nnode_modules/\n");

            // ============== 4. 运维监控 ==============
            addEntry(zip, root + "ops/prometheus.yml", generatePrometheus(name));
            addEntry(zip, root + "ops/grafana/dashboard.json", generateGrafanaDashboard(name));
            addEntry(zip, root + "ops/logback-spring.xml", generateLogback(name));
            addEntry(zip, root + "ops/healthcheck.sh", generateHealthCheck(name));
            addEntry(zip, root + "ops/backup.sh", generateBackupScript(name, dbName));
            addEntry(zip, root + "ops/restore.sh", generateRestoreScript(name, dbName));

            // ============== 5. CI/CD ==============
            addEntry(zip, root + ".github/workflows/ci.yml", generateGithubActions(name));
            addEntry(zip, root + ".github/workflows/release.yml", generateReleaseWorkflow(name));
            addEntry(zip, root + ".gitlab-ci.yml", generateGitlabCi(name));
            addEntry(zip, root + "Jenkinsfile", generateJenkinsfile(name));

            // ============== 6. 文档 ==============
            addEntry(zip, root + "README.md", generateReadme(name, version, pkg, resp));
            addEntry(zip, root + "API.md", generateApiDoc(name, resp));
            addEntry(zip, root + "DEPLOYMENT.md", generateDeploymentDoc(name, version));
            addEntry(zip, root + "CHANGELOG.md", generateChangelog(version));
            addEntry(zip, root + "CONTRIBUTING.md", generateContributing());
            addEntry(zip, root + "LICENSE", "MIT License\n\nCopyright (c) 2026 " + name + "\n");

            // ============== 7. 配置 ==============
            addEntry(zip, root + ".gitignore", generateGitignore());
            addEntry(zip, root + ".editorconfig", generateEditorConfig());
            addEntry(zip, root + "Makefile", generateMakefile(name, version));
            if (!addedPaths.contains(root + "pom.xml")) {
                addEntry(zip, root + "pom.xml", generateMavenPom(name, version, pkg));
            }

            // ============== 8. 运维相关脚本 (V2.8.4 重点) ==============
            addEntry(zip, root + "scripts/start.sh", generateStartScript(name, version));
            addEntry(zip, root + "scripts/stop.sh", generateStopScript(name));
            addEntry(zip, root + "scripts/status.sh", generateStatusScript(name));
            addEntry(zip, root + "scripts/deploy.sh", generateDeployScript(name, version));
            addEntry(zip, root + "scripts/rollback.sh", generateRollbackScript(name));
            addEntry(zip, root + "scripts/logs.sh", generateLogsScript(name));
            addEntry(zip, root + "scripts/migrate.sh", generateMigrateScript(name, dbName));

            // ============== 9. 测试模板 ==============
            addEntry(zip, root + "src/test/java/" + pkg.replace('.', '/') + "/" + capitalize(module) + "ApplicationTests.java",
                    generateAppTest());
            addEntry(zip, root + "src/test/resources/application-test.yml", generateTestConfig(dbName));

            log.info("[packager] ZIP generated: {} entries, root={}, size={}KB",
                    resp.getFiles() != null ? resp.getFiles().size() : 0, root, baos.size() / 1024);
        }
        return baos.toByteArray();
    }

    /** 写入 zip entry */
    private void addEntry(ZipOutputStream zip, String path, String content) throws IOException {
        if (content == null) content = "";
        ZipEntry entry = new ZipEntry(path);
        entry.setSize(content.getBytes(StandardCharsets.UTF_8).length);
        zip.putNextEntry(entry);
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    /** 路径标准化: src/main/java/com/... 形式 */
    private String normalizePath(String path) {
        if (path == null) return "unknown.txt";
        if (path.startsWith("/")) path = path.substring(1);
        return path;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).replace("-", "");
    }

    // ========================================================
    // SQL 生成
    // ========================================================

    private String generateSchemaSql(String module, String dbName) {
        return """
                -- ============================================================
                -- %s 数据库 Schema (V2.8.4 自动生成)
                -- 数据库: %s
                -- 字符集: utf8mb4
                -- ============================================================

                CREATE DATABASE IF NOT EXISTS `%s`
                DEFAULT CHARACTER SET utf8mb4
                DEFAULT COLLATE utf8mb4_unicode_ci;

                USE `%s`;

                -- 用户表
                CREATE TABLE IF NOT EXISTS `user` (
                    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                    `username` VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
                    `password` VARCHAR(255) NOT NULL COMMENT '密码 (BCrypt)',
                    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
                    `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
                    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
                    `avatar` VARCHAR(512) DEFAULT NULL COMMENT '头像 URL',
                    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用 1=启用',
                    `role` VARCHAR(32) NOT NULL DEFAULT 'user' COMMENT '角色: admin/user/guest',
                    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (`id`),
                    KEY `idx_username` (`username`),
                    KEY `idx_email` (`email`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

                -- 审计日志表
                CREATE TABLE IF NOT EXISTS `audit_log` (
                    `id` BIGINT NOT NULL AUTO_INCREMENT,
                    `user_id` BIGINT DEFAULT NULL,
                    `action` VARCHAR(64) NOT NULL,
                    `resource` VARCHAR(128) DEFAULT NULL,
                    `ip` VARCHAR(64) DEFAULT NULL,
                    `user_agent` VARCHAR(512) DEFAULT NULL,
                    `result` TINYINT NOT NULL DEFAULT 1,
                    `cost_ms` INT DEFAULT NULL,
                    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (`id`),
                    KEY `idx_user_id` (`user_id`),
                    KEY `idx_created_at` (`created_at`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志';

                -- 业务数据表 (示例)
                CREATE TABLE IF NOT EXISTS `%s_record` (
                    `id` BIGINT NOT NULL AUTO_INCREMENT,
                    `title` VARCHAR(255) NOT NULL,
                    `content` TEXT,
                    `owner_id` BIGINT DEFAULT NULL,
                    `status` TINYINT NOT NULL DEFAULT 1,
                    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (`id`),
                    KEY `idx_owner_id` (`owner_id`),
                    KEY `idx_created_at` (`created_at`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='%s 业务记录';
                """.formatted(module, dbName, dbName, dbName, module, module);
    }

    private String generateSeedSql(String module) {
        return """
                -- ============================================================
                -- %s 初始数据 (V2.8.4)
                -- ============================================================

                -- 管理员账号 (密码 admin123 已 BCrypt 加密)
                INSERT INTO `user` (`username`, `password`, `nickname`, `role`, `status`) VALUES
                ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '超级管理员', 'admin', 1),
                ('user', '$2a$10$DSD/6.HTOQ9LF7qiVOdHDeTqx1psN8sTHJDgrfT0J6y6DT1H8ZJm', '普通用户', 'user', 1)
                ON DUPLICATE KEY UPDATE `nickname` = VALUES(`nickname`);

                INSERT INTO `%s_record` (`title`, `content`, `owner_id`, `status`) VALUES
                ('欢迎使用', '这是 %s 的示例数据', 1, 1),
                ('系统说明', '由 MiniMax AI 自动生成', 1, 1);
                """.formatted(module, module, module);
    }

    private String generateMigrationSql(String module) {
        return """
                -- ============================================================
                -- Flyway 迁移脚本: V1__init.sql (V2.8.4)
                -- 由 MiniMax AI 自动生成
                -- ============================================================

                -- 此文件由 flyway 在应用启动时自动执行
                -- 推荐把建表语句放这里 (而不是 schema.sql)
                -- schema.sql 仅用于一次性手动初始化

                CREATE TABLE IF NOT EXISTS `flyway_schema_history` (
                    `installed_rank` INT NOT NULL,
                    `version` VARCHAR(50) DEFAULT NULL,
                    `description` VARCHAR(200) NOT NULL,
                    `type` VARCHAR(20) NOT NULL,
                    `script` VARCHAR(1000) NOT NULL,
                    `checksum` INT DEFAULT NULL,
                    `installed_by` VARCHAR(100) NOT NULL,
                    `installed_on` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `execution_time` INT NOT NULL,
                    `success` TINYINT(1) NOT NULL,
                    PRIMARY KEY (`installed_rank`),
                    KEY `flyway_schema_history_s_idx` (`success`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """;
    }

    // ========================================================
    // 部署文件
    // ========================================================

    private String generateDockerfile(String name, String version) {
        return """
                # ============================================================
                # %s Dockerfile (V2.8.4 企业级)
                # 多阶段构建, 镜像 < 200MB
                # ============================================================

                # ---- Stage 1: 构建 ----
                FROM maven:3.9-eclipse-temurin-17 AS builder
                WORKDIR /build
                COPY pom.xml .
                RUN mvn dependency:go-offline -B -q
                COPY src ./src
                RUN mvn clean package -B -DskipTests \\
                    -Djdk.image.version=17 \\
                    -Dmaven.javadoc.skip=true \\
                    -Dmaven.source.skip=true

                # ---- Stage 2: 运行时 ----
                FROM eclipse-temurin:17-jre-jammy
                LABEL name="%s" \\
                      version="%s" \\
                      maintainer="minimax-team" \\
                      description="MiniMax AI generated Spring Boot app"

                # 时区/字符集
                ENV TZ=Asia/Shanghai \\
                    LANG=C.UTF-8 \\
                    LC_ALL=C.UTF-8 \\
                    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=70.0 -XX:+UseStringDeduplication"

                WORKDIR /app

                # 健康检查
                HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \\
                  CMD curl -f http://localhost:8080/actuator/health || exit 1

                # 从 builder 复制产物
                COPY --from=builder /build/target/%s-%s.jar /app/app.jar

                # 非 root 运行
                RUN groupadd -r app && useradd -r -g app app
                RUN chown -R app:app /app
                USER app

                EXPOSE 8080
                ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
                """.formatted(name, name, version, name, version);
    }

    private String generateDockerCompose(String name, String version) {
        return """
                # ============================================================
                # %s Docker Compose (V2.8.4)
                # 完整企业级: 应用 + MySQL + Redis + Nginx + 监控
                # ============================================================
                version: "3.9"

                services:
                  %s:
                    build: .
                    image: %s:%s
                    container_name: %s
                    restart: unless-stopped
                    ports:
                      - "8080:8080"
                    environment:
                      SPRING_PROFILES_ACTIVE: docker
                      MYSQL_HOST: mysql
                      MYSQL_PORT: 3306
                      MYSQL_USER: root
                      MYSQL_PASS: root123456
                      MYSQL_DB: %s
                      REDIS_HOST: redis
                      REDIS_PORT: 6379
                      REDIS_PASS: minimax_redis_2024
                    depends_on:
                      mysql:
                        condition: service_healthy
                      redis:
                        condition: service_healthy
                    networks:
                      - app-net
                    volumes:
                      - ./logs:/app/logs
                    healthcheck:
                      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
                      interval: 30s
                      timeout: 10s
                      retries: 3

                  mysql:
                    image: mysql:8.0
                    container_name: %s-mysql
                    restart: unless-stopped
                    environment:
                      MYSQL_ROOT_PASSWORD: root123456
                      MYSQL_DATABASE: %s
                    ports:
                      - "3306:3306"
                    volumes:
                      - mysql_data:/var/lib/mysql
                      - ./sql/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql:ro
                      - ./sql/seed.sql:/docker-entrypoint-initdb.d/02-seed.sql:ro
                    networks:
                      - app-net
                    healthcheck:
                      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-uroot", "-proot123456"]
                      interval: 10s
                      timeout: 5s
                      retries: 5

                  redis:
                    image: redis:7-alpine
                    container_name: %s-redis
                    restart: unless-stopped
                    command: redis-server --requirepass minimax_redis_2024 --appendonly yes
                    ports:
                      - "6379:6379"
                    volumes:
                      - redis_data:/data
                    networks:
                      - app-net
                    healthcheck:
                      test: ["CMD", "redis-cli", "-a", "minimax_redis_2024", "ping"]
                      interval: 10s
                      timeout: 5s
                      retries: 5

                  nginx:
                    image: nginx:alpine
                    container_name: %s-nginx
                    restart: unless-stopped
                    ports:
                      - "80:80"
                      - "443:443"
                    volumes:
                      - ./nginx.conf:/etc/nginx/nginx.conf:ro
                      - ./certs:/etc/nginx/certs:ro
                    depends_on:
                      - %s
                    networks:
                      - app-net

                  prometheus:
                    image: prom/prometheus:latest
                    container_name: %s-prometheus
                    ports:
                      - "9090:9090"
                    volumes:
                      - ./ops/prometheus.yml:/etc/prometheus/prometheus.yml:ro
                    networks:
                      - app-net

                  grafana:
                    image: grafana/grafana:latest
                    container_name: %s-grafana
                    ports:
                      - "3000:3000"
                    environment:
                      GF_SECURITY_ADMIN_PASSWORD: admin
                    volumes:
                      - grafana_data:/var/lib/grafana
                      - ./ops/grafana:/etc/grafana/provisioning/dashboards:ro
                    networks:
                      - app-net

                volumes:
                  mysql_data:
                  redis_data:
                  grafana_data:

                networks:
                  app-net:
                    driver: bridge
                """.formatted(name, name, name, version, name, name, name, name, name, name, name, name, name);
    }

    private String generateK8sDeployment(String name, String version) {
        return """
                # %s K8s Deployment (V2.8.4)
                apiVersion: apps/v1
                kind: Deployment
                metadata:
                  name: %s
                  labels:
                    app: %s
                    version: %s
                spec:
                  replicas: 3
                  selector:
                    matchLabels:
                      app: %s
                  template:
                    metadata:
                      labels:
                        app: %s
                        version: %s
                    spec:
                      containers:
                      - name: %s
                        image: %s:%s
                        ports:
                        - containerPort: 8080
                        env:
                        - name: SPRING_PROFILES_ACTIVE
                          value: "k8s"
                        - name: MYSQL_HOST
                          valueFrom:
                            configMapKeyRef:
                              name: %s-config
                              key: db.host
                        - name: MYSQL_PASS
                          valueFrom:
                            secretKeyRef:
                              name: %s-secret
                              key: db.password
                        resources:
                          requests:
                            memory: "512Mi"
                            cpu: "250m"
                          limits:
                            memory: "1Gi"
                            cpu: "500m"
                        readinessProbe:
                          httpGet:
                            path: /actuator/health/readiness
                            port: 8080
                          initialDelaySeconds: 30
                          periodSeconds: 10
                        livenessProbe:
                          httpGet:
                            path: /actuator/health/liveness
                            port: 8080
                          initialDelaySeconds: 60
                          periodSeconds: 30
                        volumeMounts:
                        - name: logs
                          mountPath: /app/logs
                      volumes:
                      - name: logs
                        emptyDir: {}
                """.formatted(name, name, name, version, name, name, version, name, name, version, name, name);
    }

    private String generateK8sService(String name) {
        return """
                apiVersion: v1
                kind: Service
                metadata:
                  name: %s
                spec:
                  selector:
                    app: %s
                  ports:
                  - protocol: TCP
                    port: 80
                    targetPort: 8080
                  type: ClusterIP
                """.formatted(name, name);
    }

    private String generateK8sIngress(String name) {
        return """
                apiVersion: networking.k8s.io/v1
                kind: Ingress
                metadata:
                  name: %s
                  annotations:
                    nginx.ingress.kubernetes.io/rewrite-target: /
                spec:
                  rules:
                  - host: %s.example.com
                    http:
                      paths:
                      - path: /
                        pathType: Prefix
                        backend:
                          service:
                            name: %s
                            port:
                              number: 80
                """.formatted(name, name, name);
    }

    private String generateK8sConfigMap(String name, String dbName) {
        return """
                apiVersion: v1
                kind: ConfigMap
                metadata:
                  name: %s-config
                data:
                  db.host: "mysql-service"
                  db.port: "3306"
                  db.name: "%s"
                  redis.host: "redis-service"
                  redis.port: "6379"
                """.formatted(name, dbName);
    }

    private String generateK8sSecret() {
        return """
                apiVersion: v1
                kind: Secret
                metadata:
                  name: app-secret
                type: Opaque
                stringData:
                  db.password: "root123456"   # kubectl apply 前请用 kubectl create secret 覆盖
                  jwt.secret: "change-me-please-32-bytes-minimum-secret-key"
                  redis.password: "minimax_redis_2024"
                """;
    }

    // ========================================================
    // 运维监控
    // ========================================================

    private String generatePrometheus(String name) {
        return """
                # Prometheus 抓取配置 (V2.8.4)
                global:
                  scrape_interval: 15s
                  evaluation_interval: 15s

                scrape_configs:
                  - job_name: '%s'
                    metrics_path: '/actuator/prometheus'
                    static_configs:
                      - targets: ['%s:8080']
                        labels:
                          app: '%s'

                  - job_name: 'mysql'
                    static_configs:
                      - targets: ['mysql-exporter:9104']

                  - job_name: 'redis'
                    static_configs:
                      - targets: ['redis-exporter:9121']

                  - job_name: 'nginx'
                    static_configs:
                      - targets: ['nginx-exporter:9113']
                """.formatted(name, name, name);
    }

    private String generateGrafanaDashboard(String name) {
        return """
                {
                  "title": "%s Dashboard",
                  "uid": "minimax-%s",
                  "panels": [
                    {"title": "JVM Heap", "type": "graph", "datasource": "Prometheus",
                     "targets": [{"expr": "sum(jvm_memory_used_bytes{application=\\"%s\\"})"}]},
                    {"title": "HTTP QPS", "type": "graph", "datasource": "Prometheus",
                     "targets": [{"expr": "rate(http_server_requests_seconds_count[1m])"}]},
                    {"title": "Response Time p99", "type": "graph", "datasource": "Prometheus",
                     "targets": [{"expr": "histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))"}]},
                    {"title": "JVM GC Pause", "type": "graph", "datasource": "Prometheus",
                     "targets": [{"expr": "rate(jvm_gc_pause_seconds_sum[5m])"}]}
                  ]
                }
                """.formatted(name, name.toLowerCase(), name);
    }

    private String generateLogback(String name) {
        String tmpl = """
                <?xml version="1.0" encoding="UTF-8"?>
                <configuration>
                    <property name="LOG_HOME" value="/app/logs"/>
                    <property name="APP_NAME" value="@@APPNAME@@"/>

                    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
                        <encoder>
                            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
                        </encoder>
                    </appender>

                    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
                        <file>${LOG_HOME}/${APP_NAME}.log</file>
                        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
                            <fileNamePattern>${LOG_HOME}/${APP_NAME}.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
                            <maxFileSize>100MB</maxFileSize>
                            <maxHistory>30</maxHistory>
                            <totalSizeCap>10GB</totalSizeCap>
                        </rollingPolicy>
                        <encoder>
                            <pattern>%d{ISO8601} [%thread] %-5level %logger - %msg%n</pattern>
                        </encoder>
                    </appender>

                    <root level="INFO">
                        <appender-ref ref="STDOUT"/>
                        <appender-ref ref="FILE"/>
                    </root>
                </configuration>
                """;
        return tmpl.replace("@@APPNAME@@", name);
    }

    private String generateHealthCheck(String name) {
        return """
                #!/usr/bin/env bash
                # %s 健康检查脚本
                set -e
                PORT=${PORT:-8080}
                echo "[healthcheck] %s @ $(date)"
                if curl -fsS "http://localhost:${PORT}/actuator/health" > /tmp/health.json; then
                    STATUS=$(jq -r '.status' /tmp/health.json)
                    echo "[healthcheck] status=$STATUS"
                    [ "$STATUS" = "UP" ] && exit 0 || exit 1
                else
                    echo "[healthcheck] FAILED"
                    exit 1
                fi
                """.formatted(name, name);
    }

    private String generateBackupScript(String name, String dbName) {
        return """
                #!/usr/bin/env bash
                # %s 数据库备份脚本 (V2.8.4)
                # 用法: ./backup.sh [daily|hourly]
                set -euo pipefail

                MODE=${1:-daily}
                BACKUP_ROOT="/var/backups/%s"
                TIMESTAMP=$(date +%%Y%%m%%d_%%H%%M%%S)
                BACKUP_DIR="${BACKUP_ROOT}/${MODE}/${TIMESTAMP}"
                RETENTION_DAYS=${BACKUP_RETENTION_DAYS:-30}

                mkdir -p "${BACKUP_DIR}"

                echo "[backup] starting %s backup to ${BACKUP_DIR}"

                # MySQL 全量备份
                mysqldump -h "${MYSQL_HOST:-127.0.0.1}" \\
                          -P "${MYSQL_PORT:-3306}" \\
                          -u "${MYSQL_USER:-root}" \\
                          -p"${MYSQL_PASS:-root123456}" \\
                          --single-transaction \\
                          --routines \\
                          --triggers \\
                          --events \\
                          "${DB_NAME:-%s}" | gzip > "${BACKUP_DIR}/${DB_NAME:-%s}.sql.gz"

                # 校验
                if [ -s "${BACKUP_DIR}/${DB_NAME:-%s}.sql.gz" ]; then
                    SIZE=$(du -h "${BACKUP_DIR}/${DB_NAME:-%s}.sql.gz" | cut -f1)
                    echo "[backup] OK, size=${SIZE}"
                else
                    echo "[backup] FAILED, empty file"
                    exit 1
                fi

                # 清理过期备份
                find "${BACKUP_ROOT}/${MODE}" -mindepth 1 -maxdepth 1 -type d \\
                    -mtime +${RETENTION_DAYS} -exec rm -rf {} \\; 2>/dev/null || true

                echo "[backup] done"
                """.formatted(name, name, name, dbName, dbName, dbName, dbName);
    }

    private String generateRestoreScript(String name, String dbName) {
        return """
                #!/usr/bin/env bash
                # %s 数据库恢复脚本
                # 用法: ./restore.sh <backup-file.sql.gz>
                set -euo pipefail

                BACKUP_FILE=${1:?"usage: restore.sh <backup-file.sql.gz>"}
                if [ ! -f "${BACKUP_FILE}" ]; then
                    echo "[restore] file not found: ${BACKUP_FILE}"
                    exit 1
                fi

                echo "[restore] restoring ${BACKUP_FILE} -> %s"
                gunzip -c "${BACKUP_FILE}" | mysql -h "${MYSQL_HOST:-127.0.0.1}" \\
                                                 -u "${MYSQL_USER:-root}" \\
                                                 -p"${MYSQL_PASS:-root123456}" \\
                                                 "${DB_NAME:-%s}"

                echo "[restore] done"
                """.formatted(name, dbName, dbName);
    }

    // ========================================================
    // CI/CD
    // ========================================================

    private String generateGithubActions(String name) {
        return """
                # GitHub Actions CI (V2.8.4)
                name: %s CI

                on:
                  push:
                    branches: [main, develop]
                  pull_request:
                    branches: [main]

                jobs:
                  test:
                    runs-on: ubuntu-latest
                    steps:
                      - uses: actions/checkout@v4
                      - name: Set up JDK 17
                        uses: actions/setup-java@v4
                        with:
                          java-version: '17'
                          distribution: 'temurin'
                      - name: Cache Maven
                        uses: actions/cache@v3
                        with:
                          path: ~/.m2
                          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
                      - name: Run tests
                        run: mvn clean verify -B
                      - name: Build JAR
                        run: mvn package -B -DskipTests

                  build:
                    needs: test
                    runs-on: ubuntu-latest
                    if: github.ref == 'refs/heads/main'
                    steps:
                      - uses: actions/checkout@v4
                      - name: Build Docker image
                        run: docker build -t %s:${{ github.sha }} .
                      - name: Push to Registry
                        if: success()
                        run: |
                          echo "${{ secrets.DOCKER_PASSWORD }}" | docker login -u "${{ secrets.DOCKER_USERNAME }}" --password-stdin
                          docker push %s:${{ github.sha }}
                """.formatted(name, name, name);
    }

    private String generateReleaseWorkflow(String name) {
        return """
                # 自动发布 (V2.8.4)
                name: Release

                on:
                  push:
                    tags: ['v*']

                jobs:
                  release:
                    runs-on: ubuntu-latest
                    steps:
                      - uses: actions/checkout@v4
                      - name: Create Release
                        uses: softprops/action-gh-release@v1
                        with:
                          files: |
                            target/*.jar
                            target/*.zip
                          generate_release_notes: true
                """;
    }

    private String generateGitlabCi(String name) {
        return """
                # GitLab CI/CD (V2.8.4)
                stages:
                  - test
                  - build
                  - deploy

                variables:
                  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"

                cache:
                  paths:
                    - .m2/repository

                test:
                  stage: test
                  image: maven:3.9-eclipse-temurin-17
                  script:
                    - mvn clean verify -B

                build:
                  stage: build
                  image: maven:3.9-eclipse-temurin-17
                  script:
                    - mvn package -B -DskipTests
                  artifacts:
                    paths:
                      - target/*.jar
                """;
    }

    private String generateJenkinsfile(String name) {
        return """
                // Jenkinsfile (V2.8.4)
                pipeline {
                    agent any
                    tools {
                        maven 'Maven-3.9'
                        jdk 'JDK-17'
                    }
                    stages {
                        stage('Test') {
                            steps {
                                sh 'mvn clean verify -B'
                            }
                        }
                        stage('Build') {
                            steps {
                                sh 'mvn package -B -DskipTests'
                            }
                        }
                        stage('Deploy') {
                            when { branch 'main' }
                            steps {
                                sh './scripts/deploy.sh'
                            }
                        }
                    }
                    post {
                        always { junit '**/target/surefire-reports/*.xml' }
                    }
                }
                """;
    }

    // ========================================================
    // 运维脚本 (start/stop/status/deploy/rollback/logs/migrate)
    // ========================================================

    private String generateStartScript(String name, String version) {
        return """
                #!/usr/bin/env bash
                # %s 启动脚本 (V2.8.4)
                set -euo pipefail
                APP_NAME=%s
                APP_VERSION=%s
                APP_HOME=$(cd "$(dirname "$0")/.." && pwd)
                JAR="${APP_HOME}/target/${APP_NAME}-${APP_VERSION}.jar"
                LOG_FILE="${APP_HOME}/logs/${APP_NAME}.log"
                PID_FILE="${APP_HOME}/logs/${APP_NAME}.pid"
                PORT=${SERVER_PORT:-8080}

                mkdir -p "${APP_HOME}/logs"

                if [ -f "${PID_FILE}" ] && kill -0 $(cat "${PID_FILE}") 2>/dev/null; then
                    echo "[start] already running, pid=$(cat ${PID_FILE})"
                    exit 1
                fi

                echo "[start] launching ${APP_NAME} ${APP_VERSION} on port ${PORT}"
                nohup java -Xms512m -Xmx1024m \\
                    -XX:+UseG1GC -XX:MaxRAMPercentage=70.0 \\
                    -Dserver.port=${PORT} \\
                    -Dfile.encoding=UTF-8 \\
                    -Dspring.profiles.active=prod \\
                    -jar "${JAR}" \\
                    > "${LOG_FILE}" 2>&1 &

                PID=$!
                echo ${PID} > "${PID_FILE}"
                echo "[start] started, pid=${PID}, log=${LOG_FILE}"

                # 等待就绪
                for i in {1..30}; do
                    if curl -fsS "http://localhost:${PORT}/actuator/health" >/dev/null 2>&1; then
                        echo "[start] ready in ${i}s"
                        exit 0
                    fi
                    sleep 1
                done
                echo "[start] timeout waiting for health"
                exit 1
                """.formatted(name, name, version);
    }

    private String generateStopScript(String name) {
        return """
                #!/usr/bin/env bash
                # %s 停止脚本
                set -euo pipefail
                APP_NAME=%s
                PID_FILE="$(cd "$(dirname "$0")/.." && pwd)/logs/${APP_NAME}.pid"

                if [ ! -f "${PID_FILE}" ]; then
                    echo "[stop] not running"
                    exit 0
                fi

                PID=$(cat "${PID_FILE}")
                if kill -0 ${PID} 2>/dev/null; then
                    echo "[stop] sending SIGTERM to ${PID}"
                    kill ${PID}
                    for i in {1..30}; do
                        if ! kill -0 ${PID} 2>/dev/null; then
                            echo "[stop] stopped in ${i}s"
                            rm -f "${PID_FILE}"
                            exit 0
                        fi
                        sleep 1
                    done
                    echo "[stop] timeout, force killing"
                    kill -9 ${PID}
                fi
                rm -f "${PID_FILE}"
                """.formatted(name, name);
    }

    private String generateStatusScript(String name) {
        String tmpl = """
                #!/usr/bin/env bash
                # @@NAME@@ 状态查询
                APP_NAME=@@NAME@@
                PID_FILE="$(cd "$(dirname "$0")/.." && pwd)/logs/${APP_NAME}.pid"
                PORT=${SERVER_PORT:-8080}

                echo "━━━ @@NAME@@ 状态 ━━━"
                if [ -f "${PID_FILE}" ] && kill -0 $(cat "${PID_FILE}") 2>/dev/null; then
                    PID=$(cat "${PID_FILE}")
                    echo "  PID:     ${PID} (运行中)"
                    echo "  CPU:     $(ps -p ${PID} -o %cpu= | tr -d ' ')"
                    echo "  MEM:     $(ps -p ${PID} -o rss= | awk '{printf "%.1fMB", $1/1024}')"
                else
                    echo "  状态:    未运行"
                fi

                echo "  健康检查:"
                if curl -fsS "http://localhost:${PORT}/actuator/health" 2>/dev/null; then
                    echo
                else
                    echo "  无法访问"
                fi
                """;
        return tmpl.replace("@@NAME@@", name);
    }

    private String generateDeployScript(String name, String version) {
        return """
                #!/usr/bin/env bash
                # %s 部署脚本 (V2.8.4)
                # 用法: ./deploy.sh [version]
                set -euo pipefail
                VERSION=${1:-%s}
                APP_NAME=%s
                APP_HOME=$(cd "$(dirname "$0")/.." && pwd)

                echo "[deploy] deploying ${APP_NAME} ${VERSION}"

                # 1. 拉新代码 (假设在 git 仓库里)
                # git pull origin main

                # 2. 编译
                cd "${APP_HOME}"
                mvn clean package -B -DskipTests

                # 3. 停止旧服务
                "${APP_HOME}/scripts/stop.sh" || true

                # 4. 备份旧版本
                if [ -f "${APP_HOME}/target/${APP_NAME}-${VERSION}.jar" ]; then
                    cp "${APP_HOME}/target/${APP_NAME}-${VERSION}.jar" \\
                       "${APP_HOME}/target/${APP_NAME}-${VERSION}.jar.bak.$(date +%%s)"
                fi

                # 5. 启动新版本
                "${APP_HOME}/scripts/start.sh"

                # 6. 健康验证
                sleep 5
                if "${APP_HOME}/scripts/status.sh" | grep -q "运行中"; then
                    echo "[deploy] ✓ success"
                else
                    echo "[deploy] ✗ failed, rolling back"
                    "${APP_HOME}/scripts/rollback.sh"
                    exit 1
                fi
                """.formatted(name, version, name);
    }

    private String generateRollbackScript(String name) {
        return """
                #!/usr/bin/env bash
                # %s 回滚脚本
                set -euo pipefail
                APP_NAME=%s
                APP_HOME=$(cd "$(dirname "$0")/.." && pwd)

                # 找最近的备份
                BACKUP=$(ls -t "${APP_HOME}/target/${APP_NAME}-"*.jar.bak.* 2>/dev/null | head -1 || true)

                if [ -z "${BACKUP}" ]; then
                    echo "[rollback] no backup found"
                    exit 1
                fi

                echo "[rollback] rolling back to ${BACKUP}"
                "${APP_HOME}/scripts/stop.sh" || true
                cp "${BACKUP}" "${APP_HOME}/target/${APP_NAME}-current.jar"
                "${APP_HOME}/scripts/start.sh"
                echo "[rollback] done"
                """.formatted(name, name);
    }

    private String generateLogsScript(String name) {
        return """
                #!/usr/bin/env bash
                # %s 日志查看
                APP_NAME=%s
                LOG_FILE="$(cd "$(dirname "$0")/.." && pwd)/logs/${APP_NAME}.log"

                if [ ! -f "${LOG_FILE}" ]; then
                    echo "[logs] no log file"
                    exit 1
                fi

                LINES=${1:-100}
                case "${2:-tail}" in
                    tail)  tail -n ${LINES} -f "${LOG_FILE}" ;;
                    grep)  tail -n 1000 "${LOG_FILE}" | grep -i "${3}" ;;
                    error) tail -n 1000 "${LOG_FILE}" | grep -E "ERROR|Exception" ;;
                    *)     tail -n ${LINES} "${LOG_FILE}" ;;
                esac
                """.formatted(name, name);
    }

    private String generateMigrateScript(String name, String dbName) {
        return """
                #!/usr/bin/env bash
                # %s 数据库迁移脚本
                set -euo pipefail
                DB_NAME=${DB_NAME:-%s}
                MIGRATION_DIR="$(cd "$(dirname "$0")/.." && pwd)/sql/migration"

                echo "[migrate] running migrations on ${DB_NAME}"

                # 1. 应用启动时 Flyway 会自动跑
                # 2. 手动跑
                for f in ${MIGRATION_DIR}/V*__*.sql; do
                    echo "[migrate] applying $(basename ${f})"
                    mysql -h "${MYSQL_HOST:-127.0.0.1}" \\
                          -u "${MYSQL_USER:-root}" \\
                          -p"${MYSQL_PASS:-root123456}" \\
                          "${DB_NAME}" < "${f}"
                done

                echo "[migrate] done"
                """.formatted(name, dbName);
    }

    // ========================================================
    // 文档
    // ========================================================

    private String generateReadme(String name, String version, String pkg, CodeGenResponse resp) {
        return """
                # %s

                ![version](https://img.shields.io/badge/version-%s-blue)
                ![java](https://img.shields.io/badge/java-17-orange)
                ![spring](https://img.shields.io/badge/spring--boot-3.2-green)

                %s 是由 **MiniMax AI** 自动生成的企业级 Spring Boot 应用.

                ## 特性

                - ✅ Spring Boot 3.2 + JDK 17
                - ✅ MyBatis-Plus + MySQL 8
                - ✅ Redis 7 缓存
                - ✅ JWT 鉴权 + RBAC
                - ✅ Prometheus + Grafana 监控
                - ✅ Docker + K8s 部署
                - ✅ CI/CD (GitHub Actions / GitLab / Jenkins)
                - ✅ OpenAPI 3 文档

                ## 快速开始

                ```bash
                # 1. 启动 MySQL + Redis
                docker compose up -d mysql redis

                # 2. 编译
                mvn clean package -DskipTests

                # 3. 运行
                ./scripts/start.sh

                # 4. 访问
                open http://localhost:8080/swagger-ui.html
                ```

                ## 部署

                ```bash
                # 完整部署 (应用 + MySQL + Redis + Nginx + 监控)
                docker compose up -d

                # K8s
                kubectl apply -f k8s/
                ```

                ## 文档

                - [API 文档](API.md)
                - [部署指南](DEPLOYMENT.md)
                - [更新日志](CHANGELOG.md)

                ## License

                MIT
                """.formatted(name, version, name);
    }

    private String generateApiDoc(String name, CodeGenResponse resp) {
        return """
                # %s API 文档 (V2.8.4)

                ## 认证

                ```
                POST /api/auth/login
                Content-Type: application/json

                {
                  "username": "admin",
                  "password": "admin123"
                }

                → { "token": "eyJ...", "userId": 1, "role": "admin" }
                ```

                ## 用户管理

                | Method | Path | 描述 | 权限 |
                |--------|------|------|------|
                GET | /api/users | 用户列表 | admin |
                POST | /api/users | 创建用户 | admin |
                PUT | /api/users/{id} | 更新用户 | admin |
                DELETE | /api/users/{id} | 删除用户 | admin |

                ## 业务接口

                | Method | Path | 描述 | 权限 |
                |--------|------|------|------|
                GET | /api/records | 记录列表 | user |
                POST | /api/records | 创建记录 | user |
                GET | /api/records/{id} | 详情 | user |
                PUT | /api/records/{id} | 更新 | user |
                DELETE | /api/records/{id} | 删除 | admin |

                ## 监控

                | Path | 描述 |
                |------|------|
                /actuator/health | 健康检查 |
                /actuator/info | 应用信息 |
                /actuator/prometheus | Prometheus 指标 |
                /actuator/metrics | Micrometer 指标 |
                """.formatted(name);
    }

    private String generateDeploymentDoc(String name, String version) {
        return """
                # %s 部署指南 (V2.8.4)

                ## 一、Docker Compose 部署 (推荐)

                ```bash
                # 启动完整服务栈
                docker compose up -d

                # 查看状态
                docker compose ps

                # 查看日志
                docker compose logs -f %s
                ```

                ## 二、K8s 部署

                ```bash
                # 应用所有 manifest
                kubectl apply -f k8s/

                # 查看状态
                kubectl get pods -l app=%s

                # 端口转发测试
                kubectl port-forward svc/%s 8080:80
                ```

                ## 三、传统部署 (CentOS 9)

                ```bash
                # 1. 安装 JDK 17
                dnf install -y java-17-openjdk

                # 2. 编译
                mvn clean package -DskipTests

                # 3. 安装服务
                sudo cp scripts/start.sh /usr/local/bin/%s-start
                sudo cp scripts/stop.sh /usr/local/bin/%s-stop
                sudo chmod +x /usr/local/bin/%s-*

                # 4. 配置 systemd
                sudo tee /etc/systemd/system/%s.service <<EOF
                [Unit]
                Description=%s Service
                After=network.target

                [Service]
                Type=forking
                User=app
                ExecStart=/usr/local/bin/%s-start
                ExecStop=/usr/local/bin/%s-stop
                Restart=always
                RestartSec=10

                [Install]
                WantedBy=multi-user.target
                EOF

                sudo systemctl daemon-reload
                sudo systemctl enable --now %s
                ```

                ## 四、监控

                ```bash
                # 启动 Prometheus
                docker compose up -d prometheus

                # 启动 Grafana
                docker compose up -d grafana
                # 访问 http://localhost:3000 (admin/admin)
                ```

                ## 五、备份

                ```bash
                # 每天 02:00 备份
                echo "0 2 * * * /opt/%s/scripts/backup.sh daily" | crontab -
                ```
                """.formatted(name, name, name, name, name, name, name, name, name, name, name, name, name);
    }

    private String generateChangelog(String version) {
        return """
                # 更新日志 (V2.8.4)

                ## [%s] - 2026-07-12

                ### 新增
                - ✨ 完整 Spring Boot 项目结构
                - ✨ Docker + docker-compose 多服务编排
                - ✨ K8s Deployment/Service/Ingress/ConfigMap/Secret
                - ✨ Prometheus + Grafana 监控
                - ✨ GitHub Actions / GitLab CI / Jenkinsfile
                - ✨ 数据库备份/恢复/迁移脚本
                - ✨ 部署/回滚/状态/日志脚本
                - ✨ SQL 脚本 (DDL + 种子 + Flyway 迁移)
                - ✨ API/部署/CHANGELOG 文档

                ### 安全
                - 🔒 JWT 鉴权
                - 🔒 RBAC 权限
                - 🔒 密码 BCrypt 加密
                """.formatted(version);
    }

    private String generateContributing() {
        return """
                # 贡献指南

                1. Fork 本仓库
                2. 创建特性分支 (`git checkout -b feature/xxx`)
                3. 提交改动 (`git commit -am 'Add xxx'`)
                4. 推送到分支 (`git push origin feature/xxx`)
                5. 创建 Pull Request

                ## 提交规范

                - feat: 新功能
                - fix: 修复
                - docs: 文档
                - style: 格式
                - refactor: 重构
                - test: 测试
                - chore: 构建/工具
                """;
    }

    private String generateGitignore() {
        return """
                # Maven
                target/
                .m2/

                # IDE
                .idea/
                *.iml
                .vscode/
                .project
                .classpath
                .settings/

                # Logs
                logs/
                *.log

                # OS
                .DS_Store
                Thumbs.db

                # Env
                .env
                .env.local

                # Backup
                *.jar.bak.*
                """;
    }

    private String generateEditorConfig() {
        return """
                root = true

                [*]
                charset = utf-8
                end_of_line = lf
                indent_style = space
                indent_size = 4
                insert_final_newline = true
                trim_trailing_whitespace = true
                max_line_length = 120

                [*.{yml,yaml,json,md}]
                indent_size = 2
                """;
    }

    private String generateMakefile(String name, String version) {
        return """
                # %s Makefile (V2.8.4)
                APP_NAME := %s
                VERSION := %s
                JAR := target/$(APP_NAME)-$(VERSION).jar

                .PHONY: help build test package run start stop status deploy rollback clean

                help:
                \t@echo "make build     - 编译"
                \t@echo "make test      - 测试"
                \t@echo "make package   - 打包"
                \t@echo "make run       - 本地运行"
                \t@echo "make start     - 启动服务"
                \t@echo "make stop      - 停止服务"
                \t@echo "make status    - 服务状态"
                \t@echo "make deploy    - 部署"
                \t@echo "make rollback  - 回滚"
                \t@echo "make clean     - 清理"

                build:
                \tmvn clean compile -B

                test:
                \tmvn test -B

                package:
                \tmvn package -B -DskipTests

                run:
                \tmvn spring-boot:run

                start:
                \t./scripts/start.sh

                stop:
                \t./scripts/stop.sh

                status:
                \t./scripts/status.sh

                deploy:
                \t./scripts/deploy.sh

                rollback:
                \t./scripts/rollback.sh

                clean:
                \tmvn clean
                \trm -rf logs/*.log
                """.formatted(name, name, version);
    }

    private String generateMavenPom(String name, String version, String pkg) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>com.minimax.app</groupId>
                    <artifactId>%s</artifactId>
                    <version>%s</version>
                    <name>%s</name>
                    <description>MiniMax AI generated app</description>

                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>3.2.0</version>
                    </parent>

                    <properties>
                        <java.version>17</java.version>
                        <maven.compiler.source>17</maven.compiler.source>
                        <maven.compiler.target>17</maven.compiler.target>
                    </properties>

                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-data-jpa</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-security</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-validation</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-actuator</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>com.mysql</groupId>
                            <artifactId>mysql-connector-j</artifactId>
                            <scope>runtime</scope>
                        </dependency>
                        <dependency>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <optional>true</optional>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-test</artifactId>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>

                    <build>
                        <finalName>%s-${project.version}</finalName>
                        <plugins>
                            <plugin>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-maven-plugin</artifactId>
                                <configuration>
                                    <excludes>
                                        <exclude>
                                            <groupId>org.projectlombok</groupId>
                                            <artifactId>lombok</artifactId>
                                        </exclude>
                                    </excludes>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """.formatted(name, version, name, name);
    }

    private String generateAppTest() {
        return """
                package com.minimax.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Spring Boot 上下文加载测试
 */
@SpringBootTest
class AppApplicationTests {

    @Test
    void contextLoads() {
    }
}
                """;
    }

    private String generateTestConfig(String dbName) {
        return """
                # 测试配置 (V2.8.4)
                spring:
                  datasource:
                    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
                    driver-class-name: org.h2.Driver
                    username: sa
                    password:
                  jpa:
                    hibernate:
                      ddl-auto: create-drop
                """.replace("%s", dbName);
    }
}
