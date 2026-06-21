#!/bin/bash
# 输出全部到文件
mkdir -p /workspace/.e2e
OUT=/workspace/.e2e/e2e-result.txt
> $OUT
# 不要 set -e, 失败也继续
set +e

pkill -9 -f "minimax-" 2>/dev/null
sleep 1

echo "=== start $(date) ==="
cd /workspace/minimax-platform/backend/minimax-auth/target
java -Xms256m -Xmx512m -jar minimax-auth.jar --spring.profiles.active=test --server.port=8081 > /workspace/.e2e/auth.log 2>&1 &
A=$!
cd /workspace/minimax-platform/backend/minimax-memory/target
java -Xms256m -Xmx768m -jar minimax-memory.jar --spring.profiles.active=test --server.port=8084 > /workspace/.e2e/memory.log 2>&1 &
M=$!
echo "auth=$A mem=$M"

# wait ready
READY=0
for i in $(seq 1 40); do
  sleep 1
  curl -sf -m 1 http://localhost:8081/api/v1/auth/health >/dev/null 2>&1 || continue
  curl -sf -m 1 http://localhost:8084/api/v1/memory/short-term/100/size >/dev/null 2>&1 || continue
  echo "ready in ${i}s"
  READY=1
  break
done
[ $READY -eq 0 ] && { echo "FAILED to start"; tail -20 /workspace/.e2e/memory.log; kill $A $M; exit 1; }

echo "--- LOGIN ---"
LOGIN=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin@123"}')
echo "$LOGIN" | head -c 300
echo
TOKEN=$(echo "$LOGIN" | grep -oE '"accessToken":"[^"]+"' | cut -d'"' -f4)
echo "TOKEN=${TOKEN:0:40}..."

echo
echo "--- E2E 1: store 3 ---"
for msg in "我最喜欢吃川菜" "我的工作是后端开发" "我养了一只猫叫小黄"; do
  curl -s -X POST http://localhost:8084/api/v1/memory/long-term \
    -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d "{\"userId\":1,\"sessionId\":999,\"role\":\"user\",\"content\":\"$msg\"}"
  echo
done

echo
echo "--- E2E 2: recall pet ---"
curl -s -X POST http://localhost:8084/api/v1/memory/long-term/recall \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"userId":1,"query":"我养什么宠物","topK":3}'
echo

echo
echo "--- E2E 3: recall k8s ---"
curl -s -X POST http://localhost:8084/api/v1/memory/long-term/recall \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"userId":1,"query":"kubernetes 部署","topK":3}'
echo

echo
echo "--- E2E 4: recent ---"
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8084/api/v1/memory/long-term/recent?userId=1&limit=5"
echo

echo
echo "--- E2E 5: pref set/get ---"
curl -s -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "http://localhost:8084/api/v1/memory/pref/language?userId=1" \
  -d '{"value":"zh-CN","source":"manual"}'
echo
curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8084/api/v1/memory/pref/language?userId=1"
echo
curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8084/api/v1/memory/pref?userId=1"
echo

echo
echo "--- E2E 6: cross context ---"
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  http://localhost:8084/api/v1/memory/cross-context \
  -d '{"userId":1,"sessionId":999,"systemPrompt":"你是助手","maxContext":4096,"recallTopK":3}'
echo

echo
echo "--- E2E 7: short-term append+get+size ---"
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  http://localhost:8084/api/v1/memory/short-term/999 \
  -d '{"role":"user","content":"今天很开心"}'
echo
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8084/api/v1/memory/short-term/999
echo
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8084/api/v1/memory/short-term/999/size
echo

echo
echo "=== end ==="
kill $A $M 2>/dev/null
echo "result saved to: $OUT"
