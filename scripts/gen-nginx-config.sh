#!/usr/bin/env bash
# =============================================================
# 自动生成 nginx upstream + 反向代理配置
# 扫描所有 minimax-* 服务, 读取端口, 生成 upstream.conf + nginx.conf
#
# 好处: 新增服务时无需手改 nginx, 重新跑脚本即可
# =============================================================

set +e  # 允许空 port 跳过 (不严格)
# shellcheck disable=SC2155

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
NGINX_DIR="$PROJECT_DIR/nginx"
UPSTREAM_OUT="$NGINX_DIR/upstream.conf"
NGINX_OUT="$NGINX_DIR/nginx.conf"

green()  { echo -e "\033[32m$*\033[0m"; }
red()    { echo -e "\033[31m$*\033[0m"; }
yellow() { echo -e "\033[33m$*\033[0m"; }
bold()   { echo -e "\033[1m$*\033[0m"; }

# ============== 扫描所有服务端口 ==============
declare -A PORTS
declare -A DESCS

scan_services() {
  for svc_dir in "$PROJECT_DIR/backend"/minimax-*; do
    [ ! -d "$svc_dir" ] && continue
    svc=$(basename "$svc_dir" | sed 's/minimax-//')
    
    # 1. 从 application.yml 读 port
    port=$(grep -E "^\s*port:" "$svc_dir/src/main/resources/application.yml" 2>/dev/null | head -1 | grep -oE "[0-9]+")
    
    # 2. 从 pom.xml 读 description
    desc=$(grep -oP '<description>\K[^<]+' "$svc_dir/pom.xml" 2>/dev/null | head -1)
    
    if [ -n "$port" ]; then
      PORTS[$svc]=$port
      DESCS[$svc]=$desc
    fi  # 空 port (common 等非服务模块) 跳过
  done
}

# ============== 生成 upstream.conf ==============
gen_upstream() {
  {
    cat << 'HEADER'
# =============================================================
# MiniMax Platform - 后端服务 upstream (自动生成)
# 由 scripts/gen-nginx-config.sh 扫描 backend/* 生成
#
# ⚠️  不要手改! 改了会被重新生成覆盖
#   重新生成: bash scripts/gen-nginx-config.sh
# =============================================================

HEADER

    # 按服务名排序输出
    for svc in $(echo "${!PORTS[@]}" | tr ' ' '\n' | sort); do
      port=${PORTS[$svc]}
      desc=${DESCS[$svc]:-""}
      cat << EOF
# $svc (端口 $port) - $desc
upstream ${svc}_service {
    server 127.0.0.1:${port};
    keepalive 16;
}

EOF
    done
  } > "$UPSTREAM_OUT"
  
  echo "✓ upstream.conf: $UPSTREAM_OUT"
  echo "  包含 $(echo "${!PORTS[@]}" | tr ' ' '\n' | wc -l) 个 upstream"
}

