# 双录一体化平台 - 灾备手册

> 🛡️ RPO ≤ 5min · RTO ≤ 30min · 银行 V 类生产环境标准

## 📊 容灾等级

| 维度 | 目标 | 实际方案 |
|------|------|----------|
| **RPO**(数据恢复点) | ≤ 5min | MySQL 半同步 + binlog 实时归档 |
| **RTO**(恢复时间) | ≤ 30min | 异地温备 + 自动故障切换 |
| **可用性** | ≥ 99.95% | 同城双活 + 异地灾备 |
| **数据零丢失** | 强一致 | 链上数据不可篡改 + 链下加密备份 |

## 🏗️ 部署架构

```
┌──────────────────────────────────────────────────────────┐
│                    主数据中心(北京)                       │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐    │
│  │  K8s 集群│  │  MySQL  │  │  Redis  │  │  Fabric │    │
│  │  3 节点 │  │  主库   │  │  主从   │  │  4 组织  │    │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘    │
│       │            │            │            │         │
│       │     实时同步(Galera)    │      链上广播  │         │
└───────┼────────────┼────────────┼────────────┼─────────┘
        │            │            │            │
        ▼            ▼            ▼            ▼
┌──────────────────────────────────────────────────────────┐
│                    同城灾备(上海)                         │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐    │
│  │  K8s 集群│  │  MySQL  │  │  Redis  │  │  Fabric │    │
│  │  温备  │  │  备库   │  │  备库   │  │  4 组织  │    │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘    │
└──────────────────────────────────────────────────────────┘
        │
        │ 每日全量 + 实时 binlog
        ▼
┌──────────────────────────────────────────────────────────┐
│                    异地灾备(深圳)                         │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐                  │
│  │  备份   │  │  备份   │  │  备份   │                  │
│  │  MySQL  │  │  Redis  │  │  对象存储 │                  │
│  └─────────┘  └─────────┘  └─────────┘                  │
└──────────────────────────────────────────────────────────┘
```

## 🔄 备份策略

### MySQL 数据库

| 类型 | 周期 | 保留 | 工具 |
|------|------|------|------|
| 全量备份 | 每日 02:00 | 30 天 | `mysqldump` / `xtrabackup` |
| 增量备份 | 实时 (binlog) | 7 天 | `binlog` 归档 |
| 半同步复制 | 实时 | - | MySQL Semi-Sync |
| 异地归档 | 每日 04:00 | 永久 | OSS / S3 |

#### 自动备份脚本

```bash
#!/bin/bash
# /opt/dual-record/scripts/mysql-backup.sh
set -euo pipefail

BACKUP_DIR=/data/backup/mysql
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

# 1. 全量备份
xtrabackup --backup \
  --target-dir=$BACKUP_DIR/full_$DATE \
  --user=dual_record \
  --password=$DB_PASSWORD \
  --parallel=4

# 2. 压缩
tar czf $BACKUP_DIR/full_$DATE.tar.gz -C $BACKUP_DIR full_$DATE

# 3. 上传到 OSS
ossutil cp $BACKUP_DIR/full_$DATE.tar.gz \
  oss://dual-record-backup/mysql/full/

# 4. 清理本地旧备份
find $BACKUP_DIR -type d -name "full_*" -mtime +$RETENTION_DAYS -exec rm -rf {} \;

# 5. 上报监控系统
curl -X POST http://dual-record-monitor.bank.com/api/backup-status \
  -H "Content-Type: application/json" \
  -d "{\"type\":\"mysql\",\"date\":\"$DATE\",\"status\":\"success\"}"

echo "✅ MySQL 备份完成: $DATE"
```

#### Cron 配置

```cron
# 每天 02:00 全量备份
0 2 * * * /opt/dual-record/scripts/mysql-backup.sh >> /var/log/dual-record/backup.log 2>&1

# 每 5 分钟 binlog 检查
*/5 * * * * /opt/dual-record/scripts/binlog-monitor.sh >> /var/log/dual-record/backup.log 2>&1
```

