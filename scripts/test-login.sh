#!/bin/bash
mkdir -p /workspace/.e2e
pkill -9 -f "minimax-" 2>/dev/null
sleep 1
cd /workspace/minimax-platform/backend/minimax-auth/target
java -Xms256m -Xmx512m -jar minimax-auth.jar --spring.profiles.active=test --server.port=8081 > /workspace/.e2e/auth3.log 2>&1 &
A=$!
echo "auth=$A" > /workspace/.e2e/test-login.txt

for i in $(seq 1 25); do
  sleep 1
  if curl -sf -m 1 http://localhost:8081/api/v1/auth/health >/dev/null 2>&1; then
    echo "ready ${i}s" >> /workspace/.e2e/test-login.txt
    break
  fi
done

echo "--- /auth/health ---" >> /workspace/.e2e/test-login.txt
curl -s http://localhost:8081/api/v1/auth/health >> /workspace/.e2e/test-login.txt 2>&1
echo "" >> /workspace/.e2e/test-login.txt

echo "--- LOGIN ---" >> /workspace/.e2e/test-login.txt
curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin@123"}' >> /workspace/.e2e/test-login.txt 2>&1
echo "" >> /workspace/.e2e/test-login.txt

echo "--- LOGIN different json keys ---" >> /workspace/.e2e/test-login.txt
curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"account":"admin","pwd":"admin@123"}' >> /workspace/.e2e/test-login.txt 2>&1
echo "" >> /workspace/.e2e/test-login.txt

kill $A 2>/dev/null
echo "DONE" >> /workspace/.e2e/test-login.txt