# ============== 生成 nginx.conf ==============
gen_nginx() {
  {
    cat << 'HEADER'
# ============================================================
# MiniMax Platform V3.5.5+ 宿主 Nginx 配置 (自动生成)
# 单端口 80 入口:
#   /             → /usr/share/nginx/html  (前端 Vue3 SPA)
#   /api/v1/{module}/* → 127.0.0.1:{port}  ({module} 后端服务)
#
# ⚠️  不要手改 upstream 段! 改了会被 scripts/gen-nginx-config.sh 覆盖
#    改完跑: bash scripts/gen-nginx-config.sh
# ============================================================

# 后端 upstream (从独立文件 include, 自动生成)
include /etc/nginx/conf.d/minimax-upstream.conf;

# ============== HTTP 服务 ==============
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name _;

    # 日志 (写到 /var/log/nginx + systemd journal)
    access_log /var/log/nginx/minimax-access.log;
    error_log  /var/log/nginx/minimax-error.log warn;

    # 上传文件大小
    client_max_body_size 50M;

    # 性能优化
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;

    # Gzip 压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types
        text/plain
        text/css
        text/xml
        text/javascript
        application/json
        application/javascript
        application/xml+rss
        application/atom+xml
        image/svg+xml;

    # ============== 健康检查 ==============
    location = /healthz {
        add_header Content-Type text/plain;
        return 200 "ok\n";
    }

    # ============== 前端 SPA (Vue3 + Vite, vue-router history) ==============
    location = / {
        root   /usr/share/nginx/html;
        try_files /index-mini.html /index.html;
    }
    location / {
        root   /usr/share/nginx/html;
        index  index-mini.html index.html;
        # SPA fallback: vue-router history 模式刷新不 404
        try_files $uri $uri/ /index-mini.html /index.html;
    }

    # 静态资源 1 天缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        root   /usr/share/nginx/html;
        expires 1d;
        add_header Cache-Control "public, max-age=86400";
        try_files $uri =404;
    }

    # Service Worker (PWA) - 不能缓存
    location = /sw.js {
        root   /usr/share/nginx/html;
        add_header Cache-Control "no-cache";
    }

    # H2 控制台 (开发调试, 生产建议禁用)
    location /h2-console/ {
        proxy_pass http://ai_service/;
    }

HEADER

    # ============== 自动生成所有服务反向代理 ==============
    echo "    # ============== 后端 API 反向代理 (自动生成) =============="
    
    # 按字母排序, 长前缀先匹配
    for svc in $(echo "${!PORTS[@]}" | tr ' ' '\n' | sort); do
      port=${PORTS[$svc]}
      cat << EOF
    # $svc 服务 (端口 $port)
    location /api/v1/${svc}/ {
        proxy_pass         http://${svc}_service;
        proxy_set_header   Host              \$host;
        proxy_set_header   X-Real-IP         \$remote_addr;
        proxy_set_header   X-Forwarded-For   \$proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto \$scheme;
        proxy_http_version 1.1;
        proxy_set_header   Connection        "";
        proxy_connect_timeout 30s;
        proxy_send_timeout    60s;
        proxy_read_timeout    60s;
    }

EOF
    done

    cat << 'FOOTER'

    # 兜底: /api/v1/* 未匹配时, 走 gateway (业务网关聚合)
    location /api/v1/ {
        proxy_pass         http://gateway_service;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_http_version 1.1;
        proxy_set_header   Connection        "";
        proxy_connect_timeout 30s;
        proxy_send_timeout    60s;
        proxy_read_timeout    60s;
    }

    # 兼容旧路径
    location /api/ {
        proxy_pass         http://gateway_service;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_http_version 1.1;
        proxy_set_header   Connection        "";
    }

    # ============== WebSocket (实时通信) ==============
    location /ws/ {
        proxy_pass         http://ws_service;
        proxy_http_version 1.1;
        proxy_set_header   Upgrade           $http_upgrade;
        proxy_set_header   Connection        "upgrade";
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_read_timeout  3600s;   # WS 长连接
    }

    # ============== SSE (Server-Sent Events, AI 流式) ==============
    location /sse/ {
        proxy_pass         http://ai_service;
        proxy_http_version 1.1;
        proxy_set_header   Connection        "";
        proxy_buffering    off;          # SSE 不能缓冲
        proxy_cache        off;
        proxy_read_timeout  600s;        # SSE 长连接
        proxy_set_header   X-Accel-Buffering no;
    }

    # 错误页
    error_page 502 503 504 /50x.html;
    location = /50x.html {
        root /usr/share/nginx/html;
    }
}
FOOTER
  } > "$NGINX_OUT"
  
  echo "✓ nginx.conf: $NGINX_OUT"
  echo "  包含 $(echo "${!PORTS[@]}" | tr ' ' '\n' | wc -l) 个 location 反向代理"
}

# ============== 主流程 ==============
bold ""
bold "🔧 扫描后端服务 + 生成 nginx 配置"
bold ""

scan_services
echo ""
echo "扫描到 $(echo "${!PORTS[@]}" | tr ' ' '\n' | wc -l) 个服务:"
for svc in $(echo "${!PORTS[@]}" | tr ' ' '\n' | sort); do
  port=${PORTS[$svc]}
  printf "  %-12s : %s\n" "$svc" "$port"
done
echo ""

# 生成
gen_upstream
gen_nginx

echo ""
green "✅ 生成完成!"
echo ""
yellow "部署到宿主:"
echo "  sudo cp $UPSTREAM_OUT /etc/nginx/conf.d/minimax-upstream.conf"
echo "  sudo cp $NGINX_OUT /etc/nginx/conf.d/minimax.conf"
echo "  sudo nginx -t && sudo systemctl reload nginx"
echo ""
yellow "本地验证:"
echo "  curl http://localhost/healthz                    # 健康"
echo "  curl http://localhost/api/v1/ai/intent/predict \\"
echo "       -X POST -H 'Content-Type: application/json' \\"
echo "       -d '{\"text\":\"我要退款\"}'"
