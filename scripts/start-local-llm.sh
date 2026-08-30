#!/bin/bash
# =====================================================
# V9.0 一键启动 — 本地大模型 + 全栈
# =====================================================
# 用法:
#   bash scripts/start-local-llm.sh
#   跳过大模型: bash scripts/start-local-llm.sh --skip-models
# =====================================================

set -e

SKIP_MODELS=false
if [ "$1" = "--skip-models" ]; then
    SKIP_MODELS=true
fi

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

step() { echo -e "${GREEN}▶ $1${NC}"; }
warn() { echo -e "${YELLOW}⚠ $1${NC}"; }
err()  { echo -e "${RED}✗ $1${NC}"; }

# 1. 检查依赖
step "检查依赖 (JDK 17 / Maven 3.8+ / Node 18+)..."
if ! command -v java &> /dev/null; then
    err "JDK 未安装, 请先装 JDK 17+"
    exit 1
fi
if ! command -v mvn &> /dev/null; then
    err "Maven 未安装"
    exit 1
fi
if ! command -v node &> /dev/null; then
    err "Node 未安装"
    exit 1
fi

# 2. 下载 Qwen2.5-0.5B (本地兜底模型, 488MB)
if [ "$SKIP_MODELS" = false ]; then
    step "下载 Qwen2.5-0.5B 本地模型 (约 488MB)..."
    if [ -d "data/models/qwen2.5-0.5b-instruct" ]; then
        warn "模型已存在, 跳过下载"
    else
        mkdir -p data/models
        # 用 Hugging Face 镜像
        if [ -f "scripts/download-models.sh" ]; then
            bash scripts/download-models.sh qwen
        else
            warn "scripts/download-models.sh 不存在, 跳过模型下载"
        fi
    fi
else
    warn "跳过模型下载 (--skip-models)"
fi

# 3. 启动后端
step "编译并启动后端..."
cd backend
mvn clean install -DskipTests -q

# 同时启动 minimax-ai + minimax-chat + minimax-gateway
nohup mvn -pl minimax-ai -am spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx2g" > /tmp/ai.log 2>&1 &
AI_PID=$!
echo "  minimax-ai PID: $AI_PID"

# 等待 AI 启动
step "等待 minimax-ai 启动 (端口 8090)..."
for i in {1..30}; do
    if curl -s http://localhost:8090/api/v1/ai/llm/status > /dev/null 2>&1; then
        echo "  ✅ minimax-ai 已就绪"
        break
    fi
    sleep 2
done

# 验证本地模型
step "验证 Qwen2.5-0.5B 模型状态..."
STATUS=$(curl -s http://localhost:8090/api/v1/ai/llm/status 2>/dev/null)
echo "  Response: $STATUS"
if echo "$STATUS" | grep -q '"localReady":true'; then
    echo "  ✅ Qwen2.5-0.5B 已加载, 可用本地兜底"
else
    warn "Qwen2.5-0.5B 未就绪, 走纯云端模式"
    warn "  执行: bash scripts/download-models.sh qwen"
fi

# 4. 测试兜底
step "测试 LLM 兜底 (发一个 chat 请求)..."
curl -s -X POST http://localhost:8090/api/v1/ai/llm/chat \
  -H "Content-Type: application/json" \
  -d '{"messages":[{"role":"user","content":"说一句话"}]}' | head -c 300
echo

# 5. 启动前端
step "启动前端 (端口 3000)..."
cd ../frontend
npm install --no-audit --no-fund
nohnpm run dev
nohup npm run dev > /tmp/frontend.log 2>&1 &
FRONT_PID=$!
echo "  frontend PID: $FRONT_PID"

# 等待前端
for i in {1..20}; do
    if curl -s http://localhost:3000 > /dev/null 2>&1; then
        echo "  ✅ 前端已就绪: http://localhost:3000"
        break
    fi
    sleep 2
done

echo
echo -e "${GREEN}=============================================${NC}"
echo -e "${GREEN}✅ 启动完成!${NC}"
echo -e "${GREEN}=============================================${NC}"
echo "前端:        http://localhost:3000"
echo "minimax-ai:  http://localhost:8090"
echo "LLM Status:  http://localhost:8090/api/v1/ai/llm/status"
echo
echo "下一步:"
echo "1. 打开 http://localhost:3000/chat"
echo "2. 发消息, 看 AI 回复是否带 '☁️ 云端' / '💻 本地模型' / '🔄 本地兜底' 标签"
echo
echo "停止服务:"
echo "  kill $AI_PID $FRONT_PID"
echo
echo "日志:"
echo "  tail -f /tmp/ai.log"
echo "  tail -f /tmp/frontend.log"