### Redis 数据

| 类型 | 周期 | 保留 |
|------|------|------|
| RDB 快照 | 每 6 小时 | 7 天 |
| AOF 重写 | 每小时 | - |
| 异地复制 | 实时 | - |

#### Redis 配置

```conf
# /etc/redis/redis.conf 关键参数
appendonly yes
appendfsync everysec
save 3600 1
save 300 100
save 60 10000
```

### Fabric 链码数据

链上数据**天然持久化**,但仍需备份:

```bash
# 1. 备份账本(blockchain ledger)
cp -r /var/hyperledger/production/ledgersData /data/backup/fabric/ledger_$(date +%Y%m%d)

# 2. 备份链码
cp -r /var/hyperledger/production/chaincodes /data/backup/fabric/chaincode_$(date +%Y%m%d)

# 3. 备份 MSP 身份
cp -r /var/hyperledger/production/msp /data/backup/fabric/msp_$(date +%Y%m%d)
```

### 文件存储(录像/合同)

```bash
# /opt/dual-record/scripts/storage-backup.sh
# OSS 跨区域复制自动完成,只需监控
ossutil ls oss://dual-record-storage/ --recursive | \
  awk '$1=="OSS" && $4=="OK"' > /var/log/dual-record/storage-sync.log
```

## ⚠️ 故障场景与响应

### 场景 1:MySQL 主库宕机

**检测时间**: ≤ 10s(K8s 健康检查 + Prometheus)

**自动切换流程**:
1. K8s Service 标记主库 Pod unhealthy
2. MHA/Orchestrator 提升备库为主
3. VIP 漂移到新主库(5-10s)
4. 应用无感知(连接串重连)

**手动验证**:
```bash
# 1. 确认主备状态
mysql -h dual-record-mysql -e "SHOW SLAVE STATUS\G"

# 2. 检查应用连接
kubectl exec -n dual-record-prod $(kubectl get pod -n dual-record-prod -l app.kubernetes.io/name=dual-record -o jsonpath='{.items[0].metadata.name}') -- \
  curl localhost:8080/actuator/health/db
```

**RTO**: ≤ 5 min  
**RPO**: 0(半同步)

### 场景 2:整个 K8s 节点故障

**自动恢复**:
1. K8s 检测到 Node NotReady(40s)
2. Pod 自动迁移到健康节点(60-120s)
3. PDB 保证至少 2 副本可用
4. HPA 触发扩容补偿

**手动干预**:
```bash
# 1. 标记节点不可调度
kubectl cordon <node-name>

# 2. 驱逐 Pod(PDB 保证不超限)
kubectl drain <node-name> --ignore-daemonsets --delete-emptydir-data

# 3. 修复后重新加入
kubectl uncordon <node-name>
```

**RTO**: ≤ 5 min  
**RPO**: 0(无状态应用)

### 场景 3:主数据中心完全失联

**切换到同城灾备**:
1. 监控系统触发告警(银行专线延迟 > 1s)
2. DNS 切换到灾备 VIP(`dual-record.bank.com` → 灾备 IP)
3. 灾备 K8s 集群接管流量(自动扩缩到 3 副本)
4. MySQL 灾备自动提升为读写

```bash
# 灾备切换命令(需双 4 眼授权)
./dr-switchover.sh primary --to=shanghai-dr \
  --confirm-token=<一次性密码>
```

**RTO**: 15-30 min(取决于数据同步延迟)  
**RPO**: ≤ 5 min

### 场景 4:链码数据异常

链上数据不可篡改,异常处理:

