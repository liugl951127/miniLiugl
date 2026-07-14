#!/usr/bin/env bash
# =============================================================
# 清理所有 minimax-* 旧配置文件 + nginx 默认 site
# 用于升级 V3.5.5+ (单文件配置) 时清场
# =============================================================

set -e

green() { echo -e "\033[32m$*\033[0m"; }
red()   { echo -e "\033[31m$*\033[0m"; }
yellow(){ echo -e "\033[33m$*\033[0m"; }
bold()  { echo -e "\033[1m$*\033[0m"; }

bold ""
bold "🧹 清理 minimax 旧 nginx 配置 + 默认 site"
bold ""

# 1. 清理 sites-enabled/default (默认监听 80, 跟我们的 server 冲突)
if [ -f /etc/nginx/sites-enabled/default ]; then
    rm -f /etc/nginx/sites-enabled/default
    yellow "  ✓ 删除: /etc/nginx/sites-enabled/default"
fi
if [ -f /etc/nginx/sites-available/default ]; then
    rm -f /etc/nginx/sites-available/default
    yellow "  ✓ 删除: /etc/nginx/sites-available/default"
fi

# 2. 清理 /etc/nginx/conf.d/ 下所有 minimax-* 拆分文件
yellow ""
yellow "  清理 /etc/nginx/conf.d/ 下的 minimax-* 文件:"
shopt -s nullglob 2>/dev/null || true
for f in /etc/nginx/conf.d/minimax-*.conf; do
    if [ -f "$f" ]; then
        rm -f "$f"
        yellow "    ✓ 删除: $(basename $f)"
    fi
done
shopt -u nullglob 2>/dev/null || true

# 3. 清理 sites-enabled / sites-available 下的 minimax
yellow ""
yellow "  清理 sites-enabled / sites-available 下的 minimax*:"
for f in /etc/nginx/sites-enabled/minimax* /etc/nginx/sites-available/minimax*; do
    if [ -f "$f" ]; then
        rm -f "$f"
        yellow "    ✓ 删除: $f"
    fi
done

# 4. 显示现状
yellow ""
yellow "  当前 /etc/nginx/conf.d/ 内容:"
ls -la /etc/nginx/conf.d/ 2>/dev/null
echo ""

# 5. 验证
if command -v nginx &>/dev/null; then
    yellow "  nginx -t 验证:"
    nginx -t 2>&1
fi

green ""
green "✅ 清理完成"
echo ""
echo "  下一步:"
echo "    sudo cp <project>/nginx/nginx.conf /etc/nginx/conf.d/minimax.conf"
echo "    sudo nginx"
echo ""
