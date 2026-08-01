# 双录一体化平台 - Helm Chart

> 🚀 K8s 一键部署 · 生产可用 · 含 HPA / PDB / NetworkPolicy / ServiceMonitor

## 📋 组件清单

| 资源 | 作用 | 默认值 |
|------|------|--------|
| Deployment | 应用部署 | 3 副本 |
| Service | ClusterIP 服务 | 8080 |
| Ingress | HTTPS 入口 | nginx + cert-manager |
| HPA | 自动扩缩 | 3-10 副本 (CPU 70%) |
| PDB | Pod 中断预算 | minAvailable=2 |
| Secret | 凭证存储 | jwt/db/redis/llm/sm4 |
| ConfigMap | 应用配置 | application.yml |
| PVC | 持久化存储 | 100Gi |
| ServiceAccount | K8s 身份 | default |
| NetworkPolicy | 网络隔离 | 入/出站白名单 |
| ServiceMonitor | Prometheus 抓取 | 30s 间隔 |

## 🚀 快速开始

### 1. 准备命名空间

```bash
kubectl create namespace dual-record-prod
kubectl label namespace dual-record-prod name=dual-record-prod
```

### 2. 准备 Harbor 凭证

```bash
kubectl create secret docker-registry harbor-credential \
  --docker-server=harbor.bank.com \
  --docker-username=robot\$dual-record \
  --docker-password='YOUR_HARBOR_PASSWORD' \
  -n dual-record-prod
```

### 3. 准备敏感凭证

```bash
# 方式 A:通过 values.yaml 注入(不推荐)
# 方式 B:使用 sealed-secrets / external-secrets
# 方式 C:用 Helm 命令行覆盖

helm install dual-record ./dual-record \
  --namespace dual-record-prod \
  --set secrets.jwtSecret=$(openssl rand -base64 48 | tr -d '=+/' | cut -c1-48) \
  --set secrets.dbPassword='YOUR_DB_PASSWORD' \
  --set secrets.redisPassword='YOUR_REDIS_PASSWORD' \
  --set secrets.qwenApiKey='YOUR_QWEN_KEY' \
  --set secrets.deepseekApiKey='YOUR_DEEPSEEK_KEY' \
  --set secrets.sm4Key=$(openssl rand -hex 16)
```

### 4. 验证

```bash
# 等待就绪
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=dual-record \
  -n dual-record-prod --timeout=300s

# 健康检查
kubectl exec -it -n dual-record-prod $(kubectl get pod -n dual-record-prod -l app.kubernetes.io/name=dual-record -o jsonpath='{.items[0].metadata.name}') \
  -- curl localhost:8080/actuator/health
```

## ⚙️ 自定义配置

### JVM 调优

```yaml
jvm:
  heapMax: 4g
  heapMin: 2g
  gc: ZGC   # G1GC / ZGC / ParallelGC
  gcOpts: "-XX:+UseZGC -XX:+ZGenerational"
```

### 弹性策略

```yaml
autoscaling:
  minReplicas: 5
  maxReplicas: 20
  targetCPUUtilizationPercentage: 60
  targetMemoryUtilizationPercentage: 75
```

### 持久化

```yaml
persistence:
  enabled: true
  size: 500Gi
  storageClass: csi-alicloud    # 阿里云
  # storageClass: csi-tencentcloud  # 腾讯云
  # storageClass: gp3              # AWS
```

### Ingress 注解

```yaml
ingress:
  annotations:
    nginx.ingress.kubernetes.io/configuration-snippet: |
      more_set_headers "X-Frame-Options: DENY";
      more_set_headers "X-Content-Type-Options: nosniff";
```

## 🛡️ 安全特性

- ✅ `runAsNonRoot: true`(非 root 用户)
- ✅ `readOnlyRootFilesystem: true`(只读根文件系统)
- ✅ 禁用特权升级(`allowPrivilegeEscalation: false`)
- ✅ 限制 capabilities 到 `["ALL"]` drop
- ✅ NetworkPolicy 限制入/出站
- ✅ Secret 通过 K8s Secret 加密存储
- ✅ cert-manager 自动签发 TLS 证书

## 📊 可观测性

### Prometheus 指标端点

```
https://dual-record.bank.com/actuator/prometheus
```

### Grafana Dashboard 导入

```bash
# 预置 Dashboard ID(占位)
# 实际 JSON 在 deploy/grafana/dual-record-dashboard.json
```

## 🆙 升级

```bash
# 1. 更新镜像
helm upgrade dual-record ./dual-record \
  --reuse-values \
  --set image.tag=1.2.1 \
  -n dual-record-prod

# 2. 滚动重启(零停机)
kubectl rollout restart deployment/dual-record -n dual-record-prod

# 3. 查看状态
kubectl rollout status deployment/dual-record -n dual-record-prod
```

## 🆘 故障排查

### Pod 一直 Pending

```bash
kubectl describe pod -n dual-record-prod <pod-name>
# 常见原因:资源不足 / PVC 未绑定 / 节点选择器不匹配
```

### 健康检查失败

```bash
kubectl logs -n dual-record-prod <pod-name> | tail -100
# 检查:DB 连接 / Redis 连接 / Kafka 集群 / Fabric 网络
```

### 限流误触发

```bash
# 调高阈值
helm upgrade dual-record ./dual-record --reuse-values \
  --set rate-limit.write=100 --set rate-limit.read=300 \
  -n dual-record-prod
```

## 📚 参考

- [Kubernetes 官方文档](https://kubernetes.io/docs/home/)
- [Helm 官方文档](https://helm.sh/docs/)
- [Spring Boot on K8s](https://spring.io/guides/gs/spring-boot-kubernetes/)

## 🏷️ 版本

| Chart 版本 | 应用版本 | 关键变化 |
|-----------|---------|----------|
| 1.2.0 | 1.2.0 | 安全修复 + Prometheus 指标 |
| 1.1.0 | 1.1.0 | 加入 HPA + PDB |
| 1.0.0 | 1.0.0 | 初始版本 |