```bash
# 1. 查询历史版本
peer chaincode query -C dual-record-channel \
  -n dual-record-chaincode \
  -c '{"function":"getEvidenceHistory","Args":["ORDER123"]}'

# 2. 验证现有数据
peer chaincode query -C dual-record-channel \
  -n dual-record-chaincode \
  -c '{"function":"verifyEvidence","Args":["ORDER123","HASH1","HASH2","HASH3"]}'

# 3. 如需修复(必须多组织背书)
./multi-org-endorse.sh fixEvidence --order-id=ORDER123 --reason="自然灾备"
```

### 场景 5:Redis 故障

```bash
# 1. 启用降级模式(业务降级)
kubectl exec -n dual-record-prod <pod> -- \
  curl -X POST localhost:8080/admin/degrade/redis

# 2. 应用切换到本地缓存(Caffeine)
# 3. 限流降级为本地限流(Guava)

# 4. 修复 Redis 后恢复
kubectl exec -n dual-record-prod <pod> -- \
  curl -X POST localhost:8080/admin/recover/redis
```

**RTO**: ≤ 2 min(降级模式立即生效)

## 🧪 灾备演练(季度)

### 演练计划

| 季度 | 演练类型 | 影响 | 时长 |
|------|----------|------|------|
| Q1 | MySQL 主备切换 | 只读中断 30s | 2h |
| Q2 | K8s 节点故障 | 0 中断(Pod 自愈) | 1h |
| Q3 | 同城切换 | 5min 部分中断 | 4h |
| Q4 | 异地灾备恢复 | 30min 全量中断 | 8h |

### 演练流程(Q3 同城切换)

```bash
# 1. 演练前快照
mysqldump --all-databases > /data/snapshot_pre_dr_$(date +%Y%m%d).sql

# 2. 启动应用写入监控
./write-monitor.sh start

# 3. 触发切换
./dr-switchover.sh primary --to=shanghai-dr --drill-mode

# 4. 监控关键指标
./monitor-during-dr.sh

# 5. 切换回主
./dr-switchover.sh primary --to=beijing --drill-mode

# 6. 评估报告
./dr-report-generator.sh > dr-report-q3.html
```

## 📞 应急联系

| 角色 | 联系人 | 电话 | 邮箱 |
|------|--------|------|------|
| 一线值班 | 运维工程师 | 7×24 | ops@bank.com |
| 业务负责人 | 产品经理 | 工作日 | product@bank.com |
| DBA | 数据库团队 | 7×24 | dba@bank.com |
| 架构师 | 平台架构 | 工作日 | arch@bank.com |

## 🛠️ 恢复工具集

### 数据库恢复

```bash
# 从全量备份恢复
xtrabackup --prepare --target-dir=/data/restore/full_xxx
xtrabackup --copy-back --target-dir=/data/restore/full_xxx

# 从 binlog 恢复
mysqlbinlog --start-datetime="2026-08-01 02:00:00" \
            --stop-datetime="2026-08-01 03:00:00" \
            /var/lib/mysql/binlog.* | mysql -u root -p
```

### K8s 应用恢复

```bash
# 列出历史版本
helm history dual-record -n dual-record-prod

# 回滚到指定版本
helm rollback dual-record 3 -n dual-record-prod
```

### Fabric 链码恢复

```bash
# 重新部署链码
./deploy-network-config.sh re-deploy

# 验证链码版本
peer chaincode query -C dual-record-channel \
  -n dual-record-chaincode \
  -c '{"function":"getVersion","Args":[]}'
```

## 📈 SLA 监控指标

```yaml
# Prometheus 告警规则
groups:
  - name: dual_record_sla
    rules:
      - alert: MysqlReplicationLag
        expr: mysql_slave_status_seconds_behind_master > 30
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "MySQL 复制延迟 > 30s"

      - alert: KafkaConsumerLag
        expr: kafka_consumer_lag_max > 10000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Kafka 消费积压"

      - alert: PodCrashLooping
        expr: rate(kube_pod_container_status_restarts_total[15m]) > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Pod 频繁重启"
```

---

**文档版本**: 1.0  
**最后更新**: 2026-08-01  
**下次演练**: 2026-09-15
