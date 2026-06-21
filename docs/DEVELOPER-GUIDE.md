# 🚀 MiniMax 开发者上手手册

> 从零搭建 + 配置 + 上线全流程
> **预计耗时**: 新手 4-6 小时, 老手 1-2 小时

---

## 📑 目录

- [0. 环境准备](#0-环境准备)
- [1. 项目克隆 + 初始化](#1-项目克隆--初始化)
- [2. 各平台 AppID 申请](#2-各平台-appid-申请)
- [3. 数据库配置](#3-数据库配置)
- [4. 微信回调域 + 公网域名](#4-微信回调域--公网域名)
- [5. JWT 密钥 + 业务配置](#5-jwt-密钥--业务配置)
- [6. AI 模型 API Key](#6-ai-模型-api-key)
- [7. 前端域名 + nginx](#7-前端域名--nginx)
- [8. 启动 + 联调](#8-启动--联调)
- [9. 联调测试 Checklist](#9-联调测试-checklist)
- [10. 常见问题](#10-常见问题)

---

## 0. 环境准备

### 必备工具

| 工具 | 版本 | 用途 |
|------|------|------|
| **JDK** | 17+ | 后端运行 |
| **Maven** | 3.8+ | 后端构建 |
| **Node.js** | 22 LTS | 前端构建 |
| **MariaDB / MySQL** | 10.5+ / 8.0+ | 数据库 |
| **Git** | 2.30+ | 代码管理 |
| **nginx** | 1.18+ | 反向代理 |
| **openssl** | - | HTTPS 证书 |

### 一键安装 (Ubuntu 22.04)

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven nodejs npm mariadb-server nginx git curl

# 安装 nvm 管理 node (推荐 22 LTS)
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
source ~/.bashrc
nvm install 22 && nvm use 22
```

### macOS (开发机)

```bash
brew install openjdk@17 maven node@22 mariadb nginx git
brew services start mariadb
brew services start nginx
```

### 验证环境

```bash
java -version          # openjdk version "17.x"
mvn -version           # Apache Maven 3.8+
node -v                # v22.x
mysql --version        # mysql  Ver 15.1 Distrib 10.5-MariaDB
nginx -v               # nginx version: nginx/1.18
```

### 准备域名 + 服务器

| 需求 | 推荐 |
|------|------|
| 公网 IP | 阿里云/腾讯云 ECS |
| 域名 | your-domain.com (需 ICP 备案) |
| HTTPS | Let's Encrypt 免费证书 |

微信开放平台要求:
- **域名必须 HTTPS** (微信强制要求)
- **域名必须 ICP 备案** (国内)

---

## 1. 项目克隆 + 初始化

### 1.1 克隆代码

```bash
git clone https://github.com/liugl951127/miniLiugl.git
cd miniLiugl
```

### 1.2 初始化数据库

```bash
# 启动 MariaDB
sudo systemctl enable mariadb
sudo systemctl start mariadb

# 设置 root 密码 (首次)
sudo mysql_secure_installation

# 创建数据库 + 用户
mysql -uroot -p <<'SQL'
CREATE DATABASE minimax_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'minimax'@'localhost' IDENTIFIED BY 'minimax_pass_2024';
GRANT ALL ON minimax_platform.* TO 'minimax'@'localhost';
GRANT ALL ON minimax_platform.* TO 'minimax'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL

# 导入 SQL (41 张表)
mysql -uroot -p minimax_platform < sql/init-minimax.sql

# 验证
mysql -uminimax -pminimax_pass_2024 -e "SHOW TABLES;" minimax_platform | wc -l
# 应该返回 42 (含表头 "Tables_in_xxx")
```

### 1.3 配置文件

**最简方式**: 编辑 `backend/*/src/main/resources/application.yml`

> 沙箱场景可以用默认配置直接跑 (mock 模式).
> 生产场景需要替换占位符 → 真实凭证.

### 1.4 一键启动 (开发模式)

```bash
# 终端 1: 启动后端 auth 模块
cd backend/minimax-auth
mvn spring-boot:run

# 终端 2: 启动后端 model 模块
cd backend/minimax-model
mvn spring-boot:run

# ... 其他模块同理

# 终端 N: 启动前端
cd frontend
npm install
npm run dev
# → http://localhost:5173
```

### 1.5 一键打包 (生产模式)

```bash
sudo bash scripts/deploy-linux.sh install
# → 自动 systemd + nginx + 启动所有服务
```

---

## 2. 各平台 AppID 申请

### 2.1 微信开放平台 (主战场)

#### 步骤 1: 注册开放平台账号

1. 访问 https://open.weixin.qq.com
2. 用邮箱注册开发者账号 (企业主体或个人都可)
3. 完成邮箱验证

#### 步骤 2: 开发者资质认证

- **个人开发者**: 身份证认证 (1-2 工作日)
- **企业开发者**: 营业执照 + 对公账户验证 (3-7 工作日)

> ⚠️ 个人账号无法申请网站应用, 只能申请小程序.

#### 步骤 3: 创建"网站应用" (扫码登录用)

进入管理中心 → 网站应用 → 创建网站应用:

| 字段 | 填写 |
|------|------|
| 应用名称 | MiniMax 大模型平台 |
| 应用官网 | `https://your-domain.com` |
| 应用简介 | AI 对话平台, 支持多模型 |
| 应用类目 | 工具 → 在线工具 |

审核通过后获得 **AppID** + **AppSecret**.

#### 步骤 4: 创建"移动应用" (App 一键登录)

类似步骤 3, 应用平台选 "移动应用", 填写应用签名 (Android) 或 Bundle ID (iOS).

#### 步骤 5: 创建"公众号"

1. 访问 https://mp.weixin.qq.com 用同一个邮箱注册公众号
2. 选择"服务号" (有 OAuth 权限)
3. 完成企业认证

#### 步骤 6: 创建"小程序"

1. https://mp.weixin.qq.com 选"小程序"
2. 同样的邮箱注册, 但跟公众号是分开的 (需要单独认证)

#### 步骤 7: 把所有应用绑定到同一开放平台

**关键**: 必须在 https://open.weixin.qq.com 的"管理中心 → 公众账号"中绑定公众号/小程序:

| 微信公众平台 | 开放平台 |
|--------------|----------|
| AppID: wx1234abc | 绑定的开放平台: wxp_xxx |
| AppID: wx5678def | 绑定的开放平台: wxp_xxx (同上) |

只有这样 unionid 才会一致 (跨应用打通).

#### 步骤 8: 配置回调域

进入应用详情 → 开发信息:

| 字段 | 填写 |
|------|------|
| 授权回调域 | `your-domain.com` (不带 https) |
| 业务域名 | `your-domain.com` (用于 JS SDK) |
| 网页授权域名 | `your-domain.com` |

#### 拿到凭证后保存

```text
微信开放平台 (open.weixin.qq.com):
  网站应用 AppID:     wx0a1b2c3d4e5f6g7h
  网站应用 AppSecret: a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
  移动应用 AppID:     wx9z8y7x6w5v4u3t2s1r
  移动应用 AppSecret: q1w2e3r4t5y6u7i8o9p0a1s2d3f4g5h6

公众号 AppID:         wxabcdefghijklmnop
公众号 AppSecret:     1234567890abcdefghijklmnopqrstuv

小程序 AppID:         wxqrstuvwxyz123456
小程序 AppSecret:     zyxwvutsrqponmlkjihgfedcba098765
```

### 2.2 QQ 互联

#### 步骤 1: 注册

1. https://connect.qq.com 用 QQ 登录
2. 进入"应用管理" → 创建应用
3. 选择"网站应用"

#### 步骤 2: 填写资料

| 字段 | 填写 |
|------|------|
| 应用名称 | MiniMax 大模型平台 |
| 应用域名 | `your-domain.com` |
| 回调地址 | `https://your-domain.com/auth/oauth/qq/callback` |
| 应用简介 | AI 对话 |

审核 1-3 天, 通过后获得 **APP ID** + **APP Key**.

```text
QQ APP ID:  123456789
QQ APP Key: aBcDeFgHiJkLmNoPqRsTuVwXyZ
```

### 2.3 支付宝开放平台

#### 步骤 1: 注册

1. https://open.alipay.com 用支付宝账号登录
2. 进入"开发者中心" → 创建应用 → "网页 & 移动应用"

#### 步骤 2: 填写资料

| 字段 | 填写 |
|------|------|
| 应用名称 | MiniMax 大模型平台 |
| 应用类型 | 网页应用 |
| 授权回调地址 | `https://your-domain.com/auth/oauth/alipay/callback` |

#### 步骤 3: 生成 RSA 密钥

```bash
# 用支付宝提供的工具生成 (https://opendocs.alipay.com/common/02kkv7)
# 或 openssl:
openssl genrsa -out app_private_key.pem 2048
openssl rsa -in app_private_key.pem -pubout -out app_public_key.pem

# 把 app_private_key.pem 内容填到应用配置 (去除 BEGIN/END 行 + 换行)
cat app_private_key.pem | grep -v "BEGIN\|END" | tr -d '\n' > app_private_key_one_line.txt
```

#### 步骤 4: 上传公钥到支付宝后台

把 `app_public_key.pem` 内容粘贴到支付宝应用 → 应用公钥.

支付宝返回 **支付宝公钥** (用来验签), 也保存下来.

```text
支付宝 AppID:       2021000123456789
支付宝 私钥:        (私钥内容, 一行)
支付宝 公钥:        (从支付宝后台复制)
```

> ⚠️ 支付宝私钥绝对不能提交到 git!

### 2.4 GitHub OAuth (可选)

1. https://github.com/settings/developers → New OAuth App
2. 回调: `https://your-domain.com/auth/oauth/github/callback`

```text
GitHub Client ID:     Iv1.abc123...
GitHub Client Secret: 1234567890abcdef...
```

### 2.5 汇总表

打印这张表, 对照配置:

```
┌──────────┬────────────┬────────────┬──────────────────────────────┐
│  平台    │  AppID     │ AppSecret  │  备注                        │
├──────────┼────────────┼────────────┼──────────────────────────────┤
│ 微信 web │ wx0a1...   │ a1b2...    │ 开放平台网站应用              │
│ 微信 mp  │ wxabcd...  │ 1234...    │ 公众号服务号                  │
│ 微信 mini│ wxqrst...  │ zyxw...    │ 小程序                        │
│ QQ web   │ 123456789  │ aBcDe...   │ QQ 互联                       │
│ 支付宝   │ 20210001.. │ (RSA 私钥) │ 开放平台网页应用              │
│ GitHub   │ Iv1.abc..  │ 1234...    │ (可选)                        │
└──────────┴────────────┴────────────┴──────────────────────────────┘
```

---

## 3. 数据库配置

### 3.1 更新 oauth_app_config (核心)

```bash
mysql -uminimax -pminimax_pass_2024 minimax_platform
```

#### 微信扫码登录

```sql
-- 启用网站应用扫码 (替代 V5 老 wechat_config)
INSERT INTO oauth_app_config (platform, app_type, app_id, app_secret, redirect_uri, enabled)
VALUES ('wechat', 'web', 'wx0a1b2c3d4e5f6g7h',
        'a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6',
        'https://your-domain.com/auth/oauth/wechat/callback', 1)
ON DUPLICATE KEY UPDATE
  app_id = VALUES(app_id), app_secret = VALUES(app_secret),
  redirect_uri = VALUES(redirect_uri), enabled = 1;

-- 公众号 OAuth (移动端)
INSERT INTO oauth_app_config (platform, app_type, app_id, app_secret, redirect_uri, enabled)
VALUES ('wechat', 'mp', 'wxabcdefghijklmnop',
        '1234567890abcdefghijklmnopqrstuv',
        'https://your-domain.com/auth/oauth/wechat/callback', 1)
ON DUPLICATE KEY UPDATE
  app_id = VALUES(app_id), app_secret = VALUES(app_secret), enabled = 1;

-- 小程序
INSERT INTO oauth_app_config (platform, app_type, app_id, app_secret, redirect_uri, enabled)
VALUES ('wechat', 'mini', 'wxqrstuvwxyz123456',
        'zyxwvutsrqponmlkjihgfedcba098765', '', 1)
ON DUPLICATE KEY UPDATE
  app_id = VALUES(app_id), app_secret = VALUES(app_secret), enabled = 1;
```

#### QQ 登录

```sql
INSERT INTO oauth_app_config (platform, app_type, app_id, app_secret, redirect_uri, enabled)
VALUES ('qq', 'web', '123456789',
        'aBcDeFgHiJkLmNoPqRsTuVwXyZ',
        'https://your-domain.com/auth/oauth/qq/callback', 1)
ON DUPLICATE KEY UPDATE
  app_id = VALUES(app_id), app_secret = VALUES(app_secret), enabled = 1;
```

#### 支付宝

```sql
INSERT INTO oauth_app_config (platform, app_type, app_id, app_secret, public_key, redirect_uri, enabled)
VALUES ('alipay', 'web', '2021000123456789',
        '-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEA...\n-----END RSA PRIVATE KEY-----',
        '-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...\n-----END PUBLIC KEY-----',
        'https://your-domain.com/auth/oauth/alipay/callback', 1)
ON DUPLICATE KEY UPDATE
  app_id = VALUES(app_id), app_secret = VALUES(app_secret),
  public_key = VALUES(public_key), enabled = 1;
```

#### GitHub (可选)

```sql
INSERT INTO oauth_app_config (platform, app_type, app_id, app_secret, redirect_uri, enabled)
VALUES ('github', 'web', 'Iv1.abc123...',
        '1234567890abcdef...',
        'https://your-domain.com/auth/oauth/github/callback', 1)
ON DUPLICATE KEY UPDATE
  app_id = VALUES(app_id), app_secret = VALUES(app_secret), enabled = 1;
```

### 3.2 验证配置

```sql
SELECT platform, app_type, enabled, app_id, redirect_uri FROM oauth_app_config WHERE enabled = 1;
```

应该看到至少 4-5 条 enabled = 1 的记录.

---

## 4. 微信回调域 + 公网域名

### 4.1 微信 JS 接口安全域名

**用途**: 微信内嵌网页用 JS SDK (扫一扫 / 分享 / 支付).

进入 **微信公众平台** (mp.weixin.qq.com) → 公众号设置 → 功能设置 → JS 接口安全域名:

| 域名 | 说明 |
|------|------|
| `your-domain.com` | 主域名 |
| `api.your-domain.com` | API 子域名 |

按要求上传 `MP_verify_xxx.txt` 到 `your-domain.com/MP_verify_xxx.txt`:

```bash
# 微信会给你一个文件, 放到前端项目
cp MP_verify_xxx.txt /workspace/minimax-platform/frontend/public/
# 重启前端服务
```

### 4.2 微信开放平台授权回调域

进入 **微信开放平台** (open.weixin.qq.com) → 网站应用详情 → 开发信息:

| 字段 | 填写 |
|------|------|
| 授权回调域 | `api.your-domain.com` (不带 https) |

### 4.3 公众号网页授权域名

进入 **微信公众平台** → 公众号设置 → 功能设置 → 网页授权域名:

| 域名 | 说明 |
|------|------|
| `api.your-domain.com` | 放验证文件 |

上传 `MP_verify_xxx.txt` 到 `api.your-domain.com/MP_verify_xxx.txt`:

```bash
# 在前端 nginx 根目录
cp MP_verify_xxx.txt /var/www/html/
```

### 4.4 小程序服务器域名

进入 **微信公众平台** (mp.weixin.qq.com) → 小程序后台 → 开发 → 开发管理 → 服务器域名:

| 域名 | 用途 |
|------|------|
| `https://api.your-domain.com` | request 合法域名 |
| `wss://api.your-domain.com` | socket 合法域名 |
| `https://api.your-domain.com` | uploadFile 合法域名 |
| `https://api.your-domain.com` | downloadFile 合法域名 |

### 4.5 配置 HTTPS 证书 (必须!)

微信强制要求 HTTPS.

#### 用 Let's Encrypt 免费证书

```bash
# 安装 certbot
sudo apt install certbot python3-certbot-nginx

# 申请证书 (注意: 域名必须先解析到服务器 IP)
sudo certbot --nginx -d your-domain.com -d api.your-domain.com

# 自动续期
sudo crontab -e
# 加一行: 0 3 * * * certbot renew --quiet
```

#### 用阿里云/腾讯云免费证书

1. 控制台 → SSL 证书 → 申请免费 DV
2. 下载 Nginx 格式
3. 上传到 `/etc/nginx/ssl/your-domain.com.pem` + `.key`

### 4.6 验证回调

```bash
# 测试 API 是否能被微信服务器访问
curl https://api.your-domain.com/auth/oauth/wechat/config?appType=web
# 应返回: {"platform":"wechat","appType":"web","configured":true,"enabled":true,...}
```

---

## 5. JWT 密钥 + 业务配置

### 5.1 JWT 密钥 (必改)

**位置**: `backend/minimax-auth/src/main/resources/application.yml`

```yaml
minimax:
  jwt:
    # ⚠️ 生产必须改成随机密钥 (至少 32 字节)
    # 生成: openssl rand -base64 32
    secret: 'VwSWPd816F4nwowFzF5B0F8rihlle2836g6QAh5i13o='
    expiration-ms: 1800000   # 30 分钟
    refresh-expiration-ms: 604800000  # 7 天
```

**生成强密钥**:

```bash
openssl rand -base64 32
# 输出: aBcDeFgHiJkLmNoPqRsTuVwXyZ1234567890==
```

⚠️ 改完所有 12 个后端模块的 yml 都要改, 或用环境变量:

```bash
export JWT_SECRET="$(openssl rand -base64 32)"
```

### 5.2 数据库连接 (12 个模块都要改)

**位置**: `backend/minimax-*/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/minimax_platform?...
    username: minimax
    password: minimax_pass_2024    # 生产改成强密码
```

或者用环境变量统一管理:

```bash
# /etc/minimax.env
DB_URL=jdbc:mysql://127.0.0.1:3306/minimax_platform?useSSL=true
DB_USER=minimax
DB_PASS=你的强密码
JWT_SECRET=你的密钥
```

然后 systemd service 里 `EnvironmentFile=/etc/minimax.env`.

### 5.3 修改 super admin 默认密码 (重要!)

```bash
# 用 BCrypt 工具生成新密码哈希
python3 -c "import bcrypt; print(bcrypt.hashpw(b'你的新密码', bcrypt.gensalt(10)).decode())"
# 输出: $2b$10$xxx...

# 更新 DB
mysql -uroot minimax_platform -e \
  "UPDATE sys_user SET password='\$2b\$10\$xxx...' WHERE username='adminLiugl';"
```

---

## 6. AI 模型 API Key

### 6.1 各大模型厂商

| 厂商 | 申请地址 | 模型 |
|------|---------|------|
| **SiliconFlow** | https://cloud.siliconflow.cn | Qwen2.5 / DeepSeek / GLM-4 |
| **阿里云 DashScope** | https://dashscope.aliyun.com | Qwen-Max / Qwen-Plus |
| **DeepSeek 官方** | https://platform.deepseek.com | DeepSeek-V2 / V3 |
| **OpenAI** | https://platform.openai.com | GPT-4o / GPT-4-Turbo |
| **Anthropic** | https://console.anthropic.com | Claude-3.5-Sonnet |
| **智谱 AI** | https://open.bigmodel.cn | GLM-4-Plus |

### 6.2 申请 API Key

以 SiliconFlow 为例:
1. 注册 + 实名认证
2. 控制台 → API Keys → 创建
3. 复制 `sk-xxx...`

### 6.3 配置到数据库

**位置**: `model_api_key` 表 (V4 已有)

```sql
-- SiliconFlow
INSERT INTO model_api_key (provider, api_key, base_url, enabled, priority)
VALUES ('siliconflow', 'sk-siliconflow-xxx...',
        'https://api.siliconflow.cn/v1', 1, 100);

-- 阿里云 DashScope
INSERT INTO model_api_key (provider, api_key, base_url, enabled, priority)
VALUES ('dashscope', 'sk-dashscope-xxx...',
        'https://dashscope.aliyuncs.com/compatible-mode/v1', 1, 90);

-- DeepSeek
INSERT INTO model_api_key (provider, api_key, base_url, enabled, priority)
VALUES ('deepseek', 'sk-deepseek-xxx...',
        'https://api.deepseek.com/v1', 1, 80);

-- OpenAI (可选)
INSERT INTO model_api_key (provider, api_key, base_url, enabled, priority)
VALUES ('openai', 'sk-openai-xxx...',
        'https://api.openai.com/v1', 0, 70);  -- 暂不启用
```

### 6.4 配置默认模型

```sql
UPDATE model_provider SET enabled = 1 WHERE provider IN ('siliconflow', 'dashscope', 'deepseek');
```

### 6.5 验证

```bash
ADMIN_TOKEN=$(curl -X POST https://api.your-domain.com/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"adminLiugl","password":"你的密码"}' | jq -r .data.accessToken)

curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  https://api.your-domain.com/api/v1/model/providers
```

应该返回启用的模型列表.

---

## 7. 前端域名 + nginx

### 7.1 前端域名配置

**位置**: `frontend/vite.config.js`

```js
export default defineConfig({
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api/v1/auth':  { target: 'http://localhost:8081', changeOrigin: true },
      '/api/v1/chat':  { target: 'http://localhost:8082', changeOrigin: true },
      '/api/v1/model': { target: 'http://localhost:8083', changeOrigin: true },
      // ... 12 个模块
    }
  }
})
```

**生产构建**:

```bash
cd frontend
npm run build
# → dist/ 目录
```

### 7.2 nginx 配置

**位置**: `/etc/nginx/conf.d/minimax.conf`

```nginx
# 强制 HTTPS
server {
    listen 80;
    server_name your-domain.com api.your-domain.com;
    return 301 https://$host$request_uri;
}

# 前端主域名
server {
    listen 443 ssl http2;
    server_name your-domain.com;
    ssl_certificate /etc/nginx/ssl/your-domain.com.pem;
    ssl_certificate_key /etc/nginx/ssl/your-domain.com.key;

    root /var/www/minimax/frontend;
    index index.html;

    # 微信验证文件
    location /MP_verify_xxx.txt { root /var/www/minimax; }

    # SPA 路由
    location / {
        try_files $uri $uri/ /index.html;
    }
}

# 后端 API 子域名
server {
    listen 443 ssl http2;
    server_name api.your-domain.com;
    ssl_certificate /etc/nginx/ssl/your-domain.com.pem;
    ssl_certificate_key /etc/nginx/ssl/your-domain.com.key;

    client_max_body_size 100M;

    # 微服务分流
    location /api/v1/auth/      { proxy_pass http://127.0.0.1:8081; }
    location /api/v1/chat/      { proxy_pass http://127.0.0.1:8082; }
    location /api/v1/model/     { proxy_pass http://127.0.0.1:8083; }
    location /api/v1/memory/    { proxy_pass http://127.0.0.1:8084; }
    location /api/v1/rag/       { proxy_pass http://127.0.0.1:8085; }
    location /api/v1/function/  { proxy_pass http://127.0.0.1:8086; }
    location /api/v1/admin/     { proxy_pass http://127.0.0.1:8087; }
    location /api/v1/multi/     { proxy_pass http://127.0.0.1:8088; }
    location /api/v1/monitor/   { proxy_pass http://127.0.0.1:8089; }
    location /api/v1/agent/     { proxy_pass http://127.0.0.1:8090; }
    location /api/v1/prompt/    { proxy_pass http://127.0.0.1:8091; }
    location /ws/              {
        proxy_pass http://127.0.0.1:8095;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

### 7.3 复制前端构建产物

```bash
sudo mkdir -p /var/www/minimax/frontend
sudo cp -r frontend/dist/* /var/www/minimax/frontend/

# 复制微信验证文件
sudo cp MP_verify_xxx.txt /var/www/minimax/

sudo nginx -t
sudo systemctl reload nginx
```

---

## 8. 启动 + 联调

### 8.1 启动顺序

```bash
# 1. 数据库
sudo systemctl start mariadb

# 2. 后端所有服务 (按 deploy-linux.sh 已配置好)
sudo systemctl start minimax-auth
sudo systemctl start minimax-chat
sudo systemctl start minimax-model
# ... 其他模块

# 或者一键
sudo bash scripts/deploy-linux.sh start

# 3. nginx
sudo systemctl start nginx
```

### 8.2 验证服务

```bash
# 后端健康检查
curl https://api.your-domain.com/auth/oauth/wechat/config?appType=web

# 前端首页
curl -I https://your-domain.com
```

### 8.3 看日志

```bash
sudo tail -f /var/log/minimax/auth.log
sudo journalctl -u minimax-auth -f
```

---

## 9. 联调测试 Checklist

### ✅ 第一阶段: 数据库 + 后端启动

- [ ] MariaDB 启动, 41 张表都建好
- [ ] 后端 12 个模块全部启动 (端口 8081-8095)
- [ ] `curl http://localhost:8081/auth/login` 返回正常
- [ ] adminLiugl 能登录, 拿到 JWT

### ✅ 第二阶段: OAuth 配置

- [ ] oauth_app_config 表 enabled = 1 的记录 ≥ 4 条
- [ ] 微信开放平台授权回调域已配 `api.your-domain.com`
- [ ] 公众号 JS 安全域名已配 `your-domain.com`
- [ ] MP_verify_xxx.txt 上传成功
- [ ] HTTPS 证书有效
- [ ] `curl https://api.your-domain.com/auth/oauth/wechat/config` 返回 configured=true

### ✅ 第三阶段: 微信扫码

- [ ] 前端访问 `https://your-domain.com/login`
- [ ] 选"微信扫码" tab, 显示二维码 (mock 模式有"模拟扫码"按钮)
- [ ] 真实模式: 用微信扫码 → 跳转到 `your-domain.com/wechat-scan-result?ok=1&access_token=xxx`
- [ ] 拿到 token 后能访问 `/api/v1/auth/me`
- [ ] DB `sys_user.wechat_openid` 自动写入

### ✅ 第四阶段: 公众号 OAuth (移动端)

- [ ] 微信内打开 `your-domain.com/h5-login`
- [ ] 点"微信公众号"按钮 → 自动跳微信授权页
- [ ] 授权后回到 `your-domain.com/h5-login?access_token=xxx`
- [ ] 用户已登录, 跳业务页

### ✅ 第五阶段: QQ 登录

- [ ] PC 访问 `https://your-domain.com/h5-login`
- [ ] 点"QQ 登录" → 跳 `graph.qq.com/oauth2.0/authorize`
- [ ] QQ 授权后跳回 `your-domain.com/auth/oauth/qq/callback?code=xxx`
- [ ] 自动登录, `sys_user.qq_openid` 写入

### ✅ 第六阶段: 支付宝登录

- [ ] 点"支付宝登录" → 跳 `openauth.alipay.com`
- [ ] 支付宝授权 → 自动登录
- [ ] `sys_user.alipay_openid` 写入
- [ ] 如果是生产, 检查 RSA 签名正确

### ✅ 第七阶段: 跨应用 unionid 打通

- [ ] 用微信扫码 → 创建 user A (id=N)
- [ ] 用 QQ 登录 (mock unionid 同) → 复用 user A
- [ ] DB 查 oauth_binding 应该返回 2 条 (wechat + qq)
- [ ] admin 端 `cross-platform-stats` 显示 multiPlatformUsers = 1

### ✅ 第八阶段: AI 模型

- [ ] admin 配置好 model_api_key (siliconflow / dashscope / deepseek)
- [ ] 进入 `/chat` 选模型, 发消息
- [ ] 模型正常回复 (token 消耗 = 真实 API 费用)
- [ ] 多模型对决页能同时调 3 个模型

### ✅ 第九阶段: WebSocket

- [ ] 打开 `/chat`, 发消息, 看到打字效果 (流式输出)
- [ ] 后端日志看到 `WS message` 日志
- [ ] 多标签页实时同步

### ✅ 第十阶段: 部署验收

- [ ] 服务器重启后 systemd 自动拉起所有服务
- [ ] 数据库自动备份 (`deploy-linux.sh backup`)
- [ ] 监控脚本能告警 (`/api/v1/monitor/stats`)

---

## 10. 常见问题

### Q1: 微信扫码后报 "redirect_uri 参数错误"

A: 检查 3 处:
1. **开放平台** → 网站应用 → 授权回调域 是否填了 `api.your-domain.com` (不带 https)
2. **数据库** → `oauth_app_config.redirect_uri` 是否 `https://api.your-domain.com/auth/oauth/wechat/callback`
3. nginx 转发是否正常 (`curl https://api.your-domain.com/auth/oauth/wechat/callback`)

### Q2: 公众号 JS SDK 报 "invalid url domain"

A: 公众号 → 功能设置 → JS 接口安全域名:
- 必须是 `your-domain.com` (不带 https, 不带子域名)
- 必须上传 `MP_verify_xxx.txt` 到域名根目录

### Q3: 小程序报 "不在以下 request 合法域名列表中"

A: 小程序后台 → 开发管理 → 服务器域名 → 添加 `https://api.your-domain.com`

### Q4: 数据库连接被拒 "Access denied"

A: MariaDB 重装后会丢用户. 重建:
```sql
CREATE USER IF NOT EXISTS 'minimax'@'localhost' IDENTIFIED BY 'minimax_pass_2024';
CREATE USER IF NOT EXISTS 'minimax'@'127.0.0.1' IDENTIFIED BY 'minimax_pass_2024';
GRANT ALL ON minimax_platform.* TO 'minimax'@'localhost';
GRANT ALL ON minimax_platform.* TO 'minimax'@'127.0.0.1';
FLUSH PRIVILEGES;
```

### Q5: 401 Unauthorized 用所有 API 都失败

A: JWT 密钥不一致. 所有模块的 `application.yml` 都要改 `jwt.secret`, 或者用环境变量统一管理.

### Q6: 静态资源 404 (前端页面打不开)

A: `application.yml` 加:
```yaml
spring:
  mvc:
    throw-exception-if-no-handler-found: true
  web:
    resources:
      add-mappings: false
```

让 Spring 优先走 Controller, 不抢静态资源.

### Q7: HTTPS 证书过期

A: Let's Encrypt 90 天, 用 certbot 自动续期:
```bash
sudo crontab -e
0 3 * * * certbot renew --quiet && systemctl reload nginx
```

### Q8: 怎么禁用某个平台的登录按钮?

A: 改 `oauth_app_config.enabled = 0`, 前端按钮会自动隐藏.

### Q9: 怎么加新平台 (例如 GitHub)?

A: 3 步:
1. 创建 `GitHubOAuthClient implements OAuthPlatformClient`
2. 在 `OAuthPlatformService.getClient()` switch 加 case
3. 加 `INSERT INTO oauth_app_config ... VALUES ('github', 'web', ...)`
4. (可选) 前端 `H5Login.vue` 加按钮

### Q10: 怎么修改默认 admin 密码?

A: 不靠 SQL 直接改 (密码是 BCrypt 哈希, 不能 reverse):
```java
// 临时方案: 用 Java BCrypt 生成
BCrypt.hashpw("新密码", BCrypt.gensalt(10))
// 然后 UPDATE sys_user SET password='...' WHERE username='adminLiugl';
```

或启动时改 `application.yml`:
```yaml
minimax:
  auth:
    super-admin-password: 新密码
```

---

## 📞 遇到问题

1. 先看日志: `tail -f /var/log/minimax/*.log`
2. 看 GitHub Issues: https://github.com/liugl951127/miniLiugl/issues
3. 看文档: `docs/` 目录

## 🎯 完整时间估算

| 阶段 | 时间 | 难度 |
|------|------|------|
| 环境准备 | 1h | ⭐ |
| 项目克隆 + DB 初始化 | 0.5h | ⭐ |
| 微信开放平台认证 | 1-3 天 (审核) | ⭐⭐⭐ |
| 各平台 AppID 申请 | 1h | ⭐⭐ |
| 数据库配置 | 0.5h | ⭐ |
| 域名 + HTTPS | 0.5h | ⭐⭐ |
| 联调测试 | 2h | ⭐⭐ |
| **总计** | **5-10h (除审核)** | |

---

> MiniMax 大模型平台 · 开发者上手手册 · 2026-06