# MiniMax Platform - 运维手册 (V2.0)

> 一切从这里开始 🚀

## 📋 目录

1. [快速启动](#快速启动)
2. [服务管理](#服务管理)
3. [故障排查](#故障排查)
4. [性能优化](#性能优化)
5. [备份恢复](#备份恢复)
6. [监控告警](#监控告警)
7. [升级迁移](#升级迁移)

---

## 🚀 快速启动

### 第一次部署

```bash
# 1. 克隆
git clone https://github.com/liugl951127/miniLiugl.git /opt/miniLiugl
cd /opt/miniLiugl

# 2. 一键启动 (默认用宿主机 nginx, 不强制域名)
sudo ./deploy-simple/docker-deploy.sh up

# 3. 验证 (等 5-10 分钟首次编译)
curl -I http://localhost/
# 期望: HTTP/1.1 200 OK
```

### 配置域名 + HTTPS

```bash
# DNS 解析 OK 后 (5-30 分钟生效):
sudo ./deploy-simple/docker-deploy.sh frontend liugeliang.com admin@liugeliang.com

# 验证
sudo ./deploy-simple/docker-deploy.sh verify liugeliang.com
```

### 默认账号

| 账号 | 密码 | 说明 |
|------|------|------|
| `adminLiugl` | `Liugl@2026` | **超级管理员** (创建即有) |
| `admin` | `admin@123` | 普通管理员 (需手动 INSERT) |

---

## 🛠️ 服务管理

### 查看所有服务状态

```bash
# 方式 1: docker compose
cd /opt/miniLiugl
docker compose ps

# 方式 2: 一键脚本
./deploy-simple/docker-deploy.sh ps

# 方式 3: 看资源占用
docker stats --no-stream
```

### 启动 / 停止

```bash
# 启动全部
./deploy-simple/docker-deploy.sh up

# 停止全部 (数据保留)
./deploy-simple/docker-deploy.sh down

# 启动单个
docker compose up -d gateway

# 重启单个
docker compose restart auth

# 强制重新构建 (代码改动后)
./deploy-simple/docker-deploy.sh rebuild gateway
```

### 看日志

```bash
# 所有服务 (混合)
./deploy-simple/docker-deploy.sh logs

# 单个服务
./deploy-simple/docker-deploy.sh logs gateway

# 最近 100 行 + 跟踪
docker compose logs -f --tail=100 gateway
```

### 进入容器调试

```bash
# gateway 容器
docker exec -it minimax-gateway bash

# 看 JVM 实时状态
docker exec minimax-gateway jcmd 1 VM.flags
docker exec minimax-gateway jcmd 1 GC.heap_info

# 看应用配置
docker exec minimax-gateway cat /app/config/application.yml
```

---

## 🔧 故障排查

### 80 端口被占用

```bash
# 自动修复
sudo ./deploy-simple/docker-deploy.sh fix-80

# 手动
sudo systemctl stop nginx       # 宿主机 nginx
sudo lsof -i :80                # 找占用进程
docker compose restart nginx    # 重启 docker nginx
```

### 服务启动失败

```bash
# 看具体错误
docker compose logs gateway --tail=50

# 常见原因:
# 1. nacos 没起来 → docker compose up -d nacos
# 2. mysql 没起来 → docker compose logs mysql
# 3. JVM OOM → 调大 memory limit
# 4. 端口冲突 → ss -tlnp | grep :7080
```

### 登录失败

```bash
# 1. auth 服务日志
docker compose logs auth --tail=30

# 2. 测试直接访问 (绕过 nginx)
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"adminLiugl","password":"Liugl@2026"}'

# 3. 看 JWT secret 是否一致
docker exec minimax-auth cat /app/config/application.yml | grep -A 3 jwt:
docker exec minimax-gateway cat /app/config/application.yml | grep -A 3 jwt:
```

### 内存持续增长 (内存泄漏)

```bash
# 1. 看哪个容器占用最多
docker stats --no-stream --format "table {{.Name}}\t{{.MemUsage}}"

# 2. 触发 Heap Dump
docker exec minimax-gateway jcmd 1 GC.heap_dump /tmp/heap.hprof
docker cp minimax-gateway:/tmp/heap.hprof /tmp/

# 3. 用 VisualVM 打开分析 (本地)
# scp /tmp/heap.hprof local:~/Desktop/
```

### 数据库连接池耗尽

```bash
# 看 mysql 连接数
docker exec minimax-mysql mysql -uroot -proot123456 -e \
  "SHOW PROCESSLIST; SELECT COUNT(*) FROM information_schema.processlist;"

# 重启服务清空连接
docker compose restart gateway auth chat
```

---

## ⚡ 性能优化

### JVM 内存 (V2.0 已优化)

每个微服务运行内存: **~256MB** (之前 ~512MB)

关键参数 (`backend/parent-jvm-args.txt`):
```
-XX:MaxRAMPercentage=70.0     # Docker 内存感知
-XX:+UseG1GC                  # G1 垃圾回收
-XX:+UseStringDeduplication   # 字符串去重
-XX:+UseCompressedOops        # 压缩指针
```

### 编译加速

```bash
# 用阿里云镜像 + 4 线程并行
mvn clean install -s .mvn/settings.xml -DskipTests -T 4
```

### Redis 连接池

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 16      # 默认 8, 提高
          max-idle: 8
          min-idle: 2
```

### 数据库连接池

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 8    # V2.0 精简, 可按需调整
      minimum-idle: 2
```

---

## 💾 备份恢复

### 自动备份脚本

```bash
# 创建 /root/backup-minimax.sh
cat > /root/backup-minimax.sh << 'EOF'
#!/usr/bin/env bash
BACKUP_DIR=/opt/minimax/backup
DATA_DIR=/opt/minimax/data
DATE=$(date +%Y%m%d-%H%M%S)

mkdir -p $BACKUP_DIR

# 1. MySQL
docker exec minimax-mysql mysqldump -uroot -proot123456 \
  --all-databases --single-transaction --routines --triggers \
  > $BACKUP_DIR/mysql-$DATE.sql

# 2. 压缩数据目录
tar czf $BACKUP_DIR/data-$DATE.tar.gz -C $DATA_DIR .

# 3. 删除 7 天前
find $BACKUP_DIR -type f -mtime +7 -delete

echo "✓ Backup done: $BACKUP_DIR"
ls -lah $BACKUP_DIR/
EOF
chmod +x /root/backup-minimax.sh

# 加 cron 每天 3 点
echo "0 3 * * * root /root/backup-minimax.sh >> /var/log/backup.log 2>&1" | \
  sudo tee /etc/cron.d/minimax-backup
```

### 恢复

```bash
# 1. MySQL
docker exec -i minimax-mysql mysql -uroot -proot123456 < backup.sql

# 2. 数据目录
tar xzf data.tar.gz -C /opt/minimax/data/

# 3. 重启服务
docker compose restart
```

---

## 📊 监控告警

### 健康检查

```bash
# gateway
curl http://localhost:7080/actuator/health/liveness
curl http://localhost:7080/actuator/health/readiness

# Prometheus 指标
curl http://localhost:7080/actuator/prometheus
```

### 看 OpenTelemetry 链路

```bash
# OTEL Collector 端口
# 4317 (gRPC)
# 4318 (HTTP)
# 8888 (Prometheus)

# 看 traces (需要 jaeger/tempo)
# 配置 OTEL Collector 把 traces 导到 jaeger
```

### 日志位置

| 类型 | 路径 |
|------|------|
| **nginx 访问** | `/var/log/nginx/access.log` |
| **nginx 错误** | `/var/log/nginx/error.log` |
| **应用 stdout** | `docker logs <container>` |
| **JVM OOM dump** | `/var/log/minimax/oom/` |
| **Nginx 自动续期** | `/var/log/certbot-renew.log` |

---

## 🔄 升级迁移

### 升级到新版本

```bash
# 1. 备份
/root/backup-minimax.sh

# 2. 拉最新代码
cd /opt/miniLiugl
git pull

# 3. 强制重建镜像
./deploy-simple/docker-deploy.sh rebuild

# 4. 重启
./deploy-simple/docker-deploy.sh down
./deploy-simple/docker-deploy.sh up

# 5. 验证
./deploy-simple/docker-deploy.sh verify liugeliang.com
```

### 迁移到新服务器

```bash
# 老服务器: 打包数据
cd /opt/miniLiugl
git pull
/root/backup-minimax.sh
scp -r /opt/miniLiugl root@<new-server>:/opt/
scp /opt/minimax/backup/mysql-*.sql root@<new-server>:/tmp/

# 新服务器: 恢复
cd /opt/miniLiugl
./deploy-simple/docker-deploy.sh up
docker exec -i minimax-mysql mysql -uroot -proot123456 < /tmp/mysql-*.sql
```

---

## 📞 常用命令速查

| 需求 | 命令 |
|------|------|
| 一键启动 | `sudo ./deploy-simple/docker-deploy.sh up` |
| 配 HTTPS | `sudo ./deploy-simple/docker-deploy.sh frontend DOMAIN EMAIL` |
| 看状态 | `./deploy-simple/docker-deploy.sh ps` |
| 看日志 | `./deploy-simple/docker-deploy.sh logs gateway` |
| 重启服务 | `./deploy-simple/docker-deploy.sh` 然后 up |
| 修 80 端口 | `sudo ./deploy-simple/docker-deploy.sh fix-80` |
| 验证链路 | `sudo ./deploy-simple/docker-deploy.sh verify DOMAIN` |
| 强制重建 | `./deploy-simple/docker-deploy.sh rebuild` |
| 完全停止 | `./deploy-simple/docker-deploy.sh down` |
| 看内存 | `docker stats --no-stream` |
| 看磁盘 | `df -h /opt/minimax` |
| 进入容器 | `docker exec -it minimax-gateway bash` |

---

## 🆘 联系支持

- GitHub: https://github.com/liugl951127/miniLiugl/issues
- 文档: 本目录的 `README.md`
- 部署: `deploy-simple/README.md`

---

## V6.8 版本更新 (2026-08-10)

### V6.8.1 ~ V6.8.3: 前端重构与 API 修复

#### V6.8.1 API 路径修复
- **根因**: 后端 context-path `/api/v1` 和 gateway StripPrefix=2 冲突
- **修法**: 删除 11 个微服务的 `context-path=/api/v1`，改 StripPrefix=0
- **影响**: 100+ controller 已有 `/api/v1` 前缀，统一走 gateway 不剥前缀

#### V6.8.2 业务组件化
- **PageStandard / CrudTable / FormDrawer** 标准组件重构
- **useTable / useCrud / useConfirm / usePageSetup** composable 替代 60+ view 重复 reactive
- 17 admin view 从 5490 行→2837 行 (-48%)
- 12 ai/agent/analytics view 批量重构

#### V6.8.3 t() i18n 修复
- 4 view 补 `import { t } from '@/i18n'`
- layout/Index.vue 加 `import { t } from '@/i18n'`
- schema 补 ai_chat_session 5 列 (status/intent/confidence/alternatives/model)

### V6.8.4 ~ V6.8.6: 批量重构

#### V6.8.4 路由切 V2
- 17 admin 路由全部指向 V2 版本
- 25 个单元测试通过 (92.6%)
- 修 5 真错: `import { http }` → `import http` / 槽嵌套错 / format.js

#### V6.8.5 登录链路审计
- `/login` 路由链路 100% 对齐
- 12 V2 view `import { http }` → default import
- 9 speechCall `.value` 模板错修复

#### V6.8.6 批量重构剩余 24 view
- 24 个 V2 生成 (ai 10 + analytics 4 + pipeline 1 + user 2...)
- 删除 39 个旧 view (节省 14388 行)
- 修 TrainingV2 columns 数组语法错
- 修 VideoStream/Intent 路由漏改 V2

### V6.8.7 ~ V6.8.9: 全链路审计

#### V6.8.7 全链路审计
- 130 后端 @RequestMapping vs 441 前端 http.* API 100% 对齐
- 0 个 `import { http }` 错 / 0 个模板 .value 错 / 0 个 watch 未导入
- build 0 错 0 警 / vitest 269 通过 / E2E 5 端点全通

#### V6.8.8 Bug 修复
- `t.t is not a function`: 4 view + layout 补 t import
- 4 菜单路径错: `/ai/ai-ppt` → `/ai-ppt` (相对父路由正确)
- schema 缺 5 列: `ALTER TABLE ai_chat_session ADD COLUMN status ...`

#### V6.8.9 全量静态审计
- t() import: 29/29 OK / 菜单路径: 29/29 对齐
- 修 CrudTable/FormDrawer reactive `.value` 模板错 (7 处)
- 菜单跳转 35/35 全过 SPA E2E 验证

### V6.8.10 401 Token 根因修复

#### 问题现象
登录后页面跳转提示 "缺少 token" 或 "缺少 Authorization 头"

#### 根因分析
29 个 view 直接调用 `http.get('admin/xxx')` 无前导 `/`
→ http.js interceptor 不追加 `/api/v1` 前缀
→ 请求到 `/admin/xxx` (非 `/api/v1/admin/xxx`)
→ nginx/gateway 路由到 auth 服务
→ auth 服务无 Authorization 头报 401

#### 修复文件
| 文件 | 修复数 |
|------|--------|
| `api/admin.js` | 11 |
| `admin/DashboardV2.vue` | 2 |
| `admin/GovernanceV2.vue` | 4 |
| `admin/ProviderV2.vue` | 5 |
| `admin/WechatBindingsV2.vue` | 3 |
| `admin/WechatUnionidAdminV2.vue` | 2 |
| `admin/ApiKeyStatsV2.vue` | 2 |
| `admin/TracesV2.vue` | 1 |
| `admin/I18nCoverageV2.vue` | 1 |
| `admin/IndexV2.vue` | 1 |
| `knowledge/Index.vue` | 4 |
| `memory/Index.vue` | 4 |
| **合计** | **40+ 处** |

#### 验证命令
```bash
# 本地验证
git pull origin main
cd backend && mvn clean install -DskipTests
docker compose up -d --build
cd ../frontend && npm install && npm run build
# 强刷浏览器 Ctrl+Shift+R
```

### API 路径规范 (必须遵守)

所有前端 API 调用**必须以 `/` 开头**，由 http.js interceptor 统一追加 `/api/v1`：

```javascript
// ✅ 正确
http.get('/admin/users')
http.post('/ai/chat/sessions')
http.put(`/rag/kb/${id}`)

// ❌ 错误 (缺失前导 /, 导致 401 或 404)
http.get('admin/users')
http.post('ai/chat/sessions')
```

### 前端路由跳转规范

路由 path 相对于父路由：

```javascript
// 父路由 /ai, 子路由 path: 'ai-ppt'
// → 绝对路径 /ai-ppt (不是 /ai/ai-ppt)
{ path: '/ai', children: [{ path: 'ai-ppt', ... }] }

// menuRoutes 必须和 router 子路径一致
menuRoutes = [{ path: '/ai-ppt', ... }]  // 不是 /ai/ai-ppt
```

### 常见 401 排查

| 症状 | 原因 | 修法 |
|------|------|------|
| 登录后立即 401 | API 路径无 `/` 前缀 | 加 `/` |
| 刷新页面 401 | token 未持久化 | 检查 pinia persist |
| 特定接口 401 | 后端 service 路径错 | 检查 nginx location |
| 跨域 401 | CORS 未配置 | gateway 加 CORS header |
| refresh 后 401 | refreshToken 过期 | 重新登录 |