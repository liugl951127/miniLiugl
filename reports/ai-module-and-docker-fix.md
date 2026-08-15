# AI 模块编译 + Docker SQL 注释 (V6.8.1)

## 1. AI 模块编译验证
```
mvn clean compile -pl minimax-ai -am
[INFO] BUILD SUCCESS
```

## 2. 警告修复
- `minimax-pipeline/pom.xml` 重复声明 `minimax-analytics` 依赖 (L41 + L96)
- 删 L96 重复声明

## 3. Docker 注释 SQL 执行
`docker-compose.yml` mariadb 服务:
```yaml
volumes:
  - /data/minimax/mariadb:/var/lib/mysql
  # V6.8.1: 注释掉 SQL 自动执行 (避免重启覆盖数据 / 测试用已存在的 db)
  # V3.5.19 重生: 2 个新 SQL (77 表 + 5 账号 + AI 关键词)
  # - ./sql/minimax-mysql-final.sql:/docker-entrypoint-initdb.d/01-schema.sql:ro
  # - :/docker-entrypoint-initdb.d/02-seed.sql:ro
```

理由: 
- Docker 启动 mariadb 时会自动执行 `docker-entrypoint-initdb.d/*.sql`
- 注释掉后, 重启容器不会覆盖现有数据库
- 用户测试用已初始化的 db, 避免每次重启都重灌

## 4. 验证
- `mariadb volumes` 只剩 `/data/minimax/mariadb:/var/lib/mysql` ✅
- docker-compose.yml 19 个 service 完整 ✅
- 后端 14 module 编译 BUILD SUCCESS ✅
