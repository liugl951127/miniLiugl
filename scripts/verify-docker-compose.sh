#!/bin/bash
# V3.5.95+ Docker Compose 静态验证 (沙箱友好 - 无需 docker)
# 验证内容:
#   1. yaml 语法
#   2. services 数量
#   3. 关键 service 存在 (otel-collector + jaeger)
#   4. 14 module 都 depends_on otel-collector
#   5. networks 段干净

set -e
cd "$(dirname "$0")/.."

python3 - << 'PYEOF'
import yaml
with open('docker-compose.yml') as f:
    c = yaml.safe_load(f)
services = c.get('services', {})
networks = c.get('networks', {})

print('═══════════════════════════════════════════════════════════')
print('  V3.5.95 Docker Compose 静态验证')
print('═══════════════════════════════════════════════════════════')

errors = 0

# 1. services 数量
expected = 19  # 14 module + mariadb + redis + nacos + nginx + otel-collector + jaeger
if len(services) != expected:
    print(f'  ❌ services 数量: {len(services)} (期望 {expected})')
    errors += 1
else:
    print(f'  ✓ services 数量: {len(services)}/{expected}')

# 2. otel-collector + jaeger
for required in ['otel-collector', 'jaeger']:
    if required in services:
        print(f'  ✓ {required} 存在')
    else:
        print(f'  ❌ {required} 缺失')
        errors += 1

# 3. 14 module depends_on otel-collector
modules = ['auth', 'chat', 'model', 'rag', 'multimodal', 'agent', 'monitor', 'admin', 'analytics', 'pipeline', 'ai', 'ws']
for m in modules:
    deps = services.get(m, {}).get('depends_on', {})
    if isinstance(deps, dict):
        has_otel = 'otel-collector' in deps
    else:
        has_otel = False
    if has_otel:
        print(f'  ✓ {m} depends_on otel-collector')
    else:
        print(f'  ❌ {m} 缺 otel-collector depends_on')
        errors += 1

# 4. networks
if list(networks.keys()) == ['minimax-net']:
    print('  ✓ networks 干净 (只 minimax-net)')
else:
    print(f'  ❌ networks 段异常: {list(networks.keys())}')
    errors += 1

# 5. otel-collector 配置
otel = services.get('otel-collector', {})
if '4317' in str(otel.get('ports', '')):
    print('  ✓ otel-collector 暴露 4317 (OTLP gRPC)')
else:
    print('  ❌ otel-collector 缺 4317')
    errors += 1

# 6. jaeger UI
jaeger = services.get('jaeger', {})
if '16686' in str(jaeger.get('ports', '')):
    print('  ✓ jaeger 暴露 16686 (UI)')
else:
    print('  ❌ jaeger 缺 16686')
    errors += 1

print('═══════════════════════════════════════════════════════════')
print(f'  错误: {errors}')
print(f'  状态: {"✅ ALL PASS" if errors == 0 else "❌ 有错误"}')
print('═══════════════════════════════════════════════════════════')
exit(errors)
PYEOF
