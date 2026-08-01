# 双录一体化平台 - 每日定时任务部署指南

> 每天 22:00 自动跑健康检查 + 测试 + GitHub 同步 + 生成日报

## 部署方案

### 方案 A: 常驻进程(沙箱/容器/无 crontab 环境)

```bash
# 1. 上传脚本
scp cron-daemon.sh daily-task.sh user@server:/opt/dual-record/

# 2. 启动
chmod +x /opt/dual-record/*.sh
/opt/dual-record/cron-daemon.sh start

# 3. 检查
/opt/dual-record/cron-daemon.sh status

# 4. 停止
/opt/dual-record/cron-daemon.sh stop
```

**特性**:
- 任意目录可调用(用绝对路径)
- 日志: `.cron-daemon.log`
- PID 文件: `.cron-daemon.pid`
- 支持环境变量 `DAILY_INTERVAL` 自定义间隔(秒)

### 方案 B: Linux Crontab(传统服务器)

```bash
# 1. 编辑 crontab
crontab -e

# 2. 添加(每天 22:00)
0 22 * * * /opt/dual-record/daily-task.sh >> /var/log/dual-record-daily.log 2>&1
```

### 方案 C: Systemd Timer(现代 Linux)

```bash
# /etc/systemd/system/dual-record-daily.service
[Unit]
Description=Dual Record Daily Health Check
After=network.target

[Service]
Type=oneshot
User=app
ExecStart=/opt/dual-record/daily-task.sh
StandardOutput=append:/var/log/dual-record-daily.log
StandardError=append:/var/log/dual-record-daily.log

# /etc/systemd/system/dual-record-daily.timer
[Unit]
Description=Run dual-record-daily daily at 22:00

[Timer]
OnCalendar=*-*-* 22:00:00
Persistent=true

[Install]
WantedBy=timers.target
```

```bash
systemctl daemon-reload
systemctl enable --now dual-record-daily.timer
systemctl list-timers
```

### 方案 D: Kubernetes CronJob

```yaml
# daily-task-cronjob.yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: dual-record-daily
spec:
  schedule: "0 22 * * *"
  timeZone: "Asia/Shanghai"
  concurrencyPolicy: Forbid
  successfulJobsHistoryLimit: 3
  failedJobsHistoryLimit: 3
  jobTemplate:
    spec:
      template:
        spec:
          restartPolicy: OnFailure
          containers:
          - name: daily
            image: alpine:3.18
            command:
            - /bin/sh
            - -c
            - |
              apk add --no-cache git maven nodejs npm mysql-client python3
              cd /workspace
              ./double-record-code/daily-task.sh
            env:
            - name: GITHUB_TOKEN
              valueFrom:
                secretKeyRef:
                  name: github-credentials
                  key: token
            volumeMounts:
            - name: workspace
              mountPath: /workspace
          volumes:
          - name: workspace
            persistentVolumeClaim:
              claimName: workspace-pvc
```

```bash
kubectl apply -f daily-task-cronjob.yaml
kubectl get cronjob dual-record-daily
```

## 任务执行流程

```
触发(22:00)
   ↓
[1] 环境检查
   ├─ java, mvn, node, npm, git, docker, python3, mysql
   ↓
[2] Git 状态
   ├─ working tree 状态
   ├─ 最近 5 次 commit
   └─ remote 列表
   ↓
[3] Java 链码测试
   └─ mvn test (链码 53 测试)
   ↓
[4] 后端测试
   └─ mvn test (后端 6 测试)
   ↓
[5] TypeScript SDK 测试
   └─ jest (SDK 62 测试)
   ↓
[6] SQL 验证(若 MySQL 可用)
   ├─ 表数量
   ├─ 今日订单数
   ├─ 今日事件数
   └─ 状态分布
   ↓
[7] 代码体积统计
   └─ 5 个模块 文件/行数
   ↓
[8] GitHub 同步
   ├─ git add
   ├─ git commit
   ├─ git push(token 临时嵌入,完成后还原)
   ↓
[9] 待办检查
   └─ 17 个发现 + P0-P3 跟踪
   ↓
[10] 报告生成
   └─ .daily-reports/YYYY-MM-DD.md
```

## 报告样例

报告位置:`double-record-code/.daily-reports/YYYY-MM-DD.md`

```markdown
# 双录一体化平台 - 每日健康检查报告

> 日期: 2026-08-01 14:17:59

## 1. 环境检查
| 工具 | 版本 | 状态 |
| ... |

## 2. Git 状态
...

## 3-7. 测试结果
...

## 8. GitHub 同步
✅ 推送成功

## 9. 待办跟踪
| 优先级 | 任务 | 状态 |
| ...
```

## 监控告警

日报可对接 Prometheus + AlertManager:

```yaml
# prometheus rule
- alert: DailyTaskFailed
  expr: time() - daily_task_last_success_timestamp > 90000
  for: 1h
  labels:
    severity: critical
  annotations:
    summary: "每日任务失败超过 25 小时"
```

## 清理策略

- 报告保留 30 天(`find ... -mtime +30 -delete`)
- 日志保留 30 天
- 任务失败不删除,人工排查

## 联系方式

- 维护:Mavis
- 邮件:Mavis@bank.com
- 7×24 运维热线:400-xxx-xxxx

---

**版本**: 1.0
**最后更新**: 2026-08-01
