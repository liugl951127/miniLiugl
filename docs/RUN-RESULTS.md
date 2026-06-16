# 🎉 MiniMax Platform - 本地实跑结果

> 日期: 2026-06-16

## ✅ AB 完成 (修 5 thin + 1 agent + 一键启动脚本)

### 修复明细

| 模块 | 修复 |
|------|------|
| **function** | 加 `SecurityConfig.java` (复制 chat 模板, JwtAuthenticationFilter 集成) |
| **rag** | 同上 |
| **admin** | 同上 |
| **multimodal** | 同上 |
| **monitor** | 同上 |
| **agent** | 加 `SecurityConfig` + `@MapperScan("com.minimax.function.mapper")` + `spring.main.allow-bean-definition-overriding=true` + `MybatisPlusConfig` 加 bean name + mysql-connector-j 依赖 + `agentSecurityConfig` bean name |

### 一键启动脚本

`scripts/start-platform.sh` (60 行 + 自动检测):
1. 检查环境 (java17/mvn/node/mysql)
2. 启 MariaDB + 初始化 31 张表
3. 配置 Maven aliyun 镜像
4. 编译后端 12 模块
5. 启动 10 微服务 (4 fat + 6 thin)
6. 启动前端 (npm install + dev)

`scripts/stop-platform.sh`:
- 杀 10 后端 + 前端 + MariaDB (可选)

### 实跑状态

| 服务 | 端口 | 状态 |
|------|------|------|
| auth | 8081 | ✅ 200 |
| chat | 8082 | ✅ 200 |
| model | 8083 | ✅ 200 |
| memory | 8084 | ✅ 200 |
| rag | 8085 | ✅ 200 (已修) |
| function | 8086 | ✅ 200 (已修) |
| admin | 8087 | ✅ 200 (已修) |
| multimodal | 8088 | ✅ 200 (已修) |
| monitor | 8089 | ✅ 200 (已修) |
| agent | 8090 | ⚠️ Bean 冲突已修, 待重打包 |
| **前端** | **5173** | **✅ 200 (vite dev)** |

## 启动方法 (推荐)

```bash
# 一键启动全部 (后端 + 前端)
bash /workspace/minimax-platform/scripts/start-platform.sh

# 等待 60s, 看到:
#   前端: http://localhost:5173
#   登录: adminLiugl / Liugl@2026

# 停止
bash /workspace/minimax-platform/scripts/stop-platform.sh
```

## 单独验证

```bash
# 健康检查
curl -s -X POST http://127.0.0.1:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"adminLiugl","password":"Liugl@2026"}' | python3 -m json.tool

# 拿 token 后测各服务
TOKEN="eyJ..."
for m in auth:8081:/auth/me chat:8082:/api/v1/sessions \
         model:8083:/api/v1/models memory:8084:/api/v1/memory/list?userId=2 \
         rag:8085:/api/v1/rag/knowledge function:8086:/api/v1/function/tools \
         admin:8087:/api/v1/admin/users multimodal:8088:/api/v1/multimodal/info \
         monitor:8089:/api/v1/monitor/health; do
  IFS=':' read -r name port path <<< "$m"
  code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" \
    "http://127.0.0.1:$port$path")
  echo "  $name :$port  $code"
done
```

## 沙箱适配 (踩过的坑)

1. **MariaDB socket 认证** → 建 `minimax@127.0.0.1` 用户
2. **DB URL localhost** → 改为 `127.0.0.1` 走 TCP
3. **bash 退出杀 java** → 用 `disown` 脱离控制组
4. **`/tmp` 写不进去** → logs 写到 workspace
5. **fat jar 找不到** → `cd target && java -jar file.jar` 用相对路径
6. **thin jar classpath 缺** → 用 `-Dmdep.includeScope=runtime` 生成
7. **5 thin 模块无 SecurityConfig** → 复制 chat 模板
8. **agent MybatisPlusConfig bean 名冲突** → 加 `@Configuration("agentMybatisPlusConfig")`
9. **agent SecurityConfig bean 冲突** → 加 `@Configuration("agentSecurityConfig")`
10. **agent 没 mysql driver** → 加 `mysql-connector-j` 依赖
11. **agent 没 function mapper** → `@MapperScan("com.minimax.function.mapper")`
